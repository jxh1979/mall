CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U17 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 17 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U17 — 未发布材料第 17 段「接口覆盖说明」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录 L8330–8337；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件；本席独立核 **76/76 sha256 与清单一致、0 mismatch**
- 本段：2 文件，物理行 1112，标记 **NORMAL**；main 主归属：**M390**（矩阵 md）、**M394**（附录生成器）
- 冻结判据：M390 映射行 L6769 ∪ M394 映射行 L6773 = **43** 项（相对开工提示 MISSING=[] EXTRA=[]）；M394 的 GEN-001–018 是 M390 的真子集；定义区从锁全文读过
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U17-001、GR-U17-002、GR-U17-003、GR-U17-004、GR-U17-005、GR-U17-006、GR-U17-007、GR-U17-008、GR-U17-009、GR-U17-010
- 添加项（B1 只登记，不代用户回答 2.2）：**1 条 = GR-U17-002**，即矩阵把 `POST /api/v1/platform/merchants/{merchant_id}/self-operated`（`setMerchantSelfOperated`）写成正式 operation。操作员 2026-09-09 已对 `questions/U04-001.md` 答「删除」；U14-002 / U15-002 / U16-002 已定稿同根。本段是该添加的**验收矩阵面**，**不新开 2.2、不再问**。无其它新添加。
- 同根已定稿、不新开 2.2：U04-001 / U05-001 / U05-003 / U05-006 / U05-011 / U08-001 / U13-001 / U14-001…U14-008 / U15-001…U15-008 / U16-001…U16-007
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。不得把 U14 JSON / U15 YAML / U16 附录的 B2 定稿写成「本矩阵/生成器已审过、可跳过」。
- 本席独立实跑（隔离 `/tmp/u17-b1-grok*`，不改副本/main）：未发布 json+本矩阵 + 未发布矩阵生成器 `--check` **PASS**（730 operations）；未发布 json+未发布附录 + 未发布本段生成器 `--check` **PASS**（730）；main json+main 矩阵 + main 生成器 **PASS**（720）；main json+main 附录 + 未发布本段生成器 **PASS**（720，标题仍读到 V0.1）；**只把本矩阵覆到 main JSON：`--check` FAIL**（CMD-OVL-MATRIX EXIT 1）；未发布 json+未发布附录 + **main** 附录生成器 **FAIL**（标题版本硬编码 V0.1 vs 材料 V0.03，CMD-OVL-GEN EXIT 1）；未发布 json+本矩阵 + **main** 矩阵生成器 **FAIL**（CMD-UNPUB-MAIN-MGEN EXIT 1）。副本自洽 **不能** 消解 HTML 冲突、添加项或对 main 的契约分叉。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（`disable-model-invocation: true`，本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | 隔离 `/tmp` 叠矩阵/附录/JSON 复现 `--check` 红绿 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段 md/mjs → U14 JSON / U16 附录 / U18 矩阵生成器 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；副本 `--check` 绿不能写成 HTML 通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（独立自营写口、字面量 `none` 事件、双值 locker 事件名、taskGroup 缺 staff） |
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

