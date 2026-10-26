package com.complex.platform.dividend;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 分红期间解析：月份（2026-07）、季度（2026-Q3）、半年（2026-H1 / 2026-H2）、年度（2026）标签 → [periodStart, periodEnd] 自然日 +
 * 该期覆盖的费用归集月份清单（V297 merchant_expense_entries.period 为 yyyy-MM 粒度）。
 * BSM L1777「分红周期一律取月及月的整数倍粒度（如月、季、半年、年），不使用周粒度」。
 * 时窗换算（经营日切分）由服务层经 BusinessDayResolver 完成，与 V296 成本汇总同口径。
 */
public record DividendPeriod(String label, LocalDate start, LocalDate end, List<String> months) {

    /** 方案周期类型 → 每期月数（dividend_plans.period_type 闭集）。 */
    public static final Map<String, Integer> MONTHS_PER_PERIOD =
            Map.of("monthly", 1, "quarterly", 3, "semiannual", 6, "annual", 12);

    private static final Pattern MONTH = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final Pattern QUARTER = Pattern.compile("^\\d{4}-Q[1-4]$");
    private static final Pattern HALF = Pattern.compile("^\\d{4}-H[12]$");
    private static final Pattern YEAR = Pattern.compile("^\\d{4}$");

    public static boolean isMonthLabel(String label) {
        return label != null && MONTH.matcher(label).matches();
    }

    /** 期间标签对应的方案周期类型（monthly / quarterly / semiannual / annual）；不是合法标签返回 null。 */
    public static String periodTypeOf(String label) {
        if (label == null) {
            return null;
        }
        if (MONTH.matcher(label).matches()) {
            return "monthly";
        }
        if (QUARTER.matcher(label).matches()) {
            return "quarterly";
        }
        if (HALF.matcher(label).matches()) {
            return "semiannual";
        }
        return YEAR.matcher(label).matches() ? "annual" : null;
    }

    /** 解析期间标签；非法格式抛 VALIDATION_FAILED。 */
    public static DividendPeriod parse(String label) {
        if (label == null || label.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_label is required");
        }
        String trimmed = label.trim();
        String type = periodTypeOf(trimmed);
        if (type == null) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "分红期间必须为月份（如 2026-07）、季度（如 2026-Q3）、半年（如 2026-H1）或年度（如 2026）：" + label);
        }
        int year = Integer.parseInt(trimmed.substring(0, 4));
        int count = MONTHS_PER_PERIOD.get(type);
        YearMonth first = switch (type) {
            case "monthly" -> YearMonth.parse(trimmed);
            case "annual" -> YearMonth.of(year, 1);
            default -> YearMonth.of(year, (trimmed.charAt(6) - '1') * count + 1); // 2026-Q3 / 2026-H2 的序号
        };
        List<String> months = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            months.add(first.plusMonths(i).toString());
        }
        return new DividendPeriod(trimmed, first.atDay(1), first.plusMonths(count - 1L).atEndOfMonth(), List.copyOf(months));
    }
}
