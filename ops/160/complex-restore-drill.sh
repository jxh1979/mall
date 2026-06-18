#!/usr/bin/env bash
set -euo pipefail
umask 077

BACKUP_ARG="${1:-latest}"
BACKUP_KIND="${RESTORE_BACKUP_KIND:-manual}"
BACKUP_BASE="$(realpath -m "${COMPLEX_BACKUP_BASE:-/opt/complex/backups}")"
ENV_FILE="${COMPLEX_ENV_FILE:-/etc/complex/complex.env}"
HOME_ENV_FILE="$HOME/.complex/complex.env"
DRILL_STAMP="$(date +%Y%m%d_%H%M%S)"
DRILL_ID="restore-drill-$DRILL_STAMP"
RESTORE_DB="complex_restore_drill_$DRILL_STAMP"
RESTORE_ROOT="${COMPLEX_RESTORE_DRILL_DIR:-$HOME/.complex/restore-drills}"
REPORT_DIR="$RESTORE_ROOT/$DRILL_ID"
REPORT_FILE="$REPORT_DIR/restore-drill-report.json"
WORK_ROOT=""
RESTORE_DB_CREATED=false

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

cleanup() {
  if [[ "$RESTORE_DB_CREATED" == "true" && "${KEEP_RESTORE_DB:-false}" != "true" ]]; then
    docker exec complex-postgres dropdb -U complex "$RESTORE_DB" >/dev/null 2>&1 || true
  fi

  if [[ -n "$WORK_ROOT" && -d "$WORK_ROOT" ]]; then
    rm -rf "$WORK_ROOT"
  fi
}
trap cleanup EXIT

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "restore drill FAIL: $1 is not installed or not in PATH" >&2
    exit 1
  fi
}

resolve_backup_path() {
  if [[ "$BACKUP_ARG" != "latest" ]]; then
    realpath -m "$BACKUP_ARG"
    return 0
  fi

  local backup_kind_root
  backup_kind_root="$(realpath -m "$BACKUP_BASE/$BACKUP_KIND")"
  if [[ "$backup_kind_root" != "$BACKUP_BASE/"* ]]; then
    echo "restore drill FAIL: backup kind path escaped backup directory" >&2
    exit 1
  fi

  local latest
  latest="$(find "$backup_kind_root" -maxdepth 1 -type f \( -name '*.tar.gz.enc' -o -name '*.tar.gz' \) -printf '%T@ %p\n' 2>/dev/null | sort -nr | awk 'NR==1 {print $2}')"
  if [[ -z "$latest" ]]; then
    echo "restore drill FAIL: no backup archive found under $backup_kind_root" >&2
    exit 1
  fi
  echo "$latest"
}

find_passphrase_file() {
  local passphrase_file="${BACKUP_ENCRYPTION_PASSPHRASE_FILE:-}"
  if [[ -z "$passphrase_file" && -f "/etc/complex/backup.passphrase" ]]; then
    passphrase_file="/etc/complex/backup.passphrase"
  fi
  if [[ -z "$passphrase_file" && -f "$HOME/.complex/backup.passphrase" ]]; then
    passphrase_file="$HOME/.complex/backup.passphrase"
  fi

  if [[ -z "$passphrase_file" || ! -f "$passphrase_file" ]]; then
    echo "restore drill FAIL: encrypted backup passphrase file is required" >&2
    exit 1
  fi

  local mode
  mode="$(stat -c '%a' "$passphrase_file")"
  case "$mode" in
    *00) ;;
    *)
      echo "restore drill FAIL: backup passphrase file must not be group/world readable: $passphrase_file" >&2
      exit 1
      ;;
  esac

  echo "$passphrase_file"
}

verify_checksum_if_present() {
  local backup_path="$1"
  if [[ -d "$backup_path" ]]; then
    echo "checksum=directory"
    return 0
  fi

  local checksum_file="$backup_path.sha256"
  if [[ ! -f "$checksum_file" ]]; then
    echo "checksum=missing"
    return 0
  fi

  (
    cd "$(dirname "$backup_path")"
    sha256sum -c "$(basename "$checksum_file")" >/dev/null
  )
  echo "checksum=verified"
}

calculate_backup_sha() {
  local backup_path="$1"
  if [[ -d "$backup_path" ]]; then
    (
      cd "$backup_path"
      find . -type f -print0 | sort -z | xargs -0 sha256sum | sha256sum | awk '{print $1}'
    )
    return 0
  fi

  sha256sum "$backup_path" | awk '{print $1}'
}

