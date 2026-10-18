package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarPayrollSupport.*;
import com.complex.platform.bar.BarBillAdjustmentRequest.RefundLine;
import java.util.*;
import org.springframework.stereotype.Service;

/** B01：真实原账单和分项经原退款审批核实；不从总退款猜比例。 */
@Service
public class BarPayrollRefundService {
    private final BarPayrollSupport p;
    BarPayrollRefundService(BarPayrollSupport p){this.p=p;}
    public record RefundSource(String sourceBillId,String tableId,Long remainingGoodsCent,long remainingServiceFeeCent,long remainingTipCent) {}
    public record PriorLine(String adjustmentId,String sourceBillId,String componentType,long amountCent,String paymentStatus) {}
    public record RefundContext(String billId,long remainingGoodsAndServiceFeeCent,long remainingTipCent,List<RefundSource> sources,List<PriorLine> priorLines) {}
    public RefundContext context(String billId) {
        var bill=p.jdbc.queryForMap("SELECT * FROM bar_bills WHERE id=?",billId);
        var scope=p.tableScope((String)bill.get("table_id"));
        p.requireScope(scope.project(),scope.merchant(),"bar.bill.refund");
        p.auth.requirePermission("bar.bill.adjust");
        var ids=p.jdbc.queryForList("SELECT source_bill_id FROM bar_payroll_collection_links WHERE collection_bill_id=? AND active ORDER BY source_bill_id",String.class,billId);
        if(ids.isEmpty())ids=List.of(billId);
        List<RefundSource> sources=new ArrayList<>();
        for(String source:ids) {
            String table=p.jdbc.queryForObject("SELECT table_id FROM bar_bills WHERE id=?",String.class,source);
            long fee=remaining(source,"service_fee",billId),tip=remaining(source,"tip",billId);
            Long goods=ids.size()==1 ? Math.max(0L,((Number)bill.get("payable_amount_cent")).longValue()-fee) : null;
            sources.add(new RefundSource(source,table,goods,fee,tip));
        }
        var prior=p.jdbc.query("""
            SELECT r.*,CASE WHEN (SELECT COALESCE(sum(p.amount_cent),0) FROM ar_ap_payments p WHERE p.ar_ap_item_id=r.ar_ap_item_id)>=a.amount_cent
              THEN 'paid' ELSE 'pending' END payment_status
            FROM bar_payroll_refund_lines r JOIN bar_bill_adjustments a ON a.id=r.adjustment_id
            WHERE r.collection_bill_id=? ORDER BY r.created_at,r.id
            """,(rs,n)->new PriorLine(rs.getString("adjustment_id"),rs.getString("source_bill_id"),rs.getString("component_type"),rs.getLong("amount_cent"),rs.getString("payment_status")),billId);
        return new RefundContext(billId,((Number)bill.get("payable_amount_cent")).longValue(),((Number)bill.get("tip_amount_cent")).longValue(),sources,prior);
    }
    private long remaining(String source,String type,String collection) {
        long original=p.jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM bar_payroll_bill_components WHERE source_bill_id=? AND component_type=? AND collection_bill_id=?",Long.class,source,type,collection);
        long reserved=p.jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM bar_payroll_refund_lines WHERE source_bill_id=? AND component_type=? AND collection_bill_id=?",Long.class,source,type,collection);
        return Math.max(0L,original-reserved);
    }
    List<RefundLine> validate(BarBillRecord collection,List<RefundLine> requested,long total) {
        var sourceIds=p.jdbc.queryForList("SELECT source_bill_id FROM bar_payroll_collection_links WHERE collection_bill_id=? AND active ORDER BY source_bill_id",String.class,collection.id());
        if(sourceIds.isEmpty())sourceIds=List.of(collection.id());
        List<RefundLine> lines=requested==null?List.of():List.copyOf(requested);
        // 旧单若只有一份商品且从未涉及服务费或小费，分项唯一，不引入任何比例或归属判断。
        if(lines.isEmpty() && sourceIds.size()==1 && collection.tipAmountCent()==0
                && p.jdbc.queryForObject("SELECT count(*) FROM bar_bill_adjustments WHERE bill_id=? AND adjustment_type='service_fee'",Integer.class,collection.id())==0
                && p.jdbc.queryForObject("SELECT count(*) FROM bar_payroll_bill_components WHERE source_bill_id=?",Integer.class,sourceIds.getFirst())==0)
            lines=List.of(new RefundLine(sourceIds.getFirst(),"goods",total));
        if(lines.isEmpty())throw conflict("BAR_REFUND_BREAKDOWN_REQUIRED","请明确实际原账单及商品、服务费、小费退款明细");
        Set<String> unique=new HashSet<>();long sum=0;
        for(var line:lines) {
            if(line==null || !sourceIds.contains(line.sourceBillId()) || line.componentType()==null || !Set.of("goods","service_fee","tip").contains(line.componentType()) || line.amountCent()<=0
                    || !unique.add(line.sourceBillId()+":"+line.componentType()))throw invalid("退款来源须属于本收款账单，分项不得重复且金额须为正数");
            sum=Math.addExact(sum,line.amountCent());
            if(!"goods".equals(line.componentType())) {
                p.lock("refund-source:"+line.sourceBillId());
                long original=p.jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM bar_payroll_bill_components WHERE source_bill_id=? AND component_type=? AND collection_bill_id=?",Long.class,line.sourceBillId(),line.componentType(),collection.id());
                long reserved=p.jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM bar_payroll_refund_lines WHERE source_bill_id=? AND component_type=? AND collection_bill_id=?",Long.class,line.sourceBillId(),line.componentType(),collection.id());
                if(Math.addExact(reserved,line.amountCent())>original)throw conflict("BAR_REFUND_COMPONENT_EXCEEDED","该原账单分项退款累计超过原收费；处理中退款同样占用原额度");
                if(p.jdbc.queryForObject("""
                    SELECT count(*) FROM bar_payroll_allocations b JOIN staff_payroll_statement_lines l
                    ON l.source_type='bar_payroll_allocation' AND l.source_id=b.id
                    JOIN ar_ap_items a ON a.source_type='staff_payroll' AND a.source_id=l.statement_id
                    WHERE b.source_bill_ids @> ?::jsonb AND a.paid_amount_cent>0
                    """,Integer.class,p.encode(List.of(line.sourceBillId())))>0)
                    throw conflict("BAR_SERVICE_INCOME_ALREADY_PAID","该服务收入已有实际工资清偿，不能按发放前退款流程办理");
            }
        }
        if(sum!=total)throw invalid("商品、服务费、小费各原账单退款明细合计必须等于本次实际退款额");
        long tip=lines.stream().filter(l->"tip".equals(l.componentType())).mapToLong(RefundLine::amountCent).sum();
        long remainingFee=p.jdbc.queryForObject("""
            SELECT COALESCE((SELECT sum(amount_cent) FROM bar_payroll_bill_components WHERE collection_bill_id=? AND component_type='service_fee'),0)
              - COALESCE((SELECT sum(amount_cent) FROM bar_payroll_refund_lines WHERE collection_bill_id=? AND component_type='service_fee'),0)
            """,Long.class,collection.id(),collection.id());
        long goods=lines.stream().filter(l->"goods".equals(l.componentType())).mapToLong(RefundLine::amountCent).sum();
        if(tip>collection.tipAmountCent() || total-tip>collection.payableAmountCent() || goods>Math.max(0L,collection.payableAmountCent()-remainingFee))
            throw invalid("退款分项超过账单尚可退的小费、服务费或商品金额，不能改名挪用其他分项额度");
        return lines;
    }
    void record(BarBillAdjustmentRecord adjustment,List<RefundLine> lines) {
        for(var line:lines)p.jdbc.update("INSERT INTO bar_payroll_refund_lines(id,adjustment_id,collection_bill_id,source_bill_id,component_type,amount_cent,ar_ap_item_id) VALUES(?,?,?,?,?,?,?)",
            id("bprf"),adjustment.id(),adjustment.billId(),line.sourceBillId(),line.componentType(),line.amountCent(),adjustment.arApItemId());
    }
    /** 任一部分仍在退款中就保留未结；退款成功凭实际付款事实确认。 */
    void requireResolved(String collectionBill) {
        var adjustments=p.jdbc.queryForList("SELECT id,amount_cent,ar_ap_item_id FROM bar_bill_adjustments WHERE bill_id=? AND adjustment_type='refund' ORDER BY id",collectionBill);
        for(var a:adjustments) {
            long expected=((Number)a.get("amount_cent")).longValue();
            long lines=p.jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM bar_payroll_refund_lines WHERE adjustment_id=?",Long.class,a.get("id"));
            if(lines!=expected)throw conflict("BAR_PAYROLL_REFUND_BREAKDOWN_REQUIRED","原退款尚未有经核实的完整分项明细");
            long paid=p.jdbc.queryForObject("SELECT COALESCE(sum(amount_cent),0) FROM ar_ap_payments WHERE ar_ap_item_id=?",Long.class,a.get("ar_ap_item_id"));
            if(paid<expected)throw conflict("BAR_PAYROLL_REFUND_PENDING","退款尚未实际全部付成，不以退款申请时间认定成功");
        }
    }
    long refunded(String source,String type) {
        return p.jdbc.queryForObject("""
            SELECT COALESCE(sum(r.amount_cent),0) FROM bar_payroll_refund_lines r
            JOIN bar_bill_adjustments a ON a.id=r.adjustment_id
            WHERE r.source_bill_id=? AND r.component_type=?
              AND (SELECT COALESCE(sum(p.amount_cent),0) FROM ar_ap_payments p WHERE p.ar_ap_item_id=r.ar_ap_item_id)>=a.amount_cent
            """,Long.class,source,type);
    }
}
