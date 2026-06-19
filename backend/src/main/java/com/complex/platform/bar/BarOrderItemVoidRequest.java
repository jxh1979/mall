package com.complex.platform.bar;

import java.util.List;

/** V091 退菜退酒请求：对单项明细发起撤单/退酒；returnToStock 仅在已出品退酒时回补库存。 */
public record BarOrderItemVoidRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String orderItemId,
        String reason,
        Boolean returnToStock,
        String billId
) {
}
