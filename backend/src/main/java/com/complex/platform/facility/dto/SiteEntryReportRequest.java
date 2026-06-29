package com.complex.platform.facility.dto;

import java.time.LocalDate;

/** Create construction/renovation site entry report request. */
public record SiteEntryReportRequest(
        String projectId,
        String spaceId,
        String workScope,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        Integer crewCount,
        String contactName,
        String contactPhone,
        String idempotencyKey) {}
