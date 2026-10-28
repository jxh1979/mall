// 文件版本：V0.01
package com.complex.platform.compliance.dto;

import java.time.LocalDate;
import java.util.List;

/** 第十八批 E 段第 22 条（PGM:531）按证照类型配置、宽限期申请与续期登记的请求体（前端蛇形键）。 */
public final class ComplianceGraceRequests {

    private ComplianceGraceRequests() {
    }

    /** PUT /compliance/item-type-configs/{item_type}：宽限三项、预警三项各自同空或同填。 */
    public record ItemTypeConfigRequest(
            String idempotencyKey,
            Integer graceDays,
            Integer graceMinDays,
            Integer graceMaxDays,
            Integer warningMonths,
            Integer warningMinMonths,
            Integer warningMaxMonths
    ) {
    }

    /** POST /compliance/expiry-items/{id}/grace-requests。 */
    public record GraceRequest(String idempotencyKey, String reason) {
    }

    /** POST /compliance/expiry-items/{id}/renewal：新的有效期止必须晚于原有效期止；未填的证号、起始日、凭证沿用原值。 */
    public record RenewalRequest(
            String idempotencyKey,
            LocalDate validFrom,
            LocalDate validTo,
            String documentNo,
            List<String> evidenceAttachmentIds
    ) {
    }
}
