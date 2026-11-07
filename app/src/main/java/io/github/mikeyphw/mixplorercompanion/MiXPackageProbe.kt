package io.github.mikeyphw.mixplorercompanion

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Detects MiXplorer variants without QUERY_ALL_PACKAGES. */
class MiXPackageProbe(private val context: Context) {
    data class Candidate(val packageName: String, val versionName: String, val versionCode: Long, val evidence: String)

    fun installed(): List<Candidate> = PackageCatalog.targets.mapNotNull { packageName ->
        try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            Candidate(packageName, info.versionName ?: "unknown", code, PackageCatalog.evidenceLevel(packageName, code))
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
