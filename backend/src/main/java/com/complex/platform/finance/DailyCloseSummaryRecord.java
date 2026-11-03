package com.complex.platform.finance;

import java.time.Instant;

/**
 * R5 T7：日结多源汇总（七源）一行汇总。source_stream ∈
 * official_ticket / channel_ticket / bar / amusement / supply_cost / thirdparty_reflow / bar_deposit（订台定金，第十八批 O 段第 126 条 126-R1）。
 */
public record DailyCloseSummaryRecord(
        String id,
        String dailyCloseId,
        String sourceStream,
        long amountCent,
        long refundCent,
        int orderCount,
        Instant createdAt
) {
}
