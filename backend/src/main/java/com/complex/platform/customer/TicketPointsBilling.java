package com.complex.platform.customer;

// 文件版本：V0.03

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class TicketPointsBilling {
    private TicketPointsBilling() {}
    public record Rule(String id,String version,LocalDate effectiveMonth,int statementDay,int objectionWorkingDays,
            int minimumDays,int maximumDays,List<Integer> payDays,String status,String reviewId) {}
    public record RuleRequest(String idempotencyKey,LocalDate effectiveMonth,int statementDay,int objectionWorkingDays,
            int minimumDays,int maximumDays,String reason,String finalApproverId) {}
    public record Day(LocalDate date,boolean workingDay,boolean bankWorkingDay) {}
    public record CalendarRequest(String idempotencyKey,List<Day> days,String sourceNote,String finalApproverId) {}
    public record OutageRequest(String idempotencyKey,String projectId,Instant unavailableFrom,Instant unavailableTo,
            String reason,String finalApproverId) {}
    public record ObjectionRequest(String idempotencyKey,String reason) {}
    public record ResolutionRequest(String idempotencyKey,String resolution,String finalApproverId) {}
    public record CaseReceipt(String id,String status,String reviewId) {}
    public record Bill(String id,LocalDate billingMonth,String billingRuleId,String sellerEntityId,String sellerMerchantId,
            String receiverEntityId,String projectId,long amountCent,Instant visibleAt,LocalDate objectionDeadline,
            LocalDate dueDate,String arApItemId,long paidAmountCent,long unpaidAmountCent,String paymentStatus,
            int openObjections,String feeKind,
            // 第十八批 AL 段 240-R1（ENUM:192；第十八批裁决 54「积分账单有未处理异议→待复核」、裁决 64）：五态结算状态 draft／pending_review／confirmed／archived。
            String settlementState) {}
    public record FeeLine(String rewardId,String parentTicketId,int slotIndex,int useIndex,long points,
            java.math.BigDecimal unitPriceCent,long amountCent,String priceVersionId,Instant confirmedAt,String sourceType,String sourceId) {}
}
