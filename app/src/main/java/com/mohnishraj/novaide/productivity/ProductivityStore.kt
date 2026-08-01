package com.mohnishraj.novaide.productivity

import android.content.Context
import android.util.Base64

class ProductivityStore(context: Context) {
    private val prefs = context.getSharedPreferences("nova_productivity", Context.MODE_PRIVATE)

    fun customTasks(): List<NovaTask> = prefs.getStringSet(KEY_TASKS, emptySet()).orEmpty().mapNotNull(::decodeTask).sortedBy { it.name }

    fun saveTask(task: NovaTask) {
        TaskRunner.validate(task)
        val items = prefs.getStringSet(KEY_TASKS, emptySet()).orEmpty().toMutableSet()
        items.removeAll { decodeTask(it)?.id == task.id }
        items += encodeTask(task)
        prefs.edit().putStringSet(KEY_TASKS, items).apply()
    }

    fun deleteTask(id: String) {
        val kept = prefs.getStringSet(KEY_TASKS, emptySet()).orEmpty().filterNot { decodeTask(it)?.id == id }.toSet()
        prefs.edit().putStringSet(KEY_TASKS, kept).apply()
    }

    private fun encodeTask(task: NovaTask): String {
        val fields = listOf(task.id, task.name, task.description, task.commands.joinToString("\u001E"))
        return Base64.encodeToString(fields.joinToString("\u001F").toByteArray(), Base64.NO_WRAP)
    }

    private fun decodeTask(encoded: String): NovaTask? = runCatching {
        val raw = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
        val fields = raw.split("\u001F", limit = 4)
        TaskRunner.validate(NovaTask(fields[0], fields[1], fields[2], fields[3].split("\u001E").filter { it.isNotBlank() }))
    }.getOrNull()

    companion object { private const val KEY_TASKS = "tasks" }
}
