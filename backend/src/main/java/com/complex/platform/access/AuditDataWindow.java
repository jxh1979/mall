package com.complex.platform.access;

// 文件版本：V0.02

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 第十八批 BX 段第 372 条 372-R1、372-R3：外审数据时段（北京时间自然日，起止两端都含）。依据 PGM:682「外部审计/监管只能查看划定的范围与时间段」；
 * 第十八批裁决 77 所选选项原文「外审账号在各清单、明细和导出里只看得到这段时间内发生的记录；没有发生日期的资料（规则设置、设备台账、平面图等）照常显示」。
 *
 * <p>读取侧只认请求属性：{@link AuditDataWindowWebMvcConfig} 在请求开始时给已划定时段的外部人员账号放入；没有请求或未放入即无时段
 * （定时作业、回调、直调服务的单测不受影响）。有发生日期的清单在查询尾拼 {@link #andInstant}、{@link #andDate}（行与总数共用同一 where），
 * 明细在过完项目与商户闸后 {@link #requireCovers}。
 */
public record AuditDataWindow(LocalDate from, LocalDate to) {

    static final String ATTRIBUTE = AuditDataWindow.class.getName();

    /** 「2026-01-01～2026-12-31」。 */
    public String label() {
        return from + "～" + to;
    }

    public static Optional<AuditDataWindow> current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes == null ? Optional.empty()
                : Optional.ofNullable((AuditDataWindow) attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
    }

    // ponytail: 起止日期取自库内 DATE 列、不含用户文本，直接拼字面值——命名参数与问号参数两种 JDBC 写法都能拼，不改各仓储参数。

    /** 时刻列：无时段返回空串，否则「 AND 列 >= 起日北京 0 点 AND 列 < 止日次日北京 0 点」。 */
    public static String andInstant(String column) {
        return current().map(window -> " AND " + column + " >= TIMESTAMPTZ '" + window.from.atStartOfDay(LocalZone.ID).toInstant()
                + "' AND " + column + " < TIMESTAMPTZ '" + window.to.plusDays(1).atStartOfDay(LocalZone.ID).toInstant() + "'")
                .orElse("");
    }

    /** 日期列（营业日等）：无时段返回空串，否则「 AND 列 BETWEEN 起日 AND 止日」。 */
    public static String andDate(String column) {
        return current().map(window -> " AND " + column + " BETWEEN DATE '" + window.from + "' AND DATE '" + window.to + "'")
                .orElse("");
    }

    /**
     * 第十八批 CN 段第 372 条第 3 段 372-S3-R1（第十八批裁决 77「各清单、明细、导出只看得到这段时间发生的记录」）：期间列（账期、开票期间）——
     * 无时段返回空串，否则「 AND 起列 >= 起日 AND 止列 <= 止日」，即期间整段落在时段内才列。
     */
    public static String andPeriod(String startColumn, String endColumn) {
        return current().map(window -> " AND " + startColumn + " >= DATE '" + window.from + "' AND " + endColumn + " <= DATE '" + window.to + "'")
                .orElse("");
    }

    /** 372-S3-R1：年-月文本列（yyyy-MM）——只列整月落在时段内的月份；无时段返回空串，时段内没有整月时「 AND FALSE」。 */
    public static String andMonth(String column) {
        return current().map(window -> {
            YearMonth first = YearMonth.from(window.from.minusDays(1)).plusMonths(1); // 起日是 1 日取当月，否则取下月
            YearMonth last = YearMonth.from(window.to.plusDays(1)).minusMonths(1);    // 止日是月末取当月，否则取上月
            return first.isAfter(last) ? " AND FALSE" : " AND " + column + " BETWEEN '" + first + "' AND '" + last + "'";
        }).orElse("");
    }

    /** 372-S3-R1：内存筛选（子记录须先过项目闸、不能先在 SQL 里筛时）——无时段或时刻为空为真，否则北京日期在起止内（两端都含）。 */
    public static boolean covers(Instant at) {
        return at == null || current().map(window -> {
            LocalDate on = at.atZone(LocalZone.ID).toLocalDate();
            return !on.isBefore(window.from) && !on.isAfter(window.to);
        }).orElse(true);
    }

    /** 明细：有时段且该记录的发生时刻不在时段内 403 OUT_OF_AUDIT_DATA_WINDOW。 */
    public static void requireCovers(Instant occurredAt) {
        if (occurredAt != null) {
            requireCovers(occurredAt.atZone(LocalZone.ID).toLocalDate());
        }
    }

    public static void requireCovers(LocalDate occurredOn) {
        current().filter(window -> occurredOn != null && (occurredOn.isBefore(window.from) || occurredOn.isAfter(window.to)))
                .ifPresent(window -> {
                    throw ApiException.forbidden("OUT_OF_AUDIT_DATA_WINDOW", "该记录不在划定的数据时段内");
                });
    }
}
