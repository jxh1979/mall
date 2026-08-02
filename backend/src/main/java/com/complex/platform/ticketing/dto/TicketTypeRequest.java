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
        // AUD-004/2026-07-18 拍板：动态码刷新秒数（可空=默认15）：仅允许 10/15/30/60。
        Integer refreshIntervalSeconds,
        // V369 综合／联合通票售价自动算：票种折扣（百分数 0~100，100=不打折）。可空=100 不打折。
        // 系统算出售价 = Σ(各权益票面原价 × 份数) × 本折扣 ÷ 100，运营填的售价与它的差额不得超过后台可配的允许误差。
        java.math.BigDecimal comboDiscountRate
) {
    // 2026-07-18 拍板：entitlementTemplateIds 死字段删除（权益统一走独立绑定端点 bindTicketTypeEntitlement）。
}
