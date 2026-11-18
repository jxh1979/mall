package com.complex.platform.dividend;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.support.EntityTaxFixture;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

/**
 * 第十八批 AQ 段第 234 条 234-R2 其他股东分配真库回归。依据：BSM:1775「娱乐平台公司、主理人和其他股东按每家酒吧工商登记的真实持股比例分配」；
 * BSM:1757「除涉税金额外，所有业务金额一律四舍五入、保留到分……多方分配产生的非涉税尾差单独留痕，并由该笔业务实际承担结算责任的主体承担」；
 * BSM:1776「股东为个人股东……由发放的经营主体代扣个人所得税……股东为公司股东的，不代扣个人所得税，按约定全额发放」；BSM:1787「分红由该酒吧公司自己从其
 * 对公账户支付给主理人与其他股东……发放走应付款项」；BSM:1803「审批通过后的发放进入应付款项台账，走付款确认」；BSM:1781「欠款抵扣：主理人这期该得的钱」；
 * 第十八批裁决 55、65（分红应付按台账来源 dividend_payout 走付款确认）。
 *
 * <p>酒吧 2026-08 五类全标、经营主体有已生效税费登记、已确认分账单里单店门票直付 10,000.06 元（即可分配额 D）。方案版本自 2000-01-01 起：平台 60%，
 * 其他股东个人「股东乙」10%、公司「丙公司」5%，主理人即 25%，代扣税率 20%，第一个付账日。请求按分红页、台账页与主管复核页的蛇形键走真实入口与授权链。
 * 不开测试事务：登记付款带准入闸，前一笔受闸调用失败会把测试事务标为只回滚（与线上每个请求各自一个事务不同）；夹具以纳秒后缀隔离。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class DividendOtherShareholdersJdbcIntegrationTests {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_31 = LocalDate.of(2026, 8, 31);
    /** 可分配额：平台 600,003.6、乙 100,000.6、丙 50,000.3、主理人 250,001.5 分各自四舍五入后合计多 1 分，尾差 −1。 */
    private static final long D = 1_000_006L;
    private static final long PLATFORM = MoneyRounding.bpsOf(D, 6000);
    private static final long PRINCIPAL = MoneyRounding.bpsOf(D, 2500);
    private static final long B_GROSS = MoneyRounding.bpsOf(D, 1000);
    private static final long C_GROSS = MoneyRounding.bpsOf(D, 500);
    private static final long B_TAX = B_GROSS * 2000 / 10000;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "dos_p_" + s;
    private final String bar = "dos_bar_" + s;
    private final String plan = "divp_dos_" + s;
    private final String principal = "usr_dos_principal_" + s;
    private final String payerId = "usr_dos_pf_" + s;
    private final String mainId = "usr_dos_s1_" + s;
    private final String finLineId = "usr_dos_fl_" + s;
    private final String legalLineId = "usr_dos_ll_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor approver;
    private RequestPostProcessor payer;
    private RequestPostProcessor main;
    private RequestPostProcessor finLine;
    private RequestPostProcessor legalLine;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','其他股东分红综合体','active',TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,?,now())", bar, "其他股东分红酒吧" + s, project, Timestamp.from(Instant.parse("2026-04-30T16:00:00Z")));
        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_31);
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 'confirmed', now(), now())", "sb_dos_" + s, project, AUG_1, AUG_31);
        jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent,"
                + " status) VALUES (?, ?, ?, 'exclusive_full', ?, ?, 'draft')", "si_dos_" + s, "sb_dos_" + s, bar, D, D);
        EntityTaxFixture.taxDeclaredForAllMonths(jdbc, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff("usr_dos_fin_" + s, "platform_admin", null, null, "finance.dividend.manage", "finance.dividend.view");
        approver = staffs.staff("usr_dos_apv_" + s, "platform_admin", null, null, "finance.dividend.manage", "finance.dividend.view");
        payer = staffs.staff(payerId, "platform_admin", null, null, "finance.payment.register", "finance.ar_ap.view");
        main = staffs.staff(mainId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        finLine = staffs.staff(finLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        legalLine = staffs.staff(legalLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", payerId, mainId);
    }

    /**
     * ① 正式单四方各自四舍五入、尾差带符号单列（−1 分）；两位其他股东各落应得、代扣、实发三行：个人股东按方案税率整除代扣，公司股东代扣 0；
     * 劈分恒等式含其他股东与尾差。修复前没有其他股东：计算单其他股东合计为 0、无股东明细行，断言红。
     */
    @Test
    void officialStatementSplitsAmongAllShareholdersWithSignedResidual() throws Exception {
        plan("individual", false);
        String statement = draft("1");

        assertThat(shareholderLines(statement)).containsExactly(
                tuple("shareholder_gross", B_GROSS), tuple("shareholder_tax_withheld", -B_TAX), tuple("shareholder_payout", B_GROSS - B_TAX),
                tuple("shareholder_gross", C_GROSS), tuple("shareholder_tax_withheld", 0L), tuple("shareholder_payout", C_GROSS));
        Map<String, Object> row = jdbc.queryForMap("SELECT distributable_cent, platform_share_cent, principal_gross_cent,"
                + " other_shareholders_gross_cent, rounding_residual_cent, tax_cent FROM dividend_statements WHERE id = ?", statement);
        assertThat(cent(row, "distributable_cent")).isEqualTo(D);
        assertThat(cent(row, "platform_share_cent")).isEqualTo(PLATFORM);
        assertThat(cent(row, "principal_gross_cent")).isEqualTo(PRINCIPAL);
        assertThat(cent(row, "other_shareholders_gross_cent")).isEqualTo(B_GROSS + C_GROSS);
        assertThat(cent(row, "rounding_residual_cent")).isEqualTo(D - PLATFORM - PRINCIPAL - B_GROSS - C_GROSS).isEqualTo(-1L);
        assertThat(cent(row, "tax_cent")).isEqualTo(PRINCIPAL * 2000 / 10000);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'rounding_residual'",
                Long.class, statement)).isEqualTo(-1L);
    }

    /** ①b 主理人为公司股东：主理人代扣 0，个人股东乙仍按方案税率 20% 整除代扣（不随主理人生效税率变 0）。修复前无股东明细行：断言红。 */
    @Test
    void companyPrincipalDoesNotZeroIndividualShareholderWithholding() throws Exception {
        plan("company", false);
        String statement = draft("1b");

        assertThat(jdbc.queryForObject("SELECT tax_cent FROM dividend_statements WHERE id = ?", Long.class, statement)).isZero();
        assertThat(shareholderLines(statement)).filteredOn(line -> "shareholder_tax_withheld".equals(line.toList().get(0)))
                .containsExactly(tuple("shareholder_tax_withheld", -B_TAX), tuple("shareholder_tax_withheld", 0L));
    }

    /**
     * ② 审批通过：主理人照旧一笔应付（来源为计算单）；每位其他股东另挂一笔 dividend_payout 应付，应付对方与来源都是该股东实发行，金额为实发，到期日与
     * 主理人同为方案付账日。方案开欠款抵扣、商户有未清应收 300 元：只抵主理人实发，两位其他股东实发不变。修复前审批不挂股东应付：断言红。
     */
    @Test
    void approvalHangsOnePayablePerShareholderAndArrearsOffsetOnlyReducesPrincipal() throws Exception {
        plan("individual", true);
        jdbc.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,"
                + " unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id)"
                + " VALUES (?, ?, 'receivable', ?, 'manual', ?, 30000, 0, 30000, 'CNY', '2026-07-01', '2026-07', 'pending_collection', now(), now(), ?)",
                "arap_dos_rcv_" + s, "ARAP-DOS-" + s, bar, "arap_dos_rcv_" + s, bar);
        String statement = approved("2");

        long principalNet = PRINCIPAL - PRINCIPAL * 2000 / 10000;
        Map<String, Object> head = jdbc.queryForMap("SELECT arrears_offset_cent, payout_cent, ar_ap_item_id FROM dividend_statements WHERE id = ?",
                statement);
        assertThat(cent(head, "arrears_offset_cent")).isEqualTo(30_000L);
        assertThat(cent(head, "payout_cent")).isEqualTo(principalNet - 30_000L);
        Map<String, Object> principalPayable = jdbc.queryForMap("SELECT source_id, amount_cent, due_date FROM ar_ap_items WHERE id = ?",
                head.get("ar_ap_item_id"));
        assertThat(principalPayable.get("source_id")).isEqualTo(statement);

        List<Map<String, Object>> payables = shareholderPayables(statement);
        assertThat(payables).extracting(p -> p.get("counterparty_id").equals(p.get("line_id")), p -> cent(p, "amount_cent"), p -> p.get("due_date"))
                .containsExactly(tuple(true, B_GROSS - B_TAX, principalPayable.get("due_date")),
                        tuple(true, C_GROSS, principalPayable.get("due_date")));
    }

    /**
     * ③ 主理人应付按付款确认付清后，其他股东应付未付清时标记发放 409 DIVIDEND_PAYOUT_NOT_PAID；股东乙应付不经付款确认登记付款 409
     * PAYMENT_CONFIRMATION_REQUIRED；乙、丙各自发起付款确认（类型 dividend_payout）、各线通过并登记付清后，标记发放 200。修复前主理人付清即可标记发放：断言红。
     */
    @Test
    void markPaidWaitsForEveryShareholderPayableSettledThroughPaymentConfirmation() throws Exception {
        plan("individual", false);
        String statement = approved("3");
        String principalPayable = jdbc.queryForObject("SELECT ar_ap_item_id FROM dividend_statements WHERE id = ?", String.class, statement);
        confirmAndPay(principalPayable, "pr");

        markPaid(statement, "early").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_PAYOUT_NOT_PAID"));
        List<Map<String, Object>> payables = shareholderPayables(statement);
        assertThat(payables).hasSize(2);
        String shareholderB = (String) payables.get(0).get("id");
        pay(shareholderB, "b-early", cent(payables.get(0), "amount_cent")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));

        confirmAndPay(shareholderB, "b");
        markPaid(statement, "half").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_PAYOUT_NOT_PAID"));
        confirmAndPay((String) payables.get(1).get("id"), "c");
        markPaid(statement, "done").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
    }

    /**
     * ④ 方案版本：平台与其他股东合计超过 100% 或股东名称为空 400 VALIDATION_FAILED、不落版本；最高权限本人提交带其他股东的版本直接生效，再在其上提交
     * 不带名单的新版本时照改前版本复制名单。修复前忽略 other_shareholders：前两次提交都落成新版本，断言红。
     */
    @Test
    void planVersionValidatesShareholdersAndCopiesListToNextVersion() throws Exception {
        plan("individual", false);
        LocalDate tomorrow = businessDays.merchantBusinessDate(null, bar, Instant.now()).plusDays(1);
        submit(finance, plan, body("4a", tomorrow, ",\"other_shareholders\":[{\"shareholder_name\":\"股东乙\","
                + "\"shareholder_type\":\"individual\",\"share_bps\":4100}]"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(finance, plan, body("4b", tomorrow, ",\"other_shareholders\":[{\"shareholder_name\":\" \","
                + "\"shareholder_type\":\"individual\",\"share_bps\":1000}]"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM dividend_plans WHERE merchant_id = ?", Integer.class, bar)).isEqualTo(1);

        String v2 = JsonPath.read(submit(master(), plan, body("4c", tomorrow, ",\"other_shareholders\":[{\"shareholder_name\":\"股东丁\","
                        + "\"shareholder_type\":\"company\",\"share_bps\":2000}]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.other_shareholders[0].shareholder_name").value("股东丁"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        String v3 = JsonPath.read(submit(master(), v2, body("4d", tomorrow.plusDays(1), ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbc.queryForList("SELECT shareholder_name, shareholder_type, share_bps FROM dividend_plan_shareholders WHERE plan_id = ?", v3))
                .extracting(r -> r.get("shareholder_name"), r -> r.get("shareholder_type"), r -> r.get("share_bps"))
                .containsExactly(tuple("股东丁", "company", 2000));
    }

    // ---------- helpers ----------

    /** 已批准方案版本与其他股东名单（主理人类型、是否开欠款抵扣按用例给）。 */
    private void plan(String principalType, boolean arrearsOffset) {
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, platform_share_bps, tax_rate_bps, period_type, revenue_domains,"
                + " include_inventory_cost, status, payout_pay_slot, effective_from, reason, period_min_months, period_max_months, principal_type,"
                + " arrears_offset_enabled) VALUES (?, ?, ?, 6000, 2000, 'monthly', 'ticket', TRUE, 'active', 1, DATE '2000-01-01', '工商登记持股比例',"
                + " 1, 12, ?, ?)", plan, bar, principal, principalType, arrearsOffset);
        jdbc.update("INSERT INTO dividend_plan_shareholders (id, plan_id, shareholder_name, shareholder_type, share_bps, sort_order)"
                + " VALUES (?, ?, '股东乙', 'individual', 1000, 0), (?, ?, '丙公司', 'company', 500, 1)", "dps_b_" + s, plan, "dps_c_" + s, plan);
    }

    /** 分红页期间作业「正式单」的真实请求，返回草稿单号。 */
    private String draft(String tag) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dos-c-" + tag + "-" + s + "\",\"period_label\":\"2026-08\",\"kind\":\"official\","
                                + "\"merchant_id\":\"" + bar + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.computed[0].id");
    }

    /** 出正式单、登记依法可分配金额（重算出新草稿）、提交审阅、另一人审批通过，返回已审批单号。 */
    private String approved(String tag) throws Exception {
        String draft = draft(tag);
        String confirmed = JsonPath.read(mockMvc.perform(post("/api/v1/finance/dividends/statements/" + draft + "/legal-confirmation")
                        .with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dos-l-" + tag + "-" + s + "\",\"legal_distributable_cent\":2000000,"
                                + "\"tax_accounting_ref\":\"财税核算" + s + "\",\"resolution_ref\":\"分配决议" + s + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        statementAction(finance, confirmed, "submit", tag).andExpect(status().isOk());
        statementAction(approver, confirmed, "approve", tag).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("approved"));
        return confirmed;
    }

    private ResultActions statementAction(RequestPostProcessor who, String statement, String action, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/" + statement + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content("{\"idempotency_key\":\"dos-" + action + "-" + tag + "-" + s + "\"}"));
    }

    private ResultActions markPaid(String statement, String tag) throws Exception {
        return statementAction(approver, statement, "mark-paid", tag);
    }

    /** 该单其他股东明细行（行类型、金额，按行序）。 */
    private List<org.assertj.core.groups.Tuple> shareholderLines(String statement) {
        return jdbc.queryForList("SELECT line_type, amount_cent FROM dividend_statement_lines WHERE statement_id = ?"
                        + " AND line_type LIKE 'shareholder%' ORDER BY seq", statement).stream()
                .map(r -> tuple(r.get("line_type"), cent(r, "amount_cent"))).toList();
    }

    /** 该单每条其他股东实发行对应的 dividend_payout 应付（按行序）。 */
    private List<Map<String, Object>> shareholderPayables(String statement) {
        return jdbc.queryForList("SELECT i.id, i.counterparty_id, i.amount_cent, i.due_date, l.id AS line_id FROM dividend_statement_lines l"
                + " JOIN ar_ap_items i ON i.direction = 'payable' AND i.source_type = 'dividend_payout' AND i.source_id = l.id"
                + " WHERE l.statement_id = ? AND l.line_type = 'shareholder_payout' ORDER BY l.seq", statement);
    }

    /** 台账页勾选该笔应付发起付款确认（主线与财务、法务两线），各线通过后登记付清。 */
    private void confirmAndPay(String item, String tag) throws Exception {
        String review = JsonPath.read(mockMvc.perform(post("/api/v1/finance/ar-ap-payment-confirmations").with(payer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dos-pc-" + tag + "-" + s + "\",\"ar_ap_item_ids\":[\"" + item + "\"],"
                                + "\"reason\":\"八月分红发放\",\"final_approver_id\":\"" + mainId + "\",\"parallel_lines\":["
                                + "{\"line_code\":\"finance_review\",\"final_approver_id\":\"" + finLineId + "\"},"
                                + "{\"line_code\":\"legal_review\",\"final_approver_id\":\"" + legalLineId + "\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_type").value("dividend_payout"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
        approve(main, review, tag + "-main");
        approve(finLine, review, tag + "-fin");
        approve(legalLine, review, tag + "-legal");
        long unpaid = jdbc.queryForObject("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id = ?", Long.class, item);
        pay(item, tag, unpaid).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("settled"));
    }

    private void approve(RequestPostProcessor who, String review, String tag) throws Exception {
        mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(who).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dos-ap-" + tag + "-" + s + "\",\"decision\":\"approved\",\"comment\":\"已核对付款明细\"}"))
                .andExpect(status().isOk());
    }

    /** 台账页「登记核销」的真实请求。 */
    private ResultActions pay(String item, String tag, long amountCent) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + item + "/payments").with(payer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"dos-pay-" + tag + "-" + s + "\",\"amount_cent\":" + amountCent
                        + ",\"payment_method\":\"bank_transfer\",\"bank_flow_ref\":\"FLOW-dos-" + tag + "-" + s + "\"}"));
    }

    /** 分红页方案弹窗「提交最高权限审批」的真实请求形状（蛇形键），extra 为追加的键。 */
    private static String body(String tag, LocalDate effectiveFrom, String extra) {
        return "{\"idempotency_key\":\"dos-v-" + tag + "-" + System.nanoTime() + "\",\"platform_share_bps\":6000,\"payout_pay_slot\":1,"
                + "\"status\":\"active\",\"effective_from\":\"" + effectiveFrom + "\",\"reason\":\"工商登记股东变更\"" + extra + "}";
    }

    private ResultActions submit(RequestPostProcessor who, String planId, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/finance/dividends/plans/" + planId).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static long cent(Map<String, Object> row, String column) {
        return ((Number) row.get(column)).longValue();
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
}
