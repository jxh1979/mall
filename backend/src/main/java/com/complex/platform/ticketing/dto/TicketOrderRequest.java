package com.complex.platform.ticketing.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TicketOrderRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ticketTypeId,
        String priceRuleId,
        Integer quantity,
        LocalDate visitDate,
        String salesChannel,
        String buyerPhone,
        String customerId,
        Map<String, Object> dynamicPricingContext,
        // AUD-076 单店票挂酒吧：客人下单选中的酒吧商户 id；服务端据票种 bar_id 校验并快照，客户端值仅作核对。
        String barId
) {
    // AUD-076 11 参兼容构造（无 barId）→ barId=null（既有员工/终端/测试调用点零改动）。
    public TicketOrderRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String ticketTypeId,
            String priceRuleId,
            Integer quantity,
            LocalDate visitDate,
            String salesChannel,
            String buyerPhone,
            String customerId,
            Map<String, Object> dynamicPricingContext
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketTypeId, priceRuleId, quantity,
                visitDate, salesChannel, buyerPhone, customerId, dynamicPricingContext, null);
    }

    public TicketOrderRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String ticketTypeId,
            String priceRuleId,
            Integer quantity,
            LocalDate visitDate,
            String salesChannel,
            String buyerPhone,
            String customerId
    ) {
        this(
                idempotencyKey,
                operatorNote,
                evidenceAttachmentIds,
                ticketTypeId,
                priceRuleId,
                quantity,
                visitDate,
                salesChannel,
                buyerPhone,
                customerId,
                Map.of()
        );
    }
}
