package com.complex.platform.integration;

import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 批6 核销回传出站队列（HTML「券核销级回流」的回传半程）：
 * 渠道售出的通票在本地核销/冲正成功后，向渠道回传核销结果——真实供应商适配器未到位（开关默认关），
 * 先落 integration_retry_tasks 队列（task_type=verification_callback / verification_reversal_callback），
 * 由既有「回调/轮询/补偿」页可见可重试；适配器接入后由重试执行器消费出站。
 *
 * <p>AFTER_COMMIT 异步于核销主链：入队失败只记日志绝不回滚核销（本地核销为准，回传是通知义务）。
 * 幂等：任务 id 锚定核销记录 id（retry_vcb_/retry_vrb_ 前缀），重复事件 upsert 同一行不重复入队。
 * 非渠道订单（本地售出）查不到回流行 → 静默跳过，零开销。
 */
@Component
public class TicketChannelCallbackEnqueuer {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(TicketChannelCallbackEnqueuer.class);

    private final IntegrationRepository repository;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketChannelCallbackEnqueuer(IntegrationRepository repository) {
        this(repository, Clock.systemUTC());
    }

    TicketChannelCallbackEnqueuer(IntegrationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onVerified(SettlementSettledEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verification_id", event.verificationId());
        payload.put("entitlement_id", event.entitlementId());
        payload.put("entitlement_type", event.entitlementType());
        payload.put("used_times", event.usedTimes());
        payload.put("total_times", event.totalTimes());
        payload.put("verified_at", event.verifiedAt() == null ? null : event.verifiedAt().toString());
        payload.put("callback_kind", "verification");
        enqueue(event.orderId(), "verification_callback", "retry_vcb_" + event.verificationId(), payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onReversed(VerificationReversedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verification_id", event.verificationId());
        payload.put("reversal_id", event.reversalId());
        payload.put("entitlement_id", event.entitlementId());
        payload.put("callback_kind", "verification_reversal");
        enqueue(event.orderId(), "verification_reversal_callback", "retry_vrb_" + event.reversalId(), payload);
    }

    private void enqueue(String localOrderId, String taskType, String taskId, Map<String, Object> payload) {
        if (localOrderId == null || localOrderId.isBlank()) {
            return; // direct 出票无订单，必非渠道单。
        }
        try {
            repository.findTicketOrderSyncByLocalOrderId(localOrderId).ifPresent(sync -> {
                Map<String, Object> taskPayload = new LinkedHashMap<>(payload);
                taskPayload.put("local_ticket_order_id", localOrderId);
                taskPayload.put("channel", sync.channel());
                taskPayload.put("external_channel_order_id", sync.externalChannelOrderId());
                Instant now = clock.instant();
                repository.saveRetryTask(new IntegrationTaskRecord(
                        taskId,
                        "retry_task",
                        sync.providerCode(),
                        taskType,
                        "ticket",
                        sync.externalBusinessId(),
                        "open",
                        0,
                        5,
                        null,
                        null,
                        null,
                        taskPayload,
                        List.of(),
                        now,
                        now
                ));
            });
        } catch (RuntimeException exception) {
            // 回传入队失败不回滚核销主链：本地核销为准，回传缺一条由对账兜底，此处只留痕。
            LOGGER.warn("核销回传入队失败（不影响本地核销）：orderId={} taskType={}", localOrderId, taskType, exception);
        }
    }
}
