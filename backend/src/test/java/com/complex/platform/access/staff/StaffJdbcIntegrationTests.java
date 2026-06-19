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
