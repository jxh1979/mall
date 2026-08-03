package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * V381 渠道抽成「固定金额」模式验收 —— 业务 HTML :739/:742「先扣掉第三方渠道抽成(比例/金额后台可配)」的「金额」半边。
 *
 * <p>口径:固定金额=<b>每张票</b>收固定金额(与 V328 门票综合服务费固定额同语义),
 * 票数按「该票首笔进池核销」锚点计——一张票多次核销只收一道;冲正冲掉锚点行则计数取反(冲正取反)。
 * 整单前移 + 按业态毛额占比分摊 + 尾差归酒吧支的既有链路(S11)一概不变。
 *
 * <p>非 @Transactional(settleMonth 内部真提交);@AfterEach 按项目清理;每例独立 projectId。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementChannelFeeFixedAmountTests {

    private static final YearMonth MONTH = YearMonth.of(2016, 3); // 远离今日的确定月
    private static final LocalDate PERIOD_START = MONTH.atDay(1);

    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private JdbcSettlementBillRepository billRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<String> projectIds = new ArrayList<>();
    private final List<String> merchantIds = new ArrayList<>();
    private final List<String> orderIds = new ArrayList<>();
    private final List<String> ticketTypeIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String pid : projectIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(
                    jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update("""
                    DELETE FROM settlement_bill_format_shares
                     WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)""", pid);
            jdbcTemplate.update("""
                    DELETE FROM settlement_items
                     WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)""", pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM channel_fee_configs WHERE project_id = ?", pid);
        }
        for (String oid : orderIds) {
            jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", oid);
        }
        for (String tid : ticketTypeIds) {
            jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", tid);
        }
        for (String mid : merchantIds) {
            jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", mid);
        }
        projectIds.clear();
        merchantIds.clear();
        orderIds.clear();
        ticketTypeIds.clear();
    }

    // ---------------------------------------------------------------- ① 模式解析

    /** 配置解析:渠道专属固定金额 > 通用比例;别的渠道落回通用比例;别的项目不受影响(与原 bps 优先级同一套)。 */
    @Test
    void finderResolvesModeAndPriorityPerChannel() {
        String pid = newProject();
        setChannelFeeBps(pid, null, 200);
        setChannelFeeFixed(pid, "meituan", 800L);

        var meituan = billRepository.findChannelFee(pid, "meituan");
        assertThat(meituan.fixedAmountCent()).as("美团配的是每张票固定金额").isEqualTo(800L);
        assertThat(meituan.feeBps()).isZero();
        var miniapp = billRepository.findChannelFee(pid, "miniapp");
        assertThat(miniapp.fixedAmountCent()).as("没单独配的渠道落回通用比例").isNull();
        assertThat(miniapp.feeBps()).isEqualTo(200);
        assertThat(billRepository.findChannelFee(null, "meituan").feeBps())
                .as("别的项目不受本项目配置影响(全局种子=比例 0)").isZero();
        assertThat(billRepository.findChannelFee(null, "meituan").fixedAmountCent()).isNull();
    }

    /** DB CHECK:比例与固定金额二选一,同行双值直接被 chk_cfc_fee_mode 拒掉。 */
    @Test
    void dbCheckRejectsBothModesAtOnce() {
        String pid = newProject();
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, fixed_amount_cent, status, is_draft)
                VALUES (?, ?, NULL, 100, 100, 'active', FALSE)
                """, "cfc_both_" + pid, pid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------- ② 引擎:每张票一道 + 业态分摊 + 尾差归酒吧

    /**
     * 固定金额 500 分/张 × 2 张票 = 1000 分整单渠道费;一张票核销两次(酒吧+游乐)只收一道。
     * 分摊按业态毛额占比,<b>各家(含酒吧)一律按精确占比四舍五入</b>:
     * 游乐 round(1000×30000/130000)=231、酒吧 round(1000×100000/130000)=769,合计恰 1000 → 分摊尾差 0。
     *
     * <p>2026-08-03 口径矫回:酒吧<b>不再拿补数</b> —— 治理地图「尾差…不得悄悄并进任一参与方」,
     * 酒吧也是参与方。分摊尾差改为单列进 {@code rounding_residual_cent} 并挂责任主体(V383)。
     */
    @Test
    void fixedAmountChargesPerTicketAndAllocatesByFormatWithRemainderToBar() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 10000);
        setChannelFeeFixed(pid, null, 500L);
        seedMerchant("m_cff_bar", "bar");
        seedMerchant("m_cff_amu", "amusement");
        String tt = seedTicketType();
        String o1 = seedOrder(tt, "meituan");
        String o2 = seedOrder(tt, "meituan");
        // T1:首笔核销(锚点)在酒吧,第二笔在游乐——只按锚点计 1 张。
        seedSettled(pid, "m_cff_bar", 60000L, o1, "tkt_cff_1", "vf_cff_1", 9);
        seedSettled(pid, "m_cff_amu", 30000L, o1, "tkt_cff_1", "vf_cff_2", 10);
        // T2:单次核销,1 张。
        seedSettled(pid, "m_cff_bar", 40000L, o2, "tkt_cff_2", "vf_cff_3", 9);

        String billId = settlementService.settleMonth(pid, MONTH);
        Map<String, Object> b = bill(pid);
        Map<String, Map<String, Object>> shares = formatShares(billId);

        assertThat(b.get("ticket_revenue_cent")).isEqualTo(130000L);
        assertThat(b.get("channel_fee_cent")).as("500 分/张 × 2 张,多次核销不重复收").isEqualTo(1000L);
        assertThat(shares.get("amusement").get("channel_fee_cent")).isEqualTo(231L);
        assertThat(shares.get("bar").get("channel_fee_cent"))
                .as("酒吧按自己的精确占比四舍五入,不再替全场拿补数").isEqualTo(769L);
        assertThat(((Number) b.get("rounding_residual_cent")).longValue())
                .as("本例各家舍入后恰好分尽 → 渠道分摊尾差 0(池分账亦无尾差)").isZero();
        assertThat(b.get("rounding_residual_entity_id")).as("尾差为 0 → 无责任主体").isNull();
        assertThat(b.get("available_pool_cent")).isEqualTo(99231L);
        assertThat(itemAmounts(pid).values()).containsExactly(99231L);
    }

    /**
     * 冲正取反:锚点行被冲正 → 该票计数 −1(本例同月核销+冲正净 0 张);
     * 非锚点核销被冲正 → 计数不动(那张票的一道费照收)。
     */
    @Test
    void reversalOfAnchorTakesCountNegativeAndNonAnchorReversalDoesNot() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 10000);
        setChannelFeeFixed(pid, null, 500L);
        seedMerchant("m_cfr_bar", "bar");
        seedMerchant("m_cfr_amu", "amusement");
        String tt = seedTicketType();
        String o1 = seedOrder(tt, "meituan");
        String o2 = seedOrder(tt, "meituan");
        // T1:锚点核销后整个被冲正 → 计数 +1−1=0。
        seedSettled(pid, "m_cfr_bar", 50000L, o1, "tkt_cfr_1", "vf_cfr_1", 5);
        seedReversed(pid, "m_cfr_bar", 50000L, o1, "tkt_cfr_1", "vf_cfr_1", 20);
        // T2:锚点在酒吧,游乐那笔(非锚点)核销又冲正 → 计数仍 +1。
        seedSettled(pid, "m_cfr_bar", 30000L, o2, "tkt_cfr_2", "vf_cfr_2", 5);
        seedSettled(pid, "m_cfr_amu", 20000L, o2, "tkt_cfr_2", "vf_cfr_3", 6);
        seedReversed(pid, "m_cfr_amu", 20000L, o2, "tkt_cfr_2", "vf_cfr_3", 21);

        String billId = settlementService.settleMonth(pid, MONTH);
        Map<String, Object> b = bill(pid);
        Map<String, Map<String, Object>> shares = formatShares(billId);

        assertThat(b.get("ticket_revenue_cent")).isEqualTo(30000L);
        assertThat(b.get("channel_fee_cent")).as("净 1 张 × 500 分").isEqualTo(500L);
        assertThat(shares.get("bar").get("channel_fee_cent")).isEqualTo(500L);
        assertThat(b.get("available_pool_cent")).isEqualTo(29500L);
    }

    /** 固定金额配 0 → 渠道费 0,与比例 0(官方直销)逐分同值=保值。 */
    @Test
    void fixedAmountZeroChargesNothing() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 10000);
        setChannelFeeFixed(pid, null, 0L);
        seedMerchant("m_cfz_bar", "bar");
        String tt = seedTicketType();
        String o1 = seedOrder(tt, "meituan");
        seedSettled(pid, "m_cfz_bar", 50000L, o1, "tkt_cfz_1", "vf_cfz_1", 9);

        settlementService.settleMonth(pid, MONTH);
        Map<String, Object> b = bill(pid);
        assertThat(b.get("channel_fee_cent")).isEqualTo(0L);
        assertThat(b.get("available_pool_cent")).isEqualTo(50000L);
    }

    // ---------------------------------------------------------------- 夹具

    private String newProject() {
        String pid = "prj_cff_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        projectIds.add(pid);
        return pid;
    }

    private void seedMerchant(String merchantId, String businessFormat) {
        merchantIds.add(merchantId);
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, ?, ?, 'active', now(), now())
                ON CONFLICT (id) DO UPDATE SET merchant_type = EXCLUDED.merchant_type
                """, merchantId, merchantId, businessFormat);
    }

    private void setRankTier(String projectId, int from, int to, int shareBps) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps, status, is_draft)
                VALUES (?, ?, ?, ?, ?, 'active', FALSE)
                """, "tdrt_" + projectId + "_" + from, projectId, from, to, shareBps);
    }

    private void setChannelFeeBps(String projectId, String channel, int bps) {
        jdbcTemplate.update("""
                INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, status, is_draft)
                VALUES (?, ?, ?, ?, 'active', FALSE)
                """, "cfc_" + projectId + "_" + (channel == null ? "all" : channel), projectId, channel, bps);
    }

    private void setChannelFeeFixed(String projectId, String channel, Long fixedAmountCent) {
        jdbcTemplate.update("""
                INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, fixed_amount_cent, status, is_draft)
                VALUES (?, ?, ?, 0, ?, 'active', FALSE)
                """, "cfcf_" + projectId + "_" + (channel == null ? "all" : channel), projectId, channel, fixedAmountCent);
    }

    private String seedTicketType() {
        String id = "tt_cff_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        ticketTypeIds.add(id);
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, '渠道费固定额测试票种', 'fixed_price', FALSE, 'active', now(), now())
                """, id);
        return id;
    }

    private String seedOrder(String ticketTypeId, String salesChannel) {
        String id = "ord_cff_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        orderIds.add(id);
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel,
                                           amount_cent, currency, pay_status, issue_status, status, created_at, updated_at)
                VALUES (?, ?, ?, 1, ?, 0, 'CNY', 'paid', 'issued', 'created', now(), now())
                """, id, "ORD-" + id, ticketTypeId, salesChannel);
        return id;
    }

    private void seedSettled(String projectId, String merchantId, long cent, String orderId,
                             String parentTicketId, String verificationId, int dayOfMonth) {
        insertTransaction(projectId, merchantId, cent, orderId, parentTicketId, verificationId,
                "verification_settled", 1, dayOfMonth, "st_cfs_");
    }

    private void seedReversed(String projectId, String merchantId, long cent, String orderId,
                              String parentTicketId, String verificationId, int dayOfMonth) {
        insertTransaction(projectId, merchantId, -cent, orderId, parentTicketId, verificationId,
                "verification_reversed", -1, dayOfMonth, "st_cfr_");
    }

    private void insertTransaction(String projectId, String merchantId, long cent, String orderId,
                                   String parentTicketId, String verificationId, String type,
                                   int verifiedCount, int dayOfMonth, String idPrefix) {
        String id = idPrefix + java.util.UUID.randomUUID().toString().substring(0, 12);
        java.sql.Timestamp ts = java.sql.Timestamp.from(
                PERIOD_START.plusDays(dayOfMonth - 1L).atStartOfDay().toInstant(ZoneOffset.UTC));
        com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, """
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    order_id, parent_ticket_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,
                    merchant_id, project_id, verification_id, settlement_status, entitlement_valid_to,
                    business_at, discount_amount_cent, created_at, updated_at, comp_service_fee_cent)
                VALUES (?, ?, ?, ?, ?, ?, 'pass', ?, 1, ?, ?, ?, 0, 'merchant', ?, ?, ?, 'draft', ?, ?, 0, ?, ?, 0)
                """,
                id, "TN-" + id, type, "ent_" + id, orderId, parentTicketId, verifiedCount,
                verifiedCount, cent, cent, merchantId, projectId, verificationId, ts, ts, ts, ts);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private Map<String, Long> itemAmounts(String projectId) {
        Map<String, Long> out = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT si.merchant_id, si.final_amount_cent
                  FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? ORDER BY si.merchant_id
                """, rs -> {
            out.put(rs.getString("merchant_id"), rs.getLong("final_amount_cent"));
        }, projectId);
        return out;
    }

    private Map<String, Map<String, Object>> formatShares(String billId) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT * FROM settlement_bill_format_shares WHERE bill_id = ? ORDER BY business_format", billId)) {
            out.put((String) row.get("business_format"), row);
        }
        return out;
    }
}
