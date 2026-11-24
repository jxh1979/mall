package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementBusinessDayService;
import com.complex.platform.amusement.AmusementRepository;
import com.complex.platform.amusement.AmusementSessionLifecycleService;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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

/**
 * 第十八批 CF 段第 407 条 407-R2（BSM:1271「某台设备巡检超期、巡检不合格……按其类别处置：辅助设备只停用该台设备本身，项目是否可以在其余条件下继续运行，
 * 由工程岗位或安全岗位判断……判断结论必须留痕，记下是谁判的、什么时间判的、依据了哪几项条件，没有留痕的一律按停止运行处理；关键安全设备……不存在依赖关系、
 * 且其余关键安全设备均无故障的，只停该台故障设备，项目可继续运行；存在依赖关系的，整个游乐项目停止新增核销并停止运行」「停用的设备完成合格巡检后才放行」；
 * 决策台账 DEC-R3B-2026-08-04-22）：按设备分级判停机范围，辅助设备停用后能否继续运行的判定留痕。夹具项目当日已有合格巡检，三台设备按网页台账建档并经工程岗复核生效
 * （两台关键、不依赖；一台辅助，设备码即核销终端编号，核销时即指定该设备）。真库、真实 HTTP、真实授权链与蛇形键。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@AutoConfigureMockMvc
class AmusementDeviceStopScopeJdbcTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;
    private AmusementTicketJdbcFixture.Data f;
    private AccessActor gate;
    private AccessActor viewer;
    private AccessActor judge;
    private String crit1;
    private String crit2;
    private String aux;
    private AccessActor manager;
    private AccessActor engineer;

    @AfterEach void cleanup() {
        try {
            if (f != null) AmusementTicketJdbcFixture.cleanupTicket(jdbc, f);
        } finally { SecurityContextHolder.clearContext(); }
    }

    /**
     * 辅助设备超期：项目报 AUXILIARY_DEVICE_STOPPED，停用设备清单列出该台；工程岗判「可继续运行」（依据一项）后项目放行，但指定该台核销仍 409；
     * 判定早于本次停用（之后合格巡检又超期）不再算。判定接口：没停用的辅助设备 409、关键设备 409。
     */
    @Test void auxiliaryStopNeedsARecordedContinueJudgement() throws Exception {
        seed();
        expire(aux, 25);
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'AUXILIARY_DEVICE_STOPPED')]").exists());
        mockMvc.perform(get("/api/v1/amusement/projects/" + f.project() + "/devices/stops").with(web(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].device_id").value(aux))
                .andExpect(jsonPath("$.data[0].reasons[0]").value("expired"));

        String judged = judge(aux, "continue").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conclusion").value("continue"))
                .andExpect(jsonPath("$.data.judged_by").value(judge.id()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        eligibility().andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
        verify("aux").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("SAFETY_BLOCKED"));

        jdbc.update("UPDATE amusement_device_continue_judgements SET judged_at = now() - interval '30 hours' WHERE id=?",
                JsonPath.<String>read(judged, "$.data.id"));
        expire(aux, 25);
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'AUXILIARY_DEVICE_STOPPED')]").exists());

        refresh(aux);
        judge(aux, "continue").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DEVICE_NOT_STOPPED"));
        expire(crit1, 25);
        judge(crit1, "continue").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DEVICE_NOT_AUXILIARY"));
    }

    /** 关键设备：一台不依赖的超期、另一台正常，项目照常放行；两台都超期报 CRITICAL_DEVICE_STOPPED；改为依赖后一台超期即报。 */
    @Test void criticalStopScopeFollowsDependency() throws Exception {
        seed();
        expire(crit1, 25);
        eligibility().andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
        expire(crit2, 25);
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'CRITICAL_DEVICE_STOPPED')]").exists());
        refresh(crit2);
        jdbc.update("UPDATE amusement_devices SET dependency='dependent' WHERE id=?", crit1);
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'CRITICAL_DEVICE_STOPPED')]").exists());
    }

    /** 辅助设备的设备级巡检不合格只停该台（项目不报 SAFETY_REOPEN_PENDING，判可继续运行后放行）；分级未生效设备的设备级不合格照旧整项目拦。 */
    @Test void deviceLevelFailureFollowsClassification() throws Exception {
        seed();
        failedInspection(f.device());
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'SAFETY_REOPEN_PENDING')]").doesNotExist())
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'AUXILIARY_DEVICE_STOPPED')]").exists());
        judge(aux, "continue").andExpect(status().isOk());
        eligibility().andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));

        String plain = device("plain", "auxiliary", null, false);
        failedInspection(code(plain));
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'SAFETY_REOPEN_PENDING')]").exists());
    }

    /**
     * 收尾复核修正（BSM:1271「停用的设备完成合格巡检后才放行」；设计判断第 14 项「合格时点有复检取复检时刻」）：辅助设备的设备级巡检不合格，
     * 复检晚于原巡检一个周期以上、主管批准恢复营业后，最近合格巡检时间取复检时刻，该台不再按巡检超期停用、项目放行；复检早于已记的最近合格巡检时，
     * 批准不把最近合格巡检时间改回更早；项目级巡检（不指定设备）复检早于各设备已记的最近合格巡检时，批准同样不把全部设备改回更早。
     */
    @Test void approvedRecheckReleasesTheDeviceWithoutRollingBackLastPass() throws Exception {
        seed();
        AccessActor supervisor = staff("reopen", "amusement.safety_inspection.approve_reopen", "amusement.safety_inspection.view");
        expire(aux, 40);
        String late = awaitingApproval(f.device(), 30, 0);
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'AUXILIARY_DEVICE_STOPPED')]").exists());
        approveReopen(supervisor, late).andExpect(status().isOk());
        stops().andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        eligibility().andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));

        String earlier = awaitingApproval(f.device(), 30, 26);
        approveReopen(supervisor, earlier).andExpect(status().isOk());
        stops().andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        eligibility().andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));

        refresh(aux);
        String projectLevel = awaitingApproval(null, 30, 26);
        approveReopen(supervisor, projectLevel).andExpect(status().isOk());
        stops().andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        eligibility().andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private void seed() throws Exception {
        f = AmusementTicketJdbcFixture.seed(jdbc, repository, lifecycle, days, reviews, 10, "per_use", 5);
        gate = (AccessActor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        viewer = staff("view", "amusement.safety_inspection.view", "amusement.device.view");
        judge = staff("judge", "amusement.device.continue_judge", "amusement.device.view");
        manager = staff("manager", "amusement.device.manage", "amusement.device.view", "governance.supervisor_review.view");
        engineer = staff("engineer", "amusement.device.class.approve", "governance.supervisor_review.approve",
                "governance.supervisor_review.view");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", manager.id(), engineer.id());
        crit1 = device("crit1", "critical", "independent", true);
        crit2 = device("crit2", "critical", "independent", true);
        aux = device(null, "auxiliary", null, true);
    }

    /**
     * 按网页设备台账建档（分级、理由、最终审批人为工程岗），approved 为真时工程岗批准即生效；最近合格巡检置为当前。
     * tag 为空即设备码取核销终端编号。
     */
    private String device(String tag, String safetyClass, String dependency, boolean approved) throws Exception {
        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("idempotency_key", "a407-device-" + UUID.randomUUID());
        request.put("device_code", tag == null ? f.device() : tag + "_" + f.key());
        request.put("name", "设备" + (tag == null ? "辅助" : tag));
        request.put("status", "active");
        request.put("safety_check_interval_hours", 24);
        request.put("safety_class", safetyClass);
        if (dependency != null) request.put("dependency", dependency);
        request.put("operator_note", "按安全评估分级");
        request.put("final_approver_id", engineer.id());
        String created = mockMvc.perform(post("/api/v1/amusement/projects/" + f.project() + "/devices").with(web(manager))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(created, "$.data.id");
        String history = mockMvc.perform(get("/api/v1/amusement/projects/" + f.project() + "/devices/classification-changes").with(web(manager)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        if (approved) {
            String review = JsonPath.<List<String>>read(history, "$.data[?(@.device_id == '" + id + "')].review_id").get(0);
            mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(web(engineer))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of("decision", "approved",
                                    "comment", "工程岗复核同意", "idempotency_key", "a407-approve-" + UUID.randomUUID()))))
                    .andExpect(status().isOk());
        }
        refresh(id);
        return id;
    }

    private String code(String device) {
        return jdbc.queryForObject("SELECT device_code FROM amusement_devices WHERE id=?", String.class, device);
    }

    private void expire(String device, int hoursAgo) {
        jdbc.update("UPDATE amusement_devices SET last_pass_inspection_at = now() - make_interval(hours => ?) WHERE id=?", hoursAgo, device);
    }

    private void refresh(String device) {
        jdbc.update("UPDATE amusement_devices SET last_pass_inspection_at = now() WHERE id=?", device);
    }

    private void failedInspection(String deviceCode) {
        jdbc.update("""
            INSERT INTO amusement_safety_inspections(id,project_id,device_id,template_id,items_json,overall_result,inspection_status,
              rectification_status,reopen_status,inspected_at,submitted_at,created_at,updated_at)
            VALUES(?,?,?,?,'[{"name":"restraint","result":"fail"}]'::jsonb,'fail','submitted','pending','pending',now(),now(),now(),now())
            """, "a407_fail_" + UUID.randomUUID(), f.project(), deviceCode, f.safety());
    }

    private ResultActions judge(String device, String conclusion) throws Exception {
        String body = JSON.writeValueAsString(Map.of("idempotency_key", "a407-judge-" + UUID.randomUUID(), "conclusion", conclusion,
                "conditions", List.of("其余关键安全设备运行正常", "")));
        return mockMvc.perform(post("/api/v1/amusement/projects/" + f.project() + "/devices/" + device + "/continue-judgements")
                .with(web(judge)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions eligibility() throws Exception {
        return mockMvc.perform(get("/api/v1/amusement/projects/" + f.project() + "/open-eligibility").with(web(viewer)));
    }

    private ResultActions stops() throws Exception {
        return mockMvc.perform(get("/api/v1/amusement/projects/" + f.project() + "/devices/stops").with(web(viewer)));
    }

    /**
     * 一条复检通过、待主管批准恢复营业的巡检：deviceCode 为设备码即设备级、为空即项目级；巡检于 inspectedHoursAgo 小时前，复检于 recheckedHoursAgo 小时前。
     */
    private String awaitingApproval(String deviceCode, int inspectedHoursAgo, int recheckedHoursAgo) {
        String id = "a407_recheck_" + UUID.randomUUID();
        jdbc.update("""
            INSERT INTO amusement_safety_inspections(id,project_id,device_id,template_id,items_json,overall_result,inspection_status,
              rectification_status,reopen_status,inspected_at,submitted_at,rechecked_at,created_at,updated_at)
            VALUES(?,?,?,?,'[{"name":"restraint","result":"fail"}]'::jsonb,'pass','submitted','rechecked','pending_approval',
              now() - make_interval(hours => ?),now() - make_interval(hours => ?),now() - make_interval(hours => ?),
              now() - make_interval(hours => ?),now())
            """, id, f.project(), deviceCode, f.safety(), inspectedHoursAgo, inspectedHoursAgo, recheckedHoursAgo, inspectedHoursAgo);
        return id;
    }

    /** 后台安全巡检页「批准恢复营业」实发形状。 */
    private ResultActions approveReopen(AccessActor who, String inspection) throws Exception {
        return mockMvc.perform(post("/api/v1/amusement/safety-inspections/" + inspection + "/approve-reopen").with(web(who))
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of(
                        "idempotency_key", "a407-reopen-" + UUID.randomUUID(), "reason", "主管现场确认整改到位"))));
    }

    /** 员工端门票核销屏实发形状（蛇形键、credential_type=qr）：设备号即本台辅助设备码。 */
    private ResultActions verify(String tag) throws Exception {
        String body = JSON.writeValueAsString(Map.of("idempotency_key", "a407_" + tag + "_" + f.code(), "credential_type", "qr",
                "credential_payload", f.code(), "device_id", f.device(), "point_id", f.point(),
                "business_context", Map.of("scenario", "ticket_checkin", "staff_id", "gate")));
        return mockMvc.perform(post("/api/v1/ticketing/verifications").with(web(gate))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static RequestPostProcessor web(AccessActor who) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(who, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 本综合体公司员工（不挂商户，账号类型同夹具核销操作人）：中文角色名、后缀纯数字；只授权限表里已有的码（新码随迁移落库）。 */
    private AccessActor staff(String tag, String... permissions) {
        String digits = String.valueOf(System.nanoTime());
        String user = "a407_user_" + tag + "_" + digits;
        String role = "a407_role_" + tag + "_" + digits;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'platform_admin',?,'active',now())",
                user, user, f.complex());
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())", role, "游乐设备停用回归角色" + digits);
        for (String permission : permissions) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code=?",
                    role, permission);
        }
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", user, f.complex());
        return new AccessActor(user, user, "platform_admin", f.complex(), null, "active");
    }
}
