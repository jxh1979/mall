package com.complex.platform.dealer;

// 文件版本：V0.02

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** CG-11c 经销退货单、退货明细、电子发票、退货库存去向与应收冲减的仓储（V453）。 */
@Repository
public class JdbcDealerReturnRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcDealerReturnRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── 退货单 ──

    public void saveReturn(DealerReturnRecord r) {
        jdbc.update("""
                INSERT INTO dealer_returns (id, return_no, order_id, dealer_id, sales_entity_id, status, reason_type, requested_quantity, accepted_quantity,
                    accepted_amount_cent, offset_receivable_cent, refund_cent, refunded_cent, refund_review_id, refund_approved, return_freight_bearer,
                    first_leg_freight_refundable, freight_confirmed, approved_by, approved_at, received_by, received_at, inspected_by, inspected_at,
                    refund_executed_by, refund_executed_at, refund_reference_no, failure_reason, note, created_by, created_at, updated_at)
                VALUES (:id, :no, :order, :dealer, :entity, :status, :reason, :req_qty, :acc_qty,
                    :acc_amt, :offset, :refund, :refunded, :review, :approved, :bearer,
                    :first_leg, :freight_confirmed, :approved_by, :approved_at, :received_by, :received_at, :inspected_by, :inspected_at,
                    :exec_by, :exec_at, :ref_no, :failure, :note, :created_by, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, accepted_quantity = EXCLUDED.accepted_quantity, accepted_amount_cent = EXCLUDED.accepted_amount_cent,
                    offset_receivable_cent = EXCLUDED.offset_receivable_cent, refund_cent = EXCLUDED.refund_cent, refunded_cent = EXCLUDED.refunded_cent,
                    refund_review_id = EXCLUDED.refund_review_id, refund_approved = EXCLUDED.refund_approved, return_freight_bearer = EXCLUDED.return_freight_bearer,
                    first_leg_freight_refundable = EXCLUDED.first_leg_freight_refundable, freight_confirmed = EXCLUDED.freight_confirmed,
                    approved_by = EXCLUDED.approved_by, approved_at = EXCLUDED.approved_at, received_by = EXCLUDED.received_by, received_at = EXCLUDED.received_at,
                    inspected_by = EXCLUDED.inspected_by, inspected_at = EXCLUDED.inspected_at, refund_executed_by = EXCLUDED.refund_executed_by,
                    refund_executed_at = EXCLUDED.refund_executed_at, refund_reference_no = EXCLUDED.refund_reference_no, failure_reason = EXCLUDED.failure_reason,
                    note = EXCLUDED.note, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("id", r.id()).addValue("no", r.returnNo()).addValue("order", r.orderId()).addValue("dealer", r.dealerId())
                .addValue("entity", r.salesEntityId()).addValue("status", r.status()).addValue("reason", r.reasonType()).addValue("req_qty", r.requestedQuantity())
                .addValue("acc_qty", r.acceptedQuantity()).addValue("acc_amt", r.acceptedAmountCent()).addValue("offset", r.offsetReceivableCent())
                .addValue("refund", r.refundCent()).addValue("refunded", r.refundedCent()).addValue("review", r.refundReviewId()).addValue("approved", r.refundApproved())
                .addValue("bearer", r.returnFreightBearer()).addValue("first_leg", r.firstLegFreightRefundable()).addValue("freight_confirmed", r.freightConfirmed())
                .addValue("approved_by", r.approvedBy()).addValue("approved_at", ts(r.approvedAt())).addValue("received_by", r.receivedBy())
                .addValue("received_at", ts(r.receivedAt())).addValue("inspected_by", r.inspectedBy()).addValue("inspected_at", ts(r.inspectedAt()))
                .addValue("exec_by", r.refundExecutedBy()).addValue("exec_at", ts(r.refundExecutedAt())).addValue("ref_no", r.refundReferenceNo())
                .addValue("failure", r.failureReason()).addValue("note", r.note()).addValue("created_by", r.createdBy())
                .addValue("created_at", ts(r.createdAt())).addValue("updated_at", ts(r.updatedAt())));
    }

    public Optional<DealerReturnRecord> findReturn(String id) {
        return jdbc.query("SELECT * FROM dealer_returns WHERE id = :id", Map.of("id", id), this::mapReturn).stream().findFirst();
    }

    public Optional<DealerReturnRecord> findReturnByReview(String reviewId) {
        return jdbc.query("SELECT * FROM dealer_returns WHERE refund_review_id = :r", Map.of("r", reviewId), this::mapReturn).stream().findFirst();
    }

    public List<DealerReturnRecord> listReturns(String orderId) {
        return jdbc.query("SELECT * FROM dealer_returns WHERE order_id = :o ORDER BY created_at DESC", Map.of("o", orderId), this::mapReturn);
    }

    public boolean hasOpenReturn(String orderId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM dealer_returns WHERE order_id = :o AND status IN ('pending_approval', 'pending_receipt', 'inspecting', 'pending_reversal', 'pending_refund', 'refunding', 'refund_failed')",
                Map.of("o", orderId), Integer.class);
        return n != null && n > 0;
    }

    private DealerReturnRecord mapReturn(ResultSet rs, int i) throws SQLException {
        return new DealerReturnRecord(rs.getString("id"), rs.getString("return_no"), rs.getString("order_id"), rs.getString("dealer_id"), rs.getString("sales_entity_id"),
                rs.getString("status"), rs.getString("reason_type"), rs.getInt("requested_quantity"), rs.getInt("accepted_quantity"), rs.getLong("accepted_amount_cent"),
                rs.getLong("offset_receivable_cent"), rs.getLong("refund_cent"), rs.getLong("refunded_cent"), rs.getString("refund_review_id"), rs.getBoolean("refund_approved"),
                rs.getString("return_freight_bearer"), (Boolean) rs.getObject("first_leg_freight_refundable"), rs.getBoolean("freight_confirmed"),
                rs.getString("approved_by"), nullableInstant(rs, "approved_at"), rs.getString("received_by"), nullableInstant(rs, "received_at"),
                rs.getString("inspected_by"), nullableInstant(rs, "inspected_at"), rs.getString("refund_executed_by"), nullableInstant(rs, "refund_executed_at"),
                rs.getString("refund_reference_no"), rs.getString("failure_reason"), rs.getString("note"), rs.getString("created_by"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    // ── 明细 ──

    public void saveLine(DealerReturnLineRecord l) {
        jdbc.update("""
                INSERT INTO dealer_return_lines (id, return_id, order_line_id, item_id, spec, requested_quantity, accepted_quantity, resalable_quantity,
                    quarantined_quantity, unit_price_cent, accepted_amount_cent, inspection_note, source_receipt_line_id)
                VALUES (:id, :ret, :order_line, :item, :spec, :req, :acc, :resalable, :quarantined, :price, :amount, :note, :receipt)
                ON CONFLICT (id) DO UPDATE SET accepted_quantity = EXCLUDED.accepted_quantity, resalable_quantity = EXCLUDED.resalable_quantity,
                    quarantined_quantity = EXCLUDED.quarantined_quantity, accepted_amount_cent = EXCLUDED.accepted_amount_cent, inspection_note = EXCLUDED.inspection_note
                """, new MapSqlParameterSource().addValue("id", l.id()).addValue("ret", l.returnId()).addValue("order_line", l.orderLineId()).addValue("item", l.itemId())
                .addValue("spec", l.spec()).addValue("req", l.requestedQuantity()).addValue("acc", l.acceptedQuantity()).addValue("resalable", l.resalableQuantity())
                .addValue("quarantined", l.quarantinedQuantity()).addValue("price", l.unitPriceCent()).addValue("amount", l.acceptedAmountCent()).addValue("note", l.inspectionNote()).addValue("receipt",l.sourceReceiptLineId()));
    }

    public List<DealerReturnLineRecord> listLines(String returnId) {
        return jdbc.query("SELECT * FROM dealer_return_lines WHERE return_id = :r ORDER BY id", Map.of("r", returnId), (rs, i) -> new DealerReturnLineRecord(
                rs.getString("id"), rs.getString("return_id"), rs.getString("order_line_id"), rs.getString("item_id"), rs.getString("spec"), rs.getInt("requested_quantity"),
                rs.getInt("accepted_quantity"), rs.getInt("resalable_quantity"), rs.getInt("quarantined_quantity"), rs.getLong("unit_price_cent"),
                rs.getLong("accepted_amount_cent"), rs.getString("inspection_note"),rs.getString("source_receipt_line_id")));
    }

    /** 该订单明细上尚在退货流程中的申请数量（未验收完成的退货占用）。 */
    public int pendingReturnQuantity(String orderLineId) {
        Integer n = jdbc.queryForObject("""
                SELECT COALESCE(SUM(l.requested_quantity), 0) FROM dealer_return_lines l JOIN dealer_returns r ON r.id = l.return_id
                 WHERE l.order_line_id = :ol AND r.status IN ('pending_approval', 'pending_receipt', 'inspecting')
                """, Map.of("ol", orderLineId), Integer.class);
        return n == null ? 0 : n;
    }

    public record ReturnSource(String receiptLineId,String orderLineId,String batchId,int availableQuantity) {}
    public List<ReturnSource> returnSources(String orderId) {
        return jdbc.query("""
                WITH received AS (
                 SELECT rl.id,rl.order_line_id,r.batch_id,r.confirmed_at,rl.accepted_quantity+COALESCE((
                  SELECT SUM(s.quantity) FROM dealer_delivery_resolutions s JOIN dealer_delivery_discrepancies d ON d.id=s.discrepancy_id
                  WHERE d.receipt_line_id=rl.id AND s.status='completed'
                   AND s.resolution_type='price_adjustment' AND s.physical_route='accepted_as_is'),0) accepted_quantity
                 FROM dealer_delivery_receipt_lines rl JOIN dealer_delivery_receipts r ON r.id=rl.receipt_id
                 WHERE r.order_id=:order
                )
                SELECT rl.id,rl.order_line_id,rl.batch_id,rl.accepted_quantity-COALESCE((
                 SELECT SUM(CASE WHEN dr.status IN ('pending_approval','pending_receipt','inspecting') THEN l.requested_quantity ELSE l.accepted_quantity END)
                 FROM dealer_return_lines l JOIN dealer_returns dr ON dr.id=l.return_id
                 WHERE l.source_receipt_line_id=rl.id AND dr.status<>'rejected'),0) available
                FROM received rl WHERE rl.accepted_quantity>0 ORDER BY rl.confirmed_at,rl.id
                """,Map.of("order",orderId),(r,n)->new ReturnSource(r.getString(1),r.getString(2),r.getString(3),r.getInt(4)));
    }

    public boolean hasDiscountedRetainedGoods(String receiptLineId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                 SELECT 1 FROM dealer_delivery_resolutions s
                 JOIN dealer_delivery_discrepancies d ON d.id=s.discrepancy_id
                 WHERE d.receipt_line_id=:receipt AND s.status='completed'
                  AND s.resolution_type='price_adjustment' AND s.physical_route='accepted_as_is'
                  AND s.adjustment_cent>0)
                """,new MapSqlParameterSource("receipt",receiptLineId),Boolean.class));
    }

    // ── 发票 ──

    public void saveInvoice(DealerInvoiceRecord v) {
        jdbc.update("""
                INSERT INTO dealer_invoices (id, order_id, dealer_id, sales_entity_id, amount_cent, tax_rate_bps, status, invoice_no, reversed_by_return_id,
                    reissued_from_invoice_id, requested_by, requested_at, issued_by, issued_at, reversed_at, note)
                VALUES (:id, :order, :dealer, :entity, :amount, :tax, :status, :no, :reversed_by, :reissued_from, :requested_by, :requested_at, :issued_by, :issued_at, :reversed_at, :note)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, invoice_no = EXCLUDED.invoice_no, reversed_by_return_id = EXCLUDED.reversed_by_return_id,
                    issued_by = EXCLUDED.issued_by, issued_at = EXCLUDED.issued_at, reversed_at = EXCLUDED.reversed_at, note = EXCLUDED.note
                """, new MapSqlParameterSource().addValue("id", v.id()).addValue("order", v.orderId()).addValue("dealer", v.dealerId()).addValue("entity", v.salesEntityId())
                .addValue("amount", v.amountCent()).addValue("tax", v.taxRateBps()).addValue("status", v.status()).addValue("no", v.invoiceNo())
                .addValue("reversed_by", v.reversedByReturnId()).addValue("reissued_from", v.reissuedFromInvoiceId()).addValue("requested_by", v.requestedBy())
                .addValue("requested_at", ts(v.requestedAt())).addValue("issued_by", v.issuedBy()).addValue("issued_at", ts(v.issuedAt()))
                .addValue("reversed_at", ts(v.reversedAt())).addValue("note", v.note()));
    }

    public Optional<DealerInvoiceRecord> findInvoice(String id) {
        return jdbc.query("SELECT * FROM dealer_invoices WHERE id = :id", Map.of("id", id), this::mapInvoice).stream().findFirst();
    }

    public List<DealerInvoiceRecord> listInvoices(String orderId) {
        return jdbc.query("SELECT * FROM dealer_invoices WHERE order_id = :o ORDER BY requested_at", Map.of("o", orderId), this::mapInvoice);
    }

    /** 当前有效（未红冲 / 未作废）的发票。 */
    public Optional<DealerInvoiceRecord> findActiveInvoice(String orderId) {
        return jdbc.query("SELECT * FROM dealer_invoices WHERE order_id = :o AND status IN ('pending', 'issuing', 'issued') ORDER BY requested_at DESC",
                Map.of("o", orderId), this::mapInvoice).stream().findFirst();
    }

    private DealerInvoiceRecord mapInvoice(ResultSet rs, int i) throws SQLException {
        return new DealerInvoiceRecord(rs.getString("id"), rs.getString("order_id"), rs.getString("dealer_id"), rs.getString("sales_entity_id"), rs.getLong("amount_cent"),
                JdbcDealerRepository.intOrNull(rs, "tax_rate_bps"), rs.getString("status"), rs.getString("invoice_no"), rs.getString("reversed_by_return_id"),
                rs.getString("reissued_from_invoice_id"), rs.getString("requested_by"), nullableInstant(rs, "requested_at"), rs.getString("issued_by"),
                nullableInstant(rs, "issued_at"), nullableInstant(rs, "reversed_at"), rs.getString("note"),
                (Long)rs.getObject("retained_goods_cent"),(Long)rs.getObject("retained_freight_cent"),rs.getBoolean("requires_reversal"),
                rs.getString("reversal_status"),rs.getString("reversal_reference_no"),strings(rs.getString("issue_evidence_attachment_ids")),strings(rs.getString("reversal_evidence_attachment_ids")));
    }

    // ── 库存去向（BSM:1240「可复售品验收后回可用库存，确认有质量问题的隔离禁售」） ──

    public void restock(String itemId, String warehouseId, int qty, String returnId, String operatorId) {
        int updated = jdbc.update("""
                UPDATE inventory_stock_balances SET quantity = quantity + :qty, available_quantity = available_quantity + :qty, updated_at = now()
                 WHERE id = (SELECT id FROM inventory_stock_balances WHERE item_id = :item AND warehouse_id = :wh ORDER BY updated_at DESC LIMIT 1)
                """, new MapSqlParameterSource().addValue("qty", qty).addValue("item", itemId).addValue("wh", warehouseId));
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, warehouse_id, project_id)
                    VALUES (:id, :item, :wh, :qty, 0, :qty, now(), :wh, (SELECT project_id FROM warehouses WHERE id = :wh))
                    """, new MapSqlParameterSource().addValue("id", "bal_" + UUID.randomUUID()).addValue("item", itemId).addValue("wh", warehouseId).addValue("qty", qty));
        }
        movement("dealer_return_restock", itemId, null, warehouseId, qty, returnId, operatorId);
    }

    /** 质量问题隔离禁售：不回可用库存，只落流水留痕（目标位置为中心仓隔离区）。 */
    public void quarantine(String itemId, String warehouseId, int qty, String returnId, String operatorId) {
        movement("dealer_return_quarantine", itemId, null, warehouseId + ":quarantine", qty, returnId, operatorId);
    }

    private void movement(String type, String itemId, String source, String target, int qty, String returnId, String operatorId) {
        jdbc.update("""
                INSERT INTO inventory_stock_movements (id, movement_type, item_id, source_location_id, target_location_id, quantity, business_type, business_id, operator_id, status, created_at)
                VALUES (:id, :type, :item, :src, :tgt, :qty, 'dealer_return', :ret, :op, 'completed', now())
                """, new MapSqlParameterSource().addValue("id", "mov_" + UUID.randomUUID()).addValue("type", type).addValue("item", itemId).addValue("src", source)
                .addValue("tgt", target).addValue("qty", qty).addValue("ret", returnId).addValue("op", operatorId));
    }

    // ── 应收冲减（BSM:1240「退货金额先冲减原订单未收货款……不跨订单抵扣，调整留痕，剩余应收仍按原约定到期」） ──

    public void offsetReceivable(String arApItemId, long offsetCent) {
        jdbc.update("""
                UPDATE ar_ap_items SET amount_cent=amount_cent-:offset,
                       paid_amount_cent=LEAST(paid_amount_cent,amount_cent-:offset),
                       unpaid_amount_cent=GREATEST(0,unpaid_amount_cent-:offset),
                       status=CASE WHEN unpaid_amount_cent<=:offset THEN 'settled' ELSE status END,updated_at=now()
                 WHERE id=:id AND amount_cent>=:offset
                """, new MapSqlParameterSource().addValue("offset", offsetCent).addValue("id", arApItemId));
    }

    private static List<String> strings(String value) {
        try {return new com.fasterxml.jackson.databind.ObjectMapper().readValue(value,new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});}
        catch(Exception error){throw new IllegalStateException(error);}
    }

    private static Timestamp ts(Instant at) {
        return at == null ? null : Timestamp.from(at);
    }
}
