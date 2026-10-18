package com.complex.platform.access.staff;

// 文件版本：V0.02

import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.complex.platform.governance.chain.ApprovalChainNode;
import com.complex.platform.governance.chain.ApprovalChainService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 仅治理现有分档公式及其真实税务认定，不替代专业财税软件或正式纳税申报。 */
@Service
public class PayrollTaxService {
    public static final String MANAGE="staff.tax_bracket.manage";
    public static final String VIEW="staff.payroll.manage";
    public static final String ACTION="finance.tax_rule.adjust";
    public static final String VERSION="payroll_tax_version";
    public static final String ASSESSMENT="payroll_tax_assessment";
    public static final String ROUNDING="truncate_cent_each_step";
    private final JdbcTemplate jdbc;
    private final JdbcStaffRepository staff;
    private final AccessRepository access;
    private final AuthorizationService auth;
    private final SupervisorReviewService reviews;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator attachments;
    private final ObjectMapper json;

    public PayrollTaxService(JdbcTemplate jdbc,JdbcStaffRepository staff,AccessRepository access,
            AuthorizationService auth,SupervisorReviewService reviews,IdempotencyService idempotency,
            AuditEventService audit,EvidenceEventRepository evidence,EvidenceAttachmentValidator attachments,ObjectMapper json) {
        this.jdbc=jdbc;this.staff=staff;this.access=access;this.auth=auth;this.reviews=reviews;
        this.idempotency=idempotency;this.audit=audit;this.evidence=evidence;this.attachments=attachments;this.json=json;
    }

    public List<PayrollTaxVersion> versions(String category) {
        try {
            requireRead();
            var rows=jdbc.query("SELECT * FROM payroll_tax_versions WHERE (?::text IS NULL OR category=?) ORDER BY category,effective_from DESC,created_at DESC,id",
                    this::versionRow,blank(category),blank(category));
            success("versions_viewed",VERSION,null,null,Map.of("count",rows.size()));return rows;
        }catch(RuntimeException e){failure("versions_view_failed",VERSION,null,e);throw e;}
    }

