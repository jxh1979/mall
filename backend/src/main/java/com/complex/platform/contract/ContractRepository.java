package com.complex.platform.contract;

// 文件版本：V0.03

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 合同域仓储（V162 重建）。 */
public interface ContractRepository {

    // 合同
    ContractRecord saveContract(ContractRecord contract);

    List<ContractRecord> listContracts();

    // AUD-005 真分页: 默认内存切片(测试双子); Jdbc 覆写 SQL COUNT + LIMIT/OFFSET。
    default com.complex.platform.common.api.PageResponse<ContractRecord> listContractsPaged(int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(listContracts(), page, size);
    }

    /** 第十八批 G 段 82b-R3：只列给定项目的合同（空集合＝空页）。默认内存过滤供测试双子；Jdbc 覆写 SQL。 */
    default com.complex.platform.common.api.PageResponse<ContractRecord> listContractsPaged(int page, int size,
            Collection<String> projectIds) {
        return com.complex.platform.common.api.PageResponse.of(listContracts().stream()
                .filter(c -> c.projectId() != null && projectIds.contains(c.projectId())).toList(), page, size);
    }

    Optional<ContractRecord> findContractById(String id);

    /** S-2 确认退押金落账时对合同行加锁，串行化同一合同的并发确认（防违约金侧无封顶双挂）。 */
    Optional<ContractRecord> findContractByIdForUpdate(String id);

    /** #31 变更子合同：rent_terms.parentContractId 指向该父合同的子合同。 */
    List<ContractRecord> listChildContracts(String parentContractId);


    // HP-C01：审批实例改走统一审批模型（supervisor_review_nodes），contracts/contract_changes.review_id 挂复核单号。
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

    // 审批流程模板（V494，HP-C01）：按 (综合体, 合同类型) 取生效模板（项目覆盖优先、否则平台默认）
    Optional<ContractApprovalTemplateRecord> findActiveTemplate(String projectId, String contractType);

    /** 该综合体三类各自的生效模板（项目覆盖优先、否则平台默认）。 */
    List<ContractApprovalTemplateRecord> listTemplates(String projectId);

    /** 按 (projectId, contractType) 落为该组合唯一的生效模板（已有则改写）。 */
    ContractApprovalTemplateRecord saveTemplate(ContractApprovalTemplateRecord template);

    /** 模板校验：审批线在 approval_lines 且 active。 */
    boolean approvalLineActive(String lineCode);

    /** 模板校验：用户存在且 active。 */
    boolean userActive(String userId);

    /** HP-C03（BSM L1347）押金收取月数（全平台统一）：读 'global' 行（出厂值 3、上下限未配置为 null）；无行 fail-closed。 */
    DepositMonthsSettingRecord depositMonthsSetting();

    /** HP-C03 写押金收取月数（值与上下限同时落库）。 */
    void saveDepositMonthsSetting(DepositMonthsSettingRecord setting, String actorId);

    /** HP-C06（BSM L1352）押金退还等待期（按项目）：项目行 → '__global__' 出厂回落行（30 日、上下限 null）；均无 fail-closed。 */
    DepositRefundWaitSettingRecord depositRefundWaitSetting(String projectId);

    /** HP-C06 写项目押金退还等待期（日数与上下限同时落库）。 */
    void saveDepositRefundWaitSetting(DepositRefundWaitSettingRecord setting, String actorId);

    // 可配到期着色
    SpaceExpiryColorRuleRecord saveColorRule(SpaceExpiryColorRuleRecord rule);

    List<SpaceExpiryColorRuleRecord> listColorRules(String projectId);

    // 生效绑定空间 + 防重复出租
    void bindContractSpaces(String contractId, List<String> spaceIds, String projectId);

    /** 18F 第 80 条：铺位所在项目（合同未带项目时生效取所绑铺位的项目）。default 返回 null 供内存替身。 */
    default String findSpaceProjectId(String spaceId) {
        return null;
    }

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

    /** 第十八批 G 段 82b-R3：给定多个项目的铺位租赁状态（空集合＝空表）。 */
    default List<SpaceRentalStatusRecord> listSpaceRentalRaw(Collection<String> projectIds, LocalDate asOf) {
        // ponytail: 逐项目各查一次单项目口径；可触及项目多到成为负担时改成 WHERE s.project_id IN (:ids) 一次取
        return projectIds.stream().sorted().flatMap(p -> listSpaceRentalRaw(p, asOf).stream()).toList();
    }

    /** 该 (合同,期,费目) 是否已挂账（幂等依据）；已作废（V501 voided_at 非空）的账单不算已挂账。 */
    boolean rentBillExists(String contractId, String period, String feeType);

