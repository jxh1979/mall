package com.complex.platform.merchantbilling;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AG 段第 255 条 255-R1、255-R3④（PGM:899「谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」；
 * 先例决策台账 18AE 198-R3「服务端操作人一律取登录人」）：商户账单确认、异议、催缴与费用通知接收人（平台口径）按蛇形键走真实 HTTP、
 * 真实授权链，落库或成功留痕的操作人是登录人；请求体另填的 operator_user_id 不采信，不填也不再 400。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantBillingOperatorJdbcIntegrationTests {

    private static final String BILLS = "/api/v1/merchant-billing/bills/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcMerchantBillingRepository repository;

    private final String digits = String.valueOf(System.nanoTime());
    private final String userId = "usr_255_" + digits;
    private final String merchantId = "mch_255_" + digits;
    private final String billId = "mbill_255_" + digits;

    @Test
    void confirmDisputeAndDunningRecordTheLoggedInOperator() throws Exception {
        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff(userId, "platform_admin", null, null,
                "merchant.billing.confirm", "merchant.billing.dispute.submit", "merchant.billing.dunning.record");
        seedIssuedBill();

        send(finance, billId + "/confirm", "{\"idempotency_key\":\"confirm-255-" + digits + "\",\"operator_user_id\":\"someone_else\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));
        send(finance, billId + "/disputes", """
                {"idempotency_key":"dispute-255-%s","reason_code":"energy_amount_mismatch","description":"电表读数与账单不符",
                 "evidence_attachment_ids":["att-255-meter"],"operator_user_id":"someone_else"}
                """.formatted(digits))
                .andExpect(status().isOk());
        send(finance, billId + "/dunning-records", """
                {"idempotency_key":"dunning-255-a-%s","dunning_channel":"wecom","message_template":"rent_overdue_first_notice",
                 "status":"sent","operator_user_id":"someone_else"}
                """.formatted(digits))
                .andExpect(status().isOk());
        send(finance, billId + "/dunning-records", """
                {"idempotency_key":"dunning-255-b-%s","dunning_channel":"sms","message_template":"rent_overdue_second_notice",
                 "status":"sent"}
                """.formatted(digits))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForList("""
                SELECT metadata->>'operator_user_id' FROM audit_events
                 WHERE event_name = 'merchant.billing.confirm' AND event_result = 'success' AND business_id = ?
                """, String.class, billId)).containsExactly(userId);
        assertThat(jdbc.queryForList("SELECT operator_user_id FROM merchant_bill_disputes WHERE bill_id = ?", String.class, billId))
                .containsExactly(userId);
        assertThat(jdbc.queryForList("SELECT operator_user_id FROM merchant_bill_dunning_records WHERE bill_id = ?", String.class, billId))
                .containsExactly(userId, userId);
    }

    @Test
    void disputeWithoutReasonCodeIsStillRejected() throws Exception {
        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff(userId, "platform_admin", null, null,
                "merchant.billing.dispute.submit");
        seedIssuedBill();

        send(finance, billId + "/disputes", """
                {"idempotency_key":"dispute-255-bad-%s","description":"缺异议原因"}
                """.formatted(digits))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void platformRecipientUpsertRecordsTheLoggedInOperator() throws Exception {
        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff(userId, "platform_admin", null, null,
                "merchant.billing.manage");

        mockMvc.perform(post("/api/v1/merchant-billing/fee-notification-recipients").with(finance)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"merchant_id":"%s","fee_event":"rent","recipient_name":"收费联系人","recipient_phone":"155%s",
                                 "operator_user_id":"someone_else"}
                                """.formatted(merchantId, digits.substring(digits.length() - 8))))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT created_by FROM merchant_fee_notification_recipient WHERE merchant_id = ?",
                String.class, merchantId)).containsExactly(userId);
    }

    private ResultActions send(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(BILLS + path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void seedIssuedBill() {
        Instant now = Instant.now();
        repository.saveBill(new MerchantBillRecord(billId, merchantId, "arap_255_" + digits, "MBILL-255-" + digits,
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), LocalDate.parse("2026-07-05"), 128800L, "CNY",
                "issued", 0, "manual", "billing_batch_255",
                List.of(new MerchantBillLineRecord("mbill_line_255_" + digits, billId, "rent", "六月房租", 128800L, "CNY", now)),
                now, now));
    }
}
