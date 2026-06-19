package com.complex.platform.finance.dto;

import java.util.List;

public record RefundRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetId,
        Long refundAmountCent,
        String reason,
        String refundChannel,
        // P2#④ 统一退款方向维度：original 原路 / cash 现金 / third_party 第三方代退 / mixed 混合。
        // 可空 → 服务层默认 original（最常见、最安全的原路退回语义），向后兼容历史调用方。
        String refundDirection,
        // 跨模块闭环②a：现场/工作机发起退款时携带的操作设备标识（MDM 门控真实启用）。
        // 可空（尾部，向后兼容）：现场工作机退款应传 deviceId → 触发 MDM 硬门控（未纳管/blocked/lost 拒绝）；
        // 后台台账类退款无设备上下文 → 留空，门控安全跳过（不伪造假值）。
        String deviceId
) {
    /**
     * 向后兼容构造（无 deviceId）：供后台台账类退款（无现场设备上下文）使用，
     * deviceId 置 null → 门控安全跳过。保留历史调用方无需改动。
     */
    public RefundRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String targetId,
            Long refundAmountCent,
            String reason,
            String refundChannel,
            String refundDirection
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, targetId,
                refundAmountCent, reason, refundChannel, refundDirection, null);
    }
}
