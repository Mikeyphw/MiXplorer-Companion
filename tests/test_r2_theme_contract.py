import re
import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/io/github/mikeyphw/mixplorercompanion'
class R2SourceContract(unittest.TestCase):
    def test_version_strict(self):
        s=(SRC/'appearance/AppearanceContract.java').read_text()
        self.assertIn('BETA_VERSION_CODE = 26090412L',s)
        self.assertIn('BETA_PACKAGE.equals(pkg) && BETA_VERSION_CODE == versionCode',s)
    def test_hook_constrained(self):
        s=(SRC/'lsposed/ModuleEntry.java').read_text()
        self.assertIn('onPackageReady(PackageReadyParam param)',s)
        self.assertIn('Class.forName(AppearanceContract.BROWSE_ACTIVITY',s)
        self.assertIn('getDeclaredMethod("onResume")',s)
        self.assertIn('chain.proceed()',s)
        self.assertIn('AppearanceContract.supports(packageName, code)',s)
        self.assertIn('getBoolean(AppearanceContract.ENABLED, false)',s)
        self.assertNotIn('de.robv.android.xposed',s)
        self.assertNotIn('ACTION_TASK',s)
    def test_no_exported_automation(self):
        manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
        self.assertNotIn('<receiver',manifest)
        self.assertNotIn('MANAGE_EXTERNAL_STORAGE',manifest)
    def test_remote_preferences_not_world_readable(self):
        s=(SRC/'CompanionApp.java').read_text()
        self.assertIn('XposedServiceHelper.registerListener(this)',s)
        self.assertIn('service.getRemotePreferences(AppearanceContract.GROUP)',s)
        self.assertNotIn('MODE_WORLD_READABLE',s)
    def test_pending_runtime_gate(self):
        s=(ROOT/'r2/IMPLEMENTATION.md').read_text()
        self.assertIn('Runtime acceptance pending',s)
if __name__=='__main__':unittest.main()
