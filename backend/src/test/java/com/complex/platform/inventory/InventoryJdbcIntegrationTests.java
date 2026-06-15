package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;
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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
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
        assertThat(currentFlywayVersion()).isEqualTo("039");

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
                now
        ));
        repository.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_jdbc",
                "JDBC Supplier",
                "general",
                "Alice",
                "13800138000",
                "active",
                now,
                now
        ));
        repository.saveStockBalance(new StockBalanceRecord(
                "balance_jdbc",
                "item_jdbc",
                "warehouse_jdbc",
                10,
                1,
                9,
                now
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
                now
        ));
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
                now
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
        assertThat(repository.listPurchaseRequests())
                .extracting(PurchaseRequestRecord::id, PurchaseRequestRecord::supplierId, PurchaseRequestRecord::quantity)
                .contains(tuple("purchase_jdbc", "supplier_jdbc", 5));
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
                now
        ));
        repository.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_p1_jdbc",
                "P1 JDBC Supplier",
                "general",
                "Bob",
                "13900139000",
                "active",
                now,
                now
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
                now
        ));
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
                now
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
                now
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
