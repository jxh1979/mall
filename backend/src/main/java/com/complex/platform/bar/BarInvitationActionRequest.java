package com.complex.platform.bar;

/** V155 订卡邀请函签发/吊销请求（staff）：仅需幂等键 + 可选操作备注。 */
public record BarInvitationActionRequest(
        String idempotencyKey,
        String operatorNote
) {
}
