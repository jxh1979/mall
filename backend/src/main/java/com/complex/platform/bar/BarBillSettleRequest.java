package com.complex.platform.bar;

// 文件版本：V0.03

import java.util.List;

/**
 * 结账请求。第十批 TK-28：voucherCode / voucherInstanceId 二选一可选——本次结账抵扣一张本商户折扣券
 * （操作员裁决「一次结账一张」，两项同填即拒绝）。
 * 第十一批 TK-04-R2：memberPhone 可选——收银现场录入会员号（即注册手机号）识别本单会员，走客人查询专门通道留痕（BSM L822、PGM L878）。
 */
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
        String smsCode,
        String voucherCode,
        String voucherInstanceId,
        String memberPhone
) {
    public BarBillSettleRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
            String paymentId, Long settlementAmountCent, String paymentChannel, String cashierStaffId,
            String posTransactionNo, String phone, String smsCode, String voucherCode, String voucherInstanceId) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, paymentId, settlementAmountCent, paymentChannel,
                cashierStaffId, posTransactionNo, phone, smsCode, voucherCode, voucherInstanceId, null);
    }

    public BarBillSettleRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
            String paymentId, Long settlementAmountCent, String paymentChannel,
            String cashierStaffId, String posTransactionNo, String phone, String smsCode) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, paymentId, settlementAmountCent,
                paymentChannel, cashierStaffId, posTransactionNo, phone, smsCode, null, null);
    }

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
