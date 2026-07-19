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
        Instant expiresAt
) {
}
