package com.complex.platform.dealer.dto;

/** 验收：acceptedQuantity = resalableQuantity（回可用库存）+ quarantinedQuantity（隔离禁售）。 */
public record DealerReturnInspectionLineRequest(String itemId, String spec, Integer acceptedQuantity, Integer resalableQuantity, Integer quarantinedQuantity, String note) {
}
