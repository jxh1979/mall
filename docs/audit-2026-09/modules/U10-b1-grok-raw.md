CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U10 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 10 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U10 — 未发布材料第 10 段「员工数据库迁移」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：10 文件，物理行 90，标记 NORMAL；main 主归属：**无**（全部为未发布新增文件；main 无同名路径）
- 冻结判据：新增文件无 main 归属 → 通用工程判据 GEN-001–018（18 项；相对开工提示 MISSING=[] EXTRA=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U10-001、GR-U10-002、GR-U10-003
- 添加项（B1 只登记，不代用户回答 2.2）：**GR-U10-002** = `V408:2` `merchants.is_self_operated`，与已定稿 U04-001 / U08-001 同根；操作员 2026-09-09 已在 `questions/U04-001.md` 答「删除」。**不再弹窗。**
- 同根已定稿、不新开 2.2：U05-008 / U06-007 / U07-005 / U09-001（以 main 为底重放、迁移 ≥V411）；U05-005（`shift_visible_days` 出厂行）；U04-001 / U05-001 / U08-001（自营布尔）；U09-003（`hr_review` 种子同批）
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | 隔离库 `u10_grok_probe`（`TEMPLATE complex_test`，**非** `complex_test*`）上逐文件应用 V400–V409；应用后 DROP |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段 SQL → U05/U06/U09/U11 副本 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；未跑的 U11 集成测试不写通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（独立自营布尔列），不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md`（SHA256 `a842323e664e5af104eac5c97ad22fda929ebeb62d81c501161ac1f6f482db58`） | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md`（SHA256 `6f9ae7c1c295d7e67eda1e3a616c337c057f45f1a5bf2955efb07d539a655b79`） | 本段验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：套件文档写明已并入 `grilling`，无独立技能可装
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

内部 grilling 树（自答，未问操作员）：

1. 这 10 个文件必须存在吗？必须有**等价模式**。删掉它们，BSM L1433 两条注册路径、L1443 排班每次修改留痕、L1444 可见天数、L1445/BEQR L280 离开充电设施打卡、L1458 20% 上限三审留痕、L938/PGM L582 寄存柜扫码、PGM L870 员工证件/手机加密、BEQR L240 五态任职、铁律 6 注册自审分离，在库模式上都做不到。文件本身是细化，不是添加。
2. 整段按现文件名合进当前 main，首轮会不会过门禁？**不会。** `origin/main` 最大号已是 V410；本段 V400–V409 全部 `<= V410`。独立复现 `tools/verify-migration-version-order.mjs` 规则 → 10/10 FAIL，提示重命名为 V411 或更后。`application.yml` `validate-on-migrate: true` 且未开 `outOfOrder`。
3. 只合本段 SQL、不合 U05 Java，首轮会不会把 main 已修项覆盖掉？**同名路径不会**——HEAD 无这 10 个文件。会引入 `merchants.is_self_operated`（U04-001 已答删除）并把 Flyway 解析集插到 V410 之前。整棵 76 文件树覆盖仍会回退其它段 M009–M012（U08 已证实）。
4. SQL 在当前 V410 模式上能不能执行？能。隔离库 `u10_grok_probe`（TEMPLATE `complex_test`，当时 flyway max=410、无 400–409 行）逐文件应用 **10/10 OK**；`charging_disconnect` 可插入；非法 `employment_status='active'` 与 `method='face'` 被 CHECK 拒绝。随后 DROP，`complex_test` 仍为 max=410、无 `staff_policy_settings`。
5. `shift_visible_days` 缺种子是不是要两三件不巧才暴露？不是。V402 只插入 `payroll_penalty_cap_bps`；隔离库 `count(*)=0`。U05 `numericSetting(..., 7)` 在缺行时返回 `StaffNumericSetting(7, null, null)`，与 BSM L1444「须同时配置上下限」首轮即偏离。U11 用 Java 默认 7 天绿测，掩盖本缺口。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，属正常）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 本段无 main 映射行；适用 GEN-001–018 定义区 L12–L100，18/18 已从锁全文读过

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.70，494901 bytes，SHA256 `664fe3f932ad71f64a2e0dad3c0478de178a726318d211dfc98af015c8cc96a6`（与开工提示一致）。findings V1.61，620671 bytes，SHA256 `3ef66588a1665f487fc5e3188669e47437c6344cdeb3f9b4edfc5a5a48238156`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U00-000：未发布树相对 242f4872 全 76 文件与 main 分叉；第 10 段 V400–V409 与 main V410 编号关系见 M010-009
- M010-009 / 操作员裁定：新迁移从 V410 起编号，跳过未发布占用的 V400–V409；main 已落地 `V410__m010_business_entity_topology_and_role_isolation.sql`
- U04-001 / U08-001：`is_self_operated` 独立布尔已答删除；处置含「U10 V408 去掉该列」
- U05-005：`shift_visible_days` 缺行不得返回空上下限
- U05-008 / U06-007 / U07-005 / U09-001：以 main 为底重放，迁移 ≥V411，禁止整树覆盖
- U09-003：`hr_review` 种子随 U10 重编号同批（V407 已插入 `approval_lines`，不插入 `supervisor_review_policies`）

其它只读哈希：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| `plan/runbook.md` V0.02 | 10409 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | 421826 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `160-audit-prompt.md` V0.17 | 65856 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `unpublished-03acdb98-input.json` V0.01 | 17547 | `10eb8240ee08ee2fae053235250f19335cacc2ff8d9a2ed5edbe69be366293e9` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

三 HTML 只读、未改。本段冻结集为 GEN，无 HTML 派生 ID；分类对照仍用现行正本原文（类型 1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

清单本段 10 条与独立 `sha256sum`/`wc -l` **10/10 MATCH**。副本 SHA 与树 `e28dad05:<path>` blob 字节数一致。全 76 文件相对基线 `git diff --shortstat 242f4872 e28dad05` = **76 files changed, 4745 insertions(+), 1084 deletions(-)**（与开工提示一致）。

对象核验：`git cat-file -t 03acdb98…` fatal（无 commit）；`e28dad05…` = tree；`242f4872…` = commit。

本段相对发布基线逐文件 numstat（`git diff --numstat 242f4872 e28dad05 -- <path>`；全部为新增）：

