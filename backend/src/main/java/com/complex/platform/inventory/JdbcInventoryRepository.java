package com.complex.platform.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcInventoryRepository implements InventoryRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcInventoryRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public InventoryItemRecord saveInventoryItem(InventoryItemRecord item) {
        jdbcTemplate.update("""
                INSERT INTO inventory_items (
                    id, sku_code, item_name, item_type, unit, barcode, trace_required,
                    min_quantity, status, created_at, updated_at
                )
                VALUES (
                    :id, :sku_code, :item_name, :item_type, :unit, :barcode, :trace_required,
                    :min_quantity, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    sku_code = EXCLUDED.sku_code,
                    item_name = EXCLUDED.item_name,
                    item_type = EXCLUDED.item_type,
                    unit = EXCLUDED.unit,
                    barcode = EXCLUDED.barcode,
                    trace_required = EXCLUDED.trace_required,
                    min_quantity = EXCLUDED.min_quantity,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, itemParameters(item));
        return item;
    }

    @Override
    public List<InventoryItemRecord> listInventoryItems() {
        return jdbcTemplate.query("""
                SELECT id, sku_code, item_name, item_type, unit, barcode, trace_required,
                       min_quantity, status, created_at, updated_at
                  FROM inventory_items
                 ORDER BY created_at DESC, id
                """, this::mapInventoryItem);
    }

    @Override
    public Optional<InventoryItemRecord> findInventoryItemById(String id) {
        return jdbcTemplate.query("""
                SELECT id, sku_code, item_name, item_type, unit, barcode, trace_required,
                       min_quantity, status, created_at, updated_at
                  FROM inventory_items
                 WHERE id = :id
                """, Map.of("id", id), this::mapInventoryItem).stream().findFirst();
    }

    @Override
    public InventorySupplierRecord saveInventorySupplier(InventorySupplierRecord supplier) {
        jdbcTemplate.update("""
                INSERT INTO inventory_suppliers (
                    id, supplier_name, supplier_type, contact_name, contact_phone,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :supplier_name, :supplier_type, :contact_name, :contact_phone,
                    :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    supplier_name = EXCLUDED.supplier_name,
                    supplier_type = EXCLUDED.supplier_type,
                    contact_name = EXCLUDED.contact_name,
                    contact_phone = EXCLUDED.contact_phone,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, supplierParameters(supplier));
        return supplier;
    }

    @Override
    public List<InventorySupplierRecord> listInventorySuppliers() {
        return jdbcTemplate.query("""
                SELECT id, supplier_name, supplier_type, contact_name, contact_phone,
                       status, created_at, updated_at
                  FROM inventory_suppliers
                 ORDER BY created_at DESC, id
                """, this::mapInventorySupplier);
    }

    @Override
    public PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord request) {
        jdbcTemplate.update("""
                INSERT INTO inventory_purchase_requests (
                    id, request_no, supplier_id, item_id, quantity, expected_arrival_date,
                    status, evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :request_no, :supplier_id, :item_id, :quantity, :expected_arrival_date,
                    :status, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    request_no = EXCLUDED.request_no,
                    supplier_id = EXCLUDED.supplier_id,
                    item_id = EXCLUDED.item_id,
                    quantity = EXCLUDED.quantity,
                    expected_arrival_date = EXCLUDED.expected_arrival_date,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, purchaseRequestParameters(request));
        return request;
    }

    @Override
    public List<PurchaseRequestRecord> listPurchaseRequests() {
        return jdbcTemplate.query("""
                SELECT id, request_no, supplier_id, item_id, quantity, expected_arrival_date,
                       status, evidence_attachment_ids, created_at, updated_at
                  FROM inventory_purchase_requests
                 ORDER BY created_at DESC, id
                """, this::mapPurchaseRequest);
    }

    @Override
    public Optional<StockBalanceRecord> findStockBalance(String itemId, String locationId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at
                  FROM inventory_stock_balances
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                """, Map.of("item_id", itemId, "location_id", locationId), this::mapStockBalance).stream().findFirst();
    }

    @Override
    public StockBalanceRecord saveStockBalance(StockBalanceRecord balance) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (
                    id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at
                )
                VALUES (
                    :id, :item_id, :location_id, :quantity, :reserved_quantity, :available_quantity, :updated_at
                )
                ON CONFLICT (item_id, location_id) DO UPDATE SET
                    quantity = EXCLUDED.quantity,
                    reserved_quantity = EXCLUDED.reserved_quantity,
                    available_quantity = EXCLUDED.available_quantity,
                    updated_at = EXCLUDED.updated_at
                """, stockBalanceParameters(balance));
        return balance;
    }

    @Override
    public List<StockBalanceRecord> listStockBalances() {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at
                  FROM inventory_stock_balances
                 ORDER BY updated_at DESC, id
                """, this::mapStockBalance);
    }

    @Override
    public InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord batch) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_batches (
                    id, item_id, location_id, batch_no, supplier_id, quantity,
                    expire_at, status, evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :item_id, :location_id, :batch_no, :supplier_id, :quantity,
                    :expire_at, :status, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    item_id = EXCLUDED.item_id,
                    location_id = EXCLUDED.location_id,
                    batch_no = EXCLUDED.batch_no,
                    supplier_id = EXCLUDED.supplier_id,
                    quantity = EXCLUDED.quantity,
                    expire_at = EXCLUDED.expire_at,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, stockBatchParameters(batch));
        return batch;
    }

    @Override
    public List<InventoryStockBatchRecord> listStockBatches() {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, batch_no, supplier_id, quantity,
                       expire_at, status, evidence_attachment_ids, created_at, updated_at
                  FROM inventory_stock_batches
                 ORDER BY expire_at ASC NULLS LAST, created_at DESC, id
                """, this::mapStockBatch);
    }

    @Override
    public InventoryRiskAlertRecord saveRiskAlert(InventoryRiskAlertRecord alert) {
        jdbcTemplate.update("""
                INSERT INTO inventory_risk_alerts (
                    id, alert_type, severity, item_id, location_id, batch_id,
                    available_quantity, threshold_quantity, expire_at, status,
                    operator_user_id, created_at
                )
                VALUES (
                    :id, :alert_type, :severity, :item_id, :location_id, :batch_id,
                    :available_quantity, :threshold_quantity, :expire_at, :status,
                    :operator_user_id, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    alert_type = EXCLUDED.alert_type,
                    severity = EXCLUDED.severity,
                    item_id = EXCLUDED.item_id,
                    location_id = EXCLUDED.location_id,
                    batch_id = EXCLUDED.batch_id,
                    available_quantity = EXCLUDED.available_quantity,
                    threshold_quantity = EXCLUDED.threshold_quantity,
                    expire_at = EXCLUDED.expire_at,
                    status = EXCLUDED.status,
                    operator_user_id = EXCLUDED.operator_user_id
                """, riskAlertParameters(alert));
        return alert;
    }

    @Override
    public List<InventoryRiskAlertRecord> listRiskAlerts() {
        return jdbcTemplate.query("""
                SELECT id, alert_type, severity, item_id, location_id, batch_id,
                       available_quantity, threshold_quantity, expire_at, status,
                       operator_user_id, created_at
                  FROM inventory_risk_alerts
                 ORDER BY created_at DESC, id
                """, this::mapRiskAlert);
    }

    @Override
    public SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord reconciliation) {
        jdbcTemplate.update("""
                INSERT INTO inventory_supplier_reconciliations (
                    id, supplier_id, period_start, period_end, expected_amount_cent,
                    actual_amount_cent, difference_amount_cent, status,
                    evidence_attachment_ids, operator_user_id, created_at, updated_at
                )
                VALUES (
                    :id, :supplier_id, :period_start, :period_end, :expected_amount_cent,
                    :actual_amount_cent, :difference_amount_cent, :status,
                    CAST(:evidence_attachment_ids AS jsonb), :operator_user_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    supplier_id = EXCLUDED.supplier_id,
                    period_start = EXCLUDED.period_start,
                    period_end = EXCLUDED.period_end,
                    expected_amount_cent = EXCLUDED.expected_amount_cent,
                    actual_amount_cent = EXCLUDED.actual_amount_cent,
                    difference_amount_cent = EXCLUDED.difference_amount_cent,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    operator_user_id = EXCLUDED.operator_user_id,
                    updated_at = EXCLUDED.updated_at
                """, supplierReconciliationParameters(reconciliation));
        return reconciliation;
    }

    @Override
    public List<SupplierReconciliationRecord> listSupplierReconciliations() {
        return jdbcTemplate.query("""
                SELECT id, supplier_id, period_start, period_end, expected_amount_cent,
                       actual_amount_cent, difference_amount_cent, status,
                       evidence_attachment_ids, operator_user_id, created_at, updated_at
                  FROM inventory_supplier_reconciliations
                 ORDER BY period_end DESC, id
                """, this::mapSupplierReconciliation);
    }

    @Override
    public StockMovementRecord saveStockMovement(StockMovementRecord movement) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_movements (
                    id, movement_type, item_id, source_location_id, target_location_id, quantity,
                    business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                    status, created_at
                )
                VALUES (
                    :id, :movement_type, :item_id, :source_location_id, :target_location_id, :quantity,
                    :business_type, :business_id, :trace_code, :operator_id, CAST(:evidence_attachment_ids AS jsonb),
                    :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    movement_type = EXCLUDED.movement_type,
                    item_id = EXCLUDED.item_id,
                    source_location_id = EXCLUDED.source_location_id,
                    target_location_id = EXCLUDED.target_location_id,
                    quantity = EXCLUDED.quantity,
                    business_type = EXCLUDED.business_type,
                    business_id = EXCLUDED.business_id,
                    trace_code = EXCLUDED.trace_code,
                    operator_id = EXCLUDED.operator_id,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    status = EXCLUDED.status
                """, stockMovementParameters(movement));
        return movement;
    }

    @Override
    public List<StockMovementRecord> listStockMovements() {
        return jdbcTemplate.query("""
                SELECT id, movement_type, item_id, source_location_id, target_location_id, quantity,
                       business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                       status, created_at
                  FROM inventory_stock_movements
                 ORDER BY created_at DESC, id
                """, this::mapStockMovement);
    }

    @Override
    public StockAdjustmentRecord saveStockAdjustment(StockAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_adjustments (
                    id, item_id, location_id, adjustment_type, quantity, reason,
                    evidence_attachment_ids, status, created_at
                )
                VALUES (
                    :id, :item_id, :location_id, :adjustment_type, :quantity, :reason,
                    CAST(:evidence_attachment_ids AS jsonb), :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    item_id = EXCLUDED.item_id,
                    location_id = EXCLUDED.location_id,
                    adjustment_type = EXCLUDED.adjustment_type,
                    quantity = EXCLUDED.quantity,
                    reason = EXCLUDED.reason,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    status = EXCLUDED.status
                """, stockAdjustmentParameters(adjustment));
        return adjustment;
    }

    @Override
    public SavedBottleRecord saveSavedBottle(SavedBottleRecord savedBottle) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottles (
                    id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity,
                    remaining_quantity, unit, expire_at, status, evidence_attachment_ids,
                    created_at, updated_at
                )
                VALUES (
                    :id, :customer_id, :bar_project_id, :item_id, :bottle_name, :initial_quantity,
                    :remaining_quantity, :unit, :expire_at, :status, CAST(:evidence_attachment_ids AS jsonb),
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    customer_id = EXCLUDED.customer_id,
                    bar_project_id = EXCLUDED.bar_project_id,
                    item_id = EXCLUDED.item_id,
                    bottle_name = EXCLUDED.bottle_name,
                    initial_quantity = EXCLUDED.initial_quantity,
                    remaining_quantity = EXCLUDED.remaining_quantity,
                    unit = EXCLUDED.unit,
                    expire_at = EXCLUDED.expire_at,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, savedBottleParameters(savedBottle));
        return savedBottle;
    }

    @Override
    public Optional<SavedBottleRecord> findSavedBottleById(String id) {
        return jdbcTemplate.query("""
                SELECT id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity,
                       remaining_quantity, unit, expire_at, status, evidence_attachment_ids,
                       created_at, updated_at
                  FROM saved_bottles
                 WHERE id = :id
                """, Map.of("id", id), this::mapSavedBottle).stream().findFirst();
    }

    @Override
    public List<SavedBottleRecord> listSavedBottles() {
        return jdbcTemplate.query("""
                SELECT id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity,
                       remaining_quantity, unit, expire_at, status, evidence_attachment_ids,
                       created_at, updated_at
                  FROM saved_bottles
                 ORDER BY created_at DESC, id
                """, this::mapSavedBottle);
    }

    @Override
    public SavedBottleClaimRecord saveSavedBottleClaim(SavedBottleClaimRecord claim) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_claims (
                    id, saved_bottle_id, claim_quantity, claim_staff_id, claim_terminal_type,
                    claim_device_id, status, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :saved_bottle_id, :claim_quantity, :claim_staff_id, :claim_terminal_type,
                    :claim_device_id, :status, CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    saved_bottle_id = EXCLUDED.saved_bottle_id,
                    claim_quantity = EXCLUDED.claim_quantity,
                    claim_staff_id = EXCLUDED.claim_staff_id,
                    claim_terminal_type = EXCLUDED.claim_terminal_type,
                    claim_device_id = EXCLUDED.claim_device_id,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, savedBottleClaimParameters(claim));
        return claim;
    }

    @Override
    public TraceHandoverRecord saveTraceHandover(TraceHandoverRecord handover) {
        jdbcTemplate.update("""
                INSERT INTO inventory_trace_handovers (
                    id, trace_code, item_id, from_staff_id, to_staff_id, device_id,
                    handover_result, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :trace_code, :item_id, :from_staff_id, :to_staff_id, :device_id,
                    :handover_result, CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    trace_code = EXCLUDED.trace_code,
                    item_id = EXCLUDED.item_id,
                    from_staff_id = EXCLUDED.from_staff_id,
                    to_staff_id = EXCLUDED.to_staff_id,
                    device_id = EXCLUDED.device_id,
                    handover_result = EXCLUDED.handover_result,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, traceHandoverParameters(handover));
        return handover;
    }

    private MapSqlParameterSource itemParameters(InventoryItemRecord item) {
        return new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("sku_code", item.skuCode())
                .addValue("item_name", item.itemName())
                .addValue("item_type", item.itemType())
                .addValue("unit", item.unit())
                .addValue("barcode", item.barcode())
                .addValue("trace_required", item.traceRequired())
                .addValue("min_quantity", item.minQuantity())
                .addValue("status", item.status())
                .addValue("created_at", offset(item.createdAt()))
                .addValue("updated_at", offset(item.updatedAt()));
    }

    private MapSqlParameterSource supplierParameters(InventorySupplierRecord supplier) {
        return new MapSqlParameterSource()
                .addValue("id", supplier.id())
                .addValue("supplier_name", supplier.supplierName())
                .addValue("supplier_type", supplier.supplierType())
                .addValue("contact_name", supplier.contactName())
                .addValue("contact_phone", supplier.contactPhone())
                .addValue("status", supplier.status())
                .addValue("created_at", offset(supplier.createdAt()))
                .addValue("updated_at", offset(supplier.updatedAt()));
    }

    private MapSqlParameterSource purchaseRequestParameters(PurchaseRequestRecord request) {
        return new MapSqlParameterSource()
                .addValue("id", request.id())
                .addValue("request_no", request.requestNo())
                .addValue("supplier_id", request.supplierId())
                .addValue("item_id", request.itemId())
                .addValue("quantity", request.quantity())
                .addValue("expected_arrival_date", request.expectedArrivalDate())
                .addValue("status", request.status())
                .addValue("evidence_attachment_ids", writeList(request.evidenceAttachmentIds()))
                .addValue("created_at", offset(request.createdAt()))
                .addValue("updated_at", offset(request.updatedAt()));
    }

    private MapSqlParameterSource stockBalanceParameters(StockBalanceRecord balance) {
        return new MapSqlParameterSource()
                .addValue("id", balance.id())
                .addValue("item_id", balance.itemId())
                .addValue("location_id", balance.locationId())
                .addValue("quantity", balance.quantity())
                .addValue("reserved_quantity", balance.reservedQuantity())
                .addValue("available_quantity", balance.availableQuantity())
                .addValue("updated_at", offset(balance.updatedAt()));
    }

    private MapSqlParameterSource stockBatchParameters(InventoryStockBatchRecord batch) {
        return new MapSqlParameterSource()
                .addValue("id", batch.id())
                .addValue("item_id", batch.itemId())
                .addValue("location_id", batch.locationId())
                .addValue("batch_no", batch.batchNo())
                .addValue("supplier_id", batch.supplierId())
                .addValue("quantity", batch.quantity())
                .addValue("expire_at", offset(batch.expireAt()))
                .addValue("status", batch.status())
                .addValue("evidence_attachment_ids", writeList(batch.evidenceAttachmentIds()))
                .addValue("created_at", offset(batch.createdAt()))
                .addValue("updated_at", offset(batch.updatedAt()));
    }

    private MapSqlParameterSource riskAlertParameters(InventoryRiskAlertRecord alert) {
        return new MapSqlParameterSource()
                .addValue("id", alert.id())
                .addValue("alert_type", alert.alertType())
                .addValue("severity", alert.severity())
                .addValue("item_id", alert.itemId())
                .addValue("location_id", alert.locationId())
                .addValue("batch_id", alert.batchId())
                .addValue("available_quantity", alert.availableQuantity())
                .addValue("threshold_quantity", alert.thresholdQuantity())
                .addValue("expire_at", offset(alert.expireAt()))
                .addValue("status", alert.status())
                .addValue("operator_user_id", alert.operatorUserId())
                .addValue("created_at", offset(alert.createdAt()));
    }

    private MapSqlParameterSource supplierReconciliationParameters(SupplierReconciliationRecord reconciliation) {
        return new MapSqlParameterSource()
                .addValue("id", reconciliation.id())
                .addValue("supplier_id", reconciliation.supplierId())
                .addValue("period_start", reconciliation.periodStart())
                .addValue("period_end", reconciliation.periodEnd())
                .addValue("expected_amount_cent", reconciliation.expectedAmountCent())
                .addValue("actual_amount_cent", reconciliation.actualAmountCent())
                .addValue("difference_amount_cent", reconciliation.differenceAmountCent())
                .addValue("status", reconciliation.status())
                .addValue("evidence_attachment_ids", writeList(reconciliation.evidenceAttachmentIds()))
                .addValue("operator_user_id", reconciliation.operatorUserId())
                .addValue("created_at", offset(reconciliation.createdAt()))
                .addValue("updated_at", offset(reconciliation.updatedAt()));
    }

    private MapSqlParameterSource stockMovementParameters(StockMovementRecord movement) {
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
                .addValue("evidence_attachment_ids", writeList(movement.evidenceAttachmentIds()))
                .addValue("status", movement.status())
                .addValue("created_at", offset(movement.createdAt()));
    }

    private MapSqlParameterSource stockAdjustmentParameters(StockAdjustmentRecord adjustment) {
        return new MapSqlParameterSource()
                .addValue("id", adjustment.id())
                .addValue("item_id", adjustment.itemId())
                .addValue("location_id", adjustment.locationId())
                .addValue("adjustment_type", adjustment.adjustmentType())
                .addValue("quantity", adjustment.quantity())
                .addValue("reason", adjustment.reason())
                .addValue("evidence_attachment_ids", writeList(adjustment.evidenceAttachmentIds()))
                .addValue("status", adjustment.status())
                .addValue("created_at", offset(adjustment.createdAt()));
    }

    private MapSqlParameterSource savedBottleParameters(SavedBottleRecord savedBottle) {
        return new MapSqlParameterSource()
                .addValue("id", savedBottle.id())
                .addValue("customer_id", savedBottle.customerId())
                .addValue("bar_project_id", savedBottle.barProjectId())
                .addValue("item_id", savedBottle.itemId())
                .addValue("bottle_name", savedBottle.bottleName())
                .addValue("initial_quantity", savedBottle.initialQuantity())
                .addValue("remaining_quantity", savedBottle.remainingQuantity())
                .addValue("unit", savedBottle.unit())
                .addValue("expire_at", offset(savedBottle.expireAt()))
                .addValue("status", savedBottle.status())
                .addValue("evidence_attachment_ids", writeList(savedBottle.evidenceAttachmentIds()))
                .addValue("created_at", offset(savedBottle.createdAt()))
                .addValue("updated_at", offset(savedBottle.updatedAt()));
    }

    private MapSqlParameterSource savedBottleClaimParameters(SavedBottleClaimRecord claim) {
        return new MapSqlParameterSource()
                .addValue("id", claim.id())
                .addValue("saved_bottle_id", claim.savedBottleId())
                .addValue("claim_quantity", claim.claimQuantity())
                .addValue("claim_staff_id", claim.claimStaffId())
                .addValue("claim_terminal_type", claim.claimTerminalType())
                .addValue("claim_device_id", claim.claimDeviceId())
                .addValue("status", claim.status())
                .addValue("evidence_attachment_ids", writeList(claim.evidenceAttachmentIds()))
                .addValue("created_at", offset(claim.createdAt()));
    }

    private MapSqlParameterSource traceHandoverParameters(TraceHandoverRecord handover) {
        return new MapSqlParameterSource()
                .addValue("id", handover.id())
                .addValue("trace_code", handover.traceCode())
                .addValue("item_id", handover.itemId())
                .addValue("from_staff_id", handover.fromStaffId())
                .addValue("to_staff_id", handover.toStaffId())
                .addValue("device_id", handover.deviceId())
                .addValue("handover_result", handover.handoverResult())
                .addValue("evidence_attachment_ids", writeList(handover.evidenceAttachmentIds()))
                .addValue("created_at", offset(handover.createdAt()));
    }

    private InventoryItemRecord mapInventoryItem(ResultSet resultSet, int rowNumber) throws SQLException {
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
                instant(resultSet, "updated_at")
        );
    }

    private InventorySupplierRecord mapInventorySupplier(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InventorySupplierRecord(
                resultSet.getString("id"),
                resultSet.getString("supplier_name"),
                resultSet.getString("supplier_type"),
                resultSet.getString("contact_name"),
                resultSet.getString("contact_phone"),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    private PurchaseRequestRecord mapPurchaseRequest(ResultSet resultSet, int rowNumber) throws SQLException {
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
                instant(resultSet, "updated_at")
        );
    }

    private StockBalanceRecord mapStockBalance(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StockBalanceRecord(
                resultSet.getString("id"),
                resultSet.getString("item_id"),
                resultSet.getString("location_id"),
                resultSet.getInt("quantity"),
                resultSet.getInt("reserved_quantity"),
                resultSet.getInt("available_quantity"),
                instant(resultSet, "updated_at")
        );
    }

    private InventoryStockBatchRecord mapStockBatch(ResultSet resultSet, int rowNumber) throws SQLException {
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
                instant(resultSet, "updated_at")
        );
    }

    private InventoryRiskAlertRecord mapRiskAlert(ResultSet resultSet, int rowNumber) throws SQLException {
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
                instant(resultSet, "created_at")
        );
    }

    private SupplierReconciliationRecord mapSupplierReconciliation(ResultSet resultSet, int rowNumber) throws SQLException {
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
                instant(resultSet, "updated_at")
        );
    }

    private StockMovementRecord mapStockMovement(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StockMovementRecord(
                resultSet.getString("id"),
                resultSet.getString("movement_type"),
                resultSet.getString("item_id"),
                resultSet.getString("source_location_id"),
                resultSet.getString("target_location_id"),
                resultSet.getInt("quantity"),
                resultSet.getString("business_type"),
                resultSet.getString("business_id"),
                resultSet.getString("trace_code"),
                resultSet.getString("operator_id"),
                readList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("status"),
                instant(resultSet, "created_at")
        );
    }

    private SavedBottleRecord mapSavedBottle(ResultSet resultSet, int rowNumber) throws SQLException {
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
                instant(resultSet, "updated_at")
        );
    }

    private String writeList(List<String> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("inventory attachment ids are not serializable", exception);
        }
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

    private OffsetDateTime offset(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
