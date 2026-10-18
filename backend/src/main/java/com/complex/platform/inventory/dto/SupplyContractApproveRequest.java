package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.util.List;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;

/** 原审批入口发起完整审批链；节点裁决通过统一审批入口完成。 */
public record SupplyContractApproveRequest(String idempotencyKey,String decision,String reason,String finalApproverId,List<ParallelLine> parallelLines,String beneficiaryId) {
    public SupplyContractApproveRequest(String idempotencyKey,String decision,String reason) {this(idempotencyKey,decision,reason,null,List.of(),null);}
}
