package com.complex.platform.facility.dto;

/** Recheck or supervisor confirmation request. */
public record FacilityInspectionRecheckConfirmRequest(
        String note,
        String idempotencyKey) {}