| 路径 | + | − | 树 blob | 副本 SHA256 |
|---|---:|---:|---|---|
| `.../V400__staff_profile_pii_and_self_device_permission.sql` | 6 | 0 | `464207c56cf84c5a33856fda2687447d77358444` | `7c5c6587feb088372ca42a1a5db1a8f1a00f17d5902046956d5010b5dbd334ba` |
| `.../V401__staff_registration_initiator.sql` | 3 | 0 | `7851ace54e34c97fd0740973ea8d1ad8260184c8` | `ef514984445a6fa0299c051f3c4d0fecb1f60a8c016245c65ede4b7ffa6fdea6` |
| `.../V402__staff_policy_settings.sql` | 17 | 0 | `50b6f81aebab34a02a1c8a9a586e2f72cefc411f` | `f510a7a6da72f9bdfa9b324f858242e6b24abbbe9df1ca1768bb90089e749510` |
| `.../V403__staff_profile_encrypted_personal_fields.sql` | 4 | 0 | `84826f330f596afa539bfbb1abfb47d99e546d19` | `e4a62e9e51d36acb401384e7e909411564dd03b50f3b5fe34ccc8eb8d7e34061` |
| `.../V404__staff_employment_status.sql` | 4 | 0 | `9117d38ecb1d034098e3d6e7777f566bacf4fc76` | `1c0ddc2c0b38023c847b4214e9708be29d7bf7021cfb9fec4c9688638a4d3a46` |
| `.../V405__staff_work_phone_receipts.sql` | 6 | 0 | `d757d9865ed4742696fd5a5d973f7a9b72402bf2` | `bb00ba624b48ffaa4d63251e79cdf6b55a0f3f2066fa41ba0ad35b07a3235405` |
| `.../V406__staff_shift_revision_approvals.sql` | 14 | 0 | `52465147938734a1c2bb88e9b7ff4ab0ba5f3d1b` | `f59acfd5768d862a9f98b2f307620235e72111212526cd38160f89f72bd752ef` |
| `.../V407__staff_penalty_cap_approval_history.sql` | 17 | 0 | `979a1c8c74efbe2285ef6524709019345d921335` | `b6ea6dc70923885aa083d23d7b8ef9ced2c387e9723b8f93d23e7a48803f22a8` |
| `.../V408__staff_registration_company_routes.sql` | 6 | 0 | `d2c1d7555c29f26278b9966057fd309a1733d3fa` | `fb750c0d4bea78938c3251f45983eeccc3069f44401e8bc8e4bc745e8dffabf3` |
| `.../V409__staff_locker_scan_records.sql` | 13 | 0 | `759a619178050b072edb35ab6b6be21b1919a2f9` | `588e5f3de4475bbf49f6e262479d3e225e62fb6d550d13f4556883bd14b3abad` |
| **合计** | **90** | **0** | | |

`git diff --stat` 本段：`10 files changed, 90 insertions(+)`。树 blob 由 `git rev-parse e28dad05:<path>` 实算，与副本字节一致。

HEAD 同名：10/10 MISSING。

main 迁移最大号 V410（`V410__m010_business_entity_topology_and_role_isolation.sql`）；基线最大号 V399；副本最大号 V409。`git log --oneline 242f4872..HEAD -- backend/src/main/resources/db/migration/` 仅 `2e812adf audit(160): M001–M010 checkpoint`。

### 1.1 与 main 分叉对照（对抗式固定问题）

本段 10 文件在 HEAD **均不存在**，故**不会按同名路径覆盖** M009–M012 已修源码。冲突面是版本号与一列业务模式：

1. **版本号 vs 已发布 V410（GR-U10-001）**。操作员在 M010-009 裁定「新迁移从 V410 起编号」；main 已用 V410 写主体拓扑（删 `parent_entity_id`、收 `entity_role` 三角色、角色名按组织唯一）。把本段以 V400–V409 文件名合入当前树：
   - `verify:migration-version-order`：origin/main 最大 V410，新增必须 **>410** → 10 文件全红（本席独立复现 exit 1）。
   - Flyway：`validate-on-migrate: true`，未配置 `outOfOrder`。当前 `complex_test` flyway 为 399 然后 **410**，**历史中无 400–409**（本席 docker exec 只读）。把 V400–V409 放进 classpath 后，校验会把它们看成「已解析未应用且版本低于当前」。
2. **`V408:2` `is_self_operated` vs main 六类码（GR-U10-002）**。main `merchants` 已有 `cooperation_form`（V042），无自营布尔。U04-001 已答删除该布尔及「U10 V408 该列」。
3. **整棵未发布树覆盖**仍会回退其它段已修项（U03/U08 已证实 M009/M012）。本段文件不是那些同名路径的载体，但发布说明必须禁止整树覆盖。

V410 正文不改 `staff_*` / `staff_attendance_records` / `approval_lines` 种子集（仍为 finance/tax/legal）。本段 SQL 对象与 V410 无列级互相 DROP；隔离库在 **已应用 V410 的模式上** 执行 V400–V409 内容 **10/10 成功**。失败点是**编号与门禁**，不是这些 DDL 与 V410 列冲突。

### 1.2 HTML 锚点（分类用，非本段冻结 ID）

本段冻结集无 BEQR/BSM/PGM ID。分类对照现行正本（类型 1）：

- BEQR L240 员工状态：待入职、在职、试岗、停岗、离职
- BEQR L280 / BSM L1445 打卡：NFC、扫码、离开充电设施，取最早一次
- BSM L701 / PGM L495 / L498 / BSM L1717 合作方式六类含「自营」
- BSM L1433 / PGM L578 注册两条路径；标注为自营视同公司自有
- BSM L1443 排班每次修改须上一级审批、只能追加
- BSM L1444 可见天数按经营主体可配，出厂默认 7 天，须同时配置上下限
- BSM L1458 / BEQR L283 扣除上限 20%、人力财务法务三审、出厂 20%、须上下限；月结锁定配置
- BSM L938 / PGM L582 寄存柜扫码留痕，不设手工登记
- PGM L870 员工手机号、身份信息加密存放

## 2. 数据流（副本内 → main → SQL → HTML）

```
U10 SQL
  V400 name TEXT + role_staff_self←staff.device.bind
      → U05 JdbcStaffRepository.saveProfile piiCipher.encrypt(name)
      → U05/U06 StaffService @GuardedAction staff.device.bind
      → main V086 name VARCHAR(128)、permissions 已有 staff.device.bind、V322 未授予 self 角色
      → PGM L870 加密；BSM L935 私人手机绑定
  V401 submitted_by
      → U05 recordRegistrationInitiator / isRegistrationInitiator
      → StaffService.reviewRegistration SELF_APPROVAL_FORBIDDEN
      → 铁律 6 / 注册审批职责分离（HTML 申请人不得自审）
  V402 staff_policy_settings
      → U05 numericSetting / saveNumericSetting / listMyShifts
      → U11 shiftVisibility* 测例（缺行时靠 Java 默认 7）
      → BSM L1444 / L1458
  V403 phone TEXT, id_doc TEXT
      → U05 saveProfile encrypt/decrypt；StaffRegistrationRequest 注释「传原值」
      → main 仅 phone_hash / id_doc_hash（V086）
      → PGM L870
  V404 employment_status
      → U05 setEmploymentStatus 五码；错误文案中文五态
      → U11 employmentState*
      → BEQR L240
  V405 method VARCHAR(24)+charging_disconnect
      → U05 punch 拒绝 manual、只认 nfc/qr/charging_disconnect
      → U03 METHOD_LABELS；U11 receivingWorkPhoneCountsEarliestEventOnceAcrossMethods
      → main V210 chk_att_method nfc,qr,manual VARCHAR(16)
      → BSM L1445 / BEQR L280
  V406 staff_shift_revisions
      → U05 saveShiftRevision / publishShiftRevision
      → U09 排班上一级审批落地
      → BSM L1443
  V407 staff_penalty_cap_revisions + hr_review + statements.penalty_cap_bps
      → U05 applyPenaltyCapRevision；U09 三审策略硬编码 hr/finance/legal
      → main V391 无 hr_review；U09-003
      → BSM L1458
  V408 is_self_operated + company_id + registration_source
      → U05 StaffRegistrationService.merchant.selfOperated()；withCompanyRoute
      → U08 MerchantRecord / Jdbc 插拔（U04-001 已答删除布尔）
      → main cooperation_form 六类码，无该布尔
      → BSM L1433 / L701
  V409 staff_locker_records
      → U05 StaffLockerService 柜机扫码存取
      → U11 locker 测例
      → BSM L938 / PGM L582
```

