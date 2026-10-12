package com.huimao.ghosttrack.data

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper
import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.google.i18n.phonenumbers.geocoder.PhoneNumberOfflineGeocoder
import java.util.Locale

/**
 * 号码解析：基于 Google libphonenumber，纯离线解析，无需网络。
 * 对应原版 GhostTrack 的 Phone Number Tracker 菜单。
 */
data class PhoneInfo(
    val input: String,
    val valid: Boolean,
    val possible: Boolean,
    val typeLabel: String,
    val carrier: String,
    val regionCode: String,
    val location: String,
    val timezones: List<String>,
    val e164: String,
    val international: String,
    val national: String,
    val countryCode: Int,
    val nationalNumber: Long,
)

object PhoneLookup {

    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /**
     * 解析号码。[defaultRegion] 为无国家码时的默认归属地（如 "CN"）。
     * 解析失败时 Result 携带异常。
     */
    fun analyze(raw: String, defaultRegion: String): Result<PhoneInfo> = runCatching {
        val number: Phonenumber.PhoneNumber = try {
            util.parse(raw.trim(), defaultRegion.uppercase())
        } catch (e: NumberParseException) {
            throw IllegalArgumentException("号码无法解析（${e.errorType}）")
        }

        val carrierMapper = PhoneNumberToCarrierMapper.getInstance()
        val geocoder = PhoneNumberOfflineGeocoder.getInstance()
        val tzMapper = PhoneNumberToTimeZonesMapper.getInstance()

        val carrier = carrierMapper.getNameForNumber(number, Locale.ENGLISH).orEmpty()
        val locationZh = geocoder.getDescriptionForNumber(number, Locale.CHINESE)
        val locationEn = geocoder.getDescriptionForNumber(number, Locale.ENGLISH)

        PhoneInfo(
            input = raw.trim(),
            valid = util.isValidNumber(number),
            possible = util.isPossibleNumber(number),
            typeLabel = typeLabel(util.getNumberType(number)),
            carrier = carrier.ifBlank { "未知" },
            regionCode = util.getRegionCodeForNumber(number) ?: "未知",
            location = locationZh.ifBlank { locationEn }.ifBlank { "未知" },
            timezones = tzMapper.getTimeZonesForNumber(number)?.toList().orEmpty(),
            e164 = util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164),
            international = util.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL),
            national = util.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL),
            countryCode = number.countryCode,
            nationalNumber = number.nationalNumber,
        )
    }

    private fun typeLabel(type: PhoneNumberUtil.PhoneNumberType): String = when (type) {
        PhoneNumberUtil.PhoneNumberType.MOBILE -> "移动号码"
        PhoneNumberUtil.PhoneNumberType.FIXED_LINE -> "固定电话"
        PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE -> "固话或移动"
        PhoneNumberUtil.PhoneNumberType.TOLL_FREE -> "免费电话"
        PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE -> "高费率号码"
        PhoneNumberUtil.PhoneNumberType.SHARED_COST -> "分摊话费号码"
        PhoneNumberUtil.PhoneNumberType.VOIP -> "网络电话 (VoIP)"
        PhoneNumberUtil.PhoneNumberType.PERSONAL_NUMBER -> "个人号码"
        PhoneNumberUtil.PhoneNumberType.PAGER -> "寻呼机"
        PhoneNumberUtil.PhoneNumberType.UAN -> "企业接入号"
        PhoneNumberUtil.PhoneNumberType.VOICEMAIL -> "语音信箱"
        else -> "未知类型"
    }
}
