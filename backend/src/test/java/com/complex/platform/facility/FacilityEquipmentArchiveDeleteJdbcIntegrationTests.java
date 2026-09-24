package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.facility.dto.FacilityEquipmentDeleteApprovalRequest;
import com.complex.platform.facility.dto.FacilityEquipmentDeleteDecision;
import com.complex.platform.facility.dto.FacilityEquipmentDeleteRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** V264 设施设备存档与审批后删除集成测试。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class FacilityEquipmentArchiveDeleteJdbcIntegrationTests {

    @Autowired
    private FacilityEquipmentService service;
    @Autowired
    private FacilityRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    private void seed(String id) {
        Instant now = Instant.now();
        repository.insertEquipment(new FacilityEquipmentRecord(
                id, "电梯1", "elevator", null, null, null, null, null, "normal",
                null, null, null, null, null, null, null, null, "proj_fac_test", null, now, now));
    }

    @Test
    void archiveThenApproveDeleteSucceedsWhenNoReferences() {
        TestSecurityActors.asPlatformAdmin();
        seed("feq_del");
        service.archive("feq_del", true, "测试存档理由");
        service.requestDelete("feq_del", new FacilityEquipmentDeleteRequest("报废"));
        asMaster(); // BSM 第 577 行：删除由最高权限本人审批
        FacilityEquipmentDeleteDecision decision = service.approveDelete(
                "feq_del", new FacilityEquipmentDeleteApprovalRequest("approved", null));
        assertThat(decision.deleted()).isTrue();
        // 软删语义：审批通过后行仍保留在库（可查/可审计/可恢复），只是前台各处不再展示。
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM facility_equipment WHERE id = ?", Integer.class, "feq_del");
        assertThat(rows).isEqualTo(1); // (a) 行仍在数据库里
        String status = jdbcTemplate.queryForObject(
                "SELECT delete_status FROM facility_equipment WHERE id = ?", String.class, "feq_del");
        assertThat(status).isEqualTo("approved");
        assertThat(repository.findEquipmentById("feq_del")).isEmpty(); // (b) 前台按 id 查不到（被隐藏）
        assertThat(repository.listEquipment(true)).extracting(FacilityEquipmentRecord::id)
                .doesNotContain("feq_del"); // (b) 前台列表也查不到
    }

    @Test
    void requestDeleteRequiresArchiveFirst() {
        TestSecurityActors.asPlatformAdmin();
        seed("feq_noarch");
        assertThatThrownBy(() -> service.requestDelete("feq_noarch", new FacilityEquipmentDeleteRequest("x")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("存档");
    }

    @Test
    void approveDeleteRejectedWhenReferencedByWorkOrder() {
        TestSecurityActors.asPlatformAdmin();
        seed("feq_use");
        service.archive("feq_use", true, "测试存档理由");
        service.requestDelete("feq_use", new FacilityEquipmentDeleteRequest("试删"));
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO facility_work_orders (id, code, work_order_type, title, equipment_id, created_at, updated_at)
                VALUES ('fwo_use', 'FWO-USE-1', 'repair', '设备维修', 'feq_use', ?, ?)
                """, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        asMaster(); // BSM 第 577 行：删除由最高权限本人审批
        assertThatThrownBy(() -> service.approveDelete("feq_use",
                new FacilityEquipmentDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("维保工单");
    }

    @Test
    void ordinaryApplicantCannotApproveMasterOnlyDeletion() {
        TestSecurityActors.asPlatformAdmin("usr_same");
        seed("feq_self");
        service.archive("feq_self", true, "测试存档理由");
        service.requestDelete("feq_self", new FacilityEquipmentDeleteRequest("x"));
        assertThatThrownBy(() -> service.approveDelete("feq_self",
                new FacilityEquipmentDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("最高权限");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT delete_status FROM facility_equipment WHERE id = ?", String.class, "feq_self")).isEqualTo("pending");
    }

    @Test
    void masterRequestedDeletionTakesEffectDirectly() {
        TestSecurityActors.asPlatformAdmin();
        seed("feq_master");
        service.archive("feq_master", true, "测试存档理由");
        asMaster();
        FacilityEquipmentRecord deleted = service.requestDelete("feq_master", new FacilityEquipmentDeleteRequest("最高权限直接删除"));
        assertThat(deleted.deleteStatus()).isEqualTo("approved");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT delete_status FROM facility_equipment WHERE id = ?", String.class, "feq_master")).isEqualTo("approved");
        assertThat(repository.findEquipmentById("feq_master")).isEmpty();
    }

    /** 持授权 U 盘密钥的最高权限本人（生产会话形状：master_authority，所属公司 platform-company）。 */
    private static void asMaster() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME,
                                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"),
                        "token", java.util.List.of()));
    }
}
