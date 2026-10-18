package com.complex.platform.dealer.dto;

// 文件版本：V0.01
import java.util.List;
/** 外部实际开票/红冲结果及可核验凭证；请求提交不等于开票或红冲成功。 */
public record DealerInvoiceIssueRequest(String idempotencyKey, String invoiceNo, List<String> evidenceAttachmentIds) {
    public DealerInvoiceIssueRequest(String key,String invoiceNo) { this(key,invoiceNo,List.of()); }
}
