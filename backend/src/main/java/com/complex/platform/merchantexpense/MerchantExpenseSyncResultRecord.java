package com.complex.platform.merchantexpense;

import java.util.List;

/**
 * 自动挂接执行结果：本次新计入的能耗 / 物业费（账单路径 + 合同计费路径）/ 租金（合同租金 + 营业额抽成）
 * 行数（重跑幂等，已计入的为 0）。
 * drift = 金额漂移报告：已挂接源的台账净额（原行 + 冲销行合计）与上游单据当前金额不一致的清单
 * （上游锁定后补录审批可原地改金额，台账是快照式挂接不自动追改——发现漂移按 SOP 红字冲销旧行 + 手工录入修正额）。
 */
public record MerchantExpenseSyncResultRecord(
        String period,
        int energyInserted,
        int propertyInserted,
        int contractPropertyInserted,
        int rentInserted,
        List<MerchantExpenseDriftRecord> drift) {
}
