"""R2Q: match Cinderbox successful API36 emulator and insist on real test results."""
import importlib.util
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
FLOW=(ROOT/'.github/workflows/android-ci.yml').read_text()
RUN=(ROOT/'scripts/ci/r2q_emulator_ci.sh').read_text()
MODULE=ROOT/'tools/r2_device_probe.py'
TEST=ROOT/'app/src/androidTest/java/io/github/mikeyphw/mixplorercompanion/CompanionUiSmokeTest.kt'

class QualificationContracts(unittest.TestCase):
    def test_hosted_emulator_job_exists(self):
        self.assertIn('  instrumentation:\n    needs: android', FLOW)
        self.assertIn('runs-on: ubuntu-24.04',FLOW)
        self.assertIn('ReactiveCircus/android-emulator-runner@a421e43855164a8197daf9d8d40fe71c6996bb0d',FLOW)
        for s in ('api-level: 36','target: google_apis','arch: x86_64','profile: pixel_6','cores: 2','ram-size: 2048M','disk-size: 3072M','disable-animations: true','script: bash scripts/ci/r2q_emulator_ci.sh','if: always()'):
            self.assertIn(s,FLOW)
        self.assertNotIn('reactivecircus/android-emulator-runner@v2',FLOW)
        self.assertNotIn('udevadm',FLOW)
    def test_sdk_platform_and_download(self):
        self.assertIn("sdkmanager --install 'platforms;android-37.0' 'platforms;android-36' 'build-tools;36.0.0'", FLOW)
        self.assertIn("'system-images;android-36;google_apis;x86_64'",FLOW)
        self.assertIn('test -c /dev/kvm',FLOW)
        self.assertIn(':app:assembleDebug :app:assembleDebugAndroidTest',FLOW)
    def test_two_real_tests_required(self):
        self.assertIn('am instrument -w -r',RUN)
        self.assertIn("grep -Fxq 'OK (4 tests)'",RUN)
        self.assertIn("grep -Fxq 'INSTRUMENTATION_CODE: -1'",RUN)
        self.assertIn('R2Q_INSTRUMENTATION_RESULT_FAILED',RUN)
        self.assertIn('R2Q_HOSTED_EMULATOR_UI_PASS',RUN)
        self.assertIn('ro.kernel.qemu',RUN)
        self.assertIn('ro.build.version.sdk',RUN)
        self.assertIn('com.mixplorer.beta',RUN)
        self.assertIn('emulator-5554',RUN)
        self.assertNotIn('localhost:5555',RUN)
        self.assertNotIn('pm clear',RUN)
    def test_modern_android_test_dependencies(self):
        s=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('androidTestImplementation("androidx.test:runner:1.7.0")',s)
        self.assertIn('androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")',s)
        self.assertRegex(s, r'versionCode\s*=\s*[2-9]\d*')
        self.assertIn('targetSdk = 35',s)
        self.assertIn('compileSdk = 37',s)
    def test_ui_instrumentation_defaults_off(self):
        source=TEST.read_text()
        self.assertIn('ActivityScenarioRule(MainActivity::class.java)',source)
        self.assertIn('refusesAppearanceControlsWhenMiXplorerAndVectorAreAbsent',source)
        self.assertIn('matches(not(isEnabled()))',source)
        self.assertNotIn('ACTION_TASK',source)
    def test_probe_version_gate_non_destructive(self):
        src=MODULE.read_text()
        self.assertIn('code != BETA_CODE',src)
        self.assertIn('R2_LSPOSED_RUNTIME_INCONCLUSIVE',src)
        for forbidden in ['"force-stop"', '"logcat -c"', '"pm clear"', '"install -r"','--json']:
            self.assertNotIn(forbidden,src)
    def test_probe_parsers(self):
        spec=importlib.util.spec_from_file_location('r2_device_probe_test',MODULE)
        mod=importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        self.assertEqual(mod.extract_version_code('versionCode=26090412 minSdk=26'),26090412)
        self.assertIsNone(mod.extract_version_code('versionCode=unset'))
        self.assertEqual(mod.hook_evidence('X MiXCompanionR2: hello\nother'),1)
        self.assertEqual(mod.hook_evidence('other'),0)
    def test_no_local_gradle(self):
        self.assertNotIn('gradle ',MODULE.read_text())
        self.assertNotIn('--json',FLOW)
if __name__=='__main__':unittest.main()
