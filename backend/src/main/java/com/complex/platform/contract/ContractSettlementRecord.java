package com.complex.platform.contract;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 退租/到期结算：逐例顺序执行 + 争议挂起（pending_settlement/deposit_offsetting/arrears_collecting/disputed/
 * disputed_litigation/settled/voided）。
 * documentsChecked*（V495，HP-C02）＝BSM L1352「退租对应期间的全部相关单据收齐并核对完成」的事实，空＝尚未核对；与 ENUM L252 诸态挂同一张单。
 * disputeResolution（V498，HP-C04）＝主管裁决结论与诉讼/仲裁结果的追加留痕（BSM L1352「由主管裁决后继续」「直至诉讼或仲裁有结果后再行处理」），空＝尚未裁决。
 * reviewId（V500，HP-C02）＝到期结算挂的「到期终止」类模板统一审批复核单（BSM L1352「退租、提前终止、合同到期等各种终止结算，都要经过多级审批」）；
 * 退租/提前终止的审批挂在退租变更上，此处为空。
 */
public record ContractSettlementRecord(
        String id,
        String contractId,
        String triggerType,     // move_out / expiry / early_termination
        String status,
        List<Map<String, Object>> settlementItems, // 逐例执行结果
        String disputeReason,
        String resolvedBy,
        Instant resolvedAt,
        String projectId,
        Instant createdAt,
        Instant updatedAt,
        String documentsCheckedBy,
        Instant documentsCheckedAt,
        String documentsCheckNote,
        String disputeResolution,
        String reviewId
) {
    /** HP-C04：主管在「有争议，诉讼/仲裁」态录入诉讼/仲裁结果时追加行的前缀（区别于主管裁决结论行）。 */
    public static final String LITIGATION_OUTCOME_PREFIX = "诉讼/仲裁结果：";

    /** 新开结算单（尚未齐单核对、未挂复核单）：创建点与测试夹具沿用。 */
    public ContractSettlementRecord(String id, String contractId, String triggerType, String status,
            List<Map<String, Object>> settlementItems, String disputeReason, String resolvedBy, Instant resolvedAt,
            String projectId, Instant createdAt, Instant updatedAt) {
        this(id, contractId, triggerType, status, settlementItems, disputeReason, resolvedBy, resolvedAt, projectId,
                createdAt, updatedAt, null, null, null, null, null);
    }

    /** 未挂复核单的结算单（V500 之前的 15 列形）：测试夹具沿用。 */
    public ContractSettlementRecord(String id, String contractId, String triggerType, String status,
            List<Map<String, Object>> settlementItems, String disputeReason, String resolvedBy, Instant resolvedAt,
            String projectId, Instant createdAt, Instant updatedAt, String documentsCheckedBy, Instant documentsCheckedAt,
            String documentsCheckNote, String disputeResolution) {
        this(id, contractId, triggerType, status, settlementItems, disputeReason, resolvedBy, resolvedAt, projectId,
                createdAt, updatedAt, documentsCheckedBy, documentsCheckedAt, documentsCheckNote, disputeResolution, null);
    }

    /** 状态推进：换状态/清单/更新时间，其余字段（争议、裁决、齐单核对事实、复核单）原样保留。 */
    public ContractSettlementRecord withStatus(String newStatus, List<Map<String, Object>> items, Instant now) {
        return new ContractSettlementRecord(id, contractId, triggerType, newStatus, items, disputeReason, resolvedBy,
                resolvedAt, projectId, createdAt, now, documentsCheckedBy, documentsCheckedAt, documentsCheckNote,
                disputeResolution, reviewId);
    }

    /** 记齐单核对事实（核对人/时间/备注），状态不变。 */
    public ContractSettlementRecord withDocumentsChecked(String checkedBy, Instant checkedAt, String note) {
        return new ContractSettlementRecord(id, contractId, triggerType, status, settlementItems, disputeReason,
                resolvedBy, resolvedAt, projectId, createdAt, checkedAt, checkedBy, checkedAt, note, disputeResolution,
                reviewId);
    }

    /** HP-C04 争议流转（商户异议/不接受裁决、主管裁决/录诉讼仲裁结果）：换状态/清单/理由/结论/裁决人时，齐单核对事实与复核单原样保留。 */
    public ContractSettlementRecord withDispute(String newStatus, List<Map<String, Object>> items, String reason,
            String resolution, String by, Instant at, Instant now) {
        return new ContractSettlementRecord(id, contractId, triggerType, newStatus, items, reason, by, at, projectId,
                createdAt, now, documentsCheckedBy, documentsCheckedAt, documentsCheckNote, resolution, reviewId);
    }
}
