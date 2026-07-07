package com.complex.platform.ticketing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record TicketPriceRuleRecord(
        String id,
        String ticketTypeId,
        String variantType,
        String priceMode,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        BigDecimal discountRate,
        String channelId,
        String currency,
        String status,
        Map<String, Object> eligibilityRule,
        Instant createdAt,
        Instant updatedAt,
        // V260 存档/删除范式：archived_at 非空=已存档；delete_status none|pending|approved。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason
) {
    /** 向后兼容构造：V260 前 14 参全签名 → 存档/删除字段取默认（未存档、none）。 */
    public TicketPriceRuleRecord(
            String id, String ticketTypeId, String variantType, String priceMode, Long weekdayPriceCent,
            Long weekendPriceCent, Long holidayPriceCent, BigDecimal discountRate, String channelId,
            String currency, String status, Map<String, Object> eligibilityRule,
            Instant createdAt, Instant updatedAt) {
        this(id, ticketTypeId, variantType, priceMode, weekdayPriceCent, weekendPriceCent, holidayPriceCent,
                discountRate, channelId, currency, status, eligibilityRule, createdAt, updatedAt,
                null, null, "none", null, null, null);
    }

    public TicketPriceRuleRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new TicketPriceRuleRecord(id, ticketTypeId, variantType, priceMode, weekdayPriceCent,
                weekendPriceCent, holidayPriceCent, discountRate, channelId, currency, status, eligibilityRule,
                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }

    public TicketPriceRuleRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new TicketPriceRuleRecord(id, ticketTypeId, variantType, priceMode, weekdayPriceCent,
                weekendPriceCent, holidayPriceCent, discountRate, channelId, currency, status, eligibilityRule,
                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }
}
