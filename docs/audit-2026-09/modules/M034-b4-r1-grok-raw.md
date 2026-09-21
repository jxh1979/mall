CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M034 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M034-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M034 / 共 471，B3 第 1 次之后的 B4。计划名：功能权限点、职级三十档、权限模板与最终权限计算／数据库迁移／第1段。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M034-b4-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3／§6、`findings.md` V3.06 顶部「当前 M034 B2-r1 双PASS定稿」、本席旧 `M034-b1-grok-raw.md` V0.01/V0.02（B1+B2，允许）、席甲旧 `M034-b1-self-raw.md` 仅确认其作为 B2 输入存在而未把席甲 B4 当证据、冻结 diff 与两 tree、现行 V429 SQL、AccessJdbcIntegrationTests 新例与类上 `@SpringBootTest`/`@Transactional`、V270 原文、V003 表结构（`permissions.status` / `temporary_permission_grants.revoked_by`）、`JdbcAccessRepository.permissionExists`／`listPermissions`／`findPermissionEffects`、`AuthorizationService.permissionGrant`／`knownPermission`、V290 retired 先例、HTML 枚举「核销记录状态」原文、矩阵 L5/L63（注释引用，文件本身不在 diff）、`application.yml` flyway 块、B3 日志、本席对 `complex_test` 的只读 SELECT。ledger 只读取模块状态与 B3 次数；其中 after tree 之后写入的席甲 B4 封存行不构成本席证据。本票以两 tree 全文 diff、定稿最小处置、本席 B2 边界、本席只读探针与既有 RED/GREEN 日志为准。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；M034-001 是否按 findings V3.06 定稿最小处置与本席 B2-r1 边界实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到归属「是」的 ID（仅 001）；迁移 SQL 是否正确、可在未跑过 V429 的库上首次执行成功；注释是否与 SQL 一致；可执行位与文件模式是否未变。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读；技能本体「问人 / 派子代理查事实」作废）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：本轮只核 B3 已落的红绿证据并本席只读复核，不新写测试。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`git-guardrails-claude-code`：存在；本席未 push/reset/clean。`i-have-adhd`：存在；与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。`long-running-background-tasks`：本席无后台作业。`graft`：本环境存在，已用于定位 `permissionExists` / `findPermissionEffects` / `revoked_by`。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep` 定位 `permissionExists` / `revoked_by` / `findPermissionEffects` | 无 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 字典 status 与四张授予表是深模块接口；本 diff 不把第二套判定写进 Java | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（三工具 sha256、`cmp` 冻结 diff、schema/终态 SELECT、读 RED 违例行） | 本席环境未预导出三变量；禁止 `source /etc/complex/complex.env`；未本席重跑 JDBC（会写库） |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（定稿声称→V429 DML→`permissionExists`/`findPermissionEffects`）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实跑哈希/`cmp`/只读 SELECT；JDBC 26 例不以「应能过」代替，改核 RED/GREEN 日志+库实态 | 本席不重跑会写库的 26 例，缺口见 §1 |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `tdd` / `test-driven-development` | 存在 | 核 RED 构造器成立（无 V429 时 `permissionExists` 仍 true）+ GREEN 26/26；本席不重写测试 | 纯 SQL 本可不强制测；本轮按定稿走跨单元真库断言，合法 |
| `long-running-background-tasks` | grok bundled | 本席无后台作业 | 节点不适用 |
| `git-guardrails-claude-code` | mattpocock misc | 只读；未装 hook | **存在但未安装** |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |
| `resolving-merge-conflicts` | 存在 | 工作区无 merge/rebase | 节点不适用 |
| `writing-for-agents` | 存在 | 本 raw 给下游合取席 | 无 |
| `ponytail` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail/SKILL.md` | B4 只审：5 条 DML + 1 例复用夹具，ultra 已满足 | 不修 |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。未使用 MCP。未建脚本/持久日志/临时审计产物（复核用 `/tmp/m034_b4_r1_verify.diff` 在 `cmp` 后已删）。

