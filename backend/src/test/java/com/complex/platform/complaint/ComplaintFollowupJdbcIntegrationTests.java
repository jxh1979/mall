package com.complex.platform.complaint;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 111 条 111-R3 客诉闭环确认（真库＋真实入口＋真实授权链，请求体一律按前端蛇形键）。
 * BSM:1385「所有客诉必须有跟进、有记录、有结果，直至形成闭环；客诉须经客户关怀部确认后方可视为形成闭环，未经其确认的一律不算闭环」；
 * PGM:502「职责为受理客人投诉与商户异议、监督与协调各责任部门处理、确认闭环，并负责工单处理」；DEC-R4-2026-08-04-12。
 * 两条链共用：商户投诉走商户服务工单链（闸同员工关单：关闭码＋该商户经营项目），客人投诉走物业工单链（闸同回访关单：facility.work_order.manage＋工单项目）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "evidence.attachment.dir=/tmp/complex-complaint-followup-test"
})
@AutoConfigureMockMvc
@Transactional
class ComplaintFollowupJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FOLLOWUPS = "/api/v1/complaint-followups/";
    private static final String MERCHANT_WORK_ORDERS = "/api/v1/merchant-service/work-orders";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String merchant = "mch_cf_" + suffix;
    private RequestPostProcessor merchantAccount;
    private RequestPostProcessor dispatcher;
    private RequestPostProcessor processor;
    private RequestPostProcessor care;
    private RequestPostProcessor careOther;

    /** 项目 A、C；商户 M 主项目 A；派单员、处理员、客户关怀部员工都只到项目 A，另一名客户关怀部员工只到项目 C。 */
    @BeforeEach
    void seed() {
        for (String tag : List.of("a", "c")) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                    project(tag), "客诉跟进测试综合体" + tag);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'retail', 'active', now(), now(), ?)", merchant, "客诉跟进商户" + suffix, project("a"));
        merchantAccount = account("m", project("a"), merchant, "merchant.service_work_order.view", "merchant.service_work_order.create");
        dispatcher = account("d", project("a"), null, "merchant.service_work_order.view", "merchant.service_work_order.assign");
        processor = account("p", project("a"), null, "merchant.service_work_order.view", "merchant.service_work_order.process",
                "facility.work_order.manage");
        care = account("k", project("a"), null, "merchant.service_work_order.view", "merchant.service_work_order.close",
                "facility.work_order.manage");
        careOther = account("kc", project("c"), null, "merchant.service_work_order.close", "facility.work_order.manage");
    }

    /**
     * ①商户投诉：派单、处理到已处理待关闭、商户确认关闭后，客户关怀部读跟进为未闭环；商户账号与只到项目 C 的员工确认闭环 403、不落行；
     * 客户关怀部确认闭环即记确认人与时间；再确认 409 LOOP_ALREADY_CONFIRMED。
     */
    @Test
    void customerCareConfirmsLoopOfClosedMerchantComplaint() throws Exception {
        String id = createMerchantWorkOrder("complaint");
        mockMvc.perform(get(MERCHANT_WORK_ORDERS + "/" + id + "/owner-candidates").with(dispatcher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.user_id=='" + user("p") + "')]").isNotEmpty());
        staffPost(dispatcher, MERCHANT_WORK_ORDERS + "/" + id + "/assign", Map.of("operator_note", "派单", "owner_department", "property",
                "owner_user_id", user("p"))).andExpect(status().isOk());
        staffPost(processor, MERCHANT_WORK_ORDERS + "/" + id + "/actions", Map.of("operator_note", "已到场", "target_status", "processing"))
                .andExpect(status().isOk());
        staffPost(processor, MERCHANT_WORK_ORDERS + "/" + id + "/actions", Map.of("operator_note", "已处理", "target_status", "resolved"))
                .andExpect(status().isOk());
        staffPost(merchantAccount, MERCHANT_WORK_ORDERS + "/" + id + "/merchant-confirm", Map.of()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("closed"));

        mockMvc.perform(get(FOLLOWUPS + "merchant_service/" + id).with(care)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.work_order_id").value(id))
                .andExpect(jsonPath("$.data.loop_confirmed_at").doesNotExist());
        confirmLoop(merchantAccount, "merchant_service", id).andExpect(status().isForbidden());
        confirmLoop(careOther, "merchant_service", id).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM complaint_followups WHERE work_order_id = ?", Integer.class, id)).isZero();

        confirmLoop(care, "merchant_service", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loop_confirmed_by").value(user("k")))
                .andExpect(jsonPath("$.data.loop_confirmed_at").exists());
        Map<String, Object> row = jdbc.queryForMap("SELECT loop_confirmed_by, loop_confirmed_at FROM complaint_followups "
                + "WHERE chain = 'merchant_service' AND work_order_id = ?", id);
        assertThat(row.get("loop_confirmed_by")).isEqualTo(user("k"));
        assertThat(row.get("loop_confirmed_at")).isNotNull();
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE business_id = ?", String.class, id))
                .contains("complaint.loop_confirmed", "complaint.loop_confirm_failed");
        confirmLoop(care, "merchant_service", id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LOOP_ALREADY_CONFIRMED"));
    }

    /** ②未关闭的投诉确认闭环 409 INVALID_STATE；报修单读跟进与确认都 409 NOT_A_COMPLAINT；都不落行。 */
    @Test
    void onlyClosedComplaintsCanBeLoopConfirmed() throws Exception {
        String open = createMerchantWorkOrder("complaint");
        confirmLoop(care, "merchant_service", open).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INVALID_STATE"));
        String repair = createMerchantWorkOrder("repair");
        mockMvc.perform(get(FOLLOWUPS + "merchant_service/" + repair).with(care)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("NOT_A_COMPLAINT"));
        confirmLoop(care, "merchant_service", repair).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("NOT_A_COMPLAINT"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM complaint_followups WHERE work_order_id IN (?, ?)", Integer.class, open, repair))
                .isZero();
    }

    /** ③客人投诉：客人端 multipart 提交，员工派单、开始、完成、回访（记客人反馈）关单后，客户关怀部确认闭环；只到项目 C 的员工 403。 */
    @Test
    void customerCareConfirmsLoopOfClosedCustomerComplaint() throws Exception {
        String token = customer("cus_cf_" + suffix);
        var submit = multipart("/api/v1/customer/complaints")
                .param("idempotency_key", "cf_" + UUID.randomUUID())
                .param("project_id", project("a"))
                .param("title", "包厢太吵")
                .param("description", "客人端文字留言");
        String id = JsonPath.read(mockMvc.perform(submit.header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.work_order_id");
        String facility = "/api/v1/facility/work-orders/" + id;
        staffPost(care, facility + "/assign", Map.of("assignee_id", user("p"))).andExpect(status().isOk());
        staffPost(processor, facility + "/start", Map.of()).andExpect(status().isOk());
        staffPost(processor, facility + "/complete", Map.of("resolution", "已向客人致歉")).andExpect(status().isOk());
        confirmLoop(care, "facility", id).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("INVALID_STATE"));
        staffPost(care, facility + "/review", Map.of("customer_feedback", "客人接受道歉")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("closed"));

        confirmLoop(careOther, "facility", id).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        confirmLoop(care, "facility", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loop_confirmed_by").value(user("k")));
        mockMvc.perform(get(FOLLOWUPS + "facility/" + id).with(care)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loop_confirmed_by").value(user("k")));
    }

    // ---------- helpers ----------

    /** 照后台客诉跟进弹窗（ComplaintFollowupDialog.confirmLoop）的请求形状。 */
    private ResultActions confirmLoop(RequestPostProcessor who, String chain, String id) throws Exception {
        return staffPost(who, FOLLOWUPS + chain + "/" + id + "/loop-confirm", Map.of());
    }

    /** 照商户专区提单页（MerchantZoneWorkOrdersPage）的请求形状。 */
    private String createMerchantWorkOrder(String type) throws Exception {
        return JsonPath.read(staffPost(merchantAccount, MERCHANT_WORK_ORDERS, Map.of("operator_note", "商户专区自助提交工单",
                "work_order_type", type, "priority", "medium", "title", "噪音" + suffix, "description", "隔壁太吵",
                "reported_channel", "merchant_portal"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private ResultActions staffPost(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        actual.putIfAbsent("idempotency_key", "cf_" + UUID.randomUUID());
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(actual)));
    }

    private String customer(String id) {
        Instant now = Instant.now();
        String phone = "137" + String.format("%08d", Math.floorMod(id.hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(id, phone, "137****" + phone.substring(7), "active", null, null, now, now));
        String customerToken = "tok_" + id;
        customers.saveSession(new CustomerSessionRecord("sess_" + id, id, customerToken, "miniapp", now, now.plusSeconds(3600), "active"));
        return customerToken;
    }

    /** 员工账号（merchantId 为空＝平台员工）＋本人角色＋一行已批准的员工档案。 */
    private RequestPostProcessor account(String tag, String projectId, String merchantId, String... permissionCodes) {
        String userId = user(tag);
        String roleId = "role_cf_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '客诉跟进回归', 'project', 'active', now())",
                roleId, roleId);
        for (String code : permissionCodes) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES (?, ?, 'staff', ?, ?, 'active', now())", userId, userId, projectId, merchantId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 'approved', 'active', now(), now())", "stf_cf_" + tag + "_" + suffix, projectId, merchantId, userId, tag);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String project(String tag) {
        return "prj_cf_" + tag + "_" + suffix;
    }

    private String user(String tag) {
        return "usr_cf_" + tag + "_" + suffix;
    }
}
