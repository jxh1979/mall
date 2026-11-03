package com.complex.platform.amusement;

// 文件版本：V0.01

import java.util.List;

/**
 * 第十八批 N 段第 120 条 120-R5（BSM:991「查看游乐设施状态：开放、排队、容量、暂停、关闭、故障、维护或安全提醒。」；BSM:1268）：
 * 游乐设施状态看板一行——开放状态、容量、在玩人数（项目维容量快照）、排队人数（排队中）、维护中设备数、未结故障数、生效安全提醒标题。
 * 客人可看，不含客人编号与排队号。
 */
public record AmusementProjectStatusRecord(
        String projectId,
        String projectName,
        String openStatus,
        int capacity,
        int occupiedCount,
        int waitingCount,
        int maintenanceDeviceCount,
        int faultCount,
        List<String> safetyNotices
) {
}
