package com.complex.platform.merchantservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantServiceJdbcIntegrationTests {

    private static final String POLICIES = "/api/v1/merchant-service/work-order-policies";

    @Autowired
    private JdbcMerchantServiceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantServiceService service;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String merchant = "mch_77_" + suffix;
    private final String merchantName = "七七超时商户" + suffix;
    private RequestPostProcessor merchantAccount;
    private RequestPostProcessor dispatcherA;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void migrationCreatesMerchantServiceTablesAndSeedsPermissions() {
        assertThat(regclass("merchant_service_work_orders")).isEqualTo("merchant_service_work_orders");
        assertThat(regclass("merchant_service_work_order_actions")).isEqualTo("merchant_service_work_order_actions");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());

        assertThat(permissionCodes()).contains(
                "merchant.service_work_order.view",
                "merchant.service_work_order.create",
                "merchant.service_work_order.assign",
                "merchant.service_work_order.process",
                "merchant.service_work_order.close"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "merchant.service_work_order.create",
                "merchant.service_work_order.assign",
                "merchant.service_work_order.process",
                "merchant.service_work_order.close"
        );
    }

    @Test
    void saveAndListWorkOrdersAndActionsThroughPostgres() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveWorkOrder(new MerchantWorkOrderRecord(
                "mswo_jdbc",
                "mch_jdbc",
                "sp_jdbc",
                "ct_jdbc",
                "repair",
                "high",
                "created",
                "Water leak",
                "Shop ceiling is leaking",
                "staff_reporter",
                "web",
                null,
                null,
                Instant.parse("2026-06-15T08:00:00Z"),
                null,
                null,
                null,
                List.of("att-photo-1", "att-photo-2"),
                false,
                null,
                now,
                now
        ));
        repository.saveAction(new MerchantWorkOrderActionRecord(
                "mswo_action_jdbc",
                "mswo_jdbc",
                "created",
                null,
                "created",
                "staff_reporter",
                "create work order",
                List.of("att-photo-1"),
                now
        ));

        MerchantWorkOrderRecord assigned = repository.saveWorkOrder(repository.findWorkOrderById("mswo_jdbc")
                .orElseThrow()
                .assign("property", "staff_owner", Instant.parse("2026-06-14T01:00:00Z")));
        repository.saveAction(new MerchantWorkOrderActionRecord(
                "mswo_action_assign_jdbc",
                "mswo_jdbc",
                "assigned",
                "created",
                "assigned",
                "staff_manager",
                "assign 流转到 property",
                List.of("att-assign"),
                Instant.parse("2026-06-14T01:00:00Z")
        ));

        assertThat(assigned.status()).isEqualTo("assigned");
        assertThat(repository.findWorkOrderById("mswo_jdbc"))
                .get()
                .extracting(
                        MerchantWorkOrderRecord::merchantId,
                        MerchantWorkOrderRecord::status,
                        MerchantWorkOrderRecord::ownerDepartment,
                        MerchantWorkOrderRecord::ownerUserId
                )
                .containsExactly("mch_jdbc", "assigned", "property", "staff_owner");
        assertThat(repository.findWorkOrdersByScope(Optional.empty(), 100))
                .extracting(MerchantWorkOrderRecord::id, MerchantWorkOrderRecord::workOrderType, MerchantWorkOrderRecord::priority)
                .contains(tuple("mswo_jdbc", "repair", "high"));
        assertThat(repository.findActionsByWorkOrderId("mswo_jdbc"))
                .extracting(MerchantWorkOrderActionRecord::actionType, MerchantWorkOrderActionRecord::toStatus)
                .contains(tuple("created", "created"), tuple("assigned", "assigned"));
    }

    // ── 第十八批 I 段第 77 条 77-R3／77-R2：真库＋真实入口＋真实授权链，请求体一律按前端蛇形键 ──
    // BSM:1385「响应/处理时限按「工单类型×优先级」矩阵后台配置，超时自动升级通知主管」「处理时限对商户公开；商户服务工单不设首次响应环节」；
    // ENUM:264「（商户专区发起；超过时限自动升级并标记）」；ENUM:265「超时自动升级通知主管」；
    // 裁决 M737-iter7「商户服务工单按商户的经营项目（主项目或追加的经营项目在可触及项目内）」；PGM:528「④看不到别的商户和别的项目的数据」。

    /**
     * 77-R3：平台员工按类型×优先级配处理时限并能列出；商户账号改不了时限；商户专区建单的截止时间＝建单时刻＋处理时限（请求体带 due_at 也不认）；
     * 未配置的组合没有截止时间，作业跑一轮不升级。
     */
    @Test
    void handleMinutesPolicyDrivesMerchantWorkOrderDueAt() throws Exception {
        seedOperatingMerchant();
        savePolicy(dispatcherA, "repair", "high", 120)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.work_order_type").value("repair"))
                .andExpect(jsonPath("$.data.priority").value("high"))
                .andExpect(jsonPath("$.data.handle_minutes").value(120));
        mockMvc.perform(get(POLICIES).with(dispatcherA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.work_order_type=='repair' && @.priority=='high')].handle_minutes").value(hasItem(120)));
        savePolicy(merchantAccount, "repair", "high", 30).andExpect(status().isForbidden());

        String id = createAsMerchant("repair", "high", ",\"due_at\":\"2020-01-01T00:00:00Z\"");
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT created_at, due_at FROM merchant_service_work_orders WHERE id = ?", id);
        assertThat(row.get("due_at")).isNotNull();
        assertThat(Duration.between(((Timestamp) row.get("created_at")).toInstant(), ((Timestamp) row.get("due_at")).toInstant()))
                .isEqualTo(Duration.ofMinutes(120));

        String unconfigured = createAsMerchant("complaint", "low", "");
        assertThat(jdbcTemplate.queryForObject("SELECT due_at FROM merchant_service_work_orders WHERE id = ?", Timestamp.class, unconfigured))
                .isNull();
        new MerchantWorkOrderSlaEscalationJob(service, Clock.systemUTC()).scheduledEscalate();
        assertThat(jdbcTemplate.queryForObject("SELECT escalated_at FROM merchant_service_work_orders WHERE id = ?", Timestamp.class, unconfigured))
                .isNull();
    }

    /**
     * 77-R2：商户 M 主项目 A、追加经营项目 B；A、B、C 各一名持派单权限、有员工档案的平台员工。M 的单超过处理时限，作业升级并标记，
     * 同时给 A、B 员工各一条系统通知（标题含工单标题、正文含商户名与时限），C 员工与持派单码的商户账号都不通知；再跑一轮不重复通知。
     */
    @Test
    void escalationNotifiesDispatchersReachingTheMerchantsOperatingProjects() throws Exception {
        seedOperatingMerchant();
        savePolicy(dispatcherA, "repair", "high", 120).andExpect(status().isOk());
        String id = createAsMerchant("repair", "high", "");
        jdbcTemplate.update("UPDATE merchant_service_work_orders SET due_at = now() - interval '1 minute' WHERE id = ?", id);

        MerchantWorkOrderSlaEscalationJob job = new MerchantWorkOrderSlaEscalationJob(service, Clock.systemUTC());
        job.scheduledEscalate();

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT sla_breached, escalated_at FROM merchant_service_work_orders WHERE id = ?", id);
        assertThat(row.get("sla_breached")).isEqualTo(true);
        assertThat(row.get("escalated_at")).isNotNull();
        assertThat(repository.findActionsByWorkOrderId(id)).extracting(MerchantWorkOrderActionRecord::actionType).contains("escalated");
        for (String tag : List.of("a", "b")) {
            assertThat(notices(tag)).singleElement().satisfies(notice -> {
                assertThat(notice).containsEntry("notice_type", "system_notice");
                assertThat((String) notice.get("title")).contains("漏水" + suffix);
                assertThat((String) notice.get("body")).contains(merchantName).contains("处理时限");
            });
        }
        assertThat(notices("c")).isEmpty();
        assertThat(notices("m")).isEmpty();

        job.scheduledEscalate();
        assertThat(notices("a")).hasSize(1);
        assertThat(notices("b")).hasSize(1);
    }

    /** 项目 A、B、C；商户 M 主项目 A、已批准追加经营项目 B；A、B、C 各一名派单员（角色只到本人项目、有员工档案）；M 的商户账号也持派单码、有档案。 */
    private void seedOperatingMerchant() {
        for (String tag : List.of("a", "b", "c")) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                    project(tag), "77 " + project(tag));
        }
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'retail', 'active', now(), now(), ?)", merchant, merchantName, project("a"));
        jdbcTemplate.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", merchant, project("b"));
        String dispatch = role("dispatch", "merchant.service_work_order.view", "merchant.service_work_order.assign");
        dispatcherA = account("a", project("a"), null, dispatch);
        account("b", project("b"), null, dispatch);
        account("c", project("c"), null, dispatch);
        merchantAccount = account("m", project("a"), merchant, role("merchant", "merchant.service_work_order.view",
                "merchant.service_work_order.create", "merchant.service_work_order.assign"));
    }

    private ResultActions savePolicy(RequestPostProcessor who, String type, String priority, int minutes) throws Exception {
        return mockMvc.perform(put(POLICIES).with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"%s","priority":"%s","handle_minutes":%d,"idempotency_key":"%s"}
                """.formatted(type, priority, minutes, UUID.randomUUID())));
    }

    /** 照商户专区提单页（MerchantZoneWorkOrdersPage）的请求形状，extra 为追加的键。 */
    private String createAsMerchant(String type, String priority, String extra) throws Exception {
        String body = """
                {"idempotency_key":"%s","operator_note":"商户专区自助提交工单","work_order_type":"%s","priority":"%s",
                 "title":"漏水%s","description":"店内天花板漏水","reported_channel":"merchant_portal"%s}
                """.formatted(UUID.randomUUID(), type, priority, suffix, extra);
        return JsonPath.read(mockMvc.perform(post("/api/v1/merchant-service/work-orders").with(merchantAccount)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private List<Map<String, Object>> notices(String tag) {
        return jdbcTemplate.queryForList("""
                SELECT n.notice_type, n.title, n.body FROM employee_notices n JOIN staff_profiles p ON p.id = n.staff_profile_id WHERE p.user_id = ?
                """, user(tag));
    }

    private String role(String tag, String... permissionCodes) {
        String roleId = "role_77_" + tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '77 回归', 'project', 'active', now())",
                roleId, roleId);
        for (String code : permissionCodes) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        return roleId;
    }

    /** 员工账号（account_type staff；merchantId 为空＝平台员工）＋一行已批准的员工档案。 */
    private RequestPostProcessor account(String tag, String projectId, String merchantId, String roleId) {
        String userId = user(tag);
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES (?, ?, 'staff', ?, ?, 'active', now())", userId, userId, projectId, merchantId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 'approved', 'active', now(), now())", "stf_77_" + tag + "_" + suffix, projectId, merchantId, userId, tag);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String project(String tag) {
        return "prj_77_" + tag + "_" + suffix;
    }

    private String user(String tag) {
        return "usr_77_" + tag + "_" + suffix;
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = true
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                 ORDER BY permission_code
                """, String.class);
    }
}
