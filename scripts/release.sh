#!/usr/bin/env bash
# One-command release: pulls, bumps the version (1.0.1 -> 1.0.2), commits, tags and pushes.
# The pushed tag triggers .github/workflows/release.yml, which builds the APK and publishes the release.
# Usage: scripts/release.sh [patch|minor|major|X.Y.Z]   (default: patch)
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -z $(git status --porcelain) ]] || { echo "Working tree is not clean, commit or stash first." >&2; exit 1; }
BRANCH=$(git rev-parse --abbrev-ref HEAD)
git pull --rebase origin "$BRANCH"
VERSION=$(scripts/bump-version.sh "${1:-patch}")
git add version.properties
git commit -m "chore(release): v$VERSION"
git tag -a "v$VERSION" -m "ShellTerminal v$VERSION"
git push origin "$BRANCH" "v$VERSION"
echo "Released v$VERSION - follow the build in the Actions tab."
