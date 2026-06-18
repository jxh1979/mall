#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${COMPLEX_ENV_FILE:-/etc/complex/complex.env}"
HOME_ENV_FILE="${HOME}/.complex/complex.env"
REQUIRE_CONTAINERS="${REQUIRE_CONTAINERS:-true}"

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

fail() {
  echo "runtime-config FAIL: $1" >&2
  exit 1
}

warn() {
  echo "runtime-config WARN: $1" >&2
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    fail "$1 is not installed or not in PATH"
  fi
}

require_non_empty() {
  local name="$1"
  local value="${!name:-}"
  if [[ -z "$value" ]]; then
    fail "$name must be set explicitly"
  fi
}

parse_jdbc_host_port() {
  local url="$1"
  local stripped="${url#jdbc:postgresql://}"
  local host_port="${stripped%%/*}"
  local jdbc_host="${host_port%%:*}"
  local port="${host_port##*:}"

  if [[ "$jdbc_host" == "$host_port" ]]; then
    port="5432"
  fi

  echo "$jdbc_host" "$port"
}

load_env

if [[ ! -f "$ENV_FILE" && ! -f "$HOME_ENV_FILE" ]]; then
  fail "no env file found at $ENV_FILE or $HOME_ENV_FILE"
fi

profile="${SPRING_PROFILES_ACTIVE:-local}"

require_cmd curl
require_cmd psql

require_non_empty COMPLEX_DATABASE_URL
if [[ "$COMPLEX_DATABASE_URL" != jdbc:postgresql://* ]]; then
  fail "COMPLEX_DATABASE_URL must use jdbc:postgresql://host:port/database format"
fi
if [[ "$COMPLEX_DATABASE_URL" == *"@"* ]]; then
  fail "COMPLEX_DATABASE_URL must not embed database credentials; use COMPLEX_DATABASE_USERNAME/PASSWORD"
fi
require_non_empty COMPLEX_DATABASE_USERNAME
require_non_empty COMPLEX_DATABASE_PASSWORD

if [[ -z "${COMPLEX_API_PORT:-}" ]]; then
  if [[ -n "${SERVER_PORT:-}" ]]; then
    warn "SERVER_PORT is set but COMPLEX_API_PORT is missing; Spring Boot uses COMPLEX_API_PORT, scripts also expect it"
  else
    warn "COMPLEX_API_PORT is missing; defaulting to 8080"
  fi
fi

if [[ "$profile" == "prod" && "$COMPLEX_DATABASE_PASSWORD" == "complex_dev_pwd" ]]; then
  fail "COMPLEX_DATABASE_PASSWORD must not use the development default in prod"
fi

read -r db_host db_port < <(parse_jdbc_host_port "$COMPLEX_DATABASE_URL")
if ! pg_isready -h "$db_host" -p "$db_port" >/dev/null 2>&1; then
  fail "PostgreSQL is not ready at $db_host:$db_port"
fi

if ! PGPASSWORD="$COMPLEX_DATABASE_PASSWORD" psql \
  -h "$db_host" \
  -p "$db_port" \
  -U "$COMPLEX_DATABASE_USERNAME" \
  -d "${COMPLEX_DATABASE_NAME:-complex}" \
  -tAc "select 1" >/dev/null; then
  fail "PostgreSQL credential check failed for configured database/user"
fi

if [[ "$REQUIRE_CONTAINERS" == "true" ]]; then
  require_cmd docker
  for container in complex-postgres complex-redis complex-nats complex-minio; do
    if docker ps --format '{{.Names}}' | grep -qx "$container"; then
      echo "runtime-config OK: $container running"
    else
      fail "$container container is not running"
    fi
  done
fi

echo "runtime-config OK: profile=$profile database=$db_host:$db_port api_port=${COMPLEX_API_PORT:-8080}"
