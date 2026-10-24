package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.bar.BarPayrollRequests;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03 clients-R9 客人端只读展示（BSM 第 857 行「查看综合体介绍、营业时间、活动公告、票务入口」；第 858 行「购票前须以门票守则的形式向客人告知
 * 该票的规则，包括能否退款、有效期、年龄限制与可使用范围……订卡时在订卡规则中写明」；第 860 行「订台页面须写明该卡座适用的最低消费金额与服务费比例」；
 * 第 878 行「展示可预定项目和场次」；第 879 行「展示酒吧规则和赠饮权益」）与第十三批裁决第 5 条（首页综合体介绍与活动公告「运营编辑后走审批，批准才对
 * 客人展示」）。客人端与后台请求一律按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerShowcaseJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;
    private StaffRequestFixture staffs;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        staffs = new StaffRequestFixture(jdbc);
    }

    @Test
    void projectsListsVenueBusinessHours() throws Exception {
        String venue = "space_show_" + f.suffix;
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '主场馆', 'venue', 'active', now(), now())", venue, f.projectId);
        jdbc.update("INSERT INTO venue_business_hours (id, venue_id, day_of_week, open_time, close_time, is_draft) VALUES (?, ?, 5, ?, ?, FALSE)",
                "vbh_show_" + f.suffix, venue, LocalTime.of(20, 0), LocalTime.of(2, 0));
        String project = "$.data[?(@.project_id == '" + f.projectId + "')]";
        anonymous("/api/v1/customer/projects").andExpect(status().isOk())
                .andExpect(jsonPath(project + ".project_name").value(contains("支付组测试综合体")))
                .andExpect(jsonPath(project + ".business_hours[0].venue_name").value(contains("主场馆")))
                .andExpect(jsonPath(project + ".business_hours[0].day_of_week").value(contains(5)))
                .andExpect(jsonPath(project + ".business_hours[0].open_time").value(contains("20:00")))
                .andExpect(jsonPath(project + ".business_hours[0].close_time").value(contains("02:00")))
                .andExpect(jsonPath(project + ".intro").value(contains(nullValue())));
    }

    @Test
    void homeContentShownOnlyAfterApproval() throws Exception {
        String project = "$.data[?(@.project_id == '" + f.projectId + "')]";
        anonymous("/api/v1/customer/projects").andExpect(status().isOk()).andExpect(jsonPath(project + ".intro").value(contains(nullValue())));
        String editorId = "usr_show_ed_" + f.suffix;
        String supervisorId = "usr_show_sup_" + f.suffix;
        RequestPostProcessor editor = staffs.staff(editorId, "staff", f.projectId, null, "customer.home_content.manage");
        RequestPostProcessor supervisor = staffs.staff(supervisorId, "staff", f.projectId, null,
                "governance.supervisor_review.view", "governance.supervisor_review.approve", "governance.supervisor_review.manage");
        // 一级上级＝编辑人的直接上级（组织层级）。
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", editorId, supervisorId);
        Map<String, Object> body = Map.of("intro", "城市娱乐综合体，含酒吧与游乐。", "announcement", "本周六晚有乐队演出。");
        String blocked = publish(editor, body).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED")).andReturn().getResponse().getContentAsString();
        anonymous("/api/v1/customer/projects").andExpect(jsonPath(project + ".intro").value(contains(nullValue())));
        List<String> pending = jdbc.queryForList("SELECT review_id FROM supervisor_review_decisions WHERE action_type = 'customer.home_content.publish'"
                + " AND object_id = ? AND review_status = 'pending_review'", String.class, f.projectId).stream().filter(blocked::contains).toList();
        assertThat(pending).hasSize(1);
        // 裁决第 5 条「运营编辑后走审批」：审批人审批前能读到这次要发布的原文（不是盲批）。
        mockMvc.perform(get("/api/v1/home-contents/" + f.projectId).with(supervisor)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draft_intro").value("城市娱乐综合体，含酒吧与游乐。"))
                .andExpect(jsonPath("$.data.draft_announcement").value("本周六晚有乐队演出。"))
                .andExpect(jsonPath("$.data.intro").value(nullValue()));
        // 复核单在独立事务里建立并提交（AdmissionGuard XM-M002-J），看不到本例事务里才补的组织层级：按「层级补齐后重生成」的真实接口补出审批链，
        // 再由一级上级经真实审批接口批准。
        mockMvc.perform(post("/api/v1/platform/supervisor-review/" + pending.get(0) + "/chain/regenerate").with(supervisor))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/platform/supervisor-review/" + pending.get(0) + "/approve").with(supervisor)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "show-approve-" + f.suffix, "decision", "approved", "comment", "内容已核"))))
                .andExpect(status().isOk());
        publish(editor, body).andExpect(status().isOk()).andExpect(jsonPath("$.data.intro").value("城市娱乐综合体，含酒吧与游乐。"));
        anonymous("/api/v1/customer/projects").andExpect(status().isOk())
                .andExpect(jsonPath(project + ".intro").value(contains("城市娱乐综合体，含酒吧与游乐。")))
                .andExpect(jsonPath(project + ".announcement").value(contains("本周六晚有乐队演出。")));
        // 内容改了再提交：仍被拦下送审，客人端仍是已批准的那份。
        publish(editor, Map.of("intro", "城市娱乐综合体（新版介绍）。", "announcement", "")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        anonymous("/api/v1/customer/projects").andExpect(jsonPath(project + ".intro").value(contains("城市娱乐综合体，含酒吧与游乐。")));
    }

    @Test
    void ticketProductsCarryRulesFields() throws Exception {
        String product = "$.data.items[?(@.ticket_type_id == '" + f.ticketTypeId + "')]";
        anonymous("/api/v1/customer/ticket-products").andExpect(status().isOk())
                .andExpect(jsonPath(product + ".refundable").value(contains(false)))
                .andExpect(jsonPath(product + ".validity_days").value(contains(1)))
                .andExpect(jsonPath(product + ".usage_scope").value(contains("综合体通票")))
                .andExpect(jsonPath(product + ".entitlements[*]").value(hasItem("游乐项目")))
                .andExpect(jsonPath(product + ".age_limit_text").value(contains("全场 18 岁以上方可入场")));
    }

    @Test
    void reservationOptionsCarryServiceFeeForCardSeatOnly() throws Exception {
        String bar = "bar_show_" + f.suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '展示酒吧', 'bar', 'active', now(), now(), ?)", bar, f.projectId);
        String cardSeat = barTable(bar, "card", "card_seat", 50000);
        String scatter = barTable(bar, "scatter", "scatter", 0);
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, ratio_bps, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'min_consumption_ratio', 2000, 0, TRUE, now(), now())",
                "bar_dep_show_" + f.suffix, bar, bar);
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 10%', 'approved', 'company_show', 'usr_show')
                """, "bpr_show_" + f.suffix, f.projectId, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(1000, 0, 2000, null, null, null, List.of())));
        mockMvc.perform(get("/api/v1/customer/bar-reservation-options").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", f.token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.table_id == '" + cardSeat + "')].service_fee_rate_bps").value(contains(1000)))
                .andExpect(jsonPath("$.data.items[?(@.table_id == '" + cardSeat + "')].min_consumption_cent").value(contains(50000)))
                .andExpect(jsonPath("$.data.items[?(@.table_id == '" + cardSeat + "')].deposit_amount_cent").value(contains(10000)))
                .andExpect(jsonPath("$.data.items[?(@.table_id == '" + scatter + "')].service_fee_rate_bps").value(contains(0)));
    }

    @Test
    void amusementProjectsReadOnlyForCustomer() throws Exception {
        String ride = "ap_show_" + f.suffix;
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, eligibility_rule_json, merchant_id, created_at, updated_at)"
                + " VALUES (?, '旋转木马', 20, 'open', '{}'::jsonb, ?, now(), now())", ride, f.merchantId);
        jdbc.update("INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity, status, created_at, updated_at)"
                + " VALUES (?, ?, now() + interval '1 hour', now() + interval '2 hours', 20, 3, 'scheduled', now(), now())",
                "as_show_" + f.suffix, ride);
        String item = "$.data[?(@.amusement_project_id == '" + ride + "')]";
        mockMvc.perform(get("/api/v1/customer/amusement/projects").param("project_id", f.projectId).header("X-Terminal-Type", "miniapp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(item + ".project_name").value(contains("旋转木马")))
                .andExpect(jsonPath(item + ".sessions[0].capacity").value(contains(20)))
                .andExpect(jsonPath(item + ".sessions[0].used_capacity").value(contains(3)));
    }

    // ---------- helpers ----------

    private ResultActions anonymous(String path) throws Exception {
        return mockMvc.perform(get(path).header("X-Terminal-Type", "miniapp"));
    }

    private ResultActions publish(RequestPostProcessor who, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        actual.put("idempotency_key", "show-pub-" + UUID.randomUUID());
        return mockMvc.perform(post("/api/v1/home-contents/" + f.projectId + "/publish").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(actual)));
    }

    private String barTable(String bar, String tag, String type, long minConsumption) {
        String id = "table_show_" + tag + "_" + f.suffix;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 6, 0, ?, 'CNY', 'active', now(), now())",
                id, bar, "T-" + tag + "-" + f.suffix, tag, type, minConsumption);
        return id;
    }
}
