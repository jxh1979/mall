package com.complex.platform.customer.dto;

/** 积分转赠风控配置请求（后台，防套现：日限额 / 日次数）。 */
public record PointsTransferLimitRequest(
        String projectId,
        Long dailyPointsLimit,
        Integer dailyCountLimit,
        String status
) {
}
