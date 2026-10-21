package com.complex.platform.facility.dto;

// 文件版本：V0.01

/** 接警值守安排：在已发布班次上指定 duty_role(primary|backup|supervisor|project_lead|specialist) 与联系电话。 */
public record FacilityAlarmDutyRequest(
        String shiftId,
        String dutyRole,
        String contactPhone,
        String idempotencyKey) {}
