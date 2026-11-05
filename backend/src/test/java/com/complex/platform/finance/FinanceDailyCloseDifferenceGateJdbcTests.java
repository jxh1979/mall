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
 * 第十八批 S 段第 158 条 158-R1（ENUM:191「差异未关闭期间，高价值差异所涉营业日的日结不得确认锁定」「以该笔差异所涉金额与「高价值判定」条所配的
 * 同一金额阈值比较，高于阈值即高价值，其余为低价值；阈值未配置一律按高价值处理。」；ENUM:190「有差异」；ENUM:187 与 BSM:1729 按营业日、按日对账；
 * DEC-R4-2026-08-04-43 已删「已忽略」）：未关闭的高价值对账差异拦日结确认与锁定（409 DAY_CLOSE_DIFFERENCE_PENDING），待确认的日结显示「有差异」。
 * 财务对账批次没有写入方、第三方差异要渠道回流，批次、差异与回流行只能直插；生成、查询、确认、锁定日结与关闭、处置差异都按后台真实请求形状（蛇形键）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinanceDailyCloseDifferenceGateJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private FinanceRepository financeRepository;
    @Autowired private HighValueThresholdPort thresholds;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "fdg_p_" + s;
    private final String merchant = "fdg_m_" + s;
    private final String otherMerchant = "fdg_o_" + s;
    private final LocalDate day = LocalDate.now(LocalZone.ID).minusDays(3);
    private RequestPostProcessor admin;
    private RequestPostProcessor reviewer;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','对账差异闸项目','active',now(),now())",
                project);
        for (String m : new String[] {merchant, otherMerchant}) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                    + " VALUES(?,'对账差异闸商户','bar','active',now(),now(),?)", m, project);
        }
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_fdg_admin_" + s, "platform_admin", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage");
        reviewer = staffs.staff("usr_fdg_rev_" + s, "platform_admin", project, null,
                "finance.reconciliation.resolve", "integration.reconciliation.resolve");
    }

    /** ① 财务差异：未关闭的高价值差异让日结显示有差异、拒确认；另一人关闭差异后回到待确认、确认成功。 */
    @Test
    void openHighValueFinanceDifferenceBlocksConfirmUntilClosed() throws Exception {
        financeBatch("b1", day, "merchant", merchant);
        String diff = financeDifference("d1", "b1", 600_000L);
        String close = generate("main");

        list().andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].status").value("has_difference"));
        confirm(close, "c1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_DIFFERENCE_PENDING"))
                .andExpect(jsonPath("$.message").value("该营业日有未关闭的高价值对账差异，差异关闭后才能确认、锁定日结"));

        mockMvc.perform(post("/api/v1/finance/reconciliation/differences/" + diff + "/close").with(reviewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "fdg-close-" + s, "close_reason", "银行流水核对后以平台口径为准"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        list().andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].status").value("pending_confirm"));
        confirm(close, "c2").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    /** ② 低价值与边界：低于阈值、恰等于阈值（高于阈值才是高价值）的未关闭差异都不拦。 */
    @Test
    void lowValueAndThresholdEqualDifferencesDoNotBlock() throws Exception {
        assertThat(thresholds.findHighValueThresholdCent()).isEqualTo(500_000L);
        financeBatch("b1", day, "merchant", merchant);
        financeDifference("d1", "b1", 400_000L);
        financeDifference("d2", "b1", 500_000L);
        String close = generate("main");

        list().andExpect(jsonPath("$.data.items[0].status").value("pending_confirm"));
        confirm(close, "c1").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    /** ③ 阈值未配置一律按高价值：全平台那一行阈值非生效时，100 分的未关闭差异也拦。 */
    @Test
    void unconfiguredThresholdTreatsEveryOpenDifferenceAsHighValue() throws Exception {
        jdbc.update("UPDATE high_value_thresholds SET status = 'inactive'"
                + " WHERE status = 'active' AND is_draft = FALSE AND project_id IS NULL AND merchant_id IS NULL");
        assertThat(thresholds.findHighValueThresholdCent()).isNull();
        financeBatch("b1", day, "merchant", merchant);
        financeDifference("d1", "b1", 100L);
        String close = generate("main");

        confirm(close, "c1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_DIFFERENCE_PENDING"));
    }

    /** ④ 不误拦：次日批次上的、别的商户批次上的高价值未关闭差异不拦本商户本营业日的日结。 */
    @Test
    void differencesOfOtherDayOrOtherMerchantDoNotBlock() throws Exception {
        financeBatch("b1", day.plusDays(1), "merchant", merchant);
        financeDifference("d1", "b1", 600_000L);
        financeBatch("b2", day, "merchant", otherMerchant);
        financeDifference("d2", "b2", 600_000L);
        String close = generate("main");

        list().andExpect(jsonPath("$.data.items[0].status").value("pending_confirm"));
        confirm(close, "c1").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    /**
     * ⑤ 第三方差异：本营业日单日批次、回流门店为本商户的待复核高价值差异拦确认；改成 ignored（「已忽略」不是关闭出口）仍拦；
     * 改成 resolved 后确认成功；次日单日批次上的差异不拦本营业日。
     */
    @Test
    void openHighValueThirdPartyDifferenceBlocksUntilResolved() throws Exception {
        String diff = thirdPartyDifference("t1", day, 600_000L);
        thirdPartyDifference("t2", day.plusDays(1), 600_000L);
        String close = generate("main");

        list().andExpect(jsonPath("$.data.items[0].status").value("has_difference"));
        confirm(close, "c1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_DIFFERENCE_PENDING"));

        resolve(diff, "ignored").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ignored"));
        confirm(close, "c2").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_DIFFERENCE_PENDING"));

        resolve(diff, "resolved").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("resolved"));
        confirm(close, "c3").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    /** ⑥ 锁定也拦：已确认的日结之后出现未关闭的高价值差异，锁定拒 409。 */
    @Test
    void lockIsBlockedByOpenHighValueDifference() throws Exception {
        String close = generate("main");
        confirm(close, "c1").andExpect(status().isOk());
        financeBatch("b1", day, "merchant", merchant);
        financeDifference("d1", "b1", 600_000L);

        mockMvc.perform(post("/api/v1/finance/daily-closes/" + close + "/lock").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "fdg-lock-" + s))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_DIFFERENCE_PENDING"));
    }

    // ---------- helpers ----------

    /** 后台日结区块生成本商户该营业日日结的真实请求形状，并补齐五类最终标记（143-R2 前提），返回日结 ID。 */
    private String generate(String tag) throws Exception {
        String body = mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "fdg-gen-" + tag + "-" + s,
                                "business_date", day.toString(), "scope_type", "merchant", "scope_id", merchant))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.data.id");
        DailyCloseFixture.markAllFinal(financeRepository, id);
        return id;
    }

    private ResultActions list() throws Exception {
        return mockMvc.perform(get("/api/v1/finance/daily-closes").with(admin)
                .param("business_date", day.toString()).param("scope_type", "merchant").param("scope_id", merchant));
    }

    private ResultActions confirm(String dailyCloseId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/confirm").with(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "fdg-confirm-" + tag + "-" + s))));
    }

    /** 第三方差异复核（处置）的真实请求形状。 */
    private ResultActions resolve(String differenceId, String to) throws Exception {
        return mockMvc.perform(post("/api/v1/integration/reconciliation/differences/" + differenceId + "/resolve").with(reviewer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "fdg-resolve-" + to + "-" + s,
                        "operator_user_id", "usr_fdg_rev_" + s, "status", to, "resolution_note", "与渠道对账单逐笔核对"))));
    }

    /** 前置态：财务对账批次（主代码没有批次写入方，只能直插）。 */
    private void financeBatch(String tag, LocalDate businessDate, String scopeType, String scopeId) {
        jdbc.update("INSERT INTO finance_reconciliation_batches (id, batch_no, scope_type, scope_id, business_date, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, 'open', now())", "fdg_" + tag + "_" + s, "FDG-" + tag + "-" + s, scopeType, scopeId,
                Date.valueOf(businessDate));
    }

    /** 前置态：财务对账批次上一条未关闭（open）的金额差异，返回差异 ID。 */
    private String financeDifference(String tag, String batchTag, long amountCent) {
        String id = "fdg_" + tag + "_" + s;
        jdbc.update("INSERT INTO finance_reconciliation_differences (id, batch_id, difference_type, business_ref, amount_cent,"
                + " expected_amount_cent, actual_amount_cent, source, status, created_at)"
                + " VALUES (?, ?, 'amount', ?, ?, 0, ?, 'system', 'open', now())",
                id, "fdg_" + batchTag + "_" + s, id, amountCent, amountCent);
        return id;
    }

    /** 前置态：对账期间为单日 businessDate 的第三方批次、门店为本商户的回流行与一条待复核差异，返回差异 ID。 */
    private String thirdPartyDifference(String tag, LocalDate businessDate, long amountCent) {
        String batch = "fdg_tb_" + tag + "_" + s;
        String external = "fdg_ext_" + tag + "_" + s;
        jdbc.update("INSERT INTO integration_reconciliation_batches (id, provider_code, business_type, period_start, period_end, status,"
                + " created_at, updated_at) VALUES (?, 'pos_fdg', 'catering', ?, ?, 'difference_found', now(), now())",
                batch, Date.valueOf(businessDate), Date.valueOf(businessDate));
        jdbc.update("INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, external_business_id, status,"
                + " amount_cent, created_at, updated_at, merchant_id) VALUES (?, 'pos_fdg', 'catering', 'order', ?, 'synced', ?, now(), now(), ?)",
                "fdg_ios_" + tag + "_" + s, external, amountCent, merchant);
        String id = "fdg_td_" + tag + "_" + s;
        jdbc.update("INSERT INTO integration_reconciliation_differences (id, batch_id, provider_code, business_type, external_business_id,"
                + " difference_type, internal_amount_cent, provider_amount_cent, difference_amount_cent, status, created_at, updated_at)"
                + " VALUES (?, ?, 'pos_fdg', 'catering', ?, 'amount_mismatch', ?, 0, ?, 'needs_review', now(), now())",
                id, batch, external, amountCent, amountCent);
        return id;
    }
}
