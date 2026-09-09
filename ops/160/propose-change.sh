#!/usr/bin/env bash
# 文件版本：V0.01
# 变更提议流程(2026-07-05 起替代直推 main):
#   推 feature 分支 → 开/更新 PR → (调用方另跑独立复核) → predeploy 门禁绿 → ops/160/merge-pr.sh。
#   合并动作一律走 merge-pr.sh(它会等 CI 绿),不要手敲 gh pr merge;CI 故障需 SKIP_CI_WAIT=1 时须在 PR 说明原因。
# 用法:在含已提交改动的 feature 分支上执行:ops/160/propose-change.sh "<PR标题>" ["<PR正文>"]
set -euo pipefail
title="${1:?需要 PR 标题}"; body="${2:-}"
branch="$(git rev-parse --abbrev-ref HEAD)"
[[ "$branch" == "main" ]] && { echo "不要在 main 上提议;先切 feature 分支。" >&2; exit 1; }

if [[ -n "$(git status --porcelain)" ]]; then
  echo "[propose] 工作区有未提交改动;请先提交或 stash,再运行本地检查并推送。" >&2
  exit 1
fi

echo "[propose] 本地跑 predeploy 门禁(红则不推)..."
npm run verify:predeploy

echo "[propose] 推 feature 分支 $branch ..."
git push -u origin "$branch"

if gh pr view "$branch" >/dev/null 2>&1; then
  echo "[propose] PR 已存在,已更新:"; gh pr view "$branch" --json url -q .url
else
  gh pr create --base main --head "$branch" --title "$title" --body "${body:-自动创建;详见提交说明。独立复核见 PR 评论。}"
fi
echo "[propose] 下一步:①独立复核(codex/评审)②合并一律走 ops/160/merge-pr.sh"
echo "[propose]   merge-pr.sh 会先等 GitHub Actions predeploy-gates 对本 commit 变绿再 gh pr merge;"
echo "[propose]   本仓是私有库免费版设不了 required status check,别再手敲 gh pr merge 绕过这道检查。"
