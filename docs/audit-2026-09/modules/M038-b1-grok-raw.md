CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M038 B1 席乙（Grok 4.6/xhigh）修复前全模块审计 · V0.01

- 席位：审计席乙（独立；未读取 `modules/M038-b1-self-raw.md`，不依赖席甲发现）
- 阶段：V0.17 阶段 B / B1（本模块首次开工，PENDING→IN_PROGRESS；本席不改 ledger/findings/plan）
- 正本：`/home/james/complex-management` main 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`（落后 `origin/main`，含未推提交与暂存；本模块 16 文件与 `origin/main` 逐字节相同）
- `origin/main`：`ebb1eb99c610df330c07d24278798c2d3611299a`（M037 检查点 PR #478 已合并）
- 计划：`plan/modules.md` V0.04 M038「功能权限点、职级三十档、权限模板与最终权限计算／网页与验证／第1段」；计划冻结有效内容行 **2787**；本席实算 `git ls-files -z -- <16 paths> | xargs -0 cat | wc -l` = **2787**
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` = 上文独占行
- 映射 106 项：与锁 L6417 逐字相同；COUNT=106 unique=106 dups=[]；未新增判据、未扩别模块
- 结论：**FAIL**（有工程缺陷；发布口径 vitest 在 origin/main 一次性 worktree 38/38 绿，测试绿不替代缺陷）
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M038-001 … GR-M038-011
- 添加项（B1 只登记，不代用户回答 2.2；本批无）：无
- 只登记、建议队列「否」：观察见 §7
- 本席未改任何业务文件/三 HTML/台账/锁，未 fetch/commit/push/merge/deploy，未用 MCP/子代理/第三席，未问操作员，未对任一库做写操作
- 本报告不表示全仓或发布门禁通过。M031–M037 后端已闭环，只作只读追证；共享前端（`adminRoutes.ts`、`statusLabels.ts`、`adminClient.ts`、`types.ts`、`useAdminResource.ts`）属别的模块，凡结论依赖它们的均以 `git show origin/main:<path>` 为准

---

## 0. 环境实用强制技能（自核，铁律 3）

本席按 `160-audit-prompt.md` V0.17 §6 与 `plan/runbook.md` V0.02 §2 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读）；Arc `audit` / Trail of Bits `audit-context-building` / `differential-review` 本体要派子代理或另写 §7 外产物，**不调用**。B1 禁止修复，故 TDD 落测试、ponytail 修复、receiving-code-review 的 IMPLEMENT 一律不执行。runbook「阶段 B 不创建 worktree」被本轮用户指令覆盖：前端单测只在基于 origin/main 的一次性 worktree 里跑。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由到 audit；**不执行** Arc 全量审计工作流 | 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，记缺口 |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft ask` 定位 AccessController 映射；`graft skeleton AccessController.java` | 无 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling/SKILL.md`；`grill-me` 仅转发到 grilling | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机未找到独立 SKILL.md**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 删除测试：PermissionGate/Can 是深模块（小接口 hasPermissions + 大量 DENY/wildcard/fail-safe）；三页是浅适配器 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（sha256/vitest/psql SELECT/git show）；不改生产代码 | 无 |
| `systematic-debugging` | superpowers 同名 | 只借读错误→追数据流（页面→AdminApi→AccessController→闭集/CHECK）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS/FAIL 前实跑 sha256/ls/wc/psql/vitest/worktree remove | 无 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 可删：硬编码 master-authority 双条件、默认 `user-1`/`org-1`、总结表重复「详情」列 | 不落地 |
| `ponytail` | 同包 | B3 默认强度，B1 不用 | 节点不适用 |
| `tdd` / `test-driven-development` | 存在 | 仅 B3 可执行行为 | 节点不适用（B1 禁修） |
| `requesting-code-review` / `receiving-code-review` | 存在 | B4 | 节点不适用 |
| `writing-for-agents` | mattpocock productivity | raw 按完成条件+文件:行组织 | 无 |
| `resolving-merge-conflicts` | 存在 | 无合并 | 节点不适用 |
| `git-guardrails-claude-code` | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 可选；本体要问人装 hook | **存在但未安装**（只读审计、禁写仓库其它文件） |
| `i-have-adhd` | marketplace 存在 | Codex 回执模式；本席 stdout 由适配器丢弃 | 不套到本 raw 以外 |
| `long-running-background-tasks` | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | vitest 在前台完成（2.60s），未起 monitor | 无 |
| `audit-context-building` | Trail of Bits | 本体强制子代理写 `audit-context/` | **不用** |
| `differential-review` | Trail of Bits | PR/diff 安审 | **不用**（B1 全量非 B4 diff；禁子代理） |
| Arc `audit` | howells | 机械检查+并行评审 | **不用** |

明确不用（提示词已划掉）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`（被本轮用户指令的一次性 origin/main worktree 覆盖，仅测未改源）、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`/`debt`/`gain`、MCP、第三主体。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

