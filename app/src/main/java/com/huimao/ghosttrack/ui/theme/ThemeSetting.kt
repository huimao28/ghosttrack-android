package com.huimao.ghosttrack.ui.theme

import android.content.Context

/** Material You（莫奈）动态取色偏好，默认开启。 */
object ThemeSetting {
    private const val PREFS = "settings"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"

    fun dynamicColorEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DYNAMIC_COLOR, true)

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DYNAMIC_COLOR, enabled)
            .apply()
    }
}
