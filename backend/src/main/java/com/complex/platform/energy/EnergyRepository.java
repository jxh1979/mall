package com.complex.platform.energy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 能耗表台账 + 抄表读数（energy_meters / energy_meter_readings，V168）仓储。 */
public interface EnergyRepository {

    EnergyMeterRecord saveMeter(EnergyMeterRecord meter);

    List<EnergyMeterRecord> listMeters();

    Optional<EnergyMeterRecord> findMeterById(String id);

    EnergyMeterReadingRecord saveReading(EnergyMeterReadingRecord reading);

    List<EnergyMeterReadingRecord> listReadings();

    /** 同表同期次的已有读数（重复抄表判定）。 */
    Optional<EnergyMeterReadingRecord> findReading(String meterId, String period);

    /** 上期表底（period < 本期的最近一条），用于算本期用量；无则空（首读）。 */
    Optional<BigDecimal> findLatestReadingValueBefore(String meterId, String period);
}
