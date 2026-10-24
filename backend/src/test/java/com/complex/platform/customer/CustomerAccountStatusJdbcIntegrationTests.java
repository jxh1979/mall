package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
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
 * ROOT-03-R23 会员身份状态与 HTML 对齐（真库）。依据：
 * BSM 第 819 行「受限：暂停该客人的自助订台资格与员工为其代订的资格，其余功能不受影响……受限满后台配置时长（业务参数，按项目可配，出厂默认 90 天，
 * 须同时配置上下限）自动恢复有效，客服主管也可提前人工解除，填原因留痕。黑名单不设自动到期。解除黑名单须按列入时同级审批链（夜班值班经理批准、
 * 娱乐平台负责人批准）复核，填原因留痕；解除后原账户保持已关闭，该手机号自解除时起允许重新注册」；
 * ENUM 第 68 行「将客人列入黑名单即为关闭其账户，黑名单全平台通用，由保安团队申请、夜班值班经理批准、娱乐平台负责人批准」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerAccountStatusJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private CustomerAccountStatusService statusService;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final Instant now = Instant.now();
    private String project;
    private String nightUser;
    private String customerId;
    private String phone;
    private String token;
    private final java.util.Set<String> granted = new java.util.HashSet<>();

    @BeforeEach
    void seed() {
        project = "prj_r23_" + suffix;
        nightUser = "usr_r23_night_" + suffix;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', '会员状态综合体', 'active', now(), now())", project);
        // 本项目此刻在班的夜班值班经理（已发布排班）。
        jdbc.update("INSERT INTO staff_profiles (id, project_id, name, user_id, status, registration_status) VALUES (?, ?, '夜班经理', ?, 'active', 'approved')",
                "sp_r23_" + suffix, project, nightUser);
        jdbc.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, project_id, night_duty_project_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())""", "shift_r23_" + suffix, nightUser,
                java.sql.Date.valueOf(java.time.LocalDate.now(com.complex.platform.common.LocalZone.ID)),
                Timestamp.from(now.minusSeconds(3600)), Timestamp.from(now.plusSeconds(3600)), project, project);
        customerId = "cust_r23_" + suffix;
        phone = "13" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
        token = "cust_token_r23_" + suffix;
        customerRepository.saveCustomer(new CustomerRecord(customerId, phone, "13*******" + phone.substring(9), "active",
                null, now, now, now));
        customerRepository.saveSession(new CustomerSessionRecord("sess_r23_" + suffix, customerId, token, "miniapp",
                now, now.plusSeconds(3600), "active"));
    }

    @AfterEach
    void clear() {
        TestSecurityActors.clear();
    }

    // ---------- 受限 ----------

    @Test
    void restrictedCustomerCanLoginAndBuyButCannotReserve() throws Exception {
        jdbc.update("UPDATE customers SET status = 'suspended', suspended_until = ? WHERE id = ?",
                Timestamp.from(now.plus(Duration.ofDays(10))), customerId);
        // 其余功能不受影响：会话照常、可登录、可看订单。
        mockMvc.perform(get("/api/v1/customer/orders").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/customer/auth/phone-login").header("X-Terminal-Type", "miniapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("phone", phone, "sms_code", "000000"))))
                .andExpect(status().isOk());
        // 只停自助订台。
        mockMvc.perform(post("/api/v1/customer/bar-reservations").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("idempotency_key", "r23_resv_" + suffix, "bar_id", "bar_x",
                                "table_id", "table_x", "table_type", "booth", "guest_count", 2))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_RESTRICTED"));
    }

    @Test
    void restrictionUntilFollowsProjectSetting() throws Exception {
        // 出厂 90 天（项目未配置时回落出厂行）。按后台真实请求形状（蛇形键）。
        suspend(staff("usr_r23_cs_" + suffix, "customer.account.manage"), Map.of("project_id", project, "reason", "多次扰乱秩序"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("suspended"));
        Instant until = jdbc.queryForObject("SELECT suspended_until FROM customers WHERE id = ?", Timestamp.class, customerId).toInstant();
        assertThat(Duration.between(now, until).toDays()).isBetween(89L, 90L);

        // 本项目配成 30 天（值与上下限同配）后再置入受限，按 30 天算。
        mockMvc.perform(put("/api/v1/customer-restriction-settings/" + project).with(staff("usr_r23_ops_" + suffix, "customer.account.manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("idempotency_key", "r23_rs_" + suffix,
                                "duration_days", 30, "min_days", 7, "max_days", 180))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duration_days").value(30));
        // 上一次受限已到期、客人尚无动作（状态未懒恢复）：按有效会员可再次置入。
        jdbc.update("UPDATE customers SET suspended_until = ? WHERE id = ?", Timestamp.from(now.minusSeconds(60)), customerId);
        suspend(staff("usr_r23_cs_" + suffix, "customer.account.manage"), Map.of("project_id", project, "reason", "再次扰乱秩序"))
                .andExpect(status().isOk());
        until = jdbc.queryForObject("SELECT suspended_until FROM customers WHERE id = ?", Timestamp.class, customerId).toInstant();
        assertThat(Duration.between(now, until).toDays()).isBetween(29L, 30L);
    }

    @Test
    void restrictionSettingRequiresValueAndBoundsTogether() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>(Map.of("idempotency_key", "r23_rs_bad_" + suffix,
                "duration_days", 30, "min_days", 7));
        mockMvc.perform(put("/api/v1/customer-restriction-settings/" + project).with(staff("usr_r23_ops_" + suffix, "customer.account.manage"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        body.put("max_days", 20);
        body.put("idempotency_key", "r23_rs_bad2_" + suffix);
        mockMvc.perform(put("/api/v1/customer-restriction-settings/" + project).with(staff("usr_r23_ops_" + suffix, "customer.account.manage"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void suspendRequiresReason() throws Exception {
        suspend(staff("usr_r23_cs_" + suffix, "customer.account.manage"), Map.of("project_id", project))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customerId)).isEqualTo("active");
    }

    // ---------- 黑名单两级批准 ----------

    @Test
    void blacklistNeedsNightDutyThenFinalApproval() throws Exception {
        String requester = "usr_r23_sec_" + suffix;
        String requestId = text(mockMvc.perform(post("/api/v1/customer-accounts/" + customerId + "/blacklist-requests")
                        .with(staff(requester, "customer.blacklist.request")).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("idempotency_key", "r23_bl_" + suffix,
                                "action", "list", "reason", "店内斗殴", "project_id", project))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending")), "/data/id");

        // 申请人不得自批；非本项目此刻在班的夜班值班经理不得批第一步。
        as(requester);
        assertThatThrownBy(() -> statusService.approveBlacklistRequest(requestId, "r23_a0_" + suffix))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_BLACKLIST_SELF_APPROVAL"));
        as("usr_r23_other_" + suffix);
        assertThatThrownBy(() -> statusService.approveBlacklistRequest(requestId, "r23_a1_" + suffix))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("NIGHT_DUTY_MANAGER_REQUIRED"));

        // 夜班值班经理批准第一步：仍待终审，账户不变。
        as(nightUser);
        CustomerBlacklistRequestRecord first = statusService.approveBlacklistRequest(requestId, "r23_a2_" + suffix);
        assertThat(first.status()).isEqualTo("pending");
        assertThat(first.nightManagerApprovedBy()).isEqualTo(nightUser);
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customerId)).isEqualTo("active");

        // 同一人不能再批终审；娱乐平台负责人终审后才关闭账户。
        assertThatThrownBy(() -> statusService.approveBlacklistRequest(requestId, "r23_a3_" + suffix))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_BLACKLIST_SELF_APPROVAL"));
        as(grant("usr_r23_head_" + suffix, "customer.blacklist.approve"));
        CustomerBlacklistRequestRecord done = statusService.approveBlacklistRequest(requestId, "r23_a4_" + suffix);
        assertThat(done.status()).isEqualTo("approved");
        assertThat(done.platformHeadApprovedBy()).isEqualTo("usr_r23_head_" + suffix);
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customerId)).isEqualTo("blacklisted");
        // 已关闭：客人动作即被拒（立即撤权）。
        mockMvc.perform(get("/api/v1/customer/orders").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_BLACKLISTED"));
    }

    @Test
    void releasedBlacklistKeepsAccountClosedAndPhoneReRegisters() throws Exception {
        jdbc.update("UPDATE customers SET status = 'blacklisted' WHERE id = ?", customerId);
        as(grant("usr_r23_sec_" + suffix, "customer.blacklist.request"));
        CustomerBlacklistRequestRecord request = statusService.requestBlacklistChange(customerId,
                new CustomerBlacklistRequestCommand("r23_rel_" + suffix, "release", "复核误列", project));
        as(nightUser);
        statusService.approveBlacklistRequest(request.id(), "r23_rel_a1_" + suffix);
        as(grant("usr_r23_head_" + suffix, "customer.blacklist.approve"));
        statusService.approveBlacklistRequest(request.id(), "r23_rel_a2_" + suffix);
        TestSecurityActors.clear();

        Map<String, Object> row = jdbc.queryForMap("SELECT status, phone, phone_hash, masked_phone FROM customers WHERE id = ?", customerId);
        assertThat(row.get("status")).isEqualTo("blacklisted");
        assertThat(row.get("phone")).isNull();
        assertThat(row.get("phone_hash")).isNull();
        assertThat(row.get("masked_phone")).isNull();

        String terms = mockMvc.perform(get("/api/v1/customer/terms").header("X-Terminal-Type", "miniapp"))
                .andReturn().getResponse().getContentAsString();
        var data = JSON.readTree(terms).path("data");
        mockMvc.perform(post("/api/v1/customer/auth/register").header("X-Terminal-Type", "miniapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("phone", phone, "sms_code", "000000", "birth_date", "1990-05-01",
                                "member_terms_version", data.path("member_terms").path("version").asText(),
                                "privacy_terms_version", data.path("privacy_policy").path("version").asText()))))
                .andExpect(status().isOk());
        assertThat(customerRepository.findCustomerByPhone(phone)).get()
                .extracting(CustomerRecord::id).isNotEqualTo(customerId);
    }

    @Test
    void directBlacklistEndpointRemovedAndReinstateCannotReopenClosedAccount() throws Exception {
        RequestPostProcessor admin = staff("usr_r23_cs_" + suffix, "customer.account.manage");
        mockMvc.perform(post("/api/v1/customer-accounts/" + customerId + "/blacklist").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"直接拉黑\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customerId)).isEqualTo("active");

        jdbc.update("UPDATE customers SET status = 'blacklisted' WHERE id = ?", customerId);
        mockMvc.perform(post("/api/v1/customer-accounts/" + customerId + "/reinstate").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"直接恢复\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_BLACKLIST_RELEASE_REQUIRES_APPROVAL"));
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customerId)).isEqualTo("blacklisted");
    }

    @Test
    void pendingRequestsListedForApproval() {
        as(grant("usr_r23_sec_" + suffix, "customer.blacklist.request"));
        CustomerBlacklistRequestRecord request = statusService.requestBlacklistChange(customerId,
                new CustomerBlacklistRequestCommand("r23_ls_" + suffix, "list", "店内斗殴", project));
        List<CustomerBlacklistRequestRecord> pending = statusService.listBlacklistRequests("pending");
        assertThat(pending).extracting(CustomerBlacklistRequestRecord::id).contains(request.id());
        // 同一客人同时只有一笔待批申请。
        assertThatThrownBy(() -> statusService.requestBlacklistChange(customerId,
                new CustomerBlacklistRequestCommand("r23_ls2_" + suffix, "list", "重复", project)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_BLACKLIST_REQUEST_PENDING"));
    }

    // ---------- helpers ----------

    private ResultActions suspend(RequestPostProcessor who, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        actual.putIfAbsent("idempotency_key", "r23_sus_" + UUID.randomUUID());
        return mockMvc.perform(post("/api/v1/customer-accounts/" + customerId + "/suspend").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(actual)));
    }

    /** 本项目员工账号（按真实授权链：角色→权限码）；同一用户只建一次。 */
    private String grant(String userId, String... permissions) {
        if (granted.add(userId)) {
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '会员状态回归', 'project', 'active', now())",
                    role, role);
            for (String code : permissions) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
            }
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', ?, NULL, 'active', now())",
                    userId, userId, project);
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
            jdbc.update("INSERT INTO staff_level_assignments (user_id, staff_level_id, organization_id) VALUES (?, 'lvl_m3_upper', ?)", userId, project);
        }
        return userId;
    }

    private void as(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", project, null, "active"), null, List.of()));
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

    private static String text(ResultActions actions, String pointer) throws Exception {
        return JSON.readTree(actions.andReturn().getResponse().getContentAsString()).at(pointer).asText();
    }
}
