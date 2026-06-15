# P0 Bar Reservation Order Billing Slice Plan

## 1. Objective

Implement the P0 bar business slice on 192.168.88.160 with Java 21 + Spring Boot 4.x + PostgreSQL + Flyway.

This slice must turn the bar domain from OpenAPI contract into executable backend code: age verification placeholder, table/card-seat resources, reservation with deposit, card-seat guest check-in, product catalog, ordering, delivery, bill creation, payment intent, settlement, and commission calculation.

## 2. In Scope

Formalize these OpenAPI P0 paths:

- `POST /api/v1/bar/age-verifications`
- `POST /api/v1/bar/bills/{bill_id}/commissions/calculate`
- `POST /api/v1/bar/bills/{bill_id}/pay-intents`
- `POST /api/v1/bar/bills/{bill_id}/settle`
- `GET /api/v1/bar/orders`
- `POST /api/v1/bar/orders`
- `POST /api/v1/bar/orders/{order_id}/bill`
- `POST /api/v1/bar/orders/{order_id}/deliver`
- `POST /api/v1/bar/orders/{order_id}/items`
- `GET /api/v1/bar/production-tickets`
- `GET /api/v1/bar/products`
- `POST /api/v1/bar/products`
- `GET /api/v1/bar/reservations`
- `POST /api/v1/bar/reservations`
- `POST /api/v1/bar/reservations/{id}/check-in-guest`
- `GET /api/v1/bar/tables`
- `POST /api/v1/bar/tables`
- `POST /api/v1/bar/tables/{table_id}/open`

Also complete customer-facing bar reservation paths if still missing:

- `GET /api/v1/customer/bar-reservation-options`
- `POST /api/v1/customer/bar-reservations`

## 3. Out of Scope

- P1 advanced POS features: split bill, merge bill, transfer table,挂账, service fee, low-consumption difference auto补齐, recipe cost, employee performance settlement.
- Real age/face supplier SDK integration. P0 stores adapter boundary, result, evidence, and failure/review status only.
- Real aggregated payment provider call. P0 reuses finance payment intent placeholder.
- Complex bar floor map UX and CAD/SVG import.

## 4. Files To Add Or Modify

Add:

- `backend/src/main/resources/db/migration/V013__p0_bar_reservation_order_billing.sql`
- `backend/src/main/java/com/complex/platform/bar/BarController.java`
- `backend/src/main/java/com/complex/platform/bar/BarService.java`
- `backend/src/main/java/com/complex/platform/bar/BarRepository.java`
- `backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java`
- `backend/src/main/java/com/complex/platform/bar/*Record.java`
- `backend/src/main/java/com/complex/platform/bar/*Response.java`
- `backend/src/main/java/com/complex/platform/bar/dto/*Request.java`
- `backend/src/test/java/com/complex/platform/bar/BarControllerTests.java`
- `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`
- `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`

Modify if needed:

- `backend/src/main/java/com/complex/platform/customer/CustomerController.java`
- `backend/src/main/java/com/complex/platform/customer/CustomerService.java`
- `backend/src/main/java/com/complex/platform/customer/CustomerRepository.java`
- `backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java`
- Customer tests for bar reservation options and customer reservation.
- `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java` if it still asserts latest Flyway version.

## 5. Data Model

Add PostgreSQL tables with idempotent Flyway DDL:

- `bar_tables`
  - `id`, `bar_id`, `table_code`, `table_name`, `table_type`, `capacity`, `allowed_pass_free_guest_count`, `min_consumption_cent`, `currency`, `status`, `created_at`, `updated_at`
  - `table_type` includes `normal_table` and `card_seat`.
- `bar_reservations`
  - `id`, `bar_id`, `table_id`, `customer_id`, `reserved_at`, `guest_count`, `allowed_pass_free_guest_count`, `deposit_amount_cent`, `deposit_status`, `order_source`, `sales_staff_id`, `status`, `created_terminal`, `device_id`, `evidence_attachment_ids`, `created_at`, `updated_at`
