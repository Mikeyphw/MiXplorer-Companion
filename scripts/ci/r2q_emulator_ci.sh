#!/usr/bin/env bash
# Hosted GitHub emulator ONLY. No MiXplorer APK, data or Vector/LSPosed access.
set -euo pipefail
: "${GITHUB_SHA:?}"; : "${GITHUB_RUN_ID:?}"; : "${RUNNER_TEMP:?}"
SERIAL=emulator-5554
OUT="$RUNNER_TEMP/mixplorer-r2q-emulator"
mkdir -p "$OUT"
printf '%s\n' R2Q_HOSTED_EMULATOR_NOT_RUN > "$OUT/result.txt"
[[ "${RUNNER_OS:-}" == Linux && "$(uname -m)" == x86_64 ]] || { echo R2Q_RUNNER_REFUSED >&2; exit 1; }
[[ "$(git rev-parse HEAD)" == "$GITHUB_SHA" ]] || { echo R2Q_SOURCE_DRIFT >&2; exit 1; }
[[ "$(adb -s "$SERIAL" get-state)" == device ]] || { echo R2Q_AVD_NOT_READY >&2; exit 1; }
[[ "$(adb -s "$SERIAL" shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]] || { echo R2Q_PHYSICAL_DEVICE_REFUSED >&2; exit 1; }
[[ "$(adb -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')" == 36 ]] || { echo R2Q_WRONG_API >&2; exit 1; }
# The fixture must be empty: no real MiXplorer and no pre-existing companion.
[[ -z "$(adb -s "$SERIAL" shell pm path com.mixplorer.beta 2>/dev/null | tr -d '\r')" ]] || { echo R2Q_NONEMPTY_BETA_REFUSED >&2; exit 1; }
[[ -z "$(adb -s "$SERIAL" shell pm path io.github.mikeyphw.mixplorercompanion 2>/dev/null | tr -d '\r')" ]] || { echo R2Q_NONFRESH_COMPANION_REFUSED >&2; exit 1; }
shopt -s nullglob
apks=(app/build/outputs/apk/debug/*.apk)
tests=(app/build/outputs/apk/androidTest/debug/*.apk)
[[ ${#apks[@]} -eq 1 && ${#tests[@]} -eq 1 ]] || { echo R2Q_APK_PAIR_REFUSED >&2; exit 1; }
printf 'source_sha=%s\n' "$GITHUB_SHA" > "$OUT/source.txt"
printf 'run_id=%s\n' "$GITHUB_RUN_ID" >> "$OUT/source.txt"
sha256sum "${apks[0]}" "${tests[0]}" >> "$OUT/source.txt"
adb -s "$SERIAL" install -r -t "${apks[0]}" >/dev/null
adb -s "$SERIAL" install -r -t "${tests[0]}" >/dev/null
[[ "$(adb -s "$SERIAL" shell pm path io.github.mikeyphw.mixplorercompanion | tr -d '\r')" == package:* ]] || { echo R2Q_COMPANION_INSTALL_FAILED >&2; exit 1; }
# A failing or empty test run must NEVER become a passing emulator receipt.
if ! adb -s "$SERIAL" shell am instrument -w -r \
  io.github.mikeyphw.mixplorercompanion.test/androidx.test.runner.AndroidJUnitRunner > "$OUT/instrumentation.txt" 2>&1; then
  tail -n 50 "$OUT/instrumentation.txt" >&2
  echo R2Q_INSTRUMENTATION_COMMAND_FAILED >&2
  exit 1
fi
tr -d '\r' < "$OUT/instrumentation.txt" > "$OUT/instrumentation.normalized.txt"
mv -- "$OUT/instrumentation.normalized.txt" "$OUT/instrumentation.txt"
if ! grep -Fxq 'OK (2 tests)' "$OUT/instrumentation.txt" || \
   ! grep -Fxq 'INSTRUMENTATION_CODE: -1' "$OUT/instrumentation.txt"; then
  tail -n 50 "$OUT/instrumentation.txt" >&2
  echo R2Q_INSTRUMENTATION_RESULT_FAILED >&2
  exit 1
fi
if grep -Eiq 'FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED' "$OUT/instrumentation.txt"; then
  echo R2Q_INSTRUMENTATION_NEGATIVE_RESULT >&2
  exit 1
fi
printf '%s\n' R2Q_HOSTED_EMULATOR_UI_PASS > "$OUT/result.txt"
echo R2Q_HOSTED_EMULATOR_UI_PASS
