CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U16 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 16 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U16 — 未发布材料第 16 段「接口覆盖说明」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录 L8322–8328；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件；本席独立核 76/76 sha256 与清单一致
- 本段：1 文件，物理行 2615，标记 **NORMAL**；main 主归属：**M389**（`docs/p0-openapi-contract-closure-appendix-v0.1.md`）
- 冻结判据：M389 映射行 L6768 并集 **43** 项（相对开工提示 MISSING=[] EXTRA=[]）；定义区从锁全文读过
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U16-001、GR-U16-002、GR-U16-003、GR-U16-004、GR-U16-005、GR-U16-006、GR-U16-007
- 添加项（B1 只登记，不代用户回答 2.2）：**1 条 = GR-U16-002**，即附录把 `POST /api/v1/platform/merchants/{merchant_id}/self-operated`（`setMerchantSelfOperated`）写进权限表/事件字典/任务追溯表。操作员 2026-09-09 已对 `questions/U04-001.md` 答「删除」；U14-002 / U15-002 已定稿同根。本段是该添加的**生成附录面**，**不新开 2.2、不再问**。无其它新添加。
- 同根已定稿、不新开 2.2：U04-001 / U05-001 / U05-003 / U05-006 / U05-011 / U08-001 / U13-001 / U14-001…U14-008 / U15-001…U15-008
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。不得把 U14 JSON / U15 YAML 的 B2 定稿写成「本附录已审过、可跳过」。
- 本席独立实跑（隔离 `/tmp/u16-b1-grok*`，不改副本/main）：未发布 json+本附录 + 未发布生成器 `generate-openapi-contract-closure-appendix.mjs --check` **PASS**（730 operations）；main json+main 附录 + main 生成器 **PASS**（720）；**只把本附录覆到 main JSON：`--check` FAIL**（CMD-OVL EXIT 1）；未发布 json+本附录 + **main** 生成器 **FAIL**（标题版本硬编码 V0.1 vs 材料 V0.03）。副本自洽 **不能** 消解 HTML 冲突、添加项或对 main 的契约分叉。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（`disable-model-invocation: true`，本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | 隔离 `/tmp` 叠附录/JSON 复现 `--check` 红绿 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段 md → U14 JSON / U15 YAML / U17 生成器 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；副本 `--check` 绿不能写成 HTML 通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（独立自营写口、字面量 `none` 事件、双值 locker 事件名） |
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

1. 这 1 个 2615 行附录必须存在吗？必须有**由正式 P0 OpenAPI 反向生成的权限/事件/任务追溯面**。仓库门禁 `verify:openapi-closure`（`generate-openapi-contract-closure-appendix.mjs --check`）把「附录 ↔ JSON 逐字节同步」写成硬条件。删掉它，M389 没有本段这一层，`verify:openapi-closure` 红。文件作为生成附录是细化+技术性，不是添加。未发布增量（720→730 operation / 10 个新口 / 改 4 个既有 staff 口的事件与终端）才是本段要审的业务面。
2. 整段按现文件叠进当前 main、首轮会不会过门禁？**只叠本附录不会。** main JSON 仍是 720 口基线，`--check` 报 out of sync（CMD-OVL EXIT 1）。未发布 json+附录同批在 `/tmp` 可绿，只证明未发布树自洽。
3. 本文件会不会覆盖 main 已修的 M009–M012？**对本路径不会回退 Java。** `git rev-parse` 显示 main 该 blob == 基线 blob `94714d4e…`，`git log 242f4872..HEAD -- docs/p0-openapi-contract-closure-appendix-v0.1.md` 空。对抗式固定问题的真实形态是：整文件覆盖会把已定稿删除的自营写口、公开无设备注册、字面量 `none` 事件和跨域 `governance.supervisor_review.evaluated` **写进**当前 main 的权限/事件/任务字典，而不是回退 M009 Java。
4. 公开 `POST /staff/self-registrations` 无设备字段、`security: []`、`x-terminal-allowed` 含 `app`，附录第 3/5 节把它标成 `public` 正式口。首轮正常客户端按附录编码就会发出无绑定的明文手机号。不是两三件不巧。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值
- `criteria-lock.md` 版本 V0.11；判据总数 401
- M389 映射行 L6768：`[BEQR-097, BEQR-156, BEQR-163, BEQR-166, BSM-009, BSM-027, BSM-028, BSM-029, BSM-072, BSM-073, BSM-074, BSM-075, BSM-076, BSM-077, BSM-114, BSM-115, BSM-120, GEN-001 … GEN-018, PGM-007, PGM-022, PGM-026, PGM-027, PGM-028, PGM-029, PGM-030, PGM-031]` 共 43 ID，与开工提示逐项相同；定义区从锁全文读过（GEN L12–L100；BEQR-097 L1160；BEQR-156 L1809；BEQR-163 L1886；BEQR-166 L1919；BSM-009 L2438；BSM-027 L2716；BSM-028 L2730；BSM-029 L2756；BSM-072 L3780；BSM-073 L3795；BSM-074 L3818；BSM-075 L3841；BSM-076 L3855；BSM-077 L3879；BSM-114 L4719；BSM-115 L4799；BSM-120 L4909；PGM-007 L5156；PGM-022 L5412；PGM-026 L5481；PGM-027 L5506；PGM-028 L5527；PGM-029 L5541；PGM-030 L5573；PGM-031 L5594）
- HTML 出处：三份 HTML 取 **main 当前正本**（需求来源不变）。本段映射含 BEQR/BSM/PGM；分类按 GEN-017 对照原文（见各 finding 引用行）。

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger / findings 只读核验（不改；开工提示给出的 V2.89 / V1.73 哈希已过时，以本席实算为准）：

- ledger：版本 **V2.91**，529139 bytes，SHA256 `aa87d852e28968c5a51476d7dc3287b2db7797bf1eec126bab1ae7efb1dc1975`（提示写 V2.89 / 525786 / `f1a0d85e…`；其间 U13-010 候选、U15 定稿、U16 开工写入，属背景漂移，不阻塞本段）
- findings：版本 **V1.74**，682231 bytes，SHA256 `9eccbd45d887075336ded8d857b7d375de2dbc1463aa18fca23ffeb4ab0d1420`（提示写 V1.73 / 679341 / `946facdb…`）
- 台账中与本段直接相关的已定稿项（背景，不继承 PASS）：U04-001 添加已答删除；U14-001…008 / U15-001…008 发布前须修；U16 行 L1321 `IN_PROGRESS`

输入副本核验：

- 本段文件 sha256 `d39efa0f65182c18098a512d91289b82dbba51089f8b28f8b3b76d602fc0831e`（与开工提示、清单条目、`sha256sum` 三方一致）
- 行 2615 / 字节 564772（与清单一致）
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
| **51** | **20** | **`docs/p0-openapi-contract-closure-appendix-v0.1.md`（本段）** |
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

### 2.2 本段路径 blob / 哈希

| 对象 | blob | sha256 | 行 |
|---|---|---|---:|
| 基线 242f4872 | `94714d4ecab1e4fb38c32cefe4b0a964c9d1c13d` | `e480838a45d4b1f26fc171513bf15c64db90ef7469adc2703ddbc961a0857ac2` | 2584 |
| main HEAD 445a49f4 | **同基线** `94714d4e…` | 同左 | 2584 |
| 未发布树 e28dad05 | `49f93c4a67b50165f0d89d051ae2330ec915ebe7` | `d39efa0f65182c18098a512d91289b82dbba51089f8b28f8b3b76d602fc0831e` | 2615 |

`git log 242f4872..HEAD -- docs/p0-openapi-contract-closure-appendix-v0.1.md`：**空**。M009–M012 **未改本路径**。本文件相对 main 的分叉 = 相对基线的 +51/−20，不是「覆盖 main 已修字节」。

### 2.3 本段相对基线的真实改动（完整 diff 已读）

标题 `V0.1` → `V0.03`。§2 计数 720/328/1440/720/434 → **730/329/1460/730/440**。

权限表增量：

- `merchant.profile.manage` 2→3，加入 `setMerchantSelfOperated`
- `public` 5→7，加入 `listStaffRegistrationCompanies, submitStaffSelfRegistration`；数据范围加 `self`
- `staff.payroll.manage` 6→8，加入 `getStaffPenaltyCap, requestStaffPenaltyCap`；范围加 `project`
- `staff.profile.review` 2→3，加入 `setStaffEmploymentStatus`
- **新权限** `staff.profile.self` → `recordStaffLockerScan`
- `staff.shift.manage` 5→8，加入 `getStaffShiftVisibility, setStaffShiftVisibility, listStaffShiftHistory`；范围加 `project`

