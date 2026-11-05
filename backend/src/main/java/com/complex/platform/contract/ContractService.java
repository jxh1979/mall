package com.complex.platform.contract;

// 文件版本：V0.29

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractApprovalFlowRequest;
import com.complex.platform.contract.dto.ContractAttachmentRequest;
import com.complex.platform.contract.dto.ContractChangeRequest;
import com.complex.platform.contract.dto.ContractDraftRentTermsRequest;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.contract.dto.ContractRentBillRequest;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractScopeRequestRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.contract.dto.ContractSubmitRequest;
import com.complex.platform.contract.dto.DepositRefundWaitSettingRequest;
import com.complex.platform.contract.dto.SpaceExpiryColorRuleRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import com.complex.platform.governance.SpaceRecord;
import com.complex.platform.governance.chain.ApprovalChainNode;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.merchantbilling.MerchantFeeNotificationRecipientResolver;
import com.complex.platform.notification.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.complex.platform.common.Strings.trimToNull;
import com.complex.platform.common.Strings;
import static com.complex.platform.common.Strings.requireText;

/**
 * 合同租赁全生命周期（V162 重建，据 contract-lifecycle-prd-v1.0 V1.1 + M1 §2 + 用户裁决）。
 * 核心：起草 → 提交（HP-C01：按新建／到期终止／变更三类模板开统一审批复核单，指定主线最终审批人＋平行部门线，逐级自动补全，
 * 全部通过才生效、任一驳回退回起草）→ 生效固化快照 + 绑铺位 → 变更（6类）→ 退租逐例结算 + 争议挂起。
 */
@Service
public class ContractService implements com.complex.platform.finance.TerminationSettlementProgressPort {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private static final String MERCHANT_FEE_DOMAIN = "merchant_fee";
    private static final String MERCHANT_FEE_RENEWAL_EVENT = "renewal";
    private static final String MERCHANT_FEE_RENEWAL_TEMPLATE = "sms.merchant_fee.renewal";
    private static final Set<String> COMMISSION_MODES = Set.of(
            "guarantee_or_commission_higher", "fixed_ratio", "tiered_ratio", "guarantee_plus_commission");
    private static final Set<String> ENERGY_BASES = Set.of("none", "by_contract");
    private static final Set<String> CHANGE_TYPES = Set.of(
            "renewal", "move_out", "area_change", "rent_change", "business_scope_change", "early_termination",
            "scope_grant", "location_change");
    /** 变更子合同触发类型（#31）：仅经营位置/面积变更基于原合同开有继承关系的子合同。 */
    private static final Set<String> AMENDMENT_TYPES = Set.of("area_change", "location_change");
    private static final Set<String> GRANT_TYPES = Set.of("permanent", "temporary");
    /** 变更子合同控制键（不当作子合同 rent_terms 条款回填）；HP-C03：押金补退办理期限写进 depositAdjustment.deadline。 */
    private static final Set<String> AMENDMENT_CONTROL_KEYS = Set.of("spaceIds", "depositDeadline", "reason");
    private static final Set<String> SETTLE_TRIGGERS = Set.of("move_out", "expiry", "early_termination");
    /** HP-C02-R6：须填退租日的变更类型与载荷键（2026-10-18 裁决第 2 条「发起退租/提前终止变更时必填「退租日」」）。 */
    private static final Set<String> TERMINATION_CHANGE_TYPES = Set.of("move_out", "early_termination");
    /**
     * 变更链等系统维护的 rent_terms 键：由面积/位置变更、续租终审与超范围经营许可写入，页面从不发送。草稿编辑、租金调整与经营范围变更载荷一律拒收，
     * 否则 putAll 进 rent_terms 后变更作废重排与退租押金抵扣沿链会触及链上别的合同台账（contractChainIds 沿 parentContractId 上溯）。
     */
    private static final Set<String> CHAIN_RESERVED_KEYS = Set.of("parentContractId", "amendmentType", "increaseFrom", "depositAdjustment",
            "scopeGrants");
    private static final String MOVE_OUT_DATE_KEY = "moveOutDate";
    /** 138-R5 复核修正：保底取高、叠加合同变更或续租终审前的月租条款段（{throughPeriod, ContractRentMath 读的键}），见 withPriorRent。 */
    private static final String PRIOR_RENT_KEY = "priorRentTerms";
    private static final List<String> RENT_MATH_KEYS = List.of("baseRentCent", "freeRentMonths", "increase", "increaseFrom");
    /**
     * 落账前（在办）态＝待结算＋争议挂起两态：同一合同同一时刻只有一张在办退租结算单（BSM L1352「同一份合同的退租结算只算一次、不重复出账」）；
     * 只有落账前的单可被驳回作废、可终审落账。第三轮控制席约定：落账后的「押金抵扣中」「欠费追缴中」是已落账状态，不在此列。
     */
    private static final Set<String> LIVE_SETTLEMENT_STATUSES = Set.of("pending_settlement", "disputed", "disputed_litigation");
    /** 已落账态（第三轮裁决第 4 条「按押金够不够分流」三态）：同合同另一张单终审即作废，不重复落账。 */
    private static final Set<String> POSTED_SETTLEMENT_STATUSES = Set.of("deposit_offsetting", "arrears_collecting", "settled");
    /**
     * HP-C04 争议挂起两态（BSM L1352「有争议进「有争议」状态挂起、由主管裁决后继续 ，如主管裁决不接受，则进「有争议，诉讼/仲裁」状态无限期挂起」；
     * 2026-10-18 裁决第 5 条「最后审批不得通过」）：退租变更与到期终止复核单的最后审批一律 409，待主管裁决／录入诉讼仲裁结果回待结算。
     */
    private static final Set<String> DISPUTED_SETTLEMENT_STATUSES = Set.of("disputed", "disputed_litigation");
    /** HP-C06 押金退还等待期配置权限（V499）。 */
    private static final String DEPOSIT_REFUND_WAIT_MANAGE = "contract.deposit_refund_wait.manage";
    /** HP-C06 顺延原因（BSM L1352「算账晚于等待期完成的顺延至算账完成之日支付，顺延原因与预计时间在商户专区对商户可见」）。 */
    private static final String DEPOSIT_REFUND_DEFERRAL_REASON = "退租结算算账晚于等待期完成，顺延至算账完成之日支付";

    private final ContractRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final PlatformRepository platformRepository;
    private final MerchantFeeNotificationRecipientResolver merchantFeeRecipientResolver;
    // 批2b:提成端口(finance 实现)。构造注入(@Lazy 防潜在初始化环);旧构造传 null=收租方式退化纯月租。
    private final com.complex.platform.finance.FinanceService turnoverRentPort;
    // 退租结算落财务台账端口(finance 实现)。构造器 @Lazy 注入(防初始化环,与 turnoverRentPort 同范式;
    // 字段注入曾被判死刑=运行时可能注入 null 致静默跳过落账);旧构造传 null=降级只留结算快照。
    private final com.complex.platform.finance.TerminationSettlementPort terminationSettlementPort;
    // CG-01：合同生效→来源招商线索置已签约端口（leasing 实现）。构造器 @Lazy 注入防初始化环（LeasingService→ContractService→本端口）；
    // 旧构造传 null=不是招商签约起草的合同场景（单测）不触发。
    private final ContractEffectiveLeadPort effectiveLeadPort;
    // HP-C01（BSM L1349 / PGM L528、L536）：合同三类审批接入全平台统一审批模型（SupervisorReviewService 复核单 + 审批链节点）。
    // 构造器 @Lazy 注入防初始化环（与 terminationSettlementPort 同范式）；旧构造传 null=发起时 fail-closed
    // （CONTRACT_APPROVAL_UNAVAILABLE），不得静默跳过审批。
    private final SupervisorReviewService reviews;
    // 押金收取月数／押金退还等待期配置写口经幂等服务执行（照 CG-08b configurePolicy 范式）；旧构造传 null（不调这两个写口的单测）。
    private final IdempotencyService idempotencyService;

