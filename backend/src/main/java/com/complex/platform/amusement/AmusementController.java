package com.complex.platform.amusement;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/amusement")
public class AmusementController {

    private final AmusementService service;
    private final AuthorizationService authorizationService;

    public AmusementController(AmusementService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/device-statuses")
    public ApiResponse<PageResponse<AmusementDeviceStatusRecord>> listDeviceStatuses() {
        authorizationService.requirePermission("amusement.device_status.view");
        return ApiResponse.ok(service.listDeviceStatuses());
    }

    @GetMapping("/manual-overrides")
    public ApiResponse<PageResponse<AmusementManualOverrideRecord>> listManualOverrides() {
        authorizationService.requirePermission("amusement.verification.override");
        return ApiResponse.ok(service.listManualOverrides());
    }

    @PostMapping("/manual-overrides")
    public ApiResponse<AmusementManualOverrideRecord> createManualOverride(
            @RequestBody(required = false) AmusementManualOverrideRequest request
    ) {
        authorizationService.requirePermission("amusement.verification.override");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createManualOverride(request));
    }

    @GetMapping("/projects")
    public ApiResponse<PageResponse<AmusementProjectRecord>> listProjects() {
        authorizationService.requirePermission("amusement.session.view");
        return ApiResponse.ok(service.listProjects());
    }

