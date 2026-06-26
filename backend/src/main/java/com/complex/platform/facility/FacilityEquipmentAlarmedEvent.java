package com.complex.platform.facility;

/** 设备告警/故障事件：由设备台账发布，工单域监听转单。 */
public record FacilityEquipmentAlarmedEvent(
        String equipmentId,
        String equipmentName,
        String alarmSource,
        String note) {}
