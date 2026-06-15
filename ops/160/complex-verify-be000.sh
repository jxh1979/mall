#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${COMPLEX_PROJECT_DIR:-$HOME/complex-management}"
API_PORT="${COMPLEX_API_PORT:-${API_PORT:-8080}}"
REQUIRE_RUNNING_API="${REQUIRE_RUNNING_API:-true}"

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
    echo "BE-000 FAIL: $1 is not installed or not in PATH" >&2
    exit 1
  fi
}

load_env
require_cmd npm
require_cmd curl

cd "$PROJECT_DIR"

if [[ ! -f backend/pom.xml ]]; then
  echo "BE-000 FAIL: backend/pom.xml not found" >&2
  exit 1
fi

if ! grep -q "<java.version>21</java.version>" backend/pom.xml; then
  echo "BE-000 FAIL: backend/pom.xml must target Java 21" >&2
  exit 1
fi

if ! grep -q "<artifactId>spring-boot-starter-parent</artifactId>" backend/pom.xml || ! grep -q "<version>4\\." backend/pom.xml; then
  echo "BE-000 FAIL: backend/pom.xml must use Spring Boot 4.x" >&2
  exit 1
fi

if [[ ! -f backend/src/main/resources/db/migration/V001__baseline.sql ]]; then
  echo "BE-000 FAIL: Flyway baseline migration not found" >&2
  exit 1
fi

npm run verify:backend-env
npm run test:backend
node tools/run-backend-maven.mjs -DskipTests package

jar_file="$(find backend/target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"
if [[ -z "$jar_file" || ! -f "$jar_file" ]]; then
  echo "BE-000 FAIL: packaged Spring Boot jar not found" >&2
  exit 1
fi

echo "BE-000 build OK: $jar_file"

if [[ "$REQUIRE_RUNNING_API" == "true" ]]; then
  curl -fsS "http://127.0.0.1:$API_PORT/actuator/health" >/dev/null
  curl -fsS "http://127.0.0.1:$API_PORT/api/v1/system/health" >/dev/null
  curl -fsS "http://127.0.0.1:$API_PORT/docs/openapi" >/dev/null
  echo "BE-000 runtime OK: health and OpenAPI endpoints reachable"
else
  echo "BE-000 runtime check skipped because REQUIRE_RUNNING_API=false"
fi

echo "BE-000 PASS"
