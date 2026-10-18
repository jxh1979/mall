package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * CG-08b（ENUM「全园故障与补偿通票」）：补偿补发的权益核销成功后，把实际履约（核销记录、时点、履约时生效的内部结算价）
 * 锁定到对应补偿记录；该补偿机会使用一次后即用尽。由 amusement 域实现。
 */
public interface CompensationFulfillmentPort {

    void recordFulfillment(String compensationItemId, String verificationId, Instant fulfilledAt, Long settlementPriceCent);
}
