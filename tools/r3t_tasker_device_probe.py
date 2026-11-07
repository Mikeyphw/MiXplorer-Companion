#!/usr/bin/env python3
"""Read-only Tasker/Companion/MiXplorer installation and dispatch-marker probe.

Does not fire actions, create/delete files, grant permissions, clear logs or claim
MiXplorer file operation completion. stdout contains only sanitized receipt.
"""
import argparse
import re
import subprocess
import sys

BETA = 'com.mixplorer.beta'
COMPANION = 'io.github.mikeyphw.mixplorercompanion'
TASKER = 'net.dinglisch.android.taskerm'
MARKER = 'R3T_DISPATCHED_UNVERIFIED'


def version(output):
    m = re.search(r'\bversionCode=(\d+)', output)
    return int(m.group(1)) if m else None


def adb(serial, *args):
    allowed = (
        args == ('get-state',)
        or args == ('shell', 'getprop', 'ro.build.version.sdk')
        or args == ('shell', 'getprop', 'ro.kernel.qemu')
        or (len(args) == 4 and args[:3] == ('shell', 'pm', 'path') and args[3] in (BETA, COMPANION, TASKER))
        or (len(args) == 4 and args[:3] == ('shell', 'dumpsys', 'package') and args[3] in (BETA, COMPANION))
        or args == ('shell', 'logcat', '-d', '-t', '1500', '-v', 'brief', '-s', 'MiXTaskerBridge:I', '*:S')
    )
    if not allowed or not serial or any(x.isspace() for x in serial):
        raise ValueError('R3T_UNAPPROVED_ADB_ARGUMENT')
    p = subprocess.run(['adb', '-s', serial, *args], capture_output=True,
                       text=True, timeout=20, check=False)
    if p.returncode:
        raise RuntimeError('R3T_ADB_COMMAND_FAILED')
    return p.stdout.strip()


def probe(serial, read_logs=False):
    if adb(serial, 'get-state') != 'device':
        raise RuntimeError('R3T_DEVICE_NOT_READY')
    if adb(serial, 'shell', 'getprop', 'ro.kernel.qemu') == '1':
        raise RuntimeError('R3T_EMULATOR_REFUSED')
    api = adb(serial, 'shell', 'getprop', 'ro.build.version.sdk')
    results = ['R3T_RECEIPT_SCHEMA=1', 'R3T_ANDROID_API=' + api]
    for name, key in ((BETA, 'BETA'), (COMPANION, 'COMPANION')):
        present = adb(serial, 'shell', 'pm', 'path', name).startswith('package:')
        found = version(adb(serial, 'shell', 'dumpsys', 'package', name)) if present else None
        results.append('R3T_' + key + '_VERSION=' + str(found if found is not None else 'MISSING'))
        if key == 'BETA' and found != 26090412:
            return results + ['R3T_REFUSED_UNSUPPORTED_BETA'], 2
        if key == 'COMPANION' and (found is None or found < 5):
            return results + ['R3T_REFUSED_OLD_COMPANION'], 2
    tasker = adb(serial, 'shell', 'pm', 'path', TASKER).startswith('package:')
    results.append('R3T_TASKER_INSTALLED=' + ('YES' if tasker else 'NO'))
    if read_logs:
        try:
            raw = adb(serial, 'shell', 'logcat', '-d', '-t', '1500', '-v', 'brief', '-s', 'MiXTaskerBridge:I', '*:S')
            results.append('R3T_DISPATCH_MARKERS=' + str(sum(MARKER in line for line in raw.splitlines())))
        except (RuntimeError, subprocess.TimeoutExpired):
            results.append('R3T_DISPATCH_MARKERS=UNAVAILABLE')
    else:
        results.append('R3T_DISPATCH_MARKERS=NOT_REQUESTED')
    return results + ['R3T_PREFLIGHT_PASS', 'R3T_COMPLETION_UNVERIFIED'], 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--serial', default='localhost:5555')
    ap.add_argument('--read-logs', action='store_true')
    args = ap.parse_args()
    try:
        rows, code = probe(args.serial, args.read_logs)
    except (ValueError, RuntimeError, OSError, subprocess.TimeoutExpired):
        rows, code = ['R3T_PROBE_FAILED', 'R3T_COMPLETION_UNVERIFIED'], 2
    print('\n'.join(rows))
    return code

if __name__ == '__main__':
    sys.exit(main())
