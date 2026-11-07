package io.github.mikeyphw.mixplorercompanion;

import android.app.Application;
import android.content.SharedPreferences;

import java.util.concurrent.CopyOnWriteArraySet;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;
import io.github.mikeyphw.mixplorercompanion.appearance.AppearanceContract;

/** The framework owns cross-process preferences; hooked apps read them only. */
public final class CompanionApp extends Application implements XposedServiceHelper.OnServiceListener {
    private static volatile XposedService connected;
    private static final CopyOnWriteArraySet<Runnable> listeners = new CopyOnWriteArraySet<>();

    @Override public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(this);
    }

    @Override public void onServiceBind(XposedService service) {
        connected = service;
        notifyChanged();
    }

    @Override public void onServiceDied(XposedService service) {
        if (connected == service) connected = null;
        notifyChanged();
    }

    private static void notifyChanged() {
        for (Runnable listener : listeners) listener.run();
    }

    public static void addListener(Runnable listener) { listeners.add(listener); }
    public static void removeListener(Runnable listener) { listeners.remove(listener); }

    public static SharedPreferences appearancePreferences() {
        XposedService service = connected;
        if (service == null) return null;
        try { return service.getRemotePreferences(AppearanceContract.GROUP); }
        catch (RuntimeException | LinkageError ignored) { return null; }
    }
}
