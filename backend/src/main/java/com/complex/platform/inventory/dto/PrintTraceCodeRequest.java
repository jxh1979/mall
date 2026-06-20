package com.complex.platform.inventory.dto;

/**
 * V128 / REQ-39 溯源码出库打印请求。仅需幂等键：status registered→printed，记 printed_at，条件 UPDATE 防重复打印。
 */
public record PrintTraceCodeRequest(
        String idempotencyKey
) {
}
