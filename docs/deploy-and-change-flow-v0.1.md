# 部署与变更流程 V0.1(2026-07-05 起)

## 为什么改
此前:AI 直推 main、部署只跑后端测试、治理门禁不 gate 部署 → 曾带着红门禁(scope 登记/安全基线/后台矩阵漂移)上生产。补三道真门禁。

## 三道门禁
1. **部署门禁(硬阻断,.160)**:`complex-deploy-api.sh`/`complex-deploy-web.sh` 在构建前跑 `npm run verify:predeploy`(delivery-gates 快子集,静态无需 DB);红即 `exit 1` 不碰生产。
2. **本地 pre-push 钩子(.160)**:禁止 `git push ...:main`;正道走 feature 分支 + PR。紧急覆盖 `ALLOW_MAIN_PUSH=1`(须在 PR/说明记录原因)。
3. **GitHub Actions(独立、隔离)**:`.github/workflows/predeploy-gates.yml` 在 PR/push 到 main 时跑 `verify:predeploy`。因私有库免费版无法设 required status check,此为可见信号 + 独立复现;硬阻断由第 1、2 道承担。

## 变更流程
1. feature 分支上开发 + 提交。
2. `ops/160/propose-change.sh "<标题>"` → 本地 predeploy 绿 → 推分支 → 开 PR。
3. **独立复核**:把 PR diff 交由 **codex(不同模型)** 做对抗复核(本环境最接近"独立"的手段;非人工,须知悉此局限)。
4. GitHub Actions predeploy 绿 + 复核通过 → `gh pr merge --squash`。
5. 部署:`complex-deploy-api.sh`(自带 predeploy 硬闸)。

## 已知局限(诚实记录)
- GitHub 分支保护/required check 需 Pro 或公开库,当前用不了 → 结构性强制靠本地钩子(可 --no-verify 绕过)+ 部署硬闸。
- "独立复核"是不同模型(codex)而非人类;闭环里仍无人类工程师的独立判断,这是残留风险。

## 激活 GitHub Actions 门禁(需一次手动)
当前 gh OAuth token 无 `workflow` scope,无法自动推 `.github/workflows/`。模板已放 `ops/ci/predeploy-gates.yml`。激活:
1. 有 `workflow` scope 的令牌:`gh auth refresh -s workflow` 后 `mkdir -p .github/workflows && cp ops/ci/predeploy-gates.yml .github/workflows/ && git add .github && git commit && 走 PR`;或
2. GitHub 网页 Actions → New workflow 粘贴 `ops/ci/predeploy-gates.yml` 内容。
