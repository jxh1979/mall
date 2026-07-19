package com.complex.platform.merchantexpense;

import java.util.List;

/**
 * 自动挂接执行结果：本次新计入的能耗 / 物业费（账单路径 + 合同计费路径）/ 租金（合同租金 + 营业额抽成）
 * 行数（重跑幂等，已计入的为 0）。
 * drift = 金额漂移报告：已挂接源的台账净额（原行 + 冲销行合计）与上游单据当前金额不一致的清单
 * （上游锁定后补录审批可原地改金额，台账是快照式挂接不自动追改——发现漂移按 SOP 红字冲销旧行 + 手工录入修正额）。
 * lockedMerchants = 该月「费用侧冻结」（关账）的商户 id 清单——语义是<b>该期全部被 approved/paid 正式分红单
 * 覆盖的商户</b>（非仅本次自动挂接跳过的那批；未跳过=本就无该期上游待挂行）。费用侧冻结只锁费用台账
 * merchant_expense_entries：自动挂接对这些 (商户,月份) 一律跳过、手工录入/冲销亦拒（避免已发放分红的费用
 * 基数被追改而漂移）；营收/成本源头按设计不锁、已发单金额靠 V299 计算单快照冻结兜底。如需修正先作废该分红单解锁。
 */
public record MerchantExpenseSyncResultRecord(
        String period,
        int energyInserted,
        int propertyInserted,
        int contractPropertyInserted,
        int rentInserted,
        List<MerchantExpenseDriftRecord> drift,
        // 语义=当月全部关账商户，非仅「本次跳过」的（见类型 javadoc）。
        List<String> lockedMerchants) {
}
