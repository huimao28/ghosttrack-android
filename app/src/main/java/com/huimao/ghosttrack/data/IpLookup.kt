package com.huimao.ghosttrack.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * IP 信息查询：数据源为免费的 ipwho.is（HTTPS，无需 key）。
 * 本机 IP 通过 api.ipify.org 获取。
 */
@Serializable
data class IpWhoIsResponse(
    val ip: String = "",
    val success: Boolean = false,
    val message: String? = null,
    val type: String = "",
    val continent: String = "",
    @SerialName("continent_code") val continentCode: String = "",
    val country: String = "",
    @SerialName("country_code") val countryCode: String = "",
    val region: String = "",
    @SerialName("region_code") val regionCode: String = "",
    val city: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("is_eu") val isEu: Boolean? = null,
    val postal: String? = null,
    @SerialName("calling_code") val callingCode: String? = null,
    val capital: String? = null,
    val borders: String? = null,
    val flag: FlagInfo? = null,
    val connection: ConnectionInfo? = null,
    val timezone: TimezoneInfo? = null,
)

@Serializable
data class FlagInfo(
    val img: String = "",
    val emoji: String = "",
    @SerialName("emoji_unicode") val emojiUnicode: String = "",
)

@Serializable
data class ConnectionInfo(
    val asn: Long? = null,
    val org: String? = null,
    val isp: String? = null,
    val domain: String? = null,
)

@Serializable
data class TimezoneInfo(
    val id: String = "",
    val abbr: String = "",
    @SerialName("is_dst") val isDst: Boolean? = null,
    val offset: Long? = null,
    val utc: String = "",
    @SerialName("current_time") val currentTime: String? = null,
)

@Serializable
private data class MyIpResponse(val ip: String = "")

/**
 * IP 质量检测结果（数据源：ip-api.com 免费接口，HTTP）。
 *
 * - hosting=true → 机房/数据中心 IP
 * - proxy=true → 疑似代理 / VPN / 出口中转
 * - mobile=true → 移动网络
 * - 原生 IP / 纯净度均为基于上述标记的本地启发式估算，仅供参考，
 *   不是专业风控评分。
 */
@Serializable
data class IpApiQualityResponse(
    val status: String = "",
    val message: String? = null,
    val query: String = "",
    val proxy: Boolean = false,
    val hosting: Boolean = false,
    val mobile: Boolean = false,
)

data class IpQuality(
    val proxy: Boolean,
    val hosting: Boolean,
    val mobile: Boolean,
) {
    /** 网络类型：机房 / 移动网络 / 家庭宽带（估算） */
    fun networkTypeLabel(): String = when {
        hosting -> "机房 / 数据中心"
        mobile -> "移动网络"
        else -> "家庭宽带（估算）"
    }

    /** 原生 IP（估算）：非机房且非代理 */
    fun isNative(): Boolean = !hosting && !proxy

    /** 纯净度估算分：100 起步，机房 -50，代理 -30，移动 -10 */
    fun purityScore(): Int = (100 -
        (if (hosting) 50 else 0) -
        (if (proxy) 30 else 0) -
        (if (mobile) 10 else 0)).coerceAtLeast(0)

    fun purityGrade(): String = when (purityScore()) {
        in 90..100 -> "优秀"
        in 70..89 -> "良好"
        in 40..69 -> "一般"
        else -> "较差"
    }
}

class IpLookup(private val client: OkHttpClient = defaultClient()) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /** 查询指定 IP，失败时 Result 携带异常信息。 */
    fun lookup(ip: String): Result<IpWhoIsResponse> = runCatching {
        val request = Request.Builder()
            .url("https://ipwho.is/${ip.trim()}")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val info = parseResponse(response.body?.string().orEmpty())
            if (!info.success) throw IOException(info.message ?: "查询失败")
            info
        }
    }

    /** 获取本机出口 IP。 */
    fun myIp(): Result<String> = runCatching {
        val request = Request.Builder()
            .url("https://api.ipify.org?format=json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val ip = json.decodeFromString<MyIpResponse>(response.body?.string().orEmpty()).ip
            if (ip.isBlank()) throw IOException("返回为空")
            ip
        }
    }

    /** 纯解析逻辑，供单元测试使用。 */
    fun parseResponse(body: String): IpWhoIsResponse =
        json.decodeFromString(IpWhoIsResponse.serializer(), body)

    /**
     * IP 质量检测：机房 / 代理 / 移动网络标记。
     * 注意 ip-api.com 免费档仅支持 HTTP（仓库 manifest 已开明文流量）。
     */
    fun quality(ip: String): Result<IpQuality> = runCatching {
        val request = Request.Builder()
            .url("http://ip-api.com/json/${ip.trim()}?fields=status,message,query,proxy,hosting,mobile")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            parseQuality(response.body?.string().orEmpty())
        }
    }

    /** 纯解析逻辑，供单元测试使用。 */
    fun parseQuality(body: String): IpQuality {
        val q = json.decodeFromString(IpApiQualityResponse.serializer(), body)
        if (q.status != "success") throw IOException(q.message ?: "质量检测失败")
        return IpQuality(proxy = q.proxy, hosting = q.hosting, mobile = q.mobile)
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}
