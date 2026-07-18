package com.complex.platform.energy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 能耗仓储。合同读取和 AR 写入在此处复用既有表，不反向改合同域。 */
public interface EnergyRepository {

    List<EnergyMeterRecord> listMeters();

    Optional<EnergyMeterRecord> findMeterById(String id);

    EnergyMeterRecord saveMeter(EnergyMeterRecord meter);

    List<EnergyMeterReadingRecord> listReadings(String period);

    Optional<EnergyMeterReadingRecord> findReadingById(String id);

    Optional<EnergyMeterReadingRecord> findReadingByMeterAndPeriod(String meterId, String period);

    Optional<EnergyMeterReadingRecord> findLatestReadingBefore(String meterId, String period);

    EnergyMeterReadingRecord saveReading(EnergyMeterReadingRecord reading);

    Optional<EnergyAnomalyThresholdRecord> findThresholdByMeterType(String meterType);

    List<EnergyMeterReadingRecord> listChildReadings(String parentMeterId, String period);

    List<EnergyContractSnapshot> listEffectiveContracts(String period);

    BigDecimal sumConsumptionByGatherObject(String gatherObject, String period, String projectId);

    BigDecimal sumMerchantConsumption(String merchantId, String period, String projectId);

    boolean energyFeeArExists(String merchantId, String contractId, String period);

    EnergyFeeSettlementRecord insertEnergyFeeAr(EnergyFeeSettlementRecord record);

    record EnergyContractSnapshot(
            String contractId,
            String merchantId,
            String projectId,
            String energyFeeBasis,
            Long energyFeeCent,
            BigDecimal areaSquareMeter,
            Long energyUnitPriceCent) {
    }
}
