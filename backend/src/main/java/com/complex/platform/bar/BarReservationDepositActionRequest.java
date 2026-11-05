package com.complex.platform.bar;

import java.util.List;

/**
 * R5 T16：订台定金支付/到店核销动作请求。
 * 第十八批 S 段第 169 条 169-R2（BSM:1066「POS（记流水号+收款人）」）：posTransactionNo——登记定金选 POS 时必填的 POS 流水号。
 */
public record BarReservationDepositActionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String channel,
        String posTransactionNo
) {
    public BarReservationDepositActionRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
            String channel) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, channel, null);
    }
}
