package com.complex.platform.finance;

// 文件版本：V0.01

/**
 * FS-017 存酒退出承接垫付追收回写端口（inventory 域实现，finance 经 @Lazy 反向依赖调用，与
 * {@link TerminationSettlementProgressPort} 同范式）。BSM L1072「该娱乐平台公司可经审批先垫付并向原门店追收……
 * 对客实际交付结案与内部追收状态分别记录」：source_type=saved_bottle_remedy_recovery 的应收经统一回写点
 * {@code FinanceService.settleArApItem} 结清时，同一事务内回调把补存/赔付记录的追收状态置 recovered。
 */
public interface SavedBottleRemedyRecoveryPort {

    /** item＝已回写 paid/unpaid/status=settled 的追收应收行。 */
    void onRecoverySettled(ArApItemRecord item);
}
