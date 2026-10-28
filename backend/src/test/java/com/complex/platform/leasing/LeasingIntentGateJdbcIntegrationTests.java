package com.complex.platform.leasing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 30 条 真库、真接口、真实审批链回归（请求体一律按前端蛇形键）：
 * PGM L502「黑名单品牌拦截同品牌新线索，以及由拉黑前既有线索新建意向。确需继续的，须由具备单独授予的黑名单例外放行权限人员操作，
 * 按层级对应主管审批，申请人不得审批自己提交的申请，并记录操作人、审批人、理由、时间和关联线索或意向。」
 * PGM L502「意向五阶段中进入方案报价、商务谈判须主管确认才能推进」；PGM L500「线索经初步接触、需求确认、方案报价、商务谈判推进到签约」。
 * 线索与品牌经仓储直接落库并带本测试项目（新建线索接口不写项目，项目员工改不了无项目的线索）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class LeasingIntentGateJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcLeasingRepository leasing;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "b24l_prj_" + suffix;
    private RequestPostProcessor mgr;
    private RequestPostProcessor boss;
    private RequestPostProcessor peer;
    private RequestPostProcessor clerk;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','招商回归项目','active',now(),now())", project);
        String full = "b24l_role_" + suffix;
        String plain = "b24l_role_plain_" + suffix;
        role(full, "'leasing.lead.manage','leasing.intent.manage','leasing.brand.manage','leasing.brand.blacklist',"
                + "'leasing.brand.blacklist_exception','governance.supervisor_review.view','governance.supervisor_review.approve'");
        role(plain, "'leasing.lead.manage','leasing.intent.manage'");
        mgr = account("mgr", full);
        boss = account("boss", full);
        peer = account("peer", full);
        clerk = account("clerk", plain);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mgr"), user("boss"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("peer"), user("boss"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** R1＋R2：拉黑前既有线索转意向被拦；持单独授予权限者申请例外、直接上级批准、本人不能批，批准后本人凭单转意向一次，全字段留痕。 */
    @Test
    void preBlacklistLeadIntentNeedsApprovedExceptionUsableOnceByApplicant() throws Exception {
        String brand = brand();
        String lead = lead(brand);
        blacklist(brand);

        intent(mgr, lead, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LEASING_BRAND_BLACKLISTED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leasing_intents WHERE lead_id=?", Integer.class, lead)).isZero();
        assertThat(leadStatus(lead)).isEqualTo("pending_follow_up");

        applyIntentException(clerk, lead).andExpect(status().isForbidden());
        String exceptionId = idOf(applyIntentException(mgr, lead).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"))
                .andExpect(jsonPath("$.data.target_type").value("intent_create"))
                .andExpect(jsonPath("$.data.brand_id").value(brand))
                .andExpect(jsonPath("$.data.lead_id").value(lead))
                .andExpect(jsonPath("$.data.requested_by").value(user("mgr"))));
        intent(mgr, lead, exceptionId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LEASING_BLACKLIST_EXCEPTION_INVALID"));

        String review = exceptionReview(exceptionId);
        approve(mgr, review, "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approve(boss, review, "approved").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM leasing_blacklist_exceptions WHERE id=?", String.class, exceptionId))
                .isEqualTo("approved");

        intent(peer, lead, exceptionId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LEASING_BLACKLIST_EXCEPTION_INVALID"));
        String intentId = idOf(intent(mgr, lead, exceptionId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("initial_contact")));

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT status, requested_by, approved_by, approved_at, used_at, used_lead_id, used_intent_id, reason
                  FROM leasing_blacklist_exceptions WHERE id=?""", exceptionId);
        assertThat(row.get("status")).isEqualTo("used");
        assertThat(row.get("requested_by")).isEqualTo(user("mgr"));
        assertThat(row.get("approved_by")).isEqualTo(user("boss"));
        assertThat(row.get("approved_at")).isNotNull();
        assertThat(row.get("used_at")).isNotNull();
        assertThat(row.get("used_lead_id")).isEqualTo(lead);
        assertThat(row.get("used_intent_id")).isEqualTo(intentId);
        Map<String, Object> audit = jdbc.queryForMap("""
                SELECT actor_id, occurred_at, metadata->>'requested_by' AS requested_by, metadata->>'approved_by' AS approved_by,
                       metadata->>'reason' AS reason, metadata->>'lead_id' AS lead_id, metadata->>'intent_id' AS intent_id
                  FROM audit_events WHERE event_name='leasing.blacklist_exception.used' AND business_id=?""", exceptionId);
        assertThat(audit.get("requested_by")).isEqualTo(user("mgr"));
        assertThat(audit.get("approved_by")).isEqualTo(user("boss"));
        assertThat(audit.get("reason")).isEqualTo("品牌整改完毕，经营方确需继续洽谈");
        assertThat(audit.get("lead_id")).isEqualTo(lead);
        assertThat(audit.get("intent_id")).isEqualTo(intentId);
        assertThat(audit.get("occurred_at")).isNotNull();

        intent(mgr, lead, exceptionId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LEASING_BLACKLIST_EXCEPTION_INVALID"));
    }

    /** R2：驳回的例外放行单不能拿来转意向。 */
    @Test
    void rejectedBlacklistExceptionCannotBeUsed() throws Exception {
        String brand = brand();
        String lead = lead(brand);
        blacklist(brand);
        String exceptionId = idOf(applyIntentException(mgr, lead).andExpect(status().isOk()));

        approve(boss, exceptionReview(exceptionId), "rejected").andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT status FROM leasing_blacklist_exceptions WHERE id=?", String.class, exceptionId))
                .isEqualTo("rejected");
        intent(mgr, lead, exceptionId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LEASING_BLACKLIST_EXCEPTION_INVALID"));
    }

    /** R2：拉黑后建同品牌新线索被拦；例外放行批准后申请人凭单建成，单据记已用与所建线索。 */
    @Test
    void blacklistedBrandNewLeadNeedsApprovedException() throws Exception {
        String brand = brand();
        blacklist(brand);
        createLead(mgr, brand, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LEASING_BRAND_BLACKLISTED"));

        String exceptionId = idOf(send(mgr, "/api/v1/leasing/blacklist-exceptions",
                "{\"target_type\":\"lead_create\",\"brand_id\":\"" + brand + "\",\"reason\":\"品牌整改完毕，经营方确需继续洽谈\","
                        + "\"idempotency_key\":\"lbe_" + UUID.randomUUID() + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.target_type").value("lead_create")));
        approve(boss, exceptionReview(exceptionId), "approved").andExpect(status().isOk());

        String leadId = idOf(createLead(mgr, brand, exceptionId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brand_id").value(brand)));
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, used_lead_id, used_intent_id FROM leasing_blacklist_exceptions WHERE id=?", exceptionId);
        assertThat(row.get("status")).isEqualTo("used");
        assertThat(row.get("used_lead_id")).isEqualTo(leadId);
        assertThat(row.get("used_intent_id")).isNull();
        createLead(mgr, brand, exceptionId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LEASING_BLACKLIST_EXCEPTION_INVALID"));
    }

    /** R3：意向阶段不得向前跳级，被拒后阶段不变。 */
    @Test
    void intentCannotSkipStagesForward() throws Exception {
        String intentId = intentAt("initial_contact");

        advance(mgr, intentId, "pending_signing").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LEASING_INTENT_STAGE_SKIP"));

        assertThat(stage(intentId)).isEqualTo("initial_contact");
    }

    /** R4：进入方案报价须经发起人上级在主管复核里确认；本人不能批，批准后才进入并记确认人；驳回阶段不变。 */
    @Test
    void keyStageWaitsForSupervisorApprovalAndRejectKeepsStage() throws Exception {
        String intentId = intentAt("requirement_confirmed");

        advance(mgr, intentId, "proposal_quoted").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("requirement_confirmed"))
                .andExpect(jsonPath("$.data.pending_stage").value("proposal_quoted"));
        advance(mgr, intentId, "proposal_quoted").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("LEASING_INTENT_STAGE_PENDING_REVIEW"));
        String review = jdbc.queryForObject("SELECT pending_review_id FROM leasing_intents WHERE id=?", String.class, intentId);

        approve(mgr, review, "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approve(boss, review, "approved").andExpect(status().isOk());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT stage, last_stage_change_by, pending_stage, pending_review_id FROM leasing_intents WHERE id=?", intentId);
        assertThat(row.get("stage")).isEqualTo("proposal_quoted");
        assertThat(row.get("last_stage_change_by")).isEqualTo(user("boss"));
        assertThat(row.get("pending_stage")).isNull();
        assertThat(row.get("pending_review_id")).isNull();

        String other = intentAt("requirement_confirmed");
        advance(mgr, other, "proposal_quoted").andExpect(status().isOk());
        approve(boss, jdbc.queryForObject("SELECT pending_review_id FROM leasing_intents WHERE id=?", String.class, other), "rejected")
                .andExpect(status().isOk());
        Map<String, Object> rejected = jdbc.queryForMap(
                "SELECT stage, pending_stage, pending_review_id FROM leasing_intents WHERE id=?", other);
        assertThat(rejected.get("stage")).isEqualTo("requirement_confirmed");
        assertThat(rejected.get("pending_stage")).isNull();
        assertThat(rejected.get("pending_review_id")).isNull();
    }

    private ResultActions intent(RequestPostProcessor who, String lead, String exceptionId) throws Exception {
        return send(who, "/api/v1/leasing/intents", "{\"lead_id\":\"" + lead + "\","
                + (exceptionId == null ? "" : "\"blacklist_exception_id\":\"" + exceptionId + "\",")
                + "\"idempotency_key\":\"li_" + UUID.randomUUID() + "\"}");
    }

    private ResultActions createLead(RequestPostProcessor who, String brand, String exceptionId) throws Exception {
        return send(who, "/api/v1/leasing/leads", "{\"contact_name\":\"王经理\",\"source\":\"outbound\",\"brand_id\":\"" + brand + "\","
                + (exceptionId == null ? "" : "\"blacklist_exception_id\":\"" + exceptionId + "\",")
                + "\"idempotency_key\":\"ll_" + UUID.randomUUID() + "\"}");
    }

    private ResultActions applyIntentException(RequestPostProcessor who, String lead) throws Exception {
        return send(who, "/api/v1/leasing/blacklist-exceptions", "{\"target_type\":\"intent_create\",\"lead_id\":\"" + lead
                + "\",\"reason\":\"品牌整改完毕，经营方确需继续洽谈\",\"idempotency_key\":\"lbe_" + UUID.randomUUID() + "\"}");
    }

    private ResultActions advance(RequestPostProcessor who, String intentId, String target) throws Exception {
        return send(who, "/api/v1/leasing/intents/" + intentId + "/advance",
                "{\"target_stage\":\"" + target + "\",\"idempotency_key\":\"la_" + UUID.randomUUID() + "\"}");
    }

    private ResultActions approve(RequestPostProcessor who, String review, String decision) throws Exception {
        return send(who, "/api/v1/platform/supervisor-review/" + review + "/approve", "{\"decision\":\"" + decision
                + "\",\"comment\":\"已核对\",\"idempotency_key\":\"ap_" + UUID.randomUUID() + "\"}");
    }

    private void blacklist(String brand) throws Exception {
        send(mgr, "/api/v1/leasing/brands/" + brand + "/blacklist",
                "{\"blacklist\":true,\"reason\":\"多次违约\",\"idempotency_key\":\"bl_" + UUID.randomUUID() + "\"}")
                .andExpect(status().isOk());
    }

    private ResultActions send(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String brand() {
        Instant earlier = Instant.now().minus(2, ChronoUnit.HOURS);
        String id = "b24l_brand_" + UUID.randomUUID().toString().substring(0, 8);
        leasing.saveBrand(new LeasingBrandRecord(id, "回归品牌" + id, "prospective", "retail", null, null, null, project,
                earlier, earlier));
        return id;
    }

    private String lead(String brand) {
        Instant earlier = Instant.now().minus(1, ChronoUnit.HOURS);
        String id = "b24l_lead_" + UUID.randomUUID().toString().substring(0, 8);
        leasing.saveLead(new LeasingLeadRecord(id, "李经理", null, "outbound", "retail", null, null, brand, null,
                "pending_follow_up", null, null, project, earlier, earlier));
        return id;
    }

    private String intentAt(String stage) {
        String lead = lead(null);
        Instant now = Instant.now();
        String id = "b24l_int_" + UUID.randomUUID().toString().substring(0, 8);
        leasing.saveIntent(new LeasingIntentRecord(id, lead, stage, null, null, Map.of(), now, null, project, now, now));
        return id;
    }

    private String exceptionReview(String exceptionId) {
        return jdbc.queryForObject("SELECT review_id FROM leasing_blacklist_exceptions WHERE id=?", String.class, exceptionId);
    }

    private String leadStatus(String lead) {
        return jdbc.queryForObject("SELECT status FROM leasing_leads WHERE id=?", String.class, lead);
    }

    private String stage(String intentId) {
        return jdbc.queryForObject("SELECT stage FROM leasing_intents WHERE id=?", String.class, intentId);
    }

    private String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private void role(String role, String permissions) {
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'招商回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(" + permissions + ")", role);
    }

    private RequestPostProcessor account(String tag, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                userId, userId, project);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "b24l_" + tag + "_" + suffix;
    }
}
