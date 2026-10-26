package com.complex.platform.inventory;

// 文件版本：V0.11

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerOtpVerifier;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.dto.CustomerSavedBottleRenewRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.evidence.TraceabilityProcessInstanceRecord;
import com.complex.platform.evidence.TraceabilityService;
import com.complex.platform.evidence.TraceabilityStepRecord;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import com.complex.platform.gate.TerminalSwitchGuard;
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.RegisterTraceCodeRequest;
import com.complex.platform.inventory.dto.SavedBottleCancelRequest;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleExitNoticeRequest;
import com.complex.platform.inventory.dto.SavedBottleHandoverRequest;
import com.complex.platform.inventory.dto.SavedBottleRemedyActionRequest;
import com.complex.platform.inventory.dto.SavedBottleRemedyRequest;
import com.complex.platform.inventory.dto.SavedBottleManualPickupDispenseRequest;
import com.complex.platform.inventory.dto.SavedBottleManualPickupRequest;
import com.complex.platform.inventory.dto.SavedBottleRenewalRequest;
import com.complex.platform.inventory.dto.SavedBottleVerificationCodeRequest;
import com.complex.platform.inventory.dto.SavedBottleTransferRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.inventory.dto.SupplierReconciliationRequest;
import com.complex.platform.inventory.dto.TraceExceptionReviewRequest;
import com.complex.platform.inventory.dto.TraceHandoverCreateRequest;
import com.complex.platform.inventory.dto.TraceRobotReportRequest;
import com.complex.platform.inventory.dto.TraceScanSegmentRequest;
import com.complex.platform.notification.NotificationService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.complex.platform.common.Strings.trimToNull;
import com.complex.platform.common.Strings;
import static com.complex.platform.common.Strings.requireText;

@Service
public class InventoryService implements com.complex.platform.finance.SavedBottleRemedyRecoveryPort {
    @org.springframework.beans.factory.annotation.Autowired PurchaseWorkflowService purchaseWorkflow;
    @org.springframework.beans.factory.annotation.Autowired InventoryTransferFlowService transferFlow;
    /** OBS-13-02：到期存酒逐瓶转库存走本类代理的小事务（同 LoyaltyService.evaluateLevels 范式）；单测未注入时直接调用。 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    org.springframework.context.ApplicationContext applicationContext;

    // FS-016（BSM L1073/L1198）：客人短信验证码端口 + 按手机号限速（与 CustomerService.enforceOtpRateLimit 同口径）。
    // 字段注入：不改 13 参构造器与各单测；单测直接置桩。未注入 = 短信服务不可用，核验 fail-closed。
    @org.springframework.beans.factory.annotation.Autowired(required = false) CustomerOtpVerifier otpVerifier;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.common.ratelimit.RateLimiter rateLimiter;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.common.ratelimit.RateLimitProperties rateLimitProperties;
    // 动态取酒码有效窗口 = 全场统一刷新间隔（ticket_qr_refresh_config，BSM L585「这个刷新间隔全场用同一套统一设置、不写死」）。
    java.util.function.IntSupplier pickupCodeWindowSeconds;
    java.time.Clock clock = java.time.Clock.systemUTC();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setTicketingRepository(com.complex.platform.ticketing.TicketingRepository ticketingRepository) {
        this.pickupCodeWindowSeconds = ticketingRepository::findQrRefreshIntervalSeconds;
    }

    // FS-017（BSM L1072 ①②③④）：补存/赔付经营损失与垫付追收走 FinanceService 统一回写点（@Lazy 防初始化环；单测置桩）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    com.complex.platform.finance.FinanceService financeService;
    /** 内部结算价（分）：bottle → 目录生效价；未配价为空（对客补存不因此阻断，损失留财务按 R6-D08 补计）。 */
    java.util.function.Function<SavedBottleRecord, Optional<Long>> settlementPriceResolver = bottle -> Optional.empty();
    /** 项目 → 酒吧域归属的娱乐平台公司主体（revenue_stream_attribution，all_pass_ticket 流，与 SettlementExpiryJob 同口径）。 */
    java.util.function.Function<String, String> managementEntityResolver = projectId -> null;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setSettlementPriceResolver(com.complex.platform.ticketing.InternalSettlementPriceResolver resolver) {
        this.settlementPriceResolver = bottle -> resolver.resolve(bottle.itemId(), bottle.merchantId(), null);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setAttributionRepository(com.complex.platform.settlement.RevenueStreamAttributionRepository attribution) {
        this.managementEntityResolver = projectId -> projectId == null ? null
                : attribution.resolveManagementEntity(projectId, PLATFORM_ENTITY_STREAM);
    }

    private static final String PLATFORM_ENTITY_STREAM = "all_pass_ticket";
    private static final String STORED_WINE_EXIT_NOTICE_TEMPLATE = "sms.stored_wine.exit_notice";
    private static final String STORED_WINE_HANDOVER_TEMPLATE = "sms.stored_wine.handover";
    /** 赔付方案审批：门店主管用高价值复核码，财务用既有应收应付管理码（BSM L1072「经门店主管和财务审批后支付」）。 */
    static final String REMEDY_SUPERVISOR_APPROVE = "inventory.high_value.review";
    static final String REMEDY_FINANCE_APPROVE = "finance.ar_ap.manage";
    private static final Set<String> HANDOVER_CONDITIONS = Set.of("intact", "damaged", "short");
    private static final String PICKUP_CODE_PREFIX = "swp_";
    private static final String OTP_SCENE_PREFIX = "inventory.saved_bottle.";
    static final String MANUAL_PICKUP_INITIATE = "bar.saved_bottle.manual_pickup.initiate";
    static final String MANUAL_PICKUP_REVIEW = "bar.saved_bottle.manual_pickup.review";
    static final String MANUAL_PICKUP_CONFIRM_MARKETING = "bar.saved_bottle.manual_pickup.confirm_marketing";

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final String STORED_WINE_DOMAIN = "stored_wine";
    // 短信里给客人看的到期日:所在地(北京)日历日 + 中文写法(「所有终端零英文全中文」铁律)。
    // 旧写法直接塞 Instant.toString(),客人收到 2026-08-02T18:00:00Z——既是英文时间戳,又比北京日期早一天。
    private static final java.time.format.DateTimeFormatter EXPIRE_DATE_TEXT =
            java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final String STORED_WINE_DEPOSIT_TEMPLATE = "sms.stored_wine.deposit";
    private static final String STORED_WINE_CLAIM_TEMPLATE = "sms.stored_wine.claim";
    private static final String STORED_WINE_EXPIRING_TEMPLATE = "sms.stored_wine.expiring";
    private static final String STORED_WINE_EXPIRED_TEMPLATE = "sms.stored_wine.expired";
    private static final Set<String> STOCK_BATCH_STATUSES = Set.of("active", "consumed", "expired", "archived");
    private static final Set<String> SUPPLIER_RECONCILIATION_STATUSES = Set.of("pending", "matched", "difference_found");

    // R5 T17：五段交接链（seq 1..5）。dispatch 段为机器人被动留痕，其余为员工主动扫码。
    private static final List<String> HANDOVER_SEGMENTS =
            List.of("out_verify", "dispatch", "kitchen_receive", "staff_pickup", "deliver_customer");
    private static final String ROBOT_SEGMENT = "dispatch";
    private static final String ROBOT_SYNC_CAPABILITY = "integration.robot.sync";

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final InventoryStockMovementService stockMovementService;
    private final TerminalSwitchGuard terminalSwitchGuard;
    private final InventoryHighValueReviewService highValueReviewService;
    /** V350：「高价值」判定全平台唯一入口（存酒登记快照 / 存酒作废 / 报损 / 盘点差异共用）。 */
    private final HighValueDecisionService highValueDecisionService;
    private final TraceabilityService traceabilityService;
    // P1：evidence 统一异常责任链服务。库存风险扫描命中 + 溯源交接断链异常，除写孤岛表外再接入统一
    // risk_alerts（source_domain='inventory'），让统一表真实承载本域风险数据。复刻 Wave 1 Wecom 先例。
    // 可为 null（向后兼容/旧 6 参构造器/单测无注入）；接入为 fail-safe，不阻塞主链。
    private final RiskAlertService riskAlertService;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final com.complex.platform.finance.FinanceRepository financeRepository;
    // V296 期间成本汇总的经营日切分（与财务日结/跨域汇总同口径）。旧构造器（单测）下为 null，
    // getCostSummary 使用时要求非空（S-2：不做硬编码兜底，防口径漂移）。
    private final com.complex.platform.common.BusinessDayResolver businessDayResolver;
    private static final com.complex.platform.common.BusinessDayResolver FACTORY_DAYS = new com.complex.platform.common.BusinessDayResolver();

    /** 盘点、调拨落账日按营业日；未经 Spring 装配（旧构造器/单测）按出厂起点。成本汇总仍要求注入，见 getCostSummary。 */
    private com.complex.platform.common.BusinessDayResolver days() {
        return businessDayResolver != null ? businessDayResolver : FACTORY_DAYS;
    }

    // 本域风险级 → 统一表 RISK_LEVELS({normal,suspicious,high,critical}) 映射。
    // inventory 用的 medium 在统一表无对应枚举，必须映射为 suspicious，否则 report 抛 VALIDATION_FAILED。
    private static final Map<String, String> RISK_LEVEL_TO_UNIFIED = Map.of(
            "medium", "suspicious",
            "high", "high");

    @org.springframework.beans.factory.annotation.Autowired
    public InventoryService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            TerminalSwitchGuard terminalSwitchGuard,
            TraceabilityService traceabilityService,
            RiskAlertService riskAlertService,
            NotificationService notificationService,
            CustomerRepository customerRepository,
            com.complex.platform.finance.FinanceRepository financeRepository,
            com.complex.platform.common.BusinessDayResolver businessDayResolver,
            HighValueDecisionService highValueDecisionService,
            com.complex.platform.evidence.EvidenceAttachmentValidator evidenceAttachmentValidator
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.terminalSwitchGuard = terminalSwitchGuard;
        this.traceabilityService = traceabilityService;
        this.riskAlertService = riskAlertService;
        this.notificationService = notificationService;
        this.customerRepository = customerRepository;
        this.financeRepository = financeRepository;
        this.businessDayResolver = businessDayResolver;
        // 未经 Spring 装配（旧构造器/单测）时自建一个：其阈值端口为空 ⇒ 阈值恒「未配置」⇒ 一律判高价值。
        // 这正是业务口径的从严侧——「没接线」绝不能被伪装成「低价值放行」。
        this.highValueDecisionService = highValueDecisionService != null
                ? highValueDecisionService
                : new HighValueDecisionService(repository, authorizationService);
        this.highValueReviewService = new InventoryHighValueReviewService(
                repository, idempotencyService, auditEventService, authorizationService,
                evidenceAttachmentValidator);
        this.stockMovementService = new InventoryStockMovementService(
                repository, idempotencyService, auditEventService, authorizationService,
                highValueReviewService, this.highValueDecisionService);
    }

