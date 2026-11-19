package com.complex.platform.dividend;

// 文件版本：V0.04

import com.complex.platform.common.money.MoneyRounding;
import java.util.ArrayList;
import java.util.List;

/**
 * 持股分红纯算法（整数分）。
 *
 * <p><b>取整口径（2026-07-28 用户拍板，R-7/D15.1 收尾）——两种口径刻意混用</b>：
 * <ul>
 *   <li><b>平台分成、风险准备金留存 → 四舍五入 HALF_UP</b>（走公共入口 {@link MoneyRounding#bpsOf}）。
 *       旧整除截断使平台恒少收、差额白送主理人且不留痕，与「金额一律四舍五入到分」的全局口径不符。</li>
 *   <li><b>代扣个税 → 保持整除截断</b>（{@code principalGross * taxRateBps / 10000}，与薪资税表
 *       V217 payroll_tax_brackets 同口径）。理由：<b>与税局申报口径对齐</b>，改成四舍五入会导致代扣申报对不上。
 *       <b>此处不得改用 MoneyRounding</b>。</li>
 * </ul>
 *
 * <p><b>多方各自四舍五入、尾差单列</b>（第十八批 AQ 段第 234 条 234-R2；BSM:1775「娱乐平台公司、主理人和其他股东按每家酒吧工商登记的真实持股比例
 * 分配」；BSM:1757「多方分配产生的非涉税尾差单独留痕，并由该笔业务实际承担结算责任的主体承担」；分红 PRD:93「须按通票分账同范式单列尾差列」）：
 * 平台、各其他股东与主理人（10000 − 平台 − 其他股东合计）按持股比例各自四舍五入到分，可分配额减各方合计即尾差（带符号，
 * {@link MoneyRounding#residual}，由该酒吧公司承担、不发给任何一方）。主理人应得之后的链每一步的另一半仍取<b>补数</b>
 * （{@code principalNet := principalGross − tax}、{@code payout := afterCustom − reserve} …），故
 * {@code 平台分成 + 其他股东应得合计 + 尾差 + 代扣个税 + 欠款抵扣 + 自定义扣除 + 风险准备金 + 主理人实得 ≡ 可分配额}，逐分严格相等。
 * 其他股东代扣：个人股东按方案税率整除截断（与主理人同口径），公司股东 0。见 {@code DividendMathTests#mixedRoundingConservesTotalAcrossAllParties}。
 *
 * <p>V299 恒等式（与 dividend_statements 的 DB CHECK 一一对应）：
 * <ul>
 *   <li>distributable = max(netProfit − applied, 0)（applied=本期结转弥补额；关闭期 applied=0，退化为 max(netProfit,0)）；
 *       V507 FS-014 财务已登记依法可分配金额时再与之取小（BSM L1785「本次分配不得超出该金额」；裁决 2026-10-19 第 1 条
 *       「取小、不补」），结转链不动——取小不叠加扣减，已在核定额中扣除的亏损不重复扣</li>
 *   <li>platformShare + principalGross + Σ其他股东应得 + 尾差 = distributable（234-R2 起各方各自四舍五入，尾差带符号单列）</li>
 *   <li>principalNet = principalGross − tax（tax = principalGross * taxRateBps / 10000，<b>整除截断·对齐税局</b>）</li>
 *   <li>第十八批 AX 段 273-R2（BSM:1785「当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红」；BSM:1778「关着时仍须满足法定可分配额与借款清偿
 *       前置，再按比例分配」）：依法可分配上限之后、劈分之前先偿到期未还借款本息 loanRepayment = min(上式可分配额, 到期未还额)，distributable 为偿后余额
 *       （与 V660 chk_ds_distributable 对应）</li>
 * </ul>
 *
 * <p>V303 扣除项三开关（默认全关时逐分等于 V299 现状）：
 * <ul>
 *   <li>亏损结转：累计恒结转（after 恒累计，关闭期不清零）；开关只控本期是否扣减可分配额（applied）；结转链见 {@link Distribution}</li>
 *   <li>欠款抵扣：税后应得先抵扣未清应收 arrearsOutstanding（钳到 principalNet 上限）</li>
 *   <li>发放保护线：留存 = min(抵扣后余额, max(<b>四舍五入</b>(余额×bps/10000), 固定额))，实发 = 抵扣后余额 − 留存</li>
 * </ul>
 *
 * <p>V309 自定义扣除项（#35）：欠款抵扣之后、留存之前，对 (税后应得 − 欠款抵扣) 余额逐项按顺序封顶扣减
 * （{@link #clampDeductions}，不透支）；合计 customDeductionCent 参与发放额恒等：
 * payout = principalNet − arrears − customDeduction − reserve。默认无自定义项时逐分等于 V303 现状。
 */
