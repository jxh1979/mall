# complex-management 项目规则

本文件是**本仓库**的规则。四角色流水线的完整契约不在这里，见下方 §1。

## 1. 这个仓不是编排工作区

四角色闭环（Fable 规划 → Opus 实施 → Opus 技术审查 → Fable 最终验收）跑在 `~/ccwork`，不在本仓：

- 工作区宪法：`~/ccwork/CLAUDE.md`
- 流水线唯一真身：`~/ccwork/workflow/pipeline.md`（v3.0 FINAL）
- 机械边界：`~/ccwork/.claude/hooks/role-guard.mjs`，回归套件 `role-guard.tests.mjs`

**在本仓目录下直接开的 Claude Code 会话没有那套机械边界**——没有 role-guard hook、没有工作区写锁、没有 `policy.json` 白名单。要跑流水线：

```bash
cd ~/ccwork && claude --agent fable-orchestrator
```

本仓在流水线里的身份是**被审计与被修改的目标**，由实施子代理通过 worktree 操作，不是宿主。

## 2. worktree 铁律

`~/complex-management` 是**多个会话共用的同一个主工作区**（`git worktree list` 常年 40+ 个 `~/wt-*`）。

**任何要产出提交的活，一律先开 worktree：**

```bash
git worktree add -b <branch> /tmp/wt-<name> origin/main
ln -s ~/complex-management/node_modules /tmp/wt-<name>/node_modules   # npm run verify:* 要它
```

在主工作区里 `git checkout -b` + commit 会被并发会话踩掉：别的会话一句 `git reset --hard origin/main`，**连你正在用的分支 ref 一起拨走**，提交凭空消失。2026-07-28 实际发生过：提交 `5d4fcd72` push 成功，`gh pr create` 却报 `No commits between main and <branch>`，本地与远端分支都已被拨回。

push 后**必须回验远端真收到**，别只看 push 退出码：

```bash
git rev-list --left-right --count origin/main...origin/<branch>
```

收工 `git worktree remove --force`。

## 3. 权威源

三份 HTML 拥有最终解释权，**未经用户逐句批准禁止修改**：

- `docs/business-system-map-for-operations-v0.1.html`
- `docs/platform-governance-map-for-operations-v0.1.html`
- `docs/business-enum-quick-reference-v0.1.html`

事实优先级：最新 HTML > HTML git 提交史 + 决策台账 > PRD > 其他文档。

**绝对禁止因为代码这样写就把 PRD 改成代码的样子。** 代码证明实施现状，不能授权钱路或权责；HTML 规定业务规则，不能证明代码已实现。

机读真源在 `docs/prd-audit/`：`entry-registry.csv`、`authority-rule-ledger.csv`、`html-prd-coverage.csv`、`business-decision-ledger.md`（只追加，检索某编号必须读全文所有出现，只读原行会读反）。

## 4. 提交纪律

- **禁直推 main**（有 pre-push 钩子）。一律 feature 分支 → PR → `gh pr merge`
- `npm run verify:predeploy` 红即中止，**CI 红不强合**
- 两道 HTML 门禁（`html-ledger-sync` / `html-param-tier`）比对 `origin/main...HEAD`，**未提交时空转假绿**——commit 之后 push 之前必须本地真跑一次留全文输出
- 合并后按**内容哈希**回验，不比 SHA（squash 会改 commit SHA）
- 管道吃退出码：`cmd | tail -N; echo $?` 取的是 `tail` 的码，永远 0。用 `${PIPESTATUS[0]}`

## 5. 安全红线

除非用户针对本次任务明确授权，不得：`git commit`、`git push`、创建 PR、发布或部署、修改生产环境、破坏性数据库操作、删除用户数据、输出密码 / Token / API Key / Cookie / 私钥、`git reset --hard`、`git clean -fd(x)`。

## 6. 证据

所有进度和完成声明必须能指向本次会话里的实际工具结果。测试失败就报失败并附输出；步骤跳过就说跳过；没验证的事实明确标为未验证。

三条红线：不把未执行的命令写成已执行，不把未运行的测试写成已通过，不把推测写成已确认事实。

## 7. 环境

- 部署：systemd 用户级 `systemctl --user complex-api` 接管 8080，别裸跑 `deploy-api.sh`
- 测试库 `complex_test` 与生产库 `complex` 切开；只 export `COMPLEX_DATABASE_{URL,USERNAME,PASSWORD}` 三个变量，别 source 整个 env
- 部署跑 `MAVEN_ARGS=-DskipTests`（全量 mvn 会撞 Spring `failure.threshold=1` 级联毒化）
- 本库数据最后全删——**不为存量数据做兼容 / 回填 / 退化设计**
- 展示文字一律中文，仅机读标识留原文；日期口径 Asia/Shanghai
- 删除 = 软删除（存库 + 前台不显 + 可恢复 + 填理由 + 最高权限批准）

## 8. 已裁决的不再问

向用户确认两可项之前，先查：项目记忆、本轮 `request.md`、`docs/prd-audit/business-decision-ledger.md`。命中已有有效裁决的直接引用落笔，不重复提问。
