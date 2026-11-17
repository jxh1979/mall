package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AP 段第 229 条 229-R2（PGM:846「运营细则（已确认）：……现场留痕支持拍照、扫二维码、定位三种……」；先例第 32 条删而不补）：
 * 外包巡检页（VendorInspectionsPage）停发写死的照片编号与二维码，记录与异常上报按停发后的网页形状提交，落库照片为空、二维码为空。
 * 229-R8（BSM:940「……一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕……」；PGM:899「……谁发起……都要能查到。」）：
 * 巡检记录的巡检人取提交账号，请求体所填不采信（同服务异常上报已取登录人）。走真实 HTTP、真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FacilityInspectionWebShape229Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String digits = String.valueOf(System.nanoTime());
    private final String project = "prj_f229_" + digits;
    private final String vendorId = "usr_f229_vendor_" + digits;
    private RequestPostProcessor vendor;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','楼宇巡检综合体','active',now(),now())",
                project);
        vendor = new StaffRequestFixture(jdbc).staff(vendorId, "outsourced_vendor", project, null,
                "facility.inspection.record_assigned", "facility.inspection.report_exception_assigned");
    }

    // 外包账号记录派给本人的进行中任务：不带巡检人 → 巡检人＝本账号，照片为空、二维码为空；带别人账号 → 仍记本账号。
    @Test
    void outsourcedRecordTakesLoginAccountAsInspector() throws Exception {
        JsonNode record = submit(vendor, task("in_progress", vendorId), null);
        assertThat(record.path("inspector_id").asText()).isEqualTo(vendorId);
        assertThat(record.path("photos").size()).isZero();
        assertThat(record.path("qr_code").asText(null)).isNull();

        JsonNode other = submit(vendor, task("in_progress", vendorId), "usr_f229_other_" + digits);
        assertThat(other.path("inspector_id").asText()).isEqualTo(vendorId);
    }

    // 外包账号对派给本人的任务上报异常：照片为空、二维码为空，巡检人＝本账号。
    @Test
    void outsourcedExceptionReportKeepsNoFabricatedEvidence() throws Exception {
        String body = mockMvc.perform(post("/api/v1/facility/inspections/tasks/" + task("pending", vendorId) + "/report-exception").with(vendor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "f229-exception-" + digits, "abnormal_note", "消防门闭门器损坏"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode record = json.readTree(body).path("data");
        assertThat(record.path("photos").size()).isZero();
        assertThat(record.path("qr_code").asText(null)).isNull();
        assertThat(record.path("inspector_id").asText()).isEqualTo(vendorId);
    }

    // 平台物业账号代录本项目任务：巡检人＝平台账号。
    @Test
    void platformAccountRecordTakesItsOwnId() throws Exception {
        String platformId = "usr_f229_platform_" + digits;
        RequestPostProcessor platform = new StaffRequestFixture(jdbc).staff(platformId, "staff", project, null,
                "facility.inspection.record_assigned", "facility.inspection.manage");
        JsonNode record = submit(platform, task("in_progress", vendorId), null);
        assertThat(record.path("inspector_id").asText()).isEqualTo(platformId);
    }

    private String task(String taskStatus, String assignee) {
        String id = "fit_f229_" + taskStatus + "_" + System.nanoTime();
        jdbc.update("INSERT INTO facility_inspection_tasks(id,inspection_type,status,title,assignee_id,project_id) VALUES(?,'security',?,'夜间安保巡检',?,?)",
                id, taskStatus, assignee, project);
        return id;
    }

    private JsonNode submit(RequestPostProcessor who, String taskId, String inspectorId) throws Exception {
        Map<String, Object> request = new HashMap<>(Map.of("idempotency_key", "f229-record-" + taskId, "result", "normal"));
        if (inspectorId != null) {
            request.put("inspector_id", inspectorId);
        }
        String body = mockMvc.perform(post("/api/v1/facility/inspections/tasks/" + taskId + "/records").with(who)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).path("data");
    }
}
