package com.complex.platform.inventory;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.*;
import com.complex.platform.governance.review.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 隔离实物进入统一余额；后续复检/报损使用同一库存流水和成本账，审批与实际动作分开。 */
@Service
public class InventoryQuarantineService {
    private final InventoryRepository inventory;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator files;
    private final SupervisorReviewService reviews;
    private final AuditEventService audit;
    public InventoryQuarantineService(InventoryRepository inventory,JdbcTemplate jdbc,ObjectMapper mapper,AuthorizationService authorization,
            IdempotencyService idempotency,EvidenceEventRepository evidence,EvidenceAttachmentValidator files,SupervisorReviewService reviews,AuditEventService audit) {
        this.inventory=inventory;this.jdbc=jdbc;this.mapper=mapper;this.authorization=authorization;this.idempotency=idempotency;
        this.evidence=evidence;this.files=files;this.reviews=reviews;this.audit=audit;
    }

    /** 仅接受原订单已批准差异，数量/仓库/商品全部取真实来源，调用方持有原订单锁。 */
    @Transactional
    public void receiveDealerDeliveryResolution(String resolutionId,Instant at,List<String> attachments) {
        var facts=jdbc.queryForMap("""
                SELECT s.quantity,s.physical_route,s.status,s.physical_completed_at,o.id order_id,o.central_warehouse_id warehouse_id,
                 w.project_id,w.merchant_id,l.item_id,s.review_id,r.batch_id,b.resolution_id source_resolution_id FROM dealer_delivery_resolutions s
                 JOIN dealer_delivery_discrepancies d ON d.id=s.discrepancy_id JOIN dealer_orders o ON o.id=d.order_id
                 JOIN warehouses w ON w.id=o.central_warehouse_id JOIN dealer_order_lines l ON l.id=d.order_line_id
                 JOIN dealer_delivery_receipt_lines rl ON rl.id=d.receipt_line_id JOIN dealer_delivery_receipts r ON r.id=rl.receipt_id
                 JOIN dealer_delivery_batches b ON b.id=r.batch_id WHERE s.id=?
                """,resolutionId);
        String project=(String)facts.get("project_id"); requireProject(project,"dealer.order.ship");
        if(!"approved".equals(facts.get("status")) || facts.get("physical_completed_at")!=null
                || !reviews.findReview((String)facts.get("review_id")).map(r->"approved".equals(r.reviewStatus())).orElse(false)) throw invalid("原差异方案尚未批准或已收货");
        String route=(String)facts.get("physical_route");
        if(!Set.of("return_resalable","return_quarantine").contains(route)) throw invalid("该差异方案不是实物回库");
        checkEvidence("dealer_delivery",(String)facts.get("order_id"),attachments);
        int quantity=((Number)facts.get("quantity")).intValue(); String item=(String)facts.get("item_id"), warehouse=(String)facts.get("warehouse_id");
        Long sourceCost=sourceReturnCost(facts,item,quantity);
        List<String> balances=jdbc.query("SELECT id FROM inventory_stock_balances WHERE item_id=? AND warehouse_id=? ORDER BY id LIMIT 1 FOR UPDATE",(r,n)->r.getString(1),item,warehouse);
        String balance=balances.isEmpty()?"bal_"+UUID.randomUUID():balances.getFirst();
        boolean quarantine="return_quarantine".equals(route);
        if(balances.isEmpty()) jdbc.update("""
                INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,updated_at,warehouse_id,project_id,quarantined_quantity,merchant_id)
                VALUES(?,?,?,0,0,0,now(),?,?,0,?)
                """,balance,item,warehouse,warehouse,project,facts.get("merchant_id"));
        jdbc.update("UPDATE inventory_stock_balances SET quantity=quantity+?,available_quantity=available_quantity+?,quarantined_quantity=quarantined_quantity+?,updated_at=now() WHERE id=?",
                quantity,quarantine?0:quantity,quarantine?quantity:0,balance);
        if(quarantine) jdbc.update("""
                INSERT INTO inventory_quarantine_lots(id,stock_balance_id,item_id,warehouse_id,project_id,source_type,source_id,quantity,remaining_quantity,evidence_attachment_ids,received_by,received_at)
                VALUES(?,?,?,?,?,'dealer_delivery_resolution',?,?,?,?::jsonb,?,?)
                ""","iql_"+resolutionId,balance,item,warehouse,project,resolutionId,quantity,quantity,json(attachments),actor(),Timestamp.from(at));
        var stock=jdbc.queryForMap("SELECT location_id,merchant_id FROM inventory_stock_balances WHERE id=?",balance);
        String location=(String)stock.get("location_id"), owner=(String)stock.get("merchant_id");
        inventory.saveSourceReturnMovement(new StockMovementRecord("movement_"+resolutionId,"return",item,null,location,quantity,null,sourceCost,
                "dealer_delivery_resolution",resolutionId,null,actor(),attachments,"completed",at,owner),project);
    }

