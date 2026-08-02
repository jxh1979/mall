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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
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
        assertThat(jdbcTemplate.queryForList("""
                SELECT view_type, approval_ref FROM wecom_access_audits
                 WHERE conversation_id = 'wcv_jdbc' ORDER BY viewed_at DESC, id
                """))
                .extracting(row -> org.assertj.core.groups.Tuple.tuple(row.get("view_type"), row.get("approval_ref")))
                .containsExactly(org.assertj.core.groups.Tuple.tuple("raw", "approval_jdbc"));

        // P2 多综合体 project_id scoped 读通道：按 project_id 行级隔离，仅命中本 project 会话。
        assertThat(repository.findConversationsByProject("proj_jdbc"))
                .extracting(WecomConversationRecord::id)
                .contains("wcv_jdbc");
        assertThat(repository.findConversationsByProject("proj_nonexistent")).isEmpty();
    }

    @Test
    void migrationCreatesRiskHitsTableSeedsWecomRiskRulesAndAddsSourceDomain() {
        // P0#8：命中表建表 + 企微违规规则码 seed（compliance 域）+ analytics_risk_alerts.source_domain 列。
        assertThat(regclass("wecom_risk_hits")).isEqualTo("wecom_risk_hits");

        // 五个企微违规规则码落库且启用（domain=compliance）。
        List<String> ruleCodes = jdbcTemplate.queryForList("""
                SELECT rule_code FROM analytics_risk_rules
                 WHERE domain = 'compliance' AND enabled = true
                 ORDER BY rule_code
                """, String.class);
        assertThat(ruleCodes).contains(
                "STAFF_PRIVATE_DEAL", "STAFF_PRIVATE_PAYMENT", "ABUSE",
                "ILLEGAL_PROMISE", "LURE_PRIVATE_WECHAT");

        // 仓储读企微规则码（关键词来自 rule_config_json.keywords）。
        assertThat(repository.findEnabledWecomRiskRules())
                .extracting(WecomRiskRuleRecord::ruleCode)
                .contains("LURE_PRIVATE_WECHAT", "STAFF_PRIVATE_PAYMENT");

        // analytics_risk_alerts 新增可空 source_domain 列。
        assertThat(columnExists("analytics_risk_alerts", "source_domain")).isTrue();

        // 命中查询/复核权限落库且 platform_admin 拥有。
        assertThat(permissionCodes()).contains("wecom.risk_hit.view", "wecom.risk_hit.review");
        assertThat(platformAdminPermissionCodes()).contains("wecom.risk_hit.view", "wecom.risk_hit.review");
    }

    @Test
    void persistsRiskHitAndEnqueuesAlertWithSourceDomainWecom() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        repository.saveConversation(new WecomConversationRecord(
                "wcv_hit", "ext_hit", "proj_hit", "staff_hit", "cust_hit",
                "active", LocalDate.parse("2026-12-31"), now, now));
        repository.saveMessage(new WecomMessageRecord(
                "wmsg_hit", "wcv_hit", "staff_hit", "wecom-raw:ref",
                "加我私微", List.of("lure_private_contact"), false,
                now, LocalDate.parse("2026-06-19"), now));

        String alertId = repository.enqueueRiskAlert(new WecomRiskAlertEnqueue(
                "ara_hit", "rule_wecom_lure_private_wechat", "high", "wecom_message", "wmsg_hit",
                "WeCom compliance hit: LURE_PRIVATE_WECHAT", "wecom-hit:wmsg_hit:LURE_PRIVATE_WECHAT", now));
        repository.saveRiskHit(new WecomRiskHitRecord(
                "wrh_hit", "wcv_hit", "wmsg_hit", "LURE_PRIVATE_WECHAT", "high", "私微",
                "open", null, null, "proj_hit", alertId, now, now));

        assertThat(repository.findRiskHits("open", "wcv_hit"))
                .singleElement()
                .satisfies(hit -> {
                    assertThat(hit.ruleCode()).isEqualTo("LURE_PRIVATE_WECHAT");
                    assertThat(hit.status()).isEqualTo("open");
                    assertThat(hit.alertId()).isEqualTo(alertId);
                });
        // 复核流转 open → confirmed。
        repository.updateRiskHitReview("wrh_hit", "confirmed", "reviewer_jdbc", "确认");
        assertThat(repository.findRiskHitById("wrh_hit"))
                .get()
                .satisfies(hit -> {
                    assertThat(hit.status()).isEqualTo("confirmed");
                    assertThat(hit.reviewerId()).isEqualTo("reviewer_jdbc");
                });
        // 入复核队列写孤岛 analytics_risk_alerts，source_domain=wecom。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT source_domain FROM analytics_risk_alerts WHERE id = ?", String.class, alertId))
                .isEqualTo("wecom");

        // P2 多综合体 project_id scoped 读通道：命中按 project_id 行级隔离。
        assertThat(repository.findRiskHitsByProject("proj_hit"))
                .extracting(WecomRiskHitRecord::hitId)
                .contains("wrh_hit");
        assertThat(repository.findRiskHitsByProject("proj_nonexistent")).isEmpty();
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_name = ? AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
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
