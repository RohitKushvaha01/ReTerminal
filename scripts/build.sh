#!/usr/bin/env bash
# Local release build -> out/ShellTerminal-v<version>.apk
# Signs with signing.properties in the repo root if present, otherwise with the test key.
set -euo pipefail
cd "$(dirname "$0")/.."
scripts/fetch-rish.sh
chmod +x gradlew
./gradlew assembleRelease
VERSION=$(grep '^VERSION_NAME=' version.properties | cut -d= -f2)
mkdir -p out
APK=$(ls app/build/outputs/apk/release/*.apk | head -n1)
cp "$APK" "out/ShellTerminal-v$VERSION.apk"
echo "Built out/ShellTerminal-v$VERSION.apk"
