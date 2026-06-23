# AndLinux

[Русский](#русский) | [English](#english)

---

## Русский

**AndLinux** — мобильная Linux-терминальная среда для Android без root-доступа.

Проект делает упор на стабильный запуск Alpine Linux через proot, удобную работу с Android-клавиатурой, выбор shell и полноценные цветовые темы для терминала и интерфейса приложения.

```text
Версия: 1.4.1
Разработчик: SiriLV
Лицензия: MIT
Package ID: com.term.andlinux
Root-доступ: не требуется
Основная среда: Alpine Linux
```

### Возможности

- Alpine Linux внутри Android через proot.
- Отдельный режим Android shell.
- Несколько терминальных сессий.
- Виртуальные терминальные клавиши: `ESC`, `CTRL`, `ALT`, стрелки, `HOME`, `END`, `PGUP`, `PGDN`.
- Выбор shell для Alpine:
  - `ash`
  - `bash`
  - `fish`
  - `zsh`
- Запуск выбранного shell как login-shell.
- Автоматическая установка выбранного shell, если его ещё нет в Alpine.
- Настраиваемый размер текста терминала.
- Настраиваемый scrollback.
- Пользовательский шрифт.
- Пользовательский фон.
- Прозрачность фона.
- Переключатели status bar, title bar и virtual keys.
- Настраиваемые keyboard shortcuts.
- Темы, которые меняют терминал и интерфейс приложения.

### Темы

AndLinux 1.4.1 поддерживает набор встроенных цветовых схем:

```text
Default
Dracula
Nord
Solarized Dark
Solarized Light
Gruvbox Dark
Gruvbox Light
One Dark
Tokyo Night
Tokyo Night Light
Catppuccin Mocha
Catppuccin Latte
Monokai
Material Dark
Ayu Dark
Ayu Light
```

Темы применяются к терминалу и основному UI: экрану сессий, настройкам, карточкам, панелям, акцентным цветам и системным bar-флагам.

### Первый запуск: user и hostname

При первом запуске Alpine приложение предлагает задать видимое имя пользователя и hostname:

```text
AndLinux first setup

User name [user]: siri
Host name [andlinux]: okak

Saved identity: siri@okak
```

После этого prompt будет выглядеть примерно так:

```text
siri@okak:~#
```

Повторно изменить имя можно командой:

```sh
andlinux-identity
```

Настройка меняет prompt, `/etc/hostname`, `/etc/hosts` и переменные окружения внутри Alpine. Среда по-прежнему работает через proot.

### Shell

Открой:

```text
Settings -> Default Shell
```

Выбери `ash`, `bash`, `fish` или `zsh`, затем открой новую сессию.

Проверка:

```sh
echo "$SHELL"
echo "$0"
```

### Сборка APK

Основной workflow:

```text
.github/workflows/android.yml
```

Команда сборки:

```sh
./gradlew --no-daemon assembleFdroidRelease -x lintVitalFdroidRelease
```

Artifact в GitHub Actions:

```text
Andlinux-apk/Andlinux.apk
```

Локальный путь после сборки:

```text
app/build/outputs/apk/Fdroid/release/*.apk
```

### Roadmap

Следующие направления:

- Поддержка Arch Linux как отдельного rootfs-профиля.
- Поддержка Debian как отдельного rootfs-профиля.
- Менеджер дистрибутивов:
  - выбор профиля;
  - загрузка или import rootfs;
  - checksum-проверка;
  - отдельная директория для каждого дистрибутива;
  - отдельный init-script;
  - reset/delete/export rootfs.
- Улучшение первого setup-экрана.
- Presets для dev-пакетов.
- Более аккуратный менеджер сессий.

Arch и Debian не входят в версию 1.4.1. Сначала зафиксирована стабильная Alpine-база.

### Лицензия

MIT. См. [`LICENSE`](LICENSE).

---

## English

**AndLinux** is a mobile Linux terminal environment for Android without root access.

The project focuses on stable Alpine Linux startup through proot, practical Android keyboard behavior, shell selection, and full color themes for both the terminal and the app UI.

```text
Version: 1.4.1
Developer: SiriLV
License: MIT
Package ID: com.term.andlinux
Root access: not required
Main environment: Alpine Linux
```

### Features

- Alpine Linux on Android through proot.
- Separate Android shell mode.
- Multiple terminal sessions.
- Virtual terminal keys: `ESC`, `CTRL`, `ALT`, arrows, `HOME`, `END`, `PGUP`, `PGDN`.
- Alpine shell selector:
  - `ash`
  - `bash`
  - `fish`
  - `zsh`
- Login-shell startup for the selected shell.
- Automatic installation of the selected shell when it is missing from Alpine.
- Configurable terminal text size.
- Configurable scrollback.
- Custom font support.
- Custom background support.
- Background transparency setting.
- Status bar, title bar, and virtual keys toggles.
- Configurable keyboard shortcuts.
- Themes for the terminal and the app interface.

### Themes

AndLinux 1.4.1 includes built-in color schemes:

```text
Default
Dracula
Nord
Solarized Dark
Solarized Light
Gruvbox Dark
Gruvbox Light
One Dark
Tokyo Night
Tokyo Night Light
Catppuccin Mocha
Catppuccin Latte
Monokai
Material Dark
Ayu Dark
Ayu Light
```

Themes are applied to the terminal and the main UI: session drawer, settings, cards, panels, accent colors, and system bar flags.

### First launch: user and hostname

On the first Alpine launch, AndLinux asks for the visible user name and hostname:

```text
AndLinux first setup

User name [user]: siri
Host name [andlinux]: okak

Saved identity: siri@okak
```

The prompt will then look like this:

```text
siri@okak:~#
```

You can change it later with:

```sh
andlinux-identity
```

This changes the prompt, `/etc/hostname`, `/etc/hosts`, and environment variables inside Alpine. The environment still runs through proot.

### Shell

Open:

```text
Settings -> Default Shell
```

Choose `ash`, `bash`, `fish`, or `zsh`, then open a new session.

Check inside Alpine:

```sh
echo "$SHELL"
echo "$0"
```

### Building the APK

Main workflow:

```text
.github/workflows/android.yml
```

Build command:

```sh
./gradlew --no-daemon assembleFdroidRelease -x lintVitalFdroidRelease
```

GitHub Actions artifact:

```text
Andlinux-apk/Andlinux.apk
```

Local APK path:

```text
app/build/outputs/apk/Fdroid/release/*.apk
```

### Roadmap

Planned next steps:

- Arch Linux support as a separate rootfs profile.
- Debian support as a separate rootfs profile.
- Distribution manager:
  - profile selection;
  - rootfs download or import;
  - checksum validation;
  - separate directory per distribution;
  - separate init script;
  - reset/delete/export rootfs.
- Improved first-run setup screen.
- Developer package presets.
- Cleaner session manager.

Arch and Debian are not included in 1.4.1. This version first locks down the stable Alpine base.

### License

MIT. See [`LICENSE`](LICENSE).
