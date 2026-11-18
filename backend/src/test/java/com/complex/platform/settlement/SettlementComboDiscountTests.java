package com.complex.platform.settlement;

// 文件版本：V0.02

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
 * ②按份额承担——直插带折扣的核销流水，调 settleMonth 断言各家按实付份额分、bill.combo_discount_cent 恒 0
 * （第十八批 AW 段 293-R1，BSM:754「与折扣一并分摊到各域，由各域主管公司按份额各自承担或取得」）。
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
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", ent);
        }
        for (String pid : projectIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
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
                t.plusSeconds(3600), null, null, 1000L, null, "tkt_" + ent));

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
                t.plusSeconds(3600), null, null, null, null, "tkt_" + ent));

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
                    t.plusSeconds(3600), null, null, 9L, null, "tkt_" + ent));
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
                t.plusSeconds(3600), "barX", 500L, 1000L, null, "tkt_" + ent));

        assertThat(verifiedOf("ST-VS-vc_excl_" + ent)).isEqualTo(500L); // 按结算价直付
        assertThat(discountOf("ST-VS-vc_excl_" + ent)).isZero();        // 专属路径折扣恒 0
    }

    // ─── ② 按份额承担（池线）──────────────────────────────────────────────────

    @Test
    void discountIsBorneByTheSharesAndNotAddedBackToThePool() {
        String pid = newProject();
        // 两家非专属核销：实付份额 60000+40000=100000，折扣 12000+8000=20000（照记、不再加回）。
        // S4:每笔综合服务费按实付份额 20% 计提(消费者口径)——m1 12000、m2 8000。
        seedPooled(pid, "m1", "entry", 5, 3, 60000L, 12000L, 12000L);
        seedPooled(pid, "m2", "entry", 4, 2, 40000L, 8000L, 8000L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).as("池基数＝实付份额").isEqualTo(100000L);
        assertThat(num(bill, "combo_discount_cent")).isZero();
        assertThat(com.complex.platform.support.PassPoolFixtures.barNetCent(jdbcTemplate, (String) bill.get("id"))).isEqualTo(80000L);
    }

    // ─── ② 按份额承担（专属直付线）──────────────────────────────────────────────

    @Test
    void exclusiveMerchantIsPaidItsShareWithoutDiscountAddBack() {
        String pid = newProject();
        // 专属核销实付份额 80000、折扣 10000 → 商户按份额拿 80000（该商户专属费率 0）。
        seedExclusive(pid, "barX", "bar_drink", 12, 9, 80000L, 10000L);

        service.settleMonth(pid, MONTH);

        assertThat(exclusiveItemAmount(pid, "barX")).isEqualTo(80000L);
        assertThat(num(bill(pid), "combo_discount_cent")).isZero();
    }

    // ─── ③ 冲正：折扣负向对冲净掉 ─────────────────────────────────────────────

    @Test
    void reversalNetsDiscountToZeroInSameMonth() {
        String pid = newProject();
        // 同月一笔核销（净额 1000、折扣 200、费 240）+ 一笔冲正（负向取反）→ 净额/折扣/费皆净为 0。
        seedPooled(pid, "m1", "entry", 1, 1, 1000L, 200L, 240L);
        seedReversal(pid, "m1", "entry", 1, 1, 1000L, 200L, 240L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).isZero();
        assertThat(num(bill, "combo_discount_cent")).isZero();
        assertThat(num(bill, "comp_service_fee_pooled_cent")).isZero(); // 费同样冲正净掉
    }

    /**
     * 折扣跨月冲正：冲正落在与原销售<b>不同</b>的月时，收入侧按实付份额带符号追回，折扣不另入账（293-R1 起不再加回）。
     * 当月另有一笔普通非折扣销售托住池基数为正（ticket_revenue ≥0，避开 chk_sb_amount）。
     */
    @Test
    void crossMonthReversalClawsBackTheShareOnly() {
        String pid = newProject();
        // 本月一笔红冲（原销售在已过账的上月）：净额 −1000、折扣 −200、费 −240（取原行之反追回）。
        seedReversal(pid, "m1", "entry", 1, 1, 1000L, 200L, 240L);
        // 本月另有一笔普通非折扣销售：净额 +50000、折扣 0、费 10000。
        seedPooled(pid, "m2", "entry", 3, 2, 50000L, 0L, 10000L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "combo_discount_cent")).isZero();
        // 池基数 = 实付份额 50000−1000 = 49000。
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(49000L);
        // S4:综合服务费同样带符号跨月净掉:10000 − 240 = 9760。
        assertThat(num(bill, "comp_service_fee_pooled_cent")).isEqualTo(9760L);
    }

    // ─── ④ 零行为变更：无折扣场景与 D6 前一致 ──────────────────────────────────

    @Test
    void noDiscountRowsBehaveExactlyAsBefore() {
        String pid = newProject();
        // 无折扣（discount=0）。池须与改造前逐分一致(K9 口径);
        // 直插核销没有池快照(同不含酒吧入场、未建池快照的票):酒吧净份额按 ENUM L52 末句整笔单列未分配池余额(折扣域本身零行为变更)。
        seedPooled(pid, "top", "entry", 10, 0, 50000L, 0L, 10000L);
        seedPooled(pid, "mid", "entry", 6, 0, 30000L, 0L, 6000L);
        seedPooled(pid, "low", "entry", 2, 0, 20000L, 0L, 4000L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(100000L);
        assertThat(num(bill, "combo_discount_cent")).isZero();
        assertThat(com.complex.platform.support.PassPoolFixtures.barNetCent(jdbcTemplate, (String) bill.get("id"))).isEqualTo(80000L);
        assertThat(num(bill, "pool_undistributed_cent")).isEqualTo(80000L);
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

    /** 非专属（进池）核销行，带折扣额;S4 可带每笔计提的池链综合服务费。 */
    private void seedPooled(String projectId, String merchantId, String entType,
                            int verifiedCount, int numPeople, long verifiedCent, long discountCent) {
        seedPooled(projectId, merchantId, entType, verifiedCount, numPeople, verifiedCent, discountCent, 0L);
    }

    private void seedPooled(String projectId, String merchantId, String entType, int verifiedCount,
                            int numPeople, long verifiedCent, long discountCent, long compServiceFeeCent) {
        insert(projectId, merchantId, entType, "verification_settled",
                verifiedCount, numPeople, verifiedCent, discountCent, null, compServiceFeeCent);
    }

    /** 冲正（负向）行：金额/次数/折扣/费皆取反（严格取原行之反）。 */
    private void seedReversal(String projectId, String merchantId, String entType,
                              int verifiedCount, int numPeople, long verifiedCent, long discountCent) {
        seedReversal(projectId, merchantId, entType, verifiedCount, numPeople, verifiedCent, discountCent, 0L);
    }

    private void seedReversal(String projectId, String merchantId, String entType, int verifiedCount,
                              int numPeople, long verifiedCent, long discountCent, long compServiceFeeCent) {
        insert(projectId, merchantId, entType, "verification_reversed",
                -verifiedCount, -numPeople, -verifiedCent, -discountCent, null, -compServiceFeeCent);
    }

    /** 专属核销行（exclusive_merchant_id=该商户），带折扣额(该商户专属费率 0 → 费恒 0)。 */
    private void seedExclusive(String projectId, String exclusiveMerchantId, String entType,
                               int verifiedCount, int numPeople, long verifiedCent, long discountCent) {
        insert(projectId, exclusiveMerchantId, entType, "verification_settled",
                verifiedCount, numPeople, verifiedCent, discountCent, exclusiveMerchantId, 0L);
    }

    private void insert(String projectId, String merchantId, String entType, String type,
                        int verifiedCount, int numPeople, long verifiedCent, long discountCent,
                        String exclusiveMerchantId, long compServiceFeeCent) {
        Instant businessAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(businessAt);
        String id = "st_cd_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    exclusive_merchant_id, verification_id, settlement_status, entitlement_valid_to, created_at, updated_at,
                    business_at, discount_amount_cent, comp_service_fee_cent)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?, ?, 'draft', ?, ?, ?, ?, ?, ?)
                """,
                id, "TN-" + id, type, "ent_" + id, entType, verifiedCount,
                Math.max(Math.abs(verifiedCount), 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, exclusiveMerchantId, "vf_" + id, ts, ts, ts, ts, discountCent,
                compServiceFeeCent);
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
