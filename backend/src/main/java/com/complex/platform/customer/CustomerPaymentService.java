package com.complex.platform.customer;

// 文件版本：V0.02

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.bar.BarEntryService;
import com.complex.platform.bar.BarRepository;
import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.customer.dto.CustomerPaymentRequest;
import com.complex.platform.customer.dto.CustomerTicketOrderResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.TicketOrderService;
import com.complex.platform.ticketing.TicketingService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ROOT-03-R4 客人本人订单在线付款（BSM 第 859 行「通过官方 App/小程序购票、预定、确认订单、查看支付结果。客人付款成功后必须出票……」；
 * 第 860 行「官方 App 和小程序都可以订台，订台需要支付定金」；BSM 第 1552～1554 行聚合支付、不自研支付通道）。
 *
 * <p>发起：只许本人订单或订台，金额由服务端定（购票＝待付金额，ROOT-03-R20；定金＝该订台定金），同一单尚未付款的旧付款单作废；响应带渠道签发的
 * 拉起参数（ROOT-03-R18 launch），渠道未启用时照常返回渠道状态、不带拉起参数。确认：只认渠道查单成功（渠道未启用 409 PAYMENT_GATEWAY_UNAVAILABLE、
 * 未成功 409 PAYMENT_NOT_SUCCEEDED，订单不变）；购票付足即同事务出票（ROOT-03-R17），出票失败已收各笔原路待退、订单取消，返回 409
 * TICKET_ISSUE_FAILED_REFUNDING。重复确认返回订单现状。服务端结算作业复用同一确认内核（{@link #settleDeposit}、{@link TicketingService}）。
 */
@Service
public class CustomerPaymentService {

    static final String DEPOSIT_ORDER_TYPE = "bar_reservation_deposit";

    private final CustomerService customers;
    private final TicketingService ticketing;
    private final FinanceService finance;
    private final BarRepository bars;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final BarEntryService barEntry;
    private final TransactionTemplate transaction;
    private final CustomerProjectSwitch projectSwitch;

    public CustomerPaymentService(CustomerService customers, TicketingService ticketing, FinanceService finance,
            BarRepository bars, IdempotencyService idempotency, AuditEventService audit, BarEntryService barEntry,
            PlatformTransactionManager transactionManager, CustomerProjectSwitch projectSwitch) {
        this.customers = customers;
        this.ticketing = ticketing;
        this.finance = finance;
        this.bars = bars;
        this.idempotency = idempotency;
        this.audit = audit;
        this.barEntry = barEntry;
        this.transaction = new TransactionTemplate(transactionManager);
        this.projectSwitch = projectSwitch;
    }

    @Transactional
    public Map<String, Object> startTicketPayment(String customerToken, String orderId, CustomerPaymentRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            CustomerRecord customer = customers.requireCustomerRecord(customerToken);
            // PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：按订单综合体判按综合体的客人端开关（不取 X-Project-Id），先于幂等。
            projectSwitch.ticketOrder(orderId, customer.id());
            String channel = channelFor(customers.sessionTerminal(customerToken), request == null ? null : request.channel());
            @SuppressWarnings("unchecked")
            Map<String, Object> view = idempotency.execute("customerTicketPayment:" + customer.id() + ":" + orderId, key, request,
                    () -> launchView(ticketing.createCustomerOnlinePayment(orderId, customer.id(), channel, key)), Map.class);
            audit.recordSuccess("customer.payment_created", "not_applicable", "ticket_order", orderId, null, key,
                    Map.of("payment_intent_id", view.get("payment_intent_id"), "amount_cent", view.get("amount_cent")));
            return view;
        } catch (RuntimeException failure) {
            recordFailure("customer.payment_create_failed", "ticket_order", orderId, key, failure);
            throw failure;
        }
    }

    /** 不加事务：确认内核自带事务，出票失败的原路待退须先落库，再向客人报 409。 */
    public CustomerTicketOrderResponse confirmTicketPayment(String customerToken, String orderId, String paymentIntentId, String key) {
        try {
            CustomerRecord customer = customers.requireCustomerRecord(customerToken);
            // 客人确认入口才判；服务端结算作业无请求上下文、不经此入口。
            projectSwitch.ticketOrder(orderId, customer.id());
            TicketOrderService.OnlineSettlement settled = ticketing.settleCustomerOnlinePayment(orderId, paymentIntentId, customer.id());
            if (settled.issueFailed()) {
                throw ApiException.conflict("TICKET_ISSUE_FAILED_REFUNDING", "付款成功但出票失败，款项将原路退回");
            }
            audit.recordSuccess("customer.payment_confirmed", "not_applicable", "ticket_payment_intent", paymentIntentId, null, key,
                    Map.of("order_id", orderId, "pay_status", settled.order().payStatus()));
            return customers.orderView(settled.order());
        } catch (RuntimeException failure) {
            recordFailure("customer.payment_confirm_failed", "ticket_payment_intent", paymentIntentId, key, failure);
            throw failure;
        }
    }

    @Transactional
    public Map<String, Object> startDepositPayment(String customerToken, String reservationId, CustomerPaymentRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            CustomerRecord customer = customers.requireCustomerRecord(customerToken);
            // 按订台所属酒吧的综合体判，先于幂等。
            projectSwitch.reservation(reservationId);
            String channel = channelFor(customers.sessionTerminal(customerToken), request == null ? null : request.channel());
            @SuppressWarnings("unchecked")
            Map<String, Object> view = idempotency.execute("customerDepositPayment:" + customer.id() + ":" + reservationId, key, request,
                    () -> {
                        BarReservationRecord reservation = ownReservation(reservationId, customer.id());
                        if ("cancelled".equals(reservation.status()) || !"pending".equals(reservation.depositStatus())) {
                            throw ApiException.conflict("BAR_DEPOSIT_NOT_PAYABLE", "只有待支付状态的订金才能支付");
                        }
                        if (reservation.depositIntentId() != null && finance.gatewayReportsPaid(reservation.depositIntentId())) {
                            // ROOT-03-R18：作废旧单前先向渠道查——客人已在渠道付款只是没回前台的，按同一确认内核到账（置已支付并占台，抢不到的原路待退）、
                            // 不再拉起新的一笔，返回那笔已成功的付款。渠道确认未付或渠道未启用（不可能付成）的才作废。
                            settleDeposit(reservation.id(), reservation.depositIntentId(), customer.id());
                            return launchView(finance.findPaymentIntent(reservation.depositIntentId()).orElseThrow());
                        }
                        // ROOT-03-R19（ENUM 第 100 行「冲突时先支付定金者得」）：台位已被别人占着的不再拉起付款，免得付了钱才被原路退回。
                        if (!bars.isTableReservable(reservation.tableId())) {
                            throw BarEntryService.tableAlreadyReserved();
                        }
                        if (reservation.depositIntentId() != null) {
                            finance.voidPendingPaymentIntent(reservation.depositIntentId());
                        }
                        // BSM 第 1062 行「定金直接进该卡座所属的酒吧商户」：收款主体＝订台所属商户（商户直收），与储值商户卡同一资金归属契约。
                        PaymentIntentRecord intent = finance.createPaymentIntent(new PaymentIntentRequest(
                                "dep_" + sha256Hex(customer.id() + "|" + reservation.id() + "|" + requireText(key, "idempotency_key")).substring(0, 40),
                                null, List.of(), DEPOSIT_ORDER_TYPE, reservation.id(), reservation.depositAmountCent(), "CNY", channel,
                                null, reservation.merchantId(), depositFundMode(reservation)));
                        bars.saveReservation(reservation.withDepositIntent(intent.id(), Instant.now()));
                        return launchView(intent);
                    }, Map.class);
            audit.recordSuccess("customer.payment_created", "not_applicable", "bar_reservation", reservationId, null, key,
                    Map.of("payment_intent_id", view.get("payment_intent_id"), "amount_cent", view.get("amount_cent")));
            return view;
        } catch (RuntimeException failure) {
            recordFailure("customer.payment_create_failed", "bar_reservation", reservationId, key, failure);
            throw failure;
        }
    }

    /**
     * 不加事务：确认内核在自己的事务里提交，抢台失败时取消与原路待退须先落库，再向客人报 409（ROOT-03-R19 ENUM 第 100 行「冲突时先支付定金者得」）。
     */
    public BarReservationRecord confirmDepositPayment(String customerToken, String reservationId, String paymentIntentId, String key) {
        try {
            CustomerRecord customer = customers.requireCustomerRecord(customerToken);
            // 作业侧 settleDeposit(customerId=null) 不经此入口。
            projectSwitch.reservation(reservationId);
            BarReservationRecord saved = transaction.execute(status -> settleDeposit(reservationId, paymentIntentId, customer.id()));
            if ("cancelled".equals(saved.status())) {
                if (!"paid".equals(saved.depositStatus())) {
                    throw ApiException.conflict("BAR_DEPOSIT_NOT_PAYABLE", "该订台已取消，已付定金将原路退回");
                }
                throw ApiException.conflict("BAR_TABLE_ALREADY_RESERVED", "该台位已被他人先付定金预订，您的定金将原路退回");
            }
            audit.recordSuccess("customer.payment_confirmed", "not_applicable", "bar_reservation_deposit_intent", paymentIntentId, null, key,
                    Map.of("reservation_id", reservationId, "deposit_status", saved.depositStatus()));
            return saved;
        } catch (RuntimeException failure) {
            recordFailure("customer.payment_confirm_failed", "bar_reservation_deposit_intent", paymentIntentId, key, failure);
            throw failure;
        }
    }

    /**
     * 定金到账确认内核（客人确认与服务端结算作业共用，调用方提供事务；customerId 为空＝作业）：只认本订台当前付款意向，渠道查单成功才按
     * 员工登记定金同口径置已支付并落付款渠道（第十批 TK-24：达标退回按此原路）；已置已支付的重复确认返回原结果。
     */
    public BarReservationRecord settleDeposit(String reservationId, String paymentIntentId, String customerId) {
        BarReservationRecord reservation = customerId == null
                ? bars.findReservationByIdForUpdate(requireText(reservationId, "reservation_id"))
                        .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台不存在或已被删除"))
                : ownReservation(reservationId, customerId);
        if (!requireText(paymentIntentId, "payment_intent_id").equals(reservation.depositIntentId())) {
            throw ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "支付意向不存在或不属于此订台");
        }
        PaymentIntentRecord confirmed = finance.confirmOnlinePaymentIntent(paymentIntentId, reservation.merchantId(),
                depositFundMode(reservation));
        if ("paid".equals(reservation.depositStatus())) {
            return reservation;
        }
        if ("cancelled".equals(reservation.status()) || !"pending".equals(reservation.depositStatus())) {
            // BSM 第 1062 行「每笔定金保留原订台单、原付款人、付款渠道和金额……退款处理中的记录继续关联原付款」：订台已取消（员工取消或超时作废）后
            // 才到账的定金，与抢台败者同一写法登记原路待退并正常提交（付款单已入账，结算作业不再重试），向客人报 409。
            String refundId = finance.recordRejectedTicketPaymentRefund("bar_deposit_rejection", reservation.id(), confirmed.amountCent(),
                    confirmed.channel(), confirmed.gatewayRef(), reservation.merchantId(), reservation.barId(),
                    "订台已取消后定金才到账，原路退回");
            audit.recordSuccess("bar.reservation_deposit_refund_on_conflict", "not_applicable", "bar_reservation", reservation.id(),
                    null, "deposit-late:" + paymentIntentId, Map.of("deposit_amount_cent", confirmed.amountCent(),
                            "deposit_channel", String.valueOf(confirmed.channel()), "refund_status", "requested", "refund_id", refundId));
            return reservation;
        }
        Instant now = Instant.now();
        BarReservationRecord saved = bars.saveReservation(reservation.withDepositState("paid", reservation.depositSettlement(),
                confirmed.channel(), now, null, now));
        audit.recordSuccess("bar.reservation_deposit_paid", "not_applicable", "bar_reservation", saved.id(), null, paymentIntentId,
                Map.of("deposit_amount_cent", saved.depositAmountCent(), "channel", confirmed.channel()));
        // ROOT-03-R19（ENUM 第 100 行「客人自助订台支付定金成功……该台位即进入已预留……冲突时先支付定金者得」）：到账即占台，抢不到的取消并原路退回。
        barEntry.holdOnDepositPaid(saved, confirmed.gatewayRef());
        return bars.findReservationById(saved.id()).orElse(saved);
    }

    /** 定金收款主体的资金模式：有所属商户＝商户直收（merchant_prepaid，直达该酒吧商户）；平台直营台位为空＝平台收款。 */
    private static String depositFundMode(BarReservationRecord reservation) {
        return reservation.merchantId() == null ? null : "merchant_prepaid";
    }

    /** BSM 第 585 行说明「客人付款走第三方微信、支付宝」：App 可选微信或支付宝，小程序只能拉起微信支付；不填＝微信。 */
    static String channelFor(String terminal, String requested) {
        String channel = requested == null || requested.isBlank() ? "wechat" : requested.trim();
        if (!Set.of("wechat", "alipay").contains(channel) || ("miniapp".equals(terminal) && !"wechat".equals(channel))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "channel is invalid");
        }
        return channel;
    }

    private BarReservationRecord ownReservation(String reservationId, String customerId) {
        BarReservationRecord reservation = bars.findReservationByIdForUpdate(requireText(reservationId, "reservation_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台不存在或已被删除"));
        if (!customerId.equals(reservation.customerId())) {
            throw ApiException.forbidden("CUSTOMER_RESERVATION_FORBIDDEN", "该订台不属于当前客人");
        }
        return reservation;
    }

    private static Map<String, Object> launchView(PaymentIntentRecord intent) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("payment_intent_id", intent.id());
        view.put("business_order_id", intent.businessOrderId());
        view.put("amount_cent", intent.amountCent());
        view.put("channel", intent.channel());
        view.put("gateway_status", intent.gatewayStatus());
        view.put("payment_url", intent.paymentUrl());
        view.put("launch", intent.launch());
        return view;
    }

    private void recordFailure(String event, String businessType, String businessId, String key, RuntimeException failure) {
        try {
            audit.recordFailure(event, "not_applicable", businessType, businessId == null ? "unknown" : businessId, null, key,
                    failure instanceof ApiException api ? api.code() : failure.getClass().getSimpleName());
        } catch (RuntimeException auditFailure) {
            failure.addSuppressed(auditFailure);
        }
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing unavailable", exception);
        }
    }
}
