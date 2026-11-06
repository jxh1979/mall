package com.complex.platform.access.staff;

// 文件版本：V0.04

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.bar.BarPayrollAllocationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.FinancePayrollService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 工资先保留完整税前原件，专业税额获批后定格；实际付款只能来自财务成功原件。 */
@Service
public class StaffPayrollService {
    private final StaffPayrollConfigurationService c;
    private final JdbcTemplate jdbc;
    private final StaffPayrollWorktimeService worktime;
    private final BarPayrollAllocationService bar;
    private final PayrollTaxService taxes;
    private final FinanceService finance;
    private final com.complex.platform.access.AuthorizationService auth;
    private final com.complex.platform.common.idempotency.IdempotencyService idem;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    // 第十八批第 1 条（ENUM:187「销售提成月份，一律以开台或下单时点所属的营业日为准」）：酒吧提成归属月按营业日。
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    com.complex.platform.common.BusinessDayResolver businessDays=new com.complex.platform.common.BusinessDayResolver();
    // 第十八批 V 段第 159 条 159-R7（ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；BSM:1500「正式提成使用该项计算所依赖的
    // 已最终确认数据，再统一进入工资单月结」；决策台账 §25 Q32「不要求无关五类数据全最终」）：酒吧销售提成按订单最终保留收入计（BSM:1070），依赖订单与核销、
    // 退款与冲正两类；服务点单份额完成时按售价减管理费写定、完成后不可退（ENUM:290），只依赖订单与核销一类。
    private static final List<String> BAR_COMMISSION_FINAL=List.of("orders_verifications","refunds_reversals");
    private static final List<String> SERVICE_SHARE_FINAL=List.of("orders_verifications");
    public StaffPayrollService(StaffPayrollConfigurationService c,JdbcTemplate jdbc,StaffPayrollWorktimeService worktime,
            BarPayrollAllocationService bar,PayrollTaxService taxes,FinanceService finance,
            com.complex.platform.access.AuthorizationService auth,com.complex.platform.common.idempotency.IdempotencyService idem,
            com.fasterxml.jackson.databind.ObjectMapper json) {
        this.c=c;this.jdbc=jdbc;this.worktime=worktime;this.bar=bar;this.taxes=taxes;this.finance=finance;
        this.auth=auth;this.idem=idem;this.json=json;
    }
    record Snapshot(Scheme scheme,PeriodFacts facts,CalendarVersion calendar,StaffPayrollCalculator.Earnings earnings,
            List<Line> lines,StaffPayrollWorktimeService.Facts worktime,BigDecimal points,String grade,
            List<Map<String,Object>> adjustments,List<ScoreAdjustment> scores,int capBps,List<PendingItem> pending,
            LocalDate dueDate,Map<String,Object> dueBasis,long beforeTaxCent) {}

