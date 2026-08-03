package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * S3「门票综合服务费:一张票只收一道」验收(V367·K7)。
 *
 * <p>业务口径(business-system-map §「门票综合服务费」):该费与娱乐平台公司留存是<b>同一项、只扣一次</b>;
 * <b>只适用于酒吧相关票务</b>(纯游乐票、纯游乐通票、演出票不收),酒吧相关票务收<b>一道</b>。
 * 计提时点＝该票<b>首笔核销</b>时(用户已拍板)。
 *
 * <p>本类的商户在 {@code merchants} 表里没有行 → 业态判定与 {@code aggregateFormatRevenue} 同源兜底 bar,
 * 故仍走酒吧支;经营域闸本身由 {@code SettlementServiceFeeDimensionS2Tests} 用真商户行覆盖。
 *
 * <p>改前的真 bug:固定额模式按【每笔核销】计提 —— 一张五次通票核销 5 次收 5 道,对账时一张票冒出
 * 5 条同名费,业务解释不了。本类四组断言:
 * <ol>
 *   <li><b>K7</b>:固定额模式,同一张票核销 5 次只收 1 次(首笔),Σ = 一道;</li>
 *   <li><b>比例模式逐分不变</b>:比例费本就是同一道费按核销增量的线性拆分,S3 不设闸 ——
 *       结算单整行 {@code SELECT *} 全列比对,与改前(每笔各自计提)逐分一致;</li>
 *   <li><b>冲正取反 + 锚点复位</b>:冲掉收了费的那笔核销 → 冲正行费取原行之反、父票锚点原样沿用,
 *       该票净额抵回 0 → 下一笔核销重新收一道(票还在、费该收);</li>
 *   <li><b>无锚点拒绝计提</b>:parent_ticket_id 为空 = 不合规发放数据,直接不计提 ——
 *       不退化成「按核销笔数各收一道」(开发环境存量清掉重造,无兼容分支)。</li>
 * </ol>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementFeePerTicketS3Tests {

    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private SettlementReversedConsumer reversedConsumer;
    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2014, 6); // 远离今日的确定月
    private static final Instant AT = MONTH.atDay(10).atTime(20, 0).toInstant(ZoneOffset.UTC);

    private final List<String> projectIds = new ArrayList<>();
    private final List<String> feeConfigIds = new ArrayList<>();
    private final List<String> merchants = new ArrayList<>();

    @BeforeEach
    void setUp() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String m : merchants) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "DELETE FROM settlement_transactions WHERE merchant_id = ?", m);
        }
        for (String pid : projectIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", pid);
        }
        for (String id : feeConfigIds) {
            jdbcTemplate.update("DELETE FROM comprehensive_service_fee_configs WHERE id = ?", id);
        }
        projectIds.clear();
        feeConfigIds.clear();
        merchants.clear();
    }

    // ─────────────────────────── ① K7:固定额一张票只收一道 ───────────────────────────

    /**
     * <b>K7</b>:固定额 300/票,一张五次票核销 5 次 → 只有首笔收 300,其余四笔 0,Σ = 300(改前收 5×300)。
     */
    @Test
    void fixedFee_fiveVerificationsOfOneTicket_chargedExactlyOnce() {
        String merchant = merchant();
        String ticket = "tkt_k7_" + Long.toHexString(System.nanoTime());
        seedFixedFee(merchant, 300L);

        for (int used = 1; used <= 5; used++) {
            verifyTicket(merchant, ticket, "vk7_" + used + "_" + ticket, used);
        }

        assertThat(feeOf("ST-VS-vk7_1_" + ticket)).as("首笔核销收一道").isEqualTo(300L);
        for (int used = 2; used <= 5; used++) {
            assertThat(feeOf("ST-VS-vk7_" + used + "_" + ticket))
                    .as("第 %d 笔核销不得再收(这张票已经收过了)", used).isZero();
        }
        assertThat(ticketFeeNet(ticket)).as("K7:一张票五次核销,费净额恒 = 一道").isEqualTo(300L);
    }

    /** 固定额模式下【不同的票】各收各的一道(闸只按票收口,不误伤别的票)。 */
    @Test
    void fixedFee_differentTicketsEachChargedOnce() {
        String merchant = merchant();
        String t1 = "tkt_a_" + Long.toHexString(System.nanoTime());
        String t2 = "tkt_b_" + Long.toHexString(System.nanoTime());
        seedFixedFee(merchant, 300L);

        verifyTicket(merchant, t1, "vda_" + t1, 1);
        verifyTicket(merchant, t2, "vdb_" + t2, 1);

        assertThat(feeOf("ST-VS-vda_" + t1)).isEqualTo(300L);
        assertThat(feeOf("ST-VS-vdb_" + t2)).as("另一张票是另一道,照收").isEqualTo(300L);
    }

    // ─────────────────────────── ② 比例模式逐分不变(整行全列比对) ───────────────────────────

    /**
     * 比例费是同一道费按核销增量的线性拆分(Σ = 费率 × 该票总收入),S3 对它不设闸。
     * 证法照 S1/S2/S11:同一张五次票核销 5 次,实验组走真消费者(S3 行为),对照组按改前形态
     * (每笔核销各自按增量计提比例费)直接落库 —— 两边 settleMonth 后结算单整行 {@code SELECT *}
     * 转 Map <b>全列比对</b>,并且每一笔流水的费也逐笔相等。
     */
    @Test
    void ratioFee_everyBillColumnPennyIdenticalToPreS3() {
        String after = newProject();   // 实验组:走真消费者(S3 行为)
        String before = newProject();  // 对照组:改前形态(每笔各自计提)
        for (String pid : List.of(after, before)) {
            setRankTier(pid, 1, 50, 10000);
            seedRatioFee(pid, 1000); // 该项目 10%
        }
        String ticket = "tkt_ratio_" + Long.toHexString(System.nanoTime());

        // 实验组:同一张五次票(allocated 50000,增量各 10000)核销 5 次,进池(非专属)。
        for (int used = 1; used <= 5; used++) {
            settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                    "vra_" + used + "_" + ticket, AT.plusSeconds(used), "ent_ra_" + ticket, "ord_x", "tt_x",
                    "entry", "barA", 5, used, 1, 50000L, AT.plusSeconds(86400), null, null, null, after, ticket));
        }
        // 对照组:改前每笔核销各自计提 10% × 增量 10000 = 1000。
        for (int used = 1; used <= 5; used++) {
            seedPreS3Row(before, "vrb_" + used + "_" + ticket, "barA", 10000L, 1000L, AT.plusSeconds(used));
        }

        // 每一笔的费逐笔相等:比例模式每笔仍按增量计提,S3 未改变任何一笔的金额。
        for (int used = 1; used <= 5; used++) {
            assertThat(feeOf("ST-VS-vra_" + used + "_" + ticket))
                    .as("比例模式第 %d 笔费必须与改前一致(按增量计提,不设闸)", used).isEqualTo(1000L);
        }

        settlementService.settleMonth(after, MONTH);
        settlementService.settleMonth(before, MONTH);

        Map<String, Object> a = new HashMap<>(bill(after));
        Map<String, Object> b = new HashMap<>(bill(before));
        for (String col : List.of("id", "project_id", "created_at", "updated_at")) {
            a.remove(col);
            b.remove(col);
        }
        assertThat(a).as("比例模式:结算单每一列与改前逐分一致").isEqualTo(b);
        assertThat(itemAmounts(after)).as("各商户分账明细逐条逐分一致").isEqualTo(itemAmounts(before));
        assertThat(itemAmounts(after)).isNotEmpty();
    }

    // ─────────────────────────── ③ 冲正取反 + 锚点复位 ───────────────────────────

    /**
     * 冲掉收了费的首笔核销:冲正行费 = 原行之反、父票锚点原样沿用 → 该票净额抵回 0 →
     * 下一笔核销<b>重新收一道</b>(不能因为退过一次核销就永远免单);全生命周期净额恒 = 一道。
     */
    @Test
    void reversalNegatesFeeAndResetsAnchor_nextVerificationChargesAgain() {
        String merchant = merchant();
        String ticket = "tkt_rev_" + Long.toHexString(System.nanoTime());
        seedFixedFee(merchant, 300L);

        String v1 = "vrv1_" + ticket;
        String v2 = "vrv2_" + ticket;
        String v3 = "vrv3_" + ticket;
        verifyTicket(merchant, ticket, v1, 1); // 首笔:收 300
        verifyTicket(merchant, ticket, v2, 2); // 已收过:0
        assertThat(feeOf("ST-VS-" + v1)).isEqualTo(300L);
        assertThat(feeOf("ST-VS-" + v2)).isZero();

        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                v1, "rev_" + v1, "ent_" + ticket, "ord_x", merchant, 1));

        // 冲正行:费取原行之反,父票锚点原样沿用(否则净额抵不回 0、锚点复不了位)。
        assertThat(feeOf("ST-VR-rev_" + v1)).isEqualTo(-300L);
        assertThat(parentOf("ST-VR-rev_" + v1)).isEqualTo(ticket);
        assertThat(ticketFeeNet(ticket)).as("冲正后该票费净额抵回 0(锚点复位)").isZero();

        verifyTicket(merchant, ticket, v3, 2); // 复位后下一笔:重新收一道
        assertThat(feeOf("ST-VS-" + v3)).as("锚点复位后重新收一道").isEqualTo(300L);
        assertThat(ticketFeeNet(ticket)).as("全生命周期净额恒 = 一道").isEqualTo(300L);
    }

    // ─────────────────────────── ④ 无锚点 = 不合规数据,拒绝计提 ───────────────────────────

    /**
     * parent_ticket_id 为空(V285 前的不合规存量形态)→ 一律不计提 —— 既不收固定额、也不收比例,
     * 更不退化成「按核销笔数各收一道」。核销流水本身照落(钱的归属不受影响,只是这道费不提)。
     */
    @Test
    void missingParentTicket_refusesFeeAccrual() {
        String merchant = merchant();
        seedFixedFee(merchant, 300L);
        String vid = "vnop_" + Long.toHexString(System.nanoTime());

        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                vid, AT, "ent_" + vid, "ord_x", "tt_x", "entry", merchant,
                1, 1, 1, 10000L, AT.plusSeconds(86400), merchant, null, null, null, null));

        assertThat(feeOf("ST-VS-" + vid)).as("无锚点 = 不合规数据,直接拒绝计提").isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT verified_amount_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, "ST-VS-" + vid))
                .as("核销流水本身照落,只是不提这道费").isEqualTo(10000L);
    }

    // ─────────────────────────── helpers ───────────────────────────

    private String newProject() {
        String pid = "proj_s3_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    private String merchant() {
        String m = "bars3_" + Long.toHexString(System.nanoTime());
        merchants.add(m);
        return m;
    }

    /** 全局商户级固定额费率(票种留空=通用;经 V365 fk 允许 NULL)。 */
    private void seedFixedFee(String merchant, long fixedAmountCent) {
        String id = "csfc_s3f_" + Long.toHexString(System.nanoTime());
        feeConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id,"
                        + " fee_bps, fixed_amount_cent, status, is_draft) VALUES (?, NULL, ?, NULL, 0, ?, 'active', FALSE)",
                id, merchant, fixedAmountCent);
    }

    /** 项目级比例费率(项目下所有票种 10% 之类)。 */
    private void seedRatioFee(String projectId, int feeBps) {
        String id = "csfc_s3r_" + Long.toHexString(System.nanoTime());
        feeConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id,"
                        + " fee_bps, status, is_draft) VALUES (?, ?, NULL, NULL, ?, 'active', FALSE)",
                id, projectId, feeBps);
    }

    /** 专属商户维度、同一张票(parent=ticket)的第 used 次核销(allocated 50000/5 次,增量各 10000)。 */
    private void verifyTicket(String merchant, String ticket, String verificationId, int used) {
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                verificationId, AT.plusSeconds(used), "ent_" + ticket, "ord_x", "tt_x", "entry", merchant,
                5, used, 1, 50000L, AT.plusSeconds(86400), merchant, null, null, null, ticket));
    }

    /** 对照组:改前形态的核销结算行(每笔核销各自计提比例费),进池(非专属)。 */
    private void seedPreS3Row(String projectId, String tag, String merchantId,
                              long verifiedCent, long feeCent, Instant at) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(at);
        String id = "st_s3pre_" + tag;
        jdbcTemplate.update(
                "INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,"
                        + " order_id, ticket_type_id, entitlement_type, verified_count, total_times, num_people,"
                        + " allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,"
                        + " merchant_id, project_id, verification_id, settlement_status,"
                        + " entitlement_valid_to, business_at, discount_amount_cent, comp_service_fee_cent,"
                        + " created_at, updated_at)"
                        + " VALUES (?, ?, 'verification_settled', ?, 'ord_x', 'tt_x', 'entry', 1, 5, 1,"
                        + " ?, ?, 0, 'merchant', ?, ?, ?, 'draft', ?, ?, 0, ?, ?, ?)",
                id, "TN-" + id, "ent_pre_" + projectId, verifiedCent, verifiedCent, merchantId, projectId,
                "vf_" + id, java.sql.Timestamp.from(at.plusSeconds(86400)), ts, feeCent, ts, ts);
    }

    private void setRankTier(String projectId, int rankFrom, int rankTo, int shareBps) {
        jdbcTemplate.update(
                "INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps,"
                        + " status, is_draft) VALUES (?, ?, ?, ?, ?, 'active', FALSE)",
                "trt_s3_" + Long.toHexString(System.nanoTime()), projectId, rankFrom, rankTo, shareBps);
    }

    private long feeOf(String transactionNo) {
        return jdbcTemplate.queryForObject(
                "SELECT comp_service_fee_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, transactionNo);
    }

    private String parentOf(String transactionNo) {
        return jdbcTemplate.queryForObject(
                "SELECT parent_ticket_id FROM settlement_transactions WHERE transaction_no = ?",
                String.class, transactionNo);
    }

    private long ticketFeeNet(String parentTicketId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(comp_service_fee_cent), 0) FROM settlement_transactions WHERE parent_ticket_id = ?",
                Long.class, parentTicketId);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private Map<String, Long> itemAmounts(String projectId) {
        Map<String, Long> out = new HashMap<>();
        jdbcTemplate.query(
                "SELECT si.merchant_id, si.distribution_method, si.calculated_amount_cent, si.final_amount_cent"
                        + " FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id"
                        + " WHERE sb.project_id = ?",
                rs -> {
                    out.put(rs.getString("merchant_id") + "|" + rs.getString("distribution_method") + "|calc",
                            rs.getLong("calculated_amount_cent"));
                    out.put(rs.getString("merchant_id") + "|" + rs.getString("distribution_method") + "|final",
                            rs.getLong("final_amount_cent"));
                }, projectId);
        return out;
    }
}
