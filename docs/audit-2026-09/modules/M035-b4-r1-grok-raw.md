CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M035 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M035-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M035 / 共 471，B3 第 1 次之后的 B4。计划名：功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第1段。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M035-b4-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3／§6、`findings.md` V3.09 顶部「当前 M035 B2-r1 双PASS定稿」、本席旧 `M035-b1-grok-raw.md` V0.01/V0.02（B1+B2，允许）、冻结 diff 与两 tree、现行 V430 SQL、AccessController 被测门控（两 tree 同 blob，不在 diff 内）、AccessControllerTests／AccessJdbcIntegrationTests after 树、PGM-001 锁内原文、B3 RED/GREEN/变异日志、本席对 `complex_test` 的只读 SELECT、本席重跑两测试类。ledger 只读取模块状态与 B3 次数；其中 after tree 之后写入的席甲 B4 封存行不构成本席证据。本票以两 tree 全文 diff、定稿最小处置、本席 B2 边界、本席只读探针与既有 RED/GREEN/变异日志＋本席重跑为准。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；M035-001／002／003 是否按 findings V3.09 定稿最小处置与本席 B2-r1 边界实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到归属「是」的 ID（仅 001、002、003）；新测试断言能否失败；可执行位与文件模式是否未变。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读；技能本体「问人 / 派子代理查事实」作废）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：本轮只核 B3 已落的红绿／变异证据并本席重跑 GREEN，不新写测试。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`git-guardrails-claude-code`：存在；本席未 push/reset/clean。`i-have-adhd`：存在；与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。`long-running-background-tasks`：本席无后台作业。`graft`：本环境存在，已用于定位 `requirePermissions`／`changePositionTemplateLifecycle`／`reviewGrant`／`assignStaffLevel` 路径覆盖。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep`／`callers`／`skeleton` 定位双码门控与路径 id 覆盖 | 无 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 控制器门控 seam vs 服务层；本 diff 只在测试侧锁 seam，不改被测模块 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（三工具 sha256、`cmp` 冻结 diff、schema/终态 SELECT、读 RED/变异违例行、本席重跑 GREEN） | 变异本席未另建 worktree 重放，改核既有 `m035_b3r1_mut.mvn.log` 行号与 after 树断言对齐 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（定稿声称→V430 DML→测试闸）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实跑哈希/`cmp`/只读 SELECT/两测试类；不以「应能过」代替 | 变异 3 红以既有日志+行号对齐为证，本席未重放 worktree |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `tdd` / `test-driven-development` | 存在 | 核 001 RED→GREEN；002 锁既有行为（未改被测代码时即绿）+ 变异 3 红；003 纯改名注释不强制测 | 002 的「未改被测即绿」是定稿要求，不是假绿 |
| `long-running-background-tasks` | grok bundled | 本席无后台作业 | 节点不适用 |
| `git-guardrails-claude-code` | mattpocock misc | 只读；未装 hook | **存在但未安装** |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |
| `resolving-merge-conflicts` | 存在 | 工作区无 merge/rebase | 节点不适用 |
| `writing-for-agents` | 存在 | 本 raw 给下游合取席 | 无 |
| `ponytail` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail/SKILL.md` | B4 只审：V430 一条 UPDATE + 测试最小补面，ultra 已满足 | 不修 |
| `i-have-adhd` | `~/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 结论先行、编号；与独占锁行冲突时 160 优先 | 无 |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。未使用 MCP。未建脚本/持久日志/临时审计产物（复核用 `/tmp/m035_b4_r1_verify.diff` 在 `cmp` 后已删；本席 GREEN 重跑 tee 文件已删）。一次性 worktree 本席未新建。

graft 本席调用节省合计约 81042 tokens（`requirePermissions` / `callers` / `AccessController` skeleton / `assignStaffLevel`）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，与本文件第一行独占锁行一致。文件 889020 字节。sidecar 一行同值（对照，非抄写来源）。ledger 顶部基准同值（对照）。criteria-lock 版本仍 V0.11。LOCK-AMEND-COUNT 仍为 1。

| 项 | 值 |
|---|---|
| 模块 | M035 后台验证／第1段（计划 2 文件；本轮 B3 写入集合＝两测试类 + 新增前向迁移 V430） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V3.09：M035-001 冲突／是／是；002 技术性／是／是；003 技术性／是／是；修复队列「否」0 |
| B3 | 已执行 1 次；模块仍 IN_PROGRESS |
| before（对象类型 tree） | `8d98b7cf96b7f52240da486683ff1d69d441765c` |
| after（对象类型 tree） | `e6db773a3ba42ff53c5b0fcb59a4847d8e00c0cb` |
| `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 8d98b7cf96b7f52240da486683ff1d69d441765c e6db773a3ba42ff53c5b0fcb59a4847d8e00c0cb`（无路径过滤） |
| 实际差异 | 9908 bytes；SHA256 `58691b86a3728dde9285c696d41d9f1f7f31b94165da81bea34b5bcb917f69f9`；3 files changed, 88 insertions(+), 5 deletions(-)；3 hunk 组（V430 整文件 + Controller 4 处 + JDBC 1 处跨两测试）；UTF-8 |
| 冻结 diff | `/home/james/audit-tmp/m035_b3r1.diff` 9908 bytes，同 SHA；本席 `git diff --binary` 写入 `/tmp/m035_b4_r1_verify.diff` 后 `cmp` **IDENTICAL**，随后删除 |
| 正本 | `/home/james/complex-management` main；hostname=`y9000p-ai` |
| findings.md | V3.09；本席 `sha256sum` = `c2705540220ce58437d9c045f6f6345f54a489f1f1b23723f3840d0a3549cd06` |
| 160-audit-prompt.md V0.17 | 本席 `sha256sum` = `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`（路径 `.scratch/module-recheck-20260908/160-audit-prompt.md`） |
| 本席 B1+B2 raw | `M035-b1-grok-raw.md` 58559 bytes，SHA256 `43d8597527439bf5da294574ed02baeaa0d787084a3a1785af7fd64cc5d5f0b3`（与定稿段所引一致；本席独立实算） |
| 三 HTML（对照，不在 diff） | BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；PGM `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

