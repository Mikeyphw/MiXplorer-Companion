# MiXplorer Companion — R0–R1 foundation

Independent **Material 3** companion app with an experimental **libxposed API 102** beta-only appearance hook, and an experimental native Tasker plugin. It contains no MiXplorer source or APK bytes.

## Current functionality

- A working Android settings shell with Material 3 components and dynamic color.
- Package detection for `com.mixplorer.beta`, `com.mixplorer`, and `com.mixplorer.silver`.
- Version-aware warning: APK evidence exists for MiXplorer **6.71.15-BETA** (`26090412`) but **runtime hooks are unverified**.
- Modern LSPosed API 102 Beta `BrowseActivity.onResume` appearance hook; default off and not yet physical-device-qualified.
- Native Tasker/Locale plugin for Beta 26090412, default OFF. Dispatch acknowledgment is not file-operation completion.
- Devtool native Android runner, source contracts and hosted GitHub Actions Gradle build.

## Why the beta package matters

The user-supplied APK manifest declares `com.mixplorer.beta`, target SDK 37, and exposes `com.mixplorer.BroadcastReceiver`. Its DEX includes the string `com.mixplorer.ACTION_TASK`, but this is **not** a tested intent contract.

## How to build

All Gradle compile/test/lint runs are **hosted by GitHub Actions**. Download the debug APK from workflow artifacts after CI succeeds. Device tests must be performed separately and are not implied by CI success.

Local Devtool discovery:

```sh
devtool -r "$HOME/Code/MiXplorer-Companion" --target companion inspect
devtool -r "$HOME/Code/MiXplorer-Companion" --target companion android capabilities
```

## Stage gates

- R0 static inspection: **PASS (source only)**.
- R1/R2Q project and hosted build contracts: **passed in PR CI**; physical runtime acceptance pending.
- LSPosed module binary metadata: **verified by hosted APK checks**.
- MiXplorer runtime UI hooks: **R2A experimental opt-in appearance only; unqualified on physical hardware**.
- Tasker integration: **R3T implemented; physical operation acceptance unverified.**

Never infer a completed file operation merely because MiXplorer accepts a broadcast. Before enabling Tasker actions, document exact extras, Android background behavior, and a completion verification mechanism.

## Developer notes

The custom UI class names in the fingerprint are candidates for later investigation, not stable hook targets. The beta uses obfuscated symbols and hooks must be version gated with automatic fallback. Source-only inspection does not prove runtime compatibility.


## R2A: opt-in appearance hooks (beta-only)

R2A adds a strict MiXplorer Beta `6.71.15-BETA` / `26090412` gate for an
`onResume` hook. The default is disabled, and controls are unavailable until the
modern LSPosed service is connected. Toggle dynamic system bars or custom tab
surface separately; force-stop/relaunch MiXplorer after changes. This is an
experimental Material You foundation, **not** a full UI replacement, and on-
device acceptance remains mandatory. See `r2/IMPLEMENTATION.md`. Stable and
Silver are listed only for discovery; R2 does not hook them.

## R2Q qualification

Cinderbox-proven API36 hosted Android UI instrumentation and physical-device acceptance procedure: see `r2/QUALIFICATION.md`. The emulator cannot establish Vector/LSPosed hook success.

## R2D: physical-device evidence and safe recovery

Adds a versioned debug build (versionCode 3), source-bound module lifecycle markers,
a visible in-app **NOT VERIFIED** qualification state, and a non-destructive
`tools/r2d_runtime_probe.py`. Only `com.mixplorer.beta` versionCode 26090412
is considered eligible. Neither source tests nor the hosted API36 emulator certify
physical Vector/LSPosed injection or the visual result. See
`r2/DEVICE_ACCEPTANCE_R2D.md` for explicit opt-in and rollback steps.

## R2B — Opt-in Material You chrome and transient surfaces

Four additional independently opt-in, default-OFF appearance controls target
recognized toolbars, MiDrawer backgrounds, stock alert dialogs, and supported
PopupWindow menu backgrounds for the exact inspected Beta version only.
Unsupported custom views are skipped. No file operations or Tasker hooks are
introduced. Source/CI success does not establish device styling or rollback.
See `r2/R2B_APPEARANCE.md` for scoped behavior and acceptance.

## R3T: native Tasker plugin (opt-in)

R3T adds a Locale/Tasker action configuration editor and a guarded explicit
`com.mixplorer.ACTION_TASK` dispatch bridge for inspected MiXplorer Beta
6.71.15 (`26090412`), **default OFF**. The editor supports COPY, MOVE, RENAME,
CREATE, DELETE, ARCHIVE, and EXTRACT with configured source/destination/name.
A Tasker result of `dispatched_unverified` confirms only that Android accepted
broadcast dispatch; it **does not verify an operation completed**. Actual
file results must be checked with disposable fixtures on the user's device.
No new LSPosed hooks are required; appearance is independent.
See [`r3/TASKER.md`](r3/TASKER.md).
