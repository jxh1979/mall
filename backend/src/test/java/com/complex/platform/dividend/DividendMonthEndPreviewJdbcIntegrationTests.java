package com.complex.platform.dividend;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.finance.FinanceService;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 V 段第 159 条 159-R6 分红月度预览真库回归。依据：BSM:1777「该预览单在上月最后一个营业日的五类数据（见枚举速查表「日结状态」条）全部标记为最终
 * 之后自动生成，不设固定生成时点——数据未全部标记最终之前不得生成预览，标记完成即自动生成。」；DEC-R8-45；第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」。
 *
 * <p>不开测试事务（预览在标记提交之后才生成，测试事务里不会提交）。夹具：综合体与其下季度方案酒吧、月度方案酒吧、季度方案餐饮商户都建档 2026-05-01，
 * 编号带纯数字后缀；日结经真实生成接口、五类经真实标记接口（蛇形键）由持 finance.daily_close.lock、负责范围覆盖该综合体的财务 BP 走真实授权链；
 * 手动预览由持 finance.dividend.manage 的平台管理员走真实计算入口。类末把本类酒吧存档、综合体停用，移出五类判定单位集合
 * （其余营业日没有标记，留在库里会拖住按全部单位判定的用例）。</p>
 *
 * <p>159-R10（BSM:1777「每月另出一张只读的预览单……（预览只能看，不参与审批、不发放）」「标记完成即自动生成」）：另一家季度方案酒吧 08-25 有一张已付清的卡座账单，
 * 服务费分项在、8 月月结尚未定格；月末营业日五类标齐后预览照常生成，服务费扣留数暂不计。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DividendMonthEndPreviewJdbcIntegrationTests {

    private static final LocalDate AUG_31 = LocalDate.of(2026, 8, 31);
    private static final List<String> FIVE = List.copyOf(FinanceService.FINAL_MARK_CATEGORIES.keySet());

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "dmp_prj_" + s;
    private final String quarterlyBar = "dmp_q_" + s;
    private final String monthlyBar = "dmp_m_" + s;
    private final String catering = "dmp_c_" + s;
    private final String serviceFeeBar = "dmp_sf_" + s;
    private RequestPostProcessor financeBp;
    private RequestPostProcessor admin;

    @BeforeAll
    void units() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex',?,'active',TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", project, "月末预览综合体" + s);
        merchantWithPlan(quarterlyBar, "月末预览季度酒吧" + s, "bar", "quarterly");
        merchantWithPlan(monthlyBar, "月末预览月度酒吧" + s, "bar", "monthly");
        merchantWithPlan(catering, "月末预览餐饮商户" + s, "catering", "quarterly");
        merchantWithPlan(serviceFeeBar, "月末预览服务费酒吧" + s, "bar", "quarterly");
        serviceFeeBillAwaitingMonthClose();
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        financeBp = staffs.staff("usr_dmp_bp_" + s, "staff", project, null, "finance.daily_close.lock");
        admin = staffs.staff("usr_dmp_admin_" + s, "platform_admin", project, null, "finance.dividend.manage");
    }

    @AfterAll
    void retireUnits() {
        jdbc.update("UPDATE merchants SET archived_at = now() WHERE id IN (?, ?, ?)", quarterlyBar, monthlyBar, serviceFeeBar);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    // 修复前标记不发事件、也没有监听：标齐第五类后仍没有预览，「恰一张」断言红。
    @Test
    void previewIsGeneratedRightAfterTheLastBusinessDayOfTheMonthHasAllFiveCategoriesFinal() throws Exception {
        String barClose = generate("bar", quarterlyBar, AUG_31);
        for (String category : FIVE.subList(0, 4)) {
            mark(barClose, category);
        }
        assertThat(previews(quarterlyBar, "2026-08")).isEmpty();
        mark(barClose, FIVE.get(4));
        List<Map<String, Object>> barPreview = previews(quarterlyBar, "2026-08");
        assertThat(barPreview).hasSize(1);
        assertThat(barPreview.get(0)).containsEntry("status", "preview").containsEntry("computed_by", "system_dividend_preview");

        // 月中营业日五类标齐不出预览。
        markAll(generate("bar", quarterlyBar, LocalDate.of(2026, 9, 15)));
        assertThat(previews(quarterlyBar, "2026-09")).isEmpty();

        // 月度方案不另出预览。
        markAll(generate("bar", monthlyBar, AUG_31));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dividend_statements WHERE merchant_id = ?", Integer.class, monthlyBar))
                .isZero();

        // 综合体 project 范围的月末营业日五类标齐：其下非酒吧商户出预览，酒吧那张不重出。
        markAll(generate("project", project, AUG_31));
        assertThat(previews(catering, "2026-08")).hasSize(1);
        assertThat(previews(quarterlyBar, "2026-08")).singleElement()
                .satisfies(row -> assertThat(row.get("id")).isEqualTo(barPreview.get(0).get("id")));
    }

    // 修复前手动预览不判五类：直接出 1 张 2026-09 预览，computed 长度断言红。
    @Test
    void manualPreviewIsSkippedUntilTheLastBusinessDayOfTheMonthIsFinal() throws Exception {
        mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dmp-" + UUID.randomUUID() + "\",\"kind\":\"preview\",\"period_label\":\"2026-09\","
                                + "\"merchant_id\":\"" + quarterlyBar + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].reason").value(allOf(containsString("2026-09-30"), containsString("不得生成预览"))));
        assertThat(previews(quarterlyBar, "2026-09")).isEmpty();
    }

    // 159-R10：修复前取酒吧收入遇服务费扣留数未定即抛 409 BAR_REVENUE_PENDING，监听回滚、预览不出，「恰一张」断言红。
    @Test
    void previewStillGeneratesWhileServiceFeeAwaitsMonthCloseAndLeavesItsWithheldPartOut() throws Exception {
        markAll(generate("bar", serviceFeeBar, AUG_31));
        assertThat(previews(serviceFeeBar, "2026-08")).hasSize(1);
        Map<String, Object> line = jdbc.queryForMap("SELECT l.amount_cent, l.detail FROM dividend_statement_lines l"
                + " JOIN dividend_statements d ON d.id = l.statement_id WHERE d.merchant_id = ? AND d.kind = 'preview'"
                + " AND d.period_label = '2026-08' AND l.line_type = 'revenue_bar'", serviceFeeBar);
        assertThat(line.get("amount_cent")).isEqualTo(20_000L);
        assertThat((String) line.get("detail")).contains("尚未月结定格的服务费扣留数暂不计");

        // 同一商户手动出 2026-08 预览同样生成（刷新式：作废旧预览、重出一张）。
        mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dmp-" + UUID.randomUUID() + "\",\"kind\":\"preview\",\"period_label\":\"2026-08\","
                                + "\"merchant_id\":\"" + serviceFeeBar + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andExpect(jsonPath("$.data.computed[0].status").value("preview"))
                .andExpect(jsonPath("$.data.computed[0].revenue_cent").value(20_000));
    }

    // ---------- helpers ----------

    /**
     * 159-R10 前置态：服务费酒吧 08-25（北京 14:00）结清的一张卡座账单——折前消费 20000、服务费 2000，已付清 22000；
     * 服务费分项在（写法同 BarPlatformTurnoverRentJdbcTests 服务费待月结用例），没有已确认的 8 月月结。
     */
    private void serviceFeeBillAwaitingMonthClose() {
        String space = "dmp_space_" + s;
        String table = "dmp_seat_" + s;
        String order = "dmp_order_" + s;
        String bill = "dmp_bill_" + s;
        String session = "dmp_session_" + s;
        String at = "TIMESTAMPTZ '2026-08-25 14:00:00+08'";
        // 本类不开事务、铺位会留在库里：带上面积，免得能耗可租面积统计（MetricDetailConsistencyJdbcIntegrationTests）求和遇空值。
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, area_square_meter, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'shop', 30, 'active', now(), now())", space, project, "月末预览卡座区" + s);
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at, space_id)"
                + " VALUES (?, ?, ?, ?, '月末预览卡座', 'card_seat', 4, 0, 0, 'CNY', 'active', now(), now(), ?)",
                table, serviceFeeBar, serviceFeeBar, "DMP-SEAT-" + s, space);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','created',20000,'CNY'," + at + "," + at + ")", order, serviceFeeBar, serviceFeeBar, table);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,"
                + "deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at)"
                + " VALUES(?,?,?,?,?,?,20000,0,0,22000,22000,'CNY','paid','settled',0," + at + "," + at + ")",
                bill, "DMP" + s, order, serviceFeeBar, serviceFeeBar, table);
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at)"
                + " VALUES(?,?,'aggregate_pay',22000,?," + at + ")", "dmp_pay_" + s, bill, serviceFeeBar);
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,status,opened_at,closed_at,created_at,updated_at)"
                + " VALUES(?,?,?,DATE '2026-08-25',?,'closed'," + at + "," + at + "," + at + "," + at + ")", session, serviceFeeBar, serviceFeeBar, table);
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,"
                + "component_type,amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id)"
                + " VALUES(?,?,?,?,?,?,DATE '2026-08-25','service_fee',2000,20000,?,'[]'::jsonb,?)", "dmp_fee_" + s, bill, session, table,
                project, serviceFeeBar, "dmp_fee_event_" + s, bill);
    }

    /** 商户与其只计酒吧收入、不扣销货成本的生效分红方案。 */
    private void merchantWithPlan(String id, String name, String type, String periodType) {
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,?,'active',?,TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", id, name, type, project);
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status)"
                + " VALUES (?, ?, ?, ?, 'bar', FALSE, 'active')", "divp_" + id, id, "usr_dmp_principal_" + s, periodType);
    }

    /** 日结生成的真实请求形状（蛇形键）。 */
    private String generate(String scopeType, String scopeId, LocalDate day) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/finance/daily-closes").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dmp-" + UUID.randomUUID() + "\",\"business_date\":\"" + day
                                + "\",\"scope_type\":\"" + scopeType + "\",\"scope_id\":\"" + scopeId + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
    }

    /** 143-R2 五类最终标记的真实请求形状（蛇形键）。 */
    private void mark(String dailyCloseId, String category) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/final-marks").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dmp-" + UUID.randomUUID() + "\",\"category\":\"" + category + "\"}"))
                .andExpect(status().isOk());
    }

    private void markAll(String dailyCloseId) throws Exception {
        for (String category : FIVE) {
            mark(dailyCloseId, category);
        }
    }

    private List<Map<String, Object>> previews(String merchantId, String month) {
        return jdbc.queryForList("SELECT id, status, computed_by FROM dividend_statements WHERE merchant_id = ? AND kind = 'preview'"
                + " AND period_label = ?", merchantId, month);
    }
}
