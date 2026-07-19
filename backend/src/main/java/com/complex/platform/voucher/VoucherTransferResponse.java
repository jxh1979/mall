package com.complex.platform.voucher;

import java.time.Instant;

/**
 * V313 券转赠对客安全投影：不含密文/盲索引；claim_code 仅回给发起人（出让人自己）本人以便转发。
 * voucherName＝券定义名称（展示用）。
 */
public record VoucherTransferResponse(
        String id,
        String voucherInstanceId,
        String voucherName,
        String channel,
        String status,
        String claimCode,             // 仅发起人视图携带（受赠人/预览不给）
        String receiverPhoneMasked,   // phone 渠道展示掩码
        String voidReason,
        Instant createdAt,
        Instant claimedAt
) {

    static VoucherTransferResponse of(VoucherTransferRecord r, String voucherName, boolean includeClaimCode) {
        return new VoucherTransferResponse(
                r.id(), r.voucherInstanceId(), voucherName, r.channel(), r.status(),
                includeClaimCode ? r.claimCode() : null,
                r.receiverPhoneMasked(), r.voidReason(), r.createdAt(), r.claimedAt());
    }
}
