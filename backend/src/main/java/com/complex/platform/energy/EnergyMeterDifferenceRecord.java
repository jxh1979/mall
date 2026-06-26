package com.complex.platform.energy;

import java.math.BigDecimal;

/** 总分表抹差：总表 consumption - 子表 consumption 合计。 */
public record EnergyMeterDifferenceRecord(
        String parentMeterId,
        String period,
        BigDecimal parentConsumption,
        BigDecimal childConsumption,
        BigDecimal difference) {
}
