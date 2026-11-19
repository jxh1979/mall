package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
 * 第十八批 BC 段第 366 条（真库、真实入口、真实授权链、蛇形键）。ENUM:68「将客人列入黑名单即为关闭其账户，黑名单全平台通用，由保安团队申请、
 * 夜班值班经理批准、娱乐平台负责人批准」；BSM:833「账户关闭的，储值本金退还本人。对需要退还本金的注销或拉黑事项，系统自动生成应退本金事项」；
 * PGM:659「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」。
 * 366-R1：终审人归属娱乐平台公司（不是项目）、不挂商户，经勾了「可以查看全部项目」的角色持终审码，终审批准 200，客人在项目 A、B 的通用卡关闭、
 * 商户卡冻结，三张卡各登记一笔待退（按卡自身的商户与项目）；最高权限作终审人同样 200。修复前 403 CROSS_PROJECT_FORBIDDEN 整笔回滚。
 * 366-R2：申请列表按入口权限码可触及的项目给终审人看；归属项目 C、只经普通角色持终审码的看不到项目 A 的申请；项目 A 保安、当班夜班值班经理照旧可见。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerBlacklistFinalApproverJdbcIntegrationTests {

    private static final String APPROVE = "customer.blacklist.approve";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccessRepository accessRepository;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String projectA = "r366prj_a_" + s;
    private final String projectB = "r366prj_b_" + s;
    private final String projectC = "r366prj_c_" + s;
    private final String merchant = "r366mch_" + s;
    private final String customer = "r366cus_" + s;
    private StaffRequestFixture staffs;
    private RequestPostProcessor guard;
    private RequestPostProcessor nightManager;
    private String generalA;
    private String generalB;
    private String merchantCard;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB, projectC)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    project, "拉黑终审回归综合体" + project);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                merchant, "拉黑终审回归酒吧" + s, projectA);
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        Instant now = Instant.now();
        customerRepository.saveCustomer(new CustomerRecord(customer, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        generalA = card("ga", projectA, null, 10_000L, 2_000L);
        generalB = card("gb", projectB, null, 5_000L, 0L);
        merchantCard = card("m", projectA, merchant, 3_000L, 0L);
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(merchant, true, 10_000, now, now));

        staffs = new StaffRequestFixture(jdbc);
        guard = staffs.staff("r366usr_guard_" + s, "staff", projectA, null, "customer.blacklist.request");
        String night = "r366usr_night_" + s;
        nightManager = staffs.staff(night, "staff", projectA, null);
        // 项目 A 此刻在班的夜班值班经理（已发布排班），夹具同 CustomerAccountStatusJdbcIntegrationTests。
        jdbc.update("INSERT INTO staff_profiles (id, project_id, name, user_id, status, registration_status) VALUES (?, ?, '夜班经理', ?, 'active', 'approved')",
                "r366sp_" + s, projectA, night);
        jdbc.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, project_id, night_duty_project_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())""", "r366shift_" + s, night,
                java.sql.Date.valueOf(java.time.LocalDate.now(com.complex.platform.common.LocalZone.ID)),
                Timestamp.from(now.minusSeconds(3600)), Timestamp.from(now.plusSeconds(3600)), projectA, projectA);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** 366-R1：娱乐平台负责人终审 200，全平台各项目的卡按卡自身归属登记待退。 */
    @Test
    void entertainmentPlatformHeadApprovesAndEveryCardGetsAPendingRefund() throws Exception {
        String request = requestAndNightApprove("h");
        approve(entertainmentPlatformHead(), request, "h2").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("approved"));
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customer)).isEqualTo("blacklisted");
        assertThat(cardStatus(generalA)).isEqualTo("closed");
        assertThat(cardStatus(generalB)).isEqualTo("closed");
        assertThat(cardStatus(merchantCard)).isEqualTo("frozen");
        List<Map<String, Object>> refunds = jdbc.queryForList("SELECT target_id, merchant_id, project_id, source_type, status FROM finance_refunds"
                + " WHERE target_id IN (?, ?, ?) ORDER BY target_id", generalA, generalB, merchantCard);
        assertThat(refunds).hasSize(3).allSatisfy(row -> {
            assertThat(row.get("source_type")).isEqualTo("stored_value_account");
            assertThat(row.get("status")).isEqualTo("requested");
        });
        assertThat(refunds).anySatisfy(row -> assertThat(row).containsEntry("target_id", generalA).containsEntry("project_id", projectA)
                .containsEntry("merchant_id", null));
        assertThat(refunds).anySatisfy(row -> assertThat(row).containsEntry("target_id", generalB).containsEntry("project_id", projectB)
                .containsEntry("merchant_id", null));
        assertThat(refunds).anySatisfy(row -> assertThat(row).containsEntry("target_id", merchantCard).containsEntry("merchant_id", merchant));
    }

    /** 366-R1：最高权限作终审人同样 200，三张卡各登记一笔待退。 */
    @Test
    void masterAsFinalApproverClosesTheAccount() throws Exception {
        String request = requestAndNightApprove("m");
        approve(master(), request, "m2").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("approved"));
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customer)).isEqualTo("blacklisted");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_refunds WHERE target_id IN (?, ?, ?) AND status = 'requested'",
                Integer.class, generalA, generalB, merchantCard)).isEqualTo(3);
    }

    /** 366-R2：① 娱乐平台负责人看得到项目 A 的待批申请；② 项目 C 只经普通角色持终审码的看不到；③④ 项目 A 保安与当班夜班值班经理照旧可见。 */
    @Test
    void requestListFollowsTheEntryPermissionsReach() throws Exception {
        String request = requestAndNightApprove("l");
        assertThat(this.<String>listIds(entertainmentPlatformHead())).contains(request);
        RequestPostProcessor approverOfC = staffs.staff("r366usr_c_" + s, "staff", projectC, null, APPROVE);
        assertThat(this.<String>listIds(approverOfC)).doesNotContain(request);
        assertThat(this.<String>listIds(guard)).contains(request);
        assertThat(this.<String>listIds(nightManager)).contains(request);
    }

    // ---------- 夹具与请求 ----------

    private String card(String tag, String projectId, String merchantId, long principalCent, long bonusCent) {
        String id = "r366sv_" + tag + "_" + s;
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(id, customer, projectId, merchantId,
                principalCent + bonusCent, "active", Instant.now(), Instant.now(), merchantId == null ? null : "merchant_prepaid",
                principalCent, bonusCent));
        return id;
    }

    /** 项目 A 保安申请列入黑名单，项目 A 当班夜班值班经理批准第一步；返回申请编号。 */
    private String requestAndNightApprove(String tag) throws Exception {
        String body = mockMvc.perform(post("/api/v1/customer-accounts/" + customer + "/blacklist-requests").with(guard)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r366-req-" + tag + s + "\",\"action\":\"list\",\"reason\":\"店内斗殴\",\"project_id\":\""
                                + projectA + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String request = JsonPath.read(body, "$.data.id");
        approve(nightManager, request, tag + "1").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending"));
        return request;
    }

    private ResultActions approve(RequestPostProcessor who, String request, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/customer-blacklist-requests/" + request + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("{\"idempotency_key\":\"r366-ap-" + key + s + "\"}"));
    }

    private <T> List<T> listIds(RequestPostProcessor who) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/api/v1/customer-blacklist-requests?status=pending").with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data[*].id");
    }

    private String cardStatus(String accountId) {
        return jdbc.queryForObject("SELECT status FROM customer_stored_value_accounts WHERE id = ?", String.class, accountId);
    }

    /** 娱乐平台负责人：归属娱乐平台公司组织（域主管公司，不是项目）、不挂商户，经勾了「可以查看全部项目」的可跨项目角色持终审码。 */
    private RequestPostProcessor entertainmentPlatformHead() {
        String role = "role_r366_head_" + s;
        accessRepository.createRole(new RoleRecord(role, "娱乐平台负责人回归角色" + s, null, "project", "active", Instant.now(),
                List.of(APPROVE), null, true, List.of()));
        String userId = "r366usr_head_" + s;
        String organization = "r366ent_" + s;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", organization, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, role);
        return session(new AccessActor(userId, userId, "staff", organization, null, "active"));
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private static RequestPostProcessor master() {
        return session(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"));
    }

    private static RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
