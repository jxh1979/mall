package com.complex.platform.finance;

// 文件版本：V0.01

import java.time.LocalDate;
import java.util.List;

/**
 * 退租结算落财务台账端口（finance 实现，合同域经 @Lazy 反向依赖调用，避免初始化环）。
 * 合同退租变更（move_out/early_termination）终审通过时，由合同域调用把结算净额落到应收应付台账。
 * 幂等键统一取结算单 id，使派生台账项 id 确定、重复审批/重放不双挂。
 */
public interface TerminationSettlementPort {

    /**
     * 应退押金净额（押金抵未清应收与违约金后&gt;0）落台账：FOR UPDATE 锁定权威余额 → 按锁定余额封顶（race-safe）→
     * 挂 ar_ap 应付 + 写 deposit_ledger refund 行扣减余额。返回实退值（≤ 请求净额；余额被其它退租消费则更低）。
     * HP-C06：dueDate＝合同域按 max(算账完成日, 最后审批完成日＋等待期) 算好的所在地日期，原样作应付到期日（不推到付账日）。
     */
    long createDepositRefundPayable(String merchantId, long netRefundCent, String settlementId, LocalDate dueDate);

    /** 提前退租违约金挂应收：direction=receivable, source_type='termination_penalty'。返回台账项 id。 */
    String createTerminationPenaltyReceivable(String merchantId, long penaltyCent, String settlementId);

    /**
     * 应退押金封顶值：min(合同固化押金额, 该商户押金台账实收余额)。只退商户实际交纳的，不超退。
     * 粒度取舍：deposit_ledger 按 merchant_id 记账（商户级），一商户多合同时用商户总余额封顶=保守信号。
     */
    long effectiveRefundableDeposit(String merchantId, long contractDepositCent);

    /** 押金抵扣销账一笔：被销账的应收台账项 id 与销账金额（分）。 */
    record DepositOffset(String arApItemId, long amountCent) {
    }

    /**
     * 第四轮裁决第 3 条（财务结算 PRD L957「终止结算既有多级审批全部通过后，系统按清单自动落账，使相应应收未付额与押金余额等额减少并可对账」）：
     * 锁押金余额 → 按 arApItemIds 顺序逐笔经统一回写点 settleArApItem 以「押金抵扣」（payment_method=deposit_offset、bank_flow_ref＝结算单 id）销账，
     * 合计不超过 offsetCent 与锁定余额 → 写一条 deposit_ledger 抵扣（deduct）行减余额。返回逐笔销账（结算清单押金处置项回写对账）。
     */
    List<DepositOffset> offsetReceivablesWithDeposit(String merchantId, String settlementId, List<String> arApItemIds,
            long offsetCent);

    /**
     * HP-C03（BSM L1353「批准生效后登记应收或应退，实际收款或退款成功后才更新已收押金余额，未结差额继续跟进」）：
     * 面积/位置变更子合同押金补退差额登记进应收应付台账——deltaCent&gt;0 挂应收（商户补缴）、&lt;0 挂应付（退还），金额取绝对值，
     * 到期日＝办理期限，source_type='deposit_adjustment'、来源＝子合同；幂等键＝变更 id（重放不双挂）。登记时不写 deposit_ledger。
     */
    ArApItemRecord registerDepositAdjustment(String merchantId, String childContractId, String changeId,
            long deltaCent, LocalDate deadline);
}
