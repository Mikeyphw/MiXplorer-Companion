package io.github.mikeyphw.mixplorercompanion.tasker

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log

/** Synchronous Locale/Tasker bridge result is dispatch-only, NEVER file completion. */
class TaskerFireReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        marker("RECEIVER_ENTERED")
        if (intent.action != TaskerContract.FIRE) {
            fail(intent, "wrong_action")
            return
        }
        // Android 14+ exposes the sender when available. Identity extras are NEVER trusted.
        if (Build.VERSION.SDK_INT >= 34) {
            val sender = sentFromPackage
            if (sender != null && !TaskerContract.isTrustedHost(sender)) {
                fail(intent, "untrusted_sender")
                return
            }
            if (sender == null) marker("SENDER_IDENTITY_UNAVAILABLE")
        }
        if (!TaskerContract.isEnabled(context)) {
            fail(intent, "automation_disabled")
            return
        }
        val data = try {
            intent.getBundleExtra(TaskerContract.BUNDLE)
        } catch (_: RuntimeException) {
            fail(intent, "malformed_bundle")
            return
        }
        val candidate = try {
            data?.getString("token")
        } catch (_: RuntimeException) {
            null
        }
        if (!TaskerContract.hasValidToken(context, candidate)) {
            fail(intent, "invalid_capability")
            return
        }
        val request = try {
            TaskerRequest.fromBundle(data)
        } catch (_: RuntimeException) {
            null
        }
        if (request == null) {
            fail(intent, "invalid_schema")
            return
        }
        if (request.error() != null) {
            fail(intent, "invalid_input")
            return
        }
        val version = try {
            val info = context.packageManager.getPackageInfo(TaskerContract.MIX_PACKAGE, 0)
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        } catch (_: PackageManager.NameNotFoundException) {
            -1L
        } catch (_: RuntimeException) {
            -1L
        }
        if (version != TaskerContract.MIX_VERSION) {
            fail(intent, "unsupported_beta")
            return
        }
        val outgoing = Intent(TaskerContract.MIX_ACTION).apply {
            component = ComponentName(TaskerContract.MIX_PACKAGE, TaskerContract.MIX_RECEIVER)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            putExtra("task", request.operation.lowercase(java.util.Locale.ROOT))
            if (request.source.isNotEmpty()) putExtra("src", request.source)
            if (request.destination.isNotEmpty()) putExtra("dst", request.destination)
            if (request.name.isNotEmpty()) putExtra("name", request.name)
        }
        marker("DISPATCH_ATTEMPT")
        try {
            context.sendBroadcast(outgoing)
        } catch (_: SecurityException) {
            fail(intent, "broadcast_rejected")
            return
        } catch (_: RuntimeException) {
            fail(intent, "broadcast_rejected")
            return
        }
        // sendBroadcast() returning is not an acknowledgement from MiXplorer.
        marker("DISPATCHED_UNVERIFIED")
        if (isOrderedBroadcast) {
            resultCode = Activity.RESULT_OK
            returnVariables(intent, Bundle().apply {
                putString("%mixstatus", "dispatched_unverified")
                putString("%mixaction", request.operation.lowercase(java.util.Locale.ROOT))
            })
        } else {
            marker("UNORDERED_RESULT_UNAVAILABLE")
        }
    }

    private fun marker(code: String) {
        // code is an in-source constant only: no paths, tokens, extras or caller details.
        Log.i("MiXTaskerBridge", "R3T_$code")
    }

    private fun returnVariables(intent: Intent, vars: Bundle) {
        // Tasker protocol: return variables only when the host advertises support (bit 2).
        val capabilities = try { intent.getIntExtra(TaskerContract.HOST_CAPABILITIES, 0) }
                           catch (_: RuntimeException) { 0 }
        if ((capabilities and TaskerContract.CAP_RETURN) != 0) {
            getResultExtras(true)?.putBundle(TaskerContract.VARIABLES, vars)
        }
    }

    private fun fail(intent: Intent, reason: String) {
        marker("REFUSED_$reason")
        if (isOrderedBroadcast) {
            resultCode = TaskerContract.FAILED
            val explanation = when (reason) {
                "wrong_action" -> "Unsupported Tasker action"
                "untrusted_sender" -> "Tasker host identity rejected"
                "automation_disabled" -> "Enable Tasker actions in MiXplorer Companion"
                "malformed_bundle" -> "Malformed Tasker configuration bundle"
                "invalid_capability" -> "Invalid or revoked Tasker configuration; reconfigure the action"
                "invalid_schema" -> "Outdated or malformed Tasker configuration; configure again"
                "invalid_input" -> "Missing, invalid or unresolved action input"
                "unsupported_beta" -> "Requires MiXplorer Beta 6.71.15 (26090412)"
                "broadcast_rejected" -> "Android rejected the MiXplorer broadcast"
                else -> "MiXplorer Companion refused the action"
            }
            returnVariables(intent, Bundle().apply {
                putString("%mixstatus", "refused_$reason")
                putString("%errmsg", explanation)
            })
        }
    }
}
