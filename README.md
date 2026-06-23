# AndLinux

**AndLinux** — мобильная Linux-терминальная среда для Android без root-доступа. Проект ориентирован на стабильный запуск Alpine Linux через proot, нормальную работу экранной клавиатуры, гибкую настройку оболочки и полноценные темы интерфейса.

```text
Версия: 1.4.1
Статус: рабочая beta/stable-база
Разработчик: SiriLV
Лицензия: MIT
Root-доступ: не требуется
Основной Linux-дистрибутив: Alpine Linux
```

## Возможности

- Запуск Alpine Linux внутри Android через proot.
- Отдельный режим Android shell.
- Несколько терминальных сессий.
- Быстрое создание и закрытие сессий.
- Рабочий ввод с Android-клавиатуры без задержки до пробела.
- Виртуальные клавиши для терминала: `ESC`, `CTRL`, `ALT`, стрелки, `HOME`, `END`, `PGUP`, `PGDN`.
- Выбор shell для Alpine-сессий:
  - `ash`
  - `bash`
  - `fish`
  - `zsh`
- Запуск выбранного shell как login-shell.
- Автоматическая установка выбранного shell, если его ещё нет в Alpine.
- Настраиваемый размер текста терминала.
- Настраиваемый scrollback.
- Поддержка пользовательского шрифта.
- Поддержка пользовательского фона.
- Настройка прозрачности фона.
- Переключатели status bar, title bar и virtual keys.
- Настраиваемые keyboard shortcuts.
- Полноценные темы терминала и интерфейса приложения.

## Темы

AndLinux 1.4.1 поддерживает темы, которые меняют не только цвета текста в терминале, но и цвета всего интерфейса приложения: фон, карточки, акценты, панели, элементы настроек и системные bar-флаги.

Доступные темы:

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

Светлые темы используют тёмный терминальный foreground, чтобы текст оставался читаемым на светлом фоне. Тёмные темы используют собственные terminal color schemes с подходящими foreground/background/cursor цветами.

## Первый запуск: имя пользователя и hostname

При первом запуске Alpine AndLinux предлагает настроить видимое имя пользователя и hostname:

```text
AndLinux first setup

User name [user]: siri
Host name [andlinux]: okak

Saved identity: siri@okak
```

После настройки prompt выглядит примерно так:

```text
siri@okak:~#
```

Это меняет видимый prompt и переменные окружения внутри контейнера:

```text
USER
LOGNAME
HOSTNAME
/etc/hostname
/etc/hosts
~/.andlinux_identity
```

Фактический пользователь внутри proot остаётся root/fakeroot. Это нормально для текущей архитектуры Alpine-среды.

Повторно изменить имя пользователя и hostname можно командой:

```sh
andlinux-identity
```

## Shell selection

Открой настройки:

```text
Settings -> Default Shell
```

Выбери нужную оболочку:

```text
ash   лёгкая стандартная оболочка Alpine/BusyBox
bash  совместимость со скриптами и привычный GNU shell
fish  удобная интерактивная оболочка
zsh   гибкая настраиваемая оболочка
```

После смены shell открой новую сессию.

Проверка внутри Alpine:

```sh
echo "$SHELL"
echo "$0"
```

## Клавиатура

В AndLinux сохранён рабочий путь ввода из терминального backend. Отдельная настройка Input Mode убрана, потому что на некоторых Android-клавиатурах она вызывала задержку: команда появлялась только после нажатия пробела.

Проверочные команды:

```sh
exit
apk update
fastfetch
```

Текст должен появляться сразу во время ввода.

## Сборка APK

Основной workflow:

```text
.github/workflows/android.yml
```

Команда сборки:

```sh
./gradlew --no-daemon assembleFdroidRelease -x lintVitalFdroidRelease
```

Готовый artifact в GitHub Actions:

```text
Andlinux-apk/Andlinux.apk
```

Локальный путь после сборки:

```text
app/build/outputs/apk/Fdroid/release/*.apk
```

## Установка локально

После скачивания `Andlinux.apk` установи его как обычный APK-файл. Для обновления поверх предыдущей версии package name сохранён прежним:

```text
com.rk.terminal
```

## Roadmap

Ближайшие направления развития:

- Поддержка Arch Linux как отдельного rootfs-профиля.
- Поддержка Debian как отдельного rootfs-профиля.
- Менеджер дистрибутивов:
  - выбор профиля;
  - загрузка/import rootfs;
  - проверка checksum;
  - отдельная директория для каждого дистрибутива;
  - отдельный init-script;
  - reset/delete/export rootfs.
- Улучшение экрана первого запуска.
- Presets для dev-пакетов.
- Более аккуратный менеджер сессий.

Arch и Debian не добавлены в 1.4.1 намеренно: сначала зафиксирована стабильная Alpine-база с рабочим вводом, shell-переключением и темами.

## Лицензия

Проект распространяется под лицензией **MIT**. Подробности см. в файле [`LICENSE`](LICENSE).

## Developer

```text
SiriLV
```
