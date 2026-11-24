package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.finance.gateway.PaymentGatewayRequest;
import com.complex.platform.finance.gateway.PaymentGatewayResult;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CB 段第 396 条 396-R1（BSM:521「每一笔单据在生成时就把所用配置版本固化在单上」；ENUM:86「通道费以每笔充值支付成功时平台与该支付通路的
 * 生效合同费率为准，随该笔充值一并固化」；第十八批裁决 72「渠道费配置须按版本保存（新版本、生效日、旧版保留），每单记录所用版本号」）：客人端通用卡充值
 * 两步（发起充值 → 渠道核验成功后入账）按真实接口走，入账形成的充值批次记下所用通路费配置行与版本号。支付渠道用替身（照
 * StoredValueTopupReplayCompletedJdbcIntegrationTests）；本类开测试事务、结束回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StoredValueTopupRouteFeeVersion396Tests {

    private static final String GATEWAY = "c396_gateway";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CustomerRepository customerRepository;

    @MockitoBean
    private PaymentGatewayRouter paymentGateway;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String customerId = "cus_c396_" + suffix;
    private final String projectId = "prj_c396_" + suffix;
    private final String token = "cust_c396_" + suffix;

    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new CustomerRecord(
                customerId, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        customerRepository.saveSession(new CustomerSessionRecord(
                "sess_c396_" + suffix, customerId, token, "miniapp", now, now.plusSeconds(3600), "active"));
        when(paymentGateway.initiatePayment(any())).thenAnswer(invocation -> new PaymentGatewayResult(
                PaymentGatewayResult.Status.PENDING, GATEWAY, "ref_c396_" + UUID.randomUUID(), "pending"));
    }

    @AfterEach
    void clearActor() {
        SecurityContextHolder.clearContext();
    }

    /** 本项目微信通路费第 2 版（60 基点）生效时充值入账：该笔批次记下这一配置行与版本号 2，费率 60。修复前批次没有配置行与版本号，断言红。 */
    @Test
    void settledTopupBatchRecordsTheRouteFeeConfigRowAndVersionInForce() throws Exception {
        String config = "cfc_c396_" + suffix;
        jdbcTemplate.update("INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft, version_no)"
                + " VALUES (?, ?, 'route', 'wechat', 60, 'active', FALSE, 2)", config, projectId);

        String topupKey = "c396-topup-" + suffix;
        String intentId = new ObjectMapper().readTree(content(customerWrite("/api/v1/customer/stored-value/topup", topupKey, """
                {"idempotency_key": "%s", "project_id": "%s", "amount_cent": 5000, "channel": "wechat"}
                """.formatted(topupKey, projectId)).andExpect(status().isOk()))).at("/data/id").asText();
        String gatewayRef = jdbcTemplate.queryForObject("SELECT gateway_ref FROM finance_payment_intents WHERE id = ?", String.class, intentId);
        when(paymentGateway.isGatewayEnabled()).thenReturn(true);
        when(paymentGateway.queryStatus(gatewayRef)).thenReturn(new PaymentGatewayResult(
                PaymentGatewayResult.Status.SUCCEEDED, GATEWAY, gatewayRef, "", null,
                new PaymentGatewayRequest("stored_value_topup", customerId, 5000L, "CNY", "wechat", gatewayRef, null, null)));

        String txnId = new ObjectMapper().readTree(content(customerWrite("/api/v1/customer/stored-value/settle", "c396-settle-" + suffix, """
                {"project_id": "%s", "payment_intent_id": "%s"}
                """.formatted(projectId, intentId)).andExpect(status().isOk()))).at("/data/id").asText();

        Map<String, Object> batch = jdbcTemplate.queryForMap(
                "SELECT fee_config_id, fee_config_version, fee_bps FROM stored_value_topup_batches WHERE id = ?", txnId);
        assertThat(batch).containsEntry("fee_config_id", config).containsEntry("fee_config_version", 2).containsEntry("fee_bps", 60);
    }

    private ResultActions customerWrite(String path, String idempotencyKey, String body) throws Exception {
        return mockMvc.perform(post(path)
                .header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String content(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