事件字典增量（节选）：`governance.supervisor_review.evaluated` 成功事件被复用到 `batchUpsertStaffShifts` / `upsertStaffShift` / `requestStaffPenaltyCap`；16 行事件名字面量 `` `none` ``；`merchant.self_operated_*`；`staff.locker.store / staff.locker.retrieve`（斜杠双值）；`staff.self_registered`；删除原 `staff.shift.saved` / `staff.shift.save_failed` / `staff.attendance.punch_failed`。

任务表：10 个新 operationId 全部进 **P0-待归类**（OpenAPI `tags: ["staff"]`，生成器 `taskGroup()` 无 `staff` 分支）；`submitStaffRegistration` 摘要由「员工自助注册」改为「商户代员工提交注册」，终端改为 `web, merchant_app, merchant_portal, staff_work_phone`；`punchStaffAttendance` 失败事件改为 `none`、终端由 `staff_private_phone, web` 改为 `staff_work_phone`。

§9 变更记录仍只有 `2026-06-13 | V0.1` 一行，与标题 V0.03 脱节。

生成器相对基线：加文件版本注释 + 从现文件标题读取 `documentVersion`（不再写死 V0.1）。属 U17 段文件；本段消费其输出。

## 3. 文件结构全覆盖（不能只看差异）

本席按结构读完全文 2615 行，不只读 diff：

| 节 | 行 | 覆盖方式 |
|---|---|---|
| §1 文档定位 | 3–11 | 通读。声明由 `openapi-p0.json` 反向生成；「不是新需求池」；冲突时以 entry / OpenAPI / 矩阵 / **本文**为准 |
| §2 收口结论 | 13–21 | 通读。五格计数与 JSON 实算 730/329/1460/730/440 一致 |
| §3 权限补充表 | 23–357 | 通读全部 329 行。`public`/`authenticated`/`not_applicable` 注释已读 |
| §4 事件字典 | 359–1824 | 结构覆盖：表头+排序规则；逐段抽读 amusement / governance / merchant / `none` 块 / staff 块 / wecom 收尾；行数脚本核 = 1460 = 2×730 |
| §5 任务追溯表 | 1826–2561 | 结构覆盖：AMU 起、AUTH `submitStaffRegistration`、P0 新口 10 条、TIC 收尾；行数脚本核 = 730 |
| §6 示例口径 | 2563–2577 | 通读。request body / response 样例列表与 JSON `x-p0-example-coverage` 同源 |
| §7 验收标准 | 2579–2599 | 通读 AC-CLOSURE-001…005。AC-002 要求能从第 4 节找到事件名并在事件中心查到 |
| §8 边界 | 2601–2609 | 通读 |
| §9 变更记录 | 2611–2615 | 通读。仅 V0.1 |

数据流（本段 → 未发布其它段 → main → HTML）：

```
本附录 md
  ↑ generate-openapi-contract-closure-appendix.mjs（U17；--check 与 JSON 逐字节同步）
  ← docs/openapi/openapi-p0.json（U14） / openapi-p0.yaml（U15，生成器不读 yaml）
  ← 人工三矩阵（不在 76 文件清单；副本工作树有，main 亦有；只影响「覆盖状态」列）
  → 声称约束 Java Controller/DTO/事件中心（U05–U08 副本实现；main 无 10 新口）
  → HTML 正本（main）：BSM L701/L1433/L930/L1445/L1975–1976；PGM L495/L578–579
```

SQL/迁移：本文件无 SQL。新口依赖 U10 `V400–V409`（`is_self_operated` 等），本段只登记不执行迁移。

## 4. 三线审计

### 4.1 第一性原理

**这段东西为什么必须存在？** 三份 HTML 要求权限按入口/主体最小化（BSM-114、PGM-007/028/029）且「所有有过程的业务动作都必须留痕」（BSM-120）。P0 有 700+ operation，人工权限矩阵/事件矩阵/任务映射覆盖不全。本附录是 OpenAPI `x-permission` / `x-audit-event-*` / `x-terminal-allowed` 的完整展开，使 AC-CLOSURE-001…003 可执行。删掉它，HTML 的权限与留痕**不会**自动消失，但本仓会失去「逐 operation 可引用的字典」，`verify:openapi-closure` 红。文件载体 = 细化+技术性。

**声称的行为有没有被实际执行验证过？**

- 「与未发布 JSON 同步」：CMD-UNPUB-CHECK EXIT 0，730 operations。**已验证**。
- 「全部必须进入 Java Controller…」：main 无这 10 个 path（CMD-PARSE NEW paths 10、REMOVED 0）。未发布 Java 在其它段。本文件作为文档**不能**单独证明 Java 已实现。
- 「事件中心必须照常落库」（§8）且 AC-002：12 个失败事件名为字面量 `none`，事件中心无法按名检索。**声称失败**（GR-U16-004）。
- 「不改变已拍板业务口径」（§1）：附录把独立自营写口和公开无设备注册写成正式 operation。**与 HTML / 已答删除冲突**（GR-U16-002/003）。

**没验证就当真的前提：**

1. OpenAPI 里写的事件名都是合法、可落库的标识 → `none` 与带空格斜杠的 locker 名推翻。
2. 生成器输出即业务正确 → 生成器只做投影，不审 HTML。
3. 叠进 main 不会破坏已修项 → 本路径 main=基线，但叠入会把 730 口字典放到 720 口仓库上（CMD-OVL 红），并重新文档化已删添加。

2.3 依据类型本段用到：① HTML 原文；④ 可复现命令。本文件无独立可执行入口，类型 ② 不强制；未引用外部 RFC。

### 4.2 顺从式（按冻结 43 项）

本文件是生成文档，无测试类、无迁移、无 UI 组件。顺从式问：文档声称是否与 JSON/HTML/main 一致、计数是否可复现、有无把计划写成事实。

见 §5 逐项。结论：与未发布 JSON 自洽（GEN-002 局部绿）；与 HTML / 已定稿删除项 / main 720 口 **不一致**。顺从式不能 PASS。

### 4.3 对抗式（含「发布时覆盖/冲突 main 已修项」固定问题）

固定问题：未发布树基于 242f4872，与 main 同名文件已分叉；main 含 M009–M012。

对本路径：**main blob == 基线 blob**，M009–M012 未改此文件。发布时：

1. **只覆盖本 md、JSON/yaml/生成器仍 HEAD**：`verify:openapi-closure` 立即红（CMD-OVL EXIT 1）。字典写 730/setMerchantSelfOperated/`none`，main OpenAPI 仍 720 且无这些 path。
2. **与未发布 JSON+YAML+生成器整包覆盖**：`--check` 可绿，但会把 U04-001 已答删除的自营写口、U14-003 的公开无设备注册、U14-008 的 `none` 失败事件写进当前 main。U08 Java 若同时覆盖还会撞 M009 已删的 `parentEntityId`（U08-002 已定稿）——那是其它段，本段的冲突面是**字典为那些口背书**。
3. **正确叠法**（与 U14-001/U15-001 同根）：以 main 445a49f4 为底，先按 U14-002…008 改定稿 JSON，再 `docs:openapi-closure` **重生**本附录（禁止手改本 md 独走），U14+U15+U16+U17 同一 PR。

其它对抗：

- AC-002 vs 事件名 `none`：测试员按第 4 节去事件中心查 `none`，查不到或命中无关（GR-U16-004）。
- `governance.supervisor_review.evaluated` 作为排班保存的成功事件：审计检索「排班是否保存」会进治理审核事件，四个 operation 共用一名（GR-U16-006）。
- 公开注册 `security: []` + 终端 `app`：按附录实现的客户端会走无设备证明通道（GR-U16-003）。
- 生成器读标题版本、变更记录写死 V0.1：重生后标题可留 V0.03，§9 仍像从未加过 10 口（GR-U16-007）。
- 矩阵脚本只读 JSON（U15 已证）：只叠本附录、不叠 JSON 时矩阵 `--check` 仍绿——本附录不是矩阵输入。对抗面归 U14/U17；本段对应门禁是 `verify:openapi-closure`。

新理论极端（要两三件不巧才发生）**不修、不升 finding**：例如有人手工改附录第 4 节一行但不跑 `--check`。门禁首次正常跑就会抓住，已有闸。

## 5. 适用冻结判据逐项

