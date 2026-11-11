# MiXplorer Companion — native Tasker roadmap (2026-11-10)

## Evidence baseline

PR #6 `feature/r3t-tasker-integration-20261107`, source `e7de7da0ead565b9fbedf650de46b6dbe4794591` before HF03. Main `24dce34088130a77120a9cb1d9402b2d09141bc4`. R3T hosted run `42421929360` passed source contracts, Android build/unit/lint/APK registration and four API36 emulator tests. On the physical Android 16 device, source fixture SHA-256 is `6dd8d3a46c0f710f7470891b173ef4e14422d2b06763dedc80c1a72febfacb36`; target was absent. Old probe counted zero successful-dispatch markers, so reachability/refusal/dispatch/completion remained **unproven**.

Completed: R0 APK-only inspection; R1 companion foundation; R2A beta-only appearance; R2Q hosted API36; R2D physical hook callbacks; R2B appearance merged; R3T Tasker implementation and hosted 4-test baseline. No proprietary MiXplorer binaries in this project.

## R3T-HF03 — protocol and sanitized diagnostics (this overlay; hosted/device gates pending)

- Receiver entry, fixed-reason refusal, attempt, dispatched-unverified and ordering markers, without logging paths, tokens or Tasker variable values.
- Handle missing target package as refusal; strict schema and token checks.
- Request Tasker synchronous *dispatch-result* delivery only if host advertises it. Report `%errmsg` on failure if the host supports return variables; keep `%mixstatus` and `%mixaction` strictly dispatch-only.
- Lowercase the MiXplorer wire-protocol task verb as historically documented; still confirm on this exact Beta build. Only `src dst name` offered for variable replacement. Never replace or log the capability token.
- Offline negative tests and hosted API36 negative tests; no MiXplorer operation is executed in hosted tests.
- Read-only optional disposable COPY hash checks; matched bytes do **not** prove which app performed the copy.
- Gate: hosted CI green and physical Tasker invocation diagnosed. No claim of real operation success.

## R3V — Physical Tasker-to-MiXplorer reachability

Verify plugin discovery, configuration editor and save, default-OFF to ON user consent, Tasker execution, receiver-entry, token check, version check, explicit sendBroadcast attempt, background restrictions, and ordered result. Run only disposable COPY; capture sanitized receipt. Gate: reachability + actual dispatch attempt proven on physical Android 16 device.

## R3C — Actual COPY result

Inspect `ACTION_TASK` and exact Beta 26090412 runtime semantics for source/destination and optional parameters. Compare source and target SHA-256 independently. Do not infer completion from `sendBroadcast()`; if callback unsupported, document that and use independent verification with provenance. Gate: target equals source with timestamp/trigger attribution captured; appropriate verified/unverified status.

## R3X — Each advertised operation separately

COPY: preserve and hash; MOVE: exact target plus source removal; RENAME: exact path/name; CREATE: type/options; DELETE: user confirmation and disposable-only; ARCHIVE: format/content hash; EXTRACT: expected files plus traversal protection. Test overwrite, missing source, invalid target, permission refusal, unsupported URIs and duplicate requests. No production-ready destructive actions before qualification. EXECUTE excluded until a separate security review.

## R3S — Harden security/reliability

Validate caller and host identity, revoke/regenerate tokens, copied/stale configurations, bounded parsers and inputs, controlled variable replacement, beta-only version invalidation, repeat/idempotency behavior, process death, force-stop, background execution, long-running tasks, supported timeouts and cancellation only if MiXplorer confirms them. No broad storage or root privileges, no arbitrary commands and no unverified success.

## R3U — Tasker UX

Operation-aware Material 3 fields, validation, warnings/confirmation for destructive operations, last observable dispatch/result, authorization management, human-readable refusal guidance, safe examples, and output variables only for facts proven by evidence. Preserve R2 appearance settings.

## R3Q — Hosted + physical gate

Offline Python contracts; GitHub-hosted Gradle assemble/test/lint; APK metadata; Cinderbox-style API36 emulator; negative protocol tests; real positive operation tests; light/dark lifecycle; LSPosed/appearance regressions; exact source-commit and APK SHA receipts. CI success alone is not physical acceptance.

## R3F — Release and merge

Update UI/docs, remove stale R1/R2 claims, verify versionCode upgrade, hosted signed/source-pinned artifact with provenance, documented rollback, merge PR #6 only after supported actions pass. Do not mark beta version unknown or execute arbitrary actions. Appearance refinements are post-Tasker work.

## Physical fixture and safety

Only use `/sdcard/Download/MiXTasker-R3T/source/test.txt` and `/sdcard/Download/MiXTasker-R3T/target/test.txt` for initial non-destructive COPY; after installing HF03, Tasker execution must be user-triggered. Run:

```zsh
python3 -B tools/r3t_tasker_device_probe.py --serial localhost:5555 --read-logs --verify-copy-fixture
```

Do not clear logcat, disclose credentials or treat broadcast/receipt as file completion. Disable Tasker automation after tests; R2 appearance remains independent.

## R3T-HF03-HF01 — Historical contract repair (2026-11-10)

The first HF03 installer correctly blocked before apply: five old Python source
assertions still required Companion versionCode 5, the previous version name,
exactly four emulator tests, and fixed literal receiver log messages. The
consolidated HF03-HF01 archive updates the R2B, R2D, R2Q and R3T source
contracts in the same atomic inventory as HF03, while leaving R2 appearance
source and all physical-device state unchanged. The new HF03 regression
contract prevents the old assertions from returning. The clean GitHub source
commit remains e7de7da0ead565b9fbedf650de46b6dbe4794591. HF03 has not
passed hosted Gradle / API36 or physical COPY merely by passing offline tests.
