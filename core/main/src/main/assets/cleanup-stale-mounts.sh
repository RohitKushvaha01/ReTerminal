ALPINE_DIR=$PREFIX/local/alpine
MOUNTLIST_FILE="$PREFIX/local/.chroot_mounts"

check_and_clean_stale_mounts() {
    if [ ! -f "$MOUNTLIST_FILE" ]; then
        return 0
    fi

    stale_count=0
    while IFS= read -r m; do
        [ -z "$m" ] && continue
        if mount | grep -qF " on $m "; then
            stale_count=$((stale_count + 1))
        fi
    done < "$MOUNTLIST_FILE"

    if [ "$stale_count" -eq 0 ]; then
        rm -f "$MOUNTLIST_FILE"
        return 0
    fi

    echo "[reterm] Found $stale_count stale chroot mount(s), cleaning up..."

    sort -r "$MOUNTLIST_FILE" | while IFS= read -r m; do
        [ -z "$m" ] && continue
        if mount | grep -qF " on $m "; then
            /system/bin/su -c "umount -l '$m'" 2>/dev/null
        fi
    done

    remaining=0
    while IFS= read -r m; do
        [ -z "$m" ] && continue
        if mount | grep -qF " on $m "; then
            remaining=$((remaining + 1))
            echo "[reterm] WARNING: failed to unmount: $m"
        fi
    done < "$MOUNTLIST_FILE"

    rm -f "$MOUNTLIST_FILE"

    if [ "$remaining" -gt 0 ]; then
        echo "[reterm] $remaining mount(s) still active, manual intervention may be required"
        return 1
    fi

    echo "[reterm] Cleanup complete."
    return 0
}

check_and_clean_stale_mounts
