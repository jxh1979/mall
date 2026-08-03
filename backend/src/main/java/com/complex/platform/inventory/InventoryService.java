package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
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
public class InventoryService {

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
                            Boolean.TRUE.equals(actual.manualHighValue())
                    );
                    return repository.saveInventoryItem(item);
                }
        );
    }

    public PageResponse<PurchaseRequestRecord> listPurchaseRequests() {
        return readPage(
                "inventory.purchase_request_viewed",
                "inventory.purchase_request_view_failed",
                "inventory.purchase_request.view",
                "inventory_purchase_request",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listPurchaseRequestsByMerchant)
                                .orElseGet(repository::listPurchaseRequests);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.purchase_request.manage")
    @Transactional
    public PurchaseRequestRecord createPurchaseRequest(PurchaseRequestCreate request) {
        return executeWrite(
                "createPurchaseRequest",
                request == null ? null : request.idempotencyKey(),
                request,
                PurchaseRequestRecord.class,
                "inventory.purchase_request_changed",
                "inventory.purchase_request_change_failed",
                "inventory.purchase_request.manage",
                "inventory_purchase_request",
                () -> {
                    PurchaseRequestCreate actual = requireRequest(request);
                    Map<String, Object> item = firstItem(actual.items(), "items");
                    String itemId = requireItemText(item, "item_id");
                    int quantity = requirePositiveItemQuantity(item, "quantity");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    String slug = slug(actual.idempotencyKey());
                    return repository.savePurchaseRequest(new PurchaseRequestRecord(
                            "purchase_" + slug,
                            "PR" + now.toEpochMilli() + slug.substring(0, Math.min(6, slug.length())).toUpperCase(Locale.ROOT),
                            trimToNull(actual.supplierId()),
                            itemId,
                            quantity,
                            actual.expectedArrivalDate() == null ? LocalDate.now() : actual.expectedArrivalDate(),
                            // R5 T6：起点保留 'created'（兼容既有写入/测试），视为 submitted 同义起点；
                            // 审批流转入口 approve 从 created/submitted 均可进入。
                            "created",
                            safeList(actual.evidenceAttachmentIds()),
                            now,
                            now,
                            merchantId,
                            trimToNull(actual.supplyContractId()),
                            null,
                            null,
                            null,
                            null,
                            null
                    ));
                }
        );
    }

    public PageResponse<SavedBottleRecord> listSavedBottles() {
        return readPage(
                "bar.saved_bottle.viewed",
                "bar.saved_bottle.view_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
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
                            trimToNull(actual.customerPhone()),
                            phoneHash(actual.customerPhone()),
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
                b.transferredAt(), b.transferToNameOrNickname(), b.transferToPhone(), b.transferToPhoneHash());
    }

    public void processSavedBottleNotifications(Instant now, int expiringNoticeDays) {
        Instant actualNow = now == null ? Instant.now() : now;
        int noticeDays = Math.max(0, expiringNoticeDays);
        processExpiringSavedBottleNotifications(actualNow, noticeDays);
        processExpiredSavedBottleNotifications(actualNow);
    }

    private void processExpiringSavedBottleNotifications(Instant now, int noticeDays) {
        Instant windowEnd = now.plus(Duration.ofDays(noticeDays));
        List<SavedBottleRecord> bottles;
        try {
            bottles = repository.listStoredSavedBottlesExpiring(now, windowEnd);
        } catch (RuntimeException exception) {
            log.warn("failed to list expiring saved bottles for notification", exception);
            return;
        }
        for (SavedBottleRecord bottle : bottles) {
            notifySavedBottleNow(
                    bottle,
                    "expiring",
                    STORED_WINE_EXPIRING_TEMPLATE,
                    "swexpiring:" + bottle.id() + ":" + expireDateKey(bottle),
                    null);
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
        if (customerId != null) {
            if (customerRepository == null) {
                return null;
            }
            try {
                return customerRepository.findCustomerById(customerId)
                        .map(CustomerRecord::phone)
                        .map(Strings::trimToNull)
                        .orElse(null);
            } catch (RuntimeException exception) {
                log.warn("failed to resolve saved bottle customer phone: customer_id={}", customerId, exception);
                return null;
            }
        }
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
                    // 转让须经出让人(原持有人)手机短信验证码确认(用户 2026-07-08 裁决)。当前校验验证码必填;
                    // 与客人 OTP 的"发码+核验"绑定为后续(依赖短信服务商,同客人登录环境约束)。
                    requireText(actual.verificationCode(), "verification_code");
                    SavedBottleRecord bottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(bottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "该存酒记录不属于当前商户");
                    }
                    if (!"stored".equals(bottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_TRANSFERABLE", "该存酒不可转赠");
                    }
                    if (bottle.expireAt() != null && bottle.expireAt().isBefore(Instant.now())) {
                        throw ApiException.conflict("SAVED_BOTTLE_EXPIRED", "该存酒已过期");
                    }
                    int qty = actual.transferQuantity() == null ? bottle.remainingQuantity() : actual.transferQuantity();
                    if (qty <= 0 || qty > bottle.remainingQuantity()) {
                        throw ApiException.badRequest("VALIDATION_FAILED",
                                "转赠数量不能超过剩余数量");
                    }
                    Instant now = Instant.now();
                    String toPhone = trimToNull(actual.toPhone());
                    String toPhoneHash = phoneHash(toPhone);
                    // 记录转让目标到存酒(供后续新持有人取酒核身)。
                    repository.saveSavedBottle(new SavedBottleRecord(
                            bottle.id(), bottle.customerId(), bottle.barProjectId(), bottle.itemId(), bottle.bottleName(),
                            bottle.initialQuantity(), bottle.remainingQuantity(), bottle.unit(), bottle.expireAt(),
                            bottle.status(), bottle.evidenceAttachmentIds(), bottle.createdAt(), now, bottle.merchantId(),
                            bottle.isHighValue(), bottle.highValueThresholdCents(), bottle.bottleLabelValue(),
                            bottle.customerNameOrNickname(), bottle.customerPhone(), bottle.customerPhoneHash(),
                            bottle.registrationPhotoRefs(), bottle.manualNote(), now, toName, toPhone, toPhoneHash));
                    return repository.saveSavedBottleTransfer(new SavedBottleTransferRecord(
                            "transfer_" + slug(actual.idempotencyKey()),
                            bottle.id(), bottle.customerId(), bottle.customerNameOrNickname(),
                            toName, toPhone, toPhoneHash, qty, null, "transferred",
                            safeList(actual.evidenceAttachmentIds()), now));
                });
    }

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
                    // P1-3: verificationCode 必须提供，不可绕过
                    requireText(actual.verificationCode(), "verification_code");
                    // P0-2: SELECT FOR UPDATE 防止并发超领
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
                    // P0-2: 跨商户越权检查
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(savedBottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "该存酒记录不属于当前商户");
                    }
                    if (!"stored".equals(savedBottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_CLAIMABLE", "该存酒当前不可取用");
                    }
                    if (savedBottle.expireAt() != null && savedBottle.expireAt().isBefore(Instant.now())) {
                        throw ApiException.conflict("SAVED_BOTTLE_EXPIRED", "该存酒已过期");
                    }
                    // 取酒核身（REQ-30 + P0#4 F4/F7）：登记了瓶底标签值/姓名昵称时，取酒必须比对一致；
                    // 任一不符进入人工复核（不出酒）。比对项（取请求显式核身字段，缺省回退 verificationCode 兼容旧端）：
                    //   1) 瓶底标签值 bottleLabelValue（缺省回退 verificationCode）必须 == 登记标签值；
                    //   2) 姓名/昵称 customerNameOrNickname 必须 == 登记姓名/昵称（登记了才校验）。
                    Instant nowReview = Instant.now();
                    String claimLabel = trimToNull(actual.bottleLabelValue());
                    if (claimLabel == null) {
                        claimLabel = actual.verificationCode().trim();
                    }
                    String claimName = trimToNull(actual.customerNameOrNickname());
                    boolean labelMismatch = savedBottle.bottleLabelValue() != null
                            && !savedBottle.bottleLabelValue().equals(claimLabel);
                    boolean nameMismatch = savedBottle.customerNameOrNickname() != null
                            && !savedBottle.customerNameOrNickname().equals(claimName);
                    if (labelMismatch || nameMismatch) {
                        repository.saveSavedBottle(savedBottleWith(
                                savedBottle, "pending_identity_review", savedBottle.remainingQuantity(), nowReview));
                        return repository.saveSavedBottleClaim(new SavedBottleClaimRecord(
                                "claim_" + slug(actual.idempotencyKey()),
                                savedBottle.id(),
                                0,
                                trimToNull(actual.claimByPhone()),
                                "staff_work_phone",
                                null,
                                "pending_identity_review",
                                safeList(actual.evidenceAttachmentIds()),
                                nowReview
                        ));
                    }
                    Map<String, Object> claimItem = firstItem(actual.claimItems(), "claim_items");
                    int quantity = requirePositiveItemQuantity(claimItem, "quantity");
                    if (quantity > savedBottle.remainingQuantity()) {
                        throw ApiException.conflict("SAVED_BOTTLE_INSUFFICIENT", "存酒剩余数量不足");
                    }
                    int remaining = savedBottle.remainingQuantity() - quantity;
                    Instant now = Instant.now();
                    SavedBottleRecord updatedBottle = repository.saveSavedBottle(savedBottleWith(
                            savedBottle, remaining == 0 ? "claimed" : "stored", remaining, now));
                    SavedBottleClaimRecord claim = repository.saveSavedBottleClaim(new SavedBottleClaimRecord(
                            "claim_" + slug(actual.idempotencyKey()),
                            savedBottle.id(),
                            quantity,
                            trimToNull(actual.claimByPhone()),
                            "staff_work_phone",
                            null,
                            "picked_up",
                            safeList(actual.evidenceAttachmentIds()),
                            now
                    ));
                    notifySavedBottleAfterCommit(
                            updatedBottle,
                            "claim",
                            STORED_WINE_CLAIM_TEMPLATE,
                            "swclaim:" + claim.id(),
                            claim);
                    return claim;
                }
        );
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
                    // V350 归一：作废当刻**重新判定**（不看登记快照，也不看前端传值）——
                    // 阈值未配置 / 进货价未知 / 手工标记 / 进货价高于阈值，四者任一即高价值，一律拦下走复核。
                    if (highValueDecisionService.decide(savedBottle.itemId(), savedBottle.merchantId()).highValue()) {
                        // 高价值：双证据（照片+视频各非空）→ 建 pending 复核单 + 存酒置中间态 disputed_frozen（不直接作废）。
                        highValueReviewService.createPending(
                                actual.idempotencyKey(),
                                InventoryHighValueReviewService.SUBJECT_SAVED_BOTTLE_VOID,
                                savedBottle.id(),
                                actual.photoAttachmentIds(),
                                actual.videoAttachmentIds());
                        return repository.saveSavedBottle(
                                savedBottleWith(savedBottle, "disputed_frozen", savedBottle.remainingQuantity(), Instant.now()));
                    }
                    // 非高价值：直接 cancel（向后兼容）。
                    return repository.saveSavedBottle(
                            savedBottleWith(savedBottle, "cancelled", savedBottle.remainingQuantity(), Instant.now()));
                }
        );
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
                    return repository.saveSavedBottle(savedBottleWith(
                            savedBottle, "disputed", savedBottle.remainingQuantity(), Instant.now()));
                }
        );
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
                    LocalDate businessDate =
                            actual.businessDate() == null ? LocalDate.now() : actual.businessDate();
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
            InventoryCostSummaryRecord summary = repository.sumOutboundCost(
                    merchantId, projectId, start.toString(), end.toString(),
                    businessDayResolver.startInstant(start), businessDayResolver.startInstant(end.plusDays(1)));
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
                    Instant now = Instant.now();
                    String orderId = "trf_" + slug(actual.idempotencyKey());
                    InventoryTransferOrderRecord order = new InventoryTransferOrderRecord(orderId, transferType,
                            trimToNull(actual.lendingMode()), fromType, fromId, toType, toId,
                            trimToNull(actual.fromLocationId()), trimToNull(actual.toLocationId()),
                            "created", trimToNull(actual.operatorNote()), null, now, now, List.of());
                    repository.saveTransferOrder(order);
                    int idx = 0;
                    for (com.complex.platform.inventory.dto.TransferLineRequest lr : actual.lines()) {
                        int qty = lr.quantity() == null ? 0 : lr.quantity();
                        if (qty <= 0) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "明细数量必须大于 0");
                        }
                        long unitPrice = lr.unitPriceCent() == null ? 0L : lr.unitPriceCent();
                        if (unitPrice < 0L) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "明细单价不能为负数");
                        }
                        repository.saveTransferLine(new InventoryTransferLineRecord(
                                orderId + "_l" + (idx++), orderId, requireText(lr.itemId(), "item_id"), qty,
                                lr.traceCodes() == null ? List.of() : lr.traceCodes(),
                                unitPrice, now));
                    }
                    return order;
                });
    }

    public InventoryTransferOrderRecord shipTransferOrder(String orderId) {
        String id = requireText(orderId, "order_id");
        return executeWrite("shipTransferOrder:" + id, id, id,
                InventoryTransferOrderRecord.class, "inventory.transfer_order_shipped", "inventory.transfer_order_ship_failed",
                "inventory.transfer_order.manage", "inventory_transfer_order",
                () -> {
                    InventoryTransferOrderRecord o = repository.findTransferOrderById(id).orElseThrow(() ->
                            ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
                    if (!"created".equals(o.status())) {
                        throw ApiException.conflict("TRANSFER_ORDER_NOT_CREATED", "只有新建状态的调拨单才能发货");
                    }
                    recordTransferSettlementOnShip(o, Instant.now());
                    return repository.saveTransferOrder(withTransferStatus(o, "shipped"));
                });
    }

    public InventoryTransferOrderRecord receiveTransferOrder(String orderId, com.complex.platform.inventory.dto.ReceiveTransferRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite("receiveTransferOrder:" + id, request == null ? null : request.idempotencyKey(), request,
                InventoryTransferOrderRecord.class, "inventory.transfer_order_received", "inventory.transfer_order_receive_failed",
                "inventory.transfer_order.manage", "inventory_transfer_order",
                () -> {
                    InventoryTransferOrderRecord o = repository.findTransferOrderById(id).orElseThrow(() ->
                            ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
                    if (!"shipped".equals(o.status())) {
                        throw ApiException.conflict("TRANSFER_ORDER_NOT_SHIPPED", "只有已发货的调拨单才能收货");
                    }
                    Instant now = Instant.now();
                    java.util.LinkedHashSet<String> orderCodes = new java.util.LinkedHashSet<>();
                    for (InventoryTransferLineRecord l : repository.listTransferLinesByOrder(o.id())) {
                        orderCodes.addAll(l.traceCodes());
                    }
                    List<String> scanned = request == null || request.scannedTraceCodes() == null ? List.of() : request.scannedTraceCodes();
                    for (String code : scanned) {
                        if (!orderCodes.contains(code)) {
                            throw ApiException.badRequest("TRACE_CODE_MISMATCH", "扫描的溯源码不属于本次调拨：" + code);
                        }
                    }
                    // 防调包:本单有随货溯源码时,收货必须逐码扫齐(空扫/漏扫拒绝),扫描集须与本单集完全一致。
                    if (!orderCodes.isEmpty() && !new java.util.HashSet<>(scanned).equals(new java.util.HashSet<>(orderCodes))) {
                        throw ApiException.badRequest("TRACE_CODE_INCOMPLETE",
                                "扫描的溯源码必须与本次调拨的码完全一致，应为 " + orderCodes.size() + " 个");
                    }
                    // 溯源归属 from->to:写归属变更台账(防调包+对账)+尽力更新溯源码归属(存在则更新)。
                    int seq = 0;
                    for (String code : orderCodes) {
                        // 防调包(原子):CAS 只在归属=出让方或无主时翻转;命中0行且码已存在=归属第三方(或并发被改走)→拒并回滚。
                        // 命中0行且码不存在=P1 随货新码(归属登记泛化待 P2)→放行。CAS 结果即业务判定,消除 check-then-act 竞态。
                        int affected = repository.updateTraceCodeOwnerIfExists(code, o.fromOwnerId(), o.toOwnerType(), o.toOwnerId(), now);
                        if (affected == 0 && repository.traceCodeExists(code)) {
                            throw ApiException.badRequest("TRACE_OWNER_MISMATCH",
                                    "溯源码属于其他主体，不能收货：" + code);
                        }
                        repository.saveTraceOwnershipHistory(new InventoryTraceOwnershipHistoryRecord(
                                o.id() + "_h" + (seq++), code, o.fromOwnerType(), o.fromOwnerId(),
                                o.toOwnerType(), o.toOwnerId(), o.id(), now));
                    }
                    // ponytail: P1 只做归属变更+台账;收货方库存入账/供货方库存扣减留 P2(供货主体库存需 warehouse owner 泛化,两侧同期做更一致)。
                    return repository.saveTransferOrder(withTransferStatus(o, "received"));
                });
    }

    /** 借调归还(AUD-002/003)：借用型(lending+borrow)收货后归还,溯源码归属翻回出让方并置 returned。 */
    public InventoryTransferOrderRecord returnTransferOrder(String orderId,
            com.complex.platform.inventory.dto.ReceiveTransferRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite("returnTransferOrder:" + id, request == null ? null : request.idempotencyKey(), request,
                InventoryTransferOrderRecord.class, "inventory.transfer_order_returned",
                "inventory.transfer_order_return_failed", "inventory.transfer_order.manage", "inventory_transfer_order",
                () -> {
                    InventoryTransferOrderRecord o = repository.findTransferOrderById(id).orElseThrow(() ->
                            ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
                    if (!("lending".equals(o.transferType()) && "borrow".equals(o.lendingMode()))) {
                        throw ApiException.badRequest("TRANSFER_NOT_RETURNABLE",
                                "只有已借出的借调单才能归还");
                    }
                    if (!"received".equals(o.status())) {
                        throw ApiException.conflict("TRANSFER_ORDER_NOT_RECEIVED", "只有已收货的调拨单才能退回");
                    }
                    Instant now = Instant.now();
                    java.util.LinkedHashSet<String> orderCodes = new java.util.LinkedHashSet<>();
                    for (InventoryTransferLineRecord l : repository.listTransferLinesByOrder(o.id())) {
                        orderCodes.addAll(l.traceCodes());
                    }
                    // 归属翻回 to->from:CAS 只在归属=借入方时翻回出让方;命中0且码存在=已被改走→拒并回滚。
                    int seq = 0;
                    for (String code : orderCodes) {
                        int affected = repository.updateTraceCodeOwnerIfExists(
                                code, o.toOwnerId(), o.fromOwnerType(), o.fromOwnerId(), now);
                        if (affected == 0 && repository.traceCodeExists(code)) {
                            throw ApiException.badRequest("TRACE_OWNER_MISMATCH",
                                    "溯源码已不属于借入方，不能归还：" + code);
                        }
                        repository.saveTraceOwnershipHistory(new InventoryTraceOwnershipHistoryRecord(
                                o.id() + "_r" + (seq++), code, o.toOwnerType(), o.toOwnerId(),
                                o.fromOwnerType(), o.fromOwnerId(), o.id(), now));
                    }
                    return repository.saveTransferOrder(withTransferStatus(o, "returned"));
                });
    }

    /** 取消流转单(AUD-002/003)：created 直接取消;shipped 需冲销发货时挂的应付(未收款才可冲)。 */
    public InventoryTransferOrderRecord cancelTransferOrder(String orderId,
            com.complex.platform.inventory.dto.ReceiveTransferRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite("cancelTransferOrder:" + id, request == null ? null : request.idempotencyKey(), request,
                InventoryTransferOrderRecord.class, "inventory.transfer_order_cancelled",
                "inventory.transfer_order_cancel_failed", "inventory.transfer_order.manage", "inventory_transfer_order",
                () -> {
                    InventoryTransferOrderRecord o = repository.findTransferOrderById(id).orElseThrow(() ->
                            ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
                    if (!"created".equals(o.status()) && !"shipped".equals(o.status())) {
                        throw ApiException.conflict("TRANSFER_ORDER_NOT_CANCELLABLE",
                                "只有新建或已发货的调拨单才能取消");
                    }
                    if ("shipped".equals(o.status()) && financeRepository != null) {
                        reverseTransferSettlementOnCancel(o, Instant.now());
                    }
                    return repository.saveTransferOrder(withTransferStatus(o, "cancelled"));
                });
    }

    /** 冲销发货时挂的应付:未收款才可冲(已收款需先退款);置 paid=0/unpaid=0/status=cancelled(金额留痕不改)。 */
    private void reverseTransferSettlementOnCancel(InventoryTransferOrderRecord o, Instant now) {
        financeRepository.findArApItemById("arap_trf_" + o.id()).ifPresent(item -> {
            if (item.paidAmountCent() > 0L) {
                throw ApiException.conflict("TRANSFER_PAYABLE_PARTIALLY_PAID",
                        "调拨应付款已有付款记录，请先退款再取消");
            }
            financeRepository.updateArApItemLedger(new com.complex.platform.finance.ArApItemRecord(
                    item.id(), item.itemNo(), item.direction(), item.counterpartyId(), item.sourceType(), item.sourceId(),
                    item.amountCent(), 0L, 0L, item.currency(), item.dueDate(), item.accountingPeriod(),
                    "cancelled", item.createdAt(), now, item.merchantId(), item.projectId()));
        });
    }

    public InventoryTransferOrderRecord getTransferOrder(String orderId) {
        authorizationService.requirePermission("inventory.transfer_order.view");
        InventoryTransferOrderRecord o = repository.findTransferOrderById(requireText(orderId, "order_id")).orElseThrow(() ->
                ApiException.notFound("TRANSFER_ORDER_NOT_FOUND", "调拨单不存在或已被删除"));
        return new InventoryTransferOrderRecord(o.id(), o.transferType(), o.lendingMode(), o.fromOwnerType(), o.fromOwnerId(),
                o.toOwnerType(), o.toOwnerId(), o.fromLocationId(), o.toLocationId(), o.status(), o.operatorNote(),
                o.createdBy(), o.createdAt(), o.updatedAt(), repository.listTransferLinesByOrder(o.id()));
    }

    public PageResponse<InventoryTransferOrderRecord> listTransferOrders() {
        authorizationService.requirePermission("inventory.transfer_order.view");
        List<InventoryTransferOrderRecord> rows = repository.listTransferOrders();
        return new PageResponse<>(rows, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, rows.size());
    }

    /**
     * 发货即记结算(2026-07-08 用户拍板:发货时即生成应付·借调"借"默认不计占用费=不记)。并入现有应收应付台账。
     * 供货调拨/借调买断→按货款(Σ 数量×单价)生成"收货方应付出让方"应付项;金额为0(免费供货/未定价)或借用型=不记。
     */
    private void recordTransferSettlementOnShip(InventoryTransferOrderRecord o, Instant now) {
        boolean billable = "supply".equals(o.transferType())
                || ("lending".equals(o.transferType()) && "purchase".equals(o.lendingMode()));
        if (!billable || financeRepository == null) {
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
        financeRepository.saveArApItem(new com.complex.platform.finance.ArApItemRecord(
                itemId, "ARAP-TRF-" + o.id(), "payable",
                o.fromOwnerId(), "inventory_transfer", o.id(),
                amountCent, 0L, amountCent, "CNY",
                java.time.LocalDate.now(java.time.ZoneOffset.UTC), null, "pending_payment",
                now, now, o.toOwnerId()));
        financeRepository.saveArApItemSource(new com.complex.platform.finance.ArApItemSourceRecord(
                "arap_src_trf_" + o.id(), itemId, "inventory_transfer", o.id(), amountCent, now));
    }

    private InventoryTransferOrderRecord withTransferStatus(InventoryTransferOrderRecord o, String status) {
        return new InventoryTransferOrderRecord(o.id(), o.transferType(), o.lendingMode(), o.fromOwnerType(), o.fromOwnerId(),
                o.toOwnerType(), o.toOwnerId(), o.fromLocationId(), o.toLocationId(), status, o.operatorNote(),
                o.createdBy(), o.createdAt(), Instant.now(), o.lines());
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
        try {
            List<T> items = supplier.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", items.size())
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
