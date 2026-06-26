package com.complex.platform.energy.dto;

/** 按期结算能源费。 */
public record EnergySettleRequest(
        String idempotencyKey,
        String period) {
}
