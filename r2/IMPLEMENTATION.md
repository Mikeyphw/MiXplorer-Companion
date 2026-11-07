# R2A — Material You appearance integration (beta-only)

Target: MiXplorer 6.71.15-BETA, package `com.mixplorer.beta`, versionCode `26090412`.
Source evidence: user-provided APK static inspection. `BrowseActivity.onResume()` is declared by this build; `MiTabBar` is a distinct view class. This does NOT prove runtime safety.

## Delivered

- Native API-102 `onPackageReady` lifecycle hook with `chain.proceed()` first.
- Runtime package + version fail-closed gate (stable/Silver only detected, never hooked).
- Framework remote preferences for opt-in theme controls (no world-readable app preferences).
- Dynamic Material You neutral surfaces and system-bar contrast on supported API levels.
- Small tab-bar tint experiment; no changes to file lists, navigation behavior, storage APIs or Tasker.
- Companion connection status; controls disabled until framework service is connected.
- No automatic enabling, no invisible root/LSPosed permissions, no APK patching.

## Device acceptance (pending; required for R2 PASS)

1. Build and install the hosted debug APK. Enable the module in Vector/LSPosed and scope it ONLY to MiXplorer Beta.
2. Open companion; confirm framework service reports connected and version 26090412 is detected.
3. Enable R2 appearance and system bars; close/reopen MiXplorer Beta; verify status and navigation content remain visible under Android 16 insets.
4. Enable tab surface tint; verify tab selection, scrolling, swipe navigation, drawer, split view, and dialogs still work.
5. Test light/dark wallpaper dynamic colors, background/foreground, rotation, long filenames, and volume changes.
6. Disable appearance and force-stop/relaunch MiXplorer; verify original appearance restored. If crash, disable module in Vector/LSPosed, then relaunch.
7. Collect logcat and screenshot proof, exact APK digest, framework version, device identity, and action outcomes.

## Known limitations

- On Android 15+ with enforced edge-to-edge, system-bar color methods can be ignored; safe-area behavior remains MiXplorer-owned.
- The theme only changes selected surfaces; it is NOT yet a complete Material 3 file-browser redesign.
- Disable/reconfigure requires MiXplorer process restart for reliable removal of altered view tints.
- Framework remote-preferences behavior must be qualified on the user's Vector installation.
- Runtime acceptance pending; CI compile/source tests are not substitutes.
