package com.complex.platform.bar;
// 文件版本：V0.03

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 仅供 Finance/Analytics 内部读取；调用者先授权。流水、完成订单收入、月结扣留分别返回。 */
@Service
public class BarRevenueSourceService {
    private final JdbcTemplate jdbc;
    private final BarPayrollAllocationService allocations;
    public BarRevenueSourceService(JdbcTemplate jdbc,BarPayrollAllocationService allocations) {this.jdbc=jdbc;this.allocations=allocations;}
    public record CollectionRevenue(String billId,String merchantId,String projectId,Instant completedAt,long collectedCent,
            Long goodsRevenueCent,long serviceFeeCent,long tipCent,String status,List<String> unresolvedSources) {}
    public record WithheldRevenue(String sourceBillId,String merchantId,String projectId,LocalDate businessDate,String runId,
            Long amountCent,String status,List<String> unresolvedSources) {}
    public record GoodsRefund(String adjustmentId,String billId,String merchantId,Instant completedAt,Long amountCent,String status) {}

    public List<CollectionRevenue> completedCollections(String merchantId,Instant from,Instant to) {
        Objects.requireNonNull(merchantId,"merchantId");
        // 第十七批⑤：作废（void）账单只留存作废前已退的那部分收款（BarBillAnomalyService），其中代收分项取已退的服务费、小费，
        // 商品收入与退款营业日的已退商品款相抵；不按待付款挂起。
        // 第十八批 T 段第 157 条 157-R3（BSM:1111「商户核销未收售价，不确认销售收入，也不计入商户营业额或营业额抽成基数」「已核销结案后又收回的款项
        // 按收回时点单独入账」；第十八批裁决 38「收入与出库成本都不进分红净利润」）：所属订单挂起已确认坏账且未结清的账单整单不取；收回结清后按结清时点计入。
        return jdbc.query("""
            SELECT b.*,s.amount receipt_cent,s.completed_at,COALESCE(origin.project_id,space.project_id) project_id,
              COALESCE((SELECT sum(amount_cent) FROM bar_bill_adjustments a WHERE a.bill_id=b.id AND a.adjustment_type='refund'),0) refund_cent,
              CASE WHEN b.settlement_status='void' THEN COALESCE((SELECT sum(amount_cent) FROM bar_payroll_refund_lines r WHERE r.collection_bill_id=b.id AND r.component_type='service_fee'),0)
                ELSE COALESCE((SELECT sum(amount_cent) FROM bar_payroll_bill_components c WHERE c.collection_bill_id=b.id AND c.component_type='service_fee'),0) END service_cent,
              CASE WHEN b.settlement_status='void' THEN COALESCE((SELECT sum(amount_cent) FROM bar_payroll_refund_lines r WHERE r.collection_bill_id=b.id AND r.component_type='tip'),0)
                ELSE COALESCE((SELECT sum(amount_cent) FROM bar_payroll_bill_components c WHERE c.collection_bill_id=b.id AND c.component_type='tip'),0) END tip_cent,
              EXISTS(SELECT 1 FROM bar_bill_adjustments a WHERE a.bill_id=b.id AND a.adjustment_type='service_fee'
                  AND NOT EXISTS(SELECT 1 FROM bar_payroll_bill_components c WHERE c.source_bill_id=b.id AND c.component_type='service_fee')) missing_fee
            FROM bar_bills b JOIN LATERAL (SELECT sum(settlement_amount_cent) amount,max(settled_at) completed_at
                FROM bar_bill_settlements WHERE bill_id=b.id AND NOT reversed) s ON s.amount>0
            LEFT JOIN bar_order_service_sources origin ON origin.order_id=b.order_id
            LEFT JOIN bar_tables t ON t.id=b.table_id LEFT JOIN spaces space ON space.id=t.space_id
            WHERE b.merchant_id=? AND s.completed_at>=? AND s.completed_at<?
              AND NOT (b.settlement_status NOT IN ('settled','merged','void') AND EXISTS(SELECT 1 FROM bar_order_suspensions os
                  WHERE (os.order_id=b.order_id OR os.bill_id=b.id) AND os.bad_debt_confirmed_at IS NOT NULL))
            ORDER BY s.completed_at,b.id
            """,(rs,n)->{
                long paid=rs.getLong("receipt_cent"),fee=rs.getLong("service_cent"),tip=rs.getLong("tip_cent");
                long originalDue=Math.addExact(Math.addExact(rs.getLong("payable_amount_cent"),rs.getLong("tip_amount_cent")),rs.getLong("refund_cent"));
                List<String> unresolved=new ArrayList<>();
                boolean voided="void".equals(rs.getString("settlement_status"));
                boolean paymentPending=!voided && (!"settled".equals(rs.getString("settlement_status")) || paid<originalDue || rs.getLong("paid_amount_cent")<originalDue);
                if(paymentPending)unresolved.add("订单尚未全渠道足额完成付款");
                if(rs.getBoolean("missing_fee") || !voided && tip<rs.getLong("tip_amount_cent"))unresolved.add("服务费或小费缺少真实来源分项");
                if(rs.getString("project_id")==null)unresolved.add("收款账单缺少实际综合体");
                long goods=paid-fee-tip;
                if(goods<0)unresolved.add("原收款及代收分项金额不一致");
                return new CollectionRevenue(rs.getString("id"),merchantId,rs.getString("project_id"),rs.getTimestamp("completed_at").toInstant(),
                    paid,unresolved.isEmpty()?goods:null,fee,tip,unresolved.isEmpty()?"confirmed":paymentPending && unresolved.size()==1?"payment_pending":"pending",List.copyOf(unresolved));
            },merchantId,Timestamp.from(from),Timestamp.from(to));
    }

