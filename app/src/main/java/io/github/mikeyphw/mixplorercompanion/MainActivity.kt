package io.github.mikeyphw.mixplorercompanion

import android.os.Bundle
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
            subtitle = "R1 • Devtool foundation"
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
            switch(card, "Dynamic Material You colors", "Applies to this companion only in R1", preferences.dynamicColor) {
                preferences.dynamicColor = it
                recreate()
            }
            switch(card, "Follow system appearance", "Uncheck to use light mode in this companion", preferences.darkModeFollowSystem) {
                preferences.darkModeFollowSystem = it
                recreate()
            }
        }
        section(content, "LSPosed UI integration") { card ->
            label(card, "API 102 entry point registered. No MiXplorer hooks are installed by this release.")
            label(card, "Scoped to: com.mixplorer.beta, com.mixplorer, com.mixplorer.silver")
        }
        section(content, "Tasker automation") { card ->
            label(card, "Action dispatch is intentionally disabled pending on-device contract tests.")
            label(card, "APK evidence: ACTION_TASK is present; completion callbacks are unverified.")
        }
        section(content, "Diagnostics") { card ->
            label(card, "APK inspected: MiXplorer 6.71.15-BETA • versionCode 26090412")
            label(card, "Runtime acceptance: PENDING")
            val button = MaterialButton(this).apply {
                text = "Refresh package detection"
                setOnClickListener { render() }
            }
            card.addView(button)
        }
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
