package com.complex.platform.bar;

/**
 * 退酒原因码字典维护请求（V351）。新增/修改用 code + labelZh + sortOrder；下线用 archiveReason。
 */
public record BarVoidReasonCodeRequest(
        String idempotencyKey,
        String code,
        String labelZh,
        Integer sortOrder,
        String archiveReason
) {
}
