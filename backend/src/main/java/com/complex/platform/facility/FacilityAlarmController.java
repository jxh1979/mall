package com.complex.platform.facility;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.FacilityAlarmDispositionRequest;
import com.complex.platform.facility.dto.FacilityAlarmDutyCancelRequest;
import com.complex.platform.facility.dto.FacilityAlarmDutyRequest;
import com.complex.platform.facility.dto.FacilityAlarmPolicyRequest;
import com.complex.platform.facility.dto.FacilityAlarmTakeOverRequest;
import com.complex.platform.facility.dto.FacilityFaultRuleActionRequest;
import com.complex.platform.facility.dto.FacilityFaultRuleRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HP-F01 设备告警接警 API：响应时限矩阵、接警值守安排、接警记录、我的接警与明确接手、处置要求记录、故障处置规则。 */
@RestController
@RequestMapping("/api/v1/facility")
public class FacilityAlarmController {

    private static final String MANAGE = "facility.alarm.manage";
    private static final String TAKE_OVER = "facility.alarm.take_over";
    private static final String CONFIRM_RULE = "facility.fault_rule.confirm";

    private final FacilityAlarmService service;
    private final AuthorizationService authorizationService;

    public FacilityAlarmController(FacilityAlarmService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/alarms/policies")
    public ApiResponse<FacilityAlarmPolicyRecord> savePolicy(@RequestBody FacilityAlarmPolicyRequest request) {
        authorizationService.requirePermission(MANAGE);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.savePolicy(request));
    }

    @GetMapping("/alarms")
    public ApiResponse<PageResponse<FacilityAlarmRecord.Detail>> list() {
        authorizationService.requirePermission(MANAGE);
        return ApiResponse.ok(service.listAlarms());
    }

    @GetMapping("/alarms/mine")
    public ApiResponse<PageResponse<FacilityAlarmRecord>> mine() {
        authorizationService.requirePermission(TAKE_OVER);
        return ApiResponse.ok(service.listMine());
    }

    @PostMapping("/alarms/{alarm_id}/take-over")
    public ApiResponse<FacilityAlarmRecord> takeOver(@PathVariable("alarm_id") String alarmId,
            @RequestBody FacilityAlarmTakeOverRequest request) {
        authorizationService.requirePermission(TAKE_OVER);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.takeOver(alarmId, request));
    }

    @GetMapping("/alarm-duties")
    public ApiResponse<PageResponse<FacilityAlarmDutyRecord>> listDuties(
            @RequestParam(value = "project_id", required = false) String projectId) {
        authorizationService.requirePermission(MANAGE);
        return ApiResponse.ok(service.listDuties(projectId));
    }

    @PostMapping("/alarm-duties")
    public ApiResponse<FacilityAlarmDutyRecord> assignDuty(@RequestBody FacilityAlarmDutyRequest request) {
        authorizationService.requirePermission(MANAGE);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignDuty(request));
    }

    @PostMapping("/alarm-duties/{duty_id}/cancel")
    public ApiResponse<FacilityAlarmDutyRecord> cancelDuty(@PathVariable("duty_id") String dutyId,
            @RequestBody FacilityAlarmDutyCancelRequest request) {
        authorizationService.requirePermission(MANAGE);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.cancelDuty(dutyId, request));
    }

    @PostMapping("/alarms/{alarm_id}/disposition")
    public ApiResponse<FacilityAlarmRecord> recordDisposition(@PathVariable("alarm_id") String alarmId,
            @RequestBody FacilityAlarmDispositionRequest request) {
        authorizationService.requirePermission(TAKE_OVER);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordDisposition(alarmId, request));
    }

    /** 维保单位（外包账号）凭确认权看本项目规则；平台侧凭接警管理权看全部。 */
    @GetMapping("/fault-rules")
    public ApiResponse<PageResponse<FacilityAlarmRecord.FaultRule>> listFaultRules(
            @RequestParam(value = "project_id", required = false) String projectId) {
        authorizationService.requirePermission(
                authorizationService.isOutsourcedVendor(authorizationService.currentActor()) ? CONFIRM_RULE : MANAGE);
        return ApiResponse.ok(service.listFaultRules(projectId));
    }

    @PostMapping("/fault-rules")
    public ApiResponse<FacilityAlarmRecord.FaultRule> createFaultRule(@RequestBody FacilityFaultRuleRequest request) {
        authorizationService.requirePermission(MANAGE);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createFaultRule(request));
    }

    @PostMapping("/fault-rules/{rule_id}/confirm")
    public ApiResponse<FacilityAlarmRecord.FaultRule> confirmFaultRule(@PathVariable("rule_id") String ruleId,
            @RequestBody FacilityFaultRuleActionRequest request) {
        authorizationService.requirePermission(CONFIRM_RULE);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmFaultRule(ruleId, request));
    }

    @PostMapping("/fault-rules/{rule_id}/retire")
    public ApiResponse<FacilityAlarmRecord.FaultRule> retireFaultRule(@PathVariable("rule_id") String ruleId,
            @RequestBody FacilityFaultRuleActionRequest request) {
        authorizationService.requirePermission(MANAGE);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.retireFaultRule(ruleId, request));
    }
}
