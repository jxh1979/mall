package com.complex.platform.dealer;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.DealerDeliveryRecord.*;
import com.complex.platform.dealer.dto.*;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.complex.platform.governance.review.*;
import com.complex.platform.inventory.InventoryQuarantineService;
import java.time.Instant;
import java.util.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DealerDeliveryService {
    private final JdbcDealerOrderRepository orders;
    private final JdbcDealerDeliveryRepository delivery;
    private final DealerOrderService money;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator files;
    private final SupervisorReviewService reviews;
    private final InventoryQuarantineService inventory;
    private final AuditEventService audit;
    public DealerDeliveryService(JdbcDealerOrderRepository orders,JdbcDealerDeliveryRepository delivery,DealerOrderService money,
            AuthorizationService authorization,IdempotencyService idempotency,EvidenceEventRepository evidence,EvidenceAttachmentValidator files,
            SupervisorReviewService reviews,InventoryQuarantineService inventory,AuditEventService audit) {
        this.orders=orders; this.delivery=delivery; this.money=money; this.authorization=authorization; this.idempotency=idempotency;
        this.evidence=evidence; this.files=files; this.reviews=reviews; this.inventory=inventory; this.audit=audit;
    }

    public DealerDeliveryRecord get(String orderId) { return get(orderId,DealerOrderService.VIEW); }
    DealerDeliveryRecord get(String orderId,String permission) {
        try {
            DealerOrderRecord order=scoped(orderId,permission);
            long pending=delivery.pendingRefundCent(orderId);
            var result=new DealerDeliveryRecord(orderId,order.deliveryCompletedAt(),order.netReceivableCent(),order.paidAmountCent(),pending,
                    pending>0?"pending_refund":order.paidAmountCent()<order.netReceivableCent()?"unpaid":"settled",
                    delivery.batches(orderId),delivery.receipts(orderId),delivery.discrepancies(orderId));
            audit.recordSuccess("dealer.delivery.viewed",permission,"dealer_order",orderId,null,null,Map.of());
            return result;
        } catch(RuntimeException error) {
            recordFailure("dealer.delivery.view_failed",permission,"dealer_order",orderId,null,error);
            throw error;
        }
    }

    @Transactional
    public DealerOrderRecord receive(String orderId,DealerDeliveryReceiptRequest request) { return receive(orderId,request,DealerOrderService.MANAGE); }
    @Transactional
    DealerOrderRecord receive(String orderId,DealerDeliveryReceiptRequest request,String permission) {
        try {
            scoped(orderId,permission);
            if(request==null) throw invalid("请填写本次实际签收数量和凭证");
            return idempotency.execute("dealerDeliveryReceipt:"+orderId,request.idempotencyKey(),request,()->{
                DealerOrderRecord order=orders.lockOrder(orderId).orElseThrow();
                if(!"shipping".equals(order.status())) throw ApiException.conflict("DEALER_ORDER_NOT_SHIPPING","只有配送中的订单可登记本次签收");
                Batch batch=delivery.batch(requireText(request.deliveryBatchId(),"delivery_batch_id"))
                        .filter(b->orderId.equals(b.orderId())).orElseThrow(()->ApiException.notFound("DEALER_DELIVERY_BATCH_NOT_FOUND","发运批次不存在"));
                checkTime(request.confirmedAt(),batch.shippedAt());
                String contact=requireText(request.buyerContact(),"buyer_contact");
                proofs(orderId,request.evidenceAttachmentIds());
                if(request.lines()==null || request.lines().isEmpty()) throw invalid("本次签收明细不能为空");
                Set<String> seen=new HashSet<>();
                for(var line:request.lines()) {
                    if(line==null || !seen.add(line.orderLineId())) throw invalid("同一明细不得重复登记");
                    BatchLine original=batch.lines().stream().filter(l->l.orderLineId().equals(line.orderLineId())).findFirst()
                            .orElseThrow(()->invalid("明细不属于本次发运"));
                    long count=(long)line.acceptedQuantity()+line.rejectedQuantity()+line.damagedQuantity()+line.shortQuantity();
                    if(line.acceptedQuantity()<0 || line.rejectedQuantity()<0 || line.damagedQuantity()<0 || line.shortQuantity()<0
                            || count<=0 || count>original.quantity()-original.recordedQuantity()) throw invalid("本次分类数量必须非负且累计不得超过实际发运数量");
                }
                String receiptId=id("ddrc_");
                delivery.saveReceipt(receiptId,orderId,batch.id(),request.confirmedAt(),contact,request.evidenceAttachmentIds(),request.note(),actor());
                for(var line:request.lines()) {
                    String lineId=id("ddrl_");
                    delivery.saveReceiptLine(receiptId,new ReceiptLine(lineId,line.orderLineId(),line.acceptedQuantity(),line.rejectedQuantity(),line.damagedQuantity(),line.shortQuantity()));
                    if(line.rejectedQuantity()>0) delivery.addDiscrepancy(id("ddd_"),orderId,lineId,line.orderLineId(),"rejected",line.rejectedQuantity());
                    if(line.damagedQuantity()>0) delivery.addDiscrepancy(id("ddd_"),orderId,lineId,line.orderLineId(),"damaged",line.damagedQuantity());
                    if(line.shortQuantity()>0) delivery.addDiscrepancy(id("ddd_"),orderId,lineId,line.orderLineId(),"short",line.shortQuantity());
                }
                refreshCompletion(orderId,request.confirmedAt());
                audit.recordSuccess("dealer.delivery.received",permission,"dealer_order",orderId,null,request.idempotencyKey(),Map.of("receipt_id",receiptId,"delivery_batch_id",batch.id()));
                return orders.findOrder(orderId).orElseThrow();
            },DealerOrderRecord.class);
    
        } catch(RuntimeException error) {
            recordFailure("dealer.delivery.receive_failed",permission,"dealer_order",orderId,request==null?null:request.idempotencyKey(),error);
            throw error;
        }
    }

    @Transactional
    public Resolution propose(String discrepancyId,DealerDeliveryResolutionRequest request) {
        try {
            Discrepancy found=delivery.discrepancy(discrepancyId).orElseThrow(()->ApiException.notFound("DEALER_DELIVERY_DIFFERENCE_NOT_FOUND","交付差异不存在"));
            scoped(found.orderId(),DealerOrderService.MANAGE);
            if(request==null) throw invalid("请填写双方确认的差异处理方案");
            return idempotency.execute("dealerDeliveryResolution:"+discrepancyId,request.idempotencyKey(),request,()->{
                DealerOrderRecord order=orders.lockOrder(found.orderId()).orElseThrow();
                Discrepancy difference=delivery.discrepancy(discrepancyId).orElseThrow();
                if(request.quantity()<=0 || request.quantity()>difference.quantity()-difference.reservedQuantity()) throw invalid("方案数量超过尚未被其他有效方案占用的差异数量");
                if(!Set.of("replacement","price_adjustment").contains(requireText(request.resolutionType(),"resolution_type"))) throw invalid("请选择补交或确认调款");
                if(!Set.of("return_resalable","return_quarantine","loss_confirmed","accepted_as_is").contains(requireText(request.physicalRoute(),"physical_route"))) throw invalid("请选择实际实物处理路径");
                if("short".equals(difference.discrepancyType()) && "accepted_as_is".equals(request.physicalRoute())) throw invalid("短少的货物不能登记为收货方留用");
                DealerOrderLineRecord line=orderLine(order.id(),difference.orderLineId());
                if(request.adjustmentCent()<0 || request.adjustmentCent()>Math.multiplyExact(line.unitPriceCent(),request.quantity())
                        || "replacement".equals(request.resolutionType()) && request.adjustmentCent()!=0) throw invalid("补交不重复减货款，调款金额不得超过对应原成交货款");
                proofs(order.id(),request.evidenceAttachmentIds());
                requireText(request.responsibilityParty(),"responsibility_party"); requireText(request.agreementBasis(),"agreement_basis");
                if(request.parallelLines()==null || request.parallelLines().stream().noneMatch(l->"finance_review".equals(l.lineCode())))
                    throw invalid("差异处理须指定既有财务复核平行线");
                String id=id("ddrs_");
                var decision=reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey()+":review",request.agreementBasis(),request.evidenceAttachmentIds(),
                        "dealer.delivery_resolution","dealer_delivery_resolution",id,request.adjustmentCent(),"订单"+order.orderNo()+"交付差异，数量"+request.quantity(),
                        request.finalApproverId(),request.parallelLines(),null));
                Resolution saved=new Resolution(id,discrepancyId,request.quantity(),request.resolutionType(),request.physicalRoute(),request.adjustmentCent(),
                        request.responsibilityParty(),request.agreementBasis(),List.copyOf(request.evidenceAttachmentIds()),decision.reviewId(),
                        "approved".equals(decision.reviewStatus())?"approved":"pending_approval",actor(),Instant.now(),null,List.of(),null,null,null);
                delivery.saveResolution(saved);
                audit.recordSuccess("dealer.delivery.resolution_requested",DealerOrderService.MANAGE,"dealer_delivery_resolution",id,null,request.idempotencyKey(),Map.of("order_id",order.id(),"review_id",decision.reviewId()));
                return saved;
            },Resolution.class);
    
        } catch(RuntimeException error) {
            recordFailure("dealer.delivery.resolution_request_failed",DealerOrderService.MANAGE,"dealer_delivery_discrepancy",discrepancyId,request==null?null:request.idempotencyKey(),error);
            throw error;
        }
    }

    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if(!"dealer.delivery_resolution".equals(decision.actionType()) || !"dealer_delivery_resolution".equals(decision.objectType())) return;
        Resolution found=delivery.resolutionByReview(decision.reviewId()).orElse(null);
        if(found==null) return;
        Discrepancy difference=delivery.discrepancy(found.discrepancyId()).orElseThrow();
        orders.lockOrder(difference.orderId()).orElseThrow();
        if(Set.of("approved","rejected").contains(decision.reviewStatus())) delivery.decision(found.id(),decision.reviewId(),decision.reviewStatus());
    }

    @Transactional
    public Resolution completePhysical(String resolutionId,DealerDeliveryPhysicalRequest request) {
        try {
            Resolution found=resolution(resolutionId);
            Discrepancy difference=delivery.discrepancy(found.discrepancyId()).orElseThrow();
            scoped(difference.orderId(),DealerOrderService.SHIP);
            if(request==null) throw invalid("请填写实际交接或处置完成时间和凭证");
            money.lockDealerForOrder(difference.orderId());
            DealerOrderRecord order=orders.lockOrder(difference.orderId()).orElseThrow();
            return idempotency.execute("dealerDeliveryPhysical:"+resolutionId,request.idempotencyKey(),request,()->{
                Resolution current=resolution(resolutionId);
                if(current.physicalCompletedAt()!=null) throw ApiException.conflict("DEALER_DELIVERY_PHYSICAL_RECORDED","该方案实物结果已登记，不得重复入库或调款");
                requireApproved(current);
                checkTime(request.completedAt(),current.requestedAt());
                proofs(order.id(),request.evidenceAttachmentIds());
                if(Set.of("return_resalable","return_quarantine").contains(current.physicalRoute()))
                    inventory.receiveDealerDeliveryResolution(current.id(),request.completedAt(),request.evidenceAttachmentIds());
                // 损失、收货方留用是双方已批准的实际处置，绝不增加仓库余额。
                delivery.physicalCompleted(current.id(),request.completedAt(),request.evidenceAttachmentIds(),actor());
                if("price_adjustment".equals(current.resolutionType())) {
                    if(current.adjustmentCent()>0) money.applyDeliveryAdjustment(order,current.id(),current.adjustmentCent(),current.reviewId());
                    delivery.adjustmentApplied(current.id());
                    delivery.completeResolution(current.id(),request.completedAt());
                }
                refreshCompletion(order.id(),request.completedAt());
                audit.recordSuccess("dealer.delivery.physical_completed",DealerOrderService.SHIP,"dealer_delivery_resolution",current.id(),null,request.idempotencyKey(),Map.of("order_id",order.id(),"physical_route",current.physicalRoute()));
                return resolution(current.id());
            },Resolution.class);
    
        } catch(RuntimeException error) {
            recordFailure("dealer.delivery.physical_complete_failed",DealerOrderService.SHIP,"dealer_delivery_resolution",resolutionId,request==null?null:request.idempotencyKey(),error);
            throw error;
        }
    }

    @Transactional
    public Batch shipReplacement(String resolutionId,DealerDeliveryPhysicalRequest request) {
        try {
            Resolution found=resolution(resolutionId);
            Discrepancy difference=delivery.discrepancy(found.discrepancyId()).orElseThrow();
            scoped(difference.orderId(),DealerOrderService.SHIP);
            if(request==null) throw invalid("请填写实际补交出库凭证");
            return idempotency.execute("dealerDeliveryReplacement:"+resolutionId,request.idempotencyKey(),request,()->{
                DealerOrderRecord order=orders.lockOrder(difference.orderId()).orElseThrow();
                Resolution current=resolution(resolutionId); requireApproved(current);
                if(!"replacement".equals(current.resolutionType()) || current.physicalCompletedAt()==null) throw invalid("先按已批准方案实际处理原物，再办理补交");
                if(delivery.batches(order.id()).stream().anyMatch(b->current.id().equals(b.resolutionId()))) throw ApiException.conflict("DEALER_REPLACEMENT_SHIPPED","该方案已实际补交出库");
                checkTime(request.completedAt(),current.physicalCompletedAt()); proofs(order.id(),request.evidenceAttachmentIds());
                if(actor().equals(order.reviewedBy()) || orders.listPayments(order.id()).stream().anyMatch(p->actor().equals(p.verifiedBy())))
                    throw ApiException.forbidden("DEALER_SOD_SAME_PERSON","审核或到账核验人与补交出库人须分离");
                DealerOrderLineRecord line=orderLine(order.id(),difference.orderLineId());
                if(!orders.reserveInWarehouse(line.itemId(),order.centralWarehouseId(),current.quantity())) throw ApiException.conflict("DEALER_STOCK_INSUFFICIENT","补交库存不足，不得预售或使用隔离库存");
                if(!orders.shipFromWarehouse(line.itemId(),order.centralWarehouseId(),current.quantity(),"dealer_delivery_resolution",current.id(),actor(),request.completedAt(),request.evidenceAttachmentIds())) throw ApiException.conflict("DEALER_STOCK_INSUFFICIENT","补交实际出库失败");
                String batchId=id("ddb_");
                delivery.createBatch(batchId,order.id(),current.id(),actor(),request.completedAt(),List.of(new DealerOrderLineRecord(line.id(),order.id(),line.itemId(),line.spec(),current.quantity(),line.unitPriceCent(),line.taxRateBps(),line.priceSource(),line.inquiryId(),Math.multiplyExact(line.unitPriceCent(),current.quantity()))));
                audit.recordSuccess("dealer.delivery.replacement_shipped",DealerOrderService.SHIP,"dealer_delivery_resolution",current.id(),null,request.idempotencyKey(),Map.of("order_id",order.id(),"delivery_batch_id",batchId));
                return delivery.batch(batchId).orElseThrow();
            },Batch.class);
    
        } catch(RuntimeException error) {
            recordFailure("dealer.delivery.replacement_ship_failed",DealerOrderService.SHIP,"dealer_delivery_resolution",resolutionId,request==null?null:request.idempotencyKey(),error);
            throw error;
        }
    }

    private void refreshCompletion(String orderId,Instant factAt) {
        // 子补交批次先闭合，再向原差异传播；允许补交仍有真实差异而继续办理。
        List<Batch> batches=delivery.batches(orderId);
        boolean changed;
        do {
            changed=false;
            for(Batch batch:batches) if(batch.resolutionId()!=null && delivery.batchComplete(batch.id())) {
                Resolution resolution=resolution(batch.resolutionId());
                if("approved".equals(resolution.status())) { delivery.completeResolution(resolution.id(),factAt); changed=true; }
            }
        } while(changed);
        if(batches.isEmpty() || batches.stream().anyMatch(b->!delivery.batchComplete(b.id()))) return;
        DealerOrderRecord order=orders.findOrder(orderId).orElseThrow();
        if(order.deliveryCompletedAt()!=null) return;
        Instant completed=delivery.receipts(orderId).stream().map(Receipt::confirmedAt).max(Comparator.naturalOrder()).orElse(factAt);
        for(var d:delivery.discrepancies(orderId)) for(var r:d.resolutions()) if(r.completedAt()!=null && r.completedAt().isAfter(completed)) completed=r.completedAt();
        DealerContractRecord contract=order.contractSnapshot();
        if(contract==null) throw ApiException.conflict("DEALER_CONTRACT_SNAPSHOT_MISSING","锁定合同缺失，不能推定尾款到期规则");
        Instant due=order.balanceDueAt();
        if(order.creditTerm() && "signed".equals(contract.balanceDueAnchor())) due=completed.plusSeconds(Math.multiplyExact(contract.balanceDueDays().longValue(),86400L));
        delivery.completeOrder(order,completed,due);
        if(order.arApItemId()!=null) orders.updateArApDueDate(order.arApItemId(),due);
    }
    private void requireApproved(Resolution resolution) {
        if(!"approved".equals(resolution.status()) || resolution.reviewId()==null || !reviews.findReview(resolution.reviewId()).map(r->"approved".equals(r.reviewStatus())).orElse(false))
            throw ApiException.conflict("DEALER_DELIVERY_RESOLUTION_NOT_APPROVED","差异方案须完成原审批，不能先改数量或金额");
    }
    private DealerOrderRecord scoped(String orderId,String permission) {
        authorization.requirePermission(permission);
        DealerOrderRecord order=orders.findOrder(requireText(orderId,"order_id")).orElseThrow(()->ApiException.notFound("DEALER_ORDER_NOT_FOUND","订单不存在"));
        if(permission.startsWith("dealer_portal.")) {
            // 门户服务先核终端、四项准入与账号归属；此处仍拒绝内部账号借用门户权限绕项目限制。
            if(!"dealer".equals(authorization.currentActor().accountType())) throw ApiException.forbidden("DEALER_PORTAL_ACCOUNT_REQUIRED","须使用该订单经销商门户账号");
        } else {
            String project=orders.findOrderProjectId(orderId).orElseThrow(()->ApiException.conflict("DEALER_ORDER_PROJECT_MISSING","订单中心仓项目缺失或冲突"));
            if(!authorization.canReachProject(authorization.currentActor(),project,permission)) throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN","订单不在授权项目范围内");
        }
        return order;
    }
    private void proofs(String orderId,List<String> ids) {
        if(ids==null || ids.isEmpty()) throw invalid("须上传真实的双方确认或实物凭证");
        for(String id:ids) {
            var attachment=evidence.findAttachmentById(id).orElseThrow(()->invalid("凭证不存在"));
            if(!"dealer_delivery".equals(attachment.businessType()) || !orderId.equals(attachment.businessId())) throw invalid("凭证须绑定本订单的交付业务");
            files.requireUploaded(attachment);
        }
    }
    private DealerOrderLineRecord orderLine(String orderId,String lineId) { return orders.listLines(orderId).stream().filter(l->l.id().equals(lineId)).findFirst().orElseThrow(()->invalid("订单明细不存在")); }
    private Resolution resolution(String id) { return delivery.resolution(id).orElseThrow(()->ApiException.notFound("DEALER_DELIVERY_RESOLUTION_NOT_FOUND","差异方案不存在")); }
    private static void checkTime(Instant at,Instant earliest) { if(at==null || at.isAfter(Instant.now()) || earliest!=null && at.isBefore(earliest)) throw invalid("实际完成时间须在前序事实之后且不能晚于当前时间"); }
    private String actor() { return authorization.currentActor().id(); }
    private static String id(String prefix) { return prefix+UUID.randomUUID(); }
    private static ApiException invalid(String message) { return ApiException.badRequest("VALIDATION_FAILED",message); }
    private void recordFailure(String event,String permission,String type,String id,String key,RuntimeException error) {
        try { audit.recordFailure(event,permission,type,id,null,key,error instanceof ApiException api?api.code():error.getClass().getSimpleName()); }
        catch(RuntimeException auditError) { error.addSuppressed(auditError); }
    }
}
