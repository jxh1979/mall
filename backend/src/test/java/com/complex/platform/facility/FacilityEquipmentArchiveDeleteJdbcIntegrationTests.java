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
        "spring.datasource.password=complex_dev_pwd"
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
        service.archive("feq_del", true);
        service.requestDelete("feq_del", new FacilityEquipmentDeleteRequest("报废"));
        TestSecurityActors.asPlatformAdmin("usr_approver");
        FacilityEquipmentDeleteDecision decision = service.approveDelete(
                "feq_del", new FacilityEquipmentDeleteApprovalRequest("approved", null));
        assertThat(decision.deleted()).isTrue();
        assertThat(repository.findEquipmentById("feq_del")).isEmpty();
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
        service.archive("feq_use", true);
        service.requestDelete("feq_use", new FacilityEquipmentDeleteRequest("试删"));
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO facility_work_orders (id, code, work_order_type, title, equipment_id, created_at, updated_at)
                VALUES ('fwo_use', 'FWO-USE-1', 'repair', '设备维修', 'feq_use', ?, ?)
                """, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        TestSecurityActors.asPlatformAdmin("usr_approver");
        assertThatThrownBy(() -> service.approveDelete("feq_use",
                new FacilityEquipmentDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("维保工单");
    }

    @Test
    void selfApproveForbidden() {
        TestSecurityActors.asPlatformAdmin("usr_same");
        seed("feq_self");
        service.archive("feq_self", true);
        service.requestDelete("feq_self", new FacilityEquipmentDeleteRequest("x"));
        assertThatThrownBy(() -> service.approveDelete("feq_self",
                new FacilityEquipmentDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("发起人");
    }
}
