package com.mohnishraj.novaide.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.getBundleExtra(TermuxBridge.EXTRA_PLUGIN_RESULT_BUNDLE) ?: return
        val result = TermuxRunResult(
            executionId = intent.getIntExtra(TermuxBridge.EXTRA_NOVA_EXECUTION_ID, 0),
            label = intent.getStringExtra(TermuxBridge.EXTRA_NOVA_LABEL).orEmpty(),
            exitCode = bundle.getInt(TermuxBridge.EXTRA_RESULT_EXIT_CODE, -1),
            stdout = bundle.getString(TermuxBridge.EXTRA_RESULT_STDOUT).orEmpty(),
            stderr = bundle.getString(TermuxBridge.EXTRA_RESULT_STDERR).orEmpty(),
            errorCode = bundle.getInt(TermuxBridge.EXTRA_RESULT_ERROR, 0),
            errorMessage = bundle.getString(TermuxBridge.EXTRA_RESULT_ERROR_MESSAGE).orEmpty()
        )
        TermuxRunResultStore(context).save(result)
    }
}
