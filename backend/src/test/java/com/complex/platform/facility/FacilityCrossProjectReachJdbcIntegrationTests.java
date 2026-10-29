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
 * 第十八批 G 段第 82 条 82b-R1（设施设备台账、接警、巡检、工单的列表与既有对象写按入口权限码可触及项目判）与 82b-R2（人工新建工单、
 * 巡检模板、巡检任务落到业务所在项目）真库＋真实接口＋真实角色勾选回归，请求体一律按前端蛇形键。
 * 依据：治理地图第 659 行「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权」「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内
 * 可以查看、处理该角色可查看项目的数据。」「逐个勾选的只能查看被选中的项目」「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」；
 * 第 528 行「④看不到别的商户和别的项目的数据」；第 413 行「所有基础对象都要带“综合体项目归属”」；裁决 M737-iter7「行本身没有项目的不出」。
 *
 * <p>夹具随测试事务回滚：项目 A、B、C（编号带随机后缀），操作人都是归属 A 的员工；普通员工的角色不勾项目，持有人的角色只勾 B。
 * 共享库里有别的数据：列表断言只看本次夹具建的行（编号都以随机后缀结尾）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FacilityCrossProjectReachJdbcIntegrationTests {

    private static final String[] CODES = {"facility.equipment.manage", "facility.alarm.manage", "facility.inspection.manage",
            "facility.work_order.manage"};

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_r1_a_" + suffix;
    private final String projectB = "prj_r1_b_" + suffix;
    private final String projectC = "prj_r1_c_" + suffix;

    @BeforeEach
    void createProjects() {
        for (String projectId : List.of(projectA, projectB, projectC)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "R1 " + projectId);
        }
    }

    // 82b-R1 ①③：A 普通员工各设施列表只出 A 的行；只勾 B 的持有人只出 B 的行，不出本人项目 A（该码没有别的来源）。
    // 修复前普通员工也拿到 B 的行（第一处断言即红）。
    @Test
    void facilityListsOnlyShowRowsOfTheProjectsTheEntryCodeReaches() throws Exception {
        seed("a", projectA);
        seed("b", projectB);
        RequestPostProcessor plain = staffOfA("plain", plainRole("plain", CODES));
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", CODES));

        String[][] lists = {
                {"/api/v1/facility/equipment", "$.data.items[*].id", "feq"},
                {"/api/v1/facility/equipment/maintenance-due", "$.data.items[*].id", "feq"},
                {"/api/v1/facility/alarms", "$.data.items[*].alarm.id", "falm"},
                {"/api/v1/facility/alarm-duties", "$.data.items[*].id", "fadu"},
                {"/api/v1/facility/fault-rules", "$.data.items[*].id", "ffr"},
                {"/api/v1/facility/inspections/templates", "$.data.items[*].id", "fit"},
                {"/api/v1/facility/inspections/tasks", "$.data.items[*].id", "fitask"},
                {"/api/v1/facility/work-orders", "$.data.items[*].id", "fwo"},
                {"/api/v1/facility/work-orders/overdue", "$.data.items[*].id", "fwo"}};
        for (String[] list : lists) {
            assertThat(mine(plain, list[0], list[1])).as("普通员工 " + list[0]).containsExactly(id(list[2], "a"));
            assertThat(mine(ticked, list[0], list[1])).as("只勾 B 的持有人 " + list[0]).containsExactly(id(list[2], "b"));
        }
        // 带项目筛选的列表：请求不可触及的项目 403
        mockMvc.perform(get("/api/v1/facility/alarm-duties?project_id=" + projectB).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // 82b-R1 ②：A 普通员工改 B 的设备状态 403、不改状态、不生成工单；只勾 B 的持有人同一请求 200 并转出报修工单。
    // 修复前普通员工 200（断言即红）。
    @Test
    void equipmentStatusChangeFollowsTheEquipmentProject() throws Exception {
        seed("b", projectB);
        jdbc.update("INSERT INTO facility_alarm_response_policies (alarm_type, priority, response_minutes) VALUES ('hvac', 'high', 30) "
                + "ON CONFLICT (alarm_type, priority) DO UPDATE SET active = TRUE");
        RequestPostProcessor plain = staffOfA("sp", plainRole("sp", CODES));
        RequestPostProcessor ticked = staffOfA("st", tickedBRole("st", CODES));
        String url = "/api/v1/facility/equipment/" + id("feq", "b") + "/status";

        send(plain, post(url), statusBody("k1p")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT status FROM facility_equipment WHERE id=?", String.class, id("feq", "b"))).isEqualTo("normal");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_work_orders WHERE equipment_id=?", Integer.class, id("feq", "b"))).isZero();

        send(ticked, post(url), statusBody("k1t")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("fault"));
        assertThat(jdbc.queryForObject("SELECT project_id FROM facility_work_orders WHERE equipment_id=?", String.class, id("feq", "b")))
                .isEqualTo(projectB);
    }

    // 82b-R1 ④⑤：接警值守安排按班次项目判（A 普通员工 403、持有人 200）；转派的范围闸在幂等之前——持有人转派 B 的工单成功后角色去掉 B，
    // 同一幂等键重放 403、不返回第一次结果。修复前普通员工安排 B 的值守 200（断言即红）。
    @Test
    void writesOnExistingRecordsFollowTheRecordProjectAndTheGateRunsBeforeIdempotency() throws Exception {
        seed("b", projectB);
        RequestPostProcessor plain = staffOfA("wp", plainRole("wp", CODES));
        String role = tickedBRole("wt", CODES);
        RequestPostProcessor ticked = staffOfA("wt", role);

        send(plain, post("/api/v1/facility/alarm-duties"), dutyBody("k2p")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        send(ticked, post("/api/v1/facility/alarm-duties"), dutyBody("k2t")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(projectB));

        String reassign = "/api/v1/facility/work-orders/" + id("fwo", "b") + "/reassign";
        send(plain, post(reassign), reassignBody("k3p")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        String body = reassignBody("k3");
        send(ticked, post(reassign), body).andExpect(status().isOk()).andExpect(jsonPath("$.data.assignee_id").value("usr_new_" + suffix));
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=? AND viewable_id=?", role, projectB);
        send(ticked, post(reassign), body).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // 复核：退回返工凭管理权那一支的范围闸在幂等之前——持有人退回 B 的待验收维修单成功后角色去掉 B，同一幂等键重放 403、
    // 不返回第一次结果。修复前红：重放 200。
    @Test
    void reworkGateRunsBeforeIdempotency() throws Exception {
        seed("b", projectB);
        jdbc.update("UPDATE facility_work_orders SET status='pending_acceptance', equipment_id=? WHERE id=?", id("feq", "b"), id("fwo", "b"));
        String role = tickedBRole("rw", CODES);
        RequestPostProcessor ticked = staffOfA("rw", role);
        String url = "/api/v1/facility/work-orders/" + id("fwo", "b") + "/rework";
        String body = "{\"note\":\"未修好\",\"idempotency_key\":\"rw1-" + suffix + "\"}";

        send(ticked, post(url), body).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=?", role);
        send(ticked, post(url), body).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // 82b-R1 ⑥：项目为空的工单只给平台管理员——勾了全部项目的人看不到，平台管理员看得到。修复前勾全部的人看得到（断言即红）。
    @Test
    void workOrdersWithoutProjectOnlyShowToPlatformAdmin() throws Exception {
        workOrder("none", null, "pending_assign");
        RequestPostProcessor all = staffOfA("all", allProjectsRole("all", CODES));
        RequestPostProcessor admin = account("adm", "platform_admin", projectA, null, plainRole("adm", CODES));
        assertThat(mine(all, "/api/v1/facility/work-orders", "$.data.items[*].id")).doesNotContain(id("fwo", "none"));
        assertThat(mine(admin, "/api/v1/facility/work-orders", "$.data.items[*].id")).contains(id("fwo", "none"));
    }

    // 82b-R2 ①～③：人工新建工单落业务所在项目——普通员工不带项目、设备、空间（与页面默认取值一致）落本人项目 A 且本人列表看得到；
    // 持有人带 B 的空间落 B；带未勾的 C 403 不落库。修复前不带项目的工单 project_id 落空（第一处断言即红）。
    @Test
    void createdWorkOrdersLandInTheBusinessProject() throws Exception {
        String spaceB = space("b", projectB);
        RequestPostProcessor plain = staffOfA("cp", plainRole("cp", CODES));
        RequestPostProcessor ticked = staffOfA("ct", tickedBRole("ct", CODES));

        String created = JsonPath.read(send(plain, post("/api/v1/facility/work-orders"), workOrderBody("w1", "灯坏", ""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(ids(plain, "/api/v1/facility/work-orders")).contains(created);

        send(ticked, post("/api/v1/facility/work-orders"), workOrderBody("w2", "B 灯坏", ",\"space_id\":\"" + spaceB + "\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        send(ticked, post("/api/v1/facility/work-orders"), workOrderBody("w3", "C 灯坏 " + suffix, ",\"project_id\":\"" + projectC + "\""))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_work_orders WHERE title=?", Integer.class, "C 灯坏 " + suffix)).isZero();
    }

    // 82b-R2 ④：普通员工建巡检模板不带项目落本人项目 A；持有人只带 B 的模板建巡检任务落 B。修复前模板 project_id 落空（第一处断言即红）。
    @Test
    void createdInspectionTemplatesAndTasksLandInTheBusinessProject() throws Exception {
        seed("b", projectB);
        RequestPostProcessor plain = staffOfA("ip", plainRole("ip", CODES));
        RequestPostProcessor ticked = staffOfA("it", tickedBRole("it", CODES));

        send(plain, post("/api/v1/facility/inspections/templates"), "{\"name\":\"日巡\",\"inspection_type\":\"facility\","
                + "\"abnormal_mode\":\"light\",\"checklist\":[{\"label\":\"a\"}],\"active\":true,\"idempotency_key\":\"it1-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA));
        send(ticked, post("/api/v1/facility/inspections/tasks"), "{\"template_id\":\"" + id("fit", "b")
                + "\",\"idempotency_key\":\"it2-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String id(String kind, String tag) {
        return kind + "_r1_" + tag + "_" + suffix;
    }

    /** 某项目的一套设施行：设备（维保已到期）、已派单且已过处理时限的工单、接警单、未来班次上的备用值守、故障处置规则、巡检模板与任务。 */
    private void seed(String tag, String projectId) {
        jdbc.update("INSERT INTO facility_equipment (id, name, equipment_type, status, project_id, next_maintenance_due) "
                + "VALUES (?, ?, 'hvac', 'normal', ?, CURRENT_DATE - 1)", id("feq", tag), id("feq", tag), projectId);
        workOrder(tag, projectId, "assigned");
        jdbc.update("""
                INSERT INTO facility_alarms (id, equipment_id, work_order_id, project_id, alarm_type, priority, status, raised_at, response_due_at)
                VALUES (?, ?, ?, ?, 'hvac', 'high', 'awaiting_takeover', now(), now() + interval '30 minute')
                """, id("falm", tag), id("feq", tag), id("fwo", tag), projectId);
        jdbc.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, project_id)
                VALUES (?, ?, CURRENT_DATE + 1, now() + interval '1 day', now() + interval '1 day 8 hour', ?)
                """, id("shift", tag), id("usr_shift", tag), projectId);
        jdbc.update("INSERT INTO facility_alarm_duty_assignments (id, project_id, shift_id, duty_role, contact_phone, created_by) "
                + "VALUES (?, ?, ?, 'backup', '13800000000', 'usr_r1')", id("fadu", tag), projectId, id("shift", tag));
        jdbc.update("INSERT INTO facility_fault_handling_rules (id, project_id, equipment_type, fault_scenario, handling_requirement, created_by) "
                + "VALUES (?, ?, 'hvac', '冷机停机', '先断电再报修', 'usr_r1')", id("ffr", tag), projectId);
        jdbc.update("INSERT INTO facility_inspection_templates (id, name, inspection_type, project_id) VALUES (?, ?, 'facility', ?)",
                id("fit", tag), id("fit", tag), projectId);
        jdbc.update("INSERT INTO facility_inspection_tasks (id, inspection_type, title, project_id) VALUES (?, 'facility', ?, ?)",
                id("fitask", tag), id("fitask", tag), projectId);
    }

    private void workOrder(String tag, String projectId, String status) {
        jdbc.update("""
                INSERT INTO facility_work_orders (id, code, work_order_type, priority, status, title, source, assignee_id, assigned_at,
                        response_due_at, handle_due_at, project_id)
                VALUES (?, ?, 'repair', 'high', ?, ?, 'platform', 'usr_old', now(), now() - interval '2 hour', now() - interval '1 hour', ?)
                """, id("fwo", tag), "FWO-R1" + tag + suffix, status, id("fwo", tag), projectId);
    }

    private String space(String tag, String projectId) {
        jdbc.update("""
                INSERT INTO spaces (id, project_id, space_name, space_type, floor_no, area_square_meter, polygon_points,
                        status, created_at, updated_at)
                VALUES (?, ?, ?, 'shop', '1F', 10, '[]'::jsonb, 'active', now(), now())
                """, id("sp", tag), projectId, id("sp", tag));
        return id("sp", tag);
    }

    private String statusBody(String key) {
        return "{\"status\":\"fault\",\"priority\":\"high\",\"alarm_note\":\"x\",\"idempotency_key\":\"" + key + "-" + suffix + "\"}";
    }

    private String dutyBody(String key) {
        return "{\"shift_id\":\"" + id("shift", "b") + "\",\"duty_role\":\"primary\",\"contact_phone\":\"13800000000\","
                + "\"idempotency_key\":\"" + key + "-" + suffix + "\"}";
    }

    private String reassignBody(String key) {
        return "{\"assignee_id\":\"usr_new_" + suffix + "\",\"reason\":\"r\",\"idempotency_key\":\"" + key + "-" + suffix + "\"}";
    }

    private String workOrderBody(String key, String title, String extra) {
        return "{\"work_order_type\":\"repair\",\"priority\":\"normal\",\"title\":\"" + title + "\",\"source\":\"platform\","
                + "\"reporter_type\":\"staff\",\"idempotency_key\":\"" + key + "-" + suffix + "\"" + extra + "}";
    }

    // ── 角色与会话（照 CrossProjectGovernanceJdbcIntegrationTests） ─────────────────────────────────

    private String plainRole(String tag, String... codes) {
        return role(tag, false, List.of(), codes);
    }

    private String allProjectsRole(String tag, String... codes) {
        return role(tag, true, List.of(), codes);
    }

    private String tickedBRole(String tag, String... codes) {
        return role(tag, false, List.of(projectB), codes);
    }

    private String role(String tag, boolean viewAll, List<String> ticked, String... codes) {
        String roleId = "role_r1_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "R1 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null, viewAll, ticked));
        return roleId;
    }

    private RequestPostProcessor staffOfA(String tag, String... roleIds) {
        return account(tag, "staff", projectA, null, roleIds);
    }

    /** 建一名账号、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体，四项与账号行一致）。 */
    private RequestPostProcessor account(String tag, String accountType, String organizationId, String merchantId, String... roleIds) {
        String userId = "usr_r1_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(userId, userId, accountType, organizationId, merchantId, "active", Instant.now()));
        for (String roleId : roleIds) {
            accessRepository.assignUserRole(userId, roleId);
        }
        AccessActor actor = new AccessActor(userId, userId, accountType, organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    // ── 取数 ─────────────────────────────────────────────────────────────────────────────────────

    private ResultActions send(RequestPostProcessor session, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<String> ids(RequestPostProcessor session, String url) throws Exception {
        return JsonPath.read(mockMvc.perform(get(url).with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
    }

    /** 以给定身份 GET（须 200），取出该 JSON 路径下属于本次夹具的编号（都以随机后缀结尾）；共享库里别的行不看。 */
    private List<String> mine(RequestPostProcessor session, String url, String path) throws Exception {
        List<String> values = JsonPath.read(mockMvc.perform(get(url).with(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return values.stream().filter(value -> value != null && value.endsWith(suffix)).toList();
    }
}
