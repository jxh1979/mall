package com.complex.platform.facility.dto;

/** 设施设备存档请求。reason=存档理由(存档时必填,业务HTML 2026-07-07)。 */
public record FacilityEquipmentArchiveRequest(String reason) {
}
