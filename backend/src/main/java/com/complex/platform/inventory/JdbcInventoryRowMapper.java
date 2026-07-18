package com.complex.platform.inventory;

import com.complex.platform.common.crypto.PiiCipher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

final class JdbcInventoryRowMapper {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final PiiCipher piiCipher;

    JdbcInventoryRowMapper(ObjectMapper objectMapper, PiiCipher piiCipher) {
        this.objectMapper = objectMapper;
        this.piiCipher = piiCipher;
    }

    InventoryItemRecord mapInventoryItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InventoryItemRecord(
                resultSet.getString("id"),
                resultSet.getString("sku_code"),
                resultSet.getString("item_name"),
                resultSet.getString("item_type"),
                resultSet.getString("unit"),
                resultSet.getString("barcode"),
                resultSet.getBoolean("trace_required"),
                resultSet.getInt("min_quantity"),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                resultSet.getString("merchant_id"),
                resultSet.getObject("high_value_threshold_cents", Long.class)
        );
    }

    StockAdjustmentRecord mapStockAdjustment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StockAdjustmentRecord(
                resultSet.getString("id"),
                resultSet.getString("item_id"),
                resultSet.getString("location_id"),
                resultSet.getString("adjustment_type"),
                resultSet.getInt("quantity"),
                resultSet.getString("reason"),
                readList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                resultSet.getString("merchant_id"),
                resultSet.getObject("unit_cost_cents", Long.class)
        );
    }

    InventorySupplierRecord mapInventorySupplier(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InventorySupplierRecord(
                resultSet.getString("id"),
                resultSet.getString("supplier_name"),
                resultSet.getString("supplier_type"),
                resultSet.getString("contact_name"),
                resultSet.getString("contact_phone"),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                resultSet.getString("merchant_id")
        );
    }

    PurchaseRequestRecord mapPurchaseRequest(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PurchaseRequestRecord(
                resultSet.getString("id"),
                resultSet.getString("request_no"),
                resultSet.getString("supplier_id"),
                resultSet.getString("item_id"),
                resultSet.getInt("quantity"),
                resultSet.getObject("expected_arrival_date", java.time.LocalDate.class),
                resultSet.getString("status"),
                readList(resultSet.getString("evidence_attachment_ids")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                resultSet.getString("merchant_id"),
                resultSet.getString("supply_contract_id"),
                resultSet.getString("approved_by"),
                instantOrNull(resultSet, "ordered_at"),
                resultSet.getObject("actual_arrival_date", java.time.LocalDate.class),
                instantOrNull(resultSet, "received_at"),
                resultSet.getString("received_by")
        );
    }

    PurchaseRequestEventRecord mapPurchaseRequestEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PurchaseRequestEventRecord(
                resultSet.getString("id"),
                resultSet.getString("request_id"),
                resultSet.getString("from_status"),
                resultSet.getString("to_status"),
                resultSet.getString("operator_user_id"),
                resultSet.getString("note"),
                instant(resultSet, "created_at")
        );
    }

    StockBalanceRecord mapStockBalance(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StockBalanceRecord(
                resultSet.getString("id"),
                resultSet.getString("item_id"),
                resultSet.getString("location_id"),
                resultSet.getInt("quantity"),
                resultSet.getInt("reserved_quantity"),
                resultSet.getInt("available_quantity"),
                instant(resultSet, "updated_at"),
                resultSet.getString("merchant_id"),
                resultSet.getString("warehouse_id")
        );
    }

    SupplyContractRecord mapSupplyContract(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SupplyContractRecord(
                resultSet.getString("id"),
                resultSet.getString("contract_no"),
                resultSet.getString("supplier_id"),
                resultSet.getString("project_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("contract_mode"),
                resultSet.getString("supply_scope"),
                resultSet.getString("price_terms"),
                resultSet.getObject("payment_term_days", Integer.class),
                resultSet.getObject("valid_from", java.time.LocalDate.class),
                resultSet.getObject("valid_to", java.time.LocalDate.class),
                resultSet.getString("summary"),
                readList(resultSet.getString("purchasable_scope")),
                resultSet.getString("approval_status"),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    WarehouseRecord mapWarehouse(ResultSet resultSet, int rowNumber) throws SQLException {
        return new WarehouseRecord(
                resultSet.getString("id"),
                resultSet.getString("project_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("warehouse_type"),
                resultSet.getString("name"),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    SupplierQualificationRecord mapSupplierQualification(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SupplierQualificationRecord(
                resultSet.getString("id"),
                resultSet.getString("supplier_id"),
                resultSet.getString("qualification_type"),
                resultSet.getString("qualification_no"),
                resultSet.getString("issued_by"),
                resultSet.getObject("valid_from", java.time.LocalDate.class),
                resultSet.getObject("valid_to", java.time.LocalDate.class),
                readList(resultSet.getString("attachment_ids")),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                resultSet.getString("merchant_id")
        );
    }

    SupplierSettlementMethodRecord mapSupplierSettlementMethod(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SupplierSettlementMethodRecord(
                resultSet.getString("id"),
                resultSet.getString("supplier_id"),
                resultSet.getString("settlement_method"),
                resultSet.getObject("payment_term_days", Integer.class),
                resultSet.getString("applicable_category"),
                resultSet.getBoolean("is_default"),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                resultSet.getString("merchant_id")
        );
    }

    InventoryStockBatchRecord mapStockBatch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InventoryStockBatchRecord(
                resultSet.getString("id"),
                resultSet.getString("item_id"),
                resultSet.getString("location_id"),
                resultSet.getString("batch_no"),
                resultSet.getString("supplier_id"),
                resultSet.getInt("quantity"),
                instantOrNull(resultSet, "expire_at"),
                resultSet.getString("status"),
                readList(resultSet.getString("evidence_attachment_ids")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                resultSet.getString("merchant_id")
        );
    }

    InventoryRiskAlertRecord mapRiskAlert(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InventoryRiskAlertRecord(
                resultSet.getString("id"),
                resultSet.getString("alert_type"),
                resultSet.getString("severity"),
                resultSet.getString("item_id"),
                resultSet.getString("location_id"),
                resultSet.getString("batch_id"),
                nullableInteger(resultSet, "available_quantity"),
                nullableInteger(resultSet, "threshold_quantity"),
                instantOrNull(resultSet, "expire_at"),
                resultSet.getString("status"),
                resultSet.getString("operator_user_id"),
                instant(resultSet, "created_at"),
                resultSet.getString("merchant_id")
        );
    }

    SupplierReconciliationRecord mapSupplierReconciliation(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SupplierReconciliationRecord(
                resultSet.getString("id"),
                resultSet.getString("supplier_id"),
                resultSet.getObject("period_start", java.time.LocalDate.class),
                resultSet.getObject("period_end", java.time.LocalDate.class),
                resultSet.getLong("expected_amount_cent"),
                resultSet.getLong("actual_amount_cent"),
                resultSet.getLong("difference_amount_cent"),
                resultSet.getString("status"),
                readList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("operator_user_id"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                resultSet.getString("merchant_id")
        );
    }

    StockMovementRecord mapStockMovement(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StockMovementRecord(
                resultSet.getString("id"),
                resultSet.getString("movement_type"),
                resultSet.getString("item_id"),
                resultSet.getString("source_location_id"),
                resultSet.getString("target_location_id"),
                resultSet.getInt("quantity"),
                // V296 成本列：NULL=无成本基准，须保持 null 语义（不得折成 0）。
                resultSet.getObject("unit_cost_cent", Long.class),
                resultSet.getObject("total_cost_cent", Long.class),
                resultSet.getString("business_type"),
                resultSet.getString("business_id"),
                resultSet.getString("trace_code"),
                resultSet.getString("operator_id"),
                readList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                resultSet.getString("merchant_id")
        );
    }

    SavedBottleRecord mapSavedBottle(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SavedBottleRecord(
                resultSet.getString("id"),
                resultSet.getString("customer_id"),
                resultSet.getString("bar_project_id"),
                resultSet.getString("item_id"),
                resultSet.getString("bottle_name"),
                resultSet.getInt("initial_quantity"),
                resultSet.getInt("remaining_quantity"),
                resultSet.getString("unit"),
                instantOrNull(resultSet, "expire_at"),
                resultSet.getString("status"),
                readList(resultSet.getString("evidence_attachment_ids")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                resultSet.getString("merchant_id"),
                resultSet.getBoolean("is_high_value"),
                resultSet.getObject("high_value_threshold_cents", Long.class),
                resultSet.getString("bottle_label_value"),
                resultSet.getString("customer_name_or_nickname"),
                piiCipher.decrypt(resultSet.getString("customer_phone")),
                resultSet.getString("customer_phone_hash"),
                readList(resultSet.getString("registration_photo_refs")),
                resultSet.getString("manual_note"),
                instantOrNull(resultSet, "transferred_at"),
                resultSet.getString("transfer_to_name_or_nickname"),
                piiCipher.decrypt(resultSet.getString("transfer_to_phone")),
                resultSet.getString("transfer_to_phone_hash")
        );
    }

    TraceHandoverRecord mapTraceHandover(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TraceHandoverRecord(
                resultSet.getString("id"),
                resultSet.getString("trace_code"),
                resultSet.getString("item_id"),
                resultSet.getString("from_staff_id"),
                resultSet.getString("to_staff_id"),
                resultSet.getString("device_id"),
                resultSet.getString("handover_result"),
                resultSet.getString("overall_status"),
                resultSet.getString("current_segment"),
                readList(resultSet.getString("evidence_attachment_ids")),
                instant(resultSet, "created_at"),
                resultSet.getString("merchant_id")
        );
    }

    TraceHandoverStepRecord mapTraceHandoverStep(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TraceHandoverStepRecord(
                resultSet.getString("id"),
                resultSet.getString("handover_id"),
                resultSet.getString("segment"),
                resultSet.getInt("seq"),
                resultSet.getString("actor_type"),
                resultSet.getString("operator_user_id"),
                resultSet.getString("robot_task_ref"),
                instantOrNull(resultSet, "scan_at"),
                resultSet.getString("step_status"),
                resultSet.getString("note"),
                instant(resultSet, "created_at")
        );
    }

    TraceHandoverExceptionRecord mapTraceHandoverException(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TraceHandoverExceptionRecord(
                resultSet.getString("id"),
                resultSet.getString("handover_id"),
                resultSet.getString("segment"),
                resultSet.getString("exception_type"),
                resultSet.getString("review_status"),
                resultSet.getString("reviewer_user_id"),
                resultSet.getString("reason"),
                instant(resultSet, "created_at"),
                instantOrNull(resultSet, "reviewed_at")
        );
    }

    InventoryHighValueReviewRecord mapHighValueReview(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InventoryHighValueReviewRecord(
                resultSet.getString("id"),
                resultSet.getString("subject_type"),
                resultSet.getString("subject_id"),
                readList(resultSet.getString("photo_attachment_ids")),
                readList(resultSet.getString("video_attachment_ids")),
                resultSet.getString("review_status"),
                resultSet.getString("reviewer_user_id"),
                resultSet.getString("reason"),
                instant(resultSet, "created_at"),
                instantOrNull(resultSet, "reviewed_at"),
                resultSet.getString("merchant_id")
        );
    }

    // ── V108/V126：盘点单 + 库存差异明细。 ──
    StocktakeRecord mapStocktake(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StocktakeRecord(
                resultSet.getString("id"),
                resultSet.getString("stocktake_no"),
                resultSet.getString("warehouse_id"),
                resultSet.getString("merchant_id"),
                resultSet.getObject("business_date", java.time.LocalDate.class),
                resultSet.getString("status"),
                resultSet.getString("remark"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    InventoryVarianceRecord mapInventoryVariance(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InventoryVarianceRecord(
                resultSet.getString("id"),
                resultSet.getString("stocktake_id"),
                resultSet.getString("item_id"),
                resultSet.getString("batch_no"),
                resultSet.getBigDecimal("book_qty"),
                resultSet.getBigDecimal("counted_qty"),
                resultSet.getBigDecimal("variance_qty"),
                resultSet.getLong("variance_amount_cent"),
                resultSet.getString("reason"),
                resultSet.getString("supplier_id"),
                resultSet.getString("review_status"),
                resultSet.getString("reviewed_by"),
                instantOrNull(resultSet, "reviewed_at"),
                resultSet.getString("posted_ar_ap_item_id"),
                resultSet.getString("merchant_id"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    // ── V128 / REQ-39：溯源码主数据。 ──
    InventoryTraceCodeRecord mapTraceCode(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InventoryTraceCodeRecord(
                resultSet.getString("id"),
                resultSet.getString("trace_code"),
                resultSet.getString("item_id"),
                resultSet.getString("batch_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("status"),
                resultSet.getString("bound_ref"),
                instantOrNull(resultSet, "printed_at"),
                resultSet.getString("created_by"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("inventory attachment ids are unreadable", exception);
        }
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }

    private Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }
}
