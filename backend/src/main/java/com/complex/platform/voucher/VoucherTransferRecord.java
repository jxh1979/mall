package com.complex.platform.voucher;

import java.time.Instant;

/**
 * V313 券包会员间转赠记录（状态机流水本身，不另建历史表）。范式对齐 V287 门票转赠：
 * status: pending（发起冻结）-> claimed（领取，券归属改受赠人）/ revoked（出让人撤回）/ voided（系统作废：券核销/作废/过期）。
 * 并发裁决全落同一行 CAS（UPDATE ... WHERE status='pending'），数据库行锁天然串行化，先提交者赢。
 * 双花窗口＝同一券实例同时最多一笔 pending（部分唯一索引 uq_voucher_transfers_pending 物理保证）。
 */
public record VoucherTransferRecord(
        String id,
        String voucherInstanceId,
        String initiatorCustomerId,
        String channel,               // phone | link（二维码/微信同为 link）
        String receiverPhone,         // phone 渠道：密文（PiiCipher）
        String receiverPhoneHash,     // phone 渠道：盲索引（BlindIndex）
        String receiverPhoneMasked,   // phone 渠道：展示掩码
        String claimCode,             // link 渠道：一次性高熵领取码
        String receiverCustomerId,    // 领取成功回填
        String status,
        String voidReason,
        Instant createdAt,
        Instant claimedAt,
        Instant revokedAt,
        Instant voidedAt,
        Instant updatedAt
) {
}
