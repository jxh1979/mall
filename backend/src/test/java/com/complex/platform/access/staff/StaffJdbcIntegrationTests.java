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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class StaffJdbcIntegrationTests {

    @Autowired
    private StaffService service;

    @Autowired
    private JdbcStaffRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                new StaffRegistrationReviewRequest(key(), null, "approve", null));

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
    void registrationRejectRequiresReason() {
        StaffProfileRecord submitted = submitRegistration(null);

        assertThatThrownBy(() -> service.reviewRegistration(submitted.id(),
                new StaffRegistrationReviewRequest(key(), null, "reject", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reject_reason");

        StaffProfileRecord rejected = service.reviewRegistration(submitted.id(),
                new StaffRegistrationReviewRequest(key(), null, "reject", "材料不全"));
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
                new StaffRegistrationReviewRequest(key(), null, "approve", null));
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
}
