"""Hosted qualification: check actual APK contains LSPosed registration resources."""
from pathlib import Path
from zipfile import ZipFile
import sys

apk=Path(sys.argv[1]); assert apk.is_file(), f'APK missing: {apk}'
with ZipFile(apk) as z:
    names=set(z.namelist())
    required={
        'META-INF/xposed/module.prop',
        'META-INF/xposed/scope.list',
        'META-INF/xposed/java_init.list',
        'classes.dex',
        'AndroidManifest.xml',
    }
    missing=required-names
    assert not missing, f'Missing metadata: {sorted(missing)}'
    scope=z.read('META-INF/xposed/scope.list').decode().splitlines()
    assert scope == ['com.mixplorer.beta','com.mixplorer','com.mixplorer.silver'],scope
    entry=z.read('META-INF/xposed/java_init.list').decode().strip()
    assert entry == 'io.github.mikeyphw.mixplorercompanion.lsposed.ModuleEntry',entry
print('R1_APK_METADATA_PASS',apk)
