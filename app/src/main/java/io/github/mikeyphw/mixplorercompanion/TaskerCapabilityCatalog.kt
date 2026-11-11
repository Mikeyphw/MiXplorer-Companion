package io.github.mikeyphw.mixplorercompanion

/** The plugin UI is registered; runtime dispatch is opt-in and version gated. */
object TaskerCapabilityCatalog {
    const val PLUGIN_ENABLED = true
    const val DEFAULT_ENABLED = false
    val AVAILABLE_ACTIONS = listOf("COPY", "MOVE", "RENAME", "CREATE", "DELETE", "ARCHIVE", "EXTRACT")
    const val FUTURE_ACTION = "com.mixplorer.ACTION_TASK"
    const val RECEIVER_CLASS = "com.mixplorer.BroadcastReceiver"
}
