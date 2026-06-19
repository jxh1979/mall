package com.complex.platform.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 自动对账请求（P0#11）：从 order_syncs 标准模型按 (provider_code, business_type, channel, period)
 * 自动比对平台侧期望明细，生成对账批次 + 差异（source_domain=thirdparty）。
 *
 * <p>expectedEntries：平台侧系统取数的期望明细（external_business_id → 期望金额/状态/出现次数），
 * 由调用方（衔接 finance 侧四类差异口径，但不依赖 finance 模块）提供；
 * order_syncs 标准模型为第三方侧 actual。开关 off（适配器 webhook 关）时，强制要求 dryRun=true，
 * 不落任何对账数据（不伪造）。
 */
public record ChannelReconciliationRequest(
        String idempotencyKey,
        String providerCode,
        String businessType,
        String channel,
        LocalDate periodStart,
        LocalDate periodEnd,
        Boolean dryRun,
        List<ExpectedEntry> expectedEntries
) {

    /** 平台侧期望明细（系统取数口径）。amountCent 缺省 0；expectedCount 默认 1（同一外部单出现次数）。 */
    public record ExpectedEntry(
            String externalBusinessId,
            Long amountCent,
            String status,
            Integer expectedCount,
            Map<String, Object> meta
    ) {
    }
}
