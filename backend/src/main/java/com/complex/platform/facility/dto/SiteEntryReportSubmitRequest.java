package com.complex.platform.facility.dto;

/** Outsourced construction report submit/start request. */
public record SiteEntryReportSubmitRequest(
        String note,
        String idempotencyKey) {}
