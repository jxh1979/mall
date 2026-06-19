package com.complex.platform.amusement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * 游乐设施定期检修记录（设计 §7.14a）。承载检修单位/工程师/检修结果/异常说明/主管确认恢复营业。
 */
public record AmusementMaintenanceRecord(
        String id,
        String projectId,
        String deviceId,
        String locationPointId,
        String maintenanceVendorId,
        String engineerId,
        String engineerName,
        Instant maintenanceAt,
        String result,
        String exceptionNote,
        String sourceTerminal,
        String status,
        String supervisorConfirmedBy,
        Instant confirmedAt,
        Instant resumeBusinessAt,
        Instant createdAt,
        @JsonIgnore
        String merchantId
) {
}
