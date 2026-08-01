package com.mohnishraj.novaide.webpreview

import android.content.Context

data class WebPreviewSettings(
    val javaScriptEnabled: Boolean = true,
    val liveReload: Boolean = true,
    val allowExternalNetwork: Boolean = false,
    val spaFallback: Boolean = true,
    val viewport: PreviewViewport = PreviewViewport.RESPONSIVE
)

class WebPreviewSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("nova_web_preview", Context.MODE_PRIVATE)

    fun load(): WebPreviewSettings = WebPreviewSettings(
        javaScriptEnabled = prefs.getBoolean("javascript", true),
        liveReload = prefs.getBoolean("live_reload", true),
        allowExternalNetwork = prefs.getBoolean("external_network", false),
        spaFallback = prefs.getBoolean("spa_fallback", true),
        viewport = runCatching { PreviewViewport.valueOf(prefs.getString("viewport", null) ?: PreviewViewport.RESPONSIVE.name) }
            .getOrDefault(PreviewViewport.RESPONSIVE)
    )

    fun save(settings: WebPreviewSettings) {
        prefs.edit()
            .putBoolean("javascript", settings.javaScriptEnabled)
            .putBoolean("live_reload", settings.liveReload)
            .putBoolean("external_network", settings.allowExternalNetwork)
            .putBoolean("spa_fallback", settings.spaFallback)
            .putString("viewport", settings.viewport.name)
            .apply()
    }
}
