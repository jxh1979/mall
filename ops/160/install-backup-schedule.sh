#!/usr/bin/env bash
# install-backup-schedule.sh
# Reproducible installer for the Complex NAS backup + restore-drill schedule and
# alert framework on the 160 host, using systemd USER timers (linger already on,
# no sudo / no system unit writes -- consistent with complex-preview/portal).
#
# Installs:
#   /opt/complex/scripts/complex-backup-guard.sh
#   /opt/complex/scripts/complex-restore-drill-guard.sh
#   /opt/complex/scripts/complex-alert-dispatch.sh
#   ~/.config/systemd/user/complex-backup.{service,timer}
#   ~/.config/systemd/user/complex-restore-drill.{service,timer}
#
# Idempotent. Run:  ./install-backup-schedule.sh
# Uninstall:        ./install-backup-schedule.sh --uninstall
#
# Requires: this file sits in the repo ops/160 dir alongside the guard scripts
# and *.service/*.timer unit files.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="/opt/complex/scripts"
UNIT_DIR="$HOME/.config/systemd/user"

UNITS=(complex-backup.service complex-backup.timer
        complex-restore-drill.service complex-restore-drill.timer)
GUARDS=(complex-backup-guard.sh complex-restore-drill-guard.sh complex-alert-dispatch.sh)
TIMERS=(complex-backup.timer complex-restore-drill.timer)

if [[ "${1:-}" == "--uninstall" ]]; then
  for t in "${TIMERS[@]}"; do
    systemctl --user disable --now "$t" 2>/dev/null || true
  done
  for u in "${UNITS[@]}"; do rm -f "$UNIT_DIR/$u"; done
  systemctl --user daemon-reload
  echo "uninstalled complex backup schedule timers (guard scripts left in $SCRIPTS_DIR)."
  exit 0
fi

# 1. Deploy guard + alert scripts to runtime dir (backup any existing copy).
mkdir -p "$SCRIPTS_DIR"
ts="$(date +%Y%m%d-%H%M%S)"
for g in "${GUARDS[@]}"; do
  if [[ -f "$SCRIPTS_DIR/$g" ]]; then
    mkdir -p "$HOME/.complex/ops-script-backups/schedule-$ts"
    cp -p "$SCRIPTS_DIR/$g" "$HOME/.complex/ops-script-backups/schedule-$ts/"
  fi
  install -m 700 "$HERE/$g" "$SCRIPTS_DIR/$g"
done

# 2. Install user unit files.
mkdir -p "$UNIT_DIR"
for u in "${UNITS[@]}"; do
  install -m 644 "$HERE/$u" "$UNIT_DIR/$u"
done

# 3. Reload + enable timers.
systemctl --user daemon-reload
for t in "${TIMERS[@]}"; do
  systemctl --user enable --now "$t"
done

echo "installed. active timers:"
systemctl --user list-timers 'complex-*' --all