extract_backup() {
  local backup_path="$1"
  local extract_root="$WORK_ROOT/extract"
  mkdir -p "$extract_root"

  if [[ -d "$backup_path" ]]; then
    echo "$backup_path"
    return 0
  fi

  if [[ "$backup_path" == *.tar.gz ]]; then
    tar -xzf "$backup_path" -C "$extract_root"
    echo "$extract_root"
    return 0
  fi

  if [[ "$backup_path" != *.tar.gz.enc ]]; then
    echo "restore drill FAIL: backup must be an encrypted .tar.gz.enc archive, an unencrypted .tar.gz archive, or an extracted backup directory" >&2
    exit 1
  fi

  local passphrase_file
  local archive_tmp
  require_cmd openssl
  passphrase_file="$(find_passphrase_file)"
  archive_tmp="$WORK_ROOT/backup.tar.gz"

  openssl enc -d -aes-256-cbc -salt -pbkdf2 -iter 210000 -md sha256 \
    -in "$backup_path" \
    -out "$archive_tmp" \
    -pass "file:$passphrase_file"

  tar -xzf "$archive_tmp" -C "$extract_root"
  echo "$extract_root"
}

psql_restore_db() {
  docker exec complex-postgres psql -U complex -d "$RESTORE_DB" -tAc "$1"
}

write_report() {
  local backup_path="$1"
  local backup_sha="$2"
  local started_at="$3"
  local finished_at="$4"
  local flyway_version="$5"
  local verified_tables="$6"

  mkdir -p "$REPORT_DIR"
  local restore_database_dropped
  restore_database_dropped="true"
  if [[ "${KEEP_RESTORE_DB:-false}" == "true" ]]; then
    restore_database_dropped="false"
  fi
  cat > "$REPORT_FILE" <<EOF
{
  "event": "restore.drill_completed",
  "restore_drill_id": "$DRILL_ID",
  "backup_path": "$backup_path",
  "backup_sha256": "$backup_sha",
  "restore_target": "$RESTORE_DB",
  "restore_database_dropped": $restore_database_dropped,
  "started_at": "$started_at",
  "finished_at": "$finished_at",
  "operator": "${USER:-unknown}",
  "verification_result": "passed",
  "flyway_version": "$flyway_version",
  "verified_tables": "$verified_tables",
  "audit_event": "restore.drill_completed"
}
EOF
  chmod 600 "$REPORT_FILE"
}

load_env
require_cmd docker
require_cmd sha256sum
require_cmd tar

if ! docker ps --format '{{.Names}}' | grep -qx 'complex-postgres'; then
  echo "restore drill FAIL: complex-postgres container is not running" >&2
  exit 1
fi

mkdir -p "$RESTORE_ROOT"
chmod 700 "$RESTORE_ROOT"
WORK_ROOT="$(mktemp -d "$RESTORE_ROOT/.work-$DRILL_ID.XXXXXX")"
started_at="$(date -Iseconds)"
backup_path="$(resolve_backup_path)"

if [[ ! -e "$backup_path" ]]; then
  echo "restore drill FAIL: backup not found: $backup_path" >&2
  exit 1
fi

checksum_status="$(verify_checksum_if_present "$backup_path")"
backup_sha="$(calculate_backup_sha "$backup_path")"
extract_root="$(extract_backup "$backup_path")"
dump_file="$(find "$extract_root" -type f -name 'postgres-complex.dump' | head -n 1)"

if [[ -z "$dump_file" || ! -f "$dump_file" ]]; then
  echo "restore drill FAIL: postgres-complex.dump not found in backup" >&2
  exit 1
fi

docker exec complex-postgres createdb -U complex "$RESTORE_DB"
RESTORE_DB_CREATED=true
docker exec -i complex-postgres pg_restore -U complex -d "$RESTORE_DB" < "$dump_file"

verified_table_count="$(psql_restore_db "select count(*) from pg_tables where schemaname='public' and tablename in ('flyway_schema_history','platform_migration_baseline','auth_credentials','auth_sessions');" | tr -d '[:space:]')"
if [[ "$verified_table_count" != "4" ]]; then
  echo "restore drill FAIL: expected 4 required tables, got $verified_table_count" >&2
  exit 1
fi

verified_tables="$(psql_restore_db "select string_agg(tablename, ',' order by tablename) from pg_tables where schemaname='public' and tablename in ('flyway_schema_history','platform_migration_baseline','auth_credentials','auth_sessions');" | tr -d '[:space:]')"
flyway_version="$(psql_restore_db "select coalesce(max(version),'none') from flyway_schema_history;" | tr -d '[:space:]')"
finished_at="$(date -Iseconds)"
write_report "$backup_path" "$backup_sha" "$started_at" "$finished_at" "$flyway_version" "$verified_tables"

echo "$checksum_status"
echo "restore.drill_completed report=$REPORT_FILE db=$RESTORE_DB flyway=$flyway_version tables=$verified_tables"
