package com.complex.platform.bar;

import java.time.Instant;

/**
 * 退酒原因码字典项（V351，bar_void_reason_codes）。
 *
 * <p>{@code code} 是机读标识（聚合/对账用，不上屏），{@code labelZh} 才是店员在下拉里看到的中文——
 * 「所有终端零英文全中文」铁律：不让店员手打英文短码。
 */
public record BarVoidReasonCodeRecord(
        String code,
        String labelZh,
        int sortOrder,
        String status,
        String archiveReason,
        String updatedBy,
        Instant updatedAt
) {
}
