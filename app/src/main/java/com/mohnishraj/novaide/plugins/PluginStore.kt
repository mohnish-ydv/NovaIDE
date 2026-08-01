package com.mohnishraj.novaide.plugins

import android.content.Context
import android.util.Base64

class PluginStore(context: Context) {
    private val prefs = context.getSharedPreferences("nova_plugins", Context.MODE_PRIVATE)

    fun installed(): List<InstalledPlugin> = prefs.getStringSet(KEY_MANIFESTS, emptySet()).orEmpty()
        .mapNotNull { encoded ->
            runCatching {
                val raw = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
                val manifest = PluginManifestParser.parse(raw)
                InstalledPlugin(manifest, raw, prefs.getBoolean(enabledKey(manifest.id), true))
            }.getOrNull()
        }
        .sortedBy { it.manifest.name.lowercase() }

    fun install(raw: String): InstalledPlugin {
        val manifest = PluginManifestParser.parse(raw)
        val encoded = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val existing = prefs.getStringSet(KEY_MANIFESTS, emptySet()).orEmpty().toMutableSet()
        existing.removeAll { item ->
            runCatching {
                val oldRaw = String(Base64.decode(item, Base64.NO_WRAP), Charsets.UTF_8)
                PluginManifestParser.parse(oldRaw).id == manifest.id
            }.getOrDefault(false)
        }
        existing += encoded
        prefs.edit().putStringSet(KEY_MANIFESTS, existing).putBoolean(enabledKey(manifest.id), true).apply()
        return InstalledPlugin(manifest, raw, enabled = true)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        prefs.edit().putBoolean(enabledKey(id), enabled).apply()
    }

    fun uninstall(id: String) {
        val kept = prefs.getStringSet(KEY_MANIFESTS, emptySet()).orEmpty().filterNot { item ->
            runCatching {
                val raw = String(Base64.decode(item, Base64.NO_WRAP), Charsets.UTF_8)
                PluginManifestParser.parse(raw).id == id
            }.getOrDefault(false)
        }.toSet()
        prefs.edit().putStringSet(KEY_MANIFESTS, kept).remove(enabledKey(id)).apply()
    }

    private fun enabledKey(id: String) = "enabled.$id"

    companion object { private const val KEY_MANIFESTS = "manifests" }
}
