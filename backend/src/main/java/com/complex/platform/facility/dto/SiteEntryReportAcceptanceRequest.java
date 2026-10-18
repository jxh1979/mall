package com.complex.platform.facility.dto;

// 文件版本：V0.01

/** Site entry report acceptance request. */
public record SiteEntryReportAcceptanceRequest(
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Integer completionSequence,
        String result,
        String acceptanceComment,
        String idempotencyKey) {}
