package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.customer.MerchantConsumptionRewardService;
import com.complex.platform.evidence.TraceabilityResponsibilityRecord;
import com.complex.platform.evidence.TraceabilityService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 酒吧订单履约收尾（第十批 TK-26-R1 起在送达接口里的那一段，第十八批 AU 段收尾复核抽出共用）：BSM:823「按整笔有效消费全部实际履约完成的时点统一起算
 * 等待期……已取消或已退款部分按原规则从有效消费中排除」；ENUM:112「原订单和责任人继续按实际送达、既有退撤或异常流程处理并留痕」；BSM:1613「谁送达」。
 * 非撤单明细全部送达（至少一行）时：整单置已送达、出品票据置已送达、已结清账单生成商户消费待确认积分与成长值、服务责任记录 completed。
 * 送达接口与退菜退酒生效（即时与审批通过）共用：先送一部分、再退掉其余未送明细的，同样在退菜生效时收尾。
 */
final class BarOrderFulfillment {

    private BarOrderFulfillment() {
    }

    /** 收尾了返回已送达的订单；还有未送达的非撤单明细（或没有非撤单明细）返回空、什么也不动。 */
    static Optional<BarOrderRecord> closeIfFulfilled(BarRepository repository, TraceabilityService traceabilityService,
            MerchantConsumptionRewardService merchantRewards, BarOrderRecord order, String actorId, String idempotencyKey,
            String operatorNote, List<String> evidenceAttachmentIds, Instant now) {
        List<BarOrderItemRecord> live = repository.listOrderItems(order.id()).stream()
                .filter(item -> !"voided".equals(item.deliveryStatus()))
                .toList();
        if (live.isEmpty() || !live.stream().allMatch(item -> "delivered".equals(item.deliveryStatus()))) {
            return Optional.empty();
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
        markProductionTicketDelivered(repository, order.id(), idempotencyKey, operatorNote, actorId, evidenceAttachmentIds, now);
        // 第十一批 TK-04-R3（BSM L823「按整笔有效消费全部实际履约完成的时点统一起算」）：最后一项送达后，已结清账单生成待确认积分与成长值。
        if (merchantRewards != null) merchantRewards.tryCreateForBarOrder(order.id());
        // B1 留痕接线（REQ-34/37）：出品送达写服务责任链记录（merchant 由 token 注入）。
        traceabilityService.recordResponsibility(new TraceabilityResponsibilityRecord(
                null, order.id(), order.tableId(), order.customerId(), null,
                actorId, List.of(), order.salesStaffId(), null, null, null, null,
                actorId, null, null, null, null, null, "completed"));
        return Optional.of(deliveredOrder);
    }

    private static void markProductionTicketDelivered(BarRepository repository, String orderId, String idempotencyKey, String operatorNote,
            String actorId, List<String> evidenceAttachmentIds, Instant now) {
        repository.findProductionTicketByOrderId(orderId)
                .filter(ticket -> !"cancelled".equals(ticket.ticketStatus()))
                .filter(ticket -> !"exception".equals(ticket.ticketStatus()))
                .filter(ticket -> !"delivered".equals(ticket.ticketStatus()))
                .ifPresent(ticket -> {
                    String key = requireText(idempotencyKey, "idempotency_key");
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
                            "production_event_" + slug(ticket.id() + ":delivered:" + key),
                            ticket.id(),
                            ticket.orderId(),
                            ticket.barId(),
                            "deliver",
                            ticket.ticketStatus(),
                            "delivered",
                            null,
                            actorId,
                            trimToDefault(operatorNote, "delivered by service staff"),
                            key,
                            evidenceAttachmentIds,
                            now
                    ));
                });
    }
}
