package com.complex.platform.merchantservice.dto;

import java.util.List;

public record MerchantWorkOrderCloseRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String resolution,
        Integer satisfactionScore,
        String operatorUserId,
        // 第十八批 L 段第 102 条 102-R2：投诉由客户关怀部关闭时必填的关闭缘由（BSM:1385）。
        String closeReason
) {
}
