#!/usr/bin/env bash
set -euo pipefail
umask 077

BACKUP_KIND="${1:-manual}"
if [[ ! "$BACKUP_KIND" =~ ^(manual|daily|pre-deploy|pre-rollback|p0-final-clean)$ ]]; then
  echo "Invalid backup kind: $BACKUP_KIND" >&2
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_BASE="$(realpath -m "/opt/complex/backups")"
BACKUP_KIND_ROOT="$(realpath -m "$BACKUP_BASE/$BACKUP_KIND")"
BACKUP_ROOT="$(realpath -m "$BACKUP_KIND_ROOT/$STAMP")"
PROJECT_DIR="${COMPLEX_PROJECT_DIR:-$HOME/complex-management}"
NAS_BACKUP_TARGET_DIR="${NAS_BACKUP_TARGET_DIR:-}"
REQUIRE_NAS_BACKUP_SYNC="${REQUIRE_NAS_BACKUP_SYNC:-false}"
ARCHIVE_TMP=""

cleanup() {
  if [[ -n "$ARCHIVE_TMP" && -f "$ARCHIVE_TMP" ]]; then
    rm -f "$ARCHIVE_TMP"
  fi
}
trap cleanup EXIT

if [[ "$BACKUP_KIND_ROOT" != "$BACKUP_BASE/"* || "$BACKUP_ROOT" != "$BACKUP_KIND_ROOT/"* ]]; then
  echo "Backup path escaped backup directory" >&2
  exit 1
fi

write_checksum() {
  local artifact="$1"
  local checksum_file="$artifact.sha256"

  if ! command -v sha256sum >/dev/null 2>&1; then
    echo "sha256sum is required for backup checksum generation" >&2
    exit 1
  fi

  (
    cd "$(dirname "$artifact")"
    sha256sum "$(basename "$artifact")" > "$(basename "$checksum_file")"
  )
  chmod 600 "$checksum_file"
  echo "$checksum_file"
}

sync_to_nas() {
  local artifact="$1"
  local checksum_file="$2"

  if [[ -z "$NAS_BACKUP_TARGET_DIR" ]]; then
    if [[ "$REQUIRE_NAS_BACKUP_SYNC" == "true" ]]; then
      echo "NAS_BACKUP_TARGET_DIR must be set when REQUIRE_NAS_BACKUP_SYNC=true" >&2
      exit 1
    fi
    echo "NAS sync skipped: NAS_BACKUP_TARGET_DIR is not set"
    return 0
  fi

  local nas_root
  local nas_kind_root
  nas_root="$(realpath -m "$NAS_BACKUP_TARGET_DIR")"
  nas_kind_root="$(realpath -m "$nas_root/$BACKUP_KIND")"

  mkdir -p "$nas_kind_root"
  chmod 700 "$nas_kind_root"
  cp -p "$artifact" "$checksum_file" "$nas_kind_root/"
  echo "NAS sync completed: $nas_kind_root/$(basename "$artifact")"
}

ENCRYPT_BACKUP=true
PASSPHRASE_FILE="${BACKUP_ENCRYPTION_PASSPHRASE_FILE:-}"
if [[ "${ALLOW_UNENCRYPTED_BACKUPS:-false}" == "true" ]]; then
  ENCRYPT_BACKUP=false
elif [[ -z "$PASSPHRASE_FILE" && -f "/etc/complex/backup.passphrase" ]]; then
  PASSPHRASE_FILE="/etc/complex/backup.passphrase"
fi
if [[ "$ENCRYPT_BACKUP" == "true" && -z "$PASSPHRASE_FILE" && -f "$HOME/.complex/backup.passphrase" ]]; then
  PASSPHRASE_FILE="$HOME/.complex/backup.passphrase"
fi

if [[ "$ENCRYPT_BACKUP" == "true" && -z "$PASSPHRASE_FILE" ]]; then
  echo "Missing backup encryption passphrase. Set BACKUP_ENCRYPTION_PASSPHRASE_FILE or create /etc/complex/backup.passphrase." >&2
  exit 1
fi

if [[ "$ENCRYPT_BACKUP" == "true" ]]; then
  if [[ ! -f "$PASSPHRASE_FILE" ]]; then
    echo "Backup encryption passphrase file not found: $PASSPHRASE_FILE" >&2
    exit 1
  fi
  PASS_MODE="$(stat -c '%a' "$PASSPHRASE_FILE")"
  case "$PASS_MODE" in
    *00) ;;
    *)
      echo "Backup encryption passphrase file must not be group/world readable: $PASSPHRASE_FILE" >&2
      exit 1
      ;;
  esac
  if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl is required for encrypted backups" >&2
    exit 1
  fi
fi

mkdir -p "$BACKUP_ROOT"
chmod 700 "$BACKUP_ROOT"

echo "Creating backup at $BACKUP_ROOT"

if docker ps --format '{{.Names}}' | grep -qx 'complex-postgres'; then
  docker exec complex-postgres pg_dump -U complex -d complex --format=custom > "$BACKUP_ROOT/postgres-complex.dump"
