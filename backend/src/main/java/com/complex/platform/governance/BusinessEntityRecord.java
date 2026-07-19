package com.complex.platform.governance;

import java.time.Instant;

public record BusinessEntityRecord(
        String id,
        String entityName,
        String entityRole,
        // 四层组织(terminology-canonical §一):L3 管理公司/业态主体 的 parent = L2 项目公司;L2 的 parent = L1 平台。
        // null = 顶层(平台)或未挂接。用于「一个项目公司下有多个管理公司主体」的层级落库。
        String parentEntityId,
        String unifiedSocialCreditCode,
        String legalRepresentative,
        String bankAccountRef,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
