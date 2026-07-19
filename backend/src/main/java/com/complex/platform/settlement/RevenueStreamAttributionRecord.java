package com.complex.platform.settlement;

import java.time.Instant;

/**
 * 六业态收入归属一行(revenue_stream_attribution):某(项目, 业态收入流) → 对应 L3 管理公司/业态主体。
 * 口径:terminology-canonical-v1.0.md §三。projectId=null 为平台默认口径。
 */
public record RevenueStreamAttributionRecord(
        String id,
        String projectId,
        String revenueStream,
        String managementEntityId,
        String status,
        String updatedBy,
        Instant updatedAt
) {
}
