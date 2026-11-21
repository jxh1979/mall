package com.complex.platform.dividend;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.EntityTaxFixture;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BR 段第 274 条 274-R1、274-R2（ENUM:297「参股方、主理人和其他股东按每家采用参股经营的经营主体工商登记的真实持股比例分配；酒吧的参股方为娱乐平台
 * 公司，引进参股的游乐项目的参股方为引进该项目的域主管公司」；BSM:1774「分红基数按「净利润」算：一个周期里的经营收入（按含税实收口径）……」；BSM:753「游乐公司
 * 再按已批准的内部结算价或域内规则，与实际提供服务的游乐商户结算。」；决策台账 DEC-R3-2026-08-04-17）。
 *
 * <p>夹具随测试事务回滚：综合体 2026-08 两类以上数据全标最终；游乐商户 M（经营引进参股的游乐项目）经营主体有已生效税费登记；M 的游乐域内结算行（357-R2 入场时
 * 固化）由夹具直写。方案与出单走分红页真实请求形状（蛇形键）。修复前收入域闭集只有酒吧、售票：建方案 400，状态断言红。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DividendAmusementRevenueJdbcIntegrationTests {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_31 = LocalDate.of(2026, 8, 31);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "dam_p_" + s;
    private final String shop = "dam_m_" + s;
    private final String plan = "divp_dam_" + s;
    private int seq;
    private RequestPostProcessor finance;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','游乐分红综合体','active',TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'amusement','active',?,TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", shop, "引进参股游乐商户" + s, project);
        markRangeFinal(jdbc, "project", project, AUG_1, AUG_31);
        EntityTaxFixture.taxDeclaredForAllMonths(jdbc, shop);
        finance = new StaffRequestFixture(jdbc).staff("usr_dam_fin_" + s, "platform_admin", null, null,
                "finance.dividend.manage", "finance.dividend.view");
    }

    /** ①：财务给游乐商户建只计游乐域内结算的方案，返回 200 并带回该收入域。 */
    @Test
    void financeCreatesAmusementOnlyPlanForAmusementMerchant() throws Exception {
        String effective = businessDays.merchantBusinessDate(null, shop, Instant.now()).plusDays(1).toString();
        mockMvc.perform(post("/api/v1/finance/dividends/plans").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dam-plan-" + s + "\",\"merchant_id\":\"" + shop + "\",\"principal_user_id\":\"usr_dam_principal\","
                                + "\"period_type\":\"monthly\",\"period_min_months\":1,\"period_max_months\":12,\"payout_pay_slot\":1,"
                                + "\"platform_share_min_bps\":5000,\"platform_share_max_bps\":9000,\"revenue_domains\":[\"amusement\"],"
                                + "\"effective_from\":\"" + effective + "\",\"reason\":\"按工商登记持股比例\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue_domains", hasItem("amusement")));
    }

    /**
     * ②（274-R1）④（274-R2）：期内两笔核销 1000006、3000 与其中 3000 那笔的撤销计入，期外 9 月一笔不计：收入与 revenue_amusement 行 1000006。平台（参股方）60%、
     * 个人股东 10%、公司股东 5%、主理人 25% 各自四舍五入出尾差 −1：参股方分成行写「参股方（引进该项目的域主管公司）」，尾差行写「本经营主体」，不写酒吧公司。
     */
    @Test
    void amusementDomainSettlementIsTheAmusementMerchantsRevenue() throws Exception {
        plan("amusement");
        jdbc.update("INSERT INTO dividend_plan_shareholders (id, plan_id, shareholder_name, shareholder_type, share_bps, sort_order)"
                + " VALUES (?, ?, '股东乙', 'individual', 1000, 0), (?, ?, '丙公司', 'company', 500, 1)", "dps_damb_" + s, plan, "dps_damc_" + s, plan);
        String big = line("settled", 1_000_006, LocalDate.of(2026, 8, 5), null);
        String small = line("settled", 3000, LocalDate.of(2026, 8, 10), null);
        line("reversed", -3000, LocalDate.of(2026, 8, 20), small);
        line("settled", 3000, LocalDate.of(2026, 9, 2), null);
        assertThat(big).isNotNull();

        String statement = draft("am");

        assertThat(jdbc.queryForObject("SELECT revenue_cent FROM dividend_statements WHERE id = ?", Long.class, statement)).isEqualTo(1_000_006L);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'revenue_amusement'",
                Long.class, statement)).isEqualTo(1_000_006L);
        assertThat(detail(statement, "platform_share")).contains("参股方（引进该项目的域主管公司）").doesNotContain("平台分成");
        assertThat(detail(statement, "rounding_residual")).contains("本经营主体").doesNotContain("酒吧公司");
    }

    /** ③：同一商户方案收入域不含游乐域内结算时，收入不取这些行、没有 revenue_amusement 行。 */
    @Test
    void planWithoutAmusementDomainIgnoresDomainSettlementLines() throws Exception {
        plan("bar");
        line("settled", 3000, LocalDate.of(2026, 8, 10), null);

        String statement = draft("bar");

        assertThat(jdbc.queryForObject("SELECT revenue_cent FROM dividend_statements WHERE id = ?", Long.class, statement)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'revenue_amusement'",
                Integer.class, statement)).isZero();
    }

    /** ⑤（274-R1）：分红按经营主体取，商户经营的别的综合体里游乐项目的域内结算同样计入，不按商户所属综合体截取。 */
    @Test
    void domainSettlementInAnotherComplexCountsTowardsTheMerchant() throws Exception {
        plan("amusement");
        String other = "dam_q_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','游乐分红另一综合体','active',TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", other);
        markRangeFinal(jdbc, "project", other, AUG_1, AUG_31);
        line("settled", 2000, LocalDate.of(2026, 8, 6), null);
        String elsewhere = line("settled", 5000, LocalDate.of(2026, 8, 12), null);
        jdbc.update("UPDATE amusement_domain_settlement_lines SET project_id = ? WHERE verification_id = ?", other, elsewhere);

        String statement = draft("other");

        assertThat(jdbc.queryForObject("SELECT amount_cent FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'revenue_amusement'",
                Long.class, statement)).isEqualTo(7000L);
    }

    private void plan(String domains) {
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, platform_share_bps, period_type, revenue_domains,"
                + " include_inventory_cost, status, payout_pay_slot, effective_from, reason, period_min_months, period_max_months, principal_type,"
                + " arrears_offset_enabled, platform_share_min_bps, platform_share_max_bps) VALUES (?, ?, 'usr_dam_principal', 6000, 'monthly', ?, TRUE,"
                + " 'active', 1, DATE '2000-01-01', '工商登记持股比例', 1, 12, 'individual', FALSE, 5000, 9000)", plan, shop, domains);
    }

    /** M 的一行游乐域内结算（357-R2 入场时固化）；reversed 行 verification 取所冲的 settled 行。返回票务核销号。 */
    private String line(String kind, long amount, LocalDate businessDate, String verification) {
        String v = verification != null ? verification : "ver_dam_" + (++seq) + "_" + s;
        jdbc.update("""
                INSERT INTO amusement_domain_settlement_lines (id, verification_id, kind, amusement_project_id, project_id, provider_merchant_id,
                    payer_entity_id, amount_cent, price_catalog_entry_id, business_at, business_date)
                VALUES (?, ?, ?, ?, ?, ?, 'be_amusement', ?, ?, now(), ?)
                """, "adsl_dam_" + kind + "_" + v, v, kind, "ap_dam_" + s, project, shop, amount, "ipc_dam_" + s, Date.valueOf(businessDate));
        return v;
    }

    /** 分红页期间作业「正式单」的真实请求，返回草稿单号。 */
    private String draft(String tag) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dam-c-" + tag + "-" + s + "\",\"period_label\":\"2026-08\",\"kind\":\"official\","
                                + "\"merchant_id\":\"" + shop + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.computed[0].id");
    }

    private String detail(String statement, String lineType) {
        List<String> details = jdbc.queryForList("SELECT detail FROM dividend_statement_lines WHERE statement_id = ? AND line_type = ?",
                String.class, statement, lineType);
        assertThat(details).hasSize(1);
        return details.getFirst();
    }
}
