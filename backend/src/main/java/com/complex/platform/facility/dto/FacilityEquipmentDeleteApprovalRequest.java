package com.complex.platform.facility.dto;

/** 设施设备删除审批：decision=approved|rejected；comment 可空。 */
public record FacilityEquipmentDeleteApprovalRequest(String decision, String comment) {
}
