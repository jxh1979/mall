CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U05 未发布材料第 5 段（员工服务与数据访问）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T21:00:23Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `63a1e63d3a5d3998724f90234817c003e289f3e9`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 5 段，7 文件 2098 行（JdbcStaffRepository.java 1257 / StaffApprovalService.java 104 新增 / StaffController.java 370 / StaffLockerService.java 85 新增 / StaffNumericSetting.java 5 新增 / StaffProfileRecord.java 139 / StaffRegistrationService.java 138 新增），main 主归属 M054；适用判据＝M054 映射行并集。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。隔离编译在 `/tmp/u-overlay/backend`（基线 242f4872 `git archive` + 76 未发布文件，`mvn -o compile`）内进行，不触主仓、不写任何 complex_test* 库。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U05-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U04 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U05-00N，处置写给发布者），不做 B3/B4/B5。
- 只读依赖追流（不审）：副本内 `StaffService.java`、`dto/*`（第 6 段）、`SecurityConfig.java`、`DeviceCredentialVerifier.java`（第 7 段）、`PlatformService.java`/`MerchantRecord.java`（第 8 段）、`SupervisorReviewService.java`（第 9 段）、`V400–V409`（第 10 段）、`M010RegressionTests.java` 等（第 11 段）；main 当前同名文件与 `V086`/`V391`。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java | 1257 | 80826 | ac0df6e6ce81d86c71366daa5348cf1a5d5fd36190f4d9c713efee0552b163a6 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java | 104 | 5530 | d1b6358d34e2603974b6b481e33fc19ab199edbd1821f49df688695debee1497 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffController.java | 370 | 19424 | 746276dccc2c5a46c92b9999a7ecbce0fd34e8818656250243ffcb72d6c6e6da | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java | 85 | 5723 | 89cd15c99d1d59dd8e20ede570509dadaaf5749ca1778d4dd6ffb06a3d4b6271 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffNumericSetting.java | 5 | 241 | 2e6b6eeeb98346251fac08f046b62e8bf8214f27177a85803ffe99d5255780ed | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java | 139 | 8048 | 2ec7d1330133c0cf338da3a8b5cb9a17e64bd8c635364e7fd39318db5acebd83 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java | 138 | 7892 | 418bf16647a304a4c5fe6e4b5ac2f106638f9ef945aae739be227a946a283bdf | 全读 |

## 2. 可执行证据原文（/tmp/u05_b1_run.log）

