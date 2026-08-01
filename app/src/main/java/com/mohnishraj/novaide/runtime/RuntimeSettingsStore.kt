package com.mohnishraj.novaide.runtime

import android.content.Context

class RuntimeSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("nova_runtime", Context.MODE_PRIVATE)

    var allowExternalAppsConfirmed: Boolean
        get() = prefs.getBoolean("termux_allow_external_apps_confirmed", false)
        set(value) { prefs.edit().putBoolean("termux_allow_external_apps_confirmed", value).apply() }

    var lastServerPort: Int
        get() = prefs.getInt("last_server_port", 5173).coerceIn(1024, 65535)
        set(value) { prefs.edit().putInt("last_server_port", TermuxCommandPolicy.safePort(value)).apply() }
}
