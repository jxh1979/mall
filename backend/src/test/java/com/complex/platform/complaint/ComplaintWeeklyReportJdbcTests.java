package com.complex.platform.complaint;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.LocalZone;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 111 条 111-R4 客诉周报（真库＋真实入口＋真实授权链，请求体一律按前端蛇形键）。
 * BSM:1385「系统每周自动生成客诉周报，列明当周客诉的笔数、类型、处理进度与闭环情况——店内的报送该店主理人与店长，各公司的报送该公司最高负责人与
 * 平台最高负责人」「投诉分为商户投诉和消费客人投诉」；第十八批裁决 28「客人投诉时自己选涉及的门店（选填），客户关怀部可以更正；标了门店的客人投诉，
 * 加上该店自己在商户专区提的投诉，进该店周报；全部客诉按所在项目进各公司周报」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "evidence.attachment.dir=/tmp/complex-complaint-weekly-test"
})
@AutoConfigureMockMvc
@Transactional
class ComplaintWeeklyReportJdbcTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FOLLOWUPS = "/api/v1/complaint-followups/";
    private static final String MERCHANT_WORK_ORDERS = "/api/v1/merchant-service/work-orders";
    private static final String RECEIVE = "complaint.weekly_report.receive";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;
    @Autowired private ComplaintWeeklyReportService reports;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String store = "mch_cwr_" + suffix;
    private final String closedStore = "mch_cwr_t_" + suffix;
    private RequestPostProcessor storeOwner;
    private RequestPostProcessor care;
    private RequestPostProcessor processor;

    /**
     * 项目 A、B；门店 M（主项目 A，在营）与已终止的门店 T（主项目 A）。M 的店长账号 m 与主理人账号 p 持接收码（商户账号）；
     * 平台员工 c 持接收码只到项目 A、x 持接收码只到项目 B；客户关怀部员工 k 与处理员 w 只到项目 A。
     */
    @BeforeEach
    void seed() {
        for (String tag : List.of("a", "b")) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                    project(tag), "客诉周报测试综合体" + tag);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'retail', 'active', now(), now(), ?)", store, "客诉周报门店" + suffix, project("a"));
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'retail', 'terminated', now(), now(), ?)", closedStore, "已终止门店" + suffix, project("a"));
        storeOwner = account("m", project("a"), store, RECEIVE, "merchant.service_work_order.view", "merchant.service_work_order.create");
        account("p", project("a"), store, RECEIVE);
        account("c", project("a"), null, RECEIVE);
        account("x", project("b"), null, RECEIVE);
        care = account("k", project("a"), null, "facility.work_order.manage", "merchant.service_work_order.view",
                "merchant.service_work_order.close");
        processor = account("w", project("a"), null, "facility.work_order.manage");
    }

    /**
     * 客人端只列该综合体在营门店、自选门店随投诉记下；客户关怀部读到可选门店并可更正（标上、清掉）；周报按上周一 00:00 到本周一 00:00（北京时间）
     * 取当周提出的客诉：门店账号只收本店（标了本店的客人投诉＋本店在商户专区提的投诉），平台员工按可触及项目收；两周前的不计；没有客诉的照发
     * 「本周无客诉」；同一周重跑只覆盖、不重发。
     */
    @Test
    void weeklyReportGoesToStoreAndCompanyRecipients() throws Exception {
        String c1 = customer("1");
        customerGet(c1, "/api/v1/customer/complaints/store-options?project_id=" + project("a")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.merchant_id=='" + store + "')].merchant_name").value("客诉周报门店" + suffix))
                .andExpect(jsonPath("$.data[?(@.merchant_id=='" + closedStore + "')]").isEmpty());
        submit(c1, closedStore).andExpect(status().isBadRequest());
        String tagged = complaintId(submit(c1, store).andExpect(status().isOk()));
        assertThat(jdbc.queryForObject("SELECT store_merchant_id FROM complaint_followups WHERE chain = 'facility' AND work_order_id = ?",
                String.class, tagged)).isEqualTo(store);

        String plain = complaintId(submit(customer("2"), null).andExpect(status().isOk()));
        String facility = "/api/v1/facility/work-orders/" + plain;
        staffPost(care, facility + "/assign", Map.of("assignee_id", user("w"))).andExpect(status().isOk());
        staffPost(processor, facility + "/start", Map.of()).andExpect(status().isOk());
        staffPost(processor, facility + "/complete", Map.of("resolution", "已向客人致歉")).andExpect(status().isOk());
        staffPost(care, facility + "/review", Map.of("customer_feedback", "客人接受道歉")).andExpect(status().isOk());
        staffPost(care, FOLLOWUPS + "facility/" + plain + "/loop-confirm", Map.of()).andExpect(status().isOk());

        String merchantComplaint = JsonPath.read(staffPost(storeOwner, MERCHANT_WORK_ORDERS, Map.of("operator_note", "商户专区自助提交工单",
                "work_order_type", "complaint", "priority", "medium", "title", "隔壁太吵" + suffix, "description", "影响营业",
                "reported_channel", "merchant_portal")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        String old = complaintId(submit(customer("3"), store).andExpect(status().isOk()));
        jdbc.update("UPDATE facility_work_orders SET created_at = created_at - interval '14 days' WHERE id = ?", old);

        // 客户关怀部可更正：读到可选门店含 M；把未标门店的投诉标上 M，再清掉。
        mockMvc.perform(get(FOLLOWUPS + "facility/" + plain).with(care)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.store_candidates[?(@.merchant_id=='" + store + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data.store_candidates[?(@.merchant_id=='" + closedStore + "')]").isEmpty());
        staffPut(care, FOLLOWUPS + "facility/" + plain, Map.of("store_merchant_id", store)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.store_merchant_id").value(store));
        staffPut(care, FOLLOWUPS + "facility/" + plain, Map.of("store_merchant_id", "")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.store_merchant_id").doesNotExist());
        staffPut(care, FOLLOWUPS + "facility/" + plain, Map.of("store_merchant_id", "mch_absent_" + suffix)).andExpect(status().isBadRequest());
        staffPut(care, FOLLOWUPS + "merchant_service/" + merchantComplaint, Map.of("store_merchant_id", store)).andExpect(status().isBadRequest());

        Instant runAt = LocalDate.now(LocalZone.ID).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1)
                .atTime(8, 0).atZone(LocalZone.ID).toInstant();
        reports.sendWeeklyReports(runAt);

        for (String storeAccount : List.of("m", "p")) {
            assertThat(report(storeAccount)).contains("共 2 笔", "消费客人投诉 1", "商户投诉 1", "已闭环 0", "未闭环 2");
        }
        assertThat(report("c")).contains("共 3 笔", "消费客人投诉 2", "商户投诉 1", "已关闭 1", "已闭环 1", "未闭环 2");
        assertThat(report("x")).contains("本周无客诉");
        assertThat(jdbc.queryForObject("SELECT title FROM employee_notices WHERE staff_profile_id = ? AND id LIKE 'notice_cwr_%'",
                String.class, profile("c"))).startsWith("客诉周报（");
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE event_name = 'complaint.weekly_report_sent' "
                + "AND business_id = ?", String.class, runAt.atZone(LocalZone.ID).toLocalDate().minusWeeks(1).toString())).isNotEmpty();

        int before = notices();
        reports.sendWeeklyReports(runAt);
        assertThat(notices()).isEqualTo(before).isEqualTo(4);
    }

    // ---------- helpers ----------

    /** 照客人端投诉表单（ComplaintsPage / 小程序 complaints）的 multipart 请求形状；store 为空即不选门店。 */
    private ResultActions submit(String token, String storeMerchantId) throws Exception {
        var request = multipart("/api/v1/customer/complaints")
                .param("idempotency_key", "cwr_" + UUID.randomUUID())
                .param("project_id", project("a"))
                .param("title", "服务态度差")
                .param("description", "客人端文字留言");
        if (storeMerchantId != null) {
            request.param("store_merchant_id", storeMerchantId);
        }
        return mockMvc.perform(request.header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token));
    }

    private ResultActions customerGet(String token, String path) throws Exception {
        return mockMvc.perform(get(path).header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token));
    }

    private static String complaintId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.work_order_id");
    }

    private String report(String tag) {
        List<String> bodies = jdbc.queryForList("SELECT body FROM employee_notices WHERE staff_profile_id = ? AND id LIKE 'notice_cwr_%'",
                String.class, profile(tag));
        assertThat(bodies).as("收件人 " + tag + " 的客诉周报").hasSize(1);
        return bodies.getFirst();
    }

    private int notices() {
        return jdbc.queryForObject("SELECT count(*) FROM employee_notices WHERE id LIKE 'notice_cwr_%' AND staff_profile_id IN (?, ?, ?, ?)",
                Integer.class, profile("m"), profile("p"), profile("c"), profile("x"));
    }

    private ResultActions staffPost(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        return send(post(path), who, body);
    }

    /** 照后台客诉跟进弹窗（ComplaintFollowupDialog 保存门店）的请求形状。 */
    private ResultActions staffPut(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        return send(put(path), who, body);
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, RequestPostProcessor who, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        actual.putIfAbsent("idempotency_key", "cwr_" + UUID.randomUUID());
        return mockMvc.perform(builder.with(who).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(actual)));
    }

    private String customer(String tag) {
        String id = "cus_cwr_" + tag + "_" + suffix;
        Instant now = Instant.now();
        String phone = "136" + String.format("%08d", Math.floorMod(id.hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(id, phone, "136****" + phone.substring(7), "active", null, null, now, now));
        String token = "tok_" + id;
        customers.saveSession(new CustomerSessionRecord("sess_" + id, id, token, "miniapp", now, now.plusSeconds(3600), "active"));
        return token;
    }

    /** 员工账号（merchantId 为空＝平台员工）＋本人角色＋一行已批准的员工档案。 */
    private RequestPostProcessor account(String tag, String projectId, String merchantId, String... permissionCodes) {
        String userId = user(tag);
        String roleId = "role_cwr_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '客诉周报回归', 'project', 'active', now())",
                roleId, "客诉周报回归角色" + tag + suffix);
        for (String code : permissionCodes) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES (?, ?, 'staff', ?, ?, 'active', now())", userId, userId, projectId, merchantId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 'approved', 'active', now(), now())", profile(tag), projectId, merchantId, userId, "周报" + tag);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String project(String tag) {
        return "prj_cwr_" + tag + "_" + suffix;
    }

    private String user(String tag) {
        return "usr_cwr_" + tag + "_" + suffix;
    }

    private String profile(String tag) {
        return "stf_cwr_" + tag + "_" + suffix;
    }
}
