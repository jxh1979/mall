package com.complex.platform.governance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.governance.dto.ApprovalRequest;
import com.complex.platform.governance.dto.ContractActivateRequest;
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.BusinessEntityRequest;
import com.complex.platform.governance.dto.ContractDraftRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    // ② 履约闭环 S1（PRD phase6）：合同生效终态开关，默认关→activate 拒（关时合同止于 approved，现状语义不变）。
    @Value("${contract.lifecycle_effective.enabled:false}")
    boolean contractLifecycleEffectiveEnabled;

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

    public PageResponse<ContractRecord> listContracts() {
        try {
            List<ContractRecord> records = repository.listContracts();
            auditEventService.recordSuccess(
                    "contract.viewed",
                    "contract.view",
                    "contract",
                    "all",
                    null,
                    null,
                    Map.of("count", records.size())
            );
            return page(records);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "contract.view_failed",
                    "contract.view",
                    "contract",
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public ContractRecord createContractDraft(ContractDraftRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createContractDraft",
                idempotencyKey(request),
                request,
                ContractRecord.class,
                "contract.draft_saved",
                "contract.draft_save_failed",
                "contract.draft.manage",
                "contract",
                () -> {
                    ContractDraftRequest actual = requireRequest(request);
                    Instant now = Instant.now();
                    List<String> attachmentIds = actual.attachmentIds() == null
                            ? List.of()
                            : List.copyOf(actual.attachmentIds());
                    return repository.saveContract(new ContractRecord(
                            "ctr_" + UUID.randomUUID(),
                            requireText(actual.merchantId(), "merchant_id"),
                            requireText(actual.contractSubjectId(), "contract_subject_id"),
                            trimToNull(actual.operationSubjectId()),
                            requireText(actual.settlementSubjectId(), "settlement_subject_id"),
                            requireDate(actual.effectiveDate(), "effective_date"),
                            requireDate(actual.expiryDate(), "expiry_date"),
                            actual.rentTerms() == null ? Map.of() : Map.copyOf(actual.rentTerms()),
                            actual.shareholderRatioForReport(),
                            attachmentIds,
                            "draft",
                            now,
                            now
                    ));
                }
        );
    }

    public ContractRecord approveContract(String contractId, ApprovalRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(contractId, "contract_id");
        return executeWrite(
                "approveContract:" + id,
                idempotencyKey(request),
                request,
                ContractRecord.class,
                "contract.lifecycle_changed",
                "contract.lifecycle_change_failed",
                "contract.lifecycle.approve",
                "contract",
                () -> {
                    ApprovalRequest actual = requireRequest(request);
                    String lifecycleStatus = switch (requireText(actual.decision(), "decision")) {
                        case "approve" -> "approved";
                        case "reject" -> "rejected";
                        case "return" -> "returned";
                        default -> throw ApiException.badRequest("VALIDATION_FAILED", "decision is invalid");
                    };
                    ContractRecord existing = repository.findContractById(id)
                            .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "contract not found"));
                    if (!"draft".equals(existing.lifecycleStatus())) {
                        throw ApiException.conflict("INVALID_STATUS_TRANSITION", "contract is not in draft state");
                    }
                    return repository.saveContract(new ContractRecord(
                            existing.id(),
                            existing.merchantId(),
                            existing.contractSubjectId(),
                            existing.operationSubjectId(),
                            existing.settlementSubjectId(),
                            existing.effectiveDate(),
                            existing.expiryDate(),
                            existing.rentTerms(),
                            existing.shareholderRatioForReport(),
                            existing.attachmentIds(),
                            lifecycleStatus,
                            existing.createdAt(),
                            Instant.now()
                    ));
                }
        );
    }

    /**
     * ② 履约闭环 S1：合同生效（approved→effective）并绑定所租空间（首次写 contract_spaces）。
     * 校验空间存在 + 无重叠生效合同占用（防重复出租）；@Transactional 保证「置生效 + 多空间绑定」原子。
     * 准入闸 over_space 据 contract_spaces 真实判定，本切片落地后该维度生效。
     */
    @Transactional
    public ContractRecord activateContract(String contractId, ContractActivateRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(contractId, "contract_id");
        return executeWrite(
                "activateContract:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                ContractRecord.class,
                "contract.activated",
                "contract.activate_failed",
                "contract.lifecycle.activate",
                "contract",
                () -> {
                    if (!contractLifecycleEffectiveEnabled) {
                        throw ApiException.conflict("CONTRACT_LIFECYCLE_EFFECTIVE_DISABLED",
                                "contract effective lifecycle is disabled");
                    }
                    ContractActivateRequest actual = requireRequest(request);
                    List<String> spaceIds = (actual.spaceIds() == null ? List.<String>of() : actual.spaceIds())
                            .stream().map(s -> requireText(s, "space_id")).distinct().toList();
                    if (spaceIds.isEmpty()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "space_ids is required");
                    }
                    ContractRecord existing = repository.findContractById(id)
                            .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "contract not found"));
                    if (!"approved".equals(existing.lifecycleStatus())) {
                        throw ApiException.conflict("CONTRACT_NOT_APPROVED",
                                "contract must be approved before activation");
                    }
                    // 校验每个空间存在 + 无重叠生效合同占用。
                    // ponytail: 冲突检查 + @Transactional(READ COMMITTED) 足够 admin 低频生效场景；
                    //   若并发生效同空间成真，再对 space 行加咨询锁/FOR UPDATE 收紧。
                    java.util.Map<String, String> spaceProjects = new java.util.LinkedHashMap<>();
                    for (String spaceId : spaceIds) {
                        SpaceRecord space = repository.findSpaceById(spaceId)
                                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "space not found: " + spaceId));
                        if (repository.hasOverlappingEffectiveContractForSpace(
                                spaceId, existing.effectiveDate(), existing.expiryDate(), id)) {
                            throw ApiException.conflict("CONTRACT_SPACE_CONFLICT",
                                    "space already leased by an overlapping effective contract: " + spaceId);
                        }
                        spaceProjects.put(spaceId, space.projectId());
                    }
                    Instant now = Instant.now();
                    ContractRecord effective = repository.saveContract(new ContractRecord(
                            existing.id(), existing.merchantId(), existing.contractSubjectId(),
                            existing.operationSubjectId(), existing.settlementSubjectId(),
                            existing.effectiveDate(), existing.expiryDate(), existing.rentTerms(),
                            existing.shareholderRatioForReport(), existing.attachmentIds(),
                            "effective", existing.createdAt(), now));
                    spaceProjects.forEach((spaceId, projectId) -> repository.bindContractSpace(id, spaceId, projectId, now));
                    return effective;
                }
        );
    }

    /** ② 履约闭环 S1：查某合同已绑定的租赁空间。 */
    public PageResponse<ContractSpaceRecord> listContractSpaces(String contractId) {
        String id = requireText(contractId, "contract_id");
        try {
            List<ContractSpaceRecord> spaces = repository.listContractSpaces(id);
            auditEventService.recordSuccess(
                    "contract_space.viewed", "contract.view", "contract", id, null, null,
                    Map.of("count", spaces.size()));
            return page(spaces);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "contract_space.view_failed", "contract.view", "contract", id, null, null, codeOf(exception));
            throw exception;
        }
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

    private String idempotencyKey(ContractDraftRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(ApprovalRequest request) {
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
        if (value instanceof ContractRecord record) {
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