main 当前 `JdbcStaffRepository` 无这些列/表的读写。只合 U10 不合 U05：列闲置、应用仍跑。只合 U05 不合 U10：SQLException 缺列。必须同 PR。

## 3. 逐文件全量三线

十文件均全文读完（无 OVERSIZE）。每文件：必要性 / 顺从 GEN / 对抗。

### 3.1 V400 `staff_profile_pii_and_self_device_permission.sql`（6 行）

全文：`ALTER TABLE staff_profiles ALTER COLUMN name TYPE TEXT;` + `INSERT INTO role_permissions(role_id, permission_code) VALUES ('role_staff_self', 'staff.device.bind') ON CONFLICT DO NOTHING;`

- **第一性：** 姓名改 TEXT 承载 PiiCipher 信封（前缀+Base64，可超 VARCHAR(128)）。`staff.device.bind` 权限 V086 已有，V322 未授给 `role_staff_self`；HTML 私人手机由员工绑定。删掉则加密姓名放不下、自助绑定无角色授权。
- **顺从：** GEN-010/014 成立（类型放宽非破坏性；INSERT 幂等）。隔离库应用 OK，probe 上 `role_staff_self`×`staff.device.bind` 1 行。
- **对抗：** ON CONFLICT 依赖 V003 PK `(role_id, permission_code)`，存在。不写 `permissions` 行（已有）。主体归属注释「由服务校验」——授权在 U05 `@GuardedAction`，非本文件缺口。注释「M010-001、M010-020」是旧 87 模块编号，与当前审计 M010-001（删 parent 列）不是同一 ID（观察）。

分类：细化（PGM L870 + 私人手机绑定）。无新 finding。

### 3.2 V401 `staff_registration_initiator.sql`（3 行）

全文：`ALTER TABLE staff_profiles ADD COLUMN submitted_by VARCHAR(128);`

- **第一性：** 商户代注册的发起人，供审批禁自审。U05 `recordRegistrationInitiator` 在提交后 UPDATE；`isRegistrationInitiator` 用 EXISTS。自助注册无已认证主体，列可空合理。
- **顺从：** 列可空、无 FK、无「只写一次」约束。注释「后续更新不覆盖」由 `saveProfile` UPSERT **省略** `submitted_by` 实现；`recordRegistrationInitiator` 本身无 `WHERE submitted_by IS NULL`。首轮只在创建时调一次，不构成首轮失败（观察）。
- **对抗：** 自助路径不写该列 → 自审检查对 NULL 不命中；自助申请人尚无账号，不能批自己。不升 finding。

分类：细化（职责分离）。无新 finding。

### 3.3 V402 `staff_policy_settings.sql`（17 行）

建表 PK `(policy_key, scope_id)`；CHECK `min<=value<=max`；CHECK 可见天数 `min_value>=1` **或** 处罚上限必须 `scope_id='global'` 且 0–10000。种子 **仅** `('payroll_penalty_cap_bps','global',2000,0,10000)`。

- **第一性：** 一张表承载 BSM L1444 可见天数与 L1458 全平台上限。2000 bps＝20%，与出厂默认一致。可见天数按经营主体分 `scope_id`（Java 用 `company:`/`merchant:` 前缀）。
- **顺从：** 处罚上限种子满足「出厂 20% + 上下限」。**可见天数零行**——隔离库 `shift_visible_rows=0`。U05 `orElse(new StaffNumericSetting(defaultValue, null, null))` 返回无上下限。U11 `shiftVisibilityUsesEmployerSettingAndDefaultIsExactlySevenDays` 靠 Java 默认 7 绿，再自行 INSERT 一行才测配置。**GR-U10-003。**
- **对抗：** 缺行不是极端路径，get/listMyShifts 首轮即走。上下限数字 HTML 未写死，U05-005 已裁「不发 2.2、种子由发布者批准或 fail-closed」。本段表是出厂行的唯一落点。

分类：表与处罚上限种子＝细化；缺 `shift_visible_days` 出厂行＝**冲突**（对「须同时配置上下限」）。

### 3.4 V403 `staff_profile_encrypted_personal_fields.sql`（4 行）

`ADD COLUMN phone TEXT; ADD COLUMN id_doc TEXT;`

- **第一性：** PGM L870 明文要求加密存放。V086 只存不可逆哈希，无法按上级线解密展示（PGM L680 员工手机号对管理线不脱敏）。可解密列是该条的下位。注释不回填旧哈希，与铁律 8 同向。
- **顺从：** 未 DROP `phone_hash`/`id_doc_hash`；U05 INSERT 不再写哈希列。死列不影响首轮（观察）。PiiCipher 对无信封前缀的历史明文原样返回，complex_test 当前 `staff_profiles` 0 行，无解密爆炸。
- **对抗：** 无盲索引则不能按手机号精确查员工；HTML 未要求按手机号检索员工档案。不升 finding。

分类：细化。无新 finding。

### 3.5 V404 `staff_employment_status.sql`（4 行）

`ADD COLUMN employment_status VARCHAR(32) NOT NULL DEFAULT 'pending_entry' CHECK (... five English codes ...)`

- **第一性：** BEQR L240 五态；与 `registration_status` / `status(active|inactive)` 分轴。注释写明独立于账号及注册审批。U05 校验五码，错误文案为中文五态。
- **顺从：** 存储码英文是细化不是冲突（界面中文在服务层）。DEFAULT `pending_entry` 对空库/将清空库符合铁律 8。隔离库插入 `employment_status='active'` 被 CHECK 拒绝（exit 1）。
- **对抗：** 未把已批准员工写成 `employed`——铁律 8 不做回填。不升 finding。

分类：细化。无新 finding。

### 3.6 V405 `staff_work_phone_receipts.sql`（6 行）

先 `ALTER COLUMN method TYPE VARCHAR(24)`，再 DROP `chk_att_method`，再 ADD 含 `charging_disconnect`。`charging_disconnect` 19 字符，必须先放宽 VARCHAR(16)。

- **第一性：** BSM L1445 / BEQR L280 第三种领取方式。顺序正确。隔离库插入 `charging_disconnect` exit 0；`face` 被拒。
- **顺从：** 保留 `manual`。注释「历史手动记录仍可读取」；U05 新打卡拒绝 manual。空库上 `manual` 是遗留枚举，非业务发明（技术性，观察）。main 仅此一处约束名，DROP 不会 miss。
- **对抗：** 先 ADD CHECK 再扩类型也不会挡住 DDL，但 INSERT 19 字符会撞 VARCHAR(16)——本文件先扩类型，正确。

分类：细化。无新 finding。

### 3.7 V406 `staff_shift_revision_approvals.sql`（14 行）

`staff_shift_revisions`：PK id，`UNIQUE(review_id, shift_id)`，JSONB before/after，`applied_at` 可空，索引 `(shift_id, created_at)`。无 FK。

