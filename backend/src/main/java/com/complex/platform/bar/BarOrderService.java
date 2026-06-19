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
import org.springframework.stereotype.Service;

@Service
public class BarOrderService {

    private static final String CURRENCY_CNY = "CNY";

    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final TraceabilityService traceabilityService;
    private final BarReceivableLedgerService receivableLedgerService;

    public BarOrderService(BarRepository repository, AuthorizationService authorizationService,
            AuditEventService auditEventService, TraceabilityService traceabilityService,
            BarReceivableLedgerService receivableLedgerService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.traceabilityService = traceabilityService;
        this.receivableLedgerService = receivableLedgerService;
    }

    BarProductRecord createProduct(BarProductRequest request) {
        BarProductRequest actual = requireRequest(request);
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        boolean stockManaged = Boolean.TRUE.equals(actual.stockDeductionRequired());
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
                actual.lossThreshold()
        ));
    }

    BarOrderRecord createOrder(BarOrderRequest request) {
        BarOrderRequest actual = requireRequest(request);
        String barId = requireText(actual.barId(), "bar_id");
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found"));
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "order table is not in this bar");
        }
        BarReservationRecord reservation = null;
        if (trimToNull(actual.reservationId()) != null) {
            reservation = repository.findReservationById(actual.reservationId())
                    .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "bar reservation not found"));
            if (!barId.equals(reservation.barId())) {
                throw ApiException.conflict("BAR_SCOPE_MISMATCH", "order reservation is not in this bar");
            }
        }
        String orderId = "order_" + slug(actual.idempotencyKey());
        long total = 0L;
        Instant now = Instant.now();
        List<BarOrderItemRecord> orderItems = new ArrayList<>();
        for (Map<String, Object> item : requireItems(actual.items(), "items")) {
            String productId = requireItemText(item, "product_id", "sku_id", "skuId");
            BarProductRecord product = repository.findProductById(productId)
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));
            requireProductSellable(product);
            int quantity = requirePositiveItemQuantity(item, "quantity");
            long amount = product.priceCent() * quantity;
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
                    0L
            ));
        }
        BarOrderRecord order = repository.saveOrder(new BarOrderRecord(
                orderId,
                barId,
                merchantId,
                table.id(),
                reservation == null ? null : reservation.id(),
                reservation == null ? null : reservation.customerId(),
                "staff",
                "created",
                total,
                CURRENCY_CNY,
                "web",
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
            requireProductSellable(product);
            int quantity = requirePositiveItemQuantity(item, "quantity");
            long amount = product.priceCent() * quantity;
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
                    0L
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
            // REQ-22 按杯出酒量/损耗：出品时记录实际出酒量，超标准量记损耗；超阈值触发 inventory.loss_exception 核查留痕。
            Integer actualPourVolume = optionalPourVolume(delivered);
            long lossVolume = 0L;
            if (actualPourVolume != null && product.standardPourVolume() != null) {
                lossVolume = BarPourLoss.lossVolume(product.standardPourVolume(), quantity, actualPourVolume);
                if (BarPourLoss.exceedsThreshold(lossVolume, product.lossThreshold())) {
                    auditEventService.recordSuccess(
                            "inventory.loss_exception",
                            "bar.order.deliver",
                            "bar_order_item",
                            item.id(),
                            null,
                            actual.idempotencyKey(),
                            Map.of(
                                    "product_id", product.id(),
                                    "actual_pour_volume", actualPourVolume,
                                    "loss_volume", lossVolume,
                                    "loss_threshold", product.lossThreshold()
                            )
                    );
                }
            }
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
                    lossVolume
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
                targetSoldOut ? trimToDefault(actual.reason(), "marked sold out") : null
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
                returnToStock));

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
                true,
                "applied",
                idempotencyKey,
                safeList(actual.evidenceAttachmentIds()),
                now));

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
