# ShellTerminal
**ShellTerminal** is a sleek, Material 3-inspired terminal emulator that gives you a shell with **Shizuku** (adb-level) privileges, or a plain Android shell. Built on [Termux's](https://github.com/termux/termux-app) robust TerminalView.

Download the latest APK from the [Releases Section](https://github.com/venxsa/ShellTerminal/releases/latest).

# Features
- [x] Basic Terminal
- [x] Virtual Keys
- [x] Multiple Sessions
- [x] Shell via Shizuku (rish)
- [x] Configurable Keyboard Shortcuts (Paste, Session Management)

# Requirements
[Shizuku](https://github.com/RikkaApps/Shizuku) must be running; ShellTerminal asks for its permission on start.

# Building
```sh
scripts/build.sh            # -> out/ShellTerminal-v<version>.apk
```
`scripts/fetch-rish.sh` downloads the official `rish` files from the Shizuku release (done automatically by build.sh and CI).

# Releasing
- Locally: `scripts/release.sh` (bumps patch 1.0.1 -> 1.0.2, tags, pushes; `minor`, `major` or `X.Y.Z` also work).
- On GitHub: Actions -> **Release** -> Run workflow (same bump, builds and publishes the APK).

The version lives in `version.properties`. Optional signing secrets: `KEYSTORE` and `PROP` (base64); without them the test key is used.

# Screenshots
<div>
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" width="32%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/02.jpg" width="32%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/03.jpg" width="32%" />
</div>

Based on [ReTerminal](https://github.com/RohitKushvaha01/ReTerminal).
