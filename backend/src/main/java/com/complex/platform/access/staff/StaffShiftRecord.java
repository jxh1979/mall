package com.complex.platform.access.staff;

// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** E1(2026-07-02 拍板):班次到分钟+指定点位(主管排,双端)。 */
public record StaffShiftRecord(
        String id,
        String userId,
        LocalDate workDate,
        Instant startAt,
        Instant endAt,
        List<String> pointSpaceIds,
        String note,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        String reviewId,
        String publicationStatus
) {
    public StaffShiftRecord(String id, String userId, LocalDate workDate, Instant startAt, Instant endAt,
            List<String> pointSpaceIds, String note, String createdBy, Instant createdAt, Instant updatedAt) {
        this(id, userId, workDate, startAt, endAt, pointSpaceIds, note, createdBy, createdAt, updatedAt, null, "published");
    }

    public StaffShiftRecord awaitingReview(String reviewId) {
        return new StaffShiftRecord(id, userId, workDate, startAt, endAt, pointSpaceIds, note, createdBy,
                createdAt, updatedAt, reviewId, "pending_review");
    }
}