| ID | 适用证据 | 本段结论 |
|---|---|---|
| GEN-001 | 附录载体有用途；`setMerchantSelfOperated` 无独立于六类合作方式的 HTML 上位 | 载体过；GR-U16-002 添加须删 |
| GEN-002 | `--check` 与未发布 JSON 绿；与 main JSON 红；「全部进 Java」对 10 新口在 main 为假 | 局部绿；叠法 FAIL（GR-U16-001） |
| GEN-003 | 本文件无测试。门禁 `--check` 是生成器测试，断言为逐字节相等，能失败（CMD-OVL 已红） | 无本文件测试面；门禁有效。N/A 测试类 |
| GEN-004 | 公开自助注册无设备公钥/指纹，`security: []` | FAIL GR-U16-003 |
| GEN-005 | 同上；`public` 权限覆盖提交 PII 的写口 | FAIL GR-U16-003 |
| GEN-006 | 文档无事务。自营布尔与六类码可分叉（类型探针已在 U04 定稿） | 由 GR-U16-002 承担 |
| GEN-007 | 附录无幂等列。U14-006 两写口 `idempotency_required: false` 不出现在本表 | 本文件无该列；观察 OBS-4 |
| GEN-008 | 附录↔JSON 未发布树一致；↔main JSON 不一致；事件名/权限/终端与 HTML/已定稿项不一致 | FAIL 001/002/003/005/006 |
| GEN-009 | 8 个写口失败事件=`none`，失败不可定位 | FAIL GR-U16-004 |
| GEN-010 | 公开通道提交姓名/手机/证件；无设备绑定 | FAIL GR-U16-003 |
| GEN-011 | `none`、双值 locker 名、跨域复用 `supervisor_review.evaluated`；AC-002 无法兑现 | FAIL 004/005/006 |
| GEN-012 | 纯文档，无循环/连接 | N/A（无实现面） |
| GEN-013 | `npm run verify:openapi-closure` 可复现；只叠本文件失败显式 | 叠法须入队（GR-U16-001） |
| GEN-014 | 无迁移文本 | N/A（无实现面） |
| GEN-015 | 非 UI。§5 约 60 条英文摘要为 OpenAPI 既有投影，本段 10 新口摘要为中文 | 观察 OBS-5；不升 finding |
| GEN-016 | 计数与 JSON 一致；§9 版本不可复现 V0.03 增量；「不改变已拍板口径」为假 | FAIL GR-U16-007；001 |
| GEN-017 | 分类见 findings；未把 HTML 沉默当冲突；未把添加伪装技术性 | 过程遵守；内容有添加/冲突 |
| GEN-018 | 结论落到 HTML 原句与命令输出 | 本报告遵守 |
| BEQR-097 | 附录列 `inventory.supply_contract.*`，无本段增量 | 既有细化；无新冲突 |
| BEQR-156 | 附录列合同状态相关口，无本段增量 | 既有细化；无新冲突 |
| BEQR-163 | 附录 `raiseContractChange` 摘要仍写「6 类」（HTML 八类）。此句在基线已有，diff 未触及 | 观察 OBS-6（main 既有，归 M389 正审）；本材料未加重 |
| BEQR-166 | 附录列审批流口，无本段增量 | 既有细化 |
| BSM-009 | 基础管理含商户/合同/员工。10 新口中员工/商户部分见 002/003 | 由 002/003 承担 |
| BSM-027 | 同基础管理盘子 | 无独立新冲突 |
| BSM-028 | 六类合作方式原文 L701；员工注册审批 L709 | GR-U16-002 添加相对六类码 |
| BSM-029 | 权限要知道注册、设备绑定 | GR-U16-003 缺设备绑定 |
| BSM-072–074 | 商户授权范围；自营写口不在商户侧专区五项 | 002 为平台写口添加，不是商户专区细化 |
| BSM-075–077 | 招商租赁合同链。附录合同口无本段增量 | 既有细化 |
| BSM-114 | 入口表：私人手机含注册；管理后台做配置。公开 `app` 注册与十一端分列冲突 | GR-U16-003 |
| BSM-115 | 基础管理连员工设备 | 003 切断设备绑定 |
| BSM-120 | L1975 私人手机绑定设备标识；L1976 寄存柜扫码留痕 | 003 冲突；locker 口为细化，双值事件为 005 |
| PGM-007 | 合同决定后续能不能走。附录合同口无增量 | 既有细化 |
| PGM-022 | 空间点位。附录 floor_plan 口无增量 | 既有细化 |
| PGM-026 | 六类合作方式 L495；自营标注 L578 落在「视同公司自有」不是第二开关 | GR-U16-002 |
| PGM-027 | 商户侧授权范围内全开放；不看其它商户。本段无商户越权新口 | 无新冲突 |
| PGM-028–030 | 合同准入卡口。附录合同口无增量 | 既有细化 |
| PGM-031 | 经销商门户。附录 partner/经销相关口无本段增量 | 既有细化 |

无实现面的判据（GEN-003 测试类、GEN-012、GEN-014）已标 N/A 及原因。

## 6. Findings（临时 ID；正式 ID 仅 B2 分配）

### GR-U16-001 — 只叠本附录到当前 main，`verify:openapi-closure` 红（发布叠法）

- **完整原文（本文件）：** L13–21 声称 operation 730、权限 329、事件 1460、写口 440；L5「本文档由 `docs/openapi/openapi-p0.json` 反向生成」。
- **文件:行号：** `docs/p0-openapi-contract-closure-appendix-v0.1.md:1–21`（未发布副本）；对照 main 同路径 2584 行、720 口。
- **业务影响：** 发布者若整文件覆盖，文档字典与 main 契约门禁立即互斥；若再整包覆盖未发布 OpenAPI，则把 10 个未在 main 存在的写/读口（含已答删除的自营口）变成「以本附录为准」的执行口径（L11）。
- **可复现：** CMD-OVL：main JSON + 本附录 + 未发布生成器 `--check` → EXIT 1，`… is out of sync with …/openapi-p0.json; run npm run docs:openapi-closure`。CMD-UNPUB-CHECK EXIT 0；CMD-MAIN-CHECK EXIT 0。
- **冻结判据：** GEN-002、GEN-008、GEN-013、GEN-016
- **2.3 依据类型：** ④ 可复现命令
- **四分类：** **技术性**（无业务发明：是生成物与门禁的叠法。业务发明在 002/003。）
- **无业务发明理由：** 叠法/门禁同步是构建与发布约束，不引入实体/枚举/流程。
- **工程缺陷 / 建议登记：** 是 / 是（发布前须修）
- **最小处置（给发布者，本轮不修）：** 同根 U14-001 / U15-001 / U13-001。以 main 445a49f4 为底，U14 JSON 按 002–008 改定稿后 **从 JSON 运行 `docs:openapi-closure` 重生本附录**，禁止手改本 md 独走；与 U15 yaml、U17 矩阵同一 PR。禁止用未发布树整文件覆盖 main。
- **同根：** U14-001、U15-001。不新开 2.2。

### GR-U16-002 — 附录将独立自营布尔写口收为正式 operation（添加，已答删除）

- **完整原文：**
  - L273：`| \`merchant.profile.manage\` | staff-token | project, project-merchant | 3 | createMerchant, updateMerchant, setMerchantSelfOperated | 已在权限矩阵正文出现 |`
  - L1382–1383：`merchant.self_operated_change_failed` / `merchant.self_operated_changed` → `setMerchantSelfOperated` `POST /api/v1/platform/merchants/{merchant_id}/self-operated`
  - L2437：`| P0-待归类 | \`setMerchantSelfOperated\` | \`POST\` | \`/api/v1/platform/merchants/{merchant_id}/self-operated\` | 标注商户是否自营 | \`merchant.profile.manage\` | \`merchant.self_operated_changed\` / \`merchant.self_operated_change_failed\` | web, staff_work_phone, staff_private_phone | operation 级补充 |`
- **HTML 原文（main）：** BSM L701「区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式。」PGM L495 同旨。BSM L1433 / PGM L578「在商户档案上标注为自营的商户视同公司自有」——标注落在六类码，不是第二开关。
- **业务影响：** 字典让实现者认为存在独立于 `cooperation_form` 的 `self_operated` 布尔写口；与合同/结算用的六类码可分叉。操作员已答删除。
- **可复现：** CMD-PARSE NEW ids 含 `setMerchantSelfOperated`；main 720 口无此 id。`questions/U04-001.md` 答复首行「删除」。
- **冻结判据：** GEN-001、GEN-008、GEN-017、BSM-028、PGM-026
- **2.3 依据类型：** ① 三份 HTML 逐字原文
- **四分类：** **添加**
- **工程缺陷 / 建议登记：** 是 / 是
- **最小处置：** 随 U14-002 从定稿 JSON 删除该 path 与一切 `self_operated` 布尔字段后重生本附录；员工分流读六类码（U05-001）。**不新开 2.2、不再问。**
- **同根：** U04-001 / U08-001 / U10-002 / U14-002 / U15-002。

