#!/usr/bin/env bash
set -euo pipefail

: "${KEYSTORE_B64:?KEYSTORE_B64 not set}"
: "${KS_PASS:?KS_PASS not set}"
: "${KEY_PASS:?KEY_PASS not set}"

cd "$(dirname "$0")/.."

if [ ! -f android/gradlew ]; then
  echo "[*] generating gradle wrapper"
  (cd android && gradle wrapper --gradle-version 8.11.1)
fi

cd android
./gradlew assembleRelease --no-daemon

echo "[+] built: app/build/outputs/apk/release/app-release.apk"
