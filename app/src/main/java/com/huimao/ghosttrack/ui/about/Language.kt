package com.huimao.ghosttrack.ui.about

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * 语言设置：跟随系统 / 中文 / English。
 * 通过 AppCompatDelegate.setApplicationLocales 生效，重启 Activity 后切换。
 */
object LanguageSetting {
    const val SYSTEM = "system"
    const val ZH = "zh"
    const val EN = "en"

    private const val PREFS = "settings"
    private const val KEY = "lang"

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, SYSTEM) ?: SYSTEM

    fun apply(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, value).apply()
        val locales = when (value) {
            ZH -> LocaleListCompat.forLanguageTags("zh")
            EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
