package com.complex.platform.settlement;

// 文件版本：V0.02

import java.time.LocalDate;

/** 结算重构批2-3：月度结算单头（settlement_bills 一行）。金额均整数分,<b>带符号</b>(冲正月可为负=当月净追回)。 */
public record SettlementBillRecord(
        String id,
        String projectId,
        LocalDate periodStart,
        LocalDate periodEnd,
        long ticketRevenueCent,
        // 第十八批第 3 条（BSM L749 渠道费用分两层）：channelFeeCent＝销售渠道费（仅经第三方渠道售出的票，整单按渠道配置算）；
        //   routeFeeCent＝通路费（所有付款按支付通路固化、随核销逐笔分摊的月合计）。两者都在拆份额前整单扣、各业态按份额共担。
        long channelFeeCent,
        long routeFeeCent,
        // S4:可分配池(唯一的池)= 酒吧业态净份额 − 池链综合服务费。仅用于各酒吧分配,
        //   池内不再二次扣任何留存(业务 HTML「不再从可分配池中二次扣娱乐平台公司留存」)。
        long availablePoolCent,
        long distributedCent,
        // S4 门票综合服务费拆两支(同一道费,不是两种费;都归娱乐平台公司):
        //   pooled = 当月进池核销逐笔计提合计(从池链扣);exclusive = 专属直付前抽取合计(从专属净额扣)。
        long compServiceFeePooledCent,
        long compServiceFeeExclusiveCent,
        // S4 收款主体快照:两支综合服务费的收款方(business_entities.id,解析自
        //   revenue_stream_attribution 的 ticket_service_fee 流;月结时定格,后改配置不追溯已出单据)。
        String feeBeneficiaryEntityId,
        // S6:当月赠送权益预估成本合计(G,进池核销逐笔计提的线性求和,归娱乐平台公司);冲正月可为负。
        long giftCostCent,
        // S6 收款主体快照:G 的收款方(解析自 revenue_stream_attribution 的 pass_gift_cost 流;月结时定格)。
        String giftBeneficiaryEntityId,
        String distributionMethod,
        String status,
        // 六业态收入归属(terminology-canonical §三):通票收入归属的 L3 管理公司/业态主体(娱乐平台公司)。
        // 由 revenue_stream_attribution 按 (项目, all_pass_ticket) 解析;未配置=null(零金额影响,仅归属留白)。
        String attributedEntityId,
        // D6 套票折扣入账：本月项目公司实际承担的折扣额（platform_absorb 全额/split_by_ratio 平台份额；
        //   merchant_absorb=0）。为覆盖折扣使商户拿满额，项目多支出的金额。默认 0=零行为变更。
        long comboDiscountCent,
        // R-7/D15.1（V343 + V383）分账尾差（带符号）= 池分账舍入尾差 + 渠道费按业态分摊尾差。
        //   治理地图「全局原则 · 金额一律精确到分」：尾差【单列留痕】并由该笔业务
        //   【实际承担结算责任的主体】承担，不得默认转给城市项目公司/顶层平台，也不得悄悄并进任一参与方
        //   （旧口径「统一归项目公司/平台兜底」已于 2026-08-01 作废）。
        //   守恒：Σ池分账 + 池尾差 ≡ 应分总额；Σ各业态渠道费 + 渠道尾差 ≡ 整单渠道费。
        long roundingResidualCent,
        // rounding_residual_cent 的责任主体快照（V383；all_pass_ticket 流解析，与 attributedEntityId 同源）；
        //   尾差为 0 时 null（没有尾差就没有责任人）。
        String roundingResidualEntityId,
        // S8/K18 ③④（V375）：第二段无参与者（排名档位酒吧为空/权重全 0）时的池余额，显式归娱乐平台公司留痕
        //   （带符号，冲正月可为负=由平台承接追回）；正常两段分尽时恒 0。与舍入尾差分列，成因不同。
        long poolUndistributedCent,
        // pool_undistributed_cent 的归属主体快照（all_pass_ticket 流解析，V375 种子=be_ent_platform）；余额为 0 时 null。
        String poolUndistributedEntityId,
        // 第十八批第 2 条（V556；ENUM L52「按上述减除的未核销份额，按票种经营域归属……酒吧域即归娱乐平台公司；该笔在该张票的分账结果中
        //   单独立项留痕，不并入未分配池余额、也不并入金额尾差」）：通票未核销减除额与其归属主体（酒吧域＝all_pass_ticket 流主体）；
        //   缺该家数档位行而暂挂的第二段余额（ENUM L52「暂挂、不出结果」）；第一段合计超过该份池的超出额（越池判定沿用现逻辑）。
        long unverifiedDeductionCent,
        String unverifiedDeductionEntityId,
        long poolSuspendedCent,
        long poolOverflowCent
) {
}
