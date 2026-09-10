CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U18 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 18 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U18 — 未发布材料第 18 段「文档生成与检查工具」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录 L8339–8347；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（65856 字节，SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`）
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`（完整工作树快照 3584 文件，含基线迁移 + V400–V409，**不含** main 的 `V410`）
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件；本席独立核 **76/76 sha256 与清单一致、0 mismatch**
- 本段：3 文件，物理行 492，标记 **NORMAL**；main 主归属：**M029**（域归属生成器）、**M394**（operation 矩阵生成器）、**M053**（PII 加密门禁）
- 冻结判据：M029 映射行 L6408 ∪ M053 L6432 ∪ M394 L6773 = **GEN-001–018 共 18 项**（三行集合相同；相对开工提示 MISSING=[] EXTRA=[]）；定义区从锁全文 L12–L100 读过
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U18-001、GR-U18-002、GR-U18-003、GR-U18-004
- 添加项（B1 只登记，不代用户回答 2.2）：**0**。本段三文件不新开 2.2。员工表分类口径、staff PII 登记均可指到三份 HTML 上位（细化）。矩阵生成器是 OpenAPI 投影器，U14/U17 已定稿的自营写口/公开注册等业务面不在本文件发明。
- 同根已定稿、不新开 2.2：U13-001（CSV/`file_version` 须与本段生成器同批）、U17-008（矩阵标题/变更表，根因文件即本段矩阵生成器）、U10-001（V400–V409 须 ≥V411）、U05-008（JdbcStaff 加密写口须同批）、U14-001…008 / U17-001…010
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作。`docs:domain-ownership-scope` / `docs:operation-matrix` 的写盘只发生在 `/tmp/u18-b1-grok/**`。
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。不得把 U13 CSV / U17 矩阵的 B2 定稿写成「本生成器已审过、可跳过」。
- 本席独立实跑（隔离 `/tmp/u18-b1-grok`，不改副本/main）：未发布 cwd `generate-domain-ownership-scope-audit.mjs --check` **PASS**（332 tables）；未发布 `generate-openapi-operation-matrix.mjs --check` **PASS**（730）；未发布 `verify-pii-encryption.mjs` **PASS**（16 列）；main 同三项 **PASS**（328 tables / 720 ops / 13 列）。**只把本段 3 工具叠到 main 输入**：domain `--check` **FAIL**（CMD-OVL3-DOMAIN EXIT 1）；PII **FAIL**（CMD-OVL3-PII EXIT 1，staff name 明文、phone/id_doc 无写入点）；矩阵 `--check` **PASS**（720，标题仍 V0.1）。未发布 json+矩阵 + **main** 矩阵生成器 **FAIL**（CMD-MAINGEN-UNPUBMATRIX EXIT 1）。副本自洽 **不能** 消解对 main 的分叉、也不能把 `--check` 绿写成 GEN-016 通过。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（`disable-model-invocation: true`，本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | 隔离 `/tmp` 叠工具/CSV/JSON/Java 复现 `--check` 红绿 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段 mjs → U10 迁移 / U13 CSV / U14 JSON / U17 矩阵 / U05 JdbcStaff → main 对照 → HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；副本 `--check` 绿不能写成 HTML 或发布叠法通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（写死 V0.1 的变更表、每行重复的 `file_version=V0.01`、永不触发的 `uncovered` 分支） |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md`（SHA256 `a842323e664e5af104eac5c97ad22fda929ebeb62d81c501161ac1f6f482db58`） | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md`（SHA256 `6f9ae7c1c295d7e67eda1e3a616c337c057f45f1a5bf2955efb07d539a655b79`） | 本段验证均秒级，未起 monitor |
| codebase-design | mattpocock `engineering/codebase-design/SKILL.md` | 存在；B1 只读工具脚本，未改 CONTEXT/ADR |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能（套件文档写明已并入 `grilling`）
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

内部 grilling 树（自答，未问操作员）：

1. 这 3 个文件必须存在吗？必须有**由 Flyway 迁移反向生成的域归属 CSV 及其 `--check`**、**由 OpenAPI JSON 反向生成的 operation 验收矩阵及其 `--check`**、**对仓储 `addValue` 做 PII 密文包裹的可失败门禁**。`package.json` 把三者写成 `verify:domain-ownership-scope` / `verify:operation-matrix`（`verify:p0-contract` 链）/ `verify:pii-encryption`（`verify:predeploy` 链）。删掉它们，对应门禁无入口，HTML「看不到别的商户和别的项目的数据」「个人信息一律加密存放」在工程上失去持续强制点。文件作为生成器/门禁是细化+技术性，不是添加。未发布增量（staff 表分类、`file_version` 列、标题回读、staff_profiles 三列登记）才是本段要审的材料面。
2. 整段按现文件叠进当前 main、首轮会不会过门禁？**不会。** 只叠 3 工具：domain `--check` 红（CSV 无 `file_version` 列）；PII 红（main `JdbcStaffRepository:671` `addValue("name", profile.name())` 明文，且无 `phone`/`id_doc` 写入点，main 仍 `phone_hash`/`id_doc_hash`）；矩阵 `--check` 仍绿（720，标题从现文件读到 V0.1）。未发布树内三项可绿，只证明未发布自洽。
3. 本文件会不会覆盖 main 已修的 M009–M012？**对本三路径不会回退字节。** `git rev-parse` 显示 main 该 blob == 基线 blob，`git log 242f4872..HEAD -- <本段三路径>` 空。对抗式固定问题的真实形态是：整树覆盖 76 文件会把 U08 `JdbcPlatformRepository` 等已与 main 分叉、含 M009–M012 修复的兄弟文件盖掉，并把 V400–V409 写进已有 V410 的 main；只叠本段三工具则让 domain/PII 门禁变红，不回退 M009 Java。
4. 矩阵生成器内容变化时版本账是否诚实？**不诚实。** 首次正常跑 `docs:operation-matrix`（OpenAPI 已从 720 增到 730）会把现值 730 写入「2026-06-12」历史行，标题若曾手改成 V0.03 则回读冻结，§7 仍全是 V0.1。不是两三件不巧。`--check` 绿不能消解 GEN-016。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` **文件内容**同值（sidecar 自身 sha256 `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，不参与闸 1）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- M029 映射行 L6408：GEN-001…GEN-018 共 18 ID
- M053 映射行 L6432：同上 18 ID
- M394 映射行 L6773：同上 18 ID
- 并集 18，MISSING=[] EXTRA=[]
- 定义区从锁全文读过（GEN-001–018 位于 L12–L100）。本段映射**不含** BEQR/BSM/PGM；分类按 GEN-017 对照三份 HTML **main 当前正本**原文（见各 finding）。不引用未冻结 ID。

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger / findings 只读核验（不改；开工提示给出的 V2.98 / V1.79 与本席实算一致）：

- ledger：版本 **V2.98**，540086 bytes，SHA256 `d00cb55a413d03e945016c5c8644f2375181083934b3220bf786c9a06d46e37f`
- findings：版本 **V1.79**，706179 bytes，SHA256 `4dcc0ea6c43f95d61ca0bfaba21f3bda3adda0e990d5e9c58a19899f768df0b8`
- 台账中与本段直接相关的已定稿项（背景，不继承 PASS）：U13-001 CSV/`file_version` 须与第 18 段生成器同批；U17-008 矩阵标题/变更表根因交第 18 段；U10-001 迁移 ≥V411；U14/U17 契约面发布前须修。U18 行 ledger 顶部 `IN_PROGRESS`

输入副本核验：

- `tools/generate-domain-ownership-scope-audit.mjs` sha256 `c364a59c356cece0625d164653bd37c1bf1cb26e6f0b2591becdd0f2b183798b`（开工提示、清单、`sha256sum` 三方一致）；230 行 / 13783 bytes
- `tools/generate-openapi-operation-matrix.mjs` sha256 `b05d7137b38cd7cb173e1d7a422e2cd3c6dd8effaa306a3ef104a5525c4fb669`（三方一致）；193 行 / 13858 bytes
- `tools/verify-pii-encryption.mjs` sha256 `17a5d59856cfca619aa26295dadee86fc13dc584291901e5dab34134e6a38159`（三方一致）；69 行 / 5532 bytes
- 全 76 文件相对清单 sha256：**76/76 命中、0 mismatch**

## 2. 本段文件、diff stat、与 main 分叉

### 2.1 全 76 文件相对发布基线 numstat（CMD-76）

`git diff --numstat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b` 合计 **76 files changed, 4745 insertions(+), 1084 deletions(-)**，与开工提示一致。逐文件：

