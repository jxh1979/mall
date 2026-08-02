package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.inventory.dto.SupplierQualificationRequest;
import com.complex.platform.inventory.dto.SupplierSettlementMethodRequest;
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
 * P1#18 供应商资质（进货查验）+ 结算多档/账期子表服务。
 *
 * <p>职责：
 * <ul>
 *   <li>资质 CRUD（建/列）：七类 {@code qualification_type} CHECK 卡控，附件/有效期/状态。</li>
 *   <li>结算档 CRUD（建/列）：五类 {@code settlement_method} + 账期 + 适用品类 + 默认档；
 *       主表既有 {@code settlement_method} 保留为兼容默认，本服务以子表承载多档。</li>
 *   <li>进货查验卡控 {@link #requireQualified}：供采购审批引用——供应商须存在 active 资质，
 *       且无任一资质过期（{@code valid_to < today}）→ 过期则阻断。</li>
 * </ul>
 *
 * <p>隔离：全部按 {@code currentMerchantScope}——商户上下文只见/只能管本商户供应商子表。
 * 风格对齐 {@link WarehouseService}/{@link SupplyContractService}：executeWrite/readPage + 审计事件。
 */
@Service
public class SupplierQualificationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private static final Set<String> QUALIFICATION_TYPES = Set.of(
            "business_license", "food_business_license", "production_license",
            "inspection_report", "hygiene_permit", "import_record", "other");
    private static final Set<String> QUALIFICATION_STATUSES = Set.of("active", "suspended", "archived");
    private static final Set<String> SETTLEMENT_METHODS = Set.of(
            "cash_on_delivery", "monthly", "credit_term", "prepaid", "consignment");
    private static final Set<String> SETTLEMENT_STATUSES = Set.of("active", "archived");

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public SupplierQualificationService(
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

    // ── 资质：CRUD ──────────────────────────────────────────────────────────────
    public PageResponse<SupplierQualificationRecord> listQualifications(String supplierId) {
        return readPage(
                "inventory.supplier_qualification.viewed",
                "inventory.supplier_qualification.view_failed",
                "inventory.supplier.qualification.manage",
                "supplier_qualification",
                () -> {
                    String supplier = trimToNull(supplierId);
                    if (supplier != null) {
                        return scopedQualifications(supplier);
                    }
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listSupplierQualificationsByMerchant)
                                .orElseGet(repository::listSupplierQualifications);
                }
        );
    }

    @GuardedAction(capability = "inventory.supplier.manage")
    @Transactional
    public SupplierQualificationRecord createQualification(SupplierQualificationRequest request) {
        return executeWrite(
                "createSupplierQualification",
                request == null ? null : request.idempotencyKey(),
                request,
                SupplierQualificationRecord.class,
                "inventory.supplier_qualification.changed",
                "inventory.supplier_qualification.change_failed",
                "inventory.supplier.qualification.manage",
                "supplier_qualification",
                () -> {
                    SupplierQualificationRequest actual = requireRequest(request);
                    String supplierId = requireText(actual.supplierId(), "supplier_id");
                    requireSupplierInScope(supplierId);
                    String type = normalizeAllowed(actual.qualificationType(), QUALIFICATION_TYPES, "qualification_type");
                    String status = normalizeAllowedOrDefault(
                            actual.status(), QUALIFICATION_STATUSES, "active", "status");
                    if (actual.validFrom() != null && actual.validTo() != null
                            && actual.validTo().isBefore(actual.validFrom())) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "有效期结束日期不能早于开始日期");
                    }
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    return repository.saveSupplierQualification(new SupplierQualificationRecord(
                            "supplier_qual_" + slug(actual.idempotencyKey()),
                            supplierId,
                            type,
                            trimToNull(actual.qualificationNo()),
                            trimToNull(actual.issuedBy()),
                            actual.validFrom(),
                            actual.validTo(),
                            safeList(actual.attachmentIds()),
                            status,
                            now,
                            now,
                            merchantId
                    ));
                }
        );
    }

    // ── 结算档：CRUD ────────────────────────────────────────────────────────────
    public PageResponse<SupplierSettlementMethodRecord> listSettlementMethods(String supplierId) {
        return readPage(
                "inventory.supplier_settlement.viewed",
                "inventory.supplier_settlement.view_failed",
                "inventory.supplier.settlement.manage",
                "supplier_settlement_method",
                () -> {
                    String supplier = trimToNull(supplierId);
                    if (supplier != null) {
                        return scopedSettlementMethods(supplier);
                    }
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listSupplierSettlementMethodsByMerchant)
                                .orElseGet(repository::listSupplierSettlementMethods);
                }
        );
    }

    @GuardedAction(capability = "inventory.supplier.manage")
    @Transactional
    public SupplierSettlementMethodRecord createSettlementMethod(SupplierSettlementMethodRequest request) {
        return executeWrite(
                "createSupplierSettlementMethod",
                request == null ? null : request.idempotencyKey(),
                request,
                SupplierSettlementMethodRecord.class,
                "inventory.supplier_settlement.changed",
                "inventory.supplier_settlement.change_failed",
                "inventory.supplier.settlement.manage",
                "supplier_settlement_method",
                () -> {
                    SupplierSettlementMethodRequest actual = requireRequest(request);
                    String supplierId = requireText(actual.supplierId(), "supplier_id");
                    requireSupplierInScope(supplierId);
                    String method = normalizeAllowed(actual.settlementMethod(), SETTLEMENT_METHODS, "settlement_method");
                    String status = normalizeAllowedOrDefault(
                            actual.status(), SETTLEMENT_STATUSES, "active", "status");
                    Integer termDays = nonNegativeOrNull(actual.paymentTermDays(), "payment_term_days");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    return repository.saveSupplierSettlementMethod(new SupplierSettlementMethodRecord(
                            "supplier_settle_" + slug(actual.idempotencyKey()),
                            supplierId,
                            method,
                            termDays,
                            trimToNull(actual.applicableCategory()),
                            Boolean.TRUE.equals(actual.isDefault()),
                            status,
                            now,
                            now,
                            merchantId
                    ));
                }
        );
    }

    /**
     * 进货查验卡控：判定某供应商进货时资质是否合规。
     *
     * <p>要求：供应商存在 ≥1 条 {@code active} 资质，且其中无任一 active 资质已过期
     * （{@code valid_to} 非空且 {@code < today}）。任一 active 资质过期→阻断（采购审批关口最早挡住）。
     * 缺失 active 资质亦阻断（无证不可进货）。供 {@link PurchaseRequestService} 审批步复用。
     *
     * @throws ApiException 不合规时抛出，便于上层卡控。
     */
    public void requireQualified(String supplierId) {
        String supplier = requireText(supplierId, "supplier_id");
        List<SupplierQualificationRecord> qualifications = scopedQualifications(supplier).stream()
                .filter(q -> "active".equals(q.status()))
                .toList();
        if (qualifications.isEmpty()) {
            throw ApiException.conflict(
                    "SUPPLIER_QUALIFICATION_MISSING", "供应商没有生效中的资质，入库检查不通过");
        }
        LocalDate today = LocalDate.now();
        boolean anyExpired = qualifications.stream()
                .anyMatch(q -> q.validTo() != null && q.validTo().isBefore(today));
        if (anyExpired) {
            throw ApiException.conflict(
                    "SUPPLIER_QUALIFICATION_EXPIRED", "供应商资质已过期，入库被拦截");
        }
    }

    /** 供应商存在性 + 作用域隔离校验（商户上下文只能引用本商户供应商）。 */
    private void requireSupplierInScope(String supplierId) {
        InventorySupplierRecord supplier = scopedSuppliers().stream()
                .filter(s -> s.id().equals(supplierId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("SUPPLIER_NOT_FOUND", "供应商不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(supplier.merchantId())) {
            throw ApiException.forbidden("SUPPLIER_FORBIDDEN", "该供应商不在当前商户权限范围内");
        }
    }

    private List<InventorySupplierRecord> scopedSuppliers() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return scope.map(repository::listInventorySuppliersByMerchant)
                    .orElseGet(repository::listInventorySuppliers);
    }

    private List<SupplierQualificationRecord> scopedQualifications(String supplierId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return repository.listSupplierQualificationsBySupplier(supplierId).stream()
                .filter(q -> scope.isEmpty() || scope.get().equals(q.merchantId()))
                .toList();
    }

    private List<SupplierSettlementMethodRecord> scopedSettlementMethods(String supplierId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return repository.listSupplierSettlementMethodsBySupplier(supplierId).stream()
                .filter(m -> scope.isEmpty() || scope.get().equals(m.merchantId()))
                .toList();
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
        if (response instanceof SupplierQualificationRecord record) {
            return record.id();
        }
        if (response instanceof SupplierSettlementMethodRecord record) {
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

    private String normalizeAllowedOrDefault(String value, Set<String> allowed, String defaultValue, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return defaultValue;
        }
        return normalizeAllowed(trimmed, allowed, fieldName);
    }

    private Integer nonNegativeOrNull(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为负数");
        }
        return value;
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
