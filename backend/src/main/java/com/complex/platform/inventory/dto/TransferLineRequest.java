package com.complex.platform.inventory.dto;

import java.util.List;

public record TransferLineRequest(
        String itemId,
        Integer quantity,
        List<String> traceCodes,
        Long unitPriceCent
) {
}
