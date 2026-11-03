package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 N 段第 120 条 120-R6（BSM:992「工程师可在授权后填写游乐设施定期检修记录，检修单位作为供应商记录。」；BSM:993「检修完成后必须由主管确认，
 * 设施才能恢复营业。」；BSM:1269、BSM:1270）：一体机上工程师填检修、主管确认恢复营业——确认人取当前登录主管（请求体多送的确认人不算数）；
 * 在营项目的检修也能确认（项目不动），检修中的项目确认后回到开放；检修不通过的不能确认。请求按一体机真实形状（一体机终端头、登记设备号）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class KioskAmusementMaintenanceJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String complex = "prj_kam_" + s;
    private final String merchant = "mch_kam_" + s;
    private final String space = "space_kam_" + s;
    private final String kioskDevice = "kam-kiosk-" + s;
    private final String vendor = "sup_kam_" + s;
    private final String supervisorId = "usr_kam_sup_" + s;
    private RequestPostProcessor engineer;
    private RequestPostProcessor supervisor;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                complex, complex);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '游乐商户', 'amusement', 'active', now(), now(), ?)", merchant, complex);
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '游乐服务点', 'shop', 'active', now(), now())", space, complex);
        jdbc.update("INSERT INTO inventory_suppliers (id, supplier_name, supplier_type, status, created_at, updated_at)"
                + " VALUES (?, '游乐检修公司', 'general', 'active', now(), now())", vendor);
        // 游乐商户租着该空间（合同生效），主管确认恢复营业过「越界空间」准入闸。
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        jdbc.update("INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,"
                + " lifecycle_status, project_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'effective', ?, now(), now())",
                "ct_kam_" + s, merchant, "subj_kam_" + s, "subj_kam_" + s, today.minusDays(5), today.plusDays(30), complex);
        jdbc.update("INSERT INTO contract_spaces (contract_id, space_id) VALUES (?, ?)", "ct_kam_" + s, space);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor admin = staffs.staff("usr_kam_admin_" + s, "platform_admin", complex, null,
                "terminal.device.bind", "terminal.device.lifecycle");
        mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kam-bind-" + s, "device_id", kioskDevice, "terminal_type", "kiosk",
                                "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk());
        engineer = kiosk(staffs.staff("usr_kam_eng_" + s, "staff", complex, merchant, "amusement.maintenance.submit"));
        supervisor = kiosk(staffs.staff(supervisorId, "staff", complex, merchant, "amusement.maintenance.confirm"));
    }

    @Test
    void kioskEngineerSubmitsAndCurrentSupervisorConfirmsInServiceProjectWithoutTouchingIt() throws Exception {
        String project = project("open");
        String device = "dev_kam_" + s;

        JsonNode record = submit(project, device, "pass", null);
        assertThat(record.path("status").asText()).isEqualTo("pending_confirm");
        assertThat(record.path("source_terminal").asText()).isEqualTo("kiosk");
        assertThat(record.path("maintenance_vendor_id").asText()).isEqualTo(vendor);
        assertThat(deviceStatus(device)).isEqualTo("maintenance");

        String body = mockMvc.perform(post("/api/v1/amusement/maintenance-records/" + record.path("id").asText() + "/confirm-resume")
                        .with(supervisor).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kam-confirm-" + s, "supervisor_confirmed_by", "usr_other"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode confirmed = json.readTree(body).path("data");
        assertThat(confirmed.path("status").asText()).isEqualTo("confirmed");
        assertThat(confirmed.path("supervisor_confirmed_by").asText()).isEqualTo(supervisorId);
        assertThat(deviceStatus(device)).isEqualTo("operational");
        assertThat(openStatus(project)).isEqualTo("open");
    }

    @Test
    void confirmingMaintenanceOfProjectUnderMaintenanceReopensIt() throws Exception {
        String project = project("maintenance");
        JsonNode record = submit(project, "dev_kam_m_" + s, "pass", null);

        mockMvc.perform(post("/api/v1/amusement/maintenance-records/" + record.path("id").asText() + "/confirm-resume")
                        .with(supervisor).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kam-confirm-m-" + s))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supervisor_confirmed_by").value(supervisorId));
        assertThat(openStatus(project)).isEqualTo("open");
    }

    @Test
    void failedMaintenanceCannotBeConfirmed() throws Exception {
        String project = project("open");
        JsonNode record = submit(project, "dev_kam_f_" + s, "fail", "限位器异常");
        assertThat(record.path("status").asText()).isEqualTo("submitted");

        mockMvc.perform(post("/api/v1/amusement/maintenance-records/" + record.path("id").asText() + "/confirm-resume")
                        .with(supervisor).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kam-confirm-f-" + s))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MAINTENANCE_NOT_PASSED"));
    }

    private String project(String openStatus) {
        String id = "amp_kam_" + openStatus + "_" + s;
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, merchant_id, space_id, created_at, updated_at)"
                + " VALUES (?, '旋转木马', 30, ?, ?, ?, now(), now())", id, openStatus, merchant, space);
        return id;
    }

    private JsonNode submit(String project, String device, String result, String exceptionNote) throws Exception {
        Map<String, Object> request = new HashMap<>(Map.of(
                "idempotency_key", "kam-submit-" + device, "project_id", project, "device_id", device,
                "maintenance_vendor_id", vendor, "engineer_name", "张工", "result", result,
                "source_terminal", "kiosk", "location_point_id", space));
        if (exceptionNote != null) {
            request.put("exception_note", exceptionNote);
        }
        String body = mockMvc.perform(post("/api/v1/amusement/maintenance-records").with(engineer)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).path("data");
    }

    private String deviceStatus(String device) {
        return jdbc.queryForObject("SELECT status FROM amusement_device_statuses WHERE device_id = ?", String.class, device);
    }

    private String openStatus(String project) {
        return jdbc.queryForObject("SELECT open_status FROM amusement_projects WHERE id = ?", String.class, project);
    }

    /** 一体机真实形状：一体机终端头＋登记设备号（不签名，默认 grace）。 */
    private RequestPostProcessor kiosk(RequestPostProcessor who) {
        return request -> {
            MockHttpServletRequest r = who.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "kiosk");
            r.addHeader("X-Device-Id", kioskDevice);
            return r;
        };
    }
}
