package com.complex.platform.inventory;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Set;

/** 锁定供应合同的明确条款；不以供应商当前默认档替代。 */
public record PurchaseContractTerms(String purchaserEntityId,String settlementMethod,String priceBasis,Map<String,Long> unitPrices,
        String dueAnchor,Integer termDays,LocalDate fixedDueDate,LocalDate signedOn,Long prepaymentAmountCent,
        String consignmentSaleBasis,String prepaymentRemainderTerms) {
    public void requireComplete() {
        if(v(purchaserEntityId).isBlank()) throw invalid("请明确合同我方采购主体");
        if(!Set.of("cash_on_delivery","monthly","credit_term","prepaid","consignment").contains(v(settlementMethod))
                || !Set.of("contract_unit_price","accepted_actual_unit_price").contains(v(priceBasis)))
            throw invalid("请补齐合同结算方式和价格口径");
        if(!Set.of("contract_signed","ordered","each_receipt","all_fulfilled","month_end","fixed_date","confirmed_sale").contains(v(dueAnchor)))
            throw invalid("请明确合同到期起算事件");
        if("fixed_date".equals(dueAnchor)?fixedDueDate==null:termDays==null||termDays<0) throw invalid("请补齐合同到期日或账期天数");
        if("contract_signed".equals(dueAnchor)&&signedOn==null) throw invalid("须登记经核实的合同签约日期");
        if("contract_unit_price".equals(priceBasis)&&(unitPrices==null||unitPrices.isEmpty()
                ||unitPrices.entrySet().stream().anyMatch(e->e.getKey()==null||e.getKey().isBlank()||e.getValue()==null||e.getValue()<0)))
            throw invalid("固定单价须按合同商品完整登记");
        if("prepaid".equals(settlementMethod)&&(prepaymentAmountCent==null||prepaymentAmountCent<=0
                ||!Set.of("contract_signed","ordered","fixed_date").contains(dueAnchor)||v(prepaymentRemainderTerms).isBlank()))
            throw invalid("预付须明确本采购预付金额、付款起点及未用余额处理约定");
        if("consignment".equals(settlementMethod)&&(v(consignmentSaleBasis).isBlank()
                ||!Set.of("confirmed_sale","month_end","fixed_date").contains(dueAnchor)))
            throw invalid("寄售须明确人工核实售出的业务依据及到期规则");
        if(!"consignment".equals(settlementMethod)&&"confirmed_sale".equals(dueAnchor)) throw invalid("普通采购不能使用寄售售出起算");
    }
    void requirePricesFor(List<String> items) {
        requireComplete();
        if("contract_unit_price".equals(priceBasis)&&(items==null||items.stream().anyMatch(item->item==null||item.isBlank()||!unitPrices.containsKey(item))))
            throw invalid("合同可采购商品须逐项明确固定单价，缺价不能提交或采购");
    }
    long payableUnitPrice(String item,long actual) {
        requireComplete(); if(actual<0) throw invalid("实际到货单价不得为负");
        if("accepted_actual_unit_price".equals(priceBasis)) return actual;
        Long price=unitPrices.get(item);if(price==null) throw invalid("该商品缺少锁定合同单价");return price;
    }
    private static String v(String s){return s==null?"":s;}
    private static ApiException invalid(String s){return ApiException.conflict("PURCHASE_CONTRACT_TERMS_INCOMPLETE",s);}
}
