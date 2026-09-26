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

ARGS="--kill-on-exit"
ARGS="$ARGS -w /"

for system_mnt in /apex /odm /product /system /system_ext /vendor \
 /linkerconfig/ld.config.txt \
 /linkerconfig/com.android.art/ld.config.txt \
 /plat_property_contexts /property_contexts; do

 if [ -e "$system_mnt" ]; then
  system_mnt=$(realpath "$system_mnt")
  ARGS="$ARGS -b ${system_mnt}"
 fi
done
unset system_mnt

ARGS="$ARGS -b /sdcard"
ARGS="$ARGS -b /storage"
ARGS="$ARGS -b /dev"
ARGS="$ARGS -b /data"
ARGS="$ARGS -b /dev/urandom:/dev/random"
ARGS="$ARGS -b /proc"
ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b $PREFIX/local/stat:/proc/stat"
ARGS="$ARGS -b $PREFIX/local/vmstat:/proc/vmstat"

if [ -e "/proc/self/fd" ]; then
  ARGS="$ARGS -b /proc/self/fd:/dev/fd"
fi

if [ -e "/proc/self/fd/0" ]; then
  ARGS="$ARGS -b /proc/self/fd/0:/dev/stdin"
fi

if [ -e "/proc/self/fd/1" ]; then
  ARGS="$ARGS -b /proc/self/fd/1:/dev/stdout"
fi

if [ -e "/proc/self/fd/2" ]; then
  ARGS="$ARGS -b /proc/self/fd/2:/dev/stderr"
fi


ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b /sys"

if [ ! -d "$ALPINE_DIR/tmp" ]; then
 mkdir -p "$ALPINE_DIR/tmp"
 chmod 1777 "$ALPINE_DIR/tmp"
fi
ARGS="$ARGS -b $ALPINE_DIR/tmp:/dev/shm"

ARGS="$ARGS -r $ALPINE_DIR"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

$PROOT $ARGS sh $PREFIX/local/bin/init "$@"