工作区三文件 blob 与 after 树全等（`hash-object` = `git ls-tree after`）：V430 `385b7fb4…`、ControllerTests `b183a061…`、JdbcTests `da5b59ed…`。`AccessController.java` 两 tree 与工作区同 blob `5524ed40…`，diff 0 字节。无 merge/rebase。V430 工作区为未跟踪 `??`，两测试类为 ` M`，符合 B3 未提交落盘。

---

## 2. diff 逐块归因表（§5 闸 3）

闸 3：每一行必须归因到 findings.md V3.09 已定稿且归属「是」的 ID。本批仅 001、002、003。归因不到即越界 → 整轮 FAIL。

`git diff --name-only --binary <before> <after>` 恰好 3 路径，无第四文件。

| 块 | 路径 | 模式 | ± | 归属 | 理由 |
|---|---|---|---|---|---|
| A | `backend/src/main/resources/db/migration/V430__permission_names_no_latin_ai.sql` | 新文件 `100644`（非可执行） | +8 / −0 | **M035-001** | 定稿①②之②：前向迁移 `permission_name` 内「AI」→「人工智能」；`permission_code` 不出现在 SQL；不回改已应用迁移。4 行注释是 001 的依据说明，不是新业务规则 |
| B1 | `AccessControllerTests.java` `listPermissionsReturnsApiResponsePageShape` | 100644 保持 | +1 | **M035-002** | 三条成功路径之一加 `verify(authorizationService).requirePermission("access.permission.view")`，码与控制器 L88 一致 |
| B2 | 同文件 `listStaffLevelsReturnsApiResponsePageShape` | 100644 保持 | +1 | **M035-002** | `verify` `access.staff_level.view`，与控制器 L258 一致 |
| B3 | 同文件 `assignStaffLevelReturnsOkWithAssignment` | 100644 保持 | +1 | **M035-002** | `verify` `access.staff_level.manage`，与控制器 L267 一致 |
| B4 | 同文件文末三新测试 | 100644 保持 | +70 | **M035-002** | 定稿补三条：lifecycle `approve_delete` 校验复数 `requirePermissions` 双码 + `archive` 校验单数 `requirePermission`；grant-review 对复数方法 `doThrow` 期望 403 且 `never().reviewGrant`；职级+定岗请求体 `user_id=usr_body`、`ArgumentCaptor` 断言服务层收到路径 `usr_path`。未铺其余入口矩阵 |
| C1 | `AccessJdbcIntegrationTests.java` 原 `staffLevelChangeRunsInOneTransactionLikePositionAssignment` | 100644 保持 | +4 / −3 | **M035-003** | 只改注释与方法名为如实表述；两条 `getTransactionAttribute(…).isNotNull()` 与 before 树逐字相同 |
| C2 | 同文件 `seededDisplayNamesAreChinese` | 100644 保持 | +3 / −2 | **M035-001** | 去掉 `replace(permission_name, 'AI', '')` 与注释豁免句；角色名／模板名断言未改 |

