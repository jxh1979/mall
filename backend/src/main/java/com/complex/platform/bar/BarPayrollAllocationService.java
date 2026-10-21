package com.complex.platform.bar;

// 文件版本：V0.03

import static com.complex.platform.bar.BarPayrollSupport.*;
import static com.complex.platform.bar.BarPayrollRequests.*;
import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.access.staff.StaffService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.FinanceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 按原账单生成不可变薪资分配原件。未明来源单独保留，不能被零元结清。 */
@Service
public class BarPayrollAllocationService {
    private final BarPayrollSupport p;
    private final BarPayrollRuleService rules;
    private final BarServiceResponsibilityService responsibilities;
    private final IdempotencyService idem;
    private final AuditEventService audit;
    private final BarPayrollRefundService refunds;
    private final FinanceRepository finance;
    private final com.complex.platform.common.RevenueRefreshMarks revenueRefreshMarks;
    public BarPayrollAllocationService(BarPayrollSupport p,BarPayrollRuleService rules,
            BarServiceResponsibilityService responsibilities,IdempotencyService idem,AuditEventService audit,BarPayrollRefundService refunds,FinanceRepository finance,
            com.complex.platform.common.RevenueRefreshMarks revenueRefreshMarks) {
        this.p=p;this.rules=rules;this.responsibilities=responsibilities;this.idem=idem;this.audit=audit;this.refunds=refunds;this.finance=finance;
        this.revenueRefreshMarks=revenueRefreshMarks;
    }
    record Component(String id,String bill,String session,String table,String project,String merchant,LocalDate date,
            String type,long amount,String chargeRule,String crewSnapshot) {}
    record Key(String user,String payer,String type,String rule) {}
    record Calculation(String bill,String project,String merchant,String month,long tips,long fees,long tipPayable,
            long feePayable,long withheld,long residual,String settlementCompany,Map<Key,Long> amounts,Object facts) {}
    record Shift(String id,String user,Instant start,Instant end,Instant originalStart,Instant originalEnd,String payer) {}

