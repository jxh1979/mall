package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/** 第十八批 C 段第 17 条：登记吧台是否配有出酒量采集设备（BSM 第 1065 行）。 */
public record WarehousePourDeviceRequest(String idempotencyKey, Boolean pourDeviceEquipped) {
}