    /**
     * 第四轮裁决第 2 条「变更生效时重排」：作废 contractIds 上账期不早于 fromPeriod、费目在 feeTypes 内、整笔未收（台账行已收额为 0）的预排账单——
     * 应收台账沿用 V344 红冲追加严格取反的冲正行（原行不动），账单记作废时刻与原因；已部分收款的不动。返回被作废的账单 id。
     * 第五轮：租金调整终审同样调用（只作废租金）；日结锁定的原行照常作废，冲正行不继承锁、业务日期取作废当日（金额归入当期、不改原日结金额），
     * 替代它的重排应收同记作废当日（见 {@link #insertRentBill}）。
     */
    List<String> voidUnpaidRentBills(List<String> contractIds, String fromPeriod, List<String> feeTypes, String reason);

    /**
     * 逐期挂账：联动建 ar_ap_items 应收(direction=receivable/status=pending_collection/source=contract_rent)
     * + ar_ap_item_sources + contract_rent_bills。原子。
     * 重排替代变更链上同期同费目已作废的账单时，到期日取该作废账单冲正行的到期日（锁定原行＝作废当日、未锁＝原到期日），与冲正行同日相抵；否则用 dueDate。
     */
    ContractRentBillRecord insertRentBill(ContractRentBillRecord bill, LocalDate dueDate);

    /**
     * 该合同未付应收合计（退租欠费追缴依据）：ar_ap_items 中 source_id 以 contractId 起头的 receivable 未付额。
     * HP-C02-R6：through 非空只计账期（accounting_period／能源实际费用 period）不晚于该截止月的应收（裁决第 2 条「欠费口径都按这个截止月」）；
     * null＝退租日未定（手动开账、尚未发起退租变更）不截止。
     */
    long sumUnpaidArForContract(String contractId, YearMonth through);

    /**
     * 该退租结算单挂出的提前终止违约金应收未收额（ar_ap_items：direction=receivable、source_type='termination_penalty'、source_id＝结算单 id，
     * 含补录调整的冲正／重述行）。违约金并入押金抵扣（BSM L1347「只有清退时押金才能抵扣欠费与违约金」），欠费追缴中的收齐判定计入此额。
     */
    long sumUnpaidTerminationPenalty(String settlementId);

    /**
     * 第四轮裁决第 3 条（押金抵扣逐笔销账）：与 {@link #sumUnpaidArForContract} 同口径的未清应收逐笔 id（未收额&gt;0），先到期先抵（到期日、id 升序）；
     * 已被补录红冲的原行不列（其未清额由冲正行抵消、重述行承接）。
     */
    List<String> listUnpaidArIdsForContract(String contractId, YearMonth through);

    /**
     * 变更子合同链 id 集：[自身, 父, 祖父...]（沿 rent_terms.parentContractId 上溯，带环守卫）。账单查重/未清汇总/齐单核对/作废重排/押金抵扣共用。
     * 只沿同商户父合同上溯（合法子合同由变更终审复制父合同商户）：指向别的商户合同的 parentContractId 不入链，沿链写路径不跨商户。
     */
    // ponytail: 每次调用现走链(findContractById 逐代)——子合同罕见且链短(通常≤2)，root 合同 0 额外查询；
    //   若链变深或计费成热点，缓存链 id 或改 recursive CTE。
    default List<String> contractChainIds(ContractRecord c) {
        List<String> ids = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        ContractRecord cur = c;
        while (cur != null && seen.add(cur.id())) {
            ids.add(cur.id());
            Object parentId = cur.rentTerms() == null ? null : cur.rentTerms().get("parentContractId");
            String merchantId = cur.merchantId();
            cur = parentId == null ? null : findContractById(String.valueOf(parentId))
                    .filter(p -> java.util.Objects.equals(p.merchantId(), merchantId)).orElse(null);
        }
        return ids;
    }

    /**
     * HP-C02（BSM L1352「退租对应期间的全部相关单据收齐并核对完成」）：生效月→through 逐月核「相关单据」，返回缺项「yyyy-MM:fee_type」。
     * 相关单据按 ENUM L256 租金/账单类型取合同期内按月出账的三类：rent（ContractRentMath 当期应收&gt;0 而无账单）、
     * property_fee（propertyFeeCent&gt;0 而无账单）、energy_fee（energy_fee_basis=by_contract 且商户有计量表时，该月每只表的实际费用须已登记并已出账）。
     * 账单查重沿变更子合同链（与 rentBillExists 同口径）。违约金由结算自身生成、押金抵扣按台账余额算，不属「收齐」。
     */
    List<String> listMissingTerminationDocuments(ContractRecord contract, YearMonth through);
}
