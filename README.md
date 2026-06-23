# AndLinux

**AndLinux** is a mobile Linux terminal environment for Android. It is based on ReTerminal and focuses on a clean Alpine/proot workflow, stable mobile keyboard input, shell selection and an installable beta APK.

## Status

```text
Channel: Beta
Version: 1.4.0-beta
Default Linux environment: Alpine Linux
Root required: No
```

## Features

- Material 3 terminal interface.
- Alpine Linux through proot.
- Android shell mode.
- Multiple sessions.
- Virtual terminal keys.
- Configurable keyboard shortcuts.
- Custom font and background support.
- Status bar, title bar and virtual key toggles.
- Alpine shell selector: `ash`, `bash`, `fish`, `zsh`.
- Login-shell startup for the selected shell.
- Refreshed bundled startup scripts on app update.
- Safer `/proc/stat` compatibility layer for Android/proot.

## Keyboard behavior

AndLinux intentionally keeps the original terminal input path. The separate Input Mode selector was removed because it made some Android keyboards buffer words until `Space` was pressed.

## Shell selection

Open:

```text
Settings -> Default Shell
```

Choose `ash`, `bash`, `fish`, or `zsh`, then open a new session.

## btop note

`btop` can still be device-dependent under Android/proot. The current build avoids the noisy background updater and provides a static compatibility `/proc/stat` file. If `btop` is still unreliable on a device, use:

```sh
htop
top
free -h
vmstat
```

## Debian and Arch plan

Debian and Arch should be added through a real distribution manager, not through fake buttons. The planned flow is:

```text
profile -> rootfs import/download -> checksum check -> per-distro directory -> proot launch
```

The beta keeps Alpine as the only bundled environment so the app stays stable.

## Build beta APK

GitHub Actions workflow:

```text
.github/workflows/debug-apk.yml
```

Build command:

```sh
./gradlew --no-daemon assembleFdroidBeta
```

Output artifact:

```text
Andlinux.apk
```

Local output path:

```text
app/build/outputs/apk/Fdroid/beta/*.apk
```