### GR-U16-003 — 公开自助注册（及公司列表）无设备证明、终端含 `app`、失败事件 `none`

- **完整原文：**
  - L289：`| \`public\` | public | customer, none, public-project, self | 7 | login, customerPhoneLogin, customerLogout, listCustomerTicketProducts, getSystemHealth, listStaffRegistrationCompanies, submitStaffSelfRegistration | 已在权限矩阵正文出现 |`
  - L2400：`listStaffRegistrationCompanies` `GET /api/v1/staff/registration-companies` 权限 `public` 事件 `none` / `none` 终端 `app, staff_private_phone`
  - L2477：`submitStaffSelfRegistration` `POST /api/v1/staff/self-registrations` 权限 `public` 事件 `staff.self_registered` / `none` 终端 `app, staff_private_phone`
- **未发布 JSON（数据流上游，只读）：** 该 POST `security: []`，`x-auth-context: public`，request schema required 仅 `idempotency_key, company_id, name`（手机/证件可选，**无** `device_public_key` / `device_fingerprint_hash`）。
- **HTML 原文（main）：** PGM L578「公司自有员工由本人在**私人手机 App**注册」；PGM L579 / BSM L935 / BSM-120 L1975「安装时生成并绑定一个该手机专属的设备标识」。BSM-114 L1865 私人手机做注册，不是客人端 `app`。
- **业务影响：** 按附录编码的客户端可在无设备绑定、无鉴权下提交姓名（及可选明文手机/证件）。与「私人手机绑定设备标识」冲突。`app` 终端把员工注册标成客人应用通道。
- **可复现：** CMD-PARSE public 列表 7 条；main public 5 条且无这两口。
- **冻结判据：** GEN-004、GEN-005、GEN-010、BSM-029、BSM-114、BSM-120
- **2.3 依据类型：** ① HTML 逐字原文
- **四分类：** **冲突**
- **工程缺陷 / 建议登记：** 是 / 是
- **最小处置：** 同根 U14-003 / U05-003 / U01-001：请求增加设备公钥/指纹必填（无指纹 4xx）；`x-terminal-allowed` 去掉 `app` 只留私人手机码；public 保留但加频控（U07-004）；公司列表匿名发现须另证来源校验。重生附录。不新开 2.2。

### GR-U16-004 — 事件字典收纳字面量 `none`（12 失败 / 4 成功），写口失败不可审计

- **完整原文：** L1390–1405 连续 16 行事件名 `` `none` ``（失败 12：含 `upsertStaffShift`、`batchUpsertStaffShifts`、`punchStaffAttendance`、`submitStaffSelfRegistration`、`setStaffEmploymentStatus`、`setStaffShiftVisibility`、`requestStaffPenaltyCap`、`recordStaffLockerScan` 八个写口 + 四读口失败；成功 4：两设置 GET、公司列表、排班历史）。§5 对应行把成功/失败写成 `` `none` / `none` `` 或 `` `…` / `none` ``。L2585–2587 AC-CLOSURE-002：「必须能从本文第 4 节找到对应成功/失败事件名，并在事件中心查到操作者、终端、设备、业务对象、结果、失败原因和 trace_id。」L361「实现时不得只更新业务表而不写事件。」
- **对比 main 附录 L2388：** `punchStaffAttendance` 原为 `staff.attendance.punched` / `staff.attendance.punch_failed`。未发布删失败名、改 `none`。main 720 口无任何 `none`。
- **业务影响：** 排班保存、打卡、自助注册、任职状态、处罚上限、柜机扫码失败时，按本字典**没有**可检索失败事件。AC-002 自相矛盾。BSM-120「所有有过程的业务动作都必须留痕」在失败路径落空。
- **可复现：** `rg -n '\`none\`' <本文件>` 32 处；CMD-PARSE `fail==none` 12、`succ==none` 4、write fail==none 8。CMD-UNPUB-CHECK 仍绿（生成器把 OpenAPI 字面量原样抄入，**不拒绝** `none`）。
- **冻结判据：** GEN-009、GEN-011、BSM-120
- **2.3 依据类型：** ④ 可复现命令 + ① HTML（BSM-120 留痕）
- **四分类：** **技术性**（无新业务概念；是把契约里的空事件名投影进字典。不是「发明不留痕业务」。）
- **无业务发明理由：** 事件名缺失是实现/契约缺陷，HTML 已要求留痕；补 `*_failed` 不引入新实体。
- **工程缺陷 / 建议登记：** 是 / 是
- **最小处置：** 同根 U14-008：写口恢复/新给 `*_failed` 名（打卡至少恢复 `staff.attendance.punch_failed`）并在实现 `recordFailure`；读口若确无业务事件用明确 `*.viewed`/`*.view_failed`，**禁止字面量 none**。JSON 改定稿后重生本附录。生成器可加 `--check` 拒绝 `none`（U17，非本段必改）。
- **同根：** U14-008 / U15-008。

### GR-U16-005 — 柜机成功事件名为带空格斜杠的双值字面量

- **完整原文：**
  - L1596：`| \`staff.locker.store / staff.locker.retrieve\` | success | \`recordStaffLockerScan\` | \`POST\` | \`/api/v1/staff/locker-events\` |`
  - L2420：成功/失败列 `` `staff.locker.store / staff.locker.retrieve` / `none` ``
- **未发布 JSON：** `x-audit-event-success` 正是该 35 字符字符串；body `action` enum `store|retrieve`。
- **HTML：** BSM-120 L1976 / PGM L582 寄存柜扫码由柜子自动留痕——需要**可落库的单一事件名**（或按 action 分两个稳定名），不是「一个字段里用斜杠写两个」。
- **业务影响：** 事件中心按精确事件名索引时无法命中；实现若把整串当 event type 会与 store/retrieve 两动作不对齐。同根 U14-005 单端契约+单一成功名。
- **可复现：** CMD-PARSE slash in event names 仅此一条。
- **冻结判据：** GEN-008、GEN-011、BSM-120
- **2.3 依据类型：** ④ 命令 + ① HTML
- **四分类：** **技术性**
- **无业务发明理由：** 留痕动作 HTML 已有；缺陷是事件名编码。
- **工程缺陷 / 建议登记：** 是 / 是
- **最小处置：** 同根 U14-005：单一成功事件名（或按 `action` 二选一的稳定名），失败名随 004。重生附录。

### GR-U16-006 — 排班/处罚写口成功事件盗用 `governance.supervisor_review.evaluated`

- **完整原文：**
  - L1129：`` `governance.supervisor_review.evaluated` | success | `batchUpsertStaffShifts` | `POST` | `/api/v1/staff/shifts/batch` ``
  - L1131：同上，`requestStaffPenaltyCap`
  - L1132：同上，`upsertStaffShift`
  - L1130：真正的 `evaluateSupervisorReview` 也用同一成功名
  - L2305 / L2423 / L2487：§5 成功/失败列 `` `governance.supervisor_review.evaluated` / `none` ``
- **基线/main 附录：** `upsertStaffShift` / `batchUpsertStaffShifts` 原为 `staff.shift.saved` / `staff.shift.save_failed`。未发布**删掉**这两对，改成治理审核事件 + `none`。
- **业务影响：** 「主管排班是否保存」的审计检索会命中治理审核事件，与 `evaluateSupervisorReview` 无法区分。失败路径见 004。即便实现上走 `SupervisorReviewService.evaluate()`，**operation 级**成功事件仍应是本口自己的名字（或明确的 `staff.shift.saved`），不能让字典把四个口收成同一事件。
- **可复现：** CMD-PARSE SAME ID EVENT DRIFT 对 `upsertStaffShift`/`batchUpsertStaffShifts` 的 succ 字段。
- **冻结判据：** GEN-008、GEN-011
- **2.3 依据类型：** ④ 命令
- **四分类：** **技术性**
- **无业务发明理由：** 排班保存与主管审核 HTML 都有；缺陷是事件名串台，不是新流程。
- **工程缺陷 / 建议登记：** 是 / 是
- **最小处置：** B2 可并入/扩展 U14-008：恢复 `staff.shift.saved`/`staff.shift.save_failed`（及处罚口自己的 `*.requested`/`*.failed`）；`governance.supervisor_review.evaluated` 只留给 `evaluateSupervisorReview`。不要只补失败名而留下盗用的成功名。重生附录。
- **与 U14-008 关系：** 同批契约字段，但 U14-008 定稿文本只写失败=`none`。本条补成功侧串台，建议 B2 扩展同一正式 ID 或单列。

### GR-U16-007 — 标题 V0.03 与 §9 变更记录仅 V0.1，增量不可从变更记录复现

