#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-status}"
PROJECT_DIR="${COMPLEX_PROJECT_DIR:-$HOME/complex-management}"
ENV_FILE="${COMPLEX_LAN_ENV_FILE:-$HOME/.complex/complex-lan.env}"
RUNTIME_DIR="${COMPLEX_RUNTIME_DIR:-$HOME/.complex/runtime}"
LOG_DIR="${COMPLEX_RUNTIME_LOG_DIR:-$HOME/.complex/logs}"
API_PID_FILE="$RUNTIME_DIR/complex-lan-api.pid"
WEB_PID_FILE="$RUNTIME_DIR/complex-lan-web.pid"
API_LOG="$LOG_DIR/complex-lan-api.log"
WEB_LOG="$LOG_DIR/complex-lan-web.log"

load_env() {
  if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
  fi
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "complex-lan-prod FAIL: missing command: $1" >&2
    exit 1
  fi
}

ensure_dirs() {
  mkdir -p "$RUNTIME_DIR" "$LOG_DIR"
}

load_env
ensure_dirs

API_PORT="${COMPLEX_API_PORT:-8080}"
API_HOST="${COMPLEX_API_HOST:-127.0.0.1}"
WEB_PORT="${COMPLEX_WEB_PREVIEW_PORT:-3000}"
WEB_HOST="${COMPLEX_WEB_PREVIEW_HOST:-127.0.0.1}"
LAN_HOST="${COMPLEX_LAN_HOST:-192.168.88.160}"

pid_from_port() {
  local port="$1"
  ss -ltnp 2>/dev/null | awk -v port=":$port" '$0 ~ port"[[:space:]]" {print $NF}' | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | sort -u
}

clear_port() {
  local port="$1"
  local pids
  pids="$(pid_from_port "$port" | tr '\n' ' ')"
  if [[ -z "${pids// }" ]]; then
    return 0
  fi
  kill $pids 2>/dev/null || true
  sleep 1
  pids="$(pid_from_port "$port" | tr '\n' ' ')"
  if [[ -n "${pids// }" ]]; then
    kill -9 $pids 2>/dev/null || true
  fi
}

stop_pid_file() {
  local pid_file="$1"
  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      pkill -TERM -P "$pid" 2>/dev/null || true
      kill "$pid" 2>/dev/null || true
      sleep 1
      pkill -KILL -P "$pid" 2>/dev/null || true
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
}

wait_for_head() {
  local url="$1"
  local attempts="${2:-40}"
  for _ in $(seq 1 "$attempts"); do
    if curl -fsSI --max-time 5 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_for_json_ok() {
  local attempts="${1:-40}"
  for _ in $(seq 1 "$attempts"); do
    if curl -fsS --max-time 5 -H 'Content-Type: application/json' \
      --data '{"login_name":"admin","password":"admin-pass"}' \
      "http://127.0.0.1:${API_PORT}/api/v1/auth/login" | grep -q '"code":"OK"'; then
      return 0
    fi
    sleep 1
  done
  return 1
}

start_api() {
  require_cmd npm
  require_cmd curl
  cd "$PROJECT_DIR"
  if curl -fsSI --max-time 5 "http://127.0.0.1:${API_PORT}/actuator/health" >/dev/null 2>&1 || wait_for_json_ok 1; then
    echo "api=already-running port=${API_PORT}"
    return 0
  fi

  for pid in $(pid_from_port "$API_PORT"); do
    kill "$pid" 2>/dev/null || true
  done
  sleep 1

  nohup env \
    P0_ADMIN_USERNAME="${P0_ADMIN_USERNAME:-}" \
    P0_ADMIN_PASSWORD="${P0_ADMIN_PASSWORD:-}" \
    SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}" \
    COMPLEX_API_PORT="$API_PORT" \
    COMPLEX_API_HOST="$API_HOST" \
    COMPLEX_DATABASE_URL="${COMPLEX_DATABASE_URL:-jdbc:postgresql://127.0.0.1:5432/complex}" \
    COMPLEX_DATABASE_USERNAME="${COMPLEX_DATABASE_USERNAME:-complex}" \
    COMPLEX_DATABASE_PASSWORD="${COMPLEX_DATABASE_PASSWORD:-complex_dev_pwd}" \
    npm run dev:backend >"$API_LOG" 2>&1 &
  echo $! > "$API_PID_FILE"

  if wait_for_json_ok 60; then
    echo "api=running port=${API_PORT}"
    return 0
  fi
  echo "complex-lan-prod FAIL: api did not become available" >&2
  tail -120 "$API_LOG" >&2 || true
  exit 1
}

start_web() {
  require_cmd npm
  require_cmd curl
  cd "$PROJECT_DIR"
  npm run build --workspace apps/web

  stop_pid_file "$WEB_PID_FILE"
  for pid in $(pid_from_port "$WEB_PORT"); do
    kill "$pid" 2>/dev/null || true
  done
  sleep 1

  cd "$PROJECT_DIR/apps/web"
  nohup npx vite preview --host "$WEB_HOST" --port "$WEB_PORT" >"$WEB_LOG" 2>&1 &
  echo $! > "$WEB_PID_FILE"

  if wait_for_head "http://127.0.0.1:${WEB_PORT}/" 40; then
    echo "web=running port=${WEB_PORT}"
    return 0
  fi
  echo "complex-lan-prod FAIL: web did not become available" >&2
  tail -120 "$WEB_LOG" >&2 || true
  exit 1
}

stop_all() {
  stop_pid_file "$WEB_PID_FILE"
  stop_pid_file "$API_PID_FILE"
  echo "lan-prod=stopped"
}

status_all() {
  local failed=0
  echo "expected-lan-url=http://${LAN_HOST}/"
  if curl -fsSI --max-time 5 "http://127.0.0.1:${WEB_PORT}/" >/dev/null 2>&1; then
    echo "web=running local=127.0.0.1:${WEB_PORT}"
  else
    echo "web=down local=127.0.0.1:${WEB_PORT}"
    failed=1
  fi
  if wait_for_json_ok 1; then
    echo "api=running local=127.0.0.1:${API_PORT}"
  else
    echo "api=down local=127.0.0.1:${API_PORT}"
    failed=1
  fi
  if curl -fsSI --max-time 5 "http://127.0.0.1/" >/dev/null 2>&1; then
    echo "nginx-web=running port=80"
  else
    echo "nginx-web=down port=80"
    failed=1
  fi
  if curl -fsS --max-time 5 -H 'Content-Type: application/json' \
    --data '{"login_name":"admin","password":"admin-pass"}' \
    "http://127.0.0.1/api/v1/auth/login" | grep -q '"code":"OK"'; then
    echo "nginx-api=running port=80"
  else
    echo "nginx-api=down port=80"
    failed=1
  fi
  return "$failed"
}

case "$ACTION" in
  up|start)
    start_api
    start_web
    status_all
    ;;
  down|stop)
    stop_all
    ;;
  restart)
    stop_all
    clear_port "$API_PORT"
    clear_port "$WEB_PORT"
    start_api
    start_web
    status_all
    ;;
  status)
    status_all
    ;;
  *)
    echo "usage: $0 {start|stop|restart|status}" >&2
    exit 2
    ;;
esac
