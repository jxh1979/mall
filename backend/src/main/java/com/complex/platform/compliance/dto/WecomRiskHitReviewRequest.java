package com.complex.platform.compliance.dto;

/**
 * P0#8 企微违规命中复核处置请求。
 *
 * <p>{@code status}：目标状态 reviewing（领取复核）/ confirmed（确认违规）/ dismissed（误报驳回）；
 * 复核处置人取自鉴权上下文（currentActor），不由请求体传入。{@code reviewNote} 复核备注可空。
 */
public record WecomRiskHitReviewRequest(
        String status,
        String reviewNote
) {
}
