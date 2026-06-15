#!/usr/bin/env bash
set -euo pipefail

API_PORT="${COMPLEX_API_PORT:-${API_PORT:-8080}}"
API_ACTUATOR_URL="${API_ACTUATOR_URL:-http://127.0.0.1:$API_PORT/actuator/health}"
API_SYSTEM_URL="${API_SYSTEM_URL:-http://127.0.0.1:$API_PORT/api/v1/system/health}"
API_DOCS_URL="${API_DOCS_URL:-http://127.0.0.1:$API_PORT/docs/openapi}"
WEB_URL="${WEB_URL:-http://127.0.0.1:3000/}"

echo "api_actuator=$(curl -fsS "$API_ACTUATOR_URL")"
echo "api_system=$(curl -fsS "$API_SYSTEM_URL")"
echo "api_openapi_status=$(curl -fsS -o /dev/null -w '%{http_code}' "$API_DOCS_URL")"
web_status="$(curl -fsS -o /dev/null -w '%{http_code}' "$WEB_URL")"
echo "web_status=$web_status"

if [[ "$web_status" != "200" ]]; then
  exit 1
fi

for container in complex-postgres complex-redis complex-nats complex-minio; do
  if docker ps --format '{{.Names}}' | grep -qx "$container"; then
    echo "$container=running"
  else
    echo "$container=missing"
    exit 1
  fi
done
