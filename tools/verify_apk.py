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
    # A DEX marker proves packaging only, not callback execution on a device.
    dex = b''.join(z.read(n) for n in names if n.startswith('classes') and n.endswith('.dex'))
    for marker in (b'R2D_CALLBACK_REACHED', b'R2D_APPEARANCE_REQUESTED'):
        assert marker in dex, f'Runtime marker not packaged: {marker.decode()}'
print('R1_APK_METADATA_PASS',apk)
