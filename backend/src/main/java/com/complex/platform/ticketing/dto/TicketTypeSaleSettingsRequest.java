package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 第十八批 AW 段 299-R1 票种售卖设置（ENUM:30「待启用（规则已配置但未到生效时间）、售卖中（当前可售卖）、暂停售卖（临时停止售卖）、
 * 已结束（售卖周期结束）」）：整体替换。
 *
 * @param idempotencyKey 操作幂等键
 * @param reason         理由（可空，有填即记入留痕）
 * @param salePaused     是否暂停售卖（必填）
 * @param saleStartsAt   开售时间（空＝不限）
 * @param saleEndsAt     停售时间（空＝不限；两端都填时须晚于开售时间）
 */
public record TicketTypeSaleSettingsRequest(
        String idempotencyKey,
        String reason,
        Boolean salePaused,
        Instant saleStartsAt,
        Instant saleEndsAt
) {
}
