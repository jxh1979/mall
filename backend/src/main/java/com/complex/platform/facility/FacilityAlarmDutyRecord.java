package com.complex.platform.facility;

// 文件版本：V0.01

import java.time.Instant;

/** HP-F01 接警值守安排（V529 facility_alarm_duty_assignments）：挂在已发布班次上的接警角色与联系方式。 */
public record FacilityAlarmDutyRecord(
        String id,
        String projectId,
        String shiftId,
        String shiftUserId,
        Instant shiftStartAt,
        Instant shiftEndAt,
        String dutyRole,
        String contactPhone,
        String createdBy,
        Instant createdAt,
        String cancelledBy,
        Instant cancelledAt,
        String cancelReason) {}
