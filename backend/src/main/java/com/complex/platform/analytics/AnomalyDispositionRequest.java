package com.complex.platform.analytics;

/**
 * 指标异常处置请求。
 *
 * <p>action ∈ acknowledge(已阅) / resolve(已处置) / false_positive(误报) / escalate(转告警);
 * note 在 resolve/false_positive/escalate 时必填(处置留痕/升级说明),acknowledge 时可选。
 */
public record AnomalyDispositionRequest(
        String idempotencyKey,
        String action,
        String note
) {
}