- **第一性：** BSM L1443「只能追加、修改前后完整保留」。U05 用 CAST jsonb 写入，发布时 `applied_at`。
- **顺从：** UNIQUE 保证同一复核单同一班次一条。无 FK 到 `staff_shift_schedules` / `supervisor_review_decisions`——本仓大量业务表同样不加跨域 FK；服务层事务 + UNIQUE 覆盖首轮（观察，不升缺陷）。
- **对抗：** 并发未应用修订靠 U05 `updated_at` 乐观锁。表本身无「同一 shift 仅一条未应用」排除约束；服务按 review 落地。不构成首轮必炸。

分类：细化。无新 finding。

### 3.8 V407 `staff_penalty_cap_approval_history.sql`（17 行）

建 `staff_penalty_cap_revisions`（PK `review_id`，CHECK 0≤min≤value≤max≤10000）；`INSERT approval_lines hr_review`；`ALTER staff_payroll_statements ADD penalty_cap_bps INTEGER NOT NULL DEFAULT 2000 CHECK 0–10000`。

- **第一性：** BSM L1458 三审留痕 + 月结锁定上限。`hr_review` 补 V391 没有的人力线。2000 bps＝20%。
- **顺从：** 隔离库 `hr_review` 1 行，四线为 finance/hr/legal/tax。不写 `supervisor_review_policies`——策略在 U09 代码硬编码；U09-003 已定稿「种子随 U10 重编号同批」。本文件已提供线种子，缺策略表行不是本文件新根因（观察，指向 U09-003）。
- **对抗：** 只合 U09 不合本文件 → `APPROVAL_LINE_NOT_CONFIGURED: hr_review`（U09 已记）。必须同 PR，并入 GR-U10-001 处置，不另开根因。

分类：细化。无独立新 finding。

### 3.9 V408 `staff_registration_company_routes.sql`（6 行）

三句 ALTER：`merchants.is_self_operated BOOLEAN NOT NULL DEFAULT FALSE`；`staff_profiles.company_id`；`registration_source CHECK ('merchant_submitted','company_self')`；部分索引 `company_id WHERE delete_status<>'approved'`。

- **第一性：** `company_id` + `registration_source` 落实 BSM L1433 两条路径与公司归属。`delete_status` 来自 V266，索引合法。隔离库三列均在。
- **顺从 / 对抗：** **`is_self_operated` 是独立于六类合作方式的第二开关。** HTML BSM L701/PGM L495 六类已含「自营」；L1433「标注为自营」不是第二轴。类型探针（U04 B1）`cooperationForm:"lease", selfOperated:true` 可分叉。操作员已答删除。**GR-U10-002。** `company_id`/`registration_source` 须保留。
- `company_id` 可空：U05-010 要求写入强制，属 Java；空库 DEFAULT 不强制 NOT NULL（观察）。

分类：后两列细化；第一列 **添加（已答删除）**。

### 3.10 V409 `staff_locker_scan_records.sql`（13 行）

`staff_locker_records` FK `staff_profiles(id)`；CHECK 取出≥存入；部分唯一 `(cabinet_device_id, slot_ref) WHERE retrieved_at IS NULL`。

- **第一性：** BSM L938 / PGM L582 柜子自动记下谁、哪一格、存取时间。U05 `StaffLockerService` 柜机身份 + 咨询锁 + 占用冲突。U11 断言存/取/换人复用。
- **顺从：** 开放格口唯一实现「先到先选占用」。id VARCHAR(80) vs 档案 VARCHAR(64)，Java `locker_+UUID` 约 43 字符，首轮放得下（观察）。无项目隔离列——柜机已绑定点位，服务拒未验证柜机（观察）。
- **对抗：** 同一员工多格口未禁；HTML 未禁止。取出校验归属在服务。不升 finding。

分类：细化。无新 finding。

## 4. 适用冻结判据逐项（GEN-001–018）

| ID | 本段适用 | 结论 |
|---|---|---|
| GEN-001 | 是 | V408 自营布尔无独立需求承载（已答删除）。其余列/表能指到 HTML。**GR-U10-002** |
| GEN-002 | 是 | DDL 在 V410 模式上 10/10 可执行；**按现文件名合入 main 则版本门禁首轮失败**。**GR-U10-001**。缺可见天数出厂行导致读写口径与 HTML 不一致。**GR-U10-003** |
| GEN-003 | 是（本段无测试文件） | 证据改用隔离库应用 + CHECK 反例。U11 有 locker/任职/充电断开/可见天数测例，但可见天数测例用 Java 默认 7 掩盖缺种子，**不能**把 U11 绿当作本缺口已测过。未跑 U11（需写库/Spring），不声称测试通过 |
| GEN-004 | 部分 | CHECK/PK/部分唯一约束输入。授权与业务校验在 U05 服务。无缺陷 |
| GEN-005 | 部分 | V400 授 `staff.device.bind` 给自助角色，主体校验在服务。无缺陷 |
| GEN-006 | 是 | PK/UNIQUE/CHECK/部分唯一；处罚上限与排班修订靠服务事务。无首轮必炸的缺约束 |
| GEN-007 | 部分 | ON CONFLICT DO NOTHING；locker 部分唯一 + 咨询锁在 U05。无缺陷 |
| GEN-008 | 是 | 必须与 U05/U06/U09/U11 同 PR；自营列与 main 六类码双轴。**001/002/003** |
| GEN-009 | 是 | CHECK 拒绝非法枚举（已实跑）。无吞错 |
| GEN-010 | 是 | 姓名/手机/证件改为可容纳密文的 TEXT；不在 SQL 里留密钥。哈希列残留为观察 |
| GEN-011 | 部分 | 修订表 `initiated_by`/`created_at`/`applied_at` 供审计。无缺陷 |
| GEN-012 | 弱相关 | 无无界批处理。N/A 说明：纯 DDL |
| GEN-013 | 是 | 版本闸门与 Flyway validate 是构建入口。**GR-U10-001** |
| GEN-014 | 是 | 顺序必须 > main 最大号；破坏性无（无 DROP 业务列）。**GR-U10-001、003** |
| GEN-015 | 否 | 无 UI。存储英文码，中文在 U05 错误文案。N/A |
| GEN-016 | 弱相关 | SQL 注释「文件版本 V0.01」可核对；旧 M010-00x 编号与当前审计 ID 撞车（观察） |
| GEN-017 | 是 | 自营列按添加（已答删除）而非技术性。缺出厂行按冲突而非 HTML 沉默。**002/003** |
| GEN-018 | 是 | 结论落到 HTML 原文、隔离库输出、版本闸门命令。本文件 |

## 5. Findings

### GR-U10-001 — 现文件名 V400–V409 不大于 main V410，首轮门禁失败