    public List<CalculationDraft> calculations(String user,String month) {
        requireText(user,"user_id");c.month(month);
        var scopes=new ArrayList<StaffPayrollConfigurationService.Scope>();
        for(String permission:List.of(c.MANAGE,"staff.tax_bracket.manage"))
            if(auth.currentActorHasPermission(permission))scopes.add(c.scope(permission));
        if(scopes.isEmpty())auth.requirePermission(c.MANAGE);
        return jdbc.query("SELECT * FROM staff_payroll_calculation_drafts WHERE user_id=? AND month=? ORDER BY created_at DESC,id",this::calculationRow,user,month)
            .stream().filter(r->scopes.stream().anyMatch(scope->c.visible(scope,r.projectId(),r.merchantId(),r.payerCompanyId()))).toList();
    }
    /** 在分析查询分页前应用，始终按工资锁定主体核范围。 */
    public List<String> visibleStatementIds() {
        var scope=c.scope(c.MANAGE);
        if(scope.reach().platformAdmin()&&scope.actor().merchantId()==null)return null;
        return jdbc.query("SELECT id,project_id,merchant_id,payer_company_id FROM staff_payroll_statements",
            (rs,n)->c.visible(scope,rs.getString("project_id"),rs.getString("merchant_id"),rs.getString("payer_company_id"))?rs.getString("id"):null)
            .stream().filter(Objects::nonNull).toList();
    }
    @com.complex.platform.gate.GuardedAction(permission="staff.payroll.manage",capability="staff.payroll.manage")
    @Transactional public CalculationDraft calculate(SettleRequest r) {
        var scope=c.scope(c.MANAGE);validateRequest(r);var f=c.period(r.factsId());c.requireScope(scope,f.projectId(),f.merchantId(),f.payerCompanyId());
        return idem.execute("payrollCalculation",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            c.lockEmployee(r.userId());taxes.lockEmployee(r.userId(),r.month());validateFacts(r,f);
            Snapshot snap=build(r,f);String id=c.id("pcl");String fingerprint=fingerprint(snap);
            jdbc.update("INSERT INTO staff_payroll_calculation_drafts(id,user_id,month,statement_kind,origin_event_id,scheme_version_id,facts_id,project_id,merchant_id,payer_company_id,before_tax_cent,source_fingerprint,calculation_snapshot,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?)",
                id,r.userId(),r.month(),r.statementKind(),r.originEventId(),snap.scheme().id(),f.id(),f.projectId(),f.merchantId(),f.payerCompanyId(),
                snap.beforeTaxCent(),fingerprint,c.encode(snap),scope.actor().id());
            c.success("calculated","staff_payroll_calculation",id,r.idempotencyKey());return calculation(id);
        },CalculationDraft.class);
    }
    @com.complex.platform.gate.GuardedAction(permission="staff.payroll.manage",capability="staff.payroll.manage")
    @Transactional public StatementDetail settle(SettleRequest r) {
        var scope=c.scope(c.MANAGE);validateRequest(r);
        var initial=calculation(requireText(r.calculationId(),"calculation_id"));c.requireScope(scope,initial.projectId(),initial.merchantId(),initial.payerCompanyId());
        return idem.execute("payrollSettlement",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            c.lockEmployee(initial.userId());taxes.lockEmployee(initial.userId(),initial.month());
            var calc=calculation(initial.id());
            if(!"calculated".equals(calc.status())||!Objects.equals(r.userId(),calc.userId())||!Objects.equals(r.month(),calc.month())
                ||!Objects.equals(r.statementKind(),calc.statementKind())||!Objects.equals(r.factsId(),calc.factsId())||!Objects.equals(r.originEventId(),calc.originEventId()))
                throw conflict("PAYROLL_CALCULATION_MISMATCH","须使用本人、本期、同种业务的未定格税前原件");
            var f=c.period(calc.factsId());validateFacts(r,f);
            var current=build(r,f);
            if(!calc.sourceFingerprint().equals(fingerprint(current)))throw conflict("PAYROLL_SOURCE_CHANGED","工资来源已变化，须重新计算并取得专业税额认定");
            var tax=taxes.assessment(requireText(r.taxAssessmentId(),"tax_assessment_id"),true);
            if(!"approved".equals(tax.status())||!calc.id().equals(tax.payrollCalculationId())
                ||!calc.sourceFingerprint().equals(tax.sourceFingerprint())||!calc.userId().equals(tax.userId())
                ||!calc.month().equals(tax.month())||calc.beforeTaxCent()!=tax.incomeCent()
                ||!Objects.equals(calc.projectId(),tax.projectId())||!Objects.equals(calc.merchantId(),tax.merchantId())
                ||!Objects.equals(calc.payerCompanyId(),tax.payerCompanyId())||tax.taxCent()>calc.beforeTaxCent())
                throw conflict("PAYROLL_TAX_ASSESSMENT_REQUIRED","须引用该完整税前原件的已批准专业税额认定");
            String id=c.id("pay");insertStatement(id,r,current,tax,scope.actor().id());
            jdbc.update("UPDATE staff_payroll_calculation_drafts SET status='settled',statement_id=? WHERE id=?",id,calc.id());
            for(var line:current.lines()) {
                if("payroll_correction".equals(line.sourceType()))jdbc.update("UPDATE staff_payroll_corrections SET status='attached',target_statement_id=? WHERE id=? AND status='pending'",id,line.sourceId());
            }
            for(var item:current.pending())jdbc.update("INSERT INTO staff_payroll_pending_sources(id,user_id,project_id,merchant_id,original_statement_id,source_type,source_id,message) VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(user_id,source_type,source_id) DO NOTHING",
                c.id("pps"),r.userId(),f.projectId(),f.merchantId(),id,item.code(),item.sourceId(),item.message());
            resolvePendingSources(id,current);
            if(current.beforeTaxCent()>tax.taxCent())finance.ensurePayrollPayable(id);
            c.success("settled","staff_payroll",id,r.idempotencyKey());return detailInternal(id);
        },StatementDetail.class);
    }
    private Snapshot build(SettleRequest request,PeriodFacts facts) {
        var scheme=c.scheme(facts.schemeVersionId());var calendar=c.calendar(facts.calendarVersionId());
        var work=worktime.collect(scheme,facts,calendar,LocalDate.now(LocalZone.ID));
        var pending=new ArrayList<>(work.result().pendingItems());
        for(String source:bar.unresolvedAllocationSources(request.userId(),request.month())) {
            int delimiter=source.indexOf(':');String sourceId=delimiter>0?source.substring(0,delimiter):facts.id();
            pending.add(new PendingItem("BAR_ALLOCATION_PENDING",sourceId,source));
        }
        var adjustments=jdbc.queryForList("SELECT id,adj_type,amount_cent,reversal_of FROM staff_payroll_adjustments WHERE user_id=? AND month=? ORDER BY id",request.userId(),request.month());
        var scores=jdbc.query("SELECT id FROM staff_performance_score_adjustments WHERE user_id=? AND month=? AND status='approved' ORDER BY id",(rs,n)->c.score(rs.getString(1)),request.userId(),request.month());
        long requested=0;
        for(var row:adjustments) {
            long amount=((Number)row.get("amount_cent")).longValue();
            if("penalty".equals(row.get("adj_type")))requested=Math.addExact(requested,amount);
            if("penalty_reversal".equals(row.get("adj_type")))requested=Math.subtractExact(requested,amount);
        }
        int cap=jdbc.queryForObject("SELECT numeric_value FROM staff_policy_settings WHERE policy_key='payroll_penalty_cap_bps' AND scope_id='global'",Integer.class);
        BigDecimal points=points(scheme,scores);String grade=null;List<Component> confirmedComponents=scheme.components();
        try {grade=grade(scheme,facts);}catch(ApiException error) {
            if(!"PAYROLL_REVIEW_REQUIRED".equals(error.code()))throw error;
            confirmedComponents=scheme.components().stream().filter(component->!"performance".equals(component.method())).toList();
            for(var component:scheme.components())if("performance".equals(component.method()))
                pending.add(new PendingItem("PERFORMANCE_BASIS_PENDING",salaryKey(request.userId(),request.month(),component.code()),error.getMessage()+"；"+component.label()));
        }
        var earnings=StaffPayrollCalculator.calculate(confirmedComponents,facts.componentFacts(),points,scheme.performanceRules(),grade,
            work.leaveSeconds(),work.scheduledSeconds(),Math.max(0,requested),cap);
        boolean extra="supplemental".equals(request.statementKind());
        List<Line> lines=new ArrayList<>();
        if(!extra)lines.addAll(earnings.lines());
        else addDeferredSalary(lines,earnings,request,pending);
        for(var row:adjustments)if("bonus".equals(row.get("adj_type")))
            addIfUnpaid(lines,external("奖金","bonus",((Number)row.get("amount_cent")).longValue(),"payroll_bonus",(String)row.get("id"),row,work,true));
        // 159-R7：服务单份额按完成时点所在营业日（ENUM:291；有酒吧按该酒吧，没有按服务单所属综合体，第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」）
        // 判订单与核销已最终，未最终的记待结、不进本单；关联不到服务单的（手录）照旧计入。附加列取完即移出，工资行原件不变。
        var finalDays=new HashMap<String,String>();
        for(var row:jdbc.queryForList("SELECT l.id,l.source_type,l.source_id,l.amount_cent,l.note,o.bar_id order_bar_id,o.project_id order_project_id,o.completed_at order_completed_at FROM staff_commission_ledger l LEFT JOIN service_orders o ON l.source_type='service_order' AND o.id=l.source_id WHERE l.user_id=? AND l.month=? ORDER BY l.id",request.userId(),request.month())) {
            String orderBar=(String)row.remove("order_bar_id"),orderProject=(String)row.remove("order_project_id");Object completed=row.remove("order_completed_at");
            if(completed!=null) {
                String barId=orderBar==null||orderBar.isBlank()?null:orderBar.trim(),projectId=barId==null?orderProject:null;
                String waiting=nonFinal(finalDays,barId,projectId,businessDays.businessDate(projectId,barId,((Timestamp)completed).toInstant()),SERVICE_SHARE_FINAL);
                if(!waiting.isEmpty()){pending.add(new PendingItem("SERVICE_COMMISSION_PENDING",(String)row.get("id"),waiting));continue;}
            }
            addIfUnpaid(lines,external("已确认服务提成","commission",((Number)row.get("amount_cent")).longValue(),"staff_commission_ledger",(String)row.get("id"),row,work,true));
        }
        List<String> barCommissions=businessDays.barCommissionIdsInMonth(YearMonth.parse(request.month()),request.userId());
        String inMonth=barCommissions.isEmpty()?"NULL":String.join(",",Collections.nCopies(barCommissions.size(),"?"));
        for(String source:jdbc.queryForList("SELECT c.id FROM bar_commission_calculations c JOIN bar_bills b ON b.id=c.bill_id WHERE c.id IN ("+inMonth+") AND c.status NOT IN ('void','cancelled') AND b.settlement_status<>'settled' ORDER BY c.id",
                String.class,barCommissions.toArray()))pending.add(new PendingItem("BAR_COMMISSION_PENDING",source,"酒吧账单尚未确认结算，提成保留预计数"));
        // 159-R7：已结算账单的提成按账单所属营业日（ENUM:187，同 barCommissionIdsInMonth 口径）判该酒吧订单与核销、退款与冲正已最终，未最终的记待结保留预计数。
        for(var row:jdbc.queryForList("SELECT c.id,c.bill_id,c.commission_base_cent,c.commission_rate_bp,c.commission_amount_cent,c.status,b.bar_id bill_bar_id FROM bar_commission_calculations c JOIN bar_bills b ON b.id=c.bill_id WHERE c.id IN ("+inMonth+") AND c.status NOT IN ('void','cancelled') AND b.settlement_status='settled' ORDER BY c.id",
                barCommissions.toArray())) {
            String waiting=nonFinal(finalDays,(String)row.remove("bill_bar_id"),null,businessDays.barBillBusinessDate((String)row.get("bill_id")),BAR_COMMISSION_FINAL);
            if(!waiting.isEmpty()){pending.add(new PendingItem("BAR_COMMISSION_PENDING",(String)row.get("id"),waiting));continue;}
            addIfUnpaid(lines,external("已确认酒吧提成","commission",((Number)row.get("commission_amount_cent")).longValue(),"bar_commission",(String)row.get("id"),row,work,true));
        }
        // 第十七批（操作员裁决补充第 1 条）：账单作废并冲正、提成已计入定格工资的，按原期以负额冲回实发提成（同酒吧收入冲正，超出本期可付的转待核实）。
        for(var row:jdbc.queryForList("SELECT c.id,c.bill_id,l.statement_id,l.amount_cent FROM bar_commission_calculations c JOIN staff_payroll_statement_lines l ON l.source_type='bar_commission' AND l.source_id=c.id WHERE c.id IN ("+inMonth+") AND c.status='void' ORDER BY c.id",
                barCommissions.toArray())) {
            long back=-((Number)row.get("amount_cent")).longValue();
            addIfUnpaid(lines,new Line(null,"commission","commission","酒吧提成冲回（账单作废并冲正）",back,0,0,back,"bar_commission_reversal",(String)row.get("id"),row));
        }
        for(var allocation:bar.confirmedAllocations(request.userId(),request.month())) {
            if(!"confirmed".equals(allocation.status())||!Objects.equals(request.userId(),allocation.userId()))
                throw conflict("PAYROLL_SOURCE_SCOPE_MISMATCH","分配原件不是该员工已确认的收入");
            if(!Objects.equals(facts.payerCompanyId(),allocation.payerCompanyId())) {
                if(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statement_lines WHERE source_type='bar_payroll_allocation' AND source_id=?",Integer.class,allocation.id())==0)
                    pending.add(new PendingItem("BAR_ALLOCATION_PAYER_PENDING",allocation.id(),"该笔酒吧收入须由原分配发薪主体 "+allocation.payerCompanyId()+" 办理，不计入本主体工资"));
                continue;
            }
            addIfUnpaid(lines,new Line(null,allocation.componentType(),allocation.componentType(),
                "tip".equals(allocation.componentType())?"小费":"服务费",allocation.amountCent(),0,0,allocation.amountCent(),
                "bar_payroll_allocation",allocation.id(),map(allocation)));
        }
        String cycle=request.userId()+":"+facts.cycleStart()+":"+facts.cycleEnd();
        addWorkDelta(lines,request.userId()+":"+request.month()+":holiday",work.result().holidayOvertimeCent(),"法定节假日加班",work);
        if(work.result().pendingItems().isEmpty()) {
            String ordinaryKey="comprehensive".equals(scheme.worktimeTerms().system())?cycle+":ordinary":request.userId()+":"+request.month()+":ordinary";
            addWorkDelta(lines,ordinaryKey,work.result().ordinaryOvertimeCent(),"普通加班",work);
            addWorkDelta(lines,request.userId()+":"+request.month()+":rest",work.result().restDayOvertimeCent(),"休息日加班",work);
        }
        for(var correction:jdbc.queryForList("SELECT c.*,s.payer_company_id FROM staff_payroll_corrections c JOIN staff_payroll_statements s ON s.id=c.original_statement_id WHERE c.user_id=? AND c.status IN ('pending','needs_review') ORDER BY c.created_at,c.id",request.userId())) {
            if(!Objects.equals(facts.projectId(),correction.get("project_id"))||!Objects.equals(facts.merchantId(),correction.get("merchant_id"))||!Objects.equals(facts.payerCompanyId(),correction.get("payer_company_id"))) {
                pending.add(new PendingItem("CORRECTION_SCOPE_PENDING",(String)correction.get("id"),"原主体补差须在原发放主体核实后办理"));continue;
            }
            if(correction.get("amount_cent")==null||"needs_review".equals(correction.get("status"))) {
                pending.add(new PendingItem("CORRECTION_BASIS_PENDING",(String)correction.get("id"),"原已发工资缺少可复算依据，须核实真实少发差额"));continue;
            }
            long value=((Number)correction.get("amount_cent")).longValue();
            if(value>0)lines.add(new Line(null,"correction","correction","历史工资补差",value,0,0,value,"payroll_correction",(String)correction.get("id"),
                Map.of("original_statement_id",correction.get("original_statement_id"),"source_review_id",correction.get("source_review_id"))));
        }
        appendHistoricalPending(pending,request,facts);
        long sum=0;for(var line:lines)sum=Math.addExact(sum,line.amountCent());
        if(sum<0) {
            var negative=lines.stream().filter(l->Set.of("bar_payroll_allocation","bar_commission_reversal").contains(l.sourceType())&&l.amountCent()<0).toList();
            for(var line:negative){pending.add(new PendingItem("BAR_INCOME_REVERSAL_PENDING",line.sourceId(),"收入冲正超出本期可付金额，须专业核实另行处理"));sum=Math.subtractExact(sum,line.amountCent());}
            lines.removeAll(negative);
        }
        if(!extra)sum=Math.max(0,Math.subtractExact(sum,earnings.penaltyCent()));
        var due=StaffPayrollCalculator.dueDate(facts.agreedDueDate(),facts.statutoryDueDate(),c.calendarMap(calendar.days()));
        var basis=new LinkedHashMap<String,Object>();basis.put("agreed_due_date",facts.agreedDueDate());
        if(facts.statutoryDueDate()!=null)basis.put("statutory_due_date",facts.statutoryDueDate());
        basis.put("calendar_version_id",calendar.id());basis.put("payment_basis",scheme.paymentBasis());basis.put("resolved_due_date",due);
        return new Snapshot(scheme,facts,calendar,earnings,List.copyOf(lines),work,points,grade,adjustments,scores,cap,List.copyOf(pending),due,basis,sum);
    }
    /** 159-R7：单位该营业日所给类别未最终时返回待结说明，已最终返回空串；同一（单位, 营业日, 类别）一次计算只查一次。 */
    private String nonFinal(Map<String,String> cache,String merchant,String project,LocalDate day,List<String> categories) {
        return cache.computeIfAbsent(merchant+"|"+project+"|"+day+"|"+categories,k->{
            var days=finance.nonFinalDays(merchant,project,day,day,categories);
            return days.isEmpty()?"":"所属营业日数据尚未标记为最终，提成保留预计数："+FinanceService.describeNonFinalDays(days);
        });
    }
    private BigDecimal points(Scheme scheme,List<ScoreAdjustment> rows) {
        var p=scheme.performanceRules();if(p==null)return null;BigDecimal value=p.basePoints();
        for(var r:rows)value=r.reversalOf()==null?value.subtract(r.points()):value.add(r.points());
        return value.max(p.minPoints()).min(p.maxPoints());
    }
    private String grade(Scheme scheme,PeriodFacts facts) {
        var p=scheme.performanceRules();if(p==null)return null;
        var current=facts.performanceFacts();if(current==null)throw StaffPayrollCalculator.pending("绩效档次评定事实尚未确认");
        if(current.aConditionMet())return "A";
        int missed=current.targetMet()?0:1;
        for(int i=1;missed>0&&i<p.cMissedPeriods();i++) {
            String m=YearMonth.parse(facts.month()).minusMonths((long)i*p.assessmentPeriodMonths()).toString();
            var prior=jdbc.queryForList("SELECT performance_facts::text FROM staff_payroll_period_facts WHERE user_id=? AND month=? AND status='approved' ORDER BY approved_at DESC LIMIT 1",String.class,scheme.userId(),m);
            if(prior.isEmpty())throw StaffPayrollCalculator.pending("连续绩效周期尚缺已核实历史评定："+m);
            if(c.decode(prior.getFirst(),PerformanceFact.class).targetMet())break;missed++;
        }
        return missed>=p.cMissedPeriods()?"C":"B";
    }
    private Line external(String label,String type,long amount,String source,String id,Map<String,Object> input,StaffPayrollWorktimeService.Facts work,boolean leave) {
        long deduction=leave&&work.scheduledSeconds()>0?StaffPayrollCalculator.ratio(amount,work.leaveSeconds(),work.scheduledSeconds()):0;
        return new Line(null,type,type,label,amount,0,deduction,amount-deduction,source,id,input);
    }
    private void addIfUnpaid(List<Line> lines,Line line) {
        if(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statement_lines WHERE source_type=? AND source_id=?",Integer.class,line.sourceType(),line.sourceId())==0)lines.add(line);
    }
    private void addWorkDelta(List<Line> lines,String key,long total,String label,StaffPayrollWorktimeService.Facts work) {
        long previous=jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM staff_payroll_statement_lines WHERE source_type='worktime_delta' AND calculation_snapshot->>'work_key'=?",Long.class,key);
        if(total<previous)throw StaffPayrollCalculator.pending("已发工时金额高于现核实结果，须保留原单并专业复核差额");
        if(total>previous)lines.add(new Line(null,"overtime","overtime",label,total-previous,0,0,total-previous,"worktime_delta",fingerprint(List.of(key,total)),
            Map.of("work_key",key,"confirmed_total_cent",total,"previous_cent",previous,"attendance_ids",work.attendanceIds())));
    }
    private void validateRequest(SettleRequest r) {
        if(r==null)throw c.invalid("须填写工资计算请求");requireText(r.userId(),"user_id");c.month(r.month());requireText(r.factsId(),"facts_id");
        if(!Set.of("monthly","termination","supplemental").contains(Objects.toString(r.statementKind(),"")))throw c.invalid("工资类型不合法");
    }
    private void validateFacts(SettleRequest r,PeriodFacts f) {
        if(!"approved".equals(f.status())||!r.userId().equals(f.userId())||!r.month().equals(f.month()))throw c.invalid("须选择本人本期已批准计薪依据");
        c.requireSchemeFor(c.scheme(f.schemeVersionId()),r.userId(),r.month());
        if("monthly".equals(r.statementKind())&&jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statements WHERE user_id=? AND month=? AND statement_kind IN ('monthly','termination')",Integer.class,r.userId(),r.month())>0)
            throw conflict("PAYROLL_ALREADY_LOCKED","本期已有定格工资，后续真实差额须使用补发流程");
        if("termination".equals(r.statementKind())) {
            requireText(r.originEventId(),"origin_event_id");
            if(jdbc.queryForObject("SELECT count(*) FROM staff_employment_status_revisions r JOIN staff_profiles p ON p.id=r.profile_id WHERE r.review_id=? AND p.user_id=? AND r.after_status='departed' AND r.applied_at IS NOT NULL",Integer.class,r.originEventId(),r.userId())!=1)
                throw c.invalid("离职工资须关联本人已生效离职审批原件");
            if(f.statutoryDueDate()==null)throw c.invalid("离职工资须填写依法核定结清日期");
            if(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statements WHERE user_id=? AND month=? AND statement_kind IN ('monthly','termination')",Integer.class,r.userId(),r.month())>0)
                throw conflict("PAYROLL_ALREADY_LOCKED","本期已有工资，离职未结项目须单列补发");
        }
    }
    private void insertStatement(String id,SettleRequest r,Snapshot snap,PayrollTaxAssessment tax,String actor) {
        long total=snap.beforeTaxCent()-tax.taxCent();boolean extra="supplemental".equals(r.statementKind());
        var e=snap.earnings();
        jdbc.update("INSERT INTO staff_payroll_statements(id,user_id,month,base_cent,commission_cent,bonus_cent,penalty_cent,leave_deduction_cent,tax_cent,total_cent,status,closed_by,closed_at,project_id,penalty_cap_bps,payer_company_id,tax_assessment_id,tax_version_id,statement_kind,origin_event_id,scheme_version_id,facts_id,merchant_id,calculation_snapshot,payment_due_date,payment_due_basis,allowance_cent,performance_cent,overtime_cent,correction_cent,tip_cent,service_fee_cent,cash_penalty_requested_cent,cash_penalty_cap_base_cent,cash_penalty_cap_cent,pending_items) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,clock_timestamp(),?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?::jsonb,?,?,?,?,?,?,?,?,?,?::jsonb)",
            id,r.userId(),r.month(),extra?0:e.baseCent(),gross(snap.lines(),"commission"),gross(snap.lines(),"bonus"),extra?0:e.penaltyCent(),
            snap.lines().stream().mapToLong(l->l.embeddedLeaveCent()+l.additionalLeaveCent()).sum(),tax.taxCent(),total,total==0?"no_payment_due":"closed",
            actor,snap.facts().projectId(),snap.capBps(),snap.facts().payerCompanyId(),tax.id(),tax.taxVersionId(),r.statementKind(),r.originEventId(),
            snap.scheme().id(),snap.facts().id(),snap.facts().merchantId(),c.encode(snap),snap.dueDate(),c.encode(snap.dueBasis()),gross(snap.lines(),"allowance")+gross(snap.lines(),"full_attendance"),
            gross(snap.lines(),"performance"),gross(snap.lines(),"overtime"),gross(snap.lines(),"correction"),gross(snap.lines(),"tip"),gross(snap.lines(),"service_fee"),
            extra?0:e.penaltyRequestedCent(),extra?0:e.baseCent(),extra?0:e.penaltyCapCent(),c.encode(snap.pending()));
        for(var line:snap.lines())jdbc.update("INSERT INTO staff_payroll_statement_lines(id,statement_id,component_code,component_type,description,gross_cent,embedded_leave_cent,additional_leave_cent,amount_cent,source_type,source_id,calculation_snapshot) VALUES(?,?,?,?,?,?,?,?,?,?,?,?::jsonb)",
            c.id("pli"),id,line.componentCode(),line.componentType(),line.description(),line.grossCent(),line.embeddedLeaveCent(),line.additionalLeaveCent(),line.amountCent(),line.sourceType(),line.sourceId(),c.encode(line.calculationSnapshot()));
        jdbc.update("INSERT INTO staff_payroll_worktime_runs(id,user_id,month,project_id,merchant_id,facts_id,cycle_start,cycle_end,source_fingerprint,input_snapshot,result_snapshot,status) VALUES(?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,?)",
            c.id("pwr"),r.userId(),r.month(),snap.facts().projectId(),snap.facts().merchantId(),snap.facts().id(),snap.facts().cycleStart(),snap.facts().cycleEnd(),fingerprint(snap.worktime()),c.encode(snap.worktime()),c.encode(snap.worktime().result()),snap.worktime().result().pendingItems().isEmpty()?"confirmed":"needs_review");
    }
    private String salaryKey(String user,String month,String code) {return fingerprint(List.of(user,month,code));}
    private void addDeferredSalary(List<Line> lines,StaffPayrollCalculator.Earnings earnings,SettleRequest request,List<PendingItem> pending) {
        for(var line:earnings.lines()) {
            String key=salaryKey(request.userId(),request.month(),line.componentCode());
            if(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_pending_sources WHERE user_id=? AND source_type='PERFORMANCE_BASIS_PENDING' AND source_id=? AND status='pending'",Integer.class,request.userId(),key)==0)continue;
            long paid=jdbc.queryForObject("SELECT COALESCE(sum(l.amount_cent),0) FROM staff_payroll_statement_lines l JOIN staff_payroll_statements s ON s.id=l.statement_id WHERE s.user_id=? AND s.month=? AND l.source_type='salary_component' AND l.component_code=?",Long.class,request.userId(),request.month(),line.componentCode());
            if(line.amountCent()<paid) {
                pending.add(new PendingItem("PERFORMANCE_BASIS_PENDING",key,"绩效复算低于已确认工资，须保留原单并专业核实"));continue;
            }
            if(line.amountCent()>paid)lines.add(new Line(null,line.componentCode(),line.componentType(),line.description()+"补发",
                line.amountCent()-paid,0,0,line.amountCent()-paid,line.sourceType(),line.sourceId(),
                Map.of("confirmed_total_cent",line.amountCent(),"previous_cent",paid,"original_calculation",line.calculationSnapshot())));
        }
    }
    private void appendHistoricalPending(List<PendingItem> pending,SettleRequest request,PeriodFacts facts) {
        for(var row:jdbc.queryForList("SELECT p.id,p.message,s.month,s.id statement_id FROM staff_payroll_pending_sources p JOIN staff_payroll_statements s ON s.id=p.original_statement_id WHERE p.user_id=? AND p.status='pending' AND p.source_type<>'HISTORICAL_PAYROLL_PENDING' AND s.month<? AND s.project_id=? AND s.merchant_id IS NOT DISTINCT FROM ? AND s.payer_company_id=? ORDER BY s.month,p.id",
                request.userId(),request.month(),facts.projectId(),facts.merchantId(),facts.payerCompanyId())) {
            pending.add(new PendingItem("HISTORICAL_PAYROLL_PENDING",(String)row.get("id"),"原工资 "+row.get("month")+" / "+row.get("statement_id")+"："+row.get("message")+"；按原月份已批准依据补发，补发支付日按下一次实际应发日期核定"));
        }
    }
    private void resolvePendingSources(String statementId,Snapshot snapshot) {
        var open=jdbc.queryForList("SELECT p.id,p.source_type,p.source_id,s.month,f.cycle_start,f.cycle_end FROM staff_payroll_pending_sources p JOIN staff_payroll_statements s ON s.id=p.original_statement_id LEFT JOIN staff_payroll_period_facts f ON f.id=s.facts_id WHERE p.user_id=? AND p.status='pending' AND p.original_statement_id<>? AND s.payer_company_id=?",
            snapshot.scheme().userId(),statementId,snapshot.facts().payerCompanyId());
        for(var row:open) {
            String type=(String)row.get("source_type"),source=(String)row.get("source_id");
            boolean current=snapshot.pending().stream().anyMatch(p->p.code().equals(type)&&p.sourceId().equals(source));
            boolean sameMonth=snapshot.facts().month().equals(row.get("month"));
            boolean sameCycle=Objects.equals(row.get("cycle_start"),java.sql.Date.valueOf(snapshot.facts().cycleStart()))
                &&Objects.equals(row.get("cycle_end"),java.sql.Date.valueOf(snapshot.facts().cycleEnd()));
            boolean workComplete=snapshot.worktime().result().pendingItems().isEmpty();
            boolean resolved=switch(type) {
                case "WORKTIME_CYCLE_PENDING" -> sameCycle&&workComplete;
                case "WORKTIME_ROSTER_PENDING","WORKTIME_BASIS_PENDING" -> (sameMonth||sameCycle)&&workComplete;
                case "ATTENDANCE_TIME_PENDING","ATTENDANCE_INTERVAL_PENDING" -> (sameMonth||sameCycle)&&workComplete&&snapshot.worktime().intervals().stream().anyMatch(i->i.shiftId().equals(source));
                case "PERFORMANCE_BASIS_PENDING" -> sameMonth&&snapshot.grade()!=null&&snapshot.earnings().lines().stream().anyMatch(l->salaryKey(snapshot.scheme().userId(),snapshot.facts().month(),l.componentCode()).equals(source));
                case "BAR_COMMISSION_PENDING" -> snapshot.lines().stream().anyMatch(l->"bar_commission".equals(l.sourceType())&&source.equals(l.sourceId()));
                case "SERVICE_COMMISSION_PENDING" -> snapshot.lines().stream().anyMatch(l->"staff_commission_ledger".equals(l.sourceType())&&source.equals(l.sourceId()));
                case "BAR_INCOME_REVERSAL_PENDING" -> snapshot.lines().stream().anyMatch(l->Set.of("bar_payroll_allocation","bar_commission_reversal").contains(l.sourceType())&&source.equals(l.sourceId()));
                case "BAR_ALLOCATION_PAYER_PENDING" -> jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statement_lines WHERE source_type='bar_payroll_allocation' AND source_id=?",Integer.class,source)>0;
                case "BAR_ALLOCATION_PENDING" -> snapshot.lines().stream().anyMatch(l->"bar_payroll_allocation".equals(l.sourceType())&&sourceBill(l,source));
                case "CORRECTION_BASIS_PENDING","CORRECTION_SCOPE_PENDING" -> snapshot.lines().stream().anyMatch(l->"payroll_correction".equals(l.sourceType())&&source.equals(l.sourceId()));
                case "HISTORICAL_PAYROLL_PENDING" -> jdbc.queryForObject("SELECT count(*) FROM staff_payroll_pending_sources WHERE id=? AND status='resolved'",Integer.class,source)>0;
                default -> false;
            };
            if(!current&&resolved)jdbc.update("UPDATE staff_payroll_pending_sources SET status='resolved',resolved_statement_id=?,resolved_at=clock_timestamp() WHERE id=? AND status='pending'",statementId,row.get("id"));
        }
        for(var line:snapshot.lines())if("bar_payroll_allocation".equals(line.sourceType()))
            jdbc.update("UPDATE staff_payroll_pending_sources SET status='resolved',resolved_statement_id=?,resolved_at=clock_timestamp() WHERE user_id=? AND source_type='BAR_ALLOCATION_PAYER_PENDING' AND source_id=? AND status='pending'",statementId,snapshot.scheme().userId(),line.sourceId());
        jdbc.update("UPDATE staff_payroll_pending_sources child SET status='resolved',resolved_statement_id=?,resolved_at=clock_timestamp() FROM staff_payroll_pending_sources original WHERE child.user_id=? AND child.source_type='HISTORICAL_PAYROLL_PENDING' AND child.source_id=original.id AND original.status='resolved' AND child.status='pending'",statementId,snapshot.scheme().userId());
    }
    private boolean sourceBill(Line line,String source) {
        Object bills=line.calculationSnapshot().get("sourceBillIds");
        if(bills==null)bills=line.calculationSnapshot().get("source_bill_ids");
        return bills instanceof Collection<?> values&&values.contains(source);
    }
    private long gross(List<Line> lines,String type){return lines.stream().filter(l->type.equals(l.componentType())).mapToLong(Line::grossCent).sum();}
    public StatementDetail detail(String id,boolean mine) {
        var row=statement(id,false);
        if(mine){auth.requirePermission("staff.payroll.view_mine");if(!row.get("user_id").equals(auth.currentActor().id()))throw ApiException.forbidden("PAYROLL_SELF_ONLY","只能查看本人工资");}
        else c.requireScope(c.scope(c.MANAGE),(String)row.get("project_id"),(String)row.get("merchant_id"),(String)row.get("payer_company_id"));
        return detailInternal(id);
    }
    private StatementDetail detailInternal(String id) {
        var row=statement(id,false);
        var original=new StaffPayrollStatementRecord(id,(String)row.get("user_id"),(String)row.get("month"),num(row,"base_cent"),num(row,"commission_cent"),
            num(row,"bonus_cent"),num(row,"penalty_cent"),num(row,"leave_deduction_cent"),num(row,"tax_cent"),num(row,"total_cent"),(String)row.get("closed_by"),
            ((Timestamp)row.get("closed_at")).toInstant(),(String)row.get("payer_company_id"),(String)row.get("tax_assessment_id"),(String)row.get("tax_version_id"));
        var lines=jdbc.query("SELECT * FROM staff_payroll_statement_lines WHERE statement_id=? ORDER BY id",(s,n)->new Line(s.getString("id"),s.getString("component_code"),s.getString("component_type"),s.getString("description"),
            s.getLong("gross_cent"),s.getLong("embedded_leave_cent"),s.getLong("additional_leave_cent"),s.getLong("amount_cent"),s.getString("source_type"),s.getString("source_id"),c.decode(s.getString("calculation_snapshot"),new TypeReference<Map<String,Object>>(){})),id);
        var corrections=jdbc.query("SELECT * FROM staff_payroll_corrections WHERE original_statement_id=? OR target_statement_id=? ORDER BY created_at,id",(s,n)->new Correction(s.getString("id"),s.getString("original_statement_id"),s.getString("original_month"),s.getString("source_review_id"),s.getString("source_adjustment_id"),(Long)s.getObject("amount_cent"),s.getString("status"),s.getString("target_statement_id"),c.decode(s.getString("calculation_snapshot"),new TypeReference<Map<String,Object>>(){})),id,id);
        var payments=jdbc.query("SELECT * FROM staff_payroll_payouts WHERE statement_id=? ORDER BY paid_at,id",(s,n)->new Payment(s.getString("id"),s.getString("finance_payment_id"),s.getLong("paid_total_cent"),s.getString("pay_method"),s.getString("pay_ref"),s.getTimestamp("paid_at").toInstant()),id);
        Map<String,Object> raw=c.decode(row.get("calculation_snapshot").toString(),new TypeReference<Map<String,Object>>(){});
        WorkResult result=null;if(raw.get("worktime") instanceof Map<?,?> data&&data.get("result")!=null)result=c.decode(c.encode(data.get("result")),WorkResult.class);
        return new StatementDetail(original,(String)row.get("status"),(String)row.get("statement_kind"),(String)row.get("scheme_version_id"),(String)row.get("facts_id"),
            row.get("payment_due_date")==null?null:((java.sql.Date)row.get("payment_due_date")).toLocalDate(),c.decode(row.get("payment_due_basis").toString(),new TypeReference<Map<String,Object>>(){}),
            (String)row.get("ar_ap_item_id"),num(row,"paid_amount_cent"),lines,result,corrections,payments,
            jdbc.query("SELECT source_type,source_id,message FROM staff_payroll_pending_sources WHERE original_statement_id=? AND status='pending' ORDER BY created_at,id",
                (p,n)->new PendingItem(p.getString(1),p.getString(2),p.getString(3)),id));
    }
    @EventListener @Transactional public void paymentConfirmed(FinancePayrollService.PayrollPaymentConfirmed e){synchronizeConfirmedPayment(e.paymentId());}
    @Transactional public void synchronizeConfirmedPayment(String paymentId) {
        var found=jdbc.queryForList("SELECT s.id,s.user_id FROM ar_ap_payments p JOIN ar_ap_items a ON a.id=p.ar_ap_item_id JOIN staff_payroll_statements s ON s.id=a.source_id WHERE p.id=? AND a.source_type='staff_payroll'",paymentId);
        if(found.isEmpty())throw conflict("PAYROLL_FINANCE_RESULT_REQUIRED","缺少真实工资付款原件");
        c.lockEmployee((String)found.getFirst().get("user_id"));var wage=statement((String)found.getFirst().get("id"),true);
        var p=jdbc.queryForList("SELECT p.*,t.created_by FROM ar_ap_payments p JOIN finance_payroll_payment_attempts t ON t.finance_payment_id=p.id WHERE p.id=? AND t.statement_id=? AND t.status='succeeded' AND t.amount_cent=p.amount_cent AND t.payer_company_id=? AND t.recipient_user_id=?",
            paymentId,wage.get("id"),wage.get("payer_company_id"),wage.get("user_id"));
        if(p.size()!=1)throw conflict("PAYROLL_FINANCE_RESULT_REQUIRED","工资仅接受同收款人和发放主体的实际成功付款");
        var payment=p.getFirst();
        if(!Objects.equals(payment.get("ar_ap_item_id"),wage.get("ar_ap_item_id")))throw c.invalid("付款应付来源与工资单不一致");
        jdbc.update("INSERT INTO staff_payroll_payouts(id,statement_id,user_id,month,paid_total_cent,pay_method,pay_ref,paid_by,paid_at,project_id,finance_payment_id) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(finance_payment_id) WHERE finance_payment_id IS NOT NULL DO NOTHING",
            "payout_"+paymentId,wage.get("id"),wage.get("user_id"),wage.get("month"),payment.get("amount_cent"),payment.get("payment_method"),payment.get("bank_flow_ref"),payment.get("created_by"),payment.get("payment_time"),wage.get("project_id"),paymentId);
        long paid=jdbc.queryForObject("SELECT COALESCE(sum(paid_total_cent),0) FROM staff_payroll_payouts WHERE statement_id=?",Long.class,wage.get("id"));
        if(paid>num(wage,"total_cent"))throw c.invalid("实际工资付款累计超过原应付");
        jdbc.update("UPDATE staff_payroll_statements SET paid_amount_cent=?,status=? WHERE id=?",paid,paid==num(wage,"total_cent")?"paid":"partial_paid",wage.get("id"));
        if(paid==num(wage,"total_cent"))jdbc.update("UPDATE staff_payroll_corrections SET status='paid',paid_at=clock_timestamp() WHERE target_statement_id=? AND status='attached'",wage.get("id"));
    }
    /** source：补差来源说明（如 leave_revoked）；申诉撤罚沿用四参构造，来源由审批原件体现。 */
    public record CorrectionRequested(String userId,String month,String sourceReviewId,String sourceAdjustmentId,String source) {
        public CorrectionRequested(String userId,String month,String sourceReviewId,String sourceAdjustmentId){this(userId,month,sourceReviewId,sourceAdjustmentId,null);}
    }
    @EventListener @Transactional public void correctionRequested(CorrectionRequested event) {
        c.lockEmployee(event.userId());
        if(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_corrections WHERE source_review_id=?",Integer.class,event.sourceReviewId())>0)return;
        var originals=jdbc.queryForList("SELECT * FROM staff_payroll_statements WHERE user_id=? AND month=? AND statement_kind IN ('monthly','termination') ORDER BY closed_at,id FOR UPDATE",event.userId(),event.month());
        if(originals.isEmpty())return;
        var original=originals.getFirst();Long amount=null;String status="needs_review";Map<String,Object> result=new LinkedHashMap<>();
        result.put("source_review_id",event.sourceReviewId());result.put("source_adjustment_id",event.sourceAdjustmentId());
        if(event.source()!=null)result.put("source",event.source());
        var periodStatements=jdbc.queryForList("SELECT * FROM staff_payroll_statements WHERE user_id=? AND month=? AND payer_company_id IS NOT DISTINCT FROM ? ORDER BY closed_at,id FOR UPDATE",
            event.userId(),event.month(),original.get("payer_company_id"));
        if(periodStatements.stream().allMatch(row->!"{}".equals(row.get("calculation_snapshot").toString())))try {
            long totalDifference=0;var sources=new ArrayList<Map<String,Object>>();
            for(var row:periodStatements) {
                var snap=c.decode(row.get("calculation_snapshot").toString(),Snapshot.class);
                long difference=lockedStatementDifference(snap,"supplemental".equals(row.get("statement_kind")));
                totalDifference=Math.addExact(totalDifference,difference);
                sources.add(Map.of("statement_id",row.get("id"),"locked_calculation",snap,"actual_difference_cent",difference));
            }
            long already=jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM staff_payroll_corrections WHERE original_statement_id=? AND amount_cent IS NOT NULL AND status IN ('pending','attached','paid','no_difference')",Long.class,original.get("id"));
            amount=Math.max(0,totalDifference-already);
            result.put("original_locked_statements",sources);result.put("already_restored_cent",already);result.put("actual_difference_cent",amount);
            status=amount==0?"no_difference":"pending";
        }catch(ApiException error) {
            // 锁定快照不足以复算（如销假涉及公式内事假扣减）：保留 needs_review，由专业核实真实差额。
            if(!"PAYROLL_REVIEW_REQUIRED".equals(error.code()))throw error;
            result.put("review_reason",error.getMessage());
        }
        jdbc.update("INSERT INTO staff_payroll_corrections(id,user_id,project_id,merchant_id,original_statement_id,source_review_id,source_adjustment_id,original_month,amount_cent,calculation_snapshot,status) VALUES(?,?,?,?,?,?,?,?,?,?::jsonb,?)",
            c.id("pco"),event.userId(),original.get("project_id"),original.get("merchant_id"),original.get("id"),
            event.sourceReviewId(),event.sourceAdjustmentId(),event.month(),amount,c.encode(result),status);
    }
    /** 补发过的绩效也按其当时锁定原件复算，不能只检查原月单。 */
    private long lockedStatementDifference(Snapshot snap,boolean supplemental) {
        var work=snap.worktime();long leaveSeconds=work.leaveSeconds();
        // BSM L1481：定格后销假的事假，按锁定快照把该假的重合工时从事假工时中扣除后复算，不回改原单。
        for(String leave:work.leaveIds()) {
            if(jdbc.queryForObject("SELECT count(*) FROM staff_leave_requests WHERE id=? AND status='revoked'",Integer.class,leave)==0)continue;
            Long seconds=work.leaveSecondsById()==null?null:work.leaveSecondsById().get(leave);
            if(seconds==null||snap.facts().componentFacts().stream().anyMatch(f->f.appliedLeaveIds()!=null&&f.appliedLeaveIds().contains(leave)))
                throw StaffPayrollCalculator.pending("锁定快照缺少该次事假的逐条工时或已含公式内扣减，销假差额须专业核实："+leave);
            leaveSeconds=Math.subtractExact(leaveSeconds,seconds);
        }
        long externalRestored=0;
        if(leaveSeconds<work.leaveSeconds())for(var line:snap.lines())if(line.additionalLeaveCent()>0&&!"salary_component".equals(line.sourceType()))
            externalRestored=Math.addExact(externalRestored,line.additionalLeaveCent()-StaffPayrollCalculator.ratio(line.grossCent(),leaveSeconds,work.scheduledSeconds()));
        var paidCodes=snap.lines().stream().filter(line->"salary_component".equals(line.sourceType())).map(Line::componentCode).collect(java.util.stream.Collectors.toSet());
        if(supplemental&&paidCodes.isEmpty())return externalRestored;
        long remaining=0;
        for(var adjustment:snap.adjustments()) {
            if(!"penalty".equals(adjustment.get("adj_type")))continue;
            boolean reversed=jdbc.queryForObject("SELECT count(*) FROM staff_payroll_adjustments WHERE reversal_of=? AND adj_type='penalty_reversal'",Integer.class,adjustment.get("id"))>0;
            if(!reversed)remaining=Math.addExact(remaining,((Number)adjustment.get("amount_cent")).longValue());
        }
        BigDecimal points=snap.points();
        if(points!=null) {
            BigDecimal total=snap.scheme().performanceRules().basePoints();
            for(var score:snap.scores()) {
                if(score.reversalOf()!=null)continue;
                if(jdbc.queryForObject("SELECT count(*) FROM staff_performance_score_adjustments WHERE reversal_of=? AND status='approved'",Integer.class,score.id())==0)
                    total=total.subtract(score.points());
            }
            points=total.max(snap.scheme().performanceRules().minPoints()).min(snap.scheme().performanceRules().maxPoints());
        }
        var confirmed=snap.scheme().components().stream().filter(component->snap.earnings().lines().stream().anyMatch(line->line.componentCode().equals(component.code()))).toList();
        var recomputed=StaffPayrollCalculator.calculate(confirmed,snap.facts().componentFacts(),points,
            snap.scheme().performanceRules(),snap.grade(),leaveSeconds,work.scheduledSeconds(),remaining,snap.capBps());
        long oldSalary=snap.earnings().lines().stream().filter(line->paidCodes.contains(line.componentCode())).mapToLong(Line::amountCent).sum();
        long newSalary=recomputed.lines().stream().filter(line->paidCodes.contains(line.componentCode())).mapToLong(Line::amountCent).sum();
        long originalAmount=snap.lines().stream().mapToLong(Line::amountCent).sum();
        long before=Math.max(0,originalAmount-(supplemental?0:snap.earnings().penaltyCent()));
        long after=Math.max(0,originalAmount+externalRestored-oldSalary+newSalary-(supplemental?0:recomputed.penaltyCent()));
        return Math.max(0,after-before);
    }
    @Transactional public List<String> preparePendingCorrections(String statementId) {
        var first=statement(statementId,false);c.lockEmployee((String)first.get("user_id"));var row=statement(statementId,true);
        return jdbc.queryForList("SELECT id FROM staff_payroll_statements WHERE user_id=? AND payment_due_date=? AND statement_kind='supplemental' AND status IN ('closed','partial_paid') AND id<>? ORDER BY closed_at,id",
            String.class,row.get("user_id"),row.get("payment_due_date"),statementId);
    }
    public String requireEvidenceScope(String type,String id,String permission){return c.requireEvidenceScope(type,id,permission);}
    CalculationDraft calculation(String id){return jdbc.query("SELECT * FROM staff_payroll_calculation_drafts WHERE id=?",this::calculationRow,id).stream().findFirst().orElseThrow(()->c.missing("税前计算原件"));}
    private CalculationDraft calculationRow(ResultSet r,int n)throws SQLException{return new CalculationDraft(r.getString("id"),r.getString("user_id"),r.getString("month"),r.getString("statement_kind"),r.getString("origin_event_id"),r.getString("scheme_version_id"),r.getString("facts_id"),r.getString("project_id"),r.getString("merchant_id"),r.getString("payer_company_id"),r.getLong("before_tax_cent"),r.getString("source_fingerprint"),c.decode(r.getString("calculation_snapshot"),new TypeReference<Map<String,Object>>(){}),r.getString("status"),r.getTimestamp("created_at").toInstant());}
    private Map<String,Object> statement(String id,boolean lock){return jdbc.queryForList("SELECT * FROM staff_payroll_statements WHERE id=?"+(lock?" FOR UPDATE":""),id).stream().findFirst().orElseThrow(()->c.missing("工资单"));}
    private Map<String,Object> map(Object obj){return json.convertValue(obj,new TypeReference<Map<String,Object>>(){});}
    private String fingerprint(Object obj){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(c.encode(obj).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private long num(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private ApiException conflict(String code,String message){return ApiException.conflict(code,message);}
}
