package com.complex.platform.customer;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.gateway.PaymentGatewayRequest;
import com.complex.platform.finance.gateway.PaymentGatewayResult;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * XM-ONESHOT-RETURN B02 审查修正（治理地图第 303 行「全局原则 · 同时操作以先成功为准」：同一个操作被重复提交，系统只认一次，
 * 不会因为重试就重复入账；操作员 2026-09-25 确认的追加句：同一次操作重复提交时，返回第一次的结果并提示已完成）：
 * 客人端储值充值两步的真实库、真实接口回归。
 *
 * <p>发起充值：幂等层按由本请求编号派生的 svtop_ 键回放第一次的支付意向——同一请求编号重复提交提示已完成。
 * 充值入账：同一支付意向再次入账返回第一次的充值流水——客人端每次写请求换新请求编号，按支付意向认出重复提交，提示已完成、余额只加一次。
 *
 * <p>支付渠道用替身：真实渠道未接入（占位实现返回未配置），入账必须向渠道核验成功回执。本类开测试事务、结束回滚；
 * 写请求带 {@code X-Terminal-Type: web} 与客人登录凭证。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StoredValueTopupReplayCompletedJdbcIntegrationTests {

    private static final String COMPLETED = "该操作已完成（重复提交，返回首次结果）";
    private static final String GATEWAY = "b02_gateway";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StoredValueService storedValueService;

    @MockitoBean
    private PaymentGatewayRouter paymentGateway;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String customerId = "cus_b02sv_" + suffix;
    private final String projectId = "prj_b02sv_" + suffix;
    private final String token = "cust_b02sv_" + suffix;

    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new CustomerRecord(
                customerId, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        customerRepository.saveSession(new CustomerSessionRecord(
                "sess_b02sv_" + suffix, customerId, token, "miniapp", now, now.plusSeconds(3600), "active"));
        // 发起充值时渠道受理并返回渠道单号（每笔不同）
        when(paymentGateway.initiatePayment(any())).thenAnswer(invocation -> new PaymentGatewayResult(
                PaymentGatewayResult.Status.PENDING, GATEWAY, "ref_b02sv_" + UUID.randomUUID(), "pending"));
    }

    @AfterEach
    void clearActor() {
        SecurityContextHolder.clearContext();
    }

    // 发起充值：同一请求编号重复提交，幂等层按派生的 svtop_ 键回放第一次的支付意向——提示已完成
    @Test
    void aTopupResubmittedWithTheSameKeyReturnsTheFirstPaymentIntentAndSaysItIsAlreadyCompleted() throws Exception {
        String key = "b02-topup-" + suffix;
        String body = topupJson(key, 5000L);

        String firstIntent = intentId(customerWrite("/api/v1/customer/stored-value/topup", key, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));
        assertThat(intents()).isEqualTo(1);

        // 同键同正文重复提交：返回第一次的支付意向并提示已完成（修复前与第一次一样：message=success、没有标记）
        assertCompleted(customerWrite("/api/v1/customer/stored-value/topup", key, body), firstIntent);
        // 第二次还能过：再重复提交一次，照样返回第一次的支付意向并提示已完成，不再建新的支付意向
        assertCompleted(customerWrite("/api/v1/customer/stored-value/topup", key, body), firstIntent);
        assertThat(intents()).isEqualTo(1);

        // 换请求编号是新的一次充值：建新的支付意向，不带已完成标记（派生键只认本请求自己的）
        String nextKey = "b02-topup-next-" + suffix;
        String nextIntent = intentId(customerWrite("/api/v1/customer/stored-value/topup", nextKey, topupJson(nextKey, 5000L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));
        assertThat(nextIntent).isNotEqualTo(firstIntent);
        assertThat(intents()).isEqualTo(2);

        // 同键改金额：按请求编号冲突拒绝，不是「已完成」（放在最后：失败的请求把测试事务标成只能回滚）
        customerWrite("/api/v1/customer/stored-value/topup", key, topupJson(key, 6000L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_CONFLICT"))
                .andExpect(jsonPath("$.replayed").doesNotExist());
        assertThat(intents()).isEqualTo(2);
    }

    // 第十八批 N 段第 124 条 124-R5（PGM L966「已拍板：所有有过程的业务动作都必须留痕……」；OpenAPI customerStoredValueTopup 的
    // x-audit-event-success）：客人发起一次充值，按本次幂等键落一条 customer.stored_value.topup，business_id 是返回的支付意向
    @Test
    void aTopupRecordsTheDeclaredSuccessEventOnce() throws Exception {
        String key = "r5-topup-" + suffix;
        String intent = intentId(customerWrite("/api/v1/customer/stored-value/topup", key, topupJson(key, 5000L))
                .andExpect(status().isOk()));
        assertThat(jdbcTemplate.queryForList("""
                SELECT business_type || '|' || business_id || '|' || permission_code FROM audit_events
                 WHERE event_name = 'customer.stored_value.topup' AND event_result = 'success' AND idempotency_key = ?""",
                String.class, key)).containsExactly("payment_intent|" + intent + "|customer.stored_value.topup");
    }

    // 充值入账：同一支付意向再次入账返回第一次的充值流水并提示已完成，余额只加一次；每次入账请求用新的请求编号（客人端现状）
    @Test
    void aSettleResubmittedForTheSamePaymentIntentReturnsTheFirstTopupAndSaysItIsAlreadyCompleted() throws Exception {
        String topupKey = "b02-settle-topup-" + suffix;
        String intentId = intentId(customerWrite("/api/v1/customer/stored-value/topup", topupKey, topupJson(topupKey, 5000L))
                .andExpect(status().isOk()));
        String gatewayRef = jdbcTemplate.queryForObject(
                "SELECT gateway_ref FROM finance_payment_intents WHERE id = ?", String.class, intentId);
        // 渠道核验：支付已成功，回执与本次充值单一致
        when(paymentGateway.isGatewayEnabled()).thenReturn(true);
        when(paymentGateway.queryStatus(gatewayRef)).thenReturn(new PaymentGatewayResult(
                PaymentGatewayResult.Status.SUCCEEDED, GATEWAY, gatewayRef, "", null,
                new PaymentGatewayRequest("stored_value_topup", customerId, 5000L, "CNY", "wechat", gatewayRef, null, null)));
        String body = """
                {"project_id": "%s", "payment_intent_id": "%s"}
                """.formatted(projectId, intentId);

        String first = content(customerWrite("/api/v1/customer/stored-value/settle", "b02-settle-1-" + suffix, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist())
                .andExpect(jsonPath("$.data.txn_type").value("topup")));
        String firstTxn = new ObjectMapper().readTree(first).at("/data/id").asText();
        assertThat(balance()).isEqualTo(5000L);

        // 再次入账（新的请求编号）：同一支付意向＝同一次操作，返回第一次的充值流水并提示已完成，余额不再加
        // （修复前 message=success、没有标记，与首次入账分不出来）
        assertSettleCompleted(customerWrite("/api/v1/customer/stored-value/settle", "b02-settle-2-" + suffix, body), firstTxn);
        // 第二次还能过：再入账一次，照样
        assertSettleCompleted(customerWrite("/api/v1/customer/stored-value/settle", "b02-settle-3-" + suffix, body), firstTxn);
        assertThat(balance()).isEqualTo(5000L);
        assertThat(settledTxns(intentId)).isEqualTo(1);

        // 柜台确认链复用同一入账内核：命中「已入账」时它返回的是确认单（归 B09），入账内核不在这里记已完成
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor("usr_master_authority", "master-authority", SessionIssuer.MASTER_ACCOUNT_TYPE,
                        "org_1", null, "active"), null, List.of()));
        String posKey = "b02-pos-" + suffix;
        IdempotentReplayContext previous = IdempotentReplayContext.enter(posKey, "b02-pos-digest");
        try {
            storedValueService.confirmOfflineTopupAndSettle(intentId, new OfflinePaymentConfirmRequest(
                    posKey, null, List.of(), intentId, 5000L, "pos", Instant.now()));
            assertThat(IdempotentReplayContext.current().replayed()).as("柜台确认链").isFalse();
        } finally {
            IdempotentReplayContext.restore(previous);
        }
        assertThat(balance()).isEqualTo(5000L);
        assertThat(settledTxns(intentId)).isEqualTo(1);
    }

    private void assertCompleted(ResultActions replay, String firstIntent) throws Exception {
        replay.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value(COMPLETED))
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.data.id").value(firstIntent));
    }

    private void assertSettleCompleted(ResultActions replay, String firstTxn) throws Exception {
        replay.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(COMPLETED))
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.data.id").value(firstTxn))
                .andExpect(jsonPath("$.data.txn_type").value("topup"))
                .andExpect(jsonPath("$.data.amount_cent").value(5000))
                .andExpect(jsonPath("$.data.balance_after_cent").value(5000));
    }

    private ResultActions customerWrite(String path, String idempotencyKey, String body) throws Exception {
        return mockMvc.perform(post(path)
                .header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String topupJson(String key, long amountCent) {
        return """
                {"idempotency_key": "%s", "project_id": "%s", "amount_cent": %d, "channel": "wechat"}
                """.formatted(key, projectId, amountCent);
    }

    private long balance() {
        return jdbcTemplate.queryForObject("""
                SELECT balance_cent FROM customer_stored_value_accounts
                 WHERE customer_id = ? AND project_id = ? AND merchant_id IS NULL
                """, Long.class, customerId, projectId);
    }

    private Integer intents() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stored_value_topup_intents WHERE customer_id = ?", Integer.class, customerId);
    }

    private Integer settledTxns(String intentId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM customer_stored_value_transactions WHERE customer_id = ? AND payment_intent_id = ?",
                Integer.class, customerId, intentId);
    }

    private static String intentId(ResultActions actions) throws Exception {
        return new ObjectMapper().readTree(content(actions)).at("/data/id").asText();
    }

    private static String content(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
