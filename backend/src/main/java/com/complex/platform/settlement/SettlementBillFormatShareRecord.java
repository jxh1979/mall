package com.complex.platform.settlement;

// 文件版本：V0.03

/**
 * S11 结算单 × 业态份额留痕（V366 {@code settlement_bill_format_shares} 一行）。
 *
 * <p>一张联合／综合通票的售价先整单扣掉第三方渠道抽成，剩下的按各业态票面原价占比拆成几份：
 * 游乐那份归游乐公司、餐饮与零售那份归城市项目公司（第十八批 X 段 176-R2，ENUM:49），酒吧那份才继续往下走
 * （门票综合服务费 → 可分配池 → 分配）。本记录就是这几份钱各自的账。
 *
 * <p>金额<b>带符号</b>：跨月红冲会让某业态当月份额为负＝向该业态追回。
 *
 * @param billId                     结算单 id
 * @param businessFormat             业态（bar/catering/retail/amusement/media/other，V348+V384 六值）
 * @param grossRevenueCent           本业态毛额 = 已核销净额 + 项目公司承担的套票折扣补回
 * @param platformBorneDiscountCent  其中项目公司承担的套票折扣（§20.4：口径对每个业态一致）
 * @param channelFeeCent             整单销售渠道费（仅第三方渠道）按份额占比分摊到本业态的部分
 * @param routeFeeCent               第十八批第 3 条：本业态当月核销逐笔分摊到的通路费合计（所有付款都产生，官方渠道同样扣）
 * @param compServiceFeeCent         S4:本业态当月进池核销计提的门票综合服务费（池链支，归娱乐平台公司）
 * @param giftCostCent               S6:本业态当月进池核销计提的赠送权益预估成本（G，跟核销走，归娱乐平台公司；
 *                                   常态首笔核销=入场→G 落酒吧行，即 HTML:742「酒吧份额再依次扣掉赠送权益预估成本」）
 * @param giftActualPaidCent         S7:本业态当月赠送权益兑付实付（A，娱乐平台公司按内部结算价付给该业态
 *                                   提供方商户的净额，含冲正对冲；HTML:962「成本按提供方的商户业态分开记账」）。
 *                                   纯留痕不进净份额（兑付由娱乐平台公司承担，不动各业态分账）
 * @param netRevenueCent             净份额 = 毛额 − 销售渠道费 − 通路费 − 赠送权益预估成本 − 综合服务费；酒吧业态的净份额即可分配池
 * @param beneficiaryKind            entity=归某经营主体；per_merchant=归提供该权益的各商户自己
 * @param beneficiaryEntityId        beneficiary_kind=entity 时的 business_entities.id；归属未配置时为空
 * @param internalSettledCent        第十八批 X 段 176-R2：本业态有价专属直付按内部结算价应直付给实际提供商户的合计（ENUM:53），
 *                                   与净份额之差即归属主体承担（负）或归属主体所得（正）的差额
 * @param interEntityNetCent         第十八批 AI 段 237-R1：域间份额——本业态净份额中由娱乐平台公司代收、应归归属主体的部分（游乐、传媒行扣除
 *                                   归属主体自收的纯游乐票、纯游乐通票与传媒自办演出票；餐饮零售行即净份额；酒吧与「其他」为 0；ENUM:32、BSM:1720）
 * @param recoveryNetCent            第十八批 AI 段 214-R1：原月已过账后撤销、在本月追收的域间份额（负＝向归属主体追回；决策台账 FS-021）
 * @param recoveryInternalSettledCent 第十八批 AI 段 214-R1：原月已过账后撤销、在本月追收的按内部结算价应直付额
 * @param unverifiedNetCent          第十八批 AI 段 214-R2：本月到期未核销、归本业态归属主体的组合通票票款（扣实际发生的通路费与销售渠道费；ENUM:49）
 */
public record SettlementBillFormatShareRecord(
        String billId,
        String businessFormat,
        long grossRevenueCent,
        long platformBorneDiscountCent,
        long channelFeeCent,
        long routeFeeCent,
        long compServiceFeeCent,
        long giftCostCent,
        long giftActualPaidCent,
        long netRevenueCent,
        String beneficiaryKind,
        String beneficiaryEntityId,
        long internalSettledCent,
        long interEntityNetCent,
        long recoveryNetCent,
        long recoveryInternalSettledCent,
        long unverifiedNetCent
) {
}
