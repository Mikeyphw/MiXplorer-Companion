package io.github.mikeyphw.mixplorercompanion;

import org.junit.Test;
import static org.junit.Assert.*;
import io.github.mikeyphw.mixplorercompanion.appearance.AppearanceContract;

public final class AppearanceContractTest {
    @Test public void onlyInspectedBetaIsSupported() {
        assertTrue(AppearanceContract.supports("com.mixplorer.beta", 26090412L));
        assertFalse(AppearanceContract.supports("com.mixplorer.beta", 26090413L));
        assertFalse(AppearanceContract.supports("com.mixplorer.silver", 26090412L));
        assertFalse(AppearanceContract.supports("com.mixplorer", 26090412L));
    }
}
