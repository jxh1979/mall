package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 第十八批 AJ 段 236-R3 跨域组合通票票种规则版本（ENUM:32「跨域组合通票的定价与域间结算规则由项目之上的平台权限统一审批」；BSM:754「每次发布时把凑整差额金额
 * 及各域分摊数额记入票种规则版本、发布后不得再改」）：三档价、折扣、最低赠酒杯数、权益组成、合计与算出售价、凑整差额与各域分摊。
 *
 * @param status     draft 草稿、pending_review 待审批、effective 生效中、superseded 已被取代、withdrawn 已撤回
 * @param priceCheck 票种已上架时其未结版本的定价核算（与现行允许误差比对）；其余为空
 */
public record TicketTypeRuleVersionView(
        String id,
        String ticketTypeId,
        String projectId,
        int versionNo,
        String status,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        BigDecimal comboDiscountRate,
        Integer minGiftCups,
        Integer minGiftCupsLower,
        Integer minGiftCupsUpper,
        List<Entitlement> entitlements,
        long componentTotalCent,
        long computedPriceCent,
        Long roundingDiffCent,
        List<DomainAllocation> domainAllocations,
        String reason,
        String reviewId,
        String createdBy,
        String updatedBy,
        String submittedBy,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant decidedAt,
        TicketTypePriceCheck priceCheck
) {
    /** 版本里的一条权益绑定：提供方与业态（经营域）随版本记下。 */
    public record Entitlement(
            String bindingId,
            String entitlementTemplateId,
            String templateName,
            String entitlementType,
            String providerMerchantId,
            String providerMerchantName,
            String businessFormat,
            String domain,
            int quantity,
            int totalTimes,
            int numPeople,
            Long valueAmountCent,
            String effectiveRule,
            Long componentPriceCent,
            String costBearer,
            Instant createdAt
    ) {
    }

    /**
     * 一个经营域的分摊（BSM:754「凑整产生的差额按各业态价格比例，与折扣一并分摊到各域」）：与出票拆分同一算法（最大余数补尾）。
     *
     * @param componentTotalCent 该域各业态价格合计
     * @param discountShareCent  折扣分摊＝该域合计－按算出售价所得
     * @param roundingShareCent  凑整分摊＝按填的平日价所得－按算出售价所得
     * @param shareCent          按填的平日价所得
     */
    public record DomainAllocation(
            String domain,
            String domainName,
            long componentTotalCent,
            long discountShareCent,
            long roundingShareCent,
            long shareCent
    ) {
    }

    TicketTypeRuleVersionView withPriceCheck(TicketTypePriceCheck check) {
        return new TicketTypeRuleVersionView(id, ticketTypeId, projectId, versionNo, status, weekdayPriceCent, weekendPriceCent,
                holidayPriceCent, comboDiscountRate, minGiftCups, minGiftCupsLower, minGiftCupsUpper, entitlements, componentTotalCent,
                computedPriceCent, roundingDiffCent, domainAllocations, reason, reviewId, createdBy, updatedBy, submittedBy, createdAt,
                updatedAt, submittedAt, decidedAt, check);
    }

    boolean open() {
        return "draft".equals(status) || "pending_review".equals(status);
    }
}
