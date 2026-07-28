package com.complex.platform.bar;

import java.util.List;

/**
 * V091 退菜退酒请求：对单项明细发起撤单/退酒；returnToStock 仅在已出品退酒时回补库存。
 *
 * <p>V345（BAR-VOID-A1）：已出品且金额超后台可配阈值的高价值退酒须<b>双证据</b>——
 * {@code reasonCode}（结构化原因码）+ {@code evidenceAttachmentIds}（附件，不可空），缺一拒收；
 * 低价值/未出品不强制（保留即时生效通路）。
 */
public record BarOrderItemVoidRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String orderItemId,
        String reason,
        Boolean returnToStock,
        String billId,
        String reasonCode
) {
}
