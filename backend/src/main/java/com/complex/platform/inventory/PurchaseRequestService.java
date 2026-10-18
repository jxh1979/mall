package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.inventory.dto.PurchaseRequestApproveRequest;
import com.complex.platform.inventory.dto.PurchaseRequestReceiveRequest;
import com.complex.platform.inventory.dto.PurchaseRequestTransitionRequest;
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
    private final SupplierQualificationService supplierQualificationService;
    private final InventoryStockMovementService stockMovementService;

    /**
     * CG-10（BSM「管理采购周期，从采购申请、审批、下单、预计到货」：「入库确认后按供应合同的价格口径与账期形成对供应商的应付，进入应收应付台账」）：
     * 应收应付台账写入端口。字段注入免改构造点；生产恒注入，无 Spring 容器的单测为 null 则不挂账。
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.finance.FinanceRepository financeRepository;

    public PurchaseRequestService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            SupplyContractService supplyContractService,
            SupplierQualificationService supplierQualificationService,
            HighValueDecisionService highValueDecisionService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.supplyContractService = supplyContractService;
        this.supplierQualificationService = supplierQualificationService;
        this.stockMovementService = new InventoryStockMovementService(
                repository, idempotencyService, auditEventService, authorizationService,
                // 采购收货只走入库，不经报损/盘点差异，故不建高价值复核单（附件校验器传 null 无影响）。
                new InventoryHighValueReviewService(
                        repository, idempotencyService, auditEventService, authorizationService, null),
                // V350：判定器随构造注入；未注入（旧单测）自建一个 —— 其阈值端口为空 ⇒ 一律判高价值（从严侧）。
                highValueDecisionService != null
                        ? highValueDecisionService
                        : new HighValueDecisionService(repository, authorizationService));
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
                    // P1#18 进货查验卡控（approve 步）：批准放行前校验供应商资质——
                    // 须存在 active 资质且无任一资质过期，过期→阻断（仅 approved 决议时校验，rejected 不卡）。
                    if ("approved".equals(decision) && current.supplierId() != null) {
                        supplierQualificationService.requireQualified(current.supplierId());
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
                    // CG-10：入库确认须录入合同单价——没有价格就形不成「对供应商的应付」（BSM「管理采购周期，从采购申请、审批、下单、预计到货」）。
                    if (actual.unitCostCent() == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "入库确认须录入合同单价（unit_cost_cent）");
                    }
                    PurchaseRequestRecord current = requireForUpdate(id);
                    requireSourceStatus(current, Set.of("arrived"));
                    String operator = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    // received 生成入库流水（inbound）：落 location + 可空 warehouse_id + 采购数量，关联采购单。
                    // V296：unit_cost_cent（进货单价）随入库确认录入，滚动移动加权均价。
                    StockMovementRecord inbound = stockMovementService.createPurchaseInbound(
                            actual.idempotencyKey(),
                            current.id(),
                            current.itemId(),
                            locationId,
                            warehouseId,
                            current.quantity(),
                            actual.unitCostCent(),
                            actual.evidenceAttachmentIds());
                    // CG-10：同事务形成对供应商的应付，进入应收应付台账。
                    recordSupplierPayable(current, inbound, actual.unitCostCent(), now);
                    PurchaseRequestRecord updated = withReceived(current, operator, now);
                    recordEvent(current.id(), current.status(), "received", operator, trimToNull(actual.note()), now);
                    return repository.savePurchaseRequest(updated);
                }
        );
    }

    /**
     * CG-10（BSM「管理采购周期，从采购申请、审批、下单、预计到货」：「入库确认后按供应合同的价格口径与账期形成对供应商的应付，进入应收应付台账」；
     * BSM「应收应付台账」：「来源（含采购入库形成的供应商应付）」）。
     *
     * <p>金额 = 入库数量 × 入库确认录入的合同单价（现采购单无合格/拒收拆分，入库数量即采购数量）。
     * 到期日 = 入库日按账期推算：账期天数优先取采购单所挂供应合同的 {@code paymentTermDays}，其次取供应商生效默认结算档
     * （{@code supplier_settlement_methods}）的 {@code paymentTermDays}；结算方式 monthly（月结）以入库日所在月最后一天为基准再加账期，
     * 其余（cash_on_delivery / credit_term / prepaid / consignment）以入库日为基准加账期；账期为空按 0 天。
     * 台账项 id 按采购单确定性派生（{@code arap_pin_<采购单id>}），纯追加写入 + 唯一索引（direction, source_type, source_id）双重防重；
     * 幂等重放经 {@code IdempotencyService} 不再进入本方法。采购单无供应商或金额为 0 时不挂账（见 PR 待裁决）。
     */
    private void recordSupplierPayable(PurchaseRequestRecord purchase, StockMovementRecord inbound,
            long unitCostCent, Instant now) {
        if (financeRepository == null || purchase.supplierId() == null) {
            return;
        }
        long amountCent = Math.multiplyExact((long) purchase.quantity(), unitCostCent);
        if (amountCent <= 0L) {
            return;
        }
        LocalDate receivedOn = LocalDate.now(LocalZone.ID);
        LocalDate dueDate = supplierPayableDueDate(purchase, receivedOn);
        String itemId = "arap_pin_" + purchase.id();
        financeRepository.appendArApItem(new com.complex.platform.finance.ArApItemRecord(
                itemId, "ARAP-PIN-" + purchase.id(), "payable",
                purchase.supplierId(), "purchase_inbound", purchase.id(),
                amountCent, 0L, amountCent, "CNY",
                dueDate, null, "pending_payment",
                now, now, purchase.merchantId()));
        financeRepository.saveArApItemSource(new com.complex.platform.finance.ArApItemSourceRecord(
                "arap_src_pin_" + purchase.id(), itemId, "purchase_inbound", inbound.id(), amountCent, now));
    }

    /** 账期推算：合同账期 > 供应商默认结算档账期 > 0 天；月结以当月最后一天为基准。 */
    private LocalDate supplierPayableDueDate(PurchaseRequestRecord purchase, LocalDate receivedOn) {
        Integer contractTermDays = purchase.supplyContractId() == null ? null
                : repository.findSupplyContractByIdForUpdate(purchase.supplyContractId())
                        .map(SupplyContractRecord::paymentTermDays).orElse(null);
        Optional<SupplierSettlementMethodRecord> method = repository
                .listSupplierSettlementMethodsBySupplier(purchase.supplierId()).stream()
                .filter(m -> "active".equals(m.status()))
                .sorted((a, b) -> Boolean.compare(b.isDefault(), a.isDefault()))
                .findFirst();
        int termDays = contractTermDays != null ? contractTermDays
                : method.map(SupplierSettlementMethodRecord::paymentTermDays).orElse(null) != null
                        ? method.get().paymentTermDays() : 0;
        LocalDate base = method.map(SupplierSettlementMethodRecord::settlementMethod)
                .filter("monthly"::equals)
                .map(m -> receivedOn.withDayOfMonth(receivedOn.lengthOfMonth()))
                .orElse(receivedOn);
        return base.plusDays(Math.max(termDays, 0));
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
                .orElseThrow(() -> ApiException.notFound("PURCHASE_REQUEST_NOT_FOUND", "采购申请不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(record.merchantId())) {
            throw ApiException.forbidden("PURCHASE_REQUEST_FORBIDDEN", "该采购申请不在当前商户权限范围内");
        }
        return record;
    }

    private void requireSourceStatus(PurchaseRequestRecord record, Set<String> allowed) {
        if (!allowed.contains(record.status())) {
            throw ApiException.conflict("INVALID_STATE",
                    "采购申请状态「" + record.status() + "」不能流转，应为以下之一：" + allowed);
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
        // 实际到货日按所在地(北京)日历日:收货员按北京当天签收,裸 LocalDate.now() 取服务器时区(洛杉矶)会记成昨天。
        LocalDate arrivalDate = "arrived".equals(toStatus)
                ? LocalDate.now(LocalZone.ID) : c.actualArrivalDate();
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

    private String slug(String seed) {
        return com.complex.platform.common.Hashing.idempotencyCode(requireText(seed, "seed"));
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }
}
