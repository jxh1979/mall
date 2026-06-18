#!/usr/bin/env bash
set -euo pipefail

# NAS_IT_CONTROLLED_MOUNT_EXAMPLE_READY
# EXAMPLE_ONLY_REQUIRES_IT_MAINTENANCE_WINDOW
# NO_PLAINTEXT_SECRET_IN_SCRIPT
# DEFAULT_DRY_RUN_NO_SYSTEM_WRITE
# MANIFEST_EVIDENCE_MAPPING_READY

NAS_UNC="${NAS_UNC:-//192.168.88.113/bn}"
MOUNT_PATH="${MOUNT_PATH:-/mnt/complex-nas-backup}"
CREDENTIALS_FILE="${NAS_SMB_CREDENTIALS_FILE:-/etc/complex/secrets/nas-smb.credentials}"
MOUNT_USER="${MOUNT_USER:-james}"
MOUNT_GROUP="${MOUNT_GROUP:-james}"
SMB_VERSION="${SMB_VERSION:-3.0}"
EXECUTE_MOUNT="${EXECUTE_MOUNT:-false}"
EVIDENCE_DIR="${EVIDENCE_DIR:-/tmp/complex-nas-mount-evidence}"

fail() {
  echo "NAS_IT_CONTROLLED_MOUNT_FAIL: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required"
}

require_command findmnt
require_command stat
require_command id
require_command mkdir
require_command python3

require_cifs_helper() {
  if command -v mount.cifs >/dev/null 2>&1; then
    return 0
  fi
  for candidate in /sbin/mount.cifs /usr/sbin/mount.cifs /bin/mount.cifs /usr/bin/mount.cifs; do
    if [[ -x "$candidate" ]]; then
      return 0
    fi
  done
  fail "mount.cifs is required; install or repair cifs-utils before executing the NAS mount"
}

require_cifs_helper

if [[ "$EXECUTE_MOUNT" != "true" ]]; then
  cat <<EOF
NAS_IT_CONTROLLED_MOUNT_DRY_RUN
This script is a controlled IT handoff template. It does not write system state unless EXECUTE_MOUNT=true.

Expected production values:
  NAS_UNC=$NAS_UNC
  MOUNT_PATH=$MOUNT_PATH
  NAS_SMB_CREDENTIALS_FILE=$CREDENTIALS_FILE
  MOUNT_USER=$MOUNT_USER
  MOUNT_GROUP=$MOUNT_GROUP
  SMB_VERSION=$SMB_VERSION

Run during an approved IT maintenance window:
  cd /home/james/complex-management
  sudo env EXECUTE_MOUNT=true \\
    NAS_UNC='$NAS_UNC' \\
    MOUNT_PATH='$MOUNT_PATH' \\
    NAS_SMB_CREDENTIALS_FILE='$CREDENTIALS_FILE' \\
    MOUNT_USER='$MOUNT_USER' \\
    MOUNT_GROUP='$MOUNT_GROUP' \\
    SMB_VERSION='$SMB_VERSION' \\
    EVIDENCE_DIR='$EVIDENCE_DIR' \\
    bash ops/160/complex-nas-system-mount.example.sh

After it passes:
  npm run docs:nas-current-state-drift
  npm run verify:nas-current-state-drift
  npm run verify:closed-loop-current-state
  npm run verify:delivery-gates
EOF
  exit 0
fi

[[ "$(id -u)" -eq 0 ]] || fail "EXECUTE_MOUNT=true requires root or sudo"
require_command mount
require_command runuser
require_command df

[[ -f "$CREDENTIALS_FILE" ]] || fail "credential file does not exist: $CREDENTIALS_FILE"
credential_mode="$(stat -c '%a' "$CREDENTIALS_FILE")"
case "$credential_mode" in
  400|600) ;;
  *) fail "credential file mode must be 400 or 600, got $credential_mode" ;;
esac

uid="$(id -u "$MOUNT_USER")" || fail "unknown mount user: $MOUNT_USER"
gid="$(id -g "$MOUNT_GROUP")" || fail "unknown mount group: $MOUNT_GROUP"

