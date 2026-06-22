#!/bin/sh
set -e

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export HOME=/root
export LANG=C.UTF-8
export LC_ALL=C.UTF-8
export PIP_BREAK_SYSTEM_PACKAGES=1

mkdir -p "$HOME"

if [ ! -s /etc/resolv.conf ]; then
    echo "nameserver 8.8.8.8" > /etc/resolv.conf
    echo "nameserver 8.8.4.4" >> /etc/resolv.conf
fi

install_packages() {
    missing=""
    for pkg in "$@"; do
        if ! apk info -e "$pkg" >/dev/null 2>&1; then
            missing="$missing $pkg"
        fi
    done

    if [ -n "$missing" ]; then
        echo -e "\e[34;1m[*]\e[0m Installing packages:$missing"
        apk update
        apk add $missing
        echo -e "\e[32;1m[+]\e[0m Packages installed"
    fi
}

# Base compatibility packages. Keep this list small; optional shells are installed only when selected.
install_packages bash gcompat glib nano

# Fix Android linker warning in some proot builds.
if [ ! -f /linkerconfig/ld.config.txt ]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

select_shell() {
    case "${RET_DEFAULT_SHELL:-ash}" in
        ash|sh)
            SELECTED_SHELL=/bin/ash
            SELECTED_PKG=""
            ;;
        bash)
            SELECTED_SHELL=/bin/bash
            SELECTED_PKG=bash
            ;;
        fish)
            SELECTED_SHELL=/usr/bin/fish
            SELECTED_PKG=fish
            ;;
        zsh)
            SELECTED_SHELL=/bin/zsh
            SELECTED_PKG=zsh
            ;;
        *)
            echo "Unknown shell '${RET_DEFAULT_SHELL}', falling back to ash"
            SELECTED_SHELL=/bin/ash
            SELECTED_PKG=""
            ;;
    esac
}

select_shell

if [ -n "$SELECTED_PKG" ] && [ ! -x "$SELECTED_SHELL" ]; then
    if ! install_packages "$SELECTED_PKG"; then
        echo "Failed to install shell package '$SELECTED_PKG', falling back to ash"
        SELECTED_SHELL=/bin/ash
    fi
fi

if [ ! -x "$SELECTED_SHELL" ]; then
    echo "Shell '$SELECTED_SHELL' is not executable, falling back to ash"
    SELECTED_SHELL=/bin/ash
fi

export SHELL="$SELECTED_SHELL"
cd "$HOME"

if [ "$#" -eq 0 ]; then
    exec "$SELECTED_SHELL" -l
else
    exec "$@"
fi
