package io.github.mikeyphw.mixplorercompanion

import android.os.Bundle
import android.content.SharedPreferences
import android.widget.Toast
import io.github.mikeyphw.mixplorercompanion.appearance.AppearanceContract
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {
    private lateinit var preferences: Preferences
    private val serviceListener = Runnable { runOnUiThread { render() } }

    override fun onStart() {
        super.onStart()
        CompanionApp.addListener(serviceListener)
    }

    override fun onStop() {
        CompanionApp.removeListener(serviceListener)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = Preferences(this)
        AppCompatDelegate.setDefaultNightMode(if (preferences.darkModeFollowSystem) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_NO)
        if (preferences.dynamicColor) DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        render()
    }

    private fun dip(n: Int) = (n * resources.displayMetrics.density + 0.5f).toInt()

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val toolbar = MaterialToolbar(this).apply {
            title = "MiXplorer Companion"
            subtitle = "R2A • Appearance preview"
        }
        root.addView(toolbar)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dip(16), dip(12), dip(16), dip(32))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        section(content, "Package compatibility") { card ->
            val matches = MiXPackageProbe(this).installed()
            val desc = if (matches.isEmpty()) {
                "MiXplorer not detected. Install the beta or another supported package."
            } else matches.joinToString("\n\n") {
                "${it.packageName}\n${it.versionName} (${it.versionCode})\n${it.evidence}"
            }
            label(card, desc)
        }
        section(content, "Appearance") { card ->
            switch(card, "Dynamic Material You colors", "Applies to this companion only", preferences.dynamicColor) {
                preferences.dynamicColor = it
                recreate()
            }
            switch(card, "Follow system appearance", "Uncheck to use light mode in this companion", preferences.darkModeFollowSystem) {
                preferences.darkModeFollowSystem = it
                recreate()
            }
        }
        val beta = MiXPackageProbe(this).installed().firstOrNull { it.packageName == AppearanceContract.BETA_PACKAGE }
        val qualified = beta != null && AppearanceContract.supports(beta.packageName, beta.versionCode)
        val remote = CompanionApp.appearancePreferences()
        val canChange = qualified && remote != null
        section(content, "MiXplorer Beta appearance • R2A") { card ->
            label(card, if (qualified) "Inspected beta 6.71.15 detected." else "Unsupported or missing beta version: appearance is disabled.")
            label(card, if (remote == null) "LSPosed service not connected. Enable the module and open its scope in Vector." else "LSPosed service connected. Changes apply on next MiXplorer launch.")
            remoteSwitch(card, "Enable Material You experiment", "Off by default; only beta version 26090412", remote, AppearanceContract.ENABLED, false, canChange)
            remoteSwitch(card, "System bar surfaces", "Dynamic neutral surfaces and icon contrast (Android 15+ may override)", remote, AppearanceContract.SYSTEM_BARS, true, canChange)
            remoteSwitch(card, "Tab bar surface", "Subtle Material You surface for the existing MiXplorer tabs", remote, AppearanceContract.TAB_SURFACE, true, canChange)
            label(card, "After changing settings, force-stop/relaunch MiXplorer to apply or undo its view styling. Disable the module in Vector for emergency rollback.")
        }
        section(content, "LSPosed UI integration") { card ->
            label(card, "API 102: version-pinned BrowseActivity.onResume hook for the inspected beta only.")
            label(card, "Scope catalog: beta, stable, Silver. Stable/Silver receive no UI hooks in R2.")
        }
        // Diagnostic readiness is deliberately distinct from runtime qualification.
        val experimentEnabled = try {
            remote?.getBoolean(AppearanceContract.ENABLED, false) ?: false
        } catch (_: RuntimeException) { false }
        section(content, "Runtime qualification") { card ->
            label(card, "R2D runtime acceptance: NOT VERIFIED")
            label(card, if (qualified) "Target version: exact inspected Beta match" else "Target version: unsupported or absent")
            label(card, if (remote != null) "Vector/LSPosed service: connected" else "Vector/LSPosed service: disconnected")
            label(card, if (experimentEnabled) "Appearance preference: enabled (not proof of injection)" else "Appearance preference: disabled")
            label(card, "Host CI tests companion UI only. Verify callback, appearance and rollback on your own device before acceptance.")
        }
        section(content, "Tasker automation") { card ->
            label(card, "Action dispatch is intentionally disabled pending on-device contract tests.")
            label(card, "APK evidence: ACTION_TASK is present; completion callbacks are unverified.")
        }
        section(content, "Diagnostics") { card ->
            label(card, "APK inspected: MiXplorer 6.71.15-BETA • versionCode 26090412")
            label(card, "R2D device runtime acceptance: PENDING • CI does not prove injection")
            val button = MaterialButton(this).apply {
                text = "Refresh package detection"
                setOnClickListener { render() }
            }
            card.addView(button)
        }
    }

    private fun remoteSwitch(
        card: LinearLayout, title: String, detail: String,
        remote: SharedPreferences?, key: String, default: Boolean, enabled: Boolean
    ) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val description = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        label(description, title)
        label(description, detail)
        row.addView(description, LinearLayout.LayoutParams(0, -2, 1f))
        val current = try { remote?.getBoolean(key, default) ?: default } catch (_: RuntimeException) { default }
        val control = MaterialSwitch(this).apply {
            isChecked = current
            isEnabled = enabled
            contentDescription = title
            setOnCheckedChangeListener { _, value ->
                val saved = try { remote?.edit()?.putBoolean(key, value)?.commit() == true }
                    catch (_: RuntimeException) { false }
                if (!saved) Toast.makeText(this@MainActivity, "Could not save framework preference", Toast.LENGTH_SHORT).show()
                render()
            }
        }
        row.addView(control, LinearLayout.LayoutParams(-2, -2))
        card.addView(row)
    }

    private fun section(parent: LinearLayout, title: String, content: (LinearLayout) -> Unit) {
        val text = TextView(this).apply {
            this.text = title
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dip(4), dip(16), dip(4), dip(8))
        }
        parent.addView(text)
        val surface = MaterialCardView(this).apply { radius = dip(18).toFloat() }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dip(16), dip(14), dip(16), dip(14))
        }
        content(inner)
        surface.addView(inner)
        parent.addView(surface, LinearLayout.LayoutParams(-1, -2))
    }

    private fun label(parent: LinearLayout, message: String) {
        parent.addView(TextView(this).apply {
            text = message
            textSize = 14f
            setPadding(0, dip(5), 0, dip(5))
            setLineSpacing(dip(2).toFloat(), 1f)
        })
    }

    private fun switch(parent: LinearLayout, title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val description = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        label(description, title)
        label(description, detail)
        row.addView(description, LinearLayout.LayoutParams(0, -2, 1f))
        val control = MaterialSwitch(this).apply {
            isChecked = checked
            contentDescription = title
            setOnCheckedChangeListener { _, value -> onChange(value) }
        }
        row.addView(control, LinearLayout.LayoutParams(-2, -2))
        parent.addView(row)
    }
}
