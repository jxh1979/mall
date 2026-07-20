package com.complex.platform.access.staff;

import java.time.Instant;

/**
 * BYOD 二期 Wave1（2026-07-19 拍板·员工工资条自查）：本人某月一条提成明细。
 * 提成来自两处台账，合并后按 sourceType 归类展示（酒水/卡座提成 vs 服务点单份额）：
 *   - staff_commission_ledger：source_type ∈ {service_order, ...}（服务点单等）
 *   - bar_commission_calculations：酒水/卡座提成，映射 sourceType=bar_table
 * 金额单位：分。仅本人可见（按 token 员工 id 过滤，见 StaffService#listMyCommissions）。
 */
public record StaffCommissionLineRecord(
        String sourceType,
        String sourceId,
        long amountCent,
        String note,
        Instant createdAt
) {
}
