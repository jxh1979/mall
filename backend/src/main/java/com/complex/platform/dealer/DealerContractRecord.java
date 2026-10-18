package com.complex.platform.dealer;

import java.time.Instant;
import java.time.LocalDate;

/** 经销合同：指定销售主体（ENUM:182）与中心仓（BSM:1229），挂独立可版本化的价格目录（BSM:1219）。 */
public record DealerContractRecord(
        String id,
        String dealerId,
        String contractNo,
        String salesEntityId,
        String centralWarehouseId,
        LocalDate validFrom,
        LocalDate validTo,
        String status,
        String createdBy,
        Instant createdAt
) {
}
