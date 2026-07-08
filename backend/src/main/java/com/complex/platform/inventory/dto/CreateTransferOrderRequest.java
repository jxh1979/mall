package com.complex.platform.inventory.dto;

import java.util.List;

public record CreateTransferOrderRequest(
        String idempotencyKey,
        String transferType,
        String lendingMode,
        String fromOwnerType,
        String fromOwnerId,
        String toOwnerType,
        String toOwnerId,
        String fromLocationId,
        String toLocationId,
        String operatorNote,
        List<TransferLineRequest> lines
) {
}
