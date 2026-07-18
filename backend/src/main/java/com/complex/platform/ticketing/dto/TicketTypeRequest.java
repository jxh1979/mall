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
        Boolean stockRequired,
        String status,
        // P0 修复（V133）：按票种总量限量。可空=不限量；非空=库存上限，下单原子扣减、超额拒绝。
        Integer stockQuantity,
        // 结算重构批4（V230）：票种「资格/定价档」分类标签 standard|new_person|student（可空=标准）。
        // 仅分类/报表用,不碰定价;新人/学生便宜=后台给该票种配低价。
        String ticketCategory,
        // 方案2/规则2（V286）：是否可退票（可空=不可退，与建表默认一致）。true 才允许客人申请整票退款、到期未用自动退。
        Boolean refundable,
        // AUD-076 单店票挂酒吧：绑定的酒吧商户 id（可空=通票）。建票时校验业态 bar 且未存档；建后不可变（纠错=下架重建）。
        String barId,
        // AUD-004 有效期天数（可空=默认1天）：日/周/月/年票据此出票计算 valid_to；必须为正。
        Integer validityDays,
        // AUD-004 动态码刷新秒数（可空=默认60）：仅允许 30/60。
        Integer refreshIntervalSeconds
) {
    // 2026-07-18 拍板：entitlementTemplateIds 死字段删除（权益统一走独立绑定端点 bindTicketTypeEntitlement）。
    // 以下兼容构造器保留旧签名（含 List<String> 参数，忽略其值），使既有调用点零改动编译。

    /** 2026-07-18 前的 17 参全签名（含 entitlementTemplateIds）→ 忽略该参数。 */
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
            Integer stockQuantity,
            String ticketCategory,
            Boolean refundable,
            String barId,
            Integer validityDays,
            Integer refreshIntervalSeconds) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketName, baseCalendarPriceMode,
                weekdayPriceCent, weekendPriceCent, holidayPriceCent, stockRequired,
                status, stockQuantity, ticketCategory, refundable, barId, validityDays, refreshIntervalSeconds);
    }

    /** AUD-004 前的 15 参全签名（无 validityDays/refreshIntervalSeconds）→ 均 null（默认1天/60秒）。 */
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
            Integer stockQuantity,
            String ticketCategory,
            Boolean refundable,
            String barId) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketName, baseCalendarPriceMode,
                weekdayPriceCent, weekendPriceCent, holidayPriceCent, stockRequired,
                status, stockQuantity, ticketCategory, refundable, barId, null, null);
    }

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
                weekdayPriceCent, weekendPriceCent, holidayPriceCent, stockRequired,
                status, null, null, null, null, null, null);
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
                weekdayPriceCent, weekendPriceCent, holidayPriceCent, stockRequired,
                status, stockQuantity, null, null, null, null, null);
    }

    /** AUD-076 前的 14 参全签名（含 refundable、无 barId）→ barId=null。 */
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
            Integer stockQuantity,
            String ticketCategory,
            Boolean refundable) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketName, baseCalendarPriceMode,
                weekdayPriceCent, weekendPriceCent, holidayPriceCent, stockRequired,
                status, stockQuantity, ticketCategory, refundable, null, null, null);
    }
}
