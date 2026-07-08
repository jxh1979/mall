package com.complex.platform.finance;

/**
 * 提成租金计算端口（供合同域排租金按「收租方式」组合月租+提成时调用）。
 * finance 侧实现：取该商户该自然月回流营业额 + 规则阶梯 → 提成额（分）。ruleId 空/规则无效返 0。
 * 依赖方向 contract→finance（finance 调 ContractRentMath 为 static，无 bean 循环）。period 形如 "YYYY-MM"。
 */
public interface TurnoverRentPort {
    long computePeriodTurnoverRent(String merchantId, String contractId, String period);
}