    /** 仅工资服务内部调用；调用方负责工资人员和范围授权。 */
    public List<BarPayrollAllocation> confirmedAllocations(String userId,String month) {
        month(month);
        return p.jdbc.query("SELECT * FROM bar_payroll_allocations WHERE user_id=? AND month=? AND status='confirmed' ORDER BY id",
            this::allocationRow,userId,month);
    }
    /** 仅内部预检。未结补充收入不妨碍已核实基础工资按期支付。 */
    public List<String> unresolvedAllocationSources(String userId,String month) {
        var range=month(month);
        var profile=p.staff.findProfileByUserId(userId).orElse(null);
        if(profile==null)return List.of("员工档案缺失，无法核对服务收入来源");
        var sources=p.jdbc.queryForList("""
            SELECT DISTINCT c.source_bill_id FROM bar_payroll_bill_components c
            WHERE c.business_date>=? AND c.business_date<?
            AND (c.merchant_id=? OR EXISTS(SELECT 1 FROM bar_service_responsibilities s WHERE s.session_id=c.session_id AND s.user_id=?)
              OR EXISTS(SELECT 1 FROM bar_payroll_rules r WHERE r.rule_kind='allocation' AND r.merchant_id=c.merchant_id AND r.rule_json->'participants' @> ?::jsonb))
            ORDER BY c.source_bill_id
            """,String.class,range.atDay(1),range.plusMonths(1).atDay(1),profile.merchantId(),userId,p.encode(List.of(Map.of("user_id",userId))));
        List<String> result=new ArrayList<>();
        for(String source:sources) {
            try {
                var calculation=calculate(source,month);
                if(!currentCommitted(calculation))result.add(source+":服务收入尚未按当前原件完成月结");
            }catch(ApiException e){result.add(source+":"+e.getMessage());}
        }
        if(profile.merchantId()!=null)result.addAll(legacySources(profile.merchantId(),range));
        return List.copyOf(result);
    }
    public List<MonthRun> overview(String project,String merchant,String month) {
        p.requireScope(project,merchant,VIEW);var period=month(month);
        List<MonthRun> rows=new ArrayList<>(p.jdbc.query("SELECT * FROM bar_payroll_runs WHERE project_id=? AND merchant_id=? AND month=? ORDER BY created_at,id",
            this::runRow,project,merchant,month));
        for(String source:sources(project,merchant,period)) {
            try {var c=calculate(source,month);if(!currentCommitted(c))rows.add(pending(c.project(),c.merchant(),month,source,"服务收入待月结"));}
            catch(ApiException e){rows.add(pending(project,merchant,month,source,e.getMessage()));}
        }
        for(String source:legacySources(merchant,period))rows.add(pending(project,merchant,month,source,"历史账单缺少不可变服务来源，须走有证据的补录复核"));
        return rows;
    }
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability="bar.payroll.manage")
    public List<MonthRun> close(CloseMonth r) {
        if(r==null)throw invalid("须填写月结范围");
        p.requireScope(requireText(r.projectId(),"project_id"),requireText(r.merchantId(),"merchant_id"),MANAGE);
        var period=month(r.month());
        if(!period.isBefore(YearMonth.now()))throw invalid("本月尚未结束，暂不能确认月结");
        return Arrays.asList(idem.execute("barPayrollMonthClose",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            p.lock("month:"+r.projectId()+":"+r.merchantId()+":"+r.month());
            // BSM L1142/L1143：跨主体拨付是普通内部结算应收应付，按三个付账日结清；到期取月末后第一个付账日，付账日未配置则整次月结不写。
            LocalDate due=StaffService.nextInternalPayDayAfter(p.staff.internalSettlementPayDays(),period.atEndOfMonth());
            List<MonthRun> result=new ArrayList<>();
            for(String bill:sources(r.projectId(),r.merchantId(),period)) {
                p.lock("refund-source:"+bill);
                Calculation c;
                try {c=calculate(bill,r.month());}
                catch(ApiException e){result.add(pending(r.projectId(),r.merchantId(),r.month(),bill,e.getMessage()));continue;}
                result.add(commit(c,due));
            }
            for(String source:legacySources(r.merchantId(),period))result.add(pending(r.projectId(),r.merchantId(),r.month(),source,"来源缺失，不能自动倒推分配"));
            audit.recordSuccess("bar.payroll.month_processed",MANAGE,"bar_payroll_run",null,null,r.idempotencyKey(),
                Map.of("project_id",r.projectId(),"merchant_id",r.merchantId(),"month",r.month(),"source_count",result.size()));
            return result.toArray(MonthRun[]::new);
        },MonthRun[].class));
    }
    /** 内部收入预检返回待结原因，不阻断其他商户已确认部分的展示。 */
    public List<String> sourceUnresolved(String bill,String month) {
        try {return currentCommitted(calculate(bill,month))?List.of():List.of("服务收入尚未按当前原件月结");}
        catch(ApiException e){return List.of(e.getMessage());}
    }
    private List<String> sources(String project,String merchant,YearMonth month) {
        return p.jdbc.queryForList("SELECT DISTINCT source_bill_id FROM bar_payroll_bill_components WHERE project_id=? AND merchant_id=? AND business_date>=? AND business_date<? ORDER BY source_bill_id",
            String.class,project,merchant,month.atDay(1),month.plusMonths(1).atDay(1));
    }
    private List<String> legacySources(String merchant,YearMonth month) {
        return p.jdbc.queryForList("""
            SELECT DISTINCT b.id FROM bar_bills b LEFT JOIN bar_orders o ON o.id=b.order_id
            WHERE b.merchant_id=? AND b.created_at>=? AND b.created_at<?
              AND ((b.tip_amount_cent>0 AND NOT EXISTS(SELECT 1 FROM bar_payroll_bill_components c JOIN bar_payroll_collection_links l ON l.source_bill_id=c.source_bill_id WHERE l.collection_bill_id=b.id AND l.active AND c.component_type='tip'))
                OR EXISTS(SELECT 1 FROM bar_bill_adjustments a WHERE a.bill_id=b.id AND a.adjustment_type='service_fee'
                   AND NOT EXISTS(SELECT 1 FROM bar_payroll_bill_components c WHERE c.source_bill_id=b.id AND c.component_type='service_fee')))
            ORDER BY b.id
            """,String.class,merchant,ts(month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()),ts(month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
    }
    private Calculation calculate(String bill,String month) {
        var components=p.jdbc.query("SELECT * FROM bar_payroll_bill_components WHERE source_bill_id=? ORDER BY id",
            (rs,n)->new Component(rs.getString("id"),rs.getString("source_bill_id"),rs.getString("session_id"),rs.getString("source_table_id"),
                rs.getString("project_id"),rs.getString("merchant_id"),rs.getDate("business_date").toLocalDate(),rs.getString("component_type"),
                rs.getLong("amount_cent"),rs.getString("charge_rule_id"),rs.getString("source_snapshot")),bill);
        if(components.isEmpty())throw conflict("BAR_PAYROLL_SOURCE_REQUIRED","缺少收费来源原件");
        var first=components.getFirst();String sessionId=first.session();
        if(components.stream().anyMatch(c->!sessionId.equals(c.session()) || !month.equals(c.date().toString().substring(0,7))))
            throw conflict("BAR_PAYROLL_SOURCE_MISMATCH","原账单来源场次或营业月份不一致");
        var collection=p.jdbc.queryForList("SELECT collection_bill_id FROM bar_payroll_collection_links WHERE source_bill_id=? AND active ORDER BY collection_bill_id",String.class,bill);
        if(collection.isEmpty())throw conflict("BAR_PAYROLL_COLLECTION_REQUIRED","缺少真实收款账单关联");
        List<Object> collectionFacts=new ArrayList<>();
        for(String id:collection) {
            var rows=p.jdbc.queryForList("""
                SELECT b.id,b.settlement_status,b.payable_amount_cent,b.tip_amount_cent,b.paid_amount_cent,
                  COALESCE((SELECT sum(s.settlement_amount_cent) FROM bar_bill_settlements s WHERE s.bill_id=b.id AND NOT s.reversed),0) receipt_cent,
                  COALESCE((SELECT sum(a.amount_cent) FROM bar_bill_adjustments a WHERE a.bill_id=b.id AND a.adjustment_type='refund'),0) refund_cent
                FROM bar_bills b WHERE b.id=?
                """,id);
            if(rows.size()!=1)throw conflict("BAR_PAYROLL_COLLECTION_REQUIRED","收款账单不存在");
            var row=rows.getFirst();long due=Math.addExact(number(row,"payable_amount_cent")+number(row,"tip_amount_cent"),number(row,"refund_cent"));
            if(!"settled".equals(row.get("settlement_status")) || number(row,"paid_amount_cent")<due || number(row,"receipt_cent")<due)
                throw conflict("BAR_PAYROLL_NOT_COLLECTED","原收款账单尚未完成真实收款");
            refunds.requireResolved(id);
            collectionFacts.add(row);
        }
        var session=responsibilities.session(sessionId,false);
        var duties=responsibilities.responsibilityRows(sessionId);
        if(duties.stream().anyMatch(d->d.endedAt()==null))throw conflict("BAR_SERVICE_NOT_FINISHED","场次服务仍未结束或交接区间未闭合");
        if(session.closedAt()==null)throw conflict("BAR_SERVICE_NOT_FINISHED","尚未记录实际服务结束");
        Map<Key,Long> amounts=new HashMap<>();
        long tipTotal=0,feeTotal=0,tipPay=0,feePay=0,withheld=0,residual=0;
        List<Object> rulesUsed=new ArrayList<>();
        List<Component> effectiveComponents=new ArrayList<>();
        for(String type:List.of("tip","service_fee")) {
            var same=components.stream().filter(c->type.equals(c.type())).toList();if(same.isEmpty())continue;
            var original=same.getFirst();
            long amount=Math.subtractExact(same.stream().mapToLong(Component::amount).sum(),refunds.refunded(bill,type));
            if(amount<0)throw conflict("BAR_REFUND_COMPONENT_EXCEEDED","成功退款超过原分项");
            if(amount>0)effectiveComponents.add(new Component(original.id(),original.bill(),original.session(),original.table(),original.project(),
                original.merchant(),original.date(),type,amount,original.chargeRule(),mergedCrewSnapshots(same)));
        }
        for(var c:effectiveComponents) {
            Map<String,BigDecimal> weights;Map<String,String> payers=new HashMap<>();String version;long payable;
            if("tip".equals(c.type())) {
                tipTotal=Math.addExact(tipTotal,c.amount());payable=c.amount();version="tip_responsibility_V0.01";
                String tableType=p.jdbc.queryForObject("SELECT table_type FROM bar_tables WHERE id=?",String.class,c.table());
                if(Set.of("card_seat","vip").contains(tableType)) {
                    List<BarPayrollMath.Interval> all=duties.stream().map(d->new BarPayrollMath.Interval(d.startedAt(),d.endedAt())).toList();
                    long covered=BarPayrollMath.coveredMillis(all,session.openedAt(),session.closedAt());
                    if(covered!=Duration.between(session.openedAt(),session.closedAt()).toMillis())
                        throw conflict("BAR_SERVICE_HISTORY_INCOMPLETE","实际服务责任时间有空档，不能按当前负责人补齐");
                    weights=new TreeMap<>();
                    for(var d:duties) {
                        String key=d.userId()+"|"+d.payerCompanyId();
                        weights.merge(key,BigDecimal.valueOf(Duration.between(d.startedAt(),d.endedAt()).toMillis()),BigDecimal::add);
                        payers.put(key,d.payerCompanyId());
                    }
                } else {
                    weights=new TreeMap<>();
                    for(var shift:regionalShifts(c,session.openedAt(),session.closedAt())) {
                        String key=shift.user()+"|"+shift.payer();
                        weights.put(key,BigDecimal.ONE);payers.put(key,shift.payer());
                    }
                    if(weights.isEmpty())throw conflict("BAR_REGION_SCHEDULE_REQUIRED","散台或吧台缺少本区域当班人员事实");
                }
            } else {
                feeTotal=Math.addExact(feeTotal,c.amount());
                var rule=rules.effective("allocation",c.project(),c.merchant(),c.date());version=rule.id();rulesUsed.add(rule);
                long retained=MoneyRounding.bpsOf(c.amount(),rule.values().withheldBps());withheld=Math.addExact(withheld,retained);
                payable=Math.subtractExact(c.amount(),retained);weights=new TreeMap<>();
                var shifts=regionalShifts(c,session.openedAt(),session.closedAt());
                for(var participant:rule.values().participants()) {
                    var assigned=shifts.stream().filter(s->participant.userId().equals(s.user())).toList();
                    if(assigned.isEmpty())continue;
                    if(participant.payerCompanyId()==null)throw conflict("BAR_PAYER_REQUIRED","分配规则缺少已确认发薪主体");
                    BigDecimal fraction=BigDecimal.ONE;
                    if("actual_shift_fraction".equals(participant.partialCoverMethod())) {
                        fraction=BigDecimal.ZERO;
                        for(var shift:assigned) {
                            long actual=Duration.between(shift.start(),shift.end()).toMillis();
                            long planned=Duration.between(shift.originalStart(),shift.originalEnd()).toMillis();
                            if(planned<=0)throw conflict("BAR_SHIFT_BASIS_REQUIRED","顶班缺少原班次完整时段");
                            fraction=fraction.add(BigDecimal.valueOf(actual).divide(BigDecimal.valueOf(planned),12,RoundingMode.HALF_UP));
                        }
                    }
                    String key=participant.userId()+"|"+participant.payerCompanyId();
                    weights.put(key,BigDecimal.valueOf(participant.shareUnits()).multiply(fraction));
                    payers.put(key,participant.payerCompanyId());
                }
                if(payable>0 && weights.isEmpty())throw conflict("BAR_SERVICE_FEE_PARTICIPANTS_REQUIRED","本场无符合已批准分配规则及排班的人员");
            }
            if(payable==0)continue;
            var distribution=BarPayrollMath.distribute(payable,weights);residual=Math.addExact(residual,distribution.residualCent());
            for(var entry:distribution.amounts().entrySet()) {
                String user=entry.getKey().substring(0,entry.getKey().lastIndexOf('|'));
                amounts.merge(new Key(user,payers.get(entry.getKey()),c.type(),version),entry.getValue(),Math::addExact);
                if("tip".equals(c.type()))tipPay=Math.addExact(tipPay,entry.getValue());else feePay=Math.addExact(feePay,entry.getValue());
            }
        }
        List<Map<String,Object>> calculated=amounts.entrySet().stream().sorted(Comparator.comparing(e->e.getKey().toString()))
            .map(e->Map.<String,Object>of("key",e.getKey(),"amount",e.getValue())).toList();
        return new Calculation(bill,first.project(),first.merchant(),month,tipTotal,feeTotal,tipPay,feePay,withheld,residual,
            p.settlementCompany(first.merchant()),amounts,Map.of("source_bill_id",bill,"components",components,"collections",collectionFacts,"responsibilities",duties,"rules",rulesUsed,"calculated",calculated,
                "refunded_tip_cent",refunds.refunded(bill,"tip"),"refunded_service_fee_cent",refunds.refunded(bill,"service_fee")));
    }
    private String mergedCrewSnapshots(List<Component> components) {
        Map<String,com.fasterxml.jackson.databind.JsonNode> people=new TreeMap<>();
        try {
            for(var component:components)for(var person:p.json.readTree(component.crewSnapshot())) {
                String key=person.path("shift_id").asText()+"|"+person.path("user_id").asText();
                var prior=people.putIfAbsent(key,person);
                if(prior!=null && !prior.path("payer_company_id").equals(person.path("payer_company_id")))
                    throw conflict("BAR_REGION_PAYER_SNAPSHOT_REQUIRED","同一原班次的发薪主体快照有冲突，须复核原件");
            }
            return p.encode(people.values());
        }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
    }
    private List<Shift> regionalShifts(Component c,Instant start,Instant end) {
        var rows=p.jdbc.query("""
            SELECT s.id,s.user_id,s.start_at,s.end_at,
                   COALESCE(o.start_at,s.start_at) original_start,COALESCE(o.end_at,s.end_at) original_end
            FROM staff_shift_schedules s
            JOIN bar_tables t ON t.id=?
            LEFT JOIN staff_shift_covers cover ON cover.cover_shift_id=s.id
            LEFT JOIN staff_shift_schedules o ON o.id=cover.shift_id
            WHERE s.start_at<? AND s.end_at>?
              AND ((?='tip' AND s.point_space_ids @> jsonb_build_array(t.space_id))
                OR (?='service_fee' AND EXISTS(SELECT 1 FROM spaces area WHERE area.merchant_id=? AND area.project_id=?
                    AND s.point_space_ids @> jsonb_build_array(area.id))))
            ORDER BY s.user_id,s.start_at,s.id
            """,(rs,n)->new Shift(rs.getString("id"),rs.getString("user_id"),rs.getTimestamp("start_at").toInstant(),
                rs.getTimestamp("end_at").toInstant(),rs.getTimestamp("original_start").toInstant(),rs.getTimestamp("original_end").toInstant(),null),c.table(),ts(end),ts(start),c.type(),c.type(),c.merchant(),c.project());
        List<Shift> result=new ArrayList<>();
        for(var s:rows) {
            var covers=p.jdbc.query("SELECT start_at,end_at FROM staff_shift_covers WHERE shift_id=? ORDER BY start_at,id",
                (rs,n)->new BarPayrollMath.Interval(rs.getTimestamp(1).toInstant(),rs.getTimestamp(2).toInstant()),s.id());
            Instant cursor=s.start();
            for(var cover:covers) {
                if(cover.from().isAfter(cursor))result.add(new Shift(s.id(),s.user(),cursor,cover.from(),s.originalStart(),s.originalEnd(),snapshotPayer(c,s.id(),s.user())));
                if(cover.to().isAfter(cursor))cursor=cover.to();
            }
            if(s.end().isAfter(cursor))result.add(new Shift(s.id(),s.user(),cursor,s.end(),s.originalStart(),s.originalEnd(),snapshotPayer(c,s.id(),s.user())));
        }
        return result.stream().filter(s->s.start().isBefore(end) && s.end().isAfter(start)).toList();
    }
    private String snapshotPayer(Component c,String shiftId,String user) {
        if("service_fee".equals(c.type()))return ""; // 服务费使用双方批准规则中锁定的发薪主体。
        try {
            var nodes=p.json.readTree(c.crewSnapshot());
            for(var node:nodes)if(shiftId.equals(node.path("shift_id").asText()) && user.equals(node.path("user_id").asText())
                    && !node.path("payer_company_id").asText().isBlank())return node.path("payer_company_id").asText();
        }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
        throw conflict("BAR_REGION_PAYER_SNAPSHOT_REQUIRED","区域排班已变更或缺少原发薪主体快照，须补录复核，不能倒查当前公司");
    }
    private boolean currentCommitted(Calculation c) {
        return p.jdbc.queryForObject("SELECT count(*) FROM bar_payroll_runs WHERE project_id=? AND merchant_id=? AND month=? AND source_fingerprint=?",
            Integer.class,c.project(),c.merchant(),c.month(),fingerprint(c))>0;
    }
    private MonthRun commit(Calculation c,LocalDate due) {
        String fingerprint=fingerprint(c);
        var existing=p.jdbc.query("SELECT * FROM bar_payroll_runs WHERE project_id=? AND merchant_id=? AND month=? AND source_fingerprint=?",
            this::runRow,c.project(),c.merchant(),c.month(),fingerprint);
        if(!existing.isEmpty())return existing.getFirst();
        Map<Key,Long> before=new HashMap<>();
        p.jdbc.query("SELECT * FROM bar_payroll_allocations WHERE project_id=? AND merchant_id=? AND month=? AND source_bill_ids @> ?::jsonb",
            rs->{var a=allocationRow(rs,0);before.merge(new Key(a.userId(),a.payerCompanyId(),a.componentType(),a.ruleVersionId()),a.amountCent(),Math::addExact);},
            c.project(),c.merchant(),c.month(),p.encode(List.of(c.bill())));
        String id=id("bprun");
        p.jdbc.update("INSERT INTO bar_payroll_runs(id,project_id,merchant_id,month,source_fingerprint,source_snapshot,collected_tip_cent,collected_service_fee_cent,payable_tip_cent,payable_service_fee_cent,withheld_service_fee_cent,rounding_residual_cent,settlement_entity_id,status,created_by,created_at) VALUES(?,?,?,?,?,?::jsonb,?,?,?,?,?,?,?,'confirmed',?,clock_timestamp())",
            id,c.project(),c.merchant(),c.month(),fingerprint,p.encode(c.facts()),c.tips(),c.fees(),c.tipPayable(),c.feePayable(),c.withheld(),c.residual(),c.settlementCompany(),p.auth.currentActor().id());
        // ROOT-04-R4：服务费扣留按原营业日计酒吧收入（BarRevenueSourceService.withheldServiceFees），跨期月结与服务费退款后重算都在此出新原件，登记原营业日待看板重算。
        revenueRefreshMarks.mark(p.jdbc.queryForList("SELECT DISTINCT business_date FROM bar_payroll_bill_components WHERE source_bill_id=? AND component_type='service_fee'",LocalDate.class,c.bill()));
        Set<Key> keys=new HashSet<>(before.keySet());keys.addAll(c.amounts().keySet());
        Instant now=Instant.now();
        for(var k:keys) {
            long delta=Math.subtractExact(c.amounts().getOrDefault(k,0L),before.getOrDefault(k,0L));if(delta==0)continue;
            String allocation=id("bpa");
            p.jdbc.update("INSERT INTO bar_payroll_allocations(id,run_id,user_id,month,component_type,amount_cent,merchant_id,project_id,payer_company_id,source_bill_ids,rule_version_id,status) VALUES(?,?,?,?,?,?,?,?,?,?::jsonb,?,'confirmed')",
                allocation,id,k.user(),c.month(),k.type(),delta,c.merchant(),c.project(),k.payer(),p.encode(List.of(c.bill())),k.rule());
            // BSM L1143：应付本人的报酬由该酒吧按月拨付给人员所属主体，作为平台内部普通内部结算应收应付；发薪主体即酒吧结算主体时不造债务。
            // 每条分配单独一行：正额为酒吧对所属主体的应付，负额（退款差额）单列应收，不轧差；幂等由 run 指纹 + ON CONFLICT(id) DO NOTHING 保证。
            if(k.payer().equals(c.settlementCompany()))continue;
            String item="arap_bpt_"+allocation;long amount=Math.abs(delta);
            finance.appendArApItem(new ArApItemRecord(item,"ARAP-BPT-"+allocation.substring(4),delta>0?"payable":"receivable",k.payer(),
                "bar_payroll_transfer",allocation,amount,0,amount,"CNY",due,c.month(),delta>0?"pending_payment":"pending_collection",now,now,c.merchant(),c.project()));
            finance.saveArApItemSource(new ArApItemSourceRecord("arap_src_"+item,item,"bar_payroll_transfer",allocation,amount,now));
        }
        return p.jdbc.queryForObject("SELECT * FROM bar_payroll_runs WHERE id=?",this::runRow,id);
    }
    private String fingerprint(Calculation c) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(p.encode(c.facts()).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private MonthRun pending(String project,String merchant,String month,String source,String reason) {
        return new MonthRun(null,project,merchant,month,0,0,0,0,0,0,null,"pending",List.of(),List.of(source+":"+reason),null);
    }
    private static long number(Map<String,Object> row,String key) {return ((Number)row.get(key)).longValue();}
    private BarPayrollAllocation allocationRow(ResultSet rs,int n)throws SQLException {
        return new BarPayrollAllocation(rs.getString("id"),rs.getString("user_id"),rs.getString("month"),rs.getString("component_type"),
            rs.getLong("amount_cent"),rs.getString("merchant_id"),rs.getString("project_id"),rs.getString("payer_company_id"),
            p.strings(rs.getString("source_bill_ids")),rs.getString("rule_version_id"),rs.getString("status"));
    }
    private MonthRun runRow(ResultSet rs,int n)throws SQLException {
        String id=rs.getString("id");
        return new MonthRun(id,rs.getString("project_id"),rs.getString("merchant_id"),rs.getString("month"),
            rs.getLong("collected_tip_cent"),rs.getLong("collected_service_fee_cent"),rs.getLong("payable_tip_cent"),rs.getLong("payable_service_fee_cent"),
            rs.getLong("withheld_service_fee_cent"),rs.getLong("rounding_residual_cent"),rs.getString("settlement_entity_id"),rs.getString("status"),
            p.jdbc.query("SELECT * FROM bar_payroll_allocations WHERE run_id=? ORDER BY user_id,component_type,id",this::allocationRow,id),
            List.of(),rs.getTimestamp("created_at").toInstant());
    }
}
