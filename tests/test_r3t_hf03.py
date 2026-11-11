"""HF03 privacy, capability and read-only receipt regressions (no Gradle)."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/io/github/mikeyphw/mixplorercompanion/tasker'


class HF03Tests(unittest.TestCase):
    def test_receiver_markers_have_no_private_values(self):
        receiver = (SRC/'TaskerFireReceiver.kt').read_text()
        for code in ('RECEIVER_ENTERED', 'REFUSED_', 'DISPATCH_ATTEMPT',
                     'DISPATCHED_UNVERIFIED', 'broadcast_rejected',
                     'invalid_capability', 'unsupported_beta', 'invalid_schema',
                     'automation_disabled', 'untrusted_sender'):
            self.assertIn(code, receiver)
        self.assertIn('PackageManager.NameNotFoundException', receiver)
        self.assertIn('putExtra("task", request.operation.lowercase(java.util.Locale.ROOT))', receiver)
        self.assertIn('TaskerContract.CAP_RETURN', receiver)
        self.assertIn('putString("%errmsg", explanation)', receiver)
        self.assertIn('resultCode = TaskerContract.FAILED', receiver)
        self.assertIn('version != TaskerContract.MIX_VERSION', receiver)
        self.assertNotIn('Log.i("MiXTaskerBridge", request.', receiver)
        self.assertNotIn('Log.i("MiXTaskerBridge", candidate', receiver)
        self.assertNotIn('Log.i("MiXTaskerBridge", intent.', receiver)

    def test_tasker_editor_honors_host_extensions(self):
        editor = (SRC/'TaskerEditActivity.kt').read_text()
        contract = (SRC/'TaskerContract.kt').read_text()
        self.assertIn('TaskerContract.CAP_SYNC', editor)
        self.assertIn('TaskerContract.REQUEST_TIMEOUT', editor)
        self.assertIn('TaskerContract.CAP_RELEVANT', editor)
        self.assertIn('"src dst name"', editor)
        self.assertIn('CAP_RETURN = 2', contract)
        self.assertIn('CAP_RELEVANT = 16', contract)
        self.assertIn('CAP_SYNC = 32', contract)
        self.assertIn('getString("token")', (SRC/'TaskerFireReceiver.kt').read_text())

    def test_cross_phase_regression_contracts_match_current_build(self):
        build = (ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 6', build)
        self.assertIn('versionName = "0.6.0-r3t-hf03"', build)
        workflow = (ROOT/'.github/workflows/android-ci.yml').read_text()
        runner = (ROOT/'scripts/ci/r2q_emulator_ci.sh').read_text()
        self.assertIn("grep -Fxq 'OK (6 tests)'",workflow)
        self.assertIn("grep -Fxq 'OK (6 tests)'",runner)
        for name in ('test_r2b_appearance.py','test_r2d_runtime.py',
                     'test_r2q_acceptance.py', 'test_r3t_tasker.py'):
            content = (ROOT/'tests'/name).read_text()
            self.assertNotIn('OK (4 tests)',content,name)
        for name in ('test_r2b_appearance.py','test_r2d_runtime.py',
                     'test_r3t_tasker.py'):
            content = (ROOT/'tests'/name).read_text()
            self.assertNotIn("self.assertIn('versionCode = 5'",content,name)
        receiver = (SRC/'TaskerFireReceiver.kt').read_text()
        self.assertIn('marker("RECEIVER_ENTERED")', receiver)
        self.assertIn('marker("DISPATCHED_UNVERIFIED")', receiver)
        self.assertIn('marker("REFUSED_$reason")', receiver)

    def test_device_probe_never_dispatches(self):
        source = (ROOT/'tools/r3t_tasker_device_probe.py').read_text()
        for forbidden in ('am broadcast', 'logcat -c', 'force-stop', 'pm clear',
                          'shell=True', 'su -c', 'install -r'):
            self.assertNotIn(forbidden, source)
        spec = importlib.util.spec_from_file_location('r3tprobe', ROOT/'tools/r3t_tasker_device_probe.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        def fake(serial, *args):
            if args == ('get-state',): return 'device'
            if args == ('shell','getprop','ro.kernel.qemu'): return ''
            if args == ('shell','getprop','ro.build.version.sdk'): return '36'
            if args[:3] == ('shell','pm','path'): return 'package:/base.apk'
            if args == ('shell','dumpsys','package',module.BETA): return 'versionCode=26090412'
            if args == ('shell','dumpsys','package',module.COMPANION): return 'versionCode=6'
            if args[:3] == ('shell','logcat','-d'):
                return 'I/MiXTaskerBridge: R3T_RECEIVER_ENTERED\nI/MiXTaskerBridge: R3T_REFUSED_invalid_capability'
            if args == ('shell','sha256sum',module.FIXTURE_SOURCE):
                return module.FIXTURE_SHA256 + '  ' + module.FIXTURE_SOURCE
            if args == ('shell','sha256sum',module.FIXTURE_TARGET):
                raise RuntimeError('not-readable')
            raise AssertionError(args)
        with patch.object(module,'adb',side_effect=fake):
            rows,code = module.probe('localhost:5555',True,True)
        self.assertEqual(code,0)
        self.assertIn('R3T_RECEIVER_ENTERED_COUNT=1',rows)
        self.assertIn('R3T_REFUSED_INVALID_CAPABILITY_COUNT=1',rows)
        self.assertIn('R3T_DISPATCHED_UNVERIFIED_COUNT=0',rows)
        self.assertIn('R3T_COPY_TARGET_MISSING_OR_UNREADABLE',rows)
        self.assertIn('R3T_COMPLETION_UNVERIFIED',rows)
        self.assertIn('found < 6', source)


if __name__ == '__main__': unittest.main()
