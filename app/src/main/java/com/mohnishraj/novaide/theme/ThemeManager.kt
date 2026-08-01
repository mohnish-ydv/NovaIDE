package com.mohnishraj.novaide.theme

import android.content.Context

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("nova_theme", Context.MODE_PRIVATE)

    enum class ThemeId { MIDNIGHT, AMOLED, DAYLIGHT }

    var currentTheme: ThemeId
        get() = runCatching {
            ThemeId.valueOf(prefs.getString("theme", ThemeId.MIDNIGHT.name) ?: ThemeId.MIDNIGHT.name)
        }.getOrDefault(ThemeId.MIDNIGHT)
        set(value) {
            prefs.edit().putString("theme", value.name).apply()
        }

    val palette: NovaPalette
        get() = when (currentTheme) {
            ThemeId.MIDNIGHT -> NovaPalette.MIDNIGHT
            ThemeId.AMOLED -> NovaPalette.AMOLED
            ThemeId.DAYLIGHT -> NovaPalette.DAYLIGHT
        }

    fun next(): ThemeId {
        val values = ThemeId.entries
        val next = values[(currentTheme.ordinal + 1) % values.size]
        currentTheme = next
        return next
    }
}
