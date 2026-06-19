package com.complex.platform.finance.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * P2#④ CAM 年度 true-up（多退少补）：对某 settlementPeriod 的已预收做年度对账。
 * <p>actualTotalAmountCent 为该归属期实际发生总额（权威实际值）；逐户 shares 给出实际权重；
 * 服务层据各户实际应摊与该期已预收（来自 cam_allocations 归集）之差生成 source_type='cam' 的应收/应付调整。
 */
public record CamTrueUpRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String settlementPeriod,
        Long actualTotalAmountCent,
        LocalDate dueDate,
        List<TrueUpShare> shares
) {
    /** 单户实际权重（基点，批次内合计应为 10000，与 CAM 预收权重口径一致）。 */
    public record TrueUpShare(
            String merchantId,
            Integer weightBps
    ) {
    }
}
