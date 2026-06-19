package com.complex.platform.amusement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AmusementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> PROJECT_STATUSES = Set.of("open", "paused", "closed", "maintenance");
    private static final Set<String> SESSION_STATUSES = Set.of("scheduled", "open", "closed", "cancelled");
    private static final Set<String> OVERRIDE_TYPES = Set.of("allow", "deny", "pause", "resume");
    private static final Set<String> RULE_TYPES = Set.of("age", "height", "weight", "capacity", "safety_notice", "manual");

    // 游乐权益类前缀（§6.4/§12.1：游乐只消费票务生成的游乐权益）。
    // entitlement_type 以 "amusement" 开头视为游乐类（amusement_single / amusement_pass / 游乐通票等）。
    private static final String AMUSEMENT_ENTITLEMENT_TYPE_PREFIX = "amusement";

    private final AmusementRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AmusementBookingService bookingService;
    private final AmusementInspectionService inspectionService;
    private final AmusementMaintenanceService maintenanceService;
    private final AuthorizationService authorizationService;
    private final AmusementEntitlementGateway entitlementGateway;

    public AmusementService(
            AmusementRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            AmusementEntitlementGateway entitlementGateway
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.entitlementGateway = entitlementGateway;
        this.bookingService = new AmusementBookingService(repository, idempotencyService, auditEventService, authorizationService);
        this.inspectionService = new AmusementInspectionService(repository, idempotencyService, auditEventService, authorizationService);
        this.maintenanceService = new AmusementMaintenanceService(repository, idempotencyService, auditEventService, authorizationService);
    }

    public PageResponse<AmusementDeviceStatusRecord> listDeviceStatuses() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.device_status.viewed",
                "amusement.device_status.viewed_failed",
                "amusement.device_status.view",
                "amusement_device_status",
                scope.isPresent() ? () -> repository.listDeviceStatusesByMerchant(scope.get()) : repository::listDeviceStatuses
        );
    }

    public PageResponse<AmusementManualOverrideRecord> listManualOverrides() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.manual_override.viewed",
                "amusement.manual_override.view_failed",
                "amusement.verification.override",
                "amusement_manual_override",
                scope.isPresent() ? () -> repository.listManualOverridesByMerchant(scope.get()) : repository::listManualOverrides
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.verification.override", admissionAction = "amusement.manual_override", objectType = "amusement_manual_override")
    @Transactional
    public AmusementManualOverrideRecord createManualOverride(AmusementManualOverrideRequest request) {
        return executeWrite(
                "createAmusementManualOverride",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementManualOverrideRecord.class,
                "amusement.verification_overridden",
                "amusement.verification_override_failed",
                "amusement.verification.override",
                "amusement_manual_override",
                () -> createManualOverrideInternal(request)
        );
    }

    public PageResponse<AmusementProjectRecord> listProjects() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.session.viewed",
                "amusement.session.view_failed",
                "amusement.session.view",
                "amusement_project",
                scope.isPresent() ? () -> repository.listProjectsByMerchant(scope.get()) : repository::listProjects
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.session.manage")
    @Transactional
    public AmusementProjectRecord createProject(AmusementProjectRequest request) {
        return executeWrite(
                "createAmusementProject",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementProjectRecord.class,
                "amusement.session.changed",
                "amusement.session.change_failed",
                "amusement.session.manage",
                "amusement_project",
                () -> createProjectInternal(request)
        );
    }

    public AmusementOpenEligibilityResponse getOpenEligibility(String projectId) {
        String id = requireText(projectId, "project_id");
        checkProjectOwnership(id);
        try {
            AmusementOpenEligibilityResponse response = evaluateOpenEligibility(id, null);
            if (response.allowed()) {
                auditEventService.recordSuccess(
                        "amusement.open_eligibility.viewed",
                        "amusement.safety_inspection.view",
                        "amusement_project",
                        id,
                        null,
                        null,
                        Map.of("blocked_reasons", response.blockedReasons())
                );
            } else {
                auditEventService.recordFailure(
                        "amusement.open_eligibility.blocked",
                        "amusement.safety_inspection.view",
                        "amusement_project",
                        id,
                        null,
                        null,
                        String.join(",", response.blockedReasons())
                );
            }
            return response;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "amusement.open_eligibility.blocked",
                    "amusement.safety_inspection.view",
                    "amusement_project",
                    id,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.project.manage")
    @Transactional
    public AmusementProjectRuleRecord createProjectRule(String projectId, AmusementProjectRuleRequest request) {
        String id = requireText(projectId, "project_id");
        return executeWrite(
                "createAmusementProjectRule:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementProjectRuleRecord.class,
                "amusement.project_rule.changed",
                "amusement.project_rule.changed_failed",
                "amusement.project.manage",
                "amusement_project_rule",
                () -> createProjectRuleInternal(id, request)
        );
    }

    public PageResponse<AmusementQueueEntryRecord> listQueueEntries() {
        return bookingService.listQueueEntries();
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.queue.manage")
    @Transactional
    public AmusementQueueEntryRecord createQueueEntry(AmusementQueueEntryRequest request) {
        return bookingService.createQueueEntry(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.queue.manage")
    @Transactional
    public AmusementQueueEntryRecord changeQueueEntryStatus(String queueEntryId, StatusChangeRequest request) {
        return bookingService.changeQueueEntryStatus(queueEntryId, request);
    }

    public PageResponse<AmusementReservationRecord> listReservations() {
        return bookingService.listReservations();
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.reservation.manage")
    @Transactional
    public AmusementReservationRecord createReservation(AmusementReservationRequest request) {
        return bookingService.createReservation(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.reservation.manage")
    @Transactional
    public AmusementReservationRecord changeReservationStatus(String reservationId, StatusChangeRequest request) {
        return bookingService.changeReservationStatus(reservationId, request);
    }

    public PageResponse<AmusementGroupBookingRecord> listGroupBookings() {
        return bookingService.listGroupBookings();
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.group_booking.manage")
    @Transactional
    public AmusementGroupBookingRecord createGroupBooking(AmusementGroupBookingRequest request) {
        return bookingService.createGroupBooking(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.group_booking.approve", admissionAction = "amusement.group_booking_approve", objectType = "amusement_group_booking")
    @Transactional
    public AmusementGroupBookingRecord approveGroupBooking(String bookingId, AmusementGroupBookingApprovalRequest request) {
        return bookingService.approveGroupBooking(bookingId, request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.group_booking.manage")
    @Transactional
    public AmusementGroupBookingRecord releaseGroupBooking(String bookingId, AmusementGroupBookingReleaseRequest request) {
        return bookingService.releaseGroupBooking(bookingId, request);
    }

    public PageResponse<AmusementSafetyInspectionRecord> listSafetyInspections() {
        return inspectionService.listSafetyInspections();
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.create")
    @Transactional
    public AmusementSafetyInspectionRecord createSafetyInspection(AmusementSafetyInspectionRequest request) {
        return inspectionService.createSafetyInspection(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.submit")
    @Transactional
    public AmusementSafetyInspectionRecord submitSafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        return inspectionService.submitSafetyInspection(inspectionId, request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.rectify")
    @Transactional
    public AmusementSafetyInspectionRecord rectifySafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        return inspectionService.rectifySafetyInspection(inspectionId, request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.recheck")
    @Transactional
    public AmusementSafetyInspectionRecord recheckSafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        return inspectionService.recheckSafetyInspection(inspectionId, request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.approve_reopen", admissionAction = "amusement.reopen", objectType = "inspection")
    @Transactional
    public AmusementSafetyInspectionRecord approveReopen(String inspectionId, StatusChangeRequest request) {
        return inspectionService.approveReopen(inspectionId, request);
    }

    public PageResponse<AmusementMaintenanceRecord> listMaintenanceRecords() {
        return maintenanceService.listMaintenanceRecords();
    }

    public AmusementMaintenanceRecord getMaintenanceRecord(String recordId) {
        return maintenanceService.getMaintenanceRecord(recordId);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.maintenance.submit")
    @Transactional
    public AmusementMaintenanceRecord createMaintenanceRecord(AmusementMaintenanceRecordRequest request) {
        return maintenanceService.createMaintenanceRecord(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.maintenance.confirm", admissionAction = "amusement.reopen", objectType = "amusement_maintenance_record")
    @Transactional
    public AmusementMaintenanceRecord confirmMaintenanceResume(String recordId, AmusementMaintenanceResumeRequest request) {
        return maintenanceService.confirmMaintenanceResume(recordId, request);
    }

    public PageResponse<AmusementSafetyTemplateRecord> listSafetyTemplates() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.safety_template.viewed",
                "amusement.safety_template.view_failed",
                "amusement.safety_template.view",
                "amusement_safety_template",
                scope.isPresent() ? () -> repository.listSafetyTemplatesByMerchant(scope.get()) : repository::listSafetyTemplates
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_template.manage")
    @Transactional
    public AmusementSafetyTemplateRecord createSafetyTemplate(AmusementSafetyTemplateRequest request) {
        return executeWrite(
                "createAmusementSafetyTemplate",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyTemplateRecord.class,
                "amusement.safety_template.changed",
                "amusement.safety_template.change_failed",
                "amusement.safety_template.manage",
                "amusement_safety_template",
                () -> createSafetyTemplateInternal(request)
        );
    }

    public PageResponse<AmusementSessionRecord> listSessions() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.session.viewed",
                "amusement.session.view_failed",
                "amusement.session.view",
                "amusement_session",
                scope.isPresent() ? () -> repository.listSessionsByMerchant(scope.get()) : repository::listSessions
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.session.manage")
    @Transactional
    public AmusementSessionRecord createSession(AmusementSessionRequest request) {
        return executeWrite(
                "createAmusementSession",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSessionRecord.class,
                "amusement.session.changed",
                "amusement.session.change_failed",
                "amusement.session.manage",
                "amusement_session",
                () -> createSessionInternal(request)
        );
    }

    public PageResponse<AmusementVerificationRecord> listVerifications() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.verification.viewed",
                "amusement.verification.view_failed",
                "amusement.verification.view",
                "amusement_verification",
                scope.isPresent() ? () -> repository.listVerificationsByMerchant(scope.get()) : repository::listVerifications
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.verification.execute")
    @Transactional
    public AmusementVerificationRecord createVerification(VerificationRequest request) {
        return executeWrite(
                "createAmusementVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementVerificationRecord.class,
                "amusement.verification_succeeded",
                "amusement.verification_failed",
                "amusement.verification.execute",
                "amusement_verification",
                () -> createVerificationInternal(request)
        );
    }

    private AmusementSafetyTemplateRecord createSafetyTemplateInternal(AmusementSafetyTemplateRequest request) {
        AmusementSafetyTemplateRequest actual = requireRequest(request);
        if (actual.items() == null || actual.items().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "items is required");
        }
        Instant now = Instant.now();
        return repository.saveSafetyTemplate(new AmusementSafetyTemplateRecord(
                "template_" + slug(actual.idempotencyKey()),
                requireText(actual.templateName(), "template_name"),
                List.copyOf(actual.items()),
                actual.effectiveFrom() == null ? LocalDate.now() : actual.effectiveFrom(),
                "active",
                evidence(actual.evidenceAttachmentIds()),
                now,
                now,
                authorizationService.currentMerchantScope().orElse(null)
        ));
    }

    private AmusementProjectRecord createProjectInternal(AmusementProjectRequest request) {
        AmusementProjectRequest actual = requireRequest(request);
        String templateId = trimToNull(actual.safetyTemplateId());
        if (templateId != null) {
            repository.findSafetyTemplateById(templateId)
                    .orElseThrow(() -> ApiException.notFound("SAFETY_TEMPLATE_NOT_FOUND", "safety template not found"));
        }
        Instant now = Instant.now();
        return repository.saveProject(new AmusementProjectRecord(
                "project_" + slug(actual.idempotencyKey()),
                requireText(actual.projectName(), "project_name"),
                nonNegativeOrDefault(actual.capacity(), 0, "capacity"),
                enumOrDefault(actual.openStatus(), "open", PROJECT_STATUSES, "open_status"),
                templateId,
                actual.eligibilityRule() == null ? Map.of() : Map.copyOf(actual.eligibilityRule()),
                now,
                now,
                authorizationService.currentMerchantScope().orElse(null)
        ));
    }

    private AmusementSessionRecord createSessionInternal(AmusementSessionRequest request) {
        AmusementSessionRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        Instant start = actual.startTime() == null ? Instant.now() : actual.startTime();
        Instant end = actual.endTime() == null ? start.plusSeconds(3600) : actual.endTime();
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "end_time must be after start_time");
        }
        Instant now = Instant.now();
        return repository.saveSession(new AmusementSessionRecord(
                "session_" + slug(actual.idempotencyKey()),
                projectId,
                start,
                end,
                nonNegativeOrDefault(actual.capacity(), 0, "capacity"),
                0,
                0,
                enumOrDefault(actual.status(), "scheduled", SESSION_STATUSES, "status"),
                evidence(actual.evidenceAttachmentIds()),
                now,
                now
        ));
    }

    private AmusementProjectRuleRecord createProjectRuleInternal(String projectId, AmusementProjectRuleRequest request) {
        AmusementProjectRuleRequest actual = requireRequest(request);
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        String ruleType = enumOrDefault(actual.ruleType(), null, RULE_TYPES, "rule_type");
        Instant now = Instant.now();
        return repository.saveProjectRule(new AmusementProjectRuleRecord(
                "rule_" + slug(actual.idempotencyKey()),
                projectId,
                ruleType,
                requireText(actual.ruleValue(), "rule_value"),
                trimToNull(actual.effectiveFrom()),
                trimToNull(actual.effectiveTo()),
                actual.blocking() == null || actual.blocking(),
                "active",
                evidence(actual.evidenceAttachmentIds()),
                now,
                now
        ));
    }

    private AmusementVerificationRecord createVerificationInternal(VerificationRequest request) {
        VerificationRequest actual = requireRequest(request);
        String credential = requireText(actual.credentialPayload(), "credential_payload");
        String projectId = requireText(textFromMap(actual.businessContext(), "project_id", "projectId", "business_id", "businessId"), "project_id");
        String sessionId = requireText(textFromMap(actual.businessContext(), "session_id", "sessionId"), "session_id");
        String staffId = trimToDefault(textFromMap(actual.businessContext(), "staff_id", "staffId"), "staff");
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        if (!"open".equals(project.openStatus())) {
            throw ApiException.conflict("PROJECT_NOT_OPEN", "project is not open");
        }
        AmusementSessionRecord session = repository.findSessionById(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "session not found"));
        if (!project.id().equals(session.projectId())) {
            throw ApiException.conflict("SESSION_PROJECT_MISMATCH", "session does not belong to project");
        }
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("SESSION_NOT_OPEN", "session is not open");
        }
        AmusementOpenEligibilityResponse eligibility = evaluateOpenEligibility(project.id(), null);
        if (!eligibility.allowed()) {
            throw ApiException.conflict("SAFETY_BLOCKED", "safety gate blocked amusement verification");
        }

        // §6.4 步骤 2/3：识别票/权益实例 → 在线校验（存在、active、有效期、剩余次数、游乐类、绑定本项目）。
        AmusementEntitlementView entitlement = entitlementGateway.findEntitlementByCredential(credential)
                .orElseThrow(() -> ApiException.notFound("TICKET_NOT_FOUND", "ticket entitlement not found"));
        if (!"active".equals(entitlement.status())) {
            throw ApiException.conflict("ENTITLEMENT_USED", "entitlement is not active");
        }
        Instant now = Instant.now();
        if ((entitlement.validFrom() != null && now.isBefore(entitlement.validFrom()))
                || (entitlement.validTo() != null && now.isAfter(entitlement.validTo()))) {
            throw ApiException.conflict("ENTITLEMENT_EXPIRED", "entitlement is not within validity window");
        }
        if (entitlement.remainingTimes() <= 0) {
            throw ApiException.conflict("ENTITLEMENT_USED", "entitlement has no remaining times");
        }
        if (!isAmusementEntitlementType(entitlement.entitlementType())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_AMUSEMENT", "entitlement is not an amusement entitlement");
        }
        // 绑定本项目：该权益 type 在 amusement_project_entitlements 内。
        // 回退口径（V082 向后兼容）：项目无任何绑定配置时，仅按「游乐类」放行（上方已校验）。
        List<String> boundTypes = repository.listProjectEntitlementTypes(project.id());
        if (!boundTypes.isEmpty() && !boundTypes.contains(entitlement.entitlementType())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_FOR_PROJECT", "entitlement is not bound to this project");
        }

        // §6.4 步骤 4/6：先消费票务权益（原子扣 used_times），再按三维度扣容量。
        // 顺序与回滚：权益扣减失败不扣容量；容量任一维度扣减失败则回补权益（补偿），保证两者一致。
        if (!entitlementGateway.consumeEntitlement(entitlement.id())) {
            throw ApiException.conflict("ENTITLEMENT_USED", "entitlement has no remaining times");
        }
        try {
            // §6.4 三维度取最小可放行余量：对已配置的受控维度（场次 / 点位·设备 / 项目安全上限）
            // 原子 check+increment，任一维度余量为 0 即拒绝（session_full / SESSION_CAPACITY_EXHAUSTED）；
            // 未配置维度跳过（无 capacity_config 退化为场次单维，向后兼容）。
            String pointId = trimToNull(textFromMap(actual.businessContext(),
                    "location_point_id", "locationPointId", "point_id", "pointId"));
            CapacityClaimResult claim = repository.claimCapacityAcrossDimensions(new CapacityClaimContext(
                    project.id(),
                    session.id(),
                    pointId,
                    trimToNull(actual.deviceId()),
                    project.capacity()
            ));
            if (!claim.claimed()) {
                throw ApiException.conflict("SESSION_CAPACITY_EXHAUSTED",
                        "session capacity exhausted (dimension=" + claim.exhaustedDimension() + ")");
            }
        } catch (RuntimeException exception) {
            // 容量扣减失败：回补已消费的权益，避免权益被白扣。
            entitlementGateway.reverseEntitlement(entitlement.id());
            throw exception;
        }
        return repository.saveVerification(new AmusementVerificationRecord(
                "verification_" + slug(actual.idempotencyKey()),
                project.id(),
                session.id(),
                credential,
                trimToNull(actual.deviceId()),
                staffId,
                "success",
                null,
                List.of(),
                now
        ));
    }

    private boolean isAmusementEntitlementType(String entitlementType) {
        return entitlementType != null
                && entitlementType.trim().toLowerCase(Locale.ROOT).startsWith(AMUSEMENT_ENTITLEMENT_TYPE_PREFIX);
    }

    private AmusementManualOverrideRecord createManualOverrideInternal(AmusementManualOverrideRequest request) {
        AmusementManualOverrideRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        String sessionId = trimToNull(actual.sessionId());
        if (sessionId != null) {
            repository.findSessionById(sessionId)
                    .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "session not found"));
        }
        String type = enumOrDefault(actual.overrideType(), null, OVERRIDE_TYPES, "override_type");
        String reason = requireText(actual.reason(), "reason");
        String status = switch (type) {
            case "allow" -> "approved";
            case "deny" -> "denied";
            default -> "recorded";
        };
        return repository.saveManualOverride(new AmusementManualOverrideRecord(
                "override_" + slug(actual.idempotencyKey()),
                projectId,
                sessionId,
                type,
                reason,
                status,
                trimToNull(actual.operatorNote()),
                evidence(actual.evidenceAttachmentIds()),
                Instant.now()
        ));
    }

    private void checkProjectOwnership(String projectId) {
        authorizationService.currentMerchantScope().ifPresent(merchantId -> {
            AmusementProjectRecord project = repository.findProjectById(projectId)
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
            if (!merchantId.equals(project.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "project does not belong to your merchant");
            }
        });
    }

    private AmusementOpenEligibilityResponse evaluateOpenEligibility(String projectId, String sessionId) {
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        List<String> reasons = new ArrayList<>();
        if (!"open".equals(project.openStatus())) {
            reasons.add("PROJECT_NOT_OPEN");
        }
        repository.latestSafetyInspectionForProject(projectId)
                .ifPresentOrElse(inspection -> {
                    if ("pass".equals(inspection.overallResult())) {
                        if (!"submitted".equals(inspection.inspectionStatus())) {
                            reasons.add("SAFETY_INSPECTION_NOT_SUBMITTED");
                        }
                    } else if (!"approved".equals(inspection.reopenStatus())) {
                        reasons.add("SAFETY_REOPEN_PENDING");
                    }
                }, () -> reasons.add("MISSING_SAFETY_INSPECTION"));
        if (!repository.listActiveBlockingProjectRules(projectId).isEmpty()) {
            reasons.add("PROJECT_RULE_BLOCKED");
        }
        if (sessionId != null) {
            AmusementSessionRecord session = repository.findSessionById(sessionId)
                    .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "session not found"));
            if (!"open".equals(session.status())) {
                reasons.add("SESSION_NOT_OPEN");
            }
            if (session.usedCapacity() + session.lockedCapacity() >= session.capacity()) {
                reasons.add("SESSION_CAPACITY_EXHAUSTED");
            }
        }
        return new AmusementOpenEligibilityResponse(projectId, sessionId, reasons.isEmpty(), List.copyOf(reasons));
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        try {
            List<T> items = action.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T saved = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(saved),
                                null,
                                idempotencyKey,
                                Map.of()
                        );
                        return saved;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private String businessId(Object response) {
        if (response instanceof AmusementProjectRecord record) {
            return record.id();
        }
        if (response instanceof AmusementSessionRecord record) {
            return record.id();
        }
        if (response instanceof AmusementSafetyTemplateRecord record) {
            return record.id();
        }
        if (response instanceof AmusementSafetyInspectionRecord record) {
            return record.id();
        }
        if (response instanceof AmusementProjectRuleRecord record) {
            return record.id();
        }
        if (response instanceof AmusementVerificationRecord record) {
            return record.id();
        }
        if (response instanceof AmusementManualOverrideRecord record) {
            return record.id();
        }
        if (response instanceof AmusementReservationRecord record) {
            return record.id();
        }
        if (response instanceof AmusementQueueEntryRecord record) {
            return record.id();
        }
        if (response instanceof AmusementGroupBookingRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private int nonNegativeOrDefault(Integer value, int defaultValue, String fieldName) {
        int actual = value == null ? defaultValue : value;
        if (actual < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return actual;
    }

    private int positiveInteger(Integer value, String fieldName) {
        int actual = value == null ? 0 : value;
        if (actual <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return actual;
    }

    private String enumOrDefault(String value, String defaultValue, Set<String> allowed, String fieldName) {
        String actual = trimToDefault(value, defaultValue);
        if (actual == null || !allowed.contains(actual)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return actual;
    }

    private List<String> evidence(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<String> evidenceOrExisting(List<String> values, List<String> existing) {
        List<String> evidence = evidence(values);
        return evidence.isEmpty() ? List.copyOf(existing == null ? List.of() : existing) : evidence;
    }

    private String textFromMap(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private String slug(String value) {
        String source = requireText(value, "idempotency_key");
        UUID uuid = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(asBytes(uuid)).substring(0, 24);
    }

    private byte[] asBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        for (int index = 0; index < 8; index++) {
            bytes[index] = (byte) (most >>> (8 * (7 - index)));
            bytes[index + 8] = (byte) (least >>> (8 * (7 - index)));
        }
        return bytes;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