    @Autowired
    public ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository,
            MerchantFeeNotificationRecipientResolver merchantFeeRecipientResolver,
            @org.springframework.context.annotation.Lazy com.complex.platform.finance.FinanceService turnoverRentPort,
            @org.springframework.context.annotation.Lazy com.complex.platform.finance.TerminationSettlementPort terminationSettlementPort,
            @org.springframework.context.annotation.Lazy ContractEffectiveLeadPort effectiveLeadPort,
            @org.springframework.context.annotation.Lazy SupervisorReviewService reviews,
            IdempotencyService idempotencyService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.platformRepository = platformRepository;
        this.merchantFeeRecipientResolver = merchantFeeRecipientResolver;
        this.turnoverRentPort = turnoverRentPort;
        this.terminationSettlementPort = terminationSettlementPort;
        this.effectiveLeadPort = effectiveLeadPort;
        this.reviews = reviews;
        this.idempotencyService = idempotencyService;
    }

    /** 兼容构造（HP-C01 的十参形）：不注入幂等服务。 */
    public ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository,
            MerchantFeeNotificationRecipientResolver merchantFeeRecipientResolver,
            com.complex.platform.finance.FinanceService turnoverRentPort,
            com.complex.platform.finance.TerminationSettlementPort terminationSettlementPort,
            ContractEffectiveLeadPort effectiveLeadPort, SupervisorReviewService reviews) {
        this(repository, auditEventService, authorizationService, notificationService, platformRepository,
                merchantFeeRecipientResolver, turnoverRentPort, terminationSettlementPort, effectiveLeadPort, reviews, null);
    }

    /** 兼容构造（HP-C01 前的九参形）：统一审批服务为空=发起审批 fail-closed。 */
    public ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository,
            MerchantFeeNotificationRecipientResolver merchantFeeRecipientResolver,
            com.complex.platform.finance.FinanceService turnoverRentPort,
            com.complex.platform.finance.TerminationSettlementPort terminationSettlementPort,
            ContractEffectiveLeadPort effectiveLeadPort) {
        this(repository, auditEventService, authorizationService, notificationService, platformRepository,
                merchantFeeRecipientResolver, turnoverRentPort, terminationSettlementPort, effectiveLeadPort, null);
    }

    public ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository,
            MerchantFeeNotificationRecipientResolver merchantFeeRecipientResolver) {
        this(repository, auditEventService, authorizationService, notificationService, platformRepository, merchantFeeRecipientResolver, null, null, null, null);
    }

    public ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository) {
        this(repository, auditEventService, authorizationService, notificationService, platformRepository, null, null, null, null, null);
    }

    // 单测便捷构造：仅注入退租结算端口（其余 finance 端口/依赖 null，统一审批服务 null=发起审批 fail-closed）。
    ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository,
            com.complex.platform.finance.TerminationSettlementPort terminationSettlementPort) {
        this(repository, auditEventService, authorizationService, notificationService, platformRepository,
                null, null, terminationSettlementPort, null, null);
    }

    // 单测便捷构造：退租结算端口 + 统一审批服务（HP-C01）。
    ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository,
            com.complex.platform.finance.TerminationSettlementPort terminationSettlementPort,
            SupervisorReviewService reviews) {
        this(repository, auditEventService, authorizationService, notificationService, platformRepository,
                null, null, terminationSettlementPort, null, reviews);
    }

    // 单测便捷构造：仅注入合同生效→线索已签约端口。
    ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, ContractEffectiveLeadPort effectiveLeadPort) {
        this(repository, auditEventService, authorizationService, null, null, null, null, null, effectiveLeadPort, null);
    }

    // 单测便捷构造：合同生效→线索已签约端口 + 统一审批服务（HP-C01）。
    ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, ContractEffectiveLeadPort effectiveLeadPort,
            SupervisorReviewService reviews) {
        this(repository, auditEventService, authorizationService, null, null, null, null, null, effectiveLeadPort, reviews);
    }

    // ============ 起草 ============
    public PageResponse<ContractRecord> listContracts() {
        return listContracts(1, 500);
    }

    // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, total 取真值; 500 默认保留既有前端一次拉取契约。
    // 第十八批 G 段 82b-R3（PGM 第 659 行「普通岗位没有跨项目查看权」「逐个勾选的只能查看被选中的项目」）：平台管理员照旧全量，
    // 其余只列 contract.draft.manage 可触及项目的合同（total 同口径）。
    public PageResponse<ContractRecord> listContracts(int page, int size) {
        var actor = authorizationService.currentActor();
        if (authorizationService.isPlatformAdmin(actor)) {
            return repository.listContractsPaged(page, size);
        }
        return repository.listContractsPaged(page, size,
                authorizationService.projectReach(actor, "contract.draft.manage").organizationIds());
    }

    /** 手工起草合同，状态 drafting；落地项目按 contract.draft.manage 核。 */
    @Transactional
    public ContractRecord createDraft(ContractDraftRequest request) {
        return createDraft(request, "contract.draft.manage");
    }

    /**
     * 起草合同（手工或招商签约带入），状态 drafting。也是招商 signIntent 接线入口：落地项目按 reachPermission 核——招商签约传
     * leasing.convert（签约是这一个动作，入口已按它核过意向项目，不另要 contract.draft.manage；PGM:659「在所分配的权限内可以查看、
     * 处理该角色可查看项目的数据」）。
     */
    // AUD-002①：公有入口开事务（原 @Transactional 误标在私有自调用方法上=Spring 代理不生效，事务从未开）。
    @Transactional
    public ContractRecord createDraft(ContractDraftRequest request, String reachPermission) {
        authorizationService.requirePlatformScope();
        ContractDraftRequest actual = requireRequest(request);
        // AUD-002③：字段级业务校验前置——先于任何写入、且先于幂等短路，确保幂等命中(重放)也必过校验、不被绕过。
        String commissionMode = trimToNull(actual.commissionMode());
        if (commissionMode != null && !COMMISSION_MODES.contains(commissionMode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "commission_mode is invalid");
        }
        String energyBasis = trimToNull(actual.energyFeeBasis()) == null ? "none" : actual.energyFeeBasis().trim();
        if (!ENERGY_BASES.contains(energyBasis)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "energy_fee_basis is invalid");
        }
        // 公区水电损已包含物业费；独表仅凭能源域核实的实际账单代收。
        rejectFixedEnergyCharge(actual.rentTerms());
        Map<String, Object> inTerms = camelKeys(actual.rentTerms());
        // 变更链键只由变更/续租终审写入：起草带入即拒收（同草稿编辑口径），否则伪造的链会让作废重排与退租押金抵扣触及别的合同台账。
        if (!java.util.Collections.disjoint(inTerms.keySet(), Set.of("parentContractId", "amendmentType", "depositAdjustment"))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "租金条款不得带变更链等系统维护项（parent_contract_id／amendment_type／deposit_adjustment）");
        }
        Long draftDeposit = draftDeposit(inTerms, actual.depositCent());
        String projectId = draftProject(actual, reachPermission);
        // 1.5 幂等：带 idempotencyKey 时合同 id 由其确定性派生；重复提交/招商重签命中既有草稿直接返回，不重复建。
        // 校验已前置于此，故重放亦已过校验；失败草稿在 @Transactional 下整体回滚，重试将重走完整校验，无半成品可复用。
        String idempotencyKey = trimToNull(actual.idempotencyKey());
        String contractId = idempotencyKey != null
                ? "ctr_" + deterministicSuffix(idempotencyKey)
                : "ctr_" + UUID.randomUUID();
        if (idempotencyKey != null) {
            ContractRecord existing = repository.findContractById(contractId).orElse(null);
            if (existing != null) {
                return existing;
            }
        }
        Instant now = Instant.now();
        Map<String, Object> rentTerms = new HashMap<>(inTerms);
        if (trimToNull(actual.businessScope()) != null) {
            rentTerms.put("businessScope", actual.businessScope().trim());   // 经营范围文字描述存 rent_terms
        }
        // I-1: 堵校验绕过——客户端可不走专用字段、直接把 allowedCategories(或蛇形键)塞进 rent_terms 整 Map；
        // 词表外值入库会让 guard 误拦全品类（错配 DoS），故内嵌键同样归一化+词表校验。
        Object embedded = rentTerms.remove("allowed_categories");
        if (embedded == null) {
            embedded = rentTerms.get(ContractScopeGuard.RENT_TERMS_KEY);
        }
        List<String> embeddedCategories = ContractScopeGuard.validateAllowedCategories(embedded);
        if (embeddedCategories != null) {
            rentTerms.put(ContractScopeGuard.RENT_TERMS_KEY, embeddedCategories);
        }
        // 经营范围结构化（2026-07-18 拍板）：取值校验限既有业务类目；存 rent_terms.allowedCategories，
        // 下单侧（bar/票务）读此键拦超范围经营；空/缺省=不拦截（存量合同兼容）。专用字段优先于内嵌键。
        List<String> allowedCategories = ContractScopeGuard.validateAllowedCategories(actual.allowedCategories());
        if (allowedCategories != null && !allowedCategories.isEmpty()) {
            rentTerms.put(ContractScopeGuard.RENT_TERMS_KEY, allowedCategories);
        }
        // AUD-002②：空间存在性 + 与生效合同租期重叠 —— 落库前校验（早检不写库，校验失败零脏写）。
        List<String> draftSpaces = validateDraftSpaces(
                actual.effectiveDate(), actual.expiryDate(), contractId, actual.spaceIds());
        ContractRecord saved = repository.saveContract(new ContractRecord(
                contractId,
                requireText(actual.merchantId(), "merchant_id"),
                requireText(actual.contractSubjectId(), "contract_subject_id"),
                trimToNull(actual.operationSubjectId()),
                requireText(actual.settlementSubjectId(), "settlement_subject_id"),
                requireDate(actual.effectiveDate(), "effective_date"),
                requireDate(actual.expiryDate(), "expiry_date"),
                rentTerms,
                commissionMode, energyBasis, draftDeposit,
                actual.penaltyTerms() == null ? Map.of() : Map.copyOf(actual.penaltyTerms()),
                actual.settlementRules() == null ? List.of() : List.copyOf(actual.settlementRules()),
                null, null,
                actual.attachmentIds() == null ? List.of() : List.copyOf(actual.attachmentIds()),
                "drafting", projectId, null, null, null, now, now));
        auditEventService.recordSuccess("contract.drafted", "contract.draft.manage", "contract", saved.id(),
                null, actual.idempotencyKey(), Map.of("merchant_id", saved.merchantId()));
        // 3.13 预绑定意向空间（存在性/冲突已在 validateDraftSpaces 落库前校验）。
        if (!draftSpaces.isEmpty()) {
            repository.bindContractSpaces(saved.id(), draftSpaces, saved.projectId());
        }
        return saved;
    }

    /**
     * 第十八批 G 段 82b-R3（PGM 第 659 行「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」；第 413 行「所有基础对象都要带“综合体项目归属”」）：
     * 起草落地项目＝请求项目 → 第一个意向铺位所在项目 → 本人项目，须是项目且在入口码可触及范围内（照 LeasingService.landingProject；
     * 复核：城市项目公司员工的本人归属是公司编号、不是项目，不带项目时 403）；手工起草按 contract.draft.manage，招商签约 signIntent
     * 带线索所在项目、按 leasing.convert。平台管理员不带项目也没带铺位时照旧为空。
     */
    private String draftProject(ContractDraftRequest actual, String reachPermission) {
        var actor = authorizationService.currentActor();
        boolean admin = authorizationService.isPlatformAdmin(actor);
        String space = actual.spaceIds() == null || actual.spaceIds().isEmpty() ? null : trimToNull(actual.spaceIds().get(0));
        String fallback = space == null ? null : repository.findSpaceProjectId(space);
        String landing = trimToNull(actual.projectId()) != null ? trimToNull(actual.projectId()) : fallback != null ? fallback
                : admin ? null : actor.organizationId();
        if (landing == null && admin) {
            return null;
        }
        if (landing == null || !(admin || (platformRepository.projectExists(landing)
                && authorizationService.canReachProject(actor, landing, reachPermission)))) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", AuthorizationService.PROJECT_OUT_OF_REACH);
        }
        return landing;
    }

    /**
     * 第四轮裁决第 4 条「合同草稿可编辑补录」（「合同工作台增加草稿编辑，签约起草后在草稿上补录租金条款再提交」）：只开放租金条款与纯提成约定押金——
     * 请求 rent_terms 按起草同口径蛇形→驼峰归一后并入草稿 rent_terms（经营范围键不在此改）；押金按起草同口径：有月租的按「收取月数×月租」重算
     * （BSM L1347），纯提成录约定押金且大于 0（第三轮裁决第 1 条）。仅草拟中（含驳回退回起草）可编辑，否则 409 CONTRACT_NOT_DRAFTING。
     * 权限沿用起草合同：contract.draft.manage＋平台口径＋草稿所属项目在本人可触及范围（范围闸在幂等之前，重放不绕过）；
     * 写库经幂等服务执行，成功留 contract.draft_rent_terms_updated、失败留 contract.draft_rent_terms_update_failed（同域写口范式）；先锁合同行再判状态。
     */
    @Transactional
    public ContractRecord editDraftRentTerms(String contractId, ContractDraftRentTermsRequest request) {
        try {
            authorizationService.requirePermission("contract.draft.manage");
            authorizationService.requirePlatformScope();
            ContractDraftRentTermsRequest actual = requireRequest(request);
            ContractRecord found = requireContractInReach(contractId, "contract.draft.manage");
            return idempotencyService.execute("editContractDraftRentTerms:" + found.id(), actual.idempotencyKey(), actual,
                    () -> editDraftRentTermsOnce(found.id(), actual), ContractRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("contract.draft_rent_terms_update_failed", "contract.draft.manage", "contract",
                    trimToNull(contractId), null, request == null ? null : request.idempotencyKey(), errorCodeOf(exception));
            throw exception;
        }
    }

    private ContractRecord editDraftRentTermsOnce(String contractId, ContractDraftRentTermsRequest actual) {
        ContractRecord c = lockContract(contractId);
        if (!"drafting".equals(c.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_DRAFTING", "只有草拟中的合同才能编辑租金条款");
        }
        Map<String, Object> incoming = camelKeys(actual.rentTerms());
        // 变更链键（parentContractId／amendmentType／increaseFrom／depositAdjustment）由变更/续租终审写入、页面从不发送：草稿编辑一并拒绝，
        // 否则变更作废重排与退租押金抵扣沿链会触及链上别的合同台账。
        if (incoming.containsKey("businessScope") || incoming.containsKey(ContractScopeGuard.RENT_TERMS_KEY)
                || !java.util.Collections.disjoint(incoming.keySet(), CHAIN_RESERVED_KEYS)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "草稿编辑只开放租金条款，经营范围与变更链等系统维护项不在此修改");
        }
        rejectFixedEnergyCharge(incoming);
        Map<String, Object> terms = new HashMap<>(c.rentTerms() == null ? Map.of() : c.rentTerms());
        terms.putAll(incoming);
        Long deposit = draftDeposit(terms, actual.depositCent());
        ContractRecord saved = repository.saveContract(new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(),
                c.operationSubjectId(), c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(), terms, c.commissionMode(),
                c.energyFeeBasis(), deposit, c.penaltyTerms(), c.settlementRules(), c.snapshot(), c.shareholderRatioForReport(),
                c.attachmentIds(), c.lifecycleStatus(), c.projectId(), c.terminatedAt(), c.terminationReason(),
                c.renewedFromContractId(), c.createdAt(), Instant.now(), c.reviewId()));
        auditEventService.recordSuccess("contract.draft_rent_terms_updated", "contract.draft.manage", "contract", c.id(),
                null, actual.idempotencyKey(), Map.of("rent_terms_keys", String.join(",", new java.util.TreeSet<>(incoming.keySet())),
                        "deposit_cent", deposit == null ? "" : deposit));
        return saved;
    }

    /**
     * web 客户端（adminClient.mapKeys）把 rent_terms 嵌套键蛇形化（base_rent_cent／rent_mode／per_sqm_rent_cent／rent_billing_mode…）：顶层蛇形键一律
     * 归一回驼峰（raiseChange 同范式）——押金公式、计租口径、纯提成识别与出账读的都是驼峰键，否则月租读作 0、押金＝月数×0。起草与草稿编辑共用。
     */
    private static Map<String, Object> camelKeys(Map<String, Object> rentTerms) {
        Map<String, Object> out = new HashMap<>(rentTerms == null ? Map.of() : rentTerms);
        for (String key : List.copyOf(out.keySet())) {
            String camel = java.util.regex.Pattern.compile("_([a-z0-9])").matcher(key).replaceAll(m -> m.group(1).toUpperCase());
            if (!camel.equals(key)) {
                out.putIfAbsent(camel, out.remove(key));
            }
        }
        return out;
    }

    /**
     * 起草与草稿编辑共用的押金口径。HP-C03（BSM L1347「押金金额 ＝ 收取月数 × 该商户签约时的月租金（只乘月租，不含物业费、水电能源费等其他费用）」）：
     * 押金由系统按公式算，生效固化时定额（见 activate）；草稿按当前配置月数×月租预填，每平米计租待生效绑铺位固化月租后再算；不接受与公式不一致的手工押金。
     * 第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入，不走月数公式，但不得为零」（BSM L1347「不允许零押金或负押金」）：
     * 纯提成（收租方式 turnover_only，没有月租）必填约定押金且大于 0，原样作为合同押金。
     */
    private Long draftDeposit(Map<String, Object> terms, Long requested) {
        if (isTurnoverOnly(terms)) {
            if (requested == null || requested <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "纯提成合同须录入合同约定押金（分，大于 0），不允许零押金或负押金");
            }
            return requested;
        }
        Long deposit = isPerSqm(terms) ? null : depositFor(terms, repository.depositMonthsSetting().value());
        if (requested != null && !requested.equals(deposit)) {
            throw ApiException.badRequest("CONTRACT_DEPOSIT_FORMULA_MISMATCH",
                    "押金＝收取月数×签约月租（只乘月租），由系统计算，不接受与公式不一致的手工押金");
        }
        return deposit;
    }

    /** 幂等键 → 稳定 id 后缀（sha256 前 48 位 hex，避免长键截断碰撞）。 */
    private String deterministicSuffix(String idempotencyKey) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).substring(0, 48);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /** 追加合同附件（电子签章扫描件归档），去重合并。 */
    @Transactional
    public ContractRecord addAttachments(String contractId, ContractAttachmentRequest request) {
        authorizationService.requirePermission("contract.draft.manage");
        ContractAttachmentRequest actual = requireRequest(request);
        List<String> add = (actual.attachmentIds() == null ? List.<String>of() : actual.attachmentIds())
                .stream().map(s -> requireText(s, "attachment_id")).distinct().toList();
        if (add.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "attachment_ids is required");
        }
        ContractRecord c = requireContractInReach(contractId, "contract.draft.manage");
        List<String> merged = new ArrayList<>(c.attachmentIds() == null ? List.of() : c.attachmentIds());
        for (String a : add) {
            if (!merged.contains(a)) {
                merged.add(a);
            }
        }
        ContractRecord saved = repository.saveContract(withAttachments(c, merged));
        auditEventService.recordSuccess("contract.attachments_added", "contract.draft.manage", "contract", contractId,
                null, actual.idempotencyKey(), Map.of("added", add.size(), "total", merged.size()));
        return saved;
    }

    /**
     * 3.13 / AUD-002②：草稿意向空间存在性 + 与生效合同租期重叠早检——纯校验、不写库，供 createDraft 落库前调用。
     * 返回去重后的 spaceId 列表（供落库后 bindContractSpaces）。空/缺省=不校验不绑定。
     */
    private List<String> validateDraftSpaces(
            LocalDate effectiveDate, LocalDate expiryDate, String contractId, List<String> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) {
            return List.of();
        }
        List<String> resolved = spaceIds.stream().map(id -> requireText(id, "space_id")).distinct().toList();
        for (String spaceId : resolved) {
            if (platformRepository != null && platformRepository.findSpaceById(spaceId).isEmpty()) {
                throw ApiException.badRequest("CONTRACT_SPACE_NOT_FOUND", "铺位不存在：" + spaceId);
            }
            if (effectiveDate != null && expiryDate != null
                    && repository.hasOverlappingEffectiveContractForSpace(spaceId, effectiveDate, expiryDate, contractId)) {
                throw ApiException.conflict("CONTRACT_SPACE_CONFLICT",
                        "该铺位已被生效期重叠的合同占用：" + spaceId);
            }
        }
        return resolved;
    }

    private void requireNoOverlappingEffectiveContract(ContractRecord contract, String spaceId) {
        if (contract.effectiveDate() == null || contract.expiryDate() == null) {
            return; // 租期未定无从判重叠；生效终检兜底。
        }
        if (repository.hasOverlappingEffectiveContractForSpace(
                spaceId, contract.effectiveDate(), contract.expiryDate(), contract.id())) {
            throw ApiException.conflict("CONTRACT_SPACE_CONFLICT",
                    "该铺位已被生效期重叠的合同占用：" + spaceId);
        }
    }

    // ============ 提交 + 统一审批（HP-C01） ============
    /**
     * 提交审批：按「新建」类模板开统一审批复核单（BSM L1349「发起时指定这条主线的最终审批人（默认总经理），系统按组织层级逐级自动
     * 补全至该最终审批人；业务审核、财务审核、法务审核作为平行的相关部门线，各指定该部门的最终审批人」），合同→待确认并挂复核单号。
     * 审批人在 /api/v1/platform/supervisor-review/{review_id}/approve 逐节点裁决，终态经 {@link #onReviewDecided} 回写。
     */
    // AUD-002①：submit 多写（开复核单+改状态）须原子；@Transactional 曾被私有方法挤走，此处补回。
    @Transactional
    public ContractRecord submit(String contractId, ContractSubmitRequest request) {
        authorizationService.requirePlatformScope();
        ContractSubmitRequest actual = requireRequest(request);
        ContractRecord contract = requireContractInReach(contractId, "contract.draft.manage");
        if (!"drafting".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_DRAFTING", "只有草拟中的合同才能提交审批");
        }
        // 3.13：提交审批前对草稿预绑定空间再检冲突——审批人不该审一份注定冲突的合同。
        for (String spaceId : repository.listContractSpaceIds(contractId)) {
            requireNoOverlappingEffectiveContract(contract, spaceId);
        }
        // 第四轮裁决第 4 条「非纯提成合同月租为 0 的不得提交审批」（每平米计租看单价，月租待生效按铺位面积固化）；第三轮裁决第 1 条纯提成约定押金不得为零。
        Map<String, Object> terms = contract.rentTerms() == null ? Map.of() : contract.rentTerms();
        if (isTurnoverOnly(terms)) {
            if (contract.depositCent() == null || contract.depositCent() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "纯提成合同须录入合同约定押金（分，大于 0），不允许零押金或负押金");
            }
        } else if (numLong(terms.get(isPerSqm(terms) ? "perSqmRentCent" : "baseRentCent")) <= 0) {
            throw ApiException.badRequest("CONTRACT_RENT_REQUIRED", "请先在草稿上补录月租");
        }
        String reviewId = openReview(ContractApprovalTemplateRecord.TYPE_NEW, "contract", contractId, contract.projectId(),
                actual.finalApproverId(), actual.parallelLines(), actual.reason(), actual.idempotencyKey());
        ContractRecord saved = repository.saveContract(withReview(withStatus(contract, "pending_confirmation"), reviewId));
        auditEventService.recordSuccess("contract.submitted", "contract.draft.manage", "contract", contractId,
                null, actual.idempotencyKey(), Map.of("review_id", reviewId));
        return saved;
    }

    /**
     * HP-C01：统一审批复核单终态回写（BSM L1349「主线与各条平行线都通过才生效」「任一节点驳回则整单退回起草」）。
     * 由 SupervisorReviewService.approve 在同一事务内发布；只认合同/变更上挂着的那张复核单（review_id 一致且对象仍待审），
     * 其余事件（别的域、过期复核单）一律忽略。新签：通过→待生效（生效仍走 activate 绑铺位），驳回→退回起草；
     * 变更/超范围申请：通过→应用变更（含退租变更自动结算），驳回→变更置 rejected；
     * 到期终止（挂在到期结算单上）：通过→过齐单/争议闸后自动落账、合同到期，驳回→结算单作废（重新开账即重新发起）。
     */
    @org.springframework.context.event.EventListener
    @Transactional
    public void onReviewDecided(SupervisorReviewDecisionRecord decision) {
        if (decision == null || !Set.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        boolean approved = "approved".equals(decision.reviewStatus());
        if (("contract." + ContractApprovalTemplateRecord.TYPE_EXPIRY_TERMINATION).equals(decision.actionType())
                && "contract_settlement".equals(decision.objectType())) {
            ContractSettlementRecord settlement = repository.findSettlementById(decision.objectId()).orElse(null);
            if (settlement == null || !LIVE_SETTLEMENT_STATUSES.contains(settlement.status())
                    || !decision.reviewId().equals(settlement.reviewId())) {
                return;
            }
            if (approved) {
                finalizeSettlementWithLedger(settlement, decision.reviewId());
            } else {
                // 第三轮并发约定：先锁合同行再重读（与商户异议/不接受裁决同序），仍在办才作废——异议事务不会再把已作废的单写回「有争议」。
                lockContract(settlement.contractId());
                ContractSettlementRecord live = repository.findSettlementById(settlement.id()).orElseThrow();
                if (LIVE_SETTLEMENT_STATUSES.contains(live.status())) {
                    repository.saveSettlement(live.withStatus("voided", live.settlementItems(), Instant.now()));
                    auditEventService.recordSuccess("contract.settlement_voided", "governance.supervisor_review.approve",
                            "contract_settlement", live.id(), null, decision.reviewId(),
                            Map.of("trigger", live.triggerType(), "reason", "review_rejected"));
                }
            }
            return;
        }
        if (("contract." + ContractApprovalTemplateRecord.TYPE_NEW).equals(decision.actionType())
                && "contract".equals(decision.objectType())) {
            ContractRecord contract = repository.findContractById(decision.objectId()).orElse(null);
            if (contract == null || !"pending_confirmation".equals(contract.lifecycleStatus())
                    || !decision.reviewId().equals(contract.reviewId())) {
                return;
            }
            ContractRecord saved = repository.saveContract(withStatus(contract, approved ? "pending_effective" : "drafting"));
            // HP-C01-R3：复核终态留痕 contract.review_approved / contract.review_rejected（变更沿用 contract.change_approved/_rejected）。
            auditEventService.recordSuccess(approved ? "contract.review_approved" : "contract.review_rejected",
                    "governance.supervisor_review.approve", "contract", contract.id(), null, decision.reviewId(),
                    Map.of("review_id", decision.reviewId(), "status", saved.lifecycleStatus()));
            return;
        }
        if (("contract." + ContractApprovalTemplateRecord.TYPE_CHANGE).equals(decision.actionType())
                && "contract_change".equals(decision.objectType())) {
            ContractChangeRecord change = repository.findChangeById(decision.objectId()).orElse(null);
            if (change == null || !"pending".equals(change.approvalStatus())
                    || !decision.reviewId().equals(change.reviewId())) {
                return;
            }
            Instant now = Instant.now();
            if (!approved) {
                repository.saveChange(new ContractChangeRecord(change.id(), change.contractId(), change.changeType(),
                        change.payload(), "rejected", null, change.projectId(), change.createdAt(), now, change.reviewId()));
                // HP-C02：退租/提前终止驳回 → 发起时开的那张结算单作废（合同继续履行，不留悬挂单；商户异议挂起的有争议／诉讼仲裁态同样作废）。
                // 第三轮并发约定：先锁合同行再读结算单（与商户异议/不接受裁决同序），异议事务不会再把已作废的单写回「有争议」。
                if (TERMINATION_CHANGE_TYPES.contains(change.changeType())) {
                    lockContract(change.contractId());
                    repository.findSettlementById("cst_" + deterministicSuffix(change.id()))
                            .filter(st -> LIVE_SETTLEMENT_STATUSES.contains(st.status()))
                            .ifPresent(st -> repository.saveSettlement(st.withStatus("voided", st.settlementItems(), now)));
                }
                auditEventService.recordSuccess("contract.change_rejected", "governance.supervisor_review.approve",
                        "contract_change", change.id(), null, decision.reviewId(),
                        Map.of("change_type", change.changeType(), "review_id", decision.reviewId()));
                return;
            }
            // 「只有生效中的合同才能变更」（raiseChange 既有规则）在终审应用时复核：同一合同可同时挂多张待审变更，先终审的面积/位置变更（原合同置已变更）
            // 或退租（已终止）之后，其余变更不得再改它（否则租金调整等把它写回生效中并重复出账，同一铺位两份生效合同——BSM L1350「绑定铺位（防止同一铺位重复出租）」）。
            // 先锁合同行（与各终审写合同同序），非生效中即 409 CONTRACT_NOT_EFFECTIVE、终审整笔回滚由审批人驳回（同 applyRenewal 409 范式）；
            // 退租/提前终止沿用 settleTerminationIfNeeded 既有的非生效即跳过（I2 守卫）。
            ContractRecord contract = lockContract(change.contractId());
            if (!TERMINATION_CHANGE_TYPES.contains(change.changeType()) && !"effective".equals(contract.lifecycleStatus())) {
                throw ApiException.conflict("CONTRACT_NOT_EFFECTIVE", "只有生效中的合同才能变更");
            }
            Map<String, Object> newSnapshot = applyChange(contract, change);
            settleTerminationIfNeeded(contract, change, now);   // 退租变更终审通过→自动结算落财务台账（now＝最后审批完成时刻）
            repository.saveChange(new ContractChangeRecord(change.id(), change.contractId(), change.changeType(),
                    change.payload(), "approved", newSnapshot, change.projectId(), change.createdAt(), now, change.reviewId()));
            auditEventService.recordSuccess("contract.change_approved", "governance.supervisor_review.approve",
                    "contract_change", change.id(), null, decision.reviewId(),
                    Map.of("change_type", change.changeType(), "review_id", decision.reviewId()));
        }
    }

    // ============ 生效 + 固化 ============
    /** 生效：固化收益快照 + 绑定铺位（防重复出租）。合同须待生效。 */
    @Transactional
    public ContractRecord activate(String contractId, ContractActivateRequest request) {
        authorizationService.requirePermission("contract.lifecycle.activate");
        ContractActivateRequest actual = requireRequest(request);
        List<String> spaceIds = (actual.spaceIds() == null ? List.<String>of() : actual.spaceIds())
                .stream().map(s -> requireText(s, "space_id")).distinct().toList();
        if (spaceIds.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "space_ids is required");
        }
        ContractRecord contract = requireContractInReach(contractId, "contract.lifecycle.activate");
        if (!"pending_effective".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_PENDING_EFFECTIVE",
                    "合同必须通过全部审批后才能生效");
        }
        for (String spaceId : spaceIds) {
            if (repository.hasOverlappingEffectiveContractForSpace(
                    spaceId, contract.effectiveDate(), contract.expiryDate(), contractId)) {
                throw ApiException.conflict("CONTRACT_SPACE_CONFLICT",
                        "该铺位已被生效期重叠的合同占用：" + spaceId);
            }
        }
        // 18F 第 80 条（PGM:413「所有基础对象都要带“综合体项目归属”」；裁决 M737-iter4 说明（三）「跨项目经营商户的金额按项目拆分」）：
        // 合同未带项目（工作台起草不提交所属综合体）时取所绑第一个铺位的项目，铺位绑定、生效合同与预排的租金应收随之带项目。
        String projectId = contract.projectId() != null ? contract.projectId() : repository.findSpaceProjectId(spaceIds.get(0));
        repository.bindContractSpaces(contractId, spaceIds, projectId);
        // 批2b:每平米租金生效固化——用绑定铺位登记面积×每平米单价算月租(baseRentCent)固化进生效快照。
        ContractRecord contractEff = withRentTerms(contract, resolvePerSqmBaseRent(contract.rentTerms(), spaceIds));
        // HP-C03（BSM L1347「押金金额 ＝ 收取月数 × 该商户签约时的月租金」）：押金＝当前配置收取月数×生效固化月租（每平米为上一步按铺位面积
        // 固化的 baseRentCent），月数随快照固化（BSM L1353「原合同押金月数按原合同快照沿用，不改用当前配置月数」）。
        // 第三轮裁决第 1 条：纯提成合同押金沿用起草录入的约定金额，快照不写押金月数（不走月数公式）。
        boolean agreedDeposit = isTurnoverOnly(contractEff.rentTerms());
        Integer depositMonths = agreedDeposit ? null : repository.depositMonthsSetting().value();
        ContractRecord frozen = new ContractRecord(
                contractEff.id(), contractEff.merchantId(), contractEff.contractSubjectId(), contractEff.operationSubjectId(),
                contractEff.settlementSubjectId(), contractEff.effectiveDate(), contractEff.expiryDate(), contractEff.rentTerms(),
                contractEff.commissionMode(), contractEff.energyFeeBasis(),
                agreedDeposit ? contractEff.depositCent() : Long.valueOf(depositFor(contractEff.rentTerms(), depositMonths)),
                contractEff.penaltyTerms(), contractEff.settlementRules(), null, contractEff.shareholderRatioForReport(),
                contract.attachmentIds(), "effective", projectId, null, null,
                contract.renewedFromContractId(), contract.createdAt(), Instant.now(), contract.reviewId());
        ContractRecord effective = repository.saveContract(withSnapshot(frozen, buildSnapshot(frozen, depositMonths)));
        // P1-A 收入闭环：生效即按整租期逐期幂等排租金/物业/能源 AR（base+递增−免租，免租/0 跳过）；营业额抽成类收租方式不预排租金（138-R5，见 scheduleTermBills）。
        // ponytail: 整租期一次性预排 vs @Scheduled 逐期挂账——选预排：确定可测 + 幂等（genFeeBill 查重，重复生效不重挂），
        //   且每期账单各带自身 due_date（期首），看板按 due_date 逐期确认收入，不在生效日堆叠整租期。
        //   若改 @Scheduled 累积巨量空跑作业，反更重；catch-up 仍可手工调 period-bills 端点（P1-B 按钮）。
        int billed = scheduleTermBills(effective, YearMonth.from(effective.effectiveDate()), "activate:" + effective.id(), true);
        // CG-01（PGM「招商运营细则」）：「合同审批全部通过、生效后转为已签约」——来源招商线索在此置已签约
        // （签约起草 LeasingService.signIntent 不再置）。同一生效事务内，生效失败则线索不动。
        if (effectiveLeadPort != null) {
            effectiveLeadPort.markSourceLeadSigned(contractId, actual.idempotencyKey());
        }
        auditEventService.recordSuccess("contract.activated", "contract.lifecycle.activate", "contract", contractId,
                null, actual.idempotencyKey(), Map.of("space_count", spaceIds.size(), "rent_bills", billed));
        return effective;
    }

    /**
     * 自 from 月起至到期月逐期幂等挂账各费目；返回新挂账条数。生效自动调用（from＝生效月，整租期）；HP-C03-4 续租终审调用（from＝原到期月，补排延长期，
     * 已出账期间由 billPeriodFees 去重跳过）；第四轮裁决第 2 条面积/位置变更子合同生效调用（from＝变更生效月，重排剩余租期；纯提成合同 withRent=false 只排物业费）；
     * 第五轮裁决第 2 条租金调整终审调用（from＝终审当月，同一合同按调整后月租重排剩余租期，被作废账单不再算已出账）。
     * 第十八批 Q 段第 138 条 138-R5（ENUM:257「营业额抽成账单在该账期的薪资月结完成、该账期月结定格时确定的服务费应发数与扣留数定数之后出账，账单一次出准，
     * 不使用预估数、不在下期补差」）：只有固定租金合同预排租金；营业额抽成类收租方式（纯提成、保底与抽成取高、保底加抽成叠加）只预排物业费、车位月租，
     * 租金由财务在该账期月结定格后经按期出账（generatePeriodBills）或单期租金挂账（generateRentBill）一次出准。
     */
    private int scheduleTermBills(ContractRecord c, YearMonth from, String idemPrefix, boolean withRent) {
        YearMonth end = YearMonth.from(c.expiryDate());
        boolean rent = withRent && isFixedRent(c.rentTerms());
        int count = 0;
        for (YearMonth ym = from; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            count += billPeriodFees(c, ym.toString(), idemPrefix + ":" + ym, rent).size();
        }
        return count;
    }

    // ============ 变更（6类） ============
    @Transactional
    public ContractChangeRecord raiseChange(String contractId, ContractChangeRequest request) {
        authorizationService.requirePermission("contract.change.manage");
        ContractChangeRequest actual = requireRequest(request);
        String type = requireEnum(actual.changeType(), CHANGE_TYPES, "change_type");
        if ("scope_grant".equals(type)) {
            // scope_grant 只从专用申请入口(requestScopeGrant)发起，通用变更入口拒收(避免缺 requestedCategory 载荷)。
            throw ApiException.badRequest("VALIDATION_FAILED", "超范围经营申请请走 scope-requests 入口");
        }
        ContractRecord contract = requireContractInReach(contractId, "contract.change.manage");
        if (!"effective".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_EFFECTIVE", "只有生效中的合同才能变更");
        }
        Instant now = Instant.now();
        // 经营范围变更载荷规范化：web 客户端会把嵌套键蛇形化（allowed_categories），统一归一到
        // rent_terms 既有的 camel 键 allowedCategories 并做词表校验，approveChange→applyChange putAll 后即生效。
        Map<String, Object> payload = new HashMap<>(actual.payload() == null ? Map.of() : actual.payload());
        rejectFixedEnergyCharge(payload);
        // 租金调整/经营范围变更终审 putAll 载荷进 rent_terms：变更链等系统维护键（驼峰或蛇形）一律拒收，同草稿编辑口径。
        if (Set.of("rent_change", "business_scope_change").contains(type)
                && !java.util.Collections.disjoint(camelKeys(payload).keySet(), CHAIN_RESERVED_KEYS)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "变更载荷不得带变更链与临时经营许可等系统维护项（parent_contract_id 等）");
        }
        Object rawCategories = payload.remove("allowed_categories");
        if (rawCategories == null) {
            rawCategories = payload.get(ContractScopeGuard.RENT_TERMS_KEY);
        }
        List<String> changedCategories = ContractScopeGuard.validateAllowedCategories(rawCategories);
        if (changedCategories != null) {
            payload.put(ContractScopeGuard.RENT_TERMS_KEY, changedCategories);
        }
        // HP-C02-R6（2026-10-18 裁决第 2 条「发起退租/提前终止变更时必填「退租日」」）：载荷退租日须为 ISO 日期，web 蛇形键 move_out_date
        // 归一为 moveOutDate；缺失或不可解析即 400。
        YearMonth through = null;
        if (TERMINATION_CHANGE_TYPES.contains(type)) {
            Object rawDate = payload.remove("move_out_date");
            if (rawDate == null) {
                rawDate = payload.get(MOVE_OUT_DATE_KEY);
            }
            LocalDate moveOutDate;
            try {
                moveOutDate = LocalDate.parse(String.valueOf(rawDate));
            } catch (java.time.format.DateTimeParseException e) {
                throw ApiException.badRequest("VALIDATION_FAILED", "退租/提前终止须填写退租日（yyyy-MM-dd）");
            }
            payload.put(MOVE_OUT_DATE_KEY, moveOutDate.toString());
            through = YearMonth.from(moveOutDate);
        }
        // 第五轮裁决第 2 条「租金调整生效时……按调整后月租重排剩余租期」：web 蛇形键归一（否则 putAll 进 rent_terms 的是 base_rent_cent、月租不变）；
        // 非纯提成合同须给调整后月租——每平米计租给调整后单价 perSqmRentCent（终审按合同铺位面积算月租），否则给 baseRentCent，缺项 400（同续租口径）。
        if ("rent_change".equals(type)) {
            for (String[] key : new String[][]{{"base_rent_cent", "baseRentCent"}, {"per_sqm_rent_cent", "perSqmRentCent"}}) {
                if (payload.containsKey(key[0])) {
                    payload.putIfAbsent(key[1], payload.remove(key[0]));
                }
            }
            boolean perSqm = isPerSqm(contract.rentTerms());
            if (!isTurnoverOnly(contract.rentTerms()) && numLong(payload.get(perSqm ? "perSqmRentCent" : "baseRentCent")) <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        perSqm ? "每平米计租合同租金调整须填写调整后每平米单价（分，大于 0）" : "租金调整须填写调整后月租（分，大于 0）");
            }
        }
        // HP-C03-1（BSM L1353「面积或位置变更……的目标押金＝原合同押金月数×变更……后的月租；子合同写明目标金额、补退差额及办理期限……
        // 原合同押金月数按原合同快照沿用」）：押金按公式算、不收手工押金；押金补退办理期限必填（ISO 日期）；非每平米计租须给变更后月租
        // baseRentCent&gt;0（每平米按子合同铺位面积重算）；原合同快照缺押金月数（本次改动前生效的旧合同）拒绝并提示、不推算。web 蛇形键归一。
        // HP-C03-4（裁决第 4 条「续租变更载荷带续租后月租、新到期日、押金办理期限」；BSM L1353「面积或位置变更以及续租的目标押金＝原合同押金月数×变更或续租后
        // 的月租」）：续租同走上述押金口径；新到期日须晚于现到期日；每平米计租给续租后单价 perSqmRentCent（终审按合同铺位面积算月租），否则给续租后月租 baseRentCent。
        boolean renewal = "renewal".equals(type);
        if (AMENDMENT_TYPES.contains(type) || renewal) {
            if (payload.containsKey("newDepositCent") || payload.containsKey("new_deposit_cent")) {
                throw ApiException.badRequest("VALIDATION_FAILED", "变更或续租后押金＝原合同押金月数×变更或续租后月租，由系统计算，不接受手工押金");
            }
            for (String[] key : new String[][]{{"base_rent_cent", "baseRentCent"}, {"deposit_deadline", "depositDeadline"},
                    {"space_ids", "spaceIds"}, {"per_sqm_rent_cent", "perSqmRentCent"}, {"new_expiry_date", "newExpiryDate"}}) {
                if (payload.containsKey(key[0])) {
                    payload.putIfAbsent(key[1], payload.remove(key[0]));
                }
            }
            if (renewal) {
                LocalDate newExpiry;
                try {
                    newExpiry = LocalDate.parse(String.valueOf(payload.get("newExpiryDate")));
                } catch (java.time.format.DateTimeParseException e) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "续租须填写续租后到期日（yyyy-MM-dd）");
                }
                if (!newExpiry.isAfter(contract.expiryDate())) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "续租后到期日须晚于现到期日");
                }
                payload.put("newExpiryDate", newExpiry.toString());
            }
            // 第三轮裁决第 1 条「这类合同变更/续租时押金不按公式重算」：纯提成合同（没有月租）不要求变更/续租后月租、押金补退办理期限与快照押金月数。
            if (!isTurnoverOnly(contract.rentTerms())) {
                try {
                    payload.put("depositDeadline", LocalDate.parse(String.valueOf(payload.get("depositDeadline"))).toString());
                } catch (java.time.format.DateTimeParseException e) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "面积/位置变更及续租须填写押金补退办理期限（yyyy-MM-dd）");
                }
                if (renewal) {
                    boolean perSqm = isPerSqm(contract.rentTerms());
                    if (numLong(payload.get(perSqm ? "perSqmRentCent" : "baseRentCent")) <= 0) {
                        throw ApiException.badRequest("VALIDATION_FAILED",
                                perSqm ? "每平米计租合同续租须填写续租后每平米单价（分，大于 0）" : "续租须填写续租后月租（分，大于 0）");
                    }
                } else {
                    Map<String, Object> childTerms = new HashMap<>(contract.rentTerms() == null ? Map.of() : contract.rentTerms());
                    childTerms.putAll(payload);
                    if (!isPerSqm(childTerms) && numLong(payload.get("baseRentCent")) <= 0) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "面积/位置变更须填写变更后月租（分，大于 0）");
                    }
                }
                requireDepositMonths(contract);
            }
        }
        // HP-C01：变更按「变更」类模板开统一审批复核单（PGM L536「三类各走各自的审批流程模板」），变更记录挂复核单号。
        String changeId = "cch_" + UUID.randomUUID();
        String reviewId = openReview(ContractApprovalTemplateRecord.TYPE_CHANGE, "contract_change", changeId,
                contract.projectId(), actual.finalApproverId(), actual.parallelLines(), actual.reason(), actual.idempotencyKey());
        ContractChangeRecord saved = repository.saveChange(new ContractChangeRecord(
                changeId, contractId, type, Map.copyOf(payload), "pending", null, contract.projectId(), now, now, reviewId));
        // HP-C02（BSM L1352／ENUM L252、Q16A「不是齐单前连审批也不得发起」）：退租/提前终止发起即按 settle() 口径开一张待结算单
        // 承载齐单核对事实与诸态（不落账、不另查权限）；id 由变更 id 确定性派生，审批终态沿用同一张单落账、驳回则作废。
        // 同合同已有在办结算单（手动开账或争议挂起）则不重复开，最后审批沿用那张单落账。
        if (TERMINATION_CHANGE_TYPES.contains(type) && findLiveSettlement(contractId) == null) {
            repository.saveSettlement(new ContractSettlementRecord("cst_" + deterministicSuffix(changeId), contractId,
                    type, "pending_settlement", computeTerminationItems(contract, type, through), null, null, null,
                    contract.projectId(), now, now));
        }
        auditEventService.recordSuccess("contract.change_raised", "contract.change.manage", "contract_change",
                saved.id(), null, actual.idempotencyKey(),
                Map.of("change_type", type, "contract_id", contractId, "review_id", reviewId));
        return saved;
    }

    // ============ 超范围下单经营申请（#30，2026-07-19 拍板） ============
    /**
     * 超范围下单被合同经营范围拦下后，就某品类提交经营申请：进 contract_changes(type=scope_grant) 走既有审批链。
     * 终审通过 → applyChange 授予该商户该品类临时/永久经营许可（写 rent_terms，下单侧 guard 即读即放行）；驳回 → 不予下单。
     */
    @Transactional
    public ContractChangeRecord requestScopeGrant(String contractId, ContractScopeRequestRequest request) {
        authorizationService.requirePermission("contract.scope_request.raise");
        ContractScopeRequestRequest actual = requireRequest(request);
        String category = requireText(actual.category(), "category");
        if (!ContractScopeGuard.ALLOWED_CATEGORY_VALUES.contains(category)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "category 含系统外取值「" + category + "」，超范围经营申请只能就既有业务类目提交");
        }
        String reason = requireText(actual.reason(), "reason");
        String grantType = requireEnum(
                trimToNull(actual.grantType()) == null ? "permanent" : actual.grantType().trim(),
                GRANT_TYPES, "grant_type");
        LocalDate validUntil = null;
        if ("temporary".equals(grantType)) {
            validUntil = requireDate(actual.validUntil(), "valid_until");
            if (!validUntil.isAfter(LocalDate.now())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "临时经营许可 valid_until 须为将来日期");
            }
        }
        ContractRecord contract = requireContractInReach(contractId, "contract.scope_request.raise");
        if (!"effective".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_EFFECTIVE", "只有生效中的合同才能发起范围调整申请");
        }
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestedCategory", category);
        payload.put("reason", reason);
        payload.put("grantType", grantType);
        if (validUntil != null) {
            payload.put("validUntil", validUntil.toString());
        }
        // HP-C01：超范围申请按「变更」类模板开统一审批复核单（ENUM L251 经营范围授权属合同变更类型）。
        String changeId = "cch_" + UUID.randomUUID();
        String reviewId = openReview(ContractApprovalTemplateRecord.TYPE_CHANGE, "contract_change", changeId,
                contract.projectId(), actual.finalApproverId(), actual.parallelLines(), reason, actual.idempotencyKey());
        ContractChangeRecord saved = repository.saveChange(new ContractChangeRecord(
                changeId, contractId, "scope_grant", Map.copyOf(payload), "pending", null, contract.projectId(), now, now,
                reviewId));
        auditEventService.recordSuccess("contract.scope_request_raised", "contract.scope_request.raise",
                "contract_change", saved.id(), null, actual.idempotencyKey(),
                Map.of("contract_id", contractId, "category", category, "grant_type", grantType, "review_id", reviewId));
        return saved;
    }

    public PageResponse<ContractChangeRecord> listChanges(String contractId) {
        requireContractInReach(contractId, "contract.change.manage");
        List<ContractChangeRecord> items = repository.listChangesByContract(contractId);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    // ============ 退租 / 到期结算 ============
    /**
     * 退租/到期结算：按合同 settlement_rules 逐例算；带 disputeReason 则挂起「有争议」。
     * HP-C02（BSM L1352「退租、提前终止、合同到期等各种终止结算，都要经过多级审批……审批全部通过后，系统按算好的结算清单自动落账，不再单设手动确认入口」；
     * ENUM L255「到期不得无审批自动终止」）：到期结算开账即按「到期终止」类模板开统一审批复核单并挂在结算单上，终审回写（onReviewDecided）落账；
     * 退租/提前终止的审批挂在退租变更上（raiseChange），此处只开单。
     */
    @Transactional
    public ContractSettlementRecord settle(String contractId, ContractSettleRequest request) {
        authorizationService.requirePermission("contract.settlement.manage");
        ContractSettleRequest actual = requireRequest(request);
        String trigger = requireEnum(actual.triggerType(), SETTLE_TRIGGERS, "trigger_type");
        ContractRecord contract = requireContractInReach(contractId, "contract.settlement.manage");
        if (!Set.of("effective", "expired").contains(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_SETTLEABLE", "合同不在生效或到期状态");
        }
        // I-3 退租/提前终止须合同仍生效（终止活跃租约）；到期合同只可走 expiry——否则确认时会被误判「被别的退租终止」而作废、押金静默不退。
        if (("move_out".equals(trigger) || "early_termination".equals(trigger))
                && !"effective".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_SETTLEABLE",
                    "退租/提前终止结算仅限生效合同；到期合同请走到期结算");
        }
        String dispute = trimToNull(actual.disputeReason());
        Instant now = Instant.now();
        // 开账即算结算清单（押金退还净额/违约金/未清清算），但不落账——落账只在终审回写时发生。
        String status = dispute != null ? "disputed" : "pending_settlement";
        List<Map<String, Object>> items = dispute != null ? List.of()
                : computeTerminationItems(contract, trigger, documentsThroughMonth(contract, trigger));
        String settlementId = "cst_" + UUID.randomUUID();
        String reviewId = "expiry".equals(trigger)
                ? openReview(ContractApprovalTemplateRecord.TYPE_EXPIRY_TERMINATION, "contract_settlement", settlementId,
                        contract.projectId(), actual.finalApproverId(), actual.parallelLines(), actual.reason(),
                        actual.idempotencyKey())
                : null;
        ContractSettlementRecord settlement = repository.saveSettlement(new ContractSettlementRecord(
                settlementId, contractId, trigger, status, items, dispute, null, null,
                contract.projectId(), now, now, null, null, null, null, reviewId));
        auditEventService.recordSuccess("contract.settled", "contract.settlement.manage", "contract_settlement",
                settlement.id(), null, actual.idempotencyKey(), Map.of("trigger", trigger, "status", status));
        return settlement;
    }

    /**
     * 主管裁决退租争议（HP-C04，BSM L1352「由主管裁决后继续」「直至诉讼或仲裁有结果后再行处理」、2026-10-18 裁决第 5、6 条「主管裁决（写结论留痕）后
     * 回待结算继续审批」「诉讼/仲裁有结果后，由有裁决权的主管在后台录入结果，单子回待结算继续审批」）：有争议 → 写裁决结论；有争议，诉讼/仲裁 → 录诉讼/仲裁结果。
     * 结论必填、追加落 dispute_resolution 并记裁决人/时，按既有口径重算清单回待结算，退租变更或到期终止复核单的最后审批随之可继续。
     */
    @Transactional
    public ContractSettlementRecord resolveDispute(String settlementId, ContractResolveDisputeRequest request) {
        authorizationService.requirePermission("contract.settlement.dispute.resolve");
        ContractResolveDisputeRequest actual = requireRequest(request);
        ContractSettlementRecord found = repository.findSettlementById(requireText(settlementId, "settlement_id"))
                .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "结算单不存在或已被删除"));
        requireContractInReach(found.contractId(), "contract.settlement.dispute.resolve");
        ContractRecord contract = lockContract(found.contractId());
        ContractSettlementRecord settlement = repository.findSettlementById(found.id()).orElseThrow();   // 锁后重读状态
        if (!DISPUTED_SETTLEMENT_STATUSES.contains(settlement.status())) {
            throw ApiException.conflict("CONTRACT_SETTLEMENT_NOT_DISPUTED", "结算单不在异议状态");
        }
        String note = requireText(actual.resolutionNote(), "resolution_note");
        String entry = "disputed_litigation".equals(settlement.status())
                ? ContractSettlementRecord.LITIGATION_OUTCOME_PREFIX + note : note;
        String existing = trimToNull(settlement.disputeResolution());
        Instant now = Instant.now();
        ContractSettlementRecord resumed = repository.saveSettlement(settlement.withDispute("pending_settlement",
                computeTerminationItems(contract, settlement.triggerType(), documentsThroughMonth(contract, settlement.triggerType())),
                settlement.disputeReason(), existing == null ? entry : existing + "\n" + entry,
                authorizationService.currentActor().id(), now, now));
        auditEventService.recordSuccess("contract.dispute_resolved", "contract.settlement.dispute.resolve",
                "contract_settlement", settlement.id(), null, actual.idempotencyKey(), Map.of("from_status", settlement.status()));
        return resumed;
    }

    /**
     * HP-C02 齐单核对（BSM L1352／ENUM L252「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；缺单时保持待处理」、
     * 裁决 Q16A「缺单保持待处理。不是齐单前连审批也不得发起」）：生效月→截止月逐月核「相关单据」（ContractRepository.listMissingTerminationDocuments），
     * 缺项→409 列明细、结算单保持原态不记核对人；齐全→在结算单上记核对人/时间/备注。
     * 核对人岗位 HTML 未指定，沿用运营/财务共用的结算权限 contract.settlement.manage。
     * OpenAPI 声明（x-audit-event-failure／x-idempotency-required）：失败留 contract.settlement_documents_check_failed，写库经幂等服务执行（同键重放返回首次结果）；
     * 第三轮并发约定：先锁合同行再重读结算单状态（与商户异议/不接受裁决、主管裁决、终审落账同序），不把并发提交的异议或落账冲回待结算。
     */
    @Transactional
    public ContractSettlementRecord checkSettlementDocuments(String settlementId, ContractResolveDisputeRequest request) {
        try {
            authorizationService.requirePermission("contract.settlement.manage");
            ContractResolveDisputeRequest actual = requireRequest(request);
            String id = requireText(settlementId, "settlement_id");
            requireContractInReach(repository.findSettlementById(id)
                    .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "结算单不存在或已被删除")).contractId(),
                    "contract.settlement.manage");
            return idempotencyService.execute("checkContractSettlementDocuments:" + id, actual.idempotencyKey(), actual,
                    () -> checkDocumentsOnce(id, actual), ContractSettlementRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("contract.settlement_documents_check_failed", "contract.settlement.manage",
                    "contract_settlement", trimToNull(settlementId), null, request == null ? null : request.idempotencyKey(),
                    errorCodeOf(exception));
            throw exception;
        }
    }

    private ContractSettlementRecord checkDocumentsOnce(String settlementId, ContractResolveDisputeRequest actual) {
        ContractSettlementRecord found = repository.findSettlementById(settlementId)
                .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "结算单不存在或已被删除"));
        ContractRecord contract = lockContract(found.contractId());
        ContractSettlementRecord settlement = repository.findSettlementById(found.id()).orElseThrow();   // 锁后重读状态
        if (!"pending_settlement".equals(settlement.status())) {   // 第三轮控制席约定：齐单闸只作用于落账前的「待结算」
            throw ApiException.conflict("CONTRACT_SETTLEMENT_NOT_OPEN", "结算单当前状态不可核对单据");
        }
        YearMonth through = documentsThroughMonth(contract, settlement.triggerType());
        if (through == null) {
            throw ApiException.conflict("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE",
                    "尚未发起退租/提前终止变更，退租日未定，退租对应期间无法核对，保持待处理");
        }
        List<String> missing = repository.listMissingTerminationDocuments(contract, through);
        if (!missing.isEmpty()) {
            throw ApiException.conflict("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE",
                    "退租对应期间相关单据未收齐，保持待处理：" + String.join("、", missing));
        }
        Instant now = Instant.now();
        ContractSettlementRecord checked = repository.saveSettlement(settlement.withDocumentsChecked(
                authorizationService.currentActor().id(), now, trimToNull(actual.resolutionNote())));
        auditEventService.recordSuccess("contract.settlement_documents_checked", "contract.settlement.manage",
                "contract_settlement", settlement.id(), null, actual.idempotencyKey(),
                Map.of("through", through.toString()));
        return checked;
    }

    /**
     * HP-C02-R6 截止月（2026-10-18 裁决第 2 条「期间＝生效月→退租日所在月（合同到期走到期日所在月）。齐单核对、缺单判定、欠费口径都按这个
     * 截止月」）：到期结算＝到期日所在月；退租/提前终止＝合同在审退租/提前终止变更的退租日所在月，尚未发起该变更（手动开账）＝null。
     */
    private YearMonth documentsThroughMonth(ContractRecord contract, String trigger) {
        if ("expiry".equals(trigger)) {
            return YearMonth.from(contract.expiryDate());
        }
        return repository.listChangesByContract(contract.id()).stream()
                .filter(ch -> "pending".equals(ch.approvalStatus()) && TERMINATION_CHANGE_TYPES.contains(ch.changeType()))
                .map(ContractService::moveOutMonth).findFirst().orElse(null);
    }

    /** 退租/提前终止变更载荷的退租日所在月（raiseChange 已校验必填并归一为 moveOutDate）。 */
    private static YearMonth moveOutMonth(ContractChangeRecord change) {
        return YearMonth.from(LocalDate.parse(String.valueOf(change.payload().get(MOVE_OUT_DATE_KEY))));
    }

    /** 合同当前在办的退租结算单（发起时开的／手动开账的／争议挂起的那张），无则 null。 */
    private ContractSettlementRecord findLiveSettlement(String contractId) {
        return repository.listSettlementsByContract(contractId).stream()
                .filter(st -> LIVE_SETTLEMENT_STATUSES.contains(st.status())).findFirst().orElse(null);
    }

    /**
     * HP-C02-R2 退租变更／到期终止复核单最后审批闸（BSM L1352「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；缺单时保持待处理」
     * 「有争议进「有争议」状态挂起、由主管裁决后继续」、裁决 Q16A）：在变更复核单「全部通过」判定点、落账之前核合同在办结算单——
     * 争议挂起 → 409 CONTRACT_SETTLEMENT_DISPUTED；无在办单、未核对或核对后仍有缺项 → 409 CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE。
     * 抛出即随 SupervisorReviewService.approve 整笔回滚：末节点裁决不写、复核单与变更保持待审、不落账；非末阶段事件不到此处。
     */
    private void requireSettlementReadyForFinalApproval(ContractRecord contract, ContractSettlementRecord live, YearMonth through) {
        requireNotDisputed(live);
        if (live == null || live.documentsCheckedAt() == null) {
            throw ApiException.conflict("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE",
                    "退租对应期间相关单据尚未核对完成，最后审批保持待处理");
        }
        List<String> missing = repository.listMissingTerminationDocuments(contract, through);
        if (!missing.isEmpty()) {
            throw ApiException.conflict("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE",
                    "退租对应期间相关单据未收齐，保持待处理：" + String.join("、", missing));
        }
    }

    /** HP-C04：结算单处于「有争议」或「有争议，诉讼/仲裁」→ 409 CONTRACT_SETTLEMENT_DISPUTED（退租变更与到期终止最后审批同闸）。 */
    private static void requireNotDisputed(ContractSettlementRecord settlement) {
        if (settlement != null && DISPUTED_SETTLEMENT_STATUSES.contains(settlement.status())) {
            throw ApiException.conflict("CONTRACT_SETTLEMENT_DISPUTED", "disputed".equals(settlement.status())
                    ? "结算有争议，须主管裁决后继续" : "结算有争议（诉讼/仲裁中），须主管录入诉讼/仲裁结果后继续");
        }
    }

    public PageResponse<ContractSettlementRecord> listSettlements(String contractId) {
        requireContractInReach(contractId, "contract.settlement.manage");
        List<ContractSettlementRecord> items = repository.listSettlementsByContract(contractId);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    /** 到期批转（替代旧 governance ContractExpiryJob 路径）：过期 effective 合同 → expired。系统作业调用，无用户 scope。 */
    public int generateExpiredTransitions(LocalDate asOf) {
        int count = repository.markContractsExpired(asOf);
        if (count > 0) {
            auditEventService.recordSuccess("contract.expired_batch", "contract.lifecycle.activate", "contract",
                    "batch", null, null, Map.of("count", count, "as_of", asOf.toString()));
        }
        return count;
    }

    /**
     * 商户费用续约短信提醒扫描入口。只追加通知 hook：不变更合同状态、不发起续约流程、不写业务表。
     * 由 ContractRenewalNotificationJob 薄 @Scheduled 包装调用，亦供单测直调对拍通知日志。
     * asOf 必传（扫描基准日由调用方按所在地日历日给定）：给不出基准日就不该发这批短信，
     * 与其静默回落到某个"今天"把提醒发错日子，不如当场炸出来。
     */
    public void processMerchantFeeRenewalNotifications(LocalDate asOf, int noticeDays) {
        if (notificationService == null) {
            return;
        }
        LocalDate untilDate = asOf.plusDays(Math.max(0, noticeDays));
        List<ContractRecord> contracts;
        try {
            contracts = repository.listEffectiveContractsExpiringBetween(asOf, untilDate);
        } catch (RuntimeException exception) {
            log.warn("failed to list contracts for merchant fee renewal notification", exception);
            return;
        }
        for (ContractRecord contract : contracts) {
            notifyMerchantFeeRenewalAfterCommit(contract, asOf);
        }
    }

    private void notifyMerchantFeeRenewalAfterCommit(ContractRecord contract, LocalDate asOf) {
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()
                    && TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyMerchantFeeRenewalSafely(contract, asOf);
                    }
                });
                return;
            }
            notifyMerchantFeeRenewalSafely(contract, asOf);
        } catch (RuntimeException exception) {
            log.warn(
                    "merchant fee renewal notification registration failed: contract_id={}",
                    contract == null ? null : contract.id(),
                    exception);
        }
    }

    private void notifyMerchantFeeRenewalSafely(ContractRecord contract, LocalDate asOf) {
        if (contract == null) {
            return;
        }
        try {
            notificationService.notify(
                    MERCHANT_FEE_DOMAIN,
                    MERCHANT_FEE_RENEWAL_EVENT,
                    resolveMerchantFeeRecipientPhone(contract.merchantId(), MERCHANT_FEE_RENEWAL_EVENT),
                    MERCHANT_FEE_RENEWAL_TEMPLATE,
                    merchantFeeRenewalParams(contract, asOf),
                    "mfrenewal:" + contract.id() + ":" + contract.expiryDate());
        } catch (RuntimeException exception) {
            log.warn(
                    "merchant fee renewal notification failed: contract_id={}, idempotency_key={}",
                    contract.id(),
                    "mfrenewal:" + contract.id() + ":" + contract.expiryDate(),
                    exception);
        }
    }

    private Map<String, Object> merchantFeeRenewalParams(ContractRecord contract, LocalDate asOf) {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfNotNull(params, "contract_id", contract.id());
        putIfNotNull(params, "merchant_id", contract.merchantId());
        putIfNotNull(params, "project_id", contract.projectId());
        putIfNotNull(params, "effective_date", contract.effectiveDate());
        putIfNotNull(params, "expiry_date", contract.expiryDate());
        if (contract.expiryDate() != null && asOf != null) {
            params.put("days_to_expiry", ChronoUnit.DAYS.between(asOf, contract.expiryDate()));
        }
        return params;
    }

    private String resolveMerchantFeeRecipientPhone(String merchantId, String event) {
        if (merchantFeeRecipientResolver != null) {
            try {
                String resolved = trimToNull(merchantFeeRecipientResolver.resolvePhone(merchantId, event));
                if (resolved != null) {
                    return resolved;
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "failed to resolve merchant fee recipient: merchant_id={}, event={}",
                        merchantId,
                        event,
                        exception);
            }
        }
        return resolveMerchantPrimaryContactPhone(merchantId);
    }

    private String resolveMerchantPrimaryContactPhone(String merchantId) {
        String normalizedMerchantId = trimToNull(merchantId);
        if (normalizedMerchantId == null || platformRepository == null) {
            return null;
        }
        try {
            return platformRepository.listMerchants().stream()
                    .filter(merchant -> normalizedMerchantId.equals(merchant.id()))
                    .findFirst()
                    .map(MerchantRecord::contactPhone)
                    .map(Strings::trimToNull)
                    .orElse(null);
        } catch (RuntimeException exception) {
            log.warn("failed to resolve merchant primary contact phone: merchant_id={}", normalizedMerchantId, exception);
            return null;
        }
    }

    private void putIfNotNull(Map<String, Object> params, String key, Object value) {
        if (value != null) {
            params.put(key, value);
        }
    }

    // ============ 配置：审批流程模板（V494，三类按综合体可配）+ 到期着色 ============
    public ContractApprovalTemplateRecord saveApprovalFlow(ContractApprovalFlowRequest request) {
        authorizationService.requirePermission("contract.approval.flow.manage");
        ContractApprovalFlowRequest actual = requireRequest(request);
        String projectId = trimToNull(actual.projectId());
        String type = requireEnum(actual.contractType(), ContractApprovalTemplateRecord.TYPES, "contract_type");
        List<String> lines = (actual.requiredLineCodes() == null ? List.<String>of() : actual.requiredLineCodes())
                .stream().map(line -> requireText(line, "required_line_codes")).distinct().toList();
        for (String line : lines) {
            if (!repository.approvalLineActive(line)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "审批线不存在或未启用：" + line);
            }
        }
        String approver = trimToNull(actual.defaultFinalApproverId());
        if (approver != null && !repository.userActive(approver)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "默认最终审批人不存在或未启用：" + approver);
        }
        // HP-C01-R5（BSM L1349「须同时配置上下限」、裁决第 1 条「主线层级数配上下限」）：都不填＝不校验；填则两者同时填写且 1≤最少≤最多。
        Integer minLevels = actual.minMainLevels();
        Integer maxLevels = actual.maxMainLevels();
        if ((minLevels == null) != (maxLevels == null) || (minLevels != null && (minLevels < 1 || minLevels > maxLevels))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "主线层级数上下限须同时填写，且 1 ≤ 最少 ≤ 最多");
        }
        ContractApprovalTemplateRecord saved = repository.saveTemplate(new ContractApprovalTemplateRecord(null, projectId,
                type, trimToNull(actual.name()) == null ? "合同审批流程模板·" + type : actual.name().trim(), lines, approver, true,
                minLevels, maxLevels));
        auditEventService.recordSuccess("contract.approval_flow_saved", "contract.approval.flow.manage",
                "contract_approval_flow", saved.id(), null, actual.idempotencyKey(),
                Map.of("project_id", projectId == null ? "default" : projectId, "contract_type", type, "line_count", lines.size(),
                        "min_main_levels", minLevels == null ? "" : minLevels, "max_main_levels", maxLevels == null ? "" : maxLevels,
                        "default_final_approver_id", approver == null ? "" : approver));
        return saved;
    }

    /** 缺 contract_type 返回该综合体三类各自的生效模板（项目覆盖优先、否则平台默认）。 */
    public List<ContractApprovalTemplateRecord> listApprovalFlow(String projectId, String contractType) {
        String type = trimToNull(contractType);
        if (type == null) {
            return repository.listTemplates(trimToNull(projectId));
        }
        return repository.findActiveTemplate(trimToNull(projectId),
                requireEnum(type, ContractApprovalTemplateRecord.TYPES, "contract_type")).map(List::of).orElse(List.of());
    }

    // ============ 配置：押金退还等待期（HP-C06，按项目可配） ============
    public DepositRefundWaitSettingRecord getDepositRefundWaitSetting(String projectId) {
        authorizationService.requirePermission(DEPOSIT_REFUND_WAIT_MANAGE);
        return repository.depositRefundWaitSetting(requireWaitProject(projectId));
    }

    /**
     * BSM L1352／ENUM L252「业务参数，按项目可配，出厂默认 30 日，须同时配置上下限」：日数与上下限须同时填写，0 ≤ 下限 ≤ 日数 ≤ 上限
     * （控制席技术约定：值落在闭区间内，不自拟上下限出厂数值）；只作用于该项目，项目须在本权限可触及范围内（82b-R3）。
     */
    @Transactional
    public DepositRefundWaitSettingRecord saveDepositRefundWaitSetting(DepositRefundWaitSettingRequest request) {
        try {
            authorizationService.requirePermission(DEPOSIT_REFUND_WAIT_MANAGE);
            DepositRefundWaitSettingRequest actual = requireRequest(request);
            String projectId = requireWaitProject(actual.projectId());
            Integer value = actual.value();
            Integer min = actual.minValue();
            Integer max = actual.maxValue();
            if (value == null || min == null || max == null || min < 0 || value < min || value > max) {
                throw ApiException.badRequest("VALIDATION_FAILED", "押金退还等待期须同时填写日数及上下限，下限不小于 0，日数须在上下限内");
            }
            return idempotencyService.execute("saveContractDepositRefundWait", actual.idempotencyKey(), actual, () -> {
                DepositRefundWaitSettingRecord saved = new DepositRefundWaitSettingRecord(projectId, value, min, max);
                repository.saveDepositRefundWaitSetting(saved, authorizationService.currentActor().id());
                auditEventService.recordSuccess("contract.deposit_refund_wait_configured", DEPOSIT_REFUND_WAIT_MANAGE,
                        "contract_deposit_refund_wait_policy", projectId, null, actual.idempotencyKey(),
                        Map.of("value", value, "min", min, "max", max, "reason", actual.reason() == null ? "" : actual.reason()));
                return saved;
            }, DepositRefundWaitSettingRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("contract.deposit_refund_wait_config_failed", DEPOSIT_REFUND_WAIT_MANAGE,
                    "contract_deposit_refund_wait_policy", request == null ? null : request.projectId(), null,
                    request == null ? null : request.idempotencyKey(), errorCodeOf(exception));
            throw exception;
        }
    }

    /**
     * 第十八批 G 段 82b-R3④／82b-R4（PGM 第 659 行「在所分配的权限内可以查看、处理该角色可查看项目的数据」）：等待期按项目，
     * 项目须在 contract.deposit_refund_wait.manage 可触及范围内（含可跨项目角色勾选的项目）；不带取本人项目，平台管理员须指定项目。
     */
    private String requireWaitProject(String projectId) {
        return authorizationService.requireReachableProject(projectId, DEPOSIT_REFUND_WAIT_MANAGE, AuthorizationService.PROJECT_OUT_OF_REACH);
    }

    // ============ 配置：押金收取月数（HP-C03，全平台统一，仅平台可改） ============
    public DepositMonthsSettingRecord getDepositMonthsSetting() {
        authorizationService.requirePlatformScope();
        authorizationService.requirePermission("contract.deposit_months.manage");
        return repository.depositMonthsSetting();
    }

    /**
     * BSM L1347「商户押金的收取月数全平台统一（业务参数，全平台统一可配，出厂默认三个月，须同时配置上下限；下限不得低于三个月，配置校验拒绝
     * 更低的值，不允许零押金或负押金）」：值与上下限须同时填写，下限 ≥ 3，值落在上下限闭区间内（零/负自然被下限拒绝）。
     */
    @Transactional
    public DepositMonthsSettingRecord saveDepositMonthsSetting(com.complex.platform.contract.dto.DepositMonthsSettingRequest request) {
        try {
            authorizationService.requirePlatformScope();
            authorizationService.requirePermission("contract.deposit_months.manage");
            com.complex.platform.contract.dto.DepositMonthsSettingRequest actual = requireRequest(request);
            Integer value = actual.value();
            Integer min = actual.minValue();
            Integer max = actual.maxValue();
            if (value == null || min == null || max == null || min < 3 || value < min || value > max) {
                throw ApiException.badRequest("VALIDATION_FAILED", "押金收取月数须同时填写参数值及上下限，下限不得低于 3 个月，参数值须在上下限内");
            }
            return idempotencyService.execute("saveContractDepositMonths", actual.idempotencyKey(), actual, () -> {
                DepositMonthsSettingRecord saved = new DepositMonthsSettingRecord(value, min, max);
                repository.saveDepositMonthsSetting(saved, authorizationService.currentActor().id());
                auditEventService.recordSuccess("contract.deposit_months_configured", "contract.deposit_months.manage",
                        "contract_deposit_policy", "global", null, actual.idempotencyKey(),
                        Map.of("value", value, "min", min, "max", max, "reason", actual.reason() == null ? "" : actual.reason()));
                return saved;
            }, DepositMonthsSettingRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("contract.deposit_months_config_failed", "contract.deposit_months.manage",
                    "contract_deposit_policy", "global", null, request == null ? null : request.idempotencyKey(),
                    errorCodeOf(exception));
            throw exception;
        }
    }

    private static String errorCodeOf(RuntimeException exception) {
        return exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR";
    }

    public SpaceExpiryColorRuleRecord saveColorRule(SpaceExpiryColorRuleRequest request) {
        authorizationService.requirePermission("contract.space.color.manage");
        SpaceExpiryColorRuleRequest actual = requireRequest(request);
        int days = actual.thresholdDays() == null ? 0 : actual.thresholdDays();
        if (days <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "提醒天数必须大于 0");
        }
        SpaceExpiryColorRuleRecord saved = repository.saveColorRule(new SpaceExpiryColorRuleRecord(
                "secr_" + UUID.randomUUID(), trimToNull(actual.projectId()),
                requireText(actual.categoryName(), "category_name"), requireText(actual.color(), "color"),
                days, actual.seq() == null ? 0 : actual.seq(), true));
        auditEventService.recordSuccess("contract.color_rule_saved", "contract.space.color.manage",
                "space_expiry_color_rule", saved.id(), null, actual.idempotencyKey(),
                Map.of("threshold_days", days));
        return saved;
    }

    public List<SpaceExpiryColorRuleRecord> listColorRules(String projectId) {
        return repository.listColorRules(trimToNull(projectId));
    }

    /** 铺位平面图数据源：每铺租赁状态 + 套用着色分类（已租按距到期命中最紧一档→即将到期着该色）。 */
    public List<SpaceRentalStatusRecord> spaceRentalStatus(String projectId) {
        String pid = trimToNull(projectId);
        LocalDate asOf = LocalDate.now(LocalZone.ID);
        List<SpaceExpiryColorRuleRecord> rules = repository.listColorRules(pid).stream()
                .sorted((a, b) -> Integer.compare(a.thresholdDays(), b.thresholdDays())).toList();
        List<SpaceRentalStatusRecord> out = new ArrayList<>();
        for (SpaceRentalStatusRecord raw : spaceRentalRaw(pid, asOf)) {
            if (!"let".equals(raw.rentalStatus()) || raw.daysToExpiry() == null) {
                out.add(raw);
                continue;
            }
            SpaceExpiryColorRuleRecord hit = rules.stream()
                    .filter(r -> raw.daysToExpiry() <= r.thresholdDays()).findFirst().orElse(null);
            out.add(hit == null ? raw
                    : new SpaceRentalStatusRecord(raw.spaceId(), raw.spaceName(), "expiring", hit.color(),
                            raw.daysToExpiry()));
        }
        return out;
    }

    /**
     * 第十八批 G 段 82b-R3（PGM 第 659 行「普通岗位没有跨项目查看权」「逐个勾选的只能查看被选中的项目」）：平台管理员照旧（不带＝全平台）；
     * 其余只出 contract.draft.manage 可触及项目的铺位，带 project_id 时取它与可触及项目的交集。
     */
    private List<SpaceRentalStatusRecord> spaceRentalRaw(String pid, LocalDate asOf) {
        var actor = authorizationService.currentActor();
        if (authorizationService.isPlatformAdmin(actor)) {
            return repository.listSpaceRentalRaw(pid, asOf);
        }
        var reach = authorizationService.projectReach(actor, "contract.draft.manage");
        return repository.listSpaceRentalRaw(pid == null ? reach.organizationIds() : reach.covers(pid) ? Set.of(pid) : Set.<String>of(), asOf);
    }

    // ============ 租金 AR 逐期挂账（用 ContractRentMath：base+递增−免租）============
    /** 生效合同某期挂账：算应收 → 联动 ar_ap_items(receivable)+contract_rent_bills。免租/生效前返回 null。 */
    @Transactional
    public ContractRentBillRecord generateRentBill(String contractId, ContractRentBillRequest request) {
        authorizationService.requirePermission("finance.ar_ap.manage");
        ContractRentBillRequest actual = requireRequest(request);
        String period = requireText(actual.period(), "period");
        if (!period.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账期格式不正确，应为 YYYY-MM");
        }
        ContractRecord c = requireContractInReach(contractId, "finance.ar_ap.manage");
        if (!billableForPeriod(c, period)) {
            throw ApiException.conflict("CONTRACT_NOT_EFFECTIVE", "只有生效中的合同才能生成租金账单（已到期合同可出到期日所在月及以前的账期）");
        }
        if (rentBillExistsInChain(c, period, "rent")) {
            throw ApiException.conflict("CONTRACT_RENT_ALREADY_BILLED", "该账期的租金已生成账单：" + period);
        }
        long baseRent = ContractRentMath.periodAmountCent(rentTermsFor(c.rentTerms(), period), c.effectiveDate(), period);
        com.complex.platform.finance.FinanceService.PeriodTurnoverRent rent =
                resolveBillingRent(c, c.rentTerms() == null ? Map.of() : c.rentTerms(), period, baseRent);
        if (rent.amountCent() <= 0) {
            return null;   // 免租期 / 生效前 / 收租方式组合后仍为 0：无应收
        }
        return genFeeBill(c, period, "rent", rent.amountCent(), rent.ruleId(),
                dueDateOf(period), actual.idempotencyKey(), "contract.rent_billed");
    }

    /**
     * 生成当期租金和物业费；独表实际能源费由能源域核实账单后挂账。
     * 各费目幂等(合同,期,费目)；免租/0 金额跳过。
     */
    @Transactional
    public List<ContractRentBillRecord> generatePeriodBills(String contractId, ContractRentBillRequest request) {
        authorizationService.requirePermission("finance.ar_ap.manage");
        ContractRentBillRequest actual = requireRequest(request);
        String period = requireText(actual.period(), "period");
        if (!period.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账期格式不正确，应为 YYYY-MM");
        }
        ContractRecord c = requireContractInReach(contractId, "finance.ar_ap.manage");
        if (!billableForPeriod(c, period)) {
            throw ApiException.conflict("CONTRACT_NOT_EFFECTIVE", "只有生效中的合同才能生成账单（已到期合同可出到期日所在月及以前的账期）");
        }
        return billPeriodFees(c, period, actual.idempotencyKey(), true);
    }

    /**
     * 单期各费目幂等挂账核心（无权限校验，由调用方门控：generatePeriodBills 查 finance.ar_ap.manage，
     * activate 已查 contract.lifecycle.activate）。租金（withRent=false 时不排）+ 物业费 + 车位月租 + 能源费(基非 none)；免租/0/已挂账跳过。
     */
    private List<ContractRentBillRecord> billPeriodFees(ContractRecord c, String period, String idemKey, boolean withRent) {
        Map<String, Object> rt = c.rentTerms() == null ? Map.of() : c.rentTerms();
        LocalDate dueDate = dueDateOf(period);
        List<ContractRentBillRecord> out = new ArrayList<>();
        if (withRent && !rentBillExistsInChain(c, period, "rent")) {
            long baseRent = ContractRentMath.periodAmountCent(rentTermsFor(rt, period), c.effectiveDate(), period);
            com.complex.platform.finance.FinanceService.PeriodTurnoverRent rent =
                    resolveBillingRent(c, rt, period, baseRent);
            ContractRentBillRecord rb = genFeeBill(c, period, "rent", rent.amountCent(), rent.ruleId(), dueDate, idemKey, "contract.fee_billed");
            if (rb != null) {
                out.add(rb);
            }
        }
        long property = numLong(rt.get("propertyFeeCent"));
        ContractRentBillRecord pb = genFeeBill(c, period, "property_fee", property, null, dueDate, idemKey, "contract.fee_billed");
        if (pb != null) {
            out.add(pb);
        }
        // HP-P01（BSM L1437「商户月租车位按合同约定、按月出账，账单类型为「车位月租」」）：合同约定车位月租（分/月）逐期出账，0 不出。
        ContractRentBillRecord parking = genFeeBill(c, period, "parking_monthly", numLong(rt.get("parkingMonthlyCent")), null,
                dueDate, idemKey, "contract.fee_billed");
        if (parking != null) {
            out.add(parking);
        }
        // 清单 P1-3 方案B:能源费(含 by_contract 包干固定额)统一由能耗抄表域 EnergyService.settleEnergyFee 出账,
        // 合同域不再挂能源费,消除合同域(contract_energy_fee)×能耗域(energy_fee)对同一户同期双挂账。
        return out;
    }

    /** 单费目挂账：金额≤0 或已挂账则跳过返 null；否则建 ar_ap_items 应收 + contract_rent_bills。 */
    private ContractRentBillRecord genFeeBill(ContractRecord c, String period, String feeType, long amountCent,
            String turnoverRuleId, LocalDate dueDate, String idemKey, String event) {
        if (amountCent <= 0 || rentBillExistsInChain(c, period, feeType)) {
            return null;
        }
        ContractRentBillRecord bill = repository.insertRentBill(new ContractRentBillRecord(
                "crb_" + UUID.randomUUID(), c.id(), c.merchantId(), c.projectId(), period, feeType, amountCent,
                "arap_" + feeType + "_" + UUID.randomUUID(), Instant.now(), turnoverRuleId), dueDate);
        auditEventService.recordSuccess(event, "finance.ar_ap.manage", "contract_rent_bill",
                bill.id(), null, idemKey, Map.of("period", period, "fee_type", feeType, "amount_cent", amountCent));
        return bill;
    }

    private LocalDate dueDateOf(String period) {
        String[] ym = period.split("-");
        return LocalDate.of(Integer.parseInt(ym[0]), Integer.parseInt(ym[1]), 1);
    }

    /** 按合同收租方式组合月租和提成；使用提成的账单同时锁定本期规则。 */
    private com.complex.platform.finance.FinanceService.PeriodTurnoverRent resolveBillingRent(
            ContractRecord c, Map<String, Object> rt, String period, long baseRent) {
        if (isFixedRent(rt)) {
            return new com.complex.platform.finance.FinanceService.PeriodTurnoverRent(null, baseRent);
        }
        String mode = strVal(rt.get("rentBillingMode"));
        if (turnoverRentPort == null) {
            throw ApiException.conflict("TURNOVER_RENT_UNAVAILABLE", "抽成租金计算服务不可用，暂不能出账");
        }
        com.complex.platform.finance.FinanceService.PeriodTurnoverRent turnover = turnoverAlongChain(c, period);
        long amount = switch (mode) {
            case "turnover_only" -> turnover.amountCent();
            case "higher_of" -> Math.max(baseRent, turnover.amountCent());
            case "sum" -> baseRent + turnover.amountCent();
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "合同收租方式不正确");
        };
        return new com.complex.platform.finance.FinanceService.PeriodTurnoverRent(turnover.ruleId(), amount);
    }

    /**
     * 抽成规则按 contract_id 精确匹配；面积/位置变更子合同 id 终审时才派生、没有自己的规则（第四轮裁决第 2 条重排剩余租期，BSM L1347「保底与抽成取高……
     * 保底加抽成叠加」）：沿变更链先子后父，取第一份有该期唯一规则的合同；全链都没有时照旧 409 TURNOVER_RENT_RULE_PERIOD_UNRESOLVED（不回落固定租金）。
     */
    private com.complex.platform.finance.FinanceService.PeriodTurnoverRent turnoverAlongChain(ContractRecord c, String period) {
        ApiException unresolved = null;
        for (String id : repository.contractChainIds(c)) {
            try {
                return turnoverRentPort.computePeriodTurnoverRentSnapshot(c.merchantId(), id, period);
            } catch (ApiException e) {
                if (!"TURNOVER_RENT_RULE_PERIOD_UNRESOLVED".equals(e.code())) {
                    throw e;
                }
                unresolved = unresolved == null ? e : unresolved;
            }
        }
        throw unresolved;
    }

    /** 每平米生效固化:rentMode=per_sqm 时用绑定铺位登记面积×单价算 baseRentCent,写回 rentTerms 副本;否则原样返回。 */
    private Map<String, Object> resolvePerSqmBaseRent(Map<String, Object> rentTerms, List<String> spaceIds) {
        if (rentTerms == null || !"per_sqm".equals(strVal(rentTerms.get("rentMode")))) {
            return rentTerms;
        }
        long perSqm = numLong(rentTerms.get("perSqmRentCent"));
        if (perSqm <= 0 || platformRepository == null) {
            return rentTerms;
        }
        java.math.BigDecimal totalArea = java.math.BigDecimal.ZERO;
        for (String sid : spaceIds) {
            java.math.BigDecimal a = platformRepository.findSpaceById(sid).map(SpaceRecord::areaSquareMeter).orElse(null);
            if (a != null) {
                totalArea = totalArea.add(a);
            }
        }
        long baseRent = totalArea.multiply(java.math.BigDecimal.valueOf(perSqm))
                .setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        Map<String, Object> out = new HashMap<>(rentTerms);
        out.put("baseRentCent", baseRent);
        out.put("resolvedAreaSqm", totalArea.toPlainString());
        return out;
    }

    private String strVal(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private void rejectFixedEnergyCharge(Map<String, Object> terms) {
        if (terms == null) return;
        for (String key : List.of("energyFeeCent", "energy_fee_cent")) {
            Object value = terms.get(key);
            if (value == null) continue;
            try {
                if (new java.math.BigDecimal(value.toString()).signum() == 0) continue;
            } catch (NumberFormatException ignored) {
                // 未知或无效金额也不能写成合同固定收费。
            }
            throw ApiException.badRequest("VALIDATION_FAILED", "合同不可预设固定能源费，独表须核实实际费用后代收");
        }
    }

    private long numLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    /** 宽松取整数：Number 直取；字符串型数字（序列化差异）解析兜底；否则 0。用于人工录入金额字段。 */
    private long numLongLoose(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        if (o instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    // ============ engine helpers（HP-C01：统一审批模型） ============
    private ContractApprovalTemplateRecord requireTemplate(String projectId, String contractType) {
        return repository.findActiveTemplate(projectId, contractType)
                .orElseThrow(() -> ApiException.conflict("CONTRACT_NO_APPROVAL_FLOW", "未配置生效中的审批流"));
    }

    /**
     * HP-C01：按合同类型取生效模板（项目覆盖优先、否则平台默认），开统一审批复核单，返回复核单号。
     * <ul>
     *   <li>主线最终审批人 = 请求值，否则模板默认（BSM L1349「默认总经理」）；两者皆空 → 400 CONTRACT_FINAL_APPROVER_REQUIRED（fail-closed）；</li>
     *   <li>模板要求的每条平行线都须指定最终审批人（缺 → 400 CONTRACT_APPROVAL_LINE_FINAL_APPROVER_REQUIRED），模板外的线拒绝
     *       （400 CONTRACT_APPROVAL_LINE_NOT_IN_TEMPLATE）——PGM L528「每个部门或业务线须预先设定其平行审批的最低起点层级」由
     *       审批链引擎按 approval_lines 向上补全；</li>
     *   <li>主线逐级补全、档位下限与节点裁决由 SupervisorReviewService/ApprovalChainService 承担；复核单未进入待审即 409
     *       CONTRACT_APPROVAL_REQUIRED（与 BarPayrollRuleService 同范式）。</li>
     * </ul>
     * 到期终止类模板（TYPE_EXPIRY_TERMINATION，actionType=contract.expiry_termination，objectType=contract_settlement）由到期结算开账（settle）发起。
     */
    private String openReview(String contractType, String objectType, String objectId, String projectId,
            String finalApproverId, List<ParallelLine> lines, String reason, String idempotencyKey) {
        if (reviews == null) {
            throw ApiException.conflict("CONTRACT_APPROVAL_UNAVAILABLE", "统一审批服务不可用，合同事项不能发起审批");
        }
        ContractApprovalTemplateRecord template = requireTemplate(projectId, contractType);
        String finalApprover = trimToNull(finalApproverId) != null
                ? finalApproverId.trim() : trimToNull(template.defaultFinalApproverId());
        if (finalApprover == null) {
            throw ApiException.badRequest("CONTRACT_FINAL_APPROVER_REQUIRED",
                    "须指定这条主线的最终审批人（该类合同模板未配置默认最终审批人）");
        }
        Map<String, String> given = new LinkedHashMap<>();
        for (ParallelLine line : lines == null ? List.<ParallelLine>of() : lines) {
            String code = requireText(line == null ? null : line.lineCode(), "parallel_lines.line_code");
            if (!template.requiredLineCodes().contains(code)) {
                throw ApiException.badRequest("CONTRACT_APPROVAL_LINE_NOT_IN_TEMPLATE", "审批线不在该类合同模板内：" + code);
            }
            given.put(code, trimToNull(line.finalApproverId()));
        }
        List<ParallelLine> resolved = new ArrayList<>();
        for (String code : template.requiredLineCodes()) {
            String approver = given.get(code);
            if (approver == null) {
                throw ApiException.badRequest("CONTRACT_APPROVAL_LINE_FINAL_APPROVER_REQUIRED",
                        "须为平行审批线指定该部门的最终审批人：" + code);
            }
            resolved.add(new ParallelLine(code, approver));
        }
        SupervisorReviewDecisionRecord review = reviews.evaluate(new SupervisorReviewEvaluateRequest(
                idempotencyKey + ":review", trimToNull(reason), List.of(), "contract." + contractType, objectType, objectId,
                0L, null, finalApprover, resolved, null));
        if (!"pending_review".equals(review.reviewStatus())) {
            throw ApiException.conflict("CONTRACT_APPROVAL_REQUIRED", "合同事项必须进入已配置的审批流程，不能直接生效");
        }
        // HP-C01-R5（BSM L1349「阶段数与审批岗位按综合体后台可配（业务参数，按综合体可配，须同时配置上下限）」；裁决第 1 条「阶段数＝主线
        // 自动补全后的层级数……发起时校验：补全后层级数不在上下限内即拒绝」）：数审批链引擎补全后的主线节点；模板未配置不校验。
        // 拒绝即随发起事务整笔回滚（复核单、节点、合同/变更状态都不落）。
        if (template.minMainLevels() != null) {
            long levels = reviews.chain(review.reviewId()).stream()
                    .filter(node -> ApprovalChainNode.MAIN_LINE.equals(node.lineCode())).count();
            if (levels < template.minMainLevels() || levels > template.maxMainLevels()) {
                throw ApiException.badRequest("CONTRACT_APPROVAL_LEVELS_OUT_OF_RANGE", "主线自动补全后为 " + levels
                        + " 层，不在该类合同模板配置的主线层级数 " + template.minMainLevels() + "～" + template.maxMainLevels() + " 内");
            }
        }
        return review.reviewId();
    }

    private Map<String, Object> buildSnapshot(ContractRecord c, Integer depositMonths) {
        // 固化 4 项：①租金规则 ②物业费+能源费口径 ③分成/抽成 ④保证金+违约金
        Map<String, Object> snap = new HashMap<>();
        snap.put("rentTerms", c.rentTerms() == null ? Map.of() : c.rentTerms());
        snap.put("energyFeeBasis", c.energyFeeBasis());
        snap.put("commissionMode", c.commissionMode());
        snap.put("depositCent", c.depositCent());
        // HP-C03：押金收取月数随快照固化（BSM L1353「原合同押金月数按原合同快照沿用，不改用当前配置月数」）；本次改动前生效的旧合同为 null。
        snap.put("depositMonths", depositMonths);
        snap.put("penaltyTerms", c.penaltyTerms() == null ? Map.of() : c.penaltyTerms());
        snap.put("frozenAt", Instant.now().toString());
        return snap;
    }

    /** 应用已批变更：可改条款的（面积/租金/经营范围）改 contract 并重固化；续租同一合同延期（HP-C03-4）。退租/提前终止留待 settle。 */
    private Map<String, Object> applyChange(ContractRecord contract, ContractChangeRecord change) {
        Map<String, Object> payload = change.payload() == null ? Map.of() : change.payload();
        ContractRecord updated = contract;
        switch (change.changeType()) {
            case "rent_change", "business_scope_change" -> {
                Map<String, Object> newTerms = new HashMap<>(contract.rentTerms() == null ? Map.of()
                        : contract.rentTerms());
                newTerms.putAll(payload); // payload 覆盖对应条款（租金/经营范围细节）
                // 第五轮裁决第 2 条：租金调整（非纯提成合同）生效日＝终审当日（变更载荷不带生效日）——递增以调整后月租为基数、自生效日起重新计期
                // （ContractRentMath 读 increaseFrom）；每平米计租按合同铺位面积×调整后单价算月租。
                boolean rebillRent = "rent_change".equals(change.changeType()) && !isTurnoverOnly(contract.rentTerms());
                LocalDate rentEffective = LocalDate.now(LocalZone.ID);
                if (rebillRent) {
                    newTerms.put("increaseFrom", rentEffective.toString());
                    newTerms = new HashMap<>(resolvePerSqmBaseRent(newTerms, repository.listContractSpaceIds(contract.id())));
                    newTerms = withPriorRent(newTerms, contract.rentTerms(), YearMonth.from(rentEffective).minusMonths(1));
                }
                updated = repository.saveContract(new ContractRecord(contract.id(), contract.merchantId(),
                        contract.contractSubjectId(), contract.operationSubjectId(), contract.settlementSubjectId(),
                        contract.effectiveDate(), contract.expiryDate(), newTerms, contract.commissionMode(),
                        contract.energyFeeBasis(), contract.depositCent(), contract.penaltyTerms(),
                        contract.settlementRules(), buildSnapshot(withRentTerms(contract, newTerms), snapshotDepositMonths(contract)),
                        contract.shareholderRatioForReport(), contract.attachmentIds(), "effective",
                        contract.projectId(), null, null, contract.renewedFromContractId(), contract.createdAt(),
                        Instant.now(), contract.reviewId()));
                if (rebillRent) {
                    // 「作废自生效月起尚未收款的预排租金应收（留痕），按调整后月租重排剩余租期」：复用面积/位置变更的作废＋重排例程，只作废租金
                    // （裁决只写租金，物业费不动）；同一合同重排（不开子合同），已部分收款的账期不作废、不重排。重放由 onReviewDecided 只认待审变更挡住。
                    YearMonth rebillFrom = YearMonth.from(rentEffective);
                    repository.voidUnpaidRentBills(repository.contractChainIds(updated), rebillFrom.toString(), List.of("rent"),
                            "租金调整 " + change.id() + " 生效重排");
                    scheduleTermBills(updated, rebillFrom, "rent_change:" + change.id(), true);
                }
            }
            case "area_change", "location_change" -> {
                return applyAmendment(contract, change);   // #31：开变更子合同（继承+押金同步），原合同置 amended
            }
            case "scope_grant" -> {
                return applyScopeGrant(contract, change);   // #30：授予该品类临时/永久经营许可
            }
            case "renewal" -> {
                return applyRenewal(contract, change);   // HP-C03-4：同一合同延期
            }
            default -> { /* move_out / early_termination：实际终止走 settle */ }
        }
        return updated.snapshot();
    }

    /**
     * #30 授予经营许可：permanent → 追加到 rent_terms.allowedCategories；temporary → 追加到 rent_terms.scopeGrants
     * ({category, validUntil}，guard 读未过期者放行)。仅动经营范围键，不改租金/押金等条款。
     */
    private Map<String, Object> applyScopeGrant(ContractRecord contract, ContractChangeRecord change) {
        Map<String, Object> payload = change.payload() == null ? Map.of() : change.payload();
        String category = strVal(payload.get("requestedCategory"));
        Map<String, Object> newTerms = new HashMap<>(contract.rentTerms() == null ? Map.of() : contract.rentTerms());
        if ("temporary".equals(strVal(payload.get("grantType")))) {
            List<Map<String, Object>> grants = new ArrayList<>();
            if (newTerms.get("scopeGrants") instanceof Collection<?> values) {
                for (Object v : values) {
                    if (v instanceof Map<?, ?> m) {
                        Map<String, Object> copy = new LinkedHashMap<>();
                        m.forEach((k, val) -> copy.put(String.valueOf(k), val));
                        grants.add(copy);
                    }
                }
            }
            Map<String, Object> grant = new LinkedHashMap<>();
            grant.put("category", category);
            grant.put("validUntil", strVal(payload.get("validUntil")));
            grants.add(grant);
            newTerms.put("scopeGrants", grants);
        } else {
            List<String> allowed = new ArrayList<>();
            if (newTerms.get(ContractScopeGuard.RENT_TERMS_KEY) instanceof Collection<?> values) {
                values.forEach(v -> allowed.add(String.valueOf(v)));
            }
            if (!allowed.contains(category)) {
                allowed.add(category);
            }
            newTerms.put(ContractScopeGuard.RENT_TERMS_KEY, allowed);
        }
        return repository.saveContract(withRentTerms(contract, newTerms)).snapshot();
    }

    /**
     * #31 变更子合同（仅经营位置/面积变更）：基于原合同派生子合同，继承承租主体/收费口径等关键条款、记录 parentContractId，
     * HP-C03-1/2（BSM L1353）：目标押金＝原合同快照押金月数×变更后月租（每平米按子合同铺位面积重算），子合同押金＝目标；差额≠0 登记应收（补缴）
     * 或应付（退还）进应收应付台账、到期日＝办理期限，rent_terms.depositAdjustment 写明目标/差额/方向/期限/台账项；已收押金余额等实际收退才更新。
     * 原合同置 amended（退出计费/到期作业，保持商户单一生效合同不变量）。子合同直接 effective（变更已过审批链）。
     * 第三轮裁决第 3 条：子合同租金递增自变更生效日起重新计期；第 1 条：纯提成合同押金沿用约定金额、不登记差额。
     * 第四轮裁决第 2 条「变更生效时重排」：作废父合同（及变更链上的前序合同）自变更生效月起整笔未收的预排租金/物业费应收（应收台账沿用 V344 红冲追加冲正行、
     * 原行不动——BSM L1353「不将未来应收视为到账，也不覆盖原收款历史」；账单记作废时刻与原因〔含变更 id〕），再按子合同新月租与重新计期的递增为剩余租期重排；
     * 已部分收款的账期不作废、不重排（链去重仍认父账单）；纯提成合同无月租，只重排物业费。
     * 幂等：子合同 id 由变更 id 确定性派生，重放即返回已派生子合同、不重复作废/重排；作废只认未作废账单，重排逐期链去重。
     */
    private Map<String, Object> applyAmendment(ContractRecord parent, ContractChangeRecord change) {
        String childId = "ctr_" + deterministicSuffix(change.id());
        ContractRecord existingChild = repository.findContractById(childId).orElse(null);
        if (existingChild != null) {
            return existingChild.snapshot();   // 已派生（重复审批/重放）：不重开子合同
        }
        Map<String, Object> payload = change.payload() == null ? Map.of() : change.payload();
        Map<String, Object> childTerms = new HashMap<>(parent.rentTerms() == null ? Map.of() : parent.rentTerms());
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            if (!AMENDMENT_CONTROL_KEYS.contains(e.getKey())) {
                childTerms.put(e.getKey(), e.getValue());   // 变更条款（面积/每平米单价等）并入子合同
            }
        }
        childTerms.put("parentContractId", parent.id());
        childTerms.put("amendmentType", change.changeType());
        childTerms.remove("depositAdjustment");   // 清掉从父合同继承来的上一轮押金补退记录，本轮按新差额重算
        // 第三轮裁决第 3 条：变更录入的月租即新基数，原递增条款的周期与比例沿用、自变更生效日（终审当日）起重新计期（ContractRentMath 读 increaseFrom）。
        LocalDate changeEffective = LocalDate.now(LocalZone.ID);
        childTerms.put("increaseFrom", changeEffective.toString());
        childTerms = withPriorRent(childTerms, parent.rentTerms(), YearMonth.from(changeEffective).minusMonths(1));
        // 第三轮裁决第 1 条：纯提成合同押金沿用原合同约定金额，不按公式重算、不登记差额。
        boolean agreedDeposit = isTurnoverOnly(parent.rentTerms());
        Integer months = agreedDeposit ? null : requireDepositMonths(parent);
        List<String> childSpaces = amendmentSpaces(parent, payload);
        childTerms = resolvePerSqmBaseRent(childTerms, childSpaces);   // 每平米：子合同铺位面积×单价重算月租；否则用载荷 baseRentCent
        Long target = agreedDeposit ? parent.depositCent() : Long.valueOf(depositFor(childTerms, months));
        Map<String, Object> adj = agreedDeposit ? null
                : registerDepositDelta(parent, childId, change.id(), target, strVal(payload.get("depositDeadline")));
        if (adj != null) {
            childTerms.put("depositAdjustment", adj);
        }
        Instant now = Instant.now();
        ContractRecord draft = new ContractRecord(childId, parent.merchantId(), parent.contractSubjectId(),
                parent.operationSubjectId(), parent.settlementSubjectId(), parent.effectiveDate(),
                parent.expiryDate(), childTerms, parent.commissionMode(), parent.energyFeeBasis(), target,
                parent.penaltyTerms(), parent.settlementRules(), null, parent.shareholderRatioForReport(),
                parent.attachmentIds(), "effective", parent.projectId(), null, null,
                parent.renewedFromContractId(), now, now);
        ContractRecord child = repository.saveContract(withSnapshot(draft, buildSnapshot(draft, months)));
        repository.saveContract(withStatus(parent, "amended"));   // 先于绑铺位，使铺位占用早检排除原合同
        for (String spaceId : childSpaces) {
            if (repository.hasOverlappingEffectiveContractForSpace(spaceId, child.effectiveDate(),
                    child.expiryDate(), child.id())) {
                throw ApiException.conflict("CONTRACT_SPACE_CONFLICT",
                        "该铺位已被生效期重叠的合同占用：" + spaceId);
            }
        }
        repository.bindContractSpaces(child.id(), childSpaces, child.projectId());
        // 第四轮裁决第 2 条：变更生效月起作废父链整笔未收的预排账单，再为子合同重排剩余租期（纯提成合同 agreedDeposit 只作废/重排物业费）。
        YearMonth rebillFrom = YearMonth.from(changeEffective);
        List<String> voided = repository.voidUnpaidRentBills(repository.contractChainIds(parent), rebillFrom.toString(),
                agreedDeposit ? List.of("property_fee") : List.of("rent", "property_fee"),
                "面积/位置变更 " + change.id() + " 生效重排");
        int billed = scheduleTermBills(child, rebillFrom, "amendment:" + change.id(), !agreedDeposit);
        auditEventService.recordSuccess("contract.amended", "contract.change.manage", "contract", child.id(),
                null, change.id(), Map.of("parent_contract_id", parent.id(),
                        "amendment_type", change.changeType(), "deposit_delta_cent", adj == null ? 0L : adj.get("deltaCent"),
                        "space_count", childSpaces.size(), "voided_bills", voided.size(), "rent_bills", billed));
        return child.snapshot();
    }

    /**
     * HP-C03-4 续租＝同一合同延期（2026-10-18 裁决第 4 条「终审通过时延长到期日、换月租、重固化快照、状态回「生效」、补排延长期账单，并按公式登记押金差额」）：
     * 月租换为续租后月租（每平米计租＝续租后单价×合同铺位面积，经 resolvePerSqmBaseRent）；目标押金＝原合同快照押金月数×续租后月租（BSM L1353），差额经
     * {@link #registerDepositDelta} 与面积/位置变更同一例程登记（应收/应退，到期日＝办理期限）；快照重固化、押金月数沿用；状态回生效；自原到期月起补排至新到期月。
     * 第三轮裁决第 3 条：租金递增以续租后月租为基数、自续租生效日（原到期日次日）起重新计期；第 1 条：纯提成合同押金沿用约定金额、不登记差额。
     * 第三轮裁决第 5、6 条：续租都在原租期内完成终审，原租期内发起续租合同保持生效中、终审通过即延期。
     * 延长期铺位须未被别的生效合同占用（「绑定铺位（防止同一铺位重复出租）」，BSM L1350），冲突 409 随终审整笔回滚。
     * 幂等：终审事件只认待审变更（onReviewDecided），差额登记以变更 id 为幂等键，排账逐期去重。
     */
    private Map<String, Object> applyRenewal(ContractRecord contract, ContractChangeRecord change) {
        Map<String, Object> payload = change.payload();
        LocalDate newExpiry = LocalDate.parse(strVal(payload.get("newExpiryDate")));
        for (String spaceId : repository.listContractSpaceIds(contract.id())) {
            if (repository.hasOverlappingEffectiveContractForSpace(spaceId, contract.effectiveDate(), newExpiry, contract.id())) {
                throw ApiException.conflict("CONTRACT_SPACE_CONFLICT", "该铺位已被生效期重叠的合同占用：" + spaceId);
            }
        }
        boolean agreedDeposit = isTurnoverOnly(contract.rentTerms());
        Integer months = agreedDeposit ? null : requireDepositMonths(contract);
        Map<String, Object> terms = new HashMap<>(contract.rentTerms() == null ? Map.of() : contract.rentTerms());
        terms.remove("depositAdjustment");   // 上一轮押金补退记录不沿用，本轮按新差额重算
        terms.put("increaseFrom", contract.expiryDate().plusDays(1).toString());   // 续租生效日起重新计递增期（ContractRentMath）
        if (!agreedDeposit) {   // 纯提成合同没有月租可换
            if (isPerSqm(terms)) {
                terms.put("perSqmRentCent", payload.get("perSqmRentCent"));
                terms = resolvePerSqmBaseRent(terms, repository.listContractSpaceIds(contract.id()));
            } else {
                terms.put("baseRentCent", payload.get("baseRentCent"));
            }
        }
        terms = withPriorRent(terms, contract.rentTerms(), YearMonth.from(contract.expiryDate()));
        Long target = agreedDeposit ? contract.depositCent() : Long.valueOf(depositFor(terms, months));
        Map<String, Object> adj = agreedDeposit ? null : registerDepositDelta(contract, contract.id(), change.id(), target,
                strVal(payload.get("depositDeadline")));
        if (adj != null) {
            terms.put("depositAdjustment", adj);
        }
        ContractRecord draft = new ContractRecord(contract.id(), contract.merchantId(), contract.contractSubjectId(),
                contract.operationSubjectId(), contract.settlementSubjectId(), contract.effectiveDate(),
                newExpiry, terms, contract.commissionMode(),
                contract.energyFeeBasis(), target, contract.penaltyTerms(), contract.settlementRules(), null,
                contract.shareholderRatioForReport(), contract.attachmentIds(), "effective", contract.projectId(),
                contract.terminatedAt(), contract.terminationReason(), contract.renewedFromContractId(), contract.createdAt(),
                Instant.now(), contract.reviewId());
        ContractRecord renewed = repository.saveContract(withSnapshot(draft, buildSnapshot(draft, months)));
        int billed = scheduleTermBills(renewed, YearMonth.from(contract.expiryDate()), "renewal:" + change.id(), true);
        auditEventService.recordSuccess("contract.renewed", "contract.change.manage", "contract", contract.id(),
                null, change.id(), Map.of("expiry_date", renewed.expiryDate().toString(),
                        "deposit_delta_cent", adj == null ? 0L : adj.get("deltaCent"), "rent_bills", billed));
        return renewed.snapshot();
    }

    /**
     * HP-C03（BSM L1353「目标押金＝原合同押金月数×变更或续租后的月租；子合同写明目标金额、补退差额及办理期限；批准生效后登记应收或应退」）：面积/位置变更子合同与
     * 续租共用——差额＝目标−原合同押金，≠0 时登记应收（补缴）/应付（退还）进应收应付台账、到期日＝办理期限（幂等键＝变更 id），返回写进 rent_terms 的
     * depositAdjustment（目标/差额/方向/期限/台账项）；差额 0 返回 null、不登记。已收押金余额等实际收退才更新。
     */
    private Map<String, Object> registerDepositDelta(ContractRecord original, String contractId, String changeId, long target,
            String deadline) {
        long delta = target - (original.depositCent() == null ? 0L : original.depositCent());
        if (delta == 0) {
            return null;
        }
        com.complex.platform.finance.ArApItemRecord item = terminationSettlementPort.registerDepositAdjustment(
                original.merchantId(), contractId, changeId, delta, LocalDate.parse(deadline));
        Map<String, Object> adj = new LinkedHashMap<>();
        adj.put("targetDepositCent", target);
        adj.put("deltaCent", delta);
        adj.put("direction", delta > 0 ? "collect" : "refund");   // 补缴 vs 退还
        adj.put("deadline", deadline);
        adj.put("arApItemId", item.id());   // 未结差额经应收应付台账（未收/未付）跟进
        adj.put("itemNo", item.itemNo());
        return adj;
    }

    /** 生效快照里固化的押金收取月数；本次改动前生效的旧合同快照无此键＝null。 */
    private static Integer snapshotDepositMonths(ContractRecord c) {
        return c.snapshot() != null && c.snapshot().get("depositMonths") instanceof Number n ? n.intValue() : null;
    }

    /** HP-C03（BSM L1353「原合同押金月数按原合同快照沿用」）：快照缺押金月数的旧合同拒绝并提示，不推算。 */
    private static int requireDepositMonths(ContractRecord c) {
        Integer months = snapshotDepositMonths(c);
        if (months == null) {
            throw ApiException.conflict("CONTRACT_DEPOSIT_MONTHS_MISSING",
                    "原合同生效快照未记录押金月数（押金月数规则上线前生效），系统不推算，暂不能按公式办理面积/位置变更押金");
        }
        return months;
    }

    /** HP-C03（BSM L1347「押金金额 ＝ 收取月数 × 该商户签约时的月租金（只乘月租……）」）：月数 × rent_terms.baseRentCent。 */
    private long depositFor(Map<String, Object> rentTerms, int months) {
        return months * numLong(rentTerms == null ? null : rentTerms.get("baseRentCent"));
    }

    /** 第三轮裁决第 1 条「纯提成合同（固定比例、阶梯比例，没有月租）」：收租方式纯提成（rent_terms.rentBillingMode=turnover_only，出账只按营业额提成）。 */
    private boolean isTurnoverOnly(Map<String, Object> rentTerms) {
        return rentTerms != null && "turnover_only".equals(strVal(rentTerms.get("rentBillingMode")));
    }

    /** 收租方式为空或 fixed_only 即固定租金（出账只按月租）；其余 turnover_only／higher_of／sum 为营业额抽成类，138-R5 起不预排租金。 */
    private boolean isFixedRent(Map<String, Object> rentTerms) {
        String mode = rentTerms == null ? null : strVal(rentTerms.get("rentBillingMode"));
        return mode == null || "fixed_only".equals(mode);
    }

    /**
     * 第十八批 Q 段第 138 条 138-R5 复核修正（ENUM:257 营业额抽成账单在该账期月结定格之后出账；ENUM:252「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算
     * 最后审批通过」；第七批合同裁决第 2 条「合同到期走到期日所在月」）：营业额抽成类合同的最后一期只能在到期、月结定格后出，这时到期作业已把合同转为已到期——
     * 已到期合同放行到期日所在月及以前的账期出账，其余非生效状态照旧 409。
     */
    private static boolean billableForPeriod(ContractRecord c, String period) {
        return "effective".equals(c.lifecycleStatus()) || ("expired".equals(c.lifecycleStatus())
                && !YearMonth.parse(period).isAfter(YearMonth.from(c.expiryDate())));
    }

    /**
     * 第十八批 Q 段第 138 条 138-R5 复核修正（第七批合同裁决第 4 条所引 ENUM L255「续租审批未完成期间按原合同继续出账」；第 10 条「从续租/变更生效日起重新计期」；
     * 第 16 条「作废父合同自变更生效月起……按子合同新月租和重新计期的递增重排剩余租期」；第 20 条「作废自生效月起……按调整后月租重排剩余租期」）：
     * 保底取高、叠加合同不预排租金，续租、租金调整、面积/位置变更终审换月租前，把原月租条款（ContractRentMath 读的键）
     * 连同适用末月 through 记进 rent_terms.priorRentTerms（按末月升序），through 及以前的账期出账仍按原月租；已记的段末月不早于 through 的截到 through、其后的段不再适用。
     */
    private Map<String, Object> withPriorRent(Map<String, Object> newTerms, Map<String, Object> oldTerms, YearMonth through) {
        if (oldTerms == null || isFixedRent(oldTerms) || isTurnoverOnly(oldTerms)) {
            return newTerms;   // 固定租金合同变更前各期已预排；纯提成合同没有月租
        }
        String end = through.toString();
        List<Map<String, Object>> segments = new ArrayList<>();
        Map<String, Object> out = new HashMap<>(newTerms);
        out.put(PRIOR_RENT_KEY, segments);
        for (Map<String, Object> seg : priorRentSegments(oldTerms)) {
            Map<String, Object> kept = new LinkedHashMap<>(seg);
            segments.add(kept);
            if (end.compareTo(String.valueOf(seg.get("throughPeriod"))) <= 0) {
                kept.put("throughPeriod", end);   // 原月租还没轮到就被这次终审取代
                return out;
            }
        }
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("throughPeriod", end);
        RENT_MATH_KEYS.stream().filter(oldTerms::containsKey).forEach(k -> current.put(k, oldTerms.get(k)));
        segments.add(current);
        return out;
    }

    /** 该账期实际适用的月租条款：priorRentTerms 中第一段末月不早于该期的，否则为当前条款。 */
    private static Map<String, Object> rentTermsFor(Map<String, Object> rentTerms, String period) {
        for (Map<String, Object> seg : priorRentSegments(rentTerms)) {
            if (period.compareTo(String.valueOf(seg.get("throughPeriod"))) <= 0) {
                return seg;
            }
        }
        return rentTerms;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> priorRentSegments(Map<String, Object> rentTerms) {
        return rentTerms != null && rentTerms.get(PRIOR_RENT_KEY) instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    private boolean isPerSqm(Map<String, Object> rentTerms) {
        return rentTerms != null && "per_sqm".equals(strVal(rentTerms.get("rentMode")));
    }

    /** 子合同铺位集：优先变更载荷 spaceIds（新经营位置），否则继承原合同现有铺位（仅面积变更）。 */
    private List<String> amendmentSpaces(ContractRecord parent, Map<String, Object> payload) {
        if (payload.get("spaceIds") instanceof Collection<?> values && !values.isEmpty()) {
            List<String> out = new ArrayList<>();
            for (Object v : values) {
                String space = trimToNull(String.valueOf(v));
                if (space != null && !out.contains(space)) {
                    out.add(space);
                }
            }
            return out;
        }
        return repository.listContractSpaceIds(parent.id());
    }

    /** #31 变更子合同追溯：列某合同派生的变更子合同（rent_terms.parentContractId 指向它）。 */
    public List<ContractRecord> listChildContracts(String parentContractId) {
        authorizationService.requirePermission("contract.change.manage");
        String pid = requireText(parentContractId, "parent_contract_id");
        requireContractInReach(pid, "contract.change.manage");   // 与其它端点对齐：先证合同存在且在可触及项目内再查子合同
        return repository.listChildContracts(pid);
    }

    /**
     * 未清应收沿变更子合同链汇总：子合同退租结算把变更前挂在父/祖合同的欠租也纳入押金抵扣安全网(P2-1)。
     * HP-C02-R6：只计账期不晚于截止月 through 的应收（null＝退租日未定，不截止）。
     */
    private long sumUnpaidArOverChain(ContractRecord c, YearMonth through) {
        long sum = 0L;
        for (String id : repository.contractChainIds(c)) {
            sum += repository.sumUnpaidArForContract(id, through);
        }
        return sum;
    }

    /** 账单去重跨变更子合同链：父合同已出账的月份，子合同不重开(P2-1 防双开账单)。链 id 走仓储 default（HP-C02 齐单核对共用）。 */
    private boolean rentBillExistsInChain(ContractRecord c, String period, String feeType) {
        for (String id : repository.contractChainIds(c)) {
            if (repository.rentBillExists(id, period, feeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 手动结算开账/裁决后重算结算清单：与自动退租统一走 TerminationSettlementMath——
     * 押金按台账实收余额封顶后抵未清应收、净额&gt;0 应退、提前退租违约金取合同条款。纯算不落账。
     */
    private List<Map<String, Object>> computeTerminationItems(ContractRecord contract, String trigger, YearMonth through) {
        return new ArrayList<>(computeTerminationResult(contract, trigger, through).items());
    }

    /** 现算退租结算（押金台账余额封顶 → 抵截止月 through 前未清应收 → 净额；提前退租违约金取合同条款）。开账与确认均调此，同口径。 */
    private TerminationSettlementMath.Result computeTerminationResult(ContractRecord contract, String trigger, YearMonth through) {
        long rawDeposit = contract.depositCent() == null ? 0L : contract.depositCent();
        long deposit = terminationSettlementPort == null ? rawDeposit
                : terminationSettlementPort.effectiveRefundableDeposit(contract.merchantId(), rawDeposit);
        long unpaid = sumUnpaidArOverChain(contract, through);
        long penaltyCent = 0L;
        String penaltyReason = null;
        if ("early_termination".equals(trigger)) {
            penaltyCent = numLongLoose(contract.penaltyTerms() == null ? null : contract.penaltyTerms().get("amountCent"));
            if (penaltyCent > 0) {
                penaltyReason = "合同条款违约金";
            }
        }
        return TerminationSettlementMath.compute(trigger, deposit, unpaid, penaltyCent, penaltyReason);
    }

    /**
     * 到期终止复核单终审通过 → 落账（退租/提前终止走 settleTerminationIfNeeded）。
     * 跨路径去重（资金安全核心）：该合同已有已落账单 → 本单作废（voided）绝不重复退押金；否则过最后审批闸（齐单核对＋非争议，
     * {@link #requireSettlementReadyForFinalApproval}，抛出即随 approve 整笔回滚）→ 按最新欠费现算 → 落 ar_ap+deposit_ledger（端口幂等键=本结算单 id）
     * → 到期合同 → 按押金够不够分流（押金抵扣中/欠费追缴中/已结清，第三轮裁决第 4 条）；押金退还等待期自本次最后审批完成时刻起算（HP-C06）。
     */
    private ContractSettlementRecord finalizeSettlementWithLedger(ContractSettlementRecord raised, String reviewId) {
        // S-2 资金路径窄竞态：对合同行 FOR UPDATE，串行化同一合同的并发落账；第三轮并发约定：锁后重读结算单状态（等锁期间商户异议/已落账按锁后状态判定）。
        ContractRecord contract = lockContract(raised.contractId());
        ContractSettlementRecord settlement = repository.findSettlementById(raised.id()).orElseThrow();
        if (!LIVE_SETTLEMENT_STATUSES.contains(settlement.status())) {
            return settlement;
        }
        String trigger = settlement.triggerType();
        Instant now = Instant.now();
        boolean alreadyPosted = repository.listSettlementsByContract(contract.id()).stream()
                .anyMatch(s -> !s.id().equals(settlement.id()) && POSTED_SETTLEMENT_STATUSES.contains(s.status()));
        if (alreadyPosted) {
            ContractSettlementRecord voided = repository.saveSettlement(
                    settlement.withStatus("voided", settlement.settlementItems(), now));
            auditEventService.recordSuccess("contract.settlement_voided", "governance.supervisor_review.approve",
                    "contract_settlement", settlement.id(), null, reviewId,
                    Map.of("trigger", trigger, "reason", "already_settled"));
            return voided;
        }
        YearMonth through = documentsThroughMonth(contract, trigger);
        requireSettlementReadyForFinalApproval(contract, settlement, through);
        // I-4 口径统一：按最新 unpaid 现算（与退租自动落账同口径），不用开账快照——开账到终审间新增欠费也被押金优先抵扣。
        TerminationSettlementMath.Result result = computeTerminationResult(contract, trigger, through);
        List<Map<String, Object>> items = new ArrayList<>(result.items());
        long[] posted = postTerminationLedger(contract, settlement.id(), items, result, through, depositRefundDue(contract, now, now));
        annotateThroughMonth(items, through);
        ContractSettlementRecord settled = repository.saveSettlement(settlement.withStatus(postedStatus(posted[0], result), items, now));
        finalizeContractAfterSettlement(contract, trigger, now);
        auditEventService.recordSuccess("contract.settlement_completed", "governance.supervisor_review.approve",
                "contract_settlement", settlement.id(), null, reviewId,
                Map.of("trigger", trigger, "deposit_refund_cent", posted[0], "penalty_cent", posted[1],
                        "ledger_posted", terminationSettlementPort != null));
        return settled;
    }

    /**
     * 落退租结算财务台账（到期终止终审 / 退租变更终审共用），实退值/台账项 id 回写 items 留痕。返回 [实退押金, 违约金]。端口空（旧构造/未接 finance）=降级只留快照不落账。
     * ① 违约金&gt;0 挂应收；② 押金抵扣（第四轮裁决第 3 条，财务结算 PRD L957「终止结算既有多级审批全部通过后，系统按清单自动落账，使相应应收未付额与押金余额
     * 等额减少并可对账」）：截止月 through 前未清应收（与 sumUnpaidArOverChain 同口径，变更前父合同在前、各自先到期先抵）→ 违约金应收，逐笔以「押金抵扣」销账至
     * 抵扣额、写一条 deposit_ledger 抵扣行，逐笔销账回写押金处置项 offsets（对账）；③ 押金退还净额（＝扣减后余额）&gt;0 挂应付＋写 refund 行。
     */
    private long[] postTerminationLedger(ContractRecord contract, String settlementId, List<Map<String, Object>> items,
            TerminationSettlementMath.Result result, YearMonth through, RefundDue due) {
        if (terminationSettlementPort == null) {
            return new long[]{0L, 0L};
        }
        String merchantId = contract.merchantId();
        String penaltyArId = null;
        if (result.penaltyCent() > 0) {
            penaltyArId = terminationSettlementPort.createTerminationPenaltyReceivable(merchantId, result.penaltyCent(), settlementId);
            annotateSettlementItem(items, "termination_penalty", penaltyArId);
        }
        if (result.offsetCent() > 0) {
            List<String> chain = new ArrayList<>(repository.contractChainIds(contract));
            java.util.Collections.reverse(chain);
            List<String> covered = new ArrayList<>();
            for (String id : chain) {
                covered.addAll(repository.listUnpaidArIdsForContract(id, through));
            }
            if (penaltyArId != null) {
                covered.add(penaltyArId);
            }
            annotateDepositOffsets(items,
                    terminationSettlementPort.offsetReceivablesWithDeposit(merchantId, settlementId, covered, result.offsetCent()));
        }
        long actualRefund = 0L;
        if (result.depositRefundCent() > 0) {
            actualRefund = terminationSettlementPort.createDepositRefundPayable(
                    merchantId, result.depositRefundCent(), settlementId, due.dueDate());
            recordActualDepositRefund(items, actualRefund, result.depositRefundCent());
            if (actualRefund > 0) {
                annotateDepositRefundDue(items, due);
            }
        }
        return new long[]{actualRefund, result.penaltyCent()};
    }

    /** 第四轮裁决第 3 条「可对账」：押金处置项回写每笔押金抵扣销账的台账项 id 与金额。 */
    private static void annotateDepositOffsets(List<Map<String, Object>> items,
            List<com.complex.platform.finance.TerminationSettlementPort.DepositOffset> offsets) {
        List<Map<String, Object>> rows = offsets.stream()
                .map(o -> Map.<String, Object>of("arApItemId", o.arApItemId(), "amountCent", o.amountCent())).toList();
        for (Map<String, Object> item : items) {
            if ("deposit_disposal".equals(item.get("item"))) {
                item.put("offsets", rows);
            }
        }
    }

    /**
     * 退租变更（move_out/到期退租、early_termination/提前退租）复核单「全部通过」→ 自动生成退租结算并落财务台账。
     * ① 未清应收清算（只读汇总，不重复挂账）② 押金处置（净额&gt;0 挂应付、≤ 0 只列示）③ 提前退租违约金挂应收。
     * 幂等：发起时开的结算单 id 由变更 id 确定性派生，已落账/作废即跳过（防重复审批/重放双挂），端口幂等键取结算单 id。
     * HP-C02-R2：落账前过最后审批闸（{@link #requireSettlementReadyForFinalApproval}），落账沿用合同在办的那张结算单。
     */
    private void settleTerminationIfNeeded(ContractRecord contract, ContractChangeRecord change, Instant approvedAt) {
        String trigger = change.changeType();
        if (!"move_out".equals(trigger) && !"early_termination".equals(trigger)) {
            return;
        }
        // 第三轮并发约定：先锁合同行再读合同/结算单状态（与商户异议/不接受裁决、主管裁决串行）。
        contract = lockContract(contract.id());
        // I2 双花守卫：合同已非生效（另一退租变更已结算并终止）→ 跳过，绝不重复退押金/结算。
        // onReviewDecided 每次重取 contract，B 审批时读到 terminated 即在此返回。
        if (!"effective".equals(contract.lifecycleStatus())) {
            return;
        }
        // HP-C02：发起时开的待结算单已落账/作废（重复审批/重放）→ 不重复落账。
        ContractSettlementRecord raised = repository.findSettlementById("cst_" + deterministicSuffix(change.id())).orElse(null);
        if (raised != null && !LIVE_SETTLEMENT_STATUSES.contains(raised.status())) {
            return;
        }
        // HP-C02-R2：在办单＝发起时开的那张，否则更早手动开账/争议挂起的那张；齐单核对事实随单保留，落账沿用其 id。
        ContractSettlementRecord live = raised != null ? raised : findLiveSettlement(contract.id());
        YearMonth through = moveOutMonth(change);   // HP-C02-R6：齐单闸与欠费口径同按退租日所在月
        requireSettlementReadyForFinalApproval(contract, live, through);
        String settlementId = live.id();
        // I3/#3② 押金按台账实收余额封顶（用户裁决）：只退商户实际交纳的，不超退合同固化额。
        // 退押金落账时写 deposit_ledger refund 行扣减余额（见 FinanceService.createDepositRefundPayable），
        // 故同一商户多合同退租按台账余额消费顺序封顶——第二个合同读到已扣减余额，杜绝叠加超退。
        // 端口未接=按 0（I1 修好后生产不该发生）。
        long rawDeposit = contract.depositCent() == null ? 0L : contract.depositCent();
        long deposit = terminationSettlementPort == null ? 0L
                : terminationSettlementPort.effectiveRefundableDeposit(contract.merchantId(), rawDeposit);
        long unpaid = sumUnpaidArOverChain(contract, through);
        // 违约金仅提前退租：合同条款 penaltyTerms.amountCent 优先；缺则取变更载荷人工录入 penaltyAmountCent+penaltyReason。
        Map<String, Object> payload = change.payload() == null ? Map.of() : change.payload();
        long penaltyCent = 0L;
        String penaltyReason = null;
        if ("early_termination".equals(trigger)) {
            long termPenalty = numLongLoose(contract.penaltyTerms() == null ? null : contract.penaltyTerms().get("amountCent"));
            if (termPenalty > 0) {
                penaltyCent = termPenalty;
                penaltyReason = "合同条款违约金";
            } else {
                penaltyCent = numLongLoose(payload.get("penaltyAmountCent"));
                penaltyReason = trimToNull(strVal(payload.get("penaltyReason")));
            }
        }
        TerminationSettlementMath.Result result =
                TerminationSettlementMath.compute(trigger, deposit, unpaid, penaltyCent, penaltyReason);
        List<Map<String, Object>> items = new ArrayList<>(result.items());
        // 落财务台账（端口空=旧构造/未接 finance：降级只留结算快照不落账）——与到期终止终审路径共用同一落账逻辑。
        Instant now = Instant.now();
        long[] posted = postTerminationLedger(contract, settlementId, items, result, through, depositRefundDue(contract, approvedAt, now));
        annotateThroughMonth(items, through);
        repository.saveSettlement(live.withStatus(postedStatus(posted[0], result), items, now));
        finalizeContractAfterSettlement(contract, trigger, now);
        // I1c 审计只记实际落账金额（端口未落账则为 0）+ 落账标志，杜绝"已结算 X 元"而台账无痕的误导。
        auditEventService.recordSuccess("contract.termination_settled", "contract.change.manage",
                "contract_settlement", settlementId, null, change.id(),
                Map.of("trigger", trigger, "deposit_refund_cent", posted[0],
                        "penalty_cent", posted[1], "unpaid_cent", result.unpaidCent(),
                        "ledger_posted", terminationSettlementPort != null));
    }

    /** 第三轮并发约定：锁合同行（FOR UPDATE）——商户异议/不接受裁决、主管裁决、终审落账同序先锁合同行再读结算单状态，互不覆盖。 */
    private ContractRecord lockContract(String contractId) {
        return repository.findContractByIdForUpdate(contractId)
                .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "合同不存在或已被删除"));
    }

    /**
     * 第三轮裁决第 4 条「按押金够不够分流」：押金抵未清应收与违约金后有余额、实际挂了押金退还应付 → 押金抵扣中；押金不足抵扣（商户仍欠差额）→ 欠费追缴中；
     * 正好抵完、无退无欠 → 已结清。违约金并入押金抵扣基数（BSM L1347「只有清退时押金才能抵扣欠费与违约金」，TerminationSettlementMath）。
     */
    private static String postedStatus(long postedRefundCent, TerminationSettlementMath.Result result) {
        if (postedRefundCent > 0) {
            return "deposit_offsetting";
        }
        return result.merchantStillOwesCent() > 0 ? "arrears_collecting" : "settled";
    }

    /** 未清应收汇总项写落账截止月：欠费追缴中按同一截止月复核差额是否收齐。 */
    private static void annotateThroughMonth(List<Map<String, Object>> items, YearMonth through) {
        for (Map<String, Object> item : items) {
            if ("unpaid_receivable_summary".equals(item.get("item"))) {
                item.put("throughMonth", through.toString());
            }
        }
    }

    /**
     * 第三轮裁决第 4 条「押金退还付清后→「已结清」」「差额收齐后→「已结清」」（TerminationSettlementProgressPort：FinanceService.settleArApItem
     * 每登记一笔收付款、补录调整审批通过即在同一事务内回调）：押金退还应付（source_type=deposit_refund、来源＝结算单）付清 → 该押金抵扣中的单已结清；
     * 商户的应收每收一笔 → 复核该商户欠费追缴中的单是否收齐。终审落账时的押金抵扣销账同样回调，此刻本结算单仍在落账前状态、不推进（状态由落账按押金够不够分流）。
     */
    @Override
    @Transactional
    public void onArApItemPaid(com.complex.platform.finance.ArApItemRecord item) {
        if ("deposit_refund".equals(item.sourceType())) {
            if ("settled".equals(item.status())) {
                repository.findSettlementById(item.sourceId()).filter(st -> "deposit_offsetting".equals(st.status()))
                        .ifPresent(st -> markSettlementCleared(st, item.id()));
            }
            return;
        }
        if ("receivable".equals(item.direction())) {
            // 第三轮并发约定：每张候选单先锁合同行再重读状态与未清额（与终审落账同序）——并发的另一笔收款或终审落账先提交时按锁后事实判定，
            // 收齐的单不会永久停在欠费追缴中；候选含落账前的在办单（终审落账事务可能正持锁、提交后即为欠费追缴中）。
            // ponytail: 押金补退应收收款（先锁押金余额）与同商户终审落账（先锁合同行）恰好同刻时锁序相反，数据库判死锁回滚其一、可重试；不另设全局锁序。
            for (ContractSettlementRecord candidate : repository.listSettlementsByMerchant(item.counterpartyId())) {
                if (!"arrears_collecting".equals(candidate.status()) && !LIVE_SETTLEMENT_STATUSES.contains(candidate.status())) {
                    continue;
                }
                lockContract(candidate.contractId());
                repository.findSettlementById(candidate.id())
                        .filter(st -> "arrears_collecting".equals(st.status()) && arrearsCollected(st))
                        .ifPresent(st -> markSettlementCleared(st, item.id()));
            }
        }
    }

    /**
     * 收齐判定（第四轮裁决第 3 条「「欠费追缴中」的收齐判定随之改为看台账未收额」、第 1 条「违约金和欠费都收齐才「已结清」」）：该结算列明的欠费应收
     * （截止月 throughMonth、沿变更子合同链，与落账同口径；被押金覆盖的部分落账时已以押金抵扣销账）与本结算单挂出的违约金应收，台账未收额全部为零。
     */
    private boolean arrearsCollected(ContractSettlementRecord settlement) {
        YearMonth through = null;
        for (Map<String, Object> item : settlement.settlementItems()) {
            if ("unpaid_receivable_summary".equals(item.get("item"))) {
                through = YearMonth.parse(String.valueOf(item.get("throughMonth")));
            }
        }
        return sumUnpaidArOverChain(requireContract(settlement.contractId()), through)
                + repository.sumUnpaidTerminationPenalty(settlement.id()) <= 0;
    }

    private void markSettlementCleared(ContractSettlementRecord settlement, String arApItemId) {
        repository.saveSettlement(settlement.withStatus("settled", settlement.settlementItems(), Instant.now()));
        auditEventService.recordSuccess("contract.settlement_cleared", "finance.payment.register", "contract_settlement",
                settlement.id(), null, arApItemId, Map.of("from_status", settlement.status()));
    }

    /** HP-C06：等待期届满日（最后审批完成日＋等待期）与押金退还应付到期日，均为所在地日期；到期日晚于届满日＝顺延。 */
    private record RefundDue(LocalDate waitEnd, LocalDate dueDate) {
        boolean deferred() {
            return dueDate.isAfter(waitEnd);
        }
    }

    /**
     * HP-C06 押金退还应付到期日（BSM L1352／ENUM L252「以退租结算算账完成为准执行支付，且不早于等待期届满（等待期自最后一个审批完成之日起算，
     * 业务参数，按项目可配……）」「算账晚于等待期完成的顺延至算账完成之日支付」、裁决第 7 条）：到期日＝max(算账完成日, 最后审批完成日＋等待期)；
     * 日期按所在地（BSM L1351「以合同对应门店／点位的当地时间为准，不使用服务器默认时区」→ LocalZone）。
     */
    private RefundDue depositRefundDue(ContractRecord contract, Instant lastApprovedAt, Instant settledAt) {
        LocalDate waitEnd = lastApprovedAt.atZone(LocalZone.ID).toLocalDate()
                .plusDays(repository.depositRefundWaitSetting(contract.projectId()).value());
        LocalDate settledDay = settledAt.atZone(LocalZone.ID).toLocalDate();
        return new RefundDue(waitEnd, settledDay.isAfter(waitEnd) ? settledDay : waitEnd);
    }

    /** 押金处置项写「预计退押金日期」（＝应付到期日）；顺延时另写顺延原因——商户专区「我的对账」读同一结算单。 */
    private static void annotateDepositRefundDue(List<Map<String, Object>> items, RefundDue due) {
        for (Map<String, Object> item : items) {
            if ("deposit_disposal".equals(item.get("item"))) {
                item.put("expectedRefundDate", due.dueDate().toString());
                if (due.deferred()) {
                    item.put("refundDeferralReason", DEPOSIT_REFUND_DEFERRAL_REASON);
                }
            }
        }
    }

    /** 把落账后的台账项 id 回写到对应结算清单项（留痕：清单项 ↔ ar_ap_items）。 */
    private void annotateSettlementItem(List<Map<String, Object>> items, String itemKey, String arApItemId) {
        for (Map<String, Object> item : items) {
            if (itemKey.equals(item.get("item"))) {
                item.put("arApItemId", arApItemId);
            }
        }
    }

    /** 退押金实退值（以 FOR UPDATE 落账为准，可能因余额被其它合同退租消费而低于算法净额）回写结算清单留痕。 */
    private void recordActualDepositRefund(List<Map<String, Object>> items, long actualRefundCent, long computedNetCent) {
        for (Map<String, Object> item : items) {
            if ("deposit_disposal".equals(item.get("item"))) {
                item.put("actualRefundCent", actualRefundCent);
                if (actualRefundCent < computedNetCent) {
                    item.put("status", actualRefundCent > 0 ? "payable_posted_capped" : "listed_only");
                    item.put("note", "台账实收余额已被其它退租消费，按余额封顶实退（低于算法净额）");
                }
            }
        }
    }

    private void finalizeContractAfterSettlement(ContractRecord contract, String trigger, Instant now) {
        String status = "expiry".equals(trigger) ? "expired" : "terminated";
        repository.saveContract(new ContractRecord(contract.id(), contract.merchantId(), contract.contractSubjectId(),
                contract.operationSubjectId(), contract.settlementSubjectId(), contract.effectiveDate(),
                contract.expiryDate(), contract.rentTerms(), contract.commissionMode(), contract.energyFeeBasis(),
                contract.depositCent(), contract.penaltyTerms(), contract.settlementRules(), contract.snapshot(),
                contract.shareholderRatioForReport(), contract.attachmentIds(), status, contract.projectId(),
                now, "settled via " + trigger, contract.renewedFromContractId(), contract.createdAt(), now, contract.reviewId()));
    }

    // ============ small helpers ============
    private ContractRecord withStatus(ContractRecord c, String status) {
        return new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(), c.operationSubjectId(),
                c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(), c.rentTerms(), c.commissionMode(),
                c.energyFeeBasis(), c.depositCent(), c.penaltyTerms(), c.settlementRules(), c.snapshot(),
                c.shareholderRatioForReport(), c.attachmentIds(), status, c.projectId(), c.terminatedAt(),
                c.terminationReason(), c.renewedFromContractId(), c.createdAt(), Instant.now(), c.reviewId());
    }

    /** HP-C01：挂上提交审批时开的统一审批复核单号（contracts.review_id）。 */
    private ContractRecord withReview(ContractRecord c, String reviewId) {
        return new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(), c.operationSubjectId(),
                c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(), c.rentTerms(), c.commissionMode(),
                c.energyFeeBasis(), c.depositCent(), c.penaltyTerms(), c.settlementRules(), c.snapshot(),
                c.shareholderRatioForReport(), c.attachmentIds(), c.lifecycleStatus(), c.projectId(), c.terminatedAt(),
                c.terminationReason(), c.renewedFromContractId(), c.createdAt(), c.updatedAt(), reviewId);
    }

    private ContractRecord withAttachments(ContractRecord c, List<String> attachmentIds) {
        return new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(), c.operationSubjectId(),
                c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(), c.rentTerms(), c.commissionMode(),
                c.energyFeeBasis(), c.depositCent(), c.penaltyTerms(), c.settlementRules(), c.snapshot(),
                c.shareholderRatioForReport(), attachmentIds, c.lifecycleStatus(), c.projectId(), c.terminatedAt(),
                c.terminationReason(), c.renewedFromContractId(), c.createdAt(), Instant.now(), c.reviewId());
    }

    private ContractRecord withSnapshot(ContractRecord c, Map<String, Object> snapshot) {
        return new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(), c.operationSubjectId(),
                c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(), c.rentTerms(), c.commissionMode(),
                c.energyFeeBasis(), c.depositCent(), c.penaltyTerms(), c.settlementRules(), snapshot,
                c.shareholderRatioForReport(), c.attachmentIds(), c.lifecycleStatus(), c.projectId(),
                c.terminatedAt(), c.terminationReason(), c.renewedFromContractId(), c.createdAt(), Instant.now(), c.reviewId());
    }

    private ContractRecord withRentTerms(ContractRecord c, Map<String, Object> rentTerms) {
        return new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(), c.operationSubjectId(),
                c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(), rentTerms, c.commissionMode(),
                c.energyFeeBasis(), c.depositCent(), c.penaltyTerms(), c.settlementRules(), c.snapshot(),
                c.shareholderRatioForReport(), c.attachmentIds(), c.lifecycleStatus(), c.projectId(), c.terminatedAt(),
                c.terminationReason(), c.renewedFromContractId(), c.createdAt(), c.updatedAt(), c.reviewId());
    }

    private ContractRecord requireContract(String contractId) {
        return repository.findContractById(requireText(contractId, "contract_id"))
                .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "合同不存在或已被删除"));
    }

    /**
     * 第十八批 G 段 82b-R3（PGM 第 659 行「在所分配的权限内可以查看、处理该角色可查看项目的数据」「逐个勾选的只能查看被选中的项目」）：
     * 既有合同的读写入口取到合同后、进幂等执行与行锁之前核合同所属项目在该入口权限码可触及范围内（含可跨项目角色勾选的项目）；
     * 平台管理员不限，其余账号对未带项目的合同一律 403。
     */
    private ContractRecord requireContractInReach(String contractId, String code) {
        ContractRecord c = requireContract(contractId);
        var actor = authorizationService.currentActor();
        if (!authorizationService.isPlatformAdmin(actor)
                && (c.projectId() == null || !authorizationService.canReachProject(actor, c.projectId(), code))) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "合同不在本权限可触及的项目范围内");
        }
        return c;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String requireEnum(String value, Set<String> allowed, String field) {
        String t = requireText(value, field);
        if (!allowed.contains(t)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return t;
    }

    private LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value;
    }
}
