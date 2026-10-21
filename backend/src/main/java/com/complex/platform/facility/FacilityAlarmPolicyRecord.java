package com.complex.platform.facility;

// 文件版本：V0.01

import java.time.Instant;

/** HP-F01 告警响应时限矩阵一格（V529 facility_alarm_response_policies）：告警类型（设备四组台账）×优先级。 */
public record FacilityAlarmPolicyRecord(
        String alarmType,
        String priority,
        int responseMinutes,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
