package com.complex.platform.bar;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.finance.PaymentIntentRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

final class JdbcBarParameterSource {

    private final ObjectMapper objectMapper;

    JdbcBarParameterSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    MapSqlParameterSource tableParameters(BarTableRecord table) {
        return new MapSqlParameterSource()
                .addValue("id", table.id())
                .addValue("bar_id", table.barId())
                .addValue("merchant_id", table.merchantId())
                .addValue("table_code", table.tableCode())
                .addValue("table_name", table.tableName())
                .addValue("table_type", table.tableType())
                .addValue("capacity", table.capacity())
                .addValue("allowed_pass_free_guest_count", table.allowedPassFreeGuestCount())
                .addValue("min_consumption_cent", table.minConsumptionCent())
                .addValue("currency", table.currency())
                .addValue("status", table.status())
                .addValue("created_at", timestampWithTimeZone(table.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(table.updatedAt()));
    }

    MapSqlParameterSource reservationParameters(BarReservationRecord reservation) {
        return new MapSqlParameterSource()
                .addValue("id", reservation.id())
                .addValue("bar_id", reservation.barId())
                .addValue("merchant_id", reservation.merchantId())
                .addValue("table_id", reservation.tableId())
                .addValue("customer_id", reservation.customerId())
                .addValue("reserved_at", timestampWithTimeZone(reservation.reservedAt()))
                .addValue("guest_count", reservation.guestCount())
                .addValue("allowed_pass_free_guest_count", reservation.allowedPassFreeGuestCount())
                .addValue("deposit_amount_cent", reservation.depositAmountCent())
                .addValue("deposit_status", reservation.depositStatus())
                .addValue("order_source", reservation.orderSource())
                .addValue("sales_staff_id", reservation.salesStaffId())
                .addValue("status", reservation.status())
                .addValue("created_terminal", reservation.createdTerminal())
                .addValue("device_id", reservation.deviceId())
                .addValue("evidence_attachment_ids", writeJson(reservation.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(reservation.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(reservation.updatedAt()))
                .addValue("deposit_rule", reservation.depositRule());
    }

    MapSqlParameterSource checkinParameters(BarTableGuestCheckinRecord checkin) {
        return new MapSqlParameterSource()
                .addValue("id", checkin.id())
                .addValue("reservation_id", checkin.reservationId())
                .addValue("bar_id", checkin.barId())
                .addValue("table_id", checkin.tableId())
                .addValue("guest_identifier", checkin.guestIdentifier())
                .addValue("verification_result", checkin.verificationResult())
                .addValue("checked_in_count_after", checkin.checkedInCountAfter())
                .addValue("device_id", checkin.deviceId())
                .addValue("staff_id", checkin.staffId())
                .addValue("evidence_attachment_ids", writeJson(checkin.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(checkin.createdAt()));
    }

    MapSqlParameterSource ageVerificationParameters(BarAgeVerificationRecord verification) {
        return new MapSqlParameterSource()
                .addValue("id", verification.id())
                .addValue("business_id", verification.businessId())
                .addValue("credential_payload", verification.credentialPayload())
                .addValue("device_id", verification.deviceId())
                .addValue("staff_id", verification.staffId())
                .addValue("result", verification.result())
                .addValue("provider_code", verification.providerCode())
                .addValue("review_status", verification.reviewStatus())
                .addValue("evidence_attachment_ids", writeJson(verification.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(verification.createdAt()));
    }

    MapSqlParameterSource productParameters(BarProductRecord product) {
        return new MapSqlParameterSource()
                .addValue("id", product.id())
                .addValue("bar_id", product.barId())
                .addValue("merchant_id", product.merchantId())
                .addValue("product_name", product.productName())
                .addValue("product_type", product.productType())
                .addValue("price_cent", product.priceCent())
                .addValue("currency", product.currency())
                .addValue("inventory_item_id", product.inventoryItemId())
                .addValue("stock_location_id", product.stockLocationId())
                .addValue("stock_deduction_required", product.stockDeductionRequired())
                .addValue("status", product.status())
                .addValue("created_at", timestampWithTimeZone(product.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(product.updatedAt()))
                .addValue("standard_pour_volume", product.standardPourVolume())
                .addValue("pour_volume_unit", product.pourVolumeUnit())
                .addValue("loss_threshold", product.lossThreshold());
    }

    MapSqlParameterSource orderParameters(BarOrderRecord order) {
        return new MapSqlParameterSource()
                .addValue("id", order.id())
                .addValue("bar_id", order.barId())
                .addValue("merchant_id", order.merchantId())
                .addValue("table_id", order.tableId())
                .addValue("reservation_id", order.reservationId())
                .addValue("customer_id", order.customerId())
                .addValue("order_source", order.orderSource())
                .addValue("status", order.status())
                .addValue("total_amount_cent", order.totalAmountCent())
                .addValue("currency", order.currency())
                .addValue("created_terminal", order.createdTerminal())
                .addValue("device_id", order.deviceId())
                .addValue("sales_staff_id", order.salesStaffId())
                .addValue("created_at", timestampWithTimeZone(order.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(order.updatedAt()));
    }

    MapSqlParameterSource billSettlementParameters(BarBillSettlementRecord settlement) {
        return new MapSqlParameterSource()
                .addValue("id", settlement.id())
                .addValue("bill_id", settlement.billId())
                .addValue("settle_channel", settlement.settleChannel())
                .addValue("cashier_staff_id", settlement.cashierStaffId())
                .addValue("pos_transaction_no", settlement.posTransactionNo())
                .addValue("settlement_amount_cent", settlement.settlementAmountCent())
                .addValue("merchant_id", settlement.merchantId())
                .addValue("settled_at", timestampWithTimeZone(settlement.settledAt()));
    }

    MapSqlParameterSource orderItemParameters(BarOrderItemRecord item) {
        return new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("order_id", item.orderId())
                .addValue("product_id", item.productId())
                .addValue("product_name", item.productName())
                .addValue("quantity", item.quantity())
                .addValue("unit_price_cent", item.unitPriceCent())
                .addValue("amount_cent", item.amountCent())
                .addValue("delivery_status", item.deliveryStatus())
                .addValue("created_at", timestampWithTimeZone(item.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(item.updatedAt()))
                .addValue("actual_pour_volume", item.actualPourVolume())
                .addValue("loss_volume", item.lossVolume());
    }

    MapSqlParameterSource productionTicketParameters(BarProductionTicketRecord ticket) {
        return new MapSqlParameterSource()
                .addValue("id", ticket.id())
                .addValue("order_id", ticket.orderId())
                .addValue("bar_id", ticket.barId())
                .addValue("merchant_id", ticket.merchantId())
                .addValue("ticket_status", ticket.ticketStatus())
                .addValue("created_at", timestampWithTimeZone(ticket.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(ticket.updatedAt()));
    }

    MapSqlParameterSource productionTicketEventParameters(BarProductionTicketEventRecord event) {
        return new MapSqlParameterSource()
                .addValue("id", event.id())
                .addValue("ticket_id", event.ticketId())
                .addValue("order_id", event.orderId())
                .addValue("bar_id", event.barId())
                .addValue("action_type", event.actionType())
                .addValue("from_status", event.fromStatus())
                .addValue("to_status", event.toStatus())
                .addValue("station_code", event.stationCode())
                .addValue("operator_user_id", event.operatorUserId())
                .addValue("operator_note", event.operatorNote())
                .addValue("idempotency_key", event.idempotencyKey())
                .addValue("evidence_attachment_ids", writeJson(event.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(event.createdAt()));
    }

    MapSqlParameterSource billParameters(BarBillRecord bill) {
        return new MapSqlParameterSource()
                .addValue("id", bill.id())
                .addValue("bill_no", bill.billNo())
                .addValue("order_id", bill.orderId())
                .addValue("bar_id", bill.barId())
                .addValue("merchant_id", bill.merchantId())
                .addValue("table_id", bill.tableId())
                .addValue("reservation_id", bill.reservationId())
                .addValue("subtotal_amount_cent", bill.subtotalAmountCent())
                .addValue("discount_amount_cent", bill.discountAmountCent())
                .addValue("deposit_deduct_amount_cent", bill.depositDeductAmountCent())
                .addValue("payable_amount_cent", bill.payableAmountCent())
                .addValue("paid_amount_cent", bill.paidAmountCent())
                .addValue("currency", bill.currency())
                .addValue("payment_status", bill.paymentStatus())
                .addValue("settlement_status", bill.settlementStatus())
                .addValue("sales_staff_id", bill.salesStaffId())
                .addValue("sales_role_type", bill.salesRoleType())
                .addValue("created_at", timestampWithTimeZone(bill.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(bill.updatedAt()));
    }

    MapSqlParameterSource billAdjustmentParameters(BarBillAdjustmentRecord adjustment) {
        return new MapSqlParameterSource()
                .addValue("id", adjustment.id())
                .addValue("bill_id", adjustment.billId())
                .addValue("adjustment_type", adjustment.adjustmentType())
                .addValue("amount_cent", adjustment.amountCent())
                .addValue("before_payable_amount_cent", adjustment.beforePayableAmountCent())
                .addValue("after_payable_amount_cent", adjustment.afterPayableAmountCent())
                .addValue("reason", adjustment.reason())
                .addValue("requested_by_user_id", adjustment.requestedByUserId())
                .addValue("approval_required", adjustment.approvalRequired())
                .addValue("status", adjustment.status())
                .addValue("idempotency_key", adjustment.idempotencyKey())
                .addValue("evidence_attachment_ids", writeJson(adjustment.evidenceAttachmentIds()))
                .addValue("ar_ap_item_id", adjustment.arApItemId())
                .addValue("snapshot_json", adjustment.snapshotJson() == null ? "{}" : adjustment.snapshotJson())
                .addValue("created_at", timestampWithTimeZone(adjustment.createdAt()));
    }

    MapSqlParameterSource billOperationParameters(BarBillOperationRecord operation) {
        return new MapSqlParameterSource()
                .addValue("id", operation.id())
                .addValue("operation_type", operation.operationType())
                .addValue("primary_bill_id", operation.primaryBillId())
                .addValue("secondary_bill_id", operation.secondaryBillId())
                .addValue("generated_bill_id", operation.generatedBillId())
                .addValue("amount_cent", operation.amountCent())
                .addValue("reason", operation.reason())
                .addValue("requested_by_user_id", operation.requestedByUserId())
                .addValue("status", operation.status())
                .addValue("idempotency_key", operation.idempotencyKey())
                .addValue("evidence_attachment_ids", writeJson(operation.evidenceAttachmentIds()))
                .addValue("snapshot_json", operation.snapshotJson() == null ? "{}" : operation.snapshotJson())
                .addValue("created_at", timestampWithTimeZone(operation.createdAt()));
    }

    MapSqlParameterSource commissionParameters(BarCommissionCalculationRecord calculation) {
        return new MapSqlParameterSource()
                .addValue("id", calculation.id())
                .addValue("bill_id", calculation.billId())
                .addValue("sales_staff_id", calculation.salesStaffId())
                .addValue("sales_role_type", calculation.salesRoleType())
                .addValue("commission_base_cent", calculation.commissionBaseCent())
                .addValue("commission_rate_bp", calculation.commissionRateBp())
                .addValue("commission_amount_cent", calculation.commissionAmountCent())
                .addValue("status", calculation.status())
                .addValue("created_at", timestampWithTimeZone(calculation.createdAt()));
    }

    MapSqlParameterSource tableSessionParameters(BarTableSessionRecord session) {
        return new MapSqlParameterSource()
                .addValue("id", session.id())
                .addValue("bar_id", session.barId())
                .addValue("merchant_id", session.merchantId())
                .addValue("business_date", session.businessDate())
                .addValue("table_id", session.tableId())
                .addValue("reservation_id", session.reservationId())
                .addValue("customer_id", session.customerId())
                .addValue("guest_count", session.guestCount())
                .addValue("min_consumption_snapshot_cent", session.minConsumptionSnapshotCent())
                .addValue("allowed_pass_free_guest_count_snapshot", session.allowedPassFreeGuestCountSnapshot())
                .addValue("sales_staff_id", session.salesStaffId())
                .addValue("service_staff_id", session.serviceStaffId())
                .addValue("status", session.status())
                .addValue("opened_at", timestampWithTimeZone(session.openedAt()))
                .addValue("closed_at", timestampWithTimeZone(session.closedAt()))
                .addValue("source_session_id", session.sourceSessionId())
                .addValue("session_version", session.sessionVersion())
                .addValue("created_at", timestampWithTimeZone(session.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(session.updatedAt()));
    }

    MapSqlParameterSource tableSessionAdjustmentParameters(BarTableSessionAdjustmentRecord adjustment) {
        return new MapSqlParameterSource()
                .addValue("id", adjustment.id())
                .addValue("session_id", adjustment.sessionId())
                .addValue("adjustment_type", adjustment.adjustmentType())
                .addValue("source_session_id", adjustment.sourceSessionId())
                .addValue("target_session_id", adjustment.targetSessionId())
                .addValue("from_table_id", adjustment.fromTableId())
                .addValue("to_table_id", adjustment.toTableId())
                .addValue("bar_id", adjustment.barId())
                .addValue("merchant_id", adjustment.merchantId())
                .addValue("business_date", adjustment.businessDate())
                .addValue("min_consumption_before_cent", adjustment.minConsumptionBeforeCent())
                .addValue("min_consumption_after_cent", adjustment.minConsumptionAfterCent())
                .addValue("renewed_until", timestampWithTimeZone(adjustment.renewedUntil()))
                .addValue("additional_min_consumption_cent", adjustment.additionalMinConsumptionCent())
                .addValue("additional_deposit_amount_cent", adjustment.additionalDepositAmountCent())
                .addValue("additional_service_fee_cent", adjustment.additionalServiceFeeCent())
                .addValue("reason", adjustment.reason())
                .addValue("requested_by_user_id", adjustment.requestedByUserId())
                .addValue("approval_required", adjustment.approvalRequired())
                .addValue("status", adjustment.status())
                .addValue("idempotency_key", adjustment.idempotencyKey())
                .addValue("evidence_attachment_ids", writeJson(adjustment.evidenceAttachmentIds()))
                .addValue("snapshot_json", adjustment.snapshotJson() == null ? "{}" : adjustment.snapshotJson())
                .addValue("created_at", timestampWithTimeZone(adjustment.createdAt()));
    }

    MapSqlParameterSource stockBalanceParameters(StockBalanceSnapshot balance) {
        return new MapSqlParameterSource()
                .addValue("id", balance.id())
                .addValue("item_id", balance.itemId())
                .addValue("location_id", balance.locationId())
                .addValue("quantity", balance.quantity())
                .addValue("reserved_quantity", balance.reservedQuantity())
                .addValue("available_quantity", balance.availableQuantity())
                .addValue("updated_at", timestampWithTimeZone(balance.updatedAt()));
    }

    MapSqlParameterSource stockMovementParameters(StockMovementSnapshot movement) {
        return new MapSqlParameterSource()
                .addValue("id", movement.id())
                .addValue("movement_type", movement.movementType())
                .addValue("item_id", movement.itemId())
                .addValue("source_location_id", movement.sourceLocationId())
                .addValue("target_location_id", movement.targetLocationId())
                .addValue("quantity", movement.quantity())
                .addValue("business_type", movement.businessType())
                .addValue("business_id", movement.businessId())
                .addValue("trace_code", movement.traceCode())
                .addValue("operator_id", movement.operatorId())
                .addValue("evidence_attachment_ids", writeJson(movement.evidenceAttachmentIds()))
                .addValue("status", movement.status())
                .addValue("created_at", timestampWithTimeZone(movement.createdAt()));
    }

    MapSqlParameterSource paymentIntentParameters(PaymentIntentRecord paymentIntent) {
        return new MapSqlParameterSource()
                .addValue("id", paymentIntent.id())
                .addValue("business_order_type", paymentIntent.businessOrderType())
                .addValue("business_order_id", paymentIntent.businessOrderId())
                .addValue("amount_cent", paymentIntent.amountCent())
                .addValue("currency", paymentIntent.currency())
                .addValue("channel", paymentIntent.channel())
                .addValue("status", paymentIntent.status())
                .addValue("created_at", timestampWithTimeZone(paymentIntent.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(paymentIntent.updatedAt()));
    }



    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("bar JSON value is not serializable", exception);
        }
    }

}
