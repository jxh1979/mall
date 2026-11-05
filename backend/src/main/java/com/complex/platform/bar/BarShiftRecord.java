package com.complex.platform.bar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * 酒吧收银班次 + 班结对账（V095，对照 PRD §10.1 交班/班结对账）。
 *
 * <p>一个班次 = 一个收银员在一个吧台的一段收银时间窗口 [openedAt, closedAt)。开班记备用金；
 * 班结按窗口聚合 bar_bill_settlements 各支付方式（cash/pos/online）、退款、小费 → 系统应收快照，
 * 录入实点现金 declaredCash → 现金差异 cashDiff = declaredCash - (openingCash + systemCash)，
 * 差额不为 0 即 needsReview 进复核（不设容差）。系统应收只读聚合，不改账单、不二次过账（与日结口径不重复扣）。
 *
 * <p>第十八批 R 段第 142 条 142-R1：财务复核平账后 needsReview 置 false（已平账），记复核人 resolvedByUserId、
 * 复核时点 resolvedAt、长款原因 overageCause（短款为空）。
 *
 * <p>第十八批 R 段第 142 条 142-R2：交班单当班收银员签字 cashierSignedAt、接班人签字 successorSignedAt（最后一班没有接班人的由见证财务代签，
 * 与 witnessedAt 同刻）、见证财务 witnessedByUserId 与见证时点 witnessedAt（见证即归档）。
 *
 * <p>第十八批 R 段第 142 条 142-R4（V597）：同一钱箱的其他当班收银员 members（加入人、加入时点、交班单签字时点）；
 * 有成员的班次短款能定位到具体收银员时记 responsibleUserId，为空即当班组共同补足。
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
        Instant updatedAt,
        String resolvedByUserId,
        Instant resolvedAt,
        String overageCause,
        Instant cashierSignedAt,
        Instant successorSignedAt,
        String witnessedByUserId,
        Instant witnessedAt,
        String responsibleUserId,
        List<Member> members
) {

    /** 第十八批 R 段第 142 条 142-R4：加入本钱箱的当班收银员。 */
    public record Member(String userId, Instant joinedAt, Instant signedAt) {
    }

    BarShiftRecord withMembers(List<Member> list) {
        return new BarShiftRecord(id, projectId, merchantId, barId, cashierUserId, status, openedAt, closedAt, openingCashCent,
                declaredCashCent, systemCashCent, systemPosCent, systemOnlineCent, systemRefundCent, systemTipCent, cashDiffCent,
                needsReview, handoverToUserId, note, openedByUserId, closedByUserId, createdAt, updatedAt, resolvedByUserId, resolvedAt,
                overageCause, cashierSignedAt, successorSignedAt, witnessedByUserId, witnessedAt, responsibleUserId, list);
    }

    /** 是否本班次的当班收银员（开班人或已加入本钱箱的成员）。 */
    boolean onShift(String userId) {
        return userId.equals(cashierUserId) || members.stream().anyMatch(m -> m.userId().equals(userId));
    }
}
