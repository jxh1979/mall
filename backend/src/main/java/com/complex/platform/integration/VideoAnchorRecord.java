package com.complex.platform.integration;

import java.time.Instant;

/**
 * R5 T14 视频"点位 + 时间锚点"。本期只写锚点（point_id + event_time + business_ref），
 * clipIndexRef 留空待厂商监控片段索引接入。
 */
public record VideoAnchorRecord(
        String id,
        String pointId,
        Instant eventTime,
        String businessRef,
        String clipIndexRef,
        Instant createdAt
) {
}
