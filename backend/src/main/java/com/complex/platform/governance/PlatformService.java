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

@Service
public class PlatformService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final PlatformRepository repository;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    // F-AUTHZ-02 治理写仅平台：治理写方法（建商户/法律实体绑定/合同/项目空间等）首行经
    // requirePlatformScope() fail-closed 拒 merchant-scoped 主体，防权限被下放给商户角色后越权治理。
    private final AuthorizationService authorizationService;

    public PlatformService(
            PlatformRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<BusinessEntityRecord> listBusinessEntities() {
        List<BusinessEntityRecord> records = repository.listBusinessEntities();
        auditEventService.recordSuccess(
                "entity_binding.viewed",
                "entity.binding.view",
                "business_entity",
                "all",
                null,
                null,
                Map.of("count", records.size())
        );
        return page(records);
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

    public PageResponse<MerchantRecord> listMerchants() {
        List<MerchantRecord> records = repository.listMerchants();
        auditEventService.recordSuccess(
                "merchant.viewed",
                "merchant.profile.view",
                "merchant",
                "all",
                null,
                null,
                Map.of("count", records.size())
        );
        return page(records);
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

    public PageResponse<ProjectRecord> listProjects() {
        List<ProjectRecord> records = repository.listProjects();
        auditEventService.recordSuccess(
                "project.viewed",
                "governance.project.view",
                "project",
                "all",
                null,
                null,
                Map.of("count", records.size())
        );
        return page(records);
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

    public PageResponse<SpaceRecord> listSpaces() {
        List<SpaceRecord> records = repository.listSpaces();
        auditEventService.recordSuccess(
                "space.viewed",
                "governance.space.view",
                "space",
                "all",
                null,
                null,
                Map.of("count", records.size())
        );
        return page(records);
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
        try {
            List<BusinessEntityBindingRecord> records = repository.listBusinessEntityBindings();
            auditEventService.recordSuccess(
                    "entity_binding.viewed",
                    "entity.binding.view",
                    "business_entity_binding",
                    "all",
                    null,
                    null,
                    Map.of("count", records.size())
            );
            return page(records);
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

    private String requireText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
