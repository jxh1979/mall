package com.complex.platform.bar;

import java.util.List;

/** 开台请求。营业日由服务端按开台时点所属酒吧营业日确定（ENUM:187），不收前端日期。 */
public record BarTableSessionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String merchantId,
        String tableId,
        String reservationId,
        String customerId,
        Integer guestCount,
        String salesStaffId,
        String serviceStaffId,
        String openedByUserId
) {
}
