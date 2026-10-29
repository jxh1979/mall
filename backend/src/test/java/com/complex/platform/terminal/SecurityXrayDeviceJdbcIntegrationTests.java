package com.complex.platform.terminal;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

/**
 * 第 63 条（BSM:800「安检环节不产生按人记录，仅记设备状态、故障事件与维修工单留痕」；PGM:618「设备要绑定员工、班次、点位和状态」；
 * PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」；PGM:624「记录设备领用、归还、维修、挂失和报废」）：真实库、真实接口、
 * 真实授权链回归，请求体按后台前端的蛇形键。安检 X 光设备在后台登记进设备台账（编号、型号、点位、责任人），人工登记故障即开一张
 * 维修工单，修好后恢复在用；不接设备接口、不走员工端 /devices/bind。
 * 修复前没有登记入口：第一条用例 200 的断言即红。
 * 第 82 条 82c-R1（PGM:618「综合体入场三道环节设备（安检环节的 X 光设备、年龄核验环节的人脸扫描设备、检票环节的检票闸机）」）：
 * 人脸扫描设备、检票闸机与安检 X 光设备共用后台登记入口 /devices/entry-devices（请求带 terminal_type）；只有安检 X 光设备送修即开维修工单。
 * 第 82 条 82c-R2（PGM:618「设备要绑定员工、班次、点位和状态」；PGM:624）：工作手机、平板、一体机修复完成回待领用（returned）。
 * 第 82 条 82c-R3（PGM:623「记录编号、型号、点位、责任人和状态」）：入场三道环节设备单独入口更换责任人（/devices/{id}/responsible-staff）。
 *
 * <p>不开测试事务（走真实接口、真实提交）。夹具按随机后缀隔离、结束时删除；设备状态流水与审计事件只追加不删，按后缀隔离留在库里。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class SecurityXrayDeviceJdbcIntegrationTests {

    private static final String DEVICES = "/api/v1/terminals/devices";
    private static final List<String> CODES = List.of(
            "terminal.device.lifecycle", "terminal.switch.view", "terminal.device.bind");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessRepository accessRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_x63_a_" + suffix;
    private final String projectB = "prj_x63_b_" + suffix;
    private final String spaceA = "spc_x63_a_" + suffix;
    private final String spaceB = "spc_x63_b_" + suffix;
    private final String holderA = "stf_x63_a_" + suffix;
    // 第 82 条 82c-R3：项目甲另一名员工（新责任人）、项目乙的员工
    private final String holderA2 = "stf_x63_a2_" + suffix;
    private final String holderB = "stf_x63_b_" + suffix;
    // 项目甲的项目员工（不带商户归属），持设备治理权限
    private final String projectStaff = "usr_x63_p_" + suffix;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", project, "X63 " + project);
        }
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at) "
                + "VALUES (?, ?, '安检口甲', 'shop', 'active', now(), now())", spaceA, projectA);
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at) "
                + "VALUES (?, ?, '安检口乙', 'shop', 'active', now(), now())", spaceB, projectB);
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, '安检责任人', ?, 'approved', 'active', now(), now())
                """, holderA, projectA, "usr_" + holderA, projectA);
        for (String[] staff : List.of(new String[] {holderA2, projectA}, new String[] {holderB, projectB})) {
            jdbcTemplate.update("""
                    INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                            registration_status, status, created_at, updated_at)
                    VALUES (?, ?, NULL, ?, '设备责任人', ?, 'approved', 'active', now(), now())
                    """, staff[0], staff[1], "usr_" + staff[0], staff[1]);
        }
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(projectStaff, projectStaff, "staff", projectA, null, "active", now));
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), projectStaff, CODES,
                now.minusSeconds(3600), now.plusSeconds(86_400), false, false, "active", "第 63 条安检 X 光设备回归",
                "seed-admin", now, null, null));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM facility_work_orders WHERE space_id IN (?, ?)", spaceA, spaceB);
        jdbcTemplate.update("DELETE FROM terminal_devices WHERE device_code LIKE ?", "XR-%" + suffix);
        jdbcTemplate.update("DELETE FROM staff_profiles WHERE id IN (?, ?, ?)", holderA, holderA2, holderB);
        jdbcTemplate.update("DELETE FROM spaces WHERE id IN (?, ?)", spaceA, spaceB);
        jdbcTemplate.update("DELETE FROM projects WHERE id IN (?, ?)", projectA, projectB);
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id = ?", projectStaff);
        jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", projectStaff);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "x63-%" + suffix);
    }

    // 后台登记：编号、型号、点位、责任人四项 → 进台账、在用；设备列表带型号。
    @Test
    void registerXrayDevice() throws Exception {
        send(DEVICES + "/entry-devices", registerJson(key("reg"), "a", spaceA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.device_type").value("security_xray"))
                .andExpect(jsonPath("$.data.status").value("bound"))
                .andExpect(jsonPath("$.data.device_code").value(code("a")))
                .andExpect(jsonPath("$.data.device_model").value("FISCAN CX6040"))
                .andExpect(jsonPath("$.data.bound_space_id").value(spaceA))
                .andExpect(jsonPath("$.data.staff_id").value(holderA));

        JsonNode listed = listedDevice(code("a"));
        assertThat(listed).as("设备台账列出该行").isNotNull();
        assertThat(listed.path("terminal_type").asText()).isEqualTo("security_xray");
        assertThat(listed.path("device_model").asText()).isEqualTo("FISCAN CX6040");
        assertThat(listed.path("location_point_id").asText()).isEqualTo(spaceA);
    }

    // 人工登记故障即开维修工单：报修类、来源平台、设备所在空间与项目、待派单；同一提交编号重放不重复开单。
    @Test
    void xrayFaultOpensRepairWorkOrder() throws Exception {
        String id = register("f");
        String body = reasonJson(key("fault"), "传送带卡滞");
        String workOrderId = read(send(DEVICES + "/" + id + "/repair", body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("repair"))
                .andExpect(jsonPath("$.data.repair_work_order_id").isNotEmpty()))
                .path("data").path("repair_work_order_id").asText();

        Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT work_order_type, source, space_id, project_id, status, reporter_id, description "
                        + "FROM facility_work_orders WHERE id = ?", workOrderId);
        assertThat(order.get("work_order_type")).isEqualTo("repair");
        assertThat(order.get("source")).isEqualTo("platform");
        assertThat(order.get("space_id")).isEqualTo(spaceA);
        assertThat(order.get("project_id")).isEqualTo(projectA);
        assertThat(order.get("status")).isEqualTo("pending_assign");
        assertThat(order.get("reporter_id")).isEqualTo(projectStaff);
        assertThat(order.get("description")).isEqualTo("传送带卡滞");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT repair_work_order_id FROM terminal_devices WHERE id = ?", String.class, id)).isEqualTo(workOrderId);

        send(DEVICES + "/" + id + "/repair", body).andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM facility_work_orders WHERE space_id = ?", Integer.class, spaceA)).isEqualTo(1);
    }

    // 修好后恢复在用；第 82 条 82c-R2：工作手机修复完成回待领用（returned），不回在用。
    @Test
    void xrayRepairComplete() throws Exception {
        String id = register("c");
        send(DEVICES + "/" + id + "/repair", reasonJson(key("c-fault"), "显示器黑屏")).andExpect(status().isOk());
        send(DEVICES + "/" + id + "/repair-complete", reasonJson(key("c-done"), "已更换显示器"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("bound"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM terminal_devices WHERE id = ?", String.class, id)).isEqualTo("bound");

        String phone = "td_x63_phone_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    bound_space_id, staff_id, status, created_at, updated_at)
                VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, ?, 'repair', now(), now())
                """, phone, code("phone"), code("phone"), spaceA, holderA);
        send(DEVICES + "/" + phone + "/repair-complete", reasonJson(key("phone-done"), "修好"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("returned"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM terminal_devices WHERE id = ?", String.class, phone)).isEqualTo("returned");
    }

    // 员工端领用绑定入口不收安检 X 光设备（只在后台登记）。
    @Test
    void xrayCannotBindViaTerminalBind() throws Exception {
        send(DEVICES + "/bind", """
                {"idempotency_key": "%s", "device_id": "%s", "terminal_type": "security_xray", "binding_scope": "space",
                 "bound_space_id": "%s", "staff_id": "%s"}
                """.formatted(key("bind"), code("bind"), spaceA, holderA))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM terminal_devices WHERE device_code = ?", Integer.class, code("bind"))).isZero();
    }

    // 项目员工不能把设备登记到别的项目的空间：403、留失败痕、不写台账。
    @Test
    void otherProjectSpaceRejected() throws Exception {
        send(DEVICES + "/entry-devices", registerJson(key("other"), "o", spaceB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'terminal.device.entry_device_register_failed' "
                        + "AND event_result = 'failure' AND idempotency_key = ?", Integer.class, key("other"))).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM terminal_devices WHERE device_code = ?", Integer.class, code("o"))).isZero();
    }

    // 同一编号不能登记两次。
    @Test
    void duplicateCodeRejected() throws Exception {
        register("d");
        send(DEVICES + "/entry-devices", registerJson(key("dup"), "d", spaceA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CODE_EXISTS"));
    }

    // 第 82 条 82c-R1：人脸扫描设备、检票闸机走同一后台登记入口进台账，登记即在用，留成功审计。
    @Test
    void registerFaceScannerAndTicketGate() throws Exception {
        for (String type : List.of("face_scanner", "ticket_gate")) {
            send(DEVICES + "/entry-devices", registerJson(key("reg-" + type), type, spaceA, type))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.device_type").value(type))
                    .andExpect(jsonPath("$.data.status").value("bound"))
                    .andExpect(jsonPath("$.data.device_model").value("FISCAN CX6040"));
            JsonNode listed = listedDevice(code(type));
            assertThat(listed).as("设备台账列出 " + type).isNotNull();
            assertThat(listed.path("terminal_type").asText()).isEqualTo(type);
            assertThat(listed.path("status").asText()).isEqualTo("bound");
            assertThat(listed.path("device_model").asText()).isEqualTo("FISCAN CX6040");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM audit_events WHERE event_name = 'terminal.device.entry_device_registered' "
                            + "AND event_result = 'success' AND idempotency_key = ?", Integer.class, key("reg-" + type)))
                    .isEqualTo(1);
        }
    }

    // 第 82 条 82c-R1（BSM:800 维修工单留痕只写安检环节）：人脸扫描设备登记故障只进维修中、不开维修工单；修好后恢复在用。
    @Test
    void entryDeviceFaultOpensNoWorkOrder() throws Exception {
        String id = register("face", "face_scanner");
        send(DEVICES + "/" + id + "/repair", reasonJson(key("face-fault"), "摄像头无法对焦"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("repair"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT repair_work_order_id FROM terminal_devices WHERE id = ?", String.class, id)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM facility_work_orders WHERE space_id = ?", Integer.class, spaceA)).isZero();
        send(DEVICES + "/" + id + "/repair-complete", reasonJson(key("face-done"), "已更换摄像头"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("bound"));
    }

    // 第 82 条 82c-R1：后台登记入口只收入场三道环节设备，别的类型 400、不落台账。
    @Test
    void unknownEntryTypeRejected() throws Exception {
        send(DEVICES + "/entry-devices", registerJson(key("kiosk"), "k", spaceA, "kiosk"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM terminal_devices WHERE device_code = ?", Integer.class, code("k"))).isZero();
    }

    // 第 82 条 82c-R3（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」）：在用的入场设备更换责任人，原因必填；设备仍在用，
    // 留状态流水与成功审计。修复前没有该入口：200 的断言即红。
    @Test
    void changeResponsibleStaff() throws Exception {
        String id = register("rs");
        send(DEVICES + "/" + id + "/responsible-staff", staffJson(key("rs"), holderA2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staff_id").value(holderA2))
                .andExpect(jsonPath("$.data.status").value("bound"));
        assertThat(jdbcTemplate.queryForMap("SELECT staff_id, status FROM terminal_devices WHERE id = ?", id))
                .containsEntry("staff_id", holderA2).containsEntry("status", "bound");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM terminal_device_status_events WHERE device_id = ? "
                        + "AND event_type = 'terminal.device.responsible_staff_changed' AND staff_id = ? AND reason = '原责任人调岗'",
                Integer.class, id, holderA2)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'terminal.device.responsible_staff_changed' "
                        + "AND event_result = 'success' AND idempotency_key = ?", Integer.class, key("rs"))).isEqualTo(1);
    }

    // 82c-R3：新责任人须是本项目的人——填另一项目的员工 403、留失败痕、责任人不变；越范围请求不占幂等位，
    // 同一提交编号改填本项目员工重试 200。
    @Test
    void responsibleStaffOtherProjectRejected() throws Exception {
        String id = register("rso");
        send(DEVICES + "/" + id + "/responsible-staff", staffJson(key("rso"), holderB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'terminal.device.responsible_staff_change_failed' "
                        + "AND event_result = 'failure' AND idempotency_key = ?", Integer.class, key("rso"))).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT staff_id FROM terminal_devices WHERE id = ?", String.class, id)).isEqualTo(holderA);
        send(DEVICES + "/" + id + "/responsible-staff", staffJson(key("rso"), holderA2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staff_id").value(holderA2));
    }

    // 82c-R3：与原责任人相同 409 DEVICE_STAFF_UNCHANGED；工作手机等随领用、归还换持有人，不走该入口 409 INVALID_DEVICE_STATUS；
    // 报废后 409 DEVICE_TERMINAL_STATUS。
    @Test
    void responsibleStaffChangeRejected() throws Exception {
        String id = register("rsu");
        send(DEVICES + "/" + id + "/responsible-staff", staffJson(key("rsu-same"), holderA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_STAFF_UNCHANGED"));

        String phone = "td_x63_rsp_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    bound_space_id, staff_id, status, created_at, updated_at)
                VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, ?, 'bound', now(), now())
                """, phone, code("rsp"), code("rsp"), spaceA, holderA);
        send(DEVICES + "/" + phone + "/responsible-staff", staffJson(key("rsp"), holderA2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INVALID_DEVICE_STATUS"));

        send(DEVICES + "/" + id + "/scrap", reasonJson(key("rsu-scrap"), "无法修复")).andExpect(status().isOk());
        send(DEVICES + "/" + id + "/responsible-staff", staffJson(key("rsu-after"), holderA2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_TERMINAL_STATUS"));
    }

    private String staffJson(String key, String staff) {
        return """
                {"idempotency_key": "%s", "staff_id": "%s", "reason": "原责任人调岗"}
                """.formatted(key, staff);
    }

    private String register(String tag) throws Exception {
        return register(tag, "security_xray");
    }

    private String register(String tag, String type) throws Exception {
        return read(send(DEVICES + "/entry-devices", registerJson(key("reg-" + tag), tag, spaceA, type))
                .andExpect(status().isOk())).path("data").path("id").asText();
    }

    private String code(String tag) {
        return "XR-" + tag + "-" + suffix;
    }

    private String key(String name) {
        return "x63-" + name + "-" + suffix;
    }

    private String registerJson(String key, String tag, String space) {
        return registerJson(key, tag, space, "security_xray");
    }

    private String registerJson(String key, String tag, String space, String type) {
        return """
                {"idempotency_key": "%s", "terminal_type": "%s", "device_code": "%s", "device_model": "FISCAN CX6040",
                 "bound_space_id": "%s", "staff_id": "%s"}
                """.formatted(key, type, code(tag), space, holderA);
    }

    private String reasonJson(String key, String reason) {
        return """
                {"idempotency_key": "%s", "reason": "%s"}
                """.formatted(key, reason);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与员工表当前行一致）。
    private RequestPostProcessor session() {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(projectStaff, projectStaff, "staff", projectA, null, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions send(String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(session()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return new ObjectMapper().readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode listedDevice(String deviceCode) throws Exception {
        JsonNode body = read(mockMvc.perform(get(DEVICES).with(session())).andExpect(status().isOk()));
        for (JsonNode item : body.get("data").get("items")) {
            if (deviceCode.equals(item.path("device_code").asText())) {
                return item;
            }
        }
        return null;
    }
}
