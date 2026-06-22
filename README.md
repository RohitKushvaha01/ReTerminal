# AndLinux

**AndLinux** is a mobile Linux terminal environment for Android. It is based on the original ReTerminal codebase, but this fork focuses on a cleaner Alpine/proot workflow, stable keyboard input, practical shell selection and an installable alpha build.

The project is designed for phones without root access.

## Current status

```text
Channel: Alpha
Version: 1.3.0-alpha
Default Linux environment: Alpine Linux
Root required: No
```

## Features

- Material 3 terminal interface.
- Alpine Linux environment through proot.
- Android shell mode.
- Multiple terminal sessions.
- Virtual keys for terminal work.
- Configurable keyboard shortcuts.
- Custom font support.
- Custom background support.
- Terminal font size configuration.
- Status bar, title bar and virtual key toggles.
- Default shell selector for Alpine sessions:
  - `ash`
  - `bash`
  - `fish`
  - `zsh`
- Login-shell startup for the selected shell.
- Fixed Alpine startup script refresh from bundled assets.
- btop-compatible `/proc/stat` shim for Android/proot environments.

## Keyboard behavior

The input mode selector was removed from this fork. AndLinux keeps the original ReTerminal terminal input behavior because it commits typed characters immediately on Android keyboards that otherwise buffer words until `Space` is pressed.

This is important for terminal commands such as:

```sh
exit
apk update
btop
```

## Shell selection

Open:

```text
Settings -> Default Shell
```

Choose one of:

```text
ash   lightweight BusyBox shell
bash  best script compatibility
fish  modern interactive shell
zsh   configurable power shell
```

Open a new terminal session after changing the shell.

Inside Alpine, verify the active shell:

```sh
echo "$SHELL"
echo "$0"
```

## btop note

Android/proot can expose a `/proc/stat` format that some Linux tools do not parse correctly. AndLinux now binds a small generated `/proc/stat` file into the Alpine environment so tools such as `btop` can start instead of failing with:

```text
Failed to parse /proc/stat
```

The shim is compatibility-focused. It is not intended to be a perfect replacement for a native kernel procfs.

## Building alpha APK

The repository includes a GitHub Actions workflow:

```text
.github/workflows/debug-apk.yml
```

It builds an installable alpha APK:

```sh
./gradlew --no-daemon assembleFdroidAlpha
```

The uploaded artifact is named:

```text
Andlinux.apk
```

Manual local build:

```sh
chmod +x gradlew
./gradlew --no-daemon assembleFdroidAlpha
```

APK path:

```text
app/build/outputs/apk/Fdroid/alpha/*.apk
```

## Roadmap

- Distribution manager for more rootfs profiles.
- Safer rootfs reset/import/export tools.
- Built-in terminal color theme presets.
- Better first-run setup screen.
- Optional package bootstrap presets for developer tools.

## License

This fork keeps the upstream project licensing terms. See `LICENSE`.
