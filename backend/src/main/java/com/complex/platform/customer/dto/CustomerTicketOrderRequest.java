package com.complex.platform.customer.dto;

// 文件版本：V0.01

import java.time.LocalDate;

public record CustomerTicketOrderRequest(
        String idempotencyKey,
        String ticketTypeId,
        String priceRuleId,
        Integer quantity,
        LocalDate visitDate,
        String contactPhone,
        String buyerNote,
        // AUD-076 单店票挂酒吧：客人选中的酒吧商户 id（单店票必填、通票必空；服务端据票种校验并快照）。
        // TK-06（BSM:843「购票和转赠均不要求录入人脸照」）：下单不再收人脸照与人脸同意。
        String barId
) {
    // AUD-076 7 参兼容构造（无 barId）→ barId=null（既有测试调用点零改动）。
    public CustomerTicketOrderRequest(
            String idempotencyKey, String ticketTypeId, String priceRuleId, Integer quantity,
            LocalDate visitDate, String contactPhone, String buyerNote) {
        this(idempotencyKey, ticketTypeId, priceRuleId, quantity, visitDate, contactPhone, buyerNote, null);
    }
}
