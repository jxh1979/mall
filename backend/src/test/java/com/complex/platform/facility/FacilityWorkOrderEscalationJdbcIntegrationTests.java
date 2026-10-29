package com.complex.platform.facility;

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
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 77 条 77-R1 物业工单响应／处理超时自动升级通知主管真库回归：建单、派单、开始处理、标记逾期、超时清单都按前端蛇形键走真实入口与授权链，
 * 升级由定时作业 {@link FacilityWorkOrderEscalationJob} 跑一轮。
 * 依据：BSM:1382「按响应时限和处理时限统计时效，超时自动升级通知主管。」；BSM:1385「超时自动升级通知主管；楼宇设备告警的接警响应及休息期间例外，
 * 按本文件「楼宇运营」告警条执行」「维修处理与验收未完成的，继续按原工单时效升级跟进」；ENUM:265；PGM:528「④看不到别的商户和别的项目的数据」；
 * 主管＝持 facility.work_order.manage 的物业主管（决策台账第十二批裁决第 9 条落地）。
 *
 * <p>夹具随测试事务回滚：项目 P、Q（编号带随机后缀）各一名主管（角色持 facility.work_order.manage、不勾项目、有员工档案）。共享库里别的超时工单
 * 也会被同一轮升级，断言只看本次夹具的行与收件人。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FacilityWorkOrderEscalationJdbcIntegrationTests {

    private static final String BASE = "/api/v1/facility/work-orders";

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FacilityWorkOrderEscalationJob job;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectP = "prj_esc_p_" + suffix;
    private final String projectQ = "prj_esc_q_" + suffix;
    private RequestPostProcessor supervisorP;

    @BeforeEach
    void seed() {
        for (String projectId : List.of(projectP, projectQ)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "升级 " + projectId);
        }
        String role = "role_esc_" + suffix;
        accessRepository.createRole(new RoleRecord(role, "工单升级 " + suffix, null, "project", "active", Instant.now(),
                List.of("facility.work_order.manage"), null, false, List.of()));
        supervisorP = supervisor("p", projectP, role);
        supervisor("q", projectQ, role);
    }

    // ①② 定时入口默认关闭不升级；作业跑一轮：响应超时置超时标记、记响应升级时刻，P 的主管收一条系统通知（标题含工单编号、正文含「响应时限」），
    // Q 的主管不收，逐单留痕 kind=response；再跑一轮不重复。派单、开始处理后处理超时：P 的主管收第二条（处理时限、当前状态），
    // 超时清单仍列出该单（标记后不消失）。修复前作业不升级（overtime 断言即红）。
    @Test
    void responseAndHandleTimeoutsEachEscalateOnceToTheSupervisorOfTheProject() throws Exception {
        String id = create("w1");
        jdbc.update("UPDATE facility_work_orders SET response_due_at = now() - interval '1 minute' WHERE id = ?", id);

        job.scheduledEscalate();
        assertThat(escalatedAt(id, "response")).isNull();

        job.escalate(Instant.now());

        Map<String, Object> row = jdbc.queryForMap("SELECT code, overtime FROM facility_work_orders WHERE id = ?", id);
        assertThat(row.get("overtime")).isEqualTo(true);
        assertThat(escalatedAt(id, "response")).isNotNull();
        assertThat(escalatedAt(id, "handle")).isNull();
        assertThat(notices("p")).singleElement().satisfies(n -> {
            assertThat(n.get("notice_type")).isEqualTo("system_notice");
            assertThat((String) n.get("title")).isEqualTo("工单超时：" + row.get("code"));
            assertThat((String) n.get("body")).contains("响应时限").contains("仍未开始处理");
        });
        assertThat(notices("q")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT metadata->>'kind' FROM audit_events WHERE event_name = 'facility.work_order_escalated' "
                + "AND business_id = ?", String.class, id)).isEqualTo("response");

        job.escalate(Instant.now());
        assertThat(notices("p")).hasSize(1);

        send(post(BASE + "/" + id + "/assign"), "{\"assignee_id\":\"usr_fixer_" + suffix + "\",\"idempotency_key\":\"a-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("assigned"));
        send(post(BASE + "/" + id + "/start"), "{\"idempotency_key\":\"s-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        jdbc.update("UPDATE facility_work_orders SET handle_due_at = now() - interval '1 minute' WHERE id = ?", id);

        job.escalate(Instant.now());

        assertThat(escalatedAt(id, "handle")).isNotNull();
        assertThat(notices("p")).hasSize(2).last().satisfies(n -> assertThat((String) n.get("body"))
                .contains("处理时限").contains("仍未完成").contains("处理中").contains("usr_fixer_" + suffix));
        assertThat(notices("q")).isEmpty();
        List<String> overdue = JsonPath.read(mockMvc.perform(get(BASE + "/overdue").with(supervisorP)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
        assertThat(overdue).contains(id);
    }

    // ③ 设备告警来源单响应超时不进本作业（接警升级按告警条）、处理超时照常升级；挂起、已关闭不升级；
    // 待验收（验收未完成）处理超时照常升级。修复前待验收单不升级（断言即红）。
    @Test
    void alarmResponseSuspendedAndClosedAreSkippedWhileUnfinishedAcceptanceStillEscalates() {
        String alarm = raw("al", "equipment_alarm", "pending_assign", "now() - interval '1 minute'", "now() + interval '1 hour'");
        String suspended = raw("su", "platform", "suspended", "now() - interval '1 minute'", "now() - interval '1 minute'");
        String closed = raw("cl", "platform", "closed", "now() - interval '1 minute'", "now() - interval '1 minute'");
        String acceptance = raw("pa", "platform", "pending_acceptance", "now() + interval '1 hour'", "now() - interval '1 minute'");

        job.escalate(Instant.now());

        assertThat(escalatedAt(acceptance, "handle")).isNotNull();
        assertThat(notices("p")).singleElement().satisfies(n -> assertThat((String) n.get("body")).contains("待验收"));
        for (String skipped : List.of(alarm, suspended, closed)) {
            assertThat(escalatedAt(skipped, "response")).as(skipped).isNull();
            assertThat(escalatedAt(skipped, "handle")).as(skipped).isNull();
        }

        jdbc.update("UPDATE facility_work_orders SET handle_due_at = now() - interval '1 minute' WHERE id = ?", alarm);
        job.escalate(Instant.now());

        assertThat(escalatedAt(alarm, "handle")).isNotNull();
        assertThat(escalatedAt(alarm, "response")).isNull();
        assertThat(notices("p")).hasSize(2);
    }

    // ⑤ 手动「标记逾期」走同一升级：升级并通知主管、逐单留痕；随后作业不重复通知。修复前只置标记不通知（断言即红）。
    @Test
    void manualMarkOverdueEscalatesAndNotifiesOnceThenTheJobDoesNotRepeat() throws Exception {
        String id = create("w5");
        jdbc.update("UPDATE facility_work_orders SET response_due_at = now() - interval '1 minute' WHERE id = ?", id);

        mockMvc.perform(post(BASE + "/mark-overdue?idempotency_key=m-" + suffix).with(supervisorP))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(1));

        assertThat(escalatedAt(id, "response")).isNotNull();
        assertThat(notices("p")).singleElement().satisfies(n -> assertThat((String) n.get("body")).contains("响应时限"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'facility.work_order_escalated' "
                + "AND business_id = ?", Integer.class, id)).isEqualTo(1);

        job.escalate(Instant.now());
        assertThat(notices("p")).hasSize(1);
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** P 的主管按后台建单表单的蛇形键建一张报修×紧急工单，返回工单编号。 */
    private String create(String key) throws Exception {
        String body = "{\"work_order_type\":\"repair\",\"priority\":\"urgent\",\"title\":\"空调不制冷 " + key + "\",\"description\":\"3F\","
                + "\"source\":\"platform\",\"reporter_type\":\"staff\",\"project_id\":\"" + projectP + "\",\"idempotency_key\":\""
                + key + "-" + suffix + "\"}";
        return JsonPath.read(send(post(BASE), body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /** P 的一张给定来源与状态的工单（时限为 SQL 表达式），返回编号。 */
    private String raw(String tag, String source, String status, String responseDue, String handleDue) {
        String id = "fwo_esc_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO facility_work_orders (id, code, work_order_type, priority, status, title, source, project_id, "
                + "response_due_at, handle_due_at) VALUES (?, ?, 'repair', 'high', ?, ?, ?, ?, " + responseDue + ", " + handleDue + ")",
                id, "FWO-ESC" + tag + suffix, status, "工单 " + tag, source, projectP);
        return id;
    }

    private Object escalatedAt(String id, String kind) {
        return jdbc.queryForObject("SELECT " + kind + "_escalated_at FROM facility_work_orders WHERE id = ?", Object.class, id);
    }

    private List<Map<String, Object>> notices(String tag) {
        return jdbc.queryForList("""
                SELECT n.notice_type, n.title, n.body FROM employee_notices n JOIN staff_profiles p ON p.id = n.staff_profile_id
                 WHERE p.user_id = ? ORDER BY n.created_at, n.id
                """, user(tag));
    }

    private String user(String tag) {
        return "usr_esc_" + tag + "_" + suffix;
    }

    /** 某项目的物业主管：在用员工账号、不带商户归属、挂给定角色、有已批准的员工档案；返回以其身份登录的后台会话。 */
    private RequestPostProcessor supervisor(String tag, String projectId, String role) {
        String userId = user(tag);
        accessRepository.createUser(new AccessUser(userId, userId, "staff", projectId, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, role);
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, ?, 'approved', 'active', now(), now())
                """, "stf_esc_" + tag + "_" + suffix, projectId, userId, "主管 " + tag);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectId, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(supervisorP).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