    /** 原订单锁串行化同一来源批次的退回；累计分摊使最后一份精确收回原出库总成本。 */
    private Long sourceReturnCost(Map<String,Object> facts,String item,int quantity) {
        String batch=(String)facts.get("batch_id"), sourceResolution=(String)facts.get("source_resolution_id");
        String sourceType=sourceResolution==null?"dealer_order":"dealer_delivery_resolution";
        String sourceId=sourceResolution==null?(String)facts.get("order_id"):sourceResolution;
        var source=jdbc.queryForMap("""
                SELECT COALESCE(SUM(quantity),0) quantity,SUM(total_cost_cent) cost,COUNT(*) movement_count,COUNT(total_cost_cent) priced_count
                FROM inventory_stock_movements WHERE business_type=? AND business_id=? AND item_id=?
                 AND movement_type='dealer_outbound' AND source_location_id IS NOT NULL AND target_location_id IS NULL AND status='completed'
                """,sourceType,sourceId,item);
        long shipped=((Number)source.get("quantity")).longValue();
        Long expected=jdbc.queryForObject("""
                SELECT SUM(bl.quantity) FROM dealer_delivery_batch_lines bl JOIN dealer_order_lines l ON l.id=bl.order_line_id
                WHERE bl.batch_id=? AND l.item_id=?
                """,Long.class,batch,item);
        if(expected==null || shipped!=expected || shipped<=0) throw invalid("实际原出库流水与交付批次数量不一致，须核实后回库");
        var restored=jdbc.queryForMap("""
                SELECT COALESCE(SUM(m.quantity),0) quantity,COALESCE(SUM(m.total_cost_cent),0) cost,COUNT(*) movement_count,COUNT(m.total_cost_cent) priced_count
                FROM inventory_stock_movements m JOIN dealer_delivery_resolutions s ON s.id=m.business_id
                 JOIN dealer_delivery_discrepancies d ON d.id=s.discrepancy_id
                 JOIN dealer_delivery_receipt_lines rl ON rl.id=d.receipt_line_id JOIN dealer_delivery_receipts r ON r.id=rl.receipt_id
                WHERE m.business_type='dealer_delivery_resolution' AND m.movement_type='return' AND m.source_location_id IS NULL
                 AND m.target_location_id IS NOT NULL AND m.status='completed' AND r.batch_id=? AND m.item_id=?
                """,batch,item);
        long returned=((Number)restored.get("quantity")).longValue();
        if(returned+quantity>shipped) throw invalid("累计实物退回超过对应原出库数量");
        if(!source.get("movement_count").equals(source.get("priced_count"))) return null;
        if(!restored.get("movement_count").equals(restored.get("priced_count"))) throw invalid("既有退回成本尚未核实，不能再次分摊");
        long cumulative=com.complex.platform.common.money.MoneyRounding.prorate(((Number)source.get("cost")).longValue(),returned+quantity,shipped);
        long cost=cumulative-((Number)restored.get("cost")).longValue();
        if(cost<0) throw invalid("累计退回成本与原出库不一致，须核实后办理");
        return cost;
    }

