package com.complex.platform.dealer;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 经销合同：指定销售主体（ENUM:182）与中心仓（BSM:1229），挂独立可版本化的价格目录（BSM:1219）。
 * CG-11b 付款条款（BSM:1227）：creditTerm 是否约定账期；首付款按比例（万分比）或固定金额；首付款截止＝通知付款后 N 天；
 * 尾款到期＝出库后 N 天。运费模板（BSM:1229）：运费、包邮门槛、承担方、版本（变更只影响新订单）。
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
        int freightTemplateVersion
) {
    /** CG-11a 兼容构造：未约定条款。 */
    public DealerContractRecord(String id, String dealerId, String contractNo, String salesEntityId, String centralWarehouseId,
            LocalDate validFrom, LocalDate validTo, String status, String createdBy, Instant createdAt) {
        this(id, dealerId, contractNo, salesEntityId, centralWarehouseId, validFrom, validTo, status, createdBy, createdAt,
                false, null, null, null, null, null, null, "dealer", 0);
    }
}
