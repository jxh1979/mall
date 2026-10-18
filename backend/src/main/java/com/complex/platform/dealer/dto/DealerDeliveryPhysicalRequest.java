package com.complex.platform.dealer.dto;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

public record DealerDeliveryPhysicalRequest(String idempotencyKey, Instant completedAt, List<String> evidenceAttachmentIds) {}
