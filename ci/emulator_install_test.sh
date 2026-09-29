#!/usr/bin/env bash
set -euo pipefail

APK="apk/N8N-Advanced-Mobile-v0.8.0.apk"
PKG="com.naten.advancedmobile.v8"

adb wait-for-device
echo "Android API: $(adb shell getprop ro.build.version.sdk)"

for i in $(seq 1 20); do
  echo "=== INSTALL TEST PASS $i ==="
  adb uninstall "$PKG" >/dev/null 2>&1 || true
  adb install "$APK"
  adb shell pm path "$PKG"
  adb shell monkey -p "$PKG" 1 >/dev/null
  adb shell am force-stop "$PKG"
done

echo "20 install/launch/uninstall passes completed successfully."
