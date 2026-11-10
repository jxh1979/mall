package com.complex.platform.finance.dto;

// 文件版本：V0.01

/**
 * 第十八批 Z 段第 188 条 188-R1（第十八批裁决 41「财务中心按综合体＋月份在平台办理；……四流一致由财务核对后勾选确认留痕」）：
 * 封账请求——综合体 projectId、月份 periodMonth（YYYY-MM）、财务核对后勾选的四流一致确认 fourFlowsConfirmed（须为 true）。
 */
public record PeriodCloseRequest(String idempotencyKey, String projectId, String periodMonth, Boolean fourFlowsConfirmed) {
}
