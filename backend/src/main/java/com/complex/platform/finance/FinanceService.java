package com.complex.platform.finance;

// 文件版本：V0.21

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest;
import com.complex.platform.finance.dto.ArApAdjustmentRequest;
import com.complex.platform.finance.dto.ArApItemRequest;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.finance.dto.PropertyFeeBillRequest;
import com.complex.platform.finance.dto.PropertyFeeRateRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.DepositActionRequest;
import com.complex.platform.finance.dto.InventoryVariancePostRequest;
import com.complex.platform.finance.dto.TurnoverRentBillRequest;
import com.complex.platform.finance.dto.TurnoverRentRuleRequest;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.dto.CloseReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.ReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.RefundRequest;
import com.complex.platform.finance.dto.RefundOperationRequest;
import com.complex.platform.finance.dto.RefundResultConfirmRequest;
import com.complex.platform.finance.dto.RefundExecutionRequest;
import com.complex.platform.finance.gateway.PaymentGatewayRequest;
import com.complex.platform.finance.gateway.PaymentGatewayResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class FinanceService implements TerminationSettlementPort {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final String CURRENCY_CNY = "CNY";
    private static final Set<String> DIRECTIONS = Set.of("receivable", "payable");
    // P1#13/#14 + R1-LOOP-01：库存差异、综合体计费三源、卡座销售提成入账来源。
    private static final Set<String> SOURCE_TYPES = Set.of(
            "contract", "order", "refund", "manual", "settlement", "bar_bill",
            "employee_commission", "inventory_difference", "turnover_rent", "cam", "deposit",
            "property_fee", "pass_drink_settlement", "ticket_distribution", "dividend_payout",
            "dividend_reserve_withheld", "dividend_reserve_released", "termination_penalty", "deposit_refund",
            // CG-10：采购入库确认形成的供应商应付（BSM「应收应付台账」来源「含采购入库形成的供应商应付」）。
            "purchase_inbound",
            // CG-11b 经销订单应收（BSM:1230「尾款持续列入该订单应收账款」）
            "dealer_order",
            // D7 超池特批·超额 C 口径(甲标签法):池内额过 ticket_distribution(项目公司承担)、超出总池的超额部分过
            //   platform_incentive_subsidy(L3 娱乐平台公司承担)。DB 层 source_type 自由 VARCHAR 无 CHECK,零迁移。
            "platform_incentive_subsidy",
            // CG-07：派驻/营销人员提成由酒吧按月拨付人员所属主体的内部结算应付（StaffService 提成月结写入）。
            "dispatch_commission_transfer",
            // 第六批：酒吧小费/服务费月结分配跨主体拨付（BSM「派驻人员的成本与报酬」：应付本人的提成、服务费等报酬由该酒吧按月拨付给人员所属主体；
            //   BarPayrollAllocationService 月结按 allocation 逐条写入，正额应付/负额应收）。
            "bar_payroll_transfer",
            // HP-C03：面积/位置变更子合同押金补退差额（BSM L1353「批准生效后登记应收或应退」）。
            "deposit_adjustment",
            // FS-013：单一酒吧门票综合服务费按计费月汇总后单独挂酒吧应收（结算过账写入；退款重算的应退同来源 direction=payable）。
            "single_bar_service_fee",
            // 第十批 TK-24-R3：订台定金达标原路退回的应付退款项（BSM L1062「达标时，各笔定金分别原路退回」）。
            "bar_deposit_refund",
            // FS-021-R1：通票分账负额明细（向商户追回）单独挂追收应收，不与应付轧差（BSM L755）。
            "ticket_distribution_recovery");
    // P1#13 库存差异原因（与 V108 chk_inventory_variance_reason 一致）。
    private static final Set<String> INVENTORY_VARIANCE_REASONS = Set.of("damage", "shortage", "overage", "supplier_fault");
    // P1#14 保证金四动作（与 V110 chk_deposit_ledger_action 一致）。
    private static final Set<String> DEPOSIT_ACTIONS = Set.of("collect", "refund", "deduct", "forfeit");
    // P2#④ 退款方向统一维度（与 V118 chk_finance_refunds_direction 一致）。默认 original。
    private static final Set<String> REFUND_DIRECTIONS = Set.of("original", "cash", "third_party", "mixed");
    private static final String DEFAULT_REFUND_DIRECTION = "original";
    private static final Set<String> DAILY_CLOSE_SCOPES = Set.of("project", "merchant", "bar", "ticketing", "amusement");
    private static final Set<String> OFFLINE_PAYMENT_METHODS = Set.of("cash", "bank_transfer", "pos", "other");
    // 第四轮裁决第 3 条：退租结算押金抵扣销账的收款方式（财务结算 PRD L945 押金动作「抵扣（offset）」；同 dividend_offset 范式）。
    private static final String DEPOSIT_OFFSET_METHOD = "deposit_offset";
    // R5 T7：六源（与 finance_daily_close_summaries.chk_fdcs_source 一致）。
    private static final List<String> DAILY_CLOSE_SOURCES = List.of(
            "official_ticket", "channel_ticket", "bar", "amusement", "supply_cost", "thirdparty_reflow");
    private static final Set<String> ADJUSTMENT_DECISIONS = Set.of("approve", "reject");
    // P0#7 / R6-D07：对账差异四类枚举（与 V105 chk_frd_difference_type 一致）。
    private static final Set<String> DIFFERENCE_TYPES = Set.of("amount", "count", "status", "missing");
    // 取数来源：system=平台侧系统取数；manual=第三方侧未接入手工录入进复核（与 V105 chk_frd_source 一致）。
    private static final Set<String> DIFFERENCE_SOURCES = Set.of("system", "manual");

    // i18n：AR/AP 台账 CSV 数据行的用户可见枚举列（收付方向/来源类型/状态）后端翻中文；未知值原样输出不吞值（ponytail: 就近小映射，非全域枚举表）。
    private static final Map<String, String> AR_AP_DIRECTION_LABELS = Map.of(
            "receivable", "应收", "payable", "应付", "internal_accounting", "内部核算");
    private static final Map<String, String> AR_AP_STATUS_LABELS = Map.of(
            "pending_collection", "待收款", "pending_payment", "待付款",
            "partial_paid", "部分收付", "settled", "已结清",
            // P0-B（V344）锁定后补录红冲：冲正行状态（原行金额的严格取反凭证，不参与收付）。
            "reversed", "已红冲");
    private static final Map<String, String> AR_AP_SOURCE_TYPE_LABELS = Map.ofEntries(
            Map.entry("contract", "合同"), Map.entry("order", "订单"), Map.entry("refund", "退款"),
            Map.entry("manual", "手工录入"), Map.entry("settlement", "结算"), Map.entry("bar_bill", "酒吧账单"),
            Map.entry("employee_commission", "员工提成"), Map.entry("inventory_difference", "库存差异"),
            Map.entry("turnover_rent", "营业额抽成租金"), Map.entry("cam", "公共区域维护费"),
            Map.entry("deposit", "押金"), Map.entry("property_fee", "物业费"),
            Map.entry("pass_drink_settlement", "通票酒水结算"), Map.entry("ticket_distribution", "门票分销"),
            Map.entry("dividend_payout", "持股分红发放"), Map.entry("dividend_reserve_withheld", "分红留存待释放"),
            Map.entry("dividend_reserve_released", "分红留存已释放"), Map.entry("dealer_order", "经销订单"),
            Map.entry("termination_penalty", "退租违约金"), Map.entry("deposit_refund", "退租押金退还"),
            Map.entry("platform_incentive_subsidy", "平台激励补贴(超池特批)"),
            Map.entry("bar_deposit_refund", "订台定金达标退回"),
            Map.entry("ticket_points_issuance", "门票积分发行月结"),
            Map.entry("staff_payroll", "员工工资"),
            Map.entry("dispatch_commission_transfer", "派驻提成月结拨付"),
            Map.entry("bar_payroll_transfer", "酒吧报酬月结拨付"),
            Map.entry("deposit_adjustment", "变更押金补退差额"),
            // FS-012：补偿新增履约由补偿批次承担主体对实际提供方的补偿应付（AmusementCompensationFulfillmentAdapter 写入）。
            Map.entry("compensation_fulfillment", "补偿新增履约结算"),
            // 裁决 2026-10-19 第二轮第 5 条：补偿应付已付部分在该次核销撤销后对提供方的追收应收。
            Map.entry("compensation_fulfillment_recovery", "补偿应付已付追收"),
            // FS-017（BSM L1072）：存酒作废补存/赔付的经营损失（internal_accounting，承担方=原门店或接收后的娱乐平台公司）；
            // 平台垫付后向原门店追收的应收（receivable，经统一回写点结清时回写补存/赔付记录的追收状态）。
            Map.entry("saved_bottle_remedy", "存酒补存/赔付损失"),
            Map.entry("saved_bottle_remedy_recovery", "存酒退出承接垫付追收"),
            Map.entry("single_bar_service_fee", "单店门票综合服务费"),
            Map.entry("ticket_distribution_recovery", "通票分账追收"),
            // HP-P01（BSM L1437 账单类型「车位月租」；L1445「停车费收入归城市项目公司」）：合同车位月租应收、客人停车费内部核算。
            Map.entry("contract_parking_monthly", "车位月租"),
            Map.entry("parking_fee", "停车费"),
            // HP-P01 第二组（BSM「停车场」章「消费抵免」条）：城市项目公司对承担商户的停车抵免成本普通内部结算应收。
            Map.entry("parking_credit_cost", "停车抵免成本"));

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationEventPublisher refundEvents;
    @org.springframework.beans.factory.annotation.Autowired
    private com.complex.platform.dealer.JdbcDealerOrderRepository dealerRefunds;
    @org.springframework.beans.factory.annotation.Autowired
    private com.complex.platform.dealer.JdbcDealerRepository refundDealers;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate ticketPointsJdbc;

    /** 积分中心月结桥：唯一数据源是已公布、异议期完成的月账单，不接受操作者提供金额和主体。 */
    @Transactional
    public String recordTicketPointsIssuanceBill(String billId) {
        record Fact(String seller,String merchant,String receiver,String project,long amount,LocalDate due,LocalDate month) {}
        List<Fact> facts=ticketPointsJdbc.query("""
            SELECT b.* FROM ticket_points_monthly_bills b
            WHERE b.id=? AND b.visible_at IS NOT NULL AND b.objection_deadline IS NOT NULL
              AND b.objection_deadline<=?
              AND b.due_date>=b.objection_deadline
              AND NOT EXISTS(SELECT 1 FROM ticket_points_bill_objections o WHERE o.bill_id=b.id AND o.status='open')
            FOR UPDATE
            """,(rs,n)->new Fact(rs.getString("seller_entity_id"),rs.getString("seller_merchant_id"),rs.getString("receiver_entity_id"),rs.getString("project_id"),rs.getLong("amount_cent"),rs.getObject("due_date",LocalDate.class),rs.getObject("billing_month",LocalDate.class)),billId,LocalDate.now(com.complex.platform.common.LocalZone.ID));
        if(facts.size()!=1)throw ApiException.conflict("POINTS_BILL_NOT_READY","积分账单尚未满足正式入账条件");
        Fact f=facts.getFirst();String id="arap_"+billId;
        if(repository.findArApItemById(id).isPresent())return id;
        if(f.amount()<=0 || f.seller()==null || f.receiver()==null || f.project()==null)throw ApiException.conflict("POINTS_BILL_FACTS_INCOMPLETE","积分账单缺少真实金额或双方主体");
        Instant now=Instant.now();
        LocalDate date=LocalDate.ofInstant(now,com.complex.platform.common.LocalZone.ID);
        if(repository.lockedDailyCloseExistsForDate(date,f.merchant()))throw ApiException.conflict("DAY_CLOSE_LOCKED","当前业务日已封账，积分账单保留待入账");
        repository.saveArApItem(new ArApItemRecord(id,"POINTS-"+billId,"receivable",f.seller(),"ticket_points_issuance",billId,f.amount(),0,f.amount(),CURRENCY_CNY,f.due(),f.month().toString().substring(0,7),"pending_collection",now,now,f.merchant(),f.project()));
        repository.saveArApItemSource(new ArApItemSourceRecord("src_"+billId,id,"ticket_points_issuance",billId,f.amount(),now));
        auditEventService.recordSuccess("ticket.points.issuance.posted",null,"ar_ap_item",id,f.merchant(),billId,Map.of("seller_entity_id",f.seller(),"receiver_entity_id",f.receiver(),"project_id",f.project(),"amount_cent",f.amount(),"due_date",f.due().toString()));
        return id;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private FinancePayrollService payrollFinance;

    @Transactional
    public ArApItemRecord ensurePayrollPayable(String statementId) {
        return payrollFinance.ensurePayrollPayable(statementId);
    }

    private java.util.Set<String> payrollVisibleItems(String permission) {
        return payrollFinance == null ? java.util.Set.of() : payrollFinance.visiblePayableIds(permission);
    }

    private final FinanceRepository repository;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final com.complex.platform.common.BusinessDayResolver businessDayResolver;
    private final com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway;
    // 跨模块闭环②a：MDM 高风险门控（terminal 模块 public 服务）。依赖方向 finance→terminal 无环
    // （finance 已先引用 terminal.TerminalDeviceRepository 为证；terminal 不引用 finance）。
    // 可为 null=门控未接（旧构造/兼容路径）。
    private final com.complex.platform.terminal.MdmService mdmService;
    // 第三轮裁决第 4 条：登记收付款后回调合同域推进「押金抵扣中」「欠费追缴中」结算单（合同域实现）。构造器 @Lazy 注入防初始化环
    // （与合同域注入 TerminationSettlementPort 同范式）；旧构造传 null＝不回调（不接合同域的单测）。
    private final TerminationSettlementProgressPort settlementProgress;

    // Stage B（审计 A2，适用所有退款）：退款分级授权引擎 + 功能标志（默认关）。用字段注入以免改既有多处构造点；
    // 可为 null（旧构造/单测未注入）；包私有便于单测置位。开标志后超额财务退款须复核（见 requireRefundTierAuthorized）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    RefundAuthorizationService refundAuthorizationService;

    // FS-017：存酒退出承接垫付追收回写端口（inventory 域实现；@Lazy 防初始化环；可为 null＝不回写）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    SavedBottleRemedyRecoveryPort savedBottleRemedyRecovery;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.evidence.EvidenceAttachmentValidator refundEvidenceValidator;

    private org.springframework.transaction.support.TransactionTemplate refundTransactions;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setRefundTransactionManager(org.springframework.transaction.PlatformTransactionManager manager) {
        refundTransactions = new org.springframework.transaction.support.TransactionTemplate(manager);
        refundTransactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private <T> T refundTransaction(Supplier<T> action) {
        if (refundTransactions == null) {
            throw ApiException.conflict("REFUND_TRANSACTION_UNAVAILABLE", "退款事务管理器不可用，不能发起外部出款");
        }
        return refundTransactions.execute(status -> action.get());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FinanceService(
            FinanceRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.common.BusinessDayResolver businessDayResolver,
            com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway,
            com.complex.platform.terminal.MdmService mdmService,
            @org.springframework.context.annotation.Lazy TerminationSettlementProgressPort settlementProgress
    ) {
        this.repository = repository;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.businessDayResolver = businessDayResolver;
        this.paymentGateway = paymentGateway;
        this.mdmService = mdmService;
        this.settlementProgress = settlementProgress;
    }

    /** 兼容构造（第三轮前的七参形）：不接合同域回调。 */
    public FinanceService(
            FinanceRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.common.BusinessDayResolver businessDayResolver,
            com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway,
            com.complex.platform.terminal.MdmService mdmService
    ) {
        this(repository, operations, auditEventService, authorizationService, businessDayResolver, paymentGateway,
                mdmService, null);
    }

    /**
     * 向后兼容构造：未提供支付渠道路由 / MDM 门控时，使用"开关关"语义占位路由且门控未接（mdmService=null）。
     * 供既有单测构造（开关默认关，行为与生产默认一致）。
     */
    public FinanceService(
            FinanceRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.common.BusinessDayResolver businessDayResolver,
            com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway
    ) {
        this(repository, operations, auditEventService, authorizationService, businessDayResolver,
                paymentGateway, null);
    }

    /**
     * 向后兼容构造：未提供支付渠道路由时，使用"开关关"语义的占位路由（不调外部渠道）。
     * 供既有单测构造（开关默认关，行为与生产默认一致）。
     */
    public FinanceService(
            FinanceRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.common.BusinessDayResolver businessDayResolver
    ) {
        this(repository, operations, auditEventService, authorizationService, businessDayResolver,
                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null);
    }

    public PageResponse<ArApItemRecord> listArApItems(String keyword, int page, int size) {
        // 服务端真分页:租户 scope(currentMerchantScope)决定 merchant_id 谓词,keyword 跨 单号/方向/状态 服务端筛选。
        return operations.readPageResult(
                AuditedOperation.of(
                        "finance_ar_ap.viewed", "finance_ar_ap.view_failed",
                        "finance.ar_ap.view", "ar_ap_item"),
                () -> {
                    authorizationService.requirePermission("finance.ar_ap.view");
                    var actor = authorizationService.currentActor();
                    var reach = authorizationService.projectReach(actor, "finance.ar_ap.view");
                    return repository.listArApItemsForProjects(reach.platformAdmin() ? null : trimToNull(actor.merchantId()),
                            reach.platformAdmin() ? null : reach.organizationIds(), keyword, page, size, payrollVisibleItems(FinancePayrollService.VIEW));
                }
        );
    }

    public ArApLedgerExportRecord exportArApItems() {
        try {
            authorizationService.requirePermission("finance.ar_ap.export");
            var actor = authorizationService.currentActor();
            var reach = authorizationService.projectReach(actor, "finance.ar_ap.export");
            List<ArApItemRecord> items = new java.util.ArrayList<>();
            for (int page=1;;page++) {
                var result = repository.listArApItemsForProjects(reach.platformAdmin() ? null : trimToNull(actor.merchantId()),
                        reach.platformAdmin() ? null : reach.organizationIds(), null, page, 200, payrollVisibleItems(FinancePayrollService.EXPORT));
                items.addAll(result.items());
                if (result.items().isEmpty() || items.size() >= result.total()) break;
            }
            ArApLedgerExportRecord export = new ArApLedgerExportRecord(
                    "arap_export_" + UUID.randomUUID().toString().replace("-", ""),
                    "csv",
                    items.size(),
                    List.copyOf(items),
                    arApCsv(items),
                    Instant.now()
            );
            auditEventService.recordSuccess(
                    "finance_ar_ap.exported",
                    "finance.ar_ap.export",
                    "ar_ap_export",
                    export.id(),
                    "req_" + UUID.randomUUID(),
                    null,
                    Map.of("row_count", export.rowCount(), "format", export.format())
            );
            return export;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "finance_ar_ap.export_failed",
                    "finance.ar_ap.export",
                    "ar_ap_export",
                    "unknown",
                    "req_" + UUID.randomUUID(),
                    null,
                    exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR"
            );
            throw exception;
        }
    }

    private String arApCsv(List<ArApItemRecord> items) {
        StringBuilder csv = new StringBuilder("单据编号,收付方向,往来对象编号,来源类型,来源编号,金额(分),已收付金额(分),未收付金额(分),币种,到期日,账期,状态\n");
        for (ArApItemRecord item : items) {
            csv.append(csv(item.itemNo())).append(',')
                    .append(csv(arApLabel(AR_AP_DIRECTION_LABELS, item.direction()))).append(',')
                    .append(csv(item.counterpartyId())).append(',')
                    .append(csv(arApLabel(AR_AP_SOURCE_TYPE_LABELS, item.sourceType()))).append(',')
                    .append(csv(item.sourceId())).append(',')
                    .append(item.amountCent()).append(',')
                    .append(item.paidAmountCent()).append(',')
                    .append(item.unpaidAmountCent()).append(',')
                    .append(csv(item.currency())).append(',')
                    .append(csv(item.dueDate() == null ? null : item.dueDate().toString())).append(',')
                    .append(csv(item.accountingPeriod())).append(',')
                    .append(csv(arApLabel(AR_AP_STATUS_LABELS, item.status()))).append('\n');
        }
        return csv.toString();
    }

    /** 枚举列翻中文：命中映射用中文，未知值（含 null）原样返回不吞值。 */
    private static String arApLabel(Map<String, String> labels, String raw) {
        return raw == null ? null : labels.getOrDefault(raw, raw);
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\n") || safe.contains("\"")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.ar_ap.manage", admissionAction = "finance.locked_backfill", objectType = "ar_ap_item")
    @Transactional
    public ArApItemRecord createArApItem(ArApItemRequest request) {
        return executeWrite(
                "createArApItem",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApItemRecord.class,
                "finance_ar_ap.created",
                "finance_ar_ap.create_failed",
                "finance.ar_ap.manage",
                "ar_ap_item",
                () -> createItem(request, false, null)
        );
    }

    /**
     * 🔴 P2:显式项目归属过账(覆盖 token 项目域)。结算过账用:ticket_distribution 传 bill.projectId、
     * platform_incentive_subsidy 传 null(平台域)——不继承过账人 token 域,保项目公司聚合口径正确。
     * explicitProjectId 可为 null(=平台域)。鉴权/审计/幂等同 createArApItem(request)。
     */
    public ArApItemRecord createArApItem(ArApItemRequest request, String explicitProjectId) {
        return executeWrite(
                "createArApItem",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApItemRecord.class,
                "finance_ar_ap.created",
                "finance_ar_ap.create_failed",
                "finance.ar_ap.manage",
                "ar_ap_item",
                () -> createItem(request, true, explicitProjectId)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
    @Transactional
    public ArApPaymentResponse registerArApPayment(String arApItemId, ArApPaymentRequest request) {
        String itemId = requireText(arApItemId, "ar_ap_item_id");
        requireArApBeforeReplay(itemId, "finance.payment.register", "finance_payment.register_failed",
                request == null ? null : request.idempotencyKey());
        return executeWrite(
                "createArApPayment:" + itemId,
                request == null ? null : request.idempotencyKey(),
                request,
                ArApPaymentResponse.class,
                "finance_payment.registered",
                "finance_payment.register_failed",
                "finance.payment.register",
                "ar_ap_payment",
                () -> registerPayment(itemId, request)
        );
    }

    /**
     * S6：按 (direction, source_type, source_id) 判断分录是否存在。
     * 供 bar 域赠饮冲销前判断「原应付到底挂没挂出」——商户让利型/自营吧核销时不挂应付,
     * 冲正若不查存在性会凭空造一笔反向分录(商户平白欠平台一笔钱)。
     */
    @Transactional(readOnly = true)
    public boolean arApItemExistsBySource(String direction, String sourceType, String sourceId) {
        return repository.existsArApItemBySource(direction, sourceType, sourceId);
    }

    /**
     * R5 T7 协同：判断某经营日是否已被锁定日结纳入（status='locked'）。
     * 供其他域（如 bar 反结）在改动结账后金额前校验，锁定则须走补录审批，不得直接冲减锁定日 net。
     */
    @Transactional(readOnly = true)
    public boolean isBusinessDayLocked(LocalDate businessDate) {
        if (businessDate == null) {
            return false;
        }
        // F-OPS-09：按调用方 merchant scope 判定——商户 scope 仅看本商户封账，平台口径看全部封账。
        return repository.lockedDailyCloseExistsForDate(
                businessDate, authorizationService.currentMerchantScope().orElse(null));
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.daily_close.lock")
    @Transactional
    public DailyCloseRecord createDailyClose(DailyCloseRequest request) {
        return executeWrite(
                "createDailyClose",
                request == null ? null : request.idempotencyKey(),
                request,
                DailyCloseRecord.class,
                "finance_daily_close.locked",
                "finance_daily_close.lock_failed",
                "finance.daily_close.lock",
                "finance_daily_close",
                () -> lockDailyClose(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment_intent.create")
    @Transactional
    public PaymentIntentRecord createPaymentIntent(PaymentIntentRequest request) {
        return executeWrite(
                "createPaymentIntent",
                request == null ? null : request.idempotencyKey(),
                request,
                PaymentIntentRecord.class,
                "payment.intent_created",
                "payment.intent_failed",
                "finance.payment_intent.create",
                "finance_payment_intent",
                () -> createIntent(request)
        );
    }

    public Optional<PaymentIntentRecord> findPaymentIntentForUpdate(String paymentIntentId) {
        return repository.findPaymentIntentByIdForUpdate(paymentIntentId);
    }

    /** M006-027：仅信任已启用渠道的服务端查询结果，不能用内部成功标志代替收款。 */
    @Transactional
    public PaymentIntentRecord confirmOnlinePaymentIntent(String paymentIntentId, String payeeMerchantId, String fundMode) {
        if (paymentGateway == null || !paymentGateway.isGatewayEnabled()) {
            throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道未启用");
        }
        PaymentIntentRecord intent = repository.findPaymentIntentByIdForUpdate(paymentIntentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在"));
        if (!Set.of("wechat", "alipay", "unionpay").contains(intent.channel())
                || intent.gatewayRef() == null || intent.gatewayRef().isBlank()
                || intent.gatewayProvider() == null || intent.gatewayProvider().isBlank()
                || "reserved".equals(intent.gatewayProvider())) {
            throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道尚未返回有效支付单");
        }
        var result = paymentGateway.queryStatus(intent.gatewayRef());
        if (result == null || result.requiresInternalFallback()) {
            throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道未配置");
        }
        if (result.status() != com.complex.platform.finance.gateway.PaymentGatewayResult.Status.SUCCEEDED) {
            throw ApiException.conflict("PAYMENT_NOT_SUCCEEDED", "支付渠道尚未确认收款成功");
        }
        var confirmed = result.confirmedPayment();
        if (confirmed == null
                || !java.util.Objects.equals(intent.gatewayProvider(), result.gatewayProvider())
                || !java.util.Objects.equals(intent.gatewayRef(), result.gatewayRef())
                || !java.util.Objects.equals(intent.gatewayRef(), confirmed.gatewayRef())
                || !intent.businessOrderType().equals(confirmed.businessOrderType())
                || !intent.businessOrderId().equals(confirmed.businessOrderId())
                || intent.amountCent() != confirmed.amountCent()
                || !intent.currency().equals(confirmed.currency())
                || !intent.channel().equals(confirmed.channel())
                || !java.util.Objects.equals(payeeMerchantId, confirmed.payeeMerchantId())
                || !java.util.Objects.equals(fundMode, confirmed.fundMode())) {
            throw ApiException.conflict("PAYMENT_CONFIRMATION_MISMATCH", "支付渠道确认与本次充值单不一致");
        }
        if ("succeeded".equals(intent.status())) return intent;
        if (!"created".equals(intent.status())) {
            throw ApiException.conflict("PAYMENT_INTENT_NOT_PENDING", "支付意向不在待支付状态");
        }
        recordInternalAccountingRevenue(intent.id(), intent.businessOrderType(), intent.businessOrderId(), intent.amountCent());
        return repository.savePaymentIntent(new PaymentIntentRecord(intent.id(), intent.businessOrderType(),
                intent.businessOrderId(), intent.amountCent(), intent.currency(), intent.channel(), "succeeded",
                result.gatewayProvider(), result.gatewayRef(), result.status().name(), intent.createdAt(), Instant.now()));
    }

    /** 按 id 查支付意图（储值充值/会员开通：入账前校验支付意图存在且已成功）。 */
    public Optional<PaymentIntentRecord> findPaymentIntent(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return Optional.empty();
        }
        return repository.findPaymentIntentById(paymentIntentId);
    }

    /**
     * 将支付意图标记为成功（占位代替真实支付渠道回调）。储值充值 / 付费会员开通在测试与本地联调期
     * 通过本方法把绑定的支付意图置 succeeded，再由各自服务校验后入账，保证"无支付不入账"。
     * 真实支付渠道接入后由渠道回调驱动此状态。
     */
    @Transactional
    public PaymentIntentRecord markPaymentIntentSucceeded(String paymentIntentId) {
        PaymentIntentRecord intent = repository.findPaymentIntentById(paymentIntentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在或已被删除"));
        if ("succeeded".equals(intent.status())) {
            return intent;
        }
        if (!"created".equals(intent.status())) {
            throw ApiException.conflict("PAYMENT_INTENT_NOT_PENDING", "支付意向不在待支付状态");
        }
        // F2 内部核算归集：支付意图确认成功（created→succeeded，本方法早返回保证单次）即为收入确认点——
        // 票务/储值/会员（及经票务权益的游乐）收入按 direction=internal_accounting 写内部核算台账，供对账/内部核算下钻。
        // 按裁定 C1，internal_accounting 不并入 revenue.net（net 仍取业务表）。同事务（@Transactional）写入。
        recordInternalAccountingRevenue(intent.id(), intent.businessOrderType(), intent.businessOrderId(), intent.amountCent());
        return repository.savePaymentIntent(new PaymentIntentRecord(
                intent.id(),
                intent.businessOrderType(),
                intent.businessOrderId(),
                intent.amountCent(),
                intent.currency(),
                intent.channel(),
                "succeeded",
                intent.createdAt(),
                Instant.now()));
    }

    /**
     * F2 内部核算归集：每笔成功支付意图生成一条 direction=internal_accounting 的内部核算台账（平台自营经营收入归集）。
     * 幂等键 = paymentIntentId（每笔支付意图唯一，markPaymentIntentSucceeded 早返回保证单次触发）——
     * 修正此前用 businessOrderType+businessOrderId 派生 id 的缺陷：储值/会员的 businessOrderId=customerId，
     * 同客户多次充值/开通会命中同一 id 被 ON CONFLICT DO UPDATE 覆盖、少计收入。source_type/source_id 仍记业务单维度供追溯。
     * 内部核算=已确认收入，paid=amount、unpaid=0、settled；不并入 revenue.net（C1）；无 merchant scope（平台口径）。
     */
    private void recordInternalAccountingRevenue(String paymentIntentId, String sourceType, String sourceId, long amountCent) {
        String type = trimToNull(sourceType);
        String id = trimToNull(sourceId);
        String intentId = trimToNull(paymentIntentId);
        if (type == null || id == null || intentId == null || amountCent <= 0) {
            return;
        }
        // AUD-059：商户卡充值(stored_value_topup_merchant)不是平台经营收入——商户预收模式钱归商户、平台代收模式为平台预收负债，
        // 二者都不得计入平台内部核算收入。此处跳过；平台代收的消费结算由 recordStoredValueMerchantPayable 逐笔挂应付商户。
        if ("stored_value_topup_merchant".equals(type)) {
            return;
        }
        String key = slug(intentId);
        String itemId = "arap_internal_" + key;
        Instant now = Instant.now();
        // F-OPS-02：内部核算入账须遵守日结锁，禁止向已封账经营日补写收入（与 createArApItem/recordInboundAr 同口径）。
        // F-OPS-09：内部核算=平台口径（merchant_id=null，见上方方法注释），按平台口径判锁（受任一封账约束）。
        java.time.LocalDate businessDate = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        if (repository.lockedDailyCloseExistsForDate(businessDate, null)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "营业日 " + businessDate + " 已锁定，请走补录调整流程");
        }
        repository.saveArApItem(new ArApItemRecord(
                itemId,
                "INTACC-" + key,
                "internal_accounting",
                "platform_self",
                type,
                id,
                amountCent,
                amountCent,
                0L,
                CURRENCY_CNY,
                businessDate,
                null,
                "settled",
                now,
                now,
                null
        ));
        repository.saveArApItemSource(new ArApItemSourceRecord(
                "arap_src_internal_" + key, itemId, type, id, amountCent, now));
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register", admissionAction = "finance.offline_payment_confirm", objectType = "finance_offline_payment_confirmation")
    @Transactional
    public OfflinePaymentConfirmationRecord confirmOfflinePayment(
            String paymentId,
            OfflinePaymentConfirmRequest request
    ) {
        String pathPaymentId = requireText(paymentId, "payment_id");
        if (request != null && trimToNull(request.arApItemId()) != null)
            requireArApBeforeReplay(request.arApItemId(), "finance.payment.register", "finance_offline_payment.confirm_failed", request.idempotencyKey());
        return executeWrite(
                "confirmOfflinePayment:" + pathPaymentId,
                request == null ? null : request.idempotencyKey(),
                request,
                OfflinePaymentConfirmationRecord.class,
                "finance_offline_payment.confirmed",
                "finance_offline_payment.confirm_failed",
                "finance.payment.register",
                "finance_offline_payment_confirmation",
                () -> confirmOffline(pathPaymentId, request)
        );
    }

    public PageResponse<ReconciliationBatchRecord> listReconciliationBatches() {
        return readPage(
                "finance.reconciliation_batch.viewed",
                "finance.reconciliation_batch.viewed_failed",
                "finance.reconciliation.view",
                "finance_reconciliation_batch",
                () -> {
                    requirePlatformReconciliationAccess();
                    return repository.listReconciliationBatches();
                }
        );
    }

    public PageResponse<ReconciliationDifferenceRecord> listReconciliationDifferences() {
        return readPage(
                "finance_reconciliation.difference_found",
                "finance_reconciliation.view_failed",
                "finance.reconciliation.view",
                "finance_reconciliation_difference",
                () -> {
                    requirePlatformReconciliationAccess();
                    return repository.listReconciliationDifferences();
                }
        );
    }

    /**
     * Reconciliation batches/differences are platform/channel-level finance artifacts with no merchant
     * dimension in the data model. A merchant-scoped caller must not read them — otherwise it would see
     * every merchant's reconciliation data. Platform admins (no merchant scope) may read.
     */
    private void requirePlatformReconciliationAccess() {
        if (authorizationService.currentMerchantScope().isPresent()) {
            throw ApiException.forbidden(
                    "RECONCILIATION_PLATFORM_ONLY",
                    "对账数据为平台级数据，商户账号无权查看");
        }
    }

    /**
     * P0#7 / R6-D07：创建对账差异（强制化）。
     * <ul>
     *   <li>平台侧 expected 由系统取数（按 business_ref 聚合平台权威台账）——不接受请求体手填 expected。</li>
     *   <li>source=system：以系统取数 expected 比对申报 actual，写差额；无系统取数支撑则拒绝。</li>
     *   <li>source=manual：第三方侧未接入，允许录入 actual，但必须有平台侧系统取数支撑（否则纯手填→拒绝），
     *       且强制状态 needs_review（进复核）。</li>
     * </ul>
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.reconciliation.difference.create")
    @Transactional
    public ReconciliationDifferenceRecord createReconciliationDifference(ReconciliationDifferenceRequest request) {
        return executeWrite(
                "createReconciliationDifference",
                request == null ? null : request.idempotencyKey(),
                request,
                ReconciliationDifferenceRecord.class,
                "finance_reconciliation.difference_created",
                "finance_reconciliation.difference_create_failed",
                "finance.reconciliation.difference.create",
                "finance_reconciliation_difference",
                () -> createDifference(request));
    }

    private ReconciliationDifferenceRecord createDifference(ReconciliationDifferenceRequest request) {
        // 对账差异为平台级数据，商户角色不得写。
        requirePlatformReconciliationAccess();

        ReconciliationDifferenceRequest actual = requireRequest(request);
        String batchId = requireText(actual.batchId(), "batch_id");
        String differenceType = requireEnum(actual.differenceType(), DIFFERENCE_TYPES, "difference_type");
        String businessRef = requireText(actual.businessRef(), "business_ref");
        String source = requireEnum(actual.source(), DIFFERENCE_SOURCES, "source");
        long actualAmountCent = requirePositive(actual.actualAmountCent(), "actual_amount_cent");

        // 校验批次存在（差异必须挂在已建对账批次下）。
        repository.findReconciliationBatchById(batchId)
                .orElseThrow(() -> ApiException.notFound("RECONCILIATION_BATCH_NOT_FOUND", "对账批次不存在或已被删除"));

        // 平台侧 expected 一律系统取数（不接受请求体手填）。0=无系统取数支撑。
        long expectedAmountCent = repository.aggregatePlatformExpectedForBusinessRef(businessRef);
        if (expectedAmountCent <= 0) {
            // 纯手填差异（无平台侧系统取数支撑）一律拒绝，无论 source。
            throw ApiException.badRequest("RECONCILIATION_NO_SYSTEM_EXPECTED",
                    "差异登记被拒绝：业务单号 " + businessRef + " 无平台侧系统收款，不允许仅手工登记的差异");
        }

        // source=manual（第三方侧未接入）强制进复核；source=system 仍以系统取数与申报值比对，差额为 0 不应建差异。
        long differenceAmountCent = Math.abs(expectedAmountCent - actualAmountCent);
        String status;
        if ("manual".equals(source)) {
            status = "needs_review";
        } else {
            if (differenceAmountCent == 0) {
                throw ApiException.badRequest("RECONCILIATION_NO_DIFFERENCE",
                        "应收与实收一致，无差异可登记");
            }
            status = "open";
        }

        Instant now = Instant.now();
        ReconciliationDifferenceRecord saved = repository.saveReconciliationDifference(new ReconciliationDifferenceRecord(
                "recon_diff_" + slug(batchId + "_" + actual.idempotencyKey()),
                batchId,
                differenceType,
                businessRef,
                differenceAmountCent,
                expectedAmountCent,
                actualAmountCent,
                source,
                status,
                now));

        // 留痕：executeWrite 已写 finance_reconciliation.difference_created（操作级）。
        // 这里额外写一条带取数明细（expected/actual/source/status）的强制化留痕，便于审计平台侧系统取数来源，
        // 与 summarizeDailyClose 额外发 finance.daily_close_summarized 的做法一致；复用既有 recordSuccess 不改签名。
        auditEventService.recordSuccess(
                "finance_reconciliation.difference_hardened",
                "finance.reconciliation.difference.create",
                "finance_reconciliation_difference",
                saved.id(),
                null,
                actual.idempotencyKey(),
                Map.of(
                        "difference_type", differenceType,
                        "source", source,
                        "expected_amount_cent", expectedAmountCent,
                        "actual_amount_cent", actualAmountCent,
                        "status", status));
        return saved;
    }

    /** 正式关闭对账差异（1.6）：幂等（同 key/已关闭返回同结果），落 close_reason+审计。 */
    @Transactional
    public ReconciliationDifferenceRecord closeReconciliationDifference(
            String differenceId, CloseReconciliationDifferenceRequest request) {
        return executeWrite(
                "closeFinanceReconciliationDifference",
                request == null ? null : request.idempotencyKey(),
                request,
                ReconciliationDifferenceRecord.class,
                "finance.reconciliation.difference_closed",
                "finance.reconciliation.difference_close_failed",
                "finance.reconciliation.resolve",
                "finance_reconciliation_difference",
                () -> closeDifference(differenceId, request));
    }

    private ReconciliationDifferenceRecord closeDifference(
            String differenceId, CloseReconciliationDifferenceRequest request) {
        requirePlatformReconciliationAccess();
        CloseReconciliationDifferenceRequest actual = requireRequest(request);
        String closeReason = requireText(actual.closeReason(), "close_reason");
        ReconciliationDifferenceRecord difference = repository
                .findReconciliationDifferenceById(requireText(differenceId, "difference_id"))
                .orElseThrow(() -> ApiException.notFound(
                        "RECONCILIATION_DIFFERENCE_NOT_FOUND", "对账差异不存在或已被删除"));
        // 幂等：已关闭直接返回（executeWrite 幂等键层亦兜底）。
        if ("closed".equals(difference.status())) {
            return difference;
        }
        String closedBy = authorizationService.currentActor().id();
        return repository.closeReconciliationDifference(difference.id(), closeReason, closedBy, Instant.now());
    }

    // ── P1#13 库存差异 → AR/AP（R6-D08）─────────────────────────────────────────

    /**
     * 库存差异经复核入账：把 review_status='approved' 的 inventory_variance 行入账成 ar_ap_items。
     * 科目方向（R6-D08）：
     * <ul>
     *   <li>报损 damage / 盘亏 shortage（variance_qty &lt; 0）→ 计损益，方向 receivable（向责任方/内部损益挂应收冲减口径）。</li>
     *   <li>盘盈 overage（variance_qty &gt; 0）→ 冲成本，方向 payable（账面成本回冲）。</li>
     *   <li>供应商责任 supplier_fault → 向供应商挂应付索赔 payable（counterparty=供应商）。</li>
     * </ul>
     * 已入账（posted_ar_ap_item_id 非空）幂等返回原台账项；未复核通过拒绝。
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.inventory_variance.post", admissionAction = "finance.inventory_variance_post", objectType = "ar_ap_item")
    @Transactional
    public ArApItemRecord postInventoryVariance(InventoryVariancePostRequest request) {
        return executeWrite(
                "postInventoryVariance",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApItemRecord.class,
                "finance_inventory_variance.posted",
                "finance_inventory_variance.post_failed",
                "finance.inventory_variance.post",
                "ar_ap_item",
                () -> postInventoryVarianceInternal(request));
    }

    // ── P1#14 营业额抽成计费 ─────────────────────────────────────────────────────

    /**
     * 营业额抽成计费引擎：按抽成规则 × 回流营业额生成应收 ar_ap_items(source_type='turnover_rent')。
     * rent = clamp(turnover × rateBps / 10000, minAmountCent, maxAmountCent)；turnover 由回流营业额只读取数。
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.turnover_rent.bill", admissionAction = "finance.turnover_rent_bill", objectType = "ar_ap_item")
    @Transactional
    public ArApItemRecord billTurnoverRent(TurnoverRentBillRequest request) {
        return executeWrite(
                "billTurnoverRent",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApItemRecord.class,
                "finance_turnover_rent.billed",
                "finance_turnover_rent.bill_failed",
                "finance.turnover_rent.bill",
                "ar_ap_item",
                () -> billTurnoverRentInternal(request));
    }

    /**
     * 建提成租金规则(填合同条款时):挂合同 + 阶梯超额累进 + 保底(无封顶)。
     *
     * <p><b>P0-E（v7 审计·抽成规则草案闸）</b>：本端点<b>只落草案</b>（is_draft=TRUE），落库后对结算<b>零影响</b>——
     * 读侧 {@code findActiveTurnoverRentRuleByContract} 只认 is_draft=FALSE。要让新规则生效，须由
     * <b>另一位</b>持 {@code settlement.config.manage} 的同事在结算配置中心复核转正：
     * {@code POST /api/v1/settlement/config/turnover_rent_rule/{id}/activate}（差人守卫，起草人不得自审）。
     * 转正时同合同 + 同商户的旧生效规则被软替换为 inactive（留库可追溯），草案翻为生效。
     */
    public TurnoverRentRuleRecord createTurnoverRentRule(TurnoverRentRuleRequest request) {
        return executeWrite(
                "createTurnoverRentRule",
                request == null ? null : request.idempotencyKey(),
                request,
                TurnoverRentRuleRecord.class,
                "finance_turnover_rent_rule.created",
                "finance_turnover_rent_rule.create_failed",
                "finance.turnover_rent.bill",
                "turnover_rent_rule",
                () -> createTurnoverRentRuleInternal(request));
    }

    // ── P1#14 保证金四动作台账 ───────────────────────────────────────────────────

    /** 保证金台账动作：collect 收取 / refund 退还 / deduct 抵扣 / forfeit 没收，维护运行余额。 */
    @com.complex.platform.gate.GuardedAction(capability = "finance.deposit.manage", admissionAction = "finance.deposit_action", objectType = "deposit_ledger")
    @Transactional
    public DepositLedgerRecord recordDepositAction(DepositActionRequest request) {
        return executeWrite(
                "recordDepositAction",
                request == null ? null : request.idempotencyKey(),
                request,
                DepositLedgerRecord.class,
                "finance_deposit.action_recorded",
                "finance_deposit.action_failed",
                "finance.deposit.manage",
                "deposit_ledger",
                () -> recordDepositActionInternal(request));
    }

    // ── P1#14 CAM 权重分摊 ───────────────────────────────────────────────────────

    // ── F-OPS-08 物业费固定计费（替代已废止 CAM）─────────────────────────────────

    /** 维护物业费费率（项目级、可按期 upsert）。effectivePeriod 为空=设为该项目默认费率（精确期未命中时回退）。 */
    @com.complex.platform.gate.GuardedAction(capability = "finance.property_fee.manage")
    @Transactional
    public PropertyFeeRateRecord createPropertyFeeRate(PropertyFeeRateRequest request) {
        return executeWrite(
                "createPropertyFeeRate",
                request == null ? null : request.idempotencyKey(),
                request,
                PropertyFeeRateRecord.class,
                "finance_property_fee.rate_set",
                "finance_property_fee.rate_set_failed",
                "finance.property_fee.manage",
                "property_fee_rate",
                () -> createPropertyFeeRateInternal(request));
    }

    /** 列出某项目的物业费费率（含默认与各期）。 */
    public List<PropertyFeeRateRecord> listPropertyFeeRates(String projectId) {
        authorizationService.requirePermission("finance.property_fee.manage");
        return repository.listPropertyFeeRates(requireText(projectId, "project_id"));
    }

    /**
     * 物业费按期计费：对 projectId 下运营提供的 merchantIds，按「每平米固定单价 × 商户面积」逐户生成应收，
     * 不退不补、按 (项目,期,商户) 幂等。面积缺失/已计费的商户跳过。
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.property_fee.manage", admissionAction = "finance.property_fee_bill", objectType = "property_fee_bill")
    @Transactional
    public List<PropertyFeeBillRecord> billPropertyFee(PropertyFeeBillRequest request) {
        @SuppressWarnings("unchecked")
        List<PropertyFeeBillRecord> result = executeWrite(
                "billPropertyFee",
                request == null ? null : request.idempotencyKey(),
                request,
                (Class<List<PropertyFeeBillRecord>>) (Class<?>) List.class,
                "finance_property_fee.billed",
                "finance_property_fee.bill_failed",
                "finance.property_fee.manage",
                "property_fee_bill",
                () -> billPropertyFeeInternal(request));
        return result;
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.refund.create", admissionAction = "finance.refund", objectType = "finance_refund")
    public FinanceRefundRecord createRefund(RefundRequest request) {
        FinanceRefundRecord prepared = executeWrite(
                "createFinanceRefund",
                request == null ? null : request.idempotencyKey(),
                request,
                FinanceRefundRecord.class,
                "finance_refund.created",
                "finance_refund.create_failed",
                "finance.refund.create",
                "finance_refund",
                () -> refund(request)
        );
        return dispatchPreparedRefund(prepared);
    }

    /**
     * AUD-060 储值退款打款台账：储值退款在渠道未真正兑付（RESERVED/NOT_CONFIGURED/PENDING）时登记一笔
     * status=requested 的退款待办（内部/手工确认打款），复用 finance_refunds 状态机。target_id 承载储值账户 id
     * （该列无外键）。不复用 {@link #createRefund}：其内部强制 target 为 ar_ap_payment 单据，储值退款无 ar_ap 单据。
     * 仅供储值域服务内部调用（调用方已过 customer.stored_value.refund 权限闸并在同一事务/审计留痕），不暴露控制器入口。
     * 幂等：id 由储值退款幂等键派生；重放先核对原来源及金额，保留原支付状态，不能重置已经兑付的待办。
     */
    public String recordStoredValuePayoutRefund(String accountId, long payoutCent, String reason,
            String merchantId, String projectId, String idempotencyKey) {
        String targetId = requireText(accountId, "account_id");
        long amount = requirePositive(payoutCent, "payout_cent");
        String sourceProject = trimToNull(projectId);
        // TK-21-R3：客人自助注销（客人 token 主体）为系统路径，按本人卡登记待退，不过员工的商户/项目范围闸。
        boolean customerSelfService = "customer".equals(authorizationService.currentActor().accountType());
        if (!customerSelfService) checkMerchantOwnership(merchantId);
        String reasonText = trimToDefault(reason, "储值本金待退");
        String id = storedValuePayoutRefundId(idempotencyKey);
        Optional<FinanceRefundRecord> existing = repository.findRefundById(id, true);
        if (existing.isPresent()) {
            FinanceRefundRecord original = existing.get();
            if (!"stored_value_account".equals(original.sourceType()) || !targetId.equals(original.targetId())
                    || original.refundAmountCent() != amount || !reasonText.equals(original.reason())
                    || !java.util.Objects.equals(merchantId, original.merchantId())
                    || (sourceProject != null && !sourceProject.equals(original.projectId()))) {
                throw ApiException.conflict("IDEMPOTENCY_KEY_CONFLICT", "该退款编号已用于其他来源或金额");
            }
            if (!customerSelfService) checkProjectScope(original.projectId());
            return original.id();
        }
        String project = sourceProject;
        if (project == null && trimToNull(merchantId) != null) {
            List<String> projects = repository.listMerchantOperatingProjectIds(merchantId);
            if (projects.size() == 1) project = projects.getFirst();
        }
        // 商户卡可无独立项目。无法唯一归属时登记待核实，不拿调用者项目补写原卡，也不允许出款。
        if (!customerSelfService) checkProjectScope(project);
        Instant now = Instant.now();
        FinanceRefundRecord saved = repository.saveRefund(new FinanceRefundRecord(
                id, targetId, amount, reasonText, "original", DEFAULT_REFUND_DIRECTION, "requested", now,
                "stored_value_account", merchantId, project,
                authorizationService.currentActor().id(), null, null, null, now, null));
        return saved.id();
    }

    private String storedValuePayoutRefundId(String idempotencyKey) {
        return "refund_sv_" + stableToken("stored_value_refund", requireText(idempotencyKey, "idempotency_key"));
    }

    /** TK-21：按储值退款幂等键找已登记的储值待退事项（账户关闭/注销登记的 svclose_ 事项），无则空。仅供储值域内部调用。 */
    public Optional<FinanceRefundRecord> findStoredValuePayoutRefund(String idempotencyKey) {
        return repository.findRefundById(storedValuePayoutRefundId(idempotencyKey), false)
                .filter(refund -> "stored_value_account".equals(refund.sourceType()));
    }

    /**
     * TK-21-R2（BSM「储值」：「沿原退款审批和原收款路径执行；实际退款成功才结清」；CG-06b 第 16 条「商户按原退款流程实际退款」）：
     * 发卡商户柜台退卡成功＝该商户卡待退事项的实际退款成功，事项置 success（gateway_ref＝兑付凭证号、succeeded_at＝现在），
     * 并留一条 confirm 办理记录。只在储值域柜台退卡事务内调用；不触碰客人账号状态。
     */
    public FinanceRefundRecord settleStoredValuePayoutRefundAtCounter(FinanceRefundRecord pending, String redemptionVoucherNo) {
        FinanceRefundRecord refund = repository.findRefundById(pending.id(), true).orElse(pending);
        if ("success".equals(refund.status())) return refund;
        String voucher = requireText(redemptionVoucherNo, "redemption_voucher_no");
        repository.appendRefundAttempt(new FinanceRefundAttemptRecord(
                "refund_attempt_" + UUID.randomUUID().toString().replace("-", ""), refund.id(),
                "confirm", "success", "merchant_counter", voucher, "发卡商户柜台退卡结清",
                voucher, authorizationService.currentActor().id(), null, List.of(), Instant.now()));
        return saveRefundState(refundState(refund, "success", "merchant_counter", voucher));
    }

    /** 经销域已完成原审批及到账核实；仅登记待办，执行和结果仍走统一财务凭证流程。 */
    @Transactional
    public String recordDealerOrderRefund(String dealerRefundId, long amountCent, String channel,
            String originalPaymentRef, String projectId, String reason) {
        String target = requireText(dealerRefundId, "经销退款编号");
        String project = requireText(projectId, "经销退款所属项目");
        String sourcePermission = dealerRefundExecutionPermission(target, project, false);
        authorizationService.requirePermission(sourcePermission);
        if (!authorizationService.canReachProject(authorizationService.currentActor(), project, sourcePermission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "经销退款不在授权项目范围内");
        }
        long amount = requirePositive(amountCent, "退款金额");
        String route = requireEnum(channel, Set.of("bank_transfer", "platform_online"), "原收款渠道");
        String reference = requireText(originalPaymentRef, "原收款流水号");
        String id = "refund_dealer_" + stableToken("dealer_order_refund", target);
        repository.lockRefundSource(id);
        Optional<FinanceRefundRecord> existing = repository.findRefundById(id, true);
        if (existing.isPresent()) {
            FinanceRefundRecord original = existing.get();
            if (!"dealer_order_refund".equals(original.sourceType()) || !target.equals(original.targetId())
                    || amount != original.refundAmountCent() || !route.equals(original.refundChannel())
                    || !reference.equals(original.originalPaymentRef()) || !project.equals(original.projectId())) {
                throw ApiException.conflict("DEALER_REFUND_SOURCE_MISMATCH", "同一经销退款的原收款、金额或项目不一致");
            }
            return original.id();
        }
        Instant now = Instant.now();
        return repository.saveRefund(new FinanceRefundRecord(id, target, amount, requireText(reason, "reason"), route, "original",
                "requested", now, "dealer_order_refund", null, project, authorizationService.currentActor().id(), reference, null, null, now, null)).id();
    }

    /** 停售后已收款只登记原路待退，未知渠道结果仍由财务凭证双人核实。内部调用与拒单同事务。 */
    @Transactional
    public String recordRejectedTicketPaymentRefund(String sourceType, String sourceId, long amountCent,
            String channel, String originalPaymentRef, String merchantId, String originalBarId, String reason) {
        return recordRejectedTicketPaymentRefund(sourceType, sourceId, amountCent, channel,
                originalPaymentRef, merchantId, originalBarId, null, reason);
    }

    /** originalProjectId 只能来自原订单售票快照或本次渠道票种的受信项目解析。 */
    @Transactional
    public String recordRejectedTicketPaymentRefund(String sourceType, String sourceId, long amountCent,
            String channel, String originalPaymentRef, String merchantId, String originalBarId,
            String originalProjectId, String reason) {
        String source = requireEnum(sourceType, Set.of("ticket_payment_rejection", "channel_ticket_rejection"), "退款来源");
        String target = requireText(sourceId, "原付款记录");
        long amount = requirePositive(amountCent, "原收款金额");
        String route = requireText(channel, "原收款渠道");
        String reference = trimToNull(originalPaymentRef);
        String project = trimToNull(originalProjectId);
        checkMerchantOwnership(merchantId);
        String id = "refund_rejected_" + stableToken(source, target);
        repository.lockRefundSource(id);
        Optional<FinanceRefundRecord> existing = repository.findRefundById(id, true);
        if (existing.isPresent()) {
            FinanceRefundRecord original = existing.get();
            if (!source.equals(original.sourceType()) || !target.equals(original.targetId())
                    || amount != original.refundAmountCent() || !route.equals(original.refundChannel())
                    || !java.util.Objects.equals(merchantId, original.merchantId())
                    || !java.util.Objects.equals(reference, original.originalPaymentRef())) {
                throw ApiException.conflict("IDEMPOTENCY_KEY_CONFLICT", "同一原付款的退款来源或金额不一致");
            }
            checkProjectScope(original.projectId());
            return original.id();
        }
        String originalScopeMerchant = trimToNull(originalBarId) != null ? originalBarId : merchantId;
        if (project == null && trimToNull(originalScopeMerchant) != null) {
            List<String> projects = repository.listMerchantOperatingProjectIds(originalScopeMerchant);
            if (projects.size() == 1) project = projects.getFirst();
        }
        checkProjectScope(project);
        Instant now = Instant.now();
        return repository.saveRefund(new FinanceRefundRecord(id, target, amount, requireText(reason, "退款原因"),
                route, "original", "requested", now, source, merchantId, project,
                authorizationService.currentActor().id(), reference, null, null, now, null)).id();
    }

    /**
     * AUD-059 平台代收商户卡消费逐笔结算：platform_collect 模式商户卡消费时，生成一条「平台应付商户」台账（逐笔可追溯）。
     * 复用 ar_ap_items（direction=payable、counterparty=merchantId、source_type=stored_value_consume、source_id=消费流水id）；
     * 幂等靠 uq_ar_ap_items_direction_source（direction,source_type,source_id）+ id 派生自消费流水，重放零重复分录；
     * merchant scope=该发卡商户；遵守日结锁（与 recordInternalAccountingRevenue 同口径，按该商户 scope 判锁）。
     * 仅供储值域服务内部调用（调用方已过 customer.stored_value.consume / 结账权限闸并在同一事务/审计留痕），不暴露控制器入口。
     */
    public ArApItemRecord recordStoredValueMerchantPayable(String merchantId, String consumeTxnId,
                                                           long amountCent, String idempotencyKey) {
        String mid = trimToNull(merchantId);
        String txnId = trimToNull(consumeTxnId);
        if (mid == null || txnId == null || amountCent <= 0) {
            return null;
        }
        String key = slug(txnId);
        String itemId = "arap_svpay_" + key;
        Instant now = Instant.now();
        LocalDate businessDate = LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        // 遵守日结锁：已封账经营日禁止新增应付，走补录审批（按该商户 scope 判锁，与内部核算/createItem 同口径）。
        if (repository.lockedDailyCloseExistsForDate(businessDate, mid)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "营业日 " + businessDate + " 已锁定，请走补录调整流程");
        }
        ArApItemRecord saved = repository.saveArApItem(new ArApItemRecord(
                itemId,
                "SVPAY-" + key,
                "payable",
                mid,
                "stored_value_consume",
                txnId,
                amountCent,
                0L,
                amountCent,
                CURRENCY_CNY,
                businessDate,
                null,
                "pending_payment",
                now,
                now,
                mid));
        repository.saveArApItemSource(new ArApItemSourceRecord(
                "arap_src_svpay_" + key, itemId, "stored_value_consume", txnId, amountCent, now));
        return saved;
    }

    /**
     * TK-20-R1 平台代收商户卡消费退款回卡：按回冲额对原「平台应付商户」追加一条冲正行（V344 红冲范式：回指原行 origin_item_id、
     * 沿用原行方向/往来对象/来源单据、金额严格取反、status=reversed），原行不动；按来源聚合即净成扣除回冲后的应付。
     * 原消费未挂应付（商户预收 / 旧卡未定格）则无可冲，返回 null。仅供储值域服务内部调用。
     */
    public ArApItemRecord recordStoredValueMerchantPayableReversal(String merchantId, String consumeTxnId,
                                                                   String reversalTxnId, long amountCent) {
        String mid = trimToNull(merchantId);
        String txnId = trimToNull(consumeTxnId);
        if (mid == null || txnId == null || amountCent <= 0) {
            return null;
        }
        ArApItemRecord origin = repository.findArApItemById("arap_svpay_" + slug(txnId)).orElse(null);
        if (origin == null) {
            return null;
        }
        Instant now = Instant.now();
        LocalDate businessDate = businessDayResolver.businessDate(now);  // 复核 money m3：钱账按营业日（ENUM L187），不按 UTC
        if (repository.lockedDailyCloseExistsForDate(businessDate, mid)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "营业日 " + businessDate + " 已锁定，请走补录调整流程");
        }
        String key = slug(requireText(reversalTxnId, "reversal_txn_id"));
        return repository.appendArApItem(new ArApItemRecord(
                "arap_svrev_" + key,
                "SVREV-" + key,
                origin.direction(),
                origin.counterpartyId(),
                origin.sourceType(),
                origin.sourceId(),
                -amountCent,
                0L,
                -amountCent,
                CURRENCY_CNY,
                businessDate,
                origin.accountingPeriod(),
                "reversed",
                now,
                now,
                origin.merchantId(),
                origin.projectId(),
                origin.id(),
                null));
    }

    /**
     * TK-29-R3（BSM L1145 三种情形分别按锁定合同出账；L1144 ①由娱乐平台公司售票收款后分给传媒公司）：某场某费项的结算行出账——
     * 应付传媒公司（counterparty=传媒公司主体 be_media）；②③付方是酒吧（merchantId=酒吧，挂酒吧账本），①付方是娱乐平台公司（merchantId 为空）。
     * source_type=media_performance_settlement、source_id=结算行 id；id 由结算行 id 派生，重放命中既有行即返回（零重复分录）；
     * 遵守日结锁（按付方商户 scope 判锁）。仅供结算域服务内部调用（调用方已过 media.performance.settle 权限闸并在同一事务）。
     */
    public ArApItemRecord postMediaPerformancePayable(String settlementId, String payeeEntityId, String payerMerchantId,
                                                       String projectId, long amountCent) {
        String key = slug(requireText(settlementId, "settlement_id"));
        String itemId = "arap_mps_" + key;
        Optional<ArApItemRecord> existing = repository.findArApItemById(itemId);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "演出结算出账金额必须大于 0");
        }
        String payee = requireText(payeeEntityId, "payee_entity_id");
        String merchantId = trimToNull(payerMerchantId);
        Instant now = Instant.now();
        LocalDate businessDate = businessDayResolver.businessDate(now);  // 复核 money m3：按营业日，不按服务器时区
        if (repository.lockedDailyCloseExistsForDate(businessDate, merchantId)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "营业日 " + businessDate + " 已锁定，请走补录调整流程");
        }
        ArApItemRecord saved = repository.saveArApItem(new ArApItemRecord(
                itemId, "MPS-" + key, "payable", payee, "media_performance_settlement", settlementId,
                amountCent, 0L, amountCent, CURRENCY_CNY, businessDate, null, "pending_payment", now, now,
                merchantId, trimToNull(projectId)));
        repository.saveArApItemSource(new ArApItemSourceRecord(
                "arap_src_mps_" + key, itemId, "media_performance_settlement", settlementId, amountCent, now));
        return saved;
    }

    /**
     * TK-29-R3（BSM L1145「已形成结算需要调整时，关联原记录并按既有历史调整规则办理，不覆盖原结果」）：调整时对原演出结算应付按 V344
     * 红冲范式追加冲正行（回指原行 origin_item_id、金额严格取反、status=reversed），原行不动；新金额由新结算行另挂。
     */
    public ArApItemRecord reverseMediaPerformancePayable(String originItemId, String adjustingSettlementId) {
        ArApItemRecord origin = repository.findArApItemById(requireText(originItemId, "origin_item_id"))
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "原演出结算应付不存在"));
        String key = slug(requireText(adjustingSettlementId, "adjusting_settlement_id"));
        Optional<ArApItemRecord> existing = repository.findArApItemById("arap_mpsrev_" + key);
        if (existing.isPresent()) {
            return existing.get();
        }
        Instant now = Instant.now();
        LocalDate businessDate = businessDayResolver.businessDate(now);  // 复核 money m3：按营业日，不按服务器时区
        if (repository.lockedDailyCloseExistsForDate(businessDate, origin.merchantId())) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "营业日 " + businessDate + " 已锁定，请走补录调整流程");
        }
        return repository.appendArApItem(new ArApItemRecord(
                "arap_mpsrev_" + key, "MPSREV-" + key, origin.direction(), origin.counterpartyId(), origin.sourceType(),
                origin.sourceId(), -origin.amountCent(), 0L, -origin.amountCent(), CURRENCY_CNY, businessDate,
                origin.accountingPeriod(), "reversed", now, now, origin.merchantId(), origin.projectId(), origin.id(), null));
    }

    public PageResponse<DailyCloseSummaryRecord> listDailyCloseSummaries(String dailyCloseId) {
        String id = requireText(dailyCloseId, "daily_close_id");
        return readPage(
                "finance.daily_close_summary.viewed",
                "finance.daily_close_summary.view_failed",
                "finance.daily_close.manage",
                "finance_daily_close_summary",
                () -> {
                    DailyCloseRecord dailyClose = repository.findDailyCloseById(id)
                            .orElseThrow(() -> ApiException.notFound("DAILY_CLOSE_NOT_FOUND", "日结记录不存在或已被删除"));
                    // 隔离：商户角色只能看自身 scope 的日结汇总。
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent()
                            && "merchant".equals(dailyClose.scopeType())
                            && !scope.get().equals(dailyClose.scopeId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "无权查看其他商户的日结记录");
                    }
                    return repository.listDailyCloseSummaries(id);
                });
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.locked_backfill.request")
    @Transactional
    public ArApAdjustmentRecord requestAdjustment(ArApAdjustmentRequest request) {
        return executeWrite(
                "requestArApAdjustment",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApAdjustmentRecord.class,
                "finance.locked_backfill_requested",
                "finance.locked_backfill_request_failed",
                "finance.locked_backfill.request",
                "ar_ap_adjustment",
                () -> createAdjustment(request));
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.locked_backfill.approve", admissionAction = "finance.locked_backfill_approve", objectType = "ar_ap_adjustment")
    @Transactional
    public ArApAdjustmentRecord approveAdjustment(String adjustmentId, ArApAdjustmentDecisionRequest request) {
        String id = requireText(adjustmentId, "adjustment_id");
        String decision = requireEnum(request == null ? null : request.decision(), ADJUSTMENT_DECISIONS, "decision");
        boolean approve = "approve".equals(decision);
        return executeWrite(
                "decideArApAdjustment:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                ArApAdjustmentRecord.class,
                approve ? "finance.locked_backfill_approved" : "finance.locked_backfill_rejected",
                "finance.locked_backfill_decision_failed",
                "finance.locked_backfill.approve",
                "ar_ap_adjustment",
                () -> decideAdjustment(id, approve));
    }

    private ArApAdjustmentRecord createAdjustment(ArApAdjustmentRequest request) {
        ArApAdjustmentRequest actual = requireRequest(request);
        String itemId = requireText(actual.arApItemId(), "ar_ap_item_id");
        long amountCent = requireNonZero(actual.adjustmentAmountCent(), "adjustment_amount_cent");
        String reason = requireText(actual.reason(), "reason");

        ArApItemRecord item = repository.findArApItemByIdForUpdate(itemId)
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "应收应付款项不存在或已被删除"));
        rejectPayrollMutation(item);
        // FS-021-R6：锁账补录按项目范围与商户交集办理。
        requireArApScope(item, "finance.locked_backfill.request");
        requireCurrentPurchasePayable(item);

        // 补录只针对已锁定台账（未锁定应直接改账，不走审批）。
        String lockedDayCloseId = repository.findArApItemLockedDayCloseId(itemId)
                .orElseThrow(() -> ApiException.badRequest("AR_AP_ITEM_NOT_LOCKED",
                        "该应收应付款项未被锁定，请直接修改，无需补录调整"));

        Instant now = Instant.now();
        return repository.saveArApAdjustment(new ArApAdjustmentRecord(
                "arap_adj_" + stableToken("ar_ap_adjustment", itemId + ":" + actual.idempotencyKey()),
                itemId,
                lockedDayCloseId,
                amountCent,
                "pending",
                authorizationService.currentActor().id(),
                null,
                reason,
                now,
                null));
    }

    private ArApAdjustmentRecord decideAdjustment(String adjustmentId, boolean approve) {
        ArApAdjustmentRecord adjustment = repository.findArApAdjustmentByIdForUpdate(adjustmentId)
                .orElseThrow(() -> ApiException.notFound("ADJUSTMENT_NOT_FOUND", "调整单不存在或已被删除"));
        if (!"pending".equals(adjustment.approvalStatus())) {
            throw ApiException.conflict("ADJUSTMENT_ALREADY_DECIDED",
                    "调整单当前状态已为：" + adjustment.approvalStatus());
        }

        ArApItemRecord item = repository.findArApItemByIdForUpdate(adjustment.arApItemId())
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "应收应付款项不存在或已被删除"));
        rejectPayrollMutation(item);
        requireArApScope(item, "finance.locked_backfill.approve");

        Instant now = Instant.now();
        String approver = authorizationService.currentActor().id();

        // F-OPS-07 职责分离（SoD）：补录审批是唯一涉及资金的 finance 审批链（locked backfill：审批通过会写入
        // ar_ap_items 金额变动）。禁止"发起人 = 审批人"自审自批——无论 approve 还是 reject，决策人都不得是该
        // 工单的发起人（requested_by），否则发起人可绕过第二人复核自行放行（或自行撤销）资金调整。
        // 发起人信息可得：requestedBy 在 createAdjustment 时由 currentActor().id() 写入、永不为空。
        if (approver != null && approver.equals(adjustment.requestedBy())) {
            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN",
                    "补录调整的申请人不能自行审批，须由其他审批人处理");
        }

        if (approve) {
            requireCurrentPurchasePayable(item);
            // P0-B（V344）红冲：审批通过不再改写原行，改为「原行不动 + 追加一条严格取反的冲正行
            // + 追加一条重述行（原额 + 补录额）」。三行相加恒等于补录后的应有金额（守恒），
            // 且原行的金额与其所属封账经营日永久留痕、事后可逐笔复核。
            // （改造前是直接 UPDATE 原行；且那条 UPDATE 的 SET 列表不含 amount_cent，
            //   真库会落出 paid+unpaid<>amount 从而被 V159 账平 CHECK 拒绝——该路径在真库从未跑通。）
            long restatedAmountCent = item.amountCent() + adjustment.adjustmentAmountCent();
            if (restatedAmountCent <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "调整后金额将为负数，不允许");
            }
            long restatedUnpaidCent = restatedAmountCent - item.paidAmountCent();
            if (restatedUnpaidCent < 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "调整后未付金额将为负数，不允许");
            }
            // ① 冲正行：原行三个金额列的严格取反（DB 侧 chk_ar_ap_items_amount_balance 对正负两侧同样成立）。
            repository.appendArApItem(correctionItem(item, adjustment, now, true,
                    -item.amountCent(), -item.paidAmountCent(), -item.unpaidAmountCent()));
            // ② 重述行：补录后的应有金额；已收付部分原样承接，未收付部分随之重算。
            ArApItemRecord restated = correctionItem(item, adjustment, now, false,
                    restatedAmountCent, item.paidAmountCent(), restatedUnpaidCent);
            repository.appendArApItem(restated);
            // 第三轮裁决第 4 条「差额收齐后→「已结清」」：红冲＋重述同样改了未清额（如 BSM L1322 账单异议「异议成立后多退少补」），
            // 与登记收付款同一事务回调合同域复核退租结算单（回调入参＝重述行）。
            if (settlementProgress != null) {
                settlementProgress.onArApItemPaid(restated);
            }
        }
        // reject：不改账，只标状态。
        return repository.updateArApAdjustment(new ArApAdjustmentRecord(
                adjustment.id(),
                adjustment.arApItemId(),
                adjustment.lockedDayCloseId(),
                adjustment.adjustmentAmountCent(),
                approve ? "approved" : "rejected",
                adjustment.requestedBy(),
                approver,
                adjustment.reason(),
                adjustment.createdAt(),
                now));
    }

    /**
     * P0-B（V344）红冲派生行构造：冲正行（reversal=true，金额取反）与重述行（reversal=false，补录后金额）。
     * 两行都刻意沿用原行的收付方向 / 往来对象 / 来源单据 / 到期日 / 会计期间 / 商户与项目归属——
     * 这样按任一维度聚合时「原行 + 冲正行 + 重述行」天然净成补录后的金额，下游报表零改动即正确；
     * 唯一识别靠 origin_item_id（回指原行）与 adjustment_id（回指补录工单）。
     * 派生行不继承 locked_day_close_id：它们是当期新增的更正凭证，可继续正常收付，不再被原封账锁冻结。
     */
    private ArApItemRecord correctionItem(ArApItemRecord origin, ArApAdjustmentRecord adjustment, Instant now,
                                          boolean reversal, long amountCent, long paidCent, long unpaidCent) {
        String status = reversal
                ? "reversed"
                : (unpaidCent == 0
                        ? "settled"
                        : ("receivable".equals(origin.direction()) ? "pending_collection" : "pending_payment"));
        return new ArApItemRecord(
                (reversal ? "arap_rev_" : "arap_res_") + adjustment.id(),
                correctionItemNo(origin.itemNo(), reversal ? "-CZ-" : "-BL-", adjustment.id()),
                origin.direction(),
                origin.counterpartyId(),
                origin.sourceType(),
                origin.sourceId(),
                amountCent,
                paidCent,
                unpaidCent,
                origin.currency(),
                origin.dueDate(),
                origin.accountingPeriod(),
                status,
                now,
                now,
                origin.merchantId(),
                origin.projectId(),
                origin.id(),
                adjustment.id());
    }

    /** 派生行单据号 = 原单据号 + 红冲/重述标记 + 工单 id（工单 id 全局唯一 → 单据号天然不撞）；超长退化为不带原单据号的短式。 */
    private String correctionItemNo(String originItemNo, String marker, String adjustmentId) {
        String candidate = (originItemNo == null || originItemNo.isBlank() ? "ARAP" : originItemNo)
                + marker + adjustmentId;
        return candidate.length() <= 128 ? candidate : ("ARAP" + marker + adjustmentId);
    }

    private void requireNotLocked(String arApItemId) {
        if (repository.findArApItemLockedDayCloseId(arApItemId).isPresent()) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "该应收应付款项已被日结锁定，请走补录调整流程");
        }
    }

    private long requireNonZero(Long value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        if (value == 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为 0");
        }
        return value;
    }

    private ArApItemRecord createItem(ArApItemRequest request, boolean overrideProjectScope, String overrideProjectId) {
        ArApItemRequest actual = requireRequest(request);
        String direction = requireEnum(actual.direction(), DIRECTIONS, "direction");
        String sourceType = optionalEnum(actual.sourceType(), SOURCE_TYPES, "source_type");
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");
        LocalDate dueDate = requireDate(actual.dueDate(), "due_date");
        // F-OPS-09：用本条目将落账的 merchant scope 判锁，与下方 ArApItemRecord.merchantId 同口径——
        // 商户 scope 仅受本商户封账约束，平台口径受任一封账约束。
        Optional<String> scope = authorizationService.currentMerchantScope();
        // P1-A：落账即写入综合体维度(与 merchantId 同来源=创建方 token 的 scope),历史行由 V243 回填。
        // 🔴 P2(2026-07-21):调用方可显式指定项目归属覆盖 token 项目域——结算过账按 source_type 派生
        //   (ticket_distribution=bill.projectId、platform_incentive_subsidy=平台域 NULL),不再继承过账人 token 域,
        //   防项目域管理员经网关过账把 td/pis 两类行错打成其项目(下游项目公司聚合会误吸补贴/错算)。
        //   Optional.ofNullable 天然表达「显式平台 NULL」:override=true 且 overrideProjectId=null → empty → project_id=NULL。
        Optional<String> projectScope = overrideProjectScope
                ? Optional.ofNullable(overrideProjectId)
                : authorizationService.currentProjectScopeQuietly();
        // R5 T7 硬拦截：禁止向已锁定经营日新增/回填台账，必须走补录审批。
        if (repository.lockedDailyCloseExistsForDate(dueDate, scope.orElse(null))) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "营业日 " + dueDate + " 已锁定，请走补录调整流程");
        }
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        String id = "arap_" + slug;
        ArApItemRecord item = new ArApItemRecord(
                id,
                "ARAP" + now.toEpochMilli() + "-" + slug.substring(0, Math.min(12, slug.length())),
                direction,
                requireText(actual.counterpartyId(), "counterparty_id"),
                sourceType,
                trimToNull(actual.sourceId()),
                amountCent,
                0L,
                amountCent,
                CURRENCY_CNY,
                dueDate,
                trimToNull(actual.accountingPeriod()),
                "receivable".equals(direction) ? "pending_collection" : "pending_payment",
                now,
                now,
                scope.orElse(null),
                projectScope.orElse(null)
        );
        ArApItemRecord saved = repository.saveArApItem(item);
        if (saved.sourceId() != null && saved.sourceType() != null) {
            repository.saveArApItemSource(new ArApItemSourceRecord(
                    "arap_src_" + slug(actual.idempotencyKey()),
                    saved.id(),
                    saved.sourceType(),
                    saved.sourceId(),
                    saved.amountCent(),
                    now
            ));
        }
        return saved;
    }

    private ArApPaymentResponse registerPayment(String arApItemId, ArApPaymentRequest request) {
        ArApPaymentRequest actual = requireRequest(request);
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");
        // 跨模块闭环②a：登记收付款=资金动作高风险，接入 MDM 门控。
        // 现场/工作机收款时请求携带 deviceId → 触发硬门控；后台台账类登记无设备上下文 → 留空安全跳过。
        requireManagedDeviceForFinanceAction(actual.deviceId(), "finance.payment");

        return settleArApItem(
                arApItemId,
                amountCent,
                requireText(actual.paymentMethod(), "payment_method"),
                actual.paymentTime(),
                trimToNull(actual.bankFlowRef()),
                actual.idempotencyKey(), "finance.payment.register");
    }

    /**
     * ar_ap 台账结算回写公共逻辑（FOR UPDATE 行锁防并发双扣 + 跨商户校验 + 锁定日拦截 + 登记 payment + 回写 paid/unpaid/status）。
     * 供 registerPayment（在线/后台登记收付款）与 F1 confirmOffline（线下确认收款关联应收）共用，保证回款口径一致。
     * permission 为 null 仅限系统按结算清单自动落账（退租押金抵扣销账），不按操作人权限范围复核，也不受日结锁定拦截（见下）。
     */
    private ArApPaymentResponse settleArApItem(
            String arApItemId, long amountCent, String paymentMethod,
            Instant paymentTime, String bankFlowRef, String idempotencyKey, String permission) {
        ArApItemRecord current = repository.findArApItemByIdForUpdate(arApItemId)
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "应收应付款项不存在或已被删除"));
        if (permission != null) {
            requireArApScope(current, permission);
        }
        requireCurrentPurchasePayable(current);
        // R5 T7 硬拦截：已锁定台账禁止直接登记收付款，必须走补录审批。系统按终止结算清单自动落账（permission 为 null）不在此限：
        // BSM L2023「日结锁定后不能直接补录影响金额，必须上级审批通过后补录」——终止结算多级审批全部通过即该上级审批，财务结算 PRD L957
        // 「系统按清单自动落账……不另设独立抵扣审批或手动确认入口」；被押金覆盖的欠费正是到期日已日结的账单，拦下即终审整笔回滚。
        if (permission != null) {
            requireNotLocked(current.id());
        }
        // 第四轮裁决第 2 条「作废……尚未收款的预排租金/物业费应收（留痕）」：已有冲正/重述行回指的原行不再是有效款项，不可再登记收付
        // （所有登记入口经此统一回写点：登记收付款、线下确认、押金抵扣、分红抵扣）。
        if (repository.hasCorrectionItems(current.id())) {
            throw ApiException.conflict("AR_AP_ITEM_REVERSED", "该应收应付已被冲正作废，请使用当前有效款项");
        }
        if ("ticket_points_issuance".equals(current.sourceType())) {
            // 月结异议可能在正式台账生成后、当日付款前提出；来源账单仍须保留付款拦截。
            var bill=ticketPointsJdbc.queryForMap("SELECT id,project_id,due_date,visible_at FROM ticket_points_monthly_bills WHERE id=? FOR UPDATE",current.sourceId());
            LocalDate due=current.dueDate();
            LocalDate paymentDay=(paymentTime==null?Instant.now():paymentTime).atZone(com.complex.platform.common.LocalZone.ID).toLocalDate();
            if(paymentDay.isBefore(due) || LocalDate.now(com.complex.platform.common.LocalZone.ID).isBefore(due))
                throw ApiException.conflict("POINTS_BILL_NOT_DUE","尚未到积分账单已锁定的可付日");
            if(ticketPointsJdbc.queryForObject("SELECT count(*) FROM ticket_points_bill_objections WHERE bill_id=? AND status='open'",Integer.class,current.sourceId())>0
                    || ticketPointsJdbc.queryForObject("SELECT count(*) FROM ticket_points_portal_outages WHERE project_id=? AND status='pending_review' AND unavailable_to>?",Integer.class,bill.get("project_id"),bill.get("visible_at"))>0)
                throw ApiException.conflict("POINTS_BILL_REVIEW_PENDING","积分账单有未结异议或门户不可用事实待复核，暂不可登记付款");
        }
        // HP-C06（BSM L1352／ENUM L252「以退租结算算账完成为准执行支付，且不早于等待期届满」「押金退还支付不受三个付账日与账单异议期约束」）：
        // 到期日＝合同域算好的 max(算账完成日, 最后审批完成日＋等待期)；所在地今天、或所登记的付款时间（缺省 now，换所在地日期；第三轮控制席约定，
        // 照 POINTS_BILL_NOT_DUE）早于到期日均拒绝登记付款，不另挂付账日/异议期闸。
        if ("deposit_refund".equals(current.sourceType())) {
            LocalDate paymentDay = (paymentTime == null ? Instant.now() : paymentTime)
                    .atZone(com.complex.platform.common.LocalZone.ID).toLocalDate();
            if (paymentDay.isBefore(current.dueDate())
                    || LocalDate.now(com.complex.platform.common.LocalZone.ID).isBefore(current.dueDate())) {
                throw ApiException.conflict("DEPOSIT_REFUND_NOT_DUE", "押金退还不早于 " + current.dueDate()
                        + " 支付（等待期届满或退租结算算账完成之日），届时方可登记付款");
            }
        }
        // V304 硬拦截：分红发放保护线留存（待释放）应付未释放前禁止付款——「待释放」是真锁不是标签，
        // 须先经释放（re-tag 为 dividend_reserve_released）才可发放给主理人，否则绕过释放规则提前发放留存。
        if ("dividend_reserve_withheld".equals(current.sourceType())) {
            throw ApiException.conflict("DIVIDEND_RESERVE_NOT_RELEASED",
                    "该应付为分红发放保护线留存（待释放），需先释放才能发放给主理人");
        }
        if (amountCent > current.unpaidAmountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "支付金额超过未付金额");
        }
        Instant now = Instant.now();
        String paymentId = "arap_pay_" + stableToken("ar_ap_payment", arApItemId + ":" + idempotencyKey);
        // HP-C03-2（BSM L1353「实际收款或退款成功后才更新已收押金余额……不将未来应收视为到账」）：变更押金补退差额按每笔实收/实付
        // 写 deposit_ledger collect/refund 行并更新已收押金余额；退还超余额拒绝（与押金四动作同一校验）。
        if ("deposit_adjustment".equals(current.sourceType())) {
            recordDepositAdjustmentLedger(current, amountCent, paymentId, now);
        }
        // 主键纳入 arApItemId，同一幂等键用于不同台账项产生不同 paymentId，避免 ON CONFLICT 覆盖。
        ArApPaymentRecord payment = repository.saveArApPayment(new ArApPaymentRecord(
                paymentId,
                current.id(),
                amountCent,
                paymentMethod,
                paymentTime == null ? now : paymentTime,
                bankFlowRef,
                now
        ));
        long paidAmountCent = current.paidAmountCent() + amountCent;
        long unpaidAmountCent = current.amountCent() - paidAmountCent;
        String status = unpaidAmountCent == 0 ? "settled" : "partial_paid";
        ArApItemRecord updated = repository.updateArApItemLedger(new ArApItemRecord(
                current.id(),
                current.itemNo(),
                current.direction(),
                current.counterpartyId(),
                current.sourceType(),
                current.sourceId(),
                current.amountCent(),
                paidAmountCent,
                unpaidAmountCent,
                current.currency(),
                current.dueDate(),
                current.accountingPeriod(),
                status,
                current.createdAt(),
                now,
                current.merchantId(),
                current.projectId(),
                current.originItemId(),
                current.adjustmentId()
        ));
        // 第三轮裁决第 4 条：同一事务内回调合同域推进「押金抵扣中」「欠费追缴中」的退租结算单。
        if (settlementProgress != null) {
            settlementProgress.onArApItemPaid(updated);
        }
        // FS-017：存酒退出承接垫付追收应收结清 → 同一事务内回写补存/赔付记录的追收状态（对客结案不读此状态）。
        if (savedBottleRemedyRecovery != null && "saved_bottle_remedy_recovery".equals(updated.sourceType())
                && "settled".equals(updated.status())) {
            savedBottleRemedyRecovery.onRecoverySettled(updated);
        }
        // 复核 money I2（BSM L1062「退款处理中的记录继续关联原付款，只有实际退款成功才记为已退」）：订台定金达标原路退款应付
        // 实付结清 → 同一事务回写定金为「达标已原路退回」（挂应付时只是退款处理中，定金仍为已支付）。
        if (ticketPointsJdbc != null && "bar_deposit_refund".equals(updated.sourceType()) && "settled".equals(updated.status())) {
            ticketPointsJdbc.update("""
                    UPDATE bar_reservations SET deposit_status='settled', deposit_settlement='refunded_on_threshold',
                           deposit_settled_at=now(), updated_at=now()
                     WHERE id=? AND deposit_status='paid'
                    """, updated.sourceId());
        }
        // 第十一批 TK-04-b（BSM L823「退款成功时取消对应尚待确认积分」）：退款应付实付结清＝退款已实际退出，同一事务通知来源域回写。
        if (refundEvents != null && "refund".equals(updated.sourceType()) && "payable".equals(updated.direction())
                && "settled".equals(updated.status())) {
            refundEvents.publishEvent(new RefundPayableSettled(updated.id(), updated.sourceId()));
        }
        // FS-021-R5（BSM L1683「每笔应付的实际到账结果回写该笔应付」）：确认到账即回写在途那次付款为到账。
        if (ticketPointsJdbc != null) {
            ticketPointsJdbc.update("""
                    UPDATE internal_payable_payment_attempts SET status='succeeded',resolved_at=now(),resolved_by=?
                     WHERE ar_ap_item_id=? AND status='initiated'
                    """, permission == null ? "system" : authorizationService.currentActor().id(), current.id());
        }
        return new ArApPaymentResponse(payment, updated);
    }

    private void recordDepositAdjustmentLedger(ArApItemRecord item, long amountCent, String paymentId, Instant now) {
        String merchantId = item.counterpartyId();
        long balance = repository.currentDepositBalanceForUpdate(merchantId);
        boolean collect = "receivable".equals(item.direction());
        if (!collect && amountCent > balance) {
            throw ApiException.badRequest("DEPOSIT_INSUFFICIENT", "押金退还 " + amountCent + " 超过余额 " + balance);
        }
        repository.saveDepositLedger(new DepositLedgerRecord("deposit_adj_" + stableToken("deposit_adjustment", paymentId),
                merchantId, collect ? "collect" : "refund", amountCent, collect ? balance + amountCent : balance - amountCent,
                item.id(), "变更押金补退差额", now));
    }

    private DailyCloseRecord lockDailyClose(DailyCloseRequest request) {
        DailyCloseRequest actual = requireRequest(request);
        LocalDate businessDate = requireDate(actual.businessDate(), "business_date");
        String scopeType = requireEnum(actual.scopeType(), DAILY_CLOSE_SCOPES, "scope_type");
        String scopeId = requireText(actual.scopeId(), "scope_id");

        // P1 fix: prevent duplicate locks for the same date + scope using different keys
        if (repository.dailyCloseExists(businessDate, scopeType, scopeId)) {
            throw ApiException.conflict("DAILY_CLOSE_EXISTS",
                    "该营业日已完成日结锁定：日期 " + businessDate + "，范围 " + scopeType + "/" + scopeId);
        }
        // TK-14（ENUM「异常状态的统一出口」：异常单据未处置完毕，所属日结不得确认锁定）：票务核销异常待主管处置时拒锁。
        if (repository.openTicketVerificationAnomalyExists(businessDayResolver.startInstant(businessDate),
                businessDayResolver.endInstant(businessDate), scopeType, scopeId)) {
            throw ApiException.conflict("DAY_CLOSE_ANOMALY_PENDING",
                    "该营业日有待主管处置的票务核销异常，处置完毕后才能确认日结锁定");
        }
        Instant now = Instant.now();
        String dailyCloseId = "daily_close_" + slug(actual.idempotencyKey());
        DailyCloseRecord saved = repository.saveDailyClose(new DailyCloseRecord(
                dailyCloseId,
                businessDate,
                scopeType,
                scopeId,
                actual.forceClose() != null && actual.forceClose(),
                "locked",
                now,
                now
        ));

        // R5 T7：六源汇总写入 finance_daily_close_summaries（口径见 r5-aggregation-metrics-draft §1.2，待财务复核）。
        summarizeDailyClose(dailyCloseId, businessDate, now, scopeType, scopeId);

        // R5 T7：把当日属于本 scope 的 ar_ap_items 打上 locked_day_close_id（merchant 角色仅锁本商户行）。
        Optional<String> merchantScope = authorizationService.currentMerchantScope();
        repository.lockArApItemsForDailyClose(dailyCloseId, businessDate, merchantScope.orElse(null));

        return saved;
    }

    /**
     * 六源汇总聚合（official_ticket / channel_ticket / bar / amusement / supply_cost / thirdparty_reflow），
     * 每源一行写入 finance_daily_close_summaries。聚合公式按聚合草案保守取值，口径未定处见草案 §四"待财务复核"。
     */
    private void summarizeDailyClose(String dailyCloseId, LocalDate businessDate, Instant now,
                                    String scopeType, String scopeId) {
        // 经营日窗口按可配日切（默认 04:00）派生，时间戳类源表统一用 [from, to) 分桶；official_ticket 以 visit_date 为经营日。
        Instant from = businessDayResolver.startInstant(businessDate);
        Instant to = businessDayResolver.endInstant(businessDate);
        for (String source : DAILY_CLOSE_SOURCES) {
            FinanceRepository.DailyCloseSourceTotals totals = switch (source) {
                case "official_ticket" -> repository.aggregateOfficialTicket(businessDate, from, to, scopeType, scopeId);
                case "channel_ticket" -> repository.aggregateChannelTicket(businessDate, from, to, scopeType, scopeId);
                case "bar" -> repository.aggregateBar(businessDate, from, to, scopeType, scopeId);
                case "amusement" -> repository.aggregateAmusement(businessDate, from, to, scopeType, scopeId);
                case "supply_cost" -> repository.aggregateSupplyCost(businessDate, from, to, scopeType, scopeId);
                case "thirdparty_reflow" -> repository.aggregateThirdpartyReflow(businessDate, from, to, scopeType, scopeId);
                default -> FinanceRepository.DailyCloseSourceTotals.empty();
            };
            repository.saveDailyCloseSummary(new DailyCloseSummaryRecord(
                    "fdcs_" + stableToken("daily_close_summary", dailyCloseId + ":" + source),
                    dailyCloseId,
                    source,
                    totals.amountCent(),
                    totals.refundCent(),
                    totals.orderCount(),
                    now
            ));
        }
        auditEventService.recordSuccess(
                "finance.daily_close_summarized",
                "finance.daily_close.manage",
                "finance_daily_close_summary",
                dailyCloseId,
                null,
                null,
                Map.of("source_count", DAILY_CLOSE_SOURCES.size()));
    }

    private PaymentIntentRecord createIntent(PaymentIntentRequest request) {
        PaymentIntentRequest actual = requireRequest(request);
        // 跨模块闭环②a：支付意图创建=支付高风险动作，接入 MDM 门控。
        // 现场/工作机发起时请求携带 deviceId → 触发硬门控；后台/服务端无设备上下文 → 留空安全跳过。
        requireManagedDeviceForFinanceAction(actual.deviceId(), "finance.payment");
        Instant now = Instant.now();
        String businessOrderType = requireText(actual.businessOrderType(), "business_order_type");
        String businessOrderId = requireText(actual.businessOrderId(), "business_order_id");
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");
        String currency = trimToDefault(actual.currency(), CURRENCY_CNY);
        String channel = PaymentChannelPolicy.requirePaymentIntentChannel(actual.channel());

        // 支付渠道开关（payment.gateway.enabled，后台可配，默认关）。
        //   关 → 不调外部渠道，支付意图标 reserved（占位，由 markPaymentIntentSucceeded 内部确认，不阻塞）。
        //   开 → 路由到 PaymentGatewayPort（当前无真实厂商=占位 NOT_CONFIGURED，留接缝，不崩）。
        // AUD-013：网关请求携带收款商户与定格资金模式（无则为 null=平台收款），真实适配器据此路由收款主体。
        com.complex.platform.finance.gateway.PaymentGatewayResult gw = paymentGateway.initiatePayment(
                new com.complex.platform.finance.gateway.PaymentGatewayRequest(
                        businessOrderType, businessOrderId, amountCent, currency, channel, null,
                        trimToNull(actual.payeeMerchantId()), trimToNull(actual.fundMode())));

        return repository.savePaymentIntent(new PaymentIntentRecord(
                "pay_intent_" + slug(actual.idempotencyKey()),
                businessOrderType,
                businessOrderId,
                amountCent,
                currency,
                channel,
                "created",
                gw.gatewayProvider(),
                gw.gatewayRef(),
                gw.status().name(),
                now,
                now,
                gw.requiresInternalFallback() ? null : gw.paymentUrl()
        ));
    }

    private OfflinePaymentConfirmationRecord confirmOffline(
            String pathPaymentId,
            OfflinePaymentConfirmRequest request
    ) {
        OfflinePaymentConfirmRequest actual = requireRequest(request);
        String bodyPaymentId = requireText(actual.paymentId(), "payment_id");
        if (!pathPaymentId.equals(bodyPaymentId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求体中的支付编号必须与路径一致");
        }
        // 2d-2 修复（用户裁定：必须关联内部支付意图）：确认线下收款必须对应一个真实支付意图，
        // 且金额一致——堵"凭空对任意 paymentId 字符串伪造已确认线下收款（任意金额）"。
        // 归属为平台财务口径（payment_intents 无 merchant 列），靠 finance.payment.register 权限治理不授商户角色。
        PaymentIntentRecord intent = repository.findPaymentIntentById(bodyPaymentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在或已被删除"));
        long confirmedAmount = requirePositive(actual.amountCent(), "amount_cent");
        if (confirmedAmount != intent.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "确认金额必须等于支付意向金额");
        }
        Instant now = Instant.now();
        String paymentMethod = requireEnum(actual.paymentMethod(), OFFLINE_PAYMENT_METHODS, "payment_method");
        OfflinePaymentConfirmationRecord confirmation = repository.saveOfflinePaymentConfirmation(
                new OfflinePaymentConfirmationRecord(
                        "offline_confirm_" + slug(actual.idempotencyKey()),
                        pathPaymentId,
                        confirmedAmount,
                        paymentMethod,
                        actual.confirmedAt() == null ? now : actual.confirmedAt(),
                        actual.evidenceAttachmentIds() == null ? List.of() : List.copyOf(actual.evidenceAttachmentIds()),
                        "confirmed",
                        now
                ));
        // F1 财务统一台账：线下确认收款若显式关联应收应付台账项，则同事务回写 ar_ap（登记 payment + paid/unpaid/status），
        // 闭合"线下确认与回款台账平行、ar_ap 永不回写"的资金缺口。回写口径与 registerArApPayment 完全一致（共用 settleArApItem）。
        String arApItemId = trimToNull(actual.arApItemId());
        if (arApItemId != null) {
            settleArApItem(arApItemId, confirmedAmount, paymentMethod,
                    actual.confirmedAt(), null, actual.idempotencyKey(), "finance.payment.register");
        }
        // AUD-014：柜台确认收款=该笔支付成功——同事务内把支付意图 created→succeeded（CAS，已成功幂等跳过），
        // 否则储值充值等依赖意图状态的入账链永远停留 created/409（生产无其他成功触发器）。
        if ("created".equals(intent.status())) {
            markPaymentIntentSucceeded(bodyPaymentId);
        }
        return confirmation;
    }

    /**
     * 跨模块闭环②a：finance 高风险动作（支付意图/退款/高价值兑付）MDM 门控接入点。
     *
     * <p><b>门控语义（device 存在则强制、为空则安全跳过）</b>：相关请求 DTO
     * （{@link RefundRequest}/{@link PaymentIntentRequest}/{@link ArApPaymentRequest}）已携带可空
     * 操作设备标识 {@code deviceId}。按"device_id 存在则门控、不存在则安全跳过"接入（不伪造假值）：
     * <ul>
     *   <li><b>前台现场场景</b>（工作机/现场设备发起退款/收款）请求携带真实 deviceId →
     *       <b>真实启用</b>硬门控：MdmService 开关开 + 设备未纳管/blocked/lost → 硬拒绝；</li>
     *   <li><b>后台台账类场景</b>（服务端/后台财务操作）无设备上下文，deviceId 为 null/空 →
     *       跳过门控，不阻断财务主链（这些操作本就无现场设备，跳过是正确语义而非占位）。</li>
     * </ul>
     * mdmService 为 null（旧兼容构造/未接门控路径）时整体跳过。
     */
    private void requireManagedDeviceForFinanceAction(String deviceId, String actionType) {
        if (mdmService == null) {
            return;
        }
        String id = trimToNull(deviceId);
        if (id == null) {
            // 后台台账类操作无现场设备上下文，安全跳过（不伪造假值）。
            return;
        }
        mdmService.requireManagedForHighRiskAction(id, actionType);
    }

    /**
     * 分级授权闸（适用所有退款）：把退款额按 {@link RefundAuthorizationService#classify} 判档——额度内/规则既定放行，
     * 超额（REVIEW_REQUIRED）因单步退款无独立复核状态机而直接拒（REFUND_REVIEW_REQUIRED），须由额度内的授权人办理。
     *
     * <p>AUD-007 fail-closed：不再受功能标志控制、不再对平台管理员隐式旁路——闸常在；引擎缺失
     * （仅旧兼容/单测构造，生产恒注入）时拒绝而非放行。如需给管理员更高额度，走同一 (subject, domain, project) 显式额度配置 + 审计。
     * <p>AUD-008：projectId 为权威综合体归属，由调用方从原支付/单据的项目归属传入，不信任请求体。
     */
    private void requireRefundTierAuthorized(String domain, long amountCent, String projectId) {
        // AUD-007 fail-closed：不再受功能标志控制、不再对平台管理员隐式旁路——闸常在；引擎缺失
        //   （仅旧兼容/单测构造，生产恒注入）时拒绝而非放行。管理员更高额度走同一显式额度配置 + 审计。
        if (refundAuthorizationService == null) {
            throw ApiException.forbidden("REFUND_AUTHORIZATION_UNAVAILABLE",
                    "退款分级授权引擎不可用，超限退款不能自助处理");
        }
        var actor = authorizationService.currentActor();
        // AUD-008：projectId 由调用方从原支付/单据的权威综合体归属传入，不信任请求体。
        if (refundAuthorizationService.classify(domain, amountCent, actor.id(), null, projectId)
                == RefundAuthorizationService.RefundTier.REVIEW_REQUIRED) {
            throw ApiException.forbidden("REFUND_REVIEW_REQUIRED",
                    "退款金额超过自助授权限额，须由有权限的审批人处理");
        }
    }

    private FinanceRefundRecord refund(RefundRequest request) {
        RefundRequest actual = requireRequest(request);
        String targetId = requireText(actual.targetId(), "target_id");
        long refundAmountCent = requirePositive(actual.refundAmountCent(), "refund_amount_cent");
        // P3：现场渠道(cash/pos)退款由服务端强制要求设备上下文，缺 deviceId 直接拒（不靠请求体可空绕过设备门控）。
        requireDeviceContextForOnsiteChannel(actual.refundChannel(), actual.deviceId());
        // 跨模块闭环②a：退款=高价值兑付高风险动作，接入 MDM 门控。
        // 现场/工作机退款时请求携带 deviceId → 触发硬门控；后台台账类退款无设备上下文 → 留空安全跳过。
        requireManagedDeviceForFinanceAction(actual.deviceId(), "finance.refund");

        // P1 fix: validate target payment exists.
        // P1 修复（TOCTOU）：对 target payment 行加 FOR UPDATE 行锁后再做累计校验，使并发退款串行化，
        // 避免多笔退款各读旧的 sumRefundedAmount 合计后均通过校验、最终退款合计超过 payment 金额。
        ArApPaymentRecord payment = repository.findArApPaymentById(targetId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "支付记录不存在：" + targetId));
        // 同一台账先锁主单再锁收款，后收先退与并发收款使用同一锁顺序。
        ArApItemRecord item = repository.findArApItemByIdForUpdate(payment.arApItemId())
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "应收应付款项不存在或已被删除"));
        payment = repository.findArApPaymentByIdForUpdate(targetId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "支付记录不存在：" + targetId));
        rejectPayrollMutation(item);
        // FS-021-R6（BSM L1673「所有数据默认按项目隔离」；PGM L664 付款执行由财务中心负责）：按项目范围与商户交集办理，
        //   项目财务没有商户编号也能办本项目的退款；他项目拒绝，商户账号仍只限本商户。
        requireArApScope(item, "finance.refund.create");

        // R5 T7 硬拦截：已锁定台账禁止直接退款（影响金额），必须走补录审批。
        requireNotLocked(item.id());

        // P1 fix: prevent refunding more than the original payment (cumulative check)
        long alreadyRefunded = repository.sumRefundedAmountByTargetId(targetId);
        if (alreadyRefunded < 0 || alreadyRefunded > payment.amountCent()
                || refundAmountCent > payment.amountCent() - alreadyRefunded) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "退款金额超过可退余额（支付金额：" + payment.amountCent() + "，已退及待退：" + alreadyRefunded + "）");
        }

        // Stage B 分级授权（功能标志默认关）：超额度财务退款须复核。财务退款为单步台账动作、无独立审批环节，
        // 故超额（REVIEW_REQUIRED）直接拒（REFUND_REVIEW_REQUIRED），由额度内的授权人办理。
        // AUD-008 权威综合体：projectId 取自原支付对应 AR/AP 单据的归属（item.projectId()），而非请求体/页面筛选器。
        requireRefundTierAuthorized("finance", refundAmountCent, item.projectId());

        if (!"receivable".equals(item.direction())) {
            throw ApiException.badRequest("REFUND_REQUIRES_RECEIVABLE", "仅已实际收到的款项可在本入口向原付款人退款");
        }
        String channel = requireText(payment.paymentMethod(), "原收款渠道");
        String requestedChannel = trimToNull(actual.refundChannel());
        if (requestedChannel != null && !channel.equals(requestedChannel)) {
            throw ApiException.conflict("REFUND_CHANNEL_MISMATCH", "退款必须沿原收款渠道办理");
        }
        String direction = optionalEnum(actual.refundDirection(), REFUND_DIRECTIONS, "refund_direction");
        if (direction == null) direction = DEFAULT_REFUND_DIRECTION;
        if (!"original".equals(direction) && !("cash".equals(direction) && "cash".equals(channel))) {
            throw ApiException.conflict("REFUND_ORIGINAL_PATH_REQUIRED", "本入口不得改用非原收款路径退款");
        }
        requireDeviceContextForOnsiteChannel(channel, actual.deviceId());
        // 混合收款后收先退；未结退款也占额，不能改编号重复申请。
        for (ArApPaymentRecord later : repository.listArApPayments(item.id())) {
            if (later.id().equals(payment.id())) break;
            if (repository.sumSucceededRefundAmountByTargetId(later.id()) < later.amountCent()) {
                throw ApiException.conflict("REFUND_LATEST_PAYMENT_FIRST", "须先退完该单后收的款项");
            }
        }
        Instant now = Instant.now();
        FinanceRefundRecord saved = repository.saveRefund(new FinanceRefundRecord(
                "refund_" + stableToken("finance_refund", actual.idempotencyKey()), targetId, refundAmountCent,
                requireText(actual.reason(), "reason"), channel, direction, "requested", now,
                "ar_ap_payment", item.merchantId(), item.projectId(), authorizationService.currentActor().id(),
                trimToNull(payment.bankFlowRef()), null, null, now, null));
        repository.appendRefundAttempt(new FinanceRefundAttemptRecord(
                "refund_attempt_" + UUID.randomUUID().toString().replace("-", ""), saved.id(), "submit", "requested",
                null, null, saved.reason(), saved.originalPaymentRef(), saved.requestedBy(), null,
                actual.evidenceAttachmentIds(), now));
        return saved;
    }

    public PageResponse<FinanceRefundRecord> listFinanceRefunds() {
        authorizationService.requirePermission("finance.ar_ap.view");
        // FS-021-R6：可触及项目与商户交集在 SQL 内过滤（与应收应付列表同口径）。
        return readPage("finance_refund.listed", "finance_refund.list_failed", "finance.ar_ap.view", "finance_refund", () -> {
            var actor = authorizationService.currentActor();
            var reach = authorizationService.projectReach(actor, "finance.ar_ap.view");
            return repository.listRefundsForProjects(reach.platformAdmin() ? null : trimToNull(actor.merchantId()),
                    reach.platformAdmin() ? null : reach.organizationIds());
        });
    }

    public FinanceRefundDetailRecord getFinanceRefund(String refundId) {
        authorizationService.requirePermission("finance.ar_ap.view");
        return operations.readSingle(AuditedOperation.of("finance_refund.viewed", "finance_refund.view_failed",
                "finance.ar_ap.view", "finance_refund"), () -> {
            FinanceRefundRecord refund = scopedRefund(refundId, false);
            return new FinanceRefundDetailRecord(refund, repository.listRefundAttempts(refund.id()));
        }, value -> ((FinanceRefundDetailRecord) value).refund().id());
    }

    public PageResponse<ArApPaymentRecord> listArApPayments(String itemId) {
        authorizationService.requirePermission("finance.ar_ap.view");
        return readPage("ar_ap_payment.listed", "ar_ap_payment.list_failed", "finance.ar_ap.view", "ar_ap_payment", () -> {
            ArApItemRecord item = repository.findArApItemById(itemId)
                    .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "应收应付款项不存在"));
            requireArApScope(item, "finance.ar_ap.view");
            return repository.listArApPayments(item.id());
        });
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.refund.create", admissionAction = "finance.refund", objectType = "finance_refund")
    public FinanceRefundRecord retryFinanceRefund(String refundId, RefundOperationRequest request) {
        authorizationService.requirePermission("finance.refund.create");
        RefundOperationRequest actual = requireRequest(request);
        FinanceRefundRecord prepared = executeWrite("retryFinanceRefund:" + refundId, actual.idempotencyKey(),
                actual, FinanceRefundRecord.class, "finance_refund.retried", "finance_refund.retry_failed",
                "finance.refund.create", "finance_refund", () -> {
                    FinanceRefundRecord refund = scopedRefund(refundId, true, "finance.refund.create");
                    requireRefundOpenForExecution(refund);
                    requireRefundExecutionAuthority(refund, actual.deviceId());
                    if (!canDispatchRefund(refund)) {
                        throw ApiException.conflict("REFUND_ROUTE_UNAVAILABLE", "尚无可自动执行的原收款路线，请核实原路退款待办");
                    }
                    if (repository.findReadyRefundAttempt(refund.id()).isEmpty()) {
                        repository.appendRefundAttempt(new FinanceRefundAttemptRecord(
                                "refund_attempt_" + UUID.randomUUID().toString().replace("-", ""),
                                refund.id(), "retry", "requested", null, null, actual.operatorNote(),
                                refund.originalPaymentRef(), authorizationService.currentActor().id(), null,
                                actual.evidenceAttachmentIds(), Instant.now()));
                    }
                    return saveRefundState(refundState(refund, "requested", null, null));
                });
        return dispatchPreparedRefund(prepared);
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.ar_ap.view")
    @Transactional
    public FinanceRefundRecord queryFinanceRefund(String refundId, RefundOperationRequest request) {
        authorizationService.requirePermission("finance.ar_ap.view");
        RefundOperationRequest actual = requireRequest(request);
        return executeWrite("queryFinanceRefund:" + refundId, actual.idempotencyKey(), actual,
                FinanceRefundRecord.class, "finance_refund.queried", "finance_refund.query_failed",
                "finance.ar_ap.view", "finance_refund", () -> {
                    FinanceRefundRecord refund = scopedRefund(refundId, true);
                    if ("success".equals(refund.status()) || "requested".equals(refund.status())) return refund;
                    FinanceRefundAttemptRecord submission = latestRefundExecution(refund.id());
                    if (submission == null || "execution".equals(submission.operation())) {
                        throw ApiException.conflict("REFUND_QUERY_UNAVAILABLE", "本退款须按已登记的执行凭证核实");
                    }
                    PaymentGatewayResult result;
                    try {
                        result = paymentGateway.queryRefund(refundGatewayRequest(refund, submission), refund.gatewayRef());
                    } catch (RuntimeException exception) {
                        result = null;
                    }
                    return applyRefundGatewayResult(refund, submission, result, true);
                });
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.refund.create", admissionAction = "finance.refund", objectType = "finance_refund")
    @Transactional
    public FinanceRefundRecord recordFinanceRefundExecution(String refundId, RefundExecutionRequest request) {
        authorizationService.requirePermission("finance.refund.create");
        RefundExecutionRequest actual = requireRequest(request);
        requireDealerRefundBeforeReplay(refundId, true, actual.idempotencyKey());
        return executeWrite("recordFinanceRefundExecution:" + refundId, actual.idempotencyKey(), actual,
                FinanceRefundRecord.class, "finance_refund.execution_recorded", "finance_refund.execution_failed",
                "finance.refund.create", "finance_refund", () -> {
                    requireNotMerchantCounterOnlyRefund(refundId);
                    FinanceRefundRecord refund = scopedRefund(refundId, true, "finance.refund.create");
                    requireRefundOpenForExecution(refund);
                    requireDealerRefundSource(refund, true, false);
                    requireRefundExecutionAuthority(refund, actual.deviceId());
                    requireRefundEvidence(refund, actual.evidenceAttachmentIds());
                    String reference = requireText(actual.paymentReference(), "原路执行流水号");
                    requireText(actual.operatorNote(), "执行说明");
                    repository.findReadyRefundAttempt(refund.id()).ifPresent(attempt -> repository.claimRefundAttempt(attempt.id()));
                    String operator = authorizationService.currentActor().id();
                    repository.appendRefundAttempt(new FinanceRefundAttemptRecord(
                            "refund_attempt_" + UUID.randomUUID().toString().replace("-", ""), refund.id(),
                            "execution", "processing", null, null, actual.operatorNote(), reference, operator, null,
                            actual.evidenceAttachmentIds(), Instant.now()));
                    return saveRefundState(refundState(refund, "processing", null, null));
                });
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.refund.create", admissionAction = "finance.refund", objectType = "finance_refund")
    @Transactional
    public FinanceRefundRecord confirmFinanceRefundResult(String refundId, RefundResultConfirmRequest request) {
        authorizationService.requirePermission("finance.refund.create");
        authorizationService.requirePermission("finance.payment.register");
        RefundResultConfirmRequest actual = requireRequest(request);
        requireDealerRefundBeforeReplay(refundId, false, actual.idempotencyKey());
        return executeWrite("confirmFinanceRefundResult:" + refundId, actual.idempotencyKey(), actual,
                FinanceRefundRecord.class, "finance_refund.confirmed", "finance_refund.confirm_failed",
                "finance.payment.register", "finance_refund", () -> {
                    requireNotMerchantCounterOnlyRefund(refundId);
                    FinanceRefundRecord refund = scopedRefund(refundId, true, "finance.payment.register");
                    requireDealerRefundSource(refund, false, true);
                    if ("success".equals(refund.status())) return refund;
                    requireRefundExecutionAuthority(refund, actual.deviceId());
                    String status = requireEnum(actual.status(), Set.of("success", "failed"), "退款结果");
                    if (requirePositive(actual.amountCent(), "退款金额") != refund.refundAmountCent()) {
                        throw ApiException.conflict("REFUND_RESULT_AMOUNT_MISMATCH", "核实金额必须等于本退款金额");
                    }
                    FinanceRefundAttemptRecord execution = latestRefundExecution(refund.id());
                    if (execution == null || !"execution".equals(execution.operation())
                            || !"processing".equals(refund.status())
                            || !java.util.Objects.equals(execution.paymentOperatorId(), actual.paymentOperatorId())
                            || !java.util.Objects.equals(execution.paymentReference(), actual.paymentReference())) {
                        throw ApiException.conflict("REFUND_EXECUTION_MISMATCH", "须关联原执行人员本人登记的同笔退款凭证");
                    }
                    String verifier = authorizationService.currentActor().id();
                    if (verifier.equals(execution.paymentOperatorId())
                            || ("cash".equals(refund.refundChannel()) && verifier.equals(refund.requestedBy()))) {
                        throw ApiException.forbidden("REFUND_SELF_CONFIRM_FORBIDDEN", "退款经办人与结果复核人必须分离");
                    }
                    requireRefundEvidence(refund, actual.evidenceAttachmentIds());
                    requireText(actual.operatorNote(), "核实说明");
                    repository.appendRefundAttempt(new FinanceRefundAttemptRecord(
                            "refund_attempt_" + UUID.randomUUID().toString().replace("-", ""), refund.id(),
                            "confirm", status, "manual_verified", actual.paymentReference(), actual.operatorNote(),
                            actual.paymentReference(), execution.paymentOperatorId(), verifier,
                            actual.evidenceAttachmentIds(), Instant.now()));
                    return saveRefundState(refundState(refund, status, "manual_verified", actual.paymentReference()));
                });
    }

    /**
     * 复核 authz I2（BSM L833「商户卡仍由发卡商户直接收款和退款，生成待退事项不代表平台已经付款」）：账户关闭 / 注销为商户卡登记的
     * 待退本金事项（svclose_）只能由发卡商户柜台退卡结清（StoredValueService.counterRefund），财务登记执行与结果确认一律拒绝，
     * 防止财务结清后柜台再付一次。
     */
    private void requireNotMerchantCounterOnlyRefund(String refundId) {
        repository.findRefundById(refundId, false)
                .filter(refund -> "stored_value_account".equals(refund.sourceType()) && trimToNull(refund.merchantId()) != null)
                .filter(refund -> findStoredValuePayoutRefund("svclose_" + refund.targetId())
                        .map(pending -> pending.id().equals(refund.id())).orElse(false))
                .ifPresent(refund -> {
                    throw ApiException.conflict("STORED_VALUE_MERCHANT_COUNTER_ONLY",
                            "商户卡待退本金由发卡商户柜台退卡结清，不在财务退款中办理");
                });
    }

    private void requireRefundEvidence(FinanceRefundRecord refund, List<String> attachments) {
        if (attachments == null || attachments.isEmpty() || refundEvidenceValidator == null) {
            throw ApiException.badRequest("REFUND_EVIDENCE_REQUIRED", "须上传真实退款执行或核实凭证");
        }
        // 经销退款已按真实项目及原收款核权；附件仍须绑定本笔退款并核实真实文件。
        // FS-021-R6：项目/平台财务（无商户编号）不按商户过滤，商户账号仍限本商户的凭证。
        String merchantScope = "dealer_order_refund".equals(refund.sourceType())
                ? null : authorizationService.currentMerchantIdQuietly().orElse(null);
        refundEvidenceValidator.requireRefundEvidence(attachments, refund.id(), merchantScope);
    }

    private FinanceRefundRecord scopedRefund(String refundId, boolean lock) {
        return scopedRefund(refundId, lock, "finance.ar_ap.view");
    }

    private FinanceRefundRecord scopedRefund(String refundId, boolean lock, String permission) {
        if (lock && dealerRefunds != null) {
            FinanceRefundRecord snapshot = repository.findRefundById(requireText(refundId, "退款编号"), false)
                    .orElseThrow(() -> ApiException.notFound("REFUND_NOT_FOUND", "退款记录不存在"));
            if ("dealer_order_refund".equals(snapshot.sourceType())) {
                var returned = dealerRefunds.findReturnRefundSource(snapshot.targetId());
                if (returned.isPresent()) {
                    var source = returned.get();
                    if (!authorizationService.canReachProject(authorizationService.currentActor(), snapshot.projectId(), "finance.ar_ap.view")) {
                        throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "无权处理其他项目的经销退款");
                    }
                    if (source.projectId() == null || !source.projectId().equals(snapshot.projectId()) || refundDealers == null) {
                        throw ApiException.conflict("DEALER_REFUND_SOURCE_MISMATCH", "退货退款与原订单项目关联不一致");
                    }
                    var order = dealerRefunds.findOrder(source.orderId())
                            .orElseThrow(() -> ApiException.conflict("DEALER_REFUND_SOURCE_MISSING", "退货退款原订单缺失"));
                    // 与退货准备退款共用客户信用、原订单、退款的锁序。
                    refundDealers.lockDealerCredit(order.dealerId()).orElseThrow();
                    dealerRefunds.lockOrder(order.id()).orElseThrow();
                }
            }
        }
        FinanceRefundRecord refund = repository.findRefundById(requireText(refundId, "退款编号"), lock)
                .orElseThrow(() -> ApiException.notFound("REFUND_NOT_FOUND", "退款记录不存在"));
        if ("dealer_order_refund".equals(refund.sourceType())) {
            if (!authorizationService.canReachProject(authorizationService.currentActor(), refund.projectId(), "finance.ar_ap.view")) {
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "无权处理其他项目的经销退款");
            }
            return refund;
        }
        // FS-021-R6：按该动作权限码的可触及项目办理；商户账号仍只限本商户。
        var actor = authorizationService.currentActor();
        var reach = authorizationService.projectReach(actor, permission);
        if (!reach.platformAdmin() && trimToNull(actor.merchantId()) != null && !actor.merchantId().equals(refund.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
        }
        if (!reach.covers(refund.projectId())) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "无权处理其他项目或归属未明的退款");
        }
        return refund;
    }

    private void requireRefundOpenForExecution(FinanceRefundRecord refund) {
        if (!Set.of("requested", "failed").contains(refund.status())) {
            throw ApiException.conflict("REFUND_NOT_RETRYABLE", "退款结果未明或已经成功，不得再次执行");
        }
    }

    private void requireRefundExecutionAuthority(FinanceRefundRecord refund, String deviceId) {
        if (!"ar_ap_payment".equals(refund.sourceType()) && trimToNull(refund.projectId()) == null) {
            throw ApiException.conflict("REFUND_PROJECT_UNRESOLVED", "待退款尚未确认唯一项目归属，不得执行或核实出款");
        }
        requireDeviceContextForOnsiteChannel(refund.refundChannel(), deviceId);
        requireManagedDeviceForFinanceAction(deviceId, "finance.refund");
        if ("dealer_order_refund".equals(refund.sourceType())) {
            requireDealerRefundSource(refund, false, false);
        } else {
            requireRefundTierAuthorized("finance", refund.refundAmountCent(), refund.projectId());
        }
    }

    private void requireDealerRefundBeforeReplay(String refundId, boolean execution, String key) {
        var snapshot = repository.findRefundById(refundId, false).orElse(null);
        if (snapshot == null || !"dealer_order_refund".equals(snapshot.sourceType())) return;
        try {
            requireDealerRefundSource(scopedRefund(refundId, false), execution, !execution);
        } catch (RuntimeException error) {
            auditEventService.recordFailure(execution ? "finance_refund.execution_failed" : "finance_refund.confirm_failed",
                    execution ? "finance.refund.create" : "finance.payment.register", "finance_refund", refundId, null, key,
                    error instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw error;
        }
    }

    private void requireDealerRefundSource(FinanceRefundRecord refund, boolean execution, boolean confirmation) {
        if (!"dealer_order_refund".equals(refund.sourceType())) return;
        String sourcePermission = dealerRefundExecutionPermission(refund.targetId(), refund.projectId(), execution);
        var source = dealerRefunds.findRefundByFinanceId(refund.id())
                .orElseThrow(() -> ApiException.conflict("DEALER_REFUND_SOURCE_MISSING", "经销退款来源关联缺失"));
        var payment = dealerRefunds.findPayment(source.paymentId()).orElseThrow();
        String paymentProject = dealerRefunds.findOrderProjectId(payment.orderId())
                .orElseThrow(() -> ApiException.conflict("DEALER_REFUND_SOURCE_MISSING", "经销原收款项目缺失"));
        String[] permissions = confirmation
                ? new String[]{sourcePermission, "finance.refund.create", "finance.payment.register"}
                : new String[]{sourcePermission, "finance.refund.create"};
        for (String permission : permissions) authorizationService.requirePermission(permission);
        if (!authorizationService.canReachProject(authorizationService.currentActor(), refund.projectId(), permissions)
                || !authorizationService.canReachProject(authorizationService.currentActor(), paymentProject, permissions)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "经销退款须同时具备业务订单和原收款项目的办理权限");
        }
        if (!refund.targetId().equals(source.id()) || !Set.of("approved", "processing", "failed", "refunded").contains(source.status())
                || source.reviewId() == null || !"verified".equals(payment.status()) || source.amountCent() != refund.refundAmountCent()
                || !java.util.Objects.equals(payment.referenceNo(), refund.originalPaymentRef())
                || !payment.method().equals(refund.refundChannel())) {
            throw ApiException.conflict("DEALER_REFUND_SOURCE_MISMATCH", "须核实经销退款原审批、原收款与原路信息");
        }
        if (execution && authorizationService.currentActor().id().equals(payment.verifiedBy())) {
            throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "退款执行人与原到账核验人必须分离");
        }
        if (confirmation) {
            var executed = latestRefundExecution(refund.id());
            if (executed != null && authorizationService.currentActor().id().equals(executed.paymentOperatorId())) {
                throw ApiException.forbidden("REFUND_SELF_CONFIRM_FORBIDDEN", "退款经办人与结果复核人必须分离");
            }
        }
    }

    private String dealerRefundExecutionPermission(String sourceId, String projectId, boolean execution) {
        if (dealerRefunds == null) {
            throw ApiException.conflict("DEALER_REFUND_SOURCE_MISSING", "经销退款来源不可核验");
        }
        var refund = dealerRefunds.findRefund(sourceId)
                .orElseThrow(() -> ApiException.conflict("DEALER_REFUND_SOURCE_MISSING", "经销退款来源缺失"));
        String businessProject = dealerRefunds.findOrderProjectId(refund.orderId())
                .orElseThrow(() -> ApiException.conflict("DEALER_REFUND_SOURCE_MISSING", "经销退款业务订单项目缺失"));
        var payment = dealerRefunds.findPayment(refund.paymentId()).orElseThrow();
        String paymentProject = dealerRefunds.findOrderProjectId(payment.orderId())
                .orElseThrow(() -> ApiException.conflict("DEALER_REFUND_SOURCE_MISSING", "经销原收款项目缺失"));
        var returned = dealerRefunds.findReturnRefundSource(sourceId);
        if (!businessProject.equals(projectId)
                || returned.isPresent() && !businessProject.equals(returned.get().projectId())) {
            throw ApiException.conflict("DEALER_REFUND_SOURCE_MISMATCH", "退款与所属业务订单项目关联不一致");
        }
        String permission = returned.isPresent() ? "dealer.return.refund.execute" : "dealer.order.refund.execute";
        authorizationService.requirePermission(permission);
        if (!authorizationService.canReachProject(authorizationService.currentActor(), businessProject, permission)
                || !authorizationService.canReachProject(authorizationService.currentActor(), paymentProject, permission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "经销退款须同时具备业务订单和原收款项目的办理权限");
        }
        if (execution && returned.isPresent() && authorizationService.currentActor().id().equals(returned.get().inspectedBy())) {
            throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "退货验收人与退款执行人必须分离");
        }
        return permission;
    }

    private boolean canDispatchRefund(FinanceRefundRecord refund) {
        return "ar_ap_payment".equals(refund.sourceType())
                && trimToNull(refund.originalPaymentRef()) != null
                && !ONSITE_REFUND_CHANNELS.contains(refund.refundChannel());
    }

    private FinanceRefundAttemptRecord latestRefundExecution(String refundId) {
        List<FinanceRefundAttemptRecord> attempts = repository.listRefundAttempts(refundId);
        for (int i = attempts.size() - 1; i >= 0; i--) {
            if (Set.of("submit", "retry", "execution").contains(attempts.get(i).operation())) return attempts.get(i);
        }
        return null;
    }

    private PaymentGatewayRequest refundGatewayRequest(FinanceRefundRecord refund, FinanceRefundAttemptRecord attempt) {
        return new PaymentGatewayRequest("finance_refund", attempt.id(), refund.refundAmountCent(),
                CURRENCY_CNY, refund.refundChannel(), refund.originalPaymentRef());
    }

    private FinanceRefundRecord dispatchPreparedRefund(FinanceRefundRecord prepared) {
        // 外层业务未提交时不调用渠道；已提交的原单可经 retry 继续同一个待派发尝试。
        if (!canDispatchRefund(prepared)
                || org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) return prepared;
        FinanceRefundAttemptRecord claimed = refundTransaction(() -> {
            FinanceRefundRecord current = scopedRefund(prepared.id(), true);
            if (!"requested".equals(current.status())) return null;
            var ready = repository.findReadyRefundAttempt(current.id());
            if (ready.isEmpty() || !repository.claimRefundAttempt(ready.get().id())) return null;
            saveRefundState(refundState(current, "processing", null, null));
            return ready.get();
        });
        if (claimed == null) return repository.findRefundById(prepared.id(), false).orElse(prepared);
        PaymentGatewayResult result;
        try {
            result = paymentGateway.refund(refundGatewayRequest(prepared, claimed));
        } catch (RuntimeException exception) {
            result = null;
        }
        PaymentGatewayResult actualResult = result;
        return refundTransaction(() -> applyRefundGatewayResult(
                scopedRefund(prepared.id(), true), claimed, actualResult, false));
    }

    private FinanceRefundRecord applyRefundGatewayResult(FinanceRefundRecord refund,
            FinanceRefundAttemptRecord attempt, PaymentGatewayResult result, boolean query) {
        if ("success".equals(refund.status())) return refund;
        FinanceRefundAttemptRecord latest = latestRefundExecution(refund.id());
        if (latest == null || !latest.id().equals(attempt.id())) return refund;
        String status = "processing";
        String provider = refund.gatewayProvider();
        String reference = refund.gatewayRef();
        String message = "渠道结果未明，保持未结，不得重复出款";
        if (result != null) {
            message = result.message();
            if (result.requiresInternalFallback()) {
                status = query ? refund.status() : "requested";
                message = "渠道未配置或未启用，退款尚未兑付";
            } else {
                boolean bound = result.confirmedPayment() != null
                        && result.confirmedPayment().equals(refundGatewayRequest(refund, attempt))
                        && trimToNull(result.gatewayProvider()) != null
                        && trimToNull(result.gatewayRef()) != null
                        && (!query || ((refund.gatewayProvider() == null || java.util.Objects.equals(refund.gatewayProvider(), result.gatewayProvider()))
                                && (refund.gatewayRef() == null || java.util.Objects.equals(refund.gatewayRef(), result.gatewayRef()))));
                if (bound && result.status() == PaymentGatewayResult.Status.SUCCEEDED) status = "success";
                if (bound && result.status() == PaymentGatewayResult.Status.FAILED) status = "failed";
                if (bound) {
                    provider = result.gatewayProvider();
                    reference = result.gatewayRef();
                }
                if (!bound && result.status() != PaymentGatewayResult.Status.PENDING) {
                    message = "渠道返回与本次退款未完成一致性核实，保持未结";
                }
            }
        }
        repository.appendRefundAttempt(new FinanceRefundAttemptRecord(
                "refund_attempt_" + UUID.randomUUID().toString().replace("-", ""), refund.id(), "query",
                status, provider, reference, message, refund.originalPaymentRef(), attempt.paymentOperatorId(),
                null, List.of(), Instant.now()));
        return saveRefundState(refundState(refund, status, provider, reference));
    }

    private FinanceRefundRecord saveRefundState(FinanceRefundRecord value) {
        FinanceRefundRecord saved = repository.saveRefund(value);
        if ("dealer_order_refund".equals(saved.sourceType())) {
            if (refundEvents == null) throw ApiException.conflict("DEALER_REFUND_SYNC_UNAVAILABLE", "经销退款结果同步不可用");
            refundEvents.publishEvent(new FinanceRefundStateChanged(saved));
        }
        return saved;
    }

    private FinanceRefundRecord refundState(FinanceRefundRecord refund, String status, String provider, String reference) {
        Instant now = Instant.now();
        return new FinanceRefundRecord(refund.id(), refund.targetId(), refund.refundAmountCent(),
                refund.reason(), refund.refundChannel(), refund.refundDirection(), status, refund.createdAt(),
                refund.sourceType(), refund.merchantId(), refund.projectId(), refund.requestedBy(),
                refund.originalPaymentRef(), provider, reference, now, "success".equals(status) ? now : null);
    }

    // ── P1#13/#14 内部实现 ──────────────────────────────────────────────────────

    /**
     * FS-017（BSM L1072「由原门店承担的作废酒品价值按内部结算价计入该酒吧的经营损失」「补酒成本计入该酒吧的经营损失」
     * 「接收后因该娱乐平台公司保管造成的损失由该公司承担」）：补存/赔付经营损失记内部核算台账（范式同 postInventoryVariance damage 分支）。
     * counterparty=承担方（原门店商户 id 或娱乐平台公司主体 id）；merchantId=原门店承担时挂原门店账本，平台承担时不挂商户。
     */
    @Transactional
    public ArApItemRecord postSavedBottleRemedyLoss(String remedyId, String bearerId, String merchantId, long amountCent, String idempotencyKey) {
        return persistDerivedArApItem("internal_accounting", requireText(bearerId, "bearer_id"), "saved_bottle_remedy",
                requireText(remedyId, "remedy_id"), amountCent, requireText(idempotencyKey, "idempotency_key"), merchantId);
    }

    /**
     * HP-F03（BSM L1408「残值或损失由财务记录并关联原设备」）：楼宇设备报废残值＝对回收方的应收，来源关联原报废单、归原设备项目；
     * 收款走既有登记收付款（registerArApPayment→settleArApItem），款项独立跟进到实际收清。
     */
    public ArApItemRecord postFacilityEquipmentScrapResidual(String scrapId, String counterpartyId, String projectId,
            long amountCent, String idempotencyKey) {
        return persistDerivedArApItem("receivable", requireText(counterpartyId, "residual_counterparty_id"),
                "facility_equipment_scrap", requireText(scrapId, "scrap_id"), amountCent,
                requireText(idempotencyKey, "idempotency_key"), null, null, LocalDate.now(), projectId);
    }

    /** HP-F03：楼宇设备报废损失＝承担方的内部核算（登记即确认），来源关联原报废单、归原设备项目。 */
    public ArApItemRecord postFacilityEquipmentScrapLoss(String scrapId, String bearerId, String projectId,
            long amountCent, String idempotencyKey) {
        return persistDerivedArApItem("internal_accounting", requireText(bearerId, "bearer_id"),
                "facility_equipment_scrap_loss", requireText(scrapId, "scrap_id"), amountCent,
                requireText(idempotencyKey, "idempotency_key"), null, null, LocalDate.now(), projectId);
    }

    /** FS-017（BSM L1072「该娱乐平台公司可经审批先垫付并向原门店追收」）：对原门店挂追收应收，经统一回写点 settleArApItem 收回。 */
    @Transactional
    public ArApItemRecord postSavedBottleRemedyRecovery(String remedyId, String originalMerchantId, long amountCent, String idempotencyKey) {
        String merchantId = requireText(originalMerchantId, "original_merchant_id");
        return persistDerivedArApItem("receivable", merchantId, "saved_bottle_remedy_recovery",
                requireText(remedyId, "remedy_id"), amountCent, requireText(idempotencyKey, "idempotency_key"), merchantId);
    }

    private ArApItemRecord postInventoryVarianceInternal(InventoryVariancePostRequest request) {
        InventoryVariancePostRequest actual = requireRequest(request);
        String varianceId = requireText(actual.inventoryVarianceId(), "inventory_variance_id");
        String counterpartyId = requireText(actual.counterpartyId(), "counterparty_id");

        InventoryVarianceRecord variance = repository.findInventoryVarianceById(varianceId)
                .orElseThrow(() -> ApiException.notFound("INVENTORY_VARIANCE_NOT_FOUND", "库存差异不存在或已被删除"));
        checkMerchantOwnership(variance.merchantId());

        // 幂等：已入账直接返回原台账项，不重复生成。
        if (variance.postedArApItemId() != null) {
            return repository.findArApItemById(variance.postedArApItemId())
                    .orElseThrow(() -> ApiException.conflict("INVENTORY_VARIANCE_ALREADY_POSTED",
                            "库存差异已过账但缺少台账条目，请联系管理员"));
        }
        // 只有复核通过的差异才能入账。
        if (!"approved".equals(variance.reviewStatus())) {
            throw ApiException.badRequest("INVENTORY_VARIANCE_NOT_APPROVED",
                    "库存差异须审核通过后才能过账，当前状态：" + variance.reviewStatus());
        }
        if (!INVENTORY_VARIANCE_REASONS.contains(variance.reason())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "库存差异原因不正确");
        }
        // 科目方向（用户裁定 C3 对齐 §6D）：
        //   supplier_fault → 向供应商挂应付索赔 payable（真实对外债务）；
        //   overage（盘盈，冲成本）→ payable（账面成本回冲）；
        //   damage/shortage（报损/盘亏）→ internal_accounting 内部核算损耗成本（非对外债权债务，不并入 net）。
        String direction;
        if ("supplier_fault".equals(variance.reason()) || "overage".equals(variance.reason())) {
            direction = "payable";
        } else {
            direction = "internal_accounting";
        }
        long amountCent = Math.abs(variance.varianceAmountCent());
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "库存差异金额为 0，无需过账");
        }

        ArApItemRecord saved = persistDerivedArApItem(
                direction,
                counterpartyId,
                "inventory_difference",
                variance.id(),
                amountCent,
                actual.idempotencyKey(),
                variance.merchantId());
        // P1 修复：条件回填（仅当 posted_ar_ap_item_id IS NULL 才 UPDATE）。并发双开时后到者命中 0 行返回 false，
        // 说明该 variance 已被他人入账，抛冲突，避免重复挂应付台账（前面 persistDerivedArApItem 凭 idempotencyKey 幂等不重复落项）。
        boolean posted = repository.markInventoryVariancePostedIfUnposted(variance.id(), saved.id());
        if (!posted) {
            throw ApiException.conflict("INVENTORY_VARIANCE_ALREADY_POSTED",
                    "库存差异已被并发请求过账，请勿重复操作");
        }
        return saved;
    }

    private TurnoverRentRuleRecord createTurnoverRentRuleInternal(TurnoverRentRuleRequest request) {
        TurnoverRentRuleRequest actual = requireRequest(request);
        String merchantId = requireText(actual.merchantId(), "merchant_id");
        checkMerchantOwnership(merchantId);
        String ruleName = requireText(actual.ruleName(), "rule_name");
        LocalDate effectiveFrom = requireDate(actual.effectiveFrom(), "effective_from");
        LocalDate effectiveTo = actual.effectiveTo();
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "生效结束日期不能早于生效开始日期");
        }
        List<TurnoverRentRuleRequest.TierInput> tierInputs = actual.tiers();
        if (tierInputs == null || tierInputs.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "至少需要一个阶梯");
        }
        long prevUpper = 0L;
        for (int i = 0; i < tierInputs.size(); i++) {
            TurnoverRentRuleRequest.TierInput t = tierInputs.get(i);
            boolean last = i == tierInputs.size() - 1;
            Integer rate = t.rateBps();
            if (rate == null || rate < 0 || rate > 10000) {
                throw ApiException.badRequest("VALIDATION_FAILED", "阶梯费率（rate_bps）必须在 0 到 10000 之间");
            }
            if (last) {
                if (t.upperBoundCent() != null) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "最高阶梯不能设置上限");
                }
            } else {
                Long upper = t.upperBoundCent();
                if (upper == null || upper <= prevUpper) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "阶梯上限金额必须为正且逐级递增");
                }
                prevUpper = upper;
            }
        }
        Instant now = Instant.now();
        String ruleId = "trr_" + slug(merchantId + "_" + actual.idempotencyKey());
        // P0-E：一律落草案 + 记起草人（差人复核转正的守卫依据），草案对结算零影响。
        TurnoverRentRuleRecord rule = repository.saveTurnoverRentRuleDraft(new TurnoverRentRuleRecord(
                ruleId,
                actual.contractId(),
                merchantId,
                ruleName,
                0,
                actual.minAmountCent(),
                null,
                effectiveFrom,
                effectiveTo,
                "active",
                now), authorizationService.currentActor().id());
        int seq = 1;
        for (TurnoverRentRuleRequest.TierInput t : tierInputs) {
            repository.saveTurnoverRentRuleTier(new TurnoverRentRuleTierRecord(
                    ruleId + "_t" + seq, ruleId, seq, t.upperBoundCent(), t.rateBps()));
            seq++;
        }
        return rule;
    }

    /** 月账单采用该期间唯一、完整覆盖的已复核版本，并把版本交给账单固化。 */
    public PeriodTurnoverRent computePeriodTurnoverRentSnapshot(String merchantId, String contractId, String period) {
        requireText(contractId, "contract_id");
        requireText(merchantId, "merchant_id");
        java.time.YearMonth month;
        try {
            month = java.time.YearMonth.parse(requireText(period, "period"));
        } catch (java.time.DateTimeException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账期格式不正确，应为 YYYY-MM");
        }
        TurnoverRentRuleRecord rule = requirePeriodTurnoverRentRule(
                contractId, merchantId, month.atDay(1), month.atEndOfMonth());
        return new PeriodTurnoverRent(rule.id(),
                calculateTurnoverRent(rule, month.atDay(1), month.atEndOfMonth()));
    }

    public long computePeriodTurnoverRent(String merchantId, String contractId, String period) {
        return computePeriodTurnoverRentSnapshot(merchantId, contractId, period).amountCent();
    }

    public record PeriodTurnoverRent(String ruleId, long amountCent) {
    }

    private TurnoverRentRuleRecord requirePeriodTurnoverRentRule(
            String contractId, String merchantId, LocalDate periodStart, LocalDate periodEnd) {
        List<TurnoverRentRuleRecord> rules = repository.findTurnoverRentRulesForPeriod(
                contractId, merchantId, periodStart, periodEnd);
        if (rules.size() != 1) {
            throw ApiException.conflict("TURNOVER_RENT_RULE_PERIOD_UNRESOLVED",
                    "账期须由唯一已复核且未人工下线的抽成规则完整覆盖；请核实规则生效期间后再出账");
        }
        return rules.getFirst();
    }

    private long calculateTurnoverRent(TurnoverRentRuleRecord rule, LocalDate periodStart, LocalDate periodEnd) {
        Instant from = businessDayResolver.startInstant(periodStart);
        Instant to = businessDayResolver.startInstant(periodEnd.plusDays(1));
        long turnover = repository.aggregateMerchantReflowTurnover(rule.merchantId(), from, to);
        List<TurnoverRentRuleTierRecord> tiers = repository.listTurnoverRentRuleTiers(rule.id());
        return tiers.isEmpty()
                ? clampRent(turnover, rule.rateBps(), rule.minAmountCent(), rule.maxAmountCent())
                : TurnoverRentMath.tieredRent(turnover, tiers, rule.minAmountCent());
    }

    /**
     * V299 持股分红发放端口：审批通过的分红计算单按主理人净发放额（代扣后）挂应付
     * (payable, counterparty=主理人账户, source_type='dividend_payout', source_id=计算单 id——
     * (direction,source_type,source_id) 部分唯一索引防一单重挂；幂等键取计算单 id 使派生 id 确定)。
     */
    public String createDividendPayoutPayable(
            String principalUserId, String merchantId, long netAmountCent, String statementId) {
        ArApItemRecord item = persistDerivedArApItem(
                "payable", principalUserId, "dividend_payout", statementId, netAmountCent, statementId, merchantId);
        return item.id();
    }

    /**
     * V303 发放保护线留存 → 欠主理人的待释放负债（非平台白拿）：挂应付
     * (payable, counterparty=主理人, source_type='dividend_reserve_withheld', source_id=计算单 id 一单一挂)。
     * 状态沿用 pending_payment（source_type 即待释放标识）；释放规则/端点/UI 为后续独立构建 R。
     */
    public String createDividendReserveWithholdingPayable(
            String principalUserId, String merchantId, long reserveCent, String statementId) {
        ArApItemRecord item = persistDerivedArApItem(
                "payable", principalUserId, "dividend_reserve_withheld", statementId, reserveCent, statementId,
                merchantId);
        return item.id();
    }

    /**
     * V304 发放保护线留存·释放：把该计算单的待释放留存应付 re-tag 为已释放（source_type flip，金额不变）。
     * FOR UPDATE 行锁串行化并发释放 + 前态守卫（仅 dividend_reserve_withheld 可翻）幂等：
     * 已释放（他线程先翻 / 无待释放应付）返回 empty，由分红侧抛 DIVIDEND_RESERVE_ALREADY_RELEASED。
     * released 应付走既有付款流程真正发放给主理人（不再被待释放支付守卫拦截）。
     */
    public java.util.Optional<DividendReserveReleaseRef> releaseDividendReserveWithholding(String statementId) {
        java.util.Optional<ArApItemRecord> withheld = repository.findReserveWithheldByStatementForUpdate(statementId);
        if (withheld.isEmpty()) {
            return java.util.Optional.empty();
        }
        ArApItemRecord item = withheld.get();
        int flipped = repository.retagArApItemSourceType(
                item.id(), "dividend_reserve_withheld", "dividend_reserve_released", Instant.now());
        if (flipped == 0) {
            return java.util.Optional.empty();   // 并发下已被他线程释放
        }
        return java.util.Optional.of(new DividendReserveReleaseRef(
                item.id(), item.amountCent(), item.counterpartyId(), item.merchantId()));
    }

    /**
     * V303 欠款抵扣：该商户未清应收快照（direction=receivable, unpaid>0），按 created_at 升序=先欠先抵。
     * 供分红计算单冻结欠款抵扣额与逐项留痕（只读，不改台账）。
     */
    public List<DividendArrearsItem> listOutstandingReceivables(String merchantId) {
        List<ArApItemRecord> items = repository.listArApItemsByMerchant(merchantId);
        // 已被冲正/重述的原行（第四轮裁决第 2 条作废的预排账单、V344 补录红冲原行）不是有效欠款：派生行沿用原行商户，同一列表内即可排除。
        Set<String> corrected = items.stream().map(ArApItemRecord::originItemId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        return items.stream()
                .filter(i -> "receivable".equals(i.direction()) && i.unpaidAmountCent() > 0 && !corrected.contains(i.id()))
                .sorted(java.util.Comparator.comparing(ArApItemRecord::createdAt)
                        .thenComparing(ArApItemRecord::id))
                .map(i -> new DividendArrearsItem(i.id(), i.itemNo(), i.unpaidAmountCent()))
                .toList();
    }

    /**
     * V303 欠款抵扣：审批时按冻结抵扣额对该商户未清应收逐项结算（先欠先抵，dividend_offset 收款）。
     * 逐项复用 settleArApItem（FOR UPDATE 行锁 + 跨商户/项目校验 + 锁定拦截 + 登记收款 + 回写 paid/unpaid/status）。
     * 返回实际结算总额；调用方（DividendService.approve）在同一事务内断言 == 冻结抵扣额，不等则回滚并要求作废重算。
     */
    public long applyDividendOffsetToReceivables(String merchantId, long offsetCent, String statementId) {
        if (offsetCent <= 0) {
            return 0L;
        }
        List<DividendArrearsItem> items = listOutstandingReceivables(merchantId);
        long available = items.stream().mapToLong(DividendArrearsItem::unpaidCent).sum();
        // 原子性 by construction：当前未清应收不足冻结抵扣额（抵扣期间被并发清偿）时一分不结算，直接返回实况，
        // 由调用方抛 DIVIDEND_ARREARS_CHANGED 干净回退——不依赖事务回滚就不会留半结（executeWrite 非独立事务）。
        if (available < offsetCent) {
            return available;
        }
        Instant now = Instant.now();
        long remaining = offsetCent;
        long settled = 0L;
        for (DividendArrearsItem item : items) {
            if (remaining <= 0) {
                break;
            }
            long apply = Math.min(remaining, item.unpaidCent());
            // idempotencyKey 纳入 statementId + 台账项 id：审批一次一结算，重放不重扣（settleArApItem 主键含两者）。
            // ponytail: 先欠先抵到 offsetCent 封顶；预检已确保总额充足，逐笔 FOR UPDATE 仍防单笔并发（罕见单笔被清则抛超额、随本单事务回滚）。
            settleArApItem(item.arApItemId(), apply, "dividend_offset", now, statementId, statementId, "finance.dividend.manage");
            remaining -= apply;
            settled += apply;
        }
        return settled;
    }

    // ponytail: 提成租金/第三方券结算/分红发放三个单实现端口接口已拆并入本类(one impl),嵌套 record 随迁。
    /** 释放引用:被 re-tag 的应付项 id / 留存额(分)/ 主理人账户 / 归属商户。 */
    public record DividendReserveReleaseRef(String arApItemId, long reserveCent, String principalUserId, String merchantId) {
    }

    /** 未清应收明细(供逐项留痕):台账项 id / 单号 / 未清额(分)。 */
    public record DividendArrearsItem(String arApItemId, String itemNo, long unpaidCent) {
    }

    /** 批3:内部价采购券核销回流 -> 按内部结算价给第三方挂应付(payable, counterparty=partnerId)。 */
    public String createPartnerVoucherPayable(String partnerId, long internalPriceCent, String idempotencyKey) {
        ArApItemRecord item = persistDerivedArApItem(
                "payable", partnerId, "partner_voucher", idempotencyKey, internalPriceCent, idempotencyKey, null);
        return item.id();
    }

    /**
     * 退租应退押金落台账（#3② 多合同超退根治）：FOR UPDATE 锁定权威余额（并发退租串行化）→ 按锁定余额封顶（race-safe）→
     * 挂 ar_ap 应付（source_type='deposit_refund'）+ 写 deposit_ledger refund 行扣减余额，使同一商户后续合同读到已减余额、不叠加超退。
     * 幂等键=结算单 id；返回实退值（≤ 请求净额）。
     */
    @Override
    public long createDepositRefundPayable(String merchantId, long netRefundCent, String settlementId, LocalDate dueDate) {
        long lockedBalance = repository.currentDepositBalanceForUpdate(merchantId);
        long effectiveRefund = Math.min(Math.max(0L, netRefundCent), Math.max(0L, lockedBalance));
        if (effectiveRefund <= 0) {
            return 0L;   // 余额已被其它合同退租消费殆尽：不挂应付、不写台账
        }
        ArApItemRecord item = persistDerivedArApItem(
                "payable", merchantId, "deposit_refund", settlementId, effectiveRefund, settlementId, merchantId, null, dueDate);
        repository.saveDepositLedger(new DepositLedgerRecord(
                "deposit_refund_" + slug(settlementId), merchantId, "refund", effectiveRefund,
                lockedBalance - effectiveRefund, item.id(), "退租应退押金结算", Instant.now()));
        return effectiveRefund;
    }

    /** 提前退租违约金挂应收（source_type='termination_penalty'）。幂等键=结算单 id。 */
    @Override
    public String createTerminationPenaltyReceivable(String merchantId, long penaltyCent, String settlementId) {
        ArApItemRecord item = persistDerivedArApItem(
                "receivable", merchantId, "termination_penalty", settlementId, penaltyCent, settlementId, merchantId);
        return item.id();
    }

    /**
     * HP-C03-2：变更押金补退差额登记（差额&gt;0 应收补缴、&lt;0 应付退还，取绝对值；到期日＝办理期限；幂等键＝变更 id）。
     * 只挂台账、不写 deposit_ledger——已收押金余额等实际收退（settleArApItem）才更新。
     */
    @Override
    public ArApItemRecord registerDepositAdjustment(String merchantId, String childContractId, String changeId,
            long deltaCent, LocalDate deadline) {
        return persistDerivedArApItem(deltaCent > 0 ? "receivable" : "payable", merchantId, "deposit_adjustment",
                childContractId, Math.abs(deltaCent), changeId, merchantId, null, deadline);
    }

    /**
     * 第四轮裁决第 3 条退租结算押金抵扣销账（见端口说明）：锁押金余额（与退还/补退串行）→ 逐笔锁台账项按未收额经 settleArApItem 以 deposit_offset 销账 →
     * 一条 deduct 行（id 由结算单 id 派生）。系统按结算清单自动落账（财务结算 PRD L957「不另设独立抵扣审批或手动确认入口」），不按当前操作人权限范围复核；
     * 销账同样回调合同域（此刻结算单仍在「待结算」，状态由终审落账按押金够不够分流，回调不推进）。
     */
    @Override
    public List<DepositOffset> offsetReceivablesWithDeposit(String merchantId, String settlementId, List<String> arApItemIds,
            long offsetCent) {
        long balance = repository.currentDepositBalanceForUpdate(merchantId);
        long remaining = Math.min(offsetCent, balance);
        Instant now = Instant.now();
        List<DepositOffset> offsets = new java.util.ArrayList<>();
        for (String id : arApItemIds) {
            if (remaining <= 0) {
                break;
            }
            long apply = Math.min(remaining, repository.findArApItemByIdForUpdate(id).orElseThrow().unpaidAmountCent());
            if (apply > 0) {
                settleArApItem(id, apply, DEPOSIT_OFFSET_METHOD, now, settlementId, settlementId, null);
                offsets.add(new DepositOffset(id, apply));
                remaining -= apply;
            }
        }
        long total = offsets.stream().mapToLong(DepositOffset::amountCent).sum();
        if (total > 0) {
            repository.saveDepositLedger(new DepositLedgerRecord("deposit_offset_" + slug(settlementId), merchantId, "deduct", total,
                    balance - total, null, "退租结算押金抵扣（结算单 " + settlementId + "）", now));
        }
        return offsets;
    }

    /** 应退押金封顶=min(合同固化额, 该商户押金台账实收余额)。只读余额（无锁），无台账=0。 */
    @Override
    public long effectiveRefundableDeposit(String merchantId, long contractDepositCent) {
        long balance = repository.currentDepositBalance(merchantId);
        return Math.min(contractDepositCent, balance);
    }

    private ArApItemRecord billTurnoverRentInternal(TurnoverRentBillRequest request) {
        TurnoverRentBillRequest actual = requireRequest(request);
        String ruleId = requireText(actual.ruleId(), "rule_id");
        LocalDate periodStart = requireDate(actual.periodStart(), "period_start");
        LocalDate periodEnd = requireDate(actual.periodEnd(), "period_end");
        LocalDate dueDate = requireDate(actual.dueDate(), "due_date");
        if (periodEnd.isBefore(periodStart)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "期间结束日期不能早于开始日期");
        }

        TurnoverRentRuleRecord rule = repository.findTurnoverRentRuleById(ruleId)
                .orElseThrow(() -> ApiException.notFound("TURNOVER_RENT_RULE_NOT_FOUND", "营业额抽成规则不存在或已被删除"));
        checkMerchantOwnership(rule.merchantId());
        if (repository.isTurnoverRentRuleDraft(ruleId)) {
            throw ApiException.badRequest("TURNOVER_RENT_RULE_NOT_ACTIVATED",
                    "抽成规则仍是待复核草案，须经结算配置中心复核转正后才能计费");
        }
        TurnoverRentRuleRecord periodRule = requirePeriodTurnoverRentRule(
                rule.contractId(), rule.merchantId(), periodStart, periodEnd);
        if (!ruleId.equals(periodRule.id())) {
            throw ApiException.conflict("TURNOVER_RENT_RULE_PERIOD_MISMATCH",
                    "指定的抽成规则不是该账期应采用的版本");
        }
        long rent = calculateTurnoverRent(periodRule, periodStart, periodEnd);
        if (rent <= 0) {
            throw ApiException.badRequest("TURNOVER_RENT_ZERO", "计算出的营业额抽成为 0，无需生成账单");
        }
        // 营业额抽成应收：counterparty=商户。V299 起落 accounting_period=期间起始月——
        // 租金自动挂接（费用台账→分红基数）按 accounting_period 归期；历史行无期间标记不挂接（如实边界）。
        return persistDerivedArApItem(
                "receivable",
                rule.merchantId(),
                "turnover_rent",
                rule.id(),
                rent,
                actual.idempotencyKey(),
                rule.merchantId(),
                java.time.YearMonth.from(periodStart).toString());
    }

    /**
     * rent = clamp(round_half_up(turnover × rateBps / 10000), min, max)。min/max 为 null 时不夹。
     *
     * <p>R-7(D15.1「提成」明列):旧 {@code Math.floorDiv(turnover*rateBps, 10000)} 是向下取整,
     * 营业额抽成租金恒少收且余数无留痕;改公共入口 HALF_UP。
     */
    private long clampRent(long turnover, int rateBps, Long minAmountCent, Long maxAmountCent) {
        long rent = MoneyRounding.bpsOf(turnover, rateBps);
        if (minAmountCent != null && rent < minAmountCent) {
            rent = minAmountCent;
        }
        if (maxAmountCent != null && rent > maxAmountCent) {
            rent = maxAmountCent;
        }
        return rent;
    }

    private DepositLedgerRecord recordDepositActionInternal(DepositActionRequest request) {
        DepositActionRequest actual = requireRequest(request);
        String merchantId = requireText(actual.merchantId(), "merchant_id");
        checkMerchantOwnership(merchantId);
        String action = requireEnum(actual.action(), DEPOSIT_ACTIONS, "action");
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");

        // P1 修复：余额读取改对该商户 deposit_ledger 最新行加 FOR UPDATE 行锁，使并发 collect/deduct 串行化，
        // 在锁内读取权威余额并完成超额校验，避免读旧余额各自 append 导致 balance_after 互覆盖丢更新、超额校验被绕过。
        long balance = repository.currentDepositBalanceForUpdate(merchantId);
        long newBalance;
        if ("collect".equals(action)) {
            newBalance = balance + amountCent;
        } else {
            // refund / deduct / forfeit 均减少余额，不得超额（不可为负）。
            if (amountCent > balance) {
                throw ApiException.badRequest("DEPOSIT_INSUFFICIENT",
                        "押金操作 " + action + " 金额 " + amountCent + " 超过余额 " + balance);
            }
            newBalance = balance - amountCent;
        }

        // deduct/forfeit 联动生成应收（向商户索赔/没收入账）；collect/refund 不生成台账项。
        String arApItemId = null;
        if ("deduct".equals(action) || "forfeit".equals(action)) {
            ArApItemRecord item = persistDerivedArApItem(
                    "receivable",
                    merchantId,
                    "deposit",
                    "deposit_" + action + "_" + slug(actual.idempotencyKey()),
                    amountCent,
                    actual.idempotencyKey(),
                    merchantId);
            arApItemId = item.id();
        }

        Instant now = Instant.now();
        return repository.saveDepositLedger(new DepositLedgerRecord(
                "deposit_" + slug(merchantId + "_" + actual.idempotencyKey()),
                merchantId,
                action,
                amountCent,
                newBalance,
                arApItemId,
                trimToNull(actual.reason()),
                now));
    }

    /**
     * 由派生计费/差异流程直接生成一条 ar_ap_items（不经请求体 createItem，但同样遵守锁定经营日硬拦截）。
     * dueDate 取当日（经营日落账），sourceType ∈ SOURCE_TYPES。
     */
    // ponytail: 派生 AR/AP item id="arap_"+slug;物业费出账的 idempotencyKey 含 projectId|period|merchantId 长键→
    // slug 过长会溢出 ar_ap_items.id varchar(64)。仅对超长 slug 哈希封顶(短 slug 不变→其它调用方 id 格式不动)。
    private static String boundedArApSlug(String rawSlug) {
        if (rawSlug.length() <= 48) {
            return rawSlug;
        }
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(rawSlug.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0, 40);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("ar/ap slug hashing unavailable", e);
        }
    }

    private ArApItemRecord persistDerivedArApItem(
            String direction,
            String counterpartyId,
            String sourceType,
            String sourceId,
            long amountCent,
            String idempotencyKey,
            String merchantId) {
        return persistDerivedArApItem(direction, counterpartyId, sourceType, sourceId, amountCent,
                idempotencyKey, merchantId, null);
    }

    private ArApItemRecord persistDerivedArApItem(
            String direction,
            String counterpartyId,
            String sourceType,
            String sourceId,
            long amountCent,
            String idempotencyKey,
            String merchantId,
            String accountingPeriod) {
        return persistDerivedArApItem(direction, counterpartyId, sourceType, sourceId, amountCent, idempotencyKey, merchantId,
                accountingPeriod, LocalDate.now());
    }

    /** dueDate：台账到期日（默认当日经营日落账；HP-C03 押金补退差额＝办理期限）。锁定判定按落账当日。 */
    private ArApItemRecord persistDerivedArApItem(
            String direction,
            String counterpartyId,
            String sourceType,
            String sourceId,
            long amountCent,
            String idempotencyKey,
            String merchantId,
            String accountingPeriod,
            LocalDate dueDate) {
        return persistDerivedArApItem(direction, counterpartyId, sourceType, sourceId, amountCent, idempotencyKey,
                merchantId, accountingPeriod, dueDate, null);
    }

    /** projectId：派生台账的项目归属（HP-F03 报废残值/损失归原设备项目，供项目财务收款与隔离）；其余派生方传 null。 */
    private ArApItemRecord persistDerivedArApItem(
            String direction,
            String counterpartyId,
            String sourceType,
            String sourceId,
            long amountCent,
            String idempotencyKey,
            String merchantId,
            String accountingPeriod,
            LocalDate dueDate,
            String projectId) {
        LocalDate today = LocalDate.now();
        // F-OPS-09：派生台账按本条目归属 merchantId 判锁（与下方 ArApItemRecord.merchantId 同口径）；
        // merchantId 为空（平台口径派生）则受任一封账约束。
        if (repository.lockedDailyCloseExistsForDate(today, merchantId)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "营业日 " + today + " 已锁定，请走补录调整流程");
        }
        Instant now = Instant.now();
        String slug = boundedArApSlug(slug(sourceType + "_" + idempotencyKey));
        String id = "arap_" + slug;
        // internal_accounting（内部核算损耗/归集）非对外债权债务：登记即已确认，paid=amount、unpaid=0、settled；
        // receivable/payable 维持待收/待付（unpaid=amount）。
        boolean internalAccounting = "internal_accounting".equals(direction);
        long paidAmountCent = internalAccounting ? amountCent : 0L;
        long unpaidAmountCent = internalAccounting ? 0L : amountCent;
        String status = internalAccounting ? "settled"
                : ("receivable".equals(direction) ? "pending_collection" : "pending_payment");
        ArApItemRecord item = new ArApItemRecord(
                id,
                "ARAP" + now.toEpochMilli() + "-" + slug.substring(0, Math.min(12, slug.length())),
                direction,
                counterpartyId,
                sourceType,
                sourceId,
                amountCent,
                paidAmountCent,
                unpaidAmountCent,
                CURRENCY_CNY,
                dueDate,
                accountingPeriod,
                status,
                now,
                now,
                merchantId,
                projectId);
        ArApItemRecord saved = repository.saveArApItem(item);
        repository.saveArApItemSource(new ArApItemSourceRecord(
                "arap_src_" + slug,
                saved.id(),
                sourceType,
                sourceId,
                amountCent,
                now));
        return saved;
    }

    private PropertyFeeRateRecord createPropertyFeeRateInternal(PropertyFeeRateRequest request) {
        PropertyFeeRateRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        Long rateBoxed = actual.ratePerSquareMeterCent();
        if (rateBoxed == null || rateBoxed < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "每平米费率必须大于等于 0");
        }
        String effectivePeriod = trimToNull(actual.effectivePeriod());   // null=该项目默认费率
        String status = requireEnum(trimToDefault(actual.status(), "active"),
                Set.of("active", "inactive"), "status");
        Instant now = Instant.now();
        String id = "pfr_" + stableToken("property_fee_rate",
                projectId + "|" + (effectivePeriod == null ? "__default__" : effectivePeriod));
        return repository.savePropertyFeeRate(new PropertyFeeRateRecord(
                id, projectId, effectivePeriod, rateBoxed, status, now, now));
    }

    private List<PropertyFeeBillRecord> billPropertyFeeInternal(PropertyFeeBillRequest request) {
        PropertyFeeBillRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        String period = requireText(actual.period(), "period");
        // 期格式强校验（yyyy-MM）：错格式期会生成商户费用台账/AR 都对不上的孤儿账期，入口即拒。
        try {
            java.time.YearMonth.parse(period);
        } catch (java.time.format.DateTimeParseException e) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账期格式不正确，应为 yyyy-MM（如 2026-07）");
        }
        List<String> merchantIds = actual.merchantIds();
        if (merchantIds == null || merchantIds.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "merchant_ids is required");
        }
        // 费率按 项目+期 解析一次（精确期优先、回退默认）；无生效费率直接拒绝整批。
        PropertyFeeRateRecord rate = repository.findEffectivePropertyFeeRate(projectId, period)
                .orElseThrow(() -> ApiException.badRequest("PROPERTY_FEE_RATE_NOT_FOUND",
                        "项目 " + projectId + " 在账期 " + period + " 未配置生效的物业费率"));
        long rateCent = rate.ratePerSquareMeterCent();
        Instant now = Instant.now();
        List<PropertyFeeBillRecord> result = new java.util.ArrayList<>();
        for (String rawMerchantId : merchantIds) {
            String merchantId = requireText(rawMerchantId, "merchant_id");
            checkMerchantOwnership(merchantId);
            // 不退不补 + 幂等：该 (项目,期,商户) 已计过费则跳过（含跨请求重复计费，不二次挂账）。
            if (repository.propertyFeeBillExists(projectId, period, merchantId)) {
                continue;
            }
            java.math.BigDecimal area = repository.findMerchantAreaSquareMeter(merchantId);
            if (area == null || area.signum() <= 0) {
                // 面积缺失/非正，无法按面积计费——跳过（运营补面积后重跑，已计费的不重复）。
                continue;
            }
            long amountCent = java.math.BigDecimal.valueOf(rateCent)
                    .multiply(area)
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .longValueExact();
            if (amountCent <= 0) {
                continue;   // rate=0 等导致 0 额，不挂 0 元应收。
            }
            // ar_ap 幂等键按 (项目,期,商户) 稳定派生（与 bills 行同源），跨请求重复计费 upsert 同一台账行、不重复挂账。
            String naturalKey = projectId + "|" + period + "|" + merchantId;
            ArApItemRecord item = persistDerivedArApItem(
                    "receivable",
                    merchantId,
                    "property_fee",
                    "property_fee_" + naturalKey,
                    amountCent,
                    stableToken("property_fee", naturalKey),
                    merchantId);
            PropertyFeeBillRecord bill = repository.savePropertyFeeBill(new PropertyFeeBillRecord(
                    "pfb_" + stableToken("property_fee_bill", naturalKey),
                    projectId,
                    period,
                    merchantId,
                    area,
                    rateCent,
                    amountCent,
                    item.id(),
                    now));
            result.add(bill);
        }
        return result;
    }

    /** 由稳定自然键派生确定性、抗截断的幂等令牌（名字 UUID v3，跨请求一致；用于物业费 ar_ap/留痕行 id）。 */
    private String stableToken(String namespace, String naturalKey) {
        return java.util.UUID.nameUUIDFromBytes(
                (namespace + ":" + naturalKey).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString().replace("-", "");
    }

    private void requireArApBeforeReplay(String id, String permission, String failureEvent, String key) {
        try {
            authorizationService.requirePermission(permission);
            var item=repository.findArApItemById(id).orElseThrow(()->ApiException.notFound("AR_AP_ITEM_NOT_FOUND","应收应付款项不存在或已被删除"));
            requireArApScope(item, permission);
            requireCurrentPurchasePayable(item);
        } catch (RuntimeException error) {
            auditEventService.recordFailure(failureEvent,permission,"ar_ap_item",id,null,key,
                    error instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw error;
        }
    }

    private void rejectPayrollMutation(ArApItemRecord item) {
        if ("staff_payroll".equals(item.sourceType())) throw ApiException.conflict("PAYROLL_DEDICATED_WORKFLOW_REQUIRED",
                "工资须由原工资单形成应付并在工资银行付款入口核实，不能用普通收付、退款或调账改写");
    }

    private void requireCurrentPurchasePayable(ArApItemRecord item) {
        if (Set.of("purchase_inbound", "purchase_consignment", "purchase_prepayment").contains(item.sourceType())
                && (item.amountCent() <= 0 || "reversed".equals(item.status()) || repository.isPurchasePayableReplaced(item.id())
                    || repository.findCurrentPurchasePayable(item.sourceType(), item.sourceId())
                        .filter(current -> current.id().equals(item.id())).isEmpty())) {
            throw ApiException.conflict("PURCHASE_PAYABLE_REPLACED", "采购应付已被更正或属于红冲记录，请使用当前有效应付");
        }
    }

    /** FS-021-R5 内部应付执行状态同用此范围闸（同包）。 */
    void requireArApScope(ArApItemRecord item, String permission) {
        authorizationService.requirePermission(permission);
        if ("staff_payroll".equals(item.sourceType())) {
            if (!"finance.ar_ap.view".equals(permission)) rejectPayrollMutation(item);
            if (payrollFinance == null) throw ApiException.forbidden("PAYROLL_FINANCE_PERMISSION_REQUIRED", "工资需单独授权查看");
            payrollFinance.requireReadScope(item.sourceId(), FinancePayrollService.VIEW);
        }
        var actor=authorizationService.currentActor();
        var reach=authorizationService.projectReach(actor,permission);
        if (!reach.platformAdmin() && trimToNull(actor.merchantId())!=null && !actor.merchantId().equals(item.merchantId()))
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN","无权操作其他商户的数据");
        if (!reach.covers(item.projectId()))
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN","无权操作其他项目或缺少项目归属的财务数据");
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "无权操作其他商户的数据");
        }
    }

    /**
     * P1-A 财务项目级隔离(与 checkMerchantOwnership 同范式,两维并存):
     * 项目 scope 调用方(currentProjectScopeQuietly 非空,即带综合体归属的项目财务员)不得读写他项目台账。
     * 平台/无项目归属调用方返回 empty→不拦(全量);资源 project_id 为 null(历史未回填行)→不拦(不误伤,依赖 merchant 维)。
     */
    private void checkProjectScope(String resourceProjectId) {
        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
        if (scope.isPresent() && resourceProjectId != null && !scope.get().equals(resourceProjectId)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN",
                    "无权操作其他项目的财务数据");
        }
    }

    /**
     * P3 现场渠道退款设备门控(服务端判定,非请求体可空 deviceId):
     * 现场终端渠道(cash/pos=线下现金/刷卡,天然发生在现场设备)退款必须携带 deviceId 走 MDM 门控;
     * 缺设备上下文=fail-closed 拒绝,堵"现场退款不带 deviceId 即绕过设备门控"。
     * 后台/线上渠道(manual/第三方等)无现场设备语义,不强制。
     */
    private static final Set<String> ONSITE_REFUND_CHANNELS = Set.of("cash", "pos");

    private void requireDeviceContextForOnsiteChannel(String refundChannel, String deviceId) {
        String channel = trimToNull(refundChannel);
        if (channel != null && ONSITE_REFUND_CHANNELS.contains(channel.toLowerCase())
                && trimToNull(deviceId) == null) {
            throw ApiException.forbidden("DEVICE_CONTEXT_REQUIRED",
                    "现场渠道退款必须在运营设备上操作（缺少设备标识）");
        }
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        return operations.readPage(
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action,
                this::businessId,
                Map.of()
        );
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private long requirePositive(Long value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        if (value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须大于 0");
        }
        return value;
    }

    private String requireEnum(String value, Set<String> allowed, String fieldName) {
        String trimmed = requireText(value, fieldName);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return trimmed;
    }

    private String optionalEnum(String value, Set<String> allowed, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }

    private String businessId(Object value) {
        if (value instanceof ArApItemRecord record) {
            return record.id();
        }
        if (value instanceof ArApPaymentResponse response) {
            return response.payment().id();
        }
        if (value instanceof DailyCloseRecord record) {
            return record.id();
        }
        if (value instanceof PaymentIntentRecord record) {
            return record.id();
        }
        if (value instanceof OfflinePaymentConfirmationRecord record) {
            return record.id();
        }
        if (value instanceof FinanceRefundRecord record) {
            return record.id();
        }
        if (value instanceof ArApAdjustmentRecord record) {
            return record.id();
        }
        if (value instanceof ReconciliationDifferenceRecord record) {
            return record.id();
        }
        if (value instanceof DepositLedgerRecord record) {
            return record.id();
        }
        return null;
    }
}
