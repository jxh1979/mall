package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CB 段第 394 条 394-R1（BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」；第十八批裁决 71）：结算配置中心各转正、
 * 下线与审批闸对最高权限本人起草或提交的放行——单行配置草案转正、生效配置下线、通票分账方案转正、短期经营激励审批、付账日规则审批、收入归因规则转正；
 * 普通结算配置管理人员仍不能自审。394-R2（ENUM:164「调整须最高权限审批并留痕」；BSM:576 同句）：最高权限本人提交的高价值判定阈值提交即批准、
 * 不进最高权限复核单，按生效起始日期生效。结算配置页真实请求（蛇形键）与真实授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SettlementConfigMasterSelfApproval394Tests {

    private static final String BASE = "/api/v1/settlement/config";
    private static final String PAY_DAYS = "/api/v1/settlement/internal-pay-days";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;
    @Autowired private SettlementConfigScheduledActivationService activation;

    private final ObjectMapper json = new ObjectMapper();
    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_c394_" + s;
    private final String bar = "bar_c394_" + s;
    private RequestPostProcessor staff;
    private int seq;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '最高权限本人转正综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', 'active', ?, now(), now())", bar, "最高权限本人转正酒吧" + s, project);
        staff = new StaffRequestFixture(jdbc).staff("usr_c394_" + s, "staff", project, null,
                "settlement.config.manage", "settlement.config.view");
    }

    /** ① 最高权限本人起草的渠道费用（通路费）草案，本人复核转正得 200、批准后待生效。修复前 403 SELF_ACTIVATION_FORBIDDEN，断言红。 */
    @Test
    void masterActivatesOwnChannelFeeDraft() throws Exception {
        String draft = routeFeeDraft(master());
        activate("channel_fee", draft, master()).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status || '|' || is_draft FROM channel_fee_configs WHERE id = ?", String.class, draft))
                .isEqualTo("scheduled|false");
    }

    /** ② 普通结算配置管理人员本人起草的草案仍不能本人转正（403 SELF_ACTIVATION_FORBIDDEN）。 */
    @Test
    void nonMasterStillCannotActivateOwnDraft() throws Exception {
        String draft = routeFeeDraft(staff);
        activate("channel_fee", draft, staff).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SELF_ACTIVATION_FORBIDDEN"));
    }

    /** ③ 最高权限本人起草、本人批准、到日生效的卡座提成商户特例，本人下线得 200（下线闸比的是该生效行最后更新人＝批准人）。 */
    @Test
    void masterArchivesOwnCommissionRate() throws Exception {
        LocalDate barTomorrow = businessDays.merchantBusinessDate(project, bar, Instant.now()).plusDays(1);
        postJson(BASE + "/commission-rate", master(), """
                {"idempotency_key": "%s", "project_id": "%s", "merchant_id": "%s", "role_type": "sales", "rate_bp": 2000,
                 "effective_from": "%s"}""".formatted(key(), project, bar, barTomorrow)).andExpect(status().isOk());
        String draft = jdbc.queryForObject("SELECT id FROM bar_commission_rate_configs WHERE project_id = ? AND merchant_id = ?"
                + " AND is_draft = TRUE AND status = 'active'", String.class, project, bar);
        activate("commission_rate", draft, master()).andExpect(status().isOk());
        activation.activateDue(Instant.now().plus(Duration.ofDays(1)));
        assertThat(jdbc.queryForObject("SELECT status || '|' || updated_by FROM bar_commission_rate_configs WHERE id = ?", String.class, draft))
                .isEqualTo("active|" + SessionIssuer.MASTER_USER_ID);

        postJson(BASE + "/commission_rate/" + draft + "/archive", master(), """
                {"idempotency_key": "%s", "archive_reason": "商户合同改签，特例作废"}""".formatted(key())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM bar_commission_rate_configs WHERE id = ?", String.class, draft))
                .isEqualTo("inactive");
    }

    /** ④ 最高权限本人起草的通票分账方案，本人转正得 200、发出通告。 */
    @Test
    void masterActivatesOwnDistributionScheme() throws Exception {
        String version = schemeDraft();
        mockMvc.perform(post(BASE + "/distribution-scheme/" + version + "/activate").with(master()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("notified"));
    }

    /** ⑤ 最高权限本人提交的短期经营激励申请，本人批准得 200。 */
    @Test
    void masterDecidesOwnIncentiveApplication() throws Exception {
        String version = schemeDraft();
        String application = read(postJson(BASE + "/distribution-scheme/" + version + "/incentive-applications", master(), """
                {"idempotency_key": "%s", "reason": "第一段合计高于池金额的短期激励", "term_value": 1, "term_unit": "month"}"""
                .formatted(key())).andExpect(status().isOk())).path("data").path("id").asText();
        postJson(BASE + "/incentive-applications/" + application + "/approve", master(), """
                {"idempotency_key": "%s", "note": "本人批准"}""".formatted(key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));
    }

    /** ⑥ 最高权限本人提交的钱路指派规则，本人审批得 200；普通人提交、本人审批仍 403 SELF_APPROVAL_FORBIDDEN。 */
    @Test
    void masterApprovesOwnPayDayRule() throws Exception {
        InternalPayDayFixtures.seed(jdbc);
        InternalPayDayFixtures.allWorking(jdbc, LocalDate.of(2041, 1, 1), LocalDate.of(2041, 4, 30));
        String rule = read(postJson(PAY_DAYS + "/rules", master(), routeRule()).andExpect(status().isOk())).path("data").path("id").asText();
        postJson(PAY_DAYS + "/rules/" + rule + "/decision", master(), """
                {"idempotency_key": "%s", "decision": "approve"}""".formatted(key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));

        String own = read(postJson(PAY_DAYS + "/rules", staff, routeRule()).andExpect(status().isOk())).path("data").path("id").asText();
        postJson(PAY_DAYS + "/rules/" + own + "/decision", staff, """
                {"idempotency_key": "%s", "decision": "approve"}""".formatted(key()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SELF_APPROVAL_FORBIDDEN"));
    }

    /** ⑦ 最高权限本人在经营参数页落的收入归因规则草案，本人在结算配置中心转正得 200。 */
    @Test
    void masterActivatesOwnAttributionRule() throws Exception {
        String draft = read(postJson("/api/v1/analytics/revenue-attribution-rules", master(), """
                {"idempotency_key": "%s", "id": "mrar_c394_%s", "rule_scope": "default", "revenue_source": "ticket",
                 "attribution_mode": "gross", "refund_attribution": "net_deduct"}""".formatted(key(), s))
                .andExpect(status().isOk())).path("data").path("id").asText();
        assertThat(jdbc.queryForObject("SELECT is_draft FROM merchant_revenue_attribution_rules WHERE id = ?", Boolean.class, draft)).isTrue();
        activate("attribution_rule", draft, master()).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT is_draft FROM merchant_revenue_attribution_rules WHERE id = ?", Boolean.class, draft)).isFalse();
    }

    /**
     * 394-R2：最高权限本人提交高价值判定阈值即批准——不进最高权限复核单，该版待生效（按生效起始日期生效），留痕带 master_self_initiated。
     * 修复前提交即进最高权限复核单（发起人又不能复核自己，只能转给别人审批），断言红。
     */
    @Test
    void masterHighValueThresholdNeedsNoReview() throws Exception {
        LocalDate tomorrow = businessDays.businessDate(null, null, Instant.now()).plusDays(1);
        postJson(BASE + "/high-value-threshold", master(), """
                {"idempotency_key": "%s", "high_value_threshold_cent": 612345, "high_value_threshold_min_cent": 100000,
                 "high_value_threshold_max_cent": 1000000, "reason": "最高权限本人按近一年到货价调整", "effective_from": "%s"}"""
                .formatted(key(), tomorrow)).andExpect(status().isOk());
        String id = jdbc.queryForObject("SELECT id FROM high_value_thresholds WHERE updated_by = ? AND high_value_threshold_cent = 612345",
                String.class, SessionIssuer.MASTER_USER_ID);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE object_id = ?", Integer.class, id))
                .as("最高权限本人提交不进最高权限复核单").isZero();
        assertThat(jdbc.queryForObject("SELECT status || '|' || is_draft FROM high_value_thresholds WHERE id = ?", String.class, id))
                .isEqualTo("scheduled|false");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'settlement.config.draft.activated'"
                + " AND business_id = ? AND metadata->>'master_self_initiated' = 'true'", Integer.class, id)).isEqualTo(1);
    }

    // ---------- helpers ----------

    private String routeFeeDraft(RequestPostProcessor who) throws Exception {
        LocalDate tomorrow = businessDays.businessDate(project, null, Instant.now()).plusDays(1);
        postJson(BASE + "/channel-fee", who, """
                {"idempotency_key": "%s", "project_id": "%s", "fee_layer": "route", "channel": "wechat", "fee_bps": 60,
                 "effective_from": "%s"}""".formatted(key(), project, tomorrow)).andExpect(status().isOk());
        return jdbc.queryForObject("SELECT id FROM channel_fee_configs WHERE project_id = ? AND fee_layer = 'route' AND channel = 'wechat'"
                + " AND is_draft = TRUE AND status = 'active'", String.class, project);
    }

    private String schemeDraft() throws Exception {
        return read(postJson(BASE + "/distribution-scheme", master(), """
                {"idempotency_key": "%s", "project_id": "%s",
                 "tiers": [{"participant_count": 1, "rank_no": 1, "share_bps": 10000, "min_bps": 9000, "max_bps": 10000}],
                 "first_segment": []}""".formatted(key(), project)).andExpect(status().isOk())).path("data").asText();
    }

    /** 付账日页提交钱路指派规则（蛇形键，照 InternalPayDayJdbcIntegrationTests.submitRouteRuleWithFrontendSnakeCaseBody）。 */
    private String routeRule() {
        return """
                {"idempotency_key": "%s", "kind": "route", "route": "ticket_distribution", "statement_day": 1, "pay_slot": 2,
                 "effective_from": "2041-01-01", "reason": "按工作负荷统一制定"}""".formatted(key());
    }

    private ResultActions activate(String configType, String id, RequestPostProcessor who) throws Exception {
        return mockMvc.perform(post(BASE + "/" + configType + "/" + id + "/activate").with(who));
    }

    private ResultActions postJson(String path, RequestPostProcessor who, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String key() {
        return "c394-" + (++seq) + "-" + s;
    }

    private static RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
