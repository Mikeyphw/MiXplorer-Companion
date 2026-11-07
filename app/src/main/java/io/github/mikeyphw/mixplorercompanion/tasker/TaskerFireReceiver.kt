package io.github.mikeyphw.mixplorercompanion.tasker

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

/** Tasker-compatible dispatch bridge: receives Tasker FIRE_SETTING, not MiXplorer ACTION_TASK. */
class TaskerFireReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TaskerContract.FIRE) {
            fail("wrong_action")
            return
        }
        // API 34+ provides broadcast sender identity. If unavailable, a private
        // per-install token is still mandatory, rather than trusting Intent extras.
        if (Build.VERSION.SDK_INT >= 34) {
            val sender = sentFromPackage
            if (sender != null && !TaskerContract.isTrustedHost(sender)) {
                fail("untrusted_sender")
                return
            }
        }
        val data = try { intent.getBundleExtra(TaskerContract.BUNDLE) }
                   catch (_: RuntimeException) { null }
        if (!TaskerContract.isEnabled(context)) { fail("automation_disabled"); return }
        val candidate = try { data?.getString("token") } catch (_: RuntimeException) { null }
        if (!TaskerContract.hasValidToken(context, candidate)) {
            fail("invalid_capability"); return
        }
        val request = try { TaskerRequest.fromBundle(data) } catch (_: RuntimeException) { null }
        if (request == null) { fail("invalid_schema"); return }
        if (request.error() != null) { fail("invalid_input"); return }
        val version = try {
            val info = context.packageManager.getPackageInfo(TaskerContract.MIX_PACKAGE, 0)
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        } catch (_: RuntimeException) { -1L }
        if (version != TaskerContract.MIX_VERSION) { fail("unsupported_beta"); return }
        // Explicit exported component. No implicit broadcast, root, storage permission,
        // or privileged file manager data access is performed by the companion.
        val outgoing = Intent(TaskerContract.MIX_ACTION).apply {
            component = ComponentName(TaskerContract.MIX_PACKAGE, TaskerContract.MIX_RECEIVER)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            putExtra("task", request.operation)
            if (request.source.isNotEmpty()) putExtra("src", request.source)
            if (request.destination.isNotEmpty()) putExtra("dst", request.destination)
            if (request.name.isNotEmpty()) putExtra("name", request.name)
        }
        try {
            context.sendBroadcast(outgoing)
        } catch (_: RuntimeException) {
            fail("broadcast_rejected")
            return
        }
        // Privacy-preserving marker: never log source, destination, or token.
        android.util.Log.i("MiXTaskerBridge", "R3T_DISPATCHED_UNVERIFIED")
        // IMPORTANT: a successful sendBroadcast() is not MiXplorer completion.
        if (isOrderedBroadcast) {
            resultCode = Activity.RESULT_OK
            getResultExtras(true)?.putBundle(TaskerContract.VARIABLES, Bundle().apply {
                putString("%mixstatus", "dispatched_unverified")
                putString("%mixaction", request.operation.lowercase(java.util.Locale.ROOT))
            })
        }
    }

    private fun fail(reason: String) {
        if (isOrderedBroadcast) {
            resultCode = TaskerContract.FAILED
            getResultExtras(true)?.putBundle(TaskerContract.VARIABLES, Bundle().apply {
                putString("%mixstatus", "refused_$reason")
            })
        }
    }
}