- **完整原文：** L1 `# P0 OpenAPI 权限、事件、任务追溯收口附录 V0.03`；L2611–2615 变更记录只有 `| 2026-06-13 | V0.1 | 新增由 OpenAPI 反向生成的… |`
- **生成器（U17，只读）：** 未发布稿从现文件标题 regex 取版本写入 L1；§9 表格在模板里写死 V0.1，重生不追加。main 生成器则标题也写死 V0.1（故 CMD-MAIN-GEN-UNPUB-JSON EXIT 1）。
- **业务影响：** 读者只看 §9 会以为仍是 720 口初版；730 口、10 新口、事件 `none` 在变更记录中不存在。GEN-016：版本与完成声明须可复现。
- **可复现：** 读 L1 vs L2613；CMD-MAIN-GEN-UNPUB-JSON EXIT 1。
- **冻结判据：** GEN-016
- **2.3 依据类型：** ④ 命令（读文件+生成器 `--check`）
- **四分类：** **技术性**
- **无业务发明理由：** 版本账是文档元数据。
- **工程缺陷 / 建议登记：** 是 / 是
- **最小处置：** 重生本附录时改 §9（或删无法维护的变更表，只留标题版本）。U17 生成器不要把版本与变更记录做成一半动态一半写死。随 001 同一 PR。

## 7. 添加项清单（供主调度按 2.2；B1 不代答）

| 临时 ID | 添加了什么 | 2.2 状态 |
|---|---|---|
| GR-U16-002 | 附录收纳独立自营布尔写口 `setMerchantSelfOperated` | **已答删除**（`questions/U04-001.md`）。不新开弹窗。 |

无其它新添加。公开自助注册本身是 HTML 两条路径的细化；缺陷是缺设备证明/错终端，已按**冲突**入 GR-U16-003，不是添加。

## 8. 观察（无新增根因，完整登记不漏并）

1. **本路径 main=基线。** 对抗式「覆盖 M009–M012 已修字节」对本文件不成立；成立的是字典为未进 main 的口背书（§4.3）。
2. **未发布 `--check` 绿 ≠ 业务过。** 生成器是投影器，不读 HTML、不拒绝 `none`。
3. **`submitStaffRegistration` 摘要改为「商户代员工提交注册」** 对齐 BSM L1433 两条路径，属细化，不是缺陷。
4. **附录无幂等列。** U14-006 的 `setStaffEmploymentStatus`/`setStaffShiftVisibility` `idempotency_required: false` 不出现在本 md 正文。处置仍在 JSON/U05，重生附录不会自动显示该字段，除非改生成器。
5. **§5 约 60 条英文摘要**（如 L1834 `Approve P2 amusement group booking…`）是 OpenAPI `summary` 既有投影，10 个新口摘要为中文。归 M389 正审 / OpenAPI 源，本材料未加重。
6. **`raiseContractChange` 摘要「发起合同变更(6 类)」** 与 BEQR-163 / BSM-028 八类不一致。基线已有、diff 未触及 → main 既有，归 M389；本材料未加重。
7. **生成器 `taskGroup()` 无 `staff`。** 新口 `tags: ["staff"]` 全部进 P0-待归类（200 行既有 P0 + 本段 10 新口）；`submitStaffRegistration` 因 `tags: ["access"]` 在 AUTH。发现性差，根因在 U17/OpenAPI tags。建议 U17 加 `staff → AUTH-账号权限` 或把自助注册 tag 改为 access。不单独立项（首轮不因此单独红门禁）。
8. **打卡终端改为仅 `staff_work_phone`。** 基线附录为 `staff_private_phone, web`。BSM L1445 专章「考勤打卡从领取工作手机开始算」（NFC/扫码/离充电桩）；BSM L930/L1865 入口表仍把「考勤打卡」写在私人手机可离场事项。本席按更具体的 L1445 判未发布改为工作手机是**细化**，不升冲突。HTML 两处张力上报调度按人闸处理，本席不改 HTML。
9. **GET 可见天数 description「未配置的上下限为空」** 在 JSON（U14-004），**本附录只抄 summary「读取排班可见天数」**，该冲突句不在本文件。重生后若 JSON 已改，摘要仍短句无 description。
10. **locker `x-auth-context: staff-token` + 终端 `kiosk`。** 身份模型缺陷在 U14-005/U05-006（柜机设备令牌 vs 员工 token）。本附录只投影该对。不重复开添加。
11. **`setMerchantSelfOperated` tags=`staff`** 却是平台商户写口，进 P0 而非 GOV。随 002 删除消失。
12. **人工三矩阵不在 76 文件清单**；`--check` 的「覆盖状态」列依赖工作树里的矩阵。副本与 main 矩阵差异只改「已在矩阵出现 / 以本附录为准」文案，不改 operation 集合。
13. **wecom 留存策略行（L2488）** 已内嵌「已作废…永久保存」——基线已有，diff 未触及。
14. **§1 L11 以本文为准压过历史 PRD。** 基线已有。未发布增量借此把 10 新口抬成执行口径；处置是改 JSON 后重生，而不是删这句（这句对「人工矩阵过时、以 OpenAPI 为准」仍有技术用途）。
15. **U14-007 schema/example 互斥** 不在本 md（无 schema 表）。
16. **HTML 自身：** L930 vs L1445 考勤入口张力（观察 8）。不上报为「HTML 错误必须改」，只标记给人闸。三份 HTML 未改。
17. **ledger/findings 哈希相对开工提示已前进**（V2.91 / V1.74），因 U13-010 与 U15 定稿。锁哈希未变。
18. **commit `03acdb98` 本仓无对象**；审查输入是树 `e28dad05` + 副本目录。不得把副本称为该 commit 在本仓可 checkout。
19. **英语混入权限代表 operationId**（L87 `...` 截断）为生成器 `slice(0,12)` 技术截断，基线已有。
20. **本段无 SQL、无测试文件、无对 `complex_test*` 的访问。** 未声称任何后端测试通过。

## 9. 命令证据

隔离根：`/tmp/u16-b1-grok`、`/tmp/u16-b1-grok-mainchk`、`/tmp/u16-b1-grok-ovl`、`/tmp/u16-b1-grok-ovl2`。不改副本/main/HTML/锁/台账。

| ID | 命令 | 退出码 | 关键输出 |
|---|---|---|---|
| CMD-LOCK | `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | 0 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| CMD-HEAD | `git rev-parse HEAD origin/main` | 0 | 均为 `445a49f409f0cfbe6dd616ae079be1299824c48f` |
| CMD-TREE | `git cat-file -t e28dad05…` | 0 | `tree` |
| CMD-COMMIT | `git cat-file -t 03acdb98…` | 128 | `could not get object info` |
| CMD-BLOB | `git rev-parse {基线,HEAD,e28dad05}:docs/p0-openapi-contract-closure-appendix-v0.1.md` | 0 | 基线=HEAD `94714d4e…`；未发布 `49f93c4a…` |
| CMD-DIFF-FILE | `git diff --numstat 242f4872 e28dad05 -- docs/p0-openapi-contract-closure-appendix-v0.1.md` | 0 | `51 20` |
| CMD-76 | `git diff --numstat 242f4872 e28dad05` | 0 | 76 files, +4745/−1084 |
| CMD-LOG | `git log --oneline 242f4872..HEAD -- <本路径>` | 0 | 空 |
| CMD-HASH-FILE | `sha256sum <副本本路径>` | 0 | `d39efa0f…fc0831e` |
| CMD-HASH-76 | Python 对清单 76 路径 sha256 | 0 | ok 76 mismatch 0 |
| CMD-UNPUB-CHECK | `/tmp/u16-b1-grok` 未发布 json+附录+未发布生成器 `--check` | **0** | `in sync … 730 operations checked` |
| CMD-MAIN-CHECK | `/tmp/u16-b1-grok-mainchk` main json+附录+main 生成器 `--check` | **0** | `in sync … 720 operations checked` |
| CMD-OVL | main json + **本附录** + 未发布生成器 `--check` | **1** | `out of sync … run npm run docs:openapi-closure` |
| CMD-MAIN-GEN-UNPUB-JSON | 未发布 json+附录 + **main** 生成器 `--check` | **1** | `out of sync`（版本行 V0.1 vs V0.03） |
| CMD-PARSE | Python 遍历未发布/main OpenAPI paths | 0 | 730 vs 720；NEW 10 ids；fail==none 12；public 7；perms 329 vs 328；附录表 329/1460/730 行 |
| CMD-GEN-DIFF | `git diff 242f4872 e28dad05 -- tools/generate-openapi-contract-closure-appendix.mjs` | 0 | +版本注释；标题改为读现文件 V 号 |

未运行：Maven/Vitest/Playwright、任何写库脚本、`verify:delivery-gates` 全链（会碰共享库/部署）。不把未运行测试写通过。

## 10. 结论

**FAIL。**

发布前须修 7 条（001 叠法、002 添加已答删除、003 公开注册冲突、004 `none` 事件、005 locker 双值名、006 成功事件串台、007 变更记录）。添加项 1 条已答删除，不再问。无有条件通过。无未闭合待问却 PASS。

B2 归并建议：001↔U14-001；002↔U14-002；003↔U14-003；004↔U14-008；005↔U14-005；006 扩展 U14-008 或单列；007 随重生/U17。共同处置：U14+U15+U16+U17 同一 PR，先改定稿 JSON 再生成 yaml/附录/矩阵，以 main 为底，禁止整树覆盖。

## U16 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 50713 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 16 段：接口覆盖说明」（1 文件 2615 行 NORMAL，main 主归属 M389；适用判据＝映射行并集 43 项）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论；原文与哈希本席本节点独立实算。
- 本席 B2-r1：**PASS**（001–008 逐 ID 全 PASS；添加 1 已答删除、不发新 2.2；发布前须修「是」=001–007，「否」=008）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401；M389 映射行 L6768 共 43 ID＝BEQR-097/156/163/166、BSM-009/027/028/029/072–077/114/115/120、GEN-001–GEN-018、PGM-007/022/026–031，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、本段 blob 与树 `e28dad05` 全等、main/基线 blob `94714d4e…`、未发布附录 SHA `d39efa0f…fc0831e`、未发布 json 三口 `x-audit-event-success`、未发布/main 生成器版本读取面、HTML BSM L701/L930/L935/L1443/L1445/L1865/L1975–1976、PGM L578/L579/L582、`questions/U04-001.md` 答复首行「删除」、已定稿 U14-001…008 / U15-001…008 / U04-001 / U05-003/006 / U01-001 / U13-001、package.json 既有 `docs:openapi-closure` / `verify:openapi-closure`）；未重跑 overlay/生成器、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u16-b2-append.md` 中转，落盘后删除。本票不把任何 U-FIX worktree 当作本段已发布修复。

