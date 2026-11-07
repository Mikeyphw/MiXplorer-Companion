# R2D — Physical-device runtime qualification (not yet accepted)

**Source base:** merged R2Q, `18e8f0cee9d3300de617d32cda52a97776f80d6c`.
**Only target:** `com.mixplorer.beta` versionCode `26090412` (6.71.15-BETA).
R2D adds diagnosis and a safe probe. It does not add file-management or Tasker hooks.
A green GitHub Actions job cannot verify LSPosed injection or physical-device UI.

## Install and preflight

1. Merge the R2D PR **only when** hosted source, Android and instrumentation jobs pass.
2. Download the `mixplorer-companion-r1-debug` APK from the source-matched successful
   Actions run and update the **companion only** (versionCode 3). Do not modify MiXplorer.
3. Confirm Vector/LSPosed is enabled and the companion module is scoped only to Beta.
   Do not scope to stable or Silver for R2D acceptance.
4. From Termux, in `~/Code/MiXplorer-Companion`, run:

   ```zsh
   python3 -B tools/r2d_runtime_probe.py --serial localhost:5555
   ```

   This uses read-only ADB queries. It refuses an unsupported or missing beta,
   an older companion, and an Android emulator. It prints `R2D_PREFLIGHT_PASS`
   **only** for matching package versions; it always prints
   `R2D_RUNTIME_INCONCLUSIVE` because it cannot establish visual hook success.

## Observe the opt-in hook

5. Open Companion and confirm the **Runtime qualification** card reports the
   expected beta, service connection, and `NOT VERIFIED`.
6. With the feature **off**, observe MiXplorer's original interface and navigation.
   Use the Companion to turn on **Enable Material You experiment**, and enable
   system bars then tab surfaces one at a time. Relaunch MiXplorer using normal
   Android UI when convenient. Do not clear app data or caches.
7. Run:

   ```zsh
   python3 -B tools/r2d_runtime_probe.py --serial localhost:5555 --read-logs \
     --output "$HOME/mixplorer-r2d-observation.txt"
   ```

   Log collection is opt-in, filtered to the companion's single tag and writes
   **counts only**, not log content, filesystem paths, or screenshot data.
   `R2D_CALLBACK_REACHED_LOG_COUNT` and `R2D_APPEARANCE_REQUESTED_LOG_COUNT`
   corroborate that the callback and request were logged; they **do not** prove
   that any view changed or that the log is from the current launch.
   Framework-specific logs may not appear in logcat at all; no marker is inconclusive.

8. Manually check status bar contrast/insets, tab selection, back/swipe,
   split-pane/file listing, drawer, dialogs, long filenames, light/dark mode,
   and that existing file operations still work normally. **Do not run any
   automated file operations.** Verify no overlapping status/navigation bars.
9. Turn off the experiment, relaunch MiXplorer and verify the original look
   is restored. If trouble occurs, disable the module in Vector and relaunch.

## Acceptance is intentionally external and manual

A valid runtime acceptance requires all of: correct package/version, confirmed
Vector injection/scope, observable opt-in appearance difference, unaffected
navigation, and observed rollback. The probe **never** issues a full runtime
pass and never enables the experiment itself. Do not merge further UI hooks
based solely on CI or `R2D_PREFLIGHT_PASS`.

The probe does **not** install/uninstall APKs, root, grant permissions, force-stop
any package, clear data/logcat, capture screenshots, read user file contents,
or write to the Android device. The optional receipt is created only on Termux.
