package com.complex.platform.ops;

/** 预览（dry-run）请求：清理范围 + 条件（已存档超过 olderThanDays 天）。 */
public record DataCleanupPreviewRequest(String scopeType, Integer olderThanDays) {
}
