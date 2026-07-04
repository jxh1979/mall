#!/usr/bin/env bash
# complex-restore-drill-guard.sh
# Scheduled wrapper around complex-restore-drill.sh: runs the weekly drill
# against the latest daily backup and alerts (restore_drill_failed) on failure.
set -uo pipefail

SCRIPTS_DIR="${SCRIPTS_DIR:-/opt/complex/scripts}"
DRILL_SCRIPT="${DRILL_SCRIPT:-$SCRIPTS_DIR/complex-restore-drill.sh}"
ALERT_DISPATCH="${ALERT_DISPATCH:-$SCRIPTS_DIR/complex-alert-dispatch.sh}"
export RESTORE_BACKUP_KIND="${RESTORE_BACKUP_KIND:-daily}"

if "$DRILL_SCRIPT" latest; then
  echo "restore drill guard: OK"
else
  rc=$?
  "$ALERT_DISPATCH" "restore_drill_failed" \
    "complex-restore-drill.sh (kind=$RESTORE_BACKUP_KIND) exited $rc on 160 at $(date -Iseconds)" \
    || echo "WARN: alert dispatch failed" >&2
  exit "$rc"
fi
