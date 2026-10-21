package com.complex.platform.facility.dto;

// 文件版本：V0.01

/**
 * HP-F02（BSM L1385「责任人不在岗或本人负责此次维修时，由物业主管指定其他人员并留痕」）：
 * 物业主管为自动生成的大型维修工单指定提单确认人。
 */
public record FacilityWorkOrderConfirmerRequest(
        String confirmerId,
        String note,
        String idempotencyKey) {}