1. 这 2 个文件必须存在吗？必须有**由正式 P0 OpenAPI 反向生成的逐 operation 验收矩阵**，以及把 JSON 收成权限/事件/任务附录的生成器。仓库门禁 `verify:operation-matrix` / `verify:openapi-closure` 把「矩阵/附录 ↔ JSON 逐字节同步」写成硬条件。删掉矩阵，M390 没有本段这一层，`verify:p0-contract` 红。删掉生成器，`docs:openapi-closure` / `verify:openapi-closure` 无入口。文件作为生成物/生成器是细化+技术性，不是添加。未发布增量（720→730 operation / 10 个新口 / 改 4 个既有 staff 口的事件、终端与摘要）才是本段要审的业务面。
2. 整段按现文件叠进当前 main、首轮会不会过门禁？**只叠本矩阵不会。** main JSON 仍是 720 口基线，`generate-openapi-operation-matrix.mjs --check` 报 out of sync（CMD-OVL-MATRIX EXIT 1）。未发布 json+矩阵同批在 `/tmp` 可绿，只证明未发布树自洽。只叠本段生成器到 main json+附录：`--check` 仍绿（720，标题从现文件读到 V0.1）。未发布 json+附录 + **main** 生成器：红（V0.1 硬编码 vs 材料 V0.03）。
3. 本文件会不会覆盖 main 已修的 M009–M012？**对本两路径不会回退 Java。** `git rev-parse` 显示 main 该 blob == 基线 blob（矩阵 `464ebe10…`，生成器 `77e372c8…`），`git log 242f4872..HEAD -- <本段两路径>` 空。对抗式固定问题的真实形态是：整文件覆盖会把已定稿删除的自营写口、公开无设备注册、字面量 `none` 事件、跨域 `governance.supervisor_review.evaluated` 和柜机双值事件名 **写进**当前 main 的验收矩阵，并把 main 已有的 `staff.shift.saved` / `staff.shift.save_failed` / `staff.attendance.punch_failed` **覆盖掉**，而不是回退 M009 Java。
4. 公开 `POST /staff/self-registrations` 无设备字段、矩阵标 `public`、终端含 `app`，失败事件 `none`。首轮正常客户端按矩阵编码就会发出无绑定的明文手机号。不是两三件不巧。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` **文件内容**同值（sidecar 自身 sha256 是另一值，不参与闸 1）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- M390 映射行 L6769：43 ID，与开工提示逐项相同
- M394 映射行 L6773：GEN-001…GEN-018 共 18 ID
- 并集 43，MISSING=[] EXTRA=[]
- 定义区从锁全文读过（GEN L12–L100；BEQR-097 L1160；BEQR-156 L1809；BEQR-163 L1886；BEQR-166 L1919；BSM-009 L2438；BSM-027 L2716；BSM-028 L2730；BSM-029 L2756；BSM-072 L3780；BSM-073 L3795；BSM-074 L3818；BSM-075 L3841；BSM-076 L3855；BSM-077 L3879；BSM-114 L4719；BSM-115 L4799；BSM-120 L4909；PGM-007 L5156；PGM-022 L5412；PGM-026 L5481；PGM-027 L5506；PGM-028 L5527；PGM-029 L5541；PGM-030 L5573；PGM-031 L5594）
- HTML 出处：三份 HTML 取 **main 当前正本**（需求来源不变）。本段映射含 BEQR/BSM/PGM；分类按 GEN-017 对照原文（见各 finding 引用行）。本段映射 **不含** PGM-032/033（员工和手机专章）；员工设备/注册冲突改用已映射的 BSM-029 / BSM-114 / BSM-120 原文，不引用未冻结 ID。

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger / findings 只读核验（不改；开工提示给出的 V2.95 / V1.77 与本席实算一致）：

- ledger：版本 **V2.95**，534741 bytes，SHA256 `d2603bd2296c3fe36698bf2c33b48d5064ccc4a1d165c0a34309238f8d74699c`
- findings：版本 **V1.77**，693932 bytes，SHA256 `80dcdeee40a86a50a0888fee363696be25cb31c1ec1c99caa278aba088e8f13a`
- 台账中与本段直接相关的已定稿项（背景，不继承 PASS）：U04-001 添加已答删除；U14-001…008 / U15-001…008 / U16-001…007 发布前须修；U16-007 明文把附录标题/变更表交给「第 17 段生成器」；U17 行 ledger 顶部 `IN_PROGRESS`

输入副本核验：

- 矩阵 sha256 `523a47691e13cb87f1548e84ddcf64cc2b1b7f9648486af4861814262fb7e285`（开工提示、清单、`sha256sum` 三方一致）；822 行 / 224648 bytes
- 生成器 sha256 `12f7df9e17834230c24e5d58461625d78e5d2bb00725be1a231f693ecd3cac78`（三方一致）；290 行 / 14368 bytes
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

### 2.2 本段两文件相对基线 / 相对 main

| 路径 | 基线 blob | 未发布树 blob | main HEAD blob | `git log 242f4872..HEAD` | numstat |
|---|---|---|---|---|---|
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | `464ebe10343c79c2e4deac65cebf2e00441589db` | `b8ba1cbc15089cefc1edc707730d054898e7e97e` | **=基线** `464ebe10…` | 空（M009–M012 未改此路径） | +17/−7 |
| `tools/generate-openapi-contract-closure-appendix.mjs` | `77e372c8e00f63d1e57a130e42900fd97e64c38f` | `c44fe9c08b8b4dd0a6befa89cb2d57de63ab8c01` | **=基线** `77e372c8…` | 空 | +3/−1 |

对抗式固定问题（本段特化）：**发布时若整文件覆盖，不会回退 M009–M012 的 Java/迁移，但会用 730 口矩阵替换 main 仍为 720 口且事件名正确的矩阵**，并写入已定稿删除的自营写口、公开无设备注册、`none` 失败事件、跨域成功事件名。`verify:operation-matrix` 在只叠本矩阵时即红（CMD-OVL-MATRIX）。

生成器相对基线的真实 diff（3 插入 / 1 删除）：

```
+// 文件版本：V0.01
-const document = `# P0 OpenAPI 权限、事件、任务追溯收口附录 V0.1
+const documentVersion = fs.readFileSync(appendixPath, "utf8").split("\n")[0].match(/V\d+\.\d+/)?.[0] ?? "V0.01";
+const document = `# P0 OpenAPI 权限、事件、任务追溯收口附录 ${documentVersion}
```

§9 变更表仍写死「2026-06-13 | V0.1 | …」（未发布稿 L270–273，与 main 相同）。标题从现文件第一行回读，**内容变化时版本不递增、变更表不追加**。

### 2.3 矩阵相对 main 的 operation 差（本席从两份全文抽取，不是只看 git hunk）

- 未发布 730 口 / main 720 口；only_unpub=10，only_main=0，同 key 但整行不同=4
- 新增 10 口（矩阵 L744–753）：`listStaffRegistrationCompanies`、`submitStaffSelfRegistration`、`setStaffEmploymentStatus`、`getStaffShiftVisibility`、`setStaffShiftVisibility`、`getStaffPenaltyCap`、`requestStaffPenaltyCap`、`listStaffShiftHistory`、`setMerchantSelfOperated`、`recordStaffLockerScan`
- 改 4 口：
  - `punchStaffAttendance`：main 终端 `staff_private_phone, web` + 失败 `staff.attendance.punch_failed` → 未发布终端 `staff_work_phone` + 失败 `none`
  - `upsertStaffShift` / `batchUpsertStaffShifts`：main 成功/失败 `staff.shift.saved` / `staff.shift.save_failed` → 未发布 `governance.supervisor_review.evaluated` / `none`
  - `submitStaffRegistration`：main 摘要「员工自助注册」、终端 `staff_private_phone, web` → 未发布摘要「商户代员工提交注册」、终端 `web, merchant_app, merchant_portal, staff_work_phone`（此条摘要/终端更贴 HTML 两条路径，见观察）
- 字面量 `none`：未发布 12 行（写口失败 none=8）；main **0**
- `public` 行：未发布 7 / main 5（多出公司列表与本人注册）

## 3. 逐文件全量三线

数据流（只读）：本段矩阵 md ← U18 `tools/generate-openapi-operation-matrix.mjs`（未发布副本 +3/−1，本段不审该文件的修复，只追调用）← U14 `docs/openapi/openapi-p0.json`；本段生成器 mjs → U16 `docs/p0-openapi-contract-closure-appendix-v0.1.md` ← 同一 JSON。再对照 main 当前源码（两路径 blob=基线）→ 无本段 SQL/迁移 → main 三份 HTML 原文。

覆盖表：

| 文件 | 物理行 | 读法 | 覆盖 |
|---|---:|---|---|
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | 822 | L1–23 定位/强制规则；L22–753 表头+730 行（分段 L24–200 / 200–400 / 400–580 / 580–753）；L754–822 AC-OP-001…008、边界表、用户故事、变更记录 | 全文，不是只看 diff |
| `tools/generate-openapi-contract-closure-appendix.mjs` | 290 | L1–90 读 JSON/抽 operation；L91–167 权限/事件/任务表；L168–273 文档模板含写死 §9；L276–290 `--check`/写盘 | 全文 |

### 3.1 文件 A — 验收矩阵

**第一性原理**

1. 为什么必须存在：HTML 要求员工注册、权限、终端、事件留痕可执行（BSM-028/029/114/120）。矩阵自称「P0 可编码接口、权限、终端、幂等、审计事件和测试验收的逐接口矩阵」（L3–L7）。删掉它，`verify:operation-matrix` 无比较物，开发/测试失去逐口执行入口。存在本身是细化+技术性。
2. 声称是否执行：未发布 json+本文件+未发布矩阵生成器 `--check` 绿（730）。这只证明与 U14 JSON 同步，不证明 HTML。main 叠本文件红。
3. 未验证就当真的前提：把 OpenAPI 的 `x-permission`/`x-terminal-allowed`/`x-audit-event-*` 当作已拍板业务口径；把标题 V0.03 当作变更已入账；把 `public`+`app` 当作可编码正式口。

**顺从式（43 项见 §4）**

- 强制规则 L10–16 与 AC-OP-001…008 要求每个 operation 进 Controller/权限/事件/用例，写口幂等，事件进事件中心，终端按 D5。表体 730 行把 JSON 投影出来，结构完整。
- 基线 720 行（票务/酒吧/合同/供应/商户等）相对 main 未改；BEQR-097/156/163/166、BSM-072–077、PGM-007/022/026–031 在本段增量上无新口，投影面不新开冲突。
- 未发布增量破坏了矩阵自己的 AC：AC-OP-006（L777–779）要求失败写入事件中心，表中 8 个写口失败列为 `none`；AC-OP-002 要求客人/设备凭证不写成员工权限，但公开注册两口把身份放进 `public` 且终端含 `app`。

**对抗式**

- 最小反例 1：只把本 md 拷到 main 工作区跑 `node tools/generate-openapi-operation-matrix.mjs --check` → EXIT 1（CMD-OVL-MATRIX）。
- 最小反例 2：按 L745 编码 `POST /staff/self-registrations`，无设备公钥/指纹、`security` 空、终端 `app` → 与 BSM-120 L1975 / BSM-029 L2770「安装时生成并绑定设备标识」冲突；首轮正常客户端就会发。
- 最小反例 3：覆盖后 main 的 `staff.shift.saved` 从矩阵消失，排班成功事件被写成 `governance.supervisor_review.evaluated`（L628–629），与 `evaluateSupervisorReview`（L288）撞名。
- 新理论极端（两文件都不存在才生成）：不作为缺陷。矩阵文件在仓内基线即存在。

### 3.2 文件 B — 附录生成器

**第一性原理**

1. 为什么必须存在：`package.json` `docs:openapi-closure` / `verify:openapi-closure` 的唯一实现。删掉则附录无法从 JSON 重生，U16 定稿的「禁止手改附录」无法执行。
2. 声称是否执行：未发布 json+附录+本生成器 `--check` 绿 730；本生成器+main json+main 附录绿 720；main 生成器+未发布附录红（标题 V0.1 vs V0.03）。
3. 未验证前提：从现附录第一行抓版本等于「版本随内容走」；`permissionMatrix.includes(permission)` 等于「人工矩阵已覆盖」；`taskGroup` 缺省 `P0-待归类` 等于可执行归类。

**顺从式**

- 生成器只投影 JSON，不发明业务枚举；模板声明「不是新需求池」（L185）。技术性成立。
- `--check` 是能失败的同步闸（叠错 EXIT 1），满足 GEN-003 对「断言能失败」的窄口径；它不是 HTML 符合性测试。
- L44–66 `taskGroup` 无 `staff`（亦无 floor-plan / merchant-service 等基线标签）。未发布 JSON `tags[0]=staff` 的 30 个 operation（含新增 10 个）全部落入 `P0-待归类`。附录实文件可检索：`setMerchantSelfOperated` / `submitStaffSelfRegistration` 任务行均标 `P0-待归类`。

**对抗式**

- 最小反例：本生成器 L174 在附录不存在时 `readFileSync` 抛 ENOENT（CMD-GEN-NOFILE EXIT 1）。**收窄**：仓库基线附录始终存在，首轮正常 `docs:openapi-closure` 不会撞上；记观察，不单独立项。
- 首轮正常会撞上的：内容从 720 口改到 730 口后，标题仍读现文件 V0.03、§9 仍只有 V0.1 一行（L270–273 写死）。U16-007 已把该根因交给本段生成器。
- `String.includes` 对 `"-"` 在三个人工矩阵上全为 true（CMD-INCLUDES）；本 JSON 无缺 `x-permission` 的 operation（missingPerm=0），**首轮不触发误标**，记观察。
- `includes("none")` 在人工事件矩阵为 false，故 `none` 事件被标「接口级事件，以本附录为准」——至少没把 none 伪造成「已在正文出现」。

## 4. 冻结判据逐项

| ID | 本段适用面 | 证据 | 本段结论 |
|---|---|---|---|
| GEN-001 | 两文件必要性；自营写口多余轴 | 矩阵/生成器有门禁用途；`setMerchantSelfOperated` 无独立 HTML 上位 | 生成物必要；002 添加须删 |
| GEN-002 | `--check` 与叠法 | CMD-SELF 绿 / CMD-OVL-MATRIX 红 | 001 成立 |
| GEN-003 | 本段无测试文件；`--check` 能红 | 叠错 EXIT 1 | 无独立测试缺陷；不把绿写成行为通过 |
| GEN-004 | 公开注册无设备证明 | 矩阵 L744–745；HTML BSM-120 L1975 | 003 冲突 |
| GEN-005 | 公开口默认拒绝/鉴权 | L744–745 `public` + 终端 `app` | 003 |
| GEN-006 | 无本段状态机/金额写入 | 两文件不写库 | 无实现面 |
| GEN-007 | 两写口幂等列「否」 | L746、L748；AC-OP-004 L770 | 010 同根 U14-006 |
| GEN-008 | 矩阵↔JSON↔附录↔main | 730 vs 720；事件名分叉 | 001/004/005/006 |
| GEN-009 | 写口失败 `none` | 8 个写口；AC-OP-006 | 004 |
| GEN-010 | 公开注册 PII、无设备绑定 | L745 | 003 |
| GEN-011 | 事件名 `none` / 双值 / 跨域撞名 | L628–631、L744–753 | 004/005/006 |
| GEN-012 | 无循环/批处理实现 | 生成器 O(operations) 一次读 JSON | 无实现面 |
| GEN-013 | npm 脚本入口 | `docs:openapi-closure` / `verify:openapi-closure` / `verify:operation-matrix` | 生成器必要；叠法须同 PR |
| GEN-014 | 本段无 SQL | — | 无实现面 |
| GEN-015 | 展示中文 | 新增 10 行摘要中文；约 11 条英文摘要为 main 既有 P2 行 | 观察，不新开 |
| GEN-016 | 标题 V0.03 vs 变更表 V0.1；待归类 | 矩阵 L1/L821；生成器 L174/L270 | 007/008/009 |
| GEN-017 | 四分类 | 002 添加（已答删除）；003 冲突；其余技术性 | 见 findings |
| GEN-018 | 依据四类 | 本席命令+HTML 原文 | 本 raw |
| BEQR-097 | 供应合同三模式 | 矩阵含 `listSupplyContracts` 等基线行，本段未改 | 无新冲突 |
| BEQR-156 | 合同状态 | 基线合同口未改 | 无新冲突 |
| BEQR-163 | 合同变更八类 | 基线 `raiseContractChange` 摘要「6 类」main 既有 | 观察（M390） |
| BEQR-166 | 合同审批链 | 基线审批口未改 | 无新冲突 |
| BSM-009 | 基础管理入口 | 矩阵覆盖员工/商户/合同口 | 细化 |
| BSM-027 | 综合体盘子 | 无本段增量 | 无新冲突 |
| BSM-028 | 六类合作方式；员工注册审批 | L2741 六类含「自营」；独立布尔写口=添加 | 002 |
| BSM-029 | 注册信息、设备绑定 | L2770 | 003 |
| BSM-072–077 | 商户/招商合同 | 基线行未改；自营写口不在这些章发明 | 002 用 BSM-028/PGM-026 |
| BSM-114 | 入口：私人手机含注册/打卡 | L1865；公开注册含 `app` | 003；打卡终端见观察 |
| BSM-115 | 子系统连接 | 无本段新连接发明 | 无新冲突 |
| BSM-120 | 设备标识、寄存柜自动留痕 | L1975–1976 | 003/005 |
| PGM-007 | 合同准入卡片 | 基线 | 无新冲突 |
| PGM-022 | 空间点位 | 基线 | 无新冲突 |
| PGM-026 | 商户档案；六类合作 | L5492 | 002 |
| PGM-027–031 | 商户边界/合同准入 | 基线合同口未改 | 无新冲突 |

无实现面的判据（GEN-006/012/014 及未改动的合同/供应锚点）原因：本段是文档生成物+生成器，不写库、不跑金额、不改合同状态机。不能把 HTML 沉默判冲突。

## 5. Findings（临时 ID，正式 ID 仅 B2 分配）

### GR-U17-001 — 只叠本矩阵到 main 即门禁红；整文件覆盖会把 730 口契约写进仍为 720 口的 main

- 文件:行号：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md:1-21,20,744-753`；对照 main 同路径 L1（V0.1 / 720 口）
- 完整原文（L20）：`> 当前 docs/openapi/openapi-p0.json 共 730 个 operation。`
- 业务影响：发布者若把未发布树整包或本 md 单盖到 445a49f4，会（1）让 `verify:operation-matrix` 立即红；（2）把 10 个未审定口和 4 个已改事件/终端的既有口写进当前 main 矩阵，而 main 此 blob 自基线未动、M009–M012 未修它。Java 不会被本文件回退，但契约字典会被污染。
- 可复现：CMD-OVL-MATRIX（§7）EXIT 1；`git rev-parse HEAD:docs/p0-openapi-operation-acceptance-matrix-v0.1.md` = 基线 `464ebe10…`
- 冻结判据：GEN-002、GEN-008、GEN-013、GEN-016
- 2.3 依据类型：4（命令）
- 四分类：**技术性**（发布叠法/生成物同步；无新业务概念。无业务发明理由：门禁比较的是字节同步，不是员工规则）
- 工程缺陷：成立。建议登记：是。同根 U14-001 / U15-001 / U16-001 / U13-001
- 最小处置（写给发布者，本轮不修）：以 main 445a49f4 为底，U14 json 按 002–008 定稿后运行 `npm run docs:operation-matrix` 重生本矩阵并 `verify:operation-matrix`；禁止手改 md 独走；与 U14/U15/U16/U18 **同一 PR**；禁止整树覆盖。

