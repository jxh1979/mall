package com.complex.platform.inventory;

// 文件版本：V0.01

import java.util.List;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;

public record InventoryQuarantineDispositionRequest(String idempotencyKey,int quantity,String dispositionType,String reason,
        List<String> evidenceAttachmentIds,String finalApproverId,List<ParallelLine> parallelLines) {}
