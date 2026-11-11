# R3T — Native Tasker/Locale action plugin and MiXplorer Beta bridge

## Scope and evidence

Only the exact installed MiXplorer Beta `com.mixplorer.beta` versionCode `26090412`
(6.71.15-BETA) is allowed. The user-provided APK was statically inspected for
`com.mixplorer.ACTION_TASK`, its exported `com.mixplorer.BroadcastReceiver`, and
its `task`, `src`, `dst`, and `name` extras. **Static inspection does not establish
an operation's runtime semantics or completion.** No proprietary APK or decompiled
source is committed.

R3T implements the standard Locale/Tasker *setting* plugin: a configuration
Activity for `com.twofortyfouram.locale.intent.action.EDIT_SETTING` and a fire
receiver for `com.twofortyfouram.locale.intent.action.FIRE_SETTING`. It does
not use an implicit global MiXplorer broadcast. It is separate from R2 appearance
hooks and does not alter MiXplorer, root grants, file-manager storage, or LSPosed.

## Set up

1. Install a successful R3T GitHub Actions debug APK (companion versionCode **6**).
   Leave the original MiXplorer Beta installation unchanged.
2. Open MiXplorer Companion and turn **Enable Tasker actions** ON. It is **OFF
   by default**. This enables an exported Tasker plugin endpoint, so leave it
   OFF when not needed.
3. Tasker → Tasks → add action → **Plugin → MiXplorer Tasker action** → Configure.
4. Choose one of `COPY`, `MOVE`, `RENAME`, `CREATE`, `DELETE`, `ARCHIVE`, `EXTRACT`.
   Enter the source, destination, and optional name as applicable. Save.
5. Run it against **disposable test files**, beginning with `COPY`; check the
   resulting file independently in MiXplorer. Never test `MOVE` or `DELETE` on
   important files. Actual operation semantics and background restrictions need
   device validation with this exact beta version.

The Tasker configuration editor is available only to Tasker or Locale hosts;
it creates a random, per-install, 256-bit capability token in private app
preferences. Tasker stores the token in its plugin action configuration; it is
not displayed in the Companion UI. The fire receiver checks the token, exact
version, user-enable switch, and syntax before dispatch. Tasker variable
replacement, when advertised by the host, applies only to `src`, `dst`, `name`,
not to the token.

## Output and limitations

After Android accepts `sendBroadcast()`, the plugin returns
`%mixstatus = dispatched_unverified` and `%mixaction = <operation>` to Tasker
in the ordered receiver result. This is **dispatch acknowledgment ONLY**,
not successful file creation, copying, moving, renaming, deletion, or extraction.
Errors return a refusal status such as `refused_automation_disabled`,
`refused_invalid_capability`, `refused_unsupported_beta`, or
`refused_invalid_input`. Tasker must independently verify its expected
file-operation outcome. MiXplorer's `ACTION_TASK` handler has no confirmed
completion callback in the inspected contract.

A supported Tasker host is `net.dinglisch.android.taskerm` or
`com.twofortyfouram.locale`. The receiver additionally checks the reported
broadcast sender on Android 14+ when Android provides one; a valid capability
token remains mandatory regardless of sender identity. Treat copied Tasker
action configurations as credentials.

## Acceptance and CI

- Hosted GitHub Actions: compile, test, lint, APK metadata, and the proven
  Cinderbox-style API36 emulator. The existing 2 UI tests are extended with
  one disabled-by-default UI test and one unauthorized-fire refusal test; the
  hosted runner requires exactly **6 passed tests**.
- Physical-device preflight (read-only):

  ```zsh
  python3 -B tools/r3t_tasker_device_probe.py --serial localhost:5555
  ```

- Real Tasker acceptance: configure a `COPY` using a disposable source file,
  verify the target file and bytes through MiXplorer, then test other operations
  individually with safe fixtures. Record both Tasker dispatch and observed
  filesystem outcome; disable automation afterward. Android may restrict
  background broadcasts. Do not claim completion from the Tasker result.
- Rollback: disable **Enable Tasker actions** in Companion. No stored file is
  touched by rollback, and the R2 appearance feature remains independent.

**State:** source and offline tests are not device acceptance. Hosted CI and
actual Tasker-to-MiXplorer end-to-end acceptance remain mandatory.

## R3T-HF03 protocol diagnostics (versionCode 6)

Source-pinned HF03 adds structured, privacy-safe `MiXTaskerBridge` log markers: `R3T_RECEIVER_ENTERED`, `R3T_REFUSED_<fixed_reason>`, `R3T_DISPATCH_ATTEMPT`, `R3T_DISPATCHED_UNVERIFIED` and `R3T_UNORDERED_RESULT_UNAVAILABLE`. Names contain no file paths, capability values, variables, or file contents. The Tasker editor requests a short synchronous **receiver response** when supported; this is **never** a request to await MiXplorer file completion. Tasker failure code `2` is accompanied by `%errmsg` and `%mixstatus` when the host advertises returned-variable support; if not, log markers remain available. Set **Continue Task After Error** in Tasker to inspect `%err` and `%errmsg` for a refusal.

The bridge keeps its internal operation catalog uppercase but sends a lowercase `task` verb over the MiXplorer broadcast (`copy`, etc.). This matches documented historic MiXplorer `ACTION_TASK` examples, but exact Beta 26090412 behavior remains pending device qualification.

Only `src`, `dst`, and `name` are eligible for Tasker variable replacement. The token is never in that list. The exact package/version check and opt-in switch remain. Missing/unsupported MiXplorer Beta results in a refusal, not a receiver crash. A missing `R3T_DISPATCHED_UNVERIFIED` marker does not itself identify the failure stage; use all markers and a real Tasker execution receipt.

Use the read-only probe with `--read-logs --verify-copy-fixture` for the fixed disposable fixture. `R3T_COPY_BYTES_MATCH_UNATTRIBUTED` means the two files independently hash identically; it cannot prove Tasker or MiXplorer caused the copy. All operations remain physically unqualified until real Tasker execution plus file effects are demonstrated. See `r3/ROADMAP_TASKER.md` for all remaining gates.
