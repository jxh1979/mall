package com.complex.platform.finance.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * P1#14 CAM（公共区域维护费）权重分摊：按各商户权重分摊总额，逐商户生成应收 ar_ap_items(source_type='cam')。
 * <p>allocated = totalAmountCent × weight_bps / 10000（末位差额归入最后一个商户，保证逐分守恒）。
 */
public record CamAllocationRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String period,
        Long totalAmountCent,
        LocalDate dueDate,
        List<CamShare> shares
) {
    /** 单个商户的分摊份额（权重以基点计，批次内合计应为 10000）。 */
    public record CamShare(
            String merchantId,
            Integer weightBps
    ) {
    }
}
