package com.complex.platform.ticketing.dto;

import java.util.List;

/**
 * 改虚拟权益「签约价」(= 内部结算价, entitlement_templates.agreement_price_cent) 请求体。
 *
 * <p>资金口径:签约价决定核销时结算给商户多少钱。此前只能建不能改——填错只能作废重建,
 * 每错一次就多一条废模板 + 一次重新绑票。本请求补上改价入口。
 *
 * <p><b>只改模板这一层</b>:签约价在发放时快照进 issued_entitlements.agreement_price_cent、
 * 核销时再快照进 ticket_verifications.settlement_price_cent。故改价只对**之后发放**的权益生效,
 * 已发放/已核销的按各自快照走 ——「已发生的不可改」由快照链结构性保证,不靠本端点自律。
 *
 * <p>reason 必填:动的是钱的口径,审计要能答"谁在什么时候为什么把这个商户的结算价从 X 改成 Y"。
 */
public record EntitlementAgreementPriceRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Long agreementPriceCent,
        String reason
) {
}
