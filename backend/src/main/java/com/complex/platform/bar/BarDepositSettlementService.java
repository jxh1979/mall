package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 第十批 TK-24-R3：订台定金处置引擎——达标各笔原路退回、关台未达没收。
 *
 * <p>依据 BSM L1062「尚未处置的定金随当前接待会话关联到操作后的台位或台组：转台按目标台的最低消费进度判断，并台按合并台的
 * 合计最低消费进度判断；达标时，各笔定金分别原路退回」「已成功退还或没收的定金保留原结果，不因转台、并台或拆台自动追回、
 * 重新收取或重复退款」「客人已到店但未满足该台位最低消费的，定金不退、予以没收」；ENUM L103「达标已原路退回」；裁决 Q27。
 *
 * <p>触发点：落单/加菜（BarOrderService）、结账补足低消（BarBillingService）、并台/转台（BarTableSessionService）各调一次
 * {@link #evaluate}；关台（TK-27）调 {@link #forfeitAtClose}。「原路」= 退款应付项带原付款渠道与意向，实付由既有 finance 收付款登记完成。
 *
 * <p>复核 money I2（BSM L1062「退款处理中的记录继续关联原付款，只有实际退款成功才记为已退」）：达标时只挂原路退款应付（退款处理中），
 * 定金仍为已支付；应付经 FinanceService.settleArApItem 实付结清后才置「达标已原路退回」。已挂退款应付的定金不再重判、不被没收。
 */
@Service
public class BarDepositSettlementService {

    private final BarRepository repository;
    private final BarReceivableLedgerService receivableLedgerService;
    private final AuditEventService auditEventService;

    public BarDepositSettlementService(BarRepository repository, BarReceivableLedgerService receivableLedgerService,
            AuditEventService auditEventService) {
        this.repository = repository;
        this.receivableLedgerService = receivableLedgerService;
        this.auditEventService = auditEventService;
    }

    /** 台组进度达到合计阈值时，把链内（含整体转台前旧链）仍为已支付待判的定金逐笔原路退回；已处置的跳过。 */
    public void evaluate(String sessionId) {
        if (sessionId == null) {
            return;
        }
        if (BarMinSpend.consumedCent(repository, sessionId) < BarMinSpend.thresholdCent(repository, sessionId)) {
            return;
        }
        for (BarReservationRecord reservation : unsettledDeposits(sessionId)) {
            String arApItemId = receivableLedgerService.createDepositRefundPayable(reservation);
            auditEventService.recordSuccess("bar.reservation_deposit_refunded_on_threshold", "bar.reservation.manage",
                    "bar_reservation", reservation.id(), null, "deposit-threshold:" + reservation.id(),
                    Map.of("deposit_amount_cent", reservation.depositAmountCent(),
                            "deposit_channel", String.valueOf(reservation.depositChannel()),
                            "refund_status", "processing",
                            "session_id", sessionId,
                            "ar_ap_item_id", String.valueOf(arApItemId)));
        }
    }

    /** 落单/加菜/补足后的触发入口：订单无开台会话归属时无事可做。 */
    void evaluateForOrder(String orderId) {
        if (orderId != null) {
            repository.findSessionIdByOrderId(orderId).ifPresent(this::evaluate);
        }
    }

    /** 关台（TK-27 调用）：链内仍为已支付待判的定金一律没收（到店未达最低消费）。 */
    public void forfeitAtClose(String sessionId) {
        Instant now = Instant.now();
        for (BarReservationRecord reservation : unsettledDeposits(sessionId)) {
            BarReservationRecord saved = repository.saveReservation(reservation.withDepositState(
                    "settled", "forfeited", reservation.depositChannel(), reservation.depositPaidAt(), now, now));
            auditEventService.recordSuccess("bar.reservation_deposit_forfeited", "bar.reservation.manage",
                    "bar_reservation", saved.id(), null, "deposit-close:" + saved.id(),
                    Map.of("deposit_amount_cent", saved.depositAmountCent(),
                            "deposit_settlement", "forfeited", "session_id", sessionId));
        }
    }

    /** 台组合计低消阈值（TK-27 关台/拆台调用）。 */
    public long familyMinConsumptionCent(String sessionId) {
        return BarMinSpend.thresholdCent(repository, sessionId);
    }

    /** 台组累计消费进度（TK-27 关台/拆台调用）。 */
    public long familyConsumedCent(String sessionId) {
        return BarMinSpend.consumedCent(repository, sessionId);
    }

    private List<BarReservationRecord> unsettledDeposits(String sessionId) {
        return repository.familySessionIds(BarMinSpend.rootSessionId(repository, sessionId), true).stream()
                .map(repository::findTableSessionById)
                .flatMap(Optional::stream)
                .map(BarTableSessionRecord::reservationId)
                .filter(id -> id != null && !id.isBlank())
                .map(repository::findReservationById)
                .flatMap(Optional::stream)
                .filter(r -> "paid".equals(r.depositStatus()) && r.depositAmountCent() > 0)
                .filter(r -> !receivableLedgerService.hasDepositRefundPayable(r.id()))
                .toList();
    }
}
