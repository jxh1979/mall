package com.complex.platform.amusement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AmusementServiceTests {

    private InMemoryAmusementRepository amusementRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private AmusementService service;

    @BeforeEach
    void setUp() {
        amusementRepository = new InMemoryAmusementRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new AmusementService(
                amusementRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
    }

    @Test
    void safetyTemplateRejectsEmptyItems() {
        assertThatThrownBy(() -> service.createSafetyTemplate(new AmusementSafetyTemplateRequest(
                        "template-empty",
                        "empty",
                        List.of(),
                        "Daily Opening",
                        List.of(),
                        LocalDate.parse("2026-06-14")
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(amusementRepository.safetyTemplates).isEmpty();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("amusement.safety_template.change_failed");
    }

    @Test
    void projectSessionCreateAndListWriteOpenApiAuditEvents() {
        AmusementSafetyTemplateRecord template = createTemplate("project-session-template");
        AmusementProjectRecord project = createProject("project-session", template.id(), "open", 2);
        AmusementSessionRecord session = createSession("project-session", project.id(), 2, "open");

        assertThat(service.listProjects().items())
                .extracting(AmusementProjectRecord::id, AmusementProjectRecord::projectName)
                .contains(tuple(project.id(), "Roller Coaster project-session"));
        assertThat(service.listSessions().items())
                .extracting(AmusementSessionRecord::id, AmusementSessionRecord::projectId, AmusementSessionRecord::capacity)
                .contains(tuple(session.id(), project.id(), 2));
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "amusement.safety_template.changed",
                        "amusement.session.changed",
                        "amusement.session.viewed"
                );
    }

    @Test
    void failedInspectionBlocksOpenEligibilityUntilRectifyRecheckAndApprovalComplete() {
        AmusementProjectRecord project = projectWithTemplate("blocked");
        AmusementSafetyInspectionRecord failed = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-blocked",
                "inspect",
                List.of("att_fail"),
                project.id(),
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "fail")),
                "fail",
                Instant.parse("2026-06-14T08:00:00Z")
        ));

        service.submitSafetyInspection(failed.id(), action("submit-blocked", "submit", "submit failed inspection"));
        AmusementOpenEligibilityResponse blocked = service.getOpenEligibility(project.id());

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.blockedReasons()).contains("SAFETY_REOPEN_PENDING");

        service.rectifySafetyInspection(failed.id(), action("rectify-blocked", "submitted", "fixed restraint sensor"));
        service.recheckSafetyInspection(failed.id(), action("recheck-blocked", "pass", "passed recheck"));
        AmusementSafetyInspectionRecord approved = service.approveReopen(
                failed.id(),
                new StatusChangeRequest("approve-blocked", "approve", "supervisor approved", List.of("att_approve"))
        );

        assertThat(approved.reopenStatus()).isEqualTo("approved");
        assertThat(service.getOpenEligibility(project.id()).allowed()).isTrue();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "amusement.safety_inspection.blocked",
                        "amusement.open_eligibility.blocked",
                        "amusement.safety_inspection.rectification_submitted",
                        "amusement.safety_inspection.rechecked",
                        "amusement.safety_inspection.reopened",
                        "amusement.open_eligibility.viewed"
                );
    }

    @Test
    void passInspectionSubmitAllowsOpenEligibility() {
        AmusementProjectRecord project = projectWithTemplate("pass");
        AmusementSafetyInspectionRecord inspection = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-pass",
                "inspect",
                List.of(),
                project.id(),
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "pass")),
                "pass",
                Instant.parse("2026-06-14T08:00:00Z")
        ));

        AmusementSafetyInspectionRecord submitted = service.submitSafetyInspection(
                inspection.id(),
                action("submit-pass", "submit", "submit pass")
        );

        assertThat(submitted)
                .extracting(AmusementSafetyInspectionRecord::inspectionStatus, AmusementSafetyInspectionRecord::reopenStatus)
                .containsExactly("submitted", "not_required");
        assertThat(service.getOpenEligibility(project.id()).allowed()).isTrue();
    }

    @Test
    void approveReopenRejectsBeforeRecheckPass() {
        AmusementProjectRecord project = projectWithTemplate("approve-reject");
        AmusementSafetyInspectionRecord inspection = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-approve-reject",
                "inspect",
                List.of(),
                project.id(),
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "fail")),
                "fail",
                Instant.parse("2026-06-14T08:00:00Z")
        ));
        service.submitSafetyInspection(inspection.id(), action("submit-approve-reject", "submit", "submit fail"));
        service.rectifySafetyInspection(inspection.id(), action("rectify-approve-reject", "submitted", "fixed"));

        assertThatThrownBy(() -> service.approveReopen(
                        inspection.id(),
                        new StatusChangeRequest("approve-too-early", "approve", "too early", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RECHECK_PASS_REQUIRED");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.safety_inspection.reopen_rejected");
    }

    @Test
    void verificationRejectsClosedProjectBlockedSafetyFullSessionAndBlankCredential() {
        AmusementProjectRecord closedProject = projectWithTemplate("closed");
        amusementRepository.saveProject(new AmusementProjectRecord(
                closedProject.id(),
                closedProject.projectName(),
                closedProject.capacity(),
                "closed",
                closedProject.safetyTemplateId(),
                closedProject.eligibilityRule(),
                closedProject.createdAt(),
                Instant.parse("2026-06-14T09:00:00Z")
        ));
        AmusementSessionRecord closedSession = createSession("closed-session", closedProject.id(), 1, "open");

        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-closed",
                        closedProject.id(),
                        closedSession.id(),
                        "credential-1"
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("PROJECT_NOT_OPEN");

        AmusementProjectRecord blockedProject = projectWithTemplate("safety-blocked");
        AmusementSessionRecord blockedSession = createSession("blocked-session", blockedProject.id(), 1, "open");
        AmusementSafetyInspectionRecord failed = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-safety-blocked",
                "inspect",
                List.of(),
                blockedProject.id(),
                blockedProject.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "fail")),
                "fail",
                Instant.parse("2026-06-14T08:00:00Z")
        ));
        service.submitSafetyInspection(failed.id(), action("submit-safety-blocked", "submit", "submit fail"));

        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-safety-blocked",
                        blockedProject.id(),
                        blockedSession.id(),
                        "credential-2"
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SAFETY_BLOCKED");

        AmusementProjectRecord fullProject = projectWithTemplate("full");
        submitPassingInspection(fullProject, "full");
        AmusementSessionRecord fullSession = createSession("full-session", fullProject.id(), 1, "open");
        service.createVerification(verification("verify-full-first", fullProject.id(), fullSession.id(), "credential-3"));

        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-full-second",
                        fullProject.id(),
                        fullSession.id(),
                        "credential-4"
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_CAPACITY_EXHAUSTED");

        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-blank",
                        fullProject.id(),
                        fullSession.id(),
                        " "
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.verification_failed");
    }

    @Test
    void verificationIncrementsUsedCapacityOnlyOnSuccessAndIsIdempotent() {
        AmusementProjectRecord project = projectWithTemplate("verify-success");
        submitPassingInspection(project, "verify-success");
        AmusementSessionRecord session = createSession("verify-session", project.id(), 2, "open");
        VerificationRequest request = verification("verify-once", project.id(), session.id(), "credential-success");

        AmusementVerificationRecord first = service.createVerification(request);
        AmusementVerificationRecord second = service.createVerification(request);

        assertThat(second).isEqualTo(first);
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::usedCapacity)
                .isEqualTo(1);
        assertThat(amusementRepository.verifications).hasSize(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "amusement.verification_succeeded".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void manualOverrideRequiresReasonAndRecordsOverride() {
        AmusementProjectRecord project = projectWithTemplate("override");
        AmusementSessionRecord session = createSession("override-session", project.id(), 1, "open");

        assertThatThrownBy(() -> service.createManualOverride(new AmusementManualOverrideRequest(
                        "override-missing-reason",
                        "override",
                        List.of("att_1"),
                        project.id(),
                        session.id(),
                        "allow",
                        " "
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        AmusementManualOverrideRecord override = service.createManualOverride(new AmusementManualOverrideRequest(
                "override-once",
                "override",
                List.of("att_1"),
                project.id(),
                session.id(),
                "allow",
                "supervisor approved"
        ));

        assertThat(override)
                .extracting(AmusementManualOverrideRecord::overrideType, AmusementManualOverrideRecord::status)
                .containsExactly("allow", "approved");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.verification_override_failed", "amusement.verification_overridden");
    }

    @Test
    void reservationAndQueueWritesAreIdempotentStatusedAndAudited() {
        AmusementProjectRecord project = projectWithTemplate("reservation-queue");
        AmusementSessionRecord session = createSession("reservation-queue-session", project.id(), 3, "open");

        AmusementReservationRequest reservationRequest = new AmusementReservationRequest(
                "reservation-queue-once",
                project.id(),
                session.id(),
                "customer_1",
                null
        );
        AmusementReservationRecord firstReservation = service.createReservation(reservationRequest);
        AmusementReservationRecord secondReservation = service.createReservation(reservationRequest);

        assertThat(secondReservation).isEqualTo(firstReservation);
        assertThat(firstReservation)
                .extracting(
                        AmusementReservationRecord::projectId,
                        AmusementReservationRecord::sessionId,
                        AmusementReservationRecord::customerId,
                        AmusementReservationRecord::reservationStatus
                )
                .containsExactly(project.id(), session.id(), "customer_1", "reserved");

        AmusementReservationRecord completedReservation = service.changeReservationStatus(
                firstReservation.id(),
                new StatusChangeRequest("reservation-complete-once", "completed", "customer arrived", List.of("att_reservation"))
        );

        AmusementQueueEntryRequest queueRequest = new AmusementQueueEntryRequest(
                "queue-once",
                project.id(),
                session.id(),
                "customer_2",
                "A010",
                null
        );
        AmusementQueueEntryRecord firstQueue = service.createQueueEntry(queueRequest);
        AmusementQueueEntryRecord secondQueue = service.createQueueEntry(queueRequest);

        assertThat(secondQueue).isEqualTo(firstQueue);
        assertThat(firstQueue)
                .extracting(
                        AmusementQueueEntryRecord::projectId,
                        AmusementQueueEntryRecord::sessionId,
                        AmusementQueueEntryRecord::customerId,
                        AmusementQueueEntryRecord::queueNo,
                        AmusementQueueEntryRecord::status
                )
                .containsExactly(project.id(), session.id(), "customer_2", "A010", "waiting");

        AmusementQueueEntryRecord calledQueue = service.changeQueueEntryStatus(
                firstQueue.id(),
                new StatusChangeRequest("queue-call-once", "called", "notify customer", List.of("att_queue"))
        );

        assertThat(completedReservation.reservationStatus()).isEqualTo("completed");
        assertThat(calledQueue.status()).isEqualTo("called");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "amusement.reservation.created",
                        "amusement.reservation.status_changed",
                        "amusement.queue.created",
                        "amusement.queue.status_changed"
                );
    }

    @Test
    void reservationAndQueueRejectSessionMismatchClosedSessionAndMissingRows() {
        AmusementProjectRecord project = projectWithTemplate("reservation-reject");
        AmusementProjectRecord otherProject = projectWithTemplate("reservation-reject-other");
        AmusementSessionRecord otherSession = createSession("reservation-reject-other-session", otherProject.id(), 2, "open");
        AmusementSessionRecord closedSession = createSession("reservation-reject-closed-session", project.id(), 2, "closed");

        assertThatThrownBy(() -> service.createReservation(new AmusementReservationRequest(
                        "reservation-mismatch",
                        project.id(),
                        otherSession.id(),
                        "customer_1",
                        null
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_PROJECT_MISMATCH");

        assertThatThrownBy(() -> service.createQueueEntry(new AmusementQueueEntryRequest(
                        "queue-closed",
                        project.id(),
                        closedSession.id(),
                        "customer_1",
                        "A011",
                        null
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_NOT_ACCEPTING_ENTRY");

        assertThatThrownBy(() -> service.changeReservationStatus(
                        "missing_reservation",
                        new StatusChangeRequest("reservation-missing", "cancelled", "not found", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RESERVATION_NOT_FOUND");

        assertThatThrownBy(() -> service.changeQueueEntryStatus(
                        "missing_queue",
                        new StatusChangeRequest("queue-missing", "cancelled", "not found", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("QUEUE_ENTRY_NOT_FOUND");
    }

    @Test
    void groupBookingApprovalLocksCapacityAndReleaseRestoresAvailability() {
        AmusementProjectRecord project = projectWithTemplate("group-lock");
        submitPassingInspection(project, "group-lock");
        AmusementSessionRecord session = createSession("group-lock-session", project.id(), 2, "open");

        AmusementGroupBookingRecord booking = service.createGroupBooking(new AmusementGroupBookingRequest(
                "group-lock-create",
                "operator note",
                List.of("att_group"),
                project.id(),
                session.id(),
                "group",
                "Company Team",
                "13800000000",
                2,
                2
        ));

        assertThat(booking.bookingStatus()).isEqualTo("pending_approval");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(0);

        AmusementGroupBookingRecord approved = service.approveGroupBooking(
                booking.id(),
                new AmusementGroupBookingApprovalRequest("group-lock-approve", "ops approved", List.of("att_approve"))
        );

        assertThat(approved.bookingStatus()).isEqualTo("approved");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(2);
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-group-locked",
                        project.id(),
                        session.id(),
                        "credential-group-locked"
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_CAPACITY_EXHAUSTED");

        AmusementGroupBookingRecord released = service.releaseGroupBooking(
                booking.id(),
                new AmusementGroupBookingReleaseRequest("group-lock-release", "customer changed plan", List.of("att_release"))
        );

        assertThat(released.bookingStatus()).isEqualTo("released");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(0);
        assertThat(service.createVerification(verification(
                "verify-group-after-release",
                project.id(),
                session.id(),
                "credential-after-release"
        )).verificationResult()).isEqualTo("success");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "amusement.group_booking.created",
                        "amusement.group_booking.approved",
                        "amusement.group_booking.released"
                );
    }

    @Test
    void groupBookingRejectsRepeatedApproveAndRepeatedReleaseWithoutCapacityDrift() {
        AmusementProjectRecord project = projectWithTemplate("group-repeat");
        submitPassingInspection(project, "group-repeat");
        AmusementSessionRecord session = createSession("group-repeat-session", project.id(), 3, "open");
        AmusementGroupBookingRecord booking = service.createGroupBooking(new AmusementGroupBookingRequest(
                "group-repeat-create",
                "operator note",
                List.of("att_group"),
                project.id(),
                session.id(),
                "group",
                "Company Team",
                "13800000000",
                2,
                2
        ));

        service.approveGroupBooking(
                booking.id(),
                new AmusementGroupBookingApprovalRequest("group-repeat-approve", "ops approved", List.of())
        );

        assertThatThrownBy(() -> service.approveGroupBooking(
                        booking.id(),
                        new AmusementGroupBookingApprovalRequest("group-repeat-approve-again", "ops approved again", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("GROUP_BOOKING_NOT_PENDING");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(2);

        service.releaseGroupBooking(
                booking.id(),
                new AmusementGroupBookingReleaseRequest("group-repeat-release", "customer changed plan", List.of())
        );

        assertThatThrownBy(() -> service.releaseGroupBooking(
                        booking.id(),
                        new AmusementGroupBookingReleaseRequest("group-repeat-release-again", "customer changed plan again", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("GROUP_BOOKING_NOT_APPROVED");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(0);
    }

    @Test
    void groupBookingApprovalRejectsInsufficientRemainingCapacityWithoutMutation() {
        AmusementProjectRecord project = projectWithTemplate("group-capacity");
        submitPassingInspection(project, "group-capacity");
        AmusementSessionRecord session = createSession("group-capacity-session", project.id(), 2, "open");
        service.createVerification(verification("verify-before-group-lock", project.id(), session.id(), "credential-before-lock"));

        AmusementGroupBookingRecord booking = service.createGroupBooking(new AmusementGroupBookingRequest(
                "group-capacity-create",
                "operator note",
                List.of("att_group"),
                project.id(),
                session.id(),
                "private",
                "Private Event",
                "13900000000",
                2,
                2
        ));

        assertThatThrownBy(() -> service.approveGroupBooking(
                        booking.id(),
                        new AmusementGroupBookingApprovalRequest("group-capacity-approve", "ops approved", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_CAPACITY_LOCK_FAILED");

        assertThat(amusementRepository.findGroupBookingById(booking.id()))
                .get()
                .extracting(AmusementGroupBookingRecord::bookingStatus)
                .isEqualTo("pending_approval");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::usedCapacity, AmusementSessionRecord::lockedCapacity)
                .containsExactly(1, 0);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.group_booking.approve_failed");
    }

    private AmusementProjectRecord projectWithTemplate(String key) {
        AmusementSafetyTemplateRecord template = createTemplate(key + "-template");
        return createProject(key, template.id(), "open", 2);
    }

    private AmusementSafetyTemplateRecord createTemplate(String key) {
        return service.createSafetyTemplate(new AmusementSafetyTemplateRequest(
                "template-" + key,
                "create template",
                List.of("att_template"),
                "Daily Opening " + key,
                List.of(Map.of("name", "restraint", "required", true)),
                LocalDate.parse("2026-06-14")
        ));
    }

    private AmusementProjectRecord createProject(String key, String templateId, String status, int capacity) {
        return service.createProject(new AmusementProjectRequest(
                "project-" + key,
                "create project",
                List.of("att_project"),
                "Roller Coaster " + key,
                capacity,
                status,
                templateId,
                Map.of("min_age", 12)
        ));
    }

    private AmusementSessionRecord createSession(String key, String projectId, int capacity, String status) {
        return service.createSession(new AmusementSessionRequest(
                "session-" + key,
                "create session",
                List.of("att_session"),
                projectId,
                Instant.parse("2026-06-14T10:00:00Z"),
                Instant.parse("2026-06-14T11:00:00Z"),
                capacity,
                status
        ));
    }

    private AmusementSafetyActionRequest action(String key, String result, String comment) {
        return new AmusementSafetyActionRequest(key, comment, List.of("att_action"), result, comment, List.of("att_action"));
    }

    private void submitPassingInspection(AmusementProjectRecord project, String key) {
        AmusementSafetyInspectionRecord inspection = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-pass-" + key,
                "inspect",
                List.of(),
                project.id(),
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "pass")),
                "pass",
                Instant.parse("2026-06-14T08:00:00Z")
        ));
        service.submitSafetyInspection(inspection.id(), action("submit-pass-" + key, "submit", "submit pass"));
    }

    private VerificationRequest verification(String key, String projectId, String sessionId, String credentialPayload) {
        return new VerificationRequest(
                key,
                "qr",
                credentialPayload,
                "gate_1",
                projectId,
                Map.of("project_id", projectId, "session_id", sessionId, "staff_id", "staff_1")
        );
    }

    private static final class InMemoryAmusementRepository implements AmusementRepository {
        private final Map<String, AmusementSafetyTemplateRecord> safetyTemplates = new LinkedHashMap<>();
        private final Map<String, AmusementProjectRecord> projects = new LinkedHashMap<>();
        private final Map<String, AmusementProjectRuleRecord> projectRules = new LinkedHashMap<>();
        private final Map<String, AmusementSessionRecord> sessions = new LinkedHashMap<>();
        private final Map<String, AmusementSafetyInspectionRecord> inspections = new LinkedHashMap<>();
        private final Map<String, AmusementVerificationRecord> verifications = new LinkedHashMap<>();
        private final Map<String, AmusementManualOverrideRecord> manualOverrides = new LinkedHashMap<>();
        private final Map<String, AmusementDeviceStatusRecord> deviceStatuses = new LinkedHashMap<>();
        private final Map<String, AmusementQueueEntryRecord> queueEntries = new LinkedHashMap<>();
        private final Map<String, AmusementReservationRecord> reservations = new LinkedHashMap<>();
        private final Map<String, AmusementGroupBookingRecord> groupBookings = new LinkedHashMap<>();
        private final List<AmusementSafetyActionRecord> safetyActions = new ArrayList<>();

        @Override
        public AmusementSafetyTemplateRecord saveSafetyTemplate(AmusementSafetyTemplateRecord template) {
            safetyTemplates.put(template.id(), template);
            return template;
        }

        @Override
        public List<AmusementSafetyTemplateRecord> listSafetyTemplates() {
            return new ArrayList<>(safetyTemplates.values());
        }

        @Override
        public Optional<AmusementSafetyTemplateRecord> findSafetyTemplateById(String id) {
            return Optional.ofNullable(safetyTemplates.get(id));
        }

        @Override
        public AmusementProjectRecord saveProject(AmusementProjectRecord project) {
            projects.put(project.id(), project);
            return project;
        }

        @Override
        public List<AmusementProjectRecord> listProjects() {
            return new ArrayList<>(projects.values());
        }

        @Override
        public Optional<AmusementProjectRecord> findProjectById(String id) {
            return Optional.ofNullable(projects.get(id));
        }

        @Override
        public AmusementProjectRuleRecord saveProjectRule(AmusementProjectRuleRecord rule) {
            projectRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<AmusementProjectRuleRecord> listActiveBlockingProjectRules(String projectId) {
            return projectRules.values().stream()
                    .filter(rule -> projectId.equals(rule.projectId()))
                    .filter(AmusementProjectRuleRecord::blocking)
                    .filter(rule -> "active".equals(rule.status()))
                    .toList();
        }

        @Override
        public AmusementSessionRecord saveSession(AmusementSessionRecord session) {
            sessions.put(session.id(), session);
            return session;
        }

        @Override
        public List<AmusementSessionRecord> listSessions() {
            return new ArrayList<>(sessions.values());
        }

        @Override
        public Optional<AmusementSessionRecord> findSessionById(String id) {
            return Optional.ofNullable(sessions.get(id));
        }

        @Override
        public boolean incrementSessionUsedCapacityIfAvailable(String sessionId) {
            AmusementSessionRecord session = sessions.get(sessionId);
            if (session == null || session.usedCapacity() + session.lockedCapacity() >= session.capacity()) {
                return false;
            }
            Instant now = Instant.parse("2026-06-14T12:00:00Z");
            sessions.put(sessionId, new AmusementSessionRecord(
                    session.id(),
                    session.projectId(),
                    session.startTime(),
                    session.endTime(),
                    session.capacity(),
                    session.usedCapacity() + 1,
                    session.lockedCapacity(),
                    session.status(),
                    session.evidenceAttachmentIds(),
                    session.createdAt(),
                    now
            ));
            return true;
        }

        @Override
        public boolean lockSessionCapacityIfAvailable(String sessionId, int capacityToLock) {
            AmusementSessionRecord session = sessions.get(sessionId);
            if (session == null || capacityToLock <= 0 || session.usedCapacity() + session.lockedCapacity() + capacityToLock > session.capacity()) {
                return false;
            }
            Instant now = Instant.parse("2026-06-14T12:00:00Z");
            sessions.put(sessionId, new AmusementSessionRecord(
                    session.id(),
                    session.projectId(),
                    session.startTime(),
                    session.endTime(),
                    session.capacity(),
                    session.usedCapacity(),
                    session.lockedCapacity() + capacityToLock,
                    session.status(),
                    session.evidenceAttachmentIds(),
                    session.createdAt(),
                    now
            ));
            return true;
        }

        @Override
        public void releaseSessionLockedCapacity(String sessionId, int capacityToRelease) {
            AmusementSessionRecord session = sessions.get(sessionId);
            if (session == null || capacityToRelease <= 0) {
                return;
            }
            Instant now = Instant.parse("2026-06-14T12:00:00Z");
            sessions.put(sessionId, new AmusementSessionRecord(
                    session.id(),
                    session.projectId(),
                    session.startTime(),
                    session.endTime(),
                    session.capacity(),
                    session.usedCapacity(),
                    Math.max(session.lockedCapacity() - capacityToRelease, 0),
                    session.status(),
                    session.evidenceAttachmentIds(),
                    session.createdAt(),
                    now
            ));
        }

        @Override
        public AmusementGroupBookingRecord saveGroupBooking(AmusementGroupBookingRecord booking) {
            groupBookings.put(booking.id(), booking);
            return booking;
        }

        @Override
        public List<AmusementGroupBookingRecord> listGroupBookings() {
            return new ArrayList<>(groupBookings.values());
        }

        @Override
        public Optional<AmusementGroupBookingRecord> findGroupBookingById(String id) {
            return Optional.ofNullable(groupBookings.get(id));
        }

        @Override
        public Optional<AmusementGroupBookingRecord> approveGroupBookingWithCapacityLock(
                String id,
                String approvalNote,
                List<String> evidenceAttachmentIds,
                Instant approvedAt
        ) {
            AmusementGroupBookingRecord existing = groupBookings.get(id);
            if (existing == null || !"pending_approval".equals(existing.bookingStatus())) {
                return Optional.empty();
            }
            if (!lockSessionCapacityIfAvailable(existing.sessionId(), existing.reservedCapacity())) {
                return Optional.empty();
            }
            AmusementGroupBookingRecord approved = new AmusementGroupBookingRecord(
                    existing.id(),
                    existing.projectId(),
                    existing.sessionId(),
                    existing.bookingType(),
                    existing.customerName(),
                    existing.contactPhone(),
                    existing.partySize(),
                    existing.reservedCapacity(),
                    "approved",
                    approvalNote,
                    existing.operatorNote(),
                    evidenceAttachmentIds,
                    approvedAt,
                    existing.releasedAt(),
                    existing.createdAt(),
                    approvedAt
            );
            groupBookings.put(id, approved);
            return Optional.of(approved);
        }

        @Override
        public Optional<AmusementGroupBookingRecord> releaseGroupBookingAndCapacity(
                String id,
                String operatorNote,
                List<String> evidenceAttachmentIds,
                Instant releasedAt
        ) {
            AmusementGroupBookingRecord existing = groupBookings.get(id);
            if (existing == null || !"approved".equals(existing.bookingStatus())) {
                return Optional.empty();
            }
            releaseSessionLockedCapacity(existing.sessionId(), existing.reservedCapacity());
            AmusementGroupBookingRecord released = new AmusementGroupBookingRecord(
                    existing.id(),
                    existing.projectId(),
                    existing.sessionId(),
                    existing.bookingType(),
                    existing.customerName(),
                    existing.contactPhone(),
                    existing.partySize(),
                    existing.reservedCapacity(),
                    "released",
                    existing.approvalNote(),
                    operatorNote,
                    evidenceAttachmentIds,
                    existing.approvedAt(),
                    releasedAt,
                    existing.createdAt(),
                    releasedAt
            );
            groupBookings.put(id, released);
            return Optional.of(released);
        }

        @Override
        public AmusementSafetyInspectionRecord saveSafetyInspection(AmusementSafetyInspectionRecord inspection) {
            inspections.put(inspection.id(), inspection);
            return inspection;
        }

        @Override
        public List<AmusementSafetyInspectionRecord> listSafetyInspections() {
            return new ArrayList<>(inspections.values());
        }

        @Override
        public Optional<AmusementSafetyInspectionRecord> findSafetyInspectionById(String id) {
            return Optional.ofNullable(inspections.get(id));
        }

        @Override
        public Optional<AmusementSafetyInspectionRecord> latestSafetyInspectionForProject(String projectId) {
            return inspections.values().stream()
                    .filter(inspection -> projectId.equals(inspection.projectId()))
                    .reduce((first, second) -> second);
        }

        @Override
        public AmusementSafetyActionRecord saveSafetyAction(AmusementSafetyActionRecord action) {
            safetyActions.add(action);
            return action;
        }

        @Override
        public AmusementVerificationRecord saveVerification(AmusementVerificationRecord verification) {
            verifications.put(verification.id(), verification);
            return verification;
        }

        @Override
        public List<AmusementVerificationRecord> listVerifications() {
            return new ArrayList<>(verifications.values());
        }

        @Override
        public AmusementManualOverrideRecord saveManualOverride(AmusementManualOverrideRecord override) {
            manualOverrides.put(override.id(), override);
            return override;
        }

        @Override
        public List<AmusementManualOverrideRecord> listManualOverrides() {
            return new ArrayList<>(manualOverrides.values());
        }

        @Override
        public AmusementDeviceStatusRecord saveDeviceStatus(AmusementDeviceStatusRecord status) {
            deviceStatuses.put(status.id(), status);
            return status;
        }

        @Override
        public List<AmusementDeviceStatusRecord> listDeviceStatuses() {
            return new ArrayList<>(deviceStatuses.values());
        }

        @Override
        public AmusementQueueEntryRecord saveQueueEntry(AmusementQueueEntryRecord entry) {
            queueEntries.put(entry.id(), entry);
            return entry;
        }

        @Override
        public List<AmusementQueueEntryRecord> listQueueEntries() {
            return new ArrayList<>(queueEntries.values());
        }

        @Override
        public Optional<AmusementQueueEntryRecord> findQueueEntryById(String id) {
            return Optional.ofNullable(queueEntries.get(id));
        }

        @Override
        public AmusementReservationRecord saveReservation(AmusementReservationRecord reservation) {
            reservations.put(reservation.id(), reservation);
            return reservation;
        }

        @Override
        public List<AmusementReservationRecord> listReservations() {
            return new ArrayList<>(reservations.values());
        }

        @Override
        public Optional<AmusementReservationRecord> findReservationById(String id) {
            return Optional.ofNullable(reservations.get(id));
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }

    }
}
