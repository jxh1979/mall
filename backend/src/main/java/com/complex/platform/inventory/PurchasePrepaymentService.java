package com.complex.platform.inventory;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.*;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.governance.review.*;
import java.time.*;
import java.util.*;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 按已核合同处理未用实付与未付预付义务，原现金、审批及实际过账分别留痕。 */
@Service
public class PurchasePrepaymentService {
    private static final String MANAGE="inventory.purchase_request.manage",FINANCE="finance.ar_ap.manage";
    private final JdbcTemplate jdbc;
    private final PurchaseWorkflowService workflow;
    private final FinanceRepository finance;
    private final SupervisorReviewService reviews;
    private final AuthorizationService auth;
    public PurchasePrepaymentService(JdbcTemplate jdbc,PurchaseWorkflowService workflow,FinanceRepository finance,
            SupervisorReviewService reviews,AuthorizationService auth){
        this.jdbc=jdbc;this.workflow=workflow;this.finance=finance;this.reviews=reviews;this.auth=auth;
    }
    @Transactional
    @GuardedAction(capability=MANAGE,objectType="inventory_purchase_request")
    public Map<String,Object> resolve(String id,PurchaseWorkflowRequests.PrepaymentResolution r){
        workflow.scopedPurchase(id,MANAGE);if(r==null)throw invalid("请填写预付余额处理方案");
        return workflow.write("purchasePrepaymentResolution:"+id,r.idempotencyKey(),r,MANAGE,"inventory_purchase_request",id,()->{
            var purchase=workflow.lockedPurchase(id,MANAGE);var terms=workflow.requireSnapshot(purchase);
            if(!"prepaid".equals(terms.settlementMethod())||purchase.orderedAt()==null)throw invalid("仅已正式下单的预付采购可处理预付余额");
            if(!Set.of("refund_unused","reduce_unpaid").contains(Objects.toString(r.resolutionType(),""))||r.amountCent()<=0)throw invalid("请选择有效处理类型及金额");
            requireText(r.reason(),"reason");requireText(r.contractBasis(),"contract_basis");requireText(r.finalApproverId(),"final_approver_id");
            workflow.checkEvidence("inventory_purchase_request",id,r.evidenceAttachmentIds());
            if("refund_unused".equals(r.resolutionType())){
                var payment=workflow.prepaymentSource(purchase,r.sourcePaymentId());
                if(r.refundDueDate()==null||r.amountCent()>number(payment,"amount_cent")-workflow.prepaymentAllocated(r.sourcePaymentId())-workflow.prepaymentRefundReserved(r.sourcePaymentId(),null))
                    throw invalid("须明确退款到期日，金额不能超过真实未抵且未申请退款余额");
            }else{
                if(r.sourcePaymentId()!=null||r.refundDueDate()!=null)throw invalid("减少未付义务不引用现金付款或退款日期");
                var current=currentPrepayment(purchase);
                long reserved=sum("SELECT COALESCE(SUM(amount_cent),0) FROM purchase_prepayment_resolutions WHERE purchase_id=? AND resolution_type='reduce_unpaid' AND status IN('pending_approval','approved')",id);
                if(r.amountCent()>current.unpaidAmountCent()-reserved)throw invalid("减少额超过当前未付且尚未申请减少的义务");
            }
            String resolution="pprs_"+slug(id+":"+r.idempotencyKey());
            jdbc.update("""
                    INSERT INTO purchase_prepayment_resolutions(id,purchase_id,resolution_type,source_payment_id,amount_cent,refund_due_date,contract_basis,reason,evidence_attachment_ids,status,requested_by)
                    VALUES(?,?,?,?,?,?,?,?,?::jsonb,'pending_approval',?)
                    """,resolution,id,r.resolutionType(),r.sourcePaymentId(),r.amountCent(),r.refundDueDate(),r.contractBasis(),r.reason(),workflow.json(r.evidenceAttachmentIds()),actor());
            var facts=workflow.purchaseFacts(id);var original=new LinkedHashMap<String,Object>();
            original.put("reason",r.reason());original.put("purchase",purchase);original.put("contract_snapshot",workflow.readMap(facts.get("contract_snapshot")));
            original.put("prepayment_resolution",row(resolution));original.put("accepted_quantity",facts.get("accepted_quantity"));
            original.put("terminated_quantity",facts.get("terminated_quantity"));original.put("remaining_quantity",purchase.quantity()-number(facts,"accepted_quantity")-number(facts,"terminated_quantity"));
            var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",r.reason(),r.evidenceAttachmentIds(),
                    "inventory.purchase_prepayment_resolution","purchase_prepayment_resolution",resolution,r.amountCent(),workflow.json(original),r.finalApproverId(),r.parallelLines(),r.beneficiaryId()));
            workflow.requireBlocked(review);jdbc.update("UPDATE purchase_prepayment_resolutions SET review_id=? WHERE id=?",review.reviewId(),resolution);return row(resolution);
        },Map.class);
    }
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision){
        if(!"inventory.purchase_prepayment_resolution".equals(decision.actionType())||!"purchase_prepayment_resolution".equals(decision.objectType())
                ||!Set.of("approved","rejected").contains(decision.reviewStatus()))return;
        var actual=reviews.findReview(decision.reviewId()).orElse(null);
        if(actual==null||!Objects.equals(actual.actionType(),decision.actionType())||!Objects.equals(actual.objectType(),decision.objectType())
                ||!Objects.equals(actual.objectId(),decision.objectId())||!Objects.equals(actual.reviewStatus(),decision.reviewStatus()))return;
        var initial=row(decision.objectId());workflow.purchaseLock((String)initial.get("purchase_id"));var current=row(decision.objectId());
        if(decision.reviewId().equals(current.get("review_id"))&&"pending_approval".equals(current.get("status")))
            jdbc.update("UPDATE purchase_prepayment_resolutions SET status=? WHERE id=?",decision.reviewStatus(),decision.objectId());
    }
    @Transactional
    @GuardedAction(capability=FINANCE,objectType="purchase_prepayment_resolution")
    public Map<String,Object> post(String id,PurchaseWorkflowRequests.PrepaymentPost r){
        var initial=row(id);String purchaseId=(String)initial.get("purchase_id");workflow.scopedPurchase(purchaseId,FINANCE);
        if(r==null)throw invalid("请填写预付处理财务过账依据");
        return workflow.write("postPurchasePrepaymentResolution:"+id,r.idempotencyKey(),r,FINANCE,"purchase_prepayment_resolution",id,()->{
            var purchase=workflow.lockedPurchase(purchaseId,FINANCE);var current=row(id);
            if(!"approved".equals(current.get("status"))||!reviews.findReview((String)current.get("review_id")).map(d->"approved".equals(d.reviewStatus())).orElse(false))
                throw invalid("余额方案须完整批准，且不得重复过账");
            if(actor().equals(current.get("requested_by")))throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN","余额申请人不能自行完成财务过账");
            requireText(r.reason(),"reason");workflow.checkEvidence("purchase_prepayment_resolution",id,r.evidenceAttachmentIds());
            long amount=number(current,"amount_cent");
            if("refund_unused".equals(current.get("resolution_type"))){
                String paymentId=(String)current.get("source_payment_id");var payment=workflow.prepaymentSource(purchase,paymentId);
                if(amount>number(payment,"amount_cent")-workflow.prepaymentAllocated(paymentId)-workflow.prepaymentRefundReserved(paymentId,id))throw invalid("原付款可退余额已变化，请重新核定");
                LocalDate due=((java.sql.Date)current.get("refund_due_date")).toLocalDate();
                String receivable=workflow.postReceivable(purchase,id,amount,due,"purchase_prepayment_refund");
                jdbc.update("UPDATE purchase_prepayment_resolutions SET supplier_refund_item_id=?,status='posted',posted_by=?,posted_at=now(),post_evidence_ids=?::jsonb WHERE id=?",
                        receivable,actor(),workflow.json(r.evidenceAttachmentIds()),id);
            }else{
                var payable=currentPrepayment(purchase);if(amount>payable.unpaidAmountCent())throw invalid("原义务已发生付款变化，请重新核定减少金额");
                Instant now=Instant.now();String reversal="pprev_"+slug(id),replacement=null;
                finance.appendArApItem(new ArApItemRecord(reversal,"CZ-"+slug(reversal),"payable",purchase.supplierId(),"purchase_prepayment",purchaseId,
                        -payable.amountCent(),-payable.paidAmountCent(),-payable.unpaidAmountCent(),"CNY",payable.dueDate(),payable.accountingPeriod(),"reversed",now,now,purchase.merchantId(),payable.projectId(),payable.id(),null));
                source(reversal,purchaseId,-payable.amountCent(),now);
                long newAmount=payable.amountCent()-amount,newUnpaid=payable.unpaidAmountCent()-amount;
                if(newAmount>0){
                    replacement="ppnew_"+slug(id);
                    finance.appendArApItem(new ArApItemRecord(replacement,"AP-"+slug(replacement),"payable",purchase.supplierId(),"purchase_prepayment",purchaseId,
                            newAmount,payable.paidAmountCent(),newUnpaid,"CNY",payable.dueDate(),payable.accountingPeriod(),newUnpaid==0?"settled":payable.paidAmountCent()>0?"partial_paid":"pending_payment",now,now,purchase.merchantId(),payable.projectId(),payable.id(),null));
                    source(replacement,purchaseId,newAmount,now);
                }
                jdbc.update("UPDATE purchase_prepayment_resolutions SET original_item_id=?,reversal_item_id=?,replacement_item_id=?,status='posted',posted_by=?,posted_at=now(),post_evidence_ids=?::jsonb WHERE id=?",
                        payable.id(),reversal,replacement,actor(),workflow.json(r.evidenceAttachmentIds()),id);
            }
            return row(id);
        },Map.class);
    }
    private ArApItemRecord currentPrepayment(PurchaseRequestRecord p){
        return workflow.currentFinancialTarget(p,"purchase_prepayment",p.id());
    }
    private void source(String item,String purchase,long amount,Instant time){finance.saveArApItemSource(new ArApItemSourceRecord("pas_"+slug(item),item,"purchase_prepayment",purchase,Math.abs(amount),time));}
    private Map<String,Object> row(String id){return jdbc.queryForList("SELECT * FROM purchase_prepayment_resolutions WHERE id=?",requireText(id,"id")).stream().findFirst().orElseThrow(()->ApiException.notFound("PURCHASE_PREPAYMENT_RESOLUTION_NOT_FOUND","预付余额处理事项不存在"));}
    private long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private long sum(String sql,Object...args){Long value=jdbc.queryForObject(sql,Long.class,args);return value==null?0:value;}
    private String actor(){return auth.currentActor().id();}
    private String slug(String seed){return com.complex.platform.common.Hashing.idempotencyCode(seed);}
    private ApiException invalid(String message){return ApiException.conflict("PURCHASE_PREPAYMENT_INVALID",message);}
}
