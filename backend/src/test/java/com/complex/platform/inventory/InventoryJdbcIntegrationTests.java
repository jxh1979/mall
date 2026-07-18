package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class InventoryJdbcIntegrationTests {

    @Autowired
    private JdbcInventoryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesInventoryTablesAndSeedsPermissions() {
        assertThat(regclass("inventory_items")).isEqualTo("inventory_items");
        assertThat(regclass("inventory_suppliers")).isEqualTo("inventory_suppliers");
        assertThat(regclass("inventory_stock_balances")).isEqualTo("inventory_stock_balances");
        assertThat(regclass("inventory_stock_movements")).isEqualTo("inventory_stock_movements");
        assertThat(regclass("inventory_stock_adjustments")).isEqualTo("inventory_stock_adjustments");
        assertThat(regclass("inventory_purchase_requests")).isEqualTo("inventory_purchase_requests");
        assertThat(regclass("saved_bottles")).isEqualTo("saved_bottles");
        assertThat(regclass("saved_bottle_claims")).isEqualTo("saved_bottle_claims");
        assertThat(regclass("inventory_trace_handovers")).isEqualTo("inventory_trace_handovers");
        assertThat(regclass("inventory_stock_batches")).isEqualTo("inventory_stock_batches");
        assertThat(regclass("inventory_risk_alerts")).isEqualTo("inventory_risk_alerts");
        assertThat(regclass("inventory_supplier_reconciliations")).isEqualTo("inventory_supplier_reconciliations");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());

        assertThat(permissionCodes()).contains(
                "inventory.stock.view",
                "inventory.stock.move",
                "inventory.stock.adjust",
                "inventory.stock.batch.manage",
                "inventory.risk_alert.view",
                "inventory.risk.scan",
                "inventory.purchase_request.view",
                "inventory.purchase_request.manage",
                "inventory.supplier.view",
                "inventory.supplier.manage",
                "inventory.supplier.reconcile",
                "bar.saved_bottle.manage"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "inventory.stock.view",
                "inventory.stock.move",
                "inventory.stock.adjust",
                "inventory.stock.batch.manage",
                "inventory.risk.scan",
                "inventory.purchase_request.manage",
                "inventory.supplier.manage",
                "inventory.supplier.reconcile",
                "bar.saved_bottle.manage"
        );
    }

    @Test
    void repositoryPersistsInventorySliceRecordsAndBalances() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveInventoryItem(new InventoryItemRecord(
                "item_jdbc",
                "SKU-JDBC",
                "JDBC Vodka",
                "spirit",
                "bottle",
                "690000000001",
                true,
                2,
                "active",
                now,
                now,
                null,
                null
        ));
        repository.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_jdbc",
                "JDBC Supplier",
                "general",
                "Alice",
                "13800138000",
                "active",
                now,
                now,
                null
        ));
        repository.saveStockBalance(new StockBalanceRecord(
                "balance_jdbc",
                "item_jdbc",
                "warehouse_jdbc",
                10,
                1,
                9,
                now,
                null,
                null
        ));
        repository.saveStockMovement(new StockMovementRecord(
                "movement_jdbc",
                "inbound",
                "item_jdbc",
                null,
                "warehouse_jdbc",
                10,
                "purchase",
                "purchase_jdbc",
                "TRACE-JDBC",
                "staff_jdbc",
                List.of("attachment_jdbc"),
                "completed",
                now,
                null
        ), null);
        repository.savePurchaseRequest(new PurchaseRequestRecord(
                "purchase_jdbc",
                "PR202606140001",
                "supplier_jdbc",
                "item_jdbc",
                5,
                LocalDate.parse("2026-06-20"),
                "created",
                List.of("attachment_jdbc"),
                now,
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(repository.listInventoryItems())
                .extracting(InventoryItemRecord::id, InventoryItemRecord::itemName, InventoryItemRecord::minQuantity)
                .contains(tuple("item_jdbc", "JDBC Vodka", 2));
        assertThat(repository.listInventorySuppliers())
                .extracting(InventorySupplierRecord::id, InventorySupplierRecord::supplierName, InventorySupplierRecord::contactPhone)
                .contains(tuple("supplier_jdbc", "JDBC Supplier", "13800138000"));
        assertThat(repository.findStockBalance("item_jdbc", "warehouse_jdbc"))
                .get()
                .extracting(StockBalanceRecord::quantity, StockBalanceRecord::availableQuantity)
                .containsExactly(10, 9);
        assertThat(repository.listStockMovements())
                .extracting(StockMovementRecord::id, StockMovementRecord::traceCode, StockMovementRecord::quantity)
                .contains(tuple("movement_jdbc", "TRACE-JDBC", 10));
        // AUD-013 真分页: page size 1 只回 1 行, total 取 COUNT 真值。
        var movementPage = repository.listStockMovementsPaged(null, null, 1, 1);
        assertThat(movementPage.items()).hasSize(1);
        assertThat(movementPage.pageSize()).isEqualTo(1);
        assertThat(movementPage.total()).isGreaterThanOrEqualTo(1L);
        assertThat(repository.listPurchaseRequests())
                .extracting(PurchaseRequestRecord::id, PurchaseRequestRecord::supplierId, PurchaseRequestRecord::quantity)
                .contains(tuple("purchase_jdbc", "supplier_jdbc", 5));
    }

    @Test
    void migrationCreatesT6TablesSeedsPermissionsAndEnforcesChecks() {
        // 全部 T6 表存在。
        assertThat(regclass("supply_contracts")).isEqualTo("supply_contracts");
        assertThat(regclass("warehouses")).isEqualTo("warehouses");
        assertThat(regclass("inventory_purchase_request_events")).isEqualTo("inventory_purchase_request_events");
        assertThat(regclass("inventory_high_value_reviews")).isEqualTo("inventory_high_value_reviews");

        // T6 权限 seed。
        assertThat(permissionCodes()).contains(
                "inventory.supply_contract.manage",
                "inventory.purchase.approve",
                "inventory.warehouse.manage",
                "inventory.high_value.review");
        assertThat(platformAdminPermissionCodes()).contains(
                "inventory.supply_contract.manage",
                "inventory.purchase.approve",
                "inventory.warehouse.manage",
                "inventory.high_value.review");

        // balances.warehouse_id 列存在。
        Integer cols = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_name = 'inventory_stock_balances' AND column_name = 'warehouse_id'
                """, Integer.class);
        assertThat(cols).isEqualTo(1);

        // 合法库房 + 余额挂 warehouse_id（FK）可写。
        jdbcTemplate.update("""
                INSERT INTO inventory_suppliers (id, supplier_name, supplier_type, status, created_at, updated_at)
                VALUES ('sup_t6', 'T6 Supplier', 'general', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        jdbcTemplate.update("""
                INSERT INTO warehouses (id, project_id, warehouse_type, name, status, created_at, updated_at)
                VALUES ('wh_t6', 'proj_1', 'saved_bottle', '存酒区', 'active', now(), now())
                """);
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at)
                VALUES ('item_t6', 'SKU-T6', 'T6 Item', 'spirit', 'bottle', false, 0, 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveStockBalance(new StockBalanceRecord(
                "bal_t6", "item_t6", "loc_t6", 5, 0, 5, now, null, "wh_t6"));
        assertThat(repository.findStockBalance("item_t6", "loc_t6"))
                .get()
                .extracting(StockBalanceRecord::warehouseId)
                .isEqualTo("wh_t6");

        // 供应合同三模式持久化（含 purchasable_scope JSONB）。
        repository.saveSupplyContract(new SupplyContractRecord(
                "sc_t6", "SC-T6", "sup_t6", "proj_1", "m_a", "merchant_self_purchase",
                "scope", "terms", 30, LocalDate.parse("2026-06-01"), LocalDate.parse("2027-06-01"),
                "summary", List.of("item_t6"), "approved", "active", now, now));
        assertThat(repository.listSupplyContracts())
                .extracting(SupplyContractRecord::id, SupplyContractRecord::contractMode, SupplyContractRecord::approvalStatus)
                .contains(tuple("sc_t6", "merchant_self_purchase", "approved"));
        assertThat(repository.listSupplyContractsByMerchant("m_a"))
                .extracting(SupplyContractRecord::id)
                .contains("sc_t6");
        // P2 多综合体 project_id scoped 读通道：按 project_id 行级隔离。
        assertThat(repository.listSupplyContractsByProject("proj_1"))
                .extracting(SupplyContractRecord::id)
                .contains("sc_t6");
        assertThat(repository.listSupplyContractsByProject("proj_nonexistent")).isEmpty();
        assertThat(repository.findSupplyContractByIdForUpdate("sc_t6"))
                .get()
                .extracting(SupplyContractRecord::purchasableScope)
                .isEqualTo(List.of("item_t6"));
    }

    @Test
    void supplyContractsCheckRejectsIllegalMode() {
        jdbcTemplate.update("""
                INSERT INTO inventory_suppliers (id, supplier_name, supplier_type, status, created_at, updated_at)
                VALUES ('sup_chk', 'Chk Supplier', 'general', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO supply_contracts (id, contract_no, supplier_id, project_id, contract_mode)
                VALUES ('sc_bad', 'SC-BAD', 'sup_chk', 'proj_1', 'illegal_mode')
                """)).hasMessageContaining("chk_supply_contracts_mode");
    }

    @Test
    void warehousesCheckRejectsIllegalType() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO warehouses (id, project_id, warehouse_type, name)
                VALUES ('wh_bad', 'proj_1', 'spaceship', 'bad')
                """)).hasMessageContaining("chk_warehouses_type");
    }

    @Test
    void warehousesCheckRejectsIllegalStatus() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO warehouses (id, project_id, warehouse_type, name, status)
                VALUES ('wh_bad2', 'proj_1', 'main', 'bad', 'frozen')
                """)).hasMessageContaining("chk_warehouses_status");
    }

    @Test
    void migrationV112CreatesSupplierQualificationSettlementTablesSeedsAndPersists() {
        // P1#18 子表存在 + 权限 seed。
        assertThat(regclass("supplier_qualifications")).isEqualTo("supplier_qualifications");
        assertThat(regclass("supplier_settlement_methods")).isEqualTo("supplier_settlement_methods");
        assertThat(permissionCodes()).contains(
                "inventory.supplier.qualification.manage",
                "inventory.supplier.settlement.manage");
        assertThat(platformAdminPermissionCodes()).contains(
                "inventory.supplier.qualification.manage",
                "inventory.supplier.settlement.manage");

        jdbcTemplate.update("""
                INSERT INTO inventory_suppliers (id, supplier_name, supplier_type, status, created_at, updated_at)
                VALUES ('sup_q', 'Q Supplier', 'general', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        Instant now = Instant.parse("2026-06-14T00:00:00Z");

        // 资质 round-trip（含附件 JSONB + 有效期）。
        repository.saveSupplierQualification(new SupplierQualificationRecord(
                "qual_jdbc", "sup_q", "food_business_license", "FB-1", "市监局",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2030-01-01"),
                List.of("att_a", "att_b"), "active", now, now, "m_a"));
        assertThat(repository.listSupplierQualificationsBySupplier("sup_q"))
                .extracting(SupplierQualificationRecord::id, SupplierQualificationRecord::qualificationType,
                        SupplierQualificationRecord::attachmentIds)
                .contains(tuple("qual_jdbc", "food_business_license", List.of("att_a", "att_b")));
        assertThat(repository.listSupplierQualificationsByMerchant("m_a"))
                .extracting(SupplierQualificationRecord::id).contains("qual_jdbc");

        // 结算多档 round-trip（默认档 + 账期）。
        repository.saveSupplierSettlementMethod(new SupplierSettlementMethodRecord(
                "settle_jdbc", "sup_q", "monthly", 30, "spirit", true, "active", now, now, "m_a"));
        assertThat(repository.listSupplierSettlementMethodsBySupplier("sup_q"))
                .extracting(SupplierSettlementMethodRecord::id, SupplierSettlementMethodRecord::settlementMethod,
                        SupplierSettlementMethodRecord::paymentTermDays, SupplierSettlementMethodRecord::isDefault)
                .contains(tuple("settle_jdbc", "monthly", 30, true));
        assertThat(repository.listSupplierSettlementMethodsByMerchant("m_a"))
                .extracting(SupplierSettlementMethodRecord::id).contains("settle_jdbc");
    }

    @Test
    void supplierQualificationCheckRejectsIllegalType() {
        jdbcTemplate.update("""
                INSERT INTO inventory_suppliers (id, supplier_name, supplier_type, status, created_at, updated_at)
                VALUES ('sup_qchk', 'QChk Supplier', 'general', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO supplier_qualifications (id, supplier_id, qualification_type)
                VALUES ('qual_bad', 'sup_qchk', 'spaceship_permit')
                """)).hasMessageContaining("chk_supplier_qual_type");
    }

    @Test
    void supplierSettlementCheckRejectsIllegalMethod() {
        jdbcTemplate.update("""
                INSERT INTO inventory_suppliers (id, supplier_name, supplier_type, status, created_at, updated_at)
                VALUES ('sup_schk', 'SChk Supplier', 'general', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO supplier_settlement_methods (id, supplier_id, settlement_method)
                VALUES ('settle_bad', 'sup_schk', 'barter')
                """)).hasMessageContaining("chk_supplier_settle_method");
    }

    @Test
    void purchaseRequestStatusCheckAllowsLegacyCreatedAndRejectsIllegal() {
        // 现状写入值 'created' 须被 CHECK 接受（兼容历史/既有写入）。
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at)
                VALUES ('item_prchk', 'SKU-PRCHK', 'PR Chk Item', 'spirit', 'bottle', false, 0, 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        jdbcTemplate.update("""
                INSERT INTO inventory_purchase_requests (id, request_no, item_id, quantity, status, created_at, updated_at)
                VALUES ('pr_created', 'PR-CREATED', 'item_prchk', 1, 'created', now(), now())
                """);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO inventory_purchase_requests (id, request_no, item_id, quantity, status, created_at, updated_at)
                VALUES ('pr_bad', 'PR-BAD', 'item_prchk', 1, 'totally_invalid', now(), now())
                """)).hasMessageContaining("chk_inv_purchase_status");
    }

    @Test
    void repositoryPersistsPurchaseFlowColumnsAndEvents() {
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at)
                VALUES ('item_prflow', 'SKU-PRFLOW', 'PR Flow Item', 'spirit', 'bottle', false, 0, 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        Instant now = Instant.parse("2026-06-18T00:00:00Z");
        // 写入携带全部流转字段的采购单，验证扩列往返。
        repository.savePurchaseRequest(new PurchaseRequestRecord(
                "pr_jdbc_flow", "PR-FLOW", null, "item_prflow", 3, LocalDate.parse("2026-06-25"),
                "received", List.of(), now, now, null,
                null, "usr_appr", now, LocalDate.parse("2026-06-24"), now, "usr_recv"));
        PurchaseRequestRecord loaded = repository.findPurchaseRequestByIdForUpdate("pr_jdbc_flow").orElseThrow();
        assertThat(loaded)
                .extracting(PurchaseRequestRecord::status, PurchaseRequestRecord::approvedBy,
                        PurchaseRequestRecord::receivedBy)
                .containsExactly("received", "usr_appr", "usr_recv");
        assertThat(loaded.actualArrivalDate()).isEqualTo(LocalDate.parse("2026-06-24"));

        // 流转事件留痕往返。
        repository.savePurchaseRequestEvent(new PurchaseRequestEventRecord(
                "pr_evt_jdbc", "pr_jdbc_flow", "arrived", "received", "usr_recv", "ok", now));
        assertThat(repository.listPurchaseRequestEvents("pr_jdbc_flow"))
                .extracting(PurchaseRequestEventRecord::fromStatus, PurchaseRequestEventRecord::toStatus)
                .containsExactly(tuple("arrived", "received"));
    }

    @Test
    void repositoryPersistsP1StockBatchesRiskAlertsAndSupplierReconciliations() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveInventoryItem(new InventoryItemRecord(
                "item_p1_jdbc",
                "SKU-P1-JDBC",
                "P1 JDBC Vodka",
                "spirit",
                "bottle",
                "690000000002",
                true,
                5,
                "active",
                now,
                now,
                null,
                null
        ));
        repository.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_p1_jdbc",
                "P1 JDBC Supplier",
                "general",
                "Bob",
                "13900139000",
                "active",
                now,
                now,
                null
        ));
        repository.saveStockBatch(new InventoryStockBatchRecord(
                "batch_p1_jdbc",
                "item_p1_jdbc",
                "warehouse_p1_jdbc",
                "BATCH-P1-JDBC",
                "supplier_p1_jdbc",
                2,
                Instant.parse("2026-06-20T00:00:00Z"),
                "active",
                List.of("att_batch_jdbc"),
                now,
                now,
                null
        ), null);
        repository.saveRiskAlert(new InventoryRiskAlertRecord(
                "risk_p1_jdbc",
                "expiry_warning",
                "high",
                "item_p1_jdbc",
                "warehouse_p1_jdbc",
                "batch_p1_jdbc",
                2,
                null,
                Instant.parse("2026-06-20T00:00:00Z"),
                "open",
                "staff_ops",
                now,
                null
        ));
        repository.saveSupplierReconciliation(new SupplierReconciliationRecord(
                "supplier_recon_p1_jdbc",
                "supplier_p1_jdbc",
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                100000L,
                88000L,
                -12000L,
                "difference_found",
                List.of("att_invoice_jdbc"),
                "staff_finance",
                now,
                now,
                null
        ));

        assertThat(repository.listStockBatches())
                .extracting(InventoryStockBatchRecord::id, InventoryStockBatchRecord::batchNo, InventoryStockBatchRecord::quantity)
                .contains(tuple("batch_p1_jdbc", "BATCH-P1-JDBC", 2));
        assertThat(repository.listRiskAlerts())
                .extracting(InventoryRiskAlertRecord::id, InventoryRiskAlertRecord::alertType, InventoryRiskAlertRecord::severity)
                .contains(tuple("risk_p1_jdbc", "expiry_warning", "high"));
        assertThat(repository.listSupplierReconciliations())
                .extracting(SupplierReconciliationRecord::id, SupplierReconciliationRecord::differenceAmountCent, SupplierReconciliationRecord::status)
                .contains(tuple("supplier_recon_p1_jdbc", -12000L, "difference_found"));
    }


    // C1【Critical】数据层防跨商户扣减：atomicDecreaseStockBalance 的 merchant 谓词。
    // scope=商户B 不得扣减归属商户A 的余额（返回 0 未扣）；scope=null（管理员）与 scope=商户A 正常扣。
    @Test
    void atomicDecreaseStockBalanceEnforcesMerchantScope() {
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at, merchant_id)
                VALUES ('item_dec', 'SKU-DEC', 'Dec Item', 'spirit', 'bottle', false, 0, 'active', now(), now(), 'm_a')
                ON CONFLICT (id) DO NOTHING
                """);
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveStockBalance(new StockBalanceRecord(
                "bal_dec", "item_dec", "loc_dec", 100, 0, 100, now, "m_a", null));

        // 商户 B 视角扣减 → 0 行（谓词拦截），余额不变。
        assertThat(repository.atomicDecreaseStockBalance("item_dec", "loc_dec", 40, "m_b")).isZero();
        assertThat(repository.findStockBalance("item_dec", "loc_dec").orElseThrow().availableQuantity())
                .isEqualTo(100);

        // 商户 A 视角扣减 → 成功扣 40。
        assertThat(repository.atomicDecreaseStockBalance("item_dec", "loc_dec", 40, "m_a")).isEqualTo(1);
        assertThat(repository.findStockBalance("item_dec", "loc_dec").orElseThrow().availableQuantity())
                .isEqualTo(60);

        // 平台管理员（scope=null）放行 → 再扣 10。
        assertThat(repository.atomicDecreaseStockBalance("item_dec", "loc_dec", 10, null)).isEqualTo(1);
        assertThat(repository.findStockBalance("item_dec", "loc_dec").orElseThrow().availableQuantity())
                .isEqualTo(50);
    }

    // C2【High】库存余额项目级读隔离：listStockBalancesByProject 只返回本项目余额行。
    @Test
    void listStockBalancesByProjectIsolatesProjects() {
        // C2 迁移：project_id 列存在。
        Integer cols = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_name = 'inventory_stock_balances' AND column_name = 'project_id'
                """, Integer.class);
        assertThat(cols).isEqualTo(1);

        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at)
                VALUES ('item_pa', 'SKU-PA', 'PA Item', 'spirit', 'bottle', false, 0, 'active', now(), now()),
                       ('item_pb', 'SKU-PB', 'PB Item', 'spirit', 'bottle', false, 0, 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        // 经原子入库落 project_id（proj_a / proj_b）。
        repository.atomicIncreaseStockBalance("item_pa", "loc_pa", 10, "bal_pa", "m_a", null, "proj_a");
        repository.atomicIncreaseStockBalance("item_pb", "loc_pb", 20, "bal_pb", "m_b", null, "proj_b");

        assertThat(repository.listStockBalancesByProject("proj_a"))
                .extracting(StockBalanceRecord::id).contains("bal_pa").doesNotContain("bal_pb");
        assertThat(repository.listStockBalancesByProject("proj_b"))
                .extracting(StockBalanceRecord::id).contains("bal_pb").doesNotContain("bal_pa");
        assertThat(repository.listStockBalancesByProject("proj_nonexistent")).isEmpty();
    }

    // C2 read-side leftover: movement/batch list channels must also isolate by project.
    @Test
    void listStockMovementsAndBatchesByProjectIsolateProjects() {
        assertThat(columnExists("inventory_stock_movements", "project_id")).isEqualTo(1);
        assertThat(columnExists("inventory_stock_batches", "project_id")).isEqualTo(1);

        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at)
                VALUES ('item_ma', 'SKU-MA', 'MA Item', 'spirit', 'bottle', false, 0, 'active', now(), now()),
                       ('item_mb', 'SKU-MB', 'MB Item', 'spirit', 'bottle', false, 0, 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        Instant now = Instant.parse("2026-07-05T00:00:00Z");

        repository.saveStockMovement(new StockMovementRecord(
                "mv_pa", "inbound", "item_ma", null, "loc_ma", 10,
                "purchase", null, null, null, List.of(), "completed", now, "m_a"), "proj_a");
        repository.saveStockMovement(new StockMovementRecord(
                "mv_pb", "inbound", "item_mb", null, "loc_mb", 20,
                "purchase", null, null, null, List.of(), "completed", now, "m_b"), "proj_b");
        repository.saveStockBatch(new InventoryStockBatchRecord(
                "bt_pa", "item_ma", "loc_ma", "B-PA", null, 10, null, "active", List.of(), now, now, "m_a"), "proj_a");
        repository.saveStockBatch(new InventoryStockBatchRecord(
                "bt_pb", "item_mb", "loc_mb", "B-PB", null, 20, null, "active", List.of(), now, now, "m_b"), "proj_b");

        assertThat(repository.listStockMovementsByProject("proj_a"))
                .extracting(StockMovementRecord::id).contains("mv_pa").doesNotContain("mv_pb");
        assertThat(repository.listStockMovementsByProject("proj_b"))
                .extracting(StockMovementRecord::id).contains("mv_pb").doesNotContain("mv_pa");
        assertThat(repository.listStockMovementsByProject("proj_none")).isEmpty();

        assertThat(repository.listStockBatchesByProject("proj_a"))
                .extracting(InventoryStockBatchRecord::id).contains("bt_pa").doesNotContain("bt_pb");
        assertThat(repository.listStockBatchesByProject("proj_b"))
                .extracting(InventoryStockBatchRecord::id).contains("bt_pb").doesNotContain("bt_pa");
        assertThat(repository.listStockBatchesByProject("proj_none")).isEmpty();
    }

    private Integer columnExists(String table, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                Integer.class, table, column);
    }

    // C3【High】高价值复核单归属商户列存在 + 往返（reviewInternal 据此拒绝跨租户翻转）。
    @Test
    void highValueReviewCarriesMerchantId() {
        Integer cols = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_name = 'inventory_high_value_reviews' AND column_name = 'merchant_id'
                """, Integer.class);
        assertThat(cols).isEqualTo(1);

        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveHighValueReview(new InventoryHighValueReviewRecord(
                "hvr_m", "saved_bottle_void", "sb_x", List.of("p1"), List.of("v1"),
                "pending", null, null, now, null, "m_a"));
        assertThat(repository.findHighValueReviewByIdForUpdate("hvr_m"))
                .get().extracting(InventoryHighValueReviewRecord::merchantId).isEqualTo("m_a");
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass(?)::text", String.class, tableName);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = true
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("SELECT permission_code FROM permissions", String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                """, String.class);
    }
}
