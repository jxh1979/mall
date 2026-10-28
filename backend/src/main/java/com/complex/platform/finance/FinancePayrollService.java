package com.complex.platform.finance;

// 文件版本：V0.02

import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 工资原件生成应付；银行执行事实与成功入账分开，未知结果不能再次出款。 */
@Service
public class FinancePayrollService {
    public static final String VIEW="finance.payroll.view", PAY="finance.payroll.payment.register", EXPORT="finance.payroll.export";
    private final JdbcTemplate jdbc;
    private final FinanceRepository finance;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator files;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;

    public FinancePayrollService(JdbcTemplate jdbc,FinanceRepository finance,AuthorizationService auth,
            IdempotencyService idempotency,AuditEventService audit,EvidenceEventRepository evidence,
            EvidenceAttachmentValidator files,ObjectMapper mapper,ApplicationEventPublisher events) {
        this.jdbc=jdbc;this.finance=finance;this.auth=auth;this.idempotency=idempotency;
        this.audit=audit;this.evidence=evidence;this.files=files;this.mapper=mapper;this.events=events;
    }
    public record PrepareRequest(String idempotencyKey,long amountCent) {}
    public record SubmitRequest(String idempotencyKey,String bankRequestRef,Instant requestedAt,
            String recipientUserId,List<String> evidenceAttachmentIds) {}
    public record ResultRequest(String idempotencyKey,String status,Instant resultAt,String bankFlowRef,
            String reason,List<String> evidenceAttachmentIds) {}
    public record PayrollPaymentConfirmed(String paymentId) {}
    public record PaymentContext(List<Map<String,Object>> attempts,List<Map<String,Object>> relatedStatements,
            List<Map<String,Object>> pendingCorrections) {}
    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.beans.factory.ObjectProvider<com.complex.platform.access.staff.StaffPayrollService> staffPayroll;
    private record Scope(String actorId,String merchant,AuthorizationService.ProjectReach reach) {}
    private record Wage(String id,String user,String month,String project,String merchant,String payer,
            long total,String status,String scheme,String snapshot,LocalDate due,String dueBasis,String payable) {}

    /** 仅由工资定格事务调用，不开放手工金额造账接口。 */
    @Transactional
    public ArApItemRecord ensurePayrollPayable(String statementId) {
        Wage w=lockWage(statementId);
        if(!Set.of("closed","partial_paid","paid").contains(w.status()) || w.total()<=0
                || blank(w.project()) || blank(w.payer()) || blank(w.scheme()) || w.due()==null
                || emptyJson(w.snapshot()) || emptyJson(w.dueBasis()))
            throw invalid("工资尚未定格，或缺少已批准方案、发薪主体、完整计算与支付日期依据");
        requireApprovedBasis(w);
        String id="ap_salary_"+token(w.id());
        var existing=finance.findArApItemById(id);
        if(existing.isPresent()) { checkPayable(w,existing.get());return existing.get(); }
        Instant now=Instant.now();
        var item=finance.appendArApItem(new ArApItemRecord(id,"GZ-"+token(w.id()),"payable",w.user(),
                "staff_payroll",w.id(),w.total(),0,w.total(),"CNY",w.due(),w.month(),"pending_payment",
                now,now,w.merchant(),w.project()));
        finance.saveArApItemSource(new ArApItemSourceRecord("src_salary_"+token(w.id()),id,"staff_payroll",w.id(),w.total(),now));
        jdbc.update("UPDATE staff_payroll_statements SET ar_ap_item_id=? WHERE id=? AND ar_ap_item_id IS NULL",id,w.id());
        audit.recordSuccess("finance.payroll.posted",null,"staff_payroll",w.id(),null,w.id(),Map.of("ar_ap_item_id",id));
        return item;
    }

