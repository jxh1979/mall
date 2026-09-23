CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M038 B5-r1 席乙（Grok）修复后整模块全量审计 raw

- 席位：Grok 独立只读审计席乙（第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（B3 累计 1 后的整模块重审；不只看 diff；不复用 B2/B4 票作结论）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：`plan/modules.md` V0.04 M038「功能权限点、职级三十档、权限模板与最终权限计算／网页与验证／第1段」；标记 NORMAL；计划有效内容 2787 行（origin/main `git ls-files` 口径，已过期）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`ebb1eb99c610df330c07d24278798c2d3611299a`
- after tree（B3-r1 冻结）：`9df1fad44caa2ef615993fe970d3efc4620aca25`（本模块 16 个工作区 blob 与该 tree 逐字节一致）
- before tree：`df21dfdfa3aaa27416857eeca14dedcd04cee631`
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 其它报告）。sidecar 与 ledger 顶部同值，仅作交叉观察
- LOCK-AMEND-COUNT：1（ledger 顶部；本席未改锁）
- 映射 106 项：见文末映射行；定义区全文已读
- findings 输入：V3.19；M038-001…010 修复队列「否（已修，B3-r1）」；当前队列「是」= 0。不把全部否当成页面已全对
- 独立性：未打开 `modules/M038-b5-r1-self-raw.md` 正文。全仓检索若扫到该路径片段一律丢弃不采信。旧 B1（含 B2-r1 追加段）与 B4-r1 可读，只作背景，结论以本席全文重审 + 当场实跑为准。未改计划 / findings / ledger / 业务代码 / 三 HTML / 锁；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；未对数据库执行写语句；除 D-028 允许的一次性 worktree 外未建脚本、日志或临时审计产物。psql 只 SELECT；口令只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD=` 行读取，未 source、未回显
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 本席步骤结论：**FAIL**（001…010 的页面修复与对应回归测试成立，无把被测逻辑整页 mock 掉；仍有一条须入队的工程缺陷 GR-M038-012：权限模板页「复制模板」及「应用模板」写按钮无提交中态，可连发两笔。B2 只登记项维持；添加 0；无 HTML 修改申请）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。B5 禁止修复，故 TDD / ponytail 修复强度 / receiving-code-review 的 IMPLEMENT 一律不执行。runbook「阶段 B 不创建 worktree」被本轮用户指令覆盖：前端单测只在基于 origin/main 的一次性 worktree 里进行。

本席环境技能根：`/home/james/.claude/skills/`、`/home/james/.claude/plugins/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/plugins/cache/`。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读。本模块强制「16 文件全文、不抽样」，直接全文实读；未再跑 skeleton/ask |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读。vitest / tsc / sha256 / psql / worktree add-remove 均前台完成，未挂 monitor |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`、同目录 `grill-me/` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 已读。权限闸是深模块（小接口 hasPermissions + DENY/wildcard/fail-safe）；页面是浅适配。缺陷落在「声称防双击的写按钮是否覆盖本模块全部写入口」 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B5 禁止修复、不加诊断桩、不对库执行写 |
| tdd / test-driven-development | 已读 | 用来判断新补断言能否因目标行为变坏而红；本席不写测试 |
| verification-before-completion | 已读 | PASS/FAIL 前当场：独立 sha256、after tree blob、origin/main worktree 57 测 + tsc、psql 闭集、源码写按钮 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方 raw 可收（未读本次 self-raw） |
| requesting-code-review | 存在，B4 用 | B5 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选存在；只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径扫可删项，见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | `.../i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；本席正文只落 raw；CLI 不回带结论或机器标记（适配器优先于该技能的「正文先写下一步」） |
| audit-context-building | 已读 | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| batch-grill-me | `.../productivity/batch-grill-me` 目录存在 | 已并入 grilling 自问自答，不冒充单独跑了本体 |
| ubiquitous-language | 本机 skill 目录无独立 SKILL | **缺口** |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是本轮用户指令允许的只读核，不是 using-git-worktrees 技能本体；核完已 remove，未用 `--force` |

graft 本轮检索调用：0（全文实读）。

一次性 worktree：`git worktree add --detach /home/james/wt-m038-b5r1-grok-ro origin/main`（HEAD `ebb1eb99`），拷入修后 8 个文件（与 after tree 逐字节相同），软链主工作区 `node_modules` 与 `apps/web/node_modules`，在 `apps/web` 下跑本模块 11 个测试文件 + `npx tsc -b --noEmit`。跑完先删两条软链，再 `git checkout --` 还原 8 文件，再 `git worktree remove`（无 `--force`）。目录已不存在。未装依赖。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`wc -l -c` + `sha256sum` + `git hash-object` + `git ls-tree 9df1fad44caa2ef615993fe970d3efc4620aca25 -- <paths>`。工作区 16 文件 SHA256 与 after tree blob 全 MATCH。

| 路径 | 物理行 | 字节 | SHA256 | 工作区 git blob = after tree |
|---|---:|---:|---|---|
| `apps/web/src/admin/pages/AccessGovernancePage.tsx` | 777 | 57255 | `d7668403ba673b8375a541f24220397d19f2cb92d345392cf63f4100dc94b101` | `1e3b6545504bd930be869cd8e1b582c7aba990e5` |
| `apps/web/src/admin/pages/AccessGovernancePage.degraded.test.tsx` | 61 | 3322 | `b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5` | `462053f04c93502d0acef5858d619a7c688268c4` |
| `apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx` | 191 | 11207 | `521dc934d768003006e329b7728985d795e3006437d905c7bb201af04da48871` | `84566763a689dcd90183faddf25610d28572f8ff` |
| `apps/web/src/admin/pages/AccessGovernancePage.scopechange.test.tsx` | 82 | 5060 | `84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6` | `1f5d835d32a0c81deb1e4612c160a2ca5bb0037b` |
| `apps/web/src/admin/pages/AccessGovernancePage.test.tsx` | 328 | 19760 | `bcfe27b0f09a09c0b19f6a0f92472eac841cd64559d17172196b008e9005b327` | `9ac750066266992eb2be8bc11ec2d1bdfa855753` |
| `apps/web/src/admin/pages/AccessUsersPage.tsx` | 374 | 23060 | `b4ae66d2735e0c20b28080ea36c3ff2a472c5dcb9cd540696f0f10215f9f230d` | `6555b9495c0e5e00a9111fdffc6a3b59bf849ec4` |
| `apps/web/src/admin/pages/AccessUsersPage.metricdetail.test.tsx` | 41 | 2121 | `ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca` | `0608195475b48a2e4b61412f0a99cf503c4e64b8` |
| `apps/web/src/admin/pages/AccessUsersPage.partnerbind.test.tsx` | 113 | 5703 | `33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d` | `672a17dec65e6ab27e9aeea427f2902cac513e46` |
| `apps/web/src/admin/pages/AccessUsersPage.robustness.test.tsx` | 45 | 2503 | `fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8` | `c68d37635c6a3a72f404f1dce31939c9798b07cb` |
| `apps/web/src/admin/pages/AccessUsersPage.test.tsx` | 136 | 6905 | `d5c22dfcc87a87e06a01fd28bc6c428984f716cd4e9ef675e24c4dbb39cf42b9` | `ac386bfc6fd162622a53c2b767fdc20482db04d7` |
| `apps/web/src/admin/pages/PermissionTemplatesPage.tsx` | 552 | 27684 | `4b31fa7e991e6c540e10930767bc29f4f31d6ec4f02a661bb42a49cade64a67d` | `47478fa38f43b75ba6a890e7b127c25f5ec7d07f` |
| `apps/web/src/admin/pages/PermissionTemplatesPage.guidedcreate.test.tsx` | 116 | 6000 | `e026609a0545a09f6013b258c8cc60a7f1bbd4fb53d340636f0917da4fa9b6d6` | `878c883862ff65a7a32a342acd0cb203a1c5ccad` |
| `apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx` | 153 | 8294 | `88e1f50b434b093fd4e494c6529c6528a7d83c6d7a4a452009cf1c783af894ec` | `266a7f5724c32e30d9f73c8058c78168e6766a40` |
| `apps/web/src/admin/security/Can.tsx` | 84 | 2545 | `bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193` | `9b290087a53308659430f28ad6ca8085fe82b171` |
| `apps/web/src/admin/security/PermissionGate.tsx` | 44 | 1502 | `a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5` | `7f39e56f21fbfa6e34fff19bc609242517042f46` |
| `apps/web/src/admin/security/PermissionGate.test.ts` | 113 | 6276 | `14f28de8ffc8544fd26f9a5843e156ecb7dd315591375fb51af827185d4ccbfc` | `4b865fbfdfd60e5039eb208ef470cd914cb21e29` |
| **合计** | **3210** | **189197** | — | — |

覆盖：**16/16 全文逐行，不抽样**。`git ls-files -z -- <16> | xargs -0 cat | wc -l` 在当前工作区 = **3210**（= 计划 2787 + B3-r1 净 +423）。文件头版本：治理页 V0.04、账号页 V0.03；模板页 / Can / PermissionGate 无「文件版本」行（B2 只登记 OBS-6，不改判）。

B3-r1 改过的 8 个文件：`AccessGovernancePage.tsx` / `.test.tsx` / `.guidedcreate.test.tsx`、`AccessUsersPage.tsx` / `.test.tsx`、`PermissionTemplatesPage.tsx` / `.test.tsx`、`PermissionGate.test.ts`。其余 8 个与 origin/main 相同。

相关三 HTML（只读，铁律 4 未改）：

- `docs/platform-governance-map-for-operations-v0.1.html` L287 全局原则「界面全中文」：展示给人看的一切文字必须中文；四样例外：单据号、设备编号、二维码内容、系统内部标识符。同页 L965 已拍板复述。L643–670 PGM-040/041/042 权限点、角色模板、员工级自定义、最终权限公式、临时授权、职级 M1–M10 × 上中下。
- `docs/business-enum-quick-reference-v0.1.html` L232「职级：M1–M10，每级分上 / 中 / 下（共 30 档）」；L234 最终权限计算；L242 权限状态五态。
- `docs/business-system-map-for-operations-v0.1.html` L566–571 存档／取消存档／申请删除；L581–582 分步引导、哪一步填得不对当步就提示；L587 平台管理员日常功能 vs 最高权限。

`formatBusinessStatus`（`apps/web/src/customer/i18n/statusLabels.ts` L255–259，非本模块 FILES）：未知码 **原样返回**。本模块页内小映射必须自己盖住后端闭集，不能指望公共字典。

---

## 2. 取证块（类型 2 / 4）

### 2.1 origin/main worktree 实跑

worktree：detached `ebb1eb99`。拷入主工作区修后 8 文件后 SHA 与主工作区 / after tree 一致。软链 `node_modules`。

```text
cd /home/james/wt-m038-b5r1-grok-ro/apps/web
npx vitest run \
  src/admin/pages/AccessGovernancePage.degraded.test.tsx \
  src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx \
  src/admin/pages/AccessGovernancePage.scopechange.test.tsx \
  src/admin/pages/AccessGovernancePage.test.tsx \
  src/admin/pages/AccessUsersPage.metricdetail.test.tsx \
  src/admin/pages/AccessUsersPage.partnerbind.test.tsx \
  src/admin/pages/AccessUsersPage.robustness.test.tsx \
  src/admin/pages/AccessUsersPage.test.tsx \
  src/admin/pages/PermissionTemplatesPage.guidedcreate.test.tsx \
  src/admin/pages/PermissionTemplatesPage.test.tsx \
  src/admin/security/PermissionGate.test.ts

