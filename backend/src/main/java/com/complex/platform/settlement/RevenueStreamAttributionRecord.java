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
        Instant updatedAt,
        /**
         * V355 收款方类型:{@code entity}=归某个经营主体(managementEntityId 必填);
         * {@code per_merchant}=归产生该笔收入的各商户自己(订台低消/二次消费/额外酒水、非散台服务费,
         * managementEntityId 须为 null)。缺省 entity,与 DB DEFAULT 一致。
         */
        String beneficiaryKind
) {
    /** 归主体的简写构造(beneficiaryKind=entity),与 V355 之前的调用点等价。 */
    public RevenueStreamAttributionRecord(String id, String projectId, String revenueStream,
                                          String managementEntityId, String status,
                                          String updatedBy, Instant updatedAt) {
        this(id, projectId, revenueStream, managementEntityId, status, updatedBy, updatedAt, "entity");
    }
}
