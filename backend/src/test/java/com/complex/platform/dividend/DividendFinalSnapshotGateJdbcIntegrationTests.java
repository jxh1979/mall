package com.complex.platform.dividend;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
 * 第十八批 V 段第 159 条 159-R5 正式分红单闸真库回归。依据：ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；
 * BSM:1774「分红基数按「净利润」算：一个周期里的经营收入（按含税实收口径），减掉这期间的全部成本、费用与税费之后的余额」。
 *
 * <p>夹具随测试事务回滚：综合体与酒吧 D 都建档 2026-05-01，D 有月度、只计酒吧收入的分红方案。D 的 8 月日结除 08-20 外五类全标；08-20 的日结经真实
 * 生成、标记接口（蛇形键）由持 finance.daily_close.lock、负责范围覆盖该综合体的财务 BP 生成并标除「当日考勤记录」外四类。持 finance.dividend.manage
 * 的平台管理员按前端蛇形键走真实计算入口与授权链。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DividendFinalSnapshotGateJdbcIntegrationTests {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_20 = LocalDate.of(2026, 8, 20);
    private static final LocalDate AUG_31 = LocalDate.of(2026, 8, 31);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "dfg_prj_" + s;

    // 修复前正式单不判五类标记：第一次计算即出 1 张正式单，computed 长度断言红。
    @Test
    void officialDividendIsSkippedUntilEveryBusinessDayInPeriodHasAllFiveCategoriesFinal() throws Exception {
        project();
        String bar = "dfg_bar_" + s;
        String barName = "分红闸酒吧" + s;
        barWithMonthlyPlan(bar, barName, Instant.parse("2026-04-30T16:00:00Z"));

        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_20.minusDays(1));
        markRangeFinal(jdbc, "bar", bar, AUG_20.plusDays(1), AUG_31);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor financeBp = staffs.staff("usr_dfg_bp_" + s, "staff", project, null, "finance.daily_close.lock");
        String close20 = JsonPath.read(mockMvc.perform(post("/api/v1/finance/daily-closes").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-gen-" + s + "\",\"business_date\":\"" + AUG_20
                                + "\",\"scope_type\":\"bar\",\"scope_id\":\"" + bar + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        for (String category : List.of("orders_verifications", "refunds_reversals", "production_handover", "anomaly_resolutions")) {
            finalMark(financeBp, close20, category, "dfg-" + category + "-" + s);
        }

        RequestPostProcessor admin = staffs.staff("usr_dfg_admin_" + s, "platform_admin", project, null, "finance.dividend.manage");
        compute(admin, bar, "dfg-div-1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].merchant_id").value(bar))
                .andExpect(jsonPath("$.data.skipped[0].reason")
                        .value("以下营业日数据尚未标记为最终，不能出正式分红单：2026-08-20 " + barName + "：当日考勤记录"));
        assertThat(officialStatements(bar)).isZero();

        finalMark(financeBp, close20, "attendance", "dfg-attendance-" + s);
        compute(admin, bar, "dfg-div-2-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andExpect(jsonPath("$.data.computed[0].status").value("draft"));
        assertThat(officialStatements(bar)).isEqualTo(1);
    }

    /** 建档 2026-08-15T03:00:00Z（北京 11:00，8-14 营业日在 8-15 10:00 结束、不查）的酒吧：只标 08-15～08-31 即可出 8 月正式单。 */
    @Test
    void businessDaysBeforeOnboardingAreNotRequiredForOfficialDividend() throws Exception {
        project();
        String bar = "dfg_late_" + s;
        barWithMonthlyPlan(bar, "分红闸新开酒吧" + s, Instant.parse("2026-08-15T03:00:00Z"));
        markRangeFinal(jdbc, "bar", bar, LocalDate.of(2026, 8, 15), AUG_31);

        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_dfg_adminl_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        compute(admin, bar, "dfg-divl-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1));
        assertThat(officialStatements(bar)).isEqualTo(1);
    }

    // ---------- helpers ----------

    private void project() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','分红闸综合体','active',TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", project);
    }

    /** 酒吧与其月度、只计酒吧收入、不扣销货成本的生效分红方案（避开票务分账前置）。 */
    private void barWithMonthlyPlan(String id, String name, Instant created) {
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,?,now())", id, name, project, Timestamp.from(created));
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status)"
                + " VALUES (?, ?, 'usr_dfg_principal', 'monthly', 'bar', FALSE, 'active')", "divp_" + id, id);
    }

    /** 分红计算页「正式单」的真实请求形状（蛇形键）。 */
    private ResultActions compute(RequestPostProcessor who, String bar, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"period_label\":\"2026-08\",\"kind\":\"official\","
                        + "\"merchant_id\":\"" + bar + "\"}"));
    }

    private Integer officialStatements(String bar) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dividend_statements WHERE merchant_id = ? AND kind = 'official'"
                + " AND period_label = '2026-08' AND status <> 'voided'", Integer.class, bar);
    }

    /** 143-R2 五类最终标记的真实请求形状（蛇形键）。 */
    private void finalMark(RequestPostProcessor who, String dailyCloseId, String category, String key) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/final-marks").with(who)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key + "\",\"category\":\"" + category + "\"}"))
                .andExpect(status().isOk());
    }
}
