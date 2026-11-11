#!/usr/bin/env bash
# 文件版本：V0.03
# 合并前置检查(2026-08-07 门禁升级批 1,用户弹窗批立项):CI 绿灯才允许合并。
# 病史:本仓是私有库免费版,设不了 required status check,.github/workflows/predeploy-gates.yml
#      自己在注释里写明只是「可见信号」。也就是说 gh pr merge 对红灯毫无抵抗力,全靠人记得去看一眼。
#      「靠人看一眼」这件事在本仓已经失手过(门禁加了整轮一次没跑过)。此脚本把那一眼变成机器动作。
# 用法:在已 push 且已开 PR 的 feature 分支上执行:
#      ops/160/merge-pr.sh [--squash|--merge|--rebase]   (默认 --squash)
# 逃生口:SKIP_CI_WAIT=1 ops/160/merge-pr.sh   —— 只在 CI 本身故障时用,会打印醒目警告。
# V0.03(2026-11-11 第十八批裁决 48:不升级 Pro、不公开仓库,只加强本脚本):除 predeploy-gates 外,
#      backend-tests、web-tests 对本提交触发了的也要等绿才合并;这两份按改动路径触发,没触发的视为不需要。
set -euo pipefail
method="${1:---squash}"
case "$method" in --squash|--merge|--rebase) ;; *) echo "合并方式只能是 --squash|--merge|--rebase" >&2; exit 1;; esac

branch="$(git rev-parse --abbrev-ref HEAD)"
[[ "$branch" == "main" ]] && { echo "[merge] main 上没有待合并的 PR;请切到 feature 分支。" >&2; exit 1; }

if [[ -n "$(git status --porcelain)" ]]; then
  echo "[merge] 工作区有未提交改动;CI 跑的是远端 commit,这些改动不在里面。" >&2
  echo "        先提交并 push,或 stash 掉再合。" >&2
  git status --porcelain >&2
  exit 1
fi

head_sha="$(git rev-parse HEAD)"
remote_sha="$(git ls-remote origin "refs/heads/$branch" | cut -f1)"
if [[ -z "$remote_sha" ]]; then
  echo "[merge] origin 上没有 $branch;先跑 ops/160/propose-change.sh 推分支开 PR。" >&2; exit 1
fi
if [[ "$head_sha" != "$remote_sha" ]]; then
  echo "[merge] 本地 HEAD(${head_sha:0:8}) 与 origin/$branch(${remote_sha:0:8}) 不一致——" >&2
  echo "        CI 跑的是远端那个 commit,合并前必须先 push,否则等的是别的代码的绿灯。" >&2; exit 1
fi
gh pr view "$branch" >/dev/null 2>&1 || { echo "[merge] $branch 还没有 PR;先跑 ops/160/propose-change.sh。" >&2; exit 1; }

if [[ "${SKIP_CI_WAIT:-0}" == "1" ]]; then
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" >&2
  echo "!! SKIP_CI_WAIT=1:跳过 CI 绿灯检查,直接合并 ${head_sha:0:8} 到 main !!" >&2
  echo "!! 治理门禁(predeploy-gates)本次未被确认通过。请在 PR 里写明原因。   !!" >&2
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" >&2
else
  echo "[merge] 等 predeploy-gates 对 ${head_sha:0:8} 的 CI 结果..."
  run_ids=""
  for i in $(seq 1 30); do
    run_ids="$(gh run list --commit "$head_sha" --workflow predeploy-gates.yml --limit 10 --json databaseId -q '.[].databaseId' )"
    [[ -n "$run_ids" ]] && break
    echo "[merge]   还没有 run(第 $i/30 次),10s 后重试..."
    sleep 10
  done
  if [[ -z "$run_ids" ]]; then
    echo "[merge] 等了 5 分钟仍没有 ${head_sha:0:8} 的 predeploy-gates run,中止合并。" >&2
    echo "        可能原因:workflow 被禁用、push 的不是这个 commit、或 Actions 排队异常。" >&2
    echo "        CI 确实故障时可用 SKIP_CI_WAIT=1 强行合并(会打印警告并需在 PR 说明原因)。" >&2
    exit 1
  fi
  # 三份工作流由同一次 PR 事件同时建 run;predeploy 已有 run 后,另两份再给 1 分钟宽限,仍没有即本次改动未触发。
  for wf in backend-tests.yml web-tests.yml; do
    wf_ids=""
    for i in $(seq 1 6); do
      wf_ids="$(gh run list --commit "$head_sha" --workflow "$wf" --limit 10 --json databaseId -q '.[].databaseId' )"
      [[ -n "$wf_ids" ]] && break
      sleep 10
    done
    if [[ -z "$wf_ids" ]]; then
      echo "[merge] ${wf} 对 ${head_sha:0:8} 没有触发(改动不涉及它的路径),视为不需要。"
    else
      echo "[merge] 另等 ${wf} 对 ${head_sha:0:8} 的 CI 结果..."
      run_ids="$run_ids $wf_ids"
    fi
  done
  for rid in $run_ids; do
    if ! gh run watch "$rid" --exit-status --compact; then
      echo "[merge] CI 未通过(run $rid),中止合并。" >&2
      gh run view "$rid" --json url -q .url >&2 || true
      exit 1
    fi
  done
  echo "[merge] CI 全绿($(echo $run_ids | wc -w) 个 run),继续合并。"
fi

# 合并必须钉住刚才看绿的那个 commit:等待 CI 期间若分支被推了新 commit,按分支名合并会把未验的 tip 合进 main。
remote_now="$(git ls-remote origin "refs/heads/$branch" | cut -f1)"
pr_head="$(gh pr view "$branch" --json headRefOid -q .headRefOid)"
if [[ "$remote_now" != "$head_sha" || "$pr_head" != "$head_sha" ]]; then
  echo "[merge] 分支在等待 CI 期间被更新:已验 ${head_sha:0:8},远端 ${remote_now:0:8},PR head ${pr_head:0:8}。中止,请对新 commit 重跑本脚本。" >&2
  exit 1
fi
gh pr merge "$branch" "$method" --match-head-commit "$head_sha"
echo "[merge] 已合并 $branch($method),head ${head_sha:0:8}。"
