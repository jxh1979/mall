package com.complex.platform.finance;

/**
 * 持股分红发放端口（V299）：分红计算单审批通过后，按主理人净发放额（代扣后）挂应付
 * （ar_ap_items direction=payable, source_type='dividend_payout', counterparty=主理人账户,
 * source_id=计算单 id——(direction,source_type,source_id) 部分唯一索引天然防一单重挂）。
 * finance 侧实现；依赖方向 dividend→finance。返回 ar_ap_item_id。
 */
public interface DividendPayoutPort {
    String createDividendPayoutPayable(String principalUserId, String merchantId, long netAmountCent, String statementId);
}
