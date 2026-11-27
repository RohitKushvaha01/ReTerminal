#!/usr/bin/env bash
# Usage: scripts/bump-version.sh [patch|minor|major|X.Y.Z]   (default: patch)
# Updates version.properties and prints the new version, e.g. 1.0.1 -> 1.0.2
set -euo pipefail
cd "$(dirname "$0")/.."
CUR=$(grep '^VERSION_NAME=' version.properties | cut -d= -f2 | tr -d '[:space:]')
IFS=. read -r MA MI PA <<<"$CUR"
case "${1:-patch}" in
  patch) PA=$((PA + 1)) ;;
  minor) MI=$((MI + 1)); PA=0 ;;
  major) MA=$((MA + 1)); MI=0; PA=0 ;;
  [0-9]*.[0-9]*.[0-9]*) IFS=. read -r MA MI PA <<<"$1" ;;
  *) echo "Unknown bump '$1' (use patch|minor|major|X.Y.Z)" >&2; exit 1 ;;
esac
NEW="$MA.$MI.$PA"
printf 'VERSION_NAME=%s\n' "$NEW" > version.properties
echo "$NEW"
