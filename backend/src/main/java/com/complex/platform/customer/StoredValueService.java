package com.complex.platform.customer;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.notification.NotificationService;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 储值（预付卡 / 余额账户，gap-analysis §7A P1）。
 *
 * <p><b>预付资金合规</b>：充值不凭空加余额——充值 {@link #topup} 先经 finance 创建支付意图，
 * 支付成功后调 {@link #settleTopup} 校验支付意图已 succeeded、金额一致、未重复入账，方才入账并写流水。
 * 消费 {@link #consume} 余额不足拒；退卡 {@link #refund} 退回余额（实际退款打款待支付渠道）；
 * 冻结/解冻 {@link #freeze}/{@link #unfreeze} 阻断/恢复变动。所有余额变动写流水 + 事件。
 *
 * <p><b>B1d⑤ 储值卡分级</b>：每个操作带 {@code merchantId}——为空=综合体通用卡（综合体内各商户通用，按 project 隔离）；
 * 非空=商户级卡（只能在该发卡商户消费，按 merchant 隔离）。账户按卡范围 (customer, project, merchant) 精确解析，
 * 「只扣指定的那一张」（请求点名卡范围，不自动跨卡）。商户卡充值资金归属由开关 {@code fundOwnership} 决定
 * （默认 merchant_prepaid=发卡商户预收；platform_collect=平台代收消费时结算给商户），记账接财务结算。
 */
@Service
public class StoredValueService {

    private static final Logger log = LoggerFactory.getLogger(StoredValueService.class);
    private static final String EVENT_TOPUP = "stored_value.topup";
    private static final String EVENT_CONSUME = "stored_value.consume";
    private static final String EVENT_REFUND = "stored_value.refund";
    private static final String NOTIFICATION_DOMAIN = "stored_value";
    private static final String NOTIFICATION_EVENT_TOPUP = "topup";
    private static final String NOTIFICATION_EVENT_CONSUME = "consume";
    private static final String STORED_VALUE_TOPUP_TEMPLATE = "sms.stored_value.topup";
    private static final String STORED_VALUE_CONSUME_TEMPLATE = "sms.stored_value.consume";

    /** 商户卡充值资金归属：merchant_prepaid=发卡商户预收（默认）；platform_collect=平台代收、消费时结算给商户。 */
    static final String FUND_MERCHANT_PREPAID = "merchant_prepaid";

    private final CustomerRepository repository;
    private final FinanceService financeService;
    private final AuditEventService auditEventService;
    private final com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway;
    // B1d⑤ phase4 员工端卡范围隔离：商户范围员工只能操作本商户的商户卡。可为 null=旧构造/兼容测试（不强制）。
    private final com.complex.platform.access.AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final Clock clock;

    // B1d⑤ 商户卡充值资金归属默认值（开关，可后台/配置覆盖；默认商户预收）。
    @Value("${stored-value.merchant-card.fund-ownership:merchant_prepaid}")
    private String fundOwnershipDefault = FUND_MERCHANT_PREPAID;

    public StoredValueService(CustomerRepository repository, FinanceService financeService,
                              AuditEventService auditEventService,
                              com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway,
                              com.complex.platform.access.AuthorizationService authorizationService) {
        this(repository, financeService, auditEventService, paymentGateway, authorizationService, null, Clock.systemUTC());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public StoredValueService(CustomerRepository repository, FinanceService financeService,
                              AuditEventService auditEventService,
                              com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway,
                              com.complex.platform.access.AuthorizationService authorizationService,
                              NotificationService notificationService) {
        this(repository, financeService, auditEventService, paymentGateway, authorizationService,
                notificationService, Clock.systemUTC());
    }

    StoredValueService(CustomerRepository repository, FinanceService financeService,
                       AuditEventService auditEventService,
                       com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway,
                       com.complex.platform.access.AuthorizationService authorizationService, Clock clock) {
        this(repository, financeService, auditEventService, paymentGateway, authorizationService, null, clock);
    }

    private StoredValueService(CustomerRepository repository, FinanceService financeService,
                               AuditEventService auditEventService,
                               com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway,
                               com.complex.platform.access.AuthorizationService authorizationService,
                               NotificationService notificationService,
                               Clock clock) {
        this.repository = repository;
        this.financeService = financeService;
        this.auditEventService = auditEventService;
        this.paymentGateway = paymentGateway;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /** 向后兼容构造（无 authorizationService=不强制卡范围隔离）：供既有单测构造（带 gateway）。 */
    StoredValueService(CustomerRepository repository, FinanceService financeService,
                       AuditEventService auditEventService,
                       com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway, Clock clock) {
        this(repository, financeService, auditEventService, paymentGateway, null, clock);
    }

    /** 向后兼容构造：未提供支付渠道路由时用"开关关"占位路由（不调外部渠道）。供既有单测构造。 */
    StoredValueService(CustomerRepository repository, FinanceService financeService,
                       AuditEventService auditEventService, Clock clock) {
        this(repository, financeService, auditEventService,
                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null, clock);
    }

    // ============================================================
    // 充值（绑支付意图）：第一步创建支付意图（不入账），第二步支付成功后入账
    // ============================================================

    /**
     * 发起充值：创建一笔 finance 支付意图（status=created），返回该支付意图。此时余额不变。
     * 客户端完成支付后由支付渠道回调（测试期 {@code financeService.markPaymentIntentSucceeded}）置
     * succeeded，再调 {@link #settleTopup} 入账。merchantId 非空=给商户级卡充值。
     */
    @Transactional
    @GuardedAction(capability = "customer.stored_value.topup", permission = "customer.stored_value.topup")
    public PaymentIntentRecord topup(String customerId, String projectId, String merchantId, long amountCent,
                                     String channel, String idempotencyKey) {
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent must be positive");
        }
        requireCustomer(customerId);
        // 提前建账户（active），充值入账时复用。
        loadOrCreateAccountForUpdate(customerId, projectId, merchantId);
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? "sv_topup_" + UUID.randomUUID() : idempotencyKey;
        // B1d⑤ 资金归属：商户卡走 stored_value_topup_merchant（finance 据此把预收记入发卡商户，按开关结算）；
        // 通用卡仍走 stored_value_topup（平台综合体级余额）。businessOrderId=customerId 保持既有归属链。
        String businessOrderType = merchantId == null ? "stored_value_topup" : "stored_value_topup_merchant";
        PaymentIntentRecord intent = financeService.createPaymentIntent(new PaymentIntentRequest(
                key, null, null, businessOrderType, customerId, amountCent, "CNY",
                channel == null || channel.isBlank() ? "wechat" : channel));
        // AUD-058：把本次充值的卡范围（customer/project/merchant）绑定到支付意图，settle 只认此绑定范围入账，
        // 杜绝请求体在入账时指名入他卡/跨商户/跨项目。绑定与账户创建同一事务落库。
        repository.bindStoredValueTopupIntent(intent.id(), customerId, projectId, merchantId, now());
        return intent;
    }

    /**
     * 充值入账：校验绑定支付意图已成功、金额一致、未重复入账，余额 +，写 topup 流水 + 事件。
     * 无支付（支付意图非 succeeded）则拒绝——保证不凭空加余额。
     */
    @Transactional
    @GuardedAction(capability = "customer.stored_value.topup", permission = "customer.stored_value.topup")
    public CustomerStoredValueTransactionRecord settleTopup(String customerId, String projectId, String merchantId,
                                                            String paymentIntentId) {
        Instant now = now();
        requireCustomer(customerId);
        PaymentIntentRecord intent = financeService.findPaymentIntent(paymentIntentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "payment intent not found"));
        if (!"succeeded".equals(intent.status())) {
            throw ApiException.conflict("PAYMENT_NOT_SUCCEEDED", "payment intent is not succeeded");
        }
        // AUD-058：入账账户范围只认充值时绑定的卡范围（customer/project/merchant），请求体范围必须与之一致，
        // 否则=通用卡充值被指名入商户卡/跨商户/跨项目。绑定行由 topup 落库，按支付意图 id 读回校验。
        StoredValueTopupIntentScope bound = repository.findStoredValueTopupIntent(paymentIntentId)
                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED",
                        "payment intent does not belong to this top-up"));
        if (!customerId.equals(bound.customerId())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment intent does not belong to this top-up");
        }
        if (!java.util.Objects.equals(projectId, bound.projectId())
                || !java.util.Objects.equals(merchantId, bound.merchantId())) {
            throw ApiException.conflict("STORED_VALUE_SCOPE_MISMATCH",
                    "settle card scope does not match the top-up card scope");
        }
        if (repository.storedValueTransactionExistsForPaymentIntent(paymentIntentId)) {
            return repository.findStoredValueTransactionByIdempotencyKey("sv_settle_" + paymentIntentId)
                    .orElseThrow(() -> ApiException.conflict("STORED_VALUE_ALREADY_SETTLED",
                            "top-up already settled for this payment intent"));
        }
        CustomerStoredValueAccountRecord account = loadOrCreateAccountForUpdate(customerId, projectId, merchantId);
        if ("closed".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_CLOSED", "stored-value account is closed");
        }
        long balanceAfter = account.balanceCent() + intent.amountCent();
        repository.saveStoredValueAccount(withBalanceAndStatus(account, balanceAfter, account.status(), now));
        CustomerStoredValueTransactionRecord txn = repository.saveStoredValueTransaction(
                new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "topup", intent.amountCent(), balanceAfter,
                        // V231：充值即支付，paid_at=交易完成时刻 now（与写流水同事务）。
                        paymentIntentId, "top-up settled", "sv_settle_" + paymentIntentId, now, now));
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("amount_cent", intent.amountCent());
        detail.put("balance_after_cent", balanceAfter);
        detail.put("payment_intent_id", paymentIntentId);
        detail.put("card_scope", account.cardScope());
        if (account.merchantId() != null) {
            detail.put("merchant_id", account.merchantId());
            // B1d⑤ 商户卡资金归属：默认 merchant_prepaid=记入发卡商户预收。留痕供财务结算。
            // ponytail: 此处仅记账归属，深度 财务预收款台账/消费时结算分录 走 finance 模块跟进（独立结算工单），不在储值域内联。
            detail.put("fund_ownership", fundOwnershipDefault);
        }
        auditEventService.recordSuccess(EVENT_TOPUP, "customer.stored_value.topup",
                "customer_stored_value", account.id(), null, null, detail);
        notifyStoredValueAfterCommit(txn, account, NOTIFICATION_EVENT_TOPUP, STORED_VALUE_TOPUP_TEMPLATE,
                "svtopup:" + txn.id(), intent.amountCent(), balanceAfter);
        return txn;
    }

    // ============================================================
    // 消费（余额扣减，不足拒，冻结阻断）。merchantId 点名卡范围：商户卡只能由该商户消费、只扣这一张。
    // ============================================================

    @Transactional
    @GuardedAction(capability = "customer.stored_value.consume", permission = "customer.stored_value.consume")
    public CustomerStoredValueTransactionRecord consume(String customerId, String projectId, String merchantId,
                                                        long amountCent, String reason, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent must be positive");
        }
        Instant now = now();
        CustomerStoredValueTransactionRecord existing = idempotentTxn(idempotencyKey);
        if (existing != null) {
            // AUD-057: 幂等键全局唯一,命中流水须属当前客人;否则=跨客人键碰撞,拒绝(不泄露他人流水/不抑制本人操作)。
            if (!customerId.equals(existing.customerId())) {
                throw ApiException.conflict("IDEMPOTENCY_KEY_CONFLICT", "idempotency key already used by another operation");
            }
            return existing;
        }
        // 「只扣指定的那一张」：按 (customer, project, merchant) 精确解析卡。商户卡=该商户的卡，
        // 故商户卡消费天然限定在该发卡商户（用 merchant Y 的范围取不到 merchant X 的卡）。
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
        // 清单 P1-1:与 refund/freeze/unfreeze 一致,消费须先过卡范围校验(他商户/通用卡越权即拒,
        // 且在状态检查之前——不向越权者泄露卡状态)。此前缺此行 → merchantId 请求体可控可跨商户扣款。
        requireMerchantScopeForCard(account);
        requireActive(account);
        if (account.balanceCent() < amountCent) {
            throw ApiException.conflict("STORED_VALUE_INSUFFICIENT", "stored-value balance is insufficient");
        }
        long balanceAfter = account.balanceCent() - amountCent;
        repository.saveStoredValueAccount(withBalanceAndStatus(account, balanceAfter, account.status(), now));
        CustomerStoredValueTransactionRecord txn = repository.saveStoredValueTransaction(
                new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "consume", -amountCent, balanceAfter,
                        null, reason, idempotencyKey, now));
        auditEventService.recordSuccess(EVENT_CONSUME, "customer.stored_value.consume",
                "customer_stored_value", account.id(), null, null,
                consumeRefundDetail(account, amountCent, balanceAfter));
        notifyStoredValueAfterCommit(txn, account, NOTIFICATION_EVENT_CONSUME, STORED_VALUE_CONSUME_TEMPLATE,
                "svconsume:" + txn.id(), amountCent, balanceAfter);
        return txn;
    }

    // ============================================================
    // 退卡 / 退余额（退回余额；实际退款打款待支付渠道）
    // ============================================================

    @Transactional
    @GuardedAction(capability = "customer.stored_value.refund", permission = "customer.stored_value.refund")
    public CustomerStoredValueTransactionRecord refund(String customerId, String projectId, String merchantId,
                                                       long amountCent, String reason, boolean closeAccount,
                                                       String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent must be positive");
        }
        Instant now = now();
        CustomerStoredValueTransactionRecord existing = idempotentTxn(idempotencyKey);
        if (existing != null) {
            // AUD-057: 幂等键全局唯一,命中流水须属当前客人;否则=跨客人键碰撞,拒绝(不泄露他人流水/不抑制本人操作)。
            if (!customerId.equals(existing.customerId())) {
                throw ApiException.conflict("IDEMPOTENCY_KEY_CONFLICT", "idempotency key already used by another operation");
            }
            return existing;
        }
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
        requireMerchantScopeForCard(account);
        if ("closed".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_CLOSED", "stored-value account is closed");
        }
        if (account.balanceCent() < amountCent) {
            throw ApiException.conflict("STORED_VALUE_INSUFFICIENT", "refund exceeds remaining balance");
        }
        // B1d⑤ phase3 商户级卡退款规则：仅对商户卡（merchantId 非空）应用发卡商户自设规则。
        //   refund_allowed=false → 拒退；退款比例=实际退回客户的比例，余下为商户留存（手续费/违约金）。
        //   综合体通用卡（merchantId 空）无规则=全额退（payout=amountCent），沿用既有行为。
        long payoutCent = amountCent;
        if (account.merchantId() != null) {
            StoredValueMerchantRefundRuleRecord rule = repository.findStoredValueMerchantRefundRule(account.merchantId())
                    .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(account.merchantId()));
            if (!rule.refundAllowed()) {
                throw ApiException.conflict("STORED_VALUE_REFUND_NOT_ALLOWED",
                        "merchant card refund is not allowed by the issuing merchant");
            }
            payoutCent = amountCent * rule.refundRatioBps() / 10000L;
        }
        long balanceAfter = account.balanceCent() - amountCent;
        String newStatus = closeAccount && balanceAfter == 0 ? "closed" : account.status();
        repository.saveStoredValueAccount(withBalanceAndStatus(account, balanceAfter, newStatus, now));
        CustomerStoredValueTransactionRecord txn = repository.saveStoredValueTransaction(
                new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "refund", -amountCent, balanceAfter,
                        null, reason, idempotencyKey, now));
        // 退款打款：系统内余额已退回（上方）。实际打款金额=payoutCent（商户卡按比例）。打款路径由支付渠道开关决定——
        //   关（默认）→ 不调外部渠道，标 reserved（人工/内部确认打款，不阻塞）。
        //   开 → 路由到 PaymentGatewayPort.refund（无真实厂商=占位 NOT_CONFIGURED，留接缝，不崩）。
        com.complex.platform.finance.gateway.PaymentGatewayResult gw = paymentGateway.refund(
                com.complex.platform.finance.gateway.PaymentGatewayRequest.of(
                        "stored_value_refund", account.id(), payoutCent, null));
        // AUD-060：退款打款状态如实反映，不得一律记成功。
        //   FAILED（渠道明确失败）→ 整单回滚（@Transactional）：余额不扣、不写流水，避免"扣了余额没退钱还显示成功"。
        if (gw.status() == com.complex.platform.finance.gateway.PaymentGatewayResult.Status.FAILED) {
            throw ApiException.conflict("STORED_VALUE_REFUND_FAILED", "refund payout failed at payment gateway");
        }
        //   非 SUCCEEDED（RESERVED/NOT_CONFIGURED/PENDING=未真正兑付）→ 复用财务退款状态机登记一笔 requested 的
        //   退款台账（内部/手工确认打款），让实际打款成为可追踪待办而非仅留审计面包屑；SUCCEEDED（渠道已兑付）→ settled。
        //   payoutCent=0（商户规则退款比例 0%）无需打款，不登记。
        boolean payoutSettled = gw.status() == com.complex.platform.finance.gateway.PaymentGatewayResult.Status.SUCCEEDED;
        String payoutRefundId = payoutSettled || payoutCent <= 0 ? null
                : financeService.recordStoredValuePayoutRefund(account.id(), payoutCent, reason,
                        gw.requiresInternalFallback() ? "manual" : gw.gatewayProvider(), idempotencyKey);
        Map<String, Object> detail = consumeRefundDetail(account, amountCent, balanceAfter);
        detail.put("closed", newStatus.equals("closed"));
        detail.put("payout_cent", payoutCent);
        detail.put("payout_gateway_status", gw.status().name());
        detail.put("payout_status", payoutSettled ? "settled" : "requested");
        if (payoutRefundId != null) {
            detail.put("payout_refund_id", payoutRefundId);
        }
        auditEventService.recordSuccess(EVENT_REFUND, "customer.stored_value.refund",
                "customer_stored_value", account.id(), null, null, detail);
        return txn;
    }

    // ============================================================
    // 商户级卡退款规则配置（B1d⑤ phase3，发卡商户自设可否退/比例）
    // ============================================================

    @Transactional
    @GuardedAction(capability = "customer.stored_value.refund", permission = "customer.stored_value.refund")
    public StoredValueMerchantRefundRuleRecord configureMerchantRefundRule(String merchantId, boolean refundAllowed,
                                                                           int refundRatioBps) {
        if (merchantId == null || merchantId.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "merchant_id is required");
        }
        if (refundRatioBps < 0 || refundRatioBps > 10000) {
            throw ApiException.badRequest("VALIDATION_FAILED", "refund_ratio_bps must be within [0,10000]");
        }
        // phase4 卡范围隔离：商户范围员工只能配置本商户的退款规则。
        requireMerchantScopeForMerchant(merchantId.trim());
        Instant now = now();
        return repository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId.trim(), refundAllowed, refundRatioBps, now, now));
    }

    @GuardedAction(capability = "customer.stored_value.refund", permission = "customer.stored_value.refund")
    public StoredValueMerchantRefundRuleRecord getMerchantRefundRule(String merchantId) {
        // AUD-062：查询商户退款规则须过权限闸 + 商户 scope（与 configureMerchantRefundRule 一致），
        // 否则任意店员/跨商户可读任意商户的退款规则配置。
        requireMerchantScopeForMerchant(merchantId);
        return repository.findStoredValueMerchantRefundRule(merchantId)
                .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(merchantId));
    }

    // ============================================================
    // 冻结 / 解冻
    // ============================================================

    @Transactional
    @GuardedAction(capability = "customer.stored_value.freeze", permission = "customer.stored_value.freeze")
    public CustomerStoredValueAccountRecord freeze(String customerId, String projectId, String merchantId, String reason) {
        Instant now = now();
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
        requireMerchantScopeForCard(account);
        if ("closed".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_CLOSED", "stored-value account is closed");
        }
        if ("frozen".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ALREADY_FROZEN", "stored-value account already frozen");
        }
        CustomerStoredValueAccountRecord frozen = withBalanceAndStatus(account, account.balanceCent(), "frozen", now);
        repository.saveStoredValueAccount(frozen);
        auditEventService.recordSuccess("stored_value.frozen", "customer.stored_value.freeze",
                "customer_stored_value", account.id(), null, null, Map.of("reason", reason == null ? "" : reason));
        return frozen;
    }

    @Transactional
    @GuardedAction(capability = "customer.stored_value.freeze", permission = "customer.stored_value.freeze")
    public CustomerStoredValueAccountRecord unfreeze(String customerId, String projectId, String merchantId, String reason) {
        Instant now = now();
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
        requireMerchantScopeForCard(account);
        if (!"frozen".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_NOT_FROZEN", "stored-value account is not frozen");
        }
        CustomerStoredValueAccountRecord active = withBalanceAndStatus(account, account.balanceCent(), "active", now);
        repository.saveStoredValueAccount(active);
        auditEventService.recordSuccess("stored_value.unfrozen", "customer.stored_value.freeze",
                "customer_stored_value", account.id(), null, null, Map.of("reason", reason == null ? "" : reason));
        return active;
    }

    // ============================================================
    // 辅助
    // ============================================================

    private Map<String, Object> consumeRefundDetail(CustomerStoredValueAccountRecord account,
                                                     long amountCent, long balanceAfter) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("amount_cent", amountCent);
        detail.put("balance_after_cent", balanceAfter);
        detail.put("card_scope", account.cardScope());
        if (account.merchantId() != null) {
            detail.put("merchant_id", account.merchantId());
        }
        return detail;
    }

    private void notifyStoredValueAfterCommit(CustomerStoredValueTransactionRecord txn,
                                              CustomerStoredValueAccountRecord account,
                                              String event,
                                              String templateCode,
                                              String idempotencyKey,
                                              long amountCent,
                                              long balanceAfterCent) {
        if (notificationService == null) {
            return;
        }
        Runnable notify = () -> notifyStoredValueNow(txn, account, event, templateCode,
                idempotencyKey, amountCent, balanceAfterCent);
        try {
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
        } catch (RuntimeException exception) {
            log.warn("stored value notification registration failed: event={}, idempotency_key={}",
                    event, idempotencyKey, exception);
            return;
        }
        notify.run();
    }

    private void notifyStoredValueNow(CustomerStoredValueTransactionRecord txn,
                                      CustomerStoredValueAccountRecord account,
                                      String event,
                                      String templateCode,
                                      String idempotencyKey,
                                      long amountCent,
                                      long balanceAfterCent) {
        if (notificationService == null) {
            return;
        }
        try {
            notificationService.notify(
                    NOTIFICATION_DOMAIN,
                    event,
                    resolveStoredValueRecipient(txn == null ? null : txn.customerId()),
                    templateCode,
                    storedValueNotificationParams(account, amountCent, balanceAfterCent),
                    idempotencyKey);
        } catch (RuntimeException exception) {
            log.warn(
                    "stored value notification failed: event={}, txn_id={}, idempotency_key={}",
                    event,
                    txn == null ? null : txn.id(),
                    idempotencyKey,
                    exception);
        }
    }

    private String resolveStoredValueRecipient(String customerId) {
        String normalizedCustomerId = trimToNull(customerId);
        if (normalizedCustomerId == null) {
            return null;
        }
        try {
            return repository.findCustomerById(normalizedCustomerId)
                    .map(CustomerRecord::phone)
                    .map(this::trimToNull)
                    .orElse(null);
        } catch (RuntimeException exception) {
            log.warn("failed to resolve stored value customer phone: customer_id={}",
                    normalizedCustomerId, exception);
            return null;
        }
    }

    private Map<String, Object> storedValueNotificationParams(CustomerStoredValueAccountRecord account,
                                                              long amountCent,
                                                              long balanceAfterCent) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("amount_cent", amountCent);
        params.put("balance_after_cent", balanceAfterCent);
        if (account != null) {
            params.put("card_type", account.cardScope());
            params.put("card_scope", account.cardScope());
            if (account.merchantId() != null) {
                params.put("merchant_id", account.merchantId());
            }
        }
        return params;
    }

    private CustomerStoredValueTransactionRecord idempotentTxn(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return repository.findStoredValueTransactionByIdempotencyKey(idempotencyKey).orElse(null);
    }

    private void requireActive(CustomerStoredValueAccountRecord account) {
        if ("frozen".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_FROZEN", "stored-value account is frozen");
        }
        if ("closed".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_CLOSED", "stored-value account is closed");
        }
    }

    // B1d⑤ phase4 员工端卡范围隔离：商户范围调用者（员工）只能操作本商户发行的商户卡；
    // 通用卡（merchantId 空）属平台/综合体级，商户员工不得操作。平台管理员（scope 空）放行；authorizationService 为 null 不强制。
    private void requireMerchantScopeForCard(CustomerStoredValueAccountRecord account) {
        if (authorizationService == null) {
            return;
        }
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(account.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                        "stored-value card is out of the caller's merchant scope");
            }
        });
    }

    private void requireMerchantScopeForMerchant(String merchantId) {
        if (authorizationService == null) {
            return;
        }
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(merchantId)) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                        "merchant refund rule is out of the caller's merchant scope");
            }
        });
    }

    private CustomerStoredValueAccountRecord requireAccountForUpdate(String customerId, String projectId, String merchantId) {
        return repository.findStoredValueAccountForUpdate(customerId, projectId, merchantId)
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND",
                        "stored-value account not found"));
    }

    private CustomerStoredValueAccountRecord loadOrCreateAccountForUpdate(String customerId, String projectId, String merchantId) {
        return repository.findStoredValueAccountForUpdate(customerId, projectId, merchantId)
                .orElseGet(() -> {
                    Instant now = now();
                    return repository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                            "svacc_" + UUID.randomUUID().toString().replace("-", ""),
                            customerId, projectId, merchantId, 0L, "active", now, now));
                });
    }

    private CustomerStoredValueAccountRecord withBalanceAndStatus(CustomerStoredValueAccountRecord account,
                                                                  long balanceCent, String status, Instant now) {
        return new CustomerStoredValueAccountRecord(account.id(), account.customerId(), account.projectId(),
                account.merchantId(), balanceCent, status, account.createdAt(), now);
    }

    private CustomerRecord requireCustomer(String customerId) {
        return repository.findCustomerById(customerId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "customer not found"));
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