- **文件:行号：** `V400__…sql`–`V409__…sql` 文件名（十个版本号）
- **原文：** 文件名 `V400`…`V409`；对照 main `V410__m010_business_entity_topology_and_role_isolation.sql` 头注释「操作员裁定：新迁移自 V410 起编号（跳过未发布分支的 V400–V409）」
- **业务影响：** 按现名合入当前 main：CI `verify:migration-version-order` 红；Spring Flyway `validate-on-migrate` 在已应用 V410 的库上拒启动。员工相关模式无法随发布进入当前主干。
- **可复现：** 独立复现 `tools/verify-migration-version-order.mjs` 规则（mainMax=V410，added=本段 10 文件）：
  ```
  MIGRATION_VERSION_ORDER_FAIL: origin/main 当前最大迁移号是 V410,本分支新增的迁移必须排在它之后:
  - V400__staff_profile_pii_and_self_device_permission.sql(V400 不大于 V410) → 请重命名为 V411 或更后
  …（V401–V409 同，共 10 行）
  ```
  本机对 **未改 main** 跑原脚本：`MIGRATION_VERSION_ORDER_PASS: 本分支未新增迁移(origin/main 最大号 V410)` exit 0。
  只读 `complex_test`：flyway 390–399 然后 **410**，无 400–409 行；`staff_policy_settings` 等 to_regclass 为空。
  隔离库 `u10_grok_probe` TEMPLATE `complex_test`（max=410）**内容**应用 10/10 OK，证明要修的是**编号与叠法**不是 DDL 语法。
- **冻结判据：** GEN-002、GEN-013、GEN-014
- **2.3 依据类型：** 4（命令）+ 3（Flyway 默认 validate；`application.yml:30` `validate-on-migrate: true`，无 `outOfOrder`）
- **四分类：** **技术性**。无业务发明：版本号是迁移机编号，不是业务实体。同根 U05-008 / U06-007 / U07-005 / U09-001 / M010-009。
- **工程缺陷：** 是。建议登记、发布前须修：是。
- **最小处置（写给发布者）：** 以 main `445a49f4` 为底重放本 10 文件内容，**重编号为 V411 起连续号**；与 U05+U06+U07+U09+U11（及仍需要的员工前端/契约）同一 PR；禁止整棵未发布树覆盖；`hr_review` 种子随新编号留下（U09-003）；**不要**再占用 V400–V409。

### GR-U10-002 — `V408:2` 独立自营布尔列（U04-001 已答删除）

- **文件:行号：** `V408__staff_registration_company_routes.sql:2`
- **原文：** `ALTER TABLE merchants ADD COLUMN is_self_operated BOOLEAN NOT NULL DEFAULT FALSE;`
- **业务影响：** 与六类合作方式并列第二「自营」开关；员工注册分流可与合同合作方式分叉（U04 已用类型探针证明）。操作员已答删除整条轴。
- **可复现：** 隔离库应用后 `\d merchants` 含 `is_self_operated`；main/complex_test 仅有 `cooperation_form`、无该列。HTML：BSM L701「区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式」；BSM L1433 / PGM L578「在商户档案上标注为自营的商户视同公司自有」——标注落在六类码 `self_operated`，不是新列。`questions/U04-001.md` 操作员答复首行「删除」，并写明「U10 V408 该列」随之删除、不再问。
- **冻结判据：** GEN-001、GEN-008、GEN-017
- **2.3 依据类型：** 1（HTML）+ 4（隔离库列存在）
- **四分类：** **添加**（操作员已答删除＝U04-001 / U08-001）。不是技术性：它引入可与六类码分叉的业务开关。
- **工程缺陷：** 是。建议登记、发布前须修：是。**不新开 2.2。**
- **最小处置：** 重放时**删除该 ALTER**（不要 `is_self_operated`）；**保留** `company_id`、`registration_source` 及公司索引。U05 注册分流改读 `cooperation_form=="self_operated"`（U05-001），不依赖本列。

### GR-U10-003 — V402 未种子 `shift_visible_days` 出厂行（上下限空）

- **文件:行号：** `V402__staff_policy_settings.sql:16-17`（INSERT 只含处罚上限）；对照 CHECK L13 允许 `shift_visible_days` 但无对应 INSERT
- **原文：**
  ```
  INSERT INTO staff_policy_settings(policy_key, scope_id, numeric_value, min_value, max_value)
  VALUES ('payroll_penalty_cap_bps', 'global', 2000, 0, 10000);
  ```
  无 `shift_visible_days` 行。
- **业务影响：** 员工看排班的可见天数在任何经营主体上都没有带上下限的出厂配置。U05 缺行时返回值 7、min/max null，与「须同时配置上下限」不符；get 接口可把空上下限交给前端。
- **可复现：** 隔离库 `SELECT count(*) FROM staff_policy_settings WHERE policy_key='shift_visible_days'` → **0**。同库处罚上限 1 行 (2000,0,10000)。U05 `JdbcStaffRepository.java:61-67` `orElse(new StaffNumericSetting(defaultValue, null, null))`。U11 `:203-215` 先断言 7 天再 INSERT 测试行。HTML BSM L1444：「出厂默认 7 天，须同时配置上下限」。
- **冻结判据：** GEN-002、GEN-008、GEN-014
- **2.3 依据类型：** 1（HTML）+ 4（隔离库 count=0）
- **四分类：** **冲突**（对「须同时配置上下限」的明文；不是 HTML 沉默）。同根 U05-005。上下限**数字** HTML 未写，U05-005 已裁不发 2.2。
- **工程缺陷：** 是。建议登记、发布前须修：是。
- **最小处置：** 重编号后的策略表迁移补 `shift_visible_days` 出厂行（值 7，min/max 由发布者按上线配置写入，fail-closed 不得再返回 null 上下限）。与 U05-005 同批。禁止本席代填上下限数字。

## 6. 添加项完整列表（供主调度按 2.2；B1 不代答）

| 临时 ID | 内容 | 2.2 状态 |
|---|---|---|
| GR-U10-002 | `merchants.is_self_operated` | **已答删除**（`questions/U04-001.md`）。不要再问。 |

无其它添加项。`registration_source` 两码、任职五码、`charging_disconnect`、柜记录、修订表、加密列、`hr_review` 线、处罚上限种子均为对应 HTML 的细化。

## 7. 命令证据摘要

1. 锁 SHA：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar 同）
2. HEAD=`origin/main`=`445a49f409f0cfbe6dd616ae079be1299824c48f`
3. 对象：commit `03acdb98` 不存在；tree `e28dad05` 存在；基线 commit `242f4872` 存在
4. 76 文件 shortstat +4745/−1084；本段 +90/−0；HEAD 10/10 无同名
5. 副本 10/10 sha256/wc/bytes 与开工提示及清单一致
6. `node tools/verify-migration-version-order.mjs` 在 main：PASS（未新增）；overlay 模拟：FAIL×10，要求 ≥V411
7. docker exec 只读 `complex_test`：flyway max=410，无 400–409，无本段表/列，`chk_att_method` 仍 nfc/qr/manual，method VARCHAR(16)，name VARCHAR(128)，approval_lines=finance/tax/legal，`staff_profiles` 0 行
8. 隔离库 `u10_grok_probe`（非 `complex_test*`）TEMPLATE 后应用 V400–V409 **10/10 OK**；name=text；六列新档案字段在；`is_self_operated` 在；method VARCHAR(24)+charging_disconnect CHECK；策略表仅 penalty 一行；hr_review 在；locker/shift_rev/cap 表在；penalty_cap_bps 在；self 角色已授 bind；`charging_disconnect` INSERT 0/1；`shift_visible_days` count=0；非法任职/非法 method CHECK 失败。随后 DROP。`complex_test` 仍 max=410、无 policy 表、无 probe 库
9. 未 export 三个 DB 变量到本席环境；未 source env 文件；库口令仅用于 docker exec 容器内 psql（与 host 密码认证失败的 127.0.0.1 路径分开）。未写 `complex_test*`
10. 未跑 U11 `mvn test`（会写库/需依赖）；不声称测试通过

