package com.complex.platform.amusement;

import java.util.List;

/**
 * 游乐项目可用权益类型绑定请求体（写入 amusement_project_entitlements，供 V082 核销绑定校验使用）。
 * 配置后该项目核销将收紧为「仅接受已绑定的权益类型」，不再回退按「游乐类」放行（向后兼容仅在无任何绑定时生效）。
 */
public record AmusementProjectEntitlementRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String entitlementType
) {
}
