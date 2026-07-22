package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.ticketing.SettlementSettledEvent;
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
 * 结算配置中心 · 批1 · D6：套票折扣额落库 + 结算按归属应用 验收（新建独立测试类，不动 Batch2/Batch34 用例）。
 *
 * <p>两层：①落库——SettlementSettledConsumer 用满额份额算折扣增量写 settlement_transactions.discount_amount_cent；
 * ②归属应用——直插带折扣的核销流水 + 配置 discount_bearer，调 settleMonth 断言 bill.combo_discount_cent 与商户净额。
 *
 * <p>非 @Transactional（settleMonth/消费者内部真提交）；@AfterEach 按项目/权益清理。远离今日的确定月，独立 projectId 隔离。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementComboDiscountTests {

    @Autowired private TicketDistributionSettlementService service;
    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2012, 6);
    private final List<String> projectIds = new ArrayList<>();
    private final List<String> entIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String ent : entIds) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE entitlement_id = ?", ent);
        }
        for (String pid : projectIds) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_combo_discount_configs WHERE project_id = ?", pid);
        }
    }

    // ─── ① 折扣落库（消费者按满额份额算增量）────────────────────────────────────

    @Test
    void discountLandsOnLedgerWhenComponentFullPriceGiven() {
        String ent = ent();
        Instant t = MONTH.atDay(10).atTime(9, 0).toInstant(ZoneOffset.UTC);
        // 满额=1000、实付净额(allocated)=800、total=1、used=1 → 折扣总额=满额−净额=200，全额分配到唯一一笔 → 折扣=200。
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                "vc_full_" + ent, t, ent, "order_x", "tt_x", "entry", "mch_x", 1, 1, 1, 800L,
                t.plusSeconds(3600), null, null, 1000L));

        assertThat(discountOf("ST-VS-vc_full_" + ent)).isEqualTo(200L);
        assertThat(verifiedOf("ST-VS-vc_full_" + ent)).isEqualTo(800L);
    }

    @Test
    void noComponentFullPriceMeansZeroDiscount() {
        String ent = ent();
        Instant t = MONTH.atDay(10).atTime(9, 0).toInstant(ZoneOffset.UTC);
        // fullPriceCent 为空（component_price 全空/direct 出票）→ 折扣恒 0（零行为变更）。
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                "vc_none_" + ent, t, ent, "order_x", "tt_x", "entry", "mch_x", 1, 1, 1, 800L,
                t.plusSeconds(3600), null, null, null));

        assertThat(discountOf("ST-VS-vc_none_" + ent)).isZero();
        assertThat(verifiedOf("ST-VS-vc_none_" + ent)).isEqualTo(800L);
    }

    @Test
    void discountSumsExactlyToFullMinusNetOnNonMonotoneRamp() {
        String ent = ent();
        Instant t = MONTH.atDay(10).atTime(9, 0).toInstant(ZoneOffset.UTC);
        // 满额份额=9、净额份额=8、total=10：满额斜坡 round(9k/10) 与净额斜坡 round(8k/10) 非同步进位——
        // 旧「两斜坡差再 clamp」会把负增量吞成 0、Σ 多计到 2；新「直接分配折扣总额 9−8=1」→ Σ 恰=1。
        for (int used = 1; used <= 10; used++) {
            settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                    "vr_ramp_" + ent + "_" + used, t, ent, "order_x", "tt_x", "entry", "mch_x", 10, used, 1, 8L,
                    t.plusSeconds(3600), null, null, 9L));
        }
        assertThat(sumVerifiedOfEnt(ent)).isEqualTo(8L);   // Σ净额 = allocated
        assertThat(sumDiscountOfEnt(ent)).isEqualTo(1L);   // Σ折扣 = 满额−净额 = 1（精确，不泄漏舍入分）
    }

    @Test
    void exclusiveByPriceKeepsZeroDiscountEvenWithFullPrice() {
        String ent = ent();
        Instant t = MONTH.atDay(10).atTime(9, 0).toInstant(ZoneOffset.UTC);
        // 专属含结算价(exclusiveByPrice)→ 按内部价 500 直付，即便带满额价 1000 也无套票折扣（复核硬约束②）。
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                "vc_excl_" + ent, t, ent, "order_x", "tt_x", "bar_drink", "barX", 1, 1, 1, null,
                t.plusSeconds(3600), "barX", 500L, 1000L));

        assertThat(verifiedOf("ST-VS-vc_excl_" + ent)).isEqualTo(500L); // 按结算价直付
        assertThat(discountOf("ST-VS-vc_excl_" + ent)).isZero();        // 专属路径折扣恒 0
    }

    // ─── ② 三 bearer 归属应用（池线）────────────────────────────────────────────

    @Test
    void platformAbsorbInflatesPoolBaseAndRecordsComboDiscount() {
        String pid = newProject();
        // 默认 bearer=platform_absorb。两家非专属核销：净额 60000+40000=100000，折扣 12000+8000=20000。
        seedPooled(pid, "m1", "entry", 5, 3, 60000L, 12000L);
        seedPooled(pid, "m2", "entry", 4, 2, 40000L, 8000L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        // 项目承担折扣 → 池基数=净额 100000 + 折扣 20000 = 120000（商户按满额分）。
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(120000L);
        assertThat(num(bill, "combo_discount_cent")).isEqualTo(20000L);
        assertThat(num(bill, "available_pool_cent")).isEqualTo(120000L);
        assertThat(num(bill, "retention_cent")).isEqualTo(24000L);   // 120000 × 20%
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(96000L);
    }

    @Test
    void merchantAbsorbLeavesPoolOnNetAndZeroComboDiscount() {
        String pid = newProject();
        setComboDiscount(pid, "merchant_absorb", 0);
        seedPooled(pid, "m1", "entry", 5, 3, 60000L, 12000L);
        seedPooled(pid, "m2", "entry", 4, 2, 40000L, 8000L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        // 商户自担折扣 → 池基数=净额 100000（现状），折扣不入账。
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(100000L);
        assertThat(num(bill, "combo_discount_cent")).isZero();
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(80000L);
    }

    @Test
    void splitByRatioAddsOnlyPlatformShareToPool() {
        String pid = newProject();
        setComboDiscount(pid, "split_by_ratio", 5000); // 平台承担 50%
        seedPooled(pid, "m1", "entry", 5, 3, 60000L, 12000L);
        seedPooled(pid, "m2", "entry", 4, 2, 40000L, 8000L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        // 平台承担折扣 20000×50%=10000 加进基数 → 110000；折扣入账 10000。
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(110000L);
        assertThat(num(bill, "combo_discount_cent")).isEqualTo(10000L);
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(88000L);    // 110000 × 80%
    }

    // ─── ② bearer 归属应用（专属直付线）────────────────────────────────────────

    @Test
    void platformAbsorbGivesExclusiveMerchantFullAmount() {
        String pid = newProject();
        // 默认 platform_absorb；专属核销净额 80000、折扣 10000 → 商户拿满额 90000（综合服务费默认 0）。
        seedExclusive(pid, "barX", "bar_drink", 12, 9, 80000L, 10000L);

        service.settleMonth(pid, MONTH);

        assertThat(exclusiveItemAmount(pid, "barX")).isEqualTo(90000L);
        assertThat(num(bill(pid), "combo_discount_cent")).isEqualTo(10000L);
    }

    @Test
    void merchantAbsorbGivesExclusiveMerchantNetAmount() {
        String pid = newProject();
        setComboDiscount(pid, "merchant_absorb", 0);
        seedExclusive(pid, "barX", "bar_drink", 12, 9, 80000L, 10000L);

        service.settleMonth(pid, MONTH);

        // 商户自担折扣 → 拿净额 80000（现状），折扣不入账。
        assertThat(exclusiveItemAmount(pid, "barX")).isEqualTo(80000L);
        assertThat(num(bill(pid), "combo_discount_cent")).isZero();
    }

    // ─── ③ 冲正：折扣负向对冲净掉 ─────────────────────────────────────────────

    @Test
    void reversalNetsDiscountToZeroInSameMonth() {
        String pid = newProject();
        // 同月一笔核销（净额 1000、折扣 200）+ 一笔冲正（负向取反）→ 净额与折扣皆净为 0。
        seedPooled(pid, "m1", "entry", 1, 1, 1000L, 200L);
        seedReversal(pid, "m1", "entry", 1, 1, 1000L, 200L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).isZero();
        assertThat(num(bill, "combo_discount_cent")).isZero();
    }

    /**
     * 折扣跨月冲正（套票真上线前必修）：冲正落在与原销售<b>不同</b>的月（该月折扣 SUM 为负）时，
     * 项目承担折扣须带符号<b>追回</b>（-D），不能被钳成 0——否则收入侧 -R 追回了、折扣 -D 没追回 → 项目多付该笔折扣。
     * 当月另有一笔普通非折扣销售托住池基数为正（ticket_revenue ≥0，避开 chk_sb_amount），隔离出折扣钳0缺陷。
     */
    @Test
    void crossMonthReversalClawsBackPlatformBorneDiscount() {
        String pid = newProject();
        // 本月一笔红冲（原销售在已过账的上月）：净额 −1000、折扣 −200（platform_absorb 默认承担）。
        seedReversal(pid, "m1", "entry", 1, 1, 1000L, 200L);
        // 本月另有一笔普通非折扣销售：净额 +50000、折扣 0（托住 ticket_revenue>0，隔离折扣钳0）。
        seedPooled(pid, "m2", "entry", 3, 2, 50000L, 0L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        // 项目承担折扣带符号追回：combo_discount_cent = −200（不是 0）。
        assertThat(num(bill, "combo_discount_cent")).isEqualTo(-200L);
        // 池基数 = 净额(50000−1000) + 折扣(−200) = 48800（收入侧与折扣侧对称跨月净掉）。
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(48800L);
    }

    /**
     * split_by_ratio 跨月冲正:平台份额同样带符号追回（锁 {@code f(-x)=-f(x)} 整除舍入对称，master 点名 split_by_ratio）。
     * 折扣 −200 × 平台 50% = −100（不是 0）。
     */
    @Test
    void crossMonthReversalClawsBackSplitByRatioShare() {
        String pid = newProject();
        setComboDiscount(pid, "split_by_ratio", 5000); // 平台承担 50%
        seedReversal(pid, "m1", "entry", 1, 1, 1000L, 200L); // 折扣 −200
        seedPooled(pid, "m2", "entry", 3, 2, 50000L, 0L);    // 托住池基数为正

        service.settleMonth(pid, MONTH);

        assertThat(num(bill(pid), "combo_discount_cent")).isEqualTo(-100L); // −200 × 50%
    }

    // ─── ④ 零行为变更：无折扣场景与 D6 前一致 ──────────────────────────────────

    @Test
    void noDiscountRowsBehaveExactlyAsBefore() {
        String pid = newProject();
        // 无折扣（discount=0），默认 platform_absorb。池/留存/总池须与 D6 前逐分一致。
        seedPooled(pid, "top", "entry", 10, 0, 50000L, 0L);
        seedPooled(pid, "mid", "entry", 6, 0, 30000L, 0L);
        seedPooled(pid, "low", "entry", 2, 0, 20000L, 0L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(100000L);
        assertThat(num(bill, "combo_discount_cent")).isZero();
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(80000L);
        // by_rank_tier 默认 16/12/9/6：rank1=12800，rank2/3 各 9600。
        assertThat(itemAmount(pid, "top")).isEqualTo(12800L);
        assertThat(itemAmount(pid, "mid")).isEqualTo(9600L);
        assertThat(itemAmount(pid, "low")).isEqualTo(9600L);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String newProject() {
        String pid = "proj_cd_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    private String ent() {
        String ent = "ent_cd_" + System.nanoTime();
        entIds.add(ent);
        return ent;
    }

    /** 非专属（进池）核销行，带折扣额。 */
    private void seedPooled(String projectId, String merchantId, String entType,
                            int verifiedCount, int numPeople, long verifiedCent, long discountCent) {
        insert(projectId, merchantId, entType, "verification_settled",
                verifiedCount, numPeople, verifiedCent, discountCent, null);
    }

    /** 冲正（负向）行：金额/次数/折扣皆取反（CHECK 要求 <=0）。 */
    private void seedReversal(String projectId, String merchantId, String entType,
                              int verifiedCount, int numPeople, long verifiedCent, long discountCent) {
        insert(projectId, merchantId, entType, "verification_reversed",
                -verifiedCount, -numPeople, -verifiedCent, -discountCent, null);
    }

    /** 专属核销行（exclusive_merchant_id=该商户），带折扣额。 */
    private void seedExclusive(String projectId, String exclusiveMerchantId, String entType,
                               int verifiedCount, int numPeople, long verifiedCent, long discountCent) {
        insert(projectId, exclusiveMerchantId, entType, "verification_settled",
                verifiedCount, numPeople, verifiedCent, discountCent, exclusiveMerchantId);
    }

    private void insert(String projectId, String merchantId, String entType, String type,
                        int verifiedCount, int numPeople, long verifiedCent, long discountCent,
                        String exclusiveMerchantId) {
        Instant businessAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(businessAt);
        String id = "st_cd_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    exclusive_merchant_id, verification_id, settlement_status, entitlement_valid_to, created_at, updated_at,
                    business_at, discount_amount_cent)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?, ?, 'draft', ?, ?, ?, ?, ?)
                """,
                id, "TN-" + id, type, "ent_" + id, entType, verifiedCount,
                Math.max(Math.abs(verifiedCount), 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, exclusiveMerchantId, "vf_" + id, ts, ts, ts, ts, discountCent);
    }

    private void setComboDiscount(String projectId, String bearer, int splitPlatformBps) {
        jdbcTemplate.update("""
                INSERT INTO ticket_combo_discount_configs (id, project_id, discount_bearer, split_platform_bps,
                    status, is_draft)
                VALUES (?, ?, ?, ?, 'active', FALSE)
                """, "tcdc_" + projectId, projectId, bearer, splitPlatformBps);
    }

    private long discountOf(String transactionNo) {
        return jdbcTemplate.queryForObject(
                "SELECT discount_amount_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, transactionNo);
    }

    private long verifiedOf(String transactionNo) {
        return jdbcTemplate.queryForObject(
                "SELECT verified_amount_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, transactionNo);
    }

    private long sumDiscountOfEnt(String entitlementId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(discount_amount_cent),0) FROM settlement_transactions WHERE entitlement_id = ?",
                Long.class, entitlementId);
    }

    private long sumVerifiedOfEnt(String entitlementId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(verified_amount_cent),0) FROM settlement_transactions WHERE entitlement_id = ?",
                Long.class, entitlementId);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    /** 池分账明细金额（distribution_method != exclusive_full）。 */
    private long itemAmount(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.calculated_amount_cent FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method <> 'exclusive_full'
                """, Long.class, projectId, merchantId);
    }

    /** 专属直付明细金额。 */
    private long exclusiveItemAmount(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.calculated_amount_cent FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method = 'exclusive_full'
                """, Long.class, projectId, merchantId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
