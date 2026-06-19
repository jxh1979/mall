package com.complex.platform.bar;

import java.util.List;

/** R5 T16：订台定金支付/到店核销动作请求。 */
public record BarReservationDepositActionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String channel
) {
}
