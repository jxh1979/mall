package com.complex.platform.finance.dto;

import java.util.List;

/**
 * F-OPS-08 物业费按期计费：对 projectId 下指定 merchantIds，按「每平米固定单价 × 商户面积」生成应收，不退不补。
 * <p>merchants 表无 project_id，故商户名单由运营显式提供（类比已废止 CAM 的 shares）；费率按 projectId + period 解析。
 */
public record PropertyFeeBillRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        String period,
        List<String> merchantIds
) {
}
