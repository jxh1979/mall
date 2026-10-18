package com.complex.platform.dealer;

// 文件版本：V0.02

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import com.complex.platform.dealer.DealerDeliveryRecord.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 所有写入由订单行锁串行保护；事实只追加，批准方案只推进本身状态。 */
@Repository
public class JdbcDealerDeliveryRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public JdbcDealerDeliveryRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    public void createBatch(String id, String orderId, String resolutionId, String actor, Instant at, List<DealerOrderLineRecord> lines) {
        jdbc.update("INSERT INTO dealer_delivery_batches(id,order_id,resolution_id,shipped_by,shipped_at) VALUES(?,?,?,?,?)",
                id, orderId, resolutionId, actor, ts(at));
        for (var line : lines) jdbc.update("INSERT INTO dealer_delivery_batch_lines(batch_id,order_line_id,quantity) VALUES(?,?,?)", id, line.id(), line.quantity());
    }
    public Optional<Batch> batch(String id) {
        return jdbc.query("SELECT * FROM dealer_delivery_batches WHERE id=?", (rs,n) -> mapBatch(rs), id).stream().findFirst();
    }
    public List<Batch> batches(String orderId) {
        return jdbc.query("SELECT * FROM dealer_delivery_batches WHERE order_id=? ORDER BY shipped_at,id", (rs,n) -> mapBatch(rs), orderId);
    }
    private Batch mapBatch(ResultSet rs) throws SQLException {
        String id=rs.getString("id");
        List<BatchLine> lines=jdbc.query("""
                SELECT b.order_line_id,b.quantity,COALESCE(SUM(l.accepted_quantity+l.rejected_quantity+l.damaged_quantity+l.short_quantity),0) recorded,
                  COALESCE(SUM(l.accepted_quantity),0) accepted
                FROM dealer_delivery_batch_lines b
                LEFT JOIN dealer_delivery_receipts r ON r.batch_id=b.batch_id
                LEFT JOIN dealer_delivery_receipt_lines l ON l.receipt_id=r.id AND l.order_line_id=b.order_line_id
                WHERE b.batch_id=? GROUP BY b.order_line_id,b.quantity ORDER BY b.order_line_id
                """, (r,n) -> new BatchLine(r.getString(1),r.getInt(2),r.getInt(3),r.getInt(4)),id);
        return new Batch(id,rs.getString("order_id"),rs.getString("resolution_id"),rs.getString("shipped_by"),nullableInstant(rs,"shipped_at"),lines,
                rs.getString("resolution_id")==null?List.of():jdbc.query("SELECT evidence_attachment_ids FROM inventory_stock_movements WHERE business_type='dealer_delivery_resolution' AND business_id=? ORDER BY created_at,id",
                    (r,n)->strings(r,"evidence_attachment_ids"),rs.getString("resolution_id")).stream().flatMap(List::stream).distinct().toList());
    }
    public void saveReceipt(String id,String orderId,String batchId,Instant at,String contact,List<String> evidence,String note,String actor) {
        jdbc.update("""
                INSERT INTO dealer_delivery_receipts(id,order_id,batch_id,confirmed_at,buyer_contact,evidence_attachment_ids,note,created_by,created_at)
                VALUES(?,?,?,?,?,?::jsonb,?,?,now())
                """, id,orderId,batchId,ts(at),contact,json(evidence),note,actor);
    }
    public void saveReceiptLine(String receiptId,ReceiptLine line) {
        jdbc.update("INSERT INTO dealer_delivery_receipt_lines VALUES(?,?,?,?,?,?,?)",line.id(),receiptId,line.orderLineId(),line.acceptedQuantity(),line.rejectedQuantity(),line.damagedQuantity(),line.shortQuantity());
    }
    public void addDiscrepancy(String id,String orderId,String receiptLineId,String orderLineId,String type,int quantity) {
        jdbc.update("INSERT INTO dealer_delivery_discrepancies VALUES(?,?,?,?,?,?,now())",id,orderId,receiptLineId,orderLineId,type,quantity);
    }
    public List<Receipt> receipts(String orderId) {
        return jdbc.query("SELECT * FROM dealer_delivery_receipts WHERE order_id=? ORDER BY created_at,id", (r,n) -> new Receipt(
                r.getString("id"),r.getString("batch_id"),nullableInstant(r,"confirmed_at"),r.getString("buyer_contact"),strings(r,"evidence_attachment_ids"),
                r.getString("note"),r.getString("created_by"),jdbc.query("SELECT * FROM dealer_delivery_receipt_lines WHERE receipt_id=? ORDER BY id",
                (l,k) -> new ReceiptLine(l.getString("id"),l.getString("order_line_id"),l.getInt("accepted_quantity"),l.getInt("rejected_quantity"),l.getInt("damaged_quantity"),l.getInt("short_quantity")),r.getString("id"))),orderId);
    }
    public Optional<Discrepancy> discrepancy(String id) {
        return jdbc.query("SELECT * FROM dealer_delivery_discrepancies WHERE id=?",(r,n)->mapDiscrepancy(r),id).stream().findFirst();
    }
    public List<Discrepancy> discrepancies(String orderId) {
        return jdbc.query("SELECT * FROM dealer_delivery_discrepancies WHERE order_id=? ORDER BY created_at,id",(r,n)->mapDiscrepancy(r),orderId);
    }
    private Discrepancy mapDiscrepancy(ResultSet r) throws SQLException {
        List<Resolution> resolutions=jdbc.query("SELECT * FROM dealer_delivery_resolutions WHERE discrepancy_id=? ORDER BY requested_at,id",(s,n)->mapResolution(s),r.getString("id"));
        return new Discrepancy(r.getString("id"),r.getString("order_id"),r.getString("receipt_line_id"),r.getString("order_line_id"),r.getString("discrepancy_type"),r.getInt("quantity"),
                resolutions.stream().filter(s->!"rejected".equals(s.status())).mapToInt(Resolution::quantity).sum(),
                resolutions.stream().filter(s->"completed".equals(s.status())).mapToInt(Resolution::quantity).sum(),resolutions);
    }
    public Optional<Resolution> resolution(String id) {
        return jdbc.query("SELECT * FROM dealer_delivery_resolutions WHERE id=?",(r,n)->mapResolution(r),id).stream().findFirst();
    }
    public Optional<Resolution> resolutionByReview(String id) {
        return jdbc.query("SELECT * FROM dealer_delivery_resolutions WHERE review_id=?",(r,n)->mapResolution(r),id).stream().findFirst();
    }
    private Resolution mapResolution(ResultSet r) throws SQLException {
        return new Resolution(r.getString("id"),r.getString("discrepancy_id"),r.getInt("quantity"),r.getString("resolution_type"),r.getString("physical_route"),
                r.getLong("adjustment_cent"),r.getString("responsibility_party"),r.getString("agreement_basis"),strings(r,"evidence_attachment_ids"),r.getString("review_id"),r.getString("status"),
                r.getString("requested_by"),nullableInstant(r,"requested_at"),nullableInstant(r,"physical_completed_at"),strings(r,"physical_evidence_attachment_ids"),
                r.getString("physical_completed_by"),nullableInstant(r,"adjustment_applied_at"),nullableInstant(r,"completed_at"));
    }
    public void saveResolution(Resolution s) {
        jdbc.update("""
                INSERT INTO dealer_delivery_resolutions(id,discrepancy_id,quantity,resolution_type,physical_route,adjustment_cent,responsibility_party,
                 agreement_basis,evidence_attachment_ids,review_id,status,requested_by,requested_at,updated_at)
                VALUES(?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?,now())
                """,s.id(),s.discrepancyId(),s.quantity(),s.resolutionType(),s.physicalRoute(),s.adjustmentCent(),s.responsibilityParty(),s.agreementBasis(),json(s.evidenceAttachmentIds()),
                s.reviewId(),s.status(),s.requestedBy(),ts(s.requestedAt()));
    }
    public void decision(String id,String reviewId,String status) {
        jdbc.update("UPDATE dealer_delivery_resolutions SET status=?,updated_at=now() WHERE id=? AND review_id=? AND status='pending_approval'",status,id,reviewId);
    }
    public void physicalCompleted(String id,Instant at,List<String> evidence,String actor) {
        jdbc.update("UPDATE dealer_delivery_resolutions SET physical_completed_at=?,physical_evidence_attachment_ids=?::jsonb,physical_completed_by=?,updated_at=now() WHERE id=?",
                ts(at),json(evidence),actor,id);
    }
    public void completeResolution(String id,Instant at) {
        jdbc.update("UPDATE dealer_delivery_resolutions SET status='completed',completed_at=?,updated_at=now() WHERE id=? AND status='approved'",ts(at),id);
    }
    public void adjustmentApplied(String id) { jdbc.update("UPDATE dealer_delivery_resolutions SET adjustment_applied_at=now() WHERE id=? AND adjustment_applied_at IS NULL",id); }
    public void linkRefund(String resolutionId,String refundId) { jdbc.update("UPDATE dealer_order_refunds SET delivery_resolution_id=? WHERE id=? AND delivery_resolution_id IS NULL",resolutionId,refundId); }
    public long pendingRefundCent(String orderId) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(f.amount_cent),0) FROM dealer_order_refunds f
                WHERE f.delivery_resolution_id IS NOT NULL AND f.order_id=? AND f.status NOT IN ('refunded','voided')
                """,Long.class,orderId);
    }
    public boolean batchComplete(String batchId) {
        Batch batch=batch(batchId).orElseThrow();
        if(batch.lines().stream().anyMatch(l->l.recordedQuantity()!=l.quantity())) return false;
        return jdbc.queryForObject("""
                SELECT count(*) FROM dealer_delivery_discrepancies d JOIN dealer_delivery_receipt_lines l ON l.id=d.receipt_line_id
                 JOIN dealer_delivery_receipts r ON r.id=l.receipt_id WHERE r.batch_id=? AND d.quantity <>
                 (SELECT COALESCE(SUM(s.quantity),0) FROM dealer_delivery_resolutions s WHERE s.discrepancy_id=d.id AND s.status='completed')
                """,Integer.class,batchId)==0;
    }
    public void completeOrder(DealerOrderRecord order,Instant at,Instant dueAt) {
        jdbc.update("""
                UPDATE dealer_orders SET delivery_completed_at=?,signed_at=?,balance_due_at=?,
                status=CASE WHEN paid_amount_cent>=total_amount_cent-returned_amount_cent-delivery_adjustment_cent-first_leg_adjustment_cent AND ?=0 THEN 'completed' ELSE 'signed' END,
                completed_at=CASE WHEN paid_amount_cent>=total_amount_cent-returned_amount_cent-delivery_adjustment_cent-first_leg_adjustment_cent AND ?=0 THEN ?::timestamptz ELSE NULL END,updated_at=now()
                WHERE id=? AND delivery_completed_at IS NULL AND status='shipping'
                """,ts(at),ts(at),ts(dueAt),pendingRefundCent(order.id()),pendingRefundCent(order.id()),ts(at),order.id());
    }
    public void applyAdjustment(String orderId,long amount) {
        jdbc.update("""
                UPDATE dealer_orders SET delivery_adjustment_cent=delivery_adjustment_cent+?,
                 overdue=CASE WHEN paid_amount_cent>=total_amount_cent-returned_amount_cent-delivery_adjustment_cent-first_leg_adjustment_cent-? THEN FALSE ELSE overdue END,
                 updated_at=now() WHERE id=?
                """,amount,amount,orderId);
        jdbc.update("""
                UPDATE ar_ap_items SET amount_cent=GREATEST(0,amount_cent-?),
                 paid_amount_cent=LEAST(paid_amount_cent,GREATEST(0,amount_cent-?)),unpaid_amount_cent=GREATEST(0,unpaid_amount_cent-?),
                 status=CASE WHEN unpaid_amount_cent<=? THEN 'settled' WHEN (SELECT overdue FROM dealer_orders WHERE id=?) THEN 'overdue' ELSE 'partial_paid' END,updated_at=now()
                WHERE id=(SELECT ar_ap_item_id FROM dealer_orders WHERE id=?)
                """,amount,amount,amount,amount,orderId,orderId);
    }
    public void finishMoneyIfDeliveryCompleted(String orderId) {
        if(pendingRefundCent(orderId)==0) jdbc.update("""
                UPDATE dealer_orders SET status='completed',completed_at=now(),updated_at=now()
                WHERE id=? AND status='signed' AND delivery_completed_at IS NOT NULL
                  AND paid_amount_cent>=total_amount_cent-returned_amount_cent-delivery_adjustment_cent-first_leg_adjustment_cent
                """,orderId);
    }
    public boolean allAcceptedGoodsReturned(String orderId,List<DealerOrderLineRecord> lines) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_orders WHERE id=? AND delivery_completed_at IS NOT NULL",Integer.class,orderId)==0) return false;
        return !lines.isEmpty() && lines.stream().allMatch(line->line.returnedQuantity()>=acceptedQuantity(orderId,line.id()));
    }
    public int acceptedQuantity(String orderId,String lineId) {
        return jdbc.queryForObject("""
                SELECT (SELECT COALESCE(SUM(l.accepted_quantity),0) FROM dealer_delivery_receipt_lines l JOIN dealer_delivery_receipts r ON r.id=l.receipt_id
                WHERE r.order_id=? AND l.order_line_id=?) +
                (SELECT COALESCE(SUM(s.quantity),0) FROM dealer_delivery_resolutions s JOIN dealer_delivery_discrepancies d ON d.id=s.discrepancy_id
                WHERE d.order_id=? AND d.order_line_id=? AND s.status='completed' AND s.resolution_type='price_adjustment' AND s.physical_route='accepted_as_is')
                """,Integer.class,orderId,lineId,orderId,lineId);
    }
    public String json(Object value) { try { return mapper.writeValueAsString(value); } catch(Exception e) { throw new IllegalStateException(e); } }
    private List<String> strings(ResultSet r,String key) throws SQLException { try { return mapper.readValue(r.getString(key),new TypeReference<List<String>>(){}); } catch(Exception e) { throw new SQLException(e); } }
    private static Timestamp ts(Instant i) { return i==null?null:Timestamp.from(i); }
}
