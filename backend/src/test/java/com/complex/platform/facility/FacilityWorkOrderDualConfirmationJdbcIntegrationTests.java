package com.complex.platform.facility;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * HP-F02 真库、真接口回归（请求体按前端 adminClient.mapKeys 后的蛇形键）：
 * BSM L1385「复杂类（大型设备报修）需主管/物业验收，并且一律须提单方确认才能关闭。自动生成的大型维修工单，由该设备或点位的在岗责任人
 * 承接提单确认；责任人不在岗或本人负责此次维修时，由物业主管指定其他人员并留痕。仍须同时完成主管／物业验收和提单责任人确认，才能关闭」；
 * ENUM L262；第十二批操作员裁决第 2 条「退回原维修人返工」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FacilityWorkOrderDualConfirmationJdbcIntegrationTests {

    private static final String BASE = "/api/v1/facility/work-orders";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "hpf02_prj_" + suffix;
    private final String equipment = "hpf02_feq_" + suffix;
    private RequestPostProcessor admin;
    private RequestPostProcessor repairer;
    private RequestPostProcessor designee;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','双闸回归项目','active',now(),now())", project);
        String role = "hpf02_role_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'双闸回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'facility.work_order.manage')", role);
        admin = account("admin", "platform_admin", "org", role);
        repairer = account("eng", "staff", project, role);
        designee = account("b", "staff", project, role);
        jdbc.update("""
                INSERT INTO facility_equipment (id, name, equipment_type, space_id, responsible_user_id, status, project_id)
                VALUES (?, '2号冷机', 'hvac', 'sp_roof', ?, 'alarm', ?)""", equipment, user("eng"), project);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 第十二批操作员裁决第 12 条「关联设备的报修才两道」：报修策略按前端蛇形形状存为主管验收；没关联设备的普通报修按此配置
     * 主管验收即关闭，关联设备的报修照样走两道（主管验收后为已验收、待提单确认）；验收方式不再收「主管验收＋提单方确认」。
     */
    @Test
    void plainRepairFollowsConfiguredAcceptanceEquipmentRepairIsDual() throws Exception {
        mockMvc.perform(post(BASE + "/policies").with(admin).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"repair","priority":"high","response_minutes":60,"handle_minutes":720,
                 "requires_acceptance":true,"acceptance_by":"supervisor","active":true,"idempotency_key":"k1_%s"}
                """.formatted(suffix))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT acceptance_by FROM facility_work_order_policy WHERE id='fwop_repair_high'",
                String.class)).isEqualTo("supervisor");
        String plain = order(null, "pending_acceptance", "platform");
        jdbc.update("UPDATE facility_work_orders SET equipment_id = NULL WHERE id = ?", plain);
        call(admin, plain, "accept", "{\"satisfaction\":5,\"idempotency_key\":\"pa_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        String linked = order(null, "pending_acceptance", "platform");
        call(admin, linked, "accept", "{\"idempotency_key\":\"la_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("accepted"));

        mockMvc.perform(post(BASE + "/policies").with(admin).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"repair","priority":"high","response_minutes":60,"handle_minutes":720,
                 "requires_acceptance":true,"acceptance_by":"supervisor_and_reporter","active":true,"idempotency_key":"k2_%s"}
                """.formatted(suffix))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    /**
     * 第十二批操作员裁决第 13 条「员工手机「我的工单」」：只持 view_assigned 的提单确认承接人，在 GET /assigned-to-me 里一并看到
     * 「待我提单确认」的单（confirmer_id＝本人、走两道、提单确认未做、未关闭；未完工的不列），按前端形状从这里确认或退回返工，做完不再列出；
     * 旁人看不到。
     */
    @Test
    void myWorkOrdersListPendingMyConfirmationAndActFromThere() throws Exception {
        String viewRole = "hpf02_mine_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'我的工单回归','project','active',now())",
                viewRole, viewRole);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'facility.work_order.view_assigned')", viewRole);
        RequestPostProcessor owner = account("own", "staff", project, viewRole);
        RequestPostProcessor bystander = account("by", "staff", project, viewRole);
        String toConfirm = order("own", "pending_acceptance", "equipment_alarm");
        String toRework = order("own", "pending_acceptance", "equipment_alarm");
        order("own", "processing", "equipment_alarm");
        call(admin, toConfirm, "accept", "{\"idempotency_key\":\"ma_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("accepted"));

        mine(owner).andExpect(jsonPath("$.data.items[*].id", containsInAnyOrder(toConfirm, toRework)));
        mine(bystander).andExpect(jsonPath("$.data.items", empty()));

        call(owner, toConfirm, "review", "{\"satisfaction\":5,\"idempotency_key\":\"mr_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        call(owner, toRework, "rework", "{\"note\":\"提单确认不通过：仍漏水\",\"idempotency_key\":\"mw_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        mine(owner).andExpect(jsonPath("$.data.items", empty()));
    }

    /**
     * 第十六批第 35 条（ENUM L260、BSM L1377「报修、保洁、投诉、整改、巡检、内部协作工单（内部部门间发起与流转）、其他服务请求」）：
     * 按前端蛇形形状给内部协作工单配「类型×优先级」时限与验收方式，员工后台新建内部协作工单按所配时限落库、提单员工为当前登录人；
     * 商户自助不能提这类单（BSM L1385「商户侧可自助提报修、保洁、投诉和其他服务请求」）。
     */
    @Test
    void internalCollaborationWorkOrderFollowsConfiguredPolicyAndIsNotMerchantSelfService() throws Exception {
        mockMvc.perform(post(BASE + "/policies").with(admin).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"internal_collaboration","priority":"normal","response_minutes":30,"handle_minutes":600,
                 "requires_acceptance":false,"acceptance_by":"handler","active":true,"idempotency_key":"ic1_%s"}
                """.formatted(suffix))).andExpect(status().isOk());
        String id = JsonPath.read(mockMvc.perform(post(BASE).with(admin).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"internal_collaboration","priority":"normal","title":"安保部请工程部协助更换监控电源",
                 "source":"platform","reporter_type":"staff","project_id":"%s","idempotency_key":"ic2_%s"}
                """.formatted(project, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.work_order_type").value("internal_collaboration"))
                .andExpect(jsonPath("$.data.status").value("pending_assign"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        Map<String, Object> saved = jdbc.queryForMap("""
                SELECT work_order_type, reporter_id, (response_due_at - created_at) = interval '30 minutes' AS response_ok,
                       (handle_due_at - created_at) = interval '600 minutes' AS handle_ok
                  FROM facility_work_orders WHERE id=?""", id);
        assertThat(saved.get("work_order_type")).isEqualTo("internal_collaboration");
        assertThat(saved.get("reporter_id")).isEqualTo(user("admin"));
        assertThat(saved.get("response_ok")).isEqualTo(true);
        assertThat(saved.get("handle_ok")).isEqualTo(true);

        mockMvc.perform(post(BASE).with(admin).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"internal_collaboration","priority":"normal","title":"商户自助提内部协作单",
                 "source":"merchant_self","reporter_type":"merchant","project_id":"%s","idempotency_key":"ic3_%s"}
                """.formatted(project, suffix)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    /**
     * 第十六批操作员裁决补充第 1 条「照「其他服务请求」种一份」：「出厂时限和验收方式照现有「其他」类型的出厂策略种一份，开箱即可用，之后可改。」
     * 全新库不在后台配任何策略，按前端蛇形形状不带优先级（默认 normal）新建内部协作工单即成功，时限照「其他」出厂行（V166
     * fwop_other_normal：响应 240 分钟、处理 2880 分钟、不需验收、处理人完成即结）。
     */
    @Test
    void internalCollaborationWorkOrderWorksOutOfTheBoxWithOtherFactoryPolicy() throws Exception {
        String id = JsonPath.read(mockMvc.perform(post(BASE).with(admin).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"internal_collaboration","title":"工程部请保洁部协助清理机房","source":"platform",
                 "reporter_type":"staff","project_id":"%s","idempotency_key":"icd_%s"}
                """.formatted(project, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.work_order_type").value("internal_collaboration"))
                .andExpect(jsonPath("$.data.priority").value("normal"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        assertThat(jdbc.queryForObject("""
                SELECT (response_due_at - created_at) = interval '240 minutes' AND (handle_due_at - created_at) = interval '2880 minutes'
                  FROM facility_work_orders WHERE id=?""", Boolean.class, id)).isTrue();
        assertThat(jdbc.queryForMap("""
                SELECT response_minutes, handle_minutes, requires_acceptance, acceptance_by, active FROM facility_work_order_policy
                 WHERE work_order_type='internal_collaboration' AND priority='normal'"""))
                .isEqualTo(Map.of("response_minutes", 240, "handle_minutes", 2880, "requires_acceptance", false,
                        "acceptance_by", "handler", "active", true));
    }

    private ResultActions mine(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get(BASE + "/assigned-to-me").with(who)).andExpect(status().isOk());
    }

    /**
     * R2/R3＋裁决第 2 条：设备责任人即本次维修人 → 主管验收后提单确认须先指定他人；指定留痕；被指定人退回返工（裁决第 16 条：主管验收后
     * 只许承接人本人退回）后两项确认作废；修好后主管验收＋被指定人确认两项齐全才关闭，库内分别留存两项事实。
     */
    @Test
    void autoOrderDesignationReworkAndDualFactsPersist() throws Exception {
        String id = "hpf02_wo_" + suffix;
        jdbc.update("""
                INSERT INTO facility_work_orders (id, code, work_order_type, priority, status, title, source, space_id, equipment_id,
                    reporter_id, reporter_type, assignee_id, response_due_at, handle_due_at, assigned_at, responded_at,
                    project_id, confirmer_id)
                VALUES (?, ?, 'repair', 'high', 'processing', '设备告警: 2号冷机', 'equipment_alarm', 'sp_roof', ?,
                    'system', 'system', ?, now() + interval '1 hour', now() + interval '12 hours', now(), now(), ?, ?)""",
                id, "FWO-" + suffix, equipment, user("eng"), project, user("eng"));

        call(admin, id, "complete", "{\"resolution\":\"更换压缩机继电器\",\"idempotency_key\":\"c1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_acceptance"));
        call(admin, id, "accept", "{\"note\":\"主管现场验收\",\"idempotency_key\":\"a1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("accepted"));
        call(repairer, id, "review", "{\"satisfaction\":5,\"idempotency_key\":\"r0_" + suffix + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("CONFIRMER_DESIGNATION_REQUIRED"));

        call(admin, id, "confirmer", "{\"confirmer_id\":\"" + user("b") + "\",\"note\":\"责任人即维修人\",\"idempotency_key\":\"d1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.confirmer_id").value(user("b")));
        assertThat(jdbc.queryForObject("SELECT confirmer_id FROM facility_work_orders WHERE id=?", String.class, id))
                .isEqualTo(user("b"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name='facility.work_order_confirmer_designated' AND business_id=?
                """, Integer.class, id)).isEqualTo(1);

        call(designee, id, "rework", "{\"note\":\"复查仍有异响\",\"idempotency_key\":\"w1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        Map<String, Object> reworked = row(id);
        assertThat(reworked.get("verified_at")).isNull();
        assertThat(reworked.get("assignee_id")).isEqualTo(user("eng"));

        call(admin, id, "complete", "{\"resolution\":\"更换轴承\",\"idempotency_key\":\"c2_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_acceptance"));
        call(designee, id, "review", "{\"satisfaction\":4,\"idempotency_key\":\"r1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_acceptance"));
        call(admin, id, "accept", "{\"idempotency_key\":\"a2_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));

        Map<String, Object> closed = row(id);
        assertThat(closed.get("status")).isEqualTo("closed");
        assertThat(closed.get("verified_at")).isNotNull();
        assertThat(closed.get("verifier_id")).isEqualTo(user("admin"));
        assertThat(closed.get("reporter_confirmed_at")).isNotNull();
        assertThat(closed.get("reporter_confirmed_by")).isEqualTo(user("b"));
        assertThat(jdbc.queryForObject("SELECT status FROM facility_equipment WHERE id=?", String.class, equipment))
                .isEqualTo("normal");
    }

    /**
     * BSM L1385「由该设备或点位的在岗责任人承接提单确认」＋裁决第 2 条「提单确认不通过……退回原维修人返工」：承接人只持 view_assigned、
     * 不持 facility.work_order.manage，也能凭承接身份退回返工和做提单确认；既不持 manage 又不是承接人的被 403。
     */
    @Test
    void confirmerWithoutManageReworksAndConfirms() throws Exception {
        String viewRole = "hpf02_view_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'双闸回归','project','active',now())",
                viewRole, viewRole);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'facility.work_order.view_assigned')", viewRole);
        RequestPostProcessor owner = account("own", "staff", project, viewRole);
        RequestPostProcessor bystander = account("by", "staff", project, viewRole);
        String id = order("own", "pending_acceptance", "equipment_alarm");
        call(admin, id, "accept", "{\"idempotency_key\":\"a1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("accepted"));

        call(bystander, id, "rework", "{\"note\":\"不是我负责\",\"idempotency_key\":\"w0_" + suffix + "\"}")
                .andExpect(status().isForbidden());
        call(owner, id, "rework", "{\"note\":\"提单确认不通过：仍有异响\",\"idempotency_key\":\"w1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        assertThat(row(id).get("verified_at")).isNull();

        call(admin, id, "complete", "{\"resolution\":\"更换轴承\",\"idempotency_key\":\"c2_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_acceptance"));
        call(admin, id, "accept", "{\"idempotency_key\":\"a2_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("accepted"));
        call(bystander, id, "review", "{\"satisfaction\":5,\"idempotency_key\":\"r0_" + suffix + "\"}")
                .andExpect(status().isForbidden());
        // OpenAPI 声明的失败审计 facility.work_order_review_failed：非承接人且不持 manage 的被拒留痕。
        assertThat(failures("facility.work_order_review_failed", "r0_" + suffix)).isEqualTo(1);
        call(owner, id, "review", "{\"satisfaction\":5,\"idempotency_key\":\"r1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(row(id).get("reporter_confirmed_by")).isEqualTo(user("own"));
    }

    /**
     * 退回返工、指定提单确认人按 OpenAPI x-idempotency-required：同一幂等键按前端形状重放返回第一次的结果，不再改动、成功留痕只记一次，
     * 也不记失败留痕。
     */
    @Test
    void reworkAndConfirmerReplaySameKeyReturnFirstResult() throws Exception {
        String reworked = order(null, "accepted", "platform");
        String rework = "{\"note\":\"复查仍漏水\",\"idempotency_key\":\"rp_" + suffix + "\"}";
        call(admin, reworked, "rework", rework).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        call(admin, reworked, "rework", rework).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        assertThat(successes("facility.work_order_rework_requested", reworked)).isEqualTo(1);
        assertThat(failures("facility.work_order_rework_failed", "rp_" + suffix)).isZero();

        String designated = order("eng", "accepted", "equipment_alarm");
        String confirmer = "{\"confirmer_id\":\"" + user("b") + "\",\"note\":\"责任人即维修人\",\"idempotency_key\":\"dp_" + suffix + "\"}";
        call(admin, designated, "confirmer", confirmer).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmer_id").value(user("b")));
        call(admin, designated, "confirmer", confirmer).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmer_id").value(user("b")));
        assertThat(successes("facility.work_order_confirmer_designated", designated)).isEqualTo(1);
        assertThat(failures("facility.work_order_confirmer_designate_failed", "dp_" + suffix)).isZero();
    }

    /**
     * 开始、完成、挂起、恢复与转派同锁工单行（SELECT … FOR UPDATE）：主管转派时原处理人正点「开始」「完成」，后提交的一方读到的是
     * 对方提交后的处理人与状态，不会用旧快照整行写回把转派结果覆盖掉。
     */
    @ParameterizedTest
    @ValueSource(strings = {"start", "complete", "suspend", "resume"})
    void processingActionsLockWorkOrderRow(String action) throws Exception {
        String status = switch (action) {
            case "start" -> "assigned";
            case "resume" -> "suspended";
            default -> "processing";
        };
        String id = order(null, status, "platform");
        assertThat(rowLocks()).isZero();
        String body = "complete".equals(action)
                ? "{\"resolution\":\"更换阀门\",\"idempotency_key\":\"pl_" + suffix + "\"}"
                : "{\"note\":\"现场处理\",\"idempotency_key\":\"pl_" + suffix + "\"}";
        call(admin, id, action, body).andExpect(status().isOk());
        assertThat(rowLocks()).isPositive();
    }

    private int successes(String event, String businessId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name=? AND business_id=?", Integer.class, event,
                businessId);
    }

    private int failures(String event, String idempotencyKey) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name=? AND idempotency_key=?", Integer.class, event,
                idempotencyKey);
    }

    /** 双闸两方各自写入同一工单：主管验收、提单确认、退回返工、指定确认人都先锁工单行（SELECT … FOR UPDATE），并发时串行、不互相覆盖。 */
    @ParameterizedTest
    @ValueSource(strings = {"accept", "review", "rework", "confirmer"})
    void dualActionsLockWorkOrderRow(String action) throws Exception {
        // 指定确认人：设备责任人即本次维修人（eng）时由物业主管改指定。
        String id = "confirmer".equals(action) ? order("eng", "pending_acceptance", "equipment_alarm")
                : order(null, "pending_acceptance", "platform");
        assertThat(rowLocks()).isZero();
        String body = switch (action) {
            case "rework" -> "{\"note\":\"仍漏水\",\"idempotency_key\":\"lk_" + suffix + "\"}";
            case "confirmer" -> "{\"confirmer_id\":\"" + user("b") + "\",\"idempotency_key\":\"lk_" + suffix + "\"}";
            default -> "{\"idempotency_key\":\"lk_" + suffix + "\"}";
        };
        call(admin, id, action, body).andExpect(status().isOk());
        assertThat(rowLocks()).isPositive();
    }

    /**
     * 复核有依据直接修（BSM L1385「责任人不在岗或本人负责此次维修时，由物业主管指定其他人员并留痕」）：设备没有登记责任人
     * （自动工单的提单确认承接人为空）时，物业主管按前端形状指定确认人成功，留痕原因为承接人缺失。
     */
    @Test
    void designateConfirmerWhenEquipmentHasNoResponsibleUser() throws Exception {
        String id = order(null, "accepted", "equipment_alarm");

        call(admin, id, "confirmer", "{\"confirmer_id\":\"" + user("b") + "\",\"note\":\"设备未登记责任人\",\"idempotency_key\":\"dn_"
                + suffix + "\"}").andExpect(status().isOk()).andExpect(jsonPath("$.data.confirmer_id").value(user("b")));

        assertThat(jdbc.queryForList("""
                SELECT metadata->>'reason' FROM audit_events WHERE event_name='facility.work_order_confirmer_designated' AND business_id=?
                """, String.class, id)).containsExactly("missing");
    }

    /**
     * 第十二批操作员裁决第 15 条「提单员工本人（推荐）」：「和告警单一样，提单员工就是承接人，在手机「我的工单」里确认或退回」。
     * 提单员工＝在后台点「新建工单」的当前登录员工：本人按前端真实形状（蛇形键、不带 reporter_id 与 project_id）建关联设备的报修单，
     * 建单即记本人为提单人与承接人、项目取设备台账；主管验收照旧；后台持 manage 的旁人不能代点提单确认；承接人在「我的工单」里看到并
     * 退回返工、确认。
     */
    @Test
    void staffCreatedEquipmentRepairIsConfirmedByReporterOnly() throws Exception {
        String viewRole = "hpf02_rep_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'提单员工回归','project','active',now())",
                viewRole, viewRole);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'facility.work_order.view_assigned'),(?,'facility.work_order.manage')",
                viewRole, viewRole);
        RequestPostProcessor owner = account("own", "staff", project, viewRole);
        String id = createStaffRepair(owner, "own");
        Map<String, Object> created = jdbc.queryForMap("SELECT reporter_id, confirmer_id, project_id FROM facility_work_orders WHERE id=?", id);
        assertThat(created.get("reporter_id")).isEqualTo(user("own"));
        assertThat(created.get("confirmer_id")).isEqualTo(user("own"));
        assertThat(created.get("project_id")).isEqualTo(project);

        toAccepted(id, "s1");
        mine(owner).andExpect(jsonPath("$.data.items[*].id", containsInAnyOrder(id)));
        call(admin, id, "review", "{\"satisfaction\":5,\"idempotency_key\":\"sr0_" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CONFIRMER_MISMATCH"));
        call(owner, id, "rework", "{\"note\":\"提单确认不通过：仍有异响\",\"idempotency_key\":\"sw1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));

        call(admin, id, "complete", "{\"resolution\":\"更换轴承\",\"idempotency_key\":\"sc2_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_acceptance"));
        call(admin, id, "accept", "{\"idempotency_key\":\"sa2_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("accepted"));
        call(owner, id, "review", "{\"satisfaction\":5,\"idempotency_key\":\"sr1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(row(id).get("reporter_confirmed_by")).isEqualTo(user("own"));
    }

    /**
     * 第十二批操作员裁决第 15 条「提单人就是本次维修人时，由物业主管指定他人并留痕」（同 BSM L1385「本人负责此次维修时，由物业主管指定
     * 其他人员并留痕」）：后台建单的提单员工被派单维修，本人不能做提单确认（409）；物业主管指定他人留痕（原因 is_repairer），由被指定人确认。
     */
    @Test
    void staffReporterRepairingOwnOrderNeedsDesignatedConfirmer() throws Exception {
        String id = createStaffRepair(repairer, "eng");
        assertThat(jdbc.queryForObject("SELECT confirmer_id FROM facility_work_orders WHERE id=?", String.class, id))
                .isEqualTo(user("eng"));
        toAccepted(id, "s2");

        call(repairer, id, "review", "{\"satisfaction\":5,\"idempotency_key\":\"se0_" + suffix + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("CONFIRMER_DESIGNATION_REQUIRED"));
        call(admin, id, "confirmer", "{\"confirmer_id\":\"" + user("b") + "\",\"note\":\"提单人即维修人\",\"idempotency_key\":\"sd1_"
                + suffix + "\"}").andExpect(status().isOk()).andExpect(jsonPath("$.data.confirmer_id").value(user("b")));
        assertThat(jdbc.queryForList("""
                SELECT metadata->>'reason' FROM audit_events WHERE event_name='facility.work_order_confirmer_designated' AND business_id=?
                """, String.class, id)).containsExactly("is_repairer");
        call(designee, id, "review", "{\"satisfaction\":4,\"idempotency_key\":\"se1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
    }

    /**
     * 第十二批操作员裁决第 16 条「只许承接人本人（推荐）」：「主管验收后只剩提单确认这一道，退回也只让承接人本人做」。绑定承接人的两道单
     * （告警单、员工手工建的关联设备报修单）主管已验收后，持 manage 的主管按前端形状退回返工 403 `CONFIRMER_MISMATCH`、状态不变并记失败留痕；
     * 承接人本人退回回处理中；待验收时主管验收不通过的退回照旧。
     */
    @Test
    void afterAcceptanceOnlyConfirmerMayRework() throws Exception {
        String viewRole = "hpf02_rw_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'退回返工回归','project','active',now())",
                viewRole, viewRole);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'facility.work_order.view_assigned')", viewRole);
        RequestPostProcessor owner = account("own", "staff", project, viewRole);
        String alarm = order("own", "accepted", "equipment_alarm");
        String staffMade = order("own", "accepted", "platform");
        jdbc.update("UPDATE facility_work_orders SET reporter_type='staff', reporter_id=? WHERE id=?", user("own"), staffMade);

        for (String id : List.of(alarm, staffMade)) {
            String key = "sv_" + id;
            call(admin, id, "rework", "{\"note\":\"主管复查仍有异响\",\"idempotency_key\":\"" + key + "\"}")
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CONFIRMER_MISMATCH"));
            assertThat(row(id).get("status")).isEqualTo("accepted");
            assertThat(failures("facility.work_order_rework_failed", key)).isEqualTo(1);
            call(owner, id, "rework", "{\"note\":\"提单确认不通过：仍有异响\",\"idempotency_key\":\"ow_" + id + "\"}")
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        }

        String pending = order("own", "pending_acceptance", "equipment_alarm");
        call(admin, pending, "rework", "{\"note\":\"主管验收不通过\",\"idempotency_key\":\"pv_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
    }

    /**
     * 提单员工本人（持 manage）按后台设施工作台「新建工单」的真实请求形状建关联设备的报修单：adminClient 蛇形键；表单没有提单人和项目
     * 输入，故不带 reporter_id、project_id。
     */
    private String createStaffRepair(RequestPostProcessor reporter, String reporterTag) throws Exception {
        String title = "2号冷机异响_" + reporterTag + "_" + suffix;
        mockMvc.perform(post(BASE).with(reporter).contentType(MediaType.APPLICATION_JSON).content("""
                {"work_order_type":"repair","priority":"high","title":"%s","source":"platform","equipment_id":"%s",
                 "space_id":"sp_roof","reporter_type":"staff","idempotency_key":"cr_%s_%s"}
                """.formatted(title, equipment, reporterTag, suffix))).andExpect(status().isOk());
        return jdbc.queryForObject("SELECT id FROM facility_work_orders WHERE title=?", String.class, title);
    }

    /** 派给 eng → 开始 → 完工 → 主管验收（已验收，待提单确认）。 */
    private void toAccepted(String id, String tag) throws Exception {
        call(admin, id, "assign", "{\"assignee_id\":\"" + user("eng") + "\",\"idempotency_key\":\"as_" + tag + suffix + "\"}")
                .andExpect(status().isOk());
        call(admin, id, "start", "{\"idempotency_key\":\"st_" + tag + suffix + "\"}").andExpect(status().isOk());
        call(admin, id, "complete", "{\"resolution\":\"更换压缩机继电器\",\"idempotency_key\":\"co_" + tag + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_acceptance"));
        call(admin, id, "accept", "{\"idempotency_key\":\"ac_" + tag + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("accepted"));
    }

    /**
     * 第十二批操作员裁决第 9 条「补转派」：处理中的工单由物业主管按前端形状改派他人——只换处理人，状态与响应／处理时限、派单时间不动；
     * 留痕原处理人、新处理人和原因；同键重放返回首次结果、不重复留痕；原因必填（空原因 400 并记失败留痕）。
     */
    @Test
    void reassignSnakeCaseShapeKeepsDeadlinesAndAuditsTrail() throws Exception {
        String id = order(null, "processing", "platform");
        String times = "SELECT response_due_at, handle_due_at, assigned_at, overtime FROM facility_work_orders WHERE id=?";
        Map<String, Object> before = jdbc.queryForMap(times, id);
        String body = "{\"assignee_id\":\"" + user("b") + "\",\"reason\":\"原维修人请假\",\"idempotency_key\":\"ra_" + suffix + "\"}";

        call(admin, id, "reassign", body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignee_id").value(user("b")))
                .andExpect(jsonPath("$.data.status").value("processing"));
        call(admin, id, "reassign", body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignee_id").value(user("b")));

        assertThat(row(id).get("assignee_id")).isEqualTo(user("b"));
        assertThat(jdbc.queryForMap(times, id)).isEqualTo(before);
        assertThat(jdbc.queryForList("""
                SELECT (metadata->>'previous_assignee_id') || '|' || (metadata->>'assignee_id') || '|' || (metadata->>'reason')
                  FROM audit_events WHERE event_name='facility.work_order_reassigned' AND business_id=?
                """, String.class, id)).containsExactly(user("eng") + "|" + user("b") + "|原维修人请假");

        call(admin, id, "reassign", "{\"assignee_id\":\"" + user("admin") + "\",\"reason\":\" \",\"idempotency_key\":\"rb_"
                + suffix + "\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(row(id).get("assignee_id")).isEqualTo(user("b"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name='facility.work_order_reassign_failed' AND idempotency_key=?
                """, Integer.class, "rb_" + suffix)).isEqualTo(1);
    }

    /** 本事务在 facility_work_orders 上持有的行级锁（FOR UPDATE 取得 RowShareLock）。 */
    private int rowLocks() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM pg_locks WHERE pid = pg_backend_pid() AND relation = 'facility_work_orders'::regclass
                   AND mode = 'RowShareLock'""", Integer.class);
    }

    /** 维修人 eng、承接人 confirmerTag（可空）的报修工单。 */
    private String order(String confirmerTag, String status, String source) {
        String id = "hpf02_wo2_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO facility_work_orders (id, code, work_order_type, priority, status, title, source, space_id, equipment_id,
                    reporter_id, reporter_type, assignee_id, response_due_at, handle_due_at, assigned_at, responded_at, completed_at,
                    resolution, project_id, confirmer_id)
                VALUES (?, ?, 'repair', 'high', ?, '设备告警: 2号冷机', ?, 'sp_roof', ?,
                    'system', 'system', ?, now() + interval '1 hour', now() + interval '12 hours', now(), now(), now(), '已修', ?, ?)""",
                id, "FWO-" + id.substring(id.length() - 8), status, source, equipment, user("eng"), project,
                confirmerTag == null ? null : user(confirmerTag));
        return id;
    }

    private Map<String, Object> row(String id) {
        return jdbc.queryForMap("""
                SELECT status, assignee_id, verified_at, verifier_id, reporter_confirmed_at, reporter_confirmed_by
                  FROM facility_work_orders WHERE id=?""", id);
    }

    private ResultActions call(RequestPostProcessor who, String id, String action, String body) throws Exception {
        return mockMvc.perform(post(BASE + "/" + id + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private RequestPostProcessor account(String tag, String accountType, String organizationId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,?,?,NULL,'active',now())",
                userId, userId, accountType, organizationId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, accountType, organizationId, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "hpf02_" + tag + "_" + suffix;
    }
}
