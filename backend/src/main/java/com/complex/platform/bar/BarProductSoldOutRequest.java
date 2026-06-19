package com.complex.platform.bar;

import java.util.List;

/** V091 沽清/恢复请求：soldOut=true 置沽清(86)，false 恢复供应。 */
public record BarProductSoldOutRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Boolean soldOut,
        String reason
) {
}