---

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
cat docs/audit-2026-09/plan/criteria-lock.sha256
# 同值
git rev-parse HEAD
# e456ba96c84185c8d39f18f548011daac48a6466
git rev-parse origin/main
# ebb1eb99c610df330c07d24278798c2d3611299a
git ls-files -z -- <16 files> | xargs -0 cat | wc -l
# 2787
```

- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行 `M038 → [...]`（L6417）与开工口径 106 项逐字相同
- sidecar 与本席实算哈希相等；ledger 顶部 `CRITERIA-LOCK-SHA256:` 同行（只读，未改）
- 本席未设 `GIT_INDEX_FILE`，未动主索引
- 本模块 16 文件工作区 SHA256 与 `git show origin/main:<path> | sha256sum` 逐文件相同

ledger 只读：M038 行 `IN_PROGRESS`、B3 列历史累计不代表本版已审（本席不写台账）。findings 无本版 M038 段。

三份 HTML 只读、未改；未发现需铁律 4 人闸修改的原文错误。登记「HTML 未规定」前已 grep 三份 HTML。

### 1.1 本模块 16 文件覆盖（全文读，不抽样）

| 路径 | wc -l | SHA256（工作区 = origin/main） |
|---|---:|---|
| `apps/web/src/admin/pages/AccessGovernancePage.tsx` | 738 | `76bf98c91ae0588ad66fd7be75b8810f3b012be1a8f6529e6a972d58eff72c08` |
| `apps/web/src/admin/pages/AccessGovernancePage.degraded.test.tsx` | 61 | `b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5` |
| `apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx` | 113 | `c9978685e41352fdf40fe3846c64a3f1a250c02ac4597a4b4811771b77d2d5f3` |
| `apps/web/src/admin/pages/AccessGovernancePage.scopechange.test.tsx` | 82 | `84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6` |
| `apps/web/src/admin/pages/AccessGovernancePage.test.tsx` | 185 | `ee160c7f86bd8a7c9087da4aea21ab82db17334295d796d7b719c070b2fe3b56` |
| `apps/web/src/admin/pages/AccessUsersPage.tsx` | 370 | `e158cd17637d819a42468e17183e6403f8ad682bcb7bb630871364afb3a17151` |
| `apps/web/src/admin/pages/AccessUsersPage.metricdetail.test.tsx` | 41 | `ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca` |
| `apps/web/src/admin/pages/AccessUsersPage.partnerbind.test.tsx` | 113 | `33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d` |
| `apps/web/src/admin/pages/AccessUsersPage.robustness.test.tsx` | 45 | `fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8` |
| `apps/web/src/admin/pages/AccessUsersPage.test.tsx` | 92 | `05f3a7f0093bb847a238e8e8160e0c919b3feb86398df43252cbcbc0f13f4836` |
| `apps/web/src/admin/pages/PermissionTemplatesPage.tsx` | 548 | `59ac645509458f2da463236804c0d431068fb72b82cd0765b87e233cca919a09` |
| `apps/web/src/admin/pages/PermissionTemplatesPage.guidedcreate.test.tsx` | 116 | `e026609a0545a09f6013b258c8cc60a7f1bbd4fb53d340636f0917da4fa9b6d6` |
| `apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx` | 94 | `3baf9ac47895584e48427fd91383d2826fe2f0117577172eecc400ec3d755d09` |
| `apps/web/src/admin/security/Can.tsx` | 84 | `bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193` |
| `apps/web/src/admin/security/PermissionGate.tsx` | 44 | `a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5` |
| `apps/web/src/admin/security/PermissionGate.test.ts` | 61 | `421c6e0e9de23cfab87b0d51f32535968aee73505b81d07c3f79793444d37ffe` |
| **合计** | **2787** | 与 `git ls-files -z \| xargs -0 cat \| wc -l` 一致 |

---

## 2. 发布口径验证（origin/main 一次性 worktree）

用户指令：`git worktree add --detach`；把主工作区 `node_modules` 与 `apps/web/node_modules` 软链进去；在 `apps/web` 下 `npx vitest run` 加本模块测试路径；跑完先删软链再 `git worktree remove`（不用 `--force`）；不得改源码与测试，不得装依赖。

```
git worktree add --detach /tmp/m038-b1-vitest-origin-main origin/main
# HEAD ebb1eb99c610df330c07d24278798c2d3611299a
ln -s /home/james/complex-management/node_modules /tmp/m038-b1-vitest-origin-main/node_modules
ln -s /home/james/complex-management/apps/web/node_modules /tmp/m038-b1-vitest-origin-main/apps/web/node_modules
cd /tmp/m038-b1-vitest-origin-main/apps/web && npx vitest run <11 test files>
# Test Files  11 passed (11)
# Tests  38 passed (38)
# Duration  2.60s
# VITEST_EXIT=0
rm -f .../node_modules .../apps/web/node_modules
git worktree remove /tmp/m038-b1-vitest-origin-main
test ! -d /tmp/m038-b1-vitest-origin-main  # WORKTREE_REMOVED
```

类型 2/4 证据：vitest 可执行产物 38/38 绿。测试绿 **不** 证明无缺陷（见 GR-M038-001/002/003/008：夹具用 `wildcard: true`、`lvl_m1`/`lvl_p1`、`template_type: "base"`，从未打到库内真实 `role` 类型或 `applied` 状态）。

后端只读追证：`git show origin/main:backend/src/main/java/com/complex/platform/access/AccessController.java` 全文。psql 只对 `complex_test` 做 SELECT（三变量从 `/etc/complex/complex.env` 读入，URL 库名改 `complex_test`，口令不回显）：

- `staff_levels` 行数 **30**，id 闭集 `lvl_m1_lower` … `lvl_m10_upper`
- CHECK：`chk_staff_levels_main` M1–M10；`chk_staff_levels_sub` upper/middle/lower；`chk_staff_levels_rank` 1–30
- `chk_position_role_templates_status` active/inactive
- `chk_access_grant_requests_status` pending/applied/rejected
- `chk_access_grant_requests_type` user_role/user_position/temporary_permission/role_permission/user_scope_change/position_high_risk_release
- `permission_templates` **无** template_type CHECK；库内 5 行均为 `template_type=role`
- `access_users` 无 account_type/status CHECK

---

## 3. 第一性原理三问

### 3.1 这些文件为什么必须存在？

删掉它们，三份 HTML 下列条目做不到：

- **PermissionGate / Can**：PGM-040「每一个单一功能都可以单独授权」、BEQR-151「最终权限 = 角色 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝（拒绝优先）」、BSM-114「不同人只看自己该看的、只做自己该做的」、BSM-007「还没定类的一律先当高危、不自动同步」。页面壳（别模块 `adminRoutes`）用 PermissionGate 做整页门；本模块提供判定函数与按钮级闸。
- **AccessGovernancePage**：PGM-041 角色装配（职位↔模板、定岗自动适配、直接装配、员工单独授予/显式拒绝）、高危授权审批、临时权限频繁申请总结；BSM-004 配置存档/删除审批；BSM-005 引导新建。
- **PermissionTemplatesPage**：PGM-041「支持角色级权限模板」、BSM-007「权限模板选中后可以直接查看和修改已配的权限，勾选区域按左侧菜单归类」、BEQR-151 基础/临时模板。
- **AccessUsersPage**：PGM-033 员工注册两条路径 + 外包账号；PGM-043 外包机构顶层账号绑定；BSM-005 新建员工账号引导。

答得上 → 有业务上位。不能因「测试绿」或「后端已闭环」把本页当成纯展示。

### 3.2 声称的行为有没有被实际执行验证？

| 声称 | 验证 | 结果 |
|---|---|---|
| 本模块单测 | origin/main worktree `npx vitest run` 11 文件 | 38/38 绿，exit 0 |
| hasPermissions DENY 优先 / wildcard / loading fail-safe | `PermissionGate.test.ts` 真跑函数 | 绿，断言能失败 |
| 岗位模板引导 POST、范围变更 PUT、外包绑定 POST、模板新建 POST | 对应 *.test.tsx 打真实组件 + stub fetch | 绿 |
| 职级三十档闭集下拉 | **无**。UI 自由文本；测试还提交 `lvl_p1`/`lvl_m1` | 未验证；与 DB 30 id 不符 |
| 权限模板类型中文 | 测试只用 base/temporary；库内实际 `role` | 未验证真实数据 |
| 授权申请通过后状态中文 | 测试停在 pending；后端落地 status=`applied` | 未验证 |
| PermissionButton 无权限禁用并拦截 click | 全部页面测试 `wildcard: true`；Can.tsx 无测试文件 | 未验证 |
| 写失败可见错误 | 定岗/装配/覆盖/审批/范围变更无 try/catch | 未验证 |

### 3.3 哪个前提未经验证就当真？

1. 「职级输入框里的码就是三十档」——前提假。闭集在 `staff_levels` 与 GET `/staff-levels`，页面从不调用该端点。
2. 「formatBusinessStatus 能翻所有本页枚举」——前提假。origin/main `statusLabels.ts` 无 `applied`、`user_scope_change`、`position_high_risk_release`、`role_permission`；未知码 `return code`（英文原样）。
3. 「模板类型下拉 {base,temporary,copy} 覆盖后端」——前提假。`complex_test` 五条模板全是 `role`；后端 `templateFromRequest` 默认 `"base"`，无 CHECK。
4. 「测试绿 = 无权限按钮安全」——前提假。测试全部 wildcard。
5. 「AdminApi 幂等键能防双击」——前提假。每次请求 `nextIdempotencyKey()`，双击 = 两个键 = 两笔写。

---

## 4. 顺从式：106 判据逐项适用

判据定义均读锁全文；HTML 原文读对应锚点。共享前端以 origin/main 为准。

### 4.1 GEN（18）

| ID | 适用？ | 理由 |
|---|---|---|
| GEN-001 | 适用 | 三页+两闸均有 HTML 上位；无纯转发文件 |
| GEN-002 | 适用 | 入口/主路径有 vitest；错误路径部分写操作未接住（GR-006） |
| GEN-003 | 适用 | 11 测试文件渲染真组件；关键分支（无权限按钮、授权申请提交、applied 展示）未覆盖（GR-008/010） |
| GEN-004 | 适用 | 职级/岗位自由文本未在首次使用前校验闭集（GR-001/005） |
| GEN-005 | 适用 | 按钮权限码与 origin/main AccessController 一致；整页闸在别模块 adminRoutes |
| GEN-006 | 适用 | 本页不直接写库；失败半成品由后端事务管。前端双击可发两笔（GR-007） |
| GEN-007 | 适用 | 引导保存有 submitting；定岗/装配/审批等无 busy（GR-007） |
| GEN-008 | 适用 | 职级/模板类型/授权状态与后端闭集或码表不一致（GR-001/002/003） |
| GEN-009 | 适用 | 部分写无 catch，失败不可见（GR-006） |
| GEN-010 | 适用 | 密码框 type=password；账号详情测试断言不展示手机号。无注入面在本模块 |
| GEN-011 | 适用 | 写操作带 operatorNote；本页不展示审计日志（无本面日志 UI） |
| GEN-012 | 适用 | 授权列表分页；无无界循环 |
| GEN-013 | 无本面 | 无本模块构建配置 |
| GEN-014 | 无本面 | 无本模块迁移 |
| GEN-015 | 适用 | 键盘点行、抽屉 Esc、中文规则；字段名含 id、英文枚举上屏（GR-002/003/004/005/009/011） |
| GEN-016 | 适用 | 页头「企业微信同步结果」本页无入口（观察，不单独立项） |
| GEN-017 | 适用 | 本席按冲突→细化→技术性→添加分类 |
| GEN-018 | 适用 | 结论落到 HTML 原文、vitest、psql SELECT、git show |

### 4.2 BEQR（27）

| ID | 适用？ | 理由 |
|---|---|---|
| BEQR-063 | 无本面 | 酒吧现场反结/沽清/免单，非本页；本页只做通用授权审批壳 |
| BEQR-074 | 无本面 | 隐私桌台权限点，非本三页 |
| BEQR-143 | 无本面 | 四类主体定义，非本页录入 |
| BEQR-144 | 无本面 | 经营主体关系 |
| BEQR-145 | 无本面 | 城市项目公司职责 |
| BEQR-146 | 无本面 | 运营公司职责 |
| BEQR-147 | 无本面 | 收入归属 |
| BEQR-148 | 无本面 | 合作模式 |
| BEQR-149 | **适用** | 职级 M1–M10×上中下共 30 档。页面自由文本，未拉闭集（GR-001） |
| BEQR-150 | 无本面 | 终端十一端 |
| BEQR-151 | **适用** | hasPermissions：DENY 优先、wildcard、grants。与公式一致 |
| BEQR-152 | 适用部分 | 范围变更/dataScope=project 承接项目隔离；敏感经营数据查看点不在本页勾选清单之外另做 |
| BEQR-153 | 无本面 | 终端开关优先级 |
| BEQR-154 | 无本面 | 综合体项目状态 |
| BEQR-155 | 无本面 | 商户状态 |
| BEQR-156 | 无本面 | 合同状态 |
| BEQR-157 | 无本面 | 员工人事状态五档；本页是账号 status active/invited/disabled，后端闭集亦然 |
| BEQR-158 | 无本面 | 设备状态 |
| BEQR-159 | 适用部分 | 「权限状态」五档；模板用 active/inactive 译成有效/未激活，与五档不等。不单独立项（锚点更像权限点生命周期，模板另有 status） |
| BEQR-166 | 适用部分 | 审批链「只指定最终审批人」。范围变更有字段；授权申请弹窗无（观察：HTML 允许默认总经理） |
| BEQR-182 | 无本面 | 考勤日结果 |
| BEQR-183 | 无本面 | 打卡方式 |
| BEQR-184 | 无本面 | 请假类型 |
| BEQR-185 | 无本面 | 请假单状态 |
| BEQR-186 | 适用部分 | 定岗表单有基础薪资(元)→分；职级只绑薪资大范围见 PGM-042 |
| BEQR-187 | 无本面 | 提成发放 |
| BEQR-189 | 无本面 | 管理费方式 |

### 4.3 BSM（20）

| ID | 适用？ | 理由 |
|---|---|---|
| BSM-001 | 适用背景 | 「谁能操作」是治理底座；本页是权限操作面 |
| BSM-003 | **适用** | 数据可改可删、引导、权限分岗 |
| BSM-004 | **适用** | 岗位模板存档/申请删除/最高权限批准；权限模板页无存档删除（配置类「等」未点名模板，不单独立项） |
| BSM-005 | **适用** | 三页新建均引导弹窗（岗位模板、授权申请、员工账号、权限模板） |
| BSM-006 | 无本面 | 动态二维码 |
| BSM-007 | **适用** | 日常 vs 高危、模板按左侧菜单勾选（PermissionTemplatesPage groupedPermissions）。wildcard 由后端有效集给出 |
| BSM-009 | 适用入口 | 「基础管理」含员工与权限 |
| BSM-028 | 适用部分 | 基础管理主要功能之一是权限 |
| BSM-029 | 适用部分 | 和谁有关：权限连接各业务 |
| BSM-072 | 无本面 | 商户在授权范围内用自己的系统 |
| BSM-073 | 无本面 | 商户管理主要功能 |
| BSM-074 | 无本面 | 商户和谁有关 |
| BSM-084 | 无本面 | 员工手机端排班考勤 |
| BSM-085 | 无本面 | 日常工作手机 |
| BSM-086 | 无本面 | 排班考勤请假 |
| BSM-087 | 无本面 | 我的薪资 |
| BSM-088 | 无本面 | 提成与人员服务边界 |
| BSM-114 | **适用** | 不同人只看该看的：PermissionGate 隐藏整页、PermissionButton 禁用写按钮 |
| BSM-115 | 适用背景 | 子系统必须连权限 |
| BSM-120 | **适用** | 已拍板含界面全中文（与 PGM-066 同文） |

### 4.4 PGM（41）

| ID | 适用？ | 理由 |
|---|---|---|
| PGM-001 | **适用** | 含「界面全中文」全局原则（L287–299）；本页字段名/枚举违反见 GR |
| PGM-002 | 适用 | 治理顺序第 3 步「员工是否有当前功能权限」 |
| PGM-003 | **适用** | 模块入口「权限和职级」卡片 |
| PGM-004 | 无本面 | 综合体和集团卡片 |
| PGM-005 | 无本面 | 空间和点位 |
| PGM-006 | 无本面 | 商户和主体 |
| PGM-007 | 无本面 | 合同和准入 |
| PGM-008 | 适用部分 | 「员工和手机」卡片；本页做账号开通，不做手机绑定 |
| PGM-009 | **适用** | 「权限和职级」卡片正文 |
| PGM-010 | 无本面 | 终端开关 |
| PGM-011 | 无本面 | 设备管理入口 |
| PGM-012 | 无本面 | 商户侧入口 |
| PGM-013 | 无本面 | 开放接口 |
| PGM-014 | 适用部分 | 留痕：operatorNote/reason；无复核队列 UI |
| PGM-015 | 无本面 | 楼宇运营 |
| PGM-016 | 无本面 | 应急与隐私入口 |
| PGM-017 | 无本面 | 综合体和集团专章 |
| PGM-018 | 无本面 | 物理项目与运营公司 |
| PGM-019 | 无本面 | 各主体职责表 |
| PGM-020 | 无本面 | 综合体主要功能 |
| PGM-021 | 无本面 | 综合体业务意义 |
| PGM-022 | 无本面 | 空间点位 |
| PGM-024 | 无本面 | 空间影响业务 |
| PGM-026 | 无本面 | 商户主要功能 |
| PGM-027 | 无本面 | 商户业务边界 |
| PGM-031 | 无本面 | 外部经销商 |
| PGM-032 | 适用部分 | 员工要能对应起来；本页建账号/外包绑定，不绑私人手机 |
| PGM-033 | **适用** | 员工注册两条路径：本页 staff→POST `/staff/registrations`，外包→POST `/access/users` |
| PGM-034 | 无本面 | 工作手机和企业微信；页头描述提到但无 UI（观察） |
| PGM-035 | 无本面 | 提成发放 |
| PGM-036 | 无本面 | 设备管理专章 |
| PGM-040 | **适用** | 单一功能授权、角色批量、员工单独增减 |
| PGM-041 | **适用** | 权限规则全文：模板、自定义、DENY 优先、临时授权、角色装配、高危五类、范围变更 |
| PGM-042 | **适用** | 职级 M1–M10 上中下；只绑薪资大范围；不能替代功能权限点 |
| PGM-043 | **适用** | 外包账号+绑定机构；审计为岗位码非账号类型 |
| PGM-044 | 无本面 | 桌台/挂单/经销权限点表 |
| PGM-048 | 无本面 | 商户侧全开放 |
| PGM-059 | 适用部分 | 账号详情不展示手机明文（测试断言）；无脱敏号码 UI |
| PGM-060 | 无本面 | 会员身份营销 |
| PGM-065 | 适用背景 | 治理层连接业务：权限是连接之一 |
| PGM-066 | **适用** | 已拍板界面全中文四样例外；本页字段名/枚举违反 |

---

## 5. 对抗式（grilling 结构自问自答）

❓ **Q1 — 无权限时 HTML 要隐藏还是禁用？**
HTML 沽清「置灰不隐藏」是商品菜单，不是后台写按钮。PGM/BSM 未规定后台写按钮形态。本模块：PermissionGate 整页换成「权限不足」（隐藏内容）；PermissionButton 禁用+拦截 onClick。属细化，不是冲突。

❓ **Q2 — 职级自由文本算不算「多一个」闭集？**
算。审计要点：前端取值须逐键等于后端校验闭集与 DB CHECK。自由文本可提交 `lvl_p1`（引导测试就是这么做的）。DB 只有 30 个 `lvl_mN_{lower,middle,upper}`。GET `/staff-levels` 存在却从未调用。

❓ **Q3 — 权限码英文是不是四样例外？**
是。PGM-001/066：单据号、设备编号、二维码内容、**系统内部标识符**。权限码、用户 id、模板 id 是标识符。**字段名**「职级 id」「组织 ID」不是标识符，必须中文。

❓ **Q4 — 测试是不是把被测逻辑整个 mock 掉？**
没有 mock 掉页面组件。它们 stub `fetch`、走真实 AdminApi 解包。但全部 `wildcard: true`，等于关掉本模块最核心的按钮闸。PermissionGate 组件的 loading/denied 渲染零测试。授权申请 POST 零测试。

❓ **Q5 — 保存授权政策用 admission.policy.manage 是否前后端不一致？**
否。origin/main AccessController L127–129 同一码。属已闭环后端选型，本模块闸与后端一致，不立前端不一致项。

❓ **Q6 — 通过授权申请后用户会看到英文吗？**
会。`reviewGrant` 成功落地 `status="applied"`（AccessService L1005）。本页 `formatBusinessStatus`（origin/main 码表无 `applied`）原样返回。首次点「通过」且审批链走完即可复现。

❓ **Q7 — 打开权限模板页对着真实库会看到英文类型吗？**
会。`complex_test` 五条模板 `template_type=role`。`templateTypeLabel` 无 `role` 则 `return value`。首次打开该页即可复现。测试用 base/temporary 绿，打不中。

❓ **Q8 — 双击「定职位并适配」是一笔还是两笔？**
两笔。无 submitting；每次 `api.put` 由 AdminApi 生成新 idempotencyKey。后端按新键各执行一次。

❓ **Q9 — 为理论场景补需求了吗？**
没有。缺测试只立本聚合关键分支且首次正常使用可复现者（按钮闸、授权申请提交、真实枚举展示）。不为经销/桌台/考勤补本页需求。

---

## 6. 临时 findings

四分类顺序均已走：冲突 → 细化 → 技术性 → 添加。本批无添加。

### GR-M038-001 — 职级三十档未按闭集下拉，自由文本可提交非库内 id

- **文件:行**：`AccessGovernancePage.tsx:107`、`:508`、`:521`、`:655`；`AccessGovernancePage.guidedcreate.test.tsx:88-97`
- **原文及上下文**：

```
107:  const [templateForm, setTemplateForm] = React.useState({ id: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower", ...
508:  <input aria-label="职级 id" placeholder="职级 id" value={positionForm.staffLevelId} ... />
655:  <label>职级 id<input ... placeholder="职级 id" value={templateForm.staffLevelId} ...
```

引导测试提交：`staff_level_id` = `"lvl_p1"`（不在 30 档）。主测试表数据用 `"lvl_m1"`（亦非 30 档 id，仅展示成「M1」无上/中/下）。

- **正常触发/影响**：用户在定岗/岗位模板/范围变更输入任意字符串；后端 `UNKNOWN_STAFF_LEVEL`。页面从不调用 GET `/api/v1/access/staff-levels`（Controller L256–260，`access.staff_level.view`）。HTML 要求 30 档，DB CHECK 与种子正好 30 行。
- **最小拟处置**：拉 staff-levels，做成 M1–M10×上/中/下中文 `<select>`，值逐键等于 30 个 id；禁止自由文本。
- **四分类**：冲突？否（HTML 未写死「必须是下拉」）。细化：是（BEQR-149/PGM-042 的下位实现做错）。技术性：否。添加：否。
- **工程缺陷**：成立
- **冻结 ID**：BEQR-149，PGM-042，GEN-008，GEN-004
- **依据**：HTML 原文「M1–M10，每级分上 / 中 / 下（共 30 档）」；psql `SELECT id FROM staff_levels` 30 行；vitest 引导测试提交 `lvl_p1`

### GR-M038-002 — 权限模板类型：库内 `role` 英文上屏；下拉闭集与数据不一致

- **文件:行**：`PermissionTemplatesPage.tsx:18-21`、`:375-378`、`:453`、`:495-498`
- **原文及上下文**：

```
18: const TEMPLATE_TYPE_LABELS: Record<string, string> = { base: "基础模板", temporary: "临时模板", copy: "复制模板" };
21:   return TEMPLATE_TYPE_LABELS[value] ?? value;
453: <td>{templateTypeLabel(row.templateType)}</td>
```

- **正常触发/影响**：打开权限模板页。`complex_test` 五条均为 `template_type=role` → 类型列显示英文 `role`。新建下拉只有 base/temporary/copy，不能选库里已有的 `role`。后端无 CHECK，默认 `"base"`。
- **最小拟处置**：与后端/种子对齐闭集并中文映射；表与下拉同一闭集。在未扩锁前按现有库值补「角色模板」映射，或把种子改成页面已有码（须后端模块，不在本页假造新业务类型）。
- **四分类**：冲突（枚举取值必须中文，PGM-001/066）。细化（类型闭集）。
- **工程缺陷**：成立
- **冻结 ID**：PGM-066，PGM-001，GEN-015，GEN-008
- **依据**：HTML「枚举取值……必须全部为中文，不得出现任何英文」；psql `SELECT template_type ...` 五条 `role`；origin/main `formatBusinessStatus` 不用于该列

### GR-M038-003 — 授权申请通过态 `applied` 与内部类型无中文，原样英文上屏

- **文件:行**：`AccessGovernancePage.tsx:84-88`、`:384`、`:418`、`:572`、`:727`
- **原文及上下文**：

```
84: function statusBadge(status?: string | null) {
87:   return <span className={`admin-badge ${kind}`}>{value ? formatBusinessStatus(value) : "—"}</span>;
384: setActionResult(`授权申请已提交：${result.id}（${formatBusinessStatus(result.status ?? "")}）`);
572: <td>{formatBusinessStatus(row.requestType)}</td> ... <td>{statusBadge(row.status)}</td>
```

origin/main `AccessService.reviewGrant` 审批链走完后 `saveGrantRequest(..., "applied", ...)`。CHECK 类型含 `user_scope_change`（本页范围变更会生成）。origin/main `statusLabels.ts`：`formatBusinessStatus` 未知码 `return code`；表中无 `applied` / `user_scope_change` / `position_high_risk_release` / `role_permission`。

- **正常触发/影响**：点「通过」且为最后节点 → 结果句与表格状态出现 `applied`。提交范围变更后切到授权申请表 → 类型列 `user_scope_change`。
- **最小拟处置**：本页对授权申请 status/type 做闭集中文（或推动码表模块补键，以 origin/main 码表现状本页仍须自己挡住英文上屏）。
- **四分类**：冲突（枚举/状态必须中文）
- **工程缺陷**：成立
- **冻结 ID**：PGM-066，GEN-015，GEN-008
- **依据**：HTML 界面全中文；`git show origin/main:apps/web/src/customer/i18n/statusLabels.ts` 无上述键；AccessService L1005 `applied`

### GR-M038-004 — 展示给人看的字段名/aria-label/校验含英文 id/ID

- **文件:行**（摘）：`AccessGovernancePage.tsx:506-508`、`:517-521`、`:530-531`、`:539`、`:654-657`；`AccessUsersPage.tsx:322-325`、`:329`；`PermissionTemplatesPage.tsx:413`
- **原文及上下文**：

```
508: <input aria-label="职级 id" placeholder="职级 id" ...
325: <label>所属组织<input ... placeholder="组织 ID" /></label>
322: return "组织 ID 不能为空，用于数据范围和留痕。";
413: <label>目标员工 ID
```

另有「用户 id」「装配用户 id」「覆盖用户 id」「新所属项目 id」「新范围职级 id」「附件 ID，逗号分隔」。

- **正常触发/影响**：读屏与可见 placeholder/label。HTML：字段名必须中文；四样例外是值（单据号/设备编号/二维码/系统内部标识符），不是字段名。
- **最小拟处置**：改成「职级」「用户」「所属组织」「目标员工」「附件编号」等中文；值仍可用标识符。
- **四分类**：冲突
- **工程缺陷**：成立
- **冻结 ID**：PGM-066，PGM-001，GEN-015
- **依据**：HTML L287「包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错」

### GR-M038-005 — 岗位码中文化表不全，未映射码英文上屏；输入自由文本

- **文件:行**：`AccessGovernancePage.tsx:42-53`、`:479`、`:507`、`:654`
- **原文及上下文**：

```
42: const POSITION_LABELS: Record<string, string> = {
43:   auditor_internal: "内部审计", ... ops_manager: "运营经理",
50: function zhPosition(code?: string | null): string {
52:   return POSITION_LABELS[code] ?? code;
```

引导测试写入 `front_desk`。`complex_test` 岗位模板五码均在表内；`ops_manager` 在表内但库种子当前无。任意未映射码（测试已用 `front_desk`）表格「岗位」列英文上屏。无岗位码 CHECK（V349 注释亦称自由文本）。

- **正常触发/影响**：保存未映射岗位码后表格/抽屉显示英文码。本页已把「岗位」当给人看的列（测试禁止抽屉出现 `ops_manager`）。
- **最小拟处置**：岗位改中文选择（至少覆盖在用码+禁止未映射上屏）；或未映射时显示「未识别岗位」而非英文码。
- **四分类**：冲突（给人看的岗位列英文）
- **工程缺陷**：成立
- **冻结 ID**：PGM-066，GEN-015
- **依据**：HTML 零英文；源码 fallback `?? code`；引导测试 `front_desk`

### GR-M038-006 — 定岗/范围变更/装配角色/覆盖/审批失败无 catch，用户不可见

- **文件:行**：`AccessGovernancePage.tsx:265-280` `assignPosition`；`:284-309` `changeUserScope`；`:311-324` `roleAssignment`；`:328-343` `userPermissionOverride`；`:386-395` `reviewGrant`
- **原文及上下文**：上述函数 `await api.put/post` 无 try/catch。同文件 `savePositionTemplate`/`applyGrantRequest` 有 catch 并 `setGlobalError`。
- **正常触发/影响**：首次点「定职位并适配」遇 4xx/5xx → 未处理 Promise 拒绝，无页内错误。对比引导保存失败会留在弹窗。
- **最小拟处置**：与 `applyGrantRequest` 同一 catch → setGlobalError。
- **四分类**：冲突？否。细化？能指 PGM 操作失败要提示，但 HTML 未写这条。技术性：是（错误处理，无业务发明）。添加：否。
- **工程缺陷**：成立
- **冻结 ID**：GEN-009，GEN-002
- **依据**：源码对照；GEN-009「错误须可检测……不得吞错」

### GR-M038-007 — 无 busy 的写按钮双击生成两个幂等键，提交两笔

- **文件:行**：同上写按钮 + `:236-245` `saveGrantPolicy` + `:247-256` `changePositionLifecycle`；origin/main `adminClient.ts` `nextIdempotencyKey` 每次请求新键
- **原文及上下文**：`assignPosition` 等无 `*Saving`。`PermissionButton` 只合业务 `disabled` 与权限，无提交中态。AdminApi：`idempotencyKey: body.idempotencyKey ?? nextIdempotencyKey()`。
- **正常触发/影响**：双击「定职位并适配」→ 两次 PUT、两个幂等键 → 两次定岗。引导保存有 `templateSaving`/`grantSaving`/`submitting`/`bindBusy`，对照成立。
- **最小拟处置**：提交中 disabled（与引导保存同一范式）。
- **四分类**：技术性（GEN-007，无业务发明）
- **工程缺陷**：成立
- **冻结 ID**：GEN-007
- **依据**：origin/main adminClient 每次新键；源码无 busy

### GR-M038-008 — 本模块权限闸关键分支无测试：Can/PermissionButton 无权限禁用拦截、PermissionGate loading/denied 未渲染断言

- **文件:行**：`Can.tsx:56-82`；`PermissionGate.tsx:20-44`；全部页面测试 `ADMIN_PERMS = { wildcard: true }`；`PermissionGate.test.ts` 只测 `hasPermissions` 纯函数
- **原文及上下文**：Can 注释写明弥补「按钮级权限 0 实现」；PermissionButton 无权限须 disabled 且 onClick 直接 return。无 `Can.test.ts`。页面测试全部 wildcard，点按钮从不走拒绝。
- **正常触发/影响**：无 `access.role.assign` 的账号打开治理页，写按钮应禁用且点了不发请求。该路径零测试。hasPermissions 有测，组件闸没有。
- **最小拟处置**：渲染 PermissionButton/Can/PermissionGate，无权限时断言 disabled、click 不调用 onClick、loading 显示「权限加载中…」、denied 显示「权限不足」。
- **四分类**：细化（GEN-003；本聚合关键分支且首次正常使用可复现）
- **工程缺陷**：成立（缺测试立项）
- **冻结 ID**：GEN-003，GEN-005
- **依据**：vitest 38 例均未断言 PermissionButton disabled；源码与测试对照

### GR-M038-009 — 同一账号状态码 `active`：创建下拉写「启用」，表格写「有效」

- **文件:行**：`AccessUsersPage.tsx:340-343` vs `:284`
- **原文及上下文**：

```
341: <option value="active">启用</option>
284: <td>{formatBusinessStatus(row.status ?? "active")}</td>
```

origin/main `active: "有效"`。测试断言表格含「有效」。创建外包账号第三步选项是「启用」。

- **正常触发/影响**：同一状态两套中文。HTML 要求枚举取值中文且一致。
- **最小拟处置**：创建选项改用与表格同一中文（或统一用 formatBusinessStatus）。
- **四分类**：细化（账号状态展示的下位不一致）
- **工程缺陷**：成立
- **冻结 ID**：GEN-015，GEN-008，PGM-066
- **依据**：源码两处；origin/main statusLabels `active: "有效"`

### GR-M038-010 — 高危授权申请引导提交（POST `/grant-requests`）无测试

- **文件:行**：`AccessGovernancePage.tsx:349-384`、`:677-736`；测试目录无对应 POST 断言
- **原文及上下文**：引导弹窗 `onSubmit={() => void applyGrantRequest()}`，`submitTestId="access-grant-request-create"`。`AccessGovernancePage.guidedcreate.test.tsx` 只覆盖岗位模板。无测试打开授权申请弹窗并断言 POST body 的 request_type/target_user_id。
- **正常触发/影响**：本页核心写路径「发起授权申请」无回归网。岗位模板/范围变更/覆盖均有 POST/PUT 测试，独缺申请提交。
- **最小拟处置**：仿 guidedcreate：入口→类型/用户→权限码与过期→POST `/api/v1/access/grant-requests`。
- **四分类**：细化 GEN-003（关键分支、首次使用即走）
- **工程缺陷**：成立（缺测试）
- **冻结 ID**：GEN-003，PGM-041
- **依据**：测试文件全文无 `grant-requests` 的 POST body 断言（list GET 有）

### GR-M038-011 — 智能总结表表头两个「详情」，列数与单元格不一致

- **文件:行**：`AccessGovernancePage.tsx:620-623`
- **原文及上下文**：

```
620: <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th><th>建议</th><th>详情</th><th>详情</th></tr></thead>
622: ... <td>岗位</td><td>...</td><td>...</td><td>...</td><td>...</td><td className="admin-row-detail-cell">查看详情</td>
```

thead 7 列，tbody 6 个 td。

- **正常触发/影响**：打开「③ 智能总结」表头错位。首次打开即见。
- **最小拟处置**：删多余 `<th>详情</th>`。
- **四分类**：技术性（无业务发明）
- **工程缺陷**：成立
- **冻结 ID**：GEN-015
- **依据**：源码列计数；vitest 未断言表头

---

## 7. 观察（建议修复队列「否」）

1. **授权政策保存权限码 `admission.policy.manage`**：前端 L612 与 origin/main AccessController L127–129 一致。语义属入场域，但是后端已闭环选型，本模块闸不另立不一致。
2. **员工账号页头**「岗位角色和企业微信同步结果」：本页无企微同步 UI；岗位在治理页。PGM-034 无本面强制本页实现。描述夸大，不单独立项。
3. **默认 `organizationId="org-1"`、`targetUsersText="user-1"`**：标识符默认值。用户未改即提交可能打到不存在对象；应用按钮仅禁 `!selected?.id`。观察。
4. **权限码作为主标签**：四样例外「系统内部标识符」。small 中文名在。测试还断言抽屉含 `access.role.assign`。不立冲突。
5. **账号创建只有 staff / outsourced_vendor**：对齐 PGM-033 两条注册路径 + 外包。merchant/supervisor/sales 走登记或其它页。ALLOWED_ACCOUNT_TYPES 六值不强制本页创建下拉全开。
6. **授权申请无最终审批人字段**：HTML「默认总经理」；DTO 可选。范围变更有该字段。不强制冲突。
7. **GrantPolicy 类型未声明 `deadlineWorkingDays`**：运行时 GET 多余键会留在对象里再 POST 回去。观察。
8. **硬编码 `usr_master_authority` + `master-authority`**：只改按钮文案，删权仍走后端。观察。
9. **`AdminDataState empty={false}`**：治理页空表无空态文案。账号/模板页有。观察。
10. **PermissionGate 不包在三页内部**：整页闸在别模块路由壳。本模块提供原语。adminRoutes origin/main：账号 `access.user.view`、模板 `access.permission_template.view`、治理 `access.role.assign`，与页面副资源门控注释一致。
11. **写路径/方法/字段名**（camel→AdminApi snake）与 AccessController、请求记录类对照：users GET/POST、position PUT、scope PUT、role-assignments POST、permission-overrides POST、grant-requests GET/POST/review、grant-policy GET/POST、position-role-templates CRUD/lifecycle、permission-templates CRUD/copy/apply、permissions GET、partner bind/unbind、staff/registrations POST。字段名与记录类一致。partner 体已是 snake（`user_id`），AdminApi 再转仍是 `user_id`。无路径不一致 finding。
12. **BSM-007 模板按左侧菜单勾选**：`groupedPermissions` 从 origin/main `adminRoutes` 派生，本页实现与 HTML 末句一致。
13. **hasPermissions 空 required + loading**：ready 时空数组放行；loading 仍拒绝。fail-safe，与测试一致。
14. **权限模板无存档删除**：BSM-004 点名商户/票种/商品等，未点名模板。岗位模板已实现生命周期。不立项。

---

## 8. 三线记录摘要

- **第一性原理**：三问见 §3。用途成立。实际执行：vitest 38/38 只覆盖夹具路径。未验证前提：三十档闭集、码表完备、wildcard 测试=权限闸、幂等键防双击。
- **顺从式**：16 文件全文覆盖；106 判据逐项适用/无本面见 §4。
- **对抗式**：grilling Q1–Q9 见 §5。反例：lvl_p1、template_type=role、status=applied、双击定岗、无权限按钮未测。

---

## 9. 结论

**FAIL**。

建议 B2 修复队列「是」：GR-M038-001 … GR-M038-011。无添加项。无 HTML 修改申请。无 questions。

发布口径 vitest（origin/main worktree）38/38 绿 **不能** 把模块收成 PASS。

本席未改业务代码、未写 findings/ledger/plan、未留 worktree。一次性 worktree `/tmp/m038-b1-vitest-origin-main` 已 `git worktree remove`（无 `--force`），目录不存在。

---

## 10. 命令与产物（类型 4）

| 命令 | 要点 |
|---|---|
| `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| `git rev-parse HEAD` / `origin/main` | `e456ba96…` / `ebb1eb99…` |
| `git ls-files -z -- <16> \| xargs -0 cat \| wc -l` | 2787 |
| 16 文件 sha256 vs `git show origin/main:` | 逐字节相同 |
| origin/main worktree vitest | 11 files / 38 tests passed, exit 0；worktree 已删 |
| psql `complex_test` SELECT | staff_levels=30；grant type/status CHECK；permission_templates 五条 type=role |
| `git show origin/main:…/AccessController.java` | 门控码与页面 PermissionButton 一致（含 grant-policy 的 admission.policy.manage） |
| `git show origin/main:…/statusLabels.ts` | 无 applied / user_scope_change；未知码原样返回 |


# M038 B2-r1 整批归并复核（Grok 乙席）

- 席位：Grok 固定只读外审席乙（grok-4.6 / xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 正本：`/home/james/complex-management` main 主工作区。
- 提示词：`160-audit-prompt.md` V0.17。已冻结判据不加判据。
- 输入（本席独立实算）：
  - 本文件上方 V0.01 B1 前缀 **43552** bytes，SHA256 `56eec0788741bc116816514112adfadd924790ec8b4854867deefc29e094c8bc`（FAIL：GR-M038-001…011）。
  - `modules/M038-b1-self-raw.md` **仅 V0.01 前 43748 bytes**，SHA256 `be038208d3821d8d66ea2316f2969be75b7b13bb13c4caaeed101c6d59cfb760`（席甲 FAIL：CX-M038-001…005，OBS-1…7）。全文现 45359 bytes（SHA256 `3f5946f523fe7298cc5698ed0b39f6c581c66ffcefd4cb622c1d0360fad2da4f`）在该前缀之后另有追加段；本席按派发禁令 **未把该追加段当证据、未采信其票面**。V0.01 截断点由前缀 SHA 命中确认。
  - `findings.md` V3.17 全文件 SHA256 `10153c406c987ab2becb402100a3c1e08edb68a3a9f77e6cf4a6d23ce0f1582f`；只读顶部「当前 M038 B2-r1 整批候选」段至该节结束。其下历史模块定稿段不作本批差集材料。
- 锁：本席再算 `plan/criteria-lock.md` SHA256 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 B1 独占行同值）。**本段不写新的独占锁哈希字段行。**
- 范围：只审本批归并 / 分类 / 队列 / 最小修复边界，不重新审模块。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写新产物；未对任何数据库执行写语句；未用 MCP 或子代理。
- 本席 B2-r1 票：**PASS**（整批候选同意；001–010 逐 ID 全 PASS；添加 0、不发 2.2；无 HTML 申请、无 questions）。

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M038-001 | 席甲 CX-M038-001①②③＋本席 GR-M038-003、GR-M038-002（同根：后端给的取值页面没有中文；③模板类型 role 席甲 B1 漏） | 冲突 / 是 / 是 | **同意**。含本席未报的开通方式与空意见英文留痕两点，独立核源成立 |
| M038-002 | 本席 GR-M038-004（席甲 B1 漏） | 冲突 / 是 / 是 | **同意** |
| M038-003 | 本席 GR-M038-005（席甲 B1 记 OBS-1，现改判入队） | 冲突 / 是 / 是 | **同意**第二种思路变体「未登记岗位（岗位码）」；**不坚持**不得出现岗位码 |
| M038-004 | 席甲 CX-M038-002（本席 B1 未报） | 冲突 / 是 / 是 | **同意**；独立核源成立 |
| M038-005 | 席甲 CX-M038-003＋本席 GR-M038-006、GR-M038-007（同一组函数） | 技术性 / 是 / 是 | **同意**一标志＋一包装走七个写动作 |
| M038-006 | 席甲 CX-M038-004（本席 B1 未报） | 技术性 / 是 / 是 | **同意**；独立核源成立 |
| M038-007 | 席甲 CX-M038-005＋本席 GR-M038-011 | 技术性 / 是 / 是 | **同意** |
| M038-008 | 本席 GR-M038-001（席甲 B1 记 OBS-2 职级一项，现改判入队） | 细化 / 是 / 是 | **同意**页内生成 30 档、不调 staff-levels |
| M038-009 | 本席 GR-M038-009＋席甲 OBS-7 | 细化 / 是 / 是 | **同意**两处下拉改用 formatBusinessStatus |
| M038-010 | 本席 GR-M038-008、GR-M038-010（席甲 B1 未报） | 细化 / 是 / 是 | **同意**两处缺测合并 |
| （无新 ID） | 两席其余观察 | 只登记不入队 | **同意；无漏并、无应升队项** |

冲突入队 4（001–004）。技术性入队 3（005–007）。细化入队 3（008–010）。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 10。本模块写入集合＝本模块 16 个文件之内（三个页面、`security/PermissionGate.test.ts` 与各页面测试），不改公共字典、客户端与后端。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**一、M038-001（冲突）。票：同意。GR-003＋GR-002 与 CX-001 同根；本席未报两点独立成立；七个中文均是取值直译／同义，不须操作员定名。**

独立核源：

1. 授权申请状态／类型。`AccessGovernancePage.tsx` L84-88 `statusBadge` 走 `formatBusinessStatus`；L572 类型列同；L374／L393 结果提示同。`apps/web/src/customer/i18n/statusLabels.ts`：`pending`＝待处理、`rejected`＝已驳回、`temporary_permission`／`user_role`／`user_position` 有中文；**无** `applied`、`role_permission`、`user_scope_change`、`position_high_risk_release`；未知码 `return code`（L255-259）。CHECK 闭集本席 B1 已记：状态 pending／applied／rejected，类型六种。
2. 开通方式。`AccessUsersPage.tsx` L182 `formatBusinessStatus(row.createdVia)`。字典无 `manual_admin`／`legacy`。`AccessService.java` L1222 后台建外包账号写入 `"manual_admin"`；`AccessUser.java` L35 缺省构造器 `created_via="legacy"`。
3. 模板类型。`PermissionTemplatesPage.tsx` L18-21 页内映射只有 base／temporary／copy，L21 未知则原样返回；L375-378、L495-498 两个下拉同样三值。`/home/james/audit-tmp/m038_verify.log`：`template_type: role×5`。席甲 B1 漏此项，并入同根正确。
4. 空意见英文留痕。`AccessGovernancePage.tsx` L386-390 `comment`／`operatorNote` 在 `reviewForm.comment.trim()` 为空时回落 `decision`（`"approve"`／`"reject"`）。同页 L576-577 按钮已写「通过」「驳回」，L393 成功句已用「通过／驳回」。本席 B1 漏报，事实成立。
5. 中文用词（问 2.2 的门槛＝是否业务发明）。逐项：

| 取值 | 候选中文 | 本席核 | 须 2.2？ |
|---|---|---|---|
| `applied` | 已生效 | 本页 L526「审批通过后方可生效」；字典 `approved`＝已通过不能占用此码 | 否 |
| `role_permission` | 角色权限 | PGM-041 原文「最终权限 = **角色权限** + 基础模板 + …」 | 否 |
| `user_scope_change` | 人员范围变更 | 本页 L515 标题原文「人员范围变更（所属商户 / 跨项目）」 | 否 |
| `position_high_risk_release` | 岗位高危权限释放 | 取值拆词；PGM-041「既有**高危权限**」不得因在模板内免审 | 否 |
| `manual_admin` | 后台手动录入 | 对应 AccessService 后台建号写入该码，开通方式＝怎么开的 | 否 |
| `legacy` | 历史账号 | 对应缺省构造／库内旧行的开通来源，不是新账号类型 | 否 |
| 模板 `role` | 角色模板 | PGM-041「支持**角色级权限模板**」；与页内「基础模板／临时模板／复制模板」并列 | 否 |

无一须操作员定名。不发 questions。默认审批意见改「通过／驳回」沿用本页按钮原文，不是发明。
6. 分类。冲突（PGM-066／L287 枚举取值必须中文，四样例外不含枚举）。公共字典属别的模块不动、沿用本模块已有页内小映射（L17-18 注释已说明 `base` 进全局会串成「普通会员」）＝最小且不扩范围。
7. 边界。只改本模块三页＋测试。映射须覆盖后端闭集全部取值（状态三种、类型六种、开通两种、模板类型补 role）。下拉 value 仍走原码。

**明确同意** 001 冲突／是／是。

**二、M038-002（GR-004，冲突）。票：同意。**

独立核源：给人看的字段名／占位／aria-label／校验句含英文 id／ID，确认点包括：治理页「用户 id」「职级 id」「变更用户 id」「新所属项目 id」「新所属商户 id」「新范围职级 id」「装配用户 id」「装配角色 id」「覆盖用户 id」「角色 id」「权限模板 id」及申请职级占位「职级 id」；账号页「组织 ID」占位、L322「组织 ID 不能为空…」、L329「附件 ID，逗号分隔」；模板页 L413「目标员工 ID」、L315「至少输入一个目标员工 ID。」。L287 明列「字段名」「提示、报错」；四样例外是**取值**不是字段名。`data-testid` 不含给人看的英文。本模块测试按旧 aria-label 取元素目前确认至少 `AccessGovernancePage.test.tsx` L98 `getByLabelText("覆盖用户 id")`；随改该处（及若有按旧校验句取元素的）。分类冲突正确。

**明确同意**。

**三、M038-003（GR-005，冲突）。票：同意「未登记岗位（岗位码）」；不坚持不得出现岗位码。**

独立核源：`zhPosition` L50-52 `POSITION_LABELS[code] ?? code`，六项之外原样英文码上屏于岗位列／抽屉／授权申请／智能总结。本页把该列当给人看的文字（L116 注释；`AccessGovernancePage.test.tsx` L116 禁止抽屉出现 `ops_manager`）。引导测试提交 `front_desk`（无映射）。无岗位字典表。

本席 B1 给过两种思路：中文选择，或未映射显示「未识别岗位」而非英文码。候选取第二种变体：中文说明在前、岗位码作系统内部标识符放括号里，与本页 `roleLabel` L80-81「名称 (编号)」同一写法。PGM-066 四样例外允许系统内部标识符出现；括号里的码不是字段名。不新造岗位字典、不改输入方式——HTML 未规定岗位须下拉（席甲 OBS-2，候选只登记段已收）。比「未识别岗位」无码更能区分行。

**明确同意。不坚持不得出现岗位码。** 不发 2.2。

**四、M038-004（CX-002，冲突）。票：同意。本席 B1 漏报，独立核实成立。**

独立核源：L487 恢复按钮固定写「恢复为停用模板」。出现条件：已存档分支且不是「未删除且待删审批中」（即已存档未删除无删单，或已删除）。BSM-004 原文（锁 L2372）：存档「想恢复点"取消存档"即可」；删除项「可恢复」。后端 M031-001 已发布：restore 后回到 `active`、立刻可定岗——按钮声称「停用」与实际相反，属 GEN-016。处置按状态分两种文案：已存档未删除→「取消存档」，已删除→「恢复」；补测试且不再出现「停用模板」。分类冲突正确。

**明确同意**。

**五、M038-005（GR-006＋GR-007＋CX-003，技术性）。票：同意。同一组函数；一标志＋一包装走七个写动作。**

独立核源：无 try/catch 的五个：`assignPosition` L265-280、`changeUserScope` L284-309、`roleAssignment` L311-324、`userPermissionOverride` L328-343、`reviewGrant` L386-395；`onClick={() => void fn()}`，`PermissionButton` L76-79 只拦无权限、不拦 Promise 拒绝。另两个有 catch 但无提交中态：`saveGrantPolicy` L236-245、`changePositionLifecycle` L247-256。引导保存（`applyGrantRequest`／`savePositionTemplate`）已有 submitting，不在这七个里。`adminClient.ts` 每次请求新 `nextIdempotencyKey`，双击＝两笔。同页对照成立。

合并正确：CX-003 管失败不可见，GR-007 管双击；七个函数同一组。最小处置一页内提交中标志＋一包装（置忙、执行、失败 `setGlobalError(后端中文消息，缺省各动作中文兜底)`、复位），七个走它，提交中相应按钮 `disabled`。补测：装配角色与审批各打桩一次 403 断言提示；提交未完成再点不发第二笔。分类技术性（无业务发明）。上位 BSM-005「哪一步填得不对当步就提示」只作旁证。

**明确同意**。

**六、M038-006（CX-004，技术性）。票：同意。本席 B1 漏报，独立核实成立。**

独立核源：`toDateTimeLocal` L42-46 `date.toISOString().slice(0, 16)` 取 UTC 数字填进按本地时区解释的 `datetime-local`；保存 `toIsoOrUndefined` L49-53 `new Date(value).toISOString()` 再按本地解释。北京时间下库内 `2026-06-14T00:00:00Z` → 框 `2026-06-14T00:00` → 发出 `2026-06-13T16:00:00.000Z`（B1 可执行日志 `/home/james/audit-tmp/m038_b1_run.log` L83；本席按源码时区规则复算一致）。

后端 `AccessService.updatePermissionTemplate` L156-167：`widensWindow`＝生效时间提前（`validFrom` 变早或清空）或截止时间推后即 409 `TEMPLATE_REAUTHORIZATION_REQUIRED`。带生效时间的模板只改名称也会因往返把 `validFrom` 提前而 409；只有截止时间时往返把截止提前（不是 `isAfter`），放行但每次早 8 小时。GEN-006／PGM-041「临时授权可设有效期」。处置：`toDateTimeLocal` 按本地时区取年月日时分，使「读出→不改→保存」同一时刻；测试内时区 `Asia/Shanghai`，选中带生效／截止的模板直接保存，断言请求体两时间与原值同一时刻。不改后端放宽判断。

**明确同意**。

**七、M038-007（表头，技术性）。票：同意。**

独立核源：L620 `thead` 七列（建议、详情、详情）；L622／L623 每行六格。首次打开「③ 智能总结」即见。删重复 `<th>详情</th>`；补测试表头列数＝首行单元格数。分类技术性。

**明确同意**。

**八、M038-008（GR-001，细化）。票：同意页内 30 档闭集生成，不调 staff-levels。**

独立核源：四处职级自由文本——定岗 L508、范围变更 L521、岗位模板弹窗 L655、授权申请弹窗 L716。引导测试提交 `lvl_p1`（`AccessGovernancePage.guidedcreate.test.tsx` L89／L97）。本页 **0 处** `staff-levels`。BEQR-149 原文「M1–M10，每级分上 / 中 / 下（共 30 档）」；`m038_verify.log` 30 个 id 恰为 `lvl_m`+1..10+`_lower|_middle|_upper`。`zhLevel` L54-58 已按同一正则显示「M1·下…M10·上」。

本席 B1 拟拉 GET `/staff-levels`。候选改为页内生成闭集、范围变更另留「沿用原职级」空选项（现占位「沿用可空」）。同意理由：①HTML／CHECK／种子即这 30 个，不是开放目录；②本页写闸是 `access.role.assign`，拉职级列表要 `access.staff_level.view`，会新增权限依赖与失败面；③比本席 B1 拟法更小。分类细化（HTML 未写死必须下拉，停在细化）。随改测试里填非闭集职级的几处（至少 guidedcreate 的 `lvl_p1`）。

**明确同意**。

**九、M038-009（GR-009＋OBS-7，细化）。票：同意同根合并。**

独立核源：账号页 L340-343 下拉「启用／待激活／禁用」，表格 L284 经字典「有效／待激活／已禁用」（`active`＝有效、`disabled`＝已禁用、`invited`＝待激活）。治理页岗位模板弹窗 L664 与确认摘要 L674「启用／停用」，表／抽屉经字典「有效／未激活」（`inactive`＝未激活）。同一码两套中文。处置：两处下拉与确认摘要改 `formatBusinessStatus(取值)`，公共字典不动。分类细化。

**明确同意**。

**十、M038-010（GR-008＋GR-010，细化）。票：同意两处缺测合并。**

独立核源：①`PermissionGate.test.ts` 只测纯函数 `hasPermissions`（7 例），无 `Can`／`PermissionButton`／`PermissionGate` 渲染；页面测试全部 `wildcard: true`。`PermissionButton` L76-79 无权限须 disabled 且 onClick 直接 return；`PermissionGate` L29-40 加载中「权限加载中…」、不足「权限不足：…」。②本模块 16 文件内：`AccessGovernancePage.guidedcreate.test.tsx` 只覆盖岗位模板；无 POST `/grant-requests` body 断言。同页范围变更／覆盖有写测。

非阻断（不否决）：仓内 `apps/web/src/admin/pages/AccessGovernanceGrantRequest.guidedcreate.test.tsx`（属 **M080** FILES，不在本模块 16 文件）已覆盖「入口→权限码／过期→POST grant-requests」。本模块切片仍缺自己的锁。B3 把②补进**本模块已有** guidedcreate 测试即可，**不要新建**与 M080 同名的第二份文件。

分类细化（GEN-003 本聚合关键分支）。①补组件渲染用例进 `PermissionGate.test.ts`（若用 JSX 可改扩展名，属实施）；②治理页引导补提交并断言请求体。

**明确同意**。

---

### 八、其余观察只登记

候选「只登记不入队」已覆盖两席合取：

- 岗位编码、所属组织、目标员工、权限模板等处手填内部编号（HTML 未规定须下拉；职级因是 HTML 闭集已入 008）。＝席甲 OBS-2 去掉职级后的残余；本席 OBS-3 默认 `org-1`／`user-1` 仍在 §7。
- `hasPermissions` 的 wildcard 分支（席甲 OBS-3；后端恒 false）。
- 员工账号页标题「企业微信同步结果」「禁用」而页内无此内容（席甲 OBS-4＝本席 OBS-2）。
- 速查表「权限状态」五态、后端无改状态接口（席甲 OBS-5）。
- 若干文件无「文件版本」行（席甲 OBS-6）。
- 本席 §7 其余：授权政策权限码与后端一致、权限码作主标签（四样例外）、账号创建只有两条注册路径、授权申请无最终审批人字段、GrantPolicy 未声明 `deadlineWorkingDays`、硬编码 master-authority、治理页空表无空态、PermissionGate 不包在三页内部、路径／字段与控制器一致、`groupedPermissions` 按菜单、空 required 放行、权限模板无存档删除（BSM-004 未点名模板）。

席甲 OBS-1→003、OBS-2 职级→008、OBS-7→009，改判入队正确。无漏并、无应升队项。不为理论路径补需求。

---

### 九、添加／HTML／questions

无添加。无 HTML 修改申请。`docs/audit-2026-09/questions/` 无 M038 文件。001 七个中文均直译／同义，**不发 2.2**。不 BLOCKED-ASK。候选表无添加行。

---

### 三、最小修复边界

一次 B3 允许的写入集合＝本模块 16 个文件之内（`AccessGovernancePage.tsx`、`AccessUsersPage.tsx`、`PermissionTemplatesPage.tsx`、`security/PermissionGate.test.ts`、`security/Can.tsx` 若 010 渲染用例需要、以及各页面测试）。禁止改公共字典 `statusLabels.ts`、`adminClient.ts`、后端、OpenAPI、迁移、三 HTML、锁。

| ID | 允许（本模块 B3） | 禁止 |
|---|---|---|
| 001 | 三页页内小映射覆盖后端闭集：授权申请状态／类型、开通方式、模板类型补 `role`（映射＋两个下拉）；空意见默认「通过／驳回」。补测试覆盖全部取值 | 改 `statusLabels.ts`；新造业务类型；把申请弹窗下拉扩成后端另三种由服务生成的类型 |
| 002 | 字段名／占位／aria-label／校验句里的 id／ID 改成「编号」或去掉；`data-testid` 不动；随改按旧 aria-label 取元素的测试（至少「覆盖用户 id」） | 改 testid；改标识符**取值**的显示策略（那是 003／四样例外） |
| 003 | `zhPosition` 未映射显示「未登记岗位（岗位码）」；补测试 | 新造岗位字典；改岗位输入方式；去掉括号里的码 |
| 004 | 已存档未删除按钮「取消存档」、已删除「恢复」；补测试且不再出现「停用模板」 | 改后端 restore；把已删除也写成「取消存档」 |
| 005 | 页内一个提交中标志＋一个包装函数，七个写动作走它（置忙、执行、失败提示、复位），提交中按钮禁用；补 403 提示与双击不发第二笔 | 改 `adminClient` 幂等键策略；把已有 submitting 的引导保存再包一层当新能力；改后端 |
| 006 | `toDateTimeLocal` 按本地时区取年月日时分；补 Asia/Shanghai 往返断言 | 改 `updatePermissionTemplate` 放宽有效期判断；改服务器时区 |
| 007 | 删重复表头「详情」；补列数断言 | 改总结接口 |
| 008 | 四处改为下拉，选项页内按 30 档闭集生成、显示走 `zhLevel`；范围变更另留空选项「沿用原职级」；随改 `lvl_p1` 等非闭集夹具 | 调用 `/staff-levels`；新增 `access.staff_level.view` 依赖；改后端 CHECK／种子 |
| 009 | 两处下拉与确认摘要改 `formatBusinessStatus(取值)` | 改公共字典键值 |
| 010 | `PermissionGate.test.ts` 补组件渲染（无权限按钮禁用且点击不触发、`Can` fallback、Gate 加载中／权限不足文案）；本模块治理页引导测试补授权申请提交并断言请求体 | 新建与 M080 同路径同名的第二份 `AccessGovernanceGrantRequest.guidedcreate.test.tsx`；改生产闸语义 |

B3 实施非否决提示（不改变本票）：010② 仓内 M080 已有同路径测试，本模块补进已有 `AccessGovernancePage.guidedcreate.test.tsx` 即可。010 若用 JSX，测试文件扩展名可改为 `.tsx`。008 选项生成须与 `zhLevel` 正则及 `m038_verify.log` 30 id 逐键相同。

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止开工 B3。一次 B3 只改上表写入集合。

- 001：冲突／是／是。同根合并；七个中文直译，不发 2.2。
- 002：冲突／是／是。
- 003：冲突／是／是。同意括号里保留岗位码。
- 004：冲突／是／是。本席 B1 漏，核源成立。
- 005：技术性／是／是。七个写动作一包装。
- 006：技术性／是／是。本席 B1 漏，核源成立。
- 007：技术性／是／是。
- 008：细化／是／是。同意页内 30 档、不拉接口。
- 009：细化／是／是。OBS-7 并入。
- 010：细化／是／是。两处缺测；②不要新建 M080 同名文件。

### 结论

全批差集无原意改写、无漏并、无应升队观察。CX-001＋GR-003＋GR-002→001；GR-004→002；GR-005→003；CX-002→004；CX-003＋GR-006＋GR-007→005；CX-004→006；CX-005＋GR-011→007；GR-001→008；GR-009＋OBS-7→009；GR-008＋GR-010→010。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS、005 PASS、006 PASS、007 PASS、008 PASS、009 PASS、010 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M038 IN_PROGRESS；B3=0。

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **43552** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）

# M038 B2-r2 候选复核（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 前缀 **43552** 字节 + V0.02 B2-r1 追加，合计前缀 **65479** 字节 SHA256 `0d09b4c1035d80616349f4120305a902a7052a87fed1697a01e318914e01872d` 原样保留，未改写、未覆盖。本段不写独占锁哈希字段行）
- 席位：固定 Grok 只读外审席乙（grok-4.6 / xhigh）。步骤：B2-r2 候选复核。不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3/B4。
- 正本：`/home/james/complex-management` main 主工作区。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `ebb1eb99c610df330c07d24278798c2d3611299a`（未 fetch）。`test -f .git/MERGE_HEAD` → NO_MERGE。B3 累计 1；本批定稿后 B3-r2 为第 2 次。无源修复。
- 提示词：`160-audit-prompt.md` V0.17。已冻结判据不加判据。本席 FAIL 具否决权。
- 范围：只审本条（M038-011＝本席 B5-r1 GR-M038-012）的归并 / 分类 / 队列 / 最小修复边界，不重新审模块。001…010 不回潮。
- 独立性：输入仅为两份已封存 B5-r1 完整 raw + findings 顶部候选。本席 **未打开** `modules/M038-b1-self-raw.md` 若已存在的 V0.03 B2-r2 追加段，不采信其票面。未用 MCP / 子代理 / 第三席；不问操作员。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写本指定路径以外的任何新产物；未对任何数据库执行写语句。
- 锁：本席再算 `plan/criteria-lock.md` SHA256 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与文首独占行、sidecar 内容、B5 四值同值）。**本段不写新的独占 CRITERIA-LOCK-SHA256 行。**
- 本席 B2-r2 票：**PASS**（整批候选同意；011 逐项 PASS；001…010 不动 PASS；§8 第 15 条只登记 PASS；添加 0、无 HTML 申请、无 questions）。

此票只判断 findings.md V3.20 顶部「当前 M038 B5-r1 合取 FAIL，B2-r2 候选」相对本次两份完整 B5-r1 raw 的差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.02 B2-r1 **PASS**、B5-r1 席乙 **FAIL** 原文，亦不以本票代替 B5。不加判据。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M038-b5-r1-grok-raw.md`（本席 B5-r1 票 FAIL：GR-M038-012） | V0.01 | 42963 | `3094ede246bee6f1aa7a8ed827ccdb5550bcf5a80723d0e383d22d17714b2f33` |
| `docs/audit-2026-09/modules/M038-b5-r1-self-raw.md`（席甲 B5-r1 票 PASS，被本席否决） | V0.01 | 34574 | `4dcfe0ecc5ca6dc23b780cfc79c2b231a8f10dd32252dff267d3e573df468fc0` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M038 B5-r1 合取 FAIL，B2-r2 候选」 | V3.20 | 1181316（全文件） | `dbb1dcb4a6d36cd6d802796c4499c565ea2deaee2405e03b22beba9288b9d1db` |
| 本文件前缀（本席 B1+B2-r1，本段追加前） | V0.02 | 65479 | `0d09b4c1035d80616349f4120305a902a7052a87fed1697a01e318914e01872d` |

四输入字节/SHA 与派发清单全等。本文件独占锁哈希字段行仍仅文首一处，本段未新增。本模块 16 文件工作区 SHA256 与 B5 after tree `9df1fad44caa2ef615993fe970d3efc4620aca25` 仍逐字节相同（点核治理页 / 账号页 / 模板页 / 模板测试 / Can / PermissionGate）。工作区 `git ls-files -z -- <16> | xargs -0 cat | wc -l` = **3210**。

controller / runbook / modules.md 只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| 治理地图 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 业务地图 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 枚举速查 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

`docs/audit-2026-09/questions/` 无 M038 文件。ledger 只读：M038 `IN_PROGRESS`，B3 列 1。本席未改台账。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| graft | 用户本轮禁 MCP/子代理，且 B2 不重审模块；本节点直接点读三页写入口与 after tree 指纹，**未跑** graft CLI |
| receiving-code-review | 先核源码写入口 + 两份 B5-r1 raw + 候选表，再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：同根是否只剩复制/应用、处置是否扩到 HighRiskConfirm/客户端、005 的 403 是否被改立项、001…010 有无回潮 |
| verification-before-completion | 声称前先跑只读哈希、after tree 对照、三页 `api.post/put` 与按钮 disabled 点读；本票不声称 vitest 本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照 GR-M038-012 与候选；不改生产代码 |
| codebase-design / ponytail-review | 最小是页内互斥包装 + 两条会红的连点测试；不发明「复制必须二次确认」、不改共享确认组件 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

---

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M038-011 | 本席 B5-r1 GR-M038-012（席甲 B5-r1 票 PASS 被推翻：其 §3「账号页、模板页的写动作原本就各有 try/catch 与置忙或二次确认」对「复制模板」不成立） | 技术性 / 是 / 是 | **同意**。独立核源成立；分类与最小处置采本席 B5，且把「或 submitting 或 ref」收成与 005 `runWrite` 同构的 ref+submitting，两条测试锁住复制与应用两个入口 |
| （无新 ID） | 本席 B5-r1 §8 第 1–16 条观察 | 只登记不入队 | **同意**。第 15 条维持不单独立项（见第四节） |
| M038-001…010 | 已修，B3-r1；B4 双 PASS；B5 无回归 | 否（已修） | **同意不动、不回潮、不重开 005** |

漏并：无。改写原意：无。第二条可并队列项：无。新 ID 仅 011，符合「仅给无 ID 条目分配新 ID」（001…010 已否，不能沿用旧 ID 回 B3）。无本批添加项、无 HTML 申请、无 questions。

**整批候选：同意。**

---

### 一、同根复查其余写按钮（问一）

**票：同意。只剩复制与应用两处。无漏。**

本席 B5-r1 Q4 / GR-M038-012 已把「无提交中态、连点即两笔」定为缺陷，并把治理页七动作 + 两处引导 submitting + 账号 create submitting + bindBusy + 模板页保存修改 算「已闸」。候选称席甲按同根复查后只剩这两处。本席不采信席甲 B5 原 PASS 结论句，按下表独立点读 after tree 源码：

| 面 | 写入口 | 互斥现态 | 是否剩余 |
|---|---|---|---|
| 治理页 | `runWrite` 调用点 **14** 处：存档 / 申请删除 / 批准删除 / 驳回删除 / 恢复（取消存档）、定岗、范围变更、装配角色、卸载角色、覆盖设置、覆盖移除、审批通过、审批驳回、保存授权政策 | `writeBusyRef` 同步挡 + `writeBusy` 让按钮 `disabled`（审批两键走 `RowActions` 无 disabled 槽，B5 已登记，仍走同一 `runWrite`） | 否 |
| 治理页 | 引导弹窗 `savePositionTemplate`（`templateSaving`）、`applyGrantRequest`（`grantSaving`） | `GuidedFlowModal` `disabled={submitting}` / `loading={submitting}` | 否 |
| 账号页 | `createUser`（可再 POST 一次模板 `/apply`） | `setSubmitting(true)`，弹窗 `submitting={submitting}` | 否（创建流上的套用，不是模板页应用按钮） |
| 账号页 | `bindPartnerOrg` / `unbindPartnerOrg` | `bindBusy`，两按钮 `disabled={bindBusy}` | 否 |
| 模板页 | `saveTemplate` PUT | `setSubmitting(true)`，保存按钮 `disabled={submitting \|\| !selected?.id}` | 否 |
| 模板页 | `createTemplate` POST | `setCreateSaving(true)`，弹窗 `submitting={createSaving}` | 否 |
| 模板页 | `copyTemplate` POST `.../copy` | **无 submitting、无 busy ref**；按钮 `disabled={!selected?.id}`；`onClick={() => void copyTemplate()}` | **是** |
| 模板页 | `applyTemplate` POST `.../apply` | **无 submitting、无 busy ref**；`HighRiskConfirm` `disabled={!selected?.id}`；`onConfirm={() => void applyTemplate()}`。二次确认不是互斥：`window.confirm` 两次均为真则两笔 | **是** |

`HighRiskConfirm` / `GuidedFlowModal` 不在本模块 16 文件。`Can.tsx` `PermissionButton` 的 onClick 只拦无权限、**不看** `disabled`——故仅靠 `disabled={submitting}` 挡不住 Testing Library `fireEvent.click` 连点，必须有同步 ref（005 已证明）。复制/应用是本模块仅剩「按钮保持可点且函数无闸」的写入口。

非阻断、不升队：保存修改 / 新建 / 账号创建 / 绑定解绑仍是 setState 置忙、无 ref。本席 B5 Q4 已把它们算「已闸」（对比的是「完全无忙态」）。本批不把 setState 竞态升成第二条。

**明确同意问一。无漏的。**

---

### 二、M038-011 分类与最小处置（问二）

**票：同意。边界合适。**

独立核源（类型 4，工作区＝after tree，与候选一致）：

- `PermissionTemplatesPage.tsx` L79 `submitting` 只服务保存修改。
- L283–309 `copyTemplate`：校验后直接 `api.post(.../copy)`，无 `setSubmitting`、无 ref。L407–415「复制模板」`disabled={!selected?.id}`。
- L324–341 `applyTemplate`：同样无忙态。L419–428 `HighRiskConfirm` `disabled={!selected?.id}`，`onConfirm={() => void applyTemplate()}`。
- 对照 L391–399「保存修改」`disabled={submitting || !selected?.id}`，`saveTemplate` 会 `setSubmitting(true)`。
- `HighRiskConfirm.tsx`（非本模块 FILES）：`handleClick` 不读 `disabled`，只 `precheck` + `window.confirm` 后调 `onConfirm`。不改它则互斥必须落在 `applyTemplate` 侧。
- 默认 `copyName="普通员工01"`、`targetUsersText="user-1"`：选中一行即可连点，不是理论路径。
- 客户端每次请求新幂等键（B5 已记）；页面不闸则后端建两份副本 / 赋权两笔。不改客户端。

**正常触发：** 打开权限模板页 → 选中一行 → 连点两次「复制模板」→ 两笔 POST `/copy`。应用：点两次「应用模板」且 `window.confirm` 两次均为真 → 两笔 POST `/apply`。

**四分类（冲突 → 细化 → 技术性 → 添加）：**

1. 冲突？**否。** 三 HTML 未写「复制必须幂等」，也没有「允许连点各建一份」的相反明文。
2. 细化？**否。** 不能指到某一 HTML 条目要求「复制按钮防双击」作为下位细节；BSM-005 管的是新建引导。
3. **技术性：是。** 无业务发明（无新实体 / 枚举 / 流程）。纯 GEN-007 并发与幂等 / GEN-002 写路径声称与行为不一致。
4. 添加？不进入。

工程缺陷：**是**。队列：**是**（本模块模板页 + 其测试可修）。不重开 005（005 定稿写死的是治理页七个写动作走 `runWrite`，已否；本条是另一页写按钮）。

**最小处置核实（候选原文，本席独立裁定合适）：**

1. 模板页加一个与 `runWrite` 同构的互斥包装：`ref` 同步挡并发、沿用现有 `submitting` 让按钮禁用。比本席 B5「submitting **或** ref」更紧——005 的连点测试是两下 `fireEvent.click` 发生在 re-render 之前，且 `PermissionButton` / `HighRiskConfirm` 的 onClick 都不读 `disabled`，只设 submitting 不够。同构包装是最小完整，不是扩范围。
2. 复制与应用走它。两个入口同一缺陷，必须都进包装，否则只修复制、应用仍两笔。
3. 两个按钮的 `disabled` 并入 `submitting`（复制：`disabled={submitting || !selected?.id}`；应用：`HighRiskConfirm` 同样并入）。提交中交叉禁用，避免复制进行中再点应用。
4. `PermissionTemplatesPage.test.tsx` 补两条：①连点两次「复制模板」只发一笔且提交中两个按钮禁用；②连点两次「应用模板」并都确认只发一笔。本席 B5 原拟只补复制一条；候选补应用一条是锁住第二入口，属同一条的最小测试面，不是新 finding。
5. 不改 `HighRiskConfirm`、公共字典、客户端与后端。`HighRiskConfirm` 不在 16 文件；改它会把共享确认组件卷进本模块，且仍挡不住已弹出的两次 confirm。正确互斥点是 `applyTemplate` 包装。

写入集合＝`PermissionTemplatesPage.tsx` + `PermissionTemplatesPage.test.tsx`。不改 `PermissionTemplatesPage.guidedcreate.test.tsx`（新建已有 `createSaving`）。不改治理页 / 账号页 / Can / PermissionGate。

**允许（非要求，不构成否决）：** 保存修改也可走同一包装（共享 `submitting`）。**禁止：** 把新建引导、账号页、治理页再改一轮；改 `HighRiskConfirm` / `adminClient.ts` / `statusLabels.ts` / 后端 / 三 HTML / 锁；给复制加二次确认；把 011 并回 005。

**B3 非阻断实施注（不否决）：**

- 包装必须 `try/finally` 释放 ref 与 `submitting`（与 `runWrite` L116–128 同构）；`copyTemplate` 校验失败提前 return 不得把闸卡死。
- 连点测试须挂起首笔请求再点第二下（005「提交未完成时再次点击不发第二笔」先例），否则无法断言提交中禁用。
- 应用用例须 `vi.spyOn(window, "confirm").mockReturnValue(true)`（组件用 `window.confirm`）。应用条未强制断言两按钮禁用——复制条已锁交叉禁用，不否决。
- 计数只核 `POST .../copy` 与 `POST .../apply`，不要把列表 GET 算进去。
- 须先选中一行（`permission-template-select-tpl-1`）；复制名称默认已有。

**明确同意问二。边界合适。**

---

### 三、B5-r1 §8 第 15 条（问三）

**票：确认。只登记，不单独立项。**

本席 B5-r1 §8 第 15 条原文：「005 定稿写『装配角色与审批各打桩一次 403』，当前只有装配角色 403；该断言是 `stringMatching(/\S/)`，不能区分后端中文原句与兜底句。runWrite 代码路径已被装配角色 403 + 防双击覆盖，不为同一包装函数缺第二条同等测试单独立项。」

当场复读 `AccessGovernancePage.test.tsx`：L274–284 仅「装配角色被后端拒绝时页面报出原因（005）」，断言 `stringMatching(/\S/)`；L286–297 防双击。无审批 403 单测。候选「其余观察（§8 第 1–16 条）维持只登记」且点名第 15 条按本席「不单独立项」处理——与原文一致，未改写成新 ID、未并进 011、未重开 005。

**明确确认问三。**

---

### 四、添加／HTML／questions（问四）

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 M038 文件。不 BLOCKED-ASK。011 无业务发明，不发 2.2。不按铁律 4 申请改三 HTML。候选表无添加行。

**明确同意问四。**

---

### 五、001…010 不动

| ID | 本批候选 | 本席 |
|---|---|---|
| 001…010 | 否（已修，B3-r1） | **同意不动、不回潮。** 本席 B5-r1 §6 已逐条复验落地。011 不重开 005。不把 001…010 改回「是」。 |

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r2 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止开工 B3。一次 B3-r2 只改下表写入集合（第 2 次 B3）。

- 011：技术性／是／是。模板页加与 `runWrite` 同构的互斥包装（ref + 沿用 `submitting`）；复制与应用走它；两按钮 `disabled` 并入 `submitting`；`PermissionTemplatesPage.test.tsx` 补两条连点测试。不改 HighRiskConfirm、公共字典、客户端与后端。
- 001…010：否（已修）。不动。
- §8 第 15 条及其余 B5 观察：只登记。

| 允许（本模块 B3-r2） | 禁止 |
|---|---|
| `PermissionTemplatesPage.tsx` 加包装、复制/应用走它、两按钮 disabled 并入 submitting；可选把保存修改也走同一包装 | 改 HighRiskConfirm / GuidedFlowModal / adminClient / statusLabels / 后端 / OpenAPI / 迁移 / 三 HTML / 锁 |
| `PermissionTemplatesPage.test.tsx` 补复制连点（一笔 + 提交中两按钮禁用）与应用连点都确认（一笔） | 新建测试文件；改 guidedcreate 测试当本条主锁；把 011 并回 005；给复制加二次确认 |

### 结论

全批差集无漏并、无原意改写、无第二条可并。M038-011＝GR-M038-012，技术性／是／是，最小处置合适。问一同意只剩复制与应用、无漏。问二边界合适。问三确认第 15 条只登记。问四添加 0、HTML 申请 0、questions 0。001…010 不动。逐项：差集 PASS、011 PASS、第 15 条只登记 PASS、001…010 不动 PASS。本席 B2-r2 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M038 IN_PROGRESS；B3 累计 1，定稿后 B3-r2 为第 2 次。

- 文件自身版本：V0.03（本段追加；上方 V0.01+V0.02 前缀 **65479** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M038 B1 现锁重开（席乙 Grok）V0.04

- 席位：只读独立审计席乙（Grok 4.6 / xhigh）
- 步骤：160-audit-prompt.md V0.17 阶段 B · M038 按现行锁 V0.14 从 B1 重开
- 正本：`/home/james/complex-management` main 主工作区 HEAD `e456ba96`；origin/main `53dec141`
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M038（16 文件，计划冻结有效内容 2787 行；现状实算 3210 行）
- 锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.14；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` = `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（与 sidecar、ledger 顶部同行）
- LOCK-AMEND-COUNT：1（只读；本席不改 ledger）
- 映射：106 项（与锁内 M038 行一致，映射行与 general 未变）
- 定义正文变更且在本映射内、须按现行正文重审：BSM-114、BSM-120、PGM-043、PGM-059
- 旧锁 raw（本文件 V0.01–V0.03）只作历史可读，票不复用
- 既有输入：旧锁定稿 M038-011 尚未修、B1 期间不修；001–010 已落地在主工作区 8 个未提交文件（与 `/home/james/audit-tmp/m038_b3r1.diff` 逐字节相同）
- 三 HTML 只读：BEQR 文件头 V0.07；业务地图文件头 V0.14；治理地图文件头 **V0.13**（本席实读 `docs/platform-governance-map-for-operations-v0.1.html:2`，不沿用「V0.12」口头口径）
- 本席未读 `M038-b1-self-raw.md` 任何本次新增段，不依赖席甲发现

## 1. 环境实用强制技能（本席自核，铁律 3）

本席环境路径与 runbook 所记 Codex Windows 路径不同，按本机实有文件记录。

| 技能 | 本机路径 | 本轮 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 已读；用户指令禁止子代理/MCP，优先于技能本体派席 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 已读；Arc 默认派评审子代理作废 |
| graft | `~/.claude/skills/graft/SKILL.md` | 已调用 `graft ask`（copy/apply 落点） |
| grilling | mattpocock `skills/productivity/grilling/SKILL.md` | 已读；只借追问结构自问自答；等用户/派子代理作废 |
| grill-me | 同套件 `skills/productivity/grill-me/SKILL.md`（`disable-model-invocation: true`，转 grilling） | 存在；未另起会话 |
| batch-grill-me | — | **缺口**（本机无此技能目录） |
| ubiquitous-language | — | **缺口** |
| git-guardrails-claude-code | mattpocock `skills/misc/git-guardrails-claude-code/` | 存在；本轮无 git 写，未动钩子 |
| systematic-debugging | superpowers | 已读；只借读错误/复现/追数据流；禁止加诊断埋点、禁止改码 |
| diagnosing-bugs | mattpocock engineering | 已读；禁止 instrumentation / HITL |
| codebase-design | mattpocock engineering | 已读；PermissionGate/Can 为深模块，页面是适配器 |
| ponytail-review | ponytail 4.9.0 | 已读；本轮只列可删项，不改码 |
| verification-before-completion | superpowers | 已读；测试/类型检查结论均在见到命令输出之后才写 |
| long-running-background-tasks | grok bundled | 已读；vitest/tsc 在前台跑完，见 §4 |
| brainstorming / writing-plans | 存在 | 阶段 A 技能，本步 B1 不用 |
| requesting-code-review / receiving-code-review | 存在 | B4/B2，本步不用 |
| tdd / test-driven-development | 存在 | B3，本步不用 |
| writing-for-agents | 存在 | B5 收口文件，本步不用 |
| resolving-merge-conflicts | 存在 | 无冲突 |
| ponytail | 存在 | B3 修复强度，本步不用 |
| verification 之外的 Arc 全量审计席 | — | **不用**：本体派子代理 = 第三席 |
| audit-context-building | 存在 | **不用**：本体强制派子代理写 dossier |
| research / dispatching-parallel-agents / MCP | — | 提示词划掉 / 用户禁令 |

160 提示词 SHA256（本席实算）：`c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`  
modules.md：`8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`  
runbook.md：`c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`

## 2. 范围与文件现状（全文读，不抽样）

16 文件实算合计 **3210** 行（`wc -l`），与操作员「现状合计 3210 行」一致。主工作区相对 origin/main 仅下列 8 文件有未提交差异，且 `git diff origin/main -- <16 files>` SHA256 = `30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b`，与 `/home/james/audit-tmp/m038_b3r1.diff` **逐字节相同**（`cmp` 相同）。

| 行 | SHA256 | 路径 | 相对 origin/main |
|---:|---|---|---|
| 777 | `d7668403ba673b8375a541f24220397d19f2cb92d345392cf63f4100dc94b101` | AccessGovernancePage.tsx | 已改（001–010） |
| 61 | `b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5` | AccessGovernancePage.degraded.test.tsx | 同 origin |
| 191 | `521dc934d768003006e329b7728985d795e3006437d905c7bb201af04da48871` | AccessGovernancePage.guidedcreate.test.tsx | 已改 |
| 82 | `84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6` | AccessGovernancePage.scopechange.test.tsx | 同 origin |
| 328 | `bcfe27b0f09a09c0b19f6a0f92472eac841cd64559d17172196b008e9005b327` | AccessGovernancePage.test.tsx | 已改 |
| 374 | `b4ae66d2735e0c20b28080ea36c3ff2a472c5dcb9cd540696f0f10215f9f230d` | AccessUsersPage.tsx | 已改 |
| 41 | `ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca` | AccessUsersPage.metricdetail.test.tsx | 同 origin |
| 113 | `33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d` | AccessUsersPage.partnerbind.test.tsx | 同 origin |
| 45 | `fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8` | AccessUsersPage.robustness.test.tsx | 同 origin |
| 136 | `d5c22dfcc87a87e06a01fd28bc6c428984f716cd4e9ef675e24c4dbb39cf42b9` | AccessUsersPage.test.tsx | 已改 |
| 552 | `4b31fa7e991e6c540e10930767bc29f4f31d6ec4f02a661bb42a49cade64a67d` | PermissionTemplatesPage.tsx | 已改 |
| 116 | `e026609a0545a09f6013b258c8cc60a7d83c6d7a4a452009cf1c783af894ec` | PermissionTemplatesPage.guidedcreate.test.tsx | 同 origin |
| 153 | `88e1f50b434b093fd4e494c6529c6528a7d83c6d7a4a452009cf1c783af894ec` | PermissionTemplatesPage.test.tsx | 已改 |
| 84 | `bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193` | Can.tsx | 同 origin |
| 44 | `a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5` | PermissionGate.tsx | 同 origin |
| 113 | `14f28de8ffc8544fd26f9a5843e156ecb7dd315591375fb51af827185d4ccbfc` | PermissionGate.test.ts | 已改 |

共享前端（adminClient / HighRiskConfirm / GuidedFlowModal / adminRoutes / statusLabels / useAdminResource）按 `git show origin/main:路径` 阅读，不采主工作区未发布他模块改动。

## 3. 后端映射与库闭集（只读）

AccessController `@RequestMapping("/api/v1/access")` 与本页请求对照（本席读 `AccessController.java`）：

| 本页动作 | 方法/路径 | 后端门控码 | 前端按钮/页闸 |
|---|---|---|---|
| 权限模板列表 | GET `/permission-templates` | `access.permission_template.view` | 页闸 ADM-ACC-002 同码 |
| 新建/改/复制模板 | POST 集合、PUT `/{id}`、POST `/{id}/copy` | `access.permission_template.manage` | PermissionButton 同码 |
| 应用模板 | POST `/{id}/apply` | `access.permission_template.apply` | HighRiskConfirm 同码 |
| 权限点清单 | GET `/permissions` | `access.permission.view` | 缺权则不请求 |
| 用户清单 | GET `/users` | `access.user.view` | 缺权则不请求 |
| 角色清单 | GET `/roles` | `access.role.view` | 缺权则不请求 |
| 岗位模板 CRUD/生命周期 | `/position-role-templates` | `access.role.assign`（批准删除另加 `governance.supervisor_review.approve`） | 页闸 ADM-ACC-003 = `access.role.assign` |
| 定岗 / 范围变更 / 装配角色 / 覆盖 | PUT position、PUT scope、POST role-assignments、POST permission-overrides | `access.role.assign` | PermissionButton ASSIGN |
| 授权申请 | POST `/grant-requests` | `access.grant_request.apply` | GRANT_APPLY |
| 授权列表/总结/读政策 | GET grant-requests、summary、grant-policy | `access.grant_request.view` | GRANT_VIEW |
| 审授权 | POST `/{id}/review` | `access.grant_request.review` + `governance.supervisor_review.approve` | GRANT_REVIEW |
| 写政策 | POST `/grant-policy` | `admission.policy.manage` | 同码 |
| 建账号 | POST `/users` | `access.user.manage` | 同码 |
| 员工注册申请 | POST `/api/v1/staff/registrations` | StaffController `/api/v1/staff` | 引导提交（非 access 前缀，路径与控制器一致） |
| 绑/解绑机构 | POST `/api/v1/partner/orgs/{id}/bind-user`、`unbind-user` | `partner.org.manage` | PermissionButton 同码；列表 GET `/orgs` 后端 `partner.org.view`，本页 catch 空列表 |

`hasPermissions`（PermissionGate.tsx:5-18）：loading/error 拒绝；denies 优先于 grants 与 wildcard。与 BEQR-151「显式拒绝优先」一致。页面闸不能替代服务端 `requirePermission`（GEN-005 服务端仍由 AccessController 执行）。

### complex_test 只读 SELECT（current_database=complex_test）

- `staff_levels`：30 行，id 恰为 `lvl_m1_lower` … `lvl_m10_upper`，与 `AccessGovernancePage.tsx:56-59` 页内生成闭集逐键相等。CHECK：`main_level IN (M1–M10)`、`sub_level IN (upper,middle,lower)`、`level_rank 1–30`。
- `permission_templates`：无 `template_type` CHECK；现值仅 `role`×5。本页下拉 base/temporary/copy/role。
- `access_grant_requests` CHECK：`status IN (pending,applied,rejected)`；`request_type` 六值（user_role/user_position/temporary_permission/role_permission/user_scope_change/position_high_risk_release）。本页申请弹窗只发前三类；六值均有中文映射（001 已落地）。
- `position_role_templates` CHECK：`status IN (active,inactive)`，与弹窗下拉一致。现值岗位码五条均在 `POSITION_LABELS`。
- `access_users`：无 account_type CHECK；现值仅 `platform_admin`。本页新建下拉 staff / outsourced_vendor。

## 4. 发布口径可执行产物

一次性 worktree：`git worktree add --detach /tmp/m038-b1-grok-wt origin/main`（HEAD `53dec141`）→ `git apply /home/james/audit-tmp/m038_b3r1.diff` → 软链主仓 `node_modules` 与 `apps/web/node_modules` → 在 `apps/web` 执行：

```
npx vitest run <本模块 11 个测试文件>
```

输出：`Test Files  11 passed (11)` / `Tests  57 passed (57)` / Duration 2.50s。exit 0。

```
npx tsc -b --noEmit
```

exit 0（`TSC_EXIT=0`）。

清理：删除两处软链；`git checkout HEAD --` 还原 8 个已 apply 文件；`git worktree remove` **未**用 `--force`。worktree 目录已不存在。主工作区 HEAD 仍 `e456ba96`，16 文件未因本席改变。

未跑 origin/main 全量前端单测（已知基线他模块失败）。未调任何写接口、未登录、未对库执行非 SELECT。

## 5. 第一性原理三问（模块级）

1. **为什么必须存在？** 治理地图 `h2`「权限和职级：每一个单一功能都可以单独授权」（PGM-040，html:643）与速查表「职级」「最终权限计算」（BEQR-149/151）要求：单一功能权限点、角色/基础/临时模板、员工单独授予与显式拒绝、M1–M10×上中下、岗位模板定岗自动装配、外包账号与审计岗位。删掉这三页，上述 HTML 在后台管理端做不到。Can/PermissionGate 落实「不同人只看自己该看的」（BSM-114 管理后台行）与 DENY 优先。
2. **声称的行为有没有被实际执行验证？** 发布口径 vitest 57 全绿、tsc 通过（§4）。测试使用真实 `AdminApi` + stub `fetch`（或页面组件渲染），断言请求路径/方法/字段与中文文案；PermissionGate.test.ts 现含组件渲染（010）。复制/应用连点无测试（见 GR-01）。
3. **未经验证就当真的前提？** （a）页闸与按钮码等于后端码——已用 AccessController 逐条对照，成立。（b）职级 30 档等于库——SELECT 成立。（c）`bindBusy`/`submitting` 足以挡住连点——对复制/应用不成立（GR-01）；绑定仅有 state 无 ref（GR-02）。（d）创建外包账号再套模板是单一事务——不成立（GR-03）。

## 6. 顺从式：106 项逐项适用

判据全文只引用锁定义。符号：适=本面有实现须核对；无=本聚合网页不承载该锚点业务面。

### 6.1 四个定义变更 ID（按现行正文重审）

**BSM-114 — 各类入口：不同人只看自己该看的、只做自己该做的**（锁引 BSM html:1827；现行正文含管理后台 / Web 前端地理识别 / 经销商门户等表）  
适：本三页是管理后台（origin/main `adminRoutes.ts` ADM-ACC-001/002/003），PermissionGate 无权限渲染「权限不足：当前账号不能访问该后台页面。」（PermissionGate.tsx:36-41），按钮级 Can/PermissionButton fail-safe。管理后台行「配置、查询、复核」与访问治理/模板/账号页职责相符。  
无：Web 前端必须打开地理识别、核销/收款位置范围、商户 App、一体机、经销商门户——不在这 16 文件。不把地理识别缺位算本模块冲突。

**BSM-120 — 已确认规则和实施前输入项**（锁引 BSM html:1957；现行正文含职级 M1-M10 上中下、员工权限按单一功能、Web 地理识别、端开关 opt-in 等）  
适：职级三十档下拉（AccessGovernancePage.tsx:56-62、008 测试 30 项）；单一功能勾选网格按左侧菜单分组（PermissionTemplatesPage.tsx:135-156；BSM-007「照着目录勾」html:588）；员工级授予/拒绝（AccessGovernancePage.tsx:576-587）；角色装配与高危申请。  
无：购票入口、积分兑券、终端开关缺行默认关、私人手机设备标识等。本页不实现端开关。

**PGM-043 — 外包人员与审计人员**（锁引 PGM html:673；现行正文含外包账号范围、机构自助链接与额度、审计只读两类、导出脱敏强制清单与密码暗纹）  
适：账号页账号类型「外部协作机构账号」、绑定/解绑机构（AccessUsersPage.tsx:256-272，POST 与 PartnerController bind/unbind 同码 `partner.org.manage`）；岗位中文映射含内部/外部审计与三类外包（AccessGovernancePage.tsx:42-49）。绑定下拉只列 `outsourced_vendor`、只列 `status===active` 机构（partnerbind 测试）。  
无：机构自助开通下属、下属数量上限随合同、审计查看/导出敏感信息审批、导出强制脱敏与密码、敏感页禁复制——本 16 文件无导出口与自助链接 UI。不把缺导出脱敏算本页冲突。

**PGM-059 — 客人与员工的个人信息保护**（锁引 PGM html:868；现行正文含加密存放、界面脱敏无关闭开关、商户档案联系人例外、手机号为查客人唯一路径）  
适：员工账号表/抽屉字段不含手机号；AccessUsersPage.test.tsx:66 断言抽屉 `not.toContain("13800000001")`。  
无：客人查询专门通道、商户档案联系人明文例外、收银录会员号。本页不提供客人检索。

### 6.2 其余映射 ID

| ID | 判 | 理由 |
|---|---|---|
| BEQR-063 高危动作（反结/沽清/免单等） | 无 | 酒吧现场高危，本页无沽清/免单入口。grep 三 HTML 后确认本页不声称实现这些动作。 |
| BEQR-074 隐私桌台权限 | 无 | 桌台查看/紧急接管不在这三页；PGM-044 同族。 |
| BEQR-143–148 四类主体/经营关系/城市项目公司/运营职责/收入归属/合作模式 | 无 | 主体与收入口径；本页只用组织/商户编号作范围字段，不定义主体词表。 |
| BEQR-149 职级 | 适 | 30 档闭集下拉 = 库 30 id；显示 M1·下…M10·上。 |
| BEQR-150 终端十一端 | 无 | 无终端开关 UI。 |
| BEQR-151 最终权限计算 | 适 | 角色装配 + 模板应用 + 单独授予 − 显式拒绝；hasPermissions DENY 优先；覆盖 effect grant/deny 与库 CHECK 一致。 |
| BEQR-152 项目与敏感数据权限 | 适 | 范围变更表单（跨项目/换商户先审批）；dataScope 固定 `"project"`。不在本页展示内部结算价。 |
| BEQR-153 终端开关优先级 | 无 | |
| BEQR-154 综合体项目状态 | 无 | |
| BEQR-155 商户/运营主体状态 | 无 | 机构下拉只滤 active，不是商户六态维护页。 |
| BEQR-156 合同状态 | 无 | |
| BEQR-157 员工状态（待入职/在职/试岗/停岗/离职） | 无 | 本页是登录账号启用态 invited/active/disabled→待激活/有效/已禁用，不是人事五态；不把两套词表当成冲突。 |
| BEQR-158 设备状态 | 无 | |
| BEQR-159 权限状态（草稿/已启用/暂停/已停用/归档） | 无 | 本页不维护权限点定义生命周期；模板/岗位模板启用态走 active/inactive CHECK。 |
| BEQR-166 合同审批链 | 无 | |
| BEQR-182–187 考勤/打卡/请假/薪资/提成 | 无 | 员工手机端/人事域，BSM-084–088 同。 |
| BEQR-189 管理费方式 | 无 | 服务点单。 |
| BSM-001 整体说明 | 适 | 后台权限配置是「管什么」的底座能力之一，本页承载后台面。 |
| BSM-003 系统通用能力 | 适 | 引导新建、权限分岗、模板按目录勾选（html:588）。 |
| BSM-004 存档删除 | 适 | 岗位模板存档/申请删除/最高权限批准删除/恢复；理由必填。交易类不在本页。 |
| BSM-005 分步引导 | 适 | 新建员工账号、新建权限模板、新增岗位模板、发起授权申请均 GuidedFlowModal。 |
| BSM-006 动态二维码 | 无 | |
| BSM-007 平台管理员日常权限 | 适 | 模板按菜单勾选；本页不自动把高危授给平台管理员（高危走申请/政策）。人数上限配置不在本页。 |
| BSM-009 / BSM-028 / BSM-029 基础管理入口与主要功能/和谁有关 | 适 | 权限是基础管理的一块；本页即该入口的网页实现。 |
| BSM-072–074 商户管理 | 无 | 商户专区全开放不在管理后台这三页。 |
| BSM-084–088 员工手机端排班考勤薪资 | 无 | |
| BSM-115 相互关系 | 适 | 定岗连模板、模板连权限点、账号连机构，本页把这几段连上。 |
| GEN-001 | 适 | 三页均有 HTML 上位。GR-01 所缺的是防护而非多余功能。 |
| GEN-002 | 适 | 57 测绿、tsc 过；复制/应用连点与声称的「提交中」不一致（GR-01）。 |
| GEN-003 | 适 | 关键写路径多数有能失败断言；复制/应用连点无测（随 GR-01）。组件闸已有渲染测（010）。 |
| GEN-004 | 适 | 职级闭集下拉；岗位码仍自由文本（库无岗位词表，V349 注释确认自由文本）。 |
| GEN-005 | 适 | 页闸+按钮闸+后端 requirePermission 对齐。 |
| GEN-006 | 适 | 模板时间往返已按本地时区（006）；创建外包账号后套模板非原子（GR-03）。 |
| GEN-007 | 适 | 治理页 runWrite+ref；模板复制/应用无中态（GR-01）；绑定仅 state（GR-02）。 |
| GEN-008 | 适 | 路径/方法/字段/权限码与 AccessController 一致；职级 id 与库 CHECK 一致；授权状态/类型与 CHECK 六值+三状态一致。 |
| GEN-009 | 适 | 治理页写失败 setGlobalError；模板保存/新建有错误区；复制失败有 formError。 |
| GEN-010 | 适 | 无明文秘密；密码框 type=password；无越权仅靠隐藏。 |
| GEN-011 | 适 | 写动作带 operatorNote；审批空意见发「通过/驳回」中文（001）。 |
| GEN-012 | 适 | 授权列表分页；权限网格按已加载清单勾选。 |
| GEN-013 | 无本面构建 | 本模块无 package/CI 文件。发布口径 tsc 在 apps/web 通过。 |
| GEN-014 | 无 | 本 16 文件无迁移。 |
| GEN-015 | 适 | 治理地图 html:287 四样例外。001–010 已把状态/类型/开通方式/未登记岗位/字段「编号」中文化。权限码、单据号、岗位码取值属系统内部标识符，可保留；测试对抽屉仍允许 `access.role.assign`。 |
| GEN-016 | 适 | 本报告路径/命令/哈希均为本席实跑。 |
| GEN-017 | 适 | 本席按冲突→细化→技术性→添加分类。 |
| GEN-018 | 适 | 证据为 HTML 原文、vitest/tsc 输出、psql 输出、源文件行。 |
| PGM-001–003 治理定位/顺序/模块入口 | 适 | 「权限和职级」是入口卡之一（html:367），本页即该卡的后台页。 |
| PGM-004–008 / 017–022 / 024 / 026–027 / 031–036 | 无 | 综合体/空间/商户/合同/员工手机/设备/经销商资金等，本页不实现那些模块的主功能。PGM-008 员工和手机与账号页有弱关联（建账号），人事三表仍在 StaffController 他页。 |
| PGM-009 模块入口·权限和职级 | 适 | 同 PGM-040 的入口卡。 |
| PGM-010 终端开关 | 无 | |
| PGM-011 设备管理（入口卡） | 无 | |
| PGM-012–016 商户侧/开放接口/留痕/楼宇/应急入口卡 | 无（PGM-016 应急与隐私入口；脱敏细则在 059 已评） | |
| PGM-040 权限和职级专章 | 适 | 见 §5。 |
| PGM-041 权限规则 | 适 | 最小颗粒度、角色模板、员工自定义、敏感动作审批、项目隔离、最终权限公式、临时有效期/单次、角色装配细则、临时权限总结、五类高危授出前审批。本页有模板网格、覆盖、定岗、申请、总结、政策表单。 |
| PGM-042 职级规则 | 适 | M1–M10 上中下；定岗可填基础薪资（职级只给大范围，具体薪资另填）。 |
| PGM-044 桌台挂单经销权限 | 无 | 无桌台/挂单/经销商审核 UI。 |
| PGM-048 商户侧全开放 | 无 | |
| PGM-060 会员身份营销生物信息 | 无 | |
| PGM-065 连接关系 | 适 | 权限底座被票务/财务等消费；本页配置那座底座。 |
| PGM-066 已拍板规则 | 适 | 与 BSM-120 重叠的「职级」「单一功能权限」「界面全中文」已核；其余拍板项无本面。界面全中文原文 html:287。 |

## 7. 对抗式（grilling 结构自问自答）

❓ **Q1** — 复制模板连点会不会建两份？  
➡️ 会。`copyTemplate`（PermissionTemplatesPage.tsx:283-309）无 `setSubmitting`、无 ref。按钮（407-415）`disabled={!selected?.id}`。origin/main `adminClient.ts:86-91` 每次 POST 新 `idempotencyKey`。后端 `copyPermissionTemplate` 用新 UUID 建副本（AccessService.java:182-201）。连点两次 = 两份模板。同页「保存修改」有 submitting（248-280），对比成立。

❓ **Q2** — 应用模板有 window.confirm，还算连点吗？  
➡️ 算。HighRiskConfirm（origin/main）确认后直接 `onConfirm()`，无提交中禁用。`applyTemplate`（324-341）自身不置忙。第一次确认发出后按钮仍可点，第二次确认再发一笔。后端 apply 按目标用户追加，两笔会赋权两次。

❓ **Q3** — 治理页 005 的 runWrite 有没有漏网？  
➡️ 14 处写动作走 `runWrite`（AccessGovernancePage.tsx:116-128，`writeBusyRef` 挡同拍）。引导保存用 `templateSaving`/`grantSaving`。本页连点测试 005 已绿。不是本轮新缺陷。

❓ **Q4** — 账号页绑定的 bindBusy 是否等同 005？  
➡️ 不等同。`bindBusy` 是 React state（AccessUsersPage.tsx:46,200-237），`disabled={bindBusy}`（267-272）。无 ref。同事件循环连点时两拍都见 `false`。每次 POST 仍新幂等键（adminClient）。PGM-043 外包绑定是本聚合关键写路径。

❓ **Q5** — 外包账号「创建并赋权」失败会留下半成品吗？  
➡️ 会。`createUser`（125-142）先 POST `/access/users` 再 POST apply。apply 被 `requireGrantApprovalIfNeeded` 拒绝（AccessService.java:236-240）或 403 时，catch 报「创建员工并赋权失败」，账号已在库。不是单一事务。

❓ **Q6** — 英文还在不在？  
➡️ 给人看的状态/类型/开通方式/岗位未映射/字段名「编号」已按 001–003、009 中文。权限码、用户编号、岗位码取值属四样例外中的系统内部标识符/单据号。默认表单值 `ops_manager`、`org-1` 是标识符取值。未把「输入框里是码」升为冲突。

❓ **Q7** — 测试是不是把被测逻辑整个 mock 掉？  
➡️ 主测试走真实 AdminApi + stub fetch，断言 URL/body/中文。metricdetail 测用内存 api stub，只测 KPI 接线，不断言权限网格。PermissionGate 组件测用 createElement 真渲染。复制/应用连点无测。

❓ **Q8** — HTML 写了要有的有没有？  
➡️ 有：分步引导新建、按目录勾权限、定岗自动适配、单独授予/拒绝、跨项目先审批、外包账号与机构绑定、三十档职级、DENY 优先、存档删除审批。  
HTML 未写「复制模板」四字（三份 HTML grep「复制」只命中防伪/导出不可复制）。复制作为配置操作按细化/技术性留下，不升添加。

## 8. ponytail-review（只读）

无多余抽象可删。页内中文小映射、三十档生成、runWrite 都是 001–010 的必要行。`net: 0` 可删行。缺的是复制/应用（及绑定 ref）的互斥，不是多出来的代码。

## 9. 临时 findings

### GR-01 — 权限模板页复制/应用无提交中态，连点发两笔

- 文件:行：`apps/web/src/admin/pages/PermissionTemplatesPage.tsx:283-309`（copyTemplate 全文）、`:407-415`（复制按钮 `disabled={!selected?.id}`）、`:324-341`（applyTemplate 全文）、`:419-428`（HighRiskConfirm `disabled={!selected?.id}`，`onConfirm={() => void applyTemplate()}`）。对比同文件 `:248-280` 保存修改有 `setSubmitting`。
- 原文/上下文：复制函数在校验后直接 `api.post(.../copy)`，finally 也不置忙；应用同样。origin/main `adminClient.ts:86-91` `nextIdempotencyKey()` 每次新键。
- 正常触发/影响：选中模板后连点「复制模板」两次 → 两个副本（copy 用新 UUID）。点「应用模板」确认后再点再确认 → 对同一批目标用户 apply 两次。首次正常使用可复现。
- 最小拟处置：页内加与治理页 `runWrite` 同构的 ref 互斥，复用已有 `submitting` 禁用复制与应用；测试：连点复制只一笔且提交中两按钮禁用；连点应用并两次确认只一笔。不改 HighRiskConfirm/adminClient/后端。B1 不修。
- 四分类：冲突否（HTML 未规定提交中态）→ 细化否（不是把 HTML 句子写细）→ **技术性**（无业务发明，纯提交防护）→ 非添加。
- 工程缺陷：成立
- 冻结 ID：GEN-007、GEN-002
- 依据类型：可复现命令/源码与 origin/main adminClient 原文；vitest 未覆盖该分支（命令：本席全文阅读上述行；发布口径 57 绿不含复制连点用例）
- 与既有输入关系：行为即旧锁定稿 **M038-011** 所描述的现状；本席独立复核仍在，不复用旧 GR 号。B1 不修。

### GR-02 — 账号页绑定/解绑只有 state 置忙，无 ref，同拍连点可发两笔

- 文件:行：`apps/web/src/admin/pages/AccessUsersPage.tsx:46`（`bindBusy`）、`:195-215` bind、`:218-238` unbind、`:267-272` `disabled={bindBusy}`。
- 原文/上下文：`setBindBusy(true)` 在 await 之前，无 `bindBusyRef`。对比治理页 `writeBusyRef`（AccessGovernancePage.tsx:115-118）先挡再 setState。
- 正常触发/影响：选好外包账号与 active 机构后连点「绑定机构」或「解绑」，两笔 POST 各带新幂等键（`crypto.randomUUID()` + adminClient 再套一层）。PGM-043 外包绑定是本页关键写。
- 最小拟处置：绑定/解绑走与 runWrite 同构的 ref；disabled 并入该忙；补连点只一笔测试。B1 不修。
- 四分类：**技术性**
- 工程缺陷：成立
- 冻结 ID：GEN-007
- 依据类型：源码行 + origin/main adminClient 幂等键生成原文

### GR-03 — 外包账号创建成功后再套模板，失败留下已建账号

- 文件:行：`apps/web/src/admin/pages/AccessUsersPage.tsx:125-142`（POST `/api/v1/access/users` 随后 POST `.../apply` 同在一个 try）、`:151-155` catch 文案「创建员工并赋权失败」。
- 原文/上下文：后端 apply 对高危码 `requireGrantApprovalIfNeeded` 直接拒绝（AccessService.java:236-240，注释写明模板入口无待审窗口）。
- 正常触发/影响：新建外部协作账号并选择含高危权限的模板提交。用户已插入，apply 403，页面报整笔失败。首次按「创建并赋权」使用即可碰到（PGM-041 高危不得经模板直发）。
- 最小拟处置：套模板失败时把结果提示写成「账号已创建、赋权未完成：…」或先校验模板可否直发再创建；不在 B1 修。不新增「创建后自动走授权申请」功能（那是添加）。
- 四分类：**技术性**（错误语义/部分写入，无新业务实体）
- 工程缺陷：成立
- 冻结 ID：GEN-006、GEN-009
- 依据类型：源码行 + AccessService 原文

## 10. 观察（不立项）

- 权限模板类型 `copy` 四字未出现在三份 HTML（已 grep）。作为复制操作的 provenance，按技术性留下；最终权限公式仍是基础/临时/角色，不因 type=copy 另算。
- `permission_templates.template_type` 无 DB CHECK；闭集若要落到库，属后端模块。
- `organizationId` 初始 `"org-1"`、`targetUsersText` 初始 `"user-1"` 为标识符占位；用户列表加载后会替换目标员工。不升添加。
- 岗位编码自由文本：库无岗位词表（迁移 V349 注释写明）。未映射显示「未登记岗位（码）」（003）。不为自由文本补词表需求。
- 授权政策表单四字段（窗/岗位阈值/用户阈值/风险审批层级）与 POST `/grant-policy` 一致。审批时限键若在他模块政策 JSON 中，属 M032 范围，本 16 文件无该字段 UI。
- GET `/partner/orgs` 未在前端按 `partner.org.view` 预检，403 被 catch 成空下拉，属降级。
- 001–010 已在源码与 57 测中可见（中文映射、未登记岗位、取消存档/恢复、runWrite 防双击与失败提示、时间往返、表头列数、三十档下拉、状态下拉用词、闸组件渲染、授权申请引导提交）。

## 11. 测试有效性（GEN-003）

11 文件均渲染目标页或闸组件并操作：点行开抽屉、引导步进提交、范围变更 PUT body、绑定/解绑 URL、副资源 403 降级、KPI 出口、连点治理页装配只一笔。断言含中文/not.toContain 英文枚举，能失败。metricdetail 用内存 stub，不断言整页加载，范围仅 KPI。缺测仅 GR-01 对应的复制/应用连点（关键分支，随该条处置补，不另立缺测 ID）。

## 12. 覆盖表

| 文件 | 三线 | 读完 |
|---|---|---|
| AccessGovernancePage.tsx 777 行 | 是 | 是 |
| 其 4 个测试 | 是 | 是 |
| AccessUsersPage.tsx 374 行 | 是 | 是 |
| 其 4 个测试 | 是 | 是 |
| PermissionTemplatesPage.tsx 552 行 | 是 | 是 |
| 其 2 个测试 | 是 | 是 |
| Can.tsx / PermissionGate.tsx / PermissionGate.test.ts | 是 | 是 |

106 判据均有适用或无本面记录。四个变更 ID 按 V0.14 正文重审。

## 13. 步骤结论

本席 B1（现锁 V0.14 重开）**FAIL**。

不是模块 PASS。不是 B2 定稿。修复队列候选：GR-01（=既有 M038-011 现状，B1 不修）、GR-02、GR-03。001–010 保持已落地。B3 累计仍为 1（本席不写 ledger）。无 HTML 申请、无 LOCK-AMEND、无 questions 文件。

无「有条件通过」。

- 文件自身版本：V0.04（本段追加；上方 V0.01–V0.03 共 83469 字节原样保留，不得覆盖）

# M038 现锁 V0.14 B2-r3 整批归并复核（Grok 乙席）

- 文件自身版本：V0.05（本段追加；上方 V0.01–V0.04 前缀 **115014** 字节 SHA256 `1322fb2d24c198f80b848adc205f5b37bd46fcfc7d1a252a8a9c9349513aeb76` 原样保留，未改写、未覆盖。本段不写独占锁哈希字段行）
- 席位：固定 Grok 只读外审席乙（grok-4.6 / xhigh）。步骤：B2-r3 整批候选复核。不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3/B4。
- 正本：`/home/james/complex-management` main 主工作区。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `53dec14170f01a6b8ebb1b7beb34f5aaf10da2fb`（未 fetch）。`test -f .git/MERGE_HEAD` → NO_MERGE。B3 累计 1；本批若双 PASS 定稿后一次 B3-r2 为第 2 次。无源修复。
- 提示词：`160-audit-prompt.md` V0.17。已冻结判据不加判据。本席 FAIL 具否决权。
- 范围：只审本批的归并 / 分类 / 队列 / 最小修复边界，不重新审模块。
- 独立性：输入仅为两份已封存现锁 B1 追加段 + findings 顶部候选。席甲 raw 全文现 95182 bytes；本席只取派发指定的前 **94155** 字节（SHA256 `f3678b1bece7d6d2aaf5d0cebfd1b8a170ec09c86c6bc884eeec1a78795cc6e5`）。核前缀长度时见到后缀标题「M038 现锁 B2-r3 整批候选复核（席甲票，V0.05）」及输入哈希声明，**未继续读取其后票面与结论，不以该后缀为证据**。未用 MCP / 子代理 / 第三席；不问操作员。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写本指定路径以外的任何新产物；未对任何数据库执行写语句。
- 锁：本席再算 `plan/criteria-lock.md` SHA256 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（与 sidecar 一行、ledger 顶部同行、本文件 V0.04 追加段独占行同值）。`LOCK-AMEND-COUNT: 1`。**本段不写新的独占 CRITERIA-LOCK-SHA256 行。**
- 本席 B2-r3 票：**PASS**（整批候选同意；011 沿用定稿 PASS；001…010 维持队列否 PASS；012 归并不改写、边界合适 PASS；GR-02 只登记 PASS；其余观察只登记 PASS；添加 0、无 HTML 申请、无 questions）。

此票只判断 findings.md V3.88 顶部「当前 M038 现锁 V0.14 B2-r3 整批候选」相对本次两份现锁 B1 追加段的差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.02 B2-r1 **PASS**、V0.03 B2-r2 **PASS**、V0.04 现锁 B1 **FAIL** 原文，亦不以本票代替 B5。不加判据。无「有条件通过」。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M038-b1-grok-raw.md` 本文件前缀（V0.01–V0.04；本段追加前） | V0.04 | 115014 | `1322fb2d24c198f80b848adc205f5b37bd46fcfc7d1a252a8a9c9349513aeb76` |
| 其中现锁 B1 追加段（本席 V0.04，FAIL：GR-01＝既有 M038-011、GR-02、GR-03） | V0.04 | 自 83469 起 | 含于上列全文 |
| `docs/audit-2026-09/modules/M038-b1-self-raw.md` **仅前 94155 字节**（席甲现锁 B1 V0.04，FAIL：CX-M038-006＝既有 M038-011） | V0.04 | 94155 | `f3678b1bece7d6d2aaf5d0cebfd1b8a170ec09c86c6bc884eeec1a78795cc6e5` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M038 现锁 V0.14 B2-r3 整批候选」 | V3.88 | 1348189（全文件） | `740b274c35239f9481d186fec123c824c20dfba8f3972063058e498d385ad3f0` |

四输入字节/SHA 与派发清单全等（席甲 raw 按指定前缀截取，不采信其后追加段）。本文件独占锁哈希字段行仍仅文首旧锁一行与 V0.04 现锁一行，本段未新增。本模块 16 文件工作区 SHA256 与本席 V0.04 B1 表逐文件全等（mismatches 0）。`git diff origin/main -- <8 个已改文件>` SHA256 = `30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b`，与 `/home/james/audit-tmp/m038_b3r1.diff` 逐字节相同。工作区 `git ls-files -z -- <16> | xargs -0 cat | wc -l` = **3210**。

controller / runbook / modules.md 只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

`docs/audit-2026-09/questions/` 无 M038 文件。ledger 只读：M038 `IN_PROGRESS`，B3 列 1。本席未改台账。GR-02 探针日志 `/home/james/audit-tmp/m038_gr02_probe.log` SHA256 `9cd372861b6281141270a16d2dfb33b7ba4764ec7a600b1a16d0f34cec201b2e`；对应脚本 `m038_gr02_probe.sh` SHA256 `9ec974254709fc6139da204f902c75a42a5ccdb05444d6d770c648ea710407ca`（只读，未重跑、未改）。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| graft | 用户本轮禁 MCP/子代理；本节点用 graft CLI 一次定位 copy/apply/bind/createUser 行号后点读源码 |
| receiving-code-review | 先核源码写入口 + 两份现锁 B1 raw + 候选表 + 探针日志，再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：011 事实是否仍在、四条变更正文在三页有无对象、012 是否改写 GR-03、GR-02 有无首次可复现路径 |
| verification-before-completion | 声称前先跑只读哈希、16 文件指纹、三页写入口与探针日志点读；本票不声称 vitest 本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借对照 GR-01/02/03 与候选；不改生产代码 |
| codebase-design / ponytail-review | 011 最小是页内互斥包装 + 两条连点测试；012 最小是套模板单独 catch + 一条 403 测试；不为绑定制 ref |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

---

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M038-011 | 旧锁 B5-r1 本席 GR-M038-012 → 旧锁 B2-r2 双 PASS 定稿；现锁 B1 本席 GR-01 与席甲 CX-M038-006 各自独立重申 | 技术性 / 是 / 是 | **同意**。现锁下事实与最小处置不变，沿用为定稿 |
| M038-012 | 本席现锁 B1 GR-03（席甲现锁 B1 未报；席甲核源成立后归并） | 技术性 / 是 / 是 | **同意**。采本席第一种最小处置，未改写原意，边界合适 |
| （无新 ID） | 本席现锁 B1 GR-02 | 只登记不入队 | **同意**。探针 posts=1 disabled=true；无首次正常使用可复现触发 |
| （无新 ID） | 两席现锁 B1 其余观察（席甲 V0.04 §6、本席 V0.04 §10；同旧锁 B2-r1/B2-r2 登记项） | 只登记不入队 | **同意**。无漏并、无应升队 |
| M038-001…010 | 已修，B3-r1；主工作区未提交；现锁四条变更正文在三页无对应对象 | 否（已修） | **同意不动、不回潮、不重开** |

漏并：无。改写原意：无。第二条可并而未并的队列项：无。新 ID 仅 012（011 沿用既有号）。无本批添加项、无 HTML 申请、无 questions。

**整批候选：同意。**

---

### 一、M038-011（问一）

**票：同意。现锁下沿用为定稿。事实与最小处置不变。**

独立核源（类型 4，工作区 16 文件 SHA 与本席 V0.04 表全等；模板页 `4b31fa7e991e6c540e10930767bc29f4f31d6ec4f02a661bb42a49cade64a67d`）：

- `PermissionTemplatesPage.tsx` L79 `submitting` 只服务保存修改（L248–280 `setSubmitting`；L394 `disabled={submitting \|\| !selected?.id}`）。
- L283–309 `copyTemplate`：校验后直接 `api.post(.../copy)`，无 `setSubmitting`、无 ref。L407–415「复制模板」`disabled={!selected?.id}`，`onClick={() => void copyTemplate()}`。
- L324–341 `applyTemplate`：同样无忙态。L419–428 `HighRiskConfirm` `disabled={!selected?.id}`，`onConfirm={() => void applyTemplate()}`。
- origin/main `adminClient.ts:86-91` 每次 POST 新 `idempotencyKey`。后端 copy 用新 UUID 建副本。连点两次「复制模板」= 两份模板。
- `HighRiskConfirm.tsx` `handleClick` 只拦 `denied` / `precheck`，不读 `disabled`；`window.confirm` 两次均为真则两笔 `/apply`。二次确认不是互斥。
- 默认 `copyName="普通员工01"`、`targetUsersText="user-1"`：选中一行即可连点，首次正常使用可复现。
- `PermissionTemplatesPage.test.tsx` / `guidedcreate.test.tsx` 无复制/应用连点用例。

现锁 B1：本席 GR-01 与席甲 CX-M038-006 各自独立重申同一事实。GEN-007 / GEN-002 现锁正文未变（锁 L42–45、L17–20）。

**四分类（冲突 → 细化 → 技术性 → 添加）：** 冲突否（三 HTML 未写复制必须幂等，也没有允许连点各建一份的相反明文）→ 细化否（不能指到某一 HTML 条目要求防双击）→ **技术性是**（无新实体 / 枚举 / 流程；纯提交防护）→ 不进入添加。

工程缺陷：**是**。队列：**是**。不重开 005（005 写死的是治理页 `runWrite`，已否）。

**最小处置（与旧锁 B2-r2 定稿相同，本席独立裁定现锁下仍合适）：**

1. 模板页加一个与 `runWrite` 同构的互斥包装：`ref` 同步挡并发、沿用现有 `submitting` 让按钮禁用。
2. 复制与应用走它。
3. 两个按钮的 `disabled` 并入 `submitting`。
4. `PermissionTemplatesPage.test.tsx` 补两条：连点两次「复制模板」只发一笔且提交中两个按钮禁用；连点两次「应用模板」并都确认只发一笔。
5. 不改 `HighRiskConfirm`、公共字典、客户端与后端。

写入集合＝`PermissionTemplatesPage.tsx` + `PermissionTemplatesPage.test.tsx`。不改 guidedcreate 测试、治理页、账号页、Can、PermissionGate。

**明确同意问一。现锁下沿用为定稿。**

---

### 二、旧锁 M038-001…010（问二）

**票：同意维持队列否。四条现锁变更正文在三个后台页上确无对应对象。不重开任一项。**

001…010 已在 B3-r1 落地：主工作区 8 个未提交文件与 `/home/james/audit-tmp/m038_b3r1.diff` 逐字节相同（SHA `30bbbdc5…`）。现锁定义变更且在本映射内的四条，本席按 V0.14 锁原文对三页再核：

| 变更 ID | 现锁正文相对旧锁的变更 | 三页对应对象 | 本席 |
|---|---|---|---|
| BSM-114 | 员工私人手机可离场事项「考勤打卡」→「查看考勤」（锁 L4767） | `AccessGovernancePage.tsx` / `AccessUsersPage.tsx` / `PermissionTemplatesPage.tsx` 全文无「考勤」字样 | 无本面。不重开 |
| BSM-120 | 业务地图页脚「更新日期：2026-09-10」（锁 L4966；批准依据写明 09-08→09-10） | 三页不展示该页脚、不承载「已确认规则和实施前输入项」清单 | 无本面。不重开 |
| PGM-043 | 新增「导出文件是否脱敏…强制执行」整段（锁 L5851–5852） | 三页无「导出」「脱敏」「明文」字样，无导出口 | 无本面。外包账号/绑定机构仍是本页既有面，001…010 未改其业务规则。不重开 |
| PGM-059 | 商户档案第一/第二联系人手机号经单独授权可看完整号码的例外（锁 L6204） | 账号表/抽屉字段不含手机号；`AccessUsersPage.test.tsx` L65 断言抽屉 `not.toContain("13800000001")`。例外明文「仅适用于商户档案联系人，不扩大到…员工档案」 | 无本面。不重开 |

001…010 所修（中文取值、恢复文案、治理页 runWrite、模板时间保值、表头列数、三十档、状态下拉用词、闸组件渲染、授权申请引导）所依判据正文未随这四条变更。现锁 B1 两席均把它们作既有修复保留。本席无一项须在现锁下重开。

**明确同意问二。队列否不变。**

---

### 三、GR-03 → M038-012（问三）

**票：同意。归并未改写原意。边界合适。**

独立核源：

- `AccessUsersPage.tsx` L108–159 `createUser`：外包账号先 POST `/api/v1/access/users`（L125–135），再在**同一个 try** 里 POST `/api/v1/access/permission-templates/{id}/apply`（L136–142）。
- apply 失败进同一个 catch（L151–155）：`setActionResult("创建员工并赋权失败：…")`，`setFormError` / `setGlobalError`，**表格不加行、弹窗不关、字段不清**。
- 账号已在库。操作人按提示重试，第二次 POST 同一账号名被后端拒绝 → 「建成了却看不见」。
- 后端 `AccessService.java` L236–240 / L1893–1901：含须审批权限的模板经 `requireGrantApprovalIfNeeded` 以 `GRANT_REQUIRES_APPROVAL` 403 拒绝；注释写明模板入口无待审窗口。
- 成功路径 L143–150：入表、结果提示、清 username/password/consent/evidence、`setCreateOpen(false)`。
- 非外包走员工注册申请（L113–123），不进 apply。`selectedTemplateId` 空则不套模板。这两支不在本条范围。

本席现锁 B1 GR-03 原文最小拟处置：「套模板失败时把结果提示写成『账号已创建、赋权未完成：…』**或**先校验模板可否直发再创建；……不新增『创建后自动走授权申请』功能（那是添加）。」冻结 ID：GEN-006、GEN-009。分类：技术性。

候选采**第一种**：套模板单独 try/catch；失败时账号照常入表、清字段、关弹窗；结果提示写成「创建员工：X（账号类型），账号已创建；套用权限模板 T 失败：<后端消息>」并 `setGlobalError(消息)`；不新造创建后自动走授权申请；`AccessUsersPage.test.tsx` 补一条打桩 apply 返回 403。

对照：

- 「账号已创建；套用权限模板失败 + 后端消息」＝原文「账号已创建、赋权未完成：…」的同一语义，只是把「赋权未完成」写成可定位的套用失败。未改写成新业务规则。
- 入表、清字段、关弹窗是把「建成了却看不见」收口到与成功路径同一批界面状态，属于第一种处置的必要步骤，不是第二种「先校验再创建」，也不是添加。
- `setGlobalError` 与同页其他写失败、以及原文 catch 已有的 `setGlobalError` 一致。
- 明确拒绝「创建后自动走授权申请」，与原文「那是添加」一致。
- 未采「先校验模板可否直发再创建」的备选。本席不要求改采备选。

**四分类：** 冲突否（HTML 未写创建+套模板必须单事务）→ 细化否（不是把某一 HTML 句子写细）→ **技术性是**（错误语义与部分写入；无新实体 / 流程）→ 不进入添加。

工程缺陷：**是**。队列：**是**。写入集合＝`AccessUsersPage.tsx` + `AccessUsersPage.test.tsx`。不改 HighRiskConfirm、GuidedFlowModal、公共字典、客户端、后端、授权申请流。

**B3 非阻断实施注（不否决）：**

- 用户 POST 失败仍走现有外层 catch：不加行、不关窗、提示「创建员工并赋权失败」。
- 套模板内层 catch：入表用与成功路径同一份 `enriched`；清字段＝成功路径同一批（username / password / consentReference / evidenceAttachmentIds），不要额外清 organizationId / selectedTemplateId / accountType。
- 账号类型用现有 `formatBusinessStatus(accountType)`（`outsourced_vendor` → 「外包账号」）；不要顺手改 `statusLabels`。
- 成功路径文案（「创建员工并赋权…已应用权限模板」）不动。
- 测试：用户 POST 200 且返回新 id，apply 403 带中文 `message`；断言 POST `/access/users` 一笔、POST `/apply` 一笔、新行入表、`access-user-flow` 关闭、`setActionResult` 含「账号已创建」与「失败」、`setGlobalError` 接到后端消息。计数不要把列表 GET 算进去。
- 禁止：apply 失败再 POST `/grant-requests`；改 `requireGrantApprovalIfNeeded`；把员工注册申请支路卷进来。

**明确同意问三。归并未改写原意。边界合适。**

---

### 四、GR-02（问四）

**票：同意只登记、不入队。探针成立。本席无首次正常使用可复现的触发路径。**

独立核源：

- `AccessUsersPage.tsx` L46 `bindBusy`；L195–215 `bindPartnerOrg` / L218–238 `unbindPartnerOrg`：`setBindBusy(true)` 在 `await` 之前，函数入口**不读** `bindBusy`，无 ref。
- L267–272 两按钮 `disabled={bindBusy}`。
- `PermissionButton` onClick 只拦无权限、不读 `disabled`（`Can.tsx` L76–79）。
- 对比治理页 `runWrite`（`AccessGovernancePage.tsx` L116–128）先 `writeBusyRef` 再 `setWriteBusy`。
- 本席现锁 B1 据此称「同拍连点可发两笔」，工程缺陷成立、拟补 ref。

席甲探针（只读日志，未重跑）：一次性 worktree `origin/main` + `m038_b3r1.diff`；临时测试对「绑定机构」**两次同步 `fireEvent.click`**，首笔 POST 用永不 resolve 的 Promise 挂起（与 005「提交未完成时再次点击」同构）。日志原文：`M038GR02 posts=1 disabled=true`。脚本把 POST 在 mock 里 `posts.push(url)`，第二下若进了 `bindPartnerOrg` 必为 2。实测为 1，且当时按钮已 `disabled`。

原因（本栈，非理论）：`apps/web` React `^19.0.0`（实装 19.2.7）。离散事件（click）的状态更新在**下一个事件派发前同步刷新**。第一下 click 的 handler 执行到 `await` 前已 `setBindBusy(true)` 并返回 → React 刷新 → 按钮 `disabled` → 第二下 click 未再发出 POST。jsdom 下该探针与浏览器对 disabled 按钮的表现同为 1 笔。

本席撤回「同拍连点可发两笔」作为可复现缺陷的主张。剩余「无 ref、入口不读 bindBusy」是实现形态，不是首次正常使用可触发的双请求。键盘激活、双击（两次 click）、触控均为离散事件序列，走同一刷新。程序里连续两次调用 `bindPartnerOrg()` 不是页面首次正常使用。不为该理论场景补需求。

与 011 的差别：复制/应用**完全没有** submitting，按钮保持可点，两次离散 click 在浏览器里就是两笔。绑定在第一次 click 之后已经禁用。GEN-007 覆盖的是真实竞争窗口；此处窗口在下一次事件前已关闭。

**明确同意问四。只登记。本席给不出可复现触发路径。**

---

### 五、其余观察（问五）

**票：同意只登记。无漏并。无应升队项。**

核过的只登记项（不展开为新 ID）：

- 本席 V0.04 §10：`copy` 类型未入三 HTML；`template_type` 无 DB CHECK；`org-1` / `user-1` 占位；岗位码自由文本；GET `/partner/orgs` 前端未预检、403 降级空下拉。
- 席甲 V0.04 §6：岗位/组织/目标员工手填内部编号；`hasPermissions` wildcard 分支现走不到；账号页标题「企业微信同步结果」「禁用」页内无对应块；速查表权限状态五态无改状态接口；页文件无「文件版本」行；公共字典缺 `applied` 等键（本模块页内映射挡住上屏）；RowActions 无禁用槽（治理页仍走 `runWrite` ref）；M080 另有授权申请引导测试。
- 旧锁 B2-r1/B2-r2 已登记、现锁 B1 两席未升队的观察，维持登记。
- GR-02：见第四节，只登记。

无一项在现锁下变成可复现工程缺陷且本模块可修。无漏并。

**明确同意问五。**

---

### 六、添加／HTML／questions（问六）

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 M038 文件。不 BLOCKED-ASK。011 / 012 均无业务发明，不发 2.2。不按铁律 4 申请改三 HTML。候选表无添加行。

**明确同意问六。**

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r3 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止开工 B3。一次 B3-r2 只改下表写入集合（第 2 次 B3）。

- 011：技术性／是／是。模板页加与 `runWrite` 同构的互斥包装（ref + 沿用 `submitting`）；复制与应用走它；两按钮 `disabled` 并入 `submitting`；`PermissionTemplatesPage.test.tsx` 补两条连点测试。不改 HighRiskConfirm、公共字典、客户端与后端。
- 012：技术性／是／是。套模板单独 try/catch；失败时账号入表、清与成功路径同一批字段、关弹窗；结果提示含「账号已创建」与套用失败及后端消息，并 `setGlobalError`；`AccessUsersPage.test.tsx` 补一条 apply 403。不新造创建后自动走授权申请。
- 001…010：否（已修）。不动。
- GR-02 及两席其余观察：只登记。

| 允许（本模块 B3-r2） | 禁止 |
|---|---|
| `PermissionTemplatesPage.tsx` 加包装、复制/应用走它、两按钮 disabled 并入 submitting；可选把保存修改也走同一包装 | 改 HighRiskConfirm / GuidedFlowModal / Can / adminClient / statusLabels / 后端 / OpenAPI / 迁移 / 三 HTML / 锁 |
| `PermissionTemplatesPage.test.tsx` 补复制连点（一笔 + 提交中两按钮禁用）与应用连点都确认（一笔） | 新建测试文件；改 guidedcreate 测试当 011 主锁；把 011 并回 005；给复制加二次确认 |
| `AccessUsersPage.tsx` 套模板内层 try/catch，失败入表/清字段/关窗/改结果提示/`setGlobalError` | 创建成功后自动 POST 授权申请；改 `requireGrantApprovalIfNeeded`；改员工注册申请支路；为绑定/解绑补 ref |
| `AccessUsersPage.test.tsx` 补 apply 403 一条 | 把 012 测试写进 partnerbind/metricdetail/robustness；改公共字典 |

### 结论

全批差集无漏并、无原意改写、无应升队观察。M038-011 现锁下沿用定稿，技术性／是／是。M038-012＝GR-03 第一种处置，技术性／是／是，边界合适。001…010 维持否，四条变更无本面、不重开。GR-02 只登记。添加 0、HTML 申请 0、questions 0。逐项：差集 PASS、011 PASS、001…010 PASS、012 PASS、GR-02 只登记 PASS、观察 PASS、问六 PASS。本席 B2-r3 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M038 IN_PROGRESS；B3 累计 1，定稿后 B3-r2 为第 2 次。

- 文件自身版本：V0.05（本段追加；上方 V0.01–V0.04 前缀 **115014** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
