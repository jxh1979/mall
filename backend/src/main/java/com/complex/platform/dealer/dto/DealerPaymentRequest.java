package com.complex.platform.dealer.dto;

import java.time.Instant;

/** method：bank_transfer / platform_online（ENUM:176；现金与个人账户一律拒绝）。 */
public record DealerPaymentRequest(String idempotencyKey, String method, Long amountCent, Instant receivedAt, String referenceNo) {
}
