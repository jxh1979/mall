package com.complex.platform.evidence;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.dto.EvidenceAttachmentBindRequest;
import com.complex.platform.evidence.dto.EvidenceAttachmentCompleteRequest;
import com.complex.platform.evidence.dto.EvidenceAttachmentUploadIntentRequest;
import com.complex.platform.evidence.dto.EvidenceReviewRequest;
import com.complex.platform.evidence.dto.EventBatchRequest;
import com.complex.platform.evidence.dto.TerminalEventItemRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceEventService {

    private static final String CREATE_PERMISSION = "evidence.event.create";
    private static final String VIEW_PERMISSION = "evidence.event.view";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    private final EvidenceEventRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public EvidenceEventService(
            EvidenceEventRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public EventBatchResponse createEvents(EventBatchRequest request) {
        return executeWrite(
                "createEvidenceEvents",
                request == null ? null : request.idempotencyKey(),
                request,
                EventBatchResponse.class,
                () -> create(request)
        );
    }

    public List<EvidenceEventRecord> listEvents(int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        try {
            List<EvidenceEventRecord> events = repository.listLatest(safeLimit, merchantId);
            auditEventService.recordSuccess(
                    "evidence.event_viewed",
                    VIEW_PERMISSION,
                    "evidence_event",
                    "all",
                    null,
                    null,
                    Map.of("count", events.size())
            );
            return events;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "evidence.event_view_failed",
                    VIEW_PERMISSION,
                    "evidence_event",
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @Transactional
    public EvidenceAttachmentRecord createAttachmentUploadIntent(EvidenceAttachmentUploadIntentRequest request) {
        return executeEvidenceWrite(
                "createEvidenceAttachmentUploadIntent",
                request == null ? null : request.idempotencyKey(),
                request,
                EvidenceAttachmentRecord.class,
                "evidence.upload_intent_created",
                "evidence.upload_intent_failed",
                "evidence.attachment.create",
                EvidenceAttachmentRecord::businessType,
                EvidenceAttachmentRecord::businessId,
                () -> {
                    EvidenceAttachmentUploadIntentRequest actual = requireRequest(request);
                    String fileName = requireText(actual.fileName(), "file_name");
                    String contentType = requireText(actual.contentType(), "content_type");
                    String businessType = requireText(actual.businessType(), "business_type");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    String id = "att_" + slug(actual.idempotencyKey());
                    String objectKey = "evidence/pending/" + id + "/" + slug(fileName);
                    return repository.saveAttachment(new EvidenceAttachmentRecord(
                            id,
                            fileName,
                            contentType,
                            actual.fileSize(),
                            businessType,
                            trimToNull(actual.businessId()),
                            null,
                            objectKey,
                            "https://object-storage.local/upload/" + id,
                            "pending",
                            null,
                            now,
                            now,
                            merchantId
                    ));
                }
        );
    }

    @Transactional
    public EvidenceAttachmentRecord bindAttachment(String attachmentId, EvidenceAttachmentBindRequest request) {
        String id = requireText(attachmentId, "attachment_id");
        return executeEvidenceWrite(
                "bindEvidenceAttachment:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                EvidenceAttachmentRecord.class,
                "evidence.attachment_bound",
                "evidence.attachment_bind_failed",
                "evidence.attachment.create",
                EvidenceAttachmentRecord::businessType,
                EvidenceAttachmentRecord::businessId,
                () -> {
                    EvidenceAttachmentBindRequest actual = requireRequest(request);
                    EvidenceAttachmentRecord existing = repository.findAttachmentById(id)
                            .orElseThrow(() -> ApiException.notFound("ATTACHMENT_NOT_FOUND", "attachment not found"));

                    // P1: state machine guard — can only bind a pending attachment
                    if (!"pending".equals(existing.status())) {
                        throw ApiException.conflict("INVALID_STATE",
                                "attachment cannot be bound in state: " + existing.status());
                    }

                    // P0: ownership check — merchant users can only bind their own attachments
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(existing.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "attachment does not belong to current merchant");
                    }

                    return repository.saveAttachment(new EvidenceAttachmentRecord(
                            existing.id(),
                            existing.fileName(),
                            existing.contentType(),
                            existing.fileSize(),
                            requireText(actual.businessType(), "business_type"),
                            requireText(actual.businessId(), "business_id"),
                            trimToDefault(actual.attachmentRole(), "other"),
                            existing.objectKey(),
                            existing.uploadUrl(),
                            "bound",
                            existing.checksum(),
                            existing.createdAt(),
                            Instant.now(),
                            existing.merchantId()
                    ));
                }
        );
    }

    @Transactional
    public EvidenceAttachmentRecord completeAttachment(String attachmentId, EvidenceAttachmentCompleteRequest request) {
        String id = requireText(attachmentId, "attachment_id");
        return executeEvidenceWrite(
                "completeEvidenceAttachmentUpload:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                EvidenceAttachmentRecord.class,
                "evidence.attachment_completed",
                "evidence.attachment_complete_failed",
                "evidence.attachment.create",
                EvidenceAttachmentRecord::businessType,
                EvidenceAttachmentRecord::businessId,
                () -> {
                    EvidenceAttachmentCompleteRequest actual = requireRequest(request);
                    EvidenceAttachmentRecord existing = repository.findAttachmentById(id)
                            .orElseThrow(() -> ApiException.notFound("ATTACHMENT_NOT_FOUND", "attachment not found"));

                    // P1: state machine guard — can only complete a pending or bound attachment
                    if (!"pending".equals(existing.status()) && !"bound".equals(existing.status())) {
                        throw ApiException.conflict("INVALID_STATE",
                                "attachment cannot be completed in state: " + existing.status());
                    }

                    // P0: ownership check — merchant users can only complete their own attachments
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(existing.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "attachment does not belong to current merchant");
                    }

                    return repository.saveAttachment(new EvidenceAttachmentRecord(
                            existing.id(),
                            existing.fileName(),
                            existing.contentType(),
                            actual.fileSize() == null ? existing.fileSize() : actual.fileSize(),
                            existing.businessType(),
                            existing.businessId(),
                            existing.attachmentRole(),
                            requireText(actual.objectKey(), "object_key"),
                            existing.uploadUrl(),
                            "uploaded",
                            requireChecksum(actual.checksum()),
                            existing.createdAt(),
                            Instant.now(),
                            existing.merchantId()
                    ));
                }
        );
    }

    @Transactional
    public EvidenceReviewRecord createEvidenceReview(EvidenceReviewRequest request) {
        return executeEvidenceWrite(
                "createEvidenceReview",
                request == null ? null : request.idempotencyKey(),
                request,
                EvidenceReviewRecord.class,
                "evidence.review_created",
                "evidence.review_create_failed",
                "evidence.review.create",
                EvidenceReviewRecord::businessType,
                EvidenceReviewRecord::businessId,
                () -> {
                    EvidenceReviewRequest actual = requireRequest(request);
                    String reqBusinessType = requireText(actual.businessType(), "business_type");
                    String reqBusinessId = requireText(actual.businessId(), "business_id");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();

                    // P1: validate each attachment ID exists, belongs to the correct business object,
                    //     and has been fully uploaded
                    List<String> attachmentIds = actual.evidenceAttachmentIds() == null
                            ? List.of()
                            : List.copyOf(actual.evidenceAttachmentIds());
                    for (String attachmentId : attachmentIds) {
                        EvidenceAttachmentRecord att = repository.findAttachmentById(attachmentId)
                                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED",
                                        "attachment not found: " + attachmentId));
                        if (!reqBusinessType.equals(att.businessType()) || !reqBusinessId.equals(att.businessId())) {
                            throw ApiException.badRequest("VALIDATION_FAILED",
                                    "attachment does not belong to the specified business object: " + attachmentId);
                        }
                        if (!"uploaded".equals(att.status())) {
                            throw ApiException.badRequest("VALIDATION_FAILED",
                                    "attachment is not in uploaded state: " + attachmentId);
                        }
                    }

                    return repository.saveReview(new EvidenceReviewRecord(
                            "rev_" + slug(actual.idempotencyKey()),
                            reqBusinessType,
                            reqBusinessId,
                            "medium",
                            requireText(actual.reviewResult(), "review_result"),
                            trimToNull(actual.comment()),
                            attachmentIds,
                            "created",
                            now,
                            now,
                            merchantId
                    ));
                }
        );
    }

    public List<EvidenceTimelineItem> getTimeline(String objectType, String objectId) {
        String type = requireText(objectType, "object_type");
        String id = requireText(objectId, "object_id");
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        try {
            List<EvidenceTimelineItem> timeline = repository.listTimeline(type, id, merchantId);
            auditEventService.recordSuccess(
                    "evidence.timeline_viewed",
                    "evidence.timeline.view",
                    type,
                    id,
                    null,
                    null,
                    Map.of("count", timeline.size())
            );
            return timeline;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "evidence.timeline_view_failed",
                    "evidence.timeline.view",
                    type,
                    id,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private EventBatchResponse create(EventBatchRequest request) {
        EventBatchRequest actual = requireRequest(request);
        String deviceCode = requireText(actual.deviceCode(), "device_code");
        List<TerminalEventItemRequest> events = actual.events() == null ? List.of() : actual.events();
        if (events.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "events is required");
        }
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant receivedAt = Instant.now();
        List<EvidenceEventRecord> records = events.stream()
                .map(event -> toRecord(deviceCode, trimToNull(actual.staffId()), event, receivedAt, merchantId))
                .toList();
        repository.saveAll(records);
        return new EventBatchResponse(records.size(), receivedAt);
    }

    private EvidenceEventRecord toRecord(
            String deviceCode,
            String staffId,
            TerminalEventItemRequest event,
            Instant receivedAt,
            String merchantId
    ) {
        if (event == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "event is required");
        }
        return new EvidenceEventRecord(
                "evt_" + UUID.randomUUID().toString().replace("-", ""),
                deviceCode,
                staffId,
                requireText(event.eventType(), "event_type"),
                trimToDefault(event.eventCategory(), "android_terminal"),
                trimToDefault(event.result(), "success"),
                requireText(event.summary(), "summary"),
                event.payload() == null ? Map.of() : Map.copyOf(event.payload()),
                requireInstant(event.occurredAt(), "occurred_at"),
                receivedAt,
                merchantId
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            Supplier<T> action
    ) {
        // P0: scope idempotency key to the current actor to prevent cross-user cache sharing
        String actorId = authorizationService.currentActor().id();
        String scopedOperationId = actorId + ":" + operationId;
        try {
            return idempotencyService.execute(
                    scopedOperationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T response = action.get();
                        auditEventService.recordSuccess(
                                "evidence.events_reported",
                                CREATE_PERMISSION,
                                "evidence_event",
                                businessId(request),
                                null,
                                idempotencyKey,
                                metadata(response)
                        );
                        return response;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "evidence.events_report_failed",
                    CREATE_PERMISSION,
                    "evidence_event",
                    null,
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> T executeEvidenceWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            Function<T, String> businessType,
            Function<T, String> businessId,
            Supplier<T> action
    ) {
        // P0: scope idempotency key to the current actor to prevent cross-user cache sharing
        String actorId = authorizationService.currentActor().id();
        String scopedOperationId = actorId + ":" + operationId;
        try {
            return idempotencyService.execute(
                    scopedOperationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T response = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType.apply(response),
                                businessId.apply(response),
                                null,
                                idempotencyKey,
                                Map.of()
                        );
                        return response;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    "evidence",
                    null,
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private Map<String, Object> metadata(Object response) {
        if (response instanceof EventBatchResponse batchResponse) {
            return Map.of("accepted", batchResponse.accepted());
        }
        return Map.of();
    }

    private String businessId(Object request) {
        if (request instanceof EventBatchRequest eventBatchRequest) {
            return trimToNull(eventBatchRequest.deviceCode());
        }
        return null;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private static final Pattern SHA256_CHECKSUM = Pattern.compile("^sha256:[0-9a-f]{64}$");

    /**
     * The completed attachment checksum is the tamper-evidence digest stored against the object. There is
     * no object store in this slice to recompute the hash from bytes, but the digest must at least be a
     * well-formed, normalized SHA-256 value so a structurally-invalid/garbage token cannot be stored and
     * later trusted as integrity proof. Combined with the state machine (an uploaded attachment cannot be
     * re-completed), the stored checksum is well-formed, immutable and audited.
     */
    private String requireChecksum(String value) {
        String normalized = requireText(value, "checksum").toLowerCase(Locale.ROOT);
        if (!SHA256_CHECKSUM.matcher(normalized).matches()) {
            throw ApiException.badRequest(
                    "EVIDENCE_CHECKSUM_INVALID",
                    "checksum must be a sha256 digest of the form 'sha256:<64 hex chars>'");
        }
        return normalized;
    }

    private Instant requireInstant(Instant value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String resolved = slug.isEmpty() ? UUID.randomUUID().toString().replace("-", "") : slug;
        return resolved.length() > 56 ? resolved.substring(0, 56) : resolved;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
