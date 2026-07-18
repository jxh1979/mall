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
 * 结算重构批2-3：月度通票分账计算引擎集成验收。
 *
 * <p>直插 settlement_transactions（verification_settled，confirmed_at 落月窗）→ 调
 * {@link TicketDistributionSettlementService#settleMonth} → 断言 settlement_bills / settlement_items。
 *
 * <p>非 @Transactional（settleMonth 内部 @Transactional 真提交）；@AfterEach 按项目/月清理落地行。
 * 每个用例用独立 projectId（含 nanoTime）隔离，避免并发会话/真实数据串扰。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementBatch2EngineTests {

    @Autowired
    private TicketDistributionSettlementService service;

    @Autowired
    private JdbcSettlementBillRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2013, 5); // 远离今日的确定月
    private final List<String> projectIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String pid : projectIds) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            // 该项目临时配置行(部分用例插的项目特例)
            jdbcTemplate.update("DELETE FROM ticket_distribution_configs WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_merchant_rates WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM platform_retention_rules WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM giveaway_cost_configs WHERE project_id = ?", pid);
        }
    }

    // ---- ① 池/留存/总池 + 赠酒扣减 ----

    @Test
    void poolRetentionAndGiveawayCostAreComputedCorrectly() {
        String pid = newProject();
        // 两家酒吧核销收入合计 100000 分;另 gift_drink 10 杯(默认每杯 300)= 赠酒成本 3000。
        seedVerified(pid, "m1", "entry", 5, 3, 60000L);
        seedVerified(pid, "m2", "entry", 4, 2, 40000L);
        seedVerified(pid, "m1", "gift_drink", 10, 0, 0L); // 赠酒收入 0,仅核销 10 杯

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(100000L);
        assertThat(num(bill, "channel_fee_cent")).isEqualTo(0L);          // 渠道费先 0
        assertThat(num(bill, "giveaway_cost_cent")).isEqualTo(3000L);     // 10 × 300
        assertThat(num(bill, "available_pool_cent")).isEqualTo(97000L);   // 100000 − 0 − 3000
        assertThat(bill.get("retention_bps")).isEqualTo(2000);           // 默认 20%
        assertThat(num(bill, "retention_cent")).isEqualTo(19400L);        // 97000 × 20%
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(77600L);         // 97000 − 19400
    }

    // ---- ② by_rank_tier(默认档位 16/12/9/6)----

    @Test
    void byRankTierDistributesByRankShareBps() {
        String pid = newProject();
        // 默认方式=by_rank_tier;造 3 家不同核销数 → 排名 1/2/3。
        // 池=100000,留存 20% → 总池 80000。第1名 16%=12800,2-4名各 12%=9600。
        seedVerified(pid, "top", "entry", 10, 0, 50000L);   // rank1
        seedVerified(pid, "mid", "entry", 6, 0, 30000L);    // rank2
        seedVerified(pid, "low", "entry", 2, 0, 20000L);    // rank3

        service.settleMonth(pid, MONTH);

        assertThat(num(bill(pid), "spv_pool_cent")).isEqualTo(80000L);
        assertThat(itemAmount(pid, "top")).isEqualTo(12800L); // 80000×16%
        assertThat(itemAmount(pid, "mid")).isEqualTo(9600L);  // 80000×12%(rank2 in 2-4)
        assertThat(itemAmount(pid, "low")).isEqualTo(9600L);  // 80000×12%(rank3 in 2-4)
        assertThat(itemRank(pid, "top")).isEqualTo(1);
        assertThat(itemRank(pid, "mid")).isEqualTo(2);
        assertThat(itemRank(pid, "low")).isEqualTo(3);
        // Σ=32000 ≤ 80000 → confirmed
        assertThat(bill(pid).get("status")).isEqualTo("confirmed");
    }

    // ---- ② by_venue_per_guest(客数 × 单价)----

    @Test
    void byVenuePerGuestDistributesByGuestCount() {
        String pid = newProject();
        setMethod(pid, "by_venue_per_guest");
        setMerchantRate(pid, "va", 500L, null);  // 每客 5 元
        setMerchantRate(pid, "vb", 800L, null);  // 每客 8 元
        // 客数由 num_people 聚合:va 客数 7,vb 客数 3。
        seedVerified(pid, "va", "entry", 5, 7, 50000L);
        seedVerified(pid, "vb", "entry", 4, 3, 50000L);

        service.settleMonth(pid, MONTH);

        assertThat(itemAmount(pid, "va")).isEqualTo(3500L); // 7 × 500
        assertThat(itemAmount(pid, "vb")).isEqualTo(2400L); // 3 × 800
    }

    // ---- ② by_fixed_amount(固定额)----

    @Test
    void byFixedAmountDistributesFixed() {
        String pid = newProject();
        setMethod(pid, "by_fixed_amount");
        setMerchantRate(pid, "fa", null, 6000L);
        setMerchantRate(pid, "fb", null, 4000L);
        seedVerified(pid, "fa", "entry", 5, 2, 50000L);
        seedVerified(pid, "fb", "entry", 4, 2, 50000L);

        service.settleMonth(pid, MONTH);

        assertThat(itemAmount(pid, "fa")).isEqualTo(6000L);
        assertThat(itemAmount(pid, "fb")).isEqualTo(4000L);
    }

    // ---- ③ Σ分账 > 总池 → 待特批(draft,不确认)----

    @Test
    void overPoolKeepsDraftForSpecialApproval() {
        String pid = newProject();
        setMethod(pid, "by_fixed_amount");
        // 池=100000,留存20% → 总池 80000;两家固定额合计 200000 > 80000。
        setMerchantRate(pid, "x", null, 100000L);
        setMerchantRate(pid, "y", null, 100000L);
        seedVerified(pid, "x", "entry", 5, 2, 50000L);
        seedVerified(pid, "y", "entry", 4, 2, 50000L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(80000L);
        assertThat(num(bill, "distributed_cent")).isEqualTo(200000L);   // 超总池
        assertThat(bill.get("status")).isEqualTo("draft");             // 保持 draft 待特批
        assertThat(bill.get("special_approval_ref")).isNull();         // 待人工填
    }

    // ---- ④ 幂等:重跑同月不重复、覆盖 ----

    @Test
    void rerunSameMonthIsIdempotent() {
        String pid = newProject();
        seedVerified(pid, "m1", "entry", 5, 2, 60000L);
        seedVerified(pid, "m2", "entry", 3, 2, 40000L);

        String id1 = service.settleMonth(pid, MONTH);
        String id2 = service.settleMonth(pid, MONTH);
        assertThat(id2).isEqualTo(id1); // 同项目×月唯一,同一单

        Integer bills = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_bills WHERE project_id = ?", Integer.class, pid);
        assertThat(bills).isEqualTo(1); // 幂等:仍 1 张单
        Integer items = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_items WHERE bill_id = ?", Integer.class, id1);
        assertThat(items).isEqualTo(2); // 2 家,不重复
    }

    // ---- ⑤ 无核销/无配置 → 默认兜底(空单,不报错)----

    @Test
    void noTransactionsNoConfigFallsBackToDefaults() {
        String pid = newProject(); // 不 seed 任何核销、不加任何项目配置
        String id = service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(0L);
        assertThat(num(bill, "available_pool_cent")).isEqualTo(0L);
        assertThat(bill.get("retention_bps")).isEqualTo(2000);          // 建表默认兜底
        assertThat(bill.get("distribution_method")).isEqualTo("by_rank_tier"); // 默认方式兜底
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(0L);
        Integer items = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_items WHERE bill_id = ?", Integer.class, id);
        assertThat(items).isEqualTo(0); // 无核销→无明细
        assertThat(bill.get("status")).isEqualTo("confirmed"); // 0≤0 不越池
    }

    // ---- helpers ----

    private String newProject() {
        String pid = "proj_b2e_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    /** 直插一条 verification_settled(created_at 落 MONTH 月中=月窗驱动,归商户;confirmed_at 留 NULL 同批1双写)。 */
    private void seedVerified(String projectId, String merchantId, String entType,
                              int verifiedCount, int numPeople, long verifiedCent) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_b2e_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    settlement_status, entitlement_valid_to, created_at, updated_at)
                VALUES (?, ?, 'verification_settled', ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?,
                    'draft', ?, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, entType, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, ts, ts, ts);
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
        return jdbcTemplate.queryForMap(
                "SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private long itemAmount(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.calculated_amount_cent FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, Long.class, projectId, merchantId);
    }

    private int itemRank(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.rank_no FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, Integer.class, projectId, merchantId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
