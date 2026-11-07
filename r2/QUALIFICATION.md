# R2Q: hosted UI instrumentation and physical-device acceptance

This phase adds an automated **companion UI** smoke test on an Android 16 (API 36)
emulator, and a **non-destructive physical-device probe**. The emulator does not
have MiXplorer or Vector/LSPosed installed. It cannot verify the hook itself.

## CI

The `instrumentation` job runs after the normal Android build. It uses the exact hosted emulator configuration from `Mikeyphw/Cinderbox-Companion` successful run `42400362142` (`ubuntu-24.04`, pinned ReactiveCircus action, API36, Google APIs, x86_64, Pixel 6, 2 cores, 2048M RAM, 3072M disk). It requires a real `/dev/kvm` device, prefetches the image, and refuses software fallback or wrong emulator identity. It uses the
Cinderbox-proven API36 Google APIs x86_64 Pixel 6 hosted emulator runner, checks that the companion starts and that R2A appearance
controls stay disabled when the target MiXplorer Beta and LSPosed service are
absent, and requires the real instrumentation runner to report exactly **two passing tests** before writing `R2Q_HOSTED_EMULATOR_UI_PASS`. Any zero-test, incomplete, failed, non-emulator, or unmatched result refuses success. It uploads a source-bound receipt and instrumentation output even on failure.
Gradle runs **only** in GitHub Actions. The standalone app remains targetSdk 35;
its compileSdk 37.0 and libxposed service dependency are unchanged. The APK
version is now 2 / `0.2.0-r2q` for reliable upgrade detection.

## Actual device (requires test APK installed by the user)

1. Download the `mixplorer-companion-r1-debug` APK from a **successful** GitHub
   Actions run for the R2Q PR; install it with Android's installer or your usual
   ADB installation workflow. This is the companion APK, NOT a modified MiXplorer.
2. Confirm that your device has MiXplorer Beta 6.71.15 (versionCode `26090412`)
   installed. Unrecognized Beta builds must not be hooked. The exact version is
   checked by the probe.
3. In Vector/LSPosed, enable the MiXplorer Companion module with the Beta package
   in scope. Launch the companion. Confirm the service connects and the opt-in
   controls are available. Leave the feature off for the baseline observation.
4. In Termux with ADB connected at `localhost:5555`, run:

   ```zsh
   python3 -B tools/r2_device_probe.py --serial localhost:5555
   ```

   This checks both installed package versions and launches **only** the companion.
   It does not alter MiXplorer data or Android system settings. To test MiXplorer
   launch explicitly, run the same command with `--launch-beta`; existing
   MiXplorer sessions are not force-stopped.
5. In the companion, enable the R2A appearance experiment and system-bar/tab
   surface toggles. Manually relaunch MiXplorer when convenient. Compare screenshots
   before/after, check tab selection, navigation, status/navigation bars, light/dark
   appearance, and whether file operations still work normally. Disable again and
   relaunch to verify recovery. If something goes wrong, disable the module in
   Vector and relaunch MiXplorer.

## Acceptance boundaries

- `R2_PACKAGE_LAUNCH_PASS` means **only** package/version checks and successful
  companion launch. It is never proof that LSPosed injected the hook.
- Logcat may omit Xposed framework logs; a missing log line is inconclusive.
- A proper R2A runtime pass requires the intended opt-in visual changes to be
  observed on the physical device, with successful normal navigation and rollback.
- An emulator passing does not make R2A runtime-qualified.
- No file-operation intents or Tasker automation are enabled in this phase.

## Emulator provenance

Reference: [Cinderbox-Companion run 42400362142](https://github.com/Mikeyphw/Cinderbox-Companion/actions/runs/42400362142), successful hosted API36 deep-capture job. The unrelated R48 real-activity job can fail despite this emulator configuration; R2Q does not claim those tests pass. The fixed emulator configuration is tested only after this PR executes. Source-only checks do not qualify the hosted emulator.
