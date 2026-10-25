package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/**
 * R5 T6 高价值复核裁决请求（主管 approve/reject）。
 *
 * <p>{@code decision} 取 {@code approved}/{@code rejected}；approve 后高价值动作生效
 * （存酒置 cancelled / 报损扣减落账 / 盘点差异确认），reject 则回滚（保持原态、不扣减），并写
 * reviewer/reason/reviewed_at。
 *
 * <p>OBS-13-02：{@code role} 只用于存酒库房存货处置（saved_bottle_stock_disposal）的两道审批——supervisor（门店主管）/finance（财务），
 * 同赔付方案审批；其余复核对象不读。
 */
public record HighValueReviewDecisionRequest(
        String idempotencyKey,
        String decision,
        String reason,
        String role
) {
    public HighValueReviewDecisionRequest(String idempotencyKey, String decision, String reason) {
        this(idempotencyKey, decision, reason, null);
    }
}
