package io.github.mikeyphw.mixplorercompanion

/** R1 deliberately does not advertise unfinished Tasker actions. */
object TaskerCapabilityCatalog {
    const val PLUGIN_ENABLED = false
    val AVAILABLE_ACTIONS: List<String> = emptyList()
    const val FUTURE_ACTION = "com.mixplorer.ACTION_TASK"
    const val RECEIVER_CLASS = "com.mixplorer.BroadcastReceiver"
}
