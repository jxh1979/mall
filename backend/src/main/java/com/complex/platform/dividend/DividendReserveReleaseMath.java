package com.complex.platform.dividend;

import java.time.LocalDate;

/**
 * 分红发放保护线留存·释放纯算法（整数分，无副作用）——建设 R。
 *
 * <p>两块纯逻辑，供纯单测覆盖、服务/定时作业共用：
 * <ul>
 *   <li>{@link #releasableCent}：释放额 = 留存额 × 释放比例（万分比，整除截断）。
 *       <b>刻意保持截断</b>：向下取整使「Σ各次释放 ≤ 原留存额」恒成立（分期释放才不会超额返还）；
 *       当前服务层恒以 {@link #FULL_RATIO_BPS}=10000 全额调用，无舍入发生，本口径不影响任何现行金额。
 *       默认 {@link #FULL_RATIO_BPS}=10000 全额释放（口径：暂扣待释放<b>返还</b>，非平台白拿，全额还给主理人；
 *       金额=原留存额，不多不少）。比例参数为未来分期释放预留（当前服务层以全额释放调用）。</li>
 *   <li>{@link #isDue}：按方案释放时点判定某计算单的留存是否到期可自动释放（manual 永不自动；
 *       next_period 期末进入下一期即到期；fixed_delay 期末再延后 N 个周期；annual 期间所在自然年末清算）。</li>
 * </ul>
 */
public final class DividendReserveReleaseMath {

    /** 全额释放比例（万分比）：默认口径=留存全额返还主理人。 */
    public static final int FULL_RATIO_BPS = 10000;

    private DividendReserveReleaseMath() {
    }

    /**
     * 释放额 = 留存额 × ratioBps / 10000（long 整除截断，见类注释「刻意保持截断」）。
     * ratioBps=10000 → 全额（=留存额，不多不少）。
     *
     * @param reserveWithheldCent 原留存额（分，非负）
     * @param ratioBps            释放比例（万分比，0~10000）
     */
    public static long releasableCent(long reserveWithheldCent, int ratioBps) {
        if (ratioBps < 0 || ratioBps > FULL_RATIO_BPS) {
            throw new IllegalArgumentException("ratioBps must be within [0,10000], was " + ratioBps);
        }
        long reserve = Math.max(reserveWithheldCent, 0L);
        return reserve * ratioBps / FULL_RATIO_BPS;
    }

    /**
     * 按方案释放时点判定该计算单的留存在 asOf 是否到期可自动释放。
     *
     * @param mode         释放模式（manual / next_period / fixed_delay / annual）
     * @param periodEnd    计算单期间结束日（自然日，闭区间末日）
     * @param delayPeriods fixed_delay 的延后周期数（>=1；其余模式忽略）
     * @param periodType   方案周期类型（quarterly=每期 3 月 / monthly=每期 1 月）
     * @param asOf         判定基准日（通常=定时作业运行日）
     */
    public static boolean isDue(String mode, LocalDate periodEnd, int delayPeriods, String periodType, LocalDate asOf) {
        if (periodEnd == null || asOf == null || mode == null) {
            return false;
        }
        int monthsPerPeriod = "quarterly".equals(periodType) ? 3 : 1;
        return switch (mode) {
            case "manual" -> false;                                   // 手动模式永不自动释放
            case "next_period" -> asOf.isAfter(periodEnd);            // 期末进入下一期即到期
            case "fixed_delay" -> asOf.isAfter(
                    periodEnd.plusMonths((long) Math.max(delayPeriods, 1) * monthsPerPeriod));
            case "annual" -> asOf.isAfter(LocalDate.of(periodEnd.getYear(), 12, 31)); // 期间所在自然年末清算
            default -> false;
        };
    }
}
