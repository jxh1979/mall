package com.complex.platform.facility;

/** Published when an abnormal inspection requires a rectification work order. */
public record FacilityInspectionAbnormalEvent(
        String taskId,
        String title,
        String abnormalNote,
        String projectId,
        String spaceId,
        String inspectorId) {}
