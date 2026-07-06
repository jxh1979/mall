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
 * 结算重构批2-5：端到端集成验收（核销双写 → 月度分账引擎 → 过账 全链）。
 *
 * <p>一条完整资金链，用明确数字端到端验证 §5.0 全流程闭合，跨批2-3(引擎)+批2-4(过账)两服务。
 * 配置全走建表默认(留存 20%、by_rank_tier 档位 1600/1200/900/600、赠酒 3 元/杯)，不 seed 任何项目配置。
 * 走「直造 settlement_transactions」而非真实核销链(TicketingService.createVerification)——批1 双写已由批1 测试守，
 * 本 E2E 优先保证端到端数字断言清晰(直造亦充分覆盖引擎+过账全链)。
 *
 * <p>两条用例：
 * <ol>
 *   <li>{@code fullChainFourBars_...}：4 家酒吧 + 赠酒，引擎算池/留存/各酒吧排名分账 + 全链对账（Σitems=Σ过账应付），
 *       逐项数字对；单条结算单从 confirmed 过账到 settled。</li>
 *   <li>{@code fullChainSingleBar_...}：最小闭环——1 家酒吧 verification→distribution→posting→ar_ap→settled 全链贯通，
 *       逐环断言 ar_ap(payable/ticket_distribution/金额/counterparty)、明细 posted+回填、结算单 settled。</li>
 * </ol>
 *
 * <p><b>过账幂等键 40 字符截断丢账（曾为真实资金 bug，已修）</b>：
 * 旧 {@code SettlementPostingService} 用明文 {@code "settle-ticket-dist-"+settlementItemId} 作幂等键，
 * 经 {@code FinanceService.slug()} 截断 40 字符——标准明细 id {@code si_sb_<projectId>_<YearMonth>_<merchant>}
 * 商户判别位落在 40 字符之后 → 同一结算单多家酒吧应付 PK({@code arap_<slug40>}) 相撞、被 ON CONFLICT DO UPDATE
 * 覆盖成 1 条丢账。现幂等键改为明细 id 的短哈希（{@code settle-td-<sha20>}，定长 <40 字符不截断）→ N 家落 N 条独立应付。
 * 故用例①过账段<b>逐家真实断言 4 条独立应付</b>（每家 direction/source_type/counterparty/金额独立），守修复有效。
 *
 * <p>非 @Transactional(settleMonth/postBill 内部各自 @Transactional 真提交)；过账走 FinanceService(@GuardedAction)
 * 故 @BeforeEach 登录平台管理员。@AfterEach 按 project 清结算单/明细/核销；ar_ap_items 为不可变台账(source_id 唯一)不清。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
class SettlementBatch2E2ETests {

    @Autowired
    private TicketDistributionSettlementService settlementService;

