package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 T 段第 157 条 157-R5（真库）：第十八批裁决 37 选项原文「包厢关台时没付清就发经理待办，超时按通用时限升级」；BSM:1058「账单未结清（含挂起未结清）
 * 不得关台」；BSM:1109「经理未及时确认时，任务升级给店长或夜班值班经理，在转入该状态前继续阻断」；PGM:529「涉钱审批出厂默认 1 个工作日」。
 * 不开测试事务：待办在关台被拦的独立事务里写，要读得到已提交的账号、排班与账单（ID 后缀用纯数字）。请求按后台卡座页、订单页真实形状（蛇形键、X-Terminal-Type: web）
 * 经控制器与真实授权链提交。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class BarUnpaidCloseTodoJdbcIntegrationTests {

    private static final String TODO_TITLE = "包厢客人离开未付清：请确认能否收回";
    private static final String ESCALATED_TITLE = "包厢未付清待办已超时：请确认能否收回";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private BarUnpaidCloseEscalationJob job;

    private String s;
    private String project;
    private String bar;
    private String company;
    private String wine;
    private StaffRequestFixture staffs;
    private RequestPostProcessor cashier;
    private final Instant now = Instant.now();

    @BeforeEach
    void seed() throws Exception {
        s = Long.toString(System.nanoTime());
        project = "prj_uc_" + s;
        company = "ent_uc_" + s;
        bar = "bar_uc_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','关台待办综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'关台待办酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'包厢区','unit','active',now(),now())",
                "space_uc_" + s, project, bar);
        // 开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_uc')
                """, "bpr_uc_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        wine = "product_uc_" + s;
        bars.saveProduct(new BarProductRecord(wine, bar, bar, "干红", "wine", 30000L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        staffs = new StaffRequestFixture(jdbc);
        cashier = staffs.staff("usr_uc_cashier_" + s, "staff", project, bar, "bar.order.create", "finance.payment.register", "bar.bill.hang",
                "bar.table.open_close");
    }

    /**
     * 包厢客人离开时一张未挂起、一张已挂起的账单没付清：关台照拦（409、台位仍开台），同一刻给此刻有排班、持 bar.bill.hang_cancel 的本店经理发一条待办
     * （列包厢、账单号、未付金额、挂起与否，期限＝涉钱审批通用时限）；无排班经理、他店经理、无该码在班员工不收；再次关台不重发、不改期限。卡座只拦不发。
     */
    @Test
    void vipCloseStaysBlockedAndNotifiesOnDutyManagerOnce() throws Exception {
        String onDuty = profile("usr_uc_mgr_" + s, bar, "bar.bill.hang_cancel");
        shift("usr_uc_mgr_" + s);
        String offShift = profile("usr_uc_mgr_off_" + s, bar, "bar.bill.hang_cancel");
        String otherBar = "bar_uc_other_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'他店酒吧','bar',?,?,?,?,'active',now(),now())", otherBar, company, company, company, project);
        String otherManager = profile("usr_uc_mgr_other_" + s, otherBar, "bar.bill.hang_cancel");
        shift("usr_uc_mgr_other_" + s);
        String noPermission = profile("usr_uc_waiter_" + s, bar, "bar.order.view");
        shift("usr_uc_waiter_" + s);

        String table = table("vip", "vip");
        String session = openSession("vip", table);
        String unhung = order(table);
        String unhungBill = bill(unhung, "a");
        String hung = order(table);
        String hungBill = bill(hung, "b");
        suspend(hung, "客人称明天来付").andExpect(status().isOk());

        serviceClose(session, "uc-close-1-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_SESSION_BILL_UNSETTLED"))
                .andExpect(jsonPath("$.message").value(endsWith("已通知当班经理确认能否收回")));
        assertThat(jdbc.queryForObject("SELECT status FROM bar_table_sessions WHERE id = ?", String.class, session)).isEqualTo("open");

        List<String> bodies = jdbc.queryForList("SELECT body FROM employee_notices WHERE staff_profile_id = ? AND title = ?", String.class,
                onDuty, TODO_TITLE);
        assertThat(bodies).hasSize(1);
        assertThat(bodies.getFirst()).contains("包厢 包厢vip", billNo(unhungBill), billNo(hungBill), "未付 300.00 元",
                "未挂起：须先由收银员挂起登记原因", "已挂起（原因：客人称明天来付；责任人：门店）", "处理期限");
        for (String profile : List.of(offShift, otherManager, noPermission)) {
            assertThat(notices(profile, TODO_TITLE)).as(profile).isZero();
        }
        Map<String, Object> todo = todo(unhungBill);
        Instant notifiedAt = ((Timestamp) todo.get("notified_at")).toInstant();
        Timestamp due = (Timestamp) todo.get("due_at");
        assertThat(due.toInstant()).isEqualTo(reviews.moneyApprovalDeadline(notifiedAt));
        assertThat(todo).containsEntry("session_id", session).containsEntry("project_id", project).containsEntry("escalated_at", null);
        assertThat(todo(hungBill)).containsEntry("due_at", due);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.bill_unpaid_close_notified' AND business_id = ?",
                Integer.class, session)).isEqualTo(1);

        serviceClose(session, "uc-close-2-" + s).andExpect(status().isConflict());
        assertThat(notices(onDuty, TODO_TITLE)).isEqualTo(1);
        assertThat(todo(unhungBill)).containsEntry("due_at", due);

        String card = table("card", "card_seat");
        String cardSession = openSession("card", card);
        String cardBill = bill(order(card), "c");
        serviceClose(cardSession, "uc-close-card-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_SESSION_BILL_UNSETTLED"))
                .andExpect(jsonPath("$.message").value(not(endsWith("已通知当班经理确认能否收回"))));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_bill_unpaid_close_todos WHERE bill_id = ?", Integer.class, cardBill)).isZero();
    }

    /**
     * 到期前不升级；到期仍未付清的升级一次，给本店持 bar.bill.hang_escalation 的店长与本项目此刻在班的夜班值班经理（顶班人，非原班人）各发一条，重跑不重发；
     * 到期前结清的不升级。升级前夜班值班经理（无 bar.bill.hang_cancel）转坏账 403，升级后 200；未挂起的 409，收银员挂起后店长办理 200，随后关台 200。
     */
    @Test
    void overdueTodoEscalatesOnceAndEscalatedManagersMayConfirmBadDebt() throws Exception {
        String storeManagerId = "usr_uc_store_" + s;
        String storeManagerProfile = profile(storeManagerId, bar, "bar.bill.hang_escalation");
        RequestPostProcessor storeManager = staffs.staff(storeManagerId, "staff", project, bar);
        String nightOriginal = "usr_uc_night_" + s;
        String nightCover = "usr_uc_cover_" + s;
        String nightOriginalProfile = profile(nightOriginal, null);
        String nightCoverProfile = profile(nightCover, null);
        RequestPostProcessor night = staffs.staff(nightCover, "staff", project, null);
        String nightShift = "shift_uc_night_" + s;
        String coverShift = "shift_uc_cover_" + s;
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,project_id,night_duty_project_id) VALUES(?,?,?,?,?,?,?)",
                nightShift, nightOriginal, LocalDate.now(), Timestamp.from(now.minusSeconds(7200)), Timestamp.from(now.plusSeconds(7200)), project, project);
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,project_id) VALUES(?,?,?,?,?,?)",
                coverShift, nightCover, LocalDate.now(), Timestamp.from(now.minusSeconds(3600)), Timestamp.from(now.plusSeconds(3600)), project);
        jdbc.update("""
                INSERT INTO staff_shift_covers (id, shift_id, original_user_id, cover_user_id, arranged_by, reason, start_at, end_at,
                    handover_note, cover_shift_id, source_updated_at)
                VALUES (?, ?, ?, ?, 'usr_uc_lead', '临时顶班', ?, ?, '交接夜班', ?, now())""",
                "cov_uc_" + s, nightShift, nightOriginal, nightCover, Timestamp.from(now.minusSeconds(3600)), Timestamp.from(now.plusSeconds(3600)),
                coverShift);

        String table = table("vip", "vip");
        String session = openSession("vip", table);
        String hung = order(table);
        String hungBill = bill(hung, "a");
        suspend(hung, "客人称明天来付").andExpect(status().isOk());
        String unhung = order(table);
        String unhungBill = bill(unhung, "b");
        String paidTable = table("paid", "vip");
        String paidSession = openSession("paid", paidTable);
        String paidBill = bill(order(paidTable), "c");
        serviceClose(session, "uc-close-1-" + s).andExpect(status().isConflict());
        serviceClose(paidSession, "uc-close-paid-" + s).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_bill_unpaid_close_todos WHERE bill_id IN (?, ?, ?)", Integer.class,
                hungBill, unhungBill, paidBill)).isEqualTo(3);

        job.escalateOverdue(Instant.now());
        assertThat(notices(storeManagerProfile, ESCALATED_TITLE)).isZero();
        assertThat(todo(hungBill)).containsEntry("escalated_at", null);
        badDebt(night, hung, "uc-bd-early-" + s).andExpect(status().isForbidden());
        RequestPostProcessor viewer = staffs.staff("usr_uc_viewer_" + s, "staff", project, bar, "bar.order.view");
        assertThat(listedEscalatedAt(viewer, hung)).isNull();

        settle(paidBill, 30000L).andExpect(status().isOk());
        jdbc.update("UPDATE bar_bill_unpaid_close_todos SET due_at = now() - interval '1 minute' WHERE bill_id IN (?, ?, ?)",
                hungBill, unhungBill, paidBill);
        job.escalateOverdue(Instant.now());
        job.escalateOverdue(Instant.now());

        assertThat(notices(storeManagerProfile, ESCALATED_TITLE)).isEqualTo(2);
        assertThat(notices(nightCoverProfile, ESCALATED_TITLE)).isEqualTo(2);
        assertThat(notices(nightOriginalProfile, ESCALATED_TITLE)).isZero();
        assertThat(jdbc.queryForList("SELECT body FROM employee_notices WHERE staff_profile_id = ? AND title = ?", String.class,
                nightCoverProfile, ESCALATED_TITLE)).anySatisfy(body -> assertThat(body).contains(billNo(hungBill), "未付 300.00 元"));
        assertThat(todo(hungBill).get("escalated_at")).isNotNull();
        assertThat(todo(unhungBill).get("escalated_at")).isNotNull();
        assertThat(todo(paidBill)).containsEntry("escalated_at", null);
        // 后台订单页按订单列表的 unpaid_close_escalated_at 对已升级的单放开「确认无法收款（转坏账）」按钮（能否办理仍由后端按排班鉴权）。
        assertThat(listedEscalatedAt(viewer, hung)).isNotNull();
        assertThat(listedEscalatedAt(viewer, unhung)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.bill_unpaid_close_escalated' AND business_id = ?",
                Integer.class, hungBill)).isEqualTo(1);

        badDebt(night, hung, "uc-bd-1-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("bad_debt_processing"))
                .andExpect(jsonPath("$.data.bad_debt_confirmed_by").value(nightCover));
        badDebt(night, unhung, "uc-bd-2-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_NOT_HANGING_UNPAID"));
        suspend(unhung, "客人失联").andExpect(status().isOk());
        badDebt(storeManager, unhung, "uc-bd-3-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bad_debt_confirmed_by").value(storeManagerId));
        serviceClose(session, "uc-close-2-" + s).andExpect(status().isOk());
    }

    // ---------- helpers ----------

    /** 本店（或他店、或不挂商户）在用员工：账号、角色与员工档案；返回档案号。 */
    private String profile(String userId, String merchant, String... permissions) {
        staffs.staff(userId, "staff", project, merchant, permissions);
        String id = "sp_" + userId;
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,registration_status,status,created_at,updated_at)"
                + " VALUES(?,?,?,?,'员工','approved','active',now(),now())", id, project, merchant, userId);
        return id;
    }

    /** 此刻有排班。 */
    private void shift(String userId) {
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,project_id) VALUES(?,?,?,?,?,?)",
                "shift_" + userId, userId, LocalDate.now(), Timestamp.from(now.minusSeconds(7200)), Timestamp.from(now.plusSeconds(7200)), project);
    }

    private int notices(String profile, String title) {
        return jdbc.queryForObject("SELECT count(*) FROM employee_notices WHERE staff_profile_id = ? AND title = ?", Integer.class, profile, title);
    }

    private Map<String, Object> todo(String bill) {
        return jdbc.queryForMap("SELECT * FROM bar_bill_unpaid_close_todos WHERE bill_id = ?", bill);
    }

    private String billNo(String bill) {
        return jdbc.queryForObject("SELECT bill_no FROM bar_bills WHERE id = ?", String.class, bill);
    }

    private String table(String tag, String type) {
        String id = "table_uc_" + tag + "_" + s;
        Instant at = Instant.now();
        bars.saveTable(new BarTableRecord(id, bar, bar, "T-" + tag + "-" + s, "包厢" + tag, type, 4, 0, 0L, "CNY", "opened", at, at));
        bars.assignTableSpace(id, "space_uc_" + s);
        return id;
    }

    private String openSession(String tag, String table) {
        Instant at = Instant.now();
        String id = "session_uc_" + tag + "_" + s;
        bars.saveTableSession(new BarTableSessionRecord(id, bar, bar, LocalDate.now(), table, null, null, 2,
                0L, 0, null, null, "walk_in", "open", at, null, null, 1L, at, at));
        return id;
    }

    private String order(String table) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "uc-order-" + UUID.randomUUID());
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", wine, "quantity", 1)));
        return data(mockMvc.perform(post("/api/v1/bar/orders").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk()), "$.data.id");
    }

    private String bill(String order, String tag) throws Exception {
        return data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "uc-bill-" + tag + "-" + s)))).andExpect(status().isOk()),
                "$.data.id");
    }

    /** 卡座页「关台」：体 idempotency_key、session_version、reason。 */
    private ResultActions serviceClose(String session, String key) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("session_version", jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id = ?", Long.class, session));
        body.put("reason", "客人离开");
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/service-close").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions suspend(String order, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspend").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "uc-hang-" + order + "-" + UUID.randomUUID(), "reason", reason))));
    }

    /** 后台订单页「确认无法收款（转坏账）」：体 reason、idempotency_key。 */
    private ResultActions badDebt(RequestPostProcessor who, String order, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspension/bad-debt").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", key, "reason", "客人失联，确认无法收回"))));
    }

    /** 后台订单页读订单列表（GET /api/v1/bar/orders）：该单的关台未付清待办升级时间，未升级为空。 */
    private String listedEscalatedAt(RequestPostProcessor who, String order) throws Exception {
        String body = mockMvc.perform(get("/api/v1/bar/orders").with(who)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        for (var item : json.readTree(body).path("data").path("items")) {
            if (order.equals(item.path("id").asText())) {
                return item.hasNonNull("unpaid_close_escalated_at") ? item.get("unpaid_close_escalated_at").asText() : null;
            }
        }
        throw new AssertionError("订单列表里没有 " + order);
    }

    private ResultActions settle(String bill, long amountCent) throws Exception {
        String key = "uc-settle-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", amountCent);
        body.put("payment_channel", "cash");
        return mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/settle").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private static String data(ResultActions result, String path) throws Exception {
        return CustomerTicketPaymentFixture.read(result, path);
    }
}
