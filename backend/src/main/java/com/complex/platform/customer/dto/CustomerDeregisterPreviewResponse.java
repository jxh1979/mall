package com.complex.platform.customer.dto;

// 文件版本：V0.01

/**
 * TK-21-R3（BSM「账户注销」：「发起注销时系统须提醒其名下尚有……储值卡余额」）：注销预览——本人每张未销户储值卡的本金/赠送金分列。
 * cardScope：project=综合体通用卡（放弃余额归长沙项目公司），merchant=商户级卡（归发卡商户 merchantId）。
 */
public record CustomerDeregisterPreviewResponse(
        String accountId,
        String cardScope,
        String merchantId,
        long principalCent,
        long bonusCent
) {
}
