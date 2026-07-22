package com.complex.platform.common;

import com.complex.platform.common.api.ApiException;

/**
 * 文本清洗 / 必填校验公共工具。收拢此前散落在 50+ 服务里逐字复制的
 * trimToNull / requireText 私有副本（语义完全一致）。
 */
public final class Strings {

    private Strings() {
    }

    /** null 或全空白 → null；否则返回 trim 后的值。 */
    public static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** 必填文本：trim 后为空则抛 VALIDATION_FAILED；否则返回 trim 后的值。 */
    public static String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }
}
