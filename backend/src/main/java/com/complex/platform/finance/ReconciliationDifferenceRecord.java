package com.complex.platform.finance;

import java.time.Instant;

/**
 * 对账差异（P0#7 / R6-D07 强制化）。
 * <p>expectedAmountCent：平台侧系统取数（不接受请求体手填）；actualAmountCent：第三方侧申报值
 * （未接入前 source=manual 录入并进复核）；source：system|manual。amountCent 为差额（expected−actual），
 * 由服务层从系统取数推导，留作展示与历史兼容。
 */
public record ReconciliationDifferenceRecord(
        String id,
        String batchId,
        String differenceType,
        String businessRef,
        long amountCent,
        Long expectedAmountCent,
        Long actualAmountCent,
        String source,
        String status,
        Instant createdAt
) {

    /**
     * 兼容构造（V105 前的 7 参列）：无 expected/actual、source 默认 system。
     * 仅供历史调用点与测试双子过渡使用，新写端点应使用全参构造并显式给出系统取数。
     */
    public ReconciliationDifferenceRecord(
            String id,
            String batchId,
            String differenceType,
            String businessRef,
            long amountCent,
            String status,
            Instant createdAt
    ) {
        this(id, batchId, differenceType, businessRef, amountCent, null, null, "system", status, createdAt);
    }
}
