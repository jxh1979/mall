package com.complex.platform.finance;

/**
 * 退租结算落财务台账端口（finance 实现，合同域经 @Lazy 反向依赖调用，避免初始化环）。
 * 合同退租变更（move_out/early_termination）终审通过时，由合同域调用把结算净额落到应收应付台账。
 * 幂等键统一取结算单 id，使派生台账项 id 确定、重复审批/重放不双挂。
 */
public interface TerminationSettlementPort {

    /**
     * 应退押金净额（押金抵未清后&gt;0）落台账：FOR UPDATE 锁定权威余额 → 按锁定余额封顶（race-safe）→
     * 挂 ar_ap 应付 + 写 deposit_ledger refund 行扣减余额。返回实退值（≤ 请求净额；余额被其它退租消费则更低）。
     */
    long createDepositRefundPayable(String merchantId, long netRefundCent, String settlementId);

    /** 提前退租违约金挂应收：direction=receivable, source_type='termination_penalty'。返回台账项 id。 */
    String createTerminationPenaltyReceivable(String merchantId, long penaltyCent, String settlementId);

    /**
     * 应退押金封顶值：min(合同固化押金额, 该商户押金台账实收余额)。只退商户实际交纳的，不超退。
     * 粒度取舍：deposit_ledger 按 merchant_id 记账（商户级），一商户多合同时用商户总余额封顶=保守信号。
     */
    long effectiveRefundableDeposit(String merchantId, long contractDepositCent);
}
