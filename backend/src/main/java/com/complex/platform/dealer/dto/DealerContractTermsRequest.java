package com.complex.platform.dealer.dto;

/** 合同付款条款与运费模板（BSM:1227 / BSM:1229）。freightBearer：dealer / sales_entity。 */
public record DealerContractTermsRequest(String idempotencyKey, Boolean creditTerm, Integer firstPaymentRatioBps, Long firstPaymentAmountCent,
        Integer firstPaymentDeadlineDays, Integer balanceDueDays, Long freightCent, Long freeShippingThresholdCent, String freightBearer,
        /** CG-11c：非质量退货的退回运费承担方（dealer / sales_entity，可空＝未约定）。 */
        String returnFreightBearer,
        /** CG-11c：已收首程运费退不退（可空＝未约定）。 */
        Boolean firstLegFreightRefundable) {
    /** CG-11b 兼容构造。 */
    public DealerContractTermsRequest(String idempotencyKey, Boolean creditTerm, Integer firstPaymentRatioBps, Long firstPaymentAmountCent,
            Integer firstPaymentDeadlineDays, Integer balanceDueDays, Long freightCent, Long freeShippingThresholdCent, String freightBearer) {
        this(idempotencyKey, creditTerm, firstPaymentRatioBps, firstPaymentAmountCent, firstPaymentDeadlineDays, balanceDueDays, freightCent,
                freeShippingThresholdCent, freightBearer, null, null);
    }
}
