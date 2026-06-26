package com.complex.platform.facility.dto;

/** 工单 SLA/验收策略配置请求。 */
public record FacilityWorkOrderPolicyRequest(
        String workOrderType,
        String priority,
        Integer responseMinutes,
        Integer handleMinutes,
        Boolean requiresAcceptance,
        String acceptanceBy,
        Boolean active,
        String idempotencyKey) {}
