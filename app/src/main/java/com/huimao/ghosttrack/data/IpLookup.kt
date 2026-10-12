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

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}
