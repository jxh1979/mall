package com.complex.platform.dealer;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.dto.DealerRefundApprovalRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** 确定性交错：首次读取返回之前，另一项原收款核验已把退款作废。 */
class DealerRefundApprovalInterleavingTests {
    private final JdbcDealerOrderRepository repository = mock(JdbcDealerOrderRepository.class);
    private final JdbcDealerRepository dealers = mock(JdbcDealerRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final IdempotencyService idempotency = mock(IdempotencyService.class);
    private final SupervisorReviewService reviews = mock(SupervisorReviewService.class);
    private final DealerOrderService service = new DealerOrderService(repository, dealers, authorization,
            idempotency, mock(AuditEventService.class), null, reviews, null, null, null, mock(JdbcDealerDeliveryRepository.class), mock(DealerCounterpartyConfirmationService.class));

    private DealerOrderRefundRecord refund(String status) {
        return new DealerOrderRefundRecord("refund-race", "order-race", "payment-race", 3000L,
                status, "review-race", "applicant", null, null, null, "取消待退",
                Instant.parse("2026-10-18T00:00:00Z"), Instant.parse("2026-10-18T00:00:00Z"), null, null);
    }

    @Test
    void approvalCallbackCannotOverwriteReceiptVoidedAfterInitialRead() {
        AtomicReference<DealerOrderRefundRecord> stored = new AtomicReference<>(refund("pending_approval"));
        when(repository.findRefundByReview("review-race")).thenAnswer(call -> {
            DealerOrderRefundRecord snapshot = stored.get();
            stored.set(refund("voided"));
            return Optional.of(snapshot);
        });
        when(repository.lockRefund("refund-race")).thenAnswer(call -> Optional.of(stored.get()));
        doAnswer(call -> { stored.set(call.getArgument(0)); return null; }).when(repository).saveRefund(any());
        service.afterRefundDecision(new SupervisorReviewDecisionRecord("review-race", "refund", "refund",
                "dealer_order_refund", "refund-race", false, "approved", 1, 1, "approved",
                "既有审批完成", List.of(), Instant.now(), "applicant", "one_level_up", 1));
        assertThat(stored.get().status()).isEqualTo("voided");
    }

    @Test
    void oldApprovalCannotApproveAReplacementReview() {
        DealerOrderRefundRecord pending = refund("pending_approval");
        DealerOrderRefundRecord replacement = new DealerOrderRefundRecord(pending.id(), pending.orderId(), pending.paymentId(), pending.amountCent(),
                "pending_approval", "review-replacement", pending.requestedBy(), null, null, null, pending.reason(), pending.createdAt(), pending.updatedAt(), null, null);
        AtomicReference<DealerOrderRefundRecord> stored = new AtomicReference<>(pending);
        when(repository.findRefundByReview("review-race")).thenAnswer(call -> {
            stored.set(replacement);
            return Optional.of(pending);
        });
        when(repository.lockRefund("refund-race")).thenAnswer(call -> Optional.of(stored.get()));
        doAnswer(call -> { stored.set(call.getArgument(0)); return null; }).when(repository).saveRefund(any());
        service.afterRefundDecision(new SupervisorReviewDecisionRecord("review-race", "refund", "refund",
                "dealer_order_refund", "refund-race", false, "approved", 1, 1, "approved",
                "旧审批迟到", List.of(), Instant.now(), "applicant", "one_level_up", 1));
        assertThat(stored.get().reviewId()).isEqualTo("review-replacement");
        assertThat(stored.get().status()).isEqualTo("pending_approval");
    }

    @Test
    void reopeningApprovalCannotReplaceReceiptVoidedAfterInitialRead() {
        AtomicReference<DealerOrderRefundRecord> stored = new AtomicReference<>(refund("rejected"));
        when(repository.findRefund("refund-race")).thenAnswer(call -> {
            DealerOrderRefundRecord snapshot = stored.get();
            stored.set(refund("voided"));
            return Optional.of(snapshot);
        });
        when(repository.lockRefund("refund-race")).thenAnswer(call -> Optional.of(stored.get()));
        DealerOrderRecord order = mock(DealerOrderRecord.class);
        when(order.id()).thenReturn("order-race");
        when(order.dealerId()).thenReturn("dealer-race");
        when(repository.findOrder("order-race")).thenReturn(Optional.of(order));
        when(repository.lockOrder("order-race")).thenReturn(Optional.of(order));
        when(dealers.findDealer("dealer-race")).thenReturn(Optional.of(mock(DealerRecord.class)));
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        when(repository.findOrderProjectId("order-race")).thenReturn(Optional.of("project-race"));
        when(authorization.canReachProject(null, "project-race", DealerOrderService.PAYMENT_VERIFY)).thenReturn(true);
        when(idempotency.execute(anyString(), anyString(), any(), any(), eq(DealerOrderRefundRecord.class)))
                .thenAnswer(call -> ((Supplier<?>) call.getArgument(3)).get());
        when(reviews.evaluate(any())).thenReturn(new SupervisorReviewDecisionRecord("review-new", "refund", "refund",
                "dealer_order_refund", "refund-race", false, "pending_review", 1, null, null,
                "重开审批", List.of(), Instant.now(), "applicant", "one_level_up", 1));
        doAnswer(call -> { stored.set(call.getArgument(0)); return null; }).when(repository).saveRefund(any());
        assertThatThrownBy(() -> service.openRefundApproval("refund-race", new DealerRefundApprovalRequest("reopen", null)))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("DEALER_REFUND_STATUS_INVALID"));
        assertThat(stored.get().status()).isEqualTo("voided");
    }
}