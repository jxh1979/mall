# 部署与变更流程 V0.1(2026-07-05 起)

## 为什么改
此前:AI 直推 main、部署只跑后端测试、治理门禁不 gate 部署 → 曾带着红门禁(scope 登记/安全基线/后台矩阵漂移)上生产。补三道真门禁。

## 三道门禁
1. **部署门禁(硬阻断,.160)**:`complex-deploy-api.sh`/`complex-deploy-web.sh` 在构建前跑 `npm run verify:predeploy`(delivery-gates 快子集,静态无需 DB);红即 `exit 1` 不碰生产。
2. **本地 pre-push 钩子(.160)**:禁止 `git push ...:main`;正道走 feature 分支 + PR。紧急覆盖 `ALLOW_MAIN_PUSH=1`(须在 PR/说明记录原因)。
3. **GitHub Actions(独立、隔离,当前未激活)**:模板在 `ops/ci/predeploy-gates.yml`——**放在 ops/ci/ 下 GitHub 不会执行它**,须手动复制到 `.github/workflows/`(需 workflow scope,见文末)才会在 PR/push 到 main 时跑 `verify:predeploy`。即便激活,私有库免费版也无法设 required check,只是可见信号 + 独立复现;硬阻断由第 1、2 道承担。

## 变更流程
1. feature 分支上开发 + 提交。
2. `ops/160/propose-change.sh "<标题>"` → 本地 predeploy 绿 → 推分支 → 开 PR。
3. **独立复核**:把 PR diff 交由 **codex(不同模型)** 做对抗复核(本环境最接近"独立"的手段;非人工,须知悉此局限)。
4. GitHub Actions predeploy 绿 + 复核通过 → `gh pr merge --squash`。
5. 部署:`complex-deploy-api.sh`(自带 predeploy 硬闸)。

## 已知局限(诚实记录)
- GitHub 分支保护/required check 需 Pro 或公开库,当前用不了 → 结构性强制靠本地钩子(可 --no-verify 绕过)+ 部署硬闸。
- "独立复核"是不同模型(codex)而非人类;闭环里仍无人类工程师的独立判断,这是残留风险。
- **门禁与被部署代码同仓同信任域**:能直推/合并 main 的主体,同一提交里就能把 `package.json` 的 `verify:predeploy` 改成 `true`、或删掉部署脚本里的 gate——本方案挡不住“篡改门禁本身”。真正的服务端强制需:受保护 CI/受控部署系统执行部署,或对 `package.json`/`ops/160/complex-deploy-*.sh`/hook/CI 文件加 CODEOWNERS 人审(仍非服务端强制)。
- GitHub Actions 门禁模板未激活前**完全不运行**(不只是“不 required”),激活需 workflow scope(见下)。
- pre-push 钩子是**开发者提示非安全边界**:可 `--no-verify`/`ALLOW_MAIN_PUSH=1`/不装钩子/换 clone/走 Web·API/机器人 token 绕过。

## 激活 GitHub Actions 门禁(需一次手动)
当前 gh OAuth token 无 `workflow` scope,无法自动推 `.github/workflows/`。模板已放 `ops/ci/predeploy-gates.yml`。激活:
1. 有 `workflow` scope 的令牌:`gh auth refresh -s workflow` 后 `mkdir -p .github/workflows && cp ops/ci/predeploy-gates.yml .github/workflows/ && git add .github && git commit && 走 PR`;或
2. GitHub 网页 Actions → New workflow 粘贴 `ops/ci/predeploy-gates.yml` 内容。

## 直推 main / 无门禁部署 —— 已做到哪、还差你做什么(2026-07-06 更新)

**已在仓内做实的(我能做的)**:
1. 部署硬闸:`complex-deploy-api/web.sh` 构建前跑 `verify:predeploy`,红即 exit 1。
2. **部署脚本直接跑权威门禁数组**(codex 复核后从"解析 package.json"改为此):部署脚本内硬编码必需门禁列表并逐个 `npm run`,**不解析也不信任 package.json 的 verify:predeploy 串**。因此"把 verify:predeploy 抽空/设 `true`/惰性化(如 `\"true # 门禁名...\"`)"对部署无效——对抗自测证实:篡改后部署仍跑全部门禁。**仍非仓外强制**:能改本部署脚本(需 .160 shell)或 `tools/verify-*.mjs`(让门禁真跑但断言变 no-op)者仍可绕过。
3. pre-push 钩子禁直推 main;`.github/CODEOWNERS` 标记强制点文件。
4. GitHub Actions 门禁模板 `ops/ci/predeploy-gates.yml`(见下需手动激活)。

**只有你(账号/基础设施权限)能做的 —— 我给不了,做完才算真闭合**:
- **激活 GitHub Actions 门禁**(token 无 workflow scope,git push 与 Contents API 均被 404 拒):
  `gh auth refresh -s workflow -h github.com` 后
  `mkdir -p .github/workflows && cp ops/ci/predeploy-gates.yml .github/workflows/`,走 PR 合入;
  或 GitHub 网页 Actions → New workflow 粘贴该 yml。
- **启用受保护分支 + required status check**(这才是"仓外强制",能真正挡直推/未过门禁的合并):
  需 GitHub Pro 或把仓库设为 Public(私有免费版 `branches/*/protection` 返回 403)。开通后:
  `gh api -X PUT repos/jxh1979/mall/branches/main/protection -f required_status_checks[strict]=true -f required_status_checks[contexts][]=predeploy -f enforce_admins=true -f required_pull_request_reviews[required_approving_review_count]=1 -f restrictions=`(按需调参)。
- **独立 DB 账号/实例隔离**:当前 complex_test 与生产同实例同账号;真隔离需另建只连 complex_test、对生产 complex 无权限的账号(需 DB 管理权)。

**诚实结论**:仓内我把篡改门槛尽量抬高了,但"能改 .160 部署脚本 / 能合 main 的主体仍能绕过"这一层,只有上面三条(受保护 CI、独立权限、人类审批)能真正闭合——它们都在仓外、需你出面。

