package com.complex.platform.analytics;

/**
 * 经营复盘锁定请求（V216）：project_id 不取请求体，由 token scope 派生（AC-ISO-2）。
 * domain 对应前端业务域筛选（all/具体域），metricCount=当前筛选下的指标条数快照。
 */
public record BusinessReviewLockRequest(
        String idempotencyKey,
        String domain,
        String operatorNote,
        Integer metricCount
) {
}
