package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
 * 第十八批 BJ 段第 261 条 261-B18-R2（真库、真实入口、真实授权链、蛇形键）。依据：PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、
 * 渠道重复回调都只算一次）」；PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」。楼宇巡检的新建模板、新建任务、开始、提交记录、
 * 上报异常、复检、主管确认同编号重发，都返回第一次的结果并带已完成标记：不多插一条巡检记录、不多建一张整改工单、不多建模板与任务。
 * 修复前这些入口不认请求编号：重发提交记录或上报异常被状态闸 400，重发新建模板、任务各多建一条。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FacilityInspectionOneShotJdbcIntegrationTests {

    private static final String BASE = "/api/v1/facility/inspections";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_b18i_" + s;
    private final String template = "fit_b18i_" + s;
    private final String staffId = "usr_b18i_" + s;
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','巡检重发回归综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO facility_inspection_templates(id,name,inspection_type,abnormal_mode,project_id) VALUES(?,'消防通道巡检','fire','to_work_order',?)",
                template, project);
        staff = new StaffRequestFixture(jdbc).staff(staffId, "staff", project, null, "facility.inspection.manage",
                "facility.inspection.record_assigned", "facility.inspection.report_exception_assigned", "facility.inspection.view_assigned");
    }

    /** 提交异常记录同编号重发：巡检记录只一条、整改工单只一张，任务停在整改中。 */
    @Test
    void abnormalRecordResubmittedWithSameKeyRaisesOneRectificationOrder() throws Exception {
        String task = task("in_progress");
        String body = "{\"idempotency_key\":\"b18i-rec-" + s + "\",\"result\":\"abnormal\",\"abnormal_note\":\"消防通道被杂物堵住\"}";
        String first = replay(BASE + "/tasks/" + task + "/records", body);
        assertThat(first).isNotBlank();
        assertThat(count("SELECT count(*) FROM facility_inspection_records WHERE task_id=?", task)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM facility_work_orders WHERE source='inspection' AND project_id=?", project)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM facility_inspection_tasks WHERE id=?", String.class, task)).isEqualTo("rectifying");
        assertThat(count("SELECT count(*) FROM audit_events WHERE event_name='facility.inspection_record_submitted' AND business_id=?", first)).isEqualTo(1);
    }

    /** 上报异常同编号重发：同上，只一条记录、一张整改工单。 */
    @Test
    void exceptionReportResubmittedWithSameKeyRaisesOneRectificationOrder() throws Exception {
        String task = task("pending");
        String body = "{\"idempotency_key\":\"b18i-exc-" + s + "\",\"abnormal_note\":\"防火门闭门器损坏\"}";
        String first = replay(BASE + "/tasks/" + task + "/report-exception", body);
        assertThat(count("SELECT count(*) FROM facility_inspection_records WHERE task_id=?", task)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM facility_work_orders WHERE source='inspection' AND project_id=?", project)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM audit_events WHERE event_name='facility.inspection_exception_reported' AND business_id=?", first)).isEqualTo(1);
    }

    /** 新建模板、新建任务同编号重发各只一条；开始、复检、主管确认同编号重发返回第一次的结果。 */
    @Test
    void templateTaskAndStateEntriesReplaySameKey() throws Exception {
        String name = "夜间安保巡检模板" + s;
        replay(BASE + "/templates", "{\"idempotency_key\":\"b18i-tpl-" + s + "\",\"name\":\"" + name
                + "\",\"inspection_type\":\"security\",\"abnormal_mode\":\"light\",\"project_id\":\"" + project + "\"}");
        assertThat(count("SELECT count(*) FROM facility_inspection_templates WHERE name=?", name)).isEqualTo(1);

        String title = "夜间安保巡检任务" + s;
        String created = replay(BASE + "/tasks", "{\"idempotency_key\":\"b18i-task-" + s + "\",\"inspection_type\":\"security\",\"title\":\"" + title
                + "\",\"assignee_id\":\"" + staffId + "\",\"project_id\":\"" + project + "\"}");
        assertThat(count("SELECT count(*) FROM facility_inspection_tasks WHERE title=?", title)).isEqualTo(1);

        replay(BASE + "/tasks/" + created + "/start", "{\"idempotency_key\":\"b18i-start-" + s + "\"}");
        assertThat(count("SELECT count(*) FROM audit_events WHERE event_name='facility.inspection_task_started' AND business_id=?", created)).isEqualTo(1);

        String rectifying = task("rectifying");
        replay(BASE + "/tasks/" + rectifying + "/recheck", "{\"idempotency_key\":\"b18i-recheck-" + s + "\",\"note\":\"杂物已清走\"}");
        replay(BASE + "/tasks/" + rectifying + "/supervisor-confirm", "{\"idempotency_key\":\"b18i-confirm-" + s + "\"}");
        assertThat(jdbc.queryForObject("SELECT status FROM facility_inspection_tasks WHERE id=?", String.class, rectifying)).isEqualTo("closed");
        assertThat(count("SELECT count(*) FROM audit_events WHERE event_name='facility.inspection_task_rechecked' AND business_id=?", rectifying)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM audit_events WHERE event_name='facility.inspection_task_closed' AND business_id=?", rectifying)).isEqualTo(1);
    }

    /** 同一编号、同一正文连发两次：第一次照常办成，第二次 200、带已完成标记、返回同一个编号。返回该编号。 */
    private String replay(String url, String body) throws Exception {
        String first = JsonPath.read(mockMvc.perform(post(url).with(staff).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        mockMvc.perform(post(url).with(staff).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true)).andExpect(jsonPath("$.data.id").value(first));
        return first;
    }

    private String task(String taskStatus) {
        String id = "fitask_b18i_" + taskStatus + "_" + s;
        jdbc.update("INSERT INTO facility_inspection_tasks(id,template_id,inspection_type,status,title,assignee_id,project_id) VALUES(?,?,'fire',?,'消防通道巡检',?,?)",
                id, template, taskStatus, staffId, project);
        return id;
    }

    private int count(String sql, String arg) {
        return jdbc.queryForObject(sql, Integer.class, arg);
    }
}
