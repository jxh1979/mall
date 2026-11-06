package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.LocalZone;
import com.complex.platform.inventory.HighValueThresholdPort;
import com.complex.platform.support.DailyCloseFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.sql.Date;
import java.time.LocalDate;
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
 * 第十八批 Y 段第 182 条 182-R1（ENUM:191「差异未关闭期间，高价值差异所涉营业日的日结不得确认锁定；低价值差异单独列示带账进入下期。」
 * 「以该笔差异所涉金额与「高价值判定」条所配的同一金额阈值比较，高于阈值即高价值，其余为低价值；阈值未配置一律按高价值处理。」）：
 * 日结汇总单列 low_value_difference——营业日不晚于本日、本范围仍未关闭的低价值对账差异的笔数与金额合计（含上期带入），关闭即不再列示。
 * 批次与差异直插（前置态）；生成、确认日结与查汇总按后台真实请求形状（蛇形键）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinanceDailyCloseLowValueDifferenceJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private FinanceRepository financeRepository;
    @Autowired private HighValueThresholdPort thresholds;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "fdl_p_" + s;
    private final String merchant = "fdl_m_" + s;
    private final String otherMerchant = "fdl_o_" + s;
    private final LocalDate day = LocalDate.now(LocalZone.ID).minusDays(3);
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','低价值差异项目','active',now(),now())",
                project);
        for (String m : new String[] {merchant, otherMerchant}) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                    + " VALUES(?,'低价值差异酒吧','bar','active',now(),now(),?)", m, project);
        }
        admin = new StaffRequestFixture(jdbc).staff("usr_fdl_admin_" + s, "platform_admin", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage");
    }

    /**
     * ① 带账进入下期：前一营业日与本营业日批次上未关闭的低价值差异（各 100000，低于出厂阈值 500000）都计入本日；已关闭的低价值、高价值的不计；
     * ⑤ 次日批次上的、别的商户批次上的不计入本日。
     */
    @Test
    void carriesOpenLowValueDifferencesUpToBusinessDate() throws Exception {
        financeBatch("b0", day.minusDays(1), merchant);
        financeDifference("d0", "b0", 100_000L, "pending_identification");
        financeDifference("h0", "b0", 600_000L, "pending_identification");
        financeBatch("b1", day, merchant);
        financeDifference("d1", "b1", 100_000L, "pending_handling");
        financeDifference("c1", "b1", 100_000L, "closed");
        financeBatch("b2", day.plusDays(1), merchant);
        financeDifference("d2", "b2", 100_000L, "pending_identification");
        financeBatch("b3", day, otherMerchant);
        financeDifference("d3", "b3", 100_000L, "pending_identification");

        lowValueRow(generate("main"), 2, 200_000);
    }

    /** ② 阈值未配置一律按高价值：全平台那一行阈值非生效时，低价值一行记 0 笔。 */
    @Test
    void unconfiguredThresholdListsNoLowValueDifference() throws Exception {
        jdbc.update("UPDATE high_value_thresholds SET status = 'inactive'"
                + " WHERE status = 'active' AND is_draft = FALSE AND project_id IS NULL AND merchant_id IS NULL");
        assertThat(thresholds.findHighValueThresholdCent()).isNull();
        financeBatch("b1", day, merchant);
        financeDifference("d1", "b1", 100_000L, "pending_identification");

        lowValueRow(generate("main"), 0, 0);
    }

    /** ③ 恰等于阈值（高于阈值才是高价值）的未关闭差异计入低价值。 */
    @Test
    void thresholdEqualDifferenceCountsAsLowValue() throws Exception {
        assertThat(thresholds.findHighValueThresholdCent()).isEqualTo(500_000L);
        financeBatch("b1", day, merchant);
        financeDifference("d1", "b1", 500_000L, "pending_identification");

        lowValueRow(generate("main"), 1, 500_000);
    }

    /** ④ 低价值差异不拦确认；确认时按当前数据重算，确认后汇总仍列示（含生成后新出现的那笔）。 */
    @Test
    void lowValueDifferenceDoesNotBlockConfirmAndStaysListedAfterRecompute() throws Exception {
        financeBatch("b0", day.minusDays(1), merchant);
        financeDifference("d0", "b0", 100_000L, "pending_identification");
        String close = generate("main");
        lowValueRow(close, 1, 100_000);
        financeBatch("b1", day, merchant);
        financeDifference("d1", "b1", 100_000L, "pending_identification");

        mockMvc.perform(post("/api/v1/finance/daily-closes/" + close + "/confirm").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "fdl-confirm-" + s))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("confirmed"));
        lowValueRow(close, 2, 200_000);
    }

    /** 第三方差异：对账期间起日不晚于本日、回流门店为本商户、未处置的低价值差异计入；已处置的、期间起日在次日的不计。 */
    @Test
    void openLowValueThirdPartyDifferencesAreCarried() throws Exception {
        thirdPartyDifference("t0", day.minusDays(1), 100_000L, "needs_review");
        thirdPartyDifference("t1", day, 100_000L, "resolved");
        thirdPartyDifference("t2", day.plusDays(1), 100_000L, "needs_review");

        lowValueRow(generate("main"), 1, 100_000);
    }

    // ---------- helpers ----------

    /** 后台日结区块生成本酒吧该营业日日结的真实请求形状，并补齐五类最终标记（143-R2 前提），返回日结 ID。 */
    private String generate(String tag) throws Exception {
        String body = mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "fdl-gen-" + tag + "-" + s,
                                "business_date", day.toString(), "scope_type", "bar", "scope_id", merchant))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.data.id");
        DailyCloseFixture.markAllFinal(financeRepository, id);
        return id;
    }

    /** 查日结汇总，low_value_difference 一行的笔数、金额，退款恒 0。 */
    private ResultActions lowValueRow(String dailyCloseId, int count, int amountCent) throws Exception {
        return mockMvc.perform(get("/api/v1/finance/daily-closes/" + dailyCloseId + "/summaries").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'low_value_difference')].order_count").value(count))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'low_value_difference')].amount_cent").value(amountCent))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'low_value_difference')].refund_cent").value(0));
    }

    /** 前置态：bar 范围的财务对账批次。 */
    private void financeBatch(String tag, LocalDate businessDate, String scopeId) {
        jdbc.update("INSERT INTO finance_reconciliation_batches (id, batch_no, scope_type, scope_id, business_date, status, created_at)"
                + " VALUES (?, ?, 'bar', ?, ?, 'open', now())", "fdl_" + tag + "_" + s, "FDL-" + tag + "-" + s, scopeId,
                Date.valueOf(businessDate));
    }

    /** 前置态：财务对账批次上一条给定状态的金额差异。 */
    private void financeDifference(String tag, String batchTag, long amountCent, String status) {
        String id = "fdl_" + tag + "_" + s;
        jdbc.update("INSERT INTO finance_reconciliation_differences (id, batch_id, difference_type, business_ref, amount_cent,"
                + " expected_amount_cent, actual_amount_cent, source, status, created_at)"
                + " VALUES (?, ?, 'amount', ?, ?, 0, ?, 'system', ?, now())",
                id, "fdl_" + batchTag + "_" + s, id, amountCent, amountCent, status);
    }

    /** 前置态：对账期间为单日 businessDate 的第三方批次、门店为本商户的回流行与一条给定状态的差异。 */
    private void thirdPartyDifference(String tag, LocalDate businessDate, long amountCent, String status) {
        String batch = "fdl_tb_" + tag + "_" + s;
        String external = "fdl_ext_" + tag + "_" + s;
        jdbc.update("INSERT INTO integration_reconciliation_batches (id, provider_code, business_type, period_start, period_end, status,"
                + " created_at, updated_at) VALUES (?, 'pos_fdl', 'catering', ?, ?, 'difference_found', now(), now())",
                batch, Date.valueOf(businessDate), Date.valueOf(businessDate));
        jdbc.update("INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, external_business_id, status,"
                + " amount_cent, created_at, updated_at, merchant_id) VALUES (?, 'pos_fdl', 'catering', 'order', ?, 'synced', ?, now(), now(), ?)",
                "fdl_ios_" + tag + "_" + s, external, amountCent, merchant);
        jdbc.update("INSERT INTO integration_reconciliation_differences (id, batch_id, provider_code, business_type, external_business_id,"
                + " difference_type, internal_amount_cent, provider_amount_cent, difference_amount_cent, status, created_at, updated_at)"
                + " VALUES (?, ?, 'pos_fdl', 'catering', ?, 'amount_mismatch', ?, 0, ?, ?, now(), now())",
                "fdl_td_" + tag + "_" + s, batch, external, amountCent, amountCent, status);
    }
}
