package com.complex.platform.facility.dto;

/** 设施设备删除审批结果：id + decision(approved|rejected) + 是否已物理删除。 */
public record FacilityEquipmentDeleteDecision(String id, String decision, boolean deleted) {
}
