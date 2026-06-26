package com.complex.platform.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 合同（V162 重建，对齐 contracts 演进表 + HTML §九 8 业务态）。
 * rentTerms 承载 基础租金/递增/免租/物业费；snapshot=生效固化收益快照（4项，NULL=未生效）。
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
        Instant updatedAt
) {
}
