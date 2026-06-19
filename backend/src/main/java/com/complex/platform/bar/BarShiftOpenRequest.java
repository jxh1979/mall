package com.complex.platform.bar;

/**
 * 开班请求（V095 交班/班结）。记录吧台、收银员、备用金（备用金可缺省 0），班次开始。
 */
public record BarShiftOpenRequest(
        String idempotencyKey,
        String barId,
        String cashierUserId,
        String projectId,
        Long openingCashCent,
        Long cashDiffThresholdCent,
        String note
) {
}
