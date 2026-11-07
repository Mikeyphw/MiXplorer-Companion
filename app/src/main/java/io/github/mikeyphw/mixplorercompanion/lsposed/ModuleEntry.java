package io.github.mikeyphw.mixplorercompanion.lsposed;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.PopupWindow;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.mikeyphw.mixplorercompanion.appearance.AppearanceContract;
import io.github.mikeyphw.mixplorercompanion.appearance.ThemeApplicator;
import io.github.mikeyphw.mixplorercompanion.appearance.WindowSurfaceApplicator;

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
            log(Log.INFO, TAG, "R2D_HOOK_INSTALLED");
            // These hooks are registered only in the Beta process. Each callback
            // independently rechecks package, exact version, master opt-in,
            // and its own default-OFF switch before touching any window.
            installWindowHooks();
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Unrecognized beta layout: hook not installed", error);
        }
    }

    private void installWindowHooks() {
        try {
            Method show = Dialog.class.getMethod("show");
            hook(show).intercept(chain -> {
                Object result = chain.proceed();
                if (chain.getThisObject() instanceof Dialog) {
                    try { onDialogShown((Dialog) chain.getThisObject()); }
                    catch (Throwable error) { log(Log.WARN, TAG, "R2B dialog styling skipped", error); }
                }
                return result;
            });
            log(Log.INFO, TAG, "R2B_DIALOG_HOOK_INSTALLED");
        } catch (Throwable error) {
            log(Log.WARN, TAG, "R2B dialog hook unavailable", error);
        }
        // PopupWindow can dispatch through different overloads; style only after show.
        try {
            hookPopup(PopupWindow.class.getMethod("showAsDropDown", View.class));
            hookPopup(PopupWindow.class.getMethod("showAsDropDown",
                    View.class, int.class, int.class));
            hookPopup(PopupWindow.class.getMethod("showAsDropDown",
                    View.class, int.class, int.class, int.class));
            hookPopup(PopupWindow.class.getMethod("showAtLocation",
                    View.class, int.class, int.class, int.class));
            log(Log.INFO, TAG, "R2B_MENU_HOOKS_INSTALLED");
        } catch (Throwable error) {
            log(Log.WARN, TAG, "R2B popup hook unavailable", error);
        }
    }

    private void hookPopup(Method method) {
        hook(method).intercept(chain -> {
            Object result = chain.proceed();
            if (chain.getThisObject() instanceof PopupWindow) {
                try { onMenuShown((PopupWindow) chain.getThisObject()); }
                catch (Throwable error) { log(Log.WARN, TAG, "R2B menu styling skipped", error); }
            }
            return result;
        });
    }

    private SharedPreferences qualifiedPreferences(Context context) {
        if (!AppearanceContract.BETA_PACKAGE.equals(context.getPackageName())) return null;
        PackageInfo info;
        try { info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0); }
        catch (Exception ignored) { return null; }
        long code = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
        if (!AppearanceContract.supports(context.getPackageName(), code)) return null;
        try { return getRemotePreferences(AppearanceContract.GROUP); }
        catch (RuntimeException | LinkageError ignored) { return null; }
    }

    private void onDialogShown(Dialog dialog) {
        SharedPreferences preferences = qualifiedPreferences(dialog.getContext());
        if (preferences == null || !preferences.getBoolean(AppearanceContract.ENABLED, false)
                || !preferences.getBoolean(AppearanceContract.DIALOG_SURFACE, false)) return;
        if (WindowSurfaceApplicator.styleDialog(dialog)) {
            // The known dialog background received a tint; visible result remains unverified.
            log(Log.INFO, TAG, "R2B_DIALOG_SURFACE_REQUESTED");
        }
    }

    private void onMenuShown(PopupWindow popup) {
        View content = popup.getContentView();
        if (content == null) return;
        SharedPreferences preferences = qualifiedPreferences(content.getContext());
        if (preferences == null || !preferences.getBoolean(AppearanceContract.ENABLED, false)
                || !preferences.getBoolean(AppearanceContract.MENU_SURFACE, false)) return;
        if (WindowSurfaceApplicator.styleMenu(popup)) {
            log(Log.INFO, TAG, "R2B_MENU_SURFACE_REQUESTED");
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
        // This marker proves entry into the guarded onResume callback, not a visual change.
        log(Log.INFO, TAG, "R2D_CALLBACK_REACHED");
        SharedPreferences preferences;
        try { preferences = getRemotePreferences(AppearanceContract.GROUP); }
        catch (RuntimeException | LinkageError ignored) { return; }
        if (!preferences.getBoolean(AppearanceContract.ENABLED, false)) {
            log(Log.INFO, TAG, "R2D_EXPERIMENT_DISABLED");
            return;
        }
        boolean bars = preferences.getBoolean(AppearanceContract.SYSTEM_BARS, true);
        boolean tabs = preferences.getBoolean(AppearanceContract.TAB_SURFACE, true);
        ThemeApplicator.apply(activity, bars, tabs);
        boolean toolbar = preferences.getBoolean(AppearanceContract.TOOLBAR_SURFACE, false);
        boolean drawer = preferences.getBoolean(AppearanceContract.DRAWER_SURFACE, false);
        if (toolbar || drawer) {
            ThemeApplicator.applyChrome(activity, toolbar, drawer);
            log(Log.INFO, TAG, "R2B_CHROME_REQUESTED");
        }
        // A request was submitted; async view styling and visual acceptance remain unverified.
        log(Log.INFO, TAG, "R2D_APPEARANCE_REQUESTED");
    }
}
