package com.complex.platform.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;

public record ArApItemRecord(
        String id,
        String itemNo,
        String direction,
        String counterpartyId,
        String sourceType,
        String sourceId,
        long amountCent,
        long paidAmountCent,
        long unpaidAmountCent,
        String currency,
        LocalDate dueDate,
        String accountingPeriod,
        String status,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId,
        // P1-A 财务项目级隔离(与储值同范式):综合体维度,可空(推不出来源项目的历史行留空)。
        @JsonIgnore String projectId
) {
    /** 向后兼容构造(无 projectId):供未按项目落账的历史/内部调用方使用,projectId 置 null。 */
    public ArApItemRecord(
            String id, String itemNo, String direction, String counterpartyId,
            String sourceType, String sourceId, long amountCent, long paidAmountCent,
            long unpaidAmountCent, String currency, LocalDate dueDate, String accountingPeriod,
            String status, Instant createdAt, Instant updatedAt, String merchantId) {
        this(id, itemNo, direction, counterpartyId, sourceType, sourceId, amountCent,
                paidAmountCent, unpaidAmountCent, currency, dueDate, accountingPeriod,
                status, createdAt, updatedAt, merchantId, null);
    }
}
