package com.complex.platform.ticketing;

import java.math.BigDecimal;
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
        // P1 清单#19：票种级二维码刷新窗口秒数（可空，取值 {10,15,30,60}）。发凭证时按此取值设刷新窗口；
        // 为空则回退默认 15 秒（2026-07-18 拍板）。
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
        boolean refundable,
        // AUD-076 单店票挂酒吧：绑定的酒吧商户 id（业态 bar）；null=综合体通票/平台票。建票时挂定、不可变（纠错=下架重建）。
        String barId,
        // V369 综合／联合通票售价自动算：票种折扣（百分数，100=不打折，85=八五折）。
        // 系统算出售价 = Σ(各权益票面原价 × 份数) × 本折扣 ÷ 100（四舍五入到分）；运营填的售价与它的差额
        // 不得超过后台可配的允许误差（见 TicketCatalogService#requirePriceWithinTolerance）。放末位便于既有构造点追加。
        BigDecimal comboDiscountRate
) {

    /** 不打折（100%）——V369 前建的票种与未配折扣的票种一律按此，算出售价 = 各业态票面原价之和。 */
    public static final BigDecimal NO_COMBO_DISCOUNT = BigDecimal.valueOf(100);

    /** V369：改售价/折扣（只动定价三档与折扣，其余字段原样带过）。 */
    public TicketTypeRecord withPricing(BigDecimal discountRate, Long weekday, Long weekend, Long holiday,
            Instant updatedAt) {
        return new TicketTypeRecord(id, ticketName, baseCalendarPriceMode, weekday, weekend, holiday,
                stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory, archivedAt, archivedBy, deleteStatus,
                deleteRequestedBy, deleteRequestedAt, deleteReason, refundable, barId, discountRate);
    }

    /** V369：改组成（加/改权益绑定）后把已发布票种退回草稿，逼它重新过一次发布闸。 */
    public TicketTypeRecord withStatus(String status, Instant updatedAt) {
        return new TicketTypeRecord(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent,
                holidayPriceCent, stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory, archivedAt, archivedBy, deleteStatus,
                deleteRequestedBy, deleteRequestedAt, deleteReason, refundable, barId, comboDiscountRate);
    }

    /** V369 18 参构造（17 参 + comboDiscountRate）：建票入口透传票种折扣，其余存档/删除默认。 */
    public TicketTypeRecord(
            String id, String ticketName, String baseCalendarPriceMode, Long weekdayPriceCent,
            Long weekendPriceCent, Long holidayPriceCent, boolean stockRequired, String status,
            Integer validityDays, Integer refreshIntervalSeconds, Instant createdAt, Instant updatedAt,
            Integer stockQuantity, int soldQuantity, String ticketCategory, boolean refundable, String barId,
            BigDecimal comboDiscountRate) {
        this(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent, holidayPriceCent,
                stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory, null, null, "none", null, null, null, refundable, barId,
                comboDiscountRate);
    }

    public TicketTypeRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new TicketTypeRecord(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent,
                holidayPriceCent, stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, refundable, barId,
                comboDiscountRate);
    }

    public TicketTypeRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new TicketTypeRecord(id, ticketName, baseCalendarPriceMode, weekdayPriceCent, weekendPriceCent,
                holidayPriceCent, stockRequired, status, validityDays, refreshIntervalSeconds, createdAt, updatedAt,
                stockQuantity, soldQuantity, ticketCategory,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, refundable, barId,
                comboDiscountRate);
    }

}