Test Files  11 passed (11)
     Tests  57 passed (57)
Duration  2.57s
VITEST_EXIT:0

npx tsc -b --noEmit
TSC_EXIT:0
```

11 文件 / 57 测全绿，与派发预期一致。未跑全量前端单测（已知 origin/main 另有 TraceLinkDrilldown 1 条 + TraceLinkP2c 2 条失败，属别的模块，登记观察 XM-M038-A，不计入本模块）。未跑后端测试。

清理：删软链 → `git checkout --` 还原 8 文件 → `git worktree remove` 无 `--force` → 目录不存在。

### 2.2 complex_test 只读 SELECT

```text
staff_levels.id ORDER BY 1 → 30 行：
lvl_m1_lower, lvl_m1_middle, lvl_m1_upper, …, lvl_m10_lower, lvl_m10_middle, lvl_m10_upper
与页内 STAFF_LEVEL_IDS 生成式（main 1..10 × lower/middle/upper）逐键相同。

permission_templates.template_type → role × 5
access_grant_requests 空表；CHECK：
  status ∈ {pending, applied, rejected}
  request_type ∈ {user_role, user_position, temporary_permission, role_permission, user_scope_change, position_high_risk_release}
access_users.created_via → legacy × 2
AccessService.java:1222 写入 "manual_admin"；AccessUser 构造默认 "legacy"。闭集两值。
position_role_templates.staff_level_id → lvl_m1_lower
position_role_templates.position_code → auditor_external / auditor_internal / outsourced_construction / outsourced_facility / outsourced_security_cleaning
  （五码均在 POSITION_LABELS）
