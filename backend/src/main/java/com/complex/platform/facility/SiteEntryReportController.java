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
    // M723-F06：明文联系电话看一次扣一次（操作员 2026-10-03）；业务读权限照旧不扣
    @com.complex.platform.access.ConsumesSingleUseGrant(PhoneMask.VIEW_PLAINTEXT_PERMISSION)
    public ApiResponse<PageResponse<SiteEntryReportRecord>> list() {
        requireReadPermission();
        return ApiResponse.ok(maskPhonesForUnauthorized(service.list()));
    }

    @GetMapping("/{report_id}")
    // M723-F06：明文联系电话看一次扣一次（操作员 2026-10-03）；业务读权限照旧不扣
    @com.complex.platform.access.ConsumesSingleUseGrant(PhoneMask.VIEW_PLAINTEXT_PERMISSION)
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
        if (record == null || viewPhonePlaintext(java.util.stream.Stream.of(record.contactPhone()))) {
            return record;
        }
        return record.withMaskedPhone();
    }

    private PageResponse<SiteEntryReportRecord> maskPhonesForUnauthorized(PageResponse<SiteEntryReportRecord> page) {
        if (viewPhonePlaintext(page.items().stream().map(SiteEntryReportRecord::contactPhone))) {
            return page;
        }
        return new PageResponse<>(page.items().stream().map(SiteEntryReportRecord::withMaskedPhone).toList(),
                page.page(), page.pageSize(), page.total());
    }

    // M723-F06（操作员 2026-10-03：敏感明文「算用掉，看一次扣一次」）：持有明文码、且这次响应里确有非空电话，才核验明文码并返回明文
    // ——单次来源在这里消耗，长期来源不产生消耗；否则返回掩码、不碰明文码。一次响应只核一次（列表按整页判断）。
    private boolean viewPhonePlaintext(java.util.stream.Stream<String> phones) {
        if (phones.noneMatch(phone -> phone != null && !phone.isBlank())
                || !authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)) {
            return false;
        }
        authorizationService.requirePermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        // 本请求只是返回第一次的结果、新用掉的单次来源会被回滚（不算用掉）时不给明文
        return !com.complex.platform.common.idempotency.IdempotentReplayContext.replayDiscardsConsumption();
    }

    private void requireReadPermission() {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePermission(authorizationService.isOutsourcedVendor(actor) ? VIEW_ASSIGNED : REVIEW);
    }
}
