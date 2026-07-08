package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * 票务分销给商户的费率(ticket_distribution_merchant_rates)。项目下某商户参与分销时,
 * 按每客(perGuestCent)或固定额(fixedAmountCent)结算给商户的分销返点/费率。
 */
public record TicketDistributionMerchantRateRecord(
        String id,
        String projectId,
        String merchantId,
        Long perGuestCent,
        Long fixedAmountCent,
        String status,
        boolean isDraft,
        String updatedBy,
        Instant updatedAt
) {
}
