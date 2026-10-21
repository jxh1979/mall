package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerStoredValueAccountRecord;
import com.complex.platform.customer.StoredValueService;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-20-R3 票务退款：储值支付订单获准退款回原卡（CG-06a 第 12 条「票务……消费获准退款均回原卡，固定先回原消费尚未回冲的赠送金，
 * 再回本金；不扩大原退款资格」；ENUM「储值卡类型与收款主体」）。退款资格/比例沿既有审批，不改。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TicketRefundStoredValueJdbcIntegrationTests {

    @Autowired private JdbcTicketingRepository repository;
    @Autowired private TicketRefundService refundService;
    @Autowired private StoredValueService storedValueService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final Instant now = Instant.now();
    private String suffix;
    private String customerId;
    private String projectId;
    private String accountId;
    private String orderId;
    private String instA;
    private String instB;
    private String entityId;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        customerId = "cust_tk20r3_" + suffix;
        projectId = "prj_tk20r3_" + suffix;
        accountId = "svacc_tk20r3_" + suffix;
        orderId = "o_tk20r3_" + suffix;
        instA = "inst_tk20r3_a_" + suffix;
        instB = "inst_tk20r3_b_" + suffix;
        String phone = "139" + String.format("%08d", Math.abs(suffix.hashCode()) % 100_000_000);
        customerRepository.saveCustomer(new com.complex.platform.customer.CustomerRecord(
                customerId, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                accountId, customerId, projectId, null, 20_000L, "active", now, now, null, 15_000L, 5_000L));
        String ticketTypeId = "tt_tk20r3_" + suffix;
        repository.saveTicketType(new TicketTypeRecord(
                ticketTypeId, "储值退票票种", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, "published", null, null, now, now, null, 0, null, true, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        repository.saveTicketOrder(new TicketOrderRecord(
                orderId, "TO-" + suffix, customerId, ticketTypeId, null,
                2, java.time.LocalDate.now().plusDays(3), "miniapp", phone,
                19800L, null, 19800L, 0L, "CNY", "paid", "issued", "created", now, now, null, now, null, null));
        String templateId = "tpl_tk20r3_" + suffix;
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                templateId, "Entry", "entry", 1, 9900L, "active", null, null, null, null, now, now,
                null, null, null, null));
        repository.saveTicketInstance(new TicketInstanceRecord(
                instA, "TN-A-" + suffix, orderId, ticketTypeId, 1, 1, 1, customerId, customerId, "active", null, null, now, now));
        repository.saveTicketInstance(new TicketInstanceRecord(
                instB, "TN-B-" + suffix, orderId, ticketTypeId, 2, 1, 1, customerId, customerId, "active", null, null, now, now));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_tk20r3_a_" + suffix, "EA-" + suffix, ticketTypeId, orderId, templateId, "entry",
                null, null, null, 1, 0, 9900L, "active", now, now.plusSeconds(86400), now, now, 1, 9900L,
                null, null, null, null, instA, 1, null, null));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_tk20r3_b_" + suffix, "EB-" + suffix, ticketTypeId, orderId, templateId, "entry",
                null, null, null, 1, 0, 9900L, "active", now, now.plusSeconds(86400), now, now, 1, 9900L,
                null, null, null, null, instB, 2, null, null));
        // BSM:806 每笔退款须写明责任主体（主体主数据中在用的法律主体）。
        entityId = "be_tk20r3_" + suffix;
        jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, '退票责任主体', 'project_company', 'active', now(), now())", entityId);
        TestSecurityActors.asPlatformAdmin("usr_tk20r3_requester");
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 储值支付订单（扣本 150 赠 48 → 卡 0/2）：审批通过即回原卡，先回赠送金 48 再回本金 51，退款单直接 success。 */
    @Test
    void approvedRefundOfStoredValuePaidOrderCreditsCardBonusFirstAndIsSuccess() {
        payOrderWithStoredValue();
        TicketRefundRecord requested = requestRefund(instA, "tk20r3-r1-" + suffix);

        TestSecurityActors.asPlatformAdmin("usr_tk20r3_approver");
        TicketRefundRecord approved = refundService.approveRefund(requested.id(),
                new StatusChangeRequest("tk20r3-a1-" + suffix, "approve", "审批通过", List.of()));

        assertThat(approved.status()).isEqualTo("success");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ticket_refunds WHERE id = ?", String.class, requested.id()))
                .isEqualTo("success");
        assertThat(account()).containsEntry("principal_cent", 5_100L).containsEntry("bonus_cent", 5_000L);
        Map<String, Object> reversal = jdbcTemplate.queryForMap(
                "SELECT principal_cent, bonus_cent, original_txn_id FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = 'consume_reversal'",
                accountId);
        assertThat(reversal).containsEntry("principal_cent", 5_100L).containsEntry("bonus_cent", 4_800L);
        assertThat(reversal.get("original_txn_id")).isEqualTo(jdbcTemplate.queryForObject(
                "SELECT id FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = 'consume'", String.class, accountId));
    }

    /** 两张票分次退：累计回冲在原扣两段内，第二次只回本金；两单均 success。 */
    @Test
    void partialRefundTwiceAccumulatesWithinOriginalSegments() {
        payOrderWithStoredValue();
        TicketRefundRecord first = requestRefund(instA, "tk20r3-p1-" + suffix);
        TestSecurityActors.asPlatformAdmin("usr_tk20r3_approver");
        refundService.approveRefund(first.id(), new StatusChangeRequest("tk20r3-pa1-" + suffix, "approve", "审批通过", List.of()));

        TestSecurityActors.asPlatformAdmin("usr_tk20r3_requester");
        TicketRefundRecord second = requestRefund(instB, "tk20r3-p2-" + suffix);
        TestSecurityActors.asPlatformAdmin("usr_tk20r3_approver");
        TicketRefundRecord approved = refundService.approveRefund(second.id(),
                new StatusChangeRequest("tk20r3-pa2-" + suffix, "approve", "审批通过", List.of()));

        assertThat(approved.status()).isEqualTo("success");
        assertThat(account()).containsEntry("principal_cent", 15_000L).containsEntry("bonus_cent", 5_000L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = 'consume_reversal'", Integer.class, accountId))
                .isEqualTo(2);
    }

    /** 非储值支付订单：审批后仍停在 approved 等财务确认拨付，卡不动。 */
    @Test
    void cashPaidOrderRefundUnchanged() {
        repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(
                "tpi_tk20r3_" + suffix, "ticket_order", orderId, 19800L, "CNY", "pos", "succeeded", now, now));
        TicketRefundRecord requested = requestRefund(instA, "tk20r3-c1-" + suffix);

        TestSecurityActors.asPlatformAdmin("usr_tk20r3_approver");
        TicketRefundRecord approved = refundService.approveRefund(requested.id(),
                new StatusChangeRequest("tk20r3-ca1-" + suffix, "approve", "审批通过", List.of()));

        assertThat(approved.status()).isEqualTo("approved");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ticket_refunds WHERE id = ?", String.class, requested.id()))
                .isEqualTo("approved");
        assertThat(account()).containsEntry("principal_cent", 15_000L).containsEntry("bonus_cent", 5_000L);
    }

    // ---------- 造数 ----------

    private void payOrderWithStoredValue() {
        var debit = storedValueService.consumeInternal(customerId, projectId, null, 19800L,
                "票务订单储值支付 " + orderId, "ticketsv_" + orderId);
        repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(
                debit.id(), "ticket_order", orderId, 19800L, "CNY", "stored_value", "succeeded", now, now));
        assertThat(account()).containsEntry("principal_cent", 0L).containsEntry("bonus_cent", 200L);
    }

    private TicketRefundRecord requestRefund(String instanceId, String key) {
        return refundService.requestRefund(new TicketRefundRequest(
                key, "refund", List.of(), orderId, null, "客人退票", "original", null, List.of(instanceId), "ticket_type_rule", entityId));
    }

    private Map<String, Object> account() {
        return jdbcTemplate.queryForMap("SELECT principal_cent, bonus_cent, balance_cent FROM customer_stored_value_accounts WHERE id = ?", accountId);
    }
}
