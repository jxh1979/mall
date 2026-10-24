package com.complex.platform.finance;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerBarScanOrderService;
import com.complex.platform.customer.CustomerPaymentService;
import com.complex.platform.customer.StoredValueService;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.parking.ParkingCustomerService;
import com.complex.platform.ticketing.TicketingService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ROOT-03-R18 客人在线付款服务端自动结算（BSM 第 859 行「客人付款成功后必须出票……支付渠道会校验出票状态」；第 1553 行「各渠道（微信、支付宝等）的
 * 支付与退款结果、流水号、入账口径和对账明细」）：客人在渠道付完款却没回前台（App 被杀、断网），也由服务端向渠道查单并入账、出票，不丢单。
 *
 * <p>每轮取 30 秒前创建、仍待付、已有渠道单号的客人在线付款单（购票、订台定金、储值充值、停车缴费、扫码点单），逐笔调用与客人确认相同的内核，每笔独立事务，
 * 未付款或失败只记日志、不中断本轮；支付渠道开关关时整轮跳过。逻辑在可直接调用的 {@link #settlePending}，@Scheduled 只做薄包装，
 * customer.payment.settle.scheduled.enabled=true 才执行（prod 开）。
 */
@Component
public class CustomerOnlinePaymentSettlementJob {

    private static final Logger log = LoggerFactory.getLogger(CustomerOnlinePaymentSettlementJob.class);

    private final JdbcTemplate jdbc;
    private final PaymentGatewayRouter gateway;
    private final TicketingService ticketing;
    private final CustomerPaymentService customerPayments;
    private final StoredValueService storedValue;
    private final ParkingCustomerService parking;
    private final CustomerBarScanOrderService scanOrders;
    private final TransactionTemplate perPayment;
    private final boolean scheduledEnabled;

    public CustomerOnlinePaymentSettlementJob(JdbcTemplate jdbc, PaymentGatewayRouter gateway, TicketingService ticketing,
            CustomerPaymentService customerPayments, StoredValueService storedValue, ParkingCustomerService parking,
            CustomerBarScanOrderService scanOrders, PlatformTransactionManager transactionManager,
            @Value("${customer.payment.settle.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.jdbc = jdbc;
        this.gateway = gateway;
        this.ticketing = ticketing;
        this.customerPayments = customerPayments;
        this.storedValue = storedValue;
        this.parking = parking;
        this.scanOrders = scanOrders;
        this.perPayment = new TransactionTemplate(transactionManager);
        this.scheduledEnabled = scheduledEnabled;
    }

    @Scheduled(fixedDelayString = "${customer.payment.settle.scheduled.fixed-delay-ms:60000}")
    public void scheduledSettle() {
        if (scheduledEnabled) {
            settlePending();
        }
    }

    /** @return 本轮入账的笔数。 */
    public int settlePending() {
        if (!gateway.isGatewayEnabled()) {
            return 0;
        }
        List<Map<String, Object>> pending = jdbc.queryForList("""
                SELECT id, business_order_type, business_order_id FROM finance_payment_intents
                 WHERE status = 'created' AND gateway_ref IS NOT NULL AND created_at < ?
                   AND business_order_type IN ('ticket_order', 'bar_reservation_deposit', 'stored_value_topup',
                                               'stored_value_topup_merchant', 'parking_fee', 'bar_scan_order')
                 ORDER BY created_at, id
                """, Timestamp.from(Instant.now().minusSeconds(30)));
        int settled = 0;
        for (Map<String, Object> row : pending) {
            String intentId = (String) row.get("id");
            try {
                perPayment.executeWithoutResult(status ->
                        settle((String) row.get("business_order_type"), (String) row.get("business_order_id"), intentId));
                settled++;
            } catch (RuntimeException notSettled) {
                log.info("customer online payment {} not settled this round: {}", intentId,
                        notSettled instanceof ApiException api ? api.code() : notSettled.getClass().getSimpleName());
            }
        }
        return settled;
    }

    private void settle(String businessOrderType, String businessOrderId, String intentId) {
        switch (businessOrderType) {
            case "ticket_order" -> ticketing.settleCustomerOnlinePayment(businessOrderId, intentId, null);
            case "bar_reservation_deposit" -> customerPayments.settleDeposit(businessOrderId, intentId, null);
            // ROOT-03-R7：客人扫码点单付完款没回前台，也由服务端查单成单（BSM 第 1025 行「付款成功订单才成立」）。
            case "bar_scan_order" -> scanOrders.settle(businessOrderId, null);
            case "parking_fee" -> parking.confirm(jdbc.queryForObject("SELECT customer_id FROM parking_payments WHERE id = ?",
                    String.class, businessOrderId), businessOrderId);
            default -> storedValue.settleTopupFromGateway(intentId);
        }
    }
}
