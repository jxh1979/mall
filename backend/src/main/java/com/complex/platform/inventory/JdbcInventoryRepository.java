package com.complex.platform.inventory;

import com.complex.platform.common.crypto.PiiCipher;
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
    private final PiiCipher piiCipher;

    JdbcInventoryRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.piiCipher = piiCipher;
        this.rowMapper = new JdbcInventoryRowMapper(this.objectMapper, piiCipher);
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
    public int sumStockBalanceByItemAndWarehouse(String itemId, String warehouseId) {
        // 2e 盘点冻结：系统账面值 = 该 item 在该 warehouse 全部 location 的 quantity 合计（盘点 book_qty 取此，而非客户端传入）。
        Integer sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                  FROM inventory_stock_balances
                 WHERE item_id = :item_id
                   AND warehouse_id = :warehouse_id
                """, Map.of("item_id", itemId, "warehouse_id", warehouseId), Integer.class);
        return sum == null ? 0 : sum;
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
    public InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord batch, String projectId) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_batches (
                    id, item_id, location_id, batch_no, supplier_id, quantity,
                    expire_at, status, evidence_attachment_ids, created_at, updated_at, merchant_id, project_id
                )
                VALUES (
                    :id, :item_id, :location_id, :batch_no, :supplier_id, :quantity,
                    :expire_at, :status, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at, :merchant_id, :project_id
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
                    updated_at = EXCLUDED.updated_at,
                    project_id = COALESCE(EXCLUDED.project_id, inventory_stock_batches.project_id)
                """, stockBatchParameters(batch).addValue("project_id", projectId));
        return batch;
    }

    @Override
    public List<InventoryStockBatchRecord> listActiveBatchesForConsumption(String itemId, String locationId) {
        // 2e FEFO（先到期先出，涵盖 FIFO）：item+location 的 active 批次按 expire_at 升序（NULL 最后）、created_at 升序。
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, batch_no, supplier_id, quantity,
                       expire_at, status, evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM inventory_stock_batches
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                   AND status = 'active'
                   AND quantity > 0
                 ORDER BY expire_at ASC NULLS LAST, created_at ASC, id
                """, Map.of("item_id", itemId, "location_id", locationId), rowMapper::mapStockBatch);
    }

    @Override
    public int atomicDecreaseBatchQuantity(String batchId, int qty) {
        // 2e：原子扣减批次 quantity（CAS，quantity>=qty 才扣），扣至 0 时 status→consumed。
        return jdbcTemplate.update("""
                UPDATE inventory_stock_batches
                   SET quantity = quantity - :qty,
                       status = CASE WHEN quantity - :qty <= 0 THEN 'consumed' ELSE status END,
                       updated_at = NOW()
                 WHERE id = :id
                   AND quantity >= :qty
                   AND status = 'active'
                """, Map.of("id", batchId, "qty", qty));
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
    public StockMovementRecord saveStockMovement(StockMovementRecord movement, String projectId) {
        // V296 成本台账：所有流水写入的单一汇聚点，在此统一按移动加权口径计成本（各入口无须自理）。
        // 先 FOR UPDATE 锁状态行（同物料并发出入库串行化；写路径均在 @Transactional 内），
        // 再查流水 id 是否已存在——幂等重放不得二次滚动状态、保留库中已定稿的成本列。
        initCostStateIfAbsent(movement.itemId());
        Optional<InventoryItemCostStateRecord> state = lockCostStateForUpdate(movement.itemId());
        List<StockMovementRecord> existing = jdbcTemplate.query("""
                SELECT unit_cost_cent, total_cost_cent FROM inventory_stock_movements WHERE id = :id
                """, Map.of("id", movement.id()),
                (rs, i) -> withCosts(movement, rs.getObject("unit_cost_cent", Long.class),
                        rs.getObject("total_cost_cent", Long.class)));
        StockMovementRecord stamped;
        if (!existing.isEmpty()) {
            // 重放：更新其余字段（既有 upsert 语义），成本列与状态行保持不动。
            stamped = existing.getFirst();
            jdbcTemplate.update("""
                    UPDATE inventory_stock_movements SET
                        movement_type = :movement_type,
                        item_id = :item_id,
                        source_location_id = :source_location_id,
                        target_location_id = :target_location_id,
                        quantity = :quantity,
                        business_type = :business_type,
                        business_id = :business_id,
                        trace_code = :trace_code,
                        operator_id = :operator_id,
                        evidence_attachment_ids = CAST(:evidence_attachment_ids AS jsonb),
                        status = :status,
                        project_id = COALESCE(:project_id, project_id)
                    WHERE id = :id
                    """, stockMovementParameters(movement).addValue("project_id", projectId));
            return stamped;
        }
        InventoryCostMath.Result result = state
                .map(current -> InventoryCostMath.apply(
                        current,
                        InventoryCostMath.direction(movement.sourceLocationId(), movement.targetLocationId()),
                        movement.quantity(),
                        movement.unitCostCent(),
                        java.time.Instant.now()))
                // 状态行缺席（物料不存在等异常路径）：忠实透传入库单价，不滚动状态。
                .orElseGet(() -> new InventoryCostMath.Result(
                        movement.unitCostCent(),
                        movement.unitCostCent() == null ? null : movement.unitCostCent() * movement.quantity(),
                        null));
        stamped = withCosts(movement, result.unitCostCent(), result.totalCostCent());
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_movements (
                    id, movement_type, item_id, source_location_id, target_location_id, quantity,
                    business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                    status, created_at, merchant_id, project_id, unit_cost_cent, total_cost_cent
                )
                VALUES (
                    :id, :movement_type, :item_id, :source_location_id, :target_location_id, :quantity,
                    :business_type, :business_id, :trace_code, :operator_id, CAST(:evidence_attachment_ids AS jsonb),
                    :status, :created_at, :merchant_id, :project_id, :unit_cost_cent, :total_cost_cent
                )
                """, stockMovementParameters(stamped).addValue("project_id", projectId));
        if (result.newState() != null) {
            updateCostState(result.newState());
        }
        return stamped;
    }

    private StockMovementRecord withCosts(StockMovementRecord movement, Long unitCostCent, Long totalCostCent) {
        return new StockMovementRecord(
                movement.id(), movement.movementType(), movement.itemId(), movement.sourceLocationId(),
                movement.targetLocationId(), movement.quantity(), unitCostCent, totalCostCent,
                movement.businessType(), movement.businessId(), movement.traceCode(), movement.operatorId(),
                movement.evidenceAttachmentIds(), movement.status(), movement.createdAt(), movement.merchantId());
    }

    @Override
    public void initCostStateIfAbsent(String itemId) {
        jdbcTemplate.update("""
                INSERT INTO inventory_item_cost_state (item_id, merchant_id, updated_at)
                SELECT id, merchant_id, NOW() FROM inventory_items WHERE id = :item_id
                ON CONFLICT (item_id) DO NOTHING
                """, Map.of("item_id", itemId));
    }

    @Override
    public Optional<InventoryItemCostStateRecord> lockCostStateForUpdate(String itemId) {
        return jdbcTemplate.query("""
                SELECT item_id, merchant_id, on_hand_quantity, total_cost_cent, avg_unit_cost_cent,
                       cumulative_in_cost_cent, cumulative_out_cost_cent, cost_method, has_cost_basis, updated_at
                  FROM inventory_item_cost_state
                 WHERE item_id = :item_id
                 FOR UPDATE
                """, Map.of("item_id", itemId), (rs, i) -> new InventoryItemCostStateRecord(
                        rs.getString("item_id"),
                        rs.getString("merchant_id"),
                        rs.getLong("on_hand_quantity"),
                        rs.getLong("total_cost_cent"),
                        rs.getLong("avg_unit_cost_cent"),
                        rs.getLong("cumulative_in_cost_cent"),
                        rs.getLong("cumulative_out_cost_cent"),
                        rs.getString("cost_method"),
                        rs.getBoolean("has_cost_basis"),
                        rs.getTimestamp("updated_at").toInstant())).stream().findFirst();
    }

    @Override
    public void updateCostState(InventoryItemCostStateRecord state) {
        jdbcTemplate.update("""
                UPDATE inventory_item_cost_state SET
                    on_hand_quantity = :on_hand_quantity,
                    total_cost_cent = :total_cost_cent,
                    avg_unit_cost_cent = :avg_unit_cost_cent,
                    cumulative_in_cost_cent = :cumulative_in_cost_cent,
                    cumulative_out_cost_cent = :cumulative_out_cost_cent,
                    has_cost_basis = :has_cost_basis,
                    updated_at = NOW()
                WHERE item_id = :item_id
                """, new MapSqlParameterSource()
                        .addValue("item_id", state.itemId())
                        .addValue("on_hand_quantity", state.onHandQuantity())
                        .addValue("total_cost_cent", state.totalCostCent())
                        .addValue("avg_unit_cost_cent", state.avgUnitCostCent())
                        .addValue("cumulative_in_cost_cent", state.cumulativeInCostCent())
                        .addValue("cumulative_out_cost_cent", state.cumulativeOutCostCent())
                        .addValue("has_cost_basis", state.hasCostBasis()));
    }

    // V296 成本流水分页：与 listStockMovementsPaged 同租户谓词，另支持按物料过滤。
    @Override
    public com.complex.platform.common.api.PageResponse<StockMovementRecord> listCostLedgerPaged(
            String merchantId, String projectId, String itemId, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 50 : Math.min(size, 200);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_id", projectId, java.sql.Types.VARCHAR)
                .addValue("item_id", itemId, java.sql.Types.VARCHAR);
        String where = """
                 WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)
                   AND (:project_id IS NULL OR project_id = :project_id)
                   AND (:item_id IS NULL OR item_id = :item_id)
                """;
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_stock_movements" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<StockMovementRecord> items = jdbcTemplate.query("""
                SELECT id, movement_type, item_id, source_location_id, target_location_id, quantity,
                       business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                       status, created_at, merchant_id, unit_cost_cent, total_cost_cent
                  FROM inventory_stock_movements
                """ + where + """
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, rowMapper::mapStockMovement);
        return new com.complex.platform.common.api.PageResponse<>(
                items, safePage, safeSize, total == null ? 0L : total);
    }

    @Override
    public InventoryCostSummaryRecord sumOutboundCost(
            String merchantId, String projectId, String periodStart, String periodEnd,
            java.time.Instant from, java.time.Instant to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_id", projectId, java.sql.Types.VARCHAR)
                .addValue("from", java.time.OffsetDateTime.ofInstant(from, java.time.ZoneOffset.UTC))
                .addValue("to", java.time.OffsetDateTime.ofInstant(to, java.time.ZoneOffset.UTC));
        return jdbcTemplate.query("""
                SELECT COUNT(*) AS movement_count,
                       COALESCE(SUM(total_cost_cent), 0) AS total_cost,
                       COUNT(*) FILTER (WHERE total_cost_cent IS NULL) AS no_basis_count
                  FROM inventory_stock_movements
                 WHERE source_location_id IS NOT NULL
                   AND target_location_id IS NULL
                   AND (:merchant_id IS NULL OR merchant_id = :merchant_id)
                   AND (:project_id IS NULL OR project_id = :project_id)
                   AND created_at >= :from
                   AND created_at < :to
                """, params, (rs, i) -> new InventoryCostSummaryRecord(
                        merchantId, periodStart, periodEnd,
                        rs.getLong("total_cost"),
                        rs.getLong("movement_count"),
                        rs.getLong("no_basis_count"))).getFirst();
    }

    @Override
    public List<StockMovementRecord> listStockMovements() {
        return jdbcTemplate.query("""
                SELECT id, movement_type, item_id, source_location_id, target_location_id, quantity,
                       business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                       status, created_at, merchant_id, unit_cost_cent, total_cost_cent
                  FROM inventory_stock_movements
                 ORDER BY created_at DESC, id
                """, rowMapper::mapStockMovement);
    }

    // AUD-013 真分页: SQL 层 LIMIT/OFFSET + COUNT, total 取真值而非 items.size()。
    // 租户谓词 (:x IS NULL OR col=:x) 与 listStockMovementsByMerchant/ByProject 同口径, 参数带 SQL 类型供 PG 推断 NULL 分支。
    @Override
    public com.complex.platform.common.api.PageResponse<StockMovementRecord> listStockMovementsPaged(
            String merchantId, String projectId, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 50 : Math.min(size, 200);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_id", projectId, java.sql.Types.VARCHAR);
        String where = """
                 WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)
                   AND (:project_id IS NULL OR project_id = :project_id)
                """;
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_stock_movements" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<StockMovementRecord> items = jdbcTemplate.query("""
                SELECT id, movement_type, item_id, source_location_id, target_location_id, quantity,
                       business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                       status, created_at, merchant_id, unit_cost_cent, total_cost_cent
                  FROM inventory_stock_movements
                """ + where + """
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, rowMapper::mapStockMovement);
        return new com.complex.platform.common.api.PageResponse<>(
                items, safePage, safeSize, total == null ? 0L : total);
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
                    customer_name_or_nickname, customer_phone, customer_phone_hash,
                    registration_photo_refs, manual_note,
                    transferred_at, transfer_to_name_or_nickname, transfer_to_phone, transfer_to_phone_hash
                )
                VALUES (
                    :id, :customer_id, :bar_project_id, :item_id, :bottle_name, :initial_quantity,
                    :remaining_quantity, :unit, :expire_at, :status, CAST(:evidence_attachment_ids AS jsonb),
                    :created_at, :updated_at, :merchant_id,
                    :is_high_value, :high_value_threshold_cents, :bottle_label_value,
                    :customer_name_or_nickname, :customer_phone, :customer_phone_hash,
                    CAST(:registration_photo_refs AS jsonb), :manual_note,
                    :transferred_at, :transfer_to_name_or_nickname, :transfer_to_phone, :transfer_to_phone_hash
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
                    customer_phone_hash = EXCLUDED.customer_phone_hash,
                    registration_photo_refs = EXCLUDED.registration_photo_refs,
                    manual_note = EXCLUDED.manual_note,
                    transferred_at = EXCLUDED.transferred_at,
                    transfer_to_name_or_nickname = EXCLUDED.transfer_to_name_or_nickname,
                    transfer_to_phone = EXCLUDED.transfer_to_phone,
                    transfer_to_phone_hash = EXCLUDED.transfer_to_phone_hash
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
    public List<SavedBottleRecord> listStoredSavedBottlesExpiring(Instant now, Instant windowEnd) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM saved_bottles
                 WHERE status = 'stored'
                   AND expire_at > :now
                   AND expire_at <= :window_end
                 ORDER BY expire_at ASC, id
                """, new MapSqlParameterSource()
                .addValue("now", offset(now))
                .addValue("window_end", offset(windowEnd)), rowMapper::mapSavedBottle);
    }

    @Override
    public List<SavedBottleRecord> listStoredSavedBottlesExpired(Instant now) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM saved_bottles
                 WHERE status = 'stored'
                   AND expire_at < :now
                 ORDER BY expire_at ASC, id
                """, new MapSqlParameterSource()
                .addValue("now", offset(now)), rowMapper::mapSavedBottle);
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
                    handover_result, overall_status, current_segment, evidence_attachment_ids, created_at, merchant_id
                )
                VALUES (
                    :id, :trace_code, :item_id, :from_staff_id, :to_staff_id, :device_id,
                    :handover_result, :overall_status, :current_segment, CAST(:evidence_attachment_ids AS jsonb), :created_at, :merchant_id
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
                       handover_result, overall_status, current_segment, evidence_attachment_ids, created_at, merchant_id
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
    // C1: 数据层防跨商户扣减——WHERE 叠加 merchant 谓词（scope 为 null=平台/项目管理员放行全量）。
    // 与入口 requireItemOwnership 服务层守卫构成纵深防御：即便入口被绕过，SQL 也不会扣到他商户余额。
    @Override
    public int atomicDecreaseStockBalance(String itemId, String locationId, int qty, String merchantScope) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("qty", qty)
                .addValue("item_id", itemId)
                .addValue("location_id", locationId)
                .addValue("scope", merchantScope);
        return jdbcTemplate.update("""
                UPDATE inventory_stock_balances
                   SET quantity = quantity - :qty,
                       available_quantity = available_quantity - :qty,
                       updated_at = NOW()
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                   AND available_quantity >= :qty
                   AND (merchant_id = :scope OR CAST(:scope AS VARCHAR) IS NULL)
                """, params);
    }

    // P0-1: 原子增加库存，INSERT ON CONFLICT 保证幂等
    // C2: 余额行落归属 project_id（入库时由服务层从 currentProjectScopeQuietly 传入），供项目级读隔离。
    @Override
    public void atomicIncreaseStockBalance(String itemId, String locationId, int qty, String newId, String merchantId, String warehouseId, String projectId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", newId)
                .addValue("item_id", itemId)
                .addValue("location_id", locationId)
                .addValue("qty", qty)
                .addValue("merchant_id", merchantId)
                .addValue("warehouse_id", warehouseId)
                .addValue("project_id", projectId);
        // R5 T6：余额挂结构化库房 warehouse_id（可空）。COALESCE 保证仅在新值非空时覆盖既有挂载，
        // 避免后续不带库房的入库把已挂载的库房抹掉。project_id 同 COALESCE 语义（不被后续空值抹掉）。
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, merchant_id, warehouse_id, project_id, updated_at)
                VALUES (:id, :item_id, :location_id, :qty, 0, :qty, :merchant_id, :warehouse_id, :project_id, NOW())
                ON CONFLICT (item_id, location_id) DO UPDATE SET
                    quantity = inventory_stock_balances.quantity + EXCLUDED.quantity,
                    available_quantity = inventory_stock_balances.available_quantity + EXCLUDED.available_quantity,
                    warehouse_id = COALESCE(EXCLUDED.warehouse_id, inventory_stock_balances.warehouse_id),
                    project_id = COALESCE(EXCLUDED.project_id, inventory_stock_balances.project_id),
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

    // C2: 项目级读隔离——项目员工（无商户 scope、有项目 scope）只读本项目余额，不越读他项目全平台库存。
    @Override
    public List<StockBalanceRecord> listStockBalancesByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id
                  FROM inventory_stock_balances
                 WHERE project_id = :project_id
                 ORDER BY updated_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapStockBalance);
    }

    @Override
    public List<StockMovementRecord> listStockMovementsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, movement_type, item_id, source_location_id, target_location_id, quantity,
                       business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                       status, created_at, merchant_id, unit_cost_cent, total_cost_cent
                  FROM inventory_stock_movements
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapStockMovement);
    }

    // C2 read-side: project staff (no merchant scope) only read own-project movements.
    @Override
    public List<StockMovementRecord> listStockMovementsByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, movement_type, item_id, source_location_id, target_location_id, quantity,
                       business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                       status, created_at, merchant_id, unit_cost_cent, total_cost_cent
                  FROM inventory_stock_movements
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapStockMovement);
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

    // C2 read-side: project staff (no merchant scope) only read own-project batches.
    @Override
    public List<InventoryStockBatchRecord> listStockBatchesByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, batch_no, supplier_id, quantity,
                       expire_at, status, evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM inventory_stock_batches
                 WHERE project_id = :project_id
                 ORDER BY expire_at ASC NULLS LAST, created_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapStockBatch);
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
                .addValue("merchant_id", movement.merchantId())
                // V296：成本列（可空=无成本基准）。
                .addValue("unit_cost_cent", movement.unitCostCent())
                .addValue("total_cost_cent", movement.totalCostCent());
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
                .addValue("customer_phone", piiCipher.encrypt(savedBottle.customerPhone()))
                .addValue("customer_phone_hash", savedBottle.customerPhoneHash())
                .addValue("registration_photo_refs", writeList(savedBottle.registrationPhotoRefs()))
                .addValue("manual_note", savedBottle.manualNote())
                .addValue("transferred_at", offset(savedBottle.transferredAt()))
                .addValue("transfer_to_name_or_nickname", savedBottle.transferToNameOrNickname())
                .addValue("transfer_to_phone", piiCipher.encrypt(savedBottle.transferToPhone()))
                .addValue("transfer_to_phone_hash", savedBottle.transferToPhoneHash());
    }

    @Override
    public SavedBottleTransferRecord saveSavedBottleTransfer(SavedBottleTransferRecord transfer) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_transfers (
                    id, saved_bottle_id, from_customer_id, from_name_or_nickname,
                    to_name_or_nickname, to_phone, to_phone_hash, transfer_quantity,
                    operator_id, status, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :saved_bottle_id, :from_customer_id, :from_name_or_nickname,
                    :to_name_or_nickname, :to_phone, :to_phone_hash, :transfer_quantity,
                    :operator_id, :status, CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    saved_bottle_id = EXCLUDED.saved_bottle_id,
                    from_customer_id = EXCLUDED.from_customer_id,
                    from_name_or_nickname = EXCLUDED.from_name_or_nickname,
                    to_name_or_nickname = EXCLUDED.to_name_or_nickname,
                    to_phone = EXCLUDED.to_phone,
                    to_phone_hash = EXCLUDED.to_phone_hash,
                    transfer_quantity = EXCLUDED.transfer_quantity,
                    operator_id = EXCLUDED.operator_id,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, new MapSqlParameterSource()
                .addValue("id", transfer.id())
                .addValue("saved_bottle_id", transfer.savedBottleId())
                .addValue("from_customer_id", transfer.fromCustomerId())
                .addValue("from_name_or_nickname", transfer.fromNameOrNickname())
                .addValue("to_name_or_nickname", transfer.toNameOrNickname())
                .addValue("to_phone", piiCipher.encrypt(transfer.toPhone()))
                .addValue("to_phone_hash", transfer.toPhoneHash())
                .addValue("transfer_quantity", transfer.transferQuantity())
                .addValue("operator_id", transfer.operatorId())
                .addValue("status", transfer.status())
                .addValue("evidence_attachment_ids", writeList(transfer.evidenceAttachmentIds()))
                .addValue("created_at", offset(transfer.createdAt())));
        return transfer;
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
                .addValue("created_at", offset(handover.createdAt()))
                .addValue("merchant_id", handover.merchantId());
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

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<SupplyContractRecord> listSupplyContractsByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, contract_no, supplier_id, project_id, merchant_id, contract_mode,
                       supply_scope, price_terms, payment_term_days, valid_from, valid_to, summary,
                       purchasable_scope, approval_status, status, created_at, updated_at
                  FROM supply_contracts
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapSupplyContract);
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

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<WarehouseRecord> listWarehousesByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, warehouse_type, name, status, created_at, updated_at
                  FROM warehouses
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapWarehouse);
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
                    review_status, reviewer_user_id, reason, created_at, reviewed_at, merchant_id
                )
                VALUES (
                    :id, :subject_type, :subject_id,
                    CAST(:photo_attachment_ids AS jsonb), CAST(:video_attachment_ids AS jsonb),
                    :review_status, :reviewer_user_id, :reason, :created_at, :reviewed_at, :merchant_id
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
                .addValue("reviewed_at", offset(review.reviewedAt()))
                .addValue("merchant_id", review.merchantId()));
        return review;
    }

    @Override
    public Optional<InventoryHighValueReviewRecord> findHighValueReviewByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, photo_attachment_ids, video_attachment_ids,
                       review_status, reviewer_user_id, reason, created_at, reviewed_at, merchant_id
                  FROM inventory_high_value_reviews
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), rowMapper::mapHighValueReview).stream().findFirst();
    }

    @Override
    public List<InventoryHighValueReviewRecord> listHighValueReviews() {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, photo_attachment_ids, video_attachment_ids,
                       review_status, reviewer_user_id, reason, created_at, reviewed_at, merchant_id
                  FROM inventory_high_value_reviews
                 ORDER BY created_at DESC, id
                """, rowMapper::mapHighValueReview);
    }

    // ── P1#18：供应商资质（进货查验） ─────────────────────────────────────────────
    @Override
    public SupplierQualificationRecord saveSupplierQualification(SupplierQualificationRecord qualification) {
        jdbcTemplate.update("""
                INSERT INTO supplier_qualifications (
                    id, supplier_id, qualification_type, qualification_no, issued_by,
                    valid_from, valid_to, attachment_ids, status, merchant_id, created_at, updated_at
                )
                VALUES (
                    :id, :supplier_id, :qualification_type, :qualification_no, :issued_by,
                    :valid_from, :valid_to, CAST(:attachment_ids AS jsonb), :status, :merchant_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    qualification_type = EXCLUDED.qualification_type,
                    qualification_no = EXCLUDED.qualification_no,
                    issued_by = EXCLUDED.issued_by,
                    valid_from = EXCLUDED.valid_from,
                    valid_to = EXCLUDED.valid_to,
                    attachment_ids = EXCLUDED.attachment_ids,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", qualification.id())
                .addValue("supplier_id", qualification.supplierId())
                .addValue("qualification_type", qualification.qualificationType())
                .addValue("qualification_no", qualification.qualificationNo())
                .addValue("issued_by", qualification.issuedBy())
                .addValue("valid_from", qualification.validFrom())
                .addValue("valid_to", qualification.validTo())
                .addValue("attachment_ids", writeList(qualification.attachmentIds()))
                .addValue("status", qualification.status())
                .addValue("merchant_id", qualification.merchantId())
                .addValue("created_at", offset(qualification.createdAt()))
                .addValue("updated_at", offset(qualification.updatedAt())));
        return qualification;
    }

    @Override
    public List<SupplierQualificationRecord> listSupplierQualificationsBySupplier(String supplierId) {
        return jdbcTemplate.query("""
                SELECT id, supplier_id, qualification_type, qualification_no, issued_by,
                       valid_from, valid_to, attachment_ids, status, merchant_id, created_at, updated_at
                  FROM supplier_qualifications
                 WHERE supplier_id = :supplier_id
                 ORDER BY created_at DESC, id
                """, Map.of("supplier_id", supplierId), rowMapper::mapSupplierQualification);
    }

    @Override
    public List<SupplierQualificationRecord> listSupplierQualificationsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, supplier_id, qualification_type, qualification_no, issued_by,
                       valid_from, valid_to, attachment_ids, status, merchant_id, created_at, updated_at
                  FROM supplier_qualifications
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapSupplierQualification);
    }

    @Override
    public List<SupplierQualificationRecord> listSupplierQualifications() {
        return jdbcTemplate.query("""
                SELECT id, supplier_id, qualification_type, qualification_no, issued_by,
                       valid_from, valid_to, attachment_ids, status, merchant_id, created_at, updated_at
                  FROM supplier_qualifications
                 ORDER BY created_at DESC, id
                """, rowMapper::mapSupplierQualification);
    }

    // ── P1#18：供应商结算多档/账期 ────────────────────────────────────────────────
    @Override
    public SupplierSettlementMethodRecord saveSupplierSettlementMethod(SupplierSettlementMethodRecord method) {
        jdbcTemplate.update("""
                INSERT INTO supplier_settlement_methods (
                    id, supplier_id, settlement_method, payment_term_days, applicable_category,
                    is_default, status, merchant_id, created_at, updated_at
                )
                VALUES (
                    :id, :supplier_id, :settlement_method, :payment_term_days, :applicable_category,
                    :is_default, :status, :merchant_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    settlement_method = EXCLUDED.settlement_method,
                    payment_term_days = EXCLUDED.payment_term_days,
                    applicable_category = EXCLUDED.applicable_category,
                    is_default = EXCLUDED.is_default,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", method.id())
                .addValue("supplier_id", method.supplierId())
                .addValue("settlement_method", method.settlementMethod())
                .addValue("payment_term_days", method.paymentTermDays())
                .addValue("applicable_category", method.applicableCategory())
                .addValue("is_default", method.isDefault())
                .addValue("status", method.status())
                .addValue("merchant_id", method.merchantId())
                .addValue("created_at", offset(method.createdAt()))
                .addValue("updated_at", offset(method.updatedAt())));
        return method;
    }

    @Override
    public List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsBySupplier(String supplierId) {
        return jdbcTemplate.query("""
                SELECT id, supplier_id, settlement_method, payment_term_days, applicable_category,
                       is_default, status, merchant_id, created_at, updated_at
                  FROM supplier_settlement_methods
                 WHERE supplier_id = :supplier_id
                 ORDER BY is_default DESC, created_at DESC, id
                """, Map.of("supplier_id", supplierId), rowMapper::mapSupplierSettlementMethod);
    }

    @Override
    public List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, supplier_id, settlement_method, payment_term_days, applicable_category,
                       is_default, status, merchant_id, created_at, updated_at
                  FROM supplier_settlement_methods
                 WHERE merchant_id = :merchant_id
                 ORDER BY is_default DESC, created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapSupplierSettlementMethod);
    }

    @Override
    public List<SupplierSettlementMethodRecord> listSupplierSettlementMethods() {
        return jdbcTemplate.query("""
                SELECT id, supplier_id, settlement_method, payment_term_days, applicable_category,
                       is_default, status, merchant_id, created_at, updated_at
                  FROM supplier_settlement_methods
                 ORDER BY is_default DESC, created_at DESC, id
                """, rowMapper::mapSupplierSettlementMethod);
    }

    // ── V108/V126：盘点单（stocktake）+ 库存差异明细（inventory_variance）写入侧。 ──
    // 强制 merchant scope：stocktake 持 merchant_id；inventory_variance 无 merchant_id 列，经 JOIN stocktake 取值。

    @Override
    public StocktakeRecord saveStocktake(StocktakeRecord stocktake) {
        jdbcTemplate.update("""
                INSERT INTO stocktake (
                    id, stocktake_no, warehouse_id, merchant_id, business_date, status, remark,
                    created_at, updated_at
                )
                VALUES (
                    :id, :stocktake_no, :warehouse_id, :merchant_id, :business_date, :status, :remark,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    remark = EXCLUDED.remark,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", stocktake.id())
                .addValue("stocktake_no", stocktake.stocktakeNo())
                .addValue("warehouse_id", stocktake.warehouseId())
                .addValue("merchant_id", stocktake.merchantId())
                .addValue("business_date", stocktake.businessDate())
                .addValue("status", stocktake.status())
                .addValue("remark", stocktake.remark())
                .addValue("created_at", offset(stocktake.createdAt()))
                .addValue("updated_at", offset(stocktake.updatedAt())));
        return stocktake;
    }

    @Override
    public Optional<StocktakeRecord> findStocktakeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, stocktake_no, warehouse_id, merchant_id, business_date, status, remark,
                       created_at, updated_at
                  FROM stocktake
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapStocktake).stream().findFirst();
    }

    @Override
    public List<StocktakeRecord> listStocktakes() {
        return jdbcTemplate.query("""
                SELECT id, stocktake_no, warehouse_id, merchant_id, business_date, status, remark,
                       created_at, updated_at
                  FROM stocktake
                 ORDER BY created_at DESC, id
                """, rowMapper::mapStocktake);
    }

    @Override
    public List<StocktakeRecord> listStocktakesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, stocktake_no, warehouse_id, merchant_id, business_date, status, remark,
                       created_at, updated_at
                  FROM stocktake
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapStocktake);
    }

    @Override
    public boolean hasActiveStocktakeForWarehouse(String warehouseId) {
        // 2e 盘点冻结：该库房存在 counting/reviewing（未关闭）盘点单时，出入库须冻结，避免盘点期间库存变动致差异基线失真。
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM stocktake
                 WHERE warehouse_id = :warehouse_id
                   AND status IN ('counting', 'reviewing')
                """, Map.of("warehouse_id", warehouseId), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public InventoryVarianceRecord saveInventoryVariance(InventoryVarianceRecord variance) {
        jdbcTemplate.update("""
                INSERT INTO inventory_variance (
                    id, stocktake_id, item_id, batch_no, book_qty, counted_qty, variance_qty,
                    variance_amount_cent, reason, supplier_id, review_status, reviewed_by, reviewed_at,
                    posted_ar_ap_item_id, created_at, updated_at
                )
                VALUES (
                    :id, :stocktake_id, :item_id, :batch_no, :book_qty, :counted_qty, :variance_qty,
                    :variance_amount_cent, :reason, :supplier_id, :review_status, :reviewed_by, :reviewed_at,
                    :posted_ar_ap_item_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    review_status = EXCLUDED.review_status,
                    reviewed_by = EXCLUDED.reviewed_by,
                    reviewed_at = EXCLUDED.reviewed_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", variance.id())
                .addValue("stocktake_id", variance.stocktakeId())
                .addValue("item_id", variance.itemId())
                .addValue("batch_no", variance.batchNo())
                .addValue("book_qty", variance.bookQty())
                .addValue("counted_qty", variance.countedQty())
                .addValue("variance_qty", variance.varianceQty())
                .addValue("variance_amount_cent", variance.varianceAmountCent())
                .addValue("reason", variance.reason())
                .addValue("supplier_id", variance.supplierId())
                .addValue("review_status", variance.reviewStatus())
                .addValue("reviewed_by", variance.reviewedBy())
                .addValue("reviewed_at", offset(variance.reviewedAt()))
                .addValue("posted_ar_ap_item_id", variance.postedArApItemId())
                .addValue("created_at", offset(variance.createdAt()))
                .addValue("updated_at", offset(variance.updatedAt())));
        return variance;
    }

    @Override
    public Optional<InventoryVarianceRecord> findInventoryVarianceByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT v.id, v.stocktake_id, v.item_id, v.batch_no, v.book_qty, v.counted_qty,
                       v.variance_qty, v.variance_amount_cent, v.reason, v.supplier_id,
                       v.review_status, v.reviewed_by, v.reviewed_at, v.posted_ar_ap_item_id,
                       s.merchant_id, v.created_at, v.updated_at
                  FROM inventory_variance v
                  JOIN stocktake s ON s.id = v.stocktake_id
                 WHERE v.id = :id
                   FOR UPDATE OF v
                """, Map.of("id", id), rowMapper::mapInventoryVariance).stream().findFirst();
    }

    @Override
    public List<InventoryVarianceRecord> listInventoryVariancesByStocktake(String stocktakeId) {
        return jdbcTemplate.query("""
                SELECT v.id, v.stocktake_id, v.item_id, v.batch_no, v.book_qty, v.counted_qty,
                       v.variance_qty, v.variance_amount_cent, v.reason, v.supplier_id,
                       v.review_status, v.reviewed_by, v.reviewed_at, v.posted_ar_ap_item_id,
                       s.merchant_id, v.created_at, v.updated_at
                  FROM inventory_variance v
                  JOIN stocktake s ON s.id = v.stocktake_id
                 WHERE v.stocktake_id = :stocktake_id
                 ORDER BY v.created_at, v.id
                """, Map.of("stocktake_id", stocktakeId), rowMapper::mapInventoryVariance);
    }

    @Override
    public int reviewInventoryVarianceIfPending(
            String varianceId, String decision, String reviewedBy, Instant reviewedAt) {
        // V108 防并发重复复核：仅当 review_status='pending' 才置终态，受影响行数==1 才视为成功。
        return jdbcTemplate.update("""
                UPDATE inventory_variance
                   SET review_status = :decision,
                       reviewed_by = :reviewed_by,
                       reviewed_at = :reviewed_at,
                       updated_at = now()
                 WHERE id = :id
                   AND review_status = 'pending'
                """, new MapSqlParameterSource()
                .addValue("id", varianceId)
                .addValue("decision", decision)
                .addValue("reviewed_by", reviewedBy)
                .addValue("reviewed_at", offset(reviewedAt)));
    }

    // ── V128 / REQ-39：溯源码主数据（inventory_trace_codes）。强制 merchant scope（直接归属列）。 ──
    @Override
    public InventoryTraceCodeRecord saveTraceCode(InventoryTraceCodeRecord traceCode) {
        jdbcTemplate.update("""
                INSERT INTO inventory_trace_codes (
                    id, trace_code, item_id, batch_id, merchant_id, status, bound_ref,
                    printed_at, created_by, created_at, updated_at
                )
                VALUES (
                    :id, :trace_code, :item_id, :batch_id, :merchant_id, :status, :bound_ref,
                    :printed_at, :created_by, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    bound_ref = EXCLUDED.bound_ref,
                    printed_at = EXCLUDED.printed_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", traceCode.id())
                .addValue("trace_code", traceCode.traceCode())
                .addValue("item_id", traceCode.itemId())
                .addValue("batch_id", traceCode.batchId())
                .addValue("merchant_id", traceCode.merchantId())
                .addValue("status", traceCode.status())
                .addValue("bound_ref", traceCode.boundRef())
                .addValue("printed_at", offset(traceCode.printedAt()))
                .addValue("created_by", traceCode.createdBy())
                .addValue("created_at", offset(traceCode.createdAt()))
                .addValue("updated_at", offset(traceCode.updatedAt())));
        return traceCode;
    }

    @Override
    public Optional<InventoryTraceCodeRecord> findTraceCodeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, trace_code, item_id, batch_id, merchant_id, status, bound_ref,
                       printed_at, created_by, created_at, updated_at
                  FROM inventory_trace_codes
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTraceCode).stream().findFirst();
    }

    @Override
    public Optional<InventoryTraceCodeRecord> findTraceCodeByCode(String traceCode) {
        return jdbcTemplate.query("""
                SELECT id, trace_code, item_id, batch_id, merchant_id, status, bound_ref,
                       printed_at, created_by, created_at, updated_at
                  FROM inventory_trace_codes
                 WHERE trace_code = :trace_code
                """, Map.of("trace_code", traceCode), rowMapper::mapTraceCode).stream().findFirst();
    }

    @Override
    public List<InventoryTraceCodeRecord> listTraceCodes() {
        return jdbcTemplate.query("""
                SELECT id, trace_code, item_id, batch_id, merchant_id, status, bound_ref,
                       printed_at, created_by, created_at, updated_at
                  FROM inventory_trace_codes
                 ORDER BY created_at DESC, id
                """, rowMapper::mapTraceCode);
    }

    @Override
    public List<InventoryTraceCodeRecord> listTraceCodesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, trace_code, item_id, batch_id, merchant_id, status, bound_ref,
                       printed_at, created_by, created_at, updated_at
                  FROM inventory_trace_codes
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapTraceCode);
    }

    @Override
    public int printTraceCodeIfRegistered(String id, Instant printedAt, String boundRef) {
        // V128 防重复打印：仅当 status='registered' 才置 printed + 回填 printed_at，受影响行数==1 才视为成功。
        return jdbcTemplate.update("""
                UPDATE inventory_trace_codes
                   SET status = 'printed',
                       printed_at = :printed_at,
                       bound_ref = COALESCE(:bound_ref, bound_ref),
                       updated_at = now()
                 WHERE id = :id
                   AND status = 'registered'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("printed_at", offset(printedAt))
                .addValue("bound_ref", boundRef));
    }

    // ── V277 跨主体流转 ──
    @Override
    public SupplyEntityRecord saveSupplyEntity(SupplyEntityRecord e) {
        jdbcTemplate.update("""
                INSERT INTO supply_entities (id, name, entity_type, level, status, settlement_account, created_by, created_at, updated_at)
                VALUES (:id, :name, :entity_type, :level, :status, :settlement_account, :created_by, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name, entity_type = EXCLUDED.entity_type, status = EXCLUDED.status,
                    settlement_account = EXCLUDED.settlement_account, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", e.id()).addValue("name", e.name()).addValue("entity_type", e.entityType())
                .addValue("level", e.level()).addValue("status", e.status())
                .addValue("settlement_account", e.settlementAccount()).addValue("created_by", e.createdBy())
                .addValue("created_at", offset(e.createdAt())).addValue("updated_at", offset(e.updatedAt())));
        return e;
    }

    @Override
    public List<SupplyEntityRecord> listSupplyEntities() {
        return jdbcTemplate.query("""
                SELECT id, name, entity_type, level, status, settlement_account, created_by, created_at, updated_at
                  FROM supply_entities ORDER BY created_at DESC, id
                """, (rs, n) -> mapSupplyEntity(rs));
    }

    @Override
    public Optional<SupplyEntityRecord> findSupplyEntityById(String id) {
        return jdbcTemplate.query("""
                SELECT id, name, entity_type, level, status, settlement_account, created_by, created_at, updated_at
                  FROM supply_entities WHERE id = :id
                """, Map.of("id", id), (rs, n) -> mapSupplyEntity(rs)).stream().findFirst();
    }

    private SupplyEntityRecord mapSupplyEntity(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SupplyEntityRecord(rs.getString("id"), rs.getString("name"), rs.getString("entity_type"),
                rs.getString("level"), rs.getString("status"), rs.getString("settlement_account"),
                rs.getString("created_by"), toInstant(rs, "created_at"), toInstant(rs, "updated_at"));
    }

    @Override
    public InventoryTransferOrderRecord saveTransferOrder(InventoryTransferOrderRecord o) {
        jdbcTemplate.update("""
                INSERT INTO inventory_transfer_orders (id, transfer_type, lending_mode, from_owner_type, from_owner_id,
                    to_owner_type, to_owner_id, from_location_id, to_location_id, status, operator_note, created_by, created_at, updated_at)
                VALUES (:id, :transfer_type, :lending_mode, :from_owner_type, :from_owner_id,
                    :to_owner_type, :to_owner_id, :from_location_id, :to_location_id, :status, :operator_note, :created_by, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, operator_note = EXCLUDED.operator_note, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", o.id()).addValue("transfer_type", o.transferType()).addValue("lending_mode", o.lendingMode())
                .addValue("from_owner_type", o.fromOwnerType()).addValue("from_owner_id", o.fromOwnerId())
                .addValue("to_owner_type", o.toOwnerType()).addValue("to_owner_id", o.toOwnerId())
                .addValue("from_location_id", o.fromLocationId()).addValue("to_location_id", o.toLocationId())
                .addValue("status", o.status()).addValue("operator_note", o.operatorNote()).addValue("created_by", o.createdBy())
                .addValue("created_at", offset(o.createdAt())).addValue("updated_at", offset(o.updatedAt())));
        return o;
    }

    @Override
    public void saveTransferLine(InventoryTransferLineRecord l) {
        jdbcTemplate.update("""
                INSERT INTO inventory_transfer_lines (id, transfer_order_id, item_id, quantity, trace_codes, unit_price_cent, created_at)
                VALUES (:id, :transfer_order_id, :item_id, :quantity, CAST(:trace_codes AS jsonb), :unit_price_cent, :created_at)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", l.id()).addValue("transfer_order_id", l.transferOrderId()).addValue("item_id", l.itemId())
                .addValue("quantity", l.quantity()).addValue("trace_codes", writeList(l.traceCodes()))
                .addValue("unit_price_cent", l.unitPriceCent()).addValue("created_at", offset(l.createdAt())));
    }

    @Override
    public Optional<InventoryTransferOrderRecord> findTransferOrderById(String id) {
        return jdbcTemplate.query("""
                SELECT id, transfer_type, lending_mode, from_owner_type, from_owner_id, to_owner_type, to_owner_id,
                       from_location_id, to_location_id, status, operator_note, created_by, created_at, updated_at
                  FROM inventory_transfer_orders WHERE id = :id
                """, Map.of("id", id), (rs, n) -> mapTransferOrder(rs)).stream().findFirst();
    }

    @Override
    public List<InventoryTransferOrderRecord> listTransferOrders() {
        return jdbcTemplate.query("""
                SELECT id, transfer_type, lending_mode, from_owner_type, from_owner_id, to_owner_type, to_owner_id,
                       from_location_id, to_location_id, status, operator_note, created_by, created_at, updated_at
                  FROM inventory_transfer_orders ORDER BY created_at DESC, id
                """, (rs, n) -> mapTransferOrder(rs));
    }

    @Override
    public List<InventoryTransferLineRecord> listTransferLinesByOrder(String orderId) {
        return jdbcTemplate.query("""
                SELECT id, transfer_order_id, item_id, quantity, trace_codes, unit_price_cent, created_at
                  FROM inventory_transfer_lines WHERE transfer_order_id = :oid ORDER BY created_at, id
                """, Map.of("oid", orderId), (rs, n) -> mapTransferLine(rs));
    }

    private InventoryTransferOrderRecord mapTransferOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new InventoryTransferOrderRecord(rs.getString("id"), rs.getString("transfer_type"), rs.getString("lending_mode"),
                rs.getString("from_owner_type"), rs.getString("from_owner_id"), rs.getString("to_owner_type"), rs.getString("to_owner_id"),
                rs.getString("from_location_id"), rs.getString("to_location_id"), rs.getString("status"), rs.getString("operator_note"),
                rs.getString("created_by"), toInstant(rs, "created_at"), toInstant(rs, "updated_at"), List.of());
    }

    private InventoryTransferLineRecord mapTransferLine(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new InventoryTransferLineRecord(rs.getString("id"), rs.getString("transfer_order_id"), rs.getString("item_id"),
                rs.getInt("quantity"), readStringList(rs.getString("trace_codes")), rs.getLong("unit_price_cent"), toInstant(rs, "created_at"));
    }

    @Override
    public void saveTraceOwnershipHistory(InventoryTraceOwnershipHistoryRecord h) {
        jdbcTemplate.update("""
                INSERT INTO inventory_trace_ownership_history (id, trace_code, from_owner_type, from_owner_id, to_owner_type, to_owner_id, transfer_order_id, changed_at)
                VALUES (:id, :trace_code, :from_owner_type, :from_owner_id, :to_owner_type, :to_owner_id, :transfer_order_id, :changed_at)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", h.id()).addValue("trace_code", h.traceCode())
                .addValue("from_owner_type", h.fromOwnerType()).addValue("from_owner_id", h.fromOwnerId())
                .addValue("to_owner_type", h.toOwnerType()).addValue("to_owner_id", h.toOwnerId())
                .addValue("transfer_order_id", h.transferOrderId()).addValue("changed_at", offset(h.changedAt())));
    }

    @Override
    public boolean traceCodeExists(String traceCode) {
        Boolean e = jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM inventory_trace_codes WHERE trace_code = :tc)
                """, new MapSqlParameterSource().addValue("tc", traceCode), Boolean.class);
        return Boolean.TRUE.equals(e);
    }

    @Override
    public int updateTraceCodeOwnerIfExists(String traceCode, String fromOwnerId, String ownerType, String ownerId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE inventory_trace_codes SET owner_type = :ot, owner_id = :oid, updated_at = :now
                 WHERE trace_code = :tc AND (owner_id = :fromId OR owner_id IS NULL)
                """, new MapSqlParameterSource().addValue("ot", ownerType).addValue("oid", ownerId)
                .addValue("now", offset(now)).addValue("tc", traceCode).addValue("fromId", fromOwnerId));
    }

    private Instant toInstant(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        OffsetDateTime v = rs.getObject(col, OffsetDateTime.class);
        return v == null ? null : v.toInstant();
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("transfer line trace_codes not deserializable", e);
        }
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
