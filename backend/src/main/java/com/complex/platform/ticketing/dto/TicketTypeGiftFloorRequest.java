package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

/**
 * 第十八批 AH 段 215-R2 改票种最低赠酒杯数（ENUM:34「按票种可配，出厂默认 1 杯，须同时配置上下限」）：三个值须同时填写，
 * 且 1 ≤ 下限 ≤ 杯数 ≤ 上限。
 *
 * @param minGiftCups      最低赠酒杯数
 * @param minGiftCupsLower 最低赠酒杯数下限
 * @param minGiftCupsUpper 最低赠酒杯数上限
 */
public record TicketTypeGiftFloorRequest(
        String idempotencyKey,
        Integer minGiftCups,
        Integer minGiftCupsLower,
        Integer minGiftCupsUpper
) {
}
