#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: complex-rollback-api <release-id>" >&2
  echo "Available releases:" >&2
  ls -1 /opt/complex/api/releases 2>/dev/null || true
  exit 1
fi

RELEASE_ID="$1"
BASE_DIR="${COMPLEX_API_BASE_DIR:-/opt/complex/api}"
RELEASE_DIR="$BASE_DIR/releases/$RELEASE_ID"
LOG_DIR="${COMPLEX_LOG_DIR:-/var/log/complex}"
SERVICE_NAME="${COMPLEX_API_SERVICE:-complex-api.service}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
COMPLEX_API_PORT="${COMPLEX_API_PORT:-${API_PORT:-8080}}"
COMPLEX_API_HOST="${COMPLEX_API_HOST:-${API_HOST:-127.0.0.1}}"
COMPLEX_DATABASE_URL="${COMPLEX_DATABASE_URL:-jdbc:postgresql://127.0.0.1:5432/complex}"
COMPLEX_DATABASE_USERNAME="${COMPLEX_DATABASE_USERNAME:-complex}"
COMPLEX_DATABASE_PASSWORD="${COMPLEX_DATABASE_PASSWORD:-}"

load_env() {
  if [[ -f /etc/complex/complex.env ]]; then
    set -a
    # shellcheck disable=SC1091
    source /etc/complex/complex.env
    set +a
  fi

  if [[ -f "$HOME/.complex/complex.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$HOME/.complex/complex.env"
    set +a
  fi
}

run_root() {
  if [[ "${EUID:-$(id -u)}" -eq 0 ]]; then
    "$@"
  elif command -v sudo >/dev/null 2>&1; then
    sudo "$@"
  else
    return 1
  fi
}

restart_api() {
  if command -v systemctl >/dev/null 2>&1 && run_root systemctl status "$SERVICE_NAME" >/dev/null 2>&1; then
    run_root systemctl restart "$SERVICE_NAME"
    return 0
  fi

  mkdir -p "$LOG_DIR"
  local pid_file="$BASE_DIR/complex-api.pid"
  if [[ -f "$pid_file" ]]; then
    local old_pid
    old_pid="$(cat "$pid_file")"
    if [[ -n "$old_pid" ]] && kill -0 "$old_pid" 2>/dev/null; then
      kill "$old_pid"
      sleep 1
    fi
  fi

  pkill -f "$BASE_DIR/current/complex-api.jar" 2>/dev/null || true
  cd "$BASE_DIR/current"
  setsid env \
    SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
    COMPLEX_API_PORT="$COMPLEX_API_PORT" \
    COMPLEX_API_HOST="$COMPLEX_API_HOST" \
    COMPLEX_DATABASE_URL="$COMPLEX_DATABASE_URL" \
    COMPLEX_DATABASE_USERNAME="$COMPLEX_DATABASE_USERNAME" \
    COMPLEX_DATABASE_PASSWORD="$COMPLEX_DATABASE_PASSWORD" \
    java -jar complex-api.jar > "$LOG_DIR/api.log" 2>&1 < /dev/null &
  echo "$!" > "$pid_file"
}

wait_for_health() {
  for _ in {1..60}; do
    if curl -fsS "http://127.0.0.1:$COMPLEX_API_PORT/actuator/health" >/dev/null; then
      return 0
    fi
    if curl -fsS "http://127.0.0.1:$COMPLEX_API_PORT/api/v1/system/health" >/dev/null; then
      return 0
    fi
    sleep 2
  done
  return 1
}

if [[ ! "$RELEASE_ID" =~ ^[0-9]{8}-[0-9]{6}$ ]]; then
  echo "Invalid API release id: $RELEASE_ID" >&2
  exit 1
fi

load_env

if [[ -x /opt/complex/scripts/complex-runtime-config-check.sh ]]; then
  /opt/complex/scripts/complex-runtime-config-check.sh
elif [[ -x "$HOME/complex-management/ops/160/complex-runtime-config-check.sh" ]]; then
  "$HOME/complex-management/ops/160/complex-runtime-config-check.sh"
else
  echo "complex-runtime-config-check.sh not found; rollback continues with inline minimal checks" >&2
fi

if ! command -v java >/dev/null 2>&1; then
  echo "java is required for API rollback" >&2
  exit 1
fi

RELEASE_DIR="$(realpath -m "$RELEASE_DIR")"
if [[ "$RELEASE_DIR" != "$BASE_DIR/releases/"* ]]; then
  echo "API release path escaped releases directory" >&2
  exit 1
fi

if [[ ! -f "$RELEASE_DIR/complex-api.jar" ]]; then
  echo "API release not found or not a Java release: $RELEASE_ID" >&2
  exit 1
fi

mkdir -p "$LOG_DIR"
ln -sfn "$RELEASE_DIR" "$BASE_DIR/current"
ln -sfn "$BASE_DIR/current/complex-api.jar" "$BASE_DIR/complex-api.jar"

restart_api

if wait_for_health; then
  echo "API rolled back to: $RELEASE_ID"
  exit 0
fi

echo "API rollback failed: $RELEASE_ID" >&2
exit 1
