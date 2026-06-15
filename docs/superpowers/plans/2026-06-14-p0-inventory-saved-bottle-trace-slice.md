# P0 Inventory Saved Bottle Trace Slice Plan

## 1. Objective

Implement the P0 inventory, saved bottle, supplier, purchase request, stock movement, and trace handover foundation on 192.168.88.160 with Java 21 + Spring Boot 4.x + PostgreSQL + Flyway.

This slice gives the later bar P0 implementation a real stock and saved-bottle backbone. Bar ordering may still be implemented in the next slice, but it must not invent a separate inventory ledger.

## 2. In Scope

Formalize these OpenAPI P0 paths:

- `GET /api/v1/inventory/items`
- `POST /api/v1/inventory/items`
- `GET /api/v1/inventory/purchase-requests`
- `POST /api/v1/inventory/purchase-requests`
- `GET /api/v1/inventory/saved-bottles`
- `POST /api/v1/inventory/saved-bottles`
- `POST /api/v1/inventory/saved-bottles/{id}/claim`
- `POST /api/v1/inventory/stock-adjustments`
- `GET /api/v1/inventory/stock-balances`
- `POST /api/v1/inventory/stock-in`
- `GET /api/v1/inventory/stock-movements`
- `POST /api/v1/inventory/stock-movements`
- `POST /api/v1/inventory/stock-movements/transfer`
- `POST /api/v1/inventory/stock-movements/{movement_type}`
- `GET /api/v1/inventory/suppliers`
- `POST /api/v1/inventory/suppliers`
- `POST /api/v1/inventory/trace-codes/scan-handover`

## 3. Out of Scope

- Full ERP procurement approval, supplier settlement, invoice matching, and tax-control integration.
- Complex batch/expiry valuation, weighted average cost, FIFO/LIFO costing, and multi-warehouse cost accounting.
- Automatic bar recipe cost deduction. P0 must provide stock movement APIs; bar recipe deduction can call these later.
- Third-party warehouse hardware or scanner SDK integration.

## 4. Files To Add

- `backend/src/main/resources/db/migration/V012__p0_inventory_saved_bottle_trace.sql`
- `backend/src/main/java/com/complex/platform/inventory/InventoryController.java`
- `backend/src/main/java/com/complex/platform/inventory/InventoryService.java`
- `backend/src/main/java/com/complex/platform/inventory/InventoryRepository.java`
- `backend/src/main/java/com/complex/platform/inventory/JdbcInventoryRepository.java`
- `backend/src/main/java/com/complex/platform/inventory/*Record.java`
- `backend/src/main/java/com/complex/platform/inventory/*Response.java`
- `backend/src/main/java/com/complex/platform/inventory/dto/*Request.java`
- `backend/src/test/java/com/complex/platform/inventory/InventoryControllerTests.java`
- `backend/src/test/java/com/complex/platform/inventory/InventoryServiceTests.java`
- `backend/src/test/java/com/complex/platform/inventory/InventoryJdbcIntegrationTests.java`

## 5. Data Model

Add PostgreSQL tables with idempotent Flyway DDL:

- `inventory_items`
  - `id`, `sku_code`, `item_name`, `item_type`, `unit`, `barcode`, `trace_required`, `min_quantity`, `status`, `created_at`, `updated_at`
  - Unique `sku_code` when present.
- `inventory_suppliers`
  - `id`, `supplier_name`, `supplier_type`, `contact_name`, `contact_phone`, `status`, `created_at`, `updated_at`
- `inventory_stock_balances`
  - `id`, `item_id`, `location_id`, `quantity`, `reserved_quantity`, `available_quantity`, `updated_at`
  - Unique `(item_id, location_id)`.
- `inventory_stock_movements`
  - `id`, `movement_type`, `item_id`, `source_location_id`, `target_location_id`, `quantity`, `business_type`, `business_id`, `trace_code`, `operator_id`, `evidence_attachment_ids`, `status`, `created_at`
- `inventory_stock_adjustments`
  - `id`, `item_id`, `location_id`, `adjustment_type`, `quantity`, `reason`, `evidence_attachment_ids`, `status`, `created_at`
- `inventory_purchase_requests`
  - `id`, `request_no`, `supplier_id`, `item_id`, `quantity`, `expected_arrival_date`, `status`, `evidence_attachment_ids`, `created_at`, `updated_at`
- `saved_bottles`
  - `id`, `customer_id`, `bar_project_id`, `item_id`, `bottle_name`, `initial_quantity`, `remaining_quantity`, `unit`, `expire_at`, `status`, `evidence_attachment_ids`, `created_at`, `updated_at`
- `saved_bottle_claims`
  - `id`, `saved_bottle_id`, `claim_quantity`, `claim_staff_id`, `claim_terminal_type`, `claim_device_id`, `status`, `evidence_attachment_ids`, `created_at`
- `inventory_trace_handovers`
  - `id`, `trace_code`, `item_id`, `from_staff_id`, `to_staff_id`, `device_id`, `handover_result`, `evidence_attachment_ids`, `created_at`

