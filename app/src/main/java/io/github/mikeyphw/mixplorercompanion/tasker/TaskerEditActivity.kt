package io.github.mikeyphw.mixplorercompanion.tasker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Tasker Plugin -> Configure; no file operation is dispatched by the editor. */
class TaskerEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val caller = callingPackage ?: callingActivity?.packageName
        if (intent.action != TaskerContract.EDIT || !TaskerContract.isTrustedHost(caller)) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val prior = try { TaskerRequest.fromBundle(intent.getBundleExtra(TaskerContract.BUNDLE)) }
                    catch (_: RuntimeException) { null }
        val hostCapabilities = try { intent.getIntExtra(TaskerContract.HOST_CAPABILITIES, 0) }
                               catch (_: RuntimeException) { 0 }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val px = (16 * resources.displayMetrics.density).toInt()
            setPadding(px, px, px, px)
        }
        fun heading(text: String) { root.addView(TextView(this).apply { this.text = text; textSize = 16f }) }
        heading("MiXplorer Beta · Tasker action")
        heading("Requires the exact Beta version and Tasker actions enabled in Companion. A sent broadcast is not proof that the file operation completed.")
        val op = Spinner(this)
        op.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, TaskerContract.OPERATIONS)
        op.setSelection(TaskerContract.OPERATIONS.indexOf(prior?.operation).coerceAtLeast(0))
        root.addView(op)
        fun field(label: String, value: String): EditText = EditText(this).apply {
            hint = label
            setSingleLine(true)
            setText(value)
            root.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val source = field("Source path / URI", prior?.source.orEmpty())
        val dest = field("Destination path / URI", prior?.destination.orEmpty())
        val name = field("Optional name", prior?.name.orEmpty())
        heading("Use only test files for initial acceptance. DELETE and MOVE change real files; no automatic completion receipt is available.")
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(Button(this).apply {
            text = "Cancel"
            setOnClickListener { setResult(Activity.RESULT_CANCELED); finish() }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(Button(this).apply {
            text = "Save to Tasker"
            setOnClickListener {
                val request = TaskerRequest(
                    op.selectedItem.toString(),
                    source.text.toString().trim(), dest.text.toString().trim(), name.text.toString().trim()
                )
                val allowsVariables = (hostCapabilities and TaskerContract.CAP_REPLACE) != 0
                val error = request.error(allowVariables = allowsVariables)
                if (error != null) {
                    Toast.makeText(this@TaskerEditActivity, error, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val token = TaskerContract.issuedToken(this@TaskerEditActivity)
                val bundle = request.toBundle(token)
                // Host replaces only user-editable text values, never the capability token.
                if (allowsVariables) {
                    bundle.putString(TaskerContract.REPLACE_KEYS, "src dst name")
                }
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra(TaskerContract.BUNDLE, bundle)
                    putExtra(TaskerContract.BLURB, "MiXplorer Beta: ${request.operation} (dispatch only)")
                    if ((hostCapabilities and TaskerContract.CAP_RELEVANT) != 0) {
                        putExtra(TaskerContract.RELEVANT_VARIABLES, arrayOf(
                            "%mixstatus\nBridge status\nDispatch-only or refusal; not file completion",
                            "%mixaction\nAction\nOperation sent to MiXplorer (if dispatched)",
                            "%errmsg\nError message\nOnly present when plugin returns a failure"
                        ))
                    }
                    // Tasker waits for receiver result, NOT MiXplorer file completion.
                    if ((hostCapabilities and TaskerContract.CAP_SYNC) != 0) {
                        putExtra(TaskerContract.REQUEST_TIMEOUT, 3000)
                    }
                })
                finish()
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(controls)
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
