package com.complex.platform.serviceorder.dto;

public record ServiceCatalogItemRequest(
        String idempotencyKey,
        String id,
        String name,
        Long priceCent,
        Integer durationMinutes,
        Boolean active,
        String mgmtFeeMode,
        Long mgmtFeeValue
) {
}
