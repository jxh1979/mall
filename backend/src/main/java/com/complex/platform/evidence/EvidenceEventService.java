package com.complex.platform.evidence;

// 文件版本：V0.02

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
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

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
    private final AuditedOperationExecutor executor;

    /** 证据文件本地存储。字段注入免改既有构造点；单测未注入=null（上传端点报存储不可用）。 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    EvidenceFileStorage fileStorage;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.dealer.JdbcDealerRepository dealerRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.dealer.JdbcDealerOrderRepository dealerOrders;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.finance.FinanceRepository dealerFinance;

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
        this.executor = new AuditedOperationExecutor(idempotencyService, auditEventService);
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

    public com.complex.platform.common.api.PageResponse<EvidenceEventRecord> listEvents(int page, int size) {
        int safeSize = size <= 0 ? DEFAULT_LIMIT : Math.min(size, MAX_LIMIT);
        int safePage = page < 1 ? 1 : page;
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        return executor.readPageResult(
                AuditedOperation.of("evidence.event_viewed", "evidence.event_view_failed", VIEW_PERMISSION, "evidence_event"),
                () -> repository.listPage(safePage, safeSize, merchantId));
    }

    /**
     * 真·上传：收 multipart 字节 → 落盘 → 一步写成 {@code uploaded} 态的附件行，返回可直接引用的附件 id。
     *
     * <p>与既有 upload-intent/bind/complete 三段式的关系：那三段是给「客户端直传对象存储」准备的，
     * 但仓里从来没有对象存储实现（{@code upload_url} 是硬编码假串），后台也没有任何上传 UI，
     * 结果就是「高价值报损/盘点差异/存酒作废必须照片+视频双证据」在业务人员侧无法执行、附件 id 全是假串。
     * 本方法是业务人员真正能用的那条路：一次请求传完，服务端认字节、服务端定 content_type。
     *
     * <p>{@code content_type} 由扩展名在服务端判定（不信客户端自报），照片/视频的区分靠它——
     * 否则同一个文件可以被同时当成「照片证据」和「视频证据」交上来。
     */
    @Transactional
    public EvidenceAttachmentRecord uploadAttachment(
            org.springframework.web.multipart.MultipartFile file,
            String businessType,
            String businessId,
            String attachmentRole
    ) {
        if (fileStorage == null) {
            throw ApiException.badRequest("EVIDENCE_FILE_STORAGE_UNAVAILABLE", "证据文件存储不可用");
        }
        String type = requireText(businessType, "business_type");
        String merchantId = evidenceScope(type, businessId, "dealer.manage", "evidence.attachment.create").orElse(null);
        EvidenceFileStorage.StoredFile stored = fileStorage.store(file);
        Instant now = Instant.now();
        String id = "att_" + slug(stored.objectKey());
        EvidenceAttachmentRecord saved = repository.saveAttachment(new EvidenceAttachmentRecord(
                id,
                trimToNull(file.getOriginalFilename()),
                stored.contentType(),
                stored.fileSize(),
                type,
                trimToNull(businessId),
                trimToDefault(attachmentRole, "other"),
                stored.objectKey(),
                // 回读走受控端点（鉴权走既有 API 会话），不开公共静态目录。
                "/api/v1/evidence/attachments/" + id + "/file",
                "uploaded",
                null,
                now,
                now,
                merchantId));
        auditEventService.recordSuccess(
                "evidence.attachment_uploaded",
                "evidence.attachment.create",
                "evidence_attachment",
                saved.id(),
                null,
                null,
                java.util.Map.of("business_type", type, "content_type", stored.contentType(),
                        "file_size", String.valueOf(stored.fileSize())));
        return saved;
    }

    /** 回读一个附件的字节（含归属校验：有商户 scope 时只能读本商户的证据）。 */
    public java.nio.file.Path resolveAttachmentFile(String attachmentId) {
        EvidenceAttachmentRecord attachment = repository.findAttachmentById(requireText(attachmentId, "attachment_id"))
                .orElseThrow(() -> ApiException.notFound("ATTACHMENT_NOT_FOUND", "证据附件不存在"));
        evidenceScope(attachment.businessType(), attachment.businessId(), "dealer.view", "evidence.timeline.view").ifPresent(scope -> {
            if (!scope.equals(attachment.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "证据附件不属于本商户");
            }
        });
        if (fileStorage == null) {
            throw ApiException.badRequest("EVIDENCE_FILE_STORAGE_UNAVAILABLE", "证据文件存储不可用");
        }
        return fileStorage.resolveForRead(attachment.objectKey());
    }

    /** 附件的 content_type（受控 GET 端点回包用）。 */
    public String attachmentContentType(String attachmentId) {
        return repository.findAttachmentById(attachmentId)
                .map(EvidenceAttachmentRecord::contentType)
                .orElse("application/octet-stream");
    }

    @Transactional
    public EvidenceAttachmentRecord createAttachmentUploadIntent(EvidenceAttachmentUploadIntentRequest request) {
        EvidenceAttachmentUploadIntentRequest input = requireRequest(request);
        Optional<String> scope = evidenceScope(input.businessType(), input.businessId(), "dealer.manage", "evidence.attachment.create");
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
                    String merchantId = scope.orElse(null);
                    Instant now = Instant.now();
                    String id = "att_" + (java.util.Set.of("dealer_admission", "dealer_overpayment").contains(businessType)
                            ? UUID.randomUUID().toString().replace("-", "") : slug(actual.idempotencyKey()));
                    // F-CROSS-05 跨租户覆盖修复：object key 必须纳入 merchant scope 前缀，
                    // 否则商户 A/B 用相同 idempotency_key + file_name 会算出同一 object key，互相覆盖证据对象。
                    // 商户用户 → 其 merchantId 命名空间；平台管理员（无 scope）→ _platform 占位（避免字面 "null" 前缀）。
                    String scopeSegment = merchantId == null ? "_platform" : merchantId;
                    String objectKey = "evidence/" + scopeSegment + "/pending/" + id + "/" + slug(fileName);
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
        EvidenceAttachmentBindRequest input = requireRequest(request);
        EvidenceAttachmentRecord source = repository.findAttachmentById(id)
                .orElseThrow(() -> ApiException.notFound("ATTACHMENT_NOT_FOUND", "附件不存在或已被删除"));
        Optional<String> scope = evidenceScope(source.businessType(), source.businessId(), "dealer.manage", "evidence.attachment.create");
        evidenceScope(input.businessType(), input.businessId(), "dealer.manage", "evidence.attachment.create");
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
                            .orElseThrow(() -> ApiException.notFound("ATTACHMENT_NOT_FOUND", "附件不存在或已被删除"));

                    // P1: state machine guard — can only bind a pending attachment
                    if (!"pending".equals(existing.status())) {
                        throw ApiException.conflict("INVALID_STATE",
                                "附件当前状态不可绑定：" + existing.status());
                    }

                    // P0: ownership check — merchant users can only bind their own attachments
                    if (scope.isPresent() && !scope.get().equals(existing.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "附件不属于当前商户");
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
        EvidenceAttachmentRecord source = repository.findAttachmentById(id)
                .orElseThrow(() -> ApiException.notFound("ATTACHMENT_NOT_FOUND", "附件不存在或已被删除"));
        Optional<String> scope = evidenceScope(source.businessType(), source.businessId(), "dealer.manage", "evidence.attachment.create");
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
                            .orElseThrow(() -> ApiException.notFound("ATTACHMENT_NOT_FOUND", "附件不存在或已被删除"));

                    // P1: state machine guard — can only complete a pending or bound attachment
                    if (!"pending".equals(existing.status()) && !"bound".equals(existing.status())) {
                        throw ApiException.conflict("INVALID_STATE",
                                "附件当前状态不可完成上传：" + existing.status());
                    }

                    // P0: ownership check — merchant users can only complete their own attachments
                    if (scope.isPresent() && !scope.get().equals(existing.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "附件不属于当前商户");
                    }

                    return repository.saveAttachment(new EvidenceAttachmentRecord(
                            existing.id(),
                            existing.fileName(),
                            existing.contentType(),
                            actual.fileSize() == null ? existing.fileSize() : actual.fileSize(),
                            existing.businessType(),
                            existing.businessId(),
                            existing.attachmentRole(),
                            // F-CROSS-05 跨租户覆盖修复（complete 阶段）：忽略客户端提交的 object_key，
                            // 用 upload-intent 已签发的原 key（existing.objectKey()，已含 merchant scope 前缀），
                            // 防客户端对自己的 attachment 提交他商户命名空间 key、越权指向/覆盖他商户存储对象。
                            existing.objectKey(),
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
        EvidenceReviewRequest input = requireRequest(request);
        Optional<String> scope = evidenceScope(input.businessType(), input.businessId(), "dealer.review", "evidence.review.create");
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
                    String merchantId = scope.orElse(null);
                    Instant now = Instant.now();

                    // P1: validate each attachment ID exists, belongs to the correct business object,
                    //     and has been fully uploaded
                    List<String> attachmentIds = actual.evidenceAttachmentIds() == null
                            ? List.of()
                            : List.copyOf(actual.evidenceAttachmentIds());
                    for (String attachmentId : attachmentIds) {
                        EvidenceAttachmentRecord att = repository.findAttachmentById(attachmentId)
                                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED",
                                        "附件不存在：" + attachmentId));
                        // P1 越权修复：评审引用附件此前不校验附件归属（bindAttachment/completeAttachment 均有），
                        // 可把他商户 attachment_id 写入自己 review，跨租户引用/泄露证据。
                        Optional<String> attScope = scope;
                        if (attScope.isPresent() && !attScope.get().equals(att.merchantId())) {
                            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                    "附件属于其他商户：" + attachmentId);
                        }
                        if (!reqBusinessType.equals(att.businessType()) || !reqBusinessId.equals(att.businessId())) {
                            throw ApiException.badRequest("VALIDATION_FAILED",
                                    "附件不属于指定的业务对象：" + attachmentId);
                        }
                        if (!"uploaded".equals(att.status())) {
                            throw ApiException.badRequest("VALIDATION_FAILED",
                                    "附件不在已上传状态：" + attachmentId);
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
        try {
            String merchantId = evidenceScope(type, id, "dealer.view", "evidence.timeline.view").orElse(null);
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

    private Optional<String> evidenceScope(String businessType, String businessId, String dealerPermission, String evidencePermission) {
        String type = trimToNull(businessType);
        if ("finance_refund".equals(type) && dealerFinance != null) {
            var refund = dealerFinance.findRefundById(requireText(businessId, "business_id"), false).orElse(null);
            if (refund != null && "dealer_order_refund".equals(refund.sourceType())) {
                String permission = "evidence.timeline.view".equals(evidencePermission) ? "finance.ar_ap.view" : "finance.refund.create";
                authorizationService.requirePermissions(permission, evidencePermission);
                if (!authorizationService.canReachProject(authorizationService.currentActor(), refund.projectId(), permission, evidencePermission)) {
                    throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "经销退款凭证不在授权项目范围内");
                }
                return authorizationService.currentMerchantIdQuietly();
            }
        }
        if (!java.util.Set.of("dealer_admission", "dealer_overpayment").contains(type == null ? "" : type)) {
            return authorizationService.currentMerchantScope();
        }
        String dealerId = requireText(businessId, "business_id");
        if ("dealer_overpayment".equals(type)) {
            if (dealerOrders == null) throw ApiException.badRequest("DEALER_EVIDENCE_UNAVAILABLE", "经销资金资料范围暂不可核验");
            var overpayment = dealerOrders.findOverpayment(dealerId)
                    .orElseThrow(() -> ApiException.notFound("OVERPAYMENT_NOT_FOUND", "超收记录不存在"));
            String projectId = dealerOrders.findOrderProjectId(overpayment.orderId())
                    .orElseThrow(() -> ApiException.conflict("DEALER_ORDER_PROJECT_MISSING", "原收款订单的中心仓项目缺失或冲突"));
            dealerPermission = "evidence.timeline.view".equals(evidencePermission) ? "dealer.order.view" : "dealer.order.payment.verify";
            authorizationService.requirePermissions(dealerPermission, evidencePermission);
            if (!authorizationService.canReachProject(authorizationService.currentActor(), projectId, dealerPermission, evidencePermission)) {
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "超收款凭证不在授权项目范围内");
            }
            return authorizationService.currentMerchantIdQuietly();
        }
        authorizationService.requirePermissions(dealerPermission, evidencePermission);
        if (dealerRepository == null) throw ApiException.badRequest("DEALER_EVIDENCE_UNAVAILABLE", "经销资料范围暂不可核验");
        var dealer = dealerRepository.findDealer(dealerId)
                .orElseThrow(() -> ApiException.notFound("DEALER_NOT_FOUND", "经销商不存在"));
        if (!authorizationService.canReachProject(authorizationService.currentActor(), dealer.projectId(), dealerPermission, evidencePermission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "经销资料不在授权项目范围内");
        }
        return authorizationService.currentMerchantIdQuietly();
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
                    "校验和格式不正确，应为 sha256:<64位十六进制> 形式");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
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