public final class DividendMath {

    static final int BPS_DENOMINATOR = 10000;

    /** V299 劈分结果（整数分；尾差带符号，234-R2）。 */
    public record Split(
            long distributableCent,
            long platformShareCent,
            long principalGrossCent,
            long taxCent,
            long principalNetCent,
            long roundingResidualCent) {
    }

    /** V303 完整分配结果：V299 劈分链 + 亏损结转 + 欠款抵扣 + V309 自定义扣除 + 发放保护线（全部整数分，非负）。 */
    public record Distribution(
            long carryInCent,
            long carryAppliedCent,
            long carryAfterCent,
            long distributableCent,
            long platformShareCent,
            long principalGrossCent,
            long taxCent,
            long principalNetCent,
            long arrearsOffsetCent,
            long customDeductionCent,
            long reserveWithheldCent,
            long payoutCent,
            // 234-R2 其他股东（与入参同序）：各自应得（四舍五入到分）、代扣（个人股东按方案税率整除截断，公司股东 0）；多方各自四舍五入的尾差（带符号）。
            List<Long> otherGrossCents,
            List<Long> otherTaxCents,
            long roundingResidualCent,
            // 273-R2 出单先偿的到期未还借款本息（distributableCent 已减去本项）。
            long loanRepaymentCent) {

        Split toSplit() {
            return new Split(distributableCent, platformShareCent, principalGrossCent, taxCent, principalNetCent, roundingResidualCent);
        }
    }

    private static final long[] NO_CUSTOM_DEDUCTIONS = new long[0];
    private static final int[] NO_OTHER_SHARES = new int[0];
    private static final boolean[] NO_OTHER_TYPES = new boolean[0];

    private DividendMath() {
    }

    /**
     * V309 自定义扣除逐项封顶扣减（纯函数，无副作用；服务层写逐项算式明细行与本处合计同源，杜绝漂移）。
     * 对给定预算 budget（=税后应得 − 欠款抵扣）按顺序对每项原始额 raw 取 min(剩余预算, max(raw,0))，
     * 返回每项实扣额（逐项非负、合计 ≤ budget，保证发放额不透支）。
     *
     * @param budget       可供自定义扣除的余额（分，非负；负数按 0 处理）
     * @param rawCents     各扣除项原始额（分，按扣减先后顺序；负值按 0 处理）
     * @return 各扣除项实扣额（与 rawCents 等长同序）
     */
    public static long[] clampDeductions(long budget, long[] rawCents) {
        long[] applied = new long[rawCents == null ? 0 : rawCents.length];
        long remaining = Math.max(budget, 0L);
        for (int i = 0; i < applied.length; i++) {
            long a = Math.min(remaining, Math.max(rawCents[i], 0L));
            applied[i] = a;
            remaining -= a;
        }
        return applied;
    }

    /**
     * V299 净利润 → 平台/主理人劈分 → 主理人侧代扣股息红利个税 → 净发放额（三扣除全关）。
     *
     * @param netProfitCent    期间净利润（分，可为负；负值可分配额为 0）
     * @param platformShareBps 平台分成比例（万分比，0~10000；默认方案 8000=80%）
     * @param taxRateBps       主理人代扣税率（万分比，0~10000；真实股东股息红利默认 2000=20%）
     */
    public static Split split(long netProfitCent, int platformShareBps, int taxRateBps) {
        return compute(netProfitCent, platformShareBps, taxRateBps,
                false, 0L, false, 0L, false, 0, 0L).toSplit();
    }

    /**
     * V303 完整分配（净利润制 + 三扣除开关）。默认全关（三 enabled=false）时逐分等于 {@link #split}。
     *
     * @param lossCarryforward      亏损结转开关；开时可分配额 = max(净利 − carryInCent, 0)
     * @param carryInCent           期初往期未弥补亏损累计（非负；关时忽略）
     * @param arrearsOffset         欠款抵扣开关；开时税后应得先抵扣 arrearsOutstandingCent（钳到 principalNet）
     * @param arrearsOutstandingCent 主理人本人欠本酒吧公司的未清应收合计（非负；关时忽略；330-R1）
     * @param payoutFloor           发放保护线开关；开时按 bps/固定额留存风险准备金
     * @param reserveBps            留存比例（万分比，0~10000；关时忽略）
     * @param reserveFixedCent      留存固定额（非负；关时忽略），实取 max(比例额, 固定额) 并钳到可发额上限
     */
    public static Distribution compute(
            long netProfitCent, int platformShareBps, int taxRateBps,
            boolean lossCarryforward, long carryInCent,
            boolean arrearsOffset, long arrearsOutstandingCent,
            boolean payoutFloor, int reserveBps, long reserveFixedCent) {
        return compute(netProfitCent, platformShareBps, taxRateBps, lossCarryforward, carryInCent,
                arrearsOffset, arrearsOutstandingCent, NO_CUSTOM_DEDUCTIONS,
                payoutFloor, reserveBps, reserveFixedCent);
    }

