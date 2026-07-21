package com.complex.platform.access.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.staff.dto.EmployeeNoticeAcknowledgeRequest;
import com.complex.platform.access.staff.dto.StaffDeviceBindRequest;
import com.complex.platform.access.staff.dto.StaffDeviceIntegrityReportRequest;
import com.complex.platform.access.staff.dto.StaffDeviceRebindDecisionRequest;
import com.complex.platform.access.staff.dto.StaffDeviceRevokeRequest;
import com.complex.platform.access.staff.dto.StaffRegistrationRequest;
import com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.MigrationVersions;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * R5 T1 员工三表集成测试：迁移落地 + 状态机闭环（注册审批 / 换机 supersedes 唯一 active /
 * 完整性失效）+ 两套凭证实体隔离 + 商户 scope 隔离 + CHECK 枚举约束 + 仅存不可逆哈希。
 * 走真实 Postgres，@Transactional 用例结束回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class StaffJdbcIntegrationTests {

    @Autowired
    private StaffService service;

    @Autowired
    private JdbcStaffRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.complex.platform.access.AccessRepository accessRepository;

    @BeforeEach
    void setUp() {
        authenticateAsPlatformAdmin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void migrationCreatesStaffThreeTablesAtLatestVersionAndSeedsPermissions() {
        assertThat(regclass("staff_profiles")).isEqualTo("staff_profiles");
        assertThat(regclass("staff_device_credentials")).isEqualTo("staff_device_credentials");
        assertThat(regclass("employee_notices")).isEqualTo("employee_notices");
        assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());

        assertThat(permissionCodes()).contains(
                "staff.profile.submit", "staff.profile.review", "staff.profile.view",
                "staff.device.bind", "staff.device.review", "staff.device.revoke",
                "staff.notice.view");
    }

    @Test
    void registrationApproveGeneratesUserIdAndApprovalNotice() {
        StaffProfileRecord submitted = submitRegistration(null);
        assertThat(submitted.registrationStatus()).isEqualTo("submitted");
        assertThat(submitted.userId()).isNull();

        StaffProfileRecord approved = service.reviewRegistration(submitted.id(),
                new StaffRegistrationReviewRequest(key(), null, "approve", null, "Init-Pass-Staff-1"));

        assertThat(approved.registrationStatus()).isEqualTo("approved");
        assertThat(approved.userId()).isNotBlank();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM access_users WHERE id = ?", Integer.class, approved.userId()))
                .isEqualTo(1);
        // approval_result 通知已下发（有写入方）。
        assertThat(repository.listNotices(null)).anySatisfy(n -> {
            assertThat(n.staffProfileId()).isEqualTo(submitted.id());
            assertThat(n.noticeType()).isEqualTo("approval_result");
            assertThat(n.createdBy()).isNotBlank();
        });
    }

    @Test
    void registrationApproveRequiresInitialPassword() {
        StaffProfileRecord submitted = submitRegistration(null);
        assertThatThrownBy(() -> service.reviewRegistration(submitted.id(),
                new StaffRegistrationReviewRequest(key(), null, "approve", null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("初始登录密码");
    }

    @Test
    void registrationRejectRequiresReason() {
        StaffProfileRecord submitted = submitRegistration(null);

        assertThatThrownBy(() -> service.reviewRegistration(submitted.id(),
                new StaffRegistrationReviewRequest(key(), null, "reject", null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reject_reason");

        StaffProfileRecord rejected = service.reviewRegistration(submitted.id(),
                new StaffRegistrationReviewRequest(key(), null, "reject", "材料不全", null));
        assertThat(rejected.registrationStatus()).isEqualTo("rejected");
        assertThat(rejected.rejectReason()).isEqualTo("材料不全");
    }

    @Test
    void bindCredentialRejectsPlaceholderKeystoreValues() {
        // 1.9：占位设备凭证被拒——须为 Android Keystore 真实公钥/指纹。
        StaffProfileRecord staff = approvedProfile(null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.bindCredential(new StaffDeviceBindRequest(
                        key(), null, staff.id(), "PLACEHOLDER_PUBLIC_KEY", "PLACEHOLDER_FINGERPRINT_HASH")))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("DEVICE_CREDENTIAL_PLACEHOLDER"));
    }

    @Test
    void deviceRebindUnbindsOldThenActivatesNewKeepingExactlyOneActive() {
        StaffProfileRecord staff = approvedProfile(null);

        // 首次绑定 → pending_review → 审批 active。
        StaffDeviceCredentialRecord first = service.bindCredential(new StaffDeviceBindRequest(
                key(), null, staff.id(), "pubkey-1", "fp-hash-1"));
        assertThat(first.bindStatus()).isEqualTo("pending_review");
        assertThat(first.supersedesCredentialId()).isNull();
        StaffDeviceCredentialRecord firstActive = service.approveRebind(first.id(),
                new StaffDeviceRebindDecisionRequest(key(), null));
        assertThat(firstActive.bindStatus()).isEqualTo("active");

        // 换机：新设备绑定 → pending_review + supersedes 指向旧 active。
        StaffDeviceCredentialRecord second = service.bindCredential(new StaffDeviceBindRequest(
                key(), null, staff.id(), "pubkey-2", "fp-hash-2"));
        assertThat(second.bindStatus()).isEqualTo("pending_review");
        assertThat(second.supersedesCredentialId()).isEqualTo(first.id());

        // 审批换机 → 新 active、旧 superseded。唯一 active 始终成立。
        StaffDeviceCredentialRecord secondActive = service.approveRebind(second.id(),
                new StaffDeviceRebindDecisionRequest(key(), null));
        assertThat(secondActive.bindStatus()).isEqualTo("active");
        assertThat(repository.findCredentialById(first.id())).get()
                .extracting(StaffDeviceCredentialRecord::bindStatus).isEqualTo("superseded");
        assertThat(repository.findActiveCredential(staff.id())).get()
                .extracting(StaffDeviceCredentialRecord::id).isEqualTo(second.id());
        long activeCount = repository.listCredentials(staff.id()).stream()
                .filter(c -> "active".equals(c.bindStatus())).count();
        assertThat(activeCount).isEqualTo(1);
    }

    @Test
    void uniqueActiveIndexRejectsTwoActiveCredentialsForSameStaff() {
        StaffProfileRecord staff = approvedProfile(null);
        Instant now = Instant.now();
        repository.saveCredential(new StaffDeviceCredentialRecord(
                "scred_a_" + key(), staff.id(), "pk-a", "fp-a", "active", null, "trusted",
                null, now, null, now, now));

        assertThatThrownBy(() -> repository.saveCredential(new StaffDeviceCredentialRecord(
                "scred_b_" + key(), staff.id(), "pk-b", "fp-b", "active", null, "trusted",
                null, now, null, now, now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void integrityRootDetectedRevokesCredentialAndNotifies() {
        StaffProfileRecord staff = approvedProfile(null);
        StaffDeviceCredentialRecord cred = service.bindCredential(new StaffDeviceBindRequest(
                key(), null, staff.id(), "pubkey-r", "fp-hash-r"));

        StaffDeviceCredentialRecord revoked = service.reportIntegrity(cred.id(),
                new StaffDeviceIntegrityReportRequest(key(), null, "root_detected"));

        assertThat(revoked.bindStatus()).isEqualTo("revoked");
        assertThat(revoked.integrityStatus()).isEqualTo("root_detected");
        assertThat(revoked.revokedAt()).isNotNull();
        assertThat(repository.listNotices(null)).anySatisfy(n ->
                assertThat(n.noticeType()).isEqualTo("status_change"));
    }

    @Test
    void integrityReportRejectsCrossMerchantCredentialRevocation() {
        // P1-2 回归：商户 A 的调用者不得对商户 B 的设备凭证上报完整性异常（按 credId 跨商户吊销=DoS）。
        StaffProfileRecord mB = approvedProfile("merchant_b");
        StaffDeviceCredentialRecord cred = service.bindCredential(new StaffDeviceBindRequest(
                key(), null, mB.id(), "pubkey-xm", "fp-hash-xm"));
        // 切换为 merchant_a 调用者：跨商户凭证归属校验应拒绝。
        com.complex.platform.support.TestSecurityActors.asMerchantUser("merchant_a");
        assertThatThrownBy(() -> service.reportIntegrity(cred.id(),
                new StaffDeviceIntegrityReportRequest(key(), null, "root_detected")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
    }

    @Test
    void checkConstraintRejectsIllegalBindStatusEnum() {
        StaffProfileRecord staff = approvedProfile(null);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO staff_device_credentials
                    (id, staff_profile_id, device_public_key, device_fingerprint_hash, bind_status, integrity_status,
                     created_at, updated_at)
                VALUES (?, ?, 'pk', 'fp', 'bogus_status', 'trusted', now(), now())
                """, "scred_bad_" + key(), staff.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void twoCredentialEntitiesAreSeparate_staffVsKioskNeverCrossReference() {
        StaffProfileRecord staff = approvedProfile(null);
        StaffDeviceCredentialRecord cred = service.bindCredential(new StaffDeviceBindRequest(
                key(), null, staff.id(), "pubkey-x", "fp-hash-x"));
        // staff_device_credentials 仅外键到 staff_profiles 与自身，绝不引用 terminal_device_credentials。
        List<String> fkTargets = jdbcTemplate.queryForList("""
                SELECT ccu.table_name
                  FROM information_schema.table_constraints tc
                  JOIN information_schema.constraint_column_usage ccu
                    ON tc.constraint_name = ccu.constraint_name
                 WHERE tc.table_name = 'staff_device_credentials'
                   AND tc.constraint_type = 'FOREIGN KEY'
                """, String.class);
        assertThat(fkTargets).containsExactlyInAnyOrder("staff_profiles", "staff_device_credentials");
        assertThat(fkTargets).doesNotContain("terminal_device_credentials", "terminal_devices");
        assertThat(cred.id()).startsWith("scred_");
    }

    @Test
    void merchantScopeIsolationOnlyReturnsOwnMerchantRows() {
        StaffProfileRecord platform = approvedProfile(null);          // merchant_id NULL
        StaffProfileRecord mA = approvedProfile("merchant_a");
        StaffProfileRecord mB = approvedProfile("merchant_b");

        // 平台 scope（null）见全量。
        List<String> all = repository.listProfiles(null).stream().map(StaffProfileRecord::id).toList();
        assertThat(all).contains(platform.id(), mA.id(), mB.id());

        // 商户 scope 仅见本户。
        List<String> onlyA = repository.listProfiles("merchant_a").stream().map(StaffProfileRecord::id).toList();
        assertThat(onlyA).contains(mA.id()).doesNotContain(mB.id(), platform.id());
    }

    @Test
    void onlyIrreversibleHashesPersistedNoPlaintextPii() {
        StaffProfileRecord staff = submitRegistration(null);
        // phone_hash 列里就是我们传入的哈希值；不存在任何明文手机号列。
        assertThat(staff.phoneHash()).isEqualTo("sha256:phone-hash");
        List<String> columns = jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns WHERE table_name = 'staff_profiles'
                """, String.class);
        assertThat(columns).contains("phone_hash", "id_doc_hash")
                .doesNotContain("phone", "phone_number", "id_card", "id_doc");
    }

    @Test
    void noticeConsentRequiredMustBeExplicitlyConsentedOrDeclined() {
        StaffProfileRecord staff = approvedProfile(null);
        EmployeeNoticeRecord notice = service.createNotice(staff.id(), "compliance_consent",
                "隐私政策同意", "请阅读并同意", true, "system");

        assertThatThrownBy(() -> service.acknowledge(notice.id(),
                new EmployeeNoticeAcknowledgeRequest(key(), null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("consent");

        EmployeeNoticeRecord consented = service.acknowledge(notice.id(),
                new EmployeeNoticeAcknowledgeRequest(key(), null, true, null));
        assertThat(consented.consentStatus()).isEqualTo("consented");
        assertThat(consented.consentedAt()).isNotNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StaffProfileRecord submitRegistration(String merchantId) {
        return service.submitRegistration(new StaffRegistrationRequest(
                key(), null, "project_main", merchantId, "员工" + key(),
                "sha256:phone-hash", "sha256:id-hash", null));
    }

    private StaffProfileRecord approvedProfile(String merchantId) {
        StaffProfileRecord submitted = submitRegistration(merchantId);
        return service.reviewRegistration(submitted.id(),
                new StaffRegistrationReviewRequest(key(), null, "approve", null, "Init-Pass-Staff-1"));
    }

    @org.junit.jupiter.api.Test
    void v210ShiftAttendanceStrictJudgement() {
        // E批(2026-07-02 拍板):排班幂等覆盖+我的班次窗;打卡严格无宽限——晚于开始即迟到、
        // 早于结束即早退、无班次记 no_shift;权限种子在位。
        authenticateAsPlatformAdmin();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM permissions WHERE permission_code IN "
                        + "('staff.shift.manage','staff.shift.view','staff.attendance.punch','staff.attendance.view')",
                Integer.class)).isEqualTo(4);

        Instant now = Instant.now();
        java.time.LocalDate today = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        // 排班:已开始 1 小时、还有 7 小时结束 → 现在打上班=迟到,打下班=早退。
        com.complex.platform.access.staff.StaffShiftRecord shift = service.upsertShift(
                new com.complex.platform.access.staff.dto.StaffShiftRequest(
                        key(), "usr_admin", today, now.minusSeconds(3600), now.plusSeconds(7 * 3600),
                        List.of("sp_point_1", "sp_point_2"), "E批测试"));
        assertThat(shift.pointSpaceIds()).containsExactly("sp_point_1", "sp_point_2");
        // 幂等覆盖:同 (user, start_at) 再排 → 单行,点位更新。
        service.upsertShift(new com.complex.platform.access.staff.dto.StaffShiftRequest(
                key(), "usr_admin", today, now.minusSeconds(3600), now.plusSeconds(8 * 3600),
                List.of("sp_point_3"), "改班"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_shift_schedules WHERE user_id='usr_admin'", Integer.class)).isEqualTo(1);

        assertThat(service.listMyShifts().items()).hasSize(1);

        com.complex.platform.access.staff.StaffAttendanceRecord checkIn = service.punch(
                new com.complex.platform.access.staff.dto.StaffAttendancePunchRequest(
                        key(), "check_in", "nfc", "space:sp_point_3", "sp_point_3", "fp_test"));
        assertThat(checkIn.judgement()).isEqualTo("late");        // 严格:开始后打卡即迟到
        assertThat(checkIn.shiftId()).isNotNull();

        com.complex.platform.access.staff.StaffAttendanceRecord checkOut = service.punch(
                new com.complex.platform.access.staff.dto.StaffAttendancePunchRequest(
                        key(), "check_out", "qr", "space:sp_point_3", "sp_point_3", "fp_test"));
        assertThat(checkOut.judgement()).isEqualTo("early_leave"); // 严格:结束前打卡即早退

        assertThat(service.listMyAttendance().items()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(service.listAttendance(today).items())
                .extracting(com.complex.platform.access.staff.StaffAttendanceRecord::judgement)
                .contains("late", "early_leave");

        // 删班次后打卡 → no_shift。
        jdbcTemplate.update("DELETE FROM staff_shift_schedules WHERE user_id='usr_admin'");
        com.complex.platform.access.staff.StaffAttendanceRecord orphan = service.punch(
                new com.complex.platform.access.staff.dto.StaffAttendancePunchRequest(
                        key(), "check_in", "manual", null, null, null));
        assertThat(orphan.judgement()).isEqualTo("no_shift");
    }

    @org.junit.jupiter.api.Test
    void submitLeaveRejectsOverlapWithActiveLeave() {
        // 审计 B-P2(本轮修):与未撤销(pending/approved)请假重叠的新请假被拒,防重复扣薪。
        authenticateAsPlatformAdmin();
        com.complex.platform.access.staff.StaffLeaveRequestRecord first = service.submitLeave(
                new com.complex.platform.access.staff.dto.StaffLeaveRequest(
                        key(), "personal", java.time.LocalDate.parse("2099-09-10"),
                        java.time.LocalDate.parse("2099-09-12"), "事假"));
        service.reviewLeave(first.id(), new com.complex.platform.access.staff.dto.StaffLeaveReviewRequest(
                key(), "approve", "准"));
        // 09-11 落在 09-10..12 内 → 重叠拒绝
        assertThatThrownBy(() -> service.submitLeave(
                new com.complex.platform.access.staff.dto.StaffLeaveRequest(
                        key(), "personal", java.time.LocalDate.parse("2099-09-11"),
                        java.time.LocalDate.parse("2099-09-13"), "重叠")))
                .isInstanceOf(ApiException.class);
        // 09-20 不重叠 → 放行
        com.complex.platform.access.staff.StaffLeaveRequestRecord third = service.submitLeave(
                new com.complex.platform.access.staff.dto.StaffLeaveRequest(
                        key(), "personal", java.time.LocalDate.parse("2099-09-20"),
                        java.time.LocalDate.parse("2099-09-21"), "另一段"));
        assertThat(third.status()).isEqualTo("pending");
    }

    @org.junit.jupiter.api.Test
    void v211PayrollLeaveMonthView() {
        // F批(2026-07-02 拍板):月结定格(total=base+提成+奖-罚,可重结覆盖);员工只见已定格月;
        // 请假主管审,通过区间月视图记 leave;月视图六态口径。
        authenticateAsPlatformAdmin();
        String month = "2099-08";

        service.setBaseSalary(new com.complex.platform.access.staff.dto.StaffPayrollBaseRequest(
                key(), "usr_admin", 800000L));
        service.addCommission(new com.complex.platform.access.staff.dto.StaffCommissionRequest(
                key(), "usr_admin", month, "manual", null, 120000L, "订位提成"));
        service.addPayrollAdjustment(new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                key(), "usr_admin", month, "bonus", 50000L, "月度之星"));
        service.addPayrollAdjustment(new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                key(), "usr_admin", month, "penalty", 20000L, "迟到3次"));
        // 事由缺失必须拒绝(钱路留痕硬约束)。
        assertThatThrownBy(() -> service.addPayrollAdjustment(
                new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                        key(), "usr_admin", month, "penalty", 100L, " ")))
                .isInstanceOf(ApiException.class);

        // 月结前员工不可见(拍板:月结后才可见)。
        assertThat(service.listMyPayroll(month).items()).isEmpty();

        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), month));
        com.complex.platform.access.staff.StaffPayrollStatementRecord statement =
                service.listMyPayroll(month).items().stream()
                        .filter(row -> row.userId().equals("usr_admin")).findFirst().orElseThrow();
        assertThat(statement.baseCent()).isEqualTo(800000L);
        assertThat(statement.commissionCent()).isEqualTo(120000L);
        assertThat(statement.bonusCent()).isEqualTo(50000L);
        assertThat(statement.penaltyCent()).isEqualTo(20000L);
        assertThat(statement.taxCent()).isEqualTo(0L);   // 无 service_order 劳务 → 无个税
        assertThat(statement.totalCent()).isEqualTo(800000L + 120000L + 50000L - 20000L);

        // 补录奖金后重结=覆盖,仍单行。
        service.addPayrollAdjustment(new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                key(), "usr_admin", month, "bonus", 10000L, "补录"));
        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), month));
        java.util.List<com.complex.platform.access.staff.StaffPayrollStatementRecord> after =
                service.listMyPayroll(month).items().stream()
                        .filter(row -> row.userId().equals("usr_admin")).toList();
        assertThat(after).hasSize(1);
        assertThat(after.get(0).totalCent()).isEqualTo(800000L + 120000L + 60000L - 20000L);

        // H批:加入当月服务劳务 3 万元(service_order 台账)→ 重结代扣个税
        // 劳务报酬预扣:X=3万>4000 → Y=X*80%=2.4万 > 2万 → tax=Y*30%-2000元=5200元=520000分。
        jdbcTemplate.update("INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, created_at) "
                + "VALUES ('cml_tax_" + key() + "', 'usr_admin', '" + month + "', 'service_order', 'svo_tax', 3000000, now())");
        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), month));
        com.complex.platform.access.staff.StaffPayrollStatementRecord taxed =
                service.listMyPayroll(month).items().stream()
                        .filter(row -> row.userId().equals("usr_admin")).findFirst().orElseThrow();
        assertThat(taxed.taxCent()).isEqualTo(520000L);
        assertThat(taxed.commissionCent()).isEqualTo(120000L + 3000000L);
        assertThat(taxed.totalCent()).isEqualTo(taxed.baseCent() + taxed.commissionCent()
                + taxed.bonusCent() - taxed.penaltyCent() - taxed.taxCent());

        // 请假:提交→主管通过→月视图该日=leave。
        com.complex.platform.access.staff.StaffLeaveRequestRecord leave = service.submitLeave(
                new com.complex.platform.access.staff.dto.StaffLeaveRequest(
                        key(), "annual", java.time.LocalDate.parse("2099-08-10"),
                        java.time.LocalDate.parse("2099-08-11"), "年假"));
        assertThat(leave.status()).isEqualTo("pending");
        service.reviewLeave(leave.id(), new com.complex.platform.access.staff.dto.StaffLeaveReviewRequest(
                key(), "approve", "准"));

        // 排班一天(2099-08-05)不打卡=缺勤;请假日=leave;其余无排班=rest。
        service.upsertShift(new com.complex.platform.access.staff.dto.StaffShiftRequest(
                key(), "usr_admin", java.time.LocalDate.parse("2099-08-05"),
                Instant.parse("2099-08-05T02:00:00Z"), Instant.parse("2099-08-05T10:00:00Z"),
                List.of("sp_p1"), null));
        java.util.List<com.complex.platform.access.staff.StaffAttendanceDayRecord> view =
                service.myAttendanceMonthView(month);
        assertThat(view).hasSize(31);
        java.util.Map<String, java.util.List<String>> byDay = new java.util.LinkedHashMap<>();
        for (com.complex.platform.access.staff.StaffAttendanceDayRecord d : view) {
            byDay.put(d.date().toString(), d.dayStatus());
        }
        assertThat(byDay.get("2099-08-05")).containsExactly("absent");
        assertThat(byDay.get("2099-08-10")).containsExactly("leave");
        assertThat(byDay.get("2099-08-11")).containsExactly("leave");
        assertThat(byDay.get("2099-08-20")).containsExactly("rest");

        // 请假扣薪(2026-07-05 拍板):事假(无薪假)按日工资×天数扣;病假/年假不扣。
        // 已有年假 2099-08-10..11(2 天)不扣;新增事假 2099-08-15..16(2 天)扣、病假 2099-08-18(1 天)不扣。
        com.complex.platform.access.staff.StaffLeaveRequestRecord personalLeave = service.submitLeave(
                new com.complex.platform.access.staff.dto.StaffLeaveRequest(
                        key(), "personal", java.time.LocalDate.parse("2099-08-15"),
                        java.time.LocalDate.parse("2099-08-16"), "私事"));
        service.reviewLeave(personalLeave.id(), new com.complex.platform.access.staff.dto.StaffLeaveReviewRequest(
                key(), "approve", "准"));
        com.complex.platform.access.staff.StaffLeaveRequestRecord sickLeave = service.submitLeave(
                new com.complex.platform.access.staff.dto.StaffLeaveRequest(
                        key(), "sick", java.time.LocalDate.parse("2099-08-18"),
                        java.time.LocalDate.parse("2099-08-18"), "感冒"));
        service.reviewLeave(sickLeave.id(), new com.complex.platform.access.staff.dto.StaffLeaveReviewRequest(
                key(), "approve", "准"));

        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), month));
        com.complex.platform.access.staff.StaffPayrollStatementRecord withLeave = service.listMyPayroll(month).items().stream()
                .filter(row -> row.userId().equals("usr_admin")).findFirst().orElseThrow();
        long dailyWage = Math.round(800000L / 21.75);   // 月薪÷21.75(法定计薪天数),四舍五入到分
        assertThat(withLeave.leaveDeductionCent()).isEqualTo(dailyWage * 2);   // 仅事假 2 天,病假/年假不扣
        assertThat(withLeave.totalCent()).isEqualTo(withLeave.baseCent() + withLeave.commissionCent()
                + withLeave.bonusCent() - withLeave.penaltyCent() - withLeave.leaveDeductionCent()
                - withLeave.taxCent());
    }

    @org.junit.jupiter.api.Test
    void v214StaffRowsDeriveProjectAndScopedListsExcludeOtherProjects() {
        // P0 租户隔离(V214):员工行数据写入时按员工档案推导 project_id;
        // 主管/后台列表 scoped 查询只见本项目行(null 项目行照 3.2 口径同样不可见)。
        authenticateAsPlatformAdmin();
        StaffProfileRecord staff = approvedProfile(null); // 档案 project_main
        String userId = staff.userId();
        java.time.LocalDate day = java.time.LocalDate.parse("2099-07-01");
        Instant start = Instant.parse("2099-07-01T02:00:00Z");

        // 排班:写入即带员工项目。
        service.upsertShift(new com.complex.platform.access.staff.dto.StaffShiftRequest(
                key(), userId, day, start, start.plusSeconds(8 * 3600), List.of("sp_p1"), null));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT project_id FROM staff_shift_schedules WHERE user_id = ?", String.class, userId))
                .isEqualTo("project_main");
        assertThat(repository.listShifts(day, null, "project_main"))
                .extracting(StaffShiftRecord::userId).contains(userId);
        assertThat(repository.listShifts(day, null, "prj_other"))
                .extracting(StaffShiftRecord::userId).doesNotContain(userId);

        // 奖惩:写入即带员工项目;usr_admin(无档案)行 project 为 NULL → scoped 不可见。
        service.addPayrollAdjustment(new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                key(), userId, "2099-07", "bonus", 1000L, "隔离测试"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT project_id FROM staff_payroll_adjustments WHERE user_id = ?", String.class, userId))
                .isEqualTo("project_main");

        // 请假(usr_admin 提交,无档案 → project NULL):平台全量可见,任一项目 scoped 均不可见。
        com.complex.platform.access.staff.StaffLeaveRequestRecord leave = service.submitLeave(
                new com.complex.platform.access.staff.dto.StaffLeaveRequest(
                        key(), "annual", day, day, "隔离测试"));
        assertThat(repository.listLeaves(null, null, null))
                .extracting(StaffLeaveRequestRecord::id).contains(leave.id());
        assertThat(repository.listLeaves(null, null, "project_main"))
                .extracting(StaffLeaveRequestRecord::id).doesNotContain(leave.id());

        // 薪资单:月结按档案推导 project;scoped 只见本项目员工。
        service.setBaseSalary(new com.complex.platform.access.staff.dto.StaffPayrollBaseRequest(
                key(), userId, 500000L));
        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), "2099-07"));
        assertThat(repository.listPayrollStatements("2099-07", null, "project_main"))
                .extracting(StaffPayrollStatementRecord::userId).contains(userId);
        assertThat(repository.listPayrollStatements("2099-07", null, "prj_other"))
                .extracting(StaffPayrollStatementRecord::userId).doesNotContain(userId);

        // 考勤日列表:直插他项目行,scoped 只见本项目。
        jdbcTemplate.update("""
                INSERT INTO staff_attendance_records (id, user_id, punch_type, punched_at, method,
                        judgement, created_at, project_id)
                VALUES (?, ?, 'check_in', '2099-07-01T03:00:00Z', 'manual', 'normal', now(), 'prj_other')
                """, "att_" + key(), userId);
        assertThat(repository.listAttendanceOn(day, "project_main"))
                .extracting(StaffAttendanceRecord::userId).doesNotContain(userId);
        assertThat(repository.listAttendanceOn(day, "prj_other"))
                .extracting(StaffAttendanceRecord::userId).contains(userId);
    }

    @org.junit.jupiter.api.Test
    void idempotencyPreventsDuplicateAdjustmentCommissionAndLeave() {
        // P0 真幂等(2026-07-03):奖惩/提成/请假重复提交=命中重放,不重复入账/建单。
        authenticateAsPlatformAdmin();
        var adjReq = new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                key(), "usr_admin", "2099-06", "bonus", 1000L, "幂等测试奖金");
        service.addPayrollAdjustment(adjReq);
        service.addPayrollAdjustment(adjReq);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_payroll_adjustments WHERE user_id='usr_admin' AND reason='幂等测试奖金'",
                Integer.class)).isEqualTo(1);

        var cmlReq = new com.complex.platform.access.staff.dto.StaffCommissionRequest(
                key(), "usr_admin", "2099-06", "manual", null, 2000L, "幂等测试提成");
        service.addCommission(cmlReq);
        service.addCommission(cmlReq);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_commission_ledger WHERE user_id='usr_admin' AND note='幂等测试提成'",
                Integer.class)).isEqualTo(1);

        var leaveReq = new com.complex.platform.access.staff.dto.StaffLeaveRequest(
                key(), "annual", java.time.LocalDate.parse("2099-06-01"),
                java.time.LocalDate.parse("2099-06-02"), "幂等测试请假");
        com.complex.platform.access.staff.StaffLeaveRequestRecord l1 = service.submitLeave(leaveReq);
        com.complex.platform.access.staff.StaffLeaveRequestRecord l2 = service.submitLeave(leaveReq);
        assertThat(l2.id()).isEqualTo(l1.id());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_leave_requests WHERE reason='幂等测试请假'", Integer.class)).isEqualTo(1);
    }

    @org.junit.jupiter.api.Test
    void failurePathsRecordDeclaredFailureAuditEvents() {
        // P1 审计闭合:失败路径按 openapi x-audit-event-failure 落审计事件后再抛。
        authenticateAsPlatformAdmin();
        assertThatThrownBy(() -> service.addPayrollAdjustment(
                new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                        key(), "usr_admin", "2099-05", "penalty", 100L, " ")))
                .isInstanceOf(ApiException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name='staff.payroll.adjustment_add_failed' AND event_result='failure'",
                Integer.class)).isGreaterThanOrEqualTo(1);

        assertThatThrownBy(() -> service.closePayroll(
                new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), "not-a-month")))
                .isInstanceOf(ApiException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name='staff.payroll.month_close_failed' AND event_result='failure'",
                Integer.class)).isGreaterThanOrEqualTo(1);

        assertThatThrownBy(() -> service.reviewLeave("lv_missing_" + key(),
                new com.complex.platform.access.staff.dto.StaffLeaveReviewRequest(key(), "approve", null)))
                .isInstanceOf(ApiException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name='staff.leave.review_failed' AND event_result='failure'",
                Integer.class)).isGreaterThanOrEqualTo(1);
    }

    private String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void authenticateAsPlatformAdmin() {
        AuthService.UserRecord user = new AuthService.UserRecord(
                "usr_admin", "seed-admin", "hash", "platform_admin", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))));
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version FROM flyway_schema_history WHERE success = true
                 ORDER BY installed_rank DESC LIMIT 1
                """, String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("SELECT permission_code FROM permissions", String.class);
    }
    @org.junit.jupiter.api.Test
    void closePayrollScopedRunOnlyTouchesCallerProjectNeverOtherProjects() {
        // P0 项目级月结(V214 收口):项目 HR(scope=project_main)月结只重算/覆盖本综合体员工,
        // 绝不 INSERT/覆盖其他项目(prj_other)的薪资单;平台口径(scope=null)仍全量。
        authenticateAsPlatformAdmin();
        String month = "2099-09";
        String userMain = "usr_scope_main_" + key();
        String userOther = "usr_scope_other_" + key();
        seedProfile("project_main", userMain);
        seedProfile("prj_other", userOther);
        // 两员工都有薪资要素(固定工资)→ 无 scope 时都会出单。
        jdbcTemplate.update("INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_at, project_id) "
                + "VALUES (?, 300000, now(), 'project_main')", userMain);
        jdbcTemplate.update("INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_at, project_id) "
                + "VALUES (?, 700000, now(), 'prj_other')", userOther);

        // (1) project_main 口径月结:只生成本项目员工薪资单。
        int mainCount = repository.closePayrollMonth(month, "usr_admin", Instant.now(), "project_main");
        assertThat(statementTotal(month, userMain)).isEqualTo(300000L);
        assertThat(statementExists(month, userOther)).isFalse();
        assertThat(mainCount).isEqualTo(1);

        // (2) prj_other 已有薪资单(平台口径生成),再跑 project_main 口径不得覆盖它。
        repository.closePayrollMonth(month, "usr_admin", Instant.now(), "prj_other");
        assertThat(statementTotal(month, userOther)).isEqualTo(700000L);
        // 篡改 prj_other 底薪后,只跑 project_main 口径月结 → prj_other 薪资单必须原封不动。
        jdbcTemplate.update("UPDATE staff_base_salaries SET monthly_salary_cent = 999999 WHERE user_id = ?", userOther);
        repository.closePayrollMonth(month, "usr_admin", Instant.now(), "project_main");
        assertThat(statementTotal(month, userOther)).isEqualTo(700000L);   // 未被越权重算/覆盖

        // (3) 平台口径(null)仍能全量重算(覆盖 prj_other 到最新)。
        repository.closePayrollMonth(month, "usr_admin", Instant.now(), null);
        assertThat(statementTotal(month, userOther)).isEqualTo(999999L);
    }

    private void seedProfile(String projectId, String userId) {
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash,
                        registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, 'scoped', 'sha256:p', 'sha256:i', 'approved', 'active', now(), now())
                """, "stf_" + key(), projectId, userId);
    }

    private boolean statementExists(String month, String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_payroll_statements WHERE month = ? AND user_id = ?",
                Integer.class, month, userId) > 0;
    }

    private Long statementTotal(String month, String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT total_cent FROM staff_payroll_statements WHERE month = ? AND user_id = ?",
                Long.class, month, userId);
    }

    // ── BYOD 二期 Wave1(2026-07-19 拍板):本人提成明细 + 本人档案自助 ──

    @org.junit.jupiter.api.Test
    void wave1MyCommissionLinesReturnOwnLinesGroupableBySource() {
        authenticateAsPlatformAdmin();  // currentActor().id() = usr_admin
        String month = "2099-09";
        jdbcTemplate.update("INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, note, created_at) "
                + "VALUES (?, 'usr_admin', ?, 'service_order', 'svo_1', 60000, '服务点单份额', now())", "cml_" + key(), month);
        jdbcTemplate.update("INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, note, created_at) "
                + "VALUES (?, 'usr_admin', ?, 'manual', 'm_1', 40000, '订位', now())", "cml_" + key(), month);
        jdbcTemplate.update("INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, created_at) "
                + "VALUES (?, 'usr_other', ?, 'service_order', 'svo_x', 999999, now())", "cml_" + key(), month);

        java.util.List<com.complex.platform.access.staff.StaffCommissionLineRecord> lines =
                service.listMyCommissions(month).items();
        // 只返本人(不含 usr_other 的 999999),两条合计 1000 元。
        assertThat(lines).allMatch(l -> l.amountCent() != 999999L);
        assertThat(lines.stream().mapToLong(
                com.complex.platform.access.staff.StaffCommissionLineRecord::amountCent).sum())
                .isEqualTo(100000L);
        assertThat(lines.stream().map(
                com.complex.platform.access.staff.StaffCommissionLineRecord::sourceType).toList())
                .contains("service_order", "manual");
        // 月份格式校验。
        assertThatThrownBy(() -> service.listMyCommissions("2099/09")).isInstanceOf(ApiException.class);

        jdbcTemplate.update("DELETE FROM staff_commission_ledger WHERE month = ?", month);
    }

    @org.junit.jupiter.api.Test
    void wave1MyProfileSelfViewAndNameEditTokenDerivedOnly() {
        authenticateAsPlatformAdmin();  // usr_admin
        jdbcTemplate.update("DELETE FROM staff_profiles WHERE user_id = 'usr_admin'");
        // 无档案 → not found。
        assertThatThrownBy(() -> service.getMyProfile()).isInstanceOf(ApiException.class);

        seedProfile("prj_self", "usr_admin");
        assertThat(service.getMyProfile().name()).isEqualTo("scoped");

        // 有限字段自助改名(身份按 token 派生,请求体无 userId → 无法越权改他人)。
        com.complex.platform.access.staff.StaffProfileRecord updated = service.updateMyProfile(
                new com.complex.platform.access.staff.dto.StaffProfileSelfUpdateRequest(key(), "张三"));
        assertThat(updated.name()).isEqualTo("张三");
        assertThat(service.getMyProfile().name()).isEqualTo("张三");

        // 空名拒绝。
        assertThatThrownBy(() -> service.updateMyProfile(
                new com.complex.platform.access.staff.dto.StaffProfileSelfUpdateRequest(key(), "  ")))
                .isInstanceOf(ApiException.class);

        jdbcTemplate.update("DELETE FROM staff_profiles WHERE user_id = 'usr_admin'");
    }

    @org.junit.jupiter.api.Test
    void approvedStaffGetsSelfServiceRoleAndSeesOnlyOwnRecords() {
        // 既有缺口修复(2026-07-19)回归:审批通过为员工分配 role_staff_self →
        // 真实员工(userType=staff)token 可用 BYOD 自助端点(此前仅 platform_admin 桥可用),且只见本人。

        // 前置:无角色的真实 staff token → 自助端点被拒(证明权限门真实、非默认放行,即修复前的缺口态)。
        authenticateAsStaff("usr_norole_" + key(), "mrc_self");
        assertThatThrownBy(() -> service.listMyShifts())
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));

        // 审批两名员工 A/B(同商户)。
        authenticateAsPlatformAdmin();
        StaffProfileRecord a = submitRegistration("mrc_self");
        StaffProfileRecord b = submitRegistration("mrc_self");
        String userA = service.reviewRegistration(a.id(),
                new StaffRegistrationReviewRequest(key(), null, "approve", null, "Init-Pass-A-1")).userId();
        String userB = service.reviewRegistration(b.id(),
                new StaffRegistrationReviewRequest(key(), null, "approve", null, "Init-Pass-B-1")).userId();

        // 审批即写 user_roles → role_staff_self。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = 'role_staff_self'",
                Integer.class, userA)).isEqualTo(1);

        // 为 A/B 各定格一张薪资单 + 各排今天一个班次(走服务方法,避免硬钉表列)。
        String month = "2099-11";
        service.setBaseSalary(new com.complex.platform.access.staff.dto.StaffPayrollBaseRequest(key(), userA, 111000L));
        service.setBaseSalary(new com.complex.platform.access.staff.dto.StaffPayrollBaseRequest(key(), userB, 222000L));
        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), month));
        Instant now = Instant.now();
        java.time.LocalDate today = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        service.upsertShift(new com.complex.platform.access.staff.dto.StaffShiftRequest(
                key(), userA, today, now.minusSeconds(3600), now.plusSeconds(7 * 3600), List.of(), null));
        service.upsertShift(new com.complex.platform.access.staff.dto.StaffShiftRequest(
                key(), userB, today, now.minusSeconds(3600), now.plusSeconds(7 * 3600), List.of(), null));

        // 以真实员工 A 的 token:自助端点放行,且只见自己的记录(看不到 B)。
        authenticateAsStaff(userA, "mrc_self");
        assertThat(service.listMyPayroll(month).items())
                .extracting(com.complex.platform.access.staff.StaffPayrollStatementRecord::userId)
                .containsExactly(userA);
        assertThat(service.listMyShifts().items())
                .extracting(com.complex.platform.access.staff.StaffShiftRecord::userId)
                .containsExactly(userA);
    }

    /** 以真实员工(account_type=staff)身份认证:走 role\u2192permission 解析,不再享 platform_admin 桥的通配放行。 */
    private void authenticateAsStaff(String userId, String merchantId) {
        AuthService.UserRecord user = new AuthService.UserRecord(
                userId, "staff_" + userId, "hash", "staff", null, merchantId, "active",
                Instant.parse("2026-07-19T00:00:00Z"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));
    }

    @org.junit.jupiter.api.Test
    void d2FixListNoticesNowRequiresNoticeViewPermission() {
        // D② 修复回归:listNotices(待办消息中心列表)此前实现漏 @GuardedAction 守卫 → 任何认证主体可列员工通知;
        // 现已把 staff.notice.view 守卫归位到 listNotices。无角色真实 staff → 被拒。
        authenticateAsStaff("usr_norole_notice_" + key(), "mrc_self");
        assertThatThrownBy(() -> service.listNotices())
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
    }

    @org.junit.jupiter.api.Test
    void d2FixUpsertShiftNoLongerRequiresNoticeViewOnlyShiftManage() {
        // D② 修复回归:错扣在 upsertShift 上的 @GuardedAction(staff.notice.view) 已移走 →
        // 排班只需 staff.shift.manage,不再连带要求 notice.view。构造仅持 shift.manage 的真实 staff。
        String mgr = "usr_shiftmgr_" + key();
        accessRepository.createUser(new com.complex.platform.access.AccessUser(
                mgr, "sm_" + key(), "staff", null, null, "active", Instant.now()));
        String roleId = "role_shiftmgr_test_" + key();
        jdbcTemplate.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) "
                + "VALUES (?, ?, 'shift mgr regression role', 'project', 'active', now())", roleId, "SHIFTMGR_" + key());
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'staff.shift.manage')", roleId);
        accessRepository.assignUserRole(mgr, roleId);

        authenticateAsStaff(mgr, null);
        Instant now = Instant.now();
        java.time.LocalDate today = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        // 有 shift.manage、无 notice.view → 不再被 notice.view 拦,排班成功。
        com.complex.platform.access.staff.StaffShiftRecord saved = service.upsertShift(
                new com.complex.platform.access.staff.dto.StaffShiftRequest(
                        key(), mgr, today, now.minusSeconds(3600), now.plusSeconds(7 * 3600), List.of(), null));
        assertThat(saved.userId()).isEqualTo(mgr);
    }

    @org.junit.jupiter.api.Test
    void listProfilesRequiresProfileViewNotReachableBySelfServiceStaff() {
        // 复核发现的同类 authz 漏洞:GET /staff/profiles 实际调 listProfiles(boolean) 重载,
        // 该重载此前漏 @GuardedAction(守卫只加在无参重载、控制器未调它) → 任何认证主体
        // (含仅 role_staff_self 的员工)可列全员档案(含 phoneHash/idDocHash)=PII 泄露。
        authenticateAsPlatformAdmin();
        StaffProfileRecord p = submitRegistration("mrc_self");
        String staffUser = service.reviewRegistration(p.id(),
                new StaffRegistrationReviewRequest(key(), null, "approve", null, "Init-Pass-P-1")).userId();

        // 真实员工(role_staff_self,不含 staff.profile.view)→ 列全员档案被拒。
        authenticateAsStaff(staffUser, "mrc_self");
        assertThatThrownBy(() -> service.listProfiles(false))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));

        // 平台管理员(通配)→ 成功。
        authenticateAsPlatformAdmin();
        assertThat(service.listProfiles(false)).isNotNull();
    }
}
