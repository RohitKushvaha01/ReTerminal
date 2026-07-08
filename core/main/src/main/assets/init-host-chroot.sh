ALPINE_DIR=$PREFIX/local/alpine
mkdir -p $ALPINE_DIR
if [ -f "$PREFIX/local/bin/cleanup-stale-mounts.sh" ]; then
    /system/bin/su -c "sh $PREFIX/local/bin/cleanup-stale-mounts.sh"
fi
if [ -z "$(ls -A "$ALPINE_DIR" | grep -vE '^(root|tmp)$')" ]; then
    tar -xf "$PREFIX/files/alpine.tar.gz" -C "$ALPINE_DIR"
fi
MOUNTS=""
mnt_bind() {
    src="$1"
    dst="$ALPINE_DIR${2:-$1}"
    if [ -e "$src" ] && [ ! -e "$dst" ]; then
        mkdir -p "$(dirname "$dst")" 2>/dev/null
        [ -d "$src" ] && mkdir -p "$dst" || touch "$dst"
    fi
    if [ -e "$src" ]; then
        su -c "mount --bind '$src' '$dst'" 2>/dev/null && MOUNTS="$dst
$MOUNTS"
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
if [ ! -d "$PREFIX/local/alpine/tmp" ]; then
    mkdir -p "$PREFIX/local/alpine/tmp"
    chmod 1777 "$PREFIX/local/alpine/tmp"
fi
mnt_bind "$PREFIX/local/alpine/tmp" /dev/shm
if [ -e "$PREFIX/local/stat" ]; then
    su -c "cp '$PREFIX/local/stat' '$ALPINE_DIR/proc/stat'" 2>/dev/null
fi
if [ -e "$PREFIX/local/vmstat" ]; then
    su -c "cp '$PREFIX/local/vmstat' '$ALPINE_DIR/proc/vmstat'" 2>/dev/null
fi
echo "$MOUNTS" > "$PREFIX/local/.chroot_mounts"
cleanup() {
    while IFS= read -r m; do
        [ -n "$m" ] && su -c "umount -l '$m'" 2>/dev/null
    done < "$PREFIX/local/.chroot_mounts"
    rm -f "$PREFIX/local/.chroot_mounts"
}
trap cleanup EXIT INT TERM
su -c "'$CHROOT' '$ALPINE_DIR' /usr/bin/env -i HOME=/root PATH=/bin:/sbin:/usr/bin:/usr/sbin sh '$PREFIX/local/bin/init' $*"
cleanup
