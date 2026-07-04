#!/usr/bin/env bash
# complex-alert-dispatch.sh
# Alert framework dispatcher for the Complex backup/restore pipeline.
#
# WHAT THIS DOES: receives an event + message, appends it to the local alert
# journal, and (if an IT-supplied delivery hook is present and executable)
# forwards the alert to it. Delivery targets (WeCom webhook, email) are NOT
# baked in here on purpose -- see complex-backup-alert.example (secret-ref only).
#
# TO ACTIVATE REAL DELIVERY (IT/ops step):
#   Provide an executable at $ALERT_DELIVERY_HOOK (default
#   /opt/complex/scripts/complex-alert-delivery.sh) that reads the same 2 args
#   ($1=event, $2=message) and pushes to the real channel using credentials
#   resolved from the credential ledger (cred:// refs in the alert template).
#   Until then this dispatcher only journals -- it never invents a webhook/address.
#
# Usage: complex-alert-dispatch.sh <event> <message>
set -euo pipefail

EVENT="${1:?event required}"
MESSAGE="${2:-}"
TS="$(date -Iseconds)"
JOURNAL="${ALERT_JOURNAL:-$HOME/.complex/logs/backup-alerts.log}"
ALERT_DELIVERY_HOOK="${ALERT_DELIVERY_HOOK:-/opt/complex/scripts/complex-alert-delivery.sh}"

mkdir -p "$(dirname "$JOURNAL")"
printf '%s\t%s\t%s\n' "$TS" "$EVENT" "$MESSAGE" >> "$JOURNAL"

if [[ -x "$ALERT_DELIVERY_HOOK" ]]; then
  # ponytail: real delivery is IT's plug-in; we only journal + forward.
  "$ALERT_DELIVERY_HOOK" "$EVENT" "$MESSAGE" || \
    printf '%s\t%s\t%s\n' "$(date -Iseconds)" "alert_delivery_failed" \
      "delivery hook $ALERT_DELIVERY_HOOK exited non-zero for event=$EVENT" >> "$JOURNAL"
else
  printf '%s\t%s\t%s\n' "$(date -Iseconds)" "alert_delivery_pending" \
    "no delivery hook at $ALERT_DELIVERY_HOOK -- alert journaled only (IT to supply target)" >> "$JOURNAL"
fi

echo "alert dispatched: event=$EVENT journal=$JOURNAL"
