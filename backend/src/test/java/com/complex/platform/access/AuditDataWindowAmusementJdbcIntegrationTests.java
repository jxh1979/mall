package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CP 段第 372 条第 5 段 372-S5-R1～R4（PGM:682「外部审计/监管只能查看划定的范围与时间段」；第十八批裁决 77「各清单、明细、导出只看得到
 * 这段时间发生的记录；没有发生日期的资料照常显示」）：游乐场次、场次结束原件与未履约事项、核销记录、在玩人数释放记录、全园故障状态与其留痕、
 * 全园故障补偿批次与补偿记录，以及控制席补定（自定 82）的当班未抽检记录与设备分级申请历史按外审数据时段承载；游乐项目、状态看板、容量快照、
 * 补偿参数、营业日配置照常显示。真库＋真实接口＋真实角色授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AuditDataWindowAmusementJdbcIntegrationTests {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final String BEFORE = "2025-12-31 23:30:00+08";
    private static final String INSIDE = "2026-06-01 10:00:00+08";
    /** 外审账号与对照账号同一组查看码（setUp 一次给齐，各用例共用）。 */
    private static final String[] CODES = {"amusement.session.view", "amusement.verification.view", "amusement.park_fault.view",
            "amusement.business_day.view", "amusement.safety_inspection.view", "amusement.device.view"};

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String p1 = "adw5_p1_" + s;
    private final String m1 = "adw5_m1_" + s;
    private final String amusement = "adw5_amp_" + s;
    private final String config = "adw5_abd_" + s;
    private RequestPostProcessor auditor;
    private RequestPostProcessor staff;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','外审时段游乐综合体','active',now(),now())", p1);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '外审时段游乐商户', 'amusement', ?, 'active', now(), now())", m1, p1);
        String space = "adw5_spc_" + s;
        jdbc.update("INSERT INTO spaces (id, project_id, merchant_id, space_name, space_type, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, '外审时段游乐空间', 'amusement', 'active', now(), now())", space, p1, m1);
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, merchant_id, space_id, created_at, updated_at) "
                + "VALUES (?, '外审时段游乐项目', 10, 'open', ?, ?, CAST('2025-06-01 10:00:00+08' AS timestamptz), CAST('2025-06-01 10:00:00+08' AS timestamptz))",
                amusement, m1, space);
        jdbc.update("""
                INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,'V0.01','Asia/Shanghai',9,0,23,7,0,23,CAST('2025-05-03 00:00:00+08' AS timestamptz),'approved','外审时段营业日配置',
                  'usr_adw5_requester',CAST('2025-05-01 10:00:00+08' AS timestamptz),'usr_master',CAST('2025-05-02 10:00:00+08' AS timestamptz))
                """, config, p1);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        String auditorId = "usr_adw5_auditor_" + s;
        auditor = staffs.staff(auditorId, "outsourced_vendor", p1, null, CODES);
        staff = staffs.staff("usr_adw5_staff_" + s, "staff", p1, null, CODES);
        mockMvc.perform(put("/api/v1/access/users/" + auditorId + "/audit-data-window").with(master())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"adw5-" + UUID.randomUUID() + "\",\"data_from\":\"2026-01-01\",\"data_to\":\"2026-12-31\"}"))
                .andExpect(status().isOk());
    }

    // ---------- 372-S5-R1 场次、结束原件、未履约事项 ----------

    @Test
    void sessionsEndingsAndFollowupsFollowOwnDatesWhileUnwindowedStaffSeesAll() throws Exception {
        String s1 = session("s1", "2025-12-31", "2025-12-31 20:00:00+08", "2025-12-31 23:30:00+08");
        String s2 = session("s2", "2026-06-01", "2026-06-01 09:00:00+08", "2026-06-01 10:00:00+08");
        String e1 = ending("e1", s1, "2025-12-31", BEFORE);
        String e2 = ending("e2", s2, "2026-06-01", INSIDE);
        String f1 = followup("f1", e1, BEFORE);
        String f2 = followup("f2", e2, INSIDE);

        String sessions = body(auditor, "/api/v1/amusement/sessions");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(JsonPath.<List<String>>read(sessions, "$.data.items[*].id")).containsExactly(s2);
        softly.assertThat(((Number) JsonPath.read(sessions, "$.data.total")).longValue()).isEqualTo(1L);
        softly.assertThat(ids(auditor, "/api/v1/amusement/sessions/" + s1 + "/endings", "$.data[*].id")).isEmpty();
        String endings = body(auditor, "/api/v1/amusement/sessions/" + s2 + "/endings");
        softly.assertThat(JsonPath.<List<String>>read(endings, "$.data[*].id")).containsExactly(e2);
        softly.assertThat(JsonPath.<List<String>>read(endings, "$.data[*].business_date")).containsExactly("2026-06-01");
        softly.assertThat(ids(auditor, "/api/v1/amusement/sessions/" + s1 + "/followups", "$.data[*].id")).isEmpty();
        String followups = body(auditor, "/api/v1/amusement/sessions/" + s2 + "/followups");
        softly.assertThat(JsonPath.<List<String>>read(followups, "$.data[*].id")).containsExactly(f2);
        softly.assertThat(JsonPath.<List<Object>>read(followups, "$.data[*].created_at")).hasSize(1).doesNotContainNull();
        softly.assertAll();

        // 不划定时段的项目员工两头都看得到
        assertThat(ids(staff, "/api/v1/amusement/sessions", "$.data.items[*].id")).containsExactlyInAnyOrder(s1, s2);
        assertThat(ids(staff, "/api/v1/amusement/sessions/" + s1 + "/endings", "$.data[*].id")).containsExactly(e1);
        assertThat(ids(staff, "/api/v1/amusement/sessions/" + s1 + "/followups", "$.data[*].id")).containsExactly(f1);
    }

    // ---------- 372-S5-R2 核销记录、在玩人数释放记录 ----------

    @Test
    void verificationsAndCountReleasesFollowOccurrenceTimeAndIntersectDateParams() throws Exception {
        String sv1 = session("v1", "2025-12-31", "2025-12-31 20:00:00+08", "2025-12-31 22:00:00+08");
        String sv2 = session("v2", "2026-06-01", "2026-06-01 09:00:00+08", "2026-06-01 22:00:00+08");
        String v1 = verification("v1", sv1, "entry", "2025-12-31", null, BEFORE);
        String v2 = verification("v2", sv2, "entry", "2026-06-01", null, INSIDE);
        String v3 = verification("v3", sv2, "exit", null, v2, "2026-06-01 10:30:00+08");
        String r1 = release("r1", sv1, "2025-12-31", "2025-12-31 22:00:00+08", "2025-12-31 22:00:05+08");
        String r2 = release("r2", sv2, "2026-06-01", "2026-06-01 22:00:00+08", "2026-06-01 22:00:05+08");

        String all = body(auditor, "/api/v1/amusement/verifications");
        String december = body(auditor, "/api/v1/amusement/verifications?dateFrom=2025-12-01&dateTo=2025-12-31");
        String spanning = body(auditor, "/api/v1/amusement/verifications?dateFrom=2025-12-01&dateTo=2026-06-30");
        String releases = body(auditor, "/api/v1/amusement/count-releases?projectId=" + amusement);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(JsonPath.<List<String>>read(all, "$.data.items[*].id")).containsExactlyInAnyOrder(v2, v3);
        softly.assertThat(((Number) JsonPath.read(all, "$.data.total")).longValue()).isEqualTo(2L);
        softly.assertThat(JsonPath.<List<String>>read(december, "$.data.items[*].id")).isEmpty();
        softly.assertThat(((Number) JsonPath.read(december, "$.data.total")).longValue()).isEqualTo(0L);
        softly.assertThat(JsonPath.<List<String>>read(spanning, "$.data.items[*].id")).containsExactlyInAnyOrder(v2, v3);
        softly.assertThat(JsonPath.<List<String>>read(releases, "$.data[*].id")).containsExactly(r2);
        softly.assertThat(JsonPath.<List<Object>>read(releases, "$.data[*].reset_at")).hasSize(1).doesNotContainNull();
        softly.assertAll();

        assertThat(ids(staff, "/api/v1/amusement/verifications", "$.data.items[*].id")).containsExactlyInAnyOrder(v1, v2, v3);
        assertThat(ids(staff, "/api/v1/amusement/count-releases?projectId=" + amusement, "$.data[*].id")).containsExactlyInAnyOrder(r1, r2);
    }

    // ---------- 372-S5-R3 全园故障、留痕、补偿批次、补偿记录 ----------

    @Test
    void parkFaultsActionsAndCompensationFollowOwnTimes() throws Exception {
        String a = fault("a", BEFORE, "2026-01-02 10:00:00+08");
        String b = fault("b", INSIDE, "2026-06-02 10:00:00+08");
        faultAction("init", a, "initiate", BEFORE);
        String lift = faultAction("lift", a, "confirm_lift", "2026-01-02 10:00:00+08");
        String k1 = batch("k1", a, "2025-12-31 23:50:00+08");
        String k2 = batch("k2", a, "2026-01-02 11:00:00+08");
        String type = "adw5_tt_" + s;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '外审时段游乐票', 'fixed', 'active', CAST('2025-06-01 10:00:00+08' AS timestamptz), now())", type);
        String order = "adw5_to_" + s;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, issue_status, status,"
                + " created_at, updated_at, merchant_id, bar_id, project_id) VALUES (?, ?, ?, 1, 'web', 8800, 'paid', 'issued', 'created',"
                + " CAST(? AS timestamptz), now(), NULL, NULL, ?)", order, "ADW5-" + order, type, BEFORE, p1);
        String template = "adw5_tpl_" + s;
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, created_at, updated_at)"
                + " VALUES (?, '外审时段游乐入场', 'entry', 1, 0, 'active', now(), now())", template);
        String i1 = item("i1", k1, a, entitlement("1", type, order, template), "2025-12-31 23:50:00+08");
        String i2 = item("i2", k2, a, entitlement("2", type, order, template), "2026-01-02 11:00:00+08");

        String faults = body(auditor, "/api/v1/amusement/park-wide-faults");
        String k1Items = body(auditor, "/api/v1/amusement/compensation-batches/" + k1 + "/items");
        String k2Items = body(auditor, "/api/v1/amusement/compensation-batches/" + k2 + "/items");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(JsonPath.<List<String>>read(faults, "$.data.items[*].id")).containsExactly(b);
        softly.assertThat(((Number) JsonPath.read(faults, "$.data.total")).longValue()).isEqualTo(1L);
        softly.assertThat(ids(auditor, "/api/v1/amusement/park-wide-faults/" + a + "/actions", "$.data.items[*].id")).containsExactly(lift);
        softly.assertThat(ids(auditor, "/api/v1/amusement/park-wide-faults/" + a + "/compensation-batches", "$.data.items[*].id"))
                .containsExactly(k2);
        softly.assertThat(ids(auditor, "/api/v1/amusement/compensation-batches?project_id=" + p1, "$.data.items[*].id")).containsExactly(k2);
        softly.assertThat(JsonPath.<List<String>>read(k1Items, "$.data.items[*].id")).isEmpty();
        softly.assertThat(((Number) JsonPath.read(k1Items, "$.data.total")).longValue()).isEqualTo(0L);
        softly.assertThat(JsonPath.<List<String>>read(k2Items, "$.data.items[*].id")).containsExactly(i2);
        softly.assertThat(((Number) JsonPath.read(k2Items, "$.data.total")).longValue()).isEqualTo(1L);
        softly.assertAll();

        assertThat(ids(staff, "/api/v1/amusement/park-wide-faults", "$.data.items[*].id")).containsExactlyInAnyOrder(a, b);
        assertThat(ids(staff, "/api/v1/amusement/park-wide-faults/" + a + "/actions", "$.data.items[*].id")).hasSize(2);
        assertThat(ids(staff, "/api/v1/amusement/compensation-batches?project_id=" + p1, "$.data.items[*].id")).containsExactlyInAnyOrder(k1, k2);
        assertThat(ids(staff, "/api/v1/amusement/compensation-batches/" + k1 + "/items", "$.data.items[*].id")).containsExactly(i1);
        assertThat(ids(staff, "/api/v1/amusement/compensation-batches/" + k2 + "/items", "$.data.items[*].id")).containsExactly(i2);
    }

    // ---------- 控制席补定（自定 82）：当班未抽检记录、设备分级申请与历史 ----------

    @Test
    void spotCheckMissesAndDeviceClassChangesFollowOwnTimes() throws Exception {
        String outMiss = miss("out", "2025-12-31 21:00:00+08", BEFORE);
        String inMiss = miss("in", "2026-06-01 08:00:00+08", INSIDE);
        String device = "adw5_dev_" + s;
        jdbc.update("INSERT INTO amusement_devices (id, project_id, device_code, status, safety_check_interval_hours, last_pass_inspection_at,"
                + " created_at, updated_at) VALUES (?, ?, ?, 'active', 1, now(), CAST('2025-06-01 10:00:00+08' AS timestamptz), now())",
                device, amusement, "ADW5-" + device);
        String outChange = classChange("out", device, "approved", BEFORE);
        String inChange = classChange("in", device, "rejected", INSIDE);
        String changes = "/api/v1/amusement/projects/" + amusement + "/devices/classification-changes";

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(ids(auditor, "/api/v1/amusement/spot-check-misses", "$.data[*].id")).containsExactly(inMiss);
        softly.assertThat(ids(auditor, changes, "$.data[*].id")).containsExactly(inChange);
        softly.assertAll();

        assertThat(ids(staff, "/api/v1/amusement/spot-check-misses", "$.data[*].id")).containsExactlyInAnyOrder(outMiss, inMiss);
        assertThat(ids(staff, changes, "$.data[*].id")).containsExactlyInAnyOrder(outChange, inChange);
    }

    // ---------- 372-S5-R4 没有发生日期的游乐资料照常显示 ----------

    @Test
    void alwaysVisibleAmusementEntriesStayVisibleForWindowedAuditor() throws Exception {
        String snapshot = "adw5_snap_" + s;
        jdbc.update("INSERT INTO amusement_capacity_snapshot (id, project_id, dimension, capacity_limit, occupied_count, updated_at)"
                + " VALUES (?, ?, 'project', 10, 0, CAST('2025-06-01 10:00:00+08' AS timestamptz))", snapshot, amusement);
        assertThat(ids(auditor, "/api/v1/amusement/projects", "$.data.items[*].id")).contains(amusement);
        assertThat(ids(auditor, "/api/v1/amusement/projects/status-board", "$.data.items[*].project_id")).contains(amusement);
        assertThat(ids(auditor, "/api/v1/amusement/capacity-snapshots", "$.data.items[*].id")).contains(snapshot);
        mockMvc.perform(get("/api/v1/amusement/compensation-policies/" + p1).with(auditor)).andExpect(status().isOk());
        assertThat(ids(auditor, "/api/v1/amusement/business-day-configs?project_id=" + p1, "$.data.versions[*].id")).contains(config);
    }

    // ---------- helpers ----------

    private String body(RequestPostProcessor who, String url) throws Exception {
        return mockMvc.perform(get(url).with(who)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private List<String> ids(RequestPostProcessor who, String url, String path) throws Exception {
        return JsonPath.read(body(who, url), path);
    }

    private String session(String tag, String businessDate, String start, String end) {
        String id = "adw5_ses_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity, status, created_at, updated_at,
                    complex_id, business_day_version_id, business_date)
                VALUES (?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz), 10, 0, 'closed', CAST(? AS timestamptz), now(), ?, ?, CAST(? AS date))
                """, id, amusement, start, end, start, p1, config, businessDate);
        return id;
    }

    /** 自然结束的结束原件（营业日 9 点至次日 7 点）。 */
    private String ending(String tag, String session, String businessDate, String confirmedAt) {
        String id = "adw5_end_" + tag + "_" + s;
        String nextDay = LocalDate.parse(businessDate).plusDays(1).toString();
        jdbc.update("""
                INSERT INTO amusement_session_endings (id, session_id, project_id, complex_id, config_version_id, business_date, business_start_at,
                    business_end_at, mode, planned_end_at, actual_end_at, confirmed_at, reason, idempotency_key, cleared_people)
                VALUES (?, ?, ?, ?, ?, CAST(? AS date), CAST(? AS timestamptz), CAST(? AS timestamptz), 'natural', CAST(? AS timestamptz),
                    CAST(? AS timestamptz), CAST(? AS timestamptz), '外审时段场次结束', ?, 0)
                """, id, session, amusement, p1, config, businessDate, businessDate + " 09:00:00+08", nextDay + " 07:00:00+08",
                confirmedAt, confirmedAt, confirmedAt, "adw5-end-" + tag + "-" + s);
        return id;
    }

    /** 未履约事项与结束原件同一事务、同一时刻生成。 */
    private String followup(String tag, String ending, String createdAt) {
        String id = "adw5_fu_" + tag + "_" + s;
        jdbc.update("INSERT INTO amusement_session_followups (id, ending_id, source_type, source_id, created_at)"
                + " VALUES (?, ?, 'reservation', ?, CAST(? AS timestamptz))", id, ending, "adw5_rsv_" + tag + "_" + s, createdAt);
        return id;
    }

    private String verification(String tag, String session, String direction, String businessDate, String entry, String createdAt) {
        String id = "adw5_av_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO amusement_verifications (id, project_id, session_id, credential_payload, verification_result, verified_people,
                    direction, complex_id, business_date, entry_verification_id, created_at)
                VALUES (?, ?, ?, 'adw5', 'success', 1, ?, ?, CAST(? AS date), ?, CAST(? AS timestamptz))
                """, id, amusement, session, direction, p1, businessDate, entry, createdAt);
        return id;
    }

    /** 场次结束时系统清空在玩人数的一条释放记录。 */
    private String release(String tag, String session, String businessDate, String boundaryAt, String resetAt) {
        String id = "adw5_rel_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO amusement_concurrent_reset_log (id, project_id, scope, business_date, session_id, cleared_rows, cleared_people,
                    boundary_at, reset_at, complex_id)
                VALUES (?, ?, 'session_end', CAST(? AS date), ?, 1, 2, CAST(? AS timestamptz), CAST(? AS timestamptz), ?)
                """, id, amusement, businessDate, session, boundaryAt, resetAt, p1);
        return id;
    }

    /** 已解除的全园故障（发起即建档）。 */
    private String fault(String tag, String initiatedAt, String liftedAt) {
        String id = "adw5_pwf_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO amusement_park_wide_faults (id, project_id, status, reason, initiated_by, initiated_role, initiated_at, lifted_at,
                    created_at, updated_at)
                VALUES (?, ?, 'lifted', '外审时段全部游乐项目不可运行', 'usr_adw5_head', 'amusement_head', CAST(? AS timestamptz),
                    CAST(? AS timestamptz), CAST(? AS timestamptz), CAST(? AS timestamptz))
                """, id, p1, initiatedAt, liftedAt, initiatedAt, liftedAt);
        return id;
    }

    private String faultAction(String tag, String fault, String action, String createdAt) {
        String id = "adw5_pwfa_" + tag + "_" + s;
        jdbc.update("INSERT INTO amusement_park_wide_fault_actions (id, fault_id, project_id, action, actor_id, actor_role, created_at)"
                + " VALUES (?, ?, ?, ?, 'usr_adw5_head', 'amusement_head', CAST(? AS timestamptz))", id, fault, p1, action, createdAt);
        return id;
    }

    private String batch(String tag, String fault, String createdAt) {
        String id = "adw5_cb_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO amusement_fault_compensation_batches (id, fault_id, project_id, status, reason, bearer_company_id, validity_days,
                    window_from, window_to, initiated_by, initiated_role, initiated_at, created_at, updated_at)
                VALUES (?, ?, ?, 'issued', '外审时段全园故障补偿', 'adw5_entity', 365, CAST(? AS timestamptz), CAST(? AS timestamptz),
                    'usr_adw5_head', 'amusement_head', CAST(? AS timestamptz), CAST(? AS timestamptz), CAST(? AS timestamptz))
                """, id, fault, p1, BEFORE, createdAt, createdAt, createdAt, createdAt);
        return id;
    }

    /** 补偿记录与批次同一事务、同一时刻生成。 */
    private String item(String tag, String batch, String fault, String entitlement, String createdAt) {
        String id = "adw5_ci_" + tag + "_" + s;
        jdbc.update("INSERT INTO amusement_fault_compensation_items (id, batch_id, fault_id, project_id, entitlement_id, kind, status, created_at,"
                + " updated_at) VALUES (?, ?, ?, ?, ?, 'full_reissue', 'issued', CAST(? AS timestamptz), CAST(? AS timestamptz))",
                id, batch, fault, p1, entitlement, createdAt, createdAt);
        return id;
    }

    private String entitlement(String tag, String type, String order, String template) {
        String id = "adw5_ie_" + tag + "_" + s;
        jdbc.update("INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code, entitlement_type,"
                + " total_times, used_times, status, valid_from, valid_to, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, 'entry', 1, 0, 'active', CAST(? AS timestamptz), CAST('2027-12-31 00:00:00+08' AS timestamptz),"
                + " CAST(? AS timestamptz), now())", id, type, order, template, "CE-" + id, BEFORE, BEFORE);
        return id;
    }

    private String miss(String tag, String anchorAt, String detectedAt) {
        String id = "adw5_scm_" + tag + "_" + s;
        jdbc.update("INSERT INTO amusement_spot_check_misses (id, project_id, complex_id, anchor_at, interval_minutes, first_admission_at, detected_at)"
                + " VALUES (?, ?, ?, CAST(? AS timestamptz), 120, CAST(? AS timestamptz), CAST(? AS timestamptz))",
                id, amusement, p1, anchorAt, detectedAt, detectedAt);
        return id;
    }

    private String classChange(String tag, String device, String status, String requestedAt) {
        String id = "adw5_dcc_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO amusement_device_class_changes (id, device_id, project_id, to_class, reason, status, requested_by, requested_at,
                    decided_by, decided_at)
                VALUES (?, ?, ?, 'auxiliary', '外审时段设备分级', ?, 'usr_adw5_engineer', CAST(? AS timestamptz), 'usr_adw5_safety',
                    CAST(? AS timestamptz) + interval '1 hour')
                """, id, device, amusement, status, requestedAt, requestedAt);
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
