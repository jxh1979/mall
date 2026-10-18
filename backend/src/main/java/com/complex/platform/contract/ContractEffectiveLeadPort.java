package com.complex.platform.contract;

/**
 * 合同生效 → 来源招商线索置「已签约」端口（leasing 域实现，合同域经 @Lazy 反向依赖调用，避免初始化环；
 * 与 {@code TerminationSettlementPort} 同范式）。
 *
 * <p>业务口径（platform-governance-map「招商运营细则」）：「合同审批全部通过、生效后转为已签约，已签约后不得回退至未签约状态」。
 * 签约起草（{@code LeasingService.signIntent}）只起草合同草稿，线索状态不动；
 * 只有 {@link ContractService#activate} 成功后才经本端口把来源线索置已签约。
 */
public interface ContractEffectiveLeadPort {

    /**
     * 合同生效后调用（在生效事务内）：按合同 id 反查签约起草时绑定的来源意向 → 线索，CAS 置 {@code signed} 并留痕；
     * 合同不是招商签约起草的（无绑定意向）或线索已是已签约时无事发生（幂等）。
     */
    void markSourceLeadSigned(String contractId, String idempotencyKey);
}
