package com.complex.platform.facility;

// 文件版本：V0.03

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.PhoneMask;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperationExecutor;
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
    private final com.complex.platform.evidence.EvidenceEventService evidence;
    private final AuditedOperationExecutor operations;

    // 第十八批 N 段第 124 条 124-R3（PGM L738）：写口任一步失败（权限、校验、状态冲突）经 AuditedOperationExecutor.recordingFailure
    // 落 OpenAPI 声明的 x-audit-event-failure 再原样抛出；成功事件照旧由服务层写。
    public SiteEntryReportController(SiteEntryReportService service, AuthorizationService authorizationService,
            com.complex.platform.evidence.EvidenceEventService evidence, AuditedOperationExecutor operations) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.evidence = evidence;
        this.operations = operations;
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
        requireReadPermission(reportId);
        return ApiResponse.ok(maskPhoneForUnauthorized(service.findById(reportId)));
    }

    @GetMapping("/{report_id}/history")
    public ApiResponse<java.util.List<SiteEntryHistoryRecord>> history(@PathVariable("report_id") String reportId) {
        requireReadPermission(reportId);
        return ApiResponse.ok(service.history(reportId));
    }

    @PostMapping(value = "/{report_id}/attachments", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<com.complex.platform.evidence.EvidenceAttachmentRecord> uploadAttachment(
            @PathVariable("report_id") String reportId,
            @org.springframework.web.bind.annotation.RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        authorizationService.requirePermission(UPLOAD_COMPLETION_ASSIGNED);
        service.findById(reportId);
        return ApiResponse.ok(evidence.uploadAttachment(file, "facility_site_entry_report", reportId, "completion"));
    }

    @GetMapping("/{report_id}/attachments/{attachment_id}/file")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> attachmentFile(
            @PathVariable("report_id") String reportId, @PathVariable("attachment_id") String attachmentId) {
        requireReadPermission(reportId);
        service.requireReportAttachment(reportId, attachmentId);
        java.nio.file.Path path = evidence.resolveAttachmentFile(attachmentId);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(evidence.attachmentContentType(attachmentId)))
                .body(new org.springframework.core.io.FileSystemResource(path));
    }

    @PostMapping
    public ApiResponse<SiteEntryReportRecord> create(@RequestBody @jakarta.validation.Valid SiteEntryReportRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.site_entry.create_failed", "facility.site_entry.create_assigned",
                "facility_site_entry_report", "new", key, () -> {
                    authorizationService.requirePermission("facility.site_entry.create_assigned");
                    requireIdempotencyKey(key);
                    return maskPhoneForUnauthorized(service.create(request));
                }));
    }

    @PostMapping("/{report_id}/submit")
    public ApiResponse<SiteEntryReportRecord> submit(
            @PathVariable("report_id") String reportId, @RequestBody @jakarta.validation.Valid SiteEntryReportSubmitRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.site_entry.submit_failed", "facility.site_entry.submit_assigned",
                "facility_site_entry_report", reportId, key, () -> {
                    authorizationService.requirePermission("facility.site_entry.submit_assigned");
                    requireIdempotencyKey(key);
                    return maskPhoneForUnauthorized(service.submit(reportId, request));
                }));
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
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.site_entry.review_failed", "facility.site_entry.review",
                "facility_site_entry_report", reportId, key, () -> {
                    authorizationService.requirePermission(REVIEW);
                    requireIdempotencyKey(key);
                    return maskPhoneForUnauthorized(service.review(reportId, request));
                }));
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
        return record == null || plaintextRows(java.util.List.of(record)).test(record) ? record : record.withMaskedPhone();
    }

    private PageResponse<SiteEntryReportRecord> maskPhonesForUnauthorized(PageResponse<SiteEntryReportRecord> page) {
        var plaintext = plaintextRows(page.items());
        return new PageResponse<>(page.items().stream().map(record -> plaintext.test(record) ? record : record.withMaskedPhone()).toList(),
                page.page(), page.pageSize(), page.total());
    }

    // M723-F06（操作员 2026-10-03：敏感明文「算用掉，看一次扣一次」）：持有明文码、且这次响应里确有可给明文的行，才核验明文码并返回明文
    // ——单次来源在这里消耗，长期来源不产生消耗；否则返回掩码、不碰明文码。一次响应只核一次。
    // 18E（第 19 条 19-R3）（治理地图第 877 行「也不免除项目范围检查」；第 659 行「在所分配的权限内」）：逐行判——有电话且行所属项目被明文码
    // 可触及范围覆盖的行给明文，其余行掩码；外包账号只到本人项目，行为不变。
    private java.util.function.Predicate<SiteEntryReportRecord> plaintextRows(java.util.List<SiteEntryReportRecord> rows) {
        if (rows.stream().noneMatch(record -> record.contactPhone() != null && !record.contactPhone().isBlank())
                || !authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)) {
            return record -> false;
        }
        var reach = authorizationService.projectReach(authorizationService.currentActor(), PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        java.util.function.Predicate<SiteEntryReportRecord> eligible = record -> record.contactPhone() != null
                && !record.contactPhone().isBlank() && reach.covers(record.projectId());
        if (rows.stream().noneMatch(eligible)) {
            return record -> false;
        }
        authorizationService.requirePermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        // 本请求只是返回第一次的结果、新用掉的单次来源会被回滚（不算用掉）时不给明文
        return com.complex.platform.common.idempotency.IdempotentReplayContext.replayDiscardsConsumption() ? record -> false : eligible;
    }

    private void requireReadPermission() {
        requireReadPermission(null);
    }

    private void requireReadPermission(String reportId) {
        AccessActor actor = authorizationService.currentActor();
        String projectId = reportId == null ? actor.organizationId() : service.findById(reportId).projectId();
        authorizationService.requirePermission(authorizationService.isOutsourcedVendor(actor) ? VIEW_ASSIGNED
                : authorizationService.currentActorHasPermission(ACCEPT)
                        && authorizationService.canReachProject(actor, projectId, ACCEPT) ? ACCEPT : REVIEW);
    }
}
