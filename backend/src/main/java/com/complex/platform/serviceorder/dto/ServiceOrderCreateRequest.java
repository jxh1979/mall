package com.complex.platform.serviceorder.dto;

public record ServiceOrderCreateRequest(
        String idempotencyKey,
        String catalogItemId,
        String performerUserId,
        String barId,
        String tableId,
        String customerNote
) {
}
