package com.complex.platform.dealer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.dto.DealerCancelRequest;
import com.complex.platform.dealer.dto.DealerPaymentRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 首次读后并发事实已提交；写入须基于持锁后的订单，不能覆盖取消或已确认金额。 */
class DealerPaymentCancellationInterleavingTests {
    private final JdbcDealerOrderRepository repository = mock(JdbcDealerOrderRepository.class);
    private final JdbcDealerRepository dealers = mock(JdbcDealerRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final IdempotencyService idempotency = mock(IdempotencyService.class);
    private final DealerOrderService service = new DealerOrderService(repository, dealers, authorization,
            idempotency, mock(AuditEventService.class), null, null, null, null, null);

    @BeforeEach
    void scope() {
        when(dealers.findDealer("dealer-race")).thenReturn(Optional.of(mock(DealerRecord.class)));
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        when(repository.findOrderProjectId("order-race")).thenReturn(Optional.of("project-race"));
        when(authorization.canReachProject(null, "project-race", DealerOrderService.MANAGE)).thenReturn(true);
        when(repository.listPayments("order-race")).thenReturn(List.of());
        when(idempotency.execute(anyString(), anyString(), any(), any(), eq(DealerOrderRecord.class)))
                .thenAnswer(call -> ((Supplier<?>) call.getArgument(3)).get());
        when(idempotency.execute(anyString(), anyString(), any(), any(), eq(DealerOrderPaymentRecord.class)))
                .thenAnswer(call -> ((Supplier<?>) call.getArgument(3)).get());
    }

    private DealerOrderRecord order(String status, long paid) {
        DealerOrderRecord order = mock(DealerOrderRecord.class);
        when(order.id()).thenReturn("order-race");
        when(order.dealerId()).thenReturn("dealer-race");
        when(order.status()).thenReturn(status);
        when(order.paidAmountCent()).thenReturn(paid);
        when(order.totalAmountCent()).thenReturn(2000L);
        when(order.netReceivableCent()).thenReturn(2000L);
        if ("cancelled".equals(status)) when(order.cancelledAt()).thenReturn(Instant.now());
        return order;
    }

    @Test
    void paymentRegisteredAfterConcurrentCancellationRemainsLateAndCreatesRefund() {
        DealerOrderRecord stale = order("pending_payment", 0);
        DealerOrderRecord cancelled = order("cancelled", 0);
        when(repository.findOrder("order-race")).thenReturn(Optional.of(stale));
        when(repository.lockOrder("order-race")).thenReturn(Optional.of(cancelled));
        AtomicReference<DealerOrderRefundRecord> refund = new AtomicReference<>();
        doAnswer(call -> { refund.set(call.getArgument(0)); return null; }).when(repository).saveRefund(any());
        DealerOrderPaymentRecord saved = service.registerPayment("order-race",
                new DealerPaymentRequest("payment-race", "bank_transfer", 500L, Instant.now(), "ORIGINAL"));
        assertThat(saved.status()).isEqualTo("late");
        assertThat(saved.appliedCent()).isZero();
        assertThat(refund.get()).isNotNull();
        assertThat(refund.get().paymentId()).isEqualTo(saved.id());
    }

    @Test
    void targetCancellationsLockSharedOriginalPaymentsInOneStableOrder() {
        when(authorization.canReachProject(null, "project-race", DealerOrderService.REVIEW)).thenReturn(true);
        for (String paymentId : List.of("payment-a", "payment-b")) {
            DealerOrderPaymentRecord payment = mock(DealerOrderPaymentRecord.class);
            when(payment.id()).thenReturn(paymentId);
            when(payment.amountCent()).thenReturn(2000L);
            when(repository.findPayment(paymentId)).thenReturn(Optional.of(payment));
            DealerOrderOverpaymentRecord over = mock(DealerOrderOverpaymentRecord.class);
            when(over.paymentId()).thenReturn(paymentId);
            when(repository.findOverpayment("over-" + paymentId)).thenReturn(Optional.of(over));
        }
        java.util.ArrayList<List<String>> firstLockOrders = new java.util.ArrayList<>();
        int targetNumber = 0;
        // 两个目标分别以 B→A、A→B 顺序使用同两笔原款，模拟各自取消时的分配时间顺序。
        for (List<String> sourceOrder : List.of(List.of("payment-b", "payment-a"), List.of("payment-a", "payment-b"))) {
            String target = "target-" + ++targetNumber;
            DealerOrderRecord cancelling = order("cancelling", 2000);
            when(cancelling.id()).thenReturn(target);
            when(cancelling.cancelRequestedBy()).thenReturn("applicant");
            when(repository.findOrderProjectId(target)).thenReturn(Optional.of("project-race"));
            when(repository.lockOrder(target)).thenReturn(Optional.of(cancelling));
            when(repository.listOrderAllocations(target)).thenReturn(sourceOrder.stream().map(paymentId ->
                    new DealerOverpaymentAllocationRecord("allocation-" + paymentId, "over-" + paymentId, "prepayment_offset",
                            500L, target, List.of("proof"), "finance", Instant.now(), "已确认抵用", null)).toList());
            clearInvocations(repository);
            service.decideCancel(target, new com.complex.platform.dealer.dto.DealerOrderDecisionRequest("cancel-" + sourceOrder.getFirst(), "approve", "同意取消"));
            firstLockOrders.add(mockingDetails(repository).getInvocations().stream()
                    .filter(call -> "lockPayment".equals(call.getMethod().getName()))
                    .map(call -> (String) call.getArgument(0)).distinct().toList());
        }
        assertThat(firstLockOrders).containsExactly(List.of("payment-a", "payment-b"), List.of("payment-a", "payment-b"));
    }

    @Test
    void cancellationRequestPreservesReceiptConfirmedAfterInitialRead() {
        DealerOrderRecord stale = order("pending_payment", 1000);
        DealerOrderRecord confirmed = order("pending_shipment", 2000);
        when(repository.findOrder("order-race")).thenReturn(Optional.of(stale));
        when(repository.lockOrder("order-race")).thenReturn(Optional.of(confirmed));
        DealerOrderRecord saved = service.requestCancel("order-race", new DealerCancelRequest("cancel-race", "申请取消"));
        assertThat(saved.status()).isEqualTo("cancelling");
        assertThat(saved.paidAmountCent()).isEqualTo(2000L);
        assertThat(saved.statusBeforeCancel()).isEqualTo("pending_shipment");
    }
}
