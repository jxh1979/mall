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
        Integer stockQuantity,
        // 结算重构批4（V230）：票种「资格/定价档」分类标签 standard|new_person|student（可空=标准）。
        // 仅分类/报表用,不碰定价;新人/学生便宜=后台给该票种配低价。
        String ticketCategory,
        // 方案2/规则2（V286）：是否可退票（可空=不可退，与建表默认一致）。true 才允许客人申请整票退款、到期未用自动退。
        Boolean refundable
) {
    /** 向后兼容构造：旧 11 参签名（无 stockQuantity/分类）→ 不限量、分类 null（标准）。 */
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
                status, null, null, null);
    }

    /** 向后兼容构造：12 参签名（含 stockQuantity、无分类）→ 分类 null（标准）。 */
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
            String status,
            Integer stockQuantity) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketName, baseCalendarPriceMode,
                weekdayPriceCent, weekendPriceCent, holidayPriceCent, entitlementTemplateIds, stockRequired,
                status, stockQuantity, null, null);
    }
}
