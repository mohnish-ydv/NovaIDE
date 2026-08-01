package com.mohnishraj.novaide.files

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

class WorkspaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("nova_workspace", Context.MODE_PRIVATE)

    data class RecentWorkspace(
        val uri: Uri,
        val name: String,
        val kind: String,
        val lastOpened: Long,
        val favorite: Boolean
    )

    var workspaceUri: Uri?
        get() = prefs.getString("workspace_uri", null)?.let(Uri::parse)
        set(value) { prefs.edit().putString("workspace_uri", value?.toString()).apply() }

    private fun sessionKey(suffix: String): String = "session_${workspaceUri?.toString()?.hashCode() ?: 0}_$suffix"

    fun saveSession(activeUri: Uri?, tabs: List<TabState>) {
        val array = JSONArray()
        tabs.take(20).forEach { tab ->
            array.put(JSONObject().apply {
                put("uri", tab.uri.toString())
                put("cursorStart", tab.cursorStart)
                put("cursorEnd", tab.cursorEnd)
                put("scrollX", tab.scrollX)
                put("scrollY", tab.scrollY)
            })
        }
        prefs.edit()
            .putString(sessionKey("tabs"), array.toString())
            .putString(sessionKey("active"), activeUri?.toString())
            .apply()
    }

    fun restoreSession(): SessionState {
        val raw = prefs.getString(sessionKey("tabs"), "[]") ?: "[]"
        val tabs = mutableListOf<TabState>()
        runCatching {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                tabs += TabState(
                    uri = Uri.parse(item.getString("uri")),
                    cursorStart = item.optInt("cursorStart", 0),
                    cursorEnd = item.optInt("cursorEnd", 0),
                    scrollX = item.optInt("scrollX", 0),
                    scrollY = item.optInt("scrollY", 0)
                )
            }
        }
        return SessionState(
            activeUri = prefs.getString(sessionKey("active"), null)?.let(Uri::parse),
            tabs = tabs
        )
    }

    fun clearSession() {
        prefs.edit().remove(sessionKey("tabs")).remove(sessionKey("active")).apply()
    }

    fun recordWorkspace(uri: Uri, name: String, kind: String) {
        val existing = recentWorkspaces().associateBy { it.uri.toString() }.toMutableMap()
        val old = existing[uri.toString()]
        existing[uri.toString()] = RecentWorkspace(uri, name, kind, System.currentTimeMillis(), old?.favorite ?: false)
        saveRecent(existing.values.sortedWith(compareByDescending<RecentWorkspace> { it.favorite }.thenByDescending { it.lastOpened }).take(20))
    }

    fun recentWorkspaces(): List<RecentWorkspace> {
        val raw = prefs.getString("recent_workspaces", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(RecentWorkspace(
                        Uri.parse(item.getString("uri")),
                        runCatching { item.getString("name") }.getOrDefault("Workspace"),
                        runCatching { item.getString("kind") }.getOrDefault("General Project"),
                        runCatching { item.getString("lastOpened").toLong() }.getOrDefault(0L),
                        runCatching { item.getString("favorite").toBoolean() }.getOrDefault(false)
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun toggleFavorite(uri: Uri): Boolean {
        var state = false
        val updated = recentWorkspaces().map {
            if (it.uri == uri) it.copy(favorite = !it.favorite).also { changed -> state = changed.favorite } else it
        }
        saveRecent(updated.sortedWith(compareByDescending<RecentWorkspace> { it.favorite }.thenByDescending { it.lastOpened }))
        return state
    }

    fun removeRecent(uri: Uri) = saveRecent(recentWorkspaces().filterNot { it.uri == uri })

    private fun saveRecent(items: List<RecentWorkspace>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("uri", item.uri.toString())
                put("name", item.name)
                put("kind", item.kind)
                put("lastOpened", item.lastOpened.toString())
                put("favorite", item.favorite.toString())
            })
        }
        prefs.edit().putString("recent_workspaces", array.toString()).apply()
    }

    data class TabState(val uri: Uri, val cursorStart: Int, val cursorEnd: Int, val scrollX: Int, val scrollY: Int)
    data class SessionState(val activeUri: Uri?, val tabs: List<TabState>)
}
