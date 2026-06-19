package com.complex.platform.bar;

import java.time.Instant;

/** R5 T16：订台定金规则（后台可配：固定额 / 按低消比例；退没收免没收时限）。 */
public record BarDepositRuleRecord(
        String id,
        String barId,
        String merchantId,
        String scopeType,
        String scopeId,
        String mode,
        Long fixedAmountCent,
        Integer ratioBps,
        int cancelFreeBeforeMinutes,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
