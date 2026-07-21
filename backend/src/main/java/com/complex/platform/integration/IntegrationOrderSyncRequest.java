package com.complex.platform.integration;

import java.util.List;
import java.util.Map;

public record IntegrationOrderSyncRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String providerCode,
        String businessType,
        String syncType,
        String externalBusinessId,
        String status,
        Long amountCent,
        String currency,
        String channel,
        String externalChannelOrderId,
        // 源B（对账文件回流）行本带商户 → 直落。为空则走源A：按渠道门店号查映射表反查（未命中留 NULL）。
        String merchantId,
        Map<String, Object> providerPayload
) {
}
