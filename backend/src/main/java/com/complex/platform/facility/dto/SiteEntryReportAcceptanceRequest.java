package com.complex.platform.facility.dto;

/** Site entry report acceptance request. */
public record SiteEntryReportAcceptanceRequest(
        String result,
        String acceptanceComment,
        String idempotencyKey) {}
