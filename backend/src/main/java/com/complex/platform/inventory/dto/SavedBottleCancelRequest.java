package com.complex.platform.inventory.dto;

import java.util.List;

/**
 * 存酒作废请求（A1/REQ-32 + R5 T6 高价值复核）。
 *
 * <p>高价值存酒作废须提供<b>照片 + 视频双证据</b>（{@code photoAttachmentIds} 与
 * {@code videoAttachmentIds} 各非空，真正区分类型，替换旧"附件数≥2 不分类型"弱校验），
 * 命中后不直接作废，改为建库存域内高价值复核（pending），主管 approve 后才真正作废。
 * 非高价值作废维持直接 cancel（向后兼容）。{@code evidenceAttachmentIds} 保留为兼容字段
 * （落入复核单的综合证据，不再单独作为放行判据）。
 */
public record SavedBottleCancelRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String reason,
        List<String> photoAttachmentIds,
        List<String> videoAttachmentIds
) {
}
