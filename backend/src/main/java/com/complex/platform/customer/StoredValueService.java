package com.complex.platform.customer;

// 文件版本：V0.18

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
 * 「只扣指定的那一张」（请求点名卡范围，不自动跨卡）。商户卡充值资金归发卡商户预收（merchant_prepaid；第十八批第 11 条按
 * BSM L833、DEC-CFG-2026-08-03-11 关闭平台代收入口，下游 platform_collect 分支只对已定格的旧卡生效）。
 */
@Service
public class StoredValueService {

    private static final Logger log = LoggerFactory.getLogger(StoredValueService.class);
    private static final String EVENT_TOPUP = "customer.stored_value.settle";
    private static final String EVENT_CONSUME = "stored_value.consume";
    private static final String EVENT_REFUND = "stored_value.refund";
    private static final String EVENT_COUNTER_REFUND = "stored_value.counter_refund";
    private static final String EVENT_CONSUME_REVERSAL = "stored_value.consume_reversal";
    private static final String NOTIFICATION_DOMAIN = "stored_value";
    private static final String NOTIFICATION_EVENT_TOPUP = "topup";
    private static final String NOTIFICATION_EVENT_CONSUME = "consume";
    private static final String STORED_VALUE_TOPUP_TEMPLATE = "sms.stored_value.topup";
    private static final String STORED_VALUE_CONSUME_TEMPLATE = "sms.stored_value.consume";
    // TK-21-R1：账户关闭/注销时商户卡应退本金事项 → 通知发卡商户联系人。
    private static final String NOTIFICATION_EVENT_PENDING_REFUND = "pending_refund";
    private static final String STORED_VALUE_PENDING_REFUND_TEMPLATE = "sms.stored_value.pending_refund";
    private static final String ACCOUNT_CLOSE_KEY_PREFIX = "svclose_";

    /** 商户卡充值资金归属：merchant_prepaid=发卡商户预收（默认）；platform_collect=平台代收、消费时结算给商户。 */
    static final String FUND_MERCHANT_PREPAID = "merchant_prepaid";
    static final String FUND_PLATFORM_COLLECT = "platform_collect";