行数核对：8+1+1+1+70+4+3 = 88 insertions；3+2 = 5 deletions。与 `--stat` 一致。无剩余行。

**未出现、故不构成越界的路径（本席主动核「有没有多出来」）**：`AccessController.java`、`AccessService.java`、`AuthorizationService.java`、已应用迁移 V017／V096／V251／V429、三份需求 HTML、lock／sidecar／ledger／findings、前端、OpenAPI、隔离规格文档、其它测试类。B2 写入集合曾**允许**检查点里隔离规格自动区块机械 1 行；本 tree 对未纳入。缺席不是越界；定稿表 001 最小处置是测试去豁免 + V430，规格重生在 B2 写为检查点／生成器拥有区域（且本席 B2 已注明 M034 B3 tree 也未带）。本轮不因「少写允许项」否决。

**归因票：PASS。** 88/88 插入行、5/5 删除行均可归到 001／002／003；无无 ID 行。

---

## 3. 内容审计

### 3.1 M035-001（冲突／是／是）

定稿最小处置：① `seededDisplayNamesAreChinese` 去掉 `replace(…,'AI','')` 与注释豁免句，对任何拉丁字母都红；② 同批新增 V430：`UPDATE permissions SET permission_name = replace(permission_name, 'AI', '人工智能') WHERE permission_name ~ 'AI'`；`permission_code` 不动；已应用迁移不回改。

**测试闸。** after 树 SQL 为 `WHERE permission_name ~ '[A-Za-z]'`。注释删除「HTML 正文自身使用的『AI』除外」，改为引用治理地图「没有第五样」。角色名／模板名两条 `isEmpty()` 断言未改。符合①。

**迁移。** 磁盘与 after blob 全文：

```
-- V430：M035 审计（2026-09-21）001。
-- …历史迁移不回改… permission_code 是机读标识，不变。
UPDATE permissions
   SET permission_name = replace(permission_name, 'AI', '人工智能')
 WHERE permission_name ~ 'AI';
```

与定稿 SQL 逐字同构（仅换行）。无 `permission_code` 赋值。无对 V251／V017／V096／V429 的回写。下号：before 树已有 V429、无 V430；after 新增 V430，序号正确。

**PGM-001。** 锁内原文（criteria-lock 约 L5034）：展示给用户的一切文字「必须全部为中文，不得出现任何英文」；可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。权限**展示名**不是这四样。`wecom.ai_qc.manage` 等码是系统内部标识符，保留拉丁合法。

**「人工智能」是否业务发明。** 本席 B2 已票：同义直译，三 HTML 已用该中文词，不须 2.2。本 diff 未新造权限／状态／流程。维持。

**红绿。** B3 日志：修前 `latin_permission_names=2`（两码含「AI」）、Flyway 429；只落测试补丁后 JDBC 26 测 1 失败，失败体即 `seededDisplayNamesAreChinese` 得到 `analytics.risk_alert.view`、`wecom.ai_qc.manage`；Controller 16 测全绿。加入 V430 后 GREEN 16+26 全绿，Flyway 430。本席独立：`psql` 只 SELECT 得 `flyway_top=430`、`v430_success=1`、三类展示名拉丁计数均 0、`permission_name ~ 'AI'` = 0、`permission_code` 含 ai 仍 10（机读码未改）、两码现名＝「…人工智能质量台账」「…人工智能语义质检…」。本席重跑 `mvn -o test -Dtest='AccessControllerTests,AccessJdbcIntegrationTests'`（三变量导出、库名 `complex_test`、**未** `source /etc/complex/complex.env`）：Tests run 16+26=42，Failures 0，BUILD SUCCESS，exit 0。测后 Flyway 仍 430。

**禁止项未犯。** 未把两码加入测试白名单；未改前端／契约；未清授予表；未改三 HTML。

001 内容：**PASS。**

### 3.2 M035-002（技术性／是／是）

