package com.complex.platform.dividend;

/**
 * 持股分红纯算法（整数分）。取整口径与薪资税表（V217 payroll_tax_brackets）一致：
 * bps 比例用整除链截断（{@code amount * bps / 10000}，long 整除），不四舍五入。
 *
 * <p>V299 恒等式（与 dividend_statements 的 DB CHECK 一一对应）：
 * <ul>
 *   <li>distributable = max(netProfit − applied, 0)（applied=本期结转弥补额；关闭期 applied=0，退化为 max(netProfit,0)）</li>
 *   <li>platformShare + principalGross = distributable（余数归主理人，劈分不丢分）</li>
 *   <li>principalNet = principalGross − tax（tax = principalGross * taxRateBps / 10000）</li>
 * </ul>
 *
 * <p>V303 扣除项三开关（默认全关时逐分等于 V299 现状）：
 * <ul>
 *   <li>亏损结转：累计恒结转（after 恒累计，关闭期不清零）；开关只控本期是否扣减可分配额（applied）；结转链见 {@link Distribution}</li>
 *   <li>欠款抵扣：税后应得先抵扣未清应收 arrearsOutstanding（钳到 principalNet 上限）</li>
 *   <li>发放保护线：留存 = min(抵扣后余额, max(余额×bps/10000, 固定额))，实发 = 抵扣后余额 − 留存</li>
 * </ul>
 */
public final class DividendMath {

    static final int BPS_DENOMINATOR = 10000;

    /** V299 劈分结果（全部整数分，非负）。 */
    public record Split(
            long distributableCent,
            long platformShareCent,
            long principalGrossCent,
            long taxCent,
            long principalNetCent) {
    }

    /** V303 完整分配结果：V299 劈分链 + 亏损结转 + 欠款抵扣 + 发放保护线（全部整数分，非负）。 */
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
            long reserveWithheldCent,
            long payoutCent) {

        Split toSplit() {
            return new Split(distributableCent, platformShareCent, principalGrossCent, taxCent, principalNetCent);
        }
    }

    private DividendMath() {
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
     * @param arrearsOutstandingCent 该商户对平台未清应收合计（非负；关时忽略）
     * @param payoutFloor           发放保护线开关；开时按 bps/固定额留存风险准备金
     * @param reserveBps            留存比例（万分比，0~10000；关时忽略）
     * @param reserveFixedCent      留存固定额（非负；关时忽略），实取 max(比例额, 固定额) 并钳到可发额上限
     */
    public static Distribution compute(
            long netProfitCent, int platformShareBps, int taxRateBps,
            boolean lossCarryforward, long carryInCent,
            boolean arrearsOffset, long arrearsOutstandingCent,
            boolean payoutFloor, int reserveBps, long reserveFixedCent) {
        requireBps(platformShareBps, "platformShareBps");
        requireBps(taxRateBps, "taxRateBps");
        requireBps(reserveBps, "reserveBps");

        // 亏损结转累计恒结转：开关只控「本期扣不扣可分配额」，关闭期不清零累计（用户 2026-07-19 拍板）。
        // carryIn 恒取往期累计；applied 仅开启期扣减（=min(累计, 本期盈利)）；after 恒按累计式（关闭期把老累计+本期新亏损带下去）。
        long carryIn = Math.max(carryInCent, 0L);
        long carryApplied = lossCarryforward ? Math.min(carryIn, Math.max(netProfitCent, 0L)) : 0L;
        long distributable = Math.max(netProfitCent - carryApplied, 0L);
        long carryAfter = carryIn - carryApplied + Math.max(-netProfitCent, 0L);

        long platformShare = distributable * platformShareBps / BPS_DENOMINATOR;
        long principalGross = distributable - platformShare;
        long tax = principalGross * taxRateBps / BPS_DENOMINATOR;
        long principalNet = principalGross - tax;

        long arrears = arrearsOffset ? Math.min(principalNet, Math.max(arrearsOutstandingCent, 0L)) : 0L;
        long afterArrears = principalNet - arrears;

        long reserve = 0L;
        if (payoutFloor) {
            long byRate = afterArrears * reserveBps / BPS_DENOMINATOR;
            reserve = Math.min(afterArrears, Math.max(byRate, Math.max(reserveFixedCent, 0L)));
        }
        long payout = afterArrears - reserve;

        return new Distribution(carryIn, carryApplied, carryAfter, distributable, platformShare,
                principalGross, tax, principalNet, arrears, reserve, payout);
    }

    private static void requireBps(int bps, String name) {
        if (bps < 0 || bps > BPS_DENOMINATOR) {
            throw new IllegalArgumentException(name + " must be within [0,10000], was " + bps);
        }
    }
}