    /**
     * V309 完整分配（V303 三扣除 + 自定义扣除项）。customDeductionRawCents 为空时逐分等于 {@link #compute(long,
     * int, int, boolean, long, boolean, long, boolean, int, long)}。
     *
     * @param customDeductionRawCents 各自定义扣除项原始额（分，按扣减先后顺序；在欠款抵扣后、留存前对
     *        (税后应得 − 欠款抵扣) 余额逐项封顶扣减，见 {@link #clampDeductions}）
     */
    public static Distribution compute(
            long netProfitCent, int platformShareBps, int taxRateBps,
            boolean lossCarryforward, long carryInCent,
            boolean arrearsOffset, long arrearsOutstandingCent,
            long[] customDeductionRawCents,
            boolean payoutFloor, int reserveBps, long reserveFixedCent) {
        return compute(netProfitCent, platformShareBps, taxRateBps, lossCarryforward, carryInCent,
                arrearsOffset, arrearsOutstandingCent, customDeductionRawCents,
                payoutFloor, reserveBps, reserveFixedCent, null);
    }

    /**
     * V507 FS-014 完整分配（V309 + 依法可分配金额）。legalCapCent 为空时逐分等于 V309 口径。
     *
     * @param legalCapCent 财务登记的依法可分配金额（分，非负；空=未核定）
     */
    public static Distribution compute(
            long netProfitCent, int platformShareBps, int taxRateBps,
            boolean lossCarryforward, long carryInCent,
            boolean arrearsOffset, long arrearsOutstandingCent,
            long[] customDeductionRawCents,
            boolean payoutFloor, int reserveBps, long reserveFixedCent, Long legalCapCent) {
        return compute(netProfitCent, platformShareBps, taxRateBps, lossCarryforward, carryInCent,
                arrearsOffset, arrearsOutstandingCent, customDeductionRawCents,
                payoutFloor, reserveBps, reserveFixedCent, legalCapCent, NO_OTHER_SHARES, NO_OTHER_TYPES, 0);
    }

    /**
     * 234-R2 完整分配（V507 + 其他股东）。otherShareBps 为空时只有平台与主理人两方。
     *
     * @param taxRateBps      主理人生效代扣税率（主理人为公司股东时服务层传 0）
     * @param otherShareBps   各其他股东持股比例（万分比，按名单顺序；与平台合计不超过 10000，主理人为余下部分）
     * @param otherIsCompany  各其他股东是否公司股东（公司股东不代扣）
     * @param otherTaxRateBps 其他个人股东代扣税率（全平台统一税率，271-R1；不随主理人类型变）
     */
    public static Distribution compute(
            long netProfitCent, int platformShareBps, int taxRateBps,
            boolean lossCarryforward, long carryInCent,
            boolean arrearsOffset, long arrearsOutstandingCent,
            long[] customDeductionRawCents,
            boolean payoutFloor, int reserveBps, long reserveFixedCent, Long legalCapCent,
            int[] otherShareBps, boolean[] otherIsCompany, int otherTaxRateBps) {
        return compute(netProfitCent, platformShareBps, taxRateBps, lossCarryforward, carryInCent, arrearsOffset, arrearsOutstandingCent,
                customDeductionRawCents, payoutFloor, reserveBps, reserveFixedCent, legalCapCent, otherShareBps, otherIsCompany, otherTaxRateBps, 0L);
    }

