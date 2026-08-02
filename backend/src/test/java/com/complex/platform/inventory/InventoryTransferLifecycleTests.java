package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.TraceabilityService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.gate.TerminalSwitchGuard;
import com.complex.platform.inventory.dto.ReceiveTransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * AUD-002/003：借调归还生命周期（return/cancel）单测。
 * 借用型(lending+borrow)收货后归还→溯源码归属翻回出让方+状态 returned；
 * 已挂应付的调拨单取消→冲销应付(未收款才可冲)+状态 cancelled。
 *
 * <p>InventoryRepository 用 Mockito mock + 有状态 Answer(orders/lines/traceOwners 三张内存表),
 * 只桩流转/溯源相关方法,避免手写上百个接口方法。
 */
class InventoryTransferLifecycleTests {

    private final Map<String, InventoryTransferOrderRecord> orders = new LinkedHashMap<>();
    private final Map<String, List<InventoryTransferLineRecord>> lines = new LinkedHashMap<>();
    private final Map<String, String> traceOwners = new LinkedHashMap<>();

    private InventoryRepository repository;
    private FinanceRepository financeRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        orders.clear();
        lines.clear();
        traceOwners.clear();
        repository = mock(InventoryRepository.class);
        when(repository.findTransferOrderById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(orders.get(inv.<String>getArgument(0))));
        when(repository.listTransferLinesByOrder(anyString()))
                .thenAnswer(inv -> lines.getOrDefault(inv.<String>getArgument(0), List.of()));
        when(repository.saveTransferOrder(any())).thenAnswer(inv -> {
            InventoryTransferOrderRecord o = inv.getArgument(0);
            orders.put(o.id(), o);
            return o;
        });
        when(repository.traceCodeExists(anyString()))
                .thenAnswer(inv -> traceOwners.containsKey(inv.<String>getArgument(0)));
        when(repository.updateTraceCodeOwnerIfExists(anyString(), anyString(), anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    String code = inv.getArgument(0);
                    String expectedFromOwner = inv.getArgument(1);
                    String newOwnerId = inv.getArgument(3);
                    if (!traceOwners.containsKey(code)) {
                        return 0;
                    }
                    if (!Objects.equals(traceOwners.get(code), expectedFromOwner)) {
                        return 0;
                    }
                    traceOwners.put(code, newOwnerId);
                    return 1;
                });

