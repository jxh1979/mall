package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.inventory.dto.PurchaseRequestApproveRequest;
import com.complex.platform.inventory.dto.PurchaseRequestReceiveRequest;
import com.complex.platform.inventory.dto.PurchaseRequestTransitionRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * R5 T6 采购周期完整流转状态机服务。
 *
 * <p>状态机（docs/r5-spec-T6 §2/§3）：
 * {@code created/submitted →(approve) approved/rejected → ordered → expected → arrived → received}。
 * 起点兼容现状——createPurchaseRequest 写 {@code created}，本服务把 {@code created} 与 {@code submitted}
 * 视为同义审批起点。每步：校验合法跳步（非法→INVALID_STATE），写
 * {@code inventory_purchase_request_events}（from/to/operator/note），更新对应流转字段。
 *
 * <p>合同卡控：采购引用 {@code supply_contract_id} 时，在 <b>approve</b> 步调
 * {@link SupplyContractService#requirePurchasable} 校验物料在合同 {@code purchasable_scope} 内
 * （择 approve 步：审批为采购授权关口，最早把不可采购的物料挡住，避免下单后才发现）。
 *
 * <p>入库：{@code received} 时调 {@link InventoryStockMovementService} 生成 inbound 入库流水，
 * 落 location/可空 warehouse_id + 采购数量，business_id 关联采购单。
 *
 * <p>隔离：全部按 {@code currentMerchantScope}——商户上下文只见/只能流转本商户采购单与本商户合同。
 */
@Service
public class PurchaseRequestService {

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final SupplyContractService supplyContractService;
    private final InventoryStockMovementService stockMovementService;

    public PurchaseRequestService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            SupplyContractService supplyContractService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.supplyContractService = supplyContractService;
        this.stockMovementService = new InventoryStockMovementService(
                repository, idempotencyService, auditEventService, authorizationService,
                new InventoryHighValueReviewService(
                        repository, idempotencyService, auditEventService, authorizationService));
    }

    /** 审批：created/submitted →(decision) approved/rejected。绑定合同则按 purchasable_scope 卡控。 */
    @GuardedAction(capability = "inventory.purchase.approve", admissionAction = "inventory.purchase_approve",
            objectType = "inventory_purchase_request")
    @Transactional
    public PurchaseRequestRecord approve(String requestId, PurchaseRequestApproveRequest request) {
        String id = requireText(requestId, "request_id");
        return executeWrite(
                "approvePurchaseRequest:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                "inventory.purchase_status_changed",
                "inventory.purchase_status_change_failed",
                () -> {
                    PurchaseRequestApproveRequest actual = requireRequest(request);
                    String decision = normalizeAllowed(actual.decision(), Set.of("approved", "rejected"), "decision");
                    PurchaseRequestRecord current = requireForUpdate(id);
                    requireSourceStatus(current, Set.of("created", "submitted"));
                    // 合同卡控（approve 步）：采购引用合同时校验物料在 purchasable_scope 内 + 合同有效。
                    String contractId = trimToNull(actual.supplyContractId());
                    if (contractId == null) {
                        contractId = current.supplyContractId();
                    }
                    if (contractId != null) {
                        supplyContractService.requirePurchasable(contractId, current.itemId());
                    }
                    String operator = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    PurchaseRequestRecord updated = withApproval(current, decision, operator, contractId, now);
                    recordEvent(current.id(), current.status(), decision, operator, trimToNull(actual.note()), now);
                    return repository.savePurchaseRequest(updated);
                }
        );
    }

    /**
     * 下单：approved → ordered（落 ordered_at），同时按 §2 设置 expected 到货预告语义。
     *
     * <p>§4 仅暴露 approve/order/arrive/receive 四个端点；§2 的中间态 {@code expected} 不单列端点——
     * 下单即进入“已下单/待到货”阶段，故 order 落 {@code ordered}，arrive 接受 {@code ordered}/{@code expected}
     * 两个源态（兼容直接由系统/对接置 expected 的场景）。
     */
    @GuardedAction(capability = "inventory.purchase.approve", admissionAction = "inventory.purchase_order",
            objectType = "inventory_purchase_request")
    @Transactional
    public PurchaseRequestRecord order(String requestId, PurchaseRequestTransitionRequest request) {
        return transition(requestId, request, "ordered", Set.of("approved"));
    }

    /** 到货：ordered/expected → arrived（落 actual_arrival_date）。 */
    @GuardedAction(capability = "inventory.purchase.approve", admissionAction = "inventory.purchase_arrive",
            objectType = "inventory_purchase_request")
    @Transactional
    public PurchaseRequestRecord arrive(String requestId, PurchaseRequestTransitionRequest request) {
        return transition(requestId, request, "arrived", Set.of("ordered", "expected"));
    }

    /** 入库确认：arrived → received，落 received_at/by 并生成 inbound 入库流水到指定库位/库房。 */
    @GuardedAction(capability = "inventory.purchase.approve", admissionAction = "inventory.purchase_receive",
            objectType = "inventory_purchase_request")
    @Transactional
    public PurchaseRequestRecord receive(String requestId, PurchaseRequestReceiveRequest request) {
        String id = requireText(requestId, "request_id");
        return executeWrite(
                "receivePurchaseRequest:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                "inventory.received",
                "inventory.received_failed",
                () -> {
                    PurchaseRequestReceiveRequest actual = requireRequest(request);
                    String locationId = requireText(actual.locationId(), "location_id");
                    String warehouseId = trimToNull(actual.warehouseId());
                    PurchaseRequestRecord current = requireForUpdate(id);
                    requireSourceStatus(current, Set.of("arrived"));
                    String operator = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    // received 生成入库流水（inbound）：落 location + 可空 warehouse_id + 采购数量，关联采购单。
                    stockMovementService.createPurchaseInbound(
                            actual.idempotencyKey(),
                            current.id(),
                            current.itemId(),
                            locationId,
                            warehouseId,
                            current.quantity(),
                            actual.evidenceAttachmentIds());
                    PurchaseRequestRecord updated = withReceived(current, operator, now);
                    recordEvent(current.id(), current.status(), "received", operator, trimToNull(actual.note()), now);
                    return repository.savePurchaseRequest(updated);
                }
        );
    }

    private PurchaseRequestRecord transition(
            String requestId, PurchaseRequestTransitionRequest request, String toStatus, Set<String> from) {
        String id = requireText(requestId, "request_id");
        return executeWrite(
                "transitionPurchaseRequest:" + toStatus + ":" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                "inventory.purchase_status_changed",
                "inventory.purchase_status_change_failed",
                () -> {
                    PurchaseRequestTransitionRequest actual = requireRequest(request);
                    PurchaseRequestRecord current = requireForUpdate(id);
                    requireSourceStatus(current, from);
                    String operator = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    PurchaseRequestRecord updated = withStatus(current, toStatus, now);
                    recordEvent(current.id(), current.status(), toStatus, operator, trimToNull(actual.note()), now);
                    return repository.savePurchaseRequest(updated);
                }
        );
    }

    private PurchaseRequestRecord requireForUpdate(String id) {
        PurchaseRequestRecord record = repository.findPurchaseRequestByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("PURCHASE_REQUEST_NOT_FOUND", "purchase request not found"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(record.merchantId())) {
            throw ApiException.forbidden("PURCHASE_REQUEST_FORBIDDEN", "purchase request is out of merchant scope");
        }
        return record;
    }

    private void requireSourceStatus(PurchaseRequestRecord record, Set<String> allowed) {
        if (!allowed.contains(record.status())) {
            throw ApiException.conflict("INVALID_STATE",
                    "purchase request status '" + record.status() + "' cannot transition; expected one of " + allowed);
        }
    }

    private void recordEvent(
            String requestId, String fromStatus, String toStatus, String operator, String note, Instant now) {
        repository.savePurchaseRequestEvent(new PurchaseRequestEventRecord(
                "pr_evt_" + slug(requestId + ":" + toStatus + ":" + operator + ":" + now.toEpochMilli() + ":" + fromStatus),
                requestId,
                fromStatus,
                toStatus,
                operator,
                note,
                now));
    }

    private PurchaseRequestRecord withApproval(
            PurchaseRequestRecord c, String decision, String operator, String contractId, Instant now) {
        return new PurchaseRequestRecord(
                c.id(), c.requestNo(), c.supplierId(), c.itemId(), c.quantity(), c.expectedArrivalDate(),
                decision, c.evidenceAttachmentIds(), c.createdAt(), now, c.merchantId(),
                contractId, "approved".equals(decision) ? operator : c.approvedBy(),
                c.orderedAt(), c.actualArrivalDate(), c.receivedAt(), c.receivedBy());
    }

    private PurchaseRequestRecord withStatus(PurchaseRequestRecord c, String toStatus, Instant now) {
        Instant orderedAt = "ordered".equals(toStatus) ? now : c.orderedAt();
        LocalDate arrivalDate = "arrived".equals(toStatus) ? LocalDate.now() : c.actualArrivalDate();
        return new PurchaseRequestRecord(
                c.id(), c.requestNo(), c.supplierId(), c.itemId(), c.quantity(), c.expectedArrivalDate(),
                toStatus, c.evidenceAttachmentIds(), c.createdAt(), now, c.merchantId(),
                c.supplyContractId(), c.approvedBy(), orderedAt, arrivalDate, c.receivedAt(), c.receivedBy());
    }

    private PurchaseRequestRecord withReceived(PurchaseRequestRecord c, String operator, Instant now) {
        return new PurchaseRequestRecord(
                c.id(), c.requestNo(), c.supplierId(), c.itemId(), c.quantity(), c.expectedArrivalDate(),
                "received", c.evidenceAttachmentIds(), c.createdAt(), now, c.merchantId(),
                c.supplyContractId(), c.approvedBy(), c.orderedAt(), c.actualArrivalDate(), now, operator);
    }

    private PurchaseRequestRecord executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String successEvent,
            String failureEvent,
            Supplier<PurchaseRequestRecord> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        PurchaseRequestRecord response = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                "inventory.purchase.approve",
                                "inventory_purchase_request",
                                response.id(),
                                null,
                                idempotencyKey,
                                Map.of("operation_id", operationId, "status", response.status()));
                        return response;
                    },
                    PurchaseRequestRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    "inventory.purchase.approve",
                    "inventory_purchase_request",
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception));
            throw exception;
        }
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String normalizeAllowed(String value, Set<String> allowed, String fieldName) {
        String normalized = requireText(value, fieldName).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                            requireText(seed, "seed").getBytes(StandardCharsets.UTF_8)))
                    .substring(0, 16);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("slug generation unavailable", exception);
        }
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }
}
