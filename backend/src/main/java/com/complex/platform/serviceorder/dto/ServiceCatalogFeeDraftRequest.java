package com.complex.platform.serviceorder.dto;

// 文件版本：V0.01

import java.time.LocalDate;

/** TK-23-R3:已上架目录项的管理费变更草案(比照销售提成:草案→差人复核转正→指定生效日形成新版本)。 */
public record ServiceCatalogFeeDraftRequest(
        String idempotencyKey,
        String mgmtFeeMode,
        Long mgmtFeeValue,
        LocalDate effectiveFrom
) {
}
