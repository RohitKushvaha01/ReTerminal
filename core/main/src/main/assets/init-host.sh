ALPINE_DIR=$PREFIX/local/alpine
FAKE_PROC_DIR=$PREFIX/local/proc-fake

mkdir -p "$ALPINE_DIR" "$FAKE_PROC_DIR"

if [ -z "$(ls -A "$ALPINE_DIR" | grep -vE '^(root|tmp)$')" ]; then
    tar -xf "$PREFIX/files/alpine.tar.gz" -C "$ALPINE_DIR"
fi

[ ! -e "$PREFIX/local/bin/proot" ] && cp "$PREFIX/files/proot" "$PREFIX/local/bin"

for sofile in "$PREFIX/files/"*.so.2; do
    dest="$PREFIX/local/lib/$(basename "$sofile")"
    [ ! -e "$dest" ] && cp "$sofile" "$dest"
done

write_fake_proc_stat() {
  now=$(date +%s)
  cpu_count=$(grep -c '^processor' /proc/cpuinfo 2>/dev/null)
  case "$cpu_count" in
    ''|*[!0-9]*) cpu_count=8 ;;
  esac
  [ "$cpu_count" -lt 1 ] && cpu_count=8
  [ "$cpu_count" -gt 32 ] && cpu_count=32

  total_user=0
  total_nice=0
  total_system=0
  total_idle=0
  total_iowait=0
  total_irq=0
  total_softirq=0
  total_steal=0

  tmp="$FAKE_PROC_DIR/stat.tmp"
  : > "$tmp"

  i=0
  while [ "$i" -lt "$cpu_count" ]; do
    user=$((now * (i + 3) + 1000))
    nice=0
    system=$((now * (i + 2) / 2 + 500))
    idle=$((now * 100 + i * 10000 + 100000))
    iowait=$((now % 97))
    irq=0
    softirq=$((now % 53))
    steal=0

    total_user=$((total_user + user))
    total_nice=$((total_nice + nice))
    total_system=$((total_system + system))
    total_idle=$((total_idle + idle))
    total_iowait=$((total_iowait + iowait))
    total_irq=$((total_irq + irq))
    total_softirq=$((total_softirq + softirq))
    total_steal=$((total_steal + steal))

    echo "cpu$i $user $nice $system $idle $iowait $irq $softirq $steal 0 0" >> "$tmp"
    i=$((i + 1))
  done

  {
    echo "cpu $total_user $total_nice $total_system $total_idle $total_iowait $total_irq $total_softirq $total_steal 0 0"
    cat "$tmp"
    echo "intr 0"
    echo "ctxt $((now * 100))"
    echo "btime $((now - 3600))"
    echo "processes $((now % 100000 + 1000))"
    echo "procs_running 1"
    echo "procs_blocked 0"
    echo "softirq 0 0 0 0 0 0 0 0 0 0 0"
  } > "$FAKE_PROC_DIR/stat"

  rm -f "$tmp"
}

start_proc_stat_updater() {
  while true; do
    write_fake_proc_stat
    sleep 1
  done
}

write_fake_proc_stat
start_proc_stat_updater &
PROC_STAT_UPDATER_PID=$!

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
ARGS="$ARGS -b $FAKE_PROC_DIR/stat:/proc/stat"
ARGS="$ARGS -b $PREFIX"

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

if [ ! -d "$PREFIX/local/alpine/tmp" ]; then
 mkdir -p "$PREFIX/local/alpine/tmp"
 chmod 1777 "$PREFIX/local/alpine/tmp"
fi
ARGS="$ARGS -b $PREFIX/local/alpine/tmp:/dev/shm"

ARGS="$ARGS -r $PREFIX/local/alpine"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

$LINKER $PREFIX/local/bin/proot $ARGS sh $PREFIX/local/bin/init "$@"
STATUS=$?
kill "$PROC_STAT_UPDATER_PID" 2>/dev/null
exit "$STATUS"