    public PaymentContext attempts(String statementId) {
        try {
        Scope scope=scope(VIEW);Wage w=wage(statementId,false);checkScope(w,scope);
        var attempts=jdbc.queryForList("SELECT * FROM finance_payroll_payment_attempts WHERE statement_id=? ORDER BY created_at,id",statementId);
        var related=jdbc.queryForList("""
                SELECT id,ar_ap_item_id,total_cent,paid_amount_cent,payment_due_date,status,statement_kind
                  FROM staff_payroll_statements WHERE user_id=? AND id<>? AND payment_due_date=?
                   AND project_id=? AND payer_company_id=? AND merchant_id IS NOT DISTINCT FROM ?
                 ORDER BY closed_at,id
                """,w.user(),w.id(),w.due(),w.project(),w.payer(),w.merchant());
        var corrections=jdbc.queryForList("""
                SELECT c.id,c.original_month,c.amount_cent,c.status FROM staff_payroll_corrections c
                JOIN staff_payroll_statements original ON original.id=c.original_statement_id
                 WHERE c.user_id=? AND c.status IN ('pending','needs_review')
                   AND c.project_id=? AND original.payer_company_id=? AND c.merchant_id IS NOT DISTINCT FROM ?
                 ORDER BY c.created_at,c.id
                """,w.user(),w.project(),w.payer(),w.merchant());
        audit.recordSuccess("finance.payroll.attempts_viewed",VIEW,"staff_payroll",statementId,null,null,Map.of("attempt_count",attempts.size()));
        return new PaymentContext(attempts,related,corrections);
        } catch(RuntimeException e) {
            audit.recordFailure("finance.payroll.attempts_view_failed",VIEW,"staff_payroll",statementId,null,null,e instanceof ApiException a?a.code():"INTERNAL_ERROR");
            throw e;
        }
    }

    @Transactional
    @com.complex.platform.gate.GuardedAction(capability=PAY,objectType="staff_payroll")
    public Map<String,Object> prepare(String statementId,PrepareRequest r) {
        try {
            Scope scope=scope(PAY);checkScope(wage(statementId,false),scope);
            if(r==null || r.amountCent()<=0)throw invalid("请填写本次实际计划支付的正金额");
            return write("prepare:"+statementId,r.idempotencyKey(),r,scope,statementId,()->{
                Wage w=lockWage(statementId);checkScope(w,scope);
                staffPayroll.getObject().preparePendingCorrections(statementId);
                var payable=payable(w);
                if(r.amountCent()>payable.unpaidAmountCent())throw invalid("本次金额超过尚未支付工资");
                if(jdbc.queryForObject("SELECT count(*) FROM finance_payroll_payment_attempts WHERE statement_id=? AND status IN ('prepared','processing','unknown')",Integer.class,statementId)>0)
                    throw invalid("此工资有准备中或结果未明的付款，须先核实原笔，不能重复出款");
                String id="salary_try_"+token(statementId+":"+r.idempotencyKey());
                jdbc.update("""
                        INSERT INTO finance_payroll_payment_attempts(id,statement_id,ar_ap_item_id,amount_cent,payer_company_id,status,created_by)
                        VALUES(?,?,?,?,?,'prepared',?)
                        """,id,w.id(),payable.id(),r.amountCent(),w.payer(),scope.actorId());
                return attempt(id);
            });
        } catch(RuntimeException e) {
            recordPaymentFailure(statementId,r==null?null:r.idempotencyKey(),e);throw e;
        }
    }

    @Transactional
    @com.complex.platform.gate.GuardedAction(capability=PAY,objectType="staff_payroll_payment")
    public Map<String,Object> submit(String id,SubmitRequest r) {
        try {
            Scope scope=scope(PAY);var first=attempt(id);checkScope(wage((String)first.get("statement_id"),false),scope);
            if(r==null)throw invalid("请登记已经在银行发起的付款事实");
            return write("submit:"+id,r.idempotencyKey(),r,scope,id,()->{
                Wage w=lockWage((String)first.get("statement_id"));checkScope(w,scope);var p=payable(w);var a=attempt(id);
                if(!"prepared".equals(a.get("status")))throw invalid("仅准备中的付款可登记银行发起事实");
                if(!w.user().equals(r.recipientUserId()))throw invalid("工资收款人必须为工资单本人");
                if(number(a,"amount_cent")>p.unpaidAmountCent())throw invalid("未付余额已变化，须撤销准备记录后重建");
                requireTime(r.requestedAt(),null);String reference=bankRef(r.bankRequestRef(),"银行单笔请求编号");checkEvidence(id,w,r.evidenceAttachmentIds());
                if(jdbc.queryForObject("SELECT count(*) FROM finance_payroll_payment_attempts WHERE payer_company_id=? AND bank_request_ref=? AND id<>?",Integer.class,w.payer(),reference,id)>0)
                    throw invalid("该发薪主体的银行请求已登记，不得再次作为新付款");
                jdbc.update("UPDATE finance_payroll_payment_attempts SET status='processing',bank_request_ref=?,requested_at=?,recipient_user_id=?,updated_at=now() WHERE id=?",
                        reference,java.sql.Timestamp.from(r.requestedAt()),w.user(),id);
                appendEvent(id,"processing",r.requestedAt(),r.evidenceAttachmentIds(),"登记银行实际发起",scope.actorId());
                return attempt(id);
            });
        } catch(RuntimeException e) {
            recordPaymentFailure(id,r==null?null:r.idempotencyKey(),e);throw e;
        }
    }

