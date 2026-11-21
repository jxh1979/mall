package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
 * 第十八批 BS 段第 379 条 379-R1（BSM:657「……设备告警与巡检异常自动转工单。」；PGM:842「巡检任务分派到人，现场扫码或定位留痕，发现异常生成整改工单。」；
 * PGM:681「……只能处理被指派的工单、负责的区域或任务……」）：外包巡检员对派给本人、挂「异常转整改工单」模板的任务上报异常或提交异常记录，
 * 巡检记录照常落库、任务转整改中，并自动建一张整改工单（提单方＝该外包账号、待派单）。走真实 HTTP、真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FacilityInspectionOutsourcedToWorkOrderJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String digits = String.valueOf(System.nanoTime());
    private final String project = "prj_f379_" + digits;
    private final String vendorId = "usr_f379_vendor_" + digits;
    private final String templateId = "fitpl_f379_" + digits;
    private RequestPostProcessor vendor;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','楼宇巡检综合体','active',now(),now())",
                project);
        jdbc.update("INSERT INTO facility_inspection_templates(id,name,inspection_type,abnormal_mode,project_id) VALUES(?,'消防通道巡检','fire','to_work_order',?)",
                templateId, project);
        vendor = new StaffRequestFixture(jdbc).staff(vendorId, "outsourced_vendor", project, null,
                "facility.inspection.record_assigned", "facility.inspection.report_exception_assigned");
    }

    // 上报异常：待开始的任务直接上报 → 记录落库、任务整改中、自动建整改工单。
    @Test
    void outsourcedReportExceptionCreatesRectificationWorkOrder() throws Exception {
        String taskId = task("pending");
        JsonNode record = submit(taskId, "/report-exception",
                Map.of("idempotency_key", "f379-exception-" + System.nanoTime(), "abnormal_note", "消防通道堆放杂物"));
        assertWorkOrderCreated(taskId, record);
    }

    // 提交异常记录：进行中的任务提交「异常」→ 同上。
    @Test
    void outsourcedAbnormalRecordCreatesRectificationWorkOrder() throws Exception {
        String taskId = task("in_progress");
        JsonNode record = submit(taskId, "/records",Map.of("idempotency_key", "f379-record-" + System.nanoTime(),
                "result", "abnormal", "abnormal_note", "消防门闭门器损坏"));
        assertWorkOrderCreated(taskId, record);
    }

    private void assertWorkOrderCreated(String taskId, JsonNode record) {
        assertThat(record.path("result").asText()).isEqualTo("abnormal");
        assertThat(record.path("inspector_id").asText()).isEqualTo(vendorId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_inspection_records WHERE task_id=?", Integer.class, taskId))
                .isEqualTo(1);
        Map<String, Object> task = jdbc.queryForMap("SELECT status, work_order_id FROM facility_inspection_tasks WHERE id=?", taskId);
        assertThat(task.get("status")).isEqualTo("rectifying");
        assertThat(task.get("work_order_id")).isNotNull();
        Map<String, Object> order = jdbc.queryForMap(
                "SELECT work_order_type, source, status, priority, project_id, reporter_id FROM facility_work_orders WHERE id=?",
                task.get("work_order_id"));
        assertThat(order).containsEntry("work_order_type", "rectification").containsEntry("source", "inspection")
                .containsEntry("status", "pending_assign").containsEntry("priority", "normal")
                .containsEntry("project_id", project).containsEntry("reporter_id", vendorId);
    }

    private String task(String taskStatus) {
        String id = "fit_f379_" + taskStatus + "_" + System.nanoTime();
        jdbc.update("INSERT INTO facility_inspection_tasks(id,template_id,inspection_type,status,title,assignee_id,project_id) VALUES(?,?,'fire',?,'消防通道巡检',?,?)",
                id, templateId, taskStatus, vendorId, project);
        return id;
    }

    private JsonNode submit(String taskId, String path, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/facility/inspections/tasks/" + taskId + path).with(vendor)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(response).path("data");
    }
}