| + | − | 路径 |
|---:|---:|---|
| 12 | 0 | `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt` |
| 124 | 76 | `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` |
| 19 | 1 | `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` |
| 29 | 0 | `apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt` |
| 27 | 6 | `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` |
| 26 | 0 | `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt` |
| 36 | 0 | `apps/android-terminal/src/test/kotlin/com/complex/terminal/DeviceRequestHeadersTest.kt` |
| 12 | 0 | `apps/web/src/admin/pages/OrganizationMerchantPage.tsx` |
| 39 | 1 | `apps/web/src/admin/pages/StaffPayrollLeavePage.tsx` |
| 35 | 0 | `apps/web/src/admin/pages/StaffProfilesPage.test.tsx` |
| 72 | 12 | `apps/web/src/admin/pages/StaffProfilesPage.tsx` |
| 4 | 3 | `apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx` |
| 3 | 2 | `apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx` |
| 61 | 0 | `apps/web/src/admin/pages/StaffShiftAttendancePage.settings.test.tsx` |
| 59 | 16 | `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` |
| 2 | 0 | `apps/web/src/admin/types.ts` |
| 311 | 59 | `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` |
| 104 | 0 | `backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java` |
| 70 | 9 | `backend/src/main/java/com/complex/platform/access/staff/StaffController.java` |
| 85 | 0 | `backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java` |
| 5 | 0 | `backend/src/main/java/com/complex/platform/access/staff/StaffNumericSetting.java` |
| 69 | 15 | `backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java` |
| 138 | 0 | `backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java` |
| 306 | 124 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` |
| 14 | 1 | `backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java` |
| 4 | 0 | `backend/src/main/java/com/complex/platform/access/staff/dto/StaffEmploymentStatusRequest.java` |
| 4 | 0 | `backend/src/main/java/com/complex/platform/access/staff/dto/StaffNumericSettingRequest.java` |
| 9 | 0 | `backend/src/main/java/com/complex/platform/access/staff/dto/StaffPenaltyCapRequest.java` |
| 6 | 4 | `backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java` |
| 5 | 0 | `backend/src/main/java/com/complex/platform/access/staff/dto/StaffSelfRegistrationRequest.java` |
| 7 | 11 | `backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchResult.java` |
| 9 | 2 | `backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java` |
| 4 | 0 | `backend/src/main/java/com/complex/platform/config/SecurityConfig.java` |
| 4 | 7 | `backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` |
| 13 | 10 | `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java` |
| 15 | 4 | `backend/src/main/java/com/complex/platform/governance/MerchantRecord.java` |
| 10 | 0 | `backend/src/main/java/com/complex/platform/governance/PlatformController.java` |
| 19 | 1 | `backend/src/main/java/com/complex/platform/governance/PlatformService.java` |
| 4 | 0 | `backend/src/main/java/com/complex/platform/governance/dto/MerchantSelfOperatedRequest.java` |
| 14 | 1 | `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` |
| 3 | 1 | `backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java` |
| 6 | 0 | `backend/src/main/resources/db/migration/V400__staff_profile_pii_and_self_device_permission.sql` |
| 3 | 0 | `backend/src/main/resources/db/migration/V401__staff_registration_initiator.sql` |
| 17 | 0 | `backend/src/main/resources/db/migration/V402__staff_policy_settings.sql` |
| 4 | 0 | `backend/src/main/resources/db/migration/V403__staff_profile_encrypted_personal_fields.sql` |
| 4 | 0 | `backend/src/main/resources/db/migration/V404__staff_employment_status.sql` |
| 6 | 0 | `backend/src/main/resources/db/migration/V405__staff_work_phone_receipts.sql` |
| 14 | 0 | `backend/src/main/resources/db/migration/V406__staff_shift_revision_approvals.sql` |
| 17 | 0 | `backend/src/main/resources/db/migration/V407__staff_penalty_cap_approval_history.sql` |
| 6 | 0 | `backend/src/main/resources/db/migration/V408__staff_registration_company_routes.sql` |
| 13 | 0 | `backend/src/main/resources/db/migration/V409__staff_locker_scan_records.sql` |
| 38 | 0 | `backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java` |
| 676 | 0 | `backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java` |
| 5 | 0 | `backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java` |
| 27 | 14 | `backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java` |
| 90 | 78 | `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` |
| 12 | 4 | `backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java` |
| 13 | 6 | `backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java` |
| 29 | 0 | `backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java` |
| 11 | 2 | `backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java` |
| 4 | 2 | `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` |
| 2 | 2 | `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` |
| 333 | 329 | `docs/domain-ownership-scope-audit-v0.1.csv` |
| 764 | 33 | `docs/openapi/openapi-p0.json` |
| 712 | 180 | `docs/openapi/openapi-p0.yaml` |
| 51 | 20 | `docs/p0-openapi-contract-closure-appendix-v0.1.md` |
| 17 | 7 | `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` |
| 5 | 4 | `docs/production-readiness-evidence/security-baseline-latest.json` |
| 18 | 9 | `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` |
| 17 | 7 | `docs/r5-spec-T9-staff-device-android-dev-design-20260618.md` |
| 3 | 3 | `security-scans/findings-register-v1.md` |
| 15 | 15 | `security-scans/formal-security-baseline-v1.md` |
| 11 | 1 | `tools/generate-domain-ownership-scope-audit.mjs` |
| 3 | 1 | `tools/generate-openapi-contract-closure-appendix.mjs` |
| 3 | 1 | `tools/generate-openapi-operation-matrix.mjs` |
| 4 | 0 | `tools/verify-pii-encryption.mjs` |

### 2.2 本段三文件相对基线 / 相对 main

| 路径 | vs BASE | BASE blob | TREE blob | HEAD blob | `git log 242f4872..HEAD -- 路径` |
|---|---|---|---|---|---|
| `tools/generate-domain-ownership-scope-audit.mjs` | +11/−1 | `6fc05e2c…faf6` | `7d5be6ba…e177` | **=BASE** `6fc05e2c…faf6` | 空 |
| `tools/generate-openapi-operation-matrix.mjs` | +3/−1 | `76bda7d4…9e4e` | `40752625…2783` | **=BASE** `76bda7d4…9e4e` | 空 |
| `tools/verify-pii-encryption.mjs` | +4/−0 | `13ce9dd5…960b` | `bb5a6785…60ca` | **=BASE** `13ce9dd5…960b` | 空 |

相对基线的真实改动（`git diff BASE TREE -- 本段路径`，本席全文读过）：

1. **域归属生成器**（自身注释 V0.02）：`inheritedRules` 头部增加 `staff_shift_revisions`、`staff_locker_records`；`globalTables` 增加 `staff_penalty_cap_revisions`；`classify()` 在 analytics 覆盖之前特判 `staff_policy_settings`（`scope_id` 混合雇主/global，备注要求人力/财务/法务/顶层审批）；每行写入 `file_version: "V0.01"`，表头多一列。
2. **矩阵生成器**（自身注释 V0.01）：标题由写死 `V0.1` 改为 `readFileSync(matrixPath)` 首行回读 `V\d+\.\d+`（缺省 `V0.01`）。§7 变更表仍全是 V0.1，且 2026-06-12「客户端方法级契约闸口」行继续插值 `${rows.length}`（main 生成器同样插值，本材料未删）。
3. **PII 门禁**（自身注释 V0.01）：`REGISTRY` 顶部增加 `staff_profiles.name` / `phone` / `id_doc`，写入文件均为未发布 `JdbcStaffRepository.java`。其余 13 列与 main/基线相同（含商户电话、存酒、幂等响应体）。

**与 main 分叉的对抗式固定问题（本三路径无 M009–M012 字节回退，分叉在门禁语义与整树兄弟文件）：**

- main 最高迁移 **V410**；未发布树 387 个 sql = 基线至 V399 + V400–V409，**无 V410**。
- main CSV 329 行、无 `file_version` 列、328 表；未发布 CSV 333 行、有 `file_version`、332 表（+4 张员工域表）。
- main OpenAPI **720** operation；未发布 **730**。无 `head`/`options`/`trace`。
- main `JdbcStaffRepository:671-673` 仍 `addValue("name", profile.name())` + `phone_hash` / `id_doc_hash`；未发布 `:891-893` 改为 `piiCipher.encrypt` 的 `name`/`phone`/`id_doc`。
- 整包覆盖会碰到 U08 `JdbcPlatformRepository`（相对基线 +13/−10）等已在 main 上被 M009–M012 改过的同名文件。本段三工具本身 main=基线，不是那次 Java 修复的落点。

### 2.3 跨段数据流（只读，不审他段为 PASS）

```
本段 generate-domain-ownership-scope-audit.mjs
  → 读 backend/src/main/resources/db/migration/*.sql（U10 V400–V409；main 另有 V410）
  → 写/核 docs/domain-ownership-scope-audit-v0.1.csv（U13）
  → package.json docs:domain-ownership-scope / verify:domain-ownership-scope
  → HTML：BSM「看不到别的商户和别的项目的数据」；寄存柜/可见天数/处罚上限原文（细化分类，不是本生成器发明）

本段 generate-openapi-operation-matrix.mjs
  → 只读 docs/openapi/openapi-p0.json（U14）；文案声称 yaml+json
  → 写/核 docs/p0-openapi-operation-acceptance-matrix-v0.1.md（U17）
  → package.json docs:operation-matrix / verify:operation-matrix ⊂ verify:p0-contract
  → 投影 U14 的 x-permission / x-audit-event-* / x-terminal-allowed；不审 HTML

本段 verify-pii-encryption.mjs
  → 读 REGISTRY 中的 Java 仓储（U05 JdbcStaffRepository；U08 JdbcPlatformRepository 等）
  → 断言 addValue("<col>", …) 含 piiCipher.encrypt
  → package.json verify:pii-encryption ⊂ verify:predeploy
  → HTML：PGM「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」
```

## 3. 逐文件全量三线

三文件均已从副本**整文件读完**（230+193+69 行），不是只看 diff。main 对应文件同样整文件读过。

### 3.1 文件 A — `tools/generate-domain-ownership-scope-audit.mjs`（M029）

**覆盖：** L1 版本注释；L10–20 `directColumns`；L22–63 `inheritedRules`（含新增两行 staff）；L65–72 `globalTables`（含新增 `staff_penalty_cap_revisions`）；L74–102 迁移扫描（CREATE/ALTER ADD COLUMN/DROP）；L104–107 列名 token；L109–145 analytics 覆盖（与 main 相同）；L147–187 `classify`（staff_policy_settings 特判在前）；L189–207 CSV 构建（`file_version`）；L209–230 `--check` / 写盘。

**第一性原理**

1. **为什么必须存在？** 删掉它，`verify:domain-ownership-scope` 无入口，U13 CSV 与 Flyway 再无机械同步。HTML 红线「看不到别的商户和别的项目的数据」需要一张表级 scope 清单；本文件是该清单的生成器，不是新需求池。答得上 → 有业务上位（隔离）+ 技术性门禁。
2. **声称是否执行？** 未发布 cwd `--check` EXIT 0，`DOMAIN_OWNERSHIP_SCOPE_PASS: 332 tables classified`。`/tmp` 重生 CSV 与未发布 CSV **逐字节相同**（CMD-GEN-DOMAIN-UNPUB，CSV_IDENTICAL）。main `--check` EXIT 0，328 tables。
3. **未验证就当真的前提？** CREATE TABLE 非贪婪 `([\s\S]*?)\);` 对嵌套 CHECK 括号的稳健性——本席用未发布 V402/V407 实证：四张新表均出现在 CSV，重生一致，当前材料未踩截断。不把「正则永远安全」写成已证。

**顺从式：** 见 §4。staff_policy_settings 特判把 `scope_id` 写成 direct，因 `scope_id` 不在 `directColumns`；无特判会落入 `single_complex_default`。处罚上限「全平台统一可配，调整须经人力、财务、法务三审并由顶层平台公司最终确认」与特判备注同向，属细化。寄存柜 inherited via `staff_profile_id` 与 BSM「柜子自动记下是谁、用了哪一格」同向。`staff_penalty_cap_revisions` 无租户列、改的是 global 上限 → `global_reference` 与「全平台统一」同向。

**对抗式：** 只叠本生成器到 main 迁移+CSV → `--check` 红（表头多 `file_version`）。用本生成器对 main 迁移**写盘**会改写 main CSV 表头且表集合不变（329 行，only gen/only main 表名差集空）。`status` 永远写 `covered`，`uncovered` 分支不可达。`file_version` 每行写死 V0.01，生成器自身 V0.02。整树覆盖会带上 V400–V409（U10-001）而非本文件单独回退 M009 Java。

### 3.2 文件 B — `tools/generate-openapi-operation-matrix.mjs`（M394）

**覆盖：** L1–8 入口（只解析 json）；L10–52 单元格/ref/channel；L54–75 五方法扫描；L77–81 表行；L83–84 **本材料引入的版本回读**；L86–177 模板（定位/强制规则/AC/边界/用户故事/变更表，`${rows.length}` 在 L103 与 L175）；L179–193 `--check` / 写盘。

**第一性原理**

1. **为什么必须存在？** `verify:operation-matrix` 把「矩阵 ↔ JSON 逐字节同步」写成硬条件。删掉生成器，矩阵只能手改，U17 验收面失去单一来源。文件是投影器，不是业务发明。
2. **声称是否执行？** 未发布 `--check` EXIT 0，`730 operations checked`。`/tmp` 用本生成器重生：标题 **V0.03**（回读现文件）、变更表 2026-06-12 行 **「P0 operation 总数为 730 个」**。main `--check` 720。只叠本生成器到 main json+矩阵：`--check` 仍绿（读到 V0.1，插值 720 与现文件一致）。
3. **未验证就当真的前提？** 文案写「基于 yaml 与 json」，实现只读 json。本席点算 unpublished/main json：`ops_std5` 等于 `all8`，无 head/options。yaml↔json 一致性属 U14/U15，本生成器不核。

**顺从式：** `--check` 能红（CMD-MAINGEN-UNPUBMATRIX、CMD-UNPUBJSON-MAINMATRIX EXIT 1），断言有效。幂等列 `x-idempotency-required ? "是" : "否"` 对任意真值（main 既有）。模板「若与业务 PRD 冲突以 OpenAPI 与本文为准」是 main 既有句，本材料未改（§8）。

**对抗式：** 版本回读 + 变更表写死 V0.1 + 历史行插值现值 = GEN-016。首次正常 `docs:operation-matrix`（JSON 已 730）就会写出假历史。U17-008 是产物面；本文件是根因。生成器不过滤 U14 已定稿须删的自营写口——投影器职责，不在本文件新开添加项。

### 3.3 文件 C — `tools/verify-pii-encryption.mjs`（M053）

**覆盖：** L1–12 口径注释；L13–37 REGISTRY（本材料 +3 staff 列，其余与 main 同）；L39–62 逐列 `addValue("col", <同行表达式>)` 须含 `piiCipher.encrypt`；L64–69 FAIL/PASS。

**第一性原理**

1. **为什么必须存在？** PGM 原文「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」`verify:pii-encryption` 是该句的可失败门禁。staff 三列登记是把「员工」从注释扩进 REGISTRY，属细化。
2. **声称是否执行？** 未发布 cwd 与隔离 Java 副本：`PII_ENCRYPTION_PASS: 16 个 PII 列` EXIT 0。未发布 `JdbcStaffRepository:891-893` 确为 `piiCipher.encrypt(profile.name/phone/idDoc())`。main：`PII_ENCRYPTION_PASS: 13 个` EXIT 0。只叠本门禁到 main Java：EXIT 1，三条 staff 失败（见 GR-U18-004）。
3. **未验证就当真的前提？** 门禁不读 SQL CHECK、不扫 `Map.of`/`jdbcTemplate.update` 字面量、正则在逗号后的表达式止于同行。未发布 staff 写口是单行 encrypt，当前材料首跑不会因正则漏报。V403 只 `ADD COLUMN phone/id_doc TEXT`、无 `enc:v1:` CHECK——属 U10 迁移面，本文件注释对 ticket_transfers 已承认「正则守不住则靠库上 CHECK」。

**顺从式：** 登记与未发布唯一写口对齐（该文件仅一处 `addValue("name"|"phone"|"id_doc")`）。商户电话登记仍指向 `JdbcPlatformRepository`；未发布该文件 `:515/:521` 仍 `piiCipher.encrypt`，本门禁不撤销 M009 对商户电话的加密断言。

**对抗式：** 叠到 main 必红（main 姓名明文、哈希列名不同）。整树覆盖才会碰到 U08 与 main 已修 Java 的分叉。把 `name` 登记为 PII 与导出「员工姓名不脱敏」不冲突：脱敏是展示/导出面，加密是落库面（PGM:870 vs PGM:680）。

## 4. 冻结判据逐项（GEN-001–018）

每条：适用面 → 证据 → 结论（本段范围内）。无实现面的写明原因，不空过。

| ID | 本段适用 | 证据 | 本段结论 |
|---|---|---|---|
| GEN-001 | 三工具必要性；死分支 | package.json 三入口；`status` 恒 `covered` 使 uncovered 不可达（§8） | 工具须保留。不可达分支不构成首跑缺陷。**001–004 另论发布最小性** |
| GEN-002 | `--check`/生成与声称一致 | 未发布三项绿；OVL3 domain/PII 红；矩阵生成器 `--check` 绿但变更表写假历史 | **FAIL** 见 001、002 |
| GEN-003 | 本段无 `*.test.js`；`--check` 即能失败断言 | OVL3-PII/OVL3-DOMAIN/MAINGEN-UNPUBMATRIX 均 EXIT 1 | 以门禁为测试，断言有效。不因无单测文件另立缺陷 |
| GEN-004 | 读本地路径，输出路径写死 | 无网络/无用户输入；`outputPath`/`jsonPath` 常量 | 通过（登记）。无实现面的动态输入校验 |
| GEN-005 | 无服务端授权 | 脚本不鉴权 | **无实现面**：不是授权代码。投影器把 JSON 的 `x-permission` 抄进矩阵，授权对错在 U14 |
| GEN-006 | 不写业务库 | `--check` 只读；生成只写文档 | **无实现面**：无状态机/金额。CSV 整文件重写，无半成品行 |
| GEN-007 | 无并发窗口 | 两进程同时 generate 后写覆盖 | 极端，首跑不发生。§8 |
| GEN-008 | CSV 列、矩阵↔JSON、PII 列名↔Java | 未发布自洽；与 main 列名/表头/720↔730 不一致 | **FAIL** 见 001、003、004 |
| GEN-009 | `--check` 显式 exit 1；版本回读无 existsSync | 缺矩阵文件时 `readFileSync` 抛错（仓库恒有该文件） | 首跑不缺文件。§8。门禁失败路径已实证 |
| GEN-010 | PII 门禁；错误信息不打印明文值 | 未发布 16 列 PASS；叠 main 抓到明文 `profile.name()` | 门禁意图成立。**004** 是发布顺序，不是本树明文漏网 |
| GEN-011 | PASS 日志含表数/operation 数/列数 | 实跑输出见 §7 | 通过（登记） |
| GEN-012 | 读 ~387 个迁移进内存 | 秒级完成 | 通过（登记） |
| GEN-013 | npm 脚本与相对路径 cwd | 无 cwd 跑 unpublished 脚本会误用 main 输入（CMD-UNPUB-DOMAIN EXIT 1） | 入口约定 cwd=树根。**001** 叠法仍红 |
| GEN-014 | 生成器读迁移不执行 Flyway | 分类依赖 U10 文件名 V400–V409；main 已 V410 | 编号冲突是 U10-001。本生成器发布须等 ≥V411 后重生。**001/003** |
| GEN-015 | 无 UI | CLI/CSV。CSV 英文分类句为 main 既有风格 | **无实现面**：不是用户界面。新备注仍英文，§8 |
| GEN-016 | 矩阵版本账；CSV `file_version`；文档声称 yaml | 重生标题 V0.03 / 历史行 730 / §7 全 V0.1；文案 yaml 未读 | **FAIL** 见 002（主），003 列变更须入账 |
| GEN-017 | 四分类 | staff 分类/PII 登记能指 HTML；`file_version`/版本回读无业务发明；无 HTML 沉默当冲突 | 通过分类纪律。无添加项 |
| GEN-018 | 本报告 | 锁哈希、命令 exit、HTML 原文、blob id | 本席依据落在 1/4 类。不引用他席结论为据 |

## 5. Findings（临时 ID，正式 ID 仅 B2 分配）

### GR-U18-001 — 只叠本段三工具到 main：domain 与 PII 门禁红；整树覆盖才会碰到 M009–M012 兄弟文件

- **完整原文（未发布）：** `generate-domain-ownership-scope-audit.mjs:198-205` 每行 `file_version: "V0.01"` 且表头含该列；`verify-pii-encryption.mjs:14-16` 登记 staff 三列；`generate-openapi-operation-matrix.mjs:83-84` 回读标题（叠 main 时读到 V0.1，矩阵门禁仍绿）。
- **文件:行号：** `tools/generate-domain-ownership-scope-audit.mjs:198-205`；`tools/verify-pii-encryption.mjs:14-16`；`tools/generate-openapi-operation-matrix.mjs:83-84`
- **业务影响：** 按「尚未发布的这些」整包或只合本段三文件合进当前 main，`verify:predeploy` 在 domain/PII 两处红，发布首跑不过。整树覆盖会把 U08 等已含 M009–M012 修复的同名文件盖回旧基线，并占用 V400–V409（与已应用 V410 冲突）。**本三路径 main blob＝基线，不是 M009 Java 的回退落点。**
- **可复现：** CMD-OVL3-DOMAIN EXIT 1 `docs/domain-ownership-scope-audit-v0.1.csv is out of sync with Flyway migrations`；CMD-OVL3-PII EXIT 1 三条 staff 失败；CMD-OVL3-MATRIX EXIT 0（720）。`git log 242f4872..445a49f4 -- <三路径>` 空。
- **冻结判据：** GEN-002、GEN-008、GEN-013、GEN-016
- **2.3 依据类型：** 4（命令 + blob）
- **四分类：** 技术性
- **无业务发明理由：** 叠法/门禁同步/禁止整树覆盖，不引入实体、枚举、权限或价格。
- **工程缺陷：** 是。建议登记：是。
- **最小处置（写给发布者，本轮不修）：** 以 main `445a49f4` 为底重放本段 3 文件 hunk，**禁止整树覆盖**。domain 与 U10 迁移重编号 ≥V411、U13 CSV 同一 PR 跑 `docs:domain-ownership-scope`；PII 与 U05 `JdbcStaffRepository` 加密写口同一 PR；矩阵先按 GR-U18-002 改生成器，U14 json 定稿后再 `docs:operation-matrix`。同根 U13-001 / U10-001 / U05-008 / U17-001。

### GR-U18-002 — 矩阵生成器从现文件回读标题、§7 写死 V0.1，并把现值写入 2026-06-12 历史行

- **完整原文：**
  ```javascript
  const documentVersion = fs.readFileSync(matrixPath, "utf8").split("\n")[0].match(/V\d+\.\d+/)?.[0] ?? "V0.01";
  const document = `# P0 OpenAPI Operation 验收矩阵 ${documentVersion}
  ```
  以及模板 L175：`按当前 OpenAPI 重建 operation 清单，P0 operation 总数为 ${rows.length} 个`（落在「2026-06-12 | V0.1 客户端方法级契约闸口」行）。§7 其余行日期/版本全部 V0.1。
- **文件:行号：** `tools/generate-openapi-operation-matrix.mjs:83-84`、`:175`、`:166-177`
- **业务影响：** 内容已从 720 口变为 730 口时，文档标题可停在手改的 V0.03，变更记录仍声称 2026-06-12 的总数即现值。读者/门禁都会把旧日期当成当前契约规模。U17-008 是矩阵产物面；**本文件是根因**。`--check` 绿（未发布 730 自洽）不能当 GEN-016 通过。
- **可复现：** CMD-UNPUB-MATRIX-CWD EXIT 0（730）；CMD-GEN-UNPUB 标题 `# P0 OpenAPI Operation 验收矩阵 V0.03`，变更表 `:821` 等价行「总数为 730 个」；main 同行写 720。CMD-UNPUBGEN-MAINMATRIX EXIT 0（叠生成器到 main 仍绿，掩盖该缺陷）。main 生成器标题写死 V0.1，对未发布矩阵 CMD-MAINGEN-UNPUBMATRIX EXIT 1。
- **冻结判据：** GEN-016、GEN-002
- **2.3 依据类型：** 4
- **四分类：** 技术性
- **无业务发明理由：** 版本字符串与变更表是文档账，不是业务规则。插值 `${rows.length}` 为 main 既有；本材料新引入回读，使标题与变更表分叉。
- **工程缺陷：** 是。建议登记：是。
- **最小处置：** 与 U17-007 同口径择一：内容变化时标题版本递增并在 §7 追加一行；或删除不可维护的变更表、只留标题版本。禁止第三套版本政策。然后 `docs:operation-matrix` 重生 U17 矩阵。同一 PR。

### GR-U18-003 — 域归属生成器新增 `file_version` 列与四张员工表分类规则；须与 CSV/迁移同批，不能单独合进 main

- **完整原文：** `inheritedRules` 增 `staff_shift_revisions` / `staff_locker_records`（L23-24）；`globalTables` 增 `staff_penalty_cap_revisions`（L71）；`classify` 特判 `staff_policy_settings`（L148-152，含「global cap changes require HR, finance, legal and top authority approval」）；`buildCsv` 增 `file_version: "V0.01"`（L201-205）。
- **文件:行号：** `tools/generate-domain-ownership-scope-audit.mjs:23-24`、`:71`、`:148-152`、`:201-205`
- **业务影响：** 未发布树内 `--check` 绿、重生 CSV 与 U13 文件逐字节相同（332 表）。单独把生成器合进 main：`--check` 红；若在 main 迁移上**跑生成**，会给 328 张既有表整列加上 `file_version` 而不增加员工表（员工表在 V400–V409，main 无）。分类口径本身对照 HTML 为细化（见下），不是添加、不是冲突。
- **HTML 原文（细化上位，main 正本）：**
  - 寄存柜（`business-system-map-for-operations-v0.1.html:938`）：「寄存柜不固定，先到先选……柜子自动记下是谁、用了哪一格、什么时间存进去、什么时间取出来」
  - 可见天数（同文件 `:1444`）：「员工看自己未来若干天的排班，可见天数由后台配置（业务参数，按经营主体可配……出厂默认 7 天，须同时配置上下限）」
  - 处罚上限（同文件约 `:1458`）：「各类扣除合计不得超过该员工当月总薪资的 20%（安全阈值，全平台统一可配，调整须经人力、财务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置上下限）」
- **可复现：** CMD-UNPUB-DOMAIN-CWD EXIT 0（332）；CMD-GEN-DOMAIN-UNPUB CSV_IDENTICAL；CMD-OVL3-DOMAIN EXIT 1；CMD-GEN-DOMAIN-UNPUBGEN-MAINMIG 表头变为含 `file_version`，表名差集空，行数 329。
- **冻结判据：** GEN-008、GEN-013、GEN-016、GEN-017
- **2.3 依据类型：** 4（命令）+ 1（HTML 原文，仅用于分类）
- **四分类：** 技术性（列与发布同批）。员工表分类句为细化、本项不把分类当缺陷。
- **无业务发明理由：** `file_version` 是审计 CSV 元数据列。分类句能指到上列 HTML，不是新实体。
- **工程缺陷：** 是（发布不同批会红 / 改写 main CSV 表头）。建议登记：是。
- **最小处置：** 与 U13-001 同一 PR：迁移 ≥V411 落地后，用**经 001 重放的本生成器**跑 `docs:domain-ownership-scope` 重生 CSV；禁止手改 CSV。分类规则随 V402/V406/V407/V409 重编号后的表名保留。`file_version` 列与 U13 产物必须同进同出。

### GR-U18-004 — PII 门禁新增 staff_profiles 三列：未发布树 16 列绿；叠到 main 因明文 name 与哈希列名而红

- **完整原文：**
  ```javascript
  { table: "staff_profiles", column: "name", writeFile: "backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java" },
  { table: "staff_profiles", column: "phone", writeFile: "backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java" },
  { table: "staff_profiles", column: "id_doc", writeFile: "backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java" },
  ```
- **文件:行号：** `tools/verify-pii-encryption.mjs:14-16`
- **业务影响：** 登记本身是 PGM:870 的下位强制（员工姓名/手机/证件落库加密）。未发布 `JdbcStaffRepository:891-893` 已 `piiCipher.encrypt`，16 列 PASS。main 仍 `addValue("name", profile.name())` 且 `phone_hash`/`id_doc_hash`，只合本门禁则 `verify:pii-encryption` 红，发布首跑不过。不单独合本文件。
- **HTML 原文（`platform-governance-map-for-operations-v0.1.html:870`）：** 「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」
- **可复现：** CMD-UNPUB-PII-CWD / CMD-UNPUB-PII-ISOLATED EXIT 0（16）；CMD-MAIN-PII EXIT 0（13）；CMD-OVL3-PII EXIT 1：
  - `staff_profiles.name: addValue 未经 piiCipher.encrypt 包裹 → … addValue("name", profile.name())`
  - `staff_profiles.phone: …未找到 addValue("phone", …)`
  - `staff_profiles.id_doc: …未找到 addValue("id_doc", …)`
  main `:671-673` 实证。
- **冻结判据：** GEN-002、GEN-008、GEN-010、GEN-017
- **2.3 依据类型：** 4 + 1
- **四分类：** 细化（能指 PGM:870）。工程缺陷是发布不同批，不是登记发明业务。
- **工程缺陷：** 是。建议登记：是。
- **最小处置：** 与 U05 `JdbcStaffRepository` 加密写口、U10 V403（重编号后）同一 PR 合入本登记；禁止只合门禁或只合 Java。main 的 `phone_hash`/`id_doc_hash` 路径按 U05 定稿改密文列，不在本段发明第三套列名。

## 6. 添加项清单（供主调度按 2.2 处理，不代用户回答）

**空。** 本段 0 条添加。员工域分类与 staff PII 登记均有 HTML 上位。矩阵生成器不发明 operation。U14-002 自营写口等已在 questions/U04-001.md 答「删除」，不在本段重开。

## 7. 命令证据

工作目录默认 `/home/james/complex-management`。未发布树 `U=.scratch/module-recheck-20260908/unpublished-03acdb98`。隔离根 `/tmp/u18-b1-grok`（已用完可删，不入仓）。

| ID | 命令（摘要） | EXIT | 关键输出 |
|---|---|---:|---|
| CMD-LOCK | `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | 0 | `b012328f…6695a` |
| CMD-HEAD | `git rev-parse HEAD origin/main` | 0 | 均为 `445a49f4…c48f` |
| CMD-TREE | `git cat-file -t e28dad05…` / `03acdb98…` / `242f4872…` | 0 / 128 / 0 | tree / fatal no object / commit |
| CMD-76 | `git diff --numstat BASE TREE` | 0 | 76 files, +4745/−1084 |
| CMD-HASH-3 | `sha256sum U/tools/{三文件}` | 0 | 与清单/开工提示一致 |
| CMD-76JSON | 清单 76 文件逐一 sha256 | 0 | mismatch_count 0 |
| CMD-UNPUB-DOMAIN | 未发布脚本 + **main cwd** | 1 | CSV out of sync（=叠生成器到 main） |
| CMD-UNPUB-DOMAIN-CWD | `cd U && node tools/generate-domain-ownership-scope-audit.mjs --check` | 0 | `DOMAIN_OWNERSHIP_SCOPE_PASS: 332 tables classified` |
| CMD-UNPUB-MATRIX-CWD | `cd U && node tools/generate-openapi-operation-matrix.mjs --check` | 0 | `730 operations checked` |
| CMD-UNPUB-PII-CWD | `cd U && node tools/verify-pii-encryption.mjs` | 0 | `PII_ENCRYPTION_PASS: 16 个 PII 列` |
| CMD-MAIN-DOMAIN | main `--check` | 0 | 328 tables |
| CMD-MAIN-MATRIX | main `--check` | 0 | 720 operations |
| CMD-MAIN-PII | main 门禁 | 0 | 13 列 |
| CMD-OVL3-DOMAIN | `/tmp/.../ovl-3files` 未发布三工具 + main 迁移/CSV/JSON/Java | 1 | domain CSV out of sync |
| CMD-OVL3-MATRIX | 同上 | 0 | 720，标题 V0.1 |
| CMD-OVL3-PII | 同上 | 1 | name 明文；phone/id_doc 无写入点 |
| CMD-UNPUB-PII-ISOLATED | 未发布门禁 + 未发布 16 个 writeFile | 0 | 16 列 |
| CMD-UNPUBGEN-MAINMATRIX | 未发布生成器 + main json/矩阵 | 0 | 720 |
| CMD-MAINGEN-UNPUBMATRIX | main 生成器 + 未发布 json/矩阵 | 1 | out of sync |
| CMD-UNPUBJSON-MAINMATRIX | 未发布 json + main 矩阵 + 未发布生成器 | 1 | out of sync |
| CMD-GEN-UNPUB | `/tmp` 未发布生成器写盘 | 0 | 标题 V0.03；历史行 730 |
| CMD-GEN-MAIN-ON-UNPUBJSON | main 生成器写未发布 json | 0 | 标题 V0.1；历史行仍插值 730 |
| CMD-GEN-DOMAIN-UNPUB | `/tmp` 未发布生成器写 CSV | 0 | CSV_IDENTICAL；header 含 file_version；staff 四表+profiles 行见 log |
| CMD-GEN-DOMAIN-UNPUBGEN-MAINMIG | 未发布生成器 × main 迁移写盘 | 0 | 表头被加 file_version；表名差集空 |
| CMD-OPS | python 点算 json paths | 0 | main 720 / 未发布 730；extra head/options=[] |

未跑：Flyway 对 `complex_test*` 的 migrate（禁写库）；`mvn` 测试；`npm run verify:predeploy` 全链（会碰共享库/过长且非本段最小入口）。本段可执行声称已用上述 `--check`/隔离 generate 覆盖。

## 8. 观察（无新根因，完整登记，建议队列「否」）

1. 本三文件 main blob＝基线，`git log 242f4872..HEAD -- 三路径` 空。M009–M012 修复不在这三路径上。
2. 矩阵生成器文案「基于 yaml 与 json」只读 json。main 既有。json/yaml 差属 U14/U15。
3. 矩阵模板「若与业务 PRD 冲突以 OpenAPI、本文为准」与三份 HTML 最终解释权张力。main 既有句，本材料未改。不新立冲突项。
4. `${rows.length}` 写入 2026-06-12 历史行：main 生成器已有（写 720）。本材料回读使标题再分叉，根因记 002，不把 main 既有插值单独立项。
5. `x-idempotency-required` 任意真值→「是」。main 既有。
6. 版本回读无 `existsSync`：缺矩阵文件时 generate/`--check` 未到友好错误即抛。仓库恒有该文件。铁律 6 收窄，不修。
7. `classify` 每行 `status: "covered"`，`uncovered.length` 分支不可达。首跑不撞。
8. CSV 分类备注为英文。main 既有风格。GEN-015 针对 UI。新 staff 备注仍英文，不另立缺陷。
9. `staff_profiles` 因 `directColumns` 顺序标成 `project_id`，表上另有 `merchant_id`/`company_id`。启发式，不是 HTML 冲突。
10. locker 路径写「current authenticated employee」：join 说明，不是 ACL 实现。BSM 说柜子记是谁/哪一格。
11. PII 正则在逗号后止于同行。未发布 staff 写口单行。多行 `encrypt(` 才漏。首跑不发生。
12. V403 无 `enc:v1:` CHECK。门禁注释对转赠电话已承认靠库 CHECK。属 U10 迁移观察，不在本文件新根因。
13. 未发布 `JdbcPlatformRepository` 仍 encrypt 商户电话。本门禁不削弱 M009 商户电话断言。削弱来自整树覆盖 U08，不在本三文件。
14. V405 文件名 `staff_work_phone_receipts` 实际改考勤 `method` CHECK。属 U10，本 PII 登记未列入不存在的 work_phone 列，正确。
15. 生成器投影 U14 的自营写口/公开注册/`none` 事件进入矩阵。业务根在 U14/U17 已定稿。本生成器不过滤 HTML。不新开添加。
16. CMD-UNPUB-DOMAIN（错误 cwd=main）EXIT 1 与 OVL3-DOMAIN 同因，证明「换树跑脚本却不换输入」即红。
17. 未发布树 3584 文件是基线快照+76 改动，不是 76 文件的稀疏目录。`--check` 必须 cwd 到该根。
18. `verify:pii-encryption` 错误信息打印 `addValue("name", profile.name())` 表达式，不打印 PII 原值。
19. analytics 四表覆盖与 `data_cleanup_jobs` 全局表是 main 既有，本材料未改。
20. 本席未读 `U18-b1-self-raw` 或其它本轮本方结论。

## 9. 结论

**FAIL**

三文件全量三线已做，18 条冻结判据逐项已核，命令证据已落。未发布树内三项 `--check` 绿只证明自洽。发布路径上：只叠本段三工具则 domain/PII 门禁红；矩阵生成器把 730 写入 2026-06-12 历史行且标题回读冻结；整树覆盖才会毁掉 main 上 M009–M012 的兄弟文件修复。无添加项、无未答 questions、无 HTML 修改申请。有未定稿工程缺陷（GR-U18-001…004），故不能 PASS。没有有条件通过。

## U18 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 47569 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 18 段：文档生成与检查工具」（3 文件 492 行 NORMAL，main 主归属 M029 域归属生成器 / M394 矩阵生成器 / M053 PII 门禁；适用判据＝映射行并集 18 项）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。副本目录无独立 `.git`，本席以树对象 + 工作树文件 sha256/blob 核对象，不以 `git -C` 对该目录的 HEAD 为准。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论；原文与哈希本席本节点独立实算。
- 本席 B2-r1：**PASS**（001–005 逐 ID 全 PASS；添加 0、不发新 2.2；发布前须修「是」=001–004，「否」=005）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401；M029 映射行 L6408 共 GEN-001–GEN-018（18 ID）；M053 映射行 L6432 同上 18 ID；M394 映射行 L6773 同上 18 ID；并集 18，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、本段三文件 blob 与树 `e28dad05` 全等、main/基线 blob 三路径相等且 `git log 242f4872..445a49f4 -- 三路径` 空、标题/变更表/PII 登记/域归属规则与 `file_version` 列、HTML PGM L870 / BSM L938 / L1444 / L1458、`questions/U04-001.md` 答复首行「删除」、已定稿 U17-007/U17-008/U16-007/U13-001/U10-001/U05-008、package.json 既有 `docs:domain-ownership-scope` / `verify:domain-ownership-scope` / `docs:operation-matrix` / `verify:operation-matrix` / `verify:pii-encryption`）；未重跑 overlay/生成器 `--check`、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u18-b2-append.md` 中转，落盘后删除。本票不把任何 U-FIX worktree 当作本段已发布修复。

此票只判断 findings.md V1.80 顶部「当前 U18 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U18-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 47569 | `62a22c72d3c39cdea561e82e577be26cbd6e5f4f4a10037d54a5ced50a1952d9` |
| `docs/audit-2026-09/modules/U18-b1-self-raw.md` | V0.01 | 13947 | `68604104ad4448534a3d57090541f5304b0d01c2c8d9740f5a61d779c26bf715` |
| `docs/audit-2026-09/findings.md` | V1.80 | 714117 | `31a1317c727d09d674265a51055b4d1a7223e8bd2c9e6addab9e7d33a488fe09` |
| `docs/audit-2026-09/ledger.md`（只读） | V3.00 | 544536 | `059f7b8c903b30d60fbf510de05cb19ad21171c4659a352706ced859f1a349e3` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`；答复首行精确「删除」 |

候选形态：V1.80 顶部 U18-001…005；questions 0（引用 U04-001 仅作他段已答约束，本段无新 2.2）；添加 0；无 HTML 修改申请。U18 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa`、PGM `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`。副本本段 3/3 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算：域归属生成器 SHA `c364a59c356cece0625d164653bd37c1bf1cb26e6f0b2591becdd0f2b183798b`，`git hash-object` = `7d5be6ba364c45846d121d9be3e0bf5b3d6ae177`；矩阵生成器 SHA `b05d7137b38cd7cb173e1d7a422e2cd3c6dd8effaa306a3ef104a5525c4fb669`，`git hash-object` = `40752625cc0b49040da6896d2247c076b7ca2783`；PII 门禁 SHA `17a5d59856cfca619aa26295dadee86fc13dc584291901e5dab34134e6a38159`，`git hash-object` = `bb5a6785e2d28c1882b2e1ad51862b4ec47960ca`）。基线/main blob 同为域归属 `6fc05e2cd9d477e9cb21d82526e53604ef27faf6`、矩阵生成器 `76bda7d416e38ea49ea2c2716623d090c2de9e4e`、PII 门禁 `13ce9dd5b8644f66410e8a3aa223295c2a6e960b`。`git diff --numstat 242f4872 e28dad05` 本段三路径 = `11	1` 域归属、`3	1` 矩阵、`4	0` PII。全树 `git diff --shortstat 242f4872 e28dad05` = 76 files, +4745/−1084。

### 技能（V0.17 §6 收窄，本节点）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核路由；用户指定 B2 归并覆盖 Arc audit；未改走 audit 工作流 |
| receiving-code-review | 只取其「先核原文再接受或反驳、禁止表演性同意、B2 不修复」；本体若派同会话子代理则成第四席，**未调用** |
| grilling | 只借追问结构自查：(a) 002 作为 U17-008 根因文件与 U17-007 或支是否同向、有无第三套版本政策、(b) 003 技术性而分类句细化不判缺陷、OBS-1 常量列升或留观察、(c) 004 细化且须修是是否与 GR-004 一致、(d) 001 同根仍单列、005 是否写错 §8 第 3/4/9 条 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/hash-object/三文件行号/HTML 锚点/package.json 脚本名/CSV 表头/main JdbcStaff 明文写入；本票不声称本轮新跑 overlay 或 `--check` |
| systematic-debugging / diagnosing-bugs | 只借读错→对照数据流（本段 mjs → U10 迁移 / U13 CSV / U14 JSON / U17 矩阵 / U05 JdbcStaff → main 对照 → HTML）；本轮不建 `/tmp` overlay |
| ponytail-review | 只评最小删除/收窄，不落地。001 以 main 为底重放三 hunk；002 或支择一后 `docs:operation-matrix`；003 同 PR 重生 CSV；004 门禁与 Java 同进；不发明第三套列名或版本政策 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 矩阵生成器标题回读 + §7 写死 + 现值入历史行是否必须作为 U17-008 的根因文件单列？是，见 (a)。
2. 两条或支是否与 GR-002 / U17-007 同向、有无第三套版本政策？同向、无，见 (a)。
3. 域归属 `file_version` 列与四张员工表规则同批是否技术性、分类句是否只细化不判缺陷？是。OBS-1「常量列无信息量」是否应升？否，留观察，见 (b)。
4. 004 细化 + 须修是是否与 GR-004 一致、细化项入须修是否合规？是、合规，见 (c)。
5. 001 与 U13-001/U10-001/U05-008 同根仍在本段单列是否成立？是。005 第 3/4/9 条是否写错 §8？否，见 (d)。

### 完整差集（GR-001…004、§8 观察 1–20、席甲「无新缺陷」、OBS-1…3）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–005。未把本席 finding 降观察。席甲独有项无超出「同批已在第 13 段」句与 OBS-1…3。席甲 B1 票 PASS、以「同批要求已在第 13 段登记」判本段无新缺陷，漏判：只叠本段三工具即两门禁红须在本段登记（001）；矩阵生成器版本机制是 U17-008 根因文件（002，OBS-3 误作观察）；域归属生成器列与规则须与 CSV/迁移同批（003，OBS-1/2 误作观察或未立项）；PII 登记三列须与 U05 写口同批（004，席甲未提）。候选已拆分并接受本席四项须修，本席独立复核后维持。席甲 B1 PASS 已被本席 B1 FAIL 否决，本票不把「无新缺陷」读回。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U18-001 | U18-001 | 否：技术性叠法；只叠三工具 domain/PII 红、矩阵仍绿（掩盖）；以 main 为底重放 3 hunk；禁整树；同根 U13-001/U10-001/U05-008/U17-001 |
| GR-U18-002 | U18-002 | 否：技术性；本文件是 U17-008 根因；或支与 U17-007 同口径；禁止第三套版本政策 |
| GR-U18-003 | U18-003 | 否：技术性（列与发布同批）；员工表分类句为细化、本项不把分类当缺陷 |
| GR-U18-004 | U18-004 | 否：细化（PGM L870 上位）；工程缺陷是发布不同批；禁止只合门禁或只合 Java |
| §8 1–20 | U18-005 | 「含」为闭集转述；第 3/4/9 条未把观察升冲突，也未把 finding 降观察（见 (d)） |
| 席甲 §4「同批」句 / 「无新缺陷」 | U18-001 | 子集且漏判本段路径；并入 GR-001，补上席甲未在本段单列的叠法红 |
| 席甲 OBS-1 `file_version` 列 | U18-003（升列/同批）+ U18-005（只留「常量列无信息量」） | 席甲整条作观察。候选升同批、留无信息量。不改写 GR-003 |
| 席甲 OBS-2 `staff_policy_settings` 分类 | U18-003（分类句细化、不判缺陷）+ U18-005 | 席甲已不判缺陷。候选未把分类句升缺陷。来源格「升」对 OBS-2 过满，见文末，不改票 |
| 席甲 OBS-3 缺省版本回退 | U18-002（升机制） | 席甲作观察。本席 B1 已升技术性。候选升 002。缺省 V0.01 余量可补 005，见文末，不改票 |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U18-001 | GR-U18-001 + 席甲 §4「同批」句 | 技术性（发布叠法/门禁同批，同根 U13-001/U10-001/U05-008/U17-001）/是/是 | **PASS（技术性叠法/是/是；本段仍须单列）** |
| U18-002 | GR-U18-002 + 席甲 OBS-3（升） | 技术性（U17-008 的根因文件；同口径 U17-007/U16-007）/是/是 | **PASS（技术性/是/是；或支与 GR-002 一致，无第三套版本政策）** |
| U18-003 | GR-U18-003 + 席甲 OBS-1/OBS-2 | 技术性（生成器与产物/迁移同批；员工表分类句为细化、不判缺陷）/是/是 | **PASS（技术性/是/是；分类句不升缺陷；常量列无信息量留观察）** |
| U18-004 | Grok GR-U18-004 | 细化（PGM L870 上位；缺陷在发布不同批）/是/是 | **PASS（细化/是/是；细化项入须修合规）** |
| U18-005 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；第 3/4/9 条未写错原意）** |

技术性入队 3（001 叠法/门禁同批、002 矩阵生成器版本机制、003 域归属生成器同批）。细化入队 1（004 PII 登记同批）。登记观察 1（005）。添加 0。questions 0。无 HTML 修改申请。

发布前须修「是」=001–004（4）；「否」=005（1）。与候选汇总一致。席甲 B1 票 PASS 且把叠法红只记到第 13 段、OBS-1/2/3 误降、PII 同批未提，属低估；候选接受本席四项须修并勘误，本席独立复核后维持。

### 重点 (a) U18-002 作为 U17-008 的根因文件、处置与 U17-007 同口径或支 — **PASS（成立；与 GR-002 一致；无第三套版本政策）**

独立核本段矩阵生成器原文与已定稿 U17-007/U17-008/U16-007，不采信候选「U17-008 的根因文件」九字为己证。

**1. 独立技术性项、作为 U17-008 根因文件成立。**

U17-008 定稿最小处置写明：矩阵标题 V0.03 而变更表仍 V0.1、历史行写入现值 730，随**第 18 段矩阵生成器**按 007 同口径修正后重生。手改 U17 矩阵会被下次 `docs:operation-matrix` 覆盖，不是最懒可行。本文件就是那份生成器。本段未发布 diff 正是把硬编码 `V0.1` 改成从现矩阵首行 regex 取版本、缺省 `V0.01`，§7 模板仍写死各行 V0.1，且 2026-06-12「客户端方法级契约闸口」行继续插值 `${rows.length}`。内容从 720 口改到 730 口后，标题可停在现文件版本、变更表不追加——这是 GEN-016 在本文件上的根因，不是 U17 产物的再叙述。

本轮抽出（不采信席甲引文）：

- 未发布 `:83` `const documentVersion = fs.readFileSync(matrixPath, "utf8").split("\n")[0].match(/V\d+\.\d+/)?.[0] ?? "V0.01";`
- 未发布 `:84` 标题模板插入 `${documentVersion}`；`:166-177` §7 各行版本均为 V0.1；`:175` 把 `${rows.length}` 插进 2026-06-12 历史行。
- main `:82` 标题仍硬编码 `V0.1`；main `:173` 同样插值 `rows.length`。
- 未发布矩阵产物 L1 `V0.03`；变更表 2026-06-12 闸口行「总数为 730 个」；main 矩阵 L1 `V0.1`、共 720。

四分类：无与 HTML 相反的新规则，不是冲突；也不是 HTML 沉默下的新能力，不是添加。文档版本机械 → **技术性**。工程缺陷是。发布前须修是（本文件属本段增量，材料引入，不适用 U01「main 既有 / 须修否」）。插值模板 main 已有（写 720）；本材料新引入回读，才出现标题 V0.03 与变更表 V0.1 分叉。与 GR-002 及 U17 B2 对 008 的「材料引入」勘误同向。

**2. 处置两条或支与 GR-002 / U17-007 一致，无第三套。**

GR-002 原文：「与 U17-007 同口径择一：内容变化时标题版本递增并在 §7 追加一行；或删除不可维护的变更表、只留标题版本。禁止第三套版本政策。」候选或支字面同向：§7 对应本矩阵模板（U17-007 的 §9 是附录模板），择一即可，不发明「回读现文件 + 写死变更表」之外的第三套，也不把缺省 `V0.01` 升成单独须修。脚本名既有 `docs:operation-matrix`。票 **PASS**。

席甲 OBS-3 把回读缺省写成观察——`--check` 比的是整文字节，叠本生成器到 main 仍绿，掩盖版本账。升 002 正确。

### 重点 (b) U18-003 技术性而分类句细化不判缺陷；OBS-1「常量列无信息量」留观察 — **PASS（成立；常量列不升缺陷）**

独立核域归属生成器与 HTML，不采信候选「员工表分类句为细化」为己证。

**1. 技术性（列/规则与产物/迁移同批）成立；分类句不升缺陷。**

本轮抽出：

- `:23-24` `inheritedRules` 增 `staff_shift_revisions`、`staff_locker_records`。
- `:71` `globalTables` 增 `staff_penalty_cap_revisions`。
- `:148-152` `classify` 在 analytics 之前特判 `staff_policy_settings`（`scope_id` 混合雇主可见天数 / 全局处罚上限）。
- `:201-205` 每行 `file_version: "V0.01"`，表头 6→7 列。
- 未发布 CSV 表头含 `file_version`；main CSV 表头无该列。
- HTML 正本：BSM L938 寄存柜自动记下是谁/哪一格；L1444 可见天数按经营主体可配；L1458 处罚上限全平台统一可配、人力/财务/法务三审加顶层确认。

GEN-017 顺序：分类句能指上列 HTML → **细化**，不是冲突、不是添加。缺陷不是分类口径发明业务，而是单独合本生成器：main `--check` 红；对 main 迁移跑生成会给既有表整列加上 `file_version` 而不增加员工表（V400–V409 在 main 不存在）。列与发布同批 → 本项 **技术性**。与 GR-003 四分类拆分一致。须修是。最小处置与 U13-001 同一 PR、禁止手改 CSV、分类规则随重编号表名保留、`file_version` 列同进同出。不发明新隔离模型。

**2. 席甲 OBS-1「常量列无信息量」不应升缺陷。**

`file_version` **列本身**（表头 6→7、与 CSV 同批）已在 003。每行重复写死 `V0.01`、对盘点无增量信息，是 GEN-001/ponytail 观察，不是单独的 GEN-016 根因。升它会发明「改成文件头版本、删行内列」的第三套 schema，与 U13-001 已要求的 `file_version` 列同进同出相撞。U13-009 已把 CSV 常量列记观察。005 只留「常量列无信息量」正确。票 **PASS**。

### 重点 (c) U18-004 细化且发布前须修「是」— **PASS（与 GR-004 一致；细化项入须修合规）**

独立核 PII 门禁与 PGM L870、main/未发布 `JdbcStaffRepository`，不采信候选「细化」二字为己证。

**1. 四分类是细化，与 GR-004 一致。**

本轮抽出：

- 未发布 `:14-16` 登记 `staff_profiles.name/phone/id_doc`，写入文件均为 `JdbcStaffRepository.java`。main 登记无 staff 三列。
- 未发布 `JdbcStaffRepository:891-893` 已 `piiCipher.encrypt(profile.name/phone/idDoc())`。
- main `:671-673` 仍 `addValue("name", profile.name())` + `phone_hash` / `id_doc_hash`。
- HTML PGM L870：「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」

登记把「员工」从注释扩进 REGISTRY，能指 L870，不是 HTML 沉默下的新能力（不是添加），也不与 L870 相反（不是冲突）。GEN-017 顺序走到细化即停，不得改标技术性来回避「有 HTML 上位」。GR-004 原文：「四分类：细化（能指 PGM:870）。工程缺陷是发布不同批，不是登记发明业务。」候选分类格同向。

**2. 细化项入「发布前须修」合规。**

GEN-017 四分类是业务来源，不是须修闸。须修闸看工程缺陷与 U01 口径：材料增量（本文件 +3 行登记）且只合门禁则 `verify:pii-encryption` 红（main 姓名明文、phone/id_doc 无写入点）→ 发布首跑不过。缺陷在**发布不同批**，不在登记发明第三套列名。三列（分类 / 工程缺陷 / 须修）独立：细化可以须修，正如添加已答删除也可以须修。U01「main 既有 → 须修否」不适用——main 登记无这三行。最小处置与 U05 加密写口、U10 V403（重编号后）同一 PR；禁止只合门禁或只合 Java；main `phone_hash`/`id_doc_hash` 按 U05 定稿改密文列。不发明业务。票 **PASS**。

### 重点 (d) U18-001 同根仍单列；U18-005 未写错 §8 第 3/4/9 条 — **PASS（单列成立；观察闭集未写错原意）**

**1. 001 与 U13-001 / U10-001 / U05-008 / U17-001 同根仍在本段单列，成立。**

同根不是吞并。U13-001 拥有 CSV/隔离规格/安全基线产物；U10-001 拥有 V400–V409 编号；U05-008 拥有 Java 重放；U17-001 拥有矩阵 md 产物。本段拥有三份**工具文件**。只叠这 3 个工具到 main：domain `--check` 红、PII 红、矩阵 `--check` 仍绿（720，标题 V0.1）——这是本段自己的发布路径，不能只靠 U13 登记。三路径 main blob＝基线，`git log 242f4872..445a49f4 -- 三路径` 空；整树覆盖才会碰到 U08 等已含 M009–M012 修复的兄弟文件并占用 V400–V409。与 GR-001 一致。U01–U17 各段叠法项均按文件所有者单列。技术性。须修是。禁止整树覆盖。票 **PASS**。

**2. 005 相对本席 §8 第 3/4/9 条（用户点名）及闭集。**

**第 3 条。** 本席原文：矩阵模板「若与业务 PRD 冲突以 OpenAPI、本文为准」与三份 HTML 最终解释权张力。main 既有句，本材料未改。不新立冲突项。本轮核未发布 `:90` 与 main 模板同句（全文为「以 OpenAPI、统一口径基准、权限矩阵、OpenAPI 追溯附录和本文为准」）。候选「main 既有不立项」是该句压缩，**未升须修**。沿用 U01 口径（材料 diff 未触及该句）。正确。

**第 4 条。** 本席原文：`${rows.length}` 写入 2026-06-12 历史行，main 生成器已有（写 720）。本材料回读使标题再分叉，根因记 002，不把 main 既有插值单独立项。本轮核 main `:173` 与未发布 `:175` 同插值。候选「插值 main 既有、根因记 002」正确。

**第 9 条。** 本席原文：`staff_profiles` 因 `directColumns` 顺序标成 `project_id`，表上另有 `merchant_id`/`company_id`。启发式，不是 HTML 冲突。候选写成启发式，未升缺陷。正确。

**应升队而被降观察？无。** GR-001…004 全部入 001–004 且须修是。席甲漏判的叠法/版本机制/同批/PII 已入队。OBS-1 升列/同批（无信息量留 005）。OBS-3 升 002。§8.6 无 existsSync 按铁律 6 不修、§8.15 投影 U14 已定稿缺陷不新开添加，均维持观察。无 finding 被降。

票 **PASS**。

### 001 独立裁定（技术性叠法／须修是）

见 (d)。本三路径 main blob＝基线。B1 CMD-OVL3-DOMAIN EXIT 1、CMD-OVL3-PII EXIT 1、CMD-OVL3-MATRIX EXIT 0。最小处置不发明业务：以 main `445a49f4` 为底重放本段 3 文件 hunk；域归属与 U10 ≥V411、U13 CSV 同一 PR 跑既有 `docs:domain-ownership-scope`；PII 与 U05 `JdbcStaffRepository` 同一 PR；矩阵先按 002 修正、U14 json 定稿后再既有 `docs:operation-matrix`。禁止整树覆盖。票 **PASS**。

### 002 独立裁定（技术性／须修是；U17-008 根因文件）

见 (a)。本段生成器按或支择一修正后 `docs:operation-matrix` 重生 U17 矩阵。同一 PR。票 **PASS**。

### 003 独立裁定（技术性／须修是；分类句细化不判缺陷）

见 (b)。与 U13-001 同一 PR 重生 CSV。`file_version` 列同进同出。分类规则随重编号表名保留。票 **PASS**。

### 004 独立裁定（细化／须修是；同根 U05-008 / U10 V403）

见 (c)。门禁登记与 Java 加密写口、重编号后的 V403 同一 PR。禁止只合门禁或只合 Java。不发明第三套列名。票 **PASS**。

### 005 独立裁定（登记／须修否）

见 (d)。双方观察归并，工程缺陷否，发布前须修否。常量列无信息量、分类按写入约定、yaml 文案只读 json、以本文为准张力、插值 main 既有根因在 002、staff_profiles 启发式、无 existsSync、恒 `covered`、英文备注、PII 正则单行、V403 CHECK 属 U10、商户电话仍 encrypt、V405 文件名属 U10、投影不新开添加、换树即红、快照树、不打印 PII 原值、analytics 既有、未读 self-raw，均维持观察。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. U18-003 来源格「席甲 OBS-1/OBS-2（升）」对 OBS-2 过满：OBS-2 的分类句并未升缺陷，只并入「分类句为细化、不判缺陷」。OBS-1 升的是列/同批，不是「常量无信息量」。分类/须修不变。
2. U18-005 OBS-2「U06-001 写读键已修」不得写成已修。U06-001 是已定稿须修项；本席 B1 未写已修；不得把 U-FIX worktree 当作已发布。定稿改为「U06-001 须修写读键，分类按写入约定、不升本段缺陷」。
3. U18-005 可补席甲 OBS-3 升 002 后的余量「缺省 V0.01 静默回退」观察，与 U17-011 同口径。本席 §8.6 是无 `existsSync`（铁律 6 不修），不要把两条并成一条。不升队。
4. U18-005 第 3 条压缩「以 OpenAPI、本文为准」：定稿可补回模板全句「若与业务 PRD 冲突，以 OpenAPI、统一口径基准、权限矩阵、OpenAPI 追溯附录和本文为准」。分类/须修不变，不得删该句。
5. U18-002 变更表「各行写死 V0.1」正确；写入现值的模板行是 `:175`（产物面 U17-008 已定稿为变更表 6 行全 V0.1 且历史闸口行写 730）。根因文件行号以 `:83-84` / `:166-177` / `:175` 为准。
6. U18-004「重编号后 V414」是 V400–V409 保持内部顺序时 V403 的预期号，以 U10 实际重编号为准，不在本段另编迁移号。
7. U18-001 同 PR 集合已含 U05/U10/U13/U14/U17；定稿写明本段 002/003/004 同一 PR。禁止整树覆盖候选已写，保持。
8. U18-005 第 1 条「三文件 main＝基线」即本席「对抗式覆盖不会回退这三路径上的 M009–M012」；不得读成整包覆盖未发布树也无冲突（那是 001 的兄弟文件面）。

全批差集无漏并。四分类与 GR-001…004 一致；席甲 PASS 漏判 001–004 分项、OBS-1/3 误降、PII 未提，候选已勘误。002 作为 U17-008 根因文件成立，或支与 U17-007 一致、无第三套版本政策。003 技术性成立，分类句细化不判缺陷，常量列无信息量留观察。004 细化且须修是合规。001 同根仍单列成立。005 未写错第 3/4/9 条原意，无应升队被降观察项。逐 ID 001–005 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U18 或 main 模块 PASS。步骤仍 B2；U18 IN_PROGRESS；材料未发布，不做 B3。

本批最终：**PASS**
