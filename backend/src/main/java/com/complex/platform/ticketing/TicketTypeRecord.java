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
        String ticketCategory,
        // V260 存档/删除范式：archived_at 非空=已存档；delete_status none|pending|approved。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason,
        // 方案2/规则2（V286）：票种是否可退票。默认 false=不可退（客人不能申请退、到期直接作废不退）；
        // true=可退（未核销可退、到期未使用系统自动退）。一个总开关管申请退与到期退两条路径。放末位便于既有构造点追加。
        boolean refundable
) {
    /** 向后兼容构造：V286 前 21 参全签名（无 refundable）→ 默认 false（不可退，与建表默认一致）。 */
    public TicketTypeRecord(
            String id, String ticketName, String baseCalendarPriceMode, Long weekdayPriceCent,
            Long weekendPriceCent, Long holidayPriceCent, boolean stockRequired, String status,
            Integer validityDays, Integer refreshIntervalSeconds, Instant createdAt, Instant updatedAt,
            Integer stockQuantity, int soldQuantity, String ticketCategory,
            Instant archivedAt, String archivedBy, String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason) {
        this(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent, holidayPriceCent,
                stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory, archivedAt, archivedBy, deleteStatus,
                deleteRequestedBy, deleteRequestedAt, deleteReason, false);
    }

    /** 向后兼容构造：V260 前 15 参全签名 → 存档/删除字段取默认（未存档、none）、不可退。 */
    public TicketTypeRecord(
            String id, String ticketName, String baseCalendarPriceMode, Long weekdayPriceCent,
            Long weekendPriceCent, Long holidayPriceCent, boolean stockRequired, String status,
            Integer validityDays, Integer refreshIntervalSeconds, Instant createdAt, Instant updatedAt,
            Integer stockQuantity, int soldQuantity, String ticketCategory) {
        this(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent, holidayPriceCent,
                stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory, null, null, "none", null, null, null, false);
    }

    /** 16 参构造（15 参 + refundable）：建/发布票种入口透传可退配置，其余存档/删除默认。 */
    public TicketTypeRecord(
            String id, String ticketName, String baseCalendarPriceMode, Long weekdayPriceCent,
            Long weekendPriceCent, Long holidayPriceCent, boolean stockRequired, String status,
            Integer validityDays, Integer refreshIntervalSeconds, Instant createdAt, Instant updatedAt,
            Integer stockQuantity, int soldQuantity, String ticketCategory, boolean refundable) {
        this(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent, holidayPriceCent,
                stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory, null, null, "none", null, null, null, refundable);
    }

    public TicketTypeRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new TicketTypeRecord(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent,
                holidayPriceCent, stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, refundable);
    }

    public TicketTypeRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new TicketTypeRecord(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent,
                holidayPriceCent, stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, refundable);
    }
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
