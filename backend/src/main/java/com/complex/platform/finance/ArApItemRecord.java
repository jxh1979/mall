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
        @JsonIgnore String projectId,
        // P0-B 红冲溯源(V344):本行冲正/重述的原台账行 id。原始行为 null;
        //   非空且金额为负=冲正行(原行的严格取反),非空且金额为正=重述行(原额+补录额)。
        String originItemId,
        // P0-B 红冲溯源(V344):产生本行的「锁定后补录」审批工单 id。原始行为 null。
        String adjustmentId
) {
    /** 向后兼容构造(无红冲溯源列):既有原始行写入方不变,originItemId/adjustmentId 置 null。 */
    public ArApItemRecord(
            String id, String itemNo, String direction, String counterpartyId,
            String sourceType, String sourceId, long amountCent, long paidAmountCent,
            long unpaidAmountCent, String currency, LocalDate dueDate, String accountingPeriod,
            String status, Instant createdAt, Instant updatedAt, String merchantId, String projectId) {
        this(id, itemNo, direction, counterpartyId, sourceType, sourceId, amountCent,
                paidAmountCent, unpaidAmountCent, currency, dueDate, accountingPeriod,
                status, createdAt, updatedAt, merchantId, projectId, null, null);
    }

    /** 向后兼容构造(无 projectId):供未按项目落账的历史/内部调用方使用,projectId 置 null。 */
    public ArApItemRecord(
            String id, String itemNo, String direction, String counterpartyId,
            String sourceType, String sourceId, long amountCent, long paidAmountCent,
            long unpaidAmountCent, String currency, LocalDate dueDate, String accountingPeriod,
            String status, Instant createdAt, Instant updatedAt, String merchantId) {
        this(id, itemNo, direction, counterpartyId, sourceType, sourceId, amountCent,
                paidAmountCent, unpaidAmountCent, currency, dueDate, accountingPeriod,
                status, createdAt, updatedAt, merchantId, null, null, null);
    }
}
