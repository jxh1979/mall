package com.complex.platform.voucher;

// 文件版本：V0.01

import java.time.Instant;

/** ROOT-03-R32（BSM 第 585 行）客人券包动态核销码：明文只在本响应出现，过期即废。 */
public record VoucherRedeemCodeResponse(String voucherInstanceId, String code, Instant expiresAt, int refreshIntervalSeconds) {
}
