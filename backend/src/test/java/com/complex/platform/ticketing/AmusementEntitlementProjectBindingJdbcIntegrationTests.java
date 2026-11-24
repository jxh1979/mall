package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementBusinessDayService;
import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.amusement.AmusementRepository;
import com.complex.platform.amusement.AmusementSessionLifecycleService;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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

/**
 * 第十八批 CC 段第 399 条 399-R2（BSM:784「入场、游乐（指定项目/次数/场次）……」；BSM:753「游乐权益核销的收入按具体的游乐项目归属」）：游乐权益指定了
 * 游乐项目 A 的，在同一综合体的游乐项目 B 核销 409 ENTITLEMENT_NOT_FOR_PROJECT（规则拒绝、不扣次、B 没有入场）；同一权益在 A 照常入场、扣一次。
 * 按员工端门票核销屏实发形状（蛇形键）走真实入口与授权链。修复前在 B 照常 200、扣次：状态断言红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class AmusementEntitlementProjectBindingJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;

    private AmusementTicketJdbcFixture.Data f;
    private AccessActor actor;
    private String otherProject;
    private String otherSafety;
    private String otherDevice;

    @AfterEach
    void cleanup() {
        try {
            if (f != null) {
                AmusementTicketJdbcFixture.cleanupTicket(jdbc, f);
                AmusementTicketJdbcFixture.cleanupProject(jdbc, f);
            }
            if (otherProject != null) {
                jdbc.update("DELETE FROM amusement_projects WHERE id = ?", otherProject);
                jdbc.update("DELETE FROM amusement_safety_templates WHERE id = ?", otherSafety);
                jdbc.update("DELETE FROM terminal_devices WHERE id = ?", otherDevice);
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void entitlementForProjectAIsRejectedAtProjectBWithoutConsuming() throws Exception {
        f = AmusementTicketJdbcFixture.seed(jdbc, repository, lifecycle, days, reviews, 3, "per_use", 10);
        actor = (AccessActor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        jdbc.update("UPDATE entitlement_templates SET amusement_project_id = ? WHERE id = ?", f.project(), f.template());
        String otherPoint = otherProject(f);

        http(f.code(), "other", otherDevice, otherPoint).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_NOT_FOR_PROJECT"));
        assertThat(used()).isZero();
        assertThat(entries(otherProject)).isZero();

        http(f.code(), "own", f.device(), f.point()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));
        assertThat(used()).isEqualTo(1);
        assertThat(entries(f.project())).isEqualTo(1);
    }

    /** 同一综合体再建游乐项目 B（项目、类型绑定、巡检通过、空间、开放场次、设备，写法同 AmusementTicketJdbcFixture.seed），返回 B 的入口点位。 */
    private String otherProject(AmusementTicketJdbcFixture.Data source) {
        String k = source.key() + "b";
        otherProject = "ajf_project_" + k;
        otherSafety = "ajf_safety_" + k;
        otherDevice = "ajf_device_" + k;
        String session = "ajf_session_" + k;
        String point = "ajf_point_" + k;
        jdbc.update("""
            INSERT INTO amusement_safety_templates(id,template_name,items_json,status,created_at,updated_at)
            VALUES(?,'指定项目测试巡检','[{"name":"restraint","required":true}]'::jsonb,'active',now(),now())
            """, otherSafety);
        jdbc.update("""
            INSERT INTO amusement_projects(id,project_name,capacity,open_status,safety_template_id,
              eligibility_rule_json,location_point_id,created_at,updated_at)
            VALUES(?,'指定项目测试乙项目',10,'open',?,'{}'::jsonb,?,now(),now())
            """, otherProject, otherSafety, point);
        jdbc.update("INSERT INTO amusement_project_entitlements(id,project_id,entitlement_type,created_at) VALUES(?,?,'amusement_pass',now())",
                "ajf_binding_" + k, otherProject);
        jdbc.update("""
            INSERT INTO amusement_safety_inspections(id,project_id,template_id,items_json,overall_result,
              inspection_status,rectification_status,reopen_status,inspected_at,submitted_at,created_at,updated_at)
            VALUES(?,?,?,'[{"name":"restraint","result":"pass"}]'::jsonb,'pass','submitted',
              'not_required','not_required',now(),now(),now(),now())
            """, "ajf_inspect_" + k, otherProject, otherSafety);
        AmusementBusinessDayTestFixture.bind(jdbc, otherProject, source.complex());
        jdbc.update("""
            INSERT INTO amusement_sessions(id,project_id,start_time,end_time,capacity,used_capacity,status,created_at,updated_at)
            VALUES(?,?,now()-interval '1 hour',now()+interval '1 hour',10,0,'open',now(),now())
            """, session, otherProject);
        lifecycle.lockSessionWindow(repository.findSessionById(session).orElseThrow());
        jdbc.update("""
            INSERT INTO terminal_devices(id,device_id,device_code,terminal_type,binding_scope,status,created_at,updated_at)
            VALUES(?,?,?,'kiosk','venue','bound',now(),now())
            """, otherDevice, "physical_" + k, "terminal_" + k);
        return point;
    }

    /** 员工端门票核销屏实发形状（蛇形键、credential_type=qr）走真实入口与授权链。 */
    private ResultActions http(String code, String request, String device, String point) throws Exception {
        String body = JSON.writeValueAsString(Map.of("idempotency_key", "c399_" + request + "_" + code, "credential_type", "qr",
                "credential_payload", code, "device_id", device, "point_id", point,
                "business_context", Map.of("scenario", "ticket_checkin", "staff_id", "gate")));
        return mockMvc.perform(post("/api/v1/ticketing/verifications").with(r -> {
                    r.addHeader("X-Terminal-Type", "web");
                    return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                            List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(r);
                })
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int used() {
        return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, f.entitlement());
    }

    private int entries(String project) {
        return jdbc.queryForObject("SELECT count(*) FROM amusement_verifications WHERE project_id = ? AND direction = 'entry'", Integer.class, project);
    }
}