fi

if [[ -d "$HOME/complex-env/infra" ]]; then
  mkdir -p "$BACKUP_ROOT/config"
  cp -a "$HOME/complex-env/infra/docker-compose.yml" "$BACKUP_ROOT/config/docker-compose.yml"
fi

if [[ -d "$PROJECT_DIR/docs" ]]; then
  mkdir -p "$BACKUP_ROOT/project"
  tar -czf "$BACKUP_ROOT/project/docs.tar.gz" -C "$PROJECT_DIR" docs
fi

if [[ -d "/etc/complex" ]]; then
  mkdir -p "$BACKUP_ROOT/etc"
  cp -a /etc/complex "$BACKUP_ROOT/etc/complex" 2>/dev/null || true
fi

if docker ps --format '{{.Names}}' | grep -qx 'complex-redis'; then
  docker exec complex-redis redis-cli SAVE >/dev/null
  mkdir -p "$BACKUP_ROOT/redis-data"
  docker cp complex-redis:/data/. "$BACKUP_ROOT/redis-data/"
  tar -czf "$BACKUP_ROOT/redis-data.tar.gz" -C "$BACKUP_ROOT/redis-data" .
  rm -rf "$BACKUP_ROOT/redis-data"
fi

if docker ps --format '{{.Names}}' | grep -qx 'complex-nats'; then
  mkdir -p "$BACKUP_ROOT/nats-data"
  docker cp complex-nats:/data/. "$BACKUP_ROOT/nats-data/" 2>/dev/null || true
  tar -czf "$BACKUP_ROOT/nats-data.tar.gz" -C "$BACKUP_ROOT/nats-data" .
  rm -rf "$BACKUP_ROOT/nats-data"
fi

if docker ps --format '{{.Names}}' | grep -qx 'complex-minio'; then
  mkdir -p "$BACKUP_ROOT/minio-data"
  docker cp complex-minio:/data/. "$BACKUP_ROOT/minio-data/"
  tar -czf "$BACKUP_ROOT/minio-data.tar.gz" -C "$BACKUP_ROOT/minio-data" .
  rm -rf "$BACKUP_ROOT/minio-data"
fi

find "$BACKUP_ROOT" -type d -exec chmod 700 {} +
find "$BACKUP_ROOT" -type f -exec chmod 600 {} +

if [[ "$ENCRYPT_BACKUP" == "true" ]]; then
  ENCRYPTED_BACKUP="$(realpath -m "$BACKUP_KIND_ROOT/$STAMP.tar.gz.enc")"
  if [[ "$ENCRYPTED_BACKUP" != "$BACKUP_KIND_ROOT/"*.tar.gz.enc ]]; then
    echo "Encrypted backup path escaped backup directory" >&2
    exit 1
  fi
  ARCHIVE_TMP="$(mktemp "$BACKUP_KIND_ROOT/.${STAMP}.tar.gz.XXXXXX")"
  tar -czf "$ARCHIVE_TMP" -C "$BACKUP_KIND_ROOT" "$STAMP"
  openssl enc -aes-256-cbc -salt -pbkdf2 -iter 210000 -md sha256 \
    -in "$ARCHIVE_TMP" \
    -out "$ENCRYPTED_BACKUP" \
    -pass "file:$PASSPHRASE_FILE"
  chmod 600 "$ENCRYPTED_BACKUP"
  CHECKSUM_FILE="$(write_checksum "$ENCRYPTED_BACKUP")"
  sync_to_nas "$ENCRYPTED_BACKUP" "$CHECKSUM_FILE"
  rm -f "$ARCHIVE_TMP"
  ARCHIVE_TMP=""
  rm -rf "$BACKUP_ROOT"
  echo "Backup completed: $ENCRYPTED_BACKUP"
else
  UNENCRYPTED_BACKUP="$(realpath -m "$BACKUP_KIND_ROOT/$STAMP.tar.gz")"
  if [[ "$UNENCRYPTED_BACKUP" != "$BACKUP_KIND_ROOT/"*.tar.gz ]]; then
    echo "Unencrypted backup path escaped backup directory" >&2
    exit 1
  fi
  tar -czf "$UNENCRYPTED_BACKUP" -C "$BACKUP_KIND_ROOT" "$STAMP"
  chmod 600 "$UNENCRYPTED_BACKUP"
  CHECKSUM_FILE="$(write_checksum "$UNENCRYPTED_BACKUP")"
  sync_to_nas "$UNENCRYPTED_BACKUP" "$CHECKSUM_FILE"
  rm -rf "$BACKUP_ROOT"
  echo "Backup completed without encryption: $UNENCRYPTED_BACKUP"
fi

find "$BACKUP_BASE/daily" -mindepth 1 -maxdepth 1 \( -type d -o -name '*.tar.gz.enc' -o -name '*.tar.gz' \) -mtime +14 -print -exec rm -rf {} + 2>/dev/null || true