mkdir -p "$MOUNT_PATH"

current_mount="$(findmnt -n -T "$MOUNT_PATH" -o SOURCE,FSTYPE,OPTIONS 2>/dev/null || true)"
if [[ -n "$current_mount" ]]; then
  if [[ "$current_mount" != *"$NAS_UNC"* || "$current_mount" != *"cifs"* ]]; then
    fail "mount path is already mounted but not to expected CIFS source: $current_mount"
  fi
else
  mount -t cifs "$NAS_UNC" "$MOUNT_PATH" \
    -o "credentials=$CREDENTIALS_FILE,iocharset=utf8,vers=$SMB_VERSION,sec=ntlmssp,uid=$uid,gid=$gid,file_mode=0600,dir_mode=0700,noperm"
fi

mount_info="$(findmnt -n -T "$MOUNT_PATH" -o TARGET,SOURCE,FSTYPE,OPTIONS)"
[[ "$mount_info" == *"$NAS_UNC"* ]] || fail "findmnt does not show expected NAS source"
[[ "$mount_info" == *"cifs"* ]] || fail "findmnt does not show CIFS filesystem"

test_file="$MOUNT_PATH/.complex-nas-it-mount-test-$(date -u +%Y%m%d%H%M%S)-$$"
runuser -u "$MOUNT_USER" -- bash -c 'set -euo pipefail; f="$1"; printf "ok\n" > "$f"; test "$(cat "$f")" = "ok"; rm -f "$f"' _ "$test_file" || fail "mount user cannot write/read/delete in mount path"

free_bytes="$(( $(df -P "$MOUNT_PATH" | awk 'NR==2 {print $4}') * 1024 ))"
mkdir -p "$EVIDENCE_DIR"
chmod 700 "$EVIDENCE_DIR"
evidence_json="$EVIDENCE_DIR/nas-it-mount-evidence-$(date -u +%Y%m%dT%H%M%SZ).json"

EVIDENCE_JSON="$evidence_json" \
NAS_UNC="$NAS_UNC" \
MOUNT_PATH="$MOUNT_PATH" \
MOUNT_USER="$MOUNT_USER" \
MOUNT_GROUP="$MOUNT_GROUP" \
SMB_VERSION="$SMB_VERSION" \
MOUNT_INFO="$mount_info" \
FREE_BYTES="$free_bytes" \
CREDENTIAL_REF="cred://prod/nas/smb/complex-backup-rw" \
python3 - <<'PY'
import json
import os
from datetime import datetime, timezone
payload = {
    "marker": "NAS_IT_CONTROLLED_MOUNT_EVIDENCE",
    "generated_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    "result": "passed",
    "nas_unc": os.environ["NAS_UNC"],
    "mount_path": os.environ["MOUNT_PATH"],
    "mount_user": os.environ["MOUNT_USER"],
    "mount_group": os.environ["MOUNT_GROUP"],
    "smb_version": os.environ["SMB_VERSION"],
    "credential_ref": os.environ["CREDENTIAL_REF"],
    "findmnt": os.environ["MOUNT_INFO"],
    "write_read_delete_check": "passed",
    "free_bytes": int(os.environ["FREE_BYTES"]),
    "manifest_items_to_update": [
        "PRD-NAS-AUTOMOUNT-001",
        "PRD-NAS-SCHEDULE-001",
        "PRD-NAS-ALERT-001",
        "PRD-NAS-DRILL-CADENCE-001",
        "SEC-P0-OPS-RECOVERY-001"
    ],
    "secret_policy": "no plaintext secret captured; use credential_ref only"
}
with open(os.environ["EVIDENCE_JSON"], "w", encoding="utf-8") as handle:
    json.dump(payload, handle, ensure_ascii=False, indent=2)
    handle.write("\n")
PY
chmod 600 "$evidence_json"

bash ops/160/complex-nas-preflight.sh

echo "NAS_IT_CONTROLLED_MOUNT_PASS"
echo "mount_info=$mount_info"
echo "free_bytes=$free_bytes"
echo "evidence_json=$evidence_json"