定稿最小处置（只改该测试类，不改被测代码，不铺 33 入口矩阵）：补三条——lifecycle（`approve_delete` 校验复数方法双码、`archive` 校验单数方法）、授权审批（对复数方法打桩被拒 403 且不进服务层）、职级与定岗（请求体带别人的 `user_id`，ArgumentCaptor 断言路径 id）；三条成功路径各加一句 `verify` 对应码。本席 B2 边界：双码必须打在 `requirePermissions` 上；`reject_delete` 与 `approve_delete` 同一分支，测其一即可。

**被测代码未改。** `AccessController.java` before=after=工作区 blob `5524ed40`。符合「不改被测代码」。

**三条成功路径 verify。** 码分别为 `access.permission.view`／`access.staff_level.view`／`access.staff_level.manage`，与控制器 L88／L258／L267 及定稿列举一致。删掉这三处 `requirePermission`，verify 会红（Mockito 默认至少一次）。

**生命周期。** `approve_delete`：`verify.requirePermissions("access.role.assign", "governance.supervisor_review.approve")` 且 `never().requirePermission("access.role.assign")`。`archive`：`verify.requirePermission("access.role.assign")` 且 `never().requirePermissions(双码)`。中间 `Mockito.reset`。控制器 L195–199：`approve_delete`／`reject_delete` 走复数双码，其余（含 archive）走单数。只测 approve_delete、不测 reject_delete，符合 B2「测其一即可」。`never()` 是锁 if/else 分支、不是矩阵铺开。

**授权审批。** `doThrow` 打在 **`requirePermissions("access.grant_request.review", "governance.supervisor_review.approve")`**（复数，非单数冒充）；期望 403 + `detail_code=PERMISSION_DENIED`；`verify(service, never()).reviewGrant(any(), any())`。与控制器 L153–155 一致。未 stub 单数方法。

**职级与定岗。** 两 PUT 路径 `/users/usr_path/staff-level` 与 `/users/usr_path/position`，JSON 带 `"user_id": "usr_body"`；Captor 断言 `userId()=="usr_path"`。与控制器 L269–272／L288–291 用路径 `id` 重建请求一致。两处合在一条测试，仍是定稿点名的两处，不是 20 入口样板。

**未改被测即绿。** B3 RED 阶段 Controller 16/16 全绿（三条新测试已在、V430 未入）。这是 002 的正确形态：锁的是**现有**门控，不是先改控制器再补测。TDD「立刻绿＝在测既有行为」在此是定稿要求，不是假绿。

**断言能否失败（变异）。** 既有一次性 worktree 日志（本席打开全文关键失败段，行号对 after 树）：把控制器两处双权限改单权限、两处路径 id 改采信 `request.userId()` 后，三条新测试全红（16 测 3 失败 0 error）：

- `positionTemplateLifecycleGatesDeleteDecisionsOnBothPermissions:323` Wanted `requirePermissions(双码)` but invoked `requirePermission("access.role.assign")`
- `grantReviewRequiresBothReviewAndSupervisorApprovePermissions:352` Status expected 403 but was 200（单数调用未命中复数 stub，走进服务层）
- `staffLevelAndPositionEndpointsUseThePathUserIdNotTheBodyUserId:369` expected `usr_path` but was `usr_body`

worktree 已还原移除；主工作区控制器 blob 未变。本席未另建 worktree 重放（避免额外产物）；行号 323／352／369 与 after 树三处断言逐行对齐，失败信息与断言种类一一对应。三条成功路径的 `verify` 未进该次变异，但 `Mockito.verify` 在门控被删时必红，静态可失败。

**禁止项未犯。** 无 AccessController／AccessService 改动；无其余 20 入口 403 样板；双码未改打单数方法。

002 内容：**PASS。**

### 3.3 M035-003（技术性／是／是）

定稿：只改测试名与注释，如实写成「断言 assignUserPosition 与 assignStaffLevel 两个入口声明了事务」；不新造四步真库测试（行为在 M036）；不动两条 `getTransactionAttribute` 断言。

**断言。** before／after 两条 `assertThat(…getTransactionAttribute(AccessService.class.getMethod("assignUserPosition"|"assignStaffLevel", …), AccessService.class)).isNotNull()` 逐字相同。

**名称。** `staffLevelChangeRunsInOneTransactionLikePositionAssignment` → `positionAndStaffLevelEntryPointsDeclareATransaction`。不再声称「四步同一事务已被真库锁住」。

