package com.complex.platform.amusement;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
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
    private static final Set<String> OVERALL_RESULTS = Set.of("pass", "fail", "pending_rectification");
    private static final Set<String> OVERRIDE_TYPES = Set.of("allow", "deny", "pause", "resume");
    private static final Set<String> RULE_TYPES = Set.of("age", "height", "weight", "capacity", "safety_notice", "manual");
    private static final Set<String> RESERVATION_STATUSES = Set.of("reserved", "cancelled", "completed", "no_show");
    private static final Set<String> QUEUE_STATUSES = Set.of("waiting", "called", "checked_in", "cancelled", "no_show");
    private static final Set<String> GROUP_BOOKING_TYPES = Set.of("group", "private");
    private static final Set<String> ACCEPTING_ENTRY_SESSION_STATUSES = Set.of("scheduled", "open");

    private final AmusementRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public AmusementService(
            AmusementRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    public PageResponse<AmusementDeviceStatusRecord> listDeviceStatuses() {
        return readPage(
                "amusement.device_status.viewed",
                "amusement.device_status.viewed_failed",
                "amusement.device_status.view",
                "amusement_device_status",
                repository::listDeviceStatuses
        );
    }

    public PageResponse<AmusementManualOverrideRecord> listManualOverrides() {
        return readPage(
                "amusement.manual_override.viewed",
                "amusement.manual_override.view_failed",
                "amusement.verification.override",
                "amusement_manual_override",
                repository::listManualOverrides
        );
    }

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
        return readPage(
                "amusement.session.viewed",
                "amusement.session.view_failed",
                "amusement.session.view",
                "amusement_project",
                repository::listProjects
        );
    }

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
        return readPage(
                "amusement.queue.viewed",
                "amusement.queue.viewed_failed",
                "amusement.queue.view",
                "amusement_queue_entry",
                repository::listQueueEntries
        );
    }

    @Transactional
    public AmusementQueueEntryRecord createQueueEntry(AmusementQueueEntryRequest request) {
        return executeWrite(
                "createAmusementQueueEntry",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementQueueEntryRecord.class,
                "amusement.queue.created",
                "amusement.queue.create_failed",
                "amusement.queue.manage",
                "amusement_queue_entry",
                () -> createQueueEntryInternal(request)
        );
    }

    @Transactional
    public AmusementQueueEntryRecord changeQueueEntryStatus(String queueEntryId, StatusChangeRequest request) {
        String id = requireText(queueEntryId, "queue_entry_id");
        return executeWrite(
                "changeAmusementQueueEntryStatus:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementQueueEntryRecord.class,
                "amusement.queue.status_changed",
                "amusement.queue.status_change_failed",
                "amusement.queue.manage",
                "amusement_queue_entry",
                () -> changeQueueEntryStatusInternal(id, request)
        );
    }

    public PageResponse<AmusementReservationRecord> listReservations() {
        return readPage(
                "amusement.reservation.viewed",
                "amusement.reservation.viewed_failed",
                "amusement.reservation.view",
                "amusement_reservation",
                repository::listReservations
        );
    }

    @Transactional
    public AmusementReservationRecord createReservation(AmusementReservationRequest request) {
        return executeWrite(
                "createAmusementReservation",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementReservationRecord.class,
                "amusement.reservation.created",
                "amusement.reservation.create_failed",
                "amusement.reservation.manage",
                "amusement_reservation",
                () -> createReservationInternal(request)
        );
    }

    @Transactional
    public AmusementReservationRecord changeReservationStatus(String reservationId, StatusChangeRequest request) {
        String id = requireText(reservationId, "reservation_id");
        return executeWrite(
                "changeAmusementReservationStatus:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementReservationRecord.class,
                "amusement.reservation.status_changed",
                "amusement.reservation.status_change_failed",
                "amusement.reservation.manage",
                "amusement_reservation",
                () -> changeReservationStatusInternal(id, request)
        );
    }

    public PageResponse<AmusementGroupBookingRecord> listGroupBookings() {
        return readPage(
                "amusement.group_booking.viewed",
                "amusement.group_booking.view_failed",
                "amusement.group_booking.view",
                "amusement_group_booking",
                repository::listGroupBookings
        );
    }

    @Transactional
    public AmusementGroupBookingRecord createGroupBooking(AmusementGroupBookingRequest request) {
        return executeWrite(
                "createAmusementGroupBooking",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementGroupBookingRecord.class,
                "amusement.group_booking.created",
                "amusement.group_booking.create_failed",
                "amusement.group_booking.manage",
                "amusement_group_booking",
                () -> createGroupBookingInternal(request)
        );
    }

    @Transactional
    public AmusementGroupBookingRecord approveGroupBooking(String bookingId, AmusementGroupBookingApprovalRequest request) {
        String id = requireText(bookingId, "group_booking_id");
        return executeWrite(
                "approveAmusementGroupBooking:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementGroupBookingRecord.class,
                "amusement.group_booking.approved",
                "amusement.group_booking.approve_failed",
                "amusement.group_booking.approve",
                "amusement_group_booking",
                () -> approveGroupBookingInternal(id, request)
        );
    }

    @Transactional
    public AmusementGroupBookingRecord releaseGroupBooking(String bookingId, AmusementGroupBookingReleaseRequest request) {
        String id = requireText(bookingId, "group_booking_id");
        return executeWrite(
                "releaseAmusementGroupBooking:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementGroupBookingRecord.class,
                "amusement.group_booking.released",
                "amusement.group_booking.release_failed",
                "amusement.group_booking.manage",
                "amusement_group_booking",
                () -> releaseGroupBookingInternal(id, request)
        );
    }

    public PageResponse<AmusementSafetyInspectionRecord> listSafetyInspections() {
        return readPage(
                "amusement.safety_inspection.viewed",
                "amusement.safety_inspection.view_failed",
                "amusement.safety_inspection.view",
                "amusement_safety_inspection",
                repository::listSafetyInspections
        );
    }

    @Transactional
    public AmusementSafetyInspectionRecord createSafetyInspection(AmusementSafetyInspectionRequest request) {
        return executeWrite(
                "createAmusementSafetyInspection",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyInspectionRecord.class,
                "amusement.safety_inspection.created",
                "amusement.safety_inspection.create_failed",
                "amusement.safety_inspection.create",
                "amusement_safety_inspection",
                () -> createSafetyInspectionInternal(request)
        );
    }

    @Transactional
    public AmusementSafetyInspectionRecord submitSafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        String id = requireText(inspectionId, "inspection_id");
        String idempotencyKey = request == null ? null : request.idempotencyKey();
        try {
            return idempotencyService.execute(
                    "submitAmusementSafetyInspection:" + id,
                    idempotencyKey,
                    request,
                    () -> {
                        AmusementSafetyInspectionRecord submitted = submitSafetyInspectionInternal(id, request);
                        if ("pass".equals(submitted.overallResult())) {
                            auditEventService.recordSuccess(
                                    "amusement.safety_inspection.submitted",
                                    "amusement.safety_inspection.submit",
                                    "amusement_safety_inspection",
                                    submitted.id(),
                                    null,
                                    idempotencyKey,
                                    Map.of()
                            );
                        } else {
                            auditEventService.recordFailure(
                                    "amusement.safety_inspection.blocked",
                                    "amusement.safety_inspection.submit",
                                    "amusement_safety_inspection",
                                    submitted.id(),
                                    null,
                                    idempotencyKey,
                                    "SAFETY_REOPEN_PENDING"
                            );
                        }
                        return submitted;
                    },
                    AmusementSafetyInspectionRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "amusement.safety_inspection.blocked",
                    "amusement.safety_inspection.submit",
                    "amusement_safety_inspection",
                    id,
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @Transactional
    public AmusementSafetyInspectionRecord rectifySafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        String id = requireText(inspectionId, "inspection_id");
        return executeWrite(
                "rectifyAmusementSafetyInspection:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyInspectionRecord.class,
                "amusement.safety_inspection.rectification_submitted",
                "amusement.safety_inspection.rectification_failed",
                "amusement.safety_inspection.rectify",
                "amusement_safety_inspection",
                () -> rectifySafetyInspectionInternal(id, request)
        );
    }

    @Transactional
    public AmusementSafetyInspectionRecord recheckSafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        String id = requireText(inspectionId, "inspection_id");
        return executeWrite(
                "recheckAmusementSafetyInspection:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyInspectionRecord.class,
                "amusement.safety_inspection.rechecked",
                "amusement.safety_inspection.recheck_failed",
                "amusement.safety_inspection.recheck",
                "amusement_safety_inspection",
                () -> recheckSafetyInspectionInternal(id, request)
        );
    }

    @Transactional
    public AmusementSafetyInspectionRecord approveReopen(String inspectionId, StatusChangeRequest request) {
        String id = requireText(inspectionId, "inspection_id");
        return executeWrite(
                "approveAmusementReopen:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyInspectionRecord.class,
                "amusement.safety_inspection.reopened",
                "amusement.safety_inspection.reopen_rejected",
                "amusement.safety_inspection.approve_reopen",
                "amusement_safety_inspection",
                () -> approveReopenInternal(id, request)
        );
    }

    public PageResponse<AmusementSafetyTemplateRecord> listSafetyTemplates() {
        return readPage(
                "amusement.safety_template.viewed",
                "amusement.safety_template.view_failed",
                "amusement.safety_template.view",
                "amusement_safety_template",
                repository::listSafetyTemplates
        );
    }

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
        return readPage(
                "amusement.session.viewed",
                "amusement.session.view_failed",
                "amusement.session.view",
                "amusement_session",
                repository::listSessions
        );
    }

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
        return readPage(
                "amusement.verification.viewed",
                "amusement.verification.view_failed",
                "amusement.verification.view",
                "amusement_verification",
                repository::listVerifications
        );
    }

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
                now
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
                now
        ));
    }

    private AmusementSessionRecord createSessionInternal(AmusementSessionRequest request) {
        AmusementSessionRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
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

    private AmusementReservationRecord createReservationInternal(AmusementReservationRequest request) {
        AmusementReservationRequest actual = requireRequest(request);
        AmusementSessionRecord session = acceptingSession(
                requireText(actual.projectId(), "project_id"),
                requireText(actual.sessionId(), "session_id"),
                "SESSION_NOT_ACCEPTING_RESERVATION"
        );
        Instant now = Instant.now();
        return repository.saveReservation(new AmusementReservationRecord(
                "reservation_" + slug(actual.idempotencyKey()),
                session.projectId(),
                session.id(),
                requireText(actual.customerId(), "customer_id"),
                enumOrDefault(actual.reservationStatus(), "reserved", RESERVATION_STATUSES, "reservation_status"),
                now,
                now
        ));
    }

    private AmusementReservationRecord changeReservationStatusInternal(String reservationId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        String status = enumOrDefault(actual.action(), null, RESERVATION_STATUSES, "action");
        requireText(actual.reason(), "reason");
        AmusementReservationRecord existing = repository.findReservationById(reservationId)
                .orElseThrow(() -> ApiException.notFound("RESERVATION_NOT_FOUND", "reservation not found"));
        return repository.saveReservation(new AmusementReservationRecord(
                existing.id(),
                existing.projectId(),
                existing.sessionId(),
                existing.customerId(),
                status,
                existing.createdAt(),
                Instant.now()
        ));
    }

    private AmusementQueueEntryRecord createQueueEntryInternal(AmusementQueueEntryRequest request) {
        AmusementQueueEntryRequest actual = requireRequest(request);
        AmusementSessionRecord session = acceptingSession(
                requireText(actual.projectId(), "project_id"),
                requireText(actual.sessionId(), "session_id"),
                "SESSION_NOT_ACCEPTING_ENTRY"
        );
        Instant now = Instant.now();
        return repository.saveQueueEntry(new AmusementQueueEntryRecord(
                "queue_" + slug(actual.idempotencyKey()),
                session.projectId(),
                session.id(),
                requireText(actual.customerId(), "customer_id"),
                trimToDefault(actual.queueNo(), "Q" + slug(actual.idempotencyKey()).substring(0, 6).toUpperCase(Locale.ROOT)),
                enumOrDefault(actual.status(), "waiting", QUEUE_STATUSES, "status"),
                now,
                now
        ));
    }

    private AmusementQueueEntryRecord changeQueueEntryStatusInternal(String queueEntryId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        String status = enumOrDefault(actual.action(), null, QUEUE_STATUSES, "action");
        requireText(actual.reason(), "reason");
        AmusementQueueEntryRecord existing = repository.findQueueEntryById(queueEntryId)
                .orElseThrow(() -> ApiException.notFound("QUEUE_ENTRY_NOT_FOUND", "queue entry not found"));
        return repository.saveQueueEntry(new AmusementQueueEntryRecord(
                existing.id(),
                existing.projectId(),
                existing.sessionId(),
                existing.customerId(),
                existing.queueNo(),
                status,
                existing.createdAt(),
                Instant.now()
        ));
    }

    private AmusementGroupBookingRecord createGroupBookingInternal(AmusementGroupBookingRequest request) {
        AmusementGroupBookingRequest actual = requireRequest(request);
        AmusementSessionRecord session = acceptingSession(
                requireText(actual.projectId(), "project_id"),
                requireText(actual.sessionId(), "session_id"),
                "SESSION_NOT_ACCEPTING_GROUP_BOOKING"
        );
        int partySize = positiveInteger(actual.partySize(), "party_size");
        int reservedCapacity = positiveInteger(actual.reservedCapacity(), "reserved_capacity");
        if (reservedCapacity > session.capacity()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "reserved_capacity cannot exceed session capacity");
        }
        Instant now = Instant.now();
        return repository.saveGroupBooking(new AmusementGroupBookingRecord(
                "group_booking_" + slug(actual.idempotencyKey()),
                session.projectId(),
                session.id(),
                enumOrDefault(actual.bookingType(), "group", GROUP_BOOKING_TYPES, "booking_type"),
                requireText(actual.customerName(), "customer_name"),
                trimToNull(actual.contactPhone()),
                partySize,
                reservedCapacity,
                "pending_approval",
                null,
                trimToNull(actual.operatorNote()),
                evidence(actual.evidenceAttachmentIds()),
                null,
                null,
                now,
                now
        ));
    }

    private AmusementGroupBookingRecord approveGroupBookingInternal(String bookingId, AmusementGroupBookingApprovalRequest request) {
        AmusementGroupBookingApprovalRequest actual = requireRequest(request);
        String approvalNote = requireText(actual.approvalNote(), "approval_note");
        AmusementGroupBookingRecord existing = repository.findGroupBookingById(bookingId)
                .orElseThrow(() -> ApiException.notFound("GROUP_BOOKING_NOT_FOUND", "group booking not found"));
        if (!"pending_approval".equals(existing.bookingStatus())) {
            throw ApiException.conflict("GROUP_BOOKING_NOT_PENDING", "group booking must be pending approval");
        }
        acceptingSession(existing.projectId(), existing.sessionId(), "SESSION_NOT_ACCEPTING_GROUP_BOOKING");
        Instant now = Instant.now();
        return repository.approveGroupBookingWithCapacityLock(
                        existing.id(),
                        approvalNote,
                        evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                        now
                )
                .orElseThrow(() -> ApiException.conflict("SESSION_CAPACITY_LOCK_FAILED", "session remaining capacity is insufficient"));
    }

    private AmusementGroupBookingRecord releaseGroupBookingInternal(String bookingId, AmusementGroupBookingReleaseRequest request) {
        AmusementGroupBookingReleaseRequest actual = requireRequest(request);
        String reason = requireText(actual.reason(), "reason");
        AmusementGroupBookingRecord existing = repository.findGroupBookingById(bookingId)
                .orElseThrow(() -> ApiException.notFound("GROUP_BOOKING_NOT_FOUND", "group booking not found"));
        if (!"approved".equals(existing.bookingStatus())) {
            throw ApiException.conflict("GROUP_BOOKING_NOT_APPROVED", "only approved group booking can be released");
        }
        Instant now = Instant.now();
        return repository.releaseGroupBookingAndCapacity(
                        existing.id(),
                        trimToDefault(existing.operatorNote(), reason),
                        evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                        now
                )
                .orElseThrow(() -> ApiException.conflict("GROUP_BOOKING_RELEASE_FAILED", "group booking capacity release failed"));
    }

    private AmusementSessionRecord acceptingSession(String projectId, String sessionId, String blockedCode) {
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        AmusementSessionRecord session = repository.findSessionById(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "session not found"));
        if (!project.id().equals(session.projectId())) {
            throw ApiException.conflict("SESSION_PROJECT_MISMATCH", "session does not belong to project");
        }
        if (!ACCEPTING_ENTRY_SESSION_STATUSES.contains(session.status())) {
            throw ApiException.conflict(blockedCode, "session is not accepting reservation or queue entry");
        }
        return session;
    }

    private AmusementSafetyInspectionRecord createSafetyInspectionInternal(AmusementSafetyInspectionRequest request) {
        AmusementSafetyInspectionRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        String templateId = requireText(actual.templateId(), "template_id");
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        repository.findSafetyTemplateById(templateId)
                .orElseThrow(() -> ApiException.notFound("SAFETY_TEMPLATE_NOT_FOUND", "safety template not found"));
        if (actual.items() == null || actual.items().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "items is required");
        }
        String overall = enumOrDefault(actual.overallResult(), "pending_rectification", OVERALL_RESULTS, "overall_result");
        Instant now = Instant.now();
        return repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                "inspection_" + slug(actual.idempotencyKey()),
                projectId,
                templateId,
                List.copyOf(actual.items()),
                overall,
                "created",
                "pass".equals(overall) ? "none" : "pending",
                "pass".equals(overall) ? "not_required" : "pending",
                actual.inspectedAt() == null ? now : actual.inspectedAt(),
                null,
                null,
                null,
                trimToNull(actual.operatorNote()),
                evidence(actual.evidenceAttachmentIds()),
                now,
                now
        ));
    }

    private AmusementSafetyInspectionRecord submitSafetyInspectionInternal(String inspectionId, AmusementSafetyActionRequest request) {
        AmusementSafetyActionRequest actual = requireRequest(request);
        AmusementSafetyInspectionRecord existing = findInspection(inspectionId);
        if ("approved".equals(existing.reopenStatus())) {
            throw ApiException.conflict("INSPECTION_ALREADY_APPROVED", "inspection already approved");
        }
        if ("submitted".equals(existing.inspectionStatus())) {
            throw ApiException.conflict("INSPECTION_ALREADY_SUBMITTED", "inspection already submitted");
        }
        Instant now = Instant.now();
        repository.saveSafetyAction(new AmusementSafetyActionRecord(
                "action_" + slug(actual.idempotencyKey()),
                inspectionId,
                "submit",
                requireText(actual.actionResult(), "action_result"),
                trimToNull(actual.comment()),
                evidence(actual.attachmentIds()),
                now
        ));
        AmusementSafetyInspectionRecord updated = new AmusementSafetyInspectionRecord(
                existing.id(),
                existing.projectId(),
                existing.templateId(),
                existing.items(),
                existing.overallResult(),
                "submitted",
                "pass".equals(existing.overallResult()) ? "none" : "pending",
                "pass".equals(existing.overallResult()) ? "not_required" : "pending",
                existing.inspectedAt(),
                now,
                existing.recheckedAt(),
                existing.approvedAt(),
                trimToDefault(actual.comment(), existing.operatorNote()),
                evidenceOrExisting(actual.attachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
                now
        );
        return repository.saveSafetyInspection(updated);
    }

    private AmusementSafetyInspectionRecord rectifySafetyInspectionInternal(String inspectionId, AmusementSafetyActionRequest request) {
        AmusementSafetyActionRequest actual = requireRequest(request);
        AmusementSafetyInspectionRecord existing = findInspection(inspectionId);
        if (!"submitted".equals(existing.inspectionStatus())) {
            throw ApiException.conflict("INSPECTION_NOT_SUBMITTED", "inspection must be submitted before rectification");
        }
        if ("pass".equals(existing.overallResult())) {
            throw ApiException.conflict("RECTIFICATION_NOT_REQUIRED", "passing inspection does not require rectification");
        }
        Instant now = Instant.now();
        repository.saveSafetyAction(new AmusementSafetyActionRecord(
                "action_" + slug(actual.idempotencyKey()),
                inspectionId,
                "rectify",
                requireText(actual.actionResult(), "action_result"),
                requireText(actual.comment(), "comment"),
                evidence(actual.attachmentIds()),
                now
        ));
        return repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                existing.id(),
                existing.projectId(),
                existing.templateId(),
                existing.items(),
                existing.overallResult(),
                existing.inspectionStatus(),
                "rectification_submitted",
                "pending",
                existing.inspectedAt(),
                existing.submittedAt(),
                existing.recheckedAt(),
                existing.approvedAt(),
                actual.comment().trim(),
                evidenceOrExisting(actual.attachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
                now
        ));
    }

    private AmusementSafetyInspectionRecord recheckSafetyInspectionInternal(String inspectionId, AmusementSafetyActionRequest request) {
        AmusementSafetyActionRequest actual = requireRequest(request);
        AmusementSafetyInspectionRecord existing = findInspection(inspectionId);
        if (!"rectification_submitted".equals(existing.rectificationStatus())) {
            throw ApiException.conflict("RECTIFICATION_REQUIRED", "rectification must be submitted before recheck");
        }
        String result = requireText(actual.actionResult(), "action_result").toLowerCase(Locale.ROOT);
        Instant now = Instant.now();
        repository.saveSafetyAction(new AmusementSafetyActionRecord(
                "action_" + slug(actual.idempotencyKey()),
                inspectionId,
                "recheck",
                result,
                trimToNull(actual.comment()),
                evidence(actual.attachmentIds()),
                now
        ));
        return repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                existing.id(),
                existing.projectId(),
                existing.templateId(),
                existing.items(),
                existing.overallResult(),
                existing.inspectionStatus(),
                "pass".equals(result) ? "rechecked" : "recheck_failed",
                "pending",
                existing.inspectedAt(),
                existing.submittedAt(),
                now,
                existing.approvedAt(),
                trimToDefault(actual.comment(), existing.operatorNote()),
                evidenceOrExisting(actual.attachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
                now
        ));
    }

    private AmusementSafetyInspectionRecord approveReopenInternal(String inspectionId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        AmusementSafetyInspectionRecord existing = findInspection(inspectionId);
        if (!"rechecked".equals(existing.rectificationStatus())) {
            throw ApiException.conflict("RECHECK_PASS_REQUIRED", "recheck pass is required before reopen approval");
        }
        Instant now = Instant.now();
        repository.saveSafetyAction(new AmusementSafetyActionRecord(
                "action_" + slug(actual.idempotencyKey()),
                inspectionId,
                "approve_reopen",
                trimToDefault(actual.action(), "approve"),
                requireText(actual.reason(), "reason"),
                evidence(actual.evidenceAttachmentIds()),
                now
        ));
        return repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                existing.id(),
                existing.projectId(),
                existing.templateId(),
                existing.items(),
                existing.overallResult(),
                existing.inspectionStatus(),
                existing.rectificationStatus(),
                "approved",
                existing.inspectedAt(),
                existing.submittedAt(),
                existing.recheckedAt(),
                now,
                actual.reason().trim(),
                evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
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
        if (!repository.incrementSessionUsedCapacityIfAvailable(session.id())) {
            throw ApiException.conflict("SESSION_CAPACITY_EXHAUSTED", "session capacity exhausted");
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
                Instant.now()
        ));
    }

    private AmusementManualOverrideRecord createManualOverrideInternal(AmusementManualOverrideRequest request) {
        AmusementManualOverrideRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
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

    private AmusementSafetyInspectionRecord findInspection(String inspectionId) {
        return repository.findSafetyInspectionById(inspectionId)
                .orElseThrow(() -> ApiException.notFound("SAFETY_INSPECTION_NOT_FOUND", "safety inspection not found"));
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
