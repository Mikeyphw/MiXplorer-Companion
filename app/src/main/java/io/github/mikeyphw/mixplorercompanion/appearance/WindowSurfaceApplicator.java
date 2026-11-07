package io.github.mikeyphw.mixplorercompanion.appearance;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Window;
import android.widget.PopupWindow;

/** Tiny opt-in theme adjustments for recognized windows, never dialogs' content. */
public final class WindowSurfaceApplicator {
    private WindowSurfaceApplicator() { }

    public static boolean styleDialog(Dialog dialog) {
        if (dialog == null) return false;
        // Unknown custom dialogs (including MiXplorer file-operation UI) are untouched.
        String type = dialog.getClass().getName();
        if (!"android.app.AlertDialog".equals(type)
                && !"androidx.appcompat.app.AlertDialog".equals(type)) return false;
        Window window = dialog.getWindow();
        if (window == null) return false;
        View decor = window.getDecorView();
        if (decor == null || decor.getBackground() == null) return false;
        decor.setBackgroundTintList(ColorStateList.valueOf(
                AppearancePalette.overlay(dialog.getContext())));
        return true;
    }

    public static boolean styleMenu(PopupWindow popup) {
        if (popup == null || popup.getContentView() == null) return false;
        Drawable current = popup.getBackground();
        // Preserve custom drawables and window geometry; no menu-item manipulation.
        if (!(current instanceof ColorDrawable) && !(current instanceof GradientDrawable)) return false;
        Drawable tinted = current.mutate();
        tinted.setTintList(ColorStateList.valueOf(
                AppearancePalette.overlay(popup.getContentView().getContext())));
        popup.setBackgroundDrawable(tinted);
        return true;
    }
}
