package com.complex.platform.bar;

import java.util.List;

/** V091 反结请求：将已结账单 settled→reopened，需原因 + 高危权限 + 审批。 */
public record BarBillReopenRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String reason
) {
}
