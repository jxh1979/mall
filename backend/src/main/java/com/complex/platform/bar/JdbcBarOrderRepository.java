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
                    sales_staff_id, created_at, updated_at, order_type, paid_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :table_id, :reservation_id, :customer_id, :order_source,
                    :status, :total_amount_cent, :currency, :created_terminal, :device_id,
                    :sales_staff_id, :created_at, :updated_at, :order_type, :paid_at
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
                    updated_at = EXCLUDED.updated_at,
                    order_type = EXCLUDED.order_type,
                    -- V231：结账回填的订单 paid_at 随 upsert 落库；stampOrderPaidAt 只在为空时首次写。
                    paid_at = EXCLUDED.paid_at
                """, parameterSource.orderParameters(order));
        return order;
    }

    public List<BarOrderRecord> listOrders(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                           status, total_amount_cent, currency, created_terminal, device_id,
                           sales_staff_id, created_at, updated_at, order_type, paid_at
                      FROM bar_orders
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapOrder);
        }
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                       status, total_amount_cent, currency, created_terminal, device_id,
                       sales_staff_id, created_at, updated_at, order_type, paid_at
                  FROM bar_orders
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapOrder);
    }

    public Optional<BarOrderRecord> findOrderById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                       status, total_amount_cent, currency, created_terminal, device_id,
                       sales_staff_id, created_at, updated_at, order_type, paid_at
                  FROM bar_orders
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapOrder).stream().findFirst();
    }

    public BarOrderItemRecord saveOrderItem(BarOrderItemRecord item) {
        jdbcTemplate.update("""
                INSERT INTO bar_order_items (
                    id, order_id, product_id, product_name, quantity, unit_price_cent,
                    amount_cent, delivery_status, created_at, updated_at,
                    actual_pour_volume, loss_volume,
                    voided, void_reason, voided_at, voided_by_user_id, return_to_stock,
                    is_gift
                )
                VALUES (
                    :id, :order_id, :product_id, :product_name, :quantity, :unit_price_cent,
                    :amount_cent, :delivery_status, :created_at, :updated_at,
                    :actual_pour_volume, :loss_volume,
                    :voided, :void_reason, :voided_at, :voided_by_user_id, :return_to_stock,
                    :is_gift
                )
                ON CONFLICT (id) DO UPDATE SET
                    product_name = EXCLUDED.product_name,
                    quantity = EXCLUDED.quantity,
                    unit_price_cent = EXCLUDED.unit_price_cent,
                    amount_cent = EXCLUDED.amount_cent,
                    delivery_status = EXCLUDED.delivery_status,
                    updated_at = EXCLUDED.updated_at,
                    actual_pour_volume = EXCLUDED.actual_pour_volume,
                    loss_volume = EXCLUDED.loss_volume,
                    voided = EXCLUDED.voided,
                    void_reason = EXCLUDED.void_reason,
                    voided_at = EXCLUDED.voided_at,
                    voided_by_user_id = EXCLUDED.voided_by_user_id,
                    return_to_stock = EXCLUDED.return_to_stock,
                    is_gift = EXCLUDED.is_gift
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

    public BarOrderItemVoidRecord saveOrderItemVoid(BarOrderItemVoidRecord voidRecord) {
        jdbcTemplate.update("""
                INSERT INTO bar_order_item_voids (
                    id, order_item_id, order_id, bar_id, merchant_id, void_type, quantity,
                    amount_cent, was_delivered, return_to_stock, inventory_movement_id,
                    bill_id, bill_adjustment_id, reason, requested_by_user_id, approval_required,
                    status, idempotency_key, evidence_attachment_ids, created_at,
                    reviewed_by_user_id, reviewed_at, review_reason
                )
                VALUES (
                    :id, :order_item_id, :order_id, :bar_id, :merchant_id, :void_type, :quantity,
                    :amount_cent, :was_delivered, :return_to_stock, :inventory_movement_id,
                    :bill_id, :bill_adjustment_id, :reason, :requested_by_user_id, :approval_required,
                    :status, :idempotency_key, CAST(:evidence_attachment_ids AS jsonb), :created_at,
                    :reviewed_by_user_id, :reviewed_at, :review_reason
                )
                ON CONFLICT (order_item_id, idempotency_key) DO UPDATE SET
                    void_type = EXCLUDED.void_type,
                    quantity = EXCLUDED.quantity,
                    amount_cent = EXCLUDED.amount_cent,
                    was_delivered = EXCLUDED.was_delivered,
                    return_to_stock = EXCLUDED.return_to_stock,
                    inventory_movement_id = EXCLUDED.inventory_movement_id,
                    bill_id = EXCLUDED.bill_id,
                    bill_adjustment_id = EXCLUDED.bill_adjustment_id,
                    reason = EXCLUDED.reason,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    reviewed_by_user_id = EXCLUDED.reviewed_by_user_id,
                    reviewed_at = EXCLUDED.reviewed_at,
                    review_reason = EXCLUDED.review_reason
                """, parameterSource.orderItemVoidParameters(voidRecord));
        return voidRecord;
    }

    public Optional<BarOrderItemVoidRecord> findOrderItemVoidById(String id) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_order_item_voids WHERE id = :id
                """, Map.of("id", id), rowMapper::mapOrderItemVoid).stream().findFirst();
    }

    public Optional<BarOrderItemVoidRecord> findOrderItemVoidByIdForUpdate(String id) {
        // V125 P1-1：行锁读，使审批决策的 status==pending 守卫与翻转在同一事务串行化，
        // 并发第二次 reject 阻塞至首事务提交后读到 rejected/approved 被 BAR_ORDER_ITEM_VOID_NOT_PENDING 拦截。
        return jdbcTemplate.query("""
                SELECT * FROM bar_order_item_voids WHERE id = :id FOR UPDATE
                """, Map.of("id", id), rowMapper::mapOrderItemVoid).stream().findFirst();
    }

    public List<BarOrderItemVoidRecord> listOrderItemVoids(String orderId) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_order_item_voids WHERE order_id = :order_id ORDER BY created_at, id
                """, Map.of("order_id", orderId), rowMapper::mapOrderItemVoid);
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
                    evidence_attachment_ids, created_at, inventory_movement_id
                )
                VALUES (
                    :id, :ticket_id, :order_id, :bar_id, :action_type, :from_status, :to_status,
                    :station_code, :operator_user_id, :operator_note, :idempotency_key,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :inventory_movement_id
                )
                ON CONFLICT (ticket_id, idempotency_key) DO UPDATE SET
                    action_type = EXCLUDED.action_type,
                    from_status = EXCLUDED.from_status,
                    to_status = EXCLUDED.to_status,
                    station_code = EXCLUDED.station_code,
                    operator_user_id = EXCLUDED.operator_user_id,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    inventory_movement_id = EXCLUDED.inventory_movement_id
                """, parameterSource.productionTicketEventParameters(event));
        return event;
    }

    public List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
        return jdbcTemplate.query("""
                SELECT id, ticket_id, order_id, bar_id, action_type, from_status, to_status,
                       station_code, operator_user_id, operator_note, idempotency_key,
                       evidence_attachment_ids, created_at, inventory_movement_id
                  FROM bar_production_ticket_events
                 WHERE ticket_id = :ticket_id
                 ORDER BY created_at DESC, id
                """, Map.of("ticket_id", ticketId), rowMapper::mapProductionTicketEvent);
    }

    public boolean insertGiftRedemptionIfAbsent(BarGiftRedemptionRecord redemption) {
        // 幂等：uq_bar_gift_redemption_verification 唯一键防一次核销重复生成单；撞唯一键即 DO NOTHING（返回 0 行）。
        int affected = jdbcTemplate.update("""
                INSERT INTO bar_gift_redemptions (
                    id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                    item_ref, inventory_movement_id, status, note, created_at, updated_at
                )
                VALUES (
                    :id, :entitlement_id, :verification_id, :bar_order_id, :bar_id, :merchant_id,
                    :item_ref, :inventory_movement_id, :status, :note, :created_at, :updated_at
                )
                ON CONFLICT (verification_id) DO NOTHING
                """, parameterSource.giftRedemptionParameters(redemption));
        return affected > 0;
    }

    public BarGiftRedemptionRecord saveGiftRedemption(BarGiftRedemptionRecord redemption) {
        jdbcTemplate.update("""
                INSERT INTO bar_gift_redemptions (
                    id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                    item_ref, inventory_movement_id, status, note, created_at, updated_at
                )
                VALUES (
                    :id, :entitlement_id, :verification_id, :bar_order_id, :bar_id, :merchant_id,
                    :item_ref, :inventory_movement_id, :status, :note, :created_at, :updated_at
                )
                ON CONFLICT (verification_id) DO UPDATE SET
                    bar_order_id = EXCLUDED.bar_order_id,
                    item_ref = EXCLUDED.item_ref,
                    inventory_movement_id = EXCLUDED.inventory_movement_id,
                    status = EXCLUDED.status,
                    note = EXCLUDED.note,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.giftRedemptionParameters(redemption));
        return redemption;
    }

    public Optional<BarGiftRedemptionRecord> findGiftRedemptionByVerification(String verificationId) {
        return jdbcTemplate.query("""
                SELECT id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                       item_ref, inventory_movement_id, status, note, created_at, updated_at
                  FROM bar_gift_redemptions
                 WHERE verification_id = :verification_id
                """, Map.of("verification_id", verificationId), rowMapper::mapGiftRedemption).stream().findFirst();
    }

    public List<BarGiftRedemptionRecord> listGiftRedemptions(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                           item_ref, inventory_movement_id, status, note, created_at, updated_at
                      FROM bar_gift_redemptions
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapGiftRedemption);
        }
        return jdbcTemplate.query("""
                SELECT id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                       item_ref, inventory_movement_id, status, note, created_at, updated_at
                  FROM bar_gift_redemptions
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapGiftRedemption);
    }
}
