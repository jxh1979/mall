package com.complex.platform.bar;
// 文件版本：V0.01
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
public final class BarPayrollRequests {
    private BarPayrollRequests() {}
    public record Participant(String userId,long shareUnits,String partialCoverMethod,String payerCompanyId) {
        public Participant(String userId,long shareUnits,String partialCoverMethod) {this(userId,shareUnits,partialCoverMethod,null);}
    }
    public record RuleValues(Integer rateBps,Integer minimumRateBps,Integer maximumRateBps,
            Integer withheldBps,Integer minimumWithheldBps,Integer maximumWithheldBps,List<Participant> participants) {}
    public record DraftRule(String idempotencyKey,String ruleKind,String projectId,String merchantId,
            LocalDate effectiveFrom,RuleValues values,String platformCompanyId,String reason) {}
    public record ConfirmRule(String idempotencyKey,String party) {}
    public record SubmitRule(String idempotencyKey,List<String> evidenceAttachmentIds,String finalApproverId,List<ParallelLine> parallelLines) {}
    public record Handover(String idempotencyKey,String nextUserId,Long sessionVersion,String reason) {}
    public record CloseSession(String idempotencyKey,Long sessionVersion,String reason) {}
    public record CloseMonth(String idempotencyKey,String projectId,String merchantId,String month) {}
    public record Rule(String id,String ruleKind,String projectId,String merchantId,LocalDate effectiveFrom,String version,
            RuleValues values,String platformCompanyId,String merchantConfirmedBy,String platformConfirmedBy,
            String reviewId,String status,String reason,List<String> evidenceAttachmentIds,Instant createdAt) {}
    public record Responsibility(String id,String sessionId,String tableId,String userId,String payerCompanyId,
            Instant startedAt,Instant endedAt,String sourceId,String reason) {}
    public record MonthRun(String id,String projectId,String merchantId,String month,long collectedTipCent,
            long collectedServiceFeeCent,long payableTipCent,long payableServiceFeeCent,long withheldServiceFeeCent,
            long roundingResidualCent,String settlementEntityId,String status,List<BarPayrollAllocation> allocations,
            List<String> unresolvedSources,Instant createdAt) {}
}
