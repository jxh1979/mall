package com.complex.platform.dealer;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.dto.*;
import com.complex.platform.evidence.*;
import com.complex.platform.finance.*;
import com.complex.platform.governance.review.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 退货运费与真实退款、发票事实；资金只复用统一财务退款，不接受客户端自报到账。 */
@Service
public class DealerReturnFinancialService {
    private final JdbcDealerReturnRepository returns;
    private final JdbcDealerOrderRepository orders;
    private final JdbcDealerRepository dealers;
    private final JdbcDealerDeliveryRepository delivery;
    private final JdbcTemplate jdbc;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final SupervisorReviewService reviews;
    private final FinanceService finance;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator files;
    private final ObjectMapper mapper;
    public DealerReturnFinancialService(JdbcDealerReturnRepository returns,JdbcDealerOrderRepository orders,JdbcDealerRepository dealers,JdbcDealerDeliveryRepository delivery,
            JdbcTemplate jdbc,AuthorizationService authorization,IdempotencyService idempotency,AuditEventService audit,
            SupervisorReviewService reviews,FinanceService finance,EvidenceEventRepository evidence,EvidenceAttachmentValidator files,ObjectMapper mapper) {
        this.returns=returns;this.orders=orders;this.dealers=dealers;this.delivery=delivery;this.jdbc=jdbc;this.authorization=authorization;this.idempotency=idempotency;
        this.audit=audit;this.reviews=reviews;this.finance=finance;this.evidence=evidence;this.files=files;this.mapper=mapper;
    }
    public void requireEvidenceScope(String returnId,String evidencePermission) {
        var ret=requireReturn(returnId);String project=project(ret.orderId(),evidencePermission);
        var actor=authorization.currentActor();
        var permitted=Set.of("evidence.timeline.view","evidence.attachment.download").contains(evidencePermission)
                ?List.of(DealerReturnService.VIEW,DealerReturnService.MANAGE,DealerReturnService.INSPECT,DealerReturnService.REFUND_EXECUTE)
                :List.of(DealerReturnService.MANAGE,DealerReturnService.INSPECT,DealerReturnService.REFUND_EXECUTE);
        if(!permitted.stream().anyMatch(p->authorization.currentActorHasPermission(p) && authorization.canReachProject(actor,project,p))) throw forbidden();
    }
    public void requireInvoiceEvidenceScope(String invoiceId,String evidencePermission) {
        var invoice=requireInvoice(invoiceId);project(invoice.orderId(),evidencePermission);project(invoice.orderId(),DealerReturnService.INVOICE_MANAGE);
    }
    public DealerReturnFinancialRecord get(String returnId) {
        return audited("dealer.return.financial_viewed","dealer.return.financial_view_failed",DealerReturnService.VIEW,"dealer_return",returnId,null,()->{
            var ret=requireReturn(returnId);project(ret.orderId(),DealerReturnService.VIEW);return summary(returnId);
        });
    }
    // 仅供已通过当前经销商、门户开关及订单归属验证的门户服务使用。
    List<JdbcDealerReturnRepository.ReturnSource> ownedReturnSources(String orderId) {return returns.returnSources(orderId);}
    List<DealerReturnFinancialRecord> ownedReturnFinancials(String orderId) {return returns.listReturns(orderId).stream().map(r->summary(r.id())).toList();}
    void goodsAccepted(String orderId) {refreshInvoiceBasis(orderId);}
    public List<JdbcDealerReturnRepository.ReturnSource> returnSources(String orderId) {
        return audited("dealer.return.sources_viewed","dealer.return.sources_view_failed",DealerReturnService.VIEW,"dealer_order",orderId,null,()->{
            project(orderId,DealerReturnService.VIEW);return returns.returnSources(orderId);
        });
    }
    @Transactional
    public DealerReturnRecord confirmFreight(String returnId,DealerReturnFreightConfirmRequest request) {
        return audited("dealer.return.freight_confirmed","dealer.return.freight_confirm_failed",DealerReturnService.MANAGE,"dealer_return",returnId,key(request),()->{
            var initial=requireReturn(returnId);project(initial.orderId(),DealerReturnService.MANAGE);
            return idempotency.execute("dealerReturnFreight:"+returnId,key(request),request,()->{
                var order=lockOrder(initial.orderId());var ret=requireReturn(returnId);
                if(Set.of("rejected","pending_approval").contains(ret.status())) throw invalid("退货批准后才可确认运费");
                var contract=order.contractSnapshot();if(contract==null) throw invalid("原订单锁定合同缺失");
                String bearer=requireText(request==null?null:request.returnFreightBearer(),"return_freight_bearer");
                if(!Set.of("dealer","sales_entity").contains(bearer) || request.firstLegFreightRefundable()==null) throw invalid("请明确退回运费承担方及首程运费是否退回");
                String lockedBearer="quality_issue".equals(ret.reasonType())?"sales_entity":contract.returnFreightBearer();
                if(lockedBearer!=null && !"pending_confirmation".equals(lockedBearer) && !lockedBearer.equals(bearer)) throw invalid("不能覆盖原订单锁定的退回运费规则");
                if(contract.firstLegFreightRefundable()!=null && !contract.firstLegFreightRefundable().equals(request.firstLegFreightRefundable())) throw invalid("不能覆盖原订单锁定的首程运费规则");
                if(request.firstLegAdjustmentCent()==null || request.firstLegAdjustmentCent()<0) throw invalid("须明确本次首程运费调减金额，不能由退货比例自动推算");
                long amount=request.firstLegAdjustmentCent();
                if(!request.firstLegFreightRefundable() && amount!=0) throw invalid("不退首程运费时金额必须为零");
                if(ret.inspectedAt()==null) throw invalid("实际验收确认后才核定运费金额");
                evidence("dealer_return",returnId,request.evidenceAttachmentIds());
                if(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_return_freight_adjustments WHERE return_id=? AND status<>'rejected'",Integer.class,returnId)>0) throw invalid("本次运费已有确认记录，不能覆盖已批准或在审金额");
                long occupied=jdbc.queryForObject("SELECT COALESCE(SUM(amount_cent),0) FROM dealer_return_freight_adjustments WHERE order_id=? AND status<>'rejected'",Long.class,order.id());
                if(amount>order.freightCent()-occupied || amount>order.netReceivableCent()) throw invalid("累计首程调减不得超过原已收取或应收运费及原单剩余净额");
                String id="drfa_"+UUID.randomUUID();
                String basis=lockedBearer!=null && contract.firstLegFreightRefundable()!=null?"locked_contract":"mutual_confirmation";
                String reviewId=null,status="applied";
                if(amount>0) {
                    var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(key(request)+":freight-review","经销首程运费调减",request.evidenceAttachmentIds(),
                            "refund","dealer_return_freight",id,amount,requireText(request.note(),"reason"),null,List.of(),null));
                    reviewId=review.reviewId();status="approved".equals(review.reviewStatus())?"approved":"pending_approval";
                    if(reviewId==null) throw invalid("运费调减须完成现有退款审批");
                }
                jdbc.update("""
                        INSERT INTO dealer_return_freight_adjustments(id,return_id,order_id,contract_terms_version,basis_type,return_freight_bearer,
                         first_leg_refundable,amount_cent,review_id,status,evidence_attachment_ids,confirmed_by,confirmed_at,applied_at,note)
                        VALUES(?,?,?,?,?,?,?,?,?,?,?::jsonb,?,now(),?::timestamptz,?)
                        """,id,returnId,order.id(),contract.termsVersion(),basis,bearer,request.firstLegFreightRefundable(),amount,reviewId,status,
                        json(request.evidenceAttachmentIds()),actor(),amount==0?Timestamp.from(Instant.now()):null,request.note());
                jdbc.update("UPDATE dealer_returns SET return_freight_bearer=?,first_leg_freight_refundable=?,freight_confirmed=true,updated_at=now() WHERE id=?",bearer,request.firstLegFreightRefundable(),returnId);
                if("approved".equals(status)) applyFreight(id);
                return requireReturn(returnId);
            },DealerReturnRecord.class);
        });
    }
    @EventListener
    @Transactional
    public void freightReviewed(SupervisorReviewDecisionRecord decision) {
        if(!"refund".equals(decision.actionType()) || !"dealer_return_freight".equals(decision.objectType())) return;
        var rows=jdbc.queryForList("SELECT id,order_id FROM dealer_return_freight_adjustments WHERE review_id=?",decision.reviewId());
        if(rows.isEmpty()) return;
        lockOrder((String)rows.getFirst().get("order_id"));String id=(String)rows.getFirst().get("id");
        if(!Set.of("approved","rejected").contains(decision.reviewStatus())) return;
        int changed=jdbc.update("UPDATE dealer_return_freight_adjustments SET status=? WHERE id=? AND review_id=? AND status='pending_approval'",decision.reviewStatus(),id,decision.reviewId());
        if(changed==1 && "approved".equals(decision.reviewStatus())) applyFreight(id);
    }
    private void applyFreight(String id) {
        var row=jdbc.queryForMap("SELECT * FROM dealer_return_freight_adjustments WHERE id=? FOR UPDATE",id);
        if(!"approved".equals(row.get("status"))) return;
        String returnId=(String)row.get("return_id"),orderId=(String)row.get("order_id"),reviewId=(String)row.get("review_id");
        if(!reviews.findReview(reviewId).map(r->"approved".equals(r.reviewStatus()) && "refund".equals(r.actionType()) && "dealer_return_freight".equals(r.objectType()) && id.equals(r.objectId())).orElse(false)) throw invalid("运费审批与当前方案不符");
        evidence("dealer_return",returnId,strings((String)row.get("evidence_attachment_ids").toString()));
        var order=orders.findOrder(orderId).orElseThrow();long amount=number(row,"amount_cent");
        if(amount>order.netReceivableCent()) throw invalid("应收已变化，原批准运费金额超过剩余净额");
        long offset=Math.min(amount,Math.max(0,order.netReceivableCent()-order.paidAmountCent()));long refund=amount-offset;
        createRefunds(requireReturn(returnId),refund,reviewId,"first_leg",id);
        if(order.arApItemId()!=null) returns.offsetReceivable(order.arApItemId(),amount);
        jdbc.update("""
                UPDATE dealer_orders SET first_leg_adjustment_cent=first_leg_adjustment_cent+?,
                 overdue=CASE WHEN paid_amount_cent>=total_amount_cent-returned_amount_cent-delivery_adjustment_cent-first_leg_adjustment_cent-? THEN false ELSE overdue END,
                 updated_at=now() WHERE id=?
                """,amount,amount,orderId);
        jdbc.update("UPDATE dealer_return_freight_adjustments SET status='applied',offset_cent=?,refund_cent=?,applied_at=now() WHERE id=?",offset,refund,id);
        if(!orders.dealerHasOverdueOrders(order.dealerId())) dealers.setCreditSuspended(order.dealerId(),false);
        refreshInvoiceBasis(orderId);
    }
    /** 已批准金额按真实原收款拆分；包含预付款原款，不回预付余额，不释放失败退款占用。 */
    private void createRefunds(DealerReturnRecord ret,long amount,String reviewId,String component,String adjustmentId) {
        long existing=jdbc.queryForObject("SELECT COALESCE(SUM(amount_cent),0) FROM dealer_order_refunds WHERE return_id=? AND return_component=? AND status<>'voided'",Long.class,ret.id(),component);
        if(existing!=0) { if(existing!=amount) throw invalid("退款来源金额与批准金额不一致");return; }
        if(amount==0) return;
        var sources=new TreeMap<String,Long>();
        orders.listPayments(ret.orderId()).stream().filter(p->"verified".equals(p.status()) && p.appliedCent()>0).forEach(p->sources.merge(p.id(),p.appliedCent(),Math::addExact));
        for(var allocation:orders.listOrderAllocations(ret.orderId())) sources.merge(orders.findOverpayment(allocation.overpaymentId()).orElseThrow().paymentId(),allocation.amountCent(),Math::addExact);
        sources.keySet().forEach(orders::lockPayment);long remaining=amount;
        for(var entry:sources.entrySet()) {
            if(remaining==0) break;var payment=orders.findPayment(entry.getKey()).orElseThrow();
            long used=orders.listRefunds(ret.orderId()).stream().filter(r->entry.getKey().equals(r.paymentId()) && !"voided".equals(r.status())).mapToLong(DealerOrderRefundRecord::amountCent).sum();
            long capacity=Math.min(entry.getValue()-used,payment.amountCent()-orders.reservedRefundAmount(payment.id()));
            long part=Math.min(remaining,Math.max(0,capacity));if(part==0) continue;
            if(!"verified".equals(payment.status())) throw invalid("原收款尚未核验");
            String id="drf_"+UUID.randomUUID();Instant now=Instant.now();
            orders.saveRefund(new DealerOrderRefundRecord(id,ret.orderId(),payment.id(),part,"approved",reviewId,actor(),null,null,null,
                    "经销退货 "+ret.returnNo()+" "+component+" 原路退款",now,now,null,null));
            jdbc.update("UPDATE dealer_order_refunds SET return_id=?,return_component=?,return_freight_adjustment_id=? WHERE id=?",ret.id(),component,adjustmentId,id);
            remaining-=part;
        }
        if(remaining!=0) throw invalid("原收款可退余额不足，须核实既有退款占用，不能重复退或虚构来源");
    }
    @Transactional
    public DealerReturnFinancialRecord executeRefunds(String returnId,DealerIdempotentRequest request) {
        return audited("dealer.return.refund_requested","dealer.return.refund_execute_failed",DealerReturnService.REFUND_EXECUTE,"dealer_return",returnId,request==null?null:request.idempotencyKey(),()->{
            var initial=requireReturn(returnId);String businessProject=project(initial.orderId(),DealerReturnService.REFUND_EXECUTE);
            if(Objects.equals(actor(),initial.inspectedBy())) throw ApiException.forbidden("DEALER_SOD_SAME_PERSON","退货验收人不得执行退款");
            for(var refund:orders.listRefunds(initial.orderId())) {
                if(!orders.findReturnRefundSource(refund.id()).map(source->returnId.equals(source.returnId())).orElse(false)) continue;
                var payment=orders.findPayment(refund.paymentId()).orElseThrow();
                project(payment.orderId(),DealerReturnService.REFUND_EXECUTE);
                if(Objects.equals(actor(),payment.verifiedBy())) throw ApiException.forbidden("DEALER_SOD_SAME_PERSON","原到账核验人与退款执行人不得相同");
            }
            return idempotency.execute("dealerReturnFinance:"+returnId,request.idempotencyKey(),request,()->{
                lockOrder(initial.orderId());var ret=requireReturn(returnId);
                if(Objects.equals(actor(),ret.inspectedBy())) throw ApiException.forbidden("DEALER_SOD_SAME_PERSON","退货验收人不得执行退款");
                if("pending_reversal".equals(ret.status())) throw invalid("原发票须实际红冲成功后才能退款");
                if(ret.refundCent()>0) {
                    if(!ret.refundApproved() || !reviews.findReview(ret.refundReviewId()).map(r->"approved".equals(r.reviewStatus()) && "refund".equals(r.actionType()) && "dealer_return".equals(r.objectType()) && returnId.equals(r.objectId())).orElse(false)) throw invalid("货款退款尚未批准");
                    createRefunds(ret,ret.refundCent(),ret.refundReviewId(),"goods",null);
                }
                var ids=jdbc.query("SELECT id FROM dealer_order_refunds WHERE return_id=? ORDER BY id",(r,n)->r.getString(1),returnId);
                for(String id:ids) {
                    var refund=orders.lockRefund(id).orElseThrow();if(refund.financeRefundId()!=null) continue;
                    var payment=orders.findPayment(refund.paymentId()).orElseThrow();project(payment.orderId(),DealerReturnService.REFUND_EXECUTE);
                    if(!"approved".equals(refund.status()) || !"verified".equals(payment.status())) throw invalid("退款或原收款状态无效");
                    if(Objects.equals(actor(),payment.verifiedBy())) throw ApiException.forbidden("DEALER_SOD_SAME_PERSON","原到账核验人与退款执行人不得相同");
                    String financeId=finance.recordDealerOrderRefund(id,refund.amountCent(),payment.method(),requireText(payment.referenceNo(),"reference_no"),businessProject,refund.reason());
                    orders.saveRefund(new DealerOrderRefundRecord(refund.id(),refund.orderId(),refund.paymentId(),refund.amountCent(),refund.status(),refund.reviewId(),refund.requestedBy(),
                            null,null,null,refund.reason(),refund.createdAt(),Instant.now(),financeId,refund.allocationId()));
                }
                return summary(returnId);
            },DealerReturnFinancialRecord.class);
        });
    }
    @EventListener
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void financeResult(FinanceRefundStateChanged event) {
        if(!"dealer_order_refund".equals(event.refund().sourceType())) return;
        var source=orders.findReturnRefundSource(event.refund().targetId()).orElse(null);if(source==null) return;
        lockOrder(source.orderId());var ret=requireReturn(source.returnId());
        long refunded=successAmount(ret.id(),"goods");
        if(refunded>ret.refundCent()) throw invalid("真实退款总额超过原批准货款退款");
        String state=ret.status();
        if(ret.refundCent()>0 && !"pending_reversal".equals(state)) state=refunded==ret.refundCent()?(ret.acceptedQuantity()>=ret.requestedQuantity()?"completed":"partially_completed"):
                "failed".equals(event.refund().status())?"refund_failed":"refunding";
        jdbc.update("UPDATE dealer_returns SET refunded_cent=?,status=?,updated_at=now() WHERE id=?",refunded,state,ret.id());
        finalizeOrderStatus(ret.orderId());
        refreshInvoiceBasis(ret.orderId());
        audit.recordSuccess("dealer.return.refund_result_recorded",null,"dealer_return",ret.id(),null,null,Map.of("finance_refund_id",event.refund().id(),"status",event.refund().status()));
    }
    private long successAmount(String returnId,String component) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(r.amount_cent),0) FROM dealer_order_refunds r JOIN finance_refunds f ON f.id=r.finance_refund_id
                 WHERE r.return_id=? AND r.return_component=? AND f.status='success'
                """,Long.class,returnId,component);
    }
    private DealerReturnFinancialRecord summary(String returnId) {
        var ret=requireReturn(returnId);var rows=jdbc.queryForList("SELECT * FROM dealer_return_freight_adjustments WHERE return_id=? AND status<>'rejected'",returnId);
        Map<String,Object> freight=rows.isEmpty()?Map.of():rows.getFirst();
        var refunds=jdbc.query("SELECT id,return_component,amount_cent,status,finance_refund_id FROM dealer_order_refunds WHERE return_id=? ORDER BY created_at,id",
                (r,n)->new DealerReturnFinancialRecord.Refund(r.getString(1),r.getString(2),r.getLong(3),r.getString(4),r.getString(5)),returnId);
        return new DealerReturnFinancialRecord(returnId,ret.acceptedAmountCent(),ret.offsetReceivableCent(),ret.refundCent(),successAmount(returnId,"goods"),
                (String)freight.get("id"),(String)freight.get("status"),number(freight,"amount_cent"),number(freight,"offset_cent"),number(freight,"refund_cent"),successAmount(returnId,"first_leg"),refunds);
    }
    private record Basis(long goods,long freight) {long total(){return goods+freight;}String fingerprint(){return goods+":"+freight;}}
    private Basis basis(String orderId) {
        var order=orders.findOrder(orderId).orElseThrow();
        long offset=jdbc.queryForObject("SELECT COALESCE(SUM(offset_cent),0) FROM dealer_return_freight_adjustments WHERE order_id=? AND status='applied'",Long.class,orderId);
        long refunded=jdbc.queryForObject("""
                SELECT COALESCE(SUM(r.amount_cent),0) FROM dealer_order_refunds r JOIN finance_refunds f ON f.id=r.finance_refund_id
                WHERE r.order_id=? AND r.return_component='first_leg' AND f.status='success'
                """,Long.class,orderId);
        long goods=order.totalAmountCent()-order.freightCent()-order.returnedAmountCent()-order.deliveryAdjustmentCent();
        long freight=order.freightCent()-offset-refunded;
        if(goods<0 || freight<0) throw invalid("开票组成金额与原单调整不一致，须财务核实");
        return new Basis(goods,freight);
    }
    /** 已发票保留原金额；实际退款改变基础后必须真实红冲，不原地改写已开票。 */
    private void refreshInvoiceBasis(String orderId) {
        var current=returns.findActiveInvoice(orderId).orElse(null);if(current==null) return;var basis=basis(orderId);
        if(current.amountCent()==basis.total()) return;
        if("issued".equals(current.status())) jdbc.update("UPDATE dealer_invoices SET requires_reversal=true WHERE id=?",current.id());
        else {
            jdbc.update("UPDATE dealer_invoices SET status='voided',note=COALESCE(note,'')||'；实际退款改变开票基础，原申请作废' WHERE id=?",current.id());
            if(basis.total()>0 && !allReturned(orderId)) createInvoice(orderId,current.reissuedFromInvoiceId(),"实际退款后重新核定开票申请");
        }
    }
    @Transactional
    public DealerInvoiceRecord requestInvoice(String orderId,DealerInvoiceRequest request) {
        return audited("dealer.invoice.requested","dealer.invoice.request_failed",DealerReturnService.INVOICE_MANAGE,"dealer_order",orderId,request==null?null:request.idempotencyKey(),()->{
            project(orderId,DealerReturnService.INVOICE_MANAGE);
            return idempotency.execute("dealerInvoiceRequest:"+orderId,request.idempotencyKey(),request,()->{
                var order=lockOrder(orderId);
                if(!DealerReturnService.RETURNABLE_ORDER_STATUSES.contains(order.status()) || allReturned(orderId)) throw invalid("已签收且仍有未退商品才可申请发票");
                if(returns.findActiveInvoice(orderId).isPresent()) throw invalid("已有有效发票，须先完成原发票红冲");
                return createInvoice(orderId,null,request.note());
            },DealerInvoiceRecord.class);
        });
    }
    private DealerInvoiceRecord createInvoice(String orderId,String original,String note) {
        var order=orders.findOrder(orderId).orElseThrow();var value=basis(orderId);if(value.total()<=0) throw invalid("无可开票金额");
        String id="dinv_"+UUID.randomUUID();Instant now=Instant.now();
        Integer rate=orders.listLines(orderId).stream().map(DealerOrderLineRecord::taxRateBps).filter(Objects::nonNull).findFirst().orElse(null);
        returns.saveInvoice(new DealerInvoiceRecord(id,orderId,order.dealerId(),order.salesEntityId(),value.total(),rate,"pending",null,null,original,actor(),now,null,null,null,note));
        jdbc.update("UPDATE dealer_invoices SET basis_fingerprint=?,retained_goods_cent=?,retained_freight_cent=? WHERE id=?",value.fingerprint(),value.goods(),value.freight(),id);
        return requireInvoice(id);
    }
    @Transactional
    public DealerInvoiceRecord issueInvoice(String invoiceId,DealerInvoiceIssueRequest request) {
        return audited("dealer.invoice.issued","dealer.invoice.issue_failed",DealerReturnService.INVOICE_MANAGE,"dealer_invoice",invoiceId,request==null?null:request.idempotencyKey(),()->{
            var initial=requireInvoice(invoiceId);project(initial.orderId(),DealerReturnService.INVOICE_MANAGE);
            return idempotency.execute("dealerInvoiceIssue:"+invoiceId,request.idempotencyKey(),request,()->{
                lockOrder(initial.orderId());var invoice=requireInvoice(invoiceId);
                if(!Set.of("pending","issuing","failed").contains(invoice.status())) throw invalid("仅待开票记录可登记实际开票结果");
                var value=basis(invoice.orderId());
                if(invoice.amountCent()!=value.total()) throw invalid("开票基础已变化，须重新生成正确金额的申请");
                evidence("dealer_invoice",invoiceId,request.evidenceAttachmentIds());String no=requireText(request.invoiceNo(),"invoice_no");
                jdbc.update("UPDATE dealer_invoices SET status='issued',invoice_no=?,issued_by=?,issued_at=now(),issue_evidence_attachment_ids=?::jsonb WHERE id=?",no,actor(),json(request.evidenceAttachmentIds()),invoiceId);
                return requireInvoice(invoiceId);
            },DealerInvoiceRecord.class);
        });
    }
    @Transactional
    public DealerReturnRecord requestReturnReversal(String returnId,DealerInvoiceIssueRequest request) {
        return audited("dealer.invoice.reversal_requested","dealer.invoice.reverse_failed",DealerReturnService.INVOICE_MANAGE,"dealer_return",returnId,request==null?null:request.idempotencyKey(),()->{
            var ret=requireReturn(returnId);project(ret.orderId(),DealerReturnService.INVOICE_MANAGE);
            return idempotency.execute("dealerReturnReverse:"+returnId,request.idempotencyKey(),request,()->{
                lockOrder(ret.orderId());retCheckPendingReversal(returnId);
                var invoice=returns.findActiveInvoice(ret.orderId()).filter(i->"issued".equals(i.status())).orElseThrow(()->invalid("原单没有已开具发票"));
                jdbc.update("UPDATE dealer_invoices SET requires_reversal=true,reversal_status='requested',reversed_by_return_id=? WHERE id=? AND reversal_status IS DISTINCT FROM 'success'",returnId,invoice.id());
                return requireReturn(returnId);
            },DealerReturnRecord.class);
        });
    }
    private void retCheckPendingReversal(String id) {if(!"pending_reversal".equals(requireReturn(id).status())) throw invalid("该退货不在待红冲状态");}
    @Transactional
    public DealerInvoiceRecord recordReversal(String invoiceId,DealerInvoiceReversalResultRequest request) {
        return audited("dealer.invoice.reversal_result_recorded","dealer.invoice.reversal_result_failed",DealerReturnService.INVOICE_MANAGE,"dealer_invoice",invoiceId,request==null?null:request.idempotencyKey(),()->{
            var initial=requireInvoice(invoiceId);project(initial.orderId(),DealerReturnService.INVOICE_MANAGE);
            return idempotency.execute("dealerInvoiceRedResult:"+invoiceId,request.idempotencyKey(),request,()->{
                lockOrder(initial.orderId());var invoice=requireInvoice(invoiceId);
                var facts=jdbc.queryForMap("SELECT requires_reversal,reversal_status FROM dealer_invoices WHERE id=?",invoiceId);
                if(!"issued".equals(invoice.status()) || !Boolean.TRUE.equals(facts.get("requires_reversal"))) throw invalid("该发票没有待处理红冲事项");
                if(!Set.of("processing","failed","success").contains(request.result())) throw invalid("请登记实际红冲结果");
                evidence("dealer_invoice",invoiceId,request.evidenceAttachmentIds());
                String reference=requireText(request.referenceNo(),"reference_no");
                if("failed".equals(request.result())) requireText(request.failureReason(),"failure_reason");
                jdbc.update("UPDATE dealer_invoices SET reversal_status=?,reversal_reference_no=?,reversal_evidence_attachment_ids=?::jsonb WHERE id=?",request.result(),reference,json(request.evidenceAttachmentIds()),invoiceId);
                if("success".equals(request.result())) {
                    jdbc.update("UPDATE dealer_invoices SET status='reversed',reversed_at=now(),requires_reversal=false WHERE id=?",invoiceId);
                    if(!allReturned(invoice.orderId()) && basis(invoice.orderId()).total()>0) createInvoice(invoice.orderId(),invoiceId,"实际红冲成功后按当前未退金额重开");
                    for(var ret:returns.listReturns(invoice.orderId())) if("pending_reversal".equals(ret.status())) {
                        String reviewId=ret.refundReviewId();
                        if(ret.refundCent()>0 && reviewId==null) reviewId=reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey()+":"+ret.id(),"经销退货退款",List.of(),
                                "refund","dealer_return",ret.id(),ret.refundCent(),"原发票实际红冲成功，原路退还货款",null,List.of(),null)).reviewId();
                        String status=ret.refundCent()>0?"pending_refund":ret.acceptedQuantity()>=ret.requestedQuantity()?"completed":"partially_completed";
                        jdbc.update("UPDATE dealer_returns SET status=?,refund_review_id=?,updated_at=now() WHERE id=?",status,reviewId,ret.id());
                    }
                    finalizeOrderStatus(invoice.orderId());
                }
                return requireInvoice(invoiceId);
            },DealerInvoiceRecord.class);
        });
    }
    private void finalizeOrderStatus(String orderId) {
        if(!returns.hasOpenReturn(orderId)) orders.updateOrderStatus(orderId,allReturned(orderId)?"returned":"partially_returned");
    }
    private boolean allReturned(String orderId) {return delivery.allAcceptedGoodsReturned(orderId,orders.listLines(orderId));}
    private DealerOrderRecord lockOrder(String orderId) {var order=orders.findOrder(orderId).orElseThrow();dealers.lockDealerCredit(order.dealerId()).orElseThrow();return orders.lockOrder(orderId).orElseThrow();}
    private String project(String orderId,String permission) {authorization.requirePermission(permission);String value=orders.findOrderProjectId(orderId).orElse(null);if(value==null || !authorization.canReachProject(authorization.currentActor(),value,permission)) throw forbidden();return value;}
    private DealerReturnRecord requireReturn(String id) {return returns.findReturn(requireText(id,"return_id")).orElseThrow(()->ApiException.notFound("DEALER_RETURN_NOT_FOUND","退货单不存在"));}
    private DealerInvoiceRecord requireInvoice(String id) {return returns.findInvoice(requireText(id,"invoice_id")).orElseThrow(()->ApiException.notFound("DEALER_INVOICE_NOT_FOUND","发票不存在"));}
    private void evidence(String type,String id,List<String> ids) {if(ids==null || ids.isEmpty()) throw invalid("须上传本业务的真实凭证");for(String key:ids) {var file=evidence.findAttachmentById(key).orElseThrow(()->invalid("凭证不存在"));if(!type.equals(file.businessType()) || !id.equals(file.businessId())) throw invalid("凭证未绑定本业务");files.requireUploaded(file);}}
    private String actor(){return authorization.currentActor().id();}
    private String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
    private List<String> strings(String value){try{return mapper.readValue(value,new TypeReference<List<String>>(){});}catch(Exception e){throw new IllegalStateException(e);}}
    private static long number(Map<String,Object> row,String key){return row.get(key)==null?0:((Number)row.get(key)).longValue();}
    private static String key(DealerReturnFreightConfirmRequest request){return request==null?null:request.idempotencyKey();}
    private static ApiException invalid(String message){return ApiException.conflict("DEALER_RETURN_FINANCIAL_INVALID",message);}
    private static ApiException forbidden(){return ApiException.forbidden("CROSS_PROJECT_FORBIDDEN","退货款项或发票不在授权项目范围内");}
    private <T>T audited(String event,String failed,String permission,String type,String id,String key,Supplier<T> work) {
        try {T result=work.get();audit.recordSuccess(event,permission,type,id,null,key,Map.of());return result;}
        catch(RuntimeException error){try{audit.recordFailure(failed,permission,type,id,null,key,error instanceof ApiException api?api.code():error.getClass().getSimpleName());}catch(RuntimeException auditError){error.addSuppressed(auditError);}throw error;}
    }
}
