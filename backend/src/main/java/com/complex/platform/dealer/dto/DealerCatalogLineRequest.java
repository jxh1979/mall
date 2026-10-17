package com.complex.platform.dealer.dto;

import java.time.LocalDate;

public record DealerCatalogLineRequest(String itemId, String spec, Long unitPriceCent, Integer taxRateBps, LocalDate expectedRestockDate) {
}
