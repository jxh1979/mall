package com.complex.platform.inventory;

// 文件版本：V0.17

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

    @Override
    public Optional<InventoryQuarantineLotRecord> findQuarantineLotById(String id) {
        return jdbcTemplate.query("SELECT * FROM inventory_quarantine_lots WHERE id=:id", Map.of("id", id), (rs, n) -> {
            try {
                return new InventoryQuarantineLotRecord(rs.getString("id"), rs.getString("stock_balance_id"), rs.getString("item_id"),
                        rs.getString("warehouse_id"), rs.getString("project_id"), rs.getString("source_type"), rs.getString("source_id"),
                        rs.getInt("quantity"), rs.getInt("remaining_quantity"),
                        objectMapper.readValue(rs.getString("evidence_attachment_ids"), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}),
                        rs.getString("received_by"), com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "received_at"));
            } catch (JsonProcessingException e) {
                throw new java.sql.SQLException("隔离来源凭证格式无效", e);
            }
        }).stream().findFirst();
    }

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
        // 第十八批 I 段第 76 条：规格容量只随新建物料落库；更新段不带 spec_volume_ml——手工标记高价值、改安全库存用不带容量的
        // 写法重建记录再 upsert，带上会把容量清空。
        jdbcTemplate.update("""
                INSERT INTO inventory_items (
                    id, sku_code, item_name, item_type, unit, barcode, trace_required,
                    min_quantity, status, created_at, updated_at, merchant_id, manual_high_value, non_consumable, spec_volume_ml
                )
                VALUES (
                    :id, :sku_code, :item_name, :item_type, :unit, :barcode, :trace_required,
                    :min_quantity, :status, :created_at, :updated_at, :merchant_id, :manual_high_value, :non_consumable, :spec_volume_ml
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
                    manual_high_value = EXCLUDED.manual_high_value,
                    non_consumable = EXCLUDED.non_consumable
                """, itemParameters(item));
        return item;
    }

    @Override
    public List<InventoryItemRecord> listInventoryItems() {
        return jdbcTemplate.query("""
                SELECT id, sku_code, item_name, item_type, unit, barcode, trace_required,
                       min_quantity, status, created_at, updated_at, merchant_id, manual_high_value, non_consumable, spec_volume_ml
                  FROM inventory_items
                 ORDER BY created_at DESC, id
                """, rowMapper::mapInventoryItem);
    }

    @Override
    public Optional<InventoryItemRecord> findInventoryItemById(String id) {
        return jdbcTemplate.query("""
                SELECT id, sku_code, item_name, item_type, unit, barcode, trace_required,
                       min_quantity, status, created_at, updated_at, merchant_id, manual_high_value, non_consumable, spec_volume_ml
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
    public Optional<StockBalanceRecord> findStockBalance(String itemId, String locationId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id,
                       opened_volume_ml
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
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id,
                       opened_volume_ml
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
        return saveStockMovement(movement, projectId, false);
    }

    @Override
    public StockMovementRecord saveSourceReturnMovement(StockMovementRecord movement, String projectId) {
        // 第十八批第 18 条：借用归还（lend_return）同为按原出库成本恢复的实物回库。
        if (InventoryCostMath.direction(movement.sourceLocationId(), movement.targetLocationId()) != InventoryCostMath.Direction.INBOUND
                || !("return".equals(movement.movementType()) || "lend_return".equals(movement.movementType()))) {
            throw new IllegalArgumentException("原出库成本仅可用于实物退回入库");
        }
        return saveStockMovement(movement, projectId, true);
    }

    @Override
    public Optional<Long> findStockMovementTotalCostCent(String movementId) {
        return jdbcTemplate.query("SELECT total_cost_cent FROM inventory_stock_movements WHERE id = :id", Map.of("id", movementId),
                (rs, n) -> rs.getObject("total_cost_cent", Long.class)).stream().filter(java.util.Objects::nonNull).findFirst();
    }

    private StockMovementRecord saveStockMovement(StockMovementRecord movement, String projectId, boolean sourceReturn) {
        // V296 成本台账：所有流水写入的单一汇聚点，在此统一按移动加权口径计成本（各入口无须自理）。
        // 先 FOR UPDATE 锁状态行（同物料并发出入库串行化；写路径均在 @Transactional 内），
        // 再查流水 id 是否已存在——幂等重放不得二次滚动状态、保留库中已定稿的成本列。
        initCostStateIfAbsent(movement.itemId());
        Optional<InventoryItemCostStateRecord> state = lockCostStateForUpdate(movement.itemId());
        if (stockMovementRowExists(movement.id())) {
            return replayExisting(movement, projectId);
        }
        if (sourceReturn && state.isEmpty()) {
            throw com.complex.platform.common.api.ApiException.conflict("INVENTORY_RETURN_COST_SOURCE_MISSING", "退回商品缺少有效成本台账");
        }
        InventoryCostMath.Result result = state
                .map(current -> sourceReturn ? InventoryCostMath.sourceReturn(current, movement.quantity(), movement.totalCostCent(), Instant.now()) : InventoryCostMath.apply(
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
        StockMovementRecord stamped = withCosts(movement, result.unitCostCent(), result.totalCostCent());
        // I-3：并发同 id 先到先赢用 ON CONFLICT (id) DO NOTHING + 行数判定，不能用 catch(DuplicateKeyException)——
        // PG 重复键错误会把整个事务置为中止态（25P02），事务内（酒吧改道路径）后续读回必死。
        // 冲突目标限定 (id)（表上唯一约束仅主键），不会误吞其它约束冲突。行数判定必须先于滚状态：
        // =1 本方落账继续滚状态；=0 别人已插入（跨物料同 id 的边缘路径，同物料已被状态行锁串行化）→
        // 转幂等重放读回（一致性校验→读回或 409），本方状态行未动，绝不 updateCostState。
        int inserted = jdbcTemplate.update("""
                INSERT INTO inventory_stock_movements (
                    id, movement_type, item_id, source_location_id, target_location_id, quantity,
                    business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                    status, created_at, merchant_id, project_id, unit_cost_cent, total_cost_cent, volume_ml, loss_volume_ml
                )
                VALUES (
                    :id, :movement_type, :item_id, :source_location_id, :target_location_id, :quantity,
                    :business_type, :business_id, :trace_code, :operator_id, CAST(:evidence_attachment_ids AS jsonb),
                    :status, :created_at, :merchant_id, :project_id, :unit_cost_cent, :total_cost_cent, :volume_ml, :loss_volume_ml
                )
                ON CONFLICT (id) DO NOTHING
                """, stockMovementParameters(stamped).addValue("project_id", projectId));
        if (inserted == 0) {
            return replayExisting(movement, projectId);
        }
        if (result.newState() != null) {
            updateCostState(result.newState());
        }
        return stamped;
    }

    private boolean stockMovementRowExists(String id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_stock_movements WHERE id = :id", Map.of("id", id), Long.class);
        return count != null && count > 0;
    }

    /** I-3 幂等重放护栏：既存行的 item/数量/方向必须与本次一致，否则 409（拒绝借同 id 静默改写台账语义）。 */
    private record ReplayRow(String itemId, int quantity, String sourceLocationId, String targetLocationId,
            Long unitCostCent, Long totalCostCent) {
    }

    private StockMovementRecord replayExisting(StockMovementRecord movement, String projectId) {
        ReplayRow row = jdbcTemplate.query("""
                SELECT item_id, quantity, source_location_id, target_location_id, unit_cost_cent, total_cost_cent
                  FROM inventory_stock_movements
                 WHERE id = :id
                """, Map.of("id", movement.id()), (rs, i) -> new ReplayRow(
                        rs.getString("item_id"),
                        rs.getInt("quantity"),
                        rs.getString("source_location_id"),
                        rs.getString("target_location_id"),
                        rs.getObject("unit_cost_cent", Long.class),
                        rs.getObject("total_cost_cent", Long.class))).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("stock movement row vanished during replay: " + movement.id()));
        if (!row.itemId().equals(movement.itemId())
                || row.quantity() != movement.quantity()
                || !java.util.Objects.equals(row.sourceLocationId(), movement.sourceLocationId())
                || !java.util.Objects.equals(row.targetLocationId(), movement.targetLocationId())) {
            throw com.complex.platform.common.api.ApiException.conflict("STOCK_MOVEMENT_REPLAY_MISMATCH",
                    "库存变动编号已存在且商品/数量/方向不一致");
        }
        // 一致性通过：更新其余字段（既有 upsert 语义），成本列与状态行保持不动（不二次滚动）。
        jdbcTemplate.update("""
                UPDATE inventory_stock_movements SET
                    movement_type = :movement_type,
                    business_type = :business_type,
                    business_id = :business_id,
                    trace_code = :trace_code,
                    operator_id = :operator_id,
                    evidence_attachment_ids = CAST(:evidence_attachment_ids AS jsonb),
                    status = :status,
                    project_id = COALESCE(:project_id, project_id)
                WHERE id = :id
                """, stockMovementParameters(movement).addValue("project_id", projectId));
        return withCosts(movement, row.unitCostCent(), row.totalCostCent());
    }

    private StockMovementRecord withCosts(StockMovementRecord movement, Long unitCostCent, Long totalCostCent) {
        return new StockMovementRecord(
                movement.id(), movement.movementType(), movement.itemId(), movement.sourceLocationId(),
                movement.targetLocationId(), movement.quantity(), unitCostCent, totalCostCent,
                movement.businessType(), movement.businessId(), movement.traceCode(), movement.operatorId(),
                movement.evidenceAttachmentIds(), movement.status(), movement.createdAt(), movement.merchantId(),
                movement.volumeMl(), movement.lossVolumeMl());
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

    /** V350 高价值判定用：不加锁读成本状态行（进货价来源）。判定是只读旁路，不能拿行锁卡住出入库写路径。 */
    @Override
    public Optional<InventoryItemCostStateRecord> findCostState(String itemId) {
        return jdbcTemplate.query("""
                SELECT item_id, merchant_id, on_hand_quantity, total_cost_cent, avg_unit_cost_cent,
                       cumulative_in_cost_cent, cumulative_out_cost_cent, cost_method, has_cost_basis, updated_at
                  FROM inventory_item_cost_state
                 WHERE item_id = :item_id
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

    /**
     * 高价值判定用：该物品最近一次采购入库流水的实际到货单价（分）。只认 movement_type=inbound 且
     * business_type=purchase_request（采购单 received 时写入），按 created_at 取最近一笔；
     * 无此流水或该笔 unit_cost_cent 为空 ⇒ 空 = 到货价未知。不加锁，不卡出入库写路径。
     */
    @Override
    public Optional<Long> findLatestPurchaseArrivalUnitCostCent(String itemId) {
        List<Long> rows = jdbcTemplate.query("""
                SELECT unit_cost_cent
                  FROM inventory_stock_movements
                 WHERE item_id = :item_id
                   AND movement_type = 'inbound'
                   AND business_type = 'purchase_request'
                 ORDER BY created_at DESC
                 LIMIT 1
                """, Map.of("item_id", itemId), (rs, i) -> rs.getObject("unit_cost_cent", Long.class));
        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
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
                       status, created_at, merchant_id, unit_cost_cent, total_cost_cent, volume_ml, loss_volume_ml
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
        // 第十八批第 18 条复核：借出（lend_out）不是领用或消耗，借出方仍是所有权人（BSM:1201），不计入出库成本。
        return jdbcTemplate.query("""
                SELECT COUNT(*) AS movement_count,
                       COALESCE(SUM(total_cost_cent), 0) AS total_cost,
                       COUNT(*) FILTER (WHERE total_cost_cent IS NULL) AS no_basis_count
                  FROM inventory_stock_movements
                 WHERE source_location_id IS NOT NULL
                   AND target_location_id IS NULL
                   AND movement_type <> 'lend_out'
                   AND (CAST(:merchant_id AS VARCHAR) IS NULL OR merchant_id = :merchant_id)
                   AND (:project_id IS NULL OR project_id = :project_id)
                   AND created_at >= :from
                   AND created_at < :to
                """, params, (rs, i) -> new InventoryCostSummaryRecord(
                        merchantId, periodStart, periodEnd,
                        rs.getLong("total_cost"),
                        rs.getLong("movement_count"),
                        rs.getLong("no_basis_count"))).getFirst();
    }

    // 第十八批 K 段 97-R3（BSM:1168、BSM:1065、BSM:1068）：酒吧＝商户（V267，同 findBarProjectId）。只计售卖的酒水：赠送明细与小吃不计；
    // 回库退品的数量与出酒量记 0、损耗不退；销售形式＝有套餐来源为套餐，否则有标准出酒量为按杯，否则按瓶（BSM:1065 点酒方式只有这三种）。
    @Override
    public List<BarConsumptionStatRecord> listBarConsumptionStats(String merchantId, String projectId, Instant from, Instant to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_id", projectId, java.sql.Types.VARCHAR)
                .addValue("from", OffsetDateTime.ofInstant(from, ZoneOffset.UTC))
                .addValue("to", OffsetDateTime.ofInstant(to, ZoneOffset.UTC));
        return jdbcTemplate.query("""
                SELECT s.bar_id, MAX(s.bar_name) AS bar_name, s.sale_form, s.combo_id, MAX(s.combo_name) AS combo_name,
                       s.product_id, MAX(s.product_name) AS product_name, s.inventory_item_id, MAX(s.item_name) AS item_name,
                       SUM(s.quantity) AS quantity, SUM(s.pour_volume) AS pour_volume_ml, SUM(s.loss_volume) AS loss_volume_ml
                  FROM (
                        SELECT o.bar_id, m.merchant_name AS bar_name,
                               CASE WHEN i.combo_id IS NOT NULL THEN 'combo'
                                    WHEN p.standard_pour_volume IS NOT NULL THEN 'by_glass'
                                    ELSE 'by_bottle' END AS sale_form,
                               i.combo_id, c.combo_name, i.product_id, p.product_name, p.inventory_item_id, ii.item_name,
                               CASE WHEN i.voided AND i.return_to_stock THEN 0 ELSE i.quantity END AS quantity,
                               CASE WHEN i.voided AND i.return_to_stock THEN 0 ELSE i.actual_pour_volume END AS pour_volume,
                               i.loss_volume
                          FROM bar_order_items i
                          JOIN bar_orders o ON o.id = i.order_id
                          JOIN bar_products p ON p.id = i.product_id
                          JOIN merchants m ON m.id = o.bar_id
                          LEFT JOIN bar_combos c ON c.id = i.combo_id
                          LEFT JOIN inventory_items ii ON ii.id = p.inventory_item_id
                         WHERE i.delivered_at >= :from
                           AND i.delivered_at < :to
                           AND i.is_gift = FALSE
                           AND p.product_type <> 'snack'
                           AND (CAST(:merchant_id AS VARCHAR) IS NULL OR o.merchant_id = :merchant_id)
                           AND (CAST(:project_id AS VARCHAR) IS NULL OR m.project_id = :project_id)
                       ) s
                 GROUP BY s.bar_id, s.sale_form, s.combo_id, s.product_id, s.inventory_item_id
                 ORDER BY s.bar_id, s.sale_form, s.combo_id NULLS FIRST, s.product_id
                """, params, (rs, n) -> new BarConsumptionStatRecord(
                        rs.getString("bar_id"), rs.getString("bar_name"), rs.getString("sale_form"),
                        rs.getString("combo_id"), rs.getString("combo_name"),
                        rs.getString("product_id"), rs.getString("product_name"),
                        rs.getString("inventory_item_id"), rs.getString("item_name"),
                        rs.getLong("quantity"),
                        rs.getObject("pour_volume_ml") == null ? null : rs.getLong("pour_volume_ml"),
                        rs.getLong("loss_volume_ml")));
    }

    @Override
    public List<StockMovementRecord> listStockMovements() {
        return jdbcTemplate.query("""
                SELECT id, movement_type, item_id, source_location_id, target_location_id, quantity,
                       business_type, business_id, trace_code, operator_id, evidence_attachment_ids,
                       status, created_at, merchant_id, unit_cost_cent, total_cost_cent, volume_ml, loss_volume_ml
                  FROM inventory_stock_movements
                 ORDER BY created_at DESC, id
                """, rowMapper::mapStockMovement);
    }

    // AUD-013 真分页: SQL 层 LIMIT/OFFSET + COUNT, total 取真值而非 items.size()。
    // 租户谓词 (:x IS NULL OR col=:x), 参数带 SQL 类型供 PG 推断 NULL 分支。
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
                       status, created_at, merchant_id, unit_cost_cent, total_cost_cent, volume_ml, loss_volume_ml
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
                    transferred_at, transfer_to_name_or_nickname, transfer_to_phone, transfer_to_phone_hash,
                    remedy_of_saved_bottle_id, custodian_entity_id, pickup_location, handed_over_at, handover_id
                )
                VALUES (
                    :id, :customer_id, :bar_project_id, :item_id, :bottle_name, :initial_quantity,
                    :remaining_quantity, :unit, :expire_at, :status, CAST(:evidence_attachment_ids AS jsonb),
                    :created_at, :updated_at, :merchant_id,
                    :is_high_value, :high_value_threshold_cents, :bottle_label_value,
                    :customer_name_or_nickname, :customer_phone, :customer_phone_hash,
                    CAST(:registration_photo_refs AS jsonb), :manual_note,
                    :transferred_at, :transfer_to_name_or_nickname, :transfer_to_phone, :transfer_to_phone_hash,
                    :remedy_of_saved_bottle_id, :custodian_entity_id, :pickup_location, :handed_over_at, :handover_id
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
                    transfer_to_phone_hash = EXCLUDED.transfer_to_phone_hash,
                    remedy_of_saved_bottle_id = EXCLUDED.remedy_of_saved_bottle_id,
                    custodian_entity_id = EXCLUDED.custodian_entity_id,
                    pickup_location = EXCLUDED.pickup_location,
                    handed_over_at = EXCLUDED.handed_over_at,
                    handover_id = EXCLUDED.handover_id
                """, savedBottleParameters(savedBottle));
        return savedBottle;
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
    public List<SavedBottleRecord> listExpiredSavedBottlesPendingStock(String bottleId) {
        return jdbcTemplate.query("""
                SELECT sb.*
                  FROM saved_bottles sb
                 WHERE sb.status = 'expired'
                   AND sb.remaining_quantity > 0
                   AND (CAST(:id AS VARCHAR) IS NULL OR sb.id = :id)
                   AND NOT EXISTS (
                        SELECT 1 FROM inventory_stock_movements m
                         WHERE m.item_id = sb.item_id
                           AND m.business_type = 'saved_bottle_to_stock'
                           AND m.business_id = sb.id)
                 ORDER BY sb.updated_at ASC, sb.id
                   FOR UPDATE OF sb
                """, new MapSqlParameterSource().addValue("id", bottleId), rowMapper::mapSavedBottle);
    }

    @Override
    public SavedBottleClaimRecord saveSavedBottleClaim(SavedBottleClaimRecord claim) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_claims (
                    id, saved_bottle_id, claim_quantity, claim_staff_id, claim_terminal_type,
                    claim_device_id, status, evidence_attachment_ids, created_at,
                    remaining_before, remaining_after, verification_method, verified_phone_hash, pickup_location
                )
                VALUES (
                    :id, :saved_bottle_id, :claim_quantity, :claim_staff_id, :claim_terminal_type,
                    :claim_device_id, :status, CAST(:evidence_attachment_ids AS jsonb), :created_at,
                    :remaining_before, :remaining_after, :verification_method, :verified_phone_hash, :pickup_location
                )
                ON CONFLICT (id) DO UPDATE SET
                    saved_bottle_id = EXCLUDED.saved_bottle_id,
                    claim_quantity = EXCLUDED.claim_quantity,
                    claim_staff_id = EXCLUDED.claim_staff_id,
                    claim_terminal_type = EXCLUDED.claim_terminal_type,
                    claim_device_id = EXCLUDED.claim_device_id,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    remaining_before = EXCLUDED.remaining_before,
                    remaining_after = EXCLUDED.remaining_after,
                    verification_method = EXCLUDED.verification_method,
                    verified_phone_hash = EXCLUDED.verified_phone_hash,
                    pickup_location = EXCLUDED.pickup_location
                """, savedBottleClaimParameters(claim));
        return claim;
    }

    @Override
    public TraceHandoverRecord saveTraceHandover(TraceHandoverRecord handover) {
        jdbcTemplate.update("""
                INSERT INTO inventory_trace_handovers (
                    id, trace_code, item_id, from_staff_id, to_staff_id, device_id,
                    handover_result, overall_status, current_segment, evidence_attachment_ids, created_at, merchant_id, order_id
                )
                VALUES (
                    :id, :trace_code, :item_id, :from_staff_id, :to_staff_id, :device_id,
                    :handover_result, :overall_status, :current_segment, CAST(:evidence_attachment_ids AS jsonb), :created_at, :merchant_id, :order_id
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
                       handover_result, overall_status, current_segment, evidence_attachment_ids, created_at, merchant_id, order_id
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
    public List<TraceHandoverExceptionRecord> listTraceHandoverExceptions(String handoverId) {
        return jdbcTemplate.query("""
                SELECT id, handover_id, segment, exception_type, review_status,
                       reviewer_user_id, reason, created_at, reviewed_at
                  FROM inventory_trace_handover_exceptions
                 WHERE handover_id = :handover_id
                 ORDER BY created_at, id
                """, Map.of("handover_id", handoverId), rowMapper::mapTraceHandoverException);
    }

    @Override
    public List<TraceHandoverRecord> listTraceHandoversByTraceCode(String traceCode, String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, trace_code, item_id, from_staff_id, to_staff_id, device_id,
                       handover_result, overall_status, current_segment, evidence_attachment_ids, created_at, merchant_id, order_id
                  FROM inventory_trace_handovers
                 WHERE trace_code = :trace_code
                """ + (merchantId == null ? "" : " AND merchant_id = :merchant_id") + " ORDER BY created_at DESC, id",
                new MapSqlParameterSource("trace_code", traceCode).addValue("merchant_id", merchantId),
                rowMapper::mapTraceHandover);
    }

    /** 第十八批 K 段第 94 条 94-R1：同 countValidTableOrders 直读酒吧表；按杯商品（有标准出酒量）出品时按容量扣同一物料，不是整瓶送到客人，不列。 */
    @Override
    public List<TraceOutboundOrderRecord> listTraceOutboundOrders(String itemId, String orderId, String merchantId, String projectId) {
        return jdbcTemplate.query("""
                SELECT o.id AS order_id, t.table_name, string_agg(DISTINCT i.product_name, '、') AS product_name,
                       SUM(i.quantity) AS quantity, o.created_at
                  FROM bar_orders o
                  JOIN bar_order_items i ON i.order_id = o.id
                  JOIN bar_products p ON p.id = i.product_id
                  JOIN bar_tables t ON t.id = o.table_id
                  LEFT JOIN merchants m ON m.id = o.bar_id
                 WHERE p.inventory_item_id = :item_id
                   AND p.standard_pour_volume IS NULL
                   AND i.delivery_status = 'pending'
                   AND (CAST(:order_id AS VARCHAR) IS NULL OR o.id = :order_id)
                   AND (CAST(:merchant_id AS VARCHAR) IS NULL OR o.merchant_id = :merchant_id)
                   AND (CAST(:project_id AS VARCHAR) IS NULL OR m.project_id = :project_id)
                 GROUP BY o.id, t.table_name, o.created_at
                 ORDER BY o.created_at, o.id
                """, new MapSqlParameterSource()
                .addValue("item_id", itemId)
                .addValue("order_id", orderId)
                .addValue("merchant_id", merchantId)
                .addValue("project_id", projectId),
                (rs, n) -> new TraceOutboundOrderRecord(rs.getString("order_id"), rs.getString("table_name"),
                        rs.getString("product_name"), rs.getInt("quantity"), toInstant(rs, "created_at")));
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

    // V312 发券预占：available_quantity -> reserved_quantity（quantity 不变）。守卫式单语句 UPDATE：
    // WHERE available_quantity>=:qty 与行锁一并生效，并发第二笔阻塞到第一笔提交后按新值复核，从而不超卖。
    // 无商户 scope 谓词：预占是发券（含客人积分兑换，无商户上下文）触发的系统级台账操作，按 (item,location) 定位。
    // V012 CHECK(reserved_quantity>=0 / available_quantity>=0) 为负库存兜底硬闸。
    @Override
    public int reserveStockBalance(String itemId, String locationId, int qty) {
        return jdbcTemplate.update("""
                UPDATE inventory_stock_balances
                   SET reserved_quantity = reserved_quantity + :qty,
                       available_quantity = available_quantity - :qty,
                       updated_at = NOW()
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                   AND available_quantity >= :qty
                """, new MapSqlParameterSource()
                .addValue("qty", qty).addValue("item_id", itemId).addValue("location_id", locationId));
    }

    // V312 释放预占：reserved_quantity -> available_quantity（quantity 不变）。守卫 reserved_quantity>=:qty
    // 防对未持预占的行误释放（历史券/漂移）。核销转实扣时先释放再走 redeem_consume（available->出库），净效果 reserved->出库。
    @Override
    public int releaseStockReservation(String itemId, String locationId, int qty) {
        return jdbcTemplate.update("""
                UPDATE inventory_stock_balances
                   SET reserved_quantity = reserved_quantity - :qty,
                       available_quantity = available_quantity + :qty,
                       updated_at = NOW()
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                   AND reserved_quantity >= :qty
                """, new MapSqlParameterSource()
                .addValue("qty", qty).addValue("item_id", itemId).addValue("location_id", locationId));
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
                       min_quantity, status, created_at, updated_at, merchant_id, manual_high_value, non_consumable, spec_volume_ml
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
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id,
                       opened_volume_ml
                  FROM inventory_stock_balances
                 WHERE merchant_id = :merchant_id
                 ORDER BY updated_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapStockBalance);
    }

    // C2: 项目级读隔离——项目员工（无商户 scope、有项目 scope）只读本项目余额，不越读他项目全平台库存。
    @Override
    public List<StockBalanceRecord> listStockBalancesByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id,
                       opened_volume_ml
                  FROM inventory_stock_balances
                 WHERE project_id = :project_id
                 ORDER BY updated_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapStockBalance);
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

    // ===== FS-016 存酒持有人核验 =====

    @Override
    public int clearSavedBottleCustomerIdentity(String customerId, String phoneHash) {
        return jdbcTemplate.update("""
                UPDATE saved_bottles SET customer_name_or_nickname = NULL, customer_phone = NULL, customer_phone_hash = NULL
                 WHERE customer_id = :customer_id
                    OR (CAST(:phone_hash AS VARCHAR) IS NOT NULL AND customer_phone_hash = :phone_hash)
                """, new MapSqlParameterSource().addValue("customer_id", customerId).addValue("phone_hash", phoneHash));
    }

    @Override
    public List<SavedBottleRecord> listStoredSavedBottlesByHolder(String customerId, String phoneHash) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM saved_bottles
                 WHERE status = 'stored'
                   AND ((CAST(:customer_id AS VARCHAR) IS NOT NULL AND customer_id = :customer_id)
                     OR (CAST(:phone_hash AS VARCHAR) IS NOT NULL AND customer_phone_hash = :phone_hash))
                 ORDER BY expire_at ASC NULLS LAST, created_at DESC, id
                """, new MapSqlParameterSource()
                .addValue("customer_id", customerId)
                .addValue("phone_hash", phoneHash), rowMapper::mapSavedBottle);
    }

    @Override
    public List<SavedBottleRecord> listSavedBottlesByPhoneHash(String phoneHash, String merchantId) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM saved_bottles
                 WHERE customer_phone_hash = :phone_hash
                   AND (CAST(:merchant_id AS VARCHAR) IS NULL OR merchant_id = :merchant_id)
                 ORDER BY created_at DESC, id
                """, new MapSqlParameterSource()
                .addValue("phone_hash", phoneHash)
                .addValue("merchant_id", merchantId), rowMapper::mapSavedBottle);
    }

    @Override
    public SavedBottlePickupCodeRecord saveSavedBottlePickupCode(SavedBottlePickupCodeRecord code) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_pickup_codes (
                    id, saved_bottle_id, customer_id, code_hash, issued_at, expires_at,
                    refresh_interval_seconds, status, consumed_claim_id, created_at
                )
                VALUES (
                    :id, :saved_bottle_id, :customer_id, :code_hash, :issued_at, :expires_at,
                    :refresh_interval_seconds, :status, :consumed_claim_id, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    consumed_claim_id = EXCLUDED.consumed_claim_id
                """, new MapSqlParameterSource()
                .addValue("id", code.id())
                .addValue("saved_bottle_id", code.savedBottleId())
                .addValue("customer_id", code.customerId())
                .addValue("code_hash", code.codeHash())
                .addValue("issued_at", offset(code.issuedAt()))
                .addValue("expires_at", offset(code.expiresAt()))
                .addValue("refresh_interval_seconds", code.refreshIntervalSeconds())
                .addValue("status", code.status())
                .addValue("consumed_claim_id", code.consumedClaimId())
                .addValue("created_at", offset(code.createdAt())));
        return code;
    }

    @Override
    public Optional<SavedBottlePickupCodeRecord> findActiveSavedBottlePickupCode(String savedBottleId) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM saved_bottle_pickup_codes
                 WHERE saved_bottle_id = :saved_bottle_id AND status = 'active'
                 ORDER BY issued_at DESC
                 LIMIT 1
                 FOR UPDATE
                """, Map.of("saved_bottle_id", savedBottleId), rowMapper::mapSavedBottlePickupCode).stream().findFirst();
    }

    @Override
    public void revokeActiveSavedBottlePickupCodes(String savedBottleId) {
        jdbcTemplate.update("""
                UPDATE saved_bottle_pickup_codes SET status = 'revoked'
                 WHERE saved_bottle_id = :saved_bottle_id AND status = 'active'
                """, Map.of("saved_bottle_id", savedBottleId));
    }

    @Override
    public SavedBottleManualPickupRecord saveSavedBottleManualPickup(SavedBottleManualPickupRecord request) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_manual_pickups (
                    id, saved_bottle_id, merchant_id, project_id, customer_id, phone_hash, reason, history_condition,
                    initiated_by, initiated_at, reviewed_by, reviewed_at, marketing_confirmed_by, marketing_confirm_basis,
                    marketing_confirmed_at, dispensed_by, dispensed_at, claim_id, status, evidence_attachment_ids,
                    created_at, updated_at
                )
                VALUES (
                    :id, :saved_bottle_id, :merchant_id, :project_id, :customer_id, :phone_hash, :reason, :history_condition,
                    :initiated_by, :initiated_at, :reviewed_by, :reviewed_at, :marketing_confirmed_by, :marketing_confirm_basis,
                    :marketing_confirmed_at, :dispensed_by, :dispensed_at, :claim_id, :status, CAST(:evidence_attachment_ids AS jsonb),
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    reviewed_by = EXCLUDED.reviewed_by,
                    reviewed_at = EXCLUDED.reviewed_at,
                    marketing_confirmed_by = EXCLUDED.marketing_confirmed_by,
                    marketing_confirm_basis = EXCLUDED.marketing_confirm_basis,
                    marketing_confirmed_at = EXCLUDED.marketing_confirmed_at,
                    dispensed_by = EXCLUDED.dispensed_by,
                    dispensed_at = EXCLUDED.dispensed_at,
                    claim_id = EXCLUDED.claim_id,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", request.id())
                .addValue("saved_bottle_id", request.savedBottleId())
                .addValue("merchant_id", request.merchantId())
                .addValue("project_id", request.projectId())
                .addValue("customer_id", request.customerId())
                .addValue("phone_hash", request.phoneHash())
                .addValue("reason", request.reason())
                .addValue("history_condition", request.historyCondition())
                .addValue("initiated_by", request.initiatedBy())
                .addValue("initiated_at", offset(request.initiatedAt()))
                .addValue("reviewed_by", request.reviewedBy())
                .addValue("reviewed_at", offset(request.reviewedAt()))
                .addValue("marketing_confirmed_by", request.marketingConfirmedBy())
                .addValue("marketing_confirm_basis", request.marketingConfirmBasis())
                .addValue("marketing_confirmed_at", offset(request.marketingConfirmedAt()))
                .addValue("dispensed_by", request.dispensedBy())
                .addValue("dispensed_at", offset(request.dispensedAt()))
                .addValue("claim_id", request.claimId())
                .addValue("status", request.status())
                .addValue("evidence_attachment_ids", writeList(request.evidenceAttachmentIds()))
                .addValue("created_at", offset(request.createdAt()))
                .addValue("updated_at", offset(request.updatedAt())));
        return request;
    }

    @Override
    public Optional<SavedBottleManualPickupRecord> findSavedBottleManualPickupByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT * FROM saved_bottle_manual_pickups WHERE id = :id FOR UPDATE
                """, Map.of("id", id), rowMapper::mapSavedBottleManualPickup).stream().findFirst();
    }

    @Override
    public Optional<String> findWarehouseProjectId(String warehouseId) {
        return jdbcTemplate.query("SELECT project_id FROM warehouses WHERE id = :id", Map.of("id", warehouseId),
                (rs, n) -> rs.getString("project_id")).stream().findFirst();
    }

    @Override
    public Optional<String> findStockBalanceProjectId(String itemId, String locationId) {
        return jdbcTemplate.query("SELECT project_id FROM inventory_stock_balances WHERE item_id = :item_id AND location_id = :location_id",
                Map.of("item_id", itemId, "location_id", locationId), (rs, n) -> rs.getString("project_id"))
                .stream().filter(java.util.Objects::nonNull).findFirst();
    }

    @Override
    public Optional<String> findBarProjectId(String barId) {
        return jdbcTemplate.query("SELECT project_id FROM merchants WHERE id = :id", Map.of("id", barId),
                (rs, n) -> rs.getString("project_id")).stream().filter(java.util.Objects::nonNull).findFirst();
    }

    // ===== FS-025 存酒转存（延期） =====

    @Override
    public SavedBottleRenewalRecord saveSavedBottleRenewal(SavedBottleRenewalRecord renewal) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_renewals (
                    id, saved_bottle_id, merchant_id, project_id, customer_id, phone_hash, extension_days, previous_expire_at,
                    new_expire_at, renewal_count, max_renewals, limit_config_version, days_config_version, operator_id,
                    operation_source, confirmation_method, reviewed_by, reviewed_at, review_reason, status, created_at, updated_at
                )
                VALUES (
                    :id, :saved_bottle_id, :merchant_id, :project_id, :customer_id, :phone_hash, :extension_days, :previous_expire_at,
                    :new_expire_at, :renewal_count, :max_renewals, :limit_config_version, :days_config_version, :operator_id,
                    :operation_source, :confirmation_method, :reviewed_by, :reviewed_at, :review_reason, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    previous_expire_at = EXCLUDED.previous_expire_at,
                    new_expire_at = EXCLUDED.new_expire_at,
                    reviewed_by = EXCLUDED.reviewed_by,
                    reviewed_at = EXCLUDED.reviewed_at,
                    review_reason = EXCLUDED.review_reason,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", renewal.id())
                .addValue("saved_bottle_id", renewal.savedBottleId())
                .addValue("merchant_id", renewal.merchantId())
                .addValue("project_id", renewal.projectId())
                .addValue("customer_id", renewal.customerId())
                .addValue("phone_hash", renewal.phoneHash())
                .addValue("extension_days", renewal.extensionDays())
                .addValue("previous_expire_at", offset(renewal.previousExpireAt()))
                .addValue("new_expire_at", offset(renewal.newExpireAt()))
                .addValue("renewal_count", renewal.renewalCount())
                .addValue("max_renewals", renewal.maxRenewals())
                .addValue("limit_config_version", renewal.limitConfigVersion())
                .addValue("days_config_version", renewal.daysConfigVersion())
                .addValue("operator_id", renewal.operatorId())
                .addValue("operation_source", renewal.operationSource())
                .addValue("confirmation_method", renewal.confirmationMethod())
                .addValue("reviewed_by", renewal.reviewedBy())
                .addValue("reviewed_at", offset(renewal.reviewedAt()))
                .addValue("review_reason", renewal.reviewReason())
                .addValue("status", renewal.status())
                .addValue("created_at", offset(renewal.createdAt()))
                .addValue("updated_at", offset(renewal.updatedAt())));
        return renewal;
    }

    @Override
    public Optional<SavedBottleRenewalRecord> findSavedBottleRenewalByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT * FROM saved_bottle_renewals WHERE id = :id FOR UPDATE
                """, Map.of("id", id), rowMapper::mapSavedBottleRenewal).stream().findFirst();
    }

    @Override
    public List<SavedBottleRenewalRecord> listSavedBottleRenewals(String savedBottleId) {
        return jdbcTemplate.query("""
                SELECT * FROM saved_bottle_renewals WHERE saved_bottle_id = :saved_bottle_id ORDER BY created_at, id
                """, Map.of("saved_bottle_id", savedBottleId), rowMapper::mapSavedBottleRenewal);
    }

    @Override
    public Optional<SavedBottleRenewalPolicyRecord> findSavedBottleRenewalPolicy(String policyKey, String scopeId) {
        return jdbcTemplate.query("""
                SELECT * FROM saved_bottle_renewal_policy WHERE policy_key = :policy_key AND scope_id = :scope_id
                """, Map.of("policy_key", policyKey, "scope_id", scopeId), (rs, n) -> new SavedBottleRenewalPolicyRecord(
                        rs.getString("policy_key"), rs.getString("scope_id"), rs.getInt("numeric_value"),
                        rs.getObject("min_value", Integer.class), rs.getObject("max_value", Integer.class), rs.getInt("version")))
                .stream().findFirst();
    }

    @Override
    public SavedBottleRenewalPolicyRecord saveSavedBottleRenewalPolicy(SavedBottleRenewalPolicyRecord policy, String actorId) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_renewal_policy (policy_key, scope_id, numeric_value, min_value, max_value, version, updated_by, updated_at)
                VALUES (:policy_key, :scope_id, :value, :min, :max, 1, :actor, now())
                ON CONFLICT (policy_key, scope_id) DO UPDATE SET numeric_value = EXCLUDED.numeric_value,
                    min_value = EXCLUDED.min_value, max_value = EXCLUDED.max_value,
                    version = saved_bottle_renewal_policy.version + 1, updated_by = EXCLUDED.updated_by, updated_at = now()
                """, new MapSqlParameterSource()
                .addValue("policy_key", policy.policyKey()).addValue("scope_id", policy.scopeId())
                .addValue("value", policy.numericValue()).addValue("min", policy.minValue()).addValue("max", policy.maxValue())
                .addValue("actor", actorId));
        return findSavedBottleRenewalPolicy(policy.policyKey(), policy.scopeId()).orElseThrow();
    }

    @Override
    public Optional<SavedBottleManualPickupPolicyRecord> findSavedBottleManualPickupPolicy(String projectId) {
        return jdbcTemplate.query("""
                SELECT * FROM saved_bottle_manual_pickup_policy WHERE project_id = :project_id
                """, Map.of("project_id", projectId), (rs, n) -> new SavedBottleManualPickupPolicyRecord(
                        rs.getString("project_id"), rs.getInt("orders_within_1m"), rs.getInt("orders_within_3m"),
                        rs.getInt("orders_total"), rs.getObject("min_orders", Integer.class),
                        rs.getObject("max_orders", Integer.class)))
                .stream().findFirst();
    }

    @Override
    public Map<String, List<Integer>> listSavedBottleReminderPolicies() {
        return jdbcTemplate.query("SELECT scope_id, reminder_days FROM saved_bottle_reminder_policy", Map.of(),
                        (rs, n) -> Map.entry(rs.getString("scope_id"), java.util.Arrays.stream(rs.getString("reminder_days").split(","))
                                .map(Integer::valueOf).toList()))
                .stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public SavedBottleReminderPolicyRecord saveSavedBottleReminderPolicy(SavedBottleReminderPolicyRecord policy, String actorId) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_reminder_policy (scope_id, reminder_days, updated_by, updated_at)
                VALUES (:scope_id, :reminder_days, :actor, now())
                ON CONFLICT (scope_id) DO UPDATE SET reminder_days = EXCLUDED.reminder_days,
                    updated_by = EXCLUDED.updated_by, updated_at = now()
                """, new MapSqlParameterSource()
                .addValue("scope_id", policy.scopeId())
                .addValue("reminder_days", policy.reminderDays().stream().map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(",")))
                .addValue("actor", actorId));
        return policy;
    }

    @Override
    public void saveSavedBottleManualPickupPolicy(SavedBottleManualPickupPolicyRecord policy, String actorId) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_manual_pickup_policy (project_id, orders_within_1m, orders_within_3m, orders_total,
                    min_orders, max_orders, updated_by, updated_at)
                VALUES (:project_id, :m1, :m3, :total, :min, :max, :actor, now())
                ON CONFLICT (project_id) DO UPDATE SET orders_within_1m = EXCLUDED.orders_within_1m,
                    orders_within_3m = EXCLUDED.orders_within_3m, orders_total = EXCLUDED.orders_total,
                    min_orders = EXCLUDED.min_orders, max_orders = EXCLUDED.max_orders,
                    updated_by = EXCLUDED.updated_by, updated_at = now()
                """, new MapSqlParameterSource()
                .addValue("project_id", policy.projectId()).addValue("m1", policy.ordersWithin1m())
                .addValue("m3", policy.ordersWithin3m()).addValue("total", policy.ordersTotal())
                .addValue("min", policy.minOrders()).addValue("max", policy.maxOrders()).addValue("actor", actorId));
    }

    @Override
    public Optional<String> findDirectSuperior(String userId) {
        return jdbcTemplate.query("SELECT superior_id FROM org_hierarchy WHERE user_id = :user_id AND superior_id IS NOT NULL",
                Map.of("user_id", userId), (rs, n) -> rs.getString("superior_id")).stream().findFirst();
    }

    /**
     * 有效卡座消费订单（ledger §26：已支付且未全额退款）：bar_bills.payment_status 为已支付或部分退款（第十六批第 16 条起退款成功后
     * 改记 partial_refunded/refunded），退款减项（bar_bill_adjustments.adjustment_type='refund'）合计小于实付；按客户归属订单、酒吧所属项目计数。
     */
    @Override
    public int countValidTableOrders(String customerId, String projectId, Instant since) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM bar_bills b
                  JOIN bar_orders o ON o.id = b.order_id
                  JOIN merchants br ON br.id = b.bar_id
                 WHERE o.customer_id = :customer_id
                   AND br.project_id = :project_id
                   AND b.payment_status IN ('paid', 'partial_refunded')
                   AND (CAST(:since AS TIMESTAMPTZ) IS NULL OR COALESCE(b.paid_at, b.updated_at) >= CAST(:since AS TIMESTAMPTZ))
                   AND COALESCE((SELECT SUM(a.amount_cent) FROM bar_bill_adjustments a
                                  WHERE a.bill_id = b.id AND a.adjustment_type = 'refund'), 0) < b.paid_amount_cent
                """, new MapSqlParameterSource()
                .addValue("customer_id", customerId)
                .addValue("project_id", projectId)
                .addValue("since", offset(since)), Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public Optional<AttributedMarketer> findAttributedMarketer(String customerId, String projectId) {
        return jdbcTemplate.query("""
                SELECT b.sales_staff_id,
                       EXISTS (SELECT 1 FROM staff_profiles sp WHERE sp.user_id = b.sales_staff_id AND sp.status = 'active') AS active
                  FROM bar_bills b
                  JOIN bar_orders o ON o.id = b.order_id
                  JOIN merchants br ON br.id = b.bar_id
                 WHERE o.customer_id = :customer_id
                   AND br.project_id = :project_id
                   AND b.payment_status IN ('paid', 'partial_refunded', 'refunded')
                   AND b.sales_staff_id IS NOT NULL
                 ORDER BY COALESCE(b.paid_at, b.updated_at) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("customer_id", customerId)
                .addValue("project_id", projectId),
                (rs, n) -> new AttributedMarketer(rs.getString("sales_staff_id"), rs.getBoolean("active")))
                .stream().findFirst();
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
                .addValue("manual_high_value", item.manualHighValue())
                .addValue("non_consumable", item.nonConsumable())
                .addValue("spec_volume_ml", item.specVolumeMl());
    }

    private MapSqlParameterSource supplierParameters(InventorySupplierRecord supplier) {
        return new MapSqlParameterSource()
                .addValue("id", supplier.id())
                .addValue("supplier_name", supplier.supplierName())
                .addValue("supplier_type", supplier.supplierType())
                .addValue("contact_name", supplier.contactName())
                // P0-A/GAP-07：供应商联系人手机号 PII 落库加密（V339），范式同存酒客人手机号（同仓库 1328 行）。
                .addValue("contact_phone", piiCipher.encrypt(supplier.contactPhone()))
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
                .addValue("total_cost_cent", movement.totalCostCent())
                // 第十八批 I 段第 76 条：按杯按容量扣减的出酒量与损耗量（毫升）；按瓶为空。
                .addValue("volume_ml", movement.volumeMl())
                .addValue("loss_volume_ml", movement.lossVolumeMl());
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

    // ===== FS-017 存酒补存/赔付、门店退出交接 =====

    @Override
    public SavedBottleRemedyRecord saveSavedBottleRemedy(SavedBottleRemedyRecord remedy) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_remedies (
                    id, saved_bottle_id, merchant_id, project_id, remedy_type, replacement_saved_bottle_id, amount_cent,
                    loss_cost_cent, loss_ar_ap_item_id, bearer_type, bearer_id, supervisor_approved_by, supervisor_approved_at,
                    finance_approved_by, finance_approved_at, customer_delivery_status, delivered_at, delivery_evidence_ids, reason,
                    recovery_party_type, recovery_party_id, recovery_amount_cent, recovery_status, recovery_ar_ap_item_id,
                    advance_approved_by, advance_approved_at, recovery_evidence_ids, created_by, created_at, updated_at
                )
                VALUES (
                    :id, :saved_bottle_id, :merchant_id, :project_id, :remedy_type, :replacement_saved_bottle_id, :amount_cent,
                    :loss_cost_cent, :loss_ar_ap_item_id, :bearer_type, :bearer_id, :supervisor_approved_by, :supervisor_approved_at,
                    :finance_approved_by, :finance_approved_at, :customer_delivery_status, :delivered_at, CAST(:delivery_evidence_ids AS jsonb), :reason,
                    :recovery_party_type, :recovery_party_id, :recovery_amount_cent, :recovery_status, :recovery_ar_ap_item_id,
                    :advance_approved_by, :advance_approved_at, CAST(:recovery_evidence_ids AS jsonb), :created_by, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    loss_cost_cent = EXCLUDED.loss_cost_cent,
                    loss_ar_ap_item_id = EXCLUDED.loss_ar_ap_item_id,
                    supervisor_approved_by = EXCLUDED.supervisor_approved_by,
                    supervisor_approved_at = EXCLUDED.supervisor_approved_at,
                    finance_approved_by = EXCLUDED.finance_approved_by,
                    finance_approved_at = EXCLUDED.finance_approved_at,
                    customer_delivery_status = EXCLUDED.customer_delivery_status,
                    delivered_at = EXCLUDED.delivered_at,
                    delivery_evidence_ids = EXCLUDED.delivery_evidence_ids,
                    recovery_party_type = EXCLUDED.recovery_party_type,
                    recovery_party_id = EXCLUDED.recovery_party_id,
                    recovery_amount_cent = EXCLUDED.recovery_amount_cent,
                    recovery_status = EXCLUDED.recovery_status,
                    recovery_ar_ap_item_id = EXCLUDED.recovery_ar_ap_item_id,
                    advance_approved_by = EXCLUDED.advance_approved_by,
                    advance_approved_at = EXCLUDED.advance_approved_at,
                    recovery_evidence_ids = EXCLUDED.recovery_evidence_ids,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", remedy.id())
                .addValue("saved_bottle_id", remedy.savedBottleId())
                .addValue("merchant_id", remedy.merchantId())
                .addValue("project_id", remedy.projectId())
                .addValue("remedy_type", remedy.remedyType())
                .addValue("replacement_saved_bottle_id", remedy.replacementSavedBottleId())
                .addValue("amount_cent", remedy.amountCent())
                .addValue("loss_cost_cent", remedy.lossCostCent())
                .addValue("loss_ar_ap_item_id", remedy.lossArApItemId())
                .addValue("bearer_type", remedy.bearerType())
                .addValue("bearer_id", remedy.bearerId())
                .addValue("supervisor_approved_by", remedy.supervisorApprovedBy())
                .addValue("supervisor_approved_at", offset(remedy.supervisorApprovedAt()))
                .addValue("finance_approved_by", remedy.financeApprovedBy())
                .addValue("finance_approved_at", offset(remedy.financeApprovedAt()))
                .addValue("customer_delivery_status", remedy.customerDeliveryStatus())
                .addValue("delivered_at", offset(remedy.deliveredAt()))
                .addValue("delivery_evidence_ids", writeList(remedy.deliveryEvidenceIds()))
                .addValue("reason", remedy.reason())
                .addValue("recovery_party_type", remedy.recoveryPartyType())
                .addValue("recovery_party_id", remedy.recoveryPartyId())
                .addValue("recovery_amount_cent", remedy.recoveryAmountCent())
                .addValue("recovery_status", remedy.recoveryStatus())
                .addValue("recovery_ar_ap_item_id", remedy.recoveryArApItemId())
                .addValue("advance_approved_by", remedy.advanceApprovedBy())
                .addValue("advance_approved_at", offset(remedy.advanceApprovedAt()))
                .addValue("recovery_evidence_ids", writeList(remedy.recoveryEvidenceIds()))
                .addValue("created_by", remedy.createdBy())
                .addValue("created_at", offset(remedy.createdAt()))
                .addValue("updated_at", offset(remedy.updatedAt())));
        return remedy;
    }

    @Override
    public Optional<SavedBottleRemedyRecord> findSavedBottleRemedyByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT * FROM saved_bottle_remedies WHERE id = :id FOR UPDATE
                """, Map.of("id", id), rowMapper::mapSavedBottleRemedy).stream().findFirst();
    }

    @Override
    public List<SavedBottleRemedyRecord> listSavedBottleRemedies(String savedBottleId) {
        return jdbcTemplate.query("""
                SELECT * FROM saved_bottle_remedies WHERE saved_bottle_id = :saved_bottle_id ORDER BY created_at, id
                """, Map.of("saved_bottle_id", savedBottleId), rowMapper::mapSavedBottleRemedy);
    }

    @Override
    public Optional<SavedBottleRemedyRecord> findSavedBottleRemedyByRecoveryArApItemId(String arApItemId) {
        return jdbcTemplate.query("""
                SELECT * FROM saved_bottle_remedies WHERE recovery_ar_ap_item_id = :item_id FOR UPDATE
                """, Map.of("item_id", arApItemId), rowMapper::mapSavedBottleRemedy).stream().findFirst();
    }

    @Override
    public SavedBottleHandoverRecord saveSavedBottleHandover(SavedBottleHandoverRecord handover) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_handovers (
                    id, merchant_id, project_id, platform_entity_id, pickup_location, counted_total, received_intact,
                    damaged_or_short, photo_attachment_ids, video_attachment_ids, created_by, created_at
                )
                VALUES (
                    :id, :merchant_id, :project_id, :platform_entity_id, :pickup_location, :counted_total, :received_intact,
                    :damaged_or_short, CAST(:photo_attachment_ids AS jsonb), CAST(:video_attachment_ids AS jsonb), :created_by, :created_at
                )
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", handover.id())
                .addValue("merchant_id", handover.merchantId())
                .addValue("project_id", handover.projectId())
                .addValue("platform_entity_id", handover.platformEntityId())
                .addValue("pickup_location", handover.pickupLocation())
                .addValue("counted_total", handover.countedTotal())
                .addValue("received_intact", handover.receivedIntact())
                .addValue("damaged_or_short", handover.damagedOrShort())
                .addValue("photo_attachment_ids", writeList(handover.photoAttachmentIds()))
                .addValue("video_attachment_ids", writeList(handover.videoAttachmentIds()))
                .addValue("created_by", handover.createdBy())
                .addValue("created_at", offset(handover.createdAt())));
        for (SavedBottleHandoverRecord.SavedBottleHandoverItemRecord item : handover.items()) {
            jdbcTemplate.update("""
                    INSERT INTO saved_bottle_handover_items (handover_id, saved_bottle_id, received_quantity, condition, shortfall_quantity)
                    VALUES (:handover_id, :saved_bottle_id, :received_quantity, :condition, :shortfall_quantity)
                    ON CONFLICT (handover_id, saved_bottle_id) DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("handover_id", handover.id())
                    .addValue("saved_bottle_id", item.savedBottleId())
                    .addValue("received_quantity", item.receivedQuantity())
                    .addValue("condition", item.condition())
                    .addValue("shortfall_quantity", item.shortfallQuantity()));
        }
        return handover;
    }

    @Override
    public List<SavedBottleHandoverRecord> listSavedBottleHandovers() {
        List<SavedBottleHandoverRecord> headers = jdbcTemplate.query("""
                SELECT * FROM saved_bottle_handovers ORDER BY created_at DESC, id
                """, rowMapper::mapSavedBottleHandover);
        return headers.stream().map(h -> new SavedBottleHandoverRecord(h.id(), h.merchantId(), h.projectId(),
                h.platformEntityId(), h.pickupLocation(), h.countedTotal(), h.receivedIntact(), h.damagedOrShort(),
                h.photoAttachmentIds(), h.videoAttachmentIds(), listSavedBottleHandoverItems(h.id()), h.createdBy(), h.createdAt()))
                .toList();
    }

    private List<SavedBottleHandoverRecord.SavedBottleHandoverItemRecord> listSavedBottleHandoverItems(String handoverId) {
        return jdbcTemplate.query("""
                SELECT saved_bottle_id, received_quantity, condition, shortfall_quantity FROM saved_bottle_handover_items
                 WHERE handover_id = :handover_id ORDER BY saved_bottle_id
                """, Map.of("handover_id", handoverId), (rs, n) -> new SavedBottleHandoverRecord.SavedBottleHandoverItemRecord(
                rs.getString("saved_bottle_id"), rs.getInt("received_quantity"), rs.getString("condition"), rs.getInt("shortfall_quantity")));
    }

    @Override
    public Optional<SavedBottleHandoverRecord.SavedBottleHandoverItemRecord> findSavedBottleHandoverItem(
            String handoverId, String savedBottleId) {
        if (handoverId == null) {
            return Optional.empty();
        }
        return listSavedBottleHandoverItems(handoverId).stream()
                .filter(item -> item.savedBottleId().equals(savedBottleId)).findFirst();
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
                .addValue("transfer_to_phone_hash", savedBottle.transferToPhoneHash())
                .addValue("remedy_of_saved_bottle_id", savedBottle.remedyOfSavedBottleId())
                .addValue("custodian_entity_id", savedBottle.custodianEntityId())
                .addValue("pickup_location", savedBottle.pickupLocation())
                .addValue("handed_over_at", offset(savedBottle.handedOverAt()))
                .addValue("handover_id", savedBottle.handoverId());
    }

    @Override
    public SavedBottleTransferRecord saveSavedBottleTransfer(SavedBottleTransferRecord transfer) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottle_transfers (
                    id, saved_bottle_id, from_customer_id, from_name_or_nickname,
                    to_name_or_nickname, to_phone, to_phone_hash, transfer_quantity,
                    operator_id, status, evidence_attachment_ids, created_at,
                    operation_source, from_phone_hash, to_customer_id
                )
                VALUES (
                    :id, :saved_bottle_id, :from_customer_id, :from_name_or_nickname,
                    :to_name_or_nickname, :to_phone, :to_phone_hash, :transfer_quantity,
                    :operator_id, :status, CAST(:evidence_attachment_ids AS jsonb), :created_at,
                    :operation_source, :from_phone_hash, :to_customer_id
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
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    operation_source = EXCLUDED.operation_source,
                    from_phone_hash = EXCLUDED.from_phone_hash,
                    to_customer_id = EXCLUDED.to_customer_id
                """, new MapSqlParameterSource()
                .addValue("operation_source", transfer.operationSource())
                .addValue("from_phone_hash", transfer.fromPhoneHash())
                .addValue("to_customer_id", transfer.toCustomerId())
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
                .addValue("created_at", offset(claim.createdAt()))
                .addValue("remaining_before", claim.remainingBefore())
                .addValue("remaining_after", claim.remainingAfter())
                .addValue("verification_method", claim.verificationMethod())
                .addValue("verified_phone_hash", claim.verifiedPhoneHash())
                .addValue("pickup_location", claim.pickupLocation());
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
                .addValue("merchant_id", handover.merchantId())
                .addValue("order_id", handover.orderId());
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
                    id, project_id, merchant_id, warehouse_type, name, status, pour_device_equipped, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :merchant_id, :warehouse_type, :name, :status, :pour_device_equipped, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id,
                    warehouse_type = EXCLUDED.warehouse_type,
                    name = EXCLUDED.name,
                    status = EXCLUDED.status,
                    pour_device_equipped = EXCLUDED.pour_device_equipped,
                    updated_at = EXCLUDED.updated_at
                """, warehouseParameters(warehouse));
        return warehouse;
    }

    @Override
    public Optional<WarehouseRecord> findWarehouseById(String warehouseId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, warehouse_type, name, status, pour_device_equipped, created_at, updated_at
                  FROM warehouses
                 WHERE id = :id
                """, Map.of("id", warehouseId), rowMapper::mapWarehouse).stream().findFirst();
    }

    // 第十八批 M 段第 119 条 119-R1（BSM:1936）：库位主数据；编号全平台唯一，重复编号不覆盖、返回 false。
    @Override
    public boolean saveWarehouseLocation(WarehouseLocationRecord location) {
        return jdbcTemplate.update("""
                INSERT INTO warehouse_locations (id, warehouse_id, created_at) VALUES (:id, :warehouse_id, :created_at)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource().addValue("id", location.id()).addValue("warehouse_id", location.warehouseId())
                .addValue("created_at", offset(location.createdAt()))) == 1;
    }

    @Override
    public List<WarehouseLocationRecord> listWarehouseLocations() {
        return jdbcTemplate.query("SELECT id, warehouse_id, created_at FROM warehouse_locations ORDER BY warehouse_id, id",
                (rs, rowNum) -> new WarehouseLocationRecord(rs.getString("id"), rs.getString("warehouse_id"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    @Override
    public boolean isWarehouseLocation(String warehouseId, String locationId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM warehouse_locations WHERE id = :id AND warehouse_id = :warehouse_id)",
                new MapSqlParameterSource().addValue("id", locationId).addValue("warehouse_id", warehouseId), Boolean.class));
    }

    @Override
    public List<WarehouseRecord> listWarehouses() {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, warehouse_type, name, status, pour_device_equipped, created_at, updated_at
                  FROM warehouses
                 ORDER BY created_at DESC, id
                """, rowMapper::mapWarehouse);
    }

    @Override
    public List<WarehouseRecord> listWarehousesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, warehouse_type, name, status, pour_device_equipped, created_at, updated_at
                  FROM warehouses
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapWarehouse);
    }

    @Override
    public List<WarehouseRecord> listActiveSavedBottleWarehouses(String projectId, String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, warehouse_type, name, status, pour_device_equipped, created_at, updated_at
                  FROM warehouses
                 WHERE project_id = :project_id
                   AND merchant_id IS NOT DISTINCT FROM CAST(:merchant_id AS VARCHAR)
                   AND warehouse_type = 'saved_bottle'
                   AND status = 'active'
                 ORDER BY created_at, id
                """, new MapSqlParameterSource().addValue("project_id", projectId).addValue("merchant_id", merchantId),
                rowMapper::mapWarehouse);
    }

    @Override
    public boolean isSavedBottleWarehouse(String locationId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM warehouses WHERE id = :id AND warehouse_type = 'saved_bottle')",
                new MapSqlParameterSource().addValue("id", locationId), Boolean.class));
    }

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<WarehouseRecord> listWarehousesByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, warehouse_type, name, status, pour_device_equipped, created_at, updated_at
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
                .addValue("pour_device_equipped", warehouse.pourDeviceEquipped())
                .addValue("created_at", offset(warehouse.createdAt()))
                .addValue("updated_at", offset(warehouse.updatedAt()));
    }

    // ── R5 T6：库存域内高价值复核（inventory_high_value_reviews） ─────────────────
    @Override
    public InventoryHighValueReviewRecord saveHighValueReview(InventoryHighValueReviewRecord review) {
        jdbcTemplate.update("""
                INSERT INTO inventory_high_value_reviews (
                    id, subject_type, subject_id, photo_attachment_ids, video_attachment_ids,
                    review_status, reviewer_user_id, reason, created_at, reviewed_at, merchant_id,
                    created_by, supervisor_approved_by, finance_approved_by
                )
                VALUES (
                    :id, :subject_type, :subject_id,
                    CAST(:photo_attachment_ids AS jsonb), CAST(:video_attachment_ids AS jsonb),
                    :review_status, :reviewer_user_id, :reason, :created_at, :reviewed_at, :merchant_id,
                    :created_by, :supervisor_approved_by, :finance_approved_by
                )
                ON CONFLICT (id) DO UPDATE SET
                    subject_type = EXCLUDED.subject_type,
                    subject_id = EXCLUDED.subject_id,
                    photo_attachment_ids = EXCLUDED.photo_attachment_ids,
                    video_attachment_ids = EXCLUDED.video_attachment_ids,
                    review_status = EXCLUDED.review_status,
                    reviewer_user_id = EXCLUDED.reviewer_user_id,
                    reason = EXCLUDED.reason,
                    reviewed_at = EXCLUDED.reviewed_at,
                    supervisor_approved_by = EXCLUDED.supervisor_approved_by,
                    finance_approved_by = EXCLUDED.finance_approved_by
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
                .addValue("merchant_id", review.merchantId())
                .addValue("created_by", review.createdBy())
                .addValue("supervisor_approved_by", review.supervisorApprovedBy())
                .addValue("finance_approved_by", review.financeApprovedBy()));
        return review;
    }

    @Override
    public Optional<InventoryHighValueReviewRecord> findHighValueReviewByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, photo_attachment_ids, video_attachment_ids,
                       review_status, reviewer_user_id, reason, created_at, reviewed_at, merchant_id,
                       created_by, supervisor_approved_by, finance_approved_by
                  FROM inventory_high_value_reviews
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), rowMapper::mapHighValueReview).stream().findFirst();
    }

    @Override
    public List<InventoryHighValueReviewRecord> listHighValueReviews() {
        // 第十八批第 24 条 24-R2：后台复核页列表连表取复核对象摘要（报损/盘点差异/存酒库房处置连调整单与物品，存酒作废连存酒）。
        return jdbcTemplate.query("""
                SELECT r.id, r.subject_type, r.subject_id, r.photo_attachment_ids, r.video_attachment_ids,
                       r.review_status, r.reviewer_user_id, r.reason, r.created_at, r.reviewed_at, r.merchant_id,
                       r.created_by, r.supervisor_approved_by, r.finance_approved_by,
                       COALESCE(ai.item_name, bi.item_name, sb.bottle_name) AS summary_item_name,
                       COALESCE(a.quantity, sb.remaining_quantity) AS summary_quantity,
                       COALESCE(ai.unit, sb.unit) AS summary_unit,
                       a.location_id AS summary_location_id,
                       a.adjustment_type AS summary_adjustment_type,
                       a.reason AS summary_reason,
                       sb.id AS summary_saved_bottle_id
                  FROM inventory_high_value_reviews r
                  LEFT JOIN inventory_stock_adjustments a
                         ON r.subject_type <> 'saved_bottle_void' AND a.id = r.subject_id
                  LEFT JOIN inventory_items ai ON ai.id = a.item_id
                  LEFT JOIN saved_bottles sb ON r.subject_type = 'saved_bottle_void' AND sb.id = r.subject_id
                  LEFT JOIN inventory_items bi ON bi.id = sb.item_id
                 ORDER BY r.created_at DESC, r.id
                """, (rs, rowNumber) -> rowMapper.mapHighValueReview(rs, rowNumber).withSubjectSummary(
                        new InventoryHighValueReviewRecord.SubjectSummary(
                                rs.getString("summary_item_name"), rs.getObject("summary_quantity", Integer.class),
                                rs.getString("summary_unit"), rs.getString("summary_location_id"),
                                rs.getString("summary_adjustment_type"), rs.getString("summary_reason"),
                                rs.getString("summary_saved_bottle_id"))));
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
                    posted_ar_ap_item_id, created_at, updated_at, recorded_by, photo_attachment_ids, video_attachment_ids
                )
                VALUES (
                    :id, :stocktake_id, :item_id, :batch_no, :book_qty, :counted_qty, :variance_qty,
                    :variance_amount_cent, :reason, :supplier_id, :review_status, :reviewed_by, :reviewed_at,
                    :posted_ar_ap_item_id, :created_at, :updated_at, :recorded_by,
                    CAST(:photo_attachment_ids AS jsonb), CAST(:video_attachment_ids AS jsonb)
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
                .addValue("updated_at", offset(variance.updatedAt()))
                .addValue("recorded_by", variance.recordedBy())
                .addValue("photo_attachment_ids", writeList(variance.photoAttachmentIds()))
                .addValue("video_attachment_ids", writeList(variance.videoAttachmentIds())));
        return variance;
    }

    @Override
    public Optional<InventoryVarianceRecord> findInventoryVarianceByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT v.id, v.stocktake_id, v.item_id, v.batch_no, v.book_qty, v.counted_qty,
                       v.variance_qty, v.variance_amount_cent, v.reason, v.supplier_id,
                       v.review_status, v.reviewed_by, v.reviewed_at, v.posted_ar_ap_item_id,
                       s.merchant_id, v.created_at, v.updated_at, v.recorded_by,
                       v.photo_attachment_ids, v.video_attachment_ids
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
                       s.merchant_id, v.created_at, v.updated_at, v.recorded_by,
                       v.photo_attachment_ids, v.video_attachment_ids
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
                       printed_at, created_by, created_at, updated_at, custodian_type, custodian_id, lending_order_id
                  FROM inventory_trace_codes
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTraceCode).stream().findFirst();
    }

    @Override
    public Optional<InventoryTraceCodeRecord> findTraceCodeByCode(String traceCode) {
        return jdbcTemplate.query("""
                SELECT id, trace_code, item_id, batch_id, merchant_id, status, bound_ref,
                       printed_at, created_by, created_at, updated_at, custodian_type, custodian_id, lending_order_id
                  FROM inventory_trace_codes
                 WHERE trace_code = :trace_code
                """, Map.of("trace_code", traceCode), rowMapper::mapTraceCode).stream().findFirst();
    }

    @Override
    public List<InventoryTraceCodeRecord> listTraceCodes() {
        return jdbcTemplate.query("""
                SELECT id, trace_code, item_id, batch_id, merchant_id, status, bound_ref,
                       printed_at, created_by, created_at, updated_at, custodian_type, custodian_id, lending_order_id
                  FROM inventory_trace_codes
                 ORDER BY created_at DESC, id
                """, rowMapper::mapTraceCode);
    }

    @Override
    public List<InventoryTraceCodeRecord> listTraceCodesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, trace_code, item_id, batch_id, merchant_id, status, bound_ref,
                       printed_at, created_by, created_at, updated_at, custodian_type, custodian_id, lending_order_id
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
                    to_owner_type, to_owner_id, from_location_id, to_location_id, status, operator_note, created_by, created_at, updated_at, return_due_at, project_id)
                VALUES (:id, :transfer_type, :lending_mode, :from_owner_type, :from_owner_id,
                    :to_owner_type, :to_owner_id, :from_location_id, :to_location_id, :status, :operator_note, :created_by, :created_at, :updated_at, :return_due_at, :project_id)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, operator_note = EXCLUDED.operator_note, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", o.id()).addValue("transfer_type", o.transferType()).addValue("lending_mode", o.lendingMode())
                .addValue("from_owner_type", o.fromOwnerType()).addValue("from_owner_id", o.fromOwnerId())
                .addValue("to_owner_type", o.toOwnerType()).addValue("to_owner_id", o.toOwnerId())
                .addValue("from_location_id", o.fromLocationId()).addValue("to_location_id", o.toLocationId())
                .addValue("status", o.status()).addValue("operator_note", o.operatorNote()).addValue("created_by", o.createdBy())
                .addValue("created_at", offset(o.createdAt())).addValue("updated_at", offset(o.updatedAt()))
                .addValue("return_due_at", offset(o.returnDueAt())).addValue("project_id", o.projectId()));
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
                       from_location_id, to_location_id, status, operator_note, created_by, created_at, updated_at, return_due_at, project_id
                  FROM inventory_transfer_orders WHERE id = :id
                """, Map.of("id", id), (rs, n) -> mapTransferOrder(rs)).stream().findFirst();
    }

    @Override
    public List<InventoryTransferOrderRecord> listTransferOrders() {
        return jdbcTemplate.query("""
                SELECT id, transfer_type, lending_mode, from_owner_type, from_owner_id, to_owner_type, to_owner_id,
                       from_location_id, to_location_id, status, operator_note, created_by, created_at, updated_at, return_due_at, project_id
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
                rs.getString("created_by"), toInstant(rs, "created_at"), toInstant(rs, "updated_at"), toInstant(rs, "return_due_at"),
                rs.getString("project_id"), List.of());
    }

    private InventoryTransferLineRecord mapTransferLine(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new InventoryTransferLineRecord(rs.getString("id"), rs.getString("transfer_order_id"), rs.getString("item_id"),
                rs.getInt("quantity"), readStringList(rs.getString("trace_codes")), rs.getLong("unit_price_cent"), toInstant(rs, "created_at"));
    }

    @Override
    public void saveTraceOwnershipHistory(InventoryTraceOwnershipHistoryRecord h) {
        jdbcTemplate.update("""
                INSERT INTO inventory_trace_ownership_history (id, trace_code, from_owner_type, from_owner_id, to_owner_type, to_owner_id, transfer_order_id, changed_at, change_kind)
                VALUES (:id, :trace_code, :from_owner_type, :from_owner_id, :to_owner_type, :to_owner_id, :transfer_order_id, :changed_at, :change_kind)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", h.id()).addValue("trace_code", h.traceCode())
                .addValue("from_owner_type", h.fromOwnerType()).addValue("from_owner_id", h.fromOwnerId())
                .addValue("to_owner_type", h.toOwnerType()).addValue("to_owner_id", h.toOwnerId())
                .addValue("transfer_order_id", h.transferOrderId()).addValue("changed_at", offset(h.changedAt()))
                .addValue("change_kind", h.changeKind() == null ? InventoryTraceOwnershipHistoryRecord.OWNERSHIP : h.changeKind()));
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

    @Override
    public int transferTraceCodeOwnership(String traceCode, String fromOwnerId, String ownerType, String ownerId, String itemId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE inventory_trace_codes SET owner_type = :ot, owner_id = :oid, merchant_id = :oid, item_id = :item, updated_at = :now
                 WHERE trace_code = :tc AND (owner_id = :fromId OR owner_id IS NULL)
                """, new MapSqlParameterSource().addValue("ot", ownerType).addValue("oid", ownerId).addValue("item", itemId)
                .addValue("now", offset(now)).addValue("tc", traceCode).addValue("fromId", fromOwnerId));
    }

    /** FS-019 借用只转保管责任：所有权不动（未登记的按借出方登记），改当前保管主体与在借单；CAS 结果即业务判定。 */
    @Override
    public int updateTraceCodeCustodianIfOwnedBy(String traceCode, String ownerId, String custodianType, String custodianId,
            String lendingOrderId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE inventory_trace_codes SET owner_id = COALESCE(owner_id, :owner), custodian_type = :ct, custodian_id = :cid,
                       lending_order_id = :loid, updated_at = :now
                 WHERE trace_code = :tc AND (owner_id = :owner OR owner_id IS NULL)
                """, new MapSqlParameterSource().addValue("owner", ownerId).addValue("ct", custodianType).addValue("cid", custodianId)
                .addValue("loid", lendingOrderId).addValue("now", offset(now)).addValue("tc", traceCode));
    }

    @Override
    public void saveTransferReturn(InventoryTransferReturnRecord r) {
        jdbcTemplate.update("""
                INSERT INTO inventory_transfer_returns (id, return_no, transfer_order_id, line_id, returned_quantity, trace_codes, received_by, received_at)
                VALUES (:id, :return_no, :oid, :line_id, :qty, CAST(:trace_codes AS jsonb), :received_by, :received_at)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource().addValue("id", r.id()).addValue("return_no", r.returnNo()).addValue("oid", r.transferOrderId())
                .addValue("line_id", r.lineId()).addValue("qty", r.returnedQuantity()).addValue("trace_codes", writeList(r.traceCodes()))
                .addValue("received_by", r.receivedBy()).addValue("received_at", offset(r.receivedAt())));
    }

    /** 借用单按行的已归还、无法归还登记中（未驳回）、实物处置已完成数量与涉及的码。 */
    @Override
    public InventoryLendingFacts lendingFacts(String orderId) {
        Map<String, Integer> returned = new java.util.HashMap<>();
        Map<String, Integer> claimed = new java.util.HashMap<>();
        Map<String, Integer> closed = new java.util.HashMap<>();
        java.util.Set<String> returnedCodes = new java.util.HashSet<>();
        java.util.Set<String> claimedCodes = new java.util.HashSet<>();
        jdbcTemplate.query("""
                SELECT line_id, returned_quantity, trace_codes FROM inventory_transfer_returns WHERE transfer_order_id = :oid
                """, Map.of("oid", orderId), rs -> {
            returned.merge(rs.getString("line_id"), rs.getInt("returned_quantity"), Integer::sum);
            returnedCodes.addAll(readStringList(rs.getString("trace_codes")));
        });
        jdbcTemplate.query("""
                SELECT line_id, quantity, status, trace_codes FROM inventory_transfer_dispositions
                 WHERE transfer_order_id = :oid AND disposition_type = 'unreturnable' AND status <> 'rejected'
                """, Map.of("oid", orderId), rs -> {
            claimed.merge(rs.getString("line_id"), rs.getInt("quantity"), Integer::sum);
            claimedCodes.addAll(readStringList(rs.getString("trace_codes")));
            if ("disposal_completed".equals(rs.getString("status"))) {
                closed.merge(rs.getString("line_id"), rs.getInt("quantity"), Integer::sum);
            }
        });
        return new InventoryLendingFacts(returned, claimed, closed, returnedCodes, claimedCodes);
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
