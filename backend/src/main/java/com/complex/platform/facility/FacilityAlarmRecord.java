package com.complex.platform.facility;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

/**
 * HP-F01 接警单（V529 facility_alarms）：设备告警/故障自动建单时同事务落一张，按「告警类型×优先级」响应时限计时。
 * status 只有 awaiting_takeover（待明确接手）与 taken_over（已明确接手）；只有明确接手能改它，送达、已读、接听都不改；
 * 接手人班次结束而工单未关闭时回到 awaiting_takeover，待下一班明确接手。uncovered＝落单时无生效故障处置规则覆盖；
 * applied_rule_id／disposition_* 记录所按规则或当班专业处置人员判定的处置要求（V530）。
 */
public record FacilityAlarmRecord(
        String id,
        String equipmentId,
        String workOrderId,
        String projectId,
        String alarmType,
        String priority,
        String alarmSource,
        String note,
        String status,
        Instant raisedAt,
        Instant responseDueAt,
        int escalationRound,
        Instant fallbackToBackupAt,
        String holderUserId,
        String holderShiftId,
        Instant holderShiftEndAt,
        Instant takenOverAt,
        boolean uncovered,
        Instant createdAt,
        Instant updatedAt,
        String appliedRuleId,
        String dispositionNote,
        String dispositionBy,
        Instant dispositionAt) {

    /** 一次联系（系统消息/短信/语音）留痕；outcome 取通道真实结果。 */
    public record Attempt(String id, String alarmId, int round, String userId, String dutyRole, String channel,
            String outcome, String providerRef, String reason, Instant attemptedAt) {}

    /** 一次明确接手：确认人、班次、角色与时间；kind=first 首接，handover 换班后下一班接手。 */
    public record Takeover(String id, String alarmId, String userId, String shiftId, String dutyRole, String kind,
            String note, Instant takenOverAt) {}

    /** 后台接警记录：接警单＋逐次联系＋接手明细＋覆盖该设备的生效故障处置规则（空＝未覆盖）。 */
    public record Detail(FacilityAlarmRecord alarm, List<Attempt> attempts, List<Takeover> takeovers, List<FaultRule> rules) {}

    /**
     * 故障处置规则（V530 facility_fault_handling_rules）：设备专业负责人与维保单位两方确认后 active；
     * equipment_id 为空＝本项目该分类设备通用。
     */
    public record FaultRule(String id, String projectId, String equipmentId, String equipmentType, String faultScenario,
            String handlingRequirement, String status, String specialistConfirmedBy, Instant specialistConfirmedAt,
            String vendorConfirmedBy, Instant vendorConfirmedAt, String createdBy, Instant createdAt, Instant updatedAt) {}
}
