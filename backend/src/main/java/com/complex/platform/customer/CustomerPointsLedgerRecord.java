package com.complex.platform.customer;

import java.time.Instant;

/**
 * 积分流水。P0 清单#2 积分入账引擎扩展（V100）：在原 11 字段基础上追加
 * change_type（earn/deduct/expire/...）、status（pending/confirmed/reversed）、
 * source_doc_type/source_doc_id（业务单据来源 + 幂等键），用于
 * 支付入 pending、履约转 confirmed、退款按比例 deduct 的入账引擎。
 *
 * <p>向后兼容：保留原 11 参构造器（既有 earn/expire/check_in/transfer 调用方不变），
 * 新字段默认 changeType=sourceType、status="confirmed"、source_doc_* 为空。
 */
public record CustomerPointsLedgerRecord(
        String id,
        String customerId,
        long pointsDelta,
        long balanceAfter,
        String sourceType,
        String sourceId,
        String description,
        Instant expireAt,
        Long remainingPoints,
        boolean expired,
        Instant createdAt,
        String changeType,
        String status,
        String sourceDocType,
        String sourceDocId,
        long confirmedAmount
) {

    /** 入账引擎状态常量。 */
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_REVERSED = "reversed";

    /** 变动类型常量（入账引擎用）。 */
    public static final String CHANGE_EARN = "earn";
    public static final String CHANGE_DEDUCT = "deduct";

    /**
     * 向后兼容构造器（原 11 字段）。新字段默认：
     * changeType 回退为 sourceType、status=confirmed、source_doc_* 为空（不参与幂等）。
     */
    public CustomerPointsLedgerRecord(
            String id,
            String customerId,
            long pointsDelta,
            long balanceAfter,
            String sourceType,
            String sourceId,
            String description,
            Instant expireAt,
            Long remainingPoints,
            boolean expired,
            Instant createdAt
    ) {
        this(id, customerId, pointsDelta, balanceAfter, sourceType, sourceId, description,
                expireAt, remainingPoints, expired, createdAt,
                sourceType, STATUS_CONFIRMED, null, null, 0L);
    }

    /**
     * 兼容构造器（V100 的 15 字段，confirmed_amount 缺省 0）。
     * 新写入的 pending earn 行 confirmed_amount=0（尚未确认）；确认份额由 confirmPoints 经
     * {@code updatePointsLedgerConfirmation} 回写，不走构造器。
     */
    public CustomerPointsLedgerRecord(
            String id,
            String customerId,
            long pointsDelta,
            long balanceAfter,
            String sourceType,
            String sourceId,
            String description,
            Instant expireAt,
            Long remainingPoints,
            boolean expired,
            Instant createdAt,
            String changeType,
            String status,
            String sourceDocType,
            String sourceDocId
    ) {
        this(id, customerId, pointsDelta, balanceAfter, sourceType, sourceId, description,
                expireAt, remainingPoints, expired, createdAt,
                changeType, status, sourceDocType, sourceDocId, 0L);
    }
}
