package com.complex.platform.dealer.dto;

import java.util.List;

/** reasonType：unopened_resalable 未拆封可复售 / quality_issue 质量问题（BSM:1240，其余原因一律拒绝）。 */
public record DealerReturnRequest(String idempotencyKey, String reasonType, List<DealerReturnLineRequest> lines, String note) {
}
