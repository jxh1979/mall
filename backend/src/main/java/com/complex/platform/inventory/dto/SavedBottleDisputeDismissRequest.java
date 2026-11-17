package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/** 第十八批 AP 段 254-R6：存酒争议不成立、门店主管恢复存酒中（第十八批裁决 58）；请求体只收幂等键。 */
public record SavedBottleDisputeDismissRequest(String idempotencyKey) {
}
