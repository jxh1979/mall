package com.complex.platform.customer;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.gate.GuardedAction;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 储值（预付卡 / 余额账户，gap-analysis §7A P1）。
 *
 * <p><b>预付资金合规</b>：充值不凭空加余额——充值 {@link #topup} 先经 finance 创建支付意图，
 * 支付成功后调 {@link #settleTopup} 校验支付意图已 succeeded、金额一致、未重复入账，方才入账并写流水。
 * 消费 {@link #consume} 余额不足拒；退卡 {@link #refund} 退回余额（实际退款打款待支付渠道）；
 * 冻结/解冻 {@link #freeze}/{@link #unfreeze} 阻断/恢复变动。所有余额变动写流水 + 事件。
 */
@Service
public class StoredValueService {

    private static final String EVENT_TOPUP = "stored_value.topup";
    private static final String EVENT_CONSUME = "stored_value.consume";
    private static final String EVENT_REFUND = "stored_value.refund";

    private final CustomerRepository repository;
    private final FinanceService financeService;
    private final AuditEventService auditEventService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public StoredValueService(CustomerRepository repository, FinanceService financeService,
                              AuditEventService auditEventService) {
        this(repository, financeService, auditEventService, Clock.systemUTC());
    }

    StoredValueService(CustomerRepository repository, FinanceService financeService,
                       AuditEventService auditEventService, Clock clock) {
        this.repository = repository;
        this.financeService = financeService;
        this.auditEventService = auditEventService;
        this.clock = clock;
    }

    // ============================================================
    // 充值（绑支付意图）：第一步创建支付意图（不入账），第二步支付成功后入账
    // ============================================================

    /**
     * 发起充值：创建一笔 finance 支付意图（status=created），返回该支付意图。此时余额不变。
     * 客户端完成支付后由支付渠道回调（测试期 {@code financeService.markPaymentIntentSucceeded}）置
     * succeeded，再调 {@link #settleTopup} 入账。
     */
    @Transactional
    @GuardedAction(capability = "", permission = "customer.stored_value.topup")
    public PaymentIntentRecord topup(String customerId, String projectId, long amountCent,
                                     String channel, String idempotencyKey) {
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent must be positive");
        }
        requireCustomer(customerId);
        // 提前建账户（active），充值入账时复用。
        loadOrCreateAccountForUpdate(customerId, projectId);
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? "sv_topup_" + UUID.randomUUID() : idempotencyKey;
        return financeService.createPaymentIntent(new PaymentIntentRequest(
                key, null, null, "stored_value_topup", customerId, amountCent, "CNY",
                channel == null || channel.isBlank() ? "wechat" : channel));
    }

    /**
     * 充值入账：校验绑定支付意图已成功、金额一致、未重复入账，余额 +，写 topup 流水 + 事件。
     * 无支付（支付意图非 succeeded）则拒绝——保证不凭空加余额。
     */
    @Transactional
    @GuardedAction(capability = "", permission = "customer.stored_value.topup")
    public CustomerStoredValueTransactionRecord settleTopup(String customerId, String projectId,
                                                            String paymentIntentId) {
        Instant now = now();
        requireCustomer(customerId);
        PaymentIntentRecord intent = financeService.findPaymentIntent(paymentIntentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "payment intent not found"));
        if (!"succeeded".equals(intent.status())) {
            throw ApiException.conflict("PAYMENT_NOT_SUCCEEDED", "payment intent is not succeeded");
        }
        if (!"stored_value_topup".equals(intent.businessOrderType())
                || !customerId.equals(intent.businessOrderId())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment intent does not belong to this top-up");
        }
        if (repository.storedValueTransactionExistsForPaymentIntent(paymentIntentId)) {
            return repository.findStoredValueTransactionByIdempotencyKey("sv_settle_" + paymentIntentId)
                    .orElseThrow(() -> ApiException.conflict("STORED_VALUE_ALREADY_SETTLED",
                            "top-up already settled for this payment intent"));
        }
        CustomerStoredValueAccountRecord account = loadOrCreateAccountForUpdate(customerId, projectId);
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
        auditEventService.recordSuccess(EVENT_TOPUP, "customer.stored_value.topup",
                "customer_stored_value", account.id(), null, null,
                Map.of("amount_cent", intent.amountCent(), "balance_after_cent", balanceAfter,
                        "payment_intent_id", paymentIntentId));
        return txn;
    }

    // ============================================================
    // 消费（余额扣减，不足拒，冻结阻断）
    // ============================================================

    @Transactional
    @GuardedAction(capability = "", permission = "customer.stored_value.consume")
    public CustomerStoredValueTransactionRecord consume(String customerId, String projectId,
                                                        long amountCent, String reason, String idempotencyKey) {
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent must be positive");
        }
        Instant now = now();
        CustomerStoredValueTransactionRecord existing = idempotentTxn(idempotencyKey);
        if (existing != null) {
            return existing;
        }
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId);
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
                Map.of("amount_cent", amountCent, "balance_after_cent", balanceAfter));
        return txn;
    }

    // ============================================================
    // 退卡 / 退余额（退回余额；实际退款打款待支付渠道）
    // ============================================================

    @Transactional
    @GuardedAction(capability = "", permission = "customer.stored_value.refund")
    public CustomerStoredValueTransactionRecord refund(String customerId, String projectId,
                                                       long amountCent, String reason, boolean closeAccount,
                                                       String idempotencyKey) {
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent must be positive");
        }
        Instant now = now();
        CustomerStoredValueTransactionRecord existing = idempotentTxn(idempotencyKey);
        if (existing != null) {
            return existing;
        }
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId);
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
        auditEventService.recordSuccess(EVENT_REFUND, "customer.stored_value.refund",
                "customer_stored_value", account.id(), null, null,
                Map.of("amount_cent", amountCent, "balance_after_cent", balanceAfter,
                        "closed", newStatus.equals("closed")));
        return txn;
    }

    // ============================================================
    // 冻结 / 解冻
    // ============================================================

    @Transactional
    @GuardedAction(capability = "", permission = "customer.stored_value.freeze")
    public CustomerStoredValueAccountRecord freeze(String customerId, String projectId, String reason) {
        Instant now = now();
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId);
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
    @GuardedAction(capability = "", permission = "customer.stored_value.freeze")
    public CustomerStoredValueAccountRecord unfreeze(String customerId, String projectId, String reason) {
        Instant now = now();
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId);
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

    private CustomerStoredValueAccountRecord requireAccountForUpdate(String customerId, String projectId) {
        return repository.findStoredValueAccountForUpdate(customerId, projectId)
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND",
                        "stored-value account not found"));
    }

    private CustomerStoredValueAccountRecord loadOrCreateAccountForUpdate(String customerId, String projectId) {
        return repository.findStoredValueAccountForUpdate(customerId, projectId)
                .orElseGet(() -> {
                    Instant now = now();
                    return repository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                            "svacc_" + UUID.randomUUID().toString().replace("-", ""),
                            customerId, projectId, 0L, "active", now, now));
                });
    }

    private CustomerStoredValueAccountRecord withBalanceAndStatus(CustomerStoredValueAccountRecord account,
                                                                  long balanceCent, String status, Instant now) {
        return new CustomerStoredValueAccountRecord(account.id(), account.customerId(), account.projectId(),
                balanceCent, status, account.createdAt(), now);
    }

    private CustomerRecord requireCustomer(String customerId) {
        return repository.findCustomerById(customerId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "customer not found"));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
