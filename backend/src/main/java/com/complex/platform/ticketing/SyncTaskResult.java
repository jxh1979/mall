package com.complex.platform.ticketing;

public record SyncTaskResult(
        String clientTaskId,
        String status,
        String reason,
        String verificationId,
        String verificationNo,
        String riskAlertId
) {
}
