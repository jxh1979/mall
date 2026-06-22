package com.complex.platform.ticketing.dto;

import java.util.List;

public record TicketTypeRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ticketName,
        String baseCalendarPriceMode,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        List<String> entitlementTemplateIds,
        Boolean stockRequired,
        String status,
        // P0 修复（V133）：按票种总量限量。可空=不限量；非空=库存上限，下单原子扣减、超额拒绝。
        Integer stockQuantity
) {
    /** 向后兼容构造：旧 11 参签名（无 stockQuantity）→ 不限量（null）。 */
    public TicketTypeRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String ticketName,
            String baseCalendarPriceMode,
            Long weekdayPriceCent,
            Long weekendPriceCent,
            Long holidayPriceCent,
            List<String> entitlementTemplateIds,
            Boolean stockRequired,
            String status) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketName, baseCalendarPriceMode,
                weekdayPriceCent, weekendPriceCent, holidayPriceCent, entitlementTemplateIds, stockRequired,
                status, null);
    }
}
