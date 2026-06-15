#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-status}"
PROJECT_DIR="${COMPLEX_PROJECT_DIR:-$HOME/complex-management}"
ENV_FILE="${COMPLEX_ENV_FILE:-/etc/complex/complex.env}"
HOME_ENV_FILE="$HOME/.complex/complex.env"

load_env() {
  if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
  fi

  if [[ -f "$HOME_ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$HOME_ENV_FILE"
    set +a
  fi
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "runtime FAIL: $1 is not installed or not in PATH" >&2
    exit 1
  fi
}

load_env

API_PORT="${COMPLEX_API_PORT:-${API_PORT:-8080}}"
WEB_PORT="${WEB_PORT:-3000}"
WEB_HOST="${WEB_HOST:-127.0.0.1}"
RUNTIME_DIR="${COMPLEX_RUNTIME_DIR:-$HOME/.complex/runtime}"
LOG_DIR="${COMPLEX_RUNTIME_LOG_DIR:-$HOME/.complex/logs}"
API_PID_FILE="$RUNTIME_DIR/complex-api.pid"
WEB_PID_FILE="$RUNTIME_DIR/complex-web.pid"
API_JAR="$PROJECT_DIR/backend/target/complex-api-0.1.0-SNAPSHOT.jar"

api_url() {
  echo "http://127.0.0.1:$API_PORT/actuator/health"
}

web_url() {
  echo "http://127.0.0.1:$WEB_PORT/"
}

is_running_pid() {
  local pid_file="$1"
  if [[ ! -f "$pid_file" ]]; then
    return 1
  fi

  local pid
  pid="$(cat "$pid_file")"
  if [[ -z "$pid" ]]; then
    return 1
  fi

  kill -0 "$pid" 2>/dev/null
}

wait_for_url() {
  local url="$1"
  local attempts="${2:-40}"
  for _ in $(seq 1 "$attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

ensure_dirs() {
  mkdir -p "$RUNTIME_DIR" "$LOG_DIR"
}

ensure_api_jar() {
  if [[ -f "$API_JAR" ]]; then
    return 0
  fi

  require_cmd npm
  cd "$PROJECT_DIR"
  node tools/run-backend-maven.mjs -DskipTests package

  if [[ ! -f "$API_JAR" ]]; then
    echo "runtime FAIL: API jar not found at $API_JAR" >&2
    exit 1
  fi
}

start_api() {
  require_cmd curl
  require_cmd java
  ensure_dirs

  if wait_for_url "$(api_url)" 1; then
    echo "api=running"
    return 0
  fi

  ensure_api_jar
  cd "$PROJECT_DIR"
  setsid java -jar "$API_JAR" > "$LOG_DIR/api.log" 2>&1 < /dev/null &
  echo "$!" > "$API_PID_FILE"

  if wait_for_url "$(api_url)" 60; then
    echo "api=running"
    return 0
  fi

  echo "runtime FAIL: API did not become healthy on port $API_PORT" >&2
  tail -n 120 "$LOG_DIR/api.log" >&2 || true
  exit 1
}

start_web() {
  require_cmd curl
  require_cmd npm
  ensure_dirs

  if wait_for_url "$(web_url)" 1; then
    echo "web=running"
    return 0
  fi

  cd "$PROJECT_DIR"
  setsid npm run dev --workspace apps/web -- --host "$WEB_HOST" --port "$WEB_PORT" --strictPort > "$LOG_DIR/web.log" 2>&1 < /dev/null &
  echo "$!" > "$WEB_PID_FILE"

  if wait_for_url "$(web_url)" 40; then
    echo "web=running"
    return 0
  fi

  echo "runtime FAIL: Web did not become healthy on port $WEB_PORT" >&2
  tail -n 120 "$LOG_DIR/web.log" >&2 || true
  exit 1
}

stop_pid() {
  local label="$1"
  local pid_file="$2"

  if ! is_running_pid "$pid_file"; then
    rm -f "$pid_file"
    echo "$label=stopped"
    return 0
  fi

  local pid
  pid="$(cat "$pid_file")"
  kill "$pid"

  for _ in {1..20}; do
    if ! kill -0 "$pid" 2>/dev/null; then
      rm -f "$pid_file"
      echo "$label=stopped"
      return 0
    fi
    sleep 1
  done

  kill -TERM "$pid" 2>/dev/null || true
  rm -f "$pid_file"
  echo "$label=stopped"
}

stop_api() {
  stop_pid "api" "$API_PID_FILE"
}

stop_web() {
  stop_pid "web" "$WEB_PID_FILE"
}

status() {
  require_cmd curl
  local ok=0

  if wait_for_url "$(api_url)" 1; then
    echo "api=running"
  else
    echo "api=down"
    ok=1
  fi

  if wait_for_url "$(web_url)" 1; then
    echo "web=running"
  else
    echo "web=down"
    ok=1
  fi

  return "$ok"
}

case "$ACTION" in
  up)
    start_api
    start_web
    ;;
  down)
    stop_web
    stop_api
    ;;
  restart)
    stop_web
    stop_api
    start_api
    start_web
    ;;
  status)
    status
    ;;
  *)
    echo "Usage: $0 {up|down|restart|status}" >&2
    exit 2
    ;;
esac