- `bar_table_guest_checkins`
  - `id`, `reservation_id`, `bar_id`, `table_id`, `guest_identifier`, `verification_result`, `checked_in_count_after`, `device_id`, `staff_id`, `evidence_attachment_ids`, `created_at`
- `bar_age_verifications`
  - `id`, `business_id`, `credential_payload`, `device_id`, `staff_id`, `result`, `provider_code`, `review_status`, `evidence_attachment_ids`, `created_at`
- `bar_products`
  - `id`, `bar_id`, `product_name`, `product_type`, `price_cent`, `currency`, `inventory_item_id`, `stock_location_id`, `stock_deduction_required`, `status`, `created_at`, `updated_at`
- `bar_orders`
  - `id`, `bar_id`, `table_id`, `reservation_id`, `customer_id`, `order_source`, `status`, `total_amount_cent`, `currency`, `created_terminal`, `device_id`, `created_at`, `updated_at`
- `bar_order_items`
  - `id`, `order_id`, `product_id`, `product_name`, `quantity`, `unit_price_cent`, `amount_cent`, `delivery_status`, `created_at`, `updated_at`
- `bar_production_tickets`
  - `id`, `order_id`, `bar_id`, `ticket_status`, `created_at`, `updated_at`
- `bar_bills`
  - `id`, `bill_no`, `order_id`, `bar_id`, `table_id`, `reservation_id`, `subtotal_amount_cent`, `discount_amount_cent`, `deposit_deduct_amount_cent`, `payable_amount_cent`, `paid_amount_cent`, `currency`, `payment_status`, `settlement_status`, `created_at`, `updated_at`
- `bar_commission_calculations`
  - `id`, `bill_id`, `sales_staff_id`, `sales_role_type`, `commission_base_cent`, `commission_rate_bp`, `commission_amount_cent`, `status`, `created_at`

Seed permissions and attach to `role_platform_admin`:

- `bar.entry.age_verify`
- `bar.table_commission.calculate`
- `bar.order.view`
- `bar.order.create`
- `bar.reservation.view`
- `bar.reservation.create`
- `bar.table.guest_checkin`
- `bar.table.view`
- `bar.table.manage`
- `bar.table.open_close`

Finance permissions already exist and must be reused:

- `finance.payment_intent.create`
- `finance.payment.register`

## 6. Business Rules

- Money fields are integer cents only. Do not use float/double.
- Ticketing and card-seat/table minimum spend are unrelated. Ticket/pass benefits must not offset table minimum consumption.
- A card-seat reservation may grant pass-free entry only up to that reservation/table's `allowed_pass_free_guest_count`.
- Card-seat pass-free entry is valid only for the reserved bar. It does not grant entry to any other bar and does not create a ticket entitlement.
- `check-in-guest` must reject when the reservation is missing, cancelled, not in this bar context, or the free-entry quota is exhausted.
- `table.open` must only open active tables and should link reservation when provided.
- Reservation deposit is recorded as bar reservation deposit. It is not ticketing payment and is not a pass benefit.
- Product price and order amount use cents; order item amount = `unit_price_cent * quantity`.
- `deliver` must confirm delivery and, for stock-managed products, must enforce inventory deduction online. If stock is insufficient, reject delivery and leave the item undelivered.
- `create bill` calculates bill from order items and reservation deposit. Low consumption is not automatically offset by ticketing or card-seat benefits in P0.
- `pay-intents` delegates to or mirrors the existing finance payment intent placeholder; it must not create a second payment abstraction with different semantics.
- `settle` records bill settlement status and payment evidence. Offline/cash/POS settlement must require staff permission and evidence fields when provided.
- Commission calculation:
  - If the bill has a `sales_staff_id` / sales role, commission rate is 15%.
  - Non-sales staff commission rate is 10%.
  - Commission is only for card-seat/table consumption, not for ticket/pass sales.
  - Store rate in basis points (`1500` or `1000`) and amount in cents.
