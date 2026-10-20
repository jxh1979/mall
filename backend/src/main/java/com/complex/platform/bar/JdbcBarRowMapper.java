package com.complex.platform.bar;

// 文件版本：V0.01

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

    /** 第十批 TK-27-R2：台位列表面（带待清台起始时刻与超时标记）。 */
    BarTableRecord mapTableListing(ResultSet resultSet, int rowNumber) throws SQLException {
        BarTableRecord base = mapTable(resultSet, rowNumber);
        return new BarTableRecord(
                base.id(), base.barId(), base.merchantId(), base.tableCode(), base.tableName(), base.tableType(),
                base.capacity(), base.allowedPassFreeGuestCount(), base.minConsumptionCent(), base.currency(),
                base.status(), base.createdAt(), base.updatedAt(),
                nullableInstant(resultSet, "pending_clear_since"),
                resultSet.getBoolean("pending_clear_overdue")
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
                resultSet.getString("deposit_settlement"),
                resultSet.getString("invitation_token"),
                resultSet.getString("invitation_signature"),
                resultSet.getObject("invitation_capacity", Integer.class),
                resultSet.getString("invitation_status"),
                nullableInstant(resultSet, "invitation_issued_at"),
                nullableInstant(resultSet, "archived_at"),
                resultSet.getString("archived_by"),
                resultSet.getString("delete_status"),
                resultSet.getString("delete_requested_by"),
                nullableInstant(resultSet, "delete_requested_at"),
                resultSet.getString("delete_reason"),
                resultSet.getString("deposit_channel"),
                nullableInstant(resultSet, "deposit_settled_at")
        );
    }

    BarReservationInvitationMemberRecord mapInvitationMember(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarReservationInvitationMemberRecord(
                resultSet.getString("id"),
                resultSet.getString("reservation_id"),
                resultSet.getString("bar_id"),
                resultSet.getString("member_identifier"),
                resultSet.getString("member_customer_id"),
                resultSet.getBoolean("is_holder"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "confirmed_at"),
                nullableInstant(resultSet, "entered_at"),
                resultSet.getString("device_id"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
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

    BarShiftRecord mapShift(ResultSet resultSet, int rowNumber) throws SQLException {
        Long declaredCash = resultSet.getObject("declared_cash_cent") == null
                ? null : resultSet.getLong("declared_cash_cent");
        Long cashDiff = resultSet.getObject("cash_diff_cent") == null
                ? null : resultSet.getLong("cash_diff_cent");
        return new BarShiftRecord(
                resultSet.getString("id"),
                resultSet.getString("project_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("bar_id"),
                resultSet.getString("cashier_user_id"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "opened_at"),
                nullableInstant(resultSet, "closed_at"),
                resultSet.getLong("opening_cash_cent"),
                declaredCash,
                resultSet.getLong("system_cash_cent"),
                resultSet.getLong("system_pos_cent"),
                resultSet.getLong("system_online_cent"),
                resultSet.getLong("system_refund_cent"),
                resultSet.getLong("system_tip_cent"),
                cashDiff,
                resultSet.getLong("cash_diff_threshold_cent"),
                resultSet.getBoolean("needs_review"),
                resultSet.getString("handover_to_user_id"),
                resultSet.getString("note"),
                resultSet.getString("opened_by_user_id"),
                resultSet.getString("closed_by_user_id"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"));
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
                resultSet.getObject("loss_threshold", Integer.class),
                resultSet.getBoolean("sold_out"),
                nullableInstant(resultSet, "sold_out_at"),
                resultSet.getString("sold_out_reason"),
                resultSet.getObject("internal_settlement_price_cent", Long.class),
                resultSet.getString("category_id"),
                resultSet.getString("image_url"),
                // V262 存档/删除范式 6 列。
                nullableInstant(resultSet, "archived_at"),
                resultSet.getString("archived_by"),
                resultSet.getString("delete_status"),
                resultSet.getString("delete_requested_by"),
                nullableInstant(resultSet, "delete_requested_at"),
                resultSet.getString("delete_reason")
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
                resultSet.getString("order_type"),
                nullableInstant(resultSet, "paid_at")
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
                resultSet.getLong("loss_volume"),
                resultSet.getBoolean("voided"),
                resultSet.getString("void_reason"),
                nullableInstant(resultSet, "voided_at"),
                resultSet.getString("voided_by_user_id"),
                resultSet.getBoolean("return_to_stock"),
                resultSet.getBoolean("is_gift"),
                nullableInstant(resultSet, "delivered_at")
        );
    }

    BarOrderItemVoidRecord mapOrderItemVoid(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarOrderItemVoidRecord(
                resultSet.getString("id"),
                resultSet.getString("order_item_id"),
                resultSet.getString("order_id"),
                resultSet.getString("bar_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("void_type"),
                resultSet.getInt("quantity"),
                resultSet.getLong("amount_cent"),
                resultSet.getBoolean("was_delivered"),
                resultSet.getBoolean("return_to_stock"),
                resultSet.getString("inventory_movement_id"),
                resultSet.getString("bill_id"),
                resultSet.getString("bill_adjustment_id"),
                resultSet.getString("reason"),
                resultSet.getString("requested_by_user_id"),
                resultSet.getBoolean("approval_required"),
                resultSet.getString("status"),
                resultSet.getString("idempotency_key"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                nullableInstant(resultSet, "created_at"),
                resultSet.getString("reviewed_by_user_id"),
                nullableInstant(resultSet, "reviewed_at"),
                resultSet.getString("review_reason"),
                // V345（BAR-VOID-A1）审批闸留痕：模式 / 结构化原因码 / 阈值快照 / 副作用落地时点。
                resultSet.getString("approval_mode"),
                resultSet.getString("reason_code"),
                nullableLong(resultSet, "high_value_threshold_cent"),
                // ↑ V345 前的历史行该列为 NULL，须还原为 null 而非 0（0 会被误读成「阈值 0=全部高价值」）。
                nullableInstant(resultSet, "effects_applied_at")
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
                nullableInstant(resultSet, "updated_at"),
                resultSet.getBoolean("reopened"),
                resultSet.getString("reopened_by_user_id"),
                nullableInstant(resultSet, "reopened_at"),
                resultSet.getString("reopen_reason"),
                resultSet.getInt("reopen_count"),
                resultSet.getLong("tip_amount_cent"),
                nullableInstant(resultSet, "paid_at")
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

    // 2c-2 提成防重：按 bill_id 读取提成计算（calculateCommission 查重守卫用）。
    BarCommissionCalculationRecord mapCommission(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        return new BarCommissionCalculationRecord(
                resultSet.getString("id"),
                resultSet.getString("bill_id"),
                resultSet.getString("sales_staff_id"),
                resultSet.getString("sales_role_type"),
                resultSet.getLong("commission_base_cent"),
                resultSet.getInt("commission_rate_bp"),
                resultSet.getLong("commission_amount_cent"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at")
        );
    }

    /** 可空 BIGINT 列：SQL NULL 还原为 Java null（getLong 会把 NULL 读成 0，语义会被歪曲）。 */
    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("bar JSON payload is not readable", exception);
        }
    }
}
