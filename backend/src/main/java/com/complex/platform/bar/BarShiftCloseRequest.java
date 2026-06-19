package com.complex.platform.bar;

/**
 * 班结请求（V095 交班/班结对账）。录入实点现金 declared_cash，可选交接下一班收银员与备注。
 * 系统应收按班次时间窗口聚合，无需客户端传入。
 */
public record BarShiftCloseRequest(
        String idempotencyKey,
        Long declaredCashCent,
        String handoverToUserId,
        String note
) {
}
