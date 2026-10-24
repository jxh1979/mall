package com.complex.platform.facility.dto;

// 文件版本：V0.01

/**
 * 工单处理/验收/回访/挂起动作请求。
 * customerFeedback：ROOT-03 clients-R8（BSM 第 1379 行「客户关怀部记录问题解决方案及客人反馈后可以关闭」）——关闭客人投诉时必填的客人反馈。
 */
public record FacilityWorkOrderActionRequest(
        String resolution,
        Integer satisfaction,
        String note,
        String idempotencyKey,
        String customerFeedback) {

    /** 既有 4 参调用点（非客人投诉）不带客人反馈。 */
    public FacilityWorkOrderActionRequest(String resolution, Integer satisfaction, String note, String idempotencyKey) {
        this(resolution, satisfaction, note, idempotencyKey, null);
    }
}
