package com.complex.platform.bar;

import java.util.List;

/** R5 T16：创建/更新订台定金规则的请求载荷。 */
public record BarDepositRuleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String scopeType,
        String scopeId,
        String mode,
        Long fixedAmountCent,
        Integer ratioBps,
        Integer cancelFreeBeforeMinutes
) {
}
