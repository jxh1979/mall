package com.complex.platform.dealer.dto;

// 文件版本：V0.01
import java.util.List;
/** 首程减款须明确金额与原合同/双方确认凭证；不按退货比例猜测金额。 */
public record DealerReturnFreightConfirmRequest(String idempotencyKey, String returnFreightBearer,
        Boolean firstLegFreightRefundable, String note, Long firstLegAdjustmentCent, List<String> evidenceAttachmentIds) {
    public DealerReturnFreightConfirmRequest(String key,String bearer,Boolean refundable,String note) {
        this(key,bearer,refundable,note,null,List.of());
    }
}
