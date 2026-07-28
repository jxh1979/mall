package com.complex.platform.facility;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.PhoneMask;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.SiteEntryCompletionUploadRequest;
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
    private static final String UPLOAD_COMPLETION_ASSIGNED = "facility.site_entry.upload_completion_assigned";
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
        return ApiResponse.ok(maskPhonesForUnauthorized(service.list()));
    }

    @GetMapping("/{report_id}")
    public ApiResponse<SiteEntryReportRecord> get(@PathVariable("report_id") String reportId) {
        requireReadPermission();
        return ApiResponse.ok(maskPhoneForUnauthorized(service.findById(reportId)));
    }

    @PostMapping
    public ApiResponse<SiteEntryReportRecord> create(@RequestBody @jakarta.validation.Valid SiteEntryReportRequest request) {
        authorizationService.requirePermission("facility.site_entry.create_assigned");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhoneForUnauthorized(service.create(request)));
    }

    @PostMapping("/{report_id}/submit")
    public ApiResponse<SiteEntryReportRecord> submit(
            @PathVariable("report_id") String reportId, @RequestBody @jakarta.validation.Valid SiteEntryReportSubmitRequest request) {
        authorizationService.requirePermission("facility.site_entry.submit_assigned");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhoneForUnauthorized(service.submit(reportId, request)));
    }

    @PostMapping("/{report_id}/completion")
    public ApiResponse<SiteEntryReportRecord> uploadCompletion(
            @PathVariable("report_id") String reportId, @RequestBody @jakarta.validation.Valid SiteEntryCompletionUploadRequest request) {
        authorizationService.requirePermission(UPLOAD_COMPLETION_ASSIGNED);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhoneForUnauthorized(service.uploadCompletion(reportId, request)));
    }

    @PostMapping("/{report_id}/review")
    public ApiResponse<SiteEntryReportRecord> review(
            @PathVariable("report_id") String reportId, @RequestBody @jakarta.validation.Valid SiteEntryReportReviewRequest request) {
        authorizationService.requirePermission(REVIEW);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhoneForUnauthorized(service.review(reportId, request)));
    }

    @PostMapping("/{report_id}/accept")
    public ApiResponse<SiteEntryReportRecord> accept(
            @PathVariable("report_id") String reportId, @RequestBody @jakarta.validation.Valid SiteEntryReportAcceptanceRequest request) {
        authorizationService.requirePermission(ACCEPT);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhoneForUnauthorized(service.accept(reportId, request)));
    }

    // ── P0-A/GAP-07 进场报备联系人手机号展示脱敏（V339）。无 pii.contact_phone.view 的读端看掩码；
    // 脱敏放在 controller（service 外），与 V279 内部结算价同款范式。 ──
    private SiteEntryReportRecord maskPhoneForUnauthorized(SiteEntryReportRecord record) {
        if (record == null || authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)) {
            return record;
        }
        return record.withMaskedPhone();
    }

    private PageResponse<SiteEntryReportRecord> maskPhonesForUnauthorized(PageResponse<SiteEntryReportRecord> page) {
        if (authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)) {
            return page;
        }
        return new PageResponse<>(page.items().stream().map(SiteEntryReportRecord::withMaskedPhone).toList(),
                page.page(), page.pageSize(), page.total());
    }

    private void requireReadPermission() {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePermission(authorizationService.isOutsourcedVendor(actor) ? VIEW_ASSIGNED : REVIEW);
    }
}
