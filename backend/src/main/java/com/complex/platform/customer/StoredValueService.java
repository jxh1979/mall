package com.complex.platform.customer;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.gate.GuardedAction;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String EVENT_TOPUP = "stored_value.topup";
    private static final String EVENT_CONSUME = "stored_value.consume";
    private static final String EVENT_REFUND = "stored_value.refund";

    /** 商户卡充值资金归属：merchant_prepaid=发卡商户预收（默认）；platform_collect=平台代收、消费时结算给商户。 */
    static final String FUND_MERCHANT_PREPAID = "merchant_prepaid";

    private final CustomerRepository repository;
    private final FinanceService financeService;
    private final AuditEventService auditEventService;
    private final com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway;
    private final Clock clock;

    // B1d⑤ 商户卡充值资金归属默认值（开关，可后台/配置覆盖；默认商户预收）。
    @Value("${stored-value.merchant-card.fund-ownership:merchant_prepaid}")
    private String fundOwnershipDefault = FUND_MERCHANT_PREPAID;

    @org.springframework.beans.factory.annotation.Autowired
    public StoredValueService(CustomerRepository repository, FinanceService financeService,
                              AuditEventService auditEventService,
                              com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway) {
        this(repository, financeService, auditEventService, paymentGateway, Clock.systemUTC());
    }

    StoredValueService(CustomerRepository repository, FinanceService financeService,
                       AuditEventService auditEventService,
                       com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway, Clock clock) {
        this.repository = repository;
        this.financeService = financeService;
        this.auditEventService = auditEventService;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    /** 向后兼容构造：未提供支付渠道路由时用"开关关"占位路由（不调外部渠道）。供既有单测构造。 */
    StoredValueService(CustomerRepository repository, FinanceService financeService,
                       AuditEventService auditEventService, Clock clock) {
        this(repository, financeService, auditEventService,
                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), clock);
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
        return financeService.createPaymentIntent(new PaymentIntentRequest(
                key, null, null, businessOrderType, customerId, amountCent, "CNY",
                channel == null || channel.isBlank() ? "wechat" : channel));
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
        boolean merchantTopup = !"stored_value_topup".equals(intent.businessOrderType());
        if ((merchantTopup && !"stored_value_topup_merchant".equals(intent.businessOrderType()))
                || !customerId.equals(intent.businessOrderId())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment intent does not belong to this top-up");
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
                        paymentIntentId, "top-up settled", "sv_settle_" + paymentIntentId, now));
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
            return existing;
        }
        // 「只扣指定的那一张」：按 (customer, project, merchant) 精确解析卡。商户卡=该商户的卡，
        // 故商户卡消费天然限定在该发卡商户（用 merchant Y 的范围取不到 merchant X 的卡）。
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
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
            return existing;
        }
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
        if ("closed".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_CLOSED", "stored-value account is closed");
        }
        if (account.balanceCent() < amountCent) {
            throw ApiException.conflict("STORED_VALUE_INSUFFICIENT", "refund exceeds remaining balance");
        }
        long balanceAfter = account.balanceCent() - amountCent;
        String newStatus = closeAccount && balanceAfter == 0 ? "closed" : account.status();
        repository.saveStoredValueAccount(withBalanceAndStatus(account, balanceAfter, newStatus, now));
        CustomerStoredValueTransactionRecord txn = repository.saveStoredValueTransaction(
                new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "refund", -amountCent, balanceAfter,
                        null, reason, idempotencyKey, now));
        // 退款打款：系统内余额已退回（上方）。实际打款路径由支付渠道开关决定——
        //   关（默认）→ 不调外部渠道，标 reserved（人工/内部确认打款，不阻塞）。
        //   开 → 路由到 PaymentGatewayPort.refund（无真实厂商=占位 NOT_CONFIGURED，留接缝，不崩）。
        com.complex.platform.finance.gateway.PaymentGatewayResult gw = paymentGateway.refund(
                com.complex.platform.finance.gateway.PaymentGatewayRequest.of(
                        "stored_value_refund", account.id(), amountCent, null));
        Map<String, Object> detail = consumeRefundDetail(account, amountCent, balanceAfter);
        detail.put("closed", newStatus.equals("closed"));
        detail.put("payout_gateway_status", gw.status().name());
        auditEventService.recordSuccess(EVENT_REFUND, "customer.stored_value.refund",
                "customer_stored_value", account.id(), null, null, detail);
        return txn;
    }

    // ============================================================
    // 冻结 / 解冻
    // ============================================================

    @Transactional
    @GuardedAction(capability = "customer.stored_value.freeze", permission = "customer.stored_value.freeze")
    public CustomerStoredValueAccountRecord freeze(String customerId, String projectId, String merchantId, String reason) {
        Instant now = now();
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
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

    private Instant now() {
        return Instant.now(clock);
    }
}
