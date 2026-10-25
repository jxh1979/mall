package com.complex.platform.facility;

// 文件版本：V0.02

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03 clients-R8 / backend-R26 客人投诉真库、真接口（客人端 multipart 与蛇形键，按前端真实请求形状）。
 * BSM 第 869 行「投诉与反馈：客人可在官方 App、小程序提交投诉，支持文字留言、照片、视频与语音」；第 1379 行「客人（消费客人通过客人端投诉，客人即提单方；
 * 客户关怀部记录问题解决方案及客人反馈后可以关闭）」；第 1385 行「满意度在回访/验收时由提单方评分」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "evidence.attachment.dir=/tmp/complex-customer-complaint-test"
})
@AutoConfigureMockMvc
@Transactional
class CustomerComplaintJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_cc_" + suffix;
    private final Set<String> granted = new HashSet<>();
    private String token;
    private String customerId;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '投诉测试综合体', 'active', now(), now())", project);
        customerId = "cus_cc_" + suffix;
        token = customer(customerId);
    }

    @Test
    void submitCreatesCustomerSourcedComplaintWithMedia() throws Exception {
        String id = read(submit(token, "包厢空调不制冷", List.of(file("photo.jpg", "image/jpeg"), file("clip.mp4", "video/mp4"),
                file("voice.m4a", "audio/mp4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_assign"))
                .andExpect(jsonPath("$.data.media_count").value(3)), "$.data.work_order_id");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT work_order_type, source, reporter_id, reporter_type, project_id FROM facility_work_orders WHERE id = ?", id);
        assertThat(row).containsEntry("work_order_type", "complaint").containsEntry("source", "customer")
                .containsEntry("reporter_id", customerId).containsEntry("reporter_type", "customer").containsEntry("project_id", project);
        assertThat(jdbc.queryForList("SELECT content_type FROM customer_complaint_media WHERE work_order_id = ? ORDER BY content_type",
                String.class, id)).containsExactly("audio/mp4", "image/jpeg", "video/mp4");
        // 后台按工单查看权限受控回读附件。
        String mediaId = jdbc.queryForObject("SELECT id FROM customer_complaint_media WHERE work_order_id = ? AND content_type = 'audio/mp4'",
                String.class, id);
        mockMvc.perform(get("/api/v1/facility/work-orders/" + id + "/customer-media/" + mediaId + "/file")
                        .with(staff("usr_cc_care_" + suffix, "facility.work_order.manage")))
                .andExpect(status().isOk());
    }

    @Test
    void deregistrationKeepsComplaintMediaAsWorkOrderMaterial() throws Exception {
        // 操作员裁决第十三批第 10 条「作为工单材料保留」：附件保留在工单里，只脱去与客人的关联标识——附件行与工单只留随机客人编号，
        // 注销删去该编号下的手机号等个人信息后不可反查（与钱账留存同一做法），有工单查看权限的员工照常回读。
        String id = read(submit(token, "包厢音响太吵", List.of(file("photo.jpg", "image/jpeg"))).andExpect(status().isOk()),
                "$.data.work_order_id");
        mockMvc.perform(post("/api/v1/customer/deregister").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of("waive_balance", true))))
                .andExpect(status().isOk());
        String mediaId = jdbc.queryForObject("SELECT id FROM customer_complaint_media WHERE work_order_id = ? AND customer_id = ?",
                String.class, id, customerId);
        byte[] content = mockMvc.perform(get("/api/v1/facility/work-orders/" + id + "/customer-media/" + mediaId + "/file")
                        .with(staff("usr_cc_care_" + suffix, "facility.work_order.manage")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(content).isEqualTo(new byte[] {1, 2, 3, 4});
        Map<String, Object> account = jdbc.queryForMap("SELECT status, phone, phone_hash, masked_phone FROM customers WHERE id = ?", customerId);
        assertThat(account).containsEntry("status", "deregistered").containsEntry("phone", null).containsEntry("phone_hash", null)
                .containsEntry("masked_phone", null);
    }

    @Test
    void audioAcceptedOnlyOnComplaintEntry() throws Exception {
        submit(token, "噪音", List.of(file("voice.exe", "application/octet-stream")))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_work_orders WHERE reporter_id = ?", Integer.class, customerId))
                .isZero();
        // 语音只在投诉入口收：其余证据上传仍只收照片与视频。
        com.complex.platform.evidence.EvidenceFileStorage storage =
                new com.complex.platform.evidence.EvidenceFileStorage("/tmp/complex-customer-complaint-test", 1024);
        MockMultipartFile voice = file("voice.m4a", "audio/mp4");
        assertThat(storage.storeComplaintMedia(voice).contentType()).isEqualTo("audio/mp4");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> storage.store(voice))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);
    }

    @Test
    void listReturnsOnlyOwnComplaints() throws Exception {
        submit(token, "我的投诉", List.of()).andExpect(status().isOk());
        String other = customer("cus_cc_other_" + suffix);
        submit(other, "别人的投诉", List.of()).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/customer/complaints").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("我的投诉"))
                .andExpect(jsonPath("$.data.items[0].status_label").value("待派单"));
    }

    @Test
    void closeCustomerComplaintRequiresFeedback() throws Exception {
        String id = read(submit(token, "服务员态度差", List.of()).andExpect(status().isOk()), "$.data.work_order_id");
        resolve(id);
        RequestPostProcessor care = staff("usr_cc_care_" + suffix, "facility.work_order.manage");
        staffPost(care, "/api/v1/facility/work-orders/" + id + "/review", Map.of("note", "已回访"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("COMPLAINT_CUSTOMER_FEEDBACK_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT status FROM facility_work_orders WHERE id = ?", String.class, id)).isEqualTo("pending_review");
        staffPost(care, "/api/v1/facility/work-orders/" + id + "/review", Map.of("customer_feedback", "客人表示接受道歉"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(jdbc.queryForObject("SELECT customer_feedback FROM facility_work_orders WHERE id = ?", String.class, id))
                .isEqualTo("客人表示接受道歉");
    }

    // ---------- R26 满意度 ----------

    @Test
    void reporterRatesOnceAfterResolution() throws Exception {
        String id = read(submit(token, "出品慢", List.of()).andExpect(status().isOk()), "$.data.work_order_id");
        resolve(id);
        rate(token, id, 4).andExpect(status().isOk()).andExpect(jsonPath("$.data.satisfaction").value(4));
        rate(token, id, 5).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("FACILITY_SATISFACTION_ALREADY_RATED"));
        // 员工关闭（记客人反馈）不改写客人的评分。
        staffPost(staff("usr_cc_care_" + suffix, "facility.work_order.manage"), "/api/v1/facility/work-orders/" + id + "/review",
                Map.of("customer_feedback", "已致歉", "satisfaction", 1)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT satisfaction FROM facility_work_orders WHERE id = ?", Integer.class, id)).isEqualTo(4);
        mockMvc.perform(get("/api/v1/customer/complaints").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(jsonPath("$.data.items[0].satisfaction").value(4))
                .andExpect(jsonPath("$.data.items[0].resolution").value("已向客人致歉并退还服务费"));
    }

    @Test
    void otherCustomerCannotRate() throws Exception {
        String id = read(submit(token, "卫生间脏", List.of()).andExpect(status().isOk()), "$.data.work_order_id");
        resolve(id);
        rate(customer("cus_cc_x_" + suffix), id, 1).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_COMPLAINT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT satisfaction FROM facility_work_orders WHERE id = ?", Integer.class, id)).isNull();
    }

    @Test
    void cannotRateBeforeResolution() throws Exception {
        String id = read(submit(token, "排队太久", List.of()).andExpect(status().isOk()), "$.data.work_order_id");
        rate(token, id, 3).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("COMPLAINT_NOT_RESOLVED"));
    }

    // ---------- helpers ----------

    /** 客户关怀部派单、处理并写处理结论（投诉×普通策略：完工即待回访）。 */
    private void resolve(String id) throws Exception {
        String handler = "usr_cc_handler_" + suffix;
        RequestPostProcessor care = staff("usr_cc_care_" + suffix, "facility.work_order.manage");
        grant(handler, "facility.work_order.manage");
        staffPost(care, "/api/v1/facility/work-orders/" + id + "/assign", Map.of("assignee_id", handler)).andExpect(status().isOk());
        RequestPostProcessor worker = staff(handler, "facility.work_order.manage");
        staffPost(worker, "/api/v1/facility/work-orders/" + id + "/start", Map.of()).andExpect(status().isOk());
        staffPost(worker, "/api/v1/facility/work-orders/" + id + "/complete", Map.of("resolution", "已向客人致歉并退还服务费"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_review"));
    }

    /**
     * PGM 第 736 行「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」、第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：
     * 关掉本综合体的小程序后，投诉按客人所选综合体（project_id）在服务端判开关——不带 X-Project-Id 或把头写成另一个（开着的）综合体都提交不了、
     * 不建工单；另一综合体照常提交。判开关先于幂等：开关开着时成功过的同一幂等键，关掉后重放也 403，拿不到第一次的缓存结果。
     */
    @Test
    void closedProjectComplaintRejectedWithoutOrWithForgedProjectHeader() throws Exception {
        String other = otherProject();
        String replayKey = "cc_replay_" + suffix;
        submitAt(project, null, replayKey).andExpect(status().isOk());
        closeCustomerTerminal();
        submitAt(project, null, replayKey).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));

        for (String header : new String[] {null, other}) {
            submitAt(project, header).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        }
        // 只有开关开着时那一单。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_work_orders WHERE reporter_id = ?", Integer.class, customerId))
                .isEqualTo(1);
        submitAt(other, other).andExpect(status().isOk()).andExpect(jsonPath("$.data.work_order_id").exists());
    }

    /**
     * PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：满意度评分按工单所属综合体在服务端判开关——不带 X-Project-Id 或把头写成
     * 另一个（开着的）综合体都评不了（评分仍为空）；另一综合体的投诉照常评分。判开关先于幂等：开关开着时用同一幂等键评过分的本人投诉，
     * 关掉后重放也 403，拿不到第一次的缓存结果。
     */
    @Test
    void closedProjectComplaintRatingRejectedWithoutOrWithForgedProjectHeader() throws Exception {
        String other = otherProject();
        String id = read(submitAt(project, null).andExpect(status().isOk()), "$.data.work_order_id");
        resolve(id);
        String otherId = read(submitAt(other, other).andExpect(status().isOk()), "$.data.work_order_id");
        String replayId = read(submitAt(project, null).andExpect(status().isOk()), "$.data.work_order_id");
        // 另一综合体的投诉与本综合体用于重放的那张直接置为已写处理结论（待回访），本用例只看开关。
        jdbc.update("UPDATE facility_work_orders SET status = 'pending_review', resolution = '已处理' WHERE id IN (?, ?)", otherId, replayId);
        String replayKey = "rate_replay_" + suffix;
        rate(token, replayId, 3, null, replayKey).andExpect(status().isOk()).andExpect(jsonPath("$.data.satisfaction").value(3));
        closeCustomerTerminal();
        rate(token, replayId, 3, null, replayKey).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));

        for (String header : new String[] {null, other}) {
            rate(token, id, 4, header).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        }
        assertThat(jdbc.queryForObject("SELECT satisfaction FROM facility_work_orders WHERE id = ?", Integer.class, id)).isNull();
        rate(token, otherId, 5, other).andExpect(status().isOk()).andExpect(jsonPath("$.data.satisfaction").value(5));
    }

    /** 另一个（开着的）综合体。 */
    private String otherProject() {
        String other = "prj_cc2_" + suffix;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '投诉测试综合体二', 'active', now(), now())", other);
        return other;
    }

    /** 关掉本综合体的小程序（按综合体的端总开关）。 */
    private void closeCustomerTerminal() {
        jdbc.update("""
                INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
                VALUES (?, 'miniapp', 'terminal.enabled', 'project', ?, FALSE, '停用该综合体客人端', now(), now())""", "tcs_cc_" + suffix, project);
    }

    /** 客人端投诉真实请求形状（multipart、蛇形键）：所选综合体放 project_id；header 非空时另带 X-Project-Id。 */
    private ResultActions submitAt(String projectId, String header) throws Exception {
        return submitAt(projectId, header, "cc_" + UUID.randomUUID());
    }

    private ResultActions submitAt(String projectId, String header, String idempotencyKey) throws Exception {
        var request = multipart("/api/v1/customer/complaints")
                .param("idempotency_key", idempotencyKey)
                .param("project_id", projectId)
                .param("title", "包厢太吵")
                .param("description", "客人端文字留言");
        request.header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token);
        if (header != null) {
            request.header("X-Project-Id", header);
        }
        return mockMvc.perform(request);
    }

    private ResultActions submit(String customerToken, String title, List<MockMultipartFile> files) throws Exception {
        var request = multipart("/api/v1/customer/complaints")
                .param("idempotency_key", "cc_" + UUID.randomUUID())
                .param("project_id", project)
                .param("title", title)
                .param("description", "客人端文字留言");
        files.forEach(request::file);
        return mockMvc.perform(request.header("X-Terminal-Type", "miniapp").header("X-Customer-Token", customerToken));
    }

    private ResultActions rate(String customerToken, String id, int score) throws Exception {
        return rate(customerToken, id, score, null);
    }

    private ResultActions rate(String customerToken, String id, int score, String header) throws Exception {
        return rate(customerToken, id, score, header, "rate_" + UUID.randomUUID());
    }

    /** 客人端评分真实请求形状（蛇形键）；header 非空时另带 X-Project-Id。 */
    private ResultActions rate(String customerToken, String id, int score, String header, String idempotencyKey) throws Exception {
        var request = post("/api/v1/customer/complaints/" + id + "/satisfaction")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("idempotency_key", idempotencyKey, "score", score)));
        if (header != null) {
            request.header("X-Project-Id", header);
        }
        return mockMvc.perform(request);
    }

    private ResultActions staffPost(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new java.util.LinkedHashMap<>(body);
        actual.putIfAbsent("idempotency_key", "cc_staff_" + UUID.randomUUID());
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(actual)));
    }

    private static MockMultipartFile file(String name, String type) {
        return new MockMultipartFile("files", name, type, new byte[] {1, 2, 3, 4});
    }

    private String customer(String id) {
        Instant now = Instant.now();
        String phone = "136" + String.format("%08d", Math.floorMod(id.hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(id, phone, "136****" + phone.substring(7), "active", null, null, now, now));
        String customerToken = "tok_" + id;
        customers.saveSession(new CustomerSessionRecord("sess_" + id, id, customerToken, "miniapp", now, now.plusSeconds(3600), "active"));
        return customerToken;
    }

    private String grant(String userId, String... permissions) {
        if (granted.add(userId)) {
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '投诉回归', 'project', 'active', now())",
                    role, role);
            for (String code : permissions) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
            }
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', ?, NULL, 'active', now())",
                    userId, userId, project);
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        }
        return userId;
    }

    private RequestPostProcessor staff(String userId, String... permissions) {
        grant(userId, permissions);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private static String read(ResultActions result, String path) throws Exception {
        Object value = JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return value == null ? null : value.toString();
    }
}
