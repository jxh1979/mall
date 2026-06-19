package com.complex.platform.inventory.dto;

import java.util.List;

/**
 * P0#4 存酒转赠请求（R6-D05）：将存酒整瓶转赠给受让人。
 *
 * <p>转赠后原瓶置 {@code transferred} 终态，新建一瓶 {@code stored} 归属受让人（姓名昵称 +（可选）手机号），
 * 沿用剩余量/到期/标签等登记信息，受让人凭新瓶取酒核身。
 */
public record SavedBottleTransferRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String toNameOrNickname,
        String toPhone,
        String reason
) {
}
