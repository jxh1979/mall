package com.complex.platform.facility;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

public record SiteEntryHistoryRecord(String reportId, int sequence, String action, String actorId,
        Instant occurredAt, List<String> attachmentIds, String note, String result, Integer completionSequence) {}
