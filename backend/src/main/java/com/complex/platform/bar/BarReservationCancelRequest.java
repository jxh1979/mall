package com.complex.platform.bar;

import java.util.List;

/** R5 T16：订台取消/爽约请求（no_show=true 表示爽约，直接没收定金）。 */
public record BarReservationCancelRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Boolean noShow,
        String reason
) {
}