    public record WarehouseOption(String id,String name) {}
    public List<WarehouseOption> visibleWarehouses() {
        try {
            authorization.requirePermission("inventory.stock.view");
            String merchant=merchantScope();
            var actor=authorization.currentActor();
            var options=jdbc.query("""
                    SELECT DISTINCT w.id,w.name,w.project_id FROM warehouses w
                    JOIN inventory_quarantine_lots q ON q.warehouse_id=w.id AND q.project_id=w.project_id
                    JOIN inventory_stock_balances b ON b.id=q.stock_balance_id AND b.warehouse_id=w.id AND b.item_id=q.item_id
                    WHERE (?::text IS NULL OR (w.merchant_id=? AND b.merchant_id=?)) ORDER BY w.name,w.id
                    """,(row,n)->new String[]{row.getString(1),row.getString(2),row.getString(3)},merchant,merchant,merchant).stream()
                    .filter(row->row[2]!=null && authorization.canReachProject(actor,row[2],"inventory.stock.view"))
                    .map(row->new WarehouseOption(row[0],row[1])).toList();
            audit.recordSuccess("inventory.quarantine.warehouses_viewed","inventory.stock.view","inventory_quarantine","all",null,null,Map.of("count",options.size()));
            return options;
        } catch(RuntimeException error) {
            recordFailure("inventory.quarantine.warehouses_view_failed","inventory.stock.view","inventory_quarantine","all",null,error);
            throw error;
        }
    }
    public List<InventoryQuarantineLotRecord> list(String warehouseId) {
        try {
            authorization.requirePermission("inventory.stock.view");
            requireWarehouseScope(requireText(warehouseId,"warehouse_id"),"inventory.stock.view");
            String merchant=merchantScope();
            var rows=jdbc.query("""
                    SELECT q.id FROM inventory_quarantine_lots q JOIN inventory_stock_balances b ON b.id=q.stock_balance_id
                     WHERE q.warehouse_id=? AND (?::text IS NULL OR b.merchant_id=?) ORDER BY q.received_at,q.id
                    """,(r,n)->inventory.findQuarantineLotById(r.getString(1)).orElseThrow(),warehouseId,merchant,merchant);
            audit.recordSuccess("inventory.quarantine.viewed","inventory.stock.view","warehouse",warehouseId,null,null,Map.of("count",rows.size()));
            return rows;
        } catch(RuntimeException error) {
            recordFailure("inventory.quarantine.view_failed","inventory.stock.view","warehouse",warehouseId,null,error);
            throw error;
        }
    }
    public List<InventoryQuarantineDispositionRecord> dispositions(String lotId) {
        try {
            scopedLot(lotId,"inventory.stock.view");
            var rows=jdbc.query("SELECT * FROM inventory_quarantine_dispositions WHERE lot_id=? ORDER BY requested_at,id",(r,n)->map(r),lotId);
            audit.recordSuccess("inventory.quarantine.dispositions_viewed","inventory.stock.view","inventory_quarantine",lotId,null,null,Map.of("count",rows.size()));
            return rows;
        } catch(RuntimeException error) {
            recordFailure("inventory.quarantine.dispositions_view_failed","inventory.stock.view","inventory_quarantine",lotId,null,error);
            throw error;
        }
    }
    @Transactional
    public InventoryQuarantineDispositionRecord propose(String lotId,InventoryQuarantineDispositionRequest request) {
        try {
            scopedLot(lotId,"inventory.stock.adjust");
            if(request==null) throw invalid("请填写隔离处置方案");
            return idempotency.execute("inventoryQuarantinePropose:"+lotId,request.idempotencyKey(),request,()->{
                lockLot(lotId); var lot=inventory.findQuarantineLotById(lotId).orElseThrow();
                Integer occupied=jdbc.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM inventory_quarantine_dispositions WHERE lot_id=? AND status IN ('pending_approval','approved')",Integer.class,lotId);
                if(request.quantity()<=0 || request.quantity()>lot.remainingQuantity()-occupied) throw invalid("处置数量超过未占用隔离余额");
                if(!Set.of("release_resalable","scrap").contains(requireText(request.dispositionType(),"disposition_type"))) throw invalid("请选择复检可售释放或实际报损处置");
                requireText(request.reason(),"reason");checkEvidence("inventory_quarantine",lotId,request.evidenceAttachmentIds());
                if("scrap".equals(request.dispositionType())) requireScrapEvidence(request.evidenceAttachmentIds());
                if(request.parallelLines()==null || request.parallelLines().stream().noneMatch(l->"finance_review".equals(l.lineCode()))) throw invalid("隔离处置须指定财务复核平行线");
                String id="iqd_"+UUID.randomUUID();
                var decision=reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey()+":review",request.reason(),request.evidenceAttachmentIds(),
                        "inventory.quarantine_disposition","inventory_quarantine_disposition",id,null,"隔离实物处置数量"+request.quantity(),request.finalApproverId(),request.parallelLines(),null));
                jdbc.update("""
                        INSERT INTO inventory_quarantine_dispositions(id,lot_id,quantity,disposition_type,reason,evidence_attachment_ids,review_id,status,requested_by,requested_at)
                        VALUES(?,?,?,?,?,?::jsonb,?,?,?,now())
                        """,id,lotId,request.quantity(),request.dispositionType(),request.reason(),json(request.evidenceAttachmentIds()),decision.reviewId(),
                        "approved".equals(decision.reviewStatus())?"approved":"pending_approval",actor());
                audit.recordSuccess("inventory.quarantine.disposition_requested","inventory.stock.adjust","inventory_quarantine_disposition",id,null,request.idempotencyKey(),Map.of("lot_id",lotId));
                return disposition(id);
            },InventoryQuarantineDispositionRecord.class);
    
        } catch(RuntimeException error) {
            recordFailure("inventory.quarantine.disposition_request_failed","inventory.stock.adjust","inventory_quarantine",lotId,request==null?null:request.idempotencyKey(),error);
            throw error;
        }
    }
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if(!"inventory.quarantine_disposition".equals(decision.actionType()) || !"inventory_quarantine_disposition".equals(decision.objectType())) return;
        List<InventoryQuarantineDispositionRecord> rows=jdbc.query("SELECT * FROM inventory_quarantine_dispositions WHERE review_id=?",(r,n)->map(r),decision.reviewId());
        if(rows.isEmpty()) return;
        lockLot(rows.getFirst().lotId());
        if(Set.of("approved","rejected").contains(decision.reviewStatus())) jdbc.update("UPDATE inventory_quarantine_dispositions SET status=? WHERE id=? AND review_id=? AND status='pending_approval'",decision.reviewStatus(),rows.getFirst().id(),decision.reviewId());
    }
    @Transactional
    public InventoryQuarantineDispositionRecord complete(String id,InventoryQuarantineCompleteRequest request) {
        try {
            InventoryQuarantineDispositionRecord found=disposition(id);scopedLot(found.lotId(),"inventory.stock.adjust");
            if(request==null) throw invalid("请填写实际完成凭证");
            return idempotency.execute("inventoryQuarantineComplete:"+id,request.idempotencyKey(),request,()->{
                lockLot(found.lotId());var current=disposition(id);var lot=inventory.findQuarantineLotById(current.lotId()).orElseThrow();
                if(!"approved".equals(current.status()) || !reviews.findReview(current.reviewId()).map(r->"approved".equals(r.reviewStatus())).orElse(false)) throw invalid("处置须先批准且不能重复执行");
                if(request.completedAt()==null || request.completedAt().isBefore(current.requestedAt()) || request.completedAt().isAfter(Instant.now())) throw invalid("实际完成时间无效");
                checkEvidence("inventory_quarantine",lot.id(),request.evidenceAttachmentIds());
                boolean release="release_resalable".equals(current.dispositionType());
                if(!release) { requireScrapEvidence(current.evidenceAttachmentIds()); requireScrapEvidence(request.evidenceAttachmentIds()); }
                int changed=jdbc.update("""
                        UPDATE inventory_stock_balances SET quarantined_quantity=quarantined_quantity-?,quantity=quantity-?,available_quantity=available_quantity+?,updated_at=now()
                        WHERE id=? AND quarantined_quantity>=? AND quantity>=?
                        """,current.quantity(),release?0:current.quantity(),release?current.quantity():0,lot.stockBalanceId(),current.quantity(),current.quantity());
                if(changed!=1 || lot.remainingQuantity()<current.quantity()) throw ApiException.conflict("QUARANTINE_BALANCE_CHANGED","隔离实物余额不足，请核实实际数量");
                jdbc.update("UPDATE inventory_quarantine_lots SET remaining_quantity=remaining_quantity-? WHERE id=?",current.quantity(),lot.id());
                var stock=jdbc.queryForMap("SELECT location_id,merchant_id FROM inventory_stock_balances WHERE id=?",lot.stockBalanceId());
                String location=(String)stock.get("location_id"), owner=(String)stock.get("merchant_id");
                inventory.saveStockMovement(new StockMovementRecord("movement_"+id,release?"quarantine_release":"scrap",lot.itemId(),location,release?location:null,
                        current.quantity(),null,null,"inventory_quarantine_disposition",id,null,actor(),request.evidenceAttachmentIds(),"completed",request.completedAt(),owner),lot.projectId());
                jdbc.update("UPDATE inventory_quarantine_dispositions SET status='completed',completed_by=?,completed_at=?,completion_evidence_attachment_ids=?::jsonb WHERE id=?",actor(),Timestamp.from(request.completedAt()),json(request.evidenceAttachmentIds()),id);
                audit.recordSuccess("inventory.quarantine.disposition_completed","inventory.stock.adjust","inventory_quarantine_disposition",id,null,request.idempotencyKey(),Map.of("lot_id",lot.id(),"quantity",current.quantity()));
                return disposition(id);
            },InventoryQuarantineDispositionRecord.class);
    
        } catch(RuntimeException error) {
            recordFailure("inventory.quarantine.disposition_complete_failed","inventory.stock.adjust","inventory_quarantine_disposition",id,request==null?null:request.idempotencyKey(),error);
            throw error;
        }
    }
    private InventoryQuarantineLotRecord scopedLot(String id,String permission) {
        var lot=inventory.findQuarantineLotById(requireText(id,"lot_id")).orElseThrow(()->ApiException.notFound("QUARANTINE_LOT_NOT_FOUND","隔离实物记录不存在"));
        requireProject(lot.projectId(),permission);
        requireWarehouseScope(lot.warehouseId(),permission);
        String merchant=merchantScope();
        if(merchant!=null) {
            String owner=jdbc.queryForObject("SELECT merchant_id FROM inventory_stock_balances WHERE id=? AND warehouse_id=? AND item_id=?",String.class,lot.stockBalanceId(),lot.warehouseId(),lot.itemId());
            if(!merchant.equals(owner)) throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN","隔离实物不属于本商户货权");
        }
        return lot;
    }
    public void requireEvidenceScope(String lotId,boolean reading) {
        requireEvidenceScope(lotId,reading?"evidence.timeline.view":"evidence.attachment.create");
    }
    public void requireEvidenceScope(String lotId,String evidencePermission) {
        var lot=scopedLot(lotId,"evidence.timeline.view".equals(evidencePermission)?"inventory.stock.view":"inventory.stock.adjust");
        requireProject(lot.projectId(),evidencePermission);
    }
    private void requireWarehouseScope(String warehouseId,String permission) {
        var warehouse=jdbc.query("SELECT project_id,merchant_id FROM warehouses WHERE id=?",(r,n)->new String[]{r.getString(1),r.getString(2)},warehouseId)
                .stream().findFirst().orElseThrow(()->ApiException.notFound("WAREHOUSE_NOT_FOUND","仓库不存在"));
        requireProject(warehouse[0],permission);
        String merchant=merchantScope();
        if(merchant!=null && !merchant.equals(warehouse[1])) throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN","仓库不在本商户授权范围内");
    }
    private String merchantScope() {
        var actor=authorization.currentActor();
        return authorization.isPlatformAdmin(actor)?null:actor.merchantId();
    }
    private void lockLot(String id) { jdbc.queryForObject("SELECT id FROM inventory_quarantine_lots WHERE id=? FOR UPDATE",String.class,id); }
    private InventoryQuarantineDispositionRecord disposition(String id) {
        return jdbc.query("SELECT * FROM inventory_quarantine_dispositions WHERE id=?",(r,n)->map(r),id).stream().findFirst().orElseThrow(()->ApiException.notFound("QUARANTINE_DISPOSITION_NOT_FOUND","隔离处置方案不存在"));
    }
    private InventoryQuarantineDispositionRecord map(java.sql.ResultSet r) throws java.sql.SQLException {
        return new InventoryQuarantineDispositionRecord(r.getString("id"),r.getString("lot_id"),r.getInt("quantity"),r.getString("disposition_type"),r.getString("reason"),strings(r.getString("evidence_attachment_ids")),r.getString("review_id"),r.getString("status"),r.getString("requested_by"),nullableInstant(r,"requested_at"),r.getString("completed_by"),nullableInstant(r,"completed_at"),strings(r.getString("completion_evidence_attachment_ids")));
    }
    private void requireProject(String project,String permission) {
        authorization.requirePermission(permission);
        if(project==null || !authorization.canReachProject(authorization.currentActor(),project,permission)) throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN","隔离库存不在授权项目范围内");
    }
    private void checkEvidence(String type,String id,List<String> ids) {
        if(ids==null || ids.isEmpty()) throw invalid("须上传真实凭证");
        for(String key:ids) {var attachment=evidence.findAttachmentById(key).orElseThrow(()->invalid("凭证不存在"));
            if(!type.equals(attachment.businessType()) || !id.equals(attachment.businessId())) throw invalid("凭证未绑定本业务"); files.requireUploaded(attachment);}
    }
    private void requireScrapEvidence(List<String> ids) {
        boolean photo=false,video=false;
        for(String id:ids) {
            var file=evidence.findAttachmentById(id).orElseThrow(()->invalid("凭证不存在")); files.requireUploaded(file);
            photo|=file.contentType()!=null && file.contentType().startsWith("image/");
            video|=file.contentType()!=null && file.contentType().startsWith("video/");
        }
        if(!photo || !video) throw invalid("报损须同时具备真实照片和视频");
    }
    private String actor() { return authorization.currentActor().id(); }
    private String json(Object value) {try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
    private List<String> strings(String value) {try{return mapper.readValue(value,new TypeReference<List<String>>(){});}catch(Exception e){throw new IllegalStateException(e);}}
    private static ApiException invalid(String message) {return ApiException.badRequest("VALIDATION_FAILED",message);}
    private void recordFailure(String event,String permission,String type,String id,String key,RuntimeException error) {
        try { audit.recordFailure(event,permission,type,id,null,key,error instanceof ApiException api?api.code():error.getClass().getSimpleName()); }
        catch(RuntimeException auditError) { error.addSuppressed(auditError); }
    }
}
