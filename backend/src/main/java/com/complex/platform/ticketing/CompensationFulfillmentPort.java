package com.complex.platform.ticketing;

// 文件版本：V0.03

import java.time.Instant;

/**
 * CG-08b（ENUM「全园故障与补偿通票」）：补偿补发的权益核销成功后，把实际履约（核销记录、时点、履约时生效的内部结算价）
 * 锁定到对应补偿记录；该补偿机会使用一次后即用尽。由 amusement 域实现。
 * FS-012（BSM L800 / ENUM L148；CG 第 39 条）：每次实际履约逐笔记录，锁定履约时目录生效价与目录行版本，
 * 由批次承担主体对实际提供方形成补偿应付；该次核销冲正时对冲。
 */
public interface CompensationFulfillmentPort {

    /** @param price 履约时目录生效价与版本；null＝该权益不设内部结算价（无目录引用），只留履约记录、不形成应付。 */
    void recordFulfillment(String compensationItemId, String entitlementId, String verificationId, String providerMerchantId,
            Instant fulfilledAt, InternalSettlementPriceResolver.VersionedPrice price);

    /** 补偿核销被冲正：已形成的补偿应付按 V344 追加严格取反的冲正行，记冲正时点；已付部分重述承接并另挂对提供方的追收应收（裁决第 5 条）。 */
    void reverseFulfillment(String verificationId, Instant reversedAt);
}
