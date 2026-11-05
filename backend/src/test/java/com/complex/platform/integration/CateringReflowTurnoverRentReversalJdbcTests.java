package com.complex.platform.integration;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
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
 * 第十八批 U 段第 155 条 155-R3（ENUM:257「不论收款走平台还是走商户自有渠道、不论线上线下均计入」「冲正金额一律按该笔款项在其原账期实际计费时所适用的档位与费率
 * 原额冲回……多笔退款逐笔分别冲正，不合并折算。」；BSM:1330「连接第三方接入，餐饮和零售商户的 POS 结果回流到平台。」；DEC-R8-54；第十八批裁决 36「原月按保底收租的，
 * 该月销售跨月退款不冲回；原月按抽成额收租的，照档位费率冲回」）：餐饮、零售第三方回流退款带所退原订单的外部单号；出账时回流订单逐笔快照原账期档位费率，
 * 跨期回流退款不进退款当期基数，按原订单快照逐笔挂营业额抽成冲正应付。回流登记按后台真实请求形状（蛇形键，源 B 对账文件带商户号直落）走真实入口与真实授权链，
 * 合同工作台「排租金AR」按前端形状 {idempotency_key, period}。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CateringReflowTurnoverRentReversalJdbcTests {

    private static final Instant JULY = Instant.parse("2026-07-15T04:00:00Z");
    private static final Instant AUGUST = Instant.parse("2026-08-15T04:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "crr_prj_" + s;
    private final String merchant = "crr_mch_" + s;
    private final String contract = "crr_contract_" + s;
    private final String rule = "crr_rule_" + s;
    private final String provider = "crr-pos-" + s;

    private RequestPostProcessor admin;
    private RequestPostProcessor biller;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','回流冲正综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'回流冲正餐饮商户','catering','active',?,now(),now())",
                merchant, project);
        // 抽成规则：本商户 10%、无保底，已复核生效。
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '餐饮回流营业额抽成规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, rule, contract, merchant);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_crr_admin_" + s, "platform_admin", project, null, "integration.order_sync.manage");
        // 合同工作台「排租金AR」的后台财务账号。
        biller = staffs.staff("usr_crr_fin_" + s, "staff", project, null, "finance.ar_ap.manage");
    }

    /**
     * ① 7 月餐饮回流订单 10000 出 7 月（1000），8 月回流退款 4000 带原订单外部单号、另回流订单 20000：8 月基数不减这 4000（2000），
     * 另挂一笔按 7 月档位费率算的冲正应付 400（归 8 月、带商户）。
     */
    @Test
    void julyReflowOrderRefundedInAugustIsReversedAtTheJulyRateOutsideTheAugustBase() throws Exception {
        contract("turnover_only", 0);
        String order = reflow("order", "o1", 10_000, null, JULY);
        reflow("order", "o2", 20_000, null, AUGUST);

        periodBills("2026-07").andExpect(status().isOk());
        assertThat(rentBill("2026-07")).isEqualTo(1_000L);
        assertSnapshot(order, "[{\"rate_bps\":1000,\"amount_cent\":10000}]", false);
        String refund = reflow("refund", "r1", 4_000, "o1", AUGUST);
        periodBills("2026-08").andExpect(status().isOk());

        assertThat(rentBill("2026-08")).isEqualTo(2_000L);
        assertThat(jdbc.queryForList("SELECT amount_cent, accounting_period, source_id, merchant_id FROM ar_ap_items"
                + " WHERE source_type = 'turnover_rent_reversal' AND counterparty_id = ?", merchant))
                .containsExactly(Map.of("amount_cent", 400L, "accounting_period", "2026-08", "source_id", refund, "merchant_id", merchant));
    }

    /** ② 餐饮回流退款不带原订单外部单号 → 400 VALIDATION_FAILED（中文字段名），不落库。 */
    @Test
    void cateringRefundWithoutOriginalOrderIsRejected() throws Exception {
        Map<String, Object> body = body("refund", "r-missing", 4_000);
        mockMvc.perform(post("/api/v1/integration/order-syncs").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("原订单外部单号")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM integration_order_syncs WHERE provider_code = ?", Integer.class, provider))
                .isZero();
    }

    /**
     * ④ 超额累进逐笔定位：0～10000 按 5%、以上 10%；7 月 A 8000 在前、B 6000 在后 → 7 月抽成 900；8 月先退 B 3000、再退 A 1000 →
     * 两笔冲正 250、50 逐笔挂，8 月没有销售不出租金单。
     */
    @Test
    void twoAugustRefundsPostTwoReversalsAtTheirOwnSegments() throws Exception {
        contract("turnover_only", 0);
        jdbc.update("INSERT INTO turnover_rent_rule_tiers(id,rule_id,seq,upper_bound_cent,rate_bps) VALUES(?,?,1,10000,500),(?,?,2,NULL,1000)",
                "crr_tier1_" + s, rule, "crr_tier2_" + s, rule);
        String a = reflow("order", "a", 8_000, null, Instant.parse("2026-07-10T04:00:00Z"));
        String b = reflow("order", "b", 6_000, null, Instant.parse("2026-07-12T04:00:00Z"));
        String refundB = reflow("refund", "rb", 3_000, "b", Instant.parse("2026-08-10T04:00:00Z"));
        String refundA = reflow("refund", "ra", 1_000, "a", AUGUST);

        periodBills("2026-07").andExpect(status().isOk());
        assertThat(rentBill("2026-07")).isEqualTo(900L);
        assertSnapshot(a, "[{\"rate_bps\":500,\"amount_cent\":8000}]", false);
        assertSnapshot(b, "[{\"rate_bps\":500,\"amount_cent\":2000},{\"rate_bps\":1000,\"amount_cent\":4000}]", false);
        periodBills("2026-08").andExpect(status().isOk());
        assertThat(reversals()).containsExactly(entry(refundB, 250L), entry(refundA, 50L));
        assertThat(rentBill("2026-08")).isNull();
    }

    /**
     * ⑤ 裁决 36「原月按保底收租的，该月销售跨月退款不冲回」：纯抽成合同、规则保底 50000，7 月抽成 1000 按保底收 50000 → 快照记按保底；
     * 8 月退 7 月订单 4000 不冲回，也不进 8 月基数（8 月回流 600000 → 抽成 60000，不扣成 59600）。
     */
    @Test
    void ruleMinimumMonthIsNotReversed() throws Exception {
        contract("turnover_only", 0);
        jdbc.update("UPDATE turnover_rent_rules SET min_amount_cent = 50000 WHERE id = ?", rule);
        String order = reflow("order", "m1", 10_000, null, JULY);
        reflow("order", "m2", 600_000, null, AUGUST);
        reflow("refund", "mr", 4_000, "m1", AUGUST);

        periodBills("2026-07").andExpect(status().isOk());
        assertThat(rentBill("2026-07")).isEqualTo(50_000L);
        assertSnapshot(order, "[{\"rate_bps\":1000,\"amount_cent\":10000}]", true);
        periodBills("2026-08").andExpect(status().isOk());
        assertThat(reversals()).isEmpty();
        assertThat(rentBill("2026-08")).isEqualTo(60_000L);
    }

    /**
     * ⑦（155-R3；ENUM:257「减去当期已成功的退款金额」）：8 月回流订单 10000，8 月回流退款 4000 带原订单外部单号 → 合同工作台出 8 月：
     * 同期回流退款从原订单里减，租金按净额 6000 计 600；该订单快照 net_amount_cent＝6000、segments 合计 6000；没有营业额抽成冲正应付。
     */
    @Test
    void sameMonthReflowRefundIsDeductedFromItsOrderAndSnapshotsTheNetAmount() throws Exception {
        contract("turnover_only", 0);
        String order = reflow("order", "s1", 10_000, null, Instant.parse("2026-08-10T04:00:00Z"));
        reflow("refund", "sr", 4_000, "s1", AUGUST);

        periodBills("2026-08").andExpect(status().isOk());
        assertThat(rentBill("2026-08")).isEqualTo(600L);
        assertThat(jdbc.queryForList("SELECT period, net_amount_cent,"
                + " (SELECT sum((seg->>'amount_cent')::bigint) FROM jsonb_array_elements(segments) seg)::bigint AS segments_total"
                + " FROM turnover_rent_sale_snapshots WHERE source_kind = 'reflow_order' AND source_id = ?", order))
                .containsExactly(Map.of("period", "2026-08", "net_amount_cent", 6_000L, "segments_total", 6_000L));
        assertThat(reversals()).isEmpty();
    }

    /** 本商户合同（id 即抽成规则所挂合同），收租方式 mode、月租 baseRent，2026 全年生效。 */
    private void contract(String mode, long baseRent) {
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?,?,?,?,DATE '2026-01-01',DATE '2026-12-31','effective',?,?::jsonb,now(),now())""",
                contract, merchant, "crr_subj_" + s, "crr_subj_" + s, project,
                "{\"rentBillingMode\":\"" + mode + "\",\"baseRentCent\":" + baseRent + "}");
    }

    /** 回流登记请求体（源 B 对账文件回流：带商户号直落）。 */
    private Map<String, Object> body(String syncType, String tag, long amountCent) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "crr-" + tag + "-" + s);
        body.put("provider_code", provider);
        body.put("business_type", "catering");
        body.put("sync_type", syncType);
        body.put("external_business_id", "crr-ext-" + tag + "-" + s);
        body.put("status", "synced");
        body.put("amount_cent", amountCent);
        body.put("currency", "CNY");
        body.put("channel", "meituan");
        body.put("external_channel_order_id", "crr-ch-" + tag + "-" + s);
        body.put("merchant_id", merchant);
        return body;
    }

    /** 回流登记一笔订单或退款（退款带所退原订单的外部单号），落库时点回拨到 at，返回回流行 id。 */
    private String reflow(String syncType, String tag, long amountCent, String originalTag, Instant at) throws Exception {
        Map<String, Object> body = body(syncType, tag, amountCent);
        if (originalTag != null) {
            body.put("original_external_business_id", "crr-ext-" + originalTag + "-" + s);
        }
        ResultActions result = mockMvc.perform(post("/api/v1/integration/order-syncs").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value(merchant));
        if (originalTag != null) {
            result.andExpect(jsonPath("$.data.original_external_business_id").value("crr-ext-" + originalTag + "-" + s));
        }
        String id = json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data").path("id").asText();
        jdbc.update("UPDATE integration_order_syncs SET created_at = ?, updated_at = ? WHERE id = ?", Timestamp.from(at), Timestamp.from(at), id);
        return id;
    }

    /** 合同工作台「排租金AR」（ContractWorkbenchPage 发 {period}，adminClient 自动补 idempotency_key 并转蛇形）。 */
    private ResultActions periodBills(String period) throws Exception {
        return mockMvc.perform(post("/api/v1/contracts/" + contract + "/period-bills").with(biller)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("period", period, "idempotency_key", "crr-pb-" + UUID.randomUUID()))));
    }

    private Long rentBill(String period) {
        return jdbc.queryForList("SELECT amount_cent FROM contract_rent_bills WHERE contract_id = ? AND period = ? AND fee_type = 'rent'",
                Long.class, contract, period).stream().findFirst().orElse(null);
    }

    /** 本商户的营业额抽成冲正应付：退款回流行 → 金额（按金额降序）。 */
    private Map<String, Long> reversals() {
        Map<String, Long> out = new LinkedHashMap<>();
        jdbc.query("SELECT source_id, amount_cent FROM ar_ap_items WHERE source_type = 'turnover_rent_reversal' AND counterparty_id = ?"
                + " ORDER BY amount_cent DESC", rs -> {
                    out.put(rs.getString("source_id"), rs.getLong("amount_cent"));
                }, merchant);
        return out;
    }

    /** 该回流订单在 7 月的逐笔快照：分段（jsonb 相等）与原月是否按保底收租。 */
    private void assertSnapshot(String orderId, String segments, boolean billedByMinimum) {
        assertThat(jdbc.queryForList("SELECT period, billed_by_minimum, segments = ?::jsonb AS segments_match"
                + " FROM turnover_rent_sale_snapshots WHERE source_kind = 'reflow_order' AND source_id = ?", segments, orderId))
                .containsExactly(Map.of("period", "2026-07", "billed_by_minimum", billedByMinimum, "segments_match", true));
    }
}
