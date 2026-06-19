package com.complex.platform.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcInventoryRowMapper rowMapper;

    JdbcInventoryRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.rowMapper = new JdbcInventoryRowMapper(this.objectMapper);
    }

    @Override
    public InventoryItemRecord saveInventoryItem(InventoryItemRecord item) {
        jdbcTemplate.update("""
                INSERT INTO inventory_items (
                    id, sku_code, item_name, item_type, unit, barcode, trace_required,
                    min_quantity, status, created_at, updated_at, merchant_id, high_value_threshold_cents
                )
                VALUES (
                    :id, :sku_code, :item_name, :item_type, :unit, :barcode, :trace_required,
                    :min_quantity, :status, :created_at, :updated_at, :merchant_id, :high_value_threshold_cents
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
                    updated_at = EXCLUDED.updated_at,
                    high_value_threshold_cents = EXCLUDED.high_value_threshold_cents
                """, itemParameters(item));
        return item;
    }

    @Override
    public List<InventoryItemRecord> listInventoryItems() {
        return jdbcTemplate.query("""
                SELECT id, sku_code, item_name, item_type, unit, barcode, trace_required,
                       min_quantity, status, created_at, updated_at, merchant_id, high_value_threshold_cents
                  FROM inventory_items
                 ORDER BY created_at DESC, id
                """, rowMapper::mapInventoryItem);
    }

    @Override
    public Optional<InventoryItemRecord> findInventoryItemById(String id) {
        return jdbcTemplate.query("""
                SELECT id, sku_code, item_name, item_type, unit, barcode, trace_required,
                       min_quantity, status, created_at, updated_at, merchant_id, high_value_threshold_cents
                  FROM inventory_items
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapInventoryItem).stream().findFirst();
    }

    @Override
    public InventorySupplierRecord saveInventorySupplier(InventorySupplierRecord supplier) {
        jdbcTemplate.update("""
                INSERT INTO inventory_suppliers (
                    id, supplier_name, supplier_type, contact_name, contact_phone,
                    status, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :supplier_name, :supplier_type, :contact_name, :contact_phone,
                    :status, :created_at, :updated_at, :merchant_id
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
                       status, created_at, updated_at, merchant_id
                  FROM inventory_suppliers
                 ORDER BY created_at DESC, id
                """, rowMapper::mapInventorySupplier);
    }

    @Override
    public PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord request) {
        jdbcTemplate.update("""
                INSERT INTO inventory_purchase_requests (
                    id, request_no, supplier_id, item_id, quantity, expected_arrival_date,
                    status, evidence_attachment_ids, created_at, updated_at, merchant_id,
                    supply_contract_id, approved_by, ordered_at, actual_arrival_date, received_at, received_by
                )
                VALUES (
                    :id, :request_no, :supplier_id, :item_id, :quantity, :expected_arrival_date,
                    :status, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at, :merchant_id,
                    :supply_contract_id, :approved_by, :ordered_at, :actual_arrival_date, :received_at, :received_by
                )
                ON CONFLICT (id) DO UPDATE SET
                    request_no = EXCLUDED.request_no,
                    supplier_id = EXCLUDED.supplier_id,
                    item_id = EXCLUDED.item_id,
                    quantity = EXCLUDED.quantity,
                    expected_arrival_date = EXCLUDED.expected_arrival_date,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at,
                    supply_contract_id = EXCLUDED.supply_contract_id,
                    approved_by = EXCLUDED.approved_by,
                    ordered_at = EXCLUDED.ordered_at,
                    actual_arrival_date = EXCLUDED.actual_arrival_date,
                    received_at = EXCLUDED.received_at,
                    received_by = EXCLUDED.received_by
                """, purchaseRequestParameters(request));
        return request;
    }

    @Override
    public List<PurchaseRequestRecord> listPurchaseRequests() {
        return jdbcTemplate.query("""
                SELECT id, request_no, supplier_id, item_id, quantity, expected_arrival_date,
                       status, evidence_attachment_ids, created_at, updated_at, merchant_id,
                       supply_contract_id, approved_by, ordered_at, actual_arrival_date, received_at, received_by
                  FROM inventory_purchase_requests
                 ORDER BY created_at DESC, id
                """, rowMapper::mapPurchaseRequest);
    }

    // R5 T6 采购周期状态机：行锁读取 + 流转留痕事件。
    @Override
    public Optional<PurchaseRequestRecord> findPurchaseRequestByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, request_no, supplier_id, item_id, quantity, expected_arrival_date,
                       status, evidence_attachment_ids, created_at, updated_at, merchant_id,
                       supply_contract_id, approved_by, ordered_at, actual_arrival_date, received_at, received_by
                  FROM inventory_purchase_requests
                 WHERE id = :id
                 FOR UPDATE
                """, Map.of("id", id), rowMapper::mapPurchaseRequest).stream().findFirst();
    }

    @Override
    public PurchaseRequestEventRecord savePurchaseRequestEvent(PurchaseRequestEventRecord event) {
        jdbcTemplate.update("""
                INSERT INTO inventory_purchase_request_events (
                    id, request_id, from_status, to_status, operator_user_id, note, created_at
                )
                VALUES (:id, :request_id, :from_status, :to_status, :operator_user_id, :note, :created_at)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", event.id())
                .addValue("request_id", event.requestId())
                .addValue("from_status", event.fromStatus())
                .addValue("to_status", event.toStatus())
                .addValue("operator_user_id", event.operatorUserId())
                .addValue("note", event.note())
                .addValue("created_at", offset(event.createdAt())));
        return event;
    }

    @Override
    public List<PurchaseRequestEventRecord> listPurchaseRequestEvents(String requestId) {
        return jdbcTemplate.query("""
                SELECT id, request_id, from_status, to_status, operator_user_id, note, created_at
                  FROM inventory_purchase_request_events
                 WHERE request_id = :request_id
                 ORDER BY created_at ASC, id
                """, Map.of("request_id", requestId), rowMapper::mapPurchaseRequestEvent);
    }

    @Override
    public Optional<StockBalanceRecord> findStockBalance(String itemId, String locationId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id
                  FROM inventory_stock_balances
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                """, Map.of("item_id", itemId, "location_id", locationId), rowMapper::mapStockBalance).stream().findFirst();
    }

    @Override
    public StockBalanceRecord saveStockBalance(StockBalanceRecord balance) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (
                    id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id
                )
                VALUES (
                    :id, :item_id, :location_id, :quantity, :reserved_quantity, :available_quantity, :updated_at, :merchant_id, :warehouse_id
                )
                ON CONFLICT (item_id, location_id) DO UPDATE SET
                    quantity = EXCLUDED.quantity,
                    reserved_quantity = EXCLUDED.reserved_quantity,
                    available_quantity = EXCLUDED.available_quantity,
                    updated_at = EXCLUDED.updated_at,
                    warehouse_id = EXCLUDED.warehouse_id
                """, stockBalanceParameters(balance));
        return balance;
    }

    @Override
    public List<StockBalanceRecord> listStockBalances() {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id
                  FROM inventory_stock_balances
                 ORDER BY updated_at DESC, id
                """, rowMapper::mapStockBalance);
    }

    @Override
    public InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord batch) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_batches (
                    id, item_id, location_id, batch_no, supplier_id, quantity,
                    expire_at, status, evidence_attachment_ids, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :item_id, :location_id, :batch_no, :supplier_id, :quantity,
                    :expire_at, :status, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at, :merchant_id
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
                       expire_at, status, evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM inventory_stock_batches
                 ORDER BY expire_at ASC NULLS LAST, created_at DESC, id
                """, rowMapper::mapStockBatch);
    }

    @Override
    public InventoryRiskAlertRecord saveRiskAlert(InventoryRiskAlertRecord alert) {
        jdbcTemplate.update("""
                INSERT INTO inventory_risk_alerts (
                    id, alert_type, severity, item_id, location_id, batch_id,
                    available_quantity, threshold_quantity, expire_at, status,
                    operator_user_id, created_at, merchant_id
                )
                VALUES (
                    :id, :alert_type, :severity, :item_id, :location_id, :batch_id,
                    :available_quantity, :threshold_quantity, :expire_at, :status,
                    :operator_user_id, :created_at, :merchant_id
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
                       operator_user_id, created_at, merchant_id
                  FROM inventory_risk_alerts
                 ORDER BY created_at DESC, id
                """, rowMapper::mapRiskAlert);
    }

    @Override
    public SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord reconciliation) {
        jdbcTemplate.update("""
                INSERT INTO inventory_supplier_reconciliations (
                    id, supplier_id, period_start, period_end, expected_amount_cent,
                    actual_amount_cent, difference_amount_cent, status,
                    evidence_attachment_ids, operator_user_id, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :supplier_id, :period_start, :period_end, :expected_amount_cent,
                    :actual_amount_cent, :difference_amount_cent, :status,
                    CAST(:evidence_attachment_ids AS jsonb), :operator_user_id, :created_at, :updated_at, :merchant_id
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
                       evidence_attachment_ids, operator_user_id, created_at, updated_at, merchant_id
                  FROM inventory_supplier_reconciliations
                 ORDER BY period_end DESC, id
                """, rowMapper::mapSupplierReconciliation);
    }

    @Override
    public StockMovementRecord saveStockMovement(StockMovementRecord movement) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_movements (
                    id, movement_type, item_id, source_location_id, target_location_id, quantity,
                    business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                    status, created_at, merchant_id
                )
                VALUES (
                    :id, :movement_type, :item_id, :source_location_id, :target_location_id, :quantity,
                    :business_type, :business_id, :trace_code, :operator_id, CAST(:evidence_attachment_ids AS jsonb),
                    :status, :created_at, :merchant_id
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
                       status, created_at, merchant_id
                  FROM inventory_stock_movements
                 ORDER BY created_at DESC, id
                """, rowMapper::mapStockMovement);
    }

    @Override
    public StockAdjustmentRecord saveStockAdjustment(StockAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_adjustments (
                    id, item_id, location_id, adjustment_type, quantity, reason,
                    evidence_attachment_ids, status, created_at, merchant_id, unit_cost_cents
                )
                VALUES (
                    :id, :item_id, :location_id, :adjustment_type, :quantity, :reason,
                    CAST(:evidence_attachment_ids AS jsonb), :status, :created_at, :merchant_id, :unit_cost_cents
                )
                ON CONFLICT (id) DO UPDATE SET
                    item_id = EXCLUDED.item_id,
                    location_id = EXCLUDED.location_id,
                    adjustment_type = EXCLUDED.adjustment_type,
                    quantity = EXCLUDED.quantity,
                    reason = EXCLUDED.reason,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    status = EXCLUDED.status,
                    unit_cost_cents = EXCLUDED.unit_cost_cents
                """, stockAdjustmentParameters(adjustment));
        return adjustment;
    }

    @Override
    public Optional<StockAdjustmentRecord> findStockAdjustmentByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, adjustment_type, quantity, reason,
                       evidence_attachment_ids, status, created_at, merchant_id, unit_cost_cents
                  FROM inventory_stock_adjustments
                 WHERE id = :id
                 FOR UPDATE
                """, Map.of("id", id), rowMapper::mapStockAdjustment).stream().findFirst();
    }

    @Override
    public SavedBottleRecord saveSavedBottle(SavedBottleRecord savedBottle) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottles (
                    id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity,
                    remaining_quantity, unit, expire_at, status, evidence_attachment_ids,
                    created_at, updated_at, merchant_id,
                    is_high_value, high_value_threshold_cents, bottle_label_value,
                    customer_name_or_nickname, customer_phone, customer_phone_hash
                )
                VALUES (
                    :id, :customer_id, :bar_project_id, :item_id, :bottle_name, :initial_quantity,
                    :remaining_quantity, :unit, :expire_at, :status, CAST(:evidence_attachment_ids AS jsonb),
                    :created_at, :updated_at, :merchant_id,
                    :is_high_value, :high_value_threshold_cents, :bottle_label_value,
                    :customer_name_or_nickname, :customer_phone, :customer_phone_hash
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
                    updated_at = EXCLUDED.updated_at,
                    is_high_value = EXCLUDED.is_high_value,
                    high_value_threshold_cents = EXCLUDED.high_value_threshold_cents,
                    bottle_label_value = EXCLUDED.bottle_label_value,
                    customer_name_or_nickname = EXCLUDED.customer_name_or_nickname,
                    customer_phone = EXCLUDED.customer_phone,
                    customer_phone_hash = EXCLUDED.customer_phone_hash
                """, savedBottleParameters(savedBottle));
        return savedBottle;
    }

    @Override
    public Optional<SavedBottleRecord> findSavedBottleById(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM saved_bottles
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapSavedBottle).stream().findFirst();
    }

    @Override
    public List<SavedBottleRecord> listSavedBottles() {
        return jdbcTemplate.query("""
                SELECT *
                  FROM saved_bottles
                 ORDER BY created_at DESC, id
                """, rowMapper::mapSavedBottle);
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
                    handover_result, overall_status, current_segment, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :trace_code, :item_id, :from_staff_id, :to_staff_id, :device_id,
                    :handover_result, :overall_status, :current_segment, CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    trace_code = EXCLUDED.trace_code,
                    item_id = EXCLUDED.item_id,
                    from_staff_id = EXCLUDED.from_staff_id,
                    to_staff_id = EXCLUDED.to_staff_id,
                    device_id = EXCLUDED.device_id,
                    handover_result = EXCLUDED.handover_result,
                    overall_status = EXCLUDED.overall_status,
                    current_segment = EXCLUDED.current_segment,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, traceHandoverParameters(handover));
        return handover;
    }

    @Override
    public Optional<TraceHandoverRecord> findTraceHandoverByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, trace_code, item_id, from_staff_id, to_staff_id, device_id,
                       handover_result, overall_status, current_segment, evidence_attachment_ids, created_at
                  FROM inventory_trace_handovers
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), rowMapper::mapTraceHandover).stream().findFirst();
    }

    @Override
    public List<TraceHandoverStepRecord> listTraceHandoverSteps(String handoverId) {
        return jdbcTemplate.query("""
                SELECT id, handover_id, segment, seq, actor_type, operator_user_id,
                       robot_task_ref, scan_at, step_status, note, created_at
                  FROM inventory_trace_handover_steps
                 WHERE handover_id = :handover_id
                 ORDER BY seq
                """, Map.of("handover_id", handoverId), rowMapper::mapTraceHandoverStep);
    }

    @Override
    public Optional<TraceHandoverStepRecord> findTraceHandoverStep(String handoverId, String segment) {
        return jdbcTemplate.query("""
                SELECT id, handover_id, segment, seq, actor_type, operator_user_id,
                       robot_task_ref, scan_at, step_status, note, created_at
                  FROM inventory_trace_handover_steps
                 WHERE handover_id = :handover_id
                   AND segment = :segment
                   FOR UPDATE
                """, Map.of("handover_id", handoverId, "segment", segment), rowMapper::mapTraceHandoverStep)
                .stream().findFirst();
    }

    @Override
    public TraceHandoverStepRecord saveTraceHandoverStep(TraceHandoverStepRecord step) {
        jdbcTemplate.update("""
                INSERT INTO inventory_trace_handover_steps (
                    id, handover_id, segment, seq, actor_type, operator_user_id,
                    robot_task_ref, scan_at, step_status, note, created_at
                )
                VALUES (
                    :id, :handover_id, :segment, :seq, :actor_type, :operator_user_id,
                    :robot_task_ref, :scan_at, :step_status, :note, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    actor_type = EXCLUDED.actor_type,
                    operator_user_id = EXCLUDED.operator_user_id,
                    robot_task_ref = EXCLUDED.robot_task_ref,
                    scan_at = EXCLUDED.scan_at,
                    step_status = EXCLUDED.step_status,
                    note = EXCLUDED.note
                """, traceHandoverStepParameters(step));
        return step;
    }

    @Override
    public TraceHandoverExceptionRecord saveTraceHandoverException(TraceHandoverExceptionRecord exception) {
        jdbcTemplate.update("""
                INSERT INTO inventory_trace_handover_exceptions (
                    id, handover_id, segment, exception_type, review_status,
                    reviewer_user_id, reason, created_at, reviewed_at
                )
                VALUES (
                    :id, :handover_id, :segment, :exception_type, :review_status,
                    :reviewer_user_id, :reason, :created_at, :reviewed_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    review_status = EXCLUDED.review_status,
                    reviewer_user_id = EXCLUDED.reviewer_user_id,
                    reason = EXCLUDED.reason,
                    reviewed_at = EXCLUDED.reviewed_at
                """, traceHandoverExceptionParameters(exception));
        return exception;
    }

    @Override
    public Optional<TraceHandoverExceptionRecord> findTraceHandoverExceptionByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, handover_id, segment, exception_type, review_status,
                       reviewer_user_id, reason, created_at, reviewed_at
                  FROM inventory_trace_handover_exceptions
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), rowMapper::mapTraceHandoverException).stream().findFirst();
    }

    @Override
    public void saveRobotTaskEvent(
            String id, String robotTaskRef, String eventType, String handoverId, String payloadJson, Instant reportedAt) {
        jdbcTemplate.update("""
                INSERT INTO integration_robot_task_events (
                    id, robot_task_ref, event_type, handover_id, payload, reported_at
                )
                VALUES (
                    :id, :robot_task_ref, :event_type, :handover_id, CAST(:payload AS jsonb), :reported_at
                )
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("robot_task_ref", robotTaskRef)
                .addValue("event_type", eventType)
                .addValue("handover_id", handoverId)
                .addValue("payload", payloadJson == null ? "{}" : payloadJson)
                .addValue("reported_at", offset(reportedAt)));
    }

    // P0-1: 原子减少库存，WHERE 条件确保 available_quantity 足够，返回受影响行数（0 = 库存不足）
    @Override
    public int atomicDecreaseStockBalance(String itemId, String locationId, int qty) {
        return jdbcTemplate.update("""
                UPDATE inventory_stock_balances
                   SET quantity = quantity - :qty,
                       available_quantity = available_quantity - :qty,
                       updated_at = NOW()
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                   AND available_quantity >= :qty
                """, Map.of("qty", qty, "item_id", itemId, "location_id", locationId));
    }

    // P0-1: 原子增加库存，INSERT ON CONFLICT 保证幂等
    @Override
    public void atomicIncreaseStockBalance(String itemId, String locationId, int qty, String newId, String merchantId, String warehouseId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", newId)
                .addValue("item_id", itemId)
                .addValue("location_id", locationId)
                .addValue("qty", qty)
                .addValue("merchant_id", merchantId)
                .addValue("warehouse_id", warehouseId);
        // R5 T6：余额挂结构化库房 warehouse_id（可空）。COALESCE 保证仅在新值非空时覆盖既有挂载，
        // 避免后续不带库房的入库把已挂载的库房抹掉。
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, merchant_id, warehouse_id, updated_at)
                VALUES (:id, :item_id, :location_id, :qty, 0, :qty, :merchant_id, :warehouse_id, NOW())
                ON CONFLICT (item_id, location_id) DO UPDATE SET
                    quantity = inventory_stock_balances.quantity + EXCLUDED.quantity,
                    available_quantity = inventory_stock_balances.available_quantity + EXCLUDED.available_quantity,
                    warehouse_id = COALESCE(EXCLUDED.warehouse_id, inventory_stock_balances.warehouse_id),
                    updated_at = NOW()
                """, params);
    }

    // P0-2: SELECT FOR UPDATE 行锁，防止并发 claimSavedBottle 超领
    @Override
    public Optional<SavedBottleRecord> findSavedBottleByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM saved_bottles
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), rowMapper::mapSavedBottle).stream().findFirst();
    }

    // P1: 按商户隔离 — 各实体列表
    @Override
    public List<InventoryItemRecord> listInventoryItemsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, sku_code, item_name, item_type, unit, barcode, trace_required,
                       min_quantity, status, created_at, updated_at, merchant_id, high_value_threshold_cents
                  FROM inventory_items
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapInventoryItem);
    }

    @Override
    public List<InventorySupplierRecord> listInventorySuppliersByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, supplier_name, supplier_type, contact_name, contact_phone,
                       status, created_at, updated_at, merchant_id
                  FROM inventory_suppliers
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapInventorySupplier);
    }

    @Override
    public List<PurchaseRequestRecord> listPurchaseRequestsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, request_no, supplier_id, item_id, quantity, expected_arrival_date,
                       status, evidence_attachment_ids, created_at, updated_at, merchant_id,
                       supply_contract_id, approved_by, ordered_at, actual_arrival_date, received_at, received_by
                  FROM inventory_purchase_requests
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapPurchaseRequest);
    }

    @Override
    public List<StockBalanceRecord> listStockBalancesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id
                  FROM inventory_stock_balances
                 WHERE merchant_id = :merchant_id
                 ORDER BY updated_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapStockBalance);
    }

    @Override
    public List<StockMovementRecord> listStockMovementsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, movement_type, item_id, source_location_id, target_location_id, quantity,
                       business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                       status, created_at, merchant_id
                  FROM inventory_stock_movements
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapStockMovement);
    }

    @Override
    public List<InventoryStockBatchRecord> listStockBatchesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, batch_no, supplier_id, quantity,
                       expire_at, status, evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM inventory_stock_batches
                 WHERE merchant_id = :merchant_id
                 ORDER BY expire_at ASC NULLS LAST, created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapStockBatch);
    }

    @Override
    public List<InventoryRiskAlertRecord> listRiskAlertsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, alert_type, severity, item_id, location_id, batch_id,
                       available_quantity, threshold_quantity, expire_at, status,
                       operator_user_id, created_at, merchant_id
                  FROM inventory_risk_alerts
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapRiskAlert);
    }

    @Override
    public List<SupplierReconciliationRecord> listSupplierReconciliationsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, supplier_id, period_start, period_end, expected_amount_cent,
                       actual_amount_cent, difference_amount_cent, status,
                       evidence_attachment_ids, operator_user_id, created_at, updated_at, merchant_id
                  FROM inventory_supplier_reconciliations
                 WHERE merchant_id = :merchant_id
                 ORDER BY period_end DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapSupplierReconciliation);
    }

    @Override
    public List<SavedBottleRecord> listSavedBottlesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM saved_bottles
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapSavedBottle);
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
                .addValue("updated_at", offset(item.updatedAt()))
                .addValue("merchant_id", item.merchantId())
                .addValue("high_value_threshold_cents", item.highValueThresholdCents());
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
                .addValue("updated_at", offset(supplier.updatedAt()))
                .addValue("merchant_id", supplier.merchantId());
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
                .addValue("updated_at", offset(request.updatedAt()))
                .addValue("merchant_id", request.merchantId())
                .addValue("supply_contract_id", request.supplyContractId())
                .addValue("approved_by", request.approvedBy())
                .addValue("ordered_at", offset(request.orderedAt()))
                .addValue("actual_arrival_date", request.actualArrivalDate())
                .addValue("received_at", offset(request.receivedAt()))
                .addValue("received_by", request.receivedBy());
    }

    private MapSqlParameterSource stockBalanceParameters(StockBalanceRecord balance) {
        return new MapSqlParameterSource()
                .addValue("id", balance.id())
                .addValue("item_id", balance.itemId())
                .addValue("location_id", balance.locationId())
                .addValue("quantity", balance.quantity())
                .addValue("reserved_quantity", balance.reservedQuantity())
                .addValue("available_quantity", balance.availableQuantity())
                .addValue("updated_at", offset(balance.updatedAt()))
                .addValue("merchant_id", balance.merchantId())
                .addValue("warehouse_id", balance.warehouseId());
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
                .addValue("updated_at", offset(batch.updatedAt()))
                .addValue("merchant_id", batch.merchantId());
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
                .addValue("created_at", offset(alert.createdAt()))
                .addValue("merchant_id", alert.merchantId());
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
                .addValue("updated_at", offset(reconciliation.updatedAt()))
                .addValue("merchant_id", reconciliation.merchantId());
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
                .addValue("created_at", offset(movement.createdAt()))
                .addValue("merchant_id", movement.merchantId());
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
                .addValue("created_at", offset(adjustment.createdAt()))
                .addValue("merchant_id", adjustment.merchantId())
                .addValue("unit_cost_cents", adjustment.unitCostCents());
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
                .addValue("updated_at", offset(savedBottle.updatedAt()))
                .addValue("merchant_id", savedBottle.merchantId())
                .addValue("is_high_value", savedBottle.isHighValue())
                .addValue("high_value_threshold_cents", savedBottle.highValueThresholdCents())
                .addValue("bottle_label_value", savedBottle.bottleLabelValue())
                .addValue("customer_name_or_nickname", savedBottle.customerNameOrNickname())
                .addValue("customer_phone", savedBottle.customerPhone())
                .addValue("customer_phone_hash", savedBottle.customerPhoneHash());
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
                .addValue("overall_status", handover.overallStatus())
                .addValue("current_segment", handover.currentSegment())
                .addValue("evidence_attachment_ids", writeList(handover.evidenceAttachmentIds()))
                .addValue("created_at", offset(handover.createdAt()));
    }

    private MapSqlParameterSource traceHandoverStepParameters(TraceHandoverStepRecord step) {
        return new MapSqlParameterSource()
                .addValue("id", step.id())
                .addValue("handover_id", step.handoverId())
                .addValue("segment", step.segment())
                .addValue("seq", step.seq())
                .addValue("actor_type", step.actorType())
                .addValue("operator_user_id", step.operatorUserId())
                .addValue("robot_task_ref", step.robotTaskRef())
                .addValue("scan_at", offset(step.scanAt()))
                .addValue("step_status", step.stepStatus())
                .addValue("note", step.note())
                .addValue("created_at", offset(step.createdAt()));
    }

    private MapSqlParameterSource traceHandoverExceptionParameters(TraceHandoverExceptionRecord exception) {
        return new MapSqlParameterSource()
                .addValue("id", exception.id())
                .addValue("handover_id", exception.handoverId())
                .addValue("segment", exception.segment())
                .addValue("exception_type", exception.exceptionType())
                .addValue("review_status", exception.reviewStatus())
                .addValue("reviewer_user_id", exception.reviewerUserId())
                .addValue("reason", exception.reason())
                .addValue("created_at", offset(exception.createdAt()))
                .addValue("reviewed_at", offset(exception.reviewedAt()));
    }

    // ── R5 T6：供应合同三模式 ──────────────────────────────────────────────────
    @Override
    public SupplyContractRecord saveSupplyContract(SupplyContractRecord contract) {
        jdbcTemplate.update("""
                INSERT INTO supply_contracts (
                    id, contract_no, supplier_id, project_id, merchant_id, contract_mode,
                    supply_scope, price_terms, payment_term_days, valid_from, valid_to, summary,
                    purchasable_scope, approval_status, status, created_at, updated_at
                )
                VALUES (
                    :id, :contract_no, :supplier_id, :project_id, :merchant_id, :contract_mode,
                    :supply_scope, :price_terms, :payment_term_days, :valid_from, :valid_to, :summary,
                    CAST(:purchasable_scope AS jsonb), :approval_status, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    contract_no = EXCLUDED.contract_no,
                    supplier_id = EXCLUDED.supplier_id,
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id,
                    contract_mode = EXCLUDED.contract_mode,
                    supply_scope = EXCLUDED.supply_scope,
                    price_terms = EXCLUDED.price_terms,
                    payment_term_days = EXCLUDED.payment_term_days,
                    valid_from = EXCLUDED.valid_from,
                    valid_to = EXCLUDED.valid_to,
                    summary = EXCLUDED.summary,
                    purchasable_scope = EXCLUDED.purchasable_scope,
                    approval_status = EXCLUDED.approval_status,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, supplyContractParameters(contract));
        return contract;
    }

    @Override
    public Optional<SupplyContractRecord> findSupplyContractByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, contract_no, supplier_id, project_id, merchant_id, contract_mode,
                       supply_scope, price_terms, payment_term_days, valid_from, valid_to, summary,
                       purchasable_scope, approval_status, status, created_at, updated_at
                  FROM supply_contracts
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), rowMapper::mapSupplyContract).stream().findFirst();
    }

    @Override
    public List<SupplyContractRecord> listSupplyContracts() {
        return jdbcTemplate.query("""
                SELECT id, contract_no, supplier_id, project_id, merchant_id, contract_mode,
                       supply_scope, price_terms, payment_term_days, valid_from, valid_to, summary,
                       purchasable_scope, approval_status, status, created_at, updated_at
                  FROM supply_contracts
                 ORDER BY created_at DESC, id
                """, rowMapper::mapSupplyContract);
    }

    @Override
    public List<SupplyContractRecord> listSupplyContractsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, contract_no, supplier_id, project_id, merchant_id, contract_mode,
                       supply_scope, price_terms, payment_term_days, valid_from, valid_to, summary,
                       purchasable_scope, approval_status, status, created_at, updated_at
                  FROM supply_contracts
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapSupplyContract);
    }

    private MapSqlParameterSource supplyContractParameters(SupplyContractRecord contract) {
        return new MapSqlParameterSource()
                .addValue("id", contract.id())
                .addValue("contract_no", contract.contractNo())
                .addValue("supplier_id", contract.supplierId())
                .addValue("project_id", contract.projectId())
                .addValue("merchant_id", contract.merchantId())
                .addValue("contract_mode", contract.contractMode())
                .addValue("supply_scope", contract.supplyScope())
                .addValue("price_terms", contract.priceTerms())
                .addValue("payment_term_days", contract.paymentTermDays())
                .addValue("valid_from", contract.validFrom())
                .addValue("valid_to", contract.validTo())
                .addValue("summary", contract.summary())
                .addValue("purchasable_scope", writeList(contract.purchasableScope()))
                .addValue("approval_status", contract.approvalStatus())
                .addValue("status", contract.status())
                .addValue("created_at", offset(contract.createdAt()))
                .addValue("updated_at", offset(contract.updatedAt()));
    }

    // ── R5 T6：结构化库房 ─────────────────────────────────────────────────────
    @Override
    public WarehouseRecord saveWarehouse(WarehouseRecord warehouse) {
        jdbcTemplate.update("""
                INSERT INTO warehouses (
                    id, project_id, merchant_id, warehouse_type, name, status, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :merchant_id, :warehouse_type, :name, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id,
                    warehouse_type = EXCLUDED.warehouse_type,
                    name = EXCLUDED.name,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, warehouseParameters(warehouse));
        return warehouse;
    }

    @Override
    public List<WarehouseRecord> listWarehouses() {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, warehouse_type, name, status, created_at, updated_at
                  FROM warehouses
                 ORDER BY created_at DESC, id
                """, rowMapper::mapWarehouse);
    }

    @Override
    public List<WarehouseRecord> listWarehousesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, warehouse_type, name, status, created_at, updated_at
                  FROM warehouses
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapWarehouse);
    }

    private MapSqlParameterSource warehouseParameters(WarehouseRecord warehouse) {
        return new MapSqlParameterSource()
                .addValue("id", warehouse.id())
                .addValue("project_id", warehouse.projectId())
                .addValue("merchant_id", warehouse.merchantId())
                .addValue("warehouse_type", warehouse.warehouseType())
                .addValue("name", warehouse.name())
                .addValue("status", warehouse.status())
                .addValue("created_at", offset(warehouse.createdAt()))
                .addValue("updated_at", offset(warehouse.updatedAt()));
    }

    // ── R5 T6：库存域内高价值复核（inventory_high_value_reviews） ─────────────────
    @Override
    public InventoryHighValueReviewRecord saveHighValueReview(InventoryHighValueReviewRecord review) {
        jdbcTemplate.update("""
                INSERT INTO inventory_high_value_reviews (
                    id, subject_type, subject_id, photo_attachment_ids, video_attachment_ids,
                    review_status, reviewer_user_id, reason, created_at, reviewed_at
                )
                VALUES (
                    :id, :subject_type, :subject_id,
                    CAST(:photo_attachment_ids AS jsonb), CAST(:video_attachment_ids AS jsonb),
                    :review_status, :reviewer_user_id, :reason, :created_at, :reviewed_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    subject_type = EXCLUDED.subject_type,
                    subject_id = EXCLUDED.subject_id,
                    photo_attachment_ids = EXCLUDED.photo_attachment_ids,
                    video_attachment_ids = EXCLUDED.video_attachment_ids,
                    review_status = EXCLUDED.review_status,
                    reviewer_user_id = EXCLUDED.reviewer_user_id,
                    reason = EXCLUDED.reason,
                    reviewed_at = EXCLUDED.reviewed_at
                """, new MapSqlParameterSource()
                .addValue("id", review.id())
                .addValue("subject_type", review.subjectType())
                .addValue("subject_id", review.subjectId())
                .addValue("photo_attachment_ids", writeList(review.photoAttachmentIds()))
                .addValue("video_attachment_ids", writeList(review.videoAttachmentIds()))
                .addValue("review_status", review.reviewStatus())
                .addValue("reviewer_user_id", review.reviewerUserId())
                .addValue("reason", review.reason())
                .addValue("created_at", offset(review.createdAt()))
                .addValue("reviewed_at", offset(review.reviewedAt())));
        return review;
    }

    @Override
    public Optional<InventoryHighValueReviewRecord> findHighValueReviewByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, photo_attachment_ids, video_attachment_ids,
                       review_status, reviewer_user_id, reason, created_at, reviewed_at
                  FROM inventory_high_value_reviews
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), rowMapper::mapHighValueReview).stream().findFirst();
    }

    @Override
    public List<InventoryHighValueReviewRecord> listHighValueReviews() {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, photo_attachment_ids, video_attachment_ids,
                       review_status, reviewer_user_id, reason, created_at, reviewed_at
                  FROM inventory_high_value_reviews
                 ORDER BY created_at DESC, id
                """, rowMapper::mapHighValueReview);
    }

    private String writeList(List<String> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("inventory attachment ids are not serializable", exception);
        }
    }

    private OffsetDateTime offset(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
