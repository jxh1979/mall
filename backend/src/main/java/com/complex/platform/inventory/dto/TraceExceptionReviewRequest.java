package com.complex.platform.inventory.dto;

/**
 * R5 T17 异常复核：主管 approve/reject 闭合异常 + 留结论。
 * decision：approved/rejected。reviewerUserId 取自认证上下文，不取请求体。
 */
public record TraceExceptionReviewRequest(
        String idempotencyKey,
        String decision,
        String reason
) {
}
