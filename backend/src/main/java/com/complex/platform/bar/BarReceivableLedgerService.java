package com.complex.platform.bar;

import java.util.List;

interface BarReceivableLedgerService {
    String createReceivableForOnAccountBill(
            BarBillRecord bill,
            BarOrderRecord order,
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds
    );
}
