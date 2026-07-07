package com.complex.platform.facility.dto;

/** 设施设备删除申请（数据删除 Tier-B）：须先存档；reason 记录删除理由供审批人参考。 */
public record FacilityEquipmentDeleteRequest(String reason) {
}
