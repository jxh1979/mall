package com.complex.platform.merchantbilling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
class MerchantBillingJdbcIntegrationTests {

    @Autowired
    private JdbcMerchantBillingRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesMerchantBillingTablesAndSeedsPermissions() {
        assertThat(regclass("merchant_bills")).isEqualTo("merchant_bills");
        assertThat(regclass("merchant_bill_lines")).isEqualTo("merchant_bill_lines");
        assertThat(regclass("merchant_bill_disputes")).isEqualTo("merchant_bill_disputes");
        assertThat(regclass("merchant_bill_dunning_records")).isEqualTo("merchant_bill_dunning_records");
        assertThat(currentFlywayVersion()).isEqualTo("039");

        assertThat(permissionCodes()).contains(
                "merchant.billing.view",
                "merchant.billing.manage",
                "merchant.billing.confirm",
                "merchant.billing.dispute.submit",
                "merchant.billing.dunning.record"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "merchant.billing.manage",
                "merchant.billing.confirm",
                "merchant.billing.dispute.submit",
                "merchant.billing.dunning.record"
        );
    }

    @Test
    void saveAndReadMerchantBillDisputesAndDunningRecords() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveBill(new MerchantBillRecord(
                "mbill_jdbc",
                "merchant_jdbc",
                "arap_jdbc",
                "MBILL-JDBC-001",
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                LocalDate.parse("2026-07-05"),
                168800L,
                "CNY",
                "issued",
                0,
                "manual",
                "billing_batch_jdbc",
                List.of(
                        new MerchantBillLineRecord("mbill_line_rent_jdbc", "mbill_jdbc", "rent", "June rent", 128800L, "CNY", now),
                        new MerchantBillLineRecord("mbill_line_cam_jdbc", "mbill_jdbc", "cam", "June CAM", 40000L, "CNY", now)
                ),
                now,
                now
        ));
        repository.saveDispute(new MerchantBillDisputeRecord(
                "mbill_dispute_jdbc",
                "mbill_jdbc",
                "merchant_jdbc",
                "energy_amount_mismatch",
                "Energy reading mismatch",
                "submitted",
                List.of("att_meter_photo"),
                "staff_finance",
                now,
                now
        ));
        repository.saveDunningRecord(new MerchantBillDunningRecord(
                "mbill_dunning_jdbc",
                "mbill_jdbc",
                "merchant_jdbc",
                "wecom",
                "rent_overdue_first_notice",
                "sent",
                Instant.parse("2026-06-16T02:00:00Z"),
                "staff_finance",
                now
        ));

        assertThat(repository.findBillById("mbill_jdbc"))
                .get()
                .extracting(MerchantBillRecord::merchantId, MerchantBillRecord::status, MerchantBillRecord::totalAmountCent)
                .containsExactly("merchant_jdbc", "issued", 168800L);
        assertThat(repository.findBillById("mbill_jdbc").orElseThrow().lines())
                .extracting(MerchantBillLineRecord::feeType, MerchantBillLineRecord::amountCent)
                .containsExactly(tuple("rent", 128800L), tuple("cam", 40000L));
        assertThat(repository.findAllBills())
                .extracting(MerchantBillRecord::id, MerchantBillRecord::billNo)
                .contains(tuple("mbill_jdbc", "MBILL-JDBC-001"));
        assertThat(repository.findDisputesByBillId("mbill_jdbc"))
                .extracting(MerchantBillDisputeRecord::reasonCode, MerchantBillDisputeRecord::evidenceAttachmentIds)
                .contains(tuple("energy_amount_mismatch", List.of("att_meter_photo")));
        assertThat(repository.findDunningRecordsByBillId("mbill_jdbc"))
                .extracting(MerchantBillDunningRecord::dunningChannel, MerchantBillDunningRecord::status)
                .contains(tuple("wecom", "sent"));
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
