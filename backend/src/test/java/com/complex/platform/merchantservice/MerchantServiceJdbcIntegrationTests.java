package com.complex.platform.merchantservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import org.springframework.dao.DataIntegrityViolationException;
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
    private static final String WORK_ORDERS = "/api/v1/merchant-service/work-orders";
    private static final String CLOSE_METHODS = "/api/v1/merchant-service/work-order-close-methods";

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
    private RequestPostProcessor dispatcherB;
    private RequestPostProcessor dispatcherC;
    private RequestPostProcessor processorA;

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
     * 第 93 条 93-R1：单子用保洁/高，正文写「（保洁/高）」。
     */
    @Test
    void escalationNotifiesDispatchersReachingTheMerchantsOperatingProjects() throws Exception {
        seedOperatingMerchant();
        savePolicy(dispatcherA, "cleaning", "high", 120).andExpect(status().isOk());
        String id = createAsMerchant("cleaning", "high", "");
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
                assertThat((String) notice.get("body")).contains(merchantName).contains("（保洁/高）").contains("处理时限");
            });
        }
        assertThat(notices("c")).isEmpty();
        assertThat(notices("m")).isEmpty();

        job.scheduledEscalate();
        assertThat(notices("a")).hasSize(1);
        assertThat(notices("b")).hasSize(1);
    }

    // ── 第十八批 K 段第 93 条 93-R1：商户服务工单类型＝报修/保洁/投诉/其他服务请求 ──
    // BSM:1323「我的工单（报修/保洁/投诉/其他服务请求自己发起……）」；BSM:1385「商户侧可自助提报修、保洁、投诉和其他服务请求，整改与巡检类工单由平台发起」；
    // 裁决 D-12（不含整改、含保洁）、D-13「商户专区自助提的单走 ENUM 商户服务工单链」。

    /** 93-R1：商户专区按提单页蛇形键提保洁单，落 cleaning；提整改单 400 VALIDATION_FAILED，库里不落行。 */
    @Test
    void merchantZoneCreatesCleaningAndRejectsRectification() throws Exception {
        seedOperatingMerchant();
        String id = createAsMerchant("cleaning", "high", "");
        assertThat(jdbcTemplate.queryForObject("SELECT work_order_type FROM merchant_service_work_orders WHERE id = ?", String.class, id))
                .isEqualTo("cleaning");
        postAsMerchant("rectification", "high", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM merchant_service_work_orders WHERE merchant_id = ?", Integer.class, merchant))
                .isEqualTo(1);
    }

    /** 93-R1：处理时限能配保洁（保洁单截止时间＝建单时刻＋处理时限），配整改 400。 */
    @Test
    void cleaningHandleMinutesPolicyAppliesAndRectificationPolicyRejected() throws Exception {
        seedOperatingMerchant();
        savePolicy(dispatcherA, "cleaning", "high", 120).andExpect(status().isOk());
        String id = createAsMerchant("cleaning", "high", "");
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT created_at, due_at FROM merchant_service_work_orders WHERE id = ?", id);
        assertThat(Duration.between(((Timestamp) row.get("created_at")).toInstant(), ((Timestamp) row.get("due_at")).toInstant()))
                .isEqualTo(Duration.ofMinutes(120));
        savePolicy(dispatcherA, "rectification", "high", 120)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    /** 93-R1：库里的 CHECK 不收整改码。 */
    @Test
    void databaseRejectsRectificationWorkOrderType() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO merchant_service_work_orders (id, merchant_id, work_order_type, priority, status, title, description,
                        reported_by_user_id, reported_channel, created_at, updated_at)
                VALUES (?, ?, 'rectification', 'high', 'created', '整改', '整改', 'usr_93', 'web', now(), now())
                """, "mswo_93_" + suffix, merchant)).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── 第十八批 K 段第 93 条 93-R3：非平台管理员的平台员工按可触及项目读、处理商户服务工单 ──
    // BSM:1385「工单由客户关怀部负责处理」；PGM:502「设客户关怀部……并负责工单处理」；PGM:528「④看不到别的商户和别的项目的数据」
    // 「持有可跨项目角色的员工在该角色所含权限内查看、处理该角色可查看项目的数据，不受第④类「别的项目」限制」；
    // 裁决 M737-iter7「商户服务工单按商户的经营项目（主项目或追加的经营项目在可触及项目内）」「对平台管理员不过滤」（D-032）。

    /**
     * 93-R3：商户 M 主项目 A、追加经营项目 B。A、B 员工列表看得到 M 的单，C 员工看不到；C 按后台派单的蛇形键派单 403 CROSS_PROJECT_FORBIDDEN、
     * 单子不动、记派单失败；B 派单成功。平台管理员看得到；C 改持只勾了项目 A 的可跨项目角色后看得到。
     */
    @Test
    void projectStaffReachMerchantWorkOrdersThroughTheMerchantsOperatingProjects() throws Exception {
        seedOperatingMerchant();
        String id = createAsMerchant("repair", "high", "");

        listIds(dispatcherA).andExpect(jsonPath("$.data.items[*].id").value(hasItem(id)));
        listIds(dispatcherB).andExpect(jsonPath("$.data.items[*].id").value(hasItem(id)));
        listIds(dispatcherC).andExpect(jsonPath("$.data.items[*].id").value(not(hasItem(id))));

        assign(dispatcherC, id, user("pa")).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT status, owner_user_id FROM merchant_service_work_orders WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("created");
        assertThat(row.get("owner_user_id")).isNull();
        List<String> events = jdbcTemplate.queryForList("SELECT event_name FROM audit_events WHERE business_id = ?", String.class, id);
        assertThat(events).contains("merchant_service.assignMerchantServiceWorkOrder.failed").doesNotContain("merchant_service.assignMerchantServiceWorkOrder.succeeded");

        assign(dispatcherB, id, user("pa")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("assigned"));

        listIds(account("adm", "platform_admin", project("c"), null, role("adm", "merchant.service_work_order.view")))
                .andExpect(jsonPath("$.data.items[*].id").value(hasItem(id)));

        String cross = role("cross", "merchant.service_work_order.view", "merchant.service_work_order.assign");
        jdbcTemplate.update("INSERT INTO role_viewable_projects (role_id, viewable_id) VALUES (?, ?)", cross, project("a"));
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", user("c"));
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", user("c"), cross);
        listIds(dispatcherC).andExpect(jsonPath("$.data.items[*].id").value(hasItem(id)));
    }

    // ── 第十八批 L 段第 102 条：派单负责人与按类型关单（真库＋真实入口＋真实授权链，请求体一律按前端蛇形键） ──
    // BSM:1380「派单到具体人员或班组」；BSM:1385「验收按工单类型分别配置——保洁等无争议类由处理人完成即结束；……商户投诉须由商户确认投诉已解决；
    // ……问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉」「满意度在回访/验收时由提单方评分」；
    // ENUM:264「待商户确认、已处理待关闭」；第十八批裁决 26（报修、其他服务请求的关单方式按后台配置三选一）。

    /**
     * 102-R1：负责人候选只含在岗、持处理权限、能管到该商户经营项目的平台员工（pa 在项目 A），不含只到项目 C 的 pc、商户账号与只持派单码的派单员；
     * 按后台派单的蛇形键派给不存在的 staff_owner 或 pc 一律 400 WORK_ORDER_OWNER_INVALID、单子不动、记派单失败；派给 pa 成功。
     */
    @Test
    void assignOwnerMustBeOnDutyStaffWhoCanProcessTheMerchantsProjects() throws Exception {
        seedOperatingMerchant();
        String id = createAsMerchant("repair", "high", "");

        mockMvc.perform(get(WORK_ORDERS + "/" + id + "/owner-candidates").with(dispatcherB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].user_id").value(hasItem(user("pa"))))
                .andExpect(jsonPath("$.data[?(@.user_id=='" + user("pa") + "')].staff_name").value(hasItem("pa")))
                .andExpect(jsonPath("$.data[*].user_id").value(not(hasItem(user("pc")))))
                .andExpect(jsonPath("$.data[*].user_id").value(not(hasItem(user("m")))))
                .andExpect(jsonPath("$.data[*].user_id").value(not(hasItem(user("a")))));

        for (String owner : List.of("staff_owner", user("pc"))) {
            assign(dispatcherB, id, owner).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("WORK_ORDER_OWNER_INVALID"));
            Map<String, Object> row = jdbcTemplate.queryForMap("SELECT status, owner_user_id FROM merchant_service_work_orders WHERE id = ?", id);
            assertThat(row.get("status")).isEqualTo("created");
            assertThat(row.get("owner_user_id")).isNull();
        }
        assertThat(jdbcTemplate.queryForList("SELECT event_name FROM audit_events WHERE business_id = ?", String.class, id))
                .contains("merchant_service.assignMerchantServiceWorkOrder.failed").doesNotContain("merchant_service.assignMerchantServiceWorkOrder.succeeded");

        assign(dispatcherB, id, user("pa")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("assigned"))
                .andExpect(jsonPath("$.data.owner_user_id").value(user("pa")));
    }

    /**
     * 102-R2：投诉到待商户确认后，别的商户确认 403 CROSS_MERCHANT_FORBIDDEN，持建单码的平台员工与平台管理员 403 MERCHANT_SCOPE_REQUIRED；
     * 本商户按专区蛇形键确认（带评分 4）即关闭，关闭动作操作人是商户账号、留痕 merchant_confirmed；处理中的投诉确认 409 INVALID_STATE。
     */
    @Test
    void merchantConfirmsComplaintResolvedFromMerchantZone() throws Exception {
        seedOperatingMerchant();
        String id = createAsMerchant("complaint", "high", "");
        moveTo(id, "pending_merchant");

        String otherMerchant = "mch_102_" + suffix;
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'retail', 'active', now(), now(), ?)", otherMerchant, "别家商户" + suffix, project("a"));
        String creator = role("creator", "merchant.service_work_order.view", "merchant.service_work_order.create");
        merchantConfirm(account("m2", project("a"), otherMerchant, creator), id).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        merchantConfirm(account("sc", project("a"), null, creator), id).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        merchantConfirm(account("adm", "platform_admin", project("a"), null, creator), id).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id = ?", String.class, id))
                .isEqualTo("pending_merchant");

        merchantConfirm(merchantAccount, id).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("closed"))
                .andExpect(jsonPath("$.data.satisfaction_score").value(4));
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT status, closed_at, satisfaction_score FROM merchant_service_work_orders WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("closed");
        assertThat(row.get("closed_at")).isNotNull();
        assertThat(row.get("satisfaction_score")).isEqualTo(4);
        assertThat(jdbcTemplate.queryForList("SELECT operator_user_id FROM merchant_service_work_order_actions WHERE work_order_id = ? AND action_type = 'closed'",
                String.class, id)).containsExactly(user("m"));
        assertThat(jdbcTemplate.queryForList("SELECT event_name FROM audit_events WHERE business_id = ?", String.class, id))
                .contains("merchant_service.work_order.merchant_confirmed");

        String processing = createAsMerchant("complaint", "high", "");
        moveTo(processing, "processing");
        merchantConfirm(merchantAccount, processing).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INVALID_STATE"));
    }

    /**
     * 102-R2：员工关投诉不填关闭缘由 400 VALIDATION_FAILED、单子仍已处理待关闭、记关单失败；填了缘由、不填满意度即关闭，
     * 关闭动作说明为该缘由、满意度为空（不替提单方评分）。报修不填关闭缘由照样关。
     */
    @Test
    void staffClosingComplaintRequiresCloseReasonWithoutDefaultScore() throws Exception {
        seedOperatingMerchant();
        RequestPostProcessor closer = account("k", project("a"), null, role("closer", "merchant.service_work_order.view", "merchant.service_work_order.close"));
        String id = createAsMerchant("complaint", "high", "");
        moveTo(id, "resolved");

        close(closer, id, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id = ?", String.class, id)).isEqualTo("resolved");
        assertThat(jdbcTemplate.queryForList("SELECT event_name FROM audit_events WHERE business_id = ?", String.class, id))
                .contains("merchant_service.closeMerchantServiceWorkOrder.failed");

        close(closer, id, ",\"close_reason\":\"商户多次联系未回复，按既有规则关闭\"").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(jdbcTemplate.queryForObject("SELECT satisfaction_score FROM merchant_service_work_orders WHERE id = ?", Integer.class, id)).isNull();
        assertThat(jdbcTemplate.queryForList("SELECT operator_note FROM merchant_service_work_order_actions WHERE work_order_id = ? AND action_type = 'closed'",
                String.class, id)).containsExactly("商户多次联系未回复，按既有规则关闭");

        String repair = createAsMerchant("repair", "high", "");
        moveTo(repair, "resolved");
        close(closer, repair, "").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
    }

    /** 102-R3：保洁单处理人标已处理即关闭（BSM:1385「保洁等无争议类由处理人完成即结束」），处理说明即处理结论、动作记 closed。 */
    @Test
    void cleaningClosesWhenHandlerMarksResolved() throws Exception {
        seedOperatingMerchant();
        String id = createAsMerchant("cleaning", "high", "");
        moveTo(id, "processing");
        act(processorA, id, "resolved", "已清理完毕").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("closed"));
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT status, closed_at, resolution FROM merchant_service_work_orders WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("closed");
        assertThat(row.get("closed_at")).isNotNull();
        assertThat(row.get("resolution")).isEqualTo("已清理完毕");
        assertThat(jdbcTemplate.queryForList("SELECT action_type FROM merchant_service_work_order_actions WHERE work_order_id = ?", String.class, id))
                .contains("closed");
    }

    /**
     * 102-R3（裁决 26）：出厂报修＝员工验收后关闭、其他服务请求＝处理人完成即结束（照物业工单出厂配置）；报修标已处理后仍待员工关、商户确认 409；
     * 平台员工在处理时限配置里把报修改成「须商户确认」（商户账号改不了）后，员工关报修 409 MERCHANT_CONFIRM_REQUIRED，商户确认即关闭。
     */
    @Test
    void repairAndServiceCloseByConfiguredCloseMethod() throws Exception {
        seedOperatingMerchant();
        RequestPostProcessor closer = account("k", project("a"), null, role("closer", "merchant.service_work_order.view", "merchant.service_work_order.close"));
        mockMvc.perform(get(CLOSE_METHODS).with(merchantAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.work_order_type=='repair')].close_method").value(hasItem("staff_acceptance")))
                .andExpect(jsonPath("$.data[?(@.work_order_type=='service')].close_method").value(hasItem("handler")));

        String service = createAsMerchant("service", "high", "");
        moveTo(service, "resolved");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id = ?", String.class, service)).isEqualTo("closed");

        String repair = createAsMerchant("repair", "high", "");
        moveTo(repair, "resolved");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id = ?", String.class, repair)).isEqualTo("resolved");
        merchantConfirm(merchantAccount, repair).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_CONFIRM_NOT_APPLICABLE"));
        close(closer, repair, "").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));

        saveCloseMethod(merchantAccount, "repair", "merchant_confirm").andExpect(status().isForbidden());
        saveCloseMethod(dispatcherA, "cleaning", "merchant_confirm").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        saveCloseMethod(dispatcherA, "repair", "merchant_confirm").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.close_method").value("merchant_confirm"));

        String confirmed = createAsMerchant("repair", "high", "");
        moveTo(confirmed, "resolved");
        close(closer, confirmed, "").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("MERCHANT_CONFIRM_REQUIRED"));
        merchantConfirm(merchantAccount, confirmed).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
    }

    // ── 第十八批 M 段第 111 条 111-R2：商户服务工单取消（真库＋真实入口＋真实授权链，请求体一律按前端蛇形键） ──
    // ENUM:264「商户服务工单状态｜已提交、已派单、处理中、待商户确认、已处理待关闭、已关闭、已取消（商户专区发起；……）」；
    // 第十八批裁决 27「派单前商户自己取消；派单后，重复提或误提的由客户关怀部填原因取消」。

    /**
     * 111-R2：已提交（未派单）的单，本商户按专区蛇形键填原因取消即为已取消，取消动作操作人是商户账号、说明为原因；换幂等键再取消 409 WORK_ORDER_CLOSED。
     * 不填原因 400；别的商户 403 CROSS_MERCHANT_FORBIDDEN；持建单码的平台员工 403 MERCHANT_SCOPE_REQUIRED，单子仍已提交；已派单的单商户取消 409 INVALID_STATE、状态不变。
     */
    @Test
    void merchantCancelsUndispatchedWorkOrderWithReason() throws Exception {
        seedOperatingMerchant();
        String id = createAsMerchant("cleaning", "low", "");

        merchantCancel(merchantAccount, id, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        String otherMerchant = "mch_111_" + suffix;
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'retail', 'active', now(), now(), ?)", otherMerchant, "别家商户" + suffix, project("a"));
        String creator = role("creator", "merchant.service_work_order.view", "merchant.service_work_order.create");
        merchantCancel(account("m2", project("a"), otherMerchant, creator), id, "误报").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        merchantCancel(account("sc", project("a"), null, creator), id, "误报").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id = ?", String.class, id)).isEqualTo("created");

        merchantCancel(merchantAccount, id, "误报，问题已自行解决").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id = ?", String.class, id)).isEqualTo("cancelled");
        assertThat(jdbcTemplate.queryForList("SELECT operator_user_id, operator_note, from_status FROM merchant_service_work_order_actions "
                + "WHERE work_order_id = ? AND action_type = 'cancelled'", id))
                .singleElement().satisfies(row -> assertThat(row).containsEntry("operator_user_id", user("m"))
                        .containsEntry("operator_note", "误报，问题已自行解决").containsEntry("from_status", "created"));
        assertThat(jdbcTemplate.queryForList("SELECT event_name FROM audit_events WHERE business_id = ?", String.class, id))
                .contains("merchant_service.work_order.cancelled", "merchant_service.work_order.cancel_failed");
        merchantCancel(merchantAccount, id, "误报").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_CLOSED"));

        String assigned = createAsMerchant("repair", "high", "");
        assign(dispatcherB, assigned, user("pa")).andExpect(status().isOk());
        merchantCancel(merchantAccount, assigned, "重复提单").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("INVALID_STATE"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id = ?", String.class, assigned)).isEqualTo("assigned");
    }

    /**
     * 111-R2：派单后重复提或误提的，由客户关怀部（持关闭码、能管到该商户经营项目的平台员工）按后台蛇形键填原因取消，取消动作操作人是该员工、说明为原因；
     * 未派单的单员工取消 409 INVALID_STATE（派单前由商户自己取消）；只到项目 C 的员工 403 CROSS_PROJECT_FORBIDDEN、单子不动；不填原因 400。
     */
    @Test
    void customerCareCancelsDispatchedDuplicateWithReason() throws Exception {
        seedOperatingMerchant();
        RequestPostProcessor care = account("k", project("a"), null, role("closer", "merchant.service_work_order.view", "merchant.service_work_order.close"));
        RequestPostProcessor careC = account("kc", project("c"), null, role("closerc", "merchant.service_work_order.close"));
        String created = createAsMerchant("repair", "high", "");
        staffCancel(care, created, "重复提单").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("INVALID_STATE"));

        String id = createAsMerchant("repair", "high", "");
        moveTo(id, "processing");
        staffCancel(careC, id, "重复提单").andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        staffCancel(care, id, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id = ?", String.class, id)).isEqualTo("processing");

        staffCancel(care, id, "与上午那张重复").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("cancelled"));
        assertThat(jdbcTemplate.queryForList("SELECT operator_user_id, operator_note, from_status FROM merchant_service_work_order_actions "
                + "WHERE work_order_id = ? AND action_type = 'cancelled'", id))
                .singleElement().satisfies(row -> assertThat(row).containsEntry("operator_user_id", user("k"))
                        .containsEntry("operator_note", "与上午那张重复").containsEntry("from_status", "processing"));
    }

    /** 照商户专区「取消」（MerchantZoneWorkOrdersPage.cancelWorkOrder）的请求形状。 */
    private ResultActions merchantCancel(RequestPostProcessor who, String id, String reason) throws Exception {
        return mockMvc.perform(post(WORK_ORDERS + "/" + id + "/cancel").with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"%s","reason":"%s"}
                """.formatted(UUID.randomUUID(), reason)));
    }

    /** 照后台 ADM-MER-001「取消工单」（MerchantWorkOrdersPage.cancelWorkOrder）的请求形状。 */
    private ResultActions staffCancel(RequestPostProcessor who, String id, String reason) throws Exception {
        return mockMvc.perform(post(WORK_ORDERS + "/" + id + "/staff-cancel").with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"%s","reason":"%s"}
                """.formatted(UUID.randomUUID(), reason)));
    }

    /**
     * 第十八批 AE 段第 198 条 198-R3①（BSM:940「收银、核销、收款、放行等现场动作，一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕」）：
     * 后台按网页新形状（不带 operator_user_id）派单、处理、关闭都 200，动作记录的操作人是各次的登录员工。
     */
    @Test
    void assignProcessCloseWithoutOperatorUserIdRecordLoggedInStaff() throws Exception {
        seedOperatingMerchant();
        RequestPostProcessor closer = account("k", project("a"), null, role("closer", "merchant.service_work_order.view", "merchant.service_work_order.close"));
        String id = createAsMerchant("repair", "high", "");
        moveTo(id, "resolved");
        close(closer, id, "").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(jdbcTemplate.queryForList("SELECT action_type, operator_user_id FROM merchant_service_work_order_actions "
                + "WHERE work_order_id = ? AND action_type <> 'created'", id))
                .extracting(row -> tuple(row.get("action_type"), row.get("operator_user_id")))
                .containsExactlyInAnyOrder(tuple("assigned", user("b")), tuple("status_changed", user("pa")),
                        tuple("status_changed", user("pa")), tuple("closed", user("k")));
    }

    /** 派给 pa，由 pa 按后台处理的蛇形键依次走到目标状态（assigned 之后先 processing）。 */
    private void moveTo(String id, String target) throws Exception {
        assign(dispatcherB, id, user("pa")).andExpect(status().isOk());
        act(processorA, id, "processing", "已到场处理").andExpect(status().isOk());
        if (!"processing".equals(target)) {
            act(processorA, id, target, "已处理").andExpect(status().isOk());
        }
    }

    /** 照后台 ADM-MER-001 追加处理（MerchantWorkOrdersPage.processWorkOrder）的请求形状。 */
    private ResultActions act(RequestPostProcessor who, String id, String targetStatus, String note) throws Exception {
        return mockMvc.perform(post(WORK_ORDERS + "/" + id + "/actions").with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"%s","operator_note":"%s","evidence_attachment_ids":["att-merchant-work-order-process"],
                 "target_status":"%s"}
                """.formatted(UUID.randomUUID(), note, targetStatus)));
    }

    /** 照后台 ADM-MER-001 关闭（MerchantWorkOrdersPage.closeWorkOrder）的请求形状，满意度不填发 null；extra 为追加的键。 */
    private ResultActions close(RequestPostProcessor who, String id, String extra) throws Exception {
        return mockMvc.perform(post(WORK_ORDERS + "/" + id + "/close").with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"%s","operator_note":"完整后台关闭商户服务工单","evidence_attachment_ids":["att-merchant-work-order-close"],
                 "resolution":"已维修完成并确认关闭","satisfaction_score":null%s}
                """.formatted(UUID.randomUUID(), extra)));
    }

    /** 照商户专区「确认已解决」（MerchantZoneWorkOrdersPage.confirmWorkOrder）的请求形状。 */
    private ResultActions merchantConfirm(RequestPostProcessor who, String id) throws Exception {
        return mockMvc.perform(post(WORK_ORDERS + "/" + id + "/merchant-confirm").with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"%s","satisfaction_score":4}
                """.formatted(UUID.randomUUID())));
    }

    /** 照后台 ADM-MER-001 处理时限小节保存关单方式（MerchantWorkOrdersPage.saveCloseMethod）的请求形状。 */
    private ResultActions saveCloseMethod(RequestPostProcessor who, String type, String closeMethod) throws Exception {
        return mockMvc.perform(put(CLOSE_METHODS).with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"%s","close_method":"%s","idempotency_key":"%s"}
                """.formatted(type, closeMethod, UUID.randomUUID())));
    }

    /**
     * 第十八批 M 段第 111 条 111-R1（BSM:1466「我的工单：只看派给自己的工单和处理进度。」）：派单员把商户工单派给 pa 后，pa 的员工手机「我的工单」
     * 列出这张单（只看）；同项目另一持处理码员工列表为空；不持处理码的派单员 403；处理到关闭仍列出（看处理进度），存档后不再列出。
     */
    @Test
    void ownerListsMerchantWorkOrdersAssignedToMe() throws Exception {
        seedOperatingMerchant();
        String id = createAsMerchant("cleaning", "medium", "");
        assign(dispatcherB, id, user("pa")).andExpect(status().isOk());

        assignedToMe(processorA).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(id))
                .andExpect(jsonPath("$.data.items[0].status").value("assigned"))
                .andExpect(jsonPath("$.data.items[0].owner_user_id").value(user("pa")));
        RequestPostProcessor other = account("pd", project("a"), null, role("processd", "merchant.service_work_order.process"));
        assignedToMe(other).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        assignedToMe(dispatcherB).andExpect(status().isForbidden());

        act(processorA, id, "processing", "已到场处理").andExpect(status().isOk());
        act(processorA, id, "resolved", "已清理完毕").andExpect(status().isOk());
        assignedToMe(processorA).andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].status").value("closed"));
        RequestPostProcessor care = account("k", project("a"), null, role("closer", "merchant.service_work_order.close"));
        mockMvc.perform(post(WORK_ORDERS + "/" + id + "/archive").with(care).contentType(MediaType.APPLICATION_JSON).content("""
                {"reason":"已关闭归档"}
                """)).andExpect(status().isOk());
        assignedToMe(processorA).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }

    /**
     * 18N 第 124 条 124-R2b（台账 :4231「代码落 OpenAPI 声明的事件，OpenAPI 不改。」）：按商户专区提单的蛇形键建单、后台列表、员工手机「我的工单」，
     * 建单落 merchant_service.createMerchantServiceWorkOrder.succeeded、后台列表落 merchant_service.listMerchantServiceWorkOrders.succeeded
     * （OpenAPI 声明名），「我的工单」仍落 merchant_service.work_order.viewed；旧名不再落。
     */
    @Test
    void createAndListAuditUnderOpenApiDeclaredNames() throws Exception {
        seedOperatingMerchant();
        createAsMerchant("repair", "high", "");
        listIds(dispatcherA);
        assignedToMe(processorA).andExpect(status().isOk());

        assertThat(auditCount("m", "merchant_service.createMerchantServiceWorkOrder.succeeded")).isEqualTo(1);
        assertThat(auditCount("m", "merchant_service.work_order.created")).isZero();
        assertThat(auditCount("a", "merchant_service.listMerchantServiceWorkOrders.succeeded")).isEqualTo(1);
        assertThat(auditCount("a", "merchant_service.work_order.viewed")).isZero();
        assertThat(auditCount("pa", "merchant_service.work_order.viewed")).isEqualTo(1);
        assertThat(auditCount("pa", "merchant_service.listMerchantServiceWorkOrders.succeeded")).isZero();
    }

    /** 第十八批 U 段第 170 条 170-R3（OpenAPI 与代码一致）：商户专区按提单页蛇形键提单，priority=medium 落库；旧契约取值 normal 400 VALIDATION_FAILED。 */
    @Test
    void merchantZoneWorkOrderPriorityFollowsCodeValues() throws Exception {
        seedOperatingMerchant();
        String id = createAsMerchant("repair", "medium", "");
        assertThat(jdbcTemplate.queryForObject("SELECT priority FROM merchant_service_work_orders WHERE id = ?", String.class, id))
                .isEqualTo("medium");
        postAsMerchant("repair", "normal", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    private int auditCount(String tag, String event) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM audit_events WHERE actor_id = ? AND event_name = ?",
                Integer.class, user(tag), event);
    }

    /** 照员工手机「我的工单」（TerminalApiClient.listMyMerchantWorkOrders）的请求。 */
    private ResultActions assignedToMe(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get(WORK_ORDERS + "/assigned-to-me").with(who));
    }

    private ResultActions listIds(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get(WORK_ORDERS).with(who)).andExpect(status().isOk());
    }

    /** 照后台 ADM-MER-001 派单（MerchantWorkOrdersPage.assignWorkOrder）的请求形状；负责人从候选下拉里选（102-R1）。 */
    private ResultActions assign(RequestPostProcessor who, String id, String ownerUserId) throws Exception {
        return mockMvc.perform(post(WORK_ORDERS + "/" + id + "/assign").with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"%s","operator_note":"完整后台商户工单派单","evidence_attachment_ids":["att-merchant-work-order-assign"],
                 "owner_department":"property","owner_user_id":"%s"}
                """.formatted(UUID.randomUUID(), ownerUserId)));
    }

    /**
     * 项目 A、B、C；商户 M 主项目 A、已批准追加经营项目 B；A、B、C 各一名派单员（角色只到本人项目、有员工档案）；M 的商户账号也持派单码、有档案。
     * 102-R1：另有处理员 pa（查看＋处理，只到项目 A）与 pc（处理，只到项目 C），都有员工档案。
     */
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
        dispatcherB = account("b", project("b"), null, dispatch);
        dispatcherC = account("c", project("c"), null, dispatch);
        merchantAccount = account("m", project("a"), merchant, role("merchant", "merchant.service_work_order.view",
                "merchant.service_work_order.create", "merchant.service_work_order.assign"));
        processorA = account("pa", project("a"), null, role("process", "merchant.service_work_order.view", "merchant.service_work_order.process"));
        account("pc", project("c"), null, role("processc", "merchant.service_work_order.process"));
    }

    private ResultActions savePolicy(RequestPostProcessor who, String type, String priority, int minutes) throws Exception {
        return mockMvc.perform(put(POLICIES).with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"%s","priority":"%s","handle_minutes":%d,"idempotency_key":"%s"}
                """.formatted(type, priority, minutes, UUID.randomUUID())));
    }

    /** 照商户专区提单页（MerchantZoneWorkOrdersPage）的请求形状，extra 为追加的键。 */
    private String createAsMerchant(String type, String priority, String extra) throws Exception {
        return JsonPath.read(postAsMerchant(type, priority, extra)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private ResultActions postAsMerchant(String type, String priority, String extra) throws Exception {
        String body = """
                {"idempotency_key":"%s","operator_note":"商户专区自助提交工单","work_order_type":"%s","priority":"%s",
                 "title":"漏水%s","description":"店内天花板漏水","reported_channel":"merchant_portal"%s}
                """.formatted(UUID.randomUUID(), type, priority, suffix, extra);
        return mockMvc.perform(post("/api/v1/merchant-service/work-orders").with(merchantAccount)
                .contentType(MediaType.APPLICATION_JSON).content(body));
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
        return account(tag, "staff", projectId, merchantId, roleId);
    }

    private RequestPostProcessor account(String tag, String accountType, String projectId, String merchantId, String roleId) {
        String userId = user(tag);
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, 'active', now())", userId, userId, accountType, projectId, merchantId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 'approved', 'active', now(), now())", "stf_77_" + tag + "_" + suffix, projectId, merchantId, userId, tag);
        AccessActor actor = new AccessActor(userId, userId, accountType, projectId, merchantId, "active");
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
