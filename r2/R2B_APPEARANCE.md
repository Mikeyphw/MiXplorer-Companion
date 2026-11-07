# R2B — Opt-in Material You chrome, alert and menu surfaces

**Status: SOURCE TESTS ONLY; hosted Android compilation and physical appearance acceptance PENDING.**

R2B builds strictly on the device-tested R2D commit `8cf66cf23dc02d7c14b622d6dd613504f081b5b2`. The R2D diagnostics remain unchanged. Never claim runtime acceptance from APK packaging, emulator tests, or hook marker counts alone.

## Appearance scope

- Existing R2A master switch remains OFF by default. The original system-bar and tab options stay as previously configured.
- Four **new** independently controlled switches default **OFF**: **Toolbar surface**, **Navigation drawer surface**, **Dialog surface**, **Popup menu surface**. The companion disables them unless Vector/LSPosed service is connected and **MiXplorer Beta 6.71.15 / versionCode 26090412** is installed.
- Toolbar: supported Android/AndroidX/Material `Toolbar` backgrounds only, and only if such widgets occur in the inspected BrowseActivity's view tree. MiXplorer custom toolbar types are not guessed. No icon/foreground recoloring or toolbar replacement.
- Navigation: `com.mixplorer.widgets.MiDrawer` background only when it already has one. No drawer gestures, navigation events or list contents modified.
- Alert dialogs: platform or AndroidX/AppCompat alert classes only; existing decor background required. Unknown/custom operation dialogs are intentionally skipped.
- Popup menus: `PopupWindow` with existing `ColorDrawable` or `GradientDrawable` background only. No menu items are read or altered. No change to layout, touch targets or callbacks.
- Color: Android 12+ system neutral shades with light/dark fallback. On Android 16, actual system-bar edge-to-edge may be enforced by MiXplorer/Android; this change does not override insets.
- Chrome scan is capped at **1,600 views**, makes at most two layout passes and skips file lists, pagers, recyclers and web views. No persistent observers.

**NO file operations, Tasker actions, storage permissions, root commands, APK patching, or stable/Silver hooks.** All new behavior is best-effort, guarded by both package + version and the remote master and feature switches.

## CI versus device

The existing GitHub Actions workflow compiles/tests/lints under AGP 9.2.1, Gradle 9.4.1, compileSdk 37.0, and runs the Cinderbox-proven API 36 Pixel 6 emulator. The emulator has no MiXplorer/Vector and asserts that *all seven* appearance switches are disabled, including the four R2B controls. Test count is still exactly two. Apk verification checks new R2B marker strings are packaged, not that the hooks executed.

R2B does **not** prove that the exact toolbar, popup, or dialog widgets are present in real MiXplorer. Skips are expected if its implementation differs; manual testing distinguishes an absent target from an actual behavior regression.

## Physical acceptance and Rollback

1. Install the hosted debug Companion APK (versionCode **4**) after green CI. Do not install a replacement or modified MiXplorer APK. Keep Vector scope limited to `com.mixplorer.beta`.
2. In Companion, leave R2B switches OFF, confirm the R2D-tested baseline, record existing appearance privately.
3. Enable **one R2B switch at a time** with the master experiment ON. Use **force-stop/relaunch** between states to avoid mixing previously tinted views. Verify toolbar/drawer, a simple non-destructive alert, and a harmless popup menu as appropriate. Do not start file copying/deleting tests to qualify appearance.
4. Inspect tab selection, drawer gestures, back navigation, file display, contrast, Android 16 status/navigation-bar content, dialogs, long filenames, and light/dark modes. Confirm nothing obstructs touch targets or file browsing.
5. **Rollback:** turn each R2B switch OFF, then the master OFF, **force-stop/relaunch** MiXplorer. If the app does not reopen normally, disable the module in Vector, then relaunch. Confirm original appearance returns. Do not clear MiXplorer storage or data.
6. Run the existing read-only `python3 -B tools/r2d_runtime_probe.py --serial localhost:5555 --read-logs` to corroborate hook entry. `R2D_RUNTIME_INCONCLUSIVE` is expected; it does not establish R2B visual success. Screenshots should be inspected manually, not uploaded automatically.

For now, **R2B device acceptance is PENDING**, even when source tests and CI pass.

## Merge order

PR #4 (R2D) is still open at build time. This overlay is pinned to its exact head. The installer branches from the R2D commit and opens R2B against the R2D branch if not merged, or against `main` if R2D is an ancestor of `main`. Merge R2D first and let the R2B PR target `main` once R2D lands.
