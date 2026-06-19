package com.complex.platform.inventory.dto;

/**
 * R5 T6 高价值复核裁决请求（主管 approve/reject）。
 *
 * <p>{@code decision} 取 {@code approved}/{@code rejected}；approve 后高价值动作生效
 * （存酒置 cancelled / 报损扣减落账 / 盘点差异确认），reject 则回滚（保持原态、不扣减），并写
 * reviewer/reason/reviewed_at。
 */
public record HighValueReviewDecisionRequest(
        String idempotencyKey,
        String decision,
        String reason
) {
}
