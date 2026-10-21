package com.complex.platform.facility;

// 文件版本：V0.01

/** 设备告警/故障事件：由设备台账发布，工单域监听转单并落接警单（HP-F01 带上设备项目、位置、四组台账分类、负责人与上报优先级）。 */
public record FacilityEquipmentAlarmedEvent(
        String equipmentId,
        String equipmentName,
        String alarmSource,
        String note,
        String projectId,
        String spaceId,
        String equipmentType,
        String responsibleUserId,
        String priority) {}
