package com.complex.platform.integration;

import java.util.List;

/**
 * 自动对账结果（P0#11）：批次 + 自动生成的差异列表。
 *
 * <p>dryRun=true（开关 off 或调用方显式）时 batchId 为 null、未落库，仅返回比对结果预览（不伪造对账数据）。
 */
public record ChannelReconciliationResult(
        String batchId,
        boolean dryRun,
        int expectedCount,
        int actualCount,
        int differenceCount,
        long totalDifferenceCent,
        List<ReconciliationDifferenceRecord> differences
) {
}
