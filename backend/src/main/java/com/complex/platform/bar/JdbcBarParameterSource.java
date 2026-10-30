package com.complex.platform.bar;

// 文件版本：V0.03

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
                .addValue("deposit_rule", reservation.depositRule())
                .addValue("deposit_intent_id", reservation.depositIntentId())
                .addValue("deposit_paid_at", timestampWithTimeZone(reservation.depositPaidAt()))
                .addValue("deposit_settlement", reservation.depositSettlement())
                .addValue("invitation_token", reservation.invitationToken())
                .addValue("invitation_signature", reservation.invitationSignature())
                .addValue("invitation_capacity", reservation.invitationCapacity())
                .addValue("invitation_status", reservation.invitationStatus())
                .addValue("invitation_issued_at", timestampWithTimeZone(reservation.invitationIssuedAt()))
                .addValue("archived_at", timestampWithTimeZone(reservation.archivedAt()))
                .addValue("archived_by", reservation.archivedBy())
                .addValue("delete_status", reservation.deleteStatus() == null ? "none" : reservation.deleteStatus())
                .addValue("delete_requested_by", reservation.deleteRequestedBy())
                .addValue("delete_requested_at", timestampWithTimeZone(reservation.deleteRequestedAt()))
                .addValue("delete_reason", reservation.deleteReason())
                .addValue("deposit_channel", reservation.depositChannel())
                .addValue("deposit_settled_at", timestampWithTimeZone(reservation.depositSettledAt()))
                .addValue("latest_arrival_at", timestampWithTimeZone(reservation.latestArrivalAt()));
    }

    MapSqlParameterSource invitationMemberParameters(BarReservationInvitationMemberRecord member) {
        return new MapSqlParameterSource()
                .addValue("id", member.id())
                .addValue("reservation_id", member.reservationId())
                .addValue("bar_id", member.barId())
                .addValue("member_identifier", member.memberIdentifier())
                .addValue("member_customer_id", member.memberCustomerId())
                .addValue("is_holder", member.isHolder())
                .addValue("status", member.status())
                .addValue("confirmed_at", timestampWithTimeZone(member.confirmedAt()))
                .addValue("entered_at", timestampWithTimeZone(member.enteredAt()))
                .addValue("device_id", member.deviceId())
                .addValue("created_at", timestampWithTimeZone(member.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(member.updatedAt()));
    }

    MapSqlParameterSource depositRuleParameters(BarDepositRuleRecord rule) {
        return new MapSqlParameterSource()
                .addValue("id", rule.id())
                .addValue("bar_id", rule.barId())
                .addValue("merchant_id", rule.merchantId())
                .addValue("scope_type", rule.scopeType())
                .addValue("scope_id", rule.scopeId())
                .addValue("mode", rule.mode())
                .addValue("fixed_amount_cent", rule.fixedAmountCent())
                .addValue("ratio_bps", rule.ratioBps())
                .addValue("cancel_free_before_minutes", rule.cancelFreeBeforeMinutes())
                .addValue("enabled", rule.enabled())
                .addValue("created_at", timestampWithTimeZone(rule.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(rule.updatedAt()));
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
                .addValue("loss_threshold", product.lossThreshold())
                .addValue("sold_out", product.soldOut())
                .addValue("sold_out_at", timestampWithTimeZone(product.soldOutAt()))
                .addValue("sold_out_reason", product.soldOutReason())
                .addValue("internal_settlement_price_cent", product.internalSettlementPriceCent())
                .addValue("category_id", product.categoryId())
                .addValue("image_url", product.imageUrl());
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
                .addValue("updated_at", timestampWithTimeZone(order.updatedAt()))
                .addValue("order_type", order.orderType() == null ? "standard" : order.orderType())
                .addValue("paid_at", timestampWithTimeZone(order.paidAt()));
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

    MapSqlParameterSource shiftParameters(BarShiftRecord shift) {
        return new MapSqlParameterSource()
                .addValue("id", shift.id())
                .addValue("project_id", shift.projectId())
                .addValue("merchant_id", shift.merchantId())
                .addValue("bar_id", shift.barId())
                .addValue("cashier_user_id", shift.cashierUserId())
                .addValue("status", shift.status())
                .addValue("opened_at", timestampWithTimeZone(shift.openedAt()))
                .addValue("closed_at", timestampWithTimeZone(shift.closedAt()))
                .addValue("opening_cash_cent", shift.openingCashCent())
                .addValue("declared_cash_cent", shift.declaredCashCent())
                .addValue("system_cash_cent", shift.systemCashCent())
                .addValue("system_pos_cent", shift.systemPosCent())
                .addValue("system_online_cent", shift.systemOnlineCent())
                .addValue("system_refund_cent", shift.systemRefundCent())
                .addValue("system_tip_cent", shift.systemTipCent())
                .addValue("cash_diff_cent", shift.cashDiffCent())
                .addValue("cash_diff_threshold_cent", shift.cashDiffThresholdCent())
                .addValue("needs_review", shift.needsReview())
                .addValue("handover_to_user_id", shift.handoverToUserId())
                .addValue("note", shift.note())
                .addValue("opened_by_user_id", shift.openedByUserId())
                .addValue("closed_by_user_id", shift.closedByUserId())
                .addValue("created_at", timestampWithTimeZone(shift.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(shift.updatedAt()));
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
                .addValue("loss_volume", item.lossVolume())
                .addValue("voided", item.voided())
                .addValue("void_reason", item.voidReason())
                .addValue("voided_at", timestampWithTimeZone(item.voidedAt()))
                .addValue("voided_by_user_id", item.voidedByUserId())
                .addValue("return_to_stock", item.returnToStock())
                .addValue("is_gift", item.isGift())
                .addValue("delivered_at", timestampWithTimeZone(item.deliveredAt()));
    }

    MapSqlParameterSource orderItemVoidParameters(BarOrderItemVoidRecord voidRecord) {
        return new MapSqlParameterSource()
                .addValue("id", voidRecord.id())
                .addValue("order_item_id", voidRecord.orderItemId())
                .addValue("order_id", voidRecord.orderId())
                .addValue("bar_id", voidRecord.barId())
                .addValue("merchant_id", voidRecord.merchantId())
                .addValue("void_type", voidRecord.voidType())
                .addValue("quantity", voidRecord.quantity())
                .addValue("amount_cent", voidRecord.amountCent())
                .addValue("was_delivered", voidRecord.wasDelivered())
                .addValue("return_to_stock", voidRecord.returnToStock())
                .addValue("inventory_movement_id", voidRecord.inventoryMovementId())
                .addValue("bill_id", voidRecord.billId())
                .addValue("bill_adjustment_id", voidRecord.billAdjustmentId())
                .addValue("reason", voidRecord.reason())
                .addValue("requested_by_user_id", voidRecord.requestedByUserId())
                .addValue("approval_required", voidRecord.approvalRequired())
                .addValue("status", voidRecord.status())
                .addValue("idempotency_key", voidRecord.idempotencyKey())
                .addValue("evidence_attachment_ids", writeJson(voidRecord.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(voidRecord.createdAt()))
                .addValue("reviewed_by_user_id", voidRecord.reviewedByUserId())
                .addValue("reviewed_at", timestampWithTimeZone(voidRecord.reviewedAt()))
                .addValue("review_reason", voidRecord.reviewReason())
                // V345（BAR-VOID-A1）：审批闸模式 / 结构化原因码 / 阈值快照 / 副作用落地时点。
                .addValue("approval_mode", voidRecord.approvalMode() == null
                        ? BarOrderService.VOID_MODE_IMMEDIATE : voidRecord.approvalMode())
                .addValue("reason_code", voidRecord.reasonCode())
                .addValue("high_value_threshold_cent", voidRecord.highValueThresholdCent())
                .addValue("effects_applied_at", timestampWithTimeZone(voidRecord.effectsAppliedAt()));
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
                .addValue("created_at", timestampWithTimeZone(event.createdAt()))
                .addValue("inventory_movement_id", event.inventoryMovementId());
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
                .addValue("updated_at", timestampWithTimeZone(bill.updatedAt()))
                .addValue("reopened", bill.reopened())
                .addValue("reopened_by_user_id", bill.reopenedByUserId())
                .addValue("reopened_at", timestampWithTimeZone(bill.reopenedAt()))
                .addValue("reopen_reason", bill.reopenReason())
                .addValue("reopen_count", bill.reopenCount())
                .addValue("tip_amount_cent", bill.tipAmountCent())
                .addValue("paid_at", timestampWithTimeZone(bill.paidAt()));
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
                .addValue("created_at", timestampWithTimeZone(adjustment.createdAt()))
                .addValue("refunded_at", timestampWithTimeZone(adjustment.refundedAt()));
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
                .addValue("guest_source", session.guestSource())
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

    MapSqlParameterSource giftRedemptionParameters(BarGiftRedemptionRecord redemption) {
        return new MapSqlParameterSource()
                .addValue("id", redemption.id())
                .addValue("entitlement_id", redemption.entitlementId())
                .addValue("verification_id", redemption.verificationId())
                .addValue("bar_order_id", redemption.barOrderId())
                .addValue("bar_id", redemption.barId())
                .addValue("merchant_id", redemption.merchantId())
                .addValue("item_ref", redemption.itemRef())
                .addValue("inventory_movement_id", redemption.inventoryMovementId())
                .addValue("status", redemption.status())
                .addValue("note", redemption.note())
                .addValue("created_at", timestampWithTimeZone(redemption.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(redemption.updatedAt()));
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
