package com.complex.platform.customer;

// 文件版本：V0.10

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.OfflinePaymentConfirmationRecord;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.notification.NotificationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
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
import static com.complex.platform.common.Strings.trimToNull;
import com.complex.platform.common.Strings;

/**
 * 储值（预付卡 / 余额账户，gap-analysis §7A P1）。
 *
 * <p><b>预付资金合规</b>：充值不凭空加余额——充值 {@link #topup} 先经 finance 创建支付意图，
 * 支付成功后调 {@link #settleTopup} 校验支付意图已 succeeded、金额一致、未重复入账，方才入账并写流水。
 * 消费 {@link #payTicketOrder} 余额不足拒；退卡 {@link #refund} 退回余额（实际退款打款待支付渠道）；
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
    private static final String EVENT_COUNTER_REFUND = "stored_value.counter_refund";
    private static final String NOTIFICATION_DOMAIN = "stored_value";
    private static final String NOTIFICATION_EVENT_TOPUP = "topup";
    private static final String NOTIFICATION_EVENT_CONSUME = "consume";
    private static final String STORED_VALUE_TOPUP_TEMPLATE = "sms.stored_value.topup";
    private static final String STORED_VALUE_CONSUME_TEMPLATE = "sms.stored_value.consume";

    /** 商户卡充值资金归属：merchant_prepaid=发卡商户预收（默认）；platform_collect=平台代收、消费时结算给商户。 */
    static final String FUND_MERCHANT_PREPAID = "merchant_prepaid";
    static final String FUND_PLATFORM_COLLECT = "platform_collect";

    // AUD-002 操作命名空间隔离：内部派生幂等键的保留前缀。客户端/后台传入的幂等键不得占用，
    // 否则可预占 sv_settle_<intentId> 使正常充值入账永久冲突，或伪造 barsv_ 命中错误重放。
    // svcr_ = 柜台退款命名空间（复核发现：与线上退款共享命名空间时，跨路径同键重放会把线上退款流水
    // 错当柜台退款成功返回——不落柜台审计/凭证还可能引发双重兑付）。
    private static final String[] RESERVED_IDEMPOTENCY_PREFIXES = {
            "sv_settle_", "sv_topup_", "svtop_", "barsv_", "svtxn_", "svacc_", "svcr_", "ticketsv_"};
    private static final String COUNTER_REFUND_KEY_PREFIX = "svcr_";

    private final CustomerRepository repository;
    private final FinanceService financeService;
    private final AuditEventService auditEventService;
    private final com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway;
    // B1d⑤ phase4 员工端卡范围隔离：商户范围员工只能操作本商户的商户卡。可为 null=旧构造/兼容测试（不强制）。
    private final com.complex.platform.access.AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    private CustomerService customerService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.complex.platform.ticketing.TicketingRepository ticketingRepository;

    // B1d⑤ 商户卡充值资金归属默认值（开关，可后台/配置覆盖；默认商户预收）。
    @Value("${stored-value.merchant-card.fund-ownership:merchant_prepaid}")
    private String fundOwnershipDefault = FUND_MERCHANT_PREPAID;

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

    // ============================================================
    // 充值（绑支付意图）：第一步创建支付意图（不入账），第二步支付成功后入账
    // ============================================================

    /**
     * 发起充值：创建一笔 finance 支付意图（status=created），返回该支付意图。此时余额不变。
     * 客户端按真实渠道拉起信息支付，再由 {@link #settleTopup} 向渠道核验成功回执并入账。
     * merchantId 非空=给商户级卡充值；柜台 POS 确认仍由独立的线下确认入口办理。
     */
    @Transactional
    @GuardedAction(capability = "customer.stored_value.topup", permission = "customer.stored_value.topup")
    public PaymentIntentRecord topup(String customerId, String projectId, String merchantId, long amountCent,
                                     String channel, String idempotencyKey) {
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "金额必须大于 0");
        }
        requireClientIdempotencyKey(idempotencyKey);
        requireCustomer(customerId);
        // 发卡资格门控（AUD-061）：商户卡充值前须平台已给该商户开通「发卡资格」，默认关→拒。通用卡不受限。
        requireCardIssuanceEnabled(merchantId);
        // 提前建账户（active），充值入账时复用（开卡即从商户配置定格资金模式）。
        var card = loadOrCreateAccountForUpdate(customerId, projectId, merchantId);
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? "sv_topup_" + UUID.randomUUID() : idempotencyKey;
        // B1d⑤ 资金归属：商户卡走 stored_value_topup_merchant（finance 据此把预收记入发卡商户，按开关结算）；
        // 通用卡仍走 stored_value_topup（平台综合体级余额）。businessOrderId=customerId 保持既有归属链。
        String businessOrderType = merchantId == null ? "stored_value_topup" : "stored_value_topup_merchant";
        // AUD 复核 P1-1①：finance 幂等作用域必须纳入卡范围（customer/project/merchant）。PaymentIntentRequest 不带 merchantId/projectId，
        // 仅凭客户端 idempotencyKey 会使不同商户卡的同 key 请求哈希相同 → 复用同一支付意图，随后 bind 覆盖 scope 造成静默改绑。
        // 故把卡范围折进 finance 幂等键并 SHA-256 压成定长（避免 slug 40 字截断致 ON CONFLICT 覆盖旧意图）；相同 (key, 卡范围) 仍得同一意图，正常重试幂等不受影响。
        String scopedTopupKey = "svtop_" + sha256Hex(key + "|" + customerId + "|"
                + (projectId == null ? "" : projectId) + "|" + (merchantId == null ? "" : merchantId));
        // XM-ONESHOT-RETURN B02 审查修正（操作员 2026-09-25 确认的追加句「同一次操作重复提交时，返回第一次的结果并提示已完成」）：
        // svtop_ 键由本请求的幂等键派生，幂等层回放它＝本请求重复提交，响应提示已完成。
        IdempotentReplayContext.registerDerivedKey(idempotencyKey, scopedTopupKey);
        // AUD-013 资金归属契约：支付意图/网关请求显式携带收款商户与定格资金模式（merchant_prepaid=直达商户，
        // platform_collect=平台代收），真实支付适配器据此路由收款主体，杜绝「支付进平台、账务认商户预收」错配。
        PaymentIntentRecord intent = financeService.createPaymentIntent(new PaymentIntentRequest(
                scopedTopupKey, null, null, businessOrderType, customerId, amountCent, "CNY",
                channel == null || channel.isBlank() ? "wechat" : channel, null,
                merchantId, effectiveFundOwnership(card)));
        // AUD-058 + AUD 复核 P1-1②：把本次充值的卡范围（customer/project/merchant）绑定到支付意图，settle 只认此绑定范围入账。
        // bind「冲突即校验一致」：同一支付意图已绑定的卡范围与本次不同 → 抛 409（不再 DO UPDATE 静默改绑）；完全一致才幂等放过。
        if (repository.bindStoredValueTopupIntent(intent.id(), customerId, projectId, merchantId, now()) == 0) {
            throw ApiException.conflict("IDEMPOTENCY_KEY_CONFLICT",
                    "该支付意向已绑定到其他卡范围");
        }
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
        StoredValueTopupIntentScope bound = repository.findStoredValueTopupIntent(paymentIntentId)
                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "该支付意向不属于储值充值"));
        if (!customerId.equals(bound.customerId())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "该支付意向不属于本次充值");
        }
        if (!java.util.Objects.equals(projectId, bound.projectId())
                || !java.util.Objects.equals(merchantId, bound.merchantId())) {
            throw ApiException.conflict("STORED_VALUE_SCOPE_MISMATCH", "结算卡范围与充值卡范围不一致");
        }
        if (paymentGateway == null || !paymentGateway.isGatewayEnabled()) {
            throw ApiException.conflict("PAYMENT_GATEWAY_UNAVAILABLE", "支付渠道未启用");
        }
        CustomerStoredValueAccountRecord card = repository.findStoredValueAccount(customerId, projectId, merchantId)
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND", "储值卡不存在"));
        financeService.confirmOnlinePaymentIntent(paymentIntentId, merchantId,
                merchantId == null ? null : effectiveFundOwnership(card));
        return settleTopupInternal(customerId, projectId, merchantId, paymentIntentId, true);
    }

    /**
     * AUD-014 柜台确认收款即入账：财务柜台确认线下收款（同事务内 CAS 支付意图 created→succeeded）
     * 后立即结转本笔充值入账，同成同败——柜台确认成功=余额到账，不再停留 created/409。
     * 权限走 finance 确认闸（finance.payment.register），入账复用绑定卡范围，不重复过客户充值闸。
     */
    @Transactional
    public OfflinePaymentConfirmationRecord confirmOfflineTopupAndSettle(String paymentId,
                                                                         OfflinePaymentConfirmRequest request) {
        OfflinePaymentConfirmationRecord confirmation = financeService.confirmOfflinePayment(paymentId, request);
        StoredValueTopupIntentScope bound = repository.findStoredValueTopupIntent(paymentId)
                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED",
                        "该支付意向不属于储值充值"));
        settleTopupInternal(bound.customerId(), bound.projectId(), bound.merchantId(), paymentId, false);
        return confirmation;
    }

    /**
     * 入账内核（不带客户充值权限闸）：仅供 {@link #settleTopup} 与柜台确认链两个带闸入口复用，不对外开放。
     * customerSettle=true 仅限客人入账入口：命中「已入账」时返回的就是本次响应，据此提示已完成；柜台确认链返回的是确认单，不在此置位。
     */
    private CustomerStoredValueTransactionRecord settleTopupInternal(String customerId, String projectId,
                                                                     String merchantId, String paymentIntentId,
                                                                     boolean customerSettle) {
        Instant now = now();
        requireCustomer(customerId);
        PaymentIntentRecord intent = financeService.findPaymentIntentForUpdate(paymentIntentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在或已被删除"));
        if (!"succeeded".equals(intent.status())) {
            throw ApiException.conflict("PAYMENT_NOT_SUCCEEDED", "该支付尚未成功");
        }
        // AUD-058：入账账户范围只认充值时绑定的卡范围（customer/project/merchant），请求体范围必须与之一致，
        // 否则=通用卡充值被指名入商户卡/跨商户/跨项目。绑定行由 topup 落库，按支付意图 id 读回校验。
        StoredValueTopupIntentScope bound = repository.findStoredValueTopupIntent(paymentIntentId)
                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED",
                        "该支付意向不属于本次充值"));
        if (!customerId.equals(bound.customerId())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "该支付意向不属于本次充值");
        }
        if (!java.util.Objects.equals(projectId, bound.projectId())
                || !java.util.Objects.equals(merchantId, bound.merchantId())) {
            throw ApiException.conflict("STORED_VALUE_SCOPE_MISMATCH",
                    "结算卡范围与充值卡范围不一致");
        }
        if (repository.storedValueTransactionExistsForPaymentIntent(paymentIntentId)) {
            CustomerStoredValueTransactionRecord first = repository.findStoredValueTransactionByIdempotencyKey(
                            "sv_settle_" + paymentIntentId)
                    .orElseThrow(() -> ApiException.conflict("STORED_VALUE_ALREADY_SETTLED",
                            "该支付意向对应的充值已入账，请勿重复操作"));
            // XM-ONESHOT-RETURN B02 审查修正（操作员 2026-09-25 确认的追加句「同一次操作重复提交时，返回第一次的结果并提示已完成」）：
            // 同一支付意向再次入账＝同一次操作重复提交，排在范围、支付状态各项拒绝之后返回第一次的充值流水并提示已完成。
            // 客人端每次写请求换新的请求编号，故按支付意向认、不按请求编号认。
            if (customerSettle) {
                IdempotentReplayContext.markCurrentReplayed();
            }
            return first;
        }
        CustomerStoredValueAccountRecord account = loadOrCreateAccountForUpdate(customerId, projectId, merchantId);
        if ("closed".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_CLOSED", "储值账户已销户");
        }
        // CG-06a（ENUM「储值卡类型与收款主体」：每笔充值拆为本金与赠送金两段分别记账，赠送比例按项目可配）：
        // 本金 = 实付；赠送金 = 实付 × 项目赠送比例（仅综合体通用卡；商户级卡赠送口径 HTML 未写，赠送金 0）。
        long bonusCent = account.merchantId() == null
                ? repository.findStoredValueBonusPolicy(StoredValueBonusPolicyRecord.scopeOf(account.projectId()))
                        .map(policy -> MoneyRounding.bpsOf(intent.amountCent(), policy.bonusBps())).orElse(0L)
                : 0L;
        long principalAfter = account.principalCent() + intent.amountCent();
        long bonusAfter = account.bonusCent() + bonusCent;
        long balanceAfter = principalAfter + bonusAfter;
        repository.saveStoredValueAccount(withSplit(account, principalAfter, bonusAfter, account.status(), now));
        CustomerStoredValueTransactionRecord txn = repository.saveStoredValueTransaction(
                new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "topup", intent.amountCent() + bonusCent, balanceAfter,
                        // V231：充值即支付，paid_at=交易完成时刻 now（与写流水同事务）。
                        paymentIntentId, "top-up settled", "sv_settle_" + paymentIntentId, now, now,
                        intent.amountCent(), bonusCent));
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("amount_cent", intent.amountCent());
        detail.put("principal_cent", intent.amountCent());
        detail.put("bonus_cent", bonusCent);
        detail.put("principal_after_cent", principalAfter);
        detail.put("bonus_after_cent", bonusAfter);
        detail.put("balance_after_cent", balanceAfter);
        detail.put("payment_intent_id", paymentIntentId);
        detail.put("card_scope", account.cardScope());
        if (account.merchantId() != null) {
            detail.put("merchant_id", account.merchantId());
            // AUD-059 商户卡资金归属：用开卡时定格在卡上的 fund_ownership（非全局默认）；旧卡未定格按商户预收兜底显示。
            detail.put("fund_ownership", effectiveFundOwnership(account));
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
    public com.complex.platform.ticketing.TicketOrderRecord payTicketOrder(
            String customerId, String orderId, String accountId, String phone, String smsCode) {
        if (authorizationService == null || customerService == null || ticketingRepository == null) {
            throw ApiException.forbidden("STORED_VALUE_UNAVAILABLE", "储值订单支付当前不可用");
        }
        var actor = authorizationService.currentActor();
        if (!"customer".equals(actor.accountType()) || !"active".equals(actor.status())
                || !customerId.equals(actor.id())) {
            throw ApiException.forbidden("STORED_VALUE_CUSTOMER_MISMATCH", "仅可支付本人订单");
        }
        var order = ticketingRepository.findTicketOrderByIdForUpdate(Strings.requireText(orderId, "订单编号"))
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "订单不存在"));
        if (!customerId.equals(order.customerId())) {
            throw ApiException.forbidden("STORED_VALUE_CUSTOMER_MISMATCH", "仅可支付本人订单");
        }
        if ("cancelled".equals(order.status())) {
            throw ApiException.conflict("ORDER_CANCELLED", "订单已取消");
        }
        if ("paid".equals(order.payStatus())) return order;
        if (!"created".equals(order.status()) || !"unpaid".equals(order.payStatus())) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "订单不在待支付状态");
        }
        CustomerStoredValueAccountRecord card = repository.listStoredValueAccountsByCustomer(customerId).stream()
                .filter(a -> a.id().equals(accountId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND", "储值卡不存在"));
        // 当前票务订单未携带项目范围，不将有限定项目的卡当成全局卡使用。
        if (card.projectId() != null || (card.merchantId() != null && !card.merchantId().equals(order.barId()))) {
            throw ApiException.conflict("STORED_VALUE_SCOPE_MISMATCH", "所选储值卡不适用于该订单范围");
        }
        if (!"CNY".equals(order.currency())) {
            throw ApiException.conflict("PAYMENT_CURRENCY_MISMATCH", "订单币种与储值余额不一致");
        }
        customerService.verifyStoredValueCustomer(customerId, phone, smsCode);
        CustomerStoredValueTransactionRecord debit = consumeBalance(customerId, card.projectId(), card.merchantId(), order.amountCent(),
                "票务订单储值支付 " + order.id(), "ticketsv_" + order.id(), true);
        Instant paidAt = now();
        ticketingRepository.saveTicketPaymentIntent(new com.complex.platform.ticketing.TicketPaymentIntentRecord(
                debit.id(), "ticket_order", order.id(), order.amountCent(), order.currency(),
                "stored_value", "succeeded", paidAt, paidAt));
        return ticketingRepository.updateTicketOrder(new com.complex.platform.ticketing.TicketOrderRecord(
                order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(), order.priceRuleId(),
                order.quantity(), order.visitDate(), order.salesChannel(), order.buyerPhone(), order.amountCent(),
                order.dynamicPricingRuleId(), order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
                "paid", order.issueStatus(), order.status(), order.createdAt(), paidAt, order.merchantId(),
                paidAt, order.barId()));
    }

    /**
     * 内部消费（酒吧结账-储值支付等已过各自权限闸的域调用）：不重复过 customer.stored_value.consume 权限闸，
     * 但仍走卡范围/状态/余额守卫，并按开卡定格的资金模式对 platform_collect 商户卡逐笔挂应付商户。
     * 仅供本域及已授权域内部调用，不暴露控制器入口。@Transactional(REQUIRED)：并入调用方事务，同成同败。
     */
    @Transactional
    public CustomerStoredValueTransactionRecord consumeInternal(String customerId, String projectId, String merchantId,
                                                        long amountCent, String reason, String idempotencyKey) {
        return consumeBalance(customerId, projectId, merchantId, amountCent, reason, idempotencyKey, false);
    }

    private CustomerStoredValueTransactionRecord consumeBalance(String customerId, String projectId, String merchantId,
            long amountCent, String reason, String idempotencyKey, boolean verifiedCustomerOrder) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "金额必须大于 0");
        }
        Instant now = now();
        CustomerStoredValueTransactionRecord existing = idempotentTxn(idempotencyKey);
        if (existing != null) {
            // AUD-002: 幂等命中不能只看 customerId——须核对完整不可变指纹(操作类型/金额/卡范围/业务事由)，
            // 否则同客户可用旧 consume 键让酒吧结账「命中旧流水不扣本次应付」仍标已支付，或跨卡/跨金额错误重放。
            return verifyIdempotentReplay(existing, customerId, projectId, merchantId,
                    "consume", -amountCent, reason);
        }
        // 「只扣指定的那一张」：按 (customer, project, merchant) 精确解析卡。商户卡=该商户的卡，
        // 故商户卡消费天然限定在该发卡商户（用 merchant Y 的范围取不到 merchant X 的卡）。
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
        // 清单 P1-1:与 refund/freeze/unfreeze 一致,消费须先过卡范围校验(他商户/通用卡越权即拒,
        // 且在状态检查之前——不向越权者泄露卡状态)。此前缺此行 → merchantId 请求体可控可跨商户扣款。
        if (!verifiedCustomerOrder) requireMerchantScopeForCard(account);
        requireActive(account);
        if (account.principalCent() + account.bonusCent() < amountCent) {
            throw ApiException.conflict("STORED_VALUE_INSUFFICIENT", "储值余额不足");
        }
        // CG-06a（ENUM「储值卡类型与收款主体」/ BSM「支付方式」）：通用卡写死先扣本金、后扣赠送金；
        // 商户级卡按发卡商户自定、平台运营代录的扣款顺序（stored_value_merchant_refund_rules.consume_order）。
        long[] parts = splitDeduction(account, amountCent, bonusFirstForCard(account));
        long principalAfter = account.principalCent() - parts[0];
        long bonusAfter = account.bonusCent() - parts[1];
        long balanceAfter = principalAfter + bonusAfter;
        repository.saveStoredValueAccount(withSplit(account, principalAfter, bonusAfter, account.status(), now));
        CustomerStoredValueTransactionRecord txn = repository.saveStoredValueTransaction(
                new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "consume", -amountCent, balanceAfter,
                        null, reason, idempotencyKey, now, null, -parts[0], -parts[1]));
        Map<String, Object> consumeDetail = consumeRefundDetail(account, amountCent, balanceAfter);
        consumeDetail.put("principal_cent", parts[0]);
        consumeDetail.put("bonus_cent", parts[1]);
        consumeDetail.put("principal_after_cent", principalAfter);
        consumeDetail.put("bonus_after_cent", bonusAfter);
        auditEventService.recordSuccess(EVENT_CONSUME, "customer.stored_value.consume",
                "customer_stored_value", account.id(), null, null, consumeDetail);
        // AUD-059 平台代收(platform_collect)：消费即时逐笔生成一条「平台应付商户」台账（逐笔可追溯，结算给发卡商户）。
        // 仅对开卡定格为 platform_collect 的商户卡；商户预收/通用卡/旧卡(未定格)不挂账（钱已归商户或平台无代收关系）。
        if (FUND_PLATFORM_COLLECT.equals(account.fundOwnership()) && account.merchantId() != null) {
            financeService.recordStoredValueMerchantPayable(account.merchantId(), txn.id(), amountCent, idempotencyKey);
        }
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
        requireClientIdempotencyKey(idempotencyKey);
        return doRefund(customerId, projectId, merchantId, amountCent, reason, closeAccount,
                idempotencyKey, false, null);
    }

    /**
     * AUD-003 本店柜台/到店退款：商户预收(merchant_prepaid)卡的唯一可达退款路径（线上退对其无条件拒）。
     * 仅限商户卡；staff-token 走权限闸 + {@link #requireMerchantScopeForCard} 发卡商户 scope；
     * 仍过发卡商户退款规则(可否退/比例)；必填兑付凭证号(现场现金/原路退付的凭据)入审计留痕。
     * 款项由柜台现场兑付，不走线上打款网关/退款台账。
     */
    @Transactional
    @GuardedAction(capability = "customer.stored_value.refund", permission = "customer.stored_value.refund")
    public CustomerStoredValueTransactionRecord counterRefund(String customerId, String projectId, String merchantId,
                                                              long amountCent, String reason, boolean closeAccount,
                                                              String redemptionVoucherNo, String idempotencyKey) {
        if (merchantId == null || merchantId.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "柜台退款仅适用于商户卡");
        }
        String voucher = trimToNull(redemptionVoucherNo);
        if (voucher == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "redemption_voucher_no is required");
        }
        requireClientIdempotencyKey(idempotencyKey);
        if (idempotencyKey != null && idempotencyKey.length() > 120) {
            throw ApiException.badRequest("VALIDATION_FAILED", "幂等键长度超出限制");
        }
        // 柜台退款独立幂等命名空间：同一客户端键在线上/柜台两条路径各自幂等，互不重放（复核 Important 项）。
        String namespacedKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? idempotencyKey : COUNTER_REFUND_KEY_PREFIX + idempotencyKey;
        return doRefund(customerId, projectId, merchantId, amountCent, reason, closeAccount,
                namespacedKey, true, voucher);
    }

    private CustomerStoredValueTransactionRecord doRefund(String customerId, String projectId, String merchantId,
                                                          long amountCent, String reason, boolean closeAccount,
                                                          String idempotencyKey, boolean counterMode,
                                                          String redemptionVoucherNo) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "金额必须大于 0");
        }
        Instant now = now();
        CustomerStoredValueTransactionRecord existing = idempotentTxn(idempotencyKey);
        if (existing != null) {
            // M724-F10（治理地图第 528 行红线④「看不到别的商户和别的项目的数据」；枚举速查第 86 行「商户员工只能管理本商户发行的储值卡」）：
            // 返回第一次的结果之前，先核对当前主体能不能管这笔流水所在的那张卡——按流水实际所属的卡判，不信请求里传的商户字段。
            // 不在范围内即拒绝（与首次退款同一错误码），不比对请求内容、不返回流水、不标记重放；线上退款与柜台退款共用本方法。
            // M724-F15（同一条红线④「看不到别的商户和别的项目的数据」；治理地图第 653 行「数据的项目与商户隔离不提供关闭开关」）：
            // 商户之外还核项目——同商户另一个项目的员工用原提交编号、原请求内容重提，同样在这里拒绝，不返回流水、不标记重放。
            CustomerStoredValueAccountRecord firstCard = repository.listStoredValueAccountsByCustomer(existing.customerId()).stream()
                    .filter(card -> card.id().equals(existing.accountId())).findFirst()
                    .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND", "储值账户不存在或已被删除"));
            requireMerchantScopeForCard(firstCard);
            requireProjectScopeForCard(firstCard);
            // AUD-002: 幂等命中核对完整不可变指纹(操作类型/金额/卡范围/业务事由)，不再只看 customerId。
            // 路径隔离由键命名空间保证（柜台=svcr_ 前缀），线上/柜台不会互相命中。
            CustomerStoredValueTransactionRecord first = verifyIdempotentReplay(existing, customerId, projectId, merchantId,
                    "refund", -amountCent, reason);
            verifyRefundReplayRequest(existing, counterMode, closeAccount, redemptionVoucherNo);
            // XM-ONESHOT-RETURN B02（操作员 2026-09-25 确认的追加句「同一次操作重复提交时，返回第一次的结果并提示已完成」）：
            // 返回第一次的退款流水＝本请求重复提交，响应提示已完成；柜台退款按去掉 svcr_ 命名空间前缀的原键认。
            IdempotentReplayContext.markReplayed(counterMode
                    ? idempotencyKey.substring(COUNTER_REFUND_KEY_PREFIX.length()) : idempotencyKey);
            return first;
        }
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(customerId, projectId, merchantId);
        requireMerchantScopeForCard(account);
        if ("closed".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_CLOSED", "储值账户已销户");
        }
        if (account.balanceCent() < amountCent) {
            throw ApiException.conflict("STORED_VALUE_INSUFFICIENT", "退款金额超过剩余余额");
        }
        // CG-06a（ENUM「储值卡类型与收款主体」：「退卡只退未消费的本金，赠送金不退现」）：退款额不得超过未消费本金。
        if (account.principalCent() < amountCent) {
            throw ApiException.conflict("STORED_VALUE_PRINCIPAL_INSUFFICIENT",
                    "退款金额超过未消费本金；赠送金不退现");
        }
        // B1d⑤ phase3 商户级卡退款规则：仅对商户卡（merchantId 非空）应用发卡商户自设规则。
        //   refund_allowed=false → 拒退；退款比例=实际退回客户的比例，余下为商户留存（手续费/违约金）。
        //   综合体通用卡（merchantId 空）无规则=全额退（payout=amountCent），沿用既有行为。
        long payoutCent = amountCent;
        if (account.merchantId() != null) {
            // AUD-060 预收模式退卡：商户预收(merchant_prepaid)卡不支持线上退，只能到店由发卡商户办理（钱在商户处）。
            // AUD-003 柜台路径(counterMode)即「到店办理」本身——放行 merchant_prepaid，其余守卫全保留。
            if (!counterMode && FUND_MERCHANT_PREPAID.equals(account.fundOwnership())) {
                throw ApiException.conflict("STORED_VALUE_MERCHANT_PREPAID_REFUND_IN_STORE",
                        "商户预付卡不支持线上退款，请到发卡商户门店办理");
            }
            StoredValueMerchantRefundRuleRecord rule = repository.findStoredValueMerchantRefundRule(account.merchantId())
                    .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(account.merchantId()));
            if (!rule.refundAllowed()) {
                throw ApiException.conflict("STORED_VALUE_REFUND_NOT_ALLOWED",
                        "发卡商户不允许该商户卡退款");
            }
            // R-7:退卡实退额按比例四舍五入到分(旧整除截断,客户恒少退、商户多留且不留痕)。
            payoutCent = MoneyRounding.bpsOf(amountCent, rule.refundRatioBps());
        }
        long principalAfter = account.principalCent() - amountCent;
        // CG-06a（ENUM「储值卡类型与收款主体」：「销户时……再退剩余本金，赠送金清零」；BSM「储值预付卡 / 余额」：
        // 「赠送金不退现、未使用部分按活动规则作废或收回」）：销户（本金退清）时未使用赠送金作废，单独留一条 adjust 流水。
        boolean closing = closeAccount && principalAfter == 0;
        long bonusVoidCent = closing ? account.bonusCent() : 0L;
        long bonusAfter = account.bonusCent() - bonusVoidCent;
        long balanceAfter = principalAfter + bonusAfter;
        String newStatus = closing ? "closed" : account.status();
        repository.saveStoredValueAccount(withSplit(account, principalAfter, bonusAfter, newStatus, now));
        CustomerStoredValueTransactionRecord txn = repository.saveStoredValueTransaction(
                new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "refund", -amountCent, principalAfter + account.bonusCent(),
                        null, reason, idempotencyKey, now, null, -amountCent, 0L));
        if (bonusVoidCent > 0) {
            repository.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
                    "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                    account.id(), customerId, "adjust", -bonusVoidCent, balanceAfter,
                    null, "销户：未使用赠送金作废（不退现）", idempotencyKey + "|bonus_void", now, null,
                    0L, -bonusVoidCent));
        }
        if (counterMode) {
            // AUD-003 柜台兑付：款项由发卡商户柜台现场支付给客户（现金/原路人工），不走线上打款网关；
            // 兑付凭证号入审计事件供对账/稽核。
            Map<String, Object> counterDetail = consumeRefundDetail(account, amountCent, balanceAfter);
            counterDetail.put("principal_cent", amountCent);
            counterDetail.put("bonus_voided_cent", bonusVoidCent);
            counterDetail.put("closed", newStatus.equals("closed"));
            counterDetail.put("payout_cent", payoutCent);
            counterDetail.put("payout_status", "counter_settled");
            counterDetail.put("redemption_voucher_no", redemptionVoucherNo);
            counterDetail.put("close_account", closeAccount);
            auditEventService.recordSuccess(EVENT_COUNTER_REFUND, "customer.stored_value.refund",
                    "customer_stored_value", account.id(), null, idempotencyKey, counterDetail);
            return txn;
        }
        // 退款打款：系统内余额已退回（上方）。实际打款金额=payoutCent（商户卡按比例）。打款路径由支付渠道开关决定——
        //   关（默认）→ 不调外部渠道，标 reserved（人工/内部确认打款，不阻塞）。
        //   开 → 路由到 PaymentGatewayPort.refund（无真实厂商=占位 NOT_CONFIGURED，留接缝，不崩）。
        com.complex.platform.finance.gateway.PaymentGatewayResult gw = paymentGateway.refund(
                com.complex.platform.finance.gateway.PaymentGatewayRequest.of(
                        "stored_value_refund", account.id(), payoutCent, null));
        // AUD-060：退款打款状态如实反映，不得一律记成功。
        //   FAILED（渠道明确失败）→ 整单回滚（@Transactional）：余额不扣、不写流水，避免"扣了余额没退钱还显示成功"。
        if (gw.status() == com.complex.platform.finance.gateway.PaymentGatewayResult.Status.FAILED) {
            throw ApiException.conflict("STORED_VALUE_REFUND_FAILED", "退款打款在支付网关失败，请稍后重试");
        }
        //   非 SUCCEEDED（RESERVED/NOT_CONFIGURED/PENDING=未真正兑付）→ 复用财务退款状态机登记一笔 requested 的
        //   退款台账（内部/手工确认打款），让实际打款成为可追踪待办而非仅留审计面包屑；SUCCEEDED（渠道已兑付）→ settled。
        //   payoutCent=0（商户规则退款比例 0%）无需打款，不登记。
        boolean payoutSettled = gw.status() == com.complex.platform.finance.gateway.PaymentGatewayResult.Status.SUCCEEDED;
        String payoutRefundId = payoutSettled || payoutCent <= 0 ? null
                : financeService.recordStoredValuePayoutRefund(account.id(), payoutCent, reason,
                        gw.requiresInternalFallback() ? "manual" : gw.gatewayProvider(), idempotencyKey);
        Map<String, Object> detail = consumeRefundDetail(account, amountCent, balanceAfter);
        detail.put("principal_cent", amountCent);
        detail.put("bonus_voided_cent", bonusVoidCent);
        detail.put("closed", newStatus.equals("closed"));
        detail.put("payout_cent", payoutCent);
        detail.put("payout_gateway_status", gw.status().name());
        detail.put("payout_status", payoutSettled ? "settled" : "requested");
        if (payoutRefundId != null) {
            detail.put("payout_refund_id", payoutRefundId);
        }
        detail.put("close_account", closeAccount);
        // 审计幂等键＝流水幂等键：同键重放据此找回第一次请求的 close_account（见 verifyRefundReplayRequest）
        auditEventService.recordSuccess(EVENT_REFUND, "customer.stored_value.refund",
                "customer_stored_value", account.id(), null, idempotencyKey, detail);
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
            throw ApiException.badRequest("VALIDATION_FAILED", "退还比例必须在 0 到 10000 基点之间");
        }
        // phase4 卡范围隔离：商户范围员工只能配置本商户的退款规则。
        requireMerchantScopeForMerchant(merchantId.trim());
        Instant now = now();
        // 保留平台侧配置（发卡资格/资金模式）：商户改退款规则不得清掉平台开通的发卡资格与定格的资金模式。
        StoredValueMerchantRefundRuleRecord existing = repository.findStoredValueMerchantRefundRule(merchantId.trim())
                .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(merchantId.trim()));
        return repository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId.trim(), refundAllowed, refundRatioBps,
                existing.cardIssuanceEnabled(), existing.fundOwnership(),
                existing.createdAt() == null ? now : existing.createdAt(), now));
    }

    @GuardedAction(capability = "customer.stored_value.refund", permission = "customer.stored_value.refund")
    public StoredValueMerchantRefundRuleRecord getMerchantRefundRule(String merchantId) {
        // AUD-062：查询商户退款规则须过权限闸 + 商户 scope（与 configureMerchantRefundRule 一致），
        // 否则任意店员/跨商户可读任意商户的退款规则配置。
        requireMerchantScopeForMerchant(merchantId);
        return repository.findStoredValueMerchantRefundRule(merchantId)
                .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(merchantId));
    }

    /**
     * 商户卡平台配置（AUD-061）：开通/关闭发卡资格 + 设资金模式（预收/代收）。仅平台可改
     * （requirePlatformScope 拒 merchant-scoped 调用方）；资金模式只影响之后新开的卡（开卡时定格），已发卡不变。
     * 发卡资格/资金模式与商户自设的退款规则并存于同一行，读改存互不覆盖。
     */
    @Transactional
    @GuardedAction(capability = "customer.stored_value.refund", permission = "customer.stored_value.refund")
    public StoredValueMerchantRefundRuleRecord configureMerchantCard(String merchantId, boolean cardIssuanceEnabled,
                                                                     String fundOwnership) {
        return configureMerchantCard(merchantId, cardIssuanceEnabled, fundOwnership, null);
    }

    /**
     * CG-06a（BSM「支付方式」：「商户级卡消费扣款顺序由发卡商户自定、由平台运营代为录入后台」）：
     * consumeOrder=principal_first / bonus_first；为空保留现值（缺省先本金后赠送金）。
     */
    @Transactional
    @GuardedAction(capability = "customer.stored_value.refund", permission = "customer.stored_value.refund")
    public StoredValueMerchantRefundRuleRecord configureMerchantCard(String merchantId, boolean cardIssuanceEnabled,
                                                                     String fundOwnership, String consumeOrder) {
        if (merchantId == null || merchantId.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "merchant_id is required");
        }
        String order = consumeOrder == null || consumeOrder.isBlank() ? null : consumeOrder.trim();
        if (order != null && !StoredValueMerchantRefundRuleRecord.CONSUME_PRINCIPAL_FIRST.equals(order)
                && !StoredValueMerchantRefundRuleRecord.CONSUME_BONUS_FIRST.equals(order)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "扣款顺序必须为先本金后赠送金（principal_first）或先赠送金后本金（bonus_first）");
        }
        String mode = fundOwnership == null || fundOwnership.isBlank() ? FUND_MERCHANT_PREPAID : fundOwnership.trim();
        if (!FUND_MERCHANT_PREPAID.equals(mode) && !FUND_PLATFORM_COLLECT.equals(mode)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "资金归属必须为商户预收（merchant_prepaid）或平台代收（platform_collect）");
        }
        // 仅平台可改：fail-closed 拒商户/外包 scope 调用方（authorizationService 为 null=旧构造/单测不强制）。
        if (authorizationService != null) {
            authorizationService.requirePlatformScope();
        }
        Instant now = now();
        StoredValueMerchantRefundRuleRecord existing = repository.findStoredValueMerchantRefundRule(merchantId.trim())
                .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(merchantId.trim()));
        return repository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId.trim(), existing.refundAllowed(), existing.refundRatioBps(),
                cardIssuanceEnabled, mode,
                existing.createdAt() == null ? now : existing.createdAt(), now,
                order == null ? existing.consumeOrder() : order));
    }

    // ============================================================
    // CG-06a 通用卡充值赠送比例（业务参数，按项目可配，须同时配置上下限；仅平台可改）
    // ============================================================

    @Transactional
    @GuardedAction(capability = "customer.stored_value.refund", permission = "customer.stored_value.refund")
    public StoredValueBonusPolicyRecord configureBonusPolicy(String projectId, Integer bonusBps, Integer minBps,
                                                             Integer maxBps, String reason) {
        if (authorizationService != null) {
            authorizationService.requirePlatformScope();
        }
        String scope = StoredValueBonusPolicyRecord.scopeOf(projectId);
        try {
            // ENUM「储值卡类型与收款主体」：「赠送比例可配（业务参数，按项目可配，须同时配置上下限）」——值与上下限同在，取值落在闭区间内。
            if (bonusBps == null || minBps == null || maxBps == null
                    || minBps < 0 || maxBps > 10_000 || minBps > bonusBps || bonusBps > maxBps) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "须同时填写赠送比例及其上下限（万分比，0-10000），赠送比例须在上下限内");
            }
            String actor = authorizationService == null ? null : authorizationService.currentActor().id();
            StoredValueBonusPolicyRecord saved = repository.saveStoredValueBonusPolicy(new StoredValueBonusPolicyRecord(
                    scope, bonusBps, minBps, maxBps, actor, now()));
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("project_scope", scope);
            detail.put("bonus_bps", bonusBps);
            detail.put("min_bps", minBps);
            detail.put("max_bps", maxBps);
            detail.put("reason", reason == null ? "" : reason);
            auditEventService.recordSuccess("stored_value.bonus_policy_configured", "customer.stored_value.refund",
                    "stored_value_bonus_policy", scope, null, null, detail);
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure("stored_value.bonus_policy_config_failed", "customer.stored_value.refund",
                    "stored_value_bonus_policy", scope, null, null, e.code());
            throw e;
        }
    }

    public StoredValueBonusPolicyRecord getBonusPolicy(String projectId) {
        if (authorizationService != null) {
            authorizationService.requirePlatformScope();
        }
        String scope = StoredValueBonusPolicyRecord.scopeOf(projectId);
        return repository.findStoredValueBonusPolicy(scope)
                .orElse(new StoredValueBonusPolicyRecord(scope, 0, 0, 0, null, null));
    }

    /** 发卡资格门控：商户卡（merchantId 非空）充值前须平台已开通发卡资格，默认关→拒。通用卡不受限。 */
    private void requireCardIssuanceEnabled(String merchantId) {
        if (merchantId == null) {
            return;
        }
        StoredValueMerchantRefundRuleRecord rule = repository.findStoredValueMerchantRefundRule(merchantId)
                .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(merchantId));
        if (!rule.cardIssuanceEnabled()) {
            throw ApiException.conflict("STORED_VALUE_CARD_ISSUANCE_DISABLED",
                    "平台未对该商户开通商户卡发卡");
        }
    }

    /** 开卡定格资金模式：商户卡取商户当前配置（默认商户预收）；通用卡为 null。 */
    private String resolveMerchantFundOwnership(String merchantId) {
        if (merchantId == null) {
            return null;
        }
        return repository.findStoredValueMerchantRefundRule(merchantId)
                .map(StoredValueMerchantRefundRuleRecord::fundOwnership)
                .orElse(FUND_MERCHANT_PREPAID);
    }

    /** 展示用有效资金模式：卡上定格值优先；旧卡(未定格,NULL)的商户卡按商户预收兜底；通用卡为 null。 */
    private String effectiveFundOwnership(CustomerStoredValueAccountRecord account) {
        if (account.merchantId() == null) {
            return null;
        }
        return account.fundOwnership() == null ? FUND_MERCHANT_PREPAID : account.fundOwnership();
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
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_CLOSED", "储值账户已销户");
        }
        if ("frozen".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ALREADY_FROZEN", "储值账户已是冻结状态");
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
            throw ApiException.conflict("STORED_VALUE_NOT_FROZEN", "储值账户不在冻结状态");
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
                    .map(Strings::trimToNull)
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

    /**
     * AUD-002 幂等重放校验：命中已有流水时核对完整不可变指纹——客人、操作类型、签名金额、
     * 业务事由、卡范围(按请求范围解析的账户须与流水账户同一张)。任一不符=键被其他操作占用，409 拒绝；
     * 全部一致=同请求安全重放，返回原流水。
     */
    private CustomerStoredValueTransactionRecord verifyIdempotentReplay(CustomerStoredValueTransactionRecord existing,
                                                                        String customerId, String projectId,
                                                                        String merchantId, String txnType,
                                                                        long signedAmountCent, String reason) {
        boolean fingerprintMatches = customerId.equals(existing.customerId())
                && txnType.equals(existing.txnType())
                && existing.amountCent() == signedAmountCent
                && java.util.Objects.equals(trimToNull(reason), trimToNull(existing.reason()));
        if (fingerprintMatches) {
            CustomerStoredValueAccountRecord account =
                    repository.findStoredValueAccount(customerId, projectId, merchantId).orElse(null);
            fingerprintMatches = account != null && account.id().equals(existing.accountId());
        }
        if (!fingerprintMatches) {
            throw ApiException.conflict("IDEMPOTENCY_KEY_CONFLICT",
                    "该幂等键已被其他操作使用");
        }
        return existing;
    }

    /**
     * XM-ONESHOT-RETURN B02 审查修正（操作员 2026-09-25 确认的追加句「同一次操作重复提交时，返回第一次的结果并提示已完成」）：
     * 退款重放除 AUD-002 指纹外，还核对流水上没有的两项请求内容——是否销户（close_account）、柜台兑付凭证号。
     * 两项记在第一次的审计明细里（与流水同事务写入，审计幂等键＝流水幂等键）。任一不同＝改过的请求，不是同一次操作，
     * 与统一幂等层「同键改正文」一样报 409，不返回第一次的流水、不提示已完成。
     */
    private void verifyRefundReplayRequest(CustomerStoredValueTransactionRecord existing, boolean counterMode,
                                           boolean closeAccount, String redemptionVoucherNo) {
        // 上限：只翻该卡最近 1000 条同名审计（重放紧跟首单，够用）；要查更远再给审计仓储加按幂等键精确查的方法
        Map<String, Object> first = auditEventService.searchOperationEvents("customer_stored_value",
                        existing.accountId(), null, counterMode ? EVENT_COUNTER_REFUND : EVENT_REFUND,
                        null, null, 1000, null).stream()
                .filter(e -> "success".equals(e.eventResult())
                        && existing.idempotencyKey().equals(e.idempotencyKey()))
                .map(com.complex.platform.evidence.AuditEvent::metadata)
                .findFirst().orElse(null);
        boolean same = first != null
                && Boolean.valueOf(closeAccount).equals(first.get("close_account"))
                && (!counterMode || java.util.Objects.equals(redemptionVoucherNo, first.get("redemption_voucher_no")));
        if (!same) {
            throw ApiException.conflict("IDEMPOTENCY_KEY_CONFLICT", "该幂等键已被其他操作使用");
        }
    }

    /** AUD-002 命名空间隔离：客户端/后台传入的幂等键不得使用内部派生键的保留前缀。 */
    private void requireClientIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        for (String prefix : RESERVED_IDEMPOTENCY_PREFIXES) {
            if (idempotencyKey.startsWith(prefix)) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "幂等键使用了系统保留前缀，请更换");
            }
        }
    }

    /** AUD 复核 P1-1：把充值幂等键与卡范围压成定长十六进制，供 finance 幂等键使用（避免 slug 截断碰撞）。 */
    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private void requireActive(CustomerStoredValueAccountRecord account) {
        if ("frozen".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_FROZEN", "储值账户已被冻结");
        }
        if ("closed".equals(account.status())) {
            throw ApiException.conflict("STORED_VALUE_ACCOUNT_CLOSED", "储值账户已销户");
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
                        "该储值卡不在当前商户权限范围内");
            }
        });
    }

    // M724-F15（治理地图第 528 行红线④「看不到别的商户和别的项目的数据」）：退款重复提交返回第一次的流水之前，按流水所属的卡核当前操作人的项目。
    // 卡带项目才比（商户卡可以不带项目，V175「商户卡可无独立 project」，卡上没有项目可比）；平台管理员（scope 空）放行，口径同
    // requireMerchantScopeForCard；authorizationService 为 null 不强制。只用在退款的重放分支。
    private void requireProjectScopeForCard(CustomerStoredValueAccountRecord account) {
        if (authorizationService == null || account.projectId() == null) {
            return;
        }
        authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
            if (!scope.equals(account.projectId())) {
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN",
                        "该储值卡不在当前项目权限范围内");
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
                        "该商户退款规则不在当前商户权限范围内");
            }
        });
    }

    private CustomerStoredValueAccountRecord requireAccountForUpdate(String customerId, String projectId, String merchantId) {
        return repository.findStoredValueAccountForUpdate(customerId, projectId, merchantId)
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND",
                        "储值账户不存在或已被删除"));
    }

    private CustomerStoredValueAccountRecord loadOrCreateAccountForUpdate(String customerId, String projectId, String merchantId) {
        return repository.findStoredValueAccountForUpdate(customerId, projectId, merchantId)
                .orElseGet(() -> {
                    Instant now = now();
                    // AUD-059 开卡定格资金模式：商户卡取商户当前配置钉进卡（终身不变）；通用卡为 null。
                    String fundOwnership = resolveMerchantFundOwnership(merchantId);
                    return repository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                            "svacc_" + UUID.randomUUID().toString().replace("-", ""),
                            customerId, projectId, merchantId, 0L, "active", now, now, fundOwnership));
                });
    }

    /** 仅改状态（冻结/解冻）：余额与两段不变。 */
    private CustomerStoredValueAccountRecord withBalanceAndStatus(CustomerStoredValueAccountRecord account,
                                                                  long balanceCent, String status, Instant now) {
        return new CustomerStoredValueAccountRecord(account.id(), account.customerId(), account.projectId(),
                account.merchantId(), balanceCent, status, account.createdAt(), now, account.fundOwnership(),
                account.principalCent(), account.bonusCent());
    }

    /** CG-06a：按两段写回，余额 = 本金 + 赠送金。 */
    private CustomerStoredValueAccountRecord withSplit(CustomerStoredValueAccountRecord account,
                                                       long principalCent, long bonusCent, String status, Instant now) {
        return new CustomerStoredValueAccountRecord(account.id(), account.customerId(), account.projectId(),
                account.merchantId(), principalCent + bonusCent, status, account.createdAt(), now,
                account.fundOwnership(), principalCent, bonusCent);
    }

    /** 扣款两段拆分：[本金部分, 赠送金部分]；bonusFirst=false 先本金后赠送金，true 先赠送金后本金。 */
    private static long[] splitDeduction(CustomerStoredValueAccountRecord account, long amountCent, boolean bonusFirst) {
        long first = bonusFirst ? account.bonusCent() : account.principalCent();
        long takeFirst = Math.min(first, amountCent);
        long takeSecond = amountCent - takeFirst;
        return bonusFirst ? new long[] {takeSecond, takeFirst} : new long[] {takeFirst, takeSecond};
    }

    /** 通用卡写死先本金后赠送金；商户级卡读发卡商户自定（平台代录）的顺序。 */
    private boolean bonusFirstForCard(CustomerStoredValueAccountRecord account) {
        if (account.merchantId() == null) {
            return false;
        }
        return repository.findStoredValueMerchantRefundRule(account.merchantId())
                .map(StoredValueMerchantRefundRuleRecord::consumeOrder)
                .map(StoredValueMerchantRefundRuleRecord.CONSUME_BONUS_FIRST::equals)
                .orElse(false);
    }

    private CustomerRecord requireCustomer(String customerId) {
        return repository.findCustomerById(customerId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "客人不存在或已被删除"));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
