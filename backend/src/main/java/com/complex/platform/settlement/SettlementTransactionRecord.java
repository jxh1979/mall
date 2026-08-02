package com.complex.platform.settlement;

import java.time.Instant;

/**
 * 结算重构批1：settlement_transactions 一行的领域记录（对应 V222 表）。
 * 批1只写 transaction_type='verification_settled'（核销结算双写）；unverified_expired 由后续过期归项目 Job 产生。
 */
public record SettlementTransactionRecord(
        String id,
        String transactionNo,
        String transactionType,
        String entitlementId,
        String orderId,
        String ticketTypeId,
        String verificationId,
        String entitlementType,
        int verifiedCount,
        int totalTimes,
        int numPeople,
        long allocatedRevenueCent,
        long verifiedAmountCent,
        long unverifiedAmountCent,
        String beneficiaryType,
        String merchantId,
        String projectId,
        String settlementStatus,
        Instant entitlementValidTo,
        // 结算重构批3-4(PRD §6.4)：专属某商户「不分摊」，核销双写时从权益带上定格；非空=该笔核销全额归该商户不进池。
        String exclusiveMerchantId,
        // AUD-012 结算状态机重构：业务发生时刻（核销/冲正/到期），account 按此落月窗（取代首插 created_at）。
        Instant businessAt,
        // D6 套票折扣额增量（分）：本笔核销的 满额−净额（核销行 >=0；冲正行取原行反 <=0；到期归项目=0）。
        //   下传 append-only 账本，结算引擎按 discount_bearer 归属应用。守 V331 资金守恒 CHECK（配对天然净 0）。
        long discountAmountCent,
        // P6 综合服务费实收(分):核销行=每笔增量封顶后实收(>=0);冲正行=取原 ST-VS 行 fee 之反(<=0);无专属/无费率=0。
        //   settleMonth 线性求和替代月末重算 → 固定额/封顶/比例三模式跨月红冲全守恒(每笔实收精确对冲)。
        long compServiceFeeCent,
        // P6 本笔 fee 解析到的综合服务费费率规则 id;供增量封顶按「商户×规则」查月内累计。无 fee=null。
        String compServiceFeeConfigId,
        // V355(B-7)beneficiary_type='entity' 时的收款主体 business_entities.id
        //   (未核销权益款归娱乐平台公司 / 游乐通票未核销款归游乐公司);merchant/project 时为 null。
        //   DB CHECK chk_st_beneficiary 强制:type=entity ⇒ 本列非空。
        String beneficiaryEntityId,
        // S3(V367)父票锚点 issued_entitlements.parent_ticket_id:「门票综合服务费一张票只收一道」的判定键。
        //   核销行从权益带上;冲正行原样沿用被冲那行的值(否则该票净额抵不回 0、锚点复不了位)。
        //   到期归项目/重建行恒 null(这两条路 fee 恒 0,锚点无意义)。
        String parentTicketId,
        // S6(V372)赠送权益预估成本 G(分,归娱乐平台公司):该票【首笔进池核销】一次性计提(核销行 >=0),
        //   冲正行取原行之反(<=0);settleMonth 线性求和,绝不月末重算。两模式见 pass_gift_cost_configs。
        long giftCostCent,
        // S6 本笔 G 解析到的 pass_gift_cost_configs 规则 id;供增量封顶按「项目×规则」查月内累计 + 冲正沿用。无 G=null。
        String giftCostConfigId
) {
}