    @PostMapping("/projects")
    public ApiResponse<AmusementProjectRecord> createProject(@RequestBody(required = false) AmusementProjectRequest request) {
        authorizationService.requirePermission("amusement.session.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProject(request));
    }

    @GetMapping("/projects/{id}/open-eligibility")
    public ApiResponse<AmusementOpenEligibilityResponse> getOpenEligibility(@PathVariable String id) {
        authorizationService.requirePermission("amusement.safety_inspection.view");
        return ApiResponse.ok(service.getOpenEligibility(id));
    }

    @PostMapping("/projects/{id}/rules")
    public ApiResponse<AmusementProjectRuleRecord> createProjectRule(
            @PathVariable String id,
            @RequestBody(required = false) AmusementProjectRuleRequest request
    ) {
        authorizationService.requirePermission("amusement.project.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProjectRule(id, request));
    }

    @GetMapping("/queue-entries")
    public ApiResponse<PageResponse<AmusementQueueEntryRecord>> listQueueEntries() {
        authorizationService.requirePermission("amusement.queue.view");
        return ApiResponse.ok(service.listQueueEntries());
    }

    @PostMapping("/queue-entries")
    public ApiResponse<AmusementQueueEntryRecord> createQueueEntry(
            @RequestBody(required = false) AmusementQueueEntryRequest request
    ) {
        authorizationService.requirePermission("amusement.queue.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createQueueEntry(request));
    }

    @PostMapping("/queue-entries/{id}/status")
    public ApiResponse<AmusementQueueEntryRecord> changeQueueEntryStatus(
            @PathVariable String id,
            @RequestBody(required = false) StatusChangeRequest request
    ) {
        authorizationService.requirePermission("amusement.queue.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.changeQueueEntryStatus(id, request));
    }

    @GetMapping("/reservations")
    public ApiResponse<PageResponse<AmusementReservationRecord>> listReservations() {
        authorizationService.requirePermission("amusement.reservation.view");
        return ApiResponse.ok(service.listReservations());
    }

    @PostMapping("/reservations")
    public ApiResponse<AmusementReservationRecord> createReservation(
            @RequestBody(required = false) AmusementReservationRequest request
    ) {
        authorizationService.requirePermission("amusement.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createReservation(request));
    }

    @PostMapping("/reservations/{id}/status")
    public ApiResponse<AmusementReservationRecord> changeReservationStatus(
            @PathVariable String id,
            @RequestBody(required = false) StatusChangeRequest request
    ) {
        authorizationService.requirePermission("amusement.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.changeReservationStatus(id, request));
    }

    @GetMapping("/group-bookings")
    public ApiResponse<PageResponse<AmusementGroupBookingRecord>> listGroupBookings() {
        authorizationService.requirePermission("amusement.group_booking.view");
        return ApiResponse.ok(service.listGroupBookings());
    }

    @PostMapping("/group-bookings")
    public ApiResponse<AmusementGroupBookingRecord> createGroupBooking(
            @RequestBody(required = false) AmusementGroupBookingRequest request
    ) {
        authorizationService.requirePermission("amusement.group_booking.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createGroupBooking(request));
    }

    @PostMapping("/group-bookings/{id}/approve")
    public ApiResponse<AmusementGroupBookingRecord> approveGroupBooking(
            @PathVariable String id,
            @RequestBody(required = false) AmusementGroupBookingApprovalRequest request
    ) {
        authorizationService.requirePermission("amusement.group_booking.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveGroupBooking(id, request));
    }

    @PostMapping("/group-bookings/{id}/release")
    public ApiResponse<AmusementGroupBookingRecord> releaseGroupBooking(
            @PathVariable String id,
            @RequestBody(required = false) AmusementGroupBookingReleaseRequest request
    ) {
        authorizationService.requirePermission("amusement.group_booking.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.releaseGroupBooking(id, request));
    }

    @GetMapping("/safety-inspections")
    public ApiResponse<PageResponse<AmusementSafetyInspectionRecord>> listSafetyInspections() {
        authorizationService.requirePermission("amusement.safety_inspection.view");
        return ApiResponse.ok(service.listSafetyInspections());
    }

    @PostMapping("/safety-inspections")
    public ApiResponse<AmusementSafetyInspectionRecord> createSafetyInspection(
            @RequestBody(required = false) AmusementSafetyInspectionRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_inspection.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSafetyInspection(request));
    }

    @PostMapping("/safety-inspections/{id}/approve-reopen")
    public ApiResponse<AmusementSafetyInspectionRecord> approveReopen(
            @PathVariable String id,
            @RequestBody(required = false) StatusChangeRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_inspection.approve_reopen");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveReopen(id, request));
    }

    @PostMapping("/safety-inspections/{id}/recheck")
    public ApiResponse<AmusementSafetyInspectionRecord> recheckSafetyInspection(
            @PathVariable String id,
            @RequestBody(required = false) AmusementSafetyActionRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_inspection.recheck");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recheckSafetyInspection(id, request));
    }

    @PostMapping("/safety-inspections/{id}/rectify")
    public ApiResponse<AmusementSafetyInspectionRecord> rectifySafetyInspection(
            @PathVariable String id,
            @RequestBody(required = false) AmusementSafetyActionRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_inspection.rectify");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.rectifySafetyInspection(id, request));
    }

    @PostMapping("/safety-inspections/{id}/submit")
    public ApiResponse<AmusementSafetyInspectionRecord> submitSafetyInspection(
            @PathVariable String id,
            @RequestBody(required = false) AmusementSafetyActionRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_inspection.submit");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitSafetyInspection(id, request));
    }

    @GetMapping("/maintenance-records")
    public ApiResponse<PageResponse<AmusementMaintenanceRecord>> listMaintenanceRecords() {
        authorizationService.requirePermission("amusement.maintenance.read");
        return ApiResponse.ok(service.listMaintenanceRecords());
    }

    @GetMapping("/maintenance-records/{id}")
    public ApiResponse<AmusementMaintenanceRecord> getMaintenanceRecord(@PathVariable String id) {
        authorizationService.requirePermission("amusement.maintenance.read");
        return ApiResponse.ok(service.getMaintenanceRecord(id));
    }

    @PostMapping("/maintenance-records")
    public ApiResponse<AmusementMaintenanceRecord> createMaintenanceRecord(
            @RequestBody(required = false) AmusementMaintenanceRecordRequest request
    ) {
        authorizationService.requirePermission("amusement.maintenance.submit");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createMaintenanceRecord(request));
    }

    @PostMapping("/maintenance-records/{id}/confirm-resume")
    public ApiResponse<AmusementMaintenanceRecord> confirmMaintenanceResume(
            @PathVariable String id,
            @RequestBody(required = false) AmusementMaintenanceResumeRequest request
    ) {
        authorizationService.requirePermission("amusement.maintenance.confirm");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmMaintenanceResume(id, request));
    }

    @GetMapping("/safety-templates")
    public ApiResponse<PageResponse<AmusementSafetyTemplateRecord>> listSafetyTemplates() {
        authorizationService.requirePermission("amusement.safety_template.view");
        return ApiResponse.ok(service.listSafetyTemplates());
    }

    @PostMapping("/safety-templates")
    public ApiResponse<AmusementSafetyTemplateRecord> createSafetyTemplate(
            @RequestBody(required = false) AmusementSafetyTemplateRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_template.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSafetyTemplate(request));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResponse<AmusementSessionRecord>> listSessions() {
        authorizationService.requirePermission("amusement.session.view");
        return ApiResponse.ok(service.listSessions());
    }

    @PostMapping("/sessions")
    public ApiResponse<AmusementSessionRecord> createSession(@RequestBody(required = false) AmusementSessionRequest request) {
        authorizationService.requirePermission("amusement.session.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSession(request));
    }

    @GetMapping("/verifications")
    public ApiResponse<PageResponse<AmusementVerificationRecord>> listVerifications() {
        authorizationService.requirePermission("amusement.verification.view");
        return ApiResponse.ok(service.listVerifications());
    }

    @PostMapping("/verifications")
    public ApiResponse<AmusementVerificationRecord> createVerification(@RequestBody(required = false) VerificationRequest request) {
        authorizationService.requirePermission("amusement.verification.execute");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createVerification(request));
    }
}
