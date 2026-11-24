package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CG 段第 410 条 410-R1（真库、真实入口、真实授权链、蛇形键）。依据：PGM:846「发现异常支持「轻记录/重转整改工单」分级与「全部转整改工单」两种模式，
 * 整改后复检、主管确认闭环」；PGM:842「发现异常生成整改工单。」。模板 abnormal_mode=light（分级，不挂模板的任务同此）时，异常由巡检人提交时选轻重：
 * light 记录后任务关闭、不建工单；heavy 转整改中并自动建整改工单；缺分级 400。to_work_order（全部转整改工单）不读分级，异常一律转工单。
 * 修复前分级模式只有「轻」一支：带 heavy 也直接关闭、不建工单；不带分级也照收。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FacilityInspectionAbnormalLevelJdbcIntegrationTests {

    private static final String BASE = "/api/v1/facility/inspections/tasks/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_cg410_" + s;
    private final String graded = "fit_cg410_light_" + s;
    private final String allToWorkOrder = "fit_cg410_wo_" + s;
    private final String staffId = "usr_cg410_" + s;
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','巡检分级回归综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO facility_inspection_templates(id,name,inspection_type,abnormal_mode,project_id) VALUES(?,'消防通道巡检','fire','light',?)",
                graded, project);
        jdbc.update("INSERT INTO facility_inspection_templates(id,name,inspection_type,abnormal_mode,project_id) VALUES(?,'消防泵房巡检','fire','to_work_order',?)",
                allToWorkOrder, project);
        staff = new StaffRequestFixture(jdbc).staff(staffId, "staff", project, null, "facility.inspection.manage",
                "facility.inspection.record_assigned");
    }

    /** 分级模式选重：任务转整改中并回写整改工单，项目里多一张来源巡检的整改工单。 */
    @Test
    void gradedHeavyRaisesRectificationOrder() throws Exception {
        String task = task(graded, "in_progress");
        record(task, "heavy").andExpect(status().isOk());
        assertRectifying(task);
    }

    /** 分级模式选轻：记录后任务关闭，不建工单。 */
    @Test
    void gradedLightRecordsAndCloses() throws Exception {
        String task = task(graded, "in_progress");
        record(task, "light").andExpect(status().isOk());
        assertThat(taskStatus(task)).isEqualTo("closed");
        assertThat(jdbc.queryForObject("SELECT work_order_id FROM facility_inspection_tasks WHERE id=?", String.class, task)).isNull();
        assertThat(inspectionWorkOrders()).isZero();
    }

    /** 分级模式不选轻重：400，不落记录，任务仍在巡检中。 */
    @Test
    void gradedWithoutLevelRejected() throws Exception {
        String task = task(graded, "in_progress");
        record(task, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_inspection_records WHERE task_id=?", Integer.class, task)).isZero();
        assertThat(taskStatus(task)).isEqualTo("in_progress");
    }

    /** 不挂模板的任务按分级：选重同样转整改工单。 */
    @Test
    void untemplatedHeavyRaisesRectificationOrder() throws Exception {
        String task = task(null, "in_progress");
        record(task, "heavy").andExpect(status().isOk());
        assertRectifying(task);
    }

    /** 全部转整改工单模式不读分级：带 light 也转整改工单。 */
    @Test
    void allToWorkOrderIgnoresLevel() throws Exception {
        String task = task(allToWorkOrder, "in_progress");
        record(task, "light").andExpect(status().isOk());
        assertRectifying(task);
    }

    /** 平台账号对待巡检任务上报异常、分级选重：同样转整改中并建整改工单。 */
    @Test
    void gradedExceptionReportHeavyRaisesRectificationOrder() throws Exception {
        String task = task(graded, "pending");
        send(BASE + task + "/report-exception",
                "{\"idempotency_key\":\"cg410-exc-" + s + "\",\"abnormal_note\":\"防火门闭门器损坏\",\"abnormal_level\":\"heavy\"}")
                .andExpect(status().isOk());
        assertRectifying(task);
    }

    private ResultActions record(String task, String level) throws Exception {
        String levelField = level == null ? "" : ",\"abnormal_level\":\"" + level + "\"";
        return send(BASE + task + "/records", "{\"idempotency_key\":\"cg410-rec-" + s + "\",\"result\":\"abnormal\","
                + "\"abnormal_note\":\"消防通道被杂物堵住\"" + levelField + "}");
    }

    private ResultActions send(String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(staff).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertRectifying(String task) {
        assertThat(taskStatus(task)).isEqualTo("rectifying");
        assertThat(jdbc.queryForObject("SELECT work_order_id FROM facility_inspection_tasks WHERE id=?", String.class, task)).isNotBlank();
        assertThat(inspectionWorkOrders()).isEqualTo(1);
    }

    private int inspectionWorkOrders() {
        return jdbc.queryForObject("SELECT count(*) FROM facility_work_orders WHERE source='inspection' AND work_order_type='rectification'"
                + " AND project_id=?", Integer.class, project);
    }

    private String taskStatus(String task) {
        return jdbc.queryForObject("SELECT status FROM facility_inspection_tasks WHERE id=?", String.class, task);
    }

    private String task(String template, String taskStatus) {
        String id = "fitask_cg410_" + taskStatus + "_" + s;
        jdbc.update("INSERT INTO facility_inspection_tasks(id,template_id,inspection_type,status,title,assignee_id,project_id)"
                + " VALUES(?,?,'fire',?,'消防通道巡检',?,?)", id, template, taskStatus, staffId, project);
        return id;
    }
}