### GR-U17-002 — 矩阵把独立自营布尔写口收为正式 operation（添加，已答删除）

- 文件:行号：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md:752`
- 完整原文：`| staff | POST | /api/v1/platform/merchants/{merchant_id}/self-operated | setMerchantSelfOperated | 标注商户是否自营 | object | merchant.profile.manage | staff-token | project | web, staff_work_phone, staff_private_phone | 是 | merchant.self_operated_changed | merchant.self_operated_change_failed |`
- 业务影响：在六类合作方式之外增加第二轴「是否自营」写口；员工分流可与合同合作方式分叉。HTML 只说在商户档案上**标注为自营**（六类之一），没有独立 POST。
- HTML 原文：`docs/business-system-map-for-operations-v0.1.html` BSM-028「建立商户档案，区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式。」；PGM-026 同旨；BSM L1433 / PGM L578「在商户档案上标注为自营的商户视同公司自有」。
- 可复现：矩阵 L752；未发布 JSON path `/api/v1/platform/merchants/{merchant_id}/self-operated`；main 矩阵无此行。
- 冻结判据：GEN-001、GEN-008、GEN-017、BSM-028、PGM-026
- 2.3 依据类型：1（HTML 原文）+ 4
- 四分类：**添加**（指不出独立布尔写口的上位条目；六类「自营」已有合作方式码）。操作员已对 U04-001 答「删除」。
- 工程缺陷：成立。建议登记：是。同根 U14-002 / U15-002 / U16-002 / U04-001。**不再问。**
- 最小处置：随 U14-002 删 path 后重生矩阵；tags=staff 的平台路径一并消失。

### GR-U17-003 — 公开本人注册与公司列表：无设备证明、终端含 app、失败事件 none

- 文件:行号：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md:744-745`
- 完整原文：
  - L744：`| staff | GET | /api/v1/staff/registration-companies | listStaffRegistrationCompanies | 获取可注册公司 | - | public | public | self | app, staff_private_phone | 否 | none | none |`
  - L745：`| staff | POST | /api/v1/staff/self-registrations | submitStaffSelfRegistration | 公司员工本人注册 | object | public | public | self | app, staff_private_phone | 是 | staff.self_registered | none |`
- 业务影响：矩阵作为「执行入口」把无设备绑定的公开注册写成可编码正式口；首轮客户端按表会从 `app` 提交明文资料。公司列表同样匿名可发现。
- HTML 原文：BSM-120「已拍板：员工私人手机 App 绑定一个该手机专属的设备标识，把手机和员工绑定」（业务系统地图 L1975）；BSM-029「用户权限要知道员工注册信息、设备绑定、私人手机、公司工作手机」（L2770）；BSM-114 私人手机入口含「注册」（L1865）。接口存在是两条注册路径的细化；**缺设备标识 + 终端含 app** 与明文冲突。
- 可复现：矩阵上述两行；main 无此两行；public 行 7 vs main 5。
- 冻结判据：GEN-004、GEN-005、GEN-010、BSM-029、BSM-114、BSM-120
- 2.3 依据类型：1
- 四分类：**冲突**（不是 HTML 沉默）。同根 U14-003 / U15-003 / U16-003 / U05-003 / U01-001。不新开 2.2。
- 工程缺陷：成立。建议登记：是。
- 最小处置：随 U14-003（请求增加 `device_public_key`/`device_fingerprint_hash` 必填、去掉 `app`、public 加频控）改 JSON 后重生矩阵。

### GR-U17-004 — 12 行事件名字面量 none（8 个写口失败）

