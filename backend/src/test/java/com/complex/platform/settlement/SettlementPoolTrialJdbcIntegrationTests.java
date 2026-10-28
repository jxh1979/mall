package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 第 75 条 75-R1：第一段转正前按该票种在当前各项配置下的最低可能可分配池金额逐票种试算，不通过不得转正。
 *
 * <p>依据：ENUM L52「第一段的固定金额或固定单价在配置生效之前，必须先按该票种在当前各项配置下的最低可能可分配池金额试算（即最不利的一张票：
 * 按扣点最高的销售渠道、按费率最高的支付通路计取通路费、按门票综合服务费足额计取、按当前赠送权益预估成本与当前最低实付价组合计算），
 * 确认第一段应分合计不超过该最低池金额的百分之百，试算不通过的配置无效、不得生效」；BSM L752 同义。第一段按每份计（每人每次入场一份）。
 * 请求一律按前端真实请求形状（adminClient 蛇形键）经网关提交；造数见 {@link PoolTrialFixture}。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SettlementPoolTrialJdbcIntegrationTests {

    private static final String BASE = "/api/v1/settlement/config/distribution-scheme";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private PoolTrialFixture fx;
    private RequestPostProcessor drafter;
    private RequestPostProcessor approver;
    private int seq;

    @BeforeEach
    void seed() {
        fx = new PoolTrialFixture(jdbc, suffix);
        drafter = fx.actor("ptr_drafter_" + suffix, "settlement.config.view", "settlement.config.manage");
        approver = fx.actor("ptr_approver_" + suffix, "settlement.config.view", "settlement.config.manage");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 第一段固定金额比最不利一份池多 1 分 → 复核转正 409 POOL_TRIAL_FAILED，版本仍是草案、不挂商户专区通告、不发短信、记一条失败审计；
     * 恰好等于最低池（百分之百）的另一版转正 200。
     */
    @Test
    void activationRejectedWhenFirstSegmentExceedsLowestPossiblePool() throws Exception {
        String a = fx.bar("a");
        fx.singlePass(a);
        String over = draft(null, firstSegment(a, "fixed_amount_cent", PoolTrialFixture.SINGLE_PASS_LOWEST_POOL + 1));

        MvcResult rejected = activate(over, approver);

        assertThat(rejected.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = json.readTree(rejected.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(body.path("detail_code").asText()).isEqualTo("POOL_TRIAL_FAILED");
        assertThat(body.path("message").asText()).contains("试算通票").contains("超出 0.01 元").contains("短期经营激励申请");
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_distribution_scheme_versions WHERE id = ?", String.class, over))
                .as("状态不变").isEqualTo("draft");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_zone_notices WHERE source_id = ?", Integer.class, over))
                .as("不发通告").isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'settlement.config.draft.activate_failed' AND event_result = 'failure'
                   AND business_id = ? AND actor_id = ?""", Integer.class, over, "ptr_approver_" + suffix)).isEqualTo(1);

        String exact = draft(null, firstSegment(a, "fixed_amount_cent", PoolTrialFixture.SINGLE_PASS_LOWEST_POOL));
        assertThat(activate(exact, approver).getResponse().getStatus()).as("不超过百分之百即通过").isEqualTo(200);
    }

    /**
     * 最不利的一张票逐项取值（409 消息里逐票种写明）：最低实付价＝会员价 9000 叠动态 −20% 后被最低价夹到 75.00 元（不是 72.00）；
     * 最不利一份池 44.73 元＝按 60bps（费率最高的支付通路，不是 30bps 的 44.86）、销售渠道费取 meituan 固定额 600（大于比例 450）、
     * 综合服务费 10% 足额 600（不套 1 分的月封顶）、赠送 300。
     */
    @Test
    void lowestPoolTakesHighestRouteHighestSalesFullServiceFeeAndLowestPrice() throws Exception {
        String a = fx.bar("b");
        fx.singlePass(a);
        String id = draft(null, firstSegment(a, "fixed_amount_cent", 5000L));

        MvcResult rejected = activate(id, approver);

        assertThat(rejected.getResponse().getStatus()).isEqualTo(409);
        assertThat(json.readTree(rejected.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("message").asText())
                .contains("最低实付价 75.00 元，最不利一份可分配池 44.73 元，第一段应分合计 50.00 元，超出 5.27 元");
    }

    /**
     * 两人票按份计：单张池 4463 按两份最大余数均分为 2232／2231，最不利的一份 2231；第一段固定单价 1000 与固定金额同计每份——
     * 1000＋1232＝2232 超出 0.01 元不通过（按整张池 4463 比就会误放行），1000＋1231 恰好通过。
     */
    @Test
    void perGuestAndFixedBothCountPerUnitAndMultiPersonUsesUnitPool() throws Exception {
        String a = fx.bar("c");
        String b = fx.bar("d");
        fx.twoPersonPass(a, b);

        MvcResult rejected = activate(draft(null, "[" + row(a, "per_guest_cent", 1000L) + "," + row(b, "fixed_amount_cent", 1232L) + "]"),
                approver);
        assertThat(rejected.getResponse().getStatus()).isEqualTo(409);
        assertThat(json.readTree(rejected.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("message").asText()).contains("最不利一份可分配池 22.31 元，第一段应分合计 22.32 元，超出 0.01 元");

        String ok = draft(null, "[" + row(a, "per_guest_cent", 1000L) + "," + row(b, "fixed_amount_cent", 1231L) + "]");
        assertThat(activate(ok, approver).getResponse().getStatus()).isEqualTo(200);
    }

    /** 第一段的酒吧不在任何已发布通票的入场覆盖里：没有受试票种，转正照常 200。 */
    @Test
    void firstSegmentBarNotCoveredByAnyPublishedPassPasses() throws Exception {
        String lonely = fx.bar("e");
        fx.singlePass(fx.bar("f"));

        assertThat(activate(draft(null, firstSegment(lonely, "fixed_amount_cent", 999_999L)), approver).getResponse().getStatus())
                .isEqualTo(200);
    }

    /**
     * 全平台版：第一段酒吧所在项目已有不晚于本版生效的本项目版——本版对其不生效，该项目票种不受试，转正 200
     * （排除条件按传入的生效营业日比，本版此刻尚未写 effective_at）。
     */
    @Test
    void platformVersionTrialSkipsProjectWithEarlierOwnVersion() throws Exception {
        String a = fx.bar("g");
        fx.singlePass(a);
        jdbc.update("INSERT INTO ticket_distribution_scheme_versions(id,project_id,status,notified_at,effective_at)"
                + " VALUES(?,?,'notified',now(),now() - interval '1 day')", "tdsv_ptr_own_" + suffix, fx.project);

        String global = draft("null", firstSegment(a, "fixed_amount_cent", 999_999L));

        assertThat(activate(global, approver).getResponse().getStatus()).isEqualTo(200);
    }

    /**
     * 18C 核对后操作员裁决补充第 12 条同口径：所在项目的本项目版凭激励转正、激励在本版生效营业日前已到期——那时本项目已改回此前方案，
     * 本版对其适用，该项目票种照常受试，超池即 409。
     */
    @Test
    void platformVersionTrialCoversProjectWhoseOwnVersionIncentiveLapsesFirst() throws Exception {
        String a = fx.bar("h");
        fx.singlePass(a);
        String own = "tdsv_ptr_own2_" + suffix;
        jdbc.update("INSERT INTO ticket_distribution_scheme_versions(id,project_id,status,notified_at,effective_at)"
                + " VALUES(?,?,'notified',now(),now() - interval '1 day')", own, fx.project);
        jdbc.update("""
                INSERT INTO pass_incentive_applications (id, scheme_version_id, project_id, kind, status, reason, term_value, term_unit,
                    trial_result, max_excess_cent, valid_from, valid_until)
                VALUES (?, ?, ?, 'initial', 'approved', '开业季保底', 2, 'day', '[]'::jsonb, 100, now() - interval '1 day',
                        now() + interval '1 day')""", "pia_ptr_" + suffix, own, fx.project);

        MvcResult result = activate(draft("null", firstSegment(a, "fixed_amount_cent", 999_999L)), approver);

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("detail_code").asText()).isEqualTo("POOL_TRIAL_FAILED");
    }

    /**
     * 复核问题：所在项目只有一版凭激励转正的本项目版、激励到期晚于本版生效——激励一到期，新售票就落回本版（裁决第 12 条「没有就用全平台版」），
     * 所以本版对该项目不是永久不适用，该项目票种照常受试，超池即 409（ENUM L52「试算不通过的配置无效、不得生效」）。
     */
    @Test
    void platformVersionTrialCoversProjectWhoseOnlyOwnVersionIsUnderIncentive() throws Exception {
        String a = fx.bar("i");
        fx.singlePass(a);
        incentiveOnlyOwnVersion("tdsv_ptr_own3_" + suffix, "pia_ptr3_" + suffix);

        MvcResult result = activate(draft("null", firstSegment(a, "fixed_amount_cent", PoolTrialFixture.SINGLE_PASS_LOWEST_POOL + 1)),
                approver);

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("detail_code").asText()).isEqualTo("POOL_TRIAL_FAILED");
    }

    /** 同上情形，全平台版试算通过转正：该项目的酒吧也收到本版通告（ENUM L52「须提前向受影响的酒吧商户发出通告」）。 */
    @Test
    void platformVersionNoticeReachesBarsOfProjectWhoseOnlyOwnVersionIsUnderIncentive() throws Exception {
        String a = fx.bar("j");
        fx.singlePass(a);
        incentiveOnlyOwnVersion("tdsv_ptr_own4_" + suffix, "pia_ptr4_" + suffix);
        String global = draft("null", firstSegment(a, "fixed_amount_cent", 1L));

        assertThat(activate(global, approver).getResponse().getStatus()).isEqualTo(200);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_zone_notices WHERE source_id = ? AND merchant_id = ?",
                Integer.class, global, a)).isEqualTo(1);
    }

    // ───────── helpers ─────────

    /** 本项目唯一一版：昨天生效、凭首次激励转正，激励三个月后到期（晚于全平台版此刻转正的生效营业日＝今天＋出厂通告期 1 个月）。 */
    private void incentiveOnlyOwnVersion(String own, String app) {
        jdbc.update("INSERT INTO ticket_distribution_scheme_versions(id,project_id,status,notified_at,effective_at)"
                + " VALUES(?,?,'notified',now(),now() - interval '1 day')", own, fx.project);
        jdbc.update("""
                INSERT INTO pass_incentive_applications (id, scheme_version_id, project_id, kind, status, reason, term_value, term_unit,
                    trial_result, max_excess_cent, valid_from, valid_until)
                VALUES (?, ?, ?, 'initial', 'approved', '开业季保底', 3, 'month', '[]'::jsonb, 100, now() - interval '1 day',
                        now() + interval '3 months')""", app, own, fx.project);
    }

    private String firstSegment(String bar, String field, long cent) {
        return "[" + row(bar, field, cent) + "]";
    }

    private static String row(String bar, String field, long cent) {
        return "{\"merchant_id\":\"" + bar + "\",\"" + field + "\":" + cent + "}";
    }

    /** 落草案（档位表 1 家 100%，上下限同时填）；projectJson 为 null 时取本项目，传字面 "null" 为全平台版。 */
    private String draft(String projectJson, String firstSegmentJson) throws Exception {
        String project = projectJson == null ? "\"" + fx.project + "\"" : projectJson;
        MvcResult result = mockMvc.perform(post(BASE).with(drafter).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"ptr_%s_%d","project_id":%s,
                 "tiers":[{"participant_count":1,"rank_no":1,"share_bps":10000,"min_bps":9000,"max_bps":10000}],
                 "first_segment":%s}
                """.formatted(suffix, ++seq, project, firstSegmentJson))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("data").asText();
    }

    private MvcResult activate(String id, RequestPostProcessor who) throws Exception {
        return mockMvc.perform(post(BASE + "/" + id + "/activate").with(who)).andReturn();
    }
}
