package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.inventory.dto.WarehouseCreateRequest;
import java.time.Instant;
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
 * R5 T6 结构化库房服务。
 *
 * <p>职责：库房 CRUD（建/列），六类 {@code warehouse_type} CHECK 卡控；按 {@code currentMerchantScope}
 * 隔离。库存挂 {@code warehouse_id} 能力由 {@link InventoryService} 出入库/余额承载，本服务负责库房主数据。
 */
@Service
public class WarehouseService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private static final Set<String> WAREHOUSE_TYPES =
            Set.of("main", "bar", "bar_counter", "consumable", "gift", "saved_bottle");
    private static final Set<String> WAREHOUSE_STATUSES = Set.of("active", "archived");

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public WarehouseService(
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

    public PageResponse<WarehouseRecord> listWarehouses() {
        return readPage(
                "inventory.warehouse.viewed",
                "inventory.warehouse.view_failed",
                "inventory.warehouse.manage",
                "warehouse",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listWarehousesByMerchant)
                                .orElseGet(repository::listWarehouses);
                }
        );
    }

    @GuardedAction(capability = "inventory.warehouse.manage")
    @Transactional
    public WarehouseRecord createWarehouse(WarehouseCreateRequest request) {
        return executeWrite(
                "createWarehouse",
                request == null ? null : request.idempotencyKey(),
                request,
                WarehouseRecord.class,
                "inventory.warehouse.created",
                "inventory.warehouse.create_failed",
                "inventory.warehouse.manage",
                "warehouse",
                () -> {
                    WarehouseCreateRequest actual = requireRequest(request);
                    String type = normalizeAllowed(actual.warehouseType(), WAREHOUSE_TYPES, "warehouse_type");
                    String status = normalizeAllowedOrDefault(actual.status(), WAREHOUSE_STATUSES, "active", "status");
                    String projectId = requireText(actual.projectId(), "project_id");
                    String name = requireText(actual.name(), "name");
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    String merchantId = scope.orElseGet(() -> trimToNull(actual.merchantId()));
                    Instant now = Instant.now();
                    return repository.saveWarehouse(new WarehouseRecord(
                            "warehouse_" + slug(actual.idempotencyKey()),
                            projectId,
                            merchantId,
                            type,
                            name,
                            status,
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
        if (response instanceof WarehouseRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
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
