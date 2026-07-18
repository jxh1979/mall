package com.complex.platform.dividend.dto;

/** 计算单状态机动作（提交审阅/审批/驳回/标记发放）。驳回时 note=驳回理由，必填。 */
public record DividendActionRequest(
        String idempotencyKey,
        String note) {
}
