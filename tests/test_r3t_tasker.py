"""R3T contract tests. Hosted Android build/instrumentation is mandatory next gate."""
import importlib.util
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
J = ROOT/'app/src/main/java/io/github/mikeyphw/mixplorercompanion/tasker'

class TaskerContracts(unittest.TestCase):
    def test_plugin_manifest_exact_protocol(self):
        m=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
        self.assertIn('.tasker.TaskerEditActivity',m)
        self.assertIn('android:icon="@android:drawable/ic_menu_manage"',m)
        self.assertIn('.tasker.TaskerFireReceiver',m)
        self.assertIn('com.twofortyfouram.locale.intent.action.EDIT_SETTING',m)
        self.assertIn('com.twofortyfouram.locale.intent.action.FIRE_SETTING',m)
        self.assertEqual(m.count('<receiver'),1)
        self.assertNotIn('<action android:name="com.mixplorer.ACTION_TASK"',m)
    def test_token_and_host_auth(self):
        c=(J/'TaskerContract.kt').read_text()
        edit=(J/'TaskerEditActivity.kt').read_text()
        fire=(J/'TaskerFireReceiver.kt').read_text()
        for word in ('SecureRandom','MessageDigest.isEqual','MODE_PRIVATE','DEFAULT_ENABLED'):
            self.assertIn(word,c+(ROOT/'app/src/main/java/io/github/mikeyphw/mixplorercompanion/TaskerCapabilityCatalog.kt').read_text())
        self.assertIn('isTrustedHost(caller)',edit)
        self.assertIn('hasValidToken(context, candidate)',fire)
        self.assertIn('sentFromPackage',fire)
        self.assertIn('isEnabled(context)',fire)
    def test_exact_apk_action_contract(self):
        c=(J/'TaskerContract.kt').read_text()
        fire=(J/'TaskerFireReceiver.kt').read_text()
        self.assertIn('MIX_VERSION = 26090412L',c)
        self.assertIn('MIX_PACKAGE = "com.mixplorer.beta"',c)
        self.assertIn('MIX_RECEIVER = "com.mixplorer.BroadcastReceiver"',c)
        self.assertIn('Intent(TaskerContract.MIX_ACTION)',fire)
        self.assertIn('component = ComponentName(TaskerContract.MIX_PACKAGE',fire)
        for field in ('task','src','dst','name'):
            self.assertIn('putExtra("'+field+'"',fire)
        self.assertIn('version != TaskerContract.MIX_VERSION',fire)
        self.assertNotIn('Runtime.getRuntime()',fire)
    def test_operation_catalog(self):
        c=(J/'TaskerContract.kt').read_text()
        for op in ('COPY','MOVE','RENAME','CREATE','DELETE','ARCHIVE','EXTRACT'):
            self.assertIn('"'+op+'"',c)
        for forbidden in ('ENCFS_KEY','password','su -c','MANAGE_EXTERNAL_STORAGE'):
            self.assertNotIn(forbidden,c)
    def test_dispatch_not_completion(self):
        fire=(J/'TaskerFireReceiver.kt').read_text()
        self.assertIn('marker("DISPATCHED_UNVERIFIED")',fire)
        self.assertIn('marker("RECEIVER_ENTERED")',fire)
        self.assertIn('marker("REFUSED_$reason")',fire)
        self.assertIn('Log.i("MiXTaskerBridge", "R3T_$code")',fire)
        self.assertIn('CAP_SYNC', (J/'TaskerContract.kt').read_text())
        self.assertIn('dispatched_unverified',fire)
        self.assertNotIn('operation_completed',fire)
        self.assertNotIn('RESULT_CODE_PENDING',fire)
    def test_variable_replacement_does_not_touch_token(self):
        c=(J/'TaskerContract.kt').read_text()
        edit=(J/'TaskerEditActivity.kt').read_text()
        self.assertIn('CAP_REPLACE = 8',c)
        self.assertIn('"src dst name"',edit)
        self.assertIn('REPLACE_KEYS',edit)
        self.assertIn('request.error(allowVariables = allowsVariables)',edit)
    def test_companion_switch_default_off(self):
        activity=(ROOT/'app/src/main/java/io/github/mikeyphw/mixplorercompanion/MainActivity.kt').read_text()
        c=(J/'TaskerContract.kt').read_text()
        self.assertIn('getBoolean(ENABLED, false)',c)
        self.assertIn('"Enable Tasker actions"',activity)
        self.assertIn('TaskerContract.isEnabled(this)',activity)
        self.assertIn('TaskerContract.setEnabled(this, it)',activity)
    def test_hosted_emulator_negative_test(self):
        src=(ROOT/'app/src/androidTest/java/io/github/mikeyphw/mixplorercompanion/CompanionUiSmokeTest.kt').read_text()
        shell=(ROOT/'scripts/ci/r2q_emulator_ci.sh').read_text()
        self.assertIn('untrustedPluginFireWithoutTokenCannotDispatch',src)
        self.assertIn('TaskerContract.FAILED',src)
        self.assertIn('taskerIsDisabledUntilUserExplicitlyEnablesIt',src)
        self.assertIn("OK (6 tests)",shell)
    def test_device_probe_is_read_only(self):
        src=(ROOT/'tools/r3t_tasker_device_probe.py').read_text()
        for forbidden in ('pm clear','force-stop','install -r','shell=True','su -c','--json','logcat -c'):
            self.assertNotIn(forbidden,src)
        spec=importlib.util.spec_from_file_location('r3probe',ROOT/'tools/r3t_tasker_device_probe.py')
        mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
        self.assertEqual(mod.version('versionCode=26090412 minSdk=9'),26090412)
        self.assertIsNone(mod.version('versionCode=bogus'))
        def fake(serial,*args):
            if args==('get-state',):return 'device'
            if args==('shell','getprop','ro.kernel.qemu'):return ''
            if args==('shell','getprop','ro.build.version.sdk'):return '36'
            if args[:3]==('shell','pm','path'):return 'package:/some/base.apk'
            if args==('shell','dumpsys','package',mod.BETA):return 'versionCode=26090412'
            if args==('shell','dumpsys','package',mod.COMPANION):return 'versionCode=6'
            raise AssertionError(args)
        with patch.object(mod,'adb',side_effect=fake):
            rows,code=mod.probe('localhost:5555')
        self.assertEqual(code,0)
        self.assertIn('R3T_PREFLIGHT_PASS',rows)
        self.assertIn('R3T_COMPLETION_UNVERIFIED',rows)
    def test_version_increment_and_preserve_appearance(self):
        build=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 6',build)
        self.assertIn('targetSdk = 35',build)
        self.assertIn('compileSdk = 37',build)
        module=(ROOT/'app/src/main/java/io/github/mikeyphw/mixplorercompanion/lsposed/ModuleEntry.java').read_text()
        self.assertIn('R2D_CALLBACK_REACHED',module)
        self.assertIn('R2B_CHROME_REQUESTED',module)
        self.assertNotIn('com.mixplorer.ACTION_TASK',module)

if __name__=='__main__': unittest.main()
