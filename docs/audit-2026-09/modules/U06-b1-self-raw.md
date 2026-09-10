CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U06 未发布材料第 6 段（员工服务与数据访问）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T21:37:47Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `91864d9f01d36634c384c391b2970c177ea4fc16`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 6 段，9 文件 1664 行（StaffService.java 1553 / StaffShiftRecord.java 33 / dto：StaffEmploymentStatusRequest 4 新增、StaffNumericSettingRequest 4 新增、StaffPenaltyCapRequest 9 新增、StaffRegistrationRequest 19、StaffSelfRegistrationRequest 5 新增、StaffShiftBatchResult 13、StaffShiftRequest 24），main 主归属 M054、M288；适用判据＝映射行并集。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。隔离编译在 `/tmp/u-overlay/backend`（基线 242f4872 + 76 未发布文件，`mvn -o compile`）内进行，不触主仓、不写任何 complex_test* 库。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U06-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U05 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U06-00N，处置写给发布者），不做 B3/B4/B5。
- 只读依赖追流（不审）：副本内 `JdbcStaffRepository.java`、`StaffRegistrationService.java`、`StaffApprovalService.java`、`StaffLockerService.java`（第 5 段）、`SupervisorReviewService.java`（第 9 段）、`V400–V409`（第 10 段）、第 11 段回归测试；main 当前同名文件。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/access/staff/StaffService.java | 1553 | 97552 | 605e857533d44e16a0bb0db08b03488fc78e2f8aea13f6fc0ae27b3f53c28371 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java | 33 | 1167 | bd9d8aabf0003874ace4cff5bd86b21886e5190563a45d56f30d2ee4a6d3f27a | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/dto/StaffEmploymentStatusRequest.java | 4 | 158 | 6f6298f7793df2586cf3a2d4ee0b31e7e50035b4fbe4a1cb285ea1c3abf556d8 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/dto/StaffNumericSettingRequest.java | 4 | 182 | 484d62c04e1be7b2bd7f9a7d739efe886dde6f67be8619632b6f70dba0afd4c5 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/dto/StaffPenaltyCapRequest.java | 9 | 442 | ab6931adbb43e7ca9fe40709a31c1b184aa6d785013eb5cbb628350dd43787c9 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java | 19 | 503 | c258e6cc7f201e1299451b2744585547b487984cea187415d2dad5a2f5aa6b5f | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/dto/StaffSelfRegistrationRequest.java | 5 | 227 | a601974a2fdf31b31da2d1aa0722c0338af7e6b13a0db6193d142a0baab58839 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchResult.java | 13 | 561 | 8962a3d6620e6463960a6cf79a46dcdbec62c0f52640e8b4a977567b454ece01 | 全读 |
| backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java | 24 | 763 | acf512fc6c18a6e0f6504aba43d80492bcdf5b9060b49b391157eb04b882c9b2 | 全读 |

## 2. 可执行证据原文（/tmp/u06_b1_run.log）

