package com.complex.platform.access.staff.dto;

// 文件版本：V0.01
import com.complex.platform.access.staff.StaffShiftRecord;
import java.util.List;

/** 同一批排班共用一张审批单；批准后整批发布。 */
public record StaffShiftBatchResult(int createdCount, int updatedCount, List<StaffShiftRecord> shifts,
        String reviewId, String publicationStatus) {
    public StaffShiftBatchResult(int createdCount, int updatedCount, List<StaffShiftRecord> shifts) {
        this(createdCount, updatedCount, shifts, null, "published");
    }
}
