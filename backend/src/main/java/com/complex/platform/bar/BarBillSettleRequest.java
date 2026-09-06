package com.complex.platform.bar;

// 文件版本：V0.01

import java.util.List;

public record BarBillSettleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String paymentId,
        Long settlementAmountCent,
        String paymentChannel,
        String cashierStaffId,
        String posTransactionNo,
        String phone,
        String smsCode
) {
    public BarBillSettleRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
            String paymentId, Long settlementAmountCent, String paymentChannel,
            String cashierStaffId, String posTransactionNo) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, paymentId, settlementAmountCent,
                paymentChannel, cashierStaffId, posTransactionNo, null, null);
    }

    /** 向后兼容构造器（A2/A3 前签名）：既有调用方零改动。 */
    public BarBillSettleRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String paymentId,
            Long settlementAmountCent,
            String paymentChannel
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, paymentId, settlementAmountCent,
                paymentChannel, null, null);
    }
}