    /**
     * 第十八批 AX 段 273-R2 完整分配（234-R2 + 分红前置借款清偿）。loanDueCent 为 0 时逐分等于 234-R2 口径。
     *
     * @param loanDueCent 出单当日营业日已到期未还的借款本息合计（非负；在依法可分配上限之后、劈分之前取小先偿，不透支）
     */
    public static Distribution compute(
            long netProfitCent, int platformShareBps, int taxRateBps,
            boolean lossCarryforward, long carryInCent,
            boolean arrearsOffset, long arrearsOutstandingCent,
            long[] customDeductionRawCents,
            boolean payoutFloor, int reserveBps, long reserveFixedCent, Long legalCapCent,
            int[] otherShareBps, boolean[] otherIsCompany, int otherTaxRateBps, long loanDueCent) {
        requireBps(platformShareBps, "platformShareBps");
        requireBps(taxRateBps, "taxRateBps");
        requireBps(reserveBps, "reserveBps");
        requireBps(otherTaxRateBps, "otherTaxRateBps");
        int otherTotalBps = 0;
        for (int bps : otherShareBps) {
            requireBps(bps, "otherShareBps");
            otherTotalBps += bps;
        }
        if (platformShareBps + otherTotalBps > BPS_DENOMINATOR) {
            throw new IllegalArgumentException("platformShareBps + otherShareBps must not exceed 10000, was " + (platformShareBps + otherTotalBps));
        }

        // 亏损结转累计恒结转：开关只控「本期扣不扣可分配额」，关闭期不清零累计（用户 2026-07-19 拍板）。
        // carryIn 恒取往期累计；applied 仅开启期扣减（=min(累计, 本期盈利)）；after 恒按累计式（关闭期把老累计+本期新亏损带下去）。
        long carryIn = Math.max(carryInCent, 0L);
        long carryApplied = lossCarryforward ? Math.min(carryIn, Math.max(netProfitCent, 0L)) : 0L;
        long distributable = Math.max(netProfitCent - carryApplied, 0L);
        if (legalCapCent != null) {
            distributable = Math.min(distributable, Math.max(legalCapCent, 0L));
        }
        // 273-R2（BSM:1785、PGM:436「酒吧当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红」）：依法可分配上限之后、劈分之前先偿，取小不透支。
        long loanRepayment = Math.min(distributable, Math.max(loanDueCent, 0L));
        distributable -= loanRepayment;
        long carryAfter = carryIn - carryApplied + Math.max(-netProfitCent, 0L);

        // R-7(2026-07-28 拍板):平台分成【四舍五入】——旧整除截断平台恒少收、差额白送主理人且不留痕。
        // 234-R2(BSM:1775、BSM:1757):平台、各其他股东、主理人(10000 − 平台 − 其他股东合计)按持股比例各自四舍五入到分,可分配额减各方合计即尾差
        //   (带符号,MoneyRounding.residual,由该酒吧公司承担);其他股东代扣:个人按方案税率整除截断(与下方主理人同口径),公司 0。
        long platformShare = MoneyRounding.bpsOf(distributable, platformShareBps);
        List<Long> otherGross = new ArrayList<>();
        List<Long> otherTax = new ArrayList<>();
        for (int i = 0; i < otherShareBps.length; i++) {
            long gross = MoneyRounding.bpsOf(distributable, otherShareBps[i]);
            otherGross.add(gross);
            otherTax.add(otherIsCompany[i] ? 0L : gross * otherTaxRateBps / BPS_DENOMINATOR);
        }
        long principalGross = MoneyRounding.bpsOf(distributable, BPS_DENOMINATOR - platformShareBps - otherTotalBps);
        List<Long> parts = new ArrayList<>(otherGross);
        parts.add(platformShare);
        parts.add(principalGross);
        long residual = MoneyRounding.residual(java.math.BigDecimal.valueOf(distributable), parts);
        // 🔴 代扣个税【保持整除截断】(2026-07-28 用户拍板):与税局申报口径对齐,改四舍五入会使代扣申报对不上。
        //   此行【不得】改用 MoneyRounding;与 JdbcStaffRepository 薪资个税税表整除链同口径。
        long tax = principalGross * taxRateBps / BPS_DENOMINATOR;
        long principalNet = principalGross - tax;

        long arrears = arrearsOffset ? Math.min(principalNet, Math.max(arrearsOutstandingCent, 0L)) : 0L;
        long afterArrears = principalNet - arrears;

        // V309 自定义扣除：对 (税后应得 − 欠款抵扣) 余额逐项封顶扣减（与服务层写明细行同源 clampDeductions）。
        long custom = 0L;
        for (long a : clampDeductions(afterArrears, customDeductionRawCents)) {
            custom += a;
        }
        long afterCustom = afterArrears - custom;

        long reserve = 0L;
        if (payoutFloor) {
            // R-7(2026-07-28 拍板):风险准备金留存比例额【四舍五入】(旧整除截断留存恒偏少)。
            long byRate = MoneyRounding.bpsOf(afterCustom, reserveBps);
            reserve = Math.min(afterCustom, Math.max(byRate, Math.max(reserveFixedCent, 0L)));
        }
        long payout = afterCustom - reserve;

        return new Distribution(carryIn, carryApplied, carryAfter, distributable, platformShare,
                principalGross, tax, principalNet, arrears, custom, reserve, payout,
                List.copyOf(otherGross), List.copyOf(otherTax), residual, loanRepayment);
    }

    private static void requireBps(int bps, String name) {
        if (bps < 0 || bps > BPS_DENOMINATOR) {
            throw new IllegalArgumentException(name + " must be within [0,10000], was " + bps);
        }
    }
}
