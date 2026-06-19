package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;

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
class WecomComplianceJdbcIntegrationTests {

    @Autowired
    private JdbcWecomComplianceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesWecomTablesSeedsDefaultPolicyAndPermissions() {
        assertThat(regclass("wecom_retention_policies")).isEqualTo("wecom_retention_policies");
        assertThat(regclass("wecom_conversations")).isEqualTo("wecom_conversations");
        assertThat(regclass("wecom_messages")).isEqualTo("wecom_messages");
        assertThat(regclass("wecom_access_audits")).isEqualTo("wecom_access_audits");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());

        // 默认留存策略：全局 180 天。
        assertThat(repository.findEffectivePolicy("any_project"))
                .get()
                .satisfies(policy -> {
                    assertThat(policy.retentionDays()).isEqualTo(180);
                    assertThat(policy.scopeType()).isEqualTo("global");
                });

        assertThat(permissionCodes()).contains(
                "wecom.view", "wecom.view_raw", "wecom.retention.manage", "wecom.sync");
        assertThat(platformAdminPermissionCodes()).contains(
                "wecom.view_raw", "wecom.retention.manage");
    }

    @Test
    void persistsConversationMessageAndAccessAuditThroughPostgres() {
        Instant now = Instant.parse("2026-06-18T00:00:00Z");
        WecomConversationRecord conversation = repository.saveConversation(new WecomConversationRecord(
                "wcv_jdbc", "ext_jdbc", "proj_jdbc", "staff_jdbc", "cust_jdbc",
                "active", LocalDate.parse("2026-12-31"), now, now));
        repository.saveMessage(new WecomMessageRecord(
                "wmsg_jdbc", conversation.id(), "staff_jdbc", "wecom-raw:ref",
                "电话 [PHONE_REDACTED]", List.of("private_payment"), false,
                now, LocalDate.parse("2026-06-18"), now));
        repository.saveAccessAudit(new WecomAccessAuditRecord(
                "wacc_jdbc", conversation.id(), "viewer_jdbc", "raw", "approval_jdbc", now));

        assertThat(repository.findMessagesByConversation("wcv_jdbc"))
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.contentRedacted()).isEqualTo("电话 [PHONE_REDACTED]");
                    assertThat(message.riskFlags()).containsExactly("private_payment");
                });
        assertThat(repository.findAccessAuditsByConversation("wcv_jdbc"))
                .extracting(WecomAccessAuditRecord::viewType, WecomAccessAuditRecord::approvalRef)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("raw", "approval_jdbc"));
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
        return jdbcTemplate.queryForList("SELECT permission_code FROM permissions ORDER BY permission_code", String.class);
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
