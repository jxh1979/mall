package com.complex.platform.governance.dto;
// 文件版本：V0.01

/** 第十批 TK-27 待清台提醒时限配置请求：值、下限、上限三者同填且值在闭区间（分钟）。 */
public record MerchantPendingClearAlertRequest(String idempotencyKey, Integer valueMinutes, Integer minMinutes, Integer maxMinutes) {
}
