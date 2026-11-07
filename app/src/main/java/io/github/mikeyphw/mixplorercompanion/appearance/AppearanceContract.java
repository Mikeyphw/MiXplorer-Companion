package io.github.mikeyphw.mixplorercompanion.appearance;

/** Version-pinned R2 contract. Stable/Silver packages are discovery only. */
public final class AppearanceContract {
    public static final String BETA_PACKAGE = "com.mixplorer.beta";
    public static final long BETA_VERSION_CODE = 26090412L;
    public static final String BROWSE_ACTIVITY = "com.mixplorer.activities.BrowseActivity";
    public static final String TAB_BAR = "com.mixplorer.widgets.MiTabBar";
    public static final String GROUP = "mixplorer_appearance";
    public static final String ENABLED = "r2_enabled";
    public static final String SYSTEM_BARS = "r2_system_bars";
    public static final String TAB_SURFACE = "r2_tab_surface";

    private AppearanceContract() { }

    public static boolean supports(String pkg, long versionCode) {
        return BETA_PACKAGE.equals(pkg) && BETA_VERSION_CODE == versionCode;
    }
}
