package com.complex.platform.customer.dto;

/** AUD-003 本店柜台/到店退款请求（商户预收卡唯一可达退款路径；款项柜台现场兑付，凭证号入审计）。 */
public record StoredValueCounterRefundRequest(
        String idempotencyKey,
        String projectId,
        String merchantId,
        Long amountCent,
        String reason,
        Boolean closeAccount,
        String redemptionVoucherNo
) {
}
