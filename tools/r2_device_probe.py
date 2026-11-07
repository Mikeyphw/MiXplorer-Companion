#!/usr/bin/env python3
"""Non-destructive R2 device smoke probe; never claims LSPosed injection success.

Runs ADB read-only checks and opens only the companion activity. Optionally opens
MiXplorer Beta when explicitly requested. Does not install, force-stop, clear data,
clear logs, read file-manager contents, or enable an appearance preference.
"""
import argparse
import re
import subprocess
import sys
from pathlib import Path

BETA = "com.mixplorer.beta"
BETA_CODE = 26090412
COMPANION = "io.github.mikeyphw.mixplorercompanion"
COMPANION_ACTIVITY = COMPANION + "/.MainActivity"
BETA_ACTIVITY = BETA + "/com.mixplorer.activities.BrowseActivity"


def extract_version_code(dumpsys_output):
    match = re.search(r"\bversionCode=(\d+)\b", dumpsys_output)
    return int(match.group(1)) if match else None


def hook_evidence(lines):
    """Log lines may be unavailable: absence of evidence is never a PASS."""
    matching = [line for line in lines.splitlines() if "MiXCompanionR2" in line]
    return len(matching)


def command(serial, *args, timeout=25):
    try:
        completed = subprocess.run(["adb", "-s", serial, *args], text=True,
                                   capture_output=True, timeout=timeout, check=False)
    except (FileNotFoundError, subprocess.TimeoutExpired) as exc:
        raise RuntimeError("ADB_UNAVAILABLE_OR_TIMED_OUT") from exc
    if completed.returncode != 0:
        raise RuntimeError("ADB_COMMAND_FAILED:" + args[0] + ":" +
                           completed.stderr.strip()[:160])
    return completed.stdout


def package_code(serial, name):
    path = command(serial, "shell", "pm", "path", name)
    if not path.lstrip().startswith("package:"):
        return None
    return extract_version_code(command(serial, "shell", "dumpsys", "package", name))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", default="localhost:5555")
    parser.add_argument("--launch-beta", action="store_true",
                        help="Open the already-installed Beta activity; does not force-stop it")
    args = parser.parse_args()
    try:
        state = command(args.serial, "get-state").strip()
        if state != "device":
            raise RuntimeError("ADB_DEVICE_NOT_READY")
        code = package_code(args.serial, BETA)
        companion_code = package_code(args.serial, COMPANION)
        print("R2_DEVICE_SERIAL=" + args.serial)
        print("R2_BETA_VERSION=" + (str(code) if code is not None else "NOT_INSTALLED"))
        print("R2_COMPANION_VERSION=" + (str(companion_code) if companion_code is not None else "NOT_INSTALLED"))
        if code != BETA_CODE:
            print("R2_DEVICE_REFUSED_UNSUPPORTED_BETA")
            return 2
        if companion_code is None or companion_code < 2:
            print("R2_DEVICE_REFUSED_COMPANION_MISSING_OR_OLD")
            return 2
        command(args.serial, "shell", "am", "start", "-W", "-n", COMPANION_ACTIVITY)
        print("R2_COMPANION_LAUNCH_PASS")
        if args.launch_beta:
            command(args.serial, "shell", "am", "start", "-W", "-n", BETA_ACTIVITY)
            print("R2_BETA_LAUNCH_PASS")
        try:
            logs = command(args.serial, "shell", "logcat", "-d", "-t", "1500", "-v", "brief")
            count = hook_evidence(logs)
            print("R2_HOOK_LOG_LINES=" + str(count))
        except RuntimeError:
            print("R2_HOOK_LOG_LINES=UNAVAILABLE")
        print("R2_PACKAGE_LAUNCH_PASS")
        print("R2_LSPOSED_RUNTIME_INCONCLUSIVE: verify Vector scope, preference, and visual result manually")
        return 0
    except RuntimeError as exc:
        print("R2_DEVICE_PROBE_FAILED=" + str(exc), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
