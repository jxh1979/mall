package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

public record InventoryQuarantineCompleteRequest(String idempotencyKey,Instant completedAt,List<String> evidenceAttachmentIds) {}
