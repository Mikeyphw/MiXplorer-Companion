package io.github.mikeyphw.mixplorercompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageCatalogTest {
    @Test fun verifiedBetaIsSourceInspectedOnly() {
        assertEquals("com.mixplorer.beta", PackageCatalog.VERIFIED_BETA_PACKAGE)
        assertEquals(26090412L, PackageCatalog.VERIFIED_BETA_VERSION_CODE)
        assertTrue(PackageCatalog.evidenceLevel("com.mixplorer.beta", 26090412L).contains("runtime not verified"))
    }

    @Test fun taskerActionsAreAdvertisedButDefaultDisabled() {
        assertTrue(TaskerCapabilityCatalog.PLUGIN_ENABLED)
        assertTrue(!TaskerCapabilityCatalog.DEFAULT_ENABLED)
        assertEquals(
            listOf("COPY", "MOVE", "RENAME", "CREATE", "DELETE", "ARCHIVE", "EXTRACT"),
            TaskerCapabilityCatalog.AVAILABLE_ACTIONS
        )
        assertEquals("com.mixplorer.ACTION_TASK", TaskerCapabilityCatalog.FUTURE_ACTION)
        assertEquals("com.mixplorer.BroadcastReceiver", TaskerCapabilityCatalog.RECEIVER_CLASS)
    }
}
