# 部署与变更流程 V0.03（2026-07-05 起；M001 修订：CI 定义同步、事件基线与已验提交合并）

## 为什么改
此前:AI 直推 main、部署只跑后端测试、治理门禁不 gate 部署 → 曾带着红门禁(scope 登记/安全基线/后台矩阵漂移)上生产。补三道真门禁。

## 四道门禁
1. **部署门禁(硬阻断,.160)**:`complex-deploy-api.sh`/`complex-deploy-web.sh` 在构建前跑 `npm run verify:predeploy`(delivery-gates 快子集,静态无需 DB);红即 `exit 1` 不碰生产。
2. **本地 pre-push 钩子(.160)**:禁止 `git push ...:main`;正道走 feature 分支 + PR。紧急覆盖 `ALLOW_MAIN_PUSH=1`(须在 PR/说明记录原因)。
3. **GitHub Actions(独立、隔离,✅已激活 2026-07-06)**:生效定义是 `.github/workflows/predeploy-gates.yml`,`ops/ci/predeploy-gates.yml` 是它的字节一致镜像(`verify:delivery-gates-docs` 机检两者一致且 checkout 带 `fetch-depth: 0`;浅克隆会让 html-ledger-sync/html-param-tier 取不到基线)。现于 PR/push 到 main 时在 GitHub ubuntu runner 上跑 `verify:predeploy`。即便激活,私有库免费版也无法设 required check,只是可见信号 + 独立复现;硬阻断由第 1、2 道承担。
4. **合并前置检查(`ops/160/merge-pr.sh`,2026-08-07 门禁升级批 1 补;2026-09-03 M001 修订)**:合并前取本分支 head commit 对应的 predeploy-gates run,`gh run watch --exit-status` 等到绿后,再次核对远端分支与 PR head 仍是该 commit,并以 `gh pr merge --match-head-commit <已验 SHA>` 合并;等待期间分支被推新 commit 即中止。非成功即中止并打印 run URL。补的正是第 3 道"只是可见信号"的缺口——把"人记得去看一眼"变成机器动作。逃生口 `SKIP_CI_WAIT=1`(打印醒目警告,**须在 PR 说明原因**)。

## 变更流程
1. feature 分支上开发 + 提交。
2. `ops/160/propose-change.sh "<标题>"` → 本地 predeploy 绿 → 推分支 → 开 PR。
3. **独立复核**:把 PR diff 交由 **codex(不同模型)** 做对抗复核(本环境最接近"独立"的手段;非人工,须知悉此局限)。
4. 复核通过 → `ops/160/merge-pr.sh --squash`(自动等 GitHub Actions predeploy 绿再合;CI 故障时 `SKIP_CI_WAIT=1` 强合,须在 PR 说明原因)。
5. 部署:`complex-deploy-api.sh`(自带 predeploy 硬闸)。

## 已知局限(诚实记录)
- GitHub 分支保护/required check 需 Pro 或公开库,当前用不了 → 结构性强制靠本地钩子(可 --no-verify 绕过)+ 部署硬闸。
- "独立复核"是不同模型(codex)而非人类;闭环里仍无人类工程师的独立判断,这是残留风险。
- **门禁与被部署代码同仓同信任域**:能直推/合并 main 的主体,同一提交里就能把 `package.json` 的 `verify:predeploy` 改成 `true`、或删掉部署脚本里的 gate——本方案挡不住“篡改门禁本身”。真正的服务端强制需:受保护 CI/受控部署系统执行部署,或对 `package.json`/`ops/160/complex-deploy-*.sh`/hook/CI 文件加 CODEOWNERS 人审(仍非服务端强制)。
- GitHub Actions 已激活；修改生效 workflow 时同步镜像，维护方式见下。
- pre-push 钩子是**开发者提示非安全边界**:可 `--no-verify`/`ALLOW_MAIN_PUSH=1`/不装钩子/换 clone/走 Web·API/机器人 token 绕过。

## GitHub Actions 门禁定义的维护(2026-07-06 已激活;2026-09-03 修订)
生效定义只有一份:`.github/workflows/predeploy-gates.yml`。`ops/ci/predeploy-gates.yml` 必须与它字节一致,由 `verify:delivery-gates-docs`(在 `verify:predeploy` 链内)机检;改 workflow 时两份同改。历史激活步骤(用 `ops/ci` 模板复制到 `.github/workflows/`)已完成,不再重复执行——若需重新激活,复制方向同样必须保证复制后两份一致且含 `fetch-depth: 0`。

CI 用 `HTML_GATES_BASE` 给两道 HTML 差异门禁传入事件基线：PR 使用目标分支的 base SHA，push 使用事件的 `before` SHA，检查整批提交。手动运行时可用第一个参数指定基线；没有参数或环境变量时沿用 `origin/main`。基线不存在必须失败。

参数门禁只豁免 `param-tier:ignore` 点名的数值；空标记和单独的“不可配”不能代替层级标注。条目引用门禁遇到 CSV 列数错误或损坏的引号记录时报告文件和记录位置并失败。可在可丢弃副本中运行 `node tools/gate-hardening-tests/m001-regression.mjs` 验证这些规则及合并期间提交变化的处理。

## 直推 main / 无门禁部署 —— 已做到哪、还差你做什么(2026-07-06 更新)

**已在仓内做实的(我能做的)**:
1. 部署硬闸:`complex-deploy-api/web.sh` 构建前跑 `verify:predeploy`,红即 exit 1。
2. **部署脚本直接跑权威门禁数组**(codex 复核后从"解析 package.json"改为此):部署脚本内硬编码必需门禁列表并逐个 `npm run`,**不解析也不信任 package.json 的 verify:predeploy 串**。因此"把 verify:predeploy 抽空/设 `true`/惰性化(如 `\"true # 门禁名...\"`)"对部署无效——对抗自测证实:篡改后部署仍跑全部门禁。**仍非仓外强制**:能改本部署脚本(需 .160 shell)或 `tools/verify-*.mjs`(让门禁真跑但断言变 no-op)者仍可绕过。
3. pre-push 钩子禁直推 main;`.github/CODEOWNERS` 标记强制点文件。
4. GitHub Actions 门禁(`.github/workflows/predeploy-gates.yml`,✅已激活):PR 到 main 时 GitHub 侧独立跑 verify:predeploy,首跑已 SUCCESS。因私有免费版无法设 required check,当前为可见信号(非硬阻断合并)。

**只有你(账号/基础设施权限)能做的 —— 我给不了,做完才算真闭合**:
- ~~激活 GitHub Actions 门禁~~(2026-07-06 已完成;定义维护见上节,不再用 `ops/ci` 模板覆盖生效 workflow)。
- **启用受保护分支 + required status check**(这才是"仓外强制",能真正挡直推/未过门禁的合并):
  需 GitHub Pro 或把仓库设为 Public(私有免费版 `branches/*/protection` 返回 403)。开通后:
  `gh api -X PUT repos/jxh1979/mall/branches/main/protection -f required_status_checks[strict]=true -f required_status_checks[contexts][]=predeploy -f enforce_admins=true -f required_pull_request_reviews[required_approving_review_count]=1 -f restrictions=`(按需调参)。
- **独立 DB 账号/实例隔离**:当前 complex_test 与生产同实例同账号;真隔离需另建只连 complex_test、对生产 complex 无权限的账号(需 DB 管理权)。

**诚实结论**:仓内我把篡改门槛尽量抬高了,但"能改 .160 部署脚本 / 能合 main 的主体仍能绕过"这一层,只有上面三条(受保护 CI、独立权限、人类审批)能真正闭合——它们都在仓外、需你出面。
