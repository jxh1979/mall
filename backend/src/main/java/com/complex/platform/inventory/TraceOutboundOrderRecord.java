package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 第十八批 K 段第 94 条 94-R1（BSM:1197「出库须同时核验订单和物品码」）：库管出库可选的酒吧订单——含该物品按瓶出品、待出品明细的订单；
 * productName 为这些明细的商品名，quantity 为这些明细的数量之和。
 */
public record TraceOutboundOrderRecord(String orderId, String tableName, String productName, int quantity, Instant createdAt) {
}
