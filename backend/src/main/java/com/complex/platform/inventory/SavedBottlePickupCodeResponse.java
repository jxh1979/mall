package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;

/** FS-016：客人端取动态取酒码的返回（明文码只出现在本响应，不落库）。 */
public record SavedBottlePickupCodeResponse(
        String savedBottleId,
        String code,
        Instant expiresAt,
        int refreshIntervalSeconds
) {
}
