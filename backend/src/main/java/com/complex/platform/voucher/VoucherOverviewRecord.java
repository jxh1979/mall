package com.complex.platform.voucher;

import java.time.Instant;

/**
 * "全部券"聚合只读视图行（读层并入，不搬表）：
 * source=unified（统一台账券实例，剩余次数 0/1）| pass_partner（通票第三方合作券＝V282 配置的权益实例，
 * 计次模型，remainingTimes=总次数-已用次数；状态为权益侧原状态，兼容映射不硬套）。
 */
public record VoucherOverviewRecord(
        String id,
        String source,
        String name,
        String voucherKind,
        String customerId,
        String status,
        Integer remainingTimes,
        Instant expiresAt,
        // 面额/折扣（客人端展示；统一券取定义面额，通票第三方券为 null）与核销方式（客人端「核销方式说明」）。
        // 内部结算价（成本）刻意不入此聚合视图——客人/店员天然不可见。
        Long faceValueCent,
        String redeemMethod,
        // V312/#29：核销码（统一台账券实例的 code，供客人端「使用」时出示给店员/核销点扫码；
        // 通票第三方券为 null，其核销走合作方系统）。非成本字段，客人可见其本人券码。
        String code
) {
}
