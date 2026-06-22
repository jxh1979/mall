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
        int soldQuantity
) {
    /**
     * 向后兼容构造：旧 12 参签名（无库存字段）→ 库存不限量（stockQuantity=null）、soldQuantity=0。
     * 供既有调用点/测试不改而仍编译；需设置库存的路径（创建/发布/映射）使用全参构造器。
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
                stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt, null, 0);
    }
}
