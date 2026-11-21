package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.Supplier;
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
 * 第十八批 BQ 段第 371 条 371-R1（BSM:521「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后
 * 按指定日期生效，不即改即生效。新版本只对生效日之后发生的业务适用」；ENUM:187「全系统的钱账统一按「营业日」归集。」；第十八批裁决 72）：结算配置中心
 * 分配方式、门票综合服务费、赠送权益预估成本、通票凑整参数、渠道费用、卡座提成费率落草案必填生效起始日期（须晚于该范围当前营业日），他人复核批准后待生效、
 * 业务取值不变，到日由系统转正（同范围旧生效行软替换留库，同一生效日后批准的覆盖先批准的）；批准时生效起始日期已到 409 由起草人改期重报。
 * 结算配置页真实请求（蛇形键）与真实授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SettlementConfigEffectiveDateJdbcIntegrationTests {

    private static final String BASE = "/api/v1/settlement/config";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;
    @Autowired private JdbcSettlementBillRepository bills;
    @Autowired private JdbcCommissionPointsConfigRepository commissions;
    @Autowired private SettlementConfigScheduledActivationService activation;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_c371_" + s;
    private final String bar = "bar_c371_" + s;
    private final String drafterId = "usr_c371_a_" + s;
    private RequestPostProcessor drafter;
    private RequestPostProcessor reviewer;
    private int seq;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '生效起始日期综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', 'active', ?, now(), now())", bar, "生效起始日期酒吧" + s, project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        drafter = staffs.staff(drafterId, "staff", project, null, "settlement.config.manage", "settlement.config.view");
        reviewer = staffs.staff("usr_c371_b_" + s, "staff", project, null, "settlement.config.manage", "settlement.config.view");
    }

    /**
     * 卡座提成费率（项目 × 酒吧 × 销售档）：该酒吧营业日起点已批准为与平台默认不同的整点，当天、次日都按该酒吧营业日算。不填生效起始日期、
     * 填当天（按该酒吧营业日）都 400；填次日落草案，待复核队列摘要带「生效起=次日」；他人批准后该版待生效、提成率仍取原值、队列里不再有它；
     * 未到次日作业照跑也不转正；到次日由系统转正，取值为新提成率并落一条按生效日期生效留痕。修复前请求不收日期、批准即生效，断言红。
     */
    @Test
    void commissionRateApprovedVersionWaitsForItsEffectiveDate() throws Exception {
        barBusinessDayApartFromPlatformDefault();
        LocalDate today = businessDays.merchantBusinessDate(project, bar, Instant.now());
        save("commission-rate", commission(2000, null)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        save("commission-rate", commission(2000, today)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        save("commission-rate", commission(2000, today.plusDays(1))).andExpect(status().isOk());
        String draft = draftOf("bar_commission_rate_configs");
        assertThat(pendingSummaries(draft)).singleElement().asString().contains("生效起=" + today.plusDays(1));

        approve("commission_rate", draft).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status || '|' || is_draft FROM bar_commission_rate_configs WHERE id = ?", String.class, draft))
                .isEqualTo("scheduled|false");
        assertThat(commissions.findCommissionRateBp(project, bar, "sales")).isEqualTo(1500);
        assertThat(pendingSummaries(draft)).isEmpty();

        activation.activateDue(Instant.now());
        assertThat(jdbc.queryForObject("SELECT status || '|' || is_draft FROM bar_commission_rate_configs WHERE id = ?", String.class, draft))
                .as("未到生效起始日期，作业不转正").isEqualTo("scheduled|false");
        assertThat(commissions.findCommissionRateBp(project, bar, "sales")).isEqualTo(1500);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'settlement.config.version.effective'"
                + " AND business_id = ?", Integer.class, draft)).isZero();

        activation.activateDue(later());
        assertThat(commissions.findCommissionRateBp(project, bar, "sales")).isEqualTo(2000);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'settlement.config.version.effective'"
                + " AND business_id = ?", Integer.class, draft)).isEqualTo(1);
    }

    /**
     * 项目行（渠道费用·微信通路费）按该综合体营业日：本综合体营业日起点已批准为与平台默认不同的整点。填当天（按该综合体营业日）400；
     * 填次日落草案、他人批准后待生效，未到次日作业照跑也不转正，到次日由系统转正。营业日取成平台默认时断言红。
     */
    @Test
    void projectRowFollowsTheComplexBusinessDay() throws Exception {
        projectBusinessDayApartFromPlatformDefault();
        LocalDate today = businessDays.businessDate(project, null, Instant.now());
        IntFunction<String> routeFee = days -> body("\"project_id\": \"%s\", \"fee_layer\": \"route\", \"channel\": \"wechat\", \"fee_bps\": 70"
                .formatted(project), today.plusDays(days));
        save("channel-fee", routeFee.apply(0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        String draft = approveDated("channel-fee", "channel_fee", "channel_fee_configs", routeFee.apply(1));

        activation.activateDue(Instant.now());
        assertThat(statusOf("channel_fee_configs", draft)).as("未到该综合体营业日次日，作业不转正").isEqualTo("scheduled");
        activation.activateDue(later());
        assertThat(statusOf("channel_fee_configs", draft)).as("到该综合体营业日次日由系统转正").isEqualTo("active");
    }

    /**
     * 同一范围两版都填次日、先后批准，起草人随后又为同一范围存一条后天生效的草案（未批）：到日生效的是后批准的那版，先批准的下线留库，
     * 在审草案不被转正停掉、仍在待复核队列；作业再跑一轮生效行不变。修复前批准即生效，断言红。
     */
    @Test
    void laterApprovalOnTheSameEffectiveDateWinsAndRerunKeepsIt() throws Exception {
        LocalDate tomorrow = businessDays.merchantBusinessDate(project, bar, Instant.now()).plusDays(1);
        String first = approveDated("commission-rate", "commission_rate", "bar_commission_rate_configs", commission(2100, tomorrow));
        String second = approveDated("commission-rate", "commission_rate", "bar_commission_rate_configs", commission(2200, tomorrow));
        save("commission-rate", commission(2400, tomorrow.plusDays(1))).andExpect(status().isOk());
        String pending = draftOf("bar_commission_rate_configs");

        activation.activateDue(later());
        assertThat(commissions.findCommissionRateBp(project, bar, "sales")).isEqualTo(2200);
        assertThat(statusOf("bar_commission_rate_configs", first)).isEqualTo("inactive");
        assertThat(jdbc.queryForObject("SELECT status || '|' || is_draft FROM bar_commission_rate_configs WHERE id = ?", String.class, pending))
                .as("同范围在审草案不被转正停掉").isEqualTo("active|true");
        assertThat(pendingSummaries(pending)).as("在审草案仍在待复核队列").hasSize(1);
        activation.activateDue(later());
        assertThat(statusOf("bar_commission_rate_configs", second)).isEqualTo("active");
        assertThat(commissions.findCommissionRateBp(project, bar, "sales")).isEqualTo(2200);
    }

    /** 批准时生效起始日期已到（按该酒吧营业日）409，草案不动、由起草人改期重报。修复前批准即生效，断言红。 */
    @Test
    void approvalAfterTheEffectiveDateHasArrivedIsRejected() throws Exception {
        LocalDate today = businessDays.merchantBusinessDate(project, bar, Instant.now());
        String draft = "bcrc_c371_d_" + s;
        jdbc.update("INSERT INTO bar_commission_rate_configs (id, project_id, merchant_id, role_type, rate_bp, effective_from, status, is_draft,"
                + " updated_by, updated_at) VALUES (?, ?, ?, 'sales', 2300, ?, 'active', TRUE, ?, now())", draft, project, bar, today, drafterId);
        approve("commission_rate", draft).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SETTLEMENT_CONFIG_EFFECTIVE_DATE_PASSED"));
        assertThat(jdbc.queryForObject("SELECT is_draft FROM bar_commission_rate_configs WHERE id = ?", Boolean.class, draft)).isTrue();
        assertThat(commissions.findCommissionRateBp(project, bar, "sales")).isEqualTo(1500);
    }

    /**
     * 其余五类各两版：次日生效的草案批准后业务取值不变，到日转正后为新值；第二版到日后第一版下线留库。分配方式（短期经营激励有效期上限，项目行）、
     * 门票综合服务费（项目 × 酒吧行）、赠送权益预估成本（项目行）、通票凑整参数（全平台行）、渠道费用（通路费与销售渠道费，项目行）。修复前批准即生效，断言红。
     */
    @Test
    void otherDatedConfigsKeepTheirValueUntilTheEffectiveDate() throws Exception {
        LocalDate projectNext = businessDays.businessDate(project, null, Instant.now()).plusDays(1);
        LocalDate barNext = businessDays.merchantBusinessDate(project, bar, Instant.now()).plusDays(1);
        LocalDate platformNext = businessDays.businessDate(null, null, Instant.now()).plusDays(1);
        twoVersions("distribution-method", "distribution_method", "ticket_distribution_configs",
                v -> body(("\"project_id\": \"%s\", \"method_type\": \"by_rank_tier\", \"special_approval_term_value\": %d,"
                        + " \"special_approval_term_unit\": \"day\", \"special_approval_term_min_value\": 1, \"special_approval_term_max_value\": 90")
                        .formatted(project, v == 1 ? 6 : 9), projectNext),
                () -> bills.findSpecialApprovalTerm(project).label(), "6 天", "9 天");
        twoVersions("comprehensive-service-fee", "comprehensive_service_fee", "comprehensive_service_fee_configs",
                v -> body("\"project_id\": \"%s\", \"merchant_id\": \"%s\", \"fee_bps\": %d".formatted(project, bar, v == 1 ? 300 : 500), barNext),
                () -> bills.findComprehensiveServiceFee(project, bar, null).feeBps(), 300, 500);
        twoVersions("pass-gift-cost", "pass_gift_cost", "pass_gift_cost_configs",
                v -> body("\"project_id\": \"%s\", \"prededuct_mode\": \"by_ticket_amount\", \"ticket_gift_amount_cent\": %d"
                        .formatted(project, v * 100), projectNext),
                () -> bills.findPassGiftCostConfig(project, null).ticketGiftAmountCent(), 100L, 200L);
        twoVersions("combo-discount", "combo_discount", "ticket_combo_discount_configs",
                v -> body("\"rounding_tolerance_cent\": %d, \"rounding_tolerance_min_cent\": 100, \"rounding_tolerance_max_cent\": 800"
                        .formatted(v == 1 ? 300 : 400), platformNext),
                () -> bills.findComboDiscountConfig().roundingToleranceCent(), 300L, 400L);
        twoVersions("channel-fee", "channel_fee", "channel_fee_configs",
                v -> body("\"project_id\": \"%s\", \"fee_layer\": \"route\", \"channel\": \"wechat\", \"fee_bps\": %d"
                        .formatted(project, v == 1 ? 60 : 80), projectNext),
                () -> bills.findRouteFee(project, "wechat").feeBps(), 60, 80);
        twoVersions("channel-fee", "channel_fee", "channel_fee_configs",
                v -> body("\"project_id\": \"%s\", \"fee_layer\": \"sales\", \"channel\": \"meituan\", \"fee_bps\": %d"
                        .formatted(project, v == 1 ? 500 : 700), projectNext),
                () -> bills.findChannelFee(project, "meituan").feeBps(), 500, 700);
    }

    /**
     * 第十八批 BQ 段第 360 条 360-R1（第十八批裁决 72「渠道费配置须按版本保存（新版本、生效日、旧版保留），每单记录所用版本号」）：
     * 美团销售渠道费已有第 1 版 800（同项目抖音另有第 5 版，不同范围不串号）；新版 1200 复核批准即取第 2 版、到日前仍取 800；到日后第 1 版下线留库、取 1200；
     * 同一生效日再批两版 1300、1400 依次取第 3、4 版，到日生效的是后批准的 1400。修复前批准不取号（仍为默认 1），断言红。
     */
    @Test
    void channelFeeVersionsAreNumberedPerScopeAndOldVersionsAreKept() throws Exception {
        String v1 = "cfc_c360_v1_" + s;
        jdbc.update("INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft)"
                + " VALUES (?, ?, 'sales', 'meituan', 800, 'active', FALSE)", v1, project);
        jdbc.update("INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft, version_no)"
                + " VALUES (?, ?, 'sales', 'douyin', 900, 'active', FALSE, 5)", "cfc_c360_dy_" + s, project);
        LocalDate next = businessDays.businessDate(project, null, Instant.now()).plusDays(1);
        IntFunction<String> meituan = bps -> body("\"project_id\": \"%s\", \"fee_layer\": \"sales\", \"channel\": \"meituan\", \"fee_bps\": %d"
                .formatted(project, bps), next);

        String v2 = approveDated("channel-fee", "channel_fee", "channel_fee_configs", meituan.apply(1200));
        assertThat(versionOf(v2)).as("批准即取同范围下一版本号").isEqualTo(2);
        assertThat(bills.findChannelFee(project, "meituan").feeBps()).as("到日前仍取第 1 版").isEqualTo(800);

        activation.activateDue(later());
        assertThat(statusOf("channel_fee_configs", v1)).as("第 1 版下线留库").isEqualTo("inactive");
        assertThat(bills.findChannelFee(project, "meituan").feeBps()).isEqualTo(1200);

        String v3 = approveDated("channel-fee", "channel_fee", "channel_fee_configs", meituan.apply(1300));
        String v4 = approveDated("channel-fee", "channel_fee", "channel_fee_configs", meituan.apply(1400));
        assertThat(List.of(versionOf(v3), versionOf(v4))).isEqualTo(List.of(3, 4));
        activation.activateDue(later());
        assertThat(statusOf("channel_fee_configs", v3)).isEqualTo("inactive");
        assertThat(statusOf("channel_fee_configs", v4)).isEqualTo("active");
        assertThat(bills.findChannelFee(project, "meituan").feeBps()).isEqualTo(1400);
    }

    // ---------- helpers ----------

    /** 两版：每版起草（次日生效）→ 他人批准 → 取值不变 → 到日转正 → 取值为该版；第二版到日后第一版下线留库。 */
    private void twoVersions(String endpoint, String configType, String table, IntFunction<String> body, Supplier<Object> read,
                             Object first, Object second) throws Exception {
        Object before = read.get();
        String v1 = approveDated(endpoint, configType, table, body.apply(1));
        assertThat(read.get()).as(configType + " 批准后到日前取值不变").isEqualTo(before);
        activation.activateDue(later());
        assertThat(read.get()).as(configType + " 到日转正").isEqualTo(first);
        approveDated(endpoint, configType, table, body.apply(2));
        assertThat(read.get()).as(configType + " 第二版到日前取值不变").isEqualTo(first);
        activation.activateDue(later());
        assertThat(read.get()).as(configType + " 第二版到日转正").isEqualTo(second);
        assertThat(statusOf(table, v1)).as(configType + " 第一版下线留库").isEqualTo("inactive");
    }

    /** 起草人落草案、复核人批准，断言该版待生效，返回其行 id。 */
    private String approveDated(String endpoint, String configType, String table, String json) throws Exception {
        save(endpoint, json).andExpect(status().isOk());
        String draft = draftOf(table);
        approve(configType, draft).andExpect(status().isOk());
        assertThat(statusOf(table, draft)).as(configType + " 批准后待生效").isEqualTo("scheduled");
        return draft;
    }

    /**
     * 本酒吧插一条已批准、已生效的营业日版本，起点与平台默认（出厂 10:00）错开，使该酒吧当前营业日与平台默认营业日不同：北京 10 点前起点取 0 点，
     * 10 点起取下一整点（23 点后取不到更晚的整点起点，跳过）。生效时点放在前一天，插法照 TicketDefaultVisitDateJdbcIntegrationTests。
     */
    private void barBusinessDayApartFromPlatformDefault() {
        ZonedDateTime local = Instant.now().atZone(LocalZone.ID);
        assumeTrue(local.getHour() < 23, "北京 23 点后没有更晚的整点起点可配");
        Instant effective = Instant.now().minusSeconds(86_400);
        jdbc.update("""
                INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,?,'V0.01',?,0,23,7,0,23,?,'approved','测试已批准版本','c371_requester',?,'usr_master',?)
                """, "bbd_c371_" + s, bar, project, local.getHour() < 10 ? 0 : local.getHour() + 1, Timestamp.from(effective),
                Timestamp.from(effective.minusSeconds(7200)), Timestamp.from(effective.minusSeconds(3600)));
        assertThat(businessDays.merchantBusinessDate(project, bar, Instant.now())).as("酒吧营业日与平台默认营业日不同")
                .isNotEqualTo(businessDays.businessDate(null, null, Instant.now()));
    }

    /**
     * 本综合体插一条已批准、已生效的营业日版本，起点与平台默认（出厂 10:00）错开，使该综合体当前营业日与平台默认营业日不同；起点取法同
     * {@link #barBusinessDayApartFromPlatformDefault}，插法照 ParkingCreditJdbcIntegrationTests。
     */
    private void projectBusinessDayApartFromPlatformDefault() {
        ZonedDateTime local = Instant.now().atZone(LocalZone.ID);
        assumeTrue(local.getHour() < 23, "北京 23 点后没有更晚的整点起点可配");
        Instant effective = Instant.now().minusSeconds(86_400);
        jdbc.update("""
                INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,'V0.01',?,?,0,23,7,0,23,?,'approved','测试已批准版本','c371_requester',?,'usr_master',?)
                """, "abd_c371_" + s, project, LocalZone.ID.getId(), local.getHour() < 10 ? 0 : local.getHour() + 1,
                Timestamp.from(effective), Timestamp.from(effective.minusSeconds(7200)), Timestamp.from(effective.minusSeconds(3600)));
        assertThat(businessDays.businessDate(project, null, Instant.now())).as("综合体营业日与平台默认营业日不同")
                .isNotEqualTo(businessDays.businessDate(null, null, Instant.now()));
    }

    private String commission(int rateBp, LocalDate effectiveFrom) {
        return body("\"project_id\": \"%s\", \"merchant_id\": \"%s\", \"role_type\": \"sales\", \"rate_bp\": %d"
                .formatted(project, bar, rateBp), effectiveFrom);
    }

    /** 结算配置页「改」→ 保存草案的请求体（蛇形键）；生效起始日期为空则不带该键。 */
    private String body(String fields, LocalDate effectiveFrom) {
        return "{\"idempotency_key\": \"c371-%d-%s\", %s%s}".formatted(++seq, s, fields,
                effectiveFrom == null ? "" : ", \"effective_from\": \"" + effectiveFrom + "\"");
    }

    private ResultActions save(String endpoint, String json) throws Exception {
        return mockMvc.perform(post(BASE + "/" + endpoint).with(drafter).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions approve(String configType, String id) throws Exception {
        return mockMvc.perform(post(BASE + "/" + configType + "/" + id + "/activate").with(reviewer));
    }

    private String draftOf(String table) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE updated_by = ? AND is_draft = TRUE AND status = 'active'",
                String.class, drafterId);
    }

    private List<String> pendingSummaries(String draft) throws Exception {
        String overview = mockMvc.perform(get(BASE + "/overview").with(reviewer)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(overview, "$.data.pending_drafts[?(@.id == '" + draft + "')].summary");
    }

    private int versionOf(String channelFeeId) {
        return jdbc.queryForObject("SELECT version_no FROM channel_fee_configs WHERE id = ?", Integer.class, channelFeeId);
    }

    private String statusOf(String table, String id) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id = ?", String.class, id);
    }

    private static Instant later() {
        return Instant.now().plus(Duration.ofDays(1));
    }
}
