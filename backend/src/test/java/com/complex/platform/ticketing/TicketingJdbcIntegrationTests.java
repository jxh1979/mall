package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
class TicketingJdbcIntegrationTests {

    @Autowired
    private JdbcTicketingRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesTicketingTablesAndSeedsPermissions() {
        assertThat(regclass("ticket_types")).isEqualTo("ticket_types");
        assertThat(regclass("entitlement_templates")).isEqualTo("entitlement_templates");
        assertThat(regclass("ticket_type_entitlements")).isEqualTo("ticket_type_entitlements");
        assertThat(regclass("issued_entitlements")).isEqualTo("issued_entitlements");
        assertThat(regclass("ticket_verifications")).isEqualTo("ticket_verifications");
        assertThat(regclass("terminal_sync_tasks")).isEqualTo("terminal_sync_tasks");
        assertThat(regclass("ticket_price_rules")).isEqualTo("ticket_price_rules");
        assertThat(regclass("ticket_orders")).isEqualTo("ticket_orders");
        assertThat(regclass("ticket_order_items")).isEqualTo("ticket_order_items");
        assertThat(regclass("ticket_credentials")).isEqualTo("ticket_credentials");
        assertThat(regclass("ticket_refunds")).isEqualTo("ticket_refunds");
        assertThat(regclass("ticket_refund_lines")).isEqualTo("ticket_refund_lines");

        assertThat(permissionCodes()).contains(
                "ticket.product.view",
                "ticket.product.manage",
                "ticket.entitlement.view",
                "ticket.entitlement.manage",
                "ticket.order.create",
                "ticket.price_variant.view",
                "ticket.price_variant.manage",
                "finance.payment_intent.create",
                "ticket.ticket.issue",
                "ticket.verify.view",
                "ticket.credential.view",
                "ticket.refund.view",
                "ticket.refund.request",
                "ticket.refund.approve",
                "verification.execute",
                "verification.view",
                "terminal.device.bind"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "ticket.product.manage",
                "ticket.entitlement.manage",
                "ticket.order.create",
                "ticket.price_variant.manage",
                "finance.payment_intent.create",
                "ticket.ticket.issue",
                "ticket.credential.view",
                "ticket.refund.view",
                "ticket.refund.approve",
                "verification.execute",
                "terminal.device.bind"
        );
    }

