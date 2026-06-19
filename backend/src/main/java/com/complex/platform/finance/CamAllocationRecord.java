package com.complex.platform.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * CAM 权重分摊一行（P1#14）。allocatedAmountCent = totalAmountCent × weightBps / 10000（末位差额归最后一户）。
 */
public record CamAllocationRecord(
        String id,
        String allocationBatchId,
        @JsonIgnore String merchantId,
        String period,
        long totalAmountCent,
        int weightBps,
        long allocatedAmountCent,
        String arApItemId,
        Instant createdAt
) {
}
