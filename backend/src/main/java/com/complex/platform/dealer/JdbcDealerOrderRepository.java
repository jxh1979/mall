package com.complex.platform.dealer;

// 文件版本：V0.02

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** CG-11b 经销订单、明细、到账、退款、超收与中心仓预留 / 出库的仓储（V452）。 */
@Repository
public class JdbcDealerOrderRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public JdbcDealerOrderRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    // ── 订单 ──

    public void saveOrder(DealerOrderRecord o) {
        jdbc.update("""
                INSERT INTO dealer_orders (id, order_no, dealer_id, dealer_contract_id, sales_entity_id, central_warehouse_id, status, status_before_cancel,
                    price_catalog_id, credit_term, goods_amount_cent, freight_cent, freight_bearer, freight_template_version, total_amount_cent,
                    first_payment_required_cent, first_payment_deadline_at, balance_due_at, paid_amount_cent, overdue, ar_ap_item_id, reviewed_by, reviewed_at,
                    payment_notified_at, shipped_by, shipped_at, signed_at, completed_at, cancel_requested_by, cancel_reason, cancelled_at, note, created_by,
                    created_at, updated_at)
                VALUES (:id, :order_no, :dealer, :contract, :entity, :wh, :status, :status_before_cancel,
                    :catalog, :credit, :goods, :freight, :bearer, :ft_version, :total,
                    :first_required, :deadline, :balance_due, :paid, :overdue, :arap, :reviewed_by, :reviewed_at,
                    :notified_at, :shipped_by, :shipped_at, :signed_at, :completed_at, :cancel_by, :cancel_reason, :cancelled_at, :note, :created_by,
                    :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, status_before_cancel = EXCLUDED.status_before_cancel, price_catalog_id = EXCLUDED.price_catalog_id,
                    credit_term = EXCLUDED.credit_term, goods_amount_cent = EXCLUDED.goods_amount_cent, freight_cent = EXCLUDED.freight_cent,
                    freight_bearer = EXCLUDED.freight_bearer, freight_template_version = EXCLUDED.freight_template_version, total_amount_cent = EXCLUDED.total_amount_cent,
                    first_payment_required_cent = EXCLUDED.first_payment_required_cent, first_payment_deadline_at = EXCLUDED.first_payment_deadline_at,
                    balance_due_at = EXCLUDED.balance_due_at, paid_amount_cent = EXCLUDED.paid_amount_cent, overdue = EXCLUDED.overdue,
                    ar_ap_item_id = EXCLUDED.ar_ap_item_id, reviewed_by = EXCLUDED.reviewed_by, reviewed_at = EXCLUDED.reviewed_at,
                    payment_notified_at = EXCLUDED.payment_notified_at, shipped_by = EXCLUDED.shipped_by, shipped_at = EXCLUDED.shipped_at,
                    signed_at = EXCLUDED.signed_at, completed_at = EXCLUDED.completed_at, cancel_requested_by = EXCLUDED.cancel_requested_by,
                    cancel_reason = EXCLUDED.cancel_reason, cancelled_at = EXCLUDED.cancelled_at, note = EXCLUDED.note, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", o.id()).addValue("order_no", o.orderNo()).addValue("dealer", o.dealerId()).addValue("contract", o.dealerContractId())
                .addValue("entity", o.salesEntityId()).addValue("wh", o.centralWarehouseId()).addValue("status", o.status())
                .addValue("status_before_cancel", o.statusBeforeCancel()).addValue("catalog", o.priceCatalogId()).addValue("credit", o.creditTerm())
                .addValue("goods", o.goodsAmountCent()).addValue("freight", o.freightCent()).addValue("bearer", o.freightBearer())
                .addValue("ft_version", o.freightTemplateVersion()).addValue("total", o.totalAmountCent()).addValue("first_required", o.firstPaymentRequiredCent())
                .addValue("deadline", ts(o.firstPaymentDeadlineAt())).addValue("balance_due", ts(o.balanceDueAt())).addValue("paid", o.paidAmountCent())
                .addValue("overdue", o.overdue()).addValue("arap", o.arApItemId()).addValue("reviewed_by", o.reviewedBy()).addValue("reviewed_at", ts(o.reviewedAt()))
                .addValue("notified_at", ts(o.paymentNotifiedAt())).addValue("shipped_by", o.shippedBy()).addValue("shipped_at", ts(o.shippedAt()))
                .addValue("signed_at", ts(o.signedAt())).addValue("completed_at", ts(o.completedAt())).addValue("cancel_by", o.cancelRequestedBy())
                .addValue("cancel_reason", o.cancelReason()).addValue("cancelled_at", ts(o.cancelledAt())).addValue("note", o.note())
                .addValue("created_by", o.createdBy()).addValue("created_at", ts(o.createdAt())).addValue("updated_at", ts(o.updatedAt())));
    }

    public Optional<DealerOrderRecord> findOrder(String id) {
        return jdbc.query("SELECT * FROM dealer_orders WHERE id = :id", Map.of("id", id), this::mapOrder).stream().findFirst();
    }

    public Optional<DealerOrderRecord> lockOrder(String id) {
        return jdbc.query("SELECT * FROM dealer_orders WHERE id = :id FOR UPDATE", Map.of("id", id), this::mapOrder).stream().findFirst();
    }

    public List<DealerOrderRecord> listOrders(String dealerId) {
        return jdbc.query("SELECT * FROM dealer_orders WHERE dealer_id = :d ORDER BY created_at DESC", Map.of("d", dealerId), this::mapOrder);
    }

    /** E-13：首付款截止已过且仍未出库付款阶段的订单。 */
    public List<DealerOrderRecord> listPastFirstPaymentDeadline(Instant now) {
        return jdbc.query("""
                SELECT * FROM dealer_orders
                 WHERE status IN ('pending_payment', 'payment_verifying') AND first_payment_deadline_at IS NOT NULL AND first_payment_deadline_at < :now
                   AND paid_amount_cent < first_payment_required_cent
                 ORDER BY first_payment_deadline_at
                """, Map.of("now", Timestamp.from(now)), this::mapOrder);
    }

    /** BSM:1231：已出库订单尾款到期未收齐。 */
    public List<DealerOrderRecord> listBalanceOverdue(Instant now) {
        return jdbc.query("""
                SELECT * FROM dealer_orders
                 WHERE status IN ('shipping', 'signed', 'returning', 'partially_returned', 'returned') AND overdue = FALSE AND balance_due_at IS NOT NULL AND balance_due_at < :now
                   AND paid_amount_cent < total_amount_cent - returned_amount_cent
                """, Map.of("now", Timestamp.from(now)), this::mapOrder);
    }

    public boolean dealerHasOverdueOrders(String dealerId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM dealer_orders WHERE dealer_id = :d AND overdue = TRUE", Map.of("d", dealerId), Integer.class);
        return n != null && n > 0;
    }

    private DealerOrderRecord mapOrder(ResultSet rs, int i) throws SQLException {
        return new DealerOrderRecord(rs.getString("id"), rs.getString("order_no"), rs.getString("dealer_id"), rs.getString("dealer_contract_id"),
                rs.getString("sales_entity_id"), rs.getString("central_warehouse_id"), rs.getString("status"), rs.getString("status_before_cancel"),
                rs.getString("price_catalog_id"), rs.getBoolean("credit_term"), rs.getLong("goods_amount_cent"), rs.getLong("freight_cent"),
                rs.getString("freight_bearer"), rs.getInt("freight_template_version"), rs.getLong("total_amount_cent"), rs.getLong("first_payment_required_cent"),
                nullableInstant(rs, "first_payment_deadline_at"), nullableInstant(rs, "balance_due_at"), rs.getLong("paid_amount_cent"), rs.getBoolean("overdue"),
                rs.getString("ar_ap_item_id"), rs.getString("reviewed_by"), nullableInstant(rs, "reviewed_at"), nullableInstant(rs, "payment_notified_at"),
                rs.getString("shipped_by"), nullableInstant(rs, "shipped_at"), nullableInstant(rs, "signed_at"), nullableInstant(rs, "completed_at"),
                rs.getString("cancel_requested_by"), rs.getString("cancel_reason"), nullableInstant(rs, "cancelled_at"), rs.getString("note"),
                rs.getString("created_by"), nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"), rs.getLong("returned_amount_cent"), readContractSnapshot(rs.getString("contract_snapshot")));
    }

    public void lockContractSnapshot(String orderId, DealerContractRecord contract) {
        try {
            int changed = jdbc.update("UPDATE dealer_orders SET contract_snapshot = CAST(:snapshot AS jsonb) WHERE id = :id AND contract_snapshot IS NULL",
                    Map.of("snapshot", mapper.writeValueAsString(contract), "id", orderId));
            if (changed != 1) throw new IllegalStateException("订单合同快照已锁定，不可覆盖");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("订单合同快照写入失败", e);
        }
    }

    private DealerContractRecord readContractSnapshot(String json) {
        if (json == null) return null;
        try { return mapper.readValue(json, DealerContractRecord.class); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("订单合同快照读取失败", e); }
    }

    public void updateArApDueDate(String arApId, Instant deadline) {
        if (arApId == null) return;
        jdbc.update("UPDATE ar_ap_items SET due_date = :due, updated_at = now() WHERE id = :id",
                new MapSqlParameterSource().addValue("id", arApId).addValue("due", deadline == null ? null : java.sql.Date.valueOf(deadline.atZone(java.time.ZoneOffset.UTC).toLocalDate())));
    }

    /** CG-11c：退货验收确认后回写订单累计已退金额与状态（不经 saveOrder，避免覆盖）。 */
    public void applyReturn(String orderId, long acceptedAmountCent, String status) {
        jdbc.update("""
                UPDATE dealer_orders SET returned_amount_cent = returned_amount_cent + :amt, status = :status,
                    overdue = CASE WHEN paid_amount_cent >= total_amount_cent - returned_amount_cent - :amt THEN FALSE ELSE overdue END,
                    updated_at = now() WHERE id = :id
                """, new MapSqlParameterSource().addValue("amt", acceptedAmountCent).addValue("status", status).addValue("id", orderId));
    }

    public void updateOrderStatus(String orderId, String status) {
        jdbc.update("UPDATE dealer_orders SET status = :status, updated_at = now() WHERE id = :id", Map.of("status", status, "id", orderId));
    }

    public void addReturnedQuantity(String lineId, int quantity) {
        jdbc.update("UPDATE dealer_order_lines SET returned_quantity = returned_quantity + :q WHERE id = :id", Map.of("q", quantity, "id", lineId));
    }

    // ── 明细 ──

    public void saveLine(DealerOrderLineRecord l) {
        jdbc.update("""
                INSERT INTO dealer_order_lines (id, order_id, item_id, spec, quantity, unit_price_cent, tax_rate_bps, price_source, inquiry_id, line_amount_cent)
                VALUES (:id, :order, :item, :spec, :qty, :price, :tax, :source, :inquiry, :amount)
                ON CONFLICT (id) DO UPDATE SET unit_price_cent = EXCLUDED.unit_price_cent, tax_rate_bps = EXCLUDED.tax_rate_bps, price_source = EXCLUDED.price_source,
                    inquiry_id = EXCLUDED.inquiry_id, line_amount_cent = EXCLUDED.line_amount_cent
                """, new MapSqlParameterSource().addValue("id", l.id()).addValue("order", l.orderId()).addValue("item", l.itemId()).addValue("spec", l.spec())
                .addValue("qty", l.quantity()).addValue("price", l.unitPriceCent()).addValue("tax", l.taxRateBps()).addValue("source", l.priceSource())
                .addValue("inquiry", l.inquiryId()).addValue("amount", l.lineAmountCent()));
    }

    public List<DealerOrderLineRecord> listLines(String orderId) {
        return jdbc.query("SELECT * FROM dealer_order_lines WHERE order_id = :o ORDER BY id", Map.of("o", orderId), (rs, i) -> new DealerOrderLineRecord(
                rs.getString("id"), rs.getString("order_id"), rs.getString("item_id"), rs.getString("spec"), rs.getInt("quantity"), rs.getLong("unit_price_cent"),
                JdbcDealerRepository.intOrNull(rs, "tax_rate_bps"), rs.getString("price_source"), rs.getString("inquiry_id"), rs.getLong("line_amount_cent"),
                rs.getInt("returned_quantity")));
    }

    // ── 到账 ──

    public void savePayment(DealerOrderPaymentRecord p) {
        jdbc.update("""
                INSERT INTO dealer_order_payments (id, order_id, method, amount_cent, received_at, reference_no, status, applied_cent, registered_by, verified_by,
                    verified_at, note, created_at, updated_at)
                VALUES (:id, :order, :method, :amount, :received_at, :ref, :status, :applied, :registered_by, :verified_by, :verified_at, :note, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, applied_cent = EXCLUDED.applied_cent, verified_by = EXCLUDED.verified_by,
                    verified_at = EXCLUDED.verified_at, note = EXCLUDED.note, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("id", p.id()).addValue("order", p.orderId()).addValue("method", p.method()).addValue("amount", p.amountCent())
                .addValue("received_at", ts(p.receivedAt())).addValue("ref", p.referenceNo()).addValue("status", p.status()).addValue("applied", p.appliedCent())
                .addValue("registered_by", p.registeredBy()).addValue("verified_by", p.verifiedBy()).addValue("verified_at", ts(p.verifiedAt()))
                .addValue("note", p.note()).addValue("created_at", ts(p.createdAt())).addValue("updated_at", ts(p.updatedAt())));
    }

    public Optional<DealerOrderPaymentRecord> findPayment(String id) {
        return jdbc.query("SELECT * FROM dealer_order_payments WHERE id = :id", Map.of("id", id), this::mapPayment).stream().findFirst();
    }

    public List<DealerOrderPaymentRecord> listPayments(String orderId) {
        return jdbc.query("SELECT * FROM dealer_order_payments WHERE order_id = :o ORDER BY created_at", Map.of("o", orderId), this::mapPayment);
    }

    private DealerOrderPaymentRecord mapPayment(ResultSet rs, int i) throws SQLException {
        return new DealerOrderPaymentRecord(rs.getString("id"), rs.getString("order_id"), rs.getString("method"), rs.getLong("amount_cent"),
                nullableInstant(rs, "received_at"), rs.getString("reference_no"), rs.getString("status"), rs.getLong("applied_cent"), rs.getString("registered_by"),
                rs.getString("verified_by"), nullableInstant(rs, "verified_at"), rs.getString("note"), nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    // ── 退款 ──

    public void saveRefund(DealerOrderRefundRecord r) {
        jdbc.update("""
                INSERT INTO dealer_order_refunds (id, order_id, payment_id, amount_cent, status, review_id, requested_by, executed_by, executed_at, reference_no, reason,
                    created_at, updated_at)
                VALUES (:id, :order, :payment, :amount, :status, :review, :requested_by, :executed_by, :executed_at, :ref, :reason, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, review_id = EXCLUDED.review_id, requested_by = EXCLUDED.requested_by,
                    executed_by = EXCLUDED.executed_by, executed_at = EXCLUDED.executed_at, reference_no = EXCLUDED.reference_no, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("id", r.id()).addValue("order", r.orderId()).addValue("payment", r.paymentId()).addValue("amount", r.amountCent())
                .addValue("status", r.status()).addValue("review", r.reviewId()).addValue("requested_by", r.requestedBy()).addValue("executed_by", r.executedBy())
                .addValue("executed_at", ts(r.executedAt())).addValue("ref", r.referenceNo()).addValue("reason", r.reason())
                .addValue("created_at", ts(r.createdAt())).addValue("updated_at", ts(r.updatedAt())));
    }

    public Optional<DealerOrderRefundRecord> findRefund(String id) {
        return jdbc.query("SELECT * FROM dealer_order_refunds WHERE id = :id", Map.of("id", id), this::mapRefund).stream().findFirst();
    }

    public Optional<DealerOrderRefundRecord> findRefundByReview(String reviewId) {
        return jdbc.query("SELECT * FROM dealer_order_refunds WHERE review_id = :r", Map.of("r", reviewId), this::mapRefund).stream().findFirst();
    }

    public List<DealerOrderRefundRecord> listRefunds(String orderId) {
        return jdbc.query("SELECT * FROM dealer_order_refunds WHERE order_id = :o ORDER BY created_at", Map.of("o", orderId), this::mapRefund);
    }

    private DealerOrderRefundRecord mapRefund(ResultSet rs, int i) throws SQLException {
        return new DealerOrderRefundRecord(rs.getString("id"), rs.getString("order_id"), rs.getString("payment_id"), rs.getLong("amount_cent"), rs.getString("status"),
                rs.getString("review_id"), rs.getString("requested_by"), rs.getString("executed_by"), nullableInstant(rs, "executed_at"), rs.getString("reference_no"),
                rs.getString("reason"), nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    // ── 超收 ──

    public void saveOverpayment(DealerOrderOverpaymentRecord o) {
        jdbc.update("""
                INSERT INTO dealer_order_overpayments (id, order_id, dealer_id, sales_entity_id, payment_id, amount_cent, status, target_order_id, confirmed_by, confirmed_at, note, created_at)
                VALUES (:id, :order, :dealer, :entity, :payment, :amount, :status, :target, :confirmed_by, :confirmed_at, :note, :created_at)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, target_order_id = EXCLUDED.target_order_id, confirmed_by = EXCLUDED.confirmed_by,
                    confirmed_at = EXCLUDED.confirmed_at, note = EXCLUDED.note
                """, new MapSqlParameterSource().addValue("id", o.id()).addValue("order", o.orderId()).addValue("dealer", o.dealerId()).addValue("entity", o.salesEntityId())
                .addValue("payment", o.paymentId()).addValue("amount", o.amountCent()).addValue("status", o.status()).addValue("target", o.targetOrderId())
                .addValue("confirmed_by", o.confirmedBy()).addValue("confirmed_at", ts(o.confirmedAt())).addValue("note", o.note()).addValue("created_at", ts(o.createdAt())));
    }

    public Optional<DealerOrderOverpaymentRecord> findOverpayment(String id) {
        return jdbc.query("SELECT * FROM dealer_order_overpayments WHERE id = :id", Map.of("id", id), this::mapOverpayment).stream().findFirst();
    }

    public List<DealerOrderOverpaymentRecord> listOverpayments(String dealerId) {
        return jdbc.query("SELECT * FROM dealer_order_overpayments WHERE dealer_id = :d ORDER BY created_at DESC", Map.of("d", dealerId), this::mapOverpayment);
    }

    private DealerOrderOverpaymentRecord mapOverpayment(ResultSet rs, int i) throws SQLException {
        return new DealerOrderOverpaymentRecord(rs.getString("id"), rs.getString("order_id"), rs.getString("dealer_id"), rs.getString("sales_entity_id"),
                rs.getString("payment_id"), rs.getLong("amount_cent"), rs.getString("status"), rs.getString("target_order_id"), rs.getString("confirmed_by"),
                nullableInstant(rs, "confirmed_at"), rs.getString("note"), nullableInstant(rs, "created_at"));
    }

    // ── 中心仓预留 / 释放 / 出库（ENUM:177「审核通过时按中心仓、商品和规格预留」；守卫式 UPDATE 防超卖，与 V312 预占同款） ──

    public boolean reserveInWarehouse(String itemId, String warehouseId, int qty) {
        return jdbc.update("""
                UPDATE inventory_stock_balances SET reserved_quantity = reserved_quantity + :qty, available_quantity = available_quantity - :qty, updated_at = now()
                 WHERE id = (SELECT id FROM inventory_stock_balances WHERE item_id = :item AND warehouse_id = :wh AND available_quantity >= :qty
                             ORDER BY available_quantity DESC LIMIT 1)
                """, new MapSqlParameterSource().addValue("qty", qty).addValue("item", itemId).addValue("wh", warehouseId)) > 0;
    }

    public boolean releaseInWarehouse(String itemId, String warehouseId, int qty) {
        return jdbc.update("""
                UPDATE inventory_stock_balances SET reserved_quantity = reserved_quantity - :qty, available_quantity = available_quantity + :qty, updated_at = now()
                 WHERE id = (SELECT id FROM inventory_stock_balances WHERE item_id = :item AND warehouse_id = :wh AND reserved_quantity >= :qty
                             ORDER BY reserved_quantity DESC LIMIT 1)
                """, new MapSqlParameterSource().addValue("qty", qty).addValue("item", itemId).addValue("wh", warehouseId)) > 0;
    }

    /** 出库：预留转实扣（quantity 与 reserved 同减），并落库存流水。 */
    public boolean shipFromWarehouse(String itemId, String warehouseId, int qty, String orderId, String operatorId) {
        int updated = jdbc.update("""
                UPDATE inventory_stock_balances SET quantity = quantity - :qty, reserved_quantity = reserved_quantity - :qty, updated_at = now()
                 WHERE id = (SELECT id FROM inventory_stock_balances WHERE item_id = :item AND warehouse_id = :wh AND reserved_quantity >= :qty AND quantity >= :qty
                             ORDER BY reserved_quantity DESC LIMIT 1)
                """, new MapSqlParameterSource().addValue("qty", qty).addValue("item", itemId).addValue("wh", warehouseId));
        if (updated == 0) {
            return false;
        }
        jdbc.update("""
                INSERT INTO inventory_stock_movements (id, movement_type, item_id, source_location_id, target_location_id, quantity, business_type, business_id,
                    operator_id, status, created_at)
                VALUES (:id, 'dealer_outbound', :item, :wh, NULL, :qty, 'dealer_order', :order, :op, 'completed', now())
                """, new MapSqlParameterSource().addValue("id", "mov_" + java.util.UUID.randomUUID()).addValue("item", itemId).addValue("wh", warehouseId)
                .addValue("qty", qty).addValue("order", orderId).addValue("op", operatorId));
        return true;
    }

    public int availableInWarehouse(String itemId, String warehouseId) {
        Integer n = jdbc.queryForObject("SELECT COALESCE(SUM(available_quantity), 0) FROM inventory_stock_balances WHERE item_id = :item AND warehouse_id = :wh",
                Map.of("item", itemId, "wh", warehouseId), Integer.class);
        return n == null ? 0 : n;
    }

    // ── 应收台账回写（经销订单应收：BSM:1230「尾款持续列入该订单应收账款」） ──

    public void updateArApItem(String arApItemId, long paidCent, long unpaidCent, String status) {
        jdbc.update("UPDATE ar_ap_items SET paid_amount_cent = :paid, unpaid_amount_cent = :unpaid, status = :status, updated_at = now() WHERE id = :id",
                new MapSqlParameterSource().addValue("paid", paidCent).addValue("unpaid", unpaidCent).addValue("status", status).addValue("id", arApItemId));
    }

    private static Timestamp ts(Instant at) {
        return at == null ? null : Timestamp.from(at);
    }
}
