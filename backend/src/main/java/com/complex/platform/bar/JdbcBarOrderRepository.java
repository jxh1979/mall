package com.complex.platform.bar;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

final class JdbcBarOrderRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBarRowMapper rowMapper;
    private final JdbcBarParameterSource parameterSource;

    JdbcBarOrderRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcBarRowMapper rowMapper,
            JdbcBarParameterSource parameterSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        this.parameterSource = parameterSource;
    }

    public BarOrderRecord saveOrder(BarOrderRecord order) {
        jdbcTemplate.update("""
                INSERT INTO bar_orders (
                    id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                    status, total_amount_cent, currency, created_terminal, device_id,
                    sales_staff_id, created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :table_id, :reservation_id, :customer_id, :order_source,
                    :status, :total_amount_cent, :currency, :created_terminal, :device_id,
                    :sales_staff_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    merchant_id = EXCLUDED.merchant_id,
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
                """, parameterSource.orderParameters(order));
        return order;
    }

    public List<BarOrderRecord> listOrders(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                           status, total_amount_cent, currency, created_terminal, device_id,
                           sales_staff_id, created_at, updated_at
                      FROM bar_orders
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapOrder);
        }
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                       status, total_amount_cent, currency, created_terminal, device_id,
                       sales_staff_id, created_at, updated_at
                  FROM bar_orders
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapOrder);
    }

    public Optional<BarOrderRecord> findOrderById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                       status, total_amount_cent, currency, created_terminal, device_id,
                       sales_staff_id, created_at, updated_at
                  FROM bar_orders
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapOrder).stream().findFirst();
    }

    public BarOrderItemRecord saveOrderItem(BarOrderItemRecord item) {
        jdbcTemplate.update("""
                INSERT INTO bar_order_items (
                    id, order_id, product_id, product_name, quantity, unit_price_cent,
                    amount_cent, delivery_status, created_at, updated_at,
                    actual_pour_volume, loss_volume
                )
                VALUES (
                    :id, :order_id, :product_id, :product_name, :quantity, :unit_price_cent,
                    :amount_cent, :delivery_status, :created_at, :updated_at,
                    :actual_pour_volume, :loss_volume
                )
                ON CONFLICT (id) DO UPDATE SET
                    product_name = EXCLUDED.product_name,
                    quantity = EXCLUDED.quantity,
                    unit_price_cent = EXCLUDED.unit_price_cent,
                    amount_cent = EXCLUDED.amount_cent,
                    delivery_status = EXCLUDED.delivery_status,
                    updated_at = EXCLUDED.updated_at,
                    actual_pour_volume = EXCLUDED.actual_pour_volume,
                    loss_volume = EXCLUDED.loss_volume
                """, parameterSource.orderItemParameters(item));
        return item;
    }

    public List<BarOrderItemRecord> listOrderItems(String orderId) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_order_items
                 WHERE order_id = :order_id
                 ORDER BY created_at, id
                """, Map.of("order_id", orderId), rowMapper::mapOrderItem);
    }

    public Optional<BarOrderItemRecord> findOrderItemById(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_order_items
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapOrderItem).stream().findFirst();
    }

    public BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket) {
        jdbcTemplate.update("""
                INSERT INTO bar_production_tickets (
                    id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                )
                VALUES (
                    :id, :order_id, :bar_id, :merchant_id, :ticket_status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    ticket_status = EXCLUDED.ticket_status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.productionTicketParameters(ticket));
        return ticket;
    }

    public List<BarProductionTicketRecord> listProductionTickets(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                      FROM bar_production_tickets
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapProductionTicket);
        }
        return jdbcTemplate.query("""
                SELECT id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                  FROM bar_production_tickets
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapProductionTicket);
    }

    public Optional<BarProductionTicketRecord> findProductionTicketById(String id) {
        return jdbcTemplate.query("""
                SELECT id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                  FROM bar_production_tickets
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapProductionTicket).stream().findFirst();
    }

    public Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                  FROM bar_production_tickets
                 WHERE order_id = :order_id
                 ORDER BY created_at DESC, id
                 LIMIT 1
                """, Map.of("order_id", orderId), rowMapper::mapProductionTicket).stream().findFirst();
    }

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
                """, parameterSource.productionTicketEventParameters(event));
        return event;
    }

    public List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
        return jdbcTemplate.query("""
                SELECT id, ticket_id, order_id, bar_id, action_type, from_status, to_status,
                       station_code, operator_user_id, operator_note, idempotency_key,
                       evidence_attachment_ids, created_at
                  FROM bar_production_ticket_events
                 WHERE ticket_id = :ticket_id
                 ORDER BY created_at DESC, id
                """, Map.of("ticket_id", ticketId), rowMapper::mapProductionTicketEvent);
    }
}
