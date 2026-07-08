package com.complex.platform.inventory.dto;

import java.util.List;

/** 扫码收货:收货方扫描随货溯源码核对(高价值必扫·防调包)。 */
public record ReceiveTransferRequest(
        String idempotencyKey,
        List<String> scannedTraceCodes,
        String operatorNote
) {
}
