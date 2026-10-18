package com.complex.platform.contract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 合同域仓储（V162 重建）。 */
public interface ContractRepository {

    // 合同
    ContractRecord saveContract(ContractRecord contract);

    List<ContractRecord> listContracts();

    // AUD-005 真分页: 默认内存切片(测试双子); Jdbc 覆写 SQL COUNT + LIMIT/OFFSET。
    default com.complex.platform.common.api.PageResponse<ContractRecord> listContractsPaged(int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(listContracts(), page, size);
    }

    Optional<ContractRecord> findContractById(String id);

    /** S-2 确认退押金落账时对合同行加锁，串行化同一合同的并发确认（防违约金侧无封顶双挂）。 */
    Optional<ContractRecord> findContractByIdForUpdate(String id);

    /** #31 变更子合同：rent_terms.parentContractId 指向该父合同的子合同。 */
    List<ContractRecord> listChildContracts(String parentContractId);

    // 审批（分组并行链实例）
    ContractApprovalRecord saveApproval(ContractApprovalRecord approval);

    /** 新签合同审批（change_id IS NULL）。 */
    List<ContractApprovalRecord> listApprovalsByContract(String contractId);

    void clearApprovals(String contractId);

    /** 某合同变更的审批（同一套并行链）。 */
    List<ContractApprovalRecord> listApprovalsByChange(String changeId);

    void clearApprovalsByChange(String changeId);

    // 变更
    ContractChangeRecord saveChange(ContractChangeRecord change);

    Optional<ContractChangeRecord> findChangeById(String id);

    List<ContractChangeRecord> listChangesByContract(String contractId);

    // 退租结算
    ContractSettlementRecord saveSettlement(ContractSettlementRecord settlement);

    Optional<ContractSettlementRecord> findSettlementById(String id);

    List<ContractSettlementRecord> listSettlementsByContract(String contractId);

    // ---- 商户专区一期(3.18)商户维度读取:default 由既有方法组合,Jdbc 实现覆写为单 SQL ----

    default List<ContractRecord> listContractsByMerchant(String merchantId) {
        return listContracts().stream()
                .filter(contract -> merchantId != null && merchantId.equals(contract.merchantId()))
                .toList();
    }

    default List<ContractSettlementRecord> listSettlementsByMerchant(String merchantId) {
        return listContractsByMerchant(merchantId).stream()
                .flatMap(contract -> listSettlementsByContract(contract.id()).stream())
                .toList();
    }

    // AUD-005 收尾: 平台全量结算真分页(默认内存切片供测试双子; Jdbc 覆写 SQL COUNT + LIMIT/OFFSET)。
    default com.complex.platform.common.api.PageResponse<ContractSettlementRecord> listAllSettlementsPaged(int page, int size) {
        List<ContractSettlementRecord> all = listContracts().stream()
                .flatMap(contract -> listSettlementsByContract(contract.id()).stream())
                .toList();
        return com.complex.platform.common.api.PageResponse.of(all, page, size);
    }

    // 可配审批链：引擎读「该综合体生效链」（项目覆盖优先、否则平台默认）
    List<ContractApprovalFlowStepRecord> listActiveFlowSteps(String projectId);

    List<ContractApprovalFlowStepRecord> listFlowStepsByProject(String projectId);

    void saveFlowConfig(String projectId, String name, List<ContractApprovalFlowStepRecord> steps);

    // 可配到期着色
    SpaceExpiryColorRuleRecord saveColorRule(SpaceExpiryColorRuleRecord rule);

    List<SpaceExpiryColorRuleRecord> listColorRules(String projectId);

    // 生效绑定空间 + 防重复出租
    void bindContractSpaces(String contractId, List<String> spaceIds, String projectId);

    /** 3.13：读合同已绑定（含草稿预绑定）空间 id，供提交审批阶段冲突早检。 */
    List<String> listContractSpaceIds(String contractId);

    boolean hasOverlappingEffectiveContractForSpace(
            String spaceId, LocalDate effectiveDate, LocalDate expiryDate, String excludeContractId);

    /** 到期转换：过 expiry_date 仍 effective 的合同 → expired，返回条数（替代旧 governance 到期 Job 路径）。 */
    int markContractsExpired(LocalDate asOf);

    /** 续约提醒扫描：effective 且 expiry_date 落在 (fromExclusive, toInclusive] 的合同。 */
    List<ContractRecord> listEffectiveContractsExpiringBetween(LocalDate fromExclusive, LocalDate toInclusive);

    /**
     * 铺位租赁状态原始数据（每铺最近一份合同 + 退租结算单状态 + 距到期天数）；着色由 service 套用着色分类。
     * CG-09：推导口径见 {@link SpaceRentalDerivationSql}（退租结算完成即转空置，结算单开立起已退租）。
     */
    List<SpaceRentalStatusRecord> listSpaceRentalRaw(String projectId, LocalDate asOf);

    /** 该 (合同,期,费目) 是否已挂账（幂等依据）。 */
    boolean rentBillExists(String contractId, String period, String feeType);

    /**
     * 逐期挂账：联动建 ar_ap_items 应收(direction=receivable/status=pending_collection/source=contract_rent)
     * + ar_ap_item_sources + contract_rent_bills。原子。
     */
    ContractRentBillRecord insertRentBill(ContractRentBillRecord bill, LocalDate dueDate);

    /** 该合同未付应收合计（退租欠费追缴依据）：ar_ap_items 中 source_id 以 contractId 起头的 receivable 未付额。 */
    long sumUnpaidArForContract(String contractId);
}
