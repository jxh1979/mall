package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
 * 第十八批 AB 段第 190 条 190-R2：同一会话转台次数达到本酒吧上限后，须主管审批才能继续转台（真库、真接口、真授权链，请求体按前端蛇形键）。
 * 依据：BSM:1079「同一会话的转台次数不得超过后台配置的上限（业务参数，按酒吧可配，出厂默认 10 次，须同时配置上下限），达到上限后须主管审批才能继续转台」；
 * 决策台账 DEC-R6-19「达上限须主管审批」；PGM:528「系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点，逐级审批」。
 * 本酒吧上限配为 1（下限 0、上限 5）；发起人 k 的直接上级是 boss。
 * 不开测试事务：超限申请与复核单在独立事务里提交、转台随后 409 回滚，须真库验得到（数据带随机数字后缀，留在一次性测试库）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class BarTransferLimitApprovalJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String suffix = String.valueOf(Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L));
    private final String project = "prj_tlim_" + suffix;
    private final String merchant = "mch_tlim_" + suffix;
    private final String company = "ent_tlim_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private RequestPostProcessor clerk;
    private RequestPostProcessor boss;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "转台上限项目" + suffix);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())",
                company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "table_transfer_limit,table_transfer_limit_min,table_transfer_limit_max,created_at,updated_at)"
                + " VALUES(?,'转台上限酒吧','bar',?,?,?,?,'active',1,0,5,now(),now())", merchant, company, company, company, project);
        String role = "role_tlim_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'转台上限回归','project','active',now())",
                role, "转台上限现场岗" + suffix);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'bar.table_session.create','bar.table_session.view','bar.table_session.transfer')", role);
        String bossRole = "role_tlim_boss_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'转台上限回归','project','active',now())",
                bossRole, "转台上限审批岗" + suffix);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'governance.supervisor_review.view','governance.supervisor_review.approve')", bossRole);
        clerk = account("k", role);
        account("t", role);
        boss = account("boss", bossRole);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("k"), user("boss"));
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,?,?,'approved','active','employed',false,now(),now())",
                "pro_tlim_t_" + suffix, project, merchant, user("t"), "接手服务员" + suffix, company);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 上限 1：A 转 B 成功；B 转 C 得 409 须审批——会话仍在 B、C 仍空闲，复核单待审（审批人＝发起人直接上级）、申请一行待审批（独立事务已提交）；
     * 再点转台 409 待审批且不新增；直接上级批准后 B 转 C 成功、申请用掉并记上本次转台留痕，快照带已转次数、上限与申请编号；C 转 D 又 409 须审批。
     */
    @Test
    void overLimitTransferNeedsDirectSupervisorApprovalUsedOnce() throws Exception {
        String a = table();
        String b = table();
        String c = table();
        String d = table();
        String session = open(a);
        transfer(session, b).andExpect(status().isOk());

        String review = requiredReview(session, transfer(session, c), 1);
        assertThat(tableOf(session)).isEqualTo(b);
        assertThat(tableStatus(c)).isEqualTo("active");
        assertThat(jdbc.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id = ?", String.class, review))
                .isEqualTo("pending_review");
        assertThat(jdbc.queryForList("SELECT approver_id FROM supervisor_review_nodes WHERE review_id = ? AND line_code = 'main'", String.class, review))
                .containsExactly(user("boss"));
        assertThat(approval(review)).containsEntry("status", "pending").containsEntry("target_table_id", c)
                .containsEntry("transfer_count", 1).containsEntry("limit_count", 1)
                .containsEntry("requested_by", user("k")).containsEntry("reason", "客人换台");

        transfer(session, c)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TRANSFER_LIMIT_APPROVAL_PENDING"))
                .andExpect(jsonPath("$.message").value(containsString("审批单号：" + review)));
        assertThat(approvals(session)).isEqualTo(1);
        assertThat(reviews(session)).isEqualTo(1);

        decide(review, "approved").andExpect(status().isOk());
        assertThat(approval(review)).containsEntry("status", "approved");
        assertThat(approval(review).get("decided_at")).isNotNull();

        transfer(session, c).andExpect(status().isOk());
        assertThat(tableOf(session)).isEqualTo(c);
        Map<String, Object> used = approval(review);
        Map<String, Object> adjustment = jdbc.queryForMap("SELECT id, snapshot_json->>'transfer_count' AS transfer_count,"
                + " snapshot_json->>'transfer_limit' AS transfer_limit, snapshot_json->>'transfer_limit_approval_id' AS approval_id"
                + " FROM bar_table_session_adjustments WHERE session_id = ? AND adjustment_type = 'transfer' AND to_table_id = ?", session, c);
        assertThat(used).containsEntry("status", "used").containsEntry("adjustment_id", adjustment.get("id"));
        assertThat(used.get("used_at")).isNotNull();
        assertThat(adjustment).containsEntry("transfer_count", "1").containsEntry("transfer_limit", "1").containsEntry("approval_id", used.get("id"));

        String next = requiredReview(session, transfer(session, d), 2);
        assertThat(next).isNotEqualTo(review);
        assertThat(tableOf(session)).isEqualTo(c);
        assertThat(tableStatus(d)).isEqualTo("active");
    }

    /** 直接上级驳回：申请记已驳回；再转同一目标台不报待审批，而是新建一张申请（409 须审批，审批单号换新），会话仍在原台。 */
    @Test
    void rejectedRequestLetsNextTransferFileNewRequest() throws Exception {
        String b = table();
        String c = table();
        String session = open(table());
        transfer(session, b).andExpect(status().isOk());
        String review = requiredReview(session, transfer(session, c), 1);

        decide(review, "rejected").andExpect(status().isOk());
        assertThat(approval(review)).containsEntry("status", "rejected");
        assertThat(approval(review).get("decided_at")).isNotNull();

        String again = requiredReview(session, transfer(session, c), 1);
        assertThat(again).isNotEqualTo(review);
        assertThat(approvals(session)).isEqualTo(2);
        assertThat(tableOf(session)).isEqualTo(b);
    }

    /** 批准的是转到 C 的申请，不能用于转到 D：转 D 409 须审批（另建申请），会话仍在 B、D 仍空闲，转 C 的申请仍是已批准未用。 */
    @Test
    void approvalForOneTargetTableCannotBeUsedForAnother() throws Exception {
        String b = table();
        String c = table();
        String d = table();
        String session = open(table());
        transfer(session, b).andExpect(status().isOk());
        String review = requiredReview(session, transfer(session, c), 1);
        decide(review, "approved").andExpect(status().isOk());

        String other = requiredReview(session, transfer(session, d), 1);
        assertThat(other).isNotEqualTo(review);
        assertThat(approval(other)).containsEntry("status", "pending").containsEntry("target_table_id", d);
        assertThat(approval(review)).containsEntry("status", "approved").containsEntry("adjustment_id", null);
        assertThat(tableOf(session)).isEqualTo(b);
        assertThat(tableStatus(d)).isEqualTo("active");
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 409 须审批：提示带已转次数、上限与本会话待审批申请的审批单号，返回该审批单号。 */
    private String requiredReview(String session, ResultActions result, int transferred) throws Exception {
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TRANSFER_LIMIT_APPROVAL_REQUIRED"));
        String review = jdbc.queryForObject("SELECT review_id FROM bar_table_transfer_limit_approvals WHERE session_id = ? AND status = 'pending'",
                String.class, session);
        result.andExpect(jsonPath("$.message").value(allOf(
                containsString("本会话已转台 " + transferred + " 次，达到本酒吧上限 1 次"), containsString("审批单号：" + review))));
        return review;
    }

    private Map<String, Object> approval(String review) {
        return jdbc.queryForMap("SELECT id, status, target_table_id, transfer_count, limit_count, requested_by, reason, adjustment_id, decided_at, used_at"
                + " FROM bar_table_transfer_limit_approvals WHERE review_id = ?", review);
    }

    private int approvals(String session) {
        return jdbc.queryForObject("SELECT count(*) FROM bar_table_transfer_limit_approvals WHERE session_id = ?", Integer.class, session);
    }

    private int reviews(String session) {
        return jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE action_type = 'bar.table_session.transfer_over_limit'"
                + " AND object_id IN (SELECT id FROM bar_table_transfer_limit_approvals WHERE session_id = ?)", Integer.class, session);
    }

    private String tableOf(String session) {
        return jdbc.queryForObject("SELECT table_id FROM bar_table_sessions WHERE id = ?", String.class, session);
    }

    private String tableStatus(String table) {
        return jdbc.queryForObject("SELECT status FROM bar_tables WHERE id = ?", String.class, table);
    }

    /** 发起人的直接上级在后台审批页批准或驳回（审批意见必填）。 */
    private ResultActions decide(String review, String decision) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tlim-decide-" + seq.incrementAndGet() + "-" + suffix);
        body.put("decision", decision);
        body.put("comment", "已核实客人换台");
        body.put("operator_note", "已核实客人换台");
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(boss)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String open(String table) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tlim-open-" + seq.incrementAndGet() + "-" + suffix);
        body.put("bar_id", merchant);
        body.put("table_id", table);
        body.put("guest_count", 2);
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/table-sessions").with(clerk)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk()), "$.data.id");
    }

    /** 后台「卡座与订台」页转台请求（目标台接手人 t 有目标台点位此刻的已发布班次）。 */
    private ResultActions transfer(String session, String targetTable) throws Exception {
        shift("t", jdbc.queryForObject("SELECT space_id FROM bar_tables WHERE id = ?", String.class, targetTable));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tlim-transfer-" + seq.incrementAndGet() + "-" + suffix);
        body.put("target_table_id", targetTable);
        body.put("target_service_staff_id", user("t"));
        body.put("reason", "客人换台");
        body.put("session_version", jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id = ?", Long.class, session));
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/transfer").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 本项目本商户铺位（带面积）上的 4 人卡座，低消 0。 */
    private String table() {
        String space = "spc_tlim_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,area_square_meter,status,created_at,updated_at) "
                + "VALUES(?,?,?,'卡座区','unit',20,'active',now(),now())", space, project, merchant);
        String id = "tbl_tlim_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'4人卡座','card_seat',4,4,0,'CNY','active',now(),now(),?)",
                id, merchant, merchant, "K" + id, space);
        return id;
    }

    /** 已发布班次：覆盖此刻，工作点位为该铺位。 */
    private void shift(String tag, String space) {
        Instant now = Instant.now();
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,created_by,project_id) "
                + "VALUES(?,?,?,?,?,jsonb_build_array(?::text),?,?)", "sft_tlim_" + tag + "_" + suffix + "_" + seq.incrementAndGet(), user(tag),
                LocalDate.now(), Timestamp.from(now.minus(Duration.ofHours(2))), Timestamp.from(now.plus(Duration.ofHours(4))), space, user("k"), project);
    }

    private RequestPostProcessor account(String tag, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "usr_tlim_" + tag + "_" + suffix;
    }
}
