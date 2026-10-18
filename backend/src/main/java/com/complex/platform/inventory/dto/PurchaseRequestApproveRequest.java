package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.util.List;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;

/** 原审批入口发起完整审批链；节点裁决通过统一审批入口完成。 */
public record PurchaseRequestApproveRequest(String idempotencyKey,String decision,String supplyContractId,String note,String finalApproverId,List<ParallelLine> parallelLines,String beneficiaryId,List<String> evidenceAttachmentIds) {
    public PurchaseRequestApproveRequest(String idempotencyKey,String decision,String supplyContractId,String note) {this(idempotencyKey,decision,supplyContractId,note,null,List.of(),null,List.of());}
}
