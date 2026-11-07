package io.github.mikeyphw.mixplorercompanion.lsposed;

import android.util.Log;
import io.github.libxposed.api.XposedModule;

/** Registration-only entry point: R1 installs no hooks and changes no MiXplorer behavior. */
public final class ModuleEntry extends XposedModule {
    private static final String TAG = "MiXCompanion";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "R1 module initialized; UI hooks intentionally disabled");
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        String name = param.getPackageName();
        if ("com.mixplorer.beta".equals(name) || "com.mixplorer".equals(name) || "com.mixplorer.silver".equals(name)) {
            log(Log.INFO, TAG, "Scoped package observed: " + name + "; no hooks installed");
        }
    }
}
