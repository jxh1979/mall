package com.complex.platform.amusement;

/**
 * 提交检修入参（设计 §8.7.1）。检修单位、工程师、检修时间、结果、异常说明、来源终端、点位结构化留痕。
 */
public record AmusementMaintenanceRecordRequest(
        String idempotencyKey,
        String projectId,
        String deviceId,
        String locationPointId,
        String maintenanceVendorId,
        String engineerId,
        String engineerName,
        java.time.Instant maintenanceAt,
        String result,
        String exceptionNote,
        String sourceTerminal
) {
}
