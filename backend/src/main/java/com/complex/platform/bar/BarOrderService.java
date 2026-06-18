package com.complex.platform.bar;

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
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

    public BarOrderService(BarRepository repository, AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
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
                now
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
                    now
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
                    now
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
        return deliveredOrder;
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