graft 本席调用节省合计约 441374 tokens（`permissionExists` / `revoked_by` / `findPermissionEffects`）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，与本文件第一行独占锁行一致。文件 889020 字节。sidecar 一行同值。ledger 顶部基准同值。criteria-lock 版本仍 V0.11。LOCK-AMEND-COUNT 仍为 1。findings.md 版本 V3.06，本席 `sha256sum` = `44109d038529bf0e948f52df99a12e3fb7da9af8d489c81c1f2585ff3191428e`。160-audit-prompt.md V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`。本席 B1+B2 raw `M034-b1-grok-raw.md` 42982 字节（与定稿段所引 SHA `8605cc6fe59dd9057b8cf8b64fe6b749bc8bd4760fde59a024411ff28f4f09fc` 的体积相符；本步未重算该旧文件哈希作锁）。

| 项 | 值 |
|---|---|
| 模块 | M034 数据库迁移／第1段（计划 13 文件，有效内容行 690；本轮 B3 写入集合＝新迁移 + 别模块真实库测试类 1 例） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V3.06：M034-001 冲突／是／是；修复队列「否」0 |
| B3 | 已执行 1 次；模块仍 IN_PROGRESS |
| before（对象类型 tree） | `0b52ef063c92fced6cb33ac28c30c67b314d06bb` |
| after（对象类型 tree） | `6a961e93046d113f530b5492170ff0c91398d79f` |
| `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 0b52ef063c92fced6cb33ac28c30c67b314d06bb 6a961e93046d113f530b5492170ff0c91398d79f`（无路径过滤） |
| 实际差异 | 3630 bytes；SHA256 `076be04f4c948bc7c8d630ed0066237e66ca210b838463d89f64d93e8710c3ca`；2 files changed, 31 insertions(+)；2 hunk；UTF-8 |
| 与冻结文件 `/home/james/audit-tmp/m034_b3r1.diff` | 字节、SHA256、`cmp` 全等 |
| `git diff --check` | exit 0 |
| `git diff --name-status` | `A` V429 SQL；`M` `AccessJdbcIntegrationTests.java`；无 D/R；无第三路径 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（两改动文件工作区 blob 与 after tree 全等） |
| 计划 13 个已应用迁移 | 两 tree blob **逐文件全等**（见下表）；`git diff --stat` 对这 13 路径为空 |
| 三 HTML | 两 tree blob 全等；本 diff 路径清单为空 |
| `application.yml` flyway 块 | 两 tree blob 全等 `358b31941138ee671cbb4028895ada43b3c111bf`；`validate-on-migrate: true`；yml 树无 `outOfOrder` |
| V427（M009 遗留，staged） | 两 tree 均含且 blob 同为 `e485a5d29fea55fa714091485aa26facc8c45160`，故不出现在本 diff |
| V428（M021，origin/main 已有、主工作区 HEAD 未跟踪） | 两 tree 均含且 blob 同为 `5920e7d53d41952129085f4626e1baaf82c4c6af`，故不出现在本 diff。日志「untouched applied migrations: 1」与此相符，不是本次改动 |
| TDD | RED/GREEN 摘自 `/home/james/audit-tmp/m034_b3r1_run.log`（2265 bytes，SHA256 `bc4c0fa65ffe2ded71f6e78066c31f5abed07fa69cc576808f27aa8c45c9fb93`）：RED `MVN_EXIT=1`，26 测 1 失败 `manualOverridePermissionIsRetiredAndGrantedToNobody` / `Expecting value to be false but was true`，当时 Flyway 最高 428、该码 active、role_permissions 1；GREEN `MVN_EXIT=0` 26/26，Flyway 429、该码 retired、四处授予 0、平台管理员缺的 active 码 0；同批回归 AccessJdbcIntegrationTests 26、AuthorizationServiceTests 24、TicketingControllerTests 20 全绿 |
| 只读探针 | 本席未 `source /etc/complex/complex.env`（该文件 URL 指向生产库 `complex`）。只抽三键后把库名改 `complex_test` 再 SELECT。本席**未**查询生产库，**未**对任何库发写语句 |

计划 13 文件两 tree blob（`git rev-parse TREE:path`）：

| 文件 | before = after blob |
|---|---|
| `V003__p0_access_permission_authz_slice.sql` | `a13ebe87a1a2e5a8fb160eac6980a4cd4cd84812` |
| `V092__amusement_operational_config_permissions.sql` | `a96ae0d47d6633f9ff196b53c562338cda95d6f6` |
| `V158__refund_authority_admin_permissions.sql` | `a3d8e7aec9de47504b6118c84e93247f0e15e55a` |
| `V169__access_role_assignment_closure.sql` | `771b88fb45b8b965943b29be431adda6ed739f74` |
| `V180__auditor_roles_readonly_seed.sql` | `30d20349c4a0a672113a859b2ff83b44b94a6ba2` |
| `V204__seed_runtime_checked_permissions.sql` | `c139628c77448f727ec9b0c35103807ece78eec4` |
| `V261__platform_admin_full_permissions.sql` | `8d8c308da688090f0fac1efd47a5dc33826d7e20` |
| `V270__ticket_manual_override_permission.sql` | `2cd96bc12a8e30c102ec13911c8c7a09e1f69508` |
| `V321__staff_self_profile_permission.sql` | `5d84aa019af4e5378c1ad25d50a9356d071f5c1b` |
| `V322__staff_self_service_role.sql` | `934fdc06eb759fc057cea53c6268cb77dbe5b4d6` |
| `V387__access_display_names_chinese.sql` | `4697df5390b6156bfa5c4f21ed619194df8b843c` |
| `V393__sensitive_business_data_permissions_require_approval.sql` | `3c04a565457e5a71110249d93033834b1becfedf` |
| `V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql` | `228692f19ceb47abfb0e92148a8de622daf4b436` |

两改动文件 blob（`git rev-parse TREE:path` / 工作区 `git hash-object`）与内容 SHA256：

| 文件 | before blob | after / work blob | 行 | 内容 SHA256 | git mode |
|---|---|---|---:|---|---|
| `backend/src/main/resources/db/migration/V429__retire_verification_manual_override.sql` | （不存在） | `ac3c4bb658cff3051250ad0b13a46fe06a495b9f` | 17 | `903331b27bd536cdce11c3abb3547f4ec77b1c4a0f07408cf2a706c6b3d02078` | 100644（新文件；与其它迁移同模式，非可执行） |
| `backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java` | `0dbc4fecd64528b41df307668c304ad898b36bbd` | `9716ded51702a09b8ccea3717124cebb850ced60` | 873 | `291e657bc4363c3b202b08c3b02b88de032911d46f762fdb19828596d898ff96` | 100644→100644（未变，非可执行） |

before 测试文件内容 SHA256 `ba1d17c374af1bb0693937b20a78b776f4c2db70c52a66c0a1196d69f163e282`（25 个 `@Test`）。after 26 个 `@Test`。工作区 `stat` 测试文件为 `664`、V429 为 `644`；git 只记 `100644`/`100755`，两 tree 均为 `100644`，可执行位未变。工作区 664 不进入 tree diff。

本席未改任何源文件，只新建本 raw。未 commit/push/PR/merge/部署；未对主索引 `git add`。

---

## 2. 锁、分类与本轮队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M034 的集合（GEN-001…018）。本轮只用定稿 001 已引用的 ID（GEN-005 / GEN-017；上位 HTML 枚举「核销记录状态」原文）。不 LOCK-AMEND。四分类本轮不重开。

B2-r1 定稿（findings.md V3.06）最小处置 + 本席 B2 边界（本席内容票的尺子）：

- **001** 冲突／是：已应用 V270 与本模块其余 12 个迁移不可改写 → 新增前向迁移 `V429__retire_verification_manual_override.sql`：①`UPDATE permissions SET status='retired'`（该码）；②`DELETE` 该码在 `role_permissions`、`permission_template_items`、`user_permission_overrides` 的行；③该码仍 `active` 的 `temporary_permission_grants` 置 `revoked`（`revoked_at`/`revoked_by` 有列可顺带填，不是入队条件）。TDD 在既有真实库类 `AccessJdbcIntegrationTests`（M035，PENDING）补一条真库断言：该码非 active 且四张授予表 0 行（临时授权计 active）。一次 B3 只这两个文件。禁止：回写 V270；`DELETE FROM permissions`；改鉴权 Java；在本模块删端点/契约/前端/Ticketing 测试；清 `audit_events` / 用量日志；动 V427 或其它未发布迁移。端点下线属 XM-M034-A，不在本 diff。编号：origin/main 最大 V428，故取 V429。

---

## 3. diff 逐块归因（闸 3）

2 hunk，31 条插入行，全部能绑到 V3.06 归属「是」的唯一 ID **M034-001**。无删除行、无无 ID 行、无第三路径。验证性测试归因到对应 ID（闸 3 明文允许）。git 头 `new file mode 100644` 与测试文件保持 `100644` 是文件模式核验对象，不是业务行。

| # | 文件 | hunk | 变更行（摘要） | ID | 越界? |
|---|---|---|---|---|---|
| 1 | `V429__retire_verification_manual_override.sql` | `@@ -0,0 +1,17`（新文件 mode 100644） | L1 `-- V429：M034 审计（2026-09-21）001。` | 001 | 否 |
| 1 | 同上 | 同上 | L2–L6 注释：枚举「系统不提供人工放行…」原文、矩阵自 R10 标废、V270 种入并授平台管理员、不回改历史迁移、判定先看字典 active、汇集不联字典故四处授予要清、端点下线属票务模块 | 001 | 否 |
| 1 | 同上 | 同上 | L8 `UPDATE permissions SET status = 'retired' WHERE permission_code = 'verification.manual_override';` | 001 | 否 |
| 1 | 同上 | 同上 | L10–L12 `DELETE FROM role_permissions` / `permission_template_items` / `user_permission_overrides` WHERE 该码 | 001 | 否 |
| 1 | 同上 | 同上 | L14–L17 `UPDATE temporary_permission_grants SET status='revoked', revoked_by='V429', revoked_at=now() WHERE` 该码 AND `status='active'` | 001 | 否 |
| 2 | `AccessJdbcIntegrationTests.java` | `@@ -644,6 +644,20`（文件 mode 100644 未变） | L647–L648 注释：M034-001 与枚举否定句 | 001（跨单元 TDD） | 否 |
| 2 | 同上 | 同上 | L649–L659 `@Test void manualOverridePermissionIsRetiredAndGrantedToNobody`：`permissionExists` 为 false；四表 count 之和为 0（临时授权只计 `status='active'`） | 001 TDD | 否 |

无第四路径、无重命名、无对计划 13 文件的 hunk。XM-M034-A 所需的票务端点/服务/契约/前端不在 diff 内（归属跨模块，不动是对的）。

---

## 4. 内容审计

### 4.1 前向迁移是否按定稿最小处置

V429 全文 17 行（UTF-8，LF，1360 字节，无 NUL、无 CRLF）。可执行语句恰好五条：

```
UPDATE permissions SET status = 'retired' WHERE permission_code = 'verification.manual_override';
DELETE FROM role_permissions WHERE permission_code = 'verification.manual_override';
DELETE FROM permission_template_items WHERE permission_code = 'verification.manual_override';
DELETE FROM user_permission_overrides WHERE permission_code = 'verification.manual_override';
UPDATE temporary_permission_grants
   SET status = 'revoked', revoked_by = 'V429', revoked_at = now()
 WHERE permission_code = 'verification.manual_override'
   AND status = 'active';