Seed permissions and attach to `role_platform_admin`:

- `inventory.stock.view`
- `inventory.stock.move`
- `inventory.stock.adjust`
- `inventory.purchase_request.view`
- `inventory.purchase_request.manage`
- `inventory.supplier.view`
- `inventory.supplier.manage`
- `bar.saved_bottle.manage`

## 6. Business Rules

- Quantity fields must be integer and non-negative; movement quantity must be positive.
- P0 must support forced stock deduction for formal outbound/move/claim flows. If available quantity is insufficient, return conflict and do not create a successful movement.
- Stock-in increases balance at target location.
- Outbound movement decreases source balance.
- Transfer decreases source balance and increases target balance in one transaction.
- Adjustment can increase or decrease balance but must persist an adjustment record and evidence ids.
- Saved bottle creation initializes `remaining_quantity = initial_quantity` and status `stored`.
- Saved bottle claim must reject over-claim and expired/claimed/cancelled bottles.
- Saved bottle claim reduces `remaining_quantity`; status becomes `claimed` when it reaches zero, otherwise remains `stored`.
- Trace handover records scan evidence and staff/device context, but does not need vendor SDK integration in P0.
- GET endpoints may return empty lists, but they must write the configured view audit events.

## 7. Interface Rules

- Every endpoint must call `AuthorizationService.requirePermission(...)` with exact OpenAPI permissions.
- All write endpoints with `x-idempotency-required: true` must reject missing/blank `idempotency_key` with `VALIDATION_FAILED`.
- All write operations must go through `IdempotencyService`.
- All SQL must use `NamedParameterJdbcTemplate` with named parameters.
- Request DTO fields must match OpenAPI snake_case via the project JSON naming strategy; do not introduce camelCase request fields in OpenAPI.
- Responses may use stable P0 records/list wrappers; do not invent old `/api/admin/*` or `/api/mobile/*` compatibility endpoints.

## 8. Audit Events

Use exact OpenAPI event names:

- `inventory.item.viewed` / `inventory.item.view_failed`
- `inventory.item.created` / `inventory.item.create_failed`
- `inventory.purchase_request_viewed` / `inventory.purchase_request_view_failed`
- `inventory.purchase_request_changed` / `inventory.purchase_request_change_failed`
- `bar.saved_bottle.viewed` / `bar.saved_bottle.view_failed`
- `bar.saved_bottle.created` / `bar.saved_bottle.create_failed`
- `bar.saved_bottle_picked_up` / `bar.saved_bottle_pickup_failed`
- `inventory.counted` / `inventory.count_failed`
- `inventory.stock_balance.viewed` / `inventory.stock_balance.view_failed`
- `inventory.inbound_confirmed` / `inventory.inbound_confirm_failed`
- `inventory.stock_movement.viewed` / `inventory.stock_movement.view_failed`
- `inventory.outbound_verified` / `inventory.outbound_verify_failed`
- `inventory.stock_transfer.created` / `inventory.stock_transfer.create_failed`
- `inventory.stock_movement.created` / `inventory.stock_movement.create_failed`
- `inventory.supplier_viewed` / `inventory.supplier_view_failed`
- `inventory.supplier_changed` / `inventory.supplier_change_failed`
- `operation_handover.created` / `operation_handover.create_failed`

If OpenAPI has a different exact failure event, OpenAPI wins.

## 9. TDD Plan

RED tests first:

- Controller rejects missing idempotency key on each write path.
- Controller calls exact permission for every operation.
- Service rejects outbound/transfer/claim when balance or remaining quantity is insufficient.
- Service creates stock-in, outbound, transfer, adjustment, saved bottle, claim, supplier, purchase request, and trace handover records with audit events.
- JDBC migration creates all tables, permissions, and supports save/list/update balance operations.

GREEN implementation:

- Add migration.
- Add controller/service/repository/records/DTOs.
- Keep validation local and explicit.
- Add list responses and stable IDs based on idempotency key where useful.

Refactor only after targeted tests are green.

## 10. Verification

Run targeted tests:

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=InventoryServiceTests,InventoryControllerTests,InventoryJdbcIntegrationTests test
```

Run contract and full P0:

```bash
cd /home/james/complex-management
npm run verify:p0-contract
/opt/complex/scripts/complex-verify-p0.sh
```

Security and quality scan:

```bash
cd /home/james/complex-management
grep -R -n -E 'createStatement|Statement|String\\.format|SELECT .*\\+|UPDATE .*\\+|INSERT .*\\+|DELETE .*\\+|Double|double|getDouble|Math\\.round' backend/src/main/java/com/complex/platform/inventory backend/src/test/java/com/complex/platform/inventory || true
```

## 11. Rollback

- Before implementation, create a tar snapshot under `/home/james/complex-management-backups/`.
- If the slice fails after V012 migration, restore the snapshot for code and reset the development database only if needed.
- Do not edit or rewrite already applied V001-V011 migrations; add only V012 for this slice.