```
START=2026-09-09T20:57:54Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
backend/src/main/java/com/complex/platform/access/staff/StaffService.java | 1553 | 97552 | 605e857533d44e16a0bb0db08b03488fc78e2f8aea13f6fc0ae27b3f53c28371
backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java | 33 | 1167 | bd9d8aabf0003874ace4cff5bd86b21886e5190563a45d56f30d2ee4a6d3f27a
backend/src/main/java/com/complex/platform/access/staff/dto/StaffEmploymentStatusRequest.java | 4 | 158 | 6f6298f7793df2586cf3a2d4ee0b31e7e50035b4fbe4a1cb285ea1c3abf556d8
backend/src/main/java/com/complex/platform/access/staff/dto/StaffNumericSettingRequest.java | 4 | 182 | 484d62c04e1be7b2bd7f9a7d739efe886dde6f67be8619632b6f70dba0afd4c5
backend/src/main/java/com/complex/platform/access/staff/dto/StaffPenaltyCapRequest.java | 9 | 442 | ab6931adbb43e7ca9fe40709a31c1b184aa6d785013eb5cbb628350dd43787c9
backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java | 19 | 503 | c258e6cc7f201e1299451b2744585547b487984cea187415d2dad5a2f5aa6b5f
backend/src/main/java/com/complex/platform/access/staff/dto/StaffSelfRegistrationRequest.java | 5 | 227 | a601974a2fdf31b31da2d1aa0722c0338af7e6b13a0db6193d142a0baab58839
backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchResult.java | 13 | 561 | 8962a3d6620e6463960a6cf79a46dcdbec62c0f52640e8b4a977567b454ece01
backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java | 24 | 763 | acf512fc6c18a6e0f6504aba43d80492bcdf5b9060b49b391157eb04b882c9b2
=== manifest entries ===
backend/src/main/java/com/complex/platform/access/staff/StaffService.java 1553 97552 605e857533d44e16a0bb0db08b03488fc78e2f8aea13f6fc0ae27b3f53c28371
backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java 33 1167 bd9d8aabf0003874ace4cff5bd86b21886e5190563a45d56f30d2ee4a6d3f27a
backend/src/main/java/com/complex/platform/access/staff/dto/StaffEmploymentStatusRequest.java 4 158 6f6298f7793df2586cf3a2d4ee0b31e7e50035b4fbe4a1cb285ea1c3abf556d8
backend/src/main/java/com/complex/platform/access/staff/dto/StaffNumericSettingRequest.java 4 182 484d62c04e1be7b2bd7f9a7d739efe886dde6f67be8619632b6f70dba0afd4c5
backend/src/main/java/com/complex/platform/access/staff/dto/StaffPenaltyCapRequest.java 9 442 ab6931adbb43e7ca9fe40709a31c1b184aa6d785013eb5cbb628350dd43787c9
backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java 19 503 c258e6cc7f201e1299451b2744585547b487984cea187415d2dad5a2f5aa6b5f
backend/src/main/java/com/complex/platform/access/staff/dto/StaffSelfRegistrationRequest.java 5 227 a601974a2fdf31b31da2d1aa0722c0338af7e6b13a0db6193d142a0baab58839
backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchResult.java 13 561 8962a3d6620e6463960a6cf79a46dcdbec62c0f52640e8b4a977567b454ece01
backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java 24 763 acf512fc6c18a6e0f6504aba43d80492bcdf5b9060b49b391157eb04b882c9b2
=== baseline 242f4872 -> unpublished tree e28dad05 (this segment) ===
 .../platform/access/staff/StaffService.java        | 430 +++++++++++++++------
 .../platform/access/staff/StaffShiftRecord.java    |  15 +-
 .../staff/dto/StaffEmploymentStatusRequest.java    |   4 +
 .../staff/dto/StaffNumericSettingRequest.java      |   4 +
 .../access/staff/dto/StaffPenaltyCapRequest.java   |   9 +
 .../access/staff/dto/StaffRegistrationRequest.java |  10 +-
 .../staff/dto/StaffSelfRegistrationRequest.java    |   5 +
 .../access/staff/dto/StaffShiftBatchResult.java    |  18 +-
 .../access/staff/dto/StaffShiftRequest.java        |  11 +-
 9 files changed, 364 insertions(+), 142 deletions(-)
=== main current vs unpublished copy + main_vs_baseline blob ids ===
backend/src/main/java/com/complex/platform/access/staff/StaffService.java main_sha=90c0702a6e89e2ed4b5ed6b3daef96d90070644b2149cef73bbd6f4c375fdd81 unpublished_sha=605e857533d44e16a0bb0db08b03488fc78e2f8aea13f6fc0ae27b3f53c28371 diff_lines=430 baseline=4b601fb3aa head=4b601fb3aa main_unchanged_since_baseline=yes
backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java main_sha=19dda39f0c406bc194a1d7c81f98a9ba03dace40c48a21600c3285b0d8932f62 unpublished_sha=bd9d8aabf0003874ace4cff5bd86b21886e5190563a45d56f30d2ee4a6d3f27a diff_lines=15 baseline=772ab6507b head=772ab6507b main_unchanged_since_baseline=yes
backend/src/main/java/com/complex/platform/access/staff/dto/StaffEmploymentStatusRequest.java main_exists=no
backend/src/main/java/com/complex/platform/access/staff/dto/StaffNumericSettingRequest.java main_exists=no
backend/src/main/java/com/complex/platform/access/staff/dto/StaffPenaltyCapRequest.java main_exists=no
backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java main_sha=ca988b3abcf2d7abfed6c565b612a61db4584163196ddfefeb1b1b2b1abf3bbd unpublished_sha=c258e6cc7f201e1299451b2744585547b487984cea187415d2dad5a2f5aa6b5f diff_lines=10 baseline=dd50b7e230 head=dd50b7e230 main_unchanged_since_baseline=yes
backend/src/main/java/com/complex/platform/access/staff/dto/StaffSelfRegistrationRequest.java main_exists=no
backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchResult.java main_sha=98aa623565213b161aabaffb1108f8bba8eaf647643c181367649dd3407a2bb2 unpublished_sha=8962a3d6620e6463960a6cf79a46dcdbec62c0f52640e8b4a977567b454ece01 diff_lines=18 baseline=657a59e961 head=657a59e961 main_unchanged_since_baseline=yes
backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java main_sha=2ad0cf9ffa015045d1b328d3098f6cc550bd7c9843d08cddba78b66832668a73 unpublished_sha=acf512fc6c18a6e0f6504aba43d80492bcdf5b9060b49b391157eb04b882c9b2 diff_lines=11 baseline=c1f2b94231 head=c1f2b94231 main_unchanged_since_baseline=yes
=== probes: shift visibility scope keys (writer vs reader) ===
766:    private String staffSettingScope() {
769:                ? "company:" + registrations.actorCompany(actor) : "merchant:" + actor.merchantId();
774:        return repository.numericSetting("shift_visible_days", staffSettingScope(), 7);
781:        String scope = staffSettingScope();
828:        String scope = profile.merchantId() == null ? "company:" + (profile.companyId() == null ? profile.projectId() : profile.companyId()) : "merchant:" + profile.merchantId();
829:        int days = repository.numericSetting("shift_visible_days", scope, 7).value();
59:        String companyId = requireText(request.companyId(), "所属公司");
60:        if (companies().stream().noneMatch(c -> c.id().equals(companyId))) {
66:            if (!merchant.selfOperated() || !companyId.equals(companyChain(merchant).getFirst().id())) {
72:            var profile = new StaffProfileRecord("staff_"+UUID.randomUUID(),projectOf(companyId),merchantId,null,name,
74:                    .withCompanyRoute(companyId,"company_self");
77:                    Map.of("company_id",companyId,"registration_source","company_self"));
82:    String actorCompany(AccessActor actor) {
83:        String ownCompany = staff.findProfileByUserId(actor.id()).map(StaffProfileRecord::companyId).orElse(null);
=== probes: punch rules / earliest / device owner ===
846:        if (method == null || !java.util.Set.of("nfc", "qr", "charging_disconnect").contains(method)) {
849:        if (!"charging_disconnect".equals(method) && trimToNull(request.tagRef()) == null) {
852:        if (("charging_disconnect".equals(method) || "nfc".equals(method)) && !"check_in".equals(request.punchType())) {
858:                .orElseThrow(() -> ApiException.forbidden("WORK_PHONE_REQUIRED", "须从已验证的本人工作手机记录考勤"));
861:                .orElseThrow(() -> ApiException.forbidden("WORK_PHONE_REQUIRED", "工作手机当前不可用"));
865:            throw ApiException.forbidden("WORK_PHONE_OWNER_REQUIRED", "只能用分配给本人的工作手机记录考勤");
873:        repository.lockAttendanceUser(actor.id());
880:            var first = repository.findFirstCheckIn(actor.id(), shift == null ? null : shift.id(),
=== probes: shift approval / revision / reason ===
622:                () -> staffApprovals.submitShifts(List.of(draftShift(request)), request.idempotencyKey()).shifts().getFirst(),
634:                ? repository.findShiftByUserStart(request.userId().trim(), request.startAt()).orElse(null)
637:            throw ApiException.conflict("SHIFT_STARTED", "班次已经开始，不能改写排班；临时顶班须记录顶班安排");
641:            throw ApiException.forbidden("SHIFT_AUTHOR_REQUIRED", "排班修改须由原排班负责人办理，员工应先向负责人申请");
643:        if (previous != null) requireReason(request.note());
715:        return staffApprovals.submitShifts(List.copyOf(saved), request.idempotencyKey());
745:    public List<StaffApprovalService.History> shiftHistory(String id) {
=== probes: registration review SoD / company / master ===
128:        String approvingCompany = registrations.merchantApprovingCompany(request.merchantId());
167:                        repository.recordRegistrationInitiator(saved.id(), authorizationService.currentActor().id());
199:                                && !authorizationService.isMasterAuthority(actor)) {
200:                            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN", "不能审批自己发起的员工注册");
202:                        if (!authorizationService.isMasterAuthority(actor)
206:                            throw ApiException.forbidden("COMPANY_APPROVAL_REQUIRED", "须由该申请所属公司或商户对应域公司审批");
228:                            // 既有缺口修复(2026-07-19):审批通过即为新员工分配"员工自助"角色 role_staff_self,
231:                            accessRepository.assignUserRole(created.id(), "role_staff_self");
388:        if (authorizationService.isMasterAuthority(authorizationService.currentActor())) {
400:        if (!authorizationService.isMasterAuthority(authorizationService.currentActor())) {
=== probes: permission guards changed ===
123:    @GuardedAction(permission = "staff.profile.submit")
182:    @GuardedAction(permission = "staff.profile.review")
275:    @GuardedAction(permission = "staff.profile.view")
281:    @GuardedAction(permission = "staff.profile.view")
311:    @GuardedAction(permission = "staff.profile.submit")
337:    @GuardedAction(permission = "staff.profile.review")
355:    @GuardedAction(permission = "staff.profile.review")
372:    @GuardedAction(permission = "staff.profile.review")
396:    @GuardedAction(permission = "staff.profile.approve")
431:    @GuardedAction(permission = "staff.device.bind")
485:    @GuardedAction(permission = "staff.device.review")
579:    @GuardedAction(permission = "staff.device.revoke")
936:    @GuardedAction(permission = "staff.notice.view")
944:    @GuardedAction(permission = "staff.notice.view")
29:     @GuardedAction(permission = "staff.profile.submit")
143:-    @GuardedAction(permission = "staff.profile.review")
144:+    @GuardedAction(permission = "staff.profile.submit")
166:+    @GuardedAction(permission = "staff.profile.review")
184:     @GuardedAction(permission = "staff.profile.review")
559:     @GuardedAction(permission = "staff.notice.view")
=== probes: employment states / numeric setting bounds / penalty cap ===
343:                || !java.util.Set.of("pending_entry", "employed", "trial", "suspended", "departed")
780:        StaffNumericSetting value = requireNumericSetting(request, 1, Integer.MAX_VALUE);
797:        var value = requireNumericSetting(request == null ? null : new com.complex.platform.access.staff.dto.StaffNumericSettingRequest(
798:                request.value(), request.minValue(), request.maxValue(), request.reason()), 0, 10000);
800:                () -> staffApprovals.submitPenaltyCap(value, request),
810:    private StaffNumericSetting requireNumericSetting(com.complex.platform.access.staff.dto.StaffNumericSettingRequest request,
=== probes: dead code / unused ===
1540:    private String slug(String value) {
=== DTO field names vs Kotlin models (U02) ===
public record StaffSelfRegistrationRequest(String idempotencyKey, String companyId, String merchantId,
        String name, String phone, String idDoc) {}
/** 排班及修改申请；修改时间时显式指定原班次，须审批后发布。 */
public record StaffShiftRequest(
        String idempotencyKey,
        String userId,
        LocalDate workDate,
        Instant startAt,
        Instant endAt,
        List<String> pointSpaceIds,
        String note,
        String shiftId
) {
    public StaffShiftRequest(String idempotencyKey, String userId, LocalDate workDate, Instant startAt,
            Instant endAt, List<String> pointSpaceIds, String note) {
        this(idempotencyKey, userId, workDate, startAt, endAt, pointSpaceIds, note, null);
    }
}
=== HTML greps (main 正本) ===
docs/business-system-map-for-operations-v0.1.html:570:            <div class="explain-item"><strong>删除（走审批·
docs/business-system-map-for-operations-v0.1.html:1443:                <li>主管排班：排到几点几分，可指定
docs/business-system-map-for-operations-v0.1.html:1444:                <li>员工看自己未来若干天的排班，可
docs/business-system-map-for-operations-v0.1.html:1445:                <li>考勤打卡从领取工作手机开始算：
docs/business-system-map-for-operations-v0.1.html:1763:                <li>有最高权限的管理者：审批分红单
docs/platform-governance-map-for-operations-v0.1.html:660:                <li>高危授权审批（已确认）：以下
docs/business-enum-quick-reference-v0.1.html:240:    <tr><td class="k">员工状态</td><td>待入职、在职、试岗�
=== isolated compile evidence (/tmp/u-overlay/backend) ===
START=2026-09-09T18:37:14Z
MVN_EXIT=0
END=2026-09-09T18:37:19Z
EmployeeNoticeAcknowledgeRequest.class
PayrollTaxBracketRequest.class
StaffAttendancePunchRequest.class
StaffCommissionRequest.class
StaffDeviceBindRequest.class
StaffDeviceIntegrityReportRequest.class
StaffDeviceRebindDecisionRequest.class
StaffDeviceRevokeRequest.class
StaffEmploymentStatusRequest.class
StaffLeaveRequest.class
StaffLeaveReviewRequest.class
StaffNumericSettingRequest.class
StaffPayrollAdjustmentRequest.class
StaffPayrollBaseRequest.class
StaffPayrollCloseRequest.class
StaffPayrollPayRequest.class
StaffPenaltyCapRequest.class
StaffProfileArchiveRequest.class
StaffProfileDeleteApprovalRequest.class
StaffProfileDeleteDecision.class
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

- 对象口径：未发布提交 03acdb98 的只读输入副本第 6 段（9 文件 1664 行；`StaffEmploymentStatusRequest`、`StaffNumericSettingRequest`、`StaffPenaltyCapRequest`、`StaffSelfRegistrationRequest` 为 main 无对应的新增文件），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段文件的真实改动 = `git diff 242f4872 e28dad05 -- <本段路径>`（§2 diffstat），main 当前版本仅作分叉对照。
- 第一性原理线：从 HTML 反推员工领域服务应当做什么——BSM L1443（排班上一级审批、开始锁定、修改由排班负责人填写原因再审、只能追加）、BSM L1444（可见天数按经营主体可配、出厂 7 天、须同时配置上下限）、BSM L1445/L1446（领取即上班、最早一次、严格无宽限）、BSM L1447（考勤月历六态）、BSM L1448/L1450（请假四类主管审批；只有事假扣薪）、BSM L1458（扣除上限三审+顶层确认）、PGM L578/BSM L1433（注册两路径、对应公司审批）、PGM L870–871（脱敏）、BEQR L240（员工状态五值）；逐一对照服务方法。
- 顺从式线：52 项判据逐项（§5）。
- 对抗式线：可见天数「写入键」与「读取键」是否同一推导；审批闭环是否可被发起人自批；打卡是否可用他人工作手机；排班修改能否绕过原负责人/原因/已开始锁定；月结/列表作用域（项目/商户/公司）是否漏；DTO 与客户端字段（第 2 段）是否一致；死代码与注入风格。
- 只读依赖追流（不审、不计入本段结论）：副本 `JdbcStaffRepository`（`numericSetting`/`findFirstCheckIn`/`closePayrollMonth`）、`StaffRegistrationService.actorCompany`、`StaffApprovalService`、`StaffLockerService`（第 5 段）、`SupervisorReviewService` 策略（第 9 段）、`V402/V404/V405/V406/V407`（第 10 段）、第 11 段回归测试；main 当前同名文件。
- 9 文件全文逐行读完（§1）；与基线的逐文件 diff 逐块读完（§2 diffstat）。可执行验证：`/tmp/u-overlay/backend` 离线 `mvn -o compile` 退出 0（§2）；DB 行为测试（第 11 段）需在 complex_test 上应用 V400–V409，席甲不对共享测试库做写操作，未跑（如实记）。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- `StaffService.java`、`StaffShiftRecord.java`、`StaffRegistrationRequest.java`、`StaffShiftBatchResult.java`、`StaffShiftRequest.java` main 自基线以来是否有改动见 §2 blob 对照（M009–M012 未触及 `access/staff`）；四个新增 DTO main 无对应。
- 跨段依赖同第 5 段：编译依赖第 5/9/10 段；`StaffRegistrationRequest` 字段由 `phoneHash/idDocHash` 改为 `phone/idDoc`，与第 2 段 Kotlin 模型同名（U02 §5 已核）。

## 5. 52 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 审批流、可见天数、领取即上班、员工状态、封顶申请均有 HTML 上位 |
| GEN-002 可执行正确性 | 不通过 | 可见天数配置写入键与员工读取键推导不同，配置可能永不生效（CX-U06-001） |
| GEN-003 测试有效性 | 通过（登记） | 第 11 段回归覆盖审批/打卡/状态/封顶/作用域；可见天数用例只测 `merchant:` 键（`M010RegressionTests:264-277`），未测 `company:` 键的写读一致（随 CX-U06-001） |
| GEN-004 信任边界 | 通过 | 打卡设备由服务端派生身份决定；请求体不带 userId 的自助接口按 token 派生 |
| GEN-005 授权 | 通过（登记） | SoD（发起人不得自批）、公司归属、最高权限删除、本人设备/通知；`archiveProfile` 守卫 review→submit（OBS-1）；`companyId()==null` 放行归 U05-010（OBS-9） |
| GEN-006 数据完整性 | 通过 | 排班草稿→审批→发布；已开始锁定；最早一次去重 |
| GEN-007 并发幂等 | 通过（登记） | 排班/批量/封顶/打卡均走幂等服务；`setShiftVisibility`/`setEmploymentStatus` 无幂等键归 U05-011（OBS-2） |
| GEN-008 接口一致性 | 不通过 | 同 CX-U06-001（同一配置两种键推导）；DTO 与第 2 段模型一致 |
| GEN-009 错误处理 | 通过 | 校验错误均中文业务码 |
| GEN-010 安全 | 通过 | 打卡须已验签本人工作手机；自助接口只返本人；员工自助不能改销售岗 |
| GEN-011 审计证据 | 通过 | 各写路径记审计事件（状态变更含 before/after/reason） |
| GEN-012 资源 | 通过 | 批量排班 ≤500；草稿逐条 FOR UPDATE 在上限内可接受 |
| GEN-013 构建 | 通过 | 隔离 `mvn -o compile` 退出 0（§2） |
| GEN-014 迁移 | 不适用 | 本段无迁移 |
| GEN-015 界面 | 不适用 | 无界面 |
| GEN-016 文档 | 通过（登记） | `upsertShift` 注释已改为审批语义；`slug()` 死代码（OBS-3） |
| GEN-017 分类 | 通过 | 四分类见 §6 |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、§2 命令 |
| BEQR-157 员工状态 | 通过 | `setEmploymentStatus` 五值闭集 + 事由 |
| BEQR-182 考勤日结果 | 通过 | 月视图六态按班次结束日 |
| BEQR-183 打卡方式 | 通过 | nfc/qr/charging_disconnect；nfc/充电只记上班 |
| BEQR-184 请假类型 | 通过 | 四类 |
| BEQR-185 请假单状态 | 通过 | 三态 |
| BEQR-186 薪资单构成 | 通过 | 月结分列（基数问题在第 5 段 CX-U05-001） |
| BEQR-187 员工提成发放 | 通过 | 未改 |
| BSM-001/003/009/028/029 | 通过 | 无冲突 |
| BSM-073 | 不适用 | 无实现面 |
| BSM-084 员工手机端 | 通过（登记） | 自助排班/考勤/请假/薪资只返本人；可见天数生效性见 CX-U06-001 |
| BSM-085 日常工作 | 通过 | 无冲突 |
| BSM-086 排班考勤请假 | 不通过 | L1444 可见天数配置在员工端可能不生效（CX-U06-001）；其余（审批、锁定、原因、最早一次、六态、请假审批）一致 |
| BSM-087 我的薪资 | 通过 | 月结后可见 |
| BSM-088 | 通过 | 无冲突 |
| BSM-114 各类入口 | 通过 | 公司/商户/项目作用域一致应用于列表与写路径 |
| BSM-115 | 通过 | 无实现面 |
| BSM-120 已确认规则 | 不通过 | L1444（CX-U06-001） |
| PGM-001 | 通过 | 中文错误文案 |
| PGM-008 | 通过 | 无冲突 |
| PGM-022 空间和点位 | 通过 | 打卡带点位 |
| PGM-024 | 不适用 | 无实现面 |
| PGM-026 | 通过 | 无冲突 |
| PGM-032/033 员工和手机 | 通过（登记） | 注册两路径由对应公司审批、发起人不自批；设备证明缺口在第 5 段 CX-U05-002 |
| PGM-034 | 不适用 | 无实现面 |
| PGM-035 | 通过 | 无冲突 |
| PGM-036 设备管理 | 通过 | 打卡须分配给本人的已绑定工作手机 |
| PGM-048 商户侧 | 通过 | 商户代提交按域公司审批 |
| PGM-059 个人信息保护 | 通过 | 控制器层脱敏（第 5 段） |
| PGM-066 | 通过 | 无冲突 |

## 6. 本次 Findings（临时 ID CX-U06-00N；正式 ID 由 B2 分配）

### CX-U06-001 · 技术性 · 发布前须修：是

- 位置：`StaffService.java:766-770`（`staffSettingScope`：无商户的管理员 → `"company:" + registrations.actorCompany(actor)`），`:778-786`（`setShiftVisibility` 以该键写入），`:822-831`（`listMyShifts`：员工 → `profile.merchantId()==null ? "company:" + (companyId ?? projectId) : "merchant:" + merchantId` 读取）；`StaffRegistrationService.java:82-86`（`actorCompany`：本人档案 companyId → 岗位 organizationId → actor.organizationId）。
- HTML 原句：BSM L1444「员工看自己未来若干天的排班，可见天数由后台配置（业务参数，按经营主体可配……出厂默认 7 天，须同时配置上下限）」。
- 判据：GEN-002、GEN-008、BSM-086、BSM-120；依据类型：代码事实（HTML 要求配置对员工生效）。
- 业务影响：同一参数由两种不同推导得出作用域键：管理员写入用「本人档案公司 / 岗位组织 / 令牌组织」，员工读取用「档案 companyId，缺省项目 id」。基线既有员工（`company_id` 为空）读 `company:<projectId>`，而配置者若经岗位/令牌解析到的组织不是项目公司 id，则写入的是另一把键——员工端永远回落出厂 7 天，配置「看起来保存成功」却不生效。第 11 段回归只覆盖 `merchant:` 键（`M010RegressionTests:264-277`），`company:` 键写读一致性无测试。
- 最小反例：管理员（岗位组织＝管理公司 id）调用 `POST /settings/shift-visible-days` value=3 → 写 `company:<mgmt_id>`；无 companyId 的员工 `GET /shifts/mine` → 读 `company:<project_id>` → 仍 7 天。
- 无业务发明理由：只需两处用同一函数推导经营主体键。
- 工程缺陷：是。建议登记：是。最小处置（给发布者）：抽一个「员工所属经营主体键」函数供写读共用（商户→merchant:，公司员工→company:<档案 companyId，缺省按 projectOf 推导>），并补 `company:` 键的写读回归；不在本轮修复。

## 7. 观察（登记、不入队）

- OBS-1：`archiveProfile` 守卫由 `staff.profile.review` 放宽为 `staff.profile.submit`（`:311`，第 3 段页面据此显示存档按钮）；HTML BSM-003 未细到此层，登记。
- OBS-2：`setShiftVisibility`（`:778-786`）与 `setEmploymentStatus`（`:123-140`）无幂等执行、控制器不 `requireIdempotencyKey`（第 5 段 `StaffController:95-98/:203-206`）——已在第 5 段候选 U05-011 入队，本段登记不重复。
- OBS-3：`slug()`（`:1540-1545`）不再被调用（凭证 id 改 UUID）——死代码。
- OBS-4：`listMyShifts` 按 `work_date` 取班（`:830`），月视图/月结按 `end_at` 归期；跨零点班在「可见天数」窗口边缘的归属与月视图不同（登记）。
- OBS-5：`requireUserInScope` 对带 companyId 的目标先走公司校验并 `return`，跳过其后的项目作用域校验（`:1138-1159`）——公司校验已包含项目归属（`projectOf`），登记。
- OBS-6：`StaffService` 以字段 `@Autowired` 注入四个协作者（`:69-76`），构造器注入与字段注入并存（风格）。
- OBS-7：`reviewRegistration` 审批通过时创建用户名 `staff_<profileId>` 并分配 `role_staff_self` 与职级（基线既有流程，本材料补职级分配）。
- OBS-8：第 5 段候选 U05-001（自营分流读独立布尔，`merchantApprovingCompany` 由本段 `submitRegistration` 调用）、U05-002（事假扣薪基数，本段 `closePayroll` 调用）、U05-003（公开注册设备标识）、U05-006（柜机双身份）、U05-009（`companyChain.getFirst()`）由本段调用面触发，不重复登记。
- OBS-9：`listProfiles`（`:204`）与 `requireUserInScope`（`:288`）对 `companyId()==null` 的档案放行进公司范围，与第 5 段仓储 `COALESCE(company_id, project_id)` 同根（U05-010 候选：服务侧 null 放行为同一处置的一部分），本段登记不重复入队。
- 技能：`using-superpowers`、`verification-before-completion`（编译实跑附 §2；DB 行为测试未跑如实记）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。9 文件全文三线审计、52 项判据逐项核对；发现 1 项技术性须修（CX-U06-001）、9 项观察（其中 OBS-2/8/9 引用第 5 段候选不重复入队）；无添加项；无 HTML 修改申请；隔离编译绿；未修源、未提交/推送、未写任何 complex_test* 库。本报告落盘前未读取 `U06-b1-grok-raw.md` 任何正文。
---

# U06 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.54「当前 U06 B2-r1 整批候选」U06-001…011；modules/U06-b1-grok-raw.md V0.01（46165bytes SHAc6733dbfb2e1e09c5d46afd10ba51cf2de29468e5cfc82950aad312e089e0ec0）；本文件 B1 段（32823bytes SHA2de4492e61dfc2b378f7d6b39a74fc655295e92a085c83c62c224fe407c3efed）。2026-09-09T21:52:11Z
- 席甲 B2 核实（只读，见 ledger V2.59 U06 节）：`git show 242f4872:…StaffService.java` 对副本逐项：`missing_checkout` 基线 1/副本 1；`is required` 基线 4/副本 1；`initial_password`、`（approved）` 基线各 1；撤销/销假基线与副本均 0；`SHIFT_STARTED`/顶班 基线 0、副本 1；HTML BEQR L279/L282、BSM L1443 原句已逐字核对；Grok only-9 编译 EXIT 1 与席甲 §4 分叉结论一致。

## 勘误（席甲 B1 段）

- 席甲 B1 OBS-8 引用第 5 段时写作「候选」，定稿后应读作 U05-001/002/003/006/009；B1 未把 `SHIFT_STARTED` 文案与顶班入口缺失、请假撤销/销假缺失单列（前者为本材料新增冲突，B2 采纳 Grok GR-003；后者为 main 既有）。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 写读两把推导经源码 `:766-770`/`:822-831` 核实；`listMyShifts` 回落 projectId 并入不改原意；共用推导函数最懒 |
| 002 | PASS | DTO 六字段无设备标识；同根 U05-003 只引用处置 |
| 003 | PASS | `:128` 调用面；同根 U05-001，本段不改 |
| 004 | PASS | 基线 0 命中，本材料新增锁定并自述顶班却无入口；HTML L1443 顶班明文；补入口不越界（HTML 已写） |
| 005 | PASS | GET 缺行 `(7,null,null)`；同根 U05-005 |
| 006 | PASS | `:288/:1067/:828` null 放行与 projectId 冒充；同根 U05-010 |
| 007 | PASS | only-9 编译 EXIT 1；五文件 main＝基线；同根 U05-008 |
| 008 | PASS | 三处报错基线既有且材料已修 3 处；main 既有归 M054 |
| 009 | PASS | 撤销/销假基线与副本均无；main 既有归 M054 |
| 010 | PASS | `missing_checkout` 基线 `:1330` 已有；main 既有添加归 M054，不弹窗 |
| 011 | PASS | 观察归并未改写双方原意 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 5 条，定稿以此为准）

1. 004 HTML 行号以 BSM L1443 为准（Grok B1 写 L1441 为标题行）。
2. 010「未加重」读作：月视图归期改按 BSM L1449 班次结束时点、BEQR L279 同日多班次并列——函数虽改写，第六态本身为基线既有且未加码。
3. 001 处置写明共用函数入参 `(merchantId, companyId)`：写侧＝配置者所属经营主体，读侧＝员工档案，不得回落 `projectId`。
4. 008 可并列 `:843` 夹注（check_in/check_out），仍归 M054。
5. 002 锚点以 PGM L579 / BSM L935 为准（比 Grok B1 的 L576 / L1957 更贴句子）。

## 结论

席甲 B2-r1 票：**PASS**（候选 U06-001…011 成立；发布前须修「是」7 / 「否」4；添加 1（main 既有，归 M054，不弹窗）；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 68129bytes SHAa672e989b192271664c405ec28706672f97b9c50db1a67deb789a62411a1e433，前 46165bytes 原字节不变）。
