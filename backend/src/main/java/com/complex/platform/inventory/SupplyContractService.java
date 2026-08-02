package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.inventory.dto.SupplyContractApproveRequest;
import com.complex.platform.inventory.dto.SupplyContractCreateRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/**
 * R5 T6 供应合同服务（三模式）。
 *
 * <p>职责：建合同（三模式）/审批（draft→approved/rejected，事件 supply_contract.approved）/
 * 可采购范围（{@code purchasable_scope}）卡控查询；按 {@code currentMerchantScope} 隔离——
 * 商户上下文只见本商户合同，平台管理员可见全部。
 */
@Service
public class SupplyContractService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private static final Set<String> CONTRACT_MODES =
            Set.of("platform_self_use", "merchant_self_purchase", "platform_purchase_distribution");
    private static final Set<String> APPROVAL_DECISIONS = Set.of("approved", "rejected");

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public SupplyContractService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<SupplyContractRecord> listSupplyContracts() {
        return readPage(
                "inventory.supply_contract.viewed",
                "inventory.supply_contract.view_failed",
                "inventory.supply_contract.manage",
                "supply_contract",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listSupplyContractsByMerchant)
                                .orElseGet(repository::listSupplyContracts);
                }
        );
    }

    @GuardedAction(capability = "inventory.supply_contract.manage")
    @Transactional
    public SupplyContractRecord createSupplyContract(SupplyContractCreateRequest request) {
        return executeWrite(
                "createSupplyContract",
                request == null ? null : request.idempotencyKey(),
                request,
                SupplyContractRecord.class,
                "supply_contract.created",
                "supply_contract.create_failed",
                "inventory.supply_contract.manage",
                "supply_contract",
                () -> {
                    SupplyContractCreateRequest actual = requireRequest(request);
                    String mode = normalizeAllowed(actual.contractMode(), CONTRACT_MODES, "contract_mode");
                    String projectId = requireText(actual.projectId(), "project_id");
                    String supplierId = requireText(actual.supplierId(), "supplier_id");
                    // 隔离：商户上下文强制归属当前商户，平台管理员可显式指定 merchant_id。
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    String merchantId = scope.orElseGet(() -> trimToNull(actual.merchantId()));
                    if (mode.equals("merchant_self_purchase") && merchantId == null) {
                        throw ApiException.badRequest(
                                "VALIDATION_FAILED", "商户自采必须指定商户");
                    }
                    Instant now = Instant.now();
                    String slug = slug(actual.idempotencyKey());
                    return repository.saveSupplyContract(new SupplyContractRecord(
                            "supply_contract_" + slug,
                            "SC" + now.toEpochMilli() + slug.substring(0, Math.min(6, slug.length())).toUpperCase(Locale.ROOT),
                            supplierId,
                            projectId,
                            merchantId,
                            mode,
                            trimToNull(actual.supplyScope()),
                            trimToNull(actual.priceTerms()),
                            actual.paymentTermDays(),
                            actual.validFrom(),
                            actual.validTo(),
                            trimToNull(actual.summary()),
                            safeList(actual.purchasableScope()),
                            "draft",
                            "active",
                            now,
                            now
                    ));
                }
        );
    }

    @GuardedAction(capability = "inventory.purchase.approve")
    @Transactional
    public SupplyContractRecord approveSupplyContract(String contractId, SupplyContractApproveRequest request) {
        String id = requireText(contractId, "contract_id");
        return executeWrite(
                "approveSupplyContract:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SupplyContractRecord.class,
                "supply_contract.approved",
                "supply_contract.approve_failed",
                "inventory.purchase.approve",
                "supply_contract",
                () -> {
                    SupplyContractApproveRequest actual = requireRequest(request);
                    String decision = normalizeAllowed(actual.decision(), APPROVAL_DECISIONS, "decision");
                    SupplyContractRecord contract = repository.findSupplyContractByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound(
                                    "SUPPLY_CONTRACT_NOT_FOUND", "供货合同不存在或已被删除"));
                    requireSameScope(contract.merchantId());
                    if (!"draft".equals(contract.approvalStatus())) {
                        throw ApiException.conflict(
                                "SUPPLY_CONTRACT_NOT_DRAFT", "供货合同不在草稿状态");
                    }
                    Instant now = Instant.now();
                    return repository.saveSupplyContract(new SupplyContractRecord(
                            contract.id(),
                            contract.contractNo(),
                            contract.supplierId(),
                            contract.projectId(),
                            contract.merchantId(),
                            contract.contractMode(),
                            contract.supplyScope(),
                            contract.priceTerms(),
                            contract.paymentTermDays(),
                            contract.validFrom(),
                            contract.validTo(),
                            contract.summary(),
                            contract.purchasableScope(),
                            decision,
                            contract.status(),
                            contract.createdAt(),
                            now
                    ));
                }
        );
    }

    /**
     * 可采购范围卡控：判定某物料是否可基于该合同采购。
     *
     * <p>要求合同 approved + 生命周期 active + 在有效期内 + item 在 {@code purchasable_scope} 内（空集=不限）。
     * 同时校验调用方作用域可见该合同（隔离）。供后续采购周期服务（代理2）复用。
     *
     * @throws ApiException 不可采购时抛出，便于上层卡控。
     */
    public SupplyContractRecord requirePurchasable(String contractId, String itemId) {
        String id = requireText(contractId, "supply_contract_id");
        String item = requireText(itemId, "item_id");
        SupplyContractRecord contract = repository.findSupplyContractByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound(
                        "SUPPLY_CONTRACT_NOT_FOUND", "供货合同不存在或已被删除"));
        requireSameScope(contract.merchantId());
        if (!"approved".equals(contract.approvalStatus())) {
            throw ApiException.conflict("SUPPLY_CONTRACT_NOT_APPROVED", "供货合同尚未审批通过");
        }
        if (!"active".equals(contract.status())) {
            throw ApiException.conflict("SUPPLY_CONTRACT_NOT_ACTIVE", "供货合同未生效");
        }
        LocalDate today = LocalDate.now();
        if (contract.validFrom() != null && today.isBefore(contract.validFrom())) {
            throw ApiException.conflict("SUPPLY_CONTRACT_NOT_YET_VALID", "供货合同尚未到生效日期");
        }
        if (contract.validTo() != null && today.isAfter(contract.validTo())) {
            throw ApiException.conflict("SUPPLY_CONTRACT_EXPIRED", "供货合同已过期");
        }
        List<String> scope = contract.purchasableScope();
        if (scope != null && !scope.isEmpty() && !scope.contains(item)) {
            throw ApiException.badRequest(
                    "ITEM_OUT_OF_PURCHASABLE_SCOPE", "该商品不在合同可采购范围内");
        }
        return contract;
    }

    private void requireSameScope(String contractMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(contractMerchantId)) {
            throw ApiException.forbidden(
                    "SUPPLY_CONTRACT_FORBIDDEN", "该供货合同不在当前商户权限范围内");
        }
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
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T response = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(response),
                                null,
                                idempotencyKey,
                                Map.of("operation_id", operationId)
                        );
                        return response;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> supplier
    ) {
        try {
            List<T> items = supplier.get();
            auditEventService.recordSuccess(
                    successEvent, permissionCode, businessType, "all", null, null,
                    Map.of("count", items.size()));
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent, permissionCode, businessType, "all", null, null, codeOf(exception));
            throw exception;
        }
    }

    private String businessId(Object response) {
        if (response instanceof SupplyContractRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String normalizeAllowed(String value, Set<String> allowed, String fieldName) {
        String normalized = requireText(value, fieldName).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return normalized;
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private String slug(String seed) {
        return com.complex.platform.common.Hashing.idempotencyCode(requireText(seed, "idempotency_key"));
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }
}
