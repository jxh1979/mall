package com.complex.platform.bar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * 酒吧收银班次 + 班结对账（V095，对照 PRD §10.1 交班/班结对账）。
 *
 * <p>一个班次 = 一个收银员在一个吧台的一段收银时间窗口 [openedAt, closedAt)。开班记备用金；
 * 班结按窗口聚合 bar_bill_settlements 各支付方式（cash/pos/online）、退款、小费 → 系统应收快照，
 * 录入实点现金 declaredCash → 现金差异 cashDiff = declaredCash - (openingCash + systemCash)，
 * 差额不为 0 即 needsReview 进复核（不设容差）。系统应收只读聚合，不改账单、不二次过账（与日结口径不重复扣）。
 */
public record BarShiftRecord(
        String id,
        String projectId,
        @JsonIgnore String merchantId,
        String barId,
        String cashierUserId,
        String status,
        Instant openedAt,
        Instant closedAt,
        long openingCashCent,
        Long declaredCashCent,
        long systemCashCent,
        long systemPosCent,
        long systemOnlineCent,
        long systemRefundCent,
        long systemTipCent,
        Long cashDiffCent,
        boolean needsReview,
        String handoverToUserId,
        String note,
        String openedByUserId,
        String closedByUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
