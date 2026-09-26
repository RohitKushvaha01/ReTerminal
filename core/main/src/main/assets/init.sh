set -e  # Exit immediately on Failure

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export HOME=/root

if [ ! -s /etc/resolv.conf ]; then
    echo "nameserver 8.8.8.8" > /etc/resolv.conf
fi


export PS1='\[\033[01;32m\]\u@reterm\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
# shellcheck disable=SC2034
export PIP_BREAK_SYSTEM_PACKAGES=1

#fix linker warning
if [[ ! -f /linkerconfig/ld.config.txt ]];then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

if [ "$#" -eq 0 ]; then
    if [ ! -f /etc/reterm_provisioned ]; then
        echo "ReTerminal: first boot setup - apk update, apk upgrade, installing bash curl git"
        if apk update && apk upgrade && apk add bash curl git; then
            sed -i '/^root:/s|/bin/[a-z]*sh$|/bin/bash|' /etc/passwd
            touch /etc/reterm_provisioned
            echo "ReTerminal: first boot setup finished"
        else
            echo "ReTerminal: first boot setup failed, will retry next session"
        fi
    fi

    source /etc/profile
    export PS1='\[\033[01;32m\]\u@reterm\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
    cd $HOME
    if [ -f /initrc ]; then
        source /initrc
    fi

    shell_bin=""
    case "${RETERM_LOGIN_SHELL:-0}" in
        1)
            if [ ! -x /bin/bash ]; then
                apk add bash || true
            fi
            if [ -x /bin/bash ]; then
                shell_bin=/bin/bash
            else
                echo "ReTerminal: bash is not available, falling back to default shell"
            fi
            ;;
        2)
            shell_bin=/bin/sh
            ;;
        3)
            shell_bin=/bin/ash
            ;;
        *)
            if grep -q 'ID=wolfi' /etc/os-release; then
                shell_bin=/bin/sh
            else
                shell_bin=/bin/ash
            fi
            ;;
    esac
    if [ ! -x "$shell_bin" ]; then
        shell_bin=/bin/sh
    fi
    exec "$shell_bin"
else
    exec "$@"
fi