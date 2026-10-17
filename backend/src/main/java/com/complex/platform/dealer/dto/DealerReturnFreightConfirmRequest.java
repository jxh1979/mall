package com.complex.platform.dealer.dto;

/** 合同未约定时由双方确认：退回运费承担方（dealer / sales_entity）与已收首程运费是否退还。 */
public record DealerReturnFreightConfirmRequest(String idempotencyKey, String returnFreightBearer, Boolean firstLegFreightRefundable, String note) {
}
