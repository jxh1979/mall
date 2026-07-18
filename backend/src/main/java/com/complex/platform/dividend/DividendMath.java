package com.complex.platform.dividend;

/**
 * 持股分红纯算法（整数分）。取整口径与薪资税表（V217 payroll_tax_brackets）一致：
 * bps 比例用整除链截断（{@code amount * bps / 10000}，long 整除），不四舍五入。
 *
 * <p>恒等式（与 V299 dividend_statements 的 DB CHECK 一一对应）：
 * <ul>
 *   <li>distributable = max(netProfit, 0)（亏损期不倒扣主理人，可分配额为 0）</li>
 *   <li>platformShare + principalGross = distributable（余数归主理人，劈分不丢分）</li>
 *   <li>principalNet = principalGross - tax（tax = principalGross * taxRateBps / 10000）</li>
 * </ul>
 */
public final class DividendMath {

    static final int BPS_DENOMINATOR = 10000;

    /** 劈分结果（全部整数分，非负）。 */
    public record Split(
            long distributableCent,
            long platformShareCent,
            long principalGrossCent,
            long taxCent,
            long principalNetCent) {
    }

    private DividendMath() {
    }

    /**
     * 净利润 → 平台/主理人劈分 → 主理人侧代扣股息红利个税 → 净发放额。
     *
     * @param netProfitCent    期间净利润（分，可为负；负值可分配额为 0）
     * @param platformShareBps 平台分成比例（万分比，0~10000；默认方案 8000=80%）
     * @param taxRateBps       主理人代扣税率（万分比，0~10000；真实股东股息红利默认 2000=20%）
     */
    public static Split split(long netProfitCent, int platformShareBps, int taxRateBps) {
        if (platformShareBps < 0 || platformShareBps > BPS_DENOMINATOR) {
            throw new IllegalArgumentException("platformShareBps must be within [0,10000], was " + platformShareBps);
        }
        if (taxRateBps < 0 || taxRateBps > BPS_DENOMINATOR) {
            throw new IllegalArgumentException("taxRateBps must be within [0,10000], was " + taxRateBps);
        }
        long distributable = Math.max(netProfitCent, 0L);
        long platformShare = distributable * platformShareBps / BPS_DENOMINATOR;
        long principalGross = distributable - platformShare;
        long tax = principalGross * taxRateBps / BPS_DENOMINATOR;
        long principalNet = principalGross - tax;
        return new Split(distributable, platformShare, principalGross, tax, principalNet);
    }
}
