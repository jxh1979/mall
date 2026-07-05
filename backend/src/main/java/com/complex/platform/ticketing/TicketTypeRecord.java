package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketTypeRecord(
        String id,
        String ticketName,
        String baseCalendarPriceMode,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        boolean stockRequired,
        String status,
        // R5 审计点②：票种有效期天数（可空）。发放时按此天数计算权益 valid_to；
        // 为空则回退默认 1 天（向后兼容）。
        Integer validityDays,
        // P1 清单#19：票种级二维码刷新窗口秒数（可空，取值 {30,60}）。发凭证时按此取值设刷新窗口；
        // 为空则回退默认 60 秒（向后兼容）。
        Integer refreshIntervalSeconds,
        Instant createdAt,
        Instant updatedAt,
        // P0 修复（V133）：按票种总量限量。stockQuantity 为 null=不限量；soldQuantity=已售/已占数量。
        Integer stockQuantity,
        int soldQuantity,
        // 结算重构批4（V230）：票种「资格/定价档」分类标签 standard|new_person|student（可空=标准）。
        // 仅供后台分类/报表,不碰定价/消耗/作废——新人/学生便宜=后台给该票种配低价,现有定价机制。
        // 结构型票种(单人/多人/多次)由 num_people(发放 fan-out)+total_times 派生,不在本字段。放末位便于既有构造点追加。
        String ticketCategory
) {
    /**
     * 向后兼容构造：旧 12 参签名（无库存/分类字段）→ 库存不限量、soldQuantity=0、分类 null（标准）。
     * 供既有调用点/测试不改而仍编译；需设置库存/分类的路径使用全参构造器。
     */
    public TicketTypeRecord(
            String id,
            String ticketName,
            String baseCalendarPriceMode,
            Long weekdayPriceCent,
            Long weekendPriceCent,
            Long holidayPriceCent,
            boolean stockRequired,
            String status,
            Integer validityDays,
            Integer refreshIntervalSeconds,
            Instant createdAt,
            Instant updatedAt) {
        this(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent, holidayPriceCent,
                stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt, null, 0, null);
    }

    /**
     * 向后兼容构造：14 参签名（含库存、无分类字段）→ 分类 null（标准）。
     * 供批3及之前建/发布票种入口不改而仍编译；需设置分类的路径使用全参构造器。
     */
    public TicketTypeRecord(
            String id,
            String ticketName,
            String baseCalendarPriceMode,
            Long weekdayPriceCent,
            Long weekendPriceCent,
            Long holidayPriceCent,
            boolean stockRequired,
            String status,
            Integer validityDays,
            Integer refreshIntervalSeconds,
            Instant createdAt,
            Instant updatedAt,
            Integer stockQuantity,
            int soldQuantity) {
        this(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent, holidayPriceCent,
                stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, null);
    }
}
