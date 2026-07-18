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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
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
            @RequestBody(required = false) @jakarta.validation.Valid AmusementManualOverrideRequest request
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
    public ApiResponse<AmusementProjectRecord> createProject(@RequestBody(required = false) @jakarta.validation.Valid AmusementProjectRequest request) {
        authorizationService.requirePermission("amusement.session.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProject(request));
    }

    @GetMapping("/projects/without-space")
    public ApiResponse<PageResponse<AmusementProjectSpaceBacklogRecord>> listProjectsWithoutSpace() {
        authorizationService.requirePermission("amusement.session.manage");
        return ApiResponse.ok(service.listProjectsWithoutSpace());
    }

    @GetMapping("/projects/without-device-registry")
    public ApiResponse<PageResponse<AmusementProjectDeviceRegistryBacklogRecord>> listProjectsMissingDeviceRegistry() {
        authorizationService.requirePermission("amusement.session.manage");
        return ApiResponse.ok(service.listProjectsMissingDeviceRegistry());
    }

    @PutMapping("/projects/{id}/space")
    public ApiResponse<AmusementProjectSpaceAssignmentRecord> assignProjectSpace(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementProjectSpaceAssignmentRequest request
    ) {
        authorizationService.requirePermission("amusement.session.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignProjectSpace(id, request));
    }

    @GetMapping("/projects/{id}/open-eligibility")
    public ApiResponse<AmusementOpenEligibilityResponse> getOpenEligibility(@PathVariable String id) {
        authorizationService.requirePermission("amusement.safety_inspection.view");
        return ApiResponse.ok(service.getOpenEligibility(id));
    }

    @PostMapping("/projects/{id}/rules")
    public ApiResponse<AmusementProjectRuleRecord> createProjectRule(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementProjectRuleRequest request
    ) {
        authorizationService.requirePermission("amusement.project.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProjectRule(id, request));
    }

    @GetMapping("/projects/{id}/entitlements")
    public ApiResponse<PageResponse<AmusementProjectEntitlementRecord>> listProjectEntitlements(@PathVariable String id) {
        authorizationService.requirePermission("amusement.project_entitlement.manage");
        return ApiResponse.ok(service.listProjectEntitlements(id));
    }

    @PostMapping("/projects/{id}/entitlements")
    public ApiResponse<AmusementProjectEntitlementRecord> createProjectEntitlement(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementProjectEntitlementRequest request
    ) {
        authorizationService.requirePermission("amusement.project_entitlement.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProjectEntitlement(id, request));
    }

    @PostMapping("/projects/{id}/entitlements/{entitlementType}/remove")
    public ApiResponse<AmusementProjectEntitlementRecord> removeProjectEntitlement(
            @PathVariable String id,
            @PathVariable String entitlementType,
            @RequestBody(required = false) @jakarta.validation.Valid com.complex.platform.ticketing.dto.StatusChangeRequest request
    ) {
        authorizationService.requirePermission("amusement.project_entitlement.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.deleteProjectEntitlement(
                id, entitlementType, request == null ? null : request.idempotencyKey()));
    }

    @GetMapping("/projects/{id}/capacity-config")
    public ApiResponse<PageResponse<AmusementCapacityConfigRecord>> listCapacityConfigs(@PathVariable String id) {
        authorizationService.requirePermission("amusement.capacity_config.manage");
        return ApiResponse.ok(service.listCapacityConfigs(id));
    }

    @PostMapping("/projects/{id}/capacity-config")
    public ApiResponse<AmusementCapacityConfigRecord> createCapacityConfig(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementCapacityConfigRequest request
    ) {
        authorizationService.requirePermission("amusement.capacity_config.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createCapacityConfig(id, request));
    }

    @GetMapping("/projects/{id}/devices")
    public ApiResponse<PageResponse<AmusementDeviceRecord>> listDevices(@PathVariable String id) {
        authorizationService.requirePermission("amusement.device.view");
        return ApiResponse.ok(service.listDevices(id));
    }

    @PostMapping("/projects/{id}/devices")
    public ApiResponse<AmusementDeviceRecord> createDevice(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementDeviceRequest request
    ) {
        authorizationService.requirePermission("amusement.device.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDevice(id, request));
    }

    @PostMapping("/capacity-config/{configId}/disable")
    public ApiResponse<AmusementCapacityConfigRecord> disableCapacityConfig(
            @PathVariable String configId,
            @RequestBody(required = false) @jakarta.validation.Valid com.complex.platform.ticketing.dto.StatusChangeRequest request
    ) {
        authorizationService.requirePermission("amusement.capacity_config.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.disableCapacityConfig(
                configId, request == null ? null : request.idempotencyKey()));
    }

    @GetMapping("/projects/{projectId}/over-capacity-policy")
    public ApiResponse<AmusementOverCapacityPolicy> getOverCapacityPolicy(@PathVariable String projectId) {
        authorizationService.requirePermission("amusement.over_capacity_policy.view");
        return ApiResponse.ok(service.getOverCapacityPolicy(projectId));
    }

    @PutMapping("/projects/{projectId}/over-capacity-policy")
    public ApiResponse<AmusementOverCapacityPolicy> configureOverCapacityPolicy(
            @PathVariable String projectId,
            @RequestBody(required = false) @jakarta.validation.Valid ConfigureOverCapacityPolicyRequest request
    ) {
        authorizationService.requirePermission("amusement.over_capacity_policy.configure");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.configureOverCapacityPolicy(projectId, request));
    }

    // ── REQ-53 权益会话（V129）：发起 → 展示 → 领取核销（防错领 + 会话时效）──
    @GetMapping("/entitlement-sessions")
    public ApiResponse<PageResponse<EntitlementSessionRecord>> listEntitlementSessions() {
        authorizationService.requirePermission("amusement.entitlement_session.view");
        return ApiResponse.ok(service.listEntitlementSessions());
    }

    @PostMapping("/entitlement-sessions")
    public ApiResponse<EntitlementSessionRecord> initiateEntitlementSession(
            @RequestBody(required = false) @jakarta.validation.Valid InitiateEntitlementSessionRequest request
    ) {
        authorizationService.requirePermission("amusement.entitlement_session.initiate");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.initiateEntitlementSession(request));
    }

    @GetMapping("/entitlement-sessions/{id}")
    public ApiResponse<EntitlementSessionRecord> getEntitlementSession(@PathVariable String id) {
        authorizationService.requirePermission("amusement.entitlement_session.view");
        return ApiResponse.ok(service.getEntitlementSession(id));
    }

    @PostMapping("/entitlement-sessions/{id}/claim")
    public ApiResponse<EntitlementSessionRecord> claimEntitlementSession(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid ClaimEntitlementSessionRequest request
    ) {
        authorizationService.requirePermission("amusement.entitlement_session.claim");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.claimEntitlementSession(id, request));
    }

    @GetMapping("/queue-entries")
    public ApiResponse<PageResponse<AmusementQueueEntryRecord>> listQueueEntries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        authorizationService.requirePermission("amusement.queue.view");
        return ApiResponse.ok(service.listQueueEntries(page, size));
    }

    @PostMapping("/queue-entries")
    public ApiResponse<AmusementQueueEntryRecord> createQueueEntry(
            @RequestBody(required = false) @jakarta.validation.Valid AmusementQueueEntryRequest request
    ) {
        authorizationService.requirePermission("amusement.queue.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createQueueEntry(request));
    }

    @PostMapping("/queue-entries/{id}/status")
    public ApiResponse<AmusementQueueEntryRecord> changeQueueEntryStatus(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("amusement.queue.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.changeQueueEntryStatus(id, request));
    }

    @GetMapping("/reservations")
    public ApiResponse<PageResponse<AmusementReservationRecord>> listReservations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        authorizationService.requirePermission("amusement.reservation.view");
        return ApiResponse.ok(service.listReservations(page, size));
    }

    @PostMapping("/reservations")
    public ApiResponse<AmusementReservationRecord> createReservation(
            @RequestBody(required = false) @jakarta.validation.Valid AmusementReservationRequest request
    ) {
        authorizationService.requirePermission("amusement.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createReservation(request));
    }

    @PostMapping("/reservations/{id}/status")
    public ApiResponse<AmusementReservationRecord> changeReservationStatus(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("amusement.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.changeReservationStatus(id, request));
    }

    @GetMapping("/group-bookings")
    public ApiResponse<PageResponse<AmusementGroupBookingRecord>> listGroupBookings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        authorizationService.requirePermission("amusement.group_booking.view");
        return ApiResponse.ok(service.listGroupBookings(page, size));
    }

    @PostMapping("/group-bookings")
    public ApiResponse<AmusementGroupBookingRecord> createGroupBooking(
            @RequestBody(required = false) @jakarta.validation.Valid AmusementGroupBookingRequest request
    ) {
        authorizationService.requirePermission("amusement.group_booking.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createGroupBooking(request));
    }

    @PostMapping("/group-bookings/{id}/approve")
    public ApiResponse<AmusementGroupBookingRecord> approveGroupBooking(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementGroupBookingApprovalRequest request
    ) {
        authorizationService.requirePermission("amusement.group_booking.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveGroupBooking(id, request));
    }

    @PostMapping("/group-bookings/{id}/release")
    public ApiResponse<AmusementGroupBookingRecord> releaseGroupBooking(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementGroupBookingReleaseRequest request
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

    @GetMapping("/inspection-tasks")
    public ApiResponse<PageResponse<AmusementInspectionTaskRecord>> listInspectionTasks() {
        authorizationService.requirePermission("amusement.safety_inspection.view");
        return ApiResponse.ok(service.listInspectionTasks());
    }

    @PostMapping("/safety-inspections")
    public ApiResponse<AmusementSafetyInspectionRecord> createSafetyInspection(
            @RequestBody(required = false) @jakarta.validation.Valid AmusementSafetyInspectionRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_inspection.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSafetyInspection(request));
    }

    @PostMapping("/safety-inspections/{id}/approve-reopen")
    public ApiResponse<AmusementSafetyInspectionRecord> approveReopen(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_inspection.approve_reopen");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveReopen(id, request));
    }

    @PostMapping("/safety-inspections/{id}/recheck")
    public ApiResponse<AmusementSafetyInspectionRecord> recheckSafetyInspection(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementSafetyActionRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_inspection.recheck");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recheckSafetyInspection(id, request));
    }

    @PostMapping("/safety-inspections/{id}/rectify")
    public ApiResponse<AmusementSafetyInspectionRecord> rectifySafetyInspection(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementSafetyActionRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_inspection.rectify");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.rectifySafetyInspection(id, request));
    }

    @PostMapping("/safety-inspections/{id}/submit")
    public ApiResponse<AmusementSafetyInspectionRecord> submitSafetyInspection(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementSafetyActionRequest request
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
            @RequestBody(required = false) @jakarta.validation.Valid AmusementMaintenanceRecordRequest request
    ) {
        authorizationService.requirePermission("amusement.maintenance.submit");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createMaintenanceRecord(request));
    }

    @PostMapping("/maintenance-records/{id}/confirm-resume")
    public ApiResponse<AmusementMaintenanceRecord> confirmMaintenanceResume(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AmusementMaintenanceResumeRequest request
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
            @RequestBody(required = false) @jakarta.validation.Valid AmusementSafetyTemplateRequest request
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
    public ApiResponse<AmusementSessionRecord> createSession(@RequestBody(required = false) @jakarta.validation.Valid AmusementSessionRequest request) {
        authorizationService.requirePermission("amusement.session.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSession(request));
    }

    @GetMapping("/verifications")
    public ApiResponse<PageResponse<AmusementVerificationListItem>> listVerifications(
            @RequestParam(name = "projectId", required = false) String projectId,
            @RequestParam(name = "sessionId", required = false) String sessionId,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        authorizationService.requirePermission("amusement.verification.view");
        return ApiResponse.ok(service.listVerificationItems(
                new AmusementVerificationQuery(projectId, sessionId, direction, dateFrom, dateTo)));
    }

    /** 阶段3 只读：并发在场容量看板数据（occupied/capacity，维度 point_device/project/session）。 */
    @GetMapping("/capacity-snapshots")
    public ApiResponse<PageResponse<AmusementCapacitySnapshotRecord>> listCapacitySnapshots() {
        authorizationService.requirePermission("amusement.verification.view");
        return ApiResponse.ok(service.listCapacitySnapshots());
    }

    @PostMapping("/verifications")
    public ApiResponse<AmusementVerificationRecord> createVerification(@RequestBody(required = false) @jakarta.validation.Valid VerificationRequest request) {
        authorizationService.requirePermission("amusement.verification.execute");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createVerification(request));
    }

    /** 阶段2：出场核销（自由进出型并发容量 -N 释放；场次/批次型幂等跳过）。 */
    @PostMapping("/verifications/exit")
    public ApiResponse<AmusementVerificationRecord> exitVerification(
            @RequestBody(required = false) @jakarta.validation.Valid AmusementExitRequest request) {
        authorizationService.requirePermission("amusement.verification.execute");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.exitVerification(request));
    }
}
