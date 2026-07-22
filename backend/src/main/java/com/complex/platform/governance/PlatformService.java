package com.complex.platform.governance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.BusinessEntityRequest;
import com.complex.platform.governance.dto.MerchantProfileRequest;
import com.complex.platform.governance.dto.MerchantDeleteApprovalRequest;
import com.complex.platform.governance.dto.MerchantDeleteRequest;
import com.complex.platform.governance.dto.MerchantArchiveRequest;
import com.complex.platform.governance.dto.ProjectRequest;
import com.complex.platform.governance.dto.SpaceRequest;
import com.complex.platform.governance.dto.StatusChangeRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class PlatformService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    // AUD-005: 列表端点默认页大小(保留既有一次拉取上限, 供下拉/客户端分页); total 已取真值。
    private static final int LIST_MAX_PAGE_SIZE = 500;

    private final PlatformRepository repository;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    // F-AUTHZ-02 治理写仅平台：治理写方法（建商户/法律实体绑定/合同/项目空间等）首行经
    // requirePlatformScope() fail-closed 拒 merchant-scoped 主体，防权限被下放给商户角色后越权治理。
    private final AuthorizationService authorizationService;
    // AUD-076 撤场提前到期：商户存档=撤场；被存档商户为酒吧时其单店票已售权益提前到期（走方案2）。可为 null=兼容既有测试。
    private final com.complex.platform.ticketing.TicketingService ticketingService;

    @org.springframework.beans.factory.annotation.Autowired
    public PlatformService(
            PlatformRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.ticketing.TicketingService ticketingService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.ticketingService = ticketingService;
    }

    // ponytail: 向后兼容构造——既有单测不接 ticketingService（不触发撤场提前到期）。
    public PlatformService(
            PlatformRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this(repository, operations, auditEventService, authorizationService, null);
    }

    public PageResponse<BusinessEntityRecord> listBusinessEntities() {
        return listBusinessEntities(DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, total 取真值; 默认 500 保留既有前端一次拉取(下拉源)契约。
    public PageResponse<BusinessEntityRecord> listBusinessEntities(int page, int size) {
        PageResponse<BusinessEntityRecord> result = repository.listBusinessEntitiesPaged(page, size);
        auditEventService.recordSuccess(
                "entity_binding.viewed",
                "entity.binding.view",
                "business_entity",
                "all",
                null,
                null,
                Map.of("count", result.items().size(), "total", result.total())
        );
        return result;
    }

    public BusinessEntityRecord createBusinessEntity(BusinessEntityRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createBusinessEntity",
                idempotencyKey(request),
                request,
                BusinessEntityRecord.class,
                "business_entity.changed",
                "business_entity.change_failed",
                "entity.binding.manage",
                "business_entity",
                () -> {
                    String entityName = requireText(request == null ? null : request.entityName(), "entity_name");
                    String entityRole = requireText(request == null ? null : request.entityRole(), "entity_role");
                    Instant now = Instant.now();
                    return repository.saveBusinessEntity(new BusinessEntityRecord(
                            "be_" + UUID.randomUUID(),
                            entityName,
                            entityRole,
                            trimToNull(request.parentEntityId()),
                            trimToNull(request.unifiedSocialCreditCode()),
                            trimToNull(request.legalRepresentative()),
                            trimToNull(request.bankAccountRef()),
                            trimToDefault(request.status(), "active"),
                            now,
                            now
                    ));
                }
        );
    }

    public PageResponse<MerchantRecord> listMerchants(boolean includeArchived) {
        return listMerchants(includeArchived, DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    public PageResponse<MerchantRecord> listMerchants(boolean includeArchived, int page, int size) {
        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(includeArchived, page, size);
        auditEventService.recordSuccess(
                "merchant.viewed",
                "merchant.profile.view",
                "merchant",
                "all",
                null,
                null,
                Map.of("count", result.items().size(), "total", result.total())
        );
        return result;
    }

    public MerchantRecord createMerchant(MerchantProfileRequest request) {
        authorizationService.requirePlatformScope();
        return saveMerchant(
                "createMerchant",
                "mch_" + UUID.randomUUID(),
                request
        );
    }

    public MerchantRecord updateMerchant(String merchantId, MerchantProfileRequest request) {
        authorizationService.requirePlatformScope();
        return saveMerchant(
                "updateMerchant:" + merchantId,
                requireText(merchantId, "merchant_id"),
                request
        );
    }

    // ================= V260 商户存档与审批后删除范式 =================

    /** 当前操作者 id（存档/删除申请留痕与自审校验）；无鉴权上下文（测试/内部）回退 null。 */
    private String actorId() {
        if (authorizationService == null) {
            return null;
        }
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

    /** 存档/取消存档商户（软标记，可逆；列表默认过滤已存档）。取消存档时把 pending 删除申请回退 none。 */
    public MerchantRecord archiveMerchant(String merchantId, MerchantArchiveRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(merchantId, "merchant_id");
        return executeWrite(
                "archiveMerchant:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantRecord.class,
                "merchant.archived",
                "merchant.archive_failed",
                "merchant.profile.manage",
                "merchant",
                () -> {
                    String r = requireText(request == null ? null : request.reason(), "reason");
                    MerchantRecord merchant = requireMerchant(id);
                    Instant now = Instant.now();
                    MerchantRecord saved = repository.saveMerchant(merchant.withArchive(now, actorId(), now));
                    repository.setMerchantArchiveReason(id, r);
                    // AUD-076 撤场：被存档商户为酒吧时，其单店票停售（下单侧 TICKET_BAR_OFFLINE）+
                    // 已售权益提前到该酒吧截止营业时刻到期（走方案2：可退未用自动退/不可退作废）。
                    if (ticketingService != null && "bar".equals(merchant.merchantType())) {
                        ticketingService.expireBarSingleStoreTickets(id, now);
                    }
                    return saved;
                }
        );
    }

    public MerchantRecord unarchiveMerchant(String merchantId, MerchantArchiveRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(merchantId, "merchant_id");
        return executeWrite(
                "unarchiveMerchant:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantRecord.class,
                "merchant.unarchived",
                "merchant.unarchive_failed",
                "merchant.profile.manage",
                "merchant",
                () -> {
                    MerchantRecord merchant = requireMerchant(id);
                    Instant now = Instant.now();
                    // 取消存档=保留该商户,顺带撤销待审批删除,避免"生效可见却挂着待删"的迷惑态。
                    MerchantRecord saved = repository.saveMerchant(
                            merchant.withArchive(null, null, now).withDelete("none", null, null, null, now));
                    repository.setMerchantArchiveReason(id, null);
                    return saved;
                }
        );
    }

    /** 发起删除申请：须先存档（防误删在用商户），记 requested_by/at/reason，状态置 pending。 */
    public MerchantRecord requestDeleteMerchant(String merchantId, MerchantDeleteRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(merchantId, "merchant_id");
        return executeWrite(
                "requestDeleteMerchant:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantRecord.class,
                "merchant.delete_requested",
                "merchant.delete_request_failed",
                "merchant.profile.manage",
                "merchant",
                () -> {
                    MerchantRecord merchant = requireMerchant(id);
                    if (merchant.archivedAt() == null) {
                        throw ApiException.badRequest("MERCHANT_NOT_ARCHIVED",
                                "商户必须先存档才能申请删除");
                    }
                    if ("pending".equals(merchant.deleteStatus())) {
                        throw ApiException.conflict("MERCHANT_DELETE_ALREADY_PENDING",
                                "该商户已有待审批的删除申请");
                    }
                    Instant now = Instant.now();
                    String reason = requireText(request == null ? null : request.reason(), "reason");
                    return repository.saveMerchant(merchant.withDelete("pending", actorId(), now, reason, now));
                }
        );
    }

    /**
     * 审批删除申请。approved 且无活引用 → 物理删除；rejected → 回 none。
     * 禁自审：审批人不得为删除申请发起人（照 supervisor_review 职责分离原则）。
     */
    public MerchantDeleteDecision approveDeleteMerchant(String merchantId, MerchantDeleteApprovalRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(merchantId, "merchant_id");
        return executeWrite(
                "approveDeleteMerchant:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantDeleteDecision.class,
                "merchant.delete_approved",
                "merchant.delete_approve_failed",
                "merchant.profile.approve",
                "merchant",
                () -> {
                    MerchantRecord merchant = requireMerchant(id);
                    if (!"pending".equals(merchant.deleteStatus())) {
                        throw ApiException.badRequest("MERCHANT_DELETE_NOT_PENDING",
                                "该商户没有待审批的删除申请");
                    }
                    String decision = request == null || request.decision() == null
                            ? null : request.decision().trim().toLowerCase(Locale.ROOT);
                    if (!"approved".equals(decision) && !"rejected".equals(decision)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
                    }
                    // 职责分离：删除申请发起人不能审批自己的申请。actorId 为 null（无鉴权/测试）时跳过该闸。
                    String actor = actorId();
                    if (actor != null && actor.equals(merchant.deleteRequestedBy())) {
                        throw ApiException.forbidden("MERCHANT_SELF_APPROVE_FORBIDDEN",
                                "删除申请发起人不能审批自己的申请");
                    }
                    if ("rejected".equals(decision)) {
                        Instant now = Instant.now();
                        repository.saveMerchant(merchant.withDelete("none", null, null, null, now));
                        return new MerchantDeleteDecision(id, "rejected", false);
                    }
                    // approved：先校验商户无核心运营表活引用，有则拒绝硬删（防连锁孤儿）。
                    long refs = repository.countMerchantReferences(id);
                    if (refs > 0) {
                        throw ApiException.conflict("MERCHANT_IN_USE",
                                "商户仍被 " + refs + " 条业务记录引用（合同/账单/工单/员工/酒吧等），不可删除");
                    }
                    repository.deleteMerchant(id);
                    return new MerchantDeleteDecision(id, "approved", true);
                }
        );
    }

    private MerchantRecord requireMerchant(String id) {
        return repository.findMerchantById(id)
                .orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "merchant not found"));
    }

    public PageResponse<ProjectRecord> listProjects() {
        return listProjects(DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    public PageResponse<ProjectRecord> listProjects(int page, int size) {
        PageResponse<ProjectRecord> result = repository.listProjectsPaged(page, size);
        auditEventService.recordSuccess(
                "project.viewed",
                "governance.project.view",
                "project",
                "all",
                null,
                null,
                Map.of("count", result.items().size(), "total", result.total())
        );
        return result;
    }

    public ProjectRecord createProject(ProjectRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createProject",
                idempotencyKey(request),
                request,
                ProjectRecord.class,
                "project.created",
                "project.create_failed",
                "governance.project.manage",
                "project",
                () -> {
                    String name = requireText(request == null ? null : request.name(), "name");
                    String venueType = requireText(request == null ? null : request.venueType(), "venue_type");
                    Instant now = Instant.now();
                    return repository.saveProject(new ProjectRecord(
                            "prj_" + UUID.randomUUID(),
                            trimToNull(request.merchantId()),
                            venueType,
                            name,
                            trimToNull(request.code()),
                            trimToNull(request.floor()),
                            request.capacity(),
                            trimToDefault(request.status(), "active"),
                            now,
                            now
                    ));
                }
        );
    }

    /** 3.13：空间存在性（招商线索/意向意向空间校验用）。 */
    public boolean spaceExists(String spaceId) {
        return spaceId != null && repository.findSpaceById(spaceId).isPresent();
    }

    public PageResponse<SpaceRecord> listSpaces() {
        return listSpaces(DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    public PageResponse<SpaceRecord> listSpaces(int page, int size) {
        PageResponse<SpaceRecord> result = repository.listSpacesPaged(page, size);
        auditEventService.recordSuccess(
                "space.viewed",
                "governance.space.view",
                "space",
                "all",
                null,
                null,
                Map.of("count", result.items().size(), "total", result.total())
        );
        return result;
    }

    public PageResponse<SpaceTypeRecord> listSpaceTypes() {
        List<SpaceTypeRecord> records = repository.listSpaceTypes();
        auditEventService.recordSuccess(
                "space_type.viewed", "governance.space.view", "space_type", "all", null, null,
                Map.of("count", records.size()));
        return page(records);
    }

    public SpaceTypeRecord createSpaceType(com.complex.platform.governance.dto.SpaceTypeRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createSpaceType",
                request == null ? null : request.idempotencyKey(),
                request,
                SpaceTypeRecord.class,
                "space_type.updated",
                "space_type.update_failed",
                "governance.space.manage",
                "space_type",
                () -> {
                    String typeCode = requireText(request == null ? null : request.typeCode(), "type_code");
                    String typeName = requireText(request == null ? null : request.typeName(), "type_name");
                    Instant now = Instant.now();
                    return repository.saveSpaceType(new SpaceTypeRecord(
                            "sptype_" + typeCode, typeCode, typeName,
                            request.sortOrder() == null ? 100 : request.sortOrder(),
                            trimToDefault(request.status(), "active"), now, now));
                });
    }

    public SpaceRecord createSpace(SpaceRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createSpace",
                idempotencyKey(request),
                request,
                SpaceRecord.class,
                "space.updated",
                "space.update_failed",
                "governance.space.manage",
                "space",
                () -> {
                    String spaceName = requireText(request == null ? null : request.spaceName(), "space_name");
                    String spaceType = requireText(request == null ? null : request.spaceType(), "space_type");
                    // 条目3 硬校验：space_type 必须是空间类型字典中处于 active 的取值（前端走下拉、后台可维护字典）。
                    if (!repository.listActiveSpaceTypeCodes().contains(spaceType)) {
                        throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED",
                                "space_type must be an active value in the space type dictionary");
                    }
                    Instant now = Instant.now();
                    return repository.saveSpace(new SpaceRecord(
                            "sp_" + UUID.randomUUID(),
                            trimToNull(request.projectId()),
                            trimToNull(request.merchantId()),
                            trimToNull(request.parentSpaceId()),
                            spaceName,
                            spaceType,
                            trimToNull(request.floorNo()),
                            request.areaSquareMeter(),
                            request.polygonPoints() == null ? List.of() : request.polygonPoints(),
                            trimToDefault(request.status(), "active"),
                            now,
                            now
                    ));
                }
        );
    }

    public PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindings() {
        return listBusinessEntityBindings(DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    public PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindings(int page, int size) {
        try {
            PageResponse<BusinessEntityBindingRecord> result =
                    repository.listBusinessEntityBindingsPaged(page, size);
            auditEventService.recordSuccess(
                    "entity_binding.viewed",
                    "entity.binding.view",
                    "business_entity_binding",
                    "all",
                    null,
                    null,
                    Map.of("count", result.items().size(), "total", result.total())
            );
            return result;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "entity_binding.view_failed",
                    "entity.binding.view",
                    "business_entity_binding",
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public BusinessEntityBindingRecord createBusinessEntityBinding(BusinessEntityBindingRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createBusinessEntityBinding",
                idempotencyKey(request),
                request,
                BusinessEntityBindingRecord.class,
                "entity_binding.changed",
                "entity_binding.change_failed",
                "entity.binding.manage",
                "business_entity_binding",
                () -> {
                    BusinessEntityBindingRequest actual = requireRequest(request);
                    String merchantId = requireText(actual.merchantId(), "merchant_id");
                    repository.findActiveBusinessEntityBindingByMerchantId(merchantId)
                            .ifPresent(existing -> {
                                throw ApiException.conflict(
                                        "ACTIVE_BINDING_EXISTS",
                                        "active business entity binding already exists for merchant"
                                );
                            });
                    Instant now = Instant.now();
                    return repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                            UUID.randomUUID().toString(),
                            merchantId,
                            requireText(actual.operationEntityId(), "operation_entity_id"),
                            requireText(actual.contractEntityId(), "contract_entity_id"),
                            requireText(actual.settlementEntityId(), "settlement_entity_id"),
                            "active",
                            actual.effectiveFrom() == null ? LocalDate.now(ZoneOffset.UTC) : actual.effectiveFrom(),
                            null,
                            trimToNull(actual.changeReason()),
                            now,
                            now
                    ));
                }
        );
    }

    public BusinessEntityBindingRecord changeBusinessEntityBinding(String id, StatusChangeRequest request) {
        authorizationService.requirePlatformScope();
        String bindingId = requireText(id, "id");
        return executeWrite(
                "changeBusinessEntityBinding:" + bindingId,
                idempotencyKey(request),
                request,
                BusinessEntityBindingRecord.class,
                "entity_binding.changed",
                "entity_binding.change_failed",
                "entity.binding.manage",
                "business_entity_binding",
                () -> {
                    StatusChangeRequest actual = requireRequest(request);
                    String status = requireText(actual.action(), "action");
                    if (!Set.of("active", "inactive", "suspended").contains(status)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "action must be active, inactive, or suspended");
                    }
                    String reason = requireText(actual.reason(), "reason");
                    BusinessEntityBindingRecord existing = repository.findBusinessEntityBindingById(bindingId)
                            .orElseThrow(() -> ApiException.notFound("BINDING_NOT_FOUND", "business entity binding not found"));
                    if ("active".equals(status)) {
                        repository.findActiveBusinessEntityBindingByMerchantId(existing.merchantId())
                                .filter(active -> !active.id().equals(existing.id()))
                                .ifPresent(active -> {
                                    throw ApiException.conflict(
                                            "ACTIVE_BINDING_EXISTS",
                                            "active business entity binding already exists for merchant"
                                    );
                                });
                    }
                    Instant now = Instant.now();
                    return repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                            existing.id(),
                            existing.merchantId(),
                            existing.operationEntityId(),
                            existing.contractEntityId(),
                            existing.settlementEntityId(),
                            status,
                            existing.effectiveFrom(),
                            "active".equals(status) ? null : LocalDate.now(ZoneOffset.UTC),
                            reason,
                            existing.createdAt(),
                            now
                    ));
                }
        );
    }




    private MerchantRecord saveMerchant(
            String operationId,
            String merchantId,
            MerchantProfileRequest request
    ) {
        return executeWrite(
                operationId,
                idempotencyKey(request),
                request,
                MerchantRecord.class,
                "merchant.updated",
                "merchant.update_failed",
                "merchant.profile.manage",
                "merchant",
                () -> {
                    String merchantName = requireText(request == null ? null : request.merchantName(), "merchant_name");
                    String merchantType = requireText(request == null ? null : request.merchantType(), "merchant_type");
                    Instant now = Instant.now();
                    return repository.saveMerchant(new MerchantRecord(
                            merchantId,
                            merchantName,
                            merchantType,
                            trimToNull(request.businessEntityId()),
                            trimToNull(request.contractEntityId()),
                            trimToNull(request.settlementEntityId()),
                            trimToNull(request.contactName()),
                            trimToNull(request.contactPhone()),
                            trimToNull(request.ownerPerson()),
                            trimToNull(request.mallBlockCode()),
                            request.areaSquareMeter(),
                            request.leasedAreaSquareMeter(),
                            trimToNull(request.secondaryContactName()),
                            trimToNull(request.secondaryContactPhone()),
                            request.rentPerSquareMeter(),
                            trimToNull(request.cooperationForm()),
                            trimToDefault(request.status(), "active"),
                            now,
                            now
                    ));
                }
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action,
                this::businessId,
                Map.of()
        );
    }

    private <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private String idempotencyKey(BusinessEntityRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(MerchantProfileRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(ProjectRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(SpaceRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(BusinessEntityBindingRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(StatusChangeRequest request) {
        return request == null ? null : request.idempotencyKey();
    }



    private String businessId(Object value) {
        if (value instanceof BusinessEntityRecord record) {
            return record.id();
        }
        if (value instanceof MerchantRecord record) {
            return record.id();
        }
        if (value instanceof ProjectRecord record) {
            return record.id();
        }
        if (value instanceof SpaceRecord record) {
            return record.id();
        }
        if (value instanceof BusinessEntityBindingRecord record) {
            return record.id();
        }
        return null;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
