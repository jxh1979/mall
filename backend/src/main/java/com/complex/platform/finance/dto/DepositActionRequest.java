package com.complex.platform.finance.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * P1#14 保证金四动作台账：collect 收取 / refund 退还 / deduct 抵扣 / forfeit 没收。
 * <p>余额由服务层基于该商户最新台账余额推导：collect 增余额，refund/deduct/forfeit 减余额（不得为负）。
 * deduct/forfeit 可联动生成 ar_ap_items（抵扣应收/没收损益），由服务层决定。
 */
public record DepositActionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String merchantId,
        String action,
        @NotNull @Positive Long amountCent,
        String reason
) {
}
