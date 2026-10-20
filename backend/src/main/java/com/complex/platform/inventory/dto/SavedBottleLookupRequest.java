package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/** FS-016 顺带：按手机号检索存酒（BSM L1073 / PGM L878）；手机号属 PII，走 POST body 不进 URL（AUD-020 同口径）。 */
public record SavedBottleLookupRequest(
        String phone
) {
}
