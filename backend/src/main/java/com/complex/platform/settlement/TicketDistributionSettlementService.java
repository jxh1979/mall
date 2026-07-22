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
        // D7(对抗复核 risk2):已超池特批的 confirmed 单视同冻结——special_approval_ref 是对「该次超池特定金额」的批准,
        //   重跑会改分配/超额额、使批准额与实际过账额脱钩且无二次审批门。故命中已特批单直接返回现有 id、不重算(保住批准快照);
        //   如需重算须先撤批(清 ref)。月度作业据此天然跳过(不抛异常、不 crash 批次),迟到核销并入须走人工撤批重算。
        if ("confirmed".equals(existingStatus)
                && repository.findBillSpecialApprovalRefByPeriod(projectId, periodStart, periodEnd) != null) {
            return repository.findBillId(projectId, periodStart, periodEnd);
        }
        Instant windowStart = periodStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant windowEnd = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        // D6 套票折扣归属(§7.4)：项目级配置（默认 platform_absorb）。折扣额已在核销时逐笔落 discount_amount_cent（append-only 账本），
        //   本引擎按 discount_bearer 应用——项目承担部分让商户拿满额、并记入 bill.combo_discount_cent。无折扣(折扣额=0)时全程 +0=零行为变更。
        var discountCfg = repository.findComboDiscountConfig(projectId);
        // 非专属(进池)折扣合计；platform_absorb/split_by_ratio 时把项目承担部分加进池基数(池按满额分)，merchant_absorb 时=0(池按净额分,现状)。
        long pooledDiscount = repository.sumComboDiscountCent(projectId, windowStart, windowEnd);
        long pooledPlatformBorne = platformBorneDiscount(discountCfg, pooledDiscount);

        // ① 通票收入(已核销) = Σ verified_amount_cent(全部权益)。D6：项目承担的进池折扣加进基数，商户按满额分(无折扣时 +0=现状)。
        long ticketRevenue = repository.sumVerifiedRevenueCent(projectId, windowStart, windowEnd) + pooledPlatformBorne;
        // 本月项目公司实际承担的折扣入账（池承担部分 + 后续专属直付承担部分累加）。
        long comboDiscountCent = pooledPlatformBorne;

        // ② 赠酒成本不在此扣(D5,2026-07-21 用户拍板删):通票免费酒水由 bar 域 BarGiftConsumer 每次核销按【内部结算价】
        //    给出品酒吧挂应付(pass_drink_settlement,幂等键 verificationId,可冲正)=唯一补偿口径;原「每杯 3 元池扣」
        //    是财务测算假设(PRD §3.7 假设不进系统)且与该内部结算价应付重复=对同一杯赠酒双重计,已删,不再从池扣减。

        // ③ 渠道费:率可配(channel_fee_configs,默认 0),消灭写死常量(PRD §5.0 步骤②「先扣第三方渠道抽成」)。
        //    实扣 = 率 × 第三方渠道实际交易额;渠道交易额数据源属批6(第三方对接),现无 → channelRevenue=0 → 实扣仍 0(保值,迁移前后生效值不变)。
        int channelFeeBps = repository.findChannelFeeBps(projectId);
        long channelRevenueCent = 0L; // TODO(批6): 第三方渠道流水接入后填该项目当月实际经第三方渠道成交额
        long channelFee = channelRevenueCent * channelFeeBps / 10000L;

        // ④ 可分配池 = 收入 − 渠道费(下限 0)。赠酒成本不在此扣(见 ②)。
        long availablePool = Math.max(0L, ticketRevenue - channelFee);

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
        // V336 综合服务费月度封顶(用户 2026-07-21 拍板 A「按费率规则各自封」):费率规则可跨票种(商户级规则
        //   entitlement_type=NULL)或跨商户(全局默认 merchant_id=NULL),故封顶【不能】逐 (商户×票种) 聚合行各自压——
        //   否则同商户多票种落同一条规则时会被压 N 次=N×封顶,违背「商户级规则=该商户当月封顶」。改为按【商户 × 解析到的规则 id】
        //   把封顶前原始费聚合后再压:商户级规则=该商户当月封顶、票种级规则=该票种当月封顶、全局规则=各商户各自当月封顶(不跨商户合并)。
        //   两趟:①逐聚合行累计 商户毛额 + (商户,规则)原始费/cap ②各组封顶 → 商户净额一条 exclusive_full 明细。
        java.util.LinkedHashMap<String, long[]> merchantAgg = new java.util.LinkedHashMap<>();
        // merchantAgg[mid] = {grossSum, verifiedCount, guestCount}
        java.util.LinkedHashMap<String, java.util.LinkedHashMap<String, long[]>> feeByRule = new java.util.LinkedHashMap<>();
        // feeByRule[mid][ruleId] = {rawFeeSum(封顶前), cap(-1=不封顶)}
        for (var ex : exclusives) {
            var svcFee = repository.findComprehensiveServiceFee(projectId, ex.merchantId(), ex.entitlementType());
            // D6：项目按归属承担的折扣加回该商户毛额，使其拿满额(platform_absorb 全额/split_by_ratio 平台份额/merchant_absorb=0)。
            //   综合服务费在此毛额上叠加(顺序不冲突)；折扣=0 时 gross=revenue → 与 D6 前逐分一致(零行为变更)。
            long platformBorne = platformBorneDiscount(discountCfg, ex.discountCent());
            long gross = ex.revenueCent() + platformBorne;
            comboDiscountCent += platformBorne; // 项目承担的专属折扣入账
            // 封顶前原始费:固定额=每张票固定额 × 核销张数(§7.2「所有门票都过一道」·用户 A 口径,非整包价),上限本笔毛额(防净额为负);
            //   否则比例(整数分截断余数留商户,不多扣)。默认 比例0 → rawFee=0(保值)。
            long rawFee = svcFee.fixedAmountCent() != null
                    ? Math.min(svcFee.fixedAmountCent() * ex.verifiedCount(), gross)
                    : gross * svcFee.feeBps() / 10000;
            long[] mg = merchantAgg.computeIfAbsent(ex.merchantId(), k -> new long[3]);
            mg[0] += gross;
            mg[1] += ex.verifiedCount();
            mg[2] += ex.guestCount();
            long[] rule = feeByRule.computeIfAbsent(ex.merchantId(), k -> new java.util.LinkedHashMap<>())
                    .computeIfAbsent(svcFee.id(), k -> new long[]{0L, -1L});
            rule[0] += rawFee;
            rule[1] = svcFee.monthlyCapAmountCent() == null ? -1L : svcFee.monthlyCapAmountCent();
        }
        // 各「商户×规则」组封顶(cap≥0 则 min),汇成每商户当月总费 + comp 服务费合计。
        // 🔴🔴 过账激活前必修(对抗审计 2026-07-21):封顶 min(raw,cap) 与固定额毛额钳 min(fixed×张数,gross)
        //   都是【月度非线性】运算,不满足核销↔冲正的「带符号线性可加」(f(-x)=-f(x))。当过账开启(settlement.monthly.
        //   posting.enabled=true)后,原核销月已过账时其跨月红冲落【当月】(business_at=now)、聚合行 gross 带负,
        //   本处按费率/毛额【重算】fee 而非按原月实收 fee 取反 → 触顶场景下红冲月退的 fee > 原月实收 fee(项目倒亏、
        //   商户多拿),且 net<0 撞 V224 chk_si_amount 致当月 settleMonth 崩。
        //   当前生产【过账 gated off】(账单永停 draft/confirmed,红冲总落原月同月净掉)→ 不可触发,故本潜伏 bug 不影响
        //   在服口径;但「激活首落账本」前【必须】先改:exclusive 服务费线也要按 verificationId 溯源原核销实收 fee、
        //   红冲严格取原行实收之反(不可变红冲事实),而非红冲月按费率/毛额重算。固定额模式此问题自 B1-2 起潜伏、封顶叠加放大。
        //   追踪见 PRD settlement-rebuild §7.2「🔴 过账激活前必修」清单;与折扣侧 V335 不同(去钳线性化即可),封顶天生非线性无法线性化,须架构级改(存每笔实收 fee)。
        long compServiceFee = 0L;
        java.util.HashMap<String, Long> feeByMerchant = new java.util.HashMap<>();
        for (var me : feeByRule.entrySet()) {
            long merchantFee = 0L;
            for (long[] rule : me.getValue().values()) {
                long raw = rule[0];
                long cap = rule[1];
                merchantFee += cap < 0 ? raw : Math.min(raw, cap); // 封顶只压不抬(负 gross 红冲行的非守恒隐患见上,过账开启前修)
            }
            feeByMerchant.put(me.getKey(), merchantFee);
            compServiceFee += merchantFee;
        }
        // 商户净额 = 毛额合计 − 封顶后总费;各商户一条 exclusive_full 直付明细(同商户多票种合并)。
        for (var e : merchantAgg.entrySet()) {
            long[] mg = e.getValue();
            long net = mg[0] - feeByMerchant.getOrDefault(e.getKey(), 0L);
            items.add(new ItemCalc(e.getKey(), (int) mg[1], (int) mg[2], null, net, METHOD_EXCLUSIVE_FULL));
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
                ticketRevenue, channelFee, 0L, availablePool,   // giveaway_cost_cent 恒 0:赠酒补偿走 bar 域内部结算价应付、不占分账池(D5)
                retentionBps, retention, spvPool, distributed, compServiceFee,
                method, specialApprovalRef, status, attributedEntityId, comboDiscountCent));

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

    /**
     * D6 套票折扣按归属拆出「项目公司承担」的部分(§7.4)：
     * platform_absorb → 项目全额承担(商户拿满额)；split_by_ratio → 项目按 split_platform_bps 承担；
     * merchant_absorb → 项目 0(商户自担，池/直付按净额=现状)。
     *
     * <p>🔴 <b>带符号(不钳0)</b>:核销为正(平台吸收折扣、商户按满额分),该核销在<b>后续已过账月</b>被冲正时为负
     * (平台<b>追回</b>先前吸收的折扣)——与收入侧 verified_amount_cent 带符号跨月净掉<b>一致</b>(V335 放开 combo_discount ≥0)。
     * 旧「discountCent&lt;=0 钳0」会丢弃跨月负向追回 → 收入 -R 追回了、折扣 -D 没追回,项目多付(套票真上线前必修)。
     * Java 整除截断向零使 {@code f(-x)=-f(x)},split_by_ratio 的正负份额精确对消;折扣=0 → 0(无折扣零行为变更)。
     */
    private long platformBorneDiscount(JdbcSettlementBillRepository.ComboDiscountConfig cfg, long discountCent) {
        return switch (cfg.discountBearer()) {
            case "platform_absorb" -> discountCent;
            case "split_by_ratio" -> discountCent * cfg.splitPlatformBps() / 10000;
            default -> 0L; // merchant_absorb（及未知值防御）：项目不承担
        };
    }

    /** 分账中间结果（method：池分账=当月分配方式，专属直付=exclusive_full）。 */
    private record ItemCalc(String merchantId, int verifiedCount, int guestCount, Integer rankNo, long amount,
                            String method) {
    }
}
