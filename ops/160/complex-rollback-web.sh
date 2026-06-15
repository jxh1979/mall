#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: complex-rollback-web <release-id>" >&2
  echo "Available releases:" >&2
  ls -1 /opt/complex/web/releases 2>/dev/null || true
  exit 1
fi

RELEASE_ID="$1"
BASE_DIR="/opt/complex/web"
RELEASE_DIR="$BASE_DIR/releases/$RELEASE_ID"
PROJECT_DIR="${COMPLEX_PROJECT_DIR:-$HOME/complex-management}"
LOG_DIR="${COMPLEX_LOG_DIR:-/var/log/complex}"
WEB_PORT="${WEB_PORT:-3000}"
WEB_HOST="${WEB_HOST:-127.0.0.1}"

if [[ ! "$RELEASE_ID" =~ ^[0-9]{8}-[0-9]{6}$ ]]; then
  echo "Invalid Web release id: $RELEASE_ID" >&2
  exit 1
fi

RELEASE_DIR="$(realpath -m "$RELEASE_DIR")"
if [[ "$RELEASE_DIR" != "$BASE_DIR/releases/"* ]]; then
  echo "Web release path escaped releases directory" >&2
  exit 1
fi

if [[ ! -d "$RELEASE_DIR" ]]; then
  echo "Web release not found: $RELEASE_ID" >&2
  exit 1
fi

if [[ -d "$RELEASE_DIR/dist" ]]; then
  OUT_DIR="$RELEASE_DIR/dist"
else
  OUT_DIR="$RELEASE_DIR"
fi

mkdir -p "$LOG_DIR"
ln -sfn "$RELEASE_DIR" "$BASE_DIR/current"

pids="$(ps -eo pid=,args= | awk '$0 ~ /vite preview .*--port 3000/ && $0 !~ /awk/ {print $1}')"
if [[ -n "$pids" ]]; then
  kill $pids
  sleep 1
fi

cd "$PROJECT_DIR/apps/web"
setsid npx vite preview --host "$WEB_HOST" --port "$WEB_PORT" --outDir "$OUT_DIR" > "$LOG_DIR/web.log" 2>&1 < /dev/null &

for _ in {1..20}; do
  if curl -fsS "http://127.0.0.1:$WEB_PORT/" >/dev/null; then
    echo "Web rolled back to: $RELEASE_ID"
    exit 0
  fi
  sleep 1
done

echo "Web rollback failed: $RELEASE_ID" >&2
exit 1
