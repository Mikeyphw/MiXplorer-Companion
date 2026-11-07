package io.github.mikeyphw.mixplorercompanion.appearance;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;

/** Semantic surfaces only. No file-list foreground, geometry, or navigation state. */
public final class AppearancePalette {
    private AppearancePalette() { }

    public static int chrome(Context context) {
        return resolve(context, true);
    }

    public static int overlay(Context context) {
        return resolve(context, false);
    }

    private static int resolve(Context context, boolean chrome) {
        Resources res = context.getResources();
        boolean dark = (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                int id = chrome ? (dark ? android.R.color.system_neutral1_800
                                               : android.R.color.system_neutral1_100)
                                : (dark ? android.R.color.system_neutral1_700
                                        : android.R.color.system_neutral1_50);
                return res.getColor(id, context.getTheme());
            } catch (Resources.NotFoundException | IllegalArgumentException ignored) {
                // Fallback is deliberately independent of any MiXplorer resources.
            }
        }
        if (chrome) return dark ? Color.rgb(42, 41, 46) : Color.rgb(241, 240, 246);
        return dark ? Color.rgb(51, 50, 56) : Color.rgb(252, 251, 255);
    }
}
