package com.complex.platform.integration;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.complex.platform.finance.FinanceService;
import com.complex.platform.ticketing.TicketOrderService;
import com.complex.platform.ticketing.TicketingService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TicketingChannelTicketIssueAdapterTests {
    @Test
    void zeroAmountIsRejectedWithoutInventingARefundOrIssuingTicket() {
        TicketingService tickets = mock(TicketingService.class);
        TicketOrderService orders = mock(TicketOrderService.class);
        FinanceService finance = mock(FinanceService.class);
        var adapter = new TicketingChannelTicketIssueAdapter(tickets);
        ReflectionTestUtils.setField(adapter, "orderService", orders);
        ReflectionTestUtils.setField(adapter, "financeService", finance);
        when(orders.amusementSalesSuspended("ticket-type", "merchant")).thenReturn(true);
        var command = new ChannelTicketIssuePort.IssueCommand("key", "ticket-type", null, 1,
                LocalDate.of(2026, 10, 18), "meituan", null, "停售拒收");
        var rejection = adapter.rejectSuspendedPayment(command, "channel-order", 0, "original-ref", "merchant");
        assertThat(rejection).isPresent();
        assertThat(rejection.orElseThrow().refundId()).isNull();
        verifyNoInteractions(finance, tickets);
    }

    @Test
    void receivedMoneyKeepsItsOriginalChannelReferenceAndRefundId() {
        TicketingService tickets = mock(TicketingService.class);
        TicketOrderService orders = mock(TicketOrderService.class);
        FinanceService finance = mock(FinanceService.class);
        var adapter = new TicketingChannelTicketIssueAdapter(tickets);
        ReflectionTestUtils.setField(adapter, "orderService", orders);
        ReflectionTestUtils.setField(adapter, "financeService", finance);
        when(orders.amusementSalesSuspended("ticket-type", "merchant")).thenReturn(true);
        when(finance.recordRejectedTicketPaymentRefund(anyString(), anyString(), anyLong(),
                anyString(), anyString(), anyString(), anyString(), nullable(String.class), anyString())).thenReturn("refund-registered");
        var command = new ChannelTicketIssuePort.IssueCommand("key", "ticket-type", null, 1,
                LocalDate.of(2026, 10, 18), "meituan", null, "停售拒收");
        assertThat(adapter.rejectSuspendedPayment(command, "channel-order", 8800, "original-ref", "merchant")
                .orElseThrow().refundId()).isEqualTo("refund-registered");
        verify(finance).recordRejectedTicketPaymentRefund(eq("channel_ticket_rejection"), eq("channel-order"),
                eq(8800L), eq("meituan"), eq("original-ref"), eq("merchant"), eq("merchant"), nullable(String.class), anyString());
        verifyNoInteractions(tickets);
    }
}
