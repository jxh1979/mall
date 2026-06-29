package com.complex.platform.facility.dto;

/** Site entry report review request. */
public record SiteEntryReportReviewRequest(
        String decision,
        String reviewComment,
        String idempotencyKey) {}
