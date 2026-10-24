package com.complex.platform.customer.dto;

// 文件版本：V0.01

import java.time.Instant;

/** 客人自助订台。ROOT-03-R19：latestArrivalAt 最晚到店时间必填（BSM 第 1062 行「无论是否支付定金，每一笔订台都必须填写最晚到店时间」）。 */
public record CustomerBarReservationRequest(
        String idempotencyKey,
        String barId,
        String tableId,
        String tableType,
        Instant reservationTime,
        Integer guestCount,
        String contactPhone,
        Instant latestArrivalAt
) {
    /** 向后兼容构造器（V539 前签名）。 */
    public CustomerBarReservationRequest(String idempotencyKey, String barId, String tableId, String tableType,
            Instant reservationTime, Integer guestCount, String contactPhone) {
        this(idempotencyKey, barId, tableId, tableType, reservationTime, guestCount, contactPhone, null);
    }
}