此票只判断 findings.md V1.76 顶部「当前 U16 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U16-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 50713 | `61a7d7edb9a25d84c2b28ec92c13bbaf21ee5ac277e54060dbc375913dc6386e` |
| `docs/audit-2026-09/modules/U16-b1-self-raw.md` | V0.01 | 11699 | `1998abe2a5646e728942a4bbd23a54b2977524f5a7015fdd7d854e49e1a45646` |
| `docs/audit-2026-09/findings.md` | V1.76 | 691312 | `6cc7c9af07488be646e7af866a98fd8e5067dbca3b0c4a01313c541af4c37fce` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.93 | 531746 | `148203bb435033a7476c20d4af433b890bf72ba1afa382fa0dfe6e9d305b03cc` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`；答复首行精确「删除」 |

候选形态：V1.76 顶部 U16-001…008；questions 0（引用 U04-001）；添加 1 已答删除；无 HTML 修改申请。U16 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa`、PGM `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`。副本本段 1/1 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算：副本 SHA `d39efa0f65182c18098a512d91289b82dbba51089f8b28f8b3b76d602fc0831e`，`git hash-object` = `49f93c4a67b50165f0d89d051ae2330ec915ebe7` = `e28dad05:docs/p0-openapi-contract-closure-appendix-v0.1.md`）。基线/main blob 同为 `94714d4ecab1e4fb38c32cefe4b0a964c9d1c13d`。`git diff --numstat 242f4872 e28dad05 -- docs/p0-openapi-contract-closure-appendix-v0.1.md` = `51	20`。全树 `git diff --shortstat 242f4872 e28dad05` = 76 files, +4745/−1084。

### 技能（V0.17 §6 收窄，本节点）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核路由；用户指定 B2 归并覆盖 Arc audit；未改走 audit 工作流 |
| receiving-code-review | 只取其「先核原文再接受或反驳、禁止表演性同意、B2 不修复」；本体若派同会话子代理则成第四席，**未调用** |
| grilling | 只借追问结构自查：(a) 006 单列不并入 U14-008 与事件名是否发明业务、(b) 007 交第 17 段生成器是否与 GR-007 一致、(c) 003/002/001/004/005 四分类、(d) 008 是否写错 §8.7/8/14 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/hash-object/附录行号/未发布 json 字段/生成器读取面/HTML 锚点/package.json 脚本名；本票不声称本轮新跑 overlay 或 `--check` |
| systematic-debugging / diagnosing-bugs | 只借读错→对照数据流（本附录 md → U14 JSON / U15 YAML / U17 生成器 → main 720 → HTML）；本轮不建 `/tmp` overlay |
| ponytail-review | 只评最小删除/收窄，不落地。001 只从定稿 json 用既有 `docs:openapi-closure` 重生；002–006 不在本 md 上另做一遍业务修补 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 三口成功事件盗用 `governance.supervisor_review.evaluated` 是否必须单列、能否并进已定稿 U14-008？单列成立，见 (a)。
2. `staff.shift.submitted` / `staff.payroll.penalty_cap_requested` 是否发明「待审」新流程？否，只是 operation 级事件标识符，见 (a)。
3. 标题 V0.03 与 §9 仅 V0.1 是否应交第 17 段生成器、与 GR-007 是否同向？是，见 (b)。
4. 003 冲突 / 002 添加已答删除 / 001·004·005 技术性是否与 GR 四分类一致？是，见 (c)。
5. 008 是否写错 §8 第 7/8/14 条或把应升队项降观察？否，见 (d)。

### 完整差集（GR-001…007、§8 观察 1–20、席甲 CX-001、OBS-1…2）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–008。未把本席 finding 降观察。席甲独有项无超出 CX-U16-001 与 OBS-1…2。席甲 B1 以一条「派生物随第 14 段重生」覆盖，漏判 002/003 分项、006 成功事件串台、007 版本账；候选已拆分并接受本席七项须修，本席独立复核后维持。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U16-001 | U16-001 | 否：技术性叠法；只叠本附录 `--check` 红；以 main 为底，U14 json 定稿后 `docs:openapi-closure` 重生；禁手改 md 独走；禁整树 |
| GR-U16-002 | U16-002 | 否：添加；已答删除；随 U14-002 删 path 后重生；不新开 2.2 |
| GR-U16-003 | U16-003 | 否：冲突；公开注册无设备证明 + 终端含 app + 失败 none；同根 U14-003 |
| GR-U16-004 | U16-004 | 否：技术性；事件字典 16 行字面量 `none`；同根 U14-008 |
| GR-U16-005 | U16-005 | 否：技术性；locker 成功事件双值字面量；同根 U14-005 |
| GR-U16-006 | U16-006 | 否：技术性；成功侧串台单列（U14-008 只覆盖失败 none）；operation 级成功名 |
| GR-U16-007 | U16-007 | 否：技术性；标题 V0.03 vs §9 仅 V0.1；交第 17 段生成器后重生 |
| §8 1–20 | U16-008 | 「含」为闭集转述；第 7/8/14 条未把观察升冲突，也未把 finding 降观察（见 (d)；第 14 条括注压缩，非阻断） |
| 席甲 CX-U16-001 叠法/重生句 | U16-001 | 子集（「随第 14 段契约修正后重生」）；并入 GR-001，补上席甲未单列的叠法红 |
| 席甲 CX-U16-001 自营写口行 | U16-002 | 席甲漏判为独立添加项；候选已拆出，与 GR-002 同 |
| 席甲 CX-U16-001 public/`none` 行 | U16-003 / U16-004 | 席甲捆在一条重生里；候选按 GR 拆冲突（注册面）与技术性（字典 none） |
| 席甲 OBS-2 locker 双值 | U16-005 | 席甲作观察；本席 B1 已升技术性；候选并入 005，不留观察 |
| 席甲 OBS-1 P0-待归类 | U16-008 | 与 GR §8.7 同向，维持登记 |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U16-001 | GR-U16-001 + 席甲 CX-U16-001 | 技术性（发布叠法/生成物，同根 U14-001/U15-001/U13-001）/是/是 | **PASS（技术性叠法/是/是；只引用既有 `docs:openapi-closure`）** |
| U16-002 | Grok GR-U16-002 | 添加（操作员已答删除）/是/是 | **PASS（添加已答删除/是/是；不新开 2.2）** |
| U16-003 | Grok GR-U16-003 | 冲突（同根 U14-003/U15-003/U05-003/U01-001/U13-004）/是/是 | **PASS（冲突/是/是；与 GR-003 四分类一致）** |
| U16-004 | GR-U16-004 + 席甲 CX-U16-001（none 行） | 技术性（同根 U14-008/U15-008）/是/是 | **PASS（技术性/是/是）** |
| U16-005 | GR-U16-005 + 席甲 OBS-2 | 技术性（同根 U14-005/U15-005/U05-006）/是/是 | **PASS（技术性/是/是）** |
| U16-006 | Grok GR-U16-006（席甲漏；U14-008 未覆盖成功侧） | 技术性/是/是 | **PASS（技术性单列/是/是；事件名无业务发明；不并入 U14-008）** |
| U16-007 | Grok GR-U16-007 | 技术性/是/是 | **PASS（技术性/是/是；交第 17 段生成器与 GR-007 一致）** |
| U16-008 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；第 7/8/14 条未写错原意）** |