- Age/face verification:
  - P0 records request, result, provider boundary, evidence, and review status.
  - Real supplier SDK/API remains adapter placeholder until supplier documents arrive.

## 7. Interface Rules

- Every endpoint must call `AuthorizationService.requirePermission(...)` with exact OpenAPI permissions.
- Customer reservation endpoints must use customer-token semantics already established in `CustomerService`; do not require staff permission for customer self-service.
- Public/customer bar reservation options must return only active tables/products/resources suitable for customer reservation.
- All write endpoints with `x-idempotency-required: true` must reject missing/blank `idempotency_key` with `VALIDATION_FAILED`.
- All write operations must go through `IdempotencyService`.
- SQL must use `NamedParameterJdbcTemplate` with named parameters.
- Do not introduce legacy `/api/admin/*`, `/api/mobile/*`, or `/api/integration/*` compatibility routes.

## 8. Audit Events

Use exact OpenAPI event names:

- `bar.age_verification.passed` / `bar.age_verification.failed`
- `bar.table_commission.calculated` / `bar.table_commission.calculate_failed`
- `payment.intent_created` / `payment.intent_failed`
- `bar.bill.settled` / `bar.bill.settle_failed`
- `bar.order.viewed` / `bar.order.view_failed`
- `bar.order_created` / `bar.order_create_failed`
- `bar.bill.created` / `bar.bill.create_failed`
- `bar.order_delivered` / `bar.order_deliver_failed`
- `bar.order_item.created` / `bar.order_item.create_failed`
- `bar.production_ticket.viewed` / `bar.production_ticket.view_failed`
- `bar.product.viewed` / `bar.product.viewed_failed`
- `bar.product.created` / `bar.product.create_failed`
- `bar.reservation.viewed` / `bar.reservation.view_failed`
- `bar.reservation.created` / `bar.reservation.create_failed`
- `bar.table_guest.checked_in` / `bar.table_guest.checkin_failed`
- `bar.table.viewed` / `bar.table.view_failed`
- `bar.table.changed` / `bar.table.change_failed`
- `bar.table.opened` / `bar.table.open_failed`
- `customer.bar_reservation_options_viewed` / `customer.bar_reservation_options_view_failed`
- `customer.bar_reservation.created` / `customer.bar_reservation.create_failed`

If OpenAPI has a different exact failure event, OpenAPI wins.

## 9. TDD Plan

RED tests first:

- Controller permission tests for every bar endpoint.
- Controller missing idempotency tests for all write endpoints.
- Service tests for reservation quota, same-bar restriction, no ticket/pass offset, deposit recording, order amount, bill amount, commission 15%/10%, and stock-managed delivery rejection.
- Customer service tests for reservation options and customer-created reservation.
- JDBC tests for V013 tables, permissions, save/list flows, commission rows, and guest check-in rows.

GREEN implementation:

- Add V013 migration.
- Add bar package controller/service/repository/records/DTOs.
- Extend customer package for bar reservation options and customer reservation.
- Keep supplier/age verification as adapter boundary record, not real SDK.

Refactor only after targeted tests pass.

## 10. Verification

Run targeted tests:

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=BarServiceTests,BarControllerTests,BarJdbcIntegrationTests,CustomerServiceTests,CustomerControllerTests,CustomerJdbcIntegrationTests test
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
grep -R -n -E 'createStatement|Statement|String\\.format|SELECT .*\\+|UPDATE .*\\+|INSERT .*\\+|DELETE .*\\+|Double|double|getDouble|Math\\.round' backend/src/main/java/com/complex/platform/bar backend/src/test/java/com/complex/platform/bar || true
```

## 11. Rollback

- Before implementation, create a tar snapshot under `/home/james/complex-management-backups/`.
- If the slice fails after V013 migration, restore the snapshot for code and reset the development database only if needed.
- Do not edit or rewrite already applied V001-V012 migrations; add only V013 for this slice.
