#!/usr/bin/env bash
# 安装仓库 git 钩子到共享 git 目录(worktree 共用)。新克隆/新机首次须跑一次。
set -euo pipefail
cd "$(dirname "$0")/../.."
dest="$(git rev-parse --git-common-dir)/hooks"
install -m 0755 ops/160/hooks/pre-push "$dest/pre-push"
echo "installed pre-push hook -> $dest/pre-push"
