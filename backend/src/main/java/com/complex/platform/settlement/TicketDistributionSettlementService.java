package com.complex.platform.settlement;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算重构批2-3：月度通票分账<b>计算引擎</b>（§5.0 全流程）。
 *
 * <p>算池 → 留存 → 各酒吧三方式分配 → 校验 → 写 draft 结算单/明细。
 * <b>不过账 ar_ap_items</b>（批2-4）；只落 settlement_bills / settlement_items（status=draft）。
 *
 * <p>月窗 = [月首 00:00 UTC, 次月首 00:00 UTC) 按 settlement_transactions.confirmed_at。
 * <b>与摸底不符（据实报告）</b>：批1 写 verification_settled 时未落 confirmed_at（NULL），
 * 故当前生产按 confirmed_at 过滤取不到已核销行——需批1 补写 confirmed_at 才有真实数据；
 * 本引擎按 PRD §5.0 指定口径（confirmed_at）实现，测试自造 confirmed_at 的种子行验证算法。
 *
 * <p><b>金额整数分无漂移</b>：所有比例乘除用 long 整数运算；分配后余数（因整除截断）保留在
 * 「各酒吧总池 − Σ分账」里，不强行补给某家（余数天然沉淀为项目未分配，符合「Σ分账≤总池」校验）。
 */
@Service
public class TicketDistributionSettlementService {

    static final String METHOD_BY_RANK_TIER = "by_rank_tier";
    static final String METHOD_BY_VENUE_PER_GUEST = "by_venue_per_guest";
    static final String METHOD_BY_FIXED_AMOUNT = "by_fixed_amount";

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String ITEM_STATUS_DRAFT = "draft";

    private final JdbcSettlementBillRepository repository;

    public TicketDistributionSettlementService(JdbcSettlementBillRepository repository) {
        this.repository = repository;
    }

    /**
     * 结算某项目某月的通票分账，写 draft 结算单+明细，幂等重跑覆盖。
     *
     * @param projectId 项目公司（综合体）；null=平台默认口径（COALESCE 到 __DEFAULT__）
     * @param month     结算月（如 2026-06）
     * @return 落地的结算单 id
     */
    @Transactional
    public String settleMonth(String projectId, YearMonth month) {
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();
        Instant windowStart = periodStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant windowEnd = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        // ① 通票收入(已核销) = Σ verified_amount_cent(全部权益)
        long ticketRevenue = repository.sumVerifiedRevenueCent(projectId, windowStart, windowEnd);

        // ② 赠酒成本 = Σ gift_drink 核销杯数 × 每杯成本
        long giftCups = repository.sumGiftDrinkVerifiedCount(projectId, windowStart, windowEnd);
        long giveawayCost = giftCups * repository.findGiveawayCostPerCupCent(projectId);

        // ③ 渠道费:现状无费率数据源 → 先取 0
        // TODO 渠道费明细来源待第三方渠道费率接入补
        long channelFee = 0L;

        // ④ 可分配池 = 收入 − 渠道费 − 赠酒成本(下限 0)
        long availablePool = Math.max(0L, ticketRevenue - channelFee - giveawayCost);

        // ⑤ 项目留存 = 池 × retention_bps / 10000(整数分);各酒吧总池 = 池 − 留存
        int retentionBps = repository.findRetentionBps(projectId);
        long retention = availablePool * retentionBps / 10000;
        long spvPool = availablePool - retention;

        // ⑥ 各酒吧当月统计(核销数降序)
        List<JdbcSettlementBillRepository.MerchantMonthlyStat> stats =
                repository.aggregateMerchantStats(projectId, windowStart, windowEnd);

        // ⑦ 按 method 分账
        String method = repository.findDistributionMethod(projectId);
        List<ItemCalc> items = distribute(projectId, method, spvPool, stats);

        long distributed = items.stream().mapToLong(ItemCalc::amount).sum();

        // ⑧ 校验 Σ分账 ≤ 各酒吧总池(§5.0 步骤⑧):
        //    超出 → status 保持 draft 待人工特批(special_approval_ref=null 待填),不自动确认;
        //    未超 → status=confirmed(可确认;过账仍在批2-4,confirmed 只表引擎校验通过)。
        boolean overPool = distributed > spvPool;
        String status = overPool ? STATUS_DRAFT : STATUS_CONFIRMED;
        String specialApprovalRef = null; // 越池时留 null 待人工填;未越池时也 null(无需特批)

        String billId = idFor(projectId, month);
        repository.upsertBill(new SettlementBillRecord(
                billId, projectId, periodStart, periodEnd,
                ticketRevenue, channelFee, giveawayCost, availablePool,
                retentionBps, retention, spvPool, distributed,
                method, specialApprovalRef, status));

        // 幂等重跑:先清旧明细再写(明细集合随当月核销变化,覆盖式最简)
        String actualBillId = repository.findBillId(projectId, periodStart, periodEnd);
        repository.deleteItemsByBill(actualBillId);
        for (ItemCalc item : items) {
            repository.upsertItem(new SettlementItemRecord(
                    "si_" + actualBillId + "_" + item.merchantId(),
                    actualBillId, item.merchantId(),
                    item.verifiedCount(), item.guestCount(), item.rankNo(),
                    method, item.amount(), item.amount(), ITEM_STATUS_DRAFT));
        }
        return actualBillId;
    }

    /** 按分配方式算各酒吧分账（整数分）。 */
    private List<ItemCalc> distribute(String projectId, String method, long spvPool,
                                      List<JdbcSettlementBillRepository.MerchantMonthlyStat> stats) {
        List<ItemCalc> out = new ArrayList<>();
        switch (method) {
            case METHOD_BY_RANK_TIER -> {
                List<JdbcSettlementBillRepository.RankTier> tiers = repository.findRankTiers(projectId);
                // stats 已按 verified_count DESC 排好 → 顺位即排名(1-based)
                int rank = 0;
                for (var s : stats) {
                    rank++;
                    int shareBps = shareBpsForRank(tiers, rank);
                    long amount = spvPool * shareBps / 10000;
                    out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), rank, amount));
                }
            }
            case METHOD_BY_VENUE_PER_GUEST -> {
                for (var s : stats) {
                    var rate = repository.findMerchantRate(projectId, s.merchantId());
                    long perGuest = rate != null && rate.perGuestCent() != null ? rate.perGuestCent() : 0L;
                    long amount = (long) s.guestCount() * perGuest;
                    out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), null, amount));
                }
            }
            case METHOD_BY_FIXED_AMOUNT -> {
                for (var s : stats) {
                    var rate = repository.findMerchantRate(projectId, s.merchantId());
                    long fixed = rate != null && rate.fixedAmountCent() != null ? rate.fixedAmountCent() : 0L;
                    out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), null, fixed));
                }
            }
            default -> {
                // 未知方式:不分账(全沉淀总池),防御性兜底
            }
        }
        return out;
    }

    /** 命中排名档位区间 [rank_from, rank_to] → share_bps;无命中 → 0（该家不分）。 */
    private int shareBpsForRank(List<JdbcSettlementBillRepository.RankTier> tiers, int rank) {
        for (var t : tiers) {
            if (rank >= t.rankFrom() && rank <= t.rankTo()) {
                return t.shareBps();
            }
        }
        return 0;
    }

    private String idFor(String projectId, YearMonth month) {
        return "sb_" + (projectId == null ? "default" : projectId) + "_" + month;
    }

    /** 分账中间结果。 */
    private record ItemCalc(String merchantId, int verifiedCount, int guestCount, Integer rankNo, long amount) {
    }
}
