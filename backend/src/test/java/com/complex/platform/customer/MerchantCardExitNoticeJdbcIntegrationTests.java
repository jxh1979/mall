package com.complex.platform.customer;

// 文件版本：V0.02

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
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
 * ROOT-03-R31 商户级储值卡退出公示（ENUM 第 86 行「发卡商户退租、合同终止或被清退前，必须提前公告告知持卡消费者……同时给出该卡在剩余期间的使用条件与退款
 * 条件（可用到哪天、能否退、退多少比例、怎么退、找谁退），公告须在客人手机应用、小程序和该商户的商场店面三处同时公示，公示期不少于后台配置的天数（业务参数，
 * 全平台统一可配，须同时配置上下限）」）＋第十三批裁决第 2 条「商户在商户门户自行发公告，平台只校验公示天数，不影响退租流程」。请求按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantCardExitNoticeJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;
    private StaffRequestFixture staffs;
    private RequestPostProcessor platform;
    private String shopId;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        staffs = new StaffRequestFixture(jdbc);
        shopId = "merchant_mcen_" + f.suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '退场测试咖啡店', 'catering', 'active', now(), now(), ?)", shopId, f.projectId);
        platform = staffs.staff("usr_mcen_pf_" + f.suffix, "platform_admin", null, null, "customer.stored_value.refund");
    }

    @Test
    void publishRejectedUntilPlatformConfiguresDays() throws Exception {
        mockMvc.perform(get("/api/v1/customer/merchant-card-exit-notices").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", f.token)).andExpect(status().isOk());
        publish(notice(40)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("EXIT_NOTICE_POLICY_NOT_CONFIGURED"));
    }

    @Test
    void policyRequiresValueWithBounds() throws Exception {
        policy(30, null, null).andExpect(status().isBadRequest());
        policy(5, 7, 90).andExpect(status().isBadRequest());
        policy(30, 7, 90).andExpect(status().isOk()).andExpect(jsonPath("$.data.publish_days").value(30))
                .andExpect(jsonPath("$.data.min_days").value(7)).andExpect(jsonPath("$.data.max_days").value(90));
        mockMvc.perform(get("/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy").with(platform))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.publish_days").value(30));
    }

    @Test
    void missingConditionRejected() throws Exception {
        policy(30, 7, 90).andExpect(status().isOk());
        Map<String, Object> body = notice(40);
        body.remove("refund_contact");
        publish(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void publishPeriodBelowSettingRejected() throws Exception {
        policy(30, 7, 90).andExpect(status().isOk());
        publish(notice(10)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("EXIT_NOTICE_PERIOD_TOO_SHORT"));
    }

    @Test
    void cardHolderSeesNoticeInApp() throws Exception {
        policy(30, 7, 90).andExpect(status().isOk());
        Instant now = Instant.now();
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord("svacc_mcen_" + f.suffix, f.customerId, null, shopId,
                5000L, "active", now, now, null, 5000L, 0L));
        String id = CustomerTicketPaymentFixture.read(publish(notice(40)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value(shopId)), "$.data.id");
        String item = "$.data[?(@.id == '" + id + "')]";
        mockMvc.perform(get("/api/v1/customer/merchant-card-exit-notices").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", f.token)).andExpect(status().isOk())
                .andExpect(jsonPath(item + ".merchant_name").value(contains("退场测试咖啡店")))
                .andExpect(jsonPath(item + ".use_until").value(contains(LocalDate.now().plusDays(60).toString())))
                .andExpect(jsonPath(item + ".refundable").value(contains(true)))
                .andExpect(jsonPath(item + ".refund_ratio_bps").value(contains(8000)))
                .andExpect(jsonPath(item + ".refund_method").value(contains("到店收银台原路退回")))
                .andExpect(jsonPath(item + ".refund_contact").value(contains("店长 王女士 0731-88888888")))
                .andExpect(jsonPath(item + ".holds_card").value(contains(true)));
    }

    /** 第 11 条（BSM L833「商户级卡的充值资金归发卡商户预收，平台不代收、不介入该笔资金的归属」；DEC-CFG-2026-08-03-11「关闭「可由后台改为平台统一收取」开关」；
     *  cg-pending-decisions 第 16 条「不恢复平台代收」）：资金模式只可为商户预收，请求按前端真实形状（蛇形键）。 */
    @Test
    void merchantCardFundModeOnlyMerchantPrepaid() throws Exception {
        cardConfig("platform_collect").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        cardConfig("merchant_prepaid").andExpect(status().isOk()).andExpect(jsonPath("$.data.fund_ownership").value("merchant_prepaid"));
    }

    // ---------- helpers ----------

    private ResultActions cardConfig(String fundOwnership) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "mcen-card-" + UUID.randomUUID());
        body.put("merchant_id", shopId);
        body.put("card_issuance_enabled", true);
        body.put("fund_ownership", fundOwnership);
        body.put("consume_order", "principal_first");
        return mockMvc.perform(post("/api/v1/loyalty/stored-value/merchant-card-config").with(platform)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private Map<String, Object> notice(int publishDays) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("use_until", LocalDate.now().plusDays(60).toString());
        body.put("refundable", true);
        body.put("refund_ratio_bps", 8000);
        body.put("refund_method", "到店收银台原路退回");
        body.put("refund_contact", "店长 王女士 0731-88888888");
        body.put("publish_until", LocalDate.now().plusDays(publishDays).toString());
        return body;
    }

    private ResultActions publish(Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        actual.put("idempotency_key", "mcen-" + UUID.randomUUID());
        RequestPostProcessor shop = staffs.staff("usr_mcen_sh_" + f.suffix, "staff", f.projectId, shopId,
                "merchant.portal.card_exit_notice.publish");
        return mockMvc.perform(post("/api/v1/merchant-portal/card-exit-notices").with(shop)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(actual)));
    }

    private ResultActions policy(Integer days, Integer min, Integer max) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "mcen-pol-" + UUID.randomUUID());
        body.put("publish_days", days);
        body.put("min_days", min);
        body.put("max_days", max);
        return mockMvc.perform(post("/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy").with(platform)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
}