    @Transactional
    @com.complex.platform.gate.GuardedAction(capability=PAY,objectType="staff_payroll_payment")
    public Map<String,Object> result(String id,ResultRequest r) {
        try {
            Scope scope=scope(PAY);var first=attempt(id);checkScope(wage((String)first.get("statement_id"),false),scope);
            if(r==null || !Set.of("succeeded","failed","unknown","cancelled").contains(Objects.toString(r.status(),"")))throw invalid("请选择实际银行结果，或撤销尚未发起的准备记录");
            return write("result:"+id,r.idempotencyKey(),r,scope,id,()->{
                Wage w=lockWage((String)first.get("statement_id"));checkScope(w,scope);var p=payable(w);var a=attempt(id);
                String before=(String)a.get("status");requireText(r.reason(),"核实说明");
                if("cancelled".equals(r.status())) {
                    if(!"prepared".equals(before))throw invalid("银行已发起或结果未明的付款不能按未出款撤销");
                    requireTime(r.resultAt(),null);
                } else {
                    if(!Set.of("processing","unknown").contains(before))throw invalid("该笔不处于银行处理中或待核实状态");
                    requireTime(r.resultAt(),((java.sql.Timestamp)a.get("requested_at")).toInstant());
                    checkEvidence(id,w,r.evidenceAttachmentIds());
                }
                String paymentId=null;
                if("succeeded".equals(r.status())) {
                    String flow=bankRef(r.bankFlowRef(),"银行单笔成功流水号");
                    if(jdbc.queryForObject("SELECT count(*) FROM finance_payroll_payment_attempts WHERE payer_company_id=? AND bank_flow_ref=? AND status='succeeded' AND id<>?",Integer.class,w.payer(),flow,id)>0)
                        throw invalid("该银行成功流水已入账，不能重复确认");
                    long amount=number(a,"amount_cent");if(amount>p.unpaidAmountCent())throw invalid("实际确认金额超过工资未付余额，请财务核实");
                    paymentId="salary_pay_"+token(id);Instant now=Instant.now();
                    finance.saveArApPayment(new ArApPaymentRecord(paymentId,p.id(),amount,"bank_transfer",r.resultAt(),flow,now));
                    long paid=Math.addExact(p.paidAmountCent(),amount),unpaid=p.amountCent()-paid;
                    finance.updateArApItemLedger(new ArApItemRecord(p.id(),p.itemNo(),p.direction(),p.counterpartyId(),p.sourceType(),p.sourceId(),
                            p.amountCent(),paid,unpaid,p.currency(),p.dueDate(),p.accountingPeriod(),unpaid==0?"settled":"partial_paid",p.createdAt(),now,p.merchantId(),p.projectId(),p.originItemId(),p.adjustmentId()));
                }
                jdbc.update("UPDATE finance_payroll_payment_attempts SET status=?,bank_flow_ref=?,finance_payment_id=?,result_at=?,updated_at=now() WHERE id=?",
                        r.status(),"succeeded".equals(r.status())?r.bankFlowRef().trim():null,paymentId,java.sql.Timestamp.from(r.resultAt()),id);
                appendEvent(id,r.status(),r.resultAt(),r.evidenceAttachmentIds(),r.reason(),scope.actorId());
                if(paymentId!=null)events.publishEvent(new PayrollPaymentConfirmed(paymentId));
                return attempt(id);
            });
        } catch(RuntimeException e) {
            recordPaymentFailure(id,r==null?null:r.idempotencyKey(),e);throw e;
        }
    }

    public Set<String> visiblePayableIds(String permission) {
        if(!auth.currentActorHasPermission(permission))return Set.of();
        Scope s=scope(permission);
        if(s.reach().platformAdmin())return null;
        // 第十八批 E 段 20-R3：商户账号查看各经营项目里本商户的工资应付，只按商户
        if(merchantRead(s))return new HashSet<>(jdbc.queryForList(
                "SELECT ar_ap_item_id FROM staff_payroll_statements WHERE ar_ap_item_id IS NOT NULL AND merchant_id=?",String.class,s.merchant()));
        Set<String> ids=s.reach().organizationIds();
        if(ids.isEmpty())return Set.of();
        String slots=String.join(",",Collections.nCopies(ids.size(),"?"));
        List<Object> args=new ArrayList<>(ids);args.addAll(ids);
        String sql="SELECT ar_ap_item_id FROM staff_payroll_statements WHERE ar_ap_item_id IS NOT NULL AND project_id IN ("+slots+") AND payer_company_id IN ("+slots+")";
        if(s.merchant()!=null) {sql+=" AND merchant_id=?";args.add(s.merchant());}
        return new HashSet<>(jdbc.queryForList(sql,String.class,args.toArray()));
    }

