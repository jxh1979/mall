package com.complex.platform.dealer.dto;

/** 合同付款条款与运费模板（BSM:1227 / BSM:1229）。freightBearer：dealer / sales_entity。 */
public record DealerContractTermsRequest(String idempotencyKey, Boolean creditTerm, Integer firstPaymentRatioBps, Long firstPaymentAmountCent,
        Integer firstPaymentDeadlineDays, Integer balanceDueDays, Long freightCent, Long freeShippingThresholdCent, String freightBearer) {
}
