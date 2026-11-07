package io.github.mikeyphw.mixplorercompanion

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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
}
