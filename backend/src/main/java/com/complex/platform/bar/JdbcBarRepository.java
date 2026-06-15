package com.complex.platform.bar;

import com.complex.platform.finance.PaymentIntentRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcBarRepository implements BarRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcBarRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public BarTableRecord saveTable(BarTableRecord table) {
        jdbcTemplate.update("""
                INSERT INTO bar_tables (
                    id, bar_id, table_code, table_name, table_type, capacity,
                    allowed_pass_free_guest_count, min_consumption_cent, currency,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :table_code, :table_name, :table_type, :capacity,
                    :allowed_pass_free_guest_count, :min_consumption_cent, :currency,
                    :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    table_code = EXCLUDED.table_code,
                    table_name = EXCLUDED.table_name,
                    table_type = EXCLUDED.table_type,
                    capacity = EXCLUDED.capacity,
                    allowed_pass_free_guest_count = EXCLUDED.allowed_pass_free_guest_count,
                    min_consumption_cent = EXCLUDED.min_consumption_cent,
                    currency = EXCLUDED.currency,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, tableParameters(table));
        return table;
    }

    @Override
    public List<BarTableRecord> listTables() {
        return jdbcTemplate.query("""
                SELECT id, bar_id, table_code, table_name, table_type, capacity,
                       allowed_pass_free_guest_count, min_consumption_cent, currency,
                       status, created_at, updated_at
                  FROM bar_tables
                 ORDER BY created_at DESC, id
                """, this::mapTable);
    }

    @Override
    public Optional<BarTableRecord> findTableById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, table_code, table_name, table_type, capacity,
                       allowed_pass_free_guest_count, min_consumption_cent, currency,
                       status, created_at, updated_at
                  FROM bar_tables
                 WHERE id = :id
                """, Map.of("id", id), this::mapTable).stream().findFirst();
    }

    @Override
    public BarReservationRecord saveReservation(BarReservationRecord reservation) {
        jdbcTemplate.update("""
                INSERT INTO bar_reservations (
                    id, bar_id, table_id, customer_id, reserved_at, guest_count,
                    allowed_pass_free_guest_count, deposit_amount_cent, deposit_status,
                    order_source, sales_staff_id, status, created_terminal, device_id,
                    evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :table_id, :customer_id, :reserved_at, :guest_count,
                    :allowed_pass_free_guest_count, :deposit_amount_cent, :deposit_status,
                    :order_source, :sales_staff_id, :status, :created_terminal, :device_id,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    table_id = EXCLUDED.table_id,
                    customer_id = EXCLUDED.customer_id,
                    reserved_at = EXCLUDED.reserved_at,
                    guest_count = EXCLUDED.guest_count,
                    allowed_pass_free_guest_count = EXCLUDED.allowed_pass_free_guest_count,
                    deposit_amount_cent = EXCLUDED.deposit_amount_cent,
                    deposit_status = EXCLUDED.deposit_status,
                    order_source = EXCLUDED.order_source,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    status = EXCLUDED.status,
                    created_terminal = EXCLUDED.created_terminal,
                    device_id = EXCLUDED.device_id,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, reservationParameters(reservation));
        return reservation;
    }

    @Override
    public List<BarReservationRecord> listReservations() {
        return jdbcTemplate.query("""
                SELECT id, bar_id, table_id, customer_id, reserved_at, guest_count,
                       allowed_pass_free_guest_count, deposit_amount_cent, deposit_status,
                       order_source, sales_staff_id, status, created_terminal, device_id,
                       evidence_attachment_ids, created_at, updated_at
                  FROM bar_reservations
                 ORDER BY created_at DESC, id
                """, this::mapReservation);
    }

    @Override
    public Optional<BarReservationRecord> findReservationById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, table_id, customer_id, reserved_at, guest_count,
                       allowed_pass_free_guest_count, deposit_amount_cent, deposit_status,
                       order_source, sales_staff_id, status, created_terminal, device_id,
                       evidence_attachment_ids, created_at, updated_at
                  FROM bar_reservations
                 WHERE id = :id
                """, Map.of("id", id), this::mapReservation).stream().findFirst();
    }

    @Override
    public int countGuestCheckins(String reservationId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)::int
                  FROM bar_table_guest_checkins
                 WHERE reservation_id = :reservation_id
                """, Map.of("reservation_id", reservationId), Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public BarTableGuestCheckinRecord saveGuestCheckin(BarTableGuestCheckinRecord checkin) {
        jdbcTemplate.update("""
                INSERT INTO bar_table_guest_checkins (
                    id, reservation_id, bar_id, table_id, guest_identifier,
                    verification_result, checked_in_count_after, device_id, staff_id,
                    evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :reservation_id, :bar_id, :table_id, :guest_identifier,
                    :verification_result, :checked_in_count_after, :device_id, :staff_id,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    verification_result = EXCLUDED.verification_result,
                    checked_in_count_after = EXCLUDED.checked_in_count_after,
                    device_id = EXCLUDED.device_id,
                    staff_id = EXCLUDED.staff_id,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, checkinParameters(checkin));
        return checkin;
    }

    @Override
    public BarAgeVerificationRecord saveAgeVerification(BarAgeVerificationRecord verification) {
        jdbcTemplate.update("""
                INSERT INTO bar_age_verifications (
                    id, business_id, credential_payload, device_id, staff_id,
                    result, provider_code, review_status, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :business_id, :credential_payload, :device_id, :staff_id,
                    :result, :provider_code, :review_status,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    result = EXCLUDED.result,
                    provider_code = EXCLUDED.provider_code,
                    review_status = EXCLUDED.review_status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, ageVerificationParameters(verification));
        return verification;
    }

    @Override
    public BarProductRecord saveProduct(BarProductRecord product) {
        jdbcTemplate.update("""
                INSERT INTO bar_products (
                    id, bar_id, product_name, product_type, price_cent, currency,
                    inventory_item_id, stock_location_id, stock_deduction_required,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :product_name, :product_type, :price_cent, :currency,
                    :inventory_item_id, :stock_location_id, :stock_deduction_required,
                    :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    product_name = EXCLUDED.product_name,
                    product_type = EXCLUDED.product_type,
                    price_cent = EXCLUDED.price_cent,
                    currency = EXCLUDED.currency,
                    inventory_item_id = EXCLUDED.inventory_item_id,
                    stock_location_id = EXCLUDED.stock_location_id,
                    stock_deduction_required = EXCLUDED.stock_deduction_required,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, productParameters(product));
        return product;
    }

    @Override
    public List<BarProductRecord> listProducts() {
        return jdbcTemplate.query("""
                SELECT id, bar_id, product_name, product_type, price_cent, currency,
                       inventory_item_id, stock_location_id, stock_deduction_required,
                       status, created_at, updated_at
                  FROM bar_products
                 ORDER BY created_at DESC, id
                """, this::mapProduct);
    }

    @Override
    public Optional<BarProductRecord> findProductById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, product_name, product_type, price_cent, currency,
                       inventory_item_id, stock_location_id, stock_deduction_required,
                       status, created_at, updated_at
                  FROM bar_products
                 WHERE id = :id
                """, Map.of("id", id), this::mapProduct).stream().findFirst();
    }

    @Override
    public BarOrderRecord saveOrder(BarOrderRecord order) {
        jdbcTemplate.update("""
                INSERT INTO bar_orders (
                    id, bar_id, table_id, reservation_id, customer_id, order_source,
                    status, total_amount_cent, currency, created_terminal, device_id,
                    sales_staff_id, created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :table_id, :reservation_id, :customer_id, :order_source,
                    :status, :total_amount_cent, :currency, :created_terminal, :device_id,
                    :sales_staff_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    table_id = EXCLUDED.table_id,
                    reservation_id = EXCLUDED.reservation_id,
                    customer_id = EXCLUDED.customer_id,
                    order_source = EXCLUDED.order_source,
                    status = EXCLUDED.status,
                    total_amount_cent = EXCLUDED.total_amount_cent,
                    currency = EXCLUDED.currency,
                    created_terminal = EXCLUDED.created_terminal,
                    device_id = EXCLUDED.device_id,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    updated_at = EXCLUDED.updated_at
                """, orderParameters(order));
        return order;
    }

    @Override
    public List<BarOrderRecord> listOrders() {
        return jdbcTemplate.query("""
                SELECT id, bar_id, table_id, reservation_id, customer_id, order_source,
                       status, total_amount_cent, currency, created_terminal, device_id,
                       sales_staff_id, created_at, updated_at
                  FROM bar_orders
                 ORDER BY created_at DESC, id
                """, this::mapOrder);
    }

    @Override
    public Optional<BarOrderRecord> findOrderById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, table_id, reservation_id, customer_id, order_source,
                       status, total_amount_cent, currency, created_terminal, device_id,
                       sales_staff_id, created_at, updated_at
                  FROM bar_orders
                 WHERE id = :id
                """, Map.of("id", id), this::mapOrder).stream().findFirst();
    }

    @Override
    public BarOrderItemRecord saveOrderItem(BarOrderItemRecord item) {
        jdbcTemplate.update("""
                INSERT INTO bar_order_items (
                    id, order_id, product_id, product_name, quantity, unit_price_cent,
                    amount_cent, delivery_status, created_at, updated_at
                )
                VALUES (
                    :id, :order_id, :product_id, :product_name, :quantity, :unit_price_cent,
                    :amount_cent, :delivery_status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    product_name = EXCLUDED.product_name,
                    quantity = EXCLUDED.quantity,
                    unit_price_cent = EXCLUDED.unit_price_cent,
                    amount_cent = EXCLUDED.amount_cent,
                    delivery_status = EXCLUDED.delivery_status,
                    updated_at = EXCLUDED.updated_at
                """, orderItemParameters(item));
        return item;
    }

    @Override
    public List<BarOrderItemRecord> listOrderItems(String orderId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, product_id, product_name, quantity, unit_price_cent,
                       amount_cent, delivery_status, created_at, updated_at
                  FROM bar_order_items
                 WHERE order_id = :order_id
                 ORDER BY created_at, id
                """, Map.of("order_id", orderId), this::mapOrderItem);
    }

    @Override
    public Optional<BarOrderItemRecord> findOrderItemById(String id) {
        return jdbcTemplate.query("""
                SELECT id, order_id, product_id, product_name, quantity, unit_price_cent,
                       amount_cent, delivery_status, created_at, updated_at
                  FROM bar_order_items
                 WHERE id = :id
                """, Map.of("id", id), this::mapOrderItem).stream().findFirst();
    }

    @Override
    public BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket) {
        jdbcTemplate.update("""
                INSERT INTO bar_production_tickets (
                    id, order_id, bar_id, ticket_status, created_at, updated_at
                )
                VALUES (
                    :id, :order_id, :bar_id, :ticket_status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_status = EXCLUDED.ticket_status,
                    updated_at = EXCLUDED.updated_at
                """, productionTicketParameters(ticket));
        return ticket;
    }

    @Override
    public List<BarProductionTicketRecord> listProductionTickets() {
        return jdbcTemplate.query("""
                SELECT id, order_id, bar_id, ticket_status, created_at, updated_at
                  FROM bar_production_tickets
                 ORDER BY created_at DESC, id
                """, this::mapProductionTicket);
    }

    @Override
    public Optional<BarProductionTicketRecord> findProductionTicketById(String id) {
        return jdbcTemplate.query("""
                SELECT id, order_id, bar_id, ticket_status, created_at, updated_at
                  FROM bar_production_tickets
                 WHERE id = :id
                """, Map.of("id", id), this::mapProductionTicket).stream().findFirst();
    }

    @Override
    public Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, bar_id, ticket_status, created_at, updated_at
                  FROM bar_production_tickets
                 WHERE order_id = :order_id
                 ORDER BY created_at DESC, id
                 LIMIT 1
                """, Map.of("order_id", orderId), this::mapProductionTicket).stream().findFirst();
    }

    @Override
    public BarProductionTicketEventRecord saveProductionTicketEvent(BarProductionTicketEventRecord event) {
        jdbcTemplate.update("""
                INSERT INTO bar_production_ticket_events (
                    id, ticket_id, order_id, bar_id, action_type, from_status, to_status,
                    station_code, operator_user_id, operator_note, idempotency_key,
                    evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :ticket_id, :order_id, :bar_id, :action_type, :from_status, :to_status,
                    :station_code, :operator_user_id, :operator_note, :idempotency_key,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (ticket_id, idempotency_key) DO UPDATE SET
                    action_type = EXCLUDED.action_type,
                    from_status = EXCLUDED.from_status,
                    to_status = EXCLUDED.to_status,
                    station_code = EXCLUDED.station_code,
                    operator_user_id = EXCLUDED.operator_user_id,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, productionTicketEventParameters(event));
        return event;
    }

    @Override
    public List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
        return jdbcTemplate.query("""
                SELECT id, ticket_id, order_id, bar_id, action_type, from_status, to_status,
                       station_code, operator_user_id, operator_note, idempotency_key,
                       evidence_attachment_ids, created_at
                  FROM bar_production_ticket_events
                 WHERE ticket_id = :ticket_id
                 ORDER BY created_at DESC, id
                """, Map.of("ticket_id", ticketId), this::mapProductionTicketEvent);
    }

    @Override
    public BarBillRecord saveBill(BarBillRecord bill) {
        jdbcTemplate.update("""
                INSERT INTO bar_bills (
                    id, bill_no, order_id, bar_id, table_id, reservation_id,
                    subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                    payable_amount_cent, paid_amount_cent, currency, payment_status,
                    settlement_status, sales_staff_id, sales_role_type, created_at, updated_at
                )
                VALUES (
                    :id, :bill_no, :order_id, :bar_id, :table_id, :reservation_id,
                    :subtotal_amount_cent, :discount_amount_cent, :deposit_deduct_amount_cent,
                    :payable_amount_cent, :paid_amount_cent, :currency, :payment_status,
                    :settlement_status, :sales_staff_id, :sales_role_type, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    subtotal_amount_cent = EXCLUDED.subtotal_amount_cent,
                    discount_amount_cent = EXCLUDED.discount_amount_cent,
                    deposit_deduct_amount_cent = EXCLUDED.deposit_deduct_amount_cent,
                    payable_amount_cent = EXCLUDED.payable_amount_cent,
                    paid_amount_cent = EXCLUDED.paid_amount_cent,
                    payment_status = EXCLUDED.payment_status,
                    settlement_status = EXCLUDED.settlement_status,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    sales_role_type = EXCLUDED.sales_role_type,
                    updated_at = EXCLUDED.updated_at
                """, billParameters(bill));
        return bill;
    }

    @Override
    public Optional<BarBillRecord> findBillById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bill_no, order_id, bar_id, table_id, reservation_id,
                       subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                       payable_amount_cent, paid_amount_cent, currency, payment_status,
                       settlement_status, sales_staff_id, sales_role_type, created_at, updated_at
                  FROM bar_bills
                 WHERE id = :id
                """, Map.of("id", id), this::mapBill).stream().findFirst();
    }

    @Override
    public BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_adjustments (
                    id, bill_id, adjustment_type, amount_cent,
                    before_payable_amount_cent, after_payable_amount_cent,
                    reason, requested_by_user_id, approval_required, status,
                    idempotency_key, evidence_attachment_ids, ar_ap_item_id, snapshot_json, created_at
                )
                VALUES (
                    :id, :bill_id, :adjustment_type, :amount_cent,
                    :before_payable_amount_cent, :after_payable_amount_cent,
                    :reason, :requested_by_user_id, :approval_required, :status,
                    :idempotency_key, CAST(:evidence_attachment_ids AS jsonb),
                    :ar_ap_item_id, CAST(:snapshot_json AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    amount_cent = EXCLUDED.amount_cent,
                    before_payable_amount_cent = EXCLUDED.before_payable_amount_cent,
                    after_payable_amount_cent = EXCLUDED.after_payable_amount_cent,
                    reason = EXCLUDED.reason,
                    requested_by_user_id = EXCLUDED.requested_by_user_id,
                    approval_required = EXCLUDED.approval_required,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    ar_ap_item_id = EXCLUDED.ar_ap_item_id,
                    snapshot_json = EXCLUDED.snapshot_json
                """, billAdjustmentParameters(adjustment));
        return adjustment;
    }

    @Override
    public List<BarBillAdjustmentRecord> listBillAdjustments(String billId) {
        return jdbcTemplate.query("""
                SELECT id, bill_id, adjustment_type, amount_cent,
                       before_payable_amount_cent, after_payable_amount_cent,
                       reason, requested_by_user_id, approval_required, status,
                       idempotency_key, evidence_attachment_ids, ar_ap_item_id, snapshot_json, created_at
                  FROM bar_bill_adjustments
                 WHERE bill_id = :bill_id
                 ORDER BY created_at, id
                """, Map.of("bill_id", billId), this::mapBillAdjustment);
    }

    @Override
    public BarBillOperationRecord saveBillOperation(BarBillOperationRecord operation) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_operations (
                    id, operation_type, primary_bill_id, secondary_bill_id, generated_bill_id,
                    amount_cent, reason, requested_by_user_id, status, idempotency_key,
                    evidence_attachment_ids, snapshot_json, created_at
                )
                VALUES (
                    :id, :operation_type, :primary_bill_id, :secondary_bill_id, :generated_bill_id,
                    :amount_cent, :reason, :requested_by_user_id, :status, :idempotency_key,
                    CAST(:evidence_attachment_ids AS jsonb), CAST(:snapshot_json AS jsonb), :created_at
                )
                ON CONFLICT (primary_bill_id, idempotency_key) DO UPDATE SET
                    secondary_bill_id = EXCLUDED.secondary_bill_id,
                    generated_bill_id = EXCLUDED.generated_bill_id,
                    amount_cent = EXCLUDED.amount_cent,
                    reason = EXCLUDED.reason,
                    requested_by_user_id = EXCLUDED.requested_by_user_id,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    snapshot_json = EXCLUDED.snapshot_json
                """, billOperationParameters(operation));
        return operation;
    }

    @Override
    public List<BarBillOperationRecord> listBillOperations(String billId) {
        return jdbcTemplate.query("""
                SELECT id, operation_type, primary_bill_id, secondary_bill_id, generated_bill_id,
                       amount_cent, reason, requested_by_user_id, status, idempotency_key,
                       evidence_attachment_ids, snapshot_json, created_at
                  FROM bar_bill_operations
                 WHERE primary_bill_id = :bill_id OR secondary_bill_id = :bill_id OR generated_bill_id = :bill_id
                 ORDER BY created_at, id
                """, Map.of("bill_id", billId), this::mapBillOperation);
    }

    @Override
    public BarCommissionCalculationRecord saveCommissionCalculation(BarCommissionCalculationRecord calculation) {
        jdbcTemplate.update("""
                INSERT INTO bar_commission_calculations (
                    id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,
                    commission_rate_bp, commission_amount_cent, status, created_at
                )
                VALUES (
                    :id, :bill_id, :sales_staff_id, :sales_role_type, :commission_base_cent,
                    :commission_rate_bp, :commission_amount_cent, :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    sales_role_type = EXCLUDED.sales_role_type,
                    commission_base_cent = EXCLUDED.commission_base_cent,
                    commission_rate_bp = EXCLUDED.commission_rate_bp,
                    commission_amount_cent = EXCLUDED.commission_amount_cent,
                    status = EXCLUDED.status
                """, commissionParameters(calculation));
        return calculation;
    }

    @Override
    public BarTableSessionRecord saveTableSession(BarTableSessionRecord session) {
        jdbcTemplate.update("""
                INSERT INTO bar_table_sessions (
                    id, bar_id, merchant_id, business_date, table_id, reservation_id,
                    customer_id, guest_count, min_consumption_snapshot_cent,
                    allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                    status, opened_at, closed_at, source_session_id, session_version,
                    created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :business_date, :table_id, :reservation_id,
                    :customer_id, :guest_count, :min_consumption_snapshot_cent,
                    :allowed_pass_free_guest_count_snapshot, :sales_staff_id, :service_staff_id,
                    :status, :opened_at, :closed_at, :source_session_id, :session_version,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    merchant_id = EXCLUDED.merchant_id,
                    business_date = EXCLUDED.business_date,
                    table_id = EXCLUDED.table_id,
                    reservation_id = EXCLUDED.reservation_id,
                    customer_id = EXCLUDED.customer_id,
                    guest_count = EXCLUDED.guest_count,
                    min_consumption_snapshot_cent = EXCLUDED.min_consumption_snapshot_cent,
                    allowed_pass_free_guest_count_snapshot = EXCLUDED.allowed_pass_free_guest_count_snapshot,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    service_staff_id = EXCLUDED.service_staff_id,
                    status = EXCLUDED.status,
                    opened_at = EXCLUDED.opened_at,
                    closed_at = EXCLUDED.closed_at,
                    source_session_id = EXCLUDED.source_session_id,
                    session_version = EXCLUDED.session_version,
                    updated_at = EXCLUDED.updated_at
                """, tableSessionParameters(session));
        return session;
    }

    @Override
    public List<BarTableSessionRecord> listTableSessions() {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, business_date, table_id, reservation_id,
                       customer_id, guest_count, min_consumption_snapshot_cent,
                       allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                       status, opened_at, closed_at, source_session_id, session_version,
                       created_at, updated_at
                  FROM bar_table_sessions
                 ORDER BY created_at DESC, id
                """, this::mapTableSession);
    }

    @Override
    public Optional<BarTableSessionRecord> findTableSessionById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, business_date, table_id, reservation_id,
                       customer_id, guest_count, min_consumption_snapshot_cent,
                       allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                       status, opened_at, closed_at, source_session_id, session_version,
                       created_at, updated_at
                  FROM bar_table_sessions
                 WHERE id = :id
                """, Map.of("id", id), this::mapTableSession).stream().findFirst();
    }

    @Override
    public BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO bar_table_session_adjustments (
                    id, session_id, adjustment_type, source_session_id, target_session_id,
                    from_table_id, to_table_id, bar_id, merchant_id, business_date,
                    min_consumption_before_cent, min_consumption_after_cent, renewed_until,
                    additional_min_consumption_cent, additional_deposit_amount_cent,
                    additional_service_fee_cent, reason, requested_by_user_id,
                    approval_required, status, idempotency_key, evidence_attachment_ids,
                    snapshot_json, created_at
                )
                VALUES (
                    :id, :session_id, :adjustment_type, :source_session_id, :target_session_id,
                    :from_table_id, :to_table_id, :bar_id, :merchant_id, :business_date,
                    :min_consumption_before_cent, :min_consumption_after_cent, :renewed_until,
                    :additional_min_consumption_cent, :additional_deposit_amount_cent,
                    :additional_service_fee_cent, :reason, :requested_by_user_id,
                    :approval_required, :status, :idempotency_key,
                    CAST(:evidence_attachment_ids AS jsonb), CAST(:snapshot_json AS jsonb),
                    :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    snapshot_json = EXCLUDED.snapshot_json
                """, tableSessionAdjustmentParameters(adjustment));
        return adjustment;
    }

    @Override
    public List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId) {
        return jdbcTemplate.query("""
                SELECT id, session_id, adjustment_type, source_session_id, target_session_id,
                       from_table_id, to_table_id, bar_id, merchant_id, business_date,
                       min_consumption_before_cent, min_consumption_after_cent, renewed_until,
                       additional_min_consumption_cent, additional_deposit_amount_cent,
                       additional_service_fee_cent, reason, requested_by_user_id,
                       approval_required, status, idempotency_key, evidence_attachment_ids,
                       snapshot_json, created_at
                  FROM bar_table_session_adjustments
                 WHERE session_id = :session_id
                 ORDER BY created_at DESC, id
                """, Map.of("session_id", sessionId), this::mapTableSessionAdjustment);
    }

    @Override
    public Optional<StockBalanceSnapshot> findStockBalance(String itemId, String locationId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at
                  FROM inventory_stock_balances
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                """, Map.of("item_id", itemId, "location_id", locationId), this::mapStockBalance).stream().findFirst();
    }

    @Override
    public StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance) {
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
    public StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement) {
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
                    quantity = EXCLUDED.quantity,
                    business_type = EXCLUDED.business_type,
                    business_id = EXCLUDED.business_id,
                    status = EXCLUDED.status
                """, stockMovementParameters(movement));
        return movement;
    }

    @Override
    public PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
        jdbcTemplate.update("""
                INSERT INTO finance_payment_intents (
                    id, business_order_type, business_order_id, amount_cent, currency,
                    channel, status, created_at, updated_at
                )
                VALUES (
                    :id, :business_order_type, :business_order_id, :amount_cent, :currency,
                    :channel, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    business_order_type = EXCLUDED.business_order_type,
                    business_order_id = EXCLUDED.business_order_id,
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency,
                    channel = EXCLUDED.channel,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, paymentIntentParameters(paymentIntent));
        return paymentIntent;
    }

    private MapSqlParameterSource tableParameters(BarTableRecord table) {
        return new MapSqlParameterSource()
                .addValue("id", table.id())
                .addValue("bar_id", table.barId())
                .addValue("table_code", table.tableCode())
                .addValue("table_name", table.tableName())
                .addValue("table_type", table.tableType())
                .addValue("capacity", table.capacity())
                .addValue("allowed_pass_free_guest_count", table.allowedPassFreeGuestCount())
                .addValue("min_consumption_cent", table.minConsumptionCent())
                .addValue("currency", table.currency())
                .addValue("status", table.status())
                .addValue("created_at", timestampWithTimeZone(table.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(table.updatedAt()));
    }

    private MapSqlParameterSource reservationParameters(BarReservationRecord reservation) {
        return new MapSqlParameterSource()
                .addValue("id", reservation.id())
                .addValue("bar_id", reservation.barId())
                .addValue("table_id", reservation.tableId())
                .addValue("customer_id", reservation.customerId())
                .addValue("reserved_at", timestampWithTimeZone(reservation.reservedAt()))
                .addValue("guest_count", reservation.guestCount())
                .addValue("allowed_pass_free_guest_count", reservation.allowedPassFreeGuestCount())
                .addValue("deposit_amount_cent", reservation.depositAmountCent())
                .addValue("deposit_status", reservation.depositStatus())
                .addValue("order_source", reservation.orderSource())
                .addValue("sales_staff_id", reservation.salesStaffId())
                .addValue("status", reservation.status())
                .addValue("created_terminal", reservation.createdTerminal())
                .addValue("device_id", reservation.deviceId())
                .addValue("evidence_attachment_ids", writeJson(reservation.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(reservation.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(reservation.updatedAt()));
    }

    private MapSqlParameterSource checkinParameters(BarTableGuestCheckinRecord checkin) {
        return new MapSqlParameterSource()
                .addValue("id", checkin.id())
                .addValue("reservation_id", checkin.reservationId())
                .addValue("bar_id", checkin.barId())
                .addValue("table_id", checkin.tableId())
                .addValue("guest_identifier", checkin.guestIdentifier())
                .addValue("verification_result", checkin.verificationResult())
                .addValue("checked_in_count_after", checkin.checkedInCountAfter())
                .addValue("device_id", checkin.deviceId())
                .addValue("staff_id", checkin.staffId())
                .addValue("evidence_attachment_ids", writeJson(checkin.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(checkin.createdAt()));
    }

    private MapSqlParameterSource ageVerificationParameters(BarAgeVerificationRecord verification) {
        return new MapSqlParameterSource()
                .addValue("id", verification.id())
                .addValue("business_id", verification.businessId())
                .addValue("credential_payload", verification.credentialPayload())
                .addValue("device_id", verification.deviceId())
                .addValue("staff_id", verification.staffId())
                .addValue("result", verification.result())
                .addValue("provider_code", verification.providerCode())
                .addValue("review_status", verification.reviewStatus())
                .addValue("evidence_attachment_ids", writeJson(verification.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(verification.createdAt()));
    }

    private MapSqlParameterSource productParameters(BarProductRecord product) {
        return new MapSqlParameterSource()
                .addValue("id", product.id())
                .addValue("bar_id", product.barId())
                .addValue("product_name", product.productName())
                .addValue("product_type", product.productType())
                .addValue("price_cent", product.priceCent())
                .addValue("currency", product.currency())
                .addValue("inventory_item_id", product.inventoryItemId())
                .addValue("stock_location_id", product.stockLocationId())
                .addValue("stock_deduction_required", product.stockDeductionRequired())
                .addValue("status", product.status())
                .addValue("created_at", timestampWithTimeZone(product.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(product.updatedAt()));
    }

    private MapSqlParameterSource orderParameters(BarOrderRecord order) {
        return new MapSqlParameterSource()
                .addValue("id", order.id())
                .addValue("bar_id", order.barId())
                .addValue("table_id", order.tableId())
                .addValue("reservation_id", order.reservationId())
                .addValue("customer_id", order.customerId())
                .addValue("order_source", order.orderSource())
                .addValue("status", order.status())
                .addValue("total_amount_cent", order.totalAmountCent())
                .addValue("currency", order.currency())
                .addValue("created_terminal", order.createdTerminal())
                .addValue("device_id", order.deviceId())
                .addValue("sales_staff_id", order.salesStaffId())
                .addValue("created_at", timestampWithTimeZone(order.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(order.updatedAt()));
    }

    private MapSqlParameterSource orderItemParameters(BarOrderItemRecord item) {
        return new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("order_id", item.orderId())
                .addValue("product_id", item.productId())
                .addValue("product_name", item.productName())
                .addValue("quantity", item.quantity())
                .addValue("unit_price_cent", item.unitPriceCent())
                .addValue("amount_cent", item.amountCent())
                .addValue("delivery_status", item.deliveryStatus())
                .addValue("created_at", timestampWithTimeZone(item.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(item.updatedAt()));
    }

    private MapSqlParameterSource productionTicketParameters(BarProductionTicketRecord ticket) {
        return new MapSqlParameterSource()
                .addValue("id", ticket.id())
                .addValue("order_id", ticket.orderId())
                .addValue("bar_id", ticket.barId())
                .addValue("ticket_status", ticket.ticketStatus())
                .addValue("created_at", timestampWithTimeZone(ticket.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(ticket.updatedAt()));
    }

    private MapSqlParameterSource productionTicketEventParameters(BarProductionTicketEventRecord event) {
        return new MapSqlParameterSource()
                .addValue("id", event.id())
                .addValue("ticket_id", event.ticketId())
                .addValue("order_id", event.orderId())
                .addValue("bar_id", event.barId())
                .addValue("action_type", event.actionType())
                .addValue("from_status", event.fromStatus())
                .addValue("to_status", event.toStatus())
                .addValue("station_code", event.stationCode())
                .addValue("operator_user_id", event.operatorUserId())
                .addValue("operator_note", event.operatorNote())
                .addValue("idempotency_key", event.idempotencyKey())
                .addValue("evidence_attachment_ids", writeJson(event.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(event.createdAt()));
    }

    private MapSqlParameterSource billParameters(BarBillRecord bill) {
        return new MapSqlParameterSource()
                .addValue("id", bill.id())
                .addValue("bill_no", bill.billNo())
                .addValue("order_id", bill.orderId())
                .addValue("bar_id", bill.barId())
                .addValue("table_id", bill.tableId())
                .addValue("reservation_id", bill.reservationId())
                .addValue("subtotal_amount_cent", bill.subtotalAmountCent())
                .addValue("discount_amount_cent", bill.discountAmountCent())
                .addValue("deposit_deduct_amount_cent", bill.depositDeductAmountCent())
                .addValue("payable_amount_cent", bill.payableAmountCent())
                .addValue("paid_amount_cent", bill.paidAmountCent())
                .addValue("currency", bill.currency())
                .addValue("payment_status", bill.paymentStatus())
                .addValue("settlement_status", bill.settlementStatus())
                .addValue("sales_staff_id", bill.salesStaffId())
                .addValue("sales_role_type", bill.salesRoleType())
                .addValue("created_at", timestampWithTimeZone(bill.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(bill.updatedAt()));
    }

    private MapSqlParameterSource billAdjustmentParameters(BarBillAdjustmentRecord adjustment) {
        return new MapSqlParameterSource()
                .addValue("id", adjustment.id())
                .addValue("bill_id", adjustment.billId())
                .addValue("adjustment_type", adjustment.adjustmentType())
                .addValue("amount_cent", adjustment.amountCent())
                .addValue("before_payable_amount_cent", adjustment.beforePayableAmountCent())
                .addValue("after_payable_amount_cent", adjustment.afterPayableAmountCent())
                .addValue("reason", adjustment.reason())
                .addValue("requested_by_user_id", adjustment.requestedByUserId())
                .addValue("approval_required", adjustment.approvalRequired())
                .addValue("status", adjustment.status())
                .addValue("idempotency_key", adjustment.idempotencyKey())
                .addValue("evidence_attachment_ids", writeJson(adjustment.evidenceAttachmentIds()))
                .addValue("ar_ap_item_id", adjustment.arApItemId())
                .addValue("snapshot_json", adjustment.snapshotJson() == null ? "{}" : adjustment.snapshotJson())
                .addValue("created_at", timestampWithTimeZone(adjustment.createdAt()));
    }

    private MapSqlParameterSource billOperationParameters(BarBillOperationRecord operation) {
        return new MapSqlParameterSource()
                .addValue("id", operation.id())
                .addValue("operation_type", operation.operationType())
                .addValue("primary_bill_id", operation.primaryBillId())
                .addValue("secondary_bill_id", operation.secondaryBillId())
                .addValue("generated_bill_id", operation.generatedBillId())
                .addValue("amount_cent", operation.amountCent())
                .addValue("reason", operation.reason())
                .addValue("requested_by_user_id", operation.requestedByUserId())
                .addValue("status", operation.status())
                .addValue("idempotency_key", operation.idempotencyKey())
                .addValue("evidence_attachment_ids", writeJson(operation.evidenceAttachmentIds()))
                .addValue("snapshot_json", operation.snapshotJson() == null ? "{}" : operation.snapshotJson())
                .addValue("created_at", timestampWithTimeZone(operation.createdAt()));
    }

    private MapSqlParameterSource commissionParameters(BarCommissionCalculationRecord calculation) {
        return new MapSqlParameterSource()
                .addValue("id", calculation.id())
                .addValue("bill_id", calculation.billId())
                .addValue("sales_staff_id", calculation.salesStaffId())
                .addValue("sales_role_type", calculation.salesRoleType())
                .addValue("commission_base_cent", calculation.commissionBaseCent())
                .addValue("commission_rate_bp", calculation.commissionRateBp())
                .addValue("commission_amount_cent", calculation.commissionAmountCent())
                .addValue("status", calculation.status())
                .addValue("created_at", timestampWithTimeZone(calculation.createdAt()));
    }

    private MapSqlParameterSource tableSessionParameters(BarTableSessionRecord session) {
        return new MapSqlParameterSource()
                .addValue("id", session.id())
                .addValue("bar_id", session.barId())
                .addValue("merchant_id", session.merchantId())
                .addValue("business_date", session.businessDate())
                .addValue("table_id", session.tableId())
                .addValue("reservation_id", session.reservationId())
                .addValue("customer_id", session.customerId())
                .addValue("guest_count", session.guestCount())
                .addValue("min_consumption_snapshot_cent", session.minConsumptionSnapshotCent())
                .addValue("allowed_pass_free_guest_count_snapshot", session.allowedPassFreeGuestCountSnapshot())
                .addValue("sales_staff_id", session.salesStaffId())
                .addValue("service_staff_id", session.serviceStaffId())
                .addValue("status", session.status())
                .addValue("opened_at", timestampWithTimeZone(session.openedAt()))
                .addValue("closed_at", timestampWithTimeZone(session.closedAt()))
                .addValue("source_session_id", session.sourceSessionId())
                .addValue("session_version", session.sessionVersion())
                .addValue("created_at", timestampWithTimeZone(session.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(session.updatedAt()));
    }

    private MapSqlParameterSource tableSessionAdjustmentParameters(BarTableSessionAdjustmentRecord adjustment) {
        return new MapSqlParameterSource()
                .addValue("id", adjustment.id())
                .addValue("session_id", adjustment.sessionId())
                .addValue("adjustment_type", adjustment.adjustmentType())
                .addValue("source_session_id", adjustment.sourceSessionId())
                .addValue("target_session_id", adjustment.targetSessionId())
                .addValue("from_table_id", adjustment.fromTableId())
                .addValue("to_table_id", adjustment.toTableId())
                .addValue("bar_id", adjustment.barId())
                .addValue("merchant_id", adjustment.merchantId())
                .addValue("business_date", adjustment.businessDate())
                .addValue("min_consumption_before_cent", adjustment.minConsumptionBeforeCent())
                .addValue("min_consumption_after_cent", adjustment.minConsumptionAfterCent())
                .addValue("renewed_until", timestampWithTimeZone(adjustment.renewedUntil()))
                .addValue("additional_min_consumption_cent", adjustment.additionalMinConsumptionCent())
                .addValue("additional_deposit_amount_cent", adjustment.additionalDepositAmountCent())
                .addValue("additional_service_fee_cent", adjustment.additionalServiceFeeCent())
                .addValue("reason", adjustment.reason())
                .addValue("requested_by_user_id", adjustment.requestedByUserId())
                .addValue("approval_required", adjustment.approvalRequired())
                .addValue("status", adjustment.status())
                .addValue("idempotency_key", adjustment.idempotencyKey())
                .addValue("evidence_attachment_ids", writeJson(adjustment.evidenceAttachmentIds()))
                .addValue("snapshot_json", adjustment.snapshotJson() == null ? "{}" : adjustment.snapshotJson())
                .addValue("created_at", timestampWithTimeZone(adjustment.createdAt()));
    }

    private MapSqlParameterSource stockBalanceParameters(StockBalanceSnapshot balance) {
        return new MapSqlParameterSource()
                .addValue("id", balance.id())
                .addValue("item_id", balance.itemId())
                .addValue("location_id", balance.locationId())
                .addValue("quantity", balance.quantity())
                .addValue("reserved_quantity", balance.reservedQuantity())
                .addValue("available_quantity", balance.availableQuantity())
                .addValue("updated_at", timestampWithTimeZone(balance.updatedAt()));
    }

    private MapSqlParameterSource stockMovementParameters(StockMovementSnapshot movement) {
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
                .addValue("evidence_attachment_ids", writeJson(movement.evidenceAttachmentIds()))
                .addValue("status", movement.status())
                .addValue("created_at", timestampWithTimeZone(movement.createdAt()));
    }

    private MapSqlParameterSource paymentIntentParameters(PaymentIntentRecord paymentIntent) {
        return new MapSqlParameterSource()
                .addValue("id", paymentIntent.id())
                .addValue("business_order_type", paymentIntent.businessOrderType())
                .addValue("business_order_id", paymentIntent.businessOrderId())
                .addValue("amount_cent", paymentIntent.amountCent())
                .addValue("currency", paymentIntent.currency())
                .addValue("channel", paymentIntent.channel())
                .addValue("status", paymentIntent.status())
                .addValue("created_at", timestampWithTimeZone(paymentIntent.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(paymentIntent.updatedAt()));
    }

    private BarTableRecord mapTable(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarTableRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
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

    private BarReservationRecord mapReservation(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarReservationRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
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
                nullableInstant(resultSet, "updated_at")
        );
    }

    private BarProductRecord mapProduct(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarProductRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
                resultSet.getString("product_name"),
                resultSet.getString("product_type"),
                resultSet.getLong("price_cent"),
                resultSet.getString("currency"),
                resultSet.getString("inventory_item_id"),
                resultSet.getString("stock_location_id"),
                resultSet.getBoolean("stock_deduction_required"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private BarOrderRecord mapOrder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarOrderRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
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
                nullableInstant(resultSet, "updated_at")
        );
    }

    private BarOrderItemRecord mapOrderItem(ResultSet resultSet, int rowNumber) throws SQLException {
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
                nullableInstant(resultSet, "updated_at")
        );
    }

    private BarProductionTicketRecord mapProductionTicket(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarProductionTicketRecord(
                resultSet.getString("id"),
                resultSet.getString("order_id"),
                resultSet.getString("bar_id"),
                resultSet.getString("ticket_status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private BarProductionTicketEventRecord mapProductionTicketEvent(ResultSet resultSet, int rowNumber) throws SQLException {
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
                nullableInstant(resultSet, "created_at")
        );
    }

    private BarBillRecord mapBill(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarBillRecord(
                resultSet.getString("id"),
                resultSet.getString("bill_no"),
                resultSet.getString("order_id"),
                resultSet.getString("bar_id"),
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
                nullableInstant(resultSet, "updated_at")
        );
    }

    private BarBillAdjustmentRecord mapBillAdjustment(ResultSet resultSet, int rowNumber) throws SQLException {
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
                nullableInstant(resultSet, "created_at")
        );
    }

    private BarBillOperationRecord mapBillOperation(ResultSet resultSet, int rowNumber) throws SQLException {
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

    private BarTableSessionRecord mapTableSession(ResultSet resultSet, int rowNumber) throws SQLException {
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

    private BarTableSessionAdjustmentRecord mapTableSessionAdjustment(ResultSet resultSet, int rowNumber) throws SQLException {
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

    private StockBalanceSnapshot mapStockBalance(ResultSet resultSet, int rowNumber) throws SQLException {
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

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        OffsetDateTime value = resultSet.getObject(columnName, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("bar JSON value is not serializable", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("bar JSON payload is not readable", exception);
        }
    }
}
