package com.complex.platform.contract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 合同域仓储（V162 重建）。 */
public interface ContractRepository {

    // 合同
    ContractRecord saveContract(ContractRecord contract);

    List<ContractRecord> listContracts();

    Optional<ContractRecord> findContractById(String id);

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

    // 可配审批链：引擎读「该综合体生效链」（项目覆盖优先、否则平台默认）
    List<ContractApprovalFlowStepRecord> listActiveFlowSteps(String projectId);

    List<ContractApprovalFlowStepRecord> listFlowStepsByProject(String projectId);

    void saveFlowConfig(String projectId, String name, List<ContractApprovalFlowStepRecord> steps);

    // 可配到期着色
    SpaceExpiryColorRuleRecord saveColorRule(SpaceExpiryColorRuleRecord rule);

    List<SpaceExpiryColorRuleRecord> listColorRules(String projectId);

    // 生效绑定空间 + 防重复出租
    void bindContractSpaces(String contractId, List<String> spaceIds, String projectId);

    boolean hasOverlappingEffectiveContractForSpace(
            String spaceId, LocalDate effectiveDate, LocalDate expiryDate, String excludeContractId);

    /** 到期转换：过 expiry_date 仍 effective 的合同 → expired，返回条数（替代旧 governance 到期 Job 路径）。 */
    int markContractsExpired(LocalDate asOf);

    /** 铺位租赁状态原始数据（每铺当前生效合同 + 距到期天数）；着色由 service 套用着色分类。 */
    List<SpaceRentalStatusRecord> listSpaceRentalRaw(String projectId, LocalDate asOf);

    /** 该 (合同,期) 是否已挂账（幂等依据）。 */
    boolean rentBillExists(String contractId, String period);

    /**
     * 逐期挂账：联动建 ar_ap_items 应收(direction=receivable/status=pending_collection/source=contract_rent)
     * + ar_ap_item_sources + contract_rent_bills。原子。
     */
    ContractRentBillRecord insertRentBill(ContractRentBillRecord bill, LocalDate dueDate);
}
