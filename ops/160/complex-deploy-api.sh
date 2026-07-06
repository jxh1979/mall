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
  # 2026-07-02 部署可靠化:本机常态=用户级 systemd(~/.config/systemd/user/complex-api.service,
  # Restart=always 看护,linger 已开)。user unit 重启无需 sudo,且必须发生在 current 软链切换之后,
  # 否则 unit 自动拉起的进程会挂着旧 release 的 jar(fd 指向旧目录,实证 2026-07-02 两次踩坑)。
  if command -v systemctl >/dev/null 2>&1 && systemctl --user cat "$SERVICE_NAME" >/dev/null 2>&1; then
    systemctl --user daemon-reload 2>/dev/null || true
    systemctl --user restart "$SERVICE_NAME"
    return 0
  fi

  # 系统级 systemd 需要 sudo;无终端时 sudo 失败(不再静默:提示后走手工路径)。
  if command -v systemctl >/dev/null 2>&1 && write_systemd_service; then
    run_root systemctl daemon-reload
    run_root systemctl enable "$SERVICE_NAME" >/dev/null
    run_root systemctl restart "$SERVICE_NAME"
    return 0
  fi
  echo "systemd restart unavailable (user unit missing, system unit needs sudo); falling back to manual restart" >&2

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

  # 旧进程可能以相对路径(java -jar complex-api.jar)或绝对 symlink 各形态启动,
  # 按命令行模糊匹配统一清理(原先只匹配 $BASE_DIR/current 绝对路径,旧进程续跑=踩坑根因之一)。
  pkill -f 'java .*complex-api\.jar' 2>/dev/null || true
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

# 部署收尾铁律:核对「监听端口的进程实际加载的 jar」==「current 软链指向的 release」。
# systemd/看护进程在软链切换前抢先拉起时会挂旧版本,健康检查发现不了(旧版本也健康)。
verify_running_release() {
  local pid="" jar expected
  for _ in {1..10}; do
    pid="$(ss -ltnp 2>/dev/null | grep -E "[:.]$COMPLEX_API_PORT[[:space:]]" | grep -o 'pid=[0-9]*' | head -n1 | cut -d= -f2)"
    [[ -n "$pid" ]] && break
    sleep 1
  done
  if [[ -z "$pid" ]]; then
    echo "verify_running_release: no process listening on :$COMPLEX_API_PORT" >&2
    return 1
  fi
  jar="$(ls -l "/proc/$pid/fd" 2>/dev/null | grep -o '/opt/complex/api/releases/[^ ]*complex-api.jar' | head -n1)"
  expected="$(readlink -f "$BASE_DIR/current")/complex-api.jar"
  if [[ "$jar" != "$expected" ]]; then
    echo "RUNNING RELEASE MISMATCH: pid=$pid loaded [$jar] but current -> [$expected]" >&2
    echo "手工恢复:systemctl --user restart $SERVICE_NAME,然后 ls -l /proc/\$(pgrep -f complex-api.jar)/fd | grep releases 核对。" >&2
    return 1
  fi
  echo "[$(date -Iseconds)] running release verified: pid=$pid jar=$jar" | tee -a "$LOG_DIR/deploy.log"
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
# 部署前治理门禁(2026-07-05):delivery-gates 快子集绿才允许部署;红即中止不碰生产。
# 覆盖 codex 审计暴露的漂移类(scope 登记/安全基线/后台矩阵/契约/阻断台账),静态分析无需 DB。
# 门禁完整性守卫(2026-07-06,codex 复核:防同权限者在 PR 里把 verify:predeploy 抽空/设 true 后蒙混部署)。
# 部署脚本(在 .160 部署机、不随 PR 改动)重新校验 verify:predeploy 仍含必需安全/治理门禁;缺则中止。
# 说明:这不是"仓外强制"(受保护 CI 才是),但把篡改门槛从"改一行 package.json"抬到"改 .160 部署脚本"。
if ! node -e 'const s=(require("./package.json").scripts||{})["verify:predeploy"]||"";const req=["verify:tenant-isolation","verify:security-baseline","verify:pii-encryption","verify:production-blockers","verify:full-admin-matrix","verify:test-db-isolation","verify:p0-contract"];const miss=req.filter(g=>!s.includes(g));if(miss.length){console.error("PREDEPLOY_TAMPERED 缺必需门禁: "+miss.join(", "));process.exit(1);}' ; then
  echo "[$(date -Iseconds)] DEPLOY ABORTED: verify:predeploy 被篡改/抽空(缺必需门禁),拒绝部署。" | tee -a "$LOG_DIR/deploy.log" >&2
  exit 1
fi
echo "[$(date -Iseconds)] pre-deploy governance gates: npm run verify:predeploy" | tee -a "$LOG_DIR/deploy.log"
if ! npm run verify:predeploy; then
  echo "[$(date -Iseconds)] DEPLOY ABORTED: delivery-gates red (verify:predeploy failed). Fix governance drift before deploying." | tee -a "$LOG_DIR/deploy.log" >&2
  exit 1
fi
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
  if ! verify_running_release; then
    echo "[$(date -Iseconds)] deploy api failed (running release mismatch) release=$release_id" | tee -a "$LOG_DIR/deploy.log"
    exit 1
  fi
  echo "[$(date -Iseconds)] deploy api ok release=$release_id" | tee -a "$LOG_DIR/deploy.log"
  exit 0
fi

echo "[$(date -Iseconds)] deploy api failed release=$release_id" | tee -a "$LOG_DIR/deploy.log"
exit 1
