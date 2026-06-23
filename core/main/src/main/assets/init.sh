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

sanitize_name() {
    cleaned=$(printf "%s" "$1" | tr -cd 'A-Za-z0-9_-' | cut -c1-32)
    [ -n "$cleaned" ] && printf "%s" "$cleaned"
}

write_identity_files() {
    user_name=$(sanitize_name "$1")
    host_name=$(sanitize_name "$2")
    [ -z "$user_name" ] && user_name="user"
    [ -z "$host_name" ] && host_name="andlinux"

    cat > "$HOME/.andlinux_identity" <<EOF
ANDLINUX_USER='$user_name'
ANDLINUX_HOST='$host_name'
EOF

    echo "$host_name" > /etc/hostname
    cat > /etc/hosts <<EOF
127.0.0.1 localhost $host_name
::1 localhost $host_name
EOF

    mkdir -p /etc/profile.d
    cat > /etc/profile.d/andlinux-identity.sh <<EOF
export ANDLINUX_USER='$user_name'
export ANDLINUX_HOST='$host_name'
export USER='$user_name'
export LOGNAME='$user_name'
export HOSTNAME='$host_name'
case "\$-" in
  *i*) PS1='$user_name@$host_name:\\w# ' ;;
esac
EOF

    cat > "$HOME/.bash_profile" <<EOF
[ -f /etc/profile ] && . /etc/profile
[ -f ~/.bashrc ] && . ~/.bashrc
EOF

    cat > "$HOME/.zprofile" <<EOF
export ANDLINUX_USER='$user_name'
export ANDLINUX_HOST='$host_name'
export USER='$user_name'
export LOGNAME='$user_name'
export HOSTNAME='$host_name'
PROMPT='$user_name@$host_name:%~# '
EOF

    mkdir -p "$HOME/.config/fish"
    cat > "$HOME/.config/fish/config.fish" <<EOF
set -gx ANDLINUX_USER '$user_name'
set -gx ANDLINUX_HOST '$host_name'
set -gx USER '$user_name'
set -gx LOGNAME '$user_name'
set -gx HOSTNAME '$host_name'
function fish_prompt
    set_color cyan
    printf '$user_name'
    set_color normal
    printf '@'
    set_color green
    printf '$host_name'
    set_color normal
    printf ':'
    set_color blue
    prompt_pwd
    set_color normal
    printf '# '
end
EOF
}

install_identity_tool() {
    cat > /usr/local/bin/andlinux-identity <<'EOF'
#!/bin/sh
sanitize_name() {
    cleaned=$(printf "%s" "$1" | tr -cd 'A-Za-z0-9_-' | cut -c1-32)
    [ -n "$cleaned" ] && printf "%s" "$cleaned"
}

write_identity_files() {
    user_name=$(sanitize_name "$1")
    host_name=$(sanitize_name "$2")
    [ -z "$user_name" ] && user_name="user"
    [ -z "$host_name" ] && host_name="andlinux"

    cat > "$HOME/.andlinux_identity" <<EOT
ANDLINUX_USER='$user_name'
ANDLINUX_HOST='$host_name'
EOT
    echo "$host_name" > /etc/hostname
    cat > /etc/hosts <<EOT
127.0.0.1 localhost $host_name
::1 localhost $host_name
EOT
    mkdir -p /etc/profile.d
    cat > /etc/profile.d/andlinux-identity.sh <<EOT
export ANDLINUX_USER='$user_name'
export ANDLINUX_HOST='$host_name'
export USER='$user_name'
export LOGNAME='$user_name'
export HOSTNAME='$host_name'
case "\$-" in
  *i*) PS1='$user_name@$host_name:\\w# ' ;;
esac
EOT
    cat > "$HOME/.bash_profile" <<EOT
[ -f /etc/profile ] && . /etc/profile
[ -f ~/.bashrc ] && . ~/.bashrc
EOT
    cat > "$HOME/.zprofile" <<EOT
export ANDLINUX_USER='$user_name'
export ANDLINUX_HOST='$host_name'
export USER='$user_name'
export LOGNAME='$user_name'
export HOSTNAME='$host_name'
PROMPT='$user_name@$host_name:%~# '
EOT
    mkdir -p "$HOME/.config/fish"
    cat > "$HOME/.config/fish/config.fish" <<EOT
set -gx ANDLINUX_USER '$user_name'
set -gx ANDLINUX_HOST '$host_name'
set -gx USER '$user_name'
set -gx LOGNAME '$user_name'
set -gx HOSTNAME '$host_name'
function fish_prompt
    set_color cyan
    printf '$user_name'
    set_color normal
    printf '@'
    set_color green
    printf '$host_name'
    set_color normal
    printf ':'
    set_color blue
    prompt_pwd
    set_color normal
    printf '# '
end
EOT
}

printf 'AndLinux identity setup\n'
printf 'User name [user]: '
read -r user_name
printf 'Host name [andlinux]: '
read -r host_name
[ -z "$user_name" ] && user_name=user
[ -z "$host_name" ] && host_name=andlinux
write_identity_files "$user_name" "$host_name"
printf 'Saved. Open a new session or run: exec $SHELL -l\n'
EOF
    chmod 755 /usr/local/bin/andlinux-identity
}

first_identity_setup() {
    install_identity_tool
    if [ ! -f "$HOME/.andlinux_identity" ] && [ -t 0 ]; then
        printf '\nAndLinux first setup\n'
        printf 'This changes the visible terminal prompt only. The Linux container still runs through proot.\n\n'
        printf 'User name [user]: '
        read -r user_name
        printf 'Host name [andlinux]: '
        read -r host_name
        [ -z "$user_name" ] && user_name=user
        [ -z "$host_name" ] && host_name=andlinux
        write_identity_files "$user_name" "$host_name"
        printf '\nSaved identity: %s@%s\n\n' "$(sanitize_name "$user_name")" "$(sanitize_name "$host_name")"
    elif [ -f "$HOME/.andlinux_identity" ]; then
        . "$HOME/.andlinux_identity"
        write_identity_files "$ANDLINUX_USER" "$ANDLINUX_HOST"
    fi
}

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
first_identity_setup

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
