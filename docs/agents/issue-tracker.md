# Issue tracker: GitHub

本仓库的 issue 与 PRD 记在 GitHub issues，一律用 `gh` CLI 操作。

仓库由 `git remote -v` 自动推断 —— 在本仓库任意 clone 或 worktree 内运行 `gh` 即可，无需 `-R`。

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."`，多行 body 用 heredoc。
- **Read an issue**: `gh issue view <number> --comments`，用 `jq` 过滤评论并一并取 labels。
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`，按需加 `--label` / `--state`。
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

## Pull requests as a triage surface

**PRs as a request surface: no.** _(改成 `yes` 则外部 PR 也进 triage 队列；`/triage` 读这个开关。)_

设为 `yes` 时，PR 走与 issue 相同的标签和状态，用 `gh pr` 对应命令：

- **Read a PR**: `gh pr view <number> --comments`，diff 用 `gh pr diff <number>`。
- **List external PRs for triage**: `gh pr list --state open --json number,title,body,labels,author,authorAssociation,comments`，只保留 `authorAssociation` 为 `CONTRIBUTOR`、`FIRST_TIME_CONTRIBUTOR`、`NONE` 的（丢弃 `OWNER`/`MEMBER`/`COLLABORATOR`）。
- **Comment / label / close**: `gh pr comment`、`gh pr edit --add-label`/`--remove-label`、`gh pr close`。

GitHub 的 issue 与 PR 共用一个编号空间，裸写 `#42` 两者皆可能——先 `gh pr view 42`，失败再退回 `gh issue view 42`。

## 当某个 skill 说 "publish to the issue tracker"

建一个 GitHub issue。

## 当某个 skill 说 "fetch the relevant ticket"

跑 `gh issue view <number> --comments`。

## 本仓库的特别约定

- **禁直推 main**：仓库装了 pre-push 钩子。一切改动走 feature 分支 + PR（`gh pr create` → `gh pr merge`）。
- **必用 worktree**：主工作区 `~/complex-management` 由多会话共用，在里面提交会被别的会话 `reset --hard` 拨走。
  一律 `git worktree add` 开独立工作区再干活；需要跑构建时软链 `node_modules`。
- **动手前先 fetch**：并发会话频繁推进 `origin/main`，开分支或审计前先核对 `HEAD == origin/main`。

## Wayfinding operations

供 `/wayfinder` 使用。**map** 是一个 issue，**child** issue 作为票。

- **Map**: 打 `wayfinder:map` 标签的单个 issue，body 里放 Notes / Decisions-so-far / Fog。`gh issue create --label wayfinder:map`。
- **Child ticket**: 作为 GitHub sub-issue 挂在 map 下（`gh api` 打 sub-issues 端点）。未启用 sub-issues 时，把 child 加进 map body 的任务列表，并在 child body 顶部写 `Part of #<map>`。标签：`wayfinder:<type>`（`research`/`prototype`/`grilling`/`task`）。认领后指派给推进人。
- **Blocking**: 用 GitHub **原生 issue dependencies**（UI 可见的权威表示）。加边：`gh api --method POST repos/jxh1979/mall/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`，其中 `<blocker-db-id>` 是阻塞方的数字 **database id**（`gh api repos/jxh1979/mall/issues/<n> --jq .id`，**不是** `#number` 也不是 `node_id`）。GitHub 返回 `issue_dependencies_summary.blocked_by`（只算未关闭的阻塞——这就是实时闸）。不可用时退回在 child body 顶部写 `Blocked by: #<n>, #<n>`，全部 blocker 关闭即解除。
- **Frontier query**: 列出 map 下未关闭的 children（`gh issue list --state open`，限定在 map 的 sub-issues / 任务列表内），剔除仍有未关闭 blocker 的（`issue_dependencies_summary.blocked_by > 0`，或 `Blocked by` 行里还有未关闭 issue）和已有 assignee 的，按 map 顺序取第一个。
- **Claim**: `gh issue edit <n> --add-assignee @me` —— 本次会话的第一次写操作。
- **Resolve**: `gh issue comment <n> --body "<answer>"` → `gh issue close <n>` → 把上下文指针（gist + 链接）追加进 map 的 Decisions-so-far。