- 文件:行号：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md:628,629,631,744-751,753`；AC-OP-006 L777–779
- 完整原文（代表）：L631 `| … punchStaffAttendance … | staff.attendance.punched | none |`；L777–779「Then 事件中心必须记录…失败原因…」
- 业务影响：写失败无法检索；与同文件 AC-OP-006 自相矛盾。main 同三口原为 `staff.shift.save_failed` / `staff.attendance.punch_failed`，覆盖即丢失。
- 可复现：本席抽取 none 行 12、写口失败 none 8；main none 行 0。
- 冻结判据：GEN-009、GEN-011、BSM-120
- 2.3 依据类型：4 + 同文件 AC
- 四分类：**技术性**（事件标识符缺失，不是新业务状态。无业务发明理由：`none` 不是 HTML 枚举，是投影层把空失败名写成可编码值）
- 工程缺陷：成立。建议登记：是。同根 U14-008 / U15-008 / U16-004
- 最小处置：随 U14-008 给写口 `*_failed`、读口 `*.viewed/*.view_failed`，禁止字面量 none；重生矩阵。GET 四处 none 由发布者统一 GET 策略。

### GR-U17-005 — 寄存柜成功事件双值字面量

- 文件:行号：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md:753`
- 完整原文：`| staff | POST | /api/v1/staff/locker-events | recordStaffLockerScan | 记录寄存柜扫码存取 | object | staff.profile.self | staff-token | project | kiosk | 是 | staff.locker.store / staff.locker.retrieve | none |`
- 业务影响：一个 operation 的成功事件列无法作为单一事件名落库；身份模型（员工 token + kiosk 终端）是 U14-005 的双身份契约在矩阵上的投影。HTML 要求「由柜子自动记录，不设手工登记」（BSM-120 L1976）。
- 可复现：L753；main 无此行。
- 冻结判据：GEN-008、GEN-011、BSM-120
- 2.3 依据类型：1 + 4
- 四分类：**技术性**（事件标识符/调用形态，不是新寄存柜业务。无业务发明理由：HTML 已有寄存柜自动留痕；缺陷是双值名与双身份，不是柜子本身）
- 工程缺陷：成立。建议登记：是。同根 U14-005 / U15-005 / U16-005 / U05-006
- 最小处置：随 U14-005 单一成功名（动作进明细）与柜机单端契约改 JSON 后重生。

### GR-U17-006 — 三口成功事件盗用 governance.supervisor_review.evaluated

- 文件:行号：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md:628,629,750`；对照 L288 `evaluateSupervisorReview` 正当使用同一成功名
- 完整原文（L628）：`| staff | POST | /api/v1/staff/shifts | upsertStaffShift | 主管排班(双端) | … | governance.supervisor_review.evaluated | none |`
- 业务影响：排班提交、批量排班、处罚上限申请的成功留痕与主管复核评估口撞名；覆盖后 main 矩阵失去 `staff.shift.saved`。U14-008 只定失败=`none`，成功侧串台由 U16-006 定稿，本段是矩阵面。
- 可复现：三行 vs main L628–629 `staff.shift.saved` / `staff.shift.save_failed`；L288 仍为评估口。
- 冻结判据：GEN-008、GEN-011
- 2.3 依据类型：4
- 四分类：**技术性**（审计事件标识符串台。无业务发明理由：两成功名只是 `x-audit-event-success` 标识符，不新增审批业务；U16-006 已写明）
- 工程缺陷：成立。建议登记：是。同根 U16-006
- 最小处置：三口改为 `staff.shift.submitted`（单/批）与 `staff.payroll.penalty_cap_requested`；`governance.supervisor_review.evaluated` 只留给 `evaluateSupervisorReview`；重生矩阵。服务 `recordSuccess` 仍归 U06。

### GR-U17-007 — 本段附录生成器：标题从现文件回读、§9 写死 V0.1，内容变化不入账

- 文件:行号：`tools/generate-openapi-contract-closure-appendix.mjs:174-175,270-273`
- 完整原文：L174 `const documentVersion = fs.readFileSync(appendixPath, "utf8").split("\n")[0].match(/V\d+\.\d+/)?.[0] ?? "V0.01";`；L270–273 变更表仅 `| 2026-06-13 | V0.1 | 新增由 OpenAPI 反向生成的… |`
- 业务影响：720→730 口后附录标题可停在 V0.03、变更记录仍像 V0.1；读者无法从 §9 知道新增了自营写口/公开注册。main 生成器硬编码 V0.1，对未发布附录 `--check` 恒红（CMD-OVL-GEN EXIT 1）。U16-007 已把根因交给本文件。
- 可复现：CMD-OVL-GEN EXIT 1；CMD-OVL-GEN-MAIN 用本生成器对 main 附录 PASS 720（读到 V0.1，与写死表一致，**掩盖**了「内容变了版本不变」）。
- 冻结判据：GEN-016、GEN-002
- 2.3 依据类型：4
- 四分类：**技术性**（文档版本机械，无业务发明）
- 工程缺陷：成立。建议登记：是。同根 U16-007
- 最小处置：内容变化时标题版本递增并在 §9 追加一行，或删除不可维护的变更表只留标题版本；然后重生 U16 附录。同一 PR。

### GR-U17-008 — 本段矩阵标题 V0.03，变更记录只有 V0.1，且把 730 写进 2026-06-12 的历史行

- 文件:行号：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md:1,20,821`
- 完整原文：L1 `# P0 OpenAPI Operation 验收矩阵 V0.03`；L821 `| 2026-06-12 | V0.1 客户端方法级契约闸口 | … P0 operation 总数为 730 个 … |`
- 业务影响：与 007 同机制，产物在本段。生成器是 U18 `generate-openapi-operation-matrix.mjs`（本段不改那份源，但本 md 不可验证）。main 同行写 720，日期同为 2026-06-12，说明模板把 `rows.length` 插进历史行。
- 可复现：两文件 L821 对照；未发布矩阵生成器 L83 同样从现文件读版本（U18，只读追链）。
- 冻结判据：GEN-016
- 2.3 依据类型：4
- 四分类：**技术性**
- 工程缺陷：成立。建议登记：是。机制同 U16-007，**根因文件属 U18**，本段登记产物面。
- 最小处置：随第 18 段矩阵生成器按 007 同口径修正后 `docs:operation-matrix` 重生本文件。

### GR-U17-009 — 附录生成器 taskGroup 无 staff，新增员工口全部落入 P0-待归类

- 文件:行号：`tools/generate-openapi-contract-closure-appendix.mjs:44-66`；下游附录（U16，只读）L2400/L2437/L2477
- 完整原文：L44–66 枚举 access/auth/platform/…/ops，缺省 `return "P0-待归类"`，**无 `tag === "staff"`**。
- 业务影响：未发布 JSON 30 个 `tags[0]=staff` 的 operation（含本段矩阵新增 10 个）在附录第 5 节不能按员工任务簇检索。U16-008 观察 7 明确「建议第 17 段处理」。基线 floor-plan / merchant-service 等缺项仍属 M394 既有债，本项只升 **staff**（本段增量第一轮就会进待归类）。
- 可复现：CMD-TASKGROUP `staff submitStaffSelfRegistration -> P0-待归类`；`access submitStaffRegistration -> AUTH-账号权限`。
- 冻结判据：GEN-008、GEN-016
- 2.3 依据类型：4
- 四分类：**技术性**（执行归类标签，模板已声明不替代正式任务编号。无业务发明理由：不新增员工流程，只是缺映射）
- 工程缺陷：成立。建议登记：是。
- 最小处置：`taskGroup` 增加 `tag === "staff" → 如 "STAFF-员工"`（或并入 AUTH）；重生附录。不要为 floor-plan 等基线标签在本未发布段顺手扩一圈（那些不是本段增量第一轮才出现）。

### GR-U17-010 — 两员工写口幂等列为「否」

- 文件:行号：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md:746,748`；AC-OP-004 L770
- 完整原文：L746 `setStaffEmploymentStatus` … `| 否 | staff.profile.employment_status_changed | none |`；L748 `setStaffShiftVisibility` … `| 否 |`
- 业务影响：任职状态与排班可见天数可重复提交；矩阵 AC-OP-004 要求声明了幂等或涉及不可重复动作的接口校验 `X-Idempotency-Key`。同批其它员工写口为「是」。投影自 U14 JSON `x-idempotency-required: false`。
- 可复现：L746/L748 幂等列；main 无此两行。
- 冻结判据：GEN-007、GEN-008
- 2.3 依据类型：4
- 四分类：**技术性**（幂等闸，无新业务规则。无业务发明理由：HTML 未发明「任职状态可不幂等」）
- 工程缺陷：成立。建议登记：是。同根 U14-006 / U15-006 / U05-011
- 最小处置：随 U14-006 两口改 `x-idempotency-required: true` 并加 `idempotency_key` 后重生矩阵。

## 6. 添加项清单（供主调度按 2.2 处理，不代用户回答）

| 临时 ID | 添加了什么 | 2.2 状态 |
|---|---|---|
| GR-U17-002 | 矩阵 L752 `setMerchantSelfOperated` 独立自营布尔写口 | 已答删除（`questions/U04-001.md`），引用即，**不要再写 questions/U17-*.md** |

无其它添加。公开注册接口本身是 HTML 两条路径的细化，冲突在缺设备/含 app（003），不是添加。

## 7. 命令证据

隔离目录 `/tmp/u17-b1-grok*`，不改副本/main，不装依赖，不写 `complex_test*`。

| ID | 命令要点 | EXIT | 关键输出 |
|---|---|---:|---|
| CMD-LOCK | `sha256sum …/criteria-lock.md` | 0 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| CMD-76 | `git diff --shortstat 242f4872 e28dad05` | 0 | `76 files changed, 4745 insertions(+), 1084 deletions(-)` |
| CMD-HASH76 | 清单 vs 副本 sha256 | 0 | ok 76 mismatch 0 |
| CMD-BLOB | `git rev-parse` 三方 blob | 0 | 矩阵/生成器 main=基线 ≠ 未发布 |
| CMD-SELF-MATRIX | 未发布 json+矩阵+未发布矩阵生成器 `--check` | **0** | `730 operations checked` |
| CMD-SELF-APPX | 未发布 json+附录+本段生成器 `--check` | **0** | `730 operations checked` |
| CMD-MAIN-MATRIX | main json+矩阵+main 生成器 | **0** | `720 operations checked` |
| CMD-MAIN-APPX | main json+附录+main 生成器 | **0** | `720 operations checked` |
| CMD-OVL-MATRIX | main json + **本矩阵** + main 矩阵生成器 | **1** | `out of sync … docs:operation-matrix` |
| CMD-OVL-GEN | 未发布 json+附录 + **main** 附录生成器 | **1** | `out of sync … docs:openapi-closure` |
| CMD-OVL-GEN-MAIN | main json+附录 + **本段** 生成器 | **0** | `720 operations checked`（读到 V0.1） |
| CMD-UNPUB-MAIN-MGEN | 未发布 json+矩阵 + **main** 矩阵生成器 | **1** | `out of sync` |
| CMD-TASKGROUP | 复刻 L44–66 | 0 | staff 口 → `P0-待归类` |
| CMD-INCLUDES | 人工三矩阵 `includes` | 0 | `none` 不在事件矩阵；`"-"` 三份全 true |
| CMD-GEN-NOFILE | 无附录跑 generate | **1** | ENOENT（收窄为观察） |
| CMD-CAT | `git cat-file -t 03acdb98` | 128 | `could not get object info` |

未运行任何会清理共享库的脚本。未把未运行的 Java/前端测试写通过。

## 8. 观察（无新根因，不漏并）

1. 本两路径 main blob = 发布基线；M009–M012 检查点未改它们。对抗式分叉是「用未发布契约字典覆盖 main 仍正确的 720 口矩阵」，不是回退 Java。
2. 副本 `--check` 绿 ≠ HTML 通过，也 ≠ 可单独合入 main。
3. `submitStaffRegistration` 摘要从 main「员工自助注册」改为「商户代员工提交注册」、终端改为商户端+工作手机，**更贴** HTML 两条路径（BSM L1433 / PGM L578）。属细化改进，不单独立项；与 003 的公开本人注册口分工后才自洽。
4. 打卡终端收窄为 `staff_work_phone`（L631）贴 BSM L1445「考勤打卡从领取工作手机开始算」；BSM L930/L1865 仍把「考勤打卡」写在私人手机入口。与 U16-008 同一 HTML 张力，**只登记给调度按人闸处理，本席不改 HTML、不单独立项**。
5. GET 可见天数「未配置的上下限为空」在 U14 JSON description，**本矩阵无 description 列**，U14-004 不在本文件正文出现。
6. `StaffRegistrationRequest` schema/example 互斥（U14-007）在 JSON/Java，矩阵只显示 schema 名。
7. 约 11 条英文摘要（`List P2 amusement…` 等）为 main 既有行，归 M390 基线；新增 10 行中文。
8. `raiseContractChange` 摘要「6 类」vs HTML 八类，main 既有，归 M390。
9. `includes("-")` 对人工矩阵恒真，但本 JSON missingPerm=0，首轮不误标；不升项。
10. 无附录时 generate ENOENT：要「附录被删光再生成」才发生，按铁律 6 不修。
11. 人工三矩阵不在 76 文件；生成器读取 main 副本中的同名文件（若存在）只影响「覆盖状态」列，不改变 operation 投影。
12. 矩阵生成器属 U18，本段只追数据流；008 产物面交给 U18 生成器修正。
13. 本段无 SQL、无 Java、无前端、无测试文件。GEN-006/012/014 无实现面。
14. public 7 vs main 5，增量即 003 两口。
15. staff tag 30（main 约 20 + 新增 10），全部受 009 影响。
16. 未发布 commit 对象本仓不存在，只以 tree `e28dad05` 与输入副本为准。
17. wecom 留存行、历史路径隔离 AC、§1「以本文为准」均为基线既有，归 M390。
18. 生成器权限代表 operation 截断 12 个加 `...`（L148）为既有行为，不单独立项。
19. ledger/findings 哈希与开工提示一致（V2.95 / V1.77），U17 开工行已写 IN_PROGRESS。
20. 本席未读 `U17-b1-self-raw` 或本轮其它本方结论。

## 9. 结论

**FAIL**

没有有条件通过。10 条 finding 均有四类证据之一；添加项 1 条已答删除不再问；43 条冻结判据均已逐项核对；两文件全文三线已跑；与 main 分叉已用 blob + `--check` + 逐口差集写明。发布前须按 U14 契约定稿后重生本矩阵，并修正本段附录生成器的版本/变更表与 staff 任务簇；禁止把未发布树整包盖到 445a49f4。

## U17 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 48965 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 17 段：接口覆盖说明」（2 文件 1112 行 NORMAL，main 主归属 M390 矩阵 md / M394 附录生成器；适用判据＝映射行并集 43 项）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论；原文与哈希本席本节点独立实算。
- 本席 B2-r1：**PASS**（001–011 逐 ID 全 PASS；添加 1 已答删除、不发新 2.2；发布前须修「是」=001–010，「否」=011）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401；M390 映射行 L6769 共 43 ID；M394 映射行 L6773 共 GEN-001–GEN-018（18 ID，为 M390 真子集）；并集 43，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、本段 blob 与树 `e28dad05` 全等、main/基线 blob 矩阵 `464ebe10…` / 生成器 `77e372c8…`、未发布矩阵 SHA `523a4769…` blob `b8ba1cbc…`、未发布生成器 SHA `12f7df9e…` blob `c44fe9c0…`、标题/变更表/taskGroup/矩阵 730 行与 none/public/staff 计数、HTML BSM L701/L930/L935/L1433/L1443/L1445/L1865/L1975–1976、PGM L578/L579/L582、`questions/U04-001.md` 答复首行「删除」、已定稿 U14-001…008 / U15-001…008 / U16-001…007 / U04-001 / U05-003/006/011 / U01-001、package.json 既有 `docs:operation-matrix` / `verify:operation-matrix` / `docs:openapi-closure` / `verify:openapi-closure`）；未重跑 overlay/生成器 `--check`、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u17-b2-append.md` 中转，落盘后删除。本票不把任何 U-FIX worktree 当作本段已发布修复。

此票只判断 findings.md V1.78 顶部「当前 U17 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U17-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 48965 | `41c4f2ec6068630d81e9524e26d7fce695ff3907a8889652cbaefc8947e563f6` |
| `docs/audit-2026-09/modules/U17-b1-self-raw.md` | V0.01 | 11647 | `21485c63a5a00a82decbc1d2f1a792854f2f6e9784c55312f5ba74437c5d7b23` |
| `docs/audit-2026-09/findings.md` | V1.78 | 703501 | `36232b46950518f3658cafe9dd3c9f659911d81b9aaa609e001bc50949914ac7` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.96 | 536823 | `3755ef216bd3f18d6c576ac436723c89f170f83f2b4fe74bbdcef5dea978f30f` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`；答复首行精确「删除」 |

候选形态：V1.78 顶部 U17-001…011；questions 0（引用 U04-001）；添加 1 已答删除；无 HTML 修改申请。U17 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa`、PGM `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`。副本本段 2/2 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算：矩阵 SHA `523a47691e13cb87f1548e84ddcf64cc2b1b7f9648486af4861814262fb7e285`，`git hash-object` = `b8ba1cbc15089cefc1edc707730d054898e7e97e`；生成器 SHA `12f7df9e17834230c24e5d58461625d78e5d2bb00725be1a231f693ecd3cac78`，`git hash-object` = `c44fe9c08b8b4dd0a6befa89cb2d57de63ab8c01`）。基线/main blob 同为矩阵 `464ebe10343c79c2e4deac65cebf2e00441589db`、生成器 `77e372c8e00f63d1e57a130e42900fd97e64c38f`。`git diff --numstat 242f4872 e28dad05` 本段两路径 = `17	7` 矩阵、`3	1` 生成器。全树 `git diff --shortstat 242f4872 e28dad05` = 76 files, +4745/−1084。

### 技能（V0.17 §6 收窄，本节点）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核路由；用户指定 B2 归并覆盖 Arc audit；未改走 audit 工作流 |
| receiving-code-review | 只取其「先核原文再接受或反驳、禁止表演性同意、B2 不修复」；本体若派同会话子代理则成第四席，**未调用** |
| grilling | 只借追问结构自查：(a) 007 作为 U16-007 根因文件独立技术性项与或支是否与 GR-007 同向、(b) 008 产物面交 U18 与席甲「main 既有」勘误、(c) 009 从 U16-008 观察升须修与 staff 映射是否发明业务、(d) 003/002/001/004/005/006/010 四分类与 011 是否写错 §8 第 3/4/17 条 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/hash-object/矩阵行号与计数/生成器读取面与 taskGroup/HTML 锚点/package.json 脚本名；本票不声称本轮新跑 overlay 或 `--check` |
| systematic-debugging / diagnosing-bugs | 只借读错→对照数据流（本矩阵 md → U18 矩阵生成器 / U14 JSON；本生成器 mjs → U16 附录 → main 720 → HTML）；本轮不建 `/tmp` overlay |
| ponytail-review | 只评最小删除/收窄，不落地。001/002–006/010 只从定稿 json 用既有 `docs:operation-matrix` 重生；007/009 改本段生成器后 `docs:openapi-closure`；008 改 U18 生成器后重生；不在 md 上另做一遍业务修补 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 附录生成器标题回读 + §9 写死 V0.1 是否必须作为 U16-007 的根因文件单列？是，见 (a)。
2. 两条或支是否与 GR-007 同向、有无第三套版本政策？同向、无，见 (a)。
3. 矩阵标题 V0.03 / 变更记录写 730 是否产物面、根因是否交 U18？是。席甲「归 M390 既有」是否应勘误为材料引入？同意，见 (b)。
4. taskGroup 无 staff 从 U16 观察升本段须修是否成立、映射是否发明业务、是否扩到基线缺项？成立、不发明、不扩，见 (c)。
5. 003 冲突 / 002 添加已答删除 / 001·004·005·006·010 技术性是否与 GR 四分类一致？是。011 第 3/4/17 条是否写错 §8？否，见 (d)。

### 完整差集（GR-001…010、§8 观察 1–20、席甲 CX-001、OBS-1…2）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–011。未把本席 finding 降观察。席甲独有项无超出 CX-U17-001 与 OBS-1…2。席甲 B1 以一条「派生物随第 14 段重生成」覆盖，漏判 002/003/004/005/006/010 分项；OBS-1/OBS-2 误判为机械实现 / main 既有，应升 007/008；009 未提。候选已拆分并接受本席十项须修，本席独立复核后维持。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U17-001 | U17-001 | 否：技术性叠法；只叠本矩阵 `--check` 红；以 main 为底，U14 json 定稿后 `docs:operation-matrix` 重生；禁手改 md 独走；禁整树 |
| GR-U17-002 | U17-002 | 否：添加；已答删除；随 U14-002 删 path 后重生；不新开 2.2 |
| GR-U17-003 | U17-003 | 否：冲突；公开注册无设备证明 + 终端含 app + 失败 none；同根 U14-003 |
| GR-U17-004 | U17-004 | 否：技术性；12 行字面量 `none`（8 个写口失败）；同根 U14-008 |
| GR-U17-005 | U17-005 | 否：技术性；locker 成功事件双值字面量；同根 U14-005 |
| GR-U17-006 | U17-006 | 否：技术性；成功侧串台；operation 级成功名；同根 U16-006 |
| GR-U17-007 | U17-007 | 否：技术性；本段附录生成器标题回读 + §9 写死；U16-007 根因文件 |
| GR-U17-008 | U17-008 | 否：技术性产物面；标题 V0.03 / 历史行写 730；根因交 U18 |
| GR-U17-009 | U17-009 | 否：技术性；taskGroup 无 staff；只升本段增量，不扩基线缺项 |
| GR-U17-010 | U17-010 | 否：技术性；两写口幂等列「否」；同根 U14-006 |
| §8 1–20 | U17-011 | 「含」为闭集转述；第 3/4/17 条未把观察升冲突，也未把 finding 降观察（见 (d)） |
| 席甲 CX-U17-001 叠法/重生句 | U17-001 | 子集（「随第 14 段契约修正后 docs:operation-matrix 重生成」）；并入 GR-001，补上席甲未单列的叠法红与分项 |
| 席甲 CX-U17-001 自营写口行 | U17-002 | 席甲漏判为独立添加项；候选已拆出，与 GR-002 同 |
| 席甲 CX-U17-001 public/`none`/locker/串台/幂等 | U17-003 / 004 / 005 / 006 / 010 | 席甲捆在一条重生里；候选按 GR 拆冲突与技术性 |
| 席甲 OBS-1 版本回读 | U17-007（升）+ U17-011（只留缺省回退） | 席甲作机械实现；本席 B1 已升技术性。候选升 007，缺省 V0.01 静默回退留 011，不改写 |
| 席甲 OBS-2 变更记录写现值 | U17-008（升） | 席甲归 M390 既有；候选升产物面并交 U18。本席同意勘误（见 (b)） |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U17-001 | GR-U17-001 + 席甲 CX-U17-001 | 技术性（发布叠法/生成物，同根 U14-001/U15-001/U16-001/U13-001）/是/是 | **PASS（技术性叠法/是/是；只引用既有 `docs:operation-matrix`）** |
| U17-002 | Grok GR-U17-002 | 添加（操作员已答删除）/是/是 | **PASS（添加已答删除/是/是；不新开 2.2）** |
| U17-003 | Grok GR-U17-003 | 冲突（同根 U14-003/U15-003/U16-003/U05-003/U01-001）/是/是 | **PASS（冲突/是/是；与 GR-003 四分类一致）** |
| U17-004 | Grok GR-U17-004 | 技术性（同根 U14-008/U15-008/U16-004）/是/是 | **PASS（技术性/是/是）** |
| U17-005 | Grok GR-U17-005 | 技术性（同根 U14-005/U15-005/U16-005/U05-006）/是/是 | **PASS（技术性/是/是）** |
| U17-006 | Grok GR-U17-006 | 技术性（同根 U16-006）/是/是 | **PASS（技术性/是/是；事件名无业务发明；不并入 U14-008）** |
| U17-007 | GR-U17-007 + 席甲 OBS-1（升） | 技术性（U16-007 的根因文件）/是/是 | **PASS（技术性/是/是；或支与 GR-007 一致，无第三套版本政策）** |
| U17-008 | GR-U17-008 + 席甲 OBS-2（升） | 技术性（产物面；根因属第 18 段）/是/是 | **PASS（技术性产物面/是/是；同意席甲「main 既有」勘误为材料引入）** |
| U17-009 | Grok GR-U17-009（U16-008 观察 7 升） | 技术性/是/是 | **PASS（技术性/是/是；staff 映射无业务发明，不扩基线缺项）** |
| U17-010 | Grok GR-U17-010 | 技术性（同根 U14-006/U15-006/U05-011）/是/是 | **PASS（技术性/是/是）** |
| U17-011 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；第 3/4/17 条未写错原意）** |

技术性入队 8（001 叠法、004 none 事件、005 双值名、006 成功事件串台、007 附录生成器版本机制、008 矩阵版本产物面、009 taskGroup 无 staff、010 幂等）。添加入队 1（002，已答删除）。冲突入队 1（003 公开注册无设备证明）。登记观察 1（011）。questions 0。无 HTML 修改申请。

发布前须修「是」=001–010（10）；「否」=011（1）。与候选汇总一致。席甲 B1 票 FAIL 但只立一条重生且漏 002–006/010 分项、把 007/008 降观察、009 未提，属低估；候选接受本席十项须修并勘误，本席独立复核后维持。

### 重点 (a) U17-007 作为 U16-007 的根因文件、独立技术性项 — **PASS（成立；或支与 GR-007 一致，无第三套版本政策）**

独立核本段生成器原文，不采信候选「U16-007 的根因文件」八字为己证。对照本席 GR-U17-007、已定稿 U16-007、main 生成器 L173、未发布生成器 L174–175 / L270–273。

**1. 独立技术性项、作为 U16-007 根因文件成立。**

U16-007 定稿最小处置写明：附录标题 V0.03 而 §9 仅 V0.1，随**第 17 段生成器**修正后重生。手改 U16 附录会被下次 `docs:openapi-closure` 覆盖，不是最懒可行。本文件就是那份生成器。本段未发布 diff 正是把硬编码 `V0.1` 改成从现附录首行 regex 取版本、缺省 `V0.01`，§9 模板仍写死 `| 2026-06-13 | V0.1 | … |`。内容从 720 口改到 730 口后，标题可停在现文件版本、变更表不追加——这是 GEN-016（版本与完成声明须可复现）在本文件上的根因，不是 U16 产物的再叙述。

本轮抽出（不采信席甲引文）：

- 未发布 `:174` `const documentVersion = fs.readFileSync(appendixPath, "utf8").split("\n")[0].match(/V\d+\.\d+/)?.[0] ?? "V0.01";`
- 未发布 `:175` 标题模板插入 `${documentVersion}`；`:270-273` §9 仅 V0.1 一行。
- main `:173` 标题仍硬编码 `V0.1`。本席 B1 CMD-OVL-GEN：未发布附录 + main 生成器 EXIT 1；CMD-OVL-GEN-MAIN：本生成器 + main 附录 PASS 720（读到 V0.1，**掩盖**「内容变了版本不变」）。

四分类：无与 HTML 相反的新规则，不是冲突；也不是 HTML 沉默下的新能力，不是添加。文档版本机械 → **技术性**。工程缺陷是。发布前须修是（本文件属本段增量，材料引入，不适用 U01「main 既有 / 须修否」）。

**2. 处置两条或支与 GR-007 一致，无第三套。**

GR-007 原文：「内容变化时标题版本递增并在 §9 追加一行，或删除不可维护的变更表只留标题版本；然后重生 U16 附录。同一 PR。」候选或支字面同向：择一即可，不发明「回读现文件 + 写死变更表」之外的第三套版本政策，也不把缺省 `V0.01` 升成单独须修（静默回退留 011）。脚本名既有 `docs:openapi-closure`。票 **PASS**。

席甲 OBS-1 把回读缺省写成「机械实现、不影响门禁」——门禁比的是整文字节，掩盖了版本账。升 007 正确；不把 OBS-1 整条留观察。

### 重点 (b) U17-008 产物面、根因交第 18 段；席甲「归 M390 既有」勘误 — **PASS（成立；同意材料引入）**

独立核矩阵产物与两边矩阵生成器，不采信候选「根因文件属第 18 段」为己证。

**1. 产物面在本段、根因文件属 U18，成立。**

本段文件是矩阵 md，不是 `tools/generate-openapi-operation-matrix.mjs`（U18）。本轮核：

- 未发布矩阵 L1 `# P0 OpenAPI Operation 验收矩阵 V0.03`；L20 共 730 个 operation；L821 `| 2026-06-12 | V0.1 客户端方法级契约闸口 | … 总数为 730 个 … |`；L822 仍为 2026-06-13 V0.1 附录闸口行。变更表各行版本均为 V0.1。
- main 矩阵 L1 `V0.1`；L20 共 720；L811 同行写 720。
- 未发布 U18 生成器 `:83` 与本段附录生成器同构：从现矩阵首行回读版本，缺省 `V0.01`；`:175` 模板把 `${rows.length}` 插进 2026-06-12 历史行。
- main U18 生成器 `:82` **标题写死 `V0.1`**；`:173` 同样插值 `rows.length`。

手改本 md 会被 `docs:operation-matrix` 覆盖。最小处置是 U18 生成器按 007 同口径（递增并追加变更行，**或**删变更表只留标题）后重生。本段只登记产物面。与 GR-008 一致。技术性。须修是。同一 PR。

**2. 同意席甲 OBS-2「归 M390 既有范式」应勘误为材料引入。**

U01「main 既有」口径：材料 diff 未触及、main HEAD 同位置已有 → 须修否。此处不成立：

- main 生成器标题硬编码 V0.1，标题与变更表同停在 V0.1，**没有**「标题 V0.03 而变更表仍 V0.1」这条分叉。
- 未发布 U18 生成器把标题改成从现文件回读，才让重生后的本矩阵出现 V0.03 / V0.1 分叉。该读取面是未发布树相对基线的 +3/−1（U18 文件），产物落在本段 md 的 +17/−7。
- 插值 `rows.length` 进 2026-06-12 历史行：main 模板已有（main 写 720）。这是同族 GEN-016 的旧模板债，但本材料重生后把 **730** 写进历史行，与新的标题回读叠在同一份不可验证的变更账上。008 须修的是**产物面已分叉**，不是把 main 的插值模板单独立成 U17 新业务。

席甲 OBS-2 把整件事收成「生成器模板写死、归 M390 既有」低估了本材料引入的标题回读。升 008、根因交 U18，本席同意。票 **PASS**。

### 重点 (c) U17-009 taskGroup 无 staff 从 U16-008 观察升须修「是」— **PASS（成立；映射无业务发明，不扩基线缺项）**

独立核生成器 `taskGroup` 与未发布 json tags，不采信候选「U16-008 观察 7 升项」为己证。

**1. 升本段须修成立。**

- 未发布本段生成器 L44–66 与 **main 同函数**：枚举 access/auth/platform/…/ops，无 `tag === "staff"`，缺省 `"P0-待归类"`。材料 **未改** taskGroup 函数体。
- 未发布 json `tags[0]=staff` **30** 个（main 20 + 本批新增 10）。新增 10 口第一轮全部落入附录 §5「P0-待归类」，不能按员工任务簇检索。本席 B1 CMD-TASKGROUP：`staff submitStaffSelfRegistration -> P0-待归类`；`access submitStaffRegistration -> AUTH-账号权限`。
- U16-008 观察 7 原文：「建议第 17 段处理。不单独立项（首轮不因此单独红门禁）。」那是 **U16 不拥有该生成器** 时的观察。本段拥有该文件。U16 已 REVIEWED-FAIL，扩写 U16-008 等于重开已闭环段。升到 U17-009 是材料未发布、U16 已定稿之后的最懒可行路，与 GR-009 及 U16 自己的「交第 17 段」同向。

U01 口径：函数体是 main 既有，但本材料 **加重**——新增 10 个 staff 口第一轮进待归类。须修「是」只覆盖被加重的 staff 增量面。GEN-008 / GEN-016。

**2. 最小处置无业务发明、不扩基线缺项。**

GR-009：「`taskGroup` 增加 `tag === "staff" → 如 "STAFF-员工"`（或并入 AUTH）；重生附录。不要为 floor-plan 等基线标签在本未发布段顺手扩一圈。」候选字面同向。模板 L215 已声明任务簇「不替代正式任务编号」。`STAFF-员工` 或并入已有 `AUTH-账号权限` 只是执行归类标签，不新增员工流程、不新开 2.2。floor-plan / merchant-service 等缺项材料未加重，归 M394 既有债，本段不顺手扩圈。票 **PASS**。

### 重点 (d) 003 冲突、002 添加已答删除、001/004/005/006/010 技术性；011 未写错 §8 — **PASS（与 GR 四分类一致；第 3/4/17 条未写错）**

独立核，不采信候选「按 Grok」为己证。GEN-017 顺序：冲突 → 细化 → 技术性 → 添加。

| ID | GR-U17 四分类 | 候选 U17 | 同根 |
|---|---|---|---|
| 001 | 技术性（叠法） | 技术性 | U14-001 / U15-001 / U16-001 / U13-001 |
| 002 | 添加（已答删除） | 添加（已答删除） | U14-002 / U15-002 / U16-002 / U04-001 |
| 003 | 冲突 | 冲突 | U14-003 / U05-003 / U01-001 |
| 004 | 技术性（none） | 技术性 | U14-008 / U15-008 / U16-004 |
| 005 | 技术性（双值名） | 技术性 | U14-005 / U05-006 |
| 006 | 技术性（串台） | 技术性 | U16-006 |
| 010 | 技术性（幂等） | 技术性 | U14-006 / U05-011 |

本轮原文（副本矩阵 + HTML，本席抽出）：

- L752 `setMerchantSelfOperated` 收为正式 operation。HTML BSM L701 六类已含「自营」；BSM L1433 / PGM L578 标注落在六类码，没有独立 POST。`questions/U04-001.md` 答复首行「删除」→ **添加**，引用已答，不新开 2.2。与 GR-002 同分类。
- L744–745 `listStaffRegistrationCompanies` / `submitStaffSelfRegistration`：`public`、终端 `app, staff_private_phone`、失败 `none`。public 7 vs main 5。HTML PGM L578/L579、BSM L935/L1865/L1975：本人注册在**私人手机 App**，安装时绑定设备标识；客人端 `app` 不是员工注册通道。公开无指纹与绑定顺序相反 → **冲突优先**。与 GR-003 同分类。公开注册本身是 HTML 两条路径的细化，缺陷是缺设备/错终端，不是添加。
- L1–21 / L20 730 口；只叠本矩阵到 main JSON（仍 720）门禁红（B1 CMD-OVL-MATRIX EXIT 1）。叠法/生成物同步，无新实体 → **技术性**。与 GR-001 同分类。
- none 行 12、写口失败 none 8；main 0。AC-OP-006 L777–779 要求失败可检索。缺名是契约投影 → **技术性**。与 GR-004 同分类。
- L753 成功事件 `staff.locker.store / staff.locker.retrieve`。HTML BSM-120 L1976 / PGM L582 已要求柜子自动留痕；缺陷是事件名编码 → **技术性**。与 GR-005 同分类。
- L628/L629/L750 成功事件 `governance.supervisor_review.evaluated`；L288 评估口正当同名；main L628–629 为 `staff.shift.saved`。标识符串台，不发明审批业务 → **技术性**。与 GR-006 同分类。不并入 U14-008（U14-008 只覆盖失败 none；U16-006 已单列成功侧）。
- L746/L748 幂等列「否」；AC-OP-004 L770。投影自 json `x-idempotency-required: false` → **技术性**。与 GR-010 同分类。

001/004/005/006/007/008/009/010 保持技术性：闸口一致性、失败可定位、事件名可索引、版本可复现、任务簇可检索、幂等闸。无业务发明。

**011 相对本席 §8 第 3/4/17 条（用户点名）及闭集：**

**第 3 条。** 本席原文：`submitStaffRegistration` 摘要从 main「员工自助注册」改为「商户代员工提交注册」、终端改为商户端+工作手机，**更贴** HTML 两条路径（BSM L1433 / PGM L578）；属细化改进，不单独立项；与 003 的公开本人注册口分工后才自洽。本轮核未发布矩阵 L306 确为「商户代员工提交注册」、终端 `web, merchant_app, merchant_portal, staff_work_phone`。候选「摘要改商户代提交更贴 HTML 为细化，不立项」是该句压缩，**未升须修**。正确。定稿宜补回「与 003 分工后才自洽」，见文末。

**第 4 条。** 本席原文：打卡终端收窄为 `staff_work_phone`（L631）贴 BSM L1445；BSM L930/L1865 仍把「考勤打卡」写在私人手机入口；与 U16-008 同一 HTML 张力，**只登记**给调度按人闸处理，本席不改 HTML、不单独立项。本轮核 L631 终端确为 `staff_work_phone`；main 同行 `staff_private_phone, web`。候选写成细化 + 张力只登记、不改 HTML。正确。

**第 17 条。** 本席原文：wecom 留存行、历史路径隔离 AC、§1「以本文为准」均为基线既有，归 M390。候选补「处置是改 JSON 后重生、**不得删该句**」——未改成建议删除，也未把该句升缺陷。沿用 U01「main 既有」口径，并接 U16 B2 对「不得删」的非阻断补回。正确。

**应升队而被降观察？无。** GR-001…010 全部入 001–010 且须修是。席甲漏判的 002–006/010 已入队。OBS-1 升 007（缺省回退留 011）。OBS-2 升 008。§8.5 U14-004 描述不在矩阵、§8.6 U14-007 在 JSON/Java、§8.7 英文摘要 / §8.8 合同「6 类」main 既有归 M390，均维持观察。无 finding 被降。

票 **PASS**。

### 001 独立裁定（技术性叠法／须修是）

见 (d)。本路径 main blob＝基线 blob `464ebe10…` / `77e372c8…`。B1 CMD-OVL-MATRIX EXIT 1。最小处置不发明业务：以 main `445a49f4` 为底，U14 json 按 002–008 定稿后运行既有 `docs:operation-matrix` 重生本矩阵并 `verify:operation-matrix`；与 U14/U15/U16/U18 **及本段 007/009** 同一 PR。禁止手改 md 独走。禁止整树覆盖。票 **PASS**。

### 002 独立裁定（添加已答删除／须修是；同根 U14-002 / U04-001）

见 (d)。L752 独立布尔写口。答复首行「删除」。随 U14-002 删 path 后重生矩阵（tags=staff 的平台路径一并消失）。不新开 2.2。票 **PASS**。

### 003 独立裁定（冲突／须修是；同根 U14-003）

见 (d)。公开自助注册无设备公钥/指纹，终端含 `app`。处置「随 U14-003 改 json 后重生」继承 U14-003 定稿：设备公钥/指纹必填（无指纹 4xx）、去掉 `app`、public 保留但加频控、公司列表若仍需匿名发现须另证来源校验且不能作为无绑定注册的前置。不在矩阵上发明另一套注册规则。不新开 2.2。票 **PASS**。

### 004 独立裁定（技术性／须修是；同根 U14-008）

12 行字面量 `none`（8 个写口失败）。随 U14-008 给写口 `*_failed`、读口 `*.viewed/*.view_failed`，禁止字面量 none；打卡至少恢复 `staff.attendance.punch_failed`。重生矩阵。`recordFailure` 归第 5/6 段同 PR。票 **PASS**。

### 005 独立裁定（技术性／须修是；同根 U14-005）

L753 双值成功事件。随 U14-005：单一成功名或按 `action` 分两个稳定名，失败名随 004；柜机单端契约。重生矩阵。票 **PASS**。

### 006 独立裁定（技术性／须修是；同根 U16-006；不并入 U14-008）

见 (d)。三口改回各自 operation 级成功事件名（`staff.shift.submitted` 单/批、`staff.payroll.penalty_cap_requested`）；`governance.supervisor_review.evaluated` 只留评估口。名字是 `x-audit-event-success` 标识符，括号「排班提交待审」引用 HTML BSM L1443，不发明新审批态。`recordSuccess` 归 U06 同 PR。票 **PASS**。

### 007 独立裁定（技术性／须修是；U16-007 根因文件）

见 (a)。本段生成器按或支择一修正后 `docs:openapi-closure` 重生 U16 附录。同一 PR。票 **PASS**。

### 008 独立裁定（技术性产物面／须修是；根因 U18）

见 (b)。随第 18 段矩阵生成器按 007 同口径修正后 `docs:operation-matrix` 重生本文件。同一 PR。票 **PASS**。

### 009 独立裁定（技术性／须修是；只升 staff）

见 (c)。`taskGroup` 增加 staff 映射后重生附录。不为 floor-plan 等基线标签扩圈。票 **PASS**。

### 010 独立裁定（技术性／须修是；同根 U14-006）

L746/L748 幂等列「否」。随 U14-006 两口改 `x-idempotency-required: true` 并加 `idempotency_key` 后重生矩阵。票 **PASS**。

### 011 独立裁定（登记／须修否）

见 (d)。双方观察归并，工程缺陷否，发布前须修否。缺省 V0.01 静默回退、打卡终端细化与 HTML 入口张力、L1「以本文为准」基线既有不得删、英文摘要、合同「6 类」、ENOENT、人工三矩阵不在 76 文件，均维持观察。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. U17-008「变更记录只有 2026-06-12 V0.1 一行」压缩过窄：变更表实有 6 行、全部 V0.1；L821 是把现值 730 写入历史行的那一行，L822 仍在。定稿写成「标题 V0.03，变更表各行仍为 V0.1，且 L821 历史行写入现值 730」。
2. U17-008 可写明：插值 `rows.length` 进 2026-06-12 历史行在 main 生成器模板已有（main 写 720）；本材料新引入的是标题从文件回读导致 V0.03 与变更表 V0.1 分叉。须修仍是，因产物面已分叉。分类/根因文件不变。
3. U17-001 同 PR 集合补本段 007/009（附录生成器修正）；脚本名既有 `docs:operation-matrix` / `verify:operation-matrix`。禁止整树覆盖候选已写，保持。
4. U17-011 第 3 条压缩「摘要改商户代提交为细化」：定稿补回本席「与 003 的公开本人注册口分工后才自洽」。分类/须修不变。
5. U17-003 同根格候选含 U07-004（沿 U16 非阻断补频控链）；GR-003 原文未列该 ID。可保留作同根提示，不改冲突分类。压缩句不得丢掉「私人手机通道 / 无指纹 4xx / 公司列表不能作为无绑定注册的前置」。
6. U17-007 行号候选 `:268-274`：以本席 L174–175 / L270–273 为准。或支择一，不发明第三套版本政策。
7. U17-009「如 STAFF-员工，或并入 AUTH」是或支，发布者择一即可；不得顺手为 floor-plan / merchant-service 扩映射。
8. U17-006 定稿保持：两成功名只是标识符；`recordSuccess` 归 U06；不并入 U14-008。
9. U17-011 第 17 条已写「不得删该句」——保持，不要再压成只剩「基线既有」。
10. U17-011 第 1 条「两路径 main＝基线」即本席「对抗式覆盖不会回退 M009–M012 Java」；不得读成整包覆盖未发布 OpenAPI 也无冲突（那是 001/002/003 的字典背书面）。

全批差集无漏并。四分类与 GR-001…010 一致；席甲一条重生漏判 002–006/010 分项、OBS-1/2 误降、009 未提，候选已勘误。007 作为 U16-007 根因文件成立，或支无第三套版本政策。008 产物面交 U18 成立，同意「main 既有」勘误为材料引入。009 升须修成立，staff 映射无业务发明、不扩基线缺项。003 冲突、002 添加已答删除、001/004/005/006/010 技术性与 GR 一致。011 未写错第 3/4/17 条原意，无应升队被降观察项。逐 ID 001–011 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U17 或 main 模块 PASS。步骤仍 B2；U17 IN_PROGRESS；材料未发布，不做 B3。

本批最终：**PASS**
