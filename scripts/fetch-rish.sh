#!/usr/bin/env bash
# Downloads the official rish files (shell via Shizuku) from the latest Shizuku release APK
# into core/main/src/main/assets. Apache-2.0, https://github.com/RikkaApps/Shizuku
set -euo pipefail
cd "$(dirname "$0")/.."
ASSETS=core/main/src/main/assets
if [[ -f $ASSETS/rish && -f $ASSETS/rish_shizuku.dex && ${1:-} != "--force" ]]; then
  echo "rish already present (use --force to refresh)"; exit 0
fi
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
AUTH=(); [[ -n ${GITHUB_TOKEN:-} ]] && AUTH=(-H "Authorization: Bearer $GITHUB_TOKEN")
URL=$(curl -fsSL "${AUTH[@]}" https://api.github.com/repos/RikkaApps/Shizuku/releases/latest \
  | grep -o '"browser_download_url": *"[^"]*\.apk"' | head -n1 | sed 's/.*: *"//; s/"$//')
[[ -n $URL ]] || { echo "Could not find the Shizuku APK URL" >&2; exit 1; }
echo "Downloading $URL"
curl -fsSL -o "$TMP/shizuku.apk" "$URL"
mkdir -p "$TMP/x" "$ASSETS"
unzip -q -o "$TMP/shizuku.apk" -d "$TMP/x"
RISH=$(find "$TMP/x" -type f -name rish | head -n1)
DEX=$(find "$TMP/x" -type f -name rish_shizuku.dex | head -n1)
[[ -n $RISH && -n $DEX ]] || { echo "rish / rish_shizuku.dex not found inside the Shizuku APK" >&2; exit 1; }
cp "$RISH" "$ASSETS/rish"; cp "$DEX" "$ASSETS/rish_shizuku.dex"
echo "rish installed into $ASSETS"