```
START=2026-09-09T20:57:53Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java | 1257 | 80826 | ac0df6e6ce81d86c71366daa5348cf1a5d5fd36190f4d9c713efee0552b163a6
backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java | 104 | 5530 | d1b6358d34e2603974b6b481e33fc19ab199edbd1821f49df688695debee1497
backend/src/main/java/com/complex/platform/access/staff/StaffController.java | 370 | 19424 | 746276dccc2c5a46c92b9999a7ecbce0fd34e8818656250243ffcb72d6c6e6da
backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java | 85 | 5723 | 89cd15c99d1d59dd8e20ede570509dadaaf5749ca1778d4dd6ffb06a3d4b6271
backend/src/main/java/com/complex/platform/access/staff/StaffNumericSetting.java | 5 | 241 | 2e6b6eeeb98346251fac08f046b62e8bf8214f27177a85803ffe99d5255780ed
backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java | 139 | 8048 | 2ec7d1330133c0cf338da3a8b5cb9a17e64bd8c635364e7fd39318db5acebd83
backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java | 138 | 7892 | 418bf16647a304a4c5fe6e4b5ac2f106638f9ef945aae739be227a946a283bdf
=== manifest entries ===
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java 1257 80826 ac0df6e6ce81d86c71366daa5348cf1a5d5fd36190f4d9c713efee0552b163a6
backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java 104 5530 d1b6358d34e2603974b6b481e33fc19ab199edbd1821f49df688695debee1497
backend/src/main/java/com/complex/platform/access/staff/StaffController.java 370 19424 746276dccc2c5a46c92b9999a7ecbce0fd34e8818656250243ffcb72d6c6e6da
backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java 85 5723 89cd15c99d1d59dd8e20ede570509dadaaf5749ca1778d4dd6ffb06a3d4b6271
backend/src/main/java/com/complex/platform/access/staff/StaffNumericSetting.java 5 241 2e6b6eeeb98346251fac08f046b62e8bf8214f27177a85803ffe99d5255780ed
backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java 139 8048 2ec7d1330133c0cf338da3a8b5cb9a17e64bd8c635364e7fd39318db5acebd83
backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java 138 7892 418bf16647a304a4c5fe6e4b5ac2f106638f9ef945aae739be227a946a283bdf
=== baseline 242f4872 -> unpublished tree e28dad05 (this segment) ===
 .../platform/access/staff/JdbcStaffRepository.java | 370 +++++++++++++++++----
 .../access/staff/StaffApprovalService.java         | 104 ++++++
 .../platform/access/staff/StaffController.java     |  79 ++++-
 .../platform/access/staff/StaffLockerService.java  |  85 +++++
 .../platform/access/staff/StaffNumericSetting.java |   5 +
 .../platform/access/staff/StaffProfileRecord.java  |  84 ++++-
 .../access/staff/StaffRegistrationService.java     | 138 ++++++++
 7 files changed, 782 insertions(+), 83 deletions(-)
=== main current vs unpublished copy + main_vs_baseline blob ids ===
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java main_sha=8f618140863a8a7791b33d0880d1c734292cb6c243f20b7db9ad837d9b5d13a4 unpublished_sha=ac0df6e6ce81d86c71366daa5348cf1a5d5fd36190f4d9c713efee0552b163a6 diff_lines=370 baseline=936b34cd82 head=936b34cd82 main_unchanged_since_baseline=yes
backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java main_exists=no
backend/src/main/java/com/complex/platform/access/staff/StaffController.java main_sha=e7afa9eb3b98c813c4067da2c9f0d2d4dc1752ac6c6b8d3ab6ad82e74be7bd77 unpublished_sha=746276dccc2c5a46c92b9999a7ecbce0fd34e8818656250243ffcb72d6c6e6da diff_lines=79 baseline=6d4e6dc42c head=6d4e6dc42c main_unchanged_since_baseline=yes
backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java main_exists=no
backend/src/main/java/com/complex/platform/access/staff/StaffNumericSetting.java main_exists=no
backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java main_sha=ec2966f1f6871035c3092c700ea4dd3a2f564e02b24f2220becb2a991774ed50 unpublished_sha=2ec7d1330133c0cf338da3a8b5cb9a17e64bd8c635364e7fd39318db5acebd83 diff_lines=84 baseline=cf066a433c head=cf066a433c main_unchanged_since_baseline=yes
backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java main_exists=no
=== probes: PII cipher (encrypt/decrypt), mask ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:891:                .addV
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:892:                .addV
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:893:                .addV
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:1199:                piiC
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:1200:                piiC
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:1201:                piiC
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java:102:                com.co
9
=== probes: leave deduction base + penalty cap (closePayrollMonth) ===
52:        int current = jdbcTemplate.queryForObject("SELECT numeric_value FROM staff_policy_settings WHERE policy_key='payroll_penalty_cap_bps' AND scope_id='global' FOR
56:        saveNumericSetting("payroll_penalty_cap_bps","global",new StaffNumericSetting(((Number)row.get("numeric_value")).intValue(),
148:                        bonus_cent, penalty_cent, leave_deduction_cent, tax_cent, total_cent, status, closed_by, closed_at, project_id, penalty_cap_bps)
153:                       capped.penalty,
154:                       COALESCE(lv.deduction, 0),
157:                       GREATEST(0, COALESCE(b.monthly_salary_cent, 0) + COALESCE(c.total, 0) + COALESCE(bc.total, 0)
158:                           + COALESCE(adj.bonus, 0) - capped.penalty - COALESCE(lv.deduction, 0) - COALESCE(tax.tax_cent, 0)),
159:                       'closed', :closed_by, :now, sp.project_id, cap.value
197:                      SELECT COALESCE((SELECT locked.penalty_cap_bps FROM staff_payroll_statements locked
200:                                       WHERE policy_key='payroll_penalty_cap_bps' AND scope_id='global'),2000) AS value
204:                          ROUND((COALESCE(b.monthly_salary_cent, 0)::numeric + COALESCE(adj.bonus, 0))
205:                              * cap.value
210:                      SELECT ROUND(COALESCE(b.monthly_salary_cent, 0)::numeric
=== probes: self-registration public path / device fields / rate limit ===
30:    private final IdempotencyService publicRequests;
40:        this.publicRequests = new IdempotencyService(idempotency, json);
46:    public List<CompanyOption> companies() {
52:    public Receipt selfRegister(StaffSelfRegistrationRequest request) {
59:        String companyId = requireText(request.companyId(), "所属公司");
60:        if (companies().stream().noneMatch(c -> c.id().equals(companyId))) {
67:                throw ApiException.badRequest("COMPANY_SELF_REGISTRATION_REQUIRED", "该商户不属于所选公司的自营商户");
70:        return publicRequests.execute("publicStaffSelfRegistration", request.idempotencyKey(), request, () -> {
91:        if (merchant.selfOperated()) throw ApiException.badRequest("COMPANY_SELF_REGISTRATION_REQUIRED", "自营商户员工须在私人手机自助注册");
77:                    authorize.requestMatchers(publicApiPaths()).permitAll();
79:                        authorize.requestMatchers(apiDocumentationPaths()).permitAll();
121:                "/api/v1/staff/registration-companies",
122:                "/api/v1/staff/self-registrations",
0
=== probes: locker (dual identity), callers in 76 files ===
41:        authorization.requirePermission("staff.profile.self");
46:        var terminal=TerminalIdentityHolder.current().filter(t -> t.verified() && "kiosk".equals(t.terminalType()))
47:                .orElseThrow(() -> ApiException.forbidden("LOCKER_DEVICE_REQUIRED", "只接收已验证柜机的扫码完成事件"));
48:        var device=devices.findByDeviceId(terminal.deviceId()).filter(d -> "bound".equals(d.status()) && "kiosk".equals(d.terminalType()))
49:                .orElseThrow(() -> ApiException.forbidden("LOCKER_DEVICE_REQUIRED", "柜机未登记或已停用"));
60:        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(:lock,0))",
backend/src/main/java/com/complex/platform/access/staff/StaffController.java
=== probes: permission changes / approval event wiring ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffService.java:123:    @GuardedAction(permission = "staff
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffService.java:199:                                && !au
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffService.java:202:                        if (!authoriza
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffService.java:311:    @GuardedAction(permission = "staff
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffService.java:388:        if (authorizationService.isMas
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffService.java:400:        if (!authorizationService.isMa
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java:69:    @EventListener
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java:71:    public void afterDecision(S
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java:292:                    if
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java:306:        boolean master
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java:759:        boolean master
=== probes: hardcoded zone / scope keys ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:216:                               
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:217:                               
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:222:                         AND (s
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:254:                .addValue("comp
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:778:                         AND (p
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffService.java:769:                ? "company:" + registr
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/access/staff/StaffService.java:828:        String scope = profile.merchan
=== HTML greps (main 正本) ===
docs/business-system-map-for-operations-v0.1.html:1443:                <li>主管排班：排到几点几分，可指定
docs/business-system-map-for-operations-v0.1.html:1450:                <li>请假扣薪：本综合体实行综合计算
docs/business-system-map-for-operations-v0.1.html:1458:                <li>每位员工都有一个结构化的薪资配
docs/platform-governance-map-for-operations-v0.1.html:578:                <li>员工注册分两条路径：公司自有
docs/platform-governance-map-for-operations-v0.1.html:582:                <li>办公区私人手机寄存柜流动使用
docs/platform-governance-map-for-operations-v0.1.html:870:                <li>客人和员工的手机号、身份信息
docs/platform-governance-map-for-operations-v0.1.html:871:                <li>员工日常界面只显示脱敏号码，
=== main schema facts: phone_hash nullability, approval_lines seeds ===
17:    phone_hash VARCHAR(64),                          -- 手机号不可逆哈希（PIPL）
18:    id_doc_hash VARCHAR(64),                         -- 证件不可逆哈希（可选）
8:    ('finance_review', '财务审核线', 1, NULL, 'active'),
10:    ('legal_review',   '法务审核线', 1, NULL, 'active')
=== isolated compile: /tmp/u-overlay/backend mvn -o compile (log /tmp/u_backend_compile.log); class count ===
START=2026-09-09T18:37:14Z
MVN_EXIT=0
END=2026-09-09T18:37:19Z
1489
dto
EmployeeNoticeRecord.class
JdbcStaffRepository$ShiftRevision.class
JdbcStaffRepository.class
PayrollTaxBracketRecord.class
StaffApprovalService$History.class
StaffApprovalService.class
StaffAttendanceDayRecord.class
StaffAttendanceRecord.class
StaffCommissionLineRecord.class
StaffController.class
StaffDeviceCredentialRecord.class
=== isolated scratch-DB staff test run (U11 evidence, fresh DB u11_scratch migrated V001..V409 in /tmp/u-overlay; /tmp/u11_tests.log) ===
START=2026-09-09T20:56:15Z
SCRATCH_DB_CREATED
repointed_files=5 remaining_complex_test_refs=0
MVN_EXIT=0
target/surefire-reports/com.complex.platform.access.staff.M010RegistrationHttpTests.txt:Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.677 s -- in com.complex.platform.access.staff.M010RegistrationHttpTests
target/surefire-reports/com.complex.platform.access.staff.M010RegressionTests.txt:Tests run: 43, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.178 s -- in com.complex.platform.access.staff.M010RegressionTests
target/surefire-reports/com.complex.platform.access.staff.PayrollSafetyJdbcIntegrationTests.txt:Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.063 s -- in com.complex.platform.access.staff.PayrollSafetyJdbcIntegrationTests
target/surefire-reports/com.complex.platform.access.staff.StaffAttendanceLocalZoneTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.055 s -- in com.complex.platform.access.staff.StaffAttendanceLocalZoneTests
target/surefire-reports/com.complex.platform.access.staff.StaffJdbcIntegrationTests.txt:Tests run: 29, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.605 s -- in com.complex.platform.access.staff.StaffJdbcIntegrationTests
target/surefire-reports/com.complex.platform.access.staff.StaffProfileArchiveDeleteJdbcIntegrationTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.037 s -- in com.complex.platform.access.staff.StaffProfileArchiveDeleteJdbcIntegrationTests
target/surefire-reports/com.complex.platform.access.staff.StaffScopeTests.txt:Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.547 s -- in com.complex.platform.access.staff.StaffScopeTests
flyway_max_version=409
SCRATCH_DB_DROPPED
END=2026-09-09T20:56:33Z
END=2026-09-09T20:57:54Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 5 段（7 文件 2098 行；`StaffApprovalService`、`StaffLockerService`、`StaffNumericSetting`、`StaffRegistrationService` 为 main 无对应的新增文件），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段文件的真实改动 = `git diff 242f4872 e28dad05 -- <本段路径>`（§2 diffstat），main 当前版本仅作分叉对照。
- 第一性原理线：从 HTML 反推员工数据访问层应当做什么——PGM L870/L871「个人信息一律加密存放；员工日常界面只显示脱敏号码」、BSM L1450「只有事假扣薪，按请假工时占当期总工时的比例，从其当期应发薪资中扣减；零为下限」、BSM L1458「各类扣除合计不得超过该员工当月总薪资的 20%（安全阈值，全平台统一可配，调整须经人力、财务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置上下限）（总薪资指基础工资、各项补贴、绩效工资与奖金之和）」、BSM L1443「排班表最终发布须经该主管的上一级审批后生效……每一次修改同样须经该主管的上一级审批。所有排班的填写与修改只能追加记录、不能删除，修改前后的排班时间都完整保留」、BSM L1444 可见天数、PGM L578/BSM L1433 注册两路径、PGM L582/BSM L938「寄存柜开箱与取件一律扫码，由柜子自动记录，不设手工登记」、BEQR L240 员工状态五值；逐一对照仓储 SQL、服务与控制器。
- 顺从式线：52 项判据逐项（§5）。
- 对抗式线：可逆加密是否与 HTML/文案一致；注册分流所依赖的「自营」是独立布尔还是合作方式（U04-001 操作员已答「删除」）；扣薪与封顶公式在基数、分母、例外上与 HTML 是否逐字一致；公开注册入口的信任边界（无认证、无设备证明、无频控）；柜机事件的调用方身份（柜机设备签名 + 员工令牌同请求）是否有任何客户端能发出；排班审批事件与发布的事务性；权限点变更（存档 review→submit）；作用域键（company:/merchant:）写读是否一致；main 分叉与迁移编号。
- 只读依赖追流（不审、不计入本段结论）：副本 `StaffService.java`、`dto/*`（第 6 段）、`SecurityConfig.java`/`DeviceCredentialVerifier.java`（第 7 段）、`PlatformService.java`/`MerchantRecord.java`（第 8 段）、`SupervisorReviewService.java`（第 9 段）、`V400–V409`（第 10 段）、`M010RegressionTests.java`/`M010RegistrationHttpTests.java`（第 11 段）；main `V086`（`phone_hash`/`id_doc_hash` 可空）、`V391`（审批线种子）。
- 7 文件全文逐行读完（§1）；与基线的逐文件 diff 逐块读完（§2 diffstat）。可执行验证：`/tmp/u-overlay/backend`（基线 + 76 未发布文件）离线 `mvn -o compile` 退出 0、1489 个 class（§2）；本段 DB 行为测试（第 11 段）需在 complex_test 上应用 V400–V409，席甲不对共享测试库做写操作，未跑（如实记）。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- 三个既有文件（`JdbcStaffRepository`、`StaffController`、`StaffProfileRecord`）main 自基线以来是否有改动见 §2 blob 对照；M009–M012 未触及 `access/staff`。四个新增文件 main 无对应。
- 跨段依赖：本段编译依赖第 6 段 DTO/`StaffService`、第 9 段 `SupervisorReviewService` 新策略、第 10 段迁移（`staff_policy_settings`、`staff_shift_revisions`、`staff_penalty_cap_revisions`、`staff_locker_records`、`merchants.is_self_operated`、`staff_profiles.phone/id_doc/employment_status/company_id/registration_source/submitted_by`）；main 已有 V410 而无 V400–V409 → 迁移编号须重排（第 10 段登记）。

## 5. 52 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 加密、扣薪、封顶、审批、可见天数、自助注册、柜机留痕、员工状态均有 HTML 上位 |
| GEN-002 可执行正确性 | 不通过 | 事假扣薪基数取固定工资而非 HTML「应发薪资」（CX-U05-001）；柜机事件无任何调用方能同时携带柜机签名与员工令牌（CX-U05-003） |
| GEN-003 测试有效性 | 通过（登记） | 第 11 段回归覆盖本段大部分分支；但 `M010RegressionTests:698-716` 把「基数＝固定工资」锁死，与 HTML 相反（随 CX-U05-001 改） |
| GEN-004 信任边界 | 不通过 | 公开自助注册无认证、无设备证明、无频控，接受姓名/手机/证件写库（CX-U05-002） |
| GEN-005 授权 | 通过（登记） | 注册审批 SoD（发起人不得自批）、公司归属校验、删除须最高权限、本人设备/通知 requireOwnProfile；存档权限由 review 放宽为 submit（OBS-1） |
| GEN-006 数据完整性 | 通过（登记） | 扣除上限并发锁（FOR UPDATE + POLICY_CHANGED）；排班发布乐观校验（updated_at/start>now）；`publishShiftRevision` INSERT 可能撞 `uq(user_id,start_at)` 直抛 SQL 异常（OBS-2） |
| GEN-007 并发幂等 | 通过 | 公开注册独立幂等命名空间；柜机格口 advisory lock；打卡 `lockAttendanceUser` |
| GEN-008 接口一致性 | 不通过 | 注册分流读独立自营布尔而非合作方式（CX-U05-004）；DTO 与客户端字段一致（U02 §5）；可见天数 scope 键写读不一致属第 6 段（U06 候选）；封顶分母 base+bonus 窄于 HTML「总薪资」（OBS-3） |
| GEN-009 错误处理 | 通过（登记） | 业务错误均为中文 ApiException；OBS-2 的 SQL 异常未转业务码 |
| GEN-010 安全 | 通过（登记） | 姓名/手机/证件可逆加密（PiiCipher）+ 日常接口脱敏，符合 PGM L870–871；公开端口暴露公司列表（OBS-4）；旧哈希列保留（OBS-5） |
| GEN-011 审计证据 | 通过 | 注册/审批/状态/扣薪封顶/柜机/排班发布均记审计事件 |
| GEN-012 资源 | 通过 | 月结单 SQL 一次成单；批量排班 ≤500 |
| GEN-013 构建 | 通过 | 隔离 `mvn -o compile` 退出 0（§2） |
| GEN-014 迁移 | 不适用（登记） | 本段无迁移文件；依赖第 10 段（编号与 main V410 冲突在 U10 登记） |
| GEN-015 界面 | 不适用 | 无界面 |
| GEN-016 文档 | 通过（登记） | 记录注释「原值由仓储统一加密；日常接口返回脱敏副本」与实现一致；测试/迁移以「M010-xxx」自编号与本审计 M010 模块号撞名（OBS-6） |
| GEN-017 分类 | 通过（登记） | 四分类见 §6；封顶三审为 HTML L1458 原句，非添加；自营布尔分流为 U04-001 已答「删除」的添加项消费方（CX-U05-004），本段不再问 |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、§2 命令 |
| BEQR-157 员工状态 | 通过 | 五值闭集（`StaffService` L343，`V404` CHECK） |
| BEQR-182 考勤日结果 | 通过 | 六态（present/absent/late/early_leave/missing_checkout/leave/rest）按班次结束日归行 |
| BEQR-183 打卡方式 | 通过 | nfc/qr/charging_disconnect（HTML L1445），manual 仅历史 |
| BEQR-184 请假类型 | 通过 | personal/sick/comp/annual |
| BEQR-185 请假单状态 | 通过 | pending/approved/rejected |
| BEQR-186 薪资单构成 | 通过（登记） | 固定/提成/奖金/处罚/请假扣薪/个税分列；补贴/绩效档不在本段范围（OBS-3） |
| BEQR-187 员工提成发放 | 通过 | 提成台账与月结不变 |
| BSM-001 | 通过 | 无冲突 |
| BSM-003 通用能力 | 通过 | 存档/删除范式、审批留痕 |
| BSM-009 基础管理 | 通过 | 无冲突 |
| BSM-028 | 通过 | 无实现面冲突 |
| BSM-029 | 通过 | 无实现面 |
| BSM-073 | 不适用 | 无实现面 |
| BSM-084 员工手机端 | 通过 | 自助接口只返本人 |
| BSM-085 日常工作 | 通过 | 无冲突 |
| BSM-086 排班考勤请假 | 不通过 | L1450 事假扣薪基数（CX-U05-001）；其余（审批、追加留痕、最早一次、六态）一致 |
| BSM-087 我的薪资 | 通过 | 月结后可见、脱敏 |
| BSM-088 | 通过 | 无冲突 |
| BSM-114 各类入口 | 通过 | 公司/商户/项目作用域过滤；柜机事件调用方缺失见 CX-U05-003 |
| BSM-115 | 通过 | 无实现面 |
| BSM-120 已确认规则 | 不通过 | L1450 基数（CX-U05-001）；L1433/L578 注册路径的信任边界（CX-U05-002） |
| PGM-001 整体定位 | 通过 | 错误文案中文；无用户界面 |
| PGM-008 员工和手机 | 通过 | 无冲突 |
| PGM-022 空间和点位 | 通过 | 打卡带点位标签/空间 |
| PGM-024 | 不适用 | 无实现面 |
| PGM-026 | 通过 | 无冲突 |
| PGM-032 员工和手机 | 不通过 | 公开注册无设备证明（CX-U05-002，与 U01-007/U02-002 同 PR） |
| PGM-033 | 不通过 | 同上 |
| PGM-034 | 不适用 | 无实现面 |
| PGM-035 | 通过 | 无冲突 |
| PGM-036 设备管理 | 通过 | 柜机须已登记 kiosk、工作手机须分配给本人 |
| PGM-048 商户侧 | 通过 | 商户代提交按域公司审批 |
| PGM-059 个人信息保护 | 通过（登记） | 加密存放 + 脱敏；公开端口公司列表（OBS-4） |
| PGM-066 | 通过 | 无冲突 |

## 6. 本次 Findings（临时 ID CX-U05-00N；正式 ID 由 B2 分配）

### CX-U05-001 · 冲突 · 发布前须修：是

- 位置：`JdbcStaffRepository.java:208-223`（`closePayrollMonth` 事假扣薪 LATERAL：`ROUND(COALESCE(b.monthly_salary_cent,0)::numeric * Σleave_seconds / Σshift_seconds)`），`:157-158`（total 用 `lv.deduction`）；第 11 段 `M010RegressionTests.java:698-716` 断言半月事假扣 5000＝固定工资 10000 的一半。
- HTML 原句：BSM L1450「只有事假（无薪假）扣薪，按该员工当期请假工时占当期总工时的比例，从其当期应发薪资中扣减；扣减后的当期薪资以零为下限，不得为负」。
- 判据：BSM-086、BSM-120、GEN-002；依据类型：HTML 原句。
- 业务影响：比例的作用对象是「当期应发薪资」（固定工资+提成+奖金等应发项），实现只按固定工资折算：有提成/奖金的员工事假少扣，无固定工资只有提成的员工事假不扣——月结金额与 HTML 口径系统性偏差。工时比例（按排班工时、按班次结束日归期）与零下限两点实现正确。
- 最小反例：固定工资 10000、提成 9000、事假占当月排班工时 50% → 实现扣 5000；HTML 口径按应发（19000）扣 9500。§2 探针行号。
- 工程缺陷：是。建议登记：是。最小处置（给发布者）：扣薪基数改为当期应发薪资合计（固定+提成+奖金，个税与处罚另算），并改第 11 段对应断言；不在本轮修复。

### CX-U05-002 · 技术性 · 发布前须修：是

- 位置：`StaffRegistrationService.java:52-80`（`selfRegister`：无认证主体、独立幂等命名空间、直接落库姓名/手机/证件）、`:46-50`（`companies()` 公开列出活跃公司实体）；第 7 段 `SecurityConfig.java:121-122` 两路径 permitAll；`dto/StaffSelfRegistrationRequest.java` 六字段无设备标识。
- HTML 原句：PGM L578/BSM L1433 公司自有员工由本人在私人手机 App 注册；PGM L579/BSM L935「私人手机 App 安装时生成并绑定一个该手机专属的设备标识，把手机和员工绑定」。
- 判据：GEN-004、GEN-010、PGM-032/033、BSM-120；依据类型：HTML 原句 + 代码事实。
- 业务影响：任何匿名调用方可无限制向 `staff_profiles` 写入含个人信息的待审申请（仅靠调用方自报幂等键去重），且申请与提交设备无绑定，审批人无法把申请对应到 HTML 要求的那台手机；同时公开端口枚举公司实体名单。
- 最小反例：`curl -X POST /api/v1/staff/self-registrations`（无 Authorization、无设备头）循环不同幂等键 → 每次新增一行待审档案。
- 无业务发明理由：HTML 已要求安装时设备标识绑定与私人手机通道；补设备证明/频控是执行保障，不新增规则。
- 工程缺陷：是。建议登记：是。最小处置：自助注册要求私人手机端设备公钥/指纹并在服务端留存（与 U01-007/U02-002 同 PR），公开端口加频控与来源校验；`companies()` 只返回注册所需的最小字段或改为登录后可见（HTML 沉默处登记）。不在本轮修复。

### CX-U05-003 · 技术性 · 发布前须修：是

- 位置：`StaffLockerService.java:40-55`（`record`：`requirePermission("staff.profile.self")` 取当前员工 + `TerminalIdentityHolder.current()` 须已验签 kiosk 设备）；`StaffController.java:40-44`；副本 76 文件中无任何客户端调用 `/staff/locker-events`（§2 探针）；第 11 段 `M010RegressionTests.java:155-186` 以请求属性直接注入已验签柜机模拟。
- HTML 原句：PGM L582/BSM L938「开箱与取件一律扫码，是谁、哪一格、存取时间由柜子自动记录，不设手工登记」。
- 判据：GEN-002、GEN-008、PGM-036；依据类型：HTML 原句 + 代码事实。
- 业务影响：接口要求同一请求既带柜机的设备签名（kiosk 登记公钥）又带员工的 Bearer 令牌；柜机没有员工令牌、员工私人手机没有柜机私钥，副本内也没有任何一端发出该事件——「柜子自动记录」在可执行层没有调用方，只有测试用注入属性绕过。
- 无业务发明理由：缺的是调用方与身份呈现方式，不是新规则。
- 工程缺陷：是。建议登记：是。最小处置：明确柜机作为已登记 kiosk 以设备签名上报，员工身份由扫码载荷（工作手机/私人手机端签发的短时凭证）承载并由服务端校验，补 HTTP 级测试；不在本轮修复。

### CX-U05-004 · 添加（操作员已答删除，questions/U04-001.md）· 发布前须修：是

- 位置：`StaffRegistrationService.java:66-67`（`selfRegister`：`!merchant.selfOperated()` → `COMPANY_SELF_REGISTRATION_REQUIRED`「该商户不属于所选公司的自营商户」）、`:89-91`（`merchantApprovingCompany`：`merchant.selfOperated()` → 「自营商户员工须在私人手机自助注册」）；两处以第 8 段 `MerchantRecord.selfOperated`（独立布尔，V408 `merchants.is_self_operated`）分流员工注册路径。
- HTML 原句：BSM L701/PGM L495「区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式」；BSM L1433/PGM L578「在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理」。
- 判据：GEN-017、GEN-008、BSM-120、PGM-032；依据类型：HTML 原句 + 操作员 2.2 答复。
- 事实：独立布尔与六类合作方式可分叉（U04-001，Grok 类型探针）；操作员 2026-09-09 已按 2.2 弹窗答复「删除」：自营标注＝合作方式「自营」，员工路径按合作方式派生。本段两处分流是该布尔在业务规则上的唯一消费方。
- 工程缺陷：是。建议登记：是（引用已答复，不再问）。最小处置（给发布者）：两处改读 `"self_operated".equals(merchant.cooperationForm())`（main 六类码），随 U04-001 一并去掉对 `selfOperated()`/V408 该列/第 8 段端点的依赖；第 11 段相应夹具改设 `cooperationForm`。不在本轮修复。

## 7. 观察（登记、不入队）

- OBS-1：`archiveProfile` 守卫由 `staff.profile.review` 放宽为 `staff.profile.submit`（`StaffService` L311，与第 3 段页面一致）；HTML BSM-003 权限分岗未细到存档，登记。
- OBS-2：`publishShiftRevision` 新建分支 INSERT 不处理 `uq(user_id,start_at)` 冲突（`JdbcStaffRepository.java:622-626`），与既有已发布班次同起点时直抛 SQL 异常而非业务码（GEN-009）。
- OBS-3：处罚封顶分母 `base + bonus`（`:202-207`）窄于 HTML L1458「总薪资指基础工资、各项补贴、绩效工资与奖金之和」——现行 schema 无补贴/绩效列，属结构性细化；收银短款/赔偿例外未建模。
- OBS-4：`companies()` 与 `/registration-companies` 公开返回活跃公司实体 id/名称。
- OBS-5：`phone_hash`/`id_doc_hash` 旧列保留（V086 可空），不再写入；V403 注释「不尝试从旧哈希恢复」。
- OBS-6：迁移与测试以「M010-001/M010-006/M010-017…」自编号，与本审计 M010 模块号撞名，文档口径需区分。
- OBS-7：`findFirstCheckIn` 写死 `AT TIME ZONE 'Asia/Shanghai'`（`:778`），其余用 `LocalZone.ID`/`:local_zone`；HTML L1446「以员工所在门店／点位的当地时间为准」在全仓仍是单时区（main 既有，归 M054）。
- OBS-8：`listMyShifts` 按 `work_date` 取班、月视图/月结按 `end_at` 归期，两把尺子并存（第 6 段调用）；可见天数 scope 键写读不一致在第 6 段登记。
- OBS-9：`StaffController` 以字段 `@Autowired` 注入 `registrations`/`lockers`（风格，不影响行为）。
- 技能：`using-superpowers`、`verification-before-completion`（编译实跑附 §2；DB 行为测试未跑如实记）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。7 文件全文三线审计、52 项判据逐项核对；发现 1 项冲突须修（CX-U05-001）、2 项技术性须修（CX-U05-002/003）、1 项添加消费方（CX-U05-004，操作员已于 U04-001 答「删除」，本段引用不再问）、9 项观察；封顶三审为 HTML L1458 原句非添加；无 HTML 修改申请；隔离编译绿；未修源、未提交/推送、未写任何 complex_test* 库。本报告落盘前未读取 `U05-b1-grok-raw.md` 任何正文。
---

# U05 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.52「当前 U05 B2-r1 整批候选」U05-001…012；modules/U05-b1-grok-raw.md V0.01（46953bytes SHA863c75bd05f19a74d81b7ba8ed542659a26b8ab1f486816297a62d94ae99f7d3）；本文件 B1 段（37895bytes SHAc28190d2385dc9ef0a8dbade2aae03a2ef8305021946beabcfaefe72357df98c）。2026-09-09T21:19:24Z
- 席甲 B2 核实（只读，见 ledger V2.56 U05 节）：Grok 独有 GR-004/006/007/008/009/010 逐条对照副本源码行号均成立；HTML BSM L1444/L1450/L1458 原句已逐字核对（§2 grep）。

## 勘误（席甲 B1 段）

- CX-U05-004「添加（操作员已答删除）」改按冲突登记（读第二轴与 BSM L701/PGM L495 六类闭集冲突），处置不变；CX-U05-002 由技术性改冲突（PGM L579 明文）；OBS-3 封顶分母由观察升冲突入队（B1 低估）；预注中的 `setShiftVisibility` 无幂等键在 B1 正文漏写，B2 并入 U05-011。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | `:66-67/:89-91` 读 `selfOperated()`；六类含自营；处置沿用 U04-001 已答删除，改读六类码一处 |
| 002 | PASS | BSM L1450 逐字「从其当期应发薪资中扣减」；SQL 只取 `monthly_salary_cent`；第 11 段断言锁基数；席甲独有项以 Grok B2 裁定为准 |
| 003 | PASS | `selfRegister` 无设备字段；PGM L579 明文；与 U01-007/U02-002 同根同类 |
| 004 | PASS | 封顶分母 `monthly_salary_cent + bonus` 相对基线新增；HTML 总薪资定义更宽；处置不发明薪资项 |
| 005 | PASS | 缺行返回 `(7,null,null)`、V402 无种子；HTML 明文「须同时配置上下限」；数值属上线配置不发问 |
| 006 | PASS | 双身份同请求、无单端调用方；HTML 自动记录 |
| 007 | PASS | `shiftRevisions` 恒 `FOR UPDATE`，GET 历史调用 |
| 008 | PASS | 三份既有文件 main＝基线；调用面/迁移分叉；同 PR + 重编号 |
| 009 | PASS | `getFirst()` 只认最近公司；`anyMatch` 最小 |
| 010 | PASS | `COALESCE` 五处 + U06 null 放行；铁律 8 |
| 011 | PASS | 两写口无幂等键，同控制器 23 处有 |
| 012 | PASS | 观察归并未改写双方原意；空库实跑作可执行证据并如实记录环境决定 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 7 条，定稿以此为准）

1. 002 冻结判据以 BSM-086/GEN-002 为主，BSM-120 不作主支撑；「等应发项」限定为月结已列应发栏（固定工资+提成+激励奖金；补贴/绩效落地后并入），小费/服务费全额发放不入基数。
2. 003 「无认证」本身符合尚无账号；频控/来源校验是执行加固，不是冲突必修核心。
3. 004 「现有列先取全」不要求本 PR 新建结构化薪资表。
4. 005 种子上下限由发布者批准，不得写成 HTML 出厂闭集；审计不指定数字。
5. 011 同文件 `requireIdempotencyKey` 实算 22 次调用（候选「23 处」为计数口径）；archive/unarchive/sales-position/delete-* 无键属既有，不另开 ID。
6. 012 的空库 92/92 只证明用例按自身断言可执行，不能消解 002/004 把错误口径锁死。
7. 席甲 B1 CX-U05-001 引用第 11 段行号 `:698-716` 有误（文件止于 L676），定稿以 `:637-675` 为准。

## 结论

席甲 B2-r1 票：**PASS**（候选 U05-001…012 成立；发布前须修「是」11 / 「否」1；添加 0；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 69066bytes SHA40d4c9293a51409d86bb600c44bb4bb38a83589772d5c040c993b9382a5add8f，前 46953bytes 原字节不变）。
