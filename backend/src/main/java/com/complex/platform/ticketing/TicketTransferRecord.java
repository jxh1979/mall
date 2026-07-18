package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * AUD-077 门票权益转赠记录(状态机流水本身,不另建历史表)。
 * status: pending(发起冻结) -> claimed(领取,受赠人成 holder) / revoked(撤回) / voided(系统作废:票到期/退款)。
 * 并发裁决全落同一行 CAS(UPDATE ... WHERE status='pending'),数据库行锁天然串行化,先提交者赢。
 */
public record TicketTransferRecord(
        String id,
        String parentTicketId,
        int slotIndex,
        String initiatorCustomerId,
        String channel,               // phone | link(微信/二维码同为 link)
        String receiverPhone,         // phone 渠道:密文(PiiCipher)
        String receiverPhoneHash,     // phone 渠道:盲索引(BlindIndex)
        String receiverPhoneMasked,   // phone 渠道:展示掩码
        String claimCode,             // link 渠道:一次性高熵领取码
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
