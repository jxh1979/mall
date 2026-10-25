package com.complex.platform.customer.dto;

// 文件版本：V0.03

import java.util.List;

/**
 * 注销预览（BSM 第 863 行「发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、放弃余额，径行注销」）：
 * storedValueCards＝本人每张未销户储值卡的本金/赠送金分列（TK-21-R3）；savedBottles＝本人名下在保管的存酒（ROOT-03-R21，与「我的存酒」同一安全投影）。
 * 2026-10-25 操作员更正：注销时名下未核销的票一律作废、不退款，预览不再列「可退票」。
 */
public record CustomerDeregisterPreviewResponse(List<StoredValueCard> storedValueCards, List<CustomerSavedBottleResponse> savedBottles) {

    /** cardScope：project=综合体通用卡（放弃余额归长沙项目公司），merchant=商户级卡（归发卡商户 merchantId）。 */
    public record StoredValueCard(String accountId, String cardScope, String merchantId, long principalCent, long bonusCent) {
    }
}
