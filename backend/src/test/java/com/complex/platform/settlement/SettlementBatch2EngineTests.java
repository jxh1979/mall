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
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
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
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
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

    // ---- ① 池/综合服务费(S4 合并留存:池 = 收入 − 渠道费 − 池链综合服务费;赠酒不从池扣 D5) ----

    @Test
    void giveawayVerificationsDoNotReduceDistributionPool() {
        String pid = newProject();
        // 两家酒吧核销收入合计 100000 分(每笔按 20% 计提综合服务费=等价保值口径);另 gift_drink 10 杯。
        // D5:赠酒补偿走 bar 域内部结算价应付,不从分账池扣 → 即便 10 杯赠酒核销,也不缩小池。
        seedVerified(pid, "m1", "entry", 5, 3, 60000L);
        seedVerified(pid, "m2", "entry", 4, 2, 40000L);
        seedVerified(pid, "m1", "gift_drink", 10, 0, 0L); // 赠酒 10 杯核销——不应影响池

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(100000L);
        assertThat(num(bill, "channel_fee_cent")).isEqualTo(0L);          // 渠道费先 0
        // S4:池链综合服务费 = Σ每笔计提 20000;可分配池 = 100000 − 0 − 20000(与改造前「池 − 20% 留存」逐分一致)。
        assertThat(num(bill, "comp_service_fee_pooled_cent")).isEqualTo(20000L);
        assertThat(num(bill, "available_pool_cent")).isEqualTo(80000L);
    }

    // ---- ② 第二段:排名档位归一化分尽(S8——档位只作相对权重,余额一次分光)----

    @Test
    void byRankTierDistributesByRankShareBps() {
        String pid = newProject();
        // 默认方式=by_rank_tier;造 3 家不同核销数 → 排名 1/2/3。收入 100000,每笔 20% 综合服务费 → 池 80000。
        // S8 分尽:默认档位 16/12/9/6 只作相对权重——3 家命中 1600/1200/1200(Σ=4000),
        //   归一化把 80000 全部分光:top=80000×1600/4000=32000,mid/low 各 80000×1200/4000=24000。
        //   (两段化前是绝对份额 12800/9600/9600、48000 静默沉淀在池里——正是用户否掉的隐式残值。)
        seedVerified(pid, "top", "entry", 10, 0, 50000L);   // rank1
        seedVerified(pid, "mid", "entry", 6, 0, 30000L);    // rank2
        seedVerified(pid, "low", "entry", 2, 0, 20000L);    // rank3

        service.settleMonth(pid, MONTH);

        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(80000L);
        assertThat(itemAmount(pid, "top")).isEqualTo(32000L); // 80000×1600/4000
        assertThat(itemAmount(pid, "mid")).isEqualTo(24000L); // 80000×1200/4000
        assertThat(itemAmount(pid, "low")).isEqualTo(24000L); // 80000×1200/4000
        assertThat(itemRank(pid, "top")).isEqualTo(1);
        assertThat(itemRank(pid, "mid")).isEqualTo(2);
        assertThat(itemRank(pid, "low")).isEqualTo(3);
        // 分尽:Σ=80000=池 → confirmed,无未分余额。
        assertThat(bill(pid).get("status")).isEqualTo("confirmed");
        assertThat(num(bill(pid), "distributed_cent")).isEqualTo(80000L);
        assertThat(num(bill(pid), "pool_undistributed_cent")).isZero();
    }

    // ---- ② 第一段:每客单价固定规则(S8——商户级规则,不再是项目级方式)----

    @Test
    void perGuestFixedRuleTakesAbsoluteAmountInPhaseOne() {
        String pid = newProject();
        setMerchantRate(pid, "va", 500L, null);  // 第一段固定规则:每客 5 元
        setMerchantRate(pid, "vb", 800L, null);  // 第一段固定规则:每客 8 元
        // 客数由 num_people 聚合:va 客数 7,vb 客数 3。
        seedVerified(pid, "va", "entry", 5, 7, 50000L);
        seedVerified(pid, "vb", "entry", 4, 3, 50000L);

        service.settleMonth(pid, MONTH);

        assertThat(itemAmount(pid, "va")).isEqualTo(3500L); // 7 × 500,绝对额,不归一化
        assertThat(itemAmount(pid, "vb")).isEqualTo(2400L); // 3 × 800
        // K18 ③:两家全走第一段 → 第二段无参与者,余额 80000−5900=74100 不静默沉淀,
        //   显式归娱乐平台公司(V375 all_pass_ticket 全局种子)单列留痕。
        assertThat(num(bill(pid), "pool_undistributed_cent")).isEqualTo(74100L);
        assertThat(bill(pid).get("pool_undistributed_entity_id")).isEqualTo("be_ent_platform");
        assertThat(bill(pid).get("status")).isEqualTo("confirmed"); // 5900 ≤ 80000
    }

    // ---- ② 第一段:固定额固定规则 ----

    @Test
    void fixedAmountRuleTakesAbsoluteAmountInPhaseOne() {
        String pid = newProject();
        setMerchantRate(pid, "fa", null, 6000L);
        setMerchantRate(pid, "fb", null, 4000L);
        seedVerified(pid, "fa", "entry", 5, 2, 50000L);
        seedVerified(pid, "fb", "entry", 4, 2, 50000L);

        service.settleMonth(pid, MONTH);

        assertThat(itemAmount(pid, "fa")).isEqualTo(6000L);
        assertThat(itemAmount(pid, "fb")).isEqualTo(4000L);
        // K18 ③:第二段无参与者 → 余额 80000−10000=70000 显式归娱乐平台公司留痕。
        assertThat(num(bill(pid), "pool_undistributed_cent")).isEqualTo(70000L);
        assertThat(bill(pid).get("pool_undistributed_entity_id")).isEqualTo("be_ent_platform");
    }

    // ---- ②b S8 两段混跑:第一段绝对额先拿,其余按档位分尽余额,拿了第一段不占名次 ----

    @Test
    void twoPhaseMixedFixedAndTierBarsConserveWholePool() {
        String pid = newProject();
        // 第一段:fixedBar 固定 12000;pgBar 每客 300 × 10 客 = 3000 → Q = 15000。
        setMerchantRate(pid, "fixedBar", null, 12000L);
        setMerchantRate(pid, "pgBar", 300L, null);
        // 第二段:项目档位 rank1=60% / rank2=40%(Σ=100%)。
        setRankTier(pid, 1, 1, 6000);
        setRankTier(pid, 2, 2, 4000);
        // 核销数:fixedBar(20)/pgBar(15) 反而最高——但拿了第一段就不占第二段名次(K18 ⑤)。
        seedVerified(pid, "fixedBar", "entry", 20, 5, 30000L);
        seedVerified(pid, "pgBar", "entry", 15, 10, 20000L);
        seedVerified(pid, "tierA", "entry", 8, 3, 30000L);
        seedVerified(pid, "tierB", "entry", 2, 1, 20000L);
        // 收入 100000,费 20000 → 池 80000;P2 = 80000 − 15000 = 65000。

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "available_pool_cent")).isEqualTo(80000L);
        // 第一段:绝对额,rank 留空,方式按规则种类留痕。
        assertThat(itemAmount(pid, "fixedBar")).isEqualTo(12000L);
        assertThat(itemMethod(pid, "fixedBar")).isEqualTo("by_fixed_amount");
        assertThat(itemRankOrNull(pid, "fixedBar")).isNull();
        assertThat(itemAmount(pid, "pgBar")).isEqualTo(3000L);
        assertThat(itemMethod(pid, "pgBar")).isEqualTo("by_venue_per_guest");
        assertThat(itemRankOrNull(pid, "pgBar")).isNull();
        // 第二段:排名只在第二段参与者内部编——tierA 核销数(8)低于两家第一段酒吧仍是 rank1。
        assertThat(itemRank(pid, "tierA")).isEqualTo(1);
        assertThat(itemRank(pid, "tierB")).isEqualTo(2);
        assertThat(itemAmount(pid, "tierA")).isEqualTo(39000L); // 65000×6000/10000
        assertThat(itemAmount(pid, "tierB")).isEqualTo(26000L); // 65000×4000/10000
        // K18:Q + Σ第二段 + 尾差 ≡ 池(15000 + 65000 + 0 = 80000),无未分余额。
        assertThat(num(bill, "distributed_cent")).isEqualTo(80000L);
        assertThat(num(bill, "rounding_residual_cent")).isZero();
        assertThat(num(bill, "pool_undistributed_cent")).isZero();
        assertThat(bill.get("status")).isEqualTo("confirmed");
    }

    // ---- ③ Σ分账 > 总池 → 待特批(draft,不确认);S8:第一段 Q>P 不削、第二段为 0 ----

    @Test
    void overPoolKeepsDraftForSpecialApproval() {
        String pid = newProject();
        // 收入 100000,每笔 20% 综合服务费 → 可分配池 80000;两家第一段固定额合计 200000 > 80000。
        setMerchantRate(pid, "x", null, 100000L);
        setMerchantRate(pid, "y", null, 100000L);
        seedVerified(pid, "x", "entry", 5, 2, 50000L);
        seedVerified(pid, "y", "entry", 4, 2, 50000L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "available_pool_cent")).isEqualTo(80000L);
        assertThat(num(bill, "distributed_cent")).isEqualTo(200000L);   // 超可分配池:绝对承诺不削
        assertThat(bill.get("status")).isEqualTo("draft");             // 保持 draft 待特批
        assertThat(bill.get("special_approval_ref")).isNull();         // 待人工填
        assertThat(num(bill, "pool_undistributed_cent")).isZero();     // P2=max(0,P−Q)=0,无余额可留
    }

    /** S8 边界①:Q &gt; P 且当月还有排名档位酒吧——第二段为 0(不削第一段),名次仍留痕,整月走特批。 */
    @Test
    void overPoolByPhaseOneLeavesZeroForTierBars() {
        String pid = newProject();
        setMerchantRate(pid, "big", null, 90000L);   // 第一段固定额 90000 > 池 80000
        setRankTier(pid, 1, 9, 10000);
        seedVerified(pid, "big", "entry", 10, 3, 50000L);
        seedVerified(pid, "tb", "entry", 5, 2, 50000L);
        // 收入 100000,费 20000 → 池 80000;Q=90000 > 80000 → P2=0。

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(itemAmount(pid, "big")).isEqualTo(90000L);   // 不削
        assertThat(itemAmount(pid, "tb")).isZero();             // 第二段整段为 0
        assertThat(itemRank(pid, "tb")).isEqualTo(1);           // 名次仍留痕(第二段内部 rank1)
        assertThat(num(bill, "distributed_cent")).isEqualTo(90000L);
        assertThat(bill.get("status")).isEqualTo("draft");      // 超池 → 待特批(既有路径)
        assertThat(num(bill, "pool_undistributed_cent")).isZero();
    }

    // ---- ③b 逐分不变判据:无第一段酒吧 + 档位和恰 100% → 与两段化前整行逐分一致 ----

    /**
     * S8 验收判据(用户拍板):当期没有任何固定规则酒吧、且档位对在场名次的份额和恰为 100% 时,
     * 归一化 prorate(P,w,10000) ≡ 旧绝对份额 bpsOf(P,w) —— 结算单<b>整行全列</b>与两段化前
     * 手算值逐分一致,明细逐条一致(含 .5 进位样本:80001×50% = 40000.5 → 40001)。
     */
    @Test
    void pennyInvariantWhenNoFixedSetAndTiersSumToFull() {
        String pid = newProject();
        setRankTier(pid, 1, 1, 5000);
        setRankTier(pid, 2, 2, 3000);
        setRankTier(pid, 3, 3, 2000);
        seedVerified(pid, "t1", "entry", 10, 2, 40001L); // 费 8000(40001/5 整除截断,seed 口径)
        seedVerified(pid, "t2", "entry", 6, 1, 30007L);  // 费 6001
        seedVerified(pid, "t3", "entry", 2, 1, 29992L);  // 费 5998
        // 收入 100000,费 19999 → 池 80001。

        String billId = service.settleMonth(pid, MONTH);

        // 旧引擎手算(bpsOf 绝对份额):t1=round(80001×0.5)=40001(.5 进位)、t2=round(24000.3)=24000、
        //   t3=round(16000.2)=16000;Σ=80001=池;尾差 0。新引擎归一化同基数同权重 → 必须逐分相同。
        Map<String, Object> expected = new java.util.LinkedHashMap<>();
        expected.put("id", billId);
        expected.put("project_id", pid);
        expected.put("period_start", java.sql.Date.valueOf(MONTH.atDay(1)));
        expected.put("period_end", java.sql.Date.valueOf(MONTH.atEndOfMonth()));
        expected.put("ticket_revenue_cent", 100000L);
        expected.put("channel_fee_cent", 0L);
        expected.put("available_pool_cent", 80001L);
        expected.put("distributed_cent", 80001L);
        expected.put("comp_service_fee_pooled_cent", 19999L);
        expected.put("comp_service_fee_exclusive_cent", 0L);
        expected.put("fee_beneficiary_entity_id", "be_ent_platform"); // V370 全局种子
        expected.put("gift_cost_cent", 0L);                            // S6:无赠送计提 → G 恒 0
        expected.put("gift_beneficiary_entity_id", "be_ent_platform"); // V372 归属种子
        expected.put("distribution_method", "by_rank_tier");
        expected.put("special_approval_ref", null);
        expected.put("special_approval_valid_until", null);
        expected.put("status", "confirmed");
        expected.put("confirmed_at", null);
        expected.put("settled_at", null);
        expected.put("attributed_entity_id", "be_ent_platform"); // V375 all_pass_ticket 全局种子
        expected.put("combo_discount_cent", 0L);
        expected.put("rounding_residual_cent", 0L);
        expected.put("pool_undistributed_cent", 0L);
        expected.put("pool_undistributed_entity_id", null);

        Map<String, Object> actual = new java.util.HashMap<>(bill(pid));
        actual.remove("created_at");
        actual.remove("updated_at");
        assertThat(actual).as("无第一段 + 档位和=100% → 结算单每一列与两段化前逐分不变").isEqualTo(expected);

        assertThat(itemAmount(pid, "t1")).isEqualTo(40001L);
        assertThat(itemAmount(pid, "t2")).isEqualTo(24000L);
        assertThat(itemAmount(pid, "t3")).isEqualTo(16000L);
        assertThat(itemRank(pid, "t1")).isEqualTo(1);
        assertThat(itemRank(pid, "t2")).isEqualTo(2);
        assertThat(itemRank(pid, "t3")).isEqualTo(3);
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
        assertThat(num(bill, "comp_service_fee_pooled_cent")).isEqualTo(0L); // 无核销无计提
        assertThat(bill.get("distribution_method")).isEqualTo("by_rank_tier"); // 默认方式兜底
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

    /** 直插一条 verification_settled(created_at 落 MONTH 月中=月窗驱动,归商户;confirmed_at 留 NULL 同批1双写)。
     *  S4:每笔带 20% 池链综合服务费(= V370 等价保值费率下消费者会计提的值),月结只做线性求和。 */
    private void seedVerified(String projectId, String merchantId, String entType,
                              int verifiedCount, int numPeople, long verifiedCent) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_b2e_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, created_at, updated_at,
                    comp_service_fee_cent)
                VALUES (?, ?, 'verification_settled', ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?,
                    'draft', ?, ?, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, entType, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, "vf_" + id, ts, ts, ts, verifiedCent / 5);
    }

    /** S8/V374:第一段固定规则,rule_type 按「填了哪列」二选一(与写入口同推导)。 */
    private void setMerchantRate(String projectId, String merchantId, Long perGuest, Long fixed) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_merchant_rates (id, project_id, merchant_id,
                    rule_type, per_guest_cent, fixed_amount_cent, status, is_draft)
                VALUES (?, ?, ?, ?, ?, ?, 'active', FALSE)
                """, "tdmr_" + projectId + "_" + merchantId, projectId, merchantId,
                perGuest != null ? "per_guest" : "fixed", perGuest, fixed);
    }

    private void setRankTier(String projectId, int rankFrom, int rankTo, int shareBps) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps,
                    status, is_draft)
                VALUES (?, ?, ?, ?, ?, 'active', FALSE)
                """, "trt_" + projectId + "_" + rankFrom, projectId, rankFrom, rankTo, shareBps);
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

    /** S8:第一段酒吧 rank 留空(不占第二段名次),需可空读取。 */
    private Integer itemRankOrNull(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.rank_no FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, Integer.class, projectId, merchantId);
    }

    private String itemMethod(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.distribution_method FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, String.class, projectId, merchantId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
