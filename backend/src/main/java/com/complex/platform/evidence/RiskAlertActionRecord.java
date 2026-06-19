package com.complex.platform.evidence;

import java.time.Instant;
import java.util.List;

/**
 * 责任链动作流水（append-only）。每次 report/claim/transfer/escalate/resolve/dismiss 各写一行。
 */
public record RiskAlertActionRecord(
        String actionId,
        String alertId,
        String actionType,
        String actorId,
        String fromStatus,
        String toStatus,
        String reason,
        List<String> evidenceRefs,
        Instant createdAt
) {
}
