package com.complex.platform.dealer.dto;

import java.time.LocalDate;

public record DealerContractRequest(String idempotencyKey, String contractNo, String salesEntityId, String centralWarehouseId,
        LocalDate validFrom, LocalDate validTo) {
}
