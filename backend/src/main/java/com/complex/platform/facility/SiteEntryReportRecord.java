package com.complex.platform.facility;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        List<String> completionAttachmentIds,
        String completionNote,
        Instant completionUploadedAt,
        Instant deletedAt) {

    public SiteEntryReportRecord {
        completionAttachmentIds = completionAttachmentIds == null ? List.of() : List.copyOf(completionAttachmentIds);
    }

    /**
     * P0-A/GAP-07 展示脱敏：无 {@code pii.contact_phone.view} 权限的读端返回掩码手机号。
     * 脱敏放在 controller（service 外），与 V279 内部结算价同款范式。
     */
    public SiteEntryReportRecord withMaskedPhone() {
        return new SiteEntryReportRecord(id, projectId, spaceId, workScope, plannedStartDate, plannedEndDate,
                crewCount, contactName, com.complex.platform.common.PhoneMask.mask(contactPhone), vendorUserId,
                status, reviewComment, acceptanceComment, createdBy, createdAt, updatedAt, completionAttachmentIds,
                completionNote, completionUploadedAt, deletedAt);
    }
}