```

对照 B2 允许/禁止表：

| 定稿要求 | 实核 |
|---|---|
| 该码 `permissions.status='retired'` | 是；`WHERE` 仅该码 |
| `DELETE` 三张行表该码 | 是；各一条等值 WHERE |
| 仍 active 的临时授权置 `revoked` | 是；`AND status='active'`，不删行 |
| `revoked_by`/`revoked_at` 可顺带填 | 填了 `revoked_by='V429'`、`revoked_at=now()`；列可空、无 FK，属 B2 明示允许，不是入队条件也不是越界 |
| 不改 V270 与其余 12 个已应用迁移 | 13 文件两 tree blob 全等 |
| 不 `DELETE FROM permissions` | 全文无 |
| 不清 `audit_events` / 用量日志 | 全文无这两表 |
| 不动 V427 / 其它未发布迁移 | V427/V428 两 tree 同 blob，不在本 diff |
| 不改鉴权 Java / 票务端点 | `git diff --name-only` 对 `backend/src/main/java/**`、Ticketing 测试、三 HTML、矩阵文档为空 |
| 一次 B3 只这两个文件 | `name-status` 恰 A+M 这两条 |

ponytail-review：5 条 DML，复用既有列与 `revoked` 词。Lean already. Ship. `revoked_by='V429'` 可删（B2 说不是入队条件），留下也不构成越界或发明；本席不因此 FAIL。

### 4.2 SQL 是否正确；未跑过的库上首次执行

静态结构（V003/V002，两 tree 未改）+ 本席 `complex_test` 只读 catalog：

- `permissions.status` `varchar(32) NOT NULL`；**无** status CHECK（仅 `chk_permissions_risk_level`）。`retired` 先例：V290 三条 `customer.membership.*`，本席 SELECT 现仍为 retired；本迁移后该码成为第 4 条 retired。
- `temporary_permission_grants.status` `varchar(32) NOT NULL`，无 CHECK。Java `revokeTemporaryGrant` 已写 `status='revoked'`。`revoked_by varchar(128)` 可空、无 FK；`'V429'` 4 字符。`revoked_at timestamptz` 可空；`now()` 在 PG 返回 timestamptz。
- FK：`role_permissions.permission_code`、`permission_template_items.permission_code`、`user_permission_overrides.permission_code` 均 REFERENCES `permissions(permission_code)`（无 ON DELETE CASCADE）。V429 **不删**权限行，只改 status，不触父键。随后 DELETE 的是子行。顺序（先 retired 再删子行）与 V290（先删 `role_permissions` 再 retired）相反，在无 status 触发器的前提下等价合法。本席 SELECT：这五张表 **0 条**用户触发器。
- `temporary_permission_grants.permission_code` 无指向 `permissions` 的 FK（V002 建表），UPDATE 不依赖父行。
- 无行时 UPDATE/DELETE 影响 0 行，语句仍成功——从未种过该码的库首次跑 V429 不会炸。
- 正常全量升级路径：V270 先 INSERT 该码 `active` 并授 `role_platform_admin`（`ON CONFLICT DO NOTHING`），其后 V429 再 retired＋清授予。版本号 429>270，Flyway 按序执行。
- 文件名 `V429__retire_verification_manual_override.sql` 匹配 `^V\d+__[a-z0-9_]+\.sql$`。`origin/main` 最大版本文件 V428（检查点 `148ae368`）。V429 > origin/main 当前最大 V 号。`validate-on-migrate: true`，无 out-of-order。V429 不读 V427 对象。
- 本席不在 `complex_test` 上重放 DML（库已是 429，重放不是「未跑过」；写语句本轮禁止）。首次成功的证据链：①语句对空集/对 V270 终态都是合法 DML；②catalog 无拦阻 CHECK/触发器/类型不匹配；③B3 日志 GREEN：Flyway `now at` 429 且 26/26；④本席 SELECT `flyway_schema_history` `429 | retire verification manual override | t | 832923190`。

### 4.3 注释是否与 SQL 一致

注释声称：不回改 V270；该码停用；四处授予来源要清；判定先看字典是否 active（含最高权限）；汇集不联字典状态；端点下线属票务模块。SQL 实际：`retired` 该码、DELETE 三张行表、active 临时授权 `revoked`，无端点/Java。注释用「清」覆盖 DELETE 与 revoke 两种形态，与 B2「三表 DELETE、临时授权置 revoked」同向，并未写成 `DELETE FROM temporary_permission_grants`。无注释承诺却未落地的语句，无 SQL 多出来的表。

### 4.4 测试是否按真实现语义

新例 `manualOverridePermissionIsRetiredAndGrantedToNobody`：

| 定稿要求 | 实核 |
|---|---|
| 真库 | 类 `@SpringBootTest`，URL 写死 `jdbc:postgresql://127.0.0.1:5432/complex_test` |
| 该码非 active | `repository.permissionExists("verification.manual_override")` → `JdbcAccessRepository` `WHERE permission_code=? AND status='active'`；retired 后为 false |
| 四张授予表 0 行 | `role_permissions` + `permission_template_items` + `user_permission_overrides` 全表 count + `temporary_permission_grants` **仅 active**；与「临时授权是 revoke 不是 DELETE」一致。若改成对临时授权 `count(*)` 不计 status，revoke 后会假红 |
| 一条断言、不改其它用例 | hunk 只在 L644 后插入 14 行；`@Test` 25→26；无新 import；不改 `@AfterEach` |
| RED 先于迁移 | 日志：尚无 V429、Flyway 428、该码 active 时，失败句是 `Expecting value to be false but was true`——即第一句 `permissionExists` 为 true，与「只落真库断言」相符 |
| GREEN | 日志 26/26；本席 SELECT 终态与断言同构：`still_active_in_dictionary=f`，四处 count 全 0 |

跨单元 TDD：该类不在 M034 计划 13 文件内，属 M035 PENDING。定稿与派发口径写明合法（先例 M021 改结算测试、M015 改 M017）。

`AuthorizationService.permissionGrant` 先 `knownPermission`（即 `permissionExists`）再日常桥/最高权限，故 retired 后任何主体 `requirePermission("verification.manual_override")` 均为 `PERMISSION_DENIED`。`findPermissionEffects` 五段均不联 `permissions.status`，故三 DELETE + active 临时授权 revoke 不是多余。`listPermissions` 只列 active。本 diff 不改这些 Java——正是「最小处置在迁移、不冒充改鉴权层」。

### 4.5 可执行位与文件模式

- 新文件：`new file mode 100644`，与 `V270` 及目录内既有迁移同为 `100644`。
- 测试文件：before/after 均为 `100644`（diff 头 `index 0dbc4fec..9716ded5 100644`，无 mode 行变更）。
- 无 `100755`、无 symlink。可执行位未变。

### 4.6 无业务发明、无越界

001 是删除 HTML 已禁止的能力在字典与授予面上的入口，不是发明新能力。未改三 HTML、未改矩阵文档、未改锁。未把「人工放行」换成另一种放行。未清留痕表。未在本模块下线端点（XM-M034-A）。平台管理员缺的 active 码本席 SELECT 仍为 0（active 376→375，恰退休 1 码）。

### 4.7 库终态（本席只读 SELECT，`current_database=complex_test`）

| 探针 | 结果 |
|---|---|
| Flyway 最高 | 429 success=t；427/428/429 皆 t |
| V270 | `270 \| ticket manual override permission \| t \| -543191715`（仍 success，未改写已应用行） |
| V429 | `429 \| retire verification manual override \| t \| 832923190` |
| 该码 | `verification.manual_override` / `retired` / `high` / `requires_approval=t` / 名「人工放行票务核销」——行仍在，符合「只增不删」 |
| 四处授予 | role_permissions=0, template_items=0, overrides=0, temp_all=0, temp_active=0 |
| permissions.status 分布 | active 375 / inactive 2 / retired 4（含三条 membership + 本码） |
| 平台管理员缺的 active 码 | 0 |
| 字典仍 active？ | false |

本席未重跑 26 例 JDBC（类上 `@Transactional` 仍会在其它用例写库）。终态由 SELECT 钉住；RED/GREEN 采 B3 日志。工作区无残留 surefire XML。

---

## 5. 对抗式自问（grilling 结构，不等人）

1. **Q：`revoked_by='V429'` 不是用户 id，会不会因 FK/CHECK 让未跑过的库首次失败？**  
   A：否。列 `varchar(128)` 可空，无 FK、无 CHECK。B2 写明可顺带填。Java 撤回临时授权同样写字符串。

2. **Q：先 `UPDATE permissions` 再 DELETE 子行，FK 会不会拦？**  
   A：FK 绑的是 `permission_code` 主键，不是 status。不删父行。五表无用户触发器。V290 反向顺序也合法；本顺序不构成首次失败。

3. **Q：测试只断言 `permissionExists` false，会不会把误删权限行也判绿？**  
   A：会。定稿测试口径是「非 active」而非「必须仍在且为 retired」。SQL 留下该行（本席 SELECT 仍在）。闸 3 不要求测试比定稿更严。不升 FAIL。

4. **Q：四表「0 行」对临时授权只计 active，是不是测松了？**  
   A：否。SQL 对临时授权是 revoke 不是 DELETE；全表 `count(*)` 会在「曾经有过该码临时授权」的库上假红。与 `findPermissionEffects` 的 `status='active'` 过滤对齐。

5. **Q：日志「untouched applied migrations: 1」是不是本 diff 改了已应用迁移？**  
   A：否。`name-only` 只有两个文件；13 个计划迁移 blob 全等。该 1 是主工作区 HEAD 未跟踪、origin/main 已有的 V428（两 tree 同 blob，故不进本 diff）。

6. **Q：端点还在，内容是否没修完？**  
   A：否。B2 把端点下线登记 XM-M034-A，禁止本 B3 改票务文件。鉴权层 DENY 已使「任何权限都不能人工放行」在本模块写入集合内成立。本 diff 没有端点 hunk 是合规，不是漏修。

7. **Q：定稿「若发布门禁要求，随批机械重生成生成物」，本 diff 没有 spec/csv，是否内容 FAIL？**  
   A：否。闸 3 的尺子是「归因不到的行即越界」。此刻在主工作区跑生成器会带上 V424–V428 的表/列漂移，那些行不能归 001。M021 B4 已接受把重生留在基于 origin/main 的检查点。本 diff 没有生成物 hunk 不构成内容 FAIL。

8. **Q：GREEN 是否只靠日志、本席没跑测试，能不能给内容 PASS？**  
   A：本席 SELECT 已独立核终态与断言同构；RED 失败句与 `permissionExists` 语义咬合。不重跑是「禁止写库」下的缺口，记下，不升 FAIL。

---

## 6. 顺从式核对（假定实现正确）

- 定稿文件名、retired、三 DELETE、active 临时授权 revoked、不改 Java/HTML/已应用 13 迁移、只两文件：均命中。
- TDD 类路径、方法、非 active + 四源 0 行：均命中。
- 清场：新例只 SELECT，不插行；类既有 `@Transactional` 不影响这条。
- XM-M034-A 未动：命中。
- 文件模式 100644、非可执行：命中。
- 注释与 SQL、与枚举原文：命中。

---

## 7. 最终票

| 轴 | 票 | 理由 |
|---|---|---|
| 内容 | **PASS** | 前向迁移即定稿五条 DML；`retired`/`revoked` 无 CHECK 拦阻；FK/类型/长度允许未跑过的库首次执行；注释与 SQL 同向；已应用 13 迁移零改写；测试走真库 `permissionExists`+四源 count，RED/GREEN 与库实态一致；可执行位与 git 文件模式未变；无业务发明；席甲把生成物重生留检查点在闸 3 下成立 |
| 归因 | **PASS** | 2 hunk / 31 插入行全部归 M034-001；无第三路径；计划 13 文件未改 |
| 合取本席 | **PASS** | 任一轴 FAIL 即 FAIL；两轴均 PASS |

非阻断观察（不升 FAIL、不新开 ID）：① 本席未重跑 26 例 JDBC、未查生产库、未用 Flyway 内部算法复算 checksum `832923190`（改核 version/description/success + 文件在 after tree）；② 门禁产物重生与 `verify:predeploy` 不在本 diff，必须在基于 origin/main 的检查点 worktree 执行，且发布 classpath 须带上主工作区已有的 V427/V428 与本 V429；③ `revoked_by='V429'` 是允许的顺带填充，不是最小必要列；④ 工作区测试文件 `stat` 664 对比 tree `100644`，git 不跟踪该差别。

本席未改 findings / ledger / 锁 / HTML / 业务代码。
