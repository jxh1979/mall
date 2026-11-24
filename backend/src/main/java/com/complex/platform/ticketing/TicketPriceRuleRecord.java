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
        String deleteReason,
        // 第十八批 AW 段 338-R1（V664）：按基础价加减金额（分，带符号：负数为减免、正数为加价），只在 price_mode=amount_delta 时有值。
        Long adjustmentAmountCent,
        // 第十八批 BY 段第 387 条 387-R1（第十八批裁决 81「每个分类都显示存档理由」）：含已存档列表带出存档理由（V271 archive_reason 列）。
        String archiveReason
) implements ArchivableRecord<TicketPriceRuleRecord> {
    /** 387-R1：存档理由之前的签名，既有构造点零改动，archiveReason 取空。 */
    public TicketPriceRuleRecord(String id, String ticketTypeId, String variantType, String priceMode, Long weekdayPriceCent, Long weekendPriceCent, Long holidayPriceCent, BigDecimal discountRate, String channelId, String currency, String status, Map<String, Object> eligibilityRule, Instant createdAt, Instant updatedAt, Instant archivedAt, String archivedBy, String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt, String deleteReason, Long adjustmentAmountCent) {
        this(id, ticketTypeId, variantType, priceMode, weekdayPriceCent, weekendPriceCent, holidayPriceCent, discountRate, channelId, currency, status, eligibilityRule, createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, adjustmentAmountCent, null);
    }

    /** 向后兼容构造：V664 前 20 参全签名 → 加减金额为空。 */
    public TicketPriceRuleRecord(
            String id, String ticketTypeId, String variantType, String priceMode, Long weekdayPriceCent,
            Long weekendPriceCent, Long holidayPriceCent, BigDecimal discountRate, String channelId,
            String currency, String status, Map<String, Object> eligibilityRule, Instant createdAt, Instant updatedAt,
            Instant archivedAt, String archivedBy, String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt,
            String deleteReason) {
        this(id, ticketTypeId, variantType, priceMode, weekdayPriceCent, weekendPriceCent, holidayPriceCent,
                discountRate, channelId, currency, status, eligibilityRule, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, null);
    }

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
                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                adjustmentAmountCent, archivedAt == null ? null : archiveReason);
    }

    public TicketPriceRuleRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new TicketPriceRuleRecord(id, ticketTypeId, variantType, priceMode, weekdayPriceCent,
                weekendPriceCent, holidayPriceCent, discountRate, channelId, currency, status, eligibilityRule,
                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                adjustmentAmountCent, archiveReason);
    }
}
