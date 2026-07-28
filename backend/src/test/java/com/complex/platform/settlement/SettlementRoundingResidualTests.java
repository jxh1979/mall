package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * R-7「四舍五入严格落地 + 尾差单列留痕」验收（2026-07-22 用户拍板 D15.1 / 治理地图 HTML
 * 「全局原则 · 金额一律精确到分」）。动资金引擎，TDD + 守恒。
 *
 * <p>三条核心断言：
 * <ol>
 *   <li><b>四舍五入而非整除截断</b>：按排名档位分池时各家金额 = round_half_up(池 × share_bps / 10000)；
 *       旧 {@code pool * shareBps / 10000} 在同一算例上少给（用例给出旧值以证伪）。</li>
 *   <li><b>池链综合服务费线性求和</b>(S4 合并留存后)：池 = 收入 − Σ每笔计提费,
 *       费的四舍五入发生在每笔核销计提时(ComprehensiveServiceFee),月结只做线性求和。</li>
 *   <li><b>尾差单列留痕并归平台</b>：各家舍入后与应分总额的差额落
 *       {@code settlement_bills.rounding_residual_cent}（V343），且守恒
 *       {@code Σ池分账明细 + 尾差 ≡ 应分总额} 逐分成立，不并进任一商户、不静默吞掉。</li>
 * </ol>
 *
 * <p>非 @Transactional（settleMonth 内部真提交）；@AfterEach 按项目清理落地行；每例独立 projectId。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementRoundingResidualTests {

    @Autowired private TicketDistributionSettlementService service;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2014, 7); // 远离今日的确定月
    private final List<String> projectIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String pid : projectIds) {
            // V344 起 settlement_transactions 是 append-only 硬强制表(触发器拒 UPDATE/DELETE),
            //   清场 DELETE 须走测试专用旁路(与 SettlementBatch2EngineTests 同范式)。
            com.complex.platform.support.AppendOnlyTestBypass.update(
                    jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_configs WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_merchant_rates WHERE project_id = ?", pid);
        }
    }

    // ---- ① 排名档位分池:四舍五入(旧整除截断少给) ----

    @Test
    void rankTierShareRoundsHalfUpInsteadOfTruncating() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 3333);            // 每家 33.33%(费实况 0 → 池 = 收入,算例干净)
        // 收入 1000 分 → 总池 1000;每家精确 333.3 分。
        // 旧整除:1000×3333/10000 = 333(截断);新四舍五入:333.3 → 333(本例同值,见下一例的进位算例)。
        seedVerified(pid, "r1", 3, 1, 400L);
        seedVerified(pid, "r2", 2, 1, 300L);
        seedVerified(pid, "r3", 1, 1, 300L);

        service.settleMonth(pid, MONTH);

        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(1000L);
        assertThat(itemAmount(pid, "r1")).isEqualTo(333L);
        // 应分总额 = round(333.3×3) = round(999.9) = 1000;Σ已舍入 = 999 → 尾差 +1 归平台。
        assertThat(num(bill(pid), "rounding_residual_cent")).isEqualTo(1L);
        assertConserved(pid, 1000L);   // 应分总额 = round(333.3×3) = 1000
    }

    @Test
    void rankTierShareCarriesUpWhereTruncationWouldLose() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 1255);             // 12.55%
        seedVerified(pid, "c1", 3, 1, 100L);      // 总池 100 分
        // 精确 = 100 × 12.55% = 12.55 分。旧整除截断 = 12(少给 1 分);新四舍五入 = 13。
        service.settleMonth(pid, MONTH);

        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(100L);
        assertThat(100L * 1255L / 10000L).isEqualTo(12L);   // 证伪:旧整除截断给 12
        assertThat(itemAmount(pid, "c1")).isEqualTo(13L);   // 新:四舍五入给 13
        // 应分总额 = round(12.55) = 13;Σ已舍入 = 13 → 尾差 0。
        assertThat(num(bill(pid), "rounding_residual_cent")).isZero();
        assertConserved(pid, 13L);     // 应分总额 = round(12.55) = 13
    }

    // ---- ② 池链综合服务费线性求和(S4 合并留存:原「留存四舍五入」随留存链删除) ----

    @Test
    void pooledServiceFeeReducesPoolByExactPerVerificationSum() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 0);                       // 不分账,只看池链
        // 每笔核销计提的费在核销时已按 HALF_UP 落库(如 1001 × 20.55% → 206),月结只做线性求和。
        seedVerifiedWithFee(pid, "t1", 1, 1, 1001L, 206L);
        service.settleMonth(pid, MONTH);

        Map<String, Object> b = bill(pid);
        assertThat(num(b, "comp_service_fee_pooled_cent")).isEqualTo(206L);
        assertThat(num(b, "available_pool_cent")).isEqualTo(795L); // 1001 − 206
        // 拆份额链守恒:收入 = 费 + 池(渠道费 0)
        assertThat(num(b, "comp_service_fee_pooled_cent") + num(b, "available_pool_cent"))
                .isEqualTo(num(b, "ticket_revenue_cent"));
    }

    // ---- ③ 尾差单列留痕 + 守恒(核心) ----

    @Test
    void roundingResidualIsRecordedSeparatelyAndConserves() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 3333);              // 三家各 33.33%
        seedVerified(pid, "s1", 3, 1, 5000L);
        seedVerified(pid, "s2", 2, 1, 3000L);
        seedVerified(pid, "s3", 1, 1, 2000L);      // 总池 10000 分

        service.settleMonth(pid, MONTH);

        // 各家精确 3333.0 分 → 舍入后各 3333;应分总额 = round(9999.0) = 9999 → 尾差 0(整除也刚好)。
        assertThat(itemAmount(pid, "s1")).isEqualTo(3333L);
        assertThat(num(bill(pid), "rounding_residual_cent")).isZero();
        assertConserved(pid, 9999L);   // 应分总额 = 3333.0×3 = 9999(无舍入)
    }

    @Test
    void residualIsNegativeWhenEveryShareCarriesUp() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 5050);              // 两家各 50.5%
        seedVerified(pid, "n1", 2, 1, 60L);
        seedVerified(pid, "n2", 1, 1, 40L);        // 总池 100 分

        service.settleMonth(pid, MONTH);

        // 每家精确 50.5 → HALF_UP 各 51(Σ=102);应分总额 = round(101.0) = 101 → 尾差 −1(平台追回)。
        assertThat(itemAmount(pid, "n1")).isEqualTo(51L);
        assertThat(itemAmount(pid, "n2")).isEqualTo(51L);
        assertThat(num(bill(pid), "rounding_residual_cent")).isEqualTo(-1L);
        assertConserved(pid, 101L);    // 102 + (−1) = 101,尾差单列后总额仍严格相等
    }

    @Test
    void residualIsZeroForNonRatioMethodsSoBehaviourIsUnchanged() {
        String pid = newProject();
        setMethod(pid, "by_fixed_amount");
        setMerchantRate(pid, "f1", null, 777L);
        setMerchantRate(pid, "f2", null, 333L);
        seedVerified(pid, "f1", 2, 1, 5000L);
        seedVerified(pid, "f2", 1, 1, 5000L);

        service.settleMonth(pid, MONTH);

        assertThat(itemAmount(pid, "f1")).isEqualTo(777L);
        assertThat(itemAmount(pid, "f2")).isEqualTo(333L);
        assertThat(num(bill(pid), "rounding_residual_cent")).isZero(); // 固定额无舍入 → 无尾差
        assertConserved(pid, 1110L);   // 应分总额 = 777 + 333
    }

    /**
     * 守恒核心断言:{@code Σ池分账明细 + 尾差 ≡ 应分总额}(应分总额由用例按业务算式独立给出,不复用引擎中间值),
     * 且尾差量级 ≤ 参与家数(越界=分配算法错而非舍入)。
     */
    private void assertConserved(String pid, long expectedDesignedTotalCent) {
        long residual = num(bill(pid), "rounding_residual_cent");
        Long pooled = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(si.calculated_amount_cent), 0) FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.distribution_method <> 'exclusive_full'
                """, Long.class, pid);
        Integer parties = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.distribution_method <> 'exclusive_full'
                """, Integer.class, pid);
        assertThat(Math.abs(residual))
                .as("尾差量级须 ≤ 参与分账家数,越界=分配算法错而非舍入")
                .isLessThanOrEqualTo(Math.max(1L, parties));
        assertThat(pooled + residual)
                .as("Σ池分账(%d) + 尾差(%d) 必须等于应分总额(尾差单列后总额严格相等)", pooled, residual)
                .isEqualTo(expectedDesignedTotalCent);
    }

    // ---- helpers ----

    private String newProject() {
        String pid = "proj_r7_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    private void seedVerified(String projectId, String merchantId, int verifiedCount, int numPeople,
                              long verifiedCent) {
        seedVerifiedWithFee(projectId, merchantId, verifiedCount, numPeople, verifiedCent, 0L);
    }

    private void seedVerifiedWithFee(String projectId, String merchantId, int verifiedCount, int numPeople,
                                     long verifiedCent, long compServiceFeeCent) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_r7_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, created_at, updated_at,
                    comp_service_fee_cent)
                VALUES (?, ?, 'verification_settled', ?, 'entry', ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?,
                    'draft', ?, ?, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, "vf_" + id, ts, ts, ts, compServiceFeeCent);
    }

    private void setRankTier(String projectId, int rankFrom, int rankTo, int shareBps) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps,
                    status, is_draft)
                VALUES (?, ?, ?, ?, ?, 'active', FALSE)
                """, "trt_" + projectId + "_" + rankFrom, projectId, rankFrom, rankTo, shareBps);
    }

    private void setMethod(String projectId, String method) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_configs (id, project_id, method_type, status, is_draft)
                VALUES (?, ?, ?, 'active', FALSE)
                """, "tdc_" + projectId, projectId, method);
    }

    private void setMerchantRate(String projectId, String merchantId, Long perGuest, Long fixed) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_merchant_rates (id, project_id, merchant_id,
                    per_guest_cent, fixed_amount_cent, status, is_draft)
                VALUES (?, ?, ?, ?, ?, 'active', FALSE)
                """, "tdmr_" + projectId + "_" + merchantId, projectId, merchantId, perGuest, fixed);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private long itemAmount(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.calculated_amount_cent FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, Long.class, projectId, merchantId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
