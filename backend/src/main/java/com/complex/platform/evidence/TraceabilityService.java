package com.complex.platform.evidence;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 全过程留痕三层模型内部记录服务（REQ-34/37/40）。业务动作（核销/卡座服务/出品/异常处理/财务调整）
 * 在各自 service 内调用本服务写入过程实例/步骤/服务责任链。隔离键 merchant_id 一律由 token 注入，绝不取入参。
 */
@Service
public class TraceabilityService {

    private final TraceabilityRepository repository;
    private final AuthorizationService authorizationService;

    public TraceabilityService(TraceabilityRepository repository, AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public TraceabilityProcessInstanceRecord recordProcessInstance(TraceabilityProcessInstanceRecord input) {
        String id = blankToId(input.id(), "bpi_");
        return repository.saveProcessInstance(new TraceabilityProcessInstanceRecord(
                id,
                input.processType(),
                input.businessObjectType(),
                input.businessObjectId(),
                input.projectId(),
                currentMerchantId(),
                input.venueId(),
                input.areaId(),
                input.locationPointId(),
                input.startedEventId(),
                input.currentStepCode(),
                input.status() == null ? "running" : input.status(),
                input.startedAt() == null ? Instant.now() : input.startedAt(),
                input.completedAt()));
    }

    @Transactional
    public TraceabilityStepRecord recordStep(TraceabilityStepRecord input) {
        String id = blankToId(input.id(), "bps_");
        return repository.saveStep(new TraceabilityStepRecord(
                id,
                input.processInstanceId(),
                input.stepCode(),
                input.stepName(),
                input.actorType(),
                input.actorId(),
                input.terminalType(),
                input.deviceId(),
                input.locationPointId(),
                input.eventId(),
                input.autoCaptured(),
                input.captureSource(),
                input.requiresManualReason(),
                input.result() == null ? "success" : input.result(),
                input.occurredAt() == null ? Instant.now() : input.occurredAt()));
    }

    @Transactional
    public TraceabilityResponsibilityRecord recordResponsibility(TraceabilityResponsibilityRecord input) {
        String id = blankToId(input.id(), "srr_");
        return repository.saveResponsibility(new TraceabilityResponsibilityRecord(
                id,
                input.orderId(),
                input.tableId(),
                input.customerId(),
                input.exceptionId(),
                input.primaryServiceStaffId(),
                input.assistStaffIds() == null ? List.of() : input.assistStaffIds(),
                input.salesStaffId(),
                input.firstHandlerStaffId(),
                input.refundedBy(),
                input.waivedBy(),
                input.approvedBy(),
                input.assignedBy(),
                input.shiftAssignmentId(),
                input.assignedEventId(),
                currentMerchantId(),
                input.serviceStartAt() == null ? Instant.now() : input.serviceStartAt(),
                input.serviceEndAt(),
                input.status() == null ? "active" : input.status()));
    }

    public Optional<TraceabilityProcessInstanceRecord> findProcessInstance(String id) {
        return repository.findProcessInstanceById(id);
    }

    public List<TraceabilityStepRecord> listSteps(String processInstanceId) {
        return repository.listStepsByInstance(processInstanceId);
    }

    public List<TraceabilityResponsibilityRecord> listResponsibilities() {
        return repository.listResponsibilities(currentScope());
    }

    private String currentMerchantId() {
        return authorizationService.currentActor().merchantId();
    }

    /** 平台管理员可见全量（null）；其余按本商户过滤。 */
    private String currentScope() {
        AccessActor actor = authorizationService.currentActor();
        return authorizationService.isPlatformAdmin(actor) ? null : actor.merchantId();
    }

    private String blankToId(String value, String prefix) {
        return value == null || value.isBlank() ? prefix + UUID.randomUUID() : value;
    }
}
