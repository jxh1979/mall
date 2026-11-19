package com.complex.platform.evidence;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BA 段第 324 条 324-R1、324-R2（PGM:899「谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」；PGM:682
 * 「内部稽核可查看本综合体全部数据」；第十八批裁决 71 最高权限什么都能看）：操作留痕查询与审计事件页的证据事件列表，不挂商户的非平台管理员按入口权限码可触及的
 * 项目看该项目内商户与人员所做的操作，最高权限看全部，没有项目归属的 403 PROJECT_SCOPE_REQUIRED。真库＋真实接口＋真实角色授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AuditOperationEventsScopeJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String p1 = "aud_p1_" + s;
    private final String p2 = "aud_p2_" + s;
    private final String m1 = "aud_m1_" + s;
    private final String m2 = "aud_m2_" + s;
    private final String p1Staff = "usr_aud_p1_" + s;
    private final String m1User = "usr_aud_m1_" + s;
    private final String p2Staff = "usr_aud_p2_" + s;
    private final String eventName = "b73.audit_scope_probe_" + s;
    private RequestPostProcessor auditor;
    private RequestPostProcessor auditorWithoutProject;

    @BeforeEach
    void setUp() {
        for (String project : List.of(p1, p2)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','留痕范围综合体','active',now(),now())",
                    project);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '留痕范围商户甲', 'amusement', ?, 'active', now(), now())", m1, p1);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '留痕范围商户乙', 'amusement', ?, 'active', now(), now())", m2, p2);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        auditor = staffs.staff("usr_aud_auditor_" + s, "staff", p1, null, "audit.operation_log.view", "evidence.event.view");
        auditorWithoutProject = staffs.staff("usr_aud_noprj_" + s, "staff", null, null, "audit.operation_log.view", "evidence.event.view");
        staffs.staff(p1Staff, "staff", p1, null, "ops.production_readiness.view");
        staffs.staff(m1User, "staff", p1, m1, "ops.production_readiness.view");
        staffs.staff(p2Staff, "staff", p2, null, "ops.production_readiness.view");
        auditEvent(p1Staff, null);
        auditEvent(m1User, m1);
        auditEvent(p2Staff, null);
    }

    @Test
    void auditorWithoutMerchantSeesOperationEventsOfOwnProjectAndMasterSeesAll() throws Exception {
        assertThat(actorIds(auditor)).containsExactlyInAnyOrder(p1Staff, m1User);
        assertThat(actorIds(master())).containsExactlyInAnyOrder(p1Staff, m1User, p2Staff);
        mockMvc.perform(get("/api/v1/audit/operation-events").param("event_name", eventName).with(auditorWithoutProject))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_REQUIRED"));
    }

    @Test
    void auditorWithoutMerchantSeesEvidenceEventsOfOwnProjectAndMasterSeesAll() throws Exception {
        String e1 = evidenceEvent(m1);
        String e2 = evidenceEvent(m2);
        String own = mockMvc.perform(get("/api/v1/evidence/events").param("page", "1").param("size", "20").with(auditor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ownIds = JsonPath.read(own, "$.data.items[*].id");
        assertThat(ownIds).containsExactly(e1);
        assertThat(((Number) JsonPath.read(own, "$.data.total")).longValue()).isEqualTo(1L);
        String all = mockMvc.perform(get("/api/v1/evidence/events").param("page", "1").param("size", "200").with(master()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> allIds = JsonPath.read(all, "$.data.items[*].id");
        assertThat(allIds).contains(e1, e2);
    }

    /**
     * 18BA 收尾复核修正（PGM:659「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户」；PGM:682「内部稽核可查看本综合体全部数据」）：
     * 商户所属项目＝主项目加已批准的经营项目（merchant_projects，与 listMerchantOperatingProjectIds 同一口径）。主项目在乙、经批准也在甲经营的商户，
     * 其证据事件与商户级账号的操作留痕对甲的稽核可见；只提了申请、尚未批准的不可见。
     */
    @Test
    void auditorSeesEventsOfMerchantApprovedToOperateInOwnProject() throws Exception {
        String m3 = "aud_m3_" + s;
        String m4 = "aud_m4_" + s;
        String m3User = "usr_aud_m3_" + s;
        String m4User = "usr_aud_m4_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '留痕范围商户丙', 'amusement', ?, 'active', now(), now())", m3, p2);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '留痕范围商户丁', 'amusement', ?, 'active', now(), now())", m4, p2);
        jdbc.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", m3, p1);
        jdbc.update("INSERT INTO merchant_project_requests (id, merchant_id, project_id, reason, requested_by, review_id, status) "
                + "VALUES (?, ?, ?, '到甲项目经营', ?, ?, 'pending_approval')", "mpr_aud_" + s, m4, p1, m4User, "rv_aud_" + s);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        staffs.staff(m3User, "staff", p2, m3, "ops.production_readiness.view");
        staffs.staff(m4User, "staff", p2, m4, "ops.production_readiness.view");
        auditEvent(m3User, m3);
        auditEvent(m4User, m4);
        String e3 = evidenceEvent(m3);
        evidenceEvent(m4);

        assertThat(actorIds(auditor)).containsExactlyInAnyOrder(p1Staff, m1User, m3User);
        String own = mockMvc.perform(get("/api/v1/evidence/events").param("page", "1").param("size", "20").with(auditor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ownIds = JsonPath.read(own, "$.data.items[*].id");
        assertThat(ownIds).containsExactly(e3);
        assertThat(((Number) JsonPath.read(own, "$.data.total")).longValue()).isEqualTo(1L);
    }

    // ---------- helpers ----------

    private List<String> actorIds(RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(get("/api/v1/audit/operation-events").param("event_name", eventName).with(who))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data[*].actor_id");
    }

    private void auditEvent(String actorId, String merchantId) {
        jdbc.update("INSERT INTO audit_events (id, occurred_at, event_name, event_result, actor_type, actor_id, merchant_id) "
                + "VALUES (?, now(), ?, 'success', 'staff', ?, ?)", UUID.randomUUID(), eventName, actorId, merchantId);
    }

    /** 收到时间排在最前（其余用例留在库里的事件不挤出第一页）。 */
    private String evidenceEvent(String merchantId) {
        String id = "evt_aud_" + merchantId;
        jdbc.update("INSERT INTO evidence_events (id, device_code, event_type, summary, occurred_at, received_at, merchant_id) "
                + "VALUES (?, 'DEV-AUD', 'terminal.login', '留痕范围探针', now(), now() + interval '1 day', ?)", id, merchantId);
        return id;
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
