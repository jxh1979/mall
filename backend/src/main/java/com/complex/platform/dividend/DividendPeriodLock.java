package com.complex.platform.dividend;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;

/**
 * 分红期间关账锁判定（纯函数，无副作用）。
 *
 * <p>口径：某商户某月「关账」= 该月被某张<b>非 preview 且状态∈(approved,paid)</b>的计算单按
 * [period_start, period_end] 覆盖。季度单一次覆盖 3 个自然月；月度单覆盖 1 个月。
 * 已关账月的费用台账（含自动挂接）禁止再录入/冲销——避免已发放分红的基数被追改而静默漂移。
 *
 * <p>preview（月度预览）单永不锁定；draft/pending_review/rejected/voided 亦不锁定
 * （只有真正进入「审批通过/已发放」的正式单才锁）。作废（voided）天然解锁——判定式不含 voided。
 *
 * <p>本类是 {@link JdbcMerchantExpenseRepository} 关账 SQL 谓词的等价 Java 版，供纯单测覆盖判定逻辑；
 * 生产强制由那段 SQL 执行（app 层写路径 guard + 自动挂接 NOT EXISTS）。
 */
public final class DividendPeriodLock {

    private DividendPeriodLock() {
    }

    /** 单张计算单是否锁定给定月份。 */
    public static boolean locks(
            String kind, String status, LocalDate periodStart, LocalDate periodEnd, YearMonth month) {
        if ("preview".equals(kind)) {
            return false;
        }
        if (!"approved".equals(status) && !"paid".equals(status)) {
            return false;
        }
        if (periodStart == null || periodEnd == null) {
            return false;
        }
        // 区间重叠：单期间与该月区间相交即覆盖（季度单跨 3 月，边界月同样命中）。
        return !periodStart.isAfter(month.atEndOfMonth()) && !periodEnd.isBefore(month.atDay(1));
    }

    /** 一组计算单中是否存在锁定该月份者。 */
    public static boolean isLocked(
            Collection<DividendRecords.DividendStatementRecord> statements, YearMonth month) {
        return statements.stream()
                .anyMatch(s -> locks(s.kind(), s.status(), s.periodStart(), s.periodEnd(), month));
    }
}
