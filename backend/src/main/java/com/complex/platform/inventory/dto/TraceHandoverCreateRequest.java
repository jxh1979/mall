package com.complex.platform.inventory.dto;

import java.util.List;

/**
 * R5 T17 建交接任务：建表头(overall_status=in_progress) + 预置五段 steps(pending, seq 1..5)。
 * 第十八批 K 段第 94 条 94-R1：物品取溯源码登记的物品；orderId 为出库核验的酒吧订单（BSM:1197「出库须同时核验订单和物品码」）。
 */
public record TraceHandoverCreateRequest(
        String idempotencyKey,
        String traceCode,
        String orderId,
        String deviceId,
        List<String> evidenceAttachmentIds
) {
}
