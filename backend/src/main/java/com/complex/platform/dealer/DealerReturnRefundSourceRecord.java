package com.complex.platform.dealer;

// 文件版本：V0.01

/** 由数据库原退款与原退货绑定解析，客户端不得自报。 */
public record DealerReturnRefundSourceRecord(String returnId, String orderId, String projectId, String inspectedBy, String component) {
}
