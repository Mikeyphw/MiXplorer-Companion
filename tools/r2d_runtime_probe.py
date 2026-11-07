#!/usr/bin/env python3
"""R2D: safe physical-device preflight and optional opt-in hook-log corroboration.

Never installs, force-stops, clears data/logs, captures screenshots, or reads
MiXplorer files. No success outcome from this program proves UI hook injection.
"""
import argparse
import re
import subprocess
import sys
from pathlib import Path

BETA = "com.mixplorer.beta"
COMPANION = "io.github.mikeyphw.mixplorercompanion"
REQUIRED_BETA = 26090412
MIN_COMPANION = 3
COMPANION_ACTIVITY = COMPANION + "/.MainActivity"
BETA_ACTIVITY = BETA + "/com.mixplorer.activities.BrowseActivity"
MARKERS = ("R2D_HOOK_INSTALLED", "R2D_CALLBACK_REACHED",
           "R2D_EXPERIMENT_DISABLED", "R2D_APPEARANCE_REQUESTED")


def extract_version(output):
    match = re.search(r"\bversionCode=(\d+)\b", output)
    return int(match.group(1)) if match else None


def event_counts(raw):
    # Count only exact, fixed markers from the companion; never print raw logs.
    return {marker: sum(1 for line in raw.splitlines() if marker in line)
            for marker in MARKERS}


def approved(args):
    if args in (("get-state",),
                ("shell", "getprop", "ro.kernel.qemu"),
                ("shell", "getprop", "ro.build.version.sdk")):
        return True
    if len(args) == 4 and args[:3] == ("shell", "pm", "path") and args[3] in (BETA, COMPANION):
        return True
    if len(args) == 4 and args[:3] == ("shell", "dumpsys", "package") and args[3] in (BETA, COMPANION):
        return True
    if args == ("shell", "logcat", "-d", "-t", "2000", "-v", "brief", "-s", "MiXCompanionR2:V", "*:S"):
        return True
    if len(args) == 6 and args[:4] == ("shell", "am", "start", "-n") and args[4:] in (
            (COMPANION_ACTIVITY, "-W"), (BETA_ACTIVITY, "-W")):
        return True
    return False


def adb(serial, *args):
    if not serial or any(c.isspace() for c in serial):
        raise RuntimeError("R2D_INVALID_SERIAL")
    if not approved(args):
        raise RuntimeError("R2D_COMMAND_NOT_ALLOWED")
    try:
        process = subprocess.run(["adb", "-s", serial, *args],
                                 capture_output=True, text=True, timeout=25,
                                 check=False)
    except (FileNotFoundError, subprocess.TimeoutExpired) as err:
        raise RuntimeError("R2D_ADB_UNAVAILABLE") from err
    if process.returncode != 0:
        # Do not leak remote stderr (may include paths or private data).
        raise RuntimeError("R2D_ADB_COMMAND_FAILED:" + args[0])
    return process.stdout.strip()


def version_for(serial, package):
    found = adb(serial, "shell", "pm", "path", package)
    if not any(line.startswith("package:") for line in found.splitlines()):
        return None
    return extract_version(adb(serial, "shell", "dumpsys", "package", package))


def probe(serial, *, logs=False, open_companion=False, open_beta=False):
    results = ["R2D_RECEIPT_SCHEMA=1"]
    if adb(serial, "get-state") != "device":
        raise RuntimeError("R2D_DEVICE_NOT_READY")
    # This is a device-only preflight; reject the hosted emulator explicitly.
    if adb(serial, "shell", "getprop", "ro.kernel.qemu") == "1":
        raise RuntimeError("R2D_EMULATOR_REFUSED")
    api = adb(serial, "shell", "getprop", "ro.build.version.sdk")
    if not api.isdigit():
        raise RuntimeError("R2D_DEVICE_API_UNKNOWN")
    beta = version_for(serial, BETA)
    companion = version_for(serial, COMPANION)
    results += ["R2D_ANDROID_API=" + api,
                "R2D_BETA_CODE=" + (str(beta) if beta is not None else "MISSING"),
                "R2D_COMPANION_CODE=" + (str(companion) if companion is not None else "MISSING")]
    if beta != REQUIRED_BETA:
        results.append("R2D_PREFLIGHT_REFUSED_UNSUPPORTED_BETA")
        return results, 2
    if companion is None or companion < MIN_COMPANION:
        results.append("R2D_PREFLIGHT_REFUSED_COMPANION_NOT_R2D")
        return results, 2
    if open_companion:
        adb(serial, "shell", "am", "start", "-n", COMPANION_ACTIVITY, "-W")
        results.append("R2D_COMPANION_LAUNCH_REQUESTED")
    if open_beta:
        adb(serial, "shell", "am", "start", "-n", BETA_ACTIVITY, "-W")
        results.append("R2D_BETA_LAUNCH_REQUESTED")
    if logs:
        try:
            raw = adb(serial, "shell", "logcat", "-d", "-t", "2000",
                      "-v", "brief", "-s", "MiXCompanionR2:V", "*:S")
            for marker, count in event_counts(raw).items():
                results.append(marker + "_LOG_COUNT=" + str(count))
        except RuntimeError:
            results.append("R2D_LOG_MARKERS=UNAVAILABLE")
    else:
        results.append("R2D_LOG_MARKERS=NOT_REQUESTED")
    results += ["R2D_PREFLIGHT_PASS", "R2D_RUNTIME_INCONCLUSIVE",
                "R2D_REQUIRED=MANUAL_VISUAL_AND_ROLLBACK_ACCEPTANCE"]
    return results, 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", default="localhost:5555")
    parser.add_argument("--read-logs", action="store_true",
                        help="Read only the module's filtered logcat tag; emit counts, never raw logs")
    parser.add_argument("--open-companion", action="store_true", help="Explicitly open installed Companion")
    parser.add_argument("--open-beta", action="store_true", help="Explicitly open installed Beta; never force-stop")
    parser.add_argument("--output", type=Path, help="Save only the sanitized receipt to a local text file")
    args = parser.parse_args(argv)
    try:
        rows, code = probe(args.serial, logs=args.read_logs,
                           open_companion=args.open_companion, open_beta=args.open_beta)
    except RuntimeError as error:
        rows, code = [str(error), "R2D_RUNTIME_INCONCLUSIVE"], 2
    receipt = "\n".join(rows) + "\n"
    print(receipt, end="")
    if args.output is not None:
        # Create output only on an explicit request, never on the target device.
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(receipt, encoding="utf-8")
    return code


if __name__ == "__main__":
    sys.exit(main())
