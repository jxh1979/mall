package com.complex.platform.facility;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.SiteEntryReportAcceptanceRequest;
import com.complex.platform.facility.dto.SiteEntryReportRequest;
import com.complex.platform.facility.dto.SiteEntryReportReviewRequest;
import com.complex.platform.facility.dto.SiteEntryReportSubmitRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facility/site-entry-reports")
public class SiteEntryReportController {

    private static final String VIEW_ASSIGNED = "facility.site_entry.view_assigned";
    private static final String REVIEW = "facility.site_entry.review";
    private static final String ACCEPT = "facility.site_entry.accept";

    private final SiteEntryReportService service;
    private final AuthorizationService authorizationService;

    public SiteEntryReportController(SiteEntryReportService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SiteEntryReportRecord>> list() {
        requireReadPermission();
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{report_id}")
    public ApiResponse<SiteEntryReportRecord> get(@PathVariable("report_id") String reportId) {
        requireReadPermission();
        return ApiResponse.ok(service.findById(reportId));
    }

    @PostMapping
    public ApiResponse<SiteEntryReportRecord> create(@RequestBody @jakarta.validation.Valid SiteEntryReportRequest request) {
        authorizationService.requirePermission("facility.site_entry.create_assigned");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.create(request));
    }

    @PostMapping("/{report_id}/submit")
    public ApiResponse<SiteEntryReportRecord> submit(
            @PathVariable("report_id") String reportId, @RequestBody @jakarta.validation.Valid SiteEntryReportSubmitRequest request) {
        authorizationService.requirePermission("facility.site_entry.submit_assigned");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submit(reportId, request));
    }

    @PostMapping("/{report_id}/review")
    public ApiResponse<SiteEntryReportRecord> review(
            @PathVariable("report_id") String reportId, @RequestBody @jakarta.validation.Valid SiteEntryReportReviewRequest request) {
        authorizationService.requirePermission(REVIEW);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.review(reportId, request));
    }

    @PostMapping("/{report_id}/accept")
    public ApiResponse<SiteEntryReportRecord> accept(
            @PathVariable("report_id") String reportId, @RequestBody @jakarta.validation.Valid SiteEntryReportAcceptanceRequest request) {
        authorizationService.requirePermission(ACCEPT);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.accept(reportId, request));
    }

    private void requireReadPermission() {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePermission(authorizationService.isOutsourcedVendor(actor) ? VIEW_ASSIGNED : REVIEW);
    }
}