    public List<WithheldRevenue> withheldServiceFees(String merchantId,LocalDate from,LocalDate to) {
        Objects.requireNonNull(merchantId,"merchantId");
        var sources=jdbc.queryForList("""
            SELECT source_bill_id,project_id,business_date FROM bar_payroll_bill_components
            WHERE merchant_id=? AND component_type='service_fee' AND business_date>=? AND business_date<?
            GROUP BY source_bill_id,project_id,business_date ORDER BY business_date,source_bill_id
            """,merchantId,from,to);
        List<WithheldRevenue> result=new ArrayList<>();
        for(var source:sources) {
            String bill=(String)source.get("source_bill_id"),project=(String)source.get("project_id");
            LocalDate date=((java.sql.Date)source.get("business_date")).toLocalDate();
            var runs=jdbc.queryForList("""
                SELECT id,withheld_service_fee_cent FROM bar_payroll_runs
                WHERE merchant_id=? AND source_snapshot->>'source_bill_id'=? AND status='confirmed'
                ORDER BY created_at DESC,id DESC LIMIT 1
                """,merchantId,bill);
            List<String> unresolved=allocations.sourceUnresolved(bill,date.toString().substring(0,7));
            if(runs.isEmpty() && unresolved.isEmpty())unresolved=List.of("服务费尚未月结");
            var run=runs.isEmpty()?null:runs.getFirst();
            result.add(new WithheldRevenue(bill,merchantId,project,date,run==null?null:(String)run.get("id"),
                unresolved.isEmpty()&&run!=null?((Number)run.get("withheld_service_fee_cent")).longValue():null,
                unresolved.isEmpty()&&run!=null?"confirmed":"pending",unresolved));
        }
        // 历史服务费无来源时仍是待结，不能被漏成确认的零收入。
        var legacy=jdbc.query("""
            SELECT DISTINCT b.id,s.project_id,COALESCE(o.business_date,(b.created_at AT TIME ZONE 'Asia/Shanghai')::date) business_date
            FROM bar_bills b JOIN bar_bill_adjustments a ON a.bill_id=b.id AND a.adjustment_type='service_fee'
            LEFT JOIN bar_order_service_sources o ON o.order_id=b.order_id
            LEFT JOIN bar_tables t ON t.id=b.table_id LEFT JOIN spaces s ON s.id=t.space_id
            WHERE b.merchant_id=? AND COALESCE(o.business_date,(b.created_at AT TIME ZONE 'Asia/Shanghai')::date)>=?
              AND COALESCE(o.business_date,(b.created_at AT TIME ZONE 'Asia/Shanghai')::date)<?
              AND NOT EXISTS(SELECT 1 FROM bar_payroll_bill_components c WHERE c.source_bill_id=b.id AND c.component_type='service_fee')
            """,(rs,n)->new WithheldRevenue(rs.getString("id"),merchantId,rs.getString("project_id"),rs.getDate("business_date").toLocalDate(),null,null,"pending",List.of("历史服务费尚无完整来源及月结原件")),merchantId,from,to);
        result.addAll(legacy);return List.copyOf(result);
    }

    public List<GoodsRefund> completedGoodsRefunds(String merchantId,Instant from,Instant to) {
        Objects.requireNonNull(merchantId,"merchantId");
        // 已退成同 BarPayrollRefundService#refunded（第十六批 TK-04-b「退回储值卡的部分即时成功」）：未挂应付的（全额回卡）按退款时点；
        // 挂了应付的（金额只是非储值部分）按付款合计达到应付本身金额、取最后一笔付款时点，部分回卡的混合退款取两者较晚的。
        return jdbc.query("""
            SELECT * FROM (SELECT a.id,a.bill_id,a.amount_cent,
                CASE WHEN a.ar_ap_item_id IS NULL THEN a.refunded_at
                  WHEN p.amount>=i.amount_cent THEN CASE WHEN i.amount_cent<a.amount_cent THEN GREATEST(a.refunded_at,p.completed_at) ELSE p.completed_at END
                END completed_at,
                COALESCE((SELECT sum(amount_cent) FROM bar_payroll_refund_lines r WHERE r.adjustment_id=a.id),0) line_cent,
                COALESCE((SELECT sum(amount_cent) FROM bar_payroll_refund_lines r WHERE r.adjustment_id=a.id AND r.component_type='goods'),0) goods_cent
              FROM bar_bill_adjustments a JOIN bar_bills b ON b.id=a.bill_id LEFT JOIN ar_ap_items i ON i.id=a.ar_ap_item_id
              LEFT JOIN LATERAL (SELECT sum(amount_cent) amount,max(payment_time) completed_at FROM ar_ap_payments
                WHERE ar_ap_item_id=a.ar_ap_item_id) p ON true
              WHERE a.adjustment_type='refund' AND b.merchant_id=?) x
            WHERE completed_at>=? AND completed_at<?
            ORDER BY completed_at,id
            """,(rs,n)->new GoodsRefund(rs.getString("id"),rs.getString("bill_id"),merchantId,rs.getTimestamp("completed_at").toInstant(),
                rs.getLong("line_cent")==rs.getLong("amount_cent")?rs.getLong("goods_cent"):null,
                rs.getLong("line_cent")==rs.getLong("amount_cent")?"confirmed":"pending"),merchantId,Timestamp.from(from),Timestamp.from(to));
    }
}
