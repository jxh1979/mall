package com.complex.platform.finance;

import java.time.Instant;
import java.util.List;

/**
 * CAM 年度 true-up 批次（P2#④）：按 settlementPeriod 归集，
 * 实际发生总额(actualTotalAmountCent) vs 已预收合计(prepaidTotalAmountCent) → 逐户差额行 lines（多退少补）。
 */
public record CamTrueUpRunRecord(
        String id,
        String settlementPeriod,
        long actualTotalAmountCent,
        long prepaidTotalAmountCent,
        String status,
        Instant createdAt,
        List<CamTrueUpLineRecord> lines
) {
}
