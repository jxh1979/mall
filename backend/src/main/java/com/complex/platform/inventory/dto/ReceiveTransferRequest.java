package com.complex.platform.inventory.dto;

// 文件版本：V0.02

import java.util.List;

/**
 * 扫码收货:收货方扫描随货溯源码核对(高价值必扫·防调包)。
 * FS-011（BSM L1212「签收分别记录实收、合格接收、拒收和短少数量」）：门店要货签收按行登记，合格部分入 warehouseId/locationId。
 */
public record ReceiveTransferRequest(
        String idempotencyKey,
        List<String> scannedTraceCodes,
        String operatorNote,
        String warehouseId,
        String locationId,
        List<ReceiveTransferLineRequest> lines
) {
    public ReceiveTransferRequest(String idempotencyKey, List<String> scannedTraceCodes, String operatorNote) {
        this(idempotencyKey, scannedTraceCodes, operatorNote, null, null, null);
    }

    /** 一行签收：实收＝合格＋拒收；短少＝发货数－实收，由服务算出；acceptedTraceCodes 为合格接收件的溯源码。 */
    public record ReceiveTransferLineRequest(
            String lineId,
            Integer arrivedQuantity,
            Integer acceptedQuantity,
            Integer rejectedQuantity,
            List<String> acceptedTraceCodes
    ) {
    }
}
