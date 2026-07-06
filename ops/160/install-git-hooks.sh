#!/usr/bin/env bash
# 安装仓库 git 钩子到生效的 hooks 目录(worktree 共用)。新克隆/新机首次须跑一次。
# 注意:若仓库/全局设了 core.hooksPath,Git 只认该路径——装到别处会静默失效,故在此显式处理。
set -euo pipefail
cd "$(dirname "$0")/../.."
custom="$(git config --get core.hooksPath || true)"
if [[ -n "$custom" ]]; then
  dest="$custom"
  echo "[install-hooks] 检测到 core.hooksPath=$custom,安装到该路径(否则钩子不生效)。"
else
  dest="$(git rev-parse --git-common-dir)/hooks"
fi
mkdir -p "$dest"
install -m 0755 ops/160/hooks/pre-push "$dest/pre-push"
echo "installed pre-push hook -> $dest/pre-push"
