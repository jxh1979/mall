package com.complex.platform.dealer;

// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;

/**
 * 经销合同：指定销售主体（ENUM:182）与中心仓（BSM:1229），挂独立可版本化的价格目录（BSM:1219）。
 * CG-11b 付款条款（BSM:1227）：creditTerm 是否约定账期；首付款按比例（万分比）或固定金额；首尾付款按合同约定的事件起点或固定截止时间；
 * 审核通过订单保存完整合同快照。运费模板（BSM:1229）：运费、包邮门槛、承担方、版本（变更只影响新订单）。
 */
public record DealerContractRecord(
        String id,
        String dealerId,
        String contractNo,
        String salesEntityId,
        String centralWarehouseId,
        LocalDate validFrom,
        LocalDate validTo,
        String status,
        String createdBy,
        Instant createdAt,
        boolean creditTerm,
        Integer firstPaymentRatioBps,
        Long firstPaymentAmountCent,
        Integer firstPaymentDeadlineDays,
        Integer balanceDueDays,
        Long freightCent,
        Long freeShippingThresholdCent,
        String freightBearer,
        int freightTemplateVersion,
        /** CG-11c：非质量退货的退回运费承担方（dealer / sales_entity；null＝未约定，先由双方确认）。 */
        String returnFreightBearer,
        /** CG-11c：已收首程运费退不退（null＝未约定，先由双方确认）。 */
        Boolean firstLegFreightRefundable,
        int termsVersion,
        Instant contractSignedAt,
        String firstPaymentAnchor,
        Instant firstPaymentFixedAt,
        String balanceDueAnchor,
        Instant balanceDueFixedAt
) {
    public DealerContractRecord(String id, String dealerId, String contractNo, String salesEntityId, String centralWarehouseId,
            LocalDate validFrom, LocalDate validTo, String status, String createdBy, Instant createdAt, boolean creditTerm, Integer firstPaymentRatioBps,
            Long firstPaymentAmountCent, Integer firstPaymentDeadlineDays, Integer balanceDueDays, Long freightCent, Long freeShippingThresholdCent,
            String freightBearer, int freightTemplateVersion, String returnFreightBearer, Boolean firstLegFreightRefundable) {
        this(id, dealerId, contractNo, salesEntityId, centralWarehouseId, validFrom, validTo, status, createdBy, createdAt, creditTerm, firstPaymentRatioBps,
                firstPaymentAmountCent, firstPaymentDeadlineDays, balanceDueDays, freightCent, freeShippingThresholdCent, freightBearer, freightTemplateVersion,
                returnFreightBearer, firstLegFreightRefundable, 0, null, null, null, null, null);
    }

    /** CG-11a 兼容构造：未约定条款。 */
    public DealerContractRecord(String id, String dealerId, String contractNo, String salesEntityId, String centralWarehouseId,
            LocalDate validFrom, LocalDate validTo, String status, String createdBy, Instant createdAt) {
        this(id, dealerId, contractNo, salesEntityId, centralWarehouseId, validFrom, validTo, status, createdBy, createdAt,
                false, null, null, null, null, null, null, "dealer", 0, null, null);
    }

    /** CG-11b 兼容构造：退货运费约定留空。 */
    public DealerContractRecord(String id, String dealerId, String contractNo, String salesEntityId, String centralWarehouseId,
            LocalDate validFrom, LocalDate validTo, String status, String createdBy, Instant createdAt, boolean creditTerm, Integer firstPaymentRatioBps,
            Long firstPaymentAmountCent, Integer firstPaymentDeadlineDays, Integer balanceDueDays, Long freightCent, Long freeShippingThresholdCent,
            String freightBearer, int freightTemplateVersion) {
        this(id, dealerId, contractNo, salesEntityId, centralWarehouseId, validFrom, validTo, status, createdBy, createdAt, creditTerm, firstPaymentRatioBps,
                firstPaymentAmountCent, firstPaymentDeadlineDays, balanceDueDays, freightCent, freeShippingThresholdCent, freightBearer, freightTemplateVersion, null, null);
    }
}
