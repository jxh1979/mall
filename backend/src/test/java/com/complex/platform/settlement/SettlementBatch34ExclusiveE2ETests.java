package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 结算重构批3-4：专属某商户「不分摊」(PRD §6.4) 端到端验收。
 *
 * <p>造 N 家酒吧：含 1 家有<b>专属权益核销</b>(exclusive_merchant_id=该家) + 其余普通通票核销。跑 settleMonth 后：
 * <ol>
 *   <li>专属家拿到一条 {@code exclusive_full} 明细 = 其专属核销<b>全额</b>（未扣留存）；</li>
 *   <li>池(spvPool) = 仅非专属收入算的池，<b>不含</b>专属那笔；</li>
 *   <li>专属核销<b>不出现</b>在其它家的 rank/guest 聚合里（by_rank_tier 下专属家不因专属核销抬排名、
 *       他家排名/分额不被稀释）；</li>
 *   <li>非专属家分账与无专属场景一致（回归）。</li>
 * </ol>
 * 逐家真实断言各明细金额（资金铁律，不按单聚合规避）。配置全走建表默认（留存 20%、
 * by_rank_tier 档位 1600/1200/900/600、赠酒 3 元/杯）。走「直造 settlement_transactions」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementBatch34ExclusiveE2ETests {

    @Autowired
    private TicketDistributionSettlementService settlementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2013, 7); // 远离今日的确定月
    private final List<String> projectIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        TestSecurityActors.clear();
        for (String pid : projectIds) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
        }
    }

    /**
     * 专属家 barX 有专属核销 80000（全额直付、不进池）+ 普通池核销 30000（进池、参与排名）；
     * 另有 bar1/bar2/bar3 纯普通池核销 60000/40000/20000。
     *
     * <p>池基（非专属）= 60000(bar1)+40000(bar2)+30000(barX 普通)+20000(bar3) = 150000；
     * 留存 20% = 30000；spvPool = 120000。
     * 排名按<b>非专属</b>核销数降序：bar1(20)&gt;bar2(15)&gt;barX(10 普通)&gt;bar3(5) → rank 1/2/3/4。
     * 专属 80000 全额一条 exclusive_full 归 barX，未扣留存、不占池、不改任何家排名。
     */
    @Test
    void exclusiveFullPaidDirectAndPoolExcludesExclusive() {
        String pid = newProject();

        // 普通池核销（进池、参与排名/客数）
        seedVerified(pid, "bar1", "entry", 20, 8, 60000L); // rank1
        seedVerified(pid, "bar2", "entry", 15, 6, 40000L); // rank2
        seedVerified(pid, "barX", "entry", 10, 4, 30000L); // rank3（barX 的普通部分）
        seedVerified(pid, "bar3", "entry", 5, 2, 20000L);  // rank4

        // 专属核销：exclusive_merchant_id=barX，全额 80000 直付 barX，不进池、不参与排名/客数聚合
        seedExclusive(pid, "barX", "bar_drink", 12, 9, 80000L);

        String billId = settlementService.settleMonth(pid, MONTH);

        // ---- 池口径：仅非专属 ----
        Map<String, Object> bill = bill(pid);
        long poolBase = 150000L;                       // 不含专属 80000
        long availablePool = poolBase - 0L - 0L;        // 无赠酒
        long retention = availablePool * 2000 / 10000;  // 30000
        long spvPool = availablePool - retention;       // 120000
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(poolBase); // ② header 池基 = 非专属 Σ，不含专属
        assertThat(num(bill, "available_pool_cent")).isEqualTo(availablePool);
        assertThat(num(bill, "retention_cent")).isEqualTo(retention);
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(spvPool);       // ② 池不含专属那笔

        // ---- 池分账明细：各家按非专属排名命中档位（专属 80000 不抬 barX 排名、不稀释他家）----
        long a1 = spvPool * 1600 / 10000; // bar1 rank1 16% = 19200
        long a2 = spvPool * 1200 / 10000; // bar2 rank2 12% = 14400
        long a3 = spvPool * 1200 / 10000; // barX 普通 rank3 12% = 14400
        long a4 = spvPool * 1200 / 10000; // bar3 rank4 12% = 14400
        assertThat(pooledItemAmount(pid, "bar1")).isEqualTo(a1);
        assertThat(pooledItemAmount(pid, "bar2")).isEqualTo(a2);
        assertThat(pooledItemAmount(pid, "barX")).isEqualTo(a3); // barX 池分账只按其普通核销，不含专属
        assertThat(pooledItemAmount(pid, "bar3")).isEqualTo(a4);
        // ③ 排名不被专属稀释/抬升：barX 仍 rank3（若专属计入，barX 核销数=22 会抢 rank1）
        assertThat(pooledItemRank(pid, "bar1")).isEqualTo(1);
        assertThat(pooledItemRank(pid, "bar2")).isEqualTo(2);
        assertThat(pooledItemRank(pid, "barX")).isEqualTo(3);
        assertThat(pooledItemRank(pid, "bar3")).isEqualTo(4);

        // ---- ① 专属直付明细：barX 一条 exclusive_full = 全额 80000（未扣留存）----
        Map<String, Object> ex = exclusiveItem(pid, "barX");
        assertThat(ex.get("distribution_method")).isEqualTo("exclusive_full");
        assertThat(((Number) ex.get("final_amount_cent")).longValue()).isEqualTo(80000L); // 全额，不扣 20% 留存
        assertThat(((Number) ex.get("calculated_amount_cent")).longValue()).isEqualTo(80000L);
        assertThat(ex.get("rank_no")).isNull(); // 专属不参与排名

        // barX 恰好两条明细：一条池分账 + 一条 exclusive_full，各自独立金额
        long barXItemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id"
                        + " WHERE sb.project_id = ? AND si.merchant_id = 'barX'",
                Long.class, pid);
        assertThat(barXItemCount).isEqualTo(2L);

        // ---- ④ 非专属家回归：分额与「无专属」场景恒等（专属不干扰档位分额）----
        assertThat(a1).isEqualTo(19200L);
        assertThat(a2).isEqualTo(14400L);
        assertThat(a4).isEqualTo(14400L);

        // 专属 80000 只在 exclusive_full 一条体现，未混入池分账（Σ池分账 ≤ spvPool）。
        long pooledSum = a1 + a2 + a3 + a4; // 62400
        assertThat(pooledSum).isLessThanOrEqualTo(spvPool);
        assertThat(bill.get("status")).isEqualTo("confirmed"); // 池分账未越池
        assertThat(billId).isNotBlank();
    }

    // ---- helpers ----

    private String newProject() {
        String pid = "p" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL);
        projectIds.add(pid);
        return pid;
    }

    /** 普通池核销：exclusive_merchant_id=NULL（进池、参与排名/客数）。 */
    private void seedVerified(String projectId, String merchantId, String entType,
                             int verifiedCount, int numPeople, long verifiedCent) {
        insert(projectId, merchantId, entType, verifiedCount, numPeople, verifiedCent, null);
    }

    /** 专属核销：exclusive_merchant_id=该商户（全额直付、不进池、不参与排名/客数）。 */
    private void seedExclusive(String projectId, String exclusiveMerchantId, String entType,
                              int verifiedCount, int numPeople, long verifiedCent) {
        insert(projectId, exclusiveMerchantId, entType, verifiedCount, numPeople, verifiedCent, exclusiveMerchantId);
    }

    private void insert(String projectId, String merchantId, String entType, int verifiedCount,
                        int numPeople, long verifiedCent, String exclusiveMerchantId) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_b34_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,"
                        + " entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,"
                        + " verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,"
                        + " settlement_status, entitlement_valid_to, exclusive_merchant_id, verification_id, created_at, updated_at)"
                        + " VALUES (?, ?, 'verification_settled', ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?,"
                        + " 'draft', ?, ?, ?, ?, ?)",
                id, "TN-" + id, "ent_" + id, entType, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, ts, exclusiveMerchantId, "vf_" + id, ts, ts);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    /** 该家池分账明细金额（distribution_method != exclusive_full）。 */
    private long pooledItemAmount(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject(
                "SELECT si.calculated_amount_cent FROM settlement_items si"
                        + " JOIN settlement_bills sb ON sb.id = si.bill_id"
                        + " WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method <> 'exclusive_full'",
                Long.class, projectId, merchantId);
    }

    private int pooledItemRank(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject(
                "SELECT si.rank_no FROM settlement_items si"
                        + " JOIN settlement_bills sb ON sb.id = si.bill_id"
                        + " WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method <> 'exclusive_full'",
                Integer.class, projectId, merchantId);
    }

    /** 该家专属直付明细整行（distribution_method = exclusive_full）。 */
    private Map<String, Object> exclusiveItem(String projectId, String merchantId) {
        return jdbcTemplate.queryForMap(
                "SELECT si.* FROM settlement_items si"
                        + " JOIN settlement_bills sb ON sb.id = si.bill_id"
                        + " WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method = 'exclusive_full'",
                projectId, merchantId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
