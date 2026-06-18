package com.complex.platform.merchantservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class MerchantServiceJdbcIntegrationTests {

    @Autowired
    private JdbcMerchantServiceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesMerchantServiceTablesAndSeedsPermissions() {
        assertThat(regclass("merchant_service_work_orders")).isEqualTo("merchant_service_work_orders");
        assertThat(regclass("merchant_service_work_order_actions")).isEqualTo("merchant_service_work_order_actions");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());

        assertThat(permissionCodes()).contains(
                "merchant.service_work_order.view",
                "merchant.service_work_order.create",
                "merchant.service_work_order.assign",
                "merchant.service_work_order.process",
                "merchant.service_work_order.close"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "merchant.service_work_order.create",
                "merchant.service_work_order.assign",
                "merchant.service_work_order.process",
                "merchant.service_work_order.close"
        );
    }

    @Test
    void saveAndListWorkOrdersAndActionsThroughPostgres() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveWorkOrder(new MerchantWorkOrderRecord(
                "mswo_jdbc",
                "mch_jdbc",
                "sp_jdbc",
                "ct_jdbc",
                "repair",
                "high",
                "created",
                "Water leak",
                "Shop ceiling is leaking",
                "staff_reporter",
                "web",
                null,
                null,
                Instant.parse("2026-06-15T08:00:00Z"),
                null,
                null,
                null,
                List.of("att-photo-1", "att-photo-2"),
                now,
                now
        ));
        repository.saveAction(new MerchantWorkOrderActionRecord(
                "mswo_action_jdbc",
                "mswo_jdbc",
                "created",
                null,
                "created",
                "staff_reporter",
                "create work order",
                List.of("att-photo-1"),
                now
        ));

        MerchantWorkOrderRecord assigned = repository.saveWorkOrder(repository.findWorkOrderById("mswo_jdbc")
                .orElseThrow()
                .assign("property", "staff_owner", Instant.parse("2026-06-14T01:00:00Z")));
        repository.saveAction(new MerchantWorkOrderActionRecord(
                "mswo_action_assign_jdbc",
                "mswo_jdbc",
                "assigned",
                "created",
                "assigned",
                "staff_manager",
                "assign to property",
                List.of("att-assign"),
                Instant.parse("2026-06-14T01:00:00Z")
        ));

        assertThat(assigned.status()).isEqualTo("assigned");
        assertThat(repository.findWorkOrderById("mswo_jdbc"))
                .get()
                .extracting(
                        MerchantWorkOrderRecord::merchantId,
                        MerchantWorkOrderRecord::status,
                        MerchantWorkOrderRecord::ownerDepartment,
                        MerchantWorkOrderRecord::ownerUserId
                )
                .containsExactly("mch_jdbc", "assigned", "property", "staff_owner");
        assertThat(repository.findWorkOrdersByScope(Optional.empty(), 100))
                .extracting(MerchantWorkOrderRecord::id, MerchantWorkOrderRecord::workOrderType, MerchantWorkOrderRecord::priority)
                .contains(tuple("mswo_jdbc", "repair", "high"));
        assertThat(repository.findActionsByWorkOrderId("mswo_jdbc"))
                .extracting(MerchantWorkOrderActionRecord::actionType, MerchantWorkOrderActionRecord::toStatus)
                .contains(tuple("created", "created"), tuple("assigned", "assigned"));
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = true
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                 ORDER BY permission_code
                """, String.class);
    }
}
