package com.complex.platform.dealer.dto;

// 文件版本：V0.01

import java.time.Instant;

/** 条款来源为已核验合同；事件起点必须显式提供，不采用代码默认日期。 */
public record DealerContractTermsRequest(String idempotencyKey, Boolean creditTerm, Integer firstPaymentRatioBps, Long firstPaymentAmountCent,
        Integer firstPaymentDeadlineDays, Integer balanceDueDays, Long freightCent, Long freeShippingThresholdCent, String freightBearer,
        String returnFreightBearer, Boolean firstLegFreightRefundable, Instant contractSignedAt, String firstPaymentAnchor,
        Instant firstPaymentFixedAt, String balanceDueAnchor, Instant balanceDueFixedAt) {
}
