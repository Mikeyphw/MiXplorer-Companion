# R0: uploaded MiXplorer 6.71.15-BETA APK

Read-only ZIP/binary XML/DEX inspection. Source artifact SHA-256 and metadata are pinned in `apk-fingerprint.json`.

- **Package:** `com.mixplorer.beta`, not `com.mixplorer` or Silver.
- **Version:** 6.71.15-BETA, code 26090412.
- **SDK:** min 9, target/compile 37.
- **Entry activity:** `com.mixplorer.activities.BrowseActivity`.
- **Receiver:** `com.mixplorer.BroadcastReceiver` is exported.
- **DEX literal:** `com.mixplorer.ACTION_TASK` is present. It is *not* an intent-filter action in the manifest; its extras, error replies and completion semantics have not been verified.
- **UI classes:** `MiListView`, `MiDrawer`, `MiTabBar`, `MiPager`, `MiPopupItem`, `MiViewPager`, `MiButton` among others.
- **Obfuscation:** many methods are renamed. Do not use fragile, unverified method names.
- **R1 policy:** no UI hooks and no Tasker broadcasts until device acceptance.

## Next hardware/device acceptance

1. Confirm installed package/version through Devtool's canonical Android Operations device manager.
2. Capture top activity and view hierarchy while opening drawers, tabs, settings, and dialogs.
3. Test safe, non-destructive ACTION_TASK operations against temporary files only, recording results separately from dispatched events.
4. Use exact-version hook allowlists and no-op fallbacks before enabling any Material 3 view modifications.
5. Do not modify proprietary APK or redistribute its contents.
