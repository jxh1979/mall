package com.complex.platform.bar;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

final class JdbcBarRowMapper {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    JdbcBarRowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    BarTableRecord mapTable(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarTableRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("table_code"),
                resultSet.getString("table_name"),
                resultSet.getString("table_type"),
                resultSet.getInt("capacity"),
                resultSet.getInt("allowed_pass_free_guest_count"),
                resultSet.getLong("min_consumption_cent"),
                resultSet.getString("currency"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    BarReservationRecord mapReservation(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarReservationRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("table_id"),
                resultSet.getString("customer_id"),
                nullableInstant(resultSet, "reserved_at"),
                resultSet.getInt("guest_count"),
                resultSet.getInt("allowed_pass_free_guest_count"),
                resultSet.getLong("deposit_amount_cent"),
                resultSet.getString("deposit_status"),
                resultSet.getString("order_source"),
                resultSet.getString("sales_staff_id"),
                resultSet.getString("status"),
                resultSet.getString("created_terminal"),
                resultSet.getString("device_id"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getString("deposit_rule"),
                resultSet.getString("deposit_intent_id"),
                nullableInstant(resultSet, "deposit_paid_at"),
                resultSet.getString("deposit_settlement")
        );
    }

    BarDepositRuleRecord mapDepositRule(ResultSet resultSet, int rowNumber) throws SQLException {
        Long fixedAmount = resultSet.getObject("fixed_amount_cent") == null
                ? null : resultSet.getLong("fixed_amount_cent");
        Integer ratioBps = resultSet.getObject("ratio_bps") == null
                ? null : resultSet.getInt("ratio_bps");
        return new BarDepositRuleRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("scope_type"),
                resultSet.getString("scope_id"),
                resultSet.getString("mode"),
                fixedAmount,
                ratioBps,
                resultSet.getInt("cancel_free_before_minutes"),
                resultSet.getBoolean("enabled"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    BarBillSettlementRecord mapBillSettlement(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarBillSettlementRecord(
                resultSet.getString("id"),
                resultSet.getString("bill_id"),
                resultSet.getString("settle_channel"),
                resultSet.getString("cashier_staff_id"),
                resultSet.getString("pos_transaction_no"),
                resultSet.getLong("settlement_amount_cent"),
                resultSet.getString("merchant_id"),
                nullableInstant(resultSet, "settled_at"));
    }

    BarProductRecord mapProduct(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarProductRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("product_name"),
                resultSet.getString("product_type"),
                resultSet.getLong("price_cent"),
                resultSet.getString("currency"),
                resultSet.getString("inventory_item_id"),
                resultSet.getString("stock_location_id"),
                resultSet.getBoolean("stock_deduction_required"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getObject("standard_pour_volume", Integer.class),
                resultSet.getString("pour_volume_unit"),
                resultSet.getObject("loss_threshold", Integer.class)
        );
    }

    BarOrderRecord mapOrder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarOrderRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("table_id"),
                resultSet.getString("reservation_id"),
                resultSet.getString("customer_id"),
                resultSet.getString("order_source"),
                resultSet.getString("status"),
                resultSet.getLong("total_amount_cent"),
                resultSet.getString("currency"),
                resultSet.getString("created_terminal"),
                resultSet.getString("device_id"),
                resultSet.getString("sales_staff_id"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getString("order_type")
        );
    }

    BarOrderItemRecord mapOrderItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarOrderItemRecord(
                resultSet.getString("id"),
                resultSet.getString("order_id"),
                resultSet.getString("product_id"),
                resultSet.getString("product_name"),
                resultSet.getInt("quantity"),
                resultSet.getLong("unit_price_cent"),
                resultSet.getLong("amount_cent"),
                resultSet.getString("delivery_status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getObject("actual_pour_volume", Integer.class),
                resultSet.getLong("loss_volume")
        );
    }

    BarProductionTicketRecord mapProductionTicket(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarProductionTicketRecord(
                resultSet.getString("id"),
                resultSet.getString("order_id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("ticket_status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    BarProductionTicketEventRecord mapProductionTicketEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarProductionTicketEventRecord(
                resultSet.getString("id"),
                resultSet.getString("ticket_id"),
                resultSet.getString("order_id"),
                resultSet.getString("bar_id"),
                resultSet.getString("action_type"),
                resultSet.getString("from_status"),
                resultSet.getString("to_status"),
                resultSet.getString("station_code"),
                resultSet.getString("operator_user_id"),
                resultSet.getString("operator_note"),
                resultSet.getString("idempotency_key"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                nullableInstant(resultSet, "created_at"),
                resultSet.getString("inventory_movement_id")
        );
    }

    BarGiftRedemptionRecord mapGiftRedemption(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarGiftRedemptionRecord(
                resultSet.getString("id"),
                resultSet.getString("entitlement_id"),
                resultSet.getString("verification_id"),
                resultSet.getString("bar_order_id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("item_ref"),
                resultSet.getString("inventory_movement_id"),
                resultSet.getString("status"),
                resultSet.getString("note"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    BarBillRecord mapBill(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarBillRecord(
                resultSet.getString("id"),
                resultSet.getString("bill_no"),
                resultSet.getString("order_id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("table_id"),
                resultSet.getString("reservation_id"),
                resultSet.getLong("subtotal_amount_cent"),
                resultSet.getLong("discount_amount_cent"),
                resultSet.getLong("deposit_deduct_amount_cent"),
                resultSet.getLong("payable_amount_cent"),
                resultSet.getLong("paid_amount_cent"),
                resultSet.getString("currency"),
                resultSet.getString("payment_status"),
                resultSet.getString("settlement_status"),
                resultSet.getString("sales_staff_id"),
                resultSet.getString("sales_role_type"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    BarBillAdjustmentRecord mapBillAdjustment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarBillAdjustmentRecord(
                resultSet.getString("id"),
                resultSet.getString("bill_id"),
                resultSet.getString("adjustment_type"),
                resultSet.getLong("amount_cent"),
                resultSet.getLong("before_payable_amount_cent"),
                resultSet.getLong("after_payable_amount_cent"),
                resultSet.getString("reason"),
                resultSet.getString("requested_by_user_id"),
                resultSet.getBoolean("approval_required"),
                resultSet.getString("status"),
                resultSet.getString("idempotency_key"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("ar_ap_item_id"),
                resultSet.getString("snapshot_json"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "refunded_at")
        );
    }

    BarBillOperationRecord mapBillOperation(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarBillOperationRecord(
                resultSet.getString("id"),
                resultSet.getString("operation_type"),
                resultSet.getString("primary_bill_id"),
                resultSet.getString("secondary_bill_id"),
                resultSet.getString("generated_bill_id"),
                resultSet.getLong("amount_cent"),
                resultSet.getString("reason"),
                resultSet.getString("requested_by_user_id"),
                resultSet.getString("status"),
                resultSet.getString("idempotency_key"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("snapshot_json"),
                nullableInstant(resultSet, "created_at")
        );
    }

    BarTableSessionRecord mapTableSession(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarTableSessionRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getObject("business_date", LocalDate.class),
                resultSet.getString("table_id"),
                resultSet.getString("reservation_id"),
                resultSet.getString("customer_id"),
                resultSet.getInt("guest_count"),
                resultSet.getLong("min_consumption_snapshot_cent"),
                resultSet.getInt("allowed_pass_free_guest_count_snapshot"),
                resultSet.getString("sales_staff_id"),
                resultSet.getString("service_staff_id"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "opened_at"),
                nullableInstant(resultSet, "closed_at"),
                resultSet.getString("source_session_id"),
                resultSet.getLong("session_version"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    BarTableSessionAdjustmentRecord mapTableSessionAdjustment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarTableSessionAdjustmentRecord(
                resultSet.getString("id"),
                resultSet.getString("session_id"),
                resultSet.getString("adjustment_type"),
                resultSet.getString("source_session_id"),
                resultSet.getString("target_session_id"),
                resultSet.getString("from_table_id"),
                resultSet.getString("to_table_id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getObject("business_date", LocalDate.class),
                resultSet.getLong("min_consumption_before_cent"),
                resultSet.getLong("min_consumption_after_cent"),
                nullableInstant(resultSet, "renewed_until"),
                resultSet.getLong("additional_min_consumption_cent"),
                resultSet.getLong("additional_deposit_amount_cent"),
                resultSet.getLong("additional_service_fee_cent"),
                resultSet.getString("reason"),
                resultSet.getString("requested_by_user_id"),
                resultSet.getBoolean("approval_required"),
                resultSet.getString("status"),
                resultSet.getString("idempotency_key"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("snapshot_json"),
                nullableInstant(resultSet, "created_at")
        );
    }

    StockBalanceSnapshot mapStockBalance(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StockBalanceSnapshot(
                resultSet.getString("id"),
                resultSet.getString("item_id"),
                resultSet.getString("location_id"),
                resultSet.getInt("quantity"),
                resultSet.getInt("reserved_quantity"),
                resultSet.getInt("available_quantity"),
                nullableInstant(resultSet, "updated_at")
        );
    }


    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("bar JSON payload is not readable", exception);
        }
    }
}
