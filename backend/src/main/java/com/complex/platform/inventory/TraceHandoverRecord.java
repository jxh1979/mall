package com.complex.platform.inventory;

import java.time.Instant;
import java.util.List;

/**
 * R5 T17：交接任务表头。保留 V012 既有列（含 handover_result 兼容旧单条语义），
 * 新增 overall_status(in_progress/completed/exception) 与 current_segment 承载五段全链状态机。
 */
public record TraceHandoverRecord(
        String id,
        String traceCode,
        String itemId,
        String fromStaffId,
        String toStaffId,
        String deviceId,
        String handoverResult,
        String overallStatus,
        String currentSegment,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        // 2e 溯源归属（V139）：交接归属商户（currentMerchantScope）；三方法按此校验跨租户。
        String merchantId,
        // 第十八批 K 段第 94 条 94-R1（V581）：出库核验的酒吧订单。
        String orderId
) {
    // 兼容构造（11 参，无 merchantId、orderId）：默认 null，保留既有调用点（如平台级/旧测试）。
    public TraceHandoverRecord(
            String id, String traceCode, String itemId, String fromStaffId, String toStaffId, String deviceId,
            String handoverResult, String overallStatus, String currentSegment,
            List<String> evidenceAttachmentIds, Instant createdAt) {
        this(id, traceCode, itemId, fromStaffId, toStaffId, deviceId, handoverResult, overallStatus,
                currentSegment, evidenceAttachmentIds, createdAt, null, null);
    }
}
