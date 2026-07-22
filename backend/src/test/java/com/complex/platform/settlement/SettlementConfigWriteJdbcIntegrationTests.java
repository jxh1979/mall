package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.analytics.AnalyticsRepository;
import com.complex.platform.analytics.OperatingParamsService;
import com.complex.platform.analytics.RevenueAttributionRuleRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ArchiveRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ChannelFeeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ComboDiscountRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ComprehensiveServiceFeeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.DistributionMethodRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.MerchantRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RankTierInput;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RankTiersRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RetentionRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RevenueStreamAttributionRequest;
import com.complex.platform.support.TestSecurityActors;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算配置中心批1 写入面验收(V329):Path 2 草案/生效并存保值、排名区间覆盖守卫、fail-loud finder 收敛、
 * 软删(软替换留档)、差人转正(SELF_ACTIVATION_FORBIDDEN)、归因规则 Path 2。
 *
 * <p>⚠️ 走真实 Flyway 迁移库(complex_test);<b>本类须在 B1-8 临时库串行跑</b>,勿与其它 Flyway 集成测试并发。
 * {@code @Transactional} = 每用例结束自动回滚:含删全表的 fail-loud 用例不污染库/不影响用例次序。本轮仅 test-compile,不执行。
 * <p>差人转正:起草用 {@code DRAFTER}、复核转正用 {@code APPROVER}(二者不同,否则 403)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class SettlementConfigWriteJdbcIntegrationTests {

    private static final String DRAFTER = "drafter_scw";
    private static final String APPROVER = "approver_scw";
    private static final String PROJECT = "proj_scw_it";
    private static final String MERCHANT = "merch_scw_it";

    @Autowired
    private SettlementConfigWriteService writeService;

    @Autowired
    private JdbcSettlementBillRepository billRepository;

    @Autowired
    private RevenueStreamAttributionRepository attributionRepository;

    @Autowired
    private OperatingParamsService operatingParamsService;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        cleanupProjectRows();
    }

    @AfterEach
    void tearDown() {
        cleanupProjectRows();
        TestSecurityActors.clear();
    }

    private void cleanupProjectRows() {
        for (String table : List.of("ticket_distribution_rank_tiers", "platform_retention_rules",
                "ticket_distribution_configs", "ticket_distribution_merchant_rates",
                "comprehensive_service_fee_configs", "ticket_combo_discount_configs",
                "channel_fee_configs", "revenue_stream_attribution")) {
            jdbcTemplate.update("DELETE FROM " + table + " WHERE project_id = ?", PROJECT);
        }
    }

    private String draftRowId(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM " + table + " WHERE project_id = ? AND is_draft = TRUE AND status = 'active' LIMIT 1",
                String.class, PROJECT);
    }

    /** Path 2 保值核心:项目级配置编辑存草案期间,读侧仍读旧生效值;转正后才切换(起草/复核差人)。 */
    @Test
    void retentionDraftDoesNotAffectEffectiveUntilActivated() {
        writeService.saveRetention(new RetentionRequest("k1", PROJECT, 3000), DRAFTER);
        assertThat(billRepository.findRetentionBps(PROJECT)).isEqualTo(2000); // 草案不被读,回退全局默认

        writeService.activateDraft("retention", draftRowId("platform_retention_rules"), APPROVER);
        assertThat(billRepository.findRetentionBps(PROJECT)).isEqualTo(3000);

        writeService.saveRetention(new RetentionRequest("k2", PROJECT, 5000), DRAFTER);
        assertThat(billRepository.findRetentionBps(PROJECT)).isEqualTo(3000); // 保值:草案期读侧仍旧生效

        writeService.activateDraft("retention", draftRowId("platform_retention_rules"), APPROVER);
        assertThat(billRepository.findRetentionBps(PROJECT)).isEqualTo(5000);
    }

    /** 分配方式/渠道费/综合服务费/套票折扣:草案期读侧不变,转正后切换(逐类型证伪 Path 2)。 */
    @Test
    void otherSingleRowTypesDraftThenActivate() {
        writeService.saveDistributionMethod(new DistributionMethodRequest("d1", PROJECT, "by_fixed_amount"), DRAFTER);
        assertThat(billRepository.findDistributionMethod(PROJECT)).isEqualTo("by_rank_tier");
        writeService.activateDraft("distribution_method", draftRowId("ticket_distribution_configs"), APPROVER);
        assertThat(billRepository.findDistributionMethod(PROJECT)).isEqualTo("by_fixed_amount");

        writeService.saveChannelFee(new ChannelFeeRequest("c1", PROJECT, null, 500), DRAFTER);
        assertThat(billRepository.findChannelFeeBps(PROJECT)).isZero();
        writeService.activateDraft("channel_fee", draftRowId("channel_fee_configs"), APPROVER);
        assertThat(billRepository.findChannelFeeBps(PROJECT)).isEqualTo(500);

        writeService.saveComprehensiveServiceFee(
                new ComprehensiveServiceFeeRequest("s1", PROJECT, null, null, 250, null, null), DRAFTER);
        assertThat(billRepository.findComprehensiveServiceFee(PROJECT, null, null).feeBps()).isZero();
        writeService.activateDraft("comprehensive_service_fee",
                draftRowId("comprehensive_service_fee_configs"), APPROVER);
        assertThat(billRepository.findComprehensiveServiceFee(PROJECT, null, null).feeBps()).isEqualTo(250);

        writeService.saveComboDiscount(new ComboDiscountRequest("b1", PROJECT, "merchant_absorb", 0), DRAFTER);
        assertThat(billRepository.findComboDiscountConfig(PROJECT).discountBearer()).isEqualTo("platform_absorb");
        writeService.activateDraft("combo_discount", draftRowId("ticket_combo_discount_configs"), APPROVER);
        assertThat(billRepository.findComboDiscountConfig(PROJECT).discountBearer()).isEqualTo("merchant_absorb");
    }

    /** V336 综合服务费月度封顶写侧往返:写(含封顶)→ 转正 → 读侧 monthlyCapAmountCent 落库(草案期不生效)。 */
    @Test
    void comprehensiveServiceFeeMonthlyCapDraftThenActivate() {
        writeService.saveComprehensiveServiceFee(
                new ComprehensiveServiceFeeRequest("cap1", PROJECT, MERCHANT, "ticket", 1000, null, 5000L), DRAFTER);
        // 草案期读侧仍回退全局默认(无封顶、费 0),保值
        assertThat(billRepository.findComprehensiveServiceFee(PROJECT, MERCHANT, "ticket").monthlyCapAmountCent())
                .isNull();
        writeService.activateDraft("comprehensive_service_fee",
                draftRowId("comprehensive_service_fee_configs"), APPROVER);
        var cfg = billRepository.findComprehensiveServiceFee(PROJECT, MERCHANT, "ticket");
        assertThat(cfg.feeBps()).isEqualTo(1000);
        assertThat(cfg.monthlyCapAmountCent()).isEqualTo(5000L); // 封顶值落库并生效
    }

    /** merchant-rate:草案期 finder 返 null(项目未生效);转正后返配置值。 */
    @Test
    void merchantRateDraftThenActivate() {
        writeService.saveMerchantRate(new MerchantRateRequest("m1", PROJECT, MERCHANT, 500L, null), DRAFTER);
        assertThat(billRepository.findMerchantRate(PROJECT, MERCHANT)).isNull();
        writeService.activateDraft("merchant_rate", draftRowId("ticket_distribution_merchant_rates"), APPROVER);
        assertThat(billRepository.findMerchantRate(PROJECT, MERCHANT).perGuestCent()).isEqualTo(500L);
    }

    /** 排名档位覆盖守卫:Σ≠10000 / 重叠 / 缺口 一律 400;连续无重叠无缺口且 Σ=10000 放行。 */
    @Test
    void rankTiersCoverageGuardRejectsGapOverlapAndBadSum() {
        // Σ=5000≠10000
        assertValidationFailed(() -> writeService.saveRankTiers(
                new RankTiersRequest("r1", PROJECT, List.of(new RankTierInput(1, 1, 5000))), DRAFTER));
        // 重叠:[1,2]@2500 + [2,3]@2500(名次2 被两档覆盖)
        assertValidationFailed(() -> writeService.saveRankTiers(new RankTiersRequest("r2", PROJECT,
                List.of(new RankTierInput(1, 2, 2500), new RankTierInput(2, 3, 2500))), DRAFTER));
        // 缺口:[1,1]@5000 + [3,3]@5000(名次2 无档)
        assertValidationFailed(() -> writeService.saveRankTiers(new RankTiersRequest("r3", PROJECT,
                List.of(new RankTierInput(1, 1, 5000), new RankTierInput(3, 3, 5000))), DRAFTER));
        // 首档不从 1 起
        assertValidationFailed(() -> writeService.saveRankTiers(new RankTiersRequest("r4", PROJECT,
                List.of(new RankTierInput(2, 3, 5000), new RankTierInput(4, 5, 5000))), DRAFTER));

        // 合法:第1名6000 + 2~3名各2000 = 10000
        writeService.saveRankTiers(new RankTiersRequest("r5", PROJECT,
                List.of(new RankTierInput(1, 1, 6000), new RankTierInput(2, 3, 2000))), DRAFTER);
        writeService.activateRankTiers(PROJECT, APPROVER);
        assertThat(billRepository.findRankTiers(PROJECT)).hasSize(2);
    }

    /** 5 个收敛 finder:有种子→值不变(保值);删光该表→抛 SETTLEMENT_CONFIG_MISSING(fail-loud)。 */
    @Test
    void convergedFindersFailLoudWhenSeedMissing() {
        assertThat(billRepository.findRetentionBps(null)).isEqualTo(2000);
        assertThat(billRepository.findChannelFeeBps(null)).isZero();
        assertThat(billRepository.findDistributionMethod(null)).isEqualTo("by_rank_tier");
        assertThat(billRepository.findComprehensiveServiceFee(null, null, null).feeBps()).isZero();
        assertThat(billRepository.findComboDiscountConfig(null).discountBearer()).isEqualTo("platform_absorb");

        jdbcTemplate.update("DELETE FROM platform_retention_rules");
        assertMissing(() -> billRepository.findRetentionBps(null));
        jdbcTemplate.update("DELETE FROM channel_fee_configs");
        assertMissing(() -> billRepository.findChannelFeeBps(null));
        jdbcTemplate.update("DELETE FROM ticket_distribution_configs");
        assertMissing(() -> billRepository.findDistributionMethod(null));
        jdbcTemplate.update("DELETE FROM comprehensive_service_fee_configs");
        assertMissing(() -> billRepository.findComprehensiveServiceFee(null, null, null));
        jdbcTemplate.update("DELETE FROM ticket_combo_discount_configs");
        assertMissing(() -> billRepository.findComboDiscountConfig(null));
    }

    /** 转正软替换留档:旧生效行不是物理删,而是 status→inactive,转正后仍可查回(项6)。 */
    @Test
    void activateSoftReplacesOldEffectiveRowKeepingItInactive() {
        writeService.saveRetention(new RetentionRequest("k1", PROJECT, 3000), DRAFTER);
        writeService.activateDraft("retention", draftRowId("platform_retention_rules"), APPROVER);
        String oldEffectiveId = jdbcTemplate.queryForObject(
                "SELECT id FROM platform_retention_rules WHERE project_id = ? AND retention_bps = 3000 AND status = 'active'",
                String.class, PROJECT);

        writeService.saveRetention(new RetentionRequest("k2", PROJECT, 5000), DRAFTER);
        writeService.activateDraft("retention", draftRowId("platform_retention_rules"), APPROVER);

        // 旧生效行仍在库(未物理删),status=inactive。
        assertThat(statusOf("platform_retention_rules", oldEffectiveId)).isEqualTo("inactive");
        assertThat(billRepository.findRetentionBps(PROJECT)).isEqualTo(5000);
    }

    /** revenue_stream Path 2 保值:编辑草案期间,resolveManagementEntity 仍返旧生效归属;转正后才切换。 */
    @Test
    void revenueStreamPath2PreservesEffectiveUntilActivated() {
        // 建生效归属 ent_A
        writeService.saveRevenueStreamAttribution(
                new RevenueStreamAttributionRequest("r1", PROJECT, "all_pass_ticket", "ent_A"), DRAFTER);
        writeService.activateDraft("revenue_stream_attribution", rsDraftId(), APPROVER);
        assertThat(attributionRepository.resolveManagementEntity(PROJECT, "all_pass_ticket")).isEqualTo("ent_A");

        // 编辑为 ent_B(草案);生效仍 ent_A(保值:Path 2 不动生效行)
        writeService.saveRevenueStreamAttribution(
                new RevenueStreamAttributionRequest("r2", PROJECT, "all_pass_ticket", "ent_B"), DRAFTER);
        assertThat(attributionRepository.resolveManagementEntity(PROJECT, "all_pass_ticket")).isEqualTo("ent_A");

        // 转正 → ent_B;旧生效行软替换 inactive
        writeService.activateDraft("revenue_stream_attribution", rsDraftId(), APPROVER);
        assertThat(attributionRepository.resolveManagementEntity(PROJECT, "all_pass_ticket")).isEqualTo("ent_B");
    }

    /** merchant-rate / revenue-stream 软删 = status→inactive(不物理删)。 */
    @Test
    void softDeleteSetsInactive() {
        writeService.saveMerchantRate(new MerchantRateRequest("m1", PROJECT, MERCHANT, 500L, null), DRAFTER);
        String mrId = draftRowId("ticket_distribution_merchant_rates");
        writeService.activateDraft("merchant_rate", mrId, APPROVER);
        // 差人:下线人须 ≠ 该行上次生效人(activate 后 updated_by=APPROVER),故用 DRAFTER 下线。
        writeService.archive("merchant_rate", mrId, new ArchiveRequest("a1", "口径调整下线"), DRAFTER);
        assertThat(statusOf("ticket_distribution_merchant_rates", mrId)).isEqualTo("inactive");

        writeService.saveRevenueStreamAttribution(
                new RevenueStreamAttributionRequest("rs1", PROJECT, "all_pass_ticket", "ent_scw_it"), DRAFTER);
        String rsId = rsDraftId();
        writeService.activateDraft("revenue_stream_attribution", rsId, APPROVER);
        writeService.archive("revenue_stream_attribution", rsId, new ArchiveRequest("a2", "归属下线"), DRAFTER);
        assertThat(statusOf("revenue_stream_attribution", rsId)).isEqualTo("inactive");
    }

    /** 差人转正(项5):执行人 = 起草人 → 403 SELF_ACTIVATION_FORBIDDEN(覆盖单行/归属/软删/attribution_rule)。 */
    @Test
    void selfActivationForbidden() {
        writeService.saveRetention(new RetentionRequest("k1", PROJECT, 3000), DRAFTER);
        String prrId = draftRowId("platform_retention_rules");
        assertForbidden(() -> writeService.activateDraft("retention", prrId, DRAFTER)); // 同人转正 → 403
        writeService.activateDraft("retention", prrId, APPROVER); // 换人可转

        // 软删同样差人
        writeService.saveMerchantRate(new MerchantRateRequest("m1", PROJECT, MERCHANT, 500L, null), DRAFTER);
        String mrId = draftRowId("ticket_distribution_merchant_rates");
        writeService.activateDraft("merchant_rate", mrId, APPROVER);
        assertForbidden(() -> writeService.archive("merchant_rate", mrId, new ArchiveRequest("a", "x"), APPROVER));
    }

    /** 归因规则(merchant_revenue_attribution_rules)Path 2:落草案(DRAFTER)→ 差人转正(APPROVER)换行生效;同人转正→403。 */
    @Test
    void attributionRulePath2DraftThenActivate() {
        var saved = operatingParamsService.upsertRevenueAttributionRule(
                new RevenueAttributionRuleRequest("k1", "ignored", "default", null, "ticket",
                        "gross", 10000, "net_deduct", "active", true),
                DRAFTER);
        String rowId = saved.id();
        // 草案态:读侧(is_draft=FALSE)仍取种子生效口径(不被草案影响,保值)。
        assertThat(analyticsRepository.findDefaultAttributionRule("ticket").isDraft()).isFalse();

        // 同人转正 → 403
        assertForbidden(() -> writeService.activateDraft("attribution_rule", rowId, DRAFTER));
        // 换人转正 → 生效为 gross(旧种子生效行软替换 inactive)
        writeService.activateDraft("attribution_rule", rowId, APPROVER);
        assertThat(analyticsRepository.findDefaultAttributionRule("ticket").attributionMode()).isEqualTo("gross");
    }

    /** §8:写面拒绝哨兵字面量 projectId → 400。 */
    @Test
    void rejectsSentinelProjectId() {
        assertValidationFailed(() -> writeService.saveRetention(
                new RetentionRequest("k1", "__DEFAULT__", 3000), DRAFTER));
    }

    /** 转正泛化:不存在/非草案 id → 400 DRAFT_NOT_ACTIVATABLE。 */
    @Test
    void activateNonexistentDraftFails() {
        assertThatThrownBy(() -> writeService.activateDraft("retention", "prr_d_nonexistent", APPROVER))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DRAFT_NOT_ACTIVATABLE"));
    }

    /**
     * 必修1(负路·资金级数据完整性):无待转正草案时调排名档转正 → 抛 DRAFT_NOT_ACTIVATABLE,
     * 且生效梯<b>不被破坏性停用</b>(status=active 行数不变)。锁死空草案守卫 + @Transactional + 仓储 EXISTS 三重纵深:
     * 修前该路径会第①步停用生效梯、第②步翻0行抛错但停用已提交 → by_rank_tier 给全体 0 分账。
     */
    @Test
    void activateRankTiersWithoutDraftRejectsAndKeepsActiveTiers() {
        // 先建一套生效梯(草案→转正):第1名6000 + 2~3名各2000 = 10000
        writeService.saveRankTiers(new RankTiersRequest("r1", PROJECT,
                List.of(new RankTierInput(1, 1, 6000), new RankTierInput(2, 3, 2000))), DRAFTER);
        writeService.activateRankTiers(PROJECT, APPROVER);
        int activeBefore = activeRankTierCount(PROJECT);
        assertThat(activeBefore).isEqualTo(2);

        // 无待转正草案时再调转正 → 抛 DRAFT_NOT_ACTIVATABLE(空草案守卫;差人守卫因起草人 null 放行)
        assertThatThrownBy(() -> writeService.activateRankTiers(PROJECT, APPROVER))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DRAFT_NOT_ACTIVATABLE"));
        // 生效梯行数不变 = 未被破坏性停用(资金完整性)
        assertThat(activeRankTierCount(PROJECT)).isEqualTo(activeBefore);
    }

    // ---- helpers ----

    private int activeRankTierCount(String projectId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM ticket_distribution_rank_tiers
                 WHERE is_draft = FALSE AND status = 'active'
                   AND COALESCE(project_id, '__DEFAULT__') = COALESCE(?, '__DEFAULT__')
                """, Integer.class, projectId);
    }

    private String rsDraftId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM revenue_stream_attribution WHERE project_id = ? AND is_draft = TRUE AND status = 'active' LIMIT 1",
                String.class, PROJECT);
    }

    private void assertMissing(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("SETTLEMENT_CONFIG_MISSING"));
    }

    private void assertValidationFailed(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("SELF_ACTIVATION_FORBIDDEN"));
    }

    private String statusOf(String table, String id) {
        return jdbcTemplate.queryForObject("SELECT status FROM " + table + " WHERE id = ?", String.class, id);
    }
}
