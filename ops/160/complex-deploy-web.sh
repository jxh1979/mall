#!/usr/bin/env bash
# 文件版本：V0.03
set -euo pipefail

PROJECT_DIR="${COMPLEX_PROJECT_DIR:-$HOME/complex-management}"
LOG_DIR="${COMPLEX_LOG_DIR:-/var/log/complex}"
WEB_PORT="${WEB_PORT:-3000}"
WEB_HOST="${WEB_HOST:-127.0.0.1}"

mkdir -p "$LOG_DIR" /opt/complex/web/releases

release_id="$(date +%Y%m%d-%H%M%S)"
release_dir="/opt/complex/web/releases/$release_id"

echo "[$(date -Iseconds)] deploy web start release=$release_id" | tee -a "$LOG_DIR/deploy.log"

cd "$PROJECT_DIR"
# 部署前治理门禁(2026-07-05):同 deploy-api,delivery-gates 快子集绿才允许部署。
# 部署门禁(2026-07-06,codex 复核后加固):部署脚本直接跑权威门禁列表,不解析/信任 package.json 里
# 可被抽空或惰性化(如 verify:predeploy="true # 门禁名...")的脚本串——"改 package.json 让门禁空跑"这条路被堵死。
# 仍非仓外强制:能改本部署脚本(需 .160 shell)或 tools/verify-*.mjs 者仍可绕过;真闭合需受保护 CI(见 docs/deploy-and-change-flow)。
# 普通 PR 合并发布比较第一父提交；批量发布可沿用显式 HTML_GATES_BASE。
export HTML_GATES_BASE="${HTML_GATES_BASE:-HEAD~1}"
REQUIRED_GATES=(
  verify:html-ledger-sync verify:entry-anchors verify:html-param-tier
  verify:delivery-gates-docs verify:full-admin-matrix verify:domain-ownership-scope
  verify:matrix-vs-migration verify:data-isolation-spec verify:tenant-isolation
  verify:p0-contract verify:security-baseline verify:production-readiness-evidence
  verify:production-blockers verify:pii-encryption verify:test-db-isolation
)
for gate in "${REQUIRED_GATES[@]}"; do
  echo "[$(date -Iseconds)] pre-deploy gate: npm run $gate" | tee -a "$LOG_DIR/deploy.log"
  if ! npm run "$gate"; then
    echo "[$(date -Iseconds)] DEPLOY ABORTED: $gate 未过,拒绝部署。" | tee -a "$LOG_DIR/deploy.log" >&2
    exit 1
  fi
done
npm run build --workspace apps/web

mkdir -p "$release_dir"
mkdir -p "$release_dir/dist"
cp -a apps/web/dist/. "$release_dir/dist/"
cat > "$release_dir/build-info.json" <<EOF
{
  "releaseId": "$release_id",
  "releasedAt": "$(date -Iseconds)",
  "projectDir": "$PROJECT_DIR",
  "runtime": "vite-preview-p0"
}
EOF

ln -sfn "$release_dir" /opt/complex/web/current

pids="$(ps -eo pid=,args= | awk '$0 ~ /vite preview .*--port 3000/ && $0 !~ /awk/ {print $1}')"
if [[ -n "$pids" ]]; then
  kill $pids
  sleep 1
fi

cd "$PROJECT_DIR/apps/web"
setsid npx vite preview --host "$WEB_HOST" --port "$WEB_PORT" --outDir /opt/complex/web/current/dist > "$LOG_DIR/web.log" 2>&1 < /dev/null &

for _ in {1..20}; do
  if curl -fsS "http://127.0.0.1:$WEB_PORT/" >/dev/null; then
    echo "[$(date -Iseconds)] deploy web ok release=$release_id" | tee -a "$LOG_DIR/deploy.log"
    exit 0
  fi
  sleep 1
done

echo "[$(date -Iseconds)] deploy web failed release=$release_id" | tee -a "$LOG_DIR/deploy.log"
exit 1
