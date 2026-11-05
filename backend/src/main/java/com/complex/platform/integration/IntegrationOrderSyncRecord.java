package com.complex.platform.integration;

import java.time.Instant;
import java.util.Map;

public record IntegrationOrderSyncRecord(
        String id,
        String providerCode,
        String businessType,
        String syncType,
        String externalBusinessId,
        String status,
        long amountCent,
        String currency,
        String channel,
        String externalChannelOrderId,
        // 落商户归属：NULL = 未归属（第三方给不到门店 / 未维护映射 / 存量历史行），下游仅平台级，不静默塞给某商户。
        String merchantId,
        Map<String, Object> providerPayload,
        Instant createdAt,
        Instant updatedAt,
        // 第十八批 U 段第 155 条 155-R3：退款所退原订单的外部单号（餐饮、零售退款必填；其余为空）。
        String originalExternalBusinessId
) {
    /** 不带原订单外部单号（订单、券核销、门票回流）。 */
    public IntegrationOrderSyncRecord(String id, String providerCode, String businessType, String syncType, String externalBusinessId,
            String status, long amountCent, String currency, String channel, String externalChannelOrderId, String merchantId,
            Map<String, Object> providerPayload, Instant createdAt, Instant updatedAt) {
        this(id, providerCode, businessType, syncType, externalBusinessId, status, amountCent, currency, channel, externalChannelOrderId,
                merchantId, providerPayload, createdAt, updatedAt, null);
    }
}
