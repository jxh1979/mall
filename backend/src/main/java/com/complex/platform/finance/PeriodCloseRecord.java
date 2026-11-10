package com.complex.platform.finance;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 第十八批 Z 段第 188 条 188-R1（第十八批裁决 41）：一条封账记录（finance_period_closes）。periodMonth 为 YYYY-MM；
 * 不含四流一致字段——记录存在即财务已核对四流一致并勾选确认。
 */
public record PeriodCloseRecord(String id, String projectId, String periodMonth, String closedBy, Instant closedAt) {
}
