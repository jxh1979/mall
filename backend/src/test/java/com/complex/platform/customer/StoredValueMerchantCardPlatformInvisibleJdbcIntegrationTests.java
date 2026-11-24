package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.LocalZone;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CL 段（第十八批裁决 79）：商户级卡与平台无关。
 * <ul>
 *   <li>421-R3（BSM:833「平台各岗位（含最高权限）一律看不到商户级卡的卡号、持卡人、余额与充值、退款流水」）：财务退款列表与详情对平台管理员、最高权限
 *       不出现商户级卡待退本金事项（详情 404 REFUND_NOT_FOUND），发卡商户本商户账号照看。</li>
 *   <li>422-R1（BSM:833「平台方……按工商登记合计持股过半的商户，不得发行商户级卡，只能使用综合体通用卡。」）：当日生效的已批准分红方案平台方比例过半，
 *       打开发卡与客人充值都 409 MERCHANT_CARD_PLATFORM_MAJORITY；恰好一半的照常开。</li>
 *   <li>423-R1（ENUM:86 改句「其扣款顺序由发卡商户自定、由发卡商户在商户后台自行设置」；BSM:833「商户级卡是否可退、退多少比例由发卡商户自设规则」）：
 *       发卡、扣款顺序与退款规则只由发卡商户本商户账号设置、查看，两处保存互不冲掉；平台管理员、最高权限 403 MERCHANT_SCOPE_REQUIRED，别家商户 403。</li>
 * </ul>
 * 真库、真实接口、真实授权链、蛇形键；本类开测试事务、结束回滚，被拒的请求放在各用例最后。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StoredValueMerchantCardPlatformInvisibleJdbcIntegrationTests {

    private static final String CARD_CONFIG = "/api/v1/loyalty/stored-value/merchant-card-config";
    private static final String REFUND_RULES = "/api/v1/loyalty/stored-value/merchant-refund-rules";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;
    private StaffRequestFixture staffs;
    private String merchantX;
    private RequestPostProcessor platformAdmin;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        staffs = new StaffRequestFixture(jdbc);
        merchantX = f.merchantId;
        platformAdmin = staffs.staff("usr_cl_pf_" + f.suffix, "platform_admin", null, null,
                "customer.stored_value.refund", "finance.ar_ap.view");
    }

    /** 421-R3：商户级卡待退本金事项平台侧列表不列、详情 404；通用卡待退照列；发卡商户本商户账号列表与详情都看得到（修复前平台侧都看得到）。 */
    @Test
    void financeRefundsHideMerchantCardPendingItemsFromThePlatformSide() throws Exception {
        String merchantItem = refund("m", merchantX);
        String generalItem = refund("g", null);
        RequestPostProcessor shop = staffs.staff("usr_cl_fx_" + f.suffix, "staff", f.projectId, merchantX, "finance.ar_ap.view");

        assertThat(refundIds(shop)).contains(merchantItem).doesNotContain(generalItem);
        mockMvc.perform(get("/api/v1/finance/refunds/" + merchantItem).with(shop)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refund.id").value(merchantItem));
        for (RequestPostProcessor platformSide : List.of(platformAdmin, master())) {
            assertThat(refundIds(platformSide)).contains(generalItem).doesNotContain(merchantItem);
        }
        for (RequestPostProcessor platformSide : List.of(platformAdmin, master())) {
            mockMvc.perform(get("/api/v1/finance/refunds/" + merchantItem).with(platformSide)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail_code").value("REFUND_NOT_FOUND"));
        }
    }

    /** 423-R1：发卡商户本商户账号设扣款顺序与发卡、再设退款规则，读回两样都在（修复前商户账号 403 PLATFORM_SCOPE_REQUIRED）；平台侧与别家商户都被拒。 */
    @Test
    void issuingMerchantSetsCardConfigAndRefundRuleWithoutOverwritingEachOther() throws Exception {
        RequestPostProcessor shop = merchantStaff("cx", merchantX);
        cardConfig(shop, merchantX, true, "bonus_first").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consume_order").value("bonus_first"))
                .andExpect(jsonPath("$.data.card_issuance_enabled").value(true));
        refundRule(shop, merchantX, 8000).andExpect(status().isOk());
        mockMvc.perform(get(REFUND_RULES + "/" + merchantX).with(shop)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consume_order").value("bonus_first"))
                .andExpect(jsonPath("$.data.card_issuance_enabled").value(true))
                .andExpect(jsonPath("$.data.refund_ratio_bps").value(8000));

        String merchantN = merchant("n", 0);
        cardConfig(merchantStaff("cn", merchantN), merchantX, false, "principal_first").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        for (RequestPostProcessor platformSide : List.of(platformAdmin, master())) {
            cardConfig(platformSide, merchantX, false, "principal_first").andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
            refundRule(platformSide, merchantX, 0).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        }
        mockMvc.perform(get(REFUND_RULES + "/" + merchantX).with(platformAdmin)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        assertThat(jdbc.queryForMap("SELECT consume_order, card_issuance_enabled, refund_ratio_bps FROM stored_value_merchant_refund_rules WHERE merchant_id = ?",
                merchantX)).containsEntry("consume_order", "bonus_first").containsEntry("card_issuance_enabled", true)
                .containsEntry("refund_ratio_bps", 8000);
    }

    /**
     * 422-R1：当日生效的已批准分红方案平台方比例 8000（过半）——发卡商户打开发卡 409、库里仍未开；库里直接开着时客人充值（开卡即发卡）也 409、不建卡。
     * 平台方比例恰好 5000（不过半）的商户照常打开发卡。修复前打开发卡 403 PLATFORM_SCOPE_REQUIRED（商户账号设不了），客人充值照常建卡。
     */
    @Test
    void platformMajorityMerchantCannotIssueCards() throws Exception {
        dividendPlan(merchantX, 8000);
        String merchantY = merchant("y", 5000);
        cardConfig(merchantStaff("cy", merchantY), merchantY, true, "principal_first").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.card_issuance_enabled").value(true));

        cardConfig(merchantStaff("cx", merchantX), merchantX, true, "principal_first").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_CARD_PLATFORM_MAJORITY"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_value_merchant_refund_rules WHERE merchant_id = ? AND card_issuance_enabled",
                Integer.class, merchantX)).isZero();

        jdbc.update("INSERT INTO stored_value_merchant_refund_rules (merchant_id, card_issuance_enabled) VALUES (?, TRUE)", merchantX);
        Map<String, Object> topup = new LinkedHashMap<>();
        topup.put("idempotency_key", "cl-topup-" + UUID.randomUUID());
        topup.put("project_id", f.projectId);
        topup.put("merchant_id", merchantX);
        topup.put("amount_cent", 1000);
        topup.put("channel", "wechat");
        f.customerPost("/api/v1/customer/stored-value/topup", topup).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_CARD_PLATFORM_MAJORITY"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_stored_value_accounts WHERE customer_id = ? AND merchant_id = ?",
                Integer.class, f.customerId, merchantX)).isZero();
    }

    // ---------- 夹具与请求 ----------

    private String refund(String tag, String merchantId) {
        String id = "fr_cl_" + tag + "_" + f.suffix;
        jdbc.update("""
                INSERT INTO finance_refunds (id, target_id, refund_amount_cent, reason, refund_channel, refund_direction, status, created_at,
                    source_type, merchant_id, project_id, requested_by, updated_at)
                VALUES (?, ?, 3200, '账户关闭待退本金', 'wechat', 'original', 'requested', now(), 'stored_value_account', ?, ?, 'system', now())
                """, id, "svacc_cl_" + tag + "_" + f.suffix, merchantId, f.projectId);
        return id;
    }

    private List<String> refundIds(RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(get("/api/v1/finance/refunds").with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.items[*].id");
    }

    /** 另一家商户（与 X 同项目）；platformShareBps 大于 0 时插一条当日生效的已批准分红方案。 */
    private String merchant(String tag, int platformShareBps) {
        String id = "merchant_cl_" + tag + "_" + f.suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, ?, 'bar', 'active', now(), now(), ?)", id, "商户卡测试酒吧" + tag, f.projectId);
        if (platformShareBps > 0) {
            dividendPlan(id, platformShareBps);
        }
        return id;
    }

    private void dividendPlan(String merchantId, int platformShareBps) {
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, principal_name, platform_share_bps, status, payout_pay_slot, effective_from)"
                + " VALUES (?, ?, ?, '主理人', ?, 'active', 1, ?)", "dp_cl_" + merchantId, merchantId, "usr_cl_pr_" + f.suffix, platformShareBps,
                LocalDate.now(LocalZone.ID).minusDays(1));
    }

    private RequestPostProcessor merchantStaff(String tag, String merchantId) {
        return staffs.staff("usr_cl_" + tag + "_" + f.suffix, "staff", f.projectId, merchantId, "customer.stored_value.refund");
    }

    private ResultActions cardConfig(RequestPostProcessor who, String merchantId, boolean issuance, String consumeOrder) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchant_id", merchantId);
        body.put("card_issuance_enabled", issuance);
        body.put("fund_ownership", "merchant_prepaid");
        body.put("consume_order", consumeOrder);
        return mockMvc.perform(post(CARD_CONFIG).with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions refundRule(RequestPostProcessor who, String merchantId, int ratioBps) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchant_id", merchantId);
        body.put("refund_allowed", true);
        body.put("refund_ratio_bps", ratioBps);
        return mockMvc.perform(post(REFUND_RULES).with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private static RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
