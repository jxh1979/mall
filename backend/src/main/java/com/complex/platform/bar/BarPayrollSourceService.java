package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarPayrollSupport.*;
import com.complex.platform.common.money.MoneyRounding;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

/** 在原业务事务内记录收费来源和拆合收款去向，不重新解释当前台位人员。 */
@Service
public class BarPayrollSourceService {
    private final BarPayrollSupport p;
    private final BarPayrollRuleService rules;
    BarPayrollSourceService(BarPayrollSupport p,BarPayrollRuleService rules) {this.p=p;this.rules=rules;}
    record Origin(String session,String table,String project,String merchant,LocalDate date) {}
    Origin origin(BarBillRecord bill) {
        return p.jdbc.query("SELECT * FROM bar_order_service_sources WHERE order_id=?",(rs,n)->new Origin(
            rs.getString("session_id"),rs.getString("table_id"),rs.getString("project_id"),rs.getString("merchant_id"),rs.getDate("business_date").toLocalDate()),
            bill.orderId()).stream().findFirst().orElseThrow(()->conflict("BAR_SERVICE_SOURCE_REQUIRED","订单没有创建时锁定的服务场次，须按补录复核流程补齐，不能倒推当前负责人"));
    }
    long requiredCharge(BarBillRecord bill) {
        String type=p.jdbc.queryForObject("SELECT table_type FROM bar_tables WHERE id=?",String.class,bill.tableId());
        if(!Set.of("card_seat","vip").contains(type))return 0;
        var source=origin(bill);
        var rule=rules.effective("charge",source.project(),source.merchant(),source.date());
        return MoneyRounding.bpsOf(bill.subtotalAmountCent(),rule.values().rateBps());
    }
    void recordComponent(BarBillRecord bill,String type,long amount,String event) {
        event=eventKey(event);
        if(amount<=0)throw invalid("服务收入必须为正数");
        var source=origin(bill);
        String ruleId=null;
        if("service_fee".equals(type)) {
            var rule=rules.effective("charge",source.project(),source.merchant(),source.date());ruleId=rule.id();
            long prior=p.jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM bar_payroll_bill_components WHERE source_bill_id=? AND component_type='service_fee'",Long.class,bill.id());
            if(Math.addExact(prior,amount)!=MoneyRounding.bpsOf(bill.subtotalAmountCent(),rule.values().rateBps()))
                throw conflict("BAR_SERVICE_FEE_RULE_MISMATCH","服务费须等于折前消费乘已批准费率，不得任意重复加收");
        }
        long base="tip".equals(type)?amount:bill.subtotalAmountCent();
        if(base<=0)throw conflict("BAR_PAYROLL_BASE_REQUIRED","须先明确该收入的真实消费基数");
        p.jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,component_type,amount_cent,original_base_cent,charge_rule_id,source_event_id,source_snapshot,collection_bill_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?)",
            id("bpc"),bill.id(),source.session(),source.table(),source.project(),source.merchant(),source.date(),type,amount,base,ruleId,event,p.encode(crewSnapshot(source)),bill.id());
        link(bill.id(),bill.id());
    }
    private List<Map<String,Object>> crewSnapshot(Origin origin) {
        return p.jdbc.queryForList("""
            SELECT s.id shift_id,s.user_id,p.company_id payer_company_id
            FROM staff_shift_schedules s JOIN bar_tables t ON t.id=?
            JOIN bar_table_sessions session ON session.id=?
            JOIN LATERAL (SELECT company_id FROM staff_profiles WHERE user_id=s.user_id ORDER BY created_at DESC LIMIT 1) p ON true
            WHERE s.start_at<COALESCE(session.closed_at,clock_timestamp()) AND s.end_at>session.opened_at
                AND s.point_space_ids @> jsonb_build_array(t.space_id)
            ORDER BY s.id
            """,origin.table(),origin.session());
    }
    void recordTip(BarBillRecord collection,String sourceBillId,long amount,String event,BarRepository bars) {
        List<String> sources=sourceBills(collection.id());
        String selected=sourceBillId;
        if(selected==null) {
            if(sources.size()>1)throw conflict("BAR_TIP_SOURCE_REQUIRED","合单后新增小费须选择客人原账单，不能全归主台");
            selected=sources.isEmpty()?collection.id():sources.getFirst();
        }
        if(!selected.equals(collection.id()) && !sources.contains(selected))throw invalid("小费原账单不属于本收款账单");
        var source=bars.findBillById(selected).orElseThrow(()->invalid("小费原账单不存在"));
        recordComponent(source,"tip",amount,event);
        if(!selected.equals(collection.id())) {
            p.jdbc.update("UPDATE bar_payroll_collection_links SET active=false WHERE source_bill_id=? AND collection_bill_id=?",selected,selected);
            link(selected,collection.id());
            p.jdbc.update("UPDATE bar_payroll_bill_components SET collection_bill_id=? WHERE source_event_id=?",collection.id(),eventKey(event));
        }
    }
    void validateCashCollection(BarBillRecord bill,long amount) {
        long cash=p.jdbc.queryForObject("SELECT COALESCE(sum(settlement_amount_cent),0) FROM bar_bill_settlements WHERE bill_id=? AND settle_channel='cash' AND NOT reversed",Long.class,bill.id());
        if(Math.addExact(cash,amount)>bill.payableAmountCent())
            throw conflict("BAR_CASH_TIP_DIRECT_TO_STAFF","现金收款不能包含公司代收小费；现金小费由客人直接交给服务人员");
    }
    void validateSplit(BarBillRecord bill,long amount) {
        long fees=p.jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM bar_payroll_bill_components WHERE collection_bill_id=? AND component_type='service_fee'",Long.class,bill.id());
        if(amount>bill.payableAmountCent()-fees || amount>bill.subtotalAmountCent())
            throw conflict("BAR_SPLIT_GOODS_EXCEEDED","拆单只拆商品消费，服务费及小费保留原收款账单；不能拆超商品应付");
    }
    void recordSplit(String original,String generated) {
        ensureCollection(original);
        for(String source:sourceBills(original))link(source,generated);
    }
    void recordMerge(String primary,String secondary) {
        ensureCollection(primary);ensureCollection(secondary);
        for(String source:sourceBills(secondary))link(source,primary);
        p.jdbc.update("UPDATE bar_payroll_collection_links SET active=false WHERE collection_bill_id=?",secondary);
        p.jdbc.update("UPDATE bar_payroll_bill_components SET collection_bill_id=? WHERE collection_bill_id=?",primary,secondary);
    }
    private void ensureCollection(String bill) {
        if(p.jdbc.queryForObject("SELECT count(*) FROM bar_payroll_collection_links WHERE collection_bill_id=? AND active",Integer.class,bill)==0)link(bill,bill);
    }
    private List<String> sourceBills(String collection) {
        return p.jdbc.queryForList("SELECT source_bill_id FROM bar_payroll_collection_links WHERE collection_bill_id=? AND active ORDER BY source_bill_id",String.class,collection);
    }
    private void link(String source,String collection) {
        p.jdbc.update("INSERT INTO bar_payroll_collection_links(source_bill_id,collection_bill_id,active) VALUES(?,?,true) ON CONFLICT(source_bill_id,collection_bill_id) DO UPDATE SET active=true",source,collection);
    }
}