技术性入队 5（001 叠法、004 none 事件、005 双值名、006 成功事件串台、007 版本账）。添加入队 1（002，已答删除）。冲突入队 1（003 公开注册无设备证明）。登记观察 1（008）。questions 0。无 HTML 修改申请。

发布前须修「是」=001–007（7）；「否」=008（1）。与候选汇总一致。席甲 B1 票 FAIL 但只立一条重生且漏 002/003 分项、006、007，属低估；候选接受本席七项须修并勘误，本席独立复核后维持。

### 重点 (a) U16-006 独立技术性项、不并入 U14-008 — **PASS（成立；`staff.shift.submitted` / `staff.payroll.penalty_cap_requested` 只是事件命名，无业务发明）**

独立核，不采信席甲「U14-008 未覆盖成功侧」九个字为己证。对照本席 GR-U16-006 原文、已定稿 U14-008 文本、U14-009 观察 14、附录 L1129–1132、未发布 json 三口字段、HTML BSM L1443/L1458。

**1. 单列、不并入 U14-008 成立。**

U14-008 定稿最小处置只写：8 个写口恢复/新给 `*_failed` 并 `recordFailure`，不得用字面量 none。成功事件名不在该正式 ID 的处置闭集里。U14 已 REVIEWED-FAIL；扩写 U14-008 等于重开已闭环段。本席 B1 原文已给两条路：「B2 可并入/扩展 U14-008 … 或单列」。候选走单列，与 GR 的「或」相容，且是材料未发布、U14 已定稿之后的最懒可行路。

U14-009 观察 14（本席 U14 B1 §8.14）写的是：排班改走主管审批链、成功审计改到 `governance.supervisor_review.evaluated` **与 U09 同向，属细化**；U14 段缺陷只记 failure=none。那是 JSON 段对「走审批链」这一流向的观察，**不是**批准四个 operation 共用治理评估事件名。本附录是事件字典（§4 按事件名排序，L1129–1132 四行同名相连）；AC-CLOSURE-002 要求按第 4 节事件名到事件中心检索。检索「排班是否提交/保存」会命中 `evaluateSupervisorReview` 的治理事件——这是本段 GEN-008/GEN-011 的字典串台，不是 U09 审批链本身。006 不推翻「排班发布须上一级审批」（HTML L1443 已有），只要求 operation 级成功名与 `evaluateSupervisorReview` 分开。

本轮抽出（不采信席甲引文）：

- 附录 L1129 `batchUpsertStaffShifts`、L1131 `requestStaffPenaltyCap`、L1132 `upsertStaffShift` 成功事件均为 `governance.supervisor_review.evaluated`；L1130 才是真正的 `evaluateSupervisorReview`。
- §5 L2305 / L2423 / L2487 成功/失败列为 `` `governance.supervisor_review.evaluated` / `none` ``。
- 未发布 json 三口 `x-audit-event-success` 同值；main json `upsertStaffShift`/`batchUpsertStaffShifts` 仍为 `staff.shift.saved` / `staff.shift.save_failed`；main 无 `requestStaffPenaltyCap`。

四口一名是可观测性约定，不引入实体/枚举/画面。技术性成立。工程缺陷是。发布前须修是。

**2. 处置给出的两个成功名不是业务发明。**

GEN-017 顺序：冲突 → 细化 → 技术性 → 添加。此处无与 HTML 相反的新规则，故不是冲突；也不是 HTML 沉默下的新能力，故不是添加。

- `staff.payroll.penalty_cap_requested`：未发布 json summary 已是「申请调整普通扣除上限」；HTML BSM L1458 已要求上限调整经人力、财务、法务三审并由顶层确认。该标识符只命名这条已存在的「申请」写口的成功事件，与字典既有 `staff.leave.submitted` / `contract.submitted` 同构。
- `staff.shift.submitted`（批量同名）：HTML BSM L1443「排班表最终发布须经该主管的上一级审批后生效」「每一次修改同样须经该主管的上一级审批」。括号「排班提交待审」是对这句已拍板口径的事件语义标注，不是新开待审态、不是砍掉「追加记录不能删除」。本席 GR-006 正文要求「本口自己的名字（或明确的 `staff.shift.saved`）」——`submitted` 落在「本口自己的名字」支；基线两排班口本就共用 `staff.shift.saved`，批量与单口继续共用一名不发明第二条规则。
- `governance.supervisor_review.evaluated` 只留给 `evaluateSupervisorReview`：把治理评估口的事件名还给该口，不改审批链。

实现侧 `recordSuccess` 是 U14-008 `recordFailure` 的成功面对称，归 U06 同 PR 接线，不在本 md 发明事件清单。本段最小仍是：改 json 的 `x-audit-event-success` 后 `docs:openapi-closure` 重生附录。无新 2.2。票 **PASS**。

### 重点 (b) U16-007 交第 17 段生成器修正后重生 — **PASS（成立；与 GR-007 一致）**

独立核生成器，不采信候选「第 17 段」四字为己证。

- 附录 L1 标题 `V0.03`；L2611–2615 §9 只有 `| 2026-06-13 | V0.1 | … |`。
- 未发布 `tools/generate-openapi-contract-closure-appendix.mjs:174` 从现文件首行 regex 取 `documentVersion` 写入 L1；`:269-273` §9 表格写死 V0.1。main 生成器 `:173` 标题也写死 V0.1（故本席 B1 CMD-MAIN-GEN-UNPUB-JSON EXIT 1）。
- GR-007 最小处置：「重生本附录时改 §9（或删无法维护的变更表，只留标题版本）。U17 生成器不要把版本与变更记录做成一半动态一半写死。随 001 同一 PR。」

手改本 md 的 §9 会被生成器下次覆盖，不是最懒可行。根因文件属第 17 段。候选两条或支（内容变化时标题递增并在 §9 追加一行，**或**删除不可维护的变更表只留标题版本）与 GR-007 的或支同向，不发明第三条版本政策。GEN-016（版本与完成声明须可复现）。技术性。须修是。同一 PR。票 **PASS**。

### 重点 (c) 003 冲突、002 添加已答删除、001/004/005 技术性 — **PASS（与 GR 四分类一致）**

独立核，不采信候选「按 Grok」为己证。GEN-017 顺序：冲突 → 细化 → 技术性 → 添加。

| ID | GR-U16 四分类 | 候选 U16 | 同根 |
|---|---|---|---|
| 001 | 技术性（叠法） | 技术性 | U14-001 / U15-001 / U13-001 |
| 002 | 添加（已答删除） | 添加（已答删除） | U14-002 / U15-002 / U04-001 |
| 003 | 冲突 | 冲突 | U14-003 / U05-003 / U01-001 |
| 004 | 技术性（none） | 技术性 | U14-008 / U15-008 |
| 005 | 技术性（双值名） | 技术性 | U14-005 / U05-006 |

本轮原文（副本附录 + 未发布 json，本席抽出）：

- `:273` / `:1382-1383` / `:2437` `setMerchantSelfOperated` 收为正式 operation。HTML BSM L701 六类已含「自营」；PGM L578 标注落在六类码。`questions/U04-001.md` 答复首行「删除」→ **添加**，引用已答，不新开 2.2。与 GR-002 同分类。
- `:289` public 7 口含 `listStaffRegistrationCompanies` / `submitStaffSelfRegistration`；`:2400/:2477` 终端 `app, staff_private_phone`、失败 `none`。未发布 json 该 POST `security: []`，required 无设备公钥/指纹。HTML PGM L578/L579、BSM L935、BSM-114 L1865、BSM-120 L1975：本人注册在**私人手机 App**，安装时绑定设备标识；客人端 `app` 不是员工注册通道。公开无指纹与绑定顺序相反 → **冲突优先**。与 GR-003 同分类。公开注册本身是 HTML 两条路径的细化，缺陷是缺设备/错终端，不是添加。
- `:1-21` 730/329/1460；只叠本附录到 main JSON（仍 720）门禁红；main 生成器对未发布稿也红（版本行）。叠法/生成物同步，无新实体 → **技术性**。与 GR-001 同分类。
- `:1390-1405` 16 行事件名 `none`（失败 12 含 8 写口、成功 4）；AC-002 `:2585-2587` 要求可检索。main 附录打卡原有 `staff.attendance.punch_failed`。缺名是契约投影，不是「发明不留痕业务」→ **技术性**。与 GR-004 同分类。
- `:1596` / `:2420` 成功事件 `staff.locker.store / staff.locker.retrieve`。HTML BSM-120 L1976 / PGM L582 已要求柜子自动留痕；缺陷是事件名编码 → **技术性**。与 GR-005 同分类。席甲 OBS-2 升入 005 正确。

