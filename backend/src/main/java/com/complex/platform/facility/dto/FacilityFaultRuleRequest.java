package com.complex.platform.facility.dto;

// 文件版本：V0.01

/**
 * 故障处置规则建档（HP-F01 R7）：按设备（equipment_id，项目与分类取设备台账）或按项目×设备四组台账分类
 * （project_id＋equipment_type）登记某类故障场景及其处置要求；建档后须设备专业负责人与维保单位两方确认才生效。
 */
public record FacilityFaultRuleRequest(
        String projectId,
        String equipmentId,
        String equipmentType,
        String faultScenario,
        String handlingRequirement,
        String idempotencyKey) {}
