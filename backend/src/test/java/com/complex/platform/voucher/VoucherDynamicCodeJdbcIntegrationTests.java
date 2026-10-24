package com.complex.platform.voucher;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
 * ROOT-03-R32＝clients-R41 券包权益核销动态码（BSM 第 585 行「客人出示的门票核销码、权益核销码……都不是一张固定的图，而是每隔一段时间自动更新的
 * 动态码……截图、转发出去的旧码过了有效期就作废……权益转赠一经完成，原持有人手里的码立即全部作废」）＋ R30 券包字段（BSM 第 865 行「已失效详情显示
 * 原因以及是否退回积分……可用券只有在券种允许时才显示转赠入口」）。客人端与后台请求一律按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class VoucherDynamicCodeJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;
    private StaffRequestFixture staffs;
    private RequestPostProcessor cashier;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        staffs = new StaffRequestFixture(jdbc);
        cashier = staffs.staff("usr_vdc_" + f.suffix, "staff", f.projectId, f.merchantId, "customer.voucher.redeem");
    }

    @Test
    void customerGetsFreshCodeWithUnifiedInterval() throws Exception {
        String voucher = voucher(true);
        int interval = jdbc.queryForList("SELECT refresh_interval_seconds FROM ticket_qr_refresh_config WHERE id = 'default'",
                Integer.class).stream().findFirst().orElse(15);
        String first = read(redeemCode(f.token, voucher).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucher_instance_id").value(voucher))
                .andExpect(jsonPath("$.data.refresh_interval_seconds").value(interval))
                .andExpect(jsonPath("$.data.expires_at").isNotEmpty()), "$.data.code");
        assertThat(first).startsWith("vrc_");
        String second = read(redeemCode(f.token, voucher).andExpect(status().isOk()), "$.data.code");
        assertThat(second).isNotEqualTo(first);
        // 取新码即撤旧码：同一张券同时只一枚有效码，旧码验不过。
        redeem(Map.of("code", first)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_REDEEM_CODE_EXPIRED"));
    }

    @Test
    void redeemWithFreshDynamicCodeSucceedsOnce() throws Exception {
        String voucher = voucher(true);
        String code = read(redeemCode(f.token, voucher).andExpect(status().isOk()), "$.data.code");
        redeem(Map.of("code", code)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucher_instance_id").value(voucher));
        assertThat(jdbc.queryForObject("SELECT status FROM voucher_instances WHERE id = ?", String.class, voucher)).isEqualTo("redeemed");
        redeem(Map.of("code", code)).andExpect(status().isConflict());
    }

    @Test
    void redeemWithExpiredDynamicCodeRejected() throws Exception {
        String voucher = voucher(true);
        String code = read(redeemCode(f.token, voucher).andExpect(status().isOk()), "$.data.code");
        jdbc.update("UPDATE voucher_redeem_codes SET expires_at = now() - interval '1 second' WHERE voucher_instance_id = ?", voucher);
        redeem(Map.of("code", code)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_REDEEM_CODE_EXPIRED"));
        assertThat(jdbc.queryForObject("SELECT status FROM voucher_instances WHERE id = ?", String.class, voucher)).isEqualTo("issued");
    }

    @Test
    void redeemWithStaticInstanceCodeRejected() throws Exception {
        String voucher = voucher(true);
        String staticCode = jdbc.queryForObject("SELECT code FROM voucher_instances WHERE id = ?", String.class, voucher);
        redeem(Map.of("code", staticCode)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_DYNAMIC_CODE_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT status FROM voucher_instances WHERE id = ?", String.class, voucher)).isEqualTo("issued");
    }

    @Test
    void transferClaimRevokesOldCode() throws Exception {
        String voucher = voucher(true);
        String oldCode = read(redeemCode(f.token, voucher).andExpect(status().isOk()), "$.data.code");
        var created = customerPost(f.token, "/api/v1/customer/voucher-transfers",
                Map.of("voucher_instance_id", voucher, "channel", "link")).andExpect(status().isOk());
        String receiverToken = otherCustomer("recv");
        customerPost(receiverToken, "/api/v1/customer/voucher-transfers/" + read(created, "$.data.id") + "/claim",
                Map.of("claim_code", read(created, "$.data.claim_code"))).andExpect(status().isOk());
        redeem(Map.of("code", oldCode)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_REDEEM_CODE_EXPIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM voucher_redeem_codes WHERE voucher_instance_id = ? AND status = 'active'",
                Integer.class, voucher)).isZero();
        // 新持有人用自己的码。
        String newCode = read(redeemCode(receiverToken, voucher).andExpect(status().isOk()), "$.data.code");
        redeem(Map.of("code", newCode)).andExpect(status().isOk());
    }

    @Test
    void holderStillRedeemsWhileTransferPendingAndLateClaimVoided() throws Exception {
        // 操作员裁决第十三批第 9 条「和门票一致不冻结」：发起转赠后原持有人仍可自用，先成功的动作为准，其余自动作废。
        String voucher = voucher(true);
        var created = customerPost(f.token, "/api/v1/customer/voucher-transfers",
                Map.of("voucher_instance_id", voucher, "channel", "link")).andExpect(status().isOk());
        String transfer = read(created, "$.data.id");
        String code = read(redeemCode(f.token, voucher).andExpect(status().isOk()), "$.data.code");
        redeem(Map.of("code", code)).andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT status, void_reason FROM voucher_transfers WHERE id = ?", transfer))
                .containsEntry("status", "voided").containsEntry("void_reason", "holder_redeemed");
        customerPost(otherCustomer("late"), "/api/v1/customer/voucher-transfers/" + transfer + "/claim",
                Map.of("claim_code", read(created, "$.data.claim_code")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_TRANSFER_VOIDED"));
        assertThat(jdbc.queryForObject("SELECT customer_id FROM voucher_instances WHERE id = ?", String.class, voucher))
                .isEqualTo(f.customerId);
    }

    @Test
    void holderRetransfersWhilePendingAndEarlierTransferVoided() throws Exception {
        String voucher = voucher(true);
        var first = customerPost(f.token, "/api/v1/customer/voucher-transfers",
                Map.of("voucher_instance_id", voucher, "channel", "link")).andExpect(status().isOk());
        var second = customerPost(f.token, "/api/v1/customer/voucher-transfers",
                Map.of("voucher_instance_id", voucher, "channel", "link")).andExpect(status().isOk());
        String firstId = read(first, "$.data.id");
        assertThat(jdbc.queryForMap("SELECT status, void_reason FROM voucher_transfers WHERE id = ?", firstId))
                .containsEntry("status", "voided").containsEntry("void_reason", "holder_retransferred");
        customerPost(otherCustomer("first"), "/api/v1/customer/voucher-transfers/" + firstId + "/claim",
                Map.of("claim_code", read(first, "$.data.claim_code")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_TRANSFER_VOIDED"));
        customerPost(otherCustomer("second"), "/api/v1/customer/voucher-transfers/" + read(second, "$.data.id") + "/claim",
                Map.of("claim_code", read(second, "$.data.claim_code"))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT customer_id FROM voucher_instances WHERE id = ?", String.class, voucher))
                .isEqualTo("cus_vdc_second_" + f.suffix);
    }

    @Test
    void initiateReplayWithSameKeyReturnsFirstTransferAndVoidsNothing() throws Exception {
        // 客人端网络中断后沿用原幂等键重提（customerClient 约定服务端回放第一次的结果）：门票转赠同款幂等，回放第一笔，
        // 不按裁决第十三批第 9 条「再次转赠作废前一笔」把它作废，也不多建一笔。
        String voucher = voucher(true);
        Map<String, Object> body = Map.of("idempotency_key", "vdc-replay-" + f.suffix, "voucher_instance_id", voucher,
                "channel", "link");
        String first = read(customerPost(f.token, "/api/v1/customer/voucher-transfers", body).andExpect(status().isOk()), "$.data.id");
        String replay = read(customerPost(f.token, "/api/v1/customer/voucher-transfers", body).andExpect(status().isOk()), "$.data.id");
        assertThat(replay).isEqualTo(first);
        assertThat(jdbc.queryForList("SELECT status FROM voucher_transfers WHERE voucher_instance_id = ?", String.class, voucher))
                .containsExactly("pending");
    }

    @Test
    void initiateFailureRecordsCreateFailedAudit() throws Exception {
        // OpenAPI initiateCustomerVoucherTransfer：x-audit-event-failure voucher_transfer.create_failed。
        String voucher = voucher(true);
        String key = "vdc-fail-" + f.suffix;
        customerPost(otherCustomer("nothold"), "/api/v1/customer/voucher-transfers",
                Map.of("idempotency_key", key, "voucher_instance_id", voucher, "channel", "link"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_TRANSFER_NOT_HOLDER"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'voucher_transfer.create_failed'"
                + " AND idempotency_key = ?", Integer.class, key)).isEqualTo(1);
    }

    @Test
    void otherCustomersVoucherCodeForbidden() throws Exception {
        String voucher = voucher(true);
        redeemCode(otherCustomer("peek"), voucher).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_NOT_HOLDER"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM voucher_redeem_codes WHERE voucher_instance_id = ?",
                Integer.class, voucher)).isZero();
    }

    @Test
    void customerVoucherListHasNoStaticCodeAndShowsTransferable() throws Exception {
        String voucher = voucher(true);
        String locked = voucher(false);
        myVouchers().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + voucher + "')].code").value(org.hamcrest.Matchers.contains(
                        org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + voucher + "')].transferable").value(org.hamcrest.Matchers.contains(true)))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + locked + "')].transferable").value(org.hamcrest.Matchers.contains(false)));
    }

    @Test
    void invalidVoucherShowsReasonAndPointsRefund() throws Exception {
        String voucher = voucher(true);
        jdbc.update("UPDATE voucher_instances SET status = 'revoked', revoked_at = now(), revoke_reason = '商户无法履约' WHERE id = ?", voucher);
        myVouchers().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + voucher + "')].invalid_reason")
                        .value(org.hamcrest.Matchers.contains("商户无法履约")))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + voucher + "')].points_refunded")
                        .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + voucher + "')].transferable").value(org.hamcrest.Matchers.contains(false)));
    }

    // ---------- helpers ----------

    private String voucher(boolean transferable) {
        String definition = "vdef_vdc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, face_value_cent,"
                + " valid_days, status, transferable) VALUES (?, '满百减二十', 'merchant_discount', 'in_store_staff', 'none', ?, 2000, 30,"
                + " 'active', ?)", definition, f.merchantId, transferable);
        String instance = "vi_vdc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, source_ref, status, issued_at,"
                + " expires_at) VALUES (?, ?, ?, ?, 'marketing_grant', 'vgrant_vdc', 'issued', now(), now() + interval '30 days')",
                instance, definition, f.customerId, "VC" + instance);
        return instance;
    }

    private ResultActions redeemCode(String token, String voucher) throws Exception {
        return mockMvc.perform(get("/api/v1/customer/vouchers/" + voucher + "/redeem-code")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token));
    }

    private ResultActions myVouchers() throws Exception {
        return mockMvc.perform(get("/api/v1/customer/vouchers").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token));
    }

    private ResultActions redeem(Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        actual.put("idempotency_key", "vdc-redeem-" + UUID.randomUUID());
        return mockMvc.perform(post("/api/v1/vouchers/redemptions").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(actual)));
    }

    private ResultActions customerPost(String token, String path, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        actual.putIfAbsent("idempotency_key", "vdc-" + UUID.randomUUID());
        String key = actual.get("idempotency_key").toString();
        return mockMvc.perform(post(path).header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(actual)));
    }

    private String otherCustomer(String tag) {
        Instant now = Instant.now();
        String id = "cus_vdc_" + tag + "_" + f.suffix;
        String phone = "136" + String.format("%08d", Math.floorMod((tag + f.suffix).hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(id, phone, "136****" + phone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_vdc_" + tag + "_" + f.suffix, id, "tok_vdc_" + tag + "_" + f.suffix,
                "miniapp", now, now.plusSeconds(3600), "active"));
        return "tok_vdc_" + tag + "_" + f.suffix;
    }

    private static String read(ResultActions result, String path) throws Exception {
        return CustomerTicketPaymentFixture.read(result, path);
    }
}
