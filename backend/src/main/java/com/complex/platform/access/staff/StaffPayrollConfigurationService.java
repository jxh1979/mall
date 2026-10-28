package com.complex.platform.access.staff;

// 文件版本：V0.02

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.access.*;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.*;
import com.complex.platform.governance.chain.*;
import com.complex.platform.governance.review.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffPayrollConfigurationService {
    static final String MANAGE="staff.payroll.manage";
    static final String SCHEME="staff_payroll_scheme",CALENDAR="staff_payroll_calendar",PERIOD="staff_payroll_period";
    static final String SCHEME_ACTION="staff.payroll.scheme.change",CALENDAR_ACTION="staff.payroll.calendar.publish",
        PERIOD_ACTION="staff.payroll.period.confirm",SCORE_ACTION="staff.performance.points.deduct",APPEAL_ACTION="staff.performance.points.appeal";
    final JdbcTemplate jdbc;
    final ObjectMapper json;
    final AuthorizationService auth;
    final AccessRepository access;
    final JdbcStaffRepository staff;
    final IdempotencyService idem;
    final SupervisorReviewService reviews;
    final EvidenceEventRepository evidence;
    final EvidenceAttachmentValidator attachments;
    final AuditEventService audit;
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationEventPublisher events;
    public StaffPayrollConfigurationService(JdbcTemplate jdbc,ObjectMapper json,AuthorizationService auth,AccessRepository access,
            JdbcStaffRepository staff,IdempotencyService idem,SupervisorReviewService reviews,EvidenceEventRepository evidence,
            EvidenceAttachmentValidator attachments,AuditEventService audit) {
        this.jdbc=jdbc;this.json=json;this.auth=auth;this.access=access;this.staff=staff;this.idem=idem;this.reviews=reviews;
        this.evidence=evidence;this.attachments=attachments;this.audit=audit;
    }
    record Scope(AccessActor actor,AuthorizationService.ProjectReach reach) {}
    Scope scope(String permission) {auth.requirePermission(permission);var actor=auth.currentActor();return new Scope(actor,auth.projectReach(actor,permission));}
    void requireScope(Scope scope,String project,String merchant,String payer) {
        if(!scope.reach().covers(project) || (payer!=null&&!scope.reach().covers(payer))
            || (scope.actor().merchantId()!=null&&!Objects.equals(scope.actor().merchantId(),merchant)))
            throw ApiException.forbidden("PAYROLL_SCOPE_MISMATCH","无权处理该项目、商户或发薪主体的工资");
    }
    StaffProfileRecord employee(Scope scope,String user) {
        var p=staff.findProfileByUserId(requireText(user,"user_id")).orElseThrow(()->ApiException.notFound("STAFF_NOT_FOUND","员工档案不存在"));
        if(p.projectId()==null||p.companyId()==null)throw invalid("须先明确员工项目与工资发放主体");
        requireScope(scope,p.projectId(),p.merchantId(),p.companyId());return p;
    }
    void lockEmployee(String user) {jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))","staff-payroll:"+user);}

    public List<Scheme> schemes(String user) {
        var scope=scope(MANAGE);requireText(user,"user_id");
        return jdbc.query("SELECT * FROM staff_payroll_scheme_versions WHERE user_id=? ORDER BY effective_from DESC,created_at DESC",this::schemeRow,user)
            .stream().filter(s->visible(scope,s.projectId(),s.merchantId(),s.payerCompanyId())).toList();
    }
    @com.complex.platform.gate.GuardedAction(permission=MANAGE,capability=MANAGE)
    @Transactional public Scheme draftScheme(SchemeDraftRequest r) {
        var scope=scope(MANAGE);
        if(r==null)throw invalid("须填写薪资方案");var p=employee(scope,r.userId());validateScheme(r);
        return idem.execute("payrollSchemeDraft",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            lockEmployee(r.userId());String id=id("psc");
            int seq=jdbc.queryForObject("SELECT count(*)+1 FROM staff_payroll_scheme_versions WHERE user_id=?",Integer.class,r.userId());
            jdbc.update("INSERT INTO staff_payroll_scheme_versions(id,user_id,project_id,merchant_id,payer_company_id,version,effective_from,effective_to,position_code,components,performance_rules,worktime_terms,payment_basis,reason,created_by) VALUES(?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,?::jsonb,?,?,?)",
                id,r.userId(),p.projectId(),p.merchantId(),p.companyId(),version(seq),r.effectiveFrom(),r.effectiveTo(),r.positionCode(),
                encode(r.components()),encode(r.performanceRules()),encode(r.worktimeTerms()),r.paymentBasis().trim(),r.reason().trim(),scope.actor().id());
            success("scheme_drafted",SCHEME,id,r.idempotencyKey());return scheme(id);
        },Scheme.class);
    }
    @com.complex.platform.gate.GuardedAction(permission=MANAGE,capability=MANAGE)
    @Transactional public Scheme submitScheme(String id,SubmitRequest r) {
        var scope=scope(MANAGE);var before=scheme(id);requireScope(scope,before.projectId(),before.merchantId(),before.payerCompanyId());
        return idem.execute("payrollSchemeSubmit:"+id,key(r),r,()->{
            lockEmployee(before.userId());var row=scheme(id);draft(row.status());requireApprovedScoreRule(row);verifyAttachments(SCHEME,id,r.evidenceAttachmentIds());
            var review=submit(r,SCHEME_ACTION,SCHEME,id,row.userId(),row.reason(),encode(row));
            jdbc.update("UPDATE staff_payroll_scheme_versions SET status='pending_review',review_id=?,evidence_attachment_ids=?::jsonb WHERE id=?",review.reviewId(),encode(r.evidenceAttachmentIds()),id);
            success("scheme_submitted",SCHEME,id,r.idempotencyKey());return scheme(id);
        },Scheme.class);
    }
    public List<CalendarVersion> calendars(String project) {
        var scope=scope(MANAGE);requireScope(scope,requireText(project,"project_id"),null,null);
        return jdbc.query("SELECT * FROM staff_payroll_calendar_versions WHERE project_id=? ORDER BY start_date DESC,created_at DESC",this::calendarRow,project);
    }
    @com.complex.platform.gate.GuardedAction(permission=MANAGE,capability=MANAGE)
    @Transactional public CalendarVersion draftCalendar(CalendarDraftRequest r) {
        var scope=scope(MANAGE);if(r==null)throw invalid("须填写工资日历");
        requireScope(scope,r.projectId(),null,null);
        if(r.startDate()==null||r.endDate()==null||r.endDate().isBefore(r.startDate()))throw invalid("须填写完整日历期间");
        requireText(r.sourceBasis(),"source_basis");requireText(r.reason(),"reason");
        Map<LocalDate,String> days=calendarMap(r.days());
        for(LocalDate date=r.startDate();!date.isAfter(r.endDate());date=date.plusDays(1))if(!days.containsKey(date))throw invalid("日历缺少日期："+date);
        if(days.size()!=java.time.temporal.ChronoUnit.DAYS.between(r.startDate(),r.endDate())+1)throw invalid("日历不得包含声明期间以外日期");
        return idem.execute("payrollCalendarDraft",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))","payroll-calendar:"+r.projectId());
            int seq=jdbc.queryForObject("SELECT count(*)+1 FROM staff_payroll_calendar_versions WHERE project_id=?",Integer.class,r.projectId());
            String id=id("pca");
            jdbc.update("INSERT INTO staff_payroll_calendar_versions(id,project_id,version,start_date,end_date,days,source_basis,reason,created_by) VALUES(?,?,?,?,?,?::jsonb,?,?,?)",
                id,r.projectId(),version(seq),r.startDate(),r.endDate(),encode(r.days()),r.sourceBasis().trim(),r.reason().trim(),scope.actor().id());
            success("calendar_drafted",CALENDAR,id,r.idempotencyKey());return calendar(id);
        },CalendarVersion.class);
    }
    @com.complex.platform.gate.GuardedAction(permission=MANAGE,capability=MANAGE)
    @Transactional public CalendarVersion submitCalendar(String id,SubmitRequest r) {
        var scope=scope(MANAGE);var row=calendar(id);requireScope(scope,row.projectId(),null,null);
        return idem.execute("payrollCalendarSubmit:"+id,key(r),r,()->{
            jdbc.queryForList("SELECT id FROM staff_payroll_calendar_versions WHERE id=? FOR UPDATE",id);
            var current=calendar(id);draft(current.status());verifyAttachments(CALENDAR,id,r.evidenceAttachmentIds());
            var review=submit(r,CALENDAR_ACTION,CALENDAR,id,null,current.reason(),encode(current));
            jdbc.update("UPDATE staff_payroll_calendar_versions SET status='pending_review',review_id=?,evidence_attachment_ids=?::jsonb WHERE id=?",review.reviewId(),encode(r.evidenceAttachmentIds()),id);
            return calendar(id);
        },CalendarVersion.class);
    }
    public List<PeriodFacts> periods(String user,String month) {
        var scope=scope(MANAGE);requireText(user,"user_id");month(month);
        return jdbc.query("SELECT * FROM staff_payroll_period_facts WHERE user_id=? AND month=? ORDER BY created_at DESC",this::periodRow,user,month)
            .stream().filter(s->visible(scope,s.projectId(),s.merchantId(),s.payerCompanyId())).toList();
    }
    @com.complex.platform.gate.GuardedAction(permission=MANAGE,capability=MANAGE)
    @Transactional public PeriodFacts draftPeriod(PeriodDraftRequest r) {
        var scope=scope(MANAGE);if(r==null)throw invalid("须填写计薪依据");requireText(r.userId(),"user_id");month(r.month());
        var scheme=scheme(requireText(r.schemeVersionId(),"scheme_version_id"));requireSchemeFor(scheme,r.userId(),r.month());
        requireScope(scope,scheme.projectId(),scheme.merchantId(),scheme.payerCompanyId());
        var calendar=calendar(requireText(r.calendarVersionId(),"calendar_version_id"));
        if(!"approved".equals(calendar.status())||!scheme.projectId().equals(calendar.projectId()))throw invalid("须选择本项目已批准的工资日历");
        if(r.cycleStart()==null||r.cycleEnd()==null||r.cycleEnd().isBefore(r.cycleStart())||r.cycleStandardMinutes()<0||r.agreedDueDate()==null
            ||r.cycleStart().isAfter(month(r.month()).atEndOfMonth())||r.cycleEnd().isBefore(month(r.month()).atDay(1)))
            throw invalid("须明确实际核定周期、标准工时和约定支付日");
        requireText(r.reason(),"reason");
        if(r.componentFacts()==null||r.restCompensatedMinutes()==null)throw invalid("须填写计薪项目依据及已批准调休事实");
        Set<String> codes=new HashSet<>();
        for(var fact:r.componentFacts()) {
            if(!codes.add(fact.componentCode())||scheme.components().stream().noneMatch(c->c.code().equals(fact.componentCode())))
                throw invalid("计薪项目重复或不在方案内");
            if(fact.embeddedLeaveCent()<0)throw invalid("事假已扣金额不能为负");
            requireText(fact.evidenceBasis(),"evidence_basis");
        }
        StaffPayrollCalculator.dueDate(r.agreedDueDate(),r.statutoryDueDate(),calendarMap(calendar.days()));
        return idem.execute("payrollPeriodDraft",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            lockEmployee(r.userId());String id=id("ppf");
            jdbc.update("INSERT INTO staff_payroll_period_facts(id,user_id,month,project_id,merchant_id,payer_company_id,scheme_version_id,calendar_version_id,component_facts,performance_facts,cycle_start,cycle_end,cycle_standard_minutes,rest_compensated_minutes,agreed_due_date,statutory_due_date,reason,created_by) VALUES(?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,?,?,?,?::jsonb,?,?,?,?)",
                id,r.userId(),r.month(),scheme.projectId(),scheme.merchantId(),scheme.payerCompanyId(),scheme.id(),calendar.id(),
                encode(r.componentFacts()),encode(r.performanceFacts()),r.cycleStart(),r.cycleEnd(),r.cycleStandardMinutes(),encode(r.restCompensatedMinutes()),
                r.agreedDueDate(),r.statutoryDueDate(),r.reason().trim(),scope.actor().id());
            success("period_drafted",PERIOD,id,r.idempotencyKey());return period(id);
        },PeriodFacts.class);
    }
    @com.complex.platform.gate.GuardedAction(permission=MANAGE,capability=MANAGE)
    @Transactional public PeriodFacts submitPeriod(String id,SubmitRequest r) {
        var scope=scope(MANAGE);var row=period(id);requireScope(scope,row.projectId(),row.merchantId(),row.payerCompanyId());
        return idem.execute("payrollPeriodSubmit:"+id,key(r),r,()->{
            lockEmployee(row.userId());var current=period(id);draft(current.status());verifyAttachments(PERIOD,id,r.evidenceAttachmentIds());
            var review=submit(r,PERIOD_ACTION,PERIOD,id,current.userId(),current.reason(),encode(current));
            jdbc.update("UPDATE staff_payroll_period_facts SET status='pending_review',review_id=?,evidence_attachment_ids=?::jsonb WHERE id=?",review.reviewId(),encode(r.evidenceAttachmentIds()),id);
            return period(id);
        },PeriodFacts.class);
    }
    public List<ScoreRuleVersion> scoreRules(String project,String merchant,String owner) {
        var scope=scope(MANAGE);requireScope(scope,project,merchant,owner);
        return jdbc.query("SELECT * FROM staff_performance_rule_versions WHERE project_id=? AND merchant_id IS NOT DISTINCT FROM ? AND owner_company_id=? ORDER BY effective_from DESC,created_at DESC",
            this::scoreRuleRow,project,merchant,owner);
    }
    @com.complex.platform.gate.GuardedAction(permission=MANAGE,capability=MANAGE)
    @Transactional public ScoreRuleVersion draftScoreRule(ScoreRuleDraftRequest r) {
        var scope=scope(MANAGE);if(r==null)throw invalid("须填写经营主体扣分分值表");
        requireScope(scope,r.projectId(),r.merchantId(),requireText(r.ownerCompanyId(),"owner_company_id"));
        if(!r.ownerCompanyId().equals(businessOwner(r.merchantId(),r.ownerCompanyId())))throw invalid("分值表经营主体须与商户真实所属一致");
        if(r.effectiveFrom()==null||r.effectiveFrom().getDayOfMonth()!=1||r.deductions()==null||r.deductions().isEmpty()
            ||r.deductions().entrySet().stream().anyMatch(e->e.getKey()==null||e.getKey().isBlank()||e.getValue()==null||e.getValue().signum()<=0))
            throw invalid("须填写未来计分周期和各事项正数扣分");
        requireText(r.reason(),"reason");
        return idem.execute("payrollScoreRuleDraft",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))","performance-rule:"+r.projectId()+":"+r.merchantId()+":"+r.ownerCompanyId());
            int seq=jdbc.queryForObject("SELECT count(*)+1 FROM staff_performance_rule_versions WHERE project_id=? AND merchant_id IS NOT DISTINCT FROM ? AND owner_company_id=?",Integer.class,r.projectId(),r.merchantId(),r.ownerCompanyId());
            if(seq>1&&!r.effectiveFrom().isAfter(LocalDate.now(com.complex.platform.common.LocalZone.ID).withDayOfMonth(1)))
                throw invalid("扣分分值变更只适用于未来计分周期");
            String id=id("psr");
            jdbc.update("INSERT INTO staff_performance_rule_versions(id,project_id,merchant_id,owner_company_id,version,effective_from,deductions,reason,created_by) VALUES(?,?,?,?,?,?,?::jsonb,?,?)",
                id,r.projectId(),r.merchantId(),r.ownerCompanyId(),version(seq),r.effectiveFrom(),encode(r.deductions()),r.reason(),scope.actor().id());
            return scoreRule(id);
        },ScoreRuleVersion.class);
    }
    @com.complex.platform.gate.GuardedAction(permission=MANAGE,capability=MANAGE)
    @Transactional public ScoreRuleVersion submitScoreRule(String id,SubmitRequest r) {
        var scope=scope(MANAGE);var row=scoreRule(id);requireScope(scope,row.projectId(),row.merchantId(),row.ownerCompanyId());
        return idem.execute("payrollScoreRuleSubmit:"+id,key(r),r,()->{
            jdbc.queryForList("SELECT id FROM staff_performance_rule_versions WHERE id=? FOR UPDATE",id);
            var current=scoreRule(id);draft(current.status());verifyAttachments("staff_performance_rule",id,r.evidenceAttachmentIds());
            var review=submit(r,"staff.performance.rule.publish","staff_performance_rule",id,null,current.reason(),encode(current));
            jdbc.update("UPDATE staff_performance_rule_versions SET status='pending_review',review_id=?,evidence_attachment_ids=?::jsonb WHERE id=?",review.reviewId(),encode(r.evidenceAttachmentIds()),id);
            return scoreRule(id);
        },ScoreRuleVersion.class);
    }
    ScoreRuleVersion scoreRule(String id){return jdbc.query("SELECT * FROM staff_performance_rule_versions WHERE id=?",this::scoreRuleRow,id).stream().findFirst().orElseThrow(()->missing("扣分分值表"));}
    private void requireApprovedScoreRule(Scheme scheme) {
        var rules=scheme.performanceRules();
        if(rules==null||rules.deductionRules()==null||rules.deductionRules().isEmpty())return;
        var rows=jdbc.query("SELECT * FROM staff_performance_rule_versions WHERE review_id=? AND status='approved'",this::scoreRuleRow,rules.deductionRuleReviewId());
        if(rows.size()!=1)throw invalid("扣分规则须引用真实总经理批准的分值表");
        var approved=rows.getFirst();
        if(!scheme.projectId().equals(approved.projectId())||!Objects.equals(scheme.merchantId(),approved.merchantId())
            ||!businessOwner(scheme.merchantId(),scheme.payerCompanyId()).equals(approved.ownerCompanyId())||approved.effectiveFrom().isAfter(scheme.effectiveFrom())
            ||!rules.deductionRules().equals(approved.deductions()))throw invalid("扣分分值或经营主体不符合原批准分值表");
    }
    public List<ScoreAdjustment> myScores(String month) {
        auth.requirePermission("staff.payroll.view_mine");month(month);
        return jdbc.query("SELECT * FROM staff_performance_score_adjustments WHERE user_id=? AND month=? ORDER BY created_at,id",
            this::scoreRow,auth.currentActor().id(),month);
    }
    String businessOwner(String merchant,String payer) {
        if(merchant==null)return payer;
        return jdbc.query("SELECT business_entity_id FROM merchants WHERE id=?",(rs,n)->rs.getString(1),merchant).stream()
            .filter(Objects::nonNull).findFirst().orElseThrow(()->invalid("须先明确商户真实经营主体"));
    }
    public List<ScoreAdjustment> scores(String user,String month) {
        var scope=scope(MANAGE);requireText(user,"user_id");month(month);
        return jdbc.query("SELECT a.*,s.payer_company_id FROM staff_performance_score_adjustments a JOIN staff_payroll_scheme_versions s ON s.id=a.scheme_version_id WHERE a.user_id=? AND a.month=? ORDER BY a.created_at,a.id",
            (rs,n)->visible(scope,rs.getString("project_id"),rs.getString("merchant_id"),rs.getString("payer_company_id"))?scoreRow(rs,n):null,user,month).stream().filter(Objects::nonNull).toList();
    }
    @com.complex.platform.gate.GuardedAction(permission=MANAGE,capability=MANAGE)
    @Transactional public ScoreAdjustment deductScore(ScoreRequest r) {
        var scope=scope(MANAGE);if(r==null)throw invalid("须填写绩效扣分");var p=employee(scope,r.userId());month(r.month());
        requireText(r.reason(),"reason");
        var scheme=effectiveScheme(r.userId(),r.month());requireScope(scope,scheme.projectId(),scheme.merchantId(),scheme.payerCompanyId());var rules=scheme.performanceRules();
        if(rules==null||rules.deductionRules()==null||!rules.deductionRules().containsKey(r.ruleCode()))
            throw invalid("须选择已批准分值表内的违规事项");
        BigDecimal points=rules.deductionRules().get(r.ruleCode());
        if(points==null||points.signum()<=0)throw invalid("违规事项扣分必须大于零");
        return idem.execute("payrollScoreDeduct",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            lockEmployee(r.userId());String id=id("psd");
            var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",r.reason(),List.of(),
                SCORE_ACTION,"staff_performance_score",id,0L,"绩效扣分 "+r.ruleCode()+"："+points+" 分；"+r.reason(),null,List.of(),r.userId()));
            if(!"pending_review".equals(review.reviewStatus()))throw invalid("绩效扣分必须进入人力复核");
            jdbc.update("INSERT INTO staff_performance_score_adjustments(id,user_id,month,project_id,merchant_id,points,rule_code,scheme_version_id,reason,review_id,status,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,'pending_review',?)",
                id,r.userId(),r.month(),scheme.projectId(),scheme.merchantId(),points,r.ruleCode(),scheme.id(),r.reason(),review.reviewId(),scope.actor().id());
            return score(id);
        },ScoreAdjustment.class);
    }
    @com.complex.platform.gate.GuardedAction(permission="staff.payroll.view_mine",capability="staff.payroll.view_mine")
    @Transactional public ScoreAdjustment appealScore(String id,com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest r) {
        var scope=scope("staff.payroll.view_mine");var original=score(id);
        if(!scope.actor().id().equals(original.userId()))throw ApiException.forbidden("APPEAL_SELF_ONLY","只能申诉本人的绩效扣分");
        requireText(r.reason(),"reason");
        return idem.execute("payrollScoreAppeal:"+id,requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            lockEmployee(original.userId());
            if(!"approved".equals(original.status())||original.reversalOf()!=null)throw invalid("只能申诉已生效的原扣分");
            if(jdbc.queryForObject("SELECT count(*) FROM staff_performance_score_adjustments WHERE reversal_of=? AND status IN ('pending_review','approved')",Integer.class,id)>0)
                throw ApiException.conflict("APPEAL_ALREADY_EXISTS","该扣分已有待裁定或已撤销申诉");
            String next=id("psa");
            var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",r.reason(),List.of(),
                APPEAL_ACTION,"staff_performance_score",next,0L,"申诉绩效扣分："+id+"；"+r.reason(),null,List.of(),original.userId()));
            jdbc.update("INSERT INTO staff_performance_score_adjustments(id,user_id,month,project_id,merchant_id,points,rule_code,scheme_version_id,reason,review_id,status,reversal_of,created_by) SELECT ?,user_id,month,project_id,merchant_id,points,rule_code,scheme_version_id,?,?,'pending_review',id,? FROM staff_performance_score_adjustments WHERE id=?",
                next,r.reason(),review.reviewId(),scope.actor().id(),id);
            return score(next);
        },ScoreAdjustment.class);
    }
    @EventListener @Transactional public void afterDecision(SupervisorReviewDecisionRecord d) {
        String table=switch(d.objectType()==null?"":d.objectType()) {
            case SCHEME -> "staff_payroll_scheme_versions";case CALENDAR -> "staff_payroll_calendar_versions";
            case PERIOD -> "staff_payroll_period_facts";case "staff_performance_rule" -> "staff_performance_rule_versions";case "staff_performance_score" -> "staff_performance_score_adjustments";default -> null;};
        if(table==null||!Set.of("approved","rejected").contains(d.reviewStatus()))return;
        String action=switch(d.objectType()){case SCHEME->SCHEME_ACTION;case CALENDAR->CALENDAR_ACTION;case PERIOD->PERIOD_ACTION;case "staff_performance_rule"->"staff.performance.rule.publish";default->d.actionType();};
        if(!action.equals(d.actionType())||("staff_performance_score".equals(d.objectType())&&!Set.of(SCORE_ACTION,APPEAL_ACTION).contains(d.actionType())))return;
        if(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE review_id=? AND object_id=? AND object_type=? AND action_type=? AND review_status=?",Integer.class,d.reviewId(),d.objectId(),d.objectType(),d.actionType(),d.reviewStatus())!=1)return;
        if("approved".equals(d.reviewStatus())&&"staff_performance_rule".equals(d.objectType())) {
            var row=scoreRule(d.objectId());
            var approvers=reviews.chain(d.reviewId()).stream().filter(n->ApprovalChainNode.MAIN_LINE.equals(n.lineCode())
                &&ApprovalChainNode.APPROVED.equals(n.status())).map(ApprovalChainNode::decidedBy).filter(Objects::nonNull).toList();
            if(approvers.isEmpty())throw invalid("扣分分值表缺少实际总经理审批节点");
            for(String user:approvers) {
                var account=access.findUserById(user).orElseThrow(()->invalid("批准人账号不存在"));
                if(!auth.holdsPermissionViaRole(user,"staff.performance.rule.approve"))throw invalid("批准人须持经营主体总经理的正式扣分规则审批授权");
                var actor=new AccessActor(account.id(),account.username(),account.accountType(),account.organizationId(),account.merchantId(),account.status());
                requireScope(new Scope(actor,auth.projectReach(actor,"staff.performance.rule.approve")),row.projectId(),row.merchantId(),row.ownerCompanyId());
            }
        }
        if(!CALENDAR.equals(d.objectType())&&!"staff_performance_rule".equals(d.objectType())) {
            String user=jdbc.query("SELECT user_id FROM "+table+" WHERE id=?",(rs,n)->rs.getString(1),d.objectId()).stream().findFirst().orElse(null);
            if(user==null)return;lockEmployee(user);
        }
        var rows=jdbc.queryForList("SELECT * FROM "+table+" WHERE id=? AND review_id=? AND status='pending_review' FOR UPDATE",d.objectId(),d.reviewId());
        if(rows.isEmpty())return;
        if("approved".equals(d.reviewStatus())&&SCHEME.equals(d.objectType())) {
            var row=scheme(d.objectId());
            if(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_scheme_versions WHERE user_id=? AND status='approved' AND id<>? AND effective_from=?",Integer.class,row.userId(),row.id(),row.effectiveFrom())>0)
                throw ApiException.conflict("PAYROLL_SCHEME_OVERLAP","同一生效日只能有一份已批准薪资方案");
            if(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statements WHERE user_id=? AND month>=?",Integer.class,row.userId(),YearMonth.from(row.effectiveFrom()).toString())>0)
                throw ApiException.conflict("PAYROLL_SCHEME_LOCKED_PERIOD","方案不能追溯影响已定格工资；须使用后续周期");
            requireApprovedScoreRule(row);
        }
        jdbc.update("UPDATE "+table+" SET status=?,approved_at=CASE WHEN ?='approved' THEN clock_timestamp() ELSE NULL END WHERE id=?",d.reviewStatus(),d.reviewStatus(),d.objectId());
        if("staff_performance_score".equals(d.objectType())&&"approved".equals(d.reviewStatus())) {
            var row=score(d.objectId());
            if(row.reversalOf()!=null)events.publishEvent(new StaffPayrollService.CorrectionRequested(row.userId(),row.month(),d.reviewId(),row.reversalOf()));
        }
        success("configuration_decided",d.objectType(),d.objectId(),d.reviewId());
    }

    void requireSchemeFor(Scheme scheme,String user,String month) {
        var ym=month(month);
        if(!"approved".equals(scheme.status())||!scheme.userId().equals(user)||scheme.effectiveFrom().isAfter(ym.atDay(1))
                ||(scheme.effectiveTo()!=null&&scheme.effectiveTo().isBefore(ym.atEndOfMonth())))
            throw invalid("须选择完整覆盖本期且已批准的员工薪资方案");
        if(!effectiveScheme(user,month).id().equals(scheme.id()))throw invalid("本期须使用实际生效的最新批准方案，历史工资仍保留原版本");
    }
    /** 岗位权限流程只读取批准薪资来源，不能另建一套任填金额。调用者已完成岗位范围核验。 */
    public Long approvedPositionSalary(String user,String position,Long requested) {
        LocalDate today=LocalDate.now(com.complex.platform.common.LocalZone.ID);
        var rows=jdbc.query("SELECT * FROM staff_payroll_scheme_versions WHERE user_id=? AND status='approved' AND effective_from<=? AND (effective_to IS NULL OR effective_to>=?) ORDER BY effective_from DESC LIMIT 1",this::schemeRow,user,today,today);
        Long salary=null;
        if(!rows.isEmpty()) {
            var scheme=rows.getFirst();
            if(position==null||position.equals(scheme.positionCode())) {
                var components=scheme.components().stream().filter(i->"base".equals(i.type())).toList();
                if(components.stream().allMatch(i->"fixed_monthly".equals(i.method())))salary=components.stream().mapToLong(Component::amountCent).sum();
            }
        }
        if(requested!=null&&!Objects.equals(requested,salary))throw ApiException.conflict("PAYROLL_SCHEME_REQUIRED","定岗薪资必须来自同岗位已批准生效方案；请先办理方案审批");
        return salary;
    }
    Scheme effectiveScheme(String user,String month) {
        var ym=month(month);
        var rows=jdbc.query("SELECT * FROM staff_payroll_scheme_versions WHERE user_id=? AND status='approved' AND effective_from<=? AND (effective_to IS NULL OR effective_to>=?) ORDER BY effective_from DESC",this::schemeRow,user,ym.atDay(1),ym.atEndOfMonth());
        if(rows.isEmpty())throw StaffPayrollCalculator.pending("本期没有已批准薪资方案");return rows.getFirst();
    }
    Scheme scheme(String id){return jdbc.query("SELECT * FROM staff_payroll_scheme_versions WHERE id=?",this::schemeRow,id).stream().findFirst().orElseThrow(()->missing("薪资方案"));}
    PeriodFacts period(String id){return jdbc.query("SELECT * FROM staff_payroll_period_facts WHERE id=?",this::periodRow,id).stream().findFirst().orElseThrow(()->missing("计薪依据"));}
    CalendarVersion calendar(String id){return jdbc.query("SELECT * FROM staff_payroll_calendar_versions WHERE id=?",this::calendarRow,id).stream().findFirst().orElseThrow(()->missing("工资日历"));}
    ScoreAdjustment score(String id){return jdbc.query("SELECT * FROM staff_performance_score_adjustments WHERE id=?",this::scoreRow,id).stream().findFirst().orElseThrow(()->missing("绩效扣分"));}
    public String requireEvidenceScope(String type,String id,String permission) {
        auth.requirePermission(permission);
        var actor=auth.currentActor();
        String project,merchant,payer,review;
        if(SCHEME.equals(type)){var r=scheme(id);project=r.projectId();merchant=r.merchantId();payer=r.payerCompanyId();review=r.reviewId();}
        else if(PERIOD.equals(type)){var r=period(id);project=r.projectId();merchant=r.merchantId();payer=r.payerCompanyId();review=r.reviewId();}
        else if(CALENDAR.equals(type)){var r=calendar(id);project=r.projectId();merchant=null;payer=null;review=r.reviewId();}
        else if("staff_performance_rule".equals(type)){var r=scoreRule(id);project=r.projectId();merchant=r.merchantId();payer=r.ownerCompanyId();review=r.reviewId();}
        else throw missing("工资依据");
        boolean reading="evidence.timeline.view".equals(permission);
        if(reading&&currentReviewer(type,id,review)) {
            var reach=auth.projectReach(actor,"governance.supervisor_review.approve");
            requireScope(new Scope(actor,reach),project,merchant,payer);
            requireScope(scope(permission),project,merchant,payer);return merchant;
        }
        // 第十八批 E 段 20-R4 扫尾：查看凭据与方案等查看同口径（visible，商户账号只按商户）；上传仍走 requireScope
        if(reading) {
            if(!visible(scope(MANAGE),project,merchant,payer)||!visible(scope(permission),project,merchant,payer))
                throw ApiException.forbidden("PAYROLL_SCOPE_MISMATCH","无权处理该项目、商户或发薪主体的工资");
            return merchant;
        }
        requireScope(scope(MANAGE),project,merchant,payer);
        requireScope(scope(permission),project,merchant,payer);
        return merchant;
    }
    boolean currentReviewer(String type,String id,String reviewId) {
        if(reviewId==null||!auth.currentActorHasPermission("governance.supervisor_review.approve"))return false;
        var review=reviews.findReview(reviewId).orElse(null);
        if(review==null||!"pending_review".equals(review.reviewStatus())||!type.equals(review.objectType())||!id.equals(review.objectId()))return false;
        var actor=auth.currentActor();
        return ApprovalChainService.currentNodes(reviews.chain(reviewId)).stream().anyMatch(n->actor.id().equals(n.forwardedTo())
            || (ApprovalChainNode.KIND_USER.equals(n.approverKind())&&actor.id().equals(n.approverId()))
            || (ApprovalChainNode.KIND_MASTER.equals(n.approverKind())&&auth.isMasterAuthority(actor)));
    }
    private SupervisorReviewDecisionRecord submit(SubmitRequest r,String action,String type,String id,String beneficiary,String reason,String summary) {
        var review=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",reason,r.evidenceAttachmentIds(),action,type,id,0L,
            summary,null,r.parallelLines()==null?List.of():r.parallelLines(),beneficiary));
        if(!"pending_review".equals(review.reviewStatus()))throw invalid("工资配置须完成规定复核后生效");return review;
    }
    void verifyAttachments(String type,String id,List<String> ids) {
        if(ids==null||ids.isEmpty())throw invalid("须上传已核实的原始依据");
        for(String file:ids){var a=evidence.findAttachmentById(file).orElseThrow(()->invalid("依据附件不存在"));
            if(!type.equals(a.businessType())||!id.equals(a.businessId()))throw invalid("依据原件必须绑定当前草案");attachments.requireUploaded(a);}
    }
    private void validateScheme(SchemeDraftRequest r) {
        if(r.effectiveFrom()==null||r.effectiveFrom().getDayOfMonth()!=1||r.effectiveTo()!=null&&r.effectiveTo().isBefore(r.effectiveFrom()))
            throw invalid("薪资方案须从完整计薪月首日起适用");
        requireText(r.positionCode(),"position_code");requireText(r.paymentBasis(),"payment_basis");requireText(r.reason(),"reason");
        if(r.components()==null||r.components().isEmpty()||r.components().stream().noneMatch(c->"base".equals(c.type())))throw invalid("须逐项明确基础工资及适用薪资项目");
        Set<String> codes=new HashSet<>();
        for(var c:r.components()) {
            requireText(c.code(),"component_code");requireText(c.label(),"label");requireText(c.calculationBasis(),"calculation_basis");
            if(!codes.add(c.code())||c.amountCent()<0||!Set.of("base","allowance","full_attendance","performance","bonus").contains(c.type())
                ||!Set.of("fixed_monthly","per_unit","attendance_ratio","performance").contains(c.method())
                ||!Set.of("proportional","exempt").contains(c.leaveMode())||!Set.of("month","day","hour","shift","unit").contains(c.unit()))
                throw invalid("薪资项目的类别、公式或单位不合法");
            if("performance".equals(c.type())!= "performance".equals(c.method()))throw invalid("绩效项目须使用批准的分数和档次公式");
        }
        var w=r.worktimeTerms();
        if(w==null||!Set.of("standard","comprehensive","flexible").contains(w.system())||w.hourlyBaseCent()<0
            ||w.standardDailyMinutes()<0||w.standardWeeklyMinutes()<0||w.ordinaryOvertimeBps()<0||w.restDayOvertimeBps()<0||w.holidayOvertimeBps()<0)
            throw invalid("须完整填写依法确认的工时制度和计薪依据");
        requireText(w.approvalBasis(),"approval_basis");requireText(w.calculationBasis(),"calculation_basis");
        if(r.components().stream().anyMatch(c->"performance".equals(c.type()))) {
            var p=r.performanceRules();
            if(p==null||p.basePoints()==null||p.minPoints()==null||p.maxPoints()==null||p.basePoints().signum()<=0
                ||p.minPoints().signum()<0||p.basePoints().compareTo(p.minPoints())<0||p.basePoints().compareTo(p.maxPoints())>0
                ||p.gradeCoefficients()==null||!p.gradeCoefficients().keySet().equals(Set.of("A","B","C"))
                ||p.gradeCoefficients().values().stream().anyMatch(v->v==null||v.signum()<0)
                ||p.gradeCoefficients().get("B").compareTo(BigDecimal.ONE)!=0||p.cMissedPeriods()<1||p.assessmentPeriodMonths()<1)
                throw invalid("须填写完整绩效基础分、上下限、三档系数和评定周期；B档倍率为1");
            requireText(p.aCondition(),"a_condition");
            if(p.deductionRules()!=null&&!p.deductionRules().isEmpty())requireText(p.deductionRuleReviewId(),"deduction_rule_review_id");
        }
    }
    static Map<LocalDate,String> calendarMap(List<CalendarDay> days) {
        if(days==null)throw invalid("须填写逐日性质");
        Map<LocalDate,String> result=new TreeMap<>();
        for(var day:days)if(day.date()==null||!Set.of("workday","adjusted_workday","rest_day","statutory_holiday").contains(day.kind())||result.put(day.date(),day.kind())!=null)
            throw invalid("日历日期重复或日期性质不合法");
        return result;
    }
    static YearMonth month(String value){try{return YearMonth.parse(requireText(value,"month"));}catch(java.time.DateTimeException e){throw invalid("月份须为年月格式");}}
    static String key(SubmitRequest r){if(r==null)throw invalid("须填写提交内容");return requireText(r.idempotencyKey(),"idempotency_key");}
    static void draft(String status){if(!"draft".equals(status))throw ApiException.conflict("PAYROLL_DRAFT_REQUIRED","仅草稿可提交，原已审批版本保留");}
    /** 只用于读（方案、期间事实、评分调整、试算、工资单可见）。第十八批 E 段 20-R3：商户账号只按商户，看得到各经营项目里本商户的；写入口用 requireScope。 */
    boolean visible(Scope s,String p,String m,String c){
        if(!s.reach().platformAdmin()&&s.actor().merchantId()!=null)return Objects.equals(s.actor().merchantId(),m);
        return s.reach().covers(p)&&(c==null||s.reach().covers(c))&&(s.actor().merchantId()==null||Objects.equals(s.actor().merchantId(),m));}
    static String id(String prefix){return prefix+"_"+UUID.randomUUID().toString().replace("-","");}
    static String version(int seq){return "V"+(seq/100)+"."+String.format("%02d",seq%100);}
    String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw invalid("工资依据无法保存");}}
    <T>T decode(String value,Class<T> type){try{return json.readValue(value,type);}catch(Exception e){throw invalid("工资依据无法读取");}}
    <T>T decode(String value,TypeReference<T> type){try{return json.readValue(value,type);}catch(Exception e){throw invalid("工资依据无法读取");}}
    void success(String event,String type,String id,String key){audit.recordSuccess("staff.payroll."+event,MANAGE,type,id,null,key,Map.of());}
    static ApiException invalid(String text){return ApiException.badRequest("PAYROLL_INPUT_INVALID",text);}
    static ApiException missing(String text){return ApiException.notFound("PAYROLL_RECORD_NOT_FOUND",text+"不存在");}
    private Scheme schemeRow(ResultSet r,int n)throws SQLException{return new Scheme(r.getString("id"),r.getString("user_id"),r.getString("project_id"),r.getString("merchant_id"),r.getString("payer_company_id"),r.getString("version"),
        r.getObject("effective_from",LocalDate.class),r.getObject("effective_to",LocalDate.class),r.getString("position_code"),decode(r.getString("components"),new TypeReference<List<Component>>(){}),
        decode(r.getString("performance_rules"),PerformanceRules.class),decode(r.getString("worktime_terms"),WorkTimeTerms.class),r.getString("payment_basis"),r.getString("reason"),r.getString("status"),r.getString("review_id"),
        decode(r.getString("evidence_attachment_ids"),new TypeReference<List<String>>(){}),r.getString("created_by"),r.getTimestamp("created_at").toInstant());}
    private CalendarVersion calendarRow(ResultSet r,int n)throws SQLException{return new CalendarVersion(r.getString("id"),r.getString("project_id"),r.getString("version"),
        r.getObject("start_date",LocalDate.class),r.getObject("end_date",LocalDate.class),decode(r.getString("days"),new TypeReference<List<CalendarDay>>(){}),r.getString("source_basis"),r.getString("reason"),r.getString("status"),r.getString("review_id"),
        decode(r.getString("evidence_attachment_ids"),new TypeReference<List<String>>(){}),r.getString("created_by"),r.getTimestamp("created_at").toInstant());}
    private PeriodFacts periodRow(ResultSet r,int n)throws SQLException{return new PeriodFacts(r.getString("id"),r.getString("user_id"),r.getString("month"),r.getString("project_id"),r.getString("merchant_id"),r.getString("payer_company_id"),
        r.getString("scheme_version_id"),r.getString("calendar_version_id"),decode(r.getString("component_facts"),new TypeReference<List<ComponentFact>>(){}),decode(r.getString("performance_facts"),PerformanceFact.class),
        r.getObject("cycle_start",LocalDate.class),r.getObject("cycle_end",LocalDate.class),r.getLong("cycle_standard_minutes"),decode(r.getString("rest_compensated_minutes"),new TypeReference<Map<LocalDate,Long>>(){}),
        r.getObject("agreed_due_date",LocalDate.class),r.getObject("statutory_due_date",LocalDate.class),r.getString("reason"),r.getString("status"),r.getString("review_id"),decode(r.getString("evidence_attachment_ids"),new TypeReference<List<String>>(){}),
        r.getString("created_by"),r.getTimestamp("created_at").toInstant());}
    private ScoreRuleVersion scoreRuleRow(ResultSet r,int n)throws SQLException{return new ScoreRuleVersion(r.getString("id"),r.getString("project_id"),r.getString("merchant_id"),r.getString("owner_company_id"),r.getString("version"),
        r.getObject("effective_from",LocalDate.class),decode(r.getString("deductions"),new TypeReference<Map<String,BigDecimal>>(){}),r.getString("reason"),r.getString("status"),r.getString("review_id"),
        decode(r.getString("evidence_attachment_ids"),new TypeReference<List<String>>(){}),r.getString("created_by"),r.getTimestamp("created_at").toInstant());}
    private ScoreAdjustment scoreRow(ResultSet r,int n)throws SQLException{return new ScoreAdjustment(r.getString("id"),r.getString("user_id"),r.getString("month"),r.getBigDecimal("points"),r.getString("rule_code"),r.getString("reason"),r.getString("review_id"),r.getString("status"),r.getString("reversal_of"),r.getTimestamp("created_at").toInstant());}
}
