package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
 * 第十八批 CJ 段第 416 条 416-R2（BSM:1474「主管排班：排到几点几分，可指定当天工作点位（比如某个服务区、某片保洁区域）……排班表最终发布须经该主管的
 * 上一级审批后生效。」；PGM:287「展示给用户的一切文字……日期时间等，必须全部为中文，不得出现任何英文」）：排班发布审批原件（复核单 reason，审批链页
 * 「提交时原始内容与凭证」原样给上一级看）的起止写北京时间到分钟、点位写空间名（未选写「未指定」）、夜班职责写职责句不写项目编号；单条与按周批量同一处拼。
 * 提交人是酒吧排班主管（商户账号持 staff.shift.manage），复核单由平台管理员读。真实 HTTP、蛇形键、真实授权链；夹具随测试事务回滚。
 * 各单用不同日期，免得撞同人同开始；夜班一单开始的北京日期等于工作日期、员工登记项目是真实项目（JdbcStaffRepository.requireNightDutySchedule）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StaffShiftApprovalSummaryJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_b108sa_" + s;
    private final String merchant = "mch_b108sa_" + s;
    private final String company = "ent_b108sa_" + s;
    private final String employee = "usr_b108sa_e_" + s;
    private final String managerId = "usr_b108sa_m_" + s;
    private int seq;
    private RequestPostProcessor manager;
    private RequestPostProcessor admin;
    private String spaceId;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "排班审批原件综合体" + s);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) "
                + "VALUES(?,'排班审批原件主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'排班审批原件酒吧','bar',?,?,?,?,'active',now(),now())", merchant, company, company, company, project);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                employee, employee, project, merchant);
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,?,?,'approved','active','employed',false,now(),now())",
                "stf_b108sa_" + s, project, merchant, employee, "赵六" + s, company);
        manager = new StaffRequestFixture(jdbc).staff(managerId, "staff", project, merchant, "staff.shift.manage");
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", managerId, project);
        admin = new StaffRequestFixture(jdbc).staff("usr_b108sa_admin_" + s, "platform_admin", null, null,
                "governance.space.manage", "governance.supervisor_review.view");
        String body = "{\"idempotency_key\":\"b108sa-sp-" + s + "\",\"project_id\":\"" + project + "\",\"merchant_id\":\"" + merchant
                + "\",\"space_name\":\"一号服务区" + s + "\",\"space_type\":\"zone\",\"status\":\"active\"}";
        spaceId = JsonPath.read(mockMvc.perform(post("/api/v1/platform/spaces").with(admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    // 单条：起止为北京时间到分钟、点位为空间名；不带点位的一单写「未指定」。修复前原件是 UTC 原值（2099-01-01T01:00:00Z）与空间编号。
    @Test
    void singleShiftOriginalShowsBeijingMinutesAndSpaceName() throws Exception {
        String reason = reasonOf(single("2099-01-01", "[\"" + spaceId + "\"]", false));
        assertThat(reason).contains("赵六" + s + "：新增 → 2099-01-01 09:00 至 2099-01-01 17:00").contains("点位：一号服务区" + s)
                .doesNotContain("T01:00").doesNotContain(spaceId);

        assertThat(reasonOf(single("2099-01-02", "[]", false))).contains("点位：未指定");
    }

    // 夜班值班经理：写职责句，不写项目编号。修复前写「项目 prj_…」。
    @Test
    void nightDutyOriginalStatesTheDutyWithoutProjectId() throws Exception {
        String reason = reasonOf(single("2099-01-03", "[]", true));
        assertThat(reason).contains("夜班值班职责：承担所属项目本日夜班值班经理职责").doesNotContain(project);
    }

    // 按周批量：同一处拼原件，每日时段为北京时间到分钟。修复前为 2099-01-05T02:00:00Z。
    @Test
    void batchOriginalShowsBeijingMinutes() throws Exception {
        String body = "{\"idempotency_key\":\"b108sa-b-" + s + "\",\"user_ids\":[\"" + employee + "\"],\"start_date\":\"2099-01-05\",\"days\":2,"
                + "\"weekdays\":[],\"start_time\":\"10:00\",\"end_time\":\"18:00\",\"point_space_ids\":[],\"note\":null,\"night_duty_manager\":false}";
        String reason = reasonOf(submit("/api/v1/staff/shifts/batch", body));
        assertThat(reason).contains("2099-01-05 10:00 至 2099-01-05 18:00").contains("2099-01-06 10:00 至 2099-01-06 18:00")
                .doesNotContain("T02:00");
    }

    // 申请修改：主管对本人排的、已发布未开始的班次改时段，原件「原时段 → 新时段」两侧都为北京时间到分钟。修复前原时段是 UTC 原值（2099-01-07T01:00:00Z）。
    @Test
    void editOriginalShowsBeijingMinutesOnBothSides() throws Exception {
        String shiftId = "shf_b108sa_" + s;
        jdbc.update("INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, created_by, created_at, updated_at, project_id) "
                + "VALUES (?, ?, DATE '2099-01-07', TIMESTAMPTZ '2099-01-07 09:00:00+08', TIMESTAMPTZ '2099-01-07 17:00:00+08', ?, now(), now(), ?)",
                shiftId, employee, managerId, project);
        String body = "{\"idempotency_key\":\"b108sa-e-" + s + "\",\"user_id\":\"" + employee + "\",\"shift_id\":\"" + shiftId
                + "\",\"work_date\":\"2099-01-07\",\"start_at\":\"2099-01-07T02:00:00Z\",\"end_at\":\"2099-01-07T10:00:00Z\","
                + "\"point_space_ids\":[],\"note\":\"客流调整晚到一小时\",\"night_duty_manager\":false}";
        String reason = reasonOf(submit("/api/v1/staff/shifts", body));
        assertThat(reason).contains("赵六" + s + "：2099-01-07 09:00 至 2099-01-07 17:00 → 2099-01-07 10:00 至 2099-01-07 18:00")
                .doesNotContain("T01:00").doesNotContain("T02:00");
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 后台排班页单排一班（蛇形键）：该日北京 09:00～17:00，返回复核单编号。 */
    private String single(String date, String points, boolean nightDuty) throws Exception {
        String body = "{\"idempotency_key\":\"b108sa-s-" + (++seq) + "-" + s + "\",\"user_id\":\"" + employee + "\",\"shift_id\":null,\"work_date\":\""
                + date + "\",\"start_at\":\"" + date + "T01:00:00Z\",\"end_at\":\"" + date + "T09:00:00Z\",\"point_space_ids\":" + points
                + ",\"note\":null,\"night_duty_manager\":" + nightDuty + "}";
        return submit("/api/v1/staff/shifts", body);
    }

    private String submit(String url, String body) throws Exception {
        return JsonPath.read(mockMvc.perform(post(url).with(manager).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
    }

    private String reasonOf(String reviewId) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewId).with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.reason");
    }
}
