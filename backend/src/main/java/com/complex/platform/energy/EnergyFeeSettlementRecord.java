package com.complex.platform.energy;

// 文件版本：V0.02
import java.time.Instant;
import java.time.LocalDate;

/** 独表实际费用的代收往来摘要；不计入项目经营收入。 */
public record EnergyFeeSettlementRecord(String id,String contractId,String merchantId,String projectId,
        String period,String energyFeeBasis,long amountCent,String arApItemId,String sourceType,
        String sourceId,String direction,String status,Instant createdAt,LocalDate dueDate) { }
