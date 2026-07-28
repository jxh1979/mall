package com.complex.platform.settlement;

import com.complex.platform.common.money.MoneyRounding;
import java.math.BigDecimal;
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
 * <p><b>金额精度与尾差（R-7 / D15.1，2026-07-22 用户拍板）</b>：一切按比例拆分一律
 * {@link MoneyRounding} <b>四舍五入到分（HALF_UP）</b>，不再用 {@code a*bps/10000} 整除截断
 * （旧写法系统性少给收费方且与「四舍五入」字面口径不符）。各家分别舍入后与「应分总额」的差额=<b>尾差</b>，
 * 归项目公司/平台兜底并<b>单列</b>落 {@code settlement_bills.rounding_residual_cent}（V343）留痕，
 * 绝不悄悄并进某一家、也不静默沉淀。守恒恒等式（{@link #verifyConservation} 运行期自检 + 集成测试断言）：
 * <ul>
 *   <li>{@code 通票收入 = 渠道费 + 项目留存 + 各酒吧总池}（池链无泄漏）</li>
 *   <li>{@code Σ池分账明细 + 尾差 = 应分总额}（分账链尾差单列后严格相等）</li>
 * </ul>
 * HALF_UP 对 0 对称（{@code f(-x) = -f(x)}），故跨月红冲取原行之反仍逐分对消，守恒不被舍入破坏。
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
        // S9 到期回收:冻结只认【仍在有效期内】的特批;到期后本单回到普通单待遇——照常重算,仍超池即打回 draft。
        if ("confirmed".equals(existingStatus)
                && repository.findActiveSpecialApprovalRefByPeriod(projectId, periodStart, periodEnd) != null) {
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
        // R-7:四舍五入到分(旧整除截断少扣渠道费)。收入侧取补数 → 无尾差(收入 = 渠道费 + 池)。
        long channelFee = MoneyRounding.bpsOf(channelRevenueCent, channelFeeBps);

        // ④ 可分配池 = 收入 − 渠道费(下限 0)。赠酒成本不在此扣(见 ②)。
        long availablePool = Math.max(0L, ticketRevenue - channelFee);

        // ⑤ 项目留存 = round_half_up(池 × retention_bps / 10000);各酒吧总池 = 池 − 留存(取补数 → 无尾差)。
        //    R-7:旧 `availablePool * retentionBps / 10000` 整除截断,留存恒少收、差额悄悄进各酒吧池。
        int retentionBps = repository.findRetentionBps(projectId);
        long retention = MoneyRounding.bpsOf(availablePool, retentionBps);
        long spvPool = availablePool - retention;

        // ⑥ 各酒吧当月统计(核销数降序)
        List<JdbcSettlementBillRepository.MerchantMonthlyStat> stats =
                repository.aggregateMerchantStats(projectId, windowStart, windowEnd);

        // ⑦ 按 method 分账（仅非专属进池；池基/排名/客数聚合已在仓储层排除专属）
        String method = repository.findDistributionMethod(projectId);
        PooledDistribution pooled = distribute(projectId, method, spvPool, stats);
        List<ItemCalc> items = new ArrayList<>(pooled.items());

        // ⑦b 批3-4(§6.4)：专属某商户「不分摊」→ 每个专属商户一条全额直付明细（method=exclusive_full，
        //     不扣留存、不进 spvPool、不参与他家分池排名/客数聚合）。金额=该商户专属核销 Σverified 全额。
        // 综合服务费:专属门户直付前先抽综合服务费,商户拿【净额=收入-服务费】(记入 bill.comp_service_fee_cent)。
        //   默认费率 0 -> 净额=全额 -> 零行为变更。
        //   S2(V365):费率规则的范围改成【项目 × 商户 × 票种】,且【每笔核销时】按票种解析并落库(P6 起本引擎只做线性求和,
        //   不再月末重算),故此处按 entitlement_type 分组只是聚合口径遗留、与费率命中无关(下面 merchantAgg 又按商户合并回去)。
        //   ⚠️ 本处仍只读【专属】那部分 fee;非专属(进池)那部分虽已在 S2 落进 settlement_transactions,
        //   但进主链扣减(F_pool)是 S4 —— 本刀不动引擎数值公式。
        List<JdbcSettlementBillRepository.ExclusiveMerchantRevenue> exclusives =
                repository.aggregateExclusiveRevenue(projectId, windowStart, windowEnd);
        // V336 综合服务费月度封顶(用户 2026-07-21 拍板 A「按费率规则各自封」):费率规则可跨票种(商户级规则
        //   ticket_type_id=NULL)或跨商户(全局默认 merchant_id=NULL),故封顶【不能】逐 (商户×票种) 聚合行各自压——
        //   否则同商户多票种落同一条规则时会被压 N 次=N×封顶,违背「商户级规则=该商户当月封顶」。改为按【商户 × 解析到的规则 id】
        //   把封顶前原始费聚合后再压:商户级规则=该商户当月封顶、票种级规则=该票种当月封顶、全局规则=各商户各自当月封顶(不跨商户合并)。
        //   两趟:①逐聚合行累计 商户毛额 + (商户,规则)原始费/cap ②各组封顶 → 商户净额一条 exclusive_full 明细。
        java.util.LinkedHashMap<String, long[]> merchantAgg = new java.util.LinkedHashMap<>();
        // P6:merchantAgg[mid] = {grossSum, verifiedCount, guestCount, compServiceFeeSum}
        //   compServiceFeeSum = 每笔核销/冲正落库的【实收】综合服务费之和(核销 fee>=0·增量封顶后·冲正 fee<=0·取原行之反)。
        for (var ex : exclusives) {
            // D6：项目按归属承担的折扣加回该商户毛额，使其拿满额(platform_absorb 全额/split_by_ratio 平台份额/merchant_absorb=0)。
            //   综合服务费在【核销时】已在此同口径毛额上算并落库(见 ComprehensiveServiceFee + SettlementSettledConsumer);
            //   此处只用毛额算净额，不再月末重算 fee。折扣=0 时 gross=revenue → 与 D6 前逐分一致(零行为变更)。
            long platformBorne = platformBorneDiscount(discountCfg, ex.discountCent());
            long gross = ex.revenueCent() + platformBorne;
            comboDiscountCent += platformBorne; // 项目承担的专属折扣入账
            long[] mg = merchantAgg.computeIfAbsent(ex.merchantId(), k -> new long[4]);
            mg[0] += gross;
            mg[1] += ex.verifiedCount();
            mg[2] += ex.guestCount();
            // ✅ P6 守恒修复(2026-07-21·过账激活前必修·对抗审计坐实):综合服务费【不再月末对聚合 gross 重算】——
            //   旧 min(fixed×count,gross)/min(raw,cap) 是月度非线性(f(-x)≠-f(x))，过账开启后原核销月已过账时其跨月红冲落
            //   当月、聚合行 gross 带负，按费率/毛额重算 fee≠原月实收之反 → 触顶/固定额场景项目倒亏商户多拿、net<0 撞
            //   chk_si_amount 崩。改为【每笔实收 fee 之和】(核销时按每笔+增量封顶落库、冲正严格取原行之反)→ 固定额/封顶/
            //   比例三模式跨月红冲全守恒。封顶已在核销时按「商户×规则」按月增量应用，此处无需再 clamp。
            mg[3] += ex.compServiceFeeCent();
        }
        long compServiceFee = 0L;
        // 商户净额 = 毛额合计 − 该商户当月【实收】综合服务费合计;各商户一条 exclusive_full 直付明细(同商户多票种合并)。
        for (var e : merchantAgg.entrySet()) {
            long[] mg = e.getValue();
            long merchantFee = mg[3];
            compServiceFee += merchantFee;
            long net = mg[0] - merchantFee;
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
        // R-7 尾差:应分总额(对各家精确份额合计只舍入一次) − Σ各家已舍入分账额。归项目公司/平台兜底,
        //   单列落 bill.rounding_residual_cent 留痕(V343),不并进任何一家、也不静默沉淀在「总池 − Σ分账」里。
        //   专属直付(exclusive_full)明细是「毛额 − 每笔实收服务费」的整数补数,不产生舍入 → 不进尾差口径。
        long roundingResidual = MoneyRounding.residual(pooled.exactTotal(), pooledDistributed);
        verifyConservation(availablePool, retention, spvPool,
                pooled.exactTotal(), pooledDistributed, roundingResidual, pooled.items().size());
        // 越池判定按【应分总额】(= Σ已舍入 + 尾差)而非 Σ已舍入:特批闸管的是「业务上分超了池」,
        //   不该被 ±n/2 分的舍入尾差触发(尾差本就由平台兜底,不需要为几分钱把整月单打回待特批)。
        //   无舍入(按客数/固定额/比例整除得尽)时应分总额 ≡ Σ已舍入 → 与改造前逐分同判(零行为变更)。
        long designedPooledTotal = pooledDistributed + roundingResidual;
        boolean overPool = designedPooledTotal > spvPool;
        String status = overPool ? STATUS_DRAFT : STATUS_CONFIRMED;
        String specialApprovalRef = null; // 越池时留 null 待人工填;未越池时也 null(无需特批)

        // 六业态收入归属:通票收入归属的 L3 管理公司(娱乐平台公司),按 (项目, all_pass_ticket) 可配置解析;
        //   项目特例优先→全局默认→未配置返 null(仅记归属,不改任何金额)。
        String attributedEntityId =
                attributionRepository.resolveManagementEntity(projectId, REVENUE_STREAM_ALL_PASS_TICKET);

        String billId = idFor(projectId, month);
        repository.upsertBill(new SettlementBillRecord(
                billId, projectId, periodStart, periodEnd,
                ticketRevenue, channelFee, 0L, availablePool,   // giveaway_cost_cent 恒 0:赠酒补偿走 bar 域内部结算价应付、不占分账池(D5)
                retentionBps, retention, spvPool, distributed, compServiceFee,
                method, specialApprovalRef, status, attributedEntityId, comboDiscountCent,
                roundingResidual));

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

    /**
     * 按分配方式算各酒吧分账（整数分，四舍五入到分）。
     *
     * <p>R-7：同时累计各家<b>精确（未舍入）份额</b>之和 {@link PooledDistribution#exactTotal()}，
     * 供上层算尾差 = round_half_up(Σ精确份额) − Σ已舍入份额。按客数/固定额两方式是整数乘法、
     * 精确值 ≡ 舍入值 → 尾差恒 0（零行为变更）；只有按排名档位（比例分池）才可能产生尾差。
     */
    private PooledDistribution distribute(String projectId, String method, long spvPool,
                                          List<JdbcSettlementBillRepository.MerchantMonthlyStat> stats) {
        List<ItemCalc> out = new ArrayList<>();
        BigDecimal exactTotal = BigDecimal.ZERO;
        switch (method) {
            case METHOD_BY_RANK_TIER -> {
                List<JdbcSettlementBillRepository.RankTier> tiers = repository.findRankTiers(projectId);
                // stats 已按 verified_count DESC 排好 → 顺位即排名(1-based)
                int rank = 0;
                for (var s : stats) {
                    rank++;
                    int shareBps = shareBpsForRank(tiers, rank);
                    // R-7:旧 `spvPool * shareBps / 10000` 整除截断,各家恒少拿、差额悄悄沉淀在池里不留痕。
                    long amount = MoneyRounding.bpsOf(spvPool, shareBps);
                    exactTotal = exactTotal.add(MoneyRounding.exactBps(spvPool, shareBps));
                    out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), rank, amount, method));
                }
            }
            case METHOD_BY_VENUE_PER_GUEST -> {
                for (var s : stats) {
                    var rate = repository.findMerchantRate(projectId, s.merchantId());
                    long perGuest = rate != null && rate.perGuestCent() != null ? rate.perGuestCent() : 0L;
                    long amount = (long) s.guestCount() * perGuest;   // 整数乘法,无舍入
                    exactTotal = exactTotal.add(BigDecimal.valueOf(amount));
                    out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), null, amount, method));
                }
            }
            case METHOD_BY_FIXED_AMOUNT -> {
                for (var s : stats) {
                    var rate = repository.findMerchantRate(projectId, s.merchantId());
                    long fixed = rate != null && rate.fixedAmountCent() != null ? rate.fixedAmountCent() : 0L;
                    exactTotal = exactTotal.add(BigDecimal.valueOf(fixed));   // 固定额,无舍入
                    out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), null, fixed, method));
                }
            }
            default -> {
                // 未知方式:不分账(全沉淀总池),防御性兜底
            }
        }
        return new PooledDistribution(out, exactTotal);
    }

    /**
     * R-7 资金守恒运行期自检（fail-loud，宁可拒绝出账也不出一张不守恒的结算单）。
     *
     * <p>① 池链无泄漏：{@code 可分配池 = 项目留存 + 各酒吧总池}（两者互为补数，逐分相等；池本身 =
     *    max(0, 收入 − 渠道费)，钳 0 是既有设计，跨月红冲致收入为负时池归 0，不纳入本恒等式）。
     * ② 分账链尾差单列后严格相等：{@code Σ池分账 + 尾差 = 应分总额}。
     * ③ 尾差量级合理：|尾差| ≤ 参与分账商户数（每家最多贡献 0.5 分的舍入差，n 家上界 n/2，取 n 更宽松）；
     *    越界说明分配算法本身出错（而非舍入），必须响亮失败而不是把差额记成「尾差」蒙混过关。
     */
    private void verifyConservation(long availablePool, long retention, long spvPool,
                                    BigDecimal exactPooledTotal, long pooledDistributed,
                                    long roundingResidual, int itemCount) {
        if (retention + spvPool != availablePool) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "结算资金守恒校验失败(池链):可分配池 " + availablePool + " ≠ 项目留存 " + retention
                            + " + 各酒吧总池 " + spvPool);
        }
        long designedTotal = MoneyRounding.toCent(exactPooledTotal);
        if (pooledDistributed + roundingResidual != designedTotal) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "结算资金守恒校验失败(分账链):Σ池分账 " + pooledDistributed + " + 尾差 " + roundingResidual
                            + " ≠ 应分总额 " + designedTotal);
        }
        if (Math.abs(roundingResidual) > Math.max(1L, itemCount)) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "结算尾差异常(疑分配算法错而非舍入):尾差 " + roundingResidual
                            + " 超出参与分账商户数 " + itemCount + " 的合理上界");
        }
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
     * R-7 改 HALF_UP 后仍<b>对 0 对称</b>({@code f(-x)=-f(x)},HALF_UP 远离零进位),split_by_ratio 的正负份额
     * 依旧逐分精确对消;折扣=0 → 0(无折扣零行为变更)。
     */
    private long platformBorneDiscount(JdbcSettlementBillRepository.ComboDiscountConfig cfg, long discountCent) {
        return switch (cfg.discountBearer()) {
            case "platform_absorb" -> discountCent;
            case "split_by_ratio" -> MoneyRounding.bpsOf(discountCent, cfg.splitPlatformBps());
            default -> 0L; // merchant_absorb（及未知值防御）：项目不承担
        };
    }

    /** 分账中间结果（method：池分账=当月分配方式，专属直付=exclusive_full）。 */
    private record ItemCalc(String merchantId, int verifiedCount, int guestCount, Integer rankNo, long amount,
                            String method) {
    }

    /**
     * 池分账结果 + 各家<b>精确（未舍入）份额之和</b>：R-7 尾差 = round_half_up(exactTotal) − Σ items.amount。
     * exactTotal 只用于算尾差，绝不直接落库。
     */
    private record PooledDistribution(List<ItemCalc> items, BigDecimal exactTotal) {
    }
}
