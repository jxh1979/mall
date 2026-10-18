package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

public record InventoryQuarantineDispositionRecord(String id,String lotId,int quantity,String dispositionType,String reason,
        List<String> evidenceAttachmentIds,String reviewId,String status,String requestedBy,Instant requestedAt,
        String completedBy,Instant completedAt,List<String> completionEvidenceAttachmentIds) {}
