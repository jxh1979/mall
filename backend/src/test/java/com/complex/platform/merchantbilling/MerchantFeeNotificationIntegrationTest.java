package com.complex.platform.merchantbilling;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.contract.ContractRenewalNotificationJob;
import com.complex.platform.merchantbilling.dto.MerchantFeeNotificationRecipientRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillCreateRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDunningRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillLineRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
class MerchantFeeNotificationIntegrationTest {

    private static final String PHONE_PREFIX = "15577";

    @Autowired
    private MerchantBillingService merchantBillingService;

    @Autowired
    private MerchantFeeNotificationRecipientService recipientService;

    @Autowired
    private ContractRenewalNotificationJob contractRenewalNotificationJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // V339（P0-A/GAP-07）：merchants.contact_phone 落库必须是 enc:v1: 信封密文（DB CHECK 强制）。
    // 本测试绕过仓库直插，需自己加密后再写。
    @Autowired
    private com.complex.platform.common.crypto.PiiCipher piiCipher;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        cleanupData();
        setSwitch("sms_notification", true);
        setSwitch("sms.notify.merchant_fee", true);
        setSwitch("sms.notify.merchant_fee.rent", true);
        setSwitch("sms.notify.merchant_fee.utilities", true);
        setSwitch("sms.notify.merchant_fee.property", true);
        setSwitch("sms.notify.merchant_fee.renewal", true);
    }

    @AfterEach
    void tearDown() {
        cleanupData();
        setSwitch("sms_notification", false);
        setSwitch("sms.notify.merchant_fee", true);
        setSwitch("sms.notify.merchant_fee.rent", true);
        setSwitch("sms.notify.merchant_fee.utilities", true);
        setSwitch("sms.notify.merchant_fee.property", true);
        setSwitch("sms.notify.merchant_fee.renewal", true);
        TestSecurityActors.clear();
    }

    @Test
    void createBillWritesRentNotificationLogWhenSwitchesOn() {
        String prefix = unique("bill");
        String merchantId = prefix + "_merchant";
        String arApItemId = prefix + "_arap";
        String phone = uniquePhone();
        insertMerchant(merchantId, phone);
        insertArApItem(arApItemId, merchantId, 168_800L);

        MerchantBillRecord bill = merchantBillingService.createBill(createBillRequest(
                prefix + "_create",
                merchantId,
                arApItemId,
                168_800L,
                List.of(
                        new MerchantBillLineRequest("rent", "June rent", 128_800L),
                        new MerchantBillLineRequest("cam", "June CAM", 40_000L)
                )
        ));

        assertThat(logCount("rent", phone)).isEqualTo(1);
        assertThat(logValue("rent", phone, "request_id")).isEqualTo("mfbill:" + bill.id() + ":rent");
        assertThat(logValue("rent", phone, "template_code")).isEqualTo("sms.merchant_fee.rent");
        assertThat(logValue("rent", phone, "status")).isEqualTo("sent");
        assertThat(logParam("rent", phone, "bill_id")).isEqualTo(bill.id());
        assertThat(logParam("rent", phone, "merchant_id")).isEqualTo(merchantId);
        assertThat(logParam("rent", phone, "amount_cent")).isEqualTo("128800");
        assertThat(logCount("utilities", phone)).isZero();
        assertThat(logCount("property", phone)).isZero();
    }

    @Test
    void recordDunningWritesMerchantFeeNotificationLog() {
        String prefix = unique("dunning");
        String merchantId = prefix + "_merchant";
        String arApItemId = prefix + "_arap";
        String phone = uniquePhone();
        insertMerchant(merchantId, phone);
        insertArApItem(arApItemId, merchantId, 168_800L);
        MerchantBillRecord bill = merchantBillingService.createBill(createBillRequest(
                prefix + "_create",
                merchantId,
                arApItemId,
                168_800L,
                List.of(
                        new MerchantBillLineRequest("rent", "June rent", 128_800L),
                        new MerchantBillLineRequest("cam", "June CAM", 40_000L)
                )
        ));

        MerchantBillDunningRecord dunning = merchantBillingService.recordDunning(
                bill.id(),
                new MerchantBillDunningRequest(
                        prefix + "_dunning",
                        "first reminder",
                        "sms",
                        "rent_overdue_first_notice",
                        "sent",
                        Instant.parse("2026-06-29T02:00:00Z"),
                        "staff_finance"
                ));

        String requestId = "mfdun:" + dunning.id() + ":rent";
        assertThat(logCountByRequestId(requestId)).isEqualTo(1);
        assertThat(logValueByRequestId(requestId, "domain")).isEqualTo("merchant_fee");
        assertThat(logValueByRequestId(requestId, "event_key")).isEqualTo("rent");
        assertThat(logValueByRequestId(requestId, "recipient")).isEqualTo(phone);
        assertThat(logValueByRequestId(requestId, "template_code")).isEqualTo("sms.merchant_fee.rent");
        assertThat(logValueByRequestId(requestId, "status")).isEqualTo("sent");
        assertThat(logParamByRequestId(requestId, "dunning_id")).isEqualTo(dunning.id());
        assertThat(logParamByRequestId(requestId, "dunning_channel")).isEqualTo("sms");
    }

    @Test
    void renewalScanWritesMerchantFeeRenewalNotificationLog() {
        LocalDate asOf = LocalDate.parse("2026-06-29");
        LocalDate expiryDate = asOf.plusDays(10);
        String prefix = unique("renewal");
        String merchantId = prefix + "_merchant";
        String contractId = prefix + "_contract";
        String phone = uniquePhone();
        insertMerchant(merchantId, phone);
        insertEffectiveContract(contractId, merchantId, LocalDate.parse("2026-01-01"), expiryDate);

        contractRenewalNotificationJob.runOnce(asOf);

        String requestId = "mfrenewal:" + contractId + ":" + expiryDate;
        assertThat(logCountByRequestId(requestId)).isEqualTo(1);
        assertThat(logValueByRequestId(requestId, "domain")).isEqualTo("merchant_fee");
        assertThat(logValueByRequestId(requestId, "event_key")).isEqualTo("renewal");
        assertThat(logValueByRequestId(requestId, "recipient")).isEqualTo(phone);
        assertThat(logValueByRequestId(requestId, "template_code")).isEqualTo("sms.merchant_fee.renewal");
        assertThat(logValueByRequestId(requestId, "status")).isEqualTo("sent");
        assertThat(logParamByRequestId(requestId, "contract_id")).isEqualTo(contractId);
        assertThat(logParamByRequestId(requestId, "expiry_date")).isEqualTo(expiryDate.toString());
        assertThat(logParamByRequestId(requestId, "days_to_expiry")).isEqualTo("10");

        contractRenewalNotificationJob.runOnce(asOf.plusDays(1));

        assertThat(logCountByRequestId(requestId)).isEqualTo(1);
    }

    @Test
    void createBillWritesSkippedNotificationLogWhenMainSwitchOff() {
        setSwitch("sms_notification", false);
        String prefix = unique("off");
        String merchantId = prefix + "_merchant";
        String arApItemId = prefix + "_arap";
        String phone = uniquePhone();
        insertMerchant(merchantId, phone);
        insertArApItem(arApItemId, merchantId, 128_800L);

        merchantBillingService.createBill(createBillRequest(
                prefix + "_create",
                merchantId,
                arApItemId,
                128_800L,
                List.of(new MerchantBillLineRequest("rent", "June rent", 128_800L))
        ));

        assertThat(logCount("rent", phone)).isEqualTo(1);
        assertThat(logValue("rent", phone, "status")).isEqualTo("skipped_switch_off");
        assertThat(logValue("rent", phone, "reason")).isEqualTo("switch_off:sms_notification");
    }

    @Test
    void billNotificationUsesExactConfiguredRecipient() {
        String prefix = unique("exact");
        String merchantId = prefix + "_merchant";
        String arApItemId = prefix + "_arap";
        String primaryPhone = uniquePhone();
        String exactPhone = uniquePhone();
        insertMerchant(merchantId, primaryPhone);
        insertArApItem(arApItemId, merchantId, 128_800L);
        recipientService.upsert(new MerchantFeeNotificationRecipientRequest(
                merchantId, "rent", "Fee Owner", exactPhone, "staff_finance"));

        MerchantBillRecord bill = merchantBillingService.createBill(createBillRequest(
                prefix + "_create",
                merchantId,
                arApItemId,
                128_800L,
                List.of(new MerchantBillLineRequest("rent", "June rent", 128_800L))
        ));

        String requestId = "mfbill:" + bill.id() + ":rent";
        assertThat(logValueByRequestId(requestId, "recipient")).isEqualTo(exactPhone);
        assertThat(logCount("rent", primaryPhone)).isZero();
    }

    @Test
    void merchantDefaultRecipientCoversAllBillFeeEventsAndRenewal() {
        LocalDate asOf = LocalDate.parse("2026-06-29");
        LocalDate expiryDate = asOf.plusDays(10);
        String prefix = unique("default");
        String merchantId = prefix + "_merchant";
        String arApItemId = prefix + "_arap";
        String contractId = prefix + "_contract";
        String primaryPhone = uniquePhone();
        String defaultPhone = uniquePhone();
        insertMerchant(merchantId, primaryPhone);
        insertArApItem(arApItemId, merchantId, 600L);
        insertEffectiveContract(contractId, merchantId, LocalDate.parse("2026-01-01"), expiryDate);
        recipientService.upsert(new MerchantFeeNotificationRecipientRequest(
                merchantId, null, "Fee Owner", defaultPhone, "staff_finance"));

        merchantBillingService.createBill(createBillRequest(
                prefix + "_create",
                merchantId,
                arApItemId,
                600L,
                List.of(
                        new MerchantBillLineRequest("rent", "June rent", 100L),
                        new MerchantBillLineRequest("energy_fee", "June utilities", 200L),
                        new MerchantBillLineRequest("property_fee", "June property", 300L)
                )
        ));
        contractRenewalNotificationJob.runOnce(asOf);

        assertThat(logCount("rent", defaultPhone)).isEqualTo(1);
        assertThat(logCount("utilities", defaultPhone)).isEqualTo(1);
        assertThat(logCount("property", defaultPhone)).isEqualTo(1);
        assertThat(logValueByRequestId("mfrenewal:" + contractId + ":" + expiryDate, "recipient"))
                .isEqualTo(defaultPhone);
        assertThat(logCount("rent", primaryPhone)).isZero();
        assertThat(logCount("utilities", primaryPhone)).isZero();
        assertThat(logCount("property", primaryPhone)).isZero();
    }

    @Test
    void deactivatedRecipientFallsBackToMerchantPrimaryContact() {
        String prefix = unique("inactive");
        String merchantId = prefix + "_merchant";
        String arApItemId = prefix + "_arap";
        String primaryPhone = uniquePhone();
        String configuredPhone = uniquePhone();
        insertMerchant(merchantId, primaryPhone);
        insertArApItem(arApItemId, merchantId, 128_800L);
        MerchantFeeNotificationRecipientRecord recipient = recipientService.upsert(
                new MerchantFeeNotificationRecipientRequest(
                        merchantId, "rent", "Fee Owner", configuredPhone, "staff_finance"));
        recipientService.deactivate(recipient.id());

        MerchantBillRecord bill = merchantBillingService.createBill(createBillRequest(
                prefix + "_create",
                merchantId,
                arApItemId,
                128_800L,
                List.of(new MerchantBillLineRequest("rent", "June rent", 128_800L))
        ));

        String requestId = "mfbill:" + bill.id() + ":rent";
        assertThat(logValueByRequestId(requestId, "recipient")).isEqualTo(primaryPhone);
        assertThat(logCount("rent", configuredPhone)).isZero();
    }

    private MerchantBillCreateRequest createBillRequest(
            String idempotencyKey,
            String merchantId,
            String arApItemId,
            long totalAmountCent,
            List<MerchantBillLineRequest> lines
    ) {
        return new MerchantBillCreateRequest(
                idempotencyKey,
                "issue bill",
                merchantId,
                arApItemId,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                LocalDate.parse("2026-07-05"),
                totalAmountCent,
                "manual",
                idempotencyKey + "_batch",
                lines
        );
    }

    private void insertMerchant(String merchantId, String phone) {
        jdbcTemplate.update("""
                INSERT INTO merchants (
                    id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                    settlement_entity_id, contact_name, contact_phone, status, created_at, updated_at
                )
                VALUES (?, ?, 'retail', NULL, NULL, NULL, 'Primary Contact', ?, 'active', now(), now())
                ON CONFLICT (id) DO UPDATE SET
                    merchant_name = EXCLUDED.merchant_name,
                    contact_name = EXCLUDED.contact_name,
                    contact_phone = EXCLUDED.contact_phone,
                    status = EXCLUDED.status,
                    updated_at = now()
                """, merchantId, "Merchant " + merchantId, piiCipher.encrypt(phone));
    }

    private void insertArApItem(String id, String merchantId, long amountCent) {
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (
                    id, item_no, direction, counterparty_id, source_type, source_id,
                    amount_cent, paid_amount_cent, unpaid_amount_cent, currency,
                    due_date, accounting_period, status, created_at, updated_at, merchant_id
                )
                VALUES (?, ?, 'receivable', ?, 'manual', ?, ?, 0, ?, 'CNY',
                        DATE '2026-07-05', '2026-06', 'pending_collection', now(), now(), ?)
                ON CONFLICT (id) DO UPDATE SET
                    counterparty_id = EXCLUDED.counterparty_id,
                    source_type = EXCLUDED.source_type,
                    source_id = EXCLUDED.source_id,
                    amount_cent = EXCLUDED.amount_cent,
                    paid_amount_cent = EXCLUDED.paid_amount_cent,
                    unpaid_amount_cent = EXCLUDED.unpaid_amount_cent,
                    due_date = EXCLUDED.due_date,
                    accounting_period = EXCLUDED.accounting_period,
                    status = EXCLUDED.status,
                    merchant_id = EXCLUDED.merchant_id,
                    updated_at = now()
                """, id, "ARAP-" + id, merchantId, id, amountCent, amountCent, merchantId);
    }

    private void insertEffectiveContract(
            String contractId,
            String merchantId,
            LocalDate effectiveDate,
            LocalDate expiryDate
    ) {
        jdbcTemplate.update("""
                INSERT INTO contracts (
                    id, merchant_id, contract_subject_id, operation_subject_id, settlement_subject_id,
                    effective_date, expiry_date, rent_terms, commission_mode, energy_fee_basis, deposit_cent,
                    penalty_terms, settlement_rules, snapshot, shareholder_ratio_for_report, attachment_ids,
                    lifecycle_status, project_id, terminated_at, termination_reason, renewed_from_contract_id,
                    created_at, updated_at
                )
                VALUES (
                    ?, ?, ?, NULL, ?, ?, ?, '{}'::jsonb, NULL, 'none', NULL,
                    '{}'::jsonb, '[]'::jsonb, NULL, NULL, '[]'::jsonb,
                    'effective', ?, NULL, NULL, NULL, now(), now()
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    effective_date = EXCLUDED.effective_date,
                    expiry_date = EXCLUDED.expiry_date,
                    lifecycle_status = EXCLUDED.lifecycle_status,
                    project_id = EXCLUDED.project_id,
                    updated_at = now()
                """, contractId, merchantId, contractId + "_subject", contractId + "_settlement",
                effectiveDate, expiryDate, contractId + "_project");
    }

    private void setSwitch(String capabilityCode, boolean enabled) {
        jdbcTemplate.update("""
                INSERT INTO terminal_capability_switches (
                    id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at
                )
                VALUES (?, '*', ?, 'global', 'global', ?, 'merchant fee notification test switch override', now(), now())
                ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO UPDATE SET
                    enabled = EXCLUDED.enabled,
                    reason = EXCLUDED.reason,
                    updated_at = now()
                """, idFor(capabilityCode), capabilityCode, enabled);
    }

    private void cleanupData() {
        jdbcTemplate.update("""
                DELETE FROM idempotency_records
                 WHERE idempotency_key LIKE 'mf_notif_%'
                    OR idempotency_key LIKE 'mfbill:mbill_mf_notif_%'
                    OR idempotency_key LIKE 'mfdun:mbill_dunning_mf_notif_%'
                    OR idempotency_key LIKE 'mfrenewal:mf_notif_%'
                    OR idempotency_key IN (
                        SELECT request_id
                          FROM notification_send_log
                         WHERE domain = 'merchant_fee'
                           AND recipient LIKE ?
                    )
                """, PHONE_PREFIX + "%");
        jdbcTemplate.update("""
                DELETE FROM notification_send_log
                 WHERE domain = 'merchant_fee'
                   AND recipient LIKE ?
                """, PHONE_PREFIX + "%");
        jdbcTemplate.update("""
                DELETE FROM merchant_bill_dunning_records
                 WHERE merchant_id LIKE 'mf_notif_%'
                    OR bill_id IN (
                        SELECT id FROM merchant_bills WHERE merchant_id LIKE 'mf_notif_%'
                    )
                """);
        jdbcTemplate.update("""
                DELETE FROM merchant_bill_disputes
                 WHERE merchant_id LIKE 'mf_notif_%'
                    OR bill_id IN (
                        SELECT id FROM merchant_bills WHERE merchant_id LIKE 'mf_notif_%'
                    )
                """);
        jdbcTemplate.update("DELETE FROM merchant_bills WHERE merchant_id LIKE 'mf_notif_%'");
        jdbcTemplate.update("""
                DELETE FROM contract_approvals
                 WHERE contract_id IN (SELECT id FROM contracts WHERE merchant_id LIKE 'mf_notif_%')
                """);
        jdbcTemplate.update("""
                DELETE FROM contract_changes
                 WHERE contract_id IN (SELECT id FROM contracts WHERE merchant_id LIKE 'mf_notif_%')
                """);
        jdbcTemplate.update("""
                DELETE FROM contract_settlements
                 WHERE contract_id IN (SELECT id FROM contracts WHERE merchant_id LIKE 'mf_notif_%')
                """);
        jdbcTemplate.update("""
                DELETE FROM merchant_fee_notification_recipient
                 WHERE merchant_id LIKE 'mf_notif_%'
                    OR recipient_phone LIKE ?
                """, PHONE_PREFIX + "%");
        jdbcTemplate.update("""
                DELETE FROM contract_rent_bills
                 WHERE contract_id IN (SELECT id FROM contracts WHERE merchant_id LIKE 'mf_notif_%')
                """);
        jdbcTemplate.update("""
                DELETE FROM contract_spaces
                 WHERE contract_id IN (SELECT id FROM contracts WHERE merchant_id LIKE 'mf_notif_%')
                """);
        jdbcTemplate.update("DELETE FROM contracts WHERE merchant_id LIKE 'mf_notif_%'");
        jdbcTemplate.update("DELETE FROM merchants WHERE id LIKE 'mf_notif_%'");
    }

    private String idFor(String capabilityCode) {
        return "test_tcs_" + capabilityCode.replace(".", "_");
    }

    private int logCount(String event, String recipient) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM notification_send_log
                 WHERE domain = 'merchant_fee'
                   AND event_key = ?
                   AND recipient = ?
                """, Integer.class, event, recipient);
        return count == null ? 0 : count;
    }

    private int logCountByRequestId(String requestId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM notification_send_log
                 WHERE request_id = ?
                """, Integer.class, requestId);
        return count == null ? 0 : count;
    }

    private String logValue(String event, String recipient, String column) {
        return jdbcTemplate.queryForObject("""
                SELECT %s
                  FROM notification_send_log
                 WHERE domain = 'merchant_fee'
                   AND event_key = ?
                   AND recipient = ?
                 ORDER BY created_at DESC
                 LIMIT 1
                """.formatted(column), String.class, event, recipient);
    }

    private String logValueByRequestId(String requestId, String column) {
        return jdbcTemplate.queryForObject("""
                SELECT %s
                  FROM notification_send_log
                 WHERE request_id = ?
                 LIMIT 1
                """.formatted(column), String.class, requestId);
    }

    private String logParam(String event, String recipient, String key) {
        return jdbcTemplate.queryForObject("""
                SELECT params ->> ?
                  FROM notification_send_log
                 WHERE domain = 'merchant_fee'
                   AND event_key = ?
                   AND recipient = ?
                 ORDER BY created_at DESC
                 LIMIT 1
                """, String.class, key, event, recipient);
    }

    private String logParamByRequestId(String requestId, String key) {
        return jdbcTemplate.queryForObject("""
                SELECT params ->> ?
                  FROM notification_send_log
                 WHERE request_id = ?
                 LIMIT 1
                """, String.class, key, requestId);
    }

    private String unique(String label) {
        return "mf_notif_" + label + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String uniquePhone() {
        String digits = UUID.randomUUID().toString().replaceAll("\\D", "");
        while (digits.length() < 6) {
            digits += "0";
        }
        return PHONE_PREFIX + digits.substring(0, 6);
    }
}
