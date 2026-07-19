package com.complex.platform.finance;

/**
 * 退租结算落财务台账端口（finance 实现，合同域经 @Lazy 反向依赖调用，避免初始化环）。
 * 合同退租变更（move_out/early_termination）终审通过时，由合同域调用把结算净额落到应收应付台账。
 * 幂等键统一取结算单 id，使派生台账项 id 确定、重复审批/重放不双挂。
 */
public interface TerminationSettlementPort {

    /** 应退押金净额（押金抵未清后&gt;0）挂应付给商户：direction=payable, source_type='deposit_refund'。返回台账项 id。 */
    String createDepositRefundPayable(String merchantId, long netRefundCent, String settlementId);

    /** 提前退租违约金挂应收：direction=receivable, source_type='termination_penalty'。返回台账项 id。 */
    String createTerminationPenaltyReceivable(String merchantId, long penaltyCent, String settlementId);
}
