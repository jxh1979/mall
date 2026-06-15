#!/usr/bin/env bash
set -euo pipefail

# P0-A no-system-write boundary
# P0A_NAS_PREFLIGHT: user-level check only. This script does not install mounts,
# timers, services, or credentials.

MOUNT_PATH="${MOUNT_PATH:-/mnt/complex-nas-backup}"
NAS_BACKUP_TARGET_DIR="${NAS_BACKUP_TARGET_DIR:-$MOUNT_PATH}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
CAPACITY_LIMIT_BYTES="${CAPACITY_LIMIT_BYTES:-1099511627776}"
MIN_FREE_BYTES="${MIN_FREE_BYTES:-1048576}"
TEST_FILE="$NAS_BACKUP_TARGET_DIR/.complex-preflight-write-test"

fail() {
  echo "P0A_NAS_PREFLIGHT FAIL: $*" >&2
  exit 1
}

command -v findmnt >/dev/null 2>&1 || fail "findmnt is required"
command -v df >/dev/null 2>&1 || fail "df is required"

[ -d "$MOUNT_PATH" ] || fail "mount path does not exist: $MOUNT_PATH"
[ -d "$NAS_BACKUP_TARGET_DIR" ] || fail "NAS backup target does not exist: $NAS_BACKUP_TARGET_DIR"

mount_info="$(findmnt -n -T "$MOUNT_PATH" -o SOURCE,FSTYPE,OPTIONS || true)"
[ -n "$mount_info" ] || fail "mount path is not mounted: $MOUNT_PATH"

free_kb="$(df -P "$NAS_BACKUP_TARGET_DIR" | awk 'NR==2 {print $4}')"
[ -n "$free_kb" ] || fail "cannot read free capacity from $NAS_BACKUP_TARGET_DIR"
free_bytes=$((free_kb * 1024))
[ "$free_bytes" -ge "$MIN_FREE_BYTES" ] || fail "free capacity is below MIN_FREE_BYTES"

printf 'ok\n' > "$TEST_FILE" || fail "cannot write test file"
read_back="$(cat "$TEST_FILE")" || fail "cannot read test file"
[ "$read_back" = "ok" ] || fail "test file content mismatch"
rm -f "$TEST_FILE" || fail "cannot delete test file"

echo "P0A_NAS_PREFLIGHT PASS"
echo "mount_path=$MOUNT_PATH"
echo "nas_backup_target_dir=$NAS_BACKUP_TARGET_DIR"
echo "retention_days=$RETENTION_DAYS"
echo "capacity_limit_bytes=$CAPACITY_LIMIT_BYTES"
echo "free_bytes=$free_bytes"
echo "mount_info=$mount_info"
