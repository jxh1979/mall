package com.complex.platform.access.staff;

// 文件版本：V0.01

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 金额均为分；审批原件与计算结果使用同一结构，禁止把公式当脚本执行。 */
public final class StaffPayrollModels {
    private StaffPayrollModels() {}

    public record Component(String code, String label, String type, String method,
            long amountCent, String unit, String leaveMode, String calculationBasis) {}
    public record PerformanceRules(BigDecimal basePoints, BigDecimal minPoints, BigDecimal maxPoints,
            Map<String,BigDecimal> gradeCoefficients, String aCondition, int cMissedPeriods,
            int assessmentPeriodMonths, Map<String,BigDecimal> deductionRules, String deductionRuleReviewId) {}
    public record WorkTimeTerms(String system, long standardDailyMinutes, long standardWeeklyMinutes,
            long hourlyBaseCent, int ordinaryOvertimeBps, int restDayOvertimeBps, int holidayOvertimeBps,
            String approvalBasis, String calculationBasis) {}
    public record SchemeDraftRequest(String idempotencyKey, String userId, LocalDate effectiveFrom,
            LocalDate effectiveTo, String positionCode, List<Component> components,
            PerformanceRules performanceRules, WorkTimeTerms worktimeTerms, String paymentBasis, String reason) {}
    public record SubmitRequest(String idempotencyKey, List<String> evidenceAttachmentIds,
            List<com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine> parallelLines) {}
    public record Scheme(String id,String userId,String projectId,String merchantId,String payerCompanyId,
            String version,LocalDate effectiveFrom,LocalDate effectiveTo,String positionCode,
            List<Component> components,PerformanceRules performanceRules,WorkTimeTerms worktimeTerms,
            String paymentBasis,String reason,String status,String reviewId,List<String> evidenceAttachmentIds,
            String createdBy,Instant createdAt) {}
    public record CalendarDay(LocalDate date,String kind) {}
    public record CalendarDraftRequest(String idempotencyKey,String projectId,LocalDate startDate,
            LocalDate endDate,List<CalendarDay> days,String sourceBasis,String reason) {}
    public record CalendarVersion(String id,String projectId,String version,LocalDate startDate,
            LocalDate endDate,List<CalendarDay> days,String sourceBasis,String reason,String status,
            String reviewId,List<String> evidenceAttachmentIds,String createdBy,Instant createdAt) {}
    public record ComponentFact(String componentCode,BigDecimal quantity,BigDecimal denominator,
            long embeddedLeaveCent,List<String> appliedLeaveIds,String evidenceBasis) {}
    public record PerformanceFact(boolean aConditionMet,boolean targetMet,String evidenceBasis) {}
    public record PeriodDraftRequest(String idempotencyKey,String userId,String month,String schemeVersionId,
            String calendarVersionId,List<ComponentFact> componentFacts,PerformanceFact performanceFacts,
            LocalDate cycleStart,LocalDate cycleEnd,long cycleStandardMinutes,Map<LocalDate,Long> restCompensatedMinutes,
            LocalDate agreedDueDate,LocalDate statutoryDueDate,String reason) {}
    public record PeriodFacts(String id,String userId,String month,String projectId,String merchantId,
            String payerCompanyId,String schemeVersionId,String calendarVersionId,List<ComponentFact> componentFacts,
            PerformanceFact performanceFacts,LocalDate cycleStart,LocalDate cycleEnd,long cycleStandardMinutes,
            Map<LocalDate,Long> restCompensatedMinutes,LocalDate agreedDueDate,LocalDate statutoryDueDate,
            String reason,String status,String reviewId,List<String> evidenceAttachmentIds,String createdBy,Instant createdAt) {}
    public record ScoreRuleDraftRequest(String idempotencyKey,String projectId,String merchantId,String ownerCompanyId,
            LocalDate effectiveFrom,Map<String,BigDecimal> deductions,String reason) {}
    public record ScoreRuleVersion(String id,String projectId,String merchantId,String ownerCompanyId,String version,
            LocalDate effectiveFrom,Map<String,BigDecimal> deductions,String reason,String status,String reviewId,
            List<String> evidenceAttachmentIds,String createdBy,Instant createdAt) {}
    public record ScoreRequest(String idempotencyKey,String userId,String month,String ruleCode,String reason) {}
    public record ScoreAdjustment(String id,String userId,String month,BigDecimal points,String ruleCode,
            String reason,String reviewId,String status,String reversalOf,Instant createdAt) {}
    public record SettleRequest(String idempotencyKey,String userId,String month,String statementKind,
            String factsId,String originEventId,String calculationId,String taxAssessmentId) {
        public SettleRequest(String key,String user,String month,String kind,String facts,String origin) {
            this(key,user,month,kind,facts,origin,null,null);
        }
    }
    public record CalculationDraft(String id,String userId,String month,String statementKind,String originEventId,
            String schemeVersionId,String factsId,String projectId,String merchantId,String payerCompanyId,
            long beforeTaxCent,String sourceFingerprint,Map<String,Object> calculationSnapshot,String status,Instant createdAt) {}
    public record WorkInterval(String sourceId,String shiftId,Instant from,Instant to,String payrollMonth) {}
    public record WorkSegment(String shiftId,LocalDate date,String kind,long seconds,String payrollMonth) {}
    public record PendingItem(String code,String sourceId,String message) {}
    public record WorkResult(List<WorkSegment> segments,long ordinarySeconds,long restDaySeconds,
            long holidaySeconds,long ordinaryOvertimeCent,long restDayOvertimeCent,long holidayOvertimeCent,
            List<PendingItem> pendingItems) {
        public long amountCent() { return Math.addExact(Math.addExact(ordinaryOvertimeCent,restDayOvertimeCent),holidayOvertimeCent); }
    }
    public record Line(String id,String componentCode,String componentType,String description,
            long grossCent,long embeddedLeaveCent,long additionalLeaveCent,long amountCent,
            String sourceType,String sourceId,Map<String,Object> calculationSnapshot) {}
    public record Correction(String id,String originalStatementId,String originalMonth,String sourceReviewId,
            String sourceAdjustmentId,Long amountCent,String status,String targetStatementId,Map<String,Object> calculationSnapshot) {}
    public record Payment(String id,String financePaymentId,long amountCent,String payMethod,String payRef,Instant paidAt) {}
    public record StatementDetail(StaffPayrollStatementRecord statement,String status,String statementKind,
            String schemeVersionId,String factsId,LocalDate paymentDueDate,Map<String,Object> paymentDueBasis,
            String arApItemId,long paidAmountCent,List<Line> lines,WorkResult worktime,List<Correction> corrections,
            List<Payment> payments,List<PendingItem> pendingItems) {}
}