## 8. 观察（无新根因，不漏并）

1. 本段同名路径不覆盖 M009–M012 已修文件；整树覆盖风险在其它段，发布禁令仍适用。
2. DDL 在已有 V410 的模式上可执行；与 V410 无互相 DROP。
3. `phone_hash`/`id_doc_hash` 残留，U05 不再写。死列，铁律 8 清空后无影响。
4. `recordRegistrationInitiator` 无 `IS NULL` 守卫；首轮只调一次。
5. `company_id` 可空，写入强制在 U05-010。
6. V406/V407 无跨表 FK，与仓内惯例一致。
7. 存储英文码、界面中文在 U05；U11 有的断言仍是 `pending_entry`（归 U11/展示段）。
8. CHECK 保留 `manual`：新打卡路径已拒；空库遗留枚举。
9. U11 可见天数测例用 Java 默认 7，不能证出厂上下限。
10. `complex_test` 现已是 399→410、无 400–409（相对 M010-009 当时「库停在 409」的环境已变）；本席只读确认。
11. V400 注释「M010-001」与当前审计 M010-001 不是同一项。
12. V409 id VARCHAR(80) vs 档案 VARCHAR(64)，首轮 ID 长度足够。
13. 自助注册不写 `submitted_by`，与「公开入口尚无账号」一致。
14. PiiCipher 对无信封明文原样返回；当前测试库 0 档案行。
15. `getShiftVisibility` 跨商户回落 7 是 U05/U06 范围（U06-001 作用域键）。
16. V407 不插 `supervisor_review_policies`：U09-003 已覆盖。
17. `modules.md` 文首仍写「当前 main 基线 242f4872」，与 HEAD 445a49f4 脱节，属计划文件，非本段源码。
18. 本席未读 `U10-b1-self-raw` 或本轮本方其它结论。

## 9. 审计结论

**FAIL**

有未定稿工程缺陷 GR-U10-001、GR-U10-002、GR-U10-003。无未答 questions（002 已答删除）。无有条件通过。发布者最小集：main `445a49f4` 为底、V411+ 重编号、去掉 `is_self_operated`、补 `shift_visible_days` 出厂行（上下限由发布者配置）、与员工服务/审批/测试同 PR、禁止整树覆盖。

## U10 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 39453 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 10 段：员工数据库迁移」（10 文件 90 行，main 无归属，适用判据＝GEN-001–018 共 18 项）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。main 迁移目录仅 `V410__m010_business_entity_topology_and_role_isolation.sql`，V400–V409 同名路径 10/10 MISSING。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–004 逐 ID 全 PASS；添加 1 已答删除、不发新 2.2；发布前须修「是」=001–003，「否」=004）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401；本段无 main 映射行，适用 GEN-001–018，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、blob、numstat、副本 10 SQL、V402/V408 原文、main V410/`application.yml`/`package.json` 门禁脚本、HTML L701/L1433/L1444/L521、PGM L578、副本 `JdbcStaffRepository.java:61-67`、U11 `:203-215`、`questions/U04-001.md` 答复首行「删除」、已定稿 U05-001/U05-005/U05-008）；未重跑隔离库、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u10-b2-append.md` 中转，落盘后删除。

此票只判断 findings.md V1.62 顶部「当前 U10 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U10-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 39453 | `d394f3660c68fbeedf974b023d00f0e3e514950b01f209368195b0183650a9df` |
| `docs/audit-2026-09/modules/U10-b1-self-raw.md` | V0.01 | 23336 | `d05ee4d397427fa53ff3595e2a4e6c8c24a32e059bf34f433e84d87f97012899` |
| `docs/audit-2026-09/findings.md` | V1.62 | 626329 | `1b337d7431af2687fc8c502e8334ec18888f45c80c1d5d7cd2edffc13f718df1` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.71 | 496602 | `612c7c5c79954d8e985d4ebbe93339bf85b1226c7ea38cfa3935cdcfa26bed3d` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`；答复首行精确「删除」 |

候选形态：V1.62 顶部 U10-001…004；questions 0（引用 U04-001 已答删除，不新开 2.2）；添加 1 入队；无 HTML 修改申请。U10 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa`、PGM `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`。副本本段 10/10 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算）。`git diff --numstat 242f4872 e28dad05 --` 本 10 路径全部 +n/−0；合计 +90/−0。全树 `git diff --shortstat 242f4872 e28dad05` = 76 files, +4745/−1084。`modules.md` 文首仍写「当前 main 基线 242f4872」（见 004）。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核技能；用户/提示词覆盖技能本体；本节点为指定 B2 归并，不改走 Arc audit |
| receiving-code-review | 先核 HTML/SQL/门禁脚本/已定稿 U05 再接受或反驳候选单元格；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：(a) 003 分类/处置/BSM 锚、(b) 001 门禁是否发明、(c) 002 是否还要问、(d) 004 是否写错 §8.2/10/17 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/diff/rg/python 编号规则模拟；本票不声称本轮新迁库或 U11 绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（本段 SQL → U05 仓储 → U11 测例 → main V410/门禁 → HTML）；本轮不建隔离库 |
| ponytail-review | 只评最小删除/收窄，不落地。002 只删 V408 第 2 行；003 不代填上下限数字；001 只重编号+既有门禁 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 003 应维持观察还是冲突入队？冲突。见 (a)。
2. 把 BSM-001/BSM-086 写进本段「冻结判据」是否越界？锚点作为 HTML 原文成立，但不是本段 18 项冻结集。见 (a)。
3. 001 的 spec 重生成 + verify:predeploy 是否发明新闸？否。见 (b)。
4. 002 是否还要弹窗？否。见 (c)。
5. 004 是否把 §8.2 改写成「DDL 不能在 V410 上执行」或把 §8.10 写成库仍停 409？否。见 (d)。

