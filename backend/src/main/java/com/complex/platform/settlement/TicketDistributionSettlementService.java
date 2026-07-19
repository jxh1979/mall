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
 * <p>月窗 = [月首 00:00 UTC, 次月首 00:00 UTC) 按 settlement_transactions.created_at
 * （批1 双写时 created_at 已填=核销记录创建≈核销日，语义正确；confirmed_at 是结算确认时间、
 * 双写时本应 NULL 故不用作月窗）。
 *
 * <p><b>金额整数分无漂移</b>：所有比例乘除用 long 整数运算；分配后余数（因整除截断）保留在
 * 「各酒吧总池 − Σ分账」里，不强行补给某家（余数天然沉淀为项目未分配，符合「Σ分账≤总池」校验）。
 */
@Service
public class TicketDistributionSettlementService {

    static final String METHOD_BY_RANK_TIER = "by_rank_tier";
    static final String METHOD_BY_VENUE_PER_GUEST = "by_venue_per_guest";
    static final String METHOD_BY_FIXED_AMOUNT = "by_fixed_amount";
    // 批3-4(§6.4)：专属某商户「不分摊」直付明细方式——全额归该商户，不扣留存、不进 spvPool、不参与排名。
    static final String METHOD_EXCLUSIVE_FULL = "exclusive_full";

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String ITEM_STATUS_DRAFT = "draft";

    // 六业态收入归属(terminology-canonical §三):通票收入流 → all_pass_ticket。
    static final String REVENUE_STREAM_ALL_PASS_TICKET = "all_pass_ticket";

    private final JdbcSettlementBillRepository repository;
    private final RevenueStreamAttributionRepository attributionRepository;

