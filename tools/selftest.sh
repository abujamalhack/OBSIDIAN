#!/usr/bin/env bash
set -uo pipefail

PKG="ai.obsidian.agent"
SERIAL="${1:-}"
PASS=0
FAIL=0

if [ -n "$SERIAL" ]; then
  A() { adb -s "$SERIAL" "$@"; }
else
  A() { adb "$@"; }
fi

t() {
  local name="$1"; shift
  if "$@" >/dev/null 2>&1; then
    echo "PASS $name"; PASS=$((PASS + 1))
  else
    echo "FAIL $name"; FAIL=$((FAIL + 1))
  fi
}

installed()   { A shell pm list packages | grep -q "package:$PKG"; }
owner()       { A shell dpm list-owners | grep -q "$PKG"; }
granted()     { A shell dumpsys package "$PKG" | grep -q "android.permission.$1: granted=true"; }
svc()         { A shell dumpsys activity services "$PKG" | grep -q "$1"; }
whitelisted() { A shell dumpsys deviceidle whitelist | grep -q "$PKG"; }
listener()    { A shell cmd notification listeners | grep -q "$PKG"; }
a11y()        { A shell settings get secure enabled_accessibility_services | grep -q "$PKG"; }
no_crash() {
  A logcat -c
  sleep 45
  ! A logcat -d -s AndroidRuntime:E | grep -q "$PKG"
}

t "installed"        installed
t "device-owner"     owner
t "perm.camera"      granted CAMERA
t "perm.audio"       granted RECORD_AUDIO
t "perm.location"    granted ACCESS_FINE_LOCATION
t "perm.bg-location" granted ACCESS_BACKGROUND_LOCATION
t "perm.sms"         granted READ_SMS
t "service-c2"       svc C2Service
t "service-guard"    svc GuardService
t "doze-exempt"      whitelisted
t "notif-listener"   listener
t "a11y-enabled"     a11y
t "no-crash-45s"     no_crash

echo "----------------------------------------"
echo "$PASS passed / $FAIL failed"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
