package com.complex.platform.access.staff;

// 文件版本：V0.02

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 既有服务收入台账的专业税务认定；原始收入和完整计算结果随申请锁定。 */
public record PayrollTaxAssessment(String id,String userId,String month,String projectId,String merchantId,
        String payerCompanyId,String category,LocalDate taxDate,String identityBasis,String transactionBasis,
        String professionalBasis,long incomeCent,List<SourceLine> sourceLines,String sourceFingerprint,
        String taxVersionId,long taxCent,Calculation calculationSnapshot,String reason,String status,
        String reviewId,List<String> evidenceAttachmentIds,String createdBy,Instant createdAt,Instant decidedAt,
        String payrollCalculationId,String professionalCalculationReference) {
    public record SourceLine(String id,String sourceId,long amountCent) {}
    public record Calculation(String versionId,String version,String category,LocalDate effectiveFrom,
            String roundingRule,String calculationBasis,PayrollTaxVersion.Bracket bracket,
            long grossCent,Long afterBaseCent,Long taxableCent,Long beforeQuickCent,long taxCent) {}
    public record DraftRequest(String idempotencyKey,String userId,String month,String category,LocalDate taxDate,
            String identityBasis,String transactionBasis,String professionalBasis,String reason,
            String payrollCalculationId,Long professionalTaxCent,String professionalCalculationReference) {
        public DraftRequest(String key,String user,String month,String category,LocalDate date,String identity,String transaction,String professional,String reason) {
            this(key,user,month,category,date,identity,transaction,professional,reason,null,null,null);
        }
    }
}
