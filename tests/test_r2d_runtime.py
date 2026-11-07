"""R2D source, negative-case and non-destructive device-probe tests."""
import importlib.util
import io
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
D = ROOT/'app/src/main/java/io/github/mikeyphw/mixplorercompanion'
PATH = ROOT/'tools/r2d_runtime_probe.py'
spec = importlib.util.spec_from_file_location('r2d_runtime_probe', PATH)
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)

class R2DContracts(unittest.TestCase):
    def test_marker_guard_and_interception_preserved(self):
        source = (D/'lsposed/ModuleEntry.java').read_text()
        for marker in probe.MARKERS:
            self.assertIn('"'+marker+'"', source)
        self.assertIn('chain.proceed()', source)
        self.assertIn('AppearanceContract.supports(packageName, code)', source)
        self.assertIn('getBoolean(AppearanceContract.ENABLED, false)', source)
        self.assertLess(source.index('AppearanceContract.supports(packageName, code)'),
                        source.index('"R2D_CALLBACK_REACHED"'))
        self.assertLess(source.index('ThemeApplicator.apply(activity, bars, tabs)'),
                        source.index('"R2D_APPEARANCE_REQUESTED"'))
        self.assertNotIn('de.robv.android.xposed', source)
    def test_extract_version(self):
        self.assertEqual(probe.extract_version('  versionCode=26090412 minSdk=26'),26090412)
        self.assertIsNone(probe.extract_version('versionCode=unknown'))
    def test_counts_do_not_echo_user_logs(self):
        events=probe.event_counts('private-file-name.txt\nR2D_CALLBACK_REACHED\nR2D_APPEARANCE_REQUESTED')
        self.assertEqual(events['R2D_CALLBACK_REACHED'],1)
        self.assertEqual(events['R2D_APPEARANCE_REQUESTED'],1)
        self.assertNotIn('private-file-name',str(events))
    def test_adb_whitelist(self):
        self.assertTrue(probe.approved(('get-state',)))
        self.assertTrue(probe.approved(('shell','logcat','-d','-t','2000','-v','brief','-s','MiXCompanionR2:V','*:S')))
        for args in [('shell','pm','clear',probe.BETA),
                     ('shell','am','force-stop',probe.BETA),
                     ('shell','logcat','-c'),
                     ('shell','screencap','-p'),
                     ('install','foo.apk'),('shell','su','-c','id')]:
            self.assertFalse(probe.approved(args),args)
    def test_preflight_refuses_unsupported_beta_without_launch(self):
        commands=[]
        def fake(serial,*args):
            commands.append(args)
            if args==('get-state',):return 'device'
            if args==('shell','getprop','ro.kernel.qemu'):return ''
            if args==('shell','getprop','ro.build.version.sdk'):return '36'
            if args[:3]==('shell','pm','path'):return 'package:/data/app/base.apk'
            if args==('shell','dumpsys','package',probe.BETA):return 'versionCode=1'
            if args==('shell','dumpsys','package',probe.COMPANION):return 'versionCode=3'
            raise AssertionError(args)
        with patch.object(probe,'adb',side_effect=fake):
            lines,code=probe.probe('localhost:5555',open_companion=True,open_beta=True)
        self.assertEqual(code,2)
        self.assertIn('R2D_PREFLIGHT_REFUSED_UNSUPPORTED_BETA',lines)
        self.assertFalse(any(x[1:3]==('am','start') for x in commands))
    def test_preflight_refuses_emulator_before_package_queries(self):
        with patch.object(probe,'adb',side_effect=['device','1']) as f:
            with self.assertRaisesRegex(RuntimeError,'R2D_EMULATOR_REFUSED'):
                probe.probe('emulator-5554')
        self.assertEqual(f.call_count,2)
    def test_valid_identities_do_not_claim_injection(self):
        def fake(serial,*args):
            if args==('get-state',):return 'device'
            if args==('shell','getprop','ro.kernel.qemu'):return ''
            if args==('shell','getprop','ro.build.version.sdk'):return '36'
            if args[:3]==('shell','pm','path'):return 'package:/data/app/base.apk'
            if args==('shell','dumpsys','package',probe.BETA):return 'versionCode=26090412'
            if args==('shell','dumpsys','package',probe.COMPANION):return 'versionCode=3'
            raise AssertionError(args)
        with patch.object(probe,'adb',side_effect=fake):
            lines,code=probe.probe('localhost:5555')
        self.assertEqual(code,0)
        self.assertIn('R2D_PREFLIGHT_PASS',lines)
        self.assertIn('R2D_RUNTIME_INCONCLUSIVE',lines)
        self.assertNotIn('R2D_RUNTIME_PASS',lines)
    def test_ui_and_ci_gate_remain_conservative(self):
        activity=(D/'MainActivity.kt').read_text()
        self.assertIn('R2B visual acceptance: NOT VERIFIED BY APP',activity)
        self.assertIn('Appearance preference: enabled (not proof of injection)',activity)
        manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
        self.assertNotIn('MANAGE_EXTERNAL_STORAGE',manifest)
        workflow=(ROOT/'.github/workflows/android-ci.yml').read_text()
        self.assertIn("grep -Fxq 'OK (4 tests)'",workflow)
        self.assertIn('api-level: 36',workflow)
        self.assertIn('ReactiveCircus/android-emulator-runner@a421e43855164a8197daf9d8d40fe71c6996bb0d',workflow)
        gradle=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 5',gradle)
        self.assertIn('targetSdk = 35',gradle)
        self.assertIn('compileSdk = 37',gradle)
    def test_no_unsafe_operations_in_probe(self):
        source=PATH.read_text()
        self.assertNotIn('--json',source)
        self.assertNotIn('gradle --',source)
        self.assertNotIn('shell=True',source)
        self.assertNotIn('su -c',source)

if __name__=='__main__':unittest.main()
