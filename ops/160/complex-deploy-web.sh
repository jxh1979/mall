#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${COMPLEX_PROJECT_DIR:-$HOME/complex-management}"
LOG_DIR="${COMPLEX_LOG_DIR:-/var/log/complex}"
WEB_PORT="${WEB_PORT:-3000}"
WEB_HOST="${WEB_HOST:-127.0.0.1}"

mkdir -p "$LOG_DIR" /opt/complex/web/releases

release_id="$(date +%Y%m%d-%H%M%S)"
release_dir="/opt/complex/web/releases/$release_id"

echo "[$(date -Iseconds)] deploy web start release=$release_id" | tee -a "$LOG_DIR/deploy.log"

cd "$PROJECT_DIR"
npm run build --workspace apps/web

mkdir -p "$release_dir"
mkdir -p "$release_dir/dist"
cp -a apps/web/dist/. "$release_dir/dist/"
cat > "$release_dir/build-info.json" <<EOF
{
  "releaseId": "$release_id",
  "releasedAt": "$(date -Iseconds)",
  "projectDir": "$PROJECT_DIR",
  "runtime": "vite-preview-p0"
}
EOF

ln -sfn "$release_dir" /opt/complex/web/current

pids="$(ps -eo pid=,args= | awk '$0 ~ /vite preview .*--port 3000/ && $0 !~ /awk/ {print $1}')"
if [[ -n "$pids" ]]; then
  kill $pids
  sleep 1
fi

cd "$PROJECT_DIR/apps/web"
setsid npx vite preview --host "$WEB_HOST" --port "$WEB_PORT" --outDir /opt/complex/web/current/dist > "$LOG_DIR/web.log" 2>&1 < /dev/null &

for _ in {1..20}; do
  if curl -fsS "http://127.0.0.1:$WEB_PORT/" >/dev/null; then
    echo "[$(date -Iseconds)] deploy web ok release=$release_id" | tee -a "$LOG_DIR/deploy.log"
    exit 0
  fi
  sleep 1
done

echo "[$(date -Iseconds)] deploy web failed release=$release_id" | tee -a "$LOG_DIR/deploy.log"
exit 1