    @Transactional
    public PayrollTaxVersion draft(PayrollTaxVersion.DraftRequest r) {
        try {
            requireGlobalManage();
            if(r==null || r.effectiveFrom()==null)throw invalid("须填写税表生效日期");
            String category=requireText(r.category(),"category");
            if(category.length()>64)throw invalid("税目编码不能超过64字符");
            requireText(r.applicabilityBasis(),"applicability_basis");requireText(r.calculationBasis(),"calculation_basis");
            requireText(r.reason(),"reason");
            if(!ROUNDING.equals(r.roundingRule()))throw invalid("当前分档公式仅支持经专业审核的逐步截断到分口径，其他算法须由专业财税系统核定");
            List<PayrollTaxVersion.Bracket> brackets=validateBrackets(r.brackets());
            return idempotency.execute("payrollTaxVersionDraft",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
                lock("category:"+category);
                int seq=jdbc.queryForObject("SELECT count(*)+1 FROM payroll_tax_versions WHERE category=?",Integer.class,category);
                String id=id("ptv");
                jdbc.update("INSERT INTO payroll_tax_versions(id,category,version,effective_from,applicability_basis,calculation_basis,rounding_rule,brackets,reason,status,created_by,created_at) VALUES(?,?,?,?,?,?,?,?::jsonb,?,'draft',?,clock_timestamp())",
                    id,category,"V"+(seq/100)+"."+String.format("%02d",seq%100),r.effectiveFrom(),r.applicabilityBasis().trim(),
                    r.calculationBasis().trim(),r.roundingRule(),encode(brackets),r.reason().trim(),auth.currentActor().id());
                success("version_drafted",VERSION,id,r.idempotencyKey(),Map.of("category",category));return version(id,false);
            },PayrollTaxVersion.class);
        }catch(RuntimeException e){failure("version_draft_failed",VERSION,null,e);throw e;}
    }

    @Transactional
    public PayrollTaxVersion submitVersion(String id,PayrollTaxVersion.SubmitRequest r) {
        try {
            requireGlobalManage();requireSubmit(r);
            return idempotency.execute("payrollTaxVersionSubmit:"+id,r.idempotencyKey(),r,()->{
                var row=version(id,true);requireDraft(row.status());verifyEvidence(VERSION,id,r.evidenceAttachmentIds());
                String before=jdbc.query("SELECT id,version FROM payroll_tax_versions WHERE category=? AND status='approved' ORDER BY effective_from DESC LIMIT 1",
                        (rs,n)->rs.getString("version")+"（"+rs.getString("id")+"）",row.category()).stream().findFirst().orElse("尚无已批准版本");
                var review=submitReview(r,VERSION,id,row.reason(),"原版："+before+"；完整新版本："+encode(row),null);
                jdbc.update("UPDATE payroll_tax_versions SET status='pending_review',review_id=?,evidence_attachment_ids=?::jsonb WHERE id=?",review.reviewId(),encode(r.evidenceAttachmentIds()),id);
                success("version_submitted",VERSION,id,r.idempotencyKey(),Map.of("review_id",review.reviewId()));return version(id,false);
            },PayrollTaxVersion.class);
        }catch(RuntimeException e){failure("version_submit_failed",VERSION,id,e);throw e;}
    }

    public List<PayrollTaxAssessment> assessments(String userId,String month) {
        try {
            requireRead();requireMonth(month);requireText(userId,"user_id");
            var originals=jdbc.query("SELECT * FROM payroll_tax_assessments WHERE user_id=? AND month=? ORDER BY created_at DESC,id DESC",this::assessmentRow,userId,month);
            var completePermissions=List.of(VIEW,MANAGE).stream().filter(auth::currentActorHasPermission).toList();
            List<String> legacyPermissions=List.of();
            if(originals.isEmpty()||originals.stream().anyMatch(row->row.payrollCalculationId()==null)) {
                try {legacyPermissions=employeeReadPermissions(userId);}
                catch(ApiException error) {
                    if(originals.stream().noneMatch(row->row.payrollCalculationId()!=null)||!"STAFF_SCOPE_MISMATCH".equals(error.code()))throw error;
                }
            }
            var currentEmployeePermissions=legacyPermissions;
            var rows=originals.stream().filter(row->(row.payrollCalculationId()==null?currentEmployeePermissions:completePermissions)
                .stream().anyMatch(permission->recordInScope(row,permission))).toList();
            success("assessments_viewed",ASSESSMENT,userId,null,Map.of("month",month,"count",rows.size()));return rows;
        }catch(RuntimeException e){failure("assessments_view_failed",ASSESSMENT,userId,e);throw e;}
    }

    @Transactional
    public PayrollTaxAssessment draftAssessment(PayrollTaxAssessment.DraftRequest r) {
        try {
            auth.requirePermission(MANAGE);
            if(r==null || r.taxDate()==null)throw invalid("须填写专业认定的税务适用日期");
            requireMonth(r.month());
            if(r.payrollCalculationId()!=null)return draftCompletePayrollAssessment(r);
            var profile=employee(requireText(r.userId(),"user_id"),MANAGE);
            requireText(r.category(),"category");requireText(r.identityBasis(),"identity_basis");
            requireText(r.transactionBasis(),"transaction_basis");requireText(r.professionalBasis(),"professional_basis");requireText(r.reason(),"reason");
            return idempotency.execute("payrollTaxAssessmentDraft",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
                lockEmployee(r.userId(),r.month());requireUnpaid(r.userId(),r.month());
                if(lockedAssessment(r.userId(),r.month())!=null)throw ApiException.conflict("PAYROLL_TAX_ASSESSMENT_LOCKED","工资已锁定原税务认定，不能另建认定覆盖原单");
                var lines=sources(r.userId(),r.month());if(lines.isEmpty())throw invalid("该员工本月没有待认定的服务收入台账");
                long income=total(lines);var version=versionForPayroll(r.userId(),r.month(),r.category().trim(),r.taxDate());
                var calculated=calculate(version,income);String id=id("pta");
                jdbc.update("INSERT INTO payroll_tax_assessments(id,user_id,month,project_id,merchant_id,payer_company_id,category,tax_date,identity_basis,transaction_basis,professional_basis,income_cent,source_lines,source_fingerprint,tax_version_id,tax_cent,calculation_snapshot,reason,status,created_by,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?::jsonb,?,'draft',?,clock_timestamp())",
                    id,r.userId(),r.month(),profile.projectId(),profile.merchantId(),profile.companyId(),version.category(),r.taxDate(),
                    r.identityBasis().trim(),r.transactionBasis().trim(),r.professionalBasis().trim(),income,encode(lines),fingerprint(lines),version.id(),
                    calculated.taxCent(),encode(calculated),r.reason().trim(),auth.currentActor().id());
                success("assessment_drafted",ASSESSMENT,id,r.idempotencyKey(),Map.of("user_id",r.userId(),"month",r.month(),"tax_version_id",version.id()));
                return assessment(id,false);
            },PayrollTaxAssessment.class);
        }catch(RuntimeException e){failure("assessment_draft_failed",ASSESSMENT,null,e);throw e;}
    }

    private PayrollTaxAssessment draftCompletePayrollAssessment(PayrollTaxAssessment.DraftRequest r) {
        var original=payrollCalculation(r.payrollCalculationId());
        var actor=auth.currentActor();var reach=auth.projectReach(actor,MANAGE);
        if(!reach.covers(original.projectId())||!reach.covers(original.payerCompanyId())
                ||actor.merchantId()!=null&&!Objects.equals(actor.merchantId(),original.merchantId()))
            throw ApiException.forbidden("STAFF_SCOPE_MISMATCH","无权认定该完整工资原件");
        if(!original.userId().equals(r.userId())||!original.month().equals(r.month())||r.professionalTaxCent()==null
                ||r.professionalTaxCent()<0||r.professionalTaxCent()>original.beforeTaxCent())
            throw invalid("专业税额须对应本员工本期完整原件，且不得超过税前可发金额");
        requireText(r.category(),"category");requireText(r.identityBasis(),"identity_basis");
        requireText(r.transactionBasis(),"transaction_basis");requireText(r.professionalBasis(),"professional_basis");
        requireText(r.reason(),"reason");requireText(r.professionalCalculationReference(),"professional_calculation_reference");
        return idempotency.execute("payrollCompleteTaxDraft",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            lockEmployee(original.userId(),original.month());var c=payrollCalculation(original.id());
            if(!"calculated".equals(c.status()))throw invalid("工资已定格，不得改写原税额");
            var v=effective(r.category(),r.taxDate());String id=id("pta");
            var lines=List.of(new PayrollTaxAssessment.SourceLine(c.id(),c.id(),c.beforeTaxCent()));
            var calculation=new PayrollTaxAssessment.Calculation(v.id(),v.version(),v.category(),v.effectiveFrom(),
                "professional_confirmed",r.professionalCalculationReference(),null,c.beforeTaxCent(),null,
                null,null,r.professionalTaxCent());
            jdbc.update("INSERT INTO payroll_tax_assessments(id,user_id,month,project_id,merchant_id,payer_company_id,category,tax_date,identity_basis,transaction_basis,professional_basis,income_cent,source_lines,source_fingerprint,tax_version_id,tax_cent,calculation_snapshot,reason,status,created_by,created_at,payroll_calculation_id,professional_calculation_reference) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?::jsonb,?,'draft',?,clock_timestamp(),?,?)",
                id,c.userId(),c.month(),c.projectId(),c.merchantId(),c.payerCompanyId(),v.category(),r.taxDate(),
                r.identityBasis(),r.transactionBasis(),r.professionalBasis(),c.beforeTaxCent(),encode(lines),
                c.sourceFingerprint(),v.id(),r.professionalTaxCent(),encode(calculation),r.reason(),actor.id(),c.id(),r.professionalCalculationReference());
            success("assessment_drafted",ASSESSMENT,id,r.idempotencyKey(),Map.of("payroll_calculation_id",c.id()));
            return assessment(id,false);
        },PayrollTaxAssessment.class);
    }
    private StaffPayrollModels.CalculationDraft payrollCalculation(String id) {
        return jdbc.query("SELECT * FROM staff_payroll_calculation_drafts WHERE id=?",(r,n)->
            new StaffPayrollModels.CalculationDraft(r.getString("id"),r.getString("user_id"),r.getString("month"),r.getString("statement_kind"),
                r.getString("origin_event_id"),r.getString("scheme_version_id"),r.getString("facts_id"),r.getString("project_id"),
                r.getString("merchant_id"),r.getString("payer_company_id"),r.getLong("before_tax_cent"),r.getString("source_fingerprint"),
                decode(r.getString("calculation_snapshot"),Map.class),r.getString("status"),r.getTimestamp("created_at").toInstant()),id)
            .stream().findFirst().orElseThrow(()->invalid("完整工资税前原件不存在"));
    }

    @Transactional
    public PayrollTaxAssessment submitAssessment(String id,PayrollTaxVersion.SubmitRequest r) {
        try {
            auth.requirePermission(MANAGE);requireSubmit(r);var original=assessment(id,false);
            if(original.payrollCalculationId()==null)employee(original.userId(),MANAGE);
            else {
                var actor=auth.currentActor();var reach=auth.projectReach(actor,MANAGE);
                if(!reach.covers(original.projectId())||!reach.covers(original.payerCompanyId())
                    ||actor.merchantId()!=null&&!Objects.equals(actor.merchantId(),original.merchantId()))
                    throw ApiException.forbidden("STAFF_SCOPE_MISMATCH","无权处理原完整工资的税务认定");
            }
            return idempotency.execute("payrollTaxAssessmentSubmit:"+id,r.idempotencyKey(),r,()->{
                lockEmployee(original.userId(),original.month());var row=assessment(id,true);requireDraft(row.status());validateCurrent(row);
                verifyEvidence(ASSESSMENT,id,r.evidenceAttachmentIds());
                var review=submitReview(r,ASSESSMENT,id,row.reason(),"既有服务收入专业税务认定（非正式申报）："+encode(row),row.userId());
                jdbc.update("UPDATE payroll_tax_assessments SET status='pending_review',review_id=?,evidence_attachment_ids=?::jsonb WHERE id=?",review.reviewId(),encode(r.evidenceAttachmentIds()),id);
                success("assessment_submitted",ASSESSMENT,id,r.idempotencyKey(),Map.of("review_id",review.reviewId()));return assessment(id,false);
            },PayrollTaxAssessment.class);
        }catch(RuntimeException e){failure("assessment_submit_failed",ASSESSMENT,id,e);throw e;}
    }

    @EventListener @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if(!ACTION.equals(d.actionType()) || !Set.of(VERSION,ASSESSMENT).contains(d.objectType()) || !Set.of("approved","rejected").contains(d.reviewStatus()))return;
        try {
            if(VERSION.equals(d.objectType())) {
                var row=version(d.objectId(),true);if(!matches(row.status(),row.reviewId(),d))return;
                if("approved".equals(d.reviewStatus())) {
                    lock("category:"+row.category());verifyEvidence(VERSION,row.id(),row.evidenceAttachmentIds());
                    if(jdbc.queryForObject("SELECT count(*) FROM payroll_tax_versions WHERE category=? AND effective_from=? AND status='approved'",Integer.class,row.category(),row.effectiveFrom())>0)
                        throw ApiException.conflict("TAX_EFFECTIVE_DATE_CONFLICT","该税目同一生效日已有批准版本，请重新起草");
                }
                jdbc.update("UPDATE payroll_tax_versions SET status=?,decided_at=clock_timestamp() WHERE id=?",d.reviewStatus(),row.id());
            } else {
                var original=assessment(d.objectId(),false);lockEmployee(original.userId(),original.month());
                var row=assessment(d.objectId(),true);if(!matches(row.status(),row.reviewId(),d))return;
                if("approved".equals(d.reviewStatus())) {validateCurrent(row);verifyEvidence(ASSESSMENT,row.id(),row.evidenceAttachmentIds());}
                jdbc.update("UPDATE payroll_tax_assessments SET status=?,decided_at=clock_timestamp() WHERE id=?",d.reviewStatus(),row.id());
            }
            success("decided",d.objectType(),d.objectId(),null,Map.of("review_id",d.reviewId(),"status",d.reviewStatus()));
        }catch(RuntimeException e){failure("apply_failed",d.objectType(),d.objectId(),e);throw e;}
    }

    /** 与源台账 trigger 共用员工/月锁；调用方月结事务须一直持有到工资写入完成。 */
    public void prepareMonth(String month,String project,String merchant,String company) {
        var users=jdbc.queryForList("""
                SELECT DISTINCT l.user_id FROM staff_commission_ledger l
                LEFT JOIN LATERAL (SELECT project_id,merchant_id,company_id FROM staff_profiles WHERE user_id=l.user_id ORDER BY created_at DESC LIMIT 1) p ON true
                WHERE l.month=? AND l.source_type='service_order'
                  AND (?::text IS NULL OR p.project_id=?) AND (?::text IS NULL OR p.merchant_id=?) AND (?::text IS NULL OR p.company_id=?)
                  AND NOT EXISTS(SELECT 1 FROM staff_payroll_statements s WHERE s.user_id=l.user_id AND s.month=l.month AND s.status='paid')
                ORDER BY l.user_id
                """,String.class,month,project,project,merchant,merchant,company,company);
        for(String user:users) {
            lockEmployee(user,month);var lines=sources(user,month);
            String lockedAssessment=lockedAssessment(user,month);
            var approved=jdbc.query("SELECT * FROM payroll_tax_assessments WHERE user_id=? AND month=? AND status='approved' AND (?::text IS NULL OR id=?) ORDER BY created_at DESC,id DESC LIMIT 1",this::assessmentRow,user,month,lockedAssessment,lockedAssessment);
            if(approved.isEmpty())throw ApiException.conflict("PAYROLL_TAX_ASSESSMENT_REQUIRED","服务收入尚无通过财税法及最高确认的税务认定："+user);
            var a=approved.getFirst();if(!a.sourceFingerprint().equals(fingerprint(lines)))throw ApiException.conflict("PAYROLL_TAX_SOURCE_CHANGED","服务收入明细已变更，须重新核定后再月结："+user);
            var profile=staff.findProfileByUserId(user).orElseThrow(()->invalid("缺少员工所属主体"));
            requireSamePayer(a,profile);
            String locked=lockedVersion(user,month);if(locked!=null && !locked.equals(a.taxVersionId()))throw ApiException.conflict("PAYROLL_TAX_VERSION_LOCKED","工资单已锁定另一税表版本");
        }
    }

    /** 附件必须挂到真实草案，审批读取须同时具有有效审批权及本单当前节点身份。 */
    public String requireEvidenceScope(String type,String id,String evidencePermission) {
        boolean reading="evidence.timeline.view".equals(evidencePermission);
        auth.requirePermission(evidencePermission);
        var actor=auth.currentActor();
        if(VERSION.equals(type)) {
            var row=version(id,false);
            if(reading) {if(!currentReviewer(row.reviewId(),VERSION,id))requireRead();}
            else {requireGlobalManage();requireDraft(row.status());}
            if(!auth.allowsPlatformScope(actor,evidencePermission))
                throw ApiException.forbidden("TAX_EVIDENCE_SCOPE_MISMATCH","全平台税表依据须具有平台范围的证据权限");
            return null;
        }
        if(!ASSESSMENT.equals(type))throw invalid("不支持的税务证据类型");
        var row=assessment(id,false);
        if(reading) {
            if(!currentReviewer(row.reviewId(),ASSESSMENT,id)) {
                var permissions=row.payrollCalculationId()==null?employeeReadPermissions(row.userId()):
                    List.of(VIEW,MANAGE).stream().filter(auth::currentActorHasPermission).toList();
                if(permissions.stream().noneMatch(permission->recordInScope(row,permission)))
                    throw ApiException.forbidden("STAFF_SCOPE_MISMATCH","无权处理原认定所属项目和商户的税务事实");
            }
        } else {
            auth.requirePermission(MANAGE);requireDraft(row.status());
            if(row.payrollCalculationId()==null)employee(row.userId(),MANAGE);
            requireRecordScope(row,MANAGE);
        }
        if(!auth.canReachProject(actor,row.projectId(),evidencePermission)
                || (row.payrollCalculationId()!=null&&!auth.canReachProject(actor,row.payerCompanyId(),evidencePermission))
                || (actor.merchantId()!=null && !Objects.equals(actor.merchantId(),row.merchantId())))
            throw ApiException.forbidden("TAX_EVIDENCE_SCOPE_MISMATCH","税务认定不在证据权限的实际项目和商户范围内");
        return row.merchantId();
    }

    private boolean recordInScope(PayrollTaxAssessment row,String permission) {
        var actor=auth.currentActor();
        return auth.canReachProject(actor,row.projectId(),permission)
                && (row.payrollCalculationId()==null || auth.canReachProject(actor,row.payerCompanyId(),permission))
                && (actor.merchantId()==null || Objects.equals(actor.merchantId(),row.merchantId()));
    }
    private void requireRecordScope(PayrollTaxAssessment row,String permission) {
        if(!recordInScope(row,permission))throw ApiException.forbidden("STAFF_SCOPE_MISMATCH","无权处理原认定所属项目和商户的税务事实");
    }

    private boolean currentReviewer(String reviewId,String type,String objectId) {
        if(reviewId==null || !auth.currentActorHasPermission("governance.supervisor_review.approve"))return false;
        try {
            var review=reviews.findReview(reviewId).orElse(null);
            if(review==null || !"pending_review".equals(review.reviewStatus()) || !ACTION.equals(review.actionType())
                    || !type.equals(review.objectType()) || !objectId.equals(review.objectId()))return false;
            var actor=auth.currentActor();
            return ApprovalChainService.currentNodes(reviews.chain(reviewId)).stream().anyMatch(node->
                    actor.id().equals(node.forwardedTo())
                    || (ApprovalChainNode.KIND_USER.equals(node.approverKind()) && actor.id().equals(node.approverId()))
                    || (ApprovalChainNode.KIND_MASTER.equals(node.approverKind()) && auth.isMasterAuthority(actor)));
        }catch(ApiException e){return false;}
    }
    private SupervisorReviewDecisionRecord submitReview(PayrollTaxVersion.SubmitRequest r,String type,String id,String reason,String summary,String beneficiary) {
        for(String code:List.of("finance_review","tax_review","legal_review")) {
            var entry=jdbc.query("SELECT entry_user_id FROM approval_lines WHERE line_code=? AND status='active'",(rs,n)->rs.getString(1),code);
            if(entry.isEmpty() || entry.getFirst()==null || entry.getFirst().isBlank())
                throw ApiException.conflict("TAX_DEPARTMENT_ROUTING_REQUIRED","须先配置财务、税务、法务真实部门审批起点："+code);
            var account=access.findUserById(entry.getFirst()).orElse(null);
            if(account==null || !"active".equals(account.status()) || !auth.holdsPermissionViaRole(account.id(),"governance.supervisor_review.approve"))
                throw ApiException.conflict("TAX_DEPARTMENT_ROUTING_REQUIRED","部门起点须为在用且具有正式审批角色的人员："+code);
        }
        var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",reason,r.evidenceAttachmentIds(),ACTION,type,id,0L,summary,null,r.parallelLines(),beneficiary));
        if(!"pending_review".equals(review.reviewStatus()))throw ApiException.conflict("TAX_APPROVAL_REQUIRED","税费事项必须进入财税法和最高确认审批");
        return review;
    }
    private void validateCurrent(PayrollTaxAssessment a) {
        if(a.payrollCalculationId()!=null) {
            var c=payrollCalculation(a.payrollCalculationId());
            if(!"calculated".equals(c.status())||!c.userId().equals(a.userId())||!c.sourceFingerprint().equals(a.sourceFingerprint())
                    ||!Objects.equals(c.payerCompanyId(),a.payerCompanyId())||!Objects.equals(c.projectId(),a.projectId())
                    ||!Objects.equals(c.merchantId(),a.merchantId())||c.beforeTaxCent()!=a.incomeCent())
                throw ApiException.conflict("PAYROLL_TAX_SOURCE_CHANGED","完整工资税前原件已定格或与税务认定不一致");
            if(!effective(a.category(),a.taxDate()).id().equals(a.taxVersionId()))
                throw ApiException.conflict("TAX_VERSION_CHANGED","税目适用版本变化，请重新提交专业认定");
            return;
        }
        requireUnpaid(a.userId(),a.month());
        String locked=lockedAssessment(a.userId(),a.month());
        if(locked!=null && !locked.equals(a.id()))throw ApiException.conflict("PAYROLL_TAX_ASSESSMENT_LOCKED","工资已锁定另一税务认定，不能覆盖原单");
        var profile=staff.findProfileByUserId(a.userId()).orElseThrow(()->invalid("员工档案不存在"));requireSamePayer(a,profile);
        if(!a.sourceFingerprint().equals(fingerprint(sources(a.userId(),a.month()))))throw ApiException.conflict("PAYROLL_TAX_SOURCE_CHANGED","原服务收入明细已变化，请重建认定草案");
        var version=versionForPayroll(a.userId(),a.month(),a.category(),a.taxDate());
        if(!version.id().equals(a.taxVersionId()))throw ApiException.conflict("TAX_VERSION_CHANGED","适用税表版本已变化，请重新起草认定");
    }
    private void requireSamePayer(PayrollTaxAssessment a,StaffProfileRecord p) {
        if(!Objects.equals(a.projectId(),p.projectId()) || !Objects.equals(a.merchantId(),p.merchantId()) || !Objects.equals(a.payerCompanyId(),p.companyId()))
            throw ApiException.conflict("PAYROLL_TAX_PAYER_CHANGED","员工所属项目、商户或发放主体已变化，须重新认定");
    }
    private PayrollTaxVersion versionForPayroll(String user,String month,String category,LocalDate date) {
        String locked=lockedVersion(user,month);
        if(locked!=null) {
            var v=version(locked,false);
            var existing=jdbc.query("SELECT a.tax_date FROM staff_payroll_statements s JOIN payroll_tax_assessments a ON a.id=s.tax_assessment_id WHERE s.user_id=? AND s.month=?",(rs,n)->rs.getDate(1).toLocalDate(),user,month);
            if(!v.category().equals(category) || v.effectiveFrom().isAfter(date) || (!existing.isEmpty() && !existing.getFirst().equals(date)))
                throw ApiException.conflict("PAYROLL_TAX_VERSION_LOCKED","原工资已锁定税目、适用日期和税表；不能改用新规则");
            return v;
        }
        return effective(category,date);
    }
    PayrollTaxVersion effective(String category,LocalDate date) {
        return jdbc.query("SELECT * FROM payroll_tax_versions WHERE category=? AND status='approved' AND effective_from<=? ORDER BY effective_from DESC LIMIT 1",this::versionRow,category,date)
                .stream().findFirst().orElseThrow(()->ApiException.conflict("PAYROLL_TAX_VERSION_REQUIRED","适用日期没有该税目的已批准税表"));
    }
    private String lockedAssessment(String user,String month) {return jdbc.query("SELECT tax_assessment_id FROM staff_payroll_statements WHERE user_id=? AND month=?",(rs,n)->rs.getString(1),user,month).stream().filter(Objects::nonNull).findFirst().orElse(null);}
    private String lockedVersion(String user,String month) {
        return jdbc.query("SELECT tax_version_id FROM staff_payroll_statements WHERE user_id=? AND month=?",(rs,n)->rs.getString(1),user,month).stream().filter(Objects::nonNull).findFirst().orElse(null);
    }
    private void requireUnpaid(String user,String month) {
        if(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statements WHERE user_id=? AND month=? AND status='paid'",Integer.class,user,month)>0)
            throw ApiException.conflict("PAYROLL_ALREADY_PAID","已发工资保留原税务认定，不在本入口回改");
    }
    private StaffProfileRecord employee(String user,String permission) {
        var p=staff.findProfileByUserId(requireText(user,"user_id")).orElseThrow(()->ApiException.notFound("STAFF_NOT_FOUND","员工档案不存在"));
        var u=access.findUserById(user).orElseThrow(()->ApiException.notFound("STAFF_NOT_FOUND","员工账号不存在"));
        if(!employeeInScope(p,u,permission))
            throw ApiException.forbidden("STAFF_SCOPE_MISMATCH","无权处理该员工的税务认定");
        if(p.projectId()==null || p.companyId()==null)throw invalid("须先明确员工实际项目和工资发放主体");return p;
    }
    private void requireGlobalManage(){auth.requirePermission(MANAGE);if(!auth.allowsPlatformScope(auth.currentActor(),MANAGE))throw ApiException.forbidden("TAX_PLATFORM_SCOPE_REQUIRED","全平台税表版本须具备平台范围的税档维护授权");}
    private boolean employeeInScope(StaffProfileRecord p,com.complex.platform.access.AccessUser u,String permission) {
        var actor=auth.currentActor();
        return auth.canReachUser(actor,u,permission) && auth.canReachProject(actor,p.projectId(),permission)
                && (actor.merchantId()==null || Objects.equals(actor.merchantId(),p.merchantId()));
    }
    private List<String> employeeReadPermissions(String user) {
        requireRead();
        var profile=staff.findProfileByUserId(requireText(user,"user_id")).orElseThrow(()->ApiException.notFound("STAFF_NOT_FOUND","员工档案不存在"));
        var account=access.findUserById(user).orElseThrow(()->ApiException.notFound("STAFF_NOT_FOUND","员工账号不存在"));
        var permissions=List.of(VIEW,MANAGE).stream().filter(auth::currentActorHasPermission)
                .filter(permission->employeeInScope(profile,account,permission)).toList();
        if(permissions.isEmpty())throw ApiException.forbidden("STAFF_SCOPE_MISMATCH","无权处理该员工的税务认定");
        if(profile.projectId()==null || profile.companyId()==null)throw invalid("须先明确员工实际项目和工资发放主体");
        return permissions;
    }
    private void requireRead(){auth.requirePermission(auth.currentActorHasPermission(VIEW)?VIEW:MANAGE);}
    private void verifyEvidence(String type,String id,List<String> ids) {
        if(ids==null || ids.isEmpty())throw invalid("须上传专业依据原件后提交审批");
        for(String file:ids) {
            var attachment=evidence.findAttachmentById(file).orElseThrow(()->invalid("专业依据附件不存在"));
            if(!type.equals(attachment.businessType()) || !id.equals(attachment.businessId()))throw invalid("专业依据必须绑定本份草案");
            attachments.requireUploaded(attachment);
        }
    }
    private boolean matches(String status,String reviewId,SupervisorReviewDecisionRecord d) {
        return "pending_review".equals(status) && Objects.equals(reviewId,d.reviewId())
            && jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE review_id=? AND action_type=? AND object_type=? AND object_id=? AND review_status=?",Integer.class,d.reviewId(),ACTION,d.objectType(),d.objectId(),d.reviewStatus())==1;
    }
    private void requireSubmit(PayrollTaxVersion.SubmitRequest r){if(r==null)throw invalid("须填写提交信息");requireText(r.idempotencyKey(),"idempotency_key");}
    private void requireDraft(String status){if(!"draft".equals(status))throw ApiException.conflict("TAX_DRAFT_REQUIRED","仅未提交草案可以提交，原件与终态不得改写");}
    private void requireMonth(String value){try{if(value==null || !value.matches("\\d{4}-\\d{2}"))throw new IllegalArgumentException();YearMonth.parse(value);}catch(RuntimeException e){throw invalid("月份须为有效的YYYY-MM");}}
    private List<PayrollTaxAssessment.SourceLine> sources(String user,String month){return jdbc.query("SELECT id,source_id,amount_cent FROM staff_commission_ledger WHERE user_id=? AND month=? AND source_type='service_order' ORDER BY id",(rs,n)->new PayrollTaxAssessment.SourceLine(rs.getString(1),rs.getString(2),rs.getLong(3)),user,month);}
    private long total(List<PayrollTaxAssessment.SourceLine> lines){long sum=0;for(var l:lines)sum=Math.addExact(sum,l.amountCent());if(sum<0)throw invalid("收入不能为负数");return sum;}
    private String fingerprint(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encode(value).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private void lock(String key){jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"payroll-tax:"+key);}
    void lockEmployee(String user,String month){jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))","staff-payroll:"+user);lock(user+":"+month);}

    static List<PayrollTaxVersion.Bracket> validateBrackets(List<PayrollTaxVersion.Bracket> input) {
        if(input==null || input.isEmpty() || input.stream().anyMatch(Objects::isNull))throw invalid("须提供完整税表档位");
        if(input.stream().anyMatch(b->b.seq()==null || b.incomeFloorCent()==null || b.incomeBaseBps()==null || b.flatDeductionCent()==null || b.rateBps()==null || b.quickDeductionCent()==null))throw invalid("每档全部金额、比例及序号均须明确填写，0也须显式填写");
        var rows=new ArrayList<>(input);rows.sort(Comparator.comparingInt(PayrollTaxVersion.Bracket::seq));long next=0;
        for(int i=0;i<rows.size();i++) {
            var b=rows.get(i);
            if(b.seq()!=i+1 || b.incomeFloorCent()!=next || b.incomeBaseBps()<1 || b.incomeBaseBps()>10000 || b.rateBps()<0 || b.rateBps()>10000 || b.flatDeductionCent()<0 || b.quickDeductionCent()<0)
                throw invalid("税表须按序号从0分连续覆盖，基数与费率使用合法万分比，减除不得为负");
            if(i==rows.size()-1) {if(b.incomeCeilingCent()!=null)throw invalid("最后一档须覆盖至无上限");}
            else {if(b.incomeCeilingCent()==null || b.incomeCeilingCent()<next || b.incomeCeilingCent()==Long.MAX_VALUE)throw invalid("税档不能重叠、缺口或提前无上限");next=b.incomeCeilingCent()+1;}
        }
        return List.copyOf(rows);
    }
    static PayrollTaxAssessment.Calculation calculate(PayrollTaxVersion v,long income) {
        var b=v.brackets().stream().filter(x->income>=x.incomeFloorCent() && (x.incomeCeilingCent()==null || income<=x.incomeCeilingCent())).findFirst().orElseThrow(()->invalid("税表未覆盖本次收入"));
        BigInteger unit=BigInteger.valueOf(10000),gross=BigInteger.valueOf(income);
        long base=gross.multiply(BigInteger.valueOf(b.incomeBaseBps())).divide(unit).longValueExact();
        long taxable=Math.max(0,Math.subtractExact(base,b.flatDeductionCent()));
        long before=BigInteger.valueOf(taxable).multiply(BigInteger.valueOf(b.rateBps())).divide(unit).longValueExact();
        long tax=Math.max(0,Math.subtractExact(before,b.quickDeductionCent()));
        return new PayrollTaxAssessment.Calculation(v.id(),v.version(),v.category(),v.effectiveFrom(),v.roundingRule(),v.calculationBasis(),b,income,base,taxable,before,tax);
    }
    PayrollTaxVersion version(String id,boolean lock){return jdbc.query("SELECT * FROM payroll_tax_versions WHERE id=?"+(lock?" FOR UPDATE":""),this::versionRow,id).stream().findFirst().orElseThrow(()->ApiException.notFound("TAX_VERSION_NOT_FOUND","税表版本不存在"));}
    PayrollTaxAssessment assessment(String id,boolean lock){return jdbc.query("SELECT * FROM payroll_tax_assessments WHERE id=?"+(lock?" FOR UPDATE":""),this::assessmentRow,id).stream().findFirst().orElseThrow(()->ApiException.notFound("TAX_ASSESSMENT_NOT_FOUND","税务认定不存在"));}
    private PayrollTaxVersion versionRow(ResultSet r,int n)throws SQLException{return new PayrollTaxVersion(r.getString("id"),r.getString("category"),r.getString("version"),r.getDate("effective_from").toLocalDate(),r.getString("applicability_basis"),r.getString("calculation_basis"),r.getString("rounding_rule"),list(r.getString("brackets"),PayrollTaxVersion.Bracket.class),r.getString("reason"),r.getString("status"),r.getString("review_id"),list(r.getString("evidence_attachment_ids"),String.class),r.getString("created_by"),instant(r,"created_at"),instant(r,"decided_at"));}
    private PayrollTaxAssessment assessmentRow(ResultSet r,int n)throws SQLException{return new PayrollTaxAssessment(r.getString("id"),r.getString("user_id"),r.getString("month"),r.getString("project_id"),r.getString("merchant_id"),r.getString("payer_company_id"),r.getString("category"),r.getDate("tax_date").toLocalDate(),r.getString("identity_basis"),r.getString("transaction_basis"),r.getString("professional_basis"),r.getLong("income_cent"),list(r.getString("source_lines"),PayrollTaxAssessment.SourceLine.class),r.getString("source_fingerprint"),r.getString("tax_version_id"),r.getLong("tax_cent"),decode(r.getString("calculation_snapshot"),PayrollTaxAssessment.Calculation.class),r.getString("reason"),r.getString("status"),r.getString("review_id"),list(r.getString("evidence_attachment_ids"),String.class),r.getString("created_by"),instant(r,"created_at"),instant(r,"decided_at"),r.getString("payroll_calculation_id"),r.getString("professional_calculation_reference"));}
    private <T> List<T> list(String text,Class<T> type){try{return json.readValue(text,json.getTypeFactory().constructCollectionType(List.class,type));}catch(Exception e){throw new IllegalStateException("税务原件无法解析",e);}}
    private <T> T decode(String text,Class<T> type){try{return json.readValue(text,type);}catch(Exception e){throw new IllegalStateException("税务快照无法解析",e);}}
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("税务原件无法保存",e);}}
    private static Instant instant(ResultSet r,String name)throws SQLException{Timestamp t=r.getTimestamp(name);return t==null?null:t.toInstant();}
    private static String id(String prefix){return prefix+"_"+UUID.randomUUID().toString().replace("-","");}
    private static String blank(String value){return value==null || value.isBlank()?null:value.trim();}
    private static ApiException invalid(String message){return ApiException.badRequest("VALIDATION_FAILED",message);}
    private void success(String name,String type,String id,String key,Map<String,Object> values){audit.recordSuccess("staff.payroll.tax."+name,MANAGE,type,id,null,key,values);}
    private void failure(String name,String type,String id,RuntimeException e){audit.recordFailure("staff.payroll.tax."+name,MANAGE,type,id,null,null,e instanceof ApiException api?api.code():"INTERNAL_ERROR");}
}
