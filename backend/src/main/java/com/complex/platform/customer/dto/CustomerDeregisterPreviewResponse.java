package com.complex.platform.customer.dto;

// 文件版本：V0.02

import java.util.List;

/**
 * 注销预览（BSM 第 863 行「发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、放弃余额，径行注销」）：
 * storedValueCards＝本人每张未销户储值卡的本金/赠送金分列（TK-21-R3）；savedBottles＝本人名下在保管的存酒（ROOT-03-R21，与「我的存酒」同一安全投影）；
 * refundableTickets＝本人持有、票种允许退款且完全未用的票（操作员裁决第十三批第 8 条「先退票才能注销」，与「我的票」同一安全投影）。
 */
public record CustomerDeregisterPreviewResponse(List<StoredValueCard> storedValueCards, List<CustomerSavedBottleResponse> savedBottles,
        List<com.complex.platform.ticketing.CustomerTicketResponse> refundableTickets) {

    /** cardScope：project=综合体通用卡（放弃余额归长沙项目公司），merchant=商户级卡（归发卡商户 merchantId）。 */
    public record StoredValueCard(String accountId, String cardScope, String merchantId, long principalCent, long bonusCent) {
    }
}
