package com.complex.platform.amusement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.amusement.dto.AmusementIncidentActionRequest;
import com.complex.platform.amusement.dto.AmusementIncidentCloseRequest;
import com.complex.platform.amusement.dto.AmusementIncidentCreateRequest;
import com.complex.platform.amusement.dto.AmusementSafetyNoticeCreateRequest;
import com.complex.platform.amusement.dto.AmusementWaiverAcknowledgementRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/amusement")
public class AmusementSafetyController {

    private final AmusementSafetyService service;
    private final AuthorizationService authorizationService;

    public AmusementSafetyController(
            AmusementSafetyService service,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/safety-notices")
    public ApiResponse<PageResponse<AmusementSafetyNoticeRecord>> listSafetyNotices() {
        authorizationService.requirePermission("amusement.safety_notice.view");
        return ApiResponse.ok(service.listSafetyNotices());
    }

    @PostMapping("/safety-notices")
    public ApiResponse<AmusementSafetyNoticeRecord> createSafetyNotice(
            @RequestBody AmusementSafetyNoticeCreateRequest request
    ) {
        authorizationService.requirePermission("amusement.safety_notice.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSafetyNotice(request));
    }

    @PostMapping("/safety-notices/{noticeId}/acknowledgements")
    public ApiResponse<AmusementWaiverAcknowledgementRecord> acknowledgeWaiver(
            @PathVariable String noticeId,
            @RequestBody AmusementWaiverAcknowledgementRequest request
    ) {
        authorizationService.requirePermission("amusement.waiver.acknowledge");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.acknowledgeWaiver(noticeId, request));
    }

    @GetMapping("/incidents")
    public ApiResponse<PageResponse<AmusementIncidentRecord>> listIncidents() {
        authorizationService.requirePermission("amusement.incident.view");
        return ApiResponse.ok(service.listIncidents());
    }

    @PostMapping("/incidents")
    public ApiResponse<AmusementIncidentRecord> createIncident(
            @RequestBody AmusementIncidentCreateRequest request
    ) {
        authorizationService.requirePermission("amusement.incident.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createIncident(request));
    }

    @PostMapping("/incidents/{incidentId}/actions")
    public ApiResponse<AmusementIncidentRecord> addIncidentAction(
            @PathVariable String incidentId,
            @RequestBody AmusementIncidentActionRequest request
    ) {
        authorizationService.requirePermission("amusement.incident.process");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.addIncidentAction(incidentId, request));
    }

    @PostMapping("/incidents/{incidentId}/close")
    public ApiResponse<AmusementIncidentRecord> closeIncident(
            @PathVariable String incidentId,
            @RequestBody AmusementIncidentCloseRequest request
    ) {
        authorizationService.requirePermission("amusement.incident.close");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.closeIncident(incidentId, request));
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
    }
}
