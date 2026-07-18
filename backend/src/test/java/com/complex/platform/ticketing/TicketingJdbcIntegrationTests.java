package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
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
        assertThat(regclass("ticket_credentials")).isEqualTo("ticket_credentials");
        assertThat(regclass("ticket_refunds")).isEqualTo("ticket_refunds");
        assertThat(regclass("ticket_refund_lines")).isEqualTo("ticket_refund_lines");
        assertThat(regclass("ticket_dynamic_pricing_rules")).isEqualTo("ticket_dynamic_pricing_rules");

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
                "ticket.dynamic_pricing.view",
                "ticket.dynamic_pricing.manage",
                "ticket.dynamic_pricing.publish",
                "ticket.dynamic_pricing.simulate",
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
                "ticket.dynamic_pricing.view",
                "ticket.dynamic_pricing.manage",
                "ticket.dynamic_pricing.publish",
                "ticket.dynamic_pricing.simulate",
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
                "active",null,null,
                now,
                now
        ));
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "template_jdbc",
                "P0 JDBC Entry",
                "entry",
                2,
                9900L,
                "active",null,null,null,null,
                now,
                now
        ,
        null, null));

        assertThat(repository.listTicketTypes())
                .extracting(
                        TicketTypeRecord::id,
                        TicketTypeRecord::ticketName,
                        TicketTypeRecord::baseCalendarPriceMode,
                        TicketTypeRecord::weekdayPriceCent,
                        TicketTypeRecord::status
                )
                .contains(tuple("ticket_type_jdbc", "P0 JDBC Pass", "weekday_weekend_holiday", 9900L, "active"));
        assertThat(repository.listEntitlementTemplates(false))
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
                "active",null,null,
                now,
                now
        ));
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "template_jdbc_issue",
                "P0 JDBC Entry",
                "entry",
                2,
                9900L,
                "active",null,null,null,null,
                now,
                now
        ,
        null, null));
        repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_jdbc",
                "ticket_type_jdbc_issue",
                "template_jdbc_issue",
                1,
                2,
                9900L,
                "default",
                1,
                null,
                now,
                now
        ));
        // V159 起 issued_entitlements.order_id 外键引用 ticket_orders，先建父订单避免 FK 违反。
        repository.saveTicketOrder(new TicketOrderRecord(
                "order_jdbc", "ON-JDBC", "cust_jdbc", "ticket_type_jdbc_issue", null, 2,
                java.time.LocalDate.parse("2099-06-14"), "web", null, 9900L, "CNY", "paid", "issued", "created", now, now));
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
                now,
                3,
                null
        ,
        null, null));
        // V159 起 ticket_verifications.device_id 外键引用 terminal_devices，先建父设备避免 FK 违反。
        seedTerminalDevice("dev_jdbc", now);
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
                now,
                "nonce_jdbc",
                "staff_jdbc",
                "mch_jdbc",
                Boolean.TRUE,
                null,
                3,
                null,
                null,
                null
        ,
        null));
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
                Map.of("entitlementCode", "P0-JDBC", "credentialPayload", "cred_terminal_jdbc"),
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
        // T4：核销记录回读 nonce/staff_id/merchant_id/window_valid 五列（signature_valid 本期为 P1 占位，恒 null）。
        assertThat(repository.listVerifications())
                .filteredOn(v -> "ver_jdbc".equals(v.id()))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.nonce()).isEqualTo("nonce_jdbc");
                    assertThat(v.staffId()).isEqualTo("staff_jdbc");
                    assertThat(v.merchantId()).isEqualTo("mch_jdbc");
                    assertThat(v.windowValid()).isTrue();
                    assertThat(v.signatureValid()).isNull();
                });
        TerminalSyncTaskRecord syncTask = repository.findTerminalSyncTask("P0-ANDROID-JDBC", "client_task_jdbc")
                .orElseThrow();
        assertThat(syncTask.verificationNo()).isEqualTo("V20260614000001");
        assertThat(syncTask.payload().get("credentialPayload")).isEqualTo("sha256:" + sha256("cred_terminal_jdbc"));
        assertThat(syncTask.payload().values()).doesNotContain("cred_terminal_jdbc");
    }

    // R5 T5：ticket_verifications.status CHECK 收口——非法状态被拒，manual_overridden/reversed 合法。
    @Test
    void ticketVerificationStatusCheckRejectsUnknownStatus() {
        Instant now = Instant.parse("2026-06-18T00:00:00Z");
        seedIssuedEntitlementForStatusCheck(now);

        // 合法状态 manual_overridden 必须通过（漏掉会破坏人工放行流）。
        repository.saveVerification(verificationWithStatus("ver_chk_mo", "V20260618CHK01", now, "manual_overridden"));
        assertThat(repository.findVerificationById("ver_chk_mo")).isPresent();

        // 非法状态被 CHECK 拒绝。
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        repository.saveVerification(verificationWithStatus("ver_chk_bad", "V20260618CHK02", now, "bogus_status")))
                .hasMessageContaining("chk_ticket_verifications_status");
    }

    // V300：验证二维码刷新窗口白名单放宽 {10,15,30,60}——CHECK 收 15、拒 20（默认 15 由 DDL DEFAULT 承载）。
    @Test
    void ticketCredentialRefreshIntervalCheckAcceptsFifteenRejectsTwenty() {
        Instant now = Instant.parse("2026-06-18T00:00:00Z");
        seedIssuedEntitlementForStatusCheck(now);
        repository.saveTicketCredential(new TicketCredentialRecord(
                "credential_chk_15", "ent_chk", "order_chk", "cred_chk_15", "nonce_chk_15",
                "hash_chk_15", "dynamic_qr", now, now.plusSeconds(15), "active", 0, now, now, 15, now, null, null));
        assertThat(repository.findTicketCredentialByToken("cred_chk_15")).isPresent();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        repository.saveTicketCredential(new TicketCredentialRecord(
                                "credential_chk_20", "ent_chk", "order_chk", "cred_chk_20", "nonce_chk_20",
                                "hash_chk_20", "dynamic_qr", now, now.plusSeconds(20), "active", 0, now, now, 20, now, null, null)))
                .hasMessageContaining("chk_ticket_credentials_refresh_interval");
    }

    @Test
    void issuedEntitlementPersistsNumPeople() {
        Instant now = Instant.parse("2026-06-18T00:00:00Z");
        seedIssuedEntitlementForStatusCheck(now);
        assertThat(repository.findIssuedEntitlementByCode("P0-CHK"))
                .get().extracting(IssuedEntitlementRecord::numPeople).isEqualTo(4);
    }

    private void seedIssuedEntitlementForStatusCheck(Instant now) {
        repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_chk", "P0 Chk", "weekday_weekend_holiday", 9900L, 11900L, 12900L, false, "published",null,null, now, now));
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "template_chk", "P0 Entry", "entry", 4, 9900L, "active",null,null,null,null, now, now,
                null, null));
        // V159 起 issued_entitlements.order_id 外键引用 ticket_orders，先建父订单避免 FK 违反。
        repository.saveTicketOrder(new TicketOrderRecord(
                "order_chk", "ON-CHK", "cust_chk", "ticket_type_chk", null, 4,
                java.time.LocalDate.parse("2026-06-18"), "web", null, 9900L, "CNY", "paid", "issued", "created", now, now));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_chk", "P0-CHK", "ticket_type_chk", "order_chk", "template_chk", "entry",
                "mch_chk", "venue_chk", "gate_chk", 4, 0, 9900L, "active",
                now, now.plusSeconds(86400), now, now, 4, null,
                null, null));
        // V159 起 ticket_verifications.device_id 外键引用 terminal_devices，先建父设备避免 FK 违反。
        seedTerminalDevice("dev_chk", now);
    }

    // V159：ticket_verifications.device_id → terminal_devices(id) 外键的父设备行（device_id/device_code 唯一）。
    private void seedTerminalDevice(String id, Instant now) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?)
                """, id, "dev-" + id, "DC-" + id, ts, ts);
    }

    private VerificationRecord verificationWithStatus(String id, String no, Instant now, String status) {
        return new VerificationRecord(
                id, no, "ent_chk", "P0-CHK", "qr", "P0-CHK", "dev_chk", "gate_chk",
                status, null, "android_terminal_online", null, now,
                "nonce_" + id, "staff_chk", "mch_chk", Boolean.TRUE, null, 1, null, null, null,
                null);
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
                "published",null,null,
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
                "active",null,null,null,null,
                now,
                now
        ,
        null, null));
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
                now,
                1,
                null
        ,
        null, null));
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
                now,
                60,
                now,
                null,
                null
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
                null,
                now,
                now,
                "merchant_jdbc"
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
        // T4：凭证回读 window_started_at 锚点 + 预留签名位（signature_value/version 本期为 P1，恒 null）。
        assertThat(repository.findTicketCredentialByToken("cred_jdbc"))
                .get()
                .satisfies(c -> {
                    assertThat(c.windowStartedAt()).isNotNull();
                    assertThat(c.signatureValue()).isNull();
                    assertThat(c.signatureVersion()).isNull();
                });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT credential_token FROM ticket_credentials WHERE id = 'credential_jdbc'",
                String.class
        )).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT credential_hash FROM ticket_credentials WHERE id = 'credential_jdbc'", String.class))
                .isEqualTo(sha256("cred_jdbc"));
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

    @Test
    void requestRefundWholeTicketRejectsAmountMismatchAndPersistsNothing() {
        // 方案2 真实后端活证据（走 JdbcTicketingRepository + Postgres + Flyway V285/V286）：整票退款——
        // 传部分/自由金额（≠整票额）被拒且 DB 无退款行（钱没动）；不带金额=服务端算定整票额可退并落库。
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveTicketType(new TicketTypeRecord(
                "tt_share_live", "Share Pass", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, "published", null, null, now, now, null, 0, null, true));
        repository.saveTicketOrder(new TicketOrderRecord(
                "o_share_live", "TO-SHARE-LIVE", "customer_share_live", "tt_share_live", null,
                2, java.time.LocalDate.parse("2026-06-20"), "miniapp", "13800138000",
                19800L, "CNY", "paid", "issued", "created", now, now));
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_share_live", "Entry", "entry", 1, 9900L, "active", null, null, null, null, now, now,
                null, null));
        // 两张父票实例，各一条挂父票的权益。
        repository.saveTicketInstance(new TicketInstanceRecord(
                "inst_share_a", "TN-A", "o_share_live", "tt_share_live", 1, 1, 1,
                "customer_share_live", "customer_share_live", "active", null, null, now, now));
        repository.saveTicketInstance(new TicketInstanceRecord(
                "inst_share_b", "TN-B", "o_share_live", "tt_share_live", 2, 1, 1,
                "customer_share_live", "customer_share_live", "active", null, null, now, now));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_share_a", "SHARE-A", "tt_share_live", "o_share_live", "tpl_share_live", "entry",
                null, null, null, 1, 0, 9900L, "active", now, now.plusSeconds(86400), now, now, 1, 9900L,
                null, null, null, null, "inst_share_a", 1));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_share_b", "SHARE-B", "tt_share_live", "o_share_live", "tpl_share_live", "entry",
                null, null, null, 1, 0, 9900L, "active", now, now.plusSeconds(86400), now, now, 1, 9900L,
                null, null, null, null, "inst_share_b", 2));

        TicketRefundService refundService = new TicketRefundService(repository, event -> { });

        // 传金额≠整票额（选一张票传 19800）→ REFUND_AMOUNT_MISMATCH。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> refundService.requestRefund(
                TicketRefundRequest.ofTicketInstances(
                        "share-live-over", "refund", List.of(), "o_share_live", 19800L, "customer request", "original",
                        List.of("inst_share_a"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("REFUND_AMOUNT_MISMATCH"));
        // 活证据：被拒后 DB 无该单退款行（钱没动）。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_refunds WHERE target_id = ?", Long.class, "o_share_live"))
                .isEqualTo(0L);

        // 不带金额=服务端算定整票额（9900）可退并落库。
        TicketRefundRecord ok = refundService.requestRefund(TicketRefundRequest.ofTicketInstances(
                "share-live-ok", "refund", List.of(), "o_share_live", null, "customer request", "original",
                List.of("inst_share_a")));
        assertThat(ok.refundAmountCent()).isEqualTo(9900L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_refunds WHERE target_id = ?", Long.class, "o_share_live"))
                .isEqualTo(1L);
    }


    @Test
    void persistDynamicPricingRuleSliceTables() {
        Instant now = Instant.parse("2026-06-15T00:00:00Z");
        repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_dynamic_jdbc",
                "P2 Dynamic Pass",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                false,
                "published",null,null,
                now,
                now
        ));
        TicketDynamicPricingRuleRecord draft = repository.saveDynamicPricingRule(new TicketDynamicPricingRuleRecord(
                "dynamic_rule_jdbc",
                "ticket_type_dynamic_jdbc",
                "Summer weekend uplift",
                100,
                java.time.LocalDate.parse("2026-06-01"),
                java.time.LocalDate.parse("2026-06-30"),
                "web",
                "percent_delta",
                new java.math.BigDecimal("20.00"),
                null,
                9900L,
                15000L,
                "draft",
                Map.of("inventory_level", "low"),
                "ops create",
                List.of("att_dynamic"),
                now,
                now
        ));
        repository.saveDynamicPricingRule(new TicketDynamicPricingRuleRecord(
                draft.id(),
                draft.ticketTypeId(),
                draft.ruleName(),
                draft.priority(),
                draft.effectiveFrom(),
                draft.effectiveTo(),
                draft.salesChannel(),
                draft.adjustmentType(),
                draft.adjustmentRate(),
                draft.adjustmentAmountCent(),
                draft.minPriceCent(),
                draft.maxPriceCent(),
                "published",
                draft.conditionRule(),
                "ops publish",
                List.of("att_publish"),
                draft.createdAt(),
                now.plusSeconds(60)
        ));

        assertThat(repository.findDynamicPricingRuleById("dynamic_rule_jdbc"))
                .get()
                .extracting(
                        TicketDynamicPricingRuleRecord::status,
                        TicketDynamicPricingRuleRecord::salesChannel,
                        TicketDynamicPricingRuleRecord::conditionRule,
                        TicketDynamicPricingRuleRecord::evidenceAttachmentIds
                )
                .containsExactly("published", "web", Map.of("inventory_level", "low"), List.of("att_publish"));
        assertThat(repository.listDynamicPricingRules())
                .extracting(TicketDynamicPricingRuleRecord::id, TicketDynamicPricingRuleRecord::priority)
                .contains(tuple("dynamic_rule_jdbc", 100));
        assertThat(flywayMigrationSucceeded("036")).isTrue();
    }

    @Test
    void ticketOrderDynamicTraceColumnsRoundTripThroughJdbc() {
        Instant now = Instant.parse("2026-06-16T00:00:00Z");
        repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_trace_jdbc",
                "P0 Trace Pass",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                false,
                "published",null,null,
                now,
                now
        ));
        long baseAmountCent = 11900L;
        long dynamicAdjustmentCent = -2000L;
        long amountCent = baseAmountCent + dynamicAdjustmentCent;
        // 订单 dynamic_pricing_rule_id 有 FK 约束，先建被引用的规则行
        repository.saveDynamicPricingRule(new TicketDynamicPricingRuleRecord(
                "dynamic_rule_trace_jdbc",
                "ticket_type_trace_jdbc",
                "Trace rule",
                100,
                java.time.LocalDate.parse("2026-06-01"),
                java.time.LocalDate.parse("2026-06-30"),
                "web",
                "percent_delta",
                new java.math.BigDecimal("20.00"),
                null,
                9900L,
                15000L,
                "published",
                Map.of(),
                "ops create",
                List.of(),
                now,
                now
        ));
        repository.saveTicketOrder(new TicketOrderRecord(
                "ticket_order_trace_jdbc",
                "TO20260616000001",
                "customer_trace_jdbc",
                "ticket_type_trace_jdbc",
                null,
                1,
                java.time.LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                amountCent,
                "dynamic_rule_trace_jdbc",
                baseAmountCent,
                dynamicAdjustmentCent,
                "CNY",
                "unpaid",
                "not_issued",
                "created",
                now,
                now
        ));

        assertThat(repository.findTicketOrderById("ticket_order_trace_jdbc"))
                .get()
                .extracting(
                        TicketOrderRecord::dynamicPricingRuleId,
                        TicketOrderRecord::baseAmountCent,
                        TicketOrderRecord::dynamicAdjustmentCent,
                        TicketOrderRecord::amountCent
                )
                .containsExactly("dynamic_rule_trace_jdbc", baseAmountCent, dynamicAdjustmentCent, amountCent);

        TicketOrderRecord reloaded = repository.findTicketOrderById("ticket_order_trace_jdbc").orElseThrow();
        assertThat(reloaded.baseAmountCent() + reloaded.dynamicAdjustmentCent()).isEqualTo(reloaded.amountCent());
        // 审计 F-P1:购票人手机号落库为密文(enc:v1: 信封),读回明文(PiiCipher 透明加解密)。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT buyer_phone FROM ticket_orders WHERE id = 'ticket_order_trace_jdbc'", String.class))
                .startsWith("enc:v1:");
        assertThat(reloaded.buyerPhone()).isEqualTo("13800138000");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT dynamic_pricing_rule_id FROM ticket_orders WHERE id = 'ticket_order_trace_jdbc'",
                String.class
        )).isEqualTo("dynamic_rule_trace_jdbc");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT base_amount_cent FROM ticket_orders WHERE id = 'ticket_order_trace_jdbc'",
                Long.class
        )).isEqualTo(baseAmountCent);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT dynamic_adjustment_cent FROM ticket_orders WHERE id = 'ticket_order_trace_jdbc'",
                Long.class
        )).isEqualTo(dynamicAdjustmentCent);
    }

    @Test
    void ticketCategoryPersistsReadsBackAndRejectsIllegalValue() {
        Instant now = Instant.parse("2026-07-04T00:00:00Z");
        // 带分类=student → 读回 student。
        repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_cat_student", "学生票", "weekday_weekend_holiday",
                4900L, 5900L, 6900L, false, "active", null, null,
                now, now, null, 0, "student"));
        // 不带分类（14 参兼容构造）→ NULL（标准，回归）。
        repository.saveTicketType(new TicketTypeRecord(
                "ticket_type_cat_default", "标准票", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, "active", null, null,
                now, now, null, 0));

        assertThat(repository.findTicketTypeById("ticket_type_cat_student"))
                .get().extracting(TicketTypeRecord::ticketCategory).isEqualTo("student");
        assertThat(repository.findTicketTypeById("ticket_type_cat_default"))
                .get().extracting(TicketTypeRecord::ticketCategory).isNull();

        // CHECK 拒非法值。
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                repository.saveTicketType(new TicketTypeRecord(
                        "ticket_type_cat_bad", "非法", "weekday_weekend_holiday",
                        1L, 1L, 1L, false, "active", null, null,
                        now, now, null, 0, "vip")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void merchantOrderListIncludesGuestBoughtSingleBarOrdersByBarId() {
        // AUD 复核 P2-7：客人端自助购本店单店票的订单 merchant_id=null、bar_id=酒吧；本店后台按 bar_id 也须可见（读写归属一致）。
        jdbcTemplate.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at) "
                + "VALUES ('tt_p27','单店票','fixed','active', now(), now()) ON CONFLICT (id) DO NOTHING");
        String barId = "bar_p27_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        // bar_id 有外键 → merchants(id)，先落一条酒吧商户行。
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at) "
                + "VALUES (?, '话梅酒馆', 'bar', 'active', now(), now()) ON CONFLICT (id) DO NOTHING", barId);
        String guestOrderId = "ord_guest_" + barId;    // 客人自助：merchant_id=null，bar_id=barId
        String merchantOrderId = "ord_mer_" + barId;    // 商户代售：merchant_id=barId
        jdbcTemplate.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, "
                + "pay_status, issue_status, status, merchant_id, bar_id, created_at, updated_at) "
                + "VALUES (?, ?, 'tt_p27', 1, 'web', 100, 'paid', 'not_issued', 'created', NULL, ?, now(), now())",
                guestOrderId, "ONO_" + guestOrderId, barId);
        jdbcTemplate.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, "
                + "pay_status, issue_status, status, merchant_id, bar_id, created_at, updated_at) "
                + "VALUES (?, ?, 'tt_p27', 1, 'admin', 100, 'paid', 'not_issued', 'created', ?, ?, now(), now())",
                merchantOrderId, "ONO_" + merchantOrderId, barId, barId);
        List<String> ids = repository.listTicketOrdersByMerchant(barId).stream()
                .map(TicketOrderRecord::id).toList();
        assertThat(ids).contains(guestOrderId, merchantOrderId);
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

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
