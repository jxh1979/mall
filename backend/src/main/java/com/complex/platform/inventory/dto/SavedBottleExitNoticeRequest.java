package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/** FS-017 门店退出前清点并通知客人原店取回。 */
public record SavedBottleExitNoticeRequest(
        String idempotencyKey,
        String operatorNote,
        String merchantId
) {
}
