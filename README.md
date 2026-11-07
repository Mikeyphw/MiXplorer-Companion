# MiXplorer Companion — R0–R1 foundation

Independent **Material 3** companion app and future **Tasker + libxposed API 102** integration. It contains no MiXplorer source or APK bytes.

## Current functionality

- A working Android settings shell with Material 3 components and dynamic color.
- Package detection for `com.mixplorer.beta`, `com.mixplorer`, and `com.mixplorer.silver`.
- Version-aware warning: APK evidence exists for MiXplorer **6.71.15-BETA** (`26090412`) but **runtime hooks are unverified**.
- Registered modern LSPosed API 102 entry class, scope and module property metadata. **Zero runtime hooks installed.**
- Tasker capability catalog prepared. **Zero Tasker actions advertised or dispatched.**
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
- R1 project contracts: **runnable offline**, hosted Android compile: **pending**.
- LSPosed module binary metadata: **pending hosted APK verification**.
- MiXplorer runtime UI hooks: **not implemented**.
- Tasker integration: **not implemented**.

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
