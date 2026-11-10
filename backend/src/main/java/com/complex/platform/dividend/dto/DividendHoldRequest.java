package com.complex.platform.dividend.dto;

// 文件版本：V0.01

/**
 * 211-R1 整期暂不分红／暂缓分红登记（第十八批裁决 43「平台财务在持股分红页按商户按期登记（暂不分红或暂缓分红，原因、协商结果必填，不另走审批）」）：
 * holdType=skip 暂不分红 / defer 暂缓分红；periodLabel 须与该商户生效方案的周期一致。
 */
public record DividendHoldRequest(
        String idempotencyKey,
        String merchantId,
        String periodLabel,
        String holdType,
        String reason,
        String negotiationResult) {

    /** 恢复暂缓分红：liftResult＝恢复分红的协商结果，必填。 */
    public record Lift(String idempotencyKey, String liftResult) {
    }
}
