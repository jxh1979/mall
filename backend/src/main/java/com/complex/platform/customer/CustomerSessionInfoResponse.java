package com.complex.platform.customer;

import java.time.Instant;

/** 3.3 免登录：会话恢复响应——只回身份与到期时间，不回传凭证本身（凭证在 httpOnly cookie 里）。 */
public record CustomerSessionInfoResponse(
        String customerId,
        String maskedPhone,
        Instant expiresAt
) {
}
