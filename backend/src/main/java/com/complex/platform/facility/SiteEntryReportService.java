package com.complex.platform.facility;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.SiteEntryCompletionUploadRequest;
import com.complex.platform.facility.dto.SiteEntryReportAcceptanceRequest;
import com.complex.platform.facility.dto.SiteEntryReportRequest;
import com.complex.platform.facility.dto.SiteEntryReportReviewRequest;
import com.complex.platform.facility.dto.SiteEntryReportSubmitRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import com.complex.platform.common.Strings;
import static com.complex.platform.common.Strings.requireText;

@Service
public class SiteEntryReportService {

    static final Set<String> STATUSES = Set.of("submitted", "approved", "rejected", "in_progress", "accepted", "rectifying");
    private static final String BUSINESS_TYPE = "facility_site_entry_report";
    private static final String CREATE_PERM = "facility.site_entry.create_assigned";
    private static final String SUBMIT_PERM = "facility.site_entry.submit_assigned";
    private static final String UPLOAD_COMPLETION_PERM = "facility.site_entry.upload_completion_assigned";
    private static final String REVIEW_PERM = "facility.site_entry.review";
    private static final String ACCEPT_PERM = "facility.site_entry.accept";

    private final SiteEntryReportRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public SiteEntryReportService(SiteEntryReportRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<SiteEntryReportRecord> list() {
        AccessActor actor = requireScopeActor();
        List<SiteEntryReportRecord> items = authorizationService.isOutsourcedVendor(actor)
                ? repository.listAssignedTo(actor.id(), authorizationService.requireProjectScope(actor))
                : authorizationService.isPlatformAdmin(actor)
                        ? repository.list()
                        : repository.listByProject(authorizationService.requireProjectScope(actor));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public SiteEntryReportRecord findById(String id) {
        AccessActor actor = requireScopeActor();
        SiteEntryReportRecord record = load(id);
        enforceRecordScope(actor, record);
        return record;
    }

    @Transactional
    public SiteEntryReportRecord create(SiteEntryReportRequest request) {
        SiteEntryReportRequest r = requireCreate(request);
        AccessActor actor = authorizationService.currentActor();
        if (!authorizationService.isOutsourcedVendor(actor)) {
            throw ApiException.forbidden("FORBIDDEN", "只有外包供应商才能创建进场报备");
        }
        String projectId = requireText(r.projectId(), "project_id");
        authorizationService.enforceOutsourcedAssigneeScope(actor, actor.id(), projectId);
        LocalDate plannedStart = requireDate(r.plannedStartDate(), "planned_start_date");
        LocalDate plannedEnd = requireDate(r.plannedEndDate(), "planned_end_date");
        if (plannedEnd.isBefore(plannedStart)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "计划结束日期不能早于计划开始日期");
        }
        int crewCount = positive(r.crewCount(), "crew_count");
        Instant now = Instant.now();
        SiteEntryReportRecord rec = new SiteEntryReportRecord("ser_" + UUID.randomUUID(), projectId,
                trimToNull(r.spaceId()), requireText(r.workScope(), "work_scope"), plannedStart, plannedEnd,
                crewCount, requireText(r.contactName(), "contact_name"), requireText(r.contactPhone(), "contact_phone"),
                actor.id(), "submitted", null, null, actor.id(), now, now, List.of(), null, null, null);
        repository.insert(rec);
        audit("facility.site_entry_report_created", CREATE_PERM, rec.id(), r.idempotencyKey(),
                Map.of("status", rec.status(), "project_id", rec.projectId()));
        return rec;
    }

    @Transactional
    public SiteEntryReportRecord submit(String id, SiteEntryReportSubmitRequest request) {
        SiteEntryReportSubmitRequest r = requireSubmit(request);
        AccessActor actor = authorizationService.currentActor();
        if (!authorizationService.isOutsourcedVendor(actor)) {
            throw ApiException.forbidden("FORBIDDEN", "只有外包供应商才能提交进场报备");
        }
        SiteEntryReportRecord cur = load(id);
        enforceRecordScope(actor, cur);
        requireStatus(cur, "approved", "只有审批通过的进场报备才能提交进场");
        SiteEntryReportRecord inProgress = withStatus(cur, "in_progress", null, null, Instant.now());
        repository.update(inProgress);
        audit("facility.site_entry_report_submitted", SUBMIT_PERM, inProgress.id(), r.idempotencyKey(),
                metadata(inProgress, r.note()));
        return inProgress;
    }

    @Transactional
    public SiteEntryReportRecord uploadCompletion(String id, SiteEntryCompletionUploadRequest request) {
        SiteEntryCompletionUploadRequest r = requireUpload(request);
        AccessActor actor = authorizationService.currentActor();
        if (!authorizationService.isOutsourcedVendor(actor)) {
            throw ApiException.forbidden("FORBIDDEN", "只有外包供应商才能上传完工材料");
        }
        SiteEntryReportRecord cur = load(id);
        enforceRecordScope(actor, cur);
        if (!Set.of("in_progress", "rectifying").contains(cur.status())) {
            throw ApiException.badRequest("INVALID_STATE", "只有验收前才能上传完工材料");
        }
        List<String> attachmentIds = strings(r.attachmentIds(), "attachment_ids");
        SiteEntryReportRecord uploaded = new SiteEntryReportRecord(cur.id(), cur.projectId(), cur.spaceId(),
                cur.workScope(), cur.plannedStartDate(), cur.plannedEndDate(), cur.crewCount(), cur.contactName(),
                cur.contactPhone(), cur.vendorUserId(), cur.status(), cur.reviewComment(), cur.acceptanceComment(),
                cur.createdBy(), cur.createdAt(), Instant.now(), attachmentIds, trimToNull(r.note()),
                Instant.now(), cur.deletedAt());
        repository.update(uploaded);
        audit("facility.site_entry_completion_uploaded", UPLOAD_COMPLETION_PERM, uploaded.id(), r.idempotencyKey(),
                Map.of("status", uploaded.status(), "attachment_count", attachmentIds.size()));
        return uploaded;
    }

    @Transactional
    public SiteEntryReportRecord review(String id, SiteEntryReportReviewRequest request) {
        AccessActor actor = requirePropertyActor();
        SiteEntryReportReviewRequest r = requireReview(request);
        String decision = normalizeAllowed(r.decision(), "decision", Set.of("approve", "reject"));
        SiteEntryReportRecord cur = load(id);
        enforceRecordScope(actor, cur);
        requireStatus(cur, "submitted", "只有已提交的进场报备才能审核");
        String nextStatus = "approve".equals(decision) ? "approved" : "rejected";
        SiteEntryReportRecord reviewed = withStatus(cur, nextStatus, trimToNull(r.reviewComment()),
                cur.acceptanceComment(), Instant.now());
        repository.update(reviewed);
        audit("facility.site_entry_report_reviewed", REVIEW_PERM, reviewed.id(), r.idempotencyKey(),
                Map.of("status", reviewed.status()));
        return reviewed;
    }

    @Transactional
    public SiteEntryReportRecord accept(String id, SiteEntryReportAcceptanceRequest request) {
        AccessActor actor = requirePropertyActor();
        SiteEntryReportAcceptanceRequest r = requireAcceptance(request);
        String result = normalizeAllowed(r.result(), "result", Set.of("pass", "rectify"));
        SiteEntryReportRecord cur = load(id);
        enforceRecordScope(actor, cur);
        requireStatus(cur, "in_progress", "只有进行中的进场报备才能验收");
        String nextStatus = "pass".equals(result) ? "accepted" : "rectifying";
        SiteEntryReportRecord accepted = withStatus(cur, nextStatus, cur.reviewComment(),
                trimToNull(r.acceptanceComment()), Instant.now());
        repository.update(accepted);
        audit("facility.site_entry_report_accepted", ACCEPT_PERM, accepted.id(), r.idempotencyKey(),
                Map.of("status", accepted.status()));
        return accepted;
    }

    private AccessActor requireScopeActor() {
        AccessActor actor = authorizationService.currentActor();
        if (!authorizationService.isOutsourcedVendor(actor)) {
            authorizationService.requirePlatformScope();
        }
        return actor;
    }

    private AccessActor requirePropertyActor() {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePlatformScope();
        return actor;
    }

    private void enforceRecordScope(AccessActor actor, SiteEntryReportRecord record) {
        authorizationService.enforceOutsourcedAssigneeScope(actor, record.vendorUserId(), record.projectId());
        if (authorizationService.isOutsourcedVendor(actor) || authorizationService.isPlatformAdmin(actor)) {
            return;
        }
        String scopeProjectId = authorizationService.requireProjectScope(actor);
        if (!scopeProjectId.equals(record.projectId())) {
            throw ApiException.forbidden("FORBIDDEN", "进场报备不在当前项目范围内");
        }
    }

    private SiteEntryReportRecord load(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "进场报备不存在或已被删除"));
    }

    private SiteEntryReportRecord withStatus(SiteEntryReportRecord cur, String status, String reviewComment,
            String acceptanceComment, Instant updatedAt) {
        return new SiteEntryReportRecord(cur.id(), cur.projectId(), cur.spaceId(), cur.workScope(),
                cur.plannedStartDate(), cur.plannedEndDate(), cur.crewCount(), cur.contactName(), cur.contactPhone(),
                cur.vendorUserId(), status, reviewComment, acceptanceComment, cur.createdBy(), cur.createdAt(),
                updatedAt, cur.completionAttachmentIds(), cur.completionNote(), cur.completionUploadedAt(),
                cur.deletedAt());
    }

    private SiteEntryReportRequest requireCreate(SiteEntryReportRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private SiteEntryReportSubmitRequest requireSubmit(SiteEntryReportSubmitRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private SiteEntryCompletionUploadRequest requireUpload(SiteEntryCompletionUploadRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private SiteEntryReportReviewRequest requireReview(SiteEntryReportReviewRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private SiteEntryReportAcceptanceRequest requireAcceptance(SiteEntryReportAcceptanceRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private void requireStatus(SiteEntryReportRecord cur, String expected, String message) {
        if (!expected.equals(cur.status())) {
            throw ApiException.badRequest("INVALID_STATE", message);
        }
    }

    private String normalizeAllowed(String value, String field, Set<String> allowed) {
        String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return normalized;
    }

    private LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value;
    }

    private int positive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于 0");
        }
        return value;
    }

    private List<String> strings(List<String> values, String field) {
        if (values == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        List<String> cleaned = values.stream()
                .map(Strings::trimToNull)
                .filter(value -> value != null)
                .toList();
        if (cleaned.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return cleaned;
    }

    private Map<String, Object> metadata(SiteEntryReportRecord record, String note) {
        String trimmed = trimToNull(note);
        return trimmed == null ? Map.of("status", record.status()) : Map.of("status", record.status(), "note", trimmed);
    }

    private void audit(String event, String permission, String id, String idempotencyKey, Map<String, Object> metadata) {
        auditEventService.recordSuccess(event, permission, BUSINESS_TYPE, id, null, idempotencyKey, metadata);
    }
}