    public TicketDistributionSettlementService(JdbcSettlementBillRepository repository,
                                               RevenueStreamAttributionRepository attributionRepository) {
        this.repository = repository;
        this.attributionRepository = attributionRepository;
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

        // B-1 守卫(2026-07-06 第一性原理审计 + 独立复核):仅 draft/confirmed(未过账)可安全重算(幂等覆盖);
        //   settled/reversed 及将来 CHECK 新增的任何"已出账/终结"态一律 fail-closed 拒绝——重跑会 deleteItemsByBill
        //   删掉已挂应付(ar_ap_item_id 已置)的明细,产生孤儿 AR/AP。反向白名单跟随 schema,不必每加终态回来改;
        //   注:posted 是 settlement_items 的状态、bill 永不为之(V224 CHECK 只含 draft/confirmed/settled/reversed),故不单列。
        String existingStatus = repository.findBillStatusByPeriod(projectId, periodStart, periodEnd);
        if (existingStatus != null && !"draft".equals(existingStatus) && !"confirmed".equals(existingStatus)) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_ALREADY_POSTED",
                    "该月份结算单已过账/结算/冲正(状态=" + existingStatus + "),不可重结;如需重算请先对已过账明细做冲正");
        }
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

        // ⑦ 按 method 分账（仅非专属进池；池基/排名/客数聚合已在仓储层排除专属）
        String method = repository.findDistributionMethod(projectId);
        List<ItemCalc> items = distribute(projectId, method, spvPool, stats);

        // ⑦b 批3-4(§6.4)：专属某商户「不分摊」→ 每个专属商户一条全额直付明细（method=exclusive_full，
        //     不扣留存、不进 spvPool、不参与他家分池排名/客数聚合）。金额=该商户专属核销 Σverified 全额。
        // V268 综合服务费(§7.2 口径8+"所有门票都抽"):单独/专属门票直付前,项目公司先抽综合服务费(项目x商户x票种范围可配),
        //   商户拿【净额=收入-服务费】,服务费归项目(记入 bill.comp_service_fee_cent)。默认费率 0 -> 净额=全额 -> 零行为变更。
        //   聚合已按 (merchant,entitlement_type) 拆,故费率能按票种范围精确命中;同一商户多票种的净额合并成一条明细。
        List<JdbcSettlementBillRepository.ExclusiveMerchantRevenue> exclusives =
                repository.aggregateExclusiveRevenue(projectId, windowStart, windowEnd);
        java.util.LinkedHashMap<String, long[]> byMerchant = new java.util.LinkedHashMap<>();
        // byMerchant[mid] = {netAmount, verifiedCount, guestCount}
        long compServiceFee = 0L;
        for (var ex : exclusives) {
            int feeBps = repository.findComprehensiveServiceFeeBps(projectId, ex.merchantId(), ex.entitlementType());
            long fee = ex.revenueCent() * feeBps / 10000; // 整数分,截断余数留给商户(不多扣)
            long net = ex.revenueCent() - fee;
            compServiceFee += fee;
            long[] acc = byMerchant.computeIfAbsent(ex.merchantId(), k -> new long[3]);
            acc[0] += net;
            acc[1] += ex.verifiedCount();
            acc[2] += ex.guestCount();
        }
        for (var e : byMerchant.entrySet()) {
            long[] acc = e.getValue();
            items.add(new ItemCalc(e.getKey(), (int) acc[1], (int) acc[2], null, acc[0], METHOD_EXCLUSIVE_FULL));
        }

        // distributed = 池分账 + 专属直付（两口径并列写入同一结算单明细）。
        long distributed = items.stream().mapToLong(ItemCalc::amount).sum();

        // ⑧ 校验 Σ<b>池分账</b> ≤ 各酒吧总池(§5.0 步骤⑧);专属直付不占池、单独全额，故不纳入越池判定。
        //    超出 → status 保持 draft 待人工特批(special_approval_ref=null 待填),不自动确认;
        //    未超 → status=confirmed(可确认;过账仍在批2-4,confirmed 只表引擎校验通过)。
        long pooledDistributed = items.stream()
                .filter(i -> !METHOD_EXCLUSIVE_FULL.equals(i.method()))
                .mapToLong(ItemCalc::amount).sum();
        boolean overPool = pooledDistributed > spvPool;
        String status = overPool ? STATUS_DRAFT : STATUS_CONFIRMED;
        String specialApprovalRef = null; // 越池时留 null 待人工填;未越池时也 null(无需特批)

        // 六业态收入归属:通票收入归属的 L3 管理公司(娱乐运营平台),按 (项目, all_pass_ticket) 可配置解析;
        //   项目特例优先→全局默认→未配置返 null(仅记归属,不改任何金额)。
        String attributedEntityId =
                attributionRepository.resolveManagementEntity(projectId, REVENUE_STREAM_ALL_PASS_TICKET);

        String billId = idFor(projectId, month);
        repository.upsertBill(new SettlementBillRecord(
                billId, projectId, periodStart, periodEnd,
                ticketRevenue, channelFee, giveawayCost, availablePool,
                retentionBps, retention, spvPool, distributed, compServiceFee,
                method, specialApprovalRef, status, attributedEntityId));

        // 幂等重跑:先清旧明细再写(明细集合随当月核销变化,覆盖式最简)
        String actualBillId = repository.findBillId(projectId, periodStart, periodEnd);
        repository.deleteItemsByBill(actualBillId);
        for (ItemCalc item : items) {
            // 专属直付明细 id 加 method 后缀,与该家池分账明细 id 区分(否则 PK/唯一键相撞被覆盖丢账)。
            String itemId = METHOD_EXCLUSIVE_FULL.equals(item.method())
                    ? "si_" + actualBillId + "_" + item.merchantId() + "_excl"
                    : "si_" + actualBillId + "_" + item.merchantId();
            repository.upsertItem(new SettlementItemRecord(
                    itemId,
                    actualBillId, item.merchantId(),
                    item.verifiedCount(), item.guestCount(), item.rankNo(),
                    item.method(), item.amount(), item.amount(), ITEM_STATUS_DRAFT));
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
                    out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), rank, amount, method));
                }
            }
            case METHOD_BY_VENUE_PER_GUEST -> {
                for (var s : stats) {
                    var rate = repository.findMerchantRate(projectId, s.merchantId());
                    long perGuest = rate != null && rate.perGuestCent() != null ? rate.perGuestCent() : 0L;
                    long amount = (long) s.guestCount() * perGuest;
                    out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), null, amount, method));
                }
            }
            case METHOD_BY_FIXED_AMOUNT -> {
                for (var s : stats) {
                    var rate = repository.findMerchantRate(projectId, s.merchantId());
                    long fixed = rate != null && rate.fixedAmountCent() != null ? rate.fixedAmountCent() : 0L;
                    out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), null, fixed, method));
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

    /** 分账中间结果（method：池分账=当月分配方式，专属直付=exclusive_full）。 */
    private record ItemCalc(String merchantId, int verifiedCount, int guestCount, Integer rankNo, long amount,
                            String method) {
    }
}
