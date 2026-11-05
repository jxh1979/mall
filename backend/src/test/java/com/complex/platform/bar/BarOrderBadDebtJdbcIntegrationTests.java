package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 T 段第 157 条 157-R3（真库）：BSM:1109「经理确认最终无法收款后转为坏账损耗处理中并解除该订单对交班、日结的阻断……在转入该状态前继续阻断。」；
 * BSM:1110「服务员责任以商品出品时该桌绑定的主责服务员为准，后续换台、换班或改绑不追溯变更；出品时没有主责服务员的记为门店损耗」；
 * BSM:1111「商户核销未收售价，不确认销售收入，也不计入商户营业额或营业额抽成基数，同时建立实际商品成本赔偿记录。财务确认未收售价、库存成本和赔偿记录均完成后，
 * 订单转为已核销结案……转坏账核销的订单不产生销售提成……已核销结案后又收回的款项按收回时点单独入账，不追溯重开提成。」；BSM:1112「审查通过前只登记」；
 * BSM:1058 关台坏账出口；第十八批裁决 38 选项原文「收入与出库成本都不进分红净利润」。
 * 请求按后台订单页真实形状（蛇形键、X-Terminal-Type: web）经控制器与真实授权链提交。失败的请求会把测试事务标成只回滚，之后班结的准入闸即按失败关闭——
 * 各用例先发成功的请求，预期失败的放在最后。
 * 157-R4：BSM:1112「达到既有高价值标准的商品仍执行原证据和审批要求。」；ENUM:219「主管复核强制项：……高价值报损/盘点差异/存酒作废」；
 * BSM:1109「在转入该状态前继续阻断」——本单已出品高价值商品时转坏账须带照片与视频、经库存高价值复核批准才生效。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarOrderBadDebtJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private BarRevenueSourceService revenues;
    @Autowired private BarPayrollAllocationService allocations;
    @Autowired private BusinessDayResolver days;
    @Autowired private BarUnpaidCloseEscalationJob escalation;

    private String s;
    private String project;
    private String bar;
    private String wine;
    private String highWine;
    private String cashierId;
    private String managerId;
    private String waiterId;
    private String nextWaiterId;
    private RequestPostProcessor cashier;
    private RequestPostProcessor manager;
    private RequestPostProcessor hangOnly;
    private RequestPostProcessor finance;
    private RequestPostProcessor otherFinance;
    private RequestPostProcessor admin;
    private RequestPostProcessor reviewer;
    private String reviewerId;

    @BeforeEach
    void seed() throws Exception {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        project = "prj_bd_" + s;
        String otherProject = "prj_bd_other_" + s;
        String company = "ent_bd_" + s;
        bar = "bar_bd_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','坏账订单综合体','active',now(),now()),"
                + "(?,'complex','另一综合体','active',now(),now())", project, otherProject);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'坏账订单酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'包厢区','unit','active',now(),now())",
                "space_bd_" + s, project, bar);
        // 班结过合同准入闸：本酒吧有一份含酒吧业务的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "bd_ct_" + s, bar, bar, bar);
        // 包厢开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_bd')
                """, "bpr_bd_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        // 整瓶出品扣库存：吧台库位 10 瓶，移动加权成本每瓶 8000（出品流水按此计成本）。
        String item = "item_bd_" + s;
        String location = "loc_bd_" + s;
        String counter = "wh_bd_" + s;
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '包厢吧台')", counter, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '干红 750 毫升', '酒类', '瓶', 'active', ?, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id, project_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?, ?)", "bal_bd_" + s, item, location, bar, counter, project);
        jdbc.update("INSERT INTO inventory_item_cost_state(item_id,merchant_id,on_hand_quantity,total_cost_cent,avg_unit_cost_cent,has_cost_basis)"
                + " VALUES(?,?,10,80000,8000,TRUE)", item, bar);
        Instant now = Instant.now();
        wine = "product_bd_wine_" + s;
        bars.saveProduct(new BarProductRecord(wine, bar, bar, "干红", "wine", 30000L, "CNY", item, location, true, "active",
                now, now, null, null, null, false, null, null, null));
        // 157-R4：高价值按物品最近一次实际到货价与全平台阈值（出厂 5000 元）判——干红到货价 80 元为低价值；另备到货价 6000 元的高价值洋酒，
        // 吧台 5 瓶、移动加权成本每瓶 600000。
        arrival("arr_bd_" + s, item, location, 8000L);
        String highItem = "item_bd_high_" + s;
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '陈年威士忌 700 毫升', '酒类', '瓶', 'active', ?, now(), now())", highItem, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id, project_id)"
                + " VALUES (?, ?, ?, 5, 0, 5, now(), ?, ?, ?)", "bal_bd_high_" + s, highItem, location, bar, counter, project);
        jdbc.update("INSERT INTO inventory_item_cost_state(item_id,merchant_id,on_hand_quantity,total_cost_cent,avg_unit_cost_cent,has_cost_basis)"
                + " VALUES(?,?,5,3000000,600000,TRUE)", highItem, bar);
        arrival("arr_bd_high_" + s, highItem, location, 600000L);
        highWine = "product_bd_high_" + s;
        bars.saveProduct(new BarProductRecord(highWine, bar, bar, "陈年威士忌", "wine", 900000L, "CNY", highItem, location, true, "active",
                now, now, null, null, null, false, null, null, null));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashierId = "usr_bd_cashier_" + s;
        managerId = "usr_bd_manager_" + s;
        waiterId = "usr_bd_waiter_" + s;
        nextWaiterId = "usr_bd_waiter2_" + s;
        cashier = staffs.staff(cashierId, "staff", project, bar, "bar.order.create", "finance.payment.register", "bar.bill.hang",
                "bar.shift.manage", "bar.shift.close", "bar.bill.split_merge", "bar.table_session.transfer", "bar.table.open_close",
                "bar.table_commission.calculate");
        manager = staffs.staff(managerId, "staff", project, bar, "bar.bill.hang_cancel", "evidence.attachment.create",
                "inventory.high_value.review");
        reviewerId = "usr_bd_reviewer_" + s;
        reviewer = staffs.staff(reviewerId, "staff", project, bar, "inventory.high_value.review");
        hangOnly = staffs.staff("usr_bd_hangonly_" + s, "staff", project, bar, "bar.bill.hang");
        staffs.staff(waiterId, "staff", project, bar, "bar.order.view");
        // 接手服务员须在岗、有员工档案与本综合体点单权限（交接校验）。
        staffs.staff(nextWaiterId, "staff", project, bar, "bar.order.create");
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,'接手服务员',?,'approved','active','employed',FALSE,now(),now())",
                "pro_bd_" + s, project, bar, nextWaiterId, company);
        finance = staffs.staff("usr_bd_fin_" + s, "staff", project, null, "finance.ar_ap.manage");
        otherFinance = staffs.staff("usr_bd_fin_other_" + s, "staff", otherProject, null, "finance.ar_ap.manage");
        admin = staffs.staff("usr_bd_admin_" + s, "platform_admin", project, null, "finance.daily_close.lock", "finance.daily_close.manage");
    }

    /**
     * 开台主责 A、出品 2 瓶后交接给 B、收银员挂起 → 经理确认无法收款：转坏账损耗处理中，固化未收售价、本单出库成本与赔偿登记（责任人仍为出品时的 A），留痕；
     * 随后同班次可交班、可关台，该营业日日结不再报未结清账单；该场次服务费分配不再以「原收款账单尚未完成真实收款」挂住。
     */
    @Test
    void managerConfirmsBadDebtFreezesCostAndResponsibilityAndReleasesGates() throws Exception {
        String shift = openShift();
        String table = table("vip");
        String session = openSession("vip", table, waiterId);
        responsible(session, table, waiterId);
        String order = orderId(order(table, 2));
        String bill = bill(order, "b1");
        deliver(order).andExpect(status().isOk());
        String item = jdbc.queryForObject("SELECT id FROM bar_order_items WHERE order_id = ?", String.class, order);
        handover(session, nextWaiterId).andExpect(status().isOk());
        suspend(order, "客人称明天来付").andExpect(status().isOk());

        String id = data(badDebt(manager, order, "客人失联，确认无法收回", "bd-1-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("bad_debt_processing"))
                .andExpect(jsonPath("$.data.bad_debt_reason").value("客人失联，确认无法收回"))
                .andExpect(jsonPath("$.data.bad_debt_confirmed_by").value(managerId))
                .andExpect(jsonPath("$.data.bad_debt_confirmed_at").isNotEmpty())
                .andExpect(jsonPath("$.data.unpaid_price_cent").value(60000))
                .andExpect(jsonPath("$.data.inventory_cost_cent").value(16000))
                .andExpect(jsonPath("$.data.compensation_lines.length()").value(1))
                .andExpect(jsonPath("$.data.compensation_lines[0].order_item_id").value(item))
                .andExpect(jsonPath("$.data.compensation_lines[0].responsible_user_id").value(waiterId))
                .andExpect(jsonPath("$.data.compensation_lines[0].cost_cent").value(16000)), "$.data.id");
        Map<String, Object> event = jdbc.queryForMap("SELECT metadata->>'reason' AS reason, metadata->>'unpaid_price_cent' AS unpaid,"
                + " metadata->>'inventory_cost_cent' AS cost FROM audit_events WHERE event_name = 'bar.order_suspension_bad_debt' AND business_id = ?", id);
        assertThat(event).containsEntry("reason", "客人失联，确认无法收回").containsEntry("unpaid", "60000").containsEntry("cost", "16000");

        close(shift).andExpect(status().isOk());
        serviceClose(session).andExpect(status().isOk());
        // 该场次服务费分项（收款账单即本坏账账单）：同作废处理，不再挂「原收款账单尚未完成真实收款」。
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,"
                + "component_type,amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id)"
                + " VALUES(?,?,?,?,?,?,?,'service_fee',6000,60000,?,'[]'::jsonb,?)", "bpc_bd_" + s, bill, session, table, project, bar,
                java.sql.Date.valueOf(LocalDate.now()), "bd_fee_event_" + s, bill);
        jdbc.update("INSERT INTO bar_payroll_collection_links(source_bill_id,collection_bill_id,active) VALUES(?,?,true)", bill, bill);
        assertThat(allocations.sourceUnresolved(bill, YearMonth.now().toString()))
                .noneMatch(reason -> reason.contains("原收款账单尚未完成真实收款"));

        LocalDate day = days.businessDate(null, bar, Instant.now());
        String close = data(mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bd-close-" + s, "operator_note", "日结",
                        "business_date", day.toString(), "scope_type", "merchant", "scope_id", bar)))).andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/finance/daily-closes/" + close + "/confirm").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bd-confirm-" + s))))
                .andExpect(jsonPath("$.detail_code").value(not("DAY_CLOSE_UNSETTLED_BILL")));
    }

    /** 合单后改指主单的挂起转坏账：主单不再算未结清，台组可关台；未收售价按主单计。 */
    @Test
    void badDebtOnMergedSuspensionReleasesPrimaryBill() throws Exception {
        String table = table("vipg");
        String session = openSession("vipg", table, null);
        String first = orderId(order(table, 1));
        String primary = bill(first, "g1");
        String second = orderId(order(table, 1));
        String secondary = bill(second, "g2");
        suspend(second, "客人稍后付款").andExpect(status().isOk());
        Map<String, Object> merge = new LinkedHashMap<>();
        merge.put("idempotency_key", "bd-merge-" + s);
        merge.put("merged_bill_id", secondary);
        merge.put("reason", "同包厢合并结账");
        mockMvc.perform(post("/api/v1/bar/bills/" + primary + "/merge").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(merge))).andExpect(status().isOk());
        badDebt(manager, second, "客人失联", "bd-g-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unpaid_price_cent").value(60000))
                .andExpect(jsonPath("$.data.compensation_lines.length()").value(0));
        serviceClose(session).andExpect(status().isOk());
    }

    /**
     * 部分收款后挂起、转坏账：整单不进酒吧收入来源（营业额抽成与分红共用）；财务核销结案后又收回、结清，按收回时点计入；
     * 结案前后都不产生销售提成（409 BAR_BILL_BAD_DEBT）。
     */
    @Test
    void writeOffClosesAndLaterRecoveryCountsWithoutCommission() throws Exception {
        String table = table("vipw");
        openSession("vipw", table, null);
        String order = orderId(order(table, 2));
        String bill = bill(order, "w");
        settle(bill, 20000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("partial_settled"));
        suspend(order, "客人稍后付款").andExpect(status().isOk());
        badDebt(manager, order, "客人失联", "bd-w-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unpaid_price_cent").value(40000));
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);
        assertThat(revenues.completedCollections(bar, from, to)).noneMatch(row -> row.billId().equals(bill));

        String id = data(writeOff(finance, order, "bd-wo-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("written_off_closed"))
                .andExpect(jsonPath("$.data.written_off_by").value("usr_bd_fin_" + s))
                .andExpect(jsonPath("$.data.written_off_at").isNotEmpty()), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT metadata->>'unpaid_price_cent' FROM audit_events"
                + " WHERE event_name = 'bar.order_suspension_written_off' AND business_id = ?", String.class, id)).isEqualTo("40000");

        settle(bill, 40000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(jdbc.queryForObject("SELECT status FROM bar_order_suspensions WHERE order_id = ?", String.class, order))
                .isEqualTo("written_off_closed");
        assertThat(revenues.completedCollections(bar, from, to)).anyMatch(row -> row.billId().equals(bill)
                && "confirmed".equals(row.status()) && row.goodsRevenueCent() == 60000L);

        mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/commissions/calculate").with(cashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bd-cm-" + s, "bill_id", bill))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_BAD_DEBT"));
    }

    /** 无经理权限 403；未挂起或已转坏账的再确认 409；未转坏账不能核销 409；别的综合体的财务 403 且不占幂等键；原因必填。 */
    @Test
    void permissionsStatesAndScope() throws Exception {
        String table = table("vipp");
        openSession("vipp", table, null);
        String order = orderId(order(table, 1));
        bill(order, "p");
        String plain = orderId(order(table, 1));
        bill(plain, "p2");
        suspend(order, "客人稍后付款").andExpect(status().isOk());

        badDebt(hangOnly, order, "客人失联", "bd-p-noperm-" + s).andExpect(status().isForbidden());
        badDebt(manager, plain, "客人失联", "bd-p-plain-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_NOT_HANGING_UNPAID"));
        badDebt(manager, order, "", "bd-p-blank-" + s).andExpect(status().isBadRequest());
        writeOff(finance, order, "bd-p-wo-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_NOT_BAD_DEBT"));
        String crossKey = "bd-p-cross-" + s;
        writeOff(otherFinance, order, crossKey).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_records WHERE idempotency_key = ?", Integer.class, crossKey))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM bar_order_suspensions WHERE order_id = ?", String.class, order))
                .isEqualTo("hanging_unpaid");
    }

    /**
     * 157-R4：本单已出品高价值洋酒，经理带照片与视频转坏账——不直接转坏账，建库存高价值复核单（subject bar_order_bad_debt），挂起仍为已挂起待付款；
     * 另一主管在高价值复核页批准后才转坏账损耗处理中（确认人为发起经理，复核人记审计），随后同班次可交班。
     */
    @Test
    void highValueBadDebtWaitsForSupervisorReviewThenReleasesShift() throws Exception {
        String shift = openShift();
        String table = table("viph");
        openSession("viph", table, null);
        String order = orderId(order(table, highWine, 1));
        bill(order, "h1");
        deliver(order).andExpect(status().isOk());
        suspend(order, "客人称明天来付").andExpect(status().isOk());
        String photo = upload("现场照片.jpg", "image/jpeg");
        String video = upload("现场视频.mp4", "video/mp4");

        String review = data(badDebt(manager, order, "客人失联，确认无法收回", "bd-h-" + s, List.of(photo), List.of(video))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("hanging_unpaid"))
                .andExpect(jsonPath("$.data.bad_debt_confirmed_at").value(nullValue()))
                .andExpect(jsonPath("$.data.bad_debt_review_id").isNotEmpty()), "$.data.bad_debt_review_id");
        assertThat(jdbc.queryForMap("SELECT subject_type, subject_id, review_status, created_by FROM inventory_high_value_reviews WHERE id = ?",
                review)).containsEntry("subject_type", "bar_order_bad_debt").containsEntry("subject_id", order)
                .containsEntry("review_status", "pending").containsEntry("created_by", managerId);

        decide(reviewer, review, "approved").andExpect(status().isOk()).andExpect(jsonPath("$.data.review_status").value("approved"));
        Map<String, Object> row = jdbc.queryForMap("SELECT id, status, bad_debt_reason, bad_debt_confirmed_by, unpaid_price_cent, inventory_cost_cent"
                + " FROM bar_order_suspensions WHERE order_id = ?", order);
        assertThat(row).containsEntry("status", "bad_debt_processing").containsEntry("bad_debt_reason", "客人失联，确认无法收回")
                .containsEntry("bad_debt_confirmed_by", managerId).containsEntry("unpaid_price_cent", 900000L)
                .containsEntry("inventory_cost_cent", 600000L);
        assertThat(jdbc.queryForObject("SELECT metadata->>'bad_debt_review_id' FROM audit_events WHERE event_name = 'bar.order_suspension_bad_debt'"
                + " AND business_id = ? AND metadata->>'reviewer_user_id' = ?", String.class, row.get("id"), reviewerId)).isEqualTo(review);
        close(shift).andExpect(status().isOk());
    }

    /** 157-R4：复核驳回 → 挂起仍为已挂起待付款、清空待复核的复核单与原因，可重新发起；待复核期间仍挡交班。 */
    @Test
    void rejectedHighValueBadDebtReviewClearsAndShiftStaysBlocked() throws Exception {
        String shift = openShift();
        String table = table("vipr");
        openSession("vipr", table, null);
        String order = orderId(order(table, highWine, 1));
        bill(order, "r1");
        deliver(order).andExpect(status().isOk());
        suspend(order, "客人稍后付款").andExpect(status().isOk());
        String photo = upload("现场照片.jpg", "image/jpeg");
        String video = upload("现场视频.mp4", "video/mp4");
        String first = data(badDebt(manager, order, "客人失联", "bd-r1-" + s, List.of(photo), List.of(video)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("hanging_unpaid")), "$.data.bad_debt_review_id");

        decide(reviewer, first, "rejected").andExpect(status().isOk());
        Map<String, Object> row = jdbc.queryForMap("SELECT id, status, bad_debt_review_id, bad_debt_reason FROM bar_order_suspensions WHERE order_id = ?",
                order);
        assertThat(row).containsEntry("status", "hanging_unpaid").containsEntry("bad_debt_review_id", null).containsEntry("bad_debt_reason", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.order_suspension_bad_debt_review_rejected'"
                + " AND business_id = ? AND metadata->>'bad_debt_review_id' = ?", Integer.class, row.get("id"), first)).isEqualTo(1);

        String second = data(badDebt(manager, order, "仍联系不上", "bd-r2-" + s, List.of(photo), List.of(video)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("hanging_unpaid")), "$.data.bad_debt_review_id");
        assertThat(second).isNotEqualTo(first);
        close(shift).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_SUSPENDED_ORDER_UNSETTLED"));
    }

    /** 157-R4：缺视频 400 且不建复核单、挂起不变；待复核期间再发起 409；发起经理自审 409 HIGH_VALUE_REVIEW_SELF_APPROVAL、仍待复核。 */
    @Test
    void highValueBadDebtNeedsBothEvidencesAndNoSelfApproval() throws Exception {
        String table = table("vipe");
        openSession("vipe", table, null);
        String order = orderId(order(table, highWine, 1));
        bill(order, "e1");
        String other = orderId(order(table, highWine, 1));
        bill(other, "e2");
        deliver(order).andExpect(status().isOk());
        deliver(other).andExpect(status().isOk());
        suspend(order, "客人稍后付款").andExpect(status().isOk());
        suspend(other, "客人稍后付款").andExpect(status().isOk());
        String photo = upload("现场照片.jpg", "image/jpeg");
        String video = upload("现场视频.mp4", "video/mp4");
        String review = data(badDebt(manager, order, "客人失联", "bd-e1-" + s, List.of(photo), List.of(video)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("hanging_unpaid")), "$.data.bad_debt_review_id");

        badDebt(manager, other, "客人失联", "bd-e2-" + s, List.of(photo), List.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("HIGH_VALUE_EVIDENCE_REQUIRED"));
        assertThat(jdbc.queryForMap("SELECT status, bad_debt_review_id FROM bar_order_suspensions WHERE order_id = ?", other))
                .containsEntry("status", "hanging_unpaid").containsEntry("bad_debt_review_id", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_high_value_reviews WHERE subject_id = ?", Integer.class, other)).isZero();
        badDebt(manager, order, "再提交一次", "bd-e3-" + s, List.of(photo), List.of(video)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_BAD_DEBT_REVIEW_PENDING"));
        decide(manager, review, "approved").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("HIGH_VALUE_REVIEW_SELF_APPROVAL"));
        assertThat(jdbc.queryForObject("SELECT status FROM bar_order_suspensions WHERE order_id = ?", String.class, order))
                .isEqualTo("hanging_unpaid");
        assertThat(jdbc.queryForObject("SELECT review_status FROM inventory_high_value_reviews WHERE id = ?", String.class, review))
                .isEqualTo("pending");
    }

    /**
     * 18T 复核修复②（BSM:1109「经理确认最终无法收款后转为坏账损耗处理中……经理未及时确认时，任务升级给店长或夜班值班经理」；BSM:1107「经理审批不能替代既有
     * 高价值审批链」）：包厢高价值单挂起、关台未付清待办已过期限；当班经理已带照片与视频提交转坏账、正等高价值主管复核——升级作业不升级（不属于经理未及时确认）；
     * 复核驳回、待复核标记清空后，下一轮照常升级。
     */
    @Test
    void escalationSkipsBadDebtAwaitingHighValueReviewUntilRejected() throws Exception {
        String table = table("vipx");
        String session = openSession("vipx", table, null);
        String order = orderId(order(table, highWine, 1));
        String bill = bill(order, "x1");
        deliver(order).andExpect(status().isOk());
        suspend(order, "客人称明天来付").andExpect(status().isOk());
        // 包厢关台被拦时登记的待办（关台在独立事务里登记，测试事务内直接写同一行），已过处理期限。
        jdbc.update("INSERT INTO bar_bill_unpaid_close_todos(bill_id,bar_id,merchant_id,project_id,session_id,notified_at,due_at)"
                + " VALUES(?,?,?,?,?,now() - interval '2 days',now() - interval '1 minute')", bill, bar, bar, project, session);
        String photo = upload("现场照片.jpg", "image/jpeg");
        String video = upload("现场视频.mp4", "video/mp4");
        String review = data(badDebt(manager, order, "客人失联，确认无法收回", "bd-x-" + s, List.of(photo), List.of(video))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("hanging_unpaid")), "$.data.bad_debt_review_id");

        escalation.escalateOverdue(Instant.now());
        assertThat(escalatedAt(bill)).as("经理已提交转坏账、等高价值复核时不升级").isNull();

        decide(reviewer, review, "rejected").andExpect(status().isOk());
        escalation.escalateOverdue(Instant.now());
        assertThat(escalatedAt(bill)).as("复核驳回后照常升级").isNotNull();
    }

    /**
     * 18T 复核修复⑤（BSM:1111「财务确认未收售价、库存成本和赔偿记录均完成后，订单转为已核销结案。」）：项目侧财务账号（持 finance.ar_ap.manage 与订单列表入口码
     * bar.order.view、无商户号）打开后台订单页即可读订单列表（按 bar.order.view 可触及的项目），列出本项目的坏账订单并办成核销结案；别的综合体的同类账号看不到。
     */
    @Test
    void projectFinanceListsOwnProjectOrdersAndWritesOff() throws Exception {
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor projectFinance = staffs.staff("usr_bd_finview_" + s, "staff", project, null, "finance.ar_ap.manage",
                "bar.order.view");
        RequestPostProcessor otherFinance = staffs.staff("usr_bd_finview_other_" + s, "staff", "prj_bd_other_" + s, null,
                "finance.ar_ap.manage", "bar.order.view");
        String table = table("vipf");
        openSession("vipf", table, null);
        String order = orderId(order(table, 1));
        bill(order, "f1");
        suspend(order, "客人稍后付款").andExpect(status().isOk());
        badDebt(manager, order, "客人失联", "bd-f-" + s).andExpect(status().isOk());

        assertThat(listed(projectFinance)).containsEntry(order, "bad_debt_processing");
        assertThat(listed(otherFinance)).doesNotContainKey(order);
        writeOff(projectFinance, order, "bd-f-wo-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("written_off_closed"));
        assertThat(listed(projectFinance)).containsEntry(order, "written_off_closed");
    }

    // ---------- helpers ----------

    private String table(String tag) {
        String id = "table_bd_" + tag + "_" + s;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(id, bar, bar, "T-" + tag + "-" + s, tag, "vip", 4, 0, 0L, "CNY", "opened", now, now));
        bars.assignTableSpace(id, "space_bd_" + s);
        return id;
    }

    private String openSession(String tag, String table, String serviceStaff) {
        Instant now = Instant.now();
        String id = "session_bd_" + tag + "_" + s;
        bars.saveTableSession(new BarTableSessionRecord(id, bar, bar, LocalDate.now(), table, null, null, 2,
                0L, 0, null, serviceStaff, "walk_in", "open", now, null, null, 1L, now, now));
        return id;
    }

    /** 开台会话此刻的主责服务员区间（开台确认责任人）。 */
    private void responsible(String session, String table, String userId) {
        jdbc.update("INSERT INTO bar_service_responsibilities(id,session_id,table_id,project_id,merchant_id,user_id,payer_company_id,"
                + "started_at,source_id,recorded_by,reason) VALUES(?,?,?,?,?,?,?,now() - interval '1 hour',?,?,'开台确认责任人')",
                "bsr_bd_" + s, session, table, project, bar, userId, "ent_bd_" + s, "open:" + session, cashierId);
    }

    private ResultActions order(String table, int quantity) throws Exception {
        return order(table, wine, quantity);
    }

    private ResultActions order(String table, String product, int quantity) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bd-order-" + UUID.randomUUID());
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", product, "quantity", quantity)));
        return mockMvc.perform(post("/api/v1/bar/orders").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private String bill(String order, String tag) throws Exception {
        return data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bd-bill-" + tag + "-" + s)))).andExpect(status().isOk()),
                "$.data.id");
    }

    private ResultActions deliver(String order) throws Exception {
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ?", order).stream()
                .map(row -> Map.<String, Object>of("order_item_id", row.get("id"), "quantity", row.get("quantity"))).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bd-deliver-" + order);
        body.put("operator_note", "已送达");
        body.put("delivery_photo_attachment_ids", List.of());
        body.put("delivered_items", lines);
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 卡座页「服务交接」：体 idempotency_key、next_user_id、session_version、reason。 */
    private ResultActions handover(String session, String next) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bd-handover-" + s);
        body.put("next_user_id", next);
        body.put("session_version", version(session));
        body.put("reason", "换班交接");
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/service-handover").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 卡座页「关台」：体 idempotency_key、session_version、reason。 */
    private ResultActions serviceClose(String session) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bd-svc-close-" + session);
        body.put("session_version", version(session));
        body.put("reason", "客人离开");
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/service-close").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions suspend(String order, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspend").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bd-hang-" + order, "reason", reason))));
    }

    /** 后台订单页「确认无法收款（转坏账）」：体 reason、idempotency_key。 */
    private ResultActions badDebt(RequestPostProcessor who, String order, String reason, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspension/bad-debt").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", key, "reason", reason))));
    }

    /** 157-R4 后台订单页转坏账带现场证据：体 reason、idempotency_key、photo_attachment_ids、video_attachment_ids。 */
    private ResultActions badDebt(RequestPostProcessor who, String order, String reason, String key, List<String> photos, List<String> videos)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("reason", reason);
        body.put("photo_attachment_ids", photos);
        body.put("video_attachment_ids", videos);
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspension/bad-debt").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    /** 转坏账弹窗上传现场照片、视频（POST /api/v1/evidence/attachments/upload，business_type=bar_order_bad_debt）。 */
    private String upload(String name, String contentType) throws Exception {
        return data(mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                .file(new MockMultipartFile("file", name, contentType, "现场证据".getBytes(StandardCharsets.UTF_8)))
                .param("business_type", "bar_order_bad_debt").param("attachment_role", contentType.startsWith("video") ? "video" : "photo")
                .with(manager)).andExpect(status().isOk()), "$.data.id");
    }

    /** 后台高价值复核页「批准」「驳回」：体 idempotency_key、decision、reason。 */
    private ResultActions decide(RequestPostProcessor who, String review, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/high-value-reviews/" + review + "/review").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bd-rv-" + UUID.randomUUID(), "decision", decision,
                        "reason", "核对现场照片与视频"))));
    }

    /** 采购到货流水（高价值判定只看最近一次实际到货价）。 */
    private void arrival(String id, String itemId, String location, long unitCostCent) {
        jdbc.update("INSERT INTO inventory_stock_movements (id, movement_type, item_id, target_location_id, quantity, business_type, status,"
                + " created_at, merchant_id, project_id, unit_cost_cent, total_cost_cent) VALUES (?, 'inbound', ?, ?, 1, 'purchase_request', 'completed',"
                + " now() - interval '1 day', ?, ?, ?, ?)", id, itemId, location, bar, project, unitCostCent, unitCostCent);
    }

    /** 后台订单页「财务确认核销结案」：体 idempotency_key。 */
    private ResultActions writeOff(RequestPostProcessor who, String order, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspension/write-off").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", key))));
    }

    private ResultActions settle(String bill, long amountCent) throws Exception {
        String key = "bd-settle-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", amountCent);
        body.put("payment_channel", "cash");
        return mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/settle").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String openShift() throws Exception {
        return data(mockMvc.perform(post("/api/v1/bar/shifts/open").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bd-open-" + s, "bar_id", bar,
                        "cashier_user_id", cashierId, "opening_cash_cent", 0)))).andExpect(status().isOk()), "$.data.id");
    }

    private ResultActions close(String shift) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/shifts/" + shift + "/close").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bd-close-shift-" + s, "declared_cash_cent", 0))));
    }

    /** 关台未付清待办的升级时间（未升级为空）。 */
    private Object escalatedAt(String bill) {
        return jdbc.queryForObject("SELECT escalated_at FROM bar_bill_unpaid_close_todos WHERE bill_id = ?", Object.class, bill);
    }

    /** 后台订单页读订单列表（GET /api/v1/bar/orders）：订单号 → 挂起状态。 */
    private Map<String, String> listed(RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(get("/api/v1/bar/orders").with(who)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        Map<String, String> rows = new LinkedHashMap<>();
        json.readTree(body).path("data").path("items").forEach(row -> rows.put(row.path("id").asText(), row.path("suspension_status").asText()));
        return rows;
    }

    private Long version(String session) {
        return jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id = ?", Long.class, session);
    }

    private String orderId(ResultActions result) throws Exception {
        return data(result.andExpect(status().isOk()), "$.data.id");
    }

    private static String data(ResultActions result, String path) throws Exception {
        return CustomerTicketPaymentFixture.read(result, path);
    }
}
