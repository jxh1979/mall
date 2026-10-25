package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.common.LocalZone;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.BarDepositRefunded;
import com.complex.platform.notification.NotificationService;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * OBS-13-03 订台相关短信。BSM 第 1596 行「给客人：……订台成功确认（员工代订的同时向客人知会代订人与订台内容）、最晚到店时间前提醒……、
 * 超时释放台位通知、定金没收通知、定金原路退回通知、订台取消确认」；DEC-R8-97-20。
 *
 * <p>收件人：会员订台取会员手机号，员工代订取录入时加密留存的客人手机号；都没有时记失败审计、不发。业务事务提交后才发（回滚不发），
 * 每类事件每笔订台只发一次（request_id＝barres:事件:订台号）；发送失败只记日志不影响业务。短信供应商未接入时由发送器如实记失败。
 */
@Component
public class BarReservationNotifier {

    static final String DOMAIN = "bar_reservation";
    private static final Logger log = LoggerFactory.getLogger(BarReservationNotifier.class);
    private static final DateTimeFormatter TIME_TEXT = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm");

    private final BarRepository repository;
    private final CustomerRepository customers;
    private final NotificationService notifications;
    private final AuditEventService audit;
    private final AuthorizationService authorizationService;
    private final JdbcStaffRepository staffRepository;

    public BarReservationNotifier(BarRepository repository, CustomerRepository customers, NotificationService notifications,
            AuditEventService audit, AuthorizationService authorizationService, JdbcStaffRepository staffRepository) {
        this.repository = repository;
        this.customers = customers;
        this.notifications = notifications;
        this.audit = audit;
        this.authorizationService = authorizationService;
        this.staffRepository = staffRepository;
    }

    /** 员工代订成功：同时知会代订人（当前员工档案姓名，无档案用登录名）。 */
    public void notifyStaffBooked(BarReservationRecord reservation) {
        var actor = authorizationService.currentActor();
        String bookedBy = staffRepository.findProfileByUserId(actor.id()).map(StaffProfileRecord::name)
                .filter(name -> !name.isBlank()).orElse(actor.username());
        notify(reservation, "confirmed", Map.of("booked_by", bookedBy));
    }

    public void notify(BarReservationRecord reservation, String event) {
        notify(reservation, event, Map.of());
    }

    /**
     * 第十四批 OBS-13-03-R2⑤／R6（BSM 第 1062 行「退款处理中的记录继续关联原付款，只有实际退款成功才记为已退」）：订台定金退款实际成功——
     * 达标退款应付实付结清，或原路待退（抢台失败、订台已取消后才到账、免费时限内取消）经财务核实成功。仍为已支付（退款处理中）的回写已退还，
     * 再发定金原路退回通知。与退款结果同一事务。
     */
    @EventListener
    public void onDepositRefunded(BarDepositRefunded event) {
        BarReservationRecord reservation = repository.findReservationByIdForUpdate(event.reservationId()).orElse(null);
        if (reservation == null) {
            return;
        }
        if ("paid".equals(reservation.depositStatus())) {
            Instant now = Instant.now();
            reservation = repository.saveReservation(reservation.withDepositState("settled", "refunded",
                    reservation.depositChannel(), reservation.depositPaidAt(), now, now));
        }
        notify(reservation, "deposit_refunded");
    }

    private void notify(BarReservationRecord reservation, String event, Map<String, Object> extra) {
        String requestId = "barres:" + event + ":" + reservation.id();
        try {
            String phone = recipient(reservation);
            if (phone == null) {
                audit.recordFailure("bar.reservation.notification.not_sent", "not_applicable", "bar_reservation", reservation.id(),
                        requestId, requestId, "缺少客人手机号，未发送");
                return;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("table_name", repository.findTableById(reservation.tableId()).map(BarTableRecord::tableName).orElse(""));
            params.put("reserved_at", text(reservation.reservedAt()));
            params.put("latest_arrival_at", text(reservation.latestArrivalAt()));
            params.put("guest_count", reservation.guestCount());
            if (reservation.depositPaidAt() != null) {
                params.put("deposit_amount_cent", reservation.depositAmountCent());
            }
            params.putAll(extra);
            runAfterCommit(() -> {
                try {
                    notifications.notify(DOMAIN, event, phone, "sms." + DOMAIN + "." + event, params, requestId);
                } catch (RuntimeException failure) {
                    log.warn("bar reservation notification failed: request_id={}", requestId, failure);
                }
            });
        } catch (RuntimeException failure) {
            log.warn("bar reservation notification not prepared: request_id={}", requestId, failure);
        }
    }

    private String recipient(BarReservationRecord reservation) {
        if (reservation.customerId() != null && !reservation.customerId().isBlank()) {
            return customers.findCustomerById(reservation.customerId()).map(CustomerRecord::phone).orElse(null);
        }
        return repository.findReservationCustomerPhone(reservation.id()).orElse(null);
    }

    private static String text(Instant at) {
        return at == null ? "" : TIME_TEXT.format(at.atZone(LocalZone.ID));
    }

    /** 同 StoredValueService：有事务时提交后发，无事务（定时作业、关台直调）即刻发。 */
    private static void runAfterCommit(Runnable send) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
            return;
        }
        send.run();
    }
}
