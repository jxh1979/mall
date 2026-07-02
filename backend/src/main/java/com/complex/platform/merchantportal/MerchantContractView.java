package com.complex.platform.merchantportal;

import com.complex.platform.contract.ContractRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 商户专区「我的合同」只读关键信息(PRD 3.18 §3:租期/空间/状态):
 * 最小披露——不含平台侧审批链、收益固化快照、报表口径等内部字段。
 */
public record MerchantContractView(
        String id,
        String merchantId,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String lifecycleStatus,
        Long depositCent,
        String projectId,
        List<String> spaceIds,
        String renewedFromContractId,
        Instant createdAt,
        Instant updatedAt
) {
    public static MerchantContractView of(ContractRecord contract, List<String> spaceIds) {
        return new MerchantContractView(
                contract.id(),
                contract.merchantId(),
                contract.effectiveDate(),
                contract.expiryDate(),
                contract.lifecycleStatus(),
                contract.depositCent(),
                contract.projectId(),
                spaceIds == null ? List.of() : List.copyOf(spaceIds),
                contract.renewedFromContractId(),
                contract.createdAt(),
                contract.updatedAt()
        );
    }
}
