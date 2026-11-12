package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 第十八批 AH 段 215-R2：票种的最低赠酒杯数（ENUM:34「后台配置的最低赠酒杯数（业务参数，按票种可配，出厂默认 1 杯，须同时配置上下限）」）。
 * 没配过的票种按出厂 1 杯，上下限、改动人与时间为空（HTML 未给上下限的出厂数值）。
 *
 * @param ticketTypeId     票种 id
 * @param minGiftCups      最低赠酒杯数（通票覆盖的每家酒吧，每次入场至少赠这么多杯）
 * @param minGiftCupsLower 最低赠酒杯数下限
 * @param minGiftCupsUpper 最低赠酒杯数上限
 * @param updatedBy        最后改动人
 * @param updatedAt        最后改动时间
 */
public record TicketTypeGiftFloor(
        String ticketTypeId,
        int minGiftCups,
        Integer minGiftCupsLower,
        Integer minGiftCupsUpper,
        String updatedBy,
        Instant updatedAt
) {
}
