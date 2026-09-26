#!/bin/sh
SU="/system/bin/su"
ALPINE_DIR=${ROOTFS_DIR:-$PREFIX/local/alpine}
ROOTFS_TAR=${ROOTFS_TAR:-$PREFIX/files/alpine.tar.gz}

mkdir -p $ALPINE_DIR

if [ -z "$(ls -A "$ALPINE_DIR" | grep -vE '^(root|tmp)$')" ]; then
    if [ ! -f "$ROOTFS_TAR" ]; then
        echo "ReTerminal: rootfs archive not found: $ROOTFS_TAR"
        exit 1
    fi
    tar -xf "$ROOTFS_TAR" -C "$ALPINE_DIR"
fi

if [ -f "$BIN/rm" ]; then
    rm -f "$ALPINE_DIR/bin/rm"
    cp "$BIN/rm" "$ALPINE_DIR/bin/rm"
    chmod +x "$ALPINE_DIR/bin/rm"
fi

MOUNTS=""

mnt_bind() {
    src="$1"
    dst="$ALPINE_DIR${2:-$1}"
    if [ -e "$src" ] && [ ! -e "$dst" ]; then
        mkdir -p "$(dirname "$dst")" 2>/dev/null
        if [ -d "$src" ]; then
            $SU -c "mkdir -p '$dst'"
        else
            $SU -c "touch '$dst'"
        fi
    fi
    if [ -e "$src" ]; then
        $SU -c "mount --bind '$src' '$dst'" 2>/dev/null
        MOUNTS="$MOUNTS $dst"
    fi
}

for system_mnt in /apex /odm /product /system /system_ext /vendor \
 /linkerconfig/ld.config.txt \
 /linkerconfig/com.android.art/ld.config.txt \
 /plat_property_contexts /property_contexts; do
    if [ -e "$system_mnt" ]; then
        system_mnt=$(realpath "$system_mnt")
        mnt_bind "$system_mnt"
    fi
done
unset system_mnt

mnt_bind /sdcard
mnt_bind /storage
mnt_bind /dev
mnt_bind /data
mnt_bind /proc
mnt_bind /sys
mnt_bind /dev/urandom /dev/random
mnt_bind $PREFIX

if [ -e "/proc/self/fd" ]; then mnt_bind /proc/self/fd /dev/fd; fi
if [ -e "/proc/self/fd/0" ]; then mnt_bind /proc/self/fd/0 /dev/stdin; fi
if [ -e "/proc/self/fd/1" ]; then mnt_bind /proc/self/fd/1 /dev/stdout; fi
if [ -e "/proc/self/fd/2" ]; then mnt_bind /proc/self/fd/2 /dev/stderr; fi

if [ ! -d "$ALPINE_DIR/tmp" ]; then
    $SU -c "mkdir -p '$ALPINE_DIR/tmp' && chmod 1777 '$ALPINE_DIR/tmp'"
fi
mnt_bind "$ALPINE_DIR/tmp" /dev/shm

if [ -e "$PREFIX/local/stat" ]; then
    $SU -c "cp '$PREFIX/local/stat' '$ALPINE_DIR/proc/stat'" 2>/dev/null
fi
if [ -e "$PREFIX/local/vmstat" ]; then
    $SU -c "cp '$PREFIX/local/vmstat' '$ALPINE_DIR/proc/vmstat'" 2>/dev/null
fi

cleanup() {
    for m in $MOUNTS; do
        $SU -c "umount -l '$m'" 2>/dev/null
    done
}
trap cleanup EXIT INT TERM

$SU -c "'$CHROOT' '$ALPINE_DIR' /usr/bin/env -i HOME=/root PATH=/bin:/sbin:/usr/bin:/usr/sbin RETERM_LOGIN_SHELL=${RETERM_LOGIN_SHELL:-0} sh '$PREFIX/local/bin/init' $*"
cleanup
