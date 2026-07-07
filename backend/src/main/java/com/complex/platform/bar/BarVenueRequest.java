package com.complex.platform.bar;

/**
 * 酒吧门店创建/更新请求（V263）。id 空=新建；非空=更新。
 * 新建时 barName + projectId 必填；merchantId/spaceId/barCode/minimumSpendCent 可选。
 */
public record BarVenueRequest(
        String id,
        String projectId,
        String merchantId,
        String spaceId,
        String barName,
        String barCode,
        Long minimumSpendCent,
        String status
) {
}
