package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 CF 段第 404 条 404-R1（BSM:1300「巡检必须按点位顺序完成……确需补录的一律须附证据（照片／录音／录像等）并经主管复核后方生效」；
 * 决策台账 DEC-R6-09「扫码缺失可事后人工补录……补录记录与原始扫码记录并列展示、互不覆盖」；BSM:1642「补录记录与原始记录并列展示，标明“人工补录”」）：
 * 游乐巡检点位缺扫码走通用补录主链——待复核时不放行下一个点位，主管复核通过才落走点行（带补录单编号），点位视图并列返回本轮补录单。
 * 真库＋真实 HTTP＋真实授权链，请求体蛇形键；复核走真实组织层级（巡检员 → 主管 → 经理），夹具同 TraceHandoverBackfillJdbcTests。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementInspectionPointBackfillJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private PlatformTransactionManager transactions;

    private final String digits = String.valueOf(System.nanoTime());
    private final String project = "prj_a404_" + digits;
    private final String merchant = "mch_a404_" + digits;
    private final String amusement = "amu_a404_" + digits;
    private RequestPostProcessor clerk;
    private RequestPostProcessor mid;
    private int keys;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                project, "游乐补录综合体" + digits);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,'游乐补录商户','amusement',?,'active',now(),now())", merchant, project);
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, eligibility_rule_json, merchant_id,"
                + " created_at, updated_at) VALUES (?, '过山车', 10, 'open', '{}'::jsonb, ?, now(), now())", amusement, merchant);
        String role = "role_a404_" + digits;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'游乐巡检补录回归','project','active',now())",
                role, "游乐巡检补录回归角色" + digits);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'amusement.safety_inspection.create','amusement.safety_inspection.view','amusement.safety_template.manage',"
                + "'evidence.attachment.create','governance.supervisor_review.view','governance.supervisor_review.approve')", role);
        clerk = account("clerk", role);
        mid = account("mid", role);
        account("boss", role);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("clerk"), user("mid"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mid"), user("boss"));
    }

    /**
     * 走完第 1 个点位、第 2 个点位缺扫码：按工作手机蛇形键补录 → 待复核，点位视图并列一条待复核补录、走点仍只有第 1 个，扫第 3 个 409；
     * 主管批准 → 第 2 个点位落走点行（记录人＝责任人、带补录单编号），第 3 个照常扫，建成的巡检第 2 项带补录单编号。
     */
    @Test
    void missedPointBackfillCountsOnlyAfterSupervisorApproval() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠", "出口通道");
        check(1, codes.get(0), "c1").andExpect(status().isOk());
        String submitted = backfill(clerk, 2, "b2", Instant.now(), user("clerk"), "pass", List.of(upload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.target_type").value("amusement_inspection_point"))
                .andExpect(jsonPath("$.data.target_id").value(amusement))
                .andExpect(jsonPath("$.data.target_part").value("2"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String backfillId = JsonPath.read(submitted, "$.data.id");
        String review = JsonPath.read(submitted, "$.data.review_id");
        assertThat(jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id=?", String.class, review))
                .isEqualTo("amusement.inspection.point_backfill");

        String pending = view();
        assertThat(JsonPath.<List<Object>>read(pending, "$.data.backfills")).hasSize(1);
        assertThat(read(pending, "$.data.backfills[0].status")).isEqualTo("pending");
        assertThat(read(pending, "$.data.backfills[0].target_part")).isEqualTo("2");
        assertThat(JsonPath.<List<Integer>>read(pending, "$.data.walk[*].seq")).containsExactly(1);
        check(3, codes.get(2), "c3-early")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_PREVIOUS_POINT_INCOMPLETE"));

        approve(mid, review).andExpect(status().isOk());

        String approved = view();
        assertThat(JsonPath.<List<Integer>>read(approved, "$.data.walk[*].seq")).containsExactly(1, 2);
        assertThat(read(approved, "$.data.walk[1].backfill_id")).isEqualTo(backfillId);
        assertThat(read(approved, "$.data.walk[1].checked_by")).isEqualTo(user("clerk"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_inspection_walk_points WHERE project_id=? AND backfill_id IS NULL",
                Integer.class, amusement)).isEqualTo(1);
        assertThat(read(approved, "$.data.backfills[0].status")).isEqualTo("approved");
        check(3, codes.get(2), "c3").andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/amusement/safety-inspections").with(clerk).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", key("create"), "project_id", amusement,
                                "overall_result", "pass", "evidence_attachment_ids", List.of(upload("amusement_safety_inspection"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[1].backfill_id").value(backfillId));
    }

    /**
     * 上一轮残留：第 1、2 个点位的走点行与一张补录单都在本轮起点之前；本轮直接补录第 1 个点位（责任人为经理、结果不通过）得待复核（不报已记录），
     * 批准后走点只有第 1 个且带补录单编号、记录人为责任人、结果为补录结果，点位视图的补录单不含上一轮提交的那张。
     */
    @Test
    void previousRoundLeftoversDoNotCountForBackfill() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠", "出口通道");
        String old = JsonPath.read(backfill(clerk, 1, "old", Instant.now(), user("clerk"), "pass", List.of(upload()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        check(1, codes.get(0), "s1").andExpect(status().isOk());
        check(2, codes.get(1), "s2").andExpect(status().isOk());
        jdbc.update("UPDATE amusement_inspection_walk_points SET checked_at = now() - interval '2 days' WHERE project_id = ?", amusement);
        jdbc.update("UPDATE manual_backfills SET submitted_at = now() - interval '2 days' WHERE id = ?", old);

        String submitted = backfill(clerk, 1, "new", Instant.now(), user("boss"), "fail", List.of(upload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String fresh = JsonPath.read(submitted, "$.data.id");
        approve(mid, JsonPath.read(submitted, "$.data.review_id")).andExpect(status().isOk());

        String approved = view();
        assertThat(JsonPath.<List<Integer>>read(approved, "$.data.walk[*].seq")).containsExactly(1);
        assertThat(read(approved, "$.data.walk[0].backfill_id")).isEqualTo(fresh);
        assertThat(read(approved, "$.data.walk[0].checked_by")).isEqualTo(user("boss"));
        assertThat(read(approved, "$.data.walk[0].result")).isEqualTo("fail");
        assertThat(JsonPath.<List<String>>read(approved, "$.data.backfills[*].id")).containsExactly(fresh);
    }

    /** 不附证据 400；原时点早于本轮起点 400；已扫的点位再补 409；上一个点位本轮没记 409；责任人本人复核 403。 */
    @Test
    void backfillIsRefusedWithoutEvidenceOrOutOfOrderAndResponsiblePersonCannotReview() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠", "出口通道");
        check(1, codes.get(0), "n1").andExpect(status().isOk());
        backfill(clerk, 2, "no-evidence", Instant.now(), user("clerk"), "pass", List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_EVIDENCE_REQUIRED"));
        String photo = upload();
        backfill(clerk, 2, "early", Instant.now().minus(Duration.ofDays(2)), user("clerk"), "pass", List.of(photo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        check(2, codes.get(1), "n2").andExpect(status().isOk());
        backfill(clerk, 2, "checked", Instant.now(), user("clerk"), "pass", List.of(photo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_POINT_ALREADY_CHECKED"));

        replace(List.of("入口闸机", "安全压杠", "出口通道")).andExpect(status().isOk());
        backfill(clerk, 2, "skip", Instant.now(), user("clerk"), "pass", List.of(photo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_PREVIOUS_POINT_INCOMPLETE"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM manual_backfills WHERE target_id=?", Integer.class, amusement)).isZero();

        String submitted = backfill(clerk, 1, "self", Instant.now(), user("mid"), "fail", List.of(photo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        approve(mid, JsonPath.read(submitted, "$.data.review_id"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
    }

    /** 提交后、批准前该点位已被正常扫码：批准 409、整笔批准回滚，补录单仍待复核。 */
    @Test
    void approvalIsRefusedWhenThePointWasScannedMeanwhile() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠");
        String submitted = backfill(clerk, 1, "race", Instant.now(), user("clerk"), "pass", List.of(upload()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String review = JsonPath.read(submitted, "$.data.review_id");
        check(1, codes.get(0), "race-scan").andExpect(status().isOk());

        approveInSavepoint(review).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_TARGET_CHANGED"));
        assertThat(jdbc.queryForObject("SELECT status FROM manual_backfills WHERE review_id=?", String.class, review)).isEqualTo("pending");
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String base() {
        return "/api/v1/amusement/projects/" + amusement + "/inspection-points";
    }

    private String key(String tag) {
        return "a404-" + tag + "-" + digits + "-" + (keys++);
    }

    private ResultActions replace(List<String> points) throws Exception {
        return send(clerk, put(base()), Map.of("idempotency_key", key("points"), "points", points));
    }

    private List<String> configure(String... names) throws Exception {
        String body = replace(List.of(names)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new ArrayList<>(JsonPath.<List<String>>read(body, "$.data.points[*].point_code"));
    }

    private ResultActions check(int seq, String code, String tag) throws Exception {
        return send(clerk, post(base() + "/" + seq + "/check"), Map.of("idempotency_key", key("check-" + tag), "point_code", code,
                "result", "pass"));
    }

    /** 工作手机补录屏实发形状：原时点、原因、责任人、证据、点位结果。 */
    private ResultActions backfill(RequestPostProcessor who, int seq, String tag, Instant occurredAt, String responsible, String result,
            List<String> evidence) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("idempotency_key", key("backfill-" + tag));
        body.put("occurred_at", occurredAt.toString());
        body.put("reason", "点位码被雨水泡烂，扫不出来");
        body.put("responsible_staff_id", responsible);
        body.put("evidence_attachment_ids", evidence);
        body.put("result", result);
        return send(who, post(base() + "/" + seq + "/backfill"), body);
    }

    private String view() throws Exception {
        return mockMvc.perform(get(base()).with(clerk)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String read(String body, String path) {
        return JsonPath.read(body, path);
    }

    private String upload() throws Exception {
        return upload("manual_backfill");
    }

    private String upload(String businessType) throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "point.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .param("business_type", businessType).param("attachment_role", "photo").with(clerk))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.id");
    }

    private ResultActions approve(RequestPostProcessor who, String review) throws Exception {
        return send(who, post("/api/v1/platform/supervisor-review/" + review + "/approve"),
                Map.of("decision", "approved", "comment", "已核对照片", "idempotency_key", key("approve")));
    }

    /** 批准放在保存点里：批准事务抛错被标为只回滚时回到保存点，等同生产中整笔批准回滚。 */
    private ResultActions approveInSavepoint(String review) {
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        ResultActions[] result = new ResultActions[1];
        try {
            nested.executeWithoutResult(tx -> {
                try {
                    result[0] = approve(mid, review);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (UnexpectedRollbackException rolledBack) {
            // 预期：批准失败把事务标为只回滚，保存点已回滚
        }
        return result[0];
    }

    private ResultActions send(RequestPostProcessor who, MockHttpServletRequestBuilder request, Object body) throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
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
        return "usr_a404_" + tag + "_" + digits;
    }
}
