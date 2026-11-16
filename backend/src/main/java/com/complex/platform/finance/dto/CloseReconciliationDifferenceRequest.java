package com.complex.platform.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 对账差异提交关闭复核请求（第十八批 Y 段第 181 条 181-R1，原 1.6 一步关闭改为提交复核）。close_reason 为处理结论，落库并进复核单；
 * evidence_attachment_ids 追加进差异的原件附件（补原件即在此补）；final_approver_id 为最终审批人（可空：一级上级）。idempotency_key 幂等去重。
 * 第十八批 AL 段第 223 条 223-R1（BSM:1751「高价值差异复核通过才入账、同一差异只入账一次」）：ledger_entry 为认定结果是否入应收应付台账（空按否），
 * 勾选的复核通过后按实际到账与平台记录之差向原往来方记应收或应付。
 */
public record CloseReconciliationDifferenceRequest(
        @NotBlank @Size(max = 128) String idempotencyKey,
        @NotBlank @Size(max = 1000) String closeReason,
        List<String> evidenceAttachmentIds,
        @Size(max = 64) String finalApproverId,
        Boolean ledgerEntry
) {
}
