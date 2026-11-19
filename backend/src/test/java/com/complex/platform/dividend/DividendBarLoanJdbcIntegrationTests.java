package com.complex.platform.dividend;

// 文件版本：V0.02

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.support.EntityTaxFixture;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.PaymentConfirmationFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AX 段第 273 条 273-R1、273-R2 酒吧借款登记与分红前置借款清偿真库回归。依据：PGM:436「任何公司向酒吧提供的借款，出借时须写明金额、利率、还款期限
 * 与还款顺序；酒吧当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红，存在逾期未还借款的一律不得分红，分红方案提交时须附借款清偿情况」；
 * BSM:1785「存在逾期未还借款的，分红一律优先偿还借款」；第十八批裁决 55「所有应付都先走付款确认」、65「按台账来源逐个分」、66「每类必带财务、法务两线」。
 *
 * <p>夹具随测试事务回滚：酒吧 2026-08 五类全标、经营主体各月有已生效税费登记、已确认分账单里单店门票直付 50,000 元（本期可分配 50,000 元）；月度方案平台 80%、
 * 个人主理人 20%，今天前后为已审批的工作日与银行工作日（分红应付到期日按付账日取）。出借方为平台内主体「娱乐平台公司」。付款确认发起人沿直接上级到主线最终审批人，财务线、法务线各一名最终审批人。请求按持股分红页、应收应付台账页与
 * 主管复核页的蛇形键走真实入口与授权链。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DividendBarLoanJdbcIntegrationTests {

    private static final String LOANS = "/api/v1/finance/dividends/loans";
    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_31 = LocalDate.of(2026, 8, 31);
    private static final long D = 5_000_000L;
    private static final long DUE = 1_030_000L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "dbl_p_" + s;
    private final String bar = "dbl_bar_" + s;
    private final String shop = "dbl_shop_" + s;
    private final String lender = "dbl_ent_" + s;
    private final String mainId = "usr_dbl_s1_" + s;
    private final String finLineId = "usr_dbl_fl_" + s;
    private final String legalLineId = "usr_dbl_ll_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor approver;
    private RequestPostProcessor payer;
    private RequestPostProcessor main;
    private RequestPostProcessor finLine;
    private RequestPostProcessor legalLine;
    private LocalDate today;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','酒吧借款综合体','active',TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,?,now())", bar, "酒吧借款酒吧" + s, project, Timestamp.from(Instant.parse("2026-04-30T16:00:00Z")));
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'retail','active',?,now(),now())", shop, "酒吧借款零售店" + s, project);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, '娱乐平台公司', 'platform', 'active', now(), now())", lender);
        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_31);
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 'confirmed', now(), now())", "sb_dbl_" + s, project, AUG_1, AUG_31);
        jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent,"
                + " status) VALUES (?, ?, ?, 'exclusive_full', ?, ?, 'draft')", "si_dbl_" + s, "sb_dbl_" + s, bar, D, D);
        EntityTaxFixture.taxDeclaredForAllMonths(jdbc, bar);
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, platform_share_bps, period_type, revenue_domains,"
                + " include_inventory_cost, status, payout_pay_slot, effective_from, reason, period_min_months, period_max_months, principal_type)"
                + " VALUES (?, ?, ?, 8000, 'monthly', 'ticket', TRUE, 'active', 1, DATE '2000-01-01', '工商登记持股比例', 1, 12, 'individual')",
                "divp_dbl_" + s, bar, "usr_dbl_principal_" + s);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff("usr_dbl_fin_" + s, "platform_admin", project, null, "finance.dividend.manage", "finance.dividend.view");
        approver = master(); // 第十八批 BA 段第 314 条 314-R1：分红计算单批准只认最高权限本人
        String payerId = "usr_dbl_pay_" + s;
        payer = staffs.staff(payerId, "staff", project, null, "finance.payment.register", "finance.ar_ap.view");
        main = staffs.staff(mainId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        finLine = staffs.staff(finLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        legalLine = staffs.staff(legalLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", payerId, mainId);
        today = businessDays.merchantBusinessDate(null, bar, Instant.now());
    }

    /**
     * ①③ 登记借款 200：同事务挂一笔酒吧应付（借款还款，往来方为出借方主体，金额＝本金＋约定利息，到期日＝还款期限，归该酒吧与所属综合体，待付）；
     * 非酒吧商户 400；该应付按台账来源发起付款确认，支付类型 bar_loan「借款还款」。修复前没有借款登记接口：404，断言红。
     */
    @Test
    void registeredLoanPostsBarPayableThatGoesThroughPaymentConfirmation() throws Exception {
        String body = register("1a", bar, 1, 1_000_000L, 30_000L, today.minusDays(30), today.plusDays(30))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value(bar))
                .andExpect(jsonPath("$.data.lender_name").value("娱乐平台公司"))
                .andExpect(jsonPath("$.data.annual_rate_bps").value(600))
                .andExpect(jsonPath("$.data.outstanding_cent").value(DUE))
                .andExpect(jsonPath("$.data.status").value("not_due"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String loan = JsonPath.read(body, "$.data.id");
        String item = JsonPath.read(body, "$.data.ar_ap_item_id");
        assertThat(jdbc.queryForMap("SELECT direction, source_type, source_id, counterparty_id, amount_cent, unpaid_amount_cent, due_date::text AS due,"
                        + " merchant_id, project_id, status FROM ar_ap_items WHERE id = ?", item))
                .containsEntry("direction", "payable").containsEntry("source_type", "bar_loan").containsEntry("source_id", loan)
                .containsEntry("counterparty_id", lender).containsEntry("amount_cent", DUE).containsEntry("unpaid_amount_cent", DUE)
                .containsEntry("due", today.plusDays(30).toString()).containsEntry("merchant_id", bar).containsEntry("project_id", project)
                .containsEntry("status", "pending_payment");

        register("1b", shop, 1, 1_000_000L, 30_000L, today.minusDays(30), today.plusDays(30))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        confirm("1c", item).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_type").value("bar_loan"))
                .andExpect(jsonPath("$.data.payment_type_label").value("借款还款"))
                .andExpect(jsonPath("$.data.amount_cent").value(DUE));
    }

    /** ② 列表按该酒吧当前营业日判状态：还款期限在今天之后＝未到期，今天＝已到期未还，早于今天＝逾期未还，台账付清＝已还清。 */
    @Test
    void listedLoansShowStatusByDueDateAgainstBarBusinessDay() throws Exception {
        register("2a", bar, 1, 1_000_000L, 30_000L, today.minusDays(60), today.plusDays(1)).andExpect(status().isOk());
        register("2b", bar, 2, 1_000_000L, 30_000L, today.minusDays(60), today).andExpect(status().isOk());
        register("2c", bar, 3, 1_000_000L, 30_000L, today.minusDays(60), today.minusDays(1)).andExpect(status().isOk());
        String repaid = itemOf(register("2d", bar, 4, 1_000_000L, 30_000L, today.minusDays(60), today.minusDays(1)));
        PaymentConfirmationFixture.approvedArAp(jdbc, repaid);
        pay(repaid, "2e", DUE).andExpect(status().isOk());

        String body = mockMvc.perform(get(LOANS).param("merchant_id", bar).with(finance))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(body, "$.data[*].status")).containsExactly("not_due", "due", "overdue", "repaid");
        assertThat(JsonPath.<List<Integer>>read(body, "$.data[*].outstanding_cent")).containsExactly((int) DUE, (int) DUE, (int) DUE, 0);
    }

    /**
     * ④⑥ 出单先偿到期未还借款本息：今天到期的 10,300 元先从可分配额 50,000 元里扣，余 39,700 元再按 80%／20% 劈分，明细一行借款清偿；还款期限未到的借款不扣；
     * 登记依法可分配金额后提交不被拦；依法可分配金额明细行「本次分配取二者较小」写经营口径与核定额的较小值 50,000 元，不写偿后余额 39,700 元（借款清偿由下一行
     * 单列，BSM:1785 先取依法上限、再先偿借款）。修复前出单不扣借款：单上没有 loan_repayment_cent，断言红；明细行写偿后余额：断言红。
     */
    @Test
    void officialStatementRepaysDueLoanBeforeSplitAndIgnoresLoanNotYetDue() throws Exception {
        register("4a", bar, 1, 1_000_000L, 30_000L, today.minusDays(60), today).andExpect(status().isOk());
        register("4b", bar, 2, 1_000_000L, 30_000L, today.minusDays(60), today.plusDays(10)).andExpect(status().isOk());

        String statement = idOf(compute("4c")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed[0].loan_repayment_cent").value(DUE))
                .andExpect(jsonPath("$.data.computed[0].distributable_cent").value(D - DUE))
                .andExpect(jsonPath("$.data.computed[0].platform_share_cent").value(MoneyRounding.bpsOf(D - DUE, 8000))));
        assertThat(jdbc.queryForList("SELECT amount_cent FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'loan_repayment'",
                Long.class, statement)).containsExactly(-DUE);
        assertThat(jdbc.queryForObject("SELECT detail FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'loan_repayment'",
                String.class, statement)).contains("借款清偿（分红前置）").contains("娱乐平台公司").contains(today.toString());

        String confirmed = legal(statement, "4d");
        assertThat(jdbc.queryForObject("SELECT detail FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'legal_cap'",
                String.class, confirmed)).contains("经营口径可分配额 " + D + " 分，本次分配取二者较小 " + D + " 分").doesNotContain(String.valueOf(D - DUE));
        submit(finance, confirmed, "4e").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_review"));
    }

    /**
     * ⑤ 逾期未还即拦：逾期借款照样先偿进单，提交 409 DIVIDEND_LOAN_OVERDUE；该借款应付经付款确认（主线、财务线、法务线）与登记付款还清后提交 200；
     * 审批前又登记一笔逾期未还借款，审批 409；还清后审批 200，单上借款清偿额不变。修复前提交不判借款：提交 200，断言红。
     */
    @Test
    void overdueLoanBlocksSubmitAndApproveUntilRepaid() throws Exception {
        String item = itemOf(register("5a", bar, 1, 1_000_000L, 30_000L, today.minusDays(60), today.minusDays(1)));
        String statement = legal(idOf(compute("5b").andExpect(status().isOk())), "5c");

        submit(finance, statement, "5d").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_LOAN_OVERDUE"));
        String review = JsonPath.read(confirm("5e", item).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
        for (RequestPostProcessor who : List.of(main, finLine, legalLine)) {
            mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(who).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idempotency_key\":\"dbl-apv-" + UUID.randomUUID() + "\",\"decision\":\"approved\",\"comment\":\"已核对借款协议\"}"))
                    .andExpect(status().isOk());
        }
        pay(item, "5f", DUE).andExpect(status().isOk());
        submit(finance, statement, "5g").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_review"));

        String late = itemOf(register("5h", bar, 2, 1_000_000L, 30_000L, today.minusDays(60), today.minusDays(2)));
        action(approver, statement, "approve", "5i").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_LOAN_OVERDUE"));
        PaymentConfirmationFixture.approvedArAp(jdbc, late);
        pay(late, "5j", DUE).andExpect(status().isOk());
        action(approver, statement, "approve", "5k").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.loan_repayment_cent").value(DUE));
    }

    /** ⑦ 到期本息超过可分配额：只偿到可分配额为止，可分配额与实发都为 0。 */
    @Test
    void loanRepaymentIsCappedAtDistributable() throws Exception {
        register("7a", bar, 1, 6_000_000L, 0L, today.minusDays(60), today).andExpect(status().isOk());

        compute("7b").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed[0].loan_repayment_cent").value(D))
                .andExpect(jsonPath("$.data.computed[0].distributable_cent").value(0))
                .andExpect(jsonPath("$.data.computed[0].payout_cent").value(0));
    }

    // ---------- helpers ----------

    /** 持股分红页「酒吧借款」面板「登记借款」的真实请求形状（蛇形键）。 */
    private ResultActions register(String tag, String merchant, int order, long principal, long interest, LocalDate lentOn, LocalDate due)
            throws Exception {
        return mockMvc.perform(post(LOANS).with(finance).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"dbl-r-" + tag + "-" + s + "\",\"merchant_id\":\"" + merchant + "\",\"lender_entity_id\":\"" + lender
                        + "\",\"lender_name\":\"娱乐平台公司\",\"principal_cent\":" + principal + ",\"annual_rate_bps\":600,\"interest_cent\":" + interest
                        + ",\"lent_on\":\"" + lentOn + "\",\"due_date\":\"" + due + "\",\"repay_order\":" + order + ",\"note\":\"借款协议 JK-" + tag + "\"}"));
    }

    private static String itemOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data.ar_ap_item_id");
    }

    /** 分红页期间作业「正式单」的真实请求形状（蛇形键）。 */
    private ResultActions compute(String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"dbl-c-" + tag + "-" + s + "\",\"period_label\":\"2026-08\",\"kind\":\"official\","
                        + "\"merchant_id\":\"" + bar + "\"}"));
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.computed[0].id");
    }

    /** 财务登记依法可分配金额（按核定额重算出新草稿），返回新单号。 */
    private String legal(String statement, String tag) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/finance/dividends/statements/" + statement + "/legal-confirmation").with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dbl-l-" + tag + "-" + s + "\",\"legal_distributable_cent\":" + D
                                + ",\"tax_accounting_ref\":\"财税核算报告 TA-2026-08\",\"resolution_ref\":\"股东会分配决议 R-2026-08\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private ResultActions submit(RequestPostProcessor who, String statement, String tag) throws Exception {
        return action(who, statement, "submit", tag);
    }

    private ResultActions action(RequestPostProcessor who, String statement, String verb, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/" + statement + "/" + verb).with(who)
                .contentType(MediaType.APPLICATION_JSON).content("{\"idempotency_key\":\"dbl-" + verb + "-" + tag + "-" + s + "\"}"));
    }

    /** 应收应付台账页勾选该笔借款应付发起付款确认（主线最终审批人＋财务、法务两线）的真实请求。 */
    private ResultActions confirm(String tag, String item) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/ar-ap-payment-confirmations").with(payer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"dbl-pc-" + tag + "-" + s + "\",\"ar_ap_item_ids\":[\"" + item + "\"],\"reason\":\"归还到期借款本息\","
                        + "\"final_approver_id\":\"" + mainId + "\",\"parallel_lines\":[{\"line_code\":\"finance_review\",\"final_approver_id\":\""
                        + finLineId + "\"},{\"line_code\":\"legal_review\",\"final_approver_id\":\"" + legalLineId + "\"}]}"));
    }

    /** 台账页「登记核销」的真实请求。 */
    private ResultActions pay(String item, String tag, long amountCent) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + item + "/payments").with(payer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"dbl-pay-" + tag + "-" + s + "\",\"amount_cent\":" + amountCent
                        + ",\"payment_method\":\"bank_transfer\",\"bank_flow_ref\":\"FLOW-" + tag + "-" + s + "\"}"));
    }

    /** 第十八批 BA 段第 314 条 314-R1：分红计算单批准只认最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        com.complex.platform.access.AccessActor actor = new com.complex.platform.access.AccessActor(
                com.complex.platform.auth.SessionIssuer.MASTER_USER_ID, com.complex.platform.auth.SessionIssuer.MASTER_USERNAME,
                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, null,
                            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_P0_STAFF"))))
                    .postProcessRequest(request);
        };
    }
}
