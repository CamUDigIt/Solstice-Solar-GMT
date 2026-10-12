#!/usr/bin/env bash
# Regenerate, validate, build, install on a Wear OS 5.1+ device and screenshot.
#   tools/preview.sh [adb-serial] [out.png]
set -euo pipefail
cd "$(dirname "$0")/.."

SERIAL="${1:-emulator-5554}"
OUT="${2:-tools/out/screen.png}"
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
PKG=com.solstice.gmt.debug

java tools/HandArt.java > /dev/null
java tools/WatchFace.java
java -cp "tools/wff/lib/*" tools/Validate.java
./gradlew assembleDebug -q --console=plain

"$ADB" -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk > /dev/null
"$ADB" -s "$SERIAL" shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE \
    --es operation set-watchface --es watchFaceId "$PKG" > /dev/null
sleep 4
"$ADB" -s "$SERIAL" shell input keyevent KEYCODE_BACK  # dismiss any overlay (e.g. Agenda)
sleep 2
mkdir -p "$(dirname "$OUT")"
"$ADB" -s "$SERIAL" exec-out screencap -p > "$OUT"
echo "Screenshot: $OUT"
