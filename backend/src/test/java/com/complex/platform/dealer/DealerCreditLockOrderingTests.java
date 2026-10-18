package com.complex.platform.dealer;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.dto.*;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** 五个信用状态入口统一经销商→订单→原后续资源锁，锁后复核事实。真实双事务另由 JDBC 类证明。 */
class DealerCreditLockOrderingTests {
    private final JdbcDealerOrderRepository orders=mock(JdbcDealerOrderRepository.class);
    private final JdbcDealerRepository dealers=mock(JdbcDealerRepository.class);
    private final JdbcDealerDeliveryRepository delivery=mock(JdbcDealerDeliveryRepository.class);
    private final AuthorizationService auth=mock(AuthorizationService.class);
    private final IdempotencyService idem=mock(IdempotencyService.class);
    private final AuditEventService audit=mock(AuditEventService.class);
    private final DealerOrderService money=new DealerOrderService(orders,dealers,auth,idem,audit,null,null,null,null,null,delivery);

    private DealerOrderRecord order(String id,String dealerId) {
        var order=mock(DealerOrderRecord.class);when(order.id()).thenReturn(id);when(order.dealerId()).thenReturn(dealerId);
        when(order.salesEntityId()).thenReturn("entity");when(order.status()).thenReturn("shipping");
        var dealer=mock(DealerRecord.class);
        when(dealers.findDealer(dealerId)).thenReturn(Optional.of(dealer));when(dealers.lockDealerCredit(dealerId)).thenReturn(Optional.of(dealer));
        when(orders.findOrder(id)).thenReturn(Optional.of(order));when(orders.lockOrder(id)).thenReturn(Optional.of(order));
        when(orders.findOrderProjectId(id)).thenReturn(Optional.of("project"));
        when(auth.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        when(auth.canReachProject(null,"project",DealerOrderService.PAYMENT_VERIFY)).thenReturn(true);
        when(auth.canReachProject(null,"project",DealerOrderService.SHIP)).thenReturn(true);
        return order;
    }

    @Test
    void paymentVerificationLocksDealerBeforeOrderAndPayment() {
        order("order","dealer");var payment=mock(DealerOrderPaymentRecord.class);
        when(payment.id()).thenReturn("payment");when(payment.orderId()).thenReturn("order");when(payment.status()).thenReturn("pending_verification");
        when(orders.findPayment("payment")).thenReturn(Optional.of(payment));
        assertThatThrownBy(()->money.verifyPayment("payment",new DealerPaymentVerifyRequest("key","invalid","止于业务校验"))).isInstanceOf(ApiException.class);
        var sequence=inOrder(dealers,orders);sequence.verify(dealers).lockDealerCredit("dealer");sequence.verify(orders).lockOrder("order");sequence.verify(orders).lockPayment("payment");
    }

    @Test
    void overpaymentApplicationLocksDealerThenBothOrdersInStableOrder() {
        order("a-source","dealer");order("b-target","dealer");var over=mock(DealerOrderOverpaymentRecord.class);
        when(over.id()).thenReturn("over");when(over.orderId()).thenReturn("a-source");when(over.dealerId()).thenReturn("dealer");when(over.salesEntityId()).thenReturn("entity");
        when(orders.findOverpayment("over")).thenReturn(Optional.of(over));
        assertThatThrownBy(()->money.confirmOverpaymentUse("over",new DealerOverpaymentUseRequest("key","offset_order","b-target","双方确认",100L,List.of()))).isInstanceOf(ApiException.class);
        var sequence=inOrder(dealers,orders);sequence.verify(dealers).lockDealerCredit("dealer");sequence.verify(orders).lockOrder("a-source");sequence.verify(orders).lockOrder("b-target");
    }

    @Test
    void physicalAdjustmentLocksDealerBeforeOrderAndRechecksResolution() {
        order("order","dealer");var resolution=mock(DealerDeliveryRecord.Resolution.class);var difference=mock(DealerDeliveryRecord.Discrepancy.class);
        when(resolution.id()).thenReturn("plan");when(resolution.discrepancyId()).thenReturn("diff");when(resolution.physicalCompletedAt()).thenReturn(Instant.now());
        when(difference.orderId()).thenReturn("order");when(delivery.resolution("plan")).thenReturn(Optional.of(resolution));when(delivery.discrepancy("diff")).thenReturn(Optional.of(difference));
        when(idem.execute(anyString(),anyString(),any(),any(),eq(DealerDeliveryRecord.Resolution.class))).thenAnswer(c->((Supplier<?>)c.getArgument(3)).get());
        var service=new DealerDeliveryService(orders,delivery,money,auth,idem,null,null,null,null,audit);
        assertThatThrownBy(()->service.completePhysical("plan",new DealerDeliveryPhysicalRequest("key",Instant.now(),List.of())))
                .isInstanceOfSatisfying(ApiException.class,e->org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("DEALER_DELIVERY_PHYSICAL_RECORDED"));
        var sequence=inOrder(dealers,orders);sequence.verify(dealers).lockDealerCredit("dealer");sequence.verify(orders).lockOrder("order");
    }

    @Test
    void returnInspectionLocksDealerBeforeOrderAndRejectsStatusChangedWhileWaiting() {
        order("order","dealer");var returns=mock(JdbcDealerReturnRepository.class);
        var before=mock(DealerReturnRecord.class);when(before.orderId()).thenReturn("order");when(before.dealerId()).thenReturn("dealer");when(before.status()).thenReturn("inspecting");
        var after=mock(DealerReturnRecord.class);when(after.orderId()).thenReturn("order");when(after.dealerId()).thenReturn("dealer");when(after.status()).thenReturn("completed");
        when(returns.findReturn("return")).thenReturn(Optional.of(before));
        when(dealers.lockDealerCredit("dealer")).thenAnswer(c->{when(returns.findReturn("return")).thenReturn(Optional.of(after));return Optional.of(mock(DealerRecord.class));});
        var service=new DealerReturnService(returns,orders,dealers,auth,idem,audit,null,delivery,null,null);
        assertThatThrownBy(()->service.inspectReturn("return",new DealerReturnInspectionRequest("key",List.of(new DealerReturnInspectionLineRequest("item","",1,1,0,"实际验收")),"复核")))
                .isInstanceOfSatisfying(ApiException.class,e->org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("DEALER_RETURN_STATUS_INVALID"));
        var sequence=inOrder(dealers,orders);sequence.verify(dealers).lockDealerCredit("dealer");sequence.verify(orders).lockOrder("order");
        verify(returns,never()).saveReturn(any());
    }

    @Test
    void overdueScanLocksAllCandidateDealersSortedBeforeAnyOrderAndRechecks() {
        var z=order("order-z","dealer-z");var a=order("order-a","dealer-a");
        when(orders.listBalanceOverdue(any())).thenReturn(List.of(z,a));
        for(var candidate:List.of(z,a)) {
            when(candidate.netReceivableCent()).thenReturn(1_000L);when(candidate.balanceDueAt()).thenReturn(Instant.now().minusSeconds(60));
            var paid=mock(DealerOrderRecord.class);when(paid.netReceivableCent()).thenReturn(1_000L);when(paid.paidAmountCent()).thenReturn(1_000L);
            when(orders.lockOrder(candidate.id())).thenReturn(Optional.of(paid));
        }
        money.markOverdue(Instant.now());
        var sequence=inOrder(dealers,orders);sequence.verify(dealers).lockDealerCredit("dealer-a");sequence.verify(dealers).lockDealerCredit("dealer-z");
        sequence.verify(orders).lockOrder("order-z");sequence.verify(orders).lockOrder("order-a");
        verify(dealers,never()).setCreditSuspended(anyString(),anyBoolean());
    }
}