    public void requireReadScope(String statementId,String permission) {
        Wage w=wage(statementId,false);Scope s=scope(permission);
        // 第十八批 E 段 20-R3：商户账号读工资应付只比商户；付款写仍走 checkScope
        if(merchantRead(s)) {
            if(!s.merchant().equals(w.merchant()))throw ApiException.forbidden("PAYROLL_FINANCE_SCOPE_MISMATCH","无权读取或付款该项目、商户与发薪主体的工资");
            return;
        }
        checkScope(w,s);
    }
    private static boolean merchantRead(Scope s) {return !s.reach().platformAdmin()&&s.merchant()!=null&&!s.merchant().isBlank();}
    public String requireEvidenceScope(String id,String evidencePermission) {
        String permission="evidence.timeline.view".equals(evidencePermission)?VIEW:PAY;
        Scope scope=scope(permission,evidencePermission);var a=attempt(id);
        Wage w=wage((String)a.get("statement_id"),false);
        // 第十八批 E 段 20-R4 扫尾：查看付款凭据与 requireReadScope 同口径，商户账号只比商户；上传仍走 checkScope
        if(VIEW.equals(permission)&&merchantRead(scope)) {
            if(!scope.merchant().equals(w.merchant()))throw ApiException.forbidden("PAYROLL_FINANCE_SCOPE_MISMATCH","无权读取或付款该项目、商户与发薪主体的工资");
            return w.merchant();
        }
        checkScope(w,scope);return w.merchant();
    }
    private Scope scope(String... permissions) {
        auth.requirePermissions(permissions);var actor=auth.currentActor();
        return new Scope(actor.id(),actor.merchantId(),auth.projectReach(actor,permissions));
    }
    private void checkScope(Wage w,Scope s) {
        if(!s.reach().covers(w.project()) || !s.reach().covers(w.payer())
                || (!s.reach().platformAdmin() && s.merchant()!=null && !s.merchant().equals(w.merchant())))
            throw ApiException.forbidden("PAYROLL_FINANCE_SCOPE_MISMATCH","无权读取或付款该项目、商户与发薪主体的工资");
    }
    private Wage lockWage(String id) {
        Wage first=wage(id,false);
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"staff-payroll:"+first.user());
        return wage(id,true);
    }
    private Wage wage(String id,boolean lock) {
        return jdbc.query("""
                SELECT id,user_id,month,project_id,merchant_id,payer_company_id,total_cent,status,scheme_version_id,
                       calculation_snapshot::text,payment_due_date,payment_due_basis::text,ar_ap_item_id
                  FROM staff_payroll_statements WHERE id=?
                """+(lock?" FOR UPDATE":""),(rs,n)->new Wage(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),
                rs.getString(6),rs.getLong(7),rs.getString(8),rs.getString(9),rs.getString(10),rs.getObject(11,LocalDate.class),rs.getString(12),rs.getString(13)),requireText(id,"工资单"))
                .stream().findFirst().orElseThrow(()->ApiException.notFound("PAYROLL_NOT_FOUND","工资单不存在"));
    }
    private ArApItemRecord payable(Wage w) {
        requireApprovedBasis(w);
        if(blank(w.payable()))throw invalid("工资尚未生成应付");
        var p=finance.findArApItemByIdForUpdate(w.payable()).orElseThrow(()->invalid("工资应付原件不存在"));checkPayable(w,p);
        // 工资支付日独立执行；后续银行事实只追加付款和更新余额，不回改已封日结快照。
        return p;
    }
    private void requireApprovedBasis(Wage w) {
        Integer count=jdbc.queryForObject("""
                SELECT count(*) FROM staff_payroll_statements p
                JOIN staff_payroll_scheme_versions s ON s.id=p.scheme_version_id
                JOIN staff_payroll_period_facts f ON f.id=p.facts_id
                WHERE p.id=? AND s.status='approved' AND f.status='approved'
                  AND s.user_id=p.user_id AND f.user_id=p.user_id AND f.scheme_version_id=s.id
                  AND s.project_id=p.project_id AND f.project_id=p.project_id
                  AND s.payer_company_id=p.payer_company_id AND f.payer_company_id=p.payer_company_id
                  AND s.merchant_id IS NOT DISTINCT FROM p.merchant_id
                  AND f.merchant_id IS NOT DISTINCT FROM p.merchant_id
                """,Integer.class,w.id());
        if(count==null || count!=1)throw invalid("工资须关联同员工、项目和发薪主体的已批准薪资方案及计薪依据");
    }
    private void checkPayable(Wage w,ArApItemRecord p) {
        if(!"staff_payroll".equals(p.sourceType()) || !w.id().equals(p.sourceId()) || !"payable".equals(p.direction())
                || !w.user().equals(p.counterpartyId()) || w.total()!=p.amountCent() || !Objects.equals(w.project(),p.projectId())
                || !Objects.equals(w.merchant(),p.merchantId()) || !Objects.equals(w.due(),p.dueDate()))
            throw invalid("工资与应付原件不一致，不得覆盖原金额或日期");
    }
    private Map<String,Object> attempt(String id) {
        return jdbc.queryForList("SELECT * FROM finance_payroll_payment_attempts WHERE id=?",requireText(id,"付款记录"))
                .stream().findFirst().orElseThrow(()->ApiException.notFound("PAYROLL_PAYMENT_NOT_FOUND","工资付款记录不存在"));
    }
    private void checkEvidence(String id,Wage w,List<String> ids) {
        if(ids==null || ids.isEmpty())throw invalid("须上传并核实本笔银行实际凭证");
        for(String attachmentId:ids) {
            var a=evidence.findAttachmentById(attachmentId).orElseThrow(()->invalid("银行凭证不存在"));
            if(!"staff_payroll_payment".equals(a.businessType()) || !id.equals(a.businessId()) || !Objects.equals(w.merchant(),a.merchantId()))
                throw invalid("银行凭证须绑定本笔工资付款及原商户范围");
            files.requireUploaded(a);
        }
    }
    private void appendEvent(String id,String status,Instant at,List<String> attachments,String reason,String actor) {
        jdbc.update("INSERT INTO finance_payroll_payment_events(id,attempt_id,status,result_at,evidence_attachment_ids,reason,recorded_by) VALUES(?,?,?,?,?::jsonb,?,?)",
                "salary_evt_"+UUID.randomUUID(),id,status,java.sql.Timestamp.from(at),json(attachments==null?List.of():attachments),reason,actor);
    }
    private Map<String,Object> write(String action,String key,Object request,Scope scope,String id,Supplier<Map<String,Object>> operation) {
        requireText(key,"幂等键");
        @SuppressWarnings("unchecked") Map<String,Object> result=idempotency.execute("payrollFinance:"+action,key,request,()->operation.get(),Map.class);
        audit.recordSuccess("finance.payroll."+action.split(":")[0],PAY,"staff_payroll_payment",id,null,key,Map.of("actor_id",scope.actorId()));return result;
    }
    /** 每个公开付款入口统一记失败；不记录请求正文，也不在幂等层内重复记。 */
    private void recordPaymentFailure(String id,String key,RuntimeException error) {
        audit.recordFailure("finance.payroll.payment_failed",PAY,"staff_payroll_payment",id,null,key,
                error instanceof ApiException a?a.code():"INTERNAL_ERROR");
    }
    private void requireTime(Instant at,Instant start) {if(at==null || at.isAfter(Instant.now()) || (start!=null && at.isBefore(start)))throw invalid("银行事实时间缺失、晚于当前时间或早于付款发起");}
    private String bankRef(String value,String label) {String ref=requireText(value,label).trim();if(ref.length()>256)throw invalid(label+"不能超过256字符");return ref;}
    private boolean emptyJson(String value) {return blank(value)||"{}".equals(value)||"null".equals(value);}
    private boolean blank(String v) {return v==null||v.isBlank();}
    private long number(Map<String,Object> row,String key) {return ((Number)row.get(key)).longValue();}
    private String token(String v) {return UUID.nameUUIDFromBytes(v.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-","");}
    private String json(Object v) {try{return mapper.writeValueAsString(v);}catch(Exception e){throw invalid("无法保存工资付款依据");}}
    private ApiException invalid(String message) {return ApiException.conflict("PAYROLL_PAYMENT_INVALID",message);}
}
