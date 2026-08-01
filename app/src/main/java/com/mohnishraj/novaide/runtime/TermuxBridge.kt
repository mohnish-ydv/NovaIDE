package com.mohnishraj.novaide.runtime

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import java.util.concurrent.atomic.AtomicInteger

class TermuxBridge(private val context: Context) {
    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
        const val RUN_PERMISSION = "com.termux.permission.RUN_COMMAND"
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_LABEL"
        const val EXTRA_COMMAND_DESCRIPTION = "com.termux.RUN_COMMAND_DESCRIPTION"
        const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"
        const val EXTRA_PLUGIN_RESULT_BUNDLE = "com.termux.plugin_service_bundle"
        const val EXTRA_RESULT_STDOUT = "stdout"
        const val EXTRA_RESULT_STDERR = "stderr"
        const val EXTRA_RESULT_EXIT_CODE = "exitCode"
        const val EXTRA_RESULT_ERROR = "err"
        const val EXTRA_RESULT_ERROR_MESSAGE = "errmsg"
        const val EXTRA_NOVA_EXECUTION_ID = "nova_execution_id"
        const val EXTRA_NOVA_LABEL = "nova_execution_label"
        private val nextId = AtomicInteger(10_000)
    }

    fun isInstalled(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= 33) context.packageManager.getPackageInfo(TERMUX_PACKAGE, PackageManager.PackageInfoFlags.of(0))
        else @Suppress("DEPRECATION") context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
    }.isSuccess

    fun hasPermission(): Boolean = context.checkSelfPermission(RUN_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun sharedWorkspacePath(treeUri: Uri?): String? {
        treeUri ?: return null
        val id = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        return SharedWorkspacePathResolver.resolve(treeUri.authority, id)
    }

    fun environment(treeUri: Uri?, allowExternalAppsConfirmed: Boolean): RuntimeEnvironment = RuntimeEnvironment(
        termuxInstalled = isInstalled(),
        runPermissionGranted = hasPermission(),
        sharedWorkspacePath = sharedWorkspacePath(treeUri),
        allowExternalAppsConfirmed = allowExternalAppsConfirmed
    )

    fun run(command: RuntimeCommand, workDir: String, foreground: Boolean = command.opensServer): Int {
        TermuxCommandPolicy.validate(command)
        require(workDir.startsWith("/storage/") && !workDir.contains('\n') && !workDir.contains('\r')) { "Unsafe or unsupported Termux workspace path" }
        check(isInstalled()) { "Termux is not installed" }
        check(hasPermission()) { "NovaIDE does not have Termux RUN_COMMAND permission" }
        val executionId = nextId.incrementAndGet()
        val resultIntent = Intent(context, TermuxResultReceiver::class.java).apply {
            putExtra(EXTRA_NOVA_EXECUTION_ID, executionId)
            putExtra(EXTRA_NOVA_LABEL, command.label)
        }
        val flags = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingResult = PendingIntent.getBroadcast(context, executionId, resultIntent, flags)
        val shell = "exec ${TermuxCommandPolicy.shellScript(command)}"
        val intent = Intent().apply {
            setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
            action = ACTION_RUN_COMMAND
            putExtra(EXTRA_COMMAND_PATH, "\$PREFIX/bin/bash")
            putExtra(EXTRA_ARGUMENTS, arrayOf("-lc", shell))
            putExtra(EXTRA_WORKDIR, workDir)
            putExtra(EXTRA_BACKGROUND, !foreground)
            putExtra(EXTRA_SESSION_ACTION, "0")
            putExtra(EXTRA_COMMAND_LABEL, "NovaIDE · ${command.label}")
            putExtra(EXTRA_COMMAND_DESCRIPTION, command.description.take(1_000))
            putExtra(EXTRA_PENDING_INTENT, pendingResult)
        }
        context.startService(intent)
        return executionId
    }
}
