import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class FoundationContracts(unittest.TestCase):
    def test_evidence(self):
        j = json.loads((ROOT / "r0/apk-fingerprint.json").read_text())
        self.assertEqual(j["manifest"]["package"], "com.mixplorer.beta")
        self.assertEqual(j["manifest"]["version_code"], 26090412)
        self.assertEqual(j["manifest"]["target_sdk"], 37)
        self.assertFalse(j["runtime_verified"])
    def test_scope_contains_beta(self):
        scopes = (ROOT / "app/src/main/resources/META-INF/xposed/scope.list").read_text().splitlines()
        self.assertEqual(scopes[0], "com.mixplorer.beta")
        self.assertEqual(len(scopes), len(set(scopes)))
    def test_no_unqualified_intents(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.assertNotIn("com.mixplorer.ACTION_TASK", manifest)
        self.assertNotIn("android.permission.MANAGE_EXTERNAL_STORAGE", manifest)
        source = (ROOT / "app/src/main/java/io/github/mikeyphw/mixplorercompanion/TaskerCapabilityCatalog.kt").read_text()
        self.assertIn("PLUGIN_ENABLED = false", source)
    def test_devtool_android_runner(self):
        import tomllib
        d=tomllib.loads((ROOT / '.devtool.toml').read_text())
        self.assertEqual(d['targets']['companion']['runner'],'android')
        self.assertEqual(d['targets']['companion']['tasks']['build'],[':app:assembleDebug'])
    def test_registered_entry(self):
        entry = (ROOT / 'app/src/main/resources/META-INF/xposed/java_init.list').read_text().strip()
        self.assertTrue((ROOT/'app/src/main/java'/Path(*entry.split('.'))).with_suffix('.java').is_file())
        source = (ROOT/'app/src/main/java'/Path(*entry.split('.'))).with_suffix('.java').read_text()
        self.assertNotIn('.hook(', source)

if __name__ == '__main__':unittest.main()
