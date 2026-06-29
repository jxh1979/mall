package com.complex.platform.facility;

import java.time.Instant;
import java.time.LocalDate;

/** Construction/renovation site entry report (V179). */
public record SiteEntryReportRecord(
        String id,
        String projectId,
        String spaceId,
        String workScope,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        int crewCount,
        String contactName,
        String contactPhone,
        String vendorUserId,
        String status,
        String reviewComment,
        String acceptanceComment,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {}
