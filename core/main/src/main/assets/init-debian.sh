set -e  # Exit immediately on Failure

export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export HOME=/root
export DEBIAN_FRONTEND=noninteractive

# Setup /etc/resolv.conf if it's missing or empty
if [ ! -s /etc/resolv.conf ]; then
    echo "nameserver 8.8.8.8" > /etc/resolv.conf
    echo "nameserver 8.8.4.4" >> /etc/resolv.conf
fi

# Basic mount points expected in a Debian system
mkdir -p /dev/pts
mkdir -p /proc
mount -t devpts devpts /dev/pts -o gid=5,mode=620 || true
mount -t proc proc /proc || true


cd "$XPWD"
export PS1='${debian_chroot:+($debian_chroot)}\[\033[01;32m\]\u@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '

# Fix linker warning if directory doesn't exist (common in minimal chroots)
if [ ! -d /linkerconfig ]; then
    mkdir -p /linkerconfig
fi
if [ ! -f /linkerconfig/ld.config.txt ];then
    touch /linkerconfig/ld.config.txt
fi

# Check if bash is installed, if not, try to use sh.
# A more robust solution would be to ensure the rootfs has a known shell.
PREFERRED_SHELL="/bin/bash"
FALLBACK_SHELL="/bin/sh"

# Attempt to update package list and install essential packages if missing
# This is optional and might be slow on first startup.
# Consider pre-installing these in the rootfs.
# if ! dpkg -s bash nano sudo > /dev/null 2>&1; then
#    echo "Attempting to install essential packages (bash, nano, sudo)..."
#    apt-get update || echo "apt-get update failed. Continuing..."
#    apt-get install -y bash nano sudo || echo "apt-get install failed. Some packages might be missing."
# fi


if [ "$#" -eq 0 ]; then
    # If login isn't available or fails, fall back to a direct shell.
    if command -v $PREFERRED_SHELL >/dev/null 2>&1; then
        exec $PREFERRED_SHELL -l
    elif command -v $FALLBACK_SHELL >/dev/null 2>&1; then
        exec $FALLBACK_SHELL -l
    else
        echo "Error: No shell found at $PREFERRED_SHELL or $FALLBACK_SHELL"
        exit 1
    fi
else
    exec "$@"
fi