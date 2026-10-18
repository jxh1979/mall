package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarPayrollSupport.*;
import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.bar.BarPayrollRequests.*;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.*;
import com.complex.platform.governance.review.*;
import com.complex.platform.governance.chain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 收费与分配各有不可变草案；双方确认不能代替正式审批。 */
@Service
public class BarPayrollRuleService {
    public static final String ACTION="bar.payroll_rule.adjust",OBJECT="bar_payroll_rule";
    private final BarPayrollSupport p;
    private final SupervisorReviewService reviews;
    private final IdempotencyService idem;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator attachments;
    private final AuditEventService audit;
    public BarPayrollRuleService(BarPayrollSupport p,SupervisorReviewService reviews,IdempotencyService idem,
            EvidenceEventRepository evidence,EvidenceAttachmentValidator attachments,AuditEventService audit) {
        this.p=p;this.reviews=reviews;this.idem=idem;this.evidence=evidence;this.attachments=attachments;this.audit=audit;
    }
    public List<Rule> list(String project,String merchant) {
        p.requireScope(requireText(project,"project_id"),merchant,VIEW);
        return p.jdbc.query("SELECT * FROM bar_payroll_rules WHERE project_id=? AND (merchant_id IS NULL OR merchant_id=?) ORDER BY created_at DESC,id",
            this::row,project,merchant).stream().filter(r->p.inScope(r.projectId(),r.merchantId(),VIEW)).toList();
    }
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability="bar.payroll.manage")
    public Rule draft(DraftRule r) {
        if(r==null)throw invalid("须填写规则");
        p.requireScope(requireText(r.projectId(),"project_id"),r.merchantId(),MANAGE);
        if(!Set.of("charge","allocation").contains(r.ruleKind()) || r.effectiveFrom()==null)throw invalid("须明确规则类型和生效日期");
        if("charge".equals(r.ruleKind()) && r.merchantId()!=null)throw invalid("服务费收费比例按综合体配置");
        if("allocation".equals(r.ruleKind()) && r.merchantId()==null)throw invalid("分配规则须明确商户");
        requireText(r.reason(),"reason");validate(r.ruleKind(),r.values(),r.projectId(),r.merchantId());
        String company="allocation".equals(r.ruleKind())?p.operationCompany(r.projectId(),r.merchantId()):requireText(r.platformCompanyId(),"platform_company_id");
        p.requireCompanyProject(company,r.projectId());
        if(r.platformCompanyId()!=null && !company.equals(r.platformCompanyId()))throw invalid("平台主管公司须与商户当前经营主体一致");
        return idem.execute("barPayrollRuleDraft",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            p.lock("rule:"+r.projectId()+":"+r.merchantId()+":"+r.ruleKind());
            int seq=p.jdbc.queryForObject("SELECT count(*)+1 FROM bar_payroll_rules WHERE project_id=? AND rule_kind=? AND merchant_id IS NOT DISTINCT FROM ?",Integer.class,r.projectId(),r.ruleKind(),r.merchantId());
            String id=id("bpr");
            p.jdbc.update("INSERT INTO bar_payroll_rules(id,rule_kind,project_id,merchant_id,effective_from,version,rule_json,reason,status,platform_company_id,created_by) VALUES(?,?,?,?,?,?,?::jsonb,?,'draft',?,?)",
                id,r.ruleKind(),r.projectId(),r.merchantId(),r.effectiveFrom(),"V"+seq/100+"."+String.format("%02d",seq%100),
                p.encode(snapshot(r)),r.reason(),company,p.auth.currentActor().id());
            event("drafted",id,r.idempotencyKey());return get(id,false);
        },Rule.class);
    }
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability="bar.payroll.manage")
    public Rule confirm(String id,ConfirmRule r) {
        var original=get(id,false);p.requireScope(original.projectId(),original.merchantId(),MANAGE);
        if(r==null || !Set.of("merchant","platform").contains(r.party()))throw invalid("须明确确认方");
        var actor=p.auth.currentActor();
        if("merchant".equals(r.party())) {
            if(original.merchantId()==null || !Objects.equals(actor.merchantId(),original.merchantId()))
                throw ApiException.forbidden("BAR_PAYROLL_PARTY_MISMATCH","商户确认须由本商户获授权人员本人办理");
        } else {
            var profile=p.staff.findProfileByUserId(actor.id()).orElseThrow(()->invalid("平台主管确认人须有真实员工档案"));
            if(actor.merchantId()!=null || !Objects.equals(profile.companyId(),original.platformCompanyId()))
                throw ApiException.forbidden("BAR_PAYROLL_PARTY_MISMATCH","平台主管确认须由所属经营公司获授权人员本人办理");
        }
        return idem.execute("barPayrollRuleConfirm:"+id,requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            var current=get(id,true);draftOnly(current);
            String field="merchant".equals(r.party())?"merchant":"platform";
            p.jdbc.update("UPDATE bar_payroll_rules SET "+field+"_confirmed_by=?,"+field+"_confirmed_at=clock_timestamp() WHERE id=?",
                actor.id(),id);
            event("confirmed_"+r.party(),id,r.idempotencyKey());return get(id,false);
        },Rule.class);
    }
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability="bar.payroll.manage")
    public Rule submit(String id,SubmitRule r) {
        var original=get(id,false);p.requireScope(original.projectId(),original.merchantId(),MANAGE);
        if(r==null)throw invalid("须填写提交信息");
        return idem.execute("barPayrollRuleSubmit:"+id,requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            var current=get(id,true);draftOnly(current);confirmed(current);verifyEvidence(current,r.evidenceAttachmentIds());
            var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",current.reason(),
                r.evidenceAttachmentIds(),ACTION,OBJECT,id,0L,p.encode(current),r.finalApproverId(),r.parallelLines(),null));
            if(!"pending_review".equals(review.reviewStatus()))throw conflict("BAR_PAYROLL_APPROVAL_REQUIRED","规则必须进入已配置审批流程，不能直接生效");
            p.jdbc.update("UPDATE bar_payroll_rules SET status='pending_review',review_id=?,evidence_attachment_ids=?::jsonb WHERE id=?",
                review.reviewId(),p.encode(r.evidenceAttachmentIds()),id);
            event("submitted",id,r.idempotencyKey());return get(id,false);
        },Rule.class);
    }
    @EventListener @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if(!ACTION.equals(d.actionType()) || !OBJECT.equals(d.objectType()) || !Set.of("approved","rejected").contains(d.reviewStatus()))return;
        var current=get(d.objectId(),true);
        if(!"pending_review".equals(current.status()) || !Objects.equals(current.reviewId(),d.reviewId())
                || p.jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE review_id=? AND action_type=? AND object_type=? AND object_id=? AND review_status=?",
                    Integer.class,d.reviewId(),ACTION,OBJECT,d.objectId(),d.reviewStatus())!=1)return;
        if("approved".equals(d.reviewStatus())) {
            p.lock("rule:"+current.projectId()+":"+current.merchantId()+":"+current.ruleKind());
            confirmed(current);verifyEvidence(current,current.evidenceAttachmentIds());
            if(p.jdbc.queryForObject("SELECT count(*) FROM bar_payroll_rules WHERE project_id=? AND merchant_id IS NOT DISTINCT FROM ? AND rule_kind=? AND effective_from=? AND status='approved'",
                    Integer.class,current.projectId(),current.merchantId(),current.ruleKind(),current.effectiveFrom())>0)
                throw conflict("BAR_PAYROLL_RULE_DATE_CONFLICT","该范围同日已有生效版本");
            if(p.jdbc.queryForObject("SELECT count(*) FROM bar_payroll_runs WHERE project_id=? AND (?::text IS NULL OR merchant_id=?) AND month>=?",
                    Integer.class,current.projectId(),current.merchantId(),current.merchantId(),current.effectiveFrom().toString().substring(0,7))>0)
                throw conflict("BAR_PAYROLL_RULE_PERIOD_LOCKED","已有月结原件的期间不得倒改分配规则");
        }
        p.jdbc.update("UPDATE bar_payroll_rules SET status=?,decided_at=clock_timestamp() WHERE id=?",d.reviewStatus(),current.id());
        event("decided_"+d.reviewStatus(),current.id(),null);
    }
    Rule effective(String kind,String project,String merchant,LocalDate date) {
        return p.jdbc.query("SELECT * FROM bar_payroll_rules WHERE rule_kind=? AND project_id=? AND merchant_id IS NOT DISTINCT FROM ? AND status='approved' AND effective_from<=? ORDER BY effective_from DESC,id DESC LIMIT 1",
            this::row,kind,project,"charge".equals(kind)?null:merchant,date).stream().findFirst()
            .orElseThrow(()->conflict("BAR_PAYROLL_RULE_REQUIRED","缺少该营业日已批准的"+("charge".equals(kind)?"收费":"分配")+"规则"));
    }
    Rule get(String id,boolean lock) {
        return p.jdbc.query("SELECT * FROM bar_payroll_rules WHERE id=?"+(lock?" FOR UPDATE":""),this::row,id).stream().findFirst()
            .orElseThrow(()->ApiException.notFound("BAR_PAYROLL_RULE_NOT_FOUND","服务收入规则不存在"));
    }
    public String requireEvidenceScope(String type,String id,String permission) {
        if(!OBJECT.equals(type))throw invalid("不支持的服务收入证据类型");
        p.auth.requirePermission(permission);var r=get(id,false);
        if("evidence.timeline.view".equals(permission)) {
            if(!currentReviewer(r))p.requireScope(r.projectId(),r.merchantId(),VIEW);
        } else {p.requireScope(r.projectId(),r.merchantId(),MANAGE);draftOnly(r);}
        if(!p.inScope(r.projectId(),r.merchantId(),permission))throw ApiException.forbidden("BAR_PAYROLL_EVIDENCE_SCOPE","原件不在证据授权范围");
        return r.merchantId();
    }
    private boolean currentReviewer(Rule r) {
        if(r.reviewId()==null || !p.auth.currentActorHasPermission("governance.supervisor_review.approve"))return false;
        var review=reviews.findReview(r.reviewId()).orElse(null);
        if(review==null || !"pending_review".equals(review.reviewStatus()) || !ACTION.equals(review.actionType())
                || !OBJECT.equals(review.objectType()) || !r.id().equals(review.objectId()))return false;
        var actor=p.auth.currentActor();
        return ApprovalChainService.currentNodes(reviews.chain(r.reviewId())).stream().anyMatch(node->
            actor.id().equals(node.forwardedTo()) || (ApprovalChainNode.KIND_USER.equals(node.approverKind()) && actor.id().equals(node.approverId()))
            || (ApprovalChainNode.KIND_MASTER.equals(node.approverKind()) && p.auth.isMasterAuthority(actor)));
    }
    private void verifyEvidence(Rule r,List<String> ids) {
        if(ids==null || ids.isEmpty())throw invalid("须上传收费或分配约定原件");
        for(String id:ids) {
            var a=evidence.findAttachmentById(id).orElseThrow(()->invalid("约定附件不存在"));
            if(!OBJECT.equals(a.businessType()) || !r.id().equals(a.businessId()) || !Objects.equals(r.merchantId(),a.merchantId()))throw invalid("附件必须绑定本草案及所属商户");
            attachments.requireUploaded(a);
        }
    }
    private void confirmed(Rule r) {
        if(r.platformConfirmedBy()==null || ("allocation".equals(r.ruleKind()) && r.merchantConfirmedBy()==null))
            throw conflict("BAR_PAYROLL_PARTY_CONFIRMATION_REQUIRED","收费须平台主管确认；分配须商户与所属娱乐平台分别确认");
        if("allocation".equals(r.ruleKind()) && !r.platformCompanyId().equals(p.operationCompany(r.projectId(),r.merchantId())))
            throw conflict("BAR_PAYROLL_PARTY_CHANGED","商户经营主体已变化，请重新起草并确认");
    }
    private RuleValues snapshot(DraftRule r) {
        var v=r.values();
        if("charge".equals(r.ruleKind()))return v;
        return new RuleValues(v.rateBps(),v.minimumRateBps(),v.maximumRateBps(),v.withheldBps(),v.minimumWithheldBps(),v.maximumWithheldBps(),
            v.participants().stream().map(person->new Participant(person.userId(),person.shareUnits(),person.partialCoverMethod(),
                p.requireWorker(person.userId(),r.projectId(),r.merchantId()))).toList());
    }
    private void validate(String kind,RuleValues v,String project,String merchant) {
        if(v==null)throw invalid("规则数值不能为空");
        if("charge".equals(kind))bounds(v.rateBps(),v.minimumRateBps(),v.maximumRateBps());
        else {
            bounds(v.withheldBps(),v.minimumWithheldBps(),v.maximumWithheldBps());
            if(v.participants()==null || v.participants().isEmpty())throw invalid("须明确分配人员和计份方式");
            Set<String> users=new HashSet<>();
            for(var participant:v.participants()) {
                if(participant==null || !users.add(participant.userId()) || participant.shareUnits()<=0
                        || !Set.of("full_shift_share","actual_shift_fraction").contains(participant.partialCoverMethod()))
                    throw invalid("每人须有唯一账号、正计份数及明确的部分顶班折算规则");
                p.requireWorker(participant.userId(),project,merchant);
            }
        }
    }
    private void bounds(Integer value,Integer min,Integer max) {
        if(value==null || min==null || max==null || min<0 || max>10000 || min>max || value<min || value>max)
            throw invalid("比例及上下限均须明确填写，范围为 0 至 10000 基点");
    }
    private void draftOnly(Rule r) {if(!"draft".equals(r.status()))throw conflict("BAR_PAYROLL_DRAFT_REQUIRED","仅草案可确认或提交，已提交原件不可改写");}
    private void event(String event,String id,String key) {audit.recordSuccess("bar.payroll_rule."+event,MANAGE,OBJECT,id,null,key,Map.of());}
    private Rule row(ResultSet rs,int n)throws SQLException {
        return new Rule(rs.getString("id"),rs.getString("rule_kind"),rs.getString("project_id"),rs.getString("merchant_id"),
            rs.getDate("effective_from").toLocalDate(),rs.getString("version"),p.decode(rs.getString("rule_json"),RuleValues.class),
            rs.getString("platform_company_id"),rs.getString("merchant_confirmed_by"),rs.getString("platform_confirmed_by"),
            rs.getString("review_id"),rs.getString("status"),rs.getString("reason"),p.strings(rs.getString("evidence_attachment_ids")),rs.getTimestamp("created_at").toInstant());
    }
}
