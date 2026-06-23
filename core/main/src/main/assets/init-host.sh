ALPINE_DIR=$PREFIX/local/alpine
FAKE_PROC_DIR=$PREFIX/local/proc-fake
FAKE_SYS_DIR=$PREFIX/local/sys-fake

mkdir -p "$ALPINE_DIR" "$FAKE_PROC_DIR" "$FAKE_SYS_DIR"

if [ -z "$(ls -A "$ALPINE_DIR" | grep -vE '^(root|tmp)$')" ]; then
    tar -xf "$PREFIX/files/alpine.tar.gz" -C "$ALPINE_DIR"
fi

[ ! -e "$PREFIX/local/bin/proot" ] && cp "$PREFIX/files/proot" "$PREFIX/local/bin"

for sofile in "$PREFIX/files/"*.so.2; do
    dest="$PREFIX/local/lib/$(basename "$sofile")"
    [ ! -e "$dest" ] && cp "$sofile" "$dest"
done

get_cpu_count() {
  cpu_count=$(grep -c '^processor' /proc/cpuinfo 2>/dev/null)
  case "$cpu_count" in
    ''|*[!0-9]*) cpu_count=8 ;;
  esac
  [ "$cpu_count" -lt 1 ] && cpu_count=8
  [ "$cpu_count" -gt 32 ] && cpu_count=32
  echo "$cpu_count"
}

write_fake_proc_stat() {
  now=$(date +%s)
  cpu_count=$(get_cpu_count)

  total_user=0
  total_nice=0
  total_system=0
  total_idle=0
  total_iowait=0
  total_irq=0
  total_softirq=0
  total_steal=0

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
    i=$((i + 1))
  done

  stat_file="$FAKE_PROC_DIR/stat"
  {
    echo "cpu $total_user $total_nice $total_system $total_idle $total_iowait $total_irq $total_softirq $total_steal 0 0"
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
      echo "cpu$i $user $nice $system $idle $iowait $irq $softirq $steal 0 0"
      i=$((i + 1))
    done
    echo "intr 0"
    echo "ctxt $((now * 100))"
    echo "btime $((now - 3600))"
    echo "processes $((now % 100000 + 1000))"
    echo "procs_running 1"
    echo "procs_blocked 0"
    echo "softirq 0 0 0 0 0 0 0 0 0 0 0"
  } > "$stat_file"
}

write_fake_sysfs() {
  cpu_count=$(get_cpu_count)
  last_cpu=$((cpu_count - 1))

  rm -rf "$FAKE_SYS_DIR"
  mkdir -p \
    "$FAKE_SYS_DIR/dev" \
    "$FAKE_SYS_DIR/class/thermal" \
    "$FAKE_SYS_DIR/class/hwmon" \
    "$FAKE_SYS_DIR/class/power_supply" \
    "$FAKE_SYS_DIR/devices/system/cpu" \
    "$FAKE_SYS_DIR/devices/virtual/dmi/id"

  echo "0-$last_cpu" > "$FAKE_SYS_DIR/devices/system/cpu/possible"
  echo "0-$last_cpu" > "$FAKE_SYS_DIR/devices/system/cpu/present"
  echo "0-$last_cpu" > "$FAKE_SYS_DIR/devices/system/cpu/online"
  echo "AndLinux" > "$FAKE_SYS_DIR/devices/virtual/dmi/id/sys_vendor"
  echo "Android proot" > "$FAKE_SYS_DIR/devices/virtual/dmi/id/product_name"

  i=0
  while [ "$i" -lt "$cpu_count" ]; do
    cpu_dir="$FAKE_SYS_DIR/devices/system/cpu/cpu$i"
    mkdir -p "$cpu_dir/cpufreq" "$cpu_dir/topology"
    echo 1 > "$cpu_dir/online"
    echo 1200000 > "$cpu_dir/cpufreq/scaling_cur_freq"
    echo 300000 > "$cpu_dir/cpufreq/cpuinfo_min_freq"
    echo 2400000 > "$cpu_dir/cpufreq/cpuinfo_max_freq"
    echo "$i" > "$cpu_dir/topology/core_id"
    echo 0 > "$cpu_dir/topology/physical_package_id"
    i=$((i + 1))
  done
}

# Desktop Linux compatibility for tools that read procfs/sysfs directly.
write_fake_proc_stat
write_fake_sysfs

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
ARGS="$ARGS -b $FAKE_SYS_DIR:/sys"

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

exec $LINKER $PREFIX/local/bin/proot $ARGS sh $PREFIX/local/bin/init "$@"
