package com.complex.platform.contract;

// 文件版本：V0.01

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 合同（V162 重建，对齐 contracts 演进表 + HTML §九 8 业务态）。
 * rentTerms 承载 基础租金/递增/免租/物业费；snapshot=生效固化收益快照（4项，NULL=未生效）。
 * reviewId（V494 contracts.review_id，HP-C01）=提交审批时开的统一审批复核单，终态回写合同状态；空=未提交或未接审批。
 */
public record ContractRecord(
        String id,
        String merchantId,
        String contractSubjectId,
        String operationSubjectId,
        String settlementSubjectId,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        Map<String, Object> rentTerms,
        String commissionMode,          // 抽成模式（4类，可空=无抽成）
        String energyFeeBasis,          // 公区能耗分摊（4模式）
        Long depositCent,               // 保证金（分）
        Map<String, Object> penaltyTerms,
        List<Map<String, Object>> settlementRules, // 退租结算逐例顺序
        Map<String, Object> snapshot,   // 生效固化（NULL=未生效）
        BigDecimal shareholderRatioForReport,
        List<String> attachmentIds,
        String lifecycleStatus,         // 8 业务态
        String projectId,
        Instant terminatedAt,
        String terminationReason,
        String renewedFromContractId,
        Instant createdAt,
        Instant updatedAt,
        String reviewId
) {
    /** 兼容构造（无复核单）：起草/测试夹具沿用，reviewId 为空。 */
    public ContractRecord(
            String id, String merchantId, String contractSubjectId, String operationSubjectId, String settlementSubjectId,
            LocalDate effectiveDate, LocalDate expiryDate, Map<String, Object> rentTerms, String commissionMode,
            String energyFeeBasis, Long depositCent, Map<String, Object> penaltyTerms,
            List<Map<String, Object>> settlementRules, Map<String, Object> snapshot, BigDecimal shareholderRatioForReport,
            List<String> attachmentIds, String lifecycleStatus, String projectId, Instant terminatedAt,
            String terminationReason, String renewedFromContractId, Instant createdAt, Instant updatedAt) {
        this(id, merchantId, contractSubjectId, operationSubjectId, settlementSubjectId, effectiveDate, expiryDate, rentTerms,
                commissionMode, energyFeeBasis, depositCent, penaltyTerms, settlementRules, snapshot, shareholderRatioForReport,
                attachmentIds, lifecycleStatus, projectId, terminatedAt, terminationReason, renewedFromContractId, createdAt,
                updatedAt, null);
    }
}
