package com.mohnishraj.novaide.runtime

import android.content.Context

class TermuxRunResultStore(context: Context) {
    private val prefs = context.getSharedPreferences("nova_termux_results", Context.MODE_PRIVATE)

    fun save(result: TermuxRunResult) {
        prefs.edit()
            .putInt("execution_id", result.executionId)
            .putString("label", result.label)
            .putInt("exit_code", result.exitCode)
            .putString("stdout", result.stdout.takeLast(100_000))
            .putString("stderr", result.stderr.takeLast(100_000))
            .putInt("error_code", result.errorCode)
            .putString("error_message", result.errorMessage.take(25_000))
            .putLong("finished_at", result.finishedAt)
            .putBoolean("unread", true)
            .apply()
    }

    fun takeUnread(): TermuxRunResult? {
        if (!prefs.getBoolean("unread", false)) return null
        val id = prefs.getInt("execution_id", 0)
        if (id == 0) return null
        val result = TermuxRunResult(
            executionId = id,
            label = prefs.getString("label", null).orEmpty(),
            exitCode = prefs.getInt("exit_code", -1),
            stdout = prefs.getString("stdout", null).orEmpty(),
            stderr = prefs.getString("stderr", null).orEmpty(),
            errorCode = prefs.getInt("error_code", 0),
            errorMessage = prefs.getString("error_message", null).orEmpty(),
            finishedAt = prefs.getLong("finished_at", System.currentTimeMillis())
        )
        prefs.edit().putBoolean("unread", false).apply()
        return result
    }

    fun latest(): TermuxRunResult? {
        val id = prefs.getInt("execution_id", 0)
        if (id == 0) return null
        return TermuxRunResult(
            id,
            prefs.getString("label", null).orEmpty(),
            prefs.getInt("exit_code", -1),
            prefs.getString("stdout", null).orEmpty(),
            prefs.getString("stderr", null).orEmpty(),
            prefs.getInt("error_code", 0),
            prefs.getString("error_message", null).orEmpty(),
            prefs.getLong("finished_at", 0L)
        )
    }
}
