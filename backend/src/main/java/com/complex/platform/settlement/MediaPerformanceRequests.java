package com.complex.platform.settlement;

// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;

/**
 * TK-29 传媒逐场演出合同 / 履约 / 结算写入面请求 DTO（JSON 全局 SNAKE_CASE：idempotencyKey ↔ idempotency_key 等）。
 */
public final class MediaPerformanceRequests {

    private MediaPerformanceRequests() {
    }

    /**
     * 逐场合同记录：三种情形各自可空＝「只有其中一种或两种」。①门票加价分成按操作员裁决（2026-10-20）填「本场每张门票加价金额」
     * 与适用的酒吧门票票种，比例（万分比）或金额二选一；②固定费用；③酒水提成比例 + 基数模板（show_window 演出时段 /
     * whole_store_session 当场全店）。自办场次（self_hosted）三者都不得填。
     */
    public record ShowRequest(String idempotencyKey, String projectId, String barMerchantId, String mediaMerchantId,
                              String showKind, LocalDate showDate, Instant plannedStartAt, Instant plannedEndAt,
                              String contractRef, String ticketShareMode, Integer ticketShareBps, Long ticketShareAmountCent,
                              Long ticketSurchargePerTicketCent, String ticketTypeId, Long fixedFeeCent,
                              Integer drinkCommissionBps, String drinkBaseTemplate) {
    }

    /** 只带幂等键的动作：锁定、确认、结算。 */
    public record ActionRequest(String idempotencyKey) {
    }

    /** 实际履约登记：实际时间、已完成 / 未完成内容、依据。 */
    public record FulfillmentRequest(String idempotencyKey, Instant actualStartAt, Instant actualEndAt,
                                     String completedContent, String uncompletedContent, String basis) {
    }

    public record DisputeRequest(String idempotencyKey, String disputeReason) {
    }

    public record ResolveRequest(String idempotencyKey, String resolutionNote) {
    }

    /** 调整已出账结算：新金额（分）与理由，新行回指原行。 */
    public record AdjustRequest(String idempotencyKey, Long amountCent, String adjustReason) {
    }
}
