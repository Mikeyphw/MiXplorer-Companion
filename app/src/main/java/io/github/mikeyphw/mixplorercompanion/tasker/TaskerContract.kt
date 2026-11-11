package io.github.mikeyphw.mixplorercompanion.tasker

import android.content.Context
import android.os.Bundle
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/** Locale/Tasker setting protocol, without a dependency on Tasker binaries. */
object TaskerContract {
    const val EDIT = "com.twofortyfouram.locale.intent.action.EDIT_SETTING"
    const val FIRE = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
    const val BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    const val BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"
    const val VARIABLES = "net.dinglisch.android.tasker.extras.VARIABLES"
    const val RELEVANT_VARIABLES = "net.dinglisch.android.tasker.RELEVANT_VARIABLES"
    const val REPLACE_KEYS = "net.dinglisch.android.tasker.extras.VARIABLE_REPLACE_KEYS"
    const val HOST_CAPABILITIES = "net.dinglisch.android.tasker.extras.HOST_CAPABILITIES"
    const val CAP_RETURN = 2
    const val CAP_REPLACE = 8
    const val CAP_RELEVANT = 16
    const val CAP_SYNC = 32
    const val REQUEST_TIMEOUT = "net.dinglisch.android.tasker.extras.REQUESTED_TIMEOUT"
    const val MIX_ACTION = "com.mixplorer.ACTION_TASK"
    const val MIX_PACKAGE = "com.mixplorer.beta"
    const val MIX_RECEIVER = "com.mixplorer.BroadcastReceiver"
    const val MIX_VERSION = 26090412L
    const val TASKER_PACKAGE = "net.dinglisch.android.taskerm"
    const val LOCALE_PACKAGE = "com.twofortyfouram.locale"
    const val FAILED = android.app.Activity.RESULT_FIRST_USER + 1
    const val SCHEMA = 1

    // These verbs are decoded from the inspected 6.71.15-BETA APK, not guessed.
    val OPERATIONS = listOf("COPY", "MOVE", "RENAME", "CREATE", "DELETE", "ARCHIVE", "EXTRACT")
    private const val PREFS = "mix_tasker_private_v1"
    private const val ENABLED = "tasker_enabled"
    private const val TOKEN = "tasker_token"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).apply()

    /** Token is generated only when a trusted host opens the edit activity. */
    @Synchronized fun issuedToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(TOKEN, null)?.takeIf { it.length >= 40 }?.let { return it }
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.NO_WRAP).also {
            prefs.edit().putString(TOKEN, it).commit()
        }
    }

    fun hasValidToken(context: Context, candidate: String?): Boolean {
        if (candidate == null || candidate.length !in 40..128) return false
        val expected = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TOKEN, null) ?: return false
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), candidate.toByteArray(Charsets.UTF_8))
    }

    fun isTrustedHost(name: String?): Boolean = name == TASKER_PACKAGE || name == LOCALE_PACKAGE
}

data class TaskerRequest(
    val operation: String,
    val source: String,
    val destination: String,
    val name: String,
) {
    fun error(allowVariables: Boolean = false): String? {
        if (operation !in TaskerContract.OPERATIONS) return "Unknown operation"
        if (listOf(source, destination, name).any { it.length > 2048 || it.any { ch -> ch == '\u0000' || ch == '\n' || ch == '\r' } })
            return "Invalid input length"
        if (source.isBlank() && operation != "CREATE") return "Source is required"
        if (destination.isBlank() && operation in listOf("COPY", "MOVE", "RENAME", "CREATE", "ARCHIVE", "EXTRACT"))
            return "Destination is required"
        if (!allowVariables && (source.contains("%") || destination.contains("%") || name.contains("%")))
            return "Unresolved Tasker variable; enable host variable replacement"
        return null
    }

    fun toBundle(token: String): Bundle = Bundle().apply {
        putInt("schema", TaskerContract.SCHEMA)
        putString("op", operation)
        putString("src", source)
        putString("dst", destination)
        putString("name", name)
        putString("token", token)
    }
    companion object {
        fun fromBundle(bundle: Bundle?): TaskerRequest? {
            if (bundle == null || !bundle.containsKey("schema") ||
                bundle.getInt("schema", -1) != TaskerContract.SCHEMA) return null
            val operation = bundle.getString("op") ?: return null
            val source = bundle.getString("src") ?: return null
            val destination = bundle.getString("dst") ?: return null
            val name = bundle.getString("name") ?: return null
            if (operation.length > 32) return null
            return TaskerRequest(
                operation.trim().uppercase(java.util.Locale.ROOT),
                source.trim(), destination.trim(), name.trim(),
            )
        }
    }
}