001/004/005/006/007 保持技术性：闸口一致性、失败可定位、事件名可索引、版本可复现。无业务发明。票 **PASS**。

### 重点 (d) U16-008 观察是否写错本席 §8（第 7/8/14 条）— **PASS（未写错；无应升队被降观察项）**

独立核本席 §8 原文。用户点名三条：

**第 7 条。** 本席原文：「生成器 `taskGroup()` 无 `staff`。新口 `tags: ["staff"]` 全部进 P0-待归类……建议 U17 加 `staff → AUTH-账号权限` 或把自助注册 tag 改为 access。不单独立项（首轮不因此单独红门禁）。」本轮核未发布生成器 `:45-65` 确无 `staff` 分支，默认 `"P0-待归类"`；附录 `P0-待归类` 出现 600 次（席甲 OBS-1 570→600 同向）。候选「7 生成器 taskGroup 无 staff 致新口全入待归类——建议第 17 段处理」是该句压缩。未升须修。正确。

**第 8 条。** 本席原文：打卡终端改为仅 `staff_work_phone`；基线附录为 `staff_private_phone, web`；按更具体的 BSM L1445「考勤打卡从领取工作手机开始算」判为**细化**，不升冲突；L930/L1865 与 L1445 的入口张力上报调度按人闸处理，本席不改 HTML。本轮核未发布 json `punchStaffAttendance` `x-terminal-allowed: ["staff_work_phone"]`，main 为 `staff_private_phone, web`；附录 `:2413` 终端列仅工作手机。候选「8 打卡终端收窄为工作手机按 BSM L1445 为细化，HTML L930/L1865 与 L1445 的入口张力登记给调度按人闸处理、本席不改 HTML」未写成须修、未申请改 HTML。正确。

**第 14 条。** 本席原文：「§1 L11 以本文为准压过历史 PRD。基线已有。未发布增量借此把 10 新口抬成执行口径；处置是改 JSON 后重生，而不是删这句（这句对「人工矩阵过时、以 OpenAPI 为准」仍有技术用途）。」候选「14 §1 L11「以本文为准」基线既有」压缩了「不得删这句」，但未改成建议删除，也未把 L11 升缺陷。沿用 U01「main 既有」口径：本材料未加重该句本身。正确。定稿宜补回「不得删」，见文末第 1 条。

**应升队而被降观察？无。** GR-001…007 全部入 001–007 且须修是。席甲漏判的 002/003 分项、006、007 已入队。OBS-2 升入 005。§8.4 幂等列仍在 JSON/U05，重生附录不会自动显示。§8.6 `raiseContractChange`「6 类」main 既有归 M389。§8.10 locker 身份模型在 U14-005。无 finding 被降。

票 **PASS**。

### 001 独立裁定（技术性叠法／须修是）

见 (c)。本路径 main blob＝基线 blob `94714d4e…`。B1 CMD-OVL：main json + 本附录 + 未发布生成器 `--check` EXIT 1。CMD-UNPUB-CHECK / CMD-MAIN-CHECK 各自绿。最小处置不发明业务：以 main `445a49f4` 为底，U14 json 按 002–008 改定稿后运行既有 `docs:openapi-closure` 重生本附录并 `--check`；与 U14/U15/U17 同一 PR（U13 安全基线随后重生）。禁止手改 md 独走。禁止整树覆盖。票 **PASS**。

### 002 独立裁定（添加已答删除／须修是；同根 U14-002 / U04-001）

见 (c)。`:273/:1382-1383/:2437` 独立布尔写口。答复首行「删除」，并写明 OpenAPI POST 随之去掉。随 U14-002 删 path 后重生附录。不新开 2.2。票 **PASS**。

### 003 独立裁定（冲突／须修是；同根 U14-003）

见 (c)。公开自助注册无设备公钥/指纹，终端含 `app`。处置「随 U14-003 改 json 后重生」继承 U14-003 定稿：设备公钥/指纹必填（无指纹 4xx）、去掉 `app`、public 保留但加频控、公司列表若仍需匿名发现须另证来源校验且不能作为无绑定注册的前置。不在附录上发明另一套注册规则。不新开 2.2。票 **PASS**。

### 004 独立裁定（技术性／须修是；同根 U14-008）

`:1390-1405` 16 行字面量 `none`。随 U14-008 给写口 `*_failed`、读口 `*.viewed/*.view_failed`，禁止字面量 none；打卡至少恢复 `staff.attendance.punch_failed`。重生附录。生成器拒 `none` 属 U17，非本段必改。票 **PASS**。

### 005 独立裁定（技术性／须修是；同根 U14-005）

`:1596/:2420` 双值成功事件。随 U14-005：单一成功名或按 `action` 分两个稳定名，失败名随 004；柜机单端契约。重生附录。票 **PASS**。

### 006 独立裁定（技术性／须修是；不并入 U14-008）

见 (a)。三口改回各自 operation 级成功事件名；`governance.supervisor_review.evaluated` 只留评估口。名字是标识符。票 **PASS**。

### 007 独立裁定（技术性／须修是；第 17 段生成器）

见 (b)。随 U17 生成器修正后重生，同一 PR。票 **PASS**。

### 008 独立裁定（登记／须修否）

见 (d)。双方观察归并，工程缺陷否，发布前须修否。P0-待归类范式、打卡终端细化、L11 基线既有、英文摘要、合同「6 类」、幂等列不在本 md，均维持观察。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. U16-008 第 14 条压缩「§1 L11 基线既有」：定稿补回本席「处置是改 JSON 后重生，**不得删这句**」。分类/须修不变。
2. U16-001 单元格补「禁止整树覆盖」；同 PR 集合写全 U14+U15+U16+U17（U13 安全基线随后 `docs:security-baseline`）。脚本名既有 `docs:openapi-closure` / `verify:openapi-closure`。
3. U16-006 定稿写明：`staff.shift.submitted` / `staff.payroll.penalty_cap_requested` 只是 `x-audit-event-success` 标识符；括号「排班提交待审」引用 HTML BSM L1443「须经上一级审批后生效」，不发明新审批态、不新开 2.2。`recordSuccess` 归 U06 同 PR，本段最小是改 json 后重生附录。两排班口共用一名与基线共用 `staff.shift.saved` 同构。
4. U16-005「单一成功名（动作进明细）」读成 U14-005 定稿的「单一名**或**按 action 分两个 operation」。
5. U16-003 同根格可补 U07-004（GR-003 有频控）。不改冲突分类。压缩句不得丢掉「私人手机通道 / 无指纹 4xx / 公司列表不能作为无绑定注册的前置」。
6. U16-008 第 7 条可保留 GR「不单独立项 / 首轮不因此单独红门禁」。不另开 ID。
7. U16-004 本段最小是契约事件名随 json 重生；`recordFailure` 归第 5/6 段同 PR。
8. U16-007「内容变化时标题版本递增并追加 §9」与「删除变更表只留标题」是或支，发布者择一即可，不发明第三套版本政策。
9. 席甲 OBS-1「+30 行待归类」与本席「10 个新 operation 进 P0」计数口径不同（席甲含修订行）；008 作观察并存即可，不升缺陷。
10. U16-008 第 1 条「本路径 main＝基线」即本席「对抗式覆盖 M009–M012 已修字节对本文件不成立」；不得读成整包覆盖未发布 OpenAPI 也无冲突（那是 001/002/003 的字典背书面）。

全批差集无漏并。四分类与 GR-001…007 一致；席甲一条重生漏判 002/003 分项、006、007，候选已勘误。006 单列不并入 U14-008 成立，事件名无业务发明。007 交第 17 段生成器与 GR-007 一致。003 冲突、002 添加已答删除、001/004/005 技术性与 GR 一致。008 未写错第 7/8/14 条原意，无应升队被降观察项。逐 ID 001–008 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U16 或 main 模块 PASS。步骤仍 B2；U16 IN_PROGRESS；材料未发布，不做 B3。

本批最终：**PASS**
