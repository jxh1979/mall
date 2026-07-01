package com.complex.platform.partner;

/** 外包机构管理员视角：单个下属的工作完成情况汇总（被派工单/巡检的 完成数/总数）。 */
public record PartnerSubordinateCompletionRecord(
        String subordinateId,
        String username,
        String status,
        int workOrderTotal,
        int workOrderCompleted,
        int inspectionTotal,
        int inspectionCompleted) {
}
