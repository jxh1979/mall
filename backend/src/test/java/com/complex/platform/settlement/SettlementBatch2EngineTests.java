package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.PassPoolFixtures;
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
 * <p>第十八批第 2 条起两段分配按单张票的池快照逐份执行（ENUM L52）：第一段名单与金额随票固化在快照里、按每份计；第二段按本张票的
 * 第二段参与家数取「参与家数档位表」一行、按当期入场人数排名，不再按项目月度单梯子归一化分尽；旧月度第一段规则与旧单梯子停读。
 * 分账类用例因此给父票补一行池快照（毛额即池），原月度口径期望按新口径改写，算例尽量沿用原数。
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
            PassPoolFixtures.deleteByProject(jdbcTemplate, pid);
            // 该项目临时配置行(部分用例插的项目特例)
            jdbcTemplate.update("DELETE FROM ticket_distribution_configs WHERE project_id = ?", pid);
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
        seedVerified(pid, null, "m1", "entry", 5, 3, 60000L);
        seedVerified(pid, null, "m2", "entry", 4, 2, 40000L);
        seedVerified(pid, null, "m1", "gift_drink", 10, 0, 0L); // 赠酒 10 杯核销——不应影响池

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "ticket_revenue_cent")).isEqualTo(100000L);
        assertThat(num(bill, "channel_fee_cent")).isEqualTo(0L);          // 渠道费先 0
        // S4:池链综合服务费 = Σ每笔计提 20000;酒吧净份额 = 100000 − 0 − 20000(与改造前「池 − 20% 留存」逐分一致)。
        assertThat(num(bill, "comp_service_fee_pooled_cent")).isEqualTo(20000L);
        // 直插核销无池快照(同不含酒吧入场、未建池快照的票):酒吧净份额按 ENUM L52 末句进可分配池并整笔单列未分配池余额。
        assertThat(PassPoolFixtures.barNetCent(jdbcTemplate, (String) bill.get("id"))).isEqualTo(80000L);
        assertThat(num(bill, "available_pool_cent")).isEqualTo(80000L);
        assertThat(num(bill, "pool_undistributed_cent")).isEqualTo(80000L);
    }

    // ---- ② 第二段:按参与家数行取动态百分比(不归一化),排名在本票第二段参与者内部按当期入场人数 ----

    @Test
    void secondSegmentTakesParticipantRowSharesByEntryPeopleRank() {
        String pid = newProject();
        // 一张票覆盖 3 家、都在第二段 → 取「参与 3 家」行 40/30/30;当期入场人数 10/6/2 → 排名 1/2/3;池 80000。
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 80000L, "top", "mid", "low");
        seedVerified(pid, "tk_" + pid, "top", "entry", 1, 10, 0L);   // rank1
        seedVerified(pid, "tk_" + pid, "mid", "entry", 1, 6, 0L);    // rank2
        seedVerified(pid, "tk_" + pid, "low", "entry", 1, 2, 0L);    // rank3

        service.settleMonth(pid, MONTH);

        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(80000L);
        assertThat(itemAmount(pid, "top")).isEqualTo(32000L); // 80000×40%
        assertThat(itemAmount(pid, "mid")).isEqualTo(24000L); // 80000×30%
        assertThat(itemAmount(pid, "low")).isEqualTo(24000L); // 80000×30%
        assertThat(itemRank(pid, "top")).isEqualTo(1);
        assertThat(itemRank(pid, "mid")).isEqualTo(2);
        assertThat(itemRank(pid, "low")).isEqualTo(3);
        assertThat(bill(pid).get("status")).isEqualTo("confirmed");
        assertThat(num(bill(pid), "distributed_cent")).isEqualTo(80000L);
        assertThat(num(bill(pid), "pool_undistributed_cent")).isZero();
    }

    // ---- ② 第一段:固定单价(按每份计,随票固化在快照里)----

    @Test
    void perGuestFirstSegmentTakesAbsoluteAmountPerUnit() {
        String pid = newProject();
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 80000L, PassPoolFixtures.FACTORY_SCHEME,
                List.of("va", "vb"), Map.of());
        perGuestFirstSegment("tk_" + pid, "va", 500L);   // 第一段固定单价:每份 5 元
        perGuestFirstSegment("tk_" + pid, "vb", 800L);   // 第一段固定单价:每份 8 元
        seedVerified(pid, "tk_" + pid, "va", "entry", 1, 1, 0L);
        seedVerified(pid, "tk_" + pid, "vb", "entry", 1, 1, 0L);

        service.settleMonth(pid, MONTH);

        assertThat(itemAmount(pid, "va")).isEqualTo(500L); // 绝对额,不归一化
        assertThat(itemMethod(pid, "va")).isEqualTo("by_venue_per_guest");
        assertThat(itemAmount(pid, "vb")).isEqualTo(800L);
        // K18 ③:两家全走第一段 → 第二段无参与者,余额 80000−1300=78700 不静默沉淀,显式归娱乐平台公司单列留痕。
        assertThat(num(bill(pid), "pool_undistributed_cent")).isEqualTo(78700L);
        assertThat(bill(pid).get("pool_undistributed_entity_id")).isEqualTo("be_ent_platform");
        assertThat(bill(pid).get("status")).isEqualTo("confirmed"); // 1300 ≤ 80000
    }

    // ---- ② 第一段:固定金额 ----

    @Test
    void fixedAmountFirstSegmentTakesAbsoluteAmount() {
        String pid = newProject();
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 80000L, PassPoolFixtures.FACTORY_SCHEME,
                List.of("fa", "fb"), Map.of("fa", 6000L, "fb", 4000L));
        seedVerified(pid, "tk_" + pid, "fa", "entry", 1, 2, 0L);
        seedVerified(pid, "tk_" + pid, "fb", "entry", 1, 2, 0L);

        service.settleMonth(pid, MONTH);

        assertThat(itemAmount(pid, "fa")).isEqualTo(6000L);
        assertThat(itemAmount(pid, "fb")).isEqualTo(4000L);
        // K18 ③:第二段无参与者 → 余额 80000−10000=70000 显式归娱乐平台公司留痕。
        assertThat(num(bill(pid), "pool_undistributed_cent")).isEqualTo(70000L);
        assertThat(bill(pid).get("pool_undistributed_entity_id")).isEqualTo("be_ent_platform");
    }

    // ---- ②b 两段混跑:第一段绝对额先拿,其余按参与家数行分第二段余额,拿了第一段不占名次 ----

    @Test
    void twoPhaseMixedFirstSegmentAndParticipantRowConserveWholePool() {
        String pid = newProject();
        // 方案「参与 2 家」行 60/40;第一段:fixedBar 固定 12000;pgBar 每份 3000 → Q = 15000。
        String scheme = PassPoolFixtures.scheme(jdbcTemplate, "tdsv_b2e_" + pid, pid, Map.of(2, new int[] {6000, 4000}));
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 80000L, scheme,
                List.of("fixedBar", "pgBar", "tierA", "tierB"), Map.of("fixedBar", 12000L));
        perGuestFirstSegment("tk_" + pid, "pgBar", 3000L);
        // 当期入场人数:fixedBar(20)/pgBar(15) 反而最高——但拿了第一段就不占第二段名次(K18 ⑤)。
        seedVerified(pid, "tk_" + pid, "fixedBar", "entry", 1, 20, 0L);
        seedVerified(pid, "tk_" + pid, "pgBar", "entry", 1, 15, 0L);
        seedVerified(pid, "tk_" + pid, "tierA", "entry", 1, 8, 0L);
        seedVerified(pid, "tk_" + pid, "tierB", "entry", 1, 2, 0L);
        // 池 80000;P2 = 80000 − 15000 = 65000。

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
        // 第二段:名次只在第二段参与者内部编;明细名次留痕当期全月名次(第一段两家人数更高,排在前面)。
        assertThat(itemAmount(pid, "tierA")).isEqualTo(39000L); // 65000×60%
        assertThat(itemAmount(pid, "tierB")).isEqualTo(26000L); // 65000×40%
        // Q + Σ第二段 + 尾差 ≡ 池(15000 + 65000 + 0 = 80000),无未分余额。
        assertThat(num(bill, "distributed_cent")).isEqualTo(80000L);
        assertThat(num(bill, "rounding_residual_cent")).isZero();
        assertThat(num(bill, "pool_undistributed_cent")).isZero();
        assertThat(bill.get("status")).isEqualTo("confirmed");
    }

    // ---- ③ 第一段合计 > 份池 → 待特批(draft,不确认);不削第一段、第二段为 0 ----

    @Test
    void overPoolKeepsDraftForSpecialApproval() {
        String pid = newProject();
        // 池 80000;两家第一段固定额合计 200000 > 80000。
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 80000L, PassPoolFixtures.FACTORY_SCHEME,
                List.of("x", "y"), Map.of("x", 100000L, "y", 100000L));
        seedVerified(pid, "tk_" + pid, "x", "entry", 1, 2, 0L);
        seedVerified(pid, "tk_" + pid, "y", "entry", 1, 2, 0L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(num(bill, "available_pool_cent")).isEqualTo(80000L);
        assertThat(num(bill, "distributed_cent")).isEqualTo(200000L);   // 超可分配池:绝对承诺不削
        assertThat(num(bill, "pool_overflow_cent")).isEqualTo(120000L); // 第一段超池额单列
        assertThat(bill.get("status")).isEqualTo("draft");             // 保持 draft 待特批
        assertThat(bill.get("special_approval_ref")).isNull();         // 待人工填
        assertThat(num(bill, "pool_undistributed_cent")).isZero();     // P2=max(0,P−Q)=0,无余额可留
    }

    /** 边界①:Q &gt; P 且该票还有第二段酒吧——第二段为 0(不削第一段),名次仍留痕,整月走特批。 */
    @Test
    void overPoolByPhaseOneLeavesZeroForSecondSegmentBars() {
        String pid = newProject();
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 80000L, PassPoolFixtures.FACTORY_SCHEME,
                List.of("big", "tb"), Map.of("big", 90000L));   // 第一段固定额 90000 > 池 80000
        seedVerified(pid, "tk_" + pid, "big", "entry", 1, 3, 0L);
        seedVerified(pid, "tk_" + pid, "tb", "entry", 1, 2, 0L);

        service.settleMonth(pid, MONTH);

        Map<String, Object> bill = bill(pid);
        assertThat(itemAmount(pid, "big")).isEqualTo(90000L);   // 不削
        assertThat(itemAmount(pid, "tb")).isZero();             // 第二段整段为 0
        assertThat(itemRank(pid, "tb")).isEqualTo(2);           // 名次仍留痕(当期全月名次)
        assertThat(num(bill, "distributed_cent")).isEqualTo(90000L);
        assertThat(bill.get("status")).isEqualTo("draft");      // 超池 → 待特批(既有路径)
        assertThat(num(bill, "pool_undistributed_cent")).isZero();
    }

    // ---- ③b 结算单整行落库 ----

    /**
     * 逐份分账的结算单整行全列（含 V556 三栏分列与暂挂、超池列）：无第一段、参与 3 家行 50/30/20、当期入场人数 3/2/1，
     * 含 .5 进位样本(80001×50% = 40000.5 → 40001)。
     */
    @Test
    void billRowColumnsForPerTicketDistribution() {
        String pid = newProject();
        String scheme = PassPoolFixtures.scheme(jdbcTemplate, "tdsv_b2e_" + pid, pid, Map.of(3, new int[] {5000, 3000, 2000}));
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 80001L, scheme, List.of("t1", "t2", "t3"), Map.of());
        seedVerified(pid, "tk_" + pid, "t1", "entry", 1, 3, 0L);
        seedVerified(pid, "tk_" + pid, "t2", "entry", 1, 2, 0L);
        seedVerified(pid, "tk_" + pid, "t3", "entry", 1, 1, 0L);

        String billId = service.settleMonth(pid, MONTH);

        // t1=round(80001×0.5)=40001(.5 进位)、t2=round(24000.3)=24000、t3=round(16000.2)=16000;Σ=80001=池;尾差 0。
        Map<String, Object> expected = new java.util.LinkedHashMap<>();
        expected.put("id", billId);
        expected.put("project_id", pid);
        expected.put("period_start", java.sql.Date.valueOf(MONTH.atDay(1)));
        expected.put("period_end", java.sql.Date.valueOf(MONTH.atEndOfMonth()));
        expected.put("ticket_revenue_cent", 80001L);   // 有池快照的酒吧核销不按核销额聚合,收入取快照酒吧份额
        expected.put("channel_fee_cent", 0L);
        expected.put("route_fee_cent", 0L);           // 第十八批第 3 条 V555 通路费列（无付款固化行 → 0）
        expected.put("available_pool_cent", 80001L);
        expected.put("distributed_cent", 80001L);
        expected.put("comp_service_fee_pooled_cent", 0L); // 夹具快照综合服务费 0;核销行计提已不进聚合
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
        expected.put("rounding_residual_entity_id", null); // V383 尾差为 0 → 没有责任人
        expected.put("pool_undistributed_cent", 0L);
        expected.put("pool_undistributed_entity_id", null);
        expected.put("unverified_deduction_cent", 0L);      // V556 通票未核销减除额(三家都核销)
        expected.put("unverified_deduction_entity_id", null);
        expected.put("pool_suspended_cent", 0L);
        expected.put("pool_overflow_cent", 0L);

        Map<String, Object> actual = new java.util.HashMap<>(bill(pid));
        actual.remove("created_at");
        actual.remove("updated_at");
        assertThat(actual).as("逐份分账结算单每一列").isEqualTo(expected);

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
        PassPoolFixtures.snapshot(jdbcTemplate, "tk1_" + pid, pid, 48000L, "m1");
        PassPoolFixtures.snapshot(jdbcTemplate, "tk2_" + pid, pid, 32000L, "m2");
        seedVerified(pid, "tk1_" + pid, "m1", "entry", 1, 2, 0L);
        seedVerified(pid, "tk2_" + pid, "m2", "entry", 1, 2, 0L);

        String id1 = service.settleMonth(pid, MONTH);
        String id2 = service.settleMonth(pid, MONTH);
        assertThat(id2).isEqualTo(id1); // 同项目×月唯一,同一单

        Integer bills = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_bills WHERE project_id = ?", Integer.class, pid);
        assertThat(bills).isEqualTo(1); // 幂等:仍 1 张单
        Integer items = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_items WHERE bill_id = ?", Integer.class, id1);
        assertThat(items).isEqualTo(2); // 2 家,不重复
        Integer units = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pass_pool_unit_results WHERE bill_id = ?", Integer.class, id1);
        assertThat(units).isEqualTo(2); // 逐份结果随单覆盖,不重复
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
     *  S4:每笔带 20% 池链综合服务费(= V370 等价保值费率下消费者会计提的值),月结只做线性求和。
     *  ticket 非空＝池快照父票的核销(金额与扣项改取快照,核销行只决定谁核销、当期入场人数)。 */
    private void seedVerified(String projectId, String ticket, String merchantId, String entType,
                              int verifiedCount, int numPeople, long verifiedCent) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_b2e_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, created_at, updated_at,
                    comp_service_fee_cent, parent_ticket_id)
                VALUES (?, ?, 'verification_settled', ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?,
                    'draft', ?, ?, ?, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, entType, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, "vf_" + id, ts, ts, ts, verifiedCent / 5, ticket);
    }

    /** 快照里的第一段名单一行:固定单价(每份)。 */
    private void perGuestFirstSegment(String ticket, String merchantId, long amountCent) {
        jdbcTemplate.update("""
                INSERT INTO pass_pool_snapshot_first_segment (ticket_instance_id, merchant_id, rule_type, amount_cent)
                VALUES (?, ?, 'per_guest', ?)""", ticket, merchantId, amountCent);
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
