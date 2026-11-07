package io.github.mikeyphw.mixplorercompanion.lsposed;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.mikeyphw.mixplorercompanion.appearance.AppearanceContract;
import io.github.mikeyphw.mixplorercompanion.appearance.ThemeApplicator;

/** R2: opt-in, beta-version-pinned appearance only. Never hooks file operations. */
public final class ModuleEntry extends XposedModule {
    private static final String TAG = "MiXCompanionR2";

    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "R2 module loaded, appearance default OFF");
    }

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!AppearanceContract.BETA_PACKAGE.equals(param.getPackageName())) return;
        try {
            Class<?> browse = Class.forName(AppearanceContract.BROWSE_ACTIVITY,
                    false, param.getClassLoader());
            Method onResume = browse.getDeclaredMethod("onResume");
            hook(onResume).intercept(chain -> {
                Object result = chain.proceed(); // preserve application behavior first
                if (chain.getThisObject() instanceof Activity) {
                    try { onBrowserResumed((Activity) chain.getThisObject()); }
                    catch (Throwable error) {
                        // Never crash MiXplorer as a result of appearance code.
                        log(Log.WARN, TAG, "Appearance skipped after error", error);
                    }
                }
                return result;
            });
            log(Log.INFO, TAG, "Beta BrowseActivity lifecycle hook installed");
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Unrecognized beta layout: hook not installed", error);
        }
    }

    private void onBrowserResumed(Activity activity) {
        if (!AppearanceContract.BROWSE_ACTIVITY.equals(activity.getClass().getName())) return;
        String packageName = activity.getPackageName();
        PackageInfo info;
        try { info = activity.getPackageManager().getPackageInfo(packageName, 0); }
        catch (Exception ignored) { return; }
        long code;
        if (Build.VERSION.SDK_INT >= 28) code = info.getLongVersionCode();
        else code = info.versionCode;
        if (!AppearanceContract.supports(packageName, code)) return;
        SharedPreferences preferences;
        try { preferences = getRemotePreferences(AppearanceContract.GROUP); }
        catch (RuntimeException | LinkageError ignored) { return; }
        if (!preferences.getBoolean(AppearanceContract.ENABLED, false)) return;
        boolean bars = preferences.getBoolean(AppearanceContract.SYSTEM_BARS, true);
        boolean tabs = preferences.getBoolean(AppearanceContract.TAB_SURFACE, true);
        ThemeApplicator.apply(activity, bars, tabs);
    }
}
