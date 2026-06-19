package com.complex.platform.terminal.dto;

import java.util.List;

/** R5 T9 MDM 自有侧：网站白名单新增/更新请求。 */
public record MdmSiteWhitelistRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String domain,
        String description,
        String scopeType,
        String scopeId,
        Boolean enabled,
        String reason
) {
}
