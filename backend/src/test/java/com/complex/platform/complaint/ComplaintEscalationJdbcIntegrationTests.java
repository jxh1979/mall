package com.complex.platform.complaint;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * 第十八批 M 段第 118 条 118-R1 客户关怀部认定投诉类别、同类投诉上升（真库＋真实入口＋真实授权链，请求体一律按前端蛇形键）。
 * BSM:1385「一个月内，同一类投诉由三个不同的人分别提出的，立即上升到项目负责人、娱乐平台负责人与公司最高负责人；是否属于同一类由客户关怀部认定。」
 * 「投诉分为商户投诉和消费客人投诉」；DEC-R4-2026-08-04-12。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "evidence.attachment.dir=/tmp/complex-complaint-escalation-test"
})
@AutoConfigureMockMvc
@Transactional
class ComplaintEscalationJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FOLLOWUPS = "/api/v1/complaint-followups/";
    private static final String MERCHANT_WORK_ORDERS = "/api/v1/merchant-service/work-orders";
    private static final String RECEIVE = "complaint.escalation.receive";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String merchant = "mch_cesc_" + suffix;
    private RequestPostProcessor merchantAccount;
    private RequestPostProcessor care;
    private RequestPostProcessor careOther;
    private final Map<String, String> tokens = new LinkedHashMap<>();

    /** 项目 A、B、C；商户 M 主项目 A；接收人 lead 只到 A、other 只到 B；客户关怀部 k 只到 A、kc 只到 C；客人 c1～c3。 */
    @BeforeEach
    void seed() {
        for (String tag : List.of("a", "b", "c")) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                    project(tag), "同类投诉测试综合体" + tag);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'retail', 'active', now(), now(), ?)", merchant, "同类投诉商户" + suffix, project("a"));
        merchantAccount = account("m", project("a"), merchant, "merchant.service_work_order.view", "merchant.service_work_order.create");
        account("lead", project("a"), null, RECEIVE);
        account("other", project("b"), null, RECEIVE);
        care = account("k", project("a"), null, "facility.work_order.manage", "merchant.service_work_order.view",
                "merchant.service_work_order.close");
        careOther = account("kc", project("c"), null, "facility.work_order.manage", "merchant.service_work_order.close");
        for (String tag : List.of("c1", "c2", "c3")) {
            tokens.put(tag, customer(tag));
        }
    }

    /**
     * 三位客人各投诉一笔、客户关怀部逐一认定同类：前两次不上升，第三次立即通知可触及该项目的接收人（其他项目的不收）；同值重存、同一提出人再投诉
     * 同类都不再上升；商户在商户专区提的投诉认定同类、新增第四人，再上升一次。
     */
    @Test
    void sameClassFromThreeDifferentPeopleEscalates() throws Exception {
        String cls = "包间噪音" + suffix;
        Map<String, Object> w1 = complaint("c1"), w2 = complaint("c2"), w3 = complaint("c3");
        classify("facility", w1.get("id"), cls).andExpect(status().isOk()).andExpect(jsonPath("$.data.complaint_class").value(cls))
                .andExpect(jsonPath("$.data.classified_by").value(user("k")));
        classify("facility", w2.get("id"), cls).andExpect(status().isOk());
        assertThat(escalations("lead", cls)).isEmpty();
        classify("facility", w3.get("id"), cls).andExpect(status().isOk());
        assertThat(escalations("lead", cls)).hasSize(1);
        assertThat(escalations("lead", cls).getFirst())
                .contains("3 位", (String) w1.get("code"), (String) w2.get("code"), (String) w3.get("code"));
        assertThat(escalations("other", cls)).isEmpty();
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE business_id = ?", String.class, w3.get("id")))
                .contains("complaint.classified", "complaint.same_class_escalated");

        classify("facility", w3.get("id"), cls).andExpect(status().isOk());
        classify("facility", complaint("c1").get("id"), cls).andExpect(status().isOk());
        assertThat(escalations("lead", cls)).hasSize(1);

        String merchantComplaint = merchantWorkOrder("complaint");
        classify("merchant_service", merchantComplaint, cls).andExpect(status().isOk());
        assertThat(escalations("lead", cls)).hasSize(2);
        assertThat(escalations("lead", cls)).anyMatch(text -> text.contains("4 位") && text.contains(merchantComplaint));
    }

    /** 窗口按提出时间算、认定晚于提出也照提出时间：40、35、25 天前提出的今天才认定，上升；32 天前、5 天前、今天的三笔首尾超过一个月，不上升。 */
    @Test
    void windowCountsRaiseTimeNotClassificationTime() throws Exception {
        String parking = "停车收费" + suffix;
        int[] parkingDays = {40, 35, 25};
        int i = 0;
        for (String tag : List.of("c1", "c2", "c3")) {
            String id = (String) complaint(tag).get("id");
            jdbc.update("UPDATE facility_work_orders SET created_at = created_at - make_interval(days => ?) WHERE id = ?", parkingDays[i++], id);
            classify("facility", id, parking).andExpect(status().isOk());
        }
        assertThat(escalations("lead", parking)).hasSize(1);

        String light = "灯光" + suffix;
        int[] lightDays = {32, 5, 0};
        i = 0;
        for (String tag : List.of("c1", "c2", "c3")) {
            String id = (String) complaint(tag).get("id");
            jdbc.update("UPDATE facility_work_orders SET created_at = created_at - make_interval(days => ?) WHERE id = ?", lightDays[i++], id);
            classify("facility", id, light).andExpect(status().isOk());
        }
        assertThat(escalations("lead", light)).isEmpty();
    }

    /** 非投诉 409 NOT_A_COMPLAINT；管不到该项目的员工 403 CROSS_PROJECT_FORBIDDEN；类别为空、两项都不带 400；都不落行。 */
    @Test
    void classifyGuards() throws Exception {
        classify("merchant_service", merchantWorkOrder("repair"), "噪音").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("NOT_A_COMPLAINT"));
        String id = (String) complaint("c1").get("id");
        staffPut(careOther, FOLLOWUPS + "facility/" + id, Map.of("complaint_class", "噪音")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        staffPut(care, FOLLOWUPS + "facility/" + id, Map.of("complaint_class", "  ")).andExpect(status().isBadRequest());
        staffPut(care, FOLLOWUPS + "facility/" + id, Map.of()).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM complaint_followups WHERE work_order_id = ?", Integer.class, id)).isZero();
    }

    // ---------- helpers ----------

    /** 照后台客诉跟进弹窗（ComplaintFollowupDialog 保存认定）的请求形状。 */
    private ResultActions classify(String chain, Object id, String complaintClass) throws Exception {
        return staffPut(care, FOLLOWUPS + chain + "/" + id, Map.of("complaint_class", complaintClass));
    }

    private List<String> escalations(String tag, String complaintClass) {
        return jdbc.queryForList("SELECT body FROM employee_notices WHERE staff_profile_id = ? AND title = ?", String.class,
                profile(tag), "同类投诉上升：" + complaintClass);
    }

    /** 照客人端投诉表单的 multipart 请求形状；返回工单编号与展示编号。 */
    private Map<String, Object> complaint(String customerTag) throws Exception {
        var submit = multipart("/api/v1/customer/complaints")
                .param("idempotency_key", "cesc_" + UUID.randomUUID())
                .param("project_id", project("a"))
                .param("title", "同类投诉")
                .param("description", "客人端文字留言");
        String body = mockMvc.perform(submit.header("X-Terminal-Type", "miniapp").header("X-Customer-Token", tokens.get(customerTag)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return Map.of("id", JsonPath.read(body, "$.data.work_order_id"), "code", JsonPath.read(body, "$.data.code"));
    }

    /** 照商户专区提单页（MerchantZoneWorkOrdersPage）的请求形状。 */
    private String merchantWorkOrder(String type) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>(Map.of("operator_note", "商户专区自助提交工单", "work_order_type", type,
                "priority", "medium", "title", "噪音" + suffix, "description", "隔壁太吵", "reported_channel", "merchant_portal"));
        body.put("idempotency_key", "cesc_" + UUID.randomUUID());
        return JsonPath.read(mockMvc.perform(post(MERCHANT_WORK_ORDERS).with(merchantAccount).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private ResultActions staffPut(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        actual.putIfAbsent("idempotency_key", "cesc_" + UUID.randomUUID());
        return mockMvc.perform(put(path).with(who).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(actual)));
    }

    private String customer(String tag) {
        String id = "cus_cesc_" + tag + "_" + suffix;
        Instant now = Instant.now();
        String phone = "135" + String.format("%08d", Math.floorMod(id.hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(id, phone, "135****" + phone.substring(7), "active", null, null, now, now));
        String token = "tok_" + id;
        customers.saveSession(new CustomerSessionRecord("sess_" + id, id, token, "miniapp", now, now.plusSeconds(3600), "active"));
        return token;
    }

    /** 员工账号（merchantId 为空＝平台员工）＋本人角色＋一行已批准的员工档案。 */
    private RequestPostProcessor account(String tag, String projectId, String merchantId, String... permissionCodes) {
        String userId = user(tag);
        String roleId = "role_cesc_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '同类投诉上升回归', 'project', 'active', now())",
                roleId, "同类投诉回归角色" + tag + suffix);
        for (String code : permissionCodes) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES (?, ?, 'staff', ?, ?, 'active', now())", userId, userId, projectId, merchantId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 'approved', 'active', now(), now())", profile(tag), projectId, merchantId, userId, "上升" + tag);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String project(String tag) {
        return "prj_cesc_" + tag + "_" + suffix;
    }

    private String user(String tag) {
        return "usr_cesc_" + tag + "_" + suffix;
    }

    private String profile(String tag) {
        return "stf_cesc_" + tag + "_" + suffix;
    }
}
