package com.complex.platform.common.api;

import java.time.Instant;
import java.util.UUID;

/**
 * 统一响应包络。Jackson SNAKE_CASE 序列化 → success / code / message / data / request_id / server_time / timestamp，
 * 对齐 OpenAPI 契约要求的 success/request_id/server_time（此前缺失，本次补齐）。
 * timestamp 保留以向后兼容既有读取方。request_id 当前为每响应生成 UUID（请求级关联 filter 为后续增强）。
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String detailCode,
        String message,
        T data,
        String requestId,
        Instant serverTime,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        Instant now = Instant.now();
        return new ApiResponse<>(true, "OK", null, "success", data, UUID.randomUUID().toString(), now, now);
    }

    /**
     * 错误响应（C2 错误码归一）：{@code code} 为 OpenAPI 10 枚举之一（稳定分支码），
     * {@code detailCode} 为细粒度业务错误码（前端可选下钻/排查），二者由 GlobalExceptionHandler 统一派生。
     */
    public static <T> ApiResponse<T> error(String code, String detailCode, String message) {
        Instant now = Instant.now();
        return new ApiResponse<>(false, code, detailCode, message, null, UUID.randomUUID().toString(), now, now);
    }
}
