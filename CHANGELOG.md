# Changelog

## 1.4.1 — Stable base

Stable Alpine-based AndLinux snapshot.

### Project

- Version fixed as `1.4.1`.
- Developer: `SiriLV`.
- License: `MIT`.
- Package ID changed to `com.term.andlinux`.
- Release and debug app name set to `AndLinux`.

### README

- README is now bilingual: Russian and English.
- Removed the old keyboard/Input Mode troubleshooting block.
- Removed low-level foreground/background/cursor implementation details.
- Added cleaner theme documentation without internal technical notes.
- Added Package ID information.
- Added first-launch user/hostname setup documentation.
- Added shell selection documentation.
- Added Arch/Debian roadmap as future work.

### Terminal environment

- Alpine Linux remains the stable default environment.
- `ash`, `bash`, `fish`, and `zsh` are available through shell selection.
- First-run identity setup configures visible user and hostname.
- Android/proot limitations are accepted for tools that require real `/proc` or `/sys` access.

### Themes

- Themes are applied to the terminal and main app UI.
- Built-in themes include Default, Dracula, Nord, Solarized, Gruvbox, One Dark, Tokyo Night, Catppuccin, Monokai, Material Dark, and Ayu variants.

### Future

- Arch Linux support as a separate rootfs profile.
- Debian support as a separate rootfs profile.
- Distribution manager for profile selection, rootfs import/download, checksum validation, reset/delete/export, and separate init scripts.
