package com.complex.platform.finance;

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.VerificationReversedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * P1#19 跨模块闭环②b：finance 消费 ticketing 核销冲正事件单测。
 * 覆盖：核销冲正事件 → finance 消费者落"冲正已通知"留痕（不发起退款/不写冲正金额）、以及 reversalId 幂等。
 */
class VerificationReversedConsumerTests {

    private InMemoryAuditEventRepository auditEventRepository;
    private VerificationReversedConsumer consumer;

    @BeforeEach
    void setUp() {
        auditEventRepository = new InMemoryAuditEventRepository();
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        IdempotencyService idempotencyService = new IdempotencyService(
                new InMemoryIdempotencyRepository(), new ObjectMapper());
        consumer = new VerificationReversedConsumer(auditEventService, idempotencyService);
    }

    @Test
    void verificationReversedRecordsNoticeWithoutRefundOrReversalAmount() {
        consumer.onVerificationReversed(new VerificationReversedEvent(
                "ver_1", "rev_1", "ent_1", "order_1", "merchant_1", 2));

        assertThat(auditEventRepository.events).hasSize(1);
        AuditEvent event = auditEventRepository.events.getFirst();
        assertThat(event.eventName()).isEqualTo("finance.verification_reversed_notified");
        assertThat(event.businessId()).isEqualTo("rev_1");

        Map<String, Object> metadata = event.metadata();
        assertThat(metadata)
                .containsEntry("verification_id", "ver_1")
                .containsEntry("reversal_id", "rev_1")
                .containsEntry("entitlement_id", "ent_1")
                .containsEntry("order_id", "order_1")
                .containsEntry("merchant_id", "merchant_1")
                .containsEntry("verified_people", 2)
                // 正式闭环决策：核销冲正在 finance 侧无独立财务冲销对象，仅留痕。
                .containsEntry("action", "notified_no_financial_reversal_object");
        // 明确未发起任何退款 / 未写冲正金额：留痕中无 amount/refund 数值字段。
        assertThat(metadata).doesNotContainKeys("refund_id", "reversal_amount_cent", "amount_cent");
        // note 说明已是正式闭环（非待办）：不应再含 TODO 字样。
        assertThat(metadata.get("note")).asString().doesNotContain("TODO");
    }

    @Test
    void duplicateReversalIdIsIdempotentAndRecordsOnce() {
        VerificationReversedEvent event = new VerificationReversedEvent(
                "ver_1", "rev_1", "ent_1", "order_1", "merchant_1", 2);

        consumer.onVerificationReversed(event);
        consumer.onVerificationReversed(event);
        consumer.onVerificationReversed(event);

        // 同一 reversalId 仅落一次留痕（重复事件被幂等去重）。
        assertThat(auditEventRepository.events).hasSize(1);
    }

    @Test
    void distinctReversalIdsEachRecordOnce() {
        consumer.onVerificationReversed(new VerificationReversedEvent(
                "ver_1", "rev_1", "ent_1", "order_1", "merchant_1", 1));
        consumer.onVerificationReversed(new VerificationReversedEvent(
                "ver_2", "rev_2", "ent_2", "order_2", "merchant_1", 1));

        assertThat(auditEventRepository.events).hasSize(2);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::businessId)
                .containsExactly("rev_1", "rev_2");
    }

    @Test
    void missingReversalIdIsSkipped() {
        consumer.onVerificationReversed(new VerificationReversedEvent(
                "ver_1", "  ", "ent_1", "order_1", "merchant_1", 1));
        consumer.onVerificationReversed(new VerificationReversedEvent(
                "ver_2", null, "ent_2", "order_2", "merchant_1", 1));

        assertThat(auditEventRepository.events).isEmpty();
    }

    // ─── test doubles ──────────────────────────────────────────────────────────
}
