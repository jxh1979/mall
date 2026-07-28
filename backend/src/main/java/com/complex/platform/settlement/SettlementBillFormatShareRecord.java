package com.complex.platform.settlement;

/**
 * S11 结算单 × 业态份额留痕（V366 {@code settlement_bill_format_shares} 一行）。
 *
 * <p>一张联合／综合通票的售价先整单扣掉第三方渠道抽成，剩下的按各业态票面原价占比拆成几份：
 * 游乐那份归游乐公司、餐饮那份归餐厅商户、零售那份归零售商户，酒吧那份才继续往下走
 * （赠送权益预估成本 → 门票综合服务费 → 可分配池 → 分配）。本记录就是这几份钱各自的账。
 *
 * <p>金额<b>带符号</b>：跨月红冲会让某业态当月份额为负＝向该业态追回。
 *
 * @param billId                     结算单 id
 * @param businessFormat             业态（bar/catering/retail/amusement/other，V348 五值）
 * @param grossRevenueCent           本业态毛额 = 已核销净额 + 项目公司承担的套票折扣补回
 * @param platformBorneDiscountCent  其中项目公司承担的套票折扣（§20.4：口径对每个业态一致）
 * @param channelFeeCent             整单渠道抽成按份额占比分摊到本业态的部分
 * @param netRevenueCent             净份额 = 毛额 − 渠道费；酒吧业态的净份额即可分配池基数
 * @param beneficiaryKind            entity=归某经营主体；per_merchant=归提供该权益的各商户自己
 * @param beneficiaryEntityId        beneficiary_kind=entity 时的 business_entities.id；归属未配置时为空
 */
public record SettlementBillFormatShareRecord(
        String billId,
        String businessFormat,
        long grossRevenueCent,
        long platformBorneDiscountCent,
        long channelFeeCent,
        long netRevenueCent,
        String beneficiaryKind,
        String beneficiaryEntityId
) {
}
