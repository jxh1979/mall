package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * 第十八批 BP 段第 359 条（BSM:782「动态价按时段、容量等规则配置，可先试算再发布。」；BSM:756、ENUM:58「先按日期、时段、场次和容量算动态公开价」）：
 * 动态价规则的条件只认两类——
 * <ul>
 *   <li>359-R1 每日时段 time_from、time_to（时:分）：按下单时刻（所在地时间，BSM:780）判，左闭右开；结束早于开始即跨零点（营业日 10:00～次日 10:00，夜间时段必然跨零点）；</li>
 *   <li>359-R3 day_sold_min（第十八批裁决 75「规则里填『到访当天已售满多少张起』，不要求票种设限量」）：该单到访日该票种已售张数（不含本单）不少于此数才命中。</li>
 * </ul>
 * 场次由规则的到访日期区间覆盖（票务场次＝一次开门到关门，每场所每营业日一场，BSM:778），不另设条件。条件一律由服务端自算，不收终端上报（PGM:744）。
 */
final class TicketDynamicPricingConditions {

    static final String TIME_FROM = "time_from";
    static final String TIME_TO = "time_to";
    static final String DAY_SOLD_MIN = "day_sold_min";
    private static final Set<String> KEYS = Set.of(TIME_FROM, TIME_TO, DAY_SOLD_MIN);
    private static final Pattern HH_MM = Pattern.compile("([01][0-9]|2[0-3]):[0-5][0-9]");

    private TicketDynamicPricingConditions() {
    }

    /** 建规则时校验；值为空的键视同未填并去掉（否则 Map.copyOf 遇空值抛空指针）。带了认不得的键照存会被当成无条件规则全天命中，一律拒。 */
    static Map<String, Object> validate(Map<String, Object> conditionRule) {
        Map<String, Object> actual = new LinkedHashMap<>();
        if (conditionRule != null) {
            conditionRule.forEach((key, value) -> {
                if (value != null) {
                    actual.put(key, value);
                }
            });
        }
        if (!KEYS.containsAll(actual.keySet())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "不支持的条件：动态价只能按每日时段与到访当天已售张数配置");
        }
        Object from = actual.get(TIME_FROM);
        Object to = actual.get(TIME_TO);
        if ((from != null || to != null) && (!hhmm(from) || !hhmm(to) || from.equals(to))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "时段开始与时段结束须同时填写，格式为 时:分，且不能相同");
        }
        Object min = actual.get(DAY_SOLD_MIN);
        if (min != null && !((min instanceof Integer || min instanceof Long) && ((Number) min).longValue() >= 1L)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "到访当天已售张数须为不小于 1 的整数");
        }
        return actual;
    }

    /** at：下单时刻（所在地时间）或试算时刻；daySold：到访当天该票种已售张数，规则带该条件时才取。 */
    static boolean matches(Map<String, Object> conditionRule, LocalTime at, LongSupplier daySold) {
        if (conditionRule == null) {
            return true;
        }
        Object from = conditionRule.get(TIME_FROM);
        Object to = conditionRule.get(TIME_TO);
        if (from != null && to != null && !within(LocalTime.parse(from.toString()), LocalTime.parse(to.toString()), at)) {
            return false;
        }
        Object min = conditionRule.get(DAY_SOLD_MIN);
        return min == null || daySold.getAsLong() >= ((Number) min).longValue();
    }

    /** 复核单摘要与页面同口径：「时段 20:00～22:00」「时段 22:00～次日 02:00」「到访当天已售满 50 张起」；无条件为空串。 */
    static String describe(Map<String, Object> conditionRule) {
        if (conditionRule == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        Object from = conditionRule.get(TIME_FROM);
        Object to = conditionRule.get(TIME_TO);
        if (from != null && to != null) {
            parts.add("时段 " + from + "～" + (to.toString().compareTo(from.toString()) < 0 ? "次日 " : "") + to);
        }
        Object min = conditionRule.get(DAY_SOLD_MIN);
        if (min != null) {
            parts.add("到访当天已售满 " + min + " 张起");
        }
        return String.join(" · ", parts);
    }

    private static boolean hhmm(Object value) {
        return value instanceof String text && HH_MM.matcher(text).matches();
    }

    private static boolean within(LocalTime from, LocalTime to, LocalTime at) {
        return from.isBefore(to)
                ? !at.isBefore(from) && at.isBefore(to)
                : !at.isBefore(from) || at.isBefore(to);
    }
}
