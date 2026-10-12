package com.huimao.ghosttrack.data.username

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 用户名搜索：在一批站点上并发检查用户名是否存在。
 *
 * 判定策略是逐站点实测得出的（见 docs/CHECK_STRATEGIES.md），因为很多站点对不
 * 存在的用户名并不返回 404（返回 200、302 跳转到登录页等）。结果分三种：
 * FOUND（存在）、NOT_FOUND（不存在）、UNKNOWN（无法判断，如被反爬拦截）。
 */

enum class CheckStatus { FOUND, NOT_FOUND, UNKNOWN }

sealed interface CheckStrategy {
    /** 200=存在，404=不存在，其它=未知 */
    data object StatusCode : CheckStrategy

    /** 页面包含任一标记 => 不存在；404 => 不存在；200 且无标记 => 存在 */
    data class MissingMarkers(val markers: List<String>) : CheckStrategy

    /** 最终 URL 包含任一标记（如登录页） => 不存在；200 => 存在 */
    data class RedirectMarkers(val markers: List<String>) : CheckStrategy
}

data class UsernameSite(
    val name: String,
    /** '{}' 会被替换为用户名 */
    val urlTemplate: String,
    val strategy: CheckStrategy,
)

data class SiteResult(
    val site: UsernameSite,
    val status: CheckStatus,
    /** 实际打开的个人主页 URL */
    val url: String,
    val note: String = "",
)

val USERNAME_SITES: List<UsernameSite> = listOf(
    UsernameSite("GitHub", "https://api.github.com/users/{}", CheckStrategy.StatusCode),
    UsernameSite("X", "https://x.com/{}", CheckStrategy.StatusCode),
    UsernameSite(
        "Instagram", "https://www.instagram.com/{}/",
        CheckStrategy.RedirectMarkers(listOf("accounts/login")),
    ),
    UsernameSite(
        "TikTok", "https://www.tiktok.com/@{}",
        CheckStrategy.MissingMarkers(listOf("\"statusCode\":10221")),
    ),
    UsernameSite("YouTube", "https://www.youtube.com/@{}", CheckStrategy.StatusCode),
    UsernameSite(
        "Facebook", "https://www.facebook.com/{}",
        CheckStrategy.RedirectMarkers(listOf("/login", "login.php")),
    ),
    UsernameSite("Snapchat", "https://www.snapchat.com/@{}", CheckStrategy.StatusCode),
    UsernameSite(
        "Telegram", "https://t.me/{}",
        CheckStrategy.MissingMarkers(listOf("Telegram: Contact @")),
    ),
    UsernameSite(
        "Pinterest", "https://www.pinterest.com/{}/",
        CheckStrategy.MissingMarkers(listOf("User not found")),
    ),
    UsernameSite("Tumblr", "https://www.tumblr.com/{}", CheckStrategy.StatusCode),
    UsernameSite("Dribbble", "https://www.dribbble.com/{}", CheckStrategy.StatusCode),
    UsernameSite("SoundCloud", "https://soundcloud.com/{}", CheckStrategy.StatusCode),
    UsernameSite("Quora", "https://www.quora.com/profile/{}", CheckStrategy.StatusCode),
    UsernameSite("Behance", "https://www.behance.net/{}", CheckStrategy.StatusCode),
    UsernameSite("LinkedIn", "https://www.linkedin.com/in/{}", CheckStrategy.StatusCode),
)

/**
 * 纯判定逻辑（无网络），可单元测试。
 *
 * @param code HTTP 状态码（跟随跳转后）
 * @param finalUrl 跟随跳转后的最终 URL
 * @param body 响应正文（仅 MissingMarkers 策略需要，可为 null）
 */
fun evaluate(
    site: UsernameSite,
    code: Int,
    finalUrl: String,
    body: String?,
): CheckStatus = when (val strategy = site.strategy) {
    is CheckStrategy.RedirectMarkers -> when {
        strategy.markers.any { finalUrl.contains(it, ignoreCase = true) } -> CheckStatus.NOT_FOUND
        code == 200 -> CheckStatus.FOUND
        else -> CheckStatus.UNKNOWN
    }
    is CheckStrategy.MissingMarkers -> when {
        code == 404 -> CheckStatus.NOT_FOUND
        body != null && strategy.markers.any { body.contains(it) } -> CheckStatus.NOT_FOUND
        code == 200 -> CheckStatus.FOUND
        else -> CheckStatus.UNKNOWN
    }
    CheckStrategy.StatusCode -> when (code) {
        200 -> CheckStatus.FOUND
        404 -> CheckStatus.NOT_FOUND
        else -> CheckStatus.UNKNOWN
    }
}

private const val BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

class UsernameChecker(
    private val parallelism: Int = 6,
    client: OkHttpClient? = null,
) {
    private val client: OkHttpClient = client ?: OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val withUa = chain.request().newBuilder()
                .header("User-Agent", BROWSER_UA)
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            chain.proceed(withUa)
        }
        .build()

    /** 阻塞式检查单个站点。 */
    fun checkOne(site: UsernameSite, username: String): SiteResult {
        val url = site.urlTemplate.replace("{}", username.trim())
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                // 只有需要正文标记的策略才读 body，避免下载无用大页面
                val body = if (site.strategy is CheckStrategy.MissingMarkers) {
                    response.body?.string()?.take(2_000_000)
                } else {
                    null
                }
                SiteResult(site, evaluate(site, response.code, finalUrl, body), url)
            }
        } catch (e: Exception) {
            SiteResult(site, CheckStatus.UNKNOWN, url, e.message?.take(80) ?: "请求失败")
        }
    }

    /** 并发检查全部站点，每完成一个回调一次。 */
    suspend fun checkAll(
        username: String,
        onEach: (SiteResult) -> Unit = {},
    ): List<SiteResult> = coroutineScope {
        val semaphore = Semaphore(parallelism)
        USERNAME_SITES.map { site ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    checkOne(site, username).also(onEach)
                }
            }
        }.awaitAll()
    }
}
