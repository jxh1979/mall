package com.complex.platform.terminal.dto;

import java.util.List;

/** R5 T9 MDM 自有侧：app 白名单新增/更新请求。 */
public record MdmAppWhitelistRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String packageName,
        String appLabel,
        String scopeType,
        String scopeId,
        Boolean enabled,
        String reason
) {
}
