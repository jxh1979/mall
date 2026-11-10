package com.complex.platform.access.staff;

// 文件版本：V0.01

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AC 段第 210 条 210-R1：工资单月结人工成本与派驻、营销人员提成拨付自动挂接进费用台账人工类。依据：BSM:1801「连接工资单月结，
 * 拿到这期的人工成本」；BSM:1774 人工成本「含薪资、销售提成、绩效、奖金与社保，并含主理人本人的报酬」；BSM:1143「除此之外的一切费用一律由使用方酒吧
 * 承担——包括但不限于该人员在该酒吧促成订单产生的提成」；BSM:1719「月结定格的应发部分作代收代付、不计商户收入」；BSM:1787「一个周期一旦关账，
 * 这个已核算月份的费用就不能再改」；决策台账 E-01、DEC-R4-2026-08-04-60。
 *
 * <p>夹具随测试事务回滚：综合体、酒吧甲与酒吧乙（经营主体都是 E）2024-01-01 建档，月份 2024-11。甲的本店员工发放主体 E，工资带小费 2000、
 * 服务费 1500，个税 5000；派驻员工所属主体 C、在甲工作、工资由 C 发，在甲促成两笔提成 3000、1200；乙的本店员工发放主体 E，乙该月已有已审批正式分红单。
 * 工资计算与定格由平台人事、提成月结由 C 的人事、费用挂接与正式分红单由平台财务，都按前端蛇形键走真实接口与授权链；1200 那笔账单作废后的应收冲回
 * 照 BarBillAnomalyService 写法直接落库。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantExpensePayrollAttachJdbcIntegrationTests {

    private static final String MONTH = "2024-11";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_mep_" + s;
    private final String entity = "ent_mep_e_" + s;
    private final String company = "ent_mep_c_" + s;
    private final String barA = "mer_mep_a_" + s;
    private final String barB = "mer_mep_b_" + s;
    private final String local = "usr_mep_local_" + s;
    private final String dispatched = "usr_mep_disp_" + s;
    private final String lockedLocal = "usr_mep_lock_" + s;
    private final String hrId = "usr_mep_hr_" + s;
    private RequestPostProcessor hr;
    private RequestPostProcessor companyHr;
    private RequestPostProcessor finance;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', TIMESTAMPTZ '2024-01-01 00:00:00+08', now())", project, "人工挂接综合体" + s);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'management_company', 'active', now(), now())", entity, "人工挂接酒吧公司" + s);
        for (String bar : List.of(barA, barB)) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, project_id, status, created_at, updated_at)"
                    + " VALUES (?, ?, 'bar', ?, ?, 'active', TIMESTAMPTZ '2024-01-01 00:00:00+08', now())", bar, "人工挂接酒吧" + bar, entity, project);
            jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, payout_pay_slot, status)"
                    + " VALUES (?, ?, 'usr_mep_principal', 'monthly', 'bar', 1, 'active')", "divp_" + bar, bar);
        }
        // 210-R3 第十八批裁决 42 出单闸前提：经营主体 E 各月已有生效税费登记。
        com.complex.platform.support.EntityTaxFixture.taxDeclaredForAllMonths(jdbc, barA);
        employee(local, barA, entity);
        employee(dispatched, null, company);
        employee(lockedLocal, barB, entity);
        jdbc.update("INSERT INTO staff_dispatch_records (id, user_id, company_id, merchant_id, project_id, start_date, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, DATE '2024-11-01', 'active', now())", "dsp_mep_" + s, dispatched, company, barA, project);
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        hr = fixture.staff(hrId, "platform_admin", project, null, "staff.payroll.manage");
        companyHr = fixture.staff("usr_mep_chr_" + s, "staff", company, null, "staff.payroll.manage");
        finance = fixture.staff("usr_mep_fin_" + s, "platform_admin", project, null,
                "finance.merchant_expense.manage", "finance.dividend.manage");
        markRangeFinal(jdbc, "bar", barA, LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30));
        // 乙 2024-11 已出已审批正式分红单（关账）。
        jdbc.update("INSERT INTO dividend_statements (id, merchant_id, plan_id, kind, period_label, period_start, period_end,"
                + " principal_user_id, platform_share_bps, tax_rate_bps, revenue_domains, include_inventory_cost, expense_types,"
                + " revenue_cent, cost_cent, expense_cent, net_profit_cent, distributable_cent, platform_share_cent, principal_gross_cent,"
                + " tax_cent, principal_net_cent, status) VALUES (?, ?, ?, 'official', ?, DATE '2024-11-01', DATE '2024-11-30',"
                + " 'usr_mep_principal', 8000, 2000, 'bar', TRUE, 'rent_allocation,energy,property_fee,labor,amortization,other',"
                + " 0, 0, 0, 0, 0, 0, 0, 0, 0, 'approved')", "divs_mep_" + s, barB, "divp_" + barB, MONTH);
    }

    // 修复前自动挂接不接工资单与提成拨付：响应没有 payroll_inserted，第一条断言即红。
    @Test
    void payrollLaborAndDispatchCommissionTransfersAttachToTheBarWhoseEntityPays() throws Exception {
        allocations();
        String localStatement = settle(local, barA, entity, 100000, 5000);
        assertThat(jdbc.queryForMap("SELECT total_cent, tax_cent, tip_cent, service_fee_cent FROM staff_payroll_statements WHERE id = ?",
                localStatement)).containsAllEntriesOf(Map.of("total_cent", 98500L, "tax_cent", 5000L, "tip_cent", 2000L, "service_fee_cent", 1500L));
        commission("one", barA, dispatched, 3000);
        String voided = commission("two", barA, dispatched, 1200);
        String dispatchedStatement = settle(dispatched, barA, company, 80000, 0);
        String lockedStatement = settle(lockedLocal, barB, entity, 50000, 0);
        postJson(companyHr, "/api/v1/staff/commission-settlements/close",
                "{\"idempotency_key\":\"mep-close-" + s + "\",\"month\":\"" + MONTH + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.settlements").value(1));
        String transfer = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE source_type = 'dispatch_commission_transfer'"
                + " AND direction = 'payable' AND merchant_id = ?", String.class, barA);
        String reversal = "arap_dcr_mep_" + s;
        jdbc.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,"
                + " unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id)"
                + " VALUES (?, ?, 'receivable', ?, 'dispatch_commission_transfer', ?, 1200, 0, 1200, 'CNY', DATE '2024-12-05', ?,"
                + " 'pending_collection', now(), now(), ?, ?)", reversal, "ARAP-DCR-MEP-" + s, company, voided, MONTH, barA, project);

        sync("mep-sync-1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payroll_inserted").value(3))
                .andExpect(jsonPath("$.data.drift[?(@.source == 'auto_payroll')]").isEmpty());
        Map<String, Long> attached = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT source_ref, amount_cent, expense_type FROM merchant_expense_entries"
                + " WHERE merchant_id = ? AND period = ? AND source = 'auto_payroll'", barA, MONTH)) {
            assertThat(row.get("expense_type")).isEqualTo("labor");
            attached.put((String) row.get("source_ref"), ((Number) row.get("amount_cent")).longValue());
        }
        assertThat(attached).isEqualTo(Map.of(localStatement, 100000L, transfer, 4200L, reversal, -1200L));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchant_expense_entries WHERE source_ref IN (?, ?)", Integer.class,
                dispatchedStatement, lockedStatement)).isZero();
        sync("mep-sync-2-" + s).andExpect(status().isOk()).andExpect(jsonPath("$.data.payroll_inserted").value(0));

        String statement = JsonPath.read(postJson(finance, "/api/v1/finance/dividends/statements/compute",
                        "{\"idempotency_key\":\"mep-div-" + s + "\",\"period_label\":\"" + MONTH + "\",\"kind\":\"official\","
                                + "\"merchant_id\":\"" + barA + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.computed[0].id");
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'expense_labor'",
                Long.class, statement)).isEqualTo(-103000L);
    }

    // 18AC 复核修复（BSM:1143「人员所属主体只承担该人员的基本薪资、社保和其他常规报酬……除此之外的一切费用一律由使用方酒吧承担——包括但不限于
    // 该人员在该酒吧促成订单产生的提成」）：甲的本店员工（档案商户甲、所属主体为甲经营主体 E）经后台派驻登记派到经营主体不同的酒吧丙，在丙促成的
    // 提成 3000 经提成月结由丙拨付给 E、挂丙的人工类；甲挂这张工资单时扣掉这笔，只留基本工资与在甲本店促成的 1200。
    // 修复前甲整张工资单 104200 挂进人工类（丙那笔被两家各算一次），甲人工类金额断言即红。
    @Test
    void barStaffDispatchedToAnotherBarHasCommissionThereBorneOnlyByThatBar() throws Exception {
        String otherEntity = "ent_mep_f_" + s, barC = "mer_mep_c_" + s;
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'management_company', 'active', now(), now())", otherEntity, "人工挂接另一酒吧公司" + s);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, ?, 'active', TIMESTAMPTZ '2024-01-01 00:00:00+08', now())", barC, "人工挂接酒吧" + barC, otherEntity, project);
        markRangeFinal(jdbc, "bar", barC, LocalDate.of(2024, 11, 1), LocalDate.of(2024, 11, 30));
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        postJson(fixture.staff("usr_mep_dop_" + s, "platform_admin", project, null, "staff.profile.review"), "/api/v1/staff/dispatches",
                "{\"idempotency_key\":\"mep-dsp-" + s + "\",\"user_id\":\"" + local + "\",\"merchant_id\":\"" + barC
                        + "\",\"start_date\":\"2024-11-01\",\"note\":\"派驻到另一家酒吧\"}")
                .andExpect(status().isOk());
        commission("there", barC, local, 3000);
        commission("here", barA, local, 1200);
        String localStatement = settle(local, barA, entity, 100000, 0);
        assertThat(jdbc.queryForObject("SELECT total_cent FROM staff_payroll_statements WHERE id = ?", Long.class, localStatement))
                .isEqualTo(104200L);
        postJson(fixture.staff("usr_mep_ehr_" + s, "staff", entity, null, "staff.payroll.manage"), "/api/v1/staff/commission-settlements/close",
                "{\"idempotency_key\":\"mep-close-e-" + s + "\",\"month\":\"" + MONTH + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.settlements").value(1));
        String transfer = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE source_type = 'dispatch_commission_transfer'"
                + " AND direction = 'payable' AND merchant_id = ? AND counterparty_id = ? AND amount_cent = 3000", String.class, barC, entity);

        sync("mep-sync-d1-" + s).andExpect(status().isOk()).andExpect(jsonPath("$.data.payroll_inserted").value(2));
        assertThat(labor(barA)).isEqualTo(Map.of(localStatement, 101200L));
        assertThat(labor(barC)).isEqualTo(Map.of(transfer, 3000L));
        sync("mep-sync-d2-" + s).andExpect(status().isOk()).andExpect(jsonPath("$.data.payroll_inserted").value(0));

        String statement = JsonPath.read(postJson(finance, "/api/v1/finance/dividends/statements/compute",
                        "{\"idempotency_key\":\"mep-div-d-" + s + "\",\"period_label\":\"" + MONTH + "\",\"kind\":\"official\","
                                + "\"merchant_id\":\"" + barA + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.computed[0].id");
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'expense_labor'",
                Long.class, statement)).isEqualTo(-101200L);
    }

    private Map<String, Long> labor(String merchant) {
        Map<String, Long> attached = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT source_ref, amount_cent FROM merchant_expense_entries"
                + " WHERE merchant_id = ? AND period = ? AND source = 'auto_payroll' AND expense_type = 'labor'", merchant, MONTH)) {
            attached.put((String) row.get("source_ref"), ((Number) row.get("amount_cent")).longValue());
        }
        return attached;
    }

    private ResultActions sync(String key) throws Exception {
        return postJson(finance, "/api/v1/finance/merchant-expenses/sync-auto",
                "{\"idempotency_key\":\"" + key + "\",\"period\":\"" + MONTH + "\"}");
    }

    private ResultActions postJson(RequestPostProcessor session, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void employee(String user, String merchant, String payer) {
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', ?, NULL, 'active', now())", user, user, project);
        jdbc.update("INSERT INTO staff_profiles (id, project_id, user_id, name, company_id, merchant_id, registration_status, status,"
                + " employment_status, created_at, updated_at) VALUES (?, ?, ?, '人工挂接员工', ?, ?, 'approved', 'active', 'employed', now(), now())",
                "prof_" + user, project, user, payer, merchant);
    }

    /** 甲本店员工已确认的小费、服务费分配（发放主体 E；收款与分配规则链由 BarPayroll 用例覆盖）。 */
    private void allocations() {
        String run = "bpr_mep_" + s;
        jdbc.update("INSERT INTO bar_payroll_runs(id,project_id,merchant_id,month,source_fingerprint,source_snapshot,collected_tip_cent,"
                + "collected_service_fee_cent,payable_tip_cent,payable_service_fee_cent,withheld_service_fee_cent,rounding_residual_cent,"
                + "settlement_entity_id,status,created_by) VALUES(?,?,?,?,?,'{}'::jsonb,2000,1500,2000,1500,0,0,?,'confirmed',?)",
                run, project, barA, MONTH, key(), entity, hrId);
        for (Map.Entry<String, Long> part : Map.of("tip", 2000L, "service_fee", 1500L).entrySet()) {
            jdbc.update("INSERT INTO bar_payroll_allocations(id,run_id,user_id,month,component_type,amount_cent,merchant_id,project_id,"
                    + "payer_company_id,source_bill_ids,rule_version_id,status) VALUES(?,?,?,?,?,?,?,?,?,'[]'::jsonb,?,'confirmed')",
                    key(), run, local, MONTH, part.getKey(), part.getValue(), barA, project, entity, key());
        }
    }

    /** 员工 staff 在酒吧 bar 北京 2024-11-10 12:00 下单、已付清结账的账单及其提成计提；返回提成编号。 */
    private String commission(String tag, String bar, String staff, long amount) {
        String table = "tbl_mep_" + tag + "_" + s, order = "ord_mep_" + tag + "_" + s, bill = "bill_mep_" + tag + "_" + s;
        String id = "cm_mep_" + tag + "_" + s, at = "2024-11-10T12:00:00+08:00";
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '人工挂接卡座', 'booth', 'available', now(), now())", table, bar, table);
        jdbc.update("INSERT INTO bar_orders (id, bar_id, table_id, order_source, status, total_amount_cent, currency, created_at, updated_at,"
                + " order_type) VALUES (?, ?, ?, 'staff', 'settled', 100000, 'CNY', ?::timestamptz, ?::timestamptz, 'standard')",
                order, bar, table, at, at);
        jdbc.update("INSERT INTO bar_bills (id, bill_no, order_id, bar_id, table_id, subtotal_amount_cent, payable_amount_cent,"
                + " paid_amount_cent, currency, payment_status, settlement_status, created_at, updated_at, merchant_id, sales_staff_id)"
                + " VALUES (?, ?, ?, ?, ?, 100000, 100000, 100000, 'CNY', 'paid', 'settled', ?::timestamptz, ?::timestamptz, ?, ?)",
                bill, bill, order, bar, table, at, at, bar, staff);
        jdbc.update("INSERT INTO bar_commission_calculations (id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,"
                + " commission_rate_bp, commission_amount_cent, status, created_at) VALUES (?, ?, ?, 'staff_sales', 100000, 300, ?, 'calculated',"
                + " ?::timestamptz)", id, bill, staff, amount, at);
        return id;
    }

    /** 平台人事按前端请求形状经真实接口算工资原件、（本用例批准税额后）定格；返回工资单编号。 */
    private String settle(String user, String merchant, String payer, long base, long taxCent) throws Exception {
        String facts = PayrollApprovedFactsFixture.createForSubject(jdbc, json, user, project, merchant, payer, hrId, MONTH, base, 0, null).facts();
        Map<String, Object> data = JsonPath.read(postJson(hr, "/api/v1/staff/payroll/calculations",
                        "{\"idempotency_key\":\"" + key() + "\",\"user_id\":\"" + user + "\",\"month\":\"" + MONTH + "\",\"facts_id\":\""
                                + facts + "\",\"statement_kind\":\"monthly\",\"origin_event_id\":null}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data");
        CalculationDraft calc = json.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).convertValue(data, CalculationDraft.class);
        String tax = approvedTax(calc, taxCent);
        return JsonPath.read(postJson(hr, "/api/v1/staff/payroll/settlements",
                        "{\"facts_id\":\"" + facts + "\",\"statement_kind\":\"monthly\",\"calculation_id\":\"" + calc.id()
                                + "\",\"tax_assessment_id\":\"" + tax + "\",\"idempotency_key\":\"" + key() + "\",\"user_id\":\"" + user
                                + "\",\"month\":\"" + MONTH + "\",\"origin_event_id\":null}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.statement.id");
    }

    /** 同 PayrollApprovedCloseFixture.approvedZeroTax，税额由用例指定（真实核税证据链由 Workflow/TaxVersion 测试覆盖）。 */
    private String approvedTax(CalculationDraft calc, long taxCent) {
        String version = "v_" + key(), category = "fixture_" + key(), id = "tax_" + key();
        LocalDate date = YearMonth.parse(calc.month()).atEndOfMonth();
        var bracket = new PayrollTaxVersion.Bracket(1, 0L, null, 10000, 0L, 0, 0L);
        var calculation = new PayrollTaxAssessment.Calculation(version, "V0.01", category, date, "professional_confirmed", "本用例明确批准税额",
                null, calc.beforeTaxCent(), null, null, null, taxCent);
        jdbc.update("INSERT INTO payroll_tax_versions(id,category,version,effective_from,applicability_basis,calculation_basis,rounding_rule,"
                + "brackets,reason,status,created_by,created_at,decided_at) VALUES(?,?,'V0.01',?,'测试已核身份','本用例专业税额原件',"
                + "'truncate_cent_each_step',?::jsonb,'已批准输入夹具','approved',?,now(),now())",
                version, category, date, PayrollApprovedFactsFixture.encode(json, List.of(bracket)), hrId);
        jdbc.update("INSERT INTO payroll_tax_assessments(id,user_id,month,project_id,merchant_id,payer_company_id,category,tax_date,identity_basis,"
                + "transaction_basis,professional_basis,income_cent,source_lines,source_fingerprint,tax_version_id,tax_cent,calculation_snapshot,"
                + "reason,status,created_by,created_at,decided_at,payroll_calculation_id,professional_calculation_reference)"
                + " VALUES(?,?,?,?,?,?,?,?,'测试已核身份','本次完整工资','本用例明确批准税额',?,?::jsonb,?,?,?,?::jsonb,'已批准输入夹具','approved',?,"
                + "now(),now(),?,'测试专业原件')",
                id, calc.userId(), calc.month(), calc.projectId(), calc.merchantId(), calc.payerCompanyId(), category, date, calc.beforeTaxCent(),
                PayrollApprovedFactsFixture.encode(json, List.of(new PayrollTaxAssessment.SourceLine(calc.id(), calc.id(), calc.beforeTaxCent()))),
                calc.sourceFingerprint(), version, taxCent, PayrollApprovedFactsFixture.encode(json, calculation), hrId, calc.id());
        return id;
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
