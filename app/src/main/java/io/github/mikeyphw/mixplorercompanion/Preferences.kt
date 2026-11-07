package io.github.mikeyphw.mixplorercompanion

import android.content.Context

/** These preferences apply only to the companion in R1; they do not hook MiXplorer. */
class Preferences(context: Context) {
    private val store = context.getSharedPreferences("companion_preferences", Context.MODE_PRIVATE)
    var dynamicColor: Boolean
        get() = store.getBoolean("dynamic_color", true)
        set(value) { store.edit().putBoolean("dynamic_color", value).apply() }
    var darkModeFollowSystem: Boolean
        get() = store.getBoolean("dark_mode_system", true)
        set(value) { store.edit().putBoolean("dark_mode_system", value).apply() }
}