### 完整差集（GR-001…003、§8 观察 1–18、席甲 CX-001/002、OBS-1…5）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–004。未把本席 finding 降观察。席甲 OBS-3 由候选升队，与本席 GR-U10-003 同向，不改写本席原意。席甲独有项无超出 CX-001/002 与 OBS-1…5。席甲 B1 漏判 V402 未种子可见天数出厂行——候选已勘误接受本席，本席独立复核后维持。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U10-001 | U10-001 | 否：以 main 为底、V411 起连续号、禁整树、同 PR |
| GR-U10-002 | U10-002 | 否：删 V408 第 2 行；保留 company_id / registration_source / 公司索引；不新开 2.2 |
| GR-U10-003 | U10-003 | 否：冲突、补出厂行值 7、上下限由发布者写、不新开 2.2、同根 U05-005 |
| §8 1–18 | U10-004 | 「含」为抽样（见 (d)）；第 2/10/17 条未写错 |
| 席甲 CX-U10-001 | U10-001 | 与 GR-001 同一编号/叠法事实；spec 重生成是既有门禁并入，不改写编号结论 |
| 席甲 CX-U10-002 | U10-002 | 与 GR-002 同一列 |
| 席甲 OBS-3 | U10-003 | 升队勘误，不改写本席「冲突/须修」 |
| 席甲 OBS-1/2/4/5 | U10-004 | 维持观察 |
| 席甲 GEN-003 `u11_scratch` 92/92 | 不采信为本缺口已测 | 与本席 GEN-003「U11 用 Java 默认 7 掩盖缺种子」一致；候选未把 92/92 写成 003 已覆盖 |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U10-001 | GR-U10-001 + 席甲 CX-U10-001 | 技术性（迁移编号/发布叠法，同根 U05-008/U06-007/U07-005/U08-002/U09-001）/是/是 | **PASS（技术性编号叠法/是/是；只引用既有门禁）** |
| U10-002 | GR-U10-002 + 席甲 CX-U10-002 | 添加（操作员已答删除：questions/U04-001.md；同根 U04-001/U05-001/U08-001）/是/是 | **PASS（添加已答删除/是/是；不再问）** |
| U10-003 | GR-U10-003 + 席甲 OBS-3（升队勘误） | 冲突（同根 U05-005）/是/是 | **PASS（冲突/是/是；BSM 锚点作 HTML 对照成立，非本段冻结 ID）** |
| U10-004 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；§8.2/10/17 未写错）** |

技术性入队 1（001 编号/叠法）。添加入队 1（002，已答删除）。冲突入队 1（003 可见天数出厂行）。登记观察 1（004）。questions 0。无 HTML 修改申请。

发布前须修「是」=001、002、003（3）；「否」=004（1）。与候选汇总一致。席甲 B1 票 FAIL 但漏 003，属低估该根因；候选接受本席三项须修并勘误，本席独立复核后维持。

### 重点 (a) U10-003 分类「冲突（同根 U05-005）」与处置「补出厂行 7、上下限由发布者写入、不新开 2.2」；BSM-001/BSM-086 锚点 — **PASS（与 GR-U10-003 / 已定稿 U05-005 一致；锚点作 HTML 对照成立）**

独立核，不采信席甲「OBS-3 升队」四个字为己证。

本轮源码（副本，非 main）：

- `V402__staff_policy_settings.sql:4` CHECK 允许 `policy_key IN ('shift_visible_days','payroll_penalty_cap_bps')`。
- `:16-17` 唯一 INSERT 为 `('payroll_penalty_cap_bps','global',2000,0,10000)`。全文无 `shift_visible_days` INSERT。
- 副本 `JdbcStaffRepository.java:61-67` `orElse(new StaffNumericSetting(defaultValue, null, null))`。
- 副本 U11 `M010RegressionTests.java:203-215`：先 `listMyShifts().items()).hasSize(7)`，再自行 INSERT `shift_visible_days` 测配置。

HTML / 已定稿（本轮正本）：

- BSM L1444：「员工看自己未来若干天的排班，可见天数由后台配置（业务参数，按经营主体可配……出厂默认 7 天，须同时配置上下限）。」
- BSM-086 锁出处 `:1441`（h3），现行正本 L1444 即上句。**定身份：这就是那条可见天数参数，出厂 7 天且必须有上下限。**
- BSM-001 锁出处 `:505`（h2），「未填写上下限」句在现行正本 L521（锁 HTML 原文块内）：「还必须写明取值的上限与下限，超出该区间的配置一律拒绝生效，未填写上下限的参数不得上线。」
- 已定稿 U05-005：冲突；缺行不得返回空上下限；出厂行由发布者写入 7 与批准的上下限（上下限数值属上线配置，HTML 已明文要求配置、不是新规则，不新开 2.2），或缺行 fail-closed。

四分类：**冲突**（GEN-017 顺序冲突优先）。不是 HTML 沉默：L1444 明文「须同时配置上下限」。不是技术性：缺的是出厂配置行，不是编号机。不是添加：不新造「必须 7 天」或「必须某组上下限数字」。值 7 来自 HTML 出厂默认；上下限**数字** HTML 未写死——U05-005 已裁不发 2.2，本席 GR-U10-003 同裁。

发布前须修 **是**：缺行时 GET 首轮即把 `(7,null,null)` 交给调用方，与「须同时配置上下限 / 未填写上下限不得上线」偏离。U11 用 Java 默认 7 绿，不能证出厂上下限。席甲 OBS-3 只记 CHECK 无上限，挡不住「表允许该键却零行」。

最小处置与本席 GR-U10-003、U05-005 同向、最懒：重编号后的策略表迁移补 `shift_visible_days` 出厂行（值 7；min/max 由发布者按上线配置写入），或缺行 fail-closed 直至配置。与 U05-005 同批。禁止：本席代填上下限数字；把上下限数字写成新 HTML 规则去改三份业务文档；新开 2.2。

**BSM-001/BSM-086 锚点：** 作为分类对照的 HTML 原文（依据类型 1）与同根 U05-005 引用，**成立**。BSM-086 含 L1444；BSM-001 含「未填写上下限的参数不得上线」。**不是本段冻结判据 ID。** 本段无 main 归属，冻结集仅 GEN-001–018；本席 B1 GR-U10-003 冻结判据写 GEN-002、GEN-008、GEN-014，HTML L1444 走类型 1。候选把 BSM-001、BSM-086 写进「冻结判据」单元格，属列名过宽，不改变分类/处置/须修。定稿该格应写 GEN-002、GEN-008、GEN-014；BSM-001/BSM-086 标为分类对照/同根引用。非阻断，不改票。

票 **PASS**。

### 重点 (b) U10-001 重编号 ≥V411、重生成数据隔离说明与域归属 CSV、跑 verify:predeploy — **PASS（只引用既有门禁，无发明）**

独立核：

1. `tools/verify-migration-version-order.mjs`：本分支新增迁移号必须 **大于** `origin/main` 当前最大号。main 仅 V410 → 新增必须 ≥V411。独立模拟 added=V400–V409、mainMax=410 → 10/10 `<=410`，提示重命名为 V411 或更后。AGENTS.md 铁律 9；该脚本在 `verify:delivery-gates`。
2. `application.yml:30` `validate-on-migrate: true`。`backend/src/main/resources` 下 yml **无** `outOfOrder` / `out-of-order`。已应用 V410 的库把 V400–V409 看成「已解析未应用且版本更低」。
3. `package.json` `verify:predeploy` **含** `verify:domain-ownership-scope` 与 `verify:data-isolation-spec`（`--check`）。对应 `docs:domain-ownership-scope` / `docs:data-isolation-spec` 是既有生成脚本；M001–M010 检查点已因 V399→V410 跑过同一对。生成器以 `db/migration` 目录为事实源。不是新闸。
4. AGENTS.md 写明 `verify:migration-version-order` **未挂** `verify:predeploy`。候选事实栏已单列 version-order 脚本，处置的 predeploy 覆盖 spec/CSV check，没有把编号闸伪装成 predeploy 子步骤。两条都是既有 npm script。
5. 以 main `445a49f4` 为底、禁止整树覆盖、与员工段同 PR：同根已定稿 U05-008 / U06-007 / U07-005 / U09-001。列 U12 与已定稿 U09-004 同向；本席 GR-001 写 U05+U06+U07+U09+U11 及员工前端/契约，不反对并 U12。无业务发明：版本号不是业务实体。

