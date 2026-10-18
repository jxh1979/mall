package com.complex.platform.bar;

// 文件版本：V0.01

import java.util.List;

public record BarTableTransferRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetTableId,
        String reason,
        String requestedByUserId,
        Long sessionVersion,
        String targetServiceStaffId
) {
    public BarTableTransferRequest(String idempotencyKey,String operatorNote,List<String> evidenceAttachmentIds,
            String targetTableId,String reason,String requestedByUserId,Long sessionVersion) {
        this(idempotencyKey,operatorNote,evidenceAttachmentIds,targetTableId,reason,requestedByUserId,sessionVersion,null);
    }
}
