package com.complex.platform.settlement;

// 文件版本：V0.02

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * TK-29 传媒逐场演出合同 / 实际履约 / 三种结算的行记录（V523）。JSON 全局 SNAKE_CASE。
 */
public final class MediaPerformanceRecords {

    private MediaPerformanceRecords() {
    }

    /** 逐场合同记录：三种情形各自可空；locked 后数值不可改，改动另起版本行（同 showGroupId、contractVersion+1）。 */
    public record ShowRecord(String id, String showGroupId, int contractVersion, String projectId, String barMerchantId,
                             String mediaMerchantId, String showKind, LocalDate showDate, Instant plannedStartAt,
                             Instant plannedEndAt, String contractRef, String ticketShareMode, Integer ticketShareBps,
                             Long ticketShareAmountCent, Long ticketSurchargePerTicketCent, String ticketTypeId,
                             Long fixedFeeCent, Integer drinkCommissionBps, String drinkBaseTemplate, String status,
                             Instant lockedAt, String lockedBy, String createdBy, Instant createdAt, Instant updatedAt) {

        public boolean selfHosted() {
            return "self_hosted".equals(showKind);
        }

        public boolean hasTicketShare() {
            return ticketShareMode != null && !"none".equals(ticketShareMode);
        }
    }

    /**
     * 第十八批 Y 段 183-R2 复核修复（BSM:1839「卡片上的数字就是它下面明细的合计，点进去能核到每一笔」）：场次列表行＝合同记录字段平铺，
     * 另带本场挂起待确认的演出结算条数（与总控「结算待确认」卡同口径；被新版本取代的版本为 0）。
     */
    public record ShowListItem(@JsonUnwrapped ShowRecord show, long pendingSettlementCount) {
    }

    /** 实际履约登记（传媒经办）与确认 / 争议 / 裁决留痕，一场一条。 */
    public record FulfillmentRecord(String id, String showId, Instant actualStartAt, Instant actualEndAt,
                                    String completedContent, String uncompletedContent, String basis, String recordedBy,
                                    Instant recordedAt, String status, String confirmedBy, Instant confirmedAt,
                                    String disputeReason, String disputedBy, Instant disputedAt, String resolutionNote,
                                    String resolvedBy, Instant resolvedAt) {

        /** 只有已确认或已裁决的事实才能被结算引用。 */
        public boolean settleable() {
            return "confirmed".equals(status) || "resolved".equals(status);
        }
    }

    /** 某场某费项的结算行：pending（未确认 / 争议 / 合同未约定）或 posted（已出账）；调整时新行回指原行、原行置 adjusted。 */
    public record SettlementRecord(String id, String showId, String fulfillmentId, String feeKind, long baseAmountCent,
                                   Integer rateBps, Long fixedAmountCent, long amountCent, String payerMerchantId,
                                   String payerEntityId, String payeeEntityId, String status, String pendingReason,
                                   String arApItemId, String adjustsSettlementId, String adjustReason, String createdBy,
                                   Instant createdAt, Instant updatedAt) {
    }

    public record ShowDetail(ShowRecord show, FulfillmentRecord fulfillment, List<SettlementRecord> settlements) {
    }
}
