"""R2B: source-bound, opt-in appearance expansion. Physical visuals are unqualified."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT/'app/src/main/java/io/github/mikeyphw/mixplorercompanion'

class R2BContracts(unittest.TestCase):
    def test_all_new_surface_options_default_off(self):
        contract = (JAVA/'appearance/AppearanceContract.java').read_text()
        ui = (JAVA/'MainActivity.kt').read_text()
        module = (JAVA/'lsposed/ModuleEntry.java').read_text()
        keys = ('TOOLBAR_SURFACE','DRAWER_SURFACE','DIALOG_SURFACE','MENU_SURFACE')
        for key in keys:
            self.assertRegex(contract, rf'public static final String {key} = "r2b_[a-z_]+";')
            self.assertRegex(ui, rf'AppearanceContract\.{key}, false, canChange')
            self.assertIn('AppearanceContract.'+key+', false)',module)
        self.assertIn('AppearanceContract.ENABLED, false)',module)
        self.assertIn('AppearanceContract.supports(context.getPackageName(), code)',module)
        self.assertIn('AppearanceContract.supports(packageName, code)',module)

    def test_target_limited_to_exact_beta(self):
        module = (JAVA/'lsposed/ModuleEntry.java').read_text()
        contract = (JAVA/'appearance/AppearanceContract.java').read_text()
        self.assertIn('if (!AppearanceContract.BETA_PACKAGE.equals(param.getPackageName())) return;',module)
        self.assertLess(module.index('if (!AppearanceContract.BETA_PACKAGE.equals(param.getPackageName()))'),
                        module.index('installWindowHooks()'))
        self.assertIn('BETA_VERSION_CODE = 26090412L',contract)
        self.assertIn('BETA_PACKAGE.equals(pkg) && BETA_VERSION_CODE == versionCode',contract)
        for forbidden in ('com.mixplorer.silver', 'de.robv.android.xposed'):
            self.assertNotIn(forbidden,module)

    def test_hooks_proceed_first_and_fail_open(self):
        module=(JAVA/'lsposed/ModuleEntry.java').read_text()
        self.assertGreaterEqual(module.count('Object result = chain.proceed();'),3)
        self.assertIn('catch (Throwable error)',module)
        self.assertIn('Dialog.class.getMethod("show")',module)
        self.assertIn('PopupWindow.class.getMethod("showAsDropDown"',module)
        self.assertIn('PopupWindow.class.getMethod("showAtLocation"',module)
        self.assertIn('R2D_CALLBACK_REACHED',module)
        self.assertIn('R2B_CHROME_REQUESTED',module)
        self.assertIn('R2B_DIALOG_SURFACE_REQUESTED',module)
        self.assertIn('R2B_MENU_SURFACE_REQUESTED',module)
        self.assertNotIn('ACTION_TASK',module)

    def test_view_scan_bounded_and_data_not_visited(self):
        app=(JAVA/'appearance/ThemeApplicator.java').read_text()
        self.assertIn('scanned++ < 1600',app)
        self.assertIn('decor.postDelayed(paint, 240L)',app)
        for cls in ('MiDrawer','MiPager','MiListView','RecyclerView','WebView'):
            self.assertIn(cls,app)
        self.assertIn('setBackgroundTintList',app)
        for disallowed in ('setOnClickListener','setOnTouchListener','setText(', 'setLayoutParams(', 'removeView(', 'addView('):
            self.assertNotIn(disallowed,app)

    def test_dialog_menu_restricted_and_no_content_mutation(self):
        src=(JAVA/'appearance/WindowSurfaceApplicator.java').read_text()
        self.assertIn('"android.app.AlertDialog".equals(type)',src)
        self.assertIn('"androidx.appcompat.app.AlertDialog".equals(type)',src)
        self.assertIn('current instanceof ColorDrawable',src)
        self.assertIn('current instanceof GradientDrawable',src)
        self.assertIn('setBackgroundTintList',src)
        self.assertIn('setBackgroundDrawable',src)
        for forbidden in ('getText()', 'setText(', 'getFiles', 'listFiles','setOnClickListener','requestPermissions','ContentResolver'):
            self.assertNotIn(forbidden,src)

    def test_platform_and_rollback_gates(self):
        gradle=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 4',gradle)
        self.assertIn('versionName = "0.4.0-r2b"',gradle)
        self.assertIn('minSdk = 26',gradle)
        self.assertIn('targetSdk = 35',gradle)
        self.assertIn('compileSdk = 37',gradle)
        doc=(ROOT/'r2/R2B_APPEARANCE.md').read_text()
        for phrase in ('force-stop/relaunch', 'Rollback', 'PENDING', 'NO file operations'):
            self.assertIn(phrase,doc)
        manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
        for forbidden in ('MANAGE_EXTERNAL_STORAGE','<receiver', 'android.permission.QUERY_ALL_PACKAGES'):
            self.assertNotIn(forbidden,manifest)

    def test_emulator_checks_all_disabled_controls(self):
        instrument=(ROOT/'app/src/androidTest/java/io/github/mikeyphw/mixplorercompanion/CompanionUiSmokeTest.kt').read_text()
        for s in ('Toolbar surface','Navigation drawer surface','Dialog surface','Popup menu surface'):
            self.assertIn('withContentDescription("'+s+'")',instrument)
        flow=(ROOT/'.github/workflows/android-ci.yml').read_text()
        self.assertIn('api-level: 36',flow)
        self.assertIn("grep -Fxq 'OK (2 tests)'",flow)
        self.assertIn('ReactiveCircus/android-emulator-runner@a421e43855164a8197daf9d8d40fe71c6996bb0d',flow)

    def test_existing_device_probe_remains_read_only(self):
        src=(ROOT/'tools/r2d_runtime_probe.py').read_text()
        self.assertIn('MIN_COMPANION = 3',src)
        for forbidden in ('"force-stop"','"pm clear"','"logcat -c"','"install -r"','--json'):
            self.assertNotIn(forbidden,src)
        verify=(ROOT/'tools/verify_apk.py').read_text()
        for marker in ('R2B_CHROME_REQUESTED','R2B_DIALOG_SURFACE_REQUESTED','R2B_MENU_SURFACE_REQUESTED'):
            self.assertIn(marker,verify)

if __name__ == '__main__':
    unittest.main()
