package com.complex.platform.compliance;

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
class ComplianceJdbcIntegrationTests {

    @Autowired
    private JdbcComplianceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesComplianceTablesAndSeedsPermissions() {
        assertThat(regclass("contract_key_dates")).isEqualTo("contract_key_dates");
        assertThat(regclass("compliance_expiry_items")).isEqualTo("compliance_expiry_items");
        assertThat(regclass("compliance_reminders")).isEqualTo("compliance_reminders");
        assertThat(currentFlywayVersion()).isEqualTo("054");

        assertThat(permissionCodes()).contains(
                "compliance.contract_key_date.view",
                "compliance.contract_key_date.manage",
                "compliance.expiry_item.view",
                "compliance.expiry_item.manage",
                "compliance.reminder.view",
                "compliance.reminder.scan"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "compliance.contract_key_date.manage",
                "compliance.expiry_item.manage",
                "compliance.reminder.scan"
        );
    }

    @Test
    void saveAndListContractDatesExpiryItemsAndRemindersThroughPostgres() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveContractKeyDate(new ContractKeyDateRecord(
                "ckd_jdbc",
                "contract_jdbc",
                "merchant_jdbc",
                "contract_end",
                "Contract end",
                LocalDate.parse("2026-07-01"),
                List.of(90, 30, 7),
                "active",
                "staff_legal",
                "manual",
                true,
                List.of("att-contract"),
                now,
                now
        ));
        repository.saveExpiryItem(new ComplianceExpiryItemRecord(
                "cei_jdbc",
                "merchant",
                "merchant_jdbc",
                "merchant_jdbc",
                "alcohol_license",
                "Alcohol license",
                "LIC-001",
                "Market Authority",
                LocalDate.parse("2025-06-01"),
                LocalDate.parse("2026-06-10"),
                List.of(60, 30, 7),
                "block_operation",
                "active",
                "staff_compliance",
                "manual",
                true,
                List.of("att-license"),
                now,
                now
        ));
        repository.saveReminder(new ComplianceReminderRecord(
                "cr_jdbc",
                "merchant_jdbc",
                "compliance_expiry_item",
                "cei_jdbc",
                "expired",
                LocalDate.parse("2026-06-10"),
                LocalDate.parse("2026-06-14"),
                "critical",
                "open",
                true,
                "Alcohol license is expired",
                now
        ));

        assertThat(repository.findContractKeyDateById("ckd_jdbc"))
                .get()
                .extracting(
                        ContractKeyDateRecord::contractId,
                        ContractKeyDateRecord::dateType,
                        ContractKeyDateRecord::reminderDays,
                        ContractKeyDateRecord::humanConfirmed
                )
                .containsExactly("contract_jdbc", "contract_end", List.of(90, 30, 7), true);
        assertThat(repository.findAllExpiryItems())
                .extracting(ComplianceExpiryItemRecord::id, ComplianceExpiryItemRecord::expiryAction)
                .contains(tuple("cei_jdbc", "block_operation"));
        assertThat(repository.findAllReminders())
                .extracting(ComplianceReminderRecord::id, ComplianceReminderRecord::blockingSuggested)
                .contains(tuple("cr_jdbc", true));
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