    @Autowired
    private SettlementPostingService postingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2013, 5); // 远离今日的确定月，与批2-3/2-4 同口径
    private final List<String> projectIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 过账 postBill → FinanceService.createArApItem(@GuardedAction) 需登录主体过鉴权/审计。
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
            // ar_ap_items 不可变(DB 触发器禁 DELETE)，source_id 明细 id 唯一不与后续用例冲突，不清。
        }
    }

    /**
     * 用例①：4 家酒吧 + 赠酒 → 引擎算池/留存/各酒吧排名分账 → 过账 → 全链对账（整数分无漂移）。
     *
     * <p>造数：ticket_revenue = 60000+40000+30000+20000 = 150000；gift_drink 10 杯 → 赠酒 10×300=3000。
     * 核销数降序 b1>b2>b3>b4 → 排名 1/2/3/4；默认档位第1名 16%、2-4 名各 12%。
     */
    @Test
    void fullChainFourBarsDistributionAndReconciliation() {
        String pid = newProject();

        // ① 核销双写落月：4 家酒吧 entry 核销(不同核销数 → 排名) + gift_drink 10 杯。
        seedVerified(pid, "bar1", "entry", 20, 8, 60000L); // rank1
        seedVerified(pid, "bar2", "entry", 15, 6, 40000L); // rank2
        seedVerified(pid, "bar3", "entry", 10, 4, 30000L); // rank3
        seedVerified(pid, "bar4", "entry", 5, 2, 20000L);  // rank4
        seedVerified(pid, "bar1", "gift_drink", 10, 0, 0L); // 赠酒 10 杯，收入 0

        // ② 引擎：settleMonth。
        String billId = settlementService.settleMonth(pid, MONTH);

        // ---- 断言 settlement_bills 逐项数字 ----
        Map<String, Object> bill = bill(pid);
        long ticketRevenue = 150000L;
        long giveawayCost = 10L * 300L;                          // 3000
        long availablePool = ticketRevenue - 0L - giveawayCost;  // 147000
        long retention = availablePool * 2000 / 10000;           // 29400
        long spvPool = availablePool - retention;                // 117600
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(ticketRevenue);
        assertThat(num(bill, "channel_fee_cent")).isEqualTo(0L);
        assertThat(num(bill, "giveaway_cost_cent")).isEqualTo(giveawayCost);
        assertThat(num(bill, "available_pool_cent")).isEqualTo(availablePool);
        assertThat(bill.get("retention_bps")).isEqualTo(2000);
        assertThat(num(bill, "retention_cent")).isEqualTo(retention);
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(spvPool);
        assertThat(bill.get("distribution_method")).isEqualTo("by_rank_tier");

        // ---- 断言 settlement_items 各酒吧按排名命中档位 → spv_pool × share_bps/10000 ----
        long a1 = spvPool * 1600 / 10000; // 18816
        long a2 = spvPool * 1200 / 10000; // 14112
        long a3 = spvPool * 1200 / 10000; // 14112
        long a4 = spvPool * 1200 / 10000; // 14112
        assertThat(itemAmount(pid, "bar1")).isEqualTo(a1);
        assertThat(itemAmount(pid, "bar2")).isEqualTo(a2);
        assertThat(itemAmount(pid, "bar3")).isEqualTo(a3);
        assertThat(itemAmount(pid, "bar4")).isEqualTo(a4);
        assertThat(itemRank(pid, "bar1")).isEqualTo(1);
        assertThat(itemRank(pid, "bar2")).isEqualTo(2);
        assertThat(itemRank(pid, "bar3")).isEqualTo(3);
        assertThat(itemRank(pid, "bar4")).isEqualTo(4);

        long distributed = a1 + a2 + a3 + a4; // 61152
        assertThat(num(bill, "distributed_cent")).isEqualTo(distributed);
        assertThat(distributed).isLessThanOrEqualTo(spvPool);  // Σitems.final ≤ spv_pool
        assertThat(bill.get("status")).isEqualTo("confirmed");  // 未越池 → confirmed，可过账

        // ③ 过账：postBill → ar_ap payable/ticket_distribution。
        int posted = postingService.postBill(billId);
        assertThat(posted).isEqualTo(4); // 4 家 final>0 全过

        // 全过 → 结算单 settled + settled_at 落。
        assertThat(bill(pid).get("status")).isEqualTo("settled");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT settled_at FROM settlement_bills WHERE id = ?", java.sql.Timestamp.class, billId)).isNotNull();

        // B-1 守卫(2026-07-06 审计):bill 已 settled 后再跑 settleMonth 必须被拒——否则 deleteItemsByBill
        //   会删掉已挂应付(ar_ap_item_id 已置)的明细,产生孤儿 AR/AP。守卫在 settleMonth 起始 fail-fast。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> settlementService.settleMonth(pid, MONTH))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("已过账");
        // 且已 settled 的 bill 与明细未被破坏(守卫在写前抛,事务回滚无副作用)。
        assertThat(bill(pid).get("status")).isEqualTo("settled");

        // 复核补强:reversed(V224 CHECK 合法终结态,将来接冲正即可达)也必须 fail-closed 拒绝重结——
        //   否则对已 reverse 的单重跑会删掉曾 posted 回填过 ar_ap_item_id 的明细→孤儿 AR/AP。旧的正向白名单会漏放它。
        jdbcTemplate.update("UPDATE settlement_bills SET status = 'reversed' WHERE id = ?", billId);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> settlementService.settleMonth(pid, MONTH))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("不可重结");
        assertThat(bill(pid).get("status")).isEqualTo("reversed");
        // 各明细均 posted + 回填 ar_ap_item_id。
        for (String m : new String[]{"bar1", "bar2", "bar3", "bar4"}) {
            assertThat(itemStatus(pid, m)).isEqualTo("posted");
            assertThat(itemArApId(pid, m)).isNotNull();
        }

        // ---- ④ 全链对账断言(整数分无漂移) ----
        // 收入 − 赠酒 = 池
        assertThat(ticketRevenue - giveawayCost).isEqualTo(availablePool);
        // 池 = 留存 + 总池
        assertThat(retention + spvPool).isEqualTo(availablePool);
        // Σ各酒吧分账 + 余数 = 总池(余数=整除截断沉淀，天然 ≥0)
        long remainder = spvPool - distributed;
        assertThat(distributed + remainder).isEqualTo(spvPool);
        assertThat(remainder).isGreaterThanOrEqualTo(0L);
        long sumItemsFinal = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(final_amount_cent), 0) FROM settlement_items WHERE bill_id = ?",
                Long.class, billId);
        assertThat(sumItemsFinal).isEqualTo(distributed); // 明细口径无漂移

        // ---- 逐家真实断言：4 家过账后恰好 4 条独立应付，各家金额/对手方独立正确（修前 1 条、修后 4 条）----
        long arApCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM ar_ap_items
                 WHERE direction = 'payable' AND source_type = 'ticket_distribution'
                   AND source_id IN (SELECT id FROM settlement_items WHERE bill_id = ?)
                """, Long.class, billId);
        assertThat(arApCount).isEqualTo(4L); // ← 修复核心断言：多家不相撞，落 N 条

        Map<String, Long> expectedByBar = Map.of("bar1", a1, "bar2", a2, "bar3", a3, "bar4", a4);
        for (var e : expectedByBar.entrySet()) {
            String m = e.getKey();
            Map<String, Object> arap = arApBySource(itemId(pid, m));
            assertThat(arap.get("direction")).isEqualTo("payable");
            assertThat(arap.get("source_type")).isEqualTo("ticket_distribution");
            assertThat(arap.get("counterparty_id")).isEqualTo(m); // 各家对手方=各自 merchant
            assertThat(((Number) arap.get("amount_cent")).longValue()).isEqualTo(e.getValue()); // 各家金额独立正确
        }
        // Σ各家应付 = Σ明细 final（逐家落地后，聚合亦精确相等，无丢账）。
        long sumArAp = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(aa.amount_cent), 0) FROM ar_ap_items aa
                 WHERE aa.direction = 'payable' AND aa.source_type = 'ticket_distribution'
                   AND aa.source_id IN (SELECT id FROM settlement_items WHERE bill_id = ?)
                """, Long.class, billId);
        assertThat(sumArAp).isEqualTo(sumItemsFinal);
    }

    /**
     * 用例②：单家酒吧最小全链闭环——verification → distribution → posting → ar_ap → settled 逐环贯通，
     * 逐环数字对。用短 project id 保过账幂等键落在 40 字符内、应付真实落地一条（守「各酒吧一条 payable」语义）。
     */
    @Test
    void fullChainSingleBarVerificationToPostedPayable() {
        String pid = newProject();
        seedVerified(pid, "b", "entry", 8, 5, 50000L); // 单家：收入 50000

        String billId = settlementService.settleMonth(pid, MONTH);

        // 引擎：池 = 50000 − 0 − 0(无赠酒) = 50000；留存 20% = 10000；总池 40000；rank1 16% = 6400。
        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(50000L);
        assertThat(num(bill, "giveaway_cost_cent")).isEqualTo(0L);
        assertThat(num(bill, "available_pool_cent")).isEqualTo(50000L);
        assertThat(num(bill, "retention_cent")).isEqualTo(10000L);
        assertThat(num(bill, "spv_pool_cent")).isEqualTo(40000L);
        long expected = 40000L * 1600 / 10000; // 6400
        assertThat(itemAmount(pid, "b")).isEqualTo(expected);
        assertThat(itemRank(pid, "b")).isEqualTo(1);
        assertThat(bill.get("status")).isEqualTo("confirmed");

        // 过账 → 应付真实落地一条。
        int posted = postingService.postBill(billId);
        assertThat(posted).isEqualTo(1);

        String itemId = itemId(pid, "b");
        Map<String, Object> arap = arApBySource(itemId);
        assertThat(arap.get("direction")).isEqualTo("payable");
        assertThat(arap.get("source_type")).isEqualTo("ticket_distribution");
        assertThat(arap.get("counterparty_id")).isEqualTo("b");
        assertThat(((Number) arap.get("amount_cent")).longValue()).isEqualTo(expected);
        assertThat(itemStatus(pid, "b")).isEqualTo("posted");
        assertThat(itemArApId(pid, "b")).isNotNull();

        // 全过 → 结算单 settled。
        assertThat(bill(pid).get("status")).isEqualTo("settled");
        // 全链对账：应付金额 = 明细 final = 分账。
        assertThat(((Number) arap.get("amount_cent")).longValue()).isEqualTo(expected);
    }

    // ---- helpers（照批2-3/2-4 风格）----

    /**
     * 短项目 id：结算单/明细 id 派生自 project_id。历史上过账幂等键曾受 slug 40 字符截断，短 id 是当时的规避；
     * 现幂等键已改短哈希（见类注释），长短 id 均落 N 条独立应付，短 id 仅留作稳定造数习惯。
     */
    private String newProject() {
        String pid = "p" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL);
        projectIds.add(pid);
        return pid;
    }

    /** 直插一条 verification_settled(created_at 落 MONTH 月中=月窗驱动，归商户；confirmed_at 留 NULL 同批1双写)。 */
    private void seedVerified(String projectId, String merchantId, String entType,
                             int verifiedCount, int numPeople, long verifiedCent) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_b2e2e_" + System.nanoTime();
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

    private int itemRank(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.rank_no FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, Integer.class, projectId, merchantId);
    }

    private String itemId(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.id FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, String.class, projectId, merchantId);
    }

    private String itemStatus(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.status FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, String.class, projectId, merchantId);
    }

    private String itemArApId(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.ar_ap_item_id FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, String.class, projectId, merchantId);
    }

    private Map<String, Object> arApBySource(String itemId) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM ar_ap_items WHERE source_type = 'ticket_distribution' AND source_id = ?", itemId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
