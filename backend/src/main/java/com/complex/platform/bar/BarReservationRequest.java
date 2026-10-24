package com.complex.platform.bar;

// 文件版本：V0.01

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 员工代客订台请求。ROOT-03-R19：latestArrivalAt 最晚到店时间必填（BSM 第 1062 行「无论是否支付定金，每一笔订台都必须填写最晚到店时间」）；
 * 操作员裁决第十三批第 7 条：customerPhone 客人手机号必填（「员工代订必须录入客人手机号；查到会员且受限就拒绝，查不到会员的照常代订」），只用于核对受限、不落库。
 */
public record BarReservationRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String tableId,
        Instant reservationTime,
        Integer guestCount,
        String salespersonUserId,
        BigDecimal commissionRate,
        String depositRule,
        Instant latestArrivalAt,
        String customerPhone
) {
    /** 向后兼容构造器（V539 前签名）。 */
    public BarReservationRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String barId,
            String tableId,
            Instant reservationTime,
            Integer guestCount,
            String salespersonUserId,
            BigDecimal commissionRate,
            String depositRule
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, barId, tableId, reservationTime,
                guestCount, salespersonUserId, commissionRate, depositRule, null, null);
    }

    /** 向后兼容构造器（A4 前签名）：既有调用方零改动。 */
    public BarReservationRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String barId,
            String tableId,
            Instant reservationTime,
            Integer guestCount,
            String salespersonUserId,
            BigDecimal commissionRate
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, barId, tableId, reservationTime,
                guestCount, salespersonUserId, commissionRate, null, null, null);
    }
}
