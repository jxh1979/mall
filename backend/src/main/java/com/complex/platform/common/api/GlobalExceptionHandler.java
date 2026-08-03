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
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(exception.status());
        // 限速（429）带 Retry-After 头：无论来自拦截器（IP 维度）还是服务层（账号/手机号维度），统一在此写头。
        if (exception.retryAfterSeconds() != null) {
            builder.header("Retry-After", String.valueOf(exception.retryAfterSeconds()));
        }
        // 展示文案一律中文（业务 HTML 铁律）：抛点保留英文字段键给日志排障，只在写响应体时翻译。
        log.debug("api error: code={} raw_message={}", exception.code(), exception.getMessage());
        return builder.body(ApiResponse.error(
                topLevelCode(exception.status(), exception.code()),
                exception.code(),
                FieldLabels.translate(exception.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> FieldLabels.beanFieldError(error.getField(), error.getDefaultMessage()))
                .distinct()
                .collect(Collectors.joining("；"));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "VALIDATION_FAILED",
                        message.isBlank() ? FieldLabels.FALLBACK : message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        // 不透传 getMessage（可能含内部约束/参数名）；返回固定文案，避免内部实现细节泄露。
        log.warn("constraint violation: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "VALIDATION_FAILED", FieldLabels.FALLBACK));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "REQUEST_BODY_INVALID", "请求内容格式不正确，请检查后重新提交"));
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
                        FieldLabels.translate(exception.getParameterName() + " is required")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        log.warn("method not supported: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("PARAM_INVALID", "METHOD_NOT_ALLOWED", "该接口不支持此请求方式"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        // 不透传 getMessage（可能含内部实现细节）；记录服务端日志，对外返回固定文案。
        log.warn("illegal argument: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "BAD_REQUEST", "请求参数有误，请检查后重新提交"));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException exception) {
        log.warn("no resource found: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "NOT_FOUND", "请求的内容不存在或已被删除"));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(org.springframework.web.HttpMediaTypeNotSupportedException exception) {
        log.warn("unsupported media type: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("PARAM_INVALID", "UNSUPPORTED_MEDIA_TYPE", "该接口不支持此内容格式"));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingHeader(org.springframework.web.bind.MissingRequestHeaderException exception) {
        log.warn("missing request header: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("PARAM_INVALID", "VALIDATION_FAILED", "请求缺少必需的信息，请刷新后重试"));
    }

    /**
     * 唯一/主键约束冲突（如重复创建同键配置、并发抢占）此前掉进兜底 Exception 处理器返回 500;
     * 契约上属状态冲突,应为 409 STATE_CONFLICT。仅拦精确的 DuplicateKeyException(SQLState 23505),
     * 不拦父类 DataIntegrityViolationException——NOT NULL/CHECK/FK 违例多为服务端 bug,仍应落 500 +
     * error 日志保留可观测性。不透传 getMessage(可能含约束名),返回固定文案。
     */
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    ResponseEntity<ApiResponse<Void>> handleDuplicateKey(org.springframework.dao.DuplicateKeyException exception) {
        log.warn("duplicate key violation: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("STATE_CONFLICT", "DUPLICATE_KEY", "已存在相同的记录，请勿重复提交"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "INTERNAL_ERROR", "服务出现异常，请稍后重试"));
    }
}
