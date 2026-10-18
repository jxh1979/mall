package com.complex.platform.bar;

// 文件版本：V0.01
import java.util.List;

/** 小费以原客人账单确定责任场次；合单后须指定来源。 */
public record BarBillTipRequest(String idempotencyKey,String operatorNote,List<String> evidenceAttachmentIds,
        Long tipAmountCent,Integer tipRatioBps,String sourceBillId) {
    public BarBillTipRequest(String idempotencyKey,String operatorNote,List<String> evidenceAttachmentIds,Long tipAmountCent,Integer tipRatioBps) {
        this(idempotencyKey,operatorNote,evidenceAttachmentIds,tipAmountCent,tipRatioBps,null);
    }
}