```

### 2.3 AccessController 门控码（origin/main 与当前工作区同文件，只读对照）

页面 PermissionButton / hasPermissions 所用码与控制器 `requirePermission` 对齐：

| 页面动作 | 前端码 | 控制器 |
|---|---|---|
| 岗位模板 CRUD / 定岗 / 范围 / 装配 / 覆盖 | `access.role.assign` | 同 |
| 批准/驳回删除 | `access.role.assign` + `governance.supervisor_review.approve` | lifecycle 内同 |
| 授权申请发起 | `access.grant_request.apply` | POST `/grant-requests` |
| 授权列表/政策读取/总结 | `access.grant_request.view` | GET 同 |
| 授权审批 | `access.grant_request.review` + `governance.supervisor_review.approve` | POST `.../review` |
| 保存授权政策 | `admission.policy.manage` | POST `/grant-policy`（B1 OBS-1，只登记） |
| 模板查看/管理/应用 | `access.permission_template.view/manage/apply` | 同 |
| 权限点清单 / 用户清单 | `access.permission.view` / `access.user.view` | 同 |
| 新建外包账号 | `access.user.manage` | POST `/users` |
| 绑/解外部机构 | `partner.org.manage` | `PartnerController` 同码 |

未发现本模块写按钮码与门控码不一致。

---

## 3. 第一性原理三问

### 3.1 这段东西为什么必须存在？删掉它，三份 HTML 里哪一条会做不到？

- **PermissionGate / Can / PermissionButton**：删掉则后台无法按单一功能点门控页面与写按钮。上位：BEQR-151「显式拒绝优先」、PGM-041「权限按单一功能最小颗粒度」、BSM-114「不同人只看自己该看的」、BSM-007 平台管理员日常功能 vs 高危不自动给。
- **AccessGovernancePage**：删掉则岗位↔模板、定岗自动适配、范围变更先审批、直接装配、员工级授予/拒绝、高危授权申请与审批、智能总结都没有后台入口。上位：PGM-041 角色装配/临时权限/高危审批原句、BSM-004 存档与取消存档、BSM-005 分步引导。
- **AccessUsersPage**：删掉则员工注册申请 / 外包账号开通、开通方式展示、外部机构绑定没有后台入口。上位：PGM-033 员工和手机主要功能、PGM-043 外包人员可有系统账号。
- **PermissionTemplatesPage**：删掉则角色级权限模板、临时/单次有效、复制不影响原模板、按左侧菜单勾选都没有后台入口。上位：PGM-041「支持角色级权限模板」「临时授权可设有效期或单次使用」、BSM-007 末句按左侧菜单勾选。

答得上 → 有业务上位。不是无需求承载。

### 3.2 它声称的行为，有没有被实际执行验证过？证据在哪？

- 声称「11 文件 57 测全绿、tsc 通过」：worktree 当场跑出 57 passed / TSC_EXIT:0（类型 2）。
- 声称「30 档下拉与库内编号逐键相同」：页内生成 30 个 `lvl_m{n}_{band}`；psql 30 行同序同键；测试断言 options[0]=lvl_m1_lower「M1·下」、options[29]=lvl_m10_upper「M10·上」（类型 2+4）。
- 声称「后端闭集取值中文上屏」：CHECK 三态六类型均在 GRANT_*_LABELS；库内 template_type=role 显示「角色模板」；created_via 两值有页内映射。测试打桩 applied / role_permission / user_scope_change / position_high_risk_release / legacy / manual_admin 并断言中文、否定英文码（类型 2）。
- 声称「写失败用户看得到、防双击」：治理页七个写动作走 `runWrite`（writeBusyRef 同步闸 + 按钮 disabled）；测试「装配角色 403」会 `setGlobalError`、「提交未完成再点不发第二笔」writes.length===1（类型 2）。**未覆盖**：权限模板页复制/应用（见 GR-M038-012）。
- 声称「读出再保存保值」：`toDateTimeLocal` 按本地时区取年月日时分；测试 `TZ=Asia/Shanghai` 且先断言 `getHours()===8`，再断言 PUT 的 valid_from/until 与 `2026-06-14T00:00:00Z` / `2026-06-15T00:00:00Z` 同一时刻（类型 2）。
- 测试本身也在审计范围内：新补断言绑的是页面真实组件与真实 `hasPermissions`/`Can`/`PermissionGate`，没有把被测映射函数整体 mock 掉。页面测试仍用 `wildcard: true` 走主路径（010 已在 PermissionGate.test.ts 补无权限分支，B2 定稿如此，不改判）。

### 3.3 它依赖的前提，哪一条是没验证过就当真的？

已当场验证：after tree blob、57 测、tsc、staff_levels 30 键、grant CHECK 闭集、created_via 两值、Controller 门控码、三 HTML 原句。

仍未单独执行、但有源码的前提：

- `RowActions` 无 `disabled` 槽，审批双击只靠 `writeBusyRef`（B4 已记录；本席源码确认 L615–618 仍走 `runWrite`，与 005 测试的装配角色闸同一函数）。
- 公共字典未知码原样返回：本席读到 `STATUS_LABELS[code] ?? code`。页内映射未覆盖的新码仍会英文上屏——这是映射完备性的边界，不是「字典会自己补」。
- 权限模板「复制/应用」没有提交中态：此前当「与 005 同属治理页七动作」或「B1 已观察应用按钮只禁 selected.id」处理；本席按 B5 重点「有无仍可双击的写按钮」在整模块重审，认定为未修净的工程缺陷（GR-M038-012），不是未验证就放行。

---

## 4. 顺从式：106 项逐项（适用 / 无本面）

判定口径：本模块 16 个前端文件是否必须兑现该锚点原文。无本面 = 锚点业务不在这三页/两闸的职责内，不因此删代码，也不发明实现。不新增判据。

### 4.1 BEQR（27）

| ID | 结论 | 证据 |
|---|---|---|
| BEQR-063 | 无本面 | 原文是酒吧反结/沽清/免单/存酒作废。本模块高危授权走 PGM-041，不实现酒吧动作。 |
| BEQR-074 | 无本面 | 隐私桌台查看/操作/紧急接管。本模块无桌台 UI。 |
| BEQR-143 | 无本面 | 四类主体。账号页组织/商户是标识符手填（B2 只登记），不建模四类主体。 |
| BEQR-144 | 无本面 | 经营主体多对多。范围变更只提交组织/商户编号。 |
| BEQR-145 | 无本面 | 城市项目公司职责。 |
| BEQR-146 | 无本面 | 运营公司职责。 |
| BEQR-147 | 无本面 | 收入归属。 |
| BEQR-148 | 无本面 | 合作模式枚举。 |
| BEQR-149 | **适用** | 原文「M1–M10，每级分上/中/下（共 30 档）」。四处职级改为下拉，选项=库内 30 编号，显示 `zhLevel`。测试 008。 |
| BEQR-150 | 无本面 | 终端十一端。本模块是后台 Web 三页。 |
| BEQR-151 | **适用** | 原文「角色+基础模板+有效临时+单独授予−显式拒绝（拒绝优先）」。`hasPermissions`：denies 优先于 wildcard 与 grants。PermissionGate.test.ts 四条公式测试。页面有员工级授予/拒绝入口。 |
| BEQR-152 | **适用（本面是入口与文案）** | 隔离引擎不在本模块。治理页范围变更边界文案 L565 复述「旧派生立即失效、新范围先审批」。 |
| BEQR-153 | 无本面 | 终端开关优先级。 |
| BEQR-154 | 无本面 | 综合体项目状态。 |
| BEQR-155 | 无本面 | 商户/运营主体状态。 |
| BEQR-156 | 无本面 | 合同状态。 |
| BEQR-157 | 无本面 | 员工状态五态（待入职/在职…）。账号页用的是账号 status（有效/待激活/已禁用），不是人事五态。 |
| BEQR-158 | 无本面 | 设备状态。 |
| BEQR-159 | 无本面（只登记维持） | 速查表权限状态五态「草稿、已启用、暂停、已停用、归档」。模板页展示 formatBusinessStatus(active/inactive)=有效/未激活；后端无改这五态的接口。B1 OBS-5 / B2 只登记，无新证据不改判。 |
| BEQR-166 | 无本面 | 合同审批链。 |
| BEQR-182 | 无本面 | 考勤日结果。 |
| BEQR-183 | 无本面 | 打卡方式。 |
| BEQR-184 | 无本面 | 请假类型。 |
| BEQR-185 | 无本面 | 请假单状态。 |
| BEQR-186 | 无本面（薪资字段是 PGM-042 细化） | 薪资单构成。定岗表有「基础薪资(元)」「薪资备注」，对应「职级只绑定薪资大范围，具体薪资单独填写」，不实现薪资单。 |
| BEQR-187 | 无本面 | 员工提成发放。 |
| BEQR-189 | 无本面 | 管理费方式。 |

### 4.2 BSM（20）

| ID | 结论 | 证据 |
|---|---|---|
| BSM-001 | **适用（定位）** | 系统管什么：本三页是权限/职级/账号底座的后台入口之一。 |
| BSM-003 | **适用** | 通用能力：可改可删、有引导、权限分岗。岗位模板生命周期 + 三页引导 + 权限闸。 |
| BSM-004 | **适用** | 「想恢复点取消存档」。004 已把已存档按钮改为「取消存档」、已删除为「恢复」。删除仍走申请/最高权限批准。锚点点名商户/票种/商品为例，岗位模板是同类配置主数据细化。 |
| BSM-005 | **适用** | ＋新建 → 分步引导。三页均 GuidedFlowModal（默认 singlePage）。哪步不对当步提示：模板/授权申请/新建账号引导测试有校验拦截。 |
| BSM-006 | 无本面 | 动态二维码。 |
| BSM-007 | **适用** | 平台管理员日常功能 vs 高危不自动给。wildcard 放行日常；高危仍要审批入口。权限网格按 adminRoutes 菜单分组勾选（B1 OBS-12）。 |
| BSM-009 | 无本面 | 基础管理子系统入口卡。 |
| BSM-028 | 无本面 | 基础管理主要功能（盘子）。 |
| BSM-029 | 无本面 | 基础管理和谁有关。 |
| BSM-072 | 无本面 | 商户管理整章。 |
| BSM-073 | 无本面 | 商户主要功能。 |
| BSM-074 | 无本面 | 商户和谁有关。 |
| BSM-084 | 无本面 | 员工手机端。 |
| BSM-085 | 无本面 | 日常工作（手机）。 |
| BSM-086 | 无本面 | 排班考勤请假。 |
| BSM-087 | 无本面 | 我的薪资。 |
| BSM-088 | 无本面 | 提成/活动/人员服务边界。 |
| BSM-114 | **适用** | 各类入口只看该看的。PermissionGate 整页、PermissionButton 写动作。路由壳不在本模块 FILES（B1 OBS-10）。 |
| BSM-115 | 无本面 | 子系统为何连在一起的总述。 |
| BSM-120 | **适用（已确认规则）** | 权限分岗、引导、存档删除等已确认规则由本模块承接其后台面。 |

### 4.3 GEN（18）全部适用

| ID | 本模块落点 |
|---|---|
| GEN-001 | 三页+两闸均有 HTML 上位；无纯转发空壳。 |
| GEN-002 | 57 测 + tsc 当场绿；写路径与 Controller 对齐。GR-M038-012 使「复制/应用」主路径的并发声明不成立。 |
| GEN-003 | 新补测试能因中文/30 档/未登记岗位/取消存档/时区往返/无权限按钮而红。装配角色 403 断言是 `/\S/`（能因「完全无提示」而红，不能因「提示不是后端原句」而红）——记观察，不单独立项。 |
| GEN-004 | 职级闭集下拉；引导步内校验；范围变更必填理由。岗位码仍手填（B2 只登记）。 |
| GEN-005 | 写按钮带权限码；无权限 PermissionButton 禁用并拦截 onClick（010 测试）。服务端授权不在本模块实现，只对齐码。 |
| GEN-006 | 006 时间往返已修；复制无幂等键（见 012）。 |
| GEN-007 | 治理页 runWrite 防双击已测。**模板页复制/应用未闸 → 012。** |
| GEN-008 | 页内映射覆盖后端 CHECK 闭集；职级 id 与库一致；状态用词改走 formatBusinessStatus。 |
| GEN-009 | 治理页七动作失败进 setGlobalError；引导弹窗失败留窗。复制/应用有 catch，失败看得到；缺的是忙态。 |
| GEN-010 | 无注入/密钥。权限码当标识符展示（四样例外，B1 OBS-4）。 |
| GEN-011 | 审批空意见改为「通过/驳回」中文留痕（001 测试）。 |
| GEN-012 | 列表分页；无无界循环。 |
| GEN-013 | 本模块无构建脚本。tsc 在 web 包通过。 |
| GEN-014 | 无本模块迁移文件。 |
| GEN-015 | 中文标签、aria-label 去 id/ID（002）；智能总结表头列数=单元格（007）。 |
| GEN-016 | 004 按钮文案与 BSM-004「取消存档」及 restore→active 一致。账号页头仍写「企业微信同步结果」而页内无此 UI（B1 OBS-2，只登记）。 |
| GEN-017 | 本席新 finding 按冲突→细化→技术性→添加走完，012 归技术性。 |
| GEN-018 | 结论落到 HTML 原句、vitest 输出、psql、源码 grep。 |

### 4.4 PGM（41）

| ID | 结论 | 证据 |
|---|---|---|
| PGM-001 | **适用** | 治理层底座；同节 L287 零英文全局原则。 |
| PGM-002 | 无本面 | 一件业务能不能做的判断链（治理顺序总述）。 |
| PGM-003 | **适用（入口列举）** | 模块入口含「权限和职级」。 |
| PGM-004 | 无本面 | 综合体和集团入口。 |
| PGM-005 | 无本面 | 空间和点位入口。 |
| PGM-006 | 无本面 | 商户和主体入口。 |
| PGM-007 | 无本面 | 合同和准入入口。 |
| PGM-008 | **适用（弱）** | 员工和手机入口；账号页承接开通。 |
| PGM-009 | **适用** | 「权限和职级」入口正文由本三页实现。 |
| PGM-010 | 无本面 | 终端开关入口。 |
| PGM-011 | 无本面 | 设备管理入口。 |
| PGM-012 | 无本面 | 商户侧入口。 |
| PGM-013 | 无本面 | 开放接口入口。 |
| PGM-014 | **适用（弱）** | 留痕复核：审批意见/操作备注中文留痕；复核链 UI 不在本模块。 |
| PGM-015 | 无本面 | 楼宇运营入口。 |
| PGM-016 | 无本面 | 应急与隐私入口。 |
| PGM-017 | 无本面 | 综合体和集团专章。 |
| PGM-018 | 无本面 | 物理项目与运营公司多对多。 |
| PGM-019 | 无本面 | 各主体负责什么。 |
| PGM-020 | 无本面 | 综合体主要功能。 |
| PGM-021 | 无本面 | 业务意义。 |
| PGM-022 | 无本面 | 空间和点位专章。 |
| PGM-024 | 无本面 | 空间影响业务。 |
| PGM-026 | 无本面 | 商户主要功能。 |
| PGM-027 | 无本面 | 商户业务边界。 |
| PGM-031 | 无本面 | 外部经销商主体/合同价格。 |
| PGM-032 | **适用** | 人、私人手机、工作手机要能对应。账号页管账号开通与归属，不管企微绑定 UI（OBS-2）。 |
| PGM-033 | **适用** | 员工注册申请 vs 外包账号两条路径（staff / outsourced_vendor）。B1 OBS-5 其余账号类型不强制本页创建下拉全开，不改判。 |
| PGM-034 | 无本面 | 工作手机和企业微信。页头夸大，B1 OBS-2 只登记。 |
| PGM-035 | 无本面 | 提成发放和人员服务治理。 |
| PGM-036 | 无本面 | 设备管理专章。 |
| PGM-040 | **适用** | 「每一个单一功能都可以单独授权」。权限点清单勾选 + 员工级覆盖。 |
| PGM-041 | **适用** | 角色模板、员工级自定义、敏感动作审批、最终权限公式、临时有效期/单次、角色装配细则、范围变更先审批。页面有对应入口与文案。 |
| PGM-042 | **适用** | 职级 M1–M10 × 上中下；只绑薪资大范围。008 下拉。 |
| PGM-043 | **适用（账号开通面）** | 外包可有系统账号。账号页 outsourced_vendor + 绑机构。审计只看不改不在本页实现。 |
| PGM-044 | 无本面 | 桌台、挂单与经销业务权限表。 |
| PGM-048 | 无本面 | 商户侧全开放专章。 |
| PGM-059 | 无本面 | 个人信息保护专段。账号表不展示手机号（抽屉测试排除手机号夹具）。 |
| PGM-060 | 无本面 | 会员身份/营销/生物信息。 |
| PGM-065 | 无本面 | 治理层怎么连接所有业务的总图。 |
| PGM-066 | **适用** | L965 已拍板零英文。001–003 修的就是本条。权限码/单据号/岗位码作系统内部标识符（四样例外，B1 OBS-4 / 003 括号内岗位码，不改判）。 |

**106/106 已逐项记录。** 无「未看」。不新增判据。

---

## 5. 对抗式（grilling 自问自答）

Q1 后端闭集是否还有页内映射盖不住的会上屏取值？
A：grant 状态 3、类型 6、created_via 2、template_type 库内仅 role（下拉另有 base/temporary/copy/role 四项均有中文）。岗位码未映射走「未登记岗位（码）」。职级非正则走原样（下拉不能提交非 30 档；库内模板职级是 lvl_m1_lower）。未知码 formatBusinessStatus 原样返回——这是公共字典契约，本模块已对闭集加页内表。**无新闭集漏键。**

Q2 字段名/提示是否还有英文 id/ID？
A：三页用户可见文案已改「编号」或去掉。data-testid 仍英文（002 定稿明确不动）。权限码、岗位码输入值、单据号属四样例外。

Q3 每个写动作失败用户看到什么？
A：治理页 runWrite catch → setGlobalError(Error.message 或中文兜底)。引导弹窗 catch → 窗内 error + globalError。账号创建/绑定同样 catch。模板保存/复制/应用有 catch。**失败可见。** 缺的是复制/应用的忙态，不是吞错。

Q4 是否还有可双击连发的写按钮？
A：**有。** 治理页七动作 + 两处引导 submitting + 账号 create submitting + bindBusy 已闸。`PermissionTemplatesPage`「复制模板」disabled 仅为 `!selected?.id`，`copyTemplate` 不置 submitting；「应用模板」HighRiskConfirm 同样无忙态。同页「保存修改」却用了 `submitting`。这不是理论路径：复制无二次确认，连点即两笔 POST /copy。

Q5 读出再保存是否保值？
A：006 测试在 Asia/Shanghai 下 PUT 时刻与库内 UTC 原值相同。通过。

Q6 30 档是否与库逐键一致？
A：生成式与 psql 30 行一致。测试锁死首末项。范围变更多一个空选项「沿用原职级」。未调 staff-levels 接口（008 定稿）。

Q7 权限码与 AccessController 是否一致？
A：见 §2.3。一致。`admission.policy.manage` 保存政策是后端既有选型（B1 OBS-1）。

Q8 新补测试能否失败？有无把被测逻辑 mock 掉？
A：001/003/004/006/007/008/009/010 断言都绑具体中文或请求体，改回英文/自由文本/UTC slice 会红。PermissionGate 组件测试渲染真实 Can/PermissionButton/PermissionGate。页面测试 mock 的是 fetch/AdminApi，不是页面映射函数。metricdetail 测试用假 api 对象测 KPI，不测权限映射，范围清楚。wildcard:true 仍在页面测试——010 已在闸组件补无权限，B2 定稿，不改判。

Q9 文案与 HTML / 后端现行行为是否一致？
A：取消存档 vs restore→active：一致。范围变更先审批：有入口和中文结果句。最终权限 DENY 优先：闸实现与 BEQR-151 一致。零英文：闭集已盖；标识符例外维持。

Q10 B2 只登记能否无新证据改判？
A：不能。岗位手填、wildcard 后端恒 false、页头企微、权限状态五态、无文件版本行、org-1/user-1 默认、权限码当主标签、账号创建只有两条路径、授权申请无最终审批人、GrantPolicy 多余键、master-authority 文案硬编码、empty={false}——均无新证据，维持只登记。

---

## 6. 001…010 修复落地（不复用 B4 票，对照当前源码+测试）

| ID | 定稿要点 | 当前源码 | 测试能否红 | 本席 |
|---|---|---|---|---|
| 001 | 申请状态/类型、开通方式、模板类型 role、空意见中文 | GRANT_STATUS/TYPE_LABELS 全闭集；CREATED_VIA_LABELS；TEMPLATE_TYPE_LABELS 含 role；review 空意见「通过/驳回」 | 治理表三种状态+三内部类型中文；模板列「角色模板」；开通方式「历史账号/后台手动录入」 | 落地 |
| 002 | 字段名/aria/校验去 id/ID | 三页可见文案已改「编号」 | 无单独回归句，全文检索用户可见「用户 id」「组织 ID」已无 | 落地 |
| 003 | 未映射岗位「未登记岗位（码）」 | zhPosition L50–54 | 单元格精确等于「未登记岗位（front_desk）」 | 落地 |
| 004 | 已存档「取消存档」、已删除「恢复」 | L526 | 两行按钮文案断言，且不再含「停用模板」 | 落地 |
| 005 | runWrite + 七写动作；403 提示；防双击 | L113–129 及七处 onClick | 装配角色 403 会 setGlobalError；未完成再点 writes=1。**无审批 403 单测**（同一包装，记观察） | 治理页落地 |
| 006 | toDateTimeLocal 本地时区 | L42–48 | TZ=Asia/Shanghai 且 getHours===8 护栏 + 时刻相等 | 落地 |
| 007 | 删重复表头 | 总结表 thead 6 列 | th.length === 首行 td.length | 落地 |
| 008 | 30 档下拉，范围变更留空选项 | STAFF_LEVEL_IDS + staffLevelOptions；四处 select | 30/31 项、首末键与中文 | 落地 |
| 009 | 下拉/摘要用 formatBusinessStatus | 模板状态、账号初始状态 | 「有效/未激活」「有效/待激活/已禁用」 | 落地 |
| 010 | 闸组件渲染 + 授权申请引导提交 | PermissionGate.test.ts 组件段；guidedcreate 第二 describe | 无权限按钮禁用且 onClick 不触发；POST /grant-requests 请求体 | 落地 |

001…010 **作为那十条的最小处置，页面侧已落地，无回归到修前英文/自由文本/UTC 偏移/重复表头。** 不把「十条都否」推成模块 PASS。

---

## 7. 本席 findings

### GR-M038-012 — 权限模板「复制模板」「应用模板」仍可连发两笔写请求

- **文件:行**：
  - `apps/web/src/admin/pages/PermissionTemplatesPage.tsx` L283–309 `copyTemplate`（无 submitting / 无 busy ref）
  - 同文件 L407–415 按钮 `disabled={!selected?.id}`，`onClick={() => void copyTemplate()}`
  - 同文件 L324–341 `applyTemplate`（同样无忙态）
  - 同文件 L419–428 `HighRiskConfirm` `disabled={!selected?.id}`，`onConfirm={() => void applyTemplate()}`
  - 对照：同页「保存修改」L391–399 使用 `disabled={submitting || !selected?.id}`，`saveTemplate` 会 `setSubmitting(true)`
- **原句**：

```
disabled={!selected?.id}
onClick={() => void copyTemplate()}
…复制模板…
…
disabled={!selected?.id}
onConfirm={() => void applyTemplate()}
```

- **正常触发**：打开权限模板页 → 选中一行 → 快速连点两次「复制模板」。每次 POST `/api/v1/access/permission-templates/{id}/copy`，请求体无幂等键（只有 templateName/templateType/operatorNote）。后端会建出两份副本。应用模板在 `window.confirm` 点两次「确定」后同样两笔 POST `.../apply`。
- **影响**：配置主数据被重复复制/重复赋权；与 005 已修的治理页双击问题同类，只是发生在本模块另一页。
- **四分类（写死顺序）**：
  1. 冲突？否。三 HTML 未写「复制必须幂等」，也没有相反的「允许连点两次各建一份」的明文。
  2. 细化？否。不能指到某一 HTML 条目要求「复制按钮防双击」作为下位细节；BSM-005 管的是新建引导。
  3. **技术性**：是。无业务发明（没有新实体/枚举/流程）。纯 GEN-007 并发与幂等 / GEN-002 写路径。
  4. 添加？不进入。
- **已有判据**：GEN-007、GEN-002。不新增判据。
- **依据类型**：4（源码逐行 + 与同页 saveTemplate 对照）。类型 2：本席未改测试、未在 worktree 另写用例；005 的防双击测试已经证明 PermissionButton 在 disabled/busy 缺失时连点会发两笔——本按钮连 disabled=busy 都没有。
- **工程缺陷**：成立。
- **与 005 / B2 只登记的关系**：005 定稿写死的是治理页七个写动作走 runWrite，B3 按此最小处置，B4 归因范围内。本条是 B5 整模块重审扫到的**另一页**写按钮，不是把 005 重开，也不是把 B1 OBS-3（默认 user-1、应用按钮只禁 selected.id）改判——OBS-3 讲的是打到不存在对象，本条讲的是同一对象连发两笔。用户 B5 重点明确问「有无仍可双击重复提交的写按钮」。无新业务需求，不发明「复制必须二次确认」。
- **最小拟处置（本席不修，只记录）**：复制/应用与保存修改共用同一个 submitting（或套一层与 runWrite 同构的 busy ref）；提交中按钮禁用；补一条连点「复制模板」只发一笔 POST 的测试。不改公共字典、客户端、后端。

---

## 8. 观察（建议修复队列「否」，不升队）

维持 B2 双席只登记，无新证据：

1. 岗位编码、所属组织、目标员工、权限模板编号手填内部标识符。
2. hasPermissions 的 wildcard 分支（后端恒 false）。
3. 员工账号页头「企业微信同步结果」「禁用」而页内无对应 UI。
4. 速查表权限状态五态 vs 后端无改状态接口。
5. 模板页 / Can / PermissionGate 无「文件版本」行。
6. 默认 `organizationId="org-1"`、`targetUsersText="user-1"`。
7. 权限码作为勾选主标签（四样例外；small 里有中文名）。
8. 账号创建只有 staff / outsourced_vendor。
9. 授权申请引导无最终审批人字段（范围变更有）。
10. GrantPolicy 运行时多余键（如 deadlineWorkingDays）会原样 POST 回去。
11. `usr_master_authority` + `master-authority` 只改删除按钮文案。
12. 治理页 `AdminDataState empty={false}`。
13. 保存授权政策权限码 `admission.policy.manage` 与 Controller 一致、语义属入场域。
14. origin/main 全量前端单测另有 3 条失败（TraceLinkDrilldown / TraceLinkP2c），属别的模块（XM-M038-A）。
15. **本席新增观察、不入队**：005 定稿写「装配角色与审批各打桩一次 403」，当前只有装配角色 403；该断言是 `stringMatching(/\S/)`，不能区分后端中文原句与兜底句。runWrite 代码路径已被装配角色 403 + 防双击覆盖，不为同一包装函数缺第二条同等测试单独立项。
16. ponytail-review：无「可删的无需求空壳」。STAFF_LEVEL_IDS 页内生成是 008 定稿明确不要 staff-levels 接口。GRANT_*_LABELS 是公共字典缺键的最小页内表，不是重复抽象。

---

## 9. 三线记录摘要

- **第一性原理**：三问见 §3。用途成立。实际执行：worktree 57/57 + tsc 0。未验证就放行的前提：模板页复制/应用忙态——现改为缺陷而非前提。
- **顺从式**：16 文件全文覆盖；106 判据逐项适用/无本面见 §4。
- **对抗式**：grilling Q1–Q10 见 §5。反例打到复制按钮无 busy。

---

## 10. 结论

**FAIL**。

- 001…010 作为已定稿最小处置：落地、无回归到修前行为。
- 新工程缺陷 **GR-M038-012**（技术性 / GEN-007）：权限模板复制/应用仍可双击连发。须经 B2 分类定稿后入队（当前 001…010 已否，不在修复队列，不能直接回 B3 沿用旧 ID）。
- 添加项：0。questions：0。无 HTML 修改申请。
- B2 只登记观察：不改判。
- 57 测全绿 **不能** 把模块收成 PASS（测试未覆盖复制连点；绿不等于无缺陷）。

本席未改业务代码、未写 findings/ledger/plan、未留 worktree。一次性 worktree `/home/james/wt-m038-b5r1-grok-ro` 已 `git worktree remove`（无 `--force`），目录不存在。

---

## 11. 命令与产物（类型 4）

| 命令 | 要点 |
|---|---|
| `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| `git rev-parse HEAD` / `origin/main` | `e456ba96…` / `ebb1eb99…` |
| `git ls-files -z -- <16> \| xargs -0 cat \| wc -l` | 工作区 **3210** |
| 16 文件 sha256 vs `git ls-tree 9df1fad44caa2ef615993fe970d3efc4620aca25` | 逐字节相同 |
| origin/main worktree vitest 11 files | 57 passed, exit 0；worktree 已删 |
| origin/main worktree `npx tsc -b --noEmit` | exit 0 |
| psql `complex_test` SELECT | staff_levels=30 与页内生成逐键相同；grant CHECK 3 态 6 类型；templates type=role×5；created_via=legacy；岗位五码均在 POSITION_LABELS |
| AccessController / PartnerController | 门控码与页面 PermissionButton 一致 |
| `test -f .git/MERGE_HEAD` | NO_MERGE |

映射行（锁内原文）：

`M038 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-182, BEQR-183, BEQR-184, BEQR-185, BEQR-186, BEQR-187, BEQR-189, BSM-001, BSM-003, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-073, BSM-074, BSM-084, BSM-085, BSM-086, BSM-087, BSM-088, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-022, PGM-024, PGM-026, PGM-027, PGM-031, PGM-032, PGM-033, PGM-034, PGM-035, PGM-036, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-059, PGM-060, PGM-065, PGM-066]`
