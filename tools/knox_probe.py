#!/usr/bin/env python3
import subprocess
import sys

PROPS = [
    "ro.config.knox",
    "ro.boot.warranty_bit",
    "ro.boot.kg.state",
    "ro.boot.verifiedbootstate",
    "ro.boot.flash.locked",
    "ro.csc.sales_code",
    "ro.build.version.security_patch",
    "ro.vendor.build.security_patch",
]

PACKAGES = [
    "com.samsung.android.knox.attestation",
    "com.samsung.android.knox.push",
    "com.samsung.android.kgclient",
    "com.samsung.android.knox.containeragent",
    "com.samsung.android.bbc.bbcagent",
]


def sh(cmd: list[str]) -> str:
    return subprocess.run(cmd, capture_output=True, text=True).stdout.strip()


def main() -> int:
    serial = sys.argv[1] if len(sys.argv) > 1 else None
    base = ["adb"] + (["-s", serial] if serial else [])

    print("== props ==")
    for p in PROPS:
        print(f"{p} = {sh(base + ['shell', 'getprop', p]) or '-'}")

    print("\n== knox packages ==")
    for pkg in PACKAGES:
        present = pkg in sh(base + ["shell", "pm", "list", "packages"])
        print(f"{pkg}: {'present' if present else 'absent'}")

    print("\n== knox api surface ==")
    print(sh(base + ["shell", "dumpsys", "knox_ccm"]) or "-")

    print("\n== containers ==")
    print(sh(base + ["shell", "dumpsys", "persona"]) or "-")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