    @Test
    void createAndListTicketTypeAndEntitlementTemplatePersistsFormalFields() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_jdbc",
                "P0 JDBC Pass",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                false,
                "active",
                now,
                now
        ));
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "template_jdbc",
                "P0 JDBC Entry",
                "entry",
                2,
                9900L,
                "active",
                now,
                now
        ));

        assertThat(repository.listTicketTypes())
                .extracting(
                        TicketTypeRecord::id,
                        TicketTypeRecord::ticketName,
                        TicketTypeRecord::baseCalendarPriceMode,
                        TicketTypeRecord::weekdayPriceCent,
                        TicketTypeRecord::status
                )
                .contains(tuple("ticket_type_jdbc", "P0 JDBC Pass", "weekday_weekend_holiday", 9900L, "active"));
        assertThat(repository.listEntitlementTemplates())
                .extracting(
                        EntitlementTemplateRecord::id,
                        EntitlementTemplateRecord::name,
                        EntitlementTemplateRecord::entitlementType,
                        EntitlementTemplateRecord::defaultTotalTimes,
                        EntitlementTemplateRecord::defaultValueCent
                )
                .contains(tuple("template_jdbc", "P0 JDBC Entry", "entry", 2, 9900L));
    }

    @Test
    void persistIssuedEntitlementVerificationAndTerminalSyncTask() {
        Instant now = Instant.parse("2099-06-14T00:00:00Z");
        repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_jdbc_issue",
                "P0 JDBC Pass",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                false,
                "active",
                now,
                now
        ));
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "template_jdbc_issue",
                "P0 JDBC Entry",
                "entry",
                2,
                9900L,
                "active",
                now,
                now
        ));
        repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_jdbc",
                "ticket_type_jdbc_issue",
                "template_jdbc_issue",
                1,
                2,
                9900L,
                "default",
                now,
                now
        ));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_jdbc",
                "P0-JDBC",
                "ticket_type_jdbc_issue",
                "order_jdbc",
                "template_jdbc_issue",
                "entry",
                "mch_jdbc",
                "venue_jdbc",
                "gate_jdbc",
                2,
                0,
                9900L,
                "active",
                now,
                now.plusSeconds(86400),
                now,
                now
        ));
        repository.saveVerification(new VerificationRecord(
                "ver_jdbc",
                "V20260614000001",
                "ent_jdbc",
                "P0-JDBC",
                "qr",
                "P0-JDBC",
                "dev_jdbc",
                "gate_jdbc",
                "success",
                null,
                "android_terminal_online",
                null,
                now
        ));
        repository.saveTerminalSyncTask(new TerminalSyncTaskRecord(
                "sync_jdbc",
                "P0-ANDROID-JDBC",
                "staff_jdbc",
                "client_task_jdbc",
                "offline_verification",
                "accepted",
                null,
                "ver_jdbc",
                "V20260614000001",
                "2026-06-14T00:00:00Z",
                Map.of("entitlementCode", "P0-JDBC"),
                now
        ));

        assertThat(repository.listTicketTypeEntitlements("ticket_type_jdbc_issue"))
                .extracting(TicketTypeEntitlementRecord::id, TicketTypeEntitlementRecord::entitlementTemplateId)
                .contains(tuple("link_jdbc", "template_jdbc_issue"));
        assertThat(repository.findIssuedEntitlementByCode("P0-JDBC"))
                .get()
                .extracting(IssuedEntitlementRecord::orderId, IssuedEntitlementRecord::usedTimes)
                .containsExactly("order_jdbc", 0);
        assertThat(repository.listOfflineEntitlements("mch_jdbc", "venue_jdbc", 20))
                .extracting(OfflineEntitlementResponse::entitlementCode, OfflineEntitlementResponse::totalTimes)
                .contains(tuple("P0-JDBC", 2));
        assertThat(repository.listVerifications())
                .extracting(VerificationRecord::verificationNo, VerificationRecord::status)
                .contains(tuple("V20260614000001", "success"));
        assertThat(repository.findTerminalSyncTask("P0-ANDROID-JDBC", "client_task_jdbc"))
                .get()
                .extracting(TerminalSyncTaskRecord::verificationNo)
                .isEqualTo("V20260614000001");
    }

    @Test
    void persistTicketOrderCredentialAndRefundSliceTables() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_order_jdbc",
                "P0 Order Pass",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                false,
                "published",
                now,
                now
        ));
        repository.savePriceRule(new TicketPriceRuleRecord(
                "price_rule_jdbc",
                "ticket_type_order_jdbc",
                "student",
                "fixed_price",
                6900L,
                7900L,
                8900L,
                null,
                "miniapp",
                "CNY",
                "active",
                Map.of("student", "verified"),
                now,
                now
        ));
        repository.saveTicketOrder(new TicketOrderRecord(
                "ticket_order_jdbc",
                "TO20260614000001",
                "customer_jdbc",
                "ticket_type_order_jdbc",
                "price_rule_jdbc",
                2,
                java.time.LocalDate.parse("2026-06-20"),
                "miniapp",
                "13800138000",
                15800L,
                "CNY",
                "unpaid",
                "not_issued",
                "created",
                now,
                now
        ));
        repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(
                "pay_intent_jdbc",
                "ticket_order",
                "ticket_order_jdbc",
                15800L,
                "CNY",
                "aggregate_pay",
                "created",
                now,
                now
        ));
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "template_order_jdbc",
                "P0 Entry",
                "entry",
                1,
                7900L,
                "active",
                now,
                now
        ));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_order_jdbc",
                "P0-ORDER-JDBC",
                "ticket_type_order_jdbc",
                "ticket_order_jdbc",
                "template_order_jdbc",
                "entry",
                null,
                null,
                null,
                1,
                0,
                7900L,
                "active",
                now,
                now.plusSeconds(86400),
                now,
                now
        ));
        repository.saveTicketCredential(new TicketCredentialRecord(
                "credential_jdbc",
                "ent_order_jdbc",
                "ticket_order_jdbc",
                "cred_jdbc",
                "nonce_jdbc",
                "hash_jdbc",
                "dynamic_qr",
                now,
                now.plusSeconds(300),
                "active",
                1,
                now,
                now
        ));
        repository.saveTicketRefund(new TicketRefundRecord(
                "refund_jdbc",
                "ticket_order_jdbc",
                7900L,
                "customer request",
                "original",
                "requested",
                null,
                null,
                now,
                now
        ));
        repository.saveTicketRefundLine(new TicketRefundLineRecord(
                "refund_line_jdbc",
                "refund_jdbc",
                "ticket_order_jdbc",
                "ent_order_jdbc",
                "entry",
                "entitlement",
                7900L,
                1,
                0,
                "requested",
                now,
                now
        ));

        assertThat(repository.listPriceRules())
                .extracting(TicketPriceRuleRecord::id, TicketPriceRuleRecord::currency)
                .contains(tuple("price_rule_jdbc", "CNY"));
        assertThat(repository.findTicketOrderById("ticket_order_jdbc"))
                .get()
                .extracting(TicketOrderRecord::amountCent, TicketOrderRecord::currency)
                .containsExactly(15800L, "CNY");
        assertThat(repository.findTicketCredentialByToken("cred_jdbc"))
                .get()
                .extracting(TicketCredentialRecord::entitlementId)
                .isEqualTo("ent_order_jdbc");
        assertThat(repository.findTicketRefundById("refund_jdbc"))
                .get()
                .extracting(TicketRefundRecord::status)
                .isEqualTo("requested");
        assertThat(repository.listTicketRefundLines("refund_jdbc"))
                .extracting(TicketRefundLineRecord::id, TicketRefundLineRecord::refundAmountCent, TicketRefundLineRecord::status)
                .contains(tuple("refund_line_jdbc", 7900L, "requested"));
        assertThat(flywayMigrationSucceeded("009")).isTrue();
        assertThat(flywayMigrationSucceeded("031")).isTrue();
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    private boolean flywayMigrationSucceeded(String version) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                      FROM flyway_schema_history
                     WHERE version = ?
                       AND success = TRUE
                )
                """, Boolean.class, version));
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