    public PageResponse<InventoryItemRecord> listInventoryItems() {
        return readPage(
                "inventory.item.viewed",
                "inventory.item.viewed_failed",
                "inventory.stock.view",
                "inventory_item",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listInventoryItemsByMerchant)
                                .orElseGet(repository::listInventoryItems);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public InventoryItemRecord createInventoryItem(InventoryItemRequest request) {
        return executeWrite(
                "createInventoryItem",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryItemRecord.class,
                "inventory.item.created",
                "inventory.item.created_failed",
                "inventory.stock.move",
                "inventory_item",
                () -> {
                    InventoryItemRequest actual = requireRequest(request);
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    InventoryItemRecord item = new InventoryItemRecord(
                            "item_" + slug(actual.idempotencyKey()),
                            null,
                            requireText(actual.itemName(), "item_name"),
                            requireText(actual.category(), "category"),
                            requireText(actual.unit(), "unit"),
                            null,
                            false,
                            nonNegativeOrDefault(actual.minQuantity(), 0, "min_quantity"),
                            trimToDefault(actual.status(), "active"),
                            now,
                            now,
                            merchantId,
                            Boolean.TRUE.equals(actual.manualHighValue()),
                            Boolean.TRUE.equals(actual.nonConsumable())
                    );
                    return repository.saveInventoryItem(item);
                }
        );
    }

    public PageResponse<PurchaseRequestRecord> listPurchaseRequests() {
        return purchaseWorkflow.list();
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.purchase_request.manage")
    @Transactional
    public PurchaseRequestRecord createPurchaseRequest(PurchaseRequestCreate request) {
        if (request == null || request.items() == null || request.items().size() != 1) {
            throw ApiException.badRequest("PURCHASE_SINGLE_ITEM_REQUIRED", "每份采购申请只允许一个商品，请分单提交");
        }
        if (request.supplierId() == null || request.supplierId().isBlank()) {
            throw ApiException.badRequest("PURCHASE_SUPPLIER_REQUIRED", "采购申请须关联供应商");
        }
        return purchaseWorkflow.create(request);
    }

    public PageResponse<SavedBottleRecord> listSavedBottles() {
        return listSavedBottles(null);
    }

    /**
     * FS-016 顺带：检索存酒记录只能输入手机号（BSM L1073 / PGM L878），phone 非空时按手机哈希精确匹配；
     * 这是一次按手机号查客人的动作，审计留痕记下查的哪个手机号（哈希）、谁查的（PGM L878「查了谁、谁查的都留痕」）。
     */
    public PageResponse<SavedBottleRecord> listSavedBottles(String phone) {
        String number = trimToNull(phone);
        String actor = currentActorId();
        Map<String, Object> lookup = number == null ? Map.of()
                : Map.of("phone_hash", phoneHash(number), "queried_by", actor == null ? "unknown" : actor);
        return readPage(
                "bar.saved_bottle.viewed",
                "bar.saved_bottle.view_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                lookup,
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (number != null) {
                        return repository.listSavedBottlesByPhoneHash(phoneHash(number), scope.orElse(null));
                    }
                    return scope.map(repository::listSavedBottlesByMerchant)
                                .orElseGet(repository::listSavedBottles);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage")
    @Transactional
    public SavedBottleRecord createSavedBottle(SavedBottleRequest request) {
        return executeWrite(
                "createSavedBottle",
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRecord.class,
                "bar.saved_bottle.created",
                "bar.saved_bottle.create_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    SavedBottleRequest actual = requireRequest(request);
                    Map<String, Object> item = firstItem(actual.items(), "items");
                    String itemId = requireItemText(item, "item_id");
                    int quantity = requirePositiveItemQuantity(item, "quantity");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    // P0#4 散客存酒（R6-D05）：customer_id 可空（不再 requireText 强制），允许无会员散客存酒。
                    // 登记必填核身三件套：姓名/昵称 + 瓶底标签值 + 登记照片证据（取酒核身依据）。
                    String customerId = trimToNull(actual.customerId());
                    String nameOrNickname = requireText(actual.customerNameOrNickname(), "customer_name_or_nickname");
                    String bottleLabelValue = requireText(actual.bottleLabelValue(), "bottle_label_value");
                    // FS-016：登记手机号必填（BSM L1072「记手机号」；取酒/转让按当前持有人手机短信核验，无手机号即无法核验）。
                    String customerPhone = requireText(actual.customerPhone(), "customer_phone");
                    List<String> registrationPhotoRefs = safeList(actual.registrationPhotoRefs());
                    if (registrationPhotoRefs.isEmpty()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "registration_photo_refs is required");
                    }
                    // V350 归一：登记当刻按全平台唯一口径判一次，落库作快照（不再收前端传的布尔）。
                    // 作废时以 cancelSavedBottle 里的实时重判为准——阈值/进货价会随运营变，快照只为事后举证。
                    HighValueDecisionService.HighValueDecision registrationDecision =
                            highValueDecisionService.decide(itemId, merchantId);
                    SavedBottleRecord savedBottle = repository.saveSavedBottle(new SavedBottleRecord(
                            "saved_" + slug(actual.idempotencyKey()),
                            customerId,
                            requireText(actual.barId(), "bar_id"),
                            itemId,
                            trimToDefault(textFromItem(item, "bottle_name"), itemId),
                            quantity,
                            quantity,
                            trimToDefault(textFromItem(item, "unit"), "bottle"),
                            actual.expireAt(),
                            "stored",
                            safeList(actual.evidenceAttachmentIds()),
                            now,
                            now,
                            merchantId,
                            registrationDecision.highValue(),
                            registrationDecision.thresholdCent(),
                            bottleLabelValue,
                            nameOrNickname,
                            customerPhone,
                            phoneHash(customerPhone),
                            registrationPhotoRefs,
                            trimToNull(actual.manualNote()),
                            null,
                            null,
                            null,
                            null
                    ));
                    notifySavedBottleAfterCommit(
                            savedBottle,
                            "deposit",
                            STORED_WINE_DEPOSIT_TEMPLATE,
                            "swdeposit:" + savedBottle.id(),
                            null);
                    return savedBottle;
                }
        );
    }

    /** 取酒核身用：手机号 SHA-256 哈希（精确检索，不依赖明文解密）。null/空返回 null。 */
    private static String phoneHash(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(phone.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /** 重建存酒记录（携带 A1 扩展字段），仅改 status/remaining/updatedAt。 */
    private SavedBottleRecord savedBottleWith(SavedBottleRecord b, String status, int remaining, Instant updatedAt) {
        return new SavedBottleRecord(b.id(), b.customerId(), b.barProjectId(), b.itemId(), b.bottleName(),
                b.initialQuantity(), remaining, b.unit(), b.expireAt(), status, b.evidenceAttachmentIds(),
                b.createdAt(), updatedAt, b.merchantId(), b.isHighValue(), b.highValueThresholdCents(),
                b.bottleLabelValue(), b.customerNameOrNickname(), b.customerPhone(), b.customerPhoneHash(),
                b.registrationPhotoRefs(), b.manualNote(),
                b.transferredAt(), b.transferToNameOrNickname(), b.transferToPhone(), b.transferToPhoneHash(),
                b.remedyOfSavedBottleId(), b.custodianEntityId(), b.pickupLocation(), b.handedOverAt(), b.handoverId());
    }

    public void processSavedBottleNotifications(Instant now) {
        Instant actualNow = now == null ? Instant.now() : now;
        processExpiringSavedBottleNotifications(actualNow);
        processExpiredSavedBottleNotifications(actualNow);
        postExpiredSavedBottlesToStock(actualNow);
    }

    /**
     * OBS-13-02（BSM L1072「存酒到期后，系统自动结束客户存酒权益，该瓶酒先转为所在酒吧的自有库存资产；转为库存时不确认收入」；L1194 库存流水
     * 「回库（……客人存酒到期转为酒吧自有库存）」；L863「放弃的存酒按存酒到期规则转为所在酒吧的自有库存资产」）：已结束权益（到期或注销放弃，
     * status=expired）且尚未入库的瓶逐瓶小事务转入库存；失败（没有或不止一个在用存酒库房、盘点冻结）只留失败审计，下一次作业再试。
     */
    private void postExpiredSavedBottlesToStock(Instant now) {
        InventoryService self = applicationContext != null ? applicationContext.getBean(InventoryService.class) : this;
        List<SavedBottleRecord> bottles;
        try {
            bottles = repository.listExpiredSavedBottlesPendingStock(null);
        } catch (RuntimeException exception) {
            log.warn("failed to list expired saved bottles pending stock", exception);
            return;
        }
        for (SavedBottleRecord bottle : bottles) {
            try {
                self.postExpiredSavedBottleToStock(bottle.id(), now);
            } catch (RuntimeException exception) {
                auditEventService.recordFailure("bar.saved_bottle.return_to_stock_failed", "not_applicable", "saved_bottle",
                        bottle.id(), null, null, codeOf(exception));
            }
        }
    }

    /**
     * 单瓶转库存：数量按标签剩余量（BSM L1169），单价不由本流水自定——库存成本引擎按既有无价入库口径落成本（有基准按当前均价，无基准成本列为空；
     * 回库单价与科目由财务按 R6-D08 复核确定，B-03/E-14）；不写任何收入、应收应付或结算行。入账库房＝所在酒吧在用的存酒库房；
     * 交平台保管的瓶（操作员裁决补充第十四批第 3 条）＝接收保管的娱乐平台公司在该项目的存酒库房（平台库房无商户归属）。
     */
    @Transactional
    public void postExpiredSavedBottleToStock(String savedBottleId, Instant now) {
        SavedBottleRecord bottle = repository.listExpiredSavedBottlesPendingStock(savedBottleId).stream().findFirst().orElse(null);
        if (bottle == null) {
            return;
        }
        String projectId = repository.findBarProjectId(bottle.merchantId()).orElse(null);
        String owner = bottle.custodianEntityId() == null ? bottle.merchantId() : null;
        List<WarehouseRecord> warehouses = projectId == null ? List.of() : repository.listActiveSavedBottleWarehouses(projectId, owner);
        if (warehouses.size() != 1) {
            throw ApiException.conflict(warehouses.isEmpty() ? "SAVED_BOTTLE_STOCK_WAREHOUSE_MISSING" : "SAVED_BOTTLE_STOCK_WAREHOUSE_AMBIGUOUS",
                    "须有且只有一个在用的存酒库房接收到期存酒");
        }
        String warehouseId = warehouses.getFirst().id();
        if (repository.hasActiveStocktakeForWarehouse(warehouseId)) {
            throw ApiException.conflict("INVENTORY_WAREHOUSE_STOCKTAKE_LOCKED", "仓库正在盘点中，盘点结束前库存变动已冻结");
        }
        int quantity = bottle.remainingQuantity();
        repository.atomicIncreaseStockBalance(bottle.itemId(), warehouseId, quantity,
                "balance_" + slug(bottle.itemId() + ":" + warehouseId), owner, warehouseId, projectId);
        repository.saveStockMovement(new StockMovementRecord("movement_" + slug(bottle.id() + ":to_stock"), "return",
                bottle.itemId(), null, warehouseId, quantity, null, null, "saved_bottle_to_stock", bottle.id(), null, null,
                List.of(), "completed", now, owner), projectId);
        auditEventService.recordSuccess("bar.saved_bottle.returned_to_stock", "not_applicable", "saved_bottle", bottle.id(),
                null, null, Map.of("warehouse_id", warehouseId, "quantity", quantity));
    }

    /**
     * BSM L1072「到期前按后台配置的提醒节点各发一次短信提醒（业务参数，按项目可配，出厂默认到期前第 14 天、第 7 天、第 3 天、第 1 天）；
     * 提醒节点与存酒有效期同一粒度配置，超出本次存酒实际有效期的节点自动跳过」：节点按存酒所在项目取（缺行回落 global 出厂行），
     * 距到期天数按北京日历日算；每次只发已到达的最近一个节点，节点大于本次有效期（登记日至当前到期日的天数）的跳过；
     * 幂等键＝瓶＋到期日＋节点，每个节点只发一次，转存顺延后按新到期日重新起算。
     */
    private void processExpiringSavedBottleNotifications(Instant now) {
        Map<String, List<Integer>> policies;
        List<SavedBottleRecord> bottles;
        try {
            policies = repository.listSavedBottleReminderPolicies();
            int maxDays = policies.values().stream().flatMap(List::stream).max(Integer::compare).orElse(0);
            bottles = maxDays == 0 ? List.of()
                    : repository.listStoredSavedBottlesExpiring(now, now.plus(Duration.ofDays(maxDays + 1L)));
        } catch (RuntimeException exception) {
            log.warn("failed to list expiring saved bottles for notification", exception);
            return;
        }
        LocalDate today = now.atZone(LocalZone.ID).toLocalDate();
        for (SavedBottleRecord bottle : bottles) {
            LocalDate expireDate = bottle.expireAt().atZone(LocalZone.ID).toLocalDate();
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, expireDate);
            long validityDays = java.time.temporal.ChronoUnit.DAYS.between(
                    bottle.createdAt().atZone(LocalZone.ID).toLocalDate(), expireDate);
            String projectId = repository.findBarProjectId(bottle.barProjectId()).orElse(null);
            List<Integer> nodes = policies.getOrDefault("project:" + projectId, policies.getOrDefault("global", List.of()));
            nodes.stream().filter(days -> days <= validityDays && daysLeft <= days).min(Integer::compare)
                    .ifPresent(node -> notifySavedBottleNow(
                            bottle,
                            "expiring",
                            STORED_WINE_EXPIRING_TEMPLATE,
                            "swexpiring:" + bottle.id() + ":" + expireDateKey(bottle) + ":" + node,
                            null));
        }
    }

    private void processExpiredSavedBottleNotifications(Instant now) {
        List<SavedBottleRecord> bottles;
        try {
            bottles = repository.listStoredSavedBottlesExpired(now);
        } catch (RuntimeException exception) {
            log.warn("failed to list expired saved bottles for notification", exception);
            return;
        }
        for (SavedBottleRecord bottle : bottles) {
            notifySavedBottleNow(
                    bottle,
                    "expired",
                    STORED_WINE_EXPIRED_TEMPLATE,
                    "swexpired:" + bottle.id(),
                    null);
            try {
                repository.saveSavedBottle(savedBottleWith(bottle, "expired", bottle.remainingQuantity(), now));
            } catch (RuntimeException exception) {
                log.warn("failed to mark saved bottle expired: bottle_id={}", bottle.id(), exception);
            }
        }
    }

    private void notifySavedBottleAfterCommit(
            SavedBottleRecord bottle,
            String event,
            String templateCode,
            String idempotencyKey,
            SavedBottleClaimRecord claim
    ) {
        if (notificationService == null) {
            return;
        }
        Runnable notify = () -> notifySavedBottleNow(bottle, event, templateCode, idempotencyKey, claim);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notify.run();
                }
            });
            return;
        }
        notify.run();
    }

    private void notifySavedBottleNow(
            SavedBottleRecord bottle,
            String event,
            String templateCode,
            String idempotencyKey,
            SavedBottleClaimRecord claim
    ) {
        if (notificationService == null) {
            return;
        }
        try {
            notificationService.notify(
                    STORED_WINE_DOMAIN,
                    event,
                    resolveSavedBottleRecipient(bottle),
                    templateCode,
                    savedBottleNotificationParams(bottle, claim),
                    idempotencyKey);
        } catch (RuntimeException exception) {
            log.warn(
                    "saved bottle notification failed: event={}, bottle_id={}, idempotency_key={}",
                    event,
                    bottle == null ? null : bottle.id(),
                    idempotencyKey,
                    exception);
        }
    }

    private String resolveSavedBottleRecipient(SavedBottleRecord bottle) {
        if (bottle == null) {
            return null;
        }
        String customerId = trimToNull(bottle.customerId());
        if (customerId != null && customerRepository != null) {
            try {
                String phone = customerRepository.findCustomerById(customerId)
                        .map(CustomerRecord::phone)
                        .map(Strings::trimToNull)
                        .orElse(null);
                if (phone != null) {
                    return phone;
                }
            } catch (RuntimeException exception) {
                log.warn("failed to resolve saved bottle customer phone: customer_id={}", customerId, exception);
            }
        }
        // 会员档案查不到手机时回退登记/受让时记录的手机号（散客与转让后的受让人只有这一处）。
        return trimToNull(bottle.customerPhone());
    }

    private Map<String, Object> savedBottleNotificationParams(SavedBottleRecord bottle, SavedBottleClaimRecord claim) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (bottle != null) {
            putIfNotNull(params, "saved_bottle_id", bottle.id());
            putIfNotNull(params, "bottle", bottle.bottleName());
            putIfNotNull(params, "bottle_name", bottle.bottleName());
            putIfNotNull(params, "item", bottle.itemId());
            putIfNotNull(params, "item_id", bottle.itemId());
            putIfNotNull(params, "expireAt", bottle.expireAt() == null ? null
                    : EXPIRE_DATE_TEXT.format(bottle.expireAt().atZone(LocalZone.ID)));
            putIfNotNull(params, "remaining_quantity", bottle.remainingQuantity());
            putIfNotNull(params, "pickup_location", bottle.pickupLocation());
        }
        if (claim != null) {
            putIfNotNull(params, "claim_id", claim.id());
            putIfNotNull(params, "claim_quantity", claim.claimQuantity());
        }
        return params;
    }

    private void putIfNotNull(Map<String, Object> params, String key, Object value) {
        if (value != null) {
            params.put(key, value);
        }
    }

    private String expireDateKey(SavedBottleRecord bottle) {
        if (bottle == null || bottle.expireAt() == null) {
            return "none";
        }
        return bottle.expireAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
    }

    /**
     * FS-016 存酒转让（BSM L1198）：转让须经出让人（当前持有人）手机短信验证码确认，验证成功即完成客户权益人变更
     * （customer_id/姓名昵称/手机号+哈希整体换成受让人，不设等待领取）；单个标签对应的酒瓶是最小转让单位，不拆分；
     * 完整记录转让前后客户、时间与操作来源；原持有人手里的动态取酒码立即全部作废（BSM L585）。
     */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage", admissionAction = "inventory.saved_bottle_claim", objectType = "saved_bottle")
    @Transactional
    public SavedBottleTransferRecord transferSavedBottle(String savedBottleId, SavedBottleTransferRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "transferSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleTransferRecord.class,
                "bar.saved_bottle_transferred",
                "bar.saved_bottle.transfer_failed",
                "bar.saved_bottle.manage",
                "saved_bottle_transfer",
                () -> {
                    SavedBottleTransferRequest actual = requireRequest(request);
                    String toName = requireText(actual.toNameOrNickname(), "to_name_or_nickname");
                    // 受让人手机号必填：受让人此后按其本人手机短信验证码或动态取酒码取酒（BSM L1198）。
                    String toPhone = requireText(actual.toPhone(), "to_phone");
                    String code = requireText(actual.verificationCode(), "verification_code");
                    SavedBottleRecord bottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    requireCustody(bottle);
                    if (!"stored".equals(bottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_TRANSFERABLE", "该存酒不可转赠");
                    }
                    if (bottle.expireAt() != null && bottle.expireAt().isBefore(clock.instant())) {
                        throw ApiException.conflict("SAVED_BOTTLE_EXPIRED", "该存酒已过期");
                    }
                    // 整瓶转让：缺省即全部剩余量；传了数量必须等于剩余量（同一酒瓶的剩余酒量不得拆分给多个客户）。
                    if (actual.transferQuantity() != null && actual.transferQuantity() != bottle.remainingQuantity()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "同一酒瓶的剩余酒量不得拆分，须整瓶转让");
                    }
                    int qty = bottle.remainingQuantity();
                    String holderPhone = verifyHolderSms(bottle, code, "transfer");
                    Instant now = clock.instant();
                    String toPhoneHash = phoneHash(toPhone);
                    String toCustomerId = findCustomerIdByPhone(toPhone);
                    // 验证成功即完成客户权益人变更：持有人三件套整体换成受让人；到期日/剩余量/标签/状态不变。
                    repository.saveSavedBottle(new SavedBottleRecord(
                            bottle.id(), toCustomerId, bottle.barProjectId(), bottle.itemId(), bottle.bottleName(),
                            bottle.initialQuantity(), bottle.remainingQuantity(), bottle.unit(), bottle.expireAt(),
                            bottle.status(), bottle.evidenceAttachmentIds(), bottle.createdAt(), now, bottle.merchantId(),
                            bottle.isHighValue(), bottle.highValueThresholdCents(), bottle.bottleLabelValue(),
                            toName, toPhone, toPhoneHash,
                            bottle.registrationPhotoRefs(), bottle.manualNote(), now, toName, toPhone, toPhoneHash,
                            bottle.remedyOfSavedBottleId(), bottle.custodianEntityId(), bottle.pickupLocation(),
                            bottle.handedOverAt(), bottle.handoverId()));
                    // 权益转赠一经完成，原持有人手里的码立即全部作废（BSM L585）。
                    repository.revokeActiveSavedBottlePickupCodes(bottle.id());
                    return repository.saveSavedBottleTransfer(new SavedBottleTransferRecord(
                            "transfer_" + slug(actual.idempotencyKey()),
                            bottle.id(), bottle.customerId(), bottle.customerNameOrNickname(),
                            toName, toPhone, toPhoneHash, qty, currentActorId(), "transferred",
                            safeList(actual.evidenceAttachmentIds()), now,
                            operationSource(), phoneHash(holderPhone), toCustomerId));
                });
    }

    /**
     * FS-016 取酒（BSM L1073）：必须核验当前客户的短信验证码（verification_code）或官方动态取酒码（swp_ 开头）；
     * 核验在标签/姓名比对之前，核验失败即拒、不建复核行、数量不变；手机号/姓名不单独作为放酒凭证。
     */
    public SavedBottleClaimRecord claimSavedBottle(String savedBottleId, SavedBottleClaimRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "claimSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleClaimRecord.class,
                "bar.saved_bottle_picked_up",
                "bar.saved_bottle.pickup_failed",
                "bar.saved_bottle.manage",
                "saved_bottle_claim",
                () -> {
                    SavedBottleClaimRequest actual = requireRequest(request);
                    String code = requireText(actual.verificationCode(), "verification_code");
                    // P0-2: SELECT FOR UPDATE 防止并发超领
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    requireCustody(savedBottle);
                    requireClaimable(savedBottle);
                    String method;
                    String verifiedPhoneHash;
                    SavedBottlePickupCodeRecord pickupCode = null;
                    if (code.startsWith(PICKUP_CODE_PREFIX)) {
                        pickupCode = requireActivePickupCode(savedBottle, code);
                        method = "pickup_code";
                        verifiedPhoneHash = savedBottle.customerPhoneHash();
                    } else {
                        method = "sms";
                        verifiedPhoneHash = phoneHash(verifyHolderSms(savedBottle, code, "claim"));
                    }
                    return dispenseSavedBottle(savedBottle, actual.idempotencyKey(), actual.claimItems(),
                            actual.bottleLabelValue(), actual.customerNameOrNickname(), actual.evidenceAttachmentIds(),
                            method, verifiedPhoneHash, pickupCode);
                }
        );
    }

    /**
     * 核验通过后的出酒：标签值/姓名昵称比对（登记了才比，不再把验证码当标签值回退），不符进 pending_identity_review 不出酒；
     * 留痕操作员工=当前操作者、终端来源、取用前后剩余量、核验方式与被核验手机哈希（BSM L1073）。
     */
    private SavedBottleClaimRecord dispenseSavedBottle(
            SavedBottleRecord savedBottle,
            String idempotencyKey,
            List<Map<String, Object>> claimItems,
            String bottleLabelValue,
            String customerNameOrNickname,
            List<String> evidenceAttachmentIds,
            String verificationMethod,
            String verifiedPhoneHash,
            SavedBottlePickupCodeRecord pickupCode
    ) {
        Instant now = clock.instant();
        String staffId = currentActorId();
        String terminal = operationSource();
        int before = savedBottle.remainingQuantity();
        String claimLabel = trimToNull(bottleLabelValue);
        String claimName = trimToNull(customerNameOrNickname);
        boolean labelMismatch = savedBottle.bottleLabelValue() != null
                && !savedBottle.bottleLabelValue().equals(claimLabel);
        boolean nameMismatch = savedBottle.customerNameOrNickname() != null
                && !savedBottle.customerNameOrNickname().equals(claimName);
        if (labelMismatch || nameMismatch) {
            repository.saveSavedBottle(savedBottleWith(savedBottle, "pending_identity_review", before, now));
            SavedBottleClaimRecord review = repository.saveSavedBottleClaim(new SavedBottleClaimRecord(
                    "claim_" + slug(idempotencyKey), savedBottle.id(), 0, staffId, terminal, null,
                    "pending_identity_review", safeList(evidenceAttachmentIds), now,
                    before, before, verificationMethod, verifiedPhoneHash, savedBottle.pickupLocation()));
            consumePickupCode(pickupCode, review.id());
            return review;
        }
        Map<String, Object> claimItem = firstItem(claimItems, "claim_items");
        int quantity = requirePositiveItemQuantity(claimItem, "quantity");
        if (quantity > before) {
            throw ApiException.conflict("SAVED_BOTTLE_INSUFFICIENT", "存酒剩余数量不足");
        }
        int remaining = before - quantity;
        SavedBottleRecord updatedBottle = repository.saveSavedBottle(savedBottleWith(
                savedBottle, remaining == 0 ? "claimed" : "stored", remaining, now));
        SavedBottleClaimRecord claim = repository.saveSavedBottleClaim(new SavedBottleClaimRecord(
                "claim_" + slug(idempotencyKey), savedBottle.id(), quantity, staffId, terminal, null,
                "picked_up", safeList(evidenceAttachmentIds), now,
                before, remaining, verificationMethod, verifiedPhoneHash, savedBottle.pickupLocation()));
        // 取酒成功后该瓶已出示的动态码一律作废（本次用掉的记 consumed）。
        repository.revokeActiveSavedBottlePickupCodes(savedBottle.id());
        consumePickupCode(pickupCode, claim.id());
        notifySavedBottleAfterCommit(
                updatedBottle,
                "claim",
                STORED_WINE_CLAIM_TEMPLATE,
                "swclaim:" + claim.id(),
                claim);
        return claim;
    }

    /** FS-016/FS-025：员工侧向当前持有人手机发送短信验证码（scene ∈ transfer | claim | renew），按手机号限速。 */
    @Transactional
    public SavedBottleVerificationCodeReceipt requestSavedBottleVerificationCode(
            String savedBottleId, SavedBottleVerificationCodeRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "requestSavedBottleVerificationCode:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleVerificationCodeReceipt.class,
                "bar.saved_bottle.verification_code_requested",
                "bar.saved_bottle.verification_code_request_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    SavedBottleVerificationCodeRequest actual = requireRequest(request);
                    String scene = requireText(actual.scene(), "scene").trim().toLowerCase(Locale.ROOT);
                    if (!Set.of("transfer", "claim", RENEWAL_SCENE).contains(scene)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "scene 只能是 transfer、claim 或 renew");
                    }
                    SavedBottleRecord bottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    requireCustody(bottle);
                    String holderPhone = requireHolderPhone(bottle);
                    if (otpVerifier == null) {
                        throw ApiException.conflict("CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信服务未配置，请联系管理员");
                    }
                    enforceOtpRateLimit(holderPhone);
                    otpVerifier.requestCode(holderPhone, OTP_SCENE_PREFIX + scene);
                    return new SavedBottleVerificationCodeReceipt(bottle.id(), scene, clock.instant());
                });
    }

    /**
     * FS-016 客人端签发官方动态取酒码（BSM L867/L1073）：只对当前持有人签发（customer_id 本人，或登记/受让手机哈希为本人手机）；
     * 窗口 = 全场统一刷新间隔；一瓶同时只一枚有效码，签发前旧码作废；明文只回给客人，库里只存 SHA-256。
     */
    @Transactional
    public SavedBottlePickupCodeResponse issueSavedBottlePickupCode(String savedBottleId, String customerId, String customerPhone) {
        try {
            return issuePickupCodeInternal(savedBottleId, customerId, customerPhone);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("bar.saved_bottle.pickup_code_issue_failed", "not_applicable", "saved_bottle",
                    savedBottleId == null ? "unknown" : savedBottleId, null, null, codeOf(exception));
            throw exception;
        }
    }

    private SavedBottlePickupCodeResponse issuePickupCodeInternal(String savedBottleId, String customerId, String customerPhone) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        String customer = requireText(customerId, "customer_id");
        SavedBottleRecord bottle = repository.findSavedBottleByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
        if (!isHolder(bottle, customer, customerPhone)) {
            throw ApiException.forbidden("SAVED_BOTTLE_NOT_HOLDER", "该存酒当前不属于本人");
        }
        requireClaimable(bottle);
        if (pickupCodeWindowSeconds == null) {
            throw ApiException.conflict("SAVED_BOTTLE_PICKUP_CODE_UNAVAILABLE", "动态取酒码刷新间隔未配置");
        }
        int window = pickupCodeWindowSeconds.getAsInt();
        Instant now = clock.instant();
        repository.revokeActiveSavedBottlePickupCodes(bottle.id());
        byte[] random = new byte[16];
        new java.security.SecureRandom().nextBytes(random);
        String code = PICKUP_CODE_PREFIX + HexFormat.of().formatHex(random);
        repository.saveSavedBottlePickupCode(new SavedBottlePickupCodeRecord(
                "swpc_" + java.util.UUID.randomUUID().toString().replace("-", ""),
                bottle.id(), customer, sha256Hex(code), now, now.plusSeconds(window), window, "active", null, now));
        auditEventService.recordSuccess("bar.saved_bottle.pickup_code_issued", "not_applicable", "saved_bottle",
                bottle.id(), null, null, Map.of("customer_id", customer, "refresh_interval_seconds", window));
        return new SavedBottlePickupCodeResponse(bottle.id(), code, now.plusSeconds(window), window);
    }

    /** FS-016 客人端「我的存酒」：本人名下全部 stored 存酒，跨店汇总（每行自带 barProjectId=所属酒吧）。 */
    public List<SavedBottleRecord> listSavedBottlesForCustomer(String customerId, String customerPhone) {
        return repository.listStoredSavedBottlesByHolder(requireText(customerId, "customer_id"), phoneHash(customerPhone));
    }

    /**
     * ROOT-03-R21（BSM 第 863 行「客人可以选择不取存酒……放弃的存酒按存酒到期规则转为所在酒吧的自有库存资产」）：注销时客人已确认放弃的
     * 名下在保管存酒，按到期作业同一写法置 expired（不发到期短信；由存酒到期作业同路转入库存，OBS-13-02），逐瓶留痕；随后清空本人各存酒上的
     * 客人姓名/昵称与手机号（同条「注销后……个人信息一律删除」）。返回放弃的瓶数。
     */
    public int abandonSavedBottlesOnDeregistration(String customerId, String customerPhone, Instant now) {
        List<SavedBottleRecord> bottles = listSavedBottlesForCustomer(customerId, customerPhone);
        for (SavedBottleRecord bottle : bottles) {
            repository.saveSavedBottle(savedBottleWith(bottle, "expired", bottle.remainingQuantity(), now));
            auditEventService.recordSuccess("bar.saved_bottle.abandoned_on_deregistration", "not_applicable", "saved_bottle",
                    bottle.id(), null, null, Map.of("remaining_quantity", bottle.remainingQuantity()));
        }
        repository.clearSavedBottleCustomerIdentity(customerId, phoneHash(customerPhone));
        return bottles.size();
    }

    // ===== FS-025 存酒转存（延期）：ENUM L120 / BSM L1072 =====
    // 「每份存酒的转存次数不得超过后台配置的上限（按项目可配，出厂默认 2 次），只能在当前到期日前办理；每次从当前到期日起顺延该酒吧
    // 当前配置的期限（按酒吧可配，出厂默认 30 天）；每次转存固化当时的配置版本、顺延天数、新到期日与累计转存次数。转让不消耗转存
    // 次数、不改变到期日；受让人继承剩余有效期与已用转存次数。转存须客人本人短信验证码确认（与转让同款）；员工代办转存的另须当班
    // 营销主管复核。转存留痕……加记经办人与客人确认方式。转存次数达到上限后不再受理转存……不设例外通道。」

    static final String RENEWAL_REVIEW = "bar.saved_bottle.renewal.review";
    private static final String RENEWAL_SCENE = "renew";
    private static final String RENEWAL_POLICY_LIMIT = "max_renewals";
    private static final String RENEWAL_POLICY_DAYS = "extension_days";

    /** 员工代办转存：客人本人短信验证码确认 → pending_review；到期日在营销主管复核通过时才顺延。 */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage", admissionAction = "inventory.saved_bottle_claim", objectType = "saved_bottle")
    @Transactional
    public SavedBottleRenewalRecord renewSavedBottle(String savedBottleId, SavedBottleRenewalRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "renewSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRenewalRecord.class,
                "bar.saved_bottle.renewal_requested",
                "bar.saved_bottle.renewal_request_failed",
                "bar.saved_bottle.manage",
                "saved_bottle_renewal",
                () -> {
                    SavedBottleRenewalRequest actual = requireRequest(request);
                    String code = requireText(actual.verificationCode(), "verification_code");
                    SavedBottleRecord bottle = lockSavedBottle(id);
                    requireCustody(bottle);
                    RenewalTerms terms = renewalTerms(bottle);
                    String holderPhone = verifyHolderSms(bottle, code, RENEWAL_SCENE);
                    return repository.saveSavedBottleRenewal(newRenewal("rnw_" + slug(actual.idempotencyKey()), bottle, terms,
                            phoneHash(holderPhone), currentActorId(), operationSource(), "pending_review", clock.instant()));
                });
    }

    /**
     * 当班营销主管复核员工代办转存：复核人不得是经办人；decision=approve 仍须在当前到期日前、通过即从当前到期日顺延；
     * decision=reject 须填 reason，记录置 rejected、复核人/时间/原因留痕，到期日不动且不计入累计转存次数（ENUM L120「复核」含否决）。
     */
    @Transactional
    public SavedBottleRenewalRecord reviewSavedBottleRenewal(String renewalId, SavedBottleRenewalRequest request) {
        String id = requireText(renewalId, "renewal_id");
        return executeWrite(
                "reviewSavedBottleRenewal:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRenewalRecord.class,
                "bar.saved_bottle.renewal_reviewed",
                "bar.saved_bottle.renewal_review_failed",
                RENEWAL_REVIEW,
                "saved_bottle_renewal",
                () -> {
                    SavedBottleRenewalRequest actual = requireRequest(request);
                    String decision = requireText(actual.decision(), "decision").trim().toLowerCase(Locale.ROOT);
                    if (!"approve".equals(decision) && !"reject".equals(decision)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "decision 只能是 approve（通过）或 reject（驳回）");
                    }
                    SavedBottleRenewalRecord current = repository.findSavedBottleRenewalByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_RENEWAL_NOT_FOUND", "转存记录不存在"));
                    if (!"pending_review".equals(current.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_RENEWAL_NOT_PENDING", "该转存不在待复核状态");
                    }
                    String actor = currentActorId();
                    if (actor == null || actor.equals(current.operatorId())) {
                        throw ApiException.conflict("SAVED_BOTTLE_RENEWAL_SELF_REVIEW", "员工代办转存须由当班营销主管复核，经办人不得自审");
                    }
                    SavedBottleRecord bottle = lockSavedBottle(current.savedBottleId());
                    requireCustody(bottle);
                    Instant now = clock.instant();
                    if ("reject".equals(decision)) {
                        String reason = requireText(actual.reason(), "reason");
                        return repository.saveSavedBottleRenewal(new SavedBottleRenewalRecord(
                                current.id(), current.savedBottleId(), current.merchantId(), current.projectId(), current.customerId(),
                                current.phoneHash(), current.extensionDays(), current.previousExpireAt(), current.newExpireAt(),
                                current.renewalCount(), current.maxRenewals(), current.limitConfigVersion(), current.daysConfigVersion(),
                                current.operatorId(), current.operationSource(), current.confirmationMethod(), actor, now, reason,
                                "rejected", current.createdAt(), now));
                    }
                    requireRenewable(bottle);
                    Instant newExpireAt = bottle.expireAt().plus(Duration.ofDays(current.extensionDays()));
                    repository.saveSavedBottle(savedBottleWithExpireAt(bottle, newExpireAt, now));
                    return repository.saveSavedBottleRenewal(new SavedBottleRenewalRecord(
                            current.id(), current.savedBottleId(), current.merchantId(), current.projectId(), current.customerId(),
                            current.phoneHash(), current.extensionDays(), bottle.expireAt(), newExpireAt, current.renewalCount(),
                            current.maxRenewals(), current.limitConfigVersion(), current.daysConfigVersion(), current.operatorId(),
                            current.operationSource(), current.confirmationMethod(), actor, now, null, "effective", current.createdAt(), now));
                });
    }

    /** 该瓶转存流水（含待复核），供后台展示与营销主管复核入口。 */
    public PageResponse<SavedBottleRenewalRecord> listSavedBottleRenewals(String savedBottleId) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return readPage(
                "bar.saved_bottle.renewal_viewed",
                "bar.saved_bottle.renewal_view_failed",
                "bar.saved_bottle.manage",
                "saved_bottle_renewal",
                () -> {
                    requireOwnMerchant(lockSavedBottle(id));
                    return repository.listSavedBottleRenewals(id);
                });
    }

    /** 客人端本人转存：持有人本人 + 本人短信验证码，验证成功即从当前到期日顺延（无需复核）。 */
    @Transactional
    public SavedBottleRenewalRecord renewSavedBottleByCustomer(
            String savedBottleId, String customerId, String customerPhone, CustomerSavedBottleRenewRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        String customer = requireText(customerId, "customer_id");
        return executeWrite(
                "renewSavedBottleByCustomer:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRenewalRecord.class,
                "bar.saved_bottle.renewed",
                "bar.saved_bottle.renew_failed",
                "not_applicable",
                "saved_bottle_renewal",
                () -> {
                    CustomerSavedBottleRenewRequest actual = requireRequest(request);
                    String code = requireText(actual.smsCode(), "sms_code");
                    SavedBottleRecord bottle = lockSavedBottle(id);
                    requireCurrentHolder(bottle, customer, customerPhone);
                    RenewalTerms terms = renewalTerms(bottle);
                    String holderPhone = verifyHolderSms(bottle, code, RENEWAL_SCENE);
                    Instant now = clock.instant();
                    repository.saveSavedBottle(savedBottleWithExpireAt(bottle, terms.newExpireAt(), now));
                    return repository.saveSavedBottleRenewal(newRenewal("rnw_" + slug(actual.idempotencyKey()), bottle, terms,
                            phoneHash(holderPhone), customer, "customer_self", "effective", now));
                });
    }

    /** 客人端本人转存发码：只对当前持有人本人发到本人手机（scene=renew），按手机号限速。 */
    @Transactional
    public SavedBottleVerificationCodeReceipt requestSavedBottleRenewalCodeByCustomer(
            String savedBottleId, String customerId, String customerPhone) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        try {
            SavedBottleRecord bottle = lockSavedBottle(id);
            requireCurrentHolder(bottle, requireText(customerId, "customer_id"), customerPhone);
            requireRenewable(bottle);
            String holderPhone = requireHolderPhone(bottle);
            if (otpVerifier == null) {
                throw ApiException.conflict("CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信服务未配置，请联系管理员");
            }
            enforceOtpRateLimit(holderPhone);
            otpVerifier.requestCode(holderPhone, OTP_SCENE_PREFIX + RENEWAL_SCENE);
            auditEventService.recordSuccess("bar.saved_bottle.verification_code_requested", "not_applicable", "saved_bottle",
                    bottle.id(), null, null, Map.of("scene", RENEWAL_SCENE, "customer_id", customerId));
            return new SavedBottleVerificationCodeReceipt(bottle.id(), RENEWAL_SCENE, clock.instant());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("bar.saved_bottle.verification_code_request_failed", "not_applicable", "saved_bottle",
                    id, null, null, codeOf(exception));
            throw exception;
        }
    }

    private record RenewalTerms(String projectId, SavedBottleRenewalPolicyRecord limit, SavedBottleRenewalPolicyRecord days,
                                int count, Instant newExpireAt) {
    }

    /** 转存前置：stored 且在当前到期日前；读项目上限/酒吧顺延期限（缺行回落 global）；无待复核；累计次数（不含被驳回的代办）未达上限。 */
    private RenewalTerms renewalTerms(SavedBottleRecord bottle) {
        requireRenewable(bottle);
        String projectId = repository.findBarProjectId(bottle.barProjectId()).orElse(null);
        SavedBottleRenewalPolicyRecord limit = renewalPolicy(RENEWAL_POLICY_LIMIT, projectId == null ? null : "project:" + projectId);
        SavedBottleRenewalPolicyRecord days = renewalPolicy(RENEWAL_POLICY_DAYS, "bar:" + bottle.barProjectId());
        List<SavedBottleRenewalRecord> history = repository.listSavedBottleRenewals(bottle.id()).stream()
                .filter(r -> !"rejected".equals(r.status())).toList();
        if (history.stream().anyMatch(r -> "pending_review".equals(r.status()))) {
            throw ApiException.conflict("SAVED_BOTTLE_RENEWAL_PENDING", "该存酒已有一笔员工代办转存待营销主管复核");
        }
        if (history.size() >= limit.numericValue()) {
            throw ApiException.conflict("SAVED_BOTTLE_RENEWAL_LIMIT_REACHED",
                    "转存次数已达上限（" + limit.numericValue() + " 次），不再受理转存");
        }
        return new RenewalTerms(projectId, limit, days, history.size() + 1,
                bottle.expireAt().plus(Duration.ofDays(days.numericValue())));
    }

    // ===== 业务参数配置：BSM L1073 人工例外消费历史条件（按项目，最高权限审批并留痕）/ ENUM L120 转存次数上限（按项目）与顺延期限（按酒吧） =====

    static final String MANUAL_PICKUP_POLICY_MANAGE = "bar.saved_bottle.manual_pickup_policy.manage";
    static final String RENEWAL_POLICY_MANAGE = "bar.saved_bottle.renewal_policy.manage";
    static final String REMINDER_POLICY_MANAGE = "bar.saved_bottle.reminder_policy.manage";
    /** 人工例外消费历史条件的出厂行（V548：BSM L1073 出厂默认 2 / 3 / 5 笔）。 */
    private static final String MANUAL_PICKUP_POLICY_FACTORY = "global";

    /**
     * 配置存酒到期提醒节点（BSM L1072「到期前按后台配置的提醒节点各发一次短信提醒（业务参数，按项目可配，出厂默认到期前第 14 天、
     * 第 7 天、第 3 天、第 1 天）」）：scope_id=global（出厂行）| project:&lt;id&gt;；节点为到期前第 N 天（N≥1、不重复、至少一个），
     * 超出单瓶实际有效期的节点由发送作业逐瓶跳过。项目级参数：商户账号不可改，非平台管理员只能改本项目（范围闸在幂等之前）；
     * 留痕 updated_by/updated_at 与审计事件。
     */
    @Transactional
    public SavedBottleReminderPolicyRecord configureReminderPolicy(
            com.complex.platform.inventory.dto.SavedBottleReminderPolicyRequest request) {
        authorizationService.requirePlatformScope();
        Optional<String> ownProject = authorizationService.currentProjectScopeQuietly();
        if (ownProject.isPresent() && !("project:" + ownProject.get()).equals(request == null ? null : trimToNull(request.scopeId()))) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能配置本项目的存酒到期提醒节点");
        }
        return executeWrite("configureSavedBottleReminderPolicy", request == null ? null : request.idempotencyKey(), request,
                SavedBottleReminderPolicyRecord.class, "bar.saved_bottle.reminder_policy_configured",
                "bar.saved_bottle.reminder_policy_config_failed", REMINDER_POLICY_MANAGE, "saved_bottle_reminder_policy", () -> {
                    com.complex.platform.inventory.dto.SavedBottleReminderPolicyRequest actual = requireRequest(request);
                    String scope = requireText(actual.scopeId(), "scope_id");
                    if (!"global".equals(scope) && !(scope.startsWith("project:") && scope.length() > "project:".length())) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "scope_id 须为 global 或 project:<项目 id>");
                    }
                    List<Integer> days = actual.reminderDays();
                    if (days == null || days.isEmpty() || days.stream().anyMatch(d -> d == null || d < 1)
                            || Set.copyOf(days).size() != days.size()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "提醒节点须为到期前第 N 天（N 为不小于 1 的整数、不重复），至少一个");
                    }
                    return repository.saveSavedBottleReminderPolicy(new SavedBottleReminderPolicyRecord(scope,
                            days.stream().sorted(java.util.Comparator.reverseOrder()).toList()), currentActorId());
                });
    }

    /**
     * 读存酒到期提醒节点（BSM L1072「按后台配置的提醒节点」）：出厂行（global）与各项目行。商户账号不可读；
     * 非平台管理员只看出厂行与本项目，没有项目归属则拒绝（范围闸在读之前，fail-closed）。
     */
    public PageResponse<SavedBottleReminderPolicyRecord> listReminderPolicies() {
        return readPage("bar.saved_bottle.reminder_policy_viewed", "bar.saved_bottle.reminder_policy_view_failed",
                REMINDER_POLICY_MANAGE, "saved_bottle_reminder_policy", () -> {
                    authorizationService.requirePlatformScope();
                    Optional<String> ownProject = authorizationService.currentProjectScopeQuietly();
                    return repository.listSavedBottleReminderPolicies().entrySet().stream()
                            .filter(e -> ownProject.isEmpty() || "global".equals(e.getKey())
                                    || ("project:" + ownProject.get()).equals(e.getKey()))
                            .map(e -> new SavedBottleReminderPolicyRecord(e.getKey(), e.getValue()))
                            .sorted(java.util.Comparator.comparing(SavedBottleReminderPolicyRecord::scopeId))
                            .toList();
                });
    }

    /**
     * 配置人工例外取酒消费历史条件（BSM L1073「安全阈值，按项目可配，调整须最高权限审批并留痕……须同时配置上下限」）：
     * 三个条件值与上下限须同时填写且各条件值落在上下限内；按仓内最高权限审批范式（FacilityEquipmentService.approveDelete，
     * BSM 第 577 行同款）由最高权限本人办理，否则 403 MASTER_APPROVAL_REQUIRED；留痕 updated_by/updated_at 与审计事件。
     */
    @Transactional
    public SavedBottleManualPickupPolicyRecord configureManualPickupPolicy(
            com.complex.platform.inventory.dto.SavedBottleManualPickupPolicyRequest request) {
        return executeWrite("configureSavedBottleManualPickupPolicy", request == null ? null : request.idempotencyKey(), request,
                SavedBottleManualPickupPolicyRecord.class, "bar.saved_bottle.manual_pickup_policy_configured",
                "bar.saved_bottle.manual_pickup_policy_config_failed", MANUAL_PICKUP_POLICY_MANAGE,
                "saved_bottle_manual_pickup_policy", () -> {
                    com.complex.platform.inventory.dto.SavedBottleManualPickupPolicyRequest actual = requireRequest(request);
                    if (!authorizationService.isMasterAuthority(authorizationService.currentActor())) {
                        throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "人工例外取酒消费历史条件的调整须由最高权限本人审批");
                    }
                    String projectId = requireText(actual.projectId(), "project_id");
                    Integer m1 = actual.ordersWithin1m();
                    Integer m3 = actual.ordersWithin3m();
                    Integer total = actual.ordersTotal();
                    Integer min = actual.minOrders();
                    Integer max = actual.maxOrders();
                    if (m1 == null || m3 == null || total == null || min == null || max == null || min < 1 || min > max
                            || m1 < min || m1 > max || m3 < min || m3 > max || total < min || total > max) {
                        throw ApiException.badRequest("VALIDATION_FAILED",
                                "消费历史条件须同时填写三个条件值与上下限，下限不低于 1，各条件值须在上下限内");
                    }
                    SavedBottleManualPickupPolicyRecord policy =
                            new SavedBottleManualPickupPolicyRecord(projectId, m1, m3, total, min, max);
                    repository.saveSavedBottleManualPickupPolicy(policy, currentActorId());
                    return policy;
                });
    }

    /**
     * 配置转存业务参数（ENUM L120）：max_renewals 按项目（scope_id=global|project:&lt;id&gt;），extension_days 按酒吧
     * （scope_id=global|bar:&lt;id&gt;）；值与上下限须同时填写、值在上下限内（顺延期限至少 1 天）；每次保存 version+1、留痕 updated_by。
     * 带商户 scope 的操作者只能配置本酒吧的顺延期限。
     */
    @Transactional
    public SavedBottleRenewalPolicyRecord configureRenewalPolicy(com.complex.platform.inventory.dto.SavedBottleRenewalPolicyRequest request) {
        return executeWrite("configureSavedBottleRenewalPolicy", request == null ? null : request.idempotencyKey(), request,
                SavedBottleRenewalPolicyRecord.class, "bar.saved_bottle.renewal_policy_configured",
                "bar.saved_bottle.renewal_policy_config_failed", RENEWAL_POLICY_MANAGE, "saved_bottle_renewal_policy", () -> {
                    com.complex.platform.inventory.dto.SavedBottleRenewalPolicyRequest actual = requireRequest(request);
                    String key = requireText(actual.policyKey(), "policy_key");
                    String scope = requireText(actual.scopeId(), "scope_id");
                    boolean keyOk = RENEWAL_POLICY_LIMIT.equals(key) || RENEWAL_POLICY_DAYS.equals(key);
                    boolean scopeOk = "global".equals(scope)
                            || (RENEWAL_POLICY_LIMIT.equals(key) && scope.startsWith("project:"))
                            || (RENEWAL_POLICY_DAYS.equals(key) && scope.startsWith("bar:"));
                    if (!keyOk || !scopeOk) {
                        throw ApiException.badRequest("VALIDATION_FAILED",
                                "policy_key 须为 max_renewals（scope_id=global|project:<id>）或 extension_days（scope_id=global|bar:<id>）");
                    }
                    Optional<String> merchantScope = authorizationService.currentMerchantScope();
                    if (merchantScope.isPresent() && !scope.equals("bar:" + merchantScope.get())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "只能配置本酒吧的转存顺延期限");
                    }
                    Integer value = actual.value();
                    Integer min = actual.minValue();
                    Integer max = actual.maxValue();
                    int floor = RENEWAL_POLICY_DAYS.equals(key) ? 1 : 0;
                    if (value == null || min == null || max == null || min < floor || value < min || value > max) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "转存业务参数须同时填写参数值及上下限，参数值须在上下限内");
                    }
                    return repository.saveSavedBottleRenewalPolicy(
                            new SavedBottleRenewalPolicyRecord(key, scope, value, min, max, 0), currentActorId());
                });
    }

    private SavedBottleRenewalPolicyRecord renewalPolicy(String key, String scopeId) {
        Optional<SavedBottleRenewalPolicyRecord> scoped = scopeId == null
                ? Optional.empty() : repository.findSavedBottleRenewalPolicy(key, scopeId);
        return scoped.or(() -> repository.findSavedBottleRenewalPolicy(key, "global"))
                .orElseThrow(() -> ApiException.conflict("SAVED_BOTTLE_RENEWAL_POLICY_UNSET", "转存业务参数未配置：" + key));
    }

    /** 只能在当前到期日前办理（stored 且未过期；未登记到期日的不可转存）。 */
    private void requireRenewable(SavedBottleRecord bottle) {
        requireClaimable(bottle);
        if (bottle.expireAt() == null) {
            throw ApiException.conflict("SAVED_BOTTLE_EXPIRE_AT_MISSING", "该存酒未登记到期日，不可转存");
        }
    }

    private SavedBottleRenewalRecord newRenewal(String id, SavedBottleRecord bottle, RenewalTerms terms, String phoneHash,
                                                String operatorId, String operationSource, String status, Instant now) {
        return new SavedBottleRenewalRecord(id, bottle.id(), bottle.merchantId(), terms.projectId(), bottle.customerId(), phoneHash,
                terms.days().numericValue(), bottle.expireAt(), terms.newExpireAt(), terms.count(), terms.limit().numericValue(),
                terms.limit().configVersion(), terms.days().configVersion(), operatorId, operationSource, "sms", null, null, null,
                status, now, now);
    }

    private SavedBottleRecord lockSavedBottle(String id) {
        return repository.findSavedBottleByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
    }

    private void requireCurrentHolder(SavedBottleRecord bottle, String customerId, String customerPhone) {
        if (!isHolder(bottle, customerId, customerPhone)) {
            throw ApiException.forbidden("SAVED_BOTTLE_NOT_HOLDER", "该存酒当前不属于本人");
        }
    }

    /** 重建存酒记录，仅改到期日/updatedAt（转存顺延）。 */
    private SavedBottleRecord savedBottleWithExpireAt(SavedBottleRecord b, Instant expireAt, Instant updatedAt) {
        return new SavedBottleRecord(b.id(), b.customerId(), b.barProjectId(), b.itemId(), b.bottleName(),
                b.initialQuantity(), b.remainingQuantity(), b.unit(), expireAt, b.status(), b.evidenceAttachmentIds(),
                b.createdAt(), updatedAt, b.merchantId(), b.isHighValue(), b.highValueThresholdCents(),
                b.bottleLabelValue(), b.customerNameOrNickname(), b.customerPhone(), b.customerPhoneHash(),
                b.registrationPhotoRefs(), b.manualNote(),
                b.transferredAt(), b.transferToNameOrNickname(), b.transferToPhone(), b.transferToPhoneHash(),
                b.remedyOfSavedBottleId(), b.custodianEntityId(), b.pickupLocation(), b.handedOverAt(), b.handoverId());
    }

    // ===== FS-016 人工例外取酒（BSM L1073 四项，线性：店长发起 → 另一主管复核 → 营销确认 → 本人到场放酒） =====

    /**
     * ① 店长发起：客户手机号须在本项目内满足后台配置的任一消费历史条件（条件未配置即拒）；这段消费历史查询走客人查询专门通道
     * （发起店长本人鉴权 customer.account.manage，留痕谁查、查哪个手机号、何时、为哪笔申请查）。
     */
    @Transactional
    public SavedBottleManualPickupRecord initiateManualPickup(String savedBottleId, SavedBottleManualPickupRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "initiateSavedBottleManualPickup:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleManualPickupRecord.class,
                "bar.saved_bottle.manual_pickup_initiated",
                "bar.saved_bottle.manual_pickup_initiate_failed",
                MANUAL_PICKUP_INITIATE,
                "saved_bottle_manual_pickup",
                () -> {
                    SavedBottleManualPickupRequest actual = requireRequest(request);
                    String reason = requireText(actual.reason(), "reason");
                    SavedBottleRecord bottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    requireCustody(bottle);
                    requireClaimable(bottle);
                    String holderPhone = requireHolderPhone(bottle);
                    String projectId = repository.findBarProjectId(bottle.barProjectId())
                            .orElseThrow(() -> ApiException.conflict("SAVED_BOTTLE_PROJECT_UNKNOWN", "存酒所在酒吧未归属项目，无法核对消费历史"));
                    SavedBottleManualPickupPolicyRecord policy = repository.findSavedBottleManualPickupPolicy(projectId)
                            .or(() -> repository.findSavedBottleManualPickupPolicy(MANUAL_PICKUP_POLICY_FACTORY))
                            .orElseThrow(() -> ApiException.conflict("SAVED_BOTTLE_MANUAL_PICKUP_POLICY_UNSET",
                                    "本项目尚未配置人工例外取酒的消费历史条件"));
                    // 客人查询专门通道：店长本人鉴权后按手机号查客人，留痕。
                    authorizationService.requirePermission("customer.account.manage");
                    String requestId = "mpk_" + slug(actual.idempotencyKey());
                    String customerId = trimToNull(bottle.customerId()) != null ? bottle.customerId() : findCustomerIdByPhone(holderPhone);
                    String actor = currentActorId();
                    Map<String, Object> lookup = new LinkedHashMap<>();
                    lookup.put("purpose", "saved_bottle_manual_pickup");
                    lookup.put("manual_pickup_id", requestId);
                    lookup.put("phone_hash", phoneHash(holderPhone));
                    lookup.put("queried_by", actor == null ? "unknown" : actor);
                    lookup.put("matched", customerId != null);
                    auditEventService.recordSuccess("customer.account.viewed", "customer.account.manage", "customer",
                            customerId == null ? "not_found" : customerId, requestId, actual.idempotencyKey(), lookup);
                    Instant now = clock.instant();
                    String condition = customerId == null ? null : metHistoryCondition(customerId, projectId, policy, now);
                    if (condition == null) {
                        throw ApiException.conflict("SAVED_BOTTLE_MANUAL_PICKUP_HISTORY_NOT_MET",
                                "该手机号在本项目内不满足任一消费历史条件，不得人工例外取酒");
                    }
                    return repository.saveSavedBottleManualPickup(new SavedBottleManualPickupRecord(
                            requestId, bottle.id(), bottle.merchantId(), projectId, customerId, phoneHash(holderPhone),
                            reason, condition, actor, now, null, null, null, null, null, null, null, null,
                            "initiated", safeList(actual.evidenceAttachmentIds()), now, now));
                });
    }

    /** ② 另一名主管复核：复核人不得是发起人（普通员工不得单独放酒）。 */
    @Transactional
    public SavedBottleManualPickupRecord reviewManualPickup(String manualPickupId, SavedBottleManualPickupRequest request) {
        String id = requireText(manualPickupId, "manual_pickup_id");
        return executeWrite(
                "reviewSavedBottleManualPickup:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleManualPickupRecord.class,
                "bar.saved_bottle.manual_pickup_reviewed",
                "bar.saved_bottle.manual_pickup_review_failed",
                MANUAL_PICKUP_REVIEW,
                "saved_bottle_manual_pickup",
                () -> {
                    requireRequest(request);
                    SavedBottleManualPickupRecord current = requireManualPickup(id, "initiated");
                    String actor = currentActorId();
                    if (actor != null && actor.equals(current.initiatedBy())) {
                        throw ApiException.conflict("SAVED_BOTTLE_MANUAL_PICKUP_SELF_REVIEW", "人工例外取酒须由另一名主管复核");
                    }
                    Instant now = clock.instant();
                    return repository.saveSavedBottleManualPickup(new SavedBottleManualPickupRecord(
                            current.id(), current.savedBottleId(), current.merchantId(), current.projectId(), current.customerId(),
                            current.phoneHash(), current.reason(), current.historyCondition(), current.initiatedBy(), current.initiatedAt(),
                            actor, now, null, null, null, null, null, null, "reviewed", current.evidenceAttachmentIds(),
                            current.createdAt(), now));
                });
    }

    /**
     * ③ 营销确认（BSM L1073）：客户在本项目有在职归属营销的，须由该营销本人确认（attributed_marketer）；营销已离职的由其主管确认
     * ——取该营销的直接上级（org_hierarchy），要求确认人即该上级（marketer_supervisor），未维护上级即「找不到可承担确认责任的营销主管，
     * 流程不得通过」；历史订单无归属营销的由存酒所在酒吧当班营销主管确认（on_duty_marketing_supervisor：持营销确认权限码且商户范围
     * 为存酒所在酒吧，见 requireManualPickup）。
     */
    @Transactional
    public SavedBottleManualPickupRecord confirmManualPickupMarketing(String manualPickupId, SavedBottleManualPickupRequest request) {
        String id = requireText(manualPickupId, "manual_pickup_id");
        return executeWrite(
                "confirmSavedBottleManualPickupMarketing:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleManualPickupRecord.class,
                "bar.saved_bottle.manual_pickup_marketing_confirmed",
                "bar.saved_bottle.manual_pickup_marketing_confirm_failed",
                MANUAL_PICKUP_CONFIRM_MARKETING,
                "saved_bottle_manual_pickup",
                () -> {
                    requireRequest(request);
                    SavedBottleManualPickupRecord current = requireManualPickup(id, "reviewed");
                    String actor = currentActorId();
                    Optional<InventoryRepository.AttributedMarketer> marketer = current.customerId() == null
                            ? Optional.empty()
                            : repository.findAttributedMarketer(current.customerId(), current.projectId());
                    String basis;
                    if (marketer.isPresent() && marketer.get().active()) {
                        if (actor == null || !actor.equals(marketer.get().userId())) {
                            throw ApiException.conflict("SAVED_BOTTLE_MANUAL_PICKUP_MARKETER_MISMATCH",
                                    "须由该客户对应的营销人员本人确认");
                        }
                        basis = "attributed_marketer";
                    } else if (marketer.isPresent()) {
                        String superior = repository.findDirectSuperior(marketer.get().userId())
                                .orElseThrow(() -> ApiException.conflict("SAVED_BOTTLE_MANUAL_PICKUP_SUPERVISOR_UNRESOLVED",
                                        "该客户对应的营销人员已离职且未维护直接上级，找不到可承担确认责任的营销主管，流程不得通过"));
                        if (actor == null || !actor.equals(superior)) {
                            throw ApiException.conflict("SAVED_BOTTLE_MANUAL_PICKUP_MARKETER_SUPERVISOR_MISMATCH",
                                    "营销已离职的须由其主管（直接上级）本人确认");
                        }
                        basis = "marketer_supervisor";
                    } else {
                        basis = "on_duty_marketing_supervisor";
                    }
                    Instant now = clock.instant();
                    return repository.saveSavedBottleManualPickup(new SavedBottleManualPickupRecord(
                            current.id(), current.savedBottleId(), current.merchantId(), current.projectId(), current.customerId(),
                            current.phoneHash(), current.reason(), current.historyCondition(), current.initiatedBy(), current.initiatedAt(),
                            current.reviewedBy(), current.reviewedAt(), actor, basis, now, null, null, null, "marketing_confirmed",
                            current.evidenceAttachmentIds(), current.createdAt(), now));
                });
    }

    /** ④ 本人到场放酒：present_in_person 须为 true（不允许他人代领）；取酒流水 verification_method=manual_exception。 */
    @Transactional
    public SavedBottleManualPickupRecord dispenseManualPickup(String manualPickupId, SavedBottleManualPickupDispenseRequest request) {
        String id = requireText(manualPickupId, "manual_pickup_id");
        return executeWrite(
                "dispenseSavedBottleManualPickup:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleManualPickupRecord.class,
                "bar.saved_bottle_picked_up",
                "bar.saved_bottle.pickup_failed",
                "bar.saved_bottle.manage",
                "saved_bottle_manual_pickup",
                () -> {
                    SavedBottleManualPickupDispenseRequest actual = requireRequest(request);
                    if (!Boolean.TRUE.equals(actual.presentInPerson())) {
                        throw ApiException.conflict("SAVED_BOTTLE_MANUAL_PICKUP_NOT_IN_PERSON",
                                "人工例外取酒只允许存酒客户本人到场办理，不允许他人代领");
                    }
                    SavedBottleManualPickupRecord current = requireManualPickup(id, "marketing_confirmed");
                    SavedBottleRecord bottle = repository.findSavedBottleByIdForUpdate(current.savedBottleId())
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    requireCustody(bottle);
                    requireClaimable(bottle);
                    // 持有人身份已由营销确认：按登记标签/姓名出酒，核验方式记 manual_exception、被核验手机=申请时持有人手机哈希。
                    SavedBottleClaimRecord claim = dispenseSavedBottle(bottle, actual.idempotencyKey(), actual.claimItems(),
                            bottle.bottleLabelValue(), bottle.customerNameOrNickname(), actual.evidenceAttachmentIds(),
                            "manual_exception", current.phoneHash(), null);
                    Instant now = clock.instant();
                    return repository.saveSavedBottleManualPickup(new SavedBottleManualPickupRecord(
                            current.id(), current.savedBottleId(), current.merchantId(), current.projectId(), current.customerId(),
                            current.phoneHash(), current.reason(), current.historyCondition(), current.initiatedBy(), current.initiatedAt(),
                            current.reviewedBy(), current.reviewedAt(), current.marketingConfirmedBy(), current.marketingConfirmBasis(),
                            current.marketingConfirmedAt(), currentActorId(), now, claim.id(), "dispensed",
                            current.evidenceAttachmentIds(), current.createdAt(), now));
                });
    }

    // ===== FS-016 helpers =====

    private SavedBottleManualPickupRecord requireManualPickup(String id, String expectedStatus) {
        SavedBottleManualPickupRecord current = repository.findSavedBottleManualPickupByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_MANUAL_PICKUP_NOT_FOUND", "人工例外取酒申请不存在"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(current.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该人工例外取酒申请不属于当前商户");
        }
        if (!expectedStatus.equals(current.status())) {
            throw ApiException.conflict("SAVED_BOTTLE_MANUAL_PICKUP_NOT_" + expectedStatus.toUpperCase(Locale.ROOT),
                    "人工例外取酒申请当前状态不允许本步骤");
        }
        return current;
    }

    /** 任一满足即可，按 BSM L1073 原句顺序；都不满足返回 null。 */
    private String metHistoryCondition(String customerId, String projectId, SavedBottleManualPickupPolicyRecord policy, Instant now) {
        if (repository.countValidTableOrders(customerId, projectId, now.minus(Duration.ofDays(30))) >= policy.ordersWithin1m()) {
            return "orders_within_1m";
        }
        if (repository.countValidTableOrders(customerId, projectId, now.minus(Duration.ofDays(90))) >= policy.ordersWithin3m()) {
            return "orders_within_3m";
        }
        if (repository.countValidTableOrders(customerId, projectId, null) >= policy.ordersTotal()) {
            return "orders_total";
        }
        return null;
    }

    private void requireOwnMerchant(SavedBottleRecord bottle) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(bottle.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该存酒记录不属于当前商户");
        }
    }

    private void requireClaimable(SavedBottleRecord bottle) {
        if (!"stored".equals(bottle.status())) {
            throw ApiException.conflict("SAVED_BOTTLE_NOT_CLAIMABLE", "该存酒当前不可取用");
        }
        if (bottle.expireAt() != null && bottle.expireAt().isBefore(clock.instant())) {
            throw ApiException.conflict("SAVED_BOTTLE_EXPIRED", "该存酒已过期");
        }
    }

    private String requireHolderPhone(SavedBottleRecord bottle) {
        String holderPhone = resolveSavedBottleRecipient(bottle);
        if (holderPhone == null) {
            throw ApiException.conflict("SAVED_BOTTLE_HOLDER_PHONE_MISSING", "该存酒无当前持有人手机号，无法核验");
        }
        return holderPhone;
    }

    /** 当前持有人短信核验（fail-closed：无短信端口即拒）；返回被核验的持有人手机号。 */
    private String verifyHolderSms(SavedBottleRecord bottle, String code, String scene) {
        String holderPhone = requireHolderPhone(bottle);
        if (otpVerifier == null) {
            throw ApiException.unauthorized("CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信服务未配置，请联系管理员");
        }
        enforceOtpRateLimit(holderPhone);
        otpVerifier.verify(holderPhone, code, OTP_SCENE_PREFIX + scene);
        return holderPhone;
    }

    private SavedBottlePickupCodeRecord requireActivePickupCode(SavedBottleRecord bottle, String code) {
        String hash = sha256Hex(code);
        return repository.findActiveSavedBottlePickupCode(bottle.id())
                .filter(active -> hash.equals(active.codeHash()))
                .filter(active -> active.expiresAt().isAfter(clock.instant()))
                .orElseThrow(() -> ApiException.conflict("SAVED_BOTTLE_PICKUP_CODE_INVALID", "动态取酒码无效或已过期，请客人刷新后重新出示"));
    }

    private void consumePickupCode(SavedBottlePickupCodeRecord code, String claimId) {
        if (code != null) {
            repository.saveSavedBottlePickupCode(code.withStatus("consumed", claimId));
        }
    }

    private boolean isHolder(SavedBottleRecord bottle, String customerId, String customerPhone) {
        if (customerId.equals(bottle.customerId())) {
            return true;
        }
        String hash = phoneHash(customerPhone);
        return hash != null && hash.equals(bottle.customerPhoneHash());
    }

    private String findCustomerIdByPhone(String phone) {
        if (customerRepository == null || phone == null) {
            return null;
        }
        try {
            return customerRepository.findCustomerByPhone(phone).map(CustomerRecord::id).orElse(null);
        } catch (RuntimeException exception) {
            log.warn("failed to look up customer by phone for saved bottle", exception);
            return null;
        }
    }

    private void enforceOtpRateLimit(String phone) {
        if (rateLimiter == null || rateLimitProperties == null || !rateLimitProperties.isEnabled()) {
            return;
        }
        com.complex.platform.common.ratelimit.RateLimitProperties.Rule rule = rateLimitProperties.getOtpPhone();
        com.complex.platform.common.ratelimit.RateLimiter.Decision decision =
                rateLimiter.tryAcquire("otp:phone:" + phone, rule.getLimit(), rule.getWindow());
        if (!decision.allowed()) {
            throw ApiException.tooManyRequests("CUSTOMER_OTP_RATE_LIMITED", "验证尝试次数过多，请稍后再试",
                    decision.retryAfterSeconds());
        }
    }

    private String currentActorId() {
        com.complex.platform.access.AccessActor actor = authorizationService.currentActor();
        return actor == null ? null : actor.id();
    }

    /** 操作来源：设备凭证派生的可信终端类型，无请求上下文/未自报时记 web。 */
    private static String operationSource() {
        String terminal = com.complex.platform.gate.TerminalIdentityHolder.trustedTerminalType(null);
        return terminal == null || terminal.isBlank() || "*".equals(terminal) ? "web" : terminal;
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /**
     * A1/REQ-32 + R5 T6：存酒作废（库存域内高价值复核闭环）。
     *
     * <p><b>高价值</b>存酒作废<b>不直接生效</b>：要求<b>照片 + 视频双证据</b>（{@code photoAttachmentIds} 与
     * {@code videoAttachmentIds} 各非空，真正区分类型，替换旧"附件数≥2 不分类型"弱校验）；命中后建库存域内高价值
     * 复核单（{@code inventory_high_value_reviews}，pending，subject_type=saved_bottle_void、subject_id=存酒 id），
     * 并把存酒置中间态 {@code disputed_frozen}（待主管复核冻结，不直接 cancelled）。主管经
     * {@link InventoryHighValueReviewService#review} approve 后存酒才真正置 {@code cancelled}；reject 则维持
     * {@code disputed_frozen}（不作废）。
     *
     * <p><b>非高价值</b>作废维持<b>直接 cancel</b>（向后兼容，无证据要求）。
     *
     * <p>中间态选用 {@code disputed_frozen}（V061 既有状态："争议冻结/待人工复核"语义，已在可作废源态集内），
     * 因本期无新迁移、{@code saved_bottles.status} CHECK 不含 pending_review。
     */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage", admissionAction = "inventory.saved_bottle_cancel", objectType = "saved_bottle")
    @Transactional
    public SavedBottleRecord cancelSavedBottle(String savedBottleId, SavedBottleCancelRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "cancelSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRecord.class,
                "bar.saved_bottle.cancelled",
                "bar.saved_bottle.cancel_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    SavedBottleCancelRequest actual = requireRequest(request);
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(savedBottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "该存酒记录不属于当前商户");
                    }
                    if (!List.of("stored", "pending_identity_review").contains(savedBottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_CANCELLABLE", "该存酒当前不可作废");
                    }
                    return applyCancel(savedBottle, actual.idempotencyKey(), actual.photoAttachmentIds(), actual.videoAttachmentIds());
                }
        );
    }

    /**
     * 作废收口（cancelSavedBottle 与 FS-017 交接毁损/短少共用）。V350 归一：作废当刻**重新判定**（不看登记快照，也不看前端传值）——
     * 阈值未配置 / 进货价未知 / 手工标记 / 进货价高于阈值，四者任一即高价值，一律拦下走复核：双证据（照片+视频各非空）→ 建 pending
     * 复核单 + 存酒置中间态 disputed_frozen（不直接作废）；非高价值直接 cancel（向后兼容）。
     */
    private SavedBottleRecord applyCancel(SavedBottleRecord savedBottle, String idempotencySeed,
                                          List<String> photoAttachmentIds, List<String> videoAttachmentIds) {
        if (highValueDecisionService.decide(savedBottle.itemId(), savedBottle.merchantId()).highValue()) {
            highValueReviewService.createPending(
                    idempotencySeed,
                    InventoryHighValueReviewService.SUBJECT_SAVED_BOTTLE_VOID,
                    savedBottle.id(),
                    photoAttachmentIds,
                    videoAttachmentIds);
            repository.revokeActiveSavedBottlePickupCodes(savedBottle.id());
            return repository.saveSavedBottle(
                    savedBottleWith(savedBottle, "disputed_frozen", savedBottle.remainingQuantity(), Instant.now()));
        }
        repository.revokeActiveSavedBottlePickupCodes(savedBottle.id());
        return repository.saveSavedBottle(
                savedBottleWith(savedBottle, "cancelled", savedBottle.remainingQuantity(), Instant.now()));
    }

    /**
     * P0#4 争议受理（R6-D05）：将 stored/pending_identity_review 存酒置 {@code disputed} 态，进争议处理流程
     * （区别于高价值作废的 disputed_frozen）。受理后停止取酒/转赠，待线下核实后由作废/复核流程收口。
     */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage", admissionAction = "inventory.saved_bottle_dispute", objectType = "saved_bottle")
    @Transactional
    public SavedBottleRecord disputeSavedBottle(String savedBottleId, SavedBottleCancelRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "disputeSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRecord.class,
                "bar.saved_bottle.disputed",
                "bar.saved_bottle.dispute_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    SavedBottleCancelRequest actual = requireRequest(request);
                    requireText(actual.reason(), "reason");
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(savedBottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "该存酒记录不属于当前商户");
                    }
                    if (!List.of("stored", "pending_identity_review").contains(savedBottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_DISPUTABLE", "该存酒当前不可发起异议");
                    }
                    repository.revokeActiveSavedBottlePickupCodes(savedBottle.id());
                    return repository.saveSavedBottle(savedBottleWith(
                            savedBottle, "disputed", savedBottle.remainingQuantity(), Instant.now()));
                }
        );
    }

    // ===== FS-017 存酒作废后对客补存/赔付、门店退出交接、责任归属与内部追收（BSM L1072 ①②③④） =====

    /**
     * 作废后对客补存/赔付登记（BSM L1072 ②「存酒作废的，由该酒吧为客人以同品、同等剩余量补存，到期日不变，补酒成本计入该酒吧的经营损失」
     * ③「无法交还的，优先以同品、同等剩余量替代；无法替代的，双方确认赔付方案……实物或赔款实际交付后才结案」）。
     * replacement：按原瓶复制新建存酒（同品、同等剩余量、到期日不变、持有人不变）、从该酒吧库存同品同量出库、补酒成本按内部结算价×数量
     * 记经营损失（解析不到不阻断、留财务补计），登记即实物已交付存酒区 → closed；compensation：金额必填，待门店主管+财务审批。
     * 承担方：未交平台或接收时已毁损/短少 → 原门店；接收后毁损 → 娱乐平台公司（④）。
     */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage")
    @Transactional
    public SavedBottleRemedyRecord registerSavedBottleRemedy(String savedBottleId, SavedBottleRemedyRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "registerSavedBottleRemedy:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRemedyRecord.class,
                "bar.saved_bottle.remedy_registered",
                "bar.saved_bottle.remedy_register_failed",
                "bar.saved_bottle.manage",
                "saved_bottle_remedy",
                () -> {
                    SavedBottleRemedyRequest actual = requireRequest(request);
                    String type = requireText(actual.remedyType(), "remedy_type");
                    if (!"replacement".equals(type) && !"compensation".equals(type)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "remedy_type 须为 replacement（同品同量补存）或 compensation（赔付）");
                    }
                    SavedBottleRecord bottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    requireCustody(bottle);
                    if (!"cancelled".equals(bottle.status()) && handoverShortfall(bottle) == 0) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_CANCELLED", "存酒须先作废（高价值经主管复核通过）或交平台时登记短少后才能登记补存/赔付");
                    }
                    boolean open = repository.listSavedBottleRemedies(bottle.id()).stream()
                            .anyMatch(r -> !"closed".equals(r.customerDeliveryStatus()));
                    if (open) {
                        throw ApiException.conflict("SAVED_BOTTLE_REMEDY_EXISTS", "该存酒已有未结案的补存/赔付记录");
                    }
                    Instant now = clock.instant();
                    String remedyId = "sbr_" + slug(actual.idempotencyKey());
                    String projectId = repository.findBarProjectId(bottle.merchantId()).orElse(null);
                    String[] bearer = remedyBearer(bottle);
                    String actor = currentActorId();
                    String reason = trimToNull(actual.reason());
                    if ("compensation".equals(type)) {
                        if (actual.amountCent() == null || actual.amountCent() <= 0) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent 须大于 0");
                        }
                        return repository.saveSavedBottleRemedy(new SavedBottleRemedyRecord(remedyId, bottle.id(),
                                bottle.merchantId(), projectId, type, null, actual.amountCent(), null, null, bearer[0], bearer[1],
                                null, null, null, null, "pending_approval", null, List.of(), reason, "none", null, null, "none",
                                null, null, null, List.of(), actor, now, now));
                    }
                    // 同品、同等剩余量补存：新瓶照 createSavedBottle 口径登记（新标签值 + 登记照片必填），持有人/到期日复制原瓶。
                    List<String> photos = safeList(actual.registrationPhotoRefs());
                    if (photos.isEmpty()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "registration_photo_refs is required");
                    }
                    String label = requireText(actual.bottleLabelValue(), "bottle_label_value");
                    String locationId = requireText(actual.locationId(), "location_id");
                    int qty = remedyQuantity(bottle);
                    // 存酒是客户资产、不算可售库存（BSM L1198）：补存瓶从该酒吧库存出库同品同量（outbound 流水，reference=remedy）。
                    if (repository.atomicDecreaseStockBalance(bottle.itemId(), locationId, qty,
                            authorizationService.currentMerchantScope().orElse(null)) == 0) {
                        throw ApiException.conflict("STOCK_INSUFFICIENT", "可用库存不足，无法同品同量补存");
                    }
                    consumeBatchesFefo(bottle.itemId(), locationId, qty);
                    repository.saveStockMovement(new StockMovementRecord(
                            "movement_" + slug(remedyId + ":outbound"), "outbound", bottle.itemId(), locationId, null, qty,
                            null, null, "saved_bottle_remedy", remedyId, null, actor, List.of(), "completed", now,
                            bottle.merchantId()), authorizationService.currentProjectScopeQuietly().orElse(null));
                    HighValueDecisionService.HighValueDecision decision =
                            highValueDecisionService.decide(bottle.itemId(), bottle.merchantId());
                    SavedBottleRecord replacement = repository.saveSavedBottle(new SavedBottleRecord(
                            "saved_" + slug(actual.idempotencyKey() + ":replacement"), bottle.customerId(), bottle.barProjectId(),
                            bottle.itemId(), bottle.bottleName(), qty, qty, bottle.unit(), bottle.expireAt(), "stored", List.of(),
                            now, now, bottle.merchantId(), decision.highValue(), decision.thresholdCent(), label,
                            bottle.customerNameOrNickname(), bottle.customerPhone(), bottle.customerPhoneHash(), photos,
                            trimToNull(actual.operatorNote()), null, null, null, null,
                            bottle.id(), bottle.custodianEntityId(), bottle.pickupLocation(), bottle.handedOverAt(), bottle.handoverId()));
                    Long loss = settlementPriceResolver.apply(bottle).map(price -> price * qty).orElse(null);
                    String lossItemId = postRemedyLoss(remedyId, bearer, bottle.merchantId(), loss, actual.idempotencyKey());
                    notifySavedBottleAfterCommit(replacement, "deposit", STORED_WINE_DEPOSIT_TEMPLATE,
                            "swdeposit:" + replacement.id(), null);
                    return repository.saveSavedBottleRemedy(new SavedBottleRemedyRecord(remedyId, bottle.id(),
                            bottle.merchantId(), projectId, type, replacement.id(), null, loss, lossItemId, bearer[0], bearer[1],
                            null, null, null, null, "closed", now, photos, reason, "none", null, null, "none",
                            null, null, null, List.of(), actor, now, now));
                });
    }

    public PageResponse<SavedBottleRemedyRecord> listSavedBottleRemedies(String savedBottleId) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return readPage("bar.saved_bottle.remedy_viewed", "bar.saved_bottle.remedy_view_failed", "bar.saved_bottle.manage",
                "saved_bottle_remedy", () -> {
                    repository.findSavedBottleByIdForUpdate(id).ifPresent(this::requireOwnMerchant);
                    return repository.listSavedBottleRemedies(id);
                });
    }

    /** 赔付方案审批（BSM L1072 ③「经门店主管和财务审批后支付」）：role=supervisor|finance 各核一次权限码；审批人不得是登记人、两道审批不得同一人；两方齐 → approved。 */
    @Transactional
    public SavedBottleRemedyRecord approveSavedBottleRemedy(String remedyId, SavedBottleRemedyActionRequest request) {
        String id = requireText(remedyId, "remedy_id");
        return executeWrite("approveSavedBottleRemedy:" + id, request == null ? null : request.idempotencyKey(), request,
                SavedBottleRemedyRecord.class, "bar.saved_bottle.remedy_approved", "bar.saved_bottle.remedy_approve_failed",
                "bar.saved_bottle.manage", "saved_bottle_remedy", () -> {
                    SavedBottleRemedyActionRequest actual = requireRequest(request);
                    String role = requireText(actual.role(), "role");
                    if (!"supervisor".equals(role) && !"finance".equals(role)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "role 须为 supervisor（门店主管）或 finance（财务）");
                    }
                    authorizationService.requirePermission("supervisor".equals(role) ? REMEDY_SUPERVISOR_APPROVE : REMEDY_FINANCE_APPROVE);
                    SavedBottleRemedyRecord remedy = requireRemedy(id);
                    requireCustody(bottleOf(remedy));
                    if (!"pending_approval".equals(remedy.customerDeliveryStatus())) {
                        throw ApiException.conflict("SAVED_BOTTLE_REMEDY_NOT_PENDING", "该补存/赔付不在待审批状态");
                    }
                    String approver = currentActorId();
                    if (approver != null && approver.equals(remedy.createdBy())) {
                        throw ApiException.conflict("SAVED_BOTTLE_REMEDY_SELF_APPROVAL", "审批人不得是登记人");
                    }
                    String otherRoleApprover = "supervisor".equals(role) ? remedy.financeApprovedBy() : remedy.supervisorApprovedBy();
                    if (approver != null && approver.equals(otherRoleApprover)) {
                        throw ApiException.conflict("SAVED_BOTTLE_REMEDY_SAME_APPROVER", "门店主管审批与财务审批不得由同一人完成");
                    }
                    return repository.saveSavedBottleRemedy(remedy.withApproval(role, approver, clock.instant()));
                });
    }

    /** 对客结案（BSM L1072 ③「实物或赔款实际交付后才结案」）：仅 approved 可结；记交付/支付凭证与时间，赔付金额此时记经营损失。不读任何追收字段。 */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage")
    @Transactional
    public SavedBottleRemedyRecord settleSavedBottleRemedy(String remedyId, SavedBottleRemedyActionRequest request) {
        String id = requireText(remedyId, "remedy_id");
        return executeWrite("settleSavedBottleRemedy:" + id, request == null ? null : request.idempotencyKey(), request,
                SavedBottleRemedyRecord.class, "bar.saved_bottle.remedy_settled", "bar.saved_bottle.remedy_settle_failed",
                "bar.saved_bottle.manage", "saved_bottle_remedy", () -> {
                    SavedBottleRemedyActionRequest actual = requireRequest(request);
                    SavedBottleRemedyRecord remedy = requireRemedy(id);
                    requireCustody(bottleOf(remedy));
                    if (!"approved".equals(remedy.customerDeliveryStatus())) {
                        throw ApiException.conflict("SAVED_BOTTLE_REMEDY_NOT_APPROVED", "赔付方案须经门店主管和财务审批后才能支付结案");
                    }
                    Instant now = clock.instant();
                    String lossItemId = postRemedyLoss(remedy.id(), new String[] {remedy.bearerType(), remedy.bearerId()},
                            remedy.merchantId(), remedy.amountCent(), actual.idempotencyKey());
                    SavedBottleRemedyRecord closed = remedy.withDelivery(now, safeList(actual.evidenceIds()), remedy.amountCent(), lossItemId);
                    if (closed.advanceApprovedAt() != null) {
                        closed = postRecovery(closed, actual.idempotencyKey(), now);
                    }
                    return repository.saveSavedBottleRemedy(closed);
                });
    }

    /**
     * 平台垫付（BSM L1072 ④「原店应承担的替代赔付费用，仍由原门店承担，该娱乐平台公司可经审批先垫付并向原门店追收」）：
     * 仅原瓶已交平台且承担方为原门店的记录可用；平台操作者审批；对客流程照常，损失金额定下后向原门店挂追收应收。
     */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage")
    @Transactional
    public SavedBottleRemedyRecord advanceSavedBottleRemedyByPlatform(String remedyId, SavedBottleRemedyActionRequest request) {
        String id = requireText(remedyId, "remedy_id");
        return executeWrite("advanceSavedBottleRemedy:" + id, request == null ? null : request.idempotencyKey(), request,
                SavedBottleRemedyRecord.class, "bar.saved_bottle.remedy_advanced", "bar.saved_bottle.remedy_advance_failed",
                "bar.saved_bottle.manage", "saved_bottle_remedy", () -> {
                    SavedBottleRemedyActionRequest actual = requireRequest(request);
                    if (authorizationService.currentMerchantScope().isPresent()) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "垫付须由娱乐平台公司审批");
                    }
                    SavedBottleRemedyRecord remedy = requireRemedy(id);
                    SavedBottleRecord bottle = bottleOf(remedy);
                    if (bottle.custodianEntityId() == null || !"original_merchant".equals(remedy.bearerType())) {
                        throw ApiException.conflict("SAVED_BOTTLE_REMEDY_NOT_ADVANCEABLE",
                                "仅退出门店交平台后、由原门店承担的补存/赔付可由平台垫付追收");
                    }
                    if (remedy.advanceApprovedAt() != null) {
                        return remedy;
                    }
                    Instant now = clock.instant();
                    SavedBottleRemedyRecord advanced = remedy.withAdvance(currentActorId(), now);
                    if ("closed".equals(advanced.customerDeliveryStatus())) {
                        advanced = postRecovery(advanced, actual.idempotencyKey(), now);
                    }
                    return repository.saveSavedBottleRemedy(advanced);
                });
    }

    /** 复核认定责任员工（BSM L1072 ①「属员工损坏店内物品、照价赔偿」）：金额由服务端照内部结算价×数量计，不收前端金额；不改对客状态。 */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage")
    @Transactional
    public SavedBottleRemedyRecord assignSavedBottleRemedyStaffLiability(String remedyId, SavedBottleRemedyActionRequest request) {
        String id = requireText(remedyId, "remedy_id");
        return executeWrite("assignSavedBottleRemedyStaffLiability:" + id, request == null ? null : request.idempotencyKey(), request,
                SavedBottleRemedyRecord.class, "bar.saved_bottle.remedy_staff_liability_assigned",
                "bar.saved_bottle.remedy_staff_liability_assign_failed", "bar.saved_bottle.manage", "saved_bottle_remedy", () -> {
                    SavedBottleRemedyActionRequest actual = requireRequest(request);
                    String staff = requireText(actual.staffUserId(), "staff_user_id");
                    SavedBottleRemedyRecord remedy = requireRemedy(id);
                    SavedBottleRecord bottle = bottleOf(remedy);
                    requireCustody(bottle);
                    if (!"none".equals(remedy.recoveryPartyType())) {
                        throw ApiException.conflict("SAVED_BOTTLE_REMEDY_RECOVERY_EXISTS", "该记录已有追收/赔偿对象");
                    }
                    Long amount = settlementPriceResolver.apply(bottle).map(price -> price * remedyQuantity(bottle))
                            .orElse(remedy.lossCostCent() != null ? remedy.lossCostCent() : remedy.amountCent());
                    if (amount == null) {
                        throw ApiException.conflict("SAVED_BOTTLE_REMEDY_PRICE_UNRESOLVED", "该酒品未配置内部结算价，无法照价确定员工赔偿金额");
                    }
                    return repository.saveSavedBottleRemedy(remedy.withRecovery("staff", staff, amount, "pending", null,
                            safeList(actual.evidenceIds()), clock.instant()));
                });
    }

    /** 员工赔偿收回登记（向原门店的追收由应收台账结清自动回写，不走此入口）。 */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage")
    @Transactional
    public SavedBottleRemedyRecord settleSavedBottleRemedyRecovery(String remedyId, SavedBottleRemedyActionRequest request) {
        String id = requireText(remedyId, "remedy_id");
        return executeWrite("settleSavedBottleRemedyRecovery:" + id, request == null ? null : request.idempotencyKey(), request,
                SavedBottleRemedyRecord.class, "bar.saved_bottle.remedy_recovery_settled",
                "bar.saved_bottle.remedy_recovery_settle_failed", "bar.saved_bottle.manage", "saved_bottle_remedy", () -> {
                    SavedBottleRemedyActionRequest actual = requireRequest(request);
                    SavedBottleRemedyRecord remedy = requireRemedy(id);
                    requireCustody(bottleOf(remedy));
                    if (!"staff".equals(remedy.recoveryPartyType()) || !"pending".equals(remedy.recoveryStatus())) {
                        throw ApiException.conflict("SAVED_BOTTLE_REMEDY_RECOVERY_NOT_PENDING",
                                "无待收回的员工赔偿（向原门店的追收经应收台账结清自动回写）");
                    }
                    List<String> evidence = new ArrayList<>(safeList(remedy.recoveryEvidenceIds()));
                    evidence.addAll(safeList(actual.evidenceIds()));
                    return repository.saveSavedBottleRemedy(remedy.withRecovery(remedy.recoveryPartyType(), remedy.recoveryPartyId(),
                            remedy.recoveryAmountCent(), "recovered", remedy.recoveryArApItemId(), evidence, clock.instant()));
                });
    }

    /** FinanceService 统一回写点回调：追收应收结清 → recovered（不改对客交付状态）。 */
    @Override
    public void onRecoverySettled(com.complex.platform.finance.ArApItemRecord item) {
        if (item == null || item.id() == null) {
            return;
        }
        repository.findSavedBottleRemedyByRecoveryArApItemId(item.id())
                .filter(r -> "pending".equals(r.recoveryStatus()))
                .ifPresent(r -> repository.saveSavedBottleRemedy(r.withRecovery(r.recoveryPartyType(), r.recoveryPartyId(),
                        r.recoveryAmountCent(), "recovered", r.recoveryArApItemId(), r.recoveryEvidenceIds(), clock.instant())));
    }

    /** 退出前清点并通知客人在原店取回（BSM L1072 ③）：该门店 stored 且未过期、尚未交平台的存酒逐瓶短信；返回按瓶/按物品清点数。 */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage")
    @Transactional
    public SavedBottleExitNoticeResult noticeSavedBottleExit(SavedBottleExitNoticeRequest request) {
        return executeWrite("noticeSavedBottleExit", request == null ? null : request.idempotencyKey(), request,
                SavedBottleExitNoticeResult.class, "bar.saved_bottle.exit_noticed", "bar.saved_bottle.exit_notice_failed",
                "bar.saved_bottle.manage", "saved_bottle", () -> {
                    SavedBottleExitNoticeRequest actual = requireRequest(request);
                    String merchantId = requireText(actual.merchantId(), "merchant_id");
                    requireMerchantScope(merchantId);
                    List<SavedBottleRecord> bottles = pendingHandoverBottles(merchantId);
                    Map<String, Integer> byItem = new LinkedHashMap<>();
                    int notified = 0;
                    for (SavedBottleRecord bottle : bottles) {
                        byItem.merge(bottle.itemId(), bottle.remainingQuantity(), Integer::sum);
                        if (notificationService != null) {
                            notifySavedBottleNow(bottle, "exit_notice", STORED_WINE_EXIT_NOTICE_TEMPLATE, "swexit:" + bottle.id(), null);
                            notified++;
                        }
                    }
                    return new SavedBottleExitNoticeResult(merchantId, bottles.size(), notified, byItem);
                });
    }

    /**
     * 门店退出存酒交平台（BSM L1072 ④「酒吧退出后，所属娱乐平台公司接收仍在保管有效期内的未结存酒实物和存酒记录，并指定领取地点，
     * 继续按原门店的保管规则和原有效期处理」）：由平台方（无商户 scope）办理；接收主体＝酒吧域归属的娱乐平台公司；该门店全部有效期内未结存酒
     * 必须全部清点（缺项 409）；intact 只记保管主体/领取地点/交接时间，到期日、状态、客户、验证方式全不动；damaged 与实收为 0 的 short 走作废路径
     * （「接收时已存在的毁损、短少……仍由原门店承担」→ 补存/赔付登记时承担方按交接明细判）；已交接过的瓶幂等跳过。
     * 2026-10-20 操作员裁决「按差额」：short 且实收量>0 时不作废，剩余量改为实收量、继续以原瓶由平台保管、客人照常取，
     * 补存/损失/员工赔偿数量＝登记剩余量−实收量（记 shortfall_quantity）。
     */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage")
    @Transactional
    public SavedBottleHandoverRecord handoverSavedBottlesToPlatform(SavedBottleHandoverRequest request) {
        return executeWrite("handoverSavedBottlesToPlatform", request == null ? null : request.idempotencyKey(), request,
                SavedBottleHandoverRecord.class, "bar.saved_bottle.handed_over", "bar.saved_bottle.handover_failed",
                "bar.saved_bottle.manage", "saved_bottle_handover", () -> {
                    SavedBottleHandoverRequest actual = requireRequest(request);
                    String merchantId = requireText(actual.merchantId(), "merchant_id");
                    String location = requireText(actual.pickupLocation(), "pickup_location");
                    if (authorizationService.currentMerchantScope().isPresent()) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "存酒交接须由所属娱乐平台公司接收，门店不得自行办理");
                    }
                    String projectId = repository.findBarProjectId(merchantId).orElse(null);
                    String entityId = managementEntityResolver.apply(projectId);
                    if (entityId == null) {
                        throw ApiException.conflict("REVENUE_STREAM_ATTRIBUTION_MISSING", "酒吧域归属的娱乐平台公司未配置，无法接收存酒");
                    }
                    Map<String, SavedBottleRecord> eligible = new LinkedHashMap<>();
                    for (SavedBottleRecord bottle : pendingHandoverBottles(merchantId)) {
                        eligible.put(bottle.id(), bottle);
                    }
                    Map<String, String[]> wanted = new LinkedHashMap<>();
                    for (Map<String, Object> item : actual.items() == null ? List.<Map<String, Object>>of() : actual.items()) {
                        String bottleId = requireItemText(item, "saved_bottle_id");
                        String condition = trimToDefault(textFromItem(item, "condition"), "intact");
                        if (!HANDOVER_CONDITIONS.contains(condition)) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "condition 须为 intact、damaged 或 short");
                        }
                        wanted.put(bottleId, new String[] {trimToNull(textFromItem(item, "received_quantity")), condition});
                    }
                    List<String> missing = eligible.keySet().stream().filter(bottleId -> !wanted.containsKey(bottleId)).toList();
                    if (!missing.isEmpty()) {
                        throw ApiException.conflict("SAVED_BOTTLE_HANDOVER_INCOMPLETE",
                                "该门店有效期内未结存酒须全部清点交接，缺：" + String.join(",", missing));
                    }
                    Instant now = clock.instant();
                    String handoverId = "sbh_" + slug(actual.idempotencyKey());
                    String actor = currentActorId();
                    List<SavedBottleHandoverRecord.SavedBottleHandoverItemRecord> items = new ArrayList<>();
                    List<SavedBottleRecord> toNotify = new ArrayList<>();
                    int intact = 0;
                    int damaged = 0;
                    for (Map.Entry<String, String[]> entry : wanted.entrySet()) {
                        SavedBottleRecord bottle = eligible.get(entry.getKey());
                        if (bottle == null) {
                            SavedBottleRecord other = repository.findSavedBottleByIdForUpdate(entry.getKey())
                                    .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                            if (other.custodianEntityId() != null) {
                                continue;
                            }
                            throw ApiException.conflict("SAVED_BOTTLE_NOT_HANDOVERABLE",
                                    "存酒「" + entry.getKey() + "」不在有效期内未结状态或不属于该门店，不纳入本次交接");
                        }
                        bottle = repository.findSavedBottleByIdForUpdate(bottle.id()).orElseThrow();
                        int received = entry.getValue()[0] == null ? bottle.remainingQuantity() : parseQuantity(entry.getValue()[0]);
                        String condition = entry.getValue()[1];
                        int shortfall = "short".equals(condition) ? bottle.remainingQuantity() - received : 0;
                        if ("short".equals(condition) && shortfall <= 0) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "短少须实收量小于登记剩余量");
                        }
                        SavedBottleRecord held;
                        if (shortfall > 0 && received > 0) {
                            // 按差额：实收部分不作废，剩余量改为实收量、原瓶继续由平台保管；差额由补存/赔付按 shortfall 处理。
                            held = repository.saveSavedBottle(savedBottleWithCustody(
                                    savedBottleWith(bottle, bottle.status(), received, now), entityId, location, now, handoverId));
                            damaged++;
                            toNotify.add(held);
                        } else {
                            held = repository.saveSavedBottle(savedBottleWithCustody(bottle, entityId, location, now, handoverId));
                            if ("intact".equals(condition)) {
                                intact++;
                                toNotify.add(held);
                            } else {
                                damaged++;
                                applyCancel(held, actual.idempotencyKey() + ":" + held.id(), actual.photoAttachmentIds(), actual.videoAttachmentIds());
                            }
                        }
                        items.add(new SavedBottleHandoverRecord.SavedBottleHandoverItemRecord(held.id(), received, condition, shortfall));
                    }
                    SavedBottleHandoverRecord handover = repository.saveSavedBottleHandover(new SavedBottleHandoverRecord(
                            handoverId, merchantId, projectId, entityId, location, items.size(), intact, damaged,
                            safeList(actual.photoAttachmentIds()), safeList(actual.videoAttachmentIds()), items, actor, now));
                    for (SavedBottleRecord bottle : toNotify) {
                        notifySavedBottleAfterCommit(bottle, "handover", STORED_WINE_HANDOVER_TEMPLATE, "swhandover:" + bottle.id(), null);
                    }
                    return handover;
                });
    }

    public PageResponse<SavedBottleHandoverRecord> listSavedBottleHandovers() {
        return readPage("bar.saved_bottle.handover_viewed", "bar.saved_bottle.handover_view_failed", "bar.saved_bottle.manage",
                "saved_bottle_handover", () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return repository.listSavedBottleHandovers().stream()
                            .filter(h -> scope.isEmpty() || scope.get().equals(h.merchantId()))
                            .toList();
                });
    }

    private SavedBottleRemedyRecord requireRemedy(String remedyId) {
        return repository.findSavedBottleRemedyByIdForUpdate(requireText(remedyId, "remedy_id"))
                .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_REMEDY_NOT_FOUND", "补存/赔付记录不存在"));
    }

    private SavedBottleRecord bottleOf(SavedBottleRemedyRecord remedy) {
        return repository.findSavedBottleByIdForUpdate(remedy.savedBottleId())
                .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
    }

    /** 商户范围闸 + 交平台后门店不得再办理（BSM L867「此例外不开放正常营业门店的跨店取酒」）。 */
    private void requireCustody(SavedBottleRecord bottle) {
        requireOwnMerchant(bottle);
        if (bottle.custodianEntityId() != null && authorizationService.currentMerchantScope().isPresent()) {
            throw ApiException.conflict("SAVED_BOTTLE_HANDED_OVER", "该存酒已交所属娱乐平台公司接收，须到指定领取地点办理");
        }
    }

    private void requireMerchantScope(String merchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(merchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "不得为其他商户办理");
        }
    }

    /** 承担方：未交平台或接收时已毁损/短少 → 原门店；交平台后接收完好再毁损 → 娱乐平台公司（BSM L1072 ④）。 */
    private String[] remedyBearer(SavedBottleRecord bottle) {
        if (bottle.custodianEntityId() == null) {
            return new String[] {"original_merchant", bottle.merchantId()};
        }
        String condition = repository.findSavedBottleHandoverItem(bottle.handoverId(), bottle.id())
                .map(SavedBottleHandoverRecord.SavedBottleHandoverItemRecord::condition).orElse("intact");
        return "intact".equals(condition)
                ? new String[] {"platform_company", bottle.custodianEntityId()}
                : new String[] {"original_merchant", bottle.merchantId()};
    }

    /** 交平台时登记的短少量（登记剩余量−实收量；操作员裁决「按差额」）；非 short 或未交平台为 0。 */
    private int handoverShortfall(SavedBottleRecord bottle) {
        return repository.findSavedBottleHandoverItem(bottle.handoverId(), bottle.id())
                .filter(item -> "short".equals(item.condition()))
                .map(SavedBottleHandoverRecord.SavedBottleHandoverItemRecord::shortfallQuantity)
                .orElse(0);
    }

    /** 补存/损失/员工赔偿数量：交平台短少按差额，否则＝作废时的剩余量。 */
    private int remedyQuantity(SavedBottleRecord bottle) {
        int shortfall = handoverShortfall(bottle);
        return shortfall > 0 ? shortfall : bottle.remainingQuantity();
    }

    /** 经营损失落账；无金额（内部结算价未配置）或未接财务时不阻断对客处理，记审计事件供财务按 R6-D08 补计。 */
    private String postRemedyLoss(String remedyId, String[] bearer, String originalMerchantId, Long amountCent, String idempotencyKey) {
        if (amountCent == null || amountCent <= 0 || financeService == null) {
            auditEventService.recordSuccess("bar.saved_bottle.remedy_loss_unpriced", "bar.saved_bottle.manage",
                    "saved_bottle_remedy", remedyId, null, idempotencyKey,
                    Map.of("bearer_type", bearer[0], "bearer_id", String.valueOf(bearer[1]),
                            "reason", amountCent == null ? "settlement_price_unresolved" : "finance_unavailable"));
            return null;
        }
        String ledgerMerchant = "original_merchant".equals(bearer[0]) ? originalMerchantId : null;
        return financeService.postSavedBottleRemedyLoss(remedyId, bearer[1], ledgerMerchant, amountCent, idempotencyKey).id();
    }

    /** 平台垫付后向原门店挂追收应收（source_type=saved_bottle_remedy_recovery，幂等）；对客状态不变。 */
    private SavedBottleRemedyRecord postRecovery(SavedBottleRemedyRecord remedy, String idempotencyKey, Instant now) {
        if (remedy.recoveryArApItemId() != null) {
            return remedy;
        }
        Long amount = remedy.lossCostCent();
        if (amount == null) {
            throw ApiException.conflict("SAVED_BOTTLE_REMEDY_PRICE_UNRESOLVED", "该酒品未配置内部结算价，垫付金额未定，无法向原门店追收");
        }
        if (financeService == null) {
            throw ApiException.conflict("FINANCE_UNAVAILABLE", "财务台账不可用，无法挂追收应收");
        }
        com.complex.platform.finance.ArApItemRecord item = financeService.postSavedBottleRemedyRecovery(
                remedy.id(), remedy.merchantId(), amount, idempotencyKey + ":recovery");
        return remedy.withRecovery("original_merchant", remedy.merchantId(), amount, "pending", item.id(),
                remedy.recoveryEvidenceIds(), now);
    }

    private List<SavedBottleRecord> pendingHandoverBottles(String merchantId) {
        Instant now = clock.instant();
        return repository.listSavedBottlesByMerchant(merchantId).stream()
                .filter(b -> "stored".equals(b.status()))
                .filter(b -> b.expireAt() == null || b.expireAt().isAfter(now))
                .filter(b -> b.custodianEntityId() == null)
                .toList();
    }

    private SavedBottleRecord savedBottleWithCustody(SavedBottleRecord b, String entityId, String location, Instant at, String handoverId) {
        return new SavedBottleRecord(b.id(), b.customerId(), b.barProjectId(), b.itemId(), b.bottleName(),
                b.initialQuantity(), b.remainingQuantity(), b.unit(), b.expireAt(), b.status(), b.evidenceAttachmentIds(),
                b.createdAt(), at, b.merchantId(), b.isHighValue(), b.highValueThresholdCents(),
                b.bottleLabelValue(), b.customerNameOrNickname(), b.customerPhone(), b.customerPhoneHash(),
                b.registrationPhotoRefs(), b.manualNote(),
                b.transferredAt(), b.transferToNameOrNickname(), b.transferToPhone(), b.transferToPhoneHash(),
                b.remedyOfSavedBottleId(), entityId, location, at, handoverId);
    }

    private void consumeBatchesFefo(String itemId, String locationId, int quantity) {
        int remaining = quantity;
        for (InventoryStockBatchRecord batch : repository.listActiveBatchesForConsumption(itemId, locationId)) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(remaining, batch.quantity());
            if (repository.atomicDecreaseBatchQuantity(batch.id(), take) > 0) {
                remaining -= take;
            }
        }
    }

    private static int parseQuantity(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", "received_quantity 须为不小于 0 的整数");
        }
    }

    /** 暴露给 Controller：列高价值复核单 + approve/reject。 */
    public PageResponse<InventoryHighValueReviewRecord> listHighValueReviews() {
        return highValueReviewService.listHighValueReviews();
    }

    // AUD-020: capability gate is placed on this outward wrapper. InventoryService is a Spring proxy,
    // so when the controller calls it ActionGateAspect matches this annotation and enforces the
    // inventory.high_value.review terminal switch first (disabled -> TERMINAL_DISABLED, no stock change).
    // The sub-service InventoryHighValueReviewService is hand-instantiated in-domain (not a proxy), so the
    // aspect never matches its inner review(); the guard must live on this proxied entry point.
    // GuardedActionProxyBypassArchTest prevents regressions.
    @com.complex.platform.gate.GuardedAction(capability = "inventory.high_value.review")
    @Transactional
    public InventoryHighValueReviewRecord reviewHighValue(
            String reviewId, com.complex.platform.inventory.dto.HighValueReviewDecisionRequest request) {
        return highValueReviewService.review(reviewId, request);
    }

    // ── V108/V126：盘点录入闭环（建盘点单 → 录差异 → 差异复核(approved/rejected) → 查询）。 ──
    // 复核 approved 是解锁 finance postInventoryVariance 入账的关键（finance 依赖 review_status='approved'）。

    private static final Set<String> INVENTORY_VARIANCE_REASONS =
            Set.of("damage", "shortage", "overage", "supplier_fault");

    // ── V128 / REQ-39：溯源码主数据生命周期。 ──
    private static final Set<String> TRACE_CODE_STATUSES =
            Set.of("registered", "printed", "bound", "consumed");

    public PageResponse<StocktakeRecord> listStocktakes() {
        return readPage(
                "inventory.stocktake.viewed",
                "inventory.stocktake.view_failed",
                "inventory.stocktake.view",
                "stocktake",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listStocktakesByMerchant)
                                .orElseGet(repository::listStocktakes);
                }
        );
    }

    public StocktakeDetailRecord getStocktake(String stocktakeId) {
        String id = requireText(stocktakeId, "stocktake_id");
        try {
            StocktakeRecord stocktake = loadStocktakeScoped(id);
            List<InventoryVarianceRecord> variances = repository.listInventoryVariancesByStocktake(id);
            auditEventService.recordSuccess(
                    "inventory.stocktake.detail_viewed", "inventory.stocktake.view", "stocktake",
                    id, null, null, Map.of("variance_count", variances.size()));
            return new StocktakeDetailRecord(stocktake, variances);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "inventory.stocktake.detail_view_failed", "inventory.stocktake.view", "stocktake",
                    id, null, null, codeOf(exception));
            throw exception;
        }
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stocktake.create")
    @Transactional
    public StocktakeRecord createStocktake(com.complex.platform.inventory.dto.CreateStocktakeRequest request) {
        return executeWrite(
                "createStocktake",
                request == null ? null : request.idempotencyKey(),
                request,
                StocktakeRecord.class,
                "inventory.stocktake.created",
                "inventory.stocktake.create_failed",
                "inventory.stocktake.create",
                "stocktake",
                () -> {
                    com.complex.platform.inventory.dto.CreateStocktakeRequest actual = requireRequest(request);
                    String warehouseId = requireText(actual.warehouseId(), "warehouse_id");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    // 盘点单经营日缺省取仓库所属项目的当前营业日（ENUM:187）。
                    LocalDate businessDate = actual.businessDate() == null
                            ? days().businessDate(repository.findWarehouseProjectId(warehouseId).orElse(null), null, Instant.now())
                            : actual.businessDate();
                    Instant now = Instant.now();
                    String slug = slug(actual.idempotencyKey());
                    return repository.saveStocktake(new StocktakeRecord(
                            "stocktake_" + slug,
                            "ST" + now.toEpochMilli() + slug.substring(0, Math.min(6, slug.length())).toUpperCase(Locale.ROOT),
                            warehouseId,
                            merchantId,
                            businessDate,
                            "counting",
                            trimToNull(actual.remark()),
                            now,
                            now));
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stocktake.variance_record")
    @Transactional
    public InventoryVarianceRecord recordVariance(
            String stocktakeId, com.complex.platform.inventory.dto.RecordVarianceRequest request) {
        String id = requireText(stocktakeId, "stocktake_id");
        return executeWrite(
                "recordVariance:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryVarianceRecord.class,
                "inventory.stocktake.variance_recorded",
                "inventory.stocktake.variance_record_failed",
                "inventory.stocktake.variance_record",
                "inventory_variance",
                () -> {
                    com.complex.platform.inventory.dto.RecordVarianceRequest actual = requireRequest(request);
                    StocktakeRecord stocktake = loadStocktakeScoped(id);
                    if (!List.of("counting", "reviewing").contains(stocktake.status())) {
                        throw ApiException.conflict("STOCKTAKE_NOT_OPEN",
                                "盘点已关闭，不能再登记差异");
                    }
                    String itemId = requireText(actual.itemId(), "item_id");
                    String reason = normalizeAllowed(actual.reason(), INVENTORY_VARIANCE_REASONS, "reason");
                    // 2e 盘点冻结：book_qty 取系统账面值（item 在该 warehouse 全部 location 的 quantity 合计），
                    // 不信客户端传入——此前 bookQty 由前端决定，盘点差异基线可被任意操纵，盘点公信力为零。
                    java.math.BigDecimal bookQty = java.math.BigDecimal.valueOf(
                            repository.sumStockBalanceByItemAndWarehouse(itemId, stocktake.warehouseId()));
                    java.math.BigDecimal countedQty = actual.countedQty() == null
                            ? java.math.BigDecimal.ZERO : actual.countedQty();
                    // variance_qty = counted − book（带符号）。
                    java.math.BigDecimal varianceQty = countedQty.subtract(bookQty);
                    // variance_amount_cent：显式传入优先；缺省按 variance_qty × unitCostCents 估算（成本估算，带符号）。
                    long varianceAmountCent = resolveVarianceAmountCent(
                            actual.varianceAmountCent(), varianceQty, actual.unitCostCents());
                    Instant now = Instant.now();
                    return repository.saveInventoryVariance(new InventoryVarianceRecord(
                            "variance_" + slug(actual.idempotencyKey()),
                            stocktake.id(),
                            itemId,
                            trimToNull(actual.batchNo()),
                            bookQty,
                            countedQty,
                            varianceQty,
                            varianceAmountCent,
                            reason,
                            trimToNull(actual.supplierId()),
                            "pending",
                            null,
                            null,
                            null,
                            stocktake.merchantId(),
                            now,
                            now));
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stocktake.variance_review", admissionAction = "inventory.stocktake_variance_review", objectType = "inventory_variance")
    @Transactional
    public InventoryVarianceRecord reviewVariance(
            String stocktakeId, String varianceId,
            com.complex.platform.inventory.dto.ReviewVarianceRequest request) {
        String stId = requireText(stocktakeId, "stocktake_id");
        String vId = requireText(varianceId, "variance_id");
        return executeWrite(
                "reviewVariance:" + stId + ":" + vId,
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryVarianceRecord.class,
                "inventory.stocktake.variance_reviewed",
                "inventory.stocktake.variance_review_failed",
                "inventory.stocktake.variance_review",
                "inventory_variance",
                () -> {
                    com.complex.platform.inventory.dto.ReviewVarianceRequest actual = requireRequest(request);
                    String decision = requireDecision(actual.decision());
                    InventoryVarianceRecord variance = repository.findInventoryVarianceByIdForUpdate(vId)
                            .orElseThrow(() -> ApiException.notFound(
                                    "INVENTORY_VARIANCE_NOT_FOUND", "库存差异不存在或已被删除"));
                    if (!variance.stocktakeId().equals(stId)) {
                        throw ApiException.notFound("INVENTORY_VARIANCE_NOT_FOUND",
                                "该库存差异不属于此盘点");
                    }
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(variance.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "该库存差异不属于当前商户");
                    }
                    if (!"pending".equals(variance.reviewStatus())) {
                        throw ApiException.conflict("INVENTORY_VARIANCE_NOT_PENDING",
                                "该库存差异不在待审核状态");
                    }
                    String reviewer = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    // 条件 UPDATE WHERE review_status='pending'，受影响行数==1 才成功（防并发重复复核）。
                    int affected = repository.reviewInventoryVarianceIfPending(vId, decision, reviewer, now);
                    if (affected != 1) {
                        throw ApiException.conflict("INVENTORY_VARIANCE_NOT_PENDING",
                                "该库存差异已被并发审核，请刷新后重试");
                    }
                    // 盘点纠账：复核通过即把物理库存余额回写到实盘，消除账实持续背离。delta=variance_qty=counted−book（warehouse 合计口径）。
                    // 差异为 item 级（无 location 维度），按 (item,warehouse) 的余额行回写：
                    //   盘盈(+)：累加到首个已有 location（无行则建库房级 location=warehouseId）；
                    //   盘亏(−)：按行依次扣减直至吸收完（多库位也能纠账，消除"多行即死路"）；全库房可用量不足才抛冲突（不静默背离）。同事务原子。
                    if ("approved".equals(decision)) {
                        int delta = variance.varianceQty() == null ? 0 : variance.varianceQty().intValue();
                        if (delta != 0) {
                            StocktakeRecord stocktake = loadStocktakeScoped(stId);
                            List<StockBalanceRecord> rows = repository.listStockBalances().stream()
                                    .filter(b -> variance.itemId().equals(b.itemId())
                                            && stocktake.warehouseId().equals(b.warehouseId()))
                                    .toList();
                            if (delta > 0) {
                                String targetLocation = rows.isEmpty()
                                        ? stocktake.warehouseId() : rows.get(0).locationId();
                                repository.atomicIncreaseStockBalance(
                                        variance.itemId(), targetLocation, delta,
                                        "bal_recon_" + slug(vId), variance.merchantId(), stocktake.warehouseId(),
                                        authorizationService.currentProjectScopeQuietly().orElse(null));
                                // 裁决#7 守恒账本：盘盈纠账改了余额，同事务（@Transactional reviewVariance）补写一条
                                // inventory_stock_movements（count_correction，盘盈 → 目标库位入），否则按流水重建对不平。
                                repository.saveStockMovement(new StockMovementRecord(
                                        "movement_" + slug(vId + ":variance_recon"),
                                        "count_correction",
                                        variance.itemId(),
                                        null,
                                        targetLocation,
                                        delta,
                                        // V296：盘盈无进货价（成本由仓储层按当前均价自动计入）。
                                        null,
                                        null,
                                        "inventory_variance",
                                        variance.id(),
                                        null,
                                        null,
                                        List.of(),
                                        "completed",
                                        now,
                                        variance.merchantId()), authorizationService.currentProjectScopeQuietly().orElse(null));
                            } else {
                                int remaining = -delta;
                                for (StockBalanceRecord row : rows) {
                                    if (remaining <= 0) {
                                        break;
                                    }
                                    int take = Math.min(remaining, Math.max(0, row.availableQuantity()));
                                    if (take > 0 && repository.atomicDecreaseStockBalance(
                                            variance.itemId(), row.locationId(), take,
                                            authorizationService.currentMerchantScope().orElse(null)) == 1) {
                                        remaining -= take;
                                        // 守恒账本：盘亏逐库位扣减，按库位各补一条流水（source=该库位），
                                        // 多库位也能逐位回放对平，避免合并一条导致重建在库位维度漂移。
                                        repository.saveStockMovement(new StockMovementRecord(
                                                "movement_" + slug(vId + ":variance_recon:" + row.locationId()),
                                                "count_correction",
                                                variance.itemId(),
                                                row.locationId(),
                                                null,
                                                take,
                                                // V296：盘亏出库成本由仓储层按移动加权均价自动定稿。
                                                null,
                                                null,
                                                "inventory_variance",
                                                variance.id(),
                                                null,
                                                null,
                                                List.of(),
                                                "completed",
                                                now,
                                                variance.merchantId()), authorizationService.currentProjectScopeQuietly().orElse(null));
                                    }
                                }
                                if (remaining > 0) {
                                    throw ApiException.conflict("INVENTORY_VARIANCE_RECONCILE_INSUFFICIENT",
                                            "仓库余量不足以核销已批准的短缺，需人工调整");
                                }
                            }
                        }
                    }
                    return new InventoryVarianceRecord(
                            variance.id(), variance.stocktakeId(), variance.itemId(), variance.batchNo(),
                            variance.bookQty(), variance.countedQty(), variance.varianceQty(),
                            variance.varianceAmountCent(), variance.reason(), variance.supplierId(),
                            decision, reviewer, now, variance.postedArApItemId(),
                            variance.merchantId(), variance.createdAt(), now);
                }
        );
    }

    /** 按 id 读取盘点单并校验归属（merchant scope）。不存在抛 404，跨商户抛 403。 */
    private StocktakeRecord loadStocktakeScoped(String stocktakeId) {
        StocktakeRecord stocktake = repository.findStocktakeById(stocktakeId)
                .orElseThrow(() -> ApiException.notFound("STOCKTAKE_NOT_FOUND", "盘点不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(stocktake.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "该盘点不属于当前商户");
        }
        return stocktake;
    }

    /** variance_amount_cent：显式传入优先；否则 round(variance_qty × unitCostCents)（带符号，可为 0）。 */
    private long resolveVarianceAmountCent(
            Long explicitAmountCent, java.math.BigDecimal varianceQty, Long unitCostCents) {
        if (explicitAmountCent != null) {
            return explicitAmountCent;
        }
        if (unitCostCents == null) {
            return 0L;
        }
        if (unitCostCents < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "单位成本不能为负数");
        }
        return varianceQty
                .multiply(java.math.BigDecimal.valueOf(unitCostCents))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();
    }

    // ── V128 / REQ-39：溯源码主数据 CRUD（贴码登记 → 出库打印 → 责任链查询/列表）。 ──
    // 与扫码交接（scan-handover / TraceHandover）互补：本组端点是码本身的生命周期主数据。

    public PageResponse<InventoryTraceCodeRecord> listTraceCodes() {
        return readPage(
                "inventory.trace_code.listed",
                "inventory.trace_code.list_failed",
                "inventory.trace_code.view",
                "inventory_trace_code",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listTraceCodesByMerchant)
                                .orElseGet(repository::listTraceCodes);
                }
        );
    }

    /** REQ-39 责任链查询：按 id 或 trace_code 任一定位（merchant scope）。不存在 404，跨商户 403。 */
    public InventoryTraceCodeRecord getTraceCode(String idOrCode) {
        String key = requireText(idOrCode, "trace_code_id");
        try {
            InventoryTraceCodeRecord traceCode = loadTraceCodeScoped(key);
            auditEventService.recordSuccess(
                    "inventory.trace_code.viewed", "inventory.trace_code.view", "inventory_trace_code",
                    traceCode.id(), null, null, Map.of("trace_code", traceCode.traceCode()));
            return traceCode;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "inventory.trace_code.view_failed", "inventory.trace_code.view", "inventory_trace_code",
                    key, null, null, codeOf(exception));
            throw exception;
        }
    }

    /** REQ-39 贴码登记（入库）：status=registered，幂等；trace_code 缺省服务端生成，重复 trace_code 冲突拒绝。 */
    @com.complex.platform.gate.GuardedAction(capability = "inventory.trace_code.register")
    @Transactional
    public InventoryTraceCodeRecord registerTraceCode(RegisterTraceCodeRequest request) {
        return executeWrite(
                "registerTraceCode",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryTraceCodeRecord.class,
                "inventory.trace_code.registered",
                "inventory.trace_code.register_failed",
                "inventory.trace_code.register",
                "inventory_trace_code",
                () -> {
                    RegisterTraceCodeRequest actual = requireRequest(request);
                    String itemId = requireText(actual.itemId(), "item_id");
                    repository.findInventoryItemById(itemId)
                            .orElseThrow(() -> ApiException.notFound(
                                    "INVENTORY_ITEM_NOT_FOUND", "库存商品不存在或已被删除"));
                    // 归属隔离:商户范围用户强制用自身商户;平台管理员(无范围)代登记须在请求指定归属商户,
                    // 二者皆无则返回干净业务错(而非 NOT NULL 崩溃)。
                    String merchantId = authorizationService.currentMerchantScope()
                            .orElseGet(() -> trimToNull(actual.merchantId()));
                    if (merchantId == null) {
                        throw ApiException.badRequest("MERCHANT_REQUIRED", "平台代登记溯源码需先选定所属商户");
                    }
                    String slug = slug(actual.idempotencyKey());
                    // trace_code 缺省服务端生成（基于幂等键稳定，重复提交命中幂等不重复建）。
                    String traceCode = trimToDefault(actual.traceCode(), "TRC-" + slug.toUpperCase(Locale.ROOT));
                    // 唯一冲突处理：码值已存在（非本次幂等重放）→ 409 拒绝。
                    repository.findTraceCodeByCode(traceCode).ifPresent(existing -> {
                        throw ApiException.conflict("TRACE_CODE_DUPLICATE",
                                "溯源码已存在，请勿重复登记");
                    });
                    Instant now = Instant.now();
                    return repository.saveTraceCode(new InventoryTraceCodeRecord(
                            "trace_code_" + slug,
                            traceCode,
                            itemId,
                            trimToNull(actual.batchId()),
                            merchantId,
                            "registered",
                            null,
                            null,
                            authorizationService.currentActor().id(),
                            now,
                            now));
                }
        );
    }

    /** REQ-39 出库打印码：status registered→printed，记 printed_at，幂等；条件 UPDATE 受影响 1 行才成功，防重复打印。 */
    @com.complex.platform.gate.GuardedAction(capability = "inventory.trace_code.print")
    @Transactional
    public InventoryTraceCodeRecord printTraceCode(
            String traceCodeId, com.complex.platform.inventory.dto.PrintTraceCodeRequest request) {
        String id = requireText(traceCodeId, "trace_code_id");
        return executeWrite(
                "printTraceCode:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryTraceCodeRecord.class,
                "inventory.trace_code.printed",
                "inventory.trace_code.print_failed",
                "inventory.trace_code.print",
                "inventory_trace_code",
                () -> {
                    requireRequest(request);
                    InventoryTraceCodeRecord traceCode = loadTraceCodeScoped(id);
                    if (!"registered".equals(traceCode.status())) {
                        throw ApiException.conflict("TRACE_CODE_NOT_PRINTABLE",
                                "溯源码不在已登记状态");
                    }
                    Instant now = Instant.now();
                    // 条件 UPDATE WHERE status='registered'，受影响行数==1 才成功（防并发重复打印）。
                    int affected = repository.printTraceCodeIfRegistered(id, now, null);
                    if (affected != 1) {
                        throw ApiException.conflict("TRACE_CODE_NOT_PRINTABLE",
                                "溯源码已被并发打印，请刷新后重试");
                    }
                    return new InventoryTraceCodeRecord(
                            traceCode.id(), traceCode.traceCode(), traceCode.itemId(), traceCode.batchId(),
                            traceCode.merchantId(), "printed", traceCode.boundRef(), now,
                            traceCode.createdBy(), traceCode.createdAt(), now);
                }
        );
    }

    /** 按 id 或 trace_code 读取溯源码并校验归属（merchant scope）。不存在抛 404，跨商户抛 403。 */
    private InventoryTraceCodeRecord loadTraceCodeScoped(String idOrCode) {
        InventoryTraceCodeRecord traceCode = repository.findTraceCodeById(idOrCode)
                .or(() -> repository.findTraceCodeByCode(idOrCode))
                .orElseThrow(() -> ApiException.notFound("TRACE_CODE_NOT_FOUND", "溯源码不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(traceCode.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "该溯源码不属于当前商户");
        }
        return traceCode;
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.adjust", admissionAction = "inventory.stock_adjust", objectType = "inventory_stock_adjustment")
    @Transactional
    public StockAdjustmentRecord createStockAdjustment(StockAdjustmentRequest request) {
        return stockMovementService.createStockAdjustment(request);
    }

    public PageResponse<StockBalanceRecord> listStockBalances() {
        return readPage(
                "inventory.stock_balance.viewed",
                "inventory.stock_balance.viewed_failed",
                "inventory.stock.view",
                "inventory_stock_balance",
                () -> {
                    // C2：商户员工按 merchant 隔离；商户空但项目非空（项目员工）按 project 隔离；
                    // 平台管理员（二者皆空）看全量。杜绝项目员工越读他项目/全平台库存余额。
                    Optional<String> merchantScope = authorizationService.currentMerchantScope();
                    if (merchantScope.isPresent()) {
                        return repository.listStockBalancesByMerchant(merchantScope.get());
                    }
                    Optional<String> projectScope = authorizationService.currentProjectScopeQuietly();
                    return projectScope.map(repository::listStockBalancesByProject)
                                       .orElseGet(repository::listStockBalances);
                }
        );
    }

    public PageResponse<InventoryStockBatchRecord> listStockBatches() {
        return readPage(
                "inventory.stock_batch.viewed",
                "inventory.stock_batch.view_failed",
                "inventory.stock.view",
                "inventory_stock_batch",
                () -> {
                    // C2: merchant staff -> merchant; project staff (no merchant) -> project; platform admin -> all.
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent()) {
                        return repository.listStockBatchesByMerchant(scope.get());
                    }
                    return authorizationService.currentProjectScopeQuietly()
                            .map(repository::listStockBatchesByProject)
                            .orElseGet(repository::listStockBatches);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.batch.manage")
    @Transactional
    public InventoryStockBatchRecord createStockBatch(InventoryStockBatchRequest request) {
        return executeWrite(
                "createInventoryStockBatch",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryStockBatchRecord.class,
                "inventory.stock_batch.recorded",
                "inventory.stock_batch.record_failed",
                "inventory.stock.batch.manage",
                "inventory_stock_batch",
                () -> createStockBatchOnce(request)
        );
    }

    public PageResponse<InventoryRiskAlertRecord> listRiskAlerts() {
        return readPage(
                "inventory.risk_alert.viewed",
                "inventory.risk_alert.view_failed",
                "inventory.risk_alert.view",
                "inventory_risk_alert",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listRiskAlertsByMerchant)
                                .orElseGet(repository::listRiskAlerts);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.risk.scan")
    @Transactional
    public InventoryRiskScanResult runInventoryRiskScan(InventoryRiskScanRequest request) {
        return executeWrite(
                "runInventoryRiskScan",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryRiskScanResult.class,
                "inventory.risk_scan.completed",
                "inventory.risk_scan.failed",
                "inventory.risk.scan",
                "inventory_risk_scan",
                () -> scanInventoryRisks(request)
        );
    }

    public PageResponse<SupplierReconciliationRecord> listSupplierReconciliations() {
        return readPage(
                "inventory.supplier_reconciliation.viewed",
                "inventory.supplier_reconciliation.view_failed",
                "inventory.supplier.reconcile",
                "inventory_supplier_reconciliation",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listSupplierReconciliationsByMerchant)
                                .orElseGet(repository::listSupplierReconciliations);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.supplier.reconcile")
    @Transactional
    public SupplierReconciliationRecord createSupplierReconciliation(SupplierReconciliationRequest request) {
        return executeWrite(
                "createSupplierReconciliation",
                request == null ? null : request.idempotencyKey(),
                request,
                SupplierReconciliationRecord.class,
                "inventory.supplier_reconciliation.created",
                "inventory.supplier_reconciliation.create_failed",
                "inventory.supplier.reconcile",
                "inventory_supplier_reconciliation",
                () -> createSupplierReconciliationOnce(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public StockMovementRecord createStockIn(StockInRequest request) {
        return stockMovementService.createStockIn(request);
    }

    public PageResponse<StockMovementRecord> listStockMovements(int page, int size) {
        return stockMovementService.listStockMovements(page, size);
    }

    /**
     * V296 成本流水分页：库存流水含移动加权成本列（unit_cost_cent/total_cost_cent，NULL=无成本基准）。
     * 租户口径与 listStockMovements 一致：商户员工看本商户，项目员工看本项目，平台管理员看全量。
     */
    public PageResponse<StockMovementRecord> listCostLedger(String itemId, int page, int size) {
        try {
            Optional<String> merchantScope = authorizationService.currentMerchantScope();
            String merchantId = merchantScope.orElse(null);
            String projectId = merchantId != null ? null
                    : authorizationService.currentProjectScopeQuietly().orElse(null);
            PageResponse<StockMovementRecord> result =
                    repository.listCostLedgerPaged(merchantId, projectId, trimToNull(itemId), page, size);
            auditEventService.recordSuccess(
                    "inventory.cost_ledger.viewed", "inventory.stock.view", "inventory_stock_movement",
                    "all", null, null, Map.of("count", result.total()));
            return result;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "inventory.cost_ledger.view_failed", "inventory.stock.view", "inventory_stock_movement",
                    "all", null, null, codeOf(exception));
            throw exception;
        }
    }

    /**
     * V296 期间出库成本汇总（分红引擎成本侧取数点）：按商户+期间返回出库成本合计。
     * 窗口 = [periodStart 经营日起, periodEnd+1 经营日起)，与财务日结/跨域汇总同一切分基准。
     * 商户员工强制按自身商户；merchant_id 参数仅对平台/项目管理员生效（可空=范围内全量）。
     */
    public InventoryCostSummaryRecord getCostSummary(String merchantIdParam, String periodStart, String periodEnd) {
        try {
            LocalDate start = requireIsoDate(periodStart, "period_start");
            LocalDate end = requireIsoDate(periodEnd, "period_end");
            if (end.isBefore(start)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "期间结束日期不能早于开始日期");
            }
            String scope = authorizationService.currentMerchantScope().orElse(null);
            String merchantId = scope != null ? scope : trimToNull(merchantIdParam);
            String projectId = scope != null ? null
                    : authorizationService.currentProjectScopeQuietly().orElse(null);
            // S-2：经营日切分必须来自注入的 bean（配置口径），不做硬编码兜底——分红成本侧取数点不许口径漂移。
            if (businessDayResolver == null) {
                throw new IllegalStateException(
                        "BusinessDayResolver not injected; cost summary requires the configured business-day cutoff");
            }
            // 第十八批第 1 条：与分红收入侧同窗——商户名下唯一酒吧按该酒吧营业日，否则按所属综合体。
            String bar = businessDayResolver.barOfMerchant(merchantId);
            String dayProject = bar != null ? null : merchantId != null ? businessDayResolver.projectOfMerchant(merchantId) : projectId;
            InventoryCostSummaryRecord summary = repository.sumOutboundCost(
                    merchantId, projectId, start.toString(), end.toString(),
                    businessDayResolver.start(dayProject, bar, start), businessDayResolver.end(dayProject, bar, end));
            auditEventService.recordSuccess(
                    "inventory.cost_summary.viewed", "inventory.stock.view", "inventory_stock_movement",
                    "all", null, null,
                    Map.of("total_cost_cent", summary.outboundTotalCostCent(),
                            "movement_count", summary.outboundMovementCount()));
            return summary;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "inventory.cost_summary.view_failed", "inventory.stock.view", "inventory_stock_movement",
                    "all", null, null, codeOf(exception));
            throw exception;
        }
    }

    private LocalDate requireIsoDate(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (java.time.format.DateTimeParseException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须为日期格式（yyyy-MM-dd）");
        }
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public StockMovementRecord createStockMovement(StockMovementRequest request) {
        return stockMovementService.createStockMovement(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public StockMovementRecord transferStockMovement(StockMovementRequest request) {
        return stockMovementService.transferStockMovement(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public StockMovementRecord createTypedStockMovement(String movementType, StockMovementRequest request) {
        return stockMovementService.createTypedStockMovement(movementType, request);
    }

    /** 券核销领取查商品在库位的可用量(缺货则退积分)。 */
    public int availableStock(String itemId, String locationId) {
        return repository.findStockBalance(itemId, locationId)
                .map(StockBalanceRecord::availableQuantity)
                .orElse(0);
    }

    /**
     * V312 发券预占库存：从 (item,location) 的可用量原子预占 qty 件（available->reserved，quantity 不变）。
     * 返回 true=预占成功；false=可用不足（含无余额行）→ 上层拒绝发券/兑换、不扣积分。守卫式 UPDATE 行锁防并发超卖。
     * 无 @GuardedAction/无商户 scope：这是发券（含客人积分兑换）触发的内部台账操作，随调用方事务提交。
     */
    public boolean reserveStock(String itemId, String locationId, int qty) {
        if (qty <= 0) {
            return true;
        }
        return repository.reserveStockBalance(itemId, locationId, qty) > 0;
    }

    /**
     * V312 释放预占：把 qty 件从 reserved 还回 available（核销转实扣前置、或过期/作废释放）。返回受影响行数。
     * best-effort：0 表示该行 reserved 不足以释放（历史券/漂移），由调用方按语义处理（不强抛，避免误挡核销）。
     */
    public int releaseStockReservation(String itemId, String locationId, int qty) {
        if (qty <= 0) {
            return 0;
        }
        return repository.releaseStockReservation(itemId, locationId, qty);
    }

    public PageResponse<InventorySupplierRecord> listInventorySuppliers() {
        return readPage(
                "inventory.supplier_viewed",
                "inventory.supplier_view_failed",
                "inventory.supplier.view",
                "inventory_supplier",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listInventorySuppliersByMerchant)
                                .orElseGet(repository::listInventorySuppliers);
                }
        );
    }

    // ── V277 跨主体流转:供货主体 + 流转单(建单→发货→扫码收货+溯源归属变更) ──
    @Transactional
    public SupplyEntityRecord createSupplyEntity(com.complex.platform.inventory.dto.SupplyEntityRequest request) {
        return executeWrite("createSupplyEntity", request == null ? null : request.idempotencyKey(), request,
                SupplyEntityRecord.class, "inventory.supply_entity_changed", "inventory.supply_entity_change_failed",
                "inventory.supply_entity.manage", "supply_entity",
                () -> {
                    com.complex.platform.inventory.dto.SupplyEntityRequest actual = requireRequest(request);
                    Instant now = Instant.now();
                    return repository.saveSupplyEntity(new SupplyEntityRecord(
                            "supply_" + slug(actual.idempotencyKey()),
                            requireText(actual.name(), "name"),
                            trimToDefault(actual.entityType(), "liquor"),
                            "platform",
                            trimToDefault(actual.status(), "active"),
                            trimToNull(actual.settlementAccount()),
                            null, now, now));
                });
    }

    public PageResponse<SupplyEntityRecord> listSupplyEntities() {
        authorizationService.requirePermission("inventory.supply_entity.view");
        List<SupplyEntityRecord> rows = repository.listSupplyEntities();
        return new PageResponse<>(rows, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, rows.size());
    }

    public InventoryTransferOrderRecord createTransferOrder(com.complex.platform.inventory.dto.CreateTransferOrderRequest request) {
        return executeWrite("createTransferOrder", request == null ? null : request.idempotencyKey(), request,
                InventoryTransferOrderRecord.class, "inventory.transfer_order_created", "inventory.transfer_order_create_failed",
                "inventory.transfer_order.manage", "inventory_transfer_order",
                () -> {
                    com.complex.platform.inventory.dto.CreateTransferOrderRequest actual = requireRequest(request);
                    String transferType = requireText(actual.transferType(), "transfer_type");
                    if (!"supply".equals(transferType) && !"lending".equals(transferType)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "调拨类型必须为供货（supply）或借调（lending）");
                    }
                    String fromType = requireText(actual.fromOwnerType(), "from_owner_type");
                    String fromId = requireText(actual.fromOwnerId(), "from_owner_id");
                    String toType = requireText(actual.toOwnerType(), "to_owner_type");
                    String toId = requireText(actual.toOwnerId(), "to_owner_id");
                    if ("supply".equals(transferType)) {
                        if (!"supply_entity".equals(fromType)) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "供货调拨必须由供货主体发起");
                        }
                        repository.findSupplyEntityById(fromId).orElseThrow(() ->
                                ApiException.notFound("SUPPLY_ENTITY_NOT_FOUND", "供货主体不存在或已被删除"));
                    }
                    if (!"merchant".equals(toType)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "接收方类型必须为商户");
                    }
                    if ("lending".equals(transferType) && !"merchant".equals(fromType)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "借调必须发生在商户之间");
                    }
                    if (actual.lines() == null || actual.lines().isEmpty()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "调拨单至少需要一行明细");
                    }
                    // FS-019（BSM L1201「不可消费物品（设备、器具等）的借用必须约定归还时间」「可消费物品的借用实质是调，交付时所有权随之转移」）：
                    // 借用只收不可消费物品且必须约定归还时间；可消费物品只能按内部结算价买断。
                    boolean borrow = "lending".equals(transferType) && "borrow".equals(trimToNull(actual.lendingMode()));
                    if (borrow) {
                        if (actual.returnDueAt() == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "借用必须约定归还时间（return_due_at）");
                        }
                        for (com.complex.platform.inventory.dto.TransferLineRequest lr : actual.lines()) {
                            InventoryItemRecord item = repository.findInventoryItemById(requireText(lr.itemId(), "item_id")).orElseThrow(() ->
                                    ApiException.notFound("INVENTORY_ITEM_NOT_FOUND", "库存商品不存在或已被删除"));
                            if (!item.nonConsumable()) {
                                throw ApiException.badRequest("VALIDATION_FAILED",
                                        "可消费物品不存在保留所有权的借用，只能按内部结算价买断（调）：" + item.itemName());
                            }
                        }
                    }
                    Instant now = Instant.now();
                    String orderId = "trf_" + slug(actual.idempotencyKey());
                    // FS-011（BSM L1212/L1213）：门店要货（供货调拨）从草稿起步，门店提交后进待供货主体审核；借调沿用新建→发货。
                    boolean requisition = "supply".equals(transferType);
                    InventoryTransferOrderRecord order = new InventoryTransferOrderRecord(orderId, transferType,
                            trimToNull(actual.lendingMode()), fromType, fromId, toType, toId,
                            trimToNull(actual.fromLocationId()), trimToNull(actual.toLocationId()),
                            requisition ? "draft" : "created", trimToNull(actual.operatorNote()), null, now, now,
                            borrow ? actual.returnDueAt() : null, List.of());
                    repository.saveTransferOrder(order);
                    InventoryTransferFlowService.saveLines(repository, orderId, actual.lines(), now);
                    if (requisition) {
                        transferFlow.recordEvent(orderId, null, "draft", trimToNull(actual.operatorNote()));
                    }
                    return order;
                });
    }

    public InventoryTransferOrderRecord shipTransferOrder(String orderId) {
        return shipTransferOrder(orderId, null);
    }

    /**
     * 发货。FS-019（BSM L1201「借出时间取借出方交货扫码的时刻」「每一步都必须扫码」）：借用单有随货溯源码时，借出方交货须逐码扫齐
     * （扫描集与本单码集一致），每码记一行保管台账（借出时间）；提交编号仍为单号（M724-F06）。
     */
    public InventoryTransferOrderRecord shipTransferOrder(String orderId, com.complex.platform.inventory.dto.ReceiveTransferRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite("shipTransferOrder:" + id, id, id,
                InventoryTransferOrderRecord.class, "inventory.transfer_order_shipped", "inventory.transfer_order_ship_failed",
                "inventory.transfer_order.manage", "inventory_transfer_order",
                () -> {
                    InventoryTransferOrderRecord o = repository.findTransferOrderById(id).orElseThrow(() ->
                            ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
                    // FS-011：门店要货的发货＝出库（待出库→配送中）；货款在接收方合格签收后形成，借调发货也不再挂账。
                    if ("supply".equals(o.transferType())) {
                        return transferFlow.outbound(o);
                    }
                    if (!"created".equals(o.status())) {
                        throw ApiException.conflict("TRANSFER_ORDER_NOT_CREATED", "只有新建状态的调拨单才能发货");
                    }
                    if (isBorrow(o)) {
                        Instant now = Instant.now();
                        java.util.LinkedHashSet<String> orderCodes = requireScannedAll(o, request == null ? null : request.scannedTraceCodes());
                        int seq = 0;
                        for (String code : orderCodes) {
                            repository.saveTraceOwnershipHistory(new InventoryTraceOwnershipHistoryRecord(
                                    o.id() + "_s" + (seq++), code, o.fromOwnerType(), o.fromOwnerId(),
                                    o.fromOwnerType(), o.fromOwnerId(), o.id(), now, InventoryTraceOwnershipHistoryRecord.CUSTODY));
                        }
                    }
                    return repository.saveTransferOrder(withTransferStatus(o, "shipped"));
                });
    }

    private static boolean isBorrow(InventoryTransferOrderRecord o) {
        return "lending".equals(o.transferType()) && "borrow".equals(o.lendingMode());
    }

    /** 本单随货溯源码集；有码时扫描集须与之完全一致（空扫/漏扫/非本单码拒绝，防调包）。 */
    private java.util.LinkedHashSet<String> requireScannedAll(InventoryTransferOrderRecord o, List<String> scannedCodes) {
        java.util.LinkedHashSet<String> orderCodes = new java.util.LinkedHashSet<>();
        for (InventoryTransferLineRecord l : repository.listTransferLinesByOrder(o.id())) {
            orderCodes.addAll(l.traceCodes());
        }
        List<String> scanned = scannedCodes == null ? List.of() : scannedCodes;
        for (String code : scanned) {
            if (!orderCodes.contains(code)) {
                throw ApiException.badRequest("TRACE_CODE_MISMATCH", "扫描的溯源码不属于本次调拨：" + code);
            }
        }
        if (!orderCodes.isEmpty() && !new java.util.HashSet<>(scanned).equals(new java.util.HashSet<>(orderCodes))) {
            throw ApiException.badRequest("TRACE_CODE_INCOMPLETE",
                    "扫描的溯源码必须与本次调拨的码完全一致，应为 " + orderCodes.size() + " 个");
        }
        return orderCodes;
    }

    public InventoryTransferOrderRecord receiveTransferOrder(String orderId, com.complex.platform.inventory.dto.ReceiveTransferRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite("receiveTransferOrder:" + id, request == null ? null : request.idempotencyKey(), request,
                InventoryTransferOrderRecord.class, "inventory.transfer_order_received", "inventory.transfer_order_receive_failed",
                "inventory.transfer_order.manage", "inventory_transfer_order",
                () -> {
                    InventoryTransferOrderRecord o = repository.findTransferOrderById(id).orElseThrow(() ->
                            ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
                    // FS-011：门店要货在「待签收」按行签收。
                    if ("supply".equals(o.transferType())) {
                        return transferFlow.receive(o, request);
                    }
                    if (!"shipped".equals(o.status())) {
                        throw ApiException.conflict("TRANSFER_ORDER_NOT_SHIPPED", "只有已发货的调拨单才能收货");
                    }
                    Instant now = Instant.now();
                    // 防调包:本单有随货溯源码时,收货必须逐码扫齐(空扫/漏扫拒绝),扫描集须与本单集完全一致。
                    java.util.LinkedHashSet<String> orderCodes = requireScannedAll(o, request == null ? null : request.scannedTraceCodes());
                    boolean borrow = isBorrow(o);
                    int seq = 0;
                    for (String code : orderCodes) {
                        // 防调包(原子):CAS 只在归属=出让方或无主时翻转;命中0行且码已存在=归属第三方(或并发被改走)→拒并回滚。
                        // 命中0行且码不存在=P1 随货新码(归属登记泛化待 P2)→放行。CAS 结果即业务判定,消除 check-then-act 竞态。
                        // FS-019（BSM L1201「「借」只转移保管责任、不转移所有权……借用期间保管人变更为借入方并在溯源码上登记」）：
                        // 借用型只改当前保管主体与在借单，所有权仍是借出方；买断型才翻转所有权。
                        int affected = borrow
                                ? repository.updateTraceCodeCustodianIfOwnedBy(code, o.fromOwnerId(), o.toOwnerType(), o.toOwnerId(), o.id(), now)
                                : repository.updateTraceCodeOwnerIfExists(code, o.fromOwnerId(), o.toOwnerType(), o.toOwnerId(), now);
                        if (affected == 0 && repository.traceCodeExists(code)) {
                            throw ApiException.badRequest("TRACE_OWNER_MISMATCH",
                                    "溯源码属于其他主体，不能收货：" + code);
                        }
                        repository.saveTraceOwnershipHistory(new InventoryTraceOwnershipHistoryRecord(
                                o.id() + "_h" + (seq++), code, o.fromOwnerType(), o.fromOwnerId(),
                                o.toOwnerType(), o.toOwnerId(), o.id(), now,
                                borrow ? InventoryTraceOwnershipHistoryRecord.CUSTODY : InventoryTraceOwnershipHistoryRecord.OWNERSHIP));
                    }
                    // ponytail: 借调两侧库存流水未接（门店要货的合格入库见 InventoryTransferFlowService.receive）。
                    recordLendingSettlementOnReceipt(o, now);
                    return repository.saveTransferOrder(withTransferStatus(o, "received"));
                });
    }

    /**
     * 借用归还（AUD-002/003 → FS-019）。BSM L1201「不得伪造归还扫码或实际归还时间」「实际归还时间取借出方收货扫码的时刻」
     * 「结束对应数量的原物归还事项」；台账 §25「归还后，保管责任回到出借方并关闭借用关系」：
     * 只由借出方商户账号（平台管理员除外）在收货扫码时按行登记归还数量与码，可分多次；有码行逐件扫码且只能是本行借出、尚未归还、
     * 未登记无法归还的码；每码保管主体翻回借出方、在借单置空并记一行保管台账（changedAt 即实际归还时间）；
     * 全部数量都已归还或实物处置完成才置 returned，否则保持 received。所有权始终不动。
     */
    public InventoryTransferOrderRecord returnTransferOrder(String orderId,
            com.complex.platform.inventory.dto.ReturnTransferRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite("returnTransferOrder:" + id, request == null ? null : request.idempotencyKey(), request,
                InventoryTransferOrderRecord.class, "inventory.transfer_order_returned",
                "inventory.transfer_order_return_failed", "inventory.transfer_order.manage", "inventory_transfer_order",
                () -> {
                    InventoryTransferOrderRecord o = repository.findTransferOrderById(id).orElseThrow(() ->
                            ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
                    if (!isBorrow(o)) {
                        throw ApiException.badRequest("TRANSFER_NOT_RETURNABLE",
                                "只有已借出的借调单才能归还");
                    }
                    if (!"received".equals(o.status())) {
                        throw ApiException.conflict("TRANSFER_ORDER_NOT_RECEIVED", "只有已收货的调拨单才能退回");
                    }
                    String merchant = authorizationService.currentMerchantIdQuietly().orElse(null);
                    if (merchant == null ? !authorizationService.isPlatformAdmin(authorizationService.currentActor()) : !merchant.equals(o.fromOwnerId())) {
                        throw ApiException.forbidden("LENDER_RETURN_ONLY", "归还须由借出方实际收货扫码登记");
                    }
                    com.complex.platform.inventory.dto.ReturnTransferRequest actual = requireRequest(request);
                    List<com.complex.platform.inventory.dto.ReturnTransferRequest.ReturnTransferLineRequest> lines =
                            actual.lines() == null ? List.of() : actual.lines();
                    if (lines.stream().noneMatch(l -> l.returnedQuantity() != null && l.returnedQuantity() > 0)) {
                        throw ApiException.badRequest("TRANSFER_RETURN_REQUIRES_SCAN", "归还须按实物扫码/数量登记");
                    }
                    Map<String, InventoryTransferLineRecord> byId = new java.util.LinkedHashMap<>();
                    for (InventoryTransferLineRecord l : repository.listTransferLinesByOrder(o.id())) {
                        byId.put(l.id(), l);
                    }
                    InventoryLendingFacts facts = repository.lendingFacts(o.id());
                    Instant now = Instant.now();
                    String actor = authorizationService.currentActor().id();
                    String returnNo = "trt_" + com.complex.platform.common.Hashing.idempotencyCode(
                            o.id() + ":" + requireText(actual.idempotencyKey(), "idempotency_key"));
                    Map<String, Integer> returnedNow = new java.util.HashMap<>();
                    int seq = 0;
                    for (com.complex.platform.inventory.dto.ReturnTransferRequest.ReturnTransferLineRequest in : lines) {
                        InventoryTransferLineRecord line = byId.get(in.lineId());
                        if (line == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "line_id 须为本单明细");
                        }
                        int qty = in.returnedQuantity() == null ? 0 : in.returnedQuantity();
                        if (qty <= 0) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "归还数量须大于 0");
                        }
                        if (qty > facts.outstanding(line) - returnedNow.getOrDefault(line.id(), 0)) {
                            throw ApiException.conflict("TRANSFER_RETURN_EXCEEDS_OUTSTANDING", "归还数量超过该行尚未归还的数量");
                        }
                        List<String> codes = in.traceCodes() == null ? List.of() : in.traceCodes();
                        boolean codesOk = line.traceCodes().isEmpty()
                                ? codes.isEmpty()
                                : codes.size() == qty && new java.util.HashSet<>(codes).size() == codes.size()
                                        && line.traceCodes().containsAll(codes)
                                        && codes.stream().noneMatch(c -> facts.returnedCodes().contains(c) || facts.claimedCodes().contains(c));
                        if (!codesOk) {
                            throw ApiException.badRequest("TRACE_CODE_MISMATCH", "归还须逐件扫码，且只能是本行借出、尚未归还的溯源码");
                        }
                        for (String code : codes) {
                            // 保管主体翻回借出方、在借单置空；所有权始终是借出方（CAS 命中 0 且码存在＝所有权已不是借出方→拒并回滚）。
                            int affected = repository.updateTraceCodeCustodianIfOwnedBy(code, o.fromOwnerId(), o.fromOwnerType(), o.fromOwnerId(), null, now);
                            if (affected == 0 && repository.traceCodeExists(code)) {
                                throw ApiException.badRequest("TRACE_OWNER_MISMATCH", "溯源码所有权已不是借出方，不能归还：" + code);
                            }
                            repository.saveTraceOwnershipHistory(new InventoryTraceOwnershipHistoryRecord(
                                    returnNo + "_h" + (seq++), code, o.toOwnerType(), o.toOwnerId(),
                                    o.fromOwnerType(), o.fromOwnerId(), o.id(), now, InventoryTraceOwnershipHistoryRecord.CUSTODY));
                        }
                        repository.saveTransferReturn(new InventoryTransferReturnRecord(returnNo + "_" + line.id(), returnNo, o.id(),
                                line.id(), qty, codes, actor, now));
                        returnedNow.merge(line.id(), qty, Integer::sum);
                    }
                    boolean complete = byId.values().stream().allMatch(line ->
                            facts.returned(line.id()) + returnedNow.getOrDefault(line.id(), 0) + facts.closed(line.id()) >= line.quantity());
                    if (complete) {
                        transferFlow.recordEvent(o.id(), "received", "returned", trimToNull(actual.operatorNote()));
                    }
                    return repository.saveTransferOrder(withTransferStatus(o, complete ? "returned" : "received"));
                });
    }

    /**
     * 取消流转单(AUD-002/003)：新建的借调单直接取消；门店要货见 InventoryTransferFlowService.cancel。
     * FS-011（BSM L1212「已出库数量须实际退回验收，或经复核完成损失处理，不能仅点取消就回库」）：已发货一律不能直接取消。
     */
    public InventoryTransferOrderRecord cancelTransferOrder(String orderId,
            com.complex.platform.inventory.dto.ReceiveTransferRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite("cancelTransferOrder:" + id, request == null ? null : request.idempotencyKey(), request,
                InventoryTransferOrderRecord.class, "inventory.transfer_order_cancelled",
                "inventory.transfer_order_cancel_failed", "inventory.transfer_order.manage", "inventory_transfer_order",
                () -> {
                    InventoryTransferOrderRecord o = repository.findTransferOrderById(id).orElseThrow(() ->
                            ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
                    if ("shipped".equals(o.status())) {
                        throw ApiException.conflict(InventoryTransferFlowService.SHIPPED_NOT_CANCELLABLE,
                                "已出库的单不能直接取消，须实际退回验收或经复核完成损失处理");
                    }
                    // FS-011：门店要货草稿自行取消、已提交未出库经审批取消。
                    if ("supply".equals(o.transferType())) {
                        return transferFlow.cancel(o, request);
                    }
                    if (!"created".equals(o.status())) {
                        throw ApiException.conflict("TRANSFER_ORDER_NOT_CANCELLABLE", "只有新建的借调单才能取消");
                    }
                    return repository.saveTransferOrder(withTransferStatus(o, "cancelled"));
                });
    }

    public InventoryTransferOrderRecord getTransferOrder(String orderId) {
        authorizationService.requirePermission("inventory.transfer_order.view");
        InventoryTransferOrderRecord o = repository.findTransferOrderById(requireText(orderId, "order_id")).orElseThrow(() ->
                ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
        return new InventoryTransferOrderRecord(o.id(), o.transferType(), o.lendingMode(), o.fromOwnerType(), o.fromOwnerId(),
                o.toOwnerType(), o.toOwnerId(), o.fromLocationId(), o.toLocationId(), o.status(), o.operatorNote(),
                o.createdBy(), o.createdAt(), o.updatedAt(), o.returnDueAt(), repository.listTransferLinesByOrder(o.id()));
    }

    public PageResponse<InventoryTransferOrderRecord> listTransferOrders() {
        authorizationService.requirePermission("inventory.transfer_order.view");
        List<InventoryTransferOrderRecord> rows = repository.listTransferOrders();
        return new PageResponse<>(rows, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, rows.size());
    }

    /**
     * FS-011（台账 §25「调拨买断或正式供货销售在接收方验收后转移所有权，并同步生成应收应付」）：借调买断在接收方收货后
     * 按货款(Σ 数量×单价)生成"收货方应付出让方"应付项（原为发货即挂）；借用型不计费、金额为 0 不记。
     * 门店要货按合格数量形成货款，见 InventoryTransferFlowService。
     */
    private void recordLendingSettlementOnReceipt(InventoryTransferOrderRecord o, Instant now) {
        if (!("lending".equals(o.transferType()) && "purchase".equals(o.lendingMode())) || financeRepository == null) {
            return;
        }
        long amountCent = 0L;
        for (InventoryTransferLineRecord l : repository.listTransferLinesByOrder(o.id())) {
            amountCent = Math.addExact(amountCent, Math.multiplyExact((long) l.quantity(), l.unitPriceCent()));
        }
        if (amountCent <= 0L) {
            return;
        }
        String itemId = "arap_trf_" + o.id();
        financeRepository.appendArApItem(new com.complex.platform.finance.ArApItemRecord(
                itemId, "ARAP-TRF-" + o.id(), "payable",
                o.fromOwnerId(), "inventory_transfer", o.id(),
                amountCent, 0L, amountCent, "CNY",
                days().merchantBusinessDate(null, o.toOwnerId(), now), null, "pending_payment",
                now, now, o.toOwnerId()));
        financeRepository.saveArApItemSource(new com.complex.platform.finance.ArApItemSourceRecord(
                "arap_src_trf_" + o.id(), itemId, "inventory_transfer", o.id(), amountCent, now));
    }

    private InventoryTransferOrderRecord withTransferStatus(InventoryTransferOrderRecord o, String status) {
        return new InventoryTransferOrderRecord(o.id(), o.transferType(), o.lendingMode(), o.fromOwnerType(), o.fromOwnerId(),
                o.toOwnerType(), o.toOwnerId(), o.fromLocationId(), o.toLocationId(), status, o.operatorNote(),
                o.createdBy(), o.createdAt(), Instant.now(), o.returnDueAt(), o.lines());
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.supplier.manage")
    @Transactional
    public InventorySupplierRecord createInventorySupplier(InventorySupplierRequest request) {
        return executeWrite(
                "createInventorySupplier",
                request == null ? null : request.idempotencyKey(),
                request,
                InventorySupplierRecord.class,
                "inventory.supplier_changed",
                "inventory.supplier_change_failed",
                "inventory.supplier.manage",
                "inventory_supplier",
                () -> {
                    InventorySupplierRequest actual = requireRequest(request);
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    return repository.saveInventorySupplier(new InventorySupplierRecord(
                            "supplier_" + slug(actual.idempotencyKey()),
                            requireText(actual.supplierName(), "supplier_name"),
                            "general",
                            trimToNull(actual.contactName()),
                            trimToNull(actual.contactPhone()),
                            trimToDefault(actual.status(), "active"),
                            now,
                            now,
                            merchantId
                    ));
                }
        );
    }

    /**
     * R5 T17 兼容入口：旧 scan-handover 端点不再写死 handover_result='accepted'。
     * 现内部走新分段流程——建一次交接任务（表头 in_progress + 预置五段 steps pending），
     * 返回的 handover_result 标记为 'chain_started'，真实可追责状态由 overall_status/steps 承载。
     */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.scan")
    public TraceHandoverRecord scanTraceCodeHandover(VerificationRequest request) {
        return executeWrite(
                "scanTraceCodeHandover",
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverRecord.class,
                "operation_handover.created",
                "operation_handover.failed",
                "inventory.handover.scan",
                "inventory_trace_handover",
                () -> {
                    VerificationRequest actual = requireRequest(request);
                    Map<String, Object> context = actual.businessContext() == null ? Map.of() : actual.businessContext();
                    String handoverId = "trace_" + slug(actual.idempotencyKey());
                    String traceCode = requireText(actual.credentialPayload(), "credential_payload");
                    String deviceId = requireText(actual.deviceId(), "device_id");
                    String itemId = trimToNull(textFromMap(context, "item_id", "itemId"));
                    String fromStaffId = trimToNull(textFromMap(context, "from_staff_id", "fromStaffId"));
                    String toStaffId = trimToNull(textFromMap(context, "to_staff_id", "toStaffId"));
                    return createHandoverChain(handoverId, traceCode, itemId, deviceId, fromStaffId, toStaffId, "chain_started");
                }
        );
    }

    /** R5 T17：建交接任务——表头 overall_status=in_progress + 预置五段 steps(pending, seq 1..5)。 */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.scan")
    public TraceHandoverChainView createTraceHandover(TraceHandoverCreateRequest request) {
        return executeWrite(
                "createTraceHandover",
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverChainView.class,
                "inventory.handover_created",
                "inventory.handover_create_failed",
                "inventory.handover.scan",
                "inventory_trace_handover",
                () -> {
                    TraceHandoverCreateRequest actual = requireRequest(request);
                    String handoverId = "trace_" + slug(actual.idempotencyKey());
                    String traceCode = requireText(actual.traceCode(), "trace_code");
                    String deviceId = requireText(actual.deviceId(), "device_id");
                    String itemId = trimToNull(actual.itemId());
                    TraceHandoverRecord header =
                            createHandoverChain(handoverId, traceCode, itemId, deviceId, null, null, "chain_started");
                    return chainView(header.id());
                }
        );
    }

    /** R5 T17：员工段扫码（主动留痕）。同段重复→duplicate 异常；全段 done→overall completed。 */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.scan")
    public TraceHandoverChainView scanTraceSegment(String handoverId, TraceScanSegmentRequest request) {
        String id = requireText(handoverId, "handover_id");
        return executeWrite(
                "scanTraceSegment:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverChainView.class,
                "inventory.handover_segment_scanned",
                "inventory.handover_segment_scan_failed",
                "inventory.handover.scan",
                "inventory_trace_handover_step",
                () -> {
                    TraceScanSegmentRequest actual = requireRequest(request);
                    String segment = requireSegment(actual.segment());
                    if (ROBOT_SEGMENT.equals(segment)) {
                        throw ApiException.badRequest("HANDOVER_SEGMENT_NOT_STAFF",
                                "该配送段由机器人被动执行，请使用机器人上报接口");
                    }
                    TraceHandoverRecord header = requireHandover(id);
                    enforceHandoverOwnership(header);
                    TraceHandoverStepRecord step = requireStep(id, segment);
                    String operator = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    if ("done".equals(step.stepStatus())) {
                        // 同段重复扫码 → duplicate 异常 + overall exception。
                        markStep(step, operator, null, "duplicate", now, "duplicate scan on segment " + segment);
                        raiseException(id, segment, "duplicate", "duplicate scan on segment " + segment, now);
                        return chainView(id);
                    }
                    markStep(step, operator, null, "done", now, null);
                    completeOrAdvance(header, segment, now);
                    return chainView(id);
                }
        );
    }

    /** R5 T17：机器人段(dispatch)上报（被动留痕），受 T14 integration.robot.sync 开关门控 + 写 T14 事件。 */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.scan")
    public TraceHandoverChainView reportRobotSegment(String handoverId, TraceRobotReportRequest request) {
        String id = requireText(handoverId, "handover_id");
        return executeWrite(
                "reportRobotSegment:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverChainView.class,
                "inventory.handover_segment_scanned",
                "inventory.handover_segment_scan_failed",
                "inventory.handover.scan",
                "inventory_trace_handover_step",
                () -> {
                    TraceRobotReportRequest actual = requireRequest(request);
                    // 受 T14 第三方开关门控（编程式）：关闭则 reject 留痕、不放行被动留痕。
                    terminalSwitchGuard.check(ROBOT_SYNC_CAPABILITY);
                    String robotTaskRef = requireText(actual.robotTaskRef(), "robot_task_ref");
                    String status = trimToDefault(actual.status(), "delivered");
                    TraceHandoverRecord header = requireHandover(id);
                    enforceHandoverOwnership(header);
                    TraceHandoverStepRecord step = requireStep(id, ROBOT_SEGMENT);
                    Instant now = Instant.now();
                    // T14 关联：机器人段被动留痕写 integration_robot_task_events(handover_id)。
                    repository.saveRobotTaskEvent(
                            "robotevt_" + slug(actual.idempotencyKey()),
                            robotTaskRef,
                            "delivered".equals(status) ? "delivered" : "failed",
                            id,
                            robotEventPayload(status),
                            now);
                    if ("done".equals(step.stepStatus())) {
                        markStep(step, null, robotTaskRef, "duplicate", now, "duplicate robot report on dispatch");
                        raiseException(id, ROBOT_SEGMENT, "duplicate", "duplicate robot report on dispatch", now);
                        return chainView(id);
                    }
                    if ("delivered_no_receiver".equals(status)) {
                        // 机器人上报送达但无人工接收扫码 → gap 异常进复核。
                        markStep(step, null, robotTaskRef, "gap", now, "robot delivered but no staff receipt");
                        raiseException(id, ROBOT_SEGMENT, "gap", "robot delivered but no staff receipt", now);
                        return chainView(id);
                    }
                    boolean diff = Boolean.TRUE.equals(actual.reportDiff()) || !"delivered".equals(status);
                    if (diff) {
                        // 机器人上报与人工预期不符（点位/物料/状态）→ diff 异常进复核。
                        markStep(step, null, robotTaskRef, "diff", now, "robot report diff: status=" + status);
                        raiseException(id, ROBOT_SEGMENT, "diff", "robot report diff: status=" + status, now);
                        return chainView(id);
                    }
                    markStep(step, null, robotTaskRef, "done", now, null);
                    completeOrAdvance(header, ROBOT_SEGMENT, now);
                    return chainView(id);
                }
        );
    }

    /** R5 T17：异常复核 approve/reject 闭合 + 留结论；approve 时把该段标 gap-closed 不阻塞，reject 维持 exception。 */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.review")
    public TraceHandoverExceptionRecord reviewException(String exceptionId, TraceExceptionReviewRequest request) {
        String id = requireText(exceptionId, "exception_id");
        return executeWrite(
                "reviewTraceException:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverExceptionRecord.class,
                "inventory.handover_exception_resolved",
                "inventory.handover_exception_resolve_failed",
                "inventory.handover.review",
                "inventory_trace_handover_exception",
                () -> {
                    TraceExceptionReviewRequest actual = requireRequest(request);
                    String decision = requireDecision(actual.decision());
                    TraceHandoverExceptionRecord exception = repository.findTraceHandoverExceptionByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound(
                                    "HANDOVER_EXCEPTION_NOT_FOUND", "交接异常不存在或已被删除"));
                    // 2e 溯源归属：复核异常须校验所属 handover 归属（防携他人 exceptionId 跨租户复核、篡改责任链结论）。
                    enforceHandoverOwnership(requireHandover(exception.handoverId()));
                    if (!"pending".equals(exception.reviewStatus())) {
                        throw ApiException.conflict("HANDOVER_EXCEPTION_NOT_PENDING",
                                "该交接异常不在待处理状态");
                    }
                    String reviewer = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    TraceHandoverExceptionRecord resolved = new TraceHandoverExceptionRecord(
                            exception.id(), exception.handoverId(), exception.segment(), exception.exceptionType(),
                            decision, reviewer, trimToNull(actual.reason()), exception.createdAt(), now);
                    repository.saveTraceHandoverException(resolved);
                    closeExceptionOnHeader(exception.handoverId(), now);
                    return resolved;
                }
        );
    }

    private TraceHandoverRecord createHandoverChain(
            String handoverId, String traceCode, String itemId, String deviceId,
            String fromStaffId, String toStaffId, String handoverResult) {
        Instant now = Instant.now();
        // 2e 溯源归属：创建交接链时记录归属商户（currentMerchantScope），供三方法跨租户校验。
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        TraceHandoverRecord header = repository.saveTraceHandover(new TraceHandoverRecord(
                handoverId, traceCode, itemId, fromStaffId, toStaffId, deviceId,
                handoverResult, "in_progress", HANDOVER_SEGMENTS.getFirst(), List.of(), now, merchantId));
        for (int i = 0; i < HANDOVER_SEGMENTS.size(); i++) {
            String segment = HANDOVER_SEGMENTS.get(i);
            boolean robot = ROBOT_SEGMENT.equals(segment);
            repository.saveTraceHandoverStep(new TraceHandoverStepRecord(
                    handoverId + "_" + segment,
                    handoverId,
                    segment,
                    i + 1,
                    robot ? "robot" : "staff",
                    null,
                    null,
                    null,
                    "pending",
                    null,
                    now));
        }
        // 三层留痕接线（REQ-34/37）：供应链五段交接=一条真实业务流程，建过程实例（process_type=inventory_delivery）。
        // handoverId 复用为 process_instance_id（1:1），merchant 由 token 注入（TraceabilityService 内部覆盖）。
        traceabilityService.recordProcessInstance(new TraceabilityProcessInstanceRecord(
                handoverId, "inventory_delivery", "inventory_item",
                itemId == null ? handoverId : itemId, null, null, null, null,
                null, null, HANDOVER_SEGMENTS.getFirst(), "running", now, null));
        return header;
    }

    private void markStep(
            TraceHandoverStepRecord step, String operatorUserId, String robotTaskRef,
            String status, Instant scanAt, String note) {
        repository.saveTraceHandoverStep(new TraceHandoverStepRecord(
                step.id(), step.handoverId(), step.segment(), step.seq(), step.actorType(),
                operatorUserId != null ? operatorUserId : step.operatorUserId(),
                robotTaskRef != null ? robotTaskRef : step.robotTaskRef(),
                scanAt, status, note, step.createdAt()));
    }

    private void raiseException(String handoverId, String segment, String type, String reason, Instant now) {
        String exceptionId = "traceexc_" + slug(handoverId + ":" + segment + ":" + type + ":" + now.toEpochMilli());
        repository.saveTraceHandoverException(new TraceHandoverExceptionRecord(
                exceptionId,
                handoverId, segment, type, "pending", null, reason, now, null));
        TraceHandoverRecord header = requireHandover(handoverId);
        repository.saveTraceHandover(withOverall(header, "exception", segment));
        // P1：落孤岛表 inventory_trace_handover_exceptions 后，双写统一 risk_alerts（汇聚镜像，fail-safe）。
        reportTraceHandoverExceptionToUnified(exceptionId, handoverId, segment, type, reason);
    }

    /**
     * P1：库存风险告警双写统一异常责任链 risk_alerts（evidence/RiskAlertService.report）。
     *
     * <p>source_domain='inventory'，alertType 沿用本域值，risk_level 经映射（medium→suspicious，high→high），
     * source_ref=该 risk alert 的 id，幂等键稳定唯一（id + ":unified"），重复扫描不重复上报。
     *
     * <p>fail-safe：未注入服务（旧构造器/单测）或上报异常都不阻塞扫描主链——
     * 孤岛 inventory_risk_alerts 仍是命中落地的权威，统一表为汇聚镜像。
     */
    private void reportRiskAlertToUnified(InventoryRiskAlertRecord alert) {
        if (riskAlertService == null) {
            return;
        }
        // 未知风险级映射不到统一枚举则按 high 兜底（避免 report 抛 VALIDATION_FAILED 静默丢数据）。
        String unifiedLevel = RISK_LEVEL_TO_UNIFIED.getOrDefault(alert.severity(), "high");
        try {
            riskAlertService.report(new RiskAlertReportRequest(
                    alert.id() + ":unified",
                    "inventory",
                    alert.id(),
                    alert.alertType(),
                    unifiedLevel,
                    "Inventory risk: " + alert.alertType() + " on item " + alert.itemId(),
                    null,
                    "alert_type=" + alert.alertType() + "; item=" + alert.itemId()
                            + "; location=" + alert.locationId(),
                    List.of()));
        } catch (RuntimeException exception) {
            // 统一表上报失败不影响扫描主链（命中已落孤岛 inventory_risk_alerts）。
            auditEventService.recordFailure(
                    "inventory.risk.unified_alert_failed",
                    "inventory.risk.scan",
                    "risk_alert",
                    alert.id(),
                    null,
                    alert.id() + ":unified",
                    codeOf(exception));
        }
    }

    /**
     * P1：溯源交接断链异常双写统一异常责任链 risk_alerts。
     *
     * <p>source_domain='inventory'，alertType='trace_handover_exception'，risk_level='high'，
     * source_ref=异常 id，幂等键稳定唯一（exceptionId + ":unified"）。fail-safe 同上：
     * 孤岛 inventory_trace_handover_exceptions 仍权威，统一表为汇聚镜像。
     */
    private void reportTraceHandoverExceptionToUnified(
            String exceptionId, String handoverId, String segment, String type, String reason) {
        if (riskAlertService == null) {
            return;
        }
        try {
            riskAlertService.report(new RiskAlertReportRequest(
                    exceptionId + ":unified",
                    "inventory",
                    exceptionId,
                    "trace_handover_exception",
                    "high",
                    "Trace handover exception: " + type + " on segment " + segment,
                    null,
                    "handover=" + handoverId + "; segment=" + segment + "; type=" + type
                            + "; reason=" + reason,
                    List.of()));
        } catch (RuntimeException exception) {
            // 统一表上报失败不影响交接主链（异常已落孤岛 inventory_trace_handover_exceptions）。
            auditEventService.recordFailure(
                    "inventory.trace.unified_alert_failed",
                    "inventory.handover.scan",
                    "risk_alert",
                    exceptionId,
                    null,
                    exceptionId + ":unified",
                    codeOf(exception));
        }
    }

    /** 一段 done 后：若五段全 done → overall completed + 事件；否则推进 current_segment 到下一未完成段。 */
    private void completeOrAdvance(TraceHandoverRecord header, String segment, Instant now) {
        List<TraceHandoverStepRecord> steps = repository.listTraceHandoverSteps(header.id());
        boolean allDone = steps.stream().allMatch(s -> "done".equals(s.stepStatus()));
        if (allDone) {
            repository.saveTraceHandover(withOverall(header, "completed", segment));
            // 三层留痕：本段步骤入库 + 过程实例收口（completed）。
            recordTraceStep(header, segment, now, "completed");
            auditEventService.recordSuccess(
                    "inventory.handover_completed", "inventory.handover.scan",
                    "inventory_trace_handover", header.id(), null, null,
                    Map.of("handover_id", header.id()));
            return;
        }
        String next = steps.stream()
                .filter(s -> "pending".equals(s.stepStatus()))
                .map(TraceHandoverStepRecord::segment)
                .findFirst()
                .orElse(segment);
        // 仅当当前非 exception 时维持 in_progress 推进；exception 不被一段 done 覆盖。
        if (!"exception".equals(header.overallStatus())) {
            repository.saveTraceHandover(withOverall(header, "in_progress", next));
        }
        // 三层留痕：本段步骤入库 + 过程实例当前步推进（running）。
        recordTraceStep(header, segment, now, next);
    }

    /**
     * 三层留痕步骤接线（REQ-34/37）：某段 done 时写过程步骤记录 + 推进过程实例 current_step。
     *
     * <p>actor/capture_source 区分人工扫码段（staff/qr_scan）与机器人段（robot/robot_callback）；
     * 机器人段被动留痕，requiresManualReason 维持 false。instanceStatus 为新的过程实例状态。
     */
    private void recordTraceStep(TraceHandoverRecord header, String segment, Instant now, String nextStepCode) {
        boolean robot = ROBOT_SEGMENT.equals(segment);
        String actorId = robot ? null : authorizationService.currentActor().id();
        traceabilityService.recordStep(new TraceabilityStepRecord(
                header.id() + "_step_" + segment,
                header.id(),
                segment,
                segment,
                robot ? "robot" : "staff",
                actorId,
                robot ? null : "tablet",
                header.deviceId(),
                null,
                null,
                true,
                robot ? "robot_callback" : "qr_scan",
                false,
                "success",
                now));
        boolean completed = "completed".equals(nextStepCode);
        traceabilityService.recordProcessInstance(new TraceabilityProcessInstanceRecord(
                header.id(), "inventory_delivery", "inventory_item",
                header.itemId() == null ? header.id() : header.itemId(), null, null, null, null,
                null, null, completed ? segment : nextStepCode,
                completed ? "completed" : "running", null, completed ? now : null));
    }

    /** 复核闭合后：若该交接已无 pending 异常，则按 steps 真实状态回算 overall。 */
    private void closeExceptionOnHeader(String handoverId, Instant now) {
        TraceHandoverRecord header = requireHandover(handoverId);
        List<TraceHandoverStepRecord> steps = repository.listTraceHandoverSteps(handoverId);
        boolean allDone = steps.stream().allMatch(s -> "done".equals(s.stepStatus()));
        String next = steps.stream()
                .filter(s -> "pending".equals(s.stepStatus()))
                .map(TraceHandoverStepRecord::segment)
                .findFirst()
                .orElse(header.currentSegment());
        repository.saveTraceHandover(withOverall(header, allDone ? "completed" : "in_progress", next));
    }

    private TraceHandoverRecord withOverall(TraceHandoverRecord h, String overall, String currentSegment) {
        TraceHandoverRecord updated = new TraceHandoverRecord(
                h.id(), h.traceCode(), h.itemId(), h.fromStaffId(), h.toStaffId(), h.deviceId(),
                h.handoverResult(), overall, currentSegment, h.evidenceAttachmentIds(), h.createdAt(), h.merchantId());
        if ("exception".equals(overall)) {
            auditEventService.recordSuccess(
                    "inventory.handover_exception_raised", "inventory.handover.scan",
                    "inventory_trace_handover", h.id(), null, null,
                    Map.of("handover_id", h.id(), "segment", currentSegment == null ? "" : currentSegment));
        }
        return updated;
    }

    // 2e 溯源归属：交接三方法（scan/report/review）按 currentMerchantScope 校验 handover 归属，防跨租户篡改责任链。
    private void enforceHandoverOwnership(TraceHandoverRecord header) {
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(header.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                        "无权操作其他商户的交接记录");
            }
        });
    }

    private TraceHandoverChainView chainView(String handoverId) {
        TraceHandoverRecord header = requireHandover(handoverId);
        return new TraceHandoverChainView(
                header,
                repository.listTraceHandoverSteps(handoverId),
                List.of());
    }

    private TraceHandoverRecord requireHandover(String handoverId) {
        return repository.findTraceHandoverByIdForUpdate(handoverId)
                .orElseThrow(() -> ApiException.notFound("HANDOVER_NOT_FOUND", "交接记录不存在或已被删除"));
    }

    private TraceHandoverStepRecord requireStep(String handoverId, String segment) {
        return repository.findTraceHandoverStep(handoverId, segment)
                .orElseThrow(() -> ApiException.notFound("HANDOVER_STEP_NOT_FOUND", "交接环节不存在或已被删除"));
    }

    private String requireSegment(String segment) {
        String value = requireText(segment, "segment");
        if (!HANDOVER_SEGMENTS.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "segment is invalid");
        }
        return value;
    }

    private String requireDecision(String decision) {
        String value = requireText(decision, "decision").toLowerCase(Locale.ROOT);
        if (!Set.of("approved", "rejected").contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
        }
        return value;
    }

    private String robotEventPayload(String status) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(Map.of("status", status, "source", "trace_handover_chain"));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return "{}";
        }
    }

    private InventoryStockBatchRecord createStockBatchOnce(InventoryStockBatchRequest request) {
        InventoryStockBatchRequest actual = requireRequest(request);
        String itemId = requireText(actual.itemId(), "item_id");
        InventoryItemRecord ownedItem = repository.findInventoryItemById(itemId)
                .orElseThrow(() -> ApiException.notFound("INVENTORY_ITEM_NOT_FOUND", "库存商品不存在或已被删除"));
        // C1：建批次前校验物料归属当前商户，防跨商户对他人物料建批次。
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(ownedItem.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                        "该库存商品不属于当前商户");
            }
        });
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveStockBatch(new InventoryStockBatchRecord(
                "batch_" + slug(actual.idempotencyKey()),
                itemId,
                requireText(actual.locationId(), "location_id"),
                requireText(actual.batchNo(), "batch_no"),
                trimToNull(actual.supplierId()),
                requirePositiveInteger(actual.quantity(), "quantity"),
                actual.expireAt(),
                normalizeAllowed(trimToDefault(actual.status(), "active"), STOCK_BATCH_STATUSES, "status"),
                safeList(actual.evidenceAttachmentIds()),
                now,
                now,
                merchantId
        ), authorizationService.currentProjectScopeQuietly().orElse(null));
    }

    private InventoryRiskScanResult scanInventoryRisks(InventoryRiskScanRequest request) {
        InventoryRiskScanRequest actual = requireRequest(request);
        requireText(actual.idempotencyKey(), "idempotency_key");
        // P1-5: operatorUserId 一律取自认证上下文，禁止来自请求体
        String operatorUserId = authorizationService.currentActor().id();
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        int expiryWarningDays = expiryWarningDays(actual.expiryWarningDays());
        Instant now = Instant.now();
        Instant expiryCutoff = now.plusSeconds(expiryWarningDays * 86_400L);

        // 使用租户隔离的列表
        List<InventoryItemRecord> items = merchantId != null
                ? repository.listInventoryItemsByMerchant(merchantId)
                : repository.listInventoryItems();
        List<StockBalanceRecord> balances = merchantId != null
                ? repository.listStockBalancesByMerchant(merchantId)
                : repository.listStockBalances();
        List<InventoryStockBatchRecord> batches = merchantId != null
                ? repository.listStockBatchesByMerchant(merchantId)
                : repository.listStockBatches();

        Map<String, InventoryItemRecord> itemsById = items.stream()
                .collect(java.util.stream.Collectors.toMap(InventoryItemRecord::id, item -> item, (left, right) -> left));
        java.util.ArrayList<InventoryRiskAlertRecord> alerts = new java.util.ArrayList<>();
        int index = 0;

        for (StockBalanceRecord balance : balances) {
            InventoryItemRecord item = itemsById.get(balance.itemId());
            int minQuantity = item == null ? 0 : item.minQuantity();
            if (balance.availableQuantity() <= 0) {
                alerts.add(riskAlert(actual.idempotencyKey(), index++, "out_of_stock", "high", balance.itemId(),
                        balance.locationId(), null, balance.availableQuantity(), minQuantity, null, operatorUserId, merchantId, now));
            } else if (minQuantity > 0 && balance.availableQuantity() < minQuantity) {
                alerts.add(riskAlert(actual.idempotencyKey(), index++, "safety_stock_low", "medium", balance.itemId(),
                        balance.locationId(), null, balance.availableQuantity(), minQuantity, null, operatorUserId, merchantId, now));
            }
        }

        for (InventoryStockBatchRecord batch : batches) {
            if (!"active".equals(batch.status()) || batch.expireAt() == null || batch.quantity() <= 0) {
                continue;
            }
            if (!batch.expireAt().isAfter(expiryCutoff)) {
                alerts.add(riskAlert(actual.idempotencyKey(), index++, "expiry_warning", "high", batch.itemId(),
                        batch.locationId(), batch.id(), batch.quantity(), null, batch.expireAt(), operatorUserId, merchantId, now));
            }
        }

        List<InventoryRiskAlertRecord> saved = alerts.stream()
                .map(repository::saveRiskAlert)
                .toList();
        // P1：落孤岛表 inventory_risk_alerts 后，双写统一 risk_alerts（汇聚镜像，fail-safe）。
        saved.forEach(this::reportRiskAlertToUnified);
        return new InventoryRiskScanResult(saved.size(), saved);
    }

    private InventoryRiskAlertRecord riskAlert(
            String idempotencyKey,
            int index,
            String alertType,
            String severity,
            String itemId,
            String locationId,
            String batchId,
            Integer availableQuantity,
            Integer thresholdQuantity,
            Instant expireAt,
            String operatorUserId,
            String merchantId,
            Instant now
    ) {
        return new InventoryRiskAlertRecord(
                "risk_" + slug(idempotencyKey + ":" + index),
                alertType,
                severity,
                itemId,
                locationId,
                batchId,
                availableQuantity,
                thresholdQuantity,
                expireAt,
                "open",
                operatorUserId,
                now,
                merchantId
        );
    }

    private SupplierReconciliationRecord createSupplierReconciliationOnce(SupplierReconciliationRequest request) {
        SupplierReconciliationRequest actual = requireRequest(request);
        String supplierId = requireText(actual.supplierId(), "supplier_id");
        boolean supplierExists = repository.listInventorySuppliers().stream()
                .anyMatch(supplier -> supplier.id().equals(supplierId));
        if (!supplierExists) {
            throw ApiException.notFound("SUPPLIER_NOT_FOUND", "供应商不存在或已被删除");
        }
        LocalDate periodStart = requireDate(actual.periodStart(), "period_start");
        LocalDate periodEnd = requireDate(actual.periodEnd(), "period_end");
        if (periodEnd.isBefore(periodStart)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "期间结束日期不能早于开始日期");
        }
        long expectedAmountCent = requireNonNegativeLong(actual.expectedAmountCent(), "expected_amount_cent");
        long actualAmountCent = requireNonNegativeLong(actual.actualAmountCent(), "actual_amount_cent");
        // P1-5: operatorUserId 一律取自认证上下文，禁止来自请求体
        String operatorUserId = authorizationService.currentActor().id();
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveSupplierReconciliation(new SupplierReconciliationRecord(
                "supplier_recon_" + slug(actual.idempotencyKey()),
                supplierId,
                periodStart,
                periodEnd,
                expectedAmountCent,
                actualAmountCent,
                actualAmountCent - expectedAmountCent,
                normalizeAllowed(trimToDefault(actual.status(), "pending"), SUPPLIER_RECONCILIATION_STATUSES, "status"),
                safeList(actual.evidenceAttachmentIds()),
                operatorUserId,
                now,
                now,
                merchantId
        ));
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> supplier
    ) {
        return readPage(successEvent, failureEvent, permissionCode, businessType, Map.of(), supplier);
    }

    /** details：除 count 外要留痕的查询要素（如按手机号检索时的 phone_hash / queried_by）。 */
    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Map<String, Object> details,
            Supplier<List<T>> supplier
    ) {
        try {
            List<T> items = supplier.get();
            Map<String, Object> metadata = new LinkedHashMap<>(details);
            metadata.put("count", items.size());
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    metadata
            );
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
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
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T response = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(response),
                                null,
                                idempotencyKey,
                                Map.of("operation_id", operationId)
                        );
                        return response;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private String businessId(Object response) {
        if (response instanceof InventoryItemRecord record) {
            return record.id();
        }
        if (response instanceof InventorySupplierRecord record) {
            return record.id();
        }
        if (response instanceof PurchaseRequestRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleClaimRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleRenewalRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleTransferRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleRemedyRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleHandoverRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleExitNoticeResult result) {
            return result.merchantId();
        }
        if (response instanceof SavedBottleManualPickupRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleVerificationCodeReceipt record) {
            return record.savedBottleId();
        }
        if (response instanceof StockMovementRecord record) {
            return record.id();
        }
        if (response instanceof StockAdjustmentRecord record) {
            return record.id();
        }
        if (response instanceof InventoryStockBatchRecord record) {
            return record.id();
        }
        if (response instanceof InventoryRiskScanResult) {
            return "inventory_risk_scan";
        }
        if (response instanceof InventoryRiskAlertRecord record) {
            return record.id();
        }
        if (response instanceof SupplierReconciliationRecord record) {
            return record.id();
        }
        if (response instanceof TraceHandoverRecord record) {
            return record.id();
        }
        if (response instanceof TraceHandoverChainView view) {
            return view.handover().id();
        }
        if (response instanceof TraceHandoverExceptionRecord record) {
            return record.id();
        }
        if (response instanceof InventoryHighValueReviewRecord record) {
            return record.id();
        }
        if (response instanceof StocktakeRecord record) {
            return record.id();
        }
        if (response instanceof InventoryVarianceRecord record) {
            return record.id();
        }
        if (response instanceof InventoryTraceCodeRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private Map<String, Object> firstItem(List<Map<String, Object>> items, String fieldName) {
        return requireItems(items, fieldName).getFirst();
    }

    private List<Map<String, Object>> requireItems(List<Map<String, Object>> items, String fieldName) {
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return items;
    }

    private String requireItemText(Map<String, Object> item, String fieldName) {
        return requireText(textFromItem(item, fieldName), fieldName);
    }

    private String textFromItem(Map<String, Object> item, String fieldName) {
        return textFromMap(item, fieldName, snakeToCamel(fieldName));
    }

    private String textFromMap(Map<String, Object> map, String snakeName, String camelName) {
        Object value = map.get(snakeName);
        if (value == null) {
            value = map.get(camelName);
        }
        return value == null ? null : value.toString();
    }

    private int requirePositiveItemQuantity(Map<String, Object> item, String fieldName) {
        String raw = textFromItem(item, fieldName);
        if (raw == null || !raw.matches("[0-9]+")) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须为正整数");
        }
        try {
            int quantity = Integer.parseInt(raw);
            if (quantity <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须大于 0");
            }
            return quantity;
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 超出允许范围");
        }
    }

    private int nonNegativeOrDefault(Integer value, int defaultValue, String fieldName) {
        int actual = value == null ? defaultValue : value;
        if (actual < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为负数");
        }
        return actual;
    }

    /** 可空非负长整：null 透传（如高价值阈值 null=不视高价值），非空须 >= 0。 */
    private Long nonNegativeOrNull(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为负数");
        }
        return value;
    }

    private int requirePositiveInteger(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须大于 0");
        }
        return value;
    }

    private long requireNonNegativeLong(Long value, String fieldName) {
        if (value == null || value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为负数");
        }
        return value;
    }

    private LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private String normalizeAllowed(String value, Set<String> allowed, String fieldName) {
        String normalized = requireText(value, fieldName).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return normalized;
    }

    private int expiryWarningDays(Integer value) {
        int actual = value == null ? 7 : value;
        if (actual < 1 || actual > 365) {
            throw ApiException.badRequest("VALIDATION_FAILED", "临期预警天数必须在 1 到 365 之间");
        }
        return actual;
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String snakeToCamel(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '_') {
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(character));
                upperNext = false;
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private String slug(String seed) {
        return com.complex.platform.common.Hashing.idempotencyCode(requireText(seed, "idempotency_key"));
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }
}
