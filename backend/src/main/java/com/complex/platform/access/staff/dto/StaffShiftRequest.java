package com.complex.platform.access.staff.dto;

// 文件版本：V0.02

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 排班及修改申请；修改时间时显式指定原班次，须审批后发布。 */
public record StaffShiftRequest(
        String idempotencyKey,
        String userId,
        LocalDate workDate,
        Instant startAt,
        Instant endAt,
        List<String> pointSpaceIds,
        String note,
        String shiftId,
        boolean nightDutyManager
) {
    public StaffShiftRequest(String idempotencyKey, String userId, LocalDate workDate, Instant startAt,
            Instant endAt, List<String> pointSpaceIds, String note, String shiftId) {
        this(idempotencyKey, userId, workDate, startAt, endAt, pointSpaceIds, note, shiftId, false);
    }

    public StaffShiftRequest(String idempotencyKey, String userId, LocalDate workDate, Instant startAt,
            Instant endAt, List<String> pointSpaceIds, String note) {
        this(idempotencyKey, userId, workDate, startAt, endAt, pointSpaceIds, note, null);
    }
}
