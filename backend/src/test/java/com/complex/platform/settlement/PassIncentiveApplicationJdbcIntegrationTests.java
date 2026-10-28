package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 75 条 75-R3：短期经营激励申请——配置生效前提交、项目之上的平台权限审批、转正闸认可、有效期与到期前重审。
 *
 * <p>依据：ENUM L52「娱乐平台公司确需让第一段合计高于池金额的，须在配置生效前提交短期经营激励申请并经项目之上的平台权限审批，
 * 超出部分由娱乐平台公司承担，初始有效期不超过后台配置的上限（业务参数，按项目可配，出厂默认三个月，须同时配置上下限）、到期重审」；
 * BSM L752「期限届满须重新提交审批，不得自动续期」；DEC-R3-2026-08-04-13（审批人为项目之上的平台权限）。
 * 草案第一段固定金额 5000 比最不利一份池 4473 多 527（造数见 {@link PoolTrialFixture}）。请求一律按前端真实请求形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PassIncentiveApplicationJdbcIntegrationTests {

    private static final String BASE = "/api/v1/settlement/config";
    private static final DateTimeFormatter CN_DATE = DateTimeFormatter.ofPattern("yyyy年M月d日");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private com.complex.platform.common.BusinessDayResolver businessDays;
    @Autowired private DistributionSchemeService schemes;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private PoolTrialFixture fx;
    private RequestPostProcessor drafter;
    private RequestPostProcessor approver;
    private RequestPostProcessor head;
    private int seq;

    @BeforeEach
    void seed() {
        fx = new PoolTrialFixture(jdbc, suffix);
        drafter = fx.actor("pia_drafter_" + suffix, "settlement.config.view", "settlement.config.manage");
        approver = fx.actor("pia_approver_" + suffix, "settlement.config.view", "settlement.config.manage");
        head = fx.actor("pia_head_" + suffix, "settlement.config.view", "settlement.config.manage", "settlement.bill.special_approve");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 超池草案：转正 409；提交首次申请（待审，最大超出额＝试算超出 527）；平台负责人批准后另一人转正 200，
     * 申请有效期自本版生效营业日起 3 个月（营业日起点）；提交、批准各记一条成功审计；同一幂等键重放返回原申请。
     */
    @Test
    void overPoolDraftNeedsApprovedIncentiveBeforeActivation() throws Exception {
        String version = overPoolDraft();
        assertThat(status(activate(version))).isEqualTo(409);

        JsonNode submitted = data(submit(version, drafter, "idem1", 3, "month"));
        assertThat(submitted.path("status").asText()).isEqualTo("pending");
        assertThat(submitted.path("kind").asText()).isEqualTo("initial");
        assertThat(submitted.path("max_excess_cent").asLong()).isEqualTo(527L);
        assertThat(submitted.path("trial_result").get(0).path("excess_cent").asLong()).isEqualTo(527L);
        String app = submitted.path("id").asText();
        assertThat(data(submit(version, drafter, "idem1", 3, "month")).path("id").asText()).as("幂等重放").isEqualTo(app);

        MvcResult approved = decide(app, "approve", head);
        assertThat(status(approved)).isEqualTo(200);
        assertThat(data(approved).path("status").asText()).isEqualTo("approved");

        assertThat(status(activate(version))).as("有已批准的首次激励即放行").isEqualTo(200);
        Instant effectiveAt = ts("SELECT effective_at FROM ticket_distribution_scheme_versions WHERE id = ?", version);
        assertThat(ts("SELECT valid_from FROM pass_incentive_applications WHERE id = ?", app)).isEqualTo(effectiveAt);
        LocalDate effectiveDate = businessDays.businessDate(fx.project, null, effectiveAt);
        assertThat(ts("SELECT valid_until FROM pass_incentive_applications WHERE id = ?", app))
                .isEqualTo(businessDays.start(fx.project, null, effectiveDate.plusMonths(3)));
        for (String event : new String[] {"settlement.config.pass_incentive.submitted", "settlement.config.pass_incentive.approved"}) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND business_id = ? AND event_result = 'success'",
                    Integer.class, event, app)).as(event).isEqualTo(1);
        }
    }

    /** 审批须项目之上的平台权限：只有结算配置管理权限的 403；只管本项目的人带审批权限码也 403；提交人自批 403 SELF_ACTIVATION_FORBIDDEN。 */
    @Test
    void approvalNeedsPlatformAuthority() throws Exception {
        String version = overPoolDraft();
        String app = data(submit(version, head, "idem2", 1, "month")).path("id").asText();

        assertThat(status(decide(app, "approve", approver))).isEqualTo(403);
        RequestPostProcessor projectStaff = new StaffRequestFixture(jdbc).staff("pia_prj_" + suffix, "staff", fx.project, null,
                "settlement.config.view", "settlement.bill.special_approve");
        assertThat(status(decide(app, "approve", projectStaff))).isEqualTo(403);
        MvcResult self = decide(app, "approve", head);
        assertThat(status(self)).isEqualTo(403);
        assertThat(body(self).path("detail_code").asText()).isEqualTo("SELF_ACTIVATION_FORBIDDEN");
        assertThat(jdbc.queryForObject("SELECT status FROM pass_incentive_applications WHERE id = ?", String.class, app))
                .isEqualTo("pending");
    }

    /** 驳回后转正仍 409 POOL_TRIAL_FAILED。 */
    @Test
    void rejectedIncentiveKeepsActivationBlocked() throws Exception {
        String version = overPoolDraft();
        String app = data(submit(version, drafter, "idem3", 1, "month")).path("id").asText();
        assertThat(status(decide(app, "reject", head))).isEqualTo(200);

        MvcResult result = activate(version);

        assertThat(status(result)).isEqualTo(409);
        assertThat(body(result).path("detail_code").asText()).isEqualTo("POOL_TRIAL_FAILED");
    }

    /** 期限不得超过本项目激励期限上限：项目配 2 个月时申请 3 个月 400（指出 term_value）。 */
    @Test
    void termOverProjectLimitRejected() throws Exception {
        jdbc.update("""
                INSERT INTO ticket_distribution_configs (id, project_id, method_type, special_approval_term_value, special_approval_term_unit,
                    special_approval_term_min_value, special_approval_term_max_value, status, is_draft)
                VALUES (?, ?, 'by_rank_tier', 2, 'month', 1, 6, 'active', FALSE)""", "tdc_pia_" + suffix, fx.project);
        String version = overPoolDraft();

        MvcResult result = submit(version, drafter, "idem4", 3, "month");

        assertThat(status(result)).isEqualTo(400);
        assertThat(body(result).path("detail_code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(body(result).path("message").asText()).contains("term_value");
    }

    /** 到期前重审获批：从上一笔已批准激励的到期日接续一个新期限；没有任何自动续期行。 */
    @Test
    void renewalBeforeExpiryExtendsFromPreviousEnd() throws Exception {
        String version = overPoolDraft();
        String first = data(submit(version, drafter, "idem5", 3, "month")).path("id").asText();
        decide(first, "approve", head);
        assertThat(status(activate(version))).isEqualTo(200);
        Instant previousEnd = ts("SELECT valid_until FROM pass_incentive_applications WHERE id = ?", first);

        JsonNode renewal = data(submit(version, drafter, "idem6", 1, "month"));
        assertThat(renewal.path("kind").asText()).isEqualTo("renewal");
        assertThat(status(decide(renewal.path("id").asText(), "approve", head))).isEqualTo(200);

        String id = renewal.path("id").asText();
        assertThat(ts("SELECT valid_from FROM pass_incentive_applications WHERE id = ?", id)).isEqualTo(previousEnd);
        assertThat(ts("SELECT valid_until FROM pass_incentive_applications WHERE id = ?", id)).isEqualTo(businessDays.start(fx.project,
                null, businessDays.businessDate(fx.project, null, previousEnd).plusMonths(1)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pass_incentive_applications WHERE scheme_version_id = ?", Integer.class,
                version)).as("不得自动续期：只有人工提交的两笔").isEqualTo(2);
    }

    /**
     * 18C 核对后操作员裁决补充第 12 条「到期日和『到期未重新获批就改回此前方案』在转正当天的商户专区通告里一并写明」：
     * 凭激励转正的版本，转正当天挂进受影响酒吧商户专区的那一条通告写明激励到期日与改回说明，不另发通告。
     */
    @Test
    void activationNoticeCarriesIncentiveExpiryDate() throws Exception {
        String version = overPoolDraft();
        String app = data(submit(version, drafter, "idem7", 3, "month")).path("id").asText();
        decide(app, "approve", head);

        assertThat(status(activate(version))).isEqualTo(200);

        LocalDate expiry = businessDays.businessDate(fx.project, null,
                ts("SELECT valid_until FROM pass_incentive_applications WHERE id = ?", app));
        List<String> notices = jdbc.queryForList("SELECT content FROM merchant_zone_notices WHERE source_id = ?", String.class, version);
        assertThat(notices).as("受影响酒吧一家一条，不另发").hasSize(1);
        assertThat(notices.getFirst()).contains("激励到期日为 " + expiry.format(CN_DATE))
                .contains("到期未重新获批的，自该日起新售出的票改按此前在用的方案");
    }

    /**
     * 裁决第 12 条「整版回到此前方案」：激励到期营业日起新售出的票不再取用该版，本项目没有上一版即落到全平台版；到期前的营业日仍取该版；
     * 总览把该版标为 incentive_expired（不再是现行版）。
     */
    @Test
    void expiredIncentiveVersionNoLongerTakesNewSales() throws Exception {
        String version = overPoolDraft();
        String app = data(submit(version, drafter, "idem8", 3, "month")).path("id").asText();
        decide(app, "approve", head);
        assertThat(status(activate(version))).isEqualTo(200);
        LocalDate expiry = businessDays.businessDate(fx.project, null, Instant.now());
        expireOn(version, app, expiry);

        assertThat(schemes.effectiveVersionId(fx.project, businessDays.start(fx.project, null, expiry.minusDays(1))))
                .as("到期日前的营业日仍按本版").isEqualTo(version);
        String fallback = schemes.effectiveVersionId(fx.project, Instant.now());
        assertThat(fallback).as("到期营业日起新售票不再取本版").isNotEqualTo(version);
        assertThat(jdbc.queryForObject("SELECT project_id IS NULL FROM ticket_distribution_scheme_versions WHERE id = ?",
                Boolean.class, fallback)).as("本项目没有上一版，落到全平台版").isTrue();
        String state = null;
        for (JsonNode v : data(mockMvc.perform(get(BASE + "/distribution-scheme").with(drafter)).andReturn()).path("versions")) {
            if (version.equals(v.path("id").asText())) {
                state = v.path("state").asText();
            }
        }
        assertThat(state).isEqualTo("incentive_expired");
    }

    /**
     * 裁决第 12、13 条「到期未重新获批就改回此前方案」：激励到期后不再受理重审（409 INCENTIVE_EXPIRED，中文写明到期日与「起草新版」），
     * 到期前提交、到期后才审批的重审也不批，申请仍待审。
     */
    @Test
    void renewalAfterExpiryIsRejected() throws Exception {
        String version = overPoolDraft();
        String first = data(submit(version, drafter, "idem9", 3, "month")).path("id").asText();
        decide(first, "approve", head);
        assertThat(status(activate(version))).isEqualTo(200);
        String renewal = data(submit(version, drafter, "idem10", 1, "month")).path("id").asText();
        LocalDate expiry = businessDays.businessDate(fx.project, null, Instant.now());
        expireOn(version, first, expiry);

        MvcResult late = decide(renewal, "approve", head);
        assertThat(status(late)).isEqualTo(409);
        assertThat(body(late).path("detail_code").asText()).isEqualTo("INCENTIVE_EXPIRED");
        assertThat(jdbc.queryForObject("SELECT status FROM pass_incentive_applications WHERE id = ?", String.class, renewal))
                .isEqualTo("pending");

        MvcResult again = submit(version, drafter, "idem11", 1, "month");
        assertThat(status(again)).isEqualTo(409);
        assertThat(body(again).path("detail_code").asText()).isEqualTo("INCENTIVE_EXPIRED");
        assertThat(body(again).path("message").asText()).contains("已于 " + expiry.format(CN_DATE) + " 到期").contains("起草新版");
    }

    // ───────── helpers ─────────

    /** 把凭激励转正的版本挪成「三个月前生效、激励到期日＝expiry」（均取营业日起点）。 */
    private void expireOn(String version, String app, LocalDate expiry) {
        Timestamp from = Timestamp.from(businessDays.start(fx.project, null, expiry.minusMonths(3)));
        jdbc.update("UPDATE ticket_distribution_scheme_versions SET effective_at = ? WHERE id = ?", from, version);
        jdbc.update("UPDATE pass_incentive_applications SET valid_from = ?, valid_until = ? WHERE id = ?", from,
                Timestamp.from(businessDays.start(fx.project, null, expiry)), app);
    }

    /** 本项目超池草案：单人单次通票覆盖酒吧，第一段固定金额 5000 > 最不利一份池 4473。 */
    private String overPoolDraft() throws Exception {
        String bar = fx.bar("x" + (++seq));
        fx.singlePass(bar);
        MvcResult result = mockMvc.perform(post(BASE + "/distribution-scheme").with(drafter).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"pia_%s_%d","project_id":"%s",
                 "tiers":[{"participant_count":1,"rank_no":1,"share_bps":10000,"min_bps":9000,"max_bps":10000}],
                 "first_segment":[{"merchant_id":"%s","fixed_amount_cent":5000}]}
                """.formatted(suffix, seq, fx.project, bar))).andReturn();
        assertThat(status(result)).isEqualTo(200);
        return body(result).path("data").asText();
    }

    private MvcResult activate(String version) throws Exception {
        return mockMvc.perform(post(BASE + "/distribution-scheme/" + version + "/activate").with(approver)).andReturn();
    }

    private MvcResult submit(String version, RequestPostProcessor who, String idem, int term, String unit) throws Exception {
        return mockMvc.perform(post(BASE + "/distribution-scheme/" + version + "/incentive-applications").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"%s_%s","reason":"开业季拉新，第一段酒吧保底","term_value":%d,"term_unit":"%s"}
                        """.formatted(idem, suffix, term, unit))).andReturn();
    }

    private MvcResult decide(String app, String action, RequestPostProcessor who) throws Exception {
        return mockMvc.perform(post(BASE + "/incentive-applications/" + app + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"%s_%s_%d","note":"同意，到期前复盘"}
                        """.formatted(action, suffix, ++seq))).andReturn();
    }

    private Instant ts(String sql, String id) {
        return jdbc.queryForObject(sql, Timestamp.class, id).toInstant();
    }

    private static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private JsonNode data(MvcResult result) throws Exception {
        assertThat(status(result)).isEqualTo(200);
        return body(result).path("data");
    }
}
