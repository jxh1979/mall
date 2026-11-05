package com.complex.platform.finance.dto;

// 文件版本：V0.01

/**
 * 第十八批 R 段第 143 条：按日结号办理的动作（143-R1 确认、锁定；143-R2 五类最终标记；143-R5 归档）共用的请求体。
 * category 只在五类最终标记时必填；不收备注（三份 HTML 与裁决都没有，页面也不发）。
 */
public record DailyCloseActionRequest(String idempotencyKey, String category) {
}
