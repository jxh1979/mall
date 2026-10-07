package com.complex.platform.common.api;

import com.complex.platform.common.idempotency.IdempotentReplayContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * 统一响应包络。Jackson SNAKE_CASE 序列化 → success / code / message / data / request_id / server_time / timestamp，
 * 对齐 OpenAPI 契约要求的 success/request_id/server_time（此前缺失，本次补齐）。
 * timestamp 保留以向后兼容既有读取方。request_id 当前为每响应生成 UUID（请求级关联 filter 为后续增强）。
 *
 * <p>M723-F04（操作员 2026-09-25 确认句「同一次操作重复提交时，直接返回第一次的结果并提示已完成」）：本请求是重复提交、
 * 返回的是第一次的结果时（{@link IdempotentReplayContext#replayed}），{@code data} 仍是第一次的结果，{@code message} 提示已完成；
 * 查看、预览、查询请求（{@link IdempotentReplayContext#readOnly}）重复提交不提示。
 *
 * <p>M724（XM-ONESHOT-RETURN B02）：提示已完成的响应同时带机读标记 {@code replayed=true}；首次执行、被拒与查看类请求的响应不带该字段。
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String detailCode,
        String message,
        T data,
        String requestId,
        Instant serverTime,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean replayed
) {
    public static final String REPLAYED_MESSAGE = "该操作已完成（重复提交，返回首次结果）";

    public static <T> ApiResponse<T> ok(T data) {
        Instant now = Instant.now();
        IdempotentReplayContext request = IdempotentReplayContext.current();
        boolean replayed = request != null && request.replayed() && !request.readOnly();
        return new ApiResponse<>(true, "OK", null, replayed ? REPLAYED_MESSAGE : "success", data,
                UUID.randomUUID().toString(), now, now, replayed ? Boolean.TRUE : null);
    }

    /**
     * 错误响应（C2 错误码归一）：{@code code} 为 OpenAPI 10 枚举之一（稳定分支码），
     * {@code detailCode} 为细粒度业务错误码（前端可选下钻/排查），二者由 GlobalExceptionHandler 统一派生。
     */
    public static <T> ApiResponse<T> error(String code, String detailCode, String message) {
        Instant now = Instant.now();
        return new ApiResponse<>(false, code, detailCode, message, null, UUID.randomUUID().toString(), now, now, null);
    }
}
