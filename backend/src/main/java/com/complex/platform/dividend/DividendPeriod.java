package com.complex.platform.dividend;

import com.complex.platform.common.api.ApiException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 分红期间解析：季度标签（如 2026-Q3）或月份标签（如 2026-07）→ [periodStart, periodEnd] 自然日 +
 * 该期覆盖的费用归集月份清单（V297 merchant_expense_entries.period 为 yyyy-MM 粒度）。
 * 时窗换算（经营日切分）由服务层经 BusinessDayResolver 完成，与 V296 成本汇总同口径。
 */
public record DividendPeriod(String label, LocalDate start, LocalDate end, List<String> months) {

    private static final Pattern QUARTER = Pattern.compile("^(\\d{4})-Q([1-4])$");
    private static final Pattern MONTH = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    public static boolean isQuarterLabel(String label) {
        return label != null && QUARTER.matcher(label).matches();
    }

    public static boolean isMonthLabel(String label) {
        return label != null && MONTH.matcher(label).matches();
    }

    /** 解析期间标签；非法格式抛 VALIDATION_FAILED。 */
    public static DividendPeriod parse(String label) {
        if (label == null || label.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_label is required");
        }
        String trimmed = label.trim();
        var quarter = QUARTER.matcher(trimmed);
        if (quarter.matches()) {
            int year = Integer.parseInt(quarter.group(1));
            int q = Integer.parseInt(quarter.group(2));
            int firstMonth = (q - 1) * 3 + 1;
            LocalDate start = LocalDate.of(year, firstMonth, 1);
            LocalDate end = start.plusMonths(3).minusDays(1);
            return new DividendPeriod(trimmed, start, end, List.of(
                    YearMonth.of(year, firstMonth).toString(),
                    YearMonth.of(year, firstMonth + 1).toString(),
                    YearMonth.of(year, firstMonth + 2).toString()));
        }
        if (MONTH.matcher(trimmed).matches()) {
            YearMonth month = YearMonth.parse(trimmed);
            return new DividendPeriod(trimmed, month.atDay(1), month.atEndOfMonth(), List.of(trimmed));
        }
        throw ApiException.badRequest("VALIDATION_FAILED",
                "period_label must be a quarter (e.g. 2026-Q3) or a month (e.g. 2026-07): " + label);
    }
}
