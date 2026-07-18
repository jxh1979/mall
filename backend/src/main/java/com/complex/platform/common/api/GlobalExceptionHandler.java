package com.complex.platform.common.api;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // C2 错误码归一：OpenAPI ErrorResponse.code 的 10 个稳定枚举（顶层分支码）。
    private static final Set<String> TOP_LEVEL_CODES = Set.of(
            "PARAM_INVALID", "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND", "STATE_CONFLICT",
            "RISK_REVIEW_REQUIRED", "IDEMPOTENCY_CONFLICT", "RATE_LIMITED", "PROVIDER_UNAVAILABLE", "INTERNAL_ERROR");

    /**
     * 将细粒度业务错误码归一到 10 个顶层枚举之一：
     * 细码本身已是枚举（如 IDEMPOTENCY_CONFLICT/RISK_REVIEW_REQUIRED/RATE_LIMITED/PROVIDER_UNAVAILABLE）→ 直接用；
     * 否则按 HTTP 状态映射类目。细码经 detail_code 子字段返回，前端可下钻。
     */
    static String topLevelCode(HttpStatus status, String detailCode) {
        if (detailCode != null && TOP_LEVEL_CODES.contains(detailCode)) {
            return detailCode;
        }
        return switch (status) {
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "STATE_CONFLICT";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            case BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> "PROVIDER_UNAVAILABLE";
            case BAD_REQUEST, METHOD_NOT_ALLOWED, UNPROCESSABLE_ENTITY -> "PARAM_INVALID";
            case INTERNAL_SERVER_ERROR -> "INTERNAL_ERROR";
            default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "PARAM_INVALID";
        };
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        // 顶层 code = 归一枚举；detail_code = 原细码（业务码），保留细粒度供前端下钻/排查。
        return ResponseEntity
                .status(exception.status())
                .body(ApiResponse.error(
                        topLevelCode(exception.status(), exception.code()),
                        exception.code(),
                        exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "VALIDATION_FAILED",
                        message.isBlank() ? "validation failed" : message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        // 不透传 getMessage（可能含内部约束/参数名）；返回固定文案，避免内部实现细节泄露。
        log.warn("constraint violation: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "VALIDATION_FAILED", "request validation failed"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "REQUEST_BODY_INVALID", "request body is invalid"));
    }

    /**
     * Critical 修复(2026-07-03,request_id f5a9eddd):必填 query 参数缺失(如
     * GET /api/v1/leasing/follow-ups 不带 ref_type/ref_id)此前无专属处理器,
     * 掉进兜底 Exception 处理器返回 500;契约上参数缺失=调用方错误,应为 400 PARAM_INVALID。
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            org.springframework.web.bind.MissingServletRequestParameterException exception) {
        log.warn("missing request parameter: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "VALIDATION_FAILED",
                        "required parameter '" + exception.getParameterName() + "' is missing"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        log.warn("method not supported: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("PARAM_INVALID", "METHOD_NOT_ALLOWED", "http method is not allowed for this endpoint"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        // 不透传 getMessage（可能含内部实现细节）；记录服务端日志，对外返回固定文案。
        log.warn("illegal argument: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "BAD_REQUEST", "request is invalid"));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException exception) {
        log.warn("no resource found: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "NOT_FOUND", "requested resource was not found"));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(org.springframework.web.HttpMediaTypeNotSupportedException exception) {
        log.warn("unsupported media type: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("PARAM_INVALID", "UNSUPPORTED_MEDIA_TYPE", "content type is not supported for this endpoint"));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingHeader(org.springframework.web.bind.MissingRequestHeaderException exception) {
        log.warn("missing request header: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "VALIDATION_FAILED", "a required request header is missing"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "INTERNAL_ERROR", "internal server error"));
    }
}
