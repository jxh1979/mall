package com.complex.platform.bar;

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.TraceabilityResponsibilityRecord;
import com.complex.platform.evidence.TraceabilityService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BarOrderService {

    private static final String CURRENCY_CNY = "CNY";

    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final TraceabilityService traceabilityService;
    private final BarReceivableLedgerService receivableLedgerService;
    /**
     * P0#4 按杯 pour cost 强制采集策略（文档 bar §17.4 / R6）。
     * true = 拒绝模式（从严，默认）：按杯出品缺 actual_pour_volume 直接拒单（POUR_VOLUME_REQUIRED）；
     * false = 标准量兜底模式：缺量时按 standard_pour_volume 计、loss=0，但落 inventory.loss_exception
     * 异常事件并标记 needs_review，严禁静默 loss=0 无标记。门店可通过 bar.pour-cost.strict-mode 配置切换。
     */
    private final boolean pourCostStrictMode;

    // 扫码点单功能标志（bar.scan_order.enabled，默认关）：开启后允许 placeScanOrder——
    // 扫桌台二维码下单，绑开台会话 + 继承入场闸（点单环节不重复年龄核验），来源标 app/miniapp。
    // 字段注入以免改既有多处构造点；包私有便于单测置位（仿 AmusementService.enforceDeviceRegistry）。
    @org.springframework.beans.factory.annotation.Value("${bar.scan_order.enabled:false}")
    boolean scanOrderEnabled;

    @Autowired
    public BarOrderService(BarRepository repository, AuthorizationService authorizationService,
            AuditEventService auditEventService, TraceabilityService traceabilityService,
            BarReceivableLedgerService receivableLedgerService,
            @Value("${bar.pour-cost.strict-mode:true}") boolean pourCostStrictMode) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.traceabilityService = traceabilityService;
        this.receivableLedgerService = receivableLedgerService;
        this.pourCostStrictMode = pourCostStrictMode;
    }

    /** 向后兼容构造器：默认拒绝模式（从严）。 */
    public BarOrderService(BarRepository repository, AuthorizationService authorizationService,
            AuditEventService auditEventService, TraceabilityService traceabilityService,
            BarReceivableLedgerService receivableLedgerService) {
        this(repository, authorizationService, auditEventService, traceabilityService,
                receivableLedgerService, true);
    }

    BarProductRecord createProduct(BarProductRequest request) {
        BarProductRequest actual = requireRequest(request);
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        boolean stockManaged = Boolean.TRUE.equals(actual.stockDeductionRequired());
        // V152 内部结算价（可空；非空须 >=0，DB CHECK 兜底，应用层先验给明确错误码）。
        Long internalPrice = actual.internalSettlementPriceCent();
        if (internalPrice != null && internalPrice < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "internal_settlement_price_cent must be >= 0");
        }
        return repository.saveProduct(new BarProductRecord(
                "product_" + slug(actual.idempotencyKey()),
                requireText(actual.barId(), "bar_id"),
                merchantId,
                requireText(actual.productName(), "product_name"),
                requireText(actual.category(), "category"),
                nonNegative(actual.priceCent(), "price_cent"),
                CURRENCY_CNY,
                trimToNull(actual.inventoryItemId()),
                trimToNull(actual.stockLocationId()),
                stockManaged,
                trimToDefault(actual.status(), "active"),
                now,
                now,
                actual.standardPourVolume(),
                trimToNull(actual.pourVolumeUnit()),
                actual.lossThreshold(),
                false,
                null,
                null,
                internalPrice
        ));
    }

    BarOrderRecord createOrder(BarOrderRequest request) {
        return createOrderInternal(request, "staff", "web", null, null);
    }

    /**
     * @param merchantIdOverride 非空时强制为订单商户（扫码点单取自会话，权威；员工常规下单传 null=按调用者 scope）。
     * @param customerIdOverride 非空时强制为下单客人（客人扫码自助下单=本人；传 null=取自预订客人）。
     */
    private BarOrderRecord createOrderInternal(BarOrderRequest request, String orderSource, String createdTerminal,
            String merchantIdOverride, String customerIdOverride) {
        BarOrderRequest actual = requireRequest(request);
        String barId = requireText(actual.barId(), "bar_id");
        String merchantId = merchantIdOverride != null
                ? merchantIdOverride
                : authorizationService.currentMerchantScope().orElse(null);
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found"));
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "order table is not in this bar");
        }
        // B1d④ 跨商户下单防护：桌位/预定/商品须属订单商户（裁决：资源皆属某商户，平台直营亦为特殊商户）。
        requireResourceInOrderMerchant(merchantId, table.merchantId(), "bar table");
        BarReservationRecord reservation = null;
        if (trimToNull(actual.reservationId()) != null) {
            reservation = repository.findReservationById(actual.reservationId())
                    .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "bar reservation not found"));
            if (!barId.equals(reservation.barId())) {
                throw ApiException.conflict("BAR_SCOPE_MISMATCH", "order reservation is not in this bar");
            }
            requireResourceInOrderMerchant(merchantId, reservation.merchantId(), "bar reservation");
        }
        String orderId = "order_" + slug(actual.idempotencyKey());
        long total = 0L;
        Instant now = Instant.now();
        List<BarOrderItemRecord> orderItems = new ArrayList<>();
        for (Map<String, Object> item : requireItems(actual.items(), "items")) {
            String productId = requireItemText(item, "product_id", "sku_id", "skuId");
            BarProductRecord product = repository.findProductById(productId)
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));
            requireResourceInOrderMerchant(merchantId, product.merchantId(), "bar product");
            requireProductSellable(product);
            int quantity = requirePositiveItemQuantity(item, "quantity");
            // F-TXN-05 赠菜：赠送项对客金额计 0（不计消费额/账单 subtotal），单价保留售价；
            // 成本按商品内部结算价于结账计提时从提成基数扣除（见 BarBillingService.calculateCommission）。
            boolean gift = Boolean.TRUE.equals(item.get("is_gift"));
            long amount = gift ? 0L : product.priceCent() * quantity;
            total += amount;
            orderItems.add(new BarOrderItemRecord(
                    "item_" + slug(orderId + ":" + product.id()),
                    orderId,
                    product.id(),
                    product.productName(),
                    quantity,
                    product.priceCent(),
                    amount,
                    "pending",
                    now,
                    now,
                    null,
                    0L,
                    gift
            ));
        }
        String customerId = customerIdOverride != null
                ? customerIdOverride
                : (reservation == null ? null : reservation.customerId());
        BarOrderRecord order = repository.saveOrder(new BarOrderRecord(
                orderId,
                barId,
                merchantId,
                table.id(),
                reservation == null ? null : reservation.id(),
                customerId,
                orderSource,
                "created",
                total,
                CURRENCY_CNY,
                createdTerminal,
                null,
                trimToNull(actual.salespersonUserId()),
                now,
                now
        ));
        orderItems.forEach(repository::saveOrderItem);
        repository.saveProductionTicket(new BarProductionTicketRecord(
                "ticket_" + slug(actual.idempotencyKey()),
                order.id(),
                order.barId(),
                merchantId,
                "created",
                now,
                now
        ));
        return order;
    }

    /**
     * 扫码点单（绑桌台会话 + 继承入场闸）：扫桌台二维码得 sessionId → 在该开台会话上下单。
     * bar/桌台/预订/客户 一律取自会话（扫码即定位），来源标 app/miniapp 与员工点单区分。
     * 继承入场闸：客人已在场馆统一入场闸完成年龄核验（全场 18+，bar 售酒继承入场结论，见 BarEntryService），
     * 故点单环节不重复核龄——与 createOrder 一致（本就不逐单核龄），此处显式只校验会话有效（已入场就座）、不加核龄。
     * 功能标志 bar.scan_order.enabled 默认关 → 关时拒（上线前灰度/急停开关）。
     */
    /** 员工/工作机扫码下单：经 BarService（staff-token + bar.order.create）；强制本商户会话（跨商户防护）。 */
    BarOrderRecord placeScanOrder(String sessionId, BarOrderRequest request) {
        return createScanOrder(sessionId, request, true, null);
    }

    /**
     * 客人自助扫码下单（customer-token，经 CustomerService 跨域调入）：merchant 取自会话（客人无 scope）、
     * customerId=下单客人本人；不做员工 scope 校验（客人非商户用户，会话商户即权威）。
     */
    public BarOrderRecord placeCustomerScanOrder(String sessionId, BarOrderRequest request, String customerId) {
        return createScanOrder(sessionId, request, false, customerId);
    }

    /**
     * 扫码下单共用核心（绑桌台会话 + 继承入场闸）：扫桌台二维码得 sessionId → 在开台会话上下单，
     * bar/桌台/预订/商户 一律取自会话（扫码定位），来源标 app/miniapp。继承入场闸：客人已在场馆统一入场闸
     * 完成年龄核验（全场 18+，bar 售酒继承入场结论，见 BarEntryService），点单环节不重复核龄（createOrder 本就不逐单核龄）。
     * 功能标志 bar.scan_order.enabled 默认关 → 关时拒（上线前灰度/急停开关）。
     */
    private BarOrderRecord createScanOrder(
            String sessionId, BarOrderRequest request, boolean enforceStaffMerchantScope, String customerIdOverride) {
        if (!scanOrderEnabled) {
            throw ApiException.conflict("BAR_SCAN_ORDER_DISABLED",
                    "scan-to-order is not enabled (feature flag bar.scan_order.enabled is off)");
        }
        BarOrderRequest actual = requireRequest(request);
        BarTableSessionRecord session = repository.findTableSessionById(requireText(sessionId, "session_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_SESSION_NOT_FOUND", "bar table session not found"));
        // 仅「开台中」会话可扫码下单（已关/已并台不可）。
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_NOT_OPEN", "scan order requires an open table session");
        }
        // 员工路径：商户用户只能在本商户会话下单（平台管理员 scope 空放行）。客人路径不校验（会话商户即权威）。
        if (enforceStaffMerchantScope) {
            authorizationService.currentMerchantScope().ifPresent(scope -> {
                if (!scope.equals(session.merchantId())) {
                    throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                            "access to another merchant's table session is not allowed");
                }
            });
        }
        BarOrderRequest derived = new BarOrderRequest(
                actual.idempotencyKey(),
                actual.operatorNote(),
                actual.evidenceAttachmentIds(),
                session.barId(),
                session.tableId(),
                session.reservationId(),
                actual.items(),
                actual.salespersonUserId(),
                actual.serviceUserIds(),
                actual.paymentMode());
        // merchant 取自会话（权威，修员工 scope 兜底 + 支持客人无 scope）；customerId 客人路径=本人。
        return createOrderInternal(derived, "app", "miniapp", session.merchantId(), customerIdOverride);
    }

    BarOrderItemRecord addOrderItem(String orderId, BarOrderRequest request) {
        BarOrderRequest actual = requireRequest(request);
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
        checkMerchantOwnership(order.merchantId());
        BarOrderItemRecord last = null;
        long total = order.totalAmountCent();
        Instant now = Instant.now();
        for (Map<String, Object> item : requireItems(actual.items(), "items")) {
            String productId = requireItemText(item, "product_id", "sku_id", "skuId");
            BarProductRecord product = repository.findProductById(productId)
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));
            requireResourceInOrderMerchant(order.merchantId(), product.merchantId(), "bar product");
            requireProductSellable(product);
            int quantity = requirePositiveItemQuantity(item, "quantity");
            // F-TXN-05 赠菜：赠送项对客金额计 0（不计消费额/账单 subtotal），单价保留售价；
            // 成本按商品内部结算价于结账计提时从提成基数扣除（见 BarBillingService.calculateCommission）。
            boolean gift = Boolean.TRUE.equals(item.get("is_gift"));
            long amount = gift ? 0L : product.priceCent() * quantity;
            total += amount;
            last = repository.saveOrderItem(new BarOrderItemRecord(
                    "item_" + slug(orderId + ":" + product.id() + ":" + actual.idempotencyKey()),
                    orderId,
                    product.id(),
                    product.productName(),
                    quantity,
                    product.priceCent(),
                    amount,
                    "pending",
                    now,
                    now,
                    null,
                    0L,
                    gift
            ));
        }
        repository.saveOrder(new BarOrderRecord(
                order.id(),
                order.barId(),
                order.merchantId(),
                order.tableId(),
                order.reservationId(),
                order.customerId(),
                order.orderSource(),
                order.status(),
                total,
                order.currency(),
                order.createdTerminal(),
                order.deviceId(),
                order.salesStaffId(),
                order.createdAt(),
                now
        ));
        return last;
    }

    BarOrderRecord deliverOrder(String orderId, BarOrderDeliveryRequest request) {
        BarOrderDeliveryRequest actual = requireRequest(request);
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
        checkMerchantOwnership(order.merchantId());
        String actorId = authorizationService.currentActor().id();
        Instant now = Instant.now();
        List<String> deliveryEvidenceAttachmentIds = mergedDeliveryEvidenceAttachmentIds(actual);
        List<Map<String, Object>> deliveredItems = requireItems(actual.deliveredItems(), "delivered_items");
        for (Map<String, Object> delivered : deliveredItems) {
            String itemId = requireItemText(delivered, "order_item_id", "orderItemId", "item_id", "itemId");
            BarOrderItemRecord item = repository.findOrderItemById(itemId)
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_NOT_FOUND", "bar order item not found"));
            requireDeliverableOrderItem(order, item);
            BarProductRecord product = repository.findProductById(item.productId())
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));
            int quantity = requirePositiveItemQuantity(delivered, "quantity");
            requireCompleteDeliveryQuantity(item, quantity);
            if (product.stockDeductionRequired()) {
                atomicDecreaseStock(
                        requireText(product.inventoryItemId(), "inventory_item_id"),
                        requireText(product.stockLocationId(), "stock_location_id"),
                        quantity,
                        order.id(),
                        deliveryEvidenceAttachmentIds
                );
            }
            // REQ-22 / P0#4 按杯出酒量/损耗（文档 bar §17.4 / R6）：按杯出品强制采集 actual_pour_volume。
            // 缺量时按策略处理：拒绝模式直接拒单；兜底模式按标准量计 loss=0 并落异常事件标记 needs_review，
            // 严禁静默 loss=0 无标记。actual>standard 记损耗，超阈值落 inventory.loss_exception 核查留痕。
            PourCostResult pour = resolvePourCost(product, delivered, quantity, item, actual.idempotencyKey());
            Integer actualPourVolume = pour.actualPourVolume();
            long lossVolume = pour.lossVolume();
            repository.saveOrderItem(new BarOrderItemRecord(
                    item.id(),
                    item.orderId(),
                    item.productId(),
                    item.productName(),
                    item.quantity(),
                    item.unitPriceCent(),
                    item.amountCent(),
                    "delivered",
                    item.createdAt(),
                    now,
                    actualPourVolume,
                    lossVolume,
                    false,
                    null,
                    null,
                    null,
                    false,
                    item.isGift(),
                    now
            ));
        }
        BarOrderRecord deliveredOrder = repository.saveOrder(new BarOrderRecord(
                order.id(),
                order.barId(),
                order.merchantId(),
                order.tableId(),
                order.reservationId(),
                order.customerId(),
                order.orderSource(),
                "delivered",
                order.totalAmountCent(),
                order.currency(),
                order.createdTerminal(),
                order.deviceId(),
                order.salesStaffId(),
                order.createdAt(),
                now
        ));
        markProductionTicketDelivered(order.id(), actual, actorId, deliveryEvidenceAttachmentIds, now);
        // B1 留痕接线（REQ-34/37）：出品送达写服务责任链记录（merchant 由 token 注入）。
        traceabilityService.recordResponsibility(new TraceabilityResponsibilityRecord(
                null, order.id(), order.tableId(), order.customerId(), null,
                actorId, List.of(), order.salesStaffId(), null, null, null, null,
                actorId, null, null, null, null, null, "completed"));
        return deliveredOrder;
    }

    /** 按杯 pour cost 解析结果：落库的实际出酒量与损耗量。 */
    private record PourCostResult(Integer actualPourVolume, long lossVolume) {
    }

    /**
     * P0#4 按杯 pour cost 强制采集（文档 bar §17.4 / R6）。
     *
     * <ul>
     *   <li>非按杯出品项（未配置 standard_pour_volume）：维持原行为，仅透传可空 actual_pour_volume，loss=0。</li>
     *   <li>按杯出品 + 已采集 actual_pour_volume：actual>standard 记损耗，超阈值落 inventory.loss_exception。</li>
     *   <li>按杯出品 + 缺 actual_pour_volume + 拒绝模式（默认）：抛 POUR_VOLUME_REQUIRED 拒单。</li>
     *   <li>按杯出品 + 缺 actual_pour_volume + 兜底模式：按 standard×quantity 记实际量、loss=0，
     *       但落 inventory.loss_exception 异常事件标记 needs_review，留痕核查（绝不静默）。</li>
     * </ul>
     */
    private PourCostResult resolvePourCost(BarProductRecord product, Map<String, Object> delivered,
            int quantity, BarOrderItemRecord item, String idempotencyKey) {
        Integer actualPourVolume = optionalPourVolume(delivered);
        // 非按杯出品项：不强制采集，保持既有可空透传。
        if (product.standardPourVolume() == null) {
            return new PourCostResult(actualPourVolume, 0L);
        }
        // 按杯出品但缺 actual_pour_volume：消除"不传则 loss=0"静默绕过。
        if (actualPourVolume == null) {
            if (pourCostStrictMode) {
                throw ApiException.badRequest(
                        "POUR_VOLUME_REQUIRED",
                        "by-glass delivery requires actual_pour_volume");
            }
            // 标准量兜底：按标准量计、loss=0，但落异常事件标记 needs_review 留痕（严禁静默）。
            int standardTotal = product.standardPourVolume() * quantity;
            auditEventService.recordSuccess(
                    "inventory.loss_exception",
                    "bar.order.deliver",
                    "bar_order_item",
                    item.id(),
                    null,
                    idempotencyKey,
                    Map.of(
                            "product_id", product.id(),
                            "reason", "pour_volume_fallback",
                            "actual_pour_volume", standardTotal,
                            "loss_volume", 0,
                            "needs_review", true));
            return new PourCostResult(standardTotal, 0L);
        }
        // 已采集：计算损耗，超阈值落异常事件。
        long lossVolume = BarPourLoss.lossVolume(product.standardPourVolume(), quantity, actualPourVolume);
        if (BarPourLoss.exceedsThreshold(lossVolume, product.lossThreshold())) {
            auditEventService.recordSuccess(
                    "inventory.loss_exception",
                    "bar.order.deliver",
                    "bar_order_item",
                    item.id(),
                    null,
                    idempotencyKey,
                    Map.of(
                            "product_id", product.id(),
                            "actual_pour_volume", actualPourVolume,
                            "loss_volume", lossVolume,
                            "loss_threshold", product.lossThreshold()));
        }
        return new PourCostResult(actualPourVolume, lossVolume);
    }

    private Integer optionalPourVolume(Map<String, Object> item) {
        Object value = item.get("actual_pour_volume");
        if (value == null) {
            value = item.get("actualPourVolume");
        }
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", "actual_pour_volume must be an integer");
        }
    }

    BarProductionTicketRecord transitionProductionTicket(String ticketId, BarProductionTicketTransitionRequest request) {
        BarProductionTicketTransitionRequest actual = requireRequest(request);
        BarProductionTicketRecord current = repository.findProductionTicketById(ticketId)
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCTION_TICKET_NOT_FOUND", "bar production ticket not found"));
        checkMerchantOwnership(current.merchantId());
        String idempotencyKey = requireText(actual.idempotencyKey(), "idempotency_key");
        String actorId = authorizationService.currentActor().id();
        String fromStatus = current.ticketStatus();
        String toStatus = normalizeProductionTicketStatus(actual.targetStatus());
        String actionType = productionActionType(fromStatus, toStatus);
        Instant now = Instant.now();
        BarProductionTicketRecord updated = repository.saveProductionTicket(new BarProductionTicketRecord(
                current.id(),
                current.orderId(),
                current.barId(),
                current.merchantId(),
                toStatus,
                current.createdAt(),
                now
        ));
        repository.saveProductionTicketEvent(new BarProductionTicketEventRecord(
                "production_event_" + slug(current.id() + ":" + toStatus + ":" + idempotencyKey),
                current.id(),
                current.orderId(),
                current.barId(),
                actionType,
                fromStatus,
                toStatus,
                requireText(actual.stationCode(), "station_code"),
                actorId,
                trimToDefault(actual.reason(), trimToDefault(actual.operatorNote(), actionType)),
                idempotencyKey,
                safeList(actual.evidenceAttachmentIds()),
                now
        ));
        return updated;
    }

    private List<String> mergedDeliveryEvidenceAttachmentIds(BarOrderDeliveryRequest request) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(safeList(request.evidenceAttachmentIds()));
        merged.addAll(safeList(request.deliveryPhotoAttachmentIds()));
        return List.copyOf(merged);
    }

    private void requireDeliverableOrderItem(BarOrderRecord order, BarOrderItemRecord item) {
        if (!order.id().equals(item.orderId())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_SCOPE_MISMATCH", "bar order item does not belong to order");
        }
        if ("delivered".equals(item.deliveryStatus())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_ALREADY_DELIVERED", "bar order item is already delivered");
        }
        // P1-6 修复：已撤单(voided)明细不得再次出品（此前仅拦 delivered，撤单后换幂等键可二次扣库存并复活为 delivered）。
        if ("voided".equals(item.deliveryStatus())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_VOIDED", "voided bar order item cannot be delivered");
        }
    }

    private void requireCompleteDeliveryQuantity(BarOrderItemRecord item, int quantity) {
        if (quantity > item.quantity()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "delivery quantity exceeds ordered quantity");
        }
        if (quantity != item.quantity()) {
            throw ApiException.badRequest(
                    "BAR_ORDER_PARTIAL_DELIVERY_UNSUPPORTED",
                    "partial bar order delivery is not supported yet"
            );
        }
    }

    private void markProductionTicketDelivered(
            String orderId,
            BarOrderDeliveryRequest request,
            String actorId,
            List<String> deliveryEvidenceAttachmentIds,
            Instant now
    ) {
        repository.findProductionTicketByOrderId(orderId)
                .filter(ticket -> !"cancelled".equals(ticket.ticketStatus()))
                .filter(ticket -> !"exception".equals(ticket.ticketStatus()))
                .filter(ticket -> !"delivered".equals(ticket.ticketStatus()))
                .ifPresent(ticket -> {
                    BarOrderDeliveryRequest actual = requireRequest(request);
                    String idempotencyKey = requireText(actual.idempotencyKey(), "idempotency_key");
                    repository.saveProductionTicket(new BarProductionTicketRecord(
                            ticket.id(),
                            ticket.orderId(),
                            ticket.barId(),
                            ticket.merchantId(),
                            "delivered",
                            ticket.createdAt(),
                            now
                    ));
                    repository.saveProductionTicketEvent(new BarProductionTicketEventRecord(
                            "production_event_" + slug(ticket.id() + ":delivered:" + idempotencyKey),
                            ticket.id(),
                            ticket.orderId(),
                            ticket.barId(),
                            "deliver",
                            ticket.ticketStatus(),
                            "delivered",
                            null,
                            actorId,
                            trimToDefault(actual.operatorNote(), "delivered by service staff"),
                            idempotencyKey,
                            deliveryEvidenceAttachmentIds,
                            now
                    ));
                });
    }

    private void atomicDecreaseStock(String itemId, String locationId, int quantity, String orderId, List<String> evidenceAttachmentIds) {
        boolean decreased = repository.atomicDecreaseStock(itemId, locationId, quantity);
        if (!decreased) {
            throw ApiException.conflict("STOCK_INSUFFICIENT", "available stock is insufficient");
        }
        repository.saveStockMovement(new StockMovementSnapshot(
                "movement_" + slug(orderId + ":" + itemId + ":" + quantity),
                "outbound",
                itemId,
                locationId,
                null,
                quantity,
                "bar_order",
                orderId,
                null,
                null,
                evidenceAttachmentIds,
                "completed",
                Instant.now()
        ));
    }

    // ========================================================================
    // V091 沽清（86/估清）：置沽清 / 恢复供应；下单时 requireProductSellable 拦截。
    // ========================================================================
    BarProductRecord markSoldOut(String productId, BarProductSoldOutRequest request) {
        BarProductSoldOutRequest actual = requireRequest(request);
        BarProductRecord product = repository.findProductById(requireText(productId, "product_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));
        checkMerchantOwnership(product.merchantId());
        boolean targetSoldOut = actual.soldOut() == null || Boolean.TRUE.equals(actual.soldOut());
        if (targetSoldOut == product.soldOut()) {
            throw ApiException.conflict("BAR_PRODUCT_SOLD_OUT_UNCHANGED", "bar product sold-out state is unchanged");
        }
        Instant now = Instant.now();
        BarProductRecord updated = new BarProductRecord(
                product.id(),
                product.barId(),
                product.merchantId(),
                product.productName(),
                product.productType(),
                product.priceCent(),
                product.currency(),
                product.inventoryItemId(),
                product.stockLocationId(),
                product.stockDeductionRequired(),
                product.status(),
                product.createdAt(),
                now,
                product.standardPourVolume(),
                product.pourVolumeUnit(),
                product.lossThreshold(),
                targetSoldOut,
                targetSoldOut ? now : null,
                targetSoldOut ? trimToDefault(actual.reason(), "marked sold out") : null,
                product.internalSettlementPriceCent()
        );
        BarProductRecord saved = repository.saveProduct(updated);
        auditEventService.recordSuccess(
                targetSoldOut ? "bar.product_sold_out" : "bar.product_restored",
                targetSoldOut ? "bar.product.sold_out" : "bar.product.restore",
                "bar_product",
                product.id(),
                null,
                actual.idempotencyKey(),
                Map.of("product_id", product.id(), "sold_out", targetSoldOut));
        return saved;
    }

    private void requireProductSellable(BarProductRecord product) {
        if (product.soldOut()) {
            throw ApiException.conflict("PRODUCT_SOLD_OUT", "bar product is sold out (86) and cannot be ordered");
        }
    }

    // ========================================================================
    // V091 退菜退酒：未出品→直接取消明细（不计库存损耗）；已出品→退酒（可回补库存）+ 资金从账单扣减。
    // ========================================================================
    BarOrderItemVoidRecord voidOrderItem(String orderId, BarOrderItemVoidRequest request) {
        BarOrderItemVoidRequest actual = requireRequest(request);
        BarOrderRecord order = repository.findOrderById(requireText(orderId, "order_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
        checkMerchantOwnership(order.merchantId());
        BarOrderItemRecord item = repository.findOrderItemById(requireText(actual.orderItemId(), "order_item_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_NOT_FOUND", "bar order item not found"));
        if (!order.id().equals(item.orderId())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_SCOPE_MISMATCH", "bar order item does not belong to order");
        }
        if (item.voided()) {
            throw ApiException.conflict("BAR_ORDER_ITEM_ALREADY_VOIDED", "bar order item is already voided");
        }
        String reason = requireText(actual.reason(), "reason");
        String actorId = authorizationService.currentActor().id();
        String idempotencyKey = requireText(actual.idempotencyKey(), "idempotency_key");
        boolean wasDelivered = "delivered".equals(item.deliveryStatus());
        boolean returnToStock = wasDelivered && Boolean.TRUE.equals(actual.returnToStock());
        Instant now = Instant.now();

        BarProductRecord product = repository.findProductById(item.productId())
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));

        // 已出品退酒 + 选择回补库存：原子加回 + inbound 库存流水留痕。
        String inventoryMovementId = null;
        if (returnToStock && product.stockDeductionRequired()) {
            String inventoryItemId = requireText(product.inventoryItemId(), "inventory_item_id");
            String stockLocationId = requireText(product.stockLocationId(), "stock_location_id");
            boolean increased = repository.atomicIncreaseStock(inventoryItemId, stockLocationId, item.quantity());
            if (!increased) {
                throw ApiException.conflict("STOCK_BALANCE_NOT_FOUND", "stock balance not found for return-to-stock");
            }
            inventoryMovementId = "movement_" + slug(order.id() + ":void:" + item.id() + ":" + idempotencyKey);
            repository.saveStockMovement(new StockMovementSnapshot(
                    inventoryMovementId,
                    "inbound",
                    inventoryItemId,
                    null,
                    stockLocationId,
                    item.quantity(),
                    "bar_order_item_void",
                    item.id(),
                    null,
                    actorId,
                    safeList(actual.evidenceAttachmentIds()),
                    "completed",
                    now));
        }

        // 资金：从关联账单扣减（结账前减应付 = waiver 类减项；结账后走 refund 冲正——复用既有账单调整路径）。
        String billAdjustmentId = handleVoidBillAmount(actual, item, reason, actorId, idempotencyKey);

        // 标记明细已撤单（保留原金额，撤单状态独立列承载，便于审计与聚合）。
        repository.saveOrderItem(new BarOrderItemRecord(
                item.id(),
                item.orderId(),
                item.productId(),
                item.productName(),
                item.quantity(),
                item.unitPriceCent(),
                item.amountCent(),
                "voided",
                item.createdAt(),
                now,
                item.actualPourVolume(),
                item.lossVolume(),
                true,
                reason,
                now,
                actorId,
                returnToStock,
                item.isGift(),
                item.deliveredAt()));

        // 退菜退酒为高危需审批动作：流水 status 承载审批生命周期。
        // approvalRequired=TRUE → 落 'pending'（待审批，由审批决策回写 approved/rejected）；FALSE → 'applied'（直接生效）。
        boolean approvalRequired = true;
        String initialStatus = approvalRequired ? "pending" : "applied";
        BarOrderItemVoidRecord voidRecord = repository.saveOrderItemVoid(new BarOrderItemVoidRecord(
                "void_" + slug(item.id() + ":" + idempotencyKey),
                item.id(),
                order.id(),
                order.barId(),
                order.merchantId(),
                wasDelivered ? "return" : "cancel",
                item.quantity(),
                item.amountCent(),
                wasDelivered,
                returnToStock,
                inventoryMovementId,
                trimToNull(actual.billId()),
                billAdjustmentId,
                reason,
                actorId,
                approvalRequired,
                initialStatus,
                idempotencyKey,
                safeList(actual.evidenceAttachmentIds()),
                now,
                null,
                null,
                null));

        auditEventService.recordSuccess(
                "bar.order_item_voided",
                "bar.order_item.void",
                "bar_order_item",
                item.id(),
                null,
                idempotencyKey,
                Map.of(
                        "order_id", order.id(),
                        "void_type", voidRecord.voidType(),
                        "was_delivered", wasDelivered,
                        "return_to_stock", returnToStock,
                        "amount_cent", item.amountCent()));
        return voidRecord;
    }

    /**
     * V115 / 延期项④：退菜退酒审批决策回写 + 驳回反向冲销。把 pending 流水回写为 approved（通过）
     * 或 rejected（驳回），落审批人/时点/理由，闭合“审批结果未落到 void 流水”的审计缺口。
     *
     * <p>语义：void 设计为“先生效后审批”——库存/资金副作用在 {@link #voidOrderItem} 时已同步施加。
     * <ul>
     *   <li><b>approved（通过）</b>：副作用已生效，审批通过即确认，无需重复施加。</li>
     *   <li><b>rejected（驳回）</b>：该 void 无效，须把 voidOrderItem 已施加的副作用逐一对称回滚——
     *       已回补的库存重新扣减、已减的账单项恢复、已生成的退款应付冲正，恢复到 void 前状态。
     *       见 {@link #reverseVoidSideEffects}。</li>
     * </ul>
     * 审批决策需 bar.order_item.void_decide 权限（V125 P1-2：与申请权限 bar.order_item.void 分离，
     * 杜绝自审批/自驳回）。
     */
    BarOrderItemVoidRecord decideOrderItemVoid(String voidId, BarOrderItemVoidDecisionRequest request) {
        BarOrderItemVoidDecisionRequest actual = requireRequest(request);
        // V125 P1-2：审批分离——决策端点须持独立审批权限，仅持申请权限者不得自审批。
        authorizationService.requirePermission("bar.order_item.void_decide");
        // V125 P1-1：对该 void 流水行加 FOR UPDATE 行锁后再读，使 status==pending 守卫与状态翻转串行化，
        // 不同 idempotencyKey 的并发第二次 reject 读到已非 pending 被拒，杜绝库存/账单冲销双扣。
        BarOrderItemVoidRecord current = repository.findOrderItemVoidByIdForUpdate(requireText(voidId, "void_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_VOID_NOT_FOUND", "bar order item void not found"));
        checkMerchantOwnership(current.merchantId());
        if (!"pending".equals(current.status())) {
            throw ApiException.conflict("BAR_ORDER_ITEM_VOID_NOT_PENDING", "bar order item void is not pending review");
        }
        String decision = normalizeVoidDecision(actual.decision());
        String reviewer = authorizationService.currentActor().id();
        String reviewReason = trimToNull(actual.reason());
        Instant now = Instant.now();

        // 延期项④：驳回 = 该 void 无效 → 对称冲销已施加的库存/账单/退款副作用，恢复 void 前状态。
        // 仅入口处由 status==pending 守卫保证幂等（再次决策被 BAR_ORDER_ITEM_VOID_NOT_PENDING 拦截，不会重复冲销）。
        if ("rejected".equals(decision)) {
            reverseVoidSideEffects(current, reviewer, now);
        }

        BarOrderItemVoidRecord resolved = new BarOrderItemVoidRecord(
                current.id(),
                current.orderItemId(),
                current.orderId(),
                current.barId(),
                current.merchantId(),
                current.voidType(),
                current.quantity(),
                current.amountCent(),
                current.wasDelivered(),
                current.returnToStock(),
                current.inventoryMovementId(),
                current.billId(),
                current.billAdjustmentId(),
                current.reason(),
                current.requestedByUserId(),
                current.approvalRequired(),
                decision,
                current.idempotencyKey(),
                current.evidenceAttachmentIds(),
                current.createdAt(),
                reviewer,
                now,
                reviewReason);
        BarOrderItemVoidRecord saved = repository.saveOrderItemVoid(resolved);

        auditEventService.recordSuccess(
                "bar.order_item_void_decided",
                "bar.order_item.void",
                "bar_order_item_void",
                saved.id(),
                null,
                saved.idempotencyKey(),
                Map.of(
                        "order_id", saved.orderId(),
                        "order_item_id", saved.orderItemId(),
                        "decision", decision,
                        "void_type", saved.voidType(),
                        "amount_cent", saved.amountCent()));
        return saved;
    }

    private String normalizeVoidDecision(String decision) {
        String value = requireText(decision, "decision");
        return switch (value) {
            case "approved", "rejected" -> value;
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
        };
    }

    /**
     * 延期项④：驳回退菜退酒时，对称回滚 {@link #voidOrderItem} 已施加的全部副作用，恢复 void 前状态。
     *
     * <p>回滚项（与 void 施加对称）：
     * <ol>
     *   <li><b>库存</b>：若 void 时回补过库存（returnToStock + 有 inbound 流水）→ 原子扣减相同数量（atomicDecreaseStock）
     *       并落一条 outbound 冲销流水留痕。</li>
     *   <li><b>账单</b>：若 void 时减过应付（有 billAdjustmentId）→ 把应付金额加回（恢复到 void 前），
     *       并落一条反向 BillAdjustment（add_back，beforePayable/afterPayable 记录恢复轨迹）。</li>
     *   <li><b>退款应付</b>：若 void 时为已结账单过账了 refund 应付（原 adjustment 含 arApItemId）→
     *       过一条对冲应收，使财务退款台账净额归零。</li>
     *   <li><b>明细</b>：把 order_item 的 voided 标记/状态恢复为 void 前（清除 voided=true、deliveryStatus 还原）。</li>
     * </ol>
     * 库存扣减幂等与并发由 repository 原子方法 + 行锁保证；不可负库存按既有口径抛 STOCK_INSUFFICIENT。
     */
    private void reverseVoidSideEffects(BarOrderItemVoidRecord current, String reviewer, Instant now) {
        BarOrderItemRecord item = repository.findOrderItemById(current.orderItemId())
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_NOT_FOUND", "bar order item not found"));

        // 1) 库存回滚：void 回补过库存 → 重新扣减并落 outbound 冲销流水。
        if (current.returnToStock() && current.inventoryMovementId() != null) {
            BarProductRecord product = repository.findProductById(item.productId())
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));
            if (product.stockDeductionRequired()) {
                String inventoryItemId = requireText(product.inventoryItemId(), "inventory_item_id");
                String stockLocationId = requireText(product.stockLocationId(), "stock_location_id");
                boolean decreased = repository.atomicDecreaseStock(inventoryItemId, stockLocationId, current.quantity());
                if (!decreased) {
                    throw ApiException.conflict("STOCK_INSUFFICIENT", "available stock is insufficient");
                }
                repository.saveStockMovement(new StockMovementSnapshot(
                        "movement_" + slug(current.id() + ":void_reject:" + item.id() + ":" + current.idempotencyKey()),
                        "outbound",
                        inventoryItemId,
                        stockLocationId,
                        null,
                        current.quantity(),
                        "bar_order_item_void_reject",
                        item.id(),
                        null,
                        reviewer,
                        safeList(current.evidenceAttachmentIds()),
                        "completed",
                        now));
            }
        }

        // 2/3) 账单 + 退款应付回滚：void 减过应付 → 加回应付并落反向 adjustment；已结退款过账则对冲。
        if (current.billAdjustmentId() != null && current.billId() != null && current.amountCent() > 0) {
            BarBillRecord bill = repository.findBillById(current.billId()).orElse(null);
            if (bill != null) {
                checkMerchantOwnership(bill.merchantId());
                long amount = current.amountCent();
                long beforePayable = bill.payableAmountCent();
                long afterPayable = beforePayable + amount;
                boolean settled = "settled".equals(bill.settlementStatus());

                // 反向 adjustment 用独立幂等键（区别于原 void 减项，避开 uq_bar_bill_adjustments_idempotency 唯一索引冲突）。
                String reversalIdempotencyKey = current.idempotencyKey() + ":void_reject";
                // 已结退款过账冲正：过一条对冲应收，财务退款台账净额归零。
                String reversalArApItemId = null;
                String reversalAdjustmentId = "bill_adjustment_void_reject_"
                        + slug(bill.id() + ":" + item.id() + ":" + reversalIdempotencyKey);
                if (settled) {
                    BarOrderRecord refundOrder = repository.findOrderById(bill.orderId())
                            .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
                    reversalArApItemId = receivableLedgerService.reverseRefundPayable(
                            bill, refundOrder, reversalAdjustmentId, amount,
                            reversalIdempotencyKey, "void rejected: " + current.reason(),
                            safeList(current.evidenceAttachmentIds()));
                }

                repository.saveBill(com.complex.platform.bar.BarBillRecords.billWithAmounts(
                        bill,
                        bill.subtotalAmountCent(),
                        bill.discountAmountCent(),
                        bill.depositDeductAmountCent(),
                        afterPayable,
                        bill.paidAmountCent(),
                        bill.paymentStatus(),
                        bill.settlementStatus(),
                        now));
                repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                        reversalAdjustmentId,
                        bill.id(),
                        settled ? "refund_reversal" : "waiver_reversal",
                        amount,
                        beforePayable,
                        afterPayable,
                        "void rejected, restore bill: " + current.reason(),
                        reviewer,
                        true,
                        "applied",
                        reversalIdempotencyKey,
                        safeList(current.evidenceAttachmentIds()),
                        reversalArApItemId,
                        com.complex.platform.bar.BarBillRecords.billAdjustmentSnapshot(
                                bill, afterPayable, bill.paymentStatus(), bill.settlementStatus(), reversalArApItemId),
                        now,
                        settled ? now : null));
            }
        }

        // 4) 明细回滚：清除 voided 标记，恢复 void 前出品状态（已出品→delivered，未出品→pending）。
        String restoredDeliveryStatus = current.wasDelivered() ? "delivered" : "pending";
        // V232：撤单回滚——恢复 delivered 保留原送达时刻，恢复 pending 清空(未送达)。
        Instant restoredDeliveredAt = current.wasDelivered() ? item.deliveredAt() : null;
        repository.saveOrderItem(new BarOrderItemRecord(
                item.id(),
                item.orderId(),
                item.productId(),
                item.productName(),
                item.quantity(),
                item.unitPriceCent(),
                item.amountCent(),
                restoredDeliveryStatus,
                item.createdAt(),
                now,
                item.actualPourVolume(),
                item.lossVolume(),
                false,
                null,
                null,
                null,
                false,
                item.isGift(),
                restoredDeliveredAt));
    }

    /** 退菜退酒资金回退：在关联账单上做减项（未结=waiver 减应付；已结=refund 冲正）；无账单则仅明细取消不动资金。 */
    private String handleVoidBillAmount(
            BarOrderItemVoidRequest request,
            BarOrderItemRecord item,
            String reason,
            String actorId,
            String idempotencyKey
    ) {
        String billId = trimToNull(request.billId());
        if (billId == null || item.amountCent() <= 0) {
            return null;
        }
        BarBillRecord bill = repository.findBillById(billId).orElse(null);
        if (bill == null) {
            return null;
        }
        checkMerchantOwnership(bill.merchantId());
        // P1-4 修复：billId 由客户端提供，此前仅校验同商户、未校验账单归属本订单，可把退款/冲减作用到任意同商户他单账单。
        if (!bill.orderId().equals(item.orderId())) {
            throw ApiException.conflict("BAR_BILL_ORDER_MISMATCH", "bill does not belong to the voided item's order");
        }
        boolean settled = "settled".equals(bill.settlementStatus());
        long amount = item.amountCent();
        long beforePayable = bill.payableAmountCent();
        long afterPayable = Math.max(0L, beforePayable - amount);
        Instant now = Instant.now();
        String adjustmentType = settled ? "refund" : "waiver";
        Instant refundedAt = settled ? now : null;
        // 已结账单退酒资金回退 → finance refund 应付台账（含 DAY_CLOSE_LOCKED 校验在 finance 层强拦截）。
        String arApItemId = null;
        if (settled) {
            // C4/P1-3 修复：退酒触发真实退款（settled→refund）必须额外持「退款」高危权限，与正规退款入口
            // (applyBillAdjustment refund) 口径一致，堵住凭「退酒」权限绕过退款审批把关的暗道。
            authorizationService.requirePermission("bar.bill.refund");
            BarOrderRecord refundOrder = repository.findOrderById(bill.orderId())
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
            arApItemId = receivableLedgerService.createRefundPayable(
                    bill, refundOrder,
                    "bill_adjustment_void_" + slug(bill.id() + ":" + item.id() + ":" + idempotencyKey),
                    amount, idempotencyKey, reason, safeList(request.evidenceAttachmentIds()));
        }
        repository.saveBill(com.complex.platform.bar.BarBillRecords.billWithAmounts(
                bill,
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                afterPayable,
                bill.paidAmountCent(),
                bill.paymentStatus(),
                bill.settlementStatus(),
                now));
        String adjustmentId = "bill_adjustment_void_" + slug(bill.id() + ":" + item.id() + ":" + idempotencyKey);
        repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                adjustmentId,
                bill.id(),
                adjustmentType,
                amount,
                beforePayable,
                afterPayable,
                "void item: " + reason,
                actorId,
                true,
                "applied",
                idempotencyKey,
                safeList(request.evidenceAttachmentIds()),
                arApItemId,
                com.complex.platform.bar.BarBillRecords.billAdjustmentSnapshot(
                        bill, afterPayable, bill.paymentStatus(), bill.settlementStatus(), arApItemId),
                now,
                refundedAt));
        return adjustmentId;
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "access to another merchant's resource is not allowed");
        }
    }

    /**
     * B1d④：下单/加项时校验所引用资源（桌位/预定/商品）属订单商户。
     * 订单商户取自调用者 scope（员工常规下单）或开台会话（扫码点单，权威）；非空时资源 merchant 必须一致。
     * 订单商户为空=平台域调用方（无商户 scope，跨商户合法）放行；裁决「资源皆属某商户」故资源 merchant 为空亦视为不一致而拒。
     */
    private void requireResourceInOrderMerchant(String orderMerchantId, String resourceMerchantId, String resourceLabel) {
        if (orderMerchantId != null && !orderMerchantId.equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    resourceLabel + " belongs to another merchant");
        }
    }

    private String normalizeProductionTicketStatus(String targetStatus) {
        String status = requireText(targetStatus, "target_status");
        return switch (status) {
            case "accepted", "preparing", "ready", "delivered", "cancelled", "exception" -> status;
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "unsupported production ticket target status");
        };
    }

    private String productionActionType(String fromStatus, String toStatus) {
        if (fromStatus.equals(toStatus)) {
            throw ApiException.conflict("BAR_PRODUCTION_STATUS_UNCHANGED", "production ticket status is unchanged");
        }
        return switch (fromStatus + "->" + toStatus) {
            case "created->accepted" -> "accept";
            case "accepted->preparing" -> "start_prepare";
            case "preparing->ready" -> "mark_ready";
            case "ready->delivered" -> "deliver";
            case "created->cancelled", "accepted->cancelled", "preparing->cancelled", "ready->cancelled" -> "cancel";
            case "created->exception", "accepted->exception", "preparing->exception", "ready->exception" -> "exception";
            default -> throw ApiException.conflict("BAR_PRODUCTION_INVALID_TRANSITION", "invalid production ticket transition");
        };
    }

}