**注释。** 改为「本测试只断言两个入口带事务声明，不执行四步写入本身（四步路径的行为测试在 AccessServiceTests）」。`AccessServiceTests.java` 是计划 M036 的三个文件之一，比「M036」更具体。本席在该文件未搜到「四步／套模板／替换派生」字样——父注是模块指针而非「本测试覆盖四步」的声称；与「不新造四步真库测试」同向。不作为否决。

003 内容：**PASS。**

### 3.4 可执行位与文件模式

| 路径 | before | after | 磁盘 |
|---|---|---|---|
| AccessControllerTests.java | `100644` blob `1b4ea7d7` | `100644` blob `b183a061` | 664（无 exec 位；git 仍 100644） |
| AccessJdbcIntegrationTests.java | `100644` blob `9716ded5` | `100644` blob `da5b59ed` | 664 |
| V430__permission_names_no_latin_ai.sql | （不存在） | **new file mode 100644** blob `385b7fb4` | 644 |
| AccessController.java | `100644` `5524ed40` | 同 | 未改 |

无 `100755`、无 symlink、无 binary。`--summary` 仅 `create mode 100644` 一处。模式票：**PASS。**

### 3.5 业务发明 / 越界 / ponytail

- 无新权限码、无新状态机、无新审批规则。「人工智能」是展示名直译，B2 已排除 2.2。
- 无「顺手优化／换框架／补抽象」。
- ponytail-review：`never()` 锁分支、Captor FQCN 代替 import、V430 注释——均服务定稿，无可删越界。隔离规格本 tree 未带，更短。**Lean already.**

内容票：**PASS。**

---

## 4. 对抗式自问（grilling 结构，自答）

1. **002 新测试在未改被测时即绿，是不是假绿？** 否。定稿禁止改被测代码；绿表示现有门控已正确。假绿反例是「删掉门控仍绿」——变异把双码改单码／路径改采信请求体后三条全红，反例成立于相反方向。
2. **只 stub 单数 `requirePermission` 会不会让双码测试假绿？** 本 diff 没有这样做。grant-review 的 `doThrow` 与 lifecycle 的 `verify` 都打在 `requirePermissions` 上。`AuthorizationService.requirePermission` 真身转调复数，但测试里是 mock，两方法不自动串联；打错方法锁不住。变异把复数改单数后面 403 变 200，证明打在复数上是负载的。
3. **V430 blanket `~ 'AI'` 会不会误伤其它行或改码？** SELECT：改后 `permission_name ~ 'AI'` = 0；两目标码展示名已是「人工智能…」；`permission_code` 含 ai 仍 10。GREEN 后拉丁展示名 0。二次执行 Flyway 不会重跑；字面 `人工智能` 不含 `AI`，幂等。
4. **缺隔离规格 1 行是否 001 不完整？** 定稿表最小处置是测试 + V430。B2 把规格重生列为检查点允许项，并写明 M034 B3 实际未带。本 tree 与 M034 同形。不否决。
5. **lifecycle 的 `never()` 是否越界矩阵？** 否。它只区分定稿点名的 if 双码／else 单码，没有对其余 20 入口加测。
6. **003 注释指向 AccessServiceTests 是否 GEN-016 再过声称？** 该文件属 M036；本测试明确「不执行四步」。指针精度高于「M036」模块号。不否决。

---

## 5. 本席未做 / 缺口

- 未读 `M035-b4-r1-self-raw.md`。
- 未 fetch／commit／push／merge／deploy。
- 未改业务／HTML／锁／findings／ledger。
- 未对数据库发写语句（重跑既有测试类除外；测后 SELECT 仍 430）。
- 未派 MCP／子代理／第三席。
- 未新建持久脚本或审计产物。
- 变异未本席重放 worktree（既有日志行号已与 after 树对齐）。
- `batch-grill-me`／`ubiquitous-language` 本机不存在。
- Arc `audit`／Trail of Bits 差分审／requesting-code-review 子代理路径故意不用。

---

## 6. 票面

| 维度 | 票 |
|---|---|
| 内容（是否按定稿最小处置与 B2 边界落地；断言能失败；无业务发明；模式未变） | **PASS** |
| 归因（§5 闸 3：逐行归到 001／002／003） | **PASS** |
| 合取 | 内容 FAIL 或归因 FAIL 即 FAIL；两者均 PASS |
| **本席 B4-r1 总结论** | **PASS** |

不是模块 PASS，不是 B5，不是授权收口。双席合取后才进 B5。本席 FAIL 否决权本轮不行使。

环境：`y9000p-ai`；正本 `/home/james/complex-management`；日期 2026-09-21。
