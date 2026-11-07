# R2A CI HF01 - Android 37 toolchain migration

Source: PR #2, head `83d1a4f6880b297e7eb6e32cb643525a49178afa`.

Failure in hosted run 42397708983: `:app:checkDebugAarMetadata` required `compileSdk >= 37` because `io.github.libxposed:service:102.0.0` and its `interface` dependency require API 37; project compiled SDK 35 and used AGP 8.9.2.

Change together:
- `compileSdk = 37`, keeping `targetSdk = 35` and `minSdk = 26` unchanged.
- AGP 9.2.1, compatible with API 37 and Gradle 9.4.1; Java 17 unchanged.
- Built-in Kotlin replacing obsolete `org.jetbrains.kotlin.android` plugin and `android.kotlinOptions`.
- Hosted runner installs platform Android 37 and Build Tools 36.0.0; uses Gradle 9.4.1. Wrapper version updated for parity.
- R2A source-contract tests and APK verification remain required; no changes to hooking behavior or proprietary APK files.

Hosted compilation, APK verification, and device runtime acceptance MUST be checked after applying. These are not provided by local static checks.

References: https://developer.android.com/build/releases/agp-9-2-0-release-notes and https://developer.android.com/build/migrate-to-built-in-kotlin
