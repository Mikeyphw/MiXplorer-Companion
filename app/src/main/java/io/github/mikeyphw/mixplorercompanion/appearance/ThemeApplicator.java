package io.github.mikeyphw.mixplorercompanion.appearance;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;

import java.util.ArrayDeque;

/** Small, reversible-by-process-restart look-and-feel experiment; never touches file operations. */
public final class ThemeApplicator {
    private ThemeApplicator() { }

    public static void apply(Activity activity, boolean systemBars, boolean tabSurface) {
        final boolean dark = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int surface = systemSurface(activity.getResources(), dark);
        if (systemBars) colorSystemBars(activity.getWindow(), surface, dark);
        if (tabSurface) {
            View decor = activity.getWindow().getDecorView();
            // Wait until MiXplorer has had a chance to attach its custom tab bar.
            decor.post(() -> {
                try { styleTabBar(decor, surface); }
                catch (RuntimeException ignored) { /* An unrecognized UI must remain usable. */ }
            });
        }
    }

    private static int systemSurface(Resources res, boolean dark) {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                return res.getColor(dark ? android.R.color.system_neutral1_900
                        : android.R.color.system_neutral1_50, null);
            } catch (Resources.NotFoundException ignored) { }
        }
        return dark ? Color.rgb(28, 27, 31) : Color.rgb(250, 249, 252);
    }

    private static void colorSystemBars(Window window, int surface, boolean dark) {
        if (window == null) return;
        // Android 15+ may enforce edge-to-edge behavior; these calls are best-effort.
        window.setStatusBarColor(surface);
        window.setNavigationBarColor(surface);
        if (Build.VERSION.SDK_INT >= 29) window.setNavigationBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(dark ? 0 : mask, mask);
            }
        } else {
            View decor = window.getDecorView();
            int mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | (Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
            int before = decor.getSystemUiVisibility();
            decor.setSystemUiVisibility(dark ? (before & ~mask) : (before | mask));
        }
    }

    private static void styleTabBar(View root, int surface) {
        ArrayDeque<View> pending = new ArrayDeque<>();
        pending.add(root);
        int scanned = 0;
        while (!pending.isEmpty() && scanned++ < 3000) {
            View view = pending.removeFirst();
            if (AppearanceContract.TAB_BAR.equals(view.getClass().getName())) {
                Drawable background = view.getBackground();
                if (background != null) {
                    // Tint the existing drawable to preserve MiXplorer's custom state/ripple.
                    view.setBackgroundTintList(ColorStateList.valueOf(surface));
                } else {
                    view.setBackgroundColor(surface);
                }
                view.setElevation(view.getResources().getDisplayMetrics().density * 3f);
                continue;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); ++i) {
                    View child = group.getChildAt(i);
                    if (child != null) pending.addLast(child);
                }
            }
        }
    }
}
