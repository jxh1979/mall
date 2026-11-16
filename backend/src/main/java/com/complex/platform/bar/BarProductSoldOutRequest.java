package com.complex.platform.bar;

import java.util.List;

/**
 * V091 沽清/恢复请求：soldOut=true 置沽清(86)，false 解除沽清。
 * 第十八批 AK 段第 221 条 221-R1（ENUM:111「沽清分两档」）：置沽清须带档位 soldOutType——temporary 临时沽清、long_term 长期下架。
 */
public record BarProductSoldOutRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Boolean soldOut,
        String reason,
        String soldOutType
) {
}
