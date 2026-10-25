package com.complex.platform.customer;

// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record CustomerPointsAccountRecord(
        String id,
        String customerId,
        long balance,
        String level,
        Map<String, Object> recentSummary,
        long frozenPoints,
        String accountStatus,
        Instant levelPeriodStart,
        long keepThresholdProgress,
        Instant protectionUntil,
        LocalDate lastCheckInDate,
        int checkInStreak,
        Instant createdAt,
        Instant updatedAt
) {
}
