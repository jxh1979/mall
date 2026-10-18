package com.complex.platform.dealer;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

/** 经销电子发票。status 沿用 ENUM「开票状态」：pending / issuing / issued / reversed / voided / failed；开票主体＝合同销售主体（ENUM:182）。 */
public record DealerInvoiceRecord(
        String id,
        String orderId,
        String dealerId,
        String salesEntityId,
        long amountCent,
        Integer taxRateBps,
        String status,
        String invoiceNo,
        String reversedByReturnId,
        String reissuedFromInvoiceId,
        String requestedBy,
        Instant requestedAt,
        String issuedBy,
        Instant issuedAt,
        Instant reversedAt,
        String note,
        Long retainedGoodsCent, Long retainedFreightCent, boolean requiresReversal, String reversalStatus,
        String reversalReferenceNo, List<String> issueEvidenceAttachmentIds, List<String> reversalEvidenceAttachmentIds
) {
    public DealerInvoiceRecord(String id,String orderId,String dealerId,String salesEntityId,long amountCent,Integer taxRateBps,String status,
            String invoiceNo,String reversedByReturnId,String reissuedFromInvoiceId,String requestedBy,Instant requestedAt,String issuedBy,
            Instant issuedAt,Instant reversedAt,String note) {
        this(id,orderId,dealerId,salesEntityId,amountCent,taxRateBps,status,invoiceNo,reversedByReturnId,reissuedFromInvoiceId,
                requestedBy,requestedAt,issuedBy,issuedAt,reversedAt,note,null,null,false,null,null,List.of(),List.of());
    }
}
