#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${COMPLEX_PROJECT_DIR:-$HOME/complex-management}"
BASE_DIR="${COMPLEX_API_BASE_DIR:-/opt/complex/api}"
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

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 is required for Java backend deployment" >&2
    exit 1
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

write_systemd_service() {
  local service_user="${COMPLEX_SERVICE_USER:-${USER:-james}}"
  local service_file="/etc/systemd/system/$SERVICE_NAME"
  local content
  content="$(cat <<EOF
[Unit]
Description=Complex Management Java API
After=network.target

[Service]
Type=simple
User=$service_user
WorkingDirectory=$BASE_DIR/current
EnvironmentFile=-/etc/complex/complex.env
Environment=SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE
ExecStart=$(command -v java) -jar $BASE_DIR/current/complex-api.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF
)"

  if [[ "${EUID:-$(id -u)}" -eq 0 ]]; then
    printf '%s\n' "$content" > "$service_file"
  elif command -v sudo >/dev/null 2>&1; then
    printf '%s\n' "$content" | sudo tee "$service_file" >/dev/null
  else
    return 1
  fi
}

restart_api() {
  if command -v systemctl >/dev/null 2>&1 && write_systemd_service; then
    run_root systemctl daemon-reload
    run_root systemctl enable "$SERVICE_NAME" >/dev/null
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

load_env

require_cmd npm
require_cmd curl

if [[ -x /opt/complex/scripts/complex-runtime-config-check.sh ]]; then
  /opt/complex/scripts/complex-runtime-config-check.sh
elif [[ -x "$PROJECT_DIR/ops/160/complex-runtime-config-check.sh" ]]; then
  "$PROJECT_DIR/ops/160/complex-runtime-config-check.sh"
else
  echo "complex-runtime-config-check.sh not found; deployment continues with inline minimal checks" >&2
fi

if [[ "$SPRING_PROFILES_ACTIVE" == "prod" && ( -z "$COMPLEX_DATABASE_PASSWORD" || "$COMPLEX_DATABASE_PASSWORD" == "complex_dev_pwd" ) ]]; then
  echo "COMPLEX_DATABASE_PASSWORD must be set to a production value before prod deployment" >&2
  exit 1
fi

mkdir -p "$LOG_DIR" "$BASE_DIR/releases"

release_id="$(date +%Y%m%d-%H%M%S)"
release_dir="$BASE_DIR/releases/$release_id"

echo "[$(date -Iseconds)] deploy api start release=$release_id stack=java-spring-boot" | tee -a "$LOG_DIR/deploy.log"

cd "$PROJECT_DIR"
npm run verify:backend-env
node tools/run-backend-maven.mjs clean package

jar_file="$(find backend/target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"
if [[ -z "$jar_file" || ! -f "$jar_file" ]]; then
  echo "Spring Boot jar not found under backend/target" >&2
  exit 1
fi

mkdir -p "$release_dir"
cp "$jar_file" "$release_dir/complex-api.jar"
cat > "$release_dir/build-info.json" <<EOF
{
  "releaseId": "$release_id",
  "releasedAt": "$(date -Iseconds)",
  "projectDir": "$PROJECT_DIR",
  "runtime": "java-21-spring-boot-4",
  "artifact": "complex-api.jar"
}
EOF

ln -sfn "$release_dir" "$BASE_DIR/current"
ln -sfn "$BASE_DIR/current/complex-api.jar" "$BASE_DIR/complex-api.jar"

restart_api

if wait_for_health; then
  echo "[$(date -Iseconds)] deploy api ok release=$release_id" | tee -a "$LOG_DIR/deploy.log"
  exit 0
fi

echo "[$(date -Iseconds)] deploy api failed release=$release_id" | tee -a "$LOG_DIR/deploy.log"
exit 1
