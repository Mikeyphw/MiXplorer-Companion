package io.github.mikeyphw.mixplorercompanion

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import io.github.mikeyphw.mixplorercompanion.tasker.TaskerContract
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-only acceptance: MiXplorer and Vector are intentionally absent. */
@RunWith(AndroidJUnit4::class)
class CompanionUiSmokeTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test fun launchesAndDisplaysPackageDiscovery() {
        onView(withText("MiXplorer Companion")).check(matches(isDisplayed()))
        onView(withText("Package compatibility")).check(matches(isDisplayed()))
        // Runtime acceptance must never be presented as qualified on this empty emulator.
        onView(withText("R2B visual acceptance: NOT VERIFIED BY APP"))
            .check(matches(withText("R2B visual acceptance: NOT VERIFIED BY APP")))
    }

    @Test fun untrustedPluginFireWithoutTokenCannotDispatch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)
        var result = Int.MIN_VALUE
        val request = Intent(TaskerContract.FIRE).apply {
            component = ComponentName(context, io.github.mikeyphw.mixplorercompanion.tasker.TaskerFireReceiver::class.java)
        }
        context.sendOrderedBroadcast(request, null, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                result = resultCode
                latch.countDown()
            }
        }, null, 0, null, null)
        assertTrue("Plugin fire did not respond", latch.await(10, TimeUnit.SECONDS))
        assertEquals(TaskerContract.FAILED, result)
    }

    @Test fun taskerIsDisabledUntilUserExplicitlyEnablesIt() {
        // The Tasker section is below the fold on API 36 Pixel 6. Espresso's
        // isDisplayed() requires a non-empty screen rectangle, not merely
        // View.VISIBLE; scroll the real ScrollView before asserting.
        onView(withText("Tasker automation"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withContentDescription("Enable Tasker actions"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(not(isChecked())))
    }

    @Test fun refusesAppearanceControlsWhenMiXplorerAndVectorAreAbsent() {
        onView(withContentDescription("Enable Material You experiment"))
            .check(matches(not(isEnabled())))
        onView(withContentDescription("System bar surfaces"))
            .check(matches(not(isEnabled())))
        onView(withContentDescription("Tab bar surface"))
            .check(matches(not(isEnabled())))
        onView(withContentDescription("Toolbar surface"))
            .check(matches(not(isEnabled())))
        onView(withContentDescription("Navigation drawer surface"))
            .check(matches(not(isEnabled())))
        onView(withContentDescription("Dialog surface"))
            .check(matches(not(isEnabled())))
        onView(withContentDescription("Popup menu surface"))
            .check(matches(not(isEnabled())))
    }

    @Test fun malformedTaskerBundlesHaveNoRequest() {
        assertEquals(null, io.github.mikeyphw.mixplorercompanion.tasker.TaskerRequest.fromBundle(null))
        assertEquals(null, io.github.mikeyphw.mixplorercompanion.tasker.TaskerRequest.fromBundle(Bundle()))
        val malformed = Bundle().apply {
            putInt("schema", TaskerContract.SCHEMA)
            putString("op", "COPY")
            putString("src", "/sdcard/Download/disposable")
            // No destination, name or token. Never create a dispatchable request.
        }
        assertEquals(null, io.github.mikeyphw.mixplorercompanion.tasker.TaskerRequest.fromBundle(malformed))
    }

    @Test fun unauthorizedFireWithAutomationEnabledIsRefused() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previous = TaskerContract.isEnabled(context)
        try {
            TaskerContract.setEnabled(context, true)
            val latch = CountDownLatch(1)
            var reply = Int.MIN_VALUE
            val candidate = Intent(TaskerContract.FIRE).apply {
                component = ComponentName(context, io.github.mikeyphw.mixplorercompanion.tasker.TaskerFireReceiver::class.java)
                putExtra(TaskerContract.BUNDLE, Bundle().apply {
                    putInt("schema", TaskerContract.SCHEMA)
                    putString("op", "COPY")
                    putString("src", "/sdcard/Download/test-source")
                    putString("dst", "/sdcard/Download/test-target")
                    putString("name", "")
                    putString("token", "not-a-valid-secret")
                })
            }
            context.sendOrderedBroadcast(candidate, null, object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    reply = resultCode
                    latch.countDown()
                }
            }, null, 0, null, null)
            assertTrue("Unauthorized FIRE had no response", latch.await(10, TimeUnit.SECONDS))
            assertEquals(TaskerContract.FAILED, reply)
        } finally {
            TaskerContract.setEnabled(context, previous)
        }
    }
}
