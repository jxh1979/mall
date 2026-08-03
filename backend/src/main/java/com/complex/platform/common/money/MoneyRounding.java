package com.complex.platform.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * 全局金额舍入与分账尾差的<b>唯一公共入口</b>(R-7·2026-07-22 用户拍板 D15.1)。
 *
 * <p>业务口径(治理地图 HTML「全局原则 · 金额一律精确到分」/ 基准 D15.1 / 产品总纲 §42):
 * <ul>
 *   <li>一切与钱有关的金额,计算/存储/显示<b>一律四舍五入保留到分</b>(对客两位小数=整数分)。</li>
 *   <li>「一笔钱按比例拆给多方」(通票分账、费用分摊、提成)时,各方分别四舍五入后与总额的差额(<b>尾差</b>)
 *       必须<b>单列一笔留痕</b>,并由该笔业务<b>实际承担结算责任的主体</b>承担 ——
 *       不得默认转给城市项目公司、顶层平台或其他无关主体,也绝不悄悄并进任一参与方
 *       (旧口径「统一归项目公司/平台兜底」已于 2026-08-01 作废)。</li>
 * </ul>
 *
 * <p>实现要点:
 * <ul>
 *   <li><b>HALF_UP 而非整除截断</b>:Java {@code a * bps / 10000} 是向零截断,系统性少给收费方(或少扣费),
 *       与「四舍五入」字面口径不符;本类统一 {@link RoundingMode#HALF_UP}。</li>
 *   <li><b>符号对称</b>:HALF_UP 对 0 对称(远离零进位),故 {@code f(-x) = -f(x)} 恒成立——
 *       结算冲正(红冲)取原行之反时逐分精确对消,跨月红冲守恒不被舍入破坏。</li>
 *   <li><b>不溢出</b>:全程 BigDecimal,避免 {@code base * bps} 在 long 上先溢出再除。</li>
 *   <li><b>只舍入一次</b>:多档/多段累加(如阶梯提成)先用 {@link #exactBps} 累精确值、末尾 {@link #toCent}
 *       收一次,避免逐档舍入误差累积。</li>
 * </ul>
 */
public final class MoneyRounding {

    /** 万分比(bps)分母。 */
    public static final int BPS_DENOMINATOR = 10_000;

    /** 金额最小单位=分;元的两位小数即整数分,故一切金额舍入 scale=0(分)。 */
    private static final int CENT_SCALE = 0;

    /**
     * 精确(未收敛到分)中间值保留的小数位。bps 比例(分母 10000)在 4 位内必然终止,故对
     * 「是否恰好 .5」的判定精确;任意分母(如 ÷3)的循环小数在 12 位截断后不可能改变分级四舍五入结果。
     */
    private static final int EXACT_SCALE = 12;

    private MoneyRounding() {
    }

    // ------------------------------------------------------------------
    // ① 收敛到分(四舍五入 HALF_UP)——业务落库/落账一律走这里
    // ------------------------------------------------------------------

    /**
     * 按万分比取额并四舍五入到分:{@code round_half_up(baseCent × bps ÷ 10000)}。
     * 取代散落各处的 {@code baseCent * bps / 10000} 整除截断。
     */
    public static long bpsOf(long baseCent, long bps) {
        return prorate(baseCent, bps, BPS_DENOMINATOR);
    }

    /**
     * 按比例取额并四舍五入到分:{@code round_half_up(baseCent × numerator ÷ denominator)}。
     * 取代 {@code baseCent * numerator / denominator} 整除截断与 {@code Math.round((double)…)} 浮点近似。
     *
     * @throws IllegalArgumentException denominator 为 0
     */
    public static long prorate(long baseCent, long numerator, long denominator) {
        requireNonZeroDenominator(denominator);
        return BigDecimal.valueOf(baseCent)
                .multiply(BigDecimal.valueOf(numerator))
                .divide(BigDecimal.valueOf(denominator), CENT_SCALE, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** 把精确金额收敛到分(HALF_UP)。 */
    public static long toCent(BigDecimal exactCent) {
        if (exactCent == null) {
            return 0L;
        }
        return exactCent.setScale(CENT_SCALE, RoundingMode.HALF_UP).longValueExact();
    }

    // ------------------------------------------------------------------
    // ② 精确(未舍入)份额——只用于算尾差,不得直接落库
    // ------------------------------------------------------------------

    /** 精确按万分比份额(不收敛到分),供 {@link #residual} 算尾差 / 多段累加只舍入一次。 */
    public static BigDecimal exactBps(long baseCent, long bps) {
        return exactProrate(baseCent, bps, BPS_DENOMINATOR);
    }

    /** 精确按比例份额(不收敛到分),供 {@link #residual} 算尾差 / 多段累加只舍入一次。 */
    public static BigDecimal exactProrate(long baseCent, long numerator, long denominator) {
        requireNonZeroDenominator(denominator);
        return BigDecimal.valueOf(baseCent)
                .multiply(BigDecimal.valueOf(numerator))
                .divide(BigDecimal.valueOf(denominator), EXACT_SCALE, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // ③ 尾差(归项目公司/平台兜底,调用方必须单列留痕)
    // ------------------------------------------------------------------

    /**
     * 分账尾差 = <b>应分总额</b>(对精确份额合计<b>只舍入一次</b>) − <b>各方已舍入份额之和</b>。
     *
     * <p>守恒恒等式:{@code Σ各方份额 + 尾差 ≡ 应分总额}。尾差可正可负(带符号),
     * 由该笔业务<b>实际承担结算责任的主体</b>承担;<b>调用方必须把金额与责任主体一起单列留痕</b>
     * (如 settlement_bills.rounding_residual_cent + rounding_residual_entity_id),
     * 不得静默并入任一参与方、也不得吞掉。
     *
     * @param exactTotalCent 各方<b>精确</b>份额之和(BigDecimal,未收敛到分)
     * @param roundedParts   各方<b>已四舍五入到分</b>的份额
     */
    public static long residual(BigDecimal exactTotalCent, Collection<Long> roundedParts) {
        long sum = 0L;
        if (roundedParts != null) {
            for (Long part : roundedParts) {
                sum = Math.addExact(sum, part == null ? 0L : part);
            }
        }
        return Math.subtractExact(toCent(exactTotalCent), sum);
    }

    /** {@link #residual(BigDecimal, Collection)} 的可变参数重载。 */
    public static long residual(BigDecimal exactTotalCent, long... roundedParts) {
        long sum = 0L;
        if (roundedParts != null) {
            for (long part : roundedParts) {
                sum = Math.addExact(sum, part);
            }
        }
        return Math.subtractExact(toCent(exactTotalCent), sum);
    }

    private static void requireNonZeroDenominator(long denominator) {
        if (denominator == 0L) {
            throw new IllegalArgumentException("金额比例分母不得为 0");
        }
    }
}
