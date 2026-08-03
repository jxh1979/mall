package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * S11「联合／综合通票业态份额拆分」验收（V366）。
 *
 * <p>业务上这一刀回答的是：<b>一张联合通票收到的钱，游乐、餐饮、零售、酒吧各自该分多少</b>。
 * 从前系统把这笔钱整个当成酒吧的收入进池分给各家酒吧——游乐公司和餐厅的那份也在里面，
 * 分完之后没人能从账上看出来错在哪。本类要证明四件事：
 *
 * <ol>
 *   <li><b>K9 逐分不变</b>：一张只有酒吧权益的通票（酒吧占比 100%），结算单<b>每一列</b>与改造前一模一样；</li>
 *   <li><b>K20 拆份额守恒</b>：Σ各业态净份额 + 整单渠道费 ≡ 通票收入；只有酒吧那份进池，
 *       游乐/餐饮的份额不进池、也不参与酒吧的排名与客数；门票综合服务费<b>只从酒吧那份扣</b>
 *       （HTML §「门票综合服务费」：只适用于酒吧相关票务，纯游乐票／纯游乐通票／演出票不收）；</li>
 *   <li><b>渠道抽成整单前移</b>：渠道拿的是整笔售价的成，各业态按份额一起担，
 *       不再由酒吧一家替全场扛；费率 0（官方直销）时逐分不变；</li>
 *   <li><b>折扣补回口径扩到所有业态</b>：平台承担折扣时每个业态都按满额算，不是只补酒吧那一路。</li>
 * </ol>
 *
 * <p>非 @Transactional（settleMonth 内部真提交）；@AfterEach 按项目清理；每例独立 projectId。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementFormatShareS11Tests {

    private static final YearMonth MONTH = YearMonth.of(2015, 5); // 远离今日的确定月
    private static final LocalDate PERIOD_START = MONTH.atDay(1);
    private static final LocalDate PERIOD_END = MONTH.atEndOfMonth();

    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private JdbcSettlementBillRepository billRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<String> projectIds = new ArrayList<>();
    private final List<String> merchantIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String pid : projectIds) {
            // V344 起 settlement_transactions 是 append-only 硬强制表（触发器拒 UPDATE/DELETE），清场须走测试专用旁路。
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
            jdbcTemplate.update("DELETE FROM ticket_combo_discount_configs WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM revenue_stream_attribution WHERE project_id = ?", pid);
        }
        for (String mid : merchantIds) {
            jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", mid);
        }
        projectIds.clear();
        merchantIds.clear();
    }

    // ---------------------------------------------------------------- ① K9 逐分不变

    /**
     * K9：一张只有酒吧权益的通票 —— 酒吧业态占比 100%，结算单<b>每一列</b>与拆份额之前逐分一致。
     *
     * <p>这是本刀「不动老账」的正面证明：综合体今天卖的绝大多数还是纯酒吧通票，
     * 财务这个月拿到的结算单不能因为系统多了一层业态拆分就变个数字。
     * 断言方式是把结算单整行 {@code SELECT *} 转成 Map、与手算的<b>全部列</b>逐列比对，
     * 不是挑几列看看——挑几列看，恰好没挑的那列错了就漏过去了。
     * (S4 起综合服务费两支/收款主体快照列并入本全列比对;每笔费按 20% 等价保值口径逐笔计提。)
     */
    @Test
    void barOnlyPassKeepsEveryBillColumnPennyIdentical() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 3333);
        seedMerchant("m_bar_a", "bar");
        seedMerchant("m_bar_b", "bar");
        seedMerchant("m_bar_c", "bar");
        // 每笔核销带按 20% HALF_UP 计提的池链综合服务费:8000 / 6001 / 5998(计提时逐笔舍入)。
        seedVerifiedWithFee(pid, "m_bar_a", 3, 2, 40001L, 8000L);
        seedVerifiedWithFee(pid, "m_bar_b", 2, 1, 30007L, 6001L);
        seedVerifiedWithFee(pid, "m_bar_c", 1, 1, 29991L, 5998L);

        String billId = settlementService.settleMonth(pid, MONTH);

        // 手算：收入 99999 → 渠道费 0 → 池链综合服务费 Σ=19999 → 可分配池 80000；
        //   S8 第二段归一化分尽:三家权重各 3333(Σ=9999) → 各 round(80000×3333/9999)=26667,
        //   Σ=80001;应分总额=池 80000 → 尾差 −1 单列(平台承担的追回),无未分余额。
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("id", billId);
        expected.put("project_id", pid);
        expected.put("period_start", java.sql.Date.valueOf(PERIOD_START));
        expected.put("period_end", java.sql.Date.valueOf(PERIOD_END));
        expected.put("ticket_revenue_cent", 99999L);
        expected.put("channel_fee_cent", 0L);
        expected.put("available_pool_cent", 80000L);
        expected.put("distributed_cent", 80001L);
        expected.put("comp_service_fee_pooled_cent", 19999L);
        expected.put("comp_service_fee_exclusive_cent", 0L);
        expected.put("fee_beneficiary_entity_id", "be_ent_platform"); // V370 全局归属种子:归娱乐平台公司
        expected.put("gift_cost_cent", 0L); // S6:未配非零 G 规则(种子 by_ticket_amount+0)恒 0=逐分不变
        expected.put("gift_beneficiary_entity_id", "be_ent_platform"); // V372 归属种子:归娱乐平台公司
        expected.put("distribution_method", "by_rank_tier");
        expected.put("special_approval_ref", null);
        expected.put("special_approval_valid_until", null);
        expected.put("status", "confirmed");
        expected.put("confirmed_at", null);
        expected.put("settled_at", null);
        expected.put("attributed_entity_id", "be_ent_platform"); // V375 all_pass_ticket 全局种子
        expected.put("combo_discount_cent", 0L);
        expected.put("rounding_residual_cent", -1L);
        expected.put("pool_undistributed_cent", 0L);        // S8:第二段有参与者 → 恒 0
        expected.put("pool_undistributed_entity_id", null);

        Map<String, Object> actual = new HashMap<>(bill(pid));
        actual.remove("created_at");   // 落库时刻，非业务口径
        actual.remove("updated_at");
        assertThat(actual)
                .as("单业态票（只有酒吧）→ 酒吧占比 100% → 结算单每一列逐分不变")
                .isEqualTo(expected);

        // 业态份额只有酒吧一行，净份额(毛额−渠道费−综合服务费)恰是可分配池。
        Map<String, Map<String, Object>> shares = formatShares(billId);
        assertThat(shares.keySet()).containsExactly("bar");
        assertThat(shares.get("bar").get("gross_revenue_cent")).isEqualTo(99999L);
        assertThat(shares.get("bar").get("channel_fee_cent")).isEqualTo(0L);
        assertThat(shares.get("bar").get("comp_service_fee_cent")).isEqualTo(19999L);
        assertThat(shares.get("bar").get("net_revenue_cent")).isEqualTo(80000L);
        assertThat(shares.get("bar").get("beneficiary_kind")).isEqualTo("entity");
    }

    // ---------------------------------------------------------------- ② K20 拆份额

    /**
     * K20：联合通票按业态拆份额 —— 只有酒吧那份进池；游乐/餐饮的份额既不进池，也不占酒吧的分池名次。
     *
     * <p>这条最要紧的是<b>排名</b>：从前一家餐厅核销几张餐券，就能在「按核销数排名分池」里占掉一个名次，
     * 把本该属于某家酒吧的档位挤掉——钱分错了人，而且分完的账面看着完全正常。
     */
    @Test
    void onlyBarShareEntersPoolAndOnlyBarsAreRanked() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 5000);
        seedMerchant("m_fmt_bar1", "bar");
        seedMerchant("m_fmt_bar2", "bar");
        seedMerchant("m_fmt_amu", "amusement");
        seedMerchant("m_fmt_cat", "catering");
        // 综合服务费【只适用于酒吧相关票务】(HTML §「门票综合服务费」):酒吧 12000+8000;
        //   游乐/餐饮那两份一分不抽(纯游乐票、演出票不向娱乐平台公司付这道费)。
        seedVerifiedWithFee(pid, "m_fmt_bar1", 6, 6, 60000L, 12000L);
        seedVerifiedWithFee(pid, "m_fmt_bar2", 4, 4, 40000L, 8000L);
        seedVerifiedWithFee(pid, "m_fmt_amu", 9, 9, 30000L, 0L);   // 核销数最多，从前会抢到第 1 名
        seedVerifiedWithFee(pid, "m_fmt_cat", 8, 8, 10000L, 0L);

        String billId = settlementService.settleMonth(pid, MONTH);
        Map<String, Object> b = bill(pid);

        // 通票收入是整张票的钱（各业态毛额之和），可分配池只拿酒吧那一份(扣完酒吧侧服务费)。
        assertThat(b.get("ticket_revenue_cent")).isEqualTo(140000L);
        assertThat(b.get("comp_service_fee_pooled_cent"))
                .as("池链服务费 = 只有酒吧业态计提(游乐/餐饮不收这道费)").isEqualTo(20000L);
        assertThat(b.get("available_pool_cent")).as("池 = 酒吧份额 − 酒吧侧服务费,不含游乐/餐饮").isEqualTo(80000L);

        // 分池明细里只有两家酒吧，各 50%。
        Map<String, Long> items = itemAmounts(pid);
        assertThat(items).containsOnlyKeys("m_fmt_bar1", "m_fmt_bar2");
        assertThat(items.values()).containsExactly(40000L, 40000L);

        // 业态份额三行，归属各就各位；K20：Σ净份额 + 渠道费 + 综合服务费 = 通票收入。
        Map<String, Map<String, Object>> shares = formatShares(billId);
        assertThat(shares.keySet()).containsExactlyInAnyOrder("bar", "amusement", "catering");
        assertThat(shares.get("bar").get("comp_service_fee_cent")).isEqualTo(20000L);
        assertThat(shares.get("bar").get("net_revenue_cent")).isEqualTo(80000L);
        assertThat(shares.get("amusement").get("comp_service_fee_cent"))
                .as("纯游乐份额不收门票综合服务费").isEqualTo(0L);
        assertThat(shares.get("amusement").get("net_revenue_cent")).isEqualTo(30000L);
        assertThat(shares.get("catering").get("comp_service_fee_cent"))
                .as("餐饮份额不收门票综合服务费").isEqualTo(0L);
        assertThat(shares.get("catering").get("net_revenue_cent")).isEqualTo(10000L);
        assertThat(shares.get("amusement").get("beneficiary_kind")).as("游乐份额归游乐公司这个主体").isEqualTo("entity");
        assertThat(shares.get("amusement").get("beneficiary_entity_id"))
                .as("V375 全局种子:amusement_ticket 流 → 游乐公司").isEqualTo("be_amusement");
        assertThat(shares.get("catering").get("beneficiary_kind")).as("餐饮份额归提供这份餐的商户自己").isEqualTo("per_merchant");
        assertThat(shares.get("catering").get("beneficiary_entity_id")).isNull();
        assertThat(sumOf(shares, "net_revenue_cent") + (Long) b.get("channel_fee_cent")
                + (Long) b.get("comp_service_fee_pooled_cent"))
                .as("K20:Σ各业态净份额 + 整单渠道费 + 池链综合服务费 ≡ 通票收入")
                .isEqualTo(b.get("ticket_revenue_cent"));
    }

    // ---------------------------------------------------------------- ③ 渠道抽成整单前移

    /**
     * 渠道抽成按<b>整单</b>算、各业态按份额一起担 —— 不再由酒吧一家替游乐和餐厅把渠道费垫了。
     *
     * <p>同一批数据：渠道费 10%。整单 140000 收 14000，游乐担 3000、餐饮担 1000、酒吧担 10000。
     * 从前的算法是「(全部收入 − 全部渠道费) 全进酒吧池」，酒吧既拿了别人的钱、又替别人交了费；
     * 现在酒吧只拿自己的 100000、只担自己的 10000。
     */
    @Test
    void channelFeeIsChargedOnWholeOrderAndBorneByEveryFormat() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 5000);
        setChannelFeeBps(pid, null, 1000);
        seedMerchant("m_ch_bar1", "bar");
        seedMerchant("m_ch_bar2", "bar");
        seedMerchant("m_ch_amu", "amusement");
        seedMerchant("m_ch_cat", "catering");
        seedVerifiedWithFee(pid, "m_ch_bar1", 6, 6, 60000L, 12000L);
        seedVerifiedWithFee(pid, "m_ch_bar2", 4, 4, 40000L, 8000L);
        seedVerifiedWithFee(pid, "m_ch_amu", 3, 3, 30000L, 0L);  // 游乐份额不收综合服务费
        seedVerifiedWithFee(pid, "m_ch_cat", 1, 1, 10000L, 0L);  // 餐饮份额不收综合服务费

        String billId = settlementService.settleMonth(pid, MONTH);
        Map<String, Object> b = bill(pid);
        Map<String, Map<String, Object>> shares = formatShares(billId);

        assertThat(b.get("ticket_revenue_cent")).isEqualTo(140000L);
        assertThat(b.get("channel_fee_cent")).as("渠道费按整单售价算，不是只按酒吧那一份算").isEqualTo(14000L);
        assertThat(shares.get("amusement").get("channel_fee_cent")).isEqualTo(3000L);
        assertThat(shares.get("catering").get("channel_fee_cent")).isEqualTo(1000L);
        assertThat(shares.get("bar").get("channel_fee_cent")).as("酒吧只担自己那一份渠道费").isEqualTo(10000L);
        assertThat(sumOf(shares, "channel_fee_cent")).isEqualTo(14000L);
        // 池 = 酒吧毛额 100000 − 酒吧渠道费 10000 − 酒吧侧综合服务费 20000 = 70000。
        assertThat(b.get("available_pool_cent")).isEqualTo(70000L);
        assertThat(itemAmounts(pid).values()).containsExactly(35000L, 35000L);
    }

    /** 渠道费率按【订单销售渠道】解析：某渠道配了专属率就用专属率，没配的落回通用率（默认 0＝官方直销不抽）。 */
    @Test
    void channelFeeRateResolvesPerSalesChannelAndFallsBackToGeneric() {
        String pid = newProject();
        setChannelFeeBps(pid, null, 200);        // 本项目通用率 2%
        setChannelFeeBps(pid, "meituan", 800);   // 美团专属 8%

        assertThat(billRepository.findChannelFee(pid, "meituan").feeBps()).isEqualTo(800);
        assertThat(billRepository.findChannelFee(pid, "miniapp").feeBps()).as("没单独配的渠道落回通用率").isEqualTo(200);
        assertThat(billRepository.findChannelFee(pid, null).feeBps()).isEqualTo(200);
        assertThat(billRepository.findChannelFee(null, "meituan").feeBps()).as("别的项目不受本项目费率影响").isZero();
    }

    // ---------------------------------------------------------------- ④ 折扣补回扩到所有业态

    /**
     * §20.4：平台承担折扣时，<b>每个业态</b>都按满额算 —— 不能酒吧按满额、游乐餐饮按折后，
     * 那等于同一张票里各家用两套口径分钱，谁吃亏谁占便宜全看自己是什么业态。
     */
    @Test
    void platformBorneDiscountIsAddedBackToEveryFormat() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 10000);
        seedMerchant("m_dis_bar", "bar");
        seedMerchant("m_dis_amu", "amusement");
        seedVerifiedWithDiscount(pid, "m_dis_bar", 1, 1, 80000L, 20000L);
        seedVerifiedWithDiscount(pid, "m_dis_amu", 1, 1, 30000L, 5000L);

        String billId = settlementService.settleMonth(pid, MONTH);
        Map<String, Object> b = bill(pid);
        Map<String, Map<String, Object>> shares = formatShares(billId);

        // 默认口径 platform_absorb：项目公司把折扣补上，两个业态一视同仁地按满额分。
        assertThat(shares.get("bar").get("platform_borne_discount_cent")).isEqualTo(20000L);
        assertThat(shares.get("bar").get("gross_revenue_cent")).isEqualTo(100000L);
        assertThat(shares.get("amusement").get("platform_borne_discount_cent")).isEqualTo(5000L);
        assertThat(shares.get("amusement").get("gross_revenue_cent")).isEqualTo(35000L);
        assertThat(b.get("combo_discount_cent")).isEqualTo(25000L);
        assertThat(b.get("ticket_revenue_cent")).isEqualTo(135000L);
        assertThat(b.get("available_pool_cent")).as("池只拿酒吧那份的满额").isEqualTo(100000L);

        // 改成商户自担：两个业态一样都按折后净额，仍然是同一套口径。
        setDiscountBearer(pid, "merchant_absorb");
        settlementService.settleMonth(pid, MONTH);
        Map<String, Map<String, Object>> after = formatShares(billId);
        assertThat(after.get("bar").get("platform_borne_discount_cent")).isEqualTo(0L);
        assertThat(after.get("bar").get("gross_revenue_cent")).isEqualTo(80000L);
        assertThat(after.get("amusement").get("platform_borne_discount_cent")).isEqualTo(0L);
        assertThat(after.get("amusement").get("gross_revenue_cent")).isEqualTo(30000L);
        assertThat(bill(pid).get("combo_discount_cent")).isEqualTo(0L);
    }

    // ---------------------------------------------------------------- 幂等

    /** 幂等重跑：同月重结，业态份额覆盖式重写，不留上一次的残行。 */
    @Test
    void rerunRewritesFormatSharesWithoutLeavingStaleRows() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 5000);
        seedMerchant("m_re_bar", "bar");
        seedMerchant("m_re_cat", "catering");
        seedVerified(pid, "m_re_bar", 1, 1, 50000L);
        seedVerified(pid, "m_re_cat", 1, 1, 10000L);

        String billId = settlementService.settleMonth(pid, MONTH);
        assertThat(formatShares(billId).keySet()).containsExactlyInAnyOrder("bar", "catering");

        settlementService.settleMonth(pid, MONTH);
        Map<String, Map<String, Object>> again = formatShares(billId);
        assertThat(again.keySet()).containsExactlyInAnyOrder("bar", "catering");
        assertThat(again.get("bar").get("net_revenue_cent")).isEqualTo(50000L);
        assertThat(again.get("catering").get("net_revenue_cent")).isEqualTo(10000L);
    }

    // ---------------------------------------------------------------- 夹具

    private String newProject() {
        String pid = "prj_s11_" + java.util.UUID.randomUUID().toString().substring(0, 8);
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

    private void setDiscountBearer(String projectId, String bearer) {
        jdbcTemplate.update("""
                INSERT INTO ticket_combo_discount_configs (id, project_id, discount_bearer, split_platform_bps, status, is_draft)
                VALUES (?, ?, ?, 0, 'active', FALSE)
                ON CONFLICT (id) DO UPDATE SET discount_bearer = EXCLUDED.discount_bearer
                """, "tcdc_" + projectId, projectId, bearer);
    }

    private void seedVerified(String projectId, String merchantId, int verifiedCount, int numPeople, long cent) {
        seedVerifiedFull(projectId, merchantId, verifiedCount, numPeople, cent, 0L, 0L);
    }

    private void seedVerifiedWithFee(String projectId, String merchantId, int verifiedCount, int numPeople,
                                     long cent, long compServiceFeeCent) {
        seedVerifiedFull(projectId, merchantId, verifiedCount, numPeople, cent, 0L, compServiceFeeCent);
    }

    private void seedVerifiedWithDiscount(String projectId, String merchantId, int verifiedCount,
                                          int numPeople, long cent, long discountCent) {
        seedVerifiedFull(projectId, merchantId, verifiedCount, numPeople, cent, discountCent, 0L);
    }

    private void seedVerifiedFull(String projectId, String merchantId, int verifiedCount,
                                  int numPeople, long cent, long discountCent, long compServiceFeeCent) {
        String id = "st_s11_" + java.util.UUID.randomUUID().toString().substring(0, 12);
        java.sql.Timestamp ts = java.sql.Timestamp.from(PERIOD_START.plusDays(9).atStartOfDay().toInstant(ZoneOffset.UTC));
        com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, """
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, business_at,
                    discount_amount_cent, created_at, updated_at, comp_service_fee_cent)
                VALUES (?, ?, 'verification_settled', ?, 'pass', ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?,
                    'draft', ?, ?, ?, ?, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, verifiedCount, Math.max(verifiedCount, 1), numPeople,
                cent, cent, merchantId, projectId, "vf_" + id, ts, ts, discountCent, ts, ts, compServiceFeeCent);
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

    private long sumOf(Map<String, Map<String, Object>> shares, String column) {
        return shares.values().stream().mapToLong(r -> (Long) r.get(column)).sum();
    }
}
