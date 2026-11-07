package io.github.mikeyphw.mixplorercompanion

/** Only package names verified by APK inspection or documented stable variants. */
object PackageCatalog {
    const val VERIFIED_BETA_PACKAGE = "com.mixplorer.beta"
    const val VERIFIED_BETA_VERSION = "6.71.15-BETA"
    const val VERIFIED_BETA_VERSION_CODE = 26090412L
    val targets = listOf(VERIFIED_BETA_PACKAGE, "com.mixplorer", "com.mixplorer.silver")

    fun evidenceLevel(packageName: String, versionCode: Long): String = when {
        packageName == VERIFIED_BETA_PACKAGE && versionCode == VERIFIED_BETA_VERSION_CODE ->
            "Source inspected • runtime not verified"
        packageName in targets -> "Known package • version not qualified"
        else -> "Unsupported package"
    }
}
