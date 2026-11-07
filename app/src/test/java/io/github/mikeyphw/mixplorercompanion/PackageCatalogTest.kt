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
    @Test fun taskerIsNotAdvertised() {
        assertTrue(TaskerCapabilityCatalog.AVAILABLE_ACTIONS.isEmpty())
    }
}