    // AUD-002 操作命名空间隔离：内部派生幂等键的保留前缀。客户端/后台传入的幂等键不得占用，
    // 否则可预占 sv_settle_<intentId> 使正常充值入账永久冲突，或伪造 barsv_ 命中错误重放。
    // svcr_ = 柜台退款命名空间（复核发现：与线上退款共享命名空间时，跨路径同键重放会把线上退款流水
    // 错当柜台退款成功返回——不落柜台审计/凭证还可能引发双重兑付）。
    private static final String[] RESERVED_IDEMPOTENCY_PREFIXES = {
            "sv_settle_", "sv_topup_", "svtop_", "barsv_", "svtxn_", "svacc_", "svcr_", "ticketsv_", "ticketrev_", "barsvrev_"};
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
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private com.complex.platform.ticketing.TicketOrderService ticketOrderService;
    // 复核 html m11：待退本金事项通知的收件人筛选（在职账号 + 权限码 + 员工档案手机号）；字段注入免改构造点，单测未注入则不发。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate recipientJdbc;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.complex.platform.access.staff.JdbcStaffRepository staffRepository;
    // 第十八批第 3 条：票务订单储值笔固化通路费（ENUM L86 充值时已产生，取本次消费的批次分摊额）；单测未注入则不写。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.complex.platform.settlement.RouteFeeShares routeFeeShares;
    // 第十八批 N 段第 123 条 123-R2：敏感操作定位闸（字段注入免改构造点；单测未注入即不查）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.gate.SensitiveLocationGuard locationGuard;

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
     * ROOT-03-R18 服务端结算作业：客人在渠道付完充值款却未回前台入账时，按充值时绑定的卡范围向渠道核验成功后入账（同 {@link #settleTopup} 内核，
     * 不过客人充值权限闸——作业无登录上下文；已入账的不重复入账）。
     */
    @Transactional
    public CustomerStoredValueTransactionRecord settleTopupFromGateway(String paymentIntentId) {
        StoredValueTopupIntentScope bound = repository.findStoredValueTopupIntent(paymentIntentId)
                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "该支付意向不属于储值充值"));
        CustomerStoredValueAccountRecord card = repository.findStoredValueAccount(bound.customerId(), bound.projectId(), bound.merchantId())
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND", "储值卡不存在"));
        financeService.confirmOnlinePaymentIntent(paymentIntentId, bound.merchantId(),
                bound.merchantId() == null ? null : effectiveFundOwnership(card));
        return settleTopupInternal(bound.customerId(), bound.projectId(), bound.merchantId(), paymentIntentId, false);
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
        // CG-06a／第十八批第 11 条（ENUM L86「每笔充值拆为本金与赠送金两段分别记账」「商户级卡同样可以设赠送……资金与成本全部归发卡商户承担」；
        // BSM L521「每一笔单据在生成时就把所用配置版本固化在单上」）：本金 = 实付；赠送金 = 实付 × 此刻生效的赠送比例版本，流水记下所用版本；无生效版本不赠送。
        java.util.Optional<CustomerRepository.StoredValueBonusVersion> bonusVersion =
                bonusVersion(account.projectId(), account.merchantId(), now);
        long bonusCent = bonusVersion.map(v -> MoneyRounding.bpsOf(intent.amountCent(), v.bps())).orElse(0L);
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
                        intent.amountCent(), bonusCent, bonusVersion.map(CustomerRepository.StoredValueBonusVersion::id).orElse(null)));
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("amount_cent", intent.amountCent());
        detail.put("principal_cent", intent.amountCent());
        detail.put("bonus_cent", bonusCent);
        bonusVersion.ifPresent(v -> {
            detail.put("bonus_config_version_id", v.id());
            detail.put("bonus_bps", v.bps());
        });
        detail.put("principal_after_cent", principalAfter);
        detail.put("bonus_after_cent", bonusAfter);
        detail.put("balance_after_cent", balanceAfter);
        detail.put("payment_intent_id", paymentIntentId);
        detail.put("card_scope", account.cardScope());
        if (account.merchantId() != null) {
            detail.put("merchant_id", account.merchantId());
            // AUD-059 商户卡资金归属：用开卡时定格在卡上的 fund_ownership（非全局默认）；旧卡未定格按商户预收兜底显示。
            detail.put("fund_ownership", effectiveFundOwnership(account));
        } else {
            // TK-19-R1（ENUM「储值卡类型与收款主体」：「通用卡的每笔充值分别保留当前剩余本金、支付通路和支付成功时固化费率，同一通路不同费率的
            // 充值分批保存，不得用新费率覆盖旧批次」「通道费以每笔充值支付成功时平台与该支付通路的生效合同费率为准，随该笔充值一并固化」；
            // DEC-P1A-04）：按 (项目, 支付通路) 取此刻生效费率钉进批次；未配置 = 0，不拒绝入账。批次 id = 充值流水 id。
            int feeBps = repository.findStoredValueChannelFeeBps(account.projectId(), intent.channel());
            repository.saveStoredValueTopupBatch(new StoredValueTopupBatchRecord(txn.id(), account.id(), customerId,
                    account.projectId(), intent.channel(), feeBps, intent.amountCent(), intent.amountCent(), now));
            detail.put("payment_channel", intent.channel());
            detail.put("channel_fee_bps", feeBps);
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
        return payTicketOrder(customerId, orderId, accountId, phone, smsCode, null, null);
    }

    /**
     * 客人本人订单储值付款。ROOT-03-R20（操作员原话「一个订单可分多个渠道支付，但最终必须完成一次支付才算完成」）：amountCent 为空＝付清待付金额，
     * 可只付一部分、其余在线补足，不得超过待付金额；合计足额才置已付。ROOT-03-R17（BSM 第 859 行「客人付款成功后必须出票」）：付足即同一事务自动出票，
     * 出票失败整笔回滚（余额不扣、订单仍待付）。idempotencyKey＝客人请求编号：同一请求重复提交不重复扣款。
     */
    @Transactional
    @GuardedAction(capability = "customer.stored_value.consume", permission = "customer.stored_value.consume")
    public com.complex.platform.ticketing.TicketOrderRecord payTicketOrder(String customerId, String orderId, String accountId,
            String phone, String smsCode, Long amountCent, String idempotencyKey) {
        try {
            return payOwnTicketOrder(customerId, orderId, accountId, phone, smsCode, amountCent, idempotencyKey);
        } catch (RuntimeException failure) {
            // 失败留痕走独立事务（AuditEventService.recordFailure），随本事务回滚不丢。
            auditEventService.recordFailure("customer.stored_value.pay_failed", "customer.stored_value.consume", "ticket_order",
                    orderId, null, idempotencyKey, failure instanceof ApiException api ? api.code() : failure.getClass().getSimpleName());
            throw failure;
        }
    }

    private com.complex.platform.ticketing.TicketOrderRecord payOwnTicketOrder(String customerId, String orderId, String accountId,
            String phone, String smsCode, Long amountCent, String idempotencyKey) {
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
        if (ticketOrderService == null) {
            throw ApiException.conflict("TICKET_PAYMENT_GATE_UNAVAILABLE", "票务付款校验当前不可用");
        }
        ticketOrderService.requirePaymentOpen(order);
        CustomerStoredValueAccountRecord card = repository.listStoredValueAccountsByCustomer(customerId).stream()
                .filter(a -> a.id().equals(accountId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND", "储值卡不存在"));
        // 以售票时的原项目快照判断项目卡范围；未知归属不得从操作者或当前商户关系猜测。
        if ((card.projectId() != null && !card.projectId().equals(order.projectId()))
                || (card.merchantId() != null && !card.merchantId().equals(order.barId()))) {
            throw ApiException.conflict("STORED_VALUE_SCOPE_MISMATCH", "所选储值卡不适用于该订单范围");
        }
        if (!"CNY".equals(order.currency())) {
            throw ApiException.conflict("PAYMENT_CURRENCY_MISMATCH", "订单币种与储值余额不一致");
        }
        String consumeKey = idempotencyKey == null ? "ticketsv_" + order.id()
                : "ticketsv_" + sha256Hex(order.id() + "|" + idempotencyKey).substring(0, 40);
        if (repository.findStoredValueTransactionByIdempotencyKey(consumeKey).isPresent()) {
            return order; // 同一请求重复提交：已扣过，返回订单现状。
        }
        long paidBefore = ticketingRepository.listTicketPaymentIntentsByOrder(order.id()).stream()
                .filter(intent -> "succeeded".equals(intent.status()))
                .mapToLong(com.complex.platform.ticketing.TicketPaymentIntentRecord::amountCent).sum();
        long remaining = order.amountCent() - paidBefore;
        long amount = amountCent == null ? remaining : amountCent;
        if (amount <= 0 || amount > remaining) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent 须大于 0 且不超过待付金额 " + remaining + " 分");
        }
        customerService.verifyStoredValueCustomer(customerId, phone, smsCode);
        CustomerStoredValueTransactionRecord debit = consumeBalance(customerId, card.projectId(), card.merchantId(), amount,
                "票务订单储值支付 " + order.id(), consumeKey, true);
        Instant paidAt = now();
        ticketingRepository.saveTicketPaymentIntent(new com.complex.platform.ticketing.TicketPaymentIntentRecord(
                debit.id(), "ticket_order", order.id(), amount, order.currency(),
                "stored_value", "succeeded", paidAt, paidAt));
        if (routeFeeShares != null) {
            routeFeeShares.fix(debit.id(), order.id(), order.projectId(), "stored_value", amount);
        }
        if (paidBefore + amount < order.amountCent()) {
            return order; // R20：未足额，订单保持待付，等其余渠道补足。
        }
        com.complex.platform.ticketing.TicketOrderRecord paid = ticketingRepository.updateTicketOrder(
                new com.complex.platform.ticketing.TicketOrderRecord(
                order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(), order.priceRuleId(),
                order.quantity(), order.visitDate(), order.salesChannel(), order.buyerPhone(), order.amountCent(),
                order.dynamicPricingRuleId(), order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
                "paid", order.issueStatus(), order.status(), order.createdAt(), paidAt, order.merchantId(),
                paidAt, order.barId(), order.projectId()));
        ticketOrderService.issuePaidOrderAutomatically(order.id());
        return ticketingRepository.findTicketOrderById(order.id()).orElse(paid);
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
        // TK-19-R2（ENUM「储值卡类型与收款主体」：「每次消费需扣本金时，按消费前各批次剩余本金占总剩余本金的比例分摊本次本金扣减，
        // 并逐批按其原固化费率计算对应通路费；本金已耗尽的批次不参与。本金扣减为零的消费不运行本金批次分摊」）：仅通用卡、仅本金扣减 > 0。
        // 存量无批次的本金（V517 前充值，铁律 8 不回填）不分摊；分摊总额以各批剩余之和封顶。
        if (account.merchantId() == null && parts[0] > 0) {
            allocateConsumeAcrossBatches(account, txn.id(), parts[0], consumeDetail, now);
        }
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
    // TK-20-R1 消费退款回原卡（酒吧账单退款/退品、票务退款等已过各自审批的域内部调用；不开控制器入口）
    // ============================================================

    /**
     * CG-06a / Q34（ENUM「储值卡类型与收款主体」：「综合体通用卡和商户级卡的消费退款均退回原消费卡余额，固定先回冲该笔消费中已扣且尚未回冲的
     * 赠送金，该笔消费已扣赠送金全部回冲完毕后，才回冲该笔消费中已扣且尚未回冲的本金；分次退款每次均按此顺序继续回冲，赠送金与本金各自累计
     * 回冲金额不得超过该笔消费中各自的原扣款金额，不因原消费扣款顺序不同或后续消费配置变更而改变」）。
     *
     * <p>锁原消费流水与卡；写 consume_reversal 流水（两段正向、original_txn_id 回指原消费）。卡已关闭（账户关闭/注销）时不复活：
     * 本金部分按 BSM「储值」「账户关闭的，储值本金退还本人」登记待退，赠送金作废留 adjust。通用卡回本金后按 TK-19-R3 恢复原充值批次；
     * platform_collect 商户卡同时冲正原「平台应付商户」。幂等：同键重放核对指纹后返回第一次的流水。
     */
    @Transactional
    public CustomerStoredValueTransactionRecord refundConsumption(String consumeTxnId, long amountCent, String reason,
                                                                  String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "金额必须大于 0");
        }
        Instant now = now();
        CustomerStoredValueTransactionRecord original = repository
                .findStoredValueTransactionByIdForUpdate(Strings.requireText(consumeTxnId, "consume_txn_id"))
                .filter(txn -> "consume".equals(txn.txnType()))
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_CONSUME_NOT_FOUND", "原储值消费流水不存在"));
        CustomerStoredValueTransactionRecord existing = idempotentTxn(idempotencyKey);
        if (existing != null) {
            boolean same = "consume_reversal".equals(existing.txnType())
                    && existing.amountCent() == amountCent
                    && existing.accountId().equals(original.accountId())
                    && java.util.Objects.equals(trimToNull(reason), trimToNull(existing.reason()));
            if (!same) {
                throw ApiException.conflict("IDEMPOTENCY_KEY_CONFLICT", "该幂等键已被其他操作使用");
            }
            return existing;
        }
        CustomerStoredValueAccountRecord card = repository.listStoredValueAccountsByCustomer(original.customerId()).stream()
                .filter(a -> a.id().equals(original.accountId())).findFirst()
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND", "储值账户不存在或已被删除"));
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(card.customerId(), card.projectId(), card.merchantId());
        long[] reversed = repository.sumStoredValueReversals(original.id());
        long bonusOpen = -original.bonusCent() - reversed[1];
        long principalOpen = -original.principalCent() - reversed[0];
        if (amountCent > bonusOpen + principalOpen) {
            throw ApiException.conflict("STORED_VALUE_REVERSAL_EXCEEDS_ORIGINAL",
                    "回冲金额超过该笔消费尚未回冲的扣款");
        }
        long bonusBack = Math.min(amountCent, bonusOpen);
        long principalBack = amountCent - bonusBack;
        // 复核 money m2：商户卡账户关闭 / 注销待退（svclose_ 事项仍待结清、卡冻结）期间收到消费退款，按已关闭卡处理——不把本金加回冻结卡，
        // 否则柜台退卡金额与待退事项永远对不上。
        boolean closed = "closed".equals(account.status())
                || (account.merchantId() != null && "frozen".equals(account.status())
                    && financeService.findStoredValuePayoutRefund(ACCOUNT_CLOSE_KEY_PREFIX + account.id())
                        .filter(item -> "requested".equals(item.status()) || "processing".equals(item.status())).isPresent());
        long principalAfter = account.principalCent() + (closed ? 0L : principalBack);
        long bonusAfter = account.bonusCent() + (closed ? 0L : bonusBack);
        long balanceAfter = principalAfter + bonusAfter;
        if (!closed) {
            repository.saveStoredValueAccount(withSplit(account, principalAfter, bonusAfter, account.status(), now));
        }
        CustomerStoredValueTransactionRecord txn = repository.saveStoredValueReversalTransaction(
                new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), account.customerId(), "consume_reversal", amountCent, balanceAfter,
                        null, reason, idempotencyKey, now, null, principalBack, bonusBack), original.id());
        Map<String, Object> detail = consumeRefundDetail(account, amountCent, balanceAfter);
        detail.put("original_txn_id", original.id());
        detail.put("principal_cent", principalBack);
        detail.put("bonus_cent", bonusBack);
        detail.put("principal_after_cent", principalAfter);
        detail.put("bonus_after_cent", bonusAfter);
        detail.put("closed", closed);
        if (closed) {
            // 卡已关闭（BSM「储值」：「账户关闭的，储值本金退还本人」；ENUM：「销户时……赠送金清零」）：本金登记待退、赠送金作废，不复活卡。
            if (principalBack > 0) {
                String payoutRefundId = financeService.recordStoredValuePayoutRefund(account.id(), principalBack,
                        "消费退款回冲：账户已关闭，本金退还本人", account.merchantId(), account.projectId(),
                        idempotencyKey + "|closed_payout");
                detail.put("payout_status", "requested");
                detail.put("payout_refund_id", payoutRefundId);
            }
            if (bonusBack > 0) {
                repository.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), account.customerId(), "adjust", -bonusBack, balanceAfter,
                        null, "消费退款回冲：账户已关闭，赠送金作废（不退现）", idempotencyKey + "|bonus_void", now, null,
                        0L, -bonusBack));
                detail.put("bonus_voided_cent", bonusBack);
            }
        }
        // 复核 money m1：卡已关闭时本金登记待退本人，不再恢复到原充值批次。
        if (!closed && account.merchantId() == null && principalBack > 0) {
            restorePrincipalToBatches(original.id(), txn.id(), principalBack, detail, now);
        }
        if (FUND_PLATFORM_COLLECT.equals(account.fundOwnership()) && account.merchantId() != null) {
            var reversal = financeService.recordStoredValueMerchantPayableReversal(account.merchantId(), original.id(),
                    txn.id(), amountCent);
            if (reversal != null) {
                detail.put("payable_reversal_item_id", reversal.id());
            }
        }
        auditEventService.recordSuccess(EVENT_CONSUME_REVERSAL, "customer.stored_value.refund",
                "customer_stored_value", account.id(), null, idempotencyKey, detail);
        return txn;
    }

    /**
     * 复核 money I3：撤回一笔消费退款回冲（酒吧退品驳回）——按该回冲流水的本金 / 赠送金原额从原卡扣回，写一条回指原消费的负向
     * consume_reversal（原消费已回冲累计随之净额扣减，之后再退款可再回冲）；通用卡同时撤回该次对原充值批次的恢复，
     * platform_collect 商户卡把冲正掉的「平台应付商户」按扣回额重新挂上。卡非可用 / 余额不足按既有守卫拒绝（驳回整笔回滚）。
     * 幂等：同键返回第一次的流水。仅供已授权域内部调用。
     */
    @Transactional
    public CustomerStoredValueTransactionRecord undoConsumptionRefund(String reversalTxnId, String reason, String idempotencyKey) {
        CustomerStoredValueTransactionRecord existing = idempotentTxn(Strings.requireText(idempotencyKey, "idempotency_key"));
        if (existing != null) {
            return existing;
        }
        Instant now = now();
        CustomerStoredValueTransactionRecord reversal = repository.findStoredValueTransactionByIdForUpdate(reversalTxnId)
                .filter(txn -> "consume_reversal".equals(txn.txnType()) && txn.amountCent() > 0)
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_REVERSAL_NOT_FOUND", "原消费退款回冲流水不存在"));
        String originalId = repository.findStoredValueOriginalTxnId(reversal.id())
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_CONSUME_NOT_FOUND", "原储值消费流水不存在"));
        CustomerStoredValueAccountRecord card = repository.listStoredValueAccountsByCustomer(reversal.customerId()).stream()
                .filter(a -> a.id().equals(reversal.accountId())).findFirst()
                .orElseThrow(() -> ApiException.notFound("STORED_VALUE_ACCOUNT_NOT_FOUND", "储值账户不存在或已被删除"));
        CustomerStoredValueAccountRecord account = requireAccountForUpdate(card.customerId(), card.projectId(), card.merchantId());
        requireActive(account);
        if (account.principalCent() < reversal.principalCent() || account.bonusCent() < reversal.bonusCent()) {
            throw ApiException.conflict("STORED_VALUE_INSUFFICIENT", "储值余额不足，无法扣回已回冲金额");
        }
        long principalAfter = account.principalCent() - reversal.principalCent();
        long bonusAfter = account.bonusCent() - reversal.bonusCent();
        repository.saveStoredValueAccount(withSplit(account, principalAfter, bonusAfter, account.status(), now));
        CustomerStoredValueTransactionRecord txn = repository.saveStoredValueReversalTransaction(
                new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), account.customerId(), "consume_reversal", -reversal.amountCent(), principalAfter + bonusAfter,
                        null, reason, idempotencyKey, now, null, -reversal.principalCent(), -reversal.bonusCent()), originalId);
        if (account.merchantId() == null && reversal.principalCent() > 0) {
            repository.undoStoredValueRestorations(reversal.id());
        }
        if (FUND_PLATFORM_COLLECT.equals(account.fundOwnership()) && account.merchantId() != null) {
            financeService.recordStoredValueMerchantPayable(account.merchantId(), txn.id(), reversal.amountCent(), idempotencyKey);
        }
        Map<String, Object> detail = consumeRefundDetail(account, reversal.amountCent(), principalAfter + bonusAfter);
        detail.put("original_txn_id", originalId);
        detail.put("undo_of_reversal_txn_id", reversal.id());
        detail.put("principal_cent", -reversal.principalCent());
        detail.put("bonus_cent", -reversal.bonusCent());
        auditEventService.recordSuccess(EVENT_CONSUME_REVERSAL, "customer.stored_value.refund",
                "customer_stored_value", account.id(), null, idempotencyKey, detail);
        return txn;
    }

    /**
     * TK-19-R2：按消费前各批次剩余本金的比例分摊本次本金扣减（最大余数法到分，总和恒等），逐批按其原固化费率算通路费并留原消费分摊记录。
     * 本次消费对应的通路费合计写入审计明细（channel_fee_cent），供商户结算「扣除该笔消费所对应的通路费」取数。
     */
    private void allocateConsumeAcrossBatches(CustomerStoredValueAccountRecord account, String consumeTxnId,
                                              long principalDeducted, Map<String, Object> detail, Instant now) {
        java.util.List<StoredValueTopupBatchRecord> batches = repository.listOpenStoredValueTopupBatchesForUpdate(account.id());
        if (batches.isEmpty()) {
            return;
        }
        long[] weights = batches.stream().mapToLong(StoredValueTopupBatchRecord::remainingPrincipalCent).toArray();
        long totalRemaining = java.util.Arrays.stream(weights).sum();
        long[] shares = allocateProRata(Math.min(principalDeducted, totalRemaining), weights);
        java.util.List<Map<String, Object>> allocations = new java.util.ArrayList<>();
        long feeTotal = 0L;
        for (int i = 0; i < batches.size(); i++) {
            if (shares[i] <= 0) {
                continue;
            }
            StoredValueTopupBatchRecord batch = batches.get(i);
            long fee = MoneyRounding.bpsOf(shares[i], batch.feeBps());
            repository.saveStoredValueConsumeAllocation(new StoredValueConsumeAllocationRecord(
                    "svalloc_" + UUID.randomUUID().toString().replace("-", ""), consumeTxnId, batch.id(),
                    shares[i], 0L, batch.feeBps(), fee, now));
            repository.addStoredValueTopupBatchRemaining(batch.id(), -shares[i]);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("batch_id", batch.id());
            row.put("payment_channel", batch.paymentChannel());
            row.put("fee_bps", batch.feeBps());
            row.put("principal_cent", shares[i]);
            row.put("fee_cent", fee);
            allocations.add(row);
            feeTotal += fee;
        }
        detail.put("batch_allocations", allocations);
        detail.put("channel_fee_cent", feeTotal);
    }

    /**
     * TK-19-R3（ENUM：「该本金按原消费各充值批次已扣且尚未回冲本金的比例恢复到原批次，保留原支付通路和原固化费率，各批次累计恢复金额不得
     * 超过该消费原扣该批次的本金。原消费未使用的批次（包括后续充值批次）不参与本次恢复。相应消费结算的通路费调整关联原消费批次及原费率依据，
     * 不按退款时的新费率重算；原消费、退款恢复及调整记录分别保留」；Q35A）。无分摊记录的存量消费不恢复。
     */
    private void restorePrincipalToBatches(String consumeTxnId, String reversalTxnId, long principalBack,
                                           Map<String, Object> detail, Instant now) {
        java.util.List<StoredValueConsumeAllocationRecord> allocations =
                repository.listStoredValueConsumeAllocationsForUpdate(consumeTxnId);
        if (allocations.isEmpty()) {
            return;
        }
        long[] open = allocations.stream().mapToLong(a -> a.principalCent() - a.restoredPrincipalCent()).toArray();
        long totalOpen = java.util.Arrays.stream(open).sum();
        long[] shares = allocateProRata(Math.min(principalBack, totalOpen), open);
        java.util.List<Map<String, Object>> restorations = new java.util.ArrayList<>();
        for (int i = 0; i < allocations.size(); i++) {
            if (shares[i] <= 0) {
                continue;
            }
            StoredValueConsumeAllocationRecord allocation = allocations.get(i);
            long feeAdjust = -MoneyRounding.bpsOf(shares[i], allocation.feeBps());
            repository.addStoredValueAllocationRestored(allocation.id(), shares[i]);
            repository.addStoredValueTopupBatchRemaining(allocation.batchId(), shares[i]);
            repository.saveStoredValueConsumeRestoration("svrest_" + UUID.randomUUID().toString().replace("-", ""),
                    reversalTxnId, allocation.id(), shares[i], allocation.feeBps(), feeAdjust, now);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("batch_id", allocation.batchId());
            row.put("fee_bps", allocation.feeBps());
            row.put("principal_cent", shares[i]);
            row.put("fee_adjust_cent", feeAdjust);
            restorations.add(row);
        }
        detail.put("batch_restorations", restorations);
    }

    /** 最大余数法：按 weights 比例把 total 拆到分，各份之和恒等于 total（weights 全 0 或 total ≤ 0 时全 0）。 */
    static long[] allocateProRata(long total, long[] weights) {
        long[] shares = new long[weights.length];
        long sumWeights = java.util.Arrays.stream(weights).sum();
        if (total <= 0 || sumWeights <= 0) {
            return shares;
        }
        java.math.BigDecimal[] fractions = new java.math.BigDecimal[weights.length];
        long allocated = 0L;
        for (int i = 0; i < weights.length; i++) {
            java.math.BigDecimal exact = MoneyRounding.exactProrate(total, weights[i], sumWeights);
            shares[i] = exact.setScale(0, java.math.RoundingMode.FLOOR).longValueExact();
            fractions[i] = exact.subtract(java.math.BigDecimal.valueOf(shares[i]));
            allocated += shares[i];
        }
        Integer[] order = new Integer[weights.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> fractions[b].compareTo(fractions[a]));
        for (int k = 0; allocated < total && k < order.length; k++) {
            if (weights[order[k]] > 0) {
                shares[order[k]]++;
                allocated++;
            }
        }
        return shares;
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
        if (locationGuard != null) {
            locationGuard.check("counterRefundStoredValueAccount", () -> projectId);
        }
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
        // 操作员裁决 2026-10-20 第三轮第 7 条「退卡即全退并关卡」：通用卡退卡一次退还全部剩余本金、赠送金清零、卡关闭；不支持只退一部分。
        boolean generalCard = account.merchantId() == null;
        if (generalCard && amountCent != account.principalCent()) {
            throw ApiException.badRequest("STORED_VALUE_GENERAL_CARD_FULL_REFUND_ONLY",
                    "综合体通用卡退卡须一次退还全部剩余本金（" + account.principalCent() + " 分），不支持只退一部分");
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
        // TK-21-R2（BSM「储值」：「沿原退款审批和原收款路径执行；实际退款成功才结清」）：账户关闭/注销登记的商户卡待退事项，
        // 由发卡商户柜台退卡（原收款路径＝到店）这一个口结清：实退额须等于事项金额且本金退清，否则 409；成功后事项置 success、卡销户。
        com.complex.platform.finance.FinanceRefundRecord pendingItem = counterMode
                ? financeService.findStoredValuePayoutRefund(ACCOUNT_CLOSE_KEY_PREFIX + account.id())
                        .filter(item -> "requested".equals(item.status()) || "processing".equals(item.status()))
                        .orElse(null)
                : null;
        if (pendingItem != null && (payoutCent != pendingItem.refundAmountCent() || principalAfter != 0)) {
            throw ApiException.conflict("STORED_VALUE_PENDING_REFUND_AMOUNT_MISMATCH",
                    "柜台退卡金额须等于账户关闭登记的待退本金事项金额");
        }
        // CG-06a（ENUM「储值卡类型与收款主体」：「销户时……再退剩余本金，赠送金清零」；BSM「储值」：
        // 「赠送金不退现、未使用部分按活动规则作废或收回」）：销户（本金退清）时未使用赠送金作废，单独留一条 adjust 流水。
        boolean closing = (closeAccount || pendingItem != null || generalCard) && principalAfter == 0;
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
            if (pendingItem != null) {
                financeService.settleStoredValuePayoutRefundAtCounter(pendingItem, redemptionVoucherNo);
                counterDetail.put("pending_refund_id", pendingItem.id());
                counterDetail.put("pending_refund_status", "success");
            }
            auditEventService.recordSuccess(EVENT_COUNTER_REFUND, "customer.stored_value.refund",
                    "customer_stored_value", account.id(), null, idempotencyKey, counterDetail);
            return txn;
        }
        // 账户不是原支付流水；未落实充值分笔路线前只登记应退，不向网关发送聚合账户退款。
        boolean payoutSettled = payoutCent <= 0;
        String payoutRefundId = payoutSettled ? null
                : financeService.recordStoredValuePayoutRefund(account.id(), payoutCent, reason,
                        account.merchantId(), account.projectId(), idempotencyKey);
        Map<String, Object> detail = consumeRefundDetail(account, amountCent, balanceAfter);
        detail.put("principal_cent", amountCent);
        detail.put("bonus_voided_cent", bonusVoidCent);
        detail.put("closed", newStatus.equals("closed"));
        detail.put("payout_cent", payoutCent);
        detail.put("payout_gateway_status", "NOT_DISPATCHED");
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
    // CG-06b 关闭客人账户（拉黑 / 注销）：通用卡未消费本金退还本人、赠送金作废、卡置 closed；
    // TK-21-R1 商户级卡按发卡商户规则登记应退本金事项并通知发卡商户，钱在商户处、柜台退卡才结清
    // ============================================================

    /**
     * CG-06b（BSM「核销单位」：「关闭该客人账户、该账号作废，并将其列入黑名单不允许再进入；关闭后该账户名下的储值本金退还本人」；
     * BSM「储值」：「账户关闭的，储值本金退还本人」「综合体通用卡的未消费本金默认全额可退，赠送金不退现、未使用部分按活动规则作废或收回」
     * 「对需要退还本金的注销或拉黑事项，系统自动生成应退本金事项并通知发卡主体获授权人员，沿原退款审批和原收款路径执行；实际退款成功才结清。
     * 商户卡仍由发卡商户直接收款和退款，生成待退事项不代表平台已经付款或客人已经到账」；
     * ENUM「储值卡类型与收款主体」：「商户级卡是否可退、退多少比例由发卡商户自设规则」「销户时……赠送金清零」）。
     *
     * <p>系统路径：由账户关闭/注销动作在同一事务内触发，不再过 customer.stored_value.refund 权限闸，也不受客人黑名单闸拦截。
     * 通用卡（merchant_id 为空）：未消费本金全额沿既有线上退款出账路径退还（登记到 finance_refunds，退款执行主体＝长沙项目公司），
     * 赠送金作废留 adjust 流水，卡置 closed。商户级卡：赠送金作废；本金按发卡商户规则——不可退则不生成事项、卡 closed；
     * 可退则按比例登记发卡商户名下待退事项（卡余额不动，钱在商户处，实际柜台退卡时再扣、见 doRefund），卡置 frozen 防消费，
     * 提交后短信通知发卡商户联系人。重复关闭：已销户的卡、已登记待退事项的商户卡跳过，不重复退/登记。
     */
    @Transactional
    public void closeCardsOnAccountClosure(String customerId, String reason) {
        Instant now = now();
        for (CustomerStoredValueAccountRecord card : repository.listStoredValueAccountsByCustomer(customerId)) {
            if ("closed".equals(card.status())) {
                continue;
            }
            CustomerStoredValueAccountRecord account = repository
                    .findStoredValueAccountForUpdate(customerId, card.projectId(), card.merchantId()).orElse(null);
            if (account == null || "closed".equals(account.status())) {
                continue;
            }
            String key = ACCOUNT_CLOSE_KEY_PREFIX + account.id();
            if (account.merchantId() != null && financeService.findStoredValuePayoutRefund(key).isPresent()) {
                continue;
            }
            long principal = account.principalCent();
            long bonus = account.bonusCent();
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("reason", reason == null ? "" : reason);
            detail.put("bonus_voided_cent", bonus);
            boolean merchantCard = account.merchantId() != null;
            StoredValueMerchantRefundRuleRecord rule = merchantCard
                    ? repository.findStoredValueMerchantRefundRule(account.merchantId())
                            .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(account.merchantId()))
                    : null;
            long payoutCent = merchantCard
                    ? (rule.refundAllowed() ? MoneyRounding.bpsOf(principal, rule.refundRatioBps()) : 0L)
                    : principal;
            boolean pendingAtMerchant = merchantCard && payoutCent > 0;
            String newStatus = pendingAtMerchant ? "frozen" : "closed";
            long principalAfter = pendingAtMerchant ? principal : (merchantCard ? principal : 0L);
            repository.saveStoredValueAccount(withSplit(account, principalAfter, 0L, newStatus, now));
            if (!merchantCard && principal > 0) {
                repository.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "refund", -principal, bonus,
                        null, "账户关闭：未消费本金退还本人", key, now, null, -principal, 0L));
            }
            if (payoutCent > 0) {
                String payoutRefundId = financeService.recordStoredValuePayoutRefund(account.id(), payoutCent,
                        merchantCard ? "账户关闭：商户卡本金待退" : "账户关闭：储值本金退还本人",
                        account.merchantId(), account.projectId(), key);
                detail.put("principal_refund_cent", payoutCent);
                detail.put("payout_gateway_status", "NOT_DISPATCHED");
                detail.put("payout_status", "requested");
                if (payoutRefundId != null) {
                    detail.put("payout_refund_id", payoutRefundId);
                }
            } else {
                detail.put("principal_refund_cent", 0L);
            }
            if (merchantCard) {
                detail.put("merchant_id", account.merchantId());
                detail.put("refund_allowed", rule.refundAllowed());
                detail.put("refund_ratio_bps", rule.refundRatioBps());
                if (!rule.refundAllowed()) {
                    detail.put("principal_retained_by_merchant_cent", principal);
                    detail.put("note", "按发卡商户规则不可退，不生成待退事项");
                }
            }
            if (bonus > 0) {
                repository.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "adjust", -bonus, principalAfter,
                        null, "账户关闭：未使用赠送金作废（不退现）", key + "|bonus_void", now, null, 0L, -bonus));
            }
            detail.put("card_scope", account.cardScope());
            detail.put("status", newStatus);
            detail.put("closed", "closed".equals(newStatus));
            auditEventService.recordSuccess("stored_value.closed_on_account_closure", "customer.account.manage",
                    "customer_stored_value", account.id(), null, key, detail);
            if (payoutCent > 0) {
                notifyPendingRefundAfterCommit(account, payoutCent);
            }
        }
    }

    /**
     * TK-21-R3（BSM「账户注销」：「客人可以选择……放弃余额，径行注销；放弃的储值卡余额归发卡主体（通用卡归长沙项目公司，
     * 商户级卡归发卡商户）」）：客人明确确认放弃余额——各卡本金与赠送金作废留 adjust 流水、卡置 closed，不生成任何待退事项。
     */
    @Transactional
    public void waiveBalancesOnDeregistration(String customerId) {
        Instant now = now();
        for (CustomerStoredValueAccountRecord card : repository.listStoredValueAccountsByCustomer(customerId)) {
            if ("closed".equals(card.status())) {
                continue;
            }
            CustomerStoredValueAccountRecord account = repository
                    .findStoredValueAccountForUpdate(customerId, card.projectId(), card.merchantId()).orElse(null);
            if (account == null || "closed".equals(account.status())) {
                continue;
            }
            long principal = account.principalCent();
            long bonus = account.bonusCent();
            String key = "svwaive_" + account.id();
            repository.saveStoredValueAccount(withSplit(account, 0L, 0L, "closed", now));
            if (principal + bonus > 0) {
                repository.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
                        "svtxn_" + UUID.randomUUID().toString().replace("-", ""),
                        account.id(), customerId, "adjust", -(principal + bonus), 0L,
                        null, "注销：客人放弃余额归发卡主体", key, now, null, -principal, -bonus));
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("principal_waived_cent", principal);
            detail.put("bonus_waived_cent", bonus);
            detail.put("card_scope", account.cardScope());
            if (account.merchantId() != null) {
                detail.put("merchant_id", account.merchantId());
            }
            detail.put("closed", true);
            auditEventService.recordSuccess("stored_value.waived_on_deregistration", "not_applicable",
                    "customer_stored_value", account.id(), null, key, detail);
        }
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
     * 商户卡平台配置（AUD-061）：开通/关闭发卡资格 + 设资金模式（只可商户预收，见下）。仅平台可改
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
        // 第十八批第 11 条（BSM L833「商户级卡的充值资金归发卡商户预收，平台不代收、不介入该笔资金的归属」；
        // DEC-CFG-2026-08-03-11「关闭「可由后台改为平台统一收取」开关」；cg-pending-decisions 第 16 条「不恢复平台代收」）。
        String mode = fundOwnership == null || fundOwnership.isBlank() ? FUND_MERCHANT_PREPAID : fundOwnership.trim();
        if (!FUND_MERCHANT_PREPAID.equals(mode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "商户级卡充值资金归发卡商户预收，平台不代收");
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

    /**
     * ROOT-03-R30（ENUM 第 86 行「储值卡的消费扣款顺序、赠送金的性质与退卡口径，须在客人充值之前告知」）：客人充值前读的规则。
     * merchantId 为空＝综合体通用卡：赠送比例按项目、扣款顺序写死先本金后赠送金（同句「不可配置」）；非空＝该商户的商户级卡：扣款顺序与退款
     * 比例按平台代录的商户卡规则（无配置按缺省），赠送比例按发卡商户。赠送比例取此刻生效的版本（无＝0）。客人会话由控制器校验。
     */
    public CustomerStoredValueTopupRulesResponse topupRules(String projectId, String merchantId) {
        String merchant = trimToNull(merchantId);
        int bonusBps = bonusVersion(trimToNull(projectId), merchant, now())
                .map(CustomerRepository.StoredValueBonusVersion::bps).orElse(0);
        if (merchant == null) {
            return new CustomerStoredValueTopupRulesResponse("general", trimToNull(projectId), null, bonusBps,
                    StoredValueMerchantRefundRuleRecord.CONSUME_PRINCIPAL_FIRST, "先扣本金、后扣赠送金", TOPUP_BONUS_TEXT,
                    TOPUP_REFUND_TEXT, true, null);
        }
        StoredValueMerchantRefundRuleRecord rule = repository.findStoredValueMerchantRefundRule(merchant)
                .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(merchant));
        boolean bonusFirst = StoredValueMerchantRefundRuleRecord.CONSUME_BONUS_FIRST.equals(rule.consumeOrder());
        return new CustomerStoredValueTopupRulesResponse("merchant", null, merchant, bonusBps, rule.consumeOrder(),
                bonusFirst ? "先扣赠送金、后扣本金" : "先扣本金、后扣赠送金", TOPUP_BONUS_TEXT, TOPUP_REFUND_TEXT,
                rule.refundAllowed(), rule.refundRatioBps());
    }

    // ENUM 第 86 行原句：「每笔充值拆为本金与赠送金两段分别记账」「退卡只退未消费的本金，赠送金不退现、未使用部分按活动规则作废或收回」。
    private static final String TOPUP_BONUS_TEXT = "每笔充值拆为本金与赠送金两段分别记账。";
    private static final String TOPUP_REFUND_TEXT = "退卡只退未消费的本金，赠送金不退现、未使用部分按活动规则作废或收回。";

    /** 第十八批第 11 条：某时点生效的赠送比例版本——商户级卡按发卡商户（不看项目），综合体通用卡按项目。 */
    private java.util.Optional<CustomerRepository.StoredValueBonusVersion> bonusVersion(String projectId, String merchantId, Instant at) {
        return merchantId == null
                ? repository.findEffectiveStoredValueBonus(TicketRewardConfigService.STORED_VALUE_BONUS, projectId, null, at)
                : repository.findEffectiveStoredValueBonus(TicketRewardConfigService.MERCHANT_CARD_BONUS, null, merchantId, at);
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
        runAfterCommit(event, idempotencyKey, () -> notifyStoredValueNow(txn, account, event, templateCode,
                idempotencyKey, amountCent, balanceAfterCent));
    }

    /**
     * TK-21-R1（BSM「储值」：「系统自动生成应退本金事项并通知发卡主体获授权人员」）：商户卡待退事项提交后短信通知发卡商户联系人
     * （商户档案主联系电话）；通用卡待退已进长沙项目公司财务退款列表，无短信收件人来源，不发。联系人缺失则只记日志。
     */
    /**
     * 复核 html m11（BSM L833「系统自动生成应退本金事项并通知发卡主体获授权人员」）：商户卡发给该发卡商户内持
     * customer.stored_value.refund 权限的在职员工；通用卡发给发卡主体（长沙项目公司＝卡所属项目、无商户归属）持财务退款执行权限
     * finance.refund.create 的在职员工。收件人筛选沿用 TK-27 待清台提醒范式（在职账号 + 有效权限码 + 员工档案手机号，无手机号不发）。
     */
    private void notifyPendingRefundAfterCommit(CustomerStoredValueAccountRecord account, long payoutCent) {
        if (notificationService == null || recipientJdbc == null || staffRepository == null) {
            return;
        }
        String key = "svpend:" + account.id();
        runAfterCommit(NOTIFICATION_EVENT_PENDING_REFUND, key, () -> {
            try {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("account_id", account.id());
                params.put("merchant_id", account.merchantId() == null ? "" : account.merchantId());
                params.put("customer_phone", repository.findCustomerById(account.customerId())
                        .map(CustomerRecord::maskedPhone).orElse(""));
                params.put("payout_cent", payoutCent);
                for (Map.Entry<String, String> recipient : pendingRefundRecipients(account).entrySet()) {
                    notificationService.notify(NOTIFICATION_DOMAIN, NOTIFICATION_EVENT_PENDING_REFUND, recipient.getValue(),
                            STORED_VALUE_PENDING_REFUND_TEMPLATE, params, key + ":" + recipient.getKey());
                }
            } catch (RuntimeException exception) {
                log.warn("stored value pending refund notification failed: account_id={}", account.id(), exception);
            }
        });
    }

    /** 发卡主体获授权人员 → 手机号（userId 有序）。 */
    private Map<String, String> pendingRefundRecipients(CustomerStoredValueAccountRecord account) {
        boolean merchantCard = account.merchantId() != null;
        if (!merchantCard && account.projectId() == null) {
            return Map.of();
        }
        String scopeSql = merchantCard ? "u.merchant_id = :scope" : "u.merchant_id IS NULL AND u.organization_id = :scope";
        String permission = merchantCard ? "customer.stored_value.refund" : "finance.refund.create";
        Map<String, String> recipients = new LinkedHashMap<>();
        recipientJdbc.query("SELECT u.id, u.username, u.account_type, u.organization_id, u.merchant_id, u.status FROM access_users u"
                        + " WHERE u.status = 'active' AND " + scopeSql + " ORDER BY u.id",
                Map.of("scope", merchantCard ? account.merchantId() : account.projectId()),
                (rs, n) -> new com.complex.platform.access.AccessActor(rs.getString("id"), rs.getString("username"),
                        rs.getString("account_type"), rs.getString("organization_id"), rs.getString("merchant_id"), rs.getString("status")))
                .forEach(actor -> {
                    if (!authorizationService.hasEffectivePermission(actor, permission)) {
                        return;
                    }
                    staffRepository.findProfileByUserId(actor.id())
                            .map(com.complex.platform.access.staff.StaffProfileRecord::phone)
                            .map(Strings::trimToNull)
                            .ifPresent(phone -> recipients.put(actor.id(), phone));
                });
        return recipients;
    }

    private void runAfterCommit(String event, String idempotencyKey, Runnable notify) {
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
