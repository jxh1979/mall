package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.util.List;

/**
 * FS-019 借用归还（BSM L1201「酒水与可追责物品的交接，每一步都必须扫码」「实际归还时间取借出方收货扫码的时刻」「结束对应数量的原物归还事项」）：
 * 借出方收货时逐行登记归还数量与扫到的溯源码；有随货码的行码数须等于数量，无码行只登记数量。可分多次归还。
 */
public record ReturnTransferRequest(
        String idempotencyKey,
        String operatorNote,
        List<ReturnTransferLineRequest> lines
) {
    public record ReturnTransferLineRequest(String lineId, Integer returnedQuantity, List<String> traceCodes) {
    }
}
