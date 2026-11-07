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
    // R2B is explicit opt-in per surface; all new switches default false.
    public static final String TOOLBAR_SURFACE = "r2b_toolbar_surface";
    public static final String DRAWER_SURFACE = "r2b_drawer_surface";
    public static final String DIALOG_SURFACE = "r2b_dialog_surface";
    public static final String MENU_SURFACE = "r2b_menu_surface";

    private AppearanceContract() { }

    public static boolean supports(String pkg, long versionCode) {
        return BETA_PACKAGE.equals(pkg) && BETA_VERSION_CODE == versionCode;
    }
}
