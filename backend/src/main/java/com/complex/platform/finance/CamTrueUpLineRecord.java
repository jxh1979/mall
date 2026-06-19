package com.complex.platform.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * CAM true-up 逐户差额行（P2#④）。
 * <p>diffAmountCent = actualAllocatedAmountCent - prepaidAmountCent（带符号）：
 * <ul>
 *   <li>&gt; 0：少收 → 向商户补收应收（ar_ap direction=receivable）；</li>
 *   <li>&lt; 0：多收 → 向商户退还应付（ar_ap direction=payable）；</li>
 *   <li>= 0：无差额，不生成台账项（arApItemId 为空）。</li>
 * </ul>
 */
public record CamTrueUpLineRecord(
        String id,
        String runId,
        @JsonIgnore String merchantId,
        int weightBps,
        long actualAllocatedAmountCent,
        long prepaidAmountCent,
        long diffAmountCent,
        String arApItemId,
        Instant createdAt
) {
}
