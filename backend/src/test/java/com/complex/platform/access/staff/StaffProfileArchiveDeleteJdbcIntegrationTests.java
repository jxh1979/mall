package com.complex.platform.access.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.staff.dto.StaffProfileDeleteApprovalRequest;
import com.complex.platform.access.staff.dto.StaffProfileDeleteDecision;
import com.complex.platform.access.staff.dto.StaffProfileDeleteRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** V264 员工档案存档与审批后删除集成测试。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class StaffProfileArchiveDeleteJdbcIntegrationTests {

    @Autowired
    private StaffService service;
    @Autowired
    private StaffRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    private StaffProfileRecord seed(String id, String userId) {
        Instant now = Instant.now();
        return repository.saveProfile(new StaffProfileRecord(
                id, "proj_staff_test", null, userId, "张三", "hash_p", "hash_i", null,
                "approved", "usr_admin", now, null, "active", now, now));
    }

    @Test
    void archiveThenApproveDeleteSucceedsWhenNoReferences() {
        TestSecurityActors.asPlatformAdmin();
        seed("staff_del", "user_del_1");
        service.archiveProfile("staff_del", true, "测试存档理由");
        service.requestDeleteProfile("staff_del", new StaffProfileDeleteRequest("离职"));
        TestSecurityActors.asPlatformAdmin("usr_approver");
        StaffProfileDeleteDecision decision = service.approveDeleteProfile(
                "staff_del", new StaffProfileDeleteApprovalRequest("approved", null));
        assertThat(decision.deleted()).isTrue();
        // 软删语义：行仍保留在库（可查/可审计/可恢复），仅前台不再展示。
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM staff_profiles WHERE id = ?", Integer.class, "staff_del");
        assertThat(rows).isEqualTo(1); // (a) 行仍在数据库里
        assertThat(repository.findProfileById("staff_del")).isEmpty(); // (b) 前台查不到（被隐藏）
    }

    @Test
    void requestDeleteRequiresArchiveFirst() {
        TestSecurityActors.asPlatformAdmin();
        seed("staff_noarch", "user_noarch");
        assertThatThrownBy(() -> service.requestDeleteProfile("staff_noarch", new StaffProfileDeleteRequest("x")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("存档");
    }

    @Test
    void approveDeleteRejectedWhenReferencedByAttendance() {
        TestSecurityActors.asPlatformAdmin();
        seed("staff_use", "user_use_1");
        service.archiveProfile("staff_use", true, "测试存档理由");
        service.requestDeleteProfile("staff_use", new StaffProfileDeleteRequest("试删"));
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO staff_attendance_records (id, user_id, punch_type, punched_at, method, judgement, created_at)
                VALUES ('att_use_1', 'user_use_1', 'check_in', ?, 'manual', 'normal', ?)
                """, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        TestSecurityActors.asPlatformAdmin("usr_approver");
        assertThatThrownBy(() -> service.approveDeleteProfile("staff_use",
                new StaffProfileDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("考勤");
    }

    @Test
    void selfApproveForbidden() {
        TestSecurityActors.asPlatformAdmin("usr_same");
        seed("staff_self", "user_self_1");
        service.archiveProfile("staff_self", true, "测试存档理由");
        service.requestDeleteProfile("staff_self", new StaffProfileDeleteRequest("x"));
        assertThatThrownBy(() -> service.approveDeleteProfile("staff_self",
                new StaffProfileDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("发起人");
    }
}
