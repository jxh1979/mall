#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${COMPLEX_PROJECT_DIR:-$HOME/complex-management}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${COMPLEX_ENV_FILE:-/etc/complex/complex.env}"
HOME_ENV_FILE="$HOME/.complex/complex.env"
AUTO_START_RUNTIME="${AUTO_START_RUNTIME:-false}"

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

runtime_script() {
  echo "$SCRIPT_DIR/complex-runtime.sh"
}

runtime_up_if_requested() {
  if [[ "$AUTO_START_RUNTIME" != "true" ]]; then
    return 0
  fi

  local script
  script="$(runtime_script)"
  if [[ ! -x "$script" ]]; then
    echo "P0 verification FAIL: $script is required when AUTO_START_RUNTIME=true." >&2
    exit 1
  fi

  echo "runtime up"
  "$script" up
}

runtime_status_if_requested() {
  if [[ "$AUTO_START_RUNTIME" != "true" ]]; then
    return 0
  fi

  local script
  script="$(runtime_script)"
  echo "runtime status"
  "$script" status
}

require_android_smoke_inputs() {
  export P0_API_BASE_URL="${P0_API_BASE_URL:-http://127.0.0.1:${COMPLEX_API_PORT:-8080}}"

  if [[ -z "${P0_ADMIN_USERNAME:-}" || -z "${P0_ADMIN_PASSWORD:-}" ]]; then
    echo "Android P0 smoke requires P0_ADMIN_USERNAME and P0_ADMIN_PASSWORD in $ENV_FILE or $HOME_ENV_FILE." >&2
    echo "Do not mark complete P0 verification as passed until a seeded admin account and required P0 business APIs are available." >&2
    exit 1
  fi
}

cd "$PROJECT_DIR"
load_env

npm run verify:p0-contract
runtime_up_if_requested
"$SCRIPT_DIR/complex-verify-be000.sh"

npm run build
npm run verify:android-env
npm run android:p0:build
require_android_smoke_inputs
npm run android:p0:smoke
npm run android:p0:apk

runtime_status_if_requested
"$SCRIPT_DIR/complex-health-check.sh"

echo "P0 verification PASS"