发布前须修 **是**：按现文件名合入则 version-order 红、Flyway validate 拒启动，员工域新表新列进不了当前主干。隔离库证明要修的是编号与叠法，不是 DDL 语法（本席 B1 `u10_grok_probe` 10/10；本轮不重跑）。

最小处置：以 main `445a49f4` 为底重放 10 文件内容，重编号为 V411 起连续号（保持内部顺序，不再占用 V400–V409）；与 U05+U06+U07+U09+U11+U12 同一 PR；禁止整树覆盖；重编号后跑既有 `verify:migration-version-order` 与 `verify:predeploy`（后者含 isolation/ownership `--check`；check 红则跑对应 `docs:*` 重生成）。`hr_review` 种子随新编号留下（已定稿 U09-003）。

票 **PASS**。

### 重点 (c) U10-002 按已答删除登记且保留 company_id / registration_source / 公司索引 — **PASS（与 questions/U04-001.md 及 U05-001 一致，不需再问）**

独立核：

1. 副本 `V408__staff_registration_company_routes.sql:2` 原文：`ALTER TABLE merchants ADD COLUMN is_self_operated BOOLEAN NOT NULL DEFAULT FALSE;`。其后 `:3-6` 为 `company_id`、`registration_source` CHECK `('merchant_submitted','company_self')`、部分索引 `company_id WHERE delete_status<>'approved'`。
2. `questions/U04-001.md` 操作员答复首行精确「删除」；正文写明「U10 V408 该列」随之删除、后续段引用不再问。
3. 已定稿 U04-001 / U05-001 / U08-001：自营标注落在六类码 `self_operated`；U05 分流改读 `"self_operated".equals(merchant.cooperationForm())`；去掉对 V408 该列的依赖。
4. HTML：BSM L701 六类含「自营」；BSM L1433 / PGM L578「标注为自营的商户视同公司自有」——标注落在六类码，不是第二开关。`company_id` + `registration_source` 落实两条注册路径，须保留。

四分类：**添加**（已答删除）。不是技术性。工程缺陷是。发布前须修 **是**（重放时删该 ALTER）。**不新开 2.2。**

最小处置与本席 GR-U10-002 同向：删除第 2 行；保留其余。U05 分流按已定稿 U05-001（Java 字段 `cooperationForm`；SQL 列名 `cooperation_form` 是同一码）。禁止再问操作员。

票 **PASS**。

### 重点 (d) U10-004 观察是否写错本席 §8 — **PASS（第 2/10/17 条未写错；「含」为抽样）**

004 列席甲 OBS-1/2/4/5，并以「Grok §8 1–18（2 DDL 在已有 V410 模式可执行、4 `recordRegistrationInitiator` 无 IS NULL 守卫、5 `company_id` 可空归 U05-010、7 存储英文码归 U05/U11、9 U11 可见天数测例用默认 7 不能证出厂上下限、10 complex_test 已 399→410、12 V409 id 长度、13 自助注册不写 `submitted_by`、14 PiiCipher 明文原样返回、15 跨商户回落归 U06-001、16 V407 不插策略表归 U09-003、17 modules.md 文首基线 242f4872 与 HEAD 脱节属计划文件）」收本席观察。「含」是抽样不是闭集。

第 2 条：本席原文「DDL 在已有 V410 的模式上可执行；与 V410 无互相 DROP。」004 写「2 DDL 在已有 V410 模式可执行」。未改写成「不能执行」或用来否定 001。失败点仍是编号与门禁。本轮核：main 仅 V410、本 10 文件无 DROP V410 对象。成立。

第 10 条：本席原文「`complex_test` 现已是 399→410、无 400–409（相对 M010-009 当时「库停在 409」的环境已变）」。004 写「10 complex_test 已 399→410」。未改写成库仍停 409 或仍含未发布 V400–V409。本轮核：main 已落地 V410；本票不新探 `complex_test*`。成立。

第 17 条：本席原文「`modules.md` 文首仍写「当前 main 基线 242f4872」，与 HEAD 445a49f4 脱节，属计划文件，非本段源码。」004 写「17 modules.md 文首基线 242f4872 与 HEAD 脱节属计划文件」。本轮核：`plan/modules.md` L5 原文即该基线句。未立项、未改写成本段源码缺陷。成立。

未抽样写出、定稿不得当成「Grok 无这两条」或升格入队：

| §8 | 去向 |
|---|---|
| 1 同名路径不覆盖 M009–M012；整树禁令仍适用 | 已并 001 |
| 3 哈希列残留，铁律 8 清空后无影响 | 席甲 OBS-1 并列；「须补录」不得读成发布前回填存量 |
| 6 V406/V407 无跨表 FK | 维持观察 |
| 8 CHECK 保留 `manual` | 席甲 OBS-4 邻近；维持观察 |
| 11 V400 注释「M010-001」与当前审计 M010-001 不是同一项 | 席甲 OBS-2 |
| 18 本席 B1 未读 self-raw | 过程注，不立项 |

席甲 OBS-1（旧哈希列保留、发布后加密列空）维持观察：铁律 8 不做存量回填；004 须修否，不得把「须补录」读成发布前必修。OBS-2（旧 M010-00x 撞名）计划/注释口径，须修否。OBS-4（`chk_att_method` DROP/ADD）隔离库已过，须修否。OBS-5（柜机无 FK）服务校验，须修否。OBS-3 已升队，004 未重复降回观察。

票 **PASS**。

### 001 独立裁定（技术性编号叠法／须修是）

见 (b)。票 **PASS**。

### 002 独立裁定（添加已答删除／须修是；不再问）

见 (c)。票 **PASS**。

### 003 独立裁定（冲突／须修是；同根 U05-005；不新开 2.2）

见 (a)。票 **PASS**。

### 004 独立裁定（登记／须修否）

见 (d)。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. 003 定稿「冻结判据」单元格写 GEN-002、GEN-008、GEN-014；BSM-001/BSM-086 改标「分类对照 HTML L1444 / L521，同根 U05-005」，不升格为本段冻结 ID。分类、处置、须修不变。
2. 001 定稿可点明两条既有闸：编号用 `verify:migration-version-order`（在 `verify:delivery-gates`，未挂 predeploy）；spec/CSV 用 `verify:predeploy` 内 `--check`，红则跑既有 `docs:*`。不是新闸。
3. 002 处置「U05 分流改读」以已定稿 U05-001 的 Java 字段 `cooperationForm` 为准；候选 `cooperation_form` 是同码 SQL 列名。
4. 004 「含」为抽样；§8.6 / §8.8 未写出维持观察。席甲 OBS-1「须补录」不得读成违反铁律 8 的存量回填。
5. 001 同 PR 列 U12 与已定稿 U09-004 同向，可保留。

全批差集无漏并。001 只引用既有门禁。002 已答删除不再问。003 冲突与 U05-005 / GR-U10-003 一致，BSM 锚点作 HTML 对照成立。004 未写错 §8.2/10/17。逐 ID 001–004 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U10 或 main 模块 PASS。步骤仍 B2；U10 IN_PROGRESS；材料未发布，不做 B3。
