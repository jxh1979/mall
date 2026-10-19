package com.complex.platform.contract;

// 文件版本：V0.01

import java.time.LocalDate;
import java.util.Map;

/**
 * 租金规则数学（合同域拥有语义）：按 rent_terms 算某期应收 = 基础租金 + 按年递增 − 免租期。
 * finance 计费应调本函数取代「平 baseRentCent」（见 REBUILD-CUTOVER-contract §G）。
 *
 * rent_terms 约定：
 *   baseRentCent: number（基础月租，分）
 *   freeRentMonths: number（自生效起前 N 个月免租=装修期免租，可选）
 *   increase: { mode: "ratio"|"amount", value: number, everyMonths: number=12 }（按年递增，可选）
 *   increaseFrom: "yyyy-MM-dd"（递增起算日，可选；缺省＝合同生效日）。第三轮裁决第 3 条「续租/变更录入的月租就是新的基数；原递增条款的周期与比例沿用，
 *     但从续租/变更生效日起重新计期」：续租终审、面积/位置变更子合同写入该日，递增期数自该日起算（免租期仍自合同生效日起算）。
 */
public final class ContractRentMath {

    private ContractRentMath() {
    }

    /** 算 period（"YYYY-MM"）该合同应收（分）；免租期/生效前返回 0。 */
    public static long periodAmountCent(Map<String, Object> rentTerms, LocalDate effectiveDate, String period) {
        if (rentTerms == null || effectiveDate == null || period == null) {
            return 0L;
        }
        long base = longVal(rentTerms.get("baseRentCent"));
        if (base <= 0) {
            return 0L;
        }
        int monthsElapsed = monthsBetween(effectiveDate, period);
        if (monthsElapsed < 0) {
            return 0L;   // 生效前不起租
        }
        long freeMonths = longVal(rentTerms.get("freeRentMonths"));
        if (monthsElapsed < freeMonths) {
            return 0L;   // 装修期免租
        }
        Object incObj = rentTerms.get("increase");
        if (!(incObj instanceof Map<?, ?> inc)) {
            return base;
        }
        int everyMonths = (int) Math.max(1, longVal(inc.get("everyMonths")) == 0 ? 12 : longVal(inc.get("everyMonths")));
        Object from = rentTerms.get("increaseFrom");
        int periods = (from == null ? monthsElapsed : monthsBetween(LocalDate.parse(from.toString()), period))
                / everyMonths;     // 自递增起算日满几个递增周期
        if (periods <= 0) {
            return base;
        }
        String mode = String.valueOf(inc.get("mode"));
        double value = doubleVal(inc.get("value"));
        if ("amount".equals(mode)) {
            return base + Math.round(value) * periods;          // 定额递增（分/周期）
        }
        if ("ratio".equals(mode)) {
            return Math.round(base * Math.pow(1.0 + value, periods)); // 比例复利递增
        }
        return base;
    }

    /** period("YYYY-MM") 与生效月相差的整月数（period − effective）。 */
    private static int monthsBetween(LocalDate effectiveDate, String period) {
        String[] ym = period.split("-");
        int year = Integer.parseInt(ym[0].trim());
        int month = Integer.parseInt(ym[1].trim());
        return (year - effectiveDate.getYear()) * 12 + (month - effectiveDate.getMonthValue());
    }

    private static long longVal(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private static double doubleVal(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }

    /** 自检：base / 第二年比例递增 / 定额递增 / 免租期。 */
    public static void main(String[] args) {
        LocalDate eff = LocalDate.parse("2026-01-01");
        Map<String, Object> flat = Map.of("baseRentCent", 100000);
        assert periodAmountCent(flat, eff, "2026-06") == 100000 : "flat base";

        Map<String, Object> ratio = Map.of("baseRentCent", 100000,
                "increase", Map.of("mode", "ratio", "value", 0.10, "everyMonths", 12));
        assert periodAmountCent(ratio, eff, "2026-06") == 100000 : "year1 no increase";
        assert periodAmountCent(ratio, eff, "2027-03") == 110000 : "year2 +10%";
        assert periodAmountCent(ratio, eff, "2028-03") == 121000 : "year3 compound";

        Map<String, Object> amount = Map.of("baseRentCent", 100000,
                "increase", Map.of("mode", "amount", "value", 5000));
        assert periodAmountCent(amount, eff, "2027-03") == 105000 : "year2 +5000";

        Map<String, Object> free = Map.of("baseRentCent", 100000, "freeRentMonths", 3);
        assert periodAmountCent(free, eff, "2026-02") == 0 : "free-rent month";
        assert periodAmountCent(free, eff, "2026-05") == 100000 : "after free-rent";
        assert periodAmountCent(flat, eff, "2025-12") == 0 : "before effective";
        Map<String, Object> renewed = Map.of("baseRentCent", 120000, "increaseFrom", "2027-07-01",
                "increase", Map.of("mode", "ratio", "value", 0.10, "everyMonths", 12));
        assert periodAmountCent(renewed, eff, "2028-06") == 120000 : "restart: first cycle on new base";
        assert periodAmountCent(renewed, eff, "2028-07") == 132000 : "restart: +10% after one full cycle";
        System.out.println("ContractRentMath self-check OK");
    }
}
