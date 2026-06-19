package com.complex.platform.bar;

import java.util.List;

/** V091 小费请求：向账单追加小费（额/比例之一），计入 payable 与结算（代收口径，不计低消/提成基数）。 */
public record BarBillTipRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Long tipAmountCent,
        Integer tipRatioBps
) {
}