        financeRepository = mock(FinanceRepository.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        service = new InventoryService(
                repository,
                new IdempotencyService(new FakeIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(new FakeAuditEventRepository()),
                authorizationService,
                mock(TerminalSwitchGuard.class),
                mock(TraceabilityService.class),
                null,
                null,
                null,
                financeRepository, null, null, null);
    }

    @Test
    void returnBorrowedOrderFlipsOwnershipBackToLenderAndMarksReturned() {
        // A(出让方 m_a) 借给 B(借入方 m_b)，收货时归属已翻到 m_b。归还应翻回 m_a 并置 returned。
        seedOrder(order("to_borrow", "lending", "borrow", "m_a", "m_b", "received"));
        seedLine("to_borrow", List.of("TRACE-1", "TRACE-2"));
        traceOwners.put("TRACE-1", "m_b");
        traceOwners.put("TRACE-2", "m_b");

        InventoryTransferOrderRecord returned = service.returnTransferOrder(
                "to_borrow", new ReceiveTransferRequest("idem-return-1", null, null));

        assertThat(returned.status()).isEqualTo("returned");
        assertThat(traceOwners.get("TRACE-1")).isEqualTo("m_a");
        assertThat(traceOwners.get("TRACE-2")).isEqualTo("m_a");
        verify(financeRepository, never()).updateArApItemLedger(any());
    }

    @Test
    void returnRejectsNonBorrowTransferType() {
        seedOrder(order("to_supply", "supply", null, "supply_1", "m_b", "received"));

        assertThatThrownBy(() -> service.returnTransferOrder(
                "to_supply", new ReceiveTransferRequest("idem-return-2", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("TRANSFER_NOT_RETURNABLE"));
    }

    @Test
    void returnRejectsWhenNotReceived() {
        seedOrder(order("to_borrow2", "lending", "borrow", "m_a", "m_b", "shipped"));

        assertThatThrownBy(() -> service.returnTransferOrder(
                "to_borrow2", new ReceiveTransferRequest("idem-return-3", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("TRANSFER_ORDER_NOT_RECEIVED"));
    }

    @Test
    void cancelShippedOrderReversesUnpaidPayableAndMarksCancelled() {
        seedOrder(order("to_ship", "supply", null, "supply_1", "m_b", "shipped"));
        ArApItemRecord payable = new ArApItemRecord(
                "arap_trf_to_ship", "ARAP-TRF-to_ship", "payable", "supply_1", "inventory_transfer", "to_ship",
                12000L, 0L, 12000L, "CNY", LocalDate.parse("2026-07-01"), null,
                "pending_payment", Instant.now(), Instant.now(), "m_b", null);
        when(financeRepository.findArApItemById("arap_trf_to_ship")).thenReturn(Optional.of(payable));

        InventoryTransferOrderRecord cancelled = service.cancelTransferOrder(
                "to_ship", new ReceiveTransferRequest("idem-cancel-1", null, null));

        assertThat(cancelled.status()).isEqualTo("cancelled");
        ArgumentCaptor<ArApItemRecord> captor = ArgumentCaptor.forClass(ArApItemRecord.class);
        verify(financeRepository).updateArApItemLedger(captor.capture());
        ArApItemRecord reversed = captor.getValue();
        assertThat(reversed.status()).isEqualTo("cancelled");
        assertThat(reversed.paidAmountCent()).isZero();
        assertThat(reversed.unpaidAmountCent()).isZero();
        // amount_cent 保持正值留痕(updateArApItemLedger 不改金额,且 DB CHECK 要求 >0)。
        assertThat(reversed.amountCent()).isEqualTo(12000L);
    }

    @Test
    void cancelRejectsPartiallyPaidPayable() {
        seedOrder(order("to_ship2", "supply", null, "supply_1", "m_b", "shipped"));
        ArApItemRecord partlyPaid = new ArApItemRecord(
                "arap_trf_to_ship2", "ARAP-TRF-to_ship2", "payable", "supply_1", "inventory_transfer", "to_ship2",
                12000L, 5000L, 7000L, "CNY", LocalDate.parse("2026-07-01"), null,
                "pending_payment", Instant.now(), Instant.now(), "m_b", null);
        when(financeRepository.findArApItemById("arap_trf_to_ship2")).thenReturn(Optional.of(partlyPaid));

        assertThatThrownBy(() -> service.cancelTransferOrder(
                "to_ship2", new ReceiveTransferRequest("idem-cancel-2", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("TRANSFER_PAYABLE_PARTIALLY_PAID"));
        verify(financeRepository, never()).updateArApItemLedger(any());
    }

    @Test
    void cancelCreatedOrderNeedsNoReversal() {
        seedOrder(order("to_created", "lending", "borrow", "m_a", "m_b", "created"));

        InventoryTransferOrderRecord cancelled = service.cancelTransferOrder(
                "to_created", new ReceiveTransferRequest("idem-cancel-3", null, null));

        assertThat(cancelled.status()).isEqualTo("cancelled");
        verify(financeRepository, never()).findArApItemById(any());
    }

    private void seedOrder(InventoryTransferOrderRecord order) {
        orders.put(order.id(), order);
    }

    private void seedLine(String orderId, List<String> traceCodes) {
        lines.put(orderId, List.of(new InventoryTransferLineRecord(
                orderId + "_l0", orderId, "item_x", traceCodes.size(), traceCodes, 0L, Instant.now())));
    }

    private static InventoryTransferOrderRecord order(String id, String transferType, String lendingMode,
            String fromOwnerId, String toOwnerId, String status) {
        Instant now = Instant.now();
        return new InventoryTransferOrderRecord(id, transferType, lendingMode,
                "supply".equals(transferType) ? "supply_entity" : "merchant", fromOwnerId,
                "merchant", toOwnerId, null, null, status, null, "usr_admin", now, now, List.of());
    }

    private static final class FakeIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class FakeAuditEventRepository implements AuditEventRepository {
        @Override
        public AuditEvent save(AuditEvent event) {
            return event;
        }
    }
}
