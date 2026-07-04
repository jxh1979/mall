#!/usr/bin/env bash
# complex-backup-guard.sh
# Scheduled wrapper around complex-backup.sh that adds:
#   - NAS capacity check (warn/critical thresholds) BEFORE and AFTER the backup
#   - failure alerting (dispatches backup_failed on any non-zero exit)
#   - capacity alerting (dispatches capacity_threshold_exceeded past critical %)
# Invoked by the systemd user timer (complex-backup.timer). Safe to run manually.
#
# Thresholds default to the values in complex-backup-alert.example
# (warn 80 / critical 90). Override via CAPACITY_WARNING_PERCENT /
# CAPACITY_CRITICAL_PERCENT env if IT re-tunes them.
set -uo pipefail

SCRIPTS_DIR="${SCRIPTS_DIR:-/opt/complex/scripts}"
BACKUP_SCRIPT="${BACKUP_SCRIPT:-$SCRIPTS_DIR/complex-backup.sh}"
ALERT_DISPATCH="${ALERT_DISPATCH:-$SCRIPTS_DIR/complex-alert-dispatch.sh}"
BACKUP_KIND="${BACKUP_KIND:-daily}"

# NAS sync is mandatory for the scheduled job.
export NAS_BACKUP_TARGET_DIR="${NAS_BACKUP_TARGET_DIR:-/mnt/complex-nas-backup}"
export REQUIRE_NAS_BACKUP_SYNC="${REQUIRE_NAS_BACKUP_SYNC:-true}"

CAPACITY_WARNING_PERCENT="${CAPACITY_WARNING_PERCENT:-80}"
CAPACITY_CRITICAL_PERCENT="${CAPACITY_CRITICAL_PERCENT:-90}"

alert() { "$ALERT_DISPATCH" "$1" "$2" || echo "WARN: alert dispatch failed for $1" >&2; }

check_capacity() {
  local phase="$1"
  local use_pct
  use_pct="$(df -P "$NAS_BACKUP_TARGET_DIR" | awk 'NR==2 {gsub("%","",$5); print $5}')"
  [[ -z "$use_pct" ]] && { echo "WARN: cannot read NAS usage ($phase)" >&2; return 0; }
  echo "NAS capacity ($phase): ${use_pct}% used (warn ${CAPACITY_WARNING_PERCENT}%, crit ${CAPACITY_CRITICAL_PERCENT}%)"
  if (( use_pct >= CAPACITY_CRITICAL_PERCENT )); then
    alert "capacity_threshold_exceeded" "NAS $NAS_BACKUP_TARGET_DIR at ${use_pct}% (>= critical ${CAPACITY_CRITICAL_PERCENT}%) [$phase]"
  elif (( use_pct >= CAPACITY_WARNING_PERCENT )); then
    alert "capacity_threshold_exceeded" "NAS $NAS_BACKUP_TARGET_DIR at ${use_pct}% (>= warning ${CAPACITY_WARNING_PERCENT}%) [$phase]"
  fi
}

check_capacity "pre-backup"

if "$BACKUP_SCRIPT" "$BACKUP_KIND"; then
  echo "backup guard: backup OK"
else
  rc=$?
  alert "backup_failed" "complex-backup.sh ($BACKUP_KIND) exited $rc on 160 at $(date -Iseconds)"
  check_capacity "post-failure"
  exit "$rc"
fi

check_capacity "post-backup"
