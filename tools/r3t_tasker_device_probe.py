#!/usr/bin/env python3
"""Read-only R3T receiver/dispatch and disposable COPY verification probe.

Never sends a file-operation broadcast, clears logs, dumps paths or obtains
private tokens. A matching destination hash alone cannot attribute causation.
"""
import argparse
import re
import subprocess
import sys

BETA = 'com.mixplorer.beta'
COMPANION = 'io.github.mikeyphw.mixplorercompanion'
TASKER = 'net.dinglisch.android.taskerm'
FIXTURE_SOURCE = '/sdcard/Download/MiXTasker-R3T/source/test.txt'
FIXTURE_TARGET = '/sdcard/Download/MiXTasker-R3T/target/test.txt'
FIXTURE_SHA256 = '6dd8d3a46c0f710f7470891b173ef4e14422d2b06763dedc80c1a72febfacb36'
MARKERS = (
    'RECEIVER_ENTERED', 'SENDER_IDENTITY_UNAVAILABLE', 'REFUSED_wrong_action',
    'REFUSED_untrusted_sender', 'REFUSED_automation_disabled',
    'REFUSED_malformed_bundle', 'REFUSED_invalid_capability',
    'REFUSED_invalid_schema', 'REFUSED_invalid_input',
    'REFUSED_unsupported_beta', 'REFUSED_broadcast_rejected',
    'DISPATCH_ATTEMPT', 'DISPATCHED_UNVERIFIED', 'UNORDERED_RESULT_UNAVAILABLE',
)


def version(output):
    match = re.search(r'\bversionCode=(\d+)', output)
    return int(match.group(1)) if match else None


def adb(serial, *args):
    allowed = (
        args == ('get-state',)
        or args == ('shell', 'getprop', 'ro.build.version.sdk')
        or args == ('shell', 'getprop', 'ro.kernel.qemu')
        or (len(args) == 4 and args[:3] == ('shell', 'pm', 'path') and args[3] in (BETA, COMPANION, TASKER))
        or (len(args) == 4 and args[:3] == ('shell', 'dumpsys', 'package') and args[3] in (BETA, COMPANION))
        or args == ('shell', 'logcat', '-d', '-t', '1500', '-v', 'brief', '-s', 'MiXTaskerBridge:I', '*:S')
        or args in (('shell', 'sha256sum', FIXTURE_SOURCE), ('shell', 'sha256sum', FIXTURE_TARGET))
    )
    if not allowed or not serial or any(x.isspace() for x in serial):
        raise ValueError('R3T_UNAPPROVED_ADB_ARGUMENT')
    proc = subprocess.run(['adb', '-s', serial, *args], capture_output=True,
                          text=True, timeout=20, check=False)
    if proc.returncode:
        raise RuntimeError('R3T_ADB_COMMAND_FAILED')
    return proc.stdout.strip()


def fixture_hash(serial, filename):
    try:
        output = adb(serial, 'shell', 'sha256sum', filename)
    except (RuntimeError, subprocess.TimeoutExpired):
        return None
    match = re.match(r'^([0-9a-fA-F]{64})\s+', output)
    return match.group(1).lower() if match else None


def probe(serial, read_logs=False, verify_copy_fixture=False):
    if adb(serial, 'get-state') != 'device':
        raise RuntimeError('R3T_DEVICE_NOT_READY')
    if adb(serial, 'shell', 'getprop', 'ro.kernel.qemu') == '1':
        raise RuntimeError('R3T_EMULATOR_REFUSED')
    api = adb(serial, 'shell', 'getprop', 'ro.build.version.sdk')
    rows = ['R3T_RECEIPT_SCHEMA=2', 'R3T_ANDROID_API=' + api]
    for package, key in ((BETA, 'BETA'), (COMPANION, 'COMPANION')):
        present = adb(serial, 'shell', 'pm', 'path', package).startswith('package:')
        found = version(adb(serial, 'shell', 'dumpsys', 'package', package)) if present else None
        rows.append('R3T_' + key + '_VERSION=' + str(found if found is not None else 'MISSING'))
        if key == 'BETA' and found != 26090412:
            return rows + ['R3T_REFUSED_UNSUPPORTED_BETA'], 2
        if key == 'COMPANION' and (found is None or found < 6):
            return rows + ['R3T_REFUSED_OLD_COMPANION'], 2
    rows.append('R3T_TASKER_INSTALLED=' + (
        'YES' if adb(serial, 'shell', 'pm', 'path', TASKER).startswith('package:') else 'NO'))
    if read_logs:
        try:
            output = adb(serial, 'shell', 'logcat', '-d', '-t', '1500', '-v', 'brief', '-s', 'MiXTaskerBridge:I', '*:S')
            for code in MARKERS:
                count = sum('R3T_' + code in line for line in output.splitlines())
                rows.append('R3T_' + code.upper() + '_COUNT=' + str(count))
            rows.append('R3T_LOGS_AVAILABLE=YES')
        except (RuntimeError, subprocess.TimeoutExpired):
            rows.append('R3T_LOGS_AVAILABLE=NO')
    else:
        rows.append('R3T_LOGS=NOT_REQUESTED')
    if verify_copy_fixture:
        source = fixture_hash(serial, FIXTURE_SOURCE)
        target = fixture_hash(serial, FIXTURE_TARGET)
        if source != FIXTURE_SHA256:
            rows.append('R3T_COPY_SOURCE_MISSING_OR_CHANGED')
        elif target is None:
            rows.append('R3T_COPY_TARGET_MISSING_OR_UNREADABLE')
        elif target != source:
            rows.append('R3T_COPY_TARGET_HASH_MISMATCH')
        else:
            rows.append('R3T_COPY_BYTES_MATCH_UNATTRIBUTED')
    return rows + ['R3T_PREFLIGHT_PASS', 'R3T_COMPLETION_UNVERIFIED'], 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--serial', default='localhost:5555')
    parser.add_argument('--read-logs', action='store_true')
    parser.add_argument('--verify-copy-fixture', action='store_true')
    args = parser.parse_args()
    try:
        rows, code = probe(args.serial, args.read_logs, args.verify_copy_fixture)
    except (ValueError, RuntimeError, OSError, subprocess.TimeoutExpired):
        rows, code = ['R3T_PROBE_FAILED', 'R3T_COMPLETION_UNVERIFIED'], 2
    print('\n'.join(rows))
    return code


if __name__ == '__main__':
    sys.exit(main())
