package com.complex.platform.dividend;

// 文件版本：V0.06

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.EntityTaxFixture;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "dfg_prj_" + s;
    // 210-R3：方案商户的经营主体、税费登记人与财务、税务、法务三条线的审批人。
    private final String entity = "dfg_be_" + s;
    private final String financeApprover = "usr_dfg_fr_" + s;
    private final String taxApprover = "usr_dfg_tr_" + s;
    private final String legalApprover = "usr_dfg_lr_" + s;
    private RequestPostProcessor taxAdmin;
    private RequestPostProcessor taxFinance;
    private RequestPostProcessor financeReviewer;
    private RequestPostProcessor taxReviewer;
    private RequestPostProcessor legalReviewer;

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

    /**
     * 第十八批 W 段 207-R1（PGM:287「状态……报错……必须全部为中文，不得出现任何英文（如 active/draft 英文状态……）」）：
     * 正式单登记依法可分配金额、提交审批后，同期再出单的跳过原因与再提交的状态冲突报错都写中文状态。
     * 修复前拼 pending_review：跳过原因断言红。
     */
    @Test
    void skipReasonAndStateConflictShowChineseStatementStatus() throws Exception {
        project();
        String bar = "dfg_st_" + s;
        barWithMonthlyPlan(bar, "分红状态酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_31);
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_dfg_admins_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        String draft = JsonPath.read(compute(admin, bar, "dfg-divs-1-" + s)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.computed[0].id");
        String confirmed = JsonPath.read(mockMvc.perform(post("/api/v1/finance/dividends/statements/" + draft + "/legal-confirmation")
                        .with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-legal-" + s + "\",\"legal_distributable_cent\":100000,"
                                + "\"tax_accounting_ref\":\"税务核算" + s + "\",\"resolution_ref\":\"分配决议" + s + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        submit(admin, confirmed, "dfg-submit-1-" + s).andExpect(status().isOk());

        compute(admin, bar, "dfg-divs-2-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].merchant_id").value(bar))
                .andExpect(jsonPath("$.data.skipped[0].reason").value("已有计算单进入审批流（待审批），不重算"));
        submit(admin, confirmed, "dfg-submit-2-" + s)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("只有草稿单可以提交审阅（当前状态：待审批）"));
    }

    /**
     * 18W 复核修复（PGM:287「展示给用户的一切文字，包括……提示、报错……必须全部为中文，不得出现任何英文」）：
     * 期间作业类别选「月度预览」、期间标签按占位提示填季度 2026-Q3，按前端蛇形键走真实计算入口与授权链，400 报错不含英文字母。
     * 修复前报错为「preview 只支持月度期间标签（如 2026-07）」：断言红。
     */
    @Test
    void previewWithNonMonthPeriodLabelIsRejectedInChinese() throws Exception {
        project();
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_dfg_adminp_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        String message = JsonPath.read(mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-prev-" + s + "\",\"period_label\":\"2026-Q3\",\"kind\":\"preview\"}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString(), "$.message");
        assertThat(message).contains("2026-07").doesNotContainPattern("[A-Za-z]");
    }

    /**
     * 第十八批 AC 段 211-R2（BSM:1787「发放走应付款项，落在三个付账日之一，具体哪一个由该公司在分红周期配置中指定」；DEC-R3-2026-08-04-05；
     * 第十八批裁决 44「审批通过后给主理人的应付和留存释放的应付都落在当天或之后第一个该付账日」；BSM:1683「遇法定节假日或非银行工作日顺延至下一个
     * 银行工作日」）：方案已批准版本指定第二个付账日并开发放保护线，财务出正式单、登记依法可分配金额、提交，另一账号审批后释放留存。
     * 主理人应付与留存释放后的应付到期日＝当天营业日或之后第一个第二付账日，该日记为非银行工作日时顺延到下一银行工作日。修复前到期日＝审批当天：断言红。
     */
    @Test
    void payoutAndReleasedReserveFallDueOnPlanPayDay() throws Exception {
        project();
        String bar = "dfg_pay_" + s;
        merchant(bar, "分红付账日酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_31);
        confirmedTicketRevenue(bar, 1_000_000L);
        EntityTaxFixture.taxDeclaredForAllMonths(jdbc, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor finance = staffs.staff("usr_dfg_payf_" + s, "platform_admin", project, null, "finance.dividend.manage");
        RequestPostProcessor approver = staffs.staff("usr_dfg_paya_" + s, "platform_admin", project, null, "finance.dividend.manage");
        // 234-R1：出历史期间单用直接落的已批准版本（自 2000-01-01 起，第二个付账日、开发放保护线留 10%）。
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status,"
                + " payout_pay_slot, payout_floor_enabled, payout_floor_bps, effective_from) VALUES (?, ?, 'usr_dfg_principal', 'monthly', 'ticket',"
                + " TRUE, 'active', 2, TRUE, 1000, DATE '2000-01-01')", "divp_" + bar, bar);
        String draft = JsonPath.read(compute(finance, bar, "dfg-pay-c-" + s)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.computed[0].id");
        String confirmed = JsonPath.read(mockMvc.perform(post("/api/v1/finance/dividends/statements/" + draft + "/legal-confirmation")
                        .with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-pay-legal-" + s + "\",\"legal_distributable_cent\":1000000,"
                                + "\"tax_accounting_ref\":\"税务核算" + s + "\",\"resolution_ref\":\"分配决议" + s + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        submit(finance, confirmed, "dfg-pay-s-" + s).andExpect(status().isOk());

        // 当天营业日或之后第一个第二付账日记为非银行工作日（随测试事务回滚），到期日应顺延。
        LocalDate today = businessDays.merchantBusinessDate(null, bar, Instant.now());
        InternalPayDayFixtures.allWorking(jdbc, today, today.plusDays(70));
        LocalDate slotDay = secondPayDayOnOrAfter(today);
        jdbc.update("UPDATE ticket_points_calendar_days SET bank_working_day = FALSE WHERE calendar_date = ?", slotDay);

        mockMvc.perform(post("/api/v1/finance/dividends/statements/" + confirmed + "/approve").with(approver)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idempotency_key\":\"dfg-pay-a-" + s + "\"}"))
                .andExpect(status().isOk());
        Instant approvedAt = jdbc.queryForObject("SELECT approved_at FROM dividend_statements WHERE id = ?", Timestamp.class, confirmed)
                .toInstant();
        LocalDate payoutDue = bankDayOnOrAfter(secondPayDayOnOrAfter(businessDays.merchantBusinessDate(null, bar, approvedAt)));
        assertThat(payoutDue).isAfter(slotDay);
        assertThat(dueDate("dividend_payout", confirmed)).isEqualTo(payoutDue);

        mockMvc.perform(post("/api/v1/finance/dividends/statements/" + confirmed + "/release-reserve").with(approver)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idempotency_key\":\"dfg-pay-r-" + s + "\"}"))
                .andExpect(status().isOk());
        Instant releasedAt = jdbc.queryForObject("SELECT released_at FROM dividend_reserve_releases WHERE statement_id = ?",
                Timestamp.class, confirmed).toInstant();
        assertThat(dueDate("dividend_reserve_released", confirmed))
                .isEqualTo(bankDayOnOrAfter(secondPayDayOnOrAfter(businessDays.merchantBusinessDate(null, bar, releasedAt))));
    }

    /**
     * 211-R2 发放付账日建方案必填、取 1～3；控制席并入（BSM:1774「分红基数按「净利润」算：一个周期里的经营收入（按含税实收口径），减掉这期间的
     * 全部成本、费用与税费之后的余额」）：建方案、改方案关掉销货成本或去掉任一类费用一律 400。按前端蛇形键走真实入口与授权链。修复前前四次均建成：断言红。
     */
    @Test
    void planRequiresPayoutPaySlotAndCannotDropCostOrExpenses() throws Exception {
        project();
        String bar = "dfg_cfg_" + s;
        merchant(bar, "分红方案校验酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff("usr_dfg_cfg_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        // 234-R1：提交版本必填生效起始日期（晚于当前营业日）与理由；244-R1：新建必填周期上下限（月～年）；272-R1：新建必填平台比例上下限。校验顺序不变，cfg-5 落成待最高权限审批的版本。
        String version = ",\"effective_from\":\"" + businessDays.merchantBusinessDate(null, bar, Instant.now()).plusDays(1)
                + "\",\"reason\":\"按工商登记持股比例\"";
        String base = "\"merchant_id\":\"" + bar + "\",\"principal_user_id\":\"usr_dfg_principal\",\"period_type\":\"monthly\","
                + "\"period_min_months\":1,\"period_max_months\":12,\"platform_share_min_bps\":5000,\"platform_share_max_bps\":9000" + version;
        createPlan(finance, "{\"idempotency_key\":\"dfg-cfg-1-" + s + "\"," + base + "}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("发放付账日未填写"));
        createPlan(finance, "{\"idempotency_key\":\"dfg-cfg-2-" + s + "\"," + base + ",\"payout_pay_slot\":4}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("发放付账日")));
        createPlan(finance, "{\"idempotency_key\":\"dfg-cfg-3-" + s + "\"," + base + ",\"payout_pay_slot\":1,\"include_inventory_cost\":false}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("销货成本")));
        createPlan(finance, "{\"idempotency_key\":\"dfg-cfg-4-" + s + "\"," + base + ",\"payout_pay_slot\":1,\"expense_types\":[\"labor\"]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("费用")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dividend_plans WHERE merchant_id = ?", Integer.class, bar)).isZero();

        String planId = JsonPath.read(createPlan(finance, "{\"idempotency_key\":\"dfg-cfg-5-" + s + "\"," + base + ",\"payout_pay_slot\":1}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(put("/api/v1/finance/dividends/plans/" + planId).with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-cfg-6-" + s + "\",\"include_inventory_cost\":false" + version + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("销货成本")));
        mockMvc.perform(put("/api/v1/finance/dividends/plans/" + planId).with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-cfg-7-" + s + "\",\"expense_types\":[\"rent_allocation\",\"energy\"]" + version + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("费用")));
        assertThat(jdbc.queryForMap("SELECT include_inventory_cost, expense_types, payout_pay_slot FROM dividend_plans WHERE id = ?", planId))
                .containsEntry("include_inventory_cost", true)
                .containsEntry("expense_types", "rent_allocation,energy,property_fee,labor,amortization,other")
                .containsEntry("payout_pay_slot", 1);
    }

    /**
     * 第十八批 AQ 段第 244 条 244-R1（ENUM:299「分红周期可配置（业务参数，按经营主体可配，酒吧商户作为独立经营主体同样适用，须同时配置上下限）」；
     * BSM:521「超出该区间的配置一律拒绝生效，未填写上下限的参数不得上线」；BSM:1777「分红周期一律取月及月的整数倍粒度（如月、季、半年、年）」）：
     * 上下限按月数（月、季、半年、年＝1、3、6、12），建方案必填，周期须落在上下限之内；改方案不填上下限沿用改前版本。按分红页方案弹窗的蛇形键走真实
     * 入口与授权链。修复前上下限无处落：月度配季～年照样建成，第一个 400 断言红。
     */
    @Test
    void planPeriodMustFallWithinRequiredBounds() throws Exception {
        project();
        String bar = "dfg_bnd_" + s;
        merchant(bar, "分红周期上下限酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff("usr_dfg_bnd_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        LocalDate tomorrow = businessDays.merchantBusinessDate(null, bar, Instant.now()).plusDays(1);
        String base = "\"merchant_id\":\"" + bar + "\",\"principal_user_id\":\"usr_dfg_principal\",\"payout_pay_slot\":1,"
                + "\"effective_from\":\"" + tomorrow + "\",\"reason\":\"按工商登记持股比例\",\"platform_share_min_bps\":5000,\"platform_share_max_bps\":9000";
        createPlan(finance, "{\"idempotency_key\":\"dfg-bnd-1-" + s + "\"," + base
                + ",\"period_type\":\"monthly\",\"period_min_months\":3,\"period_max_months\":12}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("分红周期须在所配上下限之内"));
        createPlan(finance, "{\"idempotency_key\":\"dfg-bnd-2-" + s + "\"," + base + ",\"period_type\":\"semiannual\",\"period_min_months\":3}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("分红周期须同时填写上下限（月、季、半年、年）"));
        createPlan(finance, "{\"idempotency_key\":\"dfg-bnd-3-" + s + "\"," + base
                + ",\"period_type\":\"semiannual\",\"period_min_months\":12,\"period_max_months\":3}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dividend_plans WHERE merchant_id = ?", Integer.class, bar)).isZero();

        // 最高权限本人建季～年的半年方案，直接生效；库内落上下限。
        String planId = JsonPath.read(createPlan(master(), "{\"idempotency_key\":\"dfg-bnd-4-" + s + "\"," + base
                        + ",\"period_type\":\"semiannual\",\"period_min_months\":3,\"period_max_months\":12}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        assertThat(jdbc.queryForMap("SELECT period_type, period_min_months, period_max_months FROM dividend_plans WHERE id = ?", planId))
                .containsEntry("period_type", "semiannual").containsEntry("period_min_months", 3).containsEntry("period_max_months", 12);

        // 改方案只改周期：年在季～年之内 200（332-R1：半年改年度，生效日取 1 月 1 日），月不在 400；只改备注 200，新版本沿用季～年。
        mockMvc.perform(put("/api/v1/finance/dividends/plans/" + planId).with(master()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-bnd-5-" + s + "\",\"period_type\":\"annual\",\"effective_from\":\""
                                + LocalDate.of(tomorrow.plusDays(2).getYear() + 1, 1, 1) + "\",\"reason\":\"改为按年分红\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.period_type").value("annual"));
        mockMvc.perform(put("/api/v1/finance/dividends/plans/" + planId).with(master()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-bnd-6-" + s + "\",\"period_type\":\"monthly\",\"effective_from\":\""
                                + tomorrow.plusDays(2) + "\",\"reason\":\"改为按月分红\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("分红周期须在所配上下限之内"));
        String noted = JsonPath.read(mockMvc.perform(put("/api/v1/finance/dividends/plans/" + planId).with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-bnd-7-" + s + "\",\"note\":\"协议续签\",\"effective_from\":\""
                                + tomorrow.plusDays(2) + "\",\"reason\":\"补记协议\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        assertThat(jdbc.queryForMap("SELECT period_type, period_min_months, period_max_months, note FROM dividend_plans WHERE id = ?", noted))
                .containsEntry("period_type", "semiannual").containsEntry("period_min_months", 3).containsEntry("period_max_months", 12)
                .containsEntry("note", "协议续签");
    }

    /**
     * 第十八批 AX 段第 272 条 272-R1（BSM:1775「新酒吧可预填 80%／20%（业务参数，按经营主体可配，须同时配置上下限）建议值，但必须以获批登记版本生效」；
     * BSM:521「超出该区间的配置一律拒绝生效，未填写上下限的参数不得上线」）：平台（参股方）比例上下限随方案版本一起审批，建方案必填，下限不大于上限，平台比例
     * 须在上下限之内；改方案不填上下限沿用改前版本。按分红页方案弹窗的蛇形键走真实入口与授权链。修复前上下限无处落：缺上下限照样建成，第一个 400 断言红。
     */
    @Test
    void planPlatformShareMustFallWithinRequiredBounds() throws Exception {
        project();
        String bar = "dfg_shb_" + s;
        merchant(bar, "分红平台比例上下限酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff("usr_dfg_shb_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        LocalDate tomorrow = businessDays.merchantBusinessDate(null, bar, Instant.now()).plusDays(1);
        String base = "\"merchant_id\":\"" + bar + "\",\"principal_user_id\":\"usr_dfg_principal\",\"payout_pay_slot\":1,\"period_type\":\"monthly\","
                + "\"period_min_months\":1,\"period_max_months\":12,\"effective_from\":\"" + tomorrow + "\",\"reason\":\"按工商登记持股比例\","
                + "\"platform_share_bps\":8000";
        String message = "平台（参股方）比例须同时配置上下限，且比例须在上下限之内";
        createPlan(finance, "{\"idempotency_key\":\"dfg-shb-1-" + s + "\"," + base + ",\"platform_share_max_bps\":9000}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(message));
        createPlan(finance, "{\"idempotency_key\":\"dfg-shb-2-" + s + "\"," + base + ",\"platform_share_min_bps\":9000,\"platform_share_max_bps\":6000}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(message));
        createPlan(finance, "{\"idempotency_key\":\"dfg-shb-3-" + s + "\"," + base + ",\"platform_share_min_bps\":5000,\"platform_share_max_bps\":7000}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(message));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dividend_plans WHERE merchant_id = ?", Integer.class, bar)).isZero();

        // 最高权限本人建 60%～90% 的方案，直接生效、响应回显上下限；只改理由提交新版本，沿用改前上下限。
        String planId = JsonPath.read(createPlan(master(), "{\"idempotency_key\":\"dfg-shb-4-" + s + "\"," + base
                        + ",\"platform_share_min_bps\":6000,\"platform_share_max_bps\":9000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platform_share_min_bps").value(6000))
                .andExpect(jsonPath("$.data.platform_share_max_bps").value(9000))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        String next = JsonPath.read(mockMvc.perform(put("/api/v1/finance/dividends/plans/" + planId).with(master())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-shb-5-" + s + "\",\"effective_from\":\"" + tomorrow.plusDays(2)
                                + "\",\"reason\":\"补记协议\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        assertThat(jdbc.queryForMap("SELECT platform_share_bps, platform_share_min_bps, platform_share_max_bps FROM dividend_plans WHERE id = ?", next))
                .containsEntry("platform_share_bps", 8000).containsEntry("platform_share_min_bps", 6000)
                .containsEntry("platform_share_max_bps", 9000);
    }

    /**
     * 第十八批 AC 段第 210 条 210-R3（BSM:1774「……以及该期间的各项税费（含增值税、所得税及其他应由该主体承担的税费）；全部扣完、税后剩下的是本期经营口径
     * 净利润」；BSM:1801「连接税务处理，拿到这期的各项税费，算出净利润」；第十八批裁决 42「分红扣本期内已生效的申报金额合计；按季申报的税整季计入季末那个
     * 月」）：季度方案商户的经营主体经真实登记入口登记、财务、税务、法务三条线与最高确认逐级批准 2026-01 增值税与所属期 2026-01～2026-03 企业所得税，另有
     * 一笔未批准的 2026-02 附加税与一笔所属期在期外（2026-04）的已生效增值税；按前端蛇形键出 2026-Q1 正式单，只扣前两笔、逐税种落 tax_period 负额行，
     * 净利润＝收入−销货成本−费用−税费，库内 CHECK 通过。修复前不扣税费：period_tax_cent 断言红。
     */
    @Test
    void officialDividendDeductsApprovedEntityTaxesWhoseDeclaredPeriodEndsInThePeriod() throws Exception {
        String bar = taxBar("dfg_tax_" + s);
        approved(declare("vat", "2026-01", "2026-01", 12_345L));
        approved(declare("corporate_income", "2026-01", "2026-03", 50_000L));
        declare("surcharge", "2026-02", "2026-02", 7_000L);
        // 233-R5：登记增值税前该月月度分账单须已确认（TICKET_SETTLEMENT_PENDING），补上 4 月的。
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, DATE '2026-04-01', DATE '2026-04-30', 'confirmed', now(), now())", "sb_dfg_tax_4_" + s, project);
        approved(declare("vat", "2026-04", "2026-04", 9_000L));

        String statement = JsonPath.read(computePeriod(bar, "2026-Q1", "official", "dfg-tax-c-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andExpect(jsonPath("$.data.computed[0].period_tax_cent").value(62_345))
                .andReturn().getResponse().getContentAsString(), "$.data.computed[0].id");
        Map<String, Object> head = jdbc.queryForMap("SELECT revenue_cent, cost_cent, expense_cent, period_tax_cent, net_profit_cent"
                + " FROM dividend_statements WHERE id = ?", statement);
        assertThat(head).containsEntry("revenue_cent", 1_000_000L).containsEntry("cost_cent", 0L).containsEntry("expense_cent", 30_000L)
                .containsEntry("period_tax_cent", 62_345L).containsEntry("net_profit_cent", 1_000_000L - 30_000L - 62_345L);
        List<Map<String, Object>> taxLines = jdbc.queryForList("SELECT amount_cent, detail FROM dividend_statement_lines"
                + " WHERE statement_id = ? AND line_type = 'tax_period' ORDER BY seq", statement);
        assertThat(taxLines).extracting(row -> ((Number) row.get("amount_cent")).longValue()).containsExactly(-50_000L, -12_345L);
        assertThat((String) taxLines.get(0).get("detail")).contains("企业所得税", "实际申报金额", "2026-01～2026-03");
        assertThat((String) taxLines.get(1).get("detail")).contains("增值税", "实际申报金额", "2026-01").doesNotContain("2026-04");
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'net_profit'",
                Long.class, statement)).isEqualTo(907_655L);
    }

    /**
     * 第十八批 AR 段第 233 条 233-R3（BSM:1760「原期间已经关账、开票或申报的，只能在当前开放期间生成关联原期间的差异调整单」「所有税费一律计入税费成本」）与
     * 233-R1（BSM:1760「实缴与申报不一致的部分……不得直接混入税费成本」）：季度方案商户的经营主体 1 月增值税 5000 元、一季度企业所得税 500 元已生效，先出
     * 2026-Q1 正式单；财务按前端蛇形键登记实缴 4500 元（未缴 500 元）与 +300 元差异调整单，都经三线与最高确认逐级批准。原申报不变，原单与重出的 2026-Q1
     * 正式单税费仍是 5500 元；调整单计入批准当月，补齐该月所在季度的五类最终标记、月度分账与税费登记后出的正式单多一行增值税 tax_period −300 元，
     * 说明写明原所属期与计入月份。修复前没有登记入口：404，状态断言红。
     */
    @Test
    void periodDifferenceAdjustmentIsDeductedInApprovalMonthWhilePaymentStaysOutOfTaxCost() throws Exception {
        String bar = taxBar("dfg_adj_" + s);
        String review = declare("vat", "2026-01", "2026-01", 500_000L);
        approved(review);
        approved(declare("corporate_income", "2026-01", "2026-03", 50_000L));
        String declaration = jdbc.queryForObject("SELECT id FROM entity_tax_declarations WHERE review_id = ?", String.class, review);
        String q1 = JsonPath.read(computePeriod(bar, "2026-Q1", "official", "dfg-adj-c1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed[0].period_tax_cent").value(550_000))
                .andReturn().getResponse().getContentAsString(), "$.data.computed[0].id");

        approved(entityTax(declaration, "payments", "{\"idempotency_key\":\"dfg-pay-" + s + "\",\"paid_amount_cent\":450000,"
                + "\"difference_breakdown\":[{\"category\":\"unpaid\",\"amount_cent\":50000}],\"reason\":\"按完税凭证登记\","));
        approved(entityTax(declaration, "adjustments", "{\"idempotency_key\":\"dfg-adj-" + s + "\",\"amount_cent\":30000,\"reason\":\"更正申报表\","));
        String month = jdbc.queryForObject("SELECT accounting_month FROM entity_tax_adjustments WHERE declaration_id = ?"
                + " AND kind = 'period_difference'", String.class, declaration);

        assertThat(jdbc.queryForObject("SELECT declared_amount_cent FROM entity_tax_declarations WHERE id = ?", Long.class, declaration))
                .isEqualTo(500_000L);
        assertThat(jdbc.queryForObject("SELECT period_tax_cent FROM dividend_statements WHERE id = ?", Long.class, q1)).isEqualTo(550_000L);
        computePeriod(bar, "2026-Q1", "official", "dfg-adj-c2-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed[0].period_tax_cent").value(550_000));

        YearMonth quarterStart = YearMonth.parse(month).withMonth((YearMonth.parse(month).getMonthValue() - 1) / 3 * 3 + 1);
        EntityTaxFixture.taxDeclaredForAllMonths(jdbc, bar);
        markRangeFinal(jdbc, "bar", bar, quarterStart.atDay(1), quarterStart.plusMonths(2).atEndOfMonth());
        for (int i = 0; i < 3; i++) {
            YearMonth m = quarterStart.plusMonths(i);
            jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, 'confirmed', now(), now())", "sb_dfg_adj_" + i + "_" + s, project, m.atDay(1), m.atEndOfMonth());
        }
        String label = quarterStart.getYear() + "-Q" + ((quarterStart.getMonthValue() + 2) / 3);
        String current = JsonPath.read(computePeriod(bar, label, "official", "dfg-adj-c3-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andExpect(jsonPath("$.data.computed[0].period_tax_cent").value(30_000))
                .andReturn().getResponse().getContentAsString(), "$.data.computed[0].id");
        List<Map<String, Object>> taxLines = jdbc.queryForList("SELECT amount_cent, detail FROM dividend_statement_lines"
                + " WHERE statement_id = ? AND line_type = 'tax_period' ORDER BY seq", current);
        assertThat(taxLines).extracting(row -> ((Number) row.get("amount_cent")).longValue()).containsExactly(-30_000L);
        assertThat((String) taxLines.get(0).get("detail"))
                .isEqualTo("增值税实际申报金额（已生效登记，所属期 2026-01 更正（差异调整单，计入 " + month + "））");
    }

    /**
     * 第十八批裁决 42「登记齐了才能出单：正式单出单前本期每个月都要有已生效的税费登记，缺的不出单并写跳过原因」：本期 2026-Q1 只有 2026-01 增值税已生效、
     * 所属期 2026-01～2026-03 的企业所得税尚在审批中时，按前端蛇形键出正式单跳过并写明缺的月份、不出单；月度预览不判此闸照常出；企业所得税经逐级批准后
     * 正式单照常出。修复前不判税费登记：第一次正式单即出 1 张，computed 长度断言红。
     */
    @Test
    void officialDividendIsSkippedUntilEveryMonthIsCoveredByAnApprovedTaxDeclaration() throws Exception {
        String bar = taxBar("dfg_taxg_" + s);
        approved(declare("vat", "2026-01", "2026-01", 12_345L));
        String pending = declare("corporate_income", "2026-01", "2026-03", 50_000L);

        computePeriod(bar, "2026-Q1", "official", "dfg-taxg-1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].merchant_id").value(bar))
                .andExpect(jsonPath("$.data.skipped[0].reason")
                        .value("本期 2026-02、2026-03 尚无已生效的税费申报登记，请先登记并审批通过后再出正式单"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dividend_statements WHERE merchant_id = ? AND kind = 'official'",
                Integer.class, bar)).isZero();
        computePeriod(bar, "2026-02", "preview", "dfg-taxg-p-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andExpect(jsonPath("$.data.computed[0].status").value("preview"));

        approved(pending);
        computePeriod(bar, "2026-Q1", "official", "dfg-taxg-2-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andExpect(jsonPath("$.data.computed[0].status").value("draft"))
                .andExpect(jsonPath("$.data.computed[0].period_tax_cent").value(62_345));
    }

    /**
     * 第十八批 AC 段第 211 条 211-R1（BSM:1777「若因特殊情况需要整期暂不分红或暂缓分红的，须由主理人与娱乐平台公司协商一致后执行，并记录原因与协商结果」；
     * 第十八批裁决 43「该期正式单已提交审批或已发放的不能再登记，草稿随登记作废；……「暂缓分红」等协商恢复后由财务登记恢复，再照常出单」）：月度方案先出
     * 2026-08 正式草稿，财务按前端蛇形键登记暂缓分红后草稿作废，再出正式单跳过并写明暂缓分红、原因与协商结果、不出新单；登记恢复后照常出单；新单登记依法
     * 可分配金额并提交审批后，该期再登记返回 409。修复前没有登记接口：登记请求 200 断言红。
     */
    @Test
    void deferHoldVoidsDraftSkipsOfficialUntilLiftedAndIsRefusedOnceSubmitted() throws Exception {
        project();
        String bar = "dfg_hold_" + s;
        barWithMonthlyPlan(bar, "分红暂缓酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_31);
        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff("usr_dfg_hold_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        String draft = JsonPath.read(compute(finance, bar, "dfg-hold-c1-" + s)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.computed[0].id");

        String holdId = JsonPath.read(hold(finance, bar, "2026-08", "defer", "dfg-hold-r1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hold_type").value("defer"))
                .andExpect(jsonPath("$.data.negotiation_result").value("双方同意待十月复业后再分"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT status FROM dividend_statements WHERE id = ?", String.class, draft)).isEqualTo("voided");
        compute(finance, bar, "dfg-hold-c2-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].merchant_id").value(bar))
                .andExpect(jsonPath("$.data.skipped[0].reason")
                        .value("本期经协商暂缓分红，不出正式单（原因：主理人装修停业；协商结果：双方同意待十月复业后再分）"));
        assertThat(officialStatements(bar)).isZero();

        mockMvc.perform(post("/api/v1/finance/dividends/holds/" + holdId + "/lift").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-hold-l-" + s + "\",\"lift_result\":\"复业后双方同意恢复分红\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lift_result").value("复业后双方同意恢复分红"));
        String again = JsonPath.read(compute(finance, bar, "dfg-hold-c3-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andReturn().getResponse().getContentAsString(), "$.data.computed[0].id");
        String confirmed = JsonPath.read(mockMvc.perform(post("/api/v1/finance/dividends/statements/" + again + "/legal-confirmation")
                        .with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-hold-legal-" + s + "\",\"legal_distributable_cent\":100000,"
                                + "\"tax_accounting_ref\":\"税务核算" + s + "\",\"resolution_ref\":\"分配决议" + s + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        submit(finance, confirmed, "dfg-hold-s-" + s).andExpect(status().isOk());

        hold(finance, bar, "2026-08", "skip", "dfg-hold-r2-" + s)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("该期正式单已提交审批或已发放（当前状态：待审批），不能再登记暂不分红或暂缓分红"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dividend_period_holds WHERE merchant_id = ? AND lifted_at IS NULL",
                Integer.class, bar)).isZero();
    }

    /**
     * 211-R1（第十八批裁决 43「「暂不分红」该期不出正式单、利润不补分；……月度预览照常」）：季度方案按前端蛇形键登记暂不分红，期间标签须与方案周期一致
     * （填 2026-08 返回 400）；登记 2026-Q3 后出该季正式单跳过并写明暂不分红，同季 2026-08 月度预览照常生成；暂不分红不能登记恢复（409）。
     * 修复前没有登记接口：400 断言红。
     */
    @Test
    void skipHoldBlocksOfficialKeepsMonthlyPreviewAndCannotBeLifted() throws Exception {
        project();
        String bar = "dfg_skip_" + s;
        merchant(bar, "分红暂不分酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status,"
                + " payout_pay_slot, effective_from) VALUES (?, ?, 'usr_dfg_principal', 'quarterly', 'bar', TRUE, 'active', 1, DATE '2000-01-01')", "divp_" + bar, bar);
        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_31);
        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff("usr_dfg_skip_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        hold(finance, bar, "2026-08", "skip", "dfg-skip-r0-" + s)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("季度标签")));
        String holdId = JsonPath.read(hold(finance, bar, "2026-Q3", "skip", "dfg-skip-r1-" + s)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");

        computeAs(finance, bar, "2026-Q3", "official", "dfg-skip-c-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].reason")
                        .value("本期经协商暂不分红，不出正式单（原因：主理人装修停业；协商结果：双方同意待十月复业后再分）"));
        computeAs(finance, bar, "2026-08", "preview", "dfg-skip-p-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andExpect(jsonPath("$.data.computed[0].status").value("preview"));
        mockMvc.perform(post("/api/v1/finance/dividends/holds/" + holdId + "/lift").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-skip-l-" + s + "\",\"lift_result\":\"双方同意恢复\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("暂不分红")));
    }

    /**
     * 第十八批 AQ 段第 243 条 243-R1（ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；BSM:1787「酒吧关闭须完成关停结算，
     * 依法核清本次分红」）与 18AQ 控制席判定（有关停登记的商户出单取方案按 min（期末日，关店最后营业日）已生效的版本，关店后的期间不再分红）：关店所在期
     * 正式单只查期初至关店最后营业日的五类标记，酒吧停用后照查。甲店 8 月只漏标 08-10，登记关停结算（关店最后营业日 08-20）后停用，出 8 月正式单跳过并列出
     * 08-10，标齐后能出；乙店仍在营、只标到 08-20，另有 08-25 起停用的方案版本，登记关停结算后出 8 月正式单按 08-20 已生效的版本出，9 月不再出。
     * 修复前停用后不查（甲店第一次即出单）、在营查到期末（乙店被 08-21～08-31 拦）、按期末日取到停用版本 404：断言红。
     */
    @Test
    void closedBarOfficialDividendChecksFinalMarksUpToClosingDayEvenAfterDeactivation() throws Exception {
        project();
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_dfg_adminc_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        String closed = "dfg_cls_" + s;
        String closedName = "分红关店酒吧" + s;
        barWithMonthlyPlan(closed, closedName, Instant.parse("2026-04-30T16:00:00Z"));
        markRangeFinal(jdbc, "bar", closed, AUG_1, LocalDate.of(2026, 8, 9));
        markRangeFinal(jdbc, "bar", closed, LocalDate.of(2026, 8, 11), AUG_20);
        closeout(admin, closed, "dfg-co-1-" + s);
        jdbc.update("UPDATE merchants SET status = 'inactive' WHERE id = ?", closed);
        compute(admin, closed, "dfg-co-c1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].reason").value(containsString("2026-08-10 " + closedName + "：")))
                .andExpect(jsonPath("$.data.skipped[0].reason").value(not(containsString("2026-08-21"))));
        markRangeFinal(jdbc, "bar", closed, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10));
        compute(admin, closed, "dfg-co-c2-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1));

        String open = "dfg_clo_" + s;
        barWithMonthlyPlan(open, "分红关店在营酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        markRangeFinal(jdbc, "bar", open, AUG_1, AUG_20);
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status,"
                + " payout_pay_slot, effective_from, stop_requested) VALUES (?, ?, 'usr_dfg_principal', 'monthly', 'bar', TRUE, 'ended', 1,"
                + " DATE '2026-08-25', TRUE)", "divp_stop_" + open, open);
        closeout(admin, open, "dfg-co-2-" + s);
        compute(admin, open, "dfg-co-c3-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andExpect(jsonPath("$.data.computed[0].plan_id").value("divp_" + open));
        computeAs(admin, open, "2026-09", "official", "dfg-co-c4-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].reason").value("该酒吧已于 2026-08-20 关店（已登记关停结算），关店后的期间不再分红"));
    }

    /**
     * 第十八批 AX 段第 333 条 333-R1（ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；BSM:1777「数据未全部标记最终之前不得
     * 生成预览」）：未登记关停结算就停用或存档的酒吧，五类最终闸照查到期末。甲店月度方案、8 月只漏标 08-10，停用后出 8 月正式单跳过并列出 08-10；乙店同样
     * 漏标、只存档，同样跳过；补标甲店 08-10 后能出；丙店季度方案、漏标 08-31、停用后出 8 月预览跳过。修复前停用、存档的酒吧不在检查单位里、直接出单：断言红。
     */
    @Test
    void deactivatedOrArchivedBarWithoutCloseoutStillChecksFinalMarks() throws Exception {
        project();
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_dfg_admind_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        String inactive = "dfg_ina_" + s;
        String inactiveName = "分红停用酒吧" + s;
        barWithMonthlyPlan(inactive, inactiveName, Instant.parse("2026-04-30T16:00:00Z"));
        markRangeFinal(jdbc, "bar", inactive, AUG_1, LocalDate.of(2026, 8, 9));
        markRangeFinal(jdbc, "bar", inactive, LocalDate.of(2026, 8, 11), AUG_31);
        jdbc.update("UPDATE merchants SET status = 'inactive' WHERE id = ?", inactive);
        compute(admin, inactive, "dfg-ina-c1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].reason").value(containsString("2026-08-10 " + inactiveName + "：")));

        String archived = "dfg_arc_" + s;
        String archivedName = "分红存档酒吧" + s;
        barWithMonthlyPlan(archived, archivedName, Instant.parse("2026-04-30T16:00:00Z"));
        markRangeFinal(jdbc, "bar", archived, AUG_1, LocalDate.of(2026, 8, 9));
        markRangeFinal(jdbc, "bar", archived, LocalDate.of(2026, 8, 11), AUG_31);
        jdbc.update("UPDATE merchants SET archived_at = now() WHERE id = ?", archived);
        compute(admin, archived, "dfg-arc-c1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].reason").value(containsString("2026-08-10 " + archivedName + "：")));

        markRangeFinal(jdbc, "bar", inactive, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10));
        compute(admin, inactive, "dfg-ina-c2-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1));

        String quarterly = "dfg_inq_" + s;
        merchant(quarterly, "分红停用季度酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status,"
                + " payout_pay_slot, effective_from) VALUES (?, ?, 'usr_dfg_principal', 'quarterly', 'bar', TRUE, 'active', 1, DATE '2000-01-01')",
                "divp_" + quarterly, quarterly);
        markRangeFinal(jdbc, "bar", quarterly, AUG_1, LocalDate.of(2026, 8, 30));
        jdbc.update("UPDATE merchants SET status = 'inactive' WHERE id = ?", quarterly);
        computeAs(admin, quarterly, "2026-08", "preview", "dfg-inq-p-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].reason").value(containsString("不得生成预览")));
    }

    /**
     * 第十八批 AX 段第 333 条 333-R1（ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；BSM:1777「数据未全部标记最终之前不得
     * 生成预览」；BSM:1787「酒吧关闭须完成关停结算」）：已登记关停结算、关店最后营业日落在当月的酒吧，月度预览只查关店最后营业日，不查关店后不会再标的期末日。
     * 丁店季度方案、只标 08-01～08-19、漏标 08-20，登记关停结算（关店最后营业日 08-20）后出 8 月预览跳过，原因写 08-20、不写 08-31；补标 08-20 后能出。
     * 预览改回查期末日（直接放行或原因写 08-31）：断言红。
     */
    @Test
    void closedBarMonthlyPreviewChecksClosingDayInsteadOfPeriodEnd() throws Exception {
        project();
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_dfg_admine_" + s, "platform_admin", project, null,
                "finance.dividend.manage");
        String closed = "dfg_clq_" + s;
        merchant(closed, "分红关店季度酒吧" + s, Instant.parse("2026-04-30T16:00:00Z"));
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status,"
                + " payout_pay_slot, effective_from) VALUES (?, ?, 'usr_dfg_principal', 'quarterly', 'bar', TRUE, 'active', 1, DATE '2000-01-01')",
                "divp_" + closed, closed);
        markRangeFinal(jdbc, "bar", closed, AUG_1, LocalDate.of(2026, 8, 19));
        closeout(admin, closed, "dfg-clq-co-" + s);
        computeAs(admin, closed, "2026-08", "preview", "dfg-clq-p1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].reason").value(containsString("该月最后一个营业日（2026-08-20）")))
                .andExpect(jsonPath("$.data.skipped[0].reason").value(not(containsString("2026-08-31"))));
        markRangeFinal(jdbc, "bar", closed, AUG_20, AUG_20);
        computeAs(admin, closed, "2026-08", "preview", "dfg-clq-p2-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1));
    }

    // ---------- helpers ----------

    /**
     * 210-R3：经营主体为 E 的酒吧（2026-01-01 前建档）与其季度、只计门票收入的已批准分红方案版本（234-R1 起直接落库）；2026-Q1 逐日五类已标最终，
     * 1～3 月月度分账单已确认、1 月该店门票直付额 10000 元，2 月费用台账其他费用 300 元；税费登记人、财务、税务、法务三条线起点与审批人就位。
     */
    private String taxBar(String bar) throws Exception {
        project();
        merchant(bar, "分红扣税酒吧" + s, Instant.parse("2025-12-31T16:00:00Z"));
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'management_company', 'active', now(), now())", entity, "分红扣税酒吧公司" + s);
        jdbc.update("UPDATE merchants SET business_entity_id = ? WHERE id = ?", entity, bar);
        markRangeFinal(jdbc, "bar", bar, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        for (int month = 1; month <= 3; month++) {
            LocalDate start = LocalDate.of(2026, month, 1);
            jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, 'confirmed', now(), now())", "sb_dfg_tax_" + month + "_" + s, project, start, start.plusMonths(1).minusDays(1));
        }
        jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent,"
                + " status) VALUES (?, ?, ?, 'exclusive_full', 1000000, 1000000, 'draft')", "si_dfg_tax_" + s, "sb_dfg_tax_1_" + s, bar);
        jdbc.update("INSERT INTO merchant_expense_entries (id, merchant_id, period, expense_type, amount_cent, source, source_ref, note,"
                + " operator_user_id, created_at) VALUES (?, ?, '2026-02', 'other', 30000, 'manual', NULL, '分红扣税测试其他费用', 'usr_dfg_tax', now())",
                "mex_dfg_tax_" + s, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        taxAdmin = staffs.staff("usr_dfg_taxa_" + s, "platform_admin", project, null, "finance.dividend.manage");
        taxFinance = staffs.staff("usr_dfg_taxf_" + s, "staff", project, null, "finance.entity_tax.manage", "finance.entity_tax.view");
        financeReviewer = staffs.staff(financeApprover, "staff", project, null, "governance.supervisor_review.approve");
        taxReviewer = staffs.staff(taxApprover, "staff", project, null, "governance.supervisor_review.approve");
        legalReviewer = staffs.staff(legalApprover, "staff", project, null, "governance.supervisor_review.approve");
        for (String[] line : List.of(new String[]{"finance_review", financeApprover}, new String[]{"tax_review", taxApprover},
                new String[]{"legal_review", legalApprover})) {
            jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,entry_user_id,status) VALUES(?,?,1,?,'active')"
                    + " ON CONFLICT(line_code) DO UPDATE SET entry_user_id=EXCLUDED.entry_user_id,status='active'", line[0], line[0], line[1]);
        }
        // 234-R1：出历史期间单用直接落的已批准版本（自 2000-01-01 起）。
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status,"
                + " payout_pay_slot, effective_from) VALUES (?, ?, 'usr_dfg_principal', 'quarterly', 'ticket', TRUE, 'active', 1, DATE '2000-01-01')",
                "divp_" + bar, bar);
        return bar;
    }

    /** 210-R2 税费申报登记页「登记并提交审批」的真实请求形状（蛇形键）；返回复核单号。 */
    private String declare(String taxType, String start, String end, long cent) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/finance/entity-taxes").with(taxFinance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dfg-etd-" + taxType + start + end + "-" + s + "\",\"business_entity_id\":\"" + entity
                                + "\",\"tax_type\":\"" + taxType + "\",\"period_start_month\":\"" + start + "\",\"period_end_month\":\"" + end
                                + "\",\"declared_amount_cent\":" + cent + ",\"reason\":\"按税务申报表登记\",\"evidence_attachment_ids\":[],"
                                + "\"parallel_lines\":[{\"line_code\":\"finance_review\",\"final_approver_id\":\"" + financeApprover + "\"},"
                                + "{\"line_code\":\"tax_review\",\"final_approver_id\":\"" + taxApprover + "\"},"
                                + "{\"line_code\":\"legal_review\",\"final_approver_id\":\"" + legalApprover + "\"}]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.review_id");
    }

    /** 233-R1、233-R3 已生效行弹窗的真实请求形状（蛇形键；head 为本操作字段，这里补证据与三条线）；返回复核单号。 */
    private String entityTax(String declaration, String action, String head) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/finance/entity-taxes/" + declaration + "/" + action).with(taxFinance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(head + "\"evidence_attachment_ids\":[],\"parallel_lines\":[{\"line_code\":\"finance_review\",\"final_approver_id\":\""
                                + financeApprover + "\"},{\"line_code\":\"tax_review\",\"final_approver_id\":\"" + taxApprover + "\"},"
                                + "{\"line_code\":\"legal_review\",\"final_approver_id\":\"" + legalApprover + "\"}]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.review_id");
    }

    /** 财务、税务、法务三条线的审批人与最高确认各用自己的会话，经真实审批接口逐级批准。 */
    private void approved(String review) throws Exception {
        for (RequestPostProcessor reviewer : List.of(financeReviewer, taxReviewer, legalReviewer, master())) {
            mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(reviewer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"approved\",\"comment\":\"已核对申报表\",\"idempotency_key\":\"dfg-ap-" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isOk());
        }
    }

    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 分红计算页期间作业的真实请求形状（蛇形键）。 */
    private ResultActions computePeriod(String bar, String label, String kind, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(taxAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"period_label\":\"" + label + "\",\"kind\":\"" + kind + "\","
                        + "\"merchant_id\":\"" + bar + "\"}"));
    }

    private void project() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','分红闸综合体','active',TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", project);
    }

    /** 酒吧与其月度、只计酒吧收入的生效分红方案（避开票务分账前置；扣销货成本与全部费用，发放付账日取第一个）；裁决 42 出单闸前提：经营主体各月已有生效税费登记。 */
    private void barWithMonthlyPlan(String id, String name, Instant created) {
        merchant(id, name, created);
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status,"
                + " payout_pay_slot, effective_from) VALUES (?, ?, 'usr_dfg_principal', 'monthly', 'bar', TRUE, 'active', 1, DATE '2000-01-01')", "divp_" + id, id);
        EntityTaxFixture.taxDeclaredForAllMonths(jdbc, id);
    }

    private void merchant(String id, String name, Instant created) {
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,?,now())", id, name, project, Timestamp.from(created));
    }

    /** 2026-08 已确认月度分账单里该商户的单店门票直付额（FS-015 门票收入取数口径）。 */
    private void confirmedTicketRevenue(String merchantId, long cent) {
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 'confirmed', now(), now())", "sb_dfg_" + s, project, AUG_1, AUG_31);
        jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent,"
                + " status) VALUES (?, ?, ?, 'exclusive_full', ?, ?, 'draft')", "si_dfg_" + s, "sb_dfg_" + s, merchantId, cent, cent);
    }

    /** 分红页方案弹窗「确认创建」的真实请求形状（蛇形键）。 */
    private ResultActions createPlan(RequestPostProcessor who, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/plans").with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 当天或之后第一个「第二个付账日」（取当时已审批生效的三个付账日配置）。 */
    private LocalDate secondPayDayOnOrAfter(LocalDate from) {
        int day = jdbc.queryForObject("SELECT day_2 FROM internal_pay_day_rules WHERE kind = 'pay_days' AND status = 'approved'"
                + " AND effective_from <= ? ORDER BY effective_from DESC, decided_at DESC LIMIT 1", Integer.class, from);
        return from.getDayOfMonth() <= day ? from.withDayOfMonth(day) : from.plusMonths(1).withDayOfMonth(day);
    }

    private LocalDate bankDayOnOrAfter(LocalDate from) {
        return jdbc.queryForObject("SELECT MIN(calendar_date) FROM ticket_points_calendar_days WHERE calendar_date >= ? AND bank_working_day",
                LocalDate.class, from);
    }

    private LocalDate dueDate(String sourceType, String statementId) {
        return jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE source_type = ? AND source_id = ?", LocalDate.class,
                sourceType, statementId);
    }

    /** 211-R1 分红页「暂不分红／暂缓分红登记」弹窗的真实请求形状（蛇形键）。 */
    private ResultActions hold(RequestPostProcessor who, String bar, String label, String type, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/holds").with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"merchant_id\":\"" + bar + "\",\"period_label\":\"" + label
                        + "\",\"hold_type\":\"" + type + "\",\"reason\":\"主理人装修停业\",\"negotiation_result\":\"双方同意待十月复业后再分\"}"));
    }

    /** 分红计算页期间作业的真实请求形状（蛇形键），按指定账号、期间与类别。 */
    private ResultActions computeAs(RequestPostProcessor who, String bar, String label, String kind, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"period_label\":\"" + label + "\",\"kind\":\"" + kind + "\","
                        + "\"merchant_id\":\"" + bar + "\"}"));
    }

    /** 243-R1 持股分红页「关停结算登记」的真实请求形状（蛇形键），关店最后营业日 2026-08-20。 */
    private void closeout(RequestPostProcessor who, String bar, String key) throws Exception {
        mockMvc.perform(post("/api/v1/finance/dividends/closeouts").with(who).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key + "\",\"merchant_id\":\"" + bar + "\",\"closed_business_date\":\"2026-08-20\","
                                + "\"reason\":\"租约到期退场关店\"}"))
                .andExpect(status().isOk());
    }

    /** 分红计算页「正式单」的真实请求形状（蛇形键）。 */
    private ResultActions compute(RequestPostProcessor who, String bar, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"period_label\":\"2026-08\",\"kind\":\"official\","
                        + "\"merchant_id\":\"" + bar + "\"}"));
    }

    /** 分红计算页「提交审阅」的真实请求形状（蛇形键）。 */
    private ResultActions submit(RequestPostProcessor who, String statementId, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/" + statementId + "/submit").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\"}"));
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
