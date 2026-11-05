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
        Map<String, Object> providerPayload,
        // 第十八批 U 段第 155 条 155-R3：退款所退原订单的外部单号（同渠道、同业务类型原订单的 external_business_id），餐饮、零售退款必填。
        String originalExternalBusinessId
) {
    /** 不带原订单外部单号（订单、券核销、门票回流）。 */
    public IntegrationOrderSyncRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String providerCode,
            String businessType, String syncType, String externalBusinessId, String status, Long amountCent, String currency, String channel,
            String externalChannelOrderId, String merchantId, Map<String, Object> providerPayload) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, providerCode, businessType, syncType, externalBusinessId, status, amountCent,
                currency, channel, externalChannelOrderId, merchantId, providerPayload, null);
    }
}
