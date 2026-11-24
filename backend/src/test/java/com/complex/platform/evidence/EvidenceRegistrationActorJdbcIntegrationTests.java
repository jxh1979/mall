package com.complex.platform.evidence;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.AuthService;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.bar.BarProductRecord;
import com.complex.platform.bar.BarRepository;
import com.complex.platform.bar.BarSoldOutRestoreJob;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CH 段第 411 条 411-R1（PGM:683「审计人员查看或导出敏感信息（资金流水、人脸与身份等个人信息、完整操作记录导出）需先经审批并全程留痕。」；
 * BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」）：审计事件页、准入审计页的导出登记，商户归属取登记人——不挂商户的稽核与
 * 最高权限记空商户、不再 403 MERCHANT_SCOPE_REQUIRED，商户账号照旧记本商户。
 * 第 412 条 412-R1（PGM:899「平台治理层要保证每个重要动作能追溯。谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」）：
 * 没有登录主体的定时作业记 system「系统作业」，开放平台第三方回调记 third_party「第三方系统」；控制席并入的同根项——未登录请求按真实主体记：
 * 登录、续期成功记该账号，客人端未登录请求记 customer「客人」，柜机上报记 device「设备」与设备编号（收尾复核后控制席补充），
 * 其余未登录请求记 anonymous「匿名访问」。
 * 真库＋真实接口＋真实角色授权链，夹具随测试事务回滚（失败留痕走独立事务提交，键唯一或按本用例开始时刻取）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class EvidenceRegistrationActorJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private BarSoldOutRestoreJob soldOutRestoreJob;
    @Autowired private AuthService authService;

    private final String s = String.valueOf(System.nanoTime());
    private final String p1 = "b106_p1_" + s;
    private final String m1 = "b106_m1_" + s;
    private StaffRequestFixture staffs;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','登记归属综合体','active',now(),now())", p1);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '登记归属酒吧', 'bar', ?, 'active', now(), now())", m1, p1);
        staffs = new StaffRequestFixture(jdbc);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ---------- 411-R1 ----------

    @Test
    void nonMerchantAuditorRegistersExportWithoutMerchant() throws Exception {
        String auditorId = "usr_b106_aud_" + s;
        RequestPostProcessor auditor = staffs.staff(auditorId, "staff", p1, null, "evidence.event.create", "evidence.event.view");
        String key = "b106-aud-" + s;
        String summary = "审计事件页导出登记" + s;
        register(auditor, key, auditorId, "audit_events_exported", "audit", summary)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(1));
        assertThat(merchantOf(summary)).isNull();
        Map<String, Object> trace = jdbc.queryForMap("SELECT event_result, actor_id, actor_type FROM audit_events"
                + " WHERE event_name = 'evidence.events_reported' AND idempotency_key = ?", key);
        assertThat(trace).containsEntry("event_result", "success").containsEntry("actor_id", auditorId).containsEntry("actor_type", "staff");

        String admissionSummary = "准入审计页导出登记" + s;
        register(auditor, "b106-adm-" + s, auditorId, "merchant.admission.audit_exported", "governance", admissionSummary)
                .andExpect(status().isOk());
        assertThat(merchantOf(admissionSummary)).isNull();
    }

    @Test
    void masterRegistersExportWithoutMerchant() throws Exception {
        String summary = "最高权限导出登记" + s;
        register(master(), "b106-master-" + s, SessionIssuer.MASTER_USER_ID, "audit_events_exported", "audit", summary)
                .andExpect(status().isOk());
        assertThat(merchantOf(summary)).isNull();
    }

    @Test
    void merchantUserStillStampsOwnMerchant() throws Exception {
        String userId = "usr_b106_m1_" + s;
        RequestPostProcessor merchantUser = staffs.staff(userId, "staff", p1, m1, "evidence.event.create");
        String summary = "商户员工登记" + s;
        register(merchantUser, "b106-m1-" + s, userId, "audit_events_exported", "audit", summary)
                .andExpect(status().isOk());
        assertThat(merchantOf(summary)).isEqualTo(m1);
    }

    // ---------- 412-R1 ----------

    @Test
    void scheduledJobTraceShowsSystem() throws Exception {
        String product = soldOutProduct();
        SecurityContextHolder.clearContext();
        soldOutRestoreJob.restoreExpired(Instant.now());

        JsonNode row = firstOperationEvent("bar_product", product);
        assertThat(row.path("event_name").asText()).isEqualTo("bar.product_sold_out_auto_restored");
        assertThat(row.path("actor_type").asText()).isEqualTo("system");
        assertThat(row.hasNonNull("actor_id")).isFalse();

        JsonNode actorTypes = body(mockMvc.perform(get("/api/v1/audit/labels").with(master())).andExpect(status().isOk()))
                .at("/data/actor_types");
        Map<String, String> names = new LinkedHashMap<>();
        actorTypes.forEach(label -> names.put(label.path("code").asText(), label.path("name").asText()));
        assertThat(names).containsEntry("system", "系统作业").containsEntry("third_party", "第三方系统")
                .containsEntry("anonymous", "匿名访问").containsEntry("device", "设备");
    }

    @Test
    void openPlatformCallbackShowsThirdParty() throws Exception {
        String endpointKey = "b106-open-" + s;
        mockMvc.perform(post("/api/open/d/" + endpointKey + "/webhooks/parking/entry")
                        .header("X-Idempotency-Key", "b106-wh-" + s)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        JsonNode row = firstOperationEvent("open_webhook", endpointKey);
        assertThat(row.path("event_name").asText()).isEqualTo("api_call.rejected");
        assertThat(row.path("actor_type").asText()).isEqualTo("third_party");
    }

    /** 控制席并入：员工登录、续期成功记该账号；登录失败没有主体，记匿名访问。 */
    @Test
    void staffLoginTraceShowsAccountAndFailedLoginShowsAnonymous() throws Exception {
        String userId = "usr_b106_login_" + s;
        String loginName = "b106login" + s;
        String password = "pw-" + UUID.randomUUID();
        authService.provisionUser(userId, loginName, password, "merchant", p1, m1, "active");

        JsonNode login = body(login(loginName, password).andExpect(status().isOk()));
        Map<String, Object> succeeded = jdbc.queryForMap("SELECT actor_type, actor_id FROM audit_events"
                + " WHERE event_name = 'auth.login.succeeded' AND business_id = ?", login.at("/data/session_id").asText());
        assertThat(succeeded).containsEntry("actor_type", "merchant").containsEntry("actor_id", userId);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + login.at("/data/access_token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refresh_token", login.at("/data/refresh_token").asText(),
                                "device_id", "b106-web"))))
                .andExpect(status().isOk());
        Map<String, Object> refreshed = jdbc.queryForMap("SELECT actor_type, actor_id FROM audit_events"
                + " WHERE event_name = 'auth.token.refreshed' AND metadata->>'user_id' = ?", userId);
        assertThat(refreshed).containsEntry("actor_type", "merchant").containsEntry("actor_id", userId);

        Instant before = Instant.now().minusMillis(5);
        login(loginName, password + "-wrong").andExpect(status().isUnauthorized());
        assertThat(latestActorType("auth.login.failed", before)).isEqualTo("anonymous");
    }

    /** 控制席并入：客人手机号注册与登录走客人端未登录接口，记客人。 */
    @Test
    void customerRegistrationAndFailedLoginShowCustomer() throws Exception {
        JsonNode terms = body(mockMvc.perform(get("/api/v1/customer/terms").header("X-Terminal-Type", "app"))
                .andExpect(status().isOk())).path("data");
        Map<String, Object> register = new LinkedHashMap<>();
        register.put("phone", newPhone());
        register.put("sms_code", "000000");
        register.put("birth_date", "1990-05-01");
        register.put("member_terms_version", terms.path("member_terms").path("version").asText());
        register.put("privacy_terms_version", terms.path("privacy_policy").path("version").asText());
        String customerId = body(mockMvc.perform(post("/api/v1/customer/auth/register").header("X-Terminal-Type", "app")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(register)))
                .andExpect(status().isOk())).at("/data/customer_id").asText();
        assertThat(jdbc.queryForObject("SELECT actor_type FROM audit_events WHERE event_name = 'customer.registered' AND business_id = ?",
                String.class, customerId)).isEqualTo("customer");

        Instant before = Instant.now().minusMillis(5);
        mockMvc.perform(post("/api/v1/customer/auth/phone-login").header("X-Terminal-Type", "app")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", newPhone(), "sms_code", "000000"))))
                .andExpect(status().isConflict());
        assertThat(latestActorType("customer.auth.login_failed", before)).isEqualTo("customer");
    }

    /** 控制席补充（PGM:899；全链路操作留痕 PRD §6.5 词表 device）：柜机凭设备令牌上报心跳、不带员工会话，留痕记设备与设备编号。 */
    @Test
    void kioskHeartbeatTraceShowsDevice() throws Exception {
        String deviceId = "b106-kiosk-" + s;
        RequestPostProcessor admin = staffs.staff("usr_b106_bind_" + s, "platform_admin", null, null, "terminal.device.bind");
        Map<String, Object> bind = new LinkedHashMap<>();
        bind.put("idempotency_key", "b106-bind-" + s);
        bind.put("device_id", deviceId);
        bind.put("terminal_type", "kiosk");
        bind.put("binding_scope", "space");
        bind.put("bound_space_id", "b106_point_" + s);
        bind.put("kiosk_mode", "service");
        String token = body(mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(bind)))
                .andExpect(status().isOk())).at("/data/device_token").asText();

        mockMvc.perform(post("/api/v1/terminals/kiosks/" + deviceId + "/heartbeat")
                        .header("X-Terminal-Type", "kiosk").header("X-Device-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("device_id", deviceId, "terminal_type", "kiosk", "status", "online"))))
                .andExpect(status().isOk());

        JsonNode row = firstOperationEvent("terminal_kiosk_heartbeat", deviceId);
        assertThat(row.path("event_name").asText()).isEqualTo("terminal.kiosk_heartbeat");
        assertThat(row.path("actor_type").asText()).isEqualTo("device");
        assertThat(row.path("actor_id").asText()).isEqualTo(deviceId);
    }

    // ---------- helpers ----------

    /** 审计事件页、准入审计页「导出」按钮提交的登记形状（EvidenceEventsApi 蛇形键）。 */
    private ResultActions register(RequestPostProcessor who, String key, String staffId, String eventType, String category,
            String summary) throws Exception {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event_type", eventType);
        event.put("event_category", category);
        event.put("result", "success");
        event.put("summary", summary);
        event.put("occurred_at", Instant.now().toString());
        event.put("payload", Map.of("page", 1));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("device_code", "web-admin");
        body.put("staff_id", staffId);
        body.put("events", List.of(event));
        return mockMvc.perform(post("/api/v1/evidence/events").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String merchantOf(String summary) {
        return jdbc.queryForObject("SELECT merchant_id FROM evidence_events WHERE summary = ?", String.class, summary);
    }

    private ResultActions login(String loginName, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login_name", loginName, "password", password,
                        "device_id", "b106-web", "terminal_type", "web"))));
    }

    /** 失败留痕走独立事务、没有业务编号：取本用例开始后该事件最新一行。 */
    private String latestActorType(String eventName, Instant since) {
        return jdbc.queryForObject("SELECT actor_type FROM audit_events WHERE event_name = ? AND occurred_at >= ?"
                + " ORDER BY occurred_at DESC LIMIT 1", String.class, eventName, Timestamp.from(since));
    }

    /** 操作日志页按业务对象查（最高权限看全部）。 */
    private JsonNode firstOperationEvent(String businessType, String businessId) throws Exception {
        return body(mockMvc.perform(get("/api/v1/audit/operation-events").param("business_type", businessType)
                .param("business_id", businessId).with(master())).andExpect(status().isOk())).at("/data/0");
    }

    /** 酒吧商品临时沽清、到期时刻已过（照 BarSoldOutRestoreJobJdbcIntegrationTests）。 */
    private String soldOutProduct() {
        String id = "b106_prd_" + s;
        Instant at = Instant.now().minus(2, ChronoUnit.HOURS);
        bars.saveProduct(new BarProductRecord(id, m1, m1, "限量威士忌", "spirit", 8800L, "CNY", null, null, false, "active",
                at, at, null, null, null, false, null, null, null)
                .withSoldOut("temporary", at, "今日备货售完", Instant.now().minus(1, ChronoUnit.MINUTES), at));
        return id;
    }

    private JsonNode body(ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private static String newPhone() {
        return "13" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
